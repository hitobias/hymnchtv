# 測試政策

## 選測腳本

`tools/affected-tests.sh [--base <ref>] [--run <serial>] [--quick]`

- 依 `git diff <base>...HEAD`、未提交變更與未追蹤檔案，列出受影響的 instrumented 測試類別（完整類別名）。
- 輸出 `ALL` 代表動到共用行為（AndroidManifest、gradle、theme/style/color），需跑全套；無輸出代表沒有對應測試。
- `--run <serial>`：以 `am instrument` 分批（每批 ≤ 25 個類別）執行；結果為 `ALL` 時改跑快速套件。
- `--quick`：另外跑快速套件（`-e annotation org.cog.hymnchtv.QuickTest`）。
- 比對為字詞搜尋，可能多選，不會漏掉直接引用；啟發式規則寫在腳本檔頭。

## PR

1. 跑 `tools/affected-tests.sh --run <API 34 serial> --quick`：受影響測試 + 快速套件，皆在 API 34。
2. 動到共用行為（預設值、工具列、播放器、主題、manifest；腳本印出 `ALL`）：合併前在 API 34 跑全套。

## 發版

1. 在最終整合 commit 上，兩個模擬器各完整跑一次：API 34 一次跑完；API 24 分 4 片
   （`-e numShards 4 -e shardIndex N`，N = 0..3）。
2. 安裝 RELEASE apk，在 API 24 與 API 34 手動冒煙測試：啟動、splash、桌面圖示。
3. 之後若有修正：只重跑失敗的測試 + 受影響測試（腳本）。

## 測試基礎

- 每次 instrumented 執行開始前，`HymnalTestListener`（由 `hymnchtv/src/androidTest/AndroidManifest.xml` 的
  meta-data `listener` 註冊，runner 仍是 `androidx.test.runner.AndroidJUnitRunner`）把預設 SharedPreferences 的
  `ckChangeLog_last_version_code` 設成目前 versionCode，「變更歷史」對話框不會在測試中跳出。
- 要測變更歷史對話框的測試：開始時 `ChangeLogSeen.clear(ctx)`，結束時（`finally`／`@After`）`ChangeLogSeen.mark(ctx)`。

## 模擬器

啟動（API 34 埠 5580；API 24 用 `-avd api24b -port 5582`）：

```
nohup /opt/homebrew/share/android-commandlinetools/emulator/emulator -avd api34b -port 5580 -no-snapshot-save -no-boot-anim -no-audio > /dev/null 2>&1 &
```

開機後把 window / transition / animator scale 設為 0：

```
for k in window_animation_scale transition_animation_scale animator_duration_scale; do
  adb -s emulator-5580 shell settings put global $k 0
done
```

安裝（serial 為 `emulator-5580` / `emulator-5582`）：

```
ANDROID_SERIAL=<serial> ./gradlew :hymnchtv:installDebug :hymnchtv:installDebugAndroidTest --max-workers=2
```

執行全套（範例，API 24 第 N 片）：

```
adb -s <serial> shell am instrument -w -e numShards 4 -e shardIndex N \
  com.ziontkec.hymnal.test/androidx.test.runner.AndroidJUnitRunner
```

## 禁止事項

- 不用 `connectedDebugAndroidTest`：它會在結束時解除安裝 app。
- 不執行 `./gradlew --stop`。
- 測完關閉模擬器：`adb -s <serial> emu kill`。
