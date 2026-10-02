# 子項目 Z：新身分發佈 實作計畫（rev 2）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**rev 2（2026-10-02）**：依使用者決策與 Codex 審查（4 個 P1、5 個 P2）改寫。主要變更：
- App 內不再有任何說明網址，改成 App 內的說明文字。
- 授權清單改由 AboutLibraries 自動產生。
- 主畫面標題改為「詩歌」。
- 媒體連結只隨 APK 內附。
- 更新安裝前要驗證：SHA-256、套件名稱、簽章憑證、versionCode。
- 安裝流程改走通知；未知來源權限要先導引。
- About 頁改用原生元件。
- 共用檔案的修改集中在一個整合 task，等 A2、B 合併後才做。

## 給執行者（Sonnet 5.5）的說明

使用者決定由 **Sonnet 5.5（`claude-sonnet-5-5`）** 執行這份計畫。建議的啟動方式：

1. 在 repo 根目錄 `/Users/hitobias/orca/hymnchtv` 執行 `claude --model claude-sonnet-5-5`。
2. 輸入：

   > 使用 superpowers:subagent-driven-development 執行 `docs/superpowers/plans/2026-10-02-z-new-identity-implementation.md`。先做 Task 0、Task 1，再用三個 worktree 平行跑 Lane A（Task 2–6）、Lane B（Task 7–10）、Lane C（Task 11）。Task 12 必須等使用者確認 A2 與 B 都已合併進 `feat/zh-hant` 才開始。子代理一律用 Sonnet。計畫和程式碼對不上就停下來問我。

**順序（必須遵守）：**

```
Task 0 → Task 1 ──┬─ Lane A：Task 2 → 3 → 4 → 5 → 6 ─┐
                  ├─ Lane B：Task 7 → 8 → 9 → 10 ─────┼─→ [閘門：A2、B 已合併] → Task 12（整合）→ Task 13（模擬器）→ Task 14（審查、PR）
                  └─ Lane C：Task 11 ─────────────────┘                                             Task 15（1.0.0 正式版建置，不發佈）→ Task 16（使用者明確核准後才發佈）
```

**檔案歸屬規則（rev 2 的核心）：**
- **Z 專屬檔案**：新檔案，以及 A2、B 不會碰的既有檔案。Lane A、B、C 只能改這些檔案（見下方「檔案結構」的 Lane 欄）。
- **共用檔案**：A2 或 B 也會改的檔案，全部只在 Task 12 修改。包括：
  - `build.gradle`（根目錄）、`hymnchtv/build.gradle`、`AndroidManifest.xml`
  - `strings.xml`（三個語系）
  - `MainActivity.java`、`ContentHandler.java`、`MediaConfig.java`、`HymnsApp.java`
  - `layout/media_config.xml`（直、橫兩版）、`changelog_master.xml`

  Task 12 開工前，先把 `feat/new-identity` rebase 到已包含 A2、B 的 `feat/zh-hant` 最新版。
- 任何 lane 發現需要改共用檔案，**停下來回報**，不要自己改。
- **我們不宣稱和 A2、B 完全不重疊**。重疊的部分集中在 Task 12 一次處理，每一步都寫明要對照 A2、B 的現況。

**Lane 開發期間的建置方式：**
- Lane 不能改 `hymnchtv/build.gradle`，但 JVM 單元測試需要真正的 `org.json`（`android.jar` 裡只有空殼）。
- 因此 Task 1 新增一個 Z 專屬的 init script：`tools/z-dev.init.gradle`。它只在建置時加入一個 `testImplementation`。
- Lane 期間所有 Gradle 指令都用 `./gradlew --console=plain -I tools/z-dev.init.gradle ...`（以下簡寫為 `$G`，執行前先設定：`G="./gradlew --console=plain -I tools/z-dev.init.gradle"`）。
- Task 12 把依賴正式寫進 `build.gradle` 後，刪除這個 init script。

**其他規則：**
- **工作目錄**：主 checkout 有其他代理在用，Z 的所有工作都在 `.claude/worktrees/z-*` 裡做。
- **計畫和程式碼對不上就停下來**（行號偏移不算，內容不符才算）。
- **同時跑幾個 Gradle**：每個 daemon 用 4 GB，最多兩個同時跑。
- **簽章金鑰**：
  - 代理不能產生、讀取或詢問正式金鑰。
  - Task 11 和 Task 13 只用一次性的測試金鑰，用完即刪。
- **最後的審查**：Task 14 用 code-reviewer、security-reviewer、`/codex review`，P1 修完才開 PR。

**Goal:**
- applicationId 改為 `com.ziontkec.hymnal`；package 與 `namespace` 維持 `org.cog.hymnchtv`。
- 所有由 applicationId 衍生、或寫死原作者資訊的地方都改對。
- App 內完全沒有說明網址（包括本 repo 的網址），說明改為 App 內文字。
- 主畫面標題改為「詩歌／诗歌／Hymnal」。
- 「檢查更新」改用 GitHub Releases（`hitobias/hymnchtv`）。
  - 下載後驗證四項：`.apk.sha256` 附件的 SHA-256、套件名稱、簽章憑證等於已安裝版本、versionCode 比已安裝版本大。
  - 驗證通過後，用通知讓使用者點選安裝；Android 8.0 以上先導引開啟「安裝不明應用程式」。
- 媒體連結只隨 APK 內附：首次啟動、或內附版本號增加時匯入。不再從網路下載。
- About 頁用原生元件，只顯示：
  - App 名稱、版本、一段簡短說明
  - 由 AboutLibraries 產生的授權清單：包含原專案的 Apache-2.0 聲明、OpenCC 資料說明、A2 的字型項目
- `tools/release.sh`：
  - 先檢查遠端的 tag 與 release 狀態。
  - 用「先建草稿、上傳附件、再公開」的方式發佈，避免留下孤兒 tag。
  - 支援 `--resume`。

**Architecture:**
- 判斷規則寫成 Kotlin 純函式，用 JVM 單元測試，採 TDD：
  - `update/`：`SemVer`、`GitHubReleaseParser`、`ReleaseResponseClassifier`、`UpdateEndpoints`、`ReleaseNotesFormatter`、`Sha256File`、`ApkTrust`、`InstallGate`、`MediaLinksPolicy`
  - `about/`：`AboutLibrariesJson`、`LicenseText`
- Android 邊界（Kotlin）：
  - 更新：`GitHubReleaseClient`（OkHttp）、`SigningCerts`、`ApkVerifier`、`UpdateNotifier`、`UpdateInstallActivity`
  - 其他：`MediaLinksUpdater`、`HelpActivity`、`LicensesActivity`
  - `SigningCerts` 寫 instrumented test；換簽章的情境在 Task 13 手動驗證。
- 既有 Java 類別保留公開 API：`UpdateServiceImpl` 的 `getInstance`、`checkForUpdates`、`isLatestVersion`、`getLatestVersion`、`removeOldDownloads`，以及 `About.hymnUrlAccess`、`About.DEFAULT_CSS`、`About.bodyTextLight/Dark`。
- 驗證完成的 APK 從 DownloadManager 的外部目錄**一邊複製到 app 私有的 `files/updates/`、一邊計算雜湊**，安裝時只用私有目錄的這一份。
  - 這避免了「驗證後被改掉」的問題：API 28 以下，有儲存權限的 App 可以寫入別人的外部 app 目錄。
  - FileProvider 只為 `files/updates/` 新增一個窄範圍的根目錄。
- Debug 專用的更新網址覆寫：只在 `BuildConfig.DEBUG` 時讀取 app 私有目錄的 `files/update_endpoint.properties`（Task 13 用 `adb` 放進去）。不需要修改 Gradle，release build 永遠忽略它。

**Tech Stack:**
- Android：minSdk 24、compileSdk 37、AGP 9.3.3 內建 Kotlin、Gradle 9.7.1；Java 11 與 Kotlin 混用。
- 函式庫：
  - OkHttp 5.5.0（已有）
  - org.json（平台內建；JVM 測試另加 `org.json:json:20250517`）
  - **AboutLibraries Gradle plugin 15.2.0**（`com.mikepenz.aboutlibraries.plugin.android`）
- 測試：JUnit 4.13.2、Truth 1.4.5、AndroidX Test。
- 工具：`gh` 2.101、Python 3。

**AboutLibraries 版本與相容性（2026-10-02 查證）：**
- 最新版是 `15.2.0`（GitHub release，2026-08-28）。README 的版本對照表寫明「v15.2.0：Compose 1.12.x｜AGP 9｜Kotlin 2.4｜Compile 37」。
- Android plugin 的原始碼（`AboutLibrariesPluginAndroidExtension.kt`）只要求 AGP ≥ 8.13、Gradle ≥ 8.8。本專案是 AGP 9.3.3、Gradle 9.7.1，符合。
- 它透過 `AndroidComponentsExtension.onVariants` 把 `aboutlibraries.json` 加為產生的 `res/raw`，所以 App 用 `R.raw.aboutlibraries` 讀取。
- 本專案沒有 Compose，**不加** AboutLibraries 的 UI 或 core 依賴。JSON 由我們自己的 `AboutLibrariesJson`（org.json）解析，以原生 ListView 顯示。
- Plugin marker（`com.mikepenz.aboutlibraries.plugin.android`，15.2.0）在 Gradle Plugin Portal 上的狀態是 200，實作 artifact 是 `com.mikepenz.aboutlibraries.plugin:aboutlibraries-plugin:15.2.0`。
- AGP 9 預設會做最佳化資源縮減，README 建議用 keep 檔保留 `@raw/aboutlibraries`。Task 8 新增這個檔案（B 之後若開啟 R8／shrinkResources，也不會被刪掉）。

**規格來源:** `docs/superpowers/plans/2026-10-02-hymnchtv-modernization-plan.md` 的「子項目 Z」，加上 2026-10-02 的使用者決策與 Codex 審查。

**分支:**
- `feat/new-identity`：Task 0 時從 `feat/zh-hant` 開出，Task 12 時 rebase 到 `feat/zh-hant` 的最新版。
- Lane 分支：`feat/new-identity-lane-a`、`-lane-b`、`-lane-c`。

**Commit 規則:**
- 使用 conventional commits。
- 每個 commit 都用第二個 `-m` 加上 `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`。

---

## 設計決定

| 項目 | 決定 | 理由 |
|---|---|---|
| applicationId | `com.ziontkec.hymnal`；`namespace`、package 不變 | 使用者決策 |
| FileProvider authority | manifest 用 `${applicationId}.files`，程式用 `getPackageName() + ".files"`，自動跟著改。Task 12 的 instrumented test 會驗證 | `AndroidManifest.xml:135`、`FileBackend.java:278-281` |
| FileProvider 路徑 | 新增 `<files-path name="updates" path="updates/" />`，安裝 APK 只透過這個根目錄 | 範圍窄，而且是私有目錄。既有的 `external-path "/"` 等仍被使用中（`ContentHandler:609-610` 分享樂譜、`LogUploadServiceImpl:68` 記錄檔、`MediaConfig:880,891` 匯出、`FileBackend:393`，都在 `Download/hymnal/` 底下），收窄它們不在 Z 的範圍，列為後續項目 |
| 寫死的 package | `glide/AssetFile.java:41` 的 `createPackageContext("org.cog.hymnchtv")` 改用自己的 `getAssets()` | 新 applicationId 下會失敗；若裝了原版 app，還會讀到原版的 assets |
| 以名稱查資源 | `HymnsApp.getFileResId` 先用 `getPackageName()` 查，查不到再用 namespace 查（Task 12） | applicationId 和 namespace 不同時，可能回傳 0 |
| 通知頻道、intent action、deep link | 不改（沒有 package 前綴；action 只用在 explicit intent 或 LocalBroadcast；沒有 deep link） | 已確認 |
| 公開下載目錄 | `Download/hymnchtv` 改名為 `Download/hymnal` | 若同時裝著原版 app，scoped storage 下無法覆寫原版建立的檔案；全新項目，沒有資料要搬 |
| 說明 | **App 內不顯示任何網址**。「線上說明」改成「使用說明」，開啟 App 內的 `HelpActivity`（純文字）。歌詞頁和媒體配置的說明按鈕也開啟它。媒體配置的影片說明按鈕隱藏 | 使用者決策 1 |
| 主畫面標題 | `app_title_main` 改成 `Hymnal`／`诗歌`／`詩歌` | 使用者決策 3；啟動圖示另案處理 |
| About | 原生版面：圖示、App 名稱、版本、一段純文字說明、「開源授權」「使用說明」按鈕，以及原有的歷史、提報錯誤、更新、確定。刪除 `content_about`、`content_help`、`copyright`。不再用 WebView 顯示字串資源裡的 HTML | Codex P1 |
| 授權清單 | AboutLibraries 15.2.0 在建置時產生 `R.raw.aboutlibraries`，`LicensesActivity` 列表顯示，點選後以純文字顯示授權全文 | 使用者決策 2 |
| 自訂授權項目 | `hymnchtv/aboutlibraries-config/libraries/`：<br>• `hymnchtv-original.json`：原專案，Apache-2.0，含「Copyright 2020 Eng Chong Meng」，屬於授權義務<br>• `opencc-data.json`：資料由 OpenCC 產生，App 不含 OpenCC<br>• A2 新增 `hymnalkai-font.json`：OFL-1.1，`uniqueId` 必須是 `org.cog.hymnal:hymnalkai-font` | 這三項會固定排在清單最前面 |
| 授權清單不顯示網址 | 函式庫沒有可點的連結；只有「授權沒有附全文」的項目，才以純文字顯示授權的 URL（法遵所需） | 使用者決策 1 |
| 錯誤回報 | 收件人留空；主旨「詩歌 App 錯誤報告」 | 不寄給原作者，也不放任何信箱 |
| 媒體連結 | 只用 APK 內附的 `assets/url_import.txt`，版本號是 `BundledMediaLinks.VERSION`（Kotlin 常數，初始值 1）。pref `VersionUrlImport` 小於它時，在 `OnlineUpdateService` 的背景執行緒匯入。**移除所有線上下載** | 使用者決策 4 |
| 版本判斷 | tag `vX.Y.Z` 以 SemVer 比較，判斷是否有新版；安裝前另外要求 archive 的 `longVersionCode` 大於已安裝版本 | Codex P1／P2 |
| 更新驗證 | 依序檢查，任何一項不符就拒絕並刪檔：<br>(1) 檔案的 SHA-256 等於 release 中 `hymnal-X.Y.Z.apk.sha256` 附件的值；附件缺少就拒絕；GitHub 的 `digest` 欄位有值時也必須一致<br>(2) 套件名稱等於自己<br>(3) `versionName` 等於 tag<br>(4) `longVersionCode` 大於已安裝版本<br>(5) 簽章憑證的 SHA-256 等於已安裝版本：API 28 以上用 `GET_SIGNING_CERTIFICATES`，接受 v3 金鑰輪替的 lineage；API 28 以下用 `GET_SIGNATURES` | Codex P1。Android 本身也會拒絕簽章不同的更新；我們的檢查是為了給出清楚的錯誤訊息，而不是讓安裝失敗 |
| 讀不到 archive 簽章（任何 API） | **一律拒絕（fail closed）**，結果為 `SIGNER_UNVERIFIABLE`，不會發出「已驗證」通知，並顯示獨立的訊息：「此 Android 版本無法驗證安裝檔的簽章，已停止自動更新」。<br>API 24–27 用 `getPackageArchiveInfo(GET_SIGNATURES)`，並且依已知的 workaround 設定 `applicationInfo.sourceDir/publicSourceDir`。<br>`SigningCertsTest` 在 api24、api26 上**硬性斷言**讀得到簽章，而且和已安裝版本一致；若斷言失敗就停下來回報 | Codex 複審 P1 |
| 下載檔案的位置 | 不讀 DownloadManager 的 `COLUMN_LOCAL_URI`（可能是 `content://`），而是用固定的 `getExternalFilesDir(DIRECTORY_DOWNLOADS)/hymnal-X.Y.Z.apk`，並檢查 canonical path 確實在該目錄內。若 enqueue 前無法刪除舊檔，就中止（避免 DownloadManager 改名成 `-1`） | Codex 複審 P2 |
| 暫存失敗 | `ApkVerifier` 會檢查 `mkdirs()`、刪除舊暫存檔、寫出與大小比對的結果；任何一步失敗都回傳 `STAGING_FAILED`，視為錯誤，絕不當成「已下載」 | Codex 複審 P2 |
| 安裝流程 | 下載完成的 receiver **只在背景執行緒驗證，不啟動任何 Activity**。<br>• 驗證通過：發出通知（`PendingIntent.FLAG_IMMUTABLE`），點選後開啟 `UpdateInstallActivity`<br>• API 26 以上若 `canRequestPackageInstalls()` 為 false，先導到 `ACTION_MANAGE_UNKNOWN_APP_SOURCES`，返回後再檢查<br>• 最後用 `ACTION_VIEW` 加 FileProvider URI 開啟安裝程式<br>• 使用者手動按「更新」、而且已有驗證過的檔案時，直接開啟 `UpdateInstallActivity`（此時 App 在前景，由使用者操作觸發） | Codex P1 |
| 已是最新版 | 只顯示「已是最新」，不再提供「重新下載」（反正會因為 versionCode 不夠大而被拒絕） | 跟隨新的驗證規則 |
| 通知權限 | API 33 以上若沒有 `POST_NOTIFICATIONS`，改用 toast 提示「到『關於』按『更新』即可安裝」；此時已驗證的檔案還在，再按一次「更新」就會直接開啟安裝 | 不卡住流程 |
| GitHub API 限制 | 未登入每小時 60 次；只在啟動 30 秒後、每 24 小時、以及手動按下時查詢。被限流時提示稍後再試 | — |
| 發佈 | 腳本先查遠端的 tag 與 release 狀態：<br>• `gh release create --draft --target <commit>` 建立草稿並上傳附件；草稿不會建立 tag<br>• 確認附件齊全後，`gh release edit --draft=false` 公開，GitHub 在此時才建立 tag<br>• 中途失敗用 `--resume` 接續 | Codex P2：不留孤兒 tag |
| 啟動圖示 | 不改（另案處理） | 使用者決策 3 |

### GitHub Release 慣例

| 項目 | 規則 |
|---|---|
| tag | `vX.Y.Z`，等於 `versionName`，不帶 pre-release 或 build 後綴 |
| versionCode | `X*100000 + Y*1000 + Z*10 + n`（n = 0–9），例如 1.0.0 → `100000`、1.1.0 → `101000` |
| 附件（必須） | `hymnal-X.Y.Z.apk`（release 簽章）、`hymnal-X.Y.Z.apk.sha256`（`shasum -a 256` 的輸出） |
| Release notes | 由 `changelog_master.xml` 中同版號的項目產生，結尾附 SHA-256 |
| 不使用 | prerelease；草稿只在發佈過程中短暫存在 |

---

## 檔案結構

**Z 專屬：新增（Kotlin 純邏輯，有 JVM 測試）**

| 檔案 | Lane |
|---|---|
| `hymnchtv/src/main/java/org/cog/hymnchtv/identity/UpdateSource.kt` | 1 |
| `hymnchtv/src/main/java/org/cog/hymnchtv/update/SemVer.kt` | A |
| `hymnchtv/src/main/java/org/cog/hymnchtv/update/ReleaseInfo.kt` | A |
| `hymnchtv/src/main/java/org/cog/hymnchtv/update/ReleaseConvention.kt` | A |
| `hymnchtv/src/main/java/org/cog/hymnchtv/update/GitHubReleaseParser.kt` | A |
| `hymnchtv/src/main/java/org/cog/hymnchtv/update/UpdateCheckResult.kt` | A |
| `hymnchtv/src/main/java/org/cog/hymnchtv/update/ReleaseResponseClassifier.kt` | A |
| `hymnchtv/src/main/java/org/cog/hymnchtv/update/UpdateEndpoints.kt` | A |
| `hymnchtv/src/main/java/org/cog/hymnchtv/utils/HtmlText.kt` | A |
| `hymnchtv/src/main/java/org/cog/hymnchtv/update/ReleaseNotesFormatter.kt` | A |
| `hymnchtv/src/main/java/org/cog/hymnchtv/update/Sha256File.kt` | A |
| `hymnchtv/src/main/java/org/cog/hymnchtv/update/ApkTrust.kt` | A |
| `hymnchtv/src/main/java/org/cog/hymnchtv/update/InstallGate.kt` | A |
| `hymnchtv/src/main/java/org/cog/hymnchtv/update/MediaLinksPolicy.kt` | A |
| `hymnchtv/src/main/java/org/cog/hymnchtv/about/AboutLibrariesJson.kt` | B |
| `hymnchtv/src/main/java/org/cog/hymnchtv/about/LicenseText.kt` | B |

**Z 專屬：新增（Android 邊界）**

| 檔案 | Lane |
|---|---|
| `hymnchtv/src/main/java/org/cog/hymnchtv/update/UpdateHttp.kt`、`GitHubReleaseClient.kt`、`UpdateEndpointsLoader.kt` | A |
| `hymnchtv/src/main/java/org/cog/hymnchtv/update/SigningCerts.kt`、`ApkVerifier.kt`、`UpdateNotifier.kt`、`UpdateInstallActivity.kt` | A |
| `hymnchtv/src/main/java/org/cog/hymnchtv/update/MediaLinksUpdater.kt` | A |
| `hymnchtv/src/main/res/values{,-zh,-b+zh+Hant}/strings_update.xml` | A |
| `hymnchtv/src/debug/res/xml/network_security_config.xml` | A |
| `hymnchtv/src/main/java/org/cog/hymnchtv/about/HelpActivity.kt`、`LicensesActivity.kt` | B |
| `hymnchtv/src/main/res/layout/help.xml`、`licenses.xml` | B |
| `hymnchtv/src/main/res/values{,-zh,-b+zh+Hant}/strings_about.xml`、`strings_help.xml` | B |
| `hymnchtv/aboutlibraries-config/libraries/hymnchtv-original.json`、`opencc-data.json` | B |
| `hymnchtv/src/main/res/raw/z_keep_aboutlibraries.xml` | B |
| `tools/z-dev.init.gradle`（Task 12 刪除） | 1 |
| `tools/release.sh`、`tools/release_notes.py` | C |

**Z 專屬：修改既有檔案**（A2、B 不碰；Task 12 開工時會再確認一次）

| 檔案 | Lane | 內容 |
|---|---|---|
| `service/androidupdate/UpdateServiceImpl.java` | A | 整檔改寫，公開 API 不變 |
| `service/androidupdate/VersionServiceImpl.java` | A | 改讀 `BuildConfig` |
| `service/androidupdate/OnlineUpdateService.java` | A | 首次匯入媒體連結、通知標題 |
| `res/xml/file_paths.xml` | A | 新增 `files-path updates` |
| `glide/AssetFile.java` | B | 拿掉寫死的 package |
| `persistance/FileBackend.java` | B | `FP_HYMNCHTV` 改 `/hymnal` |
| `About.java`、`res/layout/about.xml` | B | 整檔改寫（原生版面） |
| `README.md` | B | 改寫 |

**測試**

| 檔案 | Lane |
|---|---|
| `src/test/.../identity/UpdateSourceTest.kt` | 1 |
| `src/test/.../update/SemVerTest.kt`、`GitHubReleaseParserTest.kt`、`ReleaseConventionTest.kt`、`ReleaseResponseClassifierTest.kt`、`UpdateEndpointsTest.kt`、`ReleaseNotesFormatterTest.kt`、`Sha256FileTest.kt`、`ApkTrustTest.kt`、`InstallGateTest.kt`、`MediaLinksPolicyTest.kt` | A |
| `src/test/resources/update/release_latest.json` | A |
| `src/androidTest/.../update/SigningCertsTest.kt` | A |
| `src/test/.../about/AboutLibrariesJsonTest.kt`、`LicenseTextTest.kt`、`src/test/resources/about/aboutlibraries_sample.json` | B |
| `src/androidTest/.../AssetAndStorageTest.kt` | B |
| `src/test/.../identity/ApplicationIdentityTest.kt`、`IdentityGuardTest.kt`、`src/androidTest/.../IdentityRuntimeTest.kt` | 12 |

**共用檔案（只在 Task 12 修改）**

| 檔案 | Z 的修改 | 可能撞到的子項目 |
|---|---|---|
| `build.gradle`（根目錄） | 加 `gradlePluginPortal()` 與 AboutLibraries classpath | B（baseline profile plugin）、AGP 版本更新 |
| `hymnchtv/build.gradle` | `apply plugin`、applicationId、`aboutLibraries` 設定、`org.json` 測試依賴、刪除 `updateVersionFile`、測試輸入 | B（R8、profileinstaller） |
| `hymnchtv/src/main/AndroidManifest.xml` | 註冊 3 個 Activity | A2（設定頁 Activity） |
| `values{,-zh,-b+zh+Hant}/strings.xml` | `app_title_main`、`help_online`、`send_logs_*`；刪除 `content_about`、`content_help`、`copyright`、`app_libraries`、`version`、4 個舊的更新字串 | A2（新字串） |
| `MainActivity.java` | 刪掉 `HYMNCHTV_FAQ`；選單「使用說明」開 `HelpActivity` | A2（選單）、B（啟動流程） |
| `ContentHandler.java` | 刪掉 `HYMNCHTV_FAQ_PLAYBACK` 與 `UrlType.onlineHelp`；選單「說明」開 `HelpActivity` | A2（大幅修改） |
| `mediaconfig/MediaConfig.java` | 刪掉說明網址與影片清單；說明按鈕開 `HelpActivity` | B（B-9a 匯入） |
| `HymnsApp.java` | `getFileResId` 的 fallback | B（啟動流程） |
| `layout/media_config.xml`、`layout-land/media_config.xml` | 隱藏 `help_video` | B（不太可能） |
| `res/xml/changelog_master.xml` | 9 處原作者 FAQ 連結改成純文字 | 任何新增 release 項目的人 |
| `res/layout/content_help.xml` | 刪除（未被使用） | — |

**刪除：** `hymnchtv/version.properties`、`hymnchtv/release/version.properties`、`tools/z-dev.init.gradle`、`res/layout/content_help.xml`（都在 Task 12）。

**和 A2、B 的協調事項：**
- **A2（字型）**：新增 `hymnchtv/aboutlibraries-config/libraries/hymnalkai-font.json`：

  ```json
  {
    "uniqueId": "org.cog.hymnal:hymnalkai-font",
    "artifactVersion": "1.0",
    "name": "HymnalKai lyrics font (derived from LXGW WenKai / Klee One)",
    "description": "Subset of LXGW WenKai, renamed as required by the SIL Open Font License 1.1.",
    "developers": [],
    "licenses": ["OFL-1.1"]
  }
  ```

  若要在離線環境建置，A2 另外放 `aboutlibraries-config/licenses/OFL-1.1.json`，格式為 `{"hash":"OFL-1.1","name":"SIL Open Font License 1.1","url":"https://spdx.org/licenses/OFL-1.1.html","content":"<全文>"}`。這一項自動排在授權清單的第三位，不用改程式。
- **B**：
  - `MediaConfig.importUrlRecords(InputStream, boolean)` 必須維持 static 而且同步執行（`MediaLinksUpdater` 在背景執行緒呼叫它）。
  - 不要取消 `MainActivity.java:281` 的註解，首次匯入由 Z 負責。
  - 若 B 開啟 shrinkResources，`res/raw/z_keep_aboutlibraries.xml` 會保住授權清單。

---

### Task 0：準備 worktree、記錄基準

- [ ] **Step 1：建立 Z 的整合 worktree**

  ```bash
  cd /Users/hitobias/orca/hymnchtv
  git fetch origin
  git worktree add .claude/worktrees/z-main -b feat/new-identity feat/zh-hant
  cp local.properties .claude/worktrees/z-main/
  cd .claude/worktrees/z-main && git branch --show-current
  ```

  Expected: 印出 `feat/new-identity`。Task 0、1、12、13、14 都在這個目錄執行。

- [ ] **Step 2：基準 build、測試、lint**

  ```bash
  ./gradlew --console=plain :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug :hymnchtv:lintDebug
  for k in MissingTranslation ExtraTranslation HardcodedText; do
    printf "%s: " $k; grep -c "\[$k\]" hymnchtv/build/reports/lint-results-debug.txt
  done
  ```

  Expected: `BUILD SUCCESSFUL`。記下三個 lint 數字，稱為「Task 0 基準值」。

---

### Task 1：Lane 共用的 Z 專屬基礎

