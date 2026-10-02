# 子項目 C：介面現代化 實作計畫

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 hymnchtv 改造成 Material 3 標準的現代 App：底部導覽四頁（首頁/目錄/我的詩歌/設定）、詩本紙本版式歌詞頁、聚會速記首頁，並把長按/選單裏的功能改成看得見的操作。

**Architecture:** `MainActivity`（singleTask）改成「`MaterialToolbar` + `FragmentContainerView` + `BottomNavigationView`」主機，四個分頁各一個 Fragment（不引入 navigation component，手動 `FragmentTransaction`）。歌詞頁 `ContentHandler` 維持獨立 Activity。主題從 AppCompat 淺/深改成 `Theme.Material3.DayNight.*`，`ThemeHelper` 改成 facade。**C 不引用任何 D-1 的類別**；C 只提供空槽位/空按鈕，D-1（其 Task I1～I3）再接入。契約見 spec §9（C-1～C-11）。

**Tech Stack:** Java 11（既有畫面）、Kotlin（新畫面）、`com.google.android.material:material:1.12.0` + `androidx.preference:preference-ktx:1.2.1`（新增）、`androidx.appcompat`/`fragment`/`viewpager2`（既有）、minSdk 24 / compileSdk 37。

---

## 修訂紀錄

- rev 1（2026-10-02）：初版。Codex 審查出 16 P1 + 4 P2。
- rev 2（2026-10-02）：依 Codex 全部 P1 修正。關鍵改動：①並行化重排——四個分頁 Fragment 先各自獨立建立（Phase 1，不碰 `MainActivity.java`），`MainActivity` 主機遷移集中到單一 lane（Phase 2）；②`MainActivity.setAppTheme` 改委託 `ThemePrefs`（修 `Theme.valueOf("SYSTEM")` 崩潰）；③`ContentHandler` manifest theme 改 `@style/AppTheme`；④補 `preference-ktx`；⑤設定頁改用 `OnPreferenceChangeListener`；⑥C 對 D-1 只留空槽位、不引用其類別；⑦筆記本列槽位移到 `content_main.xml`（單一宿主）；⑧播放器列保留 `SeekBar`（不換 `Slider`）；⑨smoke test 提前到 Phase 0；⑩edge-to-edge 只套 `MainActivity`；⑪拍號/調號改第 4 行解析。
- rev 3（2026-10-02）：依 Codex 第二、三輪 P1（+4 P2）修正。關鍵改動：①新增 Task 4「綁入 LXGW WenKai 字型」（硬關卡，因 `res/font/` 與 `tools/gen_font_subset.py` 皆不存在；檔名 `lxgw_wenkai_regular` 避免與 family XML 撞資源名）；②`ThemePrefs.apply` 同步 `ThemeHelper` 快取，`ThemePrefs.resync` 於 `onConfigurationChanged` 處理 SYSTEM 模式系統深淺色切換；③edge-to-edge 豁免 `MainActivity` 免於 `HymnsApp.EdgeToEdgeDisable` 重複 insets；④`MainHost` 狀態還原改從 `savedInstanceState` 讀取、`mainHost` 為欄位、`MainActivity` 明確列出要移除的 home 欄位/方法與回呼；⑤smoke test 補翻頁 + 播放；⑥TOC 等價測試改用 checked-in fixtures；⑦H1 補「記住上次詩歌本」；⑧spec §9 改為「C 提供空槽位/按鈕，D-1 接線」。Codex 誤判「nav_toc 重複」不成立（已確認）。
- rev 4（2026-10-02，1.0.0 發佈後的 1.1 規劃）：依定案設計 `docs/superpowers/specs/2026-10-02-home-entry-and-lyrics-jump-design.md`（rev 5，Codex P1=0）新增 **Task H3（首頁改版）、H4（搜尋核心與搜尋頁）、H5a–H5h（歌詞頁跳轉與回到上一首）**，並修訂 **F2**（spec 對帳＋已決定事項）。程式碼對照 C worktree `hymnchtv-c`（`feat/c-modern-ui` 7985e0c0，等同已合併的 master 6f73d776）。**範圍決定（使用者）：1.1.0 = 主執行緒資料庫查詢清除 + D-1a 資料層（PR #11）+ H3 + H4 + F2；D-1 筆記本介面與 H5 都延後，不進 1.1.0**，兩者先後順序待定（H5 要整合 D-1 歌單時再排）。因此：①排程 H3 → H4 → F2 → 1.1.0 發版；H5 整段移到「F2 之後」、標「1.1 之後」，F2 與 D-1 接點**不得依賴 H5**；②`UiFlags.NOTEBOOK_UI_ENABLED` 在 1.1.0 維持 `false`（「＋歌單」「我的詩歌」仍隱藏），H3 的「更多›」記錄頁只有「最近開過」，唱詩紀錄分頁等 D-1；③關卡重編：新增 **G-DB**（外部：主執行緒查詢清除合併）、**G4**（H3+H4 合併）、**G5**（F2 合併，原 G4）、**G6**（H5 合併，1.1 之後）；④新增「H 系列通則」（基底與 rebase、DB 不得上主執行緒、CRLF 檔清單、三語系字串、`FontSubsetTest`、模擬器、applicationId）；⑤實地盤點修正 spec 與程式碼的出入（寫進各 Task 的「對照程式碼」）：搜尋現況是 `SearchPattern`＋`T2sMap`（不是 OpenCC）、歌詞頁頂列按鈕在每個 `ContentView` 頁內（不是 Activity 單一頂列）、`notebook.model.HymnNumbering` 已在 master（H3 重用，不再抄第三份區間表）。

## 給執行者（Sonnet 5.5）的說明

這份計畫由 **Sonnet 5.5（`claude-sonnet-5-5`）** 子代理執行，可在各自 worktree 平行。主對話（協調者）只開 worktree、派工、合併、跑模擬器與最後審查。

**開工前（協調者）：**
1. 確認 spec §7 的待確認項目（品牌色、DayNight、搜尋入口、字級、exit、長按替代搜尋）已由使用者定案；寫進本檔 rev 3。
2. 本檔 rev 2 再送 Codex 審查，處理完所有 P1 才開始 Task 0。
3. 前置：A2、B、D-1a 已合併（D-1 UI 的 G0）。本計畫的 `HymnTitleSource`（C-8）若 D-1 先合併，改用其 `notebook/ui/titles/` 版本（見 Task H2）。

**執行規則：**
- 每條 lane 只改自己「檔案範圍」的檔案；要改範圍外的檔案就停下來回報。
- **`MainActivity.java` 只有 Phase 2（Lane 0）能改。** Phase 1 的四個 Fragment lane 一律不碰 `MainActivity.java`。（1.1 的 H3／H4／F2 另依下方「H 系列通則」：只做最小的導覽委派改動，且保持 CRLF。）
- 資料層（`persistance/`）、媒體（`mediaplayer/`、`mediaconfig/`、`MediaContentHandler`、`MediaDownloadHandler`）**一律不改簽名**。
- **C 不 import 任何 `org.cog.hymnchtv.notebook.*` 類別**（1.1 通則 §8：允許主程式碼 import `notebook.model.HymnNumbering`／`HymnTypes`）。 對 D-1 只留「空容器 id／空按鈕 id／介面（由 D-1 定義）」，實際接線在 D-1 的 Task I1～I3。
- `ContentHandler` 的三個接點 `showHymn`/`onPlaybackCompleted`/`NotebookBarHost`（C-10）不得破壞。
- 模擬器：`api34b`（`emulator-5580`）/`api24b`（`emulator-5582`）（舊文 `api34nb`/`api24nb` 即這兩台，D-1a 更名）；不要碰 `api24`/`api34`。所有 adb/gradle 指令前 `export ANDROID_SERIAL=…`。


### 1.1 H 系列通則（H3、H4、H5、F2 都適用；rev 4 新增）

**範圍與順序（使用者決定）：** 1.1.0 = 主執行緒資料庫查詢清除 + D-1a 資料層（PR #11）+ **H3 → H4 → F2**。**H5（歌詞頁跳轉）與 D-1 UI 都在 1.1.0 之後**，彼此順序待定；F2、D-1 的接點不得依賴 H5（D-1 的「下一首」維持歌詞頁既有 `btn_next`／`scrollNextHymn()`）。`UiFlags.NOTEBOOK_UI_ENABLED` 在 1.1.0 維持 `false`；打開時機見 H3 Step 11。

**1. 基底、rebase 與「主執行緒資料庫查詢清除」。**
- 分支從 `origin/master`（1.0.0＝6f73d776 之後的最新）開，例如 `feat/c-1-1-home`（H3＋H4 共用一個分支與一個 PR，因為 H3 結束時搜尋入口先停用、H4 才啟用，**H3 單獨不可發版**）；F2 另開 `feat/c-1-1-reconcile`；H5 另開（1.1 之後）。
- 另一位代理同時在做 **主執行緒資料庫查詢清除**（Room 統一計畫 §2.4 清單：`MediaConfig`、`ContentHandler`、`MediaContentHandler`、`LyricsEnglishRecord`、`HomeFragment` 的歷史讀寫；並移除 `allowMainThreadQueries()`）。它改的檔案與 H 系列重疊（`HomeFragment.kt`、`ContentHandler.java`、`MediaContentHandler.java`、`MediaConfig.java`）。**關卡 G-DB = 該分支合併進 `master`。**
  - 可以在 G-DB 之前做的：**不碰上述衝突檔**的步驟——新增純檔案與純 JVM 測試，以及不與 G-DB 重疊的小修改（`HistoryRecord.java`、`strings*.xml`、`AssetHymnTitles.kt`）：H3 Step 1–7、H4 Step 1–2。
  - 必須等 G-DB 合併、`git fetch origin && git rebase origin/master` 之後才能動的：任何會改 `HomeFragment.kt`、`ContentHandler.java`、`MediaContentHandler.java`、`MediaConfig.java`、`LyricsEnglishRecord.java`、`DatabaseBackend.java` 的步驟（H3 Step 9 起、F2、H5 全部）。rebase 衝突一律「保留 G-DB 的資料庫呼叫方式，只重放 H 的 UI 變更」。
- **新程式碼不得在主執行緒查資料庫**：歷史／媒體記錄的讀寫一律 `AppExecutors.io("…") { … }`，結果以 `AppExecutors.MAIN.post { if (views != null) … }` 回主執行緒（沿用 `HomeFragment.loadHistory()` 的寫法）；不得新增任何依賴 `allowMainThreadQueries()` 的呼叫。測試碼同樣不得在 `scenario.onActivity { … }`（主執行緒）裏呼叫 `DatabaseBackend`；在 instrumentation 執行緒呼叫即可。G-DB 合併後，G4／G5 要用 `./gradlew -PstrictDbThread :hymnchtv:installDebug` 走完 H3／H4 的流程確認不崩潰。

**2. 行尾（EOL）。** 以 `git ls-files --eol <path>` 查證（C worktree 7985e0c0 實測）：
| 狀態 | 檔案 |
|---|---|
| **CRLF（改動要保持 CRLF）** | `ContentHandler.java`、`ContentSearch.java`、`HymnToc.java`、`MainActivity.java`、`MediaContentHandler.java`、`MyPagerAdapter.java`、`mediaplayer/AudioBgService.java`、`utils/HymnIdx2NoConvert.java`、`utils/HymnNo2IdxConvert.java`、`utils/HymnNoCh2EngXRef.java`、`persistance/FileBackend.java`、`AndroidManifest.xml`、`res/layout/content_lyrics.xml`、`content_main.xml`、`media_select.xml`、`media_config.xml`（含 `layout-land/`）、`media_records_list.xml`、`content_search.xml`、`search_result.xml`、`about.xml`、`http_login_dialog.xml`、`res/values/styles.xml`；`assets/lyrics_*_text/*.txt`（歌詞，`.gitattributes` 標 `-text`，**一律不改**） |
| **LF** | `ContentView.java`、`MediaGuiController.java`、`MediaDownloadHandler.java`、`mediaconfig/MediaConfig.java`、`mediaconfig/MediaRecord.java`、`hymnhistory/HistoryRecord.java`、`utils/HymnNoValidate.java`、`ui/**` 全部 Kotlin、`res/layout/fragment_home.xml`、`activity_main_host.xml`、`media_player_audio_ui.xml`、`res/values*/strings*.xml`、`assets/lyrics_toc/toc_all_eng2ch.txt` |
| **新檔** | 一律 LF；`git add` 後用 `git ls-files --eol -- <新檔>` 確認顯示 `i/lf w/lf` |
- 改 CRLF 檔的做法：**不要用會正規化行尾的編輯方式**（整檔重寫會讓 diff 變成全檔）。用 Python 以 bytes 改，或 `perl -pi -e` 只替換目標行並自行帶 `\r\n`；改完執行 `git diff --stat <file>` 與 `git diff --ignore-cr-at-eol --stat <file>`，**兩者的增刪行數必須相同**，且 `git ls-files --eol <file>` 仍為 `w/crlf`。
- 不改 `ContentHandler.java` 的整體排版；H5 的大改動也以「新增方法／小段替換」為主，改動行數在 PR 說明列出。

**3. 三語系字串。** 每個新字串同步三個檔：`res/values/strings_c.xml`（英文）、`res/values-zh/strings_c.xml`（簡體）、`res/values-b+zh+Hant/strings_c.xml`（繁體；用字照恢復本 recoveryversion.com.tw，詩歌本名沿用既有 `hymn_type_name_*`：「大本詩歌／補充本／新歌頌詠／新詩歌本／青年詩歌／兒童詩歌」）。鍵一律 `c_` 前綴。`TraditionalResourcesTest` 會擋繁體檔出現簡體字、擋缺繁體鍵。

**4. 字型：不重產、不新增缺字。** `FontSubsetTest`（`reading/FontSubsetTest.kt`）要求**所有 `values*/strings*.xml` 的每個字都在 HymnalKai SC/TC 字型子集內，沒有例外清單**。H 系列**不重產字型**：新字串寫好後先跑 `./gradlew :hymnchtv:testDebugUnitTest --tests '*FontSubsetTest'`；若列出缺字，**改用已涵蓋的字重寫字串**（不是改測試、不是重產字型）。

**5. 模擬器與裝置規則。** 專用 AVD：`api34b`＝`emulator-5580`、`api24b`＝`emulator-5582`（舊文稱 `api34nb`/`api24nb`，D-1a 實際更名為 `api34b`/`api24b`；不要碰 `api24`/`api34`）。所有 adb／gradle 指令前 `export ANDROID_SERIAL=emulator-5580`（或 5582），並用 `adb emu avd name` 確認名稱。**API 24 的 heap 只有 48MB**：H3／H4／F2 的 instrumented 測試在兩台各跑一次；**H5（播放與 Activity 重建）要在 `api24b` 跑全套**。App 的 applicationId 是 `com.ziontkec.hymnal`（程式碼 package 仍是 `org.cog.hymnchtv`）；`adb shell pm clear com.ziontkec.hymnal`、`am start -n com.ziontkec.hymnal/org.cog.hymnchtv.MainActivity`。本計畫的 Steps 由 Sonnet 子代理執行時，**只有協調者在關卡（G4／G5／G6）開模擬器**；lane 內只跑 JVM 測試與 `assembleDebug assembleDebugAndroidTest`。

**6. 測試紀律。** 先寫會失敗的測試，再實作；**測試失敗修實作，不改測試**（除非測試本身錯，要在 PR 說明寫明原因）。因為 UI 被刻意改版而失效的舊測試（例如依賴 `btn_next`、`historyListView` 的首頁測試）不算「改測試」，但要在該 Task 的清單逐一列出並更新。每個 Task 結束前：`./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug :hymnchtv:assembleDebugAndroidTest --console=plain` 全綠才 commit。

**7. Commit 格式。** `<type>: <description>`（feat/fix/refactor/docs/test/chore/perf/ci），**不加 scope**；結尾加 `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`。

**8. C 不 import `notebook.*` 的通則在 1.1 的修訂。** D-1a 資料層已在 master。H 系列主程式碼**允許** `import org.cog.hymnchtv.notebook.model.HymnNumbering`／`HymnTypes`（純 Kotlin 常數與驗證，已有 `HymnTypesConsistencyTest`／`HymnNumberingConsistencyTest` 與 `HymnNoValidate` 對照），**仍禁止** import `notebook.data`／`notebook.repo`／`notebook.ui`／`notebook.record`。

---

## 平行化地圖與合併關卡

| 階段 | Lane | Tasks（依序） | 檔案範圍 | 前置 |
|---|---|---|---|---|
| 0 | 0 | Task 0 → 1 → 2 → 3 → 4 | `hymnchtv/build.gradle`、`res/values/theme.xml`、`res/values/c_dimens.xml`、`res/values*/strings_c.xml`、`res/font/`（字型，Task 4）、`ThemeHelper.java`、`BaseActivity.java`、`HymnsApp.java`、`MainActivity.java`（僅 `setAppTheme`/`onCreate` 的 theme 兩處）、`AndroidManifest.xml`（`ContentHandler` theme）、`ui/theme/`（新）、`androidTest/.../ui/SmokeFlowTest.kt`、對應測試 | — |
| 1 | H | H1 → H2 | `ui/home/`（新）、`ui/titles/`（新，C-8）、`res/layout/fragment_home.xml`（新）、對應測試 | **G0** |
| 1 | T | T1 | `ui/toc/`（新）、`res/layout/fragment_toc.xml`（新）、`HymnToc.java`（抽共用，不動簽名） | G0 |
| 1 | S | S1 | `ui/settings/`（新）、`res/xml/c_preferences.xml`（新）、對應測試 | G0 |
| 1 | M | M1 | `ui/myhymns/`（新）、`res/layout/fragment_myhymns.xml`（新） | G0 |
| 2 | 0 | HOST1 → HOST2 | `MainActivity.java`、`res/layout/activity_main_host.xml`（新）、`res/menu/menu_bottom_nav.xml`（新）、`ui/host/MainHost.kt`（新）、`res/menu/menu_main.xml`（刪除） | **G1**：Phase 1 四 lane 合併 |
| 3 | L | L1 → L2 → L3 → L4 | `ContentHandler.java`、`ContentView.java`、`res/layout/content_lyrics.xml`、`res/layout/content_main.xml`、`res/layout/media_player_audio_ui.xml`、`res/menu/menu_content.xml`、`ui/lyrics/`（新）、對應測試 | **G2**：HOST 合併 |
| 4 | 0 | F1（**已完成**，併入 1.0.0） | （歷史列：`menu_content.xml` 已刪、`HymnToc` 已自 manifest 移除，細節見 F2） | G3 |
| 5 | H | **H3 → H4**（同一分支、同一 PR；H3 Step 1–7 與 H4 Step 1–2 不碰 G-DB 的衝突檔〔新增純檔案＋`HistoryRecord.java`／`strings*.xml`／`AssetHymnTitles.kt` 的不重疊小修改〕，可在 G-DB 前先做） | `hymn/`（新，純 Kotlin）、`ui/picker/`（新）、`ui/home/`、`ui/search/`（新）、`search/`（新核心）、`ui/host/MainHost.kt`＋`MainNavigator.kt`、`ui/toc/TocFragment.kt`、`hymnhistory/HistoryRecord.java`、`res/layout/hymn_picker.xml`＋`layout-land/`、`fragment_home.xml`、`activity_main_host.xml`、`fragment_history.xml`、`fragment_search.xml`、`strings_c.xml`×3、`MainActivity.java`（CRLF，只加導覽委派）、`AndroidManifest.xml`（CRLF，H4 移除 `ContentSearch`）、`ContentSearch.java`（刪）、對應測試 | **G-DB**（Step 9 起）與 G3 |
| 6 | 0 | **F2**（spec 對帳＋已決定事項） | `MediaConfig.java`、`ContentHandler.java`（CRLF，只刪死碼）、`HymnNoCh2EngXRef.java`（CRLF，只換 import）、`HymnToc.java`（刪外殼）、`ui/toc/TocConstants.kt`（新）、`res/layout/media_config.xml`＋`layout-land/`（CRLF）、`media_player_audio_ui.xml`、`content_lyrics.xml`（CRLF）、`ui/theme/`、`androidTest/` | **G4** |
| — | — | **1.1.0 發版**：G-DB ＋ D-1a 資料層（PR #11）＋ G4 ＋ G5 全數合併後，產 1.1.0 APK 並全面檢查 | — | G5 |
| 7（**1.1 之後**） | H5 | **H5a → H5b → H5c → H5d → H5e → H5f → H5g → H5h**（歌詞頁跳轉與回到上一首；設計已定案，實作排在 D-1 UI 之後或之前待定，H5 要整合 D-1 歌單時再排） | `ContentHandler.java`（CRLF）、`MediaContentHandler.java`（CRLF）、`MyPagerAdapter.java`（CRLF）、`mediaplayer/AudioBgService.java`（CRLF）、`MediaGuiController.java`、`MediaDownloadHandler.java`、`ContentView.java`、`content_lyrics.xml`／`content_main.xml`（CRLF）、`media_player_audio_ui.xml`、`nav/`（新，純 Kotlin）、`ui/lyrics/jump/`（新）、對應測試 | **G5**（1.1.0 已發）、**G-DB** 已合併 |

**合併關卡：**
- **G0**：Task 0～4 commit，`testDebugUnitTest` 綠，`assembleDebug` 成功，**`api34b` 上 `SmokeFlowTest` 綠（開工前基線）**。
- **G1**：Phase 1 四 lane 合併（各自獨立，無 `MainActivity.java` 衝突），`assembleDebug` 成功。
- **G2**：HOST 合併，App 可啟動、四分頁切換、share intent 仍工作（手動）。
- **G3**：L 合併，`api34b` 上 `SmokeFlowTest` 仍綠（歌詞頁重設計後）。（F1 的長按逐項核對已於 1.0.0 完成。）
- **G-DB（外部關卡，rev 4 新增）**：另一位代理的「主執行緒資料庫查詢清除」（Room 計畫 §2.4 清單＋移除 `allowMainThreadQueries()`）合併進 `master`。H3 Step 9 起、F2、H5 都要等它合併並 `rebase origin/master` 之後才開始；在此之前 H 系列只能做新增純檔案的步驟。
- **G4（rev 4 新增）**：H3＋H4 合併。條件：`testDebugUnitTest` 綠（含 `FontSubsetTest`、`TraditionalResourcesTest`）；`api34b` 與 `api24b` 上 H3／H4 的 instrumented 測試與 `SmokeFlowTest` 全綠；`-PstrictDbThread` 安裝後走完首頁→開啟→最近→更多→搜尋流程不崩潰；320×640、360×720、字級 1.3、淺／深色、橫向的截圖驗收（H3 Step 14）；`ContentSearch` 已移除且 manifest 無殘留。
- **G5（原 G4，rev 4 改號）**：F2 合併；spec 對帳清單寫進 PR 描述；`SmokeFlowTest` 全綠；320dp 截字稽核無新增項目。**G5 後才發 1.1.0。**
- **G6（rev 4 新增，1.1 之後）**：H5 合併；`api24b` 上 H5 全套（播放、Activity 重建、跳轉、返回堆疊）全綠；`api34b` 同。

---

## 檔案結構

（`java/...`、`ui/...` = `hymnchtv/src/main/java/org/cog/hymnchtv/...`；`test/...` = `hymnchtv/src/test/java/org/cog/hymnchtv`；`androidTest/...` = `hymnchtv/src/androidTest/java/org/cog/hymnchtv`；`res/` 在 `hymnchtv/src/main/`。）

**新增（Kotlin）：**

| 檔案 | 職責 | Task |
|---|---|---|
| `ui/theme/ThemePrefs.kt` | 主題偏好（跟隨系統/淺色/深色）單一來源 | 0 |
| `ui/theme/EdgeToEdge.kt` | 僅對 `MainActivity` 套 edge-to-edge | 2 |
| `ui/home/HomeFragment.kt`、`ui/home/HomeViewModel.kt` | 首頁分頁（鍵盤/詩歌本/即時詩名/歷史/＋歌單） | H1 |
| `ui/titles/HymnTitleSource.kt`、`ui/titles/AssetHymnTitles.kt` | 依「類別+編號」取詩名（沿用 `HymnToc.getHymnTitle` 演算法） | H2 |
| `ui/toc/TocFragment.kt`、`ui/toc/TocBuilder.kt` | 目錄分頁；`TocBuilder` 抽 `HymnToc.getHymnToc` 邏輯 | T1 |
| `ui/settings/SettingsFragment.kt` | 設定分頁（`PreferenceFragmentCompat` + change listener） | S1 |
| `ui/myhymns/MyHymnsFragment.kt` | 我的詩歌分頁空槽位（C-1/C-2） | M1 |
| `ui/host/MainHost.kt` | `MainActivity` 分頁主機（含 tab 狀態保存） | HOST1 |
| `ui/lyrics/LyricsMeta.kt` | 拍號/調號解析 + 節數 span | L1/L4 |

**修改（Java）：** `MainActivity.java`（僅 Phase 0 theme 兩處 + Phase 2 主機遷移）、`ContentHandler.java`、`ContentView.java`、`HymnToc.java`（抽共用）、`About.java`（入口）、`ThemeHelper.java`、`BaseActivity.java`、`HymnsApp.java`、`AndroidManifest.xml`。

**刪除/替換：** `res/menu/menu_main.xml`（Phase 2 刪）、`res/layout/main.xml`（Phase 2 被 `fragment_home.xml` 取代）、`res/menu/menu_content.xml`（Phase 3 被歌詞頁頂列取代）。

**1.1 新增（rev 4；`hymn/`、`nav/` = `…/org/cog/hymnchtv/hymn/`、`…/nav/`，純 Kotlin、無 Android 依賴、可 JVM 測試）：**

