# 子項目 B（第一波）：資料庫與啟動 實作計畫

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

## 給執行者（Sonnet 5.5）的說明

這份計畫由 **Sonnet 5.5（`claude-sonnet-5-5`）** 子代理執行，分成 3 條可以平行的 lane，每條 lane 在自己的 git worktree 裡工作。建議的啟動方式：

1. 在 repo 根目錄 `/Users/hitobias/orca/hymnchtv` 開一個新的 Claude Code session（協調者，Opus 或 Sonnet 都可以）。
2. 輸入以下指示：

   > 使用 superpowers:subagent-driven-development 執行 `docs/superpowers/plans/2026-10-02-b-data-startup-implementation.md`。Task 0、Task 1 由你依序在整合 worktree 完成；之後把 Lane A、Lane B、Lane C 各派給一個 Sonnet 5.5 子代理，各自在自己的 worktree 平行執行；三條 lane 都完成後，你做 Task F。遇到計畫和程式碼對不上就停下來問我，不要自行猜測。

**執行順序與依賴：**

```
Task 0（基準與工具）→ Task 1（共用基礎設施）
        ├─ Lane A：A1 → A2 → A3   （DatabaseBackend）
        ├─ Lane B：B1 → B2        （MediaConfig / NotionRecord / QQRecord）
        └─ Lane C：C1 → C2 → C3 → C4（MainActivity / HymnsApp / UpdateServiceImpl）
→ Task F（合併、實機驗證、量測、審查）
```

- 三條 lane **彼此之間**修改的檔案不重疊（見「檔案結構」），lane 之間合併不應該有衝突。**和子項目 A2 之間則預期會有衝突**（`MainActivity.java`，可能還有 `HymnsApp.java`），整合順序與驗收方式見 Task F。
- lane 內的 task 依序執行（同一條 lane 會改同一個檔案）。

**執行規則：**
- **計畫和程式碼對不上就停下來**：行號可能已經偏移，請用計畫中引用的原始程式碼文字定位；找不到原文就停下來回報，不要自己改計畫的意圖。
- **每個 task 結束前**都要跑：`./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug`，結果必須是 `BUILD SUCCESSFUL`。
- **模擬器是共用資源**：另一個代理可能同時在用模擬器（例如子項目 A2 的驗證）。所有會安裝 APK 或跑 instrumented test 的指令，一律用 `tools/perf/emu_lock.sh` 包起來（Task 0 建立），同一時間只有一個 worktree 使用模擬器。注意 `connectedDebugAndroidTest` 跑完會解除安裝 app，所以使用前後別人的 app 狀態都會被清掉，這是預期的。
- **裝置選擇**：一律用環境變數 `ANDROID_SERIAL` 指定裝置，例如 `export ANDROID_SERIAL=$(tools/perf/serial_for.sh api34)`。
- **不要改到第二波的檔案**：`ContentView.java`、`ContentHandler.java`、`MyPagerAdapter.java`、桌布相關程式碼（`MainActivity` 的 `initUserSettings`／桌布區塊、`WallPaperUtil`）都不能動，因為子項目 A2 正在平行修改。
- **量測只用 benchmark build**（Task 0 新增的 build type）；StrictMode 只在 debug build 開啟。

**Goal:**
- 大量匯入媒體連結（`url_import.txt` 2,696 筆、Notion、QQ）改成單一 transaction，而且不在主執行緒執行；畫面上的提示和現在一致（開始 toast、結果 toast、按鈕暫時停用）。
- `DatabaseBackend`：刪掉每次 `getWritableDatabase()` 都執行的 `PRAGMA foreign_keys`、開啟 WAL、歷史紀錄改用 `queryNumEntries` 計數、新增一次查出一首詩歌所有媒體類型的 `getMediaRecords(hymnType, hymnNo, isFu)`（呼叫端留給第二波）。
- 啟動：修掉 `handleIntent` 多呼叫的 `super.onStart()`；YB 對照表、清除舊 APK、寫入歷史紀錄移出主執行緒；更新服務的啟動延後到第一個畫面之後，並改用 `ProcessLifecycleOwner` 判斷前景（不再呼叫 `getRunningAppProcesses()`）。
- 建立量測協定與腳本（`tools/perf/`），前後數據寫進 `docs/perf/2026-10-02-b-data-startup-measurements.md`。

**Architecture:**
- Java 程式碼共用**一個**背景 executor：`org.cog.hymnchtv.concurrent.AppExecutors`（單一執行緒 `hymn-io`）。master plan B.2 原本建議放在 `HymnsApp.ioExecutor`；改成獨立的 Kotlin object，是為了讓三條 lane 不必同時修改 `HymnsApp.java`，而且 instrumented test 可以直接使用。同一個類別裡不混用 coroutine。
  - 選單一執行緒的理由：這一波的背景工作都很短（毫秒級到約 1 秒），排成一列可以讓 DB 寫入、匯入計數器（`NotionRecord`／`QQRecord` 的 static 計數）不需要額外的鎖。
  - 代價：一個長的匯入在跑時，後面排隊的工作（例如 YB 對照表預熱）要等。這是可以接受的，理由寫在 Task C2。
  - 背景工作**不持有任何 Activity 或 View**：只能捕捉 application context 和不可變的輸入；結果用 application context 的 toast 回報，或透過 process 層級的 `LiveData`，由畫面以 lifecycle-aware 的方式觀察（見 Task B1）。
  - 這一波沒有「把結果寫回某一頁」的 UI，所以不需要 request token；第二波（B-4、B-9b 呼叫端）才會用到。
- 判斷規則寫成 Kotlin 純函式並用 JVM 單元測試（`HistoryPrune`、`YbCrossRef.parse`、`LazyMap`）；資料庫行為用 instrumented test，資料庫一律用 `DatabaseBackend.createForTest(context, name)` 建立的獨立檔案，**不碰 app 的真實資料庫**。
- 全新項目、沒有舊使用者；WAL 和 journal 設定不屬於 schema 變更，`DATABASE_VERSION` 維持 5。

**Tech Stack:** Android（minSdk 24、compileSdk 37、AGP 9.3.3 內建 Kotlin）、Java 11 與 Kotlin 混用、JUnit 4.13.2、Truth 1.4.5、AndroidX Test（runner/core 1.7.0、ext-junit 1.3.0）、SQLite（`SQLiteOpenHelper`）、`adb`／`uiautomator`／bash。

**規格來源:** `docs/superpowers/plans/2026-10-02-hymnchtv-modernization-plan.md` 的「子項目 B」：B.1 的 B-7，使用者新增的 B-9a～B-9e，B.2 的背景工作規則，B.3 第 1 項（量測協定）。

**不在這一波（第二波再做）：**
- B-1、B-2、B-4、B-5、B-6、B-8、B-10、B-11，以及 B-9b 的呼叫端（`ContentHandler.getHymnMediaState`）。理由：這些都會改到 `ContentView`／`ContentHandler`／`MainActivity` 桌布區塊，A2 正在平行修改這些地方。B-9b 呼叫端的改法寫在附錄 A，第二波直接套用。
- **B-3（延後預建 WebView）**：Codex 審查（P1）指出，`am start -W` 的 TotalTime 無法證明這個改動有好處，因為成本只是被搬到第一個畫面之後。第二波要用下列指標重新設計，才能決定是否採用（記在附錄 B）：
  1. **time-to-first-interaction**：冷啟動後立刻開歌詞頁（腳本在第一個畫面出現後馬上點擊），量從啟動到歌詞頁 `Displayed` 的時間。
  2. **首幀之後的卡頓**：同一個流程期間用 `adb shell dumpsys gfxinfo org.cog.hymnchtv framestats` 收集幀資料，比較 janky frames 與第 90／99 百分位的幀時間。
  3. **競爭情境**：在延後的 WebView 預建執行前就打開歌詞頁（歌詞頁的 WebView 成為第一個 WebView），驗證語系仍然正確、時間沒有變差。
  4. 語系回歸：`tools/perf/locale_webview_check.sh` 在 api24 和 api34 都要通過。
  這一波保留 `HymnsApp.onCreate` 原本的 `new WebView(this).destroy()`，完全不動。

**分支與 worktree:**
- 整合分支：`perf/b-data-startup`，從 `feat/zh-hant` 開出（如果開工時 A 已經合併進 `master`，改從 `master` 開出，並在 Task 0 Step 1 記錄實際的 base commit）。
- lane 分支：`perf/b-lane-db`、`perf/b-lane-import`、`perf/b-lane-startup`，都從 Task 1 完成後的 `perf/b-data-startup` 開出。

**Commit 規則:**
- conventional commits（`feat:`、`fix:`、`perf:`、`test:`、`chore:`、`docs:`）。
- 每個 commit 訊息結尾加上：

  ```
  Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>
  ```

**和 spec 不同的地方（刻意的取捨）:**
- **executor 不放在 `HymnsApp`**：理由見 Architecture。
- **B-9b 只做 DB API**：呼叫端只要改兩行，但 B-4 會把整個 `getHymnMediaState` 搬到背景，到時候整段會重寫；而且 `ContentHandler` 正在被 A2 修改。現在改只會製造合併衝突。
- **YB 對照表（`createYbXTable`）不是單純「搬到背景」**，而是改成「第一次使用時才載入（thread-safe），啟動時在背景預熱」。單純搬到背景會產生競爭：使用者在載入完成前開啟 YB 詩歌會讀到空表。另外這也順便修好一個既有 bug：process 被回收後直接還原到 `ContentHandler` 時，`MainActivity.onCreate` 沒有執行，原本的表是空的。
- **C3 不再呼叫 `getRunningAppProcesses()`**：改用 `ProcessLifecycleOwner` 在主執行緒上判斷前景（`get(0)` 不保證是自己的 process）。
- **匯入的量測用 instrumented test**（debug build），不用 benchmark build。匯入的成本幾乎都在 SQLite 的 commit／fsync，和 ART 是否 debuggable 關係很小；而且同一個測試可以同時量「舊的逐筆 commit」和「新的單一 transaction」，數字可以直接比較。

---

## 檔案結構

**新增（Kotlin，純邏輯，有 JVM 單元測試）：**

| 檔案 | 職責 |
|---|---|
| `hymnchtv/src/main/java/org/cog/hymnchtv/persistance/HistoryPrune.kt` | 歷史紀錄超過上限時，算出要保留的最舊一筆的位置（維持舊行為） |
| `hymnchtv/src/main/java/org/cog/hymnchtv/toc/YbCrossRef.kt` | 解析 `toc_yb_toc.txt`；lazy、thread-safe 的 YB 對照表 |
| `hymnchtv/src/main/java/org/cog/hymnchtv/toc/LazyMap.kt` | 第一次存取時才載入內容的唯讀 `Map` |
| `hymnchtv/src/main/java/org/cog/hymnchtv/mediaconfig/ImportResult.kt` | 匯入結果（成功筆數／有效行數），immutable data class |

**新增（Kotlin，Android 邊界，靠 instrumented test 或手動驗證）：**

| 檔案 | 職責 |
|---|---|
| `hymnchtv/src/main/java/org/cog/hymnchtv/concurrent/AppExecutors.kt` | Java 共用的背景 executor 與主執行緒 handler |
| `hymnchtv/src/main/java/org/cog/hymnchtv/perf/DebugStrictMode.kt` | debug build 的 StrictMode（只 log，不當機） |

**測試：**
- JVM：`hymnchtv/src/test/java/org/cog/hymnchtv/persistance/HistoryPruneTest.kt`、`hymnchtv/src/test/java/org/cog/hymnchtv/toc/YbCrossRefTest.kt`、`hymnchtv/src/test/java/org/cog/hymnchtv/toc/LazyMapTest.kt`
- instrumented（`hymnchtv/src/androidTest/java/org/cog/hymnchtv/…`）：
  - `concurrent/AppExecutorsTest.kt`
  - `persistance/DatabaseBackendTransactionTest.kt`
  - `persistance/DatabaseBackendConfigTest.kt`
  - `persistance/HymnHistoryStoreTest.kt`
  - `persistance/MediaRecordsQueryTest.kt`
  - `mediaconfig/ImportPerfTest.kt`
  - `mediaconfig/UrlImportTest.kt`
  - `mediaconfig/UrlImportJobTest.kt`
  - `mediaconfig/NotionStoreTest.kt`
  - `mediaconfig/QQStoreTest.kt`
  - `toc/YbCrossRefAssetTest.kt`

**修改（依 lane 分）：**

| Lane | 檔案 | 修改內容 |
|---|---|---|
| Task 0 | `hymnchtv/build.gradle` | 新增 `benchmark` build type |
| Task 1 | `hymnchtv/src/main/java/org/cog/hymnchtv/HymnsApp.java` | debug 時安裝 StrictMode |
| Task 1 | `hymnchtv/src/main/java/org/cog/hymnchtv/persistance/DatabaseBackend.java` | `createForTest`、`inTransaction`、`runInTransaction`、`storeMediaRecordOrThrow` |
| A | `…/persistance/DatabaseBackend.java` | B-9c、B-9d、B-9e、B-9b |
| B | `…/mediaconfig/MediaConfig.java` | 匯入改成單一 transaction + 背景執行；process 層級的 `urlImportRunning` LiveData，畫面以 lifecycle-aware 方式觀察 |
| B | `…/mediaconfig/NotionRecord.java`、`…/mediaconfig/QQRecord.java` | 解析與寫入分開；DB 錯誤往外丟、整頁 rollback；每頁連結單一 transaction + 背景執行 |
| C | `…/MainActivity.java` | `handleIntent`、YB 對照表、`showContent` 的歷史寫入（**A2 也在改這個檔案**，見 Task F 的合併說明） |
| C | `…/HymnsApp.java` | 清除舊 APK 移到背景；更新服務延後到第一個畫面之後、改用 `ProcessLifecycleOwner` 判斷前景 |
| C | `…/service/androidupdate/UpdateServiceImpl.java` | `getInstance()` 加上 `synchronized` |

**工具與文件：**
- `tools/perf/emu_lock.sh`、`tools/perf/serial_for.sh`、`tools/perf/ui.sh`、`tools/perf/install_apk.sh`
- `tools/perf/cold_start.sh`、`tools/perf/strictmode_report.sh`、`tools/perf/locale_webview_check.sh`、`tools/perf/import_perf.sh`
- `docs/perf/2026-10-02-b-data-startup-measurements.md`

---

## 量測協定（B.3 第 1 項，這一波適用的部分）

| 項目 | 定義 |
|---|---|
| 基準裝置 | 主要：模擬器 `api34`（`system-images;android-34;google_apis;arm64-v8a`，Apple Silicon 主機）。次要：`api24`，只記錄數字，不做為驗收門檻。有實體舊手機時可以另外記錄一組，在文件中註明型號與 Android 版本。模擬器的數字只能拿來做**同一台主機上的前後比較**，不能代表實機絕對值。 |
| build type | 冷啟動：`benchmark`（`initWith release`、不可 debug、用 debug key 簽章、`minifyEnabled false`，和 release 相同）。匯入：`ImportPerfTest`（debug，理由見上）。StrictMode：debug。 |
| 資料集 | 冷啟動：已完成首次啟動（資料庫已建立）的狀態，啟動 `MainActivity`，不帶 intent extras。匯入：完整的 `assets/url_import.txt`（2,696 行），寫入空白的測試資料庫、`isOverWrite=false`。 |
| 重複次數 | 冷啟動：先跑 2 次暖機（不計），再跑 15 次，取中位數，並記錄 Q1、Q3、最小、最大。匯入：每條路徑 1 次暖機 + 5 次，取中位數。 |
| 散熱與負載 | 每組量測前讓模擬器閒置 60 秒；API 29 以上用 `dumpsys thermalservice` 確認 `Thermal Status: 0`。量測時主機不能有其他 Gradle 建置或模擬器測試在跑（`emu_lock.sh` 只保證模擬器不被搶，Gradle 要自己確認：`pgrep -fl GradleWorkerMain` 沒有輸出）。主機接電源。 |
| 誤差範圍 | 冷啟動的雜訊帶 = max(10 ms, 中位數的 5%)。如果 IQR（Q3−Q1）超過中位數的 20%，整組作廢重跑。差異落在雜訊帶內，視為「沒有變化」。 |
| 預期改善（看到基準值後在 Task 1 確認或修正） | 匯入：單一 transaction 的中位數比逐筆 commit **快 5 倍以上**；低於 3 倍就停下來回報。冷啟動：C2～C4 預期在雜訊帶內（工作很小，主要目的是不在主執行緒做 IPC／磁碟）；整體驗收是「沒有超出雜訊帶的退步」。 |
| 冷啟動的定義 | `cold_start.sh` 量的是 **process-cold, cache-warm**：每次都殺掉 process（`am start -S`），但 page cache、ART 編譯產物、WebView provider 都是熱的（沒有清快取、沒有重開機）。它代表「使用者剛用過、process 被回收後再開」，**不代表**開機後第一次啟動。文件與腳本輸出都用這個名稱。 |
| 不用 TTID 衡量的項目 | `am start -W` 的 TotalTime 只量到第一個畫面。把工作「延後到第一個畫面之後」一定會讓 TotalTime 變好看，卻可能只是把成本搬到使用者第一次操作的時候。因此這一波**不做**任何「延後到首幀之後」的取捨評估（B-3 移到第二波，見下）；C3 的延後只涉及一次非同步的 `startService`，不影響 UI。 |
| StrictMode 驗收 | 不要求零違規。只要求這一波處理、而且在 before 報告中出現過的呼叫點，在 after 報告中**不再出現**：`DatabaseBackend.storeHymnHistory`（從 `showContent` 觸發，一定會出現）、`UpdateServiceImpl.removeOldDownloads`／`getStore`（SharedPreferences 首次載入，通常會出現）。注意：StrictMode 只攔得到經過 BlockGuard 的檔案與資料庫 IO，**讀 assets（`AssetManager.open`）不會被抓到**，所以 `createYbXTable`、`HistoryRecord` 讀標題這類 asset 讀取，以及使用者手動觸發的匯入，改用程式碼審查（確認在 `AppExecutors.io` 裡執行）加上手動測試驗收。其他違規（例如 `ContentHandler`）列在文件中，留給第二波。 |

---

### Task 0：整合 worktree、benchmark build type、量測腳本與啟動基準（process-cold, cache-warm）

**Files:**
- Modify: `hymnchtv/build.gradle`（`buildTypes { ... }` 區塊）
- Create: `tools/perf/emu_lock.sh`、`tools/perf/serial_for.sh`、`tools/perf/ui.sh`、`tools/perf/install_apk.sh`、`tools/perf/cold_start.sh`、`tools/perf/strictmode_report.sh`、`tools/perf/locale_webview_check.sh`
- Create: `docs/perf/2026-10-02-b-data-startup-measurements.md`

**Prerequisites:** 子項目 A 的 Task 0 已完成（gradle wrapper、`local.properties`、AVD `api24`／`api34` 都存在）。

- [ ] **Step 1：建立整合 worktree**

  ```bash
  cd /Users/hitobias/orca/hymnchtv
  git fetch --all --prune
  git worktree add -b perf/b-data-startup ../hymnchtv-b feat/zh-hant
  cp local.properties ../hymnchtv-b/
  cd ../hymnchtv-b
  git log --oneline -1
  ./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug --console=plain | tail -3
  ```

  Expected: 最後一行是 `BUILD SUCCESSFUL`。把 `git log --oneline -1` 印出的 base commit 記下來，Step 9 要寫進量測文件。

  之後 Task 0、Task 1、Task F 的所有指令都在 `/Users/hitobias/orca/hymnchtv-b` 執行。

- [ ] **Step 2：新增 benchmark build type**

  在 `hymnchtv/build.gradle` 的 `buildTypes { ... }` 裡，`debug { ... }` 區塊之後（`buildTypes` 的右大括號之前）加入：

  ```groovy
          // Startup measurements only (plan B.3-1): release configuration, not debuggable, debug-signed so it installs anywhere.
          benchmark {
              initWith release
              signingConfig = signingConfigs.debug
              debuggable = false
              matchingFallbacks = ['release']
          }
  ```

  Run: `./gradlew :hymnchtv:assembleBenchmark --console=plain | tail -1 && ls hymnchtv/build/outputs/apk/benchmark/`
  Expected: `BUILD SUCCESSFUL`，並列出 `hymnchtv-benchmark.apk`。

