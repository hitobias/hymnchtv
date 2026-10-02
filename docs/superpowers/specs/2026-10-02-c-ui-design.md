# 子項目 C：介面現代化 —— UI 設計 spec

> 本文件是子項目 C（介面現代化）的**設計決策**，對應可執行計畫
> `docs/superpowers/plans/2026-10-02-c-modern-ui-implementation.md`。
> 修訂紀錄寫在計畫檔；本檔只有 rev 1。

**狀態：草稿 rev 1，等 codex review 與使用者確認。**

---

## 1. 目標與範圍

把 hymnchtv 從「單一 Activity 塞滿隱藏功能」改成 Material 3 標準的現代 App：

- 底部導覽四個分頁：**首頁、目錄、我的詩歌、設定**。
- 歌詞頁採用「詩本紙本」版式（文楷、宣紙白、紅色節數、拍號調號）。
- 首頁採用「聚會速記」行為（輸入即時顯示詩名、詩歌本按鈕放大、＋歌單、歌詞頁「下一首」）。
- 把長按／選單裏的功能改成看得見的按鈕或設定項。

**範圍邊界（YAGNI）**：
- 不改歌詞內容、媒體下載/播放邏輯、搜尋演算法、TOC 檔、資料層。
- 不重寫整個 App；只把「被改到的畫面」轉成 Kotlin，一次一個畫面。
- 子項目 D-1（筆記本）的介面由 D-1 UI 計畫負責；C 只提供「我的詩歌」分頁槽位與契約（見 §9）。
- 子項目 D-3（底部迷你播放器）與 C 的歌詞頁綁在一起做；本版只保留既有播放器列，D-3 另議。

---

## 2. 現況摘要（盤點結果，2026-10-02）

- **minSdk 24 / compileSdk 37 / targetSdk 37**，AGP 9.3.3，Java 11（`hymnchtv/build.gradle`）。
- 依賴 `appcompat:1.8.0`，**沒有** `com.google.android.material:material`。
- 主題：`theme.xml` 定義 `AppTheme`（Dark，parent `Theme.AppCompat`）與 `AppTheme.Light`（parent `Theme.AppCompat.Light`）；`ThemeHelper` 用 enum `LIGHT/DARK` + 靜態 `mTheme`；`BaseActivity.onCreate` 在 `super` 前 `setTheme`。**無 DayNight、無動態取色。**
- 品牌色：`color_primary #ff09354d`（深藍）、`color_primary_dark #021625`、`color_accent #2da4f2`。
- 畫面（Activity）清單：`MainActivity`（首頁，singleTask）、`ContentHandler`（歌詞頁）、`HymnToc`（目錄）、`ContentSearch`（搜尋）、`About`、`RichTextEditor`、`MediaConfig`（媒體設定）、`WallPaperUtil`（自訂桌布）、`ChineseS2TSelection`（歌詞語言）、`DialogActivity`；Fragment：`ContentView`（每頁歌詞）、`MediaGuiController`（播放器列）、`MediaDownloadHandler`（檔案傳輸）、`WebViewFragment`。
- 長按功能（已盤點 13 處，詳見 §4.3）：主畫面背景選單、青年詩歌附錄、英中對照、歷史刪除、歌詞簡繁設定、英文歌詞、歌詞頁選單、播放鍵連播、搜尋/媒體/教唱/唱詩/伴奏的替代搜尋、About 長按查更新。

---

## 3. 視覺方向（使用者 2026-10-02 已拍板，混合建議）

來源：主計畫「子項目 C」的視覺方向決策，及設計稿
<https://claude.ai/artifact/JxKzrjhQfgJpyjywury5Ja>（本 spec 撰寫時設計稿內容未能讀入，
精確色值/間距/字級 scale 標記為「待對照設計稿」，見 §7）。

### 3.1 整體骨架：Material 3（方向 A）

- 底部導覽 `BottomNavigationView`，四個分頁：首頁、目錄、我的詩歌、設定。
- 標準 Material 3 元件；Android 12+ 支援動態取色（`DynamicColors`），品牌色作為 fallback。
- 畫面延伸到系統列（edge-to-edge，`WindowCompat.setDecorFitsSystemWindows(false)` + insets）。
- 設定頁用標準 `PreferenceFragmentCompat`。

