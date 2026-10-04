# 單頁首頁、預設純歌詞、回首頁入口、主題化設定頁與高級感（1.2.0）

> 狀態：rev 2（2026-10-04）。Codex r1：P1×6、P2×1 已修正（見 §9）。使用者要求：
> 1. 取消首頁底部導覽列，首頁（點數字選詩歌）為一頁滿屏、不可捲動；
> 2. 預設只顯示歌詞，不顯示詞譜；
> 3. 播放／歌詞頁的選單加「回首頁」按鈕或選項；
> 4. 其他方便使用者之處。

## 1. 首頁成為唯一主畫面（取消底部導覽）

- `activity_main_host.xml` 移除 `BottomNavigationView @id/bottom_nav` 與 `menu_bottom_nav.xml`；`MainHost` 不再管理分頁，只顯示 `HomeFragment`。
- **目錄、設定的新入口**：首頁頂端一列（高 48dp）：左側 app 名稱（或留白），右側兩個 48dp 圖示鈕「目錄」（list 圖示，contentDescription「目錄」）、「設定」（gear，「設定」）。配色走 `UiTokens`（與首頁背景推導一致）。
- 目錄、設定改為**全頁**開啟：以 fragment 疊加（`replace` 進 host 容器並 `addToBackStack`），頂端有返回箭頭與標題；系統返回鍵回到首頁。既有 `openToc`（`btn_toc`、MainActivity.java:225）改走同一路徑。目錄選詩後的行為不變（開歌詞頁）。
- 「我的詩歌」維持隱藏（`UiFlags.NOTEBOOK_UI_ENABLED=false`），不新增入口。
- 狀態保存：旋轉／process death 後若在目錄或設定頁，回到該頁（fragment back stack 自然保存）。

## 2. 首頁一頁滿屏、不可捲動

- `fragment_home.xml` 移除 `NestedScrollView`；`hymn_picker` 以垂直 `LinearLayout`／`ConstraintLayout` 佈滿可用高度（扣除系統列 inset）：
  - 固定區：頂列、詩歌本選擇、號碼與詩名預覽（沿用 PR「預覽固定高度」）。
  - 彈性區：數字鍵盤以權重分配剩餘高度，按鍵高度介於 48dp（最小觸控）與 72dp 之間。
  - 最近詩歌：顯示「能放下的筆數」（依剩餘高度計算，最少 0、最多 5），「全部記錄」入口永遠可見。
- 直向一律不可捲動。**例外**：若在最小觸控高度下仍放不下（橫向、極小螢幕、系統字級最大），整頁退回可捲動，避免控制項被截斷——這是無障礙底線，不得裁切。橫向版面沿用此例外（可捲動）。
- 鍵盤與預覽的位置在輸入時不跳動（沿用 1.1.0 規則）。

## 3. 預設只顯示歌詞

- `DisplayMode.fromPref(null／未知)` 預設值由 `SCORE_AND_LYRICS` 改為 `LYRICS_ONLY`；設定頁 `ListPreference` 與 Aa 面板的顯示一致。
- 已明確設定過顯示模式的使用者保留原值（只改「未設定」時的預設）。
- 歌詞頁的 `button_mode` 臨時切換行為不變；若某首詩歌沒有文字歌詞（只有詞譜圖），自動顯示詞譜（沿用既有回退；若無回退則新增：無歌詞文字時以 SCORE_ONLY 呈現）。

## 4. 回首頁入口

- 歌詞頁頂列最左側新增 48dp「首頁」圖示鈕（house，contentDescription「回首頁」），跟頂列一起自動隱藏／顯示（`ChromeController`）。
- ⋮ 選單的既有 `home` 項目移到第一項，標題「回首頁」。
- 展開的播放卡右上角（收合鈕左側）**不**加首頁鈕，避免與收合、播放控制擁擠；播放卡的回首頁需求由頂列鈕滿足。
- 行為：與既有 `backToHome()` 相同——停止播放、`finish()` 回首頁。

## 5. 其他便利（本版一併做）

