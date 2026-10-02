#!/usr/bin/env python3
"""Subset LXGW WenKai / LXGW WenKai TC into the lyrics fonts HymnalKai SC / TC (plan A2).

Usage: .venv-tools/bin/python tools/gen_font_subset.py
Needs fontTools == FONTTOOLS_VERSION (pinned: output must be byte-identical) and the two source fonts in
tools/fonts-src/ (git-ignored; URLs and SHA-256 in SOURCES).

Each font must cover every character of its lyrics (SC: Simplified; TC: both Traditional variants) and of every
strings file; there are no exceptions. The manifest records each font's SHA-256, its naming records and the code
points its cmap covers, so FontSubsetTest can check coverage without parsing TrueType.

Licence: SIL OFL 1.1. "LXGW", 霞鹜, 霞鶩, 落霞孤鹜 and 落霞孤鶩 are Reserved Font Names, and a subset is a
Modified Version, so every naming record is replaced with "HymnalKai". Copyright and licence records are kept.
"""
import hashlib
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
ASSETS = ROOT / "hymnchtv/src/main/assets"
RES = ROOT / "hymnchtv/src/main/res"
FONT_DIR = RES / "font"
SRC_DIR = ROOT / "tools/fonts-src"
MANIFEST = ROOT / "tools/font_subset_manifest.txt"
FONTTOOLS_VERSION = "4.66.1"

SOURCES = {
    "sc": ("LXGWWenKai-Regular.ttf", "39ad71264b588165b469e35e6afb162a378dacd1f95348160240ba9038ac3009",
           "https://github.com/lxgw/LxgwWenKai/releases/download/v1.522/LXGWWenKai-Regular.ttf"),
    "tc": ("LXGWWenKaiTC-Regular.ttf", "b1a0795862c1415bf3f393ea50b2a4ea6275012cf5bad3f94feeb1222f555731",
           "https://github.com/lxgw/LxgwWenkaiTC/releases/download/v1.522/LXGWWenKaiTC-Regular.ttf"),
}
OUTPUTS = {  # variant: (file in res/font, family name, PostScript family)
    "sc": ("hymnal_kai_sc.ttf", "HymnalKai SC", "HymnalKaiSC"),
    "tc": ("hymnal_kai_tc.ttf", "HymnalKai TC", "HymnalKaiTC"),
}
LYRIC_DIRS = {"sc": ("lyrics_*_text",), "tc": ("lyrics_*_text_hant_tw", "lyrics_*_text_hant_hk")}
RESERVED = ("lxgw", "霞鹜", "霞鶩", "落霞孤鹜", "落霞孤鶩")
NAMING_IDS = {1, 2, 3, 4, 6, 16, 17, 18, 21, 22, 25}


def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def wanted(cp):
    """Same rule as FontSubsetTest: everything except C0 controls, DEL and the BOM."""
    return cp >= 0x20 and cp != 0x7F and cp != 0xFEFF


def codepoints(paths):
    found = set()
    for p in paths:
        found.update(cp for cp in map(ord, p.read_text(encoding="utf-8")) if wanted(cp))
    return found


def lyric_files(variant):
    return sorted(f for pattern in LYRIC_DIRS[variant] for d in ASSETS.glob(pattern) if d.is_dir() for f in d.glob("*.txt"))


def string_files():
    return sorted(RES.glob("values*/strings*.xml"))


def ranges(cps):
    """Sorted code points as 'XXXX-YYYY' / 'XXXX' runs, space separated."""
    out, items = [], sorted(cps)
    i = 0
    while i < len(items):
        j = i
        while j + 1 < len(items) and items[j + 1] == items[j] + 1:
            j += 1
        out.append("%04X" % items[i] if i == j else "%04X-%04X" % (items[i], items[j]))
        i = j + 1
    return " ".join(out)


def check_sources():
    for name, digest, url in SOURCES.values():
        path = SRC_DIR / name
        if not path.is_file():
            sys.exit(f"missing {path.relative_to(ROOT)}; download it from {url}")
        if sha256(path) != digest:
            sys.exit(f"{name}: SHA-256 mismatch (expected {digest}); wrong release?")


def rename(font, family, ps_family):
    table = font["name"]
    table.names = [r for r in table.names if r.nameID not in NAMING_IDS]
    revision = "%.3f" % font["head"].fontRevision
    values = {1: family, 2: "Regular", 3: f"{revision};{ps_family}-Regular", 4: f"{family} Regular", 6: f"{ps_family}-Regular"}
    for name_id, text in values.items():
        table.setName(text, name_id, 3, 1, 0x409)
    naming = sorted((r.nameID, r.toUnicode()) for r in table.names if r.nameID in NAMING_IDS)
    for name_id, text in naming:
        if any(word in text.lower() for word in RESERVED):
            sys.exit(f"name ID {name_id} still contains a Reserved Font Name: {text}")
    return naming


def build(variant, wanted_codepoints):
    from fontTools import subset

    opts = subset.Options()
    opts.name_IDs = ["*"]
    opts.name_languages = ["*"]
    opts.name_legacy = True
    opts.notdef_outline = True
    opts.recalc_timestamp = False
    opts.drop_tables += ["DSIG"]
    font = subset.load_font(str(SRC_DIR / SOURCES[variant][0]), opts)
    subsetter = subset.Subsetter(opts)
    subsetter.populate(unicodes=sorted(wanted_codepoints))
    subsetter.subset(font)
    out_name, family, ps_family = OUTPUTS[variant]
    naming = rename(font, family, ps_family)
    out = FONT_DIR / out_name
    subset.save_font(font, str(out), opts)
    return out, set(font.getBestCmap()), naming


def main():
    import fontTools
    if fontTools.version != FONTTOOLS_VERSION:
        sys.exit(f"fontTools {fontTools.version} found; pin {FONTTOOLS_VERSION} so the output stays byte-identical")
    check_sources()
    FONT_DIR.mkdir(exist_ok=True)
    ui = codepoints(string_files()) | set(range(0x20, 0x7F))
    rows, problems = [], []
    for variant in ("sc", "tc"):
        required = codepoints(lyric_files(variant)) | ui
        out, cmap, naming = build(variant, required)
        missing = sorted(required - cmap)
        if missing:
            problems.append(f"{variant}: not in the source font: " + " ".join(f"U+{cp:04X}({chr(cp)})" for cp in missing))
        rows.append(f"font\t{variant}\t{out.relative_to(ROOT).as_posix()}\t{sha256(out)}")
        rows += [f"name\t{variant}\t{name_id}\t{text}" for name_id, text in naming]
        rows.append(f"covers\t{variant}\t{ranges(cmap)}")
        print(f"{out.name}: {out.stat().st_size / 1e6:.2f} MB, {len(required)} required code points, {len(missing)} missing")
    if problems:
        sys.exit("\n".join(problems) + "\nFix the lyrics or strings (usually a typo); there is no exception list.")
    MANIFEST.write_text(f"# generated by tools/gen_font_subset.py; fontTools {FONTTOOLS_VERSION}\n" + "\n".join(rows) + "\n",
                        encoding="utf-8")


if __name__ == "__main__":
    main()