### 3.2 歌詞頁版式：詩本紙本（方向 B）

- 文楷（LXGW WenKai）字體；宣紙白背景；節數用紅色。
- 標題下方印出拍號與調號；預設字級 24sp（現行 20sp → 24sp）。
- 簡中／繁中、譜詞切換按鈕沿用方向 A 的樣式（現行 `button_ts` / `button_english`）。

### 3.3 首頁輸入：聚會速記（方向 C）

- 輸入號碼時即時顯示詩歌標題。
- 詩歌本按鈕放大。
- 加「＋歌單」按鈕（報號時邊聽邊加）。
- 歌詞頁加「下一首」，聚會中依歌單連續翻頁，不必回首頁。

### 3.4 年長者友善

- 觸控區至少 48dp；對比度達 WCAG AA；支援系統字級。
- 現行字級設定（`PREF_TEXT_SIZE`）保留，但改由設定頁統一控制。

---

## 4. 現有功能對應表

「→」左邊是現況（檔案:行），右邊是新版位置。**每一項都必須在新版找得到**，不可因改版而消失。

### 4.1 首頁（`MainActivity` + `layout/main.xml`）

| 現況 | 新版位置 |
|---|---|
| 數字鍵盤 n0–n9、附（n10）、刪除（n11） | **首頁分頁**，保留，改用 M3 按鈕樣式 |
| 6 個詩歌本按鈕 bs_er/xb/xg/yb/bb/db | **首頁分頁**，放大（方向 C），同時記住上次使用的詩歌本 |
| `btn_english` 英中對照（長按 alt） | 首頁分頁，保留長按 alt；或移入歌詞頁 |
| `tv_entry` 輸入顯示（點擊 → 歷史清單） | **首頁分頁**，輸入即時顯示詩名（方向 C）；歷史清單保留為點擊展開 |
| `tv_search` 搜尋框 + `btn_search` | 首頁分頁保留；或把搜尋入口移到**目錄分頁**（搜尋列）。見 §7 待確認 |
| `spinner_toc` 目錄類型（目錄/類別/筆畫/拼音/英中） | **目錄分頁**的類型切換 |
| `btn_update` 軟體更新（有更新才顯示） | **設定分頁**（主計畫：首頁空間留給常用功能） |
| `historyListView` 歷史（滑動刪除、長按刪除） | 首頁分頁的「最近」區塊（或我的詩歌分頁）；保留滑動/長按刪除 |
| 背景長按 → context menu（`menu_main`） | 移除；功能全部移入設定分頁／畫面上按鈕 |

### 4.2 選單 `menu_main.xml`（現況全在長按/overflow 選單，新版全部搬進**設定分頁**）

| 現況選單項 | 新版位置 |
|---|---|
| appTheme（themeDark/themeLight） | 設定：外觀（跟隨系統／淺色／深色） |
| appLocale（localeSystem/簡中/繁中/EN） | 設定：語言 |
| lyricsLanguage | 設定：歌詞語言（開啟 `ChineseS2TSelection`） |
| bg（12 桌布 + 自訂 sbguser） | 設定：桌布（保留 12 張 + 自訂 → `WallPaperUtil`） |
| fontColor（8 色） | 設定：歌詞文字顏色 |
| fontSize（small/middle/large/xlarge/inc/dec） | 設定：字級（用 `SeekBarPreference` 取代 6 個選項，或保留離散選項） |
| media_config | 設定：媒體設定（開啟 `MediaConfig`） |
| permission_request | 設定：權限 |
| online_help | 設定：線上說明 |
| about | 設定：關於（`About`） |
| exit | 設定：結束（或移除，改系統手勢；見 §7） |
| sn_convert（隱藏，debug） | 保留隱藏 |

### 4.3 長按功能（全部改成看得見的操作或設定項）