- [ ] **Step 3：模擬器鎖與裝置選擇** `tools/perf/emu_lock.sh`

  ```bash
  #!/usr/bin/env bash
  # Usage: tools/perf/emu_lock.sh <command...>
  # Serialises emulator use (installs, instrumented tests, measurements) across all worktrees of this repo.
  set -euo pipefail
  LOCK="$(git rev-parse --git-common-dir)/hymnchtv-emulator.lock"
  until mkdir "$LOCK" 2>/dev/null; do
    echo "waiting for emulator lock $LOCK (held by: $(cat "$LOCK/owner" 2>/dev/null || echo unknown))" >&2
    sleep 10
  done
  echo "$$ $(pwd) $(date '+%H:%M:%S')" > "$LOCK/owner"
  trap 'rm -rf "$LOCK"' EXIT
  "$@"
  ```

  `tools/perf/serial_for.sh`：

  ```bash
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
  ```

  如果鎖因為程序被強制結束而殘留，確認沒有人在用模擬器後，手動執行 `rm -rf "$(git rev-parse --git-common-dir)/hymnchtv-emulator.lock"`。

- [ ] **Step 4：UI 輔助函式** `tools/perf/ui.sh`（給其他腳本 `source`）

  ```bash
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

  long_press_center() {
    local size w h
    size=$(adb shell wm size | tr -d '\r' | awk '{print $NF}')
    w=${size%x*}; h=${size#*x}
    adb shell input swipe $((w / 2)) $((h / 2)) $((w / 2)) $((h / 2)) 1200
  }
  ```

- [ ] **Step 5：安裝腳本** `tools/perf/install_apk.sh`

  ```bash
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
  ```

- [ ] **Step 6：冷啟動腳本** `tools/perf/cold_start.sh`

  ```bash
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
  ```

- [ ] **Step 7：StrictMode 報告腳本** `tools/perf/strictmode_report.sh`

  ```bash
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
  ```

- [ ] **Step 8：語系檢查腳本** `tools/perf/locale_webview_check.sh`

  這個腳本驗證「第一個 WebView 建立之後，介面語言沒有被重設成系統語言」。腳本會**先把系統語言強制設成英文**（需要 `adb root`，`google_apis` 模擬器映像可以），app 設成繁中，所以畫面上必須出現繁中字串；如果系統語言本來就是中文，WebView 重設語言時可能誤判為通過，所以不能沿用模擬器原本的語言。結束時（包括失敗）會還原原本的系統語言。改系統語言會讓 framework 軟重啟，每次多花約 1 分鐘。

  ```bash
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

  adb shell am start -W -S -n "$PKG/.MainActivity" >/dev/null
  sleep 5
  check "main screen after idle" "內容搜尋"

  tap_id n1
  tap_id bs_db
  sleep 4
  long_press_center
  sleep 1
  if screen_has "播放條|播放条|Playback|Payback"; then
    check "lyrics context menu" "顯示/隱藏播放條"
    adb shell input keyevent KEYCODE_BACK
  else
    echo "SKIP  lyrics context menu (long press did not open it)"
  fi
  adb shell input keyevent KEYCODE_BACK
  sleep 2
  check "main screen after a lyrics WebView" "內容搜尋"

  adb shell input keyevent KEYCODE_MENU
  sleep 1
  screen_has "主題|主题|Theme" || tap_desc "More options" || tap_desc "更多選項" || tap_desc "更多选项" || true
  sleep 1
  check "main overflow menu" "應用介面主題"
  adb shell input keyevent KEYCODE_BACK

  [ "$FAIL" = 0 ] && echo "RESULT: PASS" || { echo "RESULT: FAIL"; exit 1; }
  ```

  ```bash
  chmod +x tools/perf/*.sh
  bash -n tools/perf/*.sh && echo syntax-ok
  ```

  Expected: `syntax-ok`。

- [ ] **Step 9：建立量測文件** `docs/perf/2026-10-02-b-data-startup-measurements.md`

  ````markdown
  # 子項目 B（第一波：資料庫與啟動）量測紀錄

  協定見 `docs/superpowers/plans/2026-10-02-b-data-startup-implementation.md` 的「量測協定」。
  每一列都由 `tools/perf/` 的腳本輸出，直接貼上，不手算。

  ## 環境

  | 項目 | 值 |
  |---|---|
  | base commit | （Task 0 Step 1 記下的 commit） |
  | 主機 | （`sysctl -n machdep.cpu.brand_string`，以及記憶體大小） |
  | 模擬器 | api34：`system-images;android-34;google_apis;arm64-v8a`；api24：`system-images;android-24;google_apis;arm64-v8a` |
  | 實體裝置 | （沒有就寫「無」） |

  ## 啟動 TTID：process-cold, cache-warm（ms，benchmark build，`am start -W` TotalTime）

  每次都殺 process，但快取是熱的；只量到第一個畫面。不代表開機後第一次啟動，也看不出被延後到首幀之後的成本。

  | 標籤 | API | n | 中位數 | Q1 | Q3 | 最小 | 最大 | commit |
  |---|---|---|---|---|---|---|---|---|

  ## 匯入 url_import.txt（2,696 行，ms）

  | 標籤 | API | 路徑 | 中位數 | 各次 | imported |
  |---|---|---|---|---|---|

  ## StrictMode（debug build，只計 org.cog.hymnchtv 的呼叫點）

  ### before
  | 次數 | 類型 @ 最內層 app frame |
  |---|---|

  ### after
  | 次數 | 類型 @ 最內層 app frame |
  |---|---|

  ## 語系檢查（WebView）

  | 標籤 | API | 結果 |
  |---|---|---|

  ## 決策紀錄
  ````

  把 Step 1 的 base commit、`sysctl -n machdep.cpu.brand_string` 的結果填進「環境」表（這兩格是實際值，不是之後才填的空格）。

- [ ] **Step 10：量測冷啟動基準與語系檢查基準**

  ```bash
  # api34 已經在跑就略過這一行（用 serial_for.sh 確認）
  /opt/homebrew/share/android-commandlinetools/emulator/emulator -avd api34 -no-snapshot-save &
  export ANDROID_SERIAL=$(tools/perf/serial_for.sh api34)
  tools/perf/emu_lock.sh tools/perf/install_apk.sh benchmark
  sleep 60
  tools/perf/emu_lock.sh tools/perf/cold_start.sh baseline
  tools/perf/emu_lock.sh tools/perf/install_apk.sh debug
  tools/perf/emu_lock.sh tools/perf/locale_webview_check.sh
  ```

  Expected:
  - `cold_start.sh` 印出一列 `| baseline | 34 | 15 | … |`。如果 IQR 超過中位數的 20%，重跑。
  - `locale_webview_check.sh` 最後一行是 `RESULT: PASS`（目前的程式碼本來就有 WebView 預建，這一步確認腳本本身是對的）。如果是 `FAIL`，**停下來回報**，不要往下做；代表腳本的假設有錯，Task F 的語系回歸檢查（以及第二波 B-3 的驗收）會失去意義。

  再對 `api24` 做一次（`export ANDROID_SERIAL=$(tools/perf/serial_for.sh api24)`，重複上面四個指令）。

  把輸出貼進量測文件：冷啟動兩列；語系檢查兩列，標籤寫 `baseline`。

- [ ] **Step 11：Commit**

  ```bash
  git add hymnchtv/build.gradle tools/perf docs/perf
  git commit -m "chore: add benchmark build type, perf scripts and startup baseline

  Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

### Task 1：共用基礎設施（AppExecutors、StrictMode、測試用資料庫、transaction API）與匯入／StrictMode 基準

**Files:**
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/concurrent/AppExecutors.kt`
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/perf/DebugStrictMode.kt`
- Modify: `hymnchtv/src/main/java/org/cog/hymnchtv/HymnsApp.java`（`onCreate` 開頭）
- Modify: `hymnchtv/src/main/java/org/cog/hymnchtv/persistance/DatabaseBackend.java`（建構子、新增三個方法）
- Create: `hymnchtv/src/androidTest/java/org/cog/hymnchtv/concurrent/AppExecutorsTest.kt`
- Create: `hymnchtv/src/androidTest/java/org/cog/hymnchtv/persistance/DatabaseBackendTransactionTest.kt`
- Create: `hymnchtv/src/androidTest/java/org/cog/hymnchtv/mediaconfig/ImportPerfTest.kt`
- Create: `tools/perf/import_perf.sh`

**Prerequisites:** Task 0。

- [ ] **Step 1：寫會失敗的 instrumented test** `hymnchtv/src/androidTest/java/org/cog/hymnchtv/concurrent/AppExecutorsTest.kt`

  ```kotlin
  package org.cog.hymnchtv.concurrent

  import android.os.Looper
  import androidx.test.ext.junit.runners.AndroidJUnit4
  import com.google.common.truth.Truth.assertThat
  import org.junit.Test
  import org.junit.runner.RunWith
  import java.util.concurrent.CountDownLatch
  import java.util.concurrent.TimeUnit
  import java.util.concurrent.atomic.AtomicBoolean
  import java.util.concurrent.atomic.AtomicReference

  @RunWith(AndroidJUnit4::class)
  class AppExecutorsTest {
      @Test
      fun runsOffTheMainThreadOnTheSharedIoThread() {
          val done = CountDownLatch(1)
          val threadName = AtomicReference<String>()
          val onMain = AtomicBoolean(true)
          AppExecutors.io("test") {
              threadName.set(Thread.currentThread().name)
              onMain.set(Looper.myLooper() == Looper.getMainLooper())
              done.countDown()
          }
          assertThat(done.await(5, TimeUnit.SECONDS)).isTrue()
          assertThat(threadName.get()).isEqualTo("hymn-io")
          assertThat(onMain.get()).isFalse()
      }

      @Test
      fun aFailingTaskDoesNotStopLaterTasks() {
          AppExecutors.io("boom") { throw IllegalStateException("expected in test") }
          val done = CountDownLatch(1)
          AppExecutors.io("after") { done.countDown() }
          assertThat(done.await(5, TimeUnit.SECONDS)).isTrue()
      }
  }
  ```

- [ ] **Step 2：寫會失敗的 instrumented test** `hymnchtv/src/androidTest/java/org/cog/hymnchtv/persistance/DatabaseBackendTransactionTest.kt`

  ```kotlin
  package org.cog.hymnchtv.persistance

  import android.database.DatabaseUtils
  import androidx.test.ext.junit.runners.AndroidJUnit4
  import androidx.test.platform.app.InstrumentationRegistry
  import com.google.common.truth.Truth.assertThat
  import org.cog.hymnchtv.MainActivity
  import org.cog.hymnchtv.MediaType
  import org.cog.hymnchtv.mediaconfig.MediaRecord
  import org.junit.After
  import org.junit.Assert.assertThrows
  import org.junit.Before
  import org.junit.Test
  import org.junit.runner.RunWith

  @RunWith(AndroidJUnit4::class)
  class DatabaseBackendTransactionTest {
      private val ctx = InstrumentationRegistry.getInstrumentation().targetContext
      private lateinit var db: DatabaseBackend

      @Before
      fun setUp() {
          ctx.deleteDatabase(NAME)
          db = DatabaseBackend.createForTest(ctx, NAME)
      }

      @After
      fun tearDown() {
          db.close()
          ctx.deleteDatabase(NAME)
      }

      private fun record(no: Int) =
          MediaRecord(MainActivity.HYMN_DB, no, false, MediaType.HYMN_MEDIA, "https://example.org/$no", null)

      private fun rows() = DatabaseUtils.queryNumEntries(db.readableDatabase, MainActivity.HYMN_DB)

      @Test
      fun commitsEveryWriteOfTheBody() {
          db.runInTransaction {
              db.storeMediaRecord(record(1))
              db.storeMediaRecord(record(2))
          }
          assertThat(rows()).isEqualTo(2L)
      }

      @Test
      fun rollsBackWhenTheBodyThrows() {
          val error = assertThrows(IllegalStateException::class.java) {
              db.runInTransaction {
                  db.storeMediaRecord(record(1))
                  throw IllegalStateException("boom")
              }
          }
          assertThat(error).hasMessageThat().isEqualTo("boom")
          assertThat(rows()).isEqualTo(0L)
      }

      @Test
      fun inTransactionReturnsTheBodyResult() {
          assertThat(db.inTransaction { 42 }).isEqualTo(42)
      }

      @Test
      fun testDatabaseIsSeparateFromTheAppDatabase() {
          assertThat(db.databaseName).isEqualTo(NAME)
      }

      /** Nested success: the inner transaction joins the outer one and both writes commit together. */
      @Test
      fun nestedSuccessCommitsBothWrites() {
          db.runInTransaction {
              db.storeMediaRecord(record(1))
              db.runInTransaction { db.storeMediaRecord(record(2)) }
          }
          assertThat(rows()).isEqualTo(2L)
      }

      /**
       * Android SQLiteDatabase semantics: when a nested transaction ends without being marked successful,
       * the whole outer transaction rolls back, even if the outer body catches the error and returns normally.
       */
      @Test
      fun innerFailureRollsBackTheWholeOuterTransaction() {
          db.runInTransaction {
              db.storeMediaRecord(record(1))
              try {
                  db.runInTransaction {
                      db.storeMediaRecord(record(2))
                      throw IllegalStateException("inner")
                  }
              } catch (expected: IllegalStateException) {
                  // swallowed on purpose: the outer body still "succeeds"
              }
          }
          assertThat(rows()).isEqualTo(0L)
      }

      /** storeMediaRecord() swallows SQL errors (returns -1); the OrThrow variant must surface them. */
      @Test
      fun storeMediaRecordOrThrowSurfacesDatabaseErrors() {
          db.writableDatabase.execSQL("DROP TABLE ${MainActivity.HYMN_BB}")
          val bb = MediaRecord(MainActivity.HYMN_BB, 1, false, MediaType.HYMN_MEDIA, "https://example.org/bb", null)
          assertThrows(SQLException::class.java) { db.storeMediaRecordOrThrow(bb) }
          assertThat(db.storeMediaRecord(bb)).isEqualTo(-1L)
      }

      private companion object {
          const val NAME = "test-transaction.db"
      }
  }
  ```

  另外在檔案開頭的 import 區加入 `import android.database.SQLException`。

  如果 `innerFailureRollsBackTheWholeOuterTransaction` 在 api24 或 api34 上失敗（代表平台的巢狀 transaction 語意和上面描述的不同），**停下來回報**，不要改測試去配合；B1／B2 的 rollback 設計依賴這個語意。

- [ ] **Step 3：確認測試失敗（編譯失敗）**

  Run: `./gradlew :hymnchtv:compileDebugAndroidTestKotlin --console=plain 2>&1 | grep -E "Unresolved reference|BUILD" | head -5`
  Expected: 出現 `Unresolved reference 'AppExecutors'` 和 `Unresolved reference 'createForTest'`（或 `runInTransaction`），最後是 `BUILD FAILED`。

- [ ] **Step 4：實作** `hymnchtv/src/main/java/org/cog/hymnchtv/concurrent/AppExecutors.kt`

  ```kotlin
  package org.cog.hymnchtv.concurrent

  import android.os.Handler
  import android.os.Looper
  import android.os.Process
  import timber.log.Timber
  import java.util.concurrent.ExecutorService
  import java.util.concurrent.Executors

  /**
   * The one background executor shared by Java code (plan B.2). It is a single thread on purpose: this wave's
   * tasks are short (ms to ~1 s), and running them in order keeps DB writes and the importers' static
   * counters consistent without extra locks. Kotlin code uses lifecycleScope + Dispatchers.IO instead;
   * never mix both styles in one class.
   */
  object AppExecutors {
      private const val THREAD_NAME = "hymn-io"

      private val ioExecutor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
          Thread({
              Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
              runnable.run()
          }, THREAD_NAME).apply { isDaemon = true }
      }

      /** Main-thread handler for posting results back to the UI. */
      @JvmField
      val MAIN: Handler = Handler(Looper.getMainLooper())

      /**
       * Runs [task] on the shared IO thread. Callers handle their expected errors themselves; anything
       * unexpected is logged with [tag] so one bad task cannot kill the process or the thread.
       */
      @JvmStatic
      fun io(tag: String, task: Runnable) {
          ioExecutor.execute {
              try {
                  task.run()
              } catch (e: RuntimeException) {
                  Timber.e(e, "Background task '%s' failed", tag)
              }
          }
      }
  }
  ```

- [ ] **Step 5：實作** `hymnchtv/src/main/java/org/cog/hymnchtv/perf/DebugStrictMode.kt`

  ```kotlin
  package org.cog.hymnchtv.perf

  import android.os.StrictMode

  /** Debug builds only (plan B.3-1): log, never crash, on main-thread disk/network access and leaked DB objects. */
  object DebugStrictMode {
      @JvmStatic
      fun install() {
          StrictMode.setThreadPolicy(
              StrictMode.ThreadPolicy.Builder()
                  .detectDiskReads()
                  .detectDiskWrites()
                  .detectNetwork()
                  .detectCustomSlowCalls()
                  .penaltyLog()
                  .build()
          )
          StrictMode.setVmPolicy(
              StrictMode.VmPolicy.Builder()
                  .detectLeakedSqlLiteObjects()
                  .detectLeakedClosableObjects()
                  .penaltyLog()
                  .build()
          )
      }
  }
  ```

  在 `HymnsApp.java` 的 `onCreate()` 裡，把

  ```java
          TimberLogImpl.init();
  ```

  改成

  ```java
          TimberLogImpl.init();
          if (BuildConfig.DEBUG) {
              DebugStrictMode.install();
          }
  ```

  並在 import 區加入 `import org.cog.hymnchtv.perf.DebugStrictMode;`。

- [ ] **Step 6：在 `DatabaseBackend.java` 新增測試用建構方式與 transaction API**

  把

  ```java
      private DatabaseBackend(Context context) {
          super(context, DATABASE_NAME, null, DATABASE_VERSION);
      }
  ```

  改成

  ```java
      private DatabaseBackend(Context context) {
          this(context, DATABASE_NAME);
      }

      private DatabaseBackend(Context context, String name) {
          super(context, name, null, DATABASE_VERSION);
      }

      /**
       * A separate database file for instrumented tests and measurements; never touches dbHymnApp.db.
       * The caller closes it and deletes the file (Context#deleteDatabase).
       */
      @VisibleForTesting
      public static DatabaseBackend createForTest(Context context, String name) {
          return new DatabaseBackend(context, name);
      }

      /**
       * Run body in one transaction: committed when it returns, rolled back when it throws.
       * Nested calls join the outer transaction; if a nested body throws, the WHOLE outer transaction is
       * rolled back even when the outer body catches the exception (Android SQLiteDatabase semantics,
       * pinned by DatabaseBackendTransactionTest). Must not be called on the main thread for bulk work.
       */
      public <T> T inTransaction(@NonNull Supplier<T> body) {
          SQLiteDatabase db = getWritableDatabase();
          db.beginTransactionNonExclusive();
          try {
              T result = body.get();
              db.setTransactionSuccessful();
              return result;
          } finally {
              db.endTransaction();
          }
      }

      public void runInTransaction(@NonNull Runnable body) {
          inTransaction(() -> {
              body.run();
              return null;
          });
      }
  ```

  另外把 `storeMediaRecord` 拆成「組 ContentValues」和兩種寫入方式。把整個 `public long storeMediaRecord(MediaRecord mRecord) { ... }` 換成：

  ```java
      private static ContentValues toContentValues(MediaRecord mRecord) {
          ContentValues values = new ContentValues();
          values.put(MediaConfig.HYMN_NO, mRecord.getHymnNo());
          values.put(MediaConfig.HYMN_FU, mRecord.isFu());
          values.put(MediaConfig.MEDIA_TYPE, mRecord.getMediaType().toString());
          values.put(MediaConfig.MEDIA_URI, mRecord.getMediaUri());
          values.put(MediaConfig.MEDIA_FILE_PATH, mRecord.getMediaFilePath());
          return values;
      }

      /**
       * Save the given MediaRecord to the database table mRecord.getHymnType().
       * SQL errors are logged and reported as -1 (legacy behaviour, kept for single-record UI saves).
       */
      public long storeMediaRecord(MediaRecord mRecord) {
          long row = getWritableDatabase().insert(mRecord.getHymnType(), null, toContentValues(mRecord));
          if (row == -1) {
              Timber.e("### Error in creating media record for table:hymNo: %s:%s", mRecord.getHymnType(), mRecord.getHymnNo());
          }
          return row;
      }

      /**
       * Same as storeMediaRecord, but SQL errors propagate as android.database.SQLException.
       * Bulk imports use this inside inTransaction() so one failure rolls the whole batch back.
       */
      public long storeMediaRecordOrThrow(MediaRecord mRecord) {
          return getWritableDatabase().insertOrThrow(mRecord.getHymnType(), null, toContentValues(mRecord));
      }
  ```

  （注意：`getMediaUri()`／`getMediaFilePath()` 會把字串 `"null"` 轉成 null，和原本 `values.put(..., mRecord.getMediaUri())` 的行為相同，因為原本也是呼叫這兩個 getter。）

  import 區加入：

  ```java
  import androidx.annotation.NonNull;
  import androidx.annotation.VisibleForTesting;

  import java.util.function.Supplier;
  ```

- [ ] **Step 7：執行測試，確認通過**

  ```bash
  export ANDROID_SERIAL=$(tools/perf/serial_for.sh api34)
  tools/perf/emu_lock.sh ./gradlew :hymnchtv:connectedDebugAndroidTest --console=plain \
    -Pandroid.testInstrumentationRunnerArguments.class=org.cog.hymnchtv.concurrent.AppExecutorsTest,org.cog.hymnchtv.persistance.DatabaseBackendTransactionTest \
    | tail -3
  ```

  Expected: `BUILD SUCCESSFUL`；`hymnchtv/build/reports/androidTests/connected/debug/index.html` 顯示 9 個測試（AppExecutors 2、Transaction 7）、0 個失敗。也在 api24 跑一次同一個指令（`export ANDROID_SERIAL=$(tools/perf/serial_for.sh api24)`），確認巢狀 transaction 的語意在兩個 API 等級都相同。

- [ ] **Step 8：匯入量測測試（舊路徑基準）** `hymnchtv/src/androidTest/java/org/cog/hymnchtv/mediaconfig/ImportPerfTest.kt`

  這個測試完全照搬目前 `MediaConfig.importUrlRecords` 的邏輯（逐筆 auto-commit），用來量基準。Lane B 會在同一個檔案加上新路徑的量測。

  ```kotlin
  package org.cog.hymnchtv.mediaconfig

  import android.os.SystemClock
  import android.util.Log
  import androidx.test.ext.junit.runners.AndroidJUnit4
  import androidx.test.platform.app.InstrumentationRegistry
  import com.google.common.truth.Truth.assertThat
  import org.cog.hymnchtv.persistance.DatabaseBackend
  import org.cog.hymnchtv.utils.HymnNoValidate
  import org.junit.Test
  import org.junit.runner.RunWith

  /**
   * Measures importing the bundled url_import.txt (2,696 lines) into an empty test database.
   * Results go to logcat tag "ImportPerf"; tools/perf/import_perf.sh collects them.
   */
  @RunWith(AndroidJUnit4::class)
  class ImportPerfTest {
      private val ctx = InstrumentationRegistry.getInstrumentation().targetContext

      private val content: String by lazy {
          ctx.assets.open(ASSET).bufferedReader(Charsets.UTF_8).use { it.readText() }
      }

      private fun freshDb(name: String): DatabaseBackend {
          ctx.deleteDatabase(name)
          return DatabaseBackend.createForTest(ctx, name)
      }

      private fun dispose(db: DatabaseBackend, name: String) {
          db.close()
          ctx.deleteDatabase(name)
      }

      /** The pre-B-9a loop: one auto-commit per record, isOverWrite = false. */
      private fun legacyImport(db: DatabaseBackend, text: String): Int {
          var imported = 0
          for (line in text.split(Regex("\r\n|\n"))) {
              val record = try {
                  MediaRecord.toRecord(line)
              } catch (e: IllegalArgumentException) {
                  null
              } ?: continue
              val hymnNo = if (record.isFu()) record.hymnNo - HymnNoValidate.HYMN_DB_NO_MAX else record.hymnNo
              val nui = HymnNoValidate.validateHymnNo(record.hymnType, hymnNo, record.isFu())
              if (nui != -1 && !db.getMediaRecord(record, false) && db.storeMediaRecord(record) != -1L) {
                  imported++
              }
          }
          return imported
      }

      private fun timeMs(block: () -> Unit): Long {
          val start = SystemClock.elapsedRealtime()
          block()
          return SystemClock.elapsedRealtime() - start
      }

      private fun report(label: String, samples: List<Long>, imported: Int) {
          val sorted = samples.sorted()
          Log.i(TAG, "$label median_ms=${sorted[sorted.size / 2]} samples=${sorted.joinToString(",")} imported=$imported")
      }

      private fun measure(label: String, importOnce: (DatabaseBackend) -> Int) {
          var imported = 0
          val samples = (0..RUNS).map { run ->
              val name = "perf-$label-$run.db"
              val db = freshDb(name)
              try {
                  timeMs { imported = importOnce(db) }
              } finally {
                  dispose(db, name)
              }
          }.drop(1) // run 0 is the warm-up
          assertThat(imported).isGreaterThan(0)
          report(label, samples, imported)
      }

      @Test
      fun legacyPerRecordCommit() {
          measure("legacy") { db -> legacyImport(db, content) }
      }

      private companion object {
          const val TAG = "ImportPerf"
          const val ASSET = "url_import.txt"
          const val RUNS = 5
      }
  }
  ```

  `tools/perf/import_perf.sh`：

  ```bash
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
  ```

  ```bash
  chmod +x tools/perf/import_perf.sh
  tools/perf/emu_lock.sh tools/perf/import_perf.sh baseline
  ```

  Expected: 印出一列 `| baseline | 34 | legacy | <中位數> | <5 個樣本，逗號分隔> | <imported> |`。`imported` 必須大於 0，記下這個數字，Lane B 會用它檢查新路徑。

- [ ] **Step 9：StrictMode 基準**

  ```bash
  tools/perf/emu_lock.sh tools/perf/install_apk.sh debug
  tools/perf/emu_lock.sh tools/perf/strictmode_report.sh before
  ```

  Expected: 印出 `### StrictMode before (API 34)` 和數列 `| 次數 | 類型 @ frame |`，其中至少包含 `…DiskReadViolation @ org.cog.hymnchtv.persistance.DatabaseBackend.storeHymnHistory` 或 `…DiskWriteViolation @ …storeHymnHistory`（API 24 上類型名稱是 `android.os.StrictMode$StrictModeDiskReadViolation`）。如果沒有出現，代表腳本沒抓到東西，停下來檢查 `build/perf/strictmode-before-api34.log`。`createYbXTable` 不會出現是正常的（讀 assets 不經過 StrictMode，見量測協定）。

