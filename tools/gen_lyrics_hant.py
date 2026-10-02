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
RCV_INPUTS = [ROOT / "tools" / n for n in ("rcv_prefs.py", "rcv_word_prefs.tsv", "rcv_char_prefs.tsv", "rcv_pair_decisions.tsv",
                                           "rcv_review_decisions.tsv", "rcv_word_fixes.tsv")]
REVIEW_DECISIONS = RCV_INPUTS[-2]
WORD_FIXES = RCV_INPUTS[-1]
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


TOC_DIR = ASSETS / "lyrics_toc"


def source_dirs():
    """The per-book lyrics directories plus lyrics_toc (stroke/pinyin indexes, YB table, category names)."""
    return sorted([d for d in ASSETS.glob("lyrics_*_text") if d.is_dir()] + [TOC_DIR])


def is_lyrics(path):
    """Lyrics files feed the search T2S map; the TOC indexes only reuse their text."""
    return path.parent != TOC_DIR


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


def load_word_fixes():
    """(from, to) whole-word spellings from tools/rcv_word_fixes.tsv."""
    fixes = []
    for n, line in enumerate(WORD_FIXES.read_text(encoding="utf-8").splitlines(), 1):
        if not line.strip() or line.startswith("#"):
            continue
        cols = line.split("\t")
        if len(cols) != 3 or not cols[0] or len(cols[0]) != len(cols[1]):
            sys.exit(f"{WORD_FIXES.name}:{n}: expected 'from<TAB>to<TAB>note' with equal lengths")
        fixes.append((cols[0], cols[1]))
    return fixes


def apply_word_fixes(text, fixes):
    for frm, to in fixes:
        text = text.replace(frm, to)
    return text


def load_rcv_prefs():
    word, char, pairs = (p.read_text(encoding="utf-8").splitlines() for p in RCV_INPUTS[1:4])
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

    swap_in(new_dir, old_dir)
    shutil.rmtree(STAGING)


def swap_in(new_dir, old_dir):
    """Move the current outputs aside, move the staged ones in; on any failure put the old ones back."""
    old_dir.mkdir()
    names = [T2S_MAP.name, MANIFEST.name]
    moved_aside, moved_in = [], []
    try:
        for d in sorted(ASSETS.glob("lyrics_*_text_hant_*")) + sorted(ASSETS.glob("lyrics_toc_hant_*")) + [ASSETS / n for n in names]:
            if d.exists():
                d.rename(old_dir / d.name)  # stale variants disappear with the rest of old/
                moved_aside.append(d.name)
        for item in sorted(new_dir.iterdir()):
            item.rename(ASSETS / item.name)
            moved_in.append(item.name)
    except OSError as e:
        for name in moved_in:
            target = ASSETS / name
            shutil.rmtree(target) if target.is_dir() else target.unlink()
        for name in moved_aside:
            (old_dir / name).rename(ASSETS / name)
        sys.exit(f"could not swap the generated lyrics in ({e}); the previous outputs were restored")


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


def load_review_decisions(known_sources):
    """(source, line, pos) -> (from, to) from tools/rcv_review_decisions.tsv (1-based line and char position)."""
    out = {}
    for n, line in enumerate(REVIEW_DECISIONS.read_text(encoding="utf-8").splitlines(), 1):
        if not line.strip() or line.startswith("#"):
            continue
        cols = line.split("\t")
        if len(cols) != 6 or cols[0] not in known_sources or not (cols[1].isdigit() and cols[2].isdigit()) \
                or len(cols[3]) != 1 or len(cols[4]) != 1:
            sys.exit(f"{REVIEW_DECISIONS.name}:{n}: expected 'file<TAB>line<TAB>pos<TAB>from<TAB>to<TAB>reason'")
        out[(cols[0], int(cols[1]), int(cols[2]))] = (cols[3], cols[4])
    return out