| # | 現況（檔案:行） | 動作 | 新版 |
|---|---|---|---|
| 1 | `MainActivity:246` viewMain 長按 | context menu（menu_main） | 移除；功能入設定分頁 |
| 2 | `MainActivity:295` btn_yb 長按 | 青年詩歌附錄 alt | 歌詞頁／首頁的「附」按鈕；或保留長按但加提示 |
| 3 | `MainActivity:304` btn_english 長按 | 英中對照 DB page alt | 保留長按（加 visible 提示）或移歌詞頁 |
| 4 | `MainActivity:1176` 歷史項長按 | 刪除歷史 | 歷史列加刪除按鈕（保留滑動刪除） |
| 5 | `ContentView:266` button_ts 長按 | 歌詞語言設定 | 歌詞頁按鈕旁加「設定」入口；或改由設定分頁 |
| 6 | `ContentView:272` button_english 長按 | 英文歌詞 reinit | 英文歌詞按鈕加 menu（重載） |
| 7 | `ContentView:237` lyricsView 長按 | 歌詞頁選單（menu_content） | 歌詞頁頂列按鈕（保留 overflow，改可見） |
| 8 | `MediaGuiController:496` playbackPlay 長按 | 連播（auto stream） | 播放器列加「連播」開關 |
| 9 | `MediaGuiController:512` btn_hymnSearch 長按 | Google 搜尋 | 移除或併入搜尋 |
| 10 | `MediaGuiController:516` btn_media 長按 | QQ 搜尋 | 移除或併入分享 |
| 11 | `MediaGuiController:520` btn_jiaochang 長按 | Notion 搜尋 | 移除或併入分享 |
| 12 | `MediaGuiController:524` btn_changshi 長按 | BibleTool hymnal | 移除或併入分享 |
| 13 | `About:165` history_log 長按 | 檢查更新 | 設定分頁「檢查更新」按鈕 |

### 4.4 歌詞頁選單 `menu_content.xml`

| 現況 | 新版 |
|---|---|
| alwayshow / alwayhide / menutoggle（播放器列顯示） | 播放器列收合按鈕（或自動） |
| scoreColorChange（譜顏色反轉） | 歌詞頁頂列「譜顏色」按鈕 |
| lyrcsTextSizeInc/Dec（歌詞字級） | 歌詞頁「A−/A+」按鈕 |
| media_config | 歌詞頁媒體設定 |
| lyrcsEnglish / lyrcsEnglishDelete（英文歌詞） | 歌詞頁英文歌詞按鈕 + 刪除 |
| lyrcsShare（分享歌詞） | 歌詞頁「分享」按鈕 |
| help / home | 歌詞頁「說明」「回首頁」 |

### 4.5 其餘畫面

| 現況 | 新版 |
|---|---|
| `HymnToc`（目錄 Activity） | **目錄分頁**（Fragment），詩歌本切換 + TOC 類型切換（`MaterialButtonToggleGroup` 或 `TabLayout`） |
| `ContentSearch`（搜尋 Activity） | 保留 Activity，或改為首頁/目錄的搜尋結果 Fragment；見 §7 |
| `About` | 保留 Activity（Z 子項目會改內容）；入口移設定分頁 |
| `RichTextEditor` | 保留（備份檔案編輯）；入口從 MediaConfig 而來，不屬於導覽 |
| `MediaConfig` | 保留 Activity；入口移設定分頁 |
| `WallPaperUtil` | 保留 Activity；入口移設定分頁 |
| `ChineseS2TSelection` | 保留 Activity；入口移設定分頁 |
| `MediaGuiController`（播放器列） | 保留 Fragment；樣式改 M3；此版不引入 D-3 迷你播放器 |

---

## 5. 資料與狀態歸屬

（C 不新增資料；只確認各狀態的新歸屬，避免多頭寫。）

