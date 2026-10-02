# 首頁點詩歌互動改版與歌詞頁跳轉（C 子項目 Task H3/H4/H5，rev 5 定案）

## 修訂紀錄
- rev 2：依 Codex 首輪（P1×6、P2×4、P3×2）修訂：英文改為「英文詩號」來源、附號只限大本/青年、有效性改用逐號判斷、取消硬性一屏、搜尋重構為結構化結果與範圍參數、目錄預選與完整記錄頁補介面、補無障礙/深色/橫向/狀態邊界。

- rev 3：依 Codex 複審（P1×2、P2×2）：定義青年附號跨層契約、搜尋回傳 `SearchPage(hasMore)`、目錄介面改為 `openToc(book, page)`、明定首頁移除 `btn_next`；英文語意經使用者確認；新增 §8 歌詞頁跳轉（H5）。
- rev 4：依 Codex 第三輪（P1×2、P2×3、P3×1）：青年附號契約縮小為 UI 層、媒體層不動；H5 拆出 `viewingRef`／`PlaybackSession`／`nextSlot` 三個獨立狀態；定義系統返回鍵優先序、`ReadingPosition`、播放保證範圍只限背景音訊。
- rev 5：Codex 第四輪 P1=0（定案）；併入其 P2/P3：導覽狀態整體保存、自動連播以 session 為準、`SearchResult` 改持 `HymnRef`、補測試。

## 背景
- 現況（C 的 H1 已實作，`ui/home/HomeFragment.kt`、`res/layout/fragment_home.xml`）：先打號碼，再按 6 個詩歌本按鈕之一開啟（本按鈕兼「確定」）。頂部長提示、`tv_entry`、`title_preview`、展開式歷史清單、搜尋框、數字鍵 n0–n11（附、刪）、6 本 `bs_*`、`btn_english`（輸入視為英文詩號，查 `toc_all_eng2ch.txt` 開對應中文詩；長按查大本頁）、`btn_add_playlist`、`btn_next`。
- 問題：順序與聚會報詩（先本後號）相反；選本前預覽不準；6 個確定鍵、無效號要按了才知道；小螢幕要捲動才看到本按鈕；需長提示。
- 使用者：先本後號、一場常換多本；點詩歌的人需要搜尋歌詞、看最近開過的與（D-1 後）唱詩紀錄、看目錄。

## 設計

### 1. 版面
直向：
```
🔍 搜尋詩名或歌詞…                        ← 點擊進搜尋頁（§4）
[大本][補充][新歌][新詩]                  ← 來源單選，兩列各 4 格
[青年][兒童][英文][目錄›]                 ← 「英文」是來源；「目錄›」是動作鍵（外觀區隔）
        補充本 第 45 首
     （即時詩名 / 狀態訊息）               ← live region
   7 8 9 / 4 5 6 / 1 2 3 / 附 0 ⌫         ← 每列 ≥48dp（預設 52dp）
[        開啟 ▸        ] [＋歌單]
最近：補45 大123 新詩12 …   更多›          ← 橫向 chip 列
```
- 版面目標（非硬限制）：在 360×720dp 以上（含 toolbar 與底部導覽、系統字級 1.0）時，從搜尋到「開啟」鍵不捲動可見；320×640dp 與大字級允許整頁捲動，但不得把觸控目標縮到 48dp 以下。以 320×640、360×720、字級 1.3 實測截圖驗收。
- 橫向（`layout-land/fragment_home.xml`）：雙欄，左欄搜尋、來源、預覽、最近；右欄鍵盤與開啟。
- 移除頂部長提示文字。顏色一律走 theme attribute 與現有 `HomeAppearance`，移除 `fragment_home.xml` 硬編碼前景色；淺/深色與照片背景都要達 4.5:1。