def apply_review_decisions(texts, sources, decisions, variant):
    """TW: every decision must find its 'from' char (else the table is stale). HK: applied where HK has that char."""
    out = []
    for src, text in zip(sources, texts):
        lines = text.split("\n")
        for (path, ln, pos), (frm, to) in decisions.items():
            if path != rel(src):
                continue
            line = lines[ln - 1] if ln <= len(lines) else ""
            if pos <= len(line) and line[pos - 1] == frm:
                lines[ln - 1] = line[:pos - 1] + to + line[pos:]
            elif variant == "tw":
                sys.exit(f"{REVIEW_DECISIONS.name}: {path}:{ln}:{pos} is not '{frm}' in the TW output; re-run "
                         "tools/gen_rcv_review_decisions.py")
        out.append("\n".join(lines))
    return out


def convert(texts, prefs):
    """OpenCC then the RcV tables, per variant: (raw, preferred, events) dicts keyed by variant."""
    raw, preferred, events = {}, {}, {}
    for variant, config in VARIANTS.items():
        raw[variant] = opencc_batch(texts, config)
        preferred[variant], events[variant] = apply_rcv(prefs, opencc_batch(raw[variant], "t2s.json"), raw[variant])
    return raw, preferred, events


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
    word_fixes = load_word_fixes()
    # Convert every variant first so a failure leaves the working tree untouched.
    decisions = load_review_decisions({rel(p) for p in sources})
    raw, preferred, events = convert(texts, prefs)
    converted = {}
    for variant in VARIANTS:
        decided = apply_review_decisions(preferred[variant], sources, decisions, variant)
        fixed = [apply_word_fixes(t, word_fixes) for t in decided]
        converted[variant] = [apply_overrides(t, variant, rel(src), rules) for src, t in zip(sources, fixed)]
    # The search map also keeps OpenCC's own forms (e.g. 裡 next to the RcV's 裏) because that is what people type.
    is_l = [is_lyrics(p) for p in sources]
    pick = lambda items: [x for x, keep in zip(items, is_l) if keep]  # noqa: E731
    t2s_text = build_t2s_map(pick(texts), {k: pick(v) for k, v in {**converted, **{f"{v}-opencc": t for v, t in raw.items()}}.items()})  # may sys.exit
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
        write_rcv_report(prefs, sources, texts, converted["tw"], events["tw"], decisions)
    print(f"Generated {len(sources)} files x {len(VARIANTS)} variants")


def review_positions(prefs, sources, texts, tw_texts, tw_events):
    """Yield (source, line index, pos index, simplified char, s, a) for occurrences the RcV tables could not settle."""
    for src, text, tw, file_events in zip(sources, texts, tw_texts, tw_events):
        s_lines, t_lines = text.split("\n"), tw.split("\n")
        for ln, line_events in enumerate(file_events):
            for pos, s, a, _, how in line_events:
                if rp.needs_review(prefs, s, a, how):
                    aligned = len(s_lines[ln]) == len(t_lines[ln])
                    yield src, ln, pos, (s_lines[ln][pos] if aligned else s), s, a


def write_rcv_report(prefs, sources, texts, tw_texts, tw_events, decisions):
    """Occurrences the RcV tables could not settle and tools/rcv_review_decisions.tsv does not decide."""
    rows = 0
    with RCV_REPORT.open("w", encoding="utf-8", newline="") as fh:
        writer = csv.writer(fh)
        writer.writerow(["file", "line", "pos", "simplified_char", "current", "candidates", "simplified_line", "tw_line"])
        text_of = {rel(src): (text, tw) for src, text, tw in zip(sources, texts, tw_texts)}
        for src, ln, pos, simplified, s, _ in review_positions(prefs, sources, texts, tw_texts, tw_events):
            if (rel(src), ln + 1, pos + 1) in decisions:
                continue
            s_line, t_line = (t.split("\n")[ln] for t in text_of[rel(src)])
            writer.writerow([rel(src), ln + 1, pos + 1, simplified, t_line[pos], rp.format_dist(prefs.uni[s]),
                             s_line.rstrip("\r"), t_line.rstrip("\r")])
            rows += 1
    print(f"{RCV_REPORT.relative_to(ROOT)}: {rows} rows")


if __name__ == "__main__":
    main()