**Files:**
- Create: `tools/z-dev.init.gradle`
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/identity/UpdateSource.kt`
- Create: `hymnchtv/src/test/java/org/cog/hymnchtv/identity/UpdateSourceTest.kt`

**衝突風險：** 無（全部是新檔案，不碰任何共用檔案）。

- [ ] **Step 1：新增 `tools/z-dev.init.gradle`**

  ```groovy
  // Sub-project Z, lanes only (deleted in Task 12): adds the real org.json for JVM unit tests without touching the
  // shared hymnchtv/build.gradle. Usage: ./gradlew -I tools/z-dev.init.gradle :hymnchtv:testDebugUnitTest
  allprojects {
      pluginManager.withPlugin('com.android.application') {
          dependencies {
              testImplementation 'org.json:json:20250517'
          }
      }
  }
  ```

- [ ] **Step 2：寫會失敗的測試** `hymnchtv/src/test/java/org/cog/hymnchtv/identity/UpdateSourceTest.kt`

  ```kotlin
  package org.cog.hymnchtv.identity

  import com.google.common.truth.Truth.assertThat
  import org.json.JSONObject
  import org.junit.Test

  class UpdateSourceTest {
      @Test
      fun pointsAtThisRepositorysReleases() {
          assertThat(UpdateSource.RELEASES_LATEST_API)
              .isEqualTo("https://api.github.com/repos/hitobias/hymnchtv/releases/latest")
          assertThat(UpdateSource.RELEASE_DOWNLOAD_PREFIX)
              .isEqualTo("https://github.com/hitobias/hymnchtv/releases/download/")
      }

      /** Proves tools/z-dev.init.gradle put a real org.json on the JVM test classpath. */
      @Test
      fun realOrgJsonIsAvailableInUnitTests() {
          assertThat(JSONObject("""{"a":1}""").getInt("a")).isEqualTo(1)
      }
  }
  ```

- [ ] **Step 3：執行，確認失敗**

  Run: `G="./gradlew --console=plain -I tools/z-dev.init.gradle"; $G :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.identity.UpdateSourceTest'`
  Expected: 編譯失敗，`Unresolved reference 'UpdateSource'`。

- [ ] **Step 4：新增 `hymnchtv/src/main/java/org/cog/hymnchtv/identity/UpdateSource.kt`**

  ```kotlin
  package org.cog.hymnchtv.identity

  /**
   * Where the in-app updater looks for releases (sub-project Z). Used only for network calls; these URLs are never
   * shown in the UI.
   */
  object UpdateSource {
      const val GITHUB_REPO = "hitobias/hymnchtv"
      const val RELEASES_LATEST_API = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"
      const val RELEASE_DOWNLOAD_PREFIX = "https://github.com/$GITHUB_REPO/releases/download/"
  }
  ```

- [ ] **Step 5：執行，確認通過**

  Run: 同 Step 3。
  Expected: `BUILD SUCCESSFUL`，2 個測試通過。若 `realOrgJsonIsAvailableInUnitTests` 出現 `not mocked`，代表 init script 沒有生效，**停下來回報**。

- [ ] **Step 6：Commit 並建立 lane worktree**

  ```bash
  git add tools/z-dev.init.gradle hymnchtv/src/main/java/org/cog/hymnchtv/identity hymnchtv/src/test/java/org/cog/hymnchtv/identity
  git commit -m "chore: add Z update source constants and lane-only test classpath init script" \
    -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  for l in a b c; do
    git worktree add ../z-lane-$l -b feat/new-identity-lane-$l feat/new-identity
    cp local.properties ../z-lane-$l/
  done
  git worktree list | grep z-
  ```

  Expected: 列出 `z-main`、`z-lane-a`、`z-lane-b`、`z-lane-c`。

---

### Task 2：SemVer（Lane A）

**Files:** Create `update/SemVer.kt`、`src/test/.../update/SemVerTest.kt`。**衝突風險：** 無。

- [ ] **Step 1：寫會失敗的測試** `hymnchtv/src/test/java/org/cog/hymnchtv/update/SemVerTest.kt`

  ```kotlin
  package org.cog.hymnchtv.update

  import com.google.common.truth.Truth.assertThat
  import org.junit.Test
  import java.util.Random

  class SemVerTest {
      private fun v(text: String) = checkNotNull(SemVer.parse(text)) { "unparsable: $text" }

      @Test
      fun parsesPlainAndTaggedVersions() {
          assertThat(SemVer.parse("2.9.2")).isEqualTo(SemVer(2, 9, 2))
          assertThat(SemVer.parse("v2.10.0")).isEqualTo(SemVer(2, 10, 0))
          assertThat(SemVer.parse("V2.10.0")).isEqualTo(SemVer(2, 10, 0))
          assertThat(SemVer.parse(" 2.10.0\n")).isEqualTo(SemVer(2, 10, 0))
      }

      @Test
      fun parsesPreReleaseAndIgnoresBuildMetadata() {
          assertThat(SemVer.parse("2.10.0-beta.1")).isEqualTo(SemVer(2, 10, 0, listOf("beta", "1")))
          assertThat(SemVer.parse("2.10.0+20261020")).isEqualTo(SemVer(2, 10, 0))
          assertThat(SemVer.parse("2.10.0-rc.1+abc")).isEqualTo(SemVer(2, 10, 0, listOf("rc", "1")))
      }

      @Test
      fun rejectsInvalidText() {
          listOf(
              null, "", "  ", "v", "2.10", "2.10.0.1", "02.1.0", "2.x.0", "2.10.0-",
              "2.10.0-beta..1", "99999999999.0.0", "release-2.10.0",
          ).forEach { assertThat(SemVer.parse(it)).isNull() }
      }

      @Test
      fun comparesNumericallyNotLexically() {
          assertThat(v("2.10.0")).isGreaterThan(v("2.9.2"))
          assertThat(v("2.9.10")).isGreaterThan(v("2.9.9"))
          assertThat(v("3.0.0")).isGreaterThan(v("2.99.99"))
          assertThat(v("10.0.0")).isGreaterThan(v("9.9.9"))
      }

      @Test
      fun releaseOutranksItsPreReleases() {
          assertThat(v("2.10.0")).isGreaterThan(v("2.10.0-rc.1"))
          assertThat(v("2.10.0-rc.1")).isGreaterThan(v("2.9.2"))
      }

      @Test
      fun followsSemverSpecPrecedenceExample() {
          val ordered = listOf(
              "1.0.0-alpha", "1.0.0-alpha.1", "1.0.0-alpha.beta", "1.0.0-beta",
              "1.0.0-beta.2", "1.0.0-beta.11", "1.0.0-rc.1", "1.0.0",
          ).map(::v)
          assertThat(ordered.shuffled(Random(7)).sorted()).containsExactlyElementsIn(ordered).inOrder()
          ordered.zipWithNext().forEach { (lower, higher) -> assertThat(lower).isLessThan(higher) }
      }

      @Test
      fun buildMetadataDoesNotAffectEquality() {
          assertThat(v("v2.10.0").compareTo(v("2.10.0+build.5"))).isEqualTo(0)
          assertThat(v("v2.10.0")).isEqualTo(v("2.10.0+build.5"))
      }

      @Test
      fun toStringIsCanonical() {
          assertThat(v("v2.10.0").toString()).isEqualTo("2.10.0")
          assertThat(v("2.10.0-rc.1+x").toString()).isEqualTo("2.10.0-rc.1")
      }
  }
  ```

- [ ] **Step 2：執行，確認失敗**：`$G :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.update.SemVerTest'`。Expected: `Unresolved reference 'SemVer'`。

- [ ] **Step 3：實作** `hymnchtv/src/main/java/org/cog/hymnchtv/update/SemVer.kt`

  ```kotlin
  package org.cog.hymnchtv.update

  /**
   * Semantic version with semver.org 2.0.0 precedence. A leading "v"/"V" is accepted and build metadata
   * ("+...") is ignored, so v2.10.0 == 2.10.0+build.
   */
  data class SemVer(
      val major: Int,
      val minor: Int,
      val patch: Int,
      val preRelease: List<String> = emptyList(),
  ) : Comparable<SemVer> {

      override fun compareTo(other: SemVer): Int {
          compareValues(major, other.major).let { if (it != 0) return it }
          compareValues(minor, other.minor).let { if (it != 0) return it }
          compareValues(patch, other.patch).let { if (it != 0) return it }
          return comparePreRelease(preRelease, other.preRelease)
      }

      override fun toString(): String =
          "$major.$minor.$patch" + if (preRelease.isEmpty()) "" else preRelease.joinToString(".", prefix = "-")

      companion object {
          private val PATTERN = Regex(
              """^[vV]?(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)""" +
                  """(?:-([0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*))?""" +
                  """(?:\+[0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*)?$"""
          )

          @JvmStatic
          fun parse(text: String?): SemVer? {
              val match = PATTERN.matchEntire(text?.trim() ?: return null) ?: return null
              val (major, minor, patch, pre) = match.destructured
              return SemVer(
                  major.toIntOrNull() ?: return null,
                  minor.toIntOrNull() ?: return null,
                  patch.toIntOrNull() ?: return null,
                  if (pre.isEmpty()) emptyList() else pre.split('.'),
              )
          }

          private fun comparePreRelease(a: List<String>, b: List<String>): Int {
              if (a.isEmpty() && b.isEmpty()) return 0
              if (a.isEmpty()) return 1
              if (b.isEmpty()) return -1
              for (i in 0 until minOf(a.size, b.size)) {
                  val c = compareIdentifier(a[i], b[i])
                  if (c != 0) return c
              }
              return compareValues(a.size, b.size)
          }

          private fun compareIdentifier(x: String, y: String): Int {
              val xNumeric = x.all { it in '0'..'9' }
              val yNumeric = y.all { it in '0'..'9' }
              return when {
                  xNumeric && yNumeric -> compareNumeric(x, y)
                  xNumeric -> -1
                  yNumeric -> 1
                  else -> x.compareTo(y)
              }
          }

          /** Compares digit strings of any length without overflow. */
          private fun compareNumeric(x: String, y: String): Int {
              val a = x.trimStart('0').ifEmpty { "0" }
              val b = y.trimStart('0').ifEmpty { "0" }
              return if (a.length != b.length) compareValues(a.length, b.length) else a.compareTo(b)
          }
      }
  }
  ```

- [ ] **Step 4：執行，確認通過**：Expected `BUILD SUCCESSFUL`，8 個測試通過。

- [ ] **Step 5：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/update/SemVer.kt hymnchtv/src/test/java/org/cog/hymnchtv/update/SemVerTest.kt
  git commit -m "feat: add SemVer parser and comparator for release tags" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

### Task 3：GitHub Release JSON 解析（Lane A）

**Files:** Create `update/ReleaseInfo.kt`、`update/ReleaseConvention.kt`、`update/GitHubReleaseParser.kt`、`src/test/resources/update/release_latest.json`、`GitHubReleaseParserTest.kt`、`ReleaseConventionTest.kt`。**衝突風險：** 無。

- [ ] **Step 1：fixture** `hymnchtv/src/test/resources/update/release_latest.json`

  欄位結構取自 2026-10-02 `gh api repos/cmeng-git/hymnchtv/releases/latest` 的真實回應（`uploader` 已縮減），值換成本 repo 的慣例。

  ```json
  {
    "url": "https://api.github.com/repos/hitobias/hymnchtv/releases/250000001",
    "assets_url": "https://api.github.com/repos/hitobias/hymnchtv/releases/250000001/assets",
    "html_url": "https://github.com/hitobias/hymnchtv/releases/tag/v1.1.0",
    "id": 250000001,
    "author": { "login": "hitobias", "type": "User" },
    "node_id": "RE_kwDOFixture001",
    "tag_name": "v1.1.0",
    "target_commitish": "master",
    "name": "詩歌 Hymnal 1.1.0",
    "draft": false,
    "immutable": false,
    "prerelease": false,
    "created_at": "2026-10-20T08:00:00Z",
    "published_at": "2026-10-20T08:05:00Z",
    "assets": [
      {
        "url": "https://api.github.com/repos/hitobias/hymnchtv/releases/assets/600000001",
        "id": 600000001,
        "name": "hymnal-1.1.0.apk",
        "label": null,
        "uploader": { "login": "hitobias", "type": "User" },
        "content_type": "application/vnd.android.package-archive",
        "state": "uploaded",
        "size": 121034567,
        "digest": "sha256:3095e51c68bb82552f8aaf30fa5a77a7fe6786c35952b984181f28d923aceccd",
        "download_count": 0,
        "browser_download_url": "https://github.com/hitobias/hymnchtv/releases/download/v1.1.0/hymnal-1.1.0.apk"
      },
      {
        "url": "https://api.github.com/repos/hitobias/hymnchtv/releases/assets/600000002",
        "id": 600000002,
        "name": "hymnal-1.1.0.apk.sha256",
        "label": null,
        "uploader": { "login": "hitobias", "type": "User" },
        "content_type": "text/plain",
        "state": "uploaded",
        "size": 84,
        "digest": "sha256:0f3c2d1e4b5a69788796a5b4c3d2e1f00f1e2d3c4b5a69788796a5b4c3d2e1f0",
        "download_count": 0,
        "browser_download_url": "https://github.com/hitobias/hymnchtv/releases/download/v1.1.0/hymnal-1.1.0.apk.sha256"
      }
    ],
    "body": "- 介面新增繁體中文\r\n- 歌詞可以設定預設語言\r\n\r\nSHA-256: `3095e51c68bb82552f8aaf30fa5a77a7fe6786c35952b984181f28d923aceccd`"
  }
  ```

- [ ] **Step 2：寫會失敗的測試**

  `hymnchtv/src/test/java/org/cog/hymnchtv/update/GitHubReleaseParserTest.kt`

  ```kotlin
  package org.cog.hymnchtv.update

  import com.google.common.truth.Truth.assertThat
  import org.cog.hymnchtv.identity.UpdateSource
  import org.json.JSONObject
  import org.junit.Test

  class GitHubReleaseParserTest {
      private val fixture = checkNotNull(javaClass.getResource("/update/release_latest.json")).readText()
      private val prefix = UpdateSource.RELEASE_DOWNLOAD_PREFIX
      private val apk = "hymnal-1.1.0.apk"
      private val sha = "hymnal-1.1.0.apk.sha256"

      private fun parseOk(json: String = fixture): ReleaseInfo {
          val result = GitHubReleaseParser.parse(json, prefix)
          assertThat(result).isInstanceOf(ParseResult.Ok::class.java)
          return (result as ParseResult.Ok).release
      }

      private fun reasonOf(json: String?): String {
          val result = GitHubReleaseParser.parse(json, prefix)
          assertThat(result).isInstanceOf(ParseResult.Invalid::class.java)
          return (result as ParseResult.Invalid).reason
      }

      /** The fixture with [block] applied to a fresh copy. */
      private fun mutated(block: JSONObject.() -> Unit): String = JSONObject(fixture).apply(block).toString()

      private fun JSONObject.asset(name: String): JSONObject {
          val assets = getJSONArray("assets")
          for (i in 0 until assets.length()) {
              val asset = assets.getJSONObject(i)
              if (asset.getString("name") == name) return asset
          }
          error("fixture has no asset $name")
      }

      @Test
      fun parsesCapturedFixture() {
          val release = parseOk()
          assertThat(release.tag).isEqualTo("v1.1.0")
          assertThat(release.version).isEqualTo(SemVer(1, 1, 0))
          assertThat(release.versionName).isEqualTo("1.1.0")
          assertThat(release.apkName).isEqualTo(apk)
          assertThat(release.apkUrl).isEqualTo("${prefix}v1.1.0/$apk")
          assertThat(release.apkSize).isEqualTo(121034567L)
          assertThat(release.sha256Url).isEqualTo("${prefix}v1.1.0/$sha")
          assertThat(release.apkDigestSha256).isEqualTo("3095e51c68bb82552f8aaf30fa5a77a7fe6786c35952b984181f28d923aceccd")
          assertThat(release.notes).contains("介面新增繁體中文")
      }

      @Test
      fun uppercaseDigestIsNormalised() {
          val json = mutated { asset(apk).put("digest", "sha256:3095E51C68BB82552F8AAF30FA5A77A7FE6786C35952B984181F28D923ACECCD") }
          assertThat(parseOk(json).apkDigestSha256).isEqualTo("3095e51c68bb82552f8aaf30fa5a77a7fe6786c35952b984181f28d923aceccd")
      }

      @Test
      fun missingOrMalformedDigestGivesNull() {
          assertThat(parseOk(mutated { asset(apk).remove("digest") }).apkDigestSha256).isNull()
          assertThat(parseOk(mutated { asset(apk).put("digest", JSONObject.NULL) }).apkDigestSha256).isNull()
          assertThat(parseOk(mutated { asset(apk).put("digest", "md5:abc") }).apkDigestSha256).isNull()
      }

      @Test
      fun nullBodyGivesEmptyNotes() {
          assertThat(parseOk(mutated { put("body", JSONObject.NULL) }).notes).isEmpty()
          assertThat(parseOk(mutated { remove("body") }).notes).isEmpty()
      }

      @Test
      fun rejectsDraftAndPrerelease() {
          assertThat(reasonOf(mutated { put("draft", true) })).contains("draft")
          assertThat(reasonOf(mutated { put("prerelease", true) })).contains("prerelease")
      }

      @Test
      fun rejectsTagsOutsideConvention() {
          listOf("1.1.0", "V1.1.0", "v1.1", "v1.1.0-rc.1", "v1.1.0+build", "release-1.1.0").forEach { tag ->
              assertThat(reasonOf(mutated { put("tag_name", tag) })).contains("tag")
          }
          assertThat(reasonOf(mutated { remove("tag_name") })).contains("tag_name")
      }

      @Test
      fun rejectsReleaseWithoutMatchingApk() {
          listOf("hymnal-1.0.0.apk", "hymnchtv-release.apk", "hymnal-1.1.0.APK").forEach { name ->
              val json = mutated { asset(apk).put("name", name).put("browser_download_url", "${prefix}v1.1.0/$name") }
              assertThat(reasonOf(json)).contains(apk)
          }
      }

      @Test
      fun ignoresAssetsHostedOutsideTheRepo() {
          listOf(
              "https://evil.example/$apk",
              "https://github.com/someone-else/hymnchtv/releases/download/v1.1.0/$apk",
              "${prefix}v1.1.0/../../../evil/$apk",
          ).forEach { url ->
              assertThat(reasonOf(mutated { asset(apk).put("browser_download_url", url) })).contains(apk)
          }
      }

      @Test
      fun ignoresAssetNotYetUploaded() {
          assertThat(reasonOf(mutated { asset(apk).put("state", "new") })).contains(apk)
      }

      @Test
      fun requiresChecksumAsset() {
          assertThat(reasonOf(mutated { asset(sha).put("name", "notes.txt").put("browser_download_url", "${prefix}v1.1.0/notes.txt") }))
              .contains(sha)
          assertThat(reasonOf(mutated { asset(sha).put("browser_download_url", "https://evil.example/$sha") })).contains(sha)
      }

      @Test
      fun rejectsEmptyAndMalformedJson() {
          assertThat(reasonOf(null)).contains("empty")
          assertThat(reasonOf("   ")).contains("empty")
          assertThat(reasonOf("{")).contains("malformed")
          assertThat(reasonOf("[]")).contains("malformed")
      }
  }
  ```

  `hymnchtv/src/test/java/org/cog/hymnchtv/update/ReleaseConventionTest.kt`

  ```kotlin
  package org.cog.hymnchtv.update

  import com.google.common.truth.Truth.assertThat
  import org.junit.Test

  class ReleaseConventionTest {
      @Test
      fun namesFollowTheConvention() {
          val v = SemVer(1, 1, 0)
          assertThat(ReleaseConvention.tagFor(v)).isEqualTo("v1.1.0")
          assertThat(ReleaseConvention.apkName(v)).isEqualTo("hymnal-1.1.0.apk")
          assertThat(ReleaseConvention.sha256Name(v)).isEqualTo("hymnal-1.1.0.apk.sha256")
      }

      @Test
      fun recognisesOnlyPlainApkNames() {
          assertThat(ReleaseConvention.isApkName("hymnal-1.1.0.apk")).isTrue()
          listOf(null, "", "../hymnal-1.1.0.apk", "hymnal-1.1.0.apk.sha256", "x/hymnal-1.1.0.apk", "hymnal-1.1.apk")
              .forEach { assertThat(ReleaseConvention.isApkName(it)).isFalse() }
      }
  }
  ```

- [ ] **Step 3：執行，確認失敗**：`$G :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.update.GitHubReleaseParserTest' --tests 'org.cog.hymnchtv.update.ReleaseConventionTest'`。Expected: `Unresolved reference`。

- [ ] **Step 4：實作**

  `hymnchtv/src/main/java/org/cog/hymnchtv/update/ReleaseInfo.kt`

  ```kotlin
  package org.cog.hymnchtv.update

  /** A published GitHub release that follows [ReleaseConvention]. */
  data class ReleaseInfo(
      val tag: String,
      val version: SemVer,
      val apkName: String,
      val apkUrl: String,
      val apkSize: Long,
      /** URL of the required hymnal-X.Y.Z.apk.sha256 asset. */
      val sha256Url: String,
      /** GitHub's own asset digest (lower-case hex) when provided; cross-checked against the .sha256 file. */
      val apkDigestSha256: String?,
      val notes: String,
      val htmlUrl: String,
  ) {
      val versionName: String get() = version.toString()
  }
  ```

  `hymnchtv/src/main/java/org/cog/hymnchtv/update/ReleaseConvention.kt`

  ```kotlin
  package org.cog.hymnchtv.update

  /** Release naming rules shared by the app updater and tools/release.sh. Keep both in sync. */
  object ReleaseConvention {
      private val APK_NAME = Regex("""^hymnal-(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)\.apk$""")

      @JvmStatic
      fun tagFor(version: SemVer): String = "v$version"

      @JvmStatic
      fun apkName(version: SemVer): String = "hymnal-$version.apk"

      @JvmStatic
      fun sha256Name(version: SemVer): String = apkName(version) + ".sha256"

      /** True only for a bare file name like hymnal-1.1.0.apk (no path, no suffix). */
      @JvmStatic
      fun isApkName(name: String?): Boolean = name != null && APK_NAME.matches(name)
  }
  ```

  `hymnchtv/src/main/java/org/cog/hymnchtv/update/GitHubReleaseParser.kt`

  ```kotlin
  package org.cog.hymnchtv.update

  import org.json.JSONArray
  import org.json.JSONException
  import org.json.JSONObject

  sealed class ParseResult {
      data class Ok(val release: ReleaseInfo) : ParseResult()
      data class Invalid(val reason: String) : ParseResult()
  }

  /**
   * Parses a GitHub `releases/latest` response. Only assets whose browser_download_url lives under
   * [downloadPrefix] (this repository's release downloads) are considered; both the APK and its .sha256 are required.
   */
  object GitHubReleaseParser {
      private val SHA256_DIGEST = Regex("^sha256:([0-9a-fA-F]{64})$")

      private data class Asset(val name: String, val url: String, val size: Long, val digest: String?)

      @JvmStatic
      fun parse(json: String?, downloadPrefix: String): ParseResult {
          if (json.isNullOrBlank()) return ParseResult.Invalid("empty body")
          val root = try {
              JSONObject(json)
          } catch (e: JSONException) {
              return ParseResult.Invalid("malformed json: ${e.message}")
          }
          return parseRelease(root, downloadPrefix)
      }

      private fun parseRelease(root: JSONObject, downloadPrefix: String): ParseResult {
          if (root.optBoolean("draft", false)) return ParseResult.Invalid("draft release")
          if (root.optBoolean("prerelease", false)) return ParseResult.Invalid("prerelease")
          val tag = root.stringOrNull("tag_name") ?: return ParseResult.Invalid("missing tag_name")
          val version = SemVer.parse(tag)
          if (version == null || tag != ReleaseConvention.tagFor(version)) {
              return ParseResult.Invalid("tag outside convention vX.Y.Z: $tag")
          }
          val assets = readAssets(root.optJSONArray("assets") ?: JSONArray(), downloadPrefix)
          val apkName = ReleaseConvention.apkName(version)
          val shaName = ReleaseConvention.sha256Name(version)
          val apk = assets.firstOrNull { it.name == apkName }
              ?: return ParseResult.Invalid("no $apkName asset under $downloadPrefix")
          val sha = assets.firstOrNull { it.name == shaName }
              ?: return ParseResult.Invalid("no $shaName asset under $downloadPrefix")
          return ParseResult.Ok(
              ReleaseInfo(
                  tag = tag,
                  version = version,
                  apkName = apk.name,
                  apkUrl = apk.url,
                  apkSize = apk.size,
                  sha256Url = sha.url,
                  apkDigestSha256 = apk.digest?.let { SHA256_DIGEST.matchEntire(it)?.groupValues?.get(1)?.lowercase() },
                  notes = root.stringOrNull("body").orEmpty(),
                  htmlUrl = root.stringOrNull("html_url").orEmpty(),
              )
          )
      }

      private fun readAssets(array: JSONArray, downloadPrefix: String): List<Asset> =
          (0 until array.length()).mapNotNull { index -> array.optJSONObject(index)?.let { toAsset(it, downloadPrefix) } }

      private fun toAsset(obj: JSONObject, downloadPrefix: String): Asset? {
          val name = obj.stringOrNull("name") ?: return null
          val url = obj.stringOrNull("browser_download_url") ?: return null
          val state = obj.stringOrNull("state")
          val uploaded = state == null || state == "uploaded"
          val ownedByRepo = url.startsWith(downloadPrefix) && url.endsWith("/$name") && ".." !in url
          return if (uploaded && ownedByRepo) Asset(name, url, obj.optLong("size", 0L), obj.stringOrNull("digest")) else null
      }

      private fun JSONObject.stringOrNull(key: String): String? =
          if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }
  }
  ```

- [ ] **Step 5：執行，確認通過**：Expected `BUILD SUCCESSFUL`；`GitHubReleaseParserTest` 11 個、`ReleaseConventionTest` 2 個通過。

- [ ] **Step 6：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/update hymnchtv/src/test/resources/update hymnchtv/src/test/java/org/cog/hymnchtv/update
  git commit -m "feat: parse GitHub releases/latest; require APK and .sha256 assets from this repo" \
    -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

### Task 4：回應分類與 debug 端點覆寫（Lane A）

**Files:** Create `update/UpdateCheckResult.kt`、`update/ReleaseResponseClassifier.kt`、`update/UpdateEndpoints.kt`、`ReleaseResponseClassifierTest.kt`、`UpdateEndpointsTest.kt`。**衝突風險：** 無。

