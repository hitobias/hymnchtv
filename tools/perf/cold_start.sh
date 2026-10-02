#!/usr/bin/env bash
# Usage: tools/perf/cold_start.sh <label> [runs=15] [warmup=2]
# PROCESS-COLD, CACHE-WARM start of MainActivity: the process is killed each run (am start -S) but page cache,
# ART artifacts and the WebView provider stay warm. Not a first-boot start. Metric: am start -W TotalTime
# (time to first frame only; it cannot show work merely moved after the first frame).
# Prints one markdown row for the measurements doc. Install the benchmark build first. Run it through emu_lock.sh.
set -euo pipefail
PKG=org.cog.hymnchtv
LABEL=${1:?usage: cold_start.sh <label> [runs] [warmup]}
RUNS=${2:-15}
WARMUP=${3:-2}
ROOT=$(git rev-parse --show-toplevel)
SDK=$(adb shell getprop ro.build.version.sdk | tr -d '\r')
OUT_DIR="$ROOT/build/perf"; mkdir -p "$OUT_DIR"
RAW="$OUT_DIR/cold_start-$LABEL-api$SDK.txt"
: > "$RAW"

adb shell pm list packages | tr -d '\r' | grep -qx "package:$PKG" || { echo "$PKG is not installed" >&2; exit 1; }
if adb shell dumpsys package "$PKG" | grep -q 'flags=.*DEBUGGABLE'; then
  echo "refusing to measure a debuggable build; install the benchmark variant" >&2; exit 1
fi
if [ "$SDK" -ge 29 ]; then
  adb shell dumpsys thermalservice | tr -d '\r' | grep -m1 'Thermal Status' >&2 || true
fi

for i in $(seq 1 $((WARMUP + RUNS))); do
  t=$(adb shell am start -W -S -n "$PKG/.MainActivity" | tr -d '\r' | awk -F': ' '/^TotalTime/ {print $2}')
  if [ -z "$t" ]; then echo "run $i: no TotalTime in am start output" >&2; exit 1; fi
  if [ "$i" -gt "$WARMUP" ]; then echo "$t" >> "$RAW"; fi
  sleep 3
done
adb shell am force-stop "$PKG"

SHA=$(git -C "$ROOT" rev-parse --short HEAD)
echo "# process-cold, cache-warm TTID (am start -W TotalTime, ms), raw: $RAW" >&2
sort -n "$RAW" | awk -v label="$LABEL" -v sdk="$SDK" -v sha="$SHA" '
  { a[NR] = $1 }
  END {
    med = (NR % 2) ? a[(NR + 1) / 2] : (a[NR / 2] + a[NR / 2 + 1]) / 2
    q1 = a[int((NR + 3) / 4)]; q3 = a[int((3 * NR + 1) / 4)]
    printf "| %s | %s | %d | %s | %s | %s | %s | %s | %s |\n", label, sdk, NR, med, q1, q3, a[1], a[NR], sha
  }'
