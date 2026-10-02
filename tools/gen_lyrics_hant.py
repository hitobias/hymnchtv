#!/usr/bin/env python3
"""Generate Traditional Chinese lyrics assets from the Simplified sources with OpenCC (plan A.1.9).

Pipeline per variant: OpenCC (s2tw / s2hk) -> Recovery Version preferences (tools/rcv_prefs.py with the tables
from tools/gen_rcv_prefs.py and the manual decisions in tools/rcv_pair_decisions.tsv) -> exact-count overrides.

Usage: tools/gen_lyrics_hant.py [--report]
Requires the OpenCC CLI (brew install opencc). Output is deterministic for the same inputs.
"""
import argparse
import csv
import hashlib
import os
import pathlib
import shutil
import subprocess
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import rcv_prefs as rp  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parent.parent
ASSETS = ROOT / "hymnchtv/src/main/assets"
GENERATOR = pathlib.Path(__file__).resolve()
OVERRIDES = ROOT / "tools/lyrics_hant_overrides.tsv"
RCV_INPUTS = [ROOT / "tools" / n for n in ("rcv_prefs.py", "rcv_word_prefs.tsv", "rcv_char_prefs.tsv", "rcv_pair_decisions.tsv")]
MANIFEST = ASSETS / "lyrics_hant_manifest.txt"
STAGING = ROOT / "hymnchtv/build/lyrics-hant-staging"
T2S_MAP = ASSETS / "lyrics_t2s_map.txt"
REPORT = ROOT / "docs/superpowers/plans/lyrics-hant-review.csv"
RCV_REPORT = ROOT / "docs/superpowers/plans/lyrics-hant-review-rcv.csv"
VARIANTS = {"tw": "s2tw.json", "hk": "s2hk.json"}
SPLIT = "\n@@@HYMNCHTV_SPLIT@@@\n"  # ASCII marker survives OpenCC unchanged
AMBIGUOUS = set("于里后复发只干历面云台余松谷斗志准范冲尽获系钟制致表卷借恶征党丑")


def sha1(data: bytes) -> str:
    return hashlib.sha1(data).hexdigest()


def source_dirs():
    return sorted(d for d in ASSETS.glob("lyrics_*_text") if d.is_dir())


def source_files():
    return [f for d in source_dirs() for f in sorted(d.glob("*.txt"))]


def rel(path):
    return path.relative_to(ASSETS).as_posix()


def read(path):
    # bytes -> str keeps CRLF line endings exactly
    return path.read_bytes().decode("utf-8")


def opencc_batch(texts, config):
    if any("@@@HYMNCHTV_SPLIT@@@" in t for t in texts):
        sys.exit("A lyrics file contains the split marker")
    joined = SPLIT.join(texts)
    try:
        result = subprocess.run(["opencc", "-c", config], input=joined.encode("utf-8"),
                                capture_output=True, check=True)
    except subprocess.CalledProcessError as e:
        sys.exit(f"opencc -c {config} failed: {e.stderr.decode(errors='replace')}")
    parts = result.stdout.decode("utf-8").split(SPLIT)
    if len(parts) != len(texts):
        sys.exit(f"OpenCC changed the split marker: {len(parts)} parts for {len(texts)} files")
    return parts


def load_overrides(known_sources):
    rules = []
    if not OVERRIDES.exists():
        return rules
    for n, line in enumerate(OVERRIDES.read_text(encoding="utf-8").splitlines(), 1):
        if not line.strip() or line.startswith("#"):
            continue
        cols = line.split("\t")
        if len(cols) != 5 or cols[0] not in ("tw", "hk", "*") or not cols[2] or not cols[4].isdigit() or int(cols[4]) < 1:
            sys.exit(f"{OVERRIDES.name}:{n}: expected 'variant<TAB>source-path<TAB>from<TAB>to<TAB>count'")
        if cols[1] not in known_sources:
            sys.exit(f"{OVERRIDES.name}:{n}: unknown source file {cols[1]}")
        rules.append((n, cols[0], cols[1], cols[2], cols[3], int(cols[4])))
    return rules


def apply_overrides(text, variant, source, rules):
    for n, v, path, frm, to, count in rules:
        if path != source or v not in (variant, "*"):
            continue
        hits = text.count(frm)
        if hits != count:
            sys.exit(f"{OVERRIDES.name}:{n}: expected {count} hit(s) of '{frm}' in {source} [{variant}], found {hits}")
        text = text.replace(frm, to)
    return text


def load_rcv_prefs():
    word, char, pairs = (p.read_text(encoding="utf-8").splitlines() for p in RCV_INPUTS[1:])
    try:
        return rp.parse_tables(word, char, pairs)
    except ValueError as e:
        sys.exit(f"Recovery Version tables: {e}")


def apply_rcv(prefs, keys, texts):
    """Apply the RcV preferences line by line (keys = t2s of texts). Returns (texts, events per file)."""
    out, events = [], []
    for key, text in zip(keys, texts):
        k_lines, t_lines = key.split("\n"), text.split("\n")
        if len(k_lines) != len(t_lines):
            sys.exit("OpenCC t2s changed the line count of a lyrics file")
        done = [rp.apply_line(prefs, k, t) for k, t in zip(k_lines, t_lines)]
        out.append("\n".join(line for line, _ in done))
        events.append([ev for _, ev in done])
    return out, events