- [ ] **Step 10：依基準確認預期改善幅度**

  把 Step 8、Step 9 的輸出貼進量測文件。在「決策紀錄」寫一段：
  - 匯入的逐筆 commit 基準中位數是多少；維持「新路徑快 5 倍以上、低於 3 倍停下」的門檻，或依實際數字調整（例如基準已經低於 300 ms 時，改成「新路徑中位數低於 100 ms」），並寫明理由。
  - 冷啟動基準的雜訊帶數值（max(10 ms, 5%) 算出來是多少）。

- [ ] **Step 11：完整檢查並 commit**

  ```bash
  ./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug --console=plain | tail -1
  git add hymnchtv/src/main/java/org/cog/hymnchtv/concurrent hymnchtv/src/main/java/org/cog/hymnchtv/perf \
    hymnchtv/src/main/java/org/cog/hymnchtv/HymnsApp.java \
    hymnchtv/src/main/java/org/cog/hymnchtv/persistance/DatabaseBackend.java \
    hymnchtv/src/androidTest tools/perf/import_perf.sh docs/perf
  git commit -m "chore: add shared IO executor, debug StrictMode, test DB and transaction API

  Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

  Expected: `BUILD SUCCESSFUL`，commit 成功。

- [ ] **Step 12：建立三條 lane 的 worktree**

  ```bash
  cd /Users/hitobias/orca/hymnchtv-b
  git tag b-task1-base   # lanes' old base; Task F rebases lanes with --onto from here
  for lane in db import startup; do
    git worktree add -b "perf/b-lane-$lane" "../hymnchtv-b-$lane" perf/b-data-startup
    cp local.properties "../hymnchtv-b-$lane/"
  done
  git worktree list | grep hymnchtv-b
  ```

  Expected: 列出 `hymnchtv-b`、`hymnchtv-b-db`、`hymnchtv-b-import`、`hymnchtv-b-startup` 四個 worktree。

  派工：Lane A 在 `/Users/hitobias/orca/hymnchtv-b-db`，Lane B 在 `/Users/hitobias/orca/hymnchtv-b-import`，Lane C 在 `/Users/hitobias/orca/hymnchtv-b-startup`。給每個子代理的提示要包含：worktree 路徑、這份計畫的路徑、負責的 task 編號、「只能改表格中列給自己 lane 的檔案」、「模擬器指令一律透過 `tools/perf/emu_lock.sh`」、完成標準（各 task 最後一步）與回報格式（每個 task 的 commit SHA、測試數量、遇到的偏差）。

---

## Lane A：DatabaseBackend（worktree `hymnchtv-b-db`，只改 `DatabaseBackend.java` 和新增的 `HistoryPrune.kt`／測試）

### Task A1：移除每次執行的 `PRAGMA foreign_keys`（B-9c），開啟 WAL（B-9d）

**Files:**
- Modify: `hymnchtv/src/main/java/org/cog/hymnchtv/persistance/DatabaseBackend.java`（`onCreate`、`getWritableDatabase` override、新增 `onConfigure`）
- Create: `hymnchtv/src/androidTest/java/org/cog/hymnchtv/persistance/DatabaseBackendConfigTest.kt`

**Prerequisites:** Task 1。

- [ ] **Step 1：寫會失敗的測試** `DatabaseBackendConfigTest.kt`

  ```kotlin
  package org.cog.hymnchtv.persistance

  import androidx.test.ext.junit.runners.AndroidJUnit4
  import androidx.test.platform.app.InstrumentationRegistry
  import com.google.common.truth.Truth.assertThat
  import org.junit.After
  import org.junit.Before
  import org.junit.Test
  import org.junit.runner.RunWith

  @RunWith(AndroidJUnit4::class)
  class DatabaseBackendConfigTest {
      private val ctx = InstrumentationRegistry.getInstrumentation().targetContext
      private lateinit var db: DatabaseBackend

      @Before
      fun setUp() {
          ctx.deleteDatabase(NAME)
          db = DatabaseBackend.createForTest(ctx, NAME)
      }

      @After
      fun tearDown() {
          db.close()
          ctx.deleteDatabase(NAME)
      }

      private fun pragma(name: String): String =
          db.writableDatabase.rawQuery("PRAGMA $name", null).use { c ->
              assertThat(c.moveToFirst()).isTrue()
              c.getString(0)
          }

      @Test
      fun writeAheadLoggingIsEnabled() {
          assertThat(db.writableDatabase.isWriteAheadLoggingEnabled).isTrue()
          assertThat(pragma("journal_mode").lowercase()).isEqualTo("wal")
      }

      /** No table declares a foreign key, so the per-call PRAGMA was pure overhead (B-9c). */
      @Test
      fun foreignKeyPragmaIsNoLongerForcedOn() {
          assertThat(pragma("foreign_keys")).isEqualTo("0")
      }

      private companion object {
          const val NAME = "test-config.db"
      }
  }
  ```

- [ ] **Step 2：執行測試，確認失敗**

  ```bash
  export ANDROID_SERIAL=$(tools/perf/serial_for.sh api34)
  tools/perf/emu_lock.sh ./gradlew :hymnchtv:connectedDebugAndroidTest --console=plain \
    -Pandroid.testInstrumentationRunnerArguments.class=org.cog.hymnchtv.persistance.DatabaseBackendConfigTest | tail -3
  ```

  Expected: `BUILD FAILED`，2 個測試都失敗（journal_mode 是 `delete` 或 `truncate`；foreign_keys 是 `1`）。

- [ ] **Step 3：實作**

  1. 刪掉 `onCreate` 開頭這三行：

     ```java
             // db.execSQL("PRAGMA foreign_keys=ON;");
             String query = String.format("PRAGMA foreign_keys =%s", "ON");
             db.execSQL(query);
     ```

  2. 刪掉整個 override：

     ```java
         @Override
         public SQLiteDatabase getWritableDatabase() {
             SQLiteDatabase db = super.getWritableDatabase();
             // db.execSQL("PRAGMA foreign_keys=ON;");
             String query = String.format("PRAGMA foreign_keys =%s", "ON");
             db.execSQL(query);
             return db;
         }
     ```

  3. 在 `onUpgrade` 之前加入：

     ```java
         /**
          * WAL (B-9d): readers no longer wait for a writer, and each commit needs fewer fsyncs.
          * A journal-mode change is not a schema change, so DATABASE_VERSION stays the same.
          */
         @Override
         public void onConfigure(SQLiteDatabase db) {
             super.onConfigure(db);
             db.enableWriteAheadLogging();
         }
     ```

- [ ] **Step 4：執行測試，確認通過**

  同 Step 2 的指令，再加上 Task 1 的 transaction 測試：

  ```bash
  tools/perf/emu_lock.sh ./gradlew :hymnchtv:connectedDebugAndroidTest --console=plain \
    -Pandroid.testInstrumentationRunnerArguments.class=org.cog.hymnchtv.persistance.DatabaseBackendConfigTest,org.cog.hymnchtv.persistance.DatabaseBackendTransactionTest | tail -3
  ```

  Expected: `BUILD SUCCESSFUL`，9 個測試全部通過（Config 2、Transaction 7）。

- [ ] **Step 5：Commit**

  ```bash
  ./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug --console=plain | tail -1
  git add hymnchtv/src/main/java/org/cog/hymnchtv/persistance/DatabaseBackend.java hymnchtv/src/androidTest/java/org/cog/hymnchtv/persistance/DatabaseBackendConfigTest.kt
  git commit -m "perf: enable SQLite WAL and drop per-call foreign_keys pragma

  Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

### Task A2：`storeHymnHistory` 改用 `queryNumEntries`，只在超過上限時才刪除（B-9e）

**Files:**
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/persistance/HistoryPrune.kt`
- Create: `hymnchtv/src/test/java/org/cog/hymnchtv/persistance/HistoryPruneTest.kt`
- Create: `hymnchtv/src/androidTest/java/org/cog/hymnchtv/persistance/HymnHistoryStoreTest.kt`
- Modify: `hymnchtv/src/main/java/org/cog/hymnchtv/persistance/DatabaseBackend.java`（`storeHymnHistory`）

**Prerequisites:** Task A1。

**現有行為（必須保持一致）：** 寫入前，如果歷史紀錄有 `count` 筆、超過上限 200 筆（`excess = count - 200 > 0`），就把游標移到依時間排序後第 `excess + 9` 筆（0 起算），刪掉所有比它更舊的紀錄。例如 201 筆時刪掉最舊的 10 筆，寫入後剩 192 筆。

- [ ] **Step 1：先寫 characterization test（在舊程式碼上應該就會通過）** `HymnHistoryStoreTest.kt`

  ```kotlin
  package org.cog.hymnchtv.persistance

  import android.content.ContentValues
  import android.database.DatabaseUtils
  import androidx.test.ext.junit.runners.AndroidJUnit4
  import androidx.test.platform.app.InstrumentationRegistry
  import com.google.common.truth.Truth.assertThat
  import org.cog.hymnchtv.MainActivity
  import org.cog.hymnchtv.hymnhistory.HistoryRecord
  import org.junit.After
  import org.junit.Before
  import org.junit.Test
  import org.junit.runner.RunWith

  @RunWith(AndroidJUnit4::class)
  class HymnHistoryStoreTest {
      private val ctx = InstrumentationRegistry.getInstrumentation().targetContext
      private lateinit var db: DatabaseBackend
      private val cap = HistoryRecord.NUMBER_OF_RECORDS_IN_HISTORY

      @Before
      fun setUp() {
          ctx.deleteDatabase(NAME)
          db = DatabaseBackend.createForTest(ctx, NAME)
      }

      @After
      fun tearDown() {
          db.close()
          ctx.deleteDatabase(NAME)
      }

      /** Rows with timeStamp 1..count, all distinct hymns of hymn_db. */
      private fun seed(count: Int) {
          val sdb = db.writableDatabase
          db.runInTransaction {
              for (i in 1..count) {
                  sdb.insert(HistoryRecord.TABLE_NAME, null, ContentValues().apply {
                      put(HistoryRecord.HYMN_TYPE, MainActivity.HYMN_DB)
                      put(HistoryRecord.HYMN_NO, i)
                      put(HistoryRecord.HYMN_FU, false)
                      put(HistoryRecord.HYMN_TITLE, "title $i")
                      put(HistoryRecord.TIME_STAMP, i.toLong())
                  })
              }
          }
      }

      private fun store() =
          db.storeHymnHistory(HistoryRecord(MainActivity.HYMN_BB, 1, false, "new", 10_000L))

      private fun rows() = DatabaseUtils.queryNumEntries(db.readableDatabase, HistoryRecord.TABLE_NAME)

      private fun oldestTimeStamp(): Long =
          db.readableDatabase.rawQuery(
              "SELECT MIN(${HistoryRecord.TIME_STAMP}) FROM ${HistoryRecord.TABLE_NAME}", null
          ).use { c ->
              c.moveToFirst()
              c.getLong(0)
          }

      private companion object {
          const val NAME = "test-history.db"
      }

      @Test
      fun keepsEverythingAtTheCap() {
          seed(cap)
          store()
          assertThat(rows()).isEqualTo(cap + 1L)
          assertThat(oldestTimeStamp()).isEqualTo(1L)
      }

      @Test
      fun prunesTheOldestRowsLikeBeforeWhenOverTheCap() {
          seed(cap + 1)
          store()
          assertThat(rows()).isEqualTo(192L)
          assertThat(oldestTimeStamp()).isEqualTo(11L)
      }
  }
  ```

  ```bash
  export ANDROID_SERIAL=$(tools/perf/serial_for.sh api34)
  tools/perf/emu_lock.sh ./gradlew :hymnchtv:connectedDebugAndroidTest --console=plain \
    -Pandroid.testInstrumentationRunnerArguments.class=org.cog.hymnchtv.persistance.HymnHistoryStoreTest | tail -3
  ```

  Expected: `BUILD SUCCESSFUL`，2 個測試通過（舊程式碼的行為就是這樣；如果失敗，代表上面對舊行為的描述有誤，停下來回報）。

- [ ] **Step 2：寫會失敗的 JVM 測試** `hymnchtv/src/test/java/org/cog/hymnchtv/persistance/HistoryPruneTest.kt`

  ```kotlin
  package org.cog.hymnchtv.persistance

  import com.google.common.truth.Truth.assertThat
  import org.junit.Test

  class HistoryPruneTest {
      @Test
      fun noPruneAtOrBelowTheCap() {
          assertThat(HistoryPrune.keepFromOffset(0, 200)).isNull()
          assertThat(HistoryPrune.keepFromOffset(200, 200)).isNull()
      }

      @Test
      fun offsetIsExcessPlusNineLikeTheLegacyCursorMove() {
          assertThat(HistoryPrune.keepFromOffset(201, 200)).isEqualTo(10L)
          assertThat(HistoryPrune.keepFromOffset(250, 200)).isEqualTo(59L)
      }

      @Test(expected = IllegalArgumentException::class)
      fun capMustBePositive() {
          HistoryPrune.keepFromOffset(5, 0)
      }
  }
  ```

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.persistance.HistoryPruneTest'`
  Expected: 編譯失敗，訊息為 `Unresolved reference 'HistoryPrune'`。

- [ ] **Step 3：實作** `hymnchtv/src/main/java/org/cog/hymnchtv/persistance/HistoryPrune.kt`

  ```kotlin
  package org.cog.hymnchtv.persistance

  /** History pruning rule; pure so it can be unit-tested. */
  object HistoryPrune {
      /** Prune 10 rows below the cap at once so we do not prune on every single insert (legacy behaviour). */
      private const val SLACK = 10

      /**
       * @return the 0-based position, oldest first, of the oldest row to KEEP (every older row is deleted),
       * or null when [rowCount] does not exceed [cap].
       */
      @JvmStatic
      fun keepFromOffset(rowCount: Long, cap: Int): Long? {
          require(cap > 0) { "cap must be positive: $cap" }
          val excess = rowCount - cap
          return if (excess > 0) excess + SLACK - 1 else null
      }
  }
  ```

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.persistance.HistoryPruneTest'`
  Expected: BUILD SUCCESSFUL，3 個測試通過。