- **首頁返回鍵**：在首頁按返回鍵時不直接關閉，先顯示 Snackbar「再按一次返回鍵離開」，2 秒內再按才離開（避免誤觸退出）。
- **開啟後聚焦號碼**：首頁回到前景時，號碼輸入清空、詩歌本維持上次選擇（現行若已如此則不改）。
- **閃退修正（1.1.1 遺留）**：`MediaExoPlayerFragment.playVideoUrlExt`（約 253 行）在 `startActivity` 前以 `resolveActivity` 檢查，沒有可處理的 app 時顯示 Toast「沒有可播放此媒體的應用程式」，不閃退；同類外部 intent（同檔案其他 `startActivity`）一併防護。補單元或插樁測試重現 `ActivityNotFoundException`。

## 5a. 設定與目錄頁跟隨主題、加強層次（使用者 10/04 追加）

使用者回報：「選項的頁面沒有隨主題顏色一起更改，另外層次不夠分明」。

- **跟隨主題**：設定（`SettingsFragment`、`ReadingSettingsFragment`、`c_preferences.xml`／`reading_preferences.xml`）與目錄（`TocFragment`）全頁的背景、標題列、文字、分隔線、開關／單選元件色一律由首頁同一組 `UiTokens`（`MainChrome` 的 `TokenInput`）推導：頁面底 `background`、群組卡片 `surface`、主文字 `onSurface`、摘要 `onSurfaceMuted`、分類標題 `accent`、開關啟用 `accent`。首頁背景或閱讀色變更後，回到這兩頁立即套用（`onResume` 重讀或 listener）。照片背景時頁面底取照片推導的 `surface` 純色，不鋪照片（可讀性優先）。
- **層次（參考 Android 系統設定／Material 3 分組清單）**：
  - 分類標題：14sp、medium 字重、`accent` 色，上方 24dp、下方 8dp 間距，左對齊內容。
  - 同一分類的項目放在一張圓角 16dp 的 `surface` 卡片裡，卡片之間 12dp 間距，卡片左右邊距 16dp；卡片內項目之間 1dp `outline` 分隔線（內縮 16dp）。
  - 項目：標題 16sp `onSurface`，摘要 14sp `onSurfaceMuted`，項目最小高度 56dp（有摘要 72dp），左側圖示（若有）24dp `onSurfaceMuted`。
  - 頂端標題列：返回箭頭＋頁名 20sp，與頁面底同色、捲動時加 1dp 分隔線。
- 對比：所有文字在所有 preset、閱讀色、照片端點上 ≥ 4.5:1（沿用 UiTokens 對比測試，新增 preference 卡片組合）。
- 實作：自訂 `PreferenceFragmentCompat` 的 `RecyclerView` `ItemDecoration`（卡片背景與分隔）與 preference layout（`preference_themed.xml`、`preference_category_themed.xml`），不改 preference key 與行為。

## 5b. 高級感（使用者 10/04「全部修」）

原則：克制與一致——少線條、少裝飾，以字體、間距、色階與動態建立層次。

1. **字體層級（type scale）**：新增 `TypeScale`（res `dimens`＋TextAppearance styles），全 app 只用 5 級：Display 48sp（首頁號碼）、Title 22sp（頁名、詩名）、Heading 16sp medium（分類、卡片標題）、Body 16sp、Caption 13sp。書名、詩名、分類標題用 HymnalKai（霞鷺文楷），按鈕／說明用系統 sans。首頁號碼預覽改 48sp、light/regular 字重、`fontFeatureSettings="tnum"`（等寬數字），輸入時不位移。既有散落的 sp 值收斂到這 5 級（歌詞本文字級由使用者設定，不受影響）。
2. **色塊取代線條**：卡片、鍵盤、清單改用 `surface`／`surfaceTone` 色階分層，移除非必要框線與分隔線（設定頁卡片內分隔保留，見 §5a）；陰影只保留浮動元件（膠囊、Snackbar），其餘 elevation 0。新增 token `surfaceRaised = over(surface, accent @ 4%)`，用於選中與按壓態。灰階一律由 `UiTokens` 推導（帶 accent 色調），不得出現寫死的灰色（lint 掃 `#888`、`@android:color/darker_gray` 等）。
3. **圖示與圓角**：全部圖示換成 Material Symbols Rounded（weight 400、grade 0、optical size 24）向量 drawable，同一 tint 規則；圓角只用 12dp（按鍵、chip）、16dp（卡片）、28dp（面板、bottom sheet、播放卡）。以 `shapeAppearance` styles 統一。
4. **動態與觸感**：
   - 數字鍵、詩歌本選擇按下時 `HapticFeedbackConstants.KEYBOARD_TAP`（遵守系統觸感設定）。
   - 首頁→歌詞頁：號碼以 shared element transition 移到歌詞頁標題號碼位置（Activity transition，`ActivityOptions.makeSceneTransitionAnimation`），250ms、M3 emphasized easing；頁面其餘部分 fade-through 200ms。
   - 目錄／設定全頁：fade-through 200ms；不用彈跳或滑動放大。
   - 系統「移除動畫」（`Settings.Global.ANIMATOR_DURATION_SCALE == 0`）時全部直接切換。
