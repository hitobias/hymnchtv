# 子項目 D-1a：詩歌筆記本資料層＋備份 實作計畫

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**修訂紀錄：**
- rev 1：初版。
- rev 2：納入協調者對待確認問題的決定。
- rev 3：依 Codex 審查修正。
  - 自動記錄的去重改成單一 transaction。
  - 合併加上 `updatedBy`（裝置 id），排序規則改成確定性，並拒絕超過 24 小時的未來時間。
  - `HymnKey` 改用 app 實際的編號範圍，並要求 canonical 形式。
  - id 必須是 UUID。
  - 備份 JSON 加上更多限制。
  - `playlist_item` 的位置加上 unique index。
  - 修正 TRUNCATE 與 `connectedDebugAndroidTest` 的不實說法。
  - 定義 coroutine scope 的擁有者。
  - 補上 DST 測試。
  - 破壞性的 E2E 步驟要明確 opt-in，並檢查裝置。
  - spike 改為硬性關卡。
- rev 4：依 Codex 複審修正。
  - 匯入的歌單項目改成兩階段寫入：先移到暫存的負數位置，再寫最終位置，避免交換或循環時撞到 unique index。
  - 合併的最後一個比較鍵，從 `toString()` 改成明確的 canonical 欄位序列，並用測試固定。
  - `sungAt` 必須在 0～現在＋24 小時之間，`DedupeWindow` 改用飽和運算避免溢位。
- rev 5：
  - 假 repository 的驗證順序改成和 Room 實作完全相同。
  - 新增 `SingLogRepositoryContract`（`src/sharedTest/`），同一組測試分別對假實作（JVM）和 Room 實作（裝置）執行。
  - 明確列出和 A2、B 共用的檔案，以及整合順序。
- rev 6（2026-10-02，依 Room 統一計畫 rev 5～9，`2026-10-02-room-unification.md`）：資料庫改為與舊 `DatabaseBackend` 統一的單一 `HymnchtvDatabase`（檔名 `hymnchtv.db`，WAL；schema 仍為 v1，8 個 entity）。
  - Task 0–7（已實作）由 Room 統一計畫 Task 6(a) 遷移（`NotebookDatabase` 刪除、5 個 entity／DAO 併入 `HymnchtvDatabase`、`Room*Repository` 與 androidTest 改用 `HymnchtvDatabase`、TRUNCATE 移除）；其歷史片段不重寫，只在各 Task 標題下加註記。
  - Task 8–13 完整更正：`RoomBackupStore`／測試／物件圖改用 `HymnchtvDatabase`（`Notebook.get` 取 `HymnchtvDatabase.getInstance(app)`，不另建資料庫）；備份規則與 `BackupRulesTest` 改為 `hymnchtv.db` ＋ `-wal`／`-shm`；備份範圍擴大為整個統一 DB（媒體連結、歷史、英文歌詞、筆記本）；E2E 的應用程式 ID 改為 `com.ziontkec.hymnal`。
  - Task 11 加 Auto Backup 25 MB 配額風險與實測資料。
  - dev-notes：中間三表組建（Room 統一計畫合併後、本遷移完成前）留下的 `hymnchtv.db`（含 `-wal`／`-shm`）schema 與現在不同，開發／測試裝置在安裝遷移後的組建前必須 `adb uninstall com.ziontkec.hymnal`（或 `adb shell pm clear com.ziontkec.hymnal`）；不要把 `hymnchtv.db` 加進啟動刪檔清單。`org.cog.hymnchtv` 現在只是 namespace，應用程式 ID 是 `com.ziontkec.hymnal`。
- rev 8（2026-10-02，依 Codex 對 Task 10、12 的複審）：
  - Task 10 `BackupServiceTest.exportBytes()` 改成一般 suspend 函式，不在非 suspend 的 `also {}` 中呼叫 suspend `exportTo()`（原寫法無法編譯）。
  - Task 12 `NotebookAsync` 建構子新增 `workDispatcher`（production 用 `Dispatchers.IO`）：repository 工作（含 `RoomSingLogRepository.create()` 同步呼叫的 `deviceId()`）不再跑在主執行緒，結果仍在 callback dispatcher 回呼；`NotebookAsyncTest` 與 `Notebook.get()` 同步更新。
  - Task 13 busy Auto Backup E2E 的 `integrity()` 也驗證舊三表（`hymnHistoryDao().listNewestFirst()` 含 `LEGACY_HISTORY`）。
  - Task 13 SAF 驗收預期改為 `export OK 1.0.0`（`BuildConfig.VERSION_NAME`）。Codex 第 3 輪提出的 P1（`flow.collect` import）判定為誤報：`FlowCollector` 自 coroutines 1.6 起是 `fun interface`，`tracker.recorded.collect { }` 可直接編譯，以 Task 12 實際編譯驗證。
  - Task 8 `BackupCodecTest.maliciousIdsAreSkipped`：`testUuid(1)` 全是數字，`.uppercase()` 後仍是合法 canonical UUID，實作正確接受它，測試必然失敗；改用含十六進位字母的 `testUuid(0xab)`（實作不變）。
  - AVD 實際名稱是 `api34b`／`api24b`（原寫 `api34nb`／`api24nb`），全文更正。
- rev 7（2026-10-02，依 Codex／code-reviewer 對 D-1a Task 8–13 的審查）：
  - 分支策略：實際只有 `feat/notebook-data`（worktree `/Users/hitobias/orca/hymnchtv-d1a`），Task 0–7 已線性完成在其上；Task 8–13 全部在同一條分支上線性進行，刪除 Task 12 的三個 lane merge 與「三 lane 已 commit」前置，Task 8–11 的 lane 前置與「Lane C 只編譯」改寫；Task 0–7 的 lane 歷史片段只加註記不改寫。
  - E2E：不再斷言還原後 `-wal`／`-shm` 存在（暫態檔），改為驗證主 DB 已還原且至少一筆舊表（`hymn_history`）與一筆 notebook 資料正確；sidecar 只記錄。
  - `BackupRulesTest`：`forbidden` 拆成 `forbiddenPaths`（只比對 `path="…`）與 `forbiddenAttribute`，避免 XML 註解提到 `notebook_device.xml` 造成誤判；已逐條手動推演 Task 11 範例 XML 會通過。
  - 文字更正：`org.json:json` 實際沿用 master 的 20250517；instrumented test 覆蓋的是 `com.ziontkec.hymnal`。

## 給執行者（Sonnet 5.5）的說明

這份計畫由 **Sonnet 5.5（`claude-sonnet-5-5`）** 子代理執行。主對話（協調者）負責開 worktree、分派 lane、合併，以及跑模擬器。建議的啟動方式：

1. 在 repo 根目錄 `/Users/hitobias/orca/hymnchtv` 開一個新的 Claude Code session（協調者）。
2. 確認工作目錄乾淨：`git status --short` 沒有輸出。分支的建立方式見 Task 0 Step 1。
3. 輸入以下指示：

   > 使用 superpowers:subagent-driven-development 執行 `docs/superpowers/plans/2026-10-02-d1a-notebook-data-implementation.md`。Task 0 是硬性關卡：沒有全部通過，就不要開始 Task 1。Lane 0 的 Task 0～3 依序做完並 commit 後，用 worktree 平行派出 Lane A、B、C 三個 Sonnet 5.5 子代理；三條 lane 都完成後依 A → B → C 的順序合併，再做 Task 12、13。Task 13 的破壞性步驟要先取得我的同意。計畫和程式碼對不上就停下來問我，不要自行猜測。

**執行規則：**
- **Lane 與順序**（rev 7 註記：實際上沒有開 lane 分支。Task 0–7 已線性完成在 `feat/notebook-data`，Task 8–13 也在這條分支與 worktree `/Users/hitobias/orca/hymnchtv-d1a` 上線性進行，不再有 lane 合併。下表與下方 Lane／worktree／模擬器分工的說明只作為 Task 0–7 的歷史紀錄；Task 8–13 的指示以各 task 本文為準）：

  | Lane | Tasks | 前置條件 | 會碰到的檔案（彼此不重疊） |
  |---|---|---|---|
  | 0（序列） | 0 → 1 → 2 → 3 | 無 | 根目錄 `build.gradle`、`hymnchtv/build.gradle`、`notebook/model/`、`notebook/data/`、`notebook/repo/*.kt`（介面）、`notebook/settings/NotebookPrefs.kt`、`hymnchtv/schemas/`、`src/test/.../notebook/fakes/`、`src/sharedTest/.../notebook/contract/`、`src/androidTest/.../notebook/model/`、`src/androidTest/.../notebook/data/` |
  | A | 4 | Task 3 已 commit | `notebook/repo/room/`、`src/androidTest/.../notebook/repo/` |
  | B | 5 → 6 → 7 | Task 3 已 commit | `notebook/record/`、`notebook/settings/SharedPrefsNotebookPrefs.kt`、對應測試 |
  | C | 8 → 9 → 10 → 11 | Task 3 已 commit | `notebook/backup/`、`res/xml/notebook_*.xml`、`AndroidManifest.xml`、對應測試 |
  | 0（序列） | 12 → 13 | A、B、C 都已合併 | `notebook/Notebook.kt`、`notebook/NotebookAsync.kt`、E2E 測試 |

- **禁止碰的檔案**：A2、B 兩個子項目正在平行修改 `ContentView.java`、`ContentHandler.java`、`MainActivity.java`、`DatabaseBackend.java`。
  - D-1a **任何 task 都不能修改這四個檔案**，也不修改 `HymnsApp.java` 和 `utils/HymnNoValidate.java`（只讀取，用來比對編號範圍）。
  - 歌詞頁怎麼接上 D-1a 的 API，寫在本文最後的「給 UI 階段的介面」，留到 UI 階段（A2 之後）才做。
- **和 A2、B 共用的檔案與整合順序**：見下方「共用檔案與整合順序」一節。**Task 0 開始前必須先確認那一節的前置條件。**
- **Worktree**：Lane A、B、C 各用一個 worktree。可以用 Agent tool 的 `isolation: "worktree"`，或手動執行 `git worktree add ../hymnchtv-d1a-lane-a -b feat/d1a-notebook-data-lane-a feat/d1a-notebook-data`。手動建立時，要把 `local.properties` 複製進 worktree（它在 `.gitignore` 裡）。
- **模擬器（重要）**：另一個代理正在這台電腦上用模擬器做驗證。
  - D-1a 的 instrumented test 會安裝並覆蓋 `com.ziontkec.hymnal`，Task 13 還會刻意解除安裝。
  - 依社群文件，AGP 的 connected test 跑完後會解除安裝 APK。AGP 9.3.3 的實際行為在 Task 0 Step 7 記錄，不要預設任一種結果。
  - 所以 D-1a 一律使用**專用的 AVD**：`api34b`（port 5580）和 `api24b`（port 5582）。所有 adb／gradle 指令前都要先 `export ANDROID_SERIAL=emulator-5580`（或 5582）。
- **Instrumented test 的分工**：
  - 只有 Lane 0 和 Lane A 可以在 lane 內跑 `connectedDebugAndroidTest`，而且要輪流使用 `api34b`：Lane A 開始後，Lane 0 就不再使用它，直到合併。
  - Lane B、C 在 lane 內只跑 JVM 測試，並用 `assembleDebugAndroidTest` 確認 androidTest 能編譯。它們的 instrumented test 由協調者在 Task 12 Step 1 合併後統一跑。
- **每個 task 完成都要跑**：`./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug :hymnchtv:assembleDebugAndroidTest --console=plain`，全部通過才 commit。
- **計畫和程式碼對不上就停下來**：特別是 Task 0 的 spike，任何一步的輸出和 Expected 不同，都要停下來回報，不要自己換版本（Task 0 Step 5 表格裡列出的退路除外）。
- **最後的審查**：Task 13 用 code-reviewer 和 `/codex review` 兩種方式審查。發現 P1 要先修好，才能開 PR。

**Goal:**
- 在統一的 Room 資料庫 `HymnchtvDatabase`（與舊 `DatabaseBackend` 的三張表同庫）加入「詩歌筆記本」的五張表：收藏、唱詩紀錄、筆記、歌單、歌單項目。五張表一開始就為同步預留欄位。
- 提供 UI 階段要呼叫的 API：
  - 收藏切換。
  - 唱過幾次與最近一次。
  - 自動記錄唱詩：歌詞頁停留 2 分鐘，或播放完畢。
  - 手動補記與修正。
- 備份：
  - 經由 Storage Access Framework 匯出／匯入一個有版本號的 JSON 檔。
  - 合併規則是確定性的 last-writer-wins。
  - 同時開啟 Android 自動備份。

**Architecture:**
- 新程式全部是 Kotlin，放在 `org.cog.hymnchtv.notebook` 底下。
- 判斷規則都是純 Kotlin，用 JVM 單元測試，採 TDD。包括：編號範圍、去重視窗、場合推測、備份的編碼與合併、輸入驗證。
- Room entity 直接當作資料模型（immutable `data class`），不另外做一層 domain model；所有修改都用 `copy()` 產生新物件。
- 資料存取走 Repository 模式：`Repository<T>`（`findAll`／`findById`／`create`／`update`／`delete`）加上各表專用的查詢。Room 實作放在 `repo/room/`。刪除一律是 soft delete（寫入 `deletedAt`）。
- 和 Android 打交道的部分集中在幾個薄類別：
  - `HymnchtvDatabase`（統一資料庫，見 Room 統一計畫；筆記本的 5 張表併入其中，不再有獨立的 `NotebookDatabase`）
  - `SharedPrefsNotebookPrefs`
  - `RoomBackupStore`
  - `BackupDocuments`
  - `Notebook`：建立物件圖，並擁有 app 層級的 scope。
  - `NotebookAsync`：給 Java 呼叫的 callback 包裝。
- 執行緒：Kotlin 端用 coroutines，Room 的 `suspend` DAO 會自己切到背景執行緒。Java 端透過 `NotebookAsync` 取得「主執行緒回呼＋可取消」的 API（repository 工作在 `Dispatchers.IO` 執行，只有回呼回到主執行緒）。

**Tech Stack:** Android（minSdk 24、compileSdk 37、AGP 9.3.3 內建 Kotlin＝KGP 2.2.10、Gradle 9.7.1、Java 11）、Room 2.8.5、KSP 2.3.12、kotlinx-coroutines 1.11.0（退路 1.10.2）、`org.json`（Android 內建；JVM 測試用 `org.json:json`，實際沿用 master 的 20250517，rev 7 更正）、JUnit 4.13.2、Truth 1.4.5、AndroidX Test（runner 1.7.0、ext-junit 1.3.0）。

**規格來源:** `docs/superpowers/plans/2026-10-02-hymnchtv-modernization-plan.md` 的「子項目 D → D-1 詩歌筆記本」與「子項目 S」。

**分支（協調者決定，2026-10-02）:**
- 子項目 A（`feat/zh-hant`）會先經由 PR 合併到 `master`。A2（`feat/reading-settings`）和 B（`perf/b-data-startup`）先合併進 `feat/zh-hant`。
- D-1a 使用自己的分支 `feat/d1a-notebook-data`，**在 A2 和 B 都合併進 `feat/zh-hant` 之後**才開：
  - `feat/zh-hant` 已經合併到 `master` → 從 `origin/master` 開。
  - 還沒合併 → 從 `feat/zh-hant` 的最新 commit 開，等 A 合併後再 rebase 到 `master`。
- PR 的目標是 `master`。
- Lane 分支為 `feat/d1a-notebook-data-lane-a`、`-lane-b`、`-lane-c`（歷史設計；rev 7 起未採用，實際分支只有 `feat/notebook-data`）。

### 共用檔案與整合順序

D-1a 只修改下列三個既有檔案，其他都是新增的檔案：

| 既有檔案 | D-1a 的修改（Task） | 同時在修改的子項目 | 衝突時的處理 |
|---|---|---|---|
| `build.gradle`（根目錄） | `buildscript.dependencies` 加一行 KSP classpath（Task 0） | 目前已知沒有 | — |
| `hymnchtv/build.gradle` | <ul><li>`apply plugin: 'com.google.devtools.ksp'` 一行</li><li>`android {}` 裡的 `sourceSets {}`（sharedTest）</li><li>`ksp {}` 區塊</li><li>`dependencies` 裡 6 行</li></ul>（以上都在 Task 0） | A2（依賴）、B（`buildTypes` 加 `benchmark`） | 兩邊都保留，不能整段二選一 |
| `hymnchtv/src/main/AndroidManifest.xml` | `<application>` 的 `allowBackup`、`dataExtractionRules`、`fullBackupContent` 三個屬性，並刪掉 `tools:ignore="DataExtractionRules"`（Task 11） | A2（新增 activity） | 兩邊都保留 |

**新增的檔案**（不會和任何人衝突）：
- `notebook/` 套件
- `res/xml/notebook_backup_rules.xml`、`res/xml/notebook_data_extraction_rules.xml`
- `hymnchtv/schemas/`
- `src/sharedTest/`
- 對應的 `src/test`、`src/androidTest` 測試

**D-1a 不修改的檔案**：
- `settings.gradle`、`gradle.properties`、`gradle/wrapper/`
- 所有 `strings.xml`／`values*/`（D-1a 沒有 UI 字串）
- `HymnsApp.java`、`MainActivity.java`、`ContentView.java`、`ContentHandler.java`
- `DatabaseBackend.java`、`MediaConfig.java`、`utils/HymnNoValidate.java`
- `proguard-rules.pro`

**唯讀依賴**（由測試把關）：
- `MainActivity.HYMN_*` 和 `PREF_SETTINGS`：由 `HymnTypesConsistencyTest`、`SettingsPrefsNameTest` 檢查。
- `HymnNoValidate` 的常數與陣列：由 `HymnTypesConsistencyTest`、`HymnNumberingConsistencyTest` 檢查。
- A2 或 B 如果改了這些值，D-1a 的測試會失敗，提醒同步更新 `HymnNumbering`。

**整合順序（決定）：**
1. **前置條件**：A2 和 B 都已合併進 `feat/zh-hant`。用 `git branch --merged feat/zh-hant | grep -E 'feat/reading-settings|perf/b-data-startup'` 確認兩個分支都列出來。
2. D-1a 才從那個 commit 開分支（Task 0 Step 1），所以 Task 0 和 Task 11 修改共用檔案時，A2 和 B 的修改已經在基底裡，不會有衝突。
3. D-1a 完成後開 PR 到 `master`。如果 `feat/zh-hant` 這時還沒合併到 `master`，依 Task 0 Step 1 的指令 rebase。

**退路**（只在協調者決定提早開工時使用）：
- 可以從目前的 `feat/zh-hant` 開工，但共用檔案的修改要集中在一個整合 commit。
- A2、B 合併後，執行 `git rebase feat/zh-hant`，逐一解決上表三個檔案的衝突（兩邊都保留）。
- 解完後要重跑：
  - Task 0 Step 5 的依賴檢查
  - `BackupRulesTest`
  - 完整的 Task 13 Step 2

**和 B 的互動：**
- B 在 debug build 會開 StrictMode。
- D-1a 所有的 DB 存取和 `deviceId()` 都在背景執行緒。
- 只有 `NotebookAsync.isAutoRecordEnabled()` 會在主執行緒讀 SharedPreferences（第一次讀取可能被 StrictMode 標記為 disk read）。UI 階段可以接受這一點，或改在背景預先讀取。

**Commit 規則:**
- 使用 conventional commits。
- 每個 commit 訊息的結尾加上（用第二個 `-m` 即可）：

  ```
  Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>
  ```

---

## 版本選擇與來源（2026-10-02 查證）

| 項目 | 版本 | 依據 |
|---|---|---|
| Kotlin（編譯器） | 2.2.10（不另外宣告） | <ul><li>AGP 9.3.3 的 POM 對 `org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.10` 有 runtime 依賴，本機 `./gradlew buildEnvironment` 也解析成 2.2.10。</li><li>AGP 9 release notes：「AGP 9.0 has a runtime dependency on KGP 2.2.10 … you no longer have to declare a KGP version」。</li></ul> |
| KSP | **2.3.12**（`com.google.devtools.ksp:symbol-processing-gradle-plugin`） | <ul><li>KSP 2.3.x 是 KSP2，版本已經和 Kotlin 脫鉤。</li><li>2.3.1 開始支援 AGP 9 內建 Kotlin；2.3.10 修了內建 Kotlin 下的 R class 解析；2.3.12 的最低 AGP 是 8.12.0（我們是 9.3.3）。</li><li>Maven Central 上的最新版是 2.3.12。</li><li>AGP 9 release notes：要用新版 KSP 時，在根目錄 `buildscript` 加 classpath。</li></ul> |
| Room | **2.8.5**（runtime、ktx、compiler） | <ul><li>Room release 頁：2.8.5 是最新穩定版（2026-09-09）。</li><li>Kotlin 專案要用 KSP。</li><li>2.7.0 起需要 Kotlin 2.0 以上；2.8.0 起 minSdk 23（我們是 24）。</li><li>`room-runtime-android-2.8.5.pom` 依賴 kotlin-stdlib 2.1.20。</li></ul> |
| kotlinx-coroutines | **1.11.0**（`-android`、`-test`）；退路 **1.10.2** | <ul><li>Maven Central 最新版。</li><li>`kotlinx-coroutines-core-jvm-1.11.0.pom` 依賴 kotlin-stdlib 2.2.20，和 2.2.10 編譯器同一個 minor 版本，理論上能讀 metadata。</li><li>這一點由 Task 0 的關卡實際編譯證明；不通過就改用 1.10.2（用 Kotlin 2.1 編譯）。</li></ul> |
| JSON | Android 內建 `org.json`；JVM 測試加 `testImplementation 'org.json:json:20250517'`（rev 7：原寫 20260814，實際沿用 master 已有的 20250517，行為相同） | <ul><li>不用 kotlinx-serialization：它需要 compiler plugin，而且新版用比 2.2 更新的 Kotlin 編譯。</li><li>`android.jar` 裡的 `org.json` 只是 stub，JVM 測試要另外加真正的套件。Task 0 會證明這樣可行。</li></ul> |
| Room 測試方式 | instrumented test＋in-memory DB（併發測試用檔案 DB） | <ul><li>不用 Robolectric：4.17 是另一個大依賴，而且不一定有 compileSdk 37 的 SDK jar。</li><li>真的 SQLite 行為（unique index、交易、併發）在裝置上測最可靠。</li></ul> |

來源：
- [AGP 9.0 release notes（built-in Kotlin、KGP 2.2.10、KSP classpath）](https://developer.android.com/build/releases/agp-9-0-0-release-notes)
- [Migrate to built-in Kotlin](https://developer.android.com/build/migrate-to-built-in-kotlin)
- [KSP releases（2.3.1 支援 AGP 9 內建 Kotlin、2.3.10、2.3.12）](https://github.com/google/ksp/releases)
- [KSP 2.3.10 release notes](https://newreleases.io/project/github/google/ksp/release/2.3.10)、[KSP 2.3.1 release notes](https://newreleases.io/project/github/google/ksp/release/2.3.1)
- [AGP 9 upgrade skill（KSP 需 2.3.6 以上；不要加 `android.disallowKotlinSourceSets=false`）](https://developer.android.com/agents/skills/build-system/agp/agp-9-upgrade/skill)
- [Room release notes（2.8.5、KSP、minSdk）](https://developer.android.com/jetpack/androidx/releases/room)
- [Back up user data with Auto Backup（備份時會先關閉 app）](https://developer.android.com/identity/data/autobackup)
- [Test backup and restore（bmgr 指令）](https://developer.android.com/identity/data/testingbackup)
- [Gradle forum：connectedAndroidTest 會在結束後解除安裝 APK](https://discuss.gradle.org/t/how-can-i-run-espresso-tests-without-uninstalling-apk-after/15492)（社群說法；實際行為在 Task 0 Step 7 驗證）
- Maven metadata（2026-10-02）：`com.google.devtools.ksp.gradle.plugin` 2.3.12、`androidx.room:room-*` 2.8.5、`kotlinx-coroutines-android` 1.11.0、`org.json:json` 20260814（rev 7：實際沿用 master 的 20250517）。

---

## 設計決策

### 詩歌的識別：canonical `HymnKey(hymnType, hymnNo, isFu)`

`HymnKey` 和舊資料表一樣，用「類別＋編號＋是否附錄」識別一首詩歌。`hymnNo` 是**內部編號**，也就是 `MainActivity.showContent()` 收到的值：大本詩歌的附錄 n 存成 `780 + n`，青年詩歌的補充接在 275 之後。

各本詩歌的合法範圍，照抄 `utils/HymnNoValidate.java`：

| 類別 | 合法的內部編號 | 不合法（`HymnNoValidate` 的規則） |
|---|---|---|
| `hymn_db` 大本 | 1～786（781～786 是附 1～6） | — |
| `hymn_bb` 補充本 | 1～1005 | 每一百號裡的空缺：`rangeBbLimit` 產生的 [38,100]、[151,200]…[931,1000]；`HYMN_BB_DUMMY` 2000 |
| `hymn_er` 兒童 | 1～1232 | `rangeErLimit` 產生的 [18,100]…[1119,1200] |
| `hymn_xb` 新歌頌詠 | 1～171 | 168、169、170 |
| `hymn_xg` 新詩歌本 | 1～206 | 34 |
| `hymn_yb` 青年 | 1～277 | — |

- **Canonical 規則**：`isFu` 必須等於 `HymnNumbering.isFu(type, no)`，也就是只有 `hymn_db` 且編號大於 780 才是 `true`，和 `MediaRecord.isFu()` 相同。
  - 所以 `hymn_db/781/false` 和 `hymn_db/1/true` 都**無法建構**：constructor 會丟 `IllegalArgumentException`。
  - 同一首附錄詩歌只會有一種表示法。
- **在哪裡驗證**：constructor、`of()` 和 JSON 匯入都經過同一套驗證；匯入時，非 canonical 的列會被跳過並計數。
- **確保和舊程式一致**：
  - JVM 測試（Java）比對各個 `*_NO_MAX` 常數。
  - instrumented test 直接讀 `HymnNoValidate.rangeBbLimit` 等陣列做逐號比對。

### 五張表（全部是同步就緒的欄位）

**共同欄位：**

| 欄位 | 說明 |
|---|---|
| `id` | 小寫 canonical UUID 字串，PK |
| `createdAt`、`updatedAt` | epoch 毫秒 |
| `deletedAt` | 可為 null；有值就是已刪除 |
| `updatedBy` | 最後修改這一列的裝置 id |

不設 foreign key，因為 soft delete 和日後的同步都可能讓資料先後到達。

| 表 | 其他欄位 | Index |
|---|---|---|
| `favorite` | `hymnType`、`hymnNo`、`isFu` | `(hymnType, hymnNo, isFu)` unique |
| `sing_log` | 詩歌 key、`sungAt`、`occasion`、`source`、`playlistId?` | `(hymnType, hymnNo, isFu, sungAt)`、`(sungAt)`、`(playlistId)` |
| `note` | 詩歌 key、`body`、`singLogId?` | `(hymnType, hymnNo, isFu, createdAt)`、`(singLogId)` |
| `playlist` | `name` | 無 |
| `playlist_item` | `playlistId`、`position`、詩歌 key | `(playlistId, position)` **unique** |

**ID 規則：**
- `sing_log`、`note`、`playlist`、`playlist_item` 的 id，以及 `playlistId`、`singLogId` 這兩個參照欄位，都必須是 `UUID.fromString(x).toString() == x` 的小寫 canonical UUID。
- **收藏的 id 是固定的**：`FavoriteIds.forKey(key)` 是 name-based UUID，同一首詩歌在任何裝置上都得到同一個 id。
  - 匯入時，收藏的 id 必須等於重新計算的值，否則整列跳過。
  - 取消收藏是 soft delete；再收藏時，把同一列的 `deletedAt` 清掉。

**`updatedBy`（裝置 id）：**
- 第一次需要時隨機產生一個 UUID，存在**獨立的** prefs 檔 `notebook_device.xml`。
- 這個檔案**刻意不納入自動備份**：還原到新手機時會產生新的 id，兩台手機不會共用同一個 id。
- 每一次寫入都由 repository 蓋上 `updatedBy`。

**列舉存成英文名稱字串：**
- `Occasion`：`LORDS_DAY`（主日）、`SMALL_GROUP`（小排）、`PRAYER_MEETING`（禱告聚會）、`MORNING_REVIVAL`（晨興）、`HOME`（家中）、`OTHER`（其他）。
- `SingSource`：`AUTO`、`MANUAL`。
- 讀到不認得的值時，`Occasion` 變成 `OTHER`，`SingSource` 變成 `MANUAL`，絕不丟例外。

**歌單位置 `position`：**
- 它是**只增不減的排序鍵**，不是連續的 0..n-1。
- 新增項目時放在「這個歌單所有列（含已刪除）的最大位置＋1」。
- 重新排序時，把每一項依新順序放到目前最大值之後。
- 這樣在 unique index 下，不會撞到已刪除列或交換中的列。新增與重排都在 transaction 裡完成。

**資料庫檔與 schema：**
- 檔名 `hymnchtv.db`（`HymnchtvDatabase.FILE_NAME`，統一資料庫），journal mode 為 WAL（明確設定）。因此備份規則要同時列 `hymnchtv.db`、`hymnchtv.db-wal`、`hymnchtv.db-shm`。（原先的 `notebook.db` 加 TRUNCATE 決定已由 Room 統一計畫 §2.5 撤銷。）
- **它不是備份一致性的保證**，一致性來自 Auto Backup 本身（見下面「Android 自動備份」）。
- `exportSchema = true`，schema 輸出到 `hymnchtv/schemas/` 並 commit，供日後寫 migration。這是全新項目，v1 不需要任何 migration。

### 自動記錄規則

- **觸發**：
  - 歌詞頁上同一首詩歌**連續**可見滿 2 分鐘（`AutoRecordConfig.visibleThresholdMillis = 120_000`，是程式常數，不開放使用者設定）。中途離開（翻到別首、`onPause`）就重新計時。
  - 或媒體播放完畢。
- **去重是原子操作**：所有自動記錄都呼叫 `SingLogRepository.recordUnlessDuplicate(...)`。
  - Room 實作在**同一個 write transaction** 裡做兩件事：先查有沒有同一首詩歌、未刪除、`|sungAt − 現在| < 3 小時` 的紀錄，有就回傳 null；沒有才新增。
  - SQLite 同時只允許一個 write transaction，Room 的 `withTransaction` 也會把同一個資料庫的交易串行化。所以兩個同時發生的觸發（停留滿 2 分鐘和播放完畢）只會產生一列。
  - Task 4 有一個 20 個 coroutine 同時記錄的 instrumented test。
- **時間驗證**：
  - 所有寫入路徑（手動、自動、`update`）的 `sungAt` 都必須在 `0 ≤ sungAt ≤ 現在＋24 小時` 之間（`NotebookValidation.sungAt`），否則丟 `IllegalArgumentException`；Java 端會收到 `Outcome.Err`。
  - 備份匯入用的是同一個 24 小時上限。
  - `DedupeWindow` 的邊界使用飽和運算：`sungAt ± window` 超出 `Long` 範圍時，停在 `Long.MIN_VALUE`／`Long.MAX_VALUE`，不會溢位。
- **各路徑和去重的關係**：

  | 路徑 | 去重 | 理由 |
  |---|---|---|
  | 自動（停留、播放完畢） | **是**，`recordUnlessDuplicate` | 避免重複計數 |
  | 手動補記（`recordManual`），以及 D-1c 的「整份歌單記成已唱」 | **否**，直接 `record` | 使用者明確表示唱過，例如同一場聚會唱兩次；UI 可以用 `statsFor` 提示「3 小時內已記錄過」 |
  | 備份匯入 | **否**，只依 id 合併 | 匯入是在還原資料，不是新的唱詩；同一場聚會在兩台裝置各自自動記錄，會留下兩列（已知限制，等子項目 S 再處理） |

- **開關**：`notebook.xml` 的 `auto_record_enabled`，預設 `true`。這是唯一開放給使用者的設定，畫面在 UI 階段做。
- **場合推測**（協調者確認，2026-10-02）：`OccasionInference.infer(now, zone, lastChosen)`，用 `java.util.Calendar`，因為 minSdk 24 沒有 `java.time`。
  1. 記錄當下裝置時區的**週日 06:00（含）～13:00（不含）** → `LORDS_DAY`。
  2. 其他時間 → 使用者上次**手動選擇**的場合。但如果上次選的是 `LORDS_DAY`，改成 `HOME`。
  3. 從來沒選過 → `HOME`。
  - 時區在每次記錄時才讀取（`zone()` provider），所以換時區、夏令時間都依當下的當地時間判斷。測試包含 America/Los_Angeles 的兩次 DST 切換日（2026-03-08、2026-11-01，剛好都是週日）。
  - 「上次選的場合」只在手動補記或手動修改紀錄時更新，自動記錄不會改它。
- **執行緒與 scope 的擁有者**：
  - `NotebookGraph` 擁有一個 app 層級的 `appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + 記錄錯誤的 handler)`，生命週期就是整個 process，不會主動 cancel。
  - process 結束時，正在計時的 2 分鐘會消失，這是可接受的：頂多少記一筆。正在寫入的那筆是單一 transaction，要嘛完整寫入，要嘛完全沒寫。
  - `SingTracker` 的 `onHymnVisible`／`onHymnHidden`／`onMediaCompleted` 從主執行緒呼叫，立即返回，不做 I/O。
  - `recorded`（`SharedFlow`）在 `appScope` 的執行緒上發出。Java 透過 `NotebookAsync.observeAutoRecorded` 在主執行緒收到；Kotlin UI 要用 `lifecycleScope.launch { repeatOnLifecycle(STARTED) { tracker.recorded.collect { … } } }` 收集。

### 備份檔格式（schemaVersion 1）

```json
{
  "format": "hymnchtv-notebook",
  "schemaVersion": 1,
  "exportedAt": 1790733600000,
  "appVersionName": "2.9.2",
  "favorites":     [{"id": "…", "hymnType": "hymn_db", "hymnNo": 1, "isFu": false, "createdAt": 1, "updatedAt": 1, "deletedAt": null, "updatedBy": "…"}],
  "singLogs":      [{"id": "…", "hymnType": "…", "hymnNo": 1, "isFu": false, "sungAt": 1, "occasion": "LORDS_DAY", "source": "AUTO", "playlistId": null, "createdAt": 1, "updatedAt": 1, "deletedAt": null, "updatedBy": "…"}],
  "notes":         [{"id": "…", "hymnType": "…", "hymnNo": 1, "isFu": false, "body": "…", "singLogId": null, "createdAt": 1, "updatedAt": 1, "deletedAt": null, "updatedBy": "…"}],
  "playlists":     [{"id": "…", "name": "…", "createdAt": 1, "updatedAt": 1, "deletedAt": null, "updatedBy": "…"}],
  "playlistItems": [{"id": "…", "playlistId": "…", "position": 0, "hymnType": "…", "hymnNo": 1, "isFu": false, "createdAt": 1, "updatedAt": 1, "deletedAt": null, "updatedBy": "…"}]
}
```

- **包含已刪除的列**（`deletedAt` 有值），這樣在舊手機刪掉的資料，匯入新手機後也是刪除狀態。
- `schemaVersion` 是**檔案格式**的版本，和 Room 的 DB version 分開。遇到比目前新的版本，回 `UNSUPPORTED_VERSION`。
- **整個檔案被拒絕**的情況（`BackupLimits` 的預設值）：

  | 情況 | 錯誤 |
  |---|---|
  | 檔案超過 16 MiB | `TOO_LARGE` |
  | 不是 JSON | `NOT_JSON` |
  | 巢狀深度超過 4（在 parse 前就檢查，避免深層巢狀造成 `StackOverflowError`） | `WRONG_FORMAT` |
  | `format` 不對、`schemaVersion` 不是 1 以上的整數 | `WRONG_FORMAT` |
  | `exportedAt` 不是 0 以上的數字 | `WRONG_FORMAT` |
  | `appVersionName` 不是字串，或超過 64 字 | `WRONG_FORMAT` |
  | 某個表不是陣列（缺少的表視為空） | `WRONG_FORMAT` |
  | 任一表超過 100,000 列 | `TOO_LARGE` |

- **只跳過該列並分類計數**（`SkippedRows`）：
  - `invalid`：缺欄位或型別錯、id 不是 canonical UUID、收藏 id 不等於重新計算的值、`HymnKey` 非 canonical、`updatedBy` 不是 UUID、時間為負、筆記空白或超過 100,000 字、歌單名稱空白或超過 200 字。
  - `futureTimestamp`：`createdAt`／`updatedAt`／`deletedAt`／`sungAt` 晚於「匯入當下＋24 小時」。

### 合併規則（純函式，JVM 測試）

對每一列，定義：
- `version = max(updatedAt, deletedAt ?: −∞)`
- 排序鍵 = `(version, 是否已刪除, updatedBy, canonicalContent)`，依序比較，越大越新。
  - `canonicalContent` 是內容欄位（不含 id 和同步欄位）明確的序列化：每個欄位寫成 `長度:值`，null 寫成 `~`，以 `|` 連接。
  - 這種格式是單射的：不同內容一定得到不同字串，也不依賴 data class 的 `toString()`。
  - 格式由 `BackupMergerTest` 固定；改變它等於改變合併結果，要當成檔案格式看待。

這是一個**全序**：任兩列內容不同時，一定分得出誰比較新，而且在任何一台裝置上算出來的結果都一樣（兩台裝置互相匯入會收斂到同一個結果）。「靜默保留本機」只發生在兩列完全相同時。

對檔案中的每一列（同一 id 在檔案中出現多次時，先用同一個排序挑出最新的一列）：
1. 本機沒有這個 id → 新增。
2. 和本機那列完全相同 → 不變。
3. 依排序鍵比較，檔案那列比較新 → 用檔案的；否則保留本機。

**歌單位置的衝突**：
- 兩台裝置可能在同一個歌單的同一個位置各加了一首（id 不同），合併後會違反 `(playlistId, position)` unique。
- `BackupMerger` 在合併後執行 `resolvePositionCollisions`：本機原有的列保留位置；被擠到的匯入列依 `(position, id)` 順序，移到該歌單最大位置之後。這個移位**不改** `updatedAt`。
- **寫入分兩階段**：已存在的列在匯入中互換或循環移動位置時（例如本機 A=0、B=1，匯入 A=1、B=0），直接逐列 upsert 會在交易進行中撞到 unique index。所以 `RoomBackupStore` 在同一個 transaction 裡：
  1. 先把所有要寫的歌單項目放到各自唯一的暫存負數位置（`-1 - index`）。正常的位置一定 ≥ 0，所以不會衝突。
  2. 再寫入最終位置。

  Task 10 有互換、三列循環、三列重排（中間那列不變）的 instrumented test。

**時鐘的限制（必須寫進 PR 描述）：**
- last-writer-wins 用的是裝置的牆上時鐘。時鐘快了 X 的裝置，它的修改會勝過其他裝置在之後 X 時間內做的修改。
- 匯入時會拒絕晚於匯入當下 24 小時以上的列，把損害限制在 24 小時內。本機寫入無法判斷真正的時間，所以不做限制。
- 子項目 S 會改用**伺服器指派、單調遞增的版本號**決定先後；`updatedAt` 只留作顯示用途，`updatedBy` 保留作稽核。

讀取本機資料、計算合併、寫入，三步在**同一個 Room transaction** 裡完成。匯入不會動到 prefs。

### Android 自動備份
- **啟用方式**：`android:allowBackup="true"`。
  - API 31 以上用 `android:dataExtractionRules="@xml/notebook_data_extraction_rules"`（`cloud-backup` 和 `device-transfer` 兩段）。
  - API 24～30 用 `android:fullBackupContent="@xml/notebook_backup_rules"`。
- **只 include 這些檔案**（協調者決定，2026-10-02；Room 統一計畫 §2.5 修訂）：
  - `database/hymnchtv.db`、`database/hymnchtv.db-wal`、`database/hymnchtv.db-shm`：統一資料庫。Auto Backup 的 `database` domain 只能以檔案為粒度，所以備份內容是**整個**統一 DB（媒體連結、歷史、英文歌詞、筆記本五張表），不再只有筆記本。這些都是使用者資料／設定，不含憑證或機密。
  - `sharedpref/notebook.xml`
  - `sharedpref/Settings.xml`：`MainActivity.PREF_SETTINGS`，裡面是語言、歌詞與閱讀設定。
- 只要寫了 include，其他檔案都不會備份，包括 `store.xml`、`files/`、`notebook_device.xml`（刻意排除，理由見上面的 `updatedBy`）。另外兩類本來就不在備份範圍：`cache/`，以及下載到公用 Downloads 目錄的媒體。
- **一致性**：官方文件明確寫著「During Auto Backup, the system shuts down the app to make sure it is no longer writing to the file system」。
  - 所以 Auto Backup 複製檔案時，app 已經停止寫入；WAL 的 `-wal`／`-shm` 一併備份，Room 關閉時 checkpoint。
  - Task 13 Step 5 會在 app **正在寫入時**觸發 `bmgr backupnow`，記錄 app 是否被關閉，並在還原後跑 `PRAGMA integrity_check`。
- **舊的 `dbHymnApp.db` 已不存在**（Room 統一計畫：它的三張表併入 `hymnchtv.db`，啟動時刪除舊檔）。以下為原先「不備份」的歷史理由，僅供參考：
  - 子項目 B 會把它改成 WAL 模式，備份規則就要另外列 `-wal`／`-shm` 檔。
  - 協調者原本的顧慮是「複製使用中的檔案可能不一致」。依上面的官方說法，Auto Backup 會先關閉 app，這個風險其實比較小。
  - 但它的內容價值低：媒體連結可以重新匯入，開啟歷史也不重要。所以維持不備份。
- **`Settings.xml` 的已知副作用**：裡面的 `PREF_WALLPAPER` 是舊裝置上的檔案 URI，還原到新手機後會指到不存在的檔案。
  - 讀背景圖的程式碼（`WallPaperUtil`，由 A2 改寫）必須能處理檔案不存在，退回預設背景。
  - 這要列入 A2／UI 階段的審查重點；D-1a 不修改那個檔案。
- **隱私上的取捨**（協調者決定，不使用 `disableIfNoEncryptionCapabilities`）：
  - 沒有設定螢幕鎖的手機也會備份到 Google 雲端。這種備份沒有端對端加密，但仍受 Google 帳號保護。
  - 選擇這樣做，是為了讓沒有設定螢幕鎖的長者換手機時，筆記本也能自動還原。
  - 筆記屬於個人屬靈紀錄。日後撰寫隱私權政策（子項目 S）時，要說明 Android 自動備份會把筆記本存到使用者的 Google 雲端，以及如何在系統設定中關閉。

---

## 檔案結構

路徑都在 `hymnchtv/src/main/java/org/cog/hymnchtv/` 或 `hymnchtv/src/main/res/` 底下。

**新增（純 Kotlin，有 JVM 單元測試）：**

| 檔案 | 職責 |
|---|---|
| `notebook/model/HymnTypes.kt` | 六種詩歌本的類別字串 |
| `notebook/model/HymnNumbering.kt` | 各本詩歌的合法編號範圍與附錄判斷（照抄 `HymnNoValidate`） |
| `notebook/model/HymnKey.kt` | canonical 的詩歌識別 |
| `notebook/model/Occasion.kt`、`SingSource.kt` | enum 與安全解析 |
| `notebook/model/SyncRecord.kt` | 同步欄位介面、`isActive`、`version` |
| `notebook/model/Clock.kt` | `Clock`、`IdGenerator`、`DeviceIdProvider` |
| `notebook/model/FavoriteIds.kt` | 收藏的固定 id |
| `notebook/model/DedupeWindow.kt` | 去重視窗的邊界 |
| `notebook/model/NotebookValidation.kt` | UUID、長度上限與輸入驗證 |
| `notebook/record/AutoRecordConfig.kt`、`OccasionInference.kt` | 自動記錄的參數、場合推測 |
| `notebook/record/SingTracker.kt` | 可見時間的計時與自動記錄 |
| `notebook/backup/BackupModels.kt` | `NotebookTables`、`BackupSnapshot`、`BackupLimits`、`SkippedRows`、錯誤與結果型別 |
| `notebook/backup/BackupCodec.kt` | JSON 編碼／解碼與驗證 |
| `notebook/backup/BackupMerger.kt` | 合併規則與歌單位置衝突處理 |
| `notebook/backup/BackupService.kt`、`BackupStore.kt`、`BackupFileName.kt` | 匯出／匯入流程 |
| `notebook/NotebookAsync.kt` | 給 Java 用的 callback 包裝 |

**新增（Android 邊界，用 instrumented test 驗證）：**

| 檔案 | 職責 |
|---|---|
| `notebook/data/entity/*.kt`（5 個）、`notebook/data/SingStats.kt` | Room entity |
| `notebook/data/dao/*.kt`（5 個 DAO＋`DaoSql.kt`） | 查詢 |
| `notebook/data/NotebookConverters.kt` | Room type converter（資料庫本體是 `persistance/room/HymnchtvDatabase.kt`，由 Room 統一計畫提供） |
| `notebook/repo/*.kt`、`notebook/repo/room/*.kt` | Repository 介面與 Room 實作 |
| `notebook/settings/NotebookPrefs.kt`、`SharedPrefsNotebookPrefs.kt` | 自動記錄開關、上次選的場合、裝置 id |
| `notebook/backup/RoomBackupStore.kt`、`BackupDocuments.kt` | 備份的 Room 與 SAF 邊界 |
| `notebook/Notebook.kt` | 物件圖與 `appScope` |
| `res/xml/notebook_backup_rules.xml`、`res/xml/notebook_data_extraction_rules.xml` | 自動備份規則 |

**修改：**

| 檔案 | 修改內容 |
|---|---|
| `build.gradle`（根目錄） | `buildscript` 加 KSP classpath |
| `hymnchtv/build.gradle` | 套用 KSP、Room schema 位置、依賴 |
| `hymnchtv/src/main/AndroidManifest.xml` | 開啟自動備份 |

**產生並 commit：** `hymnchtv/schemas/org.cog.hymnchtv.persistance.room.HymnchtvDatabase/1.json`（統一資料庫，v1，8 個 entity）

---

### Task 0：建置設定與 spike——**硬性關卡**（Lane 0）

> 已由 Room 統一計畫 Task 6(a) 遷移：本 Task 內的 `NotebookDatabase`／`notebook.db`／`TRUNCATE` 為歷史片段，現況為 `HymnchtvDatabase`（`hymnchtv.db`、WAL）。

**這個 task 沒有全部通過（包括 coroutines 版本確認），就不能開始 Task 1。**

**Files:**
- Modify: `build.gradle`（根目錄）
- Modify: `hymnchtv/build.gradle`
- Create（暫時，Step 6 刪除）：
  - `hymnchtv/src/main/java/org/cog/hymnchtv/notebook/spike/Spike.kt`
  - `hymnchtv/src/sharedTest/java/org/cog/hymnchtv/notebook/spike/SharedSpike.kt`
  - `hymnchtv/src/test/java/org/cog/hymnchtv/notebook/spike/SpikeCoroutinesTest.kt`
- Create（保留）: `hymnchtv/src/test/java/org/cog/hymnchtv/notebook/OrgJsonOnClasspathTest.kt`

- [ ] **Step 1：開分支**

  ```bash
  git fetch origin && git status --short
  # Prerequisite: A2 and B are merged into feat/zh-hant (both names must be listed)
  git branch -a --merged feat/zh-hant | grep -E 'feat/reading-settings|perf/b-data-startup'
  # If feat/zh-hant is already merged to master (git branch -r --merged origin/master | grep zh-hant):
  git switch -c feat/d1a-notebook-data origin/master
  # Otherwise start from the feat/zh-hant tip (rebase onto master after A's PR merges):
  # git switch -c feat/d1a-notebook-data feat/zh-hant
  ```

  Expected:
  - `git status --short` 沒有輸出。
  - `grep` 列出 `feat/reading-settings` 和 `perf/b-data-startup` 兩個分支。**少任何一個就停下來回報，不要開工**，除非協調者明確選擇「共用檔案與整合順序」裡的退路。
  - 目前分支是 `feat/d1a-notebook-data`。在回報中寫明它是從 `master` 還是 `feat/zh-hant` 開出來的。

  如果是從 `feat/zh-hant` 開的，A 合併之後執行 `git rebase --onto origin/master feat/zh-hant feat/d1a-notebook-data`。rebase 完要重跑 Task 13 Step 2，才能開 PR。

- [ ] **Step 2：根目錄 `build.gradle` 的 `buildscript.dependencies`，在 `classpath 'com.android.tools.build:gradle:9.3.3'` 下一行加入**

  ```groovy
          // KSP2 (decoupled from the Kotlin version); AGP 9 built-in Kotlin supplies KGP 2.2.10
          classpath 'com.google.devtools.ksp:symbol-processing-gradle-plugin:2.3.12'
  ```

- [ ] **Step 3：修改 `hymnchtv/build.gradle`**

  (a) 第一行 `apply plugin: 'com.android.application'` 下面加一行：

  ```groovy
  apply plugin: 'com.google.devtools.ksp'
  ```

  (a2) 在 `android { ... }` 區塊內、`testOptions {` 的上一行加入：

  ```groovy
      // Contract tests shared by the JVM fakes (test) and the Room implementations (androidTest)
      sourceSets {
          test.java.srcDir 'src/sharedTest/java'
          androidTest.java.srcDir 'src/sharedTest/java'
      }
  ```

  (b) 在 `android { ... }` 區塊結束的 `}` 之後、`dependencies {` 之前加入：

  ```groovy
  // Room schema JSON is committed so future versions can write and test migrations.
  ksp {
      arg('room.schemaLocation', "$projectDir/schemas")
      arg('room.generateKotlin', 'true')
  }
  ```

  (c) 在 `dependencies { ... }` 中，`implementation 'org.jsoup:jsoup:1.23.2'` 的下一行加入：

  ```groovy

      // D-1a notebook: Room + coroutines (coroutines fallback: 1.10.2, see Task 0 Step 5)
      implementation 'androidx.room:room-runtime:2.8.5'
      implementation 'androidx.room:room-ktx:2.8.5'
      ksp 'androidx.room:room-compiler:2.8.5'
      implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0'
  ```

  (d) 在 `testImplementation 'com.google.truth:truth:1.4.5'` 的下一行加入：

  ```groovy
      testImplementation 'org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0'
      // Real org.json for JVM tests (android.jar only has stubs)
      testImplementation 'org.json:json:20260814'
  ```

- [ ] **Step 4：寫 spike 檔案**

  `hymnchtv/src/main/java/org/cog/hymnchtv/notebook/spike/Spike.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.spike

  import androidx.room.Dao
  import androidx.room.Database
  import androidx.room.Entity
  import androidx.room.PrimaryKey
  import androidx.room.Query
  import androidx.room.RoomDatabase
  import androidx.room.Upsert
  import kotlinx.coroutines.Dispatchers
  import kotlinx.coroutines.flow.MutableSharedFlow
  import kotlinx.coroutines.flow.SharedFlow
  import kotlinx.coroutines.flow.asSharedFlow
  import kotlinx.coroutines.sync.Mutex
  import kotlinx.coroutines.sync.withLock
  import kotlinx.coroutines.withContext

  /** Temporary: proves KSP + Room + coroutines compile under AGP 9 built-in Kotlin 2.2.10. Deleted in Step 6. */
  @Entity(tableName = "spike")
  data class SpikeEntity(@PrimaryKey val id: String, val value: Long)

  @Dao
  interface SpikeDao {
      @Upsert
      suspend fun upsert(entity: SpikeEntity)

      @Query("SELECT * FROM spike WHERE id = :id")
      suspend fun find(id: String): SpikeEntity?
  }

  @Database(entities = [SpikeEntity::class], version = 1, exportSchema = true)
  abstract class SpikeDatabase : RoomDatabase() {
      abstract fun dao(): SpikeDao
  }

  /** Uses the coroutines APIs the notebook relies on (SharedFlow, Mutex, withContext). */
  class SpikeCoroutines {
      private val events = MutableSharedFlow<Int>(extraBufferCapacity = 1)
      private val mutex = Mutex()
      val emitted: SharedFlow<Int> = events.asSharedFlow()

      suspend fun ping(value: Int): Int = withContext(Dispatchers.Default) {
          mutex.withLock { events.tryEmit(value) }
          value
      }
  }
  ```

  `hymnchtv/src/sharedTest/java/org/cog/hymnchtv/notebook/spike/SharedSpike.kt`（證明 `src/sharedTest/java` 裡的 Kotlin 會同時被 test 和 androidTest 編譯）：

  ```kotlin
  package org.cog.hymnchtv.notebook.spike

  object SharedSpike {
      fun answer(): Int = 42
  }
  ```

  `hymnchtv/src/test/java/org/cog/hymnchtv/notebook/spike/SpikeCoroutinesTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.spike

  import com.google.common.truth.Truth.assertThat
  import kotlinx.coroutines.test.runTest
  import org.junit.Test

  class SpikeCoroutinesTest {
      @Test
      fun coroutinesAndCoroutinesTestWork() = runTest {
          assertThat(SpikeCoroutines().ping(7)).isEqualTo(7)
      }

      @Test
      fun sharedTestSourcesAreOnTheJvmTestClasspath() {
          assertThat(SharedSpike.answer()).isEqualTo(42)
      }
  }
  ```

  `hymnchtv/src/test/java/org/cog/hymnchtv/notebook/OrgJsonOnClasspathTest.kt`（保留，當作日後的防護）：

  ```kotlin
  package org.cog.hymnchtv.notebook

  import com.google.common.truth.Truth.assertThat
  import org.json.JSONObject
  import org.junit.Test

  /** BackupCodec relies on a real org.json in JVM tests; android.jar only ships stubs that throw. */
  class OrgJsonOnClasspathTest {
      @Test
      fun realOrgJsonIsUsedInJvmTests() {
          val encoded = JSONObject().put("a", JSONObject.NULL).put("b", 1L).toString()
          val parsed = JSONObject(encoded)
          assertThat(parsed.isNull("a")).isTrue()
          assertThat(parsed.getLong("b")).isEqualTo(1L)
      }
  }
  ```

- [ ] **Step 5：關卡檢查**

  ```bash
  ./gradlew :hymnchtv:assembleDebug :hymnchtv:assembleDebugAndroidTest :hymnchtv:testDebugUnitTest \
    --tests 'org.cog.hymnchtv.notebook.OrgJsonOnClasspathTest' --tests 'org.cog.hymnchtv.notebook.spike.*' --console=plain
  ls hymnchtv/schemas/org.cog.hymnchtv.notebook.spike.SpikeDatabase/1.json
  find hymnchtv/build/generated/ksp -name 'SpikeDatabase_Impl.kt'
  ./gradlew -q :hymnchtv:dependencies --configuration debugRuntimeClasspath | grep -E 'room-runtime:|kotlinx-coroutines-(android|core):|kotlin-stdlib:2' | sort -u | head
  ./gradlew -q :hymnchtv:dependencies --configuration debugUnitTestRuntimeClasspath | grep -E 'kotlinx-coroutines-test:' | sort -u | head -3
  find hymnchtv/build -ipath '*androidtest*' -name 'SharedSpike.class' | head -1
  ```

  Expected（**每一項都要符合，才算通過關卡**）：
  - BUILD SUCCESSFUL；`OrgJsonOnClasspathTest`、`SpikeCoroutinesTest` 都通過。
  - 編譯輸出中**沒有** `incompatible version of Kotlin`、`metadata` 相關的錯誤或警告。
  - schema JSON 存在；`SpikeDatabase_Impl.kt` 有產生，證明 KSP 在內建 Kotlin 下有執行。
  - 依賴列出 `room-runtime:2.8.5`、`kotlinx-coroutines-android:1.11.0`、`kotlinx-coroutines-core:1.11.0`、`kotlinx-coroutines-test:1.11.0`；kotlin-stdlib 是 2.2.x。
  - `SharedSpike.class` 出現在 androidTest 的編譯輸出中；`sharedTestSourcesAreOnTheJvmTestClasspath` 通過。

  **如果失敗：**

  | 症狀 | 處理 |
  |---|---|
  | coroutines 出現 `incompatible version of Kotlin` 或 metadata 錯誤 | **唯一允許自行採用的退路**：把 Step 3 的三處 `1.11.0` 全部改成 `1.10.2`，重跑 Step 5。通過的話，在回報和 commit 訊息中寫明「coroutines 1.10.2（1.11.0 與 Kotlin 2.2.10 不相容）」，並把本文所有「1.11.0」視為 1.10.2。1.10.2 也失敗的話，停下來回報。 |
  | 錯誤提到 `KotlinSourceSet`、`disallowKotlinSourceSets`，或 KSP 找不到 Kotlin plugin | 不要加 `android.disallowKotlinSourceSets=false`（官方明確不建議）。回報完整錯誤後停下。可能的方案是在根目錄加 `classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.20'`，但這會改變全專案的 Kotlin 版本，必須由使用者決定。 |
  | 找不到 `SharedSpike.class`，或 JVM 測試 `Unresolved reference 'SharedSpike'` | 在 (a2) 的 `sourceSets` 裡再加 `test.kotlin.srcDir 'src/sharedTest/java'`、`androidTest.kotlin.srcDir 'src/sharedTest/java'`，重跑 Step 5。仍然失敗就停下來回報。 |
  | `OrgJsonOnClasspathTest` 出現 `Method ... not mocked` | 回報後停下；不要改用 `unitTests.returnDefaultValues`（會讓錯誤被吞掉）。 |
  | 其他 | 貼完整錯誤後停下。 |

- [ ] **Step 6：刪掉 spike，確認建置仍然乾淨**

  ```bash
  rm -r hymnchtv/src/main/java/org/cog/hymnchtv/notebook/spike hymnchtv/src/test/java/org/cog/hymnchtv/notebook/spike \
        hymnchtv/src/sharedTest/java/org/cog/hymnchtv/notebook/spike hymnchtv/schemas/org.cog.hymnchtv.notebook.spike.SpikeDatabase
  ./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug --console=plain
  ls hymnchtv/schemas 2>/dev/null | wc -l
  ```

  Expected: BUILD SUCCESSFUL；輸出 `0`。

- [ ] **Step 7：建立 D-1a 專用模擬器，並記錄 AGP 實際的解除安裝行為**

  ```bash
  echo no | avdmanager create avd -n api34b -k "system-images;android-34;google_apis;arm64-v8a"
  echo no | avdmanager create avd -n api24b -k "system-images;android-24;google_apis;arm64-v8a"
  /opt/homebrew/share/android-commandlinetools/emulator/emulator -avd api34b -port 5580 -no-snapshot-save &
  export ANDROID_SERIAL=emulator-5580
  adb wait-for-device && adb shell getprop sys.boot_completed      # repeat until it prints 1
  test "$(adb emu avd name | head -1 | tr -d '\r')" = api34b && echo "device OK"
  ./gradlew :hymnchtv:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=org.cog.hymnchtv.ResourceLocaleResolutionTest --console=plain
  adb shell pm list packages | grep -c 'org.cog.hymnchtv'
  ```

  Expected:
  - 兩個 AVD 都建立成功；印出 `device OK`；測試通過。
  - 最後一行是 `0`（測試後已解除安裝）或 `2`（app 和 test APK 都還在）。**把實際數字寫進回報**。之後的步驟不依賴這個結果，但它決定 Task 13 是否需要先手動安裝。

- [ ] **Step 8：Commit**

  ```bash
  git add build.gradle hymnchtv/build.gradle hymnchtv/src/test/java/org/cog/hymnchtv/notebook/OrgJsonOnClasspathTest.kt
  git commit -m "chore: add KSP, Room, coroutines and JVM org.json for the notebook" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

### Task 1：核心模型（Lane 0）

> 已由 Room 統一計畫 Task 6(a) 遷移：本 Task 內的 `NotebookDatabase`／`notebook.db`／`TRUNCATE` 為歷史片段，現況為 `HymnchtvDatabase`（`hymnchtv.db`、WAL）。

**Files:**
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/notebook/model/HymnTypes.kt`、`HymnNumbering.kt`、`HymnKey.kt`、`Occasion.kt`、`SingSource.kt`、`SyncRecord.kt`、`Clock.kt`、`FavoriteIds.kt`、`DedupeWindow.kt`、`NotebookValidation.kt`
- Test（JVM）: `hymnchtv/src/test/java/org/cog/hymnchtv/notebook/model/HymnKeyTest.kt`、`HymnTypesConsistencyTest.java`、`EnumParsingTest.kt`、`FavoriteIdsTest.kt`、`SyncRecordTest.kt`、`DedupeWindowTest.kt`、`NotebookValidationTest.kt`
- Test（instrumented，在 Task 2 Step 7 執行）: `hymnchtv/src/androidTest/java/org/cog/hymnchtv/notebook/model/HymnNumberingConsistencyTest.kt`

- [ ] **Step 1：寫會失敗的測試**

  `HymnKeyTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.model

  import com.google.common.truth.Truth.assertThat
  import org.junit.Test

  class HymnKeyTest {
      private fun throwsIae(block: () -> Unit) =
          assertThat(runCatching(block).exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)

      @Test
      fun ofDerivesFuOnlyForDbAboveOffset() {
          assertThat(HymnKey.of(HymnTypes.DB, 780).isFu).isFalse()
          assertThat(HymnKey.of(HymnTypes.DB, 781).isFu).isTrue()
          assertThat(HymnKey.of(HymnTypes.DB, 786).isFu).isTrue()
          assertThat(HymnKey.of(HymnTypes.YB, 276).isFu).isFalse()
          assertThat(HymnKey.of(HymnTypes.BB, 1005).isFu).isFalse()
      }

      @Test
      fun rangesFollowHymnNoValidate() {
          val valid = listOf(
              HymnTypes.DB to 1, HymnTypes.DB to 786, HymnTypes.BB to 37, HymnTypes.BB to 101, HymnTypes.BB to 1005,
              HymnTypes.ER to 17, HymnTypes.ER to 101, HymnTypes.ER to 1232, HymnTypes.XB to 167, HymnTypes.XB to 171,
              HymnTypes.XG to 33, HymnTypes.XG to 35, HymnTypes.XG to 206, HymnTypes.YB to 277,
          )
          val invalid = listOf(
              HymnTypes.DB to 0, HymnTypes.DB to 787, HymnTypes.BB to 38, HymnTypes.BB to 100, HymnTypes.BB to 1006,
              HymnTypes.BB to 2000, HymnTypes.ER to 18, HymnTypes.ER to 1233, HymnTypes.XB to 168, HymnTypes.XB to 170,
              HymnTypes.XB to 172, HymnTypes.XG to 34, HymnTypes.XG to 207, HymnTypes.YB to 278, "x" to 1,
          )
          valid.forEach { (type, no) -> assertThat(HymnKey.isValid(type, no)).isTrue() }
          invalid.forEach { (type, no) ->
              assertThat(HymnKey.isValid(type, no)).isFalse()
              assertThat(HymnKey.ofOrNull(type, no)).isNull()
              throwsIae { HymnKey.of(type, no) }
          }
          assertThat(HymnKey.ofOrNull(null, 1)).isNull()
      }

      @Test
      fun nonCanonicalFuFlagIsRejected() {
          throwsIae { HymnKey(HymnTypes.DB, 781, false) }
          throwsIae { HymnKey(HymnTypes.DB, 1, true) }
          throwsIae { HymnKey(HymnTypes.YB, 276, true) }
          assertThat(HymnKey.isCanonical(HymnTypes.DB, 781, false)).isFalse()
          assertThat(HymnKey.isCanonical(HymnTypes.DB, 781, true)).isTrue()
      }

      @Test
      fun equalityIsByValue() {
          assertThat(HymnKey.of(HymnTypes.ER, 12)).isEqualTo(HymnKey(HymnTypes.ER, 12, false))
      }
  }
  ```

  `HymnTypesConsistencyTest.java`（用 Java 寫：javac 會內嵌 `static final` 常數，不會載入 `MainActivity`／`HymnNoValidate`；後者的 static 初始化會呼叫 `android.util.Range`，在 JVM 上會失敗）：

  ```java
  package org.cog.hymnchtv.notebook.model;

  import static com.google.common.truth.Truth.assertThat;

  import org.cog.hymnchtv.MainActivity;
  import org.cog.hymnchtv.utils.HymnNoValidate;
  import org.junit.Test;

  public class HymnTypesConsistencyTest {
      @Test
      public void hymnTypesMirrorMainActivity() {
          assertThat(HymnTypes.DB).isEqualTo(MainActivity.HYMN_DB);
          assertThat(HymnTypes.BB).isEqualTo(MainActivity.HYMN_BB);
          assertThat(HymnTypes.ER).isEqualTo(MainActivity.HYMN_ER);
          assertThat(HymnTypes.XB).isEqualTo(MainActivity.HYMN_XB);
          assertThat(HymnTypes.XG).isEqualTo(MainActivity.HYMN_XG);
          assertThat(HymnTypes.YB).isEqualTo(MainActivity.HYMN_YB);
      }

      @Test
      public void numberingConstantsMirrorHymnNoValidate() {
          assertThat(HymnNumbering.DB_NO_MAX).isEqualTo(HymnNoValidate.HYMN_DB_NO_MAX);
          assertThat(HymnNumbering.DB_NO_TMAX).isEqualTo(HymnNoValidate.HYMN_DB_NO_TMAX);
          assertThat(HymnNumbering.BB_NO_MAX).isEqualTo(HymnNoValidate.HYMN_BB_NO_MAX);
          assertThat(HymnNumbering.BB_DUMMY).isEqualTo(HymnNoValidate.HYMN_BB_DUMMY);
          assertThat(HymnNumbering.ER_NO_MAX).isEqualTo(HymnNoValidate.HYMN_ER_NO_MAX);
          assertThat(HymnNumbering.XB_NO_MAX).isEqualTo(HymnNoValidate.HYMN_XB_NO_MAX);
          assertThat(HymnNumbering.XG_NO_MAX).isEqualTo(HymnNoValidate.HYMN_XG_NO_MAX);
          assertThat(HymnNumbering.YB_NO_TMAX).isEqualTo(HymnNoValidate.HYMN_YB_NO_TMAX);
      }
  }
  ```

  `androidTest/.../notebook/model/HymnNumberingConsistencyTest.kt`（裝置上可以載入 `HymnNoValidate`，所以直接比對陣列，並逐號比對空缺範圍）：

  ```kotlin
  package org.cog.hymnchtv.notebook.model

  import androidx.test.ext.junit.runners.AndroidJUnit4
  import com.google.common.truth.Truth.assertThat
  import org.cog.hymnchtv.utils.HymnNoValidate
  import org.junit.Test
  import org.junit.runner.RunWith

  @RunWith(AndroidJUnit4::class)
  class HymnNumberingConsistencyTest {
      @Test
      fun tablesMirrorHymnNoValidate() {
          assertThat(HymnNumbering.BB_LIMITS.toList()).isEqualTo(HymnNoValidate.rangeBbLimit.toList())
          assertThat(HymnNumbering.ER_LIMITS.toList()).isEqualTo(HymnNoValidate.rangeErLimit.toList())
          assertThat(HymnNumbering.XB_INVALID).isEqualTo(HymnNoValidate.rangeXbInvalid.toSet())
          assertThat(HymnNumbering.XG_INVALID).isEqualTo(HymnNoValidate.rangeXgInvalid.toSet())
      }

      @Test
      fun gapsMatchTheLegacyRangesNumberByNumber() {
          (1..2100).forEach { n ->
              val bbLegacy = n <= HymnNoValidate.HYMN_BB_NO_MAX && HymnNoValidate.rangeBbInvalid.none { it.contains(n) }
              val erLegacy = n <= HymnNoValidate.HYMN_ER_NO_MAX && HymnNoValidate.rangeErInvalid.none { it.contains(n) }
              assertThat(HymnNumbering.isValid(HymnTypes.BB, n)).isEqualTo(bbLegacy)
              assertThat(HymnNumbering.isValid(HymnTypes.ER, n)).isEqualTo(erLegacy)
          }
      }
  }
  ```

  `EnumParsingTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.model

  import com.google.common.truth.Truth.assertThat
  import org.junit.Test

  class EnumParsingTest {
      @Test
      fun occasionParsesStoredNames() {
          Occasion.entries.forEach { assertThat(Occasion.fromStorage(it.name)).isEqualTo(it) }
      }

      @Test
      fun occasionReturnsNullForUnknown() {
          listOf(null, "", "lords_day", "主日", "SUNDAY").forEach {
              assertThat(Occasion.fromStorage(it)).isNull()
          }
      }

      @Test
      fun sourceParsesStoredNames() {
          assertThat(SingSource.fromStorage("AUTO")).isEqualTo(SingSource.AUTO)
          assertThat(SingSource.fromStorage("MANUAL")).isEqualTo(SingSource.MANUAL)
          assertThat(SingSource.fromStorage("auto")).isNull()
          assertThat(SingSource.fromStorage(null)).isNull()
      }
  }
  ```

  `FavoriteIdsTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.model

  import com.google.common.truth.Truth.assertThat
  import org.junit.Test

  class FavoriteIdsTest {
      @Test
      fun sameKeyAlwaysGivesSameId() {
          assertThat(FavoriteIds.forKey(HymnKey.of(HymnTypes.DB, 1)))
              .isEqualTo(FavoriteIds.forKey(HymnKey.of(HymnTypes.DB, 1)))
      }

      @Test
      fun differentKeysGiveDifferentIds() {
          val ids = listOf(
              HymnKey.of(HymnTypes.DB, 1), HymnKey.of(HymnTypes.BB, 1),
              HymnKey.of(HymnTypes.DB, 781), HymnKey.of(HymnTypes.ER, 1),
          ).map(FavoriteIds::forKey)
          assertThat(ids.toSet()).hasSize(4)
      }

      @Test
      fun idIsACanonicalUuid() {
          val id = FavoriteIds.forKey(HymnKey.of(HymnTypes.XG, 7))
          assertThat(NotebookValidation.uuid(id)).isEqualTo(id)
      }
  }
  ```

  `SyncRecordTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.model

  import com.google.common.truth.Truth.assertThat
  import org.junit.Test

  class SyncRecordTest {
      private data class Row(
          override val id: String = "r",
          override val createdAt: Long = 0,
          override val updatedAt: Long,
          override val deletedAt: Long? = null,
          override val updatedBy: String = "d",
      ) : SyncRecord

      @Test
      fun activeMeansNotDeleted() {
          assertThat(Row(updatedAt = 1).isActive).isTrue()
          assertThat(Row(updatedAt = 1, deletedAt = 1).isActive).isFalse()
      }

      @Test
      fun versionIsLatestOfUpdatedAndDeleted() {
          assertThat(Row(updatedAt = 5).version).isEqualTo(5L)
          assertThat(Row(updatedAt = 5, deletedAt = 9).version).isEqualTo(9L)
          assertThat(Row(updatedAt = 9, deletedAt = 5).version).isEqualTo(9L)
      }
  }
  ```

  `DedupeWindowTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.model

  import com.google.common.truth.Truth.assertThat
  import org.junit.Test

  class DedupeWindowTest {
      private val window = 3 * 3_600_000L
      private val now = 100 * 3_600_000L

      @Test
      fun strictlyInsideTheWindowOnBothSides() {
          assertThat(DedupeWindow.contains(now - window + 1, now, window)).isTrue()
          assertThat(DedupeWindow.contains(now + window - 1, now, window)).isTrue()
          assertThat(DedupeWindow.contains(now - window, now, window)).isFalse()
          assertThat(DedupeWindow.contains(now + window, now, window)).isFalse()
      }

      @Test
      fun zeroWindowNeverMatches() {
          assertThat(DedupeWindow.contains(now, now, 0)).isFalse()
          assertThat(DedupeWindow.lowerExclusive(now, 0)).isEqualTo(now)
          assertThat(DedupeWindow.upperExclusive(now, 0)).isEqualTo(now)
      }

      @Test
      fun boundsSaturateInsteadOfOverflowing() {
          assertThat(DedupeWindow.lowerExclusive(Long.MIN_VALUE + 5, 10)).isEqualTo(Long.MIN_VALUE)
          assertThat(DedupeWindow.upperExclusive(Long.MAX_VALUE - 5, 10)).isEqualTo(Long.MAX_VALUE)
          assertThat(DedupeWindow.contains(Long.MAX_VALUE - 1, Long.MAX_VALUE - 2, window)).isTrue()
          assertThat(runCatching { DedupeWindow.upperExclusive(0, -1) }.exceptionOrNull())
              .isInstanceOf(IllegalArgumentException::class.java)
      }
  }
  ```

  `NotebookValidationTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.model

  import com.google.common.truth.Truth.assertThat
  import org.junit.Test

  class NotebookValidationTest {
      private fun rejects(block: () -> Unit) =
          assertThat(runCatching(block).exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)

      @Test
      fun uuidMustBeCanonicalLowercase() {
          val ok = "123e4567-e89b-12d3-a456-426614174000"
          assertThat(NotebookValidation.uuid(ok)).isEqualTo(ok)
          listOf(ok.uppercase(), "1-1-1-1-1", "", " ", "../../etc/passwd", "$ok ", ok + "0", "x".repeat(500))
              .forEach { rejects { NotebookValidation.uuid(it) } }
      }

      @Test
      fun noteBodyKeepsTextAsIs() {
          assertThat(NotebookValidation.noteBody("  感謝主\n")).isEqualTo("  感謝主\n")
          assertThat(NotebookValidation.noteBody("字".repeat(NotebookValidation.MAX_NOTE_LENGTH))).hasLength(100_000)
          rejects { NotebookValidation.noteBody("   ") }
          rejects { NotebookValidation.noteBody("字".repeat(NotebookValidation.MAX_NOTE_LENGTH + 1)) }
      }

      @Test
      fun playlistNameIsTrimmed() {
          assertThat(NotebookValidation.playlistName("  主日 10/4 ")).isEqualTo("主日 10/4")
          assertThat(NotebookValidation.MAX_PLAYLIST_NAME_LENGTH).isEqualTo(200)
          rejects { NotebookValidation.playlistName("  ") }
          rejects { NotebookValidation.playlistName("a".repeat(NotebookValidation.MAX_PLAYLIST_NAME_LENGTH + 1)) }
      }

      @Test
      fun timeRange() {
          NotebookValidation.timeRange(1, 1)
          rejects { NotebookValidation.timeRange(2, 1) }
      }

      @Test
      fun sungAtMustBeBetweenZeroAndTwentyFourHoursAhead() {
          val now = 1_000_000L
          val day = NotebookValidation.MAX_FUTURE_SKEW_MILLIS
          assertThat(NotebookValidation.sungAt(0, now)).isEqualTo(0L)
          assertThat(NotebookValidation.sungAt(now + day, now)).isEqualTo(now + day)
          rejects { NotebookValidation.sungAt(-1, now) }
          rejects { NotebookValidation.sungAt(now + day + 1, now) }
          rejects { NotebookValidation.sungAt(Long.MAX_VALUE, now) }
      }
  }
  ```

- [ ] **Step 2：執行測試，確認它失敗**

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.notebook.model.*'`
  Expected: 編譯失敗，訊息含 `Unresolved reference 'HymnKey'`（或 Java 的 `cannot find symbol HymnTypes`）。

- [ ] **Step 3：實作**

  `HymnTypes.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.model

  /** Hymn book identifiers; values mirror MainActivity.HYMN_* (asserted by HymnTypesConsistencyTest). */
  object HymnTypes {
      const val DB = "hymn_db"
      const val BB = "hymn_bb"
      const val ER = "hymn_er"
      const val XB = "hymn_xb"
      const val XG = "hymn_xg"
      const val YB = "hymn_yb"

      @JvmField
      val ALL: Set<String> = setOf(DB, BB, ER, XB, XG, YB)
  }
  ```

  `HymnNumbering.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.model

  /**
   * Valid internal hymn numbers per book, copied from utils/HymnNoValidate (asserted by HymnTypesConsistencyTest
   * and the instrumented HymnNumberingConsistencyTest). Internal numbers are what MainActivity.showContent()
   * receives: 附 of hymn_db are 780 + n (n = 1..6); 青年詩歌 supplements continue after 275 (276..277).
   */
  object HymnNumbering {
      const val DB_NO_MAX = 780
      const val DB_NO_TMAX = 786
      const val BB_NO_MAX = 1005
      const val BB_DUMMY = 2000
      const val ER_NO_MAX = 1232
      const val XB_NO_MAX = 171
      const val XG_NO_MAX = 206
      const val YB_NO_TMAX = 277

      /** HymnNoValidate.rangeBbLimit: number i opens the gap [limit[i], 100 * (i + 1)]; the last entry has no gap. */
      @JvmField
      val BB_LIMITS: IntArray = intArrayOf(38, 151, 259, 350, 471, 544, 630, 763, 881, 931, 1006)

      /** HymnNoValidate.rangeErLimit, same rule as BB_LIMITS. */
      @JvmField
      val ER_LIMITS: IntArray = intArrayOf(18, 125, 213, 324, 446, 525, 622, 720, 837, 921, 1040, 1119, 1233)

      @JvmField
      val XB_INVALID: Set<Int> = setOf(168, 169, 170)

      @JvmField
      val XG_INVALID: Set<Int> = setOf(34)

      @JvmStatic
      fun isValid(hymnType: String?, hymnNo: Int): Boolean = when (hymnType) {
          HymnTypes.DB -> hymnNo in 1..DB_NO_TMAX
          HymnTypes.BB -> hymnNo in 1..BB_NO_MAX && !inGap(hymnNo, BB_LIMITS)
          HymnTypes.ER -> hymnNo in 1..ER_NO_MAX && !inGap(hymnNo, ER_LIMITS)
          HymnTypes.XB -> hymnNo in 1..XB_NO_MAX && hymnNo !in XB_INVALID
          HymnTypes.XG -> hymnNo in 1..XG_NO_MAX && hymnNo !in XG_INVALID
          HymnTypes.YB -> hymnNo in 1..YB_NO_TMAX
          else -> false
      }

      /** Same as MediaRecord.isFu(): only hymn_db numbers above 780 are 附. */
      @JvmStatic
      fun isFu(hymnType: String, hymnNo: Int): Boolean = hymnType == HymnTypes.DB && hymnNo > DB_NO_MAX

      /** HymnNoValidate builds inclusive Range(limit[i], 100 * (i + 1)) for every limit except the last. */
      private fun inGap(hymnNo: Int, limits: IntArray): Boolean =
          (0 until limits.size - 1).any { i -> hymnNo in limits[i]..(100 * (i + 1)) }
  }
  ```

  `HymnKey.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.model

  import androidx.room.ColumnInfo

  /**
   * Canonical hymn identity: (hymnType, internal hymnNo, isFu) where isFu is always derived from the number,
   * so a 附 hymn has exactly one representation. Embedded in every notebook entity.
   */
  data class HymnKey(
      @ColumnInfo(name = COL_TYPE) val hymnType: String,
      @ColumnInfo(name = COL_NO) val hymnNo: Int,
      @ColumnInfo(name = COL_FU) val isFu: Boolean,
  ) {
      init {
          require(isCanonical(hymnType, hymnNo, isFu)) { "Non-canonical hymn key: $hymnType/$hymnNo/fu=$isFu" }
      }

      companion object {
          const val COL_TYPE = "hymnType"
          const val COL_NO = "hymnNo"
          const val COL_FU = "isFu"

          @JvmStatic
          fun isValid(hymnType: String?, hymnNo: Int): Boolean = HymnNumbering.isValid(hymnType, hymnNo)

          @JvmStatic
          fun isCanonical(hymnType: String?, hymnNo: Int, isFu: Boolean): Boolean =
              hymnType != null && HymnNumbering.isValid(hymnType, hymnNo) && isFu == HymnNumbering.isFu(hymnType, hymnNo)

          /** Throws IllegalArgumentException when the number is not valid for the book. */
          @JvmStatic
          fun of(hymnType: String, hymnNo: Int): HymnKey = HymnKey(hymnType, hymnNo, HymnNumbering.isFu(hymnType, hymnNo))

          /** Null instead of an exception, for UI callers holding unchecked values. */
          @JvmStatic
          fun ofOrNull(hymnType: String?, hymnNo: Int): HymnKey? =
              if (hymnType != null && HymnNumbering.isValid(hymnType, hymnNo)) of(hymnType, hymnNo) else null
      }
  }
  ```

  `Occasion.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.model

  /** Where a hymn was sung. Stored by name; UI labels: 主日／小排／禱告聚會／晨興／家中／其他. */
  enum class Occasion {
      LORDS_DAY, SMALL_GROUP, PRAYER_MEETING, MORNING_REVIVAL, HOME, OTHER;

      companion object {
          /** Never throws: unknown or null values return null so callers choose the fallback. */
          @JvmStatic
          fun fromStorage(value: String?): Occasion? = entries.firstOrNull { it.name == value }
      }
  }
  ```

  `SingSource.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.model

  /** How a sing log was created. */
  enum class SingSource {
      AUTO, MANUAL;

      companion object {
          @JvmStatic
          fun fromStorage(value: String?): SingSource? = entries.firstOrNull { it.name == value }
      }
  }
  ```

  `SyncRecord.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.model

  /** Sync-ready columns shared by every notebook table (see master plan 子項目 S). Times are epoch millis. */
  interface SyncRecord {
      val id: String
      val createdAt: Long
      val updatedAt: Long
      val deletedAt: Long?

      /** Device id (NotebookPrefs.deviceId()) of the last writer; merge tie-breaker. */
      val updatedBy: String
  }

  val SyncRecord.isActive: Boolean
      get() = deletedAt == null

  /** Merge version: a delete stamped after the last update counts as the newer change. */
  val SyncRecord.version: Long
      get() = maxOf(updatedAt, deletedAt ?: Long.MIN_VALUE)
  ```

  `Clock.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.model

  import java.util.UUID

  fun interface Clock {
      fun nowMillis(): Long

      companion object {
          @JvmField
          val SYSTEM: Clock = Clock { System.currentTimeMillis() }
      }
  }

  fun interface IdGenerator {
      fun newId(): String

      companion object {
          @JvmField
          val RANDOM_UUID: IdGenerator = IdGenerator { UUID.randomUUID().toString() }
      }
  }

  /** Supplies the id stamped into updatedBy. Called on background threads only (may hit disk once). */
  fun interface DeviceIdProvider {
      fun deviceId(): String
  }
  ```

  `FavoriteIds.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.model

  import java.util.UUID

  /**
   * Favorites use a name-based UUID so the same hymn has the same id on every device:
   * backup merges and future sync match by id and never collide on the unique hymn index.
   */
  object FavoriteIds {
      @JvmStatic
      fun forKey(key: HymnKey): String =
          UUID.nameUUIDFromBytes("favorite:${key.hymnType}:${key.hymnNo}:${key.isFu}".toByteArray(Charsets.UTF_8))
              .toString()
  }
  ```

  `DedupeWindow.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.model

  /**
   * An existing log is a duplicate of a new one at [at] when |existing − at| < window (both bounds exclusive).
   * Bounds saturate at Long.MIN_VALUE / Long.MAX_VALUE instead of overflowing.
   */
  object DedupeWindow {
      @JvmStatic
      fun lowerExclusive(at: Long, windowMillis: Long): Long {
          require(windowMillis >= 0) { "windowMillis must be >= 0" }
          return if (at < Long.MIN_VALUE + windowMillis) Long.MIN_VALUE else at - windowMillis
      }

      @JvmStatic
      fun upperExclusive(at: Long, windowMillis: Long): Long {
          require(windowMillis >= 0) { "windowMillis must be >= 0" }
          return if (at > Long.MAX_VALUE - windowMillis) Long.MAX_VALUE else at + windowMillis
      }

      @JvmStatic
      fun contains(existing: Long, at: Long, windowMillis: Long): Boolean =
          existing > lowerExclusive(at, windowMillis) && existing < upperExclusive(at, windowMillis)
  }
  ```

  `NotebookValidation.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.model

  import java.util.UUID

  /** Boundary validation for user input and imported rows; throws IllegalArgumentException. */
  object NotebookValidation {
      const val MAX_NOTE_LENGTH = 100_000
      const val MAX_PLAYLIST_NAME_LENGTH = 200

      /** Accepts only the lowercase canonical form produced by UUID.toString(). */
      @JvmStatic
      fun uuid(value: String): String {
          val canonical = try {
              UUID.fromString(value).toString()
          } catch (e: IllegalArgumentException) {
              null
          }
          require(canonical == value) { "Not a canonical lowercase UUID" }
          return value
      }

      /** Note text is kept verbatim (leading spaces and line breaks are meaningful). */
      @JvmStatic
      fun noteBody(value: String): String {
          require(value.isNotBlank()) { "Note must not be blank" }
          require(value.length <= MAX_NOTE_LENGTH) { "Note longer than $MAX_NOTE_LENGTH characters" }
          return value
      }

      @JvmStatic
      fun playlistName(value: String): String {
          val trimmed = value.trim()
          require(trimmed.isNotEmpty()) { "Playlist name must not be blank" }
          require(trimmed.length <= MAX_PLAYLIST_NAME_LENGTH) { "Playlist name longer than $MAX_PLAYLIST_NAME_LENGTH" }
          return trimmed
      }

      @JvmStatic
      fun timeRange(fromInclusive: Long, toExclusive: Long) {
          require(fromInclusive <= toExclusive) { "Invalid range $fromInclusive..$toExclusive" }
      }

      /** Same skew limit as backup import (BackupLimits.maxFutureSkewMillis). */
      const val MAX_FUTURE_SKEW_MILLIS = 24 * 60 * 60 * 1000L

      /** A sing time must be >= 0 and at most 24 h after [nowMillis]. */
      @JvmStatic
      fun sungAt(value: Long, nowMillis: Long): Long {
          require(value >= 0) { "sungAt must be >= 0" }
          require(value <= nowMillis + MAX_FUTURE_SKEW_MILLIS) { "sungAt is more than 24 h in the future" }
          return value
      }
  }
  ```

- [ ] **Step 4：執行測試，確認它通過**

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.notebook.model.*' && ./gradlew :hymnchtv:assembleDebugAndroidTest`
  Expected: 7 個 JVM 測試類別、22 個測試全部通過；androidTest 能編譯（`HymnNumberingConsistencyTest` 在 Task 2 Step 7 執行）。

- [ ] **Step 5：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/notebook/model hymnchtv/src/test/java/org/cog/hymnchtv/notebook/model hymnchtv/src/androidTest/java/org/cog/hymnchtv/notebook/model
  git commit -m "feat: add notebook core model with canonical hymn keys and validation" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

### Task 2：Room entity、DAO、資料庫與 schema（Lane 0）

> 已由 Room 統一計畫 Task 6(a) 遷移：本 Task 內的 `NotebookDatabase`／`notebook.db`／`TRUNCATE` 為歷史片段，現況為 `HymnchtvDatabase`（`hymnchtv.db`、WAL）。

**Files:**
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/notebook/data/entity/FavoriteEntity.kt`、`SingLogEntity.kt`、`NoteEntity.kt`、`PlaylistEntity.kt`、`PlaylistItemEntity.kt`
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/notebook/data/SingStats.kt`、`NotebookConverters.kt`、`NotebookDatabase.kt`
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/notebook/data/dao/DaoSql.kt`、`FavoriteDao.kt`、`SingLogDao.kt`、`NoteDao.kt`、`PlaylistDao.kt`、`PlaylistItemDao.kt`
- Generate: `hymnchtv/schemas/org.cog.hymnchtv.notebook.data.NotebookDatabase/1.json`
- Test: `hymnchtv/src/androidTest/java/org/cog/hymnchtv/notebook/data/NotebookDaoTest.kt`

（DAO 不做驗證，所以 DAO 測試可以用簡短的字串 id；驗證在 repository 和 codec。）

- [ ] **Step 1：寫會失敗的 instrumented test** `NotebookDaoTest.kt`

  ```kotlin
  package org.cog.hymnchtv.notebook.data

  import android.database.sqlite.SQLiteConstraintException
  import androidx.test.core.app.ApplicationProvider
  import androidx.test.ext.junit.runners.AndroidJUnit4
  import com.google.common.truth.Truth.assertThat
  import kotlinx.coroutines.runBlocking
  import org.cog.hymnchtv.notebook.data.entity.FavoriteEntity
  import org.cog.hymnchtv.notebook.data.entity.NoteEntity
  import org.cog.hymnchtv.notebook.data.entity.PlaylistItemEntity
  import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
  import org.cog.hymnchtv.notebook.model.FavoriteIds
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.HymnTypes
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.cog.hymnchtv.notebook.model.SingSource
  import org.junit.After
  import org.junit.Before
  import org.junit.Test
  import org.junit.runner.RunWith

  @RunWith(AndroidJUnit4::class)
  class NotebookDaoTest {
      private lateinit var db: NotebookDatabase
      private val db1 = HymnKey.of(HymnTypes.DB, 1)
      private val fu1 = HymnKey.of(HymnTypes.DB, 781)
      private val er1 = HymnKey.of(HymnTypes.ER, 1)

      @Before
      fun setUp() {
          db = NotebookDatabase.inMemory(ApplicationProvider.getApplicationContext())
      }

      @After
      fun tearDown() = db.close()

      private fun fav(key: HymnKey, deletedAt: Long? = null) =
          FavoriteEntity(FavoriteIds.forKey(key), key, createdAt = 10, updatedAt = 10, deletedAt = deletedAt)

      private fun log(id: String, key: HymnKey, sungAt: Long, deletedAt: Long? = null) = SingLogEntity(
          id = id, hymn = key, sungAt = sungAt, occasion = Occasion.HOME, source = SingSource.AUTO,
          createdAt = 1, updatedAt = 1, deletedAt = deletedAt,
      )

      private fun item(id: String, playlistId: String, position: Int, key: HymnKey = db1, deletedAt: Long? = null) =
          PlaylistItemEntity(id, playlistId, position, key, createdAt = 1, updatedAt = 1, deletedAt = deletedAt)

      @Test
      fun favoriteActiveVersusDeleted(): Unit = runBlocking {
          val dao = db.favoriteDao()
          dao.upsert(fav(db1))
          dao.upsert(fav(fu1, deletedAt = 20))

          assertThat(dao.isFavorite(db1.hymnType, db1.hymnNo, db1.isFu)).isTrue()
          assertThat(dao.isFavorite(fu1.hymnType, fu1.hymnNo, fu1.isFu)).isFalse()
          assertThat(dao.isFavorite(er1.hymnType, er1.hymnNo, er1.isFu)).isFalse()
          assertThat(dao.findActive().map { it.hymn }).containsExactly(db1)
          assertThat(dao.findAllIncludingDeleted()).hasSize(2)
          assertThat(dao.findById(FavoriteIds.forKey(fu1))?.deletedAt).isEqualTo(20L)
      }

      @Test
      fun singStatsCountActiveOnly(): Unit = runBlocking {
          val dao = db.singLogDao()
          dao.upsertAll(
              listOf(log("a", db1, 100), log("b", db1, 300), log("c", db1, 500, deletedAt = 600), log("d", fu1, 900)),
          )
          assertThat(dao.statsFor(db1.hymnType, db1.hymnNo, db1.isFu)).isEqualTo(SingStats(2, 300L))
          assertThat(dao.statsFor(er1.hymnType, er1.hymnNo, er1.isFu)).isEqualTo(SingStats(0, null))
          assertThat(dao.latestForHymn(db1.hymnType, db1.hymnNo, db1.isFu)?.id).isEqualTo("b")
          assertThat(dao.findByHymn(db1.hymnType, db1.hymnNo, db1.isFu).map { it.id }).containsExactly("b", "a").inOrder()
      }

      @Test
      fun existsBetweenIsExclusiveAndIgnoresDeletedAndOtherHymns(): Unit = runBlocking {
          val dao = db.singLogDao()
          dao.upsertAll(listOf(log("a", db1, 100), log("b", db1, 150, deletedAt = 151), log("c", fu1, 160)))
          assertThat(dao.existsBetween(db1.hymnType, db1.hymnNo, db1.isFu, 99, 200)).isTrue()
          assertThat(dao.existsBetween(db1.hymnType, db1.hymnNo, db1.isFu, 100, 200)).isFalse()
          assertThat(dao.existsBetween(db1.hymnType, db1.hymnNo, db1.isFu, 0, 100)).isFalse()
          assertThat(dao.existsBetween(db1.hymnType, db1.hymnNo, db1.isFu, 120, 200)).isFalse()
      }

      @Test
      fun findBetweenIsHalfOpenAndNewestFirst(): Unit = runBlocking {
          val dao = db.singLogDao()
          dao.upsertAll(listOf(log("a", db1, 100), log("b", fu1, 200), log("c", er1, 300)))
          assertThat(dao.findBetween(100, 300).map { it.id }).containsExactly("b", "a").inOrder()
      }

      @Test
      fun enumsNullablesAndUpdatedByRoundTrip(): Unit = runBlocking {
          val dao = db.singLogDao()
          val row = log("x", db1, 1).copy(
              occasion = Occasion.PRAYER_MEETING, source = SingSource.MANUAL, playlistId = "p1", updatedBy = "dev-1",
          )
          dao.upsert(row)
          assertThat(dao.findById("x")).isEqualTo(row)
      }

      @Test
      fun notesNewestFirstPerHymn(): Unit = runBlocking {
          val dao = db.noteDao()
          dao.upsertAll(
              listOf(
                  NoteEntity("n1", db1, "第一則", null, createdAt = 1, updatedAt = 1),
                  NoteEntity("n2", db1, "第二則", "x", createdAt = 2, updatedAt = 2),
                  NoteEntity("n3", fu1, "附歌", null, createdAt = 3, updatedAt = 3),
              ),
          )
          assertThat(dao.findByHymn(db1.hymnType, db1.hymnNo, db1.isFu).map { it.id }).containsExactly("n2", "n1").inOrder()
      }

      @Test
      fun playlistItemsOrderedAndMaxPositionIncludesDeleted(): Unit = runBlocking {
          val dao = db.playlistItemDao()
          assertThat(dao.maxPositionIncludingDeleted("p")).isNull()
          dao.upsertAll(
              listOf(item("i2", "p", 1, fu1), item("i1", "p", 0), item("i3", "p", 2, er1, deletedAt = 5), item("i4", "other", 0)),
          )
          assertThat(dao.itemsOf("p").map { it.id }).containsExactly("i1", "i2").inOrder()
          assertThat(dao.maxPositionIncludingDeleted("p")).isEqualTo(2)
      }

      @Test
      fun positionIsUniquePerPlaylist(): Unit = runBlocking {
          val dao = db.playlistItemDao()
          dao.insert(item("i1", "p", 0))
          assertThat(runCatching { dao.insert(item("i9", "p", 0)) }.exceptionOrNull())
              .isInstanceOf(SQLiteConstraintException::class.java)
          dao.insert(item("i8", "other", 0))
          assertThat(dao.findAllIncludingDeleted()).hasSize(2)
      }
  }
  ```

- [ ] **Step 2：確認它編譯失敗**

  Run: `./gradlew :hymnchtv:assembleDebugAndroidTest`
  Expected: 編譯失敗，`Unresolved reference 'NotebookDatabase'`。

- [ ] **Step 3：實作 entity 與 `SingStats`**

  所有 entity 的 `updatedBy` 都放在最後一個參數，預設值是 `""`。repository 和匯入一定會填入真正的裝置 id；預設值只是讓 DAO 測試寫起來簡短。

  `data/entity/FavoriteEntity.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.data.entity

  import androidx.room.Embedded
  import androidx.room.Entity
  import androidx.room.Index
  import androidx.room.PrimaryKey
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.SyncRecord

  /** One row per hymn ever favorited; id = FavoriteIds.forKey(hymn); un-favoriting is a soft delete. */
  @Entity(
      tableName = "favorite",
      indices = [Index(value = [HymnKey.COL_TYPE, HymnKey.COL_NO, HymnKey.COL_FU], unique = true)],
  )
  data class FavoriteEntity(
      @PrimaryKey override val id: String,
      @Embedded val hymn: HymnKey,
      override val createdAt: Long,
      override val updatedAt: Long,
      override val deletedAt: Long? = null,
      override val updatedBy: String = "",
  ) : SyncRecord
  ```

  `data/entity/SingLogEntity.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.data.entity

  import androidx.room.Embedded
  import androidx.room.Entity
  import androidx.room.Index
  import androidx.room.PrimaryKey
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.cog.hymnchtv.notebook.model.SingSource
  import org.cog.hymnchtv.notebook.model.SyncRecord

  /** One time a hymn was sung. No row limit (unlike hymnHistory). */
  @Entity(
      tableName = "sing_log",
      indices = [
          Index(value = [HymnKey.COL_TYPE, HymnKey.COL_NO, HymnKey.COL_FU, "sungAt"]),
          Index(value = ["sungAt"]),
          Index(value = ["playlistId"]),
      ],
  )
  data class SingLogEntity(
      @PrimaryKey override val id: String,
      @Embedded val hymn: HymnKey,
      val sungAt: Long,
      val occasion: Occasion,
      val source: SingSource,
      val playlistId: String? = null,
      override val createdAt: Long,
      override val updatedAt: Long,
      override val deletedAt: Long? = null,
      override val updatedBy: String = "",
  ) : SyncRecord
  ```

  `data/entity/NoteEntity.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.data.entity

  import androidx.room.Embedded
  import androidx.room.Entity
  import androidx.room.Index
  import androidx.room.PrimaryKey
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.SyncRecord

  /** A personal note on a hymn, optionally linked to one sing log (D-1b builds the UI). */
  @Entity(
      tableName = "note",
      indices = [
          Index(value = [HymnKey.COL_TYPE, HymnKey.COL_NO, HymnKey.COL_FU, "createdAt"]),
          Index(value = ["singLogId"]),
      ],
  )
  data class NoteEntity(
      @PrimaryKey override val id: String,
      @Embedded val hymn: HymnKey,
      val body: String,
      val singLogId: String? = null,
      override val createdAt: Long,
      override val updatedAt: Long,
      override val deletedAt: Long? = null,
      override val updatedBy: String = "",
  ) : SyncRecord
  ```

  `data/entity/PlaylistEntity.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.data.entity

  import androidx.room.Entity
  import androidx.room.PrimaryKey
  import org.cog.hymnchtv.notebook.model.SyncRecord

  @Entity(tableName = "playlist")
  data class PlaylistEntity(
      @PrimaryKey override val id: String,
      val name: String,
      override val createdAt: Long,
      override val updatedAt: Long,
      override val deletedAt: Long? = null,
      override val updatedBy: String = "",
  ) : SyncRecord
  ```

  `data/entity/PlaylistItemEntity.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.data.entity

  import androidx.room.Embedded
  import androidx.room.Entity
  import androidx.room.Index
  import androidx.room.PrimaryKey
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.SyncRecord

  /**
   * position is a sparse, only-growing sort key, unique per playlist including soft-deleted rows.
   * No foreign key on playlistId: rows may arrive out of order from backups or future sync.
   */
  @Entity(
      tableName = "playlist_item",
      indices = [Index(value = ["playlistId", "position"], unique = true)],
  )
  data class PlaylistItemEntity(
      @PrimaryKey override val id: String,
      val playlistId: String,
      val position: Int,
      @Embedded val hymn: HymnKey,
      override val createdAt: Long,
      override val updatedAt: Long,
      override val deletedAt: Long? = null,
      override val updatedBy: String = "",
  ) : SyncRecord
  ```

  `data/SingStats.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.data

  /** How many times a hymn was sung (active logs) and when last; lastSungAt is null when never. */
  data class SingStats(val singCount: Int, val lastSungAt: Long?)
  ```

- [ ] **Step 4：實作 converter 與 DAO**

  `data/NotebookConverters.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.data

  import androidx.room.TypeConverter
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.cog.hymnchtv.notebook.model.SingSource

  /** Explicit converters so unknown stored names degrade to OTHER/MANUAL instead of throwing. */
  class NotebookConverters {
      @TypeConverter
      fun occasionToStorage(value: Occasion): String = value.name

      @TypeConverter
      fun occasionFromStorage(value: String): Occasion = Occasion.fromStorage(value) ?: Occasion.OTHER

      @TypeConverter
      fun sourceToStorage(value: SingSource): String = value.name

      @TypeConverter
      fun sourceFromStorage(value: String): SingSource = SingSource.fromStorage(value) ?: SingSource.MANUAL
  }
  ```

  `data/dao/DaoSql.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.data.dao

  /** WHERE fragment matching the embedded HymnKey columns; DAO methods name their params hymnType/hymnNo/isFu. */
  internal const val HYMN_MATCH = "hymnType = :hymnType AND hymnNo = :hymnNo AND isFu = :isFu"
  ```

  `data/dao/FavoriteDao.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.data.dao

  import androidx.room.Dao
  import androidx.room.Query
  import androidx.room.Upsert
  import org.cog.hymnchtv.notebook.data.entity.FavoriteEntity

  /** Upsert is safe here: the id is derived from the hymn, so the PK and the unique hymn index always agree. */
  @Dao
  interface FavoriteDao {
      @Query("SELECT * FROM favorite WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
      suspend fun findActive(): List<FavoriteEntity>

      @Query("SELECT * FROM favorite")
      suspend fun findAllIncludingDeleted(): List<FavoriteEntity>

      @Query("SELECT * FROM favorite WHERE id = :id")
      suspend fun findById(id: String): FavoriteEntity?

      @Query("SELECT EXISTS(SELECT 1 FROM favorite WHERE " + HYMN_MATCH + " AND deletedAt IS NULL)")
      suspend fun isFavorite(hymnType: String, hymnNo: Int, isFu: Boolean): Boolean

      @Upsert
      suspend fun upsert(row: FavoriteEntity)

      @Upsert
      suspend fun upsertAll(rows: List<FavoriteEntity>)
  }
  ```

  `data/dao/SingLogDao.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.data.dao

  import androidx.room.Dao
  import androidx.room.Query
  import androidx.room.Upsert
  import org.cog.hymnchtv.notebook.data.SingStats
  import org.cog.hymnchtv.notebook.data.entity.SingLogEntity

  @Dao
  interface SingLogDao {
      @Query("SELECT * FROM sing_log WHERE deletedAt IS NULL ORDER BY sungAt DESC")
      suspend fun findActive(): List<SingLogEntity>

      @Query("SELECT * FROM sing_log")
      suspend fun findAllIncludingDeleted(): List<SingLogEntity>

      @Query("SELECT * FROM sing_log WHERE id = :id")
      suspend fun findById(id: String): SingLogEntity?

      @Query(
          "SELECT COUNT(*) AS singCount, MAX(sungAt) AS lastSungAt FROM sing_log WHERE " + HYMN_MATCH +
              " AND deletedAt IS NULL",
      )
      suspend fun statsFor(hymnType: String, hymnNo: Int, isFu: Boolean): SingStats

      @Query("SELECT * FROM sing_log WHERE " + HYMN_MATCH + " AND deletedAt IS NULL ORDER BY sungAt DESC LIMIT 1")
      suspend fun latestForHymn(hymnType: String, hymnNo: Int, isFu: Boolean): SingLogEntity?

      /** Dedupe probe used inside the recordUnlessDuplicate transaction; both bounds exclusive. */
      @Query(
          "SELECT EXISTS(SELECT 1 FROM sing_log WHERE " + HYMN_MATCH +
              " AND deletedAt IS NULL AND sungAt > :fromExclusive AND sungAt < :toExclusive)",
      )
      suspend fun existsBetween(hymnType: String, hymnNo: Int, isFu: Boolean, fromExclusive: Long, toExclusive: Long): Boolean

      @Query("SELECT * FROM sing_log WHERE " + HYMN_MATCH + " AND deletedAt IS NULL ORDER BY sungAt DESC")
      suspend fun findByHymn(hymnType: String, hymnNo: Int, isFu: Boolean): List<SingLogEntity>

      @Query(
          "SELECT * FROM sing_log WHERE deletedAt IS NULL AND sungAt >= :fromInclusive AND sungAt < :toExclusive " +
              "ORDER BY sungAt DESC",
      )
      suspend fun findBetween(fromInclusive: Long, toExclusive: Long): List<SingLogEntity>

      @Upsert
      suspend fun upsert(row: SingLogEntity)

      @Upsert
      suspend fun upsertAll(rows: List<SingLogEntity>)
  }
  ```

  `data/dao/NoteDao.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.data.dao

  import androidx.room.Dao
  import androidx.room.Query
  import androidx.room.Upsert
  import org.cog.hymnchtv.notebook.data.entity.NoteEntity

  @Dao
  interface NoteDao {
      @Query("SELECT * FROM note WHERE deletedAt IS NULL ORDER BY createdAt DESC")
      suspend fun findActive(): List<NoteEntity>

      @Query("SELECT * FROM note")
      suspend fun findAllIncludingDeleted(): List<NoteEntity>

      @Query("SELECT * FROM note WHERE id = :id")
      suspend fun findById(id: String): NoteEntity?

      @Query("SELECT * FROM note WHERE " + HYMN_MATCH + " AND deletedAt IS NULL ORDER BY createdAt DESC")
      suspend fun findByHymn(hymnType: String, hymnNo: Int, isFu: Boolean): List<NoteEntity>

      @Upsert
      suspend fun upsert(row: NoteEntity)

      @Upsert
      suspend fun upsertAll(rows: List<NoteEntity>)
  }
  ```

  `data/dao/PlaylistDao.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.data.dao

  import androidx.room.Dao
  import androidx.room.Query
  import androidx.room.Upsert
  import org.cog.hymnchtv.notebook.data.entity.PlaylistEntity

  @Dao
  interface PlaylistDao {
      @Query("SELECT * FROM playlist WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
      suspend fun findActive(): List<PlaylistEntity>

      @Query("SELECT * FROM playlist")
      suspend fun findAllIncludingDeleted(): List<PlaylistEntity>

      @Query("SELECT * FROM playlist WHERE id = :id")
      suspend fun findById(id: String): PlaylistEntity?

      @Upsert
      suspend fun upsert(row: PlaylistEntity)

      @Upsert
      suspend fun upsertAll(rows: List<PlaylistEntity>)
  }
  ```

  `data/dao/PlaylistItemDao.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.data.dao

  import androidx.room.Dao
  import androidx.room.Insert
  import androidx.room.OnConflictStrategy
  import androidx.room.Query
  import androidx.room.Upsert
  import org.cog.hymnchtv.notebook.data.entity.PlaylistItemEntity

  /**
   * New items use insert (ABORT): Room's @Upsert falls back to UPDATE-by-PK on *any* unique violation, which would
   * silently drop a new row that collides on (playlistId, position). Upsert is used only for rows whose slot is known free.
   */
  @Dao
  interface PlaylistItemDao {
      @Query(
          "SELECT * FROM playlist_item WHERE playlistId = :playlistId AND deletedAt IS NULL " +
              "ORDER BY position ASC, createdAt ASC",
      )
      suspend fun itemsOf(playlistId: String): List<PlaylistItemEntity>

      /** Highest slot ever used in the playlist (soft-deleted rows keep their slot under the unique index). */
      @Query("SELECT MAX(position) FROM playlist_item WHERE playlistId = :playlistId")
      suspend fun maxPositionIncludingDeleted(playlistId: String): Int?

      @Query("SELECT * FROM playlist_item")
      suspend fun findAllIncludingDeleted(): List<PlaylistItemEntity>

      @Query("SELECT * FROM playlist_item WHERE id = :id")
      suspend fun findById(id: String): PlaylistItemEntity?

      @Insert(onConflict = OnConflictStrategy.ABORT)
      suspend fun insert(row: PlaylistItemEntity)

      @Upsert
      suspend fun upsert(row: PlaylistItemEntity)

      @Upsert
      suspend fun upsertAll(rows: List<PlaylistItemEntity>)
  }
  ```

- [ ] **Step 5：實作 `NotebookDatabase`**

  ```kotlin
  package org.cog.hymnchtv.notebook.data

  import android.content.Context
  import androidx.room.Database
  import androidx.room.Room
  import androidx.room.RoomDatabase
  import androidx.room.TypeConverters
  import org.cog.hymnchtv.notebook.data.dao.FavoriteDao
  import org.cog.hymnchtv.notebook.data.dao.NoteDao
  import org.cog.hymnchtv.notebook.data.dao.PlaylistDao
  import org.cog.hymnchtv.notebook.data.dao.PlaylistItemDao
  import org.cog.hymnchtv.notebook.data.dao.SingLogDao
  import org.cog.hymnchtv.notebook.data.entity.FavoriteEntity
  import org.cog.hymnchtv.notebook.data.entity.NoteEntity
  import org.cog.hymnchtv.notebook.data.entity.PlaylistEntity
  import org.cog.hymnchtv.notebook.data.entity.PlaylistItemEntity
  import org.cog.hymnchtv.notebook.data.entity.SingLogEntity

  /** The notebook database, separate from the legacy DatabaseBackend (dbHymnApp.db). */
  @Database(
      entities = [
          FavoriteEntity::class, SingLogEntity::class, NoteEntity::class,
          PlaylistEntity::class, PlaylistItemEntity::class,
      ],
      version = NotebookDatabase.VERSION,
      exportSchema = true,
  )
  @TypeConverters(NotebookConverters::class)
  abstract class NotebookDatabase : RoomDatabase() {
      abstract fun favoriteDao(): FavoriteDao
      abstract fun singLogDao(): SingLogDao
      abstract fun noteDao(): NoteDao
      abstract fun playlistDao(): PlaylistDao
      abstract fun playlistItemDao(): PlaylistItemDao

      companion object {
          const val VERSION = 1

          /** Referenced by res/xml/notebook_*.xml backup rules (asserted by BackupRulesTest). */
          const val FILE_NAME = "notebook.db"

          /**
           * TRUNCATE keeps the database in a single file so backup rules need no -wal/-shm entries. Backup
           * consistency itself comes from Auto Backup shutting the app down before copying files.
           * [fileName] is overridable only for tests that need a real file (e.g. concurrency tests).
           */
          @JvmStatic
          @JvmOverloads
          fun build(context: Context, fileName: String = FILE_NAME): NotebookDatabase =
              Room.databaseBuilder(context.applicationContext, NotebookDatabase::class.java, fileName)
                  .setJournalMode(JournalMode.TRUNCATE)
                  .build()

          @JvmStatic
          fun inMemory(context: Context): NotebookDatabase =
              Room.inMemoryDatabaseBuilder(context, NotebookDatabase::class.java).build()
      }
  }
  ```

- [ ] **Step 6：編譯並檢查 schema**

  ```bash
  ./gradlew :hymnchtv:assembleDebug :hymnchtv:assembleDebugAndroidTest --console=plain
  S=hymnchtv/schemas/org.cog.hymnchtv.notebook.data.NotebookDatabase/1.json
  for i in index_favorite_hymnType_hymnNo_isFu index_sing_log_hymnType_hymnNo_isFu_sungAt index_sing_log_sungAt \
           index_sing_log_playlistId index_note_hymnType_hymnNo_isFu_createdAt index_note_singLogId index_playlist_item_playlistId_position; do
    printf "%s: " $i; grep -c "\"$i\"" $S
  done
  grep -c '"updatedBy"' $S
  python3 -c "import json;d=json.load(open('$S'));print([ (i['name'],i['unique']) for e in d['database']['entities'] for i in e.get('indices',[]) if e['tableName']=='playlist_item'])"
  ```

  Expected: BUILD SUCCESSFUL；7 個 index 各印出 `1`；`updatedBy` 印出 `5`；最後一行是 `[('index_playlist_item_playlistId_position', True)]`。

- [ ] **Step 7：在 `api34b` 上跑 DAO 測試和編號比對測試**

  ```bash
  export ANDROID_SERIAL=emulator-5580
  test "$(adb emu avd name | head -1 | tr -d '\r')" = api34b && \
  ./gradlew :hymnchtv:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=org.cog.hymnchtv.notebook --console=plain
  ```

  Expected: `NotebookDaoTest` 8 個、`HymnNumberingConsistencyTest` 2 個，全部通過。

- [ ] **Step 8：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/notebook/data hymnchtv/schemas hymnchtv/src/androidTest/java/org/cog/hymnchtv/notebook/data
  git commit -m "feat: add notebook Room database, entities and DAOs (schema v1)" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

### Task 3：Repository 介面、prefs 介面與 JVM 測試替身（Lane 0）

> 已由 Room 統一計畫 Task 6(a) 遷移：本 Task 內的 `NotebookDatabase`／`notebook.db`／`TRUNCATE` 為歷史片段，現況為 `HymnchtvDatabase`（`hymnchtv.db`、WAL）。

**Files:**
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/notebook/repo/Repository.kt`、`FavoriteRepository.kt`、`SingLogRepository.kt`、`NoteRepository.kt`、`PlaylistRepository.kt`
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/notebook/settings/NotebookPrefs.kt`
- Create（測試替身，Lane B、C 和 Task 12 共用）: `hymnchtv/src/test/java/org/cog/hymnchtv/notebook/fakes/TestDoubles.kt`、`InMemorySingLogRepository.kt`
- Test: `hymnchtv/src/test/java/org/cog/hymnchtv/notebook/fakes/InMemorySingLogRepositoryTest.kt`
- Create（共用契約測試，test 和 androidTest 都會編譯）: `hymnchtv/src/sharedTest/java/org/cog/hymnchtv/notebook/contract/SingLogRepositoryContract.kt`
- Test: `hymnchtv/src/test/java/org/cog/hymnchtv/notebook/fakes/InMemorySingLogRepositoryContractTest.kt`（Room 版的子類別在 Task 4）

這個 task 只有介面，沒有邏輯。測試的對象是替身本身，確保 Lane B 用的假 repository 和 Room 實作遵守相同的約定：只回傳未刪除的列、soft delete、時間戳和 `updatedBy` 由 repository 蓋、`recordUnlessDuplicate` 是原子的。

- [ ] **Step 1：寫介面**

  `repo/Repository.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.repo

  import org.cog.hymnchtv.notebook.model.SyncRecord

  /**
   * Common data-access contract. Rows are immutable; every write returns the stored copy.
   * Timestamps and updatedBy are stamped by the repository (Clock + DeviceIdProvider); callers never set them.
   */
  interface Repository<T : SyncRecord> {
      /** Active (not soft-deleted) rows. */
      suspend fun findAll(): List<T>

      /** Active row by id; null when missing or soft-deleted. */
      suspend fun findById(id: String): T?

      /**
       * Stores a new row. A blank id is replaced by a fresh UUID; a non-blank id must be a canonical lowercase UUID.
       * Throws IllegalArgumentException on invalid input.
       */
      suspend fun create(item: T): T

      /** Replaces an active row's content and bumps updatedAt; null when the row is missing or deleted. */
      suspend fun update(item: T): T?

      /** Soft delete (stamps deletedAt and updatedAt); false when missing or already deleted. */
      suspend fun delete(id: String): Boolean
  }
  ```

  `repo/FavoriteRepository.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.repo

  import org.cog.hymnchtv.notebook.data.entity.FavoriteEntity
  import org.cog.hymnchtv.notebook.model.HymnKey

  interface FavoriteRepository : Repository<FavoriteEntity> {
      suspend fun isFavorite(key: HymnKey): Boolean

      /** Idempotent. Returns the stored row, or null when un-favoriting a hymn that was never favorited. */
      suspend fun setFavorite(key: HymnKey, favorite: Boolean): FavoriteEntity?

      /** Flips the state atomically and returns the new state. */
      suspend fun toggle(key: HymnKey): Boolean
  }
  ```

  `repo/SingLogRepository.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.repo

  import org.cog.hymnchtv.notebook.data.SingStats
  import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.cog.hymnchtv.notebook.model.SingSource

  interface SingLogRepository : Repository<SingLogEntity> {
      /** Unconditional insert: manual entries (and D-1c "mark playlist sung") deliberately bypass dedupe. */
      suspend fun record(
          key: HymnKey,
          sungAt: Long,
          occasion: Occasion,
          source: SingSource,
          playlistId: String? = null,
      ): SingLogEntity

      /**
       * Atomic check-and-insert for every automatic path: inserts unless an active log of the same hymn has
       * |sungAt − existing| < windowMillis (DedupeWindow). Returns null for a duplicate. Room runs it in one
       * write transaction, so concurrent callers can never both insert.
       */
      suspend fun recordUnlessDuplicate(
          key: HymnKey,
          sungAt: Long,
          occasion: Occasion,
          source: SingSource,
          windowMillis: Long,
      ): SingLogEntity?

      suspend fun statsFor(key: HymnKey): SingStats

      /** Most recent active log by sungAt. */
      suspend fun latestFor(key: HymnKey): SingLogEntity?

      /** Active logs of one hymn, newest first. */
      suspend fun findByHymn(key: HymnKey): List<SingLogEntity>

      /** Active logs with fromInclusive <= sungAt < toExclusive, newest first. */
      suspend fun findBetween(fromInclusive: Long, toExclusive: Long): List<SingLogEntity>
  }
  ```

  `repo/NoteRepository.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.repo

  import org.cog.hymnchtv.notebook.data.entity.NoteEntity
  import org.cog.hymnchtv.notebook.model.HymnKey

  interface NoteRepository : Repository<NoteEntity> {
      suspend fun add(key: HymnKey, body: String, singLogId: String? = null): NoteEntity

      /** Active notes of one hymn, newest first. */
      suspend fun findByHymn(key: HymnKey): List<NoteEntity>
  }
  ```

  `repo/PlaylistRepository.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.repo

  import org.cog.hymnchtv.notebook.data.entity.PlaylistEntity
  import org.cog.hymnchtv.notebook.data.entity.PlaylistItemEntity
  import org.cog.hymnchtv.notebook.model.HymnKey

  /** delete(playlistId) also soft-deletes the playlist's items. Positions are sparse, only-growing sort keys. */
  interface PlaylistRepository : Repository<PlaylistEntity> {
      suspend fun createPlaylist(name: String): PlaylistEntity

      suspend fun rename(id: String, name: String): PlaylistEntity?

      /** Active items ordered by position. */
      suspend fun items(playlistId: String): List<PlaylistItemEntity>

      /** Appends after the highest slot ever used (in one transaction); null when the playlist is missing or deleted. */
      suspend fun addItem(playlistId: String, key: HymnKey): PlaylistItemEntity?

      suspend fun removeItem(itemId: String): Boolean

      /** orderedItemIds must be a permutation of the active item ids; returns items in the new order. */
      suspend fun reorder(playlistId: String, orderedItemIds: List<String>): List<PlaylistItemEntity>
  }
  ```

  `settings/NotebookPrefs.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.settings

  import org.cog.hymnchtv.notebook.model.DeviceIdProvider
  import org.cog.hymnchtv.notebook.model.Occasion

  /**
   * Notebook preferences. Reads never throw.
   * - notebook.xml ([FILE_NAME]) is included in Auto Backup.
   * - notebook_device.xml ([DEVICE_FILE_NAME]) holds only the device id and is deliberately NOT backed up,
   *   so a restored phone gets a fresh id and two devices never share one.
   */
  interface NotebookPrefs : DeviceIdProvider {
      val autoRecordEnabled: Boolean

      /** Occasion the user last picked by hand; null when never picked. */
      val lastChosenOccasion: Occasion?

      fun setAutoRecordEnabled(enabled: Boolean)

      fun setLastChosenOccasion(occasion: Occasion)

      companion object {
          const val FILE_NAME = "notebook"
          const val DEVICE_FILE_NAME = "notebook_device"
          const val KEY_AUTO_RECORD = "auto_record_enabled"
          const val KEY_LAST_OCCASION = "last_chosen_occasion"
          const val KEY_DEVICE_ID = "device_id"
          const val DEFAULT_AUTO_RECORD = true
      }
  }
  ```

- [ ] **Step 2：寫替身的測試（會失敗）** `fakes/InMemorySingLogRepositoryTest.kt`

  ```kotlin
  package org.cog.hymnchtv.notebook.fakes

  import com.google.common.truth.Truth.assertThat
  import kotlinx.coroutines.Dispatchers
  import kotlinx.coroutines.async
  import kotlinx.coroutines.awaitAll
  import kotlinx.coroutines.test.runTest
  import kotlinx.coroutines.withContext
  import org.cog.hymnchtv.notebook.data.SingStats
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.HymnTypes
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.cog.hymnchtv.notebook.model.SingSource
  import org.junit.Test

  class InMemorySingLogRepositoryTest {
      private val key = HymnKey.of(HymnTypes.DB, 1)
      private val hour = 3_600_000L

      @Test
      fun recordStampsAndSoftDeletes() = runTest {
          val clock = MutableClock(1_000)
          val repo = InMemorySingLogRepository(clock)
          val a = repo.record(key, sungAt = 500, occasion = Occasion.HOME, source = SingSource.AUTO)
          assertThat(a.id).isEqualTo(testUuid(1))
          assertThat(a.createdAt).isEqualTo(1_000L)
          assertThat(a.updatedBy).isEqualTo(TEST_DEVICE)

          clock.now = 2_000
          repo.record(key, sungAt = 900, occasion = Occasion.HOME, source = SingSource.MANUAL)
          assertThat(repo.statsFor(key)).isEqualTo(SingStats(2, 900L))

          assertThat(repo.delete(a.id)).isTrue()
          assertThat(repo.delete(a.id)).isFalse()
          assertThat(repo.findById(a.id)).isNull()
          assertThat(repo.rows.getValue(a.id).deletedAt).isEqualTo(2_000L)
          assertThat(repo.statsFor(key)).isEqualTo(SingStats(1, 900L))
      }

      @Test
      fun recordUnlessDuplicateHonoursTheWindow() = runTest {
          val repo = InMemorySingLogRepository(MutableClock(0))
          assertThat(repo.recordUnlessDuplicate(key, 10 * hour, Occasion.HOME, SingSource.AUTO, 3 * hour)).isNotNull()
          assertThat(repo.recordUnlessDuplicate(key, 13 * hour - 1, Occasion.HOME, SingSource.AUTO, 3 * hour)).isNull()
          assertThat(repo.recordUnlessDuplicate(key, 13 * hour, Occasion.HOME, SingSource.AUTO, 3 * hour)).isNotNull()
      }

      @Test
      fun recordUnlessDuplicateIsAtomicUnderConcurrency() = runTest {
          val repo = InMemorySingLogRepository(MutableClock(0))
          val results = withContext(Dispatchers.Default) {
              (1..20).map { i ->
                  async { repo.recordUnlessDuplicate(key, 10 * hour + i, Occasion.HOME, SingSource.AUTO, 3 * hour) }
              }.awaitAll()
          }
          assertThat(results.count { it != null }).isEqualTo(1)
          assertThat(repo.rows).hasSize(1)
      }

      @Test
      fun failNextThrowsOnce() = runTest {
          val repo = InMemorySingLogRepository(MutableClock(0))
          repo.failNext = true
          assertThat(runCatching { repo.latestFor(key) }.isFailure).isTrue()
          assertThat(repo.latestFor(key)).isNull()
      }
  }
  ```

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.notebook.fakes.*'`
  Expected: 編譯失敗，`Unresolved reference 'MutableClock'`。

- [ ] **Step 3：寫替身**

  `fakes/TestDoubles.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.fakes

  import org.cog.hymnchtv.notebook.model.Clock
  import org.cog.hymnchtv.notebook.model.IdGenerator
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.cog.hymnchtv.notebook.settings.NotebookPrefs
  import java.util.UUID
  import java.util.concurrent.atomic.AtomicInteger

  /** Canonical lowercase UUIDs that are easy to read in failures: testUuid(1) = 00000000-0000-0000-0000-000000000001. */
  fun testUuid(n: Int): String = UUID(0L, n.toLong()).toString()

  const val TEST_DEVICE = "00000000-0000-0000-0000-0000000000aa"

  class MutableClock(@Volatile var now: Long) : Clock {
      override fun nowMillis(): Long = now
  }

  class SequentialIds(private val start: Int = 0) : IdGenerator {
      private val counter = AtomicInteger(start)
      override fun newId(): String = testUuid(counter.incrementAndGet())
  }

  /** Backing fields are private: a public `var autoRecordEnabled` would clash with setAutoRecordEnabled() on the JVM. */
  class FakeNotebookPrefs(
      autoRecord: Boolean = NotebookPrefs.DEFAULT_AUTO_RECORD,
      lastChosen: Occasion? = null,
      private val device: String = TEST_DEVICE,
  ) : NotebookPrefs {
      @Volatile
      private var autoRecord: Boolean = autoRecord

      @Volatile
      private var lastChosen: Occasion? = lastChosen

      override val autoRecordEnabled: Boolean
          get() = autoRecord

      override val lastChosenOccasion: Occasion?
          get() = lastChosen

      override fun setAutoRecordEnabled(enabled: Boolean) {
          autoRecord = enabled
      }

      override fun setLastChosenOccasion(occasion: Occasion) {
          lastChosen = occasion
      }

      override fun deviceId(): String = device
  }
  ```

  `fakes/InMemorySingLogRepository.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.fakes

  import kotlinx.coroutines.sync.Mutex
  import kotlinx.coroutines.sync.withLock
  import org.cog.hymnchtv.notebook.data.SingStats
  import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
  import org.cog.hymnchtv.notebook.model.Clock
  import org.cog.hymnchtv.notebook.model.DedupeWindow
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.IdGenerator
  import org.cog.hymnchtv.notebook.model.NotebookValidation
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.cog.hymnchtv.notebook.model.SingSource
  import org.cog.hymnchtv.notebook.model.isActive
  import org.cog.hymnchtv.notebook.repo.SingLogRepository

  /** Same contract as RoomSingLogRepository; the map is replaced, never mutated; writes are serialized by a Mutex. */
  class InMemorySingLogRepository(
      private val clock: Clock,
      private val ids: IdGenerator = SequentialIds(),
      private val device: String = TEST_DEVICE,
  ) : SingLogRepository {
      private val writeLock = Mutex()

      @Volatile
      var rows: Map<String, SingLogEntity> = emptyMap()
          private set

      /** When true, the next latestFor/create/recordUnlessDuplicate throws IllegalStateException once. */
      @Volatile
      var failNext: Boolean = false

      private fun maybeFail() {
          if (failNext) {
              failNext = false
              throw IllegalStateException("simulated failure")
          }
      }

      private fun active(key: HymnKey) = rows.values.filter { it.isActive && it.hymn == key }

      private fun insertUnlocked(item: SingLogEntity): SingLogEntity {
          val now = clock.nowMillis()
          val row = item.copy(
              id = item.id.ifBlank { ids.newId() }, sungAt = NotebookValidation.sungAt(item.sungAt, now),
              createdAt = now, updatedAt = now, deletedAt = null, updatedBy = device,
          )
          rows = rows + (row.id to row)
          return row
      }

      override suspend fun findAll() = rows.values.filter { it.isActive }.sortedByDescending { it.sungAt }

      override suspend fun findById(id: String) = rows[id]?.takeIf { it.isActive }

      override suspend fun create(item: SingLogEntity): SingLogEntity = writeLock.withLock {
          maybeFail()
          insertUnlocked(item)
      }

      override suspend fun update(item: SingLogEntity): SingLogEntity? = writeLock.withLock {
          val existing = rows[item.id]?.takeIf { it.isActive } ?: return@withLock null
          val now = clock.nowMillis()
          val row = item.copy(
              sungAt = NotebookValidation.sungAt(item.sungAt, now),
              createdAt = existing.createdAt, updatedAt = now, deletedAt = null, updatedBy = device,
          )
          rows = rows + (row.id to row)
          row
      }

      override suspend fun delete(id: String): Boolean = writeLock.withLock {
          val existing = rows[id]?.takeIf { it.isActive } ?: return@withLock false
          val now = clock.nowMillis()
          rows = rows + (id to existing.copy(updatedAt = now, deletedAt = now, updatedBy = device))
          true
      }

      override suspend fun record(
          key: HymnKey,
          sungAt: Long,
          occasion: Occasion,
          source: SingSource,
          playlistId: String?,
      ): SingLogEntity = create(newLog(key, sungAt, occasion, source, playlistId))

      override suspend fun recordUnlessDuplicate(
          key: HymnKey,
          sungAt: Long,
          occasion: Occasion,
          source: SingSource,
          windowMillis: Long,
      ): SingLogEntity? {
          // Same order as RoomSingLogRepository: validate first, so an illegal time never comes back as "duplicate".
          require(windowMillis >= 0) { "windowMillis must be >= 0" }
          NotebookValidation.sungAt(sungAt, clock.nowMillis())
          return writeLock.withLock {
              maybeFail()
              if (active(key).any { DedupeWindow.contains(it.sungAt, sungAt, windowMillis) }) {
                  null
              } else {
                  insertUnlocked(newLog(key, sungAt, occasion, source, null))
              }
          }
      }

      override suspend fun statsFor(key: HymnKey): SingStats {
          val logs = active(key)
          return SingStats(logs.size, logs.maxOfOrNull { it.sungAt })
      }

      override suspend fun latestFor(key: HymnKey): SingLogEntity? {
          maybeFail()
          return active(key).maxByOrNull { it.sungAt }
      }

      override suspend fun findByHymn(key: HymnKey) = active(key).sortedByDescending { it.sungAt }

      override suspend fun findBetween(fromInclusive: Long, toExclusive: Long): List<SingLogEntity> {
          NotebookValidation.timeRange(fromInclusive, toExclusive)
          return findAll().filter { it.sungAt in fromInclusive until toExclusive }
      }

      private fun newLog(key: HymnKey, sungAt: Long, occasion: Occasion, source: SingSource, playlistId: String?) =
          SingLogEntity(
              id = "", hymn = key, sungAt = sungAt, occasion = occasion, source = source,
              playlistId = playlistId, createdAt = 0, updatedAt = 0,
          )
  }
  ```

- [ ] **Step 4：寫共用契約測試，對假實作執行**

  契約描述每一個 `SingLogRepository` 都必須遵守的行為：驗證順序、去重邊界、soft delete、時間戳。
  - 這個 task 裡，契約對 `InMemorySingLogRepository` 在 JVM 上執行。
  - Task 4 會再加一個子類別，讓同一組測試在裝置上對 `RoomSingLogRepository` 執行。
  - 兩者有任何行為差異，都會在其中一邊失敗。

  `src/sharedTest/java/org/cog/hymnchtv/notebook/contract/SingLogRepositoryContract.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.contract

  import com.google.common.truth.Truth.assertThat
  import kotlinx.coroutines.runBlocking
  import org.cog.hymnchtv.notebook.data.SingStats
  import org.cog.hymnchtv.notebook.model.Clock
  import org.cog.hymnchtv.notebook.model.DeviceIdProvider
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.HymnTypes
  import org.cog.hymnchtv.notebook.model.NotebookValidation
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.cog.hymnchtv.notebook.model.SingSource
  import org.cog.hymnchtv.notebook.repo.SingLogRepository
  import org.junit.After
  import org.junit.Test

  class ContractClock(@Volatile var now: Long) : Clock {
      override fun nowMillis(): Long = now
  }

  /**
   * Behaviour every SingLogRepository must share. Subclasses: InMemorySingLogRepositoryContractTest (JVM) and
   * RoomSingLogRepositoryContractTest (androidTest). Add a case here whenever the fake and Room could diverge.
   */
  abstract class SingLogRepositoryContract {
      protected val clock = ContractClock(NOW)
      private val key = HymnKey.of(HymnTypes.DB, 1)
      private val repo by lazy { newRepository(clock, DeviceIdProvider { DEVICE }) }

      protected abstract fun newRepository(clock: Clock, device: DeviceIdProvider): SingLogRepository

      protected open fun tearDownRepository() {}

      @After
      fun closeRepository() = tearDownRepository()

      private fun rejects(block: suspend () -> Unit) = runBlocking {
          assertThat(runCatching { block() }.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
      }

      @Test
      fun recordStampsIdTimesAndDevice(): Unit = runBlocking {
          val log = repo.record(key, NOW - 10, Occasion.HOME, SingSource.MANUAL)
          assertThat(NotebookValidation.uuid(log.id)).isEqualTo(log.id)
          assertThat(log.createdAt).isEqualTo(NOW)
          assertThat(log.updatedAt).isEqualTo(NOW)
          assertThat(log.deletedAt).isNull()
          assertThat(log.updatedBy).isEqualTo(DEVICE)
          assertThat(repo.findById(log.id)).isEqualTo(log)
      }

      @Test
      fun dedupeWindowIsExclusiveOnBothSides(): Unit = runBlocking {
          repo.record(key, NOW, Occasion.HOME, SingSource.MANUAL)
          assertThat(repo.recordUnlessDuplicate(key, NOW - WINDOW + 1, Occasion.HOME, SingSource.AUTO, WINDOW)).isNull()
          assertThat(repo.recordUnlessDuplicate(key, NOW + WINDOW - 1, Occasion.HOME, SingSource.AUTO, WINDOW)).isNull()
          assertThat(repo.recordUnlessDuplicate(key, NOW - WINDOW, Occasion.HOME, SingSource.AUTO, WINDOW)).isNotNull()
          assertThat(repo.statsFor(key).singCount).isEqualTo(2)
      }

      @Test
      fun illegalInputIsRejectedEvenWhenItWouldBeADuplicate(): Unit = runBlocking {
          repo.record(key, NOW + DAY, Occasion.HOME, SingSource.MANUAL)
          rejects { repo.recordUnlessDuplicate(key, NOW + DAY + 1, Occasion.HOME, SingSource.AUTO, WINDOW) }
          rejects { repo.recordUnlessDuplicate(key, -1, Occasion.HOME, SingSource.AUTO, WINDOW) }
          rejects { repo.recordUnlessDuplicate(key, Long.MAX_VALUE, Occasion.HOME, SingSource.AUTO, WINDOW) }
          rejects { repo.recordUnlessDuplicate(key, NOW + DAY, Occasion.HOME, SingSource.AUTO, -1) }
          rejects { repo.record(key, -1, Occasion.HOME, SingSource.MANUAL) }
          assertThat(repo.statsFor(key).singCount).isEqualTo(1)
      }

      @Test
      fun updateValidatesTheTimeAndKeepsCreatedAt(): Unit = runBlocking {
          val log = repo.record(key, NOW, Occasion.HOME, SingSource.AUTO)
          clock.now = NOW + 1_000
          rejects { repo.update(log.copy(sungAt = -1)) }
          rejects { repo.update(log.copy(sungAt = NOW + 1_000 + DAY + 1)) }
          val updated = repo.update(log.copy(occasion = Occasion.SMALL_GROUP))
          assertThat(updated).isEqualTo(log.copy(occasion = Occasion.SMALL_GROUP, updatedAt = NOW + 1_000))
      }

      @Test
      fun softDeleteHidesTheRowAndNoLongerBlocksAutoRecord(): Unit = runBlocking {
          val log = repo.record(key, NOW, Occasion.HOME, SingSource.AUTO)
          assertThat(repo.delete(log.id)).isTrue()
          assertThat(repo.delete(log.id)).isFalse()
          assertThat(repo.findById(log.id)).isNull()
          assertThat(repo.update(log)).isNull()
          assertThat(repo.statsFor(key)).isEqualTo(SingStats(0, null))
          assertThat(repo.recordUnlessDuplicate(key, NOW, Occasion.HOME, SingSource.AUTO, WINDOW)).isNotNull()
      }

      @Test
      fun queriesReturnActiveRowsNewestFirst(): Unit = runBlocking {
          repo.record(key, NOW - 300, Occasion.HOME, SingSource.AUTO)
          repo.record(key, NOW - 100, Occasion.HOME, SingSource.AUTO)
          repo.record(HymnKey.of(HymnTypes.ER, 1), NOW - 200, Occasion.HOME, SingSource.AUTO)
          assertThat(repo.statsFor(key)).isEqualTo(SingStats(2, NOW - 100))
          assertThat(repo.findByHymn(key).map { it.sungAt }).containsExactly(NOW - 100, NOW - 300).inOrder()
          assertThat(repo.latestFor(key)?.sungAt).isEqualTo(NOW - 100)
          assertThat(repo.findBetween(NOW - 300, NOW - 100).map { it.sungAt }).containsExactly(NOW - 200, NOW - 300).inOrder()
          rejects { repo.findBetween(2, 1) }
      }

      companion object {
          const val NOW = 1_790_733_600_000L
          const val DAY = NotebookValidation.MAX_FUTURE_SKEW_MILLIS
          const val WINDOW = 3 * 3_600_000L
          const val DEVICE = "00000000-0000-0000-0000-0000000000cc"
      }
  }
  ```

  `src/test/java/org/cog/hymnchtv/notebook/fakes/InMemorySingLogRepositoryContractTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.fakes

  import org.cog.hymnchtv.notebook.contract.SingLogRepositoryContract
  import org.cog.hymnchtv.notebook.model.Clock
  import org.cog.hymnchtv.notebook.model.DeviceIdProvider
  import org.cog.hymnchtv.notebook.repo.SingLogRepository

  class InMemorySingLogRepositoryContractTest : SingLogRepositoryContract() {
      override fun newRepository(clock: Clock, device: DeviceIdProvider): SingLogRepository =
          InMemorySingLogRepository(clock, device = device.deviceId())
  }
  ```

- [ ] **Step 5：執行測試，確認它通過**

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.notebook.fakes.*' && ./gradlew :hymnchtv:assembleDebug :hymnchtv:assembleDebugAndroidTest`
  Expected: `InMemorySingLogRepositoryTest` 4 個＋`InMemorySingLogRepositoryContractTest` 6 個，全部通過；BUILD SUCCESSFUL（契約也會被編進 androidTest）。

- [ ] **Step 6：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/notebook/repo hymnchtv/src/main/java/org/cog/hymnchtv/notebook/settings hymnchtv/src/test/java/org/cog/hymnchtv/notebook/fakes hymnchtv/src/sharedTest
  git commit -m "feat: add notebook repository and prefs interfaces with JVM test doubles" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

  **到這裡 Lane 0 暫停。** 協調者從 `feat/d1a-notebook-data` 的這個 commit 開出 Lane A、B、C 三個 worktree。

---

### Task 4：Room repository 實作（Lane A）

> 已由 Room 統一計畫 Task 6(a) 遷移：本 Task 內的 `NotebookDatabase`／`notebook.db`／`TRUNCATE` 為歷史片段，現況為 `HymnchtvDatabase`（`hymnchtv.db`、WAL）。

**前置條件：** Task 3 已 commit。在 worktree `feat/d1a-notebook-data-lane-a` 中進行。這條 lane 獨占 `api34b`（`ANDROID_SERIAL=emulator-5580`）。

**Files:**
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/notebook/repo/room/RoomIds.kt`、`RoomFavoriteRepository.kt`、`RoomSingLogRepository.kt`、`RoomNoteRepository.kt`、`RoomPlaylistRepository.kt`
- Test: `hymnchtv/src/androidTest/java/org/cog/hymnchtv/notebook/repo/AndroidTestDoubles.kt`、`RoomFavoriteRepositoryTest.kt`、`RoomSingLogRepositoryTest.kt`、`RoomSingLogRepositoryContractTest.kt`、`RoomSingLogConcurrencyTest.kt`、`RoomNoteRepositoryTest.kt`、`RoomPlaylistRepositoryTest.kt`

- [ ] **Step 1：寫會失敗的 instrumented test**

  `AndroidTestDoubles.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.repo

  import org.cog.hymnchtv.notebook.model.Clock
  import org.cog.hymnchtv.notebook.model.DeviceIdProvider
  import org.cog.hymnchtv.notebook.model.IdGenerator
  import java.util.UUID
  import java.util.concurrent.atomic.AtomicInteger

  fun androidTestUuid(n: Int): String = UUID(0L, n.toLong()).toString()

  const val ANDROID_TEST_DEVICE = "00000000-0000-0000-0000-0000000000bb"

  val testDevice = DeviceIdProvider { ANDROID_TEST_DEVICE }

  class TestClock(@Volatile var now: Long) : Clock {
      override fun nowMillis(): Long = now
  }

  class TestIds : IdGenerator {
      private val counter = AtomicInteger()
      override fun newId(): String = androidTestUuid(counter.incrementAndGet())
  }
  ```

  `RoomFavoriteRepositoryTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.repo

  import androidx.test.core.app.ApplicationProvider
  import androidx.test.ext.junit.runners.AndroidJUnit4
  import com.google.common.truth.Truth.assertThat
  import kotlinx.coroutines.runBlocking
  import org.cog.hymnchtv.notebook.data.NotebookDatabase
  import org.cog.hymnchtv.notebook.data.entity.FavoriteEntity
  import org.cog.hymnchtv.notebook.model.FavoriteIds
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.HymnTypes
  import org.cog.hymnchtv.notebook.repo.room.RoomFavoriteRepository
  import org.junit.After
  import org.junit.Before
  import org.junit.Test
  import org.junit.runner.RunWith

  @RunWith(AndroidJUnit4::class)
  class RoomFavoriteRepositoryTest {
      private lateinit var db: NotebookDatabase
      private lateinit var repo: RoomFavoriteRepository
      private val clock = TestClock(1_000)
      private val key = HymnKey.of(HymnTypes.DB, 1)

      @Before
      fun setUp() {
          db = NotebookDatabase.inMemory(ApplicationProvider.getApplicationContext())
          repo = RoomFavoriteRepository(db, clock, testDevice)
      }

      @After
      fun tearDown() = db.close()

      @Test
      fun toggleFlipsStateAndKeepsOneSoftDeletedRow(): Unit = runBlocking {
          assertThat(repo.toggle(key)).isTrue()
          assertThat(repo.isFavorite(key)).isTrue()

          clock.now = 2_000
          assertThat(repo.toggle(key)).isFalse()
          assertThat(repo.isFavorite(key)).isFalse()
          assertThat(repo.findAll()).isEmpty()
          val stored = db.favoriteDao().findAllIncludingDeleted().single()
          assertThat(stored.id).isEqualTo(FavoriteIds.forKey(key))
          assertThat(stored.createdAt).isEqualTo(1_000L)
          assertThat(stored.updatedAt).isEqualTo(2_000L)
          assertThat(stored.deletedAt).isEqualTo(2_000L)
          assertThat(stored.updatedBy).isEqualTo(ANDROID_TEST_DEVICE)

          clock.now = 3_000
          assertThat(repo.toggle(key)).isTrue()
          val revived = db.favoriteDao().findAllIncludingDeleted().single()
          assertThat(revived.deletedAt).isNull()
          assertThat(revived.createdAt).isEqualTo(1_000L)
          assertThat(revived.updatedAt).isEqualTo(3_000L)
      }

      @Test
      fun setFavoriteIsIdempotent(): Unit = runBlocking {
          val first = repo.setFavorite(key, true)
          clock.now = 5_000
          assertThat(repo.setFavorite(key, true)).isEqualTo(first)
          assertThat(repo.setFavorite(HymnKey.of(HymnTypes.BB, 3), false)).isNull()
          assertThat(db.favoriteDao().findAllIncludingDeleted()).hasSize(1)
      }

      @Test
      fun createIgnoresTheGivenIdAndDeleteIsSoft(): Unit = runBlocking {
          val created = repo.create(FavoriteEntity(id = androidTestUuid(99), hymn = key, createdAt = 0, updatedAt = 0))
          assertThat(created.id).isEqualTo(FavoriteIds.forKey(key))
          assertThat(repo.findById(created.id)).isEqualTo(created)
          assertThat(repo.delete(created.id)).isTrue()
          assertThat(repo.delete(created.id)).isFalse()
          assertThat(repo.findById(created.id)).isNull()
          assertThat(repo.delete(androidTestUuid(1))).isFalse()
      }
  }
  ```

  `RoomSingLogRepositoryTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.repo

  import androidx.test.core.app.ApplicationProvider
  import androidx.test.ext.junit.runners.AndroidJUnit4
  import com.google.common.truth.Truth.assertThat
  import kotlinx.coroutines.runBlocking
  import org.cog.hymnchtv.notebook.data.NotebookDatabase
  import org.cog.hymnchtv.notebook.data.SingStats
  import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.HymnTypes
  import org.cog.hymnchtv.notebook.model.NotebookValidation
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.cog.hymnchtv.notebook.model.SingSource
  import org.cog.hymnchtv.notebook.repo.room.RoomSingLogRepository
  import org.junit.After
  import org.junit.Before
  import org.junit.Test
  import org.junit.runner.RunWith

  @RunWith(AndroidJUnit4::class)
  class RoomSingLogRepositoryTest {
      private lateinit var db: NotebookDatabase
      private lateinit var repo: RoomSingLogRepository
      private val clock = TestClock(1_000)
      private val key = HymnKey.of(HymnTypes.DB, 1)
      private val hour = 3_600_000L

      @Before
      fun setUp() {
          db = NotebookDatabase.inMemory(ApplicationProvider.getApplicationContext())
          repo = RoomSingLogRepository(db, clock, TestIds(), testDevice)
      }

      @After
      fun tearDown() = db.close()

      @Test
      fun recordStampsAndFeedsStats(): Unit = runBlocking {
          val a = repo.record(key, sungAt = 100, occasion = Occasion.LORDS_DAY, source = SingSource.MANUAL)
          assertThat(a.id).isEqualTo(androidTestUuid(1))
          assertThat(a.createdAt).isEqualTo(1_000L)
          assertThat(a.updatedBy).isEqualTo(ANDROID_TEST_DEVICE)
          repo.record(key, 300, Occasion.HOME, SingSource.AUTO, playlistId = androidTestUuid(50))

          assertThat(repo.statsFor(key)).isEqualTo(SingStats(2, 300L))
          assertThat(repo.latestFor(key)?.sungAt).isEqualTo(300L)
          assertThat(repo.findByHymn(key).map { it.sungAt }).containsExactly(300L, 100L).inOrder()
      }

      @Test
      fun manualRecordBypassesDedupeButAutoDoesNot(): Unit = runBlocking {
          repo.record(key, 10 * hour, Occasion.HOME, SingSource.MANUAL)
          assertThat(repo.record(key, 10 * hour + 1, Occasion.HOME, SingSource.MANUAL)).isNotNull()
          assertThat(repo.recordUnlessDuplicate(key, 12 * hour, Occasion.HOME, SingSource.AUTO, 3 * hour)).isNull()
          assertThat(repo.recordUnlessDuplicate(key, 13 * hour + 1, Occasion.HOME, SingSource.AUTO, 3 * hour)).isNotNull()
          assertThat(repo.statsFor(key).singCount).isEqualTo(3)
      }

      @Test
      fun deletedLogDoesNotBlockAutoRecord(): Unit = runBlocking {
          val a = repo.record(key, 10 * hour, Occasion.HOME, SingSource.AUTO)
          repo.delete(a.id)
          assertThat(repo.recordUnlessDuplicate(key, 10 * hour, Occasion.HOME, SingSource.AUTO, 3 * hour)).isNotNull()
      }

      @Test
      fun createRejectsNonUuidIds(): Unit = runBlocking {
          val bad = SingLogEntity(
              id = "log-1", hymn = key, sungAt = 1, occasion = Occasion.HOME, source = SingSource.AUTO,
              createdAt = 0, updatedAt = 0,
          )
          assertThat(runCatching { repo.create(bad) }.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
          assertThat(repo.create(bad.copy(id = androidTestUuid(7))).id).isEqualTo(androidTestUuid(7))
      }

      @Test
      fun updateKeepsCreatedAtAndBumpsUpdatedAt(): Unit = runBlocking {
          val a = repo.record(key, 100, Occasion.HOME, SingSource.AUTO)
          clock.now = 2_000
          val updated = repo.update(a.copy(occasion = Occasion.SMALL_GROUP, createdAt = 0, updatedAt = 0))
          assertThat(updated).isEqualTo(a.copy(occasion = Occasion.SMALL_GROUP, updatedAt = 2_000))
          assertThat(repo.findById(a.id)).isEqualTo(updated)
      }

      @Test
      fun deletedLogsAreHiddenAndNotUpdatable(): Unit = runBlocking {
          val a = repo.record(key, 100, Occasion.HOME, SingSource.AUTO)
          clock.now = 2_000
          assertThat(repo.delete(a.id)).isTrue()
          assertThat(repo.findById(a.id)).isNull()
          assertThat(repo.update(a)).isNull()
          assertThat(repo.statsFor(key)).isEqualTo(SingStats(0, null))
          assertThat(db.singLogDao().findById(a.id)?.deletedAt).isEqualTo(2_000L)
      }

      @Test
      fun findBetweenValidatesRange(): Unit = runBlocking {
          repo.record(key, 100, Occasion.HOME, SingSource.AUTO)
          repo.record(key, 200, Occasion.HOME, SingSource.AUTO)
          assertThat(repo.findBetween(100, 200).map { it.sungAt }).containsExactly(100L)
          assertThat(runCatching { repo.findBetween(5, 1) }.exceptionOrNull())
              .isInstanceOf(IllegalArgumentException::class.java)
      }

      @Test
      fun sungAtOutsideTheAllowedRangeIsRejected(): Unit = runBlocking {
          val day = NotebookValidation.MAX_FUTURE_SKEW_MILLIS
          listOf(-1L, 1_000L + day + 1, Long.MAX_VALUE).forEach { bad ->
              assertThat(runCatching { repo.record(key, bad, Occasion.HOME, SingSource.MANUAL) }.exceptionOrNull())
                  .isInstanceOf(IllegalArgumentException::class.java)
              assertThat(runCatching { repo.recordUnlessDuplicate(key, bad, Occasion.HOME, SingSource.AUTO, 3 * hour) }.exceptionOrNull())
                  .isInstanceOf(IllegalArgumentException::class.java)
          }
          assertThat(repo.findAll()).isEmpty()
      }
  }
  ```

  `RoomSingLogRepositoryContractTest.kt`（Task 3 的共用契約，在裝置上對 Room 實作執行）：

  ```kotlin
  package org.cog.hymnchtv.notebook.repo

  import androidx.test.core.app.ApplicationProvider
  import androidx.test.ext.junit.runners.AndroidJUnit4
  import org.cog.hymnchtv.notebook.contract.SingLogRepositoryContract
  import org.cog.hymnchtv.notebook.data.NotebookDatabase
  import org.cog.hymnchtv.notebook.model.Clock
  import org.cog.hymnchtv.notebook.model.DeviceIdProvider
  import org.cog.hymnchtv.notebook.model.IdGenerator
  import org.cog.hymnchtv.notebook.repo.room.RoomSingLogRepository
  import org.junit.runner.RunWith

  @RunWith(AndroidJUnit4::class)
  class RoomSingLogRepositoryContractTest : SingLogRepositoryContract() {
      private var db: NotebookDatabase? = null

      override fun newRepository(clock: Clock, device: DeviceIdProvider): SingLogRepository {
          val database = NotebookDatabase.inMemory(ApplicationProvider.getApplicationContext())
          db = database
          return RoomSingLogRepository(database, clock, IdGenerator.RANDOM_UUID, device)
      }

      override fun tearDownRepository() {
          db?.close()
      }
  }
  ```

  `RoomSingLogConcurrencyTest.kt`（用真的檔案資料庫，模擬兩個觸發同時發生）：

  ```kotlin
  package org.cog.hymnchtv.notebook.repo

  import android.content.Context
  import androidx.test.core.app.ApplicationProvider
  import androidx.test.ext.junit.runners.AndroidJUnit4
  import com.google.common.truth.Truth.assertThat
  import kotlinx.coroutines.Dispatchers
  import kotlinx.coroutines.async
  import kotlinx.coroutines.awaitAll
  import kotlinx.coroutines.runBlocking
  import org.cog.hymnchtv.notebook.data.NotebookDatabase
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.HymnTypes
  import org.cog.hymnchtv.notebook.model.IdGenerator
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.cog.hymnchtv.notebook.model.SingSource
  import org.cog.hymnchtv.notebook.repo.room.RoomSingLogRepository
  import org.junit.After
  import org.junit.Before
  import org.junit.Test
  import org.junit.runner.RunWith

  @RunWith(AndroidJUnit4::class)
  class RoomSingLogConcurrencyTest {
      private val context: Context = ApplicationProvider.getApplicationContext()
      private lateinit var db: NotebookDatabase
      private val key = HymnKey.of(HymnTypes.DB, 1)
      private val hour = 3_600_000L

      @Before
      fun setUp() {
          context.deleteDatabase(FILE)
          db = NotebookDatabase.build(context, FILE)
      }

      @After
      fun tearDown() {
          db.close()
          context.deleteDatabase(FILE)
      }

      @Test
      fun concurrentAutoRecordsOfTheSameHymnKeepExactlyOne(): Unit = runBlocking {
          val repo = RoomSingLogRepository(db, TestClock(0), IdGenerator.RANDOM_UUID, testDevice)
          val results = (1..20).map { i ->
              async(Dispatchers.Default) {
                  repo.recordUnlessDuplicate(key, 10 * hour + i, Occasion.HOME, SingSource.AUTO, 3 * hour)
              }
          }.awaitAll()
          assertThat(results.count { it != null }).isEqualTo(1)
          assertThat(db.singLogDao().findAllIncludingDeleted()).hasSize(1)
      }

      private companion object {
          const val FILE = "notebook-concurrency-test.db"
      }
  }
  ```

  `RoomNoteRepositoryTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.repo

  import androidx.test.core.app.ApplicationProvider
  import androidx.test.ext.junit.runners.AndroidJUnit4
  import com.google.common.truth.Truth.assertThat
  import kotlinx.coroutines.runBlocking
  import org.cog.hymnchtv.notebook.data.NotebookDatabase
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.HymnTypes
  import org.cog.hymnchtv.notebook.repo.room.RoomNoteRepository
  import org.junit.After
  import org.junit.Before
  import org.junit.Test
  import org.junit.runner.RunWith

  @RunWith(AndroidJUnit4::class)
  class RoomNoteRepositoryTest {
      private lateinit var db: NotebookDatabase
      private lateinit var repo: RoomNoteRepository
      private val clock = TestClock(1_000)
      private val key = HymnKey.of(HymnTypes.DB, 1)

      @Before
      fun setUp() {
          db = NotebookDatabase.inMemory(ApplicationProvider.getApplicationContext())
          repo = RoomNoteRepository(db, clock, TestIds(), testDevice)
      }

      @After
      fun tearDown() = db.close()

      private fun rejects(block: suspend () -> Unit) = runBlocking {
          assertThat(runCatching { block() }.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
      }

      @Test
      fun addValidatesAndListsNewestFirst(): Unit = runBlocking {
          repo.add(key, "第一則")
          clock.now = 2_000
          val second = repo.add(key, "第二則", singLogId = androidTestUuid(9))
          assertThat(second.singLogId).isEqualTo(androidTestUuid(9))
          assertThat(repo.findByHymn(key).map { it.body }).containsExactly("第二則", "第一則").inOrder()
          rejects { repo.add(key, "  ") }
          rejects { repo.add(key, "ok", singLogId = "not-a-uuid") }
      }

      @Test
      fun updateValidatesAndDeleteIsSoft(): Unit = runBlocking {
          val note = repo.add(key, "原文")
          clock.now = 2_000
          val updated = repo.update(note.copy(body = "改過"))
          assertThat(updated?.body).isEqualTo("改過")
          assertThat(updated?.createdAt).isEqualTo(1_000L)
          assertThat(updated?.updatedAt).isEqualTo(2_000L)
          rejects { repo.update(note.copy(body = "")) }
          assertThat(repo.delete(note.id)).isTrue()
          assertThat(repo.findByHymn(key)).isEmpty()
      }
  }
  ```

  `RoomPlaylistRepositoryTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.repo

  import androidx.test.core.app.ApplicationProvider
  import androidx.test.ext.junit.runners.AndroidJUnit4
  import com.google.common.truth.Truth.assertThat
  import kotlinx.coroutines.runBlocking
  import org.cog.hymnchtv.notebook.data.NotebookDatabase
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.HymnTypes
  import org.cog.hymnchtv.notebook.repo.room.RoomPlaylistRepository
  import org.junit.After
  import org.junit.Before
  import org.junit.Test
  import org.junit.runner.RunWith

  @RunWith(AndroidJUnit4::class)
  class RoomPlaylistRepositoryTest {
      private lateinit var db: NotebookDatabase
      private lateinit var repo: RoomPlaylistRepository
      private val clock = TestClock(1_000)
      private val k1 = HymnKey.of(HymnTypes.DB, 1)
      private val k2 = HymnKey.of(HymnTypes.DB, 781)
      private val k3 = HymnKey.of(HymnTypes.ER, 5)

      @Before
      fun setUp() {
          db = NotebookDatabase.inMemory(ApplicationProvider.getApplicationContext())
          repo = RoomPlaylistRepository(db, clock, TestIds(), testDevice)
      }

      @After
      fun tearDown() = db.close()

      private fun rejects(block: suspend () -> Unit) = runBlocking {
          assertThat(runCatching { block() }.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
      }

      @Test
      fun itemsAppendRemoveAndReorderWithSparsePositions(): Unit = runBlocking {
          val pl = repo.createPlaylist("  主日 10/4 ")
          assertThat(pl.name).isEqualTo("主日 10/4")
          val a = checkNotNull(repo.addItem(pl.id, k1))
          val b = checkNotNull(repo.addItem(pl.id, k2))
          val c = checkNotNull(repo.addItem(pl.id, k3))
          assertThat(listOf(a, b, c).map { it.position }).containsExactly(0, 1, 2).inOrder()

          assertThat(repo.removeItem(b.id)).isTrue()
          assertThat(repo.removeItem(b.id)).isFalse()
          assertThat(repo.items(pl.id).map { it.id }).containsExactly(a.id, c.id).inOrder()

          clock.now = 2_000
          val reordered = repo.reorder(pl.id, listOf(c.id, a.id))
          assertThat(reordered.map { it.id to it.position }).containsExactly(c.id to 3, a.id to 4).inOrder()
          assertThat(reordered.map { it.updatedAt }).containsExactly(2_000L, 2_000L)
          assertThat(repo.items(pl.id).map { it.id }).containsExactly(c.id, a.id).inOrder()
          assertThat(checkNotNull(repo.addItem(pl.id, k2)).position).isEqualTo(5)
      }

      @Test
      fun reorderInTheCurrentOrderChangesNothing(): Unit = runBlocking {
          val pl = repo.createPlaylist("歌單")
          val a = checkNotNull(repo.addItem(pl.id, k1))
          val b = checkNotNull(repo.addItem(pl.id, k2))
          assertThat(repo.reorder(pl.id, listOf(a.id, b.id))).containsExactly(a, b).inOrder()
      }

      @Test
      fun reorderRejectsNonPermutation(): Unit = runBlocking {
          val pl = repo.createPlaylist("歌單")
          val a = checkNotNull(repo.addItem(pl.id, k1))
          checkNotNull(repo.addItem(pl.id, k2))
          rejects { repo.reorder(pl.id, listOf(a.id)) }
          rejects { repo.reorder(pl.id, listOf(a.id, androidTestUuid(404))) }
      }

      @Test
      fun deletingPlaylistSoftDeletesItems(): Unit = runBlocking {
          val pl = repo.createPlaylist("歌單")
          val a = checkNotNull(repo.addItem(pl.id, k1))
          clock.now = 2_000
          assertThat(repo.delete(pl.id)).isTrue()
          assertThat(repo.findById(pl.id)).isNull()
          assertThat(repo.items(pl.id)).isEmpty()
          assertThat(repo.addItem(pl.id, k2)).isNull()
          assertThat(db.playlistItemDao().findById(a.id)?.deletedAt).isEqualTo(2_000L)
      }

      @Test
      fun renameAndNameValidation(): Unit = runBlocking {
          val pl = repo.createPlaylist("舊名")
          clock.now = 2_000
          val renamed = repo.rename(pl.id, " 新名 ")
          assertThat(renamed?.name).isEqualTo("新名")
          assertThat(renamed?.updatedAt).isEqualTo(2_000L)
          assertThat(repo.rename(androidTestUuid(404), "x")).isNull()
          rejects { repo.rename(pl.id, " ") }
          rejects { repo.createPlaylist("") }
      }
  }
  ```

  Run: `./gradlew :hymnchtv:assembleDebugAndroidTest`
  Expected: 編譯失敗，`Unresolved reference 'RoomFavoriteRepository'`。

- [ ] **Step 2：實作**

  `repo/room/RoomIds.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.repo.room

  import org.cog.hymnchtv.notebook.model.IdGenerator
  import org.cog.hymnchtv.notebook.model.NotebookValidation

  /** A blank id means "new row": generate one. Any other id must be a canonical lowercase UUID. */
  internal fun resolveId(id: String, ids: IdGenerator): String = NotebookValidation.uuid(id.ifBlank { ids.newId() })

  internal fun optionalUuid(id: String?): String? = id?.let(NotebookValidation::uuid)
  ```

  `repo/room/RoomFavoriteRepository.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.repo.room

  import androidx.room.withTransaction
  import org.cog.hymnchtv.notebook.data.NotebookDatabase
  import org.cog.hymnchtv.notebook.data.entity.FavoriteEntity
  import org.cog.hymnchtv.notebook.model.Clock
  import org.cog.hymnchtv.notebook.model.DeviceIdProvider
  import org.cog.hymnchtv.notebook.model.FavoriteIds
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.isActive
  import org.cog.hymnchtv.notebook.repo.FavoriteRepository

  class RoomFavoriteRepository(
      private val db: NotebookDatabase,
      private val clock: Clock,
      private val device: DeviceIdProvider,
  ) : FavoriteRepository {
      private val dao get() = db.favoriteDao()

      override suspend fun findAll(): List<FavoriteEntity> = dao.findActive()

      override suspend fun findById(id: String): FavoriteEntity? = dao.findById(id)?.takeIf { it.isActive }

      /** The id is always derived from the hymn key; the incoming id is ignored. */
      override suspend fun create(item: FavoriteEntity): FavoriteEntity =
          checkNotNull(setFavorite(item.hymn, true)) { "setFavorite(true) always returns a row" }

      /** A favorite has no editable content besides its key (= its identity), so update only bumps updatedAt. */
      override suspend fun update(item: FavoriteEntity): FavoriteEntity? = db.withTransaction {
          val existing = findById(item.id) ?: return@withTransaction null
          val row = existing.copy(updatedAt = clock.nowMillis(), updatedBy = device.deviceId())
          dao.upsert(row)
          row
      }

      override suspend fun delete(id: String): Boolean = db.withTransaction {
          val existing = findById(id) ?: return@withTransaction false
          val now = clock.nowMillis()
          dao.upsert(existing.copy(updatedAt = now, deletedAt = now, updatedBy = device.deviceId()))
          true
      }

      override suspend fun isFavorite(key: HymnKey): Boolean = dao.isFavorite(key.hymnType, key.hymnNo, key.isFu)

      override suspend fun setFavorite(key: HymnKey, favorite: Boolean): FavoriteEntity? = db.withTransaction {
          val id = FavoriteIds.forKey(key)
          val existing = dao.findById(id)
          val now = clock.nowMillis()
          val by = device.deviceId()
          val row = if (favorite) {
              if (existing != null && existing.isActive) return@withTransaction existing
              existing?.copy(updatedAt = now, deletedAt = null, updatedBy = by)
                  ?: FavoriteEntity(id, key, now, now, null, by)
          } else {
              if (existing == null || !existing.isActive) return@withTransaction existing
              existing.copy(updatedAt = now, deletedAt = now, updatedBy = by)
          }
          dao.upsert(row)
          row
      }

      override suspend fun toggle(key: HymnKey): Boolean = db.withTransaction {
          val next = !dao.isFavorite(key.hymnType, key.hymnNo, key.isFu)
          setFavorite(key, next)
          next
      }
  }
  ```

  `repo/room/RoomSingLogRepository.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.repo.room

  import androidx.room.withTransaction
  import org.cog.hymnchtv.notebook.data.NotebookDatabase
  import org.cog.hymnchtv.notebook.data.SingStats
  import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
  import org.cog.hymnchtv.notebook.model.Clock
  import org.cog.hymnchtv.notebook.model.DedupeWindow
  import org.cog.hymnchtv.notebook.model.DeviceIdProvider
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.IdGenerator
  import org.cog.hymnchtv.notebook.model.NotebookValidation
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.cog.hymnchtv.notebook.model.SingSource
  import org.cog.hymnchtv.notebook.model.isActive
  import org.cog.hymnchtv.notebook.repo.SingLogRepository

  class RoomSingLogRepository(
      private val db: NotebookDatabase,
      private val clock: Clock,
      private val ids: IdGenerator,
      private val device: DeviceIdProvider,
  ) : SingLogRepository {
      private val dao get() = db.singLogDao()

      override suspend fun findAll(): List<SingLogEntity> = dao.findActive()

      override suspend fun findById(id: String): SingLogEntity? = dao.findById(id)?.takeIf { it.isActive }

      override suspend fun create(item: SingLogEntity): SingLogEntity {
          val now = clock.nowMillis()
          val row = item.copy(
              id = resolveId(item.id, ids),
              sungAt = NotebookValidation.sungAt(item.sungAt, now),
              playlistId = optionalUuid(item.playlistId),
              createdAt = now, updatedAt = now, deletedAt = null, updatedBy = device.deviceId(),
          )
          dao.upsert(row)
          return row
      }

      override suspend fun update(item: SingLogEntity): SingLogEntity? = db.withTransaction {
          val existing = findById(item.id) ?: return@withTransaction null
          val now = clock.nowMillis()
          val row = item.copy(
              sungAt = NotebookValidation.sungAt(item.sungAt, now),
              playlistId = optionalUuid(item.playlistId),
              createdAt = existing.createdAt, updatedAt = now, deletedAt = null, updatedBy = device.deviceId(),
          )
          dao.upsert(row)
          row
      }

      override suspend fun delete(id: String): Boolean = db.withTransaction {
          val existing = findById(id) ?: return@withTransaction false
          val now = clock.nowMillis()
          dao.upsert(existing.copy(updatedAt = now, deletedAt = now, updatedBy = device.deviceId()))
          true
      }

      override suspend fun record(
          key: HymnKey,
          sungAt: Long,
          occasion: Occasion,
          source: SingSource,
          playlistId: String?,
      ): SingLogEntity = create(newLog(key, sungAt, occasion, source, playlistId))

      /** Check and insert run in one write transaction; SQLite allows a single writer, so this is atomic. */
      override suspend fun recordUnlessDuplicate(
          key: HymnKey,
          sungAt: Long,
          occasion: Occasion,
          source: SingSource,
          windowMillis: Long,
      ): SingLogEntity? {
          require(windowMillis >= 0) { "windowMillis must be >= 0" }
          NotebookValidation.sungAt(sungAt, clock.nowMillis())
          return db.withTransaction {
              val duplicate = dao.existsBetween(
                  key.hymnType, key.hymnNo, key.isFu,
                  DedupeWindow.lowerExclusive(sungAt, windowMillis), DedupeWindow.upperExclusive(sungAt, windowMillis),
              )
              if (duplicate) null else create(newLog(key, sungAt, occasion, source, null))
          }
      }

      override suspend fun statsFor(key: HymnKey): SingStats = dao.statsFor(key.hymnType, key.hymnNo, key.isFu)

      override suspend fun latestFor(key: HymnKey): SingLogEntity? =
          dao.latestForHymn(key.hymnType, key.hymnNo, key.isFu)

      override suspend fun findByHymn(key: HymnKey): List<SingLogEntity> =
          dao.findByHymn(key.hymnType, key.hymnNo, key.isFu)

      override suspend fun findBetween(fromInclusive: Long, toExclusive: Long): List<SingLogEntity> {
          NotebookValidation.timeRange(fromInclusive, toExclusive)
          return dao.findBetween(fromInclusive, toExclusive)
      }

      private fun newLog(key: HymnKey, sungAt: Long, occasion: Occasion, source: SingSource, playlistId: String?) =
          SingLogEntity(
              id = "", hymn = key, sungAt = sungAt, occasion = occasion, source = source,
              playlistId = playlistId, createdAt = 0, updatedAt = 0,
          )
  }
  ```

  `repo/room/RoomNoteRepository.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.repo.room

  import androidx.room.withTransaction
  import org.cog.hymnchtv.notebook.data.NotebookDatabase
  import org.cog.hymnchtv.notebook.data.entity.NoteEntity
  import org.cog.hymnchtv.notebook.model.Clock
  import org.cog.hymnchtv.notebook.model.DeviceIdProvider
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.IdGenerator
  import org.cog.hymnchtv.notebook.model.NotebookValidation
  import org.cog.hymnchtv.notebook.model.isActive
  import org.cog.hymnchtv.notebook.repo.NoteRepository

  class RoomNoteRepository(
      private val db: NotebookDatabase,
      private val clock: Clock,
      private val ids: IdGenerator,
      private val device: DeviceIdProvider,
  ) : NoteRepository {
      private val dao get() = db.noteDao()

      override suspend fun findAll(): List<NoteEntity> = dao.findActive()

      override suspend fun findById(id: String): NoteEntity? = dao.findById(id)?.takeIf { it.isActive }

      override suspend fun create(item: NoteEntity): NoteEntity {
          val now = clock.nowMillis()
          val row = item.copy(
              id = resolveId(item.id, ids),
              body = NotebookValidation.noteBody(item.body),
              singLogId = optionalUuid(item.singLogId),
              createdAt = now, updatedAt = now, deletedAt = null, updatedBy = device.deviceId(),
          )
          dao.upsert(row)
          return row
      }

      override suspend fun update(item: NoteEntity): NoteEntity? = db.withTransaction {
          val existing = findById(item.id) ?: return@withTransaction null
          val row = item.copy(
              body = NotebookValidation.noteBody(item.body),
              singLogId = optionalUuid(item.singLogId),
              createdAt = existing.createdAt, updatedAt = clock.nowMillis(), deletedAt = null, updatedBy = device.deviceId(),
          )
          dao.upsert(row)
          row
      }

      override suspend fun delete(id: String): Boolean = db.withTransaction {
          val existing = findById(id) ?: return@withTransaction false
          val now = clock.nowMillis()
          dao.upsert(existing.copy(updatedAt = now, deletedAt = now, updatedBy = device.deviceId()))
          true
      }

      override suspend fun add(key: HymnKey, body: String, singLogId: String?): NoteEntity =
          create(NoteEntity(id = "", hymn = key, body = body, singLogId = singLogId, createdAt = 0, updatedAt = 0))

      override suspend fun findByHymn(key: HymnKey): List<NoteEntity> =
          dao.findByHymn(key.hymnType, key.hymnNo, key.isFu)
  }
  ```

  `repo/room/RoomPlaylistRepository.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.repo.room

  import androidx.room.withTransaction
  import org.cog.hymnchtv.notebook.data.NotebookDatabase
  import org.cog.hymnchtv.notebook.data.entity.PlaylistEntity
  import org.cog.hymnchtv.notebook.data.entity.PlaylistItemEntity
  import org.cog.hymnchtv.notebook.model.Clock
  import org.cog.hymnchtv.notebook.model.DeviceIdProvider
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.IdGenerator
  import org.cog.hymnchtv.notebook.model.NotebookValidation
  import org.cog.hymnchtv.notebook.model.isActive
  import org.cog.hymnchtv.notebook.repo.PlaylistRepository

  class RoomPlaylistRepository(
      private val db: NotebookDatabase,
      private val clock: Clock,
      private val ids: IdGenerator,
      private val device: DeviceIdProvider,
  ) : PlaylistRepository {
      private val playlistDao get() = db.playlistDao()
      private val itemDao get() = db.playlistItemDao()

      override suspend fun findAll(): List<PlaylistEntity> = playlistDao.findActive()

      override suspend fun findById(id: String): PlaylistEntity? = playlistDao.findById(id)?.takeIf { it.isActive }

      override suspend fun create(item: PlaylistEntity): PlaylistEntity {
          val now = clock.nowMillis()
          val row = item.copy(
              id = resolveId(item.id, ids),
              name = NotebookValidation.playlistName(item.name),
              createdAt = now, updatedAt = now, deletedAt = null, updatedBy = device.deviceId(),
          )
          playlistDao.upsert(row)
          return row
      }

      override suspend fun update(item: PlaylistEntity): PlaylistEntity? = db.withTransaction {
          val existing = findById(item.id) ?: return@withTransaction null
          val row = item.copy(
              name = NotebookValidation.playlistName(item.name),
              createdAt = existing.createdAt, updatedAt = clock.nowMillis(), deletedAt = null, updatedBy = device.deviceId(),
          )
          playlistDao.upsert(row)
          row
      }

      override suspend fun delete(id: String): Boolean = db.withTransaction {
          val existing = findById(id) ?: return@withTransaction false
          val now = clock.nowMillis()
          val by = device.deviceId()
          playlistDao.upsert(existing.copy(updatedAt = now, deletedAt = now, updatedBy = by))
          val removed = itemDao.itemsOf(id).map { it.copy(updatedAt = now, deletedAt = now, updatedBy = by) }
          if (removed.isNotEmpty()) itemDao.upsertAll(removed)
          true
      }

      override suspend fun createPlaylist(name: String): PlaylistEntity =
          create(PlaylistEntity(id = "", name = name, createdAt = 0, updatedAt = 0))

      override suspend fun rename(id: String, name: String): PlaylistEntity? =
          findById(id)?.let { update(it.copy(name = name)) }

      override suspend fun items(playlistId: String): List<PlaylistItemEntity> = itemDao.itemsOf(playlistId)

      override suspend fun addItem(playlistId: String, key: HymnKey): PlaylistItemEntity? = db.withTransaction {
          if (findById(playlistId) == null) return@withTransaction null
          val now = clock.nowMillis()
          val row = PlaylistItemEntity(
              id = ids.newId(), playlistId = playlistId,
              position = nextSlot(playlistId),
              hymn = key, createdAt = now, updatedAt = now, updatedBy = device.deviceId(),
          )
          itemDao.insert(row)
          row
      }

      override suspend fun removeItem(itemId: String): Boolean = db.withTransaction {
          val existing = itemDao.findById(itemId)?.takeIf { it.isActive } ?: return@withTransaction false
          val now = clock.nowMillis()
          itemDao.upsert(existing.copy(updatedAt = now, deletedAt = now, updatedBy = device.deviceId()))
          true
      }

      /** Moves every item, in the new order, past the highest slot ever used, so no unique (playlistId, position) clash. */
      override suspend fun reorder(playlistId: String, orderedItemIds: List<String>): List<PlaylistItemEntity> =
          db.withTransaction {
              val current = itemDao.itemsOf(playlistId)
              require(orderedItemIds.size == current.size && orderedItemIds.toSet() == current.map { it.id }.toSet()) {
                  "orderedItemIds must be a permutation of the playlist's active items"
              }
              if (orderedItemIds == current.map { it.id }) return@withTransaction current
              val byId = current.associateBy { it.id }
              val base = nextSlot(playlistId)
              val now = clock.nowMillis()
              val by = device.deviceId()
              val reordered = orderedItemIds.mapIndexed { index, id ->
                  byId.getValue(id).copy(position = base + index, updatedAt = now, updatedBy = by)
              }
              itemDao.upsertAll(reordered)
              reordered
          }

      private suspend fun nextSlot(playlistId: String): Int = (itemDao.maxPositionIncludingDeleted(playlistId) ?: -1) + 1
  }
  ```

- [ ] **Step 3：在 `api34b` 跑測試，確認通過**

  ```bash
  export ANDROID_SERIAL=emulator-5580
  test "$(adb emu avd name | head -1 | tr -d '\r')" = api34b && \
  ./gradlew :hymnchtv:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=org.cog.hymnchtv.notebook --console=plain
  ```

  Expected: 全部通過，包括：
  - Task 2 的 `NotebookDaoTest`（8 個）和 `HymnNumberingConsistencyTest`（2 個）。
  - 本 task 的 `RoomFavoriteRepositoryTest`（3 個）、`RoomSingLogRepositoryTest`（8 個）、`RoomSingLogRepositoryContractTest`（6 個，和 JVM 上的假實作跑同一組）、`RoomSingLogConcurrencyTest`（1 個）、`RoomNoteRepositoryTest`（2 個）、`RoomPlaylistRepositoryTest`（5 個）。

- [ ] **Step 4：Commit**

  ```bash
  ./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug --console=plain
  git add hymnchtv/src/main/java/org/cog/hymnchtv/notebook/repo/room hymnchtv/src/androidTest/java/org/cog/hymnchtv/notebook/repo
  git commit -m "feat: add Room repositories with atomic auto-record dedupe" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

### Task 5：自動記錄參數與場合推測（Lane B）

> 已由 Room 統一計畫 Task 6(a) 遷移：本 Task 內的 `NotebookDatabase`／`notebook.db`／`TRUNCATE` 為歷史片段，現況為 `HymnchtvDatabase`（`hymnchtv.db`、WAL）。

**前置條件：** Task 3 已 commit。在 worktree `feat/d1a-notebook-data-lane-b` 中進行。只跑 JVM 測試，不使用模擬器。

**Files:**
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/notebook/record/AutoRecordConfig.kt`、`OccasionInference.kt`
- Test: `hymnchtv/src/test/java/org/cog/hymnchtv/notebook/record/AutoRecordConfigTest.kt`、`OccasionInferenceTest.kt`

（去重的判斷不在這裡：它是 `SingLogRepository.recordUnlessDuplicate` 的原子操作，邊界由 Task 1 的 `DedupeWindow` 定義。）

- [ ] **Step 1：寫會失敗的測試**

  `AutoRecordConfigTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.record

  import com.google.common.truth.Truth.assertThat
  import org.junit.Test

  class AutoRecordConfigTest {
      @Test
      fun defaultsMatchTheSpec() {
          val config = AutoRecordConfig()
          assertThat(config.visibleThresholdMillis).isEqualTo(2 * 60 * 1000L)
          assertThat(config.dedupeWindowMillis).isEqualTo(3 * 60 * 60 * 1000L)
      }

      @Test
      fun rejectsInvalidValues() {
          assertThat(runCatching { AutoRecordConfig(visibleThresholdMillis = 0) }.exceptionOrNull())
              .isInstanceOf(IllegalArgumentException::class.java)
          assertThat(runCatching { AutoRecordConfig(dedupeWindowMillis = -1) }.exceptionOrNull())
              .isInstanceOf(IllegalArgumentException::class.java)
      }
  }
  ```

  `OccasionInferenceTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.record

  import com.google.common.truth.Truth.assertThat
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.junit.Test
  import java.util.Calendar
  import java.util.TimeZone

  /**
   * Dates: 2026-10-04 Sunday, 2026-10-03 Saturday, 2026-09-30 Wednesday.
   * America/Los_Angeles: spring forward on Sunday 2026-03-08, fall back on Sunday 2026-11-01.
   */
  class OccasionInferenceTest {
      private val taipei = TimeZone.getTimeZone("Asia/Taipei")
      private val utc = TimeZone.getTimeZone("UTC")
      private val la = TimeZone.getTimeZone("America/Los_Angeles")

      private fun at(zone: TimeZone, y: Int, m: Int, d: Int, h: Int, min: Int = 0): Long =
          Calendar.getInstance(zone).apply {
              clear()
              set(y, m - 1, d, h, min)
          }.timeInMillis

      private fun infer(time: Long, last: Occasion?, zone: TimeZone = taipei) = OccasionInference.infer(time, zone, last)

      @Test
      fun sundayWindowBoundaries() {
          assertThat(infer(at(taipei, 2026, 10, 4, 5, 59), Occasion.SMALL_GROUP)).isEqualTo(Occasion.SMALL_GROUP)
          assertThat(infer(at(taipei, 2026, 10, 4, 6, 0), Occasion.SMALL_GROUP)).isEqualTo(Occasion.LORDS_DAY)
          assertThat(infer(at(taipei, 2026, 10, 4, 12, 59), Occasion.SMALL_GROUP)).isEqualTo(Occasion.LORDS_DAY)
          assertThat(infer(at(taipei, 2026, 10, 4, 13, 0), Occasion.SMALL_GROUP)).isEqualTo(Occasion.SMALL_GROUP)
          assertThat(infer(at(taipei, 2026, 10, 4, 13, 0), null)).isEqualTo(Occasion.HOME)
      }

      @Test
      fun otherDaysUseTheLastChoice() {
          assertThat(infer(at(taipei, 2026, 10, 3, 10), Occasion.SMALL_GROUP)).isEqualTo(Occasion.SMALL_GROUP)
          assertThat(infer(at(taipei, 2026, 9, 30, 6), Occasion.MORNING_REVIVAL)).isEqualTo(Occasion.MORNING_REVIVAL)
      }

      @Test
      fun lastChoiceLordsDayOutsideTheWindowBecomesHome() {
          assertThat(infer(at(taipei, 2026, 9, 30, 20), Occasion.LORDS_DAY)).isEqualTo(Occasion.HOME)
      }

      @Test
      fun neverChosenIsHome() {
          assertThat(infer(at(taipei, 2026, 9, 30, 20), null)).isEqualTo(Occasion.HOME)
      }

      @Test
      fun sameInstantDependsOnTheZone() {
          val instant = at(utc, 2026, 10, 4, 2) // Sunday 10:00 in Taipei, Sunday 02:00 in UTC
          assertThat(infer(instant, null, taipei)).isEqualTo(Occasion.LORDS_DAY)
          assertThat(infer(instant, null, utc)).isEqualTo(Occasion.HOME)
      }

      @Test
      fun springForwardSundayUsesDaylightTime() {
          assertThat(infer(at(la, 2026, 3, 8, 5, 59), null, la)).isEqualTo(Occasion.HOME)
          assertThat(infer(at(la, 2026, 3, 8, 6, 0), null, la)).isEqualTo(Occasion.LORDS_DAY)
          // 13:30 UTC is 06:30 PDT on the switch day, but would be 05:30 under PST
          assertThat(infer(at(utc, 2026, 3, 8, 13, 30), null, la)).isEqualTo(Occasion.LORDS_DAY)
          // one week earlier the same UTC time is still 05:30 PST
          assertThat(infer(at(utc, 2026, 3, 1, 13, 30), null, la)).isEqualTo(Occasion.HOME)
      }

      @Test
      fun fallBackSundayUsesStandardTime() {
          assertThat(infer(at(la, 2026, 11, 1, 6, 0), null, la)).isEqualTo(Occasion.LORDS_DAY)
          assertThat(infer(at(la, 2026, 11, 1, 12, 59), null, la)).isEqualTo(Occasion.LORDS_DAY)
          assertThat(infer(at(la, 2026, 11, 1, 13, 0), null, la)).isEqualTo(Occasion.HOME)
          // 13:30 UTC is 05:30 PST after the switch, but would be 06:30 under PDT
          assertThat(infer(at(utc, 2026, 11, 1, 13, 30), null, la)).isEqualTo(Occasion.HOME)
      }

      @Test
      fun repeatedHourOnFallBackIsNotLordsDay() {
          val firstOneThirty = at(utc, 2026, 11, 1, 8, 30)  // 01:30 PDT
          val secondOneThirty = at(utc, 2026, 11, 1, 9, 30) // 01:30 PST
          assertThat(infer(firstOneThirty, null, la)).isEqualTo(Occasion.HOME)
          assertThat(infer(secondOneThirty, null, la)).isEqualTo(Occasion.HOME)
      }
  }
  ```

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.notebook.record.*'`
  Expected: 編譯失敗，`Unresolved reference 'AutoRecordConfig'`。

- [ ] **Step 2：實作**

  `record/AutoRecordConfig.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.record

  /** Code constants (not user settings; only the on/off switch is a setting). */
  data class AutoRecordConfig(
      val visibleThresholdMillis: Long = DEFAULT_VISIBLE_THRESHOLD_MILLIS,
      val dedupeWindowMillis: Long = DEFAULT_DEDUPE_WINDOW_MILLIS,
  ) {
      init {
          require(visibleThresholdMillis > 0) { "visibleThresholdMillis must be > 0" }
          require(dedupeWindowMillis >= 0) { "dedupeWindowMillis must be >= 0" }
      }

      companion object {
          const val DEFAULT_VISIBLE_THRESHOLD_MILLIS = 2 * 60 * 1000L
          const val DEFAULT_DEDUPE_WINDOW_MILLIS = 3 * 60 * 60 * 1000L
      }
  }
  ```

  `record/OccasionInference.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.record

  import org.cog.hymnchtv.notebook.model.Occasion
  import java.util.Calendar
  import java.util.TimeZone

  /**
   * Guesses the occasion of an auto-recorded log (the user can correct it), using local wall-clock time in [zone]:
   * 1. Sunday 06:00 (incl.) – 13:00 (excl.) → LORDS_DAY.
   * 2. Otherwise the occasion the user last picked by hand, except LORDS_DAY which becomes HOME.
   * 3. Never picked → HOME.
   * Calendar handles DST; java.time would need API 26 (minSdk is 24).
   */
  object OccasionInference {
      const val LORDS_DAY_START_HOUR = 6
      const val LORDS_DAY_END_HOUR = 13

      @JvmStatic
      fun infer(nowMillis: Long, zone: TimeZone, lastChosen: Occasion?): Occasion {
          val calendar = Calendar.getInstance(zone).apply { timeInMillis = nowMillis }
          val hour = calendar.get(Calendar.HOUR_OF_DAY)
          val isSunday = calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
          if (isSunday && hour >= LORDS_DAY_START_HOUR && hour < LORDS_DAY_END_HOUR) return Occasion.LORDS_DAY
          return when (lastChosen) {
              null, Occasion.LORDS_DAY -> Occasion.HOME
              else -> lastChosen
          }
      }
  }
  ```

- [ ] **Step 3：執行測試，確認它通過**

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.notebook.record.*'`
  Expected: 10 個測試全部通過。

- [ ] **Step 4：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/notebook/record hymnchtv/src/test/java/org/cog/hymnchtv/notebook/record
  git commit -m "feat: add auto-record config and DST-safe occasion inference" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

### Task 6：SingTracker（Lane B）

> 已由 Room 統一計畫 Task 6(a) 遷移：本 Task 內的 `NotebookDatabase`／`notebook.db`／`TRUNCATE` 為歷史片段，現況為 `HymnchtvDatabase`（`hymnchtv.db`、WAL）。

**Files:**
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/notebook/record/SingTracker.kt`
- Test: `hymnchtv/src/test/java/org/cog/hymnchtv/notebook/record/SingTrackerTest.kt`

- [ ] **Step 1：寫會失敗的測試**

  ```kotlin
  package org.cog.hymnchtv.notebook.record

  import com.google.common.truth.Truth.assertThat
  import kotlinx.coroutines.ExperimentalCoroutinesApi
  import kotlinx.coroutines.flow.toList
  import kotlinx.coroutines.launch
  import kotlinx.coroutines.test.TestScope
  import kotlinx.coroutines.test.UnconfinedTestDispatcher
  import kotlinx.coroutines.test.advanceTimeBy
  import kotlinx.coroutines.test.runCurrent
  import kotlinx.coroutines.test.runTest
  import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
  import org.cog.hymnchtv.notebook.fakes.FakeNotebookPrefs
  import org.cog.hymnchtv.notebook.fakes.InMemorySingLogRepository
  import org.cog.hymnchtv.notebook.model.Clock
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.HymnTypes
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.cog.hymnchtv.notebook.model.SingSource
  import org.junit.Test
  import java.util.TimeZone

  @OptIn(ExperimentalCoroutinesApi::class)
  class SingTrackerTest {
      private val key = HymnKey.of(HymnTypes.DB, 1)
      private val other = HymnKey.of(HymnTypes.DB, 2)
      private val threshold = AutoRecordConfig.DEFAULT_VISIBLE_THRESHOLD_MILLIS
      private val window = AutoRecordConfig.DEFAULT_DEDUPE_WINDOW_MILLIS

      private class Harness(scope: TestScope, prefs: FakeNotebookPrefs, base: Long = BASE, zone: () -> TimeZone = { UTC }) {
          val clock = Clock { base + scope.testScheduler.currentTime }
          val repo = InMemorySingLogRepository(clock)
          val tracker = SingTracker(repo, prefs, clock, scope.backgroundScope, zone = zone)
      }

      private fun TestScope.harness(prefs: FakeNotebookPrefs = FakeNotebookPrefs()) = Harness(this, prefs)

      private fun TestScope.advance(millis: Long) {
          advanceTimeBy(millis)
          runCurrent()
      }

      @Test
      fun visibleForThresholdRecordsOnce() = runTest {
          val h = harness()
          h.tracker.onHymnVisible(key)
          advance(threshold - 1)
          assertThat(h.repo.rows).isEmpty()
          advance(1)
          val log = h.repo.rows.values.single()
          assertThat(log.hymn).isEqualTo(key)
          assertThat(log.source).isEqualTo(SingSource.AUTO)
          assertThat(log.sungAt).isEqualTo(BASE + threshold)
          advance(threshold * 5)
          assertThat(h.repo.rows).hasSize(1)
      }

      @Test
      fun hidingRestartsTheTimer() = runTest {
          val h = harness()
          h.tracker.onHymnVisible(key)
          advance(threshold - 20_000)
          h.tracker.onHymnHidden(key)
          advance(threshold)
          assertThat(h.repo.rows).isEmpty()

          h.tracker.onHymnVisible(key)
          advance(threshold - 1)
          assertThat(h.repo.rows).isEmpty()
          advance(1)
          assertThat(h.repo.rows).hasSize(1)
      }

      @Test
      fun switchingHymnCancelsThePreviousTimer() = runTest {
          val h = harness()
          h.tracker.onHymnVisible(key)
          advance(threshold / 2)
          h.tracker.onHymnVisible(other)
          advance(threshold)
          assertThat(h.repo.rows.values.map { it.hymn }).containsExactly(other)
      }

      @Test
      fun repeatedVisibleForTheSameHymnKeepsTheTimer() = runTest {
          val h = harness()
          h.tracker.onHymnVisible(key)
          advance(threshold / 2)
          h.tracker.onHymnVisible(key)
          advance(threshold / 2)
          assertThat(h.repo.rows).hasSize(1)
      }

      @Test
      fun hidingAnotherHymnIsIgnored() = runTest {
          val h = harness()
          h.tracker.onHymnVisible(key)
          h.tracker.onHymnHidden(other)
          advance(threshold)
          assertThat(h.repo.rows).hasSize(1)
      }

      @Test
      fun mediaCompletedRecordsImmediately() = runTest {
          val h = harness()
          h.tracker.onMediaCompleted(key)
          runCurrent()
          assertThat(h.repo.rows.values.single().sungAt).isEqualTo(BASE)
      }

      @Test
      fun simultaneousTriggersRecordOnce() = runTest {
          val h = harness()
          h.tracker.onMediaCompleted(key)
          h.tracker.onMediaCompleted(key)
          h.tracker.onMediaCompleted(key)
          runCurrent()
          assertThat(h.repo.rows).hasSize(1)
      }

      @Test
      fun dedupeWindowAppliesAcrossTriggers() = runTest {
          val h = harness()
          h.tracker.onMediaCompleted(key)
          runCurrent()
          h.tracker.onHymnVisible(key)
          advance(threshold)
          assertThat(h.repo.rows).hasSize(1)

          h.tracker.onHymnHidden(key)
          advance(window - threshold - 1)
          h.tracker.onMediaCompleted(key)
          runCurrent()
          assertThat(h.repo.rows).hasSize(1)

          advance(1)
          h.tracker.onMediaCompleted(key)
          runCurrent()
          assertThat(h.repo.rows).hasSize(2)
      }

      @Test
      fun disabledPrefSkipsEverything() = runTest {
          val h = harness(FakeNotebookPrefs(autoRecord = false))
          h.tracker.onMediaCompleted(key)
          h.tracker.onHymnVisible(key)
          advance(threshold)
          assertThat(h.repo.rows).isEmpty()
      }

      @Test
      fun occasionComesFromInference() = runTest {
          val h = harness(FakeNotebookPrefs(lastChosen = Occasion.SMALL_GROUP))
          h.tracker.onMediaCompleted(key)
          runCurrent()
          assertThat(h.repo.rows.values.single().occasion).isEqualTo(Occasion.SMALL_GROUP)
      }

      @Test
      fun zoneIsReadAtRecordTime() = runTest {
          var zone: TimeZone = UTC
          val h = Harness(this, FakeNotebookPrefs(), base = SUNDAY_0200_UTC, zone = { zone })
          h.tracker.onMediaCompleted(key)
          runCurrent()
          zone = TAIPEI // the user flies to Taipei: Sunday 10:00 local
          h.tracker.onMediaCompleted(other)
          runCurrent()
          assertThat(h.repo.rows.values.sortedBy { it.hymn.hymnNo }.map { it.occasion })
              .containsExactly(Occasion.HOME, Occasion.LORDS_DAY).inOrder()
      }

      @Test
      fun repositoryFailureIsSwallowed() = runTest {
          val h = harness()
          h.repo.failNext = true
          h.tracker.onMediaCompleted(key)
          runCurrent()
          assertThat(h.repo.rows).isEmpty()
          h.tracker.onMediaCompleted(key)
          runCurrent()
          assertThat(h.repo.rows).hasSize(1)
      }

      @Test
      fun recordedFlowEmitsEachNewLogOnly() = runTest {
          val h = harness()
          val emitted = mutableListOf<SingLogEntity>()
          backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { h.tracker.recorded.toList(emitted) }
          h.tracker.onMediaCompleted(key)
          runCurrent()
          h.tracker.onMediaCompleted(key)
          runCurrent()
          assertThat(emitted.map { it.id }).containsExactly(h.repo.rows.keys.single())
      }

      private companion object {
          const val BASE = 1_790_733_600_000L            // Wednesday 2026-09-30 02:00 UTC
          const val SUNDAY_0200_UTC = 1_791_079_200_000L // Sunday 2026-10-04 02:00 UTC = 10:00 Taipei
          val UTC: TimeZone = TimeZone.getTimeZone("UTC")
          val TAIPEI: TimeZone = TimeZone.getTimeZone("Asia/Taipei")
      }
  }
  ```

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.notebook.record.SingTrackerTest'`
  Expected: 編譯失敗，`Unresolved reference 'SingTracker'`。

- [ ] **Step 2：實作** `record/SingTracker.kt`

  ```kotlin
  package org.cog.hymnchtv.notebook.record

  import kotlinx.coroutines.CoroutineScope
  import kotlinx.coroutines.Job
  import kotlinx.coroutines.NonCancellable
  import kotlinx.coroutines.delay
  import kotlinx.coroutines.flow.MutableSharedFlow
  import kotlinx.coroutines.flow.SharedFlow
  import kotlinx.coroutines.flow.asSharedFlow
  import kotlinx.coroutines.launch
  import kotlinx.coroutines.withContext
  import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
  import org.cog.hymnchtv.notebook.model.Clock
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.SingSource
  import org.cog.hymnchtv.notebook.repo.SingLogRepository
  import org.cog.hymnchtv.notebook.settings.NotebookPrefs
  import timber.log.Timber
  import java.util.TimeZone

  /**
   * Turns lyrics-page visibility and media completion into AUTO sing logs.
   *
   * Threading: the on* methods are called from the main thread (any thread is safe), return immediately and
   * never do I/O. [scope] is NotebookGraph.appScope (process lifetime): a pending 2-minute timer dies with the
   * process, which is acceptable. Dedupe is atomic in [SingLogRepository.recordUnlessDuplicate]; once a write
   * starts it runs NonCancellable. Failures are logged and dropped, never thrown to the caller.
   */
  class SingTracker(
      private val singLogs: SingLogRepository,
      private val prefs: NotebookPrefs,
      private val clock: Clock,
      private val scope: CoroutineScope,
      private val config: AutoRecordConfig = AutoRecordConfig(),
      private val zone: () -> TimeZone = { TimeZone.getDefault() },
  ) {
      private data class Visible(val key: HymnKey, val job: Job)

      private val lock = Any()

      /** Replaced (never mutated) under [lock]. */
      @Volatile
      private var visible: Visible? = null

      private val recordedFlow = MutableSharedFlow<SingLogEntity>(extraBufferCapacity = 16)

      /**
       * Each new AUTO log, emitted on [scope]'s threads. Java: NotebookAsync.observeAutoRecorded (main thread).
       * Kotlin UI: lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) { recorded.collect { … } } }.
       */
      val recorded: SharedFlow<SingLogEntity> = recordedFlow.asSharedFlow()

      /** The hymn's lyrics became visible; starts the continuous-visibility timer unless it already runs. */
      fun onHymnVisible(key: HymnKey) {
          synchronized(lock) {
              val current = visible
              if (current != null && current.key == key && current.job.isActive) return
              current?.job?.cancel()
              val job = scope.launch {
                  delay(config.visibleThresholdMillis)
                  recordIfAllowed(key)
              }
              visible = Visible(key, job)
          }
      }

      /** The hymn's lyrics are no longer visible (page changed, activity paused); its timer stops. */
      fun onHymnHidden(key: HymnKey) {
          synchronized(lock) {
              val current = visible ?: return
              if (current.key != key) return
              current.job.cancel()
              visible = null
          }
      }

      /** Media playback of the hymn reached its end. */
      fun onMediaCompleted(key: HymnKey) {
          scope.launch { recordIfAllowed(key) }
      }

      /** Returns the new log, or null when disabled, duplicate or failed. */
      internal suspend fun recordIfAllowed(key: HymnKey): SingLogEntity? = withContext(NonCancellable) {
          try {
              if (!prefs.autoRecordEnabled) return@withContext null
              val now = clock.nowMillis()
              val occasion = OccasionInference.infer(now, zone(), prefs.lastChosenOccasion)
              singLogs.recordUnlessDuplicate(key, now, occasion, SingSource.AUTO, config.dedupeWindowMillis)
                  ?.also { recordedFlow.tryEmit(it) }
          } catch (e: Exception) {
              Timber.e(e, "Auto record failed for %s", key)
              null
          }
      }
  }
  ```

- [ ] **Step 3：執行測試，確認它通過**

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.notebook.record.*'`
  Expected: `SingTrackerTest` 13 個，加上 Task 5 的 10 個，全部通過。

- [ ] **Step 4：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/notebook/record/SingTracker.kt hymnchtv/src/test/java/org/cog/hymnchtv/notebook/record/SingTrackerTest.kt
  git commit -m "feat: add SingTracker for automatic sing logging" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

### Task 7：SharedPrefsNotebookPrefs（Lane B）

> 已由 Room 統一計畫 Task 6(a) 遷移：本 Task 內的 `NotebookDatabase`／`notebook.db`／`TRUNCATE` 為歷史片段，現況為 `HymnchtvDatabase`（`hymnchtv.db`、WAL）。

**Files:**
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/notebook/settings/SharedPrefsNotebookPrefs.kt`
- Test: `hymnchtv/src/androidTest/java/org/cog/hymnchtv/notebook/settings/SharedPrefsNotebookPrefsTest.kt`（Lane B 只編譯；由協調者在 Task 12 Step 1 執行）

- [ ] **Step 1：寫測試**

  ```kotlin
  package org.cog.hymnchtv.notebook.settings

  import android.content.Context
  import android.content.SharedPreferences
  import androidx.test.core.app.ApplicationProvider
  import androidx.test.ext.junit.runners.AndroidJUnit4
  import com.google.common.truth.Truth.assertThat
  import org.cog.hymnchtv.notebook.model.NotebookValidation
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.junit.After
  import org.junit.Before
  import org.junit.Test
  import org.junit.runner.RunWith

  @RunWith(AndroidJUnit4::class)
  class SharedPrefsNotebookPrefsTest {
      private val context: Context = ApplicationProvider.getApplicationContext()
      private val raw: SharedPreferences
          get() = context.getSharedPreferences(NotebookPrefs.FILE_NAME, Context.MODE_PRIVATE)
      private val rawDevice: SharedPreferences
          get() = context.getSharedPreferences(NotebookPrefs.DEVICE_FILE_NAME, Context.MODE_PRIVATE)

      private fun clear() {
          raw.edit().clear().commit()
          rawDevice.edit().clear().commit()
      }

      @Before
      fun clearBefore() = clear()

      @After
      fun clearAfter() = clear()

      @Test
      fun defaults() {
          val prefs = SharedPrefsNotebookPrefs(context)
          assertThat(prefs.autoRecordEnabled).isTrue()
          assertThat(prefs.lastChosenOccasion).isNull()
      }

      @Test
      fun persistsAcrossInstances() {
          SharedPrefsNotebookPrefs(context).apply {
              setAutoRecordEnabled(false)
              setLastChosenOccasion(Occasion.SMALL_GROUP)
          }
          val again = SharedPrefsNotebookPrefs(context)
          assertThat(again.autoRecordEnabled).isFalse()
          assertThat(again.lastChosenOccasion).isEqualTo(Occasion.SMALL_GROUP)
      }

      @Test
      fun illegalStoredValuesNeverThrow() {
          raw.edit()
              .putString(NotebookPrefs.KEY_AUTO_RECORD, "yes")
              .putInt(NotebookPrefs.KEY_LAST_OCCASION, 3)
              .commit()
          val prefs = SharedPrefsNotebookPrefs(context)
          assertThat(prefs.autoRecordEnabled).isEqualTo(NotebookPrefs.DEFAULT_AUTO_RECORD)
          assertThat(prefs.lastChosenOccasion).isNull()

          raw.edit().putString(NotebookPrefs.KEY_LAST_OCCASION, "SUNDAY").commit()
          assertThat(prefs.lastChosenOccasion).isNull()
      }

      @Test
      fun deviceIdIsACanonicalUuidStableAcrossInstancesAndKeptOutOfNotebookXml() {
          val first = SharedPrefsNotebookPrefs(context).deviceId()
          assertThat(NotebookValidation.uuid(first)).isEqualTo(first)
          assertThat(SharedPrefsNotebookPrefs(context).deviceId()).isEqualTo(first)
          assertThat(rawDevice.getString(NotebookPrefs.KEY_DEVICE_ID, null)).isEqualTo(first)
          assertThat(raw.contains(NotebookPrefs.KEY_DEVICE_ID)).isFalse()
      }

      @Test
      fun corruptDeviceIdIsReplaced() {
          rawDevice.edit().putString(NotebookPrefs.KEY_DEVICE_ID, "NOT-A-UUID").commit()
          val id = SharedPrefsNotebookPrefs(context).deviceId()
          assertThat(id).isNotEqualTo("NOT-A-UUID")
          assertThat(NotebookValidation.uuid(id)).isEqualTo(id)
      }
  }
  ```

  Run: `./gradlew :hymnchtv:assembleDebugAndroidTest`
  Expected: 編譯失敗，`Unresolved reference 'SharedPrefsNotebookPrefs'`。

- [ ] **Step 2：實作**

  ```kotlin
  package org.cog.hymnchtv.notebook.settings

  import android.content.Context
  import org.cog.hymnchtv.notebook.model.NotebookValidation
  import org.cog.hymnchtv.notebook.model.Occasion
  import timber.log.Timber
  import java.util.UUID

  /**
   * NotebookPrefs backed by notebook.xml (backed up) and notebook_device.xml (not backed up).
   * Reads never throw: wrong types fall back to defaults. deviceId() may touch disk once; call it off the main thread.
   */
  class SharedPrefsNotebookPrefs(context: Context) : NotebookPrefs {
      private val prefs = context.applicationContext
          .getSharedPreferences(NotebookPrefs.FILE_NAME, Context.MODE_PRIVATE)
      private val devicePrefs = context.applicationContext
          .getSharedPreferences(NotebookPrefs.DEVICE_FILE_NAME, Context.MODE_PRIVATE)

      @Volatile
      private var cachedDeviceId: String? = null

      override val autoRecordEnabled: Boolean
          get() = try {
              prefs.getBoolean(NotebookPrefs.KEY_AUTO_RECORD, NotebookPrefs.DEFAULT_AUTO_RECORD)
          } catch (e: ClassCastException) {
              Timber.w(e, "Illegal %s value; using default", NotebookPrefs.KEY_AUTO_RECORD)
              NotebookPrefs.DEFAULT_AUTO_RECORD
          }

      override val lastChosenOccasion: Occasion?
          get() = try {
              Occasion.fromStorage(prefs.getString(NotebookPrefs.KEY_LAST_OCCASION, null))
          } catch (e: ClassCastException) {
              Timber.w(e, "Illegal %s value; ignoring", NotebookPrefs.KEY_LAST_OCCASION)
              null
          }

      override fun setAutoRecordEnabled(enabled: Boolean) {
          prefs.edit().putBoolean(NotebookPrefs.KEY_AUTO_RECORD, enabled).apply()
      }

      override fun setLastChosenOccasion(occasion: Occasion) {
          prefs.edit().putString(NotebookPrefs.KEY_LAST_OCCASION, occasion.name).apply()
      }

      /** Generated once per install; a corrupt value is replaced. commit() so the id is durable before first use. */
      override fun deviceId(): String = cachedDeviceId ?: synchronized(this) {
          cachedDeviceId ?: (storedDeviceId() ?: newDeviceId()).also { cachedDeviceId = it }
      }

      private fun storedDeviceId(): String? = try {
          devicePrefs.getString(NotebookPrefs.KEY_DEVICE_ID, null)
              ?.takeIf { runCatching { NotebookValidation.uuid(it) }.isSuccess }
      } catch (e: ClassCastException) {
          Timber.w(e, "Illegal %s value; regenerating", NotebookPrefs.KEY_DEVICE_ID)
          null
      }

      private fun newDeviceId(): String {
          val id = UUID.randomUUID().toString()
          devicePrefs.edit().putString(NotebookPrefs.KEY_DEVICE_ID, id).commit()
          return id
      }
  }
  ```

- [ ] **Step 3：確認編譯與 JVM 測試**

  Run: `./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug :hymnchtv:assembleDebugAndroidTest --console=plain`
  Expected: BUILD SUCCESSFUL（instrumented test 在 Task 12 Step 1 執行）。

- [ ] **Step 4：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/notebook/settings/SharedPrefsNotebookPrefs.kt hymnchtv/src/androidTest/java/org/cog/hymnchtv/notebook/settings
  git commit -m "feat: add SharedPreferences-backed notebook prefs with a per-install device id" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

### Task 8：備份模型與 JSON 編碼（線性，`feat/notebook-data`）

**前置條件：** Task 0–7 與 Room 統一遷移已 commit 在 `feat/notebook-data`（rev 7：實際只有這一條分支，不再有 lane 分支）。Task 8–13 全部直接在 worktree `/Users/hitobias/orca/hymnchtv-d1a`（分支 `feat/notebook-data`）上線性進行。Task 8–11 只跑 JVM 測試並以 `assembleDebugAndroidTest` 確認 androidTest 能編譯，不使用模擬器；instrumented test 由 Task 12 Step 1 統一執行。

**Files:**
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/notebook/backup/BackupModels.kt`、`BackupCodec.kt`
- Create（測試資料，Task 9、10 共用）: `hymnchtv/src/test/java/org/cog/hymnchtv/notebook/backup/SampleTables.kt`
- Test: `hymnchtv/src/test/java/org/cog/hymnchtv/notebook/backup/BackupCodecTest.kt`

- [ ] **Step 1：先寫模型（純資料型別）** `backup/BackupModels.kt`

  ```kotlin
  package org.cog.hymnchtv.notebook.backup

  import org.cog.hymnchtv.notebook.data.entity.FavoriteEntity
  import org.cog.hymnchtv.notebook.data.entity.NoteEntity
  import org.cog.hymnchtv.notebook.data.entity.PlaylistEntity
  import org.cog.hymnchtv.notebook.data.entity.PlaylistItemEntity
  import org.cog.hymnchtv.notebook.data.entity.SingLogEntity

  /** All rows of the five notebook tables (including soft-deleted rows when used for backup). */
  data class NotebookTables(
      val favorites: List<FavoriteEntity> = emptyList(),
      val singLogs: List<SingLogEntity> = emptyList(),
      val notes: List<NoteEntity> = emptyList(),
      val playlists: List<PlaylistEntity> = emptyList(),
      val playlistItems: List<PlaylistItemEntity> = emptyList(),
  ) {
      val rowCount: Int
          get() = favorites.size + singLogs.size + notes.size + playlists.size + playlistItems.size

      companion object {
          @JvmField
          val EMPTY = NotebookTables()
      }
  }

  /** Contents of one backup file. [schemaVersion] versions the JSON layout, not the Room schema. */
  data class BackupSnapshot(
      val schemaVersion: Int,
      val exportedAt: Long,
      val appVersionName: String,
      val tables: NotebookTables,
  )

  /** Hard limits for untrusted backup files. */
  data class BackupLimits(
      val maxRowsPerTable: Int = 100_000,
      val maxNestingDepth: Int = 4,
      val maxFutureSkewMillis: Long = 24 * 60 * 60 * 1000L,
      val maxAppVersionLength: Int = 64,
  ) {
      init {
          require(maxRowsPerTable > 0 && maxNestingDepth > 0 && maxFutureSkewMillis >= 0 && maxAppVersionLength > 0)
      }
  }

  /** Rows dropped during decoding, by reason. */
  data class SkippedRows(val invalid: Int = 0, val futureTimestamp: Int = 0) {
      val total: Int
          get() = invalid + futureTimestamp

      operator fun plus(other: SkippedRows) =
          SkippedRows(invalid + other.invalid, futureTimestamp + other.futureTimestamp)

      companion object {
          @JvmField
          val NONE = SkippedRows()
      }
  }

  /** UI maps each value to a localized message (UI wave). */
  enum class BackupError { NOT_JSON, WRONG_FORMAT, UNSUPPORTED_VERSION, TOO_LARGE, IO, STORAGE }

  sealed interface DecodeResult {
      data class Success(val snapshot: BackupSnapshot, val skipped: SkippedRows) : DecodeResult
      data class Failure(val error: BackupError, val detail: String) : DecodeResult
  }

  data class MergeStats(val inserted: Int, val updated: Int, val unchanged: Int) {
      operator fun plus(other: MergeStats) =
          MergeStats(inserted + other.inserted, updated + other.updated, unchanged + other.unchanged)

      companion object {
          @JvmField
          val ZERO = MergeStats(0, 0, 0)
      }
  }

  /** [changes] holds only rows to upsert (inserted, updated, or re-slotted playlist items). */
  data class MergeResult(val changes: NotebookTables, val stats: MergeStats)

  sealed interface ExportResult {
      data class Success(val rowCount: Int) : ExportResult
      data class Failure(val error: BackupError, val detail: String) : ExportResult
  }

  sealed interface ImportResult {
      data class Success(val stats: MergeStats, val skipped: SkippedRows) : ImportResult
      data class Failure(val error: BackupError, val detail: String) : ImportResult
  }
  ```

- [ ] **Step 2：寫測試資料與會失敗的測試**

  `SampleTables.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.backup

  import org.cog.hymnchtv.notebook.data.entity.FavoriteEntity
  import org.cog.hymnchtv.notebook.data.entity.NoteEntity
  import org.cog.hymnchtv.notebook.data.entity.PlaylistEntity
  import org.cog.hymnchtv.notebook.data.entity.PlaylistItemEntity
  import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
  import org.cog.hymnchtv.notebook.fakes.testUuid
  import org.cog.hymnchtv.notebook.model.FavoriteIds
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.HymnTypes
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.cog.hymnchtv.notebook.model.SingSource
  import org.cog.hymnchtv.notebook.model.SyncRecord

  internal object SampleTables {
      const val DEVICE_A = "00000000-0000-0000-0000-00000000000a"
      const val DEVICE_B = "00000000-0000-0000-0000-00000000000b"
      val LOG_1 = testUuid(101)
      val LOG_2 = testUuid(102)
      val LOG_3 = testUuid(103)
      val NOTE_1 = testUuid(201)
      val NOTE_2 = testUuid(202)
      val PL_1 = testUuid(301)
      val IT_1 = testUuid(401)
      val IT_2 = testUuid(402)

      val db1 = HymnKey.of(HymnTypes.DB, 1)
      val fu1 = HymnKey.of(HymnTypes.DB, 781)
      val bb5 = HymnKey.of(HymnTypes.BB, 5)

      fun favorite(key: HymnKey, updatedAt: Long = 100, deletedAt: Long? = null, updatedBy: String = DEVICE_A) =
          FavoriteEntity(FavoriteIds.forKey(key), key, createdAt = 100, updatedAt = updatedAt, deletedAt = deletedAt, updatedBy = updatedBy)

      fun singLog(
          id: String,
          key: HymnKey = db1,
          sungAt: Long = 1_000,
          updatedAt: Long = 100,
          deletedAt: Long? = null,
          occasion: Occasion = Occasion.LORDS_DAY,
          source: SingSource = SingSource.MANUAL,
          playlistId: String? = null,
          updatedBy: String = DEVICE_A,
      ) = SingLogEntity(
          id = id, hymn = key, sungAt = sungAt, occasion = occasion, source = source, playlistId = playlistId,
          createdAt = 100, updatedAt = updatedAt, deletedAt = deletedAt, updatedBy = updatedBy,
      )

      fun note(
          id: String,
          key: HymnKey = db1,
          body: String = "主啊，我愛你\n\"引號\" / \\ 反斜線",
          singLogId: String? = null,
          updatedAt: Long = 100,
          deletedAt: Long? = null,
          updatedBy: String = DEVICE_A,
      ) = NoteEntity(id, key, body, singLogId, createdAt = 100, updatedAt = updatedAt, deletedAt = deletedAt, updatedBy = updatedBy)

      fun playlist(id: String, name: String = "主日 10/4", updatedAt: Long = 100, deletedAt: Long? = null) =
          PlaylistEntity(id, name, createdAt = 100, updatedAt = updatedAt, deletedAt = deletedAt, updatedBy = DEVICE_A)

      fun item(
          id: String,
          playlistId: String,
          position: Int,
          key: HymnKey = db1,
          updatedAt: Long = 100,
          deletedAt: Long? = null,
          updatedBy: String = DEVICE_A,
      ) = PlaylistItemEntity(id, playlistId, position, key, createdAt = 100, updatedAt = updatedAt, deletedAt = deletedAt, updatedBy = updatedBy)

      /** 10 rows: every column type, nulls, soft-deleted rows, CJK and escape-worthy text. */
      fun full() = NotebookTables(
          favorites = listOf(favorite(db1), favorite(bb5, updatedAt = 200, deletedAt = 200)),
          singLogs = listOf(
              singLog(LOG_1),
              singLog(LOG_2, key = fu1, occasion = Occasion.SMALL_GROUP, source = SingSource.AUTO, playlistId = PL_1),
              singLog(LOG_3, updatedAt = 300, deletedAt = 300),
          ),
          notes = listOf(note(NOTE_1, singLogId = LOG_1), note(NOTE_2, key = fu1)),
          playlists = listOf(playlist(PL_1)),
          playlistItems = listOf(item(IT_1, PL_1, 0), item(IT_2, PL_1, 1, key = fu1)),
      )

      /** Applies upserts by id, the same way BackupStore implementations do. */
      fun NotebookTables.upserted(changes: NotebookTables) = NotebookTables(
          upsertRows(favorites, changes.favorites),
          upsertRows(singLogs, changes.singLogs),
          upsertRows(notes, changes.notes),
          upsertRows(playlists, changes.playlists),
          upsertRows(playlistItems, changes.playlistItems),
      )

      private fun <T : SyncRecord> upsertRows(base: List<T>, changes: List<T>): List<T> =
          (base.associateBy { it.id } + changes.associateBy { it.id }).values.toList()
  }
  ```

  `BackupCodecTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.backup

  import com.google.common.truth.Truth.assertThat
  import org.cog.hymnchtv.notebook.backup.SampleTables.DEVICE_A
  import org.cog.hymnchtv.notebook.fakes.testUuid
  import org.cog.hymnchtv.notebook.model.FavoriteIds
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.cog.hymnchtv.notebook.model.SingSource
  import org.json.JSONObject
  import org.junit.Test

  class BackupCodecTest {
      private val now = 1_790_733_600_000L
      private val day = 86_400_000L
      private val favDb1 = FavoriteIds.forKey(SampleTables.db1)

      private fun snapshot(tables: NotebookTables = SampleTables.full()) =
          BackupSnapshot(BackupCodec.CURRENT_SCHEMA_VERSION, now, "2.9.2", tables)

      private fun decode(text: String, limits: BackupLimits = BackupLimits()) = BackupCodec.decode(text, now, limits)

      private fun decodeOk(text: String): DecodeResult.Success {
          val result = decode(text)
          assertThat(result).isInstanceOf(DecodeResult.Success::class.java)
          return result as DecodeResult.Success
      }

      private fun failureOf(text: String, limits: BackupLimits = BackupLimits()): BackupError? =
          (decode(text, limits) as? DecodeResult.Failure)?.error

      private fun doc(vararg tables: Pair<String, String>, header: String = "\"exportedAt\":1,\"appVersionName\":\"t\"") =
          buildString {
              append("{\"format\":\"hymnchtv-notebook\",\"schemaVersion\":1,").append(header)
              tables.forEach { (name, json) -> append(",\"$name\":$json") }
              append("}")
          }

      private fun favJson(id: String, type: String = "hymn_db", no: Int = 1, isFu: Boolean = false) =
          """{"id":"$id","hymnType":"$type","hymnNo":$no,"isFu":$isFu,"createdAt":1,"updatedAt":1,"deletedAt":null,"updatedBy":"$DEVICE_A"}"""

      private fun logJson(
          id: String = testUuid(11),
          occasion: String = "HOME",
          source: String = "AUTO",
          sungAt: String = "\"sungAt\":5,",
          updatedAt: Long = 1,
          playlistId: String = "null",
          updatedBy: String = "\"updatedBy\":\"$DEVICE_A\",",
      ) = """{"id":"$id","hymnType":"hymn_db","hymnNo":1,"isFu":false,$sungAt$updatedBy"occasion":"$occasion",""" +
          """"source":"$source","playlistId":$playlistId,"createdAt":1,"updatedAt":$updatedAt,"deletedAt":null}"""

      private fun noteJson(body: String) =
          """{"id":"${testUuid(21)}","hymnType":"hymn_db","hymnNo":1,"isFu":false,"body":"$body","singLogId":null,""" +
              """"createdAt":1,"updatedAt":1,"deletedAt":null,"updatedBy":"$DEVICE_A"}"""

      @Test
      fun roundTripKeepsEveryRowAndField() {
          val original = snapshot()
          val decoded = decodeOk(BackupCodec.encode(original))
          assertThat(decoded.snapshot).isEqualTo(original)
          assertThat(decoded.skipped).isEqualTo(SkippedRows.NONE)
      }

      @Test
      fun emptyTablesRoundTrip() {
          val original = snapshot(NotebookTables.EMPTY)
          assertThat(decodeOk(BackupCodec.encode(original)).snapshot).isEqualTo(original)
      }

      @Test
      fun headerNullsAndUpdatedByAreWrittenExplicitly() {
          val json = JSONObject(BackupCodec.encode(snapshot()))
          assertThat(json.getString("format")).isEqualTo("hymnchtv-notebook")
          assertThat(json.getInt("schemaVersion")).isEqualTo(1)
          assertThat(json.getLong("exportedAt")).isEqualTo(now)
          assertThat(json.getString("appVersionName")).isEqualTo("2.9.2")
          val favorite = json.getJSONArray("favorites").getJSONObject(0)
          assertThat(favorite.has("deletedAt")).isTrue()
          assertThat(favorite.isNull("deletedAt")).isTrue()
          assertThat(favorite.getString("updatedBy")).isEqualTo(DEVICE_A)
      }

      @Test
      fun favoriteIdMustEqualTheDerivedId() {
          assertThat(decodeOk(doc("favorites" to "[${favJson(favDb1)}]")).snapshot.tables.favorites).hasSize(1)
          val foreign = decodeOk(doc("favorites" to "[${favJson(testUuid(5))}]"))
          assertThat(foreign.snapshot.tables.favorites).isEmpty()
          assertThat(foreign.skipped).isEqualTo(SkippedRows(invalid = 1))
      }

      @Test
      fun maliciousIdsAreSkipped() {
          val ids = listOf(testUuid(0xab).uppercase(), "1-1-1-1-1", "../../etc/passwd", "", "x".repeat(300))
          val logs = ids.map { logJson(id = it) } + logJson(id = testUuid(2), playlistId = "\"nope\"")
          val decoded = decodeOk(doc("singLogs" to logs.joinToString(",", "[", "]")))
          assertThat(decoded.snapshot.tables.singLogs).isEmpty()
          assertThat(decoded.skipped).isEqualTo(SkippedRows(invalid = 6))
      }

      @Test
      fun nonCanonicalHymnKeysAreSkipped() {
          val rows = listOf(
              favJson(testUuid(1), no = 781, isFu = false),
              favJson(testUuid(2), type = "hymn_bb", no = 50),
              favJson(testUuid(3), no = 787, isFu = true),
              favJson(testUuid(4), type = "x"),
          )
          val decoded = decodeOk(doc("favorites" to rows.joinToString(",", "[", "]")))
          assertThat(decoded.skipped).isEqualTo(SkippedRows(invalid = 4))
      }

      @Test
      fun futureTimestampsAreSkippedSeparately() {
          val rows = listOf(
              logJson(id = testUuid(1), updatedAt = now + day),
              logJson(id = testUuid(2), updatedAt = now + day + 1),
              logJson(id = testUuid(3), sungAt = "\"sungAt\":${now + day + 1},"),
          )
          val decoded = decodeOk(doc("singLogs" to rows.joinToString(",", "[", "]")))
          assertThat(decoded.snapshot.tables.singLogs.map { it.id }).containsExactly(testUuid(1))
          assertThat(decoded.skipped).isEqualTo(SkippedRows(invalid = 0, futureTimestamp = 2))
      }

      @Test
      fun unknownEnumsDegrade() {
          val log = decodeOk(doc("singLogs" to "[${logJson(occasion = "SUNDAY_SERVICE", source = "BOT")}]"))
              .snapshot.tables.singLogs.single()
          assertThat(log.occasion).isEqualTo(Occasion.OTHER)
          assertThat(log.source).isEqualTo(SingSource.MANUAL)
      }

      @Test
      fun invalidRowsAreSkippedAndCounted() {
          val decoded = decodeOk(
              doc(
                  "singLogs" to "[${logJson(id = testUuid(1), sungAt = "")},${logJson(id = testUuid(2), updatedBy = "")}]",
                  "notes" to "[${noteJson("")},${noteJson("字".repeat(100_001))}]",
                  "playlists" to """[{"id":"${testUuid(31)}","name":" ","createdAt":1,"updatedAt":1,"deletedAt":null,"updatedBy":"$DEVICE_A"}]""",
              ),
          )
          assertThat(decoded.skipped).isEqualTo(SkippedRows(invalid = 5))
          assertThat(decoded.snapshot.tables.rowCount).isEqualTo(0)
      }

      @Test
      fun missingTablesAreEmpty() {
          assertThat(decodeOk(doc()).snapshot.tables).isEqualTo(NotebookTables.EMPTY)
      }

      @Test
      fun wrongTypedTableRejectsTheFile() {
          assertThat(failureOf(doc("favorites" to "{}"))).isEqualTo(BackupError.WRONG_FORMAT)
          assertThat(failureOf(doc("notes" to "\"x\""))).isEqualTo(BackupError.WRONG_FORMAT)
      }

      @Test
      fun headerIsValidated() {
          listOf(
              "\"exportedAt\":\"yesterday\",\"appVersionName\":\"t\"",
              "\"appVersionName\":\"t\"",
              "\"exportedAt\":-1,\"appVersionName\":\"t\"",
              "\"exportedAt\":1,\"appVersionName\":\"${"v".repeat(65)}\"",
              "\"exportedAt\":1,\"appVersionName\":5",
          ).forEach { assertThat(failureOf(doc(header = it))).isEqualTo(BackupError.WRONG_FORMAT) }
          assertThat(failureOf("""{"format":"hymnchtv-notebook","schemaVersion":"1","exportedAt":1,"appVersionName":"t"}"""))
              .isEqualTo(BackupError.WRONG_FORMAT)
      }

      @Test
      fun tooManyRowsRejectsTheFile() {
          val rows = (1..3).joinToString(",", "[", "]") { logJson(id = testUuid(it)) }
          assertThat(failureOf(doc("singLogs" to rows), BackupLimits(maxRowsPerTable = 2))).isEqualTo(BackupError.TOO_LARGE)
      }

      @Test
      fun deepNestingIsRejectedBeforeParsing() {
          assertThat(failureOf("[".repeat(100_000))).isEqualTo(BackupError.WRONG_FORMAT)
          assertThat(failureOf(doc("extra" to "[[[[1]]]]"))).isEqualTo(BackupError.WRONG_FORMAT)
          assertThat(decodeOk(doc("notes" to "[${noteJson("[[[[[{{{ brackets in text are fine")}]")).skipped)
              .isEqualTo(SkippedRows.NONE)
      }

      @Test
      fun byteOrderMarkIsIgnored() {
          assertThat(decodeOk("﻿" + BackupCodec.encode(snapshot())).snapshot).isEqualTo(snapshot())
      }

      @Test
      fun rejectsNonJson() {
          listOf("hello", "", "[1,2]").forEach { assertThat(failureOf(it)).isEqualTo(BackupError.NOT_JSON) }
      }

      @Test
      fun rejectsOtherFormats() {
          assertThat(failureOf("""{"format":"x","schemaVersion":1}""")).isEqualTo(BackupError.WRONG_FORMAT)
          assertThat(failureOf("""{"format":"hymnchtv-notebook"}""")).isEqualTo(BackupError.WRONG_FORMAT)
      }

      @Test
      fun rejectsNewerSchema() {
          assertThat(failureOf("""{"format":"hymnchtv-notebook","schemaVersion":2}"""))
              .isEqualTo(BackupError.UNSUPPORTED_VERSION)
      }
  }
  ```

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.notebook.backup.*'`
  Expected: 編譯失敗，`Unresolved reference 'BackupCodec'`。

- [ ] **Step 3：實作** `backup/BackupCodec.kt`

  ```kotlin
  package org.cog.hymnchtv.notebook.backup

  import org.cog.hymnchtv.notebook.data.entity.FavoriteEntity
  import org.cog.hymnchtv.notebook.data.entity.NoteEntity
  import org.cog.hymnchtv.notebook.data.entity.PlaylistEntity
  import org.cog.hymnchtv.notebook.data.entity.PlaylistItemEntity
  import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
  import org.cog.hymnchtv.notebook.model.FavoriteIds
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.NotebookValidation
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.cog.hymnchtv.notebook.model.SingSource
  import org.cog.hymnchtv.notebook.model.SyncRecord
  import org.json.JSONArray
  import org.json.JSONException
  import org.json.JSONObject

  /**
   * Versioned JSON backup format. Encoding writes every column, nulls explicitly. Decoding treats the file as
   * untrusted and never throws: file-level problems return Failure; invalid rows are skipped and counted by reason.
   */
  object BackupCodec {
      const val FORMAT = "hymnchtv-notebook"
      const val CURRENT_SCHEMA_VERSION = 1

      private const val KEY_FORMAT = "format"
      private const val KEY_SCHEMA_VERSION = "schemaVersion"
      private const val KEY_EXPORTED_AT = "exportedAt"
      private const val KEY_APP_VERSION = "appVersionName"
      private const val FAVORITES = "favorites"
      private const val SING_LOGS = "singLogs"
      private const val NOTES = "notes"
      private const val PLAYLISTS = "playlists"
      private const val PLAYLIST_ITEMS = "playlistItems"
      private val TABLES = listOf(FAVORITES, SING_LOGS, NOTES, PLAYLISTS, PLAYLIST_ITEMS)
      private const val BOM = "﻿"

      fun encode(snapshot: BackupSnapshot): String {
          val tables = snapshot.tables
          return JSONObject()
              .put(KEY_FORMAT, FORMAT)
              .put(KEY_SCHEMA_VERSION, snapshot.schemaVersion)
              .put(KEY_EXPORTED_AT, snapshot.exportedAt)
              .put(KEY_APP_VERSION, snapshot.appVersionName)
              .put(FAVORITES, JSONArray(tables.favorites.map(::favoriteToJson)))
              .put(SING_LOGS, JSONArray(tables.singLogs.map(::singLogToJson)))
              .put(NOTES, JSONArray(tables.notes.map(::noteToJson)))
              .put(PLAYLISTS, JSONArray(tables.playlists.map(::playlistToJson)))
              .put(PLAYLIST_ITEMS, JSONArray(tables.playlistItems.map(::playlistItemToJson)))
              .toString(2)
      }

      /** [nowMillis] is the importing device's time; rows later than now + maxFutureSkewMillis are skipped. */
      fun decode(text: String, nowMillis: Long, limits: BackupLimits = BackupLimits()): DecodeResult {
          val body = text.removePrefix(BOM)
          if (nestingDepth(body, limits.maxNestingDepth) > limits.maxNestingDepth) {
              return DecodeResult.Failure(BackupError.WRONG_FORMAT, "Nesting deeper than ${limits.maxNestingDepth}")
          }
          val root = try {
              JSONObject(body)
          } catch (e: JSONException) {
              return DecodeResult.Failure(BackupError.NOT_JSON, e.message.orEmpty())
          }
          headerFailure(root, limits)?.let { return it }

          val arrays = TABLES.associateWith { tableArray(root, it, limits) }
          arrays.values.firstNotNullOfOrNull { it.failure }?.let { return it }
          val maxTime = nowMillis + limits.maxFutureSkewMillis
          val favorites = parseRows(arrays.getValue(FAVORITES).array) { favoriteFromJson(it, maxTime) }
          val singLogs = parseRows(arrays.getValue(SING_LOGS).array) { singLogFromJson(it, maxTime) }
          val notes = parseRows(arrays.getValue(NOTES).array) { noteFromJson(it, maxTime) }
          val playlists = parseRows(arrays.getValue(PLAYLISTS).array) { playlistFromJson(it, maxTime) }
          val items = parseRows(arrays.getValue(PLAYLIST_ITEMS).array) { playlistItemFromJson(it, maxTime) }

          val snapshot = BackupSnapshot(
              schemaVersion = root.getInt(KEY_SCHEMA_VERSION),
              exportedAt = root.getLong(KEY_EXPORTED_AT),
              appVersionName = root.getString(KEY_APP_VERSION),
              tables = NotebookTables(favorites.rows, singLogs.rows, notes.rows, playlists.rows, items.rows),
          )
          val skipped = favorites.skipped + singLogs.skipped + notes.skipped + playlists.skipped + items.skipped
          return DecodeResult.Success(snapshot, skipped)
      }

      // ---- file-level validation ----

      private fun headerFailure(root: JSONObject, limits: BackupLimits): DecodeResult.Failure? {
          if (root.optString(KEY_FORMAT) != FORMAT) {
              return DecodeResult.Failure(BackupError.WRONG_FORMAT, "format=${root.opt(KEY_FORMAT)}")
          }
          val version = root.opt(KEY_SCHEMA_VERSION)
          if (version !is Int || version < 1) return DecodeResult.Failure(BackupError.WRONG_FORMAT, "schemaVersion=$version")
          if (version > CURRENT_SCHEMA_VERSION) {
              return DecodeResult.Failure(BackupError.UNSUPPORTED_VERSION, "schemaVersion=$version")
          }
          val exportedAt = root.opt(KEY_EXPORTED_AT)
          if (exportedAt !is Number || exportedAt.toLong() < 0) {
              return DecodeResult.Failure(BackupError.WRONG_FORMAT, "exportedAt=$exportedAt")
          }
          val appVersion = root.opt(KEY_APP_VERSION)
          if (appVersion !is String || appVersion.length > limits.maxAppVersionLength) {
              return DecodeResult.Failure(BackupError.WRONG_FORMAT, "appVersionName is not a short string")
          }
          return null
      }

      private class TableArray(val array: JSONArray?, val failure: DecodeResult.Failure?)

      private fun tableArray(root: JSONObject, name: String, limits: BackupLimits): TableArray =
          when (val value = root.opt(name)) {
              null, JSONObject.NULL -> TableArray(null, null)
              is JSONArray ->
                  if (value.length() > limits.maxRowsPerTable) {
                      TableArray(null, DecodeResult.Failure(BackupError.TOO_LARGE, "$name has ${value.length()} rows"))
                  } else {
                      TableArray(value, null)
                  }
              else -> TableArray(null, DecodeResult.Failure(BackupError.WRONG_FORMAT, "$name is not an array"))
          }

      /** Max object/array nesting outside strings; stops counting once [limit] is exceeded. */
      internal fun nestingDepth(text: String, limit: Int): Int {
          var depth = 0
          var max = 0
          var inString = false
          var escaped = false
          for (c in text) {
              if (inString) {
                  when {
                      escaped -> escaped = false
                      c == '\\' -> escaped = true
                      c == '"' -> inString = false
                  }
              } else {
                  when (c) {
                      '"' -> inString = true
                      '{', '[' -> {
                          depth++
                          if (depth > max) max = depth
                          if (max > limit) return max
                      }
                      '}', ']' -> depth--
                  }
              }
          }
          return max
      }

      // ---- row-level parsing ----

      private class FutureTimestampException : IllegalArgumentException("Timestamp too far in the future")

      private class RowResult<T>(val row: T?, val future: Boolean)

      private class Parsed<T>(val rows: List<T>, val skipped: SkippedRows)

      private fun <T : Any> parseRows(array: JSONArray?, parse: (JSONObject) -> T): Parsed<T> {
          if (array == null) return Parsed(emptyList(), SkippedRows.NONE)
          val results = (0 until array.length()).map { index -> parseRow { parse(array.getJSONObject(index)) } }
          return Parsed(
              rows = results.mapNotNull { it.row },
              skipped = SkippedRows(
                  invalid = results.count { it.row == null && !it.future },
                  futureTimestamp = results.count { it.future },
              ),
          )
      }

      private inline fun <T : Any> parseRow(parse: () -> T): RowResult<T> = try {
          RowResult(parse(), future = false)
      } catch (e: FutureTimestampException) {
          RowResult(null, future = true)
      } catch (e: JSONException) {
          RowResult(null, future = false)
      } catch (e: IllegalArgumentException) {
          RowResult(null, future = false)
      }

      private fun time(value: Long, maxTime: Long): Long {
          require(value >= 0) { "Negative timestamp" }
          if (value > maxTime) throw FutureTimestampException()
          return value
      }

      private class Sync(val id: String, val createdAt: Long, val updatedAt: Long, val deletedAt: Long?, val updatedBy: String)

      private fun JSONObject.sync(maxTime: Long): Sync = Sync(
          id = getString("id"),
          createdAt = time(getLong("createdAt"), maxTime),
          updatedAt = time(getLong("updatedAt"), maxTime),
          deletedAt = nullableLong("deletedAt")?.let { time(it, maxTime) },
          updatedBy = NotebookValidation.uuid(getString("updatedBy")),
      )

      private fun JSONObject.hymn() = HymnKey(getString("hymnType"), getInt("hymnNo"), getBoolean("isFu"))

      private fun JSONObject.nullableLong(name: String): Long? = if (isNull(name)) null else getLong(name)

      private fun JSONObject.optionalUuid(name: String): String? =
          if (isNull(name)) null else NotebookValidation.uuid(getString(name))

      private fun favoriteFromJson(json: JSONObject, maxTime: Long): FavoriteEntity {
          val sync = json.sync(maxTime)
          val hymn = json.hymn()
          // The id is derived from the hymn; a different id would break the unique hymn index and cross-device merges.
          require(sync.id == FavoriteIds.forKey(hymn)) { "Favorite id does not match its hymn" }
          return FavoriteEntity(sync.id, hymn, sync.createdAt, sync.updatedAt, sync.deletedAt, sync.updatedBy)
      }

      private fun singLogFromJson(json: JSONObject, maxTime: Long): SingLogEntity {
          val sync = json.sync(maxTime)
          return SingLogEntity(
              id = NotebookValidation.uuid(sync.id),
              hymn = json.hymn(),
              sungAt = time(json.getLong("sungAt"), maxTime),
              occasion = Occasion.fromStorage(json.optString("occasion")) ?: Occasion.OTHER,
              source = SingSource.fromStorage(json.optString("source")) ?: SingSource.MANUAL,
              playlistId = json.optionalUuid("playlistId"),
              createdAt = sync.createdAt,
              updatedAt = sync.updatedAt,
              deletedAt = sync.deletedAt,
              updatedBy = sync.updatedBy,
          )
      }

      private fun noteFromJson(json: JSONObject, maxTime: Long): NoteEntity {
          val sync = json.sync(maxTime)
          return NoteEntity(
              id = NotebookValidation.uuid(sync.id),
              hymn = json.hymn(),
              body = NotebookValidation.noteBody(json.getString("body")),
              singLogId = json.optionalUuid("singLogId"),
              createdAt = sync.createdAt,
              updatedAt = sync.updatedAt,
              deletedAt = sync.deletedAt,
              updatedBy = sync.updatedBy,
          )
      }

      private fun playlistFromJson(json: JSONObject, maxTime: Long): PlaylistEntity {
          val sync = json.sync(maxTime)
          return PlaylistEntity(
              id = NotebookValidation.uuid(sync.id),
              name = NotebookValidation.playlistName(json.getString("name")),
              createdAt = sync.createdAt,
              updatedAt = sync.updatedAt,
              deletedAt = sync.deletedAt,
              updatedBy = sync.updatedBy,
          )
      }

      private fun playlistItemFromJson(json: JSONObject, maxTime: Long): PlaylistItemEntity {
          val sync = json.sync(maxTime)
          val position = json.getInt("position")
          require(position >= 0) { "Negative position" }
          return PlaylistItemEntity(
              id = NotebookValidation.uuid(sync.id),
              playlistId = NotebookValidation.uuid(json.getString("playlistId")),
              position = position,
              hymn = json.hymn(),
              createdAt = sync.createdAt,
              updatedAt = sync.updatedAt,
              deletedAt = sync.deletedAt,
              updatedBy = sync.updatedBy,
          )
      }

      // ---- encoding ----

      private fun JSONObject.putSync(row: SyncRecord): JSONObject = put("id", row.id)
          .put("createdAt", row.createdAt)
          .put("updatedAt", row.updatedAt)
          .put("deletedAt", row.deletedAt ?: JSONObject.NULL)
          .put("updatedBy", row.updatedBy)

      private fun JSONObject.putHymn(key: HymnKey): JSONObject =
          put("hymnType", key.hymnType).put("hymnNo", key.hymnNo).put("isFu", key.isFu)

      private fun favoriteToJson(row: FavoriteEntity) = JSONObject().putSync(row).putHymn(row.hymn)

      private fun singLogToJson(row: SingLogEntity) = JSONObject().putSync(row).putHymn(row.hymn)
          .put("sungAt", row.sungAt)
          .put("occasion", row.occasion.name)
          .put("source", row.source.name)
          .put("playlistId", row.playlistId ?: JSONObject.NULL)

      private fun noteToJson(row: NoteEntity) = JSONObject().putSync(row).putHymn(row.hymn)
          .put("body", row.body)
          .put("singLogId", row.singLogId ?: JSONObject.NULL)

      private fun playlistToJson(row: PlaylistEntity) = JSONObject().putSync(row).put("name", row.name)

      private fun playlistItemToJson(row: PlaylistItemEntity) = JSONObject().putSync(row)
          .put("playlistId", row.playlistId)
          .put("position", row.position)
          .putHymn(row.hymn)
  }
  ```

- [ ] **Step 4：執行測試，確認它通過**

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.notebook.backup.*'`
  Expected: 18 個測試全部通過。

- [ ] **Step 5：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/notebook/backup hymnchtv/src/test/java/org/cog/hymnchtv/notebook/backup
  git commit -m "feat: add hardened versioned JSON codec for notebook backups" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

### Task 9：BackupMerger（線性，`feat/notebook-data`）

**Files:**
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/notebook/backup/BackupMerger.kt`
- Test: `hymnchtv/src/test/java/org/cog/hymnchtv/notebook/backup/BackupMergerTest.kt`

- [ ] **Step 1：寫會失敗的測試**

  ```kotlin
  package org.cog.hymnchtv.notebook.backup

  import com.google.common.truth.Truth.assertThat
  import org.cog.hymnchtv.notebook.backup.SampleTables.DEVICE_A
  import org.cog.hymnchtv.notebook.backup.SampleTables.DEVICE_B
  import org.cog.hymnchtv.notebook.backup.SampleTables.upserted
  import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
  import org.cog.hymnchtv.notebook.fakes.testUuid
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.cog.hymnchtv.notebook.model.SingSource
  import org.junit.Test

  class BackupMergerTest {
      private val a = testUuid(1)
      private val b = testUuid(2)
      private val c = testUuid(3)
      private val pl = testUuid(9)

      private fun log(id: String, updatedAt: Long, deletedAt: Long? = null, sungAt: Long = 1, by: String = DEVICE_A) =
          SampleTables.singLog(id, sungAt = sungAt, updatedAt = updatedAt, deletedAt = deletedAt, updatedBy = by)

      /** The row that ends up stored when [incoming] is merged into a notebook holding [local]. */
      private fun winner(local: SingLogEntity, incoming: SingLogEntity): SingLogEntity =
          BackupMerger.mergeTable(listOf(local), listOf(incoming)).changes.singleOrNull() ?: local

      @Test
      fun insertsEverythingIntoAnEmptyNotebook() {
          val full = SampleTables.full()
          val result = BackupMerger.merge(NotebookTables.EMPTY, full)
          assertThat(result.changes).isEqualTo(full)
          assertThat(result.stats).isEqualTo(MergeStats(10, 0, 0))
      }

      @Test
      fun newerIncomingWins() {
          val incoming = log(a, updatedAt = 2, sungAt = 9)
          val merge = BackupMerger.mergeTable(listOf(log(a, 1)), listOf(incoming))
          assertThat(merge.changes).containsExactly(incoming)
          assertThat(merge.stats).isEqualTo(MergeStats(0, 1, 0))
      }

      @Test
      fun olderIncomingIsIgnored() {
          val merge = BackupMerger.mergeTable(listOf(log(a, 5)), listOf(log(a, 1, sungAt = 9)))
          assertThat(merge.changes).isEmpty()
          assertThat(merge.stats).isEqualTo(MergeStats(0, 0, 1))
      }

      @Test
      fun identicalRowIsUnchanged() {
          val merge = BackupMerger.mergeTable(listOf(log(a, 5)), listOf(log(a, 5)))
          assertThat(merge.changes).isEmpty()
          assertThat(merge.stats).isEqualTo(MergeStats(0, 0, 1))
      }

      @Test
      fun onEqualTimeADeleteWinsFromEitherSide() {
          val active = log(a, 5)
          val deleted = log(a, 5, deletedAt = 5)
          assertThat(winner(local = active, incoming = deleted)).isEqualTo(deleted)
          assertThat(winner(local = deleted, incoming = active)).isEqualTo(deleted)
      }

      @Test
      fun onEqualTimeTheLargerDeviceIdWins() {
          val fromA = log(a, 5, sungAt = 1, by = DEVICE_A)
          val fromB = log(a, 5, sungAt = 2, by = DEVICE_B)
          assertThat(winner(local = fromA, incoming = fromB)).isEqualTo(fromB)
          assertThat(winner(local = fromB, incoming = fromA)).isEqualTo(fromB)
      }

      @Test
      fun onEqualTimeAndDeviceTheResultStillConverges() {
          val x = log(a, 5, sungAt = 1)
          val y = log(a, 5, sungAt = 2)
          assertThat(winner(local = x, incoming = y)).isEqualTo(winner(local = y, incoming = x))
      }

      @Test
      fun deletedAtLaterThanUpdatedAtCounts() {
          val incoming = log(a, updatedAt = 3, deletedAt = 12)
          assertThat(BackupMerger.mergeTable(listOf(log(a, 10)), listOf(incoming)).changes).containsExactly(incoming)
      }

      @Test
      fun duplicateIdsInTheFileKeepTheNewest() {
          val merge = BackupMerger.mergeTable(emptyList(), listOf(log(a, 1), log(a, 7), log(a, 3)))
          assertThat(merge.changes.single().updatedAt).isEqualTo(7L)
          assertThat(merge.stats).isEqualTo(MergeStats(1, 0, 0))
      }

      @Test
      fun mergingTheSameFileTwiceIsIdempotent() {
          val local = NotebookTables(singLogs = listOf(log(SampleTables.LOG_1, updatedAt = 50), log(c, 1)))
          val incoming = SampleTables.full()
          val first = BackupMerger.merge(local, incoming)
          val second = BackupMerger.merge(local.upserted(first.changes), incoming)
          assertThat(second.changes.rowCount).isEqualTo(0)
          assertThat(second.stats).isEqualTo(MergeStats(0, 0, incoming.rowCount))
      }

      @Test
      fun statsAddUpAcrossTables() {
          val local = NotebookTables(
              favorites = listOf(SampleTables.favorite(SampleTables.db1, updatedAt = 999)),
              notes = listOf(SampleTables.note(SampleTables.NOTE_1, updatedAt = 1)),
          )
          val stats = BackupMerger.merge(local, SampleTables.full()).stats
          assertThat(stats).isEqualTo(MergeStats(inserted = 8, updated = 1, unchanged = 1))
      }

      @Test
      fun itemsAppendedToTheSameSlotOnTwoDevicesAreReslotted() {
          val local = NotebookTables(playlistItems = listOf(SampleTables.item(a, pl, 0)))
          val incoming = NotebookTables(playlistItems = listOf(SampleTables.item(b, pl, 0, updatedAt = 200)))
          val changes = BackupMerger.merge(local, incoming).changes.playlistItems
          assertThat(changes.map { it.id to it.position }).containsExactly(b to 1)
      }

      @Test
      fun aRowMovedToAFreeSlotKeepsIt() {
          val local = NotebookTables(playlistItems = listOf(SampleTables.item(a, pl, 0)))
          val incoming = NotebookTables(playlistItems = listOf(SampleTables.item(a, pl, 5, updatedAt = 200)))
          assertThat(BackupMerger.merge(local, incoming).changes.playlistItems.single().position).isEqualTo(5)
      }

      @Test
      fun softDeletedLocalRowsStillOccupyTheirSlot() {
          val local = NotebookTables(playlistItems = listOf(SampleTables.item(a, pl, 0, deletedAt = 150)))
          val incoming = NotebookTables(playlistItems = listOf(SampleTables.item(c, pl, 0)))
          assertThat(BackupMerger.merge(local, incoming).changes.playlistItems.single().position).isEqualTo(1)
      }

      @Test
      fun canonicalContentIsPinned() {
          val log = SampleTables.singLog(
              SampleTables.LOG_2, key = SampleTables.fu1, occasion = Occasion.SMALL_GROUP, source = SingSource.AUTO,
              playlistId = SampleTables.PL_1,
          )
          assertThat(BackupMerger.canonicalContent(log))
              .isEqualTo("7:hymn_db|3:781|4:true|4:1000|11:SMALL_GROUP|4:AUTO|36:00000000-0000-0000-0000-00000000012d|3:100")
          assertThat(BackupMerger.canonicalContent(SampleTables.singLog(SampleTables.LOG_1)))
              .isEqualTo("7:hymn_db|1:1|5:false|4:1000|9:LORDS_DAY|6:MANUAL|~|3:100")
          assertThat(BackupMerger.canonicalContent(SampleTables.playlist(SampleTables.PL_1, name = "a|b")))
              .isEqualTo("3:a|b|3:100")
      }

      @Test
      fun finalTieBreakUsesCanonicalContent() {
          val later = SampleTables.note(SampleTables.NOTE_1, body = "b-body")
          val earlier = SampleTables.note(SampleTables.NOTE_1, body = "a-body")
          assertThat(BackupMerger.compare(later, earlier)).isGreaterThan(0)
          assertThat(BackupMerger.mergeTable(listOf(earlier), listOf(later)).changes).containsExactly(later)
          assertThat(BackupMerger.mergeTable(listOf(later), listOf(earlier)).changes).isEmpty()
      }

      @Test
      fun swappedItemsKeepTheirImportedSlots() {
          val local = NotebookTables(playlistItems = listOf(SampleTables.item(a, pl, 0), SampleTables.item(b, pl, 1)))
          val incoming = NotebookTables(
              playlistItems = listOf(SampleTables.item(a, pl, 1, updatedAt = 200), SampleTables.item(b, pl, 0, updatedAt = 200)),
          )
          val changes = BackupMerger.merge(local, incoming).changes.playlistItems
          assertThat(changes.associate { it.id to it.position }).containsExactly(a, 1, b, 0)
      }

      @Test
      fun reslottingIsDeterministicByPositionThenId() {
          val local = NotebookTables(playlistItems = listOf(SampleTables.item(a, pl, 0)))
          val incoming = NotebookTables(playlistItems = listOf(SampleTables.item(c, pl, 0), SampleTables.item(b, pl, 0)))
          val changes = BackupMerger.merge(local, incoming).changes.playlistItems
          assertThat(changes.associate { it.id to it.position }).containsExactly(b, 1, c, 2)
      }
  }
  ```

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.notebook.backup.BackupMergerTest'`
  Expected: 編譯失敗，`Unresolved reference 'BackupMerger'`。

- [ ] **Step 2：實作** `backup/BackupMerger.kt`

  ```kotlin
  package org.cog.hymnchtv.notebook.backup

  import org.cog.hymnchtv.notebook.data.entity.FavoriteEntity
  import org.cog.hymnchtv.notebook.data.entity.NoteEntity
  import org.cog.hymnchtv.notebook.data.entity.PlaylistEntity
  import org.cog.hymnchtv.notebook.data.entity.PlaylistItemEntity
  import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.SyncRecord
  import org.cog.hymnchtv.notebook.model.version

  /**
   * Deterministic last-writer-wins (same rule as future sync until S moves to server versions).
   * Order: version = max(updatedAt, deletedAt), then deleted over active, then larger updatedBy, then larger
   * canonicalContent(). It is a total order, so two devices importing each other's files converge. Only rows to upsert
   * are returned; playlist items are re-slotted so (playlistId, position) stays unique.
   */
  object BackupMerger {
      fun merge(local: NotebookTables, incoming: NotebookTables): MergeResult {
          val favorites = mergeTable(local.favorites, incoming.favorites)
          val singLogs = mergeTable(local.singLogs, incoming.singLogs)
          val notes = mergeTable(local.notes, incoming.notes)
          val playlists = mergeTable(local.playlists, incoming.playlists)
          val items = mergeTable(local.playlistItems, incoming.playlistItems)
          return MergeResult(
              changes = NotebookTables(
                  favorites.changes, singLogs.changes, notes.changes, playlists.changes,
                  resolvePositionCollisions(local.playlistItems, items.changes),
              ),
              stats = favorites.stats + singLogs.stats + notes.stats + playlists.stats + items.stats,
          )
      }

      @JvmStatic
      fun <T : SyncRecord> compare(a: T, b: T): Int =
          compareValuesBy(a, b, { it.version }, { it.deletedAt != null }, { it.updatedBy }, { canonicalContent(it) })

      /**
       * Explicit, injective serialization of a row's content columns (id and sync columns excluded), used as the
       * last tie-breaker. Each field is "<length>:<value>", null is "~", joined by "|". Pinned by BackupMergerTest:
       * changing it changes merge results, so treat it like a file format.
       */
      internal fun canonicalContent(row: SyncRecord): String = when (row) {
          is FavoriteEntity -> hymnFields(row.hymn) + listOf(row.createdAt)
          is SingLogEntity ->
              hymnFields(row.hymn) + listOf(row.sungAt, row.occasion.name, row.source.name, row.playlistId, row.createdAt)
          is NoteEntity -> hymnFields(row.hymn) + listOf(row.body, row.singLogId, row.createdAt)
          is PlaylistEntity -> listOf(row.name, row.createdAt)
          is PlaylistItemEntity -> listOf<Any?>(row.playlistId, row.position) + hymnFields(row.hymn) + listOf(row.createdAt)
          else -> throw IllegalArgumentException("Unsupported row type ${row::class.java.name}")
      }.joinToString("|") { field -> field?.toString()?.let { "${it.length}:$it" } ?: "~" }

      private fun hymnFields(key: HymnKey): List<Any?> = listOf(key.hymnType, key.hymnNo, key.isFu)

      /** True when [candidate] should replace [current]. */
      @JvmStatic
      fun <T : SyncRecord> isNewer(candidate: T, current: T): Boolean = compare(candidate, current) > 0

      internal fun <T : SyncRecord> mergeTable(local: List<T>, incoming: List<T>): TableMerge<T> {
          val localById = local.associateBy { it.id }
          val newestIncoming = incoming.groupBy { it.id }.values.map { rows ->
              rows.reduce { kept, next -> if (isNewer(next, kept)) next else kept }
          }
          val decisions = newestIncoming.map { row -> decide(localById[row.id], row) to row }
          return TableMerge(
              changes = decisions.filter { (decision, _) -> decision != Decision.KEEP }.map { (_, row) -> row },
              stats = MergeStats(
                  inserted = decisions.count { it.first == Decision.INSERT },
                  updated = decisions.count { it.first == Decision.UPDATE },
                  unchanged = decisions.count { it.first == Decision.KEEP },
              ),
          )
      }

      /**
       * Local rows that are not being replaced keep their slot (soft-deleted ones too). Incoming rows whose slot is
       * taken move after the playlist's highest slot, in (position, id) order. updatedAt is not changed: re-slotting
       * is a local adjustment, not an edit.
       */
      internal fun resolvePositionCollisions(
          local: List<PlaylistItemEntity>,
          changes: List<PlaylistItemEntity>,
      ): List<PlaylistItemEntity> {
          val changedIds = changes.map { it.id }.toSet()
          val kept = local.filter { it.id !in changedIds }
          val reslotted = changes.groupBy { it.playlistId }.flatMap { (playlistId, rows) ->
              val keptSlots = kept.filter { it.playlistId == playlistId }.map { it.position }
              val highest = (keptSlots + rows.map { it.position }).maxOrNull() ?: -1
              val start = Slots(keptSlots.toSet(), highest, emptyList())
              rows.sortedWith(compareBy({ it.position }, { it.id })).fold(start) { slots, row ->
                  if (row.position !in slots.occupied) slots.take(row) else slots.take(row.copy(position = slots.highest + 1))
              }.rows
          }.associateBy { it.id }
          return changes.map { reslotted.getValue(it.id) }
      }

      private data class Slots(val occupied: Set<Int>, val highest: Int, val rows: List<PlaylistItemEntity>) {
          fun take(row: PlaylistItemEntity) =
              Slots(occupied + row.position, maxOf(highest, row.position), rows + row)
      }

      private fun <T : SyncRecord> decide(current: T?, incoming: T): Decision = when {
          current == null -> Decision.INSERT
          current == incoming -> Decision.KEEP
          isNewer(incoming, current) -> Decision.UPDATE
          else -> Decision.KEEP
      }

      private enum class Decision { INSERT, UPDATE, KEEP }
  }

  internal data class TableMerge<T>(val changes: List<T>, val stats: MergeStats)
  ```

- [ ] **Step 3：執行測試，確認它通過**

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.notebook.backup.*'`
  Expected: `BackupMergerTest` 18 個，加上 `BackupCodecTest` 18 個，全部通過。

  `statsAddUpAcrossTables` 的算法：
  - 本機 db1 收藏的 updatedAt 是 999，比檔案新 → unchanged 1。
  - 本機 note-1 的 updatedAt 是 1，比檔案舊 → updated 1。
  - 其他 8 列 → inserted 8。

- [ ] **Step 4：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/notebook/backup/BackupMerger.kt hymnchtv/src/test/java/org/cog/hymnchtv/notebook/backup/BackupMergerTest.kt
  git commit -m "feat: add deterministic last-writer-wins merge for notebook backups" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

### Task 10：BackupService、RoomBackupStore 與 SAF 介面（線性，`feat/notebook-data`）

**Files:**
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/notebook/backup/BackupStore.kt`、`BackupService.kt`、`BackupFileName.kt`、`RoomBackupStore.kt`、`BackupDocuments.kt`
- Test（JVM）: `hymnchtv/src/test/java/org/cog/hymnchtv/notebook/backup/InMemoryBackupStore.kt`、`BackupServiceTest.kt`、`BackupFileNameTest.kt`
- Test（instrumented；本 task 只編譯，Task 12 Step 1 執行）: `hymnchtv/src/androidTest/java/org/cog/hymnchtv/notebook/backup/RoomBackupStoreTest.kt`

- [ ] **Step 1：寫 `BackupStore` 介面**

  ```kotlin
  package org.cog.hymnchtv.notebook.backup

  /** Data access needed by backups. Implementations must run mergeAtomically in a single transaction. */
  interface BackupStore {
      /** Every row of every table, including soft-deleted rows. */
      suspend fun readAll(): NotebookTables

      /** Reads all rows, lets [plan] decide what to upsert, writes it, and returns the plan's result. */
      suspend fun <R> mergeAtomically(plan: (local: NotebookTables) -> Planned<R>): R
  }

  data class Planned<R>(val upserts: NotebookTables, val result: R)
  ```

- [ ] **Step 2：寫會失敗的 JVM 測試**

  `InMemoryBackupStore.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.backup

  import org.cog.hymnchtv.notebook.backup.SampleTables.upserted

  class InMemoryBackupStore(initial: NotebookTables = NotebookTables.EMPTY) : BackupStore {
      @Volatile
      var tables: NotebookTables = initial
          private set

      @Volatile
      var failNext: Boolean = false

      private fun maybeFail() {
          if (failNext) {
              failNext = false
              throw IllegalStateException("simulated storage failure")
          }
      }

      override suspend fun readAll(): NotebookTables {
          maybeFail()
          return tables
      }

      override suspend fun <R> mergeAtomically(plan: (local: NotebookTables) -> Planned<R>): R {
          maybeFail()
          val planned = plan(tables)
          tables = tables.upserted(planned.upserts)
          return planned.result
      }
  }
  ```

  `BackupServiceTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.backup

  import com.google.common.truth.Truth.assertThat
  import kotlinx.coroutines.ExperimentalCoroutinesApi
  import kotlinx.coroutines.test.TestScope
  import kotlinx.coroutines.test.UnconfinedTestDispatcher
  import kotlinx.coroutines.test.runTest
  import org.cog.hymnchtv.notebook.model.Clock
  import org.json.JSONObject
  import org.junit.Test
  import java.io.ByteArrayInputStream
  import java.io.ByteArrayOutputStream
  import java.io.IOException
  import java.io.OutputStream

  @OptIn(ExperimentalCoroutinesApi::class)
  class BackupServiceTest {
      private val now = 1_790_733_600_000L
      private val clock = Clock { now }

      private fun TestScope.service(store: BackupStore, maxBytes: Int = BackupService.DEFAULT_MAX_BYTES) =
          BackupService(store, clock, "2.9.2", UnconfinedTestDispatcher(testScheduler), maxBytes)

      private suspend fun BackupService.exportBytes(): ByteArray {
          val out = ByteArrayOutputStream()
          assertThat(exportTo(out)).isInstanceOf(ExportResult.Success::class.java)
          return out.toByteArray()
      }

      private fun assertSameRows(actual: NotebookTables, expected: NotebookTables) {
          assertThat(actual.favorites).containsExactlyElementsIn(expected.favorites)
          assertThat(actual.singLogs).containsExactlyElementsIn(expected.singLogs)
          assertThat(actual.notes).containsExactlyElementsIn(expected.notes)
          assertThat(actual.playlists).containsExactlyElementsIn(expected.playlists)
          assertThat(actual.playlistItems).containsExactlyElementsIn(expected.playlistItems)
      }

      @Test
      fun exportWritesHeaderAndAllRows() = runTest {
          val out = ByteArrayOutputStream()
          assertThat(service(InMemoryBackupStore(SampleTables.full())).exportTo(out)).isEqualTo(ExportResult.Success(10))
          val json = JSONObject(out.toString(Charsets.UTF_8.name()))
          assertThat(json.getInt("schemaVersion")).isEqualTo(1)
          assertThat(json.getLong("exportedAt")).isEqualTo(now)
          assertThat(json.getString("appVersionName")).isEqualTo("2.9.2")
      }

      @Test
      fun exportThenImportIntoAnEmptyStoreRestoresEverything() = runTest {
          val bytes = service(InMemoryBackupStore(SampleTables.full())).exportBytes()
          val target = InMemoryBackupStore()
          val result = service(target).importFrom(ByteArrayInputStream(bytes))
          assertThat(result).isEqualTo(ImportResult.Success(MergeStats(10, 0, 0), SkippedRows.NONE))
          assertSameRows(target.tables, SampleTables.full())
      }

      @Test
      fun importingTwiceIsIdempotent() = runTest {
          val bytes = service(InMemoryBackupStore(SampleTables.full())).exportBytes()
          val target = InMemoryBackupStore()
          service(target).importFrom(ByteArrayInputStream(bytes))
          assertThat(service(target).importFrom(ByteArrayInputStream(bytes)))
              .isEqualTo(ImportResult.Success(MergeStats(0, 0, 10), SkippedRows.NONE))
      }

      @Test
      fun notJsonIsReported() = runTest {
          val result = service(InMemoryBackupStore()).importFrom(ByteArrayInputStream("hello".toByteArray()))
          assertThat((result as ImportResult.Failure).error).isEqualTo(BackupError.NOT_JSON)
      }

      @Test
      fun oversizedInputIsRejectedAndExactLimitAccepted() = runTest {
          val bytes = service(InMemoryBackupStore()).exportBytes()
          val tooSmall = service(InMemoryBackupStore(), maxBytes = bytes.size - 1)
          assertThat((tooSmall.importFrom(ByteArrayInputStream(bytes)) as ImportResult.Failure).error)
              .isEqualTo(BackupError.TOO_LARGE)
          val exact = service(InMemoryBackupStore(), maxBytes = bytes.size)
          assertThat(exact.importFrom(ByteArrayInputStream(bytes))).isInstanceOf(ImportResult.Success::class.java)
      }

      @Test
      fun writeFailureIsIo() = runTest {
          val broken = object : OutputStream() {
              override fun write(b: Int) = throw IOException("disk full")
              override fun write(b: ByteArray, off: Int, len: Int) = throw IOException("disk full")
          }
          val result = service(InMemoryBackupStore(SampleTables.full())).exportTo(broken)
          assertThat(result).isEqualTo(ExportResult.Failure(BackupError.IO, "disk full"))
      }

      @Test
      fun storeFailureIsStorage() = runTest {
          val store = InMemoryBackupStore(SampleTables.full()).apply { failNext = true }
          val result = service(store).exportTo(ByteArrayOutputStream())
          assertThat((result as ExportResult.Failure).error).isEqualTo(BackupError.STORAGE)

          val bytes = service(InMemoryBackupStore(SampleTables.full())).exportBytes()
          val target = InMemoryBackupStore().apply { failNext = true }
          val imported = service(target).importFrom(ByteArrayInputStream(bytes))
          assertThat((imported as ImportResult.Failure).error).isEqualTo(BackupError.STORAGE)
          assertThat(target.tables).isEqualTo(NotebookTables.EMPTY)
      }
  }
  ```

  `BackupFileNameTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.backup

  import com.google.common.truth.Truth.assertThat
  import org.junit.Test
  import java.util.TimeZone

  class BackupFileNameTest {
      private val instant = 1_790_733_600_000L // 2026-09-30 02:00 UTC

      @Test
      fun usesLocalDateAndTime() {
          assertThat(BackupFileName.suggested(instant, TimeZone.getTimeZone("UTC")))
              .isEqualTo("hymnchtv-notebook-20260930-0200.json")
          assertThat(BackupFileName.suggested(instant, TimeZone.getTimeZone("Asia/Taipei")))
              .isEqualTo("hymnchtv-notebook-20260930-1000.json")
      }
  }
  ```

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.notebook.backup.*'`
  Expected: 編譯失敗，`Unresolved reference 'BackupService'`。

- [ ] **Step 3：實作純 Kotlin 部分**

  `backup/BackupService.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.backup

  import kotlinx.coroutines.CoroutineDispatcher
  import kotlinx.coroutines.Dispatchers
  import kotlinx.coroutines.withContext
  import org.cog.hymnchtv.notebook.model.Clock
  import timber.log.Timber
  import java.io.ByteArrayOutputStream
  import java.io.IOException
  import java.io.InputStream
  import java.io.OutputStream
  import kotlin.coroutines.cancellation.CancellationException

  /** Export/import over plain streams; the caller owns (opens and closes) the streams. Never throws except cancellation. */
  class BackupService(
      private val store: BackupStore,
      private val clock: Clock,
      private val appVersionName: String,
      private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
      private val maxBytes: Int = DEFAULT_MAX_BYTES,
      private val limits: BackupLimits = BackupLimits(),
  ) {
      suspend fun exportTo(output: OutputStream): ExportResult = withContext(ioDispatcher) {
          guarded<ExportResult>(onError = { error, e -> ExportResult.Failure(error, e.message.orEmpty()) }) {
              val tables = store.readAll()
              val snapshot = BackupSnapshot(BackupCodec.CURRENT_SCHEMA_VERSION, clock.nowMillis(), appVersionName, tables)
              output.write(BackupCodec.encode(snapshot).toByteArray(Charsets.UTF_8))
              output.flush()
              ExportResult.Success(tables.rowCount)
          }
      }

      /** Decodes (rows > 24 h in the future are skipped), merges with local rows and writes in one transaction. Prefs are untouched. */
      suspend fun importFrom(input: InputStream): ImportResult = withContext(ioDispatcher) {
          guarded<ImportResult>(onError = { error, e -> ImportResult.Failure(error, e.message.orEmpty()) }) {
              val bytes = readLimited(input, maxBytes)
              if (bytes == null) {
                  ImportResult.Failure(BackupError.TOO_LARGE, "Backup larger than $maxBytes bytes")
              } else {
                  when (val decoded = BackupCodec.decode(String(bytes, Charsets.UTF_8), clock.nowMillis(), limits)) {
                      is DecodeResult.Failure -> ImportResult.Failure(decoded.error, decoded.detail)
                      is DecodeResult.Success -> {
                          val stats = store.mergeAtomically { local ->
                              val merged = BackupMerger.merge(local, decoded.snapshot.tables)
                              Planned(merged.changes, merged.stats)
                          }
                          ImportResult.Success(stats, decoded.skipped)
                      }
                  }
              }
          }
      }

      companion object {
          const val DEFAULT_MAX_BYTES = 16 * 1024 * 1024
      }
  }

  /** Maps I/O errors to IO and anything else (e.g. SQLite) to STORAGE; cancellation propagates. */
  private inline fun <T> guarded(onError: (BackupError, Exception) -> T, block: () -> T): T = try {
      block()
  } catch (e: CancellationException) {
      throw e
  } catch (e: IOException) {
      Timber.w(e, "Notebook backup I/O failed")
      onError(BackupError.IO, e)
  } catch (e: Exception) {
      Timber.e(e, "Notebook backup failed")
      onError(BackupError.STORAGE, e)
  }

  private const val BUFFER_SIZE = 64 * 1024

  /** Reads the whole stream if it is at most [limit] bytes; null when longer. */
  internal fun readLimited(input: InputStream, limit: Int): ByteArray? {
      val out = ByteArrayOutputStream()
      val buffer = ByteArray(BUFFER_SIZE)
      var total = 0
      while (true) {
          val read = input.read(buffer)
          if (read < 0) return out.toByteArray()
          total += read
          if (total > limit) return null
          out.write(buffer, 0, read)
      }
  }
  ```

  `backup/BackupFileName.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.backup

  import java.text.SimpleDateFormat
  import java.util.Date
  import java.util.Locale
  import java.util.TimeZone

  object BackupFileName {
      private const val PREFIX = "hymnchtv-notebook-"
      private const val EXTENSION = ".json"
      private const val PATTERN = "yyyyMMdd-HHmm"

      /** Default title for ActivityResultContracts.CreateDocument, e.g. hymnchtv-notebook-20261002-1530.json. */
      @JvmStatic
      @JvmOverloads
      fun suggested(nowMillis: Long, zone: TimeZone = TimeZone.getDefault()): String {
          val format = SimpleDateFormat(PATTERN, Locale.US).apply { timeZone = zone }
          return PREFIX + format.format(Date(nowMillis)) + EXTENSION
      }
  }
  ```

- [ ] **Step 4：執行 JVM 測試，確認它通過**

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.notebook.backup.*'`
  Expected: `BackupServiceTest` 7 個、`BackupFileNameTest` 1 個，加上 Task 8、9 的 36 個，全部通過。

- [ ] **Step 5：實作 Android 邊界**

  `backup/RoomBackupStore.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.backup

  import androidx.room.withTransaction
  import org.cog.hymnchtv.persistance.room.HymnchtvDatabase
  import org.cog.hymnchtv.notebook.data.entity.PlaylistItemEntity

  /**
   * Upserts are safe for the merged rows: favorite ids are derived from the hymn, every other table is keyed by
   * UUID only, and BackupMerger re-slots playlist items so their *final* (playlistId, position) is unique.
   * Playlist items are written in two phases so swaps and cycles never collide mid-transaction.
   */
  class RoomBackupStore(private val db: HymnchtvDatabase) : BackupStore {
      override suspend fun readAll(): NotebookTables = db.withTransaction { readAllRows() }

      override suspend fun <R> mergeAtomically(plan: (local: NotebookTables) -> Planned<R>): R =
          db.withTransaction {
              val planned = plan(readAllRows())
              writeRows(planned.upserts)
              planned.result
          }

      private suspend fun readAllRows() = NotebookTables(
          favorites = db.favoriteDao().findAllIncludingDeleted(),
          singLogs = db.singLogDao().findAllIncludingDeleted(),
          notes = db.noteDao().findAllIncludingDeleted(),
          playlists = db.playlistDao().findAllIncludingDeleted(),
          playlistItems = db.playlistItemDao().findAllIncludingDeleted(),
      )

      private suspend fun writeRows(rows: NotebookTables) {
          if (rows.favorites.isNotEmpty()) db.favoriteDao().upsertAll(rows.favorites)
          if (rows.singLogs.isNotEmpty()) db.singLogDao().upsertAll(rows.singLogs)
          if (rows.notes.isNotEmpty()) db.noteDao().upsertAll(rows.notes)
          if (rows.playlists.isNotEmpty()) db.playlistDao().upsertAll(rows.playlists)
          writePlaylistItems(rows.playlistItems)
      }

      /**
       * Phase 1 parks every row on its own negative slot (real positions are always >= 0), vacating all old slots of
       * the rows being moved; phase 2 writes the final slots, which BackupMerger guarantees are free. Both phases run
       * inside the caller's transaction, so readers never see the parked positions.
       */
      private suspend fun writePlaylistItems(items: List<PlaylistItemEntity>) {
          if (items.isEmpty()) return
          val dao = db.playlistItemDao()
          dao.upsertAll(items.mapIndexed { index, item -> item.copy(position = -1 - index) })
          dao.upsertAll(items)
      }
  }
  ```

  `backup/BackupDocuments.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.backup

  import android.content.ContentResolver
  import android.net.Uri
  import kotlinx.coroutines.Dispatchers
  import kotlinx.coroutines.withContext
  import timber.log.Timber
  import java.io.FileNotFoundException
  import java.io.IOException
  import java.io.InputStream
  import java.io.OutputStream

  /** Export/import by document Uri; implemented by [UriBackupIo]. */
  interface BackupIo {
      suspend fun exportTo(uri: Uri): ExportResult
      suspend fun importFrom(uri: Uri): ImportResult
  }

  /**
   * Storage Access Framework glue. The UI obtains Uris with
   * ActivityResultContracts.CreateDocument(MIME_TYPE) / OpenDocument() and passes them here.
   */
  object BackupDocuments {
      const val MIME_TYPE = "application/json"

      /** Some providers label .json files as text/plain or application/octet-stream. */
      @JvmField
      val OPEN_MIME_TYPES: Array<String> = arrayOf(MIME_TYPE, "text/plain", "application/octet-stream")

      suspend fun exportTo(resolver: ContentResolver, uri: Uri, service: BackupService): ExportResult =
          withContext(Dispatchers.IO) {
              val stream = openOutput(resolver, uri)
                  ?: return@withContext ExportResult.Failure(BackupError.IO, "Cannot open document for writing")
              try {
                  stream.use { service.exportTo(it) }
              } catch (e: IOException) {
                  Timber.w(e, "Closing exported document failed")
                  ExportResult.Failure(BackupError.IO, e.message.orEmpty())
              }
          }

      suspend fun importFrom(resolver: ContentResolver, uri: Uri, service: BackupService): ImportResult =
          withContext(Dispatchers.IO) {
              val stream = openInput(resolver, uri)
                  ?: return@withContext ImportResult.Failure(BackupError.IO, "Cannot open document for reading")
              try {
                  stream.use { service.importFrom(it) }
              } catch (e: IOException) {
                  Timber.w(e, "Closing imported document failed")
                  ImportResult.Failure(BackupError.IO, e.message.orEmpty())
              }
          }

      private fun openOutput(resolver: ContentResolver, uri: Uri): OutputStream? = try {
          openTruncating(resolver, uri)
      } catch (e: FileNotFoundException) {
          Timber.w(e, "Cannot open %s for writing", uri)
          null
      } catch (e: SecurityException) {
          Timber.w(e, "No permission to write %s", uri)
          null
      }

      /** "wt" truncates an existing document; a few providers only accept "w". */
      private fun openTruncating(resolver: ContentResolver, uri: Uri): OutputStream? = try {
          resolver.openOutputStream(uri, "wt")
      } catch (e: IllegalArgumentException) {
          resolver.openOutputStream(uri, "w")
      }

      private fun openInput(resolver: ContentResolver, uri: Uri): InputStream? = try {
          resolver.openInputStream(uri)
      } catch (e: FileNotFoundException) {
          Timber.w(e, "Cannot open %s for reading", uri)
          null
      } catch (e: SecurityException) {
          Timber.w(e, "No permission to read %s", uri)
          null
      }
  }

  class UriBackupIo(private val resolver: ContentResolver, private val service: BackupService) : BackupIo {
      override suspend fun exportTo(uri: Uri): ExportResult = BackupDocuments.exportTo(resolver, uri, service)
      override suspend fun importFrom(uri: Uri): ImportResult = BackupDocuments.importFrom(resolver, uri, service)
  }
  ```

  `androidTest/.../notebook/backup/RoomBackupStoreTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.backup

  import androidx.test.core.app.ApplicationProvider
  import androidx.test.ext.junit.runners.AndroidJUnit4
  import com.google.common.truth.Truth.assertThat
  import kotlinx.coroutines.runBlocking
  import org.cog.hymnchtv.persistance.room.HymnchtvDatabase
  import org.cog.hymnchtv.notebook.data.entity.FavoriteEntity
  import org.cog.hymnchtv.notebook.data.entity.NoteEntity
  import org.cog.hymnchtv.notebook.data.entity.PlaylistEntity
  import org.cog.hymnchtv.notebook.data.entity.PlaylistItemEntity
  import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
  import org.cog.hymnchtv.notebook.model.FavoriteIds
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.HymnTypes
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.cog.hymnchtv.notebook.model.SingSource
  import org.junit.After
  import org.junit.Before
  import org.junit.Test
  import org.junit.runner.RunWith
  import java.util.UUID

  @RunWith(AndroidJUnit4::class)
  class RoomBackupStoreTest {
      private lateinit var db: HymnchtvDatabase
      private lateinit var store: RoomBackupStore
      private val key = HymnKey.of(HymnTypes.DB, 1)
      private fun id(n: Int) = UUID(0L, n.toLong()).toString()
      private val device = id(170)

      private val tables by lazy {
          NotebookTables(
              favorites = listOf(FavoriteEntity(FavoriteIds.forKey(key), key, 1, 2, 2, device)),
              singLogs = listOf(
                  SingLogEntity(
                      id = id(1), hymn = key, sungAt = 5, occasion = Occasion.MORNING_REVIVAL, source = SingSource.MANUAL,
                      playlistId = id(3), createdAt = 1, updatedAt = 1, updatedBy = device,
                  ),
              ),
              notes = listOf(NoteEntity(id(2), key, "筆記", id(1), createdAt = 1, updatedAt = 1, updatedBy = device)),
              playlists = listOf(PlaylistEntity(id(3), "歌單", createdAt = 1, updatedAt = 1, updatedBy = device)),
              playlistItems = listOf(PlaylistItemEntity(id(4), id(3), 0, key, createdAt = 1, updatedAt = 1, deletedAt = 3, updatedBy = device)),
          )
      }

      @Before
      fun setUp() {
          db = HymnchtvDatabase.inMemory(ApplicationProvider.getApplicationContext())
          store = RoomBackupStore(db)
      }

      @After
      fun tearDown() = db.close()

      @Test
      fun writesAndReadsEveryRowIncludingDeleted(): Unit = runBlocking {
          val result = store.mergeAtomically { local ->
              assertThat(local.rowCount).isEqualTo(0)
              Planned(tables, "done")
          }
          assertThat(result).isEqualTo("done")
          assertThat(store.readAll()).isEqualTo(tables)
      }

      @Test
      fun failingPlanWritesNothing(): Unit = runBlocking {
          val outcome = runCatching { store.mergeAtomically<Unit> { throw IllegalStateException("boom") } }
          assertThat(outcome.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
          assertThat(store.readAll().rowCount).isEqualTo(0)
      }

      @Test
      fun mergedPlaylistItemsNeverViolateTheSlotIndex(): Unit = runBlocking {
          store.mergeAtomically { Planned(tables.copy(playlistItems = listOf(tables.playlistItems.single().copy(deletedAt = null))), Unit) }
          val incoming = NotebookTables(
              playlistItems = listOf(PlaylistItemEntity(id(5), id(3), 0, key, createdAt = 2, updatedAt = 2, updatedBy = device)),
          )
          store.mergeAtomically { local -> BackupMerger.merge(local, incoming).let { Planned(it.changes, Unit) } }
          assertThat(store.readAll().playlistItems.map { it.id to it.position }).containsExactly(id(4) to 0, id(5) to 1)
      }

      private fun item(n: Int, position: Int, updatedAt: Long = 1) =
          PlaylistItemEntity(id(n), id(3), position, key, createdAt = 1, updatedAt = updatedAt, updatedBy = device)

      private suspend fun seedItems(vararg rows: PlaylistItemEntity) =
          store.mergeAtomically { Planned(NotebookTables(playlistItems = rows.toList()), Unit) }

      private suspend fun importItems(vararg rows: PlaylistItemEntity) =
          store.mergeAtomically { local ->
              BackupMerger.merge(local, NotebookTables(playlistItems = rows.toList())).let { Planned(it.changes, Unit) }
          }

      private suspend fun positions() = store.readAll().playlistItems.associate { it.id to it.position }

      @Test
      fun importedSwapOfTwoExistingItemsSucceeds(): Unit = runBlocking {
          seedItems(item(10, 0), item(11, 1))
          importItems(item(10, 1, updatedAt = 2), item(11, 0, updatedAt = 2))
          assertThat(positions()).containsExactly(id(10), 1, id(11), 0)
      }

      @Test
      fun importedThreeCycleSucceeds(): Unit = runBlocking {
          seedItems(item(10, 0), item(11, 1), item(12, 2))
          importItems(item(10, 1, updatedAt = 2), item(11, 2, updatedAt = 2), item(12, 0, updatedAt = 2))
          assertThat(positions()).containsExactly(id(10), 1, id(11), 2, id(12), 0)
      }

      @Test
      fun importedThreeWayReorderWithAnUnchangedMiddleSucceeds(): Unit = runBlocking {
          seedItems(item(10, 0), item(11, 1), item(12, 2))
          importItems(item(10, 2, updatedAt = 2), item(11, 1), item(12, 0, updatedAt = 2))
          assertThat(positions()).containsExactly(id(10), 2, id(11), 1, id(12), 0)
          assertThat(store.readAll().playlistItems.none { it.position < 0 }).isTrue()
      }
  }
  ```

- [ ] **Step 6：確認全部編譯**

  Run: `./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug :hymnchtv:assembleDebugAndroidTest --console=plain`
  Expected: BUILD SUCCESSFUL（`RoomBackupStoreTest` 在 Task 12 Step 1 執行）。

- [ ] **Step 7：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/notebook/backup hymnchtv/src/test/java/org/cog/hymnchtv/notebook/backup hymnchtv/src/androidTest/java/org/cog/hymnchtv/notebook/backup
  git commit -m "feat: add notebook backup service, Room store and SAF document glue" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

### Task 11：開啟 Android 自動備份（線性，`feat/notebook-data`）

**Files:**
- Create: `hymnchtv/src/main/res/xml/notebook_backup_rules.xml`、`hymnchtv/src/main/res/xml/notebook_data_extraction_rules.xml`
- Modify: `hymnchtv/src/main/AndroidManifest.xml`（`<application>` 的屬性）
- Test: `hymnchtv/src/test/java/org/cog/hymnchtv/notebook/backup/BackupRulesTest.kt`、`hymnchtv/src/test/java/org/cog/hymnchtv/notebook/backup/SettingsPrefsNameTest.java`

備份範圍（協調者決定，Room 統一計畫 §2.5 修訂）：`hymnchtv.db`（統一資料庫）、`hymnchtv.db-wal`、`hymnchtv.db-shm`、`notebook.xml`、`Settings.xml`。**不包含** `notebook_device.xml`；舊的 `dbHymnApp.db`、`notebook.db` 已不存在（啟動時刪除）。備份範圍是**整個**統一 DB：媒體連結、歷史、英文歌詞與筆記本五張表一併備份與還原（無憑證或機密）。

**隱私說明與還原預期**：備份內容包含媒體連結、歷史、英文歌詞（不只筆記本）；換機還原後這些與筆記本一併回來。

**風險：Auto Backup 配額 25 MB。** 統一 DB 的大小已在 emulator-5582（API 24）實測：安裝並完成首次匯入後 `hymnchtv.db` 約 464 KB（含 `-wal` 約 495 KB、`-shm` 32 KB），`media_record` 2696 列，`english_lyrics` 為 0 列（英文歌詞不在首次匯入，使用者開啟時才由 WebView 下載並寫入）。英文歌詞的上限需再估算（約一千首、每首數 KB），遠低於 25 MB；若實測接近上限再決定改為從 assets 載入、不入 DB。

- [ ] **Step 1：寫會失敗的測試**

  （Gradle 跑 unit test 時的工作目錄是 `hymnchtv/`，所以用相對路徑讀資源檔。）

  `SettingsPrefsNameTest.java`（用 Java 寫：javac 會內嵌 `MainActivity.PREF_SETTINGS` 這個常數，不會載入 `MainActivity`）：

  ```java
  package org.cog.hymnchtv.notebook.backup;

  import static com.google.common.truth.Truth.assertThat;

  import org.cog.hymnchtv.MainActivity;
  import org.junit.Test;

  /** The backup rules name the legacy settings file literally; fail if MainActivity renames it. */
  public class SettingsPrefsNameTest {
      public static final String SETTINGS_FILE = "Settings";

      @Test
      public void settingsFileMatchesMainActivity() {
          assertThat(MainActivity.PREF_SETTINGS).isEqualTo(SETTINGS_FILE);
      }
  }
  ```

  `BackupRulesTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.backup

  import com.google.common.truth.Truth.assertThat
  import com.google.common.truth.Truth.assertWithMessage
  import org.cog.hymnchtv.persistance.room.HymnchtvDatabase
  import org.cog.hymnchtv.notebook.settings.NotebookPrefs
  import org.junit.Test
  import java.io.File

  /** Keeps backup rule files and the manifest in sync with the DB and prefs file names. */
  class BackupRulesTest {
      private fun read(path: String): String {
          val file = File(path)
          assertWithMessage("$path (working dir ${File(".").absolutePath})").that(file.exists()).isTrue()
          return file.readText()
      }

      private val dbIncludes = listOf("", "-wal", "-shm").map { "domain=\"database\" path=\"${HymnchtvDatabase.FILE_NAME}$it\"" }
      private val prefsInclude = "domain=\"sharedpref\" path=\"${NotebookPrefs.FILE_NAME}.xml\""
      private val settingsInclude = "domain=\"sharedpref\" path=\"${SettingsPrefsNameTest.SETTINGS_FILE}.xml\""
      // Match on the path attribute only: the XML comments legitimately mention e.g. notebook_device.xml.
      private val forbiddenPaths = listOf("dbHymnApp", "notebook.db", NotebookPrefs.DEVICE_FILE_NAME)
      private val forbiddenAttribute = "disableIfNoEncryptionCapabilities=\""


      private fun count(xml: String, needle: String) = Regex(Regex.escape(needle)).findAll(xml).count()

      @Test
      fun legacyRulesIncludeNotebookAndSettingsOnly() {
          val xml = read("src/main/res/xml/notebook_backup_rules.xml")
          (dbIncludes + listOf(prefsInclude, settingsInclude)).forEach { assertThat(count(xml, it)).isEqualTo(1) }
          assertThat(count(xml, "<include ")).isEqualTo(5)
          forbiddenPaths.forEach { assertThat(xml).doesNotContain("path=\"$it") }
      }

      @Test
      fun extractionRulesCoverCloudBackupAndDeviceTransfer() {
          val xml = read("src/main/res/xml/notebook_data_extraction_rules.xml")
          assertThat(xml).contains("<cloud-backup>")
          assertThat(xml).contains("<device-transfer>")
          (dbIncludes + listOf(prefsInclude, settingsInclude)).forEach { assertThat(count(xml, it)).isEqualTo(2) }
          assertThat(count(xml, "<include ")).isEqualTo(10)
          forbiddenPaths.forEach { assertThat(xml).doesNotContain("path=\"$it") }
          assertThat(xml).doesNotContain(forbiddenAttribute)
      }

      @Test
      fun manifestEnablesBackupWithBothRuleFiles() {
          val manifest = read("src/main/AndroidManifest.xml")
          assertThat(manifest).contains("android:allowBackup=\"true\"")
          assertThat(manifest).contains("android:dataExtractionRules=\"@xml/notebook_data_extraction_rules\"")
          assertThat(manifest).contains("android:fullBackupContent=\"@xml/notebook_backup_rules\"")
          assertThat(manifest).doesNotContain("tools:ignore=\"DataExtractionRules\"")
      }
  }
  ```

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.notebook.backup.BackupRulesTest' --tests 'org.cog.hymnchtv.notebook.backup.SettingsPrefsNameTest'`
  Expected: `SettingsPrefsNameTest` 通過；`BackupRulesTest` 的 3 個測試失敗（檔案不存在、manifest 仍是 `allowBackup="false"`）。

- [ ] **Step 2：新增規則檔**

  `hymnchtv/src/main/res/xml/notebook_backup_rules.xml`：

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <!-- Auto Backup for API 24-30 (android:fullBackupContent): the unified Room database hymnchtv.db (with its WAL
       files; media links, history, English lyrics and notebook tables all live in it) plus the notebook prefs and the
       app settings (MainActivity.PREF_SETTINGS). With <include>, everything else is left out: notebook_device.xml
       (per-install device id).
       Keep in sync with notebook_data_extraction_rules.xml (BackupRulesTest). -->
  <full-backup-content>
      <include domain="database" path="hymnchtv.db" />
      <include domain="database" path="hymnchtv.db-wal" />
      <include domain="database" path="hymnchtv.db-shm" />
      <include domain="sharedpref" path="notebook.xml" />
      <include domain="sharedpref" path="Settings.xml" />
  </full-backup-content>
  ```

  `hymnchtv/src/main/res/xml/notebook_data_extraction_rules.xml`：

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <!-- Auto Backup and device-to-device transfer for API 31+ (android:dataExtractionRules). No encryption
       requirement on purpose: phones without a screen lock (common among elderly users) must still restore.
       Excludes notebook_device.xml (see notebook_backup_rules.xml). Kept in sync by BackupRulesTest. -->
  <data-extraction-rules>
      <cloud-backup>
          <include domain="database" path="hymnchtv.db" />
          <include domain="database" path="hymnchtv.db-wal" />
          <include domain="database" path="hymnchtv.db-shm" />
          <include domain="sharedpref" path="notebook.xml" />
          <include domain="sharedpref" path="Settings.xml" />
      </cloud-backup>
      <device-transfer>
          <include domain="database" path="hymnchtv.db" />
          <include domain="database" path="hymnchtv.db-wal" />
          <include domain="database" path="hymnchtv.db-shm" />
          <include domain="sharedpref" path="notebook.xml" />
          <include domain="sharedpref" path="Settings.xml" />
      </device-transfer>
  </data-extraction-rules>
  ```

- [ ] **Step 3：修改 manifest 的 `<application>`**

  把這一行：

  ```xml
          android:allowBackup="false"
  ```

  換成：

  ```xml
          android:allowBackup="true"
          android:dataExtractionRules="@xml/notebook_data_extraction_rules"
          android:fullBackupContent="@xml/notebook_backup_rules"
  ```

  再刪掉這一行：

  ```xml
          tools:ignore="DataExtractionRules"
  ```

  `tools:targetApi="tiramisu"` 保留。

- [ ] **Step 4：驗證**

  ```bash
  ./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug :hymnchtv:lintDebug --console=plain
  grep -cE "\[(FullBackupContent|DataExtractionRules|AllowBackup)\]" hymnchtv/build/reports/lint-results-debug.txt
  M=$(find hymnchtv/build/intermediates -path '*debug*' -name AndroidManifest.xml | xargs grep -l 'android:allowBackup' | head -1)
  grep -oE 'android:(allowBackup|dataExtractionRules|fullBackupContent)="[^"]*"' "$M"
  ```

  Expected:
  - 全部測試通過；BUILD SUCCESSFUL。
  - lint 相關 issue 數量是 `0`。
  - merged manifest 印出三個屬性，值是 `true` 和兩個 `@xml/notebook_*`。

- [ ] **Step 5：Commit**

  ```bash
  git add hymnchtv/src/main/res/xml/notebook_backup_rules.xml hymnchtv/src/main/res/xml/notebook_data_extraction_rules.xml hymnchtv/src/main/AndroidManifest.xml hymnchtv/src/test/java/org/cog/hymnchtv/notebook/backup/BackupRulesTest.kt hymnchtv/src/test/java/org/cog/hymnchtv/notebook/backup/SettingsPrefsNameTest.java
  git commit -m "feat: enable Android Auto Backup for the notebook and app settings" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

### Task 12：物件圖與 Java 介面（線性，`feat/notebook-data`）

**前置條件：** Task 0–11 都已依序 commit 在 `feat/notebook-data`（rev 7：不再合併 lane）。

**Files:**
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/notebook/Notebook.kt`、`NotebookAsync.kt`
- Test: `hymnchtv/src/test/java/org/cog/hymnchtv/notebook/NotebookAsyncTest.kt`、`hymnchtv/src/test/java/org/cog/hymnchtv/notebook/fakes/InMemoryFavoriteRepository.kt`

- [ ] **Step 1：跑全部測試**

  ```bash
  cd /Users/hitobias/orca/hymnchtv-d1a
  test "$(git branch --show-current)" = feat/notebook-data
  ./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug --console=plain
  export ANDROID_SERIAL=emulator-5580
  test "$(adb emu avd name | head -1 | tr -d '\r')" = api34b && ./gradlew :hymnchtv:connectedDebugAndroidTest --console=plain
  ```

  Expected:
  - 目前分支是 `feat/notebook-data`，工作樹乾淨（沒有未提交的 Task 8–11 檔案）。
  - JVM 測試全部通過。
  - instrumented test 全部通過。其中 `RoomBackupStoreTest`（6 個）是第一次在這裡執行；`SharedPrefsNotebookPrefsTest`（5 個）已隨 Task 7 之後的全套 instrumented test 執行過，這裡只是再跑一次。

- [ ] **Step 2：寫會失敗的測試**

  `fakes/InMemoryFavoriteRepository.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook.fakes

  import kotlinx.coroutines.CompletableDeferred
  import org.cog.hymnchtv.notebook.data.entity.FavoriteEntity
  import org.cog.hymnchtv.notebook.model.Clock
  import org.cog.hymnchtv.notebook.model.FavoriteIds
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.isActive
  import org.cog.hymnchtv.notebook.repo.FavoriteRepository

  class InMemoryFavoriteRepository(private val clock: Clock, private val device: String = TEST_DEVICE) : FavoriteRepository {
      @Volatile
      var rows: Map<String, FavoriteEntity> = emptyMap()
          private set

      @Volatile
      var failNext: Boolean = false

      /** When set, isFavorite/toggle suspend until it completes (to test cancellation). */
      @Volatile
      var gate: CompletableDeferred<Unit>? = null

      private suspend fun enter() {
          gate?.await()
          if (failNext) {
              failNext = false
              throw IllegalStateException("simulated failure")
          }
      }

      private fun put(row: FavoriteEntity): FavoriteEntity {
          rows = rows + (row.id to row)
          return row
      }

      override suspend fun findAll() = rows.values.filter { it.isActive }

      override suspend fun findById(id: String) = rows[id]?.takeIf { it.isActive }

      override suspend fun create(item: FavoriteEntity): FavoriteEntity = checkNotNull(setFavorite(item.hymn, true))

      override suspend fun update(item: FavoriteEntity): FavoriteEntity? =
          findById(item.id)?.copy(updatedAt = clock.nowMillis(), updatedBy = device)?.let(::put)

      override suspend fun delete(id: String): Boolean {
          val existing = findById(id) ?: return false
          val now = clock.nowMillis()
          put(existing.copy(updatedAt = now, deletedAt = now, updatedBy = device))
          return true
      }

      override suspend fun isFavorite(key: HymnKey): Boolean {
          enter()
          return rows[FavoriteIds.forKey(key)]?.isActive == true
      }

      override suspend fun setFavorite(key: HymnKey, favorite: Boolean): FavoriteEntity? {
          val id = FavoriteIds.forKey(key)
          val existing = rows[id]
          val now = clock.nowMillis()
          val row = if (favorite) {
              if (existing != null && existing.isActive) return existing
              existing?.copy(updatedAt = now, deletedAt = null, updatedBy = device)
                  ?: FavoriteEntity(id, key, now, now, null, device)
          } else {
              if (existing == null || !existing.isActive) return existing
              existing.copy(updatedAt = now, deletedAt = now, updatedBy = device)
          }
          return put(row)
      }

      override suspend fun toggle(key: HymnKey): Boolean {
          enter()
          val next = rows[FavoriteIds.forKey(key)]?.isActive != true
          setFavorite(key, next)
          return next
      }
  }
  ```

  `NotebookAsyncTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.notebook

  import android.net.Uri
  import com.google.common.truth.Truth.assertThat
  import kotlinx.coroutines.CompletableDeferred
  import kotlinx.coroutines.ExperimentalCoroutinesApi
  import kotlinx.coroutines.test.TestScope
  import kotlinx.coroutines.test.UnconfinedTestDispatcher
  import kotlinx.coroutines.test.runCurrent
  import kotlinx.coroutines.test.runTest
  import org.cog.hymnchtv.notebook.backup.BackupIo
  import org.cog.hymnchtv.notebook.backup.ExportResult
  import org.cog.hymnchtv.notebook.backup.ImportResult
  import org.cog.hymnchtv.notebook.data.SingStats
  import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
  import org.cog.hymnchtv.notebook.fakes.FakeNotebookPrefs
  import org.cog.hymnchtv.notebook.fakes.InMemoryFavoriteRepository
  import org.cog.hymnchtv.notebook.fakes.InMemorySingLogRepository
  import org.cog.hymnchtv.notebook.model.Clock
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.HymnTypes
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.cog.hymnchtv.notebook.model.SingSource
  import org.cog.hymnchtv.notebook.record.SingTracker
  import org.junit.Test
  import java.util.TimeZone

  @OptIn(ExperimentalCoroutinesApi::class)
  class NotebookAsyncTest {
      private val key = HymnKey.of(HymnTypes.DB, 1)

      private object UnusedBackupIo : BackupIo {
          override suspend fun exportTo(uri: Uri): ExportResult = throw UnsupportedOperationException()
          override suspend fun importFrom(uri: Uri): ImportResult = throw UnsupportedOperationException()
      }

      private class Harness(scope: TestScope) {
          val clock = Clock { 1_790_733_600_000L + scope.testScheduler.currentTime }
          val favorites = InMemoryFavoriteRepository(clock)
          val singLogs = InMemorySingLogRepository(clock)
          val prefs = FakeNotebookPrefs()
          val tracker = SingTracker(singLogs, prefs, clock, scope.backgroundScope, zone = { TimeZone.getTimeZone("UTC") })
          val async = NotebookAsync(
              favorites, singLogs, prefs, tracker, UnusedBackupIo,
              callbackDispatcher = UnconfinedTestDispatcher(scope.testScheduler),
              workDispatcher = UnconfinedTestDispatcher(scope.testScheduler),
          )
      }

      @Test
      fun toggleFavoriteDeliversTheNewState() = runTest {
          val h = Harness(this)
          val outcomes = mutableListOf<Outcome<Boolean>>()
          h.async.toggleFavorite(key) { outcomes += it }
          h.async.toggleFavorite(key) { outcomes += it }
          h.async.isFavorite(key) { outcomes += it }
          runCurrent()
          assertThat(outcomes).containsExactly(Outcome.Ok(true), Outcome.Ok(false), Outcome.Ok(false)).inOrder()
      }

      @Test
      fun recordManualBypassesDedupeAndRemembersTheOccasion() = runTest {
          val h = Harness(this)
          val recorded = mutableListOf<Outcome<SingLogEntity>>()
          h.async.recordManual(key, Occasion.SMALL_GROUP, 5_000) { recorded += it }
          h.async.recordManual(key, Occasion.SMALL_GROUP, 5_001) { recorded += it }
          runCurrent()
          assertThat(recorded.map { it.getOrNull()?.source }).containsExactly(SingSource.MANUAL, SingSource.MANUAL)
          assertThat(h.prefs.lastChosenOccasion).isEqualTo(Occasion.SMALL_GROUP)

          var stats: Outcome<SingStats>? = null
          h.async.singStats(key) { stats = it }
          runCurrent()
          assertThat(stats).isEqualTo(Outcome.Ok(SingStats(2, 5_001L)))
      }

      @Test
      fun recordManualRejectsInvalidTimes() = runTest {
          val h = Harness(this)
          var outcome: Outcome<SingLogEntity>? = null
          h.async.recordManual(key, Occasion.HOME, -1) { outcome = it }
          runCurrent()
          assertThat(outcome?.errorOrNull()).isInstanceOf(IllegalArgumentException::class.java)
          assertThat(h.prefs.lastChosenOccasion).isNull()
          assertThat(h.singLogs.rows).isEmpty()
      }

      @Test
      fun updateSingLogRemembersTheCorrectedOccasion() = runTest {
          val h = Harness(this)
          val log = h.singLogs.record(key, 1_000, Occasion.HOME, SingSource.AUTO)
          var updated: Outcome<SingLogEntity?>? = null
          h.async.updateSingLog(log.copy(occasion = Occasion.PRAYER_MEETING)) { updated = it }
          runCurrent()
          assertThat(updated?.getOrNull()?.occasion).isEqualTo(Occasion.PRAYER_MEETING)
          assertThat(h.prefs.lastChosenOccasion).isEqualTo(Occasion.PRAYER_MEETING)
      }

      @Test
      fun failuresBecomeErr() = runTest {
          val h = Harness(this)
          h.favorites.failNext = true
          var outcome: Outcome<Boolean>? = null
          h.async.isFavorite(key) { outcome = it }
          runCurrent()
          assertThat(outcome?.errorOrNull()).isInstanceOf(IllegalStateException::class.java)
      }

      @Test
      fun cancelledCallsNeverCallBack() = runTest {
          val h = Harness(this)
          val gate = CompletableDeferred<Unit>().also { h.favorites.gate = it }
          val outcomes = mutableListOf<Outcome<Boolean>>()
          val call = h.async.isFavorite(key) { outcomes += it }
          call.cancel()
          gate.complete(Unit)
          runCurrent()
          assertThat(outcomes).isEmpty()
      }

      @Test
      fun trackerPassThroughIgnoresInvalidHymns() = runTest {
          val h = Harness(this)
          h.async.onMediaCompleted("bogus", 1)
          h.async.onMediaCompleted(HymnTypes.BB, 2000)
          h.async.onMediaCompleted(HymnTypes.BB, 50)
          h.async.onMediaCompleted(null, 1)
          runCurrent()
          assertThat(h.singLogs.rows).isEmpty()
          h.async.onMediaCompleted(HymnTypes.DB, 1)
          runCurrent()
          assertThat(h.singLogs.rows).hasSize(1)
      }

      @Test
      fun observeAutoRecordedDeliversUntilCancelled() = runTest {
          val h = Harness(this)
          val received = mutableListOf<Outcome<SingLogEntity>>()
          val subscription = h.async.observeAutoRecorded { received += it }
          h.async.onMediaCompleted(HymnTypes.DB, 1)
          runCurrent()
          assertThat(received).hasSize(1)

          subscription.cancel()
          h.async.onMediaCompleted(HymnTypes.DB, 2)
          runCurrent()
          assertThat(received).hasSize(1)
      }

      @Test
      fun autoRecordSwitchReadsAndWritesPrefs() = runTest {
          val h = Harness(this)
          assertThat(h.async.isAutoRecordEnabled()).isTrue()
          h.async.setAutoRecordEnabled(false)
          assertThat(h.prefs.autoRecordEnabled).isFalse()
      }
  }
  ```

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.notebook.NotebookAsyncTest'`
  Expected: 編譯失敗，`Unresolved reference 'NotebookAsync'`。

- [ ] **Step 3：實作** `NotebookAsync.kt`

  ```kotlin
  package org.cog.hymnchtv.notebook

  import android.net.Uri
  import kotlinx.coroutines.CoroutineDispatcher
  import kotlinx.coroutines.CoroutineScope
  import kotlinx.coroutines.SupervisorJob
  import kotlinx.coroutines.launch
  import kotlinx.coroutines.withContext
  import org.cog.hymnchtv.notebook.backup.BackupIo
  import org.cog.hymnchtv.notebook.backup.ExportResult
  import org.cog.hymnchtv.notebook.backup.ImportResult
  import org.cog.hymnchtv.notebook.data.SingStats
  import org.cog.hymnchtv.notebook.data.entity.FavoriteEntity
  import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.cog.hymnchtv.notebook.model.SingSource
  import org.cog.hymnchtv.notebook.record.SingTracker
  import org.cog.hymnchtv.notebook.repo.FavoriteRepository
  import org.cog.hymnchtv.notebook.repo.SingLogRepository
  import org.cog.hymnchtv.notebook.settings.NotebookPrefs
  import timber.log.Timber
  import kotlin.coroutines.cancellation.CancellationException

  /** Result of a NotebookAsync call. Java: `outcome.getOrNull()` / `outcome.errorOrNull()`. */
  sealed class Outcome<out T> {
      data class Ok<out T>(val value: T) : Outcome<T>()
      data class Err(val error: Throwable) : Outcome<Nothing>()

      fun getOrNull(): T? = (this as? Ok<T>)?.value
      fun errorOrNull(): Throwable? = (this as? Err)?.error
  }

  fun interface NotebookCallback<T> {
      fun onResult(outcome: Outcome<T>)
  }

  fun interface Cancellable {
      fun cancel()
  }

  /**
   * Callback API for Java callers (ContentHandler, MainActivity in the UI wave).
   * Repository work runs on [workDispatcher] (Dispatchers.IO in production; e.g. RoomSingLogRepository.create()
   * calls device.deviceId(), which may commit SharedPreferences once, so it must never run on the main thread).
   * Every callback runs on [callbackDispatcher] (the main thread in production) and is never invoked after
   * cancel(); activities cancel their calls in onDestroy so no result reaches a destroyed view.
   */
  class NotebookAsync(
      private val favorites: FavoriteRepository,
      private val singLogs: SingLogRepository,
      private val prefs: NotebookPrefs,
      private val tracker: SingTracker,
      private val backupIo: BackupIo,
      callbackDispatcher: CoroutineDispatcher,
      private val workDispatcher: CoroutineDispatcher,
  ) {
      private val scope = CoroutineScope(SupervisorJob() + callbackDispatcher)

      // ---- favorites ----

      fun isFavorite(key: HymnKey, callback: NotebookCallback<Boolean>): Cancellable =
          call(callback) { favorites.isFavorite(key) }

      fun toggleFavorite(key: HymnKey, callback: NotebookCallback<Boolean>): Cancellable =
          call(callback) { favorites.toggle(key) }

      fun favorites(callback: NotebookCallback<List<FavoriteEntity>>): Cancellable =
          call(callback) { favorites.findAll() }

      // ---- sing logs ----

      fun singStats(key: HymnKey, callback: NotebookCallback<SingStats>): Cancellable =
          call(callback) { singLogs.statsFor(key) }

      fun singLogsOf(key: HymnKey, callback: NotebookCallback<List<SingLogEntity>>): Cancellable =
          call(callback) { singLogs.findByHymn(key) }

      /**
       * Manual entry. Deliberately bypasses the 3-hour dedupe (the user says they sang it, e.g. twice in one
       * meeting); the UI may warn using singStats(). Also remembers [occasion] as the default for future auto logs.
       */
      fun recordManual(
          key: HymnKey,
          occasion: Occasion,
          sungAt: Long,
          callback: NotebookCallback<SingLogEntity>,
      ): Cancellable = call(callback) {
          singLogs.record(key, sungAt, occasion, SingSource.MANUAL).also { prefs.setLastChosenOccasion(occasion) }
      }

      /** User correction of a log; remembers the corrected occasion. Null when the log no longer exists. */
      fun updateSingLog(log: SingLogEntity, callback: NotebookCallback<SingLogEntity?>): Cancellable =
          call(callback) { singLogs.update(log)?.also { prefs.setLastChosenOccasion(it.occasion) } }

      fun deleteSingLog(id: String, callback: NotebookCallback<Boolean>): Cancellable =
          call(callback) { singLogs.delete(id) }

      // ---- auto record: main thread, non-blocking; invalid or non-canonical hymn numbers are ignored ----

      fun onHymnVisible(hymnType: String?, hymnNo: Int) {
          HymnKey.ofOrNull(hymnType, hymnNo)?.let(tracker::onHymnVisible)
      }

      fun onHymnHidden(hymnType: String?, hymnNo: Int) {
          HymnKey.ofOrNull(hymnType, hymnNo)?.let(tracker::onHymnHidden)
      }

      fun onMediaCompleted(hymnType: String?, hymnNo: Int) {
          HymnKey.ofOrNull(hymnType, hymnNo)?.let(tracker::onMediaCompleted)
      }

      /** Delivers each new AUTO log on the callback dispatcher (e.g. to refresh the sing count or offer undo). */
      fun observeAutoRecorded(listener: NotebookCallback<SingLogEntity>): Cancellable {
          val job = scope.launch { tracker.recorded.collect { listener.onResult(Outcome.Ok(it)) } }
          return Cancellable { job.cancel() }
      }

      fun isAutoRecordEnabled(): Boolean = prefs.autoRecordEnabled

      fun setAutoRecordEnabled(enabled: Boolean) = prefs.setAutoRecordEnabled(enabled)

      // ---- backup (Uris come from ActivityResultContracts.CreateDocument / OpenDocument) ----

      fun exportTo(uri: Uri, callback: NotebookCallback<ExportResult>): Cancellable =
          call(callback) { backupIo.exportTo(uri) }

      fun importFrom(uri: Uri, callback: NotebookCallback<ImportResult>): Cancellable =
          call(callback) { backupIo.importFrom(uri) }

      private fun <T> call(callback: NotebookCallback<T>, block: suspend () -> T): Cancellable {
          val job = scope.launch {
              val outcome: Outcome<T> = try {
                  Outcome.Ok(withContext(workDispatcher) { block() })
              } catch (e: CancellationException) {
                  throw e
              } catch (e: Exception) {
                  Timber.w(e, "Notebook call failed")
                  Outcome.Err(e)
              }
              callback.onResult(outcome)
          }
          return Cancellable { job.cancel() }
      }
  }
  ```

- [ ] **Step 4：實作** `Notebook.kt`

  ```kotlin
  package org.cog.hymnchtv.notebook

  import android.content.Context
  import kotlinx.coroutines.CoroutineExceptionHandler
  import kotlinx.coroutines.CoroutineScope
  import kotlinx.coroutines.Dispatchers
  import kotlinx.coroutines.SupervisorJob
  import org.cog.hymnchtv.BuildConfig
  import org.cog.hymnchtv.notebook.backup.BackupService
  import org.cog.hymnchtv.notebook.backup.RoomBackupStore
  import org.cog.hymnchtv.notebook.backup.UriBackupIo
  import org.cog.hymnchtv.persistance.room.HymnchtvDatabase
  import org.cog.hymnchtv.notebook.model.Clock
  import org.cog.hymnchtv.notebook.model.IdGenerator
  import org.cog.hymnchtv.notebook.record.SingTracker
  import org.cog.hymnchtv.notebook.repo.FavoriteRepository
  import org.cog.hymnchtv.notebook.repo.NoteRepository
  import org.cog.hymnchtv.notebook.repo.PlaylistRepository
  import org.cog.hymnchtv.notebook.repo.SingLogRepository
  import org.cog.hymnchtv.notebook.repo.room.RoomFavoriteRepository
  import org.cog.hymnchtv.notebook.repo.room.RoomNoteRepository
  import org.cog.hymnchtv.notebook.repo.room.RoomPlaylistRepository
  import org.cog.hymnchtv.notebook.repo.room.RoomSingLogRepository
  import org.cog.hymnchtv.notebook.settings.NotebookPrefs
  import org.cog.hymnchtv.notebook.settings.SharedPrefsNotebookPrefs
  import timber.log.Timber

  /**
   * Everything the notebook needs, built once per process. [appScope] owns background work (SingTracker timers
   * and writes): SupervisorJob + Dispatchers.Default, process lifetime, never cancelled explicitly.
   * Kotlin UI uses the repositories directly; Java uses [async].
   */
  class NotebookGraph internal constructor(
      val database: HymnchtvDatabase,
      val appScope: CoroutineScope,
      val favorites: FavoriteRepository,
      val singLogs: SingLogRepository,
      val notes: NoteRepository,
      val playlists: PlaylistRepository,
      val prefs: NotebookPrefs,
      val tracker: SingTracker,
      val backup: BackupService,
      val async: NotebookAsync,
  )

  /** Lazy process-wide entry point; no HymnsApp change needed. Java: `Notebook.async(context)`. */
  object Notebook {
      @Volatile
      private var graph: NotebookGraph? = null

      @JvmStatic
      fun get(context: Context): NotebookGraph =
          graph ?: synchronized(this) { graph ?: create(context.applicationContext).also { graph = it } }

      @JvmStatic
      fun async(context: Context): NotebookAsync = get(context).async

      private fun create(app: Context): NotebookGraph {
          val clock = Clock.SYSTEM
          val ids = IdGenerator.RANDOM_UUID
          val db = HymnchtvDatabase.getInstance(app)
          val appScope = CoroutineScope(
              SupervisorJob() + Dispatchers.Default +
                  CoroutineExceptionHandler { _, e -> Timber.e(e, "Notebook background task failed") },
          )
          val prefs = SharedPrefsNotebookPrefs(app)
          val favorites = RoomFavoriteRepository(db, clock, prefs)
          val singLogs = RoomSingLogRepository(db, clock, ids, prefs)
          val tracker = SingTracker(singLogs, prefs, clock, appScope)
          val backup = BackupService(RoomBackupStore(db), clock, BuildConfig.VERSION_NAME)
          val async = NotebookAsync(
              favorites, singLogs, prefs, tracker, UriBackupIo(app.contentResolver, backup),
              callbackDispatcher = Dispatchers.Main.immediate,
              workDispatcher = Dispatchers.IO,
          )
          return NotebookGraph(
              database = db,
              appScope = appScope,
              favorites = favorites,
              singLogs = singLogs,
              notes = RoomNoteRepository(db, clock, ids, prefs),
              playlists = RoomPlaylistRepository(db, clock, ids, prefs),
              prefs = prefs,
              tracker = tracker,
              backup = backup,
              async = async,
          )
      }
  }
  ```

- [ ] **Step 5：執行測試，確認它通過**

  Run: `./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug --console=plain`
  Expected: `NotebookAsyncTest` 9 個測試通過，其他 JVM 測試也都通過；BUILD SUCCESSFUL。

- [ ] **Step 6：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/notebook/Notebook.kt hymnchtv/src/main/java/org/cog/hymnchtv/notebook/NotebookAsync.kt hymnchtv/src/test/java/org/cog/hymnchtv/notebook/NotebookAsyncTest.kt hymnchtv/src/test/java/org/cog/hymnchtv/notebook/fakes/InMemoryFavoriteRepository.kt
  git commit -m "feat: wire the notebook graph and add a callback API for Java callers" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

### Task 13：E2E 驗證（模擬器）與審查（線性，`feat/notebook-data`）

**Files:**
- Create: `hymnchtv/src/androidTest/java/org/cog/hymnchtv/notebook/NotebookE2eTest.kt`
- 不進 repo 的腳本: `/private/tmp/claude-501/d1a-e2e/d1a-e2e.sh`

**關於這個測試：**
- 它只在用 `am instrument -e notebookE2e <mode>` 明確指定時執行；一般的 `connectedDebugAndroidTest` 會因為 `assumeTrue` 而跳過它。
- 它操作的是 app 真正的 `hymnchtv.db`（統一資料庫）。
- 種子資料的時間固定在 2026-09-30～10-01（UTC）。因為有 `sungAt ≤ 現在＋24 小時` 的驗證，模擬器的日期必須不早於 2026-10-01，請先確認 `adb shell date`。
- D-1a 沒有 UI，所以用 `file://` Uri（`ContentResolver` 支援）代替 SAF 的檔案選擇器；`BackupDocuments` 走的程式路徑和 SAF 完全相同。真正透過檔案選擇器（本機、Google 雲端硬碟）的手動測試，留到 UI 階段。

**破壞性步驟（Step 3～5）的規則：**
- 會解除安裝 app、切換 backup transport，所以**必須明確 opt-in**。
- 協調者先確認 `api34b`／`api24b` 沒有其他代理在使用，並取得使用者同意，才能設定 `D1A_ALLOW_DESTRUCTIVE`。
- 腳本會先確認目標 serial 對應的 AVD 名稱正確，才執行任何動作。

- [ ] **Step 1：寫 E2E 測試**

  ```kotlin
  package org.cog.hymnchtv.notebook

  import android.content.Context
  import android.net.Uri
  import androidx.test.core.app.ApplicationProvider
  import androidx.test.ext.junit.runners.AndroidJUnit4
  import androidx.test.platform.app.InstrumentationRegistry
  import com.google.common.truth.Truth.assertThat
  import kotlinx.coroutines.Dispatchers
  import kotlinx.coroutines.delay
  import kotlinx.coroutines.runBlocking
  import kotlinx.coroutines.withContext
  import org.cog.hymnchtv.MainActivity
  import org.cog.hymnchtv.notebook.backup.BackupDocuments
  import org.cog.hymnchtv.notebook.backup.ExportResult
  import org.cog.hymnchtv.notebook.backup.ImportResult
  import org.cog.hymnchtv.notebook.backup.RoomBackupStore
  import org.cog.hymnchtv.notebook.backup.SkippedRows
  import org.cog.hymnchtv.notebook.data.SingStats
  import org.cog.hymnchtv.notebook.model.HymnKey
  import org.cog.hymnchtv.notebook.model.HymnTypes
  import org.cog.hymnchtv.notebook.model.Occasion
  import org.cog.hymnchtv.notebook.model.SingSource
  import org.cog.hymnchtv.persistance.room.entity.HymnHistoryEntity
  import org.junit.Assert.fail
  import org.junit.Assume.assumeTrue
  import org.junit.Test
  import org.junit.runner.RunWith
  import java.io.File

  /**
   * Manual E2E driver (Task 13), run only through d1a-e2e.sh.
   * Modes: seed, export, verifyEmpty, import, importAgain, verifySeeded, writeLoop, integrity.
   */
  @RunWith(AndroidJUnit4::class)
  class NotebookE2eTest {
      private val mode: String? = InstrumentationRegistry.getArguments().getString(ARG_MODE)
      private val context: Context = ApplicationProvider.getApplicationContext()
      private val graph by lazy { Notebook.get(context) }
      private val file by lazy { File(context.filesDir, "e2e/notebook-e2e.json") }

      @Test
      fun scenario(): Unit = runBlocking {
          assumeTrue("Run only via am instrument -e $ARG_MODE <mode>", mode != null)
          when (mode) {
              "seed" -> seed()
              "export" -> export()
              "verifyEmpty" -> assertThat(tables().rowCount).isEqualTo(0)
              "import" -> importAndCheck(expectInserted = TOTAL_ROWS)
              "importAgain" -> importAndCheck(expectInserted = 0)
              "verifySeeded" -> verifySeeded(checkPrefs = true)
              "writeLoop" -> writeLoop()
              "integrity" -> integrity()
              else -> fail("Unknown mode $mode")
          }
      }

      private suspend fun tables() = RoomBackupStore(graph.database).readAll()

      /** The legacy app settings file (MainActivity.PREF_SETTINGS), included in Auto Backup. */
      private fun settings() = context.getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE)

      private suspend fun seed() {
          assertThat(tables().rowCount).isEqualTo(0)
          graph.favorites.setFavorite(DB1, true)
          graph.favorites.setFavorite(FU1, true)
          graph.favorites.setFavorite(BB5, true)
          graph.favorites.setFavorite(BB5, false)
          graph.singLogs.record(DB1, T1, Occasion.LORDS_DAY, SingSource.MANUAL)
          graph.singLogs.record(DB1, T2, Occasion.HOME, SingSource.AUTO)
          val removed = graph.singLogs.record(FU1, T1, Occasion.SMALL_GROUP, SingSource.MANUAL)
          graph.singLogs.delete(removed.id)
          graph.notes.add(DB1, "主日唱這首，很受感動。")
          graph.notes.add(FU1, "附歌一的筆記")
          val playlist = graph.playlists.createPlaylist("主日 10/4")
          graph.playlists.addItem(playlist.id, DB1)
          graph.playlists.addItem(playlist.id, FU1)
          // A legacy-table row (not part of the notebook JSON backup); only Auto Backup carries it across a restore.
          withContext(Dispatchers.IO) { graph.database.hymnHistoryDao().insert(LEGACY_HISTORY) }
          graph.prefs.setAutoRecordEnabled(false)
          settings().edit().putString(SETTINGS_MARKER_KEY, SETTINGS_MARKER_VALUE).commit()
          verifySeeded(checkPrefs = true)
      }

      private suspend fun export() {
          file.parentFile?.mkdirs()
          val result = BackupDocuments.exportTo(context.contentResolver, Uri.fromFile(file), graph.backup)
          assertThat(result).isEqualTo(ExportResult.Success(TOTAL_ROWS))
      }

      private suspend fun importAndCheck(expectInserted: Int) {
          val result = BackupDocuments.importFrom(context.contentResolver, Uri.fromFile(file), graph.backup)
          assertThat(result).isInstanceOf(ImportResult.Success::class.java)
          val success = result as ImportResult.Success
          assertThat(success.stats.inserted).isEqualTo(expectInserted)
          assertThat(success.stats.inserted + success.stats.updated + success.stats.unchanged).isEqualTo(TOTAL_ROWS)
          assertThat(success.skipped).isEqualTo(SkippedRows.NONE)
          verifySeeded(checkPrefs = false) // import never touches prefs
      }

      private suspend fun verifySeeded(checkPrefs: Boolean) {
          val all = tables()
          assertThat(all.favorites).hasSize(3)
          assertThat(all.singLogs).hasSize(3)
          assertThat(all.notes).hasSize(2)
          assertThat(all.playlists).hasSize(1)
          assertThat(all.playlistItems).hasSize(2)
          assertThat(graph.favorites.findAll().map { it.hymn }).containsExactly(DB1, FU1)
          assertThat(graph.favorites.isFavorite(BB5)).isFalse()
          if (checkPrefs) { // legacy rows are not in the JSON backup, so only the Auto Backup restore carries them
              val history = withContext(Dispatchers.IO) { graph.database.hymnHistoryDao().listNewestFirst() }
              assertThat(history).contains(LEGACY_HISTORY)
          }
          assertThat(graph.singLogs.statsFor(DB1)).isEqualTo(SingStats(2, T2))
          assertThat(graph.singLogs.statsFor(FU1)).isEqualTo(SingStats(0, null))
          assertThat(graph.notes.findByHymn(FU1).map { it.body }).containsExactly("附歌一的筆記")
          val playlist = graph.playlists.findAll().single()
          assertThat(graph.playlists.items(playlist.id).map { it.hymn }).containsExactly(DB1, FU1).inOrder()
          if (checkPrefs) {
              assertThat(graph.prefs.autoRecordEnabled).isFalse()
              assertThat(settings().getString(SETTINGS_MARKER_KEY, null)).isEqualTo(SETTINGS_MARKER_VALUE)
          }
      }

      /** Keeps writing for ~30 s so the script can trigger Auto Backup mid-write. */
      private suspend fun writeLoop() {
          repeat(WRITE_LOOP_COUNT) { i ->
              graph.singLogs.record(DB1, T2 + 60_000L * (i + 1), Occasion.HOME, SingSource.MANUAL)
              delay(100)
          }
      }

      private suspend fun integrity() {
          val check = withContext(Dispatchers.IO) {
              graph.database.openHelper.writableDatabase.query("PRAGMA integrity_check").use { cursor ->
                  cursor.moveToFirst()
                  cursor.getString(0)
              }
          }
          assertThat(check).isEqualTo("ok")
          assertThat(tables().singLogs.size).isAtLeast(3)
          assertThat(graph.favorites.findAll()).hasSize(2)
          val history = withContext(Dispatchers.IO) { graph.database.hymnHistoryDao().listNewestFirst() }
          assertThat(history).contains(LEGACY_HISTORY)
      }

      private companion object {
          const val ARG_MODE = "notebookE2e"
          const val TOTAL_ROWS = 11 // favorites 3 + sing logs 3 + notes 2 + playlists 1 + items 2
          const val T1 = 1_790_733_600_000L
          const val T2 = T1 + 86_400_000L
          const val WRITE_LOOP_COUNT = 300
          const val SETTINGS_MARKER_KEY = "notebook_e2e_marker"
          const val SETTINGS_MARKER_VALUE = "restored"
          val LEGACY_HISTORY = HymnHistoryEntity("hymn_db", 12, false, "legacy-row", 1_759_300_000_000L)
          val DB1 = HymnKey.of(HymnTypes.DB, 1)
          val FU1 = HymnKey.of(HymnTypes.DB, 781)
          val BB5 = HymnKey.of(HymnTypes.BB, 5)
      }
  }
  ```

  Run: `./gradlew :hymnchtv:assembleDebugAndroidTest`
  Expected: BUILD SUCCESSFUL。

  Commit：

  ```bash
  git add hymnchtv/src/androidTest/java/org/cog/hymnchtv/notebook/NotebookE2eTest.kt
  git commit -m "test: add notebook E2E driver for backup and Auto Backup verification" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

- [ ] **Step 2：自動化檢查（非破壞性）**

  ```bash
  ./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug :hymnchtv:lintDebug --console=plain
  for k in MissingTranslation ExtraTranslation HardcodedText; do printf "%s: " $k; grep -c "\[$k\]" hymnchtv/build/reports/lint-results-debug.txt; done
  for pair in emulator-5580:api34b emulator-5582:api24b; do
    serial=${pair%%:*}; avd=${pair##*:}
    test "$(adb -s $serial emu avd name | head -1 | tr -d '\r')" = "$avd" || { echo "ABORT: $serial is not $avd"; break; }
    ANDROID_SERIAL=$serial ./gradlew :hymnchtv:connectedDebugAndroidTest --console=plain
  done
  ```

  Expected:
  - API 34 和 API 24 都全部通過；`NotebookE2eTest` 顯示為 skipped。
  - 三個 lint 數字不比分支起點（`master` 或 `feat/zh-hant`）的基準值多（D-1a 沒有新增字串）。

- [ ] **Step 3：建立破壞性 E2E 腳本（存放在 scratch，不進 repo）**

  ```bash
  mkdir -p /private/tmp/claude-501/d1a-e2e
  cat > /private/tmp/claude-501/d1a-e2e/d1a-e2e.sh <<'SH'
  #!/usr/bin/env bash
  # Usage (from the repo root): D1A_ALLOW_DESTRUCTIVE=<avd> d1a-e2e.sh <serial> <avd> <saf|autobackup|busy>
  set -euo pipefail
  SERIAL=$1; AVD=$2; SCENARIO=$3
  case "$AVD" in api34b|api24b) ;; *) echo "ABORT: only the D-1a AVDs api34b/api24b are allowed"; exit 2;; esac
  [ "${D1A_ALLOW_DESTRUCTIVE:-}" = "$AVD" ] || { echo "ABORT: set D1A_ALLOW_DESTRUCTIVE=$AVD to confirm"; exit 2; }
  ACTUAL=$(adb -s "$SERIAL" emu avd name | head -1 | tr -d '\r')
  [ "$ACTUAL" = "$AVD" ] || { echo "ABORT: $SERIAL is '$ACTUAL', not $AVD"; exit 2; }
  export ANDROID_SERIAL=$SERIAL
  REPO=$(git rev-parse --show-toplevel)
  PKG=com.ziontkec.hymnal   # applicationId; org.cog.hymnchtv is only the namespace
  RUNNER=$PKG.test/androidx.test.runner.AndroidJUnitRunner
  OUT=/private/tmp/claude-501/d1a-e2e/$AVD; mkdir -p "$OUT"
  SDK=$(adb shell getprop ro.build.version.sdk | tr -d '\r')

  run() {
    local out
    out=$(adb shell am instrument -w -e class org.cog.hymnchtv.notebook.NotebookE2eTest -e notebookE2e "$1" "$RUNNER")
    echo "$out"
    echo "$out" | grep -q "OK (1 test)" || { echo "FAILED: mode $1"; exit 1; }
  }
  install_app()  { (cd "$REPO" && ./gradlew -q :hymnchtv:installDebug); }
  install_test() { (cd "$REPO" && ./gradlew -q :hymnchtv:installDebugAndroidTest); }
  uninstall_all() { adb uninstall $PKG.test >/dev/null 2>&1 || true; adb shell pm uninstall --user 0 $PKG >/dev/null 2>&1 || true; }
  local_transport() {
    adb shell bmgr enable true
    local lt
    lt=$(adb shell bmgr list transports | tr -d '\r *' | grep -i localtransport | head -1)
    [ -n "$lt" ] || { echo "ABORT: no local transport"; exit 1; }
    adb shell bmgr transport "$lt"
    if [ "$SDK" -ge 28 ]; then adb shell settings put secure backup_local_transport_parameters 'is_encrypted=true'; fi
  }
  backup_now() {
    local out
    out=$(adb shell bmgr backupnow $PKG 2>&1 | tr -d '\r') || true
    echo "$out" | tee -a "$OUT/backupnow.txt"
    if echo "$out" | grep -qiE "unknown command|usage"; then adb shell bmgr fullbackup $PKG | tee -a "$OUT/backupnow.txt"; fi
  }
  restore_gms_transport() { adb shell bmgr transport com.google.android.gms/.backup.BackupTransportService >/dev/null 2>&1 || true; }
  # Note: `! grep` would not abort under `set -e`, so every check exits explicitly.
  # Sidecars (hymnchtv.db-wal / -shm) are transient: record them in restored-files.txt, never assert on them.
  require_file() { grep -qx "$1" "$OUT/restored-files.txt" || { echo "FAILED: $1 was not restored"; exit 1; }; }
  forbid_file()  { if grep -q "$1" "$OUT/restored-files.txt"; then echo "FAILED: $1 must not be restored"; exit 1; fi; }
  check_restored_files() {
    adb shell run-as $PKG ls databases shared_prefs | tr -d '\r' | tee "$OUT/restored-files.txt"
    require_file hymnchtv.db; require_file notebook.xml; require_file Settings.xml
    forbid_file dbHymnApp; forbid_file notebook.db; forbid_file notebook_device; forbid_file store.xml
  }

  case "$SCENARIO" in
    saf)
      adb shell bmgr enable false            # keep Auto Backup out of this scenario
      uninstall_all; install_app; install_test
      run seed; run export
      adb exec-out run-as $PKG cat files/e2e/notebook-e2e.json > "$OUT/notebook-e2e.json"
      python3 - "$OUT/notebook-e2e.json" <<'PY'
  import json, sys
  d = json.load(open(sys.argv[1]))
  counts = [len(d[k]) for k in ("favorites", "singLogs", "notes", "playlists", "playlistItems")]
  assert d["format"] == "hymnchtv-notebook" and d["schemaVersion"] == 1 and counts == [3, 3, 2, 1, 2], (d["format"], counts)
  assert all("updatedBy" in r for k in ("favorites", "singLogs", "notes", "playlists", "playlistItems") for r in d[k])
  print("export OK", d["appVersionName"], counts)
  PY
      uninstall_all; install_app; install_test
      run verifyEmpty
      adb shell run-as $PKG mkdir -p files/e2e
      adb exec-in "run-as $PKG sh -c 'cat > files/e2e/notebook-e2e.json'" < "$OUT/notebook-e2e.json"
      run import; run importAgain
      ;;
    autobackup)
      trap restore_gms_transport EXIT
      uninstall_all; install_app; install_test
      run seed
      local_transport; backup_now
      uninstall_all; install_app                 # install triggers the restore
      check_restored_files
      install_test; run verifySeeded
      ;;
    busy)
      trap restore_gms_transport EXIT
      uninstall_all; install_app; install_test
      run seed
      local_transport
      adb shell am instrument -w -e class org.cog.hymnchtv.notebook.NotebookE2eTest -e notebookE2e writeLoop "$RUNNER" \
        > "$OUT/writeloop.txt" 2>&1 &
      WRITER=$!
      sleep 5
      echo "pid before backup: $(adb shell pidof $PKG | tr -d '\r' || true)" | tee "$OUT/busy-pids.txt"
      backup_now
      echo "pid after backup:  $(adb shell pidof $PKG | tr -d '\r' || true)" | tee -a "$OUT/busy-pids.txt"
      wait $WRITER || true
      tail -5 "$OUT/writeloop.txt"
      uninstall_all; install_app
      check_restored_files
      install_test; run integrity
      ;;
    *) echo "unknown scenario $SCENARIO"; exit 2;;
  esac
  echo "SCENARIO $SCENARIO on $AVD: PASS"
  SH
  chmod +x /private/tmp/claude-501/d1a-e2e/d1a-e2e.sh
  ```

  注意：
  - 從本文複製時，要去掉清單的兩格縮排，讓 heredoc 的結束標記 `SH` 和內層的 `PY` 位於行首；否則 heredoc 不會結束。
  - 建立後先跑 `bash -n /private/tmp/claude-501/d1a-e2e/d1a-e2e.sh` 檢查語法。

  Expected: 腳本建立完成，`bash -n` 沒有輸出。**下一步之前**，先向協調者／使用者確認 `api34b`、`api24b` 沒有其他代理在使用，並取得同意。

- [ ] **Step 4：SAF 路徑：匯出 → 解除安裝 → 重新安裝 → 匯入（破壞性，需 opt-in）**

  ```bash
  D1A_ALLOW_DESTRUCTIVE=api34b /private/tmp/claude-501/d1a-e2e/d1a-e2e.sh emulator-5580 api34b saf
  D1A_ALLOW_DESTRUCTIVE=api24b /private/tmp/claude-501/d1a-e2e/d1a-e2e.sh emulator-5582 api24b saf
  ```

  Expected:
  - 每次都印出 `export OK 1.0.0（即 BuildConfig.VERSION_NAME）[3, 3, 2, 1, 2]`，最後是 `SCENARIO saf on <avd>: PASS`。
  - `verifyEmpty` 通過，證明重新安裝後資料是空的、匯入前沒有被自動備份還原。
  - `import` 新增 11 列；`importAgain` 新增 0 列（idempotent）。

- [ ] **Step 5：自動備份，以及「寫入中觸發備份」（破壞性，需 opt-in）**

  ```bash
  D1A_ALLOW_DESTRUCTIVE=api34b /private/tmp/claude-501/d1a-e2e/d1a-e2e.sh emulator-5580 api34b autobackup
  D1A_ALLOW_DESTRUCTIVE=api24b /private/tmp/claude-501/d1a-e2e/d1a-e2e.sh emulator-5582 api24b autobackup
  D1A_ALLOW_DESTRUCTIVE=api34b /private/tmp/claude-501/d1a-e2e/d1a-e2e.sh emulator-5580 api34b busy
  ```

  Expected:
  - `autobackup`：
    - `backupnow.txt` 含 `Package com.ziontkec.hymnal with result: Success`。
    - 還原的檔案包含 `hymnchtv.db`、`notebook.xml`、`Settings.xml`，**不包含** `dbHymnApp*`、`notebook.db`、`notebook_device.xml`。`-wal`／`-shm` 是暫態檔（還原後 Room 可能已 checkpoint 並刪除它們），**只記錄**（腳本把還原後的檔案清單寫進 `restored-files.txt`），**不斷言**存在與否；三個 `<include>` 由 `BackupRulesTest` 驗證。
    - 驗證主 DB 已還原且資料正確：`verifySeeded` 除了筆記本五表，還要確認至少一筆舊三表資料（例如種入一筆 `hymn_history` 或 `media_record`，還原後仍在且內容相同）與至少一筆 notebook 資料。`seed` 要用 `HymnchtvDatabase.hymnHistoryDao().insert(...)`（或 `mediaRecordDao()`）種入舊表資料，`verifySeeded` 與 `integrity` 要斷言它存在。
    - `verifySeeded` 通過（含 `autoRecordEnabled == false` 和 `Settings.xml` 的 marker）。
    - API 34 驗證的是 `dataExtractionRules`，API 24 驗證的是 `fullBackupContent`。
  - `busy`：**記錄**以下觀察，不要預設結果：
    - `busy-pids.txt` 的兩行（依官方文件，app 應該被關閉，after 會是空的或換了 pid）。
    - `backupnow.txt` 的結果（可能是 Success，也可能延後）。
    - `writeloop.txt` 的最後幾行（預期 instrumentation 被中斷）。
  - 無論備份結果如何，還原後的 `integrity` 都必須通過（`PRAGMA integrity_check` 是 `ok`）。
  - 任一步失敗時，查 `adb logcat -d -s BackupManagerService PFTBT Backup`，把相關訊息貼進回報；不要為了讓結果通過而修改規則檔。

- [ ] **Step 6：審查**

  - 用 `superpowers:requesting-code-review` 或 code-reviewer agent 審查 `git diff origin/master...feat/notebook-data`（如果 A 還沒合併，改成 `feat/zh-hant...feat/notebook-data`）。
  - 審查重點：
    - 去重的原子性。
    - 合併排序是否確定。
    - 匯入驗證（id、hymn key、時間、上限）。
    - 歌單位置的 unique index。
    - soft delete 的一致性。
    - `NotebookAsync` 的取消與執行緒。
    - 備份規則是否漏掉或多包含檔案。
  - 用 `/codex review` 做第二份獨立審查。
  - 有 P1 就修正，並重跑 Step 2。

- [ ] **Step 7：準備 PR**

  - PR 目標是 `master`。如果分支是從 `feat/zh-hant` 開的，要等 A 的 PR 合併、依 Task 0 Step 1 rebase，並重跑 Step 2 之後，才能開 PR。
  - PR 描述包含：
    - Task 0 的關卡結果，包括實際採用的 coroutines 版本，以及 AGP 是否在測試後解除安裝。
    - Step 2、4、5 在兩台模擬器上的結果表格，包括 `busy` 的觀察。
    - 這份計畫「設計決策」的摘要，**包括「時鐘的限制」和「隱私上的取捨」兩段原文**。
    - 「給 UI 階段的介面」的連結。
  - 新分支 push 時使用 `-u`；PR 描述結尾加上 `🤖 Generated with [Claude Code](https://claude.com/claude-code)`。

---

## 給 UI 階段的介面（A2 之後才接，D-1a 不修改這些檔案）

**Java 入口：** `NotebookAsync notebook = Notebook.async(context);`。所有 callback 都在主執行緒執行；回傳的 `Cancellable` 要在 `onDestroy` 呼叫 `cancel()`。

```java
// ContentHandler: favorite star
Cancellable favCall = Notebook.async(this).isFavorite(HymnKey.of(hymnType, hymnNo), outcome -> {
    Boolean fav = outcome.getOrNull();
    if (fav != null) starButton.setSelected(fav);
});
starButton.setOnClickListener(v -> Notebook.async(this).toggleFavorite(HymnKey.of(hymnType, hymnNo),
        outcome -> starButton.setSelected(Boolean.TRUE.equals(outcome.getOrNull()))));
```

| 呼叫點（UI 階段修改） | 呼叫 |
|---|---|
| `ContentHandler` 的 `onPageSelected(position)` | `notebook.onHymnVisible(hymnType, 該頁的 hymnNo)`。tracker 會自動停掉上一首的計時。 |
| `ContentHandler.onResume()` | `notebook.onHymnVisible(hymnType, 目前頁的 hymnNo)` |
| `ContentHandler.onPause()` | `notebook.onHymnHidden(hymnType, 目前頁的 hymnNo)` |
| `MediaExoPlayerFragment` 的 `STATE_ENDED` | `notebook.onMediaCompleted(hymnType, hymnNo)` |
| 歌詞頁「唱過 N 次・最近 yyyy/MM/dd」 | <ul><li>用 `singStats(key, cb)` 顯示。</li><li>Java 用 `observeAutoRecorded(cb)` 在自動記錄後更新；Kotlin 改用 `lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) { graph.tracker.recorded.collect { … } } }`。</li><li>自動記錄後顯示「已記錄唱詩（可修正）」。</li></ul> |
| 唱詩紀錄的修正、刪除、手動補記 | <ul><li>`updateSingLog`、`deleteSingLog`、`recordManual`。</li><li>`recordManual` **不去重**：手動補記時，如果 `singStats` 顯示 3 小時內已有紀錄，UI 可以先提示使用者。</li></ul> |
| 「我的詩歌」畫面 | Kotlin 直接用 `Notebook.get(context).favorites`、`singLogs.findBetween(...)`、`playlists` |
| 設定：自動記錄開關 | `isAutoRecordEnabled()`／`setAutoRecordEnabled(boolean)`；2 分鐘門檻不開放設定 |
| 備份匯出 | <ul><li>`registerForActivityResult(new ActivityResultContracts.CreateDocument(BackupDocuments.MIME_TYPE), uri -> notebook.exportTo(uri, cb))`</li><li>`launch(BackupFileName.suggested(System.currentTimeMillis()))`</li></ul> |
| 備份匯入 | <ul><li>`registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> notebook.importFrom(uri, cb))`</li><li>`launch(BackupDocuments.OPEN_MIME_TYPES)`</li><li>`ImportResult.Failure.error` 要對應到在地化字串。</li><li>`ImportResult.Success.skipped` 不為零時，顯示「略過 N 筆（其中 M 筆時間在未來）」。</li></ul> |

- `HymnKey.of()` 收到不合法或非 canonical 的值會丟例外。
- `onHymnVisible` 等三個 tracker 方法改用 `ofOrNull`，所以傳入 `HYMN_BB_DUMMY`、補充本的空號或錯誤類別時，會被安靜忽略。

---

## 已決定事項（協調者，2026-10-02）

1. **自動備份的範圍**：`hymnchtv.db`（統一 DB，含 `-wal`／`-shm`）、`notebook.xml`、`Settings.xml`。
   - 備份整個統一 DB（Room 統一計畫 §2.5 決定）：Auto Backup 的 database domain 只能以檔案為粒度，無法只選某幾張表，所以媒體連結、歷史、英文歌詞與筆記本一併備份；這些都是使用者資料／設定，不含憑證或機密。舊的 `dbHymnApp.db`、`notebook.db` 已不存在。Auto Backup 會先關閉 app，所以「複製時不一致」的風險其實比較小，見設計決策。若日後只想備份筆記本，必須改成可篩選的邏輯備份（`BackupService` 的 SAF 匯出已是此類）。
   - `notebook_device.xml` 刻意不備份。
   - `Settings.xml` 的 `PREF_WALLPAPER` 由 A2／UI 階段負責退回預設背景。
2. **場合推測**：
   - 週日 06:00～13:00 → 主日。
   - 其他時間 → 上次手動選的場合；如果上次選的是主日，改成家中。
   - 從沒選過 → 家中。
3. **不使用 `disableIfNoEncryptionCapabilities`**：隱私上的取捨見設計決策，日後寫進隱私權政策。
4. **2 分鐘門檻維持程式常數**（YAGNI）。使用者能設定的只有「自動記錄」開關。
5. **分支**：`feat/d1a-notebook-data` 從 `master` 開（A 還沒合併時，先從 `feat/zh-hant` 開，之後再 rebase）。PR 的目標是 `master`。
6. **Codex 審查（rev 3）採納的規則**：
   - 自動記錄的去重是原子操作；手動補記和匯入不去重。
   - last-writer-wins 的排序鍵是 `(version, 已刪除, updatedBy, canonicalContent)`，是一個全序。
   - 匯入的歌單項目分兩階段寫入。
   - `sungAt` 必須在 0～現在＋24 小時之間；`DedupeWindow` 使用飽和運算。
   - 匯入時拒絕晚於匯入當下 24 小時以上的列。
   - 子項目 S 改用伺服器版本號。