- [ ] **Step 4：改寫 `storeHymnHistory`**

  把整個 `public void storeHymnHistory(HistoryRecord mRecord) { ... }` 換成：

  ```java
      /**
       * Save the given HistoryRecord to the database table hymnHistory, in one transaction.
       * Counts with queryNumEntries instead of reading every row (B-9e); prunes only when over the cap.
       *
       * @param mRecord an instance of HistoryRecord
       */
      public void storeHymnHistory(HistoryRecord mRecord) {
          runInTransaction(() -> {
              SQLiteDatabase db = getWritableDatabase();
              long rowCount = DatabaseUtils.queryNumEntries(db, HistoryRecord.TABLE_NAME);
              Long keepFrom = HistoryPrune.keepFromOffset(rowCount, HistoryRecord.NUMBER_OF_RECORDS_IN_HISTORY);
              if (keepFrom != null) {
                  pruneHymnHistory(db, keepFrom);
              }

              ContentValues values = new ContentValues();
              values.put(HistoryRecord.HYMN_TYPE, mRecord.getHymnType());
              values.put(HistoryRecord.HYMN_NO, mRecord.getHymnNo());
              values.put(HistoryRecord.HYMN_FU, mRecord.isFu());
              values.put(HistoryRecord.HYMN_TITLE, mRecord.getHymnTitle());
              values.put(TIME_STAMP, mRecord.getTimeStamp());

              long row = db.insert(HistoryRecord.TABLE_NAME, null, values);
              if (row == -1) {
                  Timber.e("### Error in creating history record HymnType#hymNo: %s#%s", mRecord.getHymnType(), mRecord.getHymnNo());
              }
          });
      }

      /**
       * Delete every history row older than the row at keepFrom (0-based, oldest first).
       */
      private void pruneHymnHistory(SQLiteDatabase db, long keepFrom) {
          String[] columns = {TIME_STAMP};
          try (Cursor cursor = db.query(HistoryRecord.TABLE_NAME, columns, null, null, null, null,
                  TIME_STAMP + " ASC", keepFrom + ",1")) {
              if (cursor.moveToFirst()) {
                  String[] args = {cursor.getString(0)};
                  int count = db.delete(HistoryRecord.TABLE_NAME, TIME_STAMP + "<?", args);
                  Timber.d("No of old history deleted : %s", count);
              }
          }
      }
  ```

  import 區加入 `import android.database.DatabaseUtils;`。

- [ ] **Step 5：執行測試，確認通過**

  ```bash
  ./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.persistance.HistoryPruneTest' --console=plain | tail -1
  tools/perf/emu_lock.sh ./gradlew :hymnchtv:connectedDebugAndroidTest --console=plain \
    -Pandroid.testInstrumentationRunnerArguments.class=org.cog.hymnchtv.persistance.HymnHistoryStoreTest | tail -3
  ```

  Expected: 兩個指令都是 `BUILD SUCCESSFUL`；instrumented 2 個測試通過（和 Step 1 的結果相同，證明行為沒變）。

- [ ] **Step 6：Commit**

  ```bash
  ./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug --console=plain | tail -1
  git add hymnchtv/src/main/java/org/cog/hymnchtv/persistance hymnchtv/src/test/java/org/cog/hymnchtv/persistance hymnchtv/src/androidTest/java/org/cog/hymnchtv/persistance/HymnHistoryStoreTest.kt
  git commit -m "perf: count hymn history with queryNumEntries and prune only over the cap

  Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

### Task A3：新增 `getMediaRecords(hymnType, hymnNo, isFu)`（B-9b，只做 DB API）

**Files:**
- Modify: `hymnchtv/src/main/java/org/cog/hymnchtv/persistance/DatabaseBackend.java`（在 `getMediaRecord` 之後新增方法）
- Create: `hymnchtv/src/androidTest/java/org/cog/hymnchtv/persistance/MediaRecordsQueryTest.kt`

**Prerequisites:** Task A2。

**這一步不改任何呼叫端**（`ContentHandler.getHymnMediaState` 留給第二波，改法見附錄 A）。

- [ ] **Step 1：寫會失敗的測試** `MediaRecordsQueryTest.kt`

  ```kotlin
  package org.cog.hymnchtv.persistance

  import android.content.ContentValues
  import androidx.test.ext.junit.runners.AndroidJUnit4
  import androidx.test.platform.app.InstrumentationRegistry
  import com.google.common.truth.Truth.assertThat
  import org.cog.hymnchtv.MainActivity
  import org.cog.hymnchtv.MediaType
  import org.cog.hymnchtv.mediaconfig.MediaConfig
  import org.cog.hymnchtv.mediaconfig.MediaRecord
  import org.junit.After
  import org.junit.Assert.assertThrows
  import org.junit.Before
  import org.junit.Test
  import org.junit.runner.RunWith

  @RunWith(AndroidJUnit4::class)
  class MediaRecordsQueryTest {
      private val ctx = InstrumentationRegistry.getInstrumentation().targetContext
      private lateinit var db: DatabaseBackend

      @Before
      fun setUp() {
          ctx.deleteDatabase(NAME)
          db = DatabaseBackend.createForTest(ctx, NAME)
      }

      @After
      fun tearDown() {
          db.close()
          ctx.deleteDatabase(NAME)
      }

      private fun store(no: Int, isFu: Boolean, type: MediaType, uri: String) {
          db.storeMediaRecord(MediaRecord(MainActivity.HYMN_DB, no, isFu, type, uri, null))
      }

      @Test
      fun returnsEveryMediaTypeOfOneHymnInOneCall() {
          store(5, false, MediaType.HYMN_MEDIA, "https://a")
          store(5, false, MediaType.HYMN_JIAOCHANG, "https://b")
          store(6, false, MediaType.HYMN_MEDIA, "https://c")
          store(5, true, MediaType.HYMN_BANZOU, "https://d")

          val records = db.getMediaRecords(MainActivity.HYMN_DB, 5, false)

          assertThat(records.keys).containsExactly(MediaType.HYMN_MEDIA, MediaType.HYMN_JIAOCHANG)
          assertThat(records[MediaType.HYMN_JIAOCHANG]!!.mediaUri).isEqualTo("https://b")
          assertThat(records[MediaType.HYMN_MEDIA]!!.hymnNo).isEqualTo(5)
      }

      @Test
      fun emptyWhenTheHymnHasNoRecords() {
          assertThat(db.getMediaRecords(MainActivity.HYMN_DB, 99, false)).isEmpty()
      }

      @Test
      fun skipsRowsWithAnUnknownMediaType() {
          db.writableDatabase.insert(MainActivity.HYMN_DB, null, ContentValues().apply {
              put(MediaConfig.HYMN_NO, 7)
              put(MediaConfig.HYMN_FU, false)
              put(MediaConfig.MEDIA_TYPE, "HYMN_URL")
              put(MediaConfig.MEDIA_URI, "https://old")
          })
          store(7, false, MediaType.HYMN_MEDIA, "https://ok")

          assertThat(db.getMediaRecords(MainActivity.HYMN_DB, 7, false).keys).containsExactly(MediaType.HYMN_MEDIA)
      }

      @Test
      fun resultIsReadOnly() {
          store(8, false, MediaType.HYMN_MEDIA, "https://a")
          val records = db.getMediaRecords(MainActivity.HYMN_DB, 8, false) as MutableMap<MediaType, MediaRecord>
          assertThrows(UnsupportedOperationException::class.java) { records.clear() }
      }

      private companion object {
          const val NAME = "test-media-records.db"
      }
  }
  ```

  ```bash
  ./gradlew :hymnchtv:compileDebugAndroidTestKotlin --console=plain 2>&1 | grep -E "No value passed|Too many arguments|BUILD" | head -3
  ```

  Expected: `BUILD FAILED`（現有的 `getMediaRecords(String)` 只有一個參數，三個參數的呼叫無法編譯）。

- [ ] **Step 2：實作**

  在 `getMediaRecord(MediaRecord mRecord, boolean update)` 方法之後加入：

  ```java
      /**
       * All media records of one hymn in a single query (B-9b), keyed by media type.
       * hymnNo is the same value getMediaRecord() uses (fu hymns keep their stored offset).
       * Rows whose media type is no longer a MediaType value are skipped.
       *
       * @return read-only map; empty when the hymn has no records
       */
      public Map<MediaType, MediaRecord> getMediaRecords(String hymnType, int hymnNo, boolean isFu) {
          SQLiteDatabase db = getReadableDatabase();
          String[] columns = {MediaConfig.MEDIA_TYPE, MediaConfig.MEDIA_URI, MediaConfig.MEDIA_FILE_PATH};
          String[] args = {Integer.toString(hymnNo), isFu ? "1" : "0"};
          Map<MediaType, MediaRecord> records = new EnumMap<>(MediaType.class);

          try (Cursor cursor = db.query(hymnType, columns,
                  MediaConfig.HYMN_NO + "=? AND " + MediaConfig.HYMN_FU + "=?", args, null, null, null)) {
              while (cursor.moveToNext()) {
                  MediaType mediaType = parseMediaType(cursor.getString(0));
                  if (mediaType != null) {
                      records.put(mediaType, new MediaRecord(hymnType, hymnNo, isFu, mediaType,
                              cursor.getString(1), cursor.getString(2)));
                  }
              }
          }
          return Collections.unmodifiableMap(records);
      }

      private static MediaType parseMediaType(String value) {
          try {
              return MediaType.valueOf(value);
          } catch (IllegalArgumentException | NullPointerException e) {
              Timber.w("Skip media record with unknown type: %s", value);
              return null;
          }
      }
  ```

  import 區加入：

  ```java
  import java.util.Collections;
  import java.util.EnumMap;
  import java.util.Map;
  ```

- [ ] **Step 3：執行測試，確認通過**

  ```bash
  export ANDROID_SERIAL=$(tools/perf/serial_for.sh api34)
  tools/perf/emu_lock.sh ./gradlew :hymnchtv:connectedDebugAndroidTest --console=plain \
    -Pandroid.testInstrumentationRunnerArguments.class=org.cog.hymnchtv.persistance.MediaRecordsQueryTest | tail -3
  ```

  Expected: `BUILD SUCCESSFUL`，4 個測試通過。

- [ ] **Step 4：跑完 Lane A 所有測試並 commit**

  ```bash
  ./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug --console=plain | tail -1
  tools/perf/emu_lock.sh ./gradlew :hymnchtv:connectedDebugAndroidTest --console=plain \
    -Pandroid.testInstrumentationRunnerArguments.package=org.cog.hymnchtv.persistance | tail -3
  git add hymnchtv/src/main/java/org/cog/hymnchtv/persistance/DatabaseBackend.java hymnchtv/src/androidTest/java/org/cog/hymnchtv/persistance/MediaRecordsQueryTest.kt
  git commit -m "feat: query all media records of a hymn in one call

  Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

  Expected: 兩個 build 都是 `BUILD SUCCESSFUL`；`persistance` 套件共 15 個 instrumented 測試通過（Transaction 7、Config 2、HistoryStore 2、MediaRecordsQuery 4），0 個失敗。

  Lane A 完成，回報三個 commit 的 SHA。

---

## Lane B：匯入（worktree `hymnchtv-b-import`，只改 `MediaConfig.java`、`NotionRecord.java`、`QQRecord.java`，新增 `ImportResult.kt`／測試）

### Task B1：`url_import` 匯入改成單一 transaction、在背景執行（B-9a）