| 檔案 | 職責 | Task |
|---|---|---|
| `hymn/HymnRef.kt` | `HymnRef(book, storedNo)`：首頁輸入、開啟、歷史顯示與重開的唯一換算點（`isFu`、`displayNo` 由本推導） | H3 |
| `hymn/HymnSource.kt` | 首頁七個來源（大本/補充/新歌/新詩/青年/兒童/英文）與偏好值 | H3 |
| `hymn/HymnNumberRules.kt` | 逐號有效性、`canAppendDigit`、`storedOf`、`alsoValidIn`、`storedNumbers`（以 `notebook.model.HymnNumbering` 為資料） | H3 |
| `hymn/EnglishXRef.kt` | 解析 `toc_all_eng2ch.txt`，英文詩號 → 中文詩候選 | H3 |
| `ui/picker/PickerState.kt`、`PickerReducer.kt` | 首頁輸入狀態與純函式 reducer（按鍵、來源切換、預覽） | H3 |
| `ui/picker/HymnPickerViewModel.kt`、`HymnPickerViews.kt`、`HymnPickerController.kt`、`HymnLabels.kt` | 可重用的 HymnPicker 元件（首頁用；H5 跳轉面板重用） | H3 |
| `ui/home/HistoryFragment.kt`、`RecentChips.kt` | 「更多›」全螢幕最近開過頁、首頁橫向 chip 列 | H3 |
| `ui/host/MainNavigator.kt` | host 導覽介面（`openToc`、`openHistory`、`openSearch`） | H3/H4 |
| `search/HymnSearch.kt`、`search/AssetLyricsSource.kt` | 搜尋核心（`SearchPage`/`SearchResult`/`SearchScope`）、assets 讀取與繁體顯示 | H4 |
| `ui/search/SearchFragment.kt`、`SearchViewModel.kt`、`SearchAdapter.kt` | 全螢幕搜尋頁 | H4 |
| `ui/toc/TocConstants.kt` | 自 `HymnToc` 搬出的目錄常數 | F2 |
| `nav/MediaLinks.kt`、`nav/YbRefs.kt`、`nav/HymnSequence.kt` | 媒體連結／下載檔名／YB 交叉參照／下一首序列的純函式 | H5a |
| `nav/PlaybackSession.kt`、`nav/ReadingPosition.kt`、`nav/ContentNavigationState.kt` | 播放 session、閱讀位置、導覽狀態（不可變，可序列化成字串） | H5b |
| `ui/lyrics/jump/JumpSheetFragment.kt` | 歌詞頁「跳轉」BottomSheet（重用 HymnPicker 與搜尋） | H5g |

**1.1 刪除：** `ui/home/HomeEntry.kt`、`HomeViewModel.kt`、`HomeViews.kt` 與 `HomeEntryTest.kt`（H3）；`ContentSearch.java`、`layout/content_search.xml`、`layout/search_result.xml`（H4）；`HymnToc.java` 的 Activity 外殼與 `layout/hymn_toc*.xml`、`HymnTocExpandableListAdapter.java`（F2，確認無引用後）。

---

## Task 0：加入 Material 3 與 Preference 依賴

**Files:** Modify `hymnchtv/build.gradle`

- [ ] **Step 1: 加兩個依賴**

在 `hymnchtv/build.gradle` 的 `dependencies { … }` 加：

```groovy
implementation 'com.google.android.material:material:1.12.0'
implementation 'androidx.preference:preference-ktx:1.2.1'
```

（`Theme.Material3.DayNight.NoActionBar`、`PreferenceFragmentCompat`、`SeekBarPreference` 都需要。）

- [ ] **Step 2: 改寫主題為 Material 3 DayNight**

`res/values/theme.xml` 整檔替換為：

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources xmlns:android="http://schemas.android.com/apk/res/android">

    <style name="AppTheme" parent="Theme.Material3.DayNight.NoActionBar">
        <item name="colorPrimary">@color/color_primary</item>
        <item name="colorOnPrimary">@color/white</item>
        <item name="colorPrimaryContainer">@color/color_primary_dark</item>
        <item name="colorSecondary">@color/color_accent</item>
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
        <item name="listItemRow">@style/RowList.Light</item>
        <item name="listItemRadio">@style/RadioListItem.Light</item>
    </style>

    <style name="AppTheme.Light" parent="Theme.Material3.Light.NoActionBar">
        <item name="colorPrimary">@color/color_primary</item>
        <item name="colorOnPrimary">@color/white</item>
        <item name="colorSecondary">@color/color_accent</item>
        <item name="listItemRow">@style/RowList.Light</item>
        <item name="listItemRadio">@style/RadioListItem.Light</item>
    </style>

    <style name="AppTheme.Dark" parent="Theme.Material3.Dark.NoActionBar">
        <item name="colorPrimary">@color/color_primary</item>
        <item name="colorOnPrimary">@color/white</item>
        <item name="colorSecondary">@color/color_accent</item>
        <item name="listItemRow">@style/RowList.Dark</item>
        <item name="listItemRadio">@style/RadioListItem.Dark</item>
    </style>
</resources>
```

> 品牌色是否改設計稿色值，見 spec §7 第 1 項；此處沿用 `#09354d`。

- [ ] **Step 3: `ContentHandler` 的 manifest theme 改 `@style/AppTheme`**

`AndroidManifest.xml` 的 `ContentHandler` activity，把：

```xml
android:theme="@style/Theme.AppCompat.Light.NoActionBar"
```

改成：

```xml
android:theme="@style/AppTheme"
```

（否則歌詞頁沿用 AppCompat Light，L1 換 Material 元件會觸發 Material theme 強制檢查而 crash，且無 DayNight。）

- [ ] **Step 4: 建立 `ThemePrefs`（三選一 + DayNight）**

`ui/theme/ThemePrefs.kt`：

```kotlin
package org.cog.hymnchtv.ui.theme

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/** 深色模式偏好。與 spec §7 第 2 項一致。 */
enum class NightMode(val delegate: Int) {
    SYSTEM(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
    LIGHT(AppCompatDelegate.MODE_NIGHT_NO),
    DARK(AppCompatDelegate.MODE_NIGHT_YES);

    companion object {
        fun from(name: String?): NightMode = entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}

object ThemePrefs {
    const val PREF_THEME = "Theme" // 沿用既有 key；舊值 DARK/LIGHT 仍會正確對應

    fun current(context: Context): NightMode =
        NightMode.from(context.getSharedPreferences("Settings", 0).getString(PREF_THEME, null))

    fun apply(context: Context, mode: NightMode) {
        context.getSharedPreferences("Settings", 0).edit().putString(PREF_THEME, mode.name).apply()
        AppCompatDelegate.setDefaultNightMode(mode.delegate)
        // 同步 ThemeHelper 快取（About/WebViewLyrics/LyricsEnglishRecord 用 isAppTheme(DARK) 選 CSS）
        org.cog.hymnchtv.utils.ThemeHelper.syncDark(resolveDark(context, mode))
    }

    fun applyStored(context: Context) = apply(context, current(context))

    /** 系統深淺色在 app 存活期間變更（SYSTEM 模式）時，重新同步 ThemeHelper 快取（rev 3 #2）。 */
    fun resync(context: Context) {
        org.cog.hymnchtv.utils.ThemeHelper.syncDark(resolveDark(context, current(context)))
    }

    /** SYSTEM 依目前 uiMode 解析成實際亮暗。 */
    private fun resolveDark(context: Context, mode: NightMode): Boolean = when (mode) {
        NightMode.DARK -> true
        NightMode.LIGHT -> false
        NightMode.SYSTEM ->
            (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
    }
}
```

- [ ] **Step 5: 寫失敗測試**

`test/.../ui/theme/ThemePrefsTest.kt`：

```kotlin
package org.cog.hymnchtv.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemePrefsTest {
    @Test fun unknownNameFallsBackToSystem() {
        assertEquals(NightMode.SYSTEM, NightMode.from(null))
        assertEquals(NightMode.SYSTEM, NightMode.from("BOGUS"))
    }

    @Test fun legacyValuesStillMap() {
        assertEquals(NightMode.LIGHT, NightMode.from("LIGHT"))
        assertEquals(NightMode.DARK, NightMode.from("DARK"))
        assertEquals(NightMode.SYSTEM, NightMode.from("SYSTEM"))
    }
}
```

- [ ] **Step 6: 跑測試**

Run: `./gradlew :hymnchtv:testDebugUnitTest --tests "org.cog.hymnchtv.ui.theme.ThemePrefsTest"`
Expected: PASS。

- [ ] **Step 7: Commit**

```bash
git add hymnchtv/build.gradle hymnchtv/src/main/res/values/theme.xml \
        hymnchtv/src/main/AndroidManifest.xml \
        hymnchtv/src/main/java/org/cog/hymnchtv/ui/theme/ \
        hymnchtv/src/test/java/org/cog/hymnchtv/ui/theme/
git commit -m "feat(c): Material 3 DayNight theme + preference dep"
```

---

## Task 1：`ThemeHelper` facade + `MainActivity.setAppTheme` 委託 + DayNight

**Files:** Modify `utils/ThemeHelper.java`、`BaseActivity.java`、`HymnsApp.java`、`MainActivity.java`（僅 theme 兩處）

- [ ] **Step 1: `ThemeHelper` 保留簽名，改成不呼叫 `ctx.setTheme`**

`utils/ThemeHelper.java`：保留 `enum Theme { LIGHT, DARK }` 與 `getAppTheme`/`setAppTheme`/`isAppTheme`（`About.getAboutInfo()` 仍依 `isAppTheme(DARK)` 選 CSS）。`setTheme(Context)` 改 no-op：

```java
public static void setTheme(Context ctx) {
    // DayNight is applied globally via AppCompatDelegate in HymnsApp; keep signature for compat.
}
```

`getAppThemeResourceId()` 維持回傳 LIGHT/DARK 對應 resource（僅 `About`/其它可能呼叫處相容；實際上不再由 Activity 套用）。

再加一個給 `ThemePrefs` 回寫快取的靜態方法（修 rev 2 的深色 CSS 失配）：

```java
/** Called by ThemePrefs after the DayNight mode is resolved. */
public static void syncDark(boolean dark) {
    mTheme = dark ? Theme.DARK : Theme.LIGHT;
}
```

- [ ] **Step 2: `BaseActivity` 移除 `setTheme` 呼叫**

`BaseActivity.onCreate` 從：

```java
ThemeHelper.setTheme(this);
super.onCreate(savedInstanceState);
```

改成：

```java
super.onCreate(savedInstanceState);
```

保留 `attachBaseContext` 的 `LocaleStore.wrap`。

- [ ] **Step 3: `HymnsApp.onCreate` 套用 DayNight + 動態取色**

在 `HymnsApp.onCreate()` 的 `DatabaseBackend.getInstance(this);` 之後加：

```java
org.cog.hymnchtv.ui.theme.ThemePrefs.applyStored(this);
com.google.android.material.color.DynamicColors.applyToActivitiesIfAvailable(this);
```

> 動態取色是 spec §3.1 的明確要求（非可選）。Android 12+ 動態色會覆蓋 `colorPrimary` 等；品牌 fallback 只在無動態色裝置生效。若需強制品牌色，改為不呼叫 `DynamicColors`（spec §7 待確認第 1 項）。

`HymnsApp` 既有的 `onConfigurationChanged`（現設定 `isPortrait`）追加同步（rev 3 #2）：

```java
@Override
public void onConfigurationChanged(@NonNull Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    isPortrait = (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT);
    // SYSTEM 模式：系統深淺色切換時，重新同步 ThemeHelper 快取（供 About/WebViewLyrics/LyricsEnglishRecord 選 CSS）
    org.cog.hymnchtv.ui.theme.ThemePrefs.resync(this);
}
```

- [ ] **Step 4: `MainActivity` 修 `Theme.valueOf("SYSTEM")` 崩潰**

`MainActivity.onCreate` 刪除這兩行（`HymnsApp` 已全域套用 DayNight）：

```java
String theme = mSharedPref.getString(PREF_THEME, Theme.DARK.toString());
setAppTheme(theme, false);
```

`setAppTheme(String, boolean)` 改為委託 `ThemePrefs`（避免舊選單 `themeDark/themeLight` 觸發 `Theme.valueOf("SYSTEM")` 崩潰，Phase 2 才移除選單）：

```java
private void setAppTheme(String sTheme, boolean prefChange) {
    if (prefChange) {
        org.cog.hymnchtv.ui.theme.ThemePrefs.apply(this,
                org.cog.hymnchtv.ui.theme.NightMode.from(sTheme));
        recreate();
    }
}
```

- [ ] **Step 5: 編譯 + 手動驗證主題切換**

Run: `./gradlew :hymnchtv:assembleDebug`；`api34nb` 啟動，切換淺色/深色不崩潰、`About` 正常。

- [ ] **Step 6: Commit**

```bash
git add hymnchtv/src/main/java/org/cog/hymnchtv/utils/ThemeHelper.java \
        hymnchtv/src/main/java/org/cog/hymnchtv/BaseActivity.java \
        hymnchtv/src/main/java/org/cog/hymnchtv/HymnsApp.java \
        hymnchtv/src/main/java/org/cog/hymnchtv/MainActivity.java
git commit -m "feat(c): ThemeHelper facade + DayNight + fix setAppTheme SYSTEM crash"
```

---

## Task 2：edge-to-edge 只套 `MainActivity`

**Files:** Create `ui/theme/EdgeToEdge.kt`；Modify `MainActivity.java`（僅加一行）

- [ ] **Step 1: helper（只在明確呼叫的 Activity 套用）**

`ui/theme/EdgeToEdge.kt`：

```kotlin
package org.cog.hymnchtv.ui.theme

import android.app.Activity
import androidx.core.view.WindowCompat

object EdgeToEdge {
    fun enable(activity: Activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
    }
}
```

- [ ] **Step 2: 只對 `MainActivity` 套用**

`MainActivity.onCreate` 在 `super.onCreate(savedInstanceState)` 之後加：

```java
org.cog.hymnchtv.ui.theme.EdgeToEdge.enable(this);
```

**不**改 `BaseActivity`、**不**移除 `HymnsApp.EdgeToEdgeDisable()`（其它 Activity 維持現況，避免 `ContentHandler`/`HymnToc` 系統列重疊）。但要在 `HymnsApp.EdgeToEdgeDisable()` 的 `onActivityPostCreated` 加一個豁免，避免 API 35+ 對 `MainActivity` 重複套 padding（否則與 host 的 `fitsSystemWindows` 雙重 insets）：

```java
// 在 setOnApplyWindowInsetsListener 最前面：
if (activity instanceof org.cog.hymnchtv.MainActivity) return; // edge-to-edge 由 MainActivity 自理
```

insets 由 `activity_main_host.xml` 用 `android:fitsSystemWindows="true"` 處理（HOST1）。

- [ ] **Step 3: 編譯驗證**

Run: `./gradlew :hymnchtv:assembleDebug`
Expected: BUILD SUCCESSFUL。

- [ ] **Step 4: Commit**

```bash
git add hymnchtv/src/main/java/org/cog/hymnchtv/ui/theme/EdgeToEdge.kt \
        hymnchtv/src/main/java/org/cog/hymnchtv/MainActivity.java
git commit -m "feat(c): edge-to-edge scoped to MainActivity"
```

---

## Task 3：開工前 smoke test（基線，先於重構）

**Files:** Create `androidTest/.../ui/SmokeFlowTest.kt`

- [ ] **Step 1: 寫 smoke test（輸入編號 → 開歌詞 → 翻頁 → 播放）**

`androidTest/.../ui/SmokeFlowTest.kt`，對「現有 UI」跑基線：

```kotlin
package org.cog.hymnchtv.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.cog.hymnchtv.R
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SmokeFlowTest {
    @Test fun inputOpensLyricsFlipPageThenPlay() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            onView(withId(R.id.n1)).perform(click())      // 輸入 1
            onView(withId(R.id.bs_db)).perform(click())   // 大本詩歌 → 歌詞頁
            onView(withId(R.id.viewPager)).check(matches(isDisplayed()))
            onView(withId(R.id.viewPager)).perform(swipeLeft())  // 翻下一頁
            onView(withId(R.id.viewPager)).check(matches(isDisplayed()))
            onView(withId(R.id.playback_play)).perform(click())  // 播放（斷言不崩潰）
            scenario.onActivity { a -> assertFalse(a.isFinishing) }
        }
    }
}
```

> 播放器列預設顯示（`PREF_MENU_SHOW` 預設 true），故 `playback_play` 可見。翻頁（`swipeLeft`）與播放（`click`）都真實執行，不允許 fallback 跳過；若未來播放器列預設改成隱藏，測試 setup 先寫 `PREF_MENU_SHOW=true`。

- [ ] **Step 2: 在 `api34nb` 跑基線**

Run: `export ANDROID_SERIAL=emulator-5580 && ./gradlew :hymnchtv:connectedDebugAndroidTest --tests "org.cog.hymnchtv.ui.SmokeFlowTest"`
Expected: PASS（這是後續所有重構的回歸保護）。

- [ ] **Step 3: Commit**

```bash
git add hymnchtv/src/androidTest/java/org/cog/hymnchtv/ui/SmokeFlowTest.kt
git commit -m "test(c): smoke flow baseline before UI refactor"
```

> **G0 關卡**：Task 0～4 commit，`testDebugUnitTest` 綠、`assembleDebug` 成功、`SmokeFlowTest` 綠。

---

## Task 4：綁入 LXGW WenKai 字型（硬關卡）

**Files:** Create `res/font/lxgw_wenkai.ttf`（或子集）、`res/font/lxgw_wenkai.xml`；Modify `res/values*/strings_c.xml`

> 前置事實（rev 3 核對）：目前 `res/font/` 不存在、`tools/gen_font_subset.py` 與 `tools/font_subset_manifest.txt` 也都不存在（那些是 D-1 UI 計畫的產物，尚未建立）。所以 C **自己綁入**文楷字型，不假設外部管線。

- [ ] **Step 1: 下載 LXGW WenKai（OFL 1.1）**

從 <https://github.com/lxgw/LxgwWenKai/releases> 下載 `LXGWWenKai-Regular.ttf`（OFL 1.1，可商用，授權義務見 About 頁既有說明），放到 `hymnchtv/src/main/res/font/lxgw_wenkai_regular.ttf`。**檔名必須是 `lxgw_wenkai_regular`**（rev 3 #1：若檔名也叫 `lxgw_wenkai`，會與 family XML 產生同名 `@font/lxgw_wenkai` 資源，aapt2 報 duplicate resource）。檔案較大（約 20MB）時，用 `pyftsubset` 或後續 D-1 的子集管線縮小；**本 Task 先以完整字型通過，子集是後續最佳化**。

- [ ] **Step 2: 字型 family XML**

`res/font/lxgw_wenkai.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<font-family xmlns:android="http://schemas.android.com/apk/res/android">
    <font android:fontStyle="normal" android:fontWeight="400" android:font="@font/lxgw_wenkai_regular" />
</font-family>
```

- [ ] **Step 3: 硬關卡驗證**

Run: `ls -la hymnchtv/src/main/res/font/lxgw_wenkai_regular.ttf && ./gradlew :hymnchtv:assembleDebug`
Expected: 字型檔存在、build 成功。**檔不存在即停**（L1 依賴此字型，不允許 serif 過渡）。

- [ ] **Step 4: Commit**

```bash
git add hymnchtv/src/main/res/font/
git commit -m "feat(c): bundle LXGW WenKai font (OFL 1.1)"
```

---

## Task H1：首頁分頁（`HomeFragment` 完整實作，獨立於 `MainActivity`）

**Files:** Create `ui/home/HomeFragment.kt`、`ui/home/HomeViewModel.kt`、`res/layout/fragment_home.xml`

> 本 lane 只新增檔案，**不碰 `MainActivity.java`**。`HomeFragment` 自足：呼叫 `MainActivity.showContent(...)`（static）開歌詞；`MainActivity` 舊鍵盤仍存在，Phase 2 才移除。

- [ ] **Step 1: 完整首頁 layout（id 與舊 `main.xml` 一致）**

`res/layout/fragment_home.xml`：結構等同舊 `main.xml`（`tv_entry`、`historyListView`、`tv_search`、`btn_search`、數字鍵 `n0`…`n11`、6 詩歌本 `bs_db/bs_bb/bs_er/bs_xb/bs_xg/bs_yb`、`btn_english`），差異：
- 根用 `NestedScrollView` + `LinearLayout`，頂部留 `android:fitsSystemWindows="true"` 或 padding 供 insets。
- 按鈕改 `com.google.android.material.button.MaterialButton`（`minHeight="48dp"`）。
- 6 個詩歌本**放大**（方向 C）：`minHeight="64dp"`、`textSize="26sp"`、2×3 格線。
- 加 `@id/title_preview`（`TextView`，`tv_entry` 下）顯示即時詩名（方向 C）。
- 加 `@id/btn_add_playlist`（`MaterialButton`，文字「＋歌單」）與 `@id/btn_next`（Phase 2 才接線，此處只宣告 id，click no-op）。

- [ ] **Step 2: `HomeViewModel` 持有狀態**

`ui/home/HomeViewModel.kt`：

```kotlin
package org.cog.hymnchtv.ui.home

import androidx.lifecycle.ViewModel
import org.cog.hymnchtv.MainActivity.HYMN_DB

class HomeViewModel : ViewModel() {
    var hymnType: String = HYMN_DB
    var number: String = ""
    var isFu: Boolean = false
    var isToc: Boolean = false
    var autoClear: Boolean = false
}
```

- [ ] **Step 3: `HomeFragment` 實作鍵盤/詩歌本/歷史/即時詩名**

`HomeFragment` 移植 `MainActivity` 的 `onNumberClick`/`onHymnButtonClicked`/`showHymnFromEng`/`initHistoryList`/`setFontSize`/`setFontColor`/`setWallpaper` 邏輯，改成操作自身 view。`showContent` 用 `MainActivity.showContent(requireContext(), ...)`（static，不變）。歷史列保留滑動刪除 + 長按刪除 + **加一個可見刪除按鈕**（row 尾 `ImageButton`）。**選詩歌本時把 `hymnType` 存入 `PREF_LAST_HYMN_TYPE`（SharedPreferences），`onCreate` 讀回並高亮對應按鈕（記住上次詩歌本，方向 C）。**

- [ ] **Step 4: 即時詩名（方向 C）**

`tv_entry` 加 `TextWatcher`：輸入改變呼叫 `HymnTitleSource.lookup(hymnType, number)` 填 `title_preview`；空/無效清空。`HymnTitleSource` 於 H2 定義，此步先用介面。

- [ ] **Step 5: 手動驗證（獨立預覽）+ Commit**

以一個臨時 host 或 `@Preview` 驗證；正式接線在 HOST。commit：

```bash
git add hymnchtv/src/main/java/org/cog/hymnchtv/ui/home/ \
        hymnchtv/src/main/res/layout/fragment_home.xml
git commit -m "feat(c): home fragment (keyboard, hymn books, live title, history)"
```

---

## Task H2：`HymnTitleSource`（C-8，沿用既有標題演算法）

**Files:** Create `ui/titles/HymnTitleSource.kt`、`ui/titles/AssetHymnTitles.kt`；Modify `ui/home/HomeFragment.kt`

- [ ] **Step 1: 介面與實作（沿用 `HymnToc.getHymnTitle` 演算法）**

`ui/titles/HymnTitleSource.kt`：

```kotlin
package org.cog.hymnchtv.ui.titles

interface HymnTitleSource {
    fun lookup(hymnType: String, hymnNo: Int): String?
}
```

`ui/titles/AssetHymnTitles.kt`：把 `HymnToc.getHymnTitle(int, String)` 的演算法抽成靜態共用（**保留三處細節**，Codex rev 2 #11）：
1. 讀 `lyrics_*_text/<type><no>.txt` 第 2 行（標題），去掉類別前綴（最後一個「－」之後）。
2. 讀第 3 行，若含「（」則把「（…）」補進標題（如「（英1）」）。
3. **YB 對照**：`HYMN_YB` 用 `MainActivity.ybXTable` 對應到實際檔案（同 `MyPagerAdapter.getHymnFragment`）。
**C 與 D-1 各自擁有各自的 `HymnTitleSource`**：C 的 `ui/titles/HymnTitleSource`（同步 `lookup`）與 D-1 的 `notebook/ui/domain/HymnTitleSource`（`suspend titlesFor`）是**不同 package、不同簽名**，**不共用、不衝突、不互相刪除**。若 D-1 已先合併，仍建 C 自己的（本 task 不變）。

> 已知限制（P2）：標題目前從簡體檔 `lyrics_*_text/*.txt` 讀出，即時詩名顯示簡體；繁中標題屬後續可選（可套 `HantVariant`，同 `ContentView.loadTraditional`），本版不做。

- [ ] **Step 2: 單元測試**

`test/.../ui/titles/AssetHymnTitlesTest.kt`：`db`+`1` 回傳含「祂的計畫」的標題；不存在編號回傳 null；`yb` 對照詩歌回傳對應標題。

- [ ] **Step 3: `HomeFragment` 用介面注入**

`HomeFragment` 的即時查詢改透過 `HymnTitleSource`（供測試替換）。

- [ ] **Step 4: Commit**

```bash
git add hymnchtv/src/main/java/org/cog/hymnchtv/ui/titles/ \
        hymnchtv/src/test/java/org/cog/hymnchtv/ui/titles/ \
        hymnchtv/src/main/java/org/cog/hymnchtv/ui/home/HomeFragment.kt
git commit -m "feat(c): HymnTitleSource reusing HymnToc title algorithm (C-8)"
```

---

## Task T1：目錄分頁（`TocFragment` + `TocBuilder`）

**Files:** Create `ui/toc/TocFragment.kt`、`ui/toc/TocBuilder.kt`、`res/layout/fragment_toc.xml`；Modify `HymnToc.java`（改呼叫 `TocBuilder`，不動簽名）

- [ ] **Step 1: 抽取 `TocBuilder`（定義邊界 + 等價測試）**

