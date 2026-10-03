# 可收合播放列（1.1）

> 狀態：rev 2（2026-10-03）。Codex r1：P1×3、P2×3、P3×1 已修正（隱藏／收合拆成兩個狀態、單一 inset 公式、橫向規則、膠囊宿主、inset 通知、狀態保存、無障礙描述）。使用者要求：播放列可以點擊或下滑隱藏，縮到右下角，再點就恢復；請參考成熟設計、做得優雅。

## 1. 參考與取捨

| 參考 | 做法 | 取用 |
|---|---|---|
| Material 3 Bottom Sheet | 頂部拖曳把手（32×4dp）、跟手拖曳、依速度／位置吸附到狀態 | 把手、跟手拖曳、吸附規則 |
| Spotify／YouTube Music 迷你播放器 | 展開播放器 ↔ 底部細長迷你列，可直接播放／暫停 | 迷你態可直接播放／暫停（見 §3 的誤觸防護） |
| Apple Music／iOS 的 Now Playing 小膠囊 | 圓角膠囊、進度以細線呈現 | 膠囊外形、進度環 |
| Material Motion「Container transform」 | 元件在兩種形態間以同一容器連續變形 | 卡片 ↔ 膠囊的轉場 |

結論：不做全寬迷你列（會吃掉歌詞的一整行高度）；做**右下角的浮動膠囊**，符合使用者要求，也最省歌詞空間。

## 2. 兩種狀態

- **展開**：現行播放卡（PR #17 的樣式），頂部新增 M3 拖曳把手，右上角新增收合鈕（⌄，48dp 觸控，contentDescription「收合播放列」）。
- **收合**：右下角浮動膠囊，貼齊右邊距 16dp、底邊距 16dp＋系統導覽 inset；高 56dp。內容：
  - 播放中／暫停中：左側 40dp 圓形播放／暫停鍵（`accent` 底、`onAccent` 圖示，外圈 3dp 進度環 `accent` 與 `surfaceTone` 軌道），右側一個 ⌃ 展開區（點擊展開）。整體寬約 104dp。
  - 未播放（播放卡開著但沒在播）：只顯示 56dp 圓形「♪」按鈕（點擊展開），不顯示進度環。
  - 膠囊底色 `surface`＋陰影 6dp；配色走 `UiTokens`（歌詞頁 `applyReadingTheme()` 傳播點）。

## 3. 互動

- **收合**：(a) 點收合鈕；(b) 在卡片上向下拖曳（從把手或卡片非控制區開始），跟手移動與縮小，放開時位移 > 卡高 40% 或向下速度 > 1000dp/s 即收合，否則彈回展開。
- **展開**：(a) 點膠囊的 ⌃ 區或「♪」鈕；(b) 在膠囊上向上拖曳。
- **膠囊上的播放／暫停**（參考成熟迷你播放器）：直接切換播放／暫停。誤觸防護：只有 40dp 圓鈕本身是播放鍵，與 ⌃ 區之間留 8dp 間隔；按下有觸覺回饋（`HapticFeedbackConstants.CONFIRM`／API 24 用 `VIRTUAL_KEY`）。
- **不做**：點卡片任意處收合（與播放鍵、進度條、來源鈕衝突）；長按（F1 政策：沒有隱藏的長按）。
- **手勢衝突**：向下拖曳收合只在卡片的「非控制區」（把手、標題列空白處）起手才生效；在進度條、按鈕上起手的拖曳交給該控制項；與歌詞捲動、左右翻頁（`NestedScrollableHost`）互不干擾：卡片在歌詞捲動層之外，手勢不穿透。
- **轉場**：Container transform（卡片 ↔ 膠囊同一容器連續變形，300ms，M3 emphasized easing）；系統「移除動畫」時直接切換。展開／收合完成時觸覺回饋一次。

## 4. 狀態與生命週期