**Files:**
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/mediaconfig/ImportResult.kt`
- Modify: `hymnchtv/src/main/java/org/cog/hymnchtv/mediaconfig/MediaConfig.java`（`importMediaRecords`、`importUrlRecords`）
- Create: `hymnchtv/src/androidTest/java/org/cog/hymnchtv/mediaconfig/UrlImportTest.kt`
- Create: `hymnchtv/src/androidTest/java/org/cog/hymnchtv/mediaconfig/UrlImportJobTest.kt`
- Modify: `hymnchtv/src/androidTest/java/org/cog/hymnchtv/mediaconfig/ImportPerfTest.kt`

**Prerequisites:** Task 1（`inTransaction`、`storeMediaRecordOrThrow`）。

**呼叫端說明：** `importUrlRecords(InputStream, boolean)` 有三個呼叫端：
- `MediaConfig.importMediaRecords`（使用者按「DB Impt」，目前在主執行緒）→ 這個 task 改成背景執行。
- `UpdateServiceImpl.checkUrlImport`（`OnlineUpdateService` 是 `IntentService`，本來就在背景）→ 不用改，只是自動得到 transaction。
- `MediaConfig.importUrlAssetFile`（目前沒有任何地方呼叫，`MainActivity` 那一行已經註解掉）→ 不改。它有一個既有 bug：匯入讀完 stream 之後才拿同一個 stream 去存檔，存出來的檔案是空的。因為是死碼，不在這一波處理，列在 Task F 的待辦。

- [ ] **Step 1：寫會失敗的測試** `UrlImportTest.kt`

  ```kotlin
  package org.cog.hymnchtv.mediaconfig

  import android.database.DatabaseUtils
  import android.database.SQLException
  import androidx.test.ext.junit.runners.AndroidJUnit4
  import androidx.test.platform.app.InstrumentationRegistry
  import com.google.common.truth.Truth.assertThat
  import org.cog.hymnchtv.MainActivity
  import org.cog.hymnchtv.MediaType
  import org.cog.hymnchtv.persistance.DatabaseBackend
  import org.junit.After
  import org.junit.Assert.assertThrows
  import org.junit.Before
  import org.junit.Test
  import org.junit.runner.RunWith

  @RunWith(AndroidJUnit4::class)
  class UrlImportTest {
      private val ctx = InstrumentationRegistry.getInstrumentation().targetContext
      private lateinit var db: DatabaseBackend

      @Before
      fun setUp() {
          ctx.deleteDatabase(NAME)
          db = DatabaseBackend.createForTest(ctx, NAME)
      }

      @After
      fun tearDown() {
          db.close()
          ctx.deleteDatabase(NAME)
      }

      private fun rows() = DatabaseUtils.queryNumEntries(db.readableDatabase, MainActivity.HYMN_DB)

      private fun uriOf(no: Int, type: MediaType): String? {
          val record = MediaRecord(MainActivity.HYMN_DB, no, false, type)
          return if (db.getMediaRecord(record, true)) record.mediaUri else null
      }

      @Test
      fun importsWellFormedLinesAndSkipsMalformedOnes() {
          val content = listOf(
              "hymn_db,1,0,HYMN_MEDIA,https://example.org/1,null",
              "hymn_db,abc,0,HYMN_MEDIA,https://example.org/bad-number,null",
              "hymn_db,3,0,NOT_A_TYPE,https://example.org/bad-type,null",
              "",
              "hymn_db,2,0,HYMN_JIAOCHANG,https://example.org/2,null",
          ).joinToString("\r\n")

          val result = MediaConfig.importUrlRecords(db, content, false)

          assertThat(result).isEqualTo(ImportResult(2, 2))
          assertThat(rows()).isEqualTo(2L)
      }

      @Test
      fun keepsExistingRecordsUnlessOverwrite() {
          db.storeMediaRecord(MediaRecord(MainActivity.HYMN_DB, 1, false, MediaType.HYMN_MEDIA, "https://old", null))
          val line = "hymn_db,1,0,HYMN_MEDIA,https://new,null"

          assertThat(MediaConfig.importUrlRecords(db, line, false)).isEqualTo(ImportResult(0, 1))
          assertThat(uriOf(1, MediaType.HYMN_MEDIA)).isEqualTo("https://old")

          assertThat(MediaConfig.importUrlRecords(db, line, true)).isEqualTo(ImportResult(1, 1))
          assertThat(uriOf(1, MediaType.HYMN_MEDIA)).isEqualTo("https://new")
      }

      /** A database error part-way through rolls back every earlier write of the same import. */
      @Test
      fun databaseErrorRollsBackTheWholeImport() {
          db.writableDatabase.execSQL("DROP TABLE ${MainActivity.HYMN_BB}")
          val content = "hymn_db,1,0,HYMN_MEDIA,https://example.org/1,null\nhymn_bb,1,0,HYMN_MEDIA,https://example.org/bb1,null"

          assertThrows(SQLException::class.java) { MediaConfig.importUrlRecords(db, content, false) }
          assertThat(rows()).isEqualTo(0L)
      }

      private companion object {
          const val NAME = "test-url-import.db"
      }
  }
  ```

  Run: `./gradlew :hymnchtv:compileDebugAndroidTestKotlin --console=plain 2>&1 | grep -E "Unresolved reference|BUILD" | head -3`
  Expected: `Unresolved reference 'ImportResult'`，`BUILD FAILED`。

- [ ] **Step 2：實作** `hymnchtv/src/main/java/org/cog/hymnchtv/mediaconfig/ImportResult.kt`

  ```kotlin
  package org.cog.hymnchtv.mediaconfig

  /** Outcome of one url-record import: [imported] rows written out of [total] well-formed lines. */
  data class ImportResult(val imported: Int, val total: Int) {
      companion object {
          @JvmField
          val EMPTY = ImportResult(0, 0)
      }
  }
  ```

- [ ] **Step 3：改寫 `MediaConfig.importUrlRecords`**

  把整個 `public static void importUrlRecords(InputStream inputStream, boolean isOverWrite) { ... }`（從 `/** Import the url records into the database form the given inputStream */` 到方法結尾）換成：

  ```java
      /**
       * Import the url records from the given inputStream into the app database, in one transaction (B-9a),
       * then show the same result toast as before. Blocks for up to a few seconds: never call on the main thread.
       */
      @WorkerThread
      public static ImportResult importUrlRecords(InputStream inputStream, boolean isOverWrite) {
          return importUrlRecords(mDB, inputStream, isOverWrite);
      }

      /**
       * Same as above for an explicit database. A database error propagates as android.database.SQLException
       * after the transaction has rolled back; a read error yields ImportResult.EMPTY.
       */
      @WorkerThread
      public static ImportResult importUrlRecords(DatabaseBackend db, InputStream inputStream, boolean isOverWrite) {
          ImportResult result;
          try {
              String content = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
              result = importUrlRecords(db, content, isOverWrite);
          } catch (IOException e) {
              Timber.w("Import file read error: %s", e.getMessage());
              result = ImportResult.EMPTY;
          }
          HymnsApp.showToastMessage(R.string.db_import_record, result.getImported(), result.getTotal());
          return result;
      }

      /**
       * Import every well-formed line of content into db in a single transaction.
       * Malformed lines are skipped; a database error rolls the whole import back.
       */
      @VisibleForTesting
      @WorkerThread
      public static ImportResult importUrlRecords(DatabaseBackend db, String content, boolean isOverWrite) {
          long start = SystemClock.elapsedRealtime();
          String[] lines = content.split("\r\n|\n");
          ImportResult result = db.inTransaction(() -> importLines(db, lines, isOverWrite));
          Timber.i("perf: url import %d/%d records in %d ms",
                  result.getImported(), result.getTotal(), SystemClock.elapsedRealtime() - start);
          return result;
      }

      private static ImportResult importLines(DatabaseBackend db, String[] lines, boolean isOverWrite) {
          int imported = 0;
          int total = 0;
          for (String line : lines) {
              MediaRecord mediaRecord = parseImportLine(line);
              if (mediaRecord == null)
                  continue;

              boolean isFu = mediaRecord.isFu();
              int hymnNo = isFu ? (mediaRecord.getHymnNo() - HYMN_DB_NO_MAX) : mediaRecord.getHymnNo();
              int nui = HymnNoValidate.validateHymnNo(mediaRecord.getHymnType(), hymnNo, isFu);
              if ((nui != -1) && (isOverWrite || !db.getMediaRecord(mediaRecord, false))) {
                  db.storeMediaRecordOrThrow(mediaRecord); // SQL errors propagate: the whole import rolls back
                  imported++;
              }
              total++;

              if (TimberLog.isFinestEnable)
                  Timber.d("Import media record: %s; %s(%s); %s", nui, hymnNo, imported, line);
          }
          return new ImportResult(imported, total);
      }

      /** @return the record, or null for a blank or malformed line (bad number or unknown media type). */
      private static MediaRecord parseImportLine(String line) {
          try {
              return MediaRecord.toRecord(line);
          } catch (IllegalArgumentException e) {
              Timber.w("Skip malformed import line: %s", line);
              return null;
          }
      }
  ```

  行為差異（刻意的）：
  - 原本 `storeMediaRecord` 回傳 -1（寫入失敗）也算進「匯入筆數」，而且其他筆照樣寫入；現在任何 SQL 錯誤都會丟出 `SQLException`，整批 rollback，由呼叫端顯示失敗 toast。
  - 原本格式錯誤的一行（數字或媒體類型不合法）會丟出例外、中斷整個匯入（在主執行緒上就是當機）；現在只跳過那一行。

  import 區：
  - 刪除 `import org.apache.http.util.EncodingUtils;`（這個檔案裡已經沒有其他地方用到；先用 `grep -n EncodingUtils hymnchtv/src/main/java/org/cog/hymnchtv/mediaconfig/MediaConfig.java` 確認只剩 import 那一行）。
  - 加入：

    ```java
    import android.content.res.AssetManager;
    import android.database.SQLException;
    import android.os.SystemClock;

    import androidx.annotation.MainThread;
    import androidx.annotation.VisibleForTesting;
    import androidx.annotation.WorkerThread;
    import androidx.lifecycle.LiveData;
    import androidx.lifecycle.MutableLiveData;

    import java.nio.charset.StandardCharsets;

    import org.apache.commons.io.IOUtils;
    import org.cog.hymnchtv.concurrent.AppExecutors;
    ```

    （`android.view.View` 已經有 import，因為 `onClick(View v)` 用到它。）

- [ ] **Step 4：寫會失敗的測試：匯入工作屬於 process，不屬於畫面** `hymnchtv/src/androidTest/java/org/cog/hymnchtv/mediaconfig/UrlImportJobTest.kt`

  **所有權規則（這個 task 的設計）：**
  - 匯入工作屬於 **process**，不屬於 `MediaConfig` 畫面。使用者離開畫面時工作繼續跑完。
  - 背景 closure 只捕捉：`DatabaseBackend`（process 單例）、`AssetManager`（application 層級）、檔名字串、`isOverWrite` 布林值。**不捕捉** `MediaConfig.this`、任何 `View`、`DialogActivity`。
  - 完成時用 `HymnsApp.showToastMessage`（application context）回報結果或錯誤。
  - 「是否正在匯入」放在 process 層級的 `MutableLiveData<Boolean>`。畫面在 `onCreate` 用 `observe(this, …)`（lifecycle-aware，畫面銷毀時自動解除）來停用／啟用按鈕；使用者在匯入中重新進入畫面，按鈕也會是停用狀態。
  - 同一時間只能有一個匯入；第二次按下時顯示「進行中」toast，不排第二個工作。

  ```kotlin
  package org.cog.hymnchtv.mediaconfig

  import android.database.DatabaseUtils
  import androidx.lifecycle.Observer
  import androidx.test.ext.junit.runners.AndroidJUnit4
  import androidx.test.platform.app.InstrumentationRegistry
  import com.google.common.truth.Truth.assertThat
  import org.cog.hymnchtv.MainActivity
  import org.cog.hymnchtv.persistance.DatabaseBackend
  import org.junit.After
  import org.junit.Before
  import org.junit.Test
  import org.junit.runner.RunWith
  import java.util.concurrent.CountDownLatch
  import java.util.concurrent.TimeUnit
  import java.util.concurrent.atomic.AtomicBoolean

  /** The url import runs without any Activity, rejects a second start, and clears its running flag. */
  @RunWith(AndroidJUnit4::class)
  class UrlImportJobTest {
      private val instrumentation = InstrumentationRegistry.getInstrumentation()
      private val ctx = instrumentation.targetContext
      private lateinit var db: DatabaseBackend

      @Before
      fun setUp() {
          ctx.deleteDatabase(NAME)
          db = DatabaseBackend.createForTest(ctx, NAME)
      }

      @After
      fun tearDown() {
          db.close()
          ctx.deleteDatabase(NAME)
      }

      @Test
      fun runsWithoutAnActivityAndClearsTheRunningFlag() {
          val firstStarted = AtomicBoolean(false)
          val secondStarted = AtomicBoolean(true)
          val finished = CountDownLatch(1)
          val observer = Observer<Boolean> { running -> if (running == false) finished.countDown() }

          instrumentation.runOnMainSync {
              firstStarted.set(MediaConfig.startUrlImport(db, "url_import.txt", null, false))
              secondStarted.set(MediaConfig.startUrlImport(db, "url_import.txt", null, false))
              MediaConfig.urlImportRunning().observeForever(observer)
          }
          try {
              assertThat(firstStarted.get()).isTrue()
              assertThat(secondStarted.get()).isFalse()
              assertThat(finished.await(60, TimeUnit.SECONDS)).isTrue()
              assertThat(DatabaseUtils.queryNumEntries(db.readableDatabase, MainActivity.HYMN_DB)).isGreaterThan(0L)
          } finally {
              instrumentation.runOnMainSync { MediaConfig.urlImportRunning().removeObserver(observer) }
          }
      }

      @Test
      fun missingFileStillClearsTheRunningFlag() {
          val finished = CountDownLatch(1)
          val observer = Observer<Boolean> { running -> if (running == false) finished.countDown() }
          instrumentation.runOnMainSync {
              MediaConfig.startUrlImport(db, null, "/does/not/exist.txt", false)
              MediaConfig.urlImportRunning().observeForever(observer)
          }
          try {
              assertThat(finished.await(10, TimeUnit.SECONDS)).isTrue()
          } finally {
              instrumentation.runOnMainSync { MediaConfig.urlImportRunning().removeObserver(observer) }
          }
      }

      private companion object {
          const val NAME = "test-url-import-job.db"
      }
  }
  ```

  （`observeForever` 在註冊時會立刻收到目前的值 `true`，之後才收到 `false`，所以 latch 只在工作結束時倒數。）

  Run: `./gradlew :hymnchtv:compileDebugAndroidTestKotlin --console=plain 2>&1 | grep -E "Unresolved reference|BUILD" | head -3`
  Expected: `Unresolved reference 'startUrlImport'`（或 `urlImportRunning`），`BUILD FAILED`。

- [ ] **Step 5：實作 process 層級的匯入工作，並讓畫面只做觀察**

  1. 在 `MediaConfig` 的 static 欄位區（`private static final DatabaseBackend mDB = …` 之後）加入：

     ```java
         /** True while a url import runs. Process-scoped; screens observe it lifecycle-aware. */
         private static final MutableLiveData<Boolean> urlImportRunning = new MutableLiveData<>(false);

         public static LiveData<Boolean> urlImportRunning() {
             return urlImportRunning;
         }

         /**
          * Start a url import owned by the process, not by any screen: it keeps running if the user leaves,
          * reports through application-context toasts, and captures no Activity or View.
          *
          * @param assetFile bundled asset to import, or null to use importFile
          * @param importFile absolute path of a user-chosen file (used when assetFile is null)
          * @return false (and nothing is started) when an import is already running
          */
         @MainThread
         public static boolean startUrlImport(DatabaseBackend db, String assetFile, String importFile, boolean isOverWrite) {
             if (Boolean.TRUE.equals(urlImportRunning.getValue())) {
                 return false;
             }
             urlImportRunning.setValue(true);
             HymnsApp.showToastMessage(R.string.db_import_start);
             AssetManager assets = HymnsApp.getAppResources().getAssets();

             AppExecutors.io("url-import", () -> {
                 try (InputStream inputStream = (assetFile != null) ? assets.open(assetFile) : new FileInputStream(importFile)) {
                     importUrlRecords(db, inputStream, isOverWrite);
                 } catch (IOException e) {
                     Timber.w("Input file not accessible: %s", e.getMessage());
                     HymnsApp.showToastMessage(R.string.file_does_not_exist);
                 } catch (SQLException e) {
                     Timber.e(e, "Url import rolled back");
                     HymnsApp.showToastMessage(R.string.add_to_db_failed);
                 } finally {
                     urlImportRunning.postValue(false);
                 }
             });
             return true;
         }
     ```

  2. 把 `importMediaRecords(String assetFile)` 裡的 `onConfirmClicked` 整個換成（這個 closure 在主執行緒同步執行完就結束，不會被背景工作持有）：

     ```java
                         public boolean onConfirmClicked(DialogActivity dialog) {
                             boolean isOverWrite = cbOverwrite.isChecked();
                             if (!startUrlImport(mDB, assetFile, importFile, isOverWrite)) {
                                 HymnsApp.showToastMessage(R.string.in_progress);
                             }
                             return true;
                         }
     ```

  3. 在 `onCreate` 裡，`findViewById(R.id.button_import).setOnLongClickListener(this);` 的下一行加入：

     ```java
             // Lifecycle-aware: removed automatically when this screen is destroyed; the import itself is process-owned.
             View btnImport = findViewById(R.id.button_import);
             urlImportRunning.observe(this, running -> btnImport.setEnabled(!Boolean.TRUE.equals(running)));
     ```

  說明：
  - 現在的 UI 只有「結果 toast」；新增的是「開始 toast」（字串 `db_import_start` 三種語言都已經存在，原本被註解掉）、匯入期間停用按鈕（和 `downloadNQRecord` 停用 `btnNQ` 的做法一致），以及重複按下時的「進行中」toast（`R.string.in_progress` 已存在）。
  - 讀取 `cbOverwrite` 仍在主執行緒；開檔、讀檔、寫 DB 都在背景。
  - `onConfirmClicked` 裡不再有任何 lambda 捕捉 `MediaConfig.this`；`observe(this, …)` 的觀察者會在畫面銷毀時被 LiveData 自動移除。

  Run:
  ```bash
  grep -n "MediaConfig.this" hymnchtv/src/main/java/org/cog/hymnchtv/mediaconfig/MediaConfig.java
  ```
  Expected: 列出的行都不在 `startUrlImport` 或 `importMediaRecords` 裡（例如 `downloadNQRecord` 原本就有的 `QQRecord.fetchQQLinks(MediaConfig.this)` 可以保留，那是既有程式碼，不在這個 task 的範圍）。

- [ ] **Step 6：在 `ImportPerfTest` 加上新路徑的量測**

  在 `legacyPerRecordCommit()` 之後加入：

  ```kotlin
      @Test
      fun batchSingleTransaction() {
          val checkName = "perf-check-legacy.db"
          val checkDb = freshDb(checkName)
          val expected = try {
              legacyImport(checkDb, content)
          } finally {
              dispose(checkDb, checkName)
          }

          measure("batch") { db ->
              val result = MediaConfig.importUrlRecords(db, content, false)
              assertThat(result.imported).isEqualTo(expected)
              result.imported
          }
      }
  ```

- [ ] **Step 7：執行測試，確認通過，並量測**

  ```bash
  export ANDROID_SERIAL=$(tools/perf/serial_for.sh api34)
  tools/perf/emu_lock.sh ./gradlew :hymnchtv:connectedDebugAndroidTest --console=plain \
    -Pandroid.testInstrumentationRunnerArguments.class=org.cog.hymnchtv.mediaconfig.UrlImportTest,org.cog.hymnchtv.mediaconfig.UrlImportJobTest | tail -3
  tools/perf/emu_lock.sh tools/perf/import_perf.sh lane-b
  ```

  Expected:
  - `UrlImportTest` + `UrlImportJobTest`：`BUILD SUCCESSFUL`，5 個測試通過。
  - `import_perf.sh` 印出兩列：`legacy` 和 `batch`。`batch` 的 `imported` 等於 `legacy` 的；`batch` 的中位數達到 Task 1 Step 10 寫下的門檻。沒達到就停下來回報，附上兩列數字。
  - 注意：這個 worktree 還沒有 Lane A 的 WAL，所以這裡的 `legacy` 應該和 Task 1 的基準相近。最後的 WAL + transaction 數字在 Task F 量。

- [ ] **Step 8：手動檢查（api34）**

  ```bash
  tools/perf/emu_lock.sh tools/perf/install_apk.sh debug
  ```

  手動操作：主畫面 → 選單 → 媒體設定（`MediaConfig`）→ 長按「DB Impt」（匯入內建的 `url_import.txt`）→ 確認。
  Expected：
  - 立刻出現「開始將檔案內容匯入資料庫媒體記錄…」；匯入期間按鈕變灰、畫面可以捲動；結束出現「資料庫匯入共 N (M) 首詩歌」，按鈕恢復。`adb logcat -d | grep "perf: url import"` 有一行時間紀錄。
  - 所有權：再匯入一次，這次一確認就按返回離開媒體設定 → 不當機，結果 toast 仍然出現（application context）。匯入中重新進入媒體設定 → 按鈕是灰的；匯入結束後自動恢復。

- [ ] **Step 9：Commit**

  ```bash
  ./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug --console=plain | tail -1
  git add hymnchtv/src/main/java/org/cog/hymnchtv/mediaconfig/MediaConfig.java hymnchtv/src/main/java/org/cog/hymnchtv/mediaconfig/ImportResult.kt hymnchtv/src/androidTest/java/org/cog/hymnchtv/mediaconfig
  git commit -m "perf: import url records in one transaction off the main thread

  Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

### Task B2：Notion／QQ 連結：解析與寫入分開，每頁單一 transaction、在背景寫入，DB 錯誤整頁 rollback（B-9a）

**Files:**
- Modify: `hymnchtv/src/main/java/org/cog/hymnchtv/mediaconfig/NotionRecord.java`（計數器、`saveNQRecord` 的 callback、`storeNQJObject` 拆成 `parseNQJObject` + `storeNQJArray`，新增 `storeNQPage`）
- Modify: `hymnchtv/src/main/java/org/cog/hymnchtv/mediaconfig/QQRecord.java`（同樣的拆法：`parseQQJObject`、`storeQQJArray`、`storeQQPage`）
- Create: `hymnchtv/src/androidTest/java/org/cog/hymnchtv/mediaconfig/NotionStoreTest.kt`
- Create: `hymnchtv/src/androidTest/java/org/cog/hymnchtv/mediaconfig/QQStoreTest.kt`

**Prerequisites:** Task B1（同一條 lane 依序執行）；Task 1 的 `inTransaction`、`storeMediaRecordOrThrow`。

**現況與問題：**
- WebView 抓到一頁連結後，`evaluateJavascript` 的 callback 在主執行緒上逐筆呼叫 `storeNQJObject`／`storeQQJObject`，每筆一次 commit。
- `storeNQJObject` 用 `catch (Exception e)` 包住整段（包括 DB 寫入），而 `storeMediaRecord` 本身遇到 SQL 錯誤只回傳 -1。所以如果直接把迴圈包進 transaction，DB 錯誤會被吞掉，transaction 照樣 commit 一半的資料，也不會有失敗 toast（Codex P1）。

**設計：**
- **解析**（`parseNQJObject`／`parseQQJObject`）：只處理資料格式。標題、編號、網址不合法時回傳 null（記 log、跳過），只攔 `JSONException`、`NumberFormatException`，**不碰 DB**。
- **寫入**（`storeNQJArray(db, …)`／`storeQQJArray(db, …)`）：在 `db.inTransaction` 裡逐筆寫入，用 `storeMediaRecordOrThrow`，**不攔任何 SQL 例外**，所以任何 DB 錯誤都會讓整頁 rollback 並往外丟。回傳 `ImportResult(saved, found)`，不碰 static 計數器，所以可以用固定的 JSON fixture 和測試資料庫直接測。
- **背景包裝**（`storeNQPage`／`storeQQPage`）：在 `AppExecutors.io` 上呼叫寫入函式，成功後把結果加進 static 計數器並顯示原本的「完成」toast；攔 `android.database.SQLException`，記 log 並顯示原本就有的 `nq_download_failed` toast。不捕捉任何 Activity。
- WebView 本身留在主執行緒，不變。
- 外部網站無法自動化測試，但「解析 + 寫入 + rollback」可以用固定 fixture 完整測試，這就是這個 task 的測試範圍。

- [ ] **Step 1：寫會失敗的測試** `hymnchtv/src/androidTest/java/org/cog/hymnchtv/mediaconfig/NotionStoreTest.kt`

  ```kotlin
  package org.cog.hymnchtv.mediaconfig

  import android.database.DatabaseUtils
  import android.database.SQLException
  import androidx.test.ext.junit.runners.AndroidJUnit4
  import androidx.test.platform.app.InstrumentationRegistry
  import com.google.common.truth.Truth.assertThat
  import org.cog.hymnchtv.MainActivity
  import org.cog.hymnchtv.MediaType
  import org.cog.hymnchtv.persistance.DatabaseBackend
  import org.json.JSONArray
  import org.json.JSONObject
  import org.junit.After
  import org.junit.Assert.assertThrows
  import org.junit.Before
  import org.junit.Test
  import org.junit.runner.RunWith

  /** Fixed Notion JSON fixtures fed into a test database: counts, malformed input, overwrite and rollback. */
  @RunWith(AndroidJUnit4::class)
  class NotionStoreTest {
      private val ctx = InstrumentationRegistry.getInstrumentation().targetContext
      private lateinit var db: DatabaseBackend

      @Before
      fun setUp() {
          ctx.deleteDatabase(NAME)
          db = DatabaseBackend.createForTest(ctx, NAME)
      }

      @After
      fun tearDown() {
          db.close()
          ctx.deleteDatabase(NAME)
      }

      private fun link(title: String, url: String): JSONObject =
          JSONObject().put(NotionRecord.NQ_TITLE, title).put(NotionRecord.NQ_URL, url)

      private fun page(vararg items: Any) = JSONArray(items.toList())

      private fun rows(table: String) = DatabaseUtils.queryNumEntries(db.readableDatabase, table)

      private fun uriOf(table: String, no: Int): String? {
          val record = MediaRecord(table, no, false, MediaType.HYMN_JIAOCHANG)
          return if (db.getMediaRecord(record, true)) record.mediaUri else null
      }

      @Test
      fun storesWellFormedLinksAndSkipsMalformedOnes() {
          val fixture = page(
              link("D1但愿荣耀归于圣父", "https://n.example/d1"),
              link("B23羔羊是配", "https://n.example/b23"),
              link("C006朵朵小花含笑", "https://n.example/c6"),
              link("Z9未知诗歌本", "https://n.example/z9"),       // unknown hymn type prefix
              link("D无编号", "https://n.example/none"),           // no hymn number
              link("", "https://n.example/empty"),                // empty title
              JSONObject().put(NotionRecord.NQ_TITLE, "D5缺网址"), // missing url
              "not a JSON object",
          )

          val result = NotionRecord.storeNQJArray(db, fixture, false)

          assertThat(result).isEqualTo(ImportResult(3, 3))
          assertThat(rows(MainActivity.HYMN_DB)).isEqualTo(1L)
          assertThat(rows(MainActivity.HYMN_BB)).isEqualTo(1L)
          assertThat(rows(MainActivity.HYMN_ER)).isEqualTo(1L)
          assertThat(uriOf(MainActivity.HYMN_ER, 6)).isEqualTo("https://n.example/c6")
      }

      @Test
      fun keepsExistingLinksUnlessOverwrite() {
          db.storeMediaRecord(NotionRecord(MainActivity.HYMN_DB, 1).apply { setMediaUri("https://old") })
          val fixture = page(link("D1但愿荣耀归于圣父", "https://new"))

          assertThat(NotionRecord.storeNQJArray(db, fixture, false)).isEqualTo(ImportResult(0, 1))
          assertThat(uriOf(MainActivity.HYMN_DB, 1)).isEqualTo("https://old")

          assertThat(NotionRecord.storeNQJArray(db, fixture, true)).isEqualTo(ImportResult(1, 1))
          assertThat(uriOf(MainActivity.HYMN_DB, 1)).isEqualTo("https://new")
      }

      /** A DB error on the second link must roll back the first one and propagate (no silent partial commit). */
      @Test
      fun databaseErrorRollsBackTheWholePage() {
          db.writableDatabase.execSQL("DROP TABLE ${MainActivity.HYMN_BB}")
          val fixture = page(
              link("D1但愿荣耀归于圣父", "https://n.example/d1"),
              link("B23羔羊是配", "https://n.example/b23"),
          )

          assertThrows(SQLException::class.java) { NotionRecord.storeNQJArray(db, fixture, false) }
          assertThat(rows(MainActivity.HYMN_DB)).isEqualTo(0L)
      }

      private companion object {
          const val NAME = "test-notion-store.db"
      }
  }
  ```

  `hymnchtv/src/androidTest/java/org/cog/hymnchtv/mediaconfig/QQStoreTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.mediaconfig

  import android.database.DatabaseUtils
  import android.database.SQLException
  import androidx.test.ext.junit.runners.AndroidJUnit4
  import androidx.test.platform.app.InstrumentationRegistry
  import com.google.common.truth.Truth.assertThat
  import org.cog.hymnchtv.MainActivity
  import org.cog.hymnchtv.persistance.DatabaseBackend
  import org.json.JSONArray
  import org.json.JSONObject
  import org.junit.After
  import org.junit.Assert.assertThrows
  import org.junit.Before
  import org.junit.Test
  import org.junit.runner.RunWith

  /** Fixed QQ JSON fixtures fed into a test database: counts, malformed input and rollback. */
  @RunWith(AndroidJUnit4::class)
  class QQStoreTest {
      private val ctx = InstrumentationRegistry.getInstrumentation().targetContext
      private lateinit var db: DatabaseBackend

      @Before
      fun setUp() {
          ctx.deleteDatabase(NAME)
          db = DatabaseBackend.createForTest(ctx, NAME)
      }

      @After
      fun tearDown() {
          db.close()
          ctx.deleteDatabase(NAME)
      }

      private fun link(title: String, url: String): JSONObject =
          JSONObject().put(QQRecord.QQ_TITLE, title).put(QQRecord.QQ_URL, url)

      private fun page(vararg items: Any) = JSONArray(items.toList())

      private fun rows(table: String) = DatabaseUtils.queryNumEntries(db.readableDatabase, table)

      @Test
      fun storesWellFormedLinksAndSkipsMalformedOnes() {
          val fixture = page(
              link("D1但愿荣耀归于圣父", "https://q.example/d1"),
              link("B755跟随榜样", "https://q.example/b755"),
              link("Q12青年诗歌", "https://q.example/q12"),            // Q is not a QQ hymn type
              link("D99999999999编号溢位", "https://q.example/huge"),  // NumberFormatException
              link("B没有编号", "https://q.example/none"),
              JSONObject().put(QQRecord.QQ_TITLE, "D2缺网址"),
              "not a JSON object",
          )

          val result = QQRecord.storeQQJArray(db, fixture)

          assertThat(result).isEqualTo(ImportResult(2, 2))
          assertThat(rows(MainActivity.HYMN_DB)).isEqualTo(1L)
          assertThat(rows(MainActivity.HYMN_BB)).isEqualTo(1L)
      }

      @Test
      fun databaseErrorRollsBackTheWholePage() {
          db.writableDatabase.execSQL("DROP TABLE ${MainActivity.HYMN_BB}")
          val fixture = page(
              link("D1但愿荣耀归于圣父", "https://q.example/d1"),
              link("B755跟随榜样", "https://q.example/b755"),
          )

          assertThrows(SQLException::class.java) { QQRecord.storeQQJArray(db, fixture) }
          assertThat(rows(MainActivity.HYMN_DB)).isEqualTo(0L)
      }

      private companion object {
          const val NAME = "test-qq-store.db"
      }
  }
  ```

  Run: `./gradlew :hymnchtv:compileDebugAndroidTestKotlin --console=plain 2>&1 | grep -E "Unresolved reference|BUILD" | head -3`
  Expected: `Unresolved reference 'storeNQJArray'`（或 `storeQQJArray`），`BUILD FAILED`。

