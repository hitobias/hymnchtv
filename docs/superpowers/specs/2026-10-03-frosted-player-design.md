# 播放面板毛玻璃（1.1.1）

> 狀態：**rev 2 定案**（2026-10-03）。Codex r1：P1×1（依賴來源）、P2×5、P3×2 均照建議修正。使用者要求：播放面板做成透明毛玻璃質感；放進 1.1.1。

## 1. 範圍
- 做：展開的播放卡（`MediaGuiController` 版面）與收合膠囊（`PlayerCapsuleView`，activity 級 overlay）改為毛玻璃底。
- 不做：頂列、下方三鈕、首頁卡片（維持現狀；之後視使用者回饋再議）。

## 2. 技術做法
- 使用開源元件 **BlurView**（`com.github.Dimezis:BlurView`，Apache-2.0；版本以 JitPack／Maven 上的最新穩定版為準，需確認支援 AGP 9／minSdk 24），以 `BlurTarget`（或該版本對應的根容器 API）包住歌詞 pager 與背景，播放卡與膠囊各自放一個 `BlurView` 當底。
- 依 API 分級：
  - **API 31+**：BlurView 的 `RenderEffect` 實作即時模糊，模糊半徑 24dp，捲動歌詞時即時更新。
  - **API 24–30**：不做即時模糊（避免 RenderScript 的效能與相容性問題），改用不透明度較高的磨砂色底（見 §3 的 fallback）。
- 授權：在開源授權頁（AboutLibraries）列出 BlurView（Apache-2.0）。

## 3. 色彩與可讀性（沿用 UiTokens）
- 玻璃疊色 `glassTint`：`surface` 色、alpha 0.72（API 31+ 模糊時）；API 24–30 fallback 為 `surface` alpha 0.94（不模糊）。
- 對比保證：卡片上的文字（`onSurface`、`onSurfaceMuted`、`onAccent` on `accent` 的播放鍵）在「玻璃疊色合成在每個背景 swatch 上、且模糊後的歌詞文字色塊為最不利情況」下仍須 ≥ 4.5:1。最不利情況以「背景 swatch 與歌詞文字色 `textColor` 以 50% 混合後的顏色」近似模糊後的文字區域；`UiTokens` 新增 `glassTint` 計算：若某 swatch 下不足，逐步提高 alpha（上限 0.94）。單元測試涵蓋所有 preset、閱讀色與照片端點。
- 邊緣：卡片 1dp `outline`（token），膠囊陰影維持。
- 使用者開啟系統「高對比文字」（`AccessibilityManager` / `Settings.Secure.ACCESSIBILITY_HIGH_TEXT_CONTRAST_ENABLED`）時改用不透明 `surface`。

## 4. 效能與穩定性
- 只有播放卡或膠囊可見時才啟用模糊；隱藏或收合時停用卡片的 BlurView。
- API 31+ 在捲動中每幀更新；若量測到掉幀（`FrameMetrics` 或 `dumpsys gfxinfo` 平均幀時 > 16ms 的比例明顯上升），降低模糊半徑或改為捲動停止後才更新，並在回報中說明。
- 不得影響現有的手勢（`NestedScrollableHost`、拖曳收合、單擊顯示工具列）與 inset 公式。
- 記憶體：API 24（heap 48MB）下不得增加常駐 bitmap（fallback 不建立 bitmap）。

## 5. 測試
- 單元：`glassTint` 對比矩陣（所有 preset／閱讀色／照片端點，作用中文字 ≥ 4.5:1）。
- 插樁：API 34 播放卡與膠囊的底層 view 為 BlurView 且啟用；API 24 為 fallback 純色且無 blur bitmap；收合／展開／隱藏時模糊啟用狀態正確；既有 `CollapsiblePlayerTest`、`LyricsChromeTest`、`PlaybackStopsOnHymnChangeTest`、`LyricsSwipeTest` 通過。
- 效能：API 34 上捲動歌詞時 `dumpsys gfxinfo` 摘要（janky frames 比例）與未模糊時比較，寫入 PR。
- 截圖：淺色、深色、照片、閱讀色一種，展開與收合，API 34 與 API 24 各一組。

## 6. rev 2 定案補充（Codex r1）

- **依賴（P1）**：鎖定 `implementation 'com.github.Dimezis:BlurView:version-3.2.0'`；root `build.gradle` 的 repositories 加 `maven { url 'https://jitpack.io' }`（只供此套件，可用 `content { includeGroup 'com.github.Dimezis' }` 限定）。不得使用 latest／snapshot。AboutLibraries 授權閘門須以 release 建置驗證其 POM 被辨識為 Apache-2.0。
- **View 樹**：把「背景 ImageView ＋ `ViewPager2`」包進獨立的 `BlurTarget`；卡片宿主與膠囊是 `BlurTarget` 的 sibling（絕不可 target 整個 `content_main` root，也不可讓 BlurTarget 包含指向自己的 BlurView）。卡片：以 FrameLayout 承載，底層 BlurView（match_parent）＋上層既有內容（`SheetDragLinearLayout` 保留拖曳行為）；不可把 BlurView 當 LinearLayout 的一個子列。膠囊同理。
- **glassTint 定義**：`glassTint = (surface & 0x00FFFFFF) | (alpha << 24)`，alpha 為最終 ARGB alpha（API 31+ 起始 0.72，fallback 0.94），不與 surface 原有 alpha 相乘。對比測試流程：`blurApprox = blend(swatch, textColor, 0.5)` → `glass = over(blurApprox, glassTint)` → 再依實際層級合成 `surfaceTone` chip、停用 chip、`outline`、`accent`／`onAccent`，逐一驗證門檻；不足時提高 alpha（上限 0.94）。
- **高對比模式**：使用新 token `opaqueGlassTint = (surface & 0x00FFFFFF) | 0xFF000000`，重新驗證所有文字／chip 對比；監聽設定變更（`AccessibilityManager.addHighTextContrastStateChangeListener` 或於 `onResume` 重讀）後立即重套。
- **轉場**：卡片或膠囊只要 alpha > 0 就啟用模糊；`GONE` 後才 `setBlurEnabled(false)`；轉場中兩者可同時啟用。
- **更新機制**：以 BlurView 的 `setBlurAutoUpdate(true)`／正常 hierarchy 更新為準，不新增 scroll listener 逐幀重繪。效能量測：API 34 同一路徑（打開同一首、捲動到底再回頂、各 3 次），比較開啟／關閉毛玻璃的 `dumpsys gfxinfo` janky frames 百分比，增幅 > 5 個百分點時降低模糊半徑並回報。
- **圓角**：卡片與膠囊以 rounded background＋`outlineProvider = BACKGROUND`＋`clipToOutline = true` 裁切模糊內容。
- **inset**：BlurView 容器不新增 inset 處理、不改 `PlayerSheetController` 的 reserve 計算；補測 IME 開啟、橫向 cutout、高對比文字。