`ui/toc/TocBuilder.kt`：把 `HymnToc.getHymnToc`/`getHymnTocType`/`getHymnTitle` 的**純產生邏輯**抽出，簽名：

```kotlin
object TocBuilder {
    /** @return 有序 map：類別 → 該類別下依序的「`%04d: 標題`」字串。input 完全與 HymnToc 相同。 */
    fun build(context: Context, hymnType: String, tocPage: String): LinkedHashMap<String, List<String>>
}
```

保留：六個詩歌本的編號跳號（`rangeBbLimit`/`rangeErLimit`）、YB 的 `mTocYB` 特例、`TOC_ENGLISH` 不排序、`Collections.sort`（stroke/pinyin）、第 3 行「（…）」補資訊。

- [ ] **Step 2: 等價測試（用 checked-in fixtures，rev 3）**

`HymnToc` 的 `getHymnToc`/`getHymnTocType` 是 **private instance method**，無法在測試直接呼叫比對。改用 **checked-in expected fixtures**：抽 `TocBuilder` 前，先對幾個 `(hymnType, tocPage)` 組合用現有 `HymnToc` 產出並存入 `test/resources/toc_fixtures/{db,bb,yb,er}-{CATEGORY,PINYIN}.txt`（每行 `類別<TAB>標題`）。抽完後 `TocBuilderTest` 讀 fixtures 斷言 `TocBuilder.build(...)` 與 fixture 逐行相等。覆蓋至少：`db`+`CATEGORY`、`db`+`PINYIN`、`bb`+`CATEGORY`、`yb`+`CATEGORY`、`er`+`CATEGORY`。測資 asset 目錄用 unit test 已設的 `hymnchtv.assetsDir`。

- [ ] **Step 3: `HymnToc` 改呼叫 `TocBuilder`（過渡期兩者並存）**

`HymnToc.initHymnTocAdapter` 的 `tocListDetail = getHymnToc(...)` 改 `tocListDetail = TocBuilder.build(this, hymnType, tocPage)`（行為不變，`HymnToc` Activity 仍可用）。

- [ ] **Step 4: `TocFragment`**

`ui/toc/TocFragment.kt` + `res/layout/fragment_toc.xml`：頂部 `MaterialButtonToggleGroup`（詩歌本）×2 行或 chips、`TabLayout`（目錄/類別/筆畫/拼音/英中）、`ExpandableListView`。子項點擊 → `MainActivity.showContent(requireContext(), hymnType, hymnNo, false)`。

- [ ] **Step 5: 手動驗證 + Commit**

手動：切詩歌本、切 TOC 類型、點子項開歌詞。

```bash
git add hymnchtv/src/main/java/org/cog/hymnchtv/ui/toc/ \
        hymnchtv/src/main/res/layout/fragment_toc.xml \
        hymnchtv/src/test/java/org/cog/hymnchtv/ui/toc/ \
        hymnchtv/src/main/java/org/cog/hymnchtv/HymnToc.java
git commit -m "feat(c): toc tab + TocBuilder extraction with parity tests"
```

---

## Task S1：設定分頁（`SettingsFragment` + `PreferenceFragmentCompat` + change listener）

**Files:** Create `ui/settings/SettingsFragment.kt`、`res/xml/c_preferences.xml`、`res/layout/fragment_settings.xml`、`res/values/array.xml`（補 `c_*` 陣列）、`res/values*/strings_c.xml`

> 本 lane 只新增檔案，**不碰 `MainActivity.java`**。Phase 2 才移除 `menu_main.xml`。

- [ ] **Step 1: 設定 XML（承接 `menu_main` 全部項）**

`res/xml/c_preferences.xml`：分類 外觀/語言/歌詞/桌布/媒體/其他。外觀用 `ListPreference`（key `Theme`，entryValues `SYSTEM/LIGHT/DARK`）；語言用 `ListPreference`（key `Locale`）；字級用 `SeekBarPreference`（key `TextSize`，25～50）；文字色用 `ListPreference`（key `TextColor`）；其餘 `Preference`：`lyricsLanguage`、`bg`（12 桌布 → 子 `ListPreference`）、`sbguser`、`media_config`、`check_update`、`permission_request`、`online_help`、`about`。

- [ ] **Step 2: `SettingsFragment` 用 change listener（不是 click 轉接）**

`ui/settings/SettingsFragment.kt` extends `PreferenceFragmentCompat`。`onCreatePreferences` inflate `c_preferences`，並為每個 key 設 `OnPreferenceChangeListener`：
- `Theme` → `ThemePrefs.apply(context, NightMode.from(newValue))`，`activity.recreate()`。
- `Locale` → `LocaleStore.set(requireContext(), AppLanguage.from(newValue))` + restart。
- `TextSize` → `MainActivity.setFontSize` 邏輯改寫到一個共用 `HomePrefs` 或直接寫 `SharedPreferences`（`PREF_TEXT_SIZE`），`HomeFragment` 讀取套用。
- `TextColor` → 寫 `PREF_TEXT_COLOR`。
- 其餘 `Preference` 的 `onPreferenceClick` → `startActivity`（`ChineseS2TSelection`/`WallPaperUtil`/`MediaConfig`/`About`）、`UpdateServiceImpl.checkForUpdates()`、系統權限頁、FAQ URL。

> `ListPreference`/`SeekBarPreference` 靠 change listener 套用並更新 summary；不複製 `MainActivity.onOptionsItemSelected` 的 view 操作。

- [ ] **Step 3: 陣列資源**

`res/values/array.xml` 補：`c_night_modes`/`c_night_mode_values`（跟隨系統/淺色/深色）、`c_locales`/`c_locale_values`、`c_font_colors`/`c_font_color_values`、`c_wallpapers`/`c_wallpaper_values`。

- [ ] **Step 4: 手動驗證（獨立預覽）+ Commit**

以臨時 host 預覽；正式接線在 HOST。commit：

```bash
git add hymnchtv/src/main/java/org/cog/hymnchtv/ui/settings/ \
        hymnchtv/src/main/res/xml/c_preferences.xml \
        hymnchtv/src/main/res/layout/fragment_settings.xml \
        hymnchtv/src/main/res/values/array.xml \
        hymnchtv/src/main/res/values/strings_c.xml
git commit -m "feat(c): settings tab (PreferenceFragmentCompat + change listeners)"
```

---

## Task M1：我的詩歌分頁（D-1 空槽位，C-1/C-2）

**Files:** Create `ui/myhymns/MyHymnsFragment.kt`、`res/layout/fragment_myhymns.xml`

- [ ] **Step 1: 空槽位 Fragment**

`ui/myhymns/MyHymnsFragment.kt`：`onCreateView` inflate `fragment_myhymns`（一個空 `FrameLayout` + 佔位 `TextView`「我的詩歌（D-1 完成後啟用）」）。**不 import 任何 `notebook.*` 類別**。D-1 合併後，由 D-1 的整合 task 把 `NotebookHomeFragment` 放進此容器（C-1）。

- [ ] **Step 2: 手動驗證 + Commit**

```bash
git add hymnchtv/src/main/java/org/cog/hymnchtv/ui/myhymns/ \
        hymnchtv/src/main/res/layout/fragment_myhymns.xml
git commit -m "feat(c): my-hymns tab as empty D-1 slot (C-1/C-2)"
```

> **G1 關卡**：Phase 1 四 lane 合併，`assembleDebug` 成功。

---

## Task HOST1：`MainHost` + 主機 layout（`MainActivity` 遷移）

**Files:** Create `res/layout/activity_main_host.xml`、`res/menu/menu_bottom_nav.xml`、`ui/host/MainHost.kt`；Modify `MainActivity.java`

> 從本 Task 起 `MainActivity.java` 才由本 lane 單獨修改。四 fragment 已於 Phase 1 建好。

- [ ] **Step 1: 底部導覽選單（含 icons + contentDescription）**

`res/menu/menu_bottom_nav.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<menu xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:id="@+id/nav_home"      android:icon="@drawable/ic_home"      android:title="@string/c_nav_home" />
    <item android:id="@+id/nav_toc"       android:icon="@drawable/ic_toc"       android:title="@string/c_nav_toc" />
    <item android:id="@+id/nav_my_hymns"  android:icon="@drawable/ic_my_hymns"  android:title="@string/c_nav_my_hymns" />
    <item android:id="@+id/nav_settings"  android:icon="@drawable/ic_settings"  android:title="@string/c_nav_settings" />
</menu>
```

（`ic_*` 用 M3 標準圖示 vector drawable；新增於 `res/drawable/`。）

- [ ] **Step 2: 主機 layout（含 Toolbar + insets）**

`res/layout/activity_main_host.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/viewMain"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fitsSystemWindows="true">

    <com.google.android.material.appbar.MaterialToolbar
        android:id="@+id/toolbar"
        android:layout_width="0dp"
        android:layout_height="?attr/actionBarSize"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <androidx.fragment.app.FragmentContainerView
        android:id="@+id/fragment_container"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintTop_toBottomOf="@id/toolbar"
        app:layout_constraintBottom_toTopOf="@id/bottom_nav"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <com.google.android.material.bottomnavigation.BottomNavigationView
        android:id="@+id/bottom_nav"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        app:menu="@menu/menu_bottom_nav"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />
</androidx.constraintlayout.widget.ConstraintLayout>
```

- [ ] **Step 3: `MainHost`（保存 selected tab + 避免重複 add）**

`ui/host/MainHost.kt`：

```kotlin
package org.cog.hymnchtv.ui.host

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.cog.hymnchtv.R
import org.cog.hymnchtv.ui.home.HomeFragment
import org.cog.hymnchtv.ui.myhymns.MyHymnsFragment
import org.cog.hymnchtv.ui.settings.SettingsFragment
import org.cog.hymnchtv.ui.toc.TocFragment

class MainHost(private val activity: FragmentActivity) {
    private val fm = activity.supportFragmentManager
    private val tabs: Map<Int, () -> Fragment> = linkedMapOf(
        R.id.nav_home to { HomeFragment() },
        R.id.nav_toc to { TocFragment() },
        R.id.nav_my_hymns to { MyHymnsFragment() },
        R.id.nav_settings to { SettingsFragment() },
    )
    private var current: Int = R.id.nav_home

    /** @param savedTab 從 `MainActivity.onCreate(savedInstanceState)` 讀出的上次分頁（rev 3：不得讀 `activity.intent`）。 */
    fun attach(nav: BottomNavigationView, savedTab: Int) {
        nav.setOnItemSelectedListener { item ->
            if (tabs.containsKey(item.itemId)) { show(item.itemId); true } else false
        }
        show(if (tabs.containsKey(savedTab)) savedTab else R.id.nav_home)
    }

    fun onSaveState(out: Bundle) { out.putInt(EXTRA_TAB, current) }

    private fun show(tabId: Int) {
        if (tabId == current && fm.findFragmentByTag("tab:$tabId") != null) return
        val tx = fm.beginTransaction()
        fm.findFragmentByTag("tab:$tabId")?.let { tx.show(it) }
            ?: tx.add(R.id.fragment_container, tabs.getValue(tabId)(), "tab:$tabId")
        fm.findFragmentByTag("tab:$current")?.let { tx.hide(it) }
        tx.commit()
        current = tabId
    }

    companion object { const val EXTRA_TAB = "c_selected_tab" }
}
```

- [ ] **Step 4: `MainActivity` 接主機（一次性搬移，避免 crash）**

`MainActivity` 加欄位 `private org.cog.hymnchtv.ui.host.MainHost mainHost;`，`onCreate` 做**同一 commit 內**的完整切換：

```java
setContentView(R.layout.activity_main_host);
setSupportActionBar(findViewById(R.id.toolbar));
mainHost = new MainHost(this);
int savedTab = (savedInstanceState == null) ? R.id.nav_home
        : savedInstanceState.getInt(MainHost.EXTRA_TAB, R.id.nav_home);
mainHost.attach(findViewById(R.id.bottom_nav), savedTab);
handleIntent(getIntent());
getOnBackPressedDispatcher().addCallback(backPressedCallback);
```

`onSaveInstanceState` 改為（**不再存取 `mEntry`**，避免 null）：

```java
@Override protected void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    mainHost.onSaveState(outState);
}
```

**同一 commit 內移除的 home 欄位/方法（這些 id 已不存在，留著會 null crash）：**
- 欄位：`btn_n0`…`btn_n9`、`btn_fu`、`btn_del`、`btn_db/bb/er/xb/xg/yb`、`btn_search`、`btn_update`、`btn_english`、`mEntry`、`tv_Search`、`mHistoryListView`、`mTocSpinner`、`mTocSpinnerItem`、`mHistoryAdapter`、`background`、`sNumber`、`isFu`、`isToc`、`autoClear`、`mFontSize`、`mFontColor`、`mTocPage`、`mFsDelta`。
- 方法：`initButton`、`onNumberClick`、`onHymnButtonClicked`、`showHymnFromEng`、`showHymnToc`、`initHistoryList`、`showHymn`、`setFontSize`、`setFontColor`、`setWallpaper`、`setBgColor`、`initTocSpinnerItem`、`initUserSettings`、`initLanguage`、`onItemSelected`/`onNothingSelected`（`OnItemSelectedListener` 介面一併移除）。
- `onResume` 裏對 `btn_update` 的動畫/可見性（`mHasUpdate` 提示改由設定分頁「檢查更新」呈現，本版先移除該段）。
- `backPressedCallback` 改為：**非首頁分頁 → 回首頁；首頁 → `finish()`**（不再存取 `mHistoryListView`/`mEntry`）。

**保留（其它類別依賴）：** `showContent`（static）、`setHymnTypeNo`（static）、`mHymnType`/`mHymnNo`（static）、`getHymnType`（static）、`ybXTable`/`createYbXTable`（static）、`HYMN_*`/`ATTR_*`/`PREF_*` 常數、`HYMN_YB_ALT`、`handleIntent`/`onNewIntent`/`getFile`、`setAppTheme`（委託版）、`setAppLocale`/`doRestart`、`onRequestPermissionsResult`、`onInfoButtonClicked`、`isDeviceLocked`/`getInstance`。

- [ ] **Step 5: 手動驗證 + Commit**

手動：四分頁切換、旋轉後回到原分頁、share intent、`SmokeFlowTest` 仍綠。

```bash
git add hymnchtv/src/main/res/layout/activity_main_host.xml \
        hymnchtv/src/main/res/menu/menu_bottom_nav.xml \
        hymnchtv/src/main/res/drawable/ic_*.xml \
        hymnchtv/src/main/java/org/cog/hymnchtv/ui/host/MainHost.kt \
        hymnchtv/src/main/java/org/cog/hymnchtv/MainActivity.java
git commit -m "feat(c): MainActivity host with toolbar + bottom nav"
```

---

## Task HOST2：移除 `menu_main.xml` 與舊首頁殘留

**Files:** Modify `MainActivity.java`；Delete `res/menu/menu_main.xml`、`res/layout/main.xml`

- [ ] **Step 1: 移除 `menu_main` inflate 與 `onOptionsItemSelected` 的分支**

`MainActivity` 移除 `onCreateOptionsMenu`/`onCreateContextMenu`/`onContextItemSelected`/`onOptionsItemSelected` 對 `menu_main` 的處理（功能已在 `SettingsFragment`）。`git rm res/menu/menu_main.xml`。

- [ ] **Step 2: 移除舊 `main.xml`**

`git rm res/layout/main.xml`（其 id 已全在 `fragment_home.xml`）。

- [ ] **Step 3: 手動回歸 + Commit**

手動：無選單、所有設定在設定分頁、`SmokeFlowTest` 綠。

```bash
git rm hymnchtv/src/main/res/menu/menu_main.xml hymnchtv/src/main/res/layout/main.xml
git add hymnchtv/src/main/java/org/cog/hymnchtv/MainActivity.java
git commit -m "refactor(c): remove menu_main + old main layout"
```

> **G2 關卡**：四分頁切換 + share intent + `SmokeFlowTest` 綠。

---

## Task L1：歌詞頁版式（詩本紙本，方向 B）+ 文楷字型

**Files:** Modify `res/layout/content_lyrics.xml`、`res/values/c_dimens.xml`、`ContentView.java`；Create `ui/lyrics/LyricsMeta.kt`

- [ ] **Step 1: 文楷字型（已於 Task 4 綁入，直接引用）**

字型 `@font/lxgw_wenkai` 已於 Task 4 綁入（硬關卡）。在 `content_lyrics.xml` 的歌詞 `ZoomTextView` 設：

```xml
android:fontFamily="@font/lxgw_wenkai"
```

**不**用 serif 過渡（文楷是方向 B 的必要元素，spec §3.2）。

- [ ] **Step 2: 宣紙白背景 + 24sp**

`content_lyrics.xml` 的 `lyricsView` 根加 `android:background="@color/c_paper_white"`（新色，宣紙白，實際色值依設計稿）。歌詞 `ZoomTextView` 改 `android:textSize="24sp"`（方向 B 預設）。

- [ ] **Step 3: 拍號/調號（第 4 行）**

`content_lyrics.xml` 在 `contentView`（譜圖）與歌詞文字間加 `@id/meter_key`（`TextView`）。`ContentView.showLyricsChText` 解析歌詞檔**第 4 行**（如「降A大调4/4」）填入；缺值（如 ER 無此行）則 `GONE`。抽 `ui/lyrics/LyricsMeta.kt` 的 `parseMeterKey(lines: List<String>): String?` 純函式。

- [ ] **Step 4: 編譯 + 手動驗證**

手動：歌詞頁白底、文楷（或 serif 過渡）、24sp、拍號調號顯示、譜圖正常。

- [ ] **Step 5: Commit**

```bash
git add hymnchtv/src/main/res/layout/content_lyrics.xml \
        hymnchtv/src/main/res/values/c_dimens.xml \
        hymnchtv/src/main/java/org/cog/hymnchtv/ContentView.java \
        hymnchtv/src/main/java/org/cog/hymnchtv/ui/lyrics/LyricsMeta.kt
git commit -m "feat(c): lyrics paper style + WenKai + meter/key from line 4"
```

---

## Task L2：歌詞頁頂列按鈕（取代 `menu_content` 長按選單）

**Files:** Modify `res/layout/content_lyrics.xml`、`ContentView.java`、`ContentHandler.java`、`res/menu/menu_content.xml`

- [ ] **Step 1: 頂列按鈕列**

`content_lyrics.xml` 頂部加 `@id/lyrics_top_bar`（`LinearLayout`），放：`button_ts`（簡繁）、`button_english`（中英）、`btn_score_color`（譜顏色）、`btn_font_dec`/`btn_font_inc`（字級）、`btn_share`（分享）、`btn_media`（媒體設定）、`btn_more`（overflow：英文刪除/說明/回首頁）。

- [ ] **Step 2: `ContentView`/`ContentHandler` 接按鈕**

把 `ContentHandler.onContextItemSelected` 對應分支邏輯接到按鈕 click。停用 `registerForContextMenu`（`menu_content` 不再長按彈出）。

- [ ] **Step 3: 手動驗證 + Commit**

手動：所有按鈕可用、長按不再彈選單。

```bash
git add hymnchtv/src/main/res/layout/content_lyrics.xml \
        hymnchtv/src/main/java/org/cog/hymnchtv/ContentView.java \
        hymnchtv/src/main/java/org/cog/hymnchtv/ContentHandler.java
git commit -m "feat(c): lyrics top bar replaces context menu"
```

---

## Task L3：歌詞頁「下一首」+ 筆記本列槽位（C-5/C-6，slot 在 `content_main.xml`）

**Files:** Modify `res/layout/content_main.xml`、`ContentHandler.java`、`ui/lyrics/`（介面）

- [ ] **Step 1: 筆記本列槽位放 `content_main.xml`（單一宿主，rev 2 #9）**

`content_main.xml` 在 `ViewPager2` 與 `mediaPlayer` 之間（或標題列）加 `@id/notebookBar`（`FrameLayout`）。**不放 `content_lyrics.xml`**（那是每頁 `ContentView`，會產生多個列）。C 只提供空容器（`visibility="gone"`）；D-1 的 `HymnNotebookBarFragment` 於其整合 task（I1）放進來並把容器設為 `visible`。

- [ ] **Step 2: 「下一首」按鈕（C-6）**

`content_lyrics.xml` 頂列加 `@id/btn_next`。click → 呼叫既有 `scrollNextHymn()`（同本翻下一首）當預設；D-1 合併後由 D-1 接 `HymnBarViewModel.nextInPlaylist()`。**C 不 import notebook 類別**，只留按鈕 id 與 `scrollNextHymn` fallback。

- [ ] **Step 3: 手動驗證 + Commit**

手動：下一首翻頁、`notebookBar` 容器存在（空）。

```bash
git add hymnchtv/src/main/res/layout/content_main.xml \
        hymnchtv/src/main/res/layout/content_lyrics.xml \
        hymnchtv/src/main/java/org/cog/hymnchtv/ContentHandler.java
git commit -m "feat(c): next button + notebook bar slot in content_main (C-5/C-6)"
```

---

## Task L4：紅色節數 span + 播放器列樣式（保留 `SeekBar`）

**Files:** Modify `ui/lyrics/LyricsMeta.kt`、`ContentView.java`、`res/layout/media_player_audio_ui.xml`、`MediaGuiController.java`

- [ ] **Step 1: 紅色節數 span**

`ContentView.showLyricsChText` 讀入歌詞後用 `SpannableString` 把節數行（`^[一二三四五六七八九十\d]+$` 或「第X節」）上色 `@color/c_verse_red` 並加粗。抽 `LyricsMeta.applyVerseSpans(text: CharSequence): CharSequence` 純函式 + 單元測試（span 數量/顏色）。

- [ ] **Step 2: 播放器列 M3 樣式（**保留 `SeekBar`，不換 `Slider`**）**

`media_player_audio_ui.xml` 的控制項只改**顏色/樣式**（`MaterialButton`/`MaterialRadioButton`），**保留 `SeekBar`**（rev 2 #10：`MediaGuiController` 依賴 `SeekBar.OnSeekBarChangeListener` 且 `playback_play` 是 `ImageView`+`AnimationDrawable`）。**不**把 `playback_play` 換 `MaterialButton`（會破壞 `AnimationDrawable` 背景）。僅「連播」開關（長按 play 的替代，spec §4.3 #8）加一個可見 `CheckBox`，接 `confirmAutoStream` 邏輯。

- [ ] **Step 3: 測試 + 手動 + Commit**

`test/.../ui/lyrics/LyricsMetaTest.kt` 驗證 span。手動：節數紅色、播放器列樣式、連播開關。

```bash
git add hymnchtv/src/main/java/org/cog/hymnchtv/ui/lyrics/ \
        hymnchtv/src/test/java/org/cog/hymnchtv/ui/lyrics/ \
        hymnchtv/src/main/java/org/cog/hymnchtv/ContentView.java \
        hymnchtv/src/main/res/layout/media_player_audio_ui.xml \
        hymnchtv/src/main/java/org/cog/hymnchtv/MediaGuiController.java
git commit -m "feat(c): red verse spans + restyled player bar (keep SeekBar)"
```

> **G3 關卡**：`api34nb` 上 `SmokeFlowTest` 仍綠；接點 `showHymn`/`onPlaybackCompleted`/`NotebookBarHost` 完好（C-10）。

---

## Task F1：移除長按/舊選單，搬移次要入口

**Files:** Modify `MainActivity.java`、`About.java`、`MediaConfig.java`、`ContentView.java`、`MediaGuiController.java`；Delete `res/menu/menu_content.xml`；Modify `AndroidManifest.xml`（移除 `HymnToc` 若已遷移）

- [ ] **Step 1: 逐項核對 spec §4.3 的 13 個長按**

#1（主畫面背景選單）已於 HOST2 移除；#2/#3（詩歌本/英中 alt）若保留長按，加 `contentDescription` 提示；#4（歷史刪除）已有可見按鈕（H1）；#5/#6（歌詞設定/英文重載）已由 L2 頂列取代；#7（歌詞頁選單）已停用（L2）；#8（連播）已加開關（L4）；#9–#12（QQ/Notion/BibleTool/Google 替代搜尋）移除；#13（About 長按查更新）改設定分頁「檢查更新」。

- [ ] **Step 2: 次要入口**

`About`/`MediaConfig`/`WallPaperUtil`/`ChineseS2TSelection` 入口已在設定分頁（S1）。移除 `About` 的 `history_log` 長按查更新。刪 `menu_content.xml`。

- [ ] **Step 3: 移除 `HymnToc` Activity（若 T 已完全遷移）**

`AndroidManifest.xml` 移除 `<activity android:name=".HymnToc" />`（僅在確認無其它呼叫者後）。

- [ ] **Step 4: 手動回歸 + Commit**

手動：無長按隱藏功能；所有功能在分頁/按鈕找到。

```bash
git rm hymnchtv/src/main/res/menu/menu_content.xml
git add hymnchtv/src/main/java/org/cog/hymnchtv/MainActivity.java \
        hymnchtv/src/main/java/org/cog/hymnchtv/About.java \
        hymnchtv/src/main/java/org/cog/hymnchtv/ContentView.java \
        hymnchtv/src/main/java/org/cog/hymnchtv/MediaGuiController.java \
        hymnchtv/src/main/AndroidManifest.xml
git commit -m "refactor(c): remove long-press/menu; move entries to settings"
```

---

## Task H3：首頁改版（先選本、再打號、即時預覽、最近與記錄頁）

> 依據：spec `2026-10-02-home-entry-and-lyrics-jump-design.md` §1–§3、§5–§7。**同一分支與 H4 合併**（G4）；H3 結束時搜尋入口先停用。