- [ ] **Step 1：寫會失敗的測試**

  `hymnchtv/src/test/java/org/cog/hymnchtv/update/ReleaseResponseClassifierTest.kt`

  ```kotlin
  package org.cog.hymnchtv.update

  import com.google.common.truth.Truth.assertThat
  import org.cog.hymnchtv.identity.UpdateSource
  import org.junit.Test

  class ReleaseResponseClassifierTest {
      private val fixture = checkNotNull(javaClass.getResource("/update/release_latest.json")).readText()

      private fun headers(vararg pairs: Pair<String, String>): (String) -> String? {
          val map = pairs.associate { it.first.lowercase() to it.second }
          return { name -> map[name.lowercase()] }
      }

      private fun classify(
          code: Int,
          body: String? = fixture,
          current: String? = "1.0.0",
          header: (String) -> String? = headers(),
      ) = ReleaseResponseClassifier.classify(code, header, body, current, UpdateSource.RELEASE_DOWNLOAD_PREFIX)

      @Test
      fun newerReleaseIsAvailable() {
          val result = classify(200)
          assertThat(result).isInstanceOf(UpdateCheckResult.Available::class.java)
          assertThat(result.release?.versionName).isEqualTo("1.1.0")
      }

      @Test
      fun sameOrNewerInstalledVersionIsUpToDate() {
          listOf("1.1.0", "1.1.1", "2.0.0").forEach {
              assertThat(classify(200, current = it)).isInstanceOf(UpdateCheckResult.UpToDate::class.java)
          }
      }

      @Test
      fun comparesVersionsNumerically() {
          assertThat(classify(200, current = "1.0.10")).isInstanceOf(UpdateCheckResult.Available::class.java)
      }

      @Test
      fun notFoundMeansNoRelease() {
          assertThat(classify(404, body = """{"message":"Not Found"}""")).isEqualTo(UpdateCheckResult.NoRelease)
      }

      @Test
      fun primaryRateLimitCarriesResetTime() {
          val result = classify(403, body = "{}", header = headers("X-RateLimit-Remaining" to "0", "X-RateLimit-Reset" to "1893456000"))
          assertThat(result).isEqualTo(UpdateCheckResult.RateLimited(1893456000L))
      }

      @Test
      fun status429IsRateLimitedEvenWithoutHeaders() {
          assertThat(classify(429, body = "")).isEqualTo(UpdateCheckResult.RateLimited(null))
      }

      @Test
      fun secondaryRateLimitUsesRetryAfter() {
          assertThat(classify(403, body = "", header = headers("Retry-After" to "60"))).isEqualTo(UpdateCheckResult.RateLimited(null))
      }

      @Test
      fun otherForbiddenIsFailure() {
          assertThat(classify(403, body = "")).isEqualTo(UpdateCheckResult.Failed("HTTP 403"))
      }

      @Test
      fun serverErrorIsFailure() {
          assertThat(classify(502, body = "bad gateway")).isEqualTo(UpdateCheckResult.Failed("HTTP 502"))
      }

      @Test
      fun malformedBodyIsFailure() {
          val result = classify(200, body = "{")
          assertThat((result as UpdateCheckResult.Failed).reason).contains("malformed")
      }

      @Test
      fun unparsableInstalledVersionIsFailure() {
          assertThat(classify(200, current = "abc")).isInstanceOf(UpdateCheckResult.Failed::class.java)
          assertThat(classify(200, current = null)).isInstanceOf(UpdateCheckResult.Failed::class.java)
      }
  }
  ```

  `hymnchtv/src/test/java/org/cog/hymnchtv/update/UpdateEndpointsTest.kt`

  ```kotlin
  package org.cog.hymnchtv.update

  import com.google.common.truth.Truth.assertThat
  import org.cog.hymnchtv.identity.UpdateSource
  import org.junit.Test

  class UpdateEndpointsTest {
      private val override = "api=http://10.0.2.2:8000/latest.json\nprefix=http://10.0.2.2:8000/\n"

      @Test
      fun releaseBuildsIgnoreOverrideFile() {
          assertThat(UpdateEndpoints.resolve(false, override)).isEqualTo(UpdateEndpoints.PRODUCTION)
      }

      @Test
      fun debugWithoutUsableOverrideUsesProduction() {
          listOf(null, "", "# comment only", "prefix=http://10.0.2.2:8000/", "api=").forEach {
              assertThat(UpdateEndpoints.resolve(true, it)).isEqualTo(UpdateEndpoints.PRODUCTION)
          }
      }

      @Test
      fun debugOverrideReplacesBothEndpoints() {
          assertThat(UpdateEndpoints.resolve(true, " api = http://10.0.2.2:8000/latest.json \r\nprefix=http://10.0.2.2:8000/"))
              .isEqualTo(UpdateEndpoints("http://10.0.2.2:8000/latest.json", "http://10.0.2.2:8000/"))
      }

      @Test
      fun debugOverrideWithoutPrefixKeepsProductionPrefix() {
          assertThat(UpdateEndpoints.resolve(true, "api=http://10.0.2.2:8000/latest.json"))
              .isEqualTo(UpdateEndpoints("http://10.0.2.2:8000/latest.json", UpdateSource.RELEASE_DOWNLOAD_PREFIX))
      }
  }
  ```

- [ ] **Step 2：執行，確認失敗**（`Unresolved reference`）。

- [ ] **Step 3：實作**

  `hymnchtv/src/main/java/org/cog/hymnchtv/update/UpdateCheckResult.kt`

  ```kotlin
  package org.cog.hymnchtv.update

  /** Outcome of one update check against GitHub Releases. */
  sealed class UpdateCheckResult {
      /** The parsed release, when the check got one. */
      open val release: ReleaseInfo? get() = null

      data class Available(override val release: ReleaseInfo) : UpdateCheckResult()
      data class UpToDate(override val release: ReleaseInfo) : UpdateCheckResult()
      data object NoRelease : UpdateCheckResult()
      data class RateLimited(val resetEpochSeconds: Long?) : UpdateCheckResult()
      data class NetworkError(val message: String) : UpdateCheckResult()
      data class Failed(val reason: String) : UpdateCheckResult()
  }
  ```

  `hymnchtv/src/main/java/org/cog/hymnchtv/update/ReleaseResponseClassifier.kt`

  ```kotlin
  package org.cog.hymnchtv.update

  /** Turns an HTTP response from `releases/latest` into an [UpdateCheckResult]. Pure; no I/O. */
  object ReleaseResponseClassifier {
      @JvmStatic
      fun classify(
          code: Int,
          header: (String) -> String?,
          body: String?,
          currentVersionName: String?,
          downloadPrefix: String,
      ): UpdateCheckResult {
          when {
              code == 200 -> Unit
              code == 404 -> return UpdateCheckResult.NoRelease
              isRateLimited(code, header) ->
                  return UpdateCheckResult.RateLimited(header("x-ratelimit-reset")?.trim()?.toLongOrNull())
              else -> return UpdateCheckResult.Failed("HTTP $code")
          }
          val current = SemVer.parse(currentVersionName)
              ?: return UpdateCheckResult.Failed("unparsable installed version: $currentVersionName")
          return when (val parsed = GitHubReleaseParser.parse(body, downloadPrefix)) {
              is ParseResult.Invalid -> UpdateCheckResult.Failed(parsed.reason)
              is ParseResult.Ok ->
                  if (parsed.release.version > current) UpdateCheckResult.Available(parsed.release)
                  else UpdateCheckResult.UpToDate(parsed.release)
          }
      }

      /** GitHub signals primary limits with remaining=0 and secondary limits with Retry-After (403 or 429). */
      private fun isRateLimited(code: Int, header: (String) -> String?): Boolean =
          code == 429 || (code == 403 && (header("x-ratelimit-remaining")?.trim() == "0" || header("retry-after") != null))
  }
  ```

  `hymnchtv/src/main/java/org/cog/hymnchtv/update/UpdateEndpoints.kt`

  ```kotlin
  package org.cog.hymnchtv.update

  import org.cog.hymnchtv.identity.UpdateSource

  /** Where the updater looks for releases and which download URLs it trusts. */
  data class UpdateEndpoints(val apiUrl: String, val downloadPrefix: String) {
      companion object {
          @JvmField
          val PRODUCTION = UpdateEndpoints(UpdateSource.RELEASES_LATEST_API, UpdateSource.RELEASE_DOWNLOAD_PREFIX)

          /** Debug-only override file in the app's private files dir (pushed with adb for Task 13). */
          const val OVERRIDE_FILE = "update_endpoint.properties"

          /** Release builds always use [PRODUCTION]; debug builds honour `api=` / optional `prefix=` lines. */
          @JvmStatic
          fun resolve(isDebug: Boolean, overrideText: String?): UpdateEndpoints {
              if (!isDebug || overrideText == null) return PRODUCTION
              val values = overrideText.lineSequence()
                  .map { it.trim() }
                  .filter { it.isNotEmpty() && !it.startsWith("#") }
                  .mapNotNull { line -> line.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0].trim() to it[1].trim() } }
                  .toMap()
              val api = values["api"].orEmpty()
              if (api.isEmpty()) return PRODUCTION
              return UpdateEndpoints(api, values["prefix"].orEmpty().ifEmpty { UpdateSource.RELEASE_DOWNLOAD_PREFIX })
          }
      }
  }
  ```

- [ ] **Step 4：執行，確認通過**：`ReleaseResponseClassifierTest` 11 個、`UpdateEndpointsTest` 4 個通過。

- [ ] **Step 5：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/update hymnchtv/src/test/java/org/cog/hymnchtv/update
  git commit -m "feat: classify GitHub release responses incl. rate limits; debug-only endpoint override" \
    -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

### Task 5：更新信任規則與其他純函式（Lane A）

**Files:**
- Create `utils/HtmlText.kt`、`update/ReleaseNotesFormatter.kt`、`update/Sha256File.kt`、`update/ApkTrust.kt`、`update/InstallGate.kt`、`update/MediaLinksPolicy.kt`
- 以及對應的測試：`HtmlTextTest`（放 `src/test/.../utils/`）、`ReleaseNotesFormatterTest`、`Sha256FileTest`、`ApkTrustTest`、`InstallGateTest`、`MediaLinksPolicyTest`

**衝突風險：** 無。

- [ ] **Step 1：寫會失敗的測試**

  `hymnchtv/src/test/java/org/cog/hymnchtv/utils/HtmlTextTest.kt`

  ```kotlin
  package org.cog.hymnchtv.utils

  import com.google.common.truth.Truth.assertThat
  import org.junit.Test

  class HtmlTextTest {
      @Test
      fun escapesMarkupCharacters() {
          assertThat(HtmlText.escape("""<a href="x">Tom & Jerry's</a>"""))
              .isEqualTo("&lt;a href=&quot;x&quot;&gt;Tom &amp; Jerry&#39;s&lt;/a&gt;")
      }

      @Test
      fun leavesChineseAndPlainTextUntouched() {
          assertThat(HtmlText.escape("詩歌 Hymnal 1.0.0")).isEqualTo("詩歌 Hymnal 1.0.0")
      }

      @Test
      fun emptyStaysEmpty() {
          assertThat(HtmlText.escape("")).isEmpty()
      }
  }
  ```

  `hymnchtv/src/test/java/org/cog/hymnchtv/update/ReleaseNotesFormatterTest.kt`

  ```kotlin
  package org.cog.hymnchtv.update

  import com.google.common.truth.Truth.assertThat
  import org.junit.Test

  class ReleaseNotesFormatterTest {
      @Test
      fun escapesMarkupSoNotesCannotInjectHtml() {
          assertThat(ReleaseNotesFormatter.toHtml("""<script>alert('x')</script> & "q"""", "無更新"))
              .isEqualTo("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt; &amp; &quot;q&quot;")
      }

      @Test
      fun convertsAllLineEndingsToBreaks() {
          assertThat(ReleaseNotesFormatter.toHtml("- 一\r\n- 二\n- 三", "無更新")).isEqualTo("- 一<br/>- 二<br/>- 三")
      }

      @Test
      fun blankNotesFallBackToEscapedPlaceholder() {
          assertThat(ReleaseNotesFormatter.toHtml(null, "無更新")).isEqualTo("無更新")
          assertThat(ReleaseNotesFormatter.toHtml("  \r\n ", "<無>")).isEqualTo("&lt;無&gt;")
      }

      @Test
      fun clipsVeryLongNotes() {
          val html = ReleaseNotesFormatter.toHtml("x".repeat(ReleaseNotesFormatter.MAX_CHARS + 1000), "無更新")
          assertThat(html).hasLength(ReleaseNotesFormatter.MAX_CHARS + 1)
          assertThat(html).endsWith("…")
      }
  }
  ```

  `hymnchtv/src/test/java/org/cog/hymnchtv/update/Sha256FileTest.kt`

  ```kotlin
  package org.cog.hymnchtv.update

  import com.google.common.truth.Truth.assertThat
  import org.junit.Test

  class Sha256FileTest {
      private val hex = "3095e51c68bb82552f8aaf30fa5a77a7fe6786c35952b984181f28d923aceccd"
      private val apk = "hymnal-1.1.0.apk"

      @Test
      fun acceptsExactShasumLine() {
          assertThat(Sha256File.parse("$hex  $apk\n", apk)).isEqualTo(hex)
          assertThat(Sha256File.parse("$hex  $apk", apk)).isEqualTo(hex)
          assertThat(Sha256File.parse("${hex.uppercase()}  $apk\n", apk)).isEqualTo(hex)
      }

      @Test
      fun rejectsOtherFileNames() {
          listOf("hymnal-1.0.0.apk", "hymnal-1.1.0.apk.sha256", "./$apk", "dist/$apk").forEach {
              assertThat(Sha256File.parse("$hex  $it\n", apk)).isNull()
          }
      }

      @Test
      fun rejectsWrongWhitespaceAndMarkers() {
          listOf("$hex $apk", "$hex   $apk", " $hex  $apk", "$hex  $apk ", "$hex *$apk", "$hex\t$apk", "$hex  $apk\r\n", hex)
              .forEach { assertThat(Sha256File.parse(it, apk)).isNull() }
      }

      @Test
      fun rejectsMultiLineAndTruncatedContent() {
          listOf(
              "$hex  $apk\n$hex  $apk\n", "\n$hex  $apk\n", "$hex  $apk\n\n",
              "${hex.dropLast(1)}  $apk", "$hex${"0"}  $apk", "${hex.take(32)}", null, "",
          ).forEach { assertThat(Sha256File.parse(it, apk)).isNull() }
      }

      @Test
      fun reconcileRequiresFileHashAndMatchingDigest() {
          assertThat(Sha256File.reconcile(hex, null)).isEqualTo(hex)
          assertThat(Sha256File.reconcile(hex, hex)).isEqualTo(hex)
          assertThat(Sha256File.reconcile(hex, "0".repeat(64))).isNull()
          assertThat(Sha256File.reconcile(null, hex)).isNull()
      }
  }
  ```

  `hymnchtv/src/test/java/org/cog/hymnchtv/update/ApkTrustTest.kt`

  ```kotlin
  package org.cog.hymnchtv.update

  import com.google.common.truth.Truth.assertThat
  import org.junit.Test

  class ApkTrustTest {
      private val version = SemVer(1, 1, 0)
      private val sha = "3095e51c68bb82552f8aaf30fa5a77a7fe6786c35952b984181f28d923aceccd"
      private val certA = "a".repeat(64)
      private val certB = "b".repeat(64)
      private val installed = InstalledApp("com.ziontkec.hymnal", 100000L, setOf(certA))
      private val good = ArchiveApk("com.ziontkec.hymnal", "1.1.0", 101000L, setOf(certA), listOf(certA))

      private fun check(
          archive: ArchiveApk = good,
          actualSha: String? = sha,
      ) = ApkTrust.check(version, sha, actualSha, installed, archive)

      @Test
      fun acceptsTrustedUpdate() {
          assertThat(check()).isEqualTo(ApkCheck.OK)
          assertThat(check(actualSha = sha.uppercase())).isEqualTo(ApkCheck.OK)
      }

      @Test
      fun checksumIsCheckedFirst() {
          assertThat(check(actualSha = "0".repeat(64))).isEqualTo(ApkCheck.CHECKSUM_MISMATCH)
          assertThat(check(archive = good.copy(packageName = "evil"), actualSha = null)).isEqualTo(ApkCheck.CHECKSUM_MISMATCH)
      }

      @Test
      fun unreadableArchiveIsRefused() {
          assertThat(check(archive = ArchiveApk(null, null, null, null))).isEqualTo(ApkCheck.UNREADABLE)
          assertThat(check(archive = good.copy(versionCode = null))).isEqualTo(ApkCheck.UNREADABLE)
      }

      @Test
      fun rejectsOtherPackages() {
          assertThat(check(archive = good.copy(packageName = "org.cog.hymnchtv"))).isEqualTo(ApkCheck.WRONG_PACKAGE)
      }

      @Test
      fun rejectsVersionNameOtherThanTheTag() {
          listOf("1.0.0", "1.1.0-debug", "1.1.1", null).forEach {
              assertThat(check(archive = good.copy(versionName = it))).isEqualTo(ApkCheck.WRONG_VERSION)
          }
      }

      @Test
      fun requiresHigherVersionCodeThanInstalled() {
          assertThat(check(archive = good.copy(versionCode = 100000L))).isEqualTo(ApkCheck.NOT_NEWER)
          assertThat(check(archive = good.copy(versionCode = 1L))).isEqualTo(ApkCheck.NOT_NEWER)
      }

      @Test
      fun rejectsDifferentSigner() {
          assertThat(check(archive = good.copy(signers = setOf(certB), signerHistory = listOf(certB)))).isEqualTo(ApkCheck.WRONG_SIGNER)
          assertThat(check(archive = good.copy(signers = setOf(certA, certB)))).isEqualTo(ApkCheck.WRONG_SIGNER)
      }

      @Test
      fun acceptsKeyRotationLineageContainingInstalledSigner() {
          assertThat(check(archive = good.copy(signers = setOf(certB), signerHistory = listOf(certA, certB)))).isEqualTo(ApkCheck.OK)
      }

      /** Fail closed on every API level: an unverifiable signer is never reported as verified. */
      @Test
      fun unreadableSignersAreNeverTrusted() {
          assertThat(check(archive = good.copy(signers = null, signerHistory = emptyList()))).isEqualTo(ApkCheck.SIGNER_UNVERIFIABLE)
          assertThat(check(archive = good.copy(signers = emptySet(), signerHistory = emptyList()))).isEqualTo(ApkCheck.SIGNER_UNVERIFIABLE)
      }

      @Test
      fun installedSignerUnknownIsNeverTrusted() {
          val result = ApkTrust.check(version, sha, sha, installed.copy(signers = emptySet()), good)
          assertThat(result).isEqualTo(ApkCheck.SIGNER_UNVERIFIABLE)
      }
  }
  ```

  `hymnchtv/src/test/java/org/cog/hymnchtv/update/InstallGateTest.kt`

  ```kotlin
  package org.cog.hymnchtv.update

  import com.google.common.truth.Truth.assertThat
  import org.junit.Test

  class InstallGateTest {
      @Test
      fun asksForUnknownSourcesOnlyOnOreoAndLaterWhenNotAllowed() {
          assertThat(InstallGate.nextStep(26, false)).isEqualTo(InstallStep.ALLOW_UNKNOWN_SOURCES)
          assertThat(InstallGate.nextStep(35, false)).isEqualTo(InstallStep.ALLOW_UNKNOWN_SOURCES)
          assertThat(InstallGate.nextStep(26, true)).isEqualTo(InstallStep.LAUNCH_INSTALLER)
          assertThat(InstallGate.nextStep(25, false)).isEqualTo(InstallStep.LAUNCH_INSTALLER)
      }
  }
  ```

  `hymnchtv/src/test/java/org/cog/hymnchtv/update/MediaLinksPolicyTest.kt`

  ```kotlin
  package org.cog.hymnchtv.update

  import com.google.common.truth.Truth.assertThat
  import org.junit.Test

  class MediaLinksPolicyTest {
      @Test
      fun importsBundledListOnlyWhenItsVersionIsNewer() {
          assertThat(MediaLinksPolicy.shouldImport(-1, 1)).isTrue()
          assertThat(MediaLinksPolicy.shouldImport(1, 2)).isTrue()
          assertThat(MediaLinksPolicy.shouldImport(1, 1)).isFalse()
          assertThat(MediaLinksPolicy.shouldImport(2, 1)).isFalse()
          assertThat(MediaLinksPolicy.BUNDLED_VERSION).isAtLeast(1)
      }
  }
  ```

- [ ] **Step 2：執行，確認失敗**（`Unresolved reference`）。

- [ ] **Step 3：實作**

  `hymnchtv/src/main/java/org/cog/hymnchtv/utils/HtmlText.kt`

  ```kotlin
  package org.cog.hymnchtv.utils

  /** Minimal HTML escaping for text inserted into WebView content. */
  object HtmlText {
      @JvmStatic
      fun escape(text: String): String = buildString(text.length) {
          for (c in text) {
              when (c) {
                  '&' -> append("&amp;")
                  '<' -> append("&lt;")
                  '>' -> append("&gt;")
                  '"' -> append("&quot;")
                  '\'' -> append("&#39;")
                  else -> append(c)
              }
          }
      }
  }
  ```

  `hymnchtv/src/main/java/org/cog/hymnchtv/update/ReleaseNotesFormatter.kt`

  ```kotlin
  package org.cog.hymnchtv.update

  import org.cog.hymnchtv.utils.HtmlText

  /** Release notes are untrusted text: escape them and keep line breaks for the WebView dialog. */
  object ReleaseNotesFormatter {
      const val MAX_CHARS = 4000

      @JvmStatic
      fun toHtml(notes: String?, emptyText: String): String {
          val text = notes?.replace("\r\n", "\n")?.trim().orEmpty()
          if (text.isEmpty()) return HtmlText.escape(emptyText)
          val clipped = if (text.length > MAX_CHARS) text.take(MAX_CHARS) + "…" else text
          return clipped.split('\n').joinToString("<br/>") { HtmlText.escape(it) }
      }
  }
  ```

  `hymnchtv/src/main/java/org/cog/hymnchtv/update/Sha256File.kt`

  ```kotlin
  package org.cog.hymnchtv.update

  /**
   * Reads the hymnal-X.Y.Z.apk.sha256 release asset. Strict `shasum -a 256` text-mode format: exactly one line
   * `<64 hex>` + two spaces + the exact apk name, optionally followed by a single "\n". Anything else is rejected.
   */
  object Sha256File {
      private val HEX = Regex("^[0-9a-fA-F]{64}$")

      /** @return lower-case hex for [apkName], or null when the text is not exactly that checksum line. */
      @JvmStatic
      fun parse(text: String?, apkName: String): String? {
          val line = text?.removeSuffix("\n") ?: return null
          if ('\n' in line || '\r' in line) return null
          val expectedSuffix = "  $apkName"
          if (!line.endsWith(expectedSuffix)) return null
          val hash = line.removeSuffix(expectedSuffix)
          return if (HEX.matches(hash)) hash.lowercase() else null
      }

      /** The published checksum, required; GitHub's asset digest, when present, must agree with it. */
      @JvmStatic
      fun reconcile(fileHash: String?, githubDigest: String?): String? = when {
          fileHash == null -> null
          githubDigest != null && !githubDigest.equals(fileHash, ignoreCase = true) -> null
          else -> fileHash
      }
  }
  ```

  `hymnchtv/src/main/java/org/cog/hymnchtv/update/ApkTrust.kt`

  ```kotlin
  package org.cog.hymnchtv.update

  /** STAGING_FAILED is produced by ApkVerifier (I/O), never by [ApkTrust]. */
  enum class ApkCheck {
      OK, CHECKSUM_MISMATCH, UNREADABLE, WRONG_PACKAGE, WRONG_VERSION, NOT_NEWER, WRONG_SIGNER, SIGNER_UNVERIFIABLE, STAGING_FAILED,
  }

  /** The running app: package, version code and SHA-256 of its signing certificate(s). */
  data class InstalledApp(val packageName: String, val versionCode: Long, val signers: Set<String>)

  /** Facts read from a downloaded APK; nulls mean the platform could not read them. */
  data class ArchiveApk(
      val packageName: String?,
      val versionName: String?,
      val versionCode: Long?,
      val signers: Set<String>?,
      /** Signing lineage, original first (APK Signature Scheme v3 key rotation); empty when unknown. */
      val signerHistory: List<String> = emptyList(),
  )

  /**
   * Decides whether a downloaded APK may be offered for installation. Android also refuses updates signed by a
   * different key; this check exists so the user gets a clear message instead of a failed install.
   */
  object ApkTrust {
      @JvmStatic
      fun check(
          expectedVersion: SemVer,
          expectedSha256: String,
          actualSha256: String?,
          installed: InstalledApp,
          archive: ArchiveApk,
      ): ApkCheck {
          if (!expectedSha256.equals(actualSha256, ignoreCase = true)) return ApkCheck.CHECKSUM_MISMATCH
          val packageName = archive.packageName ?: return ApkCheck.UNREADABLE
          if (packageName != installed.packageName) return ApkCheck.WRONG_PACKAGE
          if (SemVer.parse(archive.versionName) != expectedVersion) return ApkCheck.WRONG_VERSION
          val versionCode = archive.versionCode ?: return ApkCheck.UNREADABLE
          if (versionCode <= installed.versionCode) return ApkCheck.NOT_NEWER
          // Fail closed: without both certificate sets the update is not verified, on any API level.
          val signers = archive.signers
          if (signers.isNullOrEmpty() || installed.signers.isEmpty()) return ApkCheck.SIGNER_UNVERIFIABLE
          return if (isSameSigner(installed.signers, signers, archive.signerHistory)) ApkCheck.OK else ApkCheck.WRONG_SIGNER
      }

      @JvmStatic
      fun isSameSigner(installed: Set<String>, archive: Set<String>, archiveHistory: List<String>): Boolean {
          if (installed.isEmpty() || archive.isEmpty()) return false
          if (installed == archive) return true
          // Key rotation: a single-signer update whose lineage contains the installed signer.
          return installed.size == 1 && archive.size == 1 && installed.single() in archiveHistory
      }
  }
  ```

  `hymnchtv/src/main/java/org/cog/hymnchtv/update/InstallGate.kt`

  ```kotlin
  package org.cog.hymnchtv.update

  enum class InstallStep { ALLOW_UNKNOWN_SOURCES, LAUNCH_INSTALLER }

  /** Android 8.0+ needs the per-app "install unknown apps" permission before the installer can be used. */
  object InstallGate {
      @JvmStatic
      fun nextStep(sdkInt: Int, canRequestPackageInstalls: Boolean): InstallStep =
          if (sdkInt >= 26 && !canRequestPackageInstalls) InstallStep.ALLOW_UNKNOWN_SOURCES else InstallStep.LAUNCH_INSTALLER
  }
  ```

  `hymnchtv/src/main/java/org/cog/hymnchtv/update/MediaLinksPolicy.kt`

  ```kotlin
  package org.cog.hymnchtv.update

  /** Media links ship only inside the APK (assets/url_import.txt); a new list means a new app release. */
  object MediaLinksPolicy {
      /** Version of the bundled url_import.txt. Increase it whenever that file changes. */
      const val BUNDLED_VERSION = 1

      @JvmStatic
      fun shouldImport(installedVersion: Int, bundledVersion: Int): Boolean = bundledVersion > installedVersion
  }
  ```

- [ ] **Step 4：執行，確認通過**：`HtmlTextTest` 3、`ReleaseNotesFormatterTest` 4、`Sha256FileTest` 5、`ApkTrustTest` 10、`InstallGateTest` 1、`MediaLinksPolicyTest` 1，全部通過。

- [ ] **Step 5：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/update hymnchtv/src/main/java/org/cog/hymnchtv/utils/HtmlText.kt \
    hymnchtv/src/test/java/org/cog/hymnchtv/update hymnchtv/src/test/java/org/cog/hymnchtv/utils/HtmlTextTest.kt
  git commit -m "feat: add update trust rules (checksum, package, version code, signer) and install gate" \
    -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

### Task 6：Android 整合：更新服務、驗證、安裝流程（Lane A）

**Files:**
- Create `update/UpdateHttp.kt`、`update/UpdateEndpointsLoader.kt`、`update/GitHubReleaseClient.kt`、`update/SigningCerts.kt`、`update/ApkVerifier.kt`、`update/UpdateNotifier.kt`、`update/UpdateInstallActivity.kt`、`update/MediaLinksUpdater.kt`
- Rewrite `UpdateServiceImpl.java`、`VersionServiceImpl.java`；Modify `OnlineUpdateService.java`、`res/xml/file_paths.xml`
- Create `strings_update.xml` ×3、`src/debug/res/xml/network_security_config.xml`、`src/androidTest/.../update/SigningCertsTest.kt`

**衝突風險：** 無，這些都是 Z 專屬檔案。`UpdateInstallActivity` 要到 Task 12 才註冊到 manifest，所以本 task 只驗證到能編譯，以及 `SigningCertsTest`。

- [ ] **Step 1：`update/UpdateHttp.kt`**

  ```kotlin
  package org.cog.hymnchtv.update

  import okhttp3.OkHttpClient
  import java.util.concurrent.TimeUnit

  /** Shared OkHttp client for update traffic; built lazily so app start-up never pays for it. */
  object UpdateHttp {
      private val CLIENT: OkHttpClient by lazy {
          OkHttpClient.Builder()
              .connectTimeout(15, TimeUnit.SECONDS)
              .readTimeout(30, TimeUnit.SECONDS)
              .callTimeout(60, TimeUnit.SECONDS)
              .build()
      }

      @JvmStatic
      fun client(): OkHttpClient = CLIENT
  }
  ```

- [ ] **Step 2：`update/UpdateEndpointsLoader.kt`**

  ```kotlin
  package org.cog.hymnchtv.update

  import android.content.Context
  import org.cog.hymnchtv.BuildConfig
  import java.io.File
  import java.io.IOException

  /** Reads the debug-only endpoint override from the app's private files dir on every check. */
  object UpdateEndpointsLoader {
      @JvmStatic
      fun current(context: Context): UpdateEndpoints {
          if (!BuildConfig.DEBUG) return UpdateEndpoints.PRODUCTION
          val file = File(context.filesDir, UpdateEndpoints.OVERRIDE_FILE)
          val text = try {
              if (file.isFile) file.readText() else null
          } catch (e: IOException) {
              null
          }
          return UpdateEndpoints.resolve(true, text)
      }
  }
  ```