5. **歌詞排版精修**（`ContentView`／歌詞文字頁）：
   - 頁首：大號碼（Display 48sp，accent 色、light 字重）＋其下書名與詩名（Caption／Title，HymnalKai），與本文間距 24dp。
   - 節號：小一級（本文字級 × 0.75）、accent 色、上標對齊，與本文間 0.5em。
   - 副歌（重句）：左縮排 1.5em，字色 `onSurface` 不變，前方細直線 2dp `accent @ 40%`。
   - 行距 1.7、節間距 1.2 行；中英對照時英文用 Caption 級、`onSurfaceMuted`。
   - 若歌詞資料無法辨識副歌（無標記），不縮排（不得誤判）。實作前先盤點歌詞資料格式的副歌標記；無可靠標記則此子項只做節號與頁首。
6. **App 圖示與啟動畫面**：重新設計 adaptive icon（前景：簡化的「詩」字或琴譜意象，單色向量；背景：品牌 accent 純色；附 monochrome layer 供 Android 13 主題圖示）；啟動畫面用 `androidx.core:core-splashscreen`，背景為品牌色、中央圖示，無文字，結束時淡出。圖示設計先產 3 個候選（SVG）給使用者選，選定後才實作。
7. **空狀態**：最近詩歌為空、搜尋無結果、詩歌沒有媒體資源時，顯示 Material Symbols 圖示（48dp、`onSurfaceMuted`）＋一句說明（Body、`onSurfaceMuted`），例如「還沒有最近的詩歌」「找不到相關詩歌」「這首詩歌沒有可播放的媒體」；無媒體時播放鍵停用並顯示此說明（同時根治 1.1.1 遺留閃退的使用者面向）。

## 6. 不做
- 不新增「我的詩歌」入口、不改目錄與設定的項目內容與行為（只改外觀）、不改播放卡版面。

## 7. 測試
- 單元：`DisplayMode.fromPref(null)` = LYRICS_ONLY；最近詩歌可容筆數計算（各高度）；`MainHostTabsTest` 改寫為新導覽結構（移除分頁的測試改成驗證頂列入口）。
- 插樁（api34b、api24b）：
  - 首頁無 `bottom_nav`；直向 `viewMain` 不可捲動且所有控制項完整可見（360×640dp 與 411×891dp 兩種螢幕、系統字級 1.0 與 1.3）。
  - 點「目錄」「設定」開全頁、返回鍵回首頁；`btn_toc` 同路徑；旋轉後停在原頁。
  - 新安裝開歌詞頁預設純歌詞；已設 SCORE_AND_LYRICS 者維持。
  - 歌詞頁頂列首頁鈕與 ⋮「回首頁」都回到首頁並停止播放。
  - 首頁連按兩次返回鍵才離開。
  - 外部播放無可處理 app 時不閃退、顯示 Toast。