**對照程式碼（C worktree 7985e0c0 實測）：**
- `ui/home/HomeFragment.kt`（391 行）：鍵盤 `n0–n11`（`n10`＝附、`n11`＝清除）、六個詩歌本 `bs_er/xb/xg/yb/bs_bb/bs_db`（`bs_yb` 長按＝青年附號替代 `MainActivity.HYMN_YB_ALT`）、`btn_english`（短按＝英中對照開 BB 第一筆、長按＝查大本頁；邏輯在檔尾 `object EnglishCrossRef`）、`tv_search`＋`btn_search`（開 `ContentSearch` Activity）、`tv_entry` 點擊展開 280dp `historyListView`、`btn_next`（`HomeEntry.next`）、`btn_add_playlist`（受 `UiFlags.NOTEBOOK_UI_ENABLED` 控制）、`tv_hint` 長提示。
- `ui/home/HomeAppearance.kt`：把使用者字級／文字色／照片背景（`BackgroundDrawables.backdrop`）套到 `HomeViews`；`views.hint`、`keypadArea`、`actionArea`、`entry`、`search` 都吃背板。
- `ui/host/MainHost.kt`：`tabIds`、`attach`、`onBackPressed()`（呼叫 `HomeFragment.onBackPressed()` 關歷史清單）；`MainActivity.java`（CRLF）的 `backPressedCallback` 先問 `mainHost.onBackPressed()`、再 `popBackStack()`。
- `ui/toc/TocFragment.kt`：私有 `hymnType`/`tocPage`、`BOOK_CHIPS`、`PAGES`（`HymnToc.TOC_CATEGORY/STROKE/PINYIN/ENGLISH`）、`load()`；無對外切換介面。
- 歷史：`DatabaseBackend.historyRecords`（最新優先，背景執行緒讀）、`deleteHymnHistory(record)`（只比對 hymnType＋hymnNo）；`MainActivity.showContent()` 以 `MediaRecord.isFu(type,no)` 寫入 `HistoryRecord`（**只有大本附為 `isFu=1`，青年附 276/277 為 0**，spec §2 規定媒體層不動）；`HistoryRecord.toString()` 的 YB 分支用 `hymn_title_mc_yb` 顯示「青年 276: …」。
- `notebook.model.HymnNumbering`（純 Kotlin，master 已有）已含 `isValid(book, storedNo)`、各區間表，並有 `HymnTypesConsistencyTest`（JVM）與 `HymnNumberingConsistencyTest`（instrumented）和 `HymnNoValidate` 對照。`HymnNoValidate` 的靜態初始化需要 `android.util.Range`，**不能在 JVM 測試載入**（只有編譯期常數可用）；所以 H3 的規則類別**建在 `HymnNumbering` 上**，不碰 `HymnNoValidate` 類別。
- `assets/lyrics_toc/toc_all_eng2ch.txt`（LF，905 行）：行格式 `^ 0001: 標題 #db1`；`#db`/`#bb`/`#xg` 三種目標；同一英文號可有 BB、DB 兩行（現況：短按取第一行、長按取第二行）。

**Files：**
- Create（純 Kotlin，`hymn/`）：`HymnRef.kt`、`HymnSource.kt`、`HymnNumberRules.kt`、`EnglishXRef.kt`
- Create（`ui/picker/`）：`PickerState.kt`、`PickerReducer.kt`、`HymnPickerViewModel.kt`、`HymnPickerViews.kt`、`HymnPickerController.kt`、`HymnLabels.kt`
- Create（`ui/home/`）：`HistoryFragment.kt`、`RecentChips.kt`；（`ui/host/`）`MainNavigator.kt`
- Create（res）：`layout/hymn_picker.xml`、`layout-land/hymn_picker.xml`、`layout/fragment_history.xml`、`drawable/ic_chevron_right.xml`、`values*/strings_c.xml`（三檔，LF）、`values/c_dimens.xml`（補 `c_key_height`＝52dp）
- Modify：`ui/home/HomeFragment.kt`（縮成薄殼）、`ui/home/HomeAppearance.kt`、`ui/home/HistoryAdapter.kt`、`ui/home/HomePrefs.kt`、`ui/host/MainHost.kt`、`ui/toc/TocFragment.kt`、`hymnhistory/HistoryRecord.java`（LF）、`res/layout/fragment_home.xml`、`res/layout/activity_main_host.xml`、**`MainActivity.java`（CRLF）**
- Delete：`ui/home/HomeEntry.kt`、`ui/home/HomeViewModel.kt`、`ui/home/HomeViews.kt`、`test/.../ui/home/HomeEntryTest.kt`
- Test（JVM）：`test/.../hymn/{HymnRefTest,HymnNumberRulesTest,HymnNumberRulesAssetTest,EnglishXRefTest}.kt`、`test/.../ui/picker/{PickerReducerTest,PickerChromeTest,HymnPickerViewModelTest}.kt`
- Test（instrumented）：`androidTest/.../ui/picker/{HymnPickerTest,HymnLabelsTest,HomeLayoutTest}.kt`、`ui/home/HistoryFragmentTest.kt`、`ui/host/OverlayNavigationTest.kt`、`ui/toc/TocSelectTest.kt`；更新 `SmokeFlowTest`、`MainHostTest`、`MainScreenContrastTest`、`PhotoBackdropTest`、刪 `HomeFragmentTest`（由 `HymnPickerTest` 取代）

- [ ] **Step 0：工作區與基線**

  ```bash
  git fetch origin && git switch -c feat/c-1-1-home origin/master
  ./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug :hymnchtv:assembleDebugAndroidTest --console=plain   # 基線必須綠
  git ls-files --eol hymnchtv/src/main/java/org/cog/hymnchtv/MainActivity.java   # 預期 w/crlf
  ```

- [ ] **Step 1：`HymnSource`、`HymnNumberRules` 最小 API、`HymnRef`（TDD，純 Kotlin；先建 Step 2 要擴充的基礎，避免 Step 1 無法編譯）**

  先寫 `test/.../hymn/HymnRefTest.kt`（會編譯失敗→實作）：

  ```kotlin
  class HymnRefTest {
      @Test fun dbFuIsStoredAbove780() {
          val ref = HymnRef.fromEntry(HymnTypes.DB, 3, isFu = true)!!
          assertThat(ref).isEqualTo(HymnRef(HymnTypes.DB, 783))
          assertThat(ref.isFu).isTrue(); assertThat(ref.displayNo).isEqualTo(3)
      }
      @Test fun youthFuIsStoredAbove275() {          // 媒體層的 isFu 仍是 0；HymnRef 才負責顯示
          val ref = HymnRef.fromEntry(HymnTypes.YB, 1, isFu = true)!!
          assertThat(ref.storedNo).isEqualTo(276); assertThat(ref.isFu).isTrue(); assertThat(ref.displayNo).isEqualTo(1)
          assertThat(HymnRef(HymnTypes.YB, 275).isFu).isFalse()
      }
      @Test fun noFuInOtherBooksAndOutOfRangeFuIsNull() {
          assertThat(HymnRef.fromEntry(HymnTypes.BB, 1, isFu = true)).isNull()
          assertThat(HymnRef.fromEntry(HymnTypes.DB, 7, isFu = true)).isNull()   // 大本附只有 1–6
          assertThat(HymnRef.fromEntry(HymnTypes.YB, 3, isFu = true)).isNull()   // 青年附只有 1–2
      }
      @Test fun storedAndDisplayRoundTrip() {
          for (book in HymnTypes.ALL) for (no in HymnNumberRules.storedNumbers(book)) {
              val ref = HymnRef(book, no)
              assertThat(HymnRef.fromEntry(book, ref.displayNo, ref.isFu)).isEqualTo(ref)
          }
      }
      @Test fun dummyEnglishOnlyHymnIsNotValid() = assertThat(HymnRef(HymnTypes.BB, 2000).isValid).isFalse()
  }
  ```

  **本 Step 先實作 `HymnNumberRules` 的最小 API**（`const val YB_NO_MAX = 275`、`fuOffset(book)`（DB→`HymnNumbering.DB_NO_MAX`、YB→`YB_NO_MAX`）、`storedOf(book, number, isFu)`、`isValid`、`storedNumbers(book)`、`displayNumbers(book, isFu)`；Step 2 再補 `canAppendDigit`、`canAppendEnglishDigit`、`alsoValidIn` 與完整測試），`HymnRefTest` 才能編譯與通過。

  實作 `hymn/HymnSource.kt`（七來源；`prefValue` 沿用 `LastHymnType` 的值＋新值 `"english"`；`fromPref(null/未知)＝DB`；`supportsFu` 只有 DB、YB）與 `hymn/HymnRef.kt`：`data class HymnRef(val book: String, val storedNo: Int)`，`isFu`/`displayNo` 依 `HymnNumberRules.fuOffset(book)`（DB＝780、YB＝275）推導，`isValid = HymnNumbering.isValid(book, storedNo)`，`fromEntry(book, number, isFu): HymnRef?` 呼叫 `HymnNumberRules.storedOf`。`HymnRef` 是首頁輸入、開啟、歷史**顯示**與重開（H5 的跳轉堆疊）的**唯一**換算點；媒體層（`MediaRecord`、`media_record.isFu`、匯入匯出格式、`MediaConfig` 驗證）**一個字都不改**。

- [ ] **Step 2：`HymnNumberRules` 完整規則（TDD，純 Kotlin；資料來自 `HymnNumbering`；在 Step 1 的最小 API 上擴充）**

  `HymnNumberRulesTest.kt`（節錄，其餘照同模式補齊）：

  ```kotlin
  class HymnNumberRulesTest {
      private fun can(book: String, prefix: String, digit: Int, fu: Boolean = false) =
          HymnNumberRules.canAppendDigit(book, prefix, fu, digit)

      @Test fun validNumberCountsMatchTheBooks() {
          mapOf(HymnTypes.DB to 786, HymnTypes.BB to 513, HymnTypes.XB to 168, HymnTypes.XG to 205,
                HymnTypes.ER to 330, HymnTypes.YB to 277).forEach { (book, n) ->
              assertWithMessage(book).that(HymnNumberRules.storedNumbers(book)).hasSize(n)
          }
      }
      @Test fun gapsAndMissingNumbers() {
          assertThat(HymnNumberRules.isValid(HymnTypes.BB, 37, false)).isTrue()     // `BB_LIMITS[0]=38`：38–100（含）缺號
          assertThat(HymnNumberRules.isValid(HymnTypes.BB, 38, false)).isFalse()
          assertThat(HymnNumberRules.isValid(HymnTypes.ER, 17, false)).isTrue()
          assertThat(HymnNumberRules.isValid(HymnTypes.ER, 18, false)).isFalse()
          assertThat(HymnNumberRules.isValid(HymnTypes.XB, 168, false)).isFalse()   // 新歌 168–170
          assertThat(HymnNumberRules.isValid(HymnTypes.XB, 171, false)).isTrue()
          assertThat(HymnNumberRules.isValid(HymnTypes.XG, 34, false)).isFalse()    // 新詩 34
      }
      @Test fun dbPlainNumbersStopAt780AndFuIs1To6() {
          assertThat(HymnNumberRules.isValid(HymnTypes.DB, 780, false)).isTrue()
          assertThat(HymnNumberRules.isValid(HymnTypes.DB, 781, false)).isFalse()   // 781 只能用「附 1」到
          assertThat(HymnNumberRules.isValid(HymnTypes.DB, 6, true)).isTrue()
          assertThat(HymnNumberRules.isValid(HymnTypes.DB, 7, true)).isFalse()
          assertThat(HymnNumberRules.isValid(HymnTypes.YB, 2, true)).isTrue()
          assertThat(HymnNumberRules.isValid(HymnTypes.YB, 3, true)).isFalse()
      }
      @Test fun digitKeysFollowRealCompletions() {
          assertThat(can(HymnTypes.BB, "3", 9)).isFalse()      // 39 缺號；390–399 在 350–400 缺號內
          assertThat(can(HymnTypes.BB, "3", 7)).isTrue()      // 37 有
          assertThat(can(HymnTypes.BB, "3", 8)).isFalse()     // 38 缺號、380–389 在 350–400 缺號內
          assertThat(can(HymnTypes.BB, "100", 1)).isTrue()     // 1001 有
          assertThat(can(HymnTypes.BB, "100", 0)).isFalse()    // 1000 缺號、無 5 位數
          assertThat(can(HymnTypes.XB, "16", 8)).isFalse()
          assertThat(can(HymnTypes.XB, "17", 1)).isTrue()
          assertThat(can(HymnTypes.XG, "3", 4)).isFalse()
          assertThat(can(HymnTypes.ER, "12", 5)).isFalse()     // 125 起缺號，1250 > 1232
          assertThat(can(HymnTypes.DB, "78", 1)).isFalse()     // 非附模式 781 不存在
          assertThat(can(HymnTypes.DB, "", 7, fu = true)).isFalse()
          assertThat(can(HymnTypes.DB, "", 6, fu = true)).isTrue()
          assertThat(can(HymnTypes.DB, "", 0)).isFalse()       // 沒有 0 開頭的詩號
      }
      @Test fun alsoValidInListsOtherBooksOnly() {
          val also = HymnNumberRules.alsoValidIn(40, isFu = false, except = HymnSource.BB)
          assertThat(also).containsExactly(HymnSource.DB, HymnSource.XB, HymnSource.XG, HymnSource.YB).inOrder()  // 兒童 40 缺號
          assertThat(HymnNumberRules.alsoValidIn(1, isFu = true, except = HymnSource.DB)).containsExactly(HymnSource.YB)
      }
  }
  ```

  另寫 `HymnNumberRulesAssetTest.kt`（讀 `hymnchtv.assetsDir`）：對 `db/bb/xb/xg/er`，`storedNumbers(book)` **等於** `lyrics_<p>_text/*.txt` 的號碼集合（逐號有效性與資產一致；青年只斷言 1..277，因其歌詞多半在別本）；另斷言 `HymnNumberRules.YB_NO_MAX（275）== HymnNoValidate.HYMN_YB_NO_MAX`（編譯期常數，JVM 可讀；`HymnNumbering` 只有 `YB_NO_TMAX=277`，所以 275 由 `HymnNumberRules` 以 `const val YB_NO_MAX = 275` 自己定義，並由這個測試與 `HymnNoValidate` 對照）。**若資產與規則不一致，先回報再決定改規則或資產，不要悄悄調整。**

  補完 `HymnNumberRules`（Step 1 已有 `fuOffset`、`storedOf`、`isValid`、`storedNumbers`、`displayNumbers`）：`fuOffset(book)`（DB→`HymnNumbering.DB_NO_MAX`、YB→275）；`storedOf(book, number, isFu): Int?`（非附：`number in 1..(fuOffset ?: ∞)` 且 `HymnNumbering.isValid`；附：`number>=1` 且 `isValid(book, offset+number)`）；`isValid` ＝ `storedOf != null`；`storedNumbers(book)`（`1..max` 過濾 `HymnNumbering.isValid`，快取）；`displayNumbers(book, isFu)`；`canAppendDigit(book, prefix, isFu, digit)`：`next = prefix + digit`，長度 ≤ 4、不以 0 開頭，且 `displayNumbers(book,isFu)` 中存在以 `next` 為十進位前綴的號碼；`canAppendEnglishDigit(prefix, digit, englishNumbers)` 同規則但資料是英文號集合；`alsoValidIn(number, isFu, except): List<HymnSource>`（依 `HymnSource` 宣告順序、排除 `except` 與英文）。

- [ ] **Step 3：`EnglishXRef`（TDD，取代 `HomeFragment.kt` 檔尾的 `EnglishCrossRef`）**

  `hymn/EnglishXRef.kt`：`class EnglishXRef(private val byEnglish: Map<Int, List<HymnRef>>)`，`candidates(eng)`（檔案順序）、`has(eng)`、`numbers`（排序）；`companion parse(text: String)`：逐行以 `^\^ (\d{4}):.* #(db|bb|xg)(\d+)\s*$` 解析（標題行如 `0001~0099` 略過），`db→DB`、`bb→BB`、`xg→XG`；`fromAssets(open: () -> InputStream)` 讀 `lyrics_toc/toc_all_eng2ch.txt`（App 端以 `AppExecutors.io` 載入並快取為單例，載入失敗回傳空表並 `Timber.w`，預覽顯示「無中文對照」之外另顯 `in_development` 的錯誤 Toast 只出現一次）。

  `EnglishXRefTest.kt`：①`parse` 的合成輸入（含雙目標行、標題含 `#` 的行、非資料行）；②**舊行為的 oracle**：測試內複製舊 `EnglishCrossRef.find` 的 12 行演算法（`key=\^ %04d:.+?`、`dbPage` 取下一行），對真實資產的每個英文號斷言 `candidates(n).first()` ＝舊短按結果、有第二行時 `candidates(n)[1]` ＝舊長按結果；③`numbers` 大小與資產中不同英文號的數量一致。

- [ ] **Step 4：輸入狀態與 reducer（TDD）**

  `ui/picker/PickerState.kt`：`PickerState(source, digits, isFu, englishPick, notice)`、`enum Notice { NO_FU_IN_BOOK }`、`sealed interface Preview { Empty; Valid(ref); Invalid(source, number, isFu, alsoIn); English(englishNo, candidates, pick){ target }; NoCounterpart(englishNo) }`。`PickerReducer`（`object`，全為純函式，回傳新 `PickerState`，不改舊的）：

  ```kotlin
  fun digitEnabled(s: PickerState, digit: Int, xref: EnglishXRef): Boolean   // 英文來源用 canAppendEnglishDigit
  fun pressDigit(s: PickerState, digit: Int, xref: EnglishXRef): PickerState  // 不可用時回原狀態
  fun pressFu(s: PickerState): PickerState       // 來源無附 → notice=NO_FU_IN_BOOK；否則切換 isFu 並清空 digits
  fun backspace(s: PickerState): PickerState     // 先刪一位數字；digits 空且 isFu → 退出附模式
  fun selectSource(s: PickerState, next: HymnSource): PickerState   // 保留 digits；新來源無附 → isFu=false 並 notice=NO_FU_IN_BOOK
  fun pickEnglish(s: PickerState, index: Int): PickerState
  fun preview(s: PickerState, xref: EnglishXRef): Preview
  fun target(p: Preview): HymnRef?               // Valid 或 English 才有
  ```

  `PickerReducerTest.kt`：①補充本輸入 `4`、`5` → `Preview.Valid(HymnRef(BB,45))`；②補充本 `4`、`0` → `Invalid(BB, 40, false, alsoIn=[DB,XB,XG,YB])`，`target==null`（開啟鍵停用）；③大本按「附」再 `3` → `Valid(HymnRef(DB,783))`；青年按「附」再 `1` → `Valid(HymnRef(YB,276))`；④大本附模式再切到補充本 → `isFu=false`＋`notice=NO_FU_IN_BOOK`、digits 保留；⑤切來源後預覽重算（`45` 在 BB 有效、在 ER 無效）；⑥英文來源 `1` → `English(1, [DB 1], 0)`、有雙目標的英文號 → candidates 兩筆且 `pickEnglish(…,1)` 換 target；英文號無對照 → `NoCounterpart`；⑦`backspace` 序列；⑧`pressDigit` 對停用鍵回傳同一物件（`assertThat(next).isSameInstanceAs(s)`）；⑨狀態不可變（舊物件在操作後不變）。

  `HymnPickerViewModelTest`（JVM，純邏輯）：`restoreSource(null/"hymn_bb"/"english"/"垃圾")`＝DB/BB/ENGLISH/DB；旋轉後（同一 VM）`state` 保留；`autoClear` 規則。

- [ ] **Step 5：`PickerChrome`（模式決定哪些按鈕出現；TDD）**

  `ui/picker/PickerChrome.kt`：`data class PickerChrome(val showToc: Boolean, val showMoreHistory: Boolean, val showAddPlaylist: Boolean, val showSetNext: Boolean)`；`PickerChrome.of(mode: PickerMode, notebookEnabled: Boolean)`：`HOME` → toc＝true、more＝true、addPlaylist＝`notebookEnabled`、setNext＝false；`JUMP`（H5 的跳轉面板，H3 先實作規則、不接 UI）→ toc＝false、more＝false、addPlaylist＝false、setNext＝true。`PickerChromeTest`：兩個模式 × `notebookEnabled` true/false 的八種組合。**＋歌單仍受 `UiFlags.NOTEBOOK_UI_ENABLED` 控制**（Step 12 說明打開時機）。

- [ ] **Step 6：`HistoryRecord` 青年附顯示、`HymnLabels`、字串（三語系）**

  - `HistoryRecord.toString()`（LF）的 `HYMN_YB` 分支：`mHymnNo > HymnNoValidate.HYMN_YB_NO_MAX` 時用新字串 `hymn_title_mc_ybs`（「青年 附%1$d: %2$s」，鏡射既有 `hymn_title_mc_dbs`，三個 `strings.xml`＝`values`、`values-zh`、`values-b+zh+Hant` 各加；`values/strings.xml` 內有兩組 `hymn_title_mc_*` 的地方照既有結構都加）。`getHymnNoFu()` 同步（DB 以外也依 `HymnRef` 推導）。**`isFu` 欄位照舊由 `MediaRecord.isFu` 寫入，不改**。
  - `ui/picker/HymnLabels.kt`（需要 `Context`）：`headline(ctx, ref)`（「補充本 第 45 首」、「大本詩歌 附 3」）、`chip(ctx, ref)`（「補45」「大123」「新詩12」「大附3」「青附1」）、`spoken(ctx, ref, title)`（TalkBack：「補充本，第 45 首，祂的計劃」）、`sourceName(ctx, source)`。長名沿用既有 `hymn_type_name_*`。
  - **字串**（`strings_c.xml`×3，鍵／英／簡／繁；**不使用 `›▸←…→〈〉` 這類可能缺字的符號，箭頭一律用 vector drawable 圖示**）：

  | 鍵 | en | zh | zh-Hant |
  |---|---|---|---|
  | `c_search_hint` | Search titles or lyrics | 搜索诗名或歌词 | 搜尋詩名或歌詞 |
  | `c_src_db` / `bb` / `xb` / `xg` / `yb` / `er` / `en` | Main / Supp. / New Songs / New Hymns / Youth / Kids / English | 大本 / 补充 / 新歌 / 新诗 / 青年 / 儿童 / 英文 | 大本 / 補充 / 新歌 / 新詩 / 青年 / 兒童 / 英文 |
  | `c_short_db` … `c_short_er` | Main / Supp / NewS / NewH / Youth / Kids | 大 / 补 / 新歌 / 新诗 / 青 / 儿 | 大 / 補 / 新歌 / 新詩 / 青 / 兒 |
  | `c_chip_fmt` / `c_chip_fu_fmt` | %1$s %2$d / %1$s App.%2$d | %1$s%2$d / %1$s附%2$d | 同簡體 |
  | `c_toc_open`、`c_open` | Contents、Open | 目录、开启 | 目錄、開啟 |
  | `c_label_no` / `c_label_fu` | No. %1$d / Appx. %1$d | 第 %1$d 首 / 附 %1$d | 同簡體 |
  | `c_preview_empty` | Pick a book and type a number | 选择诗歌本并输入诗号 | 選擇詩歌本並輸入詩號 |
  | `c_preview_invalid` | %1$s has no No. %2$s | %1$s无第 %2$s 首 | %1$s無第 %2$s 首 |
  | `c_preview_also` | Also found in: | 此号亦见于： | 此號亦見於： |
  | `c_preview_en_to` | English No. %1$d matches %2$s | 英文 第 %1$d 首 对应 %2$s | 英文 第 %1$d 首 對應 %2$s |
  | `c_preview_en_none` | No Chinese counterpart | 无中文对照 | 無中文對照 |
  | `c_preview_en_other` | Open instead: | 改开： | 改開： |
  | `c_notice_no_fu` | This book has no appendix | 此本无附 | 此本無附 |
  | `c_key_unavailable` | No hymn number continues this way | 没有以此开头的诗号 | 沒有以此開頭的詩號 |
  | `c_key_fu_desc` / `c_key_delete_desc` | Appendix hymns / Delete last digit | 附录诗歌 / 删除一位 | 附錄詩歌 / 刪除一位 |
  | `c_recent_label`、`c_recent_more` | Recent、More | 最近、更多 | 最近、更多 |
  | `c_history_title`、`c_history_empty` | Recently opened、Nothing opened yet | 最近打开过、还没有打开过诗歌 | 最近開過、還沒有開啟過詩歌 |
  | `c_chip_desc` | %1$s, %2$s, %3$s | %1$s，%2$s，%3$s | 同簡體 |
  | `c_chip_remove_action` | Remove from recent | 从最近移除 | 從最近移除 |

  寫完立刻跑 `./gradlew :hymnchtv:testDebugUnitTest --tests '*FontSubsetTest' --tests '*TraditionalResourcesTest'`；缺字就**改字**（不重產字型）。`HymnLabelsTest`（instrumented，用 `createConfigurationContext` 切三語系）：大本附 3、青年附 1（`HymnRef(YB,276)`）、補充 45 的 headline／chip／spoken 在三語系都正確，**青年 276 在歷史列顯示「青年 附1」而不是「青年 276」**。

