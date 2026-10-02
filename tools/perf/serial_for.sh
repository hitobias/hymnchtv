#!/usr/bin/env bash
# Usage: export ANDROID_SERIAL=$(tools/perf/serial_for.sh api34)
# Prints the adb serial of the running emulator whose AVD name matches $1.
set -euo pipefail
AVD=${1:?usage: serial_for.sh <avd-name>}
for s in $(adb devices | awk '/^emulator-[0-9]+[[:space:]]+device$/ {print $1}'); do
  name=$(adb -s "$s" emu avd name 2>/dev/null | head -1 | tr -d '\r')
  if [ "$name" = "$AVD" ]; then echo "$s"; exit 0; fi
done
echo "AVD '$AVD' is not running; start it with: emulator -avd $AVD &" >&2
exit 1