### 2. 來源與號碼輸入
- **來源**：大本、補充、新歌、新詩、青年、兒童、英文七選一（MaterialButtonToggleGroup 單選或 ChipGroup singleSelection，暴露 checked 狀態給 TalkBack）。記住上次來源（沿用 `LastHymnType` 偏好，加入「英文」值），跨重啟恢復。未開啟的號碼與附狀態**不**跨行程恢復（只存 ViewModel，旋轉保留）。
- **切換來源**：保留數字；若新來源不支援附號則自動退出附模式（預覽提示「此本無附」一次）；預覽即時重算。
- **有效性**：新增無副作用的 `HymnNumberRules`（純 Kotlin，可單元測試），資料來自 `HymnNoValidate` 的區間表（抽出成常數，不呼叫會顯示 Toast 的 `validateHymnNo()`）：
  - `isValid(source, number, isFu)`：逐號判斷，含補充本／兒童本不連續區間、新歌 168–170、新詩 34 等缺號。
  - `canAppendDigit(source, prefix, isFu, digit)`：加上該數字後，仍存在至少一個以此為前綴的合法號碼才為 true；不符者停用該數字鍵（保留 ⌫），停用鍵的 contentDescription 說明原因。
  - 英文來源：合法性 = `toc_all_eng2ch.txt` 有對照；`canAppendDigit` 以對照表的英文號集合判斷。
- **附號**：只有大本、青年有附。**契約（rev 4 縮小範圍）**：新增 `HymnRef(book, storedNo)`，`isFu`、顯示號由 `HymnNumberRules` 依本推導（大本 storedNo>780、青年 storedNo>275）。`HymnRef` 是首頁輸入、開啟、歷史**顯示**與重開、跳轉堆疊的唯一換算點。**媒體層不動**：`MediaRecord`、`media_record.isFu` 欄位、匯入/匯出檔格式、`MediaConfig` 驗證維持現行語意（`isFu` 只代表大本附；青年附的媒體以 storedNo 276+ 且 `isFu=0` 存取，與現有資料和內建 `url_import.txt` 相容）；`MediaRecord.isFu(type,no)` 不改。歷史表的 `isFu` 欄位照舊寫入（沿用 `MediaRecord.isFu`），但讀取顯示一律由 `HymnRef` 從 `(book, storedNo)` 推導，故青年附在最近/記錄頁顯示「青年 附 N」並正確重開。測試：大本附與青年附的開啟、寫入歷史、歷史顯示、刪除、重開；青年附媒體記錄以現行鍵查得到（回歸）。
- **預覽（live region）**：
  - 合法：「補充本 第 45 首」＋詩名（沿用 `HymnTitleSource`）。
  - 不合法：「補充本無第 N 首」，開啟鍵停用；若其他來源有合法同號，顯示「此號亦見於：大本、新詩」，每個本名可點，點後切到該來源並保留數字（附號只比對大本/青年）。
  - 英文來源：「英文 第 N 首 → 大本 第 M 首 〈詩名〉」；無對照時「無中文對照」，開啟鍵停用（不再開補充本 dummy）。
- **開啟**：開該來源該號（英文來源開對應中文詩並帶英文號，沿用現有 `showContent` 參數）。舊 `btn_english` 與其長按（查大本頁）移除。
- **＋歌單**：D-1 接線前隱藏（不放 no-op 按鈕）。
- **首頁 `btn_next`（輸入號 +1）移除**：新版有即時預覽與鍵盤，+1 的用途由歌詞頁「下一首」（§8）承擔；HOST1 加入的 `HomeEntry.next` 與其測試一併移除。D-1 接點改為歌詞頁的「下一首」。

### 3. 目錄
- 「目錄›」呼叫新增的 `MainHost.openToc(book, page)`；`TocFragment` 新增 `select(book, page)`：同時設定書別與頁籤（`tocPage`），view 未建立時存 pending，建立後套用並 reload；已建立則直接切換。中文來源 → `(該本, 類別頁)`；英文來源 → `(大本, TOC_ENGLISH)`（英文索引頁籤）。