| 狀態 | 儲存 | 讀寫者（現況） | 新版歸屬 |
|---|---|---|---|
| 主題 | `PREF_SETTINGS` `PREF_THEME`（DARK/LIGHT） | `MainActivity.setAppTheme` → `ThemeHelper` | **新增「跟隨系統」**；改 `ThemePrefs`（Kotlin）集中管理，`ThemeHelper` 改成 facade（見 §7） |
| 語言 | `PREF_LOCALE`（`LocaleStore`） | `MainActivity.setAppLocale` | 設定分頁（`LocaleStore` 不變） |
| 歌詞語言/轉換 | `PREF_CONVERSION_TYPE`、`LyricsLanguagePolicy.PREF_LYRICS_DEFAULT` | `ChineseS2TSelection`、`ContentView` | 設定分頁 + 歌詞頁按鈕（不變） |
| 首頁字級 | `PREF_TEXT_SIZE` | `MainActivity.setFontSize` | 設定分頁 |
| 首頁文字色 | `PREF_TEXT_COLOR` | `MainActivity.setFontColor` | 設定分頁（歌詞文字顏色） |
| 桌布 | `PREF_BACKGROUND`（index）、`PREF_WALLPAPER`（檔名） | `MainActivity.setWallpaper`、`WallPaperUtil` | 設定分頁 |
| 上次詩歌本 | `mHymnType`（static） | `MainActivity` | 首頁分頁（記住上次使用的詩歌本，方向 C） |
| 目前詩歌 | `mHymnType`/`mHymnNo`（static） | `MainActivity.setHymnTypeNo`、`ContentHandler` | 維持 static 或改 `HymnSelection`（Kotlin singleton） |
| 歷史 | SQLite `hymnHistory`（`DatabaseBackend`） | `MainActivity.initHistoryList`、`storeHymnHistory` | 首頁分頁「最近」（不改資料層） |
| 播放器列顯示 | `PREF_MENU_SHOW` | `ContentHandler` | 歌詞頁（不變） |
| 播放器 media type/speed/loop | `PREF_MEDIA_HYMN`、`PREF_PLAYBACK_*` | `MediaGuiController` | 播放器列（不變） |
| 譜顏色/歌詞字級 scale | `PREF_SCORE_COLOR`、`PREF_LYRICS_SCALE_*` | `ContentView` | 歌詞頁（不變） |
| 筆記本資料（D-1） | Room `notebook`（D-1a） | — | 「我的詩歌」分頁（D-1 UI 負責） |

---

## 6. 導航細節

### 6.1 Activity 架構

- `MainActivity`（`launchMode="singleTask"`，launcher + SEND intent 接收）**維持單一 Activity 主入口**，但內容改為「底部導覽 + 4 個分頁 Fragment」。
- 歌詞頁 `ContentHandler` **維持獨立 Activity**（全螢幕閱讀體驗，`singleTask` 從首頁 `startActivity` 進入）。
- 其餘次要畫面（`HymnToc` → 分頁、`ContentSearch`、`About`、`MediaConfig`、`WallPaperUtil`、`ChineseS2TSelection`）維持 Activity 或轉 Fragment（見 §7）。

### 6.2 單一分頁容器（無 Navigation Component）

- `MainActivity` 用 `BottomNavigationView` + `FragmentContainerView`，手動 `FragmentTransaction` 切換（不引入 `androidx.navigation`），與 D-1 的 `NotebookNavigator` 契約一致。
- 四個分頁各為一個 Fragment：`HomeFragment`、`TocFragment`、`MyHymnsFragment`（D-1 的槽位）、`SettingsFragment`。
- 分頁切換用 `show`/`hide`（保留分頁狀態）或 `replace`（視狀態保留需求，計畫內決定）。

### 6.3 share intent（`MainActivity.handleIntent`）

- `ACTION_SEND`（text/image/video/audio）→ `MediaConfig`，**不變**。
- 新版首頁仍須在 `onCreate`/`onNewIntent` 呼叫 `handleIntent`。

### 6.4 返回鍵

| 現況 | 新版 |
|---|---|
| `MainActivity` back：歷史列收合 → finish/popBackStack | 首頁分頁：歷史列收合 → 若在非首頁分頁回「首頁」→ finish |
| `ContentHandler` back：webview → 播放器 → 停播 → 回首頁 | 不變 |
| `HymnToc`/`ContentSearch` back：finish | 目錄分頁：回上一分頁；搜尋（若轉 Fragment）回上一層 |