- [ ] **Step 3：`update/GitHubReleaseClient.kt`**

  ```kotlin
  package org.cog.hymnchtv.update

  import android.os.Looper
  import androidx.annotation.WorkerThread
  import okhttp3.OkHttpClient
  import okhttp3.Request
  import timber.log.Timber
  import java.io.IOException

  /** GitHub Releases access. Network I/O: never call on the main thread. */
  class GitHubReleaseClient(private val client: OkHttpClient) {

      @WorkerThread
      fun fetchLatest(endpoints: UpdateEndpoints, currentVersionName: String?): UpdateCheckResult {
          requireWorkerThread()
          val request = try {
              Request.Builder()
                  .url(endpoints.apiUrl)
                  .header("Accept", "application/vnd.github+json")
                  .header("X-GitHub-Api-Version", "2022-11-28")
                  .header("User-Agent", "Hymnal-Android/${currentVersionName ?: "unknown"}")
                  .build()
          } catch (e: IllegalArgumentException) {
              return UpdateCheckResult.Failed("bad update url")
          }
          return try {
              client.newCall(request).execute().use { response ->
                  ReleaseResponseClassifier.classify(
                      response.code,
                      { name -> response.header(name) },
                      response.peekBody(MAX_JSON_BYTES).string(),
                      currentVersionName,
                      endpoints.downloadPrefix,
                  )
              }
          } catch (e: IOException) {
              UpdateCheckResult.NetworkError("${e.javaClass.simpleName}: ${e.message}")
          }
      }

      /** Downloads the release's .sha256 asset; null when missing, unreadable or inconsistent with GitHub's digest. */
      @WorkerThread
      fun fetchExpectedSha256(release: ReleaseInfo): String? {
          requireWorkerThread()
          return try {
              client.newCall(Request.Builder().url(release.sha256Url).build()).execute().use { response ->
                  if (!response.isSuccessful) {
                      Timber.w("Checksum download failed: HTTP %s", response.code)
                      return null
                  }
                  val fileHash = Sha256File.parse(response.peekBody(MAX_SHA_BYTES).string(), release.apkName)
                  Sha256File.reconcile(fileHash, release.apkDigestSha256)
              }
          } catch (e: IOException) {
              Timber.w(e, "Checksum download failed")
              null
          } catch (e: IllegalArgumentException) {
              null
          }
      }

      private fun requireWorkerThread() {
          check(Looper.myLooper() != Looper.getMainLooper()) { "Update network I/O must run off the main thread" }
      }

      private companion object {
          const val MAX_JSON_BYTES = 1_000_000L
          const val MAX_SHA_BYTES = 4_096L
      }
  }
  ```

- [ ] **Step 4：`update/SigningCerts.kt`**

  ```kotlin
  package org.cog.hymnchtv.update

  import android.content.Context
  import android.content.pm.PackageInfo
  import android.content.pm.PackageManager
  import android.content.pm.Signature
  import android.os.Build
  import androidx.core.content.pm.PackageInfoCompat
  import org.cog.hymnchtv.BuildConfig
  import java.io.File
  import java.security.MessageDigest

  /**
   * Signing certificates as SHA-256 hex. API 28+: GET_SIGNING_CERTIFICATES (SigningInfo; history is original first,
   * current last, per the platform docs). Below 28: GET_SIGNATURES.
   */
  object SigningCerts {

      @JvmStatic
      fun installedApp(context: Context): InstalledApp {
          val info = try {
              context.packageManager.getPackageInfo(context.packageName, flags())
          } catch (e: PackageManager.NameNotFoundException) {
              null
          }
          return InstalledApp(context.packageName, BuildConfig.VERSION_CODE.toLong(), info?.let { signers(it).first }.orEmpty())
      }

      @JvmStatic
      fun archive(context: Context, apk: File): ArchiveApk {
          val info = context.packageManager.getPackageArchiveInfo(apk.absolutePath, flags())
              ?: return ArchiveApk(null, null, null, null)
          // Known workaround for archive PackageInfo: point applicationInfo at the file before reading from it.
          info.applicationInfo?.let {
              it.sourceDir = apk.absolutePath
              it.publicSourceDir = apk.absolutePath
          }
          val (current, history) = signers(info)
          return ArchiveApk(
              info.packageName,
              info.versionName,
              PackageInfoCompat.getLongVersionCode(info),
              current.takeIf { it.isNotEmpty() },
              history,
          )
      }

      @Suppress("DEPRECATION")
      private fun flags(): Int =
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES
          else PackageManager.GET_SIGNATURES

      @Suppress("DEPRECATION")
      private fun signers(info: PackageInfo): Pair<Set<String>, List<String>> {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
              val signing = info.signingInfo ?: return emptySet<String>() to emptyList()
              val current = signing.apkContentsSigners.orEmpty().map(::sha256).toSet()
              val history = if (signing.hasMultipleSigners()) emptyList()
              else signing.signingCertificateHistory.orEmpty().map(::sha256)
              return current to history
          }
          return info.signatures.orEmpty().map(::sha256).toSet() to emptyList()
      }

      private fun sha256(signature: Signature): String =
          MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString("") { "%02x".format(it) }
  }
  ```

- [ ] **Step 5：`update/ApkVerifier.kt`**

  ```kotlin
  package org.cog.hymnchtv.update

  import android.content.Context
  import androidx.annotation.WorkerThread
  import timber.log.Timber
  import java.io.File
  import java.io.FileInputStream
  import java.io.FileOutputStream
  import java.io.IOException
  import java.security.MessageDigest

  /**
   * Copies a finished download into private storage (files/updates/) while hashing it, then checks the private copy.
   * Only the private copy is ever installed, so nothing can swap the file between verification and installation.
   */
  object ApkVerifier {
      const val UPDATES_DIR = "updates"

      data class Result(val check: ApkCheck, val stagedApk: File?)

      @JvmStatic
      fun updatesDir(context: Context): File = File(context.filesDir, UPDATES_DIR)

      @JvmStatic
      @WorkerThread
      fun verifyAndStage(context: Context, downloaded: File, release: ReleaseInfo, expectedSha256: String): Result {
          val staged = stage(context, downloaded, release)
          downloaded.delete()
          if (staged == null) {
              Timber.w("Update %s verification: %s", release.tag, ApkCheck.STAGING_FAILED)
              return Result(ApkCheck.STAGING_FAILED, null)
          }
          val check = ApkTrust.check(
              release.version, expectedSha256, staged.second,
              SigningCerts.installedApp(context), SigningCerts.archive(context, staged.first),
          )
          if (check != ApkCheck.OK && !staged.first.delete()) Timber.w("Cannot delete rejected %s", staged.first)
          Timber.i("Update %s verification: %s", release.tag, check)
          return Result(check, staged.first.takeIf { check == ApkCheck.OK })
      }

      /**
       * Copies [downloaded] into files/updates/ while hashing it. Every I/O step is checked.
       * @return the staged file and its SHA-256, or null when any step failed (STAGING_FAILED).
       */
      private fun stage(context: Context, downloaded: File, release: ReleaseInfo): Pair<File, String>? {
          val dir = updatesDir(context)
          if (!dir.isDirectory && !dir.mkdirs()) return null
          val old = dir.listFiles() ?: return null
          if (old.any { !it.delete() }) return null // keep at most one staged update
          val staged = File(dir, release.apkName)
          return try {
              val sha = copyHashing(downloaded, staged)
              if (staged.length() != downloaded.length() || staged.length() == 0L) {
                  staged.delete()
                  null
              } else {
                  staged to sha
              }
          } catch (e: IOException) {
              Timber.w(e, "Cannot stage %s", downloaded)
              staged.delete()
              null
          }
      }

      /** A previously verified apk for [release], if it is still staged. */
      @JvmStatic
      fun staged(context: Context, release: ReleaseInfo): File? =
          File(updatesDir(context), release.apkName).takeIf { it.isFile }

      private fun copyHashing(from: File, to: File): String {
          val digest = MessageDigest.getInstance("SHA-256")
          FileInputStream(from).use { input ->
              FileOutputStream(to).use { output ->
                  val buffer = ByteArray(64 * 1024)
                  while (true) {
                      val read = input.read(buffer)
                      if (read < 0) break
                      digest.update(buffer, 0, read)
                      output.write(buffer, 0, read)
                  }
                  output.fd.sync() // write errors surface here as IOException, not later
              }
          }
          return digest.digest().joinToString("") { "%02x".format(it) }
      }
  }
  ```

- [ ] **Step 6：`update/UpdateNotifier.kt`**

  ```kotlin
  package org.cog.hymnchtv.update

  import android.Manifest
  import android.annotation.SuppressLint
  import android.app.PendingIntent
  import android.content.Context
  import android.content.pm.PackageManager
  import android.os.Build
  import androidx.core.app.NotificationCompat
  import androidx.core.app.NotificationManagerCompat
  import androidx.core.content.ContextCompat
  import org.cog.hymnchtv.R
  import org.cog.hymnchtv.service.androidnotification.NotificationHelper

  /** "Update ready" notification; tapping it opens [UpdateInstallActivity] (never started from a receiver). */
  object UpdateNotifier {
      private const val TAG = "hymnal_update_ready"
      private const val ID = 2

      /** @return false when notifications are not permitted, so the caller can fall back to a toast. */
      @JvmStatic
      @SuppressLint("MissingPermission")
      fun showReady(context: Context, apkName: String, versionName: String): Boolean {
          val permitted = Build.VERSION.SDK_INT < 33 ||
              ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
          val manager = NotificationManagerCompat.from(context)
          if (!permitted || !manager.areNotificationsEnabled()) return false
          val pending = PendingIntent.getActivity(
              context, 0, UpdateInstallActivity.intent(context, apkName),
              PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
          )
          val notification = NotificationCompat.Builder(context, NotificationHelper.DEFAULT_GROUP)
              .setSmallIcon(R.drawable.hymnchtv)
              .setContentTitle(context.getString(R.string.app_name))
              .setContentText(context.getString(R.string.update_ready_to_install, versionName))
              .setAutoCancel(true)
              .setContentIntent(pending)
              .build()
          manager.notify(TAG, ID, notification)
          return true
      }

      @JvmStatic
      fun cancel(context: Context) {
          NotificationManagerCompat.from(context).cancel(TAG, ID)
      }
  }
  ```

- [ ] **Step 7：`update/UpdateInstallActivity.kt`**

  ```kotlin
  package org.cog.hymnchtv.update

  import android.app.Activity
  import android.content.ActivityNotFoundException
  import android.content.Context
  import android.content.Intent
  import android.net.Uri
  import android.os.Build
  import android.os.Bundle
  import android.provider.Settings
  import android.widget.Toast
  import org.cog.hymnchtv.R
  import org.cog.hymnchtv.persistance.FileBackend
  import timber.log.Timber
  import java.io.File

  /**
   * Invisible trampoline opened by the user (notification tap or the About "Update" button): asks for the
   * "install unknown apps" permission on Android 8.0+ when needed, then hands the verified private copy to the
   * system installer through this app's FileProvider.
   */
  class UpdateInstallActivity : Activity() {
      private var askedForUnknownSources = false

      override fun onCreate(savedInstanceState: Bundle?) {
          super.onCreate(savedInstanceState)
          askedForUnknownSources = savedInstanceState?.getBoolean(STATE_ASKED, false) ?: false
      }

      override fun onSaveInstanceState(outState: Bundle) {
          super.onSaveInstanceState(outState)
          outState.putBoolean(STATE_ASKED, askedForUnknownSources)
      }

      override fun onResume() {
          super.onResume()
          val apk = stagedApk()
          if (apk == null) {
              toast(R.string.update_install_missing)
              finish()
              return
          }
          val canRequest = Build.VERSION.SDK_INT < Build.VERSION_CODES.O || packageManager.canRequestPackageInstalls()
          when (InstallGate.nextStep(Build.VERSION.SDK_INT, canRequest)) {
              InstallStep.ALLOW_UNKNOWN_SOURCES -> {
                  if (askedForUnknownSources) {
                      toast(R.string.update_install_needs_permission)
                      finish()
                      return
                  }
                  askedForUnknownSources = true
                  startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
              }
              InstallStep.LAUNCH_INSTALLER -> {
                  launchInstaller(apk)
                  finish()
              }
          }
      }

      private fun stagedApk(): File? {
          val name = intent.getStringExtra(EXTRA_APK_NAME)
          if (!ReleaseConvention.isApkName(name)) return null
          return File(ApkVerifier.updatesDir(this), checkNotNull(name)).takeIf { it.isFile }
      }

      private fun launchInstaller(apk: File) {
          try {
              val uri = FileBackend.getUriForFile(this, apk)
              startActivity(
                  Intent(Intent.ACTION_VIEW).setDataAndType(uri, APK_MIME_TYPE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
              )
              UpdateNotifier.cancel(this)
          } catch (e: ActivityNotFoundException) {
              Timber.w(e, "No package installer")
              toast(R.string.update_install_missing)
          } catch (e: SecurityException) {
              Timber.e(e, "No FileProvider uri for %s", apk)
              toast(R.string.update_install_missing)
          }
      }

      private fun toast(resId: Int) = Toast.makeText(this, resId, Toast.LENGTH_LONG).show()

      companion object {
          const val EXTRA_APK_NAME = "apk_name"
          private const val STATE_ASKED = "asked_unknown_sources"
          private const val APK_MIME_TYPE = "application/vnd.android.package-archive"

          @JvmStatic
          fun intent(context: Context, apkName: String): Intent =
              Intent(context, UpdateInstallActivity::class.java)
                  .putExtra(EXTRA_APK_NAME, apkName)
                  .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
  }
  ```

- [ ] **Step 8：`update/MediaLinksUpdater.kt`**

  ```kotlin
  package org.cog.hymnchtv.update

  import android.content.Context
  import android.content.SharedPreferences
  import androidx.annotation.WorkerThread
  import org.cog.hymnchtv.MainActivity
  import org.cog.hymnchtv.mediaconfig.MediaConfig
  import timber.log.Timber
  import java.io.IOException

  /**
   * Imports the media links bundled in the APK (assets/url_import.txt) on first run and whenever
   * [MediaLinksPolicy.BUNDLED_VERSION] increases. No network. Relies on the static, synchronous
   * MediaConfig.importUrlRecords(InputStream, boolean) (contract with sub-project B).
   */
  object MediaLinksUpdater {
      @JvmStatic
      @WorkerThread
      @Synchronized
      fun importBundledIfNeeded(context: Context) {
          val installed = installedVersion(context)
          if (!MediaLinksPolicy.shouldImport(installed, MediaLinksPolicy.BUNDLED_VERSION)) return
          try {
              context.assets.open(MediaConfig.ASSET_URL_IMPORT_FILE).use { MediaConfig.importUrlRecords(it, false) }
              prefs(context).edit().putInt(MediaConfig.PREF_VERSION_URL, MediaLinksPolicy.BUNDLED_VERSION).apply()
              Timber.i("Imported bundled media links v%s (was %s)", MediaLinksPolicy.BUNDLED_VERSION, installed)
          } catch (e: IOException) {
              Timber.w(e, "Bundled media links import failed")
          }
      }

      private fun prefs(context: Context): SharedPreferences =
          context.getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE)

      /** An illegal stored value counts as "never imported" instead of crashing. */
      private fun installedVersion(context: Context): Int = try {
          prefs(context).getInt(MediaConfig.PREF_VERSION_URL, MediaConfig.URL_IMPORT_VERSION)
      } catch (e: ClassCastException) {
          MediaConfig.URL_IMPORT_VERSION
      }
  }
  ```

- [ ] **Step 9：整檔改寫 `VersionServiceImpl.java`**：保留原檔第 1–16 行的授權標頭，第 17 行之後換成：

  ```java
  package org.cog.hymnchtv.service.androidupdate;

  import org.cog.hymnchtv.BuildConfig;

  /**
   * Version of the running app, taken from BuildConfig (no PackageManager lookup, cannot fail).
   *
   * @author Eng Chong Meng
   */
  public class VersionServiceImpl {
      private static final VersionServiceImpl INSTANCE = new VersionServiceImpl();

      public static VersionServiceImpl getInstance() {
          return INSTANCE;
      }

      private VersionServiceImpl() {
      }

      public long getCurrentVersionCode() {
          return BuildConfig.VERSION_CODE;
      }

      public String getCurrentVersionName() {
          return BuildConfig.VERSION_NAME;
      }
  }
  ```

- [ ] **Step 10：整檔改寫 `UpdateServiceImpl.java`**：保留原檔第 1–16 行的授權標頭，第 17 行之後換成：

  ```java
  package org.cog.hymnchtv.service.androidupdate;

  import android.annotation.SuppressLint;
  import android.app.DownloadManager;
  import android.content.BroadcastReceiver;
  import android.content.Context;
  import android.content.Intent;
  import android.content.IntentFilter;
  import android.content.SharedPreferences;
  import android.database.Cursor;
  import android.net.Uri;
  import android.os.Bundle;
  import android.os.Environment;

  import androidx.annotation.Nullable;
  import androidx.core.content.ContextCompat;

  import java.io.File;
  import java.io.IOException;
  import java.util.ArrayList;
  import java.util.List;

  import org.cog.hymnchtv.HymnsApp;
  import org.cog.hymnchtv.MainActivity;
  import org.cog.hymnchtv.R;
  import org.cog.hymnchtv.update.ApkCheck;
  import org.cog.hymnchtv.update.ApkVerifier;
  import org.cog.hymnchtv.update.GitHubReleaseClient;
  import org.cog.hymnchtv.update.ReleaseInfo;
  import org.cog.hymnchtv.update.ReleaseConvention;
  import org.cog.hymnchtv.update.ReleaseNotesFormatter;
  import org.cog.hymnchtv.update.SemVer;
  import org.cog.hymnchtv.update.UpdateCheckResult;
  import org.cog.hymnchtv.update.UpdateEndpointsLoader;
  import org.cog.hymnchtv.update.UpdateHttp;
  import org.cog.hymnchtv.update.UpdateInstallActivity;
  import org.cog.hymnchtv.update.UpdateNotifier;
  import org.cog.hymnchtv.utils.CustomDialogWv;
  import org.cog.hymnchtv.utils.DialogActivity;
  import org.jetbrains.annotations.NotNull;

  import timber.log.Timber;

  /**
   * App update via GitHub Releases (sub-project Z). Checks releases/latest, downloads hymnal-X.Y.Z.apk with
   * DownloadManager, verifies it in the background (published SHA-256, package, version code, signing certificate)
   * and posts a notification; the user's tap opens UpdateInstallActivity. The download receiver never starts
   * activities. Every public method except {@link #removeOldDownloads()} does network or file I/O: call off the main thread.
   *
   * @author Eng Chong Meng
   */
  public class UpdateServiceImpl {
      private static final String APK_MIME_TYPE = "application/vnd.android.package-archive";

      /** SharedPreferences entry holding enqueued download ids, separated by ",". */
      private static final String ENTRY_NAME = "apk_ids";

      private static UpdateServiceImpl mInstance = null;

      private GitHubReleaseClient releaseClient = null;
      private volatile ReleaseInfo latestRelease = null;
      private volatile String expectedSha256 = null;
      private volatile String currentVersion = null;
      private DownloadReceiver downloadReceiver = null;
      private SharedPreferences store;

      public static synchronized UpdateServiceImpl getInstance() {
          if (mInstance == null) {
              mInstance = new UpdateServiceImpl();
          }
          return mInstance;
      }

      /**
       * User-initiated check (About "Update", main screen update button). Call off the main thread.
       */
      public void checkForUpdates() {
          UpdateCheckResult result = fetchLatest();
          ReleaseInfo release = result.getRelease();
          if (result instanceof UpdateCheckResult.Available && release != null) {
              offerUpdate(release);
          }
          else if (result instanceof UpdateCheckResult.UpToDate && release != null) {
              DialogActivity.showDialog(HymnsApp.getGlobalContext(), R.string.app_update_none,
                      R.string.update_up_to_date, currentVersion, release.getVersionName());
          }
          else if (result instanceof UpdateCheckResult.RateLimited) {
              HymnsApp.showToastMessage(R.string.update_check_rate_limited);
          }
          else if (result instanceof UpdateCheckResult.NetworkError) {
              HymnsApp.showToastMessage(R.string.update_check_network_error);
          }
          else if (result instanceof UpdateCheckResult.NoRelease) {
              HymnsApp.showToastMessage(R.string.update_check_no_release);
          }
          else {
              HymnsApp.showToastMessage(R.string.update_check_failed);
          }
      }

      /**
       * @return false only when a newer, checksum-published release is confirmed; failures count as "latest".
       */
      public boolean isLatestVersion() {
          return !(fetchLatest() instanceof UpdateCheckResult.Available);
      }

      /**
       * @return notification text for the latest release; valid after {@link #isLatestVersion()} returned false.
       */
      public String getLatestVersion() {
          ReleaseInfo release = latestRelease;
          return HymnsApp.getResString(R.string.update_notification_text,
                  (release == null) ? "" : release.getVersionName());
      }

      private synchronized UpdateCheckResult fetchLatest() {
          Context context = HymnsApp.getGlobalContext();
          currentVersion = VersionServiceImpl.getInstance().getCurrentVersionName();
          if (releaseClient == null) {
              releaseClient = new GitHubReleaseClient(UpdateHttp.client());
          }
          UpdateCheckResult result = releaseClient.fetchLatest(UpdateEndpointsLoader.current(context), currentVersion);
          ReleaseInfo release = result.getRelease();
          String sha = null;
          if (result instanceof UpdateCheckResult.Available && release != null) {
              sha = releaseClient.fetchExpectedSha256(release);
              if (sha == null) {
                  result = new UpdateCheckResult.Failed("missing or inconsistent " + release.getApkName() + ".sha256");
              }
          }
          latestRelease = release;
          expectedSha256 = sha;
          MainActivity.mHasUpdate = result instanceof UpdateCheckResult.Available;
          Timber.i("Update check: installed %s -> %s %s", currentVersion, result.getClass().getSimpleName(),
                  (release == null) ? "" : release.getTag());
          return result;
      }

      private void offerUpdate(ReleaseInfo release) {
          Context context = HymnsApp.getGlobalContext();
          if (ApkVerifier.staged(context, release) != null) {
              // Verified earlier and still waiting: the user asked for it, the app is in the foreground.
              context.startActivity(UpdateInstallActivity.intent(context, release.getApkName()));
              return;
          }
          if (isDownloadRunning()) {
              DialogActivity.showDialog(context, R.string.in_progress, R.string.download_in_progress);
              return;
          }
          Bundle args = new Bundle();
          args.putString(CustomDialogWv.ARG_MESSAGE,
                  context.getString(R.string.update_new_available, release.getVersionName(), currentVersion));
          args.putString(CustomDialogWv.ARG_HISTORY,
                  ReleaseNotesFormatter.toHtml(release.getNotes(), context.getString(R.string.update_none)));

          DialogActivity.showCustomDialog(context, context.getString(R.string.app_update_install),
                  CustomDialogWv.class.getName(), args, context.getString(R.string.download),
                  new DialogActivity.DialogListener() {
                      @Override
                      public boolean onConfirmClicked(DialogActivity dialog) {
                          downloadApk(release);
                          return true;
                      }

                      @Override
                      public void onDialogCancelled(@NotNull DialogActivity dialog) {
                      }
                  }, null);
      }

      private boolean isDownloadRunning() {
          List<Long> ids = getOldDownloads();
          if (ids.isEmpty()) {
              return false;
          }
          int status = checkDownloadStatus(ids.get(ids.size() - 1));
          return status == DownloadManager.STATUS_PENDING || status == DownloadManager.STATUS_RUNNING
                  || status == DownloadManager.STATUS_PAUSED;
      }

      /**
       * Schedules the apk download into the app-specific Download directory (no storage permission needed).
       */
      private void downloadApk(ReleaseInfo release) {
          Context context = HymnsApp.getGlobalContext();
          File target = expectedDownloadFile(release);
          if (target == null) {
              HymnsApp.showToastMessage(R.string.download_failed);
              return;
          }
          removeOldDownloads();
          if (target.exists() && !target.delete()) {
              // DownloadManager would rename the new file (…-1.apk) and we would verify the stale one: abort.
              Timber.w("Cannot delete stale %s", target);
              HymnsApp.showToastMessage(R.string.download_failed);
              return;
          }
          registerDownloadReceiver(release, expectedSha256);

          DownloadManager.Request request = new DownloadManager.Request(Uri.parse(release.getApkUrl()));
          request.setTitle(release.getApkName());
          request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
          request.setMimeType(APK_MIME_TYPE);
          request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, release.getApkName());
          rememberDownloadId(HymnsApp.getDownloadManager().enqueue(request));
      }

      private synchronized void registerDownloadReceiver(ReleaseInfo release, String sha256) {
          unregisterDownloadReceiver();
          downloadReceiver = new DownloadReceiver(release, sha256);
          // DownloadManager broadcasts from another process, so the receiver must be exported. A forged broadcast
          // only triggers verification of our own download, which fails closed.
          ContextCompat.registerReceiver(HymnsApp.getGlobalContext(), downloadReceiver,
                  new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), ContextCompat.RECEIVER_EXPORTED);
      }

      private synchronized void unregisterDownloadReceiver() {
          if (downloadReceiver != null) {
              HymnsApp.getGlobalContext().unregisterReceiver(downloadReceiver);
              downloadReceiver = null;
          }
      }

      /**
       * Runs on a worker thread after the download finished. Never starts an activity.
       */
      private void verifyFinishedDownload(long id, ReleaseInfo release, String sha256) {
          Context context = HymnsApp.getGlobalContext();
          try {
              if (checkDownloadStatus(id) != DownloadManager.STATUS_SUCCESSFUL || sha256 == null) {
                  HymnsApp.showToastMessage(R.string.download_failed);
                  return;
              }
              File downloaded = expectedDownloadFile(release);
              if (downloaded == null || !downloaded.isFile()) {
                  HymnsApp.showToastMessage(R.string.download_failed);
                  return;
              }
              ApkVerifier.Result result = ApkVerifier.verifyAndStage(context, downloaded, release, sha256);
              if (result.getCheck() == ApkCheck.SIGNER_UNVERIFIABLE) {
                  HymnsApp.showToastMessage(R.string.update_signer_unverifiable);
              }
              else if (result.getCheck() != ApkCheck.OK) {
                  HymnsApp.showToastMessage(R.string.update_apk_invalid, result.getCheck().name());
              }
              else if (!UpdateNotifier.showReady(context, release.getApkName(), release.getVersionName())) {
                  HymnsApp.showToastMessage(R.string.update_ready_use_about);
              }
              else {
                  HymnsApp.showToastMessage(R.string.update_downloaded_tap_notification);
              }
          }
          finally {
              removeOldDownloads();
          }
      }

      private class DownloadReceiver extends BroadcastReceiver {
          private final ReleaseInfo release;
          private final String sha256;

          DownloadReceiver(ReleaseInfo release, String sha256) {
              this.release = release;
              this.sha256 = sha256;
          }

          @Override
          public void onReceive(Context context, Intent intent) {
              long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
              List<Long> ids = getOldDownloads();
              if (ids.isEmpty() || ids.get(ids.size() - 1) != id) {
                  return;
              }
              unregisterDownloadReceiver();
              PendingResult pending = goAsync();
              new Thread(() -> {
                  try {
                      verifyFinishedDownload(id, release, sha256);
                  }
                  finally {
                      pending.finish();
                  }
              }, "hymnal-apk-verify").start();
          }
      }

      @SuppressLint("Range")
      private int checkDownloadStatus(long id) {
          DownloadManager.Query query = new DownloadManager.Query().setFilterById(id);
          try (Cursor cursor = HymnsApp.getDownloadManager().query(query)) {
              if (cursor == null || !cursor.moveToFirst()) {
                  return DownloadManager.STATUS_FAILED;
              }
              return cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
          }
      }

      /**
       * The fixed download destination (app-specific Download dir + hymnal-X.Y.Z.apk). DownloadManager's
       * COLUMN_LOCAL_URI is not used: it may be a content:// uri on some versions.
       *
       * @return the file, or null when the directory is unavailable or the name escapes it
       */
      @Nullable
      private static File expectedDownloadFile(ReleaseInfo release) {
          File dir = HymnsApp.getGlobalContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
          if (dir == null || !ReleaseConvention.isApkName(release.getApkName())) {
              return null;
          }
          try {
              File file = new File(dir, release.getApkName()).getCanonicalFile();
              String root = dir.getCanonicalPath() + File.separator;
              return file.getPath().startsWith(root) ? file : null;
          }
          catch (IOException e) {
              Timber.w(e, "Cannot resolve download path");
              return null;
          }
      }

      private SharedPreferences getStore() {
          if (store == null) {
              store = HymnsApp.getGlobalContext().getSharedPreferences("store", Context.MODE_PRIVATE);
          }
          return store;
      }

      private void rememberDownloadId(long id) {
          SharedPreferences store = getStore();
          store.edit().putString(ENTRY_NAME, store.getString(ENTRY_NAME, "") + id + ",").apply();
      }

      private List<Long> getOldDownloads() {
          String storeStr = getStore().getString(ENTRY_NAME, "");
          String[] idStrs = storeStr.split(",");
          List<Long> apkIds = new ArrayList<>(idStrs.length);
          for (String idStr : idStrs) {
              try {
                  if (!idStr.isEmpty()) {
                      apkIds.add(Long.parseLong(idStr));
                  }
              }
              catch (NumberFormatException e) {
                  Timber.e("Error parsing apk id for string: %s [%s]", idStr, storeStr);
              }
          }
          return apkIds;
      }

      /**
       * Removes old update downloads (DownloadManager deletes their files) and staged apks that are already
       * installed. Called at app start-up.
       */
      public void removeOldDownloads() {
          DownloadManager downloadManager = HymnsApp.getDownloadManager();
          for (long id : getOldDownloads()) {
              downloadManager.remove(id);
          }
          getStore().edit().remove(ENTRY_NAME).apply();

          SemVer installed = SemVer.parse(VersionServiceImpl.getInstance().getCurrentVersionName());
          File[] staged = ApkVerifier.updatesDir(HymnsApp.getGlobalContext()).listFiles();
          if (staged != null && installed != null) {
              for (File apk : staged) {
                  SemVer version = SemVer.parse(apk.getName().replaceFirst("^hymnal-", "").replaceFirst("\\.apk$", ""));
                  if (version == null || version.compareTo(installed) <= 0) {
                      Timber.d("Deleting staged %s", apk.getName());
                      apk.delete();
                  }
              }
          }
      }
  }
  ```