- [ ] **Step 2：NotionRecord**

  1. 計數器與 `isOverWrite` 改成 `volatile`（計數器只在單一的 `hymn-io` 執行緒上累加；主執行緒只讀來顯示 toast，或在開始抓取時歸零）：

     ```java
         private static boolean isOverWrite = false;
         private static int mfound = 0;
         private static int mSaved = 0;
     ```

     改成

     ```java
         private static volatile boolean isOverWrite = false;
         // Accumulated only on the AppExecutors IO thread (storeNQPage); read on the main thread for toasts.
         private static volatile int mfound = 0;
         private static volatile int mSaved = 0;
     ```

  2. `saveNQRecord` 裡的 callback：

     ```java
             getURLSource(webView, title, url, data -> {
                 try {
                     JSONArray jsonArray = fetchJsonArray(data, false);
                     if (jsonArray != null && jsonArray.length() != 0) {
                         webList.remove(webView);
                         webView.destroy();
                         for (int i = 0; i < jsonArray.length(); i++) {
                             JSONObject jsonObject = (JSONObject) jsonArray.get(i);
                             storeNQJObject(jsonObject);
                         }
                         Timber.d(HymnsApp.getResString(R.string.nq_download_completed, title, mSaved, mfound));
                         showToastMessage(R.string.nq_download_completed, title, mSaved, mfound);
                     }
                 } catch (JSONException e) {
                     Timber.e("URL get source exception: %s", e.getMessage());
                 }
             });
     ```

     改成（`try/catch JSONException` 必須拿掉：`jsonArray.get(i)` 搬走之後，這段程式已經不會丟出 `JSONException`，留著會編譯失敗）：

     ```java
             getURLSource(webView, title, url, data -> {
                 JSONArray jsonArray = fetchJsonArray(data, false);
                 if (jsonArray != null && jsonArray.length() != 0) {
                     webList.remove(webView);
                     webView.destroy();
                     storeNQPage(title, jsonArray);
                 }
             });
     ```

  3. 把整個 `private static void storeNQJObject(JSONObject jsonRecord) { ... }` 方法換成下面三個方法（方法上方原本的 Javadoc 移到 `parseNQJObject` 上，內容不變）：

     ```java
         private static final Pattern NQ_PATTERN = Pattern.compile("[DBCQX](\\d+)");
         private static final Pattern NQ_FU_PATTERN = Pattern.compile("Q*附([1-9])");
         private static final Pattern NQ_YB_PATTERN = Pattern.compile("Q(\\d+)(首B|\\(B\\))+");

         /**
          * (original Javadoc of storeNQJObject)
          *
          * @return the record to store, or null when the JSON is malformed (logged and skipped). Never touches the DB.
          */
         @VisibleForTesting
         public static NotionRecord parseNQJObject(JSONObject jsonRecord) {
             try {
                 String title = jsonRecord.getString(NQ_TITLE);
                 String hymnType = title.isEmpty() ? null : nqHymn2Type.get(title.substring(0, 1));
                 if (TextUtils.isEmpty(hymnType)) {
                     Timber.w("### Invalid Notion record HymnTitle: %s", jsonRecord);
                     return null;
                 }

                 int hymnNo = 0;
                 Matcher matcher;
                 if (title.contains("附")) {
                     hymnNo = title.startsWith("Q") ? HYMN_YB_NO_MAX : HYMN_DB_NO_MAX;
                     matcher = NQ_FU_PATTERN.matcher(title);
                 }
                 else {
                     matcher = NQ_PATTERN.matcher(title);
                 }
                 if (!matcher.find() || TextUtils.isEmpty(matcher.group(1))) {
                     Timber.w("### Invalid Notion record HymnNo: %s", jsonRecord);
                     return null;
                 }
                 hymnNo += Integer.parseInt(matcher.group(1));

                 // renumber 青年诗歌 alternate hymn No,
                 if (HYMN_YB.equals(hymnType) && NQ_YB_PATTERN.matcher(title).find()) {
                     Integer altNo = HYMN_YB_ALT.get(hymnNo);
                     if (altNo != null)
                         hymnNo = altNo;
                 }

                 NotionRecord mRecord = new NotionRecord(hymnType, hymnNo);
                 mRecord.setMediaUri(jsonRecord.getString(NQ_URL));
                 return mRecord;
             } catch (JSONException | NumberFormatException e) {
                 Timber.w("### Invalid Notion record (%s): %s", e.getMessage(), jsonRecord);
                 return null;
             }
         }

         /**
          * Store one page of Notion links in a single transaction. Malformed items are skipped; any SQL error
          * propagates as android.database.SQLException after the whole page has been rolled back.
          *
          * @return ImportResult(saved, found) for this page only
          */
         @VisibleForTesting
         @WorkerThread
         public static ImportResult storeNQJArray(DatabaseBackend db, JSONArray jsonArray, boolean overWrite) {
             return db.inTransaction(() -> {
                 int found = 0;
                 int saved = 0;
                 for (int i = 0; i < jsonArray.length(); i++) {
                     JSONObject jsonObject = jsonArray.optJSONObject(i);
                     NotionRecord mRecord = (jsonObject == null) ? null : parseNQJObject(jsonObject);
                     if (mRecord == null)
                         continue;

                     found++;
                     if (overWrite || !db.getMediaRecord(mRecord, false)) {
                         db.storeMediaRecordOrThrow(mRecord);
                         saved++;
                     }
                 }
                 return new ImportResult(saved, found);
             });
         }

         /**
          * Store one fetched page on the shared IO thread (B-9a) and report with the same per-page toast as before.
          * Captures only immutable inputs; no Activity.
          */
         private static void storeNQPage(String title, JSONArray jsonArray) {
             final boolean overWrite = isOverWrite;
             AppExecutors.io("notion-store", () -> {
                 try {
                     ImportResult result = storeNQJArray(DatabaseBackend.getInstance(HymnsApp.getGlobalContext()), jsonArray, overWrite);
                     mfound += result.getTotal();
                     mSaved += result.getImported();
                     Timber.d(HymnsApp.getResString(R.string.nq_download_completed, title, mSaved, mfound));
                     showToastMessage(R.string.nq_download_completed, title, mSaved, mfound);
                 } catch (SQLException e) {
                     Timber.e(e, "Notion page rolled back: %s", title);
                     showToastMessage(R.string.nq_download_failed, title);
                 }
             });
         }
     ```

  4. import 區加入：

     ```java
     import android.database.SQLException;

     import androidx.annotation.VisibleForTesting;
     import androidx.annotation.WorkerThread;

     import org.cog.hymnchtv.concurrent.AppExecutors;
     ```

     `JSONException` 的 import 保留（`saveNQRecord` 的 `throws` 和其他方法還在用）。

- [ ] **Step 3：QQRecord**

  1. 計數器：

     ```java
         private static int mFound = 0;
         private static int mSaved = 0;
     ```

     改成

     ```java
         // Accumulated only on the AppExecutors IO thread (storeQQPage); read on the main thread for toasts.
         private static volatile int mFound = 0;
         private static volatile int mSaved = 0;
     ```

  2. `saveQQRecord` 裡的 callback：

     ```java
             getURLSource(webView, title, url, data -> {
                 try {
                     JSONArray jsonArray = createJsonArray(title, data);
                     if (jsonArray != null && jsonArray.length() != 0) {
                         webView.destroy();
                         for (int i = 0; i < jsonArray.length(); i++) {
                             JSONObject jsonObject = (JSONObject) jsonArray.get(i);
                             storeQQJObject(jsonObject);
                         }
                         Timber.d(HymnsApp.getResString(R.string.nq_download_completed, title, mSaved, mFound));
                         showToastMessage(R.string.nq_download_completed, title, mSaved, mFound);
                     }
                 } catch (JSONException e) {
                     Timber.e("URL get source exception: %s", e.getMessage());
                 }
             });
     ```

     改成

     ```java
             getURLSource(webView, title, url, data -> {
                 JSONArray jsonArray = createJsonArray(title, data);
                 if (jsonArray != null && jsonArray.length() != 0) {
                     webView.destroy();
                     storeQQPage(title, jsonArray);
                 }
             });
     ```

  3. 把整個 `private static void storeQQJObject(JSONObject jsonRecord) { ... }` 換成下面三個方法（原本的 Javadoc 移到 `parseQQJObject` 上）。QQ 原本就沒有「不覆寫」的檢查，維持一律寫入：

     ```java
         private static final Pattern QQ_PATTERN = Pattern.compile("[DBCX](\\d+)");

         /**
          * (original Javadoc of storeQQJObject)
          *
          * @return the record to store, or null when the JSON is malformed (logged and skipped). Never touches the DB.
          */
         @VisibleForTesting
         public static QQRecord parseQQJObject(JSONObject jsonRecord) {
             try {
                 String title = jsonRecord.getString(QQ_TITLE);
                 String hymnType = title.isEmpty() ? null : qqHymn2Type.get(title.substring(0, 1));
                 if (TextUtils.isEmpty(hymnType)) {
                     Timber.w("### Invalid QQ record HymnTitle: %s", jsonRecord);
                     return null;
                 }

                 Matcher matcher = QQ_PATTERN.matcher(title);
                 if (!matcher.find() || TextUtils.isEmpty(matcher.group(1))) {
                     Timber.w("### Invalid QQ record HymnNo: %s", jsonRecord);
                     return null;
                 }

                 QQRecord mRecord = new QQRecord(hymnType, Integer.parseInt(matcher.group(1)));
                 mRecord.setMediaUri(jsonRecord.getString(QQ_URL));
                 return mRecord;
             } catch (JSONException | NumberFormatException e) {
                 Timber.w("### Invalid QQ record (%s): %s", e.getMessage(), jsonRecord);
                 return null;
             }
         }

         /**
          * Store one page of QQ links in a single transaction. Malformed items are skipped; any SQL error
          * propagates as android.database.SQLException after the whole page has been rolled back.
          *
          * @return ImportResult(saved, found) for this page only
          */
         @VisibleForTesting
         @WorkerThread
         public static ImportResult storeQQJArray(DatabaseBackend db, JSONArray jsonArray) {
             return db.inTransaction(() -> {
                 int found = 0;
                 for (int i = 0; i < jsonArray.length(); i++) {
                     JSONObject jsonObject = jsonArray.optJSONObject(i);
                     QQRecord mRecord = (jsonObject == null) ? null : parseQQJObject(jsonObject);
                     if (mRecord == null)
                         continue;

                     found++;
                     db.storeMediaRecordOrThrow(mRecord);
                 }
                 return new ImportResult(found, found);
             });
         }

         /**
          * Store one fetched page on the shared IO thread (B-9a) and report with the same per-page toast as before.
          * Captures only immutable inputs; no Activity.
          */
         private static void storeQQPage(String title, JSONArray jsonArray) {
             AppExecutors.io("qq-store", () -> {
                 try {
                     ImportResult result = storeQQJArray(DatabaseBackend.getInstance(HymnsApp.getGlobalContext()), jsonArray);
                     mFound += result.getTotal();
                     mSaved += result.getImported();
                     Timber.d(HymnsApp.getResString(R.string.nq_download_completed, title, mSaved, mFound));
                     showToastMessage(R.string.nq_download_completed, title, mSaved, mFound);
                 } catch (SQLException e) {
                     Timber.e(e, "QQ page rolled back: %s", title);
                     showToastMessage(R.string.nq_download_failed, title);
                 }
             });
         }
     ```

  4. import 區加入：

     ```java
     import android.database.SQLException;

     import androidx.annotation.VisibleForTesting;
     import androidx.annotation.WorkerThread;

     import org.cog.hymnchtv.concurrent.AppExecutors;
     ```

- [ ] **Step 4：執行測試，確認通過**

  ```bash
  ./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug --console=plain | tail -1
  export ANDROID_SERIAL=$(tools/perf/serial_for.sh api34)
  tools/perf/emu_lock.sh ./gradlew :hymnchtv:connectedDebugAndroidTest --console=plain \
    -Pandroid.testInstrumentationRunnerArguments.class=org.cog.hymnchtv.mediaconfig.NotionStoreTest,org.cog.hymnchtv.mediaconfig.QQStoreTest | tail -3
  ```

  Expected: 兩個都是 `BUILD SUCCESSFUL`；5 個 instrumented 測試通過（Notion 3、QQ 2）。

  如果 `storesWellFormedLinksAndSkipsMalformedOnes` 的數字和預期不同，先確認是 fixture 的假設錯了（例如 `C006` 的編號驗證），還是解析邏輯和原本的 `storeNQJObject`／`storeQQJObject` 不一致；後者要修實作，前者停下來回報，不要直接改預期值。

- [ ] **Step 5：確認沒有遺留的主執行緒寫入與吞掉的 DB 錯誤**

  ```bash
  grep -n "storeNQJObject\|storeQQJObject\|mDB.storeMediaRecord(" hymnchtv/src/main/java/org/cog/hymnchtv/mediaconfig/NotionRecord.java hymnchtv/src/main/java/org/cog/hymnchtv/mediaconfig/QQRecord.java
  grep -n "catch (Exception" hymnchtv/src/main/java/org/cog/hymnchtv/mediaconfig/NotionRecord.java
  ```

  Expected: 第一個 `grep` 沒有輸出（舊方法已刪除，沒有其他呼叫點）。第二個 `grep` 列出的 `catch (Exception` 都不包住任何 DB 呼叫（逐一看過，在回報中說明）。

- [ ] **Step 6（選擇性）：手動測試**

  模擬器有網路時：媒體設定 → 按「NQ⬇」→ 確認。
  Expected：按鈕變灰；陸續出現每頁的「完成」toast；`adb logcat -d -s StrictMode` 不再出現 `storeMediaRecord` 的 DiskWrite 違規。網站無法連線時記錄「無法測試」即可；自動化的正確性驗證已由 Step 4 完成。

- [ ] **Step 7：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/mediaconfig/NotionRecord.java hymnchtv/src/main/java/org/cog/hymnchtv/mediaconfig/QQRecord.java hymnchtv/src/androidTest/java/org/cog/hymnchtv/mediaconfig/NotionStoreTest.kt hymnchtv/src/androidTest/java/org/cog/hymnchtv/mediaconfig/QQStoreTest.kt
  git commit -m "perf: store Notion and QQ link pages in one transaction off the main thread

  Parsing is separated from writing; SQL errors now roll the whole page back and show the failure toast.

  Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

  Lane B 完成，回報兩個 commit 的 SHA，以及 Task B1 Step 7 的兩列量測。

---

## Lane C：啟動（worktree `hymnchtv-b-startup`，只改 `MainActivity.java`、`HymnsApp.java`、`UpdateServiceImpl.java`，新增 `toc/`、測試）

**和 A2 的衝突風險：** A2 也在改 `MainActivity.java`（桌布區塊，約 1259 行附近和 `initUserSettings`）。這條 lane 只動下列位置，**不要順手整理其他程式碼**：
- import 區（新增 2 行、刪除 `java.util.HashMap`）
- `mTocYB`（約 152 行）、`ybXTable`（約 164 行）兩個欄位
- `onCreate` 裡的 `createYbXTable();`（約 260 行）
- `handleIntent` 的第一行（約 377 行）
- `showContent` 開頭的歷史寫入（約 630～634 行）
- 刪除 `createYbXTable()` 方法（約 1106～1128 行）

### Task C1：刪掉 `handleIntent` 裡多呼叫的 `super.onStart()`（B-7）

**Files:**
- Modify: `hymnchtv/src/main/java/org/cog/hymnchtv/MainActivity.java`（`handleIntent`）

**Prerequisites:** Task 1。

`handleIntent` 在 `onCreate` 和 `onNewIntent` 都會呼叫；它第一行的 `super.onStart()` 會讓 `AppCompatActivity`／fragment 的 start 流程被多跑一次，這是 bug。沒有合適的自動測試（`MainActivity` 啟動會跳權限與 changelog，見 A plan 的說明），用 adb 驗證分享 intent 的行為不變。

- [ ] **Step 1：記錄修改前的行為**

  ```bash
  export ANDROID_SERIAL=$(tools/perf/serial_for.sh api34)
  tools/perf/emu_lock.sh tools/perf/install_apk.sh debug
  adb shell am force-stop org.cog.hymnchtv
  adb shell am start -W -a android.intent.action.SEND -t text/plain \
    --es android.intent.extra.TEXT "https://youtu.be/vw_rDfb0nuc" -n org.cog.hymnchtv/.MainActivity
  sleep 3
  adb shell dumpsys activity activities | grep -E "topResumedActivity|mResumedActivity" | head -1
  ```

  Expected: 顯示的 resumed activity 是 `org.cog.hymnchtv/.mediaconfig.MediaConfig`。

- [ ] **Step 2：修改**

  在 `handleIntent(Intent intent)` 裡刪除第一行：

  ```java
          super.onStart();
  ```

- [ ] **Step 3：驗證冷啟動與 `onNewIntent` 兩條路徑**

  ```bash
  tools/perf/emu_lock.sh tools/perf/install_apk.sh debug
  # cold
  adb shell am force-stop org.cog.hymnchtv
  adb shell am start -W -a android.intent.action.SEND -t text/plain \
    --es android.intent.extra.TEXT "https://youtu.be/vw_rDfb0nuc" -n org.cog.hymnchtv/.MainActivity
  sleep 3
  adb shell dumpsys activity activities | grep -E "topResumedActivity|mResumedActivity" | head -1
  # warm (singleTask → onNewIntent)
  adb shell input keyevent KEYCODE_BACK
  sleep 1
  adb shell am start -W -a android.intent.action.SEND -t text/plain \
    --es android.intent.extra.TEXT "https://youtu.be/vw_rDfb0nuc" -n org.cog.hymnchtv/.MainActivity
  sleep 3
  adb shell dumpsys activity activities | grep -E "topResumedActivity|mResumedActivity" | head -1
  adb logcat -d | grep -E "FATAL|IllegalStateException" | head -3
  ```

  Expected: 兩次都顯示 `MediaConfig`；最後的 `grep` 沒有輸出。