- 既有依賴底部導覽的測試（MainHostTest、TocSelectTest、HomeBackgroundTest、OverlayNavigationTest、MainHostTabsTest）依新入口改寫測試步驟（行為斷言不放寬）；依賴預設顯示模式的測試（ContentHandlerReadingTest、LyricsSwipeTest、ReadingPanelTest、ReadingWeightTest、ReadingPreferencesXmlTest、DisplayModeTest、ReadingPrefsTest、ReadingPanelPrefsTest）若斷言舊預設，更新為新預設，其餘斷言不變。
- 插樁補：設定與目錄頁在淺色、深色、閱讀色（豆沙綠）、照片背景下背景色＝推導色；變更閱讀色後回到設定頁立即更新。
- 單元補：TypeScale 只含 5 級（掃 layout 與 styles 中的 sp 值，歌詞本文除外）；`surfaceRaised` 對比；寫死灰色掃描。
- 插樁補：首頁號碼等寬不位移；觸感在系統關閉時不觸發；移除動畫時無 transition；空狀態三種文案顯示；無媒體時播放鍵停用。
- 截圖：歌詞頁首、節號、副歌（若有）；圖示候選；啟動畫面。
- 截圖：設定與目錄頁四種背景；首頁淺色／深色／照片背景，兩種螢幕；目錄與設定全頁；歌詞頁頂列。

## 8. 發佈
- 版本 1.2.0（versionCode 102000），changelog 列出上述四項。完成、全套回歸通過後先提供 APK 給使用者試用，發佈前詢問使用者。

## 9. rev 2 修正（Codex r1）

- **狀態恢復（P1）**：`MainHost.kt:44-63,125-132` 的分頁 add/show/hide 機制整段移除。`HomeFragment` 只在 `savedInstanceState == null` 時 add 一次；目錄／設定以 `replace(R.id.host_container, …)`＋`addToBackStack("toc"|"settings")` 開啟；重建時完全依 FragmentManager 恢復的頂層頁，不再呼叫 `show(tab)`。
- **疊層（P1）**：搜尋與全部記錄 overlay（`MainHost.kt:94-122`，`overlay_container`）與全頁目錄／設定統一用同一 back stack：開啟目錄／設定前先 `popBackStack` 關閉所有 overlay；overlay 只能從首頁開啟。返回順序：overlay → 全頁 → 首頁。
- **滿屏計算（P1）**：`KeypadSizer.kt:10-47` 改為輸入「扣除系統列 inset 後的可用高度」，依序分配：頂列 48dp、詩歌本選擇、預覽（固定）、鍵盤（每列 48–72dp）、最近詩歌（每筆列高 × N，N = 0–5，取放得下的最大值；`RecentChips.kt:31,76` 固定 8 筆改為由 sizer 傳入 N）。鍵盤 48dp 下仍放不下 → 恢復 `NestedScrollView` 捲動（例外路徑）。新增純函式單元測試覆蓋 API 24 小螢幕（360×640dp）與大字級。
- **inset（P1）**：`MainChrome.kt:90-115` 原本只 pad toolbar／bottom nav 並 consume inset；移除兩者後改為把 systemBars＋displayCutout inset 傳給首頁根 view：頂端 padding＝top inset，底部 padding＝bottom inset；目錄／設定全頁同樣處理。API 24 與 API 34 都驗證不壓系統列。
- **顯示模式回退（P1）**：`DisplayMode.kt:29-32` 無歌詞文字時回退值改為 `SCORE_ONLY`（原為 `SCORE_AND_LYRICS`）。
- **外部播放閃退（P1）**：`MediaExoPlayerFragment.java:243-253` 目前先移除 fragment 再無條件 `startActivity`；改為先 `intent.resolveActivity(pm)`（API 30+ 需在 Manifest `<queries>` 宣告 VIEW audio/video intent），無 handler 時 Toast 並 return，**不**移除 fragment；有 handler 時才移除並啟動，`startActivity` 仍包 `try/catch ActivityNotFoundException` 作為最後防線。
- **雙按退出（P2）**：在 `MainActivity.java:371-386` 的 `OnBackPressedCallback` 中，只有 back stack 為空（無 overlay、無全頁）時才啟用雙按邏輯；pop 進行中（`isStateSaved` 或 pending transaction）不計入第一次按壓。
