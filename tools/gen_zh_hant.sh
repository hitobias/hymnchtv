#!/usr/bin/env bash
# Regenerate the zh-Hant UI strings from Simplified Chinese: opencc s2twp draft, then the manual override table.
# Manual wording choices live in tools/zh_hant_ui_overrides.tsv, so re-running this script never loses them.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/hymnchtv/src/main/res/values-zh/strings.xml"
DST_DIR="$ROOT/hymnchtv/src/main/res/values-b+zh+Hant"
OVERRIDES="$ROOT/tools/zh_hant_ui_overrides.tsv"
command -v opencc >/dev/null || { echo "opencc not found: brew install opencc" >&2; exit 1; }
mkdir -p "$DST_DIR"
opencc -c s2twp.json -i "$SRC" -o "$DST_DIR/strings.xml"

python3 - "$DST_DIR/strings.xml" "$OVERRIDES" <<'PY'
import re, sys

target, table = sys.argv[1], sys.argv[2]
text = open(target, encoding="utf-8").read()
count = 0
for lineno, line in enumerate(open(table, encoding="utf-8"), 1):
    line = line.rstrip("\n")
    if not line.strip() or line.startswith("#"):
        continue
    if "\t" not in line:
        sys.exit("%s:%d: expected key<TAB>value" % (table, lineno))
    key, value = line.split("\t", 1)
    value = value.replace("<NL>", "\n")
    pattern = re.compile(r'(<string name="%s"[^>]*>)(.*?)(</string>)' % re.escape(key), re.S)
    text, n = pattern.subn(lambda m: m.group(1) + value + m.group(3), text, count=1)
    if n != 1:
        sys.exit("override key not found: %s" % key)
    count += 1
open(target, "w", encoding="utf-8").write(text)
print("Applied %d overrides" % count)
PY
echo "Written to $DST_DIR/strings.xml - review every changed term before committing."