- [ ] **Step 7：Commit（不碰 G-DB 衝突檔的階段，可在 G-DB 前；含 `HistoryRecord.java` 與 `strings*.xml` 的小修改）**

  ```bash
  ./gradlew :hymnchtv:testDebugUnitTest --console=plain
  git add hymnchtv/src/main/java/org/cog/hymnchtv/hymn hymnchtv/src/main/java/org/cog/hymnchtv/ui/picker/PickerState.kt \
          hymnchtv/src/main/java/org/cog/hymnchtv/ui/picker/PickerReducer.kt hymnchtv/src/main/java/org/cog/hymnchtv/ui/picker/PickerChrome.kt \
          hymnchtv/src/main/java/org/cog/hymnchtv/hymnhistory/HistoryRecord.java hymnchtv/src/main/res/values*/strings*.xml hymnchtv/src/test
  git commit -m "feat: hymn reference, number rules and picker reducer for the new home entry" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

  > **到此為止都不碰 `HomeFragment.kt`／`ContentHandler.java`。Step 8 起先確認 G-DB 已合併並 `git rebase origin/master`。**

- [ ] **Step 8：版面（`hymn_picker.xml` 直向／橫向、`fragment_home.xml`）**

  `res/layout/hymn_picker.xml`（`<merge>` 不可用於 include 之外的根，故根為垂直 `LinearLayout id=picker_root`）由上而下，**id 沿用的標 ★**：
  1. `tv_search`★（`TextView`，非編輯、可點擊、`minHeight=@dimen/c_key_height`、圖示＋提示 `@string/c_search_hint`、`contentDescription`＝同提示；H4 前 `enabled=false`）。
  2. 來源兩列：`src_group_1`（`MaterialButtonToggleGroup`，`app:singleSelection="true"`、`app:selectionRequired="false"`；子鈕 `bs_db`★、`bs_bb`★、`bs_xb`★、`bs_xg`★，皆 `style=Widget.Material3.Button.OutlinedButton`、`android:checkable="true"`、`minHeight=@dimen/c_key_height`）；第二列水平 `LinearLayout`：`src_group_2`（`weight=3`，子鈕 `bs_yb`★、`bs_er`★、`bs_english`〔新〕）＋ `btn_toc`〔新，`weight=1`，**動作鍵外觀區隔**：`Widget.Material3.Button.TonalButton`、尾端 `app:icon=@drawable/ic_chevron_right`、不可勾選〕。**兩個群組各自單選，由 controller 保證跨群組互斥**（見 Step 9）。
  3. `previewArea`〔新，垂直 `LinearLayout`，`android:accessibilityLiveRegion="polite"`〕：`tv_entry`★（大字：預覽標題，如「補充本 第 45 首」或 `c_preview_empty`；`textSize=28sp`、`minHeight=48dp`）、`title_preview`★（詩名／狀態訊息，`maxLines=2`）、`also_in_group`〔新，`ChipGroup`，`visibility=gone`，放「此號亦見於」與英文候選切換的 chip〕。
  4. `keypadArea`★：沿用既有 4 列 12 鍵 `n7 n8 n9 / n4 n5 n6 / n1 n2 n3 / n10 n0 n11`，每鍵 `minHeight=@dimen/c_key_height`（52dp）、`insetTop/Bottom=0`；`n10`＝附、`n11`＝⌫（`contentDescription`＝`c_key_delete_desc`，圖示用 `ic_backspace`）。
  5. `actionArea`★：水平，`btn_open`〔新，`weight=2`，`Widget.Material3.Button`，`enabled=false` 起始，尾端 `ic_chevron_right`〕、`btn_add_playlist`★（`weight=1`，`visibility` 由 `PickerChrome` 控制）、`btn_set_next`〔新，`gone`，H5 用〕。
  6. `recentArea`〔新，水平〕：`tv_recent_label`（`c_recent_label`）、`HorizontalScrollView`＋`ChipGroup recent_chips`（`singleLine`，每個 chip `style=Widget.Material3.Chip.Assist`、`minHeight=48dp`）、`btn_recent_more`（文字鈕 `c_recent_more`＋`ic_chevron_right`）。
  - **移除** `tv_hint`、`btn_search`、`btn_english`、`btn_next`、`historyListView`（首頁不再展開 280dp 清單）。
  - **顏色**：移除 `fragment_home.xml`／新版面所有硬編碼前景色（`@color/black`、`@color/red800`…），一律 `?attr/colorOnSurface`／`?attr/colorPrimary` 等主題屬性；使用者字色與照片背板仍由 `HomeAppearance` 覆寫（Step 9）。淺／深色與照片背景都要達 4.5:1（沿用 `MainScreenColors` 與 `MainScreenContrastTest`）。
  - `res/layout-land/hymn_picker.xml`：**雙欄**——左欄垂直（`tv_search`、來源兩列、`previewArea`、`recentArea`），右欄垂直（`keypadArea`、`actionArea`）；`weight` 各 1；所有 id 與直向相同（`HymnPickerViews` 只有一份）。
  - `fragment_home.xml`：保留 `mainBackground`（背景 `ImageView`）與 `viewMain`（`NestedScrollView`，`fillViewport=true`），內含 `<include layout="@layout/hymn_picker"/>`；其餘不放東西。
  - `values/c_dimens.xml` 加 `c_key_height`＝52dp；`drawable/ic_chevron_right.xml`（vector，`autoMirrored=true`）。
  - 驗證：`./gradlew :hymnchtv:assembleDebug`（lint 的 `HardcodedColor` 不報）。

- [ ] **Step 9：`HymnPickerViews`／`Controller`／`ViewModel`，`HomeFragment` 縮成薄殼**

  - `HymnPickerViews(root)`：取代 `HomeViews`（欄位照 Step 8 的 id；`books: Map<HymnSource, MaterialButton>`、`digits`、`fu`、`delete`、`open`、`toc`、`addPlaylist`、`alsoIn`、`recentChips`、`recentMore`、`searchField`、`previewArea`、`keypadArea`、`actionArea`、`background`〔Home 專用，可為 null〕）。
  - `HymnPickerViewModel : ViewModel()`：`var state: PickerState`、`var autoClear: Boolean`、`restoreSource(saved: String?)`（只套用一次）；`HomeViewModel` 刪除。
  - `HymnPickerController(views, host: PickerHost, mode: PickerMode, vm, prefs, titleSource, xrefProvider)`：綁定按鍵、來源、預覽。要點：
    - **跨群組互斥**：`src_group_1.addOnButtonCheckedListener` 與 `src_group_2` 的 listener 都呼叫 `selectSource(source)`；程式更新選取時用 `suppress` 旗標，避免遞迴：`group1.check/uncheck`、`group2.check/uncheck` 依 `state.source`；使用者在已選鈕上再點（取消勾選）時，**還原勾選**（來源永遠恰好選一個）。
    - **每次狀態改變 → `render()`**：①依 `PickerReducer.digitEnabled` 設 0–9 鍵 `isEnabled`，停用鍵 `ViewCompat.setStateDescription(key, getString(R.string.c_key_unavailable))`（⌫ 永遠可用）；②`n10` 在 `source.supportsFu` 才可用，否則停用且 stateDescription＝`c_notice_no_fu`；`notice` 非空時預覽顯示 `c_notice_no_fu` **一次**後清除；③`tv_entry`＝`HymnLabels.headline`（Valid）／「補充本無第 40 首」（Invalid，`c_preview_invalid`）／英文「英文 第 N 首 對應 大本 第 M 首」（`c_preview_en_to`）／「無中文對照」／空白時 `c_preview_empty`；④`title_preview` 由 `titleSource.lookup(book, storedNo)` 在 `AppExecutors.io("picker-title")` 取得（以遞增 `previewRequest` 丟棄過期結果，沿用 `HomeFragment.refreshPreview` 寫法）；⑤`also_in_group`：Invalid 時為 `alsoIn` 每個來源一個 chip（點了＝`selectSource(來源)` 並保留數字；附號只比對 DB／YB）；English 有多個候選時為其他候選的 chip（`pickEnglish`）；⑥`btn_open.isEnabled = PickerReducer.target(preview) != null`；⑦`btn_add_playlist.visibility = if (chrome.showAddPlaylist) VISIBLE else GONE`（`PickerChrome.of(mode, UiFlags.NOTEBOOK_UI_ENABLED)`）。
    - **按鍵**：`autoClear`（每次 `onResume` 設為 true）為真時，下一個按鍵先把 `digits=""`、`isFu=false` 再處理；來源變更不受 `autoClear` 影響。`bs_yb` 的長按替代（`HYMN_YB_ALT`，104→272 等）**移除**：青年附改由「附」鍵進入（spec §2）。
    - **開啟**：`PickerReducer.target(preview)` 得 `HymnRef`；`MainActivity.setHymnTypeNo(ref.book, ref.storedNo)`；英文來源 `MainActivity.showContent(ctx, ref.book, ref.storedNo, false, englishNo)`，其餘 `showContent(ctx, ref.book, ref.storedNo, false)`。**不再有 `HYMN_BB_DUMMY` 開啟**（無對照時開啟鍵停用）。開啟後 `vm.autoClear = true`。
    - **記住來源**：`selectSource` 時 `prefs.edit().putString(HomePrefs.LAST_HYMN_TYPE, source.prefValue).apply()`（含 `"english"`）；`HomePrefs.LAST_HYMN_TYPE` 的 KDoc 補上新值；`onCreate` 以 `restoreSource` 恢復。**未開啟的號碼與附狀態不跨行程恢復**（只存 ViewModel，旋轉保留）。
    - `PickerHost`：`interface PickerHost { fun openToc(book: String, page: String); fun openSearch(book: String?); fun openHistory(); fun onOpenRef(ref: HymnRef, englishNo: Int?) }`；Home 的實作委派 `MainNavigator`（Step 10）。
  - `HomeFragment`：保留類別名（`MainHost`、測試、D-1 計畫都引用）；內容縮成：`onViewCreated` 建 `HymnPickerViews`＋`HomeAppearance`＋`HymnPickerController(mode=HOME)`；`onResume` 呼叫 `appearance.apply(prefs)` 與 `controller.onResume()`（`autoClear=true`、重載最近）；`onHiddenChanged` 重套外觀；**刪除** `onBackPressed()`（歷史清單已不在首頁）與 `EnglishCrossRef`、`showHymnFromEnglish`、`toggleHistory` 等；`MainHost.onBackPressed()` 對應移除對 `HomeFragment.onBackPressed()` 的呼叫。
  - `HomeAppearance`：欄位改吃 `HymnPickerViews`；`views.hint` 相關程式移除，原本給 `hint` 的 `backdrop` 改給 `previewArea`；字級：數字鍵 `HomePrefs.textSize`、其餘鍵 `size - SMALL_KEY_DELTA`；`coloredButtons` ＝ 數字鍵＋附＋刪除＋七個來源鈕＋`btn_toc`＋`btn_open`；`tv_search` 的背板與提示色沿用現有 `search` 處理。
  - 歷史讀寫都在 `AppExecutors.io`（見通則）；`openFromHistory(record)`：`HymnRef(record.hymnType, record.hymnNo)` → 設 `state`（`source=HymnSource.ofBook`、`digits=ref.displayNo`、`isFu=ref.isFu`）→ `onOpenRef`。**青年附與大本附都正確重開**（原本只處理大本）。
  - 刪除 `HomeEntry.kt`、`HomeViewModel.kt`、`HomeViews.kt`、`HomeEntryTest.kt`（HOST1 加入的 `HomeEntry.next` 與其測試一併移除；D-1 接點改為歌詞頁既有 `btn_next`）。

- [ ] **Step 10：導覽——`openToc`、overlay back stack、最近與記錄頁**

  - `ui/host/MainNavigator.kt`：`interface MainNavigator { fun openToc(book: String, page: String); fun openHistory(); fun openSearch(book: String?) }`（`openSearch` 由 H4 實作，H3 先宣告並讓 `MainActivity` 暫以 `Timber.w` 回應）。
  - `activity_main_host.xml`（LF）：在 `ConstraintLayout` 末尾加 `FragmentContainerView id=overlay_container`（約束 `top=toolbar 下緣`、`bottom=parent`、`start/end=parent`，`android:background="?attr/colorSurface"`、`android:clickable="true"`、`android:focusable="true"`、`visibility=gone`；back stack 非空時由 `MainHost` 設 `visible`）。overlay 蓋住底部導覽（全螢幕感），不攔截 toolbar。
  - `MainHost`：
    - 建構子參數改為 `AppCompatActivity`（Java 呼叫端 `new MainHost(this)` 不變）。
    - `fun openToc(book, page)`：`val existing = fm.findFragmentByTag(tag(R.id.nav_toc)) as? TocFragment`；有 → `existing.select(book, page)`；無 → 記 `pendingToc = book to page`，由 `newToc()` 建立時以 `TocFragment.args(book, page)` 帶入；最後 `popOverlays()` ＋ `nav?.selectedItemId = R.id.nav_toc`。
    - `fun showOverlay(fragment: Fragment, tag: String)`：`fm.beginTransaction().setReorderingAllowed(true).add(R.id.overlay_container, fragment, tag).addToBackStack(tag).commit()`；`fm.addOnBackStackChangedListener` 依 `backStackEntryCount` 切換 `overlay_container` 可見度，並在歸零時還原 toolbar 標題 `R.string.app_title_main`（`supportActionBar?.setTitle(...)`；overlay 自己在 `onStart` 設標題）。
    - `fun popOverlays()`：`fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)`；**使用者點底部導覽任何分頁時先 `popOverlays()`**（`setOnItemSelectedListener` 內）。
    - `onBackPressed()`：**先** `if (fm.backStackEntryCount > 0) { fm.popBackStack(); return true }`，再原本的「非首頁回首頁」；overlay 開啟時系統返回鍵只關 overlay（返回回首頁）。
  - `TocFragment`：`fun select(book: String, page: String)`（`require(book in BOOK_CHIPS && PAGES.any { it.second == page })`）設 `hymnType`/`tocPage`；view 已建立 → 以 `selecting` 旗標包住 `v.books.check(...)` 與頁籤 `selectTab`，避免 listener 重入，再 `load()`；view 未建立 → 只設欄位，`onViewCreated` 套用既有欄位；`companion fun args(book, page): Bundle`；`onCreate` 優先序：`savedInstanceState` ＞ `arguments` ＞ `LastHymnType`。中文來源 → `(該本, TOC_CATEGORY)`；英文來源 → `(HYMN_DB, TOC_ENGLISH)`（英文索引頁籤）。
  - **`MainActivity.java`（CRLF）只做最小改動**：①類別宣告加 `, MainNavigator`；②加三個方法 `openToc(String book, String page)`、`openHistory()`、`openSearch(String book)`，各一行委派給 `mainHost`（`mainHost` 於 `onCreate` 建立，方法內 null 檢查）。用 Python 以 bytes 改、帶 `\r\n`，並照通則驗證 `git diff --ignore-cr-at-eol --stat` 與 `--stat` 相同。
  - `ui/home/HistoryFragment.kt` ＋ `fragment_history.xml`（`RecyclerView history_list` ＋ `tv_history_empty`）：重用 `HistoryAdapter`（列、可見刪除鈕、滑動刪除、長按確認對話框）；`onStart` 設標題 `c_history_title`；讀取、刪除都在 `AppExecutors.io`。**1.1.0 只有「最近開過」一個清單**；D-1 之後此頁改成兩個分頁（見 D-1 計畫 Task I2 的 Step 2b）。`HistoryAdapter` 顯示字串改用 `HymnLabels`／已修的 `HistoryRecord.toString()`。
  - `RecentChips`：最新 8 筆（`historyRecords.take(8)`）→ chip（文字＝`HymnLabels.chip`，`contentDescription`＝`c_chip_desc`〔`headline`、詩名〕）；點擊＝`openFromHistory`；長按＝刪除確認對話框；另以 `ViewCompat.addAccessibilityAction(chip, getString(R.string.c_chip_remove_action)) { confirmDelete(record); true }` 給 TalkBack；沒有記錄時整列 `recentArea` 隱藏。`btn_recent_more` → `host.openHistory()`。

- [ ] **Step 11：D-1 的「＋歌單」槽位與 `UiFlags`（1.1.0 維持 false）**

  `btn_add_playlist` 的可見度只由 `PickerChrome.of(mode, UiFlags.NOTEBOOK_UI_ENABLED)` 決定；`MainHost.tabIds(UiFlags.NOTEBOOK_UI_ENABLED)` 不變。**H3 不放任何 no-op 按鈕**。`UiFlags.kt` 的 KDoc 補一句：「打開時機＝D-1 UI 完成時（Task I2 Step 4）；1.1.0 維持 false」。

- [ ] **Step 12：更新因改版而失效的舊測試（逐一列出）**

  | 檔案 | 處理 |
  |---|---|
  | `ui/home/HomeFragmentTest.kt` | `git mv` 成 `ui/picker/HymnPickerTest.kt` 並依新行為重寫（見 Step 13 的測試清單）；原「歷史列開啟／刪除」改測 `HistoryFragment` 與 chip |
  | `ui/MainHostTest.kt` | `tv_entry` 文字斷言改為預覽標題（輸入 `1`、`2` 後顯示含「第 12 首」）；`historyListView` 相關測試改為「點 `btn_recent_more` 開 `HistoryFragment`、返回鍵回首頁」；`btn_next` 的測試**刪除**（功能被 §8 的歌詞頁「下一首」取代，H5 之前歌詞頁 `btn_next` 維持現況） |
  | `ui/SmokeFlowTest.kt` | 流程改為 `bs_db` → `n1` → `btn_open`（`btn_open` 在輸入有效號碼後才可用）→ 歌詞頁 → 左滑 → 播放 |
  | `reading/MainScreenContrastTest.kt` | id 清單移除 `btn_search`，加入 `btn_open`、`bs_db` |
  | `reading/PhotoBackdropTest.kt` | 把 `tv_hint` 的背板斷言改為 `previewArea` |
  | `test/.../ui/home/HomeEntryTest.kt` | 刪除 |

- [ ] **Step 13：新測試（instrumented）**

  - **非同步與資料庫規則**：歷史寫入是 `AppExecutors.io` 的非同步工作；測試用 `FragmentHost.eventually { … }` 輪詢（逾時 3 秒），**輪詢條件在 instrumentation 執行緒呼叫 `DatabaseBackend`**（不在 `scenario.onActivity`／主執行緒 lambda 內），確認後才回 UI 斷言。
  - `HymnPickerTest`：①來源鈕單選且 `isChecked` 暴露（兩群組互斥：點 `bs_yb` 後 `bs_db.isChecked==false`）；②記住來源（含 `bs_english`，重啟 `ActivityScenario` 後仍選取）；③補充本 `3` 後 `9` 鍵停用且 `stateDescription` 是 `c_key_unavailable`；④補充本 `4`、`0` → `tv_entry` 含「無第 40 首」、`btn_open` 停用、`also_in_group` 有「大本」chip，點它→來源切到大本、數字保留、預覽變有效；⑤大本「附」`3` → 預覽「附 3」；點 `bs_bb` → 退出附模式、顯示「此本無附」；⑥英文 `1` → 預覽對應詩；英文無對照 → `btn_open` 停用；有雙目標的英文號 → 候選 chip 可切換；⑦`btn_open` 開歌詞頁，**歷史多一筆且青年附 276 顯示「青年 附1」、重開同一首**（`HistoryRecord.isFu` 欄位仍為 0）；⑧`btn_toc` 在中文來源開目錄分頁並預選 `(該本, 類別)`，英文來源預選 `(大本, 英中對照)`（`TocSelectTest`）；⑨旋轉後數字與來源保留；⑩無障礙：`previewArea` 的 `accessibilityLiveRegion` ＝ polite，來源鈕有 checked 狀態，焦點順序（`accessibilityTraversalBefore` 或視圖順序）：搜尋→來源→預覽→鍵盤→開啟→最近→更多。
  - `HistoryFragmentTest`／`OverlayNavigationTest`：`btn_recent_more` 開 overlay、返回鍵關 overlay 回首頁；overlay 開啟時點底部導覽分頁 → overlay 先被關閉；旋轉後 overlay 仍在；滑動刪除、可見刪除鈕、長按確認；歷史最多顯示 8 個 chip、順序最新優先。
  - `HomeLayoutTest`：`assumeTrue(screenHeightDp >= 720)`（G4 在 360×720 跑）→ 首頁在不捲動下 `tv_search` 到 `btn_open` 全部 `getGlobalVisibleRect` 可見；另一個測試（任何尺寸）斷言所有可點元件（`n0–n11`、`bs_*`、`btn_toc`、`btn_open`、`tv_search`、chip）高度 ≥ 48dp；**320×640 與字級 1.3 允許捲動，但不得小於 48dp**。

- [ ] **Step 14：驗收（協調者在 `api34b`；`api24b` 跑 instrumented 全套）**

  ```bash
  export ANDROID_SERIAL=emulator-5580       # api34b；確認 adb emu avd name
  OUT=$TMPDIR/h3-shots; mkdir -p $OUT
  shot() { adb shell am force-stop com.ziontkec.hymnal; adb shell am start -n com.ziontkec.hymnal/org.cog.hymnchtv.MainActivity >/dev/null; sleep 3; adb exec-out screencap -p > "$OUT/$1.png"; }
  for size in 320x640 360x720; do
    adb shell wm size $size; adb shell wm density 160
    for scale in 1.0 1.3; do
      adb shell settings put system font_scale $scale
      for night in no yes; do adb shell cmd uiautomator night $night; shot "$size-font$scale-night_$night"; done
    done
  done
  adb shell cmd uiautomator night no; adb shell settings put system accelerometer_rotation 0; adb shell settings put system user_rotation 1
  shot land-360x720; adb shell settings put system user_rotation 0
  adb shell wm size reset; adb shell wm density reset; adb shell settings put system font_scale 1.0
  ./gradlew :hymnchtv:connectedDebugAndroidTest --console=plain          # api34b，再以 5582（api24b）各跑一次
  ./gradlew -PstrictDbThread :hymnchtv:installDebug --console=plain       # G-DB 之後；手動走完整條首頁流程
  ```
  驗收標準：360×720 字級 1.0 直向無捲動即可看到 `btn_open`；320×640 與字級 1.3 可捲動、觸控目標不小於 48dp、文字不重疊；淺／深色文字對比 ≥ 4.5:1；橫向雙欄；TalkBack（`api34b` 開啟）讀得出預覽、來源 checked 狀態與停用原因。截圖貼進 PR 描述。

- [ ] **Step 15：Commit（可分 2 個）**

  ```bash
  ./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug :hymnchtv:assembleDebugAndroidTest --console=plain
  git add -A hymnchtv/src
  git commit -m "feat: reshape the home tab into book-first entry with live preview, recents and history page" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```
  （`MainActivity.java` 改動以 `git ls-files --eol` 確認仍為 CRLF；新檔皆 LF。）

---
## Task H4：搜尋核心重構與搜尋頁

> 依據：spec §4、§7。前置：H3 Step 1–4（`HymnRef`、`HymnNumberRules`）；Step 1–2（純核心）可與 H3 後段平行，Step 3 起要 H3 Step 10 的 `MainNavigator`／overlay 已就位（同一分支）。

**對照程式碼（C worktree 7985e0c0 實測）：**
- `ContentSearch.java`（CRLF，371 行，Activity）：六本各一段 `while` 迴圈逐檔讀 `lyrics_<p>_text/<p><no>.txt`，**以號碼為 key 存進 `Map<Integer,String> mHmynNoType`**（`LinkedHashMap`）——跨本同號（例如大本 5 與補充 5 都命中）後者覆寫前者的書別，點擊會開錯本；上限 `HYMN_COUNT_MAX = 100`；命中片段取「命中處所在行起，最多 64 字」；`result.substring(4)` 以魔術數字跳過檔頭號碼行（連標題首字也一起跳掉）。
- 查詢匹配：`search/SearchPattern.build(query, T2sMap)`（literal 比對；「他」同時匹配「祂」；繁體字經 `T2sMap`〔`assets/lyrics_t2s_map.txt`，由 `tools/gen_lyrics_hant.py` 產生〕展開成簡體候選）。**spec 寫「OpenCC」，實際是 `SearchPattern`＋`T2sMap`**，H4 沿用實際機制。
- 呼叫端：`HomeFragment.onSearch()`（開 `ContentSearch`，extra `MainActivity.ATTR_SEARCH`）是**唯一**呼叫端；另有 `AndroidManifest.xml`（CRLF）第 105 行 `<activity android:name=".ContentSearch" />`、`androidTest/.../ActionBarThemeTest.kt` 的 `contentSearchHasActionBar`、`layout/content_search.xml`、`layout/search_result.xml`（皆 CRLF，僅 `ContentSearch` 使用）、字串 `hymn_match*`／`hymn_search`／`error_search_empty`。
- 顯示字形：歌詞頁以 `LyricsLanguagePolicy.resolveShowTraditional(LyricsLang.fromPref(prefs.getString(PREF_LYRICS_DEFAULT)), locale)` 決定簡／繁，繁體讀 `LyricsAssets.hantPath(path, variant)`（`variant = LyricsLanguagePolicy.parseVariant(prefs.getString(ContentView.PREF_CONVERSION_TYPE), locale)`）；`LyricsHantAssetsTest` 保證 hant 目錄鏡射簡體檔案集。

**Files：**
- Create：`search/HymnSearch.kt`、`search/AssetLyricsSource.kt`、`ui/search/SearchFragment.kt`、`ui/search/SearchViewModel.kt`、`ui/search/SearchAdapter.kt`、`res/layout/fragment_search.xml`、`res/layout/row_search_result.xml`、`values*/strings_c.xml`（三檔，LF）
- Modify：`ui/titles/AssetHymnTitles.kt`（抽 `titleOf(text)`）、`ui/host/MainHost.kt`、`ui/host/MainNavigator.kt`、`ui/picker/HymnPickerController.kt`（啟用搜尋欄）、**`MainActivity.java`（CRLF，補 `openSearch` 委派）**、**`AndroidManifest.xml`（CRLF，移除 `ContentSearch`）**、`androidTest/.../ActionBarThemeTest.kt`
- Delete：**`ContentSearch.java`（CRLF）**、`res/layout/content_search.xml`、`res/layout/search_result.xml`（皆 CRLF；先 `grep -rn` 確認無其他引用）
- Test（JVM）：`test/.../search/{HymnSearchTest,SnippetExtractorTest,HymnSearchAssetTest}.kt`、`test/.../ui/titles/AssetHymnTitlesTest.kt`（補 `titleOf`）
- Test（instrumented）：`androidTest/.../ui/search/{SearchFragmentTest,SearchPerfTest}.kt`

- [ ] **Step 1：先抽 `AssetHymnTitles.titleOf`（1a），再做核心資料型別與搜尋（1b；TDD，純 Kotlin，可在 G-DB 前做）**

  **1a（前置，否則 1b 無法編譯）**：`ui/titles/AssetHymnTitles.kt`（LF）把 `lookup` 內「第 2 行去類別前綴＋第 3 行『（…）』補註」抽成 `companion fun titleOf(text: String): String?`，`lookup` 改呼叫它（行為不變，`AssetHymnTitlesTest` 原有測試必須維持綠），並補 `titleOf` 的測試；同檔私有的 `prefixOf` 改為 `internal`（Step 2 的 `AssetLyricsSource` 用）。

  **1b**：

  先寫 `HymnSearchTest.kt`（用假的 `LyricsSource`，不依賴 assets）：

  ```kotlin
  class HymnSearchTest {
      private fun text(title: String, body: String) = "1\r\n$title\r\n（英1）\r\n降A大调4/4\r\n\r\n$body\r\n"
      /** 前 n 首大本都含「祂」；其餘沒有檔案。 */
      private fun dbHits(n: Int) = LyricsSource { ref ->
          if (ref.book == HymnTypes.DB && ref.storedNo <= n) text("颂赞－标题${ref.storedNo}", "一\r\n祂是主") else null
      }

      @Test fun limitBoundaries() {
          for ((hits, expectMore) in listOf(199 to false, 200 to false, 201 to true)) {
              val page = HymnSearch(dbHits(hits)).search("祂", SearchScope.All, limit = 200)
              assertWithMessage("hits=$hits").that(page.results).hasSize(minOf(hits, 200))
              assertWithMessage("hits=$hits").that(page.hasMore).isEqualTo(expectMore)
          }
      }
      @Test fun sameNumberInTwoBooksKeepsBothWithTheirOwnBook() {   // 舊 ContentSearch 以號碼為 key 會覆寫
          val src = LyricsSource { ref -> if (ref.storedNo == 5 && (ref.book == HymnTypes.DB || ref.book == HymnTypes.BB)) text("甲－乙", "祂是主") else null }
          val refs = HymnSearch(src).search("祂", SearchScope.All).results.map { it.ref }
          assertThat(refs).containsExactly(HymnRef(HymnTypes.DB, 5), HymnRef(HymnTypes.BB, 5)).inOrder()
      }
      @Test fun scopeLimitsToOneBook() { /* Book(BB) 只回 BB 的命中，且順序為 DB,BB,XB,XG,YB,ER 的既有順序 */ }
      @Test fun traditionalQueryMatchesSimplifiedLyrics() {
          val t2s = T2sMap.parse(listOf("祂\t他", "詩\t诗"))        // 合成對照表
          val src = LyricsSource { ref -> if (ref == HymnRef(HymnTypes.DB, 1)) text("颂赞－诗歌", "一\r\n他是主") else null }
          assertThat(HymnSearch(src, t2s).search("詩", SearchScope.All).results).hasSize(1)
      }
      @Test fun fuHitsCarryTheFuRef() {                           // 大本 781＝附 1；青年 276＝附 1
          val src = LyricsSource { ref -> if (ref.storedNo > 275 && ref.book == HymnTypes.YB || ref == HymnRef(HymnTypes.DB, 781)) text("甲－乙", "祂") else null }
          val refs = HymnSearch(src).search("祂", SearchScope.All).results.map { it.ref }
          assertThat(refs.map { it.isFu }).containsExactly(true, true, true)   // DB 781、YB 276、YB 277
      }
      @Test fun titleHitAndLyricsHitBothFound() { /* 查標題中的字 → snippet 是標題行；查歌詞中的字 → snippet 是該節行 */ }
      @Test fun blankQueryReturnsEmptyAndCancelStopsEarly() { /* isCancelled=true 時盡快回傳，不拋例外 */ }
      @Test fun traditionalDisplayUsesTheHantTextWhenLineCountsMatch() { /* fake traditional(ref) 行數相同 → snippet/title 取繁體；行數不同 → 退回簡體 */ }
  }
  ```

  `SnippetExtractorTest.kt`：①**跳過第一行（詩號行）而不是 4 個字元**（標題首字也可被命中：查「颂」命中標題行）；②命中行起、行與行以單一空白相接、最多 64 字；③CRLF 與 LF 混用；④多個命中只取第一個；⑤沒有命中回 `null`。

  實作 `search/HymnSearch.kt`：

  ```kotlin
  sealed interface SearchScope { object All : SearchScope; data class Book(val book: String) : SearchScope }
  data class SearchResult(val ref: HymnRef, val title: String, val snippet: String)   // 不可分割：顯示號與附號一律由 ref 推導
  data class SearchPage(val results: List<SearchResult>, val hasMore: Boolean)

  fun interface LyricsSource {
      /** 簡體歌詞全文（含檔頭號碼行）；該號沒有檔案回 null。 */
      fun simplified(ref: HymnRef): String?
      /** 使用者字形設定為繁體時的對應繁體全文；否則 null（顯示用，不參與比對）。 */
      fun traditional(ref: HymnRef): String? = null
  }

  class HymnSearch(private val lyrics: LyricsSource, private val t2s: T2sMap = T2sMap.EMPTY) {
      fun search(query: String, scope: SearchScope, limit: Int = DEFAULT_LIMIT, isCancelled: () -> Boolean = { false }): SearchPage {
          require(limit >= 1)
          val pattern = SearchPattern.build(query, t2s) ?: return SearchPage(emptyList(), false)
          val out = ArrayList<SearchResult>()
          for (book in booksOf(scope)) for (no in HymnNumberRules.storedNumbers(book)) {
              if (isCancelled()) return SearchPage(out.take(limit), false)
              val ref = HymnRef(book, no)
              val simplified = lyrics.simplified(ref) ?: continue
              val hit = SnippetExtractor.find(simplified, pattern) ?: continue
              out += toResult(ref, simplified, hit)
              if (out.size > limit) return SearchPage(out.subList(0, limit).toList(), true)   // limit+1 探測
          }
          return SearchPage(out.toList(), false)
      }
      companion object { const val DEFAULT_LIMIT = 200; val BOOK_ORDER = listOf(DB, BB, XB, XG, YB, ER) }
  }
  ```

  `SnippetExtractor.find(text, pattern): SnippetHit?`（`SnippetHit(lineIndex, snippet)`）；`toResult` 在 `lyrics.traditional(ref)` 非 null 且行數與簡體相同時，用同一 `lineIndex` 取繁體行組 snippet、用繁體文字取標題，否則用簡體。標題用 `AssetHymnTitles.titleOf(text)`（Step 1a 已抽出）。

- [ ] **Step 2：`AssetHymnTitles.titleOf`、`AssetLyricsSource`、真實資產測試**

  - （`AssetHymnTitles.titleOf` 已在 Step 1a 完成。）
  - `search/AssetLyricsSource.kt`：`class AssetLyricsSource(context, traditionalVariant: HantVariant?)`；`simplified(ref)` 讀 `lyrics_<p>_text/<p><no>.txt`（UTF-8，`\r\n`→`\n`；`IOException` → `null` 並 `Timber.v`，不是吞掉：缺檔是正常情況〔青年只有 139 檔、XG 34 等〕）；`traditional(ref)` 在 `traditionalVariant != null` 時讀 `LyricsAssets.hantPath(...)`。工廠 `AssetLyricsSource.forPrefs(context, prefs)` 依 `LyricsLanguagePolicy.resolveShowTraditional(...)` 與 `parseVariant(...)` 決定。
  - `HymnSearchAssetTest.kt`（JVM，以 `hymnchtv.assetsDir` 讀真檔、`T2sMap.parse(assets/lyrics_t2s_map.txt)`）：①簡體「祂的计划」命中大本 1，`snippet` 含關鍵字；②繁體「祂的計劃」同樣命中（T2S）；③搜「的」於 `All`、`limit=200` → `results.size==200 && hasMore`；④`Book(BB)` 的結果 `ref.book` 全為 BB；⑤**hant 目錄與簡體行數逐檔相同**（`LyricsHantAssetsTest` 之外再加行數斷言，保證 snippet 以行號映射成立）；⑥大本附（781–786）可被搜到時 `ref.isFu`。

- [ ] **Step 3：`SearchFragment`、ViewModel、Adapter、版面、字串**

  - `SearchViewModel`：`query`、`scope`、`status`、`page`；`search()` 在 `AppExecutors.io("hymn-search")` 執行，以遞增 `requestId` 丟棄過期結果、把 `requestId` 以外的取消旗標接到 `HymnSearch` 的 `isCancelled`；輸入停 300ms 或按 IME 搜尋鍵才觸發（`Handler.postDelayed` 在 Fragment 的 `viewLifecycleOwner` 內，`onDestroyView` 清除）。ViewModel 保留結果（旋轉、開歌詞後返回仍在）。
  - `fragment_search.xml`（LF）：`TextInputLayout`＋`search_input`（`imeOptions=actionSearch`、`inputType=text`、清除圖示）、`ChipGroup search_scope`（單選：`scope_current`「目前來源：補充本」、`scope_all`「全部」；**英文來源或沒有目前來源時隱藏 `scope_current`**）、`tv_search_status`（`accessibilityLiveRegion=polite`）、`ProgressBar search_progress`、`RecyclerView search_results`。`row_search_result.xml`：三行（`tv_result_label`＝`HymnLabels.headline`、`tv_result_title`、`tv_result_snippet`），整列可點、`minHeight=64dp`，`contentDescription` 為 headline＋標題。
  - 狀態文字：`c_search_empty_hint`（尚未輸入）、`c_search_loading`、`c_search_status_count`（「找到 N 首」）、`c_search_status_none`（「找不到匹配」）、**`hasMore` 時改顯示 `c_search_status_more`（「結果過多，請縮小範圍或加長關鍵字」）**。字串三語系：

    | 鍵 | en | zh | zh-Hant |
    |---|---|---|---|
    | `c_search_title` | Search | 搜索 | 搜尋 |
    | `c_scope_current` | Current: %1$s | 当前来源：%1$s | 目前來源：%1$s |
    | `c_scope_all` | All books | 全部 | 全部 |
    | `c_search_empty_hint` | Type a title or part of the lyrics | 输入诗名或部分歌词 | 輸入詩名或部分歌詞 |
    | `c_search_loading` | Searching | 搜索中 | 搜尋中 |
    | `c_search_status_count` | %1$d hymns found | 找到 %1$d 首 | 找到 %1$d 首 |
    | `c_search_status_none` | No match | 找不到匹配 | 找不到匹配 |
    | `c_search_status_more` | Too many results; narrow the scope or use a longer keyword | 结果过多，请缩小范围或加长关键字 | 結果過多，請縮小範圍或加長關鍵字 |
    | `c_search_clear_desc` | Clear | 清除 | 清除 |

    寫完先跑 `FontSubsetTest`／`TraditionalResourcesTest`（缺字就改字，不重產字型）。
  - 點結果：`MainActivity.setHymnTypeNo(ref.book, ref.storedNo)`＋`MainActivity.showContent(ctx, ref.book, ref.storedNo, false)`。`SearchFragment` 的標題在 `onStart` 設為 `c_search_title`。

- [ ] **Step 4：接線與移除 `ContentSearch`**

  - `MainNavigator.openSearch(book: String?)`：`MainHost.openSearch(book)`＝`showOverlay(SearchFragment.newInstance(book), "search")`；`SearchFragment.newInstance(book)`：`book` 為 null（英文來源）時預設範圍＝全部。`MainActivity.java`（CRLF）把 H3 暫時的 `openSearch` 實作換成委派（bytes 改法同 H3）。
  - `HymnPickerController`：`tv_search.isEnabled = true`，點擊 → `host.openSearch(state.source.book)`（**英文來源傳 null → 範圍＝全部**）；鍵盤焦點與 IME 由搜尋頁處理，首頁不再有可編輯的搜尋欄。
  - **移除舊搜尋**：`HomeFragment`／controller 對 `ContentSearch`、`MainActivity.ATTR_SEARCH` 的使用全部刪除（`ATTR_SEARCH` 常數在 `MainActivity.java`，確認 `grep -rn ATTR_SEARCH hymnchtv/src` 只剩常數本身後**保留常數不刪**，避免動 CRLF 檔的多餘行）；`git rm ContentSearch.java res/layout/content_search.xml res/layout/search_result.xml`；`AndroidManifest.xml`（CRLF）刪除 `<activity android:name=".ContentSearch" />` 一行；`ActionBarThemeTest.kt` 刪除 `contentSearchHasActionBar`，改在 `SearchFragmentTest` 斷言「搜尋 overlay 開啟時 host 的 `Toolbar` 顯示標題 `c_search_title`」。
  - 字串清理：`hymn_match`、`hymn_match_none`、`hymn_match_*`（`er/xb/xg/yb/bb/db/db_sp`）、`hymn_search`、`error_search_empty`：逐一 `grep -rn '<key>' hymnchtv/src/main`，**只刪除在三個 `strings*.xml` 以外零引用者**，三語系同步刪；`hymn_match_auto_create` 等仍被引用的保留。刪完跑 `TraditionalResourcesTest`／`FontSubsetTest`。
  - 驗證：`grep -rn 'ContentSearch' hymnchtv/src` 無結果（歷史註解 `SearchPattern.kt` 第 8 行「same as the previous ContentSearch behavior」可保留）。

- [ ] **Step 5：測試（instrumented）與效能**

  - `SearchFragmentTest`（`FragmentHost` 或經 `MainActivity` 點 `tv_search`）：①輸入「祂的計劃」→ 列表出現、第一筆是大本 1，`tv_search_status` 顯示「找到 N 首」；②點結果開歌詞頁且是該本該號（青年附 276 的結果開 `HYMN_YB` 276）；③`scope_current`／`scope_all` 切換：BB 範圍只出 BB；英文來源進入時沒有 `scope_current`；④搜「的」→ 列表 200 筆且狀態是 `c_search_status_more`；⑤空查詢顯示提示；⑥旋轉後結果仍在；⑦返回鍵關閉搜尋頁回首頁；⑧搜尋進行中離開頁面不崩潰（`onDestroyView` 取消）。
  - `SearchPerfTest`（`api24b` 與 `api34b` 都跑）：`scope=All`、查一個不存在的字串，**硬斷言**整體時間 ≤ 10 秒（`api24b`）／≤ 5 秒（`api34b`），並 `Log` 實測值貼進 PR；超過時**先最佳化**（例如改用 `AssetManager.openFd`／並行讀檔、避免 `readText` 整檔配置）再重測，**不得放寬門檻**；同時 `Debug.getNativeHeapAllocatedSize`／`Runtime` 記憶體不應隨結果數線性暴增（結果只存 `SearchResult`，不存全文）。

- [ ] **Step 6：驗收與 Commit**

  ```bash
  ./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug :hymnchtv:assembleDebugAndroidTest --console=plain
  export ANDROID_SERIAL=emulator-5580; ./gradlew :hymnchtv:connectedDebugAndroidTest --tests 'org.cog.hymnchtv.ui.search.*' --console=plain
  export ANDROID_SERIAL=emulator-5582; ./gradlew :hymnchtv:connectedDebugAndroidTest --tests 'org.cog.hymnchtv.ui.search.*' --console=plain
  git add -A hymnchtv/src
  git commit -m "feat: structured hymn search core and full-screen search page, drop ContentSearch" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```
  `git ls-files --eol hymnchtv/src/main/AndroidManifest.xml hymnchtv/src/main/java/org/cog/hymnchtv/MainActivity.java` 仍為 `w/crlf`。**G4：H3＋H4 合併。**

---
## Task F2：對帳收尾（spec 對帳＋已決定事項；rev 4 修訂）

> **不得依賴 H5**（H5 延後到 1.1 之後）。前置：G4（H3＋H4 已合併）、G-DB 已合併並 `rebase origin/master`。分支 `feat/c-1-1-reconcile`。**G5＝本 Task 合併；G5 後才發 1.1.0。**

**F1 實際狀態（7985e0c0 實測，F2 以此為準）：** `res/menu/menu_content.xml` **已刪除**（`res/menu/` 只剩 `menu_bottom_nav.xml`、`menu_lyrics_more.xml`）；`AndroidManifest.xml` **已沒有** `HymnToc` 的 `<activity>`；但 `HymnToc.java`（CRLF，328 行）仍在，且仍是 `ContentHandler`（靜態 import `category_bb/db/er/xb`、`hymnCategory*`）、`TocBuilder.kt`、`TocFragment.kt`、`AssetHymnTitles.kt`、`HymnNoCh2EngXRef.java`（`TOC_BB/DB/XG`）的常數來源——它的 **Activity 外殼是死碼**（沒有任何 `startActivity`）。

**Files：** Modify `mediaconfig/MediaConfig.java`（LF）、`ContentHandler.java`（**CRLF**）、`utils/HymnNoCh2EngXRef.java`（**CRLF**，把 `import static …HymnToc.TOC_BB/TOC_DB/TOC_XG` 改成 `import static org.cog.hymnchtv.ui.toc.TocConstants.TOC_BB` 等；`TocConstants` 為 Kotlin `object`，`const val`／`@JvmField` 可被 Java 以靜態欄位引用）、`ui/toc/TocBuilder.kt`、`TocFragment.kt`、`ui/titles/AssetHymnTitles.kt`、**`androidTest/.../ui/toc/TocFragmentTest.kt`**（第 17、103、104 行 import 並使用 `HymnToc.TOC_CATEGORY`／`hymnCategoryBb`，改為 `TocConstants`）、`res/layout/media_config.xml`＋`layout-land/media_config.xml`（CRLF）、`media_player_audio_ui.xml`（LF）、`content_lyrics.xml`（CRLF）、`ui/theme/EdgeToEdge.kt`；Create `ui/toc/TocConstants.kt`、`androidTest/.../ui/{EllipsisAudit,TruncationAuditTest}.kt`；Delete `HymnToc.java`（CRLF）、`HymnTocExpandableListAdapter.java`、`res/layout/hymn_toc*.xml`。

- [ ] **Step 0：基線與盤點**

  ```bash
  git fetch origin && git switch -c feat/c-1-1-reconcile origin/master
  ./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug :hymnchtv:assembleDebugAndroidTest --console=plain
  grep -rnE 'HymnToc\b' hymnchtv/src --include='*.kt' --include='*.java' --include='*.xml' | grep -v 'HymnToc.java'   # 常數使用者清單
  grep -rnE 'hymnGoogleSearch|hymnQqSearch' hymnchtv/src
  grep -rnE 'button_NQ|button_import|onLongClick|QQ_LINK|ASSET_URL_IMPORT_FILE' hymnchtv/src/main/java/org/cog/hymnchtv/mediaconfig/MediaConfig.java
  ```

- [ ] **Step 1：spec 逐條對帳（寫進 PR 描述的「對帳清單」）**

  依下表逐條核對並勾選；**未滿足項在本 Task 補完，或明確標註「延後」並說明理由**。表格存成 PR 描述的 Markdown（不新增檔案）。

  | 來源 | 條目 | 預期狀態 |
  |---|---|---|
  | C spec `specs/2026-10-02-c-ui-design.md` §9 | C-1～C-11 逐條 | 逐條寫「已滿足／延後（D-1）」：C-1/C-2/C-5/C-6/C-7/C-9 為 D-1 接點（1.1.0 未接，`UiFlags` 關閉）；C-3/C-4/C-8/C-10/C-11 已滿足 |
  | C spec §4 功能對應表、§4.3 長按 13 項 | 每項有落點、長按全移除或可見化 | F1 已核對；F2 再確認 `MediaConfig` 的兩個長按（見 Step 2） |
  | home spec §1 版面 | 搜尋→來源→預覽→鍵盤→開啟→最近；360×720 不捲動；320×640／字級 1.3 可捲動且觸控 ≥ 48dp；橫向雙欄；移除長提示；顏色走主題屬性 | H3 Step 8/13/14 的測試與截圖 |
  | §2 來源與號碼 | 七來源單選、記住來源（含英文）、切來源保留數字、`HymnNumberRules` 逐號有效性與停用鍵、附號契約（`HymnRef`、媒體層不動）、預覽（合法／不合法＋亦見於／英文）、開啟、`btn_english` 與長按移除、＋歌單隱藏、`btn_next` 與 `HomeEntry.next` 移除 | H3 Step 1–13 |
  | §3 目錄 | `openToc(book, page)`／`TocFragment.select` | H3 Step 10（`TocSelectTest`） |
  | §4 搜尋 | `HymnSearch`／`SearchPage(hasMore)`／`SearchResult(ref: HymnRef)`、跨本同號、上限 200 與 199/200/201 邊界、繁簡、附號、`ContentSearch` 移除 | H4（注意：實際匹配機制是 `SearchPattern`＋`T2sMap`，spec 寫的 OpenCC 是文字出入，**F2 在 spec 勘誤欄記一筆，不改程式**） |
  | §5 記錄 | 最新 8 筆 chip（縮寫＋TalkBack label）、點擊重開、長按刪除、「更多›」`HistoryFragment`（**1.1.0 只有「最近開過」**）、移除 `tv_entry` 展開清單 | H3 Step 10；唱詩紀錄分頁＝D-1（延後） |
  | §6 無障礙 | 焦點順序、checked 狀態、停用原因、polite live region、觸控 ≥ 48dp | H3 Step 13 的測試 |
  | §7 範圍與排程 | H3、H4 完成；D-1 接點（＋歌單、唱詩紀錄分頁、歌詞頁下一首）列為 D-1 工作 | PR 描述 |
  | **§8 歌詞頁跳轉（H5）** | **延後（1.1 之後，D-1 完成後或之前再排）**；**不得列為缺漏**；1.1.0 的歌詞頁 `btn_next`／`scrollNextHymn()` 維持現況 | PR 描述寫明 |

- [ ] **Step 2：移除 `MediaConfig` 的 `button_NQ`、`button_import` 長按（已決定）**

  `mediaconfig/MediaConfig.java`（LF，1546 行）：刪除 `btnNQ.setOnLongClickListener(this)`、`findViewById(R.id.button_import).setOnLongClickListener(this)`、`onLongClick` 中 `button_NQ`（`downloadNQRecord(Mode.QQ_LINK)`）與 `button_import`（`importMediaRecords(ASSET_URL_IMPORT_FILE)`）兩個分支；`View.OnLongClickListener` 若再無其他使用者就從 `implements` 移除並刪空的 `onLongClick`。逐一確認：`Mode.QQ_LINK`、`ASSET_URL_IMPORT_FILE`（內建 `assets/url_import.txt`）、`downloadNQRecord` 是否因此成為死碼（有其他呼叫者就保留）。**產品確認（見回報）**：移除後「匯入內建連結（`url_import.txt`）」與「QQ 連結模式」失去入口；預設照使用者決定移除、不另加按鈕。既有 `UrlImportTest` 測的是公開的 `importUrlRecords(...)`（資料層），**不需修改**；`importMediaRecords(...)` 是 private，不要為測試暴露它。若 `MediaConfigRecordsListTest` 等有以長按觸發的案例，改為在 instrumented 測試對 `button_NQ`／`button_import` 執行 `performLongClick()` 並斷言回傳 `false`、且沒有啟動匯入／下載的副作用（`urlImportRunning` 仍為 false、沒有新的 `DownloadManager` job）；**不要使用不存在的 `View.hasOnLongClickListeners()`**。
  `media_config.xml`（CRLF）若有為長按寫的提示文字／`contentDescription`，一併移除。

- [ ] **Step 3：清死碼**

  - `ContentHandler.java`（**CRLF**，小段替換）：從 `enum UrlType` 刪除 `hymnGoogleSearch`、`hymnQqSearch`，並刪 `initWebView` 內對應的兩個 `case`；`mHymnSearch` 仍被 `hymnYoutubeSearch` 使用，**保留**；刪除後不再使用的 `QQRecord` import 一併清；`grep` 確認沒有其他引用（`MediaGuiController.onClick` 只用 `hymnYoutubeSearch`／`hymnNotionSearch`／`hymnBibleTool`）。
  - `HymnToc.java`（**CRLF**）：新增 `ui/toc/TocConstants.kt`（`object TocConstants`，`@JvmField`／`const val` 放 `TOC_TITLE/CATEGORY/STROKE/PINYIN/ENGLISH`、`TOC_ER/XB/XG/YB/BB/DB`、`category_*`、`hymnCategory*`、`hymnTocPage`），把 Step 0 grep 出的所有使用者（`ContentHandler`、`TocBuilder`、`TocFragment`、`AssetHymnTitles`、`HymnNoCh2EngXRef`）改指向它；確認 `HymnToc` 的 `tocToPinyin`／`tocToStroke`／`stroke` 靜態表是否被 `TocBuilder` 重用（若是，一併搬進 `ui/toc/`）；**刪除 `HymnToc.java`**、`HymnTocExpandableListAdapter.java`、`res/layout/hymn_toc.xml`、`hymn_toc_list_group.xml`、`hymn_toc_list_item.xml`（各自 `grep` 零引用後）；`TocBuilderTest`（fixtures 比對）必須維持綠——**測試失敗修實作不改測試**。`HymnNoCh2EngXRef.java`、`ContentHandler.java` 為 CRLF，只換 import／引用行（bytes 改法，通則 §2）。
  - 驗證：`grep -rn 'HymnToc' hymnchtv/src`（**含 `src/test` 與 `src/androidTest`**）無結果（註解例外）；`assembleDebug`／`lint` 無新警告。

- [ ] **Step 4：320dp 小螢幕截字改善**

  - 先建稽核工具：`androidTest/.../ui/EllipsisAudit.kt`——遍歷 view 樹，找出 `TextView` 的 `layout` 有 `getEllipsisCount(line) > 0`，或 `lineCount == maxLines` 且 `layout.getLineEnd(last) < text.length` 者，回傳（畫面、view id、文字）清單。`TruncationAuditTest` 對 **MediaConfig**、**歌詞頁（播放列展開）**、首頁、搜尋頁、目錄頁、設定頁跑稽核，**`assertThat(findings).isEmpty()`**；測試本身與尺寸無關，由協調者在 `wm size 320x640`／字級 1.0 與 1.3 下執行（H3 Step 14 的腳本）。
  - 已知問題與做法：①**`MediaConfig` 按鈕**（`media_config.xml`＋`layout-land/media_config.xml`，CRLF；`button_NQ`／`button_import`／`button_export`／`button_db_records` 等同列按鈕）：改 `maxLines=2`＋`autoSizeTextType=uniform`（`autoSizeMinTextSize=10sp`）或拆成兩列，**不縮到觸控目標 < 48dp**；②**歌詞頁下方按鈕**（`media_player_audio_ui.xml`〔LF〕的 `btn_media/jiaochang/changshi/banzhou` 單選列與 `btn_hymnSearch`）：同法；③**頂列 Media**（`content_lyrics.xml`〔CRLF〕的 `btn_lyrics_media`，英文 "Media"、`c_lyrics_media`）：頂列在 320dp 下 `HorizontalScrollView` 會把它擠出，調整 `btn_next` 的 `maxWidth` 並縮小頂列按鈕內距讓出寬度（英文字串 `Media` 不改），或把 `btn_lyrics_media` 移到溢出選單 `menu_lyrics_more`（若移動，**要在 G5 前確認 spec §4 功能對應表仍有「看得見的入口」**——產品確認項）。
  - 驗收：`TruncationAuditTest` 在 320×640 字級 1.0／1.3 皆空；截圖貼 PR。

- [ ] **Step 5：首頁淺色狀態列圖示偏淡的確認**

  `ui/theme/EdgeToEdge.kt` 只做 `setDecorFitsSystemWindows(false)`；`values/theme.xml`、`values-night/theme.xml` 的 `android:statusBarColor`／`navigationBarColor` 都是透明，圖示明暗由系統依視窗背景自動決定。要確認：`api34b` 與 `api24b`（API 24 沒有 `windowLightStatusBar` 自動判斷，需特別看）上，**淺色主題首頁**（工具列為品牌色 `#09354d` 深藍）狀態列圖示（時間、電量）對比 ≥ 3:1：用 `adb exec-out screencap -p` 取圖，取狀態列區域圖示像素與背景像素算對比（寫成 `tools/` 之外的一次性腳本，不入庫；數值貼 PR）。若偏淡：在 `MainActivity` 主題以 `<item name="android:windowLightStatusBar">false</item>`（深色工具列配淺色圖示）明確指定，或 `WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false`（放進 `EdgeToEdge.enable`，只影響 `MainActivity`）；深色主題同樣確認。結論（偏淡與否、是否修）寫進 PR。

