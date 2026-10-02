#!/usr/bin/env bash
# Usage: tools/perf/strictmode_report.sh <label>
# DEBUG build only. Cold start → open 大本 #1 → back, then counts StrictMode violations whose stack contains
# our package, grouped by violation type and the innermost org.cog.hymnchtv frame. Run it through emu_lock.sh.
set -euo pipefail
LABEL=${1:?usage: strictmode_report.sh <label>}
HERE=$(cd "$(dirname "$0")" && pwd)
source "$HERE/ui.sh"
ROOT=$(git rev-parse --show-toplevel)
SDK=$(adb shell getprop ro.build.version.sdk | tr -d '\r')
OUT_DIR="$ROOT/build/perf"; mkdir -p "$OUT_DIR"
RAW="$OUT_DIR/strictmode-$LABEL-api$SDK.log"

adb shell am force-stop "$PKG"
adb logcat -c
adb shell am start -W -n "$PKG/.MainActivity" >/dev/null
sleep 4
tap_id n1
tap_id bs_db
sleep 4
adb shell input keyevent KEYCODE_BACK
sleep 2
adb logcat -d -v brief 'StrictMode:D' '*:S' > "$RAW"

echo "### StrictMode $LABEL (API $SDK), raw log: $RAW"
awk '
  function flush() { if (type != "" && frame != "") count[type " @ " frame]++; type = ""; frame = "" }
  /policy violation/ {
    flush()
    if (match($0, /[A-Za-z0-9_.$]*Violation/)) type = substr($0, RSTART, RLENGTH); else type = "Violation"
    next
  }
  /at org\.cog\.hymnchtv\./ {
    if (type != "" && frame == "") { f = $0; sub(/.*at /, "", f); sub(/\(.*/, "", f); frame = f }
  }
  END { flush(); for (k in count) printf "| %d | %s |\n", count[k], k }
' "$RAW" | sort -t'|' -k2 -rn
