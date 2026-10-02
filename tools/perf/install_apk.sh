#!/usr/bin/env bash
# Usage: tools/perf/install_apk.sh debug|benchmark
# Builds (unless SKIP_BUILD=1) and installs with every runtime permission granted, so no permission
# dialog covers the UI during measurements. Run it through emu_lock.sh.
set -euo pipefail
VARIANT=${1:?usage: install_apk.sh debug|benchmark}
ROOT=$(git rev-parse --show-toplevel)
case "$VARIANT" in
  debug) TASK=assembleDebug ;;
  benchmark) TASK=assembleBenchmark ;;
  *) echo "unknown variant: $VARIANT" >&2; exit 2 ;;
esac
if [ "${SKIP_BUILD:-0}" != 1 ]; then (cd "$ROOT" && ./gradlew -q ":hymnchtv:$TASK"); fi
APK="$ROOT/hymnchtv/build/outputs/apk/$VARIANT/hymnchtv-$VARIANT.apk"
[ -f "$APK" ] || { echo "APK not found: $APK" >&2; exit 1; }
adb install -r -g "$APK" >/dev/null
echo "installed $VARIANT on $(adb get-serialno) (API $(adb shell getprop ro.build.version.sdk | tr -d '\r'))"