### 6.5 singleTask 遷移

- `MainActivity` 維持 `singleTask`（因為要接收 share intent + 保持單一實例）。
- 底部導覽狀態不進 back stack；切分頁不建立新 task。

---

## 7. 待確認的設計決策（已確認 2026-10-02）

> 以下決策已由使用者確認：**全部採預設**。品牌色沿用深藍 `#09354d`；其餘（DayNight 三選一、搜尋維持 `ContentSearch` Activity、字級 `SeekBarPreference`、移除 `exit`、移除長按替代搜尋）照預設。執行計畫時不再逐項詢問。

1. **品牌色**：現況 `#09354d`（深藍）；D-1 UI 暫用 `#9C2B23`（深紅，與歌詞頁紅色節數一致）。**預設：品牌 fallback 沿用 `#09354d`，歌詞頁節數紅色另用 accent**；若設計稿指定品牌色，改設計稿。← 需你確認。
2. **深色模式來源**：主計畫寫「DayNight + 動態取色」。**預設：改成 `Theme.Material3.DayNight.NoActionBar` + 三選一（跟隨系統/淺色/深色，`AppCompatDelegate`）**；`ThemeHelper` 改 facade，D-1 的 `NotebookThemes.forApp` 配合改（C-4）。
3. **搜尋入口**：現況首頁 `btn_search` 開 `ContentSearch` Activity。**預設：首頁搜尋框保留，結果維持 `ContentSearch` Activity**（改版成本低）；備選：搜尋結果改 Fragment 放目錄分頁。
4. **字級設定**：現況 6 個離散選項。**預設：設定分頁用 `SeekBarPreference`（範圍 25–50sp，step 1）**；備選：保留離散選項。
5. **exit 選單項**：現況「結束」呼叫 `System.exit(0)`（非標準）。**預設：移除**（系統手勢）；備選：保留在設定分頁。
6. **長按替代搜尋（§4.3 #9–12）**：QQ/Notion/BibleTool/Google 搜尋。**預設：移除**（YAGNI，主計畫沒要求）；備選：保留其中某些。
7. **「我的詩歌」分頁與 D-1 的整合時序**：C 先合併（提供空槽位）或 D-1 先合併（`NotebookActivity`）——由 D-1 UI 計畫的關卡 G4 依合併順序決定（C-1 的兩種模式都支援）。

---

## 8. 分階段計畫（每階段可獨立回退）

| 階段 | 內容 | 產出（可獨立驗證） | 回退方式 |
|---|---|---|---|
| **P0 基礎** | 加 `material` 依賴；定義 `Theme.Material3.DayNight.*` + `DayNight` 決策；edge-to-edge；`ThemeHelper` facade；`BaseActivity`/`BaseFragment` 接 DayNight | App 主題套用，無功能回歸 | 還原 theme.xml + ThemeHelper |
| **P1 底部導覽骨架** | `MainActivity` 改 `BottomNavigationView` + 4 空分頁；share intent/back 邏輯保留 | 4 分頁可切換，share 正常 | 還原 MainActivity |
| **P2 首頁分頁** | `HomeFragment`：鍵盤 + 詩歌本（放大、記住上次）+ 即時詩名 + 歷史列 + ＋歌單 | 首頁可用（舊功能全在） | 還原 main.xml → HomeFragment |
| **P3 目錄分頁** | `TocFragment`：詩歌本切換 + TOC 類型切換 + 樹狀目錄 | 目錄可瀏覽並開歌詞 | 還原 HymnToc Activity 或保留 |
| **P4 設定分頁** | `SettingsFragment`（`PreferenceFragmentCompat`）：語言/外觀/歌詞/字級/桌布/媒體/權限/說明/關於/更新 | 設定全集中 | 還原 menu_main |
| **P5 我的詩歌分頁** | `MyHymnsFragment` 空槽位，接 `NotebookNavigator`（C-1/C-2） | 分頁存在，D-1 可接 | 移除分頁 |
| **P6 歌詞頁重設計** | `ContentHandler`/`content_lyrics` 詩本紙本版式 + 頂列按鈕 + 下一首（C-5/C-6/C-10） | 歌詞頁新版，smoke test 過 | 還原歌詞頁 |
| **P7 整合收尾** | 移除長按/舊選單、`About`/`MediaConfig`/`WallPaperUtil` 入口搬移、smoke test、字型重產（C-11） | 全功能無長按隱藏 | 逐項還原 |