- **兩個獨立狀態**（由新的 `PlayerSheetController` 持有，`ContentHandler.onSaveInstanceState` 保存，冷啟動預設值如下）：
  - `userHidden: Boolean`——使用者用 ⋮「隱藏／顯示播放條」切換；冷啟動預設 `false`。為 true 時卡片與膠囊都不顯示。
  - `collapsed: Boolean`——展開或收合；冷啟動預設 `false`（展開）。
  - ⋮ 選單「隱藏／顯示播放條」的文字與動作只讀寫 `userHidden`，**不再**以 `MediaGuiController.isShown()`（卡片可見性）判斷（`ContentHandler.java` 約 691 行需改）；顯示時回到 `collapsed` 記錄的形態。
- 換首、翻頁時兩個狀態都保持。換首停止播放（PR #17）後，若為收合，膠囊變為未播放的「♪」形態，不自動展開。
- **橫向**：現行 `onConfigurationChanged()`（約 1834 行）在橫向一律隱藏播放列以騰出歌詞空間。新規則：橫向時強制顯示為膠囊（不顯示展開卡片），但不改寫 `collapsed`；使用者在橫向點 ⌃ 可暫時展開卡片（浮在歌詞上），轉回直向後恢復直向時記錄的 `collapsed` 形態。`userHidden` 為 true 時橫向也不顯示膠囊。
- 膠囊不參與工具列自動隱藏（`ChromeController`），一直可見（`userHidden` 除外）。
- **歌詞底部 inset（唯一公式）**：`bottomPadding = systemBottomInset + 8dp + playerReserve`，其中 `playerReserve` = 展開時「播放卡實測高度」、收合時「56dp（膠囊）＋16dp（膠囊底距）」、`userHidden` 時 0。下方三鈕的高度另依現行 `LyricsInsets` 規則疊加。`PlayerSheetController` 在狀態改變或播放卡量測完成時，經 activity 通知所有已建立的 `ContentView`（`livePages()`）重算 inset（取代目前 `ContentView` 傳入的固定 0）；未建立的頁在 `onViewCreated` 讀取目前值。

## 5. 無障礙

- 收合鈕：「收合播放列」；膠囊：播放鍵「播放 補充本 37」／「暫停 補充本 37」（視覺上不顯示曲名，只在 contentDescription 附加目前的 `hymnInfo`；資料未載入時用「播放」／「暫停」），⌃ 區「展開播放列」；未播放「♪」：「展開播放列」。
- TalkBack 開啟時不需要手勢即可完成所有操作；拖曳收合在 TalkBack 下停用（避免與探索手勢衝突），以按鈕操作。
- 膠囊觸控目標皆 ≥ 48dp（圓鈕 40dp 視覺、48dp 觸控）。

## 6. 實作位置（建議）

- 膠囊只放在 activity 級的 `content_main.xml`（CRLF）overlay，**不可**放在 `content_lyrics.xml`（那是每個 ViewPager2 頁面的版面，會產生多個膠囊）；把手與收合鈕放在播放卡（`MediaGuiController` 的版面）；新增純邏輯 `PlayerSheetState`（展開／收合、拖曳吸附判斷，可單元測試）與 `PlayerSheetController`（綁定 view、動畫、inset、與 `ChromeController` 協作）。
- 進度環由播放器既有的進度更新驅動（不新增計時器）。

## 7. 測試

- 單元：吸附判斷（位移比例、速度）、狀態保持、隱藏播放條與收合的組合、未播放／播放中形態。
- 插樁（api24b、api34b）：點收合鈕 → 膠囊出現、卡片消失；點 ⌃ → 展開；膠囊播放鍵切換播放／暫停；向下拖曳（從把手）收合、從進度條拖曳不收合；換首後保持收合且變為「♪」；直向收合 → 轉橫向顯示膠囊 → 轉回直向仍收合；直向展開 → 轉橫向顯示膠囊 → 轉回直向恢復展開；隱藏播放條後收合／展開都不顯示，再顯示回到原形態；收合後所有已建立頁的歌詞底部 padding 依公式更新；TalkBack 標籤；收合時歌詞可捲到膠囊上方。
- 截圖：淺色、深色、照片背景下的展開與收合（播放中、未播放）。

## 8. 排程

依賴 PR #18（字重，動 `ContentView`／Aa 面板）與首頁固定高度修正合併後實作，避免與歌詞頁檔案衝突；與它們一起進 preview6 或緊接其後的 preview7。