- [ ] **Step 4：Commit**

  ```bash
  ./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug --console=plain | tail -1
  git add hymnchtv/src/main/java/org/cog/hymnchtv/MainActivity.java
  git commit -m "fix: stop calling super.onStart() from handleIntent

  Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

### Task C2：YB 對照表改成 lazy 載入，啟動時在背景預熱（B-7）

**Files:**
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/toc/LazyMap.kt`
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/toc/YbCrossRef.kt`
- Create: `hymnchtv/src/test/java/org/cog/hymnchtv/toc/LazyMapTest.kt`
- Create: `hymnchtv/src/test/java/org/cog/hymnchtv/toc/YbCrossRefTest.kt`
- Create: `hymnchtv/src/androidTest/java/org/cog/hymnchtv/toc/YbCrossRefAssetTest.kt`
- Modify: `hymnchtv/src/main/java/org/cog/hymnchtv/MainActivity.java`

**Prerequisites:** Task C1。

**讀取 `ybXTable` 的地方：** `ContentHandler.java:880, 1267`、`HistoryRecord.getHymnInfoFromFile`。這些都不用改：`MainActivity.ybXTable` 的型別仍是 `Map<Integer, String>`，只是內容改成第一次存取時才載入。寫入的地方只有 `createYbXTable()`，這個 task 會刪掉它。

**預熱排在共用的單一執行緒上，可以接受嗎（Codex P2）：** 可以，理由如下。
- 預熱是在 `MainActivity.onCreate` 排進 `hymn-io` 的；那個時間點，唯一可能排在它前面的是 `HymnsApp.onCreate` 的清除舊 APK（毫秒級）。長的匯入只能由使用者在媒體設定畫面觸發；`UpdateServiceImpl` 的自動匯入跑在 `IntentService` 自己的執行緒上，不佔 `hymn-io`。所以實務上預熱不會被匯入卡住。
- 即使被卡住（例如匯入中 process 被回收，再從媒體設定還原並立刻開 YB 詩歌），`LazyMap` 會在第一次存取的執行緒上同步載入，也就是**主執行緒有可能付這個成本**，計畫不宣稱主執行緒永遠不付。這個檔案只有 9 KB、113 筆，預期最壞情況是幾毫秒；實際數字由 `YbCrossRefAssetTest.mainThreadFirstUseCost` 在 api24 量測（Task F Step 6），寫進量測文件，格式為「最壞情況（主執行緒首次使用）：api24 X ms」。如果 api24 的首次值超過 16 ms（一個幀），在決策紀錄中標註，並在第二波評估是否改用獨立的預熱 executor。結果正確性由 `LazyMapTest.concurrentFirstAccessLoadsOnceAndAgrees` 保證（只載入一次、所有執行緒看到相同內容）。
- 因此不另外開一個 executor（多一個執行緒只為了一個 9 KB 檔案不划算）。

**資產格式（已確認）：** `lyrics_toc/toc_yb_toc.txt` 共 277 行、CRLF、每行 3 個以空白分隔的欄位，例如 `#0001 神就是爱 #bb876`。第三欄是 `#yb…` 的有 164 行（不放進表），其餘 113 行指向 `bb`（71）或 `xb`（42）。

- [ ] **Step 1：寫會失敗的 JVM 測試**

  `hymnchtv/src/test/java/org/cog/hymnchtv/toc/LazyMapTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.toc

  import com.google.common.truth.Truth.assertThat
  import org.junit.Test

  class LazyMapTest {
      @Test
      fun loadsOnFirstAccessOnly() {
          var loads = 0
          val map = LazyMap { loads++; mapOf(1 to "a") }
          assertThat(loads).isEqualTo(0)

          assertThat(map[1]).isEqualTo("a")
          assertThat(map.size).isEqualTo(1)
          assertThat(map.containsKey(2)).isFalse()
          assertThat(loads).isEqualTo(1)
      }

      @Test
      fun isReadOnly() {
          val map = LazyMap { mapOf(1 to "a") } as MutableMap<Int, String>
          try {
              map[2] = "b"
              throw AssertionError("put should be unsupported")
          } catch (expected: UnsupportedOperationException) {
              assertThat(map).containsExactly(1, "a")
          }
      }

      /** Many threads hitting a cold map at once: exactly one load, and every thread sees the same contents. */
      @Test
      fun concurrentFirstAccessLoadsOnceAndAgrees() {
          val loads = AtomicInteger()
          val map = LazyMap {
              loads.incrementAndGet()
              Thread.sleep(50) // widen the race window
              mapOf(1 to "bb876", 245 to "xb161")
          }
          val threads = 16
          val pool = Executors.newFixedThreadPool(threads)
          val start = CountDownLatch(1)
          try {
              val futures = (1..threads).map {
                  pool.submit<String?> {
                      start.await()
                      map[245]
                  }
              }
              start.countDown()
              val results = futures.map { it.get(5, TimeUnit.SECONDS) }

              assertThat(loads.get()).isEqualTo(1)
              assertThat(results.toSet()).containsExactly("xb161")
          } finally {
              pool.shutdownNow()
          }
      }
  }
  ```

  這個測試檔需要的 import：`java.util.concurrent.CountDownLatch`、`java.util.concurrent.Executors`、`java.util.concurrent.TimeUnit`、`java.util.concurrent.atomic.AtomicInteger`（加在 `import org.junit.Test` 之後）。

  `hymnchtv/src/test/java/org/cog/hymnchtv/toc/YbCrossRefTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.toc

  import com.google.common.truth.Truth.assertThat
  import org.junit.Test
  import java.io.File

  class YbCrossRefTest {
      @Test
      fun parsesCrossReferencesAndSkipsYbToYb() {
          val text = "#0001 神就是爱 #bb876\r\n#0022 将一生奉献 #yb22\r\n#0245 你是我的喜乐冠冕 #xb161"
          assertThat(YbCrossRef.parse(text)).containsExactly(1, "bb876", 245, "xb161").inOrder()
      }

      @Test
      fun skipsMalformedLines() {
          val text = "\n#abc 标题 #bb1\n#0002 只有两栏\n#0003 标题 #\n#0004 标题 #bb9"
          assertThat(YbCrossRef.parse(text)).containsExactly(4, "bb9")
      }

      @Test
      fun bundledAssetHas113CrossReferences() {
          val assets = System.getProperty("hymnchtv.assetsDir")
          val table = YbCrossRef.parse(File(assets, YbCrossRef.ASSET).readText(Charsets.UTF_8))
          assertThat(table).hasSize(113)
          assertThat(table[1]).isEqualTo("bb876")
          assertThat(table[245]).isEqualTo("xb161")
          assertThat(table).doesNotContainKey(22)
      }
  }
  ```

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.toc.*'`
  Expected: 編譯失敗，`Unresolved reference 'LazyMap'`、`Unresolved reference 'YbCrossRef'`。

- [ ] **Step 2：實作** `hymnchtv/src/main/java/org/cog/hymnchtv/toc/LazyMap.kt`

  ```kotlin
  package org.cog.hymnchtv.toc

  /** Read-only map whose contents are produced once, thread-safely, on first access. */
  internal class LazyMap<K, V>(loader: () -> Map<K, V>) : AbstractMap<K, V>() {
      private val delegate: Map<K, V> by lazy(LazyThreadSafetyMode.SYNCHRONIZED, loader)

      override val entries: Set<Map.Entry<K, V>> get() = delegate.entries
      override val size: Int get() = delegate.size
      override fun get(key: K): V? = delegate[key]
      override fun containsKey(key: K): Boolean = delegate.containsKey(key)
  }
  ```

  `hymnchtv/src/main/java/org/cog/hymnchtv/toc/YbCrossRef.kt`：

  ```kotlin
  package org.cog.hymnchtv.toc

  import android.os.SystemClock
  import androidx.annotation.VisibleForTesting
  import org.cog.hymnchtv.HymnsApp
  import timber.log.Timber
  import java.io.IOException

  /**
   * YB (青年诗歌) numbers whose lyrics live in another book, e.g. 1 -> "bb876". Replaces
   * MainActivity.createYbXTable(): the table loads lazily and thread-safely on first use, so it is also
   * correct when the process is restored straight into ContentHandler (MainActivity.onCreate never ran).
   */
  object YbCrossRef {
      const val ASSET = "lyrics_toc/toc_yb_toc.txt"

      private val WHITESPACE = Regex("\\s+")

      /** Read-only. The first access loads the asset (blocking briefly) unless [prewarm] already did. */
      @JvmField
      val TABLE: Map<Int, String> = LazyMap { load() }

      /**
       * Call on a background thread at startup so the main thread usually does not pay for the first load.
       * It still can (e.g. a YB hymn opened before the warm-up ran); that synchronous load is small and measured
       * by YbCrossRefAssetTest.mainThreadFirstUseCost (see the measurements doc).
       */
      @JvmStatic
      fun prewarm() {
          TABLE.isEmpty()
      }

      /** Lines look like "#0001 神就是爱 #bb876"; YB-to-YB entries and malformed lines are skipped. */
      @JvmStatic
      fun parse(text: String): Map<Int, String> =
          text.lineSequence().mapNotNull(::parseLine).toMap()

      private fun parseLine(line: String): Pair<Int, String>? {
          val tokens = line.trim().split(WHITESPACE)
          if (tokens.size < 3) return null
          val number = tokens[0].removePrefix("#").toIntOrNull() ?: return null
          val target = tokens[2].removePrefix("#")
          if (target.isEmpty() || target.startsWith("yb")) return null
          return number to target
      }

      private fun load(): Map<Int, String> {
          val start = SystemClock.elapsedRealtime()
          val table = loadFromAssets()
          Timber.i("perf: YB cross-reference loaded (%d entries) in %d ms on %s",
              table.size, SystemClock.elapsedRealtime() - start, Thread.currentThread().name)
          return table
      }

      /** Reads and parses the asset every call; [TABLE] calls it once. Visible for the first-use cost test. */
      @VisibleForTesting
      internal fun loadFromAssets(): Map<Int, String> = try {
          HymnsApp.getAppResources().assets.open(ASSET).bufferedReader(Charsets.UTF_8).use { parse(it.readText()) }
      } catch (e: IOException) {
          Timber.w(e, "YB cross-reference not available: %s", ASSET)
          emptyMap()
      }
  }
  ```

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.toc.*'`
  Expected: BUILD SUCCESSFUL，6 個測試通過（LazyMap 3、YbCrossRef 3）。

- [ ] **Step 3：instrumented test，確認在裝置上從 assets 載入** `hymnchtv/src/androidTest/java/org/cog/hymnchtv/toc/YbCrossRefAssetTest.kt`

  ```kotlin
  package org.cog.hymnchtv.toc

  import android.os.SystemClock
  import android.util.Log
  import androidx.test.ext.junit.runners.AndroidJUnit4
  import androidx.test.platform.app.InstrumentationRegistry
  import com.google.common.truth.Truth.assertThat
  import org.cog.hymnchtv.MainActivity
  import org.junit.Test
  import org.junit.runner.RunWith

  @RunWith(AndroidJUnit4::class)
  class YbCrossRefAssetTest {
      @Test
      fun mainActivityTableLoadsFromAssetsWithoutMainActivityOnCreate() {
          assertThat(MainActivity.ybXTable[1]).isEqualTo("bb876")
          assertThat(MainActivity.ybXTable).hasSize(113)
      }

      /**
       * Worst case of the lazy design: the first use happens on the main thread before the warm-up ran.
       * Times the exact load path (asset open + parse) on the main thread. Logged to tag "YbPerf" for the
       * measurements doc; the bound is only a sanity guard against a pathological regression.
       */
      @Test
      fun mainThreadFirstUseCost() {
          val samples = mutableListOf<Long>()
          repeat(6) {
              InstrumentationRegistry.getInstrumentation().runOnMainSync {
                  val start = SystemClock.elapsedRealtime()
                  YbCrossRef.loadFromAssets()
                  samples += SystemClock.elapsedRealtime() - start
              }
          }
          val first = samples.first()
          val median = samples.drop(1).sorted()[2]
          Log.i("YbPerf", "main_thread_first_ms=$first median_ms=$median samples=${samples.joinToString(",")}")
          assertThat(first).isLessThan(100L)
      }
  }
  ```

  （`samples` 是測試內部的區域累加，不屬於產品程式碼的 immutability 規則範圍。第一個樣本最接近真實的首次使用，因為之後的幾次 asset 已經在 page cache 裡。）

- [ ] **Step 4：修改 `MainActivity.java`**

  1. 欄位：

     ```java
         public static final String mTocYB = "lyrics_toc/toc_yb_toc.txt";
     ```

     改成

     ```java
         public static final String mTocYB = YbCrossRef.ASSET;
     ```

     ```java
         public static final Map<Integer, String> ybXTable = new HashMap<>();
     ```

     改成

     ```java
         // YB -> other hymn book cross-reference: loaded lazily and thread-safely, prewarmed off the main thread in onCreate.
         public static final Map<Integer, String> ybXTable = YbCrossRef.TABLE;
     ```

  2. `onCreate` 裡：

     ```java
         initUserSettings();
         createYbXTable();
     ```

     改成

     ```java
         initUserSettings();
         AppExecutors.io("yb-xref-prewarm", YbCrossRef::prewarm);
     ```

  3. 刪除整個方法（含上面的註解行）：

     ```java
         // Create the YB hymn cross-reference table for use in History record and PagerSlider
         private void createYbXTable() {
             ...
         }
     ```

  4. import 區：刪除 `import java.util.HashMap;`（先 `grep -n "HashMap" hymnchtv/src/main/java/org/cog/hymnchtv/MainActivity.java` 確認只剩 import 那一行），加入：

     ```java
     import org.cog.hymnchtv.concurrent.AppExecutors;
     import org.cog.hymnchtv.toc.YbCrossRef;
     ```

     `EncodingUtils`、`InputStream`、`IOException` 的 import 要保留（`MainActivity` 約 688 行還在用）。

- [ ] **Step 5：執行測試**

  ```bash
  ./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug --console=plain | tail -1
  export ANDROID_SERIAL=$(tools/perf/serial_for.sh api34)
  tools/perf/emu_lock.sh ./gradlew :hymnchtv:connectedDebugAndroidTest --console=plain \
    -Pandroid.testInstrumentationRunnerArguments.class=org.cog.hymnchtv.toc.YbCrossRefAssetTest | tail -3
  ```

  Expected: 兩個都是 `BUILD SUCCESSFUL`；instrumented 2 個測試通過（第一個測試沒有啟動 `MainActivity`，證明表不再依賴 `onCreate`）。`adb logcat -d -s YbPerf:I` 有一行 `main_thread_first_ms=…`。

- [ ] **Step 6：手動確認 YB 詩歌仍然指向正確的歌詞**

  ```bash
  tools/perf/emu_lock.sh tools/perf/install_apk.sh debug
  adb shell am start -W -S -n org.cog.hymnchtv/.MainActivity >/dev/null; sleep 3
  source tools/perf/ui.sh; tap_id n1; tap_id bs_yb; sleep 3
  dump_ui | grep -o 'text="[^"]*神就是爱[^"]*"' | head -1
  ```

  Expected: 印出含「神就是爱」的文字（青年詩歌 1 首借用補充本 876 首的歌詞，標題就是這個）。如果畫面上是繁體歌詞，改 grep「神就是愛」。如果標題在 WebView 裡、`uiautomator` 抓不到，改用截圖確認：`adb exec-out screencap -p > build/perf/yb1.png`，打開圖片看歌詞是否為「神就是爱」，並在回報中附上路徑。

- [ ] **Step 7：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/toc hymnchtv/src/test/java/org/cog/hymnchtv/toc hymnchtv/src/androidTest/java/org/cog/hymnchtv/toc hymnchtv/src/main/java/org/cog/hymnchtv/MainActivity.java
  git commit -m "perf: load the YB cross-reference lazily and prewarm it off the main thread

  Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

### Task C3：清除舊 APK 移到背景；更新服務延後到首幀之後，改用 `ProcessLifecycleOwner` 判斷前景（B-7）

**Files:**
- Modify: `hymnchtv/src/main/java/org/cog/hymnchtv/HymnsApp.java`（`onCreate`、`startUpdateService`）
- Modify: `hymnchtv/src/main/java/org/cog/hymnchtv/service/androidupdate/UpdateServiceImpl.java`（`getInstance`）

**Prerequisites:** Task C2。

**現況：**
- `HymnsApp.onCreate` 最後呼叫 `UpdateServiceImpl.getInstance().removeOldDownloads()`：讀 SharedPreferences，並對每個舊下載 ID 呼叫 `DownloadManager.remove`（IPC + 磁碟）。
- `ProcessLifecycleOwner` 的 `ON_START`（主執行緒）呼叫 `startUpdateService()`：`getRunningAppProcesses()`（IPC）+ `startService`。而且它用 `runningAppProcesses.get(0)` 判斷前景，但清單的第一筆不保證是自己的 process（Codex P2）。
- `UpdateServiceImpl.getInstance()` 不是 thread-safe；清除舊 APK 搬到背景後，可能和 `OnlineUpdateService`（`IntentService` 的執行緒）同時建立兩個 instance。

**設計：**
- 清除舊 APK：搬到 `AppExecutors.io`（只碰 prefs 和 `DownloadManager`，不碰 UI）。
- 更新服務：**整段留在主執行緒**，只是延後到主執行緒閒置時（第一個畫面之後）。在真正呼叫 `startService` 之前，於主執行緒上用 `ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(STARTED)` 再確認一次前景；不在前景就不啟動，等下一次 `ON_START` 再試。完全不再呼叫 `getRunningAppProcesses()`。`startService` 仍包在 `try/catch IllegalStateException` 裡，作為最後一道保護。
- `startService` 本身是一次非同步的 binder 呼叫，不等待 service 啟動完成，所以留在主執行緒的成本很小；延後到閒置時只是避免和第一個畫面搶時間。

- [ ] **Step 1：`UpdateServiceImpl.getInstance()` 加上 `synchronized`**

  ```java
      public static UpdateServiceImpl getInstance() {
  ```

  改成

  ```java
      public static synchronized UpdateServiceImpl getInstance() {
  ```

- [ ] **Step 2：`HymnsApp` 修改**

  1. `onCreate` 裡：

     ```java
             // Purge all the previously old downloaded apk
             UpdateServiceImpl.getInstance().removeOldDownloads();
     ```

     改成

     ```java
             // Purge all the previously old downloaded apk; DownloadManager IPC and prefs, so off the main thread (B-7)
             AppExecutors.io("remove-old-apks", () -> UpdateServiceImpl.getInstance().removeOldDownloads());
     ```

  2. 整個 `startUpdateService()` 方法換成：

     ```java
         /**
          * Start online service only when app is in the foreground, before going back to background upon detect
          * the device screen is locked. Deferred until the main thread is idle (after the first frame); the
          * foreground check uses ProcessLifecycleOwner on the main thread right before startService (B-7).
          */
         @MainThread
         private static void startUpdateService() {
             if (!(updateServiceAllowed || BuildConfig.DEBUG) || isUpdateServerStarted) {
                 return;
             }
             Looper.myQueue().addIdleHandler(() -> {
                 startUpdateServiceIfStillForeground();
                 return false;
             });
         }

         @MainThread
         private static void startUpdateServiceIfStillForeground() {
             if (isUpdateServerStarted) {
                 return;
             }
             Lifecycle.State state = ProcessLifecycleOwner.get().getLifecycle().getCurrentState();
             if (!state.isAtLeast(Lifecycle.State.STARTED)) {
                 // Went to the background before the main thread became idle; the next ON_START retries.
                 return;
             }
             Intent dailyCheckupIntent = new Intent(mInstance, OnlineUpdateService.class);
             dailyCheckupIntent.setAction(OnlineUpdateService.ACTION_AUTO_UPDATE_START);
             try {
                 mInstance.startService(dailyCheckupIntent);
                 isUpdateServerStarted = true;
                 Timber.d("### Online hymnchtv app update service started!");
             } catch (IllegalStateException e) {
                 Timber.w("Update service not started: %s", e.getMessage());
             }
         }
     ```

     兩個 static 欄位 `updateServiceAllowed`、`isUpdateServerStarted` 都只在主執行緒讀寫，不用改。

  3. import 區：
     - 加入 `import androidx.annotation.MainThread;` 和 `import org.cog.hymnchtv.concurrent.AppExecutors;`（`Looper`、`Lifecycle`、`ProcessLifecycleOwner` 原本就有 import）。
     - 刪除 `import android.app.ActivityManager;` 和 `import java.util.List;`：先用 `grep -n "ActivityManager\|List<" hymnchtv/src/main/java/org/cog/hymnchtv/HymnsApp.java` 確認只剩 import 那兩行。