### 4. 搜尋（獨立 Task H4）
- 新增全螢幕搜尋頁（`SearchFragment`，由 host 以 back stack 疊加，返回回首頁）。
- 搜尋核心重構：從 `ContentSearch` 抽出 `HymnSearch.search(query, scope, limit): SearchPage(results, hasMore)`（核心以 `limit + 1` 探測，回傳最多 `limit` 筆並標記 `hasMore`），`SearchResult(ref: HymnRef, title, snippet)` 為不可分割記錄（顯示號與附號一律由 `HymnRef` 推導），修正現有以號碼作 type map key、跨本同號覆寫書別的錯誤。
- `scope` = 目前來源（英文來源時為全部）或全部，頁面上可切換。查詢先經 OpenCC 轉簡體再比對簡體資產（沿用現況），結果顯示依使用者字形設定轉換。
- 結果上限 200；`hasMore` 為真時顯示「結果過多，請縮小範圍或加長關鍵字」。測試 199、200、201 筆邊界。
- 舊 `ContentSearch` Activity 改為呼叫新核心或移除（依呼叫端盤點決定，H4 計畫列清單）。
- 測試：跨本同號、超過上限、繁體/簡體查詢、附號、標題命中與歌詞命中。

### 5. 記錄
- 「最近」：橫向 chip 列，最新 8 筆（`DatabaseBackend` history，已最新優先）；chip 視覺縮寫「補45」「大附3」，TalkBack label「補充本，第 45 首，〈詩名〉」；點擊重開，長按出現刪除確認。
- 「更多›」：H3 新增全螢幕 `HistoryFragment`（host back stack 疊加），沿用 `HistoryAdapter` 的列、滑動刪除、確認對話框；移除首頁原本點 `tv_entry` 展開 280dp 清單的行為。D-1 之後此頁改為分頁：「最近開過」＋「唱詩紀錄」。

### 6. 無障礙
- 焦點順序：搜尋 → 來源 → 預覽 → 鍵盤 → 開啟 → 最近 → 更多。
- 來源與附號暴露 checked state；停用鍵可讀取停用原因；預覽為 polite live region；觸控目標 ≥48dp。

### 7. 範圍與排程
- H3：首頁版面、`HymnNumberRules`、來源/附號/預覽/開啟、目錄預選介面、最近與 `HistoryFragment`、橫向版面。
- H4：搜尋核心重構與搜尋頁。
- 排在 C 的 L4（已完成）、F1 之後，F2 之前；F2 對帳納入本 spec。
- D-1 接點：「＋歌單」、`HistoryFragment` 的唱詩紀錄分頁、歌詞頁「下一首」。
- 測試：`HymnNumberRules`、英文對照、`HymnSearch` 單元測試；首頁、目錄預選、記錄頁、搜尋頁插樁測試；320×640 / 360×720 / 字級 1.3 / 淺深色 / 橫向截圖。

### 8. 歌詞頁跳轉與回到上一首（Task H5）
情境：正在唱 A（可能有 app 伴奏在播，也可能沒有），點詩歌的人要找下一首 B、看一下 B，再回到 A 原處繼續唱；唱完 A 一鍵到 B。

- **現況限制**（依程式）：歌詞頁 `ContentHandler` 是單一本的 ViewPager，換頁時 `updateMediaPlayerInfo()` 把播放列換成新頁；換本要重新 `showContent` 啟動 activity，而 `onUserLeaveHint`／返回處理會停止播放；伴奏由背景 `AudioBgService` 播放。
- **三個獨立狀態**（rev 4）：
  - `viewingRef: HymnRef`——正在看的那首；只供歌詞顯示、分享、目錄/英文對照等「看」的功能使用。
  - `PlaybackSession(ref, mediaType, uris, generation)`——不可變；開始播放時建立，「改播此首」/下一首推進時以新 generation 取代。所有媒體查詢、下載目標、下載完成 callback、播放器完成/錯誤、自動連播都只讀 session（callback 帶 generation，過期即丟棄），**不得**讀 `mHymnType/mHymnNo`。
  - `nextSlot: HymnRef?`——下一首槽位。
  - 實作前置：盤點 `ContentHandler` 中所有讀 `mHymnType/mHymnNo` 的媒體路徑（如 `getMediaUris`、`getHymnMediaState`、`MediaDownloadHandler` 完成後的 `startPlay()`、青年交叉參照會改寫 `mHymnType/mHymnNo` 的路徑），改為吃參數的純函式；`scrollNextHymn()` 只做 viewing 導覽，另設 `advancePlayback()` 依 session／槽位推進。青年交叉參照解析改為回傳目標 `HymnRef`，不改寫 viewing 狀態。