> 每個階段完成後單獨 commit；階段間不互相依賴（P1 之後可任選 P2/P3/P4 平行）。

---

## 9. 與 D-1 UI 的介面契約（C 要提供）

D-1 UI 計畫「依賴 C 的介面」列了 C-1～C-11；C 這一方承諾如下（**C 合併時逐條滿足**）：

| # | C 提供 | 落地位置（本計畫） |
|---|---|---|
| C-1 | 底部導覽「我的詩歌」分頁 + **空 Fragment 容器**（**不**實作 `NotebookNavigator`，由 D-1 接） | `MyHymnsFragment` 空槽位（Task M1）；D-1 於其整合 task 放 `NotebookHomeFragment` 並接 `NotebookNavigator` |
| C-2 | 頂端 app bar 由宿主提供 | `MainActivity` 的 `MaterialToolbar`（Task HOST1）；D-1 的 `MenuProvider`/`requireActivity().title` 掛到它 |
| C-3 | Material 3 主題 + 品牌色 | `Theme.Material3.DayNight.*`（Task 0）；D-1 的 `Theme.Hymnchtv.Notebook.*` parent 改接本主題或刪除 |
| C-4 | 深色模式來源（DayNight） | Task 0/1 DayNight 決策；`ThemeHelper` 由 `ThemePrefs` 同步快取（`syncDark`），D-1 的 `NotebookThemes.forApp` 改回傳單一 DayNight |
| C-5 | 歌詞頁新版面標題下筆記本列**空容器**；收藏星號頂列（D-1 接 `HymnNotebookBarFragment` 或 render `HymnBarViewModel.state`） | `content_main.xml` 加 `@id/notebookBar` 空容器（Task L3，單一宿主）；C **不** import `HymnBarViewModel` |
| C-6 | 歌詞頁「下一首」按鈕 | Task L3：按鈕 `@id/btn_next`，C 預設接 `scrollNextHymn()`；D-1 合併後接 `HymnBarViewModel.nextInPlaylist()` |
| C-7 | 首頁「＋歌單」按鈕 | Task H1：按鈕 `@id/btn_add_playlist`（C 宣告 id，click no-op）；D-1 合併後接 `AddToPlaylistDialogFragment.show(...)` |
| C-8 | 輸入即時詩名 → `HymnTitleSource`/`AssetHymnTitles` | Task H2；先合併方擁有（C 的 `ui/titles/` 或 D-1 的 `notebook/ui/titles/` 擇一，後合併方重用） |
| C-9 | 設定頁「唱詩紀錄」「備份」**類別空位** | Task S1 設定分頁留類別；D-1 合併後把 `nb_preferences.xml` 併入或嵌入 `NotebookSettingsFragment` |
| C-10 | smoke test 保留接點 | Task 3 基線 + Task F2 核對；保留 `showHymn`/`onPlaybackCompleted`/`NotebookBarHost` |
| C-11 | 文楷字型 | Task 4 綁入 LXGW WenKai；若與 D-1 的字型任務衝突，不手動合併二進位檔 |

---

## 10. 風險與對策

1. **`MainActivity` 拆成分頁後，`singleTask` + `onNewIntent` + share intent 要保留** → P1 不刪 `handleIntent`，加 smoke test。
2. **改版期間既有功能回歸** → 每個階段獨立 commit + 手動 checklist；P0 先跑既有 instrumented test。
3. **DayNight 改動影響 D-1** → `ThemeHelper` 維持 facade，C-4 契約先定，D-1 合併方對帳。
4. **長按功能移除後使用者找不到** → §4.3 每項都標「新版位置」，實作時逐項驗證。
5. **歌詞頁版式改壞** → P6 保留 `ContentView` 的資料載入邏輯，只改版式（layout + 樣式）。
