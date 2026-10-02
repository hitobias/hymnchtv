#!/usr/bin/env bash
# Usage: tools/perf/import_perf.sh <label>
# Runs ImportPerfTest on $ANDROID_SERIAL and prints one markdown row per import path. Run it through emu_lock.sh.
set -euo pipefail
LABEL=${1:?usage: import_perf.sh <label>}
ROOT=$(git rev-parse --show-toplevel)
SDK=$(adb shell getprop ro.build.version.sdk | tr -d '\r')
adb logcat -c
(cd "$ROOT" && ./gradlew -q :hymnchtv:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=org.cog.hymnchtv.mediaconfig.ImportPerfTest)
adb logcat -d -v raw -s ImportPerf:I | tr -d '\r' | awk -v label="$LABEL" -v sdk="$SDK" '
  / median_ms=/ {
    path = $1; split($2, m, "="); s = $3; sub(/^samples=/, "", s); split($4, n, "=")
    printf "| %s | %s | %s | %s | %s | %s |\n", label, sdk, path, m[2], s, n[2]
  }'
