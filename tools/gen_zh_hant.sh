#!/usr/bin/env bash
# Regenerate the zh-Hant UI strings draft from Simplified Chinese. Output MUST be reviewed by hand.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/hymnchtv/src/main/res/values-zh/strings.xml"
DST_DIR="$ROOT/hymnchtv/src/main/res/values-b+zh+Hant"
command -v opencc >/dev/null || { echo "opencc not found: brew install opencc" >&2; exit 1; }
mkdir -p "$DST_DIR"
opencc -c s2twp.json -i "$SRC" -o "$DST_DIR/strings.xml"
echo "Draft written to $DST_DIR/strings.xml - review every changed term before committing."