- [ ] **Step 11：修改 `OnlineUpdateService.java`**
  1. 在 `import org.cog.hymnchtv.service.androidnotification.NotificationHelper;` 下一行加 `import org.cog.hymnchtv.update.MediaLinksUpdater;`。
  2. `UPDATE_AVAIL_TAG` 的值改成 `"hymnal_update_available"`。
  3. `case ACTION_AUTO_UPDATE_START:` 的區塊改成：

     ```java
                     case ACTION_AUTO_UPDATE_START:
                         // First run, or an upgrade with a newer bundled list: import media links on this worker thread.
                         MediaLinksUpdater.importBundledIfNeeded(getApplicationContext());
                         setNextAlarm(CHECK_INTERVAL_ON_LAUNCH);
                         break;
     ```

  4. `nBuilder.setContentTitle(getString(R.string.app_title_main));` 改成 `nBuilder.setContentTitle(getString(R.string.app_name));`。

- [ ] **Step 12：`res/xml/file_paths.xml`**：在 `<paths>` 的最後一個子元素之後加入：

  ```xml
      <!-- Verified update apks (sub-project Z): the only root used to hand an apk to the installer. -->
      <files-path
          name="updates"
          path="updates/" />
  ```

  確認既有根目錄仍在使用中（只記錄，不刪除）：

  ```bash
  grep -rn "getUriForFile" hymnchtv/src/main/java | grep -v "public static Uri getUriForFile"
  ```

  Expected: `ContentHandler`、`LogUploadServiceImpl`、`MediaConfig`、`FileBackend`、`UpdateInstallActivity` 的呼叫。前四個用的是 `Download/hymnal/` 下的檔案（`external-path`），所以既有根目錄要保留。

- [ ] **Step 13：三個語系的 `strings_update.xml`**

  `values/strings_update.xml`

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <!-- GitHub Releases updater (sub-project Z). Own file to avoid merge conflicts in strings.xml. -->
  <resources>
      <string name="update_new_available">Version %1$s is available (installed: %2$s).</string>
      <string name="update_up_to_date">Installed version %1$s is up to date.\nLatest release: %2$s.</string>
      <string name="update_notification_text">New version available: %1$s</string>
      <string name="update_check_rate_limited">Too many update checks right now. Please try again later.</string>
      <string name="update_check_network_error">Cannot reach the update server. Check your network connection and try again.</string>
      <string name="update_check_no_release">No release has been published yet.</string>
      <string name="update_check_failed">Update check failed. Please try again later.</string>
      <string name="update_apk_invalid">The downloaded installer failed verification (%1$s) and was deleted.</string>
      <string name="update_ready_to_install">Hymnal %1$s is ready. Tap to install.</string>
      <string name="update_downloaded_tap_notification">Download complete. Tap the notification to install.</string>
      <string name="update_ready_use_about">Download complete. Open About and tap Update to install.</string>
      <string name="update_install_missing">The downloaded installer is no longer available. Please check for updates again.</string>
      <string name="update_install_needs_permission">Allow installing unknown apps for Hymnal to update.</string>
      <string name="update_signer_unverifiable">This Android version could not verify the installer\'s signature, so the update was stopped. Please install the new version manually.</string>
  </resources>
  ```

  `values-zh/strings_update.xml`

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <resources>
      <string name="update_new_available">有新版本 %1$s 可下载（目前安装：%2$s）。</string>
      <string name="update_up_to_date">目前安装的版本 %1$s 已是最新。\n最新发布：%2$s。</string>
      <string name="update_notification_text">有新版本：%1$s</string>
      <string name="update_check_rate_limited">检查更新的次数过多，请稍后再试。</string>
      <string name="update_check_network_error">无法连接更新服务器，请检查网络后再试。</string>
      <string name="update_check_no_release">目前尚无发布版本。</string>
      <string name="update_check_failed">检查更新失败，请稍后再试。</string>
      <string name="update_apk_invalid">下载的安装包验证失败（%1$s），已删除。</string>
      <string name="update_ready_to_install">诗歌 %1$s 已下载，点此安装。</string>
      <string name="update_downloaded_tap_notification">下载完成，请点通知完成安装。</string>
      <string name="update_ready_use_about">下载完成。请到“关于”按“更新”安装。</string>
      <string name="update_install_missing">找不到已下载的安装包，请重新检查更新。</string>
      <string name="update_install_needs_permission">需要允许诗歌“安装未知应用”才能更新。</string>
      <string name="update_signer_unverifiable">此 Android 版本无法验证安装包的签名，已停止自动更新。请手动下载新版安装。</string>
  </resources>
  ```

  `values-b+zh+Hant/strings_update.xml`

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <resources>
      <string name="update_new_available">有新版本 %1$s 可下載（目前安裝：%2$s）。</string>
      <string name="update_up_to_date">目前安裝的版本 %1$s 已是最新。\n最新發佈：%2$s。</string>
      <string name="update_notification_text">有新版本：%1$s</string>
      <string name="update_check_rate_limited">檢查更新的次數過多，請稍後再試。</string>
      <string name="update_check_network_error">無法連線到更新伺服器，請檢查網路後再試。</string>
      <string name="update_check_no_release">目前尚無發佈版本。</string>
      <string name="update_check_failed">檢查更新失敗，請稍後再試。</string>
      <string name="update_apk_invalid">下載的安裝檔驗證失敗（%1$s），已刪除。</string>
      <string name="update_ready_to_install">詩歌 %1$s 已下載，點此安裝。</string>
      <string name="update_downloaded_tap_notification">下載完成，請點選通知完成安裝。</string>
      <string name="update_ready_use_about">下載完成。請到「關於」按「更新」安裝。</string>
      <string name="update_install_missing">找不到已下載的安裝檔，請重新檢查更新。</string>
      <string name="update_install_needs_permission">需要允許詩歌「安裝不明應用程式」才能更新。</string>
      <string name="update_signer_unverifiable">此 Android 版本無法驗證安裝檔的簽章，已停止自動更新。請手動下載新版安裝。</string>
  </resources>
  ```

- [ ] **Step 14：debug 專用的 `hymnchtv/src/debug/res/xml/network_security_config.xml`**：內容是 main 版本加上 `10.0.2.2`。

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <!-- Debug only. Same as src/main/res/xml/network_security_config.xml (keep in sync) plus cleartext access to the
       emulator host 10.0.2.2 for the mock GitHub server used in sub-project Z verification. -->
  <network-security-config>
      <base-config>
          <trust-anchors>
              <certificates src="system" />
          </trust-anchors>
      </base-config>

      <domain-config cleartextTrafficPermitted="true">
          <domain includeSubdomains="true">witness-lee-hymns.org/</domain>
          <domain includeSubdomains="true">lightinnj.org</domain>
          <domain includeSubdomains="true">g.cgbr.org</domain>
          <domain includeSubdomains="true">mana.stmn1.com</domain>
          <domain includeSubdomains="true">four.soqimp.com</domain>
          <domain includeSubdomains="false">10.0.2.2</domain>
      </domain-config>
  </network-security-config>
  ```

  用 `diff` 和 main 版本比對，差異應該只有註解和 `10.0.2.2` 這一行。

- [ ] **Step 15：instrumented test** `hymnchtv/src/androidTest/java/org/cog/hymnchtv/update/SigningCertsTest.kt`

  ```kotlin
  package org.cog.hymnchtv.update

  import android.os.Build
  import android.util.Log
  import androidx.test.ext.junit.runners.AndroidJUnit4
  import androidx.test.platform.app.InstrumentationRegistry
  import com.google.common.truth.Truth.assertThat
  import org.junit.Test
  import org.junit.runner.RunWith
  import java.io.File

  /** Certificate extraction on a real device. A differently signed archive is checked manually in Task 13. */
  @RunWith(AndroidJUnit4::class)
  class SigningCertsTest {
      private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

      @Test
      fun installedAppHasExactlyOneSigner() {
          val installed = SigningCerts.installedApp(context)
          assertThat(installed.signers).hasSize(1)
          installed.signers.forEach { assertThat(it).matches("[0-9a-f]{64}") }
      }

      /** Our own installed APK file, read as an archive, must carry the same signer and package. */
      @Test
      fun ownApkReadAsArchiveMatchesInstalledSigner() {
          val archive = SigningCerts.archive(context, File(context.applicationInfo.sourceDir))
          assertThat(archive.packageName).isEqualTo(context.packageName)
          Log.i("SigningCertsTest", "API ${Build.VERSION.SDK_INT}: archive signers = ${archive.signers}")
          // Hard requirement on every API level (incl. 24–27 via GET_SIGNATURES): otherwise updates fail closed.
          assertThat(archive.signers).isNotEmpty()
          assertThat(archive.signers).isEqualTo(SigningCerts.installedApp(context).signers)
      }

      /** The trust decision on this device: our own APK is trusted as far as the signer is concerned. */
      @Test
      fun trustDecisionAcceptsOwnSigner() {
          val installed = SigningCerts.installedApp(context)
          val archive = SigningCerts.archive(context, File(context.applicationInfo.sourceDir))
          assertThat(ApkTrust.isSameSigner(installed.signers, archive.signers.orEmpty(), archive.signerHistory)).isTrue()
      }
  }
  ```

  在 api34 與 api24 上執行（啟動方式見 Task 13 Step 1）：

  ```bash
  $G :hymnchtv:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=org.cog.hymnchtv.update.SigningCertsTest
  adb logcat -d -s SigningCertsTest:I | tail -1
  ```

  在 api34、api26、api24 上執行（api26 的 AVD 在 Task 13 Step 1 建立；需要時先做那一步）。
  Expected: 3 個測試在三台模擬器上都通過。**若 api24 或 api26 的 `ownApkReadAsArchiveMatchesInstalledSigner` 失敗，停下來回報**：那代表在該版本上讀不到 archive 簽章，所有更新都會被拒絕（`SIGNER_UNVERIFIABLE`），需要使用者決定接下來怎麼做。

- [ ] **Step 16：建置與測試**

  ```bash
  $G :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug
  grep -rn "cmeng\|version.properties\|url_import" hymnchtv/src/main/java/org/cog/hymnchtv/service/androidupdate/
  ```

  Expected: `BUILD SUCCESSFUL`；`grep` 沒有輸出。

- [ ] **Step 17：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/update hymnchtv/src/main/java/org/cog/hymnchtv/service/androidupdate \
    hymnchtv/src/main/res/xml/file_paths.xml hymnchtv/src/main/res/values*/strings_update.xml \
    hymnchtv/src/debug/res/xml/network_security_config.xml hymnchtv/src/androidTest/java/org/cog/hymnchtv/update
  git commit -m "feat: verified GitHub Releases updates with notification-driven install; bundled-only media links" \
    -m "SigningCertsTest passes on api24/26/34" \
    -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

### Task 7：資源與儲存（Lane B）

**Files:** Modify `glide/AssetFile.java`、`persistance/FileBackend.java`；Create `src/androidTest/java/org/cog/hymnchtv/AssetAndStorageTest.kt`。**衝突風險：** 無（Z 專屬）。

- [ ] **Step 1：寫測試** `AssetAndStorageTest.kt`

  ```kotlin
  package org.cog.hymnchtv

  import androidx.test.ext.junit.runners.AndroidJUnit4
  import androidx.test.platform.app.InstrumentationRegistry
  import com.google.common.truth.Truth.assertThat
  import org.cog.hymnchtv.glide.AssetFile
  import org.cog.hymnchtv.persistance.FileBackend
  import org.junit.Test
  import org.junit.runner.RunWith

  @RunWith(AndroidJUnit4::class)
  class AssetAndStorageTest {
      @Test
      fun assetFileOpensOwnAssets() {
          val stream = AssetFile(InstrumentationRegistry.getInstrumentation().targetContext, "url_import.txt").inputStream
          assertThat(stream).isNotNull()
          stream?.close()
      }

      @Test
      fun publicStoreDirectoryIsHymnal() {
          assertThat(FileBackend.FP_HYMNCHTV).isEqualTo("/hymnal")
      }
  }
  ```

  在 api34 上執行：`$G :hymnchtv:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=org.cog.hymnchtv.AssetAndStorageTest`。
  Expected: `publicStoreDirectoryIsHymnal` 失敗。`assetFileOpensOwnAssets` 會通過，因為 lane 中的 applicationId 還是舊的；它真正的證明在 Task 12 的新 applicationId 之下。

- [ ] **Step 2：`AssetFile.java`**：把

  ```java
          if (assetManager == null) {
              try {
                  Context ctx = context.createPackageContext("org.cog.hymnchtv", 0);
                  assetManager = ctx.getAssets();
              } catch (Exception e) {
                  Timber.w("Create AssetManager Exception: %s", e.getMessage());
              }
          }
  ```

  改成

  ```java
          if (assetManager == null) {
              // Own assets: never look the package up by a hard-coded name (applicationId != code package).
              assetManager = context.getApplicationContext().getAssets();
          }
  ```

- [ ] **Step 3：`FileBackend.java`**：把

  ```java
      // android-Q accessible path to apk is: /storage/emulated/0/Android/data/org.cog.hymnchtv/files
      public static String FP_HYMNCHTV = "/hymnchtv";
  ```

  改成

  ```java
      // Public store Download/hymnal (sub-project Z): separate from the original hymnchtv app, whose files this app
      // could not overwrite under scoped storage.
      public static String FP_HYMNCHTV = "/hymnal";
  ```

  同檔約第 225 行的 `Download/hymnchtv` 改為 `Download/hymnal`。

- [ ] **Step 4：重跑 Step 1**。Expected: 2 個測試通過。

- [ ] **Step 5：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/glide/AssetFile.java hymnchtv/src/main/java/org/cog/hymnchtv/persistance/FileBackend.java \
    hymnchtv/src/androidTest/java/org/cog/hymnchtv/AssetAndStorageTest.kt
  git commit -m "fix: open own assets without a hard-coded package and move public store to Download/hymnal" \
    -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

### Task 8：授權清單解析與 AboutLibraries 設定檔（Lane B）

**Files:**
- Create：`about/AboutLibrariesJson.kt`、`about/LicenseText.kt`、`src/test/resources/about/aboutlibraries_sample.json`、`AboutLibrariesJsonTest.kt`、`LicenseTextTest.kt`
- Create：`hymnchtv/aboutlibraries-config/libraries/hymnchtv-original.json`、`opencc-data.json`、`hymnchtv/src/main/res/raw/z_keep_aboutlibraries.xml`

**衝突風險：** 無（新檔案；A2 之後只會新增 `hymnalkai-font.json`）。

- [ ] **Step 1：fixture** `hymnchtv/src/test/resources/about/aboutlibraries_sample.json`
  - 前三個 library 與 `licenses` 取自 AboutLibraries 15.2.0 repo 中 plugin 的真實產出 `app-test/files/aboutlibraries.json`。
  - 授權全文已截短；`9be0c4…` 這個授權在原始產出中本來就沒有 `content`。
  - 最後兩個 library 是 Z 的自訂設定檔，經 plugin 合併後的樣子。

  ```json
  {
    "libraries": [
      {
        "uniqueId": "javax.annotation:javax.annotation-api",
        "artifactVersion": "1.3.2",
        "name": "javax.annotation API",
        "description": "Common Annotations for the JavaTM Platform API",
        "website": "http://jcp.org/en/jsr/detail?id=250",
        "licenses": ["e1692074a62fa0fd6ef3ef00ec4904f0", "9be0c4d7964ad9a68deb2e9706266b8c"]
      },
      {
        "uniqueId": "org.jetbrains:annotations",
        "artifactVersion": "13.0",
        "name": "IntelliJ IDEA Annotations",
        "description": "A set of annotations used for code inspection support and code documentation.",
        "website": "http://www.jetbrains.org",
        "licenses": ["Apache-2.0", "fea9e903303ed8cbc7854c24956a8913"]
      },
      {
        "uniqueId": "org.jetbrains.kotlin:kotlin-stdlib",
        "artifactVersion": "2.4.10",
        "name": "Kotlin Stdlib",
        "description": "Kotlin Standard Library",
        "website": "https://kotlinlang.org/",
        "licenses": ["Apache-2.0"]
      },
      {
        "uniqueId": "org.byvoid:opencc-data",
        "artifactVersion": "1.4.2",
        "name": "OpenCC (build-time data generation)",
        "description": "Traditional Chinese lyrics data were generated at build time with OpenCC. OpenCC itself is not bundled in this app.",
        "developers": [],
        "licenses": ["Apache-2.0"]
      },
      {
        "uniqueId": "org.cog:hymnchtv",
        "artifactVersion": "2.9.2",
        "name": "hymnchtv (original project)",
        "description": "This app is a modified version of hymnchtv. Copyright 2020 Eng Chong Meng. Licensed under the Apache License, Version 2.0.",
        "developers": [],
        "licenses": ["Apache-2.0"]
      }
    ],
    "licenses": {
      "e1692074a62fa0fd6ef3ef00ec4904f0": {
        "name": "Other",
        "url": "https://raw.githubusercontent.com/javaee/javax.annotation/master/LICENSE",
        "content": "COMMON DEVELOPMENT AND DISTRIBUTION LICENSE (CDDL) Version 1.1\n\n1. Definitions.\n\n    1.1. \"Contributor\" means each individual or entity that creates or\n    cont",
        "hash": "e1692074a62fa0fd6ef3ef00ec4904f0"
      },
      "9be0c4d7964ad9a68deb2e9706266b8c": {
        "name": "CDDL + GPLv2 with classpath exception",
        "url": "https://github.com/javaee/javax.annotation/blob/master/LICENSE",
        "hash": "9be0c4d7964ad9a68deb2e9706266b8c"
      },
      "Apache-2.0": {
        "name": "Apache License 2.0",
        "url": "https://spdx.org/licenses/Apache-2.0.html",
        "content": "Apache License\nVersion 2.0, January 2004\nhttp://www.apache.org/licenses/\n\nTERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION\n\n1. Definitions.\n\n\"Licens",
        "internalHash": "Apache-2.0",
        "spdxId": "Apache-2.0",
        "hash": "Apache-2.0"
      },
      "fea9e903303ed8cbc7854c24956a8913": {
        "name": "Other",
        "url": "https://raw.githubusercontent.com/JetBrains/intellij-community/master/LICENSE.txt",
        "content": "JETBRAINS OPEN-SOURCE BUILD TERMS\n\nVersion 1.3, effective as of June 15, 2026\n\nIMPORTANT! READ CAREFULLY:\n\nTHESE TERMS APPLY TO THE OPEN-SOURCE BUILDS OF THE JE",
        "hash": "fea9e903303ed8cbc7854c24956a8913"
      }
    }
  }
  ```

- [ ] **Step 2：寫會失敗的測試**

  `hymnchtv/src/test/java/org/cog/hymnchtv/about/AboutLibrariesJsonTest.kt`

  ```kotlin
  package org.cog.hymnchtv.about

  import com.google.common.truth.Truth.assertThat
  import org.junit.Test

  class AboutLibrariesJsonTest {
      private val sample = checkNotNull(javaClass.getResource("/about/aboutlibraries_sample.json")).readText()
      private val rows = AboutLibrariesJson.parse(sample)

      @Test
      fun parsesEveryLibrary() {
          assertThat(rows).hasSize(5)
      }

      @Test
      fun pinnedProjectEntriesComeFirstThenAlphabetical() {
          assertThat(rows.map { it.id }).containsExactly(
              "org.cog:hymnchtv",
              "org.byvoid:opencc-data",
              "org.jetbrains:annotations",
              "javax.annotation:javax.annotation-api",
              "org.jetbrains.kotlin:kotlin-stdlib",
          ).inOrder()
      }

      @Test
      fun originalProjectCarriesApacheNoticeAndText() {
          val original = rows.first()
          assertThat(original.description).contains("Copyright 2020 Eng Chong Meng")
          assertThat(original.licenses.single().name).isEqualTo("Apache License 2.0")
          assertThat(original.licenses.single().content).startsWith("Apache License")
      }

      @Test
      fun resolvesHashKeyedLicensesAndKeepsUrlWhenTextIsMissing() {
          val javax = rows.first { it.id == "javax.annotation:javax.annotation-api" }
          assertThat(javax.licenses.map { it.name }).containsExactly("Other", "CDDL + GPLv2 with classpath exception").inOrder()
          assertThat(javax.licenses[1].content).isNull()
          assertThat(javax.licenses[1].url).isEqualTo("https://github.com/javaee/javax.annotation/blob/master/LICENSE")
      }

      @Test
      fun unknownLicenseKeyFallsBackToTheKey() {
          val json = """{"libraries":[{"uniqueId":"a:b","name":"B","licenses":["MPL-2.0"]}],"licenses":{}}"""
          assertThat(AboutLibrariesJson.parse(json).single().licenses.single().name).isEqualTo("MPL-2.0")
      }

      @Test
      fun toleratesMissingOptionalFields() {
          val json = """{"libraries":[{"uniqueId":"a:b"}]}"""
          val row = AboutLibrariesJson.parse(json).single()
          assertThat(row.name).isEqualTo("a:b")
          assertThat(row.version).isNull()
          assertThat(row.licenses).isEmpty()
      }

      @Test
      fun malformedInputGivesEmptyList() {
          listOf(null, "", "{", "[]", """{"libraries":"x"}""").forEach { assertThat(AboutLibrariesJson.parse(it)).isEmpty() }
      }
  }
  ```

  `hymnchtv/src/test/java/org/cog/hymnchtv/about/LicenseTextTest.kt`

  ```kotlin
  package org.cog.hymnchtv.about

  import com.google.common.truth.Truth.assertThat
  import org.junit.Test

  class LicenseTextTest {
      @Test
      fun plainStripsBreakTagsAndNormalisesLineEndings() {
          assertThat(LicenseText.plain("1. Intro\n<br />\r\n2. Terms<br/>x")).isEqualTo("1. Intro\n\n2. Terms\nx")
      }

      @Test
      fun titleShowsNameVersionAndLicenses() {
          val row = LicenseRow("a:b", "Lib", "1.0", null, listOf(LicenseInfo("MIT", "MIT License", null, "text")))
          assertThat(LicenseText.title(row)).isEqualTo("Lib 1.0 — MIT License")
      }

      @Test
      fun detailIncludesNoticeTextAndPlainUrlFallback() {
          val row = LicenseRow(
              "a:b", "Lib", null, "Copyright X",
              listOf(LicenseInfo("k", "Some License", "https://example.org/LICENSE", null), LicenseInfo("Apache-2.0", "Apache License 2.0", null, "Apache text")),
          )
          val detail = LicenseText.detail(row, "（未附授權全文）")
          assertThat(detail).contains("Copyright X")
          assertThat(detail).contains("Some License\n（未附授權全文）\nhttps://example.org/LICENSE")
          assertThat(detail).contains("Apache License 2.0\nApache text")
      }
  }
  ```

- [ ] **Step 3：執行，確認失敗**：`$G :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.about.*'`。Expected: `Unresolved reference`。

- [ ] **Step 4：實作**

  `hymnchtv/src/main/java/org/cog/hymnchtv/about/AboutLibrariesJson.kt`

  ```kotlin
  package org.cog.hymnchtv.about

  import org.json.JSONException
  import org.json.JSONObject

  data class LicenseInfo(val key: String, val name: String, val url: String?, val content: String?)

  data class LicenseRow(
      val id: String,
      val name: String,
      val version: String?,
      val description: String?,
      val licenses: List<LicenseInfo>,
  )

  /** Reads the aboutlibraries.json generated by the AboutLibraries Gradle plugin (R.raw.aboutlibraries). */
  object AboutLibrariesJson {
      /** Project notices shown first, in this order; the font entry is added by sub-project A2. */
      val PINNED = listOf("org.cog:hymnchtv", "org.byvoid:opencc-data", "org.cog.hymnal:hymnalkai-font")

      @JvmStatic
      fun parse(json: String?): List<LicenseRow> {
          if (json.isNullOrBlank()) return emptyList()
          return try {
              val root = JSONObject(json)
              val licenses = root.optJSONObject("licenses") ?: JSONObject()
              val libraries = root.getJSONArray("libraries")
              (0 until libraries.length())
                  .mapNotNull { libraries.optJSONObject(it)?.let { lib -> toRow(lib, licenses) } }
                  .sortedWith(compareBy<LicenseRow> { pinIndex(it.id) }.thenBy { it.name.lowercase() })
          } catch (e: JSONException) {
              emptyList()
          }
      }

      private fun pinIndex(id: String): Int = PINNED.indexOf(id).let { if (it < 0) PINNED.size else it }

      private fun toRow(lib: JSONObject, licenses: JSONObject): LicenseRow? {
          val id = lib.stringOrNull("uniqueId") ?: return null
          val keys = lib.optJSONArray("licenses")
          val infos = (0 until (keys?.length() ?: 0)).mapNotNull { keys?.optString(it)?.takeIf { k -> k.isNotEmpty() } }
              .map { key -> toLicense(key, licenses.optJSONObject(key)) }
          return LicenseRow(id, lib.stringOrNull("name") ?: id, lib.stringOrNull("artifactVersion"), lib.stringOrNull("description"), infos)
      }

      private fun toLicense(key: String, obj: JSONObject?): LicenseInfo =
          LicenseInfo(key, obj?.stringOrNull("name") ?: key, obj?.stringOrNull("url"), obj?.stringOrNull("content"))

      private fun JSONObject.stringOrNull(key: String): String? =
          if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
  }
  ```

  `hymnchtv/src/main/java/org/cog/hymnchtv/about/LicenseText.kt`

  ```kotlin
  package org.cog.hymnchtv.about

  /** Plain-text rendering for the native licenses screen (no HTML, no clickable links). */
  object LicenseText {
      private val BREAK_TAG = Regex("""<br\s*/?>\s*\n?""", RegexOption.IGNORE_CASE)

      @JvmStatic
      fun plain(content: String): String = content.replace("\r\n", "\n").replace(BREAK_TAG, "\n")

      @JvmStatic
      fun title(row: LicenseRow): String {
          val version = row.version?.let { " $it" }.orEmpty()
          val licenses = row.licenses.joinToString(", ") { it.name }
          return if (licenses.isEmpty()) "${row.name}$version" else "${row.name}$version — $licenses"
      }

      /** Description, then each license's name and full text; the license URL only when no text is bundled. */
      @JvmStatic
      fun detail(row: LicenseRow, missingTextLabel: String): String = buildString {
          row.description?.let { append(it).append("\n\n") }
          row.licenses.forEachIndexed { index, license ->
              if (index > 0) append("\n\n")
              append(license.name).append('\n')
              val content = license.content
              if (content != null) append(plain(content).trim())
              else {
                  append(missingTextLabel)
                  license.url?.let { append('\n').append(it) }
              }
          }
      }
  }
  ```

  註：`plain("1. Intro\n<br />\r\n2. Terms<br/>x")` 先把 `\r\n` 換成 `\n`，再把「`<br />` 加上後面的換行」換成一個 `\n`，結果是 `"1. Intro\n\n2. Terms\nx"`，和測試一致。

