# 單頁首頁、預設純歌詞、回首頁入口（1.2.0）

> 狀態：rev 1（2026-10-04）。使用者要求：
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

## 6. 不做
- 不新增「我的詩歌」入口、不改目錄與設定頁內容、不改播放卡版面。

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
- 截圖：首頁淺色／深色／照片背景，兩種螢幕；目錄與設定全頁；歌詞頁頂列。

## 8. 發佈
- 版本 1.2.0（versionCode 102000），changelog 列出上述四項。完成、全套回歸通過後先提供 APK 給使用者試用，發佈前詢問使用者。