- [ ] **Step 6：全面回歸與 Commit**

  ```bash
  ./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug :hymnchtv:assembleDebugAndroidTest --console=plain
  export ANDROID_SERIAL=emulator-5580; ./gradlew :hymnchtv:connectedDebugAndroidTest --console=plain   # api34b
  export ANDROID_SERIAL=emulator-5582; ./gradlew :hymnchtv:connectedDebugAndroidTest --console=plain   # api24b
  git add -A hymnchtv/src
  git commit -m "refactor: reconcile 1.1 UI with the spec, drop dead code and long-presses, fix 320dp truncation" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```
  `git ls-files --eol` 確認 `ContentHandler.java`、`HymnNoCh2EngXRef.java`、`media_config.xml`、`content_lyrics.xml` 仍為 `w/crlf`。**G5：F2 合併；之後才發 1.1.0（G-DB＋D-1a＋G4＋G5 全數合併後產 1.1.0 APK 並全面檢查）。**

---
## Task H5（**1.1 之後**；設計已定案，F2 與 D-1 不得依賴）：歌詞頁跳轉與回到上一首

> 依據：spec §8（rev 5）。**使用者決定：H5 延後，不進 1.1.0**；與 D-1 UI 的先後順序待定。本 Task 是完整計畫，供之後排程；開工前先確認 G5（1.1.0 已發）與 G-DB 已合併，並 `rebase origin/master`。
> **與 D-1 的整合點（H5 實作時一併處理，D-1 UI 計畫已標記）：** ①D-1 的 `NotebookBarHost.showHymn(key)`「不同本＝開新 `ContentHandler` 並結束」改呼叫 H5 的 `navigateTo(HymnRef)`；②D-1 的 `onPlaybackCompleted()` 記錄「唱過」要改記 `session.ref`（正在播放的那首，不是正在看的）；③「下一首」優先序＝**槽位 ＞ 歌單 ＞ 同本下一首**（D-1 的 `nextInPlaylistOrNextHymn()` 加一個前置判斷）；④槽位相當於單首臨時歌單。
> 播放保證範圍：**只保證 `AudioBgService` 背景音訊**（伴奏／教唱／唱詩音訊）在跳轉時不中斷；影片（ExoPlayer）／YouTube／外部播放器：跳轉前提示「跳轉會停止影片」，確認後釋放再跳轉。