- [ ] **Step 5：AboutLibraries 自訂設定檔與 keep 檔**

  `hymnchtv/aboutlibraries-config/libraries/hymnchtv-original.json`

  ```json
  {
    "uniqueId": "org.cog:hymnchtv",
    "artifactVersion": "2.9.2",
    "name": "hymnchtv (original project)",
    "description": "This app is a modified version of hymnchtv. Copyright 2020 Eng Chong Meng. Licensed under the Apache License, Version 2.0.",
    "developers": [],
    "licenses": ["Apache-2.0"]
  }
  ```

  `hymnchtv/aboutlibraries-config/libraries/opencc-data.json`

  ```json
  {
    "uniqueId": "org.byvoid:opencc-data",
    "artifactVersion": "1.4.2",
    "name": "OpenCC (build-time data generation)",
    "description": "Traditional Chinese lyrics data were generated at build time with OpenCC. OpenCC itself is not bundled in this app.",
    "developers": [],
    "licenses": ["Apache-2.0"]
  }
  ```

  `hymnchtv/aboutlibraries-config/license-allowlist.txt`（Task 12 的硬性授權閘門讀這個檔案；只有使用者可以新增項目）

  ```text
  # Approved licenses for bundled libraries (sub-project Z). assembleRelease fails on anything not listed here,
  # on libraries without a license, and on licenses without full text. Only the project owner adds entries.
  #   license <key>               approve a license key for every library
  #   library <uniqueId> <key>    approve one license key for one library (hash-keyed or unusual licenses)
  license Apache-2.0
  license MIT
  license BSD-2-Clause
  license BSD-3-Clause
  license OFL-1.1
  ```

  `hymnchtv/src/main/res/raw/z_keep_aboutlibraries.xml`

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <!-- Keep the AboutLibraries-generated license data if resource shrinking is enabled (AGP 9 default). -->
  <resources xmlns:tools="http://schemas.android.com/tools"
      tools:keep="@raw/aboutlibraries" />
  ```

- [ ] **Step 6：執行，確認通過**：`AboutLibrariesJsonTest` 7、`LicenseTextTest` 3 通過；`$G :hymnchtv:assembleDebug` 成功（keep 檔所引用的資源要到 Task 12 才存在，aapt 不會檢查 `tools:` 屬性）。

- [ ] **Step 7：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/about hymnchtv/src/test/java/org/cog/hymnchtv/about hymnchtv/src/test/resources/about \
    hymnchtv/aboutlibraries-config hymnchtv/src/main/res/raw/z_keep_aboutlibraries.xml  # 含 license-allowlist.txt
  git commit -m "feat: parse AboutLibraries output for a native licenses list; add project notice entries" \
    -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

### Task 9：About、授權、說明畫面（Lane B）

**Files:**
- Rewrite `About.java`、`res/layout/about.xml`
- Create `about/HelpActivity.kt`、`about/LicensesActivity.kt`、`res/layout/help.xml`、`res/layout/licenses.xml`、`strings_about.xml` ×3、`strings_help.xml` ×3

**衝突風險：** 無（Z 專屬）。Activity 的註冊在 Task 12。

- [ ] **Step 1：`strings_about.xml`**

  `values/strings_about.xml`

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <!-- About / licenses (sub-project Z). Own file to avoid merge conflicts in strings.xml. -->
  <resources>
      <string name="about_version">Version %1$s</string>
      <string name="about_description">Lyrics, scores and media player for the hymns. Lyrics and score contents are copyrighted by Taiwan Gospel Book Room; please do not use them for commercial purposes.</string>
      <string name="about_licenses_title">Open-source licenses</string>
      <string name="about_licenses_empty">The license list is not available in this build.</string>
      <string name="about_license_text_missing">(Full license text not bundled)</string>
  </resources>
  ```

  `values-zh/strings_about.xml`

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <resources>
      <string name="about_version">版本 %1$s</string>
      <string name="about_description">诗歌的歌词、乐谱浏览与播放。歌词与乐谱内容版权属台湾福音书房，请勿用于商业用途。</string>
      <string name="about_licenses_title">开源授权</string>
      <string name="about_licenses_empty">此版本无法显示授权清单。</string>
      <string name="about_license_text_missing">（未附授权全文）</string>
  </resources>
  ```

  `values-b+zh+Hant/strings_about.xml`

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <resources>
      <string name="about_version">版本 %1$s</string>
      <string name="about_description">詩歌的歌詞、樂譜瀏覽與播放。歌詞與樂譜內容版權屬台灣福音書房，請勿用於商業用途。</string>
      <string name="about_licenses_title">開源授權</string>
      <string name="about_licenses_empty">此版本無法顯示授權清單。</string>
      <string name="about_license_text_missing">（未附授權全文）</string>
  </resources>
  ```

- [ ] **Step 2：`strings_help.xml`**（純文字，不含 HTML 或網址）

  `values/strings_help.xml`

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <!-- In-app help (sub-project Z): plain text only, no links. -->
  <resources>
      <string name="help_text">Finding a hymn\n• Enter a hymn number and tap the hymnal button to show lyrics and score.\n• Or browse by category, stroke count, pinyin or the English–Chinese index.\n• Content search: type words and tap Search; long-press Search to turn 他 into 祂.\n\nLyrics page\n• Long-press the page to open the menu.\n• Swipe left or right for the previous or next hymn.\n• Turn the phone sideways for larger lyrics and score; the player hides automatically.\n\nPlaying media\n• Tap Play to start, tap again to pause, and again to resume.\n• Long-press Play to restart from the beginning.\n\nMedia configuration\n• Add your own audio or video links for each hymn, and export or import them to share with other devices.\n• Media files you download yourself go under Download/hymnal/.\n\nUpdates\n• About → Update checks for a new version. After it downloads, tap the notification to install.</string>
  </resources>
  ```

  `values-zh/strings_help.xml`

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <resources>
      <string name="help_text">查找诗歌\n• 输入诗歌编号后按诗歌本按钮，即可显示歌词与乐谱。\n• 也可以从诗歌类别、笔画索引、拼音索引或英中对照查找。\n• 内容搜索：输入字词后按“内容搜索”；长按可把“他”转成“祂”。\n\n歌词页\n• 长按页面开启选单。\n• 左右滑动切换上一首或下一首。\n• 手机横放时，歌词与乐谱放大，播放器自动隐藏。\n\n媒体播放\n• 按播放键开始，再按一次暂停，再按一次从暂停处继续。\n• 长按播放键从头播放。\n\n媒体配置\n• 可为每首诗歌加入自定义的影音链接，并导出或导入，与其他设备分享。\n• 自行下载的媒体文件放在 Download/hymnal/ 底下。\n\n更新\n• “关于”→“更新”可检查新版本；下载完成后点通知即可安装。</string>
  </resources>
  ```

  `values-b+zh+Hant/strings_help.xml`

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <resources>
      <string name="help_text">查找詩歌\n• 輸入詩歌編號後按詩歌本按鈕，即可顯示歌詞與樂譜。\n• 也可以從詩歌類別、筆畫索引、拼音索引或英中對照查找。\n• 內容搜尋：輸入字詞後按「內容搜尋」；長按可把「他」轉成「祂」。\n\n歌詞頁\n• 長按頁面開啟選單。\n• 左右滑動切換上一首或下一首。\n• 手機橫放時，歌詞與樂譜放大，播放器自動隱藏。\n\n媒體播放\n• 按播放鍵開始，再按一次暫停，再按一次從暫停處繼續。\n• 長按播放鍵從頭播放。\n\n媒體配置\n• 可為每首詩歌加入自訂的影音連結，並匯出或匯入，與其他裝置分享。\n• 自行下載的媒體檔放在 Download/hymnal/ 底下。\n\n更新\n• 「關於」→「更新」可檢查新版本；下載完成後點選通知即可安裝。</string>
  </resources>
  ```

  Task 13 會用 App 實際操作逐條核對說明文字。哪一條和實際行為不符，就回報給使用者決定措辭，不要自行改寫。

- [ ] **Step 3：版面**

  `res/layout/help.xml`

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
      android:layout_width="match_parent"
      android:layout_height="match_parent"
      android:padding="16dp">

      <TextView
          android:id="@+id/help_text"
          android:layout_width="match_parent"
          android:layout_height="wrap_content"
          android:lineSpacingExtra="4dp"
          android:text="@string/help_text"
          android:textAppearance="?android:attr/textAppearanceMedium"
          android:textIsSelectable="true" />
  </ScrollView>
  ```

  `res/layout/licenses.xml`

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
      android:layout_width="match_parent"
      android:layout_height="match_parent">

      <ListView
          android:id="@+id/licenses_list"
          android:layout_width="match_parent"
          android:layout_height="match_parent" />

      <TextView
          android:id="@+id/licenses_empty"
          android:layout_width="match_parent"
          android:layout_height="match_parent"
          android:gravity="center"
          android:padding="24dp"
          android:text="@string/about_licenses_empty" />
  </FrameLayout>
  ```

  `res/layout/about.xml`（整檔改寫）

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <!-- About page (sub-project Z): native views, no WebView, no links, no personal data. -->
  <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
      xmlns:tools="http://schemas.android.com/tools"
      android:layout_width="match_parent"
      android:layout_height="wrap_content"
      android:orientation="vertical"
      android:padding="10dp">

      <LinearLayout
          android:layout_width="match_parent"
          android:layout_height="wrap_content"
          android:gravity="center"
          android:orientation="horizontal"
          android:padding="10dp">

          <ImageView
              android:layout_width="60dp"
              android:layout_height="60dp"
              android:contentDescription="@string/app_name"
              android:src="@drawable/hymnchtv" />

          <LinearLayout
              android:layout_width="wrap_content"
              android:layout_height="wrap_content"
              android:orientation="vertical"
              android:paddingStart="20dp"
              tools:ignore="RtlSymmetry">

              <TextView
                  android:layout_width="wrap_content"
                  android:layout_height="wrap_content"
                  android:text="@string/app_name"
                  android:textSize="20sp"
                  android:textStyle="bold" />

              <TextView
                  android:id="@+id/about_appVersion"
                  android:layout_width="wrap_content"
                  android:layout_height="wrap_content"
                  tools:text="Version 1.0.0" />
          </LinearLayout>
      </LinearLayout>

      <TextView
          android:layout_width="match_parent"
          android:layout_height="wrap_content"
          android:padding="10dp"
          android:text="@string/about_description" />

      <LinearLayout
          style="?android:attr/buttonBarStyle"
          android:layout_width="match_parent"
          android:layout_height="wrap_content">

          <Button
              android:id="@+id/about_licenses"
              style="@style/Button"
              android:text="@string/about_licenses_title" />

          <Button
              android:id="@+id/about_help"
              style="@style/Button"
              android:layout_marginStart="1dp"
              android:text="@string/help" />
      </LinearLayout>

      <LinearLayout
          style="?android:attr/buttonBarStyle"
          android:layout_width="match_parent"
          android:layout_height="wrap_content"
          android:paddingTop="10dp"
          android:paddingBottom="5dp">

          <Button
              android:id="@+id/history_log"
              style="@style/Button"
              android:text="@string/show_history_log" />

          <Button
              android:id="@+id/submit_logs"
              style="@style/Button"
              android:layout_marginStart="1dp"
              android:text="@string/send_logs" />

          <Button
              android:id="@+id/check_new_version"
              style="@style/Button"
              android:layout_marginStart="1dp"
              android:layout_weight="0.7"
              android:text="@string/app_update_check" />

          <Button
              android:id="@+id/ok_button"
              style="@style/Button"
              android:layout_marginStart="1dp"
              android:layout_weight="0.7"
              android:text="@string/ok" />
      </LinearLayout>
  </LinearLayout>
  ```

- [ ] **Step 4：`about/HelpActivity.kt`**

  ```kotlin
  package org.cog.hymnchtv.about

  import android.os.Bundle
  import org.cog.hymnchtv.BaseActivity
  import org.cog.hymnchtv.R

  /** In-app help text (sub-project Z replaces every online help link with this screen). */
  class HelpActivity : BaseActivity() {
      override fun onCreate(savedInstanceState: Bundle?) {
          super.onCreate(savedInstanceState)
          setContentView(R.layout.help)
          setTitle(R.string.help)
      }
  }
  ```

- [ ] **Step 5：`about/LicensesActivity.kt`**

  ```kotlin
  package org.cog.hymnchtv.about

  import android.app.AlertDialog
  import android.os.Bundle
  import android.widget.ArrayAdapter
  import android.widget.ListView
  import android.widget.ScrollView
  import android.widget.TextView
  import org.cog.hymnchtv.BaseActivity
  import org.cog.hymnchtv.HymnsApp
  import org.cog.hymnchtv.R
  import timber.log.Timber
  import java.io.IOException

  /** Native list of open-source licenses generated by the AboutLibraries Gradle plugin (R.raw.aboutlibraries). */
  class LicensesActivity : BaseActivity() {
      override fun onCreate(savedInstanceState: Bundle?) {
          super.onCreate(savedInstanceState)
          setContentView(R.layout.licenses)
          setTitle(R.string.about_licenses_title)
          val list = findViewById<ListView>(R.id.licenses_list)
          list.emptyView = findViewById(R.id.licenses_empty)
          Thread({
              val rows = loadRows()
              runOnUiThread { if (!isFinishing) bind(list, rows) }
          }, "hymnal-licenses").start()
      }

      private fun bind(list: ListView, rows: List<LicenseRow>) {
          list.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, rows.map(LicenseText::title))
          list.setOnItemClickListener { _, _, position, _ -> showDetail(rows[position]) }
      }

      /** Looked up by name so this screen still builds before the plugin is applied (Task 12). */
      private fun loadRows(): List<LicenseRow> {
          val resId = HymnsApp.getFileResId("aboutlibraries", "raw")
          if (resId == 0) return emptyList()
          return try {
              resources.openRawResource(resId).bufferedReader().use { AboutLibrariesJson.parse(it.readText()) }
          } catch (e: IOException) {
              Timber.w(e, "Cannot read license data")
              emptyList()
          }
      }

      private fun showDetail(row: LicenseRow) {
          val padding = (16 * resources.displayMetrics.density).toInt()
          val text = TextView(this).apply {
              text = LicenseText.detail(row, getString(R.string.about_license_text_missing))
              setTextIsSelectable(true)
              setPadding(padding, padding, padding, padding)
          }
          AlertDialog.Builder(this)
              .setTitle(row.name)
              .setView(ScrollView(this).apply { addView(text) })
              .setPositiveButton(R.string.ok, null)
              .show()
      }
  }
  ```

- [ ] **Step 6：整檔改寫 `About.java`**：保留原檔第 1–16 行的授權標頭，第 17 行之後換成：

  ```java
  package org.cog.hymnchtv;

  import android.content.ActivityNotFoundException;
  import android.content.Context;
  import android.content.Intent;
  import android.net.Uri;
  import android.os.Bundle;
  import android.view.View;
  import android.widget.Button;
  import android.widget.TextView;

  import org.cog.hymnchtv.about.HelpActivity;
  import org.cog.hymnchtv.about.LicensesActivity;
  import org.cog.hymnchtv.logutils.LogUploadServiceImpl;
  import org.cog.hymnchtv.service.androidupdate.UpdateServiceImpl;

  import de.cketti.library.changelog.ChangeLog;
  import timber.log.Timber;

  /**
   * About page (sub-project Z): app name, version, short description, licenses and help. Native views only; no
   * links and no personal data. The original project's Apache-2.0 notice is in the generated licenses list.
   *
   * @author Eng Chong Meng
   */
  public class About extends BaseActivity implements View.OnClickListener, View.OnLongClickListener {
      /**
       * Default CSS styles used to format the change log.
       */
      public static final String DEFAULT_CSS =
              "h1 { margin-left: 0px; font-size: 1.2em; }\n" +
                      "li { margin-left: 0px; font-size: 0.9em;}\n" +
                      "ul { padding-left: 2em; }";
      public static final String bodyTextLight =
              " body { color: white }\n" +
                      " a { color: #80CBC4; text-decoration:none }";

      public static final String bodyTextDark =
              " body { color: black }\n" +
                      " a { color: #00897B; text-decoration:none }";

      public void onCreate(Bundle savedInstanceState) {
          super.onCreate(savedInstanceState);
          setContentView(R.layout.about);
          setTitle(getString(R.string.about));

          findViewById(R.id.about_licenses).setOnClickListener(this);
          findViewById(R.id.about_help).setOnClickListener(this);

          Button btn_HistoryLog = findViewById(R.id.history_log);
          btn_HistoryLog.setOnClickListener(this);
          btn_HistoryLog.setOnLongClickListener(this);

          findViewById(R.id.submit_logs).setOnClickListener(this);
          findViewById(R.id.ok_button).setOnClickListener(this);

          Button btn_chkNewVersion = findViewById(R.id.check_new_version);
          if (HymnsApp.updateServiceAllowed || BuildConfig.DEBUG) {
              btn_chkNewVersion.setVisibility(View.VISIBLE);
              btn_chkNewVersion.setOnClickListener(this);
          }
          else {
              btn_chkNewVersion.setVisibility(View.GONE);
          }

          TextView version = findViewById(R.id.about_appVersion);
          version.setText(getString(R.string.about_version, BuildConfig.VERSION_NAME));
      }

      @Override
      public void onClick(View view) {
          int id = view.getId();
          if (id == R.id.ok_button) {
              finish();
          }
          else if (id == R.id.check_new_version) {
              checkUpdate();
          }
          else if (id == R.id.submit_logs) {
              // No fixed recipient: the user picks where the report goes in the share sheet.
              new LogUploadServiceImpl().sendLogs(new String[0],
                      getString(R.string.send_logs_subject), getString(R.string.send_logs_title));
          }
          else if (id == R.id.history_log) {
              new ChangeLog(this, DEFAULT_CSS).getFullLogDialog().show();
          }
          else if (id == R.id.about_licenses) {
              startActivity(new Intent(this, LicensesActivity.class));
          }
          else if (id == R.id.about_help) {
              startActivity(new Intent(this, HelpActivity.class));
          }
          else {
              finish();
          }
      }

      @Override
      public boolean onLongClick(View view) {
          if (view.getId() == R.id.history_log) {
              checkUpdate();
              return true;
          }
          return false;
      }

      private void checkUpdate() {
          new Thread(() -> UpdateServiceImpl.getInstance().checkForUpdates(), "hymnal-update").start();
      }

      /**
       * Opens a content url (e.g. English lyrics) in a browser. There is no default help url any more.
       */
      public static void hymnUrlAccess(Context context, String url) {
          if (url == null) {
              return;
          }
          try {
              context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
          }
          catch (ActivityNotFoundException e) {
              Timber.w("No activity for %s", url);
          }
      }
  }
  ```

- [ ] **Step 7：建置**

  ```bash
  $G :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug
  grep -nE "WebView|content_about|content_help|copyright|cmeng|gmail|http" hymnchtv/src/main/java/org/cog/hymnchtv/About.java hymnchtv/src/main/res/layout/about.xml
  ```

  Expected: `BUILD SUCCESSFUL`；`grep` 沒有輸出。

- [ ] **Step 8：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/About.java hymnchtv/src/main/java/org/cog/hymnchtv/about \
    hymnchtv/src/main/res/layout/about.xml hymnchtv/src/main/res/layout/help.xml hymnchtv/src/main/res/layout/licenses.xml \
    hymnchtv/src/main/res/values*/strings_about.xml hymnchtv/src/main/res/values*/strings_help.xml
  git commit -m "feat: native About with generated licenses list and in-app help; no links or personal data" \
    -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

### Task 10：README（Lane B）

**Files:** Rewrite `README.md`。**衝突風險：** 無。README 是 repo 文件，不會出現在 App 裡。

