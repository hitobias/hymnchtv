#!/usr/bin/env bash
# Usage: ANDROID_SERIAL=<serial> tools/perf/release_smoke.sh <apk> [out-dir]
# Smoke test of a minified build (R8, 1.6.0). Use the benchmark build type: release configuration, debug-signed, so it
# installs over a debug build. Installs with every runtime permission, drives the main flows with uiautomator taps
# (contents -> hymn -> score -> MIDI playback in the foreground service -> share -> settings list dialog) and fails on any
# crash or on a class, method or worker R8 removed. Screenshots go to out-dir (default build/perf/smoke).
set -euo pipefail
APK=${1:?usage: release_smoke.sh <apk> [out-dir]}
OUT=${2:-build/perf/smoke}
PKG=com.ziontkec.hymnal
export PKG
# shellcheck source=tools/perf/ui.sh
source "$(dirname "$0")/ui.sh"
mkdir -p "$OUT"

fail() { echo "SMOKE FAIL: $*" >&2; adb exec-out screencap -p > "$OUT/fail.png" || true; exit 1; }
step() { echo "== $*"; }
rid() { echo "resource-id=\"$PKG:id/$1\""; }

adb install -r -g "$APK" >/dev/null
adb logcat -c
size=$(adb shell wm size | tr -d '\r' | awk '{print $NF}'); w=${size%x*}; h=${size#*x}

step "start"
adb shell am start -W -n "$PKG/org.cog.hymnchtv.MainActivity" >/dev/null
wait_screen "$(rid btn_home_toc)" 60 || fail "home page"
# the change history of a first start opens a moment after the home page and covers it
sleep 5
if screen_has 'text="(CONFIRM|Confirm|確認|确认)"'; then tap_node 'text="(CONFIRM|Confirm|確認|确认)"'; sleep 1; fi
wait_screen "$(rid btn_home_toc)" 30 || fail "home page after the change history"

step "contents -> first hymn"
tap_id btn_home_toc; wait_screen "$(rid toc_book_db)" || fail "contents"
tap_id toc_book_db; wait_screen "$(rid hymnCategory)" || fail "categories"
tap_id hymnCategory; wait_screen "$(rid hymnTitle)" || fail "hymn list"
tap_id hymnTitle; wait_screen "$(rid viewPager)" || fail "lyrics page"

step "score page (asset image through Glide)"
if ! screen_has "$(rid scoreContainer)"; then
  # a centre tap shows the toolbars for a moment; the mode button switches lyrics <-> score
  adb shell input tap $((w / 2)) $((h / 2)); sleep 1
  tap_id button_mode || fail "mode button"
  sleep 3
fi
screen_has "$(rid scoreContainer)" || fail "no score shown"
adb exec-out screencap -p > "$OUT/score.png"

step "MIDI playback (raw resources by name) in the foreground"
tap_id capsuleNote || tap_id capsuleExpand || fail "capsule"
wait_screen "$(rid btn_banzhou)" || fail "player card"
tap_id btn_banzhou; sleep 1
tap_id playback_play; sleep 4
adb shell dumpsys activity services "$PKG" | grep -q "isForeground=true" || fail "playback is not a foreground service"
tap_id playback_play; sleep 1

step "share"
adb shell input tap $((w / 2)) $((h / 2)); sleep 1
tap_id btn_share || fail "share button"
wait_screen 'resolver_list|intentresolver|chooser' 20 || fail "no chooser"
adb exec-out screencap -p > "$OUT/share.png"
adb shell input keyevent KEYCODE_BACK; sleep 2

step "settings list dialog"
# back out of the lyrics page (and a stopped player) to the home page
for _ in 1 2 3 4; do
  screen_has "$(rid btn_home_settings)" && break
  adb shell input keyevent KEYCODE_BACK; sleep 2
done
wait_screen "$(rid btn_home_settings)" || fail "home again"
tap_id btn_home_settings; sleep 2
tap_node 'text="(Theme|應用介面主題|应用界面主题)"' || fail "theme row"
wait_screen 'alertTitle|select_dialog_listview' || fail "theme dialog"
adb exec-out screencap -p > "$OUT/dialog.png"
adb shell input keyevent KEYCODE_BACK

step "a DB hymn without a MIDI (getFileResId probes a raw resource that does not exist; R8 once made it crash)"
# hymn 28 has no bm28.mid in res/raw (keep this number in sync if one is ever added)
[[ ! -f "$(dirname "$0")/../../hymnchtv/src/main/res/raw/bm28.mid" ]] || fail "bm28.mid exists now: pick another hymn number for this step"
adb shell am start -W -n "$PKG/org.cog.hymnchtv.ContentHandler" --es hymn_type hymn_db --ei hymn_number 28 >/dev/null
wait_screen "$(rid viewPager)" 30 || fail "lyrics page of hymn 28"
sleep 3
adb exec-out screencap -p > "$OUT/no-midi.png"
screen_has "$(rid viewPager)" || fail "hymn 28 closed (crash?)"

step "crashes and removed classes (the launch update check runs 30 s after start)"
sleep 35
if adb logcat -d -b crash | grep -q "$PKG"; then adb logcat -d -b crash | tail -40; fail "crash"; fi
APP_PID=$(adb shell pidof "$PKG" | tr -d '\r' | awk '{print $1}')
[[ -n "$APP_PID" ]] || fail "the app is not running any more (crash?)"
if adb logcat -d --pid="$APP_PID" | grep -E "ClassNotFoundException|NoSuchMethodException|NoSuchMethodError|NoSuchFieldError|Could not instantiate|Could not create Worker"; then
  fail "R8 removed something that is still needed"
fi
echo "SMOKE OK ($OUT)"