**對照程式碼（C worktree 7985e0c0 實測；行號是約略值，G-DB rebase 後以 `grep -n` 重查）：**
- `ContentHandler.java`（CRLF，1560 行）：`public String mHymnType`、`private int mHymnNo`、`private int hymnIdx`（只在 `onCreate`／`scrollNextHymn()` 更新，**手動滑頁不更新**，所以滑頁後按「下一首」會從舊位置推進——既有 bug，H5d 順手修）、`mAutoStream`（`onStop()` 清除）、`backPressedCallback`（webView → 影片 UI → 音訊播放中停止 → `onUserLeaveHint` 旗標 → `backToHome()`）、`onUserLeaveHint()` 直接呼叫 `backPressedCallback.handleOnBackPressed()`、`onEndOrError()`（`mAutoStream && scrollNextHymn()` → 100ms 後 `startPlay()`）、`getPlayHymn(MediaType, boolean)`（≈L841–1073，讀 `mHymnType/mHymnNo`；**青年分支 ≈L956–961 會改寫 `mHymnType/mHymnNo`**）、`getHymnUri()`（≈L1105，**寫入 `mDir`**，供 `startFileDownload` 用）、`getHymnMediaState()`、`getHymnTitle()`／`getHymnInfo()`（讀兩個欄位並副作用寫 `lyricsPhrase`、`mHymnSearch`）、`onPageSelected` 只更新 `mHymnNo` 再 `updateMediaPlayerInfo()`；manifest 有 `android:configChanges="keyboardHidden|orientation|screenSize"`（旋轉不重建；**深／淺色、語系、字級、閱讀設定變更、程序被殺會重建**）。
- `MyPagerAdapter.java`（CRLF）：`FragmentStateAdapter`，`mHymnType` 是 `final`，`mFragments`（`LongSparseArray`）供 `onLyricsAction` 取目前頁。
- `MediaGuiController.java`（LF）：自己的 `playerState`、`mediaHymns`（`List<Uri>`）、`PLAYER_STATE/INFO/URIS` 存檔；`startPlay()` 在 `mediaHymns` 為空時 `mContentHandler.getPlayHymn(mMediaType, true)`；`MpBroadcastReceiver` 以 `mediaHymns.contains(uri)` 過濾事件，`stop` 事件清 `mediaHymns`、呼叫 `updateMediaPlayerInfo()` 與 `onEndOrError(...)`；`initHymnInfo` **只在 `STATE_STOP` 時更新標題**（所以播放中標題已經凍結在播放的那首）。
- `mediaplayer/AudioBgService.java`（CRLF）：靜態 `uriPlayers`（以 `Uri` 為 key）；action 常數 `ACTION_PLAYER_INIT/START/PAUSE/STOP/SEEK`；廣播 `PLAYBACK_STATE`、`PLAYBACK_STATUS`（每秒），extra 只有 `PLAYBACK_URI/POSITION/DURATION`——**沒有任何 generation**。
- `MediaDownloadHandler.java`（LF）：全 App 單一實例，`initHttpFileDownload(dnLnk, dir, fileName)`；完成時**無條件**（只看 `fileXferUi.isShown() && uiLabel.startsWith(destFName)`）呼叫 `mContentHandler.startPlay()`，而 `startPlay()` 會用**當下正在看的那首**重新 `getPlayHymn`——A 下載中跳到 B，完成後會去播／下載 B。
- `MediaContentHandler.getMediaUris(hymnTable, hymnNo, mediaType, uriList)`（CRLF）：已吃參數，呼叫端傳 `mHymnType/mHymnNo`；`playIfVideo`／`playViaEmbeddedPlayer` 用靜態 `mContentHandler`。
- `content_lyrics.xml`（CRLF，**每頁一份**，`ContentView` 各自 inflate）：頂列 `lyrics_top_bar` 含 `btn_score_color/font_dec/font_inc/share/lyrics_media/next/more`；歌詞外層 `ScrollView`（**沒有 id**）、`lyrics_english`（`WebView`）。所以「跳轉」「返回」不能只放在頁內頂列——返回／正在播放條放 `content_main.xml` 的**單一共用列**，`btn_jump`、`btn_next` 留在頁內頂列。
- `content_main.xml`（CRLF）：`RelativeLayout`；`viewPager` 在 `notebookBar`（空，D-1 用）之上、`mediaPlayer` 之下。

**Files（總覽；各子 Task 再列細項）：** Create `nav/{MediaLinks,YbRefs,HymnSequence,PlaybackSession,ReadingPosition,ContentNavigationState}.kt`、`ui/lyrics/jump/JumpSheetFragment.kt`、`res/layout/sheet_jump.xml`；Modify `ContentHandler.java`、`MediaContentHandler.java`、`MyPagerAdapter.java`、`mediaplayer/AudioBgService.java`（以上 **CRLF**）、`MediaGuiController.java`、`MediaDownloadHandler.java`、`ContentView.java`、`MainActivity.java`（CRLF，只抽 `recordHistory`）、`content_lyrics.xml`、`content_main.xml`（**CRLF**）、`media_player_audio_ui.xml`（LF）、`strings_c.xml`×3。

### Task H5a：媒體路徑盤點與純函式化（前置；**不改行為**）

- [ ] **Step 1：盤點表（寫進 PR 描述；以下是 7985e0c0 的結果，重做 grep 驗證）**

  ```bash
  cd hymnchtv/src/main/java/org/cog/hymnchtv
  grep -nE 'mHymnType|mHymnNo\b|hymnIdx|\.mHymnType' ContentHandler.java MediaGuiController.java MediaDownloadHandler.java MediaContentHandler.java ContentView.java | tr -d '\r'
  ```

  | 位置 | 讀／寫 | 性質 | H5 處理 |
  |---|---|---|---|
  | `onCreate` ≈L312–345、L365 | 寫 `mHymnType/mHymnNo/hymnIdx` | 看（V） | 初始化 `ContentNavigationState.viewing` |
  | `onPageSelected` ≈L714 | 寫 `mHymnNo` | V | `nav.withViewing(...)`（手動滑頁**不推入**返回堆疊） |
  | `updateMediaPlayerInfo` ≈L736 | 讀；`MainActivity.setHymnTypeNo` | V | 保留（分享自動帶入用） |
  | `getPlayHymn` ≈L841–1073 | 讀 | **播放（P）** | 改吃 `HymnRef`（H5a Step 4） |
  | `getPlayHymn` 青年分支 ≈L956–961 | **寫** `mHymnType/mHymnNo` | P（會改寫「看」的狀態） | `YbRefs.resolve(ref)` 回傳目標 `HymnRef`，不改任何狀態 |
  | `getHymnUri` ≈L1105–1190 | 讀、**寫 `mDir`** | P | 吃 `ref`、回傳 `DownloadTarget(dir, fileName, generation)`，不寫欄位 |
  | `startFileDownload` ≈L1087 | 讀 `mDir/mFileName` | P | 吃 `DownloadTarget` |
  | `getHymnTitle`/`getHymnInfo` ≈L1300–1415 | 讀；寫 `lyricsPhrase/mHymnSearch` | P（檔名）＋V（顯示） | 純函式 `hymnInfoOf(ref)`；`lyricsPhrase` 改為回傳值的一部分 |
  | `getHymnMediaState` ≈L1234 | 讀 | V | 吃 `ref`（`updateMediaPlayerInfo` 傳 viewing；播放中標題凍結的既有行為不變） |
  | `lyricsShare`、`media_config` 動作、`getMediaUrl`、`getMediaRecord()` ≈L564–699、L1514 | 讀 | V | 用 `viewing` |
  | `hymnType2Text` ≈L1520 | 讀 | V（連播確認文字） | 吃 `ref` |
  | `scrollNextHymn` ≈L1524 | 讀 `hymnIdx`（過期） | 導覽（N） | 改用 `mPager.getCurrentItem()+1`；只做 viewing |
  | `da_link_test` ≈L804 | 寫 `mHymnNo` | 除錯用 | 改為迴圈 `HymnRef`，不改狀態 |
  | `MediaGuiController.startPlay` ≈L397 `mContentHandler.mHymnType` | 讀 | P | 改讀 session 的書別 |
  | `MediaDownloadHandler` ≈L237/249/270 | 無參回呼 | P | 帶 generation（H5c） |
  | `AudioBgService` 全部 action／廣播 | 以 `Uri` 為鍵 | P | 帶 generation（H5c） |

- [ ] **Step 2：特徵化測試（在**舊程式碼**上先建立基準，TDD 的 red-before-refactor）**

  `androidTest/.../playback/PlaybackLinkCharacterizationTest.kt`：對固定樣本（每本的首／中／末號、大本附 781、青年 276/277、數個有 `ybXTable` 對照的青年號、跨 category 邊界號，約 24 筆）逐筆 `ActivityScenario.launch(ContentHandler, intent(type,no))`，`onActivity` 取 `getHymnInfo()` 與 `getHymnUri()`，輸出 `book\tstoredNo\tinfo\turi` 到 logcat 與 `files/` 目錄；協調者在 `api34b` 跑一次並 `adb pull` 成 **`hymnchtv/src/androidTest/assets/playback_fixtures/media_links.tsv`**（Commit 它）。之後同一個測試改為「讀 fixture 逐行斷言」（重構後必須仍相等）。

- [ ] **Step 3：純函式（TDD；`nav/`，JVM）**

  - `YbRefs(table: Map<Int, String> = YbCrossRef.TABLE).resolve(ref: HymnRef): HymnRef`：非青年或沒有對照 → 原 `ref`；有對照（如 `"bb876"`）→ `HymnRef(MainActivity.getHymnType(...)的等價, 876)`（以兩字母前綴對應 `HymnTypes`，不呼叫 `MainActivity`）。測試用合成表與真實 `YbCrossRef` 資產各一。
  - `HymnSequence.next(ref): HymnRef?`：以 `HymnNumberRules.storedNumbers(book)` 的下一個號碼（跳過缺號，如 XG 34、BB 37→101）；末尾回 null；`HymnRef(BB, 2000)`（無中文對照的 dummy）回 null。**注意：這是「播放序列」，不等於 pager 頁序（pager 含 XG 34 這種空頁）**；`scrollNextHymn()` 仍依 pager 頁序。測試：BB 37→101、ER 17→101、XB 167→171、DB 780→781（附 1 緊接在後；與 pager 一致）、YB 275→276、末號→null。
  - `MediaLinks`：`titleFromInfo(info)`（`getHymnTitle()` 的演算法：`split(":\\s|？|（")[1]`、去標點 `[，、‘’！：；。？]`、去類別前綴）、`bibleToolUrl(ref, title): String?`（`getHymnUri` 的 `switch`：ER／XB／XG／YB／BB／DB 六支，含 `category_*` 查表與 DB 附 `DF` 前綴、`DB_Links` 例外）、`changshiDownloadLink(ref, title): String?`、`mediaDir(book, mediaType)`。`MediaLinksTest` 讀 Step 2 的 fixture，逐行斷言 `bibleToolUrl(HymnRef, titleFromInfo(info)) == uri`；另對各本 `changshiDownloadLink` 以手寫期望值（讀舊碼 `String.format` 得出）各斷言一筆。

- [ ] **Step 4：`ContentHandler`／`MediaGuiController` 改吃 `HymnRef`（CRLF 檔只做替換，不整檔重排）**

  `getPlayHymn(HymnRef ref, MediaType type, boolean download)`、`getHymnUri(HymnRef)`、`hymnInfoOf(HymnRef)`、`hymnMediaStateOf(HymnRef)`；舊無參版本改為薄包裝（呼叫 viewing 版），H5d 再移除；`mDir/mFileName` 欄位換成區域變數＋`DownloadTarget`（暫存在 `mPendingDownload`，由 `startFileDownload` 使用）；青年分支用 `YbRefs`。`MediaGuiController` 暫時仍取 viewing。

