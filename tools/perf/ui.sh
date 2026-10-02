#!/usr/bin/env bash
# Helpers for scripted UI checks via uiautomator. Source this file; requires ANDROID_SERIAL or a single device.
PKG=${PKG:-org.cog.hymnchtv}
UI_DUMP=/sdcard/hymn_ui.xml

dump_ui() {
  adb shell uiautomator dump "$UI_DUMP" >/dev/null 2>&1 || { sleep 2; adb shell uiautomator dump "$UI_DUMP" >/dev/null; }
  adb shell cat "$UI_DUMP"
}

# tap_node <attribute-regex>: taps the centre of the first node whose XML matches the regex.
tap_node() {
  local nums
  nums=$(dump_ui | tr '>' '\n' | grep -E "$1" | head -1 \
    | grep -oE 'bounds="\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]"' | tr -c '0-9' ' ')
  if [ -z "$nums" ]; then echo "tap_node: nothing matches $1" >&2; return 1; fi
  set -- $nums
  adb shell input tap $(( ($1 + $3) / 2 )) $(( ($2 + $4) / 2 ))
}

tap_id()   { tap_node "resource-id=\"$PKG:id/$1\""; }
tap_desc() { tap_node "content-desc=\"$1\""; }

# screen_has <extended-regex>
screen_has() { dump_ui | grep -qE "$1"; }

# wait_screen <extended-regex> [timeout-seconds=45]: polls the UI until it matches (slow or loaded hosts).
wait_screen() {
  local deadline=$((SECONDS + ${2:-45}))
  while [ "$SECONDS" -lt "$deadline" ]; do
    if screen_has "$1"; then return 0; fi
    sleep 2
  done
  return 1
}

long_press_center() {
  local size w h
  size=$(adb shell wm size | tr -d '\r' | awk '{print $NF}')
  w=${size%x*}; h=${size#*x}
  adb shell input swipe $((w / 2)) $((h / 2)) $((w / 2)) $((h / 2)) 1200
}