- [ ] **Step 3：驗證前景與背景兩種情況**

  ```bash
  ./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug --console=plain | tail -1
  grep -n "getRunningAppProcesses" hymnchtv/src/main/java/org/cog/hymnchtv/HymnsApp.java || echo "no getRunningAppProcesses"
  export ANDROID_SERIAL=$(tools/perf/serial_for.sh api34)
  tools/perf/emu_lock.sh tools/perf/install_apk.sh debug

  # 1) normal foreground start
  adb logcat -c
  adb shell am start -W -S -n org.cog.hymnchtv/.MainActivity >/dev/null; sleep 5
  adb logcat -d | grep -E "Online hymnchtv app update service started|Update service not started|FATAL" | head -3

  # 2) leave immediately: HOME right after launch, then come back
  adb logcat -c
  adb shell am force-stop org.cog.hymnchtv
  adb shell am start -n org.cog.hymnchtv/.MainActivity >/dev/null; adb shell input keyevent KEYCODE_HOME
  sleep 3
  adb shell am start -W -n org.cog.hymnchtv/.MainActivity >/dev/null; sleep 5
  adb logcat -d | grep -E "Online hymnchtv app update service started|Update service not started|FATAL" | head -3
  ```

  Expected:
  - `BUILD SUCCESSFUL`；`grep` 印出 `no getRunningAppProcesses`。
  - 情況 1：有 `### Online hymnchtv app update service started!`，沒有 `FATAL`。
  - 情況 2：沒有 `FATAL`；最後一定有一行 `### Online hymnchtv app update service started!`（如果按 HOME 時 idle handler 還沒執行，它會因為不在前景而跳過，回到前景的 `ON_START` 再啟動；如果已經執行過，也只會出現一次）。

- [ ] **Step 4：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/HymnsApp.java hymnchtv/src/main/java/org/cog/hymnchtv/service/androidupdate/UpdateServiceImpl.java
  git commit -m "perf: move old-apk cleanup off the main thread; start update service after first frame

  Foreground is re-checked with ProcessLifecycleOwner right before startService instead of getRunningAppProcesses().

  Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

### Task C4：開啟詩歌時的歷史紀錄寫入移出主執行緒

**Files:**
- Modify: `hymnchtv/src/main/java/org/cog/hymnchtv/MainActivity.java`（`showContent`）

**Prerequisites:** Task C3。

`showContent` 每開一首詩歌都在主執行緒上：建立 `HistoryRecord`（建構子會從 assets 讀歌詞檔取標題）並寫入資料庫。這是 StrictMode 基準裡的主要呼叫點之一。歷史列表（`initHistoryList`）是在使用者之後點開時才讀，寫入晚幾毫秒不影響。

- [ ] **Step 1：修改**

  ```java
          // Save the user selection into history record
          boolean isFu = MediaRecord.isFu(hymnType, hymnNo);
          HistoryRecord historyRecord = new HistoryRecord(hymnType, hymnNo, isFu);
          if (HYMN_BB_DUMMY != hymnNo) {
              DatabaseBackend.getInstance(ctx).storeHymnHistory(historyRecord);
          }
  ```

  改成

  ```java
          // Save the user selection into history record; the title lookup reads assets, so both run off the main thread
          if (HYMN_BB_DUMMY != hymnNo) {
              boolean isFu = MediaRecord.isFu(hymnType, hymnNo);
              Context appContext = ctx.getApplicationContext();
              AppExecutors.io("store-history", () -> DatabaseBackend.getInstance(appContext)
                      .storeHymnHistory(new HistoryRecord(hymnType, hymnNo, isFu)));
          }
  ```

  （`hymnType`、`hymnNo` 是方法參數，沒有被重新指派，lambda 可以直接使用。改完後先確認 `showContent` 後面沒有其他地方用到 `isFu` 或 `historyRecord`：`sed -n '/public static void showContent/,/^    }/p' hymnchtv/src/main/java/org/cog/hymnchtv/MainActivity.java | grep -n "isFu\|historyRecord"` 只會出現上面新加的那幾行。）

- [ ] **Step 2：驗證**

  ```bash
  ./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug --console=plain | tail -1
  export ANDROID_SERIAL=$(tools/perf/serial_for.sh api34)
  tools/perf/emu_lock.sh tools/perf/install_apk.sh debug
  tools/perf/emu_lock.sh tools/perf/strictmode_report.sh lane-c
  ```

  Expected: 報告中不再出現 `DatabaseBackend.storeHymnHistory`，也不再出現 `UpdateServiceImpl.removeOldDownloads`／`getStore`（如果它們在 before 報告中出現過）。

  手動：主畫面輸入 1 → 大本 → 返回 → 點輸入框旁顯示歷史紀錄的入口，確認「大本 1」出現在最上面。

- [ ] **Step 3：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/MainActivity.java
  git commit -m "perf: write hymn history off the main thread

  Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

  Lane C 完成，回報四個 commit 的 SHA。

---

### Task F：和 A2 整合、合併 lane、完整驗證、前後量測、審查

**Files:**
- Modify: `docs/perf/2026-10-02-b-data-startup-measurements.md`
- 把三條 lane 合併到 `perf/b-data-startup`；必要時 rebase 到已經包含 A2 的基底

**Prerequisites:** Lane A、Lane B、Lane C 全部完成。

**整合順序（固定）：A2 先合併，B 之後 rebase。**
- 三條 B lane 彼此之間不衝突；**B 和 A2 之間預期會衝突**（`MainActivity.java` 一定會、`HymnsApp.java` 可能會），不能假設沒有衝突。
- A2 合併進它的目標分支（`feat/zh-hant`，或 A 已併入時的 `master`）之前，B 只在自己的分支上完成驗證，**不合併**。
- A2 合併後，B 依 Step 1 rebase，解完衝突再跑 Step 2 的衝突驗收，然後才繼續。

- [ ] **Step 1：確認 A2 狀態，rebase 到包含 A2 的基底**

  ```bash
  cd /Users/hitobias/orca/hymnchtv-b
  git fetch --all --prune
  BASE=feat/zh-hant   # A 已併入 master 時改成 master
  git log --oneline "$BASE" | head -20     # 確認 A2 的 commit / merge 已在 BASE 上
  ```

  - **A2 還沒合併**：跳過這一步的 rebase，直接做 Step 3～9，並在 PR 描述寫明「等 A2 合併後依 Task F Step 1～2 rebase 並重驗」。A2 合併後回到這一步。
  - **A2 已合併**：

    ```bash
    git switch perf/b-data-startup
    git rebase "$BASE"                              # Task 0/1 的 commit；可能在 HymnsApp.java 衝突（StrictMode 那兩行）
    for lane in db import startup; do
      git rebase --onto perf/b-data-startup b-task1-base "perf/b-lane-$lane"
    done
    git switch perf/b-data-startup
    ```

    （`b-task1-base` 是 Task 1 Step 12 建立 lane 前打的本地 tag，標記 lane 的舊基底。）

    衝突解法規則：
    - import 區：取兩邊的聯集，刪掉兩邊都已不用的 import。
    - A2 修改的區塊（桌布、`initUserSettings`、閱讀設定相關）：一律保留 A2 的版本。
    - B 修改的位置（只有 Lane C 列出的那幾處）：保留 B 的版本；如果 A2 也改了同一行（例如 A2 在 `onCreate` 的 `initUserSettings();` 附近加了程式碼），兩邊的修改都要保留，B 那一行（`AppExecutors.io("yb-xref-prewarm", …)`）放在 `initUserSettings();` 之後。
    - 遇到不屬於以上情況的衝突，**停下來回報**，不要自行判斷。

- [ ] **Step 2：衝突驗收（每次 rebase 之後都要做；沒有 rebase 時也做一次，作為基準）**

  ```bash
  F=hymnchtv/src/main/java/org/cog/hymnchtv/MainActivity.java
  echo "createYbXTable: $(grep -c 'createYbXTable' $F)"                                              # expect 0
  echo "super.onStart in handleIntent: $(sed -n '/private void handleIntent/,/^    }/p' $F | grep -c 'super.onStart')"  # expect 0
  grep -n 'ybXTable = YbCrossRef.TABLE' $F                                                             # expect 1 line
  grep -n 'mTocYB = YbCrossRef.ASSET' $F                                                               # expect 1 line
  grep -n 'AppExecutors.io("yb-xref-prewarm"' $F                                                       # expect 1 line
  grep -n 'AppExecutors.io("store-history"' $F                                                         # expect 1 line
  git diff "$BASE" HEAD --stat -- $F hymnchtv/src/main/java/org/cog/hymnchtv/HymnsApp.java
  git diff "$BASE" HEAD -- $F | grep '^@@'
  ```

  Expected:
  - 前兩行都是 `0`，後四個 `grep` 各印出一行。
  - `git diff "$BASE" HEAD -- MainActivity.java` 的 hunk **只出現在** Lane C 列出的位置：import 區、`mTocYB`、`ybXTable`、`onCreate` 的預熱那一行、`handleIntent` 第一行、`showContent` 開頭、刪除 `createYbXTable()`。出現其他 hunk（尤其是桌布或 `initUserSettings` 區塊）代表 rebase 時動到了 A2 的程式碼，要修正後重做這一步。
  - `HymnsApp.java` 的 hunk 只有：StrictMode 那兩行、清除舊 APK 那一行、`startUpdateService` 兩個方法、import 區。
  - 然後執行 A2 計畫中「和 `MainActivity` 有關」的手動驗證項目（桌布、閱讀設定），結果記在回報裡。

- [ ] **Step 3：合併三條 lane**

  ```bash
  git switch perf/b-data-startup
  git merge --no-ff perf/b-lane-db -m "merge: B lane A (DatabaseBackend: WAL, history count, getMediaRecords)"
  git merge --no-ff perf/b-lane-import -m "merge: B lane B (bulk imports in one transaction off the main thread)"
  git merge --no-ff perf/b-lane-startup -m "merge: B lane C (startup work off the main thread)"
  git log --oneline -15
  ```

  Expected: 三次 merge 都沒有衝突（lane 之間的檔案不重疊）。有衝突就停下來回報。合併後再跑一次 Step 2 的檢查。

- [ ] **Step 4：完整 build 與 JVM 測試**

  ```bash
  ./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug :hymnchtv:assembleBenchmark :hymnchtv:lintDebug --console=plain | tail -1
  for k in MissingTranslation ExtraTranslation HardcodedText; do
    printf "%s: " $k; grep -c "\[$k\]" hymnchtv/build/reports/lint-results-debug.txt || true
  done
  ```

  Expected: `BUILD SUCCESSFUL`；lint 三個數字和基底分支（`$BASE`）相同，這一波沒有新增任何字串或版面。

- [ ] **Step 5：全部 instrumented test（api34，再 api24）**

  ```bash
  for avd in api34 api24; do
    export ANDROID_SERIAL=$(tools/perf/serial_for.sh $avd)
    tools/perf/emu_lock.sh ./gradlew :hymnchtv:connectedDebugAndroidTest --console=plain | tail -1
    mkdir -p build/perf && cp -r hymnchtv/build/reports/androidTests/connected/debug "build/perf/androidTests-$avd"
  done
  ```

  Expected: 兩台都是 `BUILD SUCCESSFUL`，0 個失敗（含子項目 A 的 `ResourceLocaleResolutionTest`，以及 A2 的測試）。`ImportPerfTest` 也會在這裡跑一次，只需要通過，數字以 Step 6 為準。

- [ ] **Step 6：after 量測（api34 為主，api24 記錄）**

  ```bash
  for avd in api34 api24; do
    export ANDROID_SERIAL=$(tools/perf/serial_for.sh $avd)
    tools/perf/emu_lock.sh env SKIP_BUILD=1 tools/perf/install_apk.sh benchmark
    sleep 60
    tools/perf/emu_lock.sh tools/perf/cold_start.sh after
    tools/perf/emu_lock.sh tools/perf/import_perf.sh after
    tools/perf/emu_lock.sh env SKIP_BUILD=1 tools/perf/install_apk.sh debug
    tools/perf/emu_lock.sh tools/perf/strictmode_report.sh after
    tools/perf/emu_lock.sh tools/perf/locale_webview_check.sh
    adb logcat -c
    tools/perf/emu_lock.sh ./gradlew -q :hymnchtv:connectedDebugAndroidTest \
      -Pandroid.testInstrumentationRunnerArguments.class=org.cog.hymnchtv.toc.YbCrossRefAssetTest
    echo "api$(adb shell getprop ro.build.version.sdk | tr -d '\r') $(adb logcat -d -v raw -s YbPerf:I | tr -d '\r' | tail -1)"
  done
  ```

  Expected:
  - YB 對照表主執行緒首次載入：每台印出一行 `apiNN main_thread_first_ms=… median_ms=…`，把 api24 的值寫進量測文件（見 Task C2 的說明）。
  - 啟動（process-cold, cache-warm）`after`：api34 的中位數沒有比 `baseline` 慢超過雜訊帶。這一波不預期明顯變快；目的是不退步，並把 IO／IPC 移出主執行緒。
  - 匯入 `after`：兩列，`legacy`（現在跑在 WAL 上）和 `batch`。`batch` 達到 Task 1 Step 10 的門檻；`batch.imported == legacy.imported`。
  - StrictMode `after`：「量測協定」的 StrictMode 驗收列出的呼叫點都不在報告中。
  - 語系檢查：兩台都是 `RESULT: PASS`（`HymnsApp` 的 WebView 預建沒有動，這是回歸檢查）。

- [ ] **Step 7：手動驗證（api24，最慢的那台）**

  1. 媒體設定 → 長按「DB Impt」→ 確認：開始 toast、按鈕變灰、匯入期間可以捲動清單、結果 toast、按鈕恢復。
  2. **匯入的所有權**（Task B1 的設計）：匯入進行中按返回離開媒體設定 → 不當機，匯入繼續完成，結果 toast 由 application context 顯示；匯入進行中重新進入媒體設定 → 按鈕是灰的，匯入結束後自動恢復；匯入中再按一次「DB Impt」→ 顯示「進行中」，不會開始第二個匯入。
  3. 用 `adb shell am start -a android.intent.action.SEND -t text/plain --es android.intent.extra.TEXT "https://youtu.be/vw_rDfb0nuc" -n org.cog.hymnchtv/.MainActivity` 分享連結：開啟媒體設定。
  4. 開 3 首不同的詩歌（含青年詩歌 1），回主畫面打開歷史紀錄：3 首都在，最新的在最上面。
  5. 開發者選項「不保留活動」開啟，進入一首青年詩歌，按 Home，再回來：歌詞正確（YB 對照表在 process 還原後仍可用）。
  6. （選擇性）用 Android Studio 的 Memory Profiler 或 `adb shell dumpsys meminfo org.cog.hymnchtv` 的 `Activities:` 計數：匯入中離開媒體設定、等匯入結束、觸發 GC 後，`Activities` 數量回到只剩 `MainActivity`（確認背景工作沒有留住 `MediaConfig`）。

- [ ] **Step 8：寫入量測文件**

  把 Step 6 的所有輸出貼進對應的表格。「決策紀錄」補上：
  - 每一項的前後比較（中位數差、倍數），以及是否達到預期改善。
  - StrictMode after 報告中**仍然存在**的呼叫點，逐一標註「第二波（B-4／B-6／…）處理」。
  - B-3（延後預建 WebView）不在這一波，原因與第二波要用的指標見計畫附錄 B。
  - 待辦（不在這一波）：`MediaConfig.importUrlAssetFile` 的空檔 bug（死碼）；附錄 A 的 B-9b 呼叫端；B-3、B-10、B-11 等第二波項目。

- [ ] **Step 9：審查**

  - 用 code-reviewer 子代理審查 `git diff "$BASE"...perf/b-data-startup`，重點：
    - transaction 邊界、巢狀 transaction 的 rollback 語意、`storeMediaRecordOrThrow` 是否在所有批次寫入中使用、有沒有 `catch` 吞掉 SQL 例外；
    - 背景 closure 是否捕捉了 Activity／View（`AppExecutors.io(` 的每個呼叫點逐一看）；
    - `volatile` 欄位與 `LazyMap` 的 thread safety；
    - WAL 對既有資料庫的影響；
    - `MainActivity`／`HymnsApp` 的修改範圍是否只限於列出的位置（Step 2）。
  - 執行 `/codex review`。
  - 審查發現 P1 時，先修好（每個修正一個 commit），重跑對應的測試與 Step 6 中受影響的量測，才能進行下一步。

- [ ] **Step 10：Commit 文件、清理 worktree**

  ```bash
  git add docs/perf/2026-10-02-b-data-startup-measurements.md
  git commit -m "docs: record B data/startup measurements before and after

  Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  for lane in db import startup; do git worktree remove "../hymnchtv-b-$lane"; done
  git worktree list | grep hymnchtv-b
  ```

  Expected: 只剩 `hymnchtv-b`。lane 分支與 `b-task1-base` tag 保留到 PR 合併後再刪。

  PR 描述要寫明：整合順序（A2 先合併、B rebase）、Step 2 的衝突驗收結果、`MainActivity.java` 中屬於 B 的修改位置清單。

---

## 附錄 A：B-9b 呼叫端（第二波套用，這一波不要做）

`ContentHandler.getHymnMediaState` 目前在迴圈中對每種媒體類型各查一次 DB：

```java
        String dir;
        MediaType[] mediaTypes = MediaType.values();
        for (int i = 0; i < mediaTypes.length; i++) {
            MediaType mediaType = mediaTypes[i];
            MediaRecord mediaRecord = new MediaRecord(mHymnType, mHymnNo, isFu, mediaType);

            // Skip to next if state is already evaluated to true i.e. defined in DB media link
            if ((isAvailable[i] |= mDB.getMediaRecord(mediaRecord, false)))
                continue;
```

第二波（和 B-4 一起搬到背景時）改成：

```java
        String dir;
        Map<MediaType, MediaRecord> dbRecords = mDB.getMediaRecords(mHymnType, mHymnNo, isFu);
        MediaType[] mediaTypes = MediaType.values();
        for (int i = 0; i < mediaTypes.length; i++) {
            MediaType mediaType = mediaTypes[i];

            // Skip to next if state is already evaluated to true i.e. defined in DB media link
            if ((isAvailable[i] |= dbRecords.containsKey(mediaType)))
                continue;
```

（迴圈後面的 `switch` 不再需要 `mediaRecord` 變數；套用時確認它沒有其他用途。）

## 附錄 B：B-3（延後預建 WebView）留給第二波的驗收設計

這一波刻意不做 B-3（Codex P1）：`am start -W` 的 TotalTime 只量到第一個畫面，把 `new WebView(this).destroy()` 延後到首幀之後，TotalTime 一定變好看，但成本只是被搬到使用者第一次操作的時候。第二波若要做，必須同時用下列方法驗收，任何一項變差就不採用：

1. **time-to-first-interaction**：腳本在冷啟動的第一個畫面出現後立刻點「1」→「大本」，量從 `am start` 到 `ContentHandler` 的 `Displayed` log 的時間（15 次取中位數，和現況比較）。
2. **首幀之後的卡頓**：同一流程前先 `adb shell dumpsys gfxinfo org.cog.hymnchtv reset`，流程結束後收集 `dumpsys gfxinfo org.cog.hymnchtv framestats`，比較 janky frames 比例與第 90／99 百分位的幀時間。
3. **競爭情境**：延後的預建尚未執行時就打開歌詞頁（歌詞頁的 WebView 成為 process 的第一個 WebView），驗證 (a) 語系仍正確（`locale_webview_check.sh` 的歌詞頁與返回主畫面檢查），(b) 這個情境下的 time-to-first-interaction 沒有比現況差。
4. **語系回歸**：`tools/perf/locale_webview_check.sh` 在 api24 和 api34 都通過。