- [ ] **Step 1：整檔改寫 `README.md`**

  ````markdown
  # 詩歌 Hymnal

  主的恢復詩歌的歌詞、樂譜瀏覽與播放 App（Android 7.0 以上）。介面支援繁體中文、簡體中文與英文。
  本專案衍生自 [hymnchtv](https://github.com/cmeng-git/hymnchtv)。

  ## 安裝與更新

  - 只在 [GitHub Releases](https://github.com/hitobias/hymnchtv/releases/latest) 發佈，不上架 Google Play。下載 `hymnal-X.Y.Z.apk` 安裝；第一次安裝時，Android 會要求允許「安裝不明應用程式」。
  - 在 App 的「關於」→「更新」可以檢查新版本。下載後，App 會核對 SHA-256、套件名稱、簽章與版本，通過後顯示通知，點選即可安裝。
  - 每個版本都附 `hymnal-X.Y.Z.apk.sha256`，也可以用 `shasum -a 256 hymnal-X.Y.Z.apk` 自行核對。
  - App 識別碼是 `com.ziontkec.hymnal`，可以和原版 hymnchtv 同時安裝，兩者資料互不相通。自行下載的媒體檔放在 `Download/hymnal/`（原版使用 `Download/hymnchtv/`，需要的話請自行複製）。
  - 使用說明在 App 內：主畫面選單 →「使用說明」。

  ## 問題回報

  請到 [Issues](https://github.com/hitobias/hymnchtv/issues) 回報。也可以在「關於」頁按「提報錯誤」，把記錄檔分享給自己後附加到 Issue。

  ## 建置

  - JDK 17、Android SDK（compileSdk 37、build-tools 37.0.0），在 `local.properties` 設定 `sdk.dir`。
  - `./gradlew :hymnchtv:assembleDebug`；單元測試：`./gradlew :hymnchtv:testDebugUnitTest`。
  - 授權清單由 AboutLibraries Gradle plugin 在建置時產生；專案層級的聲明放在 `hymnchtv/aboutlibraries-config/`。
  - 歌詞與樂譜素材有版權，repo 只含樣本，詳見 [documentation/readme.md](documentation/readme.md)。
  - 繁體歌詞由 `tools/gen_lyrics_hant.py` 以 OpenCC 預先產生；App 本身不含 OpenCC。

  ## 發佈（維護者）

  執行 `tools/release.sh X.Y.Z`，規則寫在腳本開頭：tag `vX.Y.Z`，附件為 `hymnal-X.Y.Z.apk` 與 `hymnal-X.Y.Z.apk.sha256`。簽章金鑰放在 repo 之外，由不進版控的 `settings.signing` 指向。中途失敗時用 `--resume` 接續。

  ## 致謝

  - 詩歌歌詞、樂譜等內容的版權屬於台灣福音書房。
  - 原版詩歌本由[書拉密女小站](http://shulami02.net/bbs)開發。
  - 正如神白白的恩典，請不要用於商業用途。

  ## 授權

  本專案以 Apache License 2.0 授權，全文見 [LICENSE](LICENSE)。

      hymnchtv: COG hymns' lyrics viewer and player client

      Copyright 2020 Eng Chong Meng

      Licensed under the Apache License, Version 2.0 (the "License");
      you may not use this file except in compliance with the License.
      You may obtain a copy of the License at

         https://www.apache.org/licenses/LICENSE-2.0

      Unless required by applicable law or agreed to in writing, software
      distributed under the License is distributed on an "AS IS" BASIS,
      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
      See the License for the specific language governing permissions and
      limitations under the License.
  ````

- [ ] **Step 2：Commit**

  ```bash
  git add README.md
  git commit -m "docs: rewrite README for the Hymnal distribution on GitHub Releases" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

### Task 11：發佈工具（Lane C）

**Files:** Create `tools/release_notes.py`、`tools/release.sh`。**衝突風險：** 無。

- [ ] **Step 1：`tools/release_notes.py`**

  ```python
  #!/usr/bin/env python3
  r"""Release helpers for tools/release.sh (sub-project Z).

  Usage:
    release_notes.py notes X.Y.Z path/to/changelog_master.xml   Markdown release notes on stdout
    release_notes.py check-version-code X.Y.Z CODE              exit 0 if CODE follows the convention

  versionCode convention: X*100000 + Y*1000 + Z*10 + n, n = 0..9 (rebuild digit).
  The changelog is a trusted file from this repository.

  >>> expected_code_base("1.0.0")
  100000
  >>> expected_code_base("2.9.2")
  209020
  >>> expected_code_base("2.10.0")
  210000
  >>> code_matches("2.10.0", 210000), code_matches("2.10.0", 210009), code_matches("2.10.0", 210010)
  (True, True, False)
  >>> code_matches("2.10.0", 209999)
  False
  >>> expected_code_base("2.100.0")
  Traceback (most recent call last):
  ...
  ValueError: minor and patch must be <= 99: 2.100.0
  >>> change_to_markdown("修正 <b>錯誤</b>\n     第二行 <a href='x'>連結</a> ")
  '- 修正 錯誤 第二行 連結'
  """
  import re
  import sys
  import xml.etree.ElementTree as ET

  TAG = re.compile(r"<[^>]+>")
  SPACE = re.compile(r"\s+")


  def expected_code_base(version):
      major, minor, patch = (int(part) for part in version.split("."))
      if minor > 99 or patch > 99:
          raise ValueError(f"minor and patch must be <= 99: {version}")
      return major * 100000 + minor * 1000 + patch * 10


  def code_matches(version, code):
      return 0 <= int(code) - expected_code_base(version) <= 9


  def change_to_markdown(text):
      return "- " + SPACE.sub(" ", TAG.sub("", text)).strip()


  def release_notes(version, changelog_path):
      root = ET.parse(changelog_path).getroot()
      for release in root.iter("release"):
          if release.get("version", "").split(" ")[0] == version:
              items = [change_to_markdown("".join(change.itertext())) for change in release.iter("change")]
              if not items:
                  raise LookupError(f"<release> {version} has no <change> entries")
              return "\n".join(items) + "\n"
      raise LookupError(f"no <release> entry for {version} in {changelog_path}")


  def main(argv):
      try:
          if len(argv) == 4 and argv[1] == "notes":
              sys.stdout.write(release_notes(argv[2], argv[3]))
              return 0
          if len(argv) == 4 and argv[1] == "check-version-code":
              return 0 if code_matches(argv[2], argv[3]) else 1
      except (LookupError, ValueError, ET.ParseError) as error:
          sys.stderr.write(f"release_notes.py: {error}\n")
          return 1
      sys.stderr.write(__doc__)
      return 2


  if __name__ == "__main__":
      sys.exit(main(sys.argv))
  ```

  ```bash
  chmod +x tools/release_notes.py
  python3 -m doctest tools/release_notes.py -v | tail -2
  python3 tools/release_notes.py notes 2.9.2 hymnchtv/src/main/res/xml/changelog_master.xml | head -2
  python3 tools/release_notes.py check-version-code 2.9.2 209020; echo "exit=$?"
  python3 tools/release_notes.py notes 9.9.9 hymnchtv/src/main/res/xml/changelog_master.xml; echo "exit=$?"
  ```

  Expected:
  - doctest 最後兩行是 `7 passed.`（舊版 Python 會印 `7 passed and 0 failed.`）與 `Test passed.`。
  - notes 印出 `- ` 開頭的清單。
  - `exit=0`。
  - 最後一個指令印出 `no <release> entry for 9.9.9` 和 `exit=1`。

- [ ] **Step 2：`tools/release.sh`**

  ```bash
  #!/usr/bin/env bash
  # Build, sign and publish a Hymnal release to GitHub Releases (sub-project Z).
  #
  # Release convention (the in-app updater depends on it; keep in sync with update/ReleaseConvention.kt):
  #   tag          vX.Y.Z, no suffix, equal to versionName in hymnchtv/build.gradle
  #   versionCode  X*100000 + Y*1000 + Z*10 + n   (n = 0..9 rebuild digit)
  #   assets       hymnal-X.Y.Z.apk          release-signed, applicationId com.ziontkec.hymnal   (required)
  #                hymnal-X.Y.Z.apk.sha256   `shasum -a 256` output; the app refuses updates without it (required)
  #   notes        the matching <release> entry of changelog_master.xml, plus the SHA-256
  #
  # Publishing never leaves an orphan tag: a DRAFT release is created with --target <commit> (drafts create no tag),
  # all assets are uploaded and checked, then the draft is published, which is when GitHub creates the tag.
  # If anything fails midway, re-run with --resume: it reuses dist/vX.Y.Z and finishes the draft.
  #
  # Signing: settings.signing (git-ignored, repo root) holds two paths OUTSIDE the repo:
  #   keystore=/path/to/hymnal-release.jks
  #   secure_properties=/path/to/hymnal-release.properties   (key.store.password, key.store.alias, key.alias.password)
  # This script never reads or prints the passwords.
  #
  # Usage: tools/release.sh X.Y.Z [--dry-run | --resume]
  set -euo pipefail

  REPO="hitobias/hymnchtv"
  APP_ID="com.ziontkec.hymnal"
  ROOT="$(cd "$(dirname "$0")/.." && pwd)"
  cd "$ROOT"

  die() { echo "ERROR: $*" >&2; exit 1; }

  version="${1:-}"
  mode="${2:-}"
  [[ "$version" =~ ^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$ ]] || die "usage: tools/release.sh X.Y.Z [--dry-run | --resume]"
  [[ -z "$mode" || "$mode" == "--dry-run" || "$mode" == "--resume" ]] || die "unknown option: $mode"
  tag="v$version"
  dist="dist/$tag"
  apk_name="hymnal-$version.apk"
  sha_name="$apk_name.sha256"

  # ---------- remote state ----------
  remote_state() {
    local json
    if ! json=$(gh release view "$tag" --repo "$REPO" --json isDraft,assets 2>/dev/null); then
      echo "none"; return
    fi
    if [[ "$(jq -r '.isDraft' <<<"$json")" == "true" ]]; then echo "draft"; else echo "published"; fi
  }
  remote_tag_exists() { git ls-remote --exit-code --tags origin "refs/tags/$tag" >/dev/null 2>&1; }

  if [[ "$mode" != "--dry-run" ]]; then
    command -v jq >/dev/null || die "jq is required (brew install jq)"
    gh auth status >/dev/null 2>&1 || die "gh is not logged in (run: gh auth login)"
    state=$(remote_state)
    if [[ "$state" == "published" ]]; then die "release $tag is already published"; fi
    if [[ "$state" == "none" ]] && remote_tag_exists; then
      die "tag $tag exists on origin without a release (not created by this script); inspect it before releasing"
    fi
    if [[ "$state" == "draft" && "$mode" != "--resume" ]]; then die "a draft $tag exists: re-run with --resume"; fi
  else
    state="none"
  fi

  # ---------- build (skipped on --resume when artifacts are present) ----------
  build() {
    [[ -f settings.signing ]] || die "settings.signing not found (see the header of tools/release.sh)"
    git diff --quiet && git diff --cached --quiet || die "working tree has uncommitted changes"
    ! git rev-parse -q --verify "refs/tags/$tag" >/dev/null || die "local tag $tag already exists"

    local gradle_file=hymnchtv/build.gradle version_name version_code
    version_name=$(sed -nE 's/^[[:space:]]*versionName[[:space:]]+"([^"]+)".*/\1/p' "$gradle_file")
    version_code=$(sed -nE 's/^[[:space:]]*versionCode[[:space:]]+([0-9]+).*/\1/p' "$gradle_file")
    [[ "$version_name" == "$version" ]] || die "versionName in $gradle_file is '$version_name', expected '$version'"
    python3 tools/release_notes.py check-version-code "$version" "$version_code" \
      || die "versionCode $version_code does not follow X*100000+Y*1000+Z*10+n for $version"

    rm -rf "$dist"
    mkdir -p "$dist"
    git rev-parse HEAD > "$dist/commit"
    python3 tools/release_notes.py notes "$version" hymnchtv/src/main/res/xml/changelog_master.xml > "$dist/notes.md" \
      || die "add a <release version=\"$version (MM/DD/YYYY)\"> entry to changelog_master.xml first"

    ./gradlew --console=plain :hymnchtv:testDebugUnitTest :hymnchtv:assembleRelease

    local apk_src=hymnchtv/build/outputs/apk/release/hymnchtv-release.apk sdk_dir build_tools badging
    [[ -f "$apk_src" ]] || die "$apk_src missing (an unsigned build is named *-release-unsigned.apk: check settings.signing)"
    cp "$apk_src" "$dist/$apk_name"

    sdk_dir=$(sed -nE 's/^sdk\.dir=(.*)$/\1/p' local.properties)
    build_tools="$sdk_dir/build-tools/37.0.0"
    "$build_tools/apksigner" verify --print-certs "$dist/$apk_name" | grep -E "^Signer #1 certificate (DN|SHA-256 digest)"
    badging=$("$build_tools/aapt2" dump badging "$dist/$apk_name" \
      | sed -nE "s/^package: name='([^']+)' versionCode='([0-9]+)' versionName='([^']+)'.*/\1 \2 \3/p")
    [[ "$badging" == "$APP_ID $version_code $version" ]] || die "APK badging mismatch: '$badging'"

    (cd "$dist" && shasum -a 256 "$apk_name" > "$sha_name")
    printf '\nSHA-256 (%s): `%s`\n' "$apk_name" "$(cut -d' ' -f1 "$dist/$sha_name")" >> "$dist/notes.md"
  }

  if [[ "$mode" == "--resume" ]]; then
    [[ -f "$dist/$apk_name" && -f "$dist/$sha_name" && -f "$dist/notes.md" && -f "$dist/commit" ]] \
      || die "--resume needs the artifacts in $dist from the earlier run"
    (cd "$dist" && shasum -a 256 -c "$sha_name") || die "$dist/$apk_name does not match its checksum"
  else
    build
  fi

  ls -l "$dist"
  if [[ "$mode" == "--dry-run" ]]; then
    echo "Dry run complete: $dist (nothing tagged or published)."
    exit 0
  fi

  commit=$(cat "$dist/commit")
  git branch -r --contains "$commit" | grep -q "origin/" || die "commit $commit is not on origin yet: push it first"

  # ---------- publish: draft -> upload -> verify -> publish ----------
  if [[ "$state" == "none" ]]; then
    gh release create "$tag" --repo "$REPO" --draft --target "$commit" --title "詩歌 Hymnal $version" \
      --notes-file "$dist/notes.md" "$dist/$apk_name" "$dist/$sha_name"
  else
    gh release upload "$tag" --repo "$REPO" --clobber "$dist/$apk_name" "$dist/$sha_name"
  fi

  assets=$(gh release view "$tag" --repo "$REPO" --json assets --jq '[.assets[].name] | sort | join(" ")')
  [[ "$assets" == "$apk_name $sha_name" ]] || die "draft $tag has assets '$assets'; fix them and re-run with --resume"

  gh release edit "$tag" --repo "$REPO" --draft=false
  git fetch --tags origin
  [[ "$(git rev-list -n1 "$tag")" == "$commit" ]] || die "published tag $tag does not point at $commit"
  echo "Published $tag at $commit."
  ```

- [ ] **Step 3：語法與失敗路徑**

  ```bash
  chmod +x tools/release.sh
  bash -n tools/release.sh && echo syntax-ok
  tools/release.sh 2.9 ; echo "exit=$?"
  tools/release.sh 2.9.2 --resume ; echo "exit=$?"
  tools/release.sh 2.9.2 --dry-run ; echo "exit=$?"
  ```

  Expected:
  - `syntax-ok`
  - `ERROR: usage: ...`、`exit=1`
  - `--resume` 這一行：如果已經登入 `gh`，會因為沒有草稿而失敗並說明原因（例如缺少 `dist/v2.9.2` 的產物）；如果沒有登入，則印出 `gh is not logged in`。兩種都是 `exit=1`。
  - `ERROR: settings.signing not found ...`、`exit=1`

- [ ] **Step 4：用一次性測試金鑰試跑（`--dry-run`，不發佈）**

  注意：這一步會用 lane C worktree 中的 `hymnchtv/build.gradle`。這時 applicationId 還是 `org.cog.hymnchtv`（Task 12 才會改），所以 badging 檢查**預期會失敗**，訊息是 `APK badging mismatch: 'org.cog.hymnchtv 209020 2.9.2'`。這正好證明這個檢查有效。badging 之前的步驟（簽章、apksigner）都要能正常跑完。完整的試跑在 Task 12 Step 13 重做。

  ```bash
  tmp=$(mktemp -d)
  keytool -genkeypair -keystore "$tmp/dry.jks" -storepass dryrun123 -keypass dryrun123 -alias dry \
    -keyalg RSA -keysize 2048 -validity 1 -dname "CN=Hymnal Dry Run" >/dev/null 2>&1
  printf 'key.store.password=dryrun123\nkey.store.alias=dry\nkey.alias.password=dryrun123\n' > "$tmp/dry.properties"
  printf 'keystore=%s\nsecure_properties=%s\n' "$tmp/dry.jks" "$tmp/dry.properties" > settings.signing
  echo "settings.signing" >> .git/info/exclude; echo "/dist/" >> .git/info/exclude
  git status --porcelain
  tools/release.sh 2.9.2 --dry-run; echo "exit=$?"
  rm -f settings.signing; rm -rf "$tmp" dist
  ```

  Expected:
  - `git status --porcelain` 沒有輸出（暫時用 `.git/info/exclude` 排除；正式的 `.gitignore` 由 Task 12 加入）。
  - 輸出包含 `Signer #1 certificate DN: CN=Hymnal Dry Run`，接著是上述 badging mismatch，`exit=1`。

- [ ] **Step 5：Commit**

  ```bash
  git add tools/release.sh tools/release_notes.py
  git commit -m "chore: add release script with draft-then-publish, remote state checks and --resume" \
    -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

### Task 12：整合（共用檔案）：等 A2、B 合併之後才做

**閘門（不可略過）：**
- 先問使用者：「A2 與 B 是否都已合併進 `feat/zh-hant`？」得到肯定答覆，而且下列指令顯示兩者的合併 commit 之後，才能開始。

  ```bash
  cd /Users/hitobias/orca/hymnchtv/.claude/worktrees/z-main
  git fetch origin && git log --oneline --merges feat/zh-hant | head -20
  ```

- 如果使用者決定先做 Z（A2 或 B 尚未完成），本 task 照樣執行，但要在 PR 說明「A2／B 合併時需要對下列共用檔案做 rebase」，並附上本 task 修改的檔案清單。

**Files:** 見「檔案結構」中的「共用檔案」表，另外新增 `ApplicationIdentityTest.kt`、`IdentityGuardTest.kt`、`IdentityRuntimeTest.kt`。

- [ ] **Step 1：合併 lane，然後 rebase**

  ```bash
  for l in a b c; do
    git merge --no-ff feat/new-identity-lane-$l -m "merge: Z lane $l" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>" || break
  done
  git rebase --rebase-merges feat/zh-hant
  ```

  Expected:
  - lane 之間的合併沒有衝突。
  - rebase 時若出現衝突，只可能在 Z 專屬檔案上（代表 A2 或 B 也動了它們）。處理方式：用 superpowers 的 resolving-merge-conflicts 流程，**保留雙方的意圖**；拿不定主意時停下來問。
  - rebase 完成後跑一次 `G="./gradlew --console=plain -I tools/z-dev.init.gradle"; $G :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug`，要 `BUILD SUCCESSFUL`。

- [ ] **Step 2：先寫守門測試，確認它失敗**

  在 `hymnchtv/build.gradle` 的 `testImplementation 'com.google.truth:truth:1.4.5'` 下一行加入（之後刪除 init script 時要靠它）：

  ```groovy
      // Real org.json for JVM unit tests (android.jar only ships stubs); the app uses the platform copy.
      testImplementation 'org.json:json:20250517'
  ```

  新增 `hymnchtv/src/test/java/org/cog/hymnchtv/identity/IdentityGuardTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.identity

  import com.google.common.truth.Truth.assertThat
  import org.junit.Test
  import java.io.File

  /**
   * Shipped content must not point at the original author's endpoints or contact data, and the UI must not show any
   * web link to this repository (sub-project Z). Scans sources, resources, text assets, Gradle files, AboutLibraries
   * config and the README.
   */
  class IdentityGuardTest {
      private val srcMain = File(checkNotNull(System.getProperty("hymnchtv.assetsDir"))).parentFile
      private val module = srcMain.parentFile.parentFile
      private val repoRoot = module.parentFile

      private val upstream = listOf(
          "cmeng-git.github.io", "raw.githubusercontent.com/cmeng-git", "github.com/cmeng-git",
          "cmeng.gm@", "gmail.com", "play.google.com", "createPackageContext(\"org.cog.hymnchtv\"",
      )
      private val uiLinks = listOf("github.com/hitobias", "#readme")
      private val textAsset = setOf("txt", "json", "xml", "html", "htm", "properties", "csv")

      private fun scan(files: Sequence<File>, forbidden: List<String>): List<String> =
          files.flatMap { file ->
              file.readLines().asSequence().mapIndexedNotNull { index, line ->
                  forbidden.firstOrNull { it in line }?.let { "${file.relativeTo(repoRoot)}:${index + 1}: $it" }
              }
          }.toList()

      private fun tree(dir: File, extensions: Set<String>) =
          dir.walkTopDown().filter { it.isFile && it.extension.lowercase() in extensions }

      @Test
      fun shippedCodeResourcesAndBuildFilesHaveNoUpstreamEndpointsOrContacts() {
          val files = tree(File(srcMain, "java"), setOf("java", "kt")) +
              tree(File(srcMain, "res"), setOf("xml")) +
              sequenceOf(File(srcMain, "AndroidManifest.xml")) +
              tree(File(srcMain, "assets"), textAsset) +
              tree(File(module, "aboutlibraries-config"), setOf("json")) +
              sequenceOf(File(module, "build.gradle"), File(repoRoot, "build.gradle"), File(repoRoot, "settings.gradle"))
          assertThat(scan(files, upstream)).isEmpty()
      }

      @Test
      fun uiResourcesAndAssetsShowNoRepositoryLinks() {
          val files = tree(File(srcMain, "res"), setOf("xml")) + tree(File(srcMain, "assets"), textAsset)
          assertThat(scan(files, uiLinks)).isEmpty()
      }

      @Test
      fun readmeHasNoUpstreamSiteOrStoreLinks() {
          assertThat(scan(sequenceOf(File(repoRoot, "README.md")), listOf("cmeng-git.github.io", "play.google.com", "youtube.com/watch"))).isEmpty()
      }
  }
  ```

  執行（這時仍帶 `-I`，以免依賴還沒生效）：`$G :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.identity.IdentityGuardTest'`
  Expected: `shippedCodeResourcesAndBuildFilesHaveNoUpstreamEndpointsOrContacts` **失敗**，清單中只有尚未清理的共用檔案位置：
  - 三個 `strings.xml` 的 Issues 連結
  - `MainActivity.java:121`
  - `ContentHandler.java:122`
  - `MediaConfig.java:130-133`
  - `changelog_master.xml` 的 9 處

  其餘兩個測試通過。如果清單中出現其他位置，**停下來回報**。

- [ ] **Step 3：根目錄 `build.gradle`**：在 `buildscript { repositories { ... } }` 中加入 `gradlePluginPortal()`，在 `dependencies` 中 AGP 那一行之後加入：

  ```groovy
          // AboutLibraries 15.2.0 (license notices for About): supports AGP 9 / compileSdk 37 per its README version
          // matrix; the Android plugin requires AGP >= 8.13 and Gradle >= 8.8.
          classpath 'com.mikepenz.aboutlibraries.plugin:aboutlibraries-plugin:15.2.0'
  ```

  保留 B 對這個檔案的所有修改（例如 baseline profile plugin、AGP 版本更新）。

- [ ] **Step 4：`hymnchtv/build.gradle`**（逐項處理，每一項都先看 B 的現況）
  1. 第 1 行 `apply plugin: 'com.android.application'` 之後加入 `apply plugin: 'com.mikepenz.aboutlibraries.plugin.android'`。
  2. `applicationId = 'org.cog.hymnchtv'` 改成：

     ```groovy
             // New identity (sub-project Z): the code package stays org.cog.hymnchtv; only the installed app id changes.
             applicationId = 'com.ziontkec.hymnal'
     ```

  3. 刪除 `tasks.register('updateVersionFile', Copy) { ... }` 整段（含上方的註解），以及 `tasks.named("build") { dependsOn "updateVersionFile" }`。然後執行 `git rm hymnchtv/version.properties hymnchtv/release/version.properties`。
  4. 在檔尾加入：

     ```groovy
     // Open-source license notices for the About page (sub-project Z), generated at build time into R.raw.aboutlibraries.
     // Property-style access avoids Groovy's built-in collect() on the configuration closures.
     aboutLibraries.collect.configPath.set(file('aboutlibraries-config'))
     aboutLibraries.collect.fetchRemoteLicense.set(false)
     aboutLibraries.export.excludeFields.addAll('developers', 'funding', 'organization', 'scm', 'website')

     // Hard license gate (sub-project Z): the plugin's strict mode plus verifyLicenseNotices, both driven by
     // aboutlibraries-config/license-allowlist.txt. assembleRelease fails on any library with no license, an
     // unapproved license, or a license without full text. Only the user adds allowlist entries (Task 12 Step 11).
     def licenseAllowlist = file('aboutlibraries-config/license-allowlist.txt')
     def licenseRules = licenseAllowlist.readLines()*.trim().findAll { it && !it.startsWith('#') }
     def allowedEverywhere = licenseRules.findAll { it.startsWith('license ') }.collect { it.substring('license '.length()).trim() }
     aboutLibraries.license.strictMode.set(com.mikepenz.aboutlibraries.plugin.StrictMode.FAIL)
     aboutLibraries.license.allowedLicenses.addAll(allowedEverywhere)
     licenseRules.findAll { it.startsWith('library ') }.each { rule ->
         def (ignored, libraryId, licenseKey) = rule.split(/\s+/, 3) as List
         aboutLibraries.license.allowedLicensesMap.put(licenseKey, (aboutLibraries.license.allowedLicensesMap.getting(licenseKey).getOrElse([]) + [libraryId]))
     }

     tasks.register('verifyLicenseNotices') {
         group = 'verification'
         description = 'Fails unless every bundled library has an approved license with full text (sub-project Z).'
         dependsOn 'prepareLibraryDefinitionsRelease'
         def generated = layout.buildDirectory.file('generated/aboutLibraries/release/res/raw/aboutlibraries.json')
         inputs.file(licenseAllowlist)
         inputs.file(generated)
         doLast {
             def data = new groovy.json.JsonSlurper().parse(generated.get().asFile)
             def problems = []
             data.libraries.each { lib ->
                 def keys = lib.licenses ?: []
                 if (keys.isEmpty()) {
                     problems << "${lib.uniqueId}: no license"
                 }
                 keys.each { key ->
                     boolean approved = allowedEverywhere.contains(key) || licenseRules.contains("library ${lib.uniqueId} ${key}".toString())
                     if (!approved) {
                         problems << "${lib.uniqueId}: license '${key}' is not in aboutlibraries-config/license-allowlist.txt"
                     }
                     if (!data.licenses[key]?.content) {
                         problems << "${lib.uniqueId}: no full text for license '${key}' (add aboutlibraries-config/licenses/<file>.json)"
                     }
                 }
             }
             if (!problems.isEmpty()) {
                 throw new GradleException("License gate failed:\n  " + problems.unique().join("\n  "))
             }
         }
     }
     tasks.matching { it.name == 'assembleRelease' }.configureEach { dependsOn 'verifyLicenseNotices' }
     ```

     `allowedLicensesMap` 的 Groovy 寫法若和 15.2.0 的型別不符（例如 `MapProperty<String, List<String>>` 不接受 `put`），就改用 `allowedLicensesMap.put(licenseKey, [libraryId])`，同一個授權有多個函式庫時合併成一份清單；`verifyLicenseNotices` 不受影響，仍然是最終的硬性閘門。
  5. 在既有的 `tasks.withType(Test).configureEach { ... }` 區塊內加入：

     ```groovy
         // IdentityGuardTest scans shipped sources, assets, Gradle files and the README (sub-project Z).
         inputs.files(fileTree('src/main/java'), fileTree('src/main/res'), fileTree('src/main/assets'),
                 file('src/main/AndroidManifest.xml'), fileTree('aboutlibraries-config'), file('build.gradle'),
                 rootProject.file('build.gradle'), rootProject.file('README.md'))
             .withPathSensitivity(PathSensitivity.RELATIVE)
             .withPropertyName('identityGuardSources')
     ```

  6. `git rm tools/z-dev.init.gradle`。此後所有指令都**不帶** `-I`。

- [ ] **Step 5：`.gitignore` 檔尾加入**

  ```gitignore

  # Release signing (sub-project Z): points at a keystore kept OUTSIDE the repo
  settings.signing
  # tools/release.sh output
  /dist/
  ```

  `.gitignore` 也可能被其他人修改，所以放在這個 task。

- [ ] **Step 6：`AndroidManifest.xml`**：在 `<activity android:name=".About" ... />` 之後加入：

  ```xml
          <activity
              android:name=".about.HelpActivity"
              android:label="@string/help" />

          <activity
              android:name=".about.LicensesActivity"
              android:label="@string/about_licenses_title" />

          <activity
              android:name=".update.UpdateInstallActivity"
              android:excludeFromRecents="true"
              android:exported="false"
              android:theme="@android:style/Theme.Translucent.NoTitleBar" />
  ```

- [ ] **Step 7：`HymnsApp.getFileResId`**：把方法本體改成：

  ```java
      public static int getFileResId(String resName, String defType) {
          Resources res = mInstance.getResources();
          int resId = res.getIdentifier(resName, defType, mInstance.getPackageName());
          if (resId == 0) {
              // applicationId (com.ziontkec.hymnal) differs from the namespace that may own the resource table.
              resId = res.getIdentifier(resName, defType, R.class.getPackage().getName());
          }
          return resId;
      }
  ```

- [ ] **Step 8：三個語系的 `strings.xml`**（只做原地修改和刪除，不在檔尾新增）

  | 名稱 | `values` | `values-zh` | `values-b+zh+Hant` |
  |---|---|---|---|
  | `app_title_main` | `Hymnal` | `诗歌` | `詩歌` |
  | `help_online` | `Help` | `使用说明` | `使用說明` |
  | `send_logs_subject` | `Hymnal bug report` | `诗歌 App 错误报告` | `詩歌 App 錯誤報告` |
  | `send_logs_title` | `Share error report` | `分享错误报告` | `分享錯誤報告` |

  刪除這些元素：`copyright`、`app_libraries`、`version`、`content_about`、`content_help`、`app_new_available`、`app_version_current`、`app_version_new_available`、`app_version_invalid`。刪除前先確認沒有其他使用者：

  ```bash
  grep -rnE "R\.string\.(copyright|app_libraries|version|content_about|content_help|app_new_available|app_version_current|app_version_new_available|app_version_invalid)\b|@string/(copyright|app_libraries|version|content_about|content_help)\"" hymnchtv/src/main
  ```

  Expected: 只出現 `res/layout/content_help.xml`（從未被引用的版面，執行 `git rm hymnchtv/src/main/res/layout/content_help.xml`）。若 A2 或 B 新增了引用，停下來回報。

- [ ] **Step 9：`MainActivity.java`、`ContentHandler.java`、`MediaConfig.java`、版面**
  - `MainActivity.java`：
    - 刪除 `public static String HYMNCHTV_FAQ = "https://cmeng-git.github.io/hymnchtv/faq.html";`。
    - 把 `About.hymnUrlAccess(this, HYMNCHTV_FAQ);`（`online_help` 分支內）改成 `startActivity(new Intent(this, org.cog.hymnchtv.about.HelpActivity.class));`。
  - `ContentHandler.java`：
    - 刪除 `HYMNCHTV_FAQ_PLAYBACK` 那一行。
    - `R.id.help` 分支的 `initWebView(UrlType.onlineHelp);` 改成 `startActivity(new Intent(this, org.cog.hymnchtv.about.HelpActivity.class));`。
    - 刪除 `initWebView` 中的 `case onlineHelp: mWebUrl = HYMNCHTV_FAQ_PLAYBACK; break;`，以及 enum `UrlType` 的 `onlineHelp,`。
    - 先 `grep -n "onlineHelp" hymnchtv/src/main/java -r`，確認沒有其他使用者。
  - `MediaConfig.java`：
    - 刪除 `HYMNCHTV_FAQ_UDC_RECORD` 與上方的註解。
    - `videoUrls` 改成 `private static final ArrayList<String> videoUrls = new ArrayList<>(); // tutorial videos removed (sub-project Z)`。
    - `onClick` 中 `help_text` 分支的三行改成 `startActivity(new Intent(this, org.cog.hymnchtv.about.HelpActivity.class));`。
  - `layout/media_config.xml` 與 `layout-land/media_config.xml`：在 `android:id="@+id/help_video"` 的下一行加入 `android:visibility="gone"`。

- [ ] **Step 10：新身分的版本 1.0.0 與全新的 changelog**（使用者決策：新身分的第一個版本是 1.0.0）

  1. `hymnchtv/build.gradle` 的 `defaultConfig`：把 `versionCode 209020` 改成 `versionCode 100000`，`versionName "2.9.2"` 改成 `versionName "1.0.0"`（依慣例 1.0.0 → 100000）。新的 applicationId 從未發佈過，所以 versionCode 比原專案小也沒有關係。
  2. 用下列內容**整檔取代** `hymnchtv/src/main/res/xml/changelog_master.xml`。原專案的歷史紀錄（含 9 處原作者 FAQ 連結）不再隨新身分發佈；需要時可在原專案的 git 歷史中查到。`MM/DD/YYYY` 用執行當天的日期填入（`date +%m/%d/%Y`）；Task 15 實際建置時若日期不同，再更新一次。

     ```xml
     <?xml version="1.0" encoding="utf-8"?>
     <!-- Change log of the Hymnal distribution (com.ziontkec.hymnal), starting at 1.0.0. Shown by ckChangeLog and
          used by tools/release_notes.py for GitHub release notes. Newest release first. -->
     <changelog>
         <release version="1.0.0 (MM/DD/YYYY)" versioncode="100000">
             <change>新身分「詩歌」第一版（com.ziontkec.hymnal），以 hymnchtv 2.9.2 為基礎，可和原版同時安裝。</change>
             <change>介面新增繁體中文；歌詞可以設定預設顯示簡體或繁體，也可以在歌詞頁臨時切換。</change>
             <change>搜尋時輸入特殊字元不再當機；看歌詞時螢幕保持常亮。</change>
             <change>透過 GitHub Releases 檢查並安裝更新：安裝前會核對 SHA-256、簽章、套件名稱與版本。</change>
             <change>「關於」頁改版：顯示開源授權清單；說明改為 App 內的「使用說明」。</change>
             <change>自行下載的媒體檔改放在 Download/hymnal/。</change>
         </release>
     </changelog>
     ```

     這份 changelog 的文字由使用者確認（PR 說明中列出，請使用者校對）。
  3. 確認：

     ```bash
     grep -c "cmeng-git" hymnchtv/src/main/res/xml/changelog_master.xml
     python3 tools/release_notes.py notes 1.0.0 hymnchtv/src/main/res/xml/changelog_master.xml
     python3 tools/release_notes.py check-version-code 1.0.0 100000; echo "exit=$?"
     ```

     Expected: `0`；印出 6 行 `- ` 開頭的 notes；`exit=0`。

- [ ] **Step 11：AboutLibraries 授權允許清單（閘門是硬性的；清單內容由使用者決定）**

  Task 8 建立的 `hymnchtv/aboutlibraries-config/license-allowlist.txt` 一開始只有 5 個 SPDX 授權，所以第一次 `assembleRelease` **預期會失敗**，並列出所有不符合的函式庫。先產生清單給使用者看：

  ```bash
  ./gradlew --console=plain :hymnchtv:prepareLibraryDefinitionsRelease
  ./gradlew --console=plain :hymnchtv:verifyLicenseNotices || true
  f=hymnchtv/build/generated/aboutLibraries/release/res/raw/aboutlibraries.json
  python3 - "$f" <<'EOF'
  import json, sys, collections
  d = json.load(open(sys.argv[1]))
  use = collections.defaultdict(list)
  for lib in d["libraries"]:
      for key in lib.get("licenses", []) or ["<none>"]:
          use[key].append(lib["uniqueId"])
  for key, libs in sorted(use.items()):
      lic = d["licenses"].get(key, {})
      print(f"{key:40} {lic.get('name','?'):45} text={'content' in lic} libs={len(libs)} e.g. {libs[:2]}")
  print("pinned present:", [x["uniqueId"] for x in d["libraries"] if x["uniqueId"] in ("org.cog:hymnchtv", "org.byvoid:opencc-data")])
  EOF
  ```

  Expected:
  - 產生檔案存在；`pinned present` 列出兩個自訂項目。
  - 下列 SPDX 授權可以直接放進允許清單：`Apache-2.0`、`MIT`、`BSD-2-Clause`、`BSD-3-Clause`、`OFL-1.1`。
  - **其他授權**，包括以雜湊為 key 的「Other」，以及沒有授權（`<none>`）的函式庫：把整份輸出交給使用者決定。可能的處理方式：
    - 用 `allowedLicensesMap` 允許特定函式庫；
    - 或在 `aboutlibraries-config/licenses/`、`libraries/` 中補上正確資料。

  使用者逐項決定之後：
  - 核准的授權寫進 `license-allowlist.txt`，格式為 `license <key>`（全域）或 `library <uniqueId> <key>`（只限某個函式庫）。
  - 缺少全文的授權，在 `aboutlibraries-config/licenses/` 補上 JSON（`{"hash":"<key>","name":…,"url":…,"content":"<全文>"}`）。
  - 不能接受的依賴，交給使用者決定是否移除。

  **代理不得自行新增允許清單項目，也不得放寬閘門。** 然後執行：

  ```bash
  ./gradlew --console=plain :hymnchtv:assembleRelease
  ```

  Expected: `BUILD SUCCESSFUL`。另外做一次反向測試：暫時刪掉允許清單中的 `license Apache-2.0`，`assembleRelease` 應該失敗並列出 Apache 函式庫；確認後還原檔案。

- [ ] **Step 12：身分測試**

  `hymnchtv/src/test/java/org/cog/hymnchtv/identity/ApplicationIdentityTest.kt`

  ```kotlin
  package org.cog.hymnchtv.identity

  import com.google.common.truth.Truth.assertThat
  import org.cog.hymnchtv.BuildConfig
  import org.junit.Test

  class ApplicationIdentityTest {
      @Test
      fun applicationIdIsTheNewIdentity() {
          assertThat(BuildConfig.APPLICATION_ID).isEqualTo("com.ziontkec.hymnal")
      }

      /** versionCode = X*100000 + Y*1000 + Z*10 + n (n = 0..9), so GitHub releases always upgrade cleanly. */
      @Test
      fun versionCodeFollowsVersionName() {
          val version = checkNotNull(org.cog.hymnchtv.update.SemVer.parse(BuildConfig.VERSION_NAME))
          assertThat(version.preRelease).isEmpty()
          val base = version.major * 100000L + version.minor * 1000L + version.patch * 10L
          assertThat(BuildConfig.VERSION_CODE.toLong() - base).isIn(com.google.common.collect.Range.closed(0L, 9L))
      }
  }
  ```

  `hymnchtv/src/androidTest/java/org/cog/hymnchtv/IdentityRuntimeTest.kt`

  ```kotlin
  package org.cog.hymnchtv

  import androidx.test.ext.junit.runners.AndroidJUnit4
  import androidx.test.platform.app.InstrumentationRegistry
  import com.google.common.truth.Truth.assertThat
  import org.cog.hymnchtv.about.AboutLibrariesJson
  import org.junit.Test
  import org.junit.runner.RunWith

  /** Everything derived from the package name works under applicationId com.ziontkec.hymnal (sub-project Z). */
  @RunWith(AndroidJUnit4::class)
  class IdentityRuntimeTest {
      private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

      @Test
      fun packageNameIsTheNewApplicationId() {
          assertThat(context.packageName).isEqualTo("com.ziontkec.hymnal")
      }

      @Test
      fun resourceLookupByNameWorksUnderNewApplicationId() {
          assertThat(HymnsApp.getFileResId("hymnchtv", "drawable")).isEqualTo(R.drawable.hymnchtv)
      }

      @Test
      fun fileProviderAuthorityFollowsApplicationId() {
          assertThat(context.packageManager.resolveContentProvider("com.ziontkec.hymnal.files", 0)).isNotNull()
      }

      @Test
      fun generatedLicensesAreBundledAndStartWithTheOriginalProjectNotice() {
          val resId = HymnsApp.getFileResId("aboutlibraries", "raw")
          assertThat(resId).isNotEqualTo(0)
          val rows = context.resources.openRawResource(resId).bufferedReader().use { AboutLibrariesJson.parse(it.readText()) }
          assertThat(rows.first().id).isEqualTo("org.cog:hymnchtv")
          assertThat(rows.first().description).contains("Copyright 2020 Eng Chong Meng")
      }
  }
  ```

- [ ] **Step 13：完整驗證**

  ```bash
  ./gradlew --console=plain :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug :hymnchtv:assembleRelease :hymnchtv:lintDebug
  for k in MissingTranslation ExtraTranslation HardcodedText; do
    printf "%s: " $k; grep -c "\[$k\]" hymnchtv/build/reports/lint-results-debug.txt
  done
  /opt/homebrew/share/android-commandlinetools/build-tools/37.0.0/aapt2 dump badging hymnchtv/build/outputs/apk/debug/hymnchtv-debug.apk | head -1
  ```

  在 api34 與 api24 上跑 `./gradlew --console=plain :hymnchtv:connectedDebugAndroidTest`（模擬器啟動方式見 Task 13 Step 1）。

  再重做一次發佈腳本的試跑：照 Task 11 Step 4 的指令，但不需要 `.git/info/exclude` 那一行，並把 `tools/release.sh 2.9.2 --dry-run` 換成 `tools/release.sh 1.0.0 --dry-run`；這次 badging 必須通過（`com.ziontkec.hymnal 100000 1.0.0`），`dist/v1.0.0/` 內有 `hymnal-1.0.0.apk` 與 `.sha256`。用完刪除 `settings.signing`（測試金鑰）與 `dist/`。

  Expected:
  - `BUILD SUCCESSFUL`；`IdentityGuardTest` 3 個測試全部通過（Step 2 失敗的那個，現在通過了）。
  - lint 數字不超過 Task 0 基準值。
  - badging 以 `package: name='com.ziontkec.hymnal'` 開頭。
  - instrumented test 全部通過，包含 `IdentityRuntimeTest`、`AssetAndStorageTest`、`SigningCertsTest`、A 的 `ResourceLocaleResolutionTest`。
  - 試跑最後印出 `Dry run complete`。

- [ ] **Step 14：Commit**（按主題分開 commit，每個都帶 Co-Authored-By）

  ```bash
  # Step 4 的 git rm（version.properties ×2、tools/z-dev.init.gradle）已經暫存
  git add build.gradle hymnchtv/build.gradle .gitignore
  git commit -m "build: switch applicationId to com.ziontkec.hymnal and generate license notices with AboutLibraries 15.2.0" \
    -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  git add hymnchtv/src/main/AndroidManifest.xml hymnchtv/src/main/java/org/cog/hymnchtv/HymnsApp.java \
    hymnchtv/src/main/java/org/cog/hymnchtv/MainActivity.java hymnchtv/src/main/java/org/cog/hymnchtv/ContentHandler.java \
    hymnchtv/src/main/java/org/cog/hymnchtv/mediaconfig/MediaConfig.java hymnchtv/src/main/res
  git commit -m "feat: in-app help instead of web links, Hymnal title, register Z activities" \
    -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  git add hymnchtv/src/test hymnchtv/src/androidTest
  git commit -m "test: identity guard and runtime checks for the new application id" \
    -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  git status --porcelain
  ```

  Expected: 最後的 `git status --porcelain` 沒有輸出。

- [ ] **Step 15：移除 lane worktree**：`for l in a b c; do git worktree remove ../z-lane-$l && git branch -d feat/new-identity-lane-$l; done`

---

### Task 13：模擬器驗證

這個 task **不 commit**，也**不修改任何追蹤中的檔案**。測試用的 9.9.9 版本在暫時 worktree 中建置。

**涵蓋範圍：**

| API | 驗證項目 |
|---|---|
| 24 | 首次匯入媒體連結、About／授權／說明、簽章讀取（必須讀得到，否則停下來回報）、成功更新（同時驗證固定的下載路徑） |
| 26 | 首次開啟「安裝不明應用程式」的導引，包括拒絕與允許兩種路徑 |
| 29 | App 在背景時下載完成：只出現通知，不會自動跳出畫面；點通知後才安裝 |
| 34 | 完整失敗矩陣：限流、404、5xx、離線、缺少 `.sha256`、校驗不符、版本不符、`NOT_NEWER`、`WRONG_SIGNER`；成功更新 |
| 35 | 成功更新（固定下載路徑）；拒絕通知權限時改用 toast，並可從「關於」→「更新」安裝 |

如果某個系統映像無法安裝，記錄下來並跳過，在回報中註明。

**Debug 簽章的限制：**
- 成功更新的路徑用的是本機的 debug 金鑰（`~/.android/debug.keystore`，兩個 build 相同），只能證明流程正確，不能證明正式金鑰。正式金鑰在 Task 15 由使用者驗證。
- `WRONG_SIGNER` 用一次性金鑰重新簽章來驗證。

- [ ] **Step 1：安裝映像、建立 AVD、準備假伺服器**

  ```bash
  SDK=/opt/homebrew/share/android-commandlinetools
  yes | sdkmanager "system-images;android-26;google_apis;arm64-v8a" "system-images;android-29;google_apis;arm64-v8a" "system-images;android-35;google_apis;arm64-v8a"
  for a in 26 29 35; do echo no | avdmanager create avd -n api$a -k "system-images;android-$a;google_apis;arm64-v8a"; done
  $SDK/emulator/emulator -list-avds
  boot() { $SDK/emulator/emulator -avd "$1" -no-snapshot-save >/dev/null 2>&1 & adb wait-for-device; adb shell 'while [ "$(getprop sys.boot_completed)" != 1 ]; do sleep 1; done'; }
  export V=$(mktemp -d)/z-verify && mkdir -p "$V" && echo "$V"
  ```

  `$V/mock_github.py`：

  ```python
  #!/usr/bin/env python3
  """Mock of api.github.com releases/latest. Serves files from the current directory.
  /latest.json behaviour comes from ./mode: ok (default) | ratelimit | notfound | error."""
  import http.server
  import pathlib


  class Handler(http.server.SimpleHTTPRequestHandler):
      def do_GET(self):
          if self.path.startswith("/latest.json"):
              mode_file = pathlib.Path("mode")
              mode = mode_file.read_text().strip() if mode_file.exists() else "ok"
              if mode == "ratelimit":
                  return self.reply(403, b'{"message":"API rate limit exceeded"}',
                                    {"X-RateLimit-Remaining": "0", "X-RateLimit-Reset": "1893456000"})
              if mode == "notfound":
                  return self.reply(404, b'{"message":"Not Found"}', {})
              if mode == "error":
                  return self.reply(502, b"bad gateway", {})
          return super().do_GET()

      def reply(self, code, body, headers):
          self.send_response(code)
          self.send_header("Content-Type", "application/json")
          self.send_header("Content-Length", str(len(body)))
          for key, value in headers.items():
              self.send_header(key, value)
          self.end_headers()
          self.wfile.write(body)


  http.server.ThreadingHTTPServer(("127.0.0.1", 8000), Handler).serve_forever()
  ```

  `$V/make_latest.py`：

  ```python
  #!/usr/bin/env python3
  """make_latest.py VERSION APK [--bad-sha] [--no-sha]: writes latest.json (+ the .sha256 asset) for files here."""
  import hashlib
  import json
  import pathlib
  import sys

  version, apk = sys.argv[1], pathlib.Path(sys.argv[2])
  base = "http://10.0.2.2:8000/"
  name = f"hymnal-{version}.apk"
  if apk.name != name:
      pathlib.Path(name).write_bytes(apk.read_bytes())
  digest = hashlib.sha256(pathlib.Path(name).read_bytes()).hexdigest()
  published = "0" * 64 if "--bad-sha" in sys.argv else digest
  pathlib.Path(f"{name}.sha256").write_text(f"{published}  {name}\n")
  assets = [{"name": name, "state": "uploaded", "size": pathlib.Path(name).stat().st_size,
             "digest": None, "browser_download_url": base + name}]
  if "--no-sha" not in sys.argv:
      assets.append({"name": f"{name}.sha256", "state": "uploaded", "size": 84, "digest": None,
                     "browser_download_url": f"{base}{name}.sha256"})
  release = {"html_url": base, "tag_name": f"v{version}", "draft": False, "prerelease": False,
             "body": f"- Mock release {version}\r\n- <b>should appear escaped</b>", "assets": assets}
  pathlib.Path("latest.json").write_text(json.dumps(release, ensure_ascii=False), encoding="utf-8")
  print("latest.json ->", release["tag_name"], name, published[:12], "sha asset:", "--no-sha" not in sys.argv)
  ```

  ```bash
  echo notfound > "$V/mode"
  (cd "$V" && python3 mock_github.py > server.log 2>&1 &)
  sleep 1 && curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8000/latest.json
  printf 'api=http://10.0.2.2:8000/latest.json\nprefix=http://10.0.2.2:8000/\n' > "$V/update_endpoint.properties"
  ```

  Expected: `404`。

- [ ] **Step 2：在暫時 worktree 建置測試用 APK（不修改目前的 checkout）**

  ```bash
  ./gradlew --console=plain :hymnchtv:assembleDebug
  cp hymnchtv/build/outputs/apk/debug/hymnchtv-debug.apk "$V/current-1.0.0.apk"
  build_variant() {  # $1 versionName  $2 versionCode  $3 output
    git worktree add -q "$V/wt" HEAD && cp local.properties "$V/wt/"
    sed -i '' -e "s/versionCode [0-9]*/versionCode $2/" -e "s/versionName \"[^\"]*\"/versionName \"$1\"/" "$V/wt/hymnchtv/build.gradle"
    (cd "$V/wt" && ./gradlew --console=plain -q :hymnchtv:assembleDebug)
    cp "$V/wt/hymnchtv/build/outputs/apk/debug/hymnchtv-debug.apk" "$3"
    git worktree remove --force "$V/wt"
  }
  build_variant 9.9.9 999990 "$V/good-9.9.9.apk"
  build_variant 9.9.9 100000 "$V/notnewer-9.9.9.apk"
  keytool -genkeypair -keystore "$V/other.jks" -storepass other123 -keypass other123 -alias other -keyalg RSA -keysize 2048 -validity 1 -dname "CN=Other" >/dev/null 2>&1
  $SDK/build-tools/37.0.0/apksigner sign --ks "$V/other.jks" --ks-pass pass:other123 --out "$V/othersigner-9.9.9.apk" "$V/good-9.9.9.apk"
  git status --porcelain; git worktree list | grep -c "$V" || true
  ```

  Expected: `git status --porcelain` 沒有輸出；worktree 清單中沒有暫時 worktree（計數為 `0`）。

- [ ] **Step 3：api34：全新安裝、首次匯入、About、授權、說明**

  ```bash
  boot api34
  adb uninstall com.ziontkec.hymnal >/dev/null 2>&1 || true
  adb install -r "$V/current-1.0.0.apk"
  adb shell monkey -p com.ziontkec.hymnal -c android.intent.category.LAUNCHER 1
  sleep 45
  adb shell run-as com.ziontkec.hymnal cat shared_prefs/Settings.xml | grep VersionUrlImport
  adb push "$V/update_endpoint.properties" /data/local/tmp/
  adb shell run-as com.ziontkec.hymnal cp /data/local/tmp/update_endpoint.properties files/
  ```

  Expected 與手動核對（每一步截圖：`adb exec-out screencap -p > "$V/<step>.png"`）：
  - `VersionUrlImport` 的值為 `1`。
  - 主畫面標題是「詩歌」（繁中介面）。
  - 選單「使用說明」開啟 App 內的說明頁，沒有開啟瀏覽器。逐條核對說明文字與實際行為，不符的條列回報。
  - 歌詞頁選單「說明」開啟同一頁；媒體配置的說明按鈕也開啟它；看不到影片說明按鈕。
  - About：只有圖示、詩歌、版本 1.0.0、說明文字、「開源授權」「說明」與原有按鈕。沒有人名、信箱、網址、WebView。
  - 「開源授權」：第一項是 `hymnchtv (original project) 2.9.2 — Apache License 2.0`，點開看到 `Copyright 2020 Eng Chong Meng` 與 Apache 全文；第二項是 OpenCC；列表中沒有可點的網址。
  - 「提報錯誤」：出現分享選單，收件人欄位是空的。

- [ ] **Step 4：api34：檢查結果的各種情況**（每次修改 `$V/mode` 或 `latest.json` 後，在 About 按「更新」）

  | 設定 | 預期 |
  |---|---|
  | `echo notfound > mode` | toast「目前尚無發佈版本。」 |
  | `echo ratelimit > mode` | toast「檢查更新的次數過多，請稍後再試。」 |
  | `echo error > mode` | toast「檢查更新失敗，請稍後再試。」 |
  | 執行 `adb shell svc wifi disable; adb shell svc data disable` | toast「無法連線到更新伺服器…」（測完重新開啟網路） |
  | `echo ok > mode; python3 make_latest.py 9.9.9 good-9.9.9.apk --no-sha` | toast「檢查更新失敗…」（缺少 `.sha256`，視為失敗） |
  | `python3 make_latest.py 9.9.9 current-1.0.0.apk`，然後下載 | 對話框中的 notes 顯示 `<b>should appear escaped</b>` 的字面文字；下載完 toast「…驗證失敗（WRONG_VERSION）…」 |
  | `python3 make_latest.py 9.9.9 good-9.9.9.apk --bad-sha`，然後下載 | toast「…（CHECKSUM_MISMATCH）…」 |
  | `python3 make_latest.py 9.9.9 notnewer-9.9.9.apk`，然後下載 | toast「…（NOT_NEWER）…」 |
  | `python3 make_latest.py 9.9.9 othersigner-9.9.9.apk`，然後下載 | toast「…（WRONG_SIGNER）…」 |

  每一種情況之後執行 `adb logcat -d | grep -c "FATAL EXCEPTION"`，應為 `0`；`adb shell run-as com.ziontkec.hymnal ls files/updates` 應為空。

- [ ] **Step 5：api34：成功更新**

  `(cd "$V" && python3 make_latest.py 9.9.9 good-9.9.9.apk)` → 在 About 按「更新」→「下載」。

  Expected:
  - 下載完成後出現 toast「下載完成，請點選通知完成安裝。」，以及通知「詩歌 9.9.9 已下載，點此安裝。」。
  - 點選通知 → 出現系統安裝畫面 → 安裝。
  - `adb shell dumpsys package com.ziontkec.hymnal | grep -m1 versionName` 顯示 `versionName=9.9.9`。

- [ ] **Step 6：api29：App 在背景時下載完成**
  - 安裝 `current-1.0.0.apk`、放入覆寫檔，設定 `make_latest.py 9.9.9 good-9.9.9.apk`。
  - 按「更新」→「下載」後**立刻按 Home**，等待下載完成。
  - Expected: 只出現通知，App 沒有被帶到前景。`adb shell dumpsys activity activities | grep -c UpdateInstallActivity` 為 `0`。點通知後才出現安裝畫面。

- [ ] **Step 7：api26：「安裝不明應用程式」導引**
  - 全新安裝 `current-1.0.0.apk`，確認詩歌**沒有**安裝不明應用程式的權限。
  - 完成下載，點通知。
  - Expected: 先開啟系統的「安裝不明應用程式」設定頁（詩歌）。
    - 不開啟就返回：toast「需要允許詩歌…」，結束。
    - 再按一次「更新」（已有驗證過的檔案，直接開啟安裝流程），在設定頁開啟權限後返回：出現安裝畫面。

- [ ] **Step 8：api35：拒絕通知權限**
  - 全新安裝後，在權限要求中拒絕通知；或執行 `adb shell pm revoke com.ziontkec.hymnal android.permission.POST_NOTIFICATIONS`。
  - 完成下載。
  - Expected: toast「下載完成。請到『關於』按『更新』安裝。」；在 About 按「更新」後直接進入安裝流程，完成安裝。

- [ ] **Step 9：api24：成功更新與首次匯入**：重做 Step 3 的匯入與 About 檢查，以及 Step 5 的成功更新（這會驗證固定下載路徑與 `GET_SIGNATURES` 的簽章比對）。再用 `othersigner-9.9.9.apk` 驗證：toast 應為「…（WRONG_SIGNER）…」，**不可以**出現「已驗證」的通知。

- [ ] **Step 10：真實的 GitHub API**：安裝 `current-1.0.0.apk`，**不放**覆寫檔。按「更新」。
  Expected: 若 `hitobias/hymnchtv` 還沒有 release，toast「目前尚無發佈版本。」；logcat 的 `Update check:` 和結果一致。

- [ ] **Step 11：整理**

  ```bash
  pkill -f mock_github.py; adb emu kill; rm -rf "$V"; git status --porcelain
  ```

  Expected: 沒有輸出。整理每個 API 的結果表與截圖清單，用文字回報（不寫成檔案）。

---

### Task 14：審查與 PR

- [ ] **Step 1：三方審查**
  - code-reviewer（Opus）審查 `git diff feat/zh-hant...HEAD`。
  - security-reviewer（Opus），重點：
    - 更新信任鏈：`.sha256` 必須存在、`digest` 交叉比對、私有目錄暫存、簽章與 versionCode 檢查
    - exported 的下載完成 receiver
    - `UpdateInstallActivity` 的 extra 驗證（`isApkName`）
    - FileProvider 根目錄
    - debug 覆寫只在 `BuildConfig.DEBUG` 生效
    - 發佈腳本不洩漏密碼、不留孤兒 tag
  - `/codex review`。

  P1 一律修完，每個修正都要重跑測試，並分開 commit。

- [ ] **Step 2：確認 release build 不讀覆寫檔**：`UpdateEndpointsTest.releaseBuildsIgnoreOverrideFile` 通過；而且 `grep -n "BuildConfig.DEBUG" hymnchtv/src/main/java/org/cog/hymnchtv/update/UpdateEndpointsLoader.kt` 顯示讀檔前有檢查。

- [ ] **Step 3：推送並開 PR**

  ```bash
  git push -u origin feat/new-identity
  ```

  用 `gh pr create` 開 PR（base 為 `master`；若 `feat/zh-hant` 尚未合併，則 base 為 `feat/zh-hant`）。內容包含：
  - 設計決定表的摘要。
  - 共用檔案清單，以及當時 A2、B 的合併狀態。
  - `SigningCertsTest` 在 api24／26／34 的結果。
  - Task 12 Step 11 使用者核准的授權允許清單。
  - Task 13 的 API 覆蓋表與截圖清單。
  - Task 15 的步驟。
  - 結尾加上 `🤖 Generated with [Claude Code](https://claude.com/claude-code)`。

---

### Task 15：1.0.0 正式版建置（不發佈）

**前置條件（使用者本人做，代理不做）**：產生正式金鑰並建立 `settings.signing`。只需要做一次；金鑰放在 repo 外並另外備份，遺失後，已安裝的使用者就無法再升級。憑證的 DN 會隨 APK 公開，所以不要放個人資料：

```bash
mkdir -p ~/keys/hymnal && chmod 700 ~/keys/hymnal
keytool -genkeypair -v -keystore ~/keys/hymnal/hymnal-release.jks -alias hymnal -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=Hymnal"
```

建立 `~/keys/hymnal/hymnal-release.properties`（`chmod 600`），內容為 `key.store.password=…`、`key.store.alias=hymnal`、`key.alias.password=…`。然後在 repo 根目錄執行：

```bash
printf 'keystore=%s\nsecure_properties=%s\n' ~/keys/hymnal/hymnal-release.jks ~/keys/hymnal/hymnal-release.properties > settings.signing
```

接著由使用者本人取得憑證的 SHA-256 指紋，**自己**把它加進 `settings.signing`（`keytool` 會互動式詢問密碼，代理不得代填）。這個欄位是 `tools/release.sh` 的簽章釘選：APK 的簽章憑證必須和它完全一致（含 `--resume`），缺少或不符就中止：

```bash
keytool -list -v -keystore ~/keys/hymnal/hymnal-release.jks -alias hymnal | grep 'SHA256:'
printf 'expectedCertSha256=%s\n' '<上一步印出的 64 位十六進位，冒號可留可去>' >> settings.signing
```

代理只能使用 Gradle 既有的 `settings.signing` 機制，**不得讀取、印出或詢問密碼**，也不得把金鑰或 `settings.signing` 加入版控。

- [ ] **Step 1：確認前置條件與狀態**

  ```bash
  test -f settings.signing && echo signing-config-present
  git check-ignore -q settings.signing && echo ignored
  grep -qE '^expectedCertSha256=[0-9A-Fa-f:]{64,95}$' settings.signing && echo cert-pin-present   # 只檢查有沒有，不要印出檔案內容
  git status --porcelain
  grep -nE 'versionCode|versionName' hymnchtv/build.gradle
  ```

  Expected: `signing-config-present`、`ignored`、`cert-pin-present`；工作區（含未追蹤檔案）乾淨；`versionCode 100000`、`versionName "1.0.0"`。缺少 `settings.signing` 時，停下來請使用者完成前置條件。

- [ ] **Step 2：建置並驗證（`--dry-run`，不建立 tag，也不發佈）**

  ```bash
  tools/release.sh 1.0.0 --dry-run
  BT=$(sed -nE 's/^sdk\.dir=(.*)$/\1/p' local.properties)/build-tools/37.0.0
  $BT/apksigner verify --verbose --print-certs dist/v1.0.0/hymnal-1.0.0.apk | grep -E "Verified using|Signer #1 certificate (DN|SHA-256)"
  $BT/aapt2 dump badging dist/v1.0.0/hymnal-1.0.0.apk | head -1
  ls -l dist/v1.0.0/
  du -h dist/v1.0.0/hymnal-1.0.0.apk
  (cd dist/v1.0.0 && shasum -a 256 -c hymnal-1.0.0.apk.sha256)
  cat dist/v1.0.0/notes.md
  ```

  Expected:
  - 腳本最後印出 `Dry run complete: dist/v1.0.0 (nothing tagged or published).`；授權閘門（`verifyLicenseNotices`）與單元測試都通過。
  - `apksigner`：至少一種 `Verified using vN scheme: true`；`Signer #1 certificate DN: CN=Hymnal`（**不是** `Android Debug`）。
  - badging：`package: name='com.ziontkec.hymnal' versionCode='100000' versionName='1.0.0'`。
  - `ls` 列出 `commit`、`hymnal-1.0.0.apk`、`hymnal-1.0.0.apk.sha256`、`notes.md`。
  - `du` 顯示 APK 大小。把它和 Task 0 時 debug 版的大小一起回報；若明顯超過 150 MB，提醒使用者（GitHub 單一附件上限是 2 GB，所以不會卡住發佈，但會影響使用者的下載時間）。
  - `shasum -c` 印出 `hymnal-1.0.0.apk: OK`。

- [ ] **Step 3：實機或模擬器安裝試用（建議）**

  ```bash
  adb uninstall com.ziontkec.hymnal >/dev/null 2>&1 || true
  adb install dist/v1.0.0/hymnal-1.0.0.apk
  ```

  Expected: 安裝成功。主畫面標題是「詩歌」；「關於」顯示「版本 1.0.0」；按「更新」顯示「目前尚無發佈版本」（因為還沒有發佈）。

- [ ] **Step 4：停下來，請使用者核准**

  把以下資訊回報給使用者，並**明確詢問是否發佈 1.0.0 到 GitHub Releases**。沒有得到「發佈」的明確同意前，不執行 Task 16：
  - 上述各項輸出
  - 憑證的 SHA-256
  - `notes.md` 全文

  `dist/` 已被 `.gitignore` 排除，不需要 commit。

---

### Task 16：發佈 1.0.0（只有在使用者明確核准後才執行）

- [ ] **Step 1：發佈 Task 15 驗證過的那一份檔案**

  ```bash
  git status --porcelain
  test "$(git rev-parse HEAD)" = "$(cat dist/v1.0.0/commit)" && echo same-commit
  tools/release.sh 1.0.0 --resume
  ```

  - `--resume` 不會重新建置。它會：
    - 用 `shasum -c` 確認 `dist/v1.0.0/` 的 APK 沒有變動；
    - 檢查遠端狀態與 commit 是否已推送；
    - 以這份 APK 和 `.sha256` 建立草稿（或補齊既有草稿）；
    - 確認附件齊全後再公開。
  - 若沒有印出 `same-commit`，代表 Task 15 之後又有新的 commit：重做 Task 15 Step 2，再請使用者核准一次。
  - 中途失敗時，再執行一次 `tools/release.sh 1.0.0 --resume` 即可。

  Expected: `same-commit`；最後印出 `Published v1.0.0 at <commit>.`。

- [ ] **Step 2：驗證**

  ```bash
  gh release view v1.0.0 --repo hitobias/hymnchtv --json tagName,isDraft,assets --jq '.tagName, .isDraft, (.assets[].name)'
  ```

  Expected: `v1.0.0`、`false`、`hymnal-1.0.0.apk`、`hymnal-1.0.0.apk.sha256`。在手機上安裝 Releases 頁面的 APK，到「關於」→「更新」，應顯示「目前安裝的版本 1.0.0 已是最新」。

  下一次發佈 1.0.1 時，用正式金鑰實際走一次 App 內的更新流程。這是 debug 簽章無法替代的驗證。

---

## 待決問題

1. **授權允許清單**：Task 12 Step 11 會列出所有偵測到的授權。非 SPDX 的「Other」項目，以及缺少授權資料的函式庫，需要使用者逐項決定（允許、補資料，或移除該依賴）。
2. **API 36／37 的模擬器**：目前驗證到 API 35。若要加上 36、37，需要確認 arm64 的 google_apis 映像是否可用。
3. **既有的寬範圍 FileProvider 根目錄**（`external-path "/"`、`root-path "/storage/"`）：分享樂譜、記錄檔、匯出仍在使用。收窄它們不在 Z 的範圍，建議另開項目處理。