- **狀態保存**：`ContentNavigationState(viewingRef, playbackSession, nextSlot, returnStack)` 為單一 Parcelable，整體存入 `onSaveInstanceState`。送往 `AudioBgService` 的 action 與其回傳廣播都攜帶 generation；Activity 重建後以保存的 session 恢復播放列，並據 generation 丟棄過期事件。
- **自動連播目標**：`advancePlayback()` 的順序：有 `nextSlot` → 播槽位並清空；否則 → 以 `session.ref` 的本與序列求下一首（不是正在看的那首的下一首）。自動連播推進時：若 `viewingRef == 舊 session.ref`，viewing 跟著前進；否則 viewing 不動（使用者正在看別首）。
- **播放保證範圍**：「跳轉不中斷」只保證 `AudioBgService` 背景音訊（伴奏/教唱/唱詩音訊）。影片（ExoPlayer）/YouTube/外部播放器：跳轉前提示「跳轉會停止影片」，確認後釋放再跳轉。
- **正在播放 vs 正在看（UI）**：播放列顯示 session 的 ref。`viewingRef` ≠ session.ref 時，播放列上方顯示「正在播放：補45 ←」（點擊回到該首）；播放鍵區提供「改播此首」。無播放時行為同現況。
- **跳轉面板**：頂列加「跳轉」，開 BottomSheet，重用首頁的來源/鍵盤/預覽/最近元件（H3 抽成可重用的 `HymnPicker` 元件）與搜尋入口（H4）。兩個動作：「開啟」（切過去看）、「設為下一首」（留在原頁，只排隊）。
- **同頁換本**：`ContentHandler` 新增 `navigateTo(HymnRef)`：同本則捲到該頁；換本則就地更換 `mHymnType` 與 pager adapter，不重啟 activity、不觸發停止播放。開啟時仍寫入歷史。
- **`ReadingPosition`**：`(displayMode, outerScrollY, webViewScrollY?)`；歌詞外層 `ScrollView` 加 id；英文 WebView 模式另存 WebView Y。換 adapter 後於目標 `ContentView` layout 完成（`doOnLayout`/`post`）才 restore。以 Parcelable 存入 `onSaveInstanceState`。
- **返回堆疊**：每次經跳轉離開某首，就把 `(HymnRef, ReadingPosition)` 推入堆疊（上限 10）；頂列顯示「← 補45」，點擊回到該首並恢復捲動位置；長按列出整串可直接選。手動左右滑頁不推入堆疊。堆疊與槽位存 `onSaveInstanceState`，旋轉後保留；離開歌詞頁即清空。
- **系統返回鍵優先序**：(1) 關閉跳轉面板/搜尋疊頁；(2) 英文 WebView 內部返回（若現行有）；(3) 跳轉堆疊非空 → 回上一首；(4) 既有行為（播放中先停止播放、再離開）。`onUserLeaveHint()`（按 Home）維持既有停止邏輯，不 pop 堆疊。
- **下一首槽位**：單一槽位，再設覆蓋；頂列「下一首」顯示「下一首：新詩12」，按下前往 B，若當時正在播放則改播 B，槽位清空。無槽位時行為同現況（同本下一號，`scrollNextHymn`）。自動連播（`mAutoStream`）遇到槽位時優先播放槽位。D-1 後「下一首」改走歌單，槽位相當於單首臨時歌單。
- **測試**：Activity 重建時背景音訊持續、回來後播放列與 session 正確；播 A、看 B、A 結束後自動連播到 A 的下一首且 viewing 停在 B；A 下載中跳至 B，下載完成不得播放/下載 B；青年交叉參照播放後 `viewingRef` 不變；影片播放中跳轉出現提示；系統返回鍵四層優先序；伴奏播放中跳到別本再返回，播放不中斷且播放列仍指向 A；返回後捲動位置恢復；連跳多首逐層返回；長按清單；設為下一首後按下一首（有/無播放）；自動連播接槽位；旋轉保留堆疊。
- **排程**：H5 依賴 H3 的 `HymnPicker` 與 H4 搜尋；排在 H4 之後、F2 之前。