def publish(outputs, t2s_text, input_paths):
    """Stage every output under hymnchtv/build, then swap whole directories in with renames.

    outputs: {asset-relative path: text}. A failure while staging leaves the assets untouched.
    """
    if STAGING.exists():
        shutil.rmtree(STAGING)
    new_dir, old_dir = STAGING / "new", STAGING / "old"
    for path, text in outputs.items():
        target = new_dir / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(text.encode("utf-8"))
    (new_dir / T2S_MAP.name).write_bytes(t2s_text.encode("utf-8"))
    rows = [f"#input\t{p.relative_to(ROOT).as_posix()}\t{sha1(p.read_bytes())}" for p in input_paths]
    hashed = {rel(p): p.read_bytes() for p in source_files()}
    hashed.update({path: text.encode("utf-8") for path, text in outputs.items()})
    hashed[T2S_MAP.name] = t2s_text.encode("utf-8")
    rows += sorted(f"{path}\t{sha1(data)}" for path, data in hashed.items())
    (new_dir / MANIFEST.name).write_bytes(("# generated by tools/gen_lyrics_hant.py\n" + "\n".join(rows) + "\n").encode("utf-8"))

    old_dir.mkdir()
    for d in ASSETS.glob("lyrics_*_text_hant_*"):
        if d.is_dir():
            d.rename(old_dir / d.name)  # stale variants disappear with the rest of old/
    for d in sorted(new_dir.iterdir()):
        if d.is_dir():
            d.rename(ASSETS / d.name)
    for name in (T2S_MAP.name, MANIFEST.name):
        os.replace(new_dir / name, ASSETS / name)
    shutil.rmtree(STAGING)


def build_t2s_map(texts, converted):
    """Return the T2S map file text. Traditional char -> every Simplified char it came from (itself included when it also stays unchanged)."""
    mapping, identity = {}, set()
    for variant_out in converted.values():
        for src, out in zip(texts, variant_out):
            s_lines, t_lines = src.splitlines(), out.splitlines()
            if len(s_lines) != len(t_lines):
                sys.exit("OpenCC changed the line count of a lyrics file; cannot align for the T2S map")
            for s_line, t_line in zip(s_lines, t_lines):
                if len(s_line) != len(t_line):
                    continue  # phrase conversion changed the length; no reliable alignment
                for a, b in zip(s_line, t_line):
                    if a == b:
                        identity.add(b)
                    else:
                        mapping.setdefault(b, set()).add(a)
    lines = []
    for b in sorted(mapping):
        candidates = mapping[b] | ({b} if b in identity else set())
        lines.append(f"{b}\t{''.join(sorted(candidates))}")
    return "\n".join(lines) + "\n"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--report", action="store_true", help=f"also write {REPORT.relative_to(ROOT)}")
    args = parser.parse_args()
    if shutil.which("opencc") is None:
        sys.exit("opencc not found: brew install opencc")

    sources = source_files()
    texts = [read(p) for p in sources]
    rules = load_overrides({rel(p) for p in sources})
    prefs = load_rcv_prefs()
    # Convert every variant first so a failure leaves the working tree untouched.
    raw, converted, events = {}, {}, {}
    for variant, config in VARIANTS.items():
        raw[variant] = opencc_batch(texts, config)
        preferred, events[variant] = apply_rcv(prefs, opencc_batch(raw[variant], "t2s.json"), raw[variant])
        converted[variant] = [apply_overrides(t, variant, rel(src), rules) for src, t in zip(sources, preferred)]
    # The search map also keeps OpenCC's own forms (e.g. 裡 next to the RcV's 裏) because that is what people type.
    t2s_text = build_t2s_map(texts, {**converted, **{f"{v}-opencc": t for v, t in raw.items()}})  # may sys.exit
    outputs = {f"{src.parent.name}_hant_{variant}/{src.name}": text
               for variant in VARIANTS for src, text in zip(sources, converted[variant])}
    publish(outputs, t2s_text, [GENERATOR, OVERRIDES] + RCV_INPUTS)

    if args.report:
        with REPORT.open("w", encoding="utf-8", newline="") as fh:
            writer = csv.writer(fh)
            writer.writerow(["file", "line", "chars", "simplified", "tw", "hk"])
            for i, src in enumerate(sources):
                s_lines = texts[i].splitlines()
                tw_lines = converted["tw"][i].splitlines()
                hk_lines = converted["hk"][i].splitlines()
                for ln, s in enumerate(s_lines):
                    hits = sorted(AMBIGUOUS.intersection(s))
                    if hits:
                        writer.writerow([rel(src), ln + 1, "".join(hits), s, tw_lines[ln], hk_lines[ln]])
        write_rcv_report(prefs, sources, texts, converted["tw"], events["tw"])
    print(f"Generated {len(sources)} files x {len(VARIANTS)} variants")


def write_rcv_report(prefs, sources, texts, tw_texts, tw_events):
    """Occurrences the RcV tables could not settle: no evidence and OpenCC's char is not the RcV majority."""
    rows = 0
    with RCV_REPORT.open("w", encoding="utf-8", newline="") as fh:
        writer = csv.writer(fh)
        writer.writerow(["file", "line", "pos", "simplified_char", "current", "candidates", "simplified_line", "tw_line"])
        for src, text, tw, file_events in zip(sources, texts, tw_texts, tw_events):
            s_lines, t_lines = text.split("\n"), tw.split("\n")
            for ln, line_events in enumerate(file_events):
                for pos, s, a, _, how in line_events:
                    if rp.needs_review(prefs, s, a, how):
                        aligned = len(s_lines[ln]) == len(t_lines[ln])
                        writer.writerow([rel(src), ln + 1, pos + 1, s_lines[ln][pos] if aligned else s, t_lines[ln][pos],
                                         rp.format_dist(prefs.uni[s]), s_lines[ln].rstrip("\r"), t_lines[ln].rstrip("\r")])
                        rows += 1
    print(f"{RCV_REPORT.relative_to(ROOT)}: {rows} rows")


if __name__ == "__main__":
    main()
