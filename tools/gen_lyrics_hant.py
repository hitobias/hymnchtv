#!/usr/bin/env python3
"""Generate Traditional Chinese lyrics assets from the Simplified sources with OpenCC (plan A.1.9).

Usage: tools/gen_lyrics_hant.py [--report]
Requires the OpenCC CLI (brew install opencc). Output is deterministic for the same inputs.
"""
import argparse
import csv
import hashlib
import pathlib
import shutil
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
ASSETS = ROOT / "hymnchtv/src/main/assets"
GENERATOR = pathlib.Path(__file__).resolve()
OVERRIDES = ROOT / "tools/lyrics_hant_overrides.tsv"
MANIFEST = ASSETS / "lyrics_hant_manifest.txt"
REPORT = ROOT / "docs/superpowers/plans/lyrics-hant-review.csv"
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


def clean_stale(expected_dirs, expected_files):
    for d in ASSETS.glob("lyrics_*_text_hant_*"):
        if d.is_dir() and d.name not in expected_dirs:
            shutil.rmtree(d)
    for d in expected_dirs:
        for f in (ASSETS / d).iterdir():
            if f.is_dir():
                shutil.rmtree(f)
            elif rel(f) not in expected_files:
                f.unlink()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--report", action="store_true", help=f"also write {REPORT.relative_to(ROOT)}")
    args = parser.parse_args()
    if shutil.which("opencc") is None:
        sys.exit("opencc not found: brew install opencc")

    sources = source_files()
    texts = [read(p) for p in sources]
    rules = load_overrides({rel(p) for p in sources})
    # Convert every variant first so a failure leaves the working tree untouched.
    converted = {
        variant: [apply_overrides(t, variant, rel(src), rules)
                  for src, t in zip(sources, opencc_batch(texts, config))]
        for variant, config in VARIANTS.items()
    }
    expected_dirs, expected_files = set(), set()
    for variant in VARIANTS:
        for src, text in zip(sources, converted[variant]):
            dst_dir = ASSETS / f"{src.parent.name}_hant_{variant}"
            dst_dir.mkdir(exist_ok=True)
            (dst_dir / src.name).write_bytes(text.encode("utf-8"))
            expected_dirs.add(dst_dir.name)
            expected_files.add(rel(dst_dir / src.name))
    clean_stale(expected_dirs, expected_files)

    rows = [f"#input\t{rel_path}\t{sha1(path.read_bytes())}"
            for rel_path, path in (("tools/gen_lyrics_hant.py", GENERATOR), ("tools/lyrics_hant_overrides.tsv", OVERRIDES))]
    outputs = sources + [ASSETS / f for f in sorted(expected_files)]
    rows += sorted(f"{rel(p)}\t{sha1(p.read_bytes())}" for p in outputs)
    version = "opencc unknown"  # opencc 1.4.2 has no --version flag; ask Homebrew when available
    if shutil.which("brew"):
        brew = subprocess.run(["brew", "list", "--versions", "opencc"], capture_output=True, text=True)
        version = brew.stdout.strip() or version
    header = f"# generated by tools/gen_lyrics_hant.py; {version}"
    MANIFEST.write_bytes((header + "\n" + "\n".join(rows) + "\n").encode("utf-8"))

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
    print(f"Generated {len(sources)} files x {len(VARIANTS)} variants")


if __name__ == "__main__":
    main()