- [ ] **Step 5：驗證與 Commit**

  `PlaybackLinkCharacterizationTest`（讀 fixture）、`SmokeFlowTest`、`LyricsTopBarTest`、`ContentHandlerReadingTest`、`MediaConfig*`、`MediaLinksTest`、`HymnSequenceTest` 全綠（`api34b`＋`api24b`）。

  ```bash
  git commit -m "refactor: pass hymn refs through the media path and extract link/sequence logic" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

### Task H5b：導覽與播放狀態模型（純 Kotlin、不可變、可序列化）

**Files：** Create `nav/PlaybackSession.kt`、`nav/ReadingPosition.kt`、`nav/ContentNavigationState.kt`；Test `test/.../nav/ContentNavigationStateTest.kt`

> 不引入 `kotlin-parcelize`（build.gradle 沒有此外掛，且不為此加）；狀態以**字串**序列化存進 `Bundle.putString`，JVM 可測。

- [ ] **Step 1：測試先行**

  ```kotlin
  class ContentNavigationStateTest {
      private val a = HymnRef(HymnTypes.DB, 45); private val b = HymnRef(HymnTypes.XG, 12); private val c = HymnRef(HymnTypes.BB, 7)
      private val pos = ReadingPosition("SCORE_AND_LYRICS", 400, null)
      private val seq = HymnSequence()

      @Test fun generationsIncreaseAndStaleOnesAreRejected() {
          val s1 = ContentNavigationState(viewing = a).beginSession(a, 3)
          val s2 = s1.beginSession(b, 3)
          assertThat(s2.session!!.generation).isGreaterThan(s1.session!!.generation)
          assertThat(s2.isCurrent(s1.session!!.generation)).isFalse()
          assertThat(s2.clearSession(s1.session!!.generation)).isEqualTo(s2)      // 過期的清除要求被丟棄
      }
      @Test fun advanceUsesTheSessionHymnNotTheViewedOne() {
          val s = ContentNavigationState(viewing = b).beginSession(a, 3)          // 在放 A，正在看 B
          val adv = s.advance(seq)!!
          assertThat(adv.target).isEqualTo(HymnRef(HymnTypes.DB, 46))
          assertThat(adv.state.viewing).isEqualTo(b)                              // 看的不動
          assertThat(adv.viewingFollows).isFalse()
      }
      @Test fun advanceFollowsViewingOnlyWhenViewingTheSessionHymn() {
          val adv = ContentNavigationState(viewing = a).beginSession(a, 3).advance(seq)!!
          assertThat(adv.state.viewing).isEqualTo(HymnRef(HymnTypes.DB, 46)); assertThat(adv.viewingFollows).isTrue()
      }
      @Test fun slotWinsOverSequenceAndIsCleared() {
          val adv = ContentNavigationState(viewing = a).beginSession(a, 3).setNextSlot(c).advance(seq)!!
          assertThat(adv.target).isEqualTo(c); assertThat(adv.usedSlot).isTrue(); assertThat(adv.state.nextSlot).isNull()
      }
      @Test fun advanceWithoutSessionOrAtTheEndIsNull() { /* 沒有 session → null；DB 786（末號）→ null */ }
      @Test fun returnStackIsCappedAtTenAndPopsNewestFirst() { /* push 11 筆 → size 10 且最舊被丟；pop 回傳最新；popReturnTo(i) 一次彈到第 i 筆 */ }
      @Test fun pushingTheSameHymnTwiceKeepsOneEntryWithTheLatestPosition() { /* 避免連按產生重複 */ }
      @Test fun encodeDecodeRoundTrip() {
          val s = ContentNavigationState(viewing = HymnRef(HymnTypes.BB, 2000)).beginSession(a, 2)
              .withSessionUris(1, listOf("file:///x/a|b;c.mp3", "https://h/p?q=1&r=二")).setNextSlot(b)
              .pushReturn(ReturnEntry(a, pos)).pushReturn(ReturnEntry(c, ReadingPosition("LYRICS_ONLY", 0, 120)))
          assertThat(ContentNavigationState.decode(s.encode())).isEqualTo(s)
      }
      @Test fun corruptOrUnknownVersionDecodesToNull() { listOf(null, "", "v9|x", "v1|garbage", "v1|hymn_db:abc").forEach { assertThat(ContentNavigationState.decode(it)).isNull() } }
  }
  ```

- [ ] **Step 2：實作**

  `PlaybackSession(ref, mediaTypeValue: Int, uris: List<String>, generation: Int)`（`mediaTypeValue` 用 `MediaType.getValue()`，避免 JVM 測試載入 `MediaType` 的 Context 依賴）；`ReadingPosition(displayMode: String, outerScrollY: Int, webViewScrollY: Int?)`；`ReturnEntry(ref, position)`；`ContentNavigationState(viewing, session, nextSlot, returnStack, lastGeneration)`：全為 `data class`＋回傳新物件的方法（`withViewing`、`beginSession`、`withSessionUris`、`clearSession(generation)`、`isCurrent`、`setNextSlot`、`pushReturn`〔上限 `MAX_STACK=10`，頂端同首則取代位置〕、`popReturn`、`popReturnTo(index)`、`advance(sequence): Advance?`）；`Advance(state, target, viewingFollows, usedSlot)`；`encode()`＝`v1|…`，欄位以 `|` 分隔、清單以 `;`、子欄位以 `,`，**所有自由字串（uri、displayMode）以 `URLEncoder.encode(UTF-8)` 編碼**；`decode(String?)` 任何格式錯誤回 `null`（呼叫端退回由 intent 重建新狀態，不崩潰）。
  `advance` 規則：目標＝`nextSlot ?: sequence.next(session.ref)`；新 session 帶新 generation、`uris` 清空、`mediaTypeValue` 沿用；`nextSlot` 清空；`viewing == 舊 session.ref` 時 viewing 跟著前進，否則不動。

- [ ] **Step 3：Commit** — `git commit -m "feat: immutable navigation and playback session state for the lyrics page"`（附 Co-Authored-By）。

### Task H5c：generation 貫穿服務、下載與廣播（修「A 下載中跳到 B」）

**Files：** Modify `mediaplayer/AudioBgService.java`（CRLF）、`MediaGuiController.java`、`MediaDownloadHandler.java`、`ContentHandler.java`（CRLF，新增少量方法）；Test `androidTest/.../playback/StaleGenerationTest.kt`

- [ ] **Step 1：`AudioBgService`（CRLF，以 bytes 小段修改）**

  新增 `public static final String PLAYBACK_GENERATION = "playback_generation";` 與 `private static final Map<Uri, Integer> uriGenerations = new ConcurrentHashMap<>();`。`onStartCommand` 的 `ACTION_PLAYER_INIT`／`START` 讀 `intent.getIntExtra(PLAYBACK_GENERATION, -1)` 並 `uriGenerations.put(uri, gen)`（`gen != -1`）；`playerRelease(uri)` 時 `remove`。**所有**廣播（`playbackState()` 與每秒的 `PLAYBACK_STATUS` runnable）加上 `PLAYBACK_GENERATION`（查不到用 `-1`，不使用 `getOrDefault` 以外的新 API；API 24 可用但保持保守寫法 `Integer g = map.get(uri)`）。

- [ ] **Step 2：`MediaGuiController`**

  - `startPlay()`：`mediaHymns` 為空時 `int gen = mContentHandler.beginPlaybackSession(mMediaType)`，`HymnRef ref = mContentHandler.getSessionRef()`，`mediaHymns = mContentHandler.getPlayHymn(ref, mMediaType, true, gen)`；`mContentHandler.onSessionUris(gen, mediaHymns)`；L397 的 `HYMN_DB.equals(mContentHandler.mHymnType)` 改讀 `ref.getBook()`。
  - 所有送給服務的 intent（`playerInit/playerStop/playStart/playerSeek`）加 `putExtra(AudioBgService.PLAYBACK_GENERATION, mGeneration)`（`mGeneration` 是本次 `startPlay` 取得的值，Activity 重建後由 `ContentHandler.currentGeneration()` 恢復）。
  - `MpBroadcastReceiver.onReceive`：取 `gen = intent.getIntExtra(PLAYBACK_GENERATION, -1)`，`if (!mContentHandler.isCurrentGeneration(gen)) { Timber.d("drop stale %s", gen); return; }`；`stop` 事件（現行 gen）→ `mContentHandler.onPlaybackStopped(gen)`（見 H5f）。過期事件**完全丟棄**，不改 `playerState`、不呼叫 `onEndOrError`。

- [ ] **Step 3：`MediaDownloadHandler`（LF）**

  `initHttpFileDownload(dnLnk, dir, fileName, int generation)`；新增 `private final static Map<Long, Integer> jobGenerations`；`DownloadReceiver` 成功分支把 `mContentHandler.startPlay()` 換成 `mContentHandler.onDownloadFinished(gen)`；`onError(statusText)`／失敗分支的 `showBibleToolHymnal()` 也只在 `isCurrentGeneration(gen)` 時執行；`onDownloadFinished(gen)` 在 `ContentHandler` 內：現行 → `startPlay()`（因 session 已存在且 `mediaHymns` 空，會以 session 的 ref 重取，下載好的檔案被 `isFileExist` 找到）；過期 → 只 `Timber.d`，**不播放、不再下載**。`DownloadOwnership`（`DownloadOwnership.owns`）的 App-update 保護邏輯不得更動。

- [ ] **Step 4：`ContentHandler`（CRLF）新增**：`int beginPlaybackSession(MediaType)`、`HymnRef getSessionRef()`、`boolean isCurrentGeneration(int)`、`int currentGeneration()`、`void onSessionUris(int, List<Uri>)`、`void onDownloadFinished(int)`、`void onPlaybackStopped(int)`（骨架；狀態欄位在 H5d 才接 `ContentNavigationState`，此 Step 先用一個 `private int mGeneration` 與 `HymnRef mSessionRef` 暫存，H5d 換成 `nav`）。

- [ ] **Step 5：測試 `StaleGenerationTest`（instrumented，`api34b`＋`api24b`）**

  ①開 session（gen1，A）→ `beginPlaybackSession` 取代為 gen2（B）→ 以 `LocalBroadcastManager` 注入 `PLAYBACK_STATE=stop`、`PLAYBACK_URI=A 的 uri`、`gen=1` → 斷言 B 的 session 仍在、`mAutoStream` 沒被清、沒有 `onEndOrError`；②A 下載中（用 `MediaDownloadHandler` 的測試入口模擬 `onDownloadFinished(1)`）而 session 已是 B（gen2）→ **不呼叫 `startPlay`、不呼叫 `initHttpFileDownload`**；③現行 gen 的事件仍正常驅動 `playerState`。有 raw midi 的詩（`HymnsApp.getFileResId(MIDI_DB + no, "raw") != 0`）可做不需網路的實播驗證。

- [ ] **Step 6：Commit** — `git commit -m "fix: tag playback service, broadcasts and downloads with a generation so stale events are dropped"`。

### Task H5d：`ContentHandler` 導覽狀態、`navigateTo`、同頁換本、閱讀位置

**Files：** Modify `ContentHandler.java`、`MyPagerAdapter.java`、`MainActivity.java`（CRLF，只抽 `recordHistory`）、`ContentView.java`（LF）、`content_lyrics.xml`（CRLF，只加一個 id）；Test `androidTest/.../ui/lyrics/NavigateToTest.kt`

- [ ] **Step 1：`MainActivity.recordHistory(Context, String, int)`**（CRLF，小改）：把 `showContent` 內「寫歷史」的 `AppExecutors.io("store-history", …)` 區塊原樣抽成 `public static void recordHistory(Context ctx, String hymnType, int hymnNo)`（含 `HYMN_BB_DUMMY` 判斷與 `MediaRecord.isFu`），`showContent` 呼叫它。**行為不變**；`navigateTo` 也呼叫它（開啟時仍寫歷史）。

- [ ] **Step 2：狀態欄位換成 `ContentNavigationState nav`**：刪除 `mHymnType`／`mHymnNo`／`hymnIdx` 欄位與 H5c 的暫存欄位；`getHymnNo()`、`getHymnType()`（若有外部使用者）由 `nav.viewing` 推導；`onCreate`：**先**以 `ContentNavigationState.decode(savedInstanceState.getString(STATE_NAV))` 還原（失敗或沒有 → 由 intent 建立 `ContentNavigationState(viewing = HymnRef(type, no))`），**再**以 `nav.viewing.book` 建 `MyPagerAdapter`（否則「跳到別本後重建」會用 intent 的原書本建 adapter，頁面錯位）；`onSaveInstanceState` 加 `STATE_NAV`。`onPageSelected` → `nav = nav.withViewing(...)`（不推返回堆疊）。`scrollNextHymn()` 改用 `mPager.getCurrentItem() + 1`（≤ `getItemCount()-1` 且 `HymnIdx2NoConvert...[0] != -1`），**只做 viewing 導覽**。

- [ ] **Step 3：spike 先行——換本用「換 adapter」是否洩漏／錯頁**

  `NavigateToSpikeTest`（`api24b`）：連跳 20 次不同本，每次 `idle` 後斷言 `supportFragmentManager.fragments.count { it is ContentView } ≤ 3`、舊本的 `ContentView` 不在 FragmentManager、`Runtime` 已用記憶體在 `System.gc()` 後成長 < 8MB。
  - **做法 A（預設）**：`mPagerAdapter = new MyPagerAdapter(this, ref.getBook()); mPager.setAdapter(mPagerAdapter); mPager.setCurrentItem(idx, false);`（換 adapter 時 RecyclerView 會回收所有 ViewHolder，`FragmentStateAdapter.onViewRecycled` 移除舊 Fragment；`registerOnPageChangeCallback` 掛在 `ViewPager2` 上，換 adapter 後仍有效）。
  - **做法 B（spike 失敗時）**：`MyPagerAdapter` 改為可變 `setHymnType()`；覆寫 `getItemId(position) = (bookOrdinal.toLong() shl 20) or position`、`containsItem(id)`；清空 `mFragments` 後 `notifyDataSetChanged()`。

- [ ] **Step 4：`navigateTo(HymnRef ref)`**

  ```java
  /** Shows [ref] in place: same book scrolls the pager, another book swaps the adapter. Never restarts the activity and never stops playback. */
  public void navigateTo(HymnRef ref) {
      boolean sameBook = ref.getBook().equals(nav.getViewing().getBook());
      int idx = HymnNo2IdxConvert.hymnNo2IdxConvert(ref.getBook(), ref.getStoredNo());
      nav = nav.withViewing(ref);
      if (!sameBook) {
          mPagerAdapter = new MyPagerAdapter(this, ref.getBook());
          mPager.setAdapter(mPagerAdapter);
      }
      mPager.setCurrentItem(idx != -1 ? idx : ref.getStoredNo(), false);
      MainActivity.recordHistory(this, ref.getBook(), ref.getStoredNo());   // 開啟時仍寫歷史（背景執行緒）
      updateMediaPlayerInfo();
      refreshNavBars();   // H5e/H5f
  }
  ```
  英文來源帶英文號（`MainActivity.showContent(..., engNo)`）時，`mHymnNoEng`／`mAutoEnglish` 由呼叫端（H5g）在 `navigateTo` 前設定。`HYMN_BB_DUMMY`（2000）沿用既有的 `else mPager.setCurrentItem(mHymnNo)` 邏輯（頁面是「無中文對應」的空頁）。

- [ ] **Step 5：`ReadingPosition` 擷取與還原**

  `content_lyrics.xml`（CRLF，只加一個屬性）：歌詞外層 `ScrollView` 加 `android:id="@+id/lyrics_scroll"`。`ContentView`（LF）：`ReadingPosition captureReadingPosition()`（`currentDisplayMode().name`、`lyrics_scroll.getScrollY()`、`lyricsEnglish` 可見時 `lyricsEnglish.getScrollY()` 否則 null）；`void restoreReadingPosition(ReadingPosition p)`：先 `ContentHandler.displayModeOverride`（若 `p.displayMode` 與目前有效模式不同才設，並 `applyDisplayMode`），再 `lyrics_scroll.doOnLayout { scrollTo(0, p.outerScrollY) }`（`androidx.core.view.doOnLayout`；英文模式另 `lyricsEnglish.post { scrollTo(0, p.webViewScrollY) }`）。`ContentHandler.navigateTo` 後若有待還原位置：目標頁 Fragment 尚未建立時存 `mPendingPosition`，`ContentView.onResume()` 在「自己是目前頁」時向 `ContentHandler.takePendingPosition()` 取走並還原。

- [ ] **Step 6：測試 `NavigateToTest`**

  ①同本：`navigateTo` 後 pager 的 `currentItem`、`viewing`、歷史多一筆（`AppExecutors.io` 讀）；②換本：adapter 類別的書別、頁面內容是新本；**青年 276（附 1）**：歷史顯示「青年 附1」；③換本期間背景音訊（有 midi 的詩）**不中斷**（`playerState` 仍 PLAY、service 仍在）；④跳到別本後 `scenario.recreate()`：viewing、頁面、`nav` 都保留；⑤捲動 400px → `captureReadingPosition` → 離開再還原，`scrollY` 誤差 ≤ 2px；英文 WebView 模式另存 WebView Y；⑥`scrollNextHymn()` 在手動滑頁後從「目前頁」推進（修舊 bug）；⑦`decode` 壞字串時退回 intent 狀態不崩潰。

- [ ] **Step 7：Commit** — `git commit -m "feat: navigate between hymns in place and keep reading positions"`。

### Task H5e：返回堆疊、返回鍵優先序、影片提示

**Files：** Modify `ContentHandler.java`、`ContentView.java`、`content_main.xml`（CRLF）、`strings_c.xml`×3；Test `androidTest/.../ui/lyrics/JumpReturnTest.kt`

- [ ] **Step 1：共用導覽列（`content_main.xml`，CRLF，以 bytes 插入）**：在 `viewPager` 之上加 `LinearLayout id=navStrip`（`alignParentTop`、水平、`visibility=gone`），內含 `Button btn_return`（文字「← 補45」，箭頭用 `drawableStart` vector；`maxLines=1`、`ellipsize=end`、`minHeight=48dp`）與 `Button btn_playing`（H5f 用）；`viewPager` 改 `layout_below=@id/navStrip`（原本 `alignParentTop` 的屬性保留給無 strip 時：用 `layout_below` 指向 `gone` 的 view 時高度為 0，行為等同置頂）。

- [ ] **Step 2：`jumpTo(HymnRef)`、`returnToPrevious()`、`returnTo(int)`、`refreshNavBars()`**

  ```java
  public void jumpTo(HymnRef ref) {
      if (ref.equals(nav.getViewing())) return;
      Runnable go = () -> {
          nav = nav.pushReturn(new ReturnEntry(nav.getViewing(), captureCurrentPosition()));
          navigateTo(ref);
      };
      if (isMediaPlayerUi) confirmStopVideo(() -> { releaseVideoPlayerUi(); go.run(); }); else go.run();
  }
  ```
  `releaseVideoPlayerUi()`＝把 `backPressedCallback` 內「影片播放器可見時 release、還原 `MediaGuiController` UI」那段原樣抽出（不改行為）；`confirmStopVideo` 用 `MaterialAlertDialogBuilder`（`c_jump_stops_video`）。`returnToPrevious()`：`popReturn()` → `navigateTo(entry.ref)` ＋還原位置（`mPendingPosition`）；**不推入堆疊**。`returnTo(index)`＝`popReturnTo(index)`。手動左右滑頁**不**推入。`refreshNavBars()`：`btn_return` 顯示「← 」＋頂端 entry 的短標籤（`HymnLabels.chip`），堆疊為空時整列（若 `btn_playing` 也不顯示）`gone`。

- [ ] **Step 3：返回鍵優先序（`backPressedCallback` 重排；`onUserLeaveHint` 不 pop）**

  (1) 跳轉面板／搜尋疊頁：由 `BottomSheetDialogFragment` 自己的對話框先消費返回鍵（H5g），不進 Activity callback；(2) `mWebView.isShown()` → 隱藏（`WebViewFragment` 內部 `canGoBack`〔≈L234〕的 callback 註冊在 dispatcher，後註冊先執行，維持現狀）；**(3) 返回堆疊非空且不是 Home 鍵觸發 → `returnToPrevious()`**；(4) 既有：影片 UI → 音訊播放中停止 → `onUserLeaveHint` 旗標 → `backToHome()`。`onUserLeaveHint()` 設旗標後仍呼叫同一個 handler，但 (3) 的條件含 `!onUserLeaveHint`，所以**按 Home 維持既有停止邏輯、不 pop**。

- [ ] **Step 4：長按清單**：`btn_return` 長按 → `PopupMenu` 列出整串（最新在上，每列 `HymnLabels.headline`），選第 i 列 → `returnTo(i)`；另在 H5g 的跳轉面板提供同一份清單的**可見**版本（不依賴長按）。

- [ ] **Step 5：字串（三語系，不用 `←` 字元，用 vector 圖示）**：`c_jump_return`（en `Back to %1$s` / zh `返回 %1$s` / hant `返回 %1$s`）、`c_jump_stops_video`（`Jumping will stop the video. Continue?` / `跳转会停止视频，要继续吗？` / `跳轉會停止影片，要繼續嗎？`）、`c_jump_stack_title`（`Recent jumps` / `最近跳转` / `最近跳轉`）；跑 `FontSubsetTest`。

- [ ] **Step 6：測試 `JumpReturnTest`**：①連跳 3 首（A→B→C→D）按返回逐層回 C、B、A，每層捲動位置還原；②堆疊上限 10（跳 12 次只留最近 10 筆）；③手動滑頁不推入；④**返回鍵優先序表**：面板開啟只關面板；堆疊非空＋音訊播放中 → 先 pop、播放**不被停止**；堆疊空＋播放中 → 停止播放；堆疊空且沒播放 → 離開；⑤**按 Home（`onUserLeaveHint`）時堆疊原樣保留**、播放依既有邏輯停止；⑥影片 UI 顯示（`showMediaPlayerUi()`）時 `jumpTo` 先出現確認對話框，取消不跳、確認後釋放再跳；⑦旋轉（`configChanges` 不重建）與 `recreate()` 後堆疊保留；⑧離開歌詞頁再開，堆疊為空。

- [ ] **Step 7：Commit** — `git commit -m "feat: return stack, back-key priority and video jump prompt on the lyrics page"`。

### Task H5f：下一首槽位、`advancePlayback`、正在播放條、改播此首

**Files：** Modify `ContentHandler.java`、`MediaGuiController.java`、`ContentView.java`、`content_lyrics.xml`／`content_main.xml`（CRLF）、`media_player_audio_ui.xml`（LF）、`strings_c.xml`×3；Test `androidTest/.../ui/lyrics/PlaybackSessionTest.kt`

- [ ] **Step 1：session 接上 `nav`**：H5c 的暫存欄位換成 `nav.session`：`beginPlaybackSession(MediaType)` ＝ `nav = nav.beginSession(nav.viewing, type.getValue())`（只在 `playerState == STOP` 的 `startPlay` 進入；暫停→繼續不建新 session）；`onSessionUris` → `nav.withSessionUris`；`onPlaybackStopped(gen)`：現行 gen 且非自動連播推進中 → `nav = nav.clearSession(gen)`；`refreshNavBars()`。

- [ ] **Step 2：`switchPlayback(HymnRef ref)`（「改播此首」與「下一首」在播放中使用）**：`mMediaGuiController.stopPlay()`（用舊 generation 送 stop）→ `nav = nav.beginSession(ref, type)` → `mMediaGuiController.resetForNewSession()`（新增：`mediaHymns.clear()`、`playerState = STATE_STOP`、更新標題）→ `startPlay()`。舊 session 的 `stop` 事件因 generation 過期被丟棄（H5c），**不會**觸發連播。

- [ ] **Step 3：`advancePlayback()`（取代 `onEndOrError` 自動連播裏的 `scrollNextHymn()`）**

  ```java
  private boolean advancePlayback() {
      Advance adv = nav.advance(new HymnSequence());
      if (adv == null) return false;           // 沒有 session、或已是本末尾且沒有槽位
      nav = adv.getState();
      if (adv.getViewingFollows()) {           // 看的就是剛播完的那首 → 跟著前進；否則 viewing 不動
          navigateTo(adv.getTarget());         // 同本會平滑捲動；不推返回堆疊
      }
      refreshNavBars();
      return true;
  }
  ```
  `onEndOrError`：`if (mAutoStream && advancePlayback())` → 釋放影片 UI（沿用現有 `isMediaPlayerUi` 區塊）→ `postDelayed(this::startPlay, 100)`；`startPlay()` 因 session 已換、`mediaHymns` 為空，會以**新 session 的 ref** 取媒體。失敗與手動停仍 `setAutoStream(false)`。**自動連播的下一首＝session.ref 的下一首（不是正在看的那首的下一首）；有槽位優先播槽位並清空。**

- [ ] **Step 4：槽位與「下一首」鈕**：`setNextSlot(HymnRef)`／`clearNextSlot()`；`nextLabel()`＝槽位存在時 `c_jump_next_label`（「下一首：新詩12」），否則 `c_next_hymn`；`ContentView` 在 `onResume` 與 `refreshNavBars()` 時更新 `btn_next` 文字與 `contentDescription`（頂列空間有限：`maxWidth=140dp`、`maxLines=1`、`ellipsize=end`，完整文字放 `contentDescription`）。`ContentHandler.onNextPressed()`（`ContentView` 的 `btn_next` click 改呼叫它）：有槽位 → 清槽位、`navigateTo(slot)`（**不推返回堆疊**）、`if (nav.session != null && 播放中) switchPlayback(slot)`；沒有槽位 → 既有 `scrollNextHymn()`（H5 之前的行為不變；D-1 的 `nextInPlaylistOrNextHymn()` 在此之前加歌單判斷，優先序「槽位 ＞ 歌單 ＞ 同本」）。

- [ ] **Step 5：「正在播放」條與「改播此首」**：`refreshNavBars()`：`session != null && viewing != session.ref` → `btn_playing` 顯示「正在播放：補45」＋箭頭圖示；點擊：若返回堆疊頂端就是 `session.ref` → `returnToPrevious()`，否則 `jumpTo`（推入目前頁，讓使用者可再回來）。`media_player_audio_ui.xml`（LF）在播放鍵區加 `Button btn_play_this`（「改播此首」，`gone`；同樣條件顯示，點擊 → `switchPlayback(viewing)`）。播放列標題維持 `initHymnInfo` 的既有行為（播放中凍結在播放的那首）。

- [ ] **Step 6：字串**：`c_jump_next_label`（`Next: %1$s` / `下一首：%1$s` / `下一首：%1$s`）、`c_jump_playing`（`Playing: %1$s` / `正在播放：%1$s` / `正在播放：%1$s`）、`c_jump_play_this`（`Play this one` / `改播此首` / `改播此首`）。

- [ ] **Step 7：測試 `PlaybackSessionTest`（`api24b` 全套＋`api34b`；用有 raw midi 的詩避免網路）**

  ①播 A、看 B、A 結束且自動連播 → 播 **A+1**、viewing 仍在 B；②自動連播＋槽位 → 播槽位、槽位清空，viewing 只有在原本看 A 時才跟著；③A 下載中跳到 B、下載完成 → **不播 B、不下載 B**，session A 仍有效（H5c 的整合版）；④青年交叉參照詩播放後 `viewing` 不變（舊碼會被改寫）；⑤「改播此首」：新 generation、舊 stop 被丟棄、播放列與條都指向新首；⑥設為下一首後按「下一首」：有播放→改播、無播放→只跳轉；⑦沒有槽位時「下一首」行為同現況；⑧**`recreate()`**：播放中重建後背景音訊持續、`btn_playing`／session／槽位／返回堆疊都在、過期 generation 的事件被丟棄；⑨旋轉（不重建）不影響。

- [ ] **Step 8：Commit** — `git commit -m "feat: next-hymn slot, session-based auto-play and playing strip on the lyrics page"`。

### Task H5g：跳轉面板（`HymnPicker` 重用）與頂列

**Files：** Create `ui/lyrics/jump/JumpSheetFragment.kt`、`res/layout/sheet_jump.xml`；Modify `ContentHandler.java`、`ContentView.java`、`content_lyrics.xml`（CRLF）、`ui/search/SearchFragment.kt`（加 `SearchHost`）、`ui/picker/HymnPickerController.kt`、`strings_c.xml`×3；Test `androidTest/.../ui/lyrics/JumpSheetTest.kt`

- [ ] **Step 1：頂列加「跳轉」（`content_lyrics.xml`，CRLF）**：在 `btn_next` 之前加 `Button btn_jump`（`c_jump`：en `Jump` / zh `跳转` / hant `跳轉`；與 `btn_next` 同樣式、`minHeight=48dp`，放在捲動區**之外**以免被捲走；320dp 寬度檢查在 H5h，必要時縮 `btn_next` 的 `maxWidth`）。`ContentView` 把 `btn_jump` 加進 click 清單 → `mContentHandler.showJumpSheet()`。

- [ ] **Step 2：`JumpSheetFragment : BottomSheetDialogFragment`**：`sheet_jump.xml` ＝ `<include layout="@layout/hymn_picker"/>`（H3 的同一份，含橫向版）＋ 「最近跳轉」清單區（`jump_stack_list`，來自 `nav.returnStack`，點一列 → `returnTo(i)`；為長按清單的**可見**版本）＋ 目前槽位列（「下一首：新詩12」，附「清除」）。以 `HymnPickerController(mode = JUMP)`：`PickerChrome` 已隱藏 `btn_toc`／`btn_recent_more`／`btn_add_playlist`，顯示 `btn_set_next`；**初始來源＝目前 `viewing.book`**（不是 `LastHymnType`，也不寫回偏好）。host 介面 `JumpHost`（`ContentHandler` 實作）：`onJumpOpen(ref, engNo)`→`jumpTo`（英文來源帶 `engNo` 時先設 `mHymnNoEng`／`mAutoEnglish`）、`onJumpSetNext(ref)`→`setNextSlot`＋關閉面板；「開啟」＝切過去看，「設為下一首」＝留在原頁只排隊。**不放「＋歌單」**。
- [ ] **Step 3：搜尋入口**：`HymnPickerController.openSearch` 在面板內以 `childFragmentManager` 疊 `SearchFragment.newInstance(book)`；H4 的 `SearchFragment` 結果點擊改為「`(parentFragment as? SearchHost ?: activity as? SearchHost)?.onResultChosen(ref)`，沒有 host 時才走預設的 `MainActivity.showContent`」（`SearchHost` 由 `JumpSheetFragment` 實作，轉成 `onJumpOpen`）。面板的 `OnBackPressedCallback`（`dialog.onBackPressedDispatcher.addCallback`）先 pop 子搜尋頁、再關面板——即返回鍵優先序第 (1) 層。
- [ ] **Step 4：無障礙**：面板有標題（`c_jump`）、焦點順序同首頁、`bottomSheet` 預設展開；`btn_jump` 的 `contentDescription`。
- [ ] **Step 5：測試 `JumpSheetTest`**：①開面板、選補充本 `4`、`5`、「開啟」→ 歌詞頁換成補充 45、返回堆疊多一筆；②「設為下一首」→ 面板關閉、`btn_next` 文字變「下一首：…」、原頁不動；③面板內搜尋 → 點結果 → 跳轉；返回鍵先關搜尋再關面板；④面板的 `btn_toc`／`btn_add_playlist`／`btn_recent_more` 不可見；⑤`UiFlags.NOTEBOOK_UI_ENABLED` 的任一值面板都不顯示「＋歌單」；⑥旋轉／`recreate()` 後面板關閉但 `nav` 保留。
- [ ] **Step 6：Commit** — `git commit -m "feat: jump sheet reusing the hymn picker, with search and next-slot"`。

### Task H5h：整合驗證（協調者在 `api24b` 與 `api34b`）

- [ ] **Step 1：全套 instrumented（兩台各一次，**`api24b` 必跑**：heap 48MB 下的播放與重建）**

  ```bash
  for s in emulator-5582 emulator-5580; do ANDROID_SERIAL=$s ./gradlew :hymnchtv:connectedDebugAndroidTest --console=plain; done
  ```
- [ ] **Step 2：記憶體與洩漏**：`api24b` 上連續 20 次跨本跳轉＋3 次 `recreate()`，`adb shell dumpsys meminfo com.ziontkec.hymnal` 前後比較；`MainActivityLeakTest`／`NavigateToSpikeTest` 綠；`ContentView` 實例不累積。
- [ ] **Step 3：320dp 檢查**：`wm size 320x640`／字級 1.3 下頂列（`btn_jump`、`btn_next`）、`navStrip`、播放列不截字（併入 F2 的截字稽核；H5 之後若 F2 已發，這裡補測）。
- [ ] **Step 4：手動情境**：播伴奏中跳到別本再返回，播放不中斷且播放列仍指向 A；返回後捲動位置恢復；影片播放中跳轉出現提示。
- [ ] **Step 5：Commit／PR**；**G6**：H5 合併。

---
---

## 對帳清單（寫進 C 與 D-1 後合併方的 PR 描述）

- C-3 主題 parent：D-1 的 `Theme.Hymnchtv.Notebook.*` parent 改接 C 的 `AppTheme`（或刪除、`NotebookActivity` 用 C 主題）。
- C-1/C-2 宿主與 Toolbar：`NotebookHomeFragment` 進 `MyHymnsFragment`；Toolbar 由 `MainActivity` 的 `MaterialToolbar` 提供。
- C-5 擇一：歌詞頁筆記本列用 `content_main.xml` 的 `@id/notebookBar`（選項 a）或 C 自己 render `HymnBarViewModel.state`（選項 b）。
- C-8 擁有權：`HymnTitleSource`/`AssetHymnTitles` 由先合併方擁有，後合併方重用（C 的 `ui/titles/` 與 D-1 的 `notebook/ui/titles/` 擇一）。
- C-11 字型：LXGW WenKai 於 Task 4 綁入 `res/font/`；若與 D-1 的字型任務衝突，不手動合併二進位檔，擇一後重跑字型驗證（D-1 的 `FontSubsetTest` 若日後建立）。
- **1.1（rev 4）**：1.1.0 = G-DB（主執行緒查詢清除）＋ D-1a 資料層（PR #11）＋ H3 ＋ H4 ＋ F2；`UiFlags.NOTEBOOK_UI_ENABLED` 維持 `false`；D-1 UI 與 H5 延後（順序待定）。
- home spec（`2026-10-02-home-entry-and-lyrics-jump-design.md`）§1–§7 由 H3／H4／F2 對帳；§8（歌詞頁跳轉）屬 H5，**1.1 之後**，F2 不得依賴。
- D-1 UI 接點（見 `2026-10-02-d1-notebook-ui-implementation.md` rev 5）：「＋歌單」在 `hymn_picker.xml` 的 `btn_add_playlist`（由 `HymnPickerController`／`PickerChrome` 控制，受 `UiFlags` 管）；唱詩紀錄分頁加進 H3 的 `HistoryFragment`；歌詞頁「下一首」維持 `btn_next`／`scrollNextHymn()`；`UiFlags` 在 D-1 UI 完成時打開。
