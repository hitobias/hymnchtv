#!/usr/bin/env bash
# Usage: tools/perf/locale_webview_check.sh            (TEST_LOCALE=en-US by default)
# DEBUG build (needs run-as on API < 33) on an emulator image that allows `adb root` (google_apis).
# Forces the SYSTEM locale to a non-Chinese locale, sets the APP to zh-Hant-TW, then checks zh-Hant strings:
# main screen after idle, the lyrics context menu, main screen after a lyrics page (WebView) was shown, and the
# main overflow menu. A WebView that resets resources to the system locale would show English and FAIL.
# Restores the original system locale on exit. Exits 1 on any failure. Run it through emu_lock.sh.
set -euo pipefail
HERE=$(cd "$(dirname "$0")" && pwd)
source "$HERE/ui.sh"
SDK=$(adb shell getprop ro.build.version.sdk | tr -d '\r')
TEST_LOCALE=${TEST_LOCALE:-en-US}
case "$TEST_LOCALE" in zh*) echo "TEST_LOCALE must not be Chinese: $TEST_LOCALE" >&2; exit 2 ;; esac
ORIG_LOCALE=$(adb shell getprop persist.sys.locale | tr -d '\r')
[ -n "$ORIG_LOCALE" ] || ORIG_LOCALE=$(adb shell getprop ro.product.locale | tr -d '\r')

# set_system_locale <tag>: needs root; soft-restarts the framework and waits for boot to complete.
set_system_locale() {
  adb root >/dev/null 2>&1 || true
  adb wait-for-device
  adb shell setprop persist.sys.locale "$1"
  adb shell 'stop; start'
  sleep 5
  adb wait-for-device
  until [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = 1 ]; do sleep 2; done
  sleep 5
  local now
  now=$(adb shell getprop persist.sys.locale | tr -d '\r')
  [ "$now" = "$1" ] || { echo "could not set system locale to $1 (got '$now'); is adb root allowed?" >&2; return 1; }
}
restore_locale() {
  if [ "$(adb shell getprop persist.sys.locale | tr -d '\r')" != "$ORIG_LOCALE" ]; then
    echo "restoring system locale $ORIG_LOCALE"
    set_system_locale "$ORIG_LOCALE" || echo "WARNING: restore failed; run: adb root; adb shell setprop persist.sys.locale $ORIG_LOCALE; adb shell 'stop; start'" >&2
  fi
}
trap restore_locale EXIT

if [ "$ORIG_LOCALE" != "$TEST_LOCALE" ]; then set_system_locale "$TEST_LOCALE"; fi
echo "API $SDK, system locale forced to $TEST_LOCALE (original: $ORIG_LOCALE), app locale zh-Hant-TW"
FAIL=0
check() { if screen_has "$2"; then echo "PASS  $1"; else echo "FAIL  $1 (expected '$2')"; FAIL=1; fi; }

adb shell pm clear "$PKG" >/dev/null
# pm clear revokes runtime permissions; pre-grant them so permission dialogs (API 33+) do not cover the UI.
for perm in POST_NOTIFICATIONS READ_MEDIA_AUDIO READ_MEDIA_IMAGES READ_MEDIA_VIDEO READ_EXTERNAL_STORAGE WRITE_EXTERNAL_STORAGE; do
  adb shell pm grant "$PKG" "android.permission.$perm" >/dev/null 2>&1 || true
done
if [ "$SDK" -ge 33 ]; then
  adb shell cmd locale set-app-locales "$PKG" --locales zh-Hant-TW
else
  # /data/local/tmp is not traversable by the app UID, so stream the prefs file through run-as stdin.
  adb shell am force-stop "$PKG"
  TMP=$(mktemp)
  printf '%s\n' "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>" "<map>" \
    '    <string name="Locale">zh-Hant-TW</string>' "</map>" > "$TMP"
  adb shell "run-as $PKG sh -c 'mkdir -p shared_prefs && cat > shared_prefs/Settings.xml'" < "$TMP"
  rm -f "$TMP"
  adb shell run-as "$PKG" cat shared_prefs/Settings.xml | grep -q 'zh-Hant-TW' \
    || { echo "failed to write shared_prefs/Settings.xml via run-as" >&2; exit 1; }
fi

adb shell am start -W -S -n "$PKG/.MainActivity" >/dev/null || true
# After pm clear the app shows its change-history dialog on first launch; dismiss it so it does not cover the UI.
wait_screen "id/btn_search|應用程式變更歷史" 60 || true
wait_screen "應用程式變更歷史" 30 || true # the dialog appears a few seconds after the main screen
for _ in 1 2 3 4 5; do
  screen_has "應用程式變更歷史" || break
  tap_node 'resource-id="android:id/button1"' || true
  sleep 2
done
wait_screen "id/btn_search" 30 || true
check "main screen after idle" "內容搜尋"

tap_id n1
tap_id bs_db
wait_screen "button_ts" 60 || true
long_press_center
if wait_screen "播放條|播放条|Playback|Payback" 10; then
  check "lyrics context menu" "顯示/隱藏播放條"
  adb shell input keyevent KEYCODE_BACK
  sleep 2
else
  echo "SKIP  lyrics context menu (long press did not open it)"
fi
adb shell input keyevent KEYCODE_BACK
wait_screen "內容搜尋" 30 || true
check "main screen after a lyrics WebView" "內容搜尋"

adb shell input keyevent KEYCODE_MENU
sleep 2
screen_has "主題|主题|Theme" || tap_desc "More options" || tap_desc "更多選項" || tap_desc "更多选项" || true
sleep 2
check "main overflow menu" "應用介面主題"
adb shell input keyevent KEYCODE_BACK

[ "$FAIL" = 0 ] && echo "RESULT: PASS" || { echo "RESULT: FAIL"; exit 1; }
