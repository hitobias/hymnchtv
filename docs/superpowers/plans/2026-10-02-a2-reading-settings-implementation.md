# 子項目 A2：閱讀設定 實作計畫（含設計）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**修訂紀錄：**
- rev 2（2026-10-02）：納入使用者決策（預設淺色主題、文楷為預設字型、修正 `xg171.txt` 的私用區字元）與 Codex 審查（照片背景的文字底板、星點納入對比、字型涵蓋 strings 且無例外、API 24 的 margin、只顯示詞時釋放樂譜、instrumented test、簡化字型測試）。

## 給執行者（Sonnet 5.5）的說明

使用者決定由 **Sonnet 5.5（`claude-sonnet-5-5`）** 子代理執行這份計畫，可以多個子代理在各自的 git worktree 裡平行進行。主對話（協調者）只負責開 worktree、派工、合併與最後驗證。

**開工前：** 主計畫（`2026-10-02-hymnchtv-modernization-plan.md`）規定 A2「開工前要先設計並經 Codex 審查」。本文件就是那份設計＋計畫；協調者要先跑一次 `/codex review`（或 `codex` 的 consult 模式，把本檔交給它），處理完 P1 意見再開始 Task 0。

**啟動方式（協調者）：**

1. 在 `/Users/hitobias/orca/hymnchtv` 開新的 Claude Code session，`claude --model claude-sonnet-5-5`。
2. 確認子項目 A 已完成（`feat/zh-hant` 的 Task 15 已通過），然後從它的最新 commit 開分支（見 Task 0 Step 1）。
3. 指示：

   > 使用 superpowers:subagent-driven-development 執行 `docs/superpowers/plans/2026-10-02-a2-reading-settings-implementation.md`。依「平行化地圖」分階段派工：同一階段的 lane 各開一個 worktree、各派一個 Sonnet 子代理；lane 內的 task 依序做。每個 lane 做完就合併回 `feat/reading-settings`，同一階段全部合併後才開下一階段。計畫和程式碼對不上就停下來問我。

**執行規則：**
- **階段順序**：階段 0 → 階段 1（P、B、F 三條 lane 平行）→ 階段 2（S）→ 階段 3（I、M 兩條 lane 平行）→ 階段 4（C1 → V1 → R1）。
- **每條 lane 只改自己「檔案範圍」裡的檔案。** 子代理發現必須改範圍外的檔案時，停下來回報，不要自己改。這是平行執行不衝突的前提。
- **模擬器只在 V1 使用。** 平行 lane 一律不啟動模擬器（另一個代理可能正在用 `api24`／`api34`）。
- **每個 task 結束前**都要跑該 task 寫明的測試指令；整合型 task（I1、I2、M1、C1）另外要跑 `./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug --console=plain`。
- **計畫和程式碼對不上就停下來**：例如找不到計畫引用的原始碼片段，回報給協調者，不要自行改變計畫意圖。
- **人工確認項目**：字型產生工具（Task F2）若回報缺字，表示歌詞或字串有錯字，要交給使用者確認；沒有例外清單可加。
- **最後的審查**：R1 用 code-reviewer 子代理和 `/codex review` 兩種方式；有 P1 先修好才開 PR。

**worktree 操作（協調者）：**

```bash
# 開一條 lane（以 lane P 為例；B、F、S、I、M 同理，名稱換掉即可）
git -C /Users/hitobias/orca/hymnchtv worktree add ../hymnchtv-a2-p -b a2/lane-p feat/reading-settings
cp /Users/hitobias/orca/hymnchtv/local.properties ../hymnchtv-a2-p/   # local.properties 不在 git 裡
# 子代理在 /Users/hitobias/orca/hymnchtv-a2-p 裡工作、commit
# lane 完成後合併並移除 worktree
git -C /Users/hitobias/orca/hymnchtv switch feat/reading-settings
git -C /Users/hitobias/orca/hymnchtv merge --no-ff a2/lane-p -m "merge: A2 lane P (reading logic)"
git -C /Users/hitobias/orca/hymnchtv worktree remove ../hymnchtv-a2-p
git -C /Users/hitobias/orca/hymnchtv branch -d a2/lane-p
```

同一階段的 lane 檔案互不重疊，合併不應出現衝突；若出現衝突，表示有子代理越界，停下來檢查。每次合併後在主 checkout 跑一次 `./gradlew :hymnchtv:testDebugUnitTest --console=plain`。同時跑的 Gradle build 最多 3 個（每個 daemon 約 2 GB 記憶體）。

**Goal:** 新增「閱讀設定」畫面與對應行為：
- 顯示模式（譜＋詞／只顯示譜／只顯示詞），歌詞頁有按鈕可以臨時切換。
- 歌詞預設字級（小／中／大／特大），和手勢縮放整合。
- 歌詞字型（系統字型／文楷：內建 LXGW WenKai 子集，改名 HymnalKai）。
- 20 款程式繪製的背景，主頁與歌詞頁分開選；保留自訂照片並加上變暗與模糊。
- 翻頁動畫、播放器預設顯示、螢幕常亮三個開關。
- 修正 `showLyricsScore` 第 5 頁樂譜蓋掉第 4 頁的 bug；刪除舊的 12 張 JPG 桌布（同時解決 B-5）。
- App 預設主題改為淺色（使用者決策）；修正 `lyrics_xg_text/xg171.txt` 的私用區字元。

**Architecture:**
- 判斷規則寫成 Kotlin 純函式（`reading/`、`reading/background/`），JVM 單元測試，TDD。
- Android 邊界集中在幾個薄的 Kotlin 物件：`ReadingPrefs`、`BackgroundPrefs`、`BackgroundApplier`、`BackgroundDrawables`、`LyricsTypefaces`。
- 設定畫面用 **androidx.preference**（`PreferenceFragmentCompat`），放在新的 `ReadingSettingsActivity`。舊的 `ChineseS2TSelection` 併入後刪除。
- 背景資源（layer-list、向量圖、紋理 PNG）由 `tools/gen_backgrounds.py` 產生；字型子集由 `tools/gen_font_subset.py` 產生。兩者都可重現，並有同步測試。
- 既有 Java（`ContentView`、`ContentHandler`、`MainActivity`）只改呼叫點；`ContentView` 因改動多，整檔替換。

**Tech Stack:** Android（minSdk 24、compileSdk 37、AGP 9.3.3 內建 Kotlin）、Java 11 與 Kotlin 混用、androidx.preference 1.2.1、JUnit 4.13.2、Truth 1.4.5、Python 3 標準函式庫（背景產生器）、fontTools 4.66.1（字型子集，放在 git 忽略的 venv）。

**規格來源:** `docs/superpowers/plans/2026-10-02-hymnchtv-modernization-plan.md` 的「子項目 A2」；背景樣式來源是選樣頁（https://claude.ai/artifact/V3fLKCbvjwetQLyMty8bZi）的 `shelves` 陣列，色值已全部抄進本計畫。

**分支:** `feat/reading-settings`（Task 0 建立）。

**Commit 規則:**
- conventional commits；每個 task 一個 commit（訊息寫在各 task 最後一步）。
- 每個 commit 訊息結尾加上：

  ```
  Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>
  ```

  以下各 task 的 `git commit -m "..."` 都要用 `-m "<標題>" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"` 的形式補上這一行。

---

## 設計決策

### D1. 設定畫面：androidx.preference，不擴充 `ChineseS2TSelection`

A2 有 12 個設定項（4 個清單、3 個開關、2 個滑桿、2 個背景挑選、加上 A 留下的繁中字形）。`ChineseS2TSelection` 是手寫 RadioGroup＋確定／取消，每加一項都要寫 view 綁定、讀寫 pref、變更偵測，會變成 600 行以上的 Activity。

`PreferenceFragmentCompat` 的優點：
- 宣告式 XML，自動讀寫 `SharedPreferences`（指定檔名 `Settings`，和現有 pref 同一個檔）。
- 子項目 C 的設定頁可以直接把這個 Fragment 放進去，或把 XML 併進它的 PreferenceScreen，不必重寫。
- 立即生效、沒有確定／取消，符合 Android 設定頁慣例。

代價是多一個依賴（約 100 KB）。結論：用 androidx.preference。`ChineseS2TSelection` 的功能（歌詞預設語言、隱藏中的繁中字形、非法值自我修復）全部搬進新畫面，然後刪除。主選單「歌詞語言」改名為「閱讀設定」。

「變更後重建歌詞頁」的機制沿用 A：`ReadingSettingsActivity` 監聽 pref 變更，結束時回傳 `EXTR_KEY_HAS_CHANGES`；`ContentHandler` 收到 `true` 就清掉兩個臨時切換並 `recreate()`。

### D2. 顯示模式（回答主計畫 A2 的三個待決問題）

- `DisplayMode`：`SCORE_AND_LYRICS`（預設）、`SCORE_ONLY`、`LYRICS_ONLY`。
- 歌詞頁按鈕列新增 `button_mode`，按一下依「譜＋詞 → 只顯示譜 → 只顯示詞 → 譜＋詞」循環；按鈕文字顯示目前模式；長按開啟閱讀設定（和 `button_ts` 長按相同）。
- 臨時切換存在 `ContentHandler.displayModeOverride`（`null` = 依設定），翻頁保持、旋轉與 Activity 重建時保存，離開歌詞頁就消失。和 A 的 `lyricsViewOverride` 同一模式。
- **沒有歌詞文字的詩歌**（文字少於 40 字，和既有「教唱提示」同一門檻）：「只顯示詞」自動改成「譜＋詞」，頁面永遠不會是空的。按鈕仍顯示使用者選的模式。
- **只顯示譜**：隱藏簡繁按鈕、英文按鈕和所有歌詞 view，只留下 `button_mode`；不觸發教唱提示（`selectJC`）。歌詞文字檔仍然讀取（每首幾 KB，用來判斷有沒有歌詞）。
- **只顯示詞**：不載入樂譜圖片（省下 Glide 解碼，這是舊手機最重的部分）；從含譜的模式切到「只顯示詞」時，用 `Glide.with(fragment).clear(view)` 清掉 5 個 `ImageView`，讓點陣圖可以被回收；切回含譜的模式時重新載入（I2 的 instrumented test 與 V1 都會檢查）。
- **橫向**：三種模式行為相同，沒有特別處理；橫向本來就有自己的字級縮放（`LyricsScaleL`）。
- 按鈕列在「只顯示譜」時位於樂譜下方，要往下捲才看得到；所以 `menu_content`（長按歌詞頁的選單）也加入「閱讀設定」。

### D3. 歌詞字級

- `LyricsFontSize`：小 1.0、中 1.25、大 1.5、特大 2.0（倍率）。
- 基準字級改為直向 16 sp、橫向 28 sp，所以「中」= 20 sp／35 sp，等於現在的預設值；「小」= 現行的最小值（`ZoomTextView` 的倍率下限 1.0）。
- `LyricsScaleP／L` 有值就用（手勢調過的結果），沒有值就用所選字級的倍率。
- 在設定裡選字級時，同時把 `LyricsScaleP` 與 `LyricsScaleL` 重設為該倍率；之後手勢從這裡繼續調整。
- 英文歌詞（WebView）的縮放維持原狀，不受字級影響。

### D4. 歌詞字型

- `LyricsFont`：`SYSTEM`、`KAI`，**預設 `KAI`（使用者決策，2026-10-02）**；`LyricsFontSizeTest`、`ReadingPrefsTest`、`ReadingPreferencesXmlTest` 都檢查這個預設。
- 簡體歌詞 view 用 `hymnal_kai_sc.ttf`，繁體 view 用 `hymnal_kai_tc.ttf`。
- `LyricsTypefaces` 在背景執行緒預先載入，失敗時回傳 `null`，呼叫端退回系統字型，不會當機。
- 子集化：`tools/gen_font_subset.py`。簡體字型必須涵蓋「簡體歌詞＋所有 strings 檔＋ASCII」的每一個字元；繁體字型必須涵蓋「台灣與香港兩套繁體歌詞＋所有 strings 檔＋ASCII」。**沒有例外清單**：缺任何一個字，產生工具和測試都會失敗。
- `lyrics_xg_text/xg171.txt` 第 37 行有一個私用區字元 `U+E5F2`（在「“这么大的救恩”」的右引號之後），是原始資料的錯字。使用者決定改成全形逗號「，」（Task F0），修正後兩套字型就沒有缺字。
- 2026-10-02 實測（已套用 F0 的修正與 Task 0 的新字串）：簡體 1.49 MB、需涵蓋 3,224 個字元；繁體 2.10 MB、3,249 個字元；兩者缺字都是 0。
- 改名：刪除所有命名紀錄（name ID 1、2、3、4、6、16、17、18、21、22、25），改成 `HymnalKai SC`／`HymnalKai TC`；保留版權（0）與授權（13、14）紀錄。
- 同步測試（Codex 建議，取代原本手寫的 TTF 解析器）：產生工具用 fontTools 讀出字型實際的 `cmap` 和命名紀錄，寫進 `tools/font_subset_manifest.txt`（每個字型一行 SHA-256、命名紀錄、涵蓋的字元範圍）。`FontSubsetTest` 只做三件事：字型檔的 SHA-256 等於 manifest（所以 manifest 的涵蓋範圍確實描述這個檔案）；歌詞與 strings 的每個字元都在涵蓋範圍內；命名紀錄沒有保留字型名稱。產生工具本身的雜湊不記錄，因為它不保護任何東西。
- 授權：`assets/licenses/OFL-HymnalKai.txt`（說明＋兩份原始 OFL 全文）；About 頁新增一行致謝，點擊顯示授權全文。

### D5. 背景

**模型**（`reading/background/`，純 Kotlin）：
- `BackgroundPreset`：20 個 enum，順序就是挑選畫面的順序。每個帶有 `id`、分類、漸層色標 `stops`、半透明疊層 `overlays`（紋理、圖案、光芒，記錄最強處的 alpha）、文字色、強調色。
- `swatches()` = 每個色標本身＋每個色標被每種疊層以最大 alpha 蓋過的顏色。疊層包括所有裝飾：雜訊紋理、拼貼線條、角落圖案的每一種顏色、光芒、**星夜的星點**。文字可能壓在任何裝飾上，所以不靠「文字不會碰到裝飾」，而是把裝飾都算進對比測試。
- `BackgroundSlot`：`MAIN`（pref `MainBackground`，預設晨曦）、`LYRICS`（pref `LyricsBackground`，預設宣紙白）。
- `BackgroundPolicy.resolve(stored, slot, darkTheme, photoAvailable)`：
  - 存的是有效 id，就用它。
  - 存的是 `photo` 且照片檔存在，就用照片。
  - 其他情況（沒選過、非法值、照片不見）→ 深色主題用「夜讀」，否則用該 slot 的預設。
- `ReadingPalette`：文字色、強調色、是否深色、樂譜紙色、文字底板色 `backdropColor`（0 = 不需要底板）。預設背景不用底板。
- **照片背景（Codex P1）**：照片可能是任何顏色，變暗滑桿無法保證可讀。所以照片背景一律在歌詞文字、英文歌詞、主頁提示文字和背景挑選畫面照片格的範例字後面加一塊底板（`PhotoBackdropTest` 在裝置上逐一檢查），顏色 `#1e1e1e`、不透明度 85%（`0xD91E1E1E`，圓角 10 dp）；文字 `#f2efe8`、強調 `#e3b77a`。`PhotoPaletteTest` 證明：照片像素從純黑到純白（每 15 一階，含兩端；混色對亮度是單調的，所以兩端就是最差情況），經過底板後文字對比 ≥ 4.5、強調 ≥ 3.0（2026-10-02 試算：純白最差，文字 9.03、強調 5.59）。變暗與模糊只是外觀選項，不影響這個保證。

**對比測試**：每款背景的文字色對所有 swatch ≥ 4.5:1（WCAG AA 一般文字）；強調色 ≥ 3.0:1（WCAG AA 大字，強調色只用在 18 sp 粗體以上的提示文字、勾選標記，以及歌詞的連結與選取底色）。2026-10-02 用相同算法試算：文字最低 4.81（星夜，壓在最亮的星點上），強調最低 3.37（羊皮紙），全部通過。

**保證的範圍**：測試證明的是「宣告的色標與裝飾顏色在最強處」的對比，以及照片底板對任意像素的對比。不保證的：漸層色標之間的過渡色（亮度介於兩個色標之間，不會比較差）、反鋸齒邊緣、使用者自行用「字型顏色」選單改過的主頁按鈕文字（按鈕有自己的底圖）。

**繪製方式**（每個檔案 ≤ 5 KB，`BackgroundResourcesTest` 檢查）：

| 背景 | 做法 |
|---|---|
| 宣紙白、羊皮紙、夜讀、墨色 | 底色或漸層＋`bgx_tex_<id>.png`（96×96 px、4-bit 調色盤、16 階 alpha 的可拼貼雜訊，`drawable-nodpi`，`tileMode="repeat"`） |
| 亞麻灰 | 底色＋`bgx_tile_linen.png`（xxhdpi 9×9 px，每 3 dp 一條 1 dp 細線） |
| 晨霧藍 | 純色 |
| 晨曦、天光、麥田金、暮色、青草地 | `GradientDrawable` 三色線性漸層；中間色的位置用 `centerY`（Android 對三色漸層會把 `centerY` 當成中間色標位置）。麥田金原本是 170°，`GradientDrawable` 只能用 45° 的倍數，改為由上而下 |
| 羊皮紙 | 放射漸層，半徑 `115%p`；CSS 的橢圓改成圓形近似 |
| 五線譜 | 底色＋`bgx_tile_staff.png`（xxhdpi 720×192 px，240×64 dp 一格，5 條線） |
| 橄欖枝、麥穗、白鴿、葡萄樹 | 底色或漸層＋角落的 VectorDrawable（`bgx_motif_<id>.xml`），用 layer-list 的 `gravity`、`width`、`height` 和負的 inset 定位，和 CSS 的位置相同 |
| 活水 | 漸層＋底部兩排波浪的 VectorDrawable（360×62 dp，`bottom\|fill_horizontal`）；寬螢幕時波長會被等比拉長 |
| 光芒 | 漸層（layer-list）＋`RaysDrawable`（程式繪製的 30 道扇形，等同 CSS `repeating-conic-gradient`；用 Path 直接畫，不佔整張螢幕大小的點陣快取） |
| 星夜 | 漸層＋`StarsDrawable`（5 顆淡星，位置照 CSS 的百分比，半徑放大 1.5 倍）。CSS 的星點最亮 alpha 0.8，壓在上面的淺色字對比只剩約 1.4，所以**最亮的星點改為 0.28**（文字 4.81、強調 3.99 的上限），其餘依原比例縮小。這個上限寫在註冊表的 STARRY 疊層，`StarsDrawable` 直接讀它 |
| 深海 | 漸層 |

**套用方式**：主頁與歌詞頁的根 layout 各加一個全螢幕 `ImageView`（`mainBackground`、`lyricsBackground`）放在最底層。預設背景設成它的 background；照片用 `centerCrop` 顯示，`setColorFilter(SRC_ATOP 黑色)` 做變暗，API 31 以上用 `RenderEffect` 模糊（只模糊這個 view，不影響上面的按鈕與歌詞），API 31 以下模糊滑桿停用並顯示「需要 Android 12 以上」。照片用 `inSampleSize` 縮到螢幕大小再解碼。

**深色背景的樂譜**：樂譜 PNG 是不透明的白底。`ScoreTintPolicy`：
- 手動等級（長按選單「樂譜顏色」循環 0～3）：1～3 沿用原本的反相倍率 −0.9／−0.8／−0.7。
- 等級 0 = 自動：淺色背景不處理；深色背景（含照片）用「雙色調」矩陣，白紙變成背景底色、黑色音符變成文字色。

**主頁**：主頁按鈕有自己的底圖，字色仍由「字型顏色」選單決定；背景的強調色套用到頂端的提示文字（原本寫死紅色 `red800`，在深色背景上看不清楚）。

**英文歌詞**：WebView 的 HTML 原本依 App 主題選字色。改成依歌詞頁背景是否深色決定（`LyricsEnglishRecord.setDarkBackground`），否則深色主題配淺色背景時會變成白字白底。

**預設與主題（使用者決策，2026-10-02）**：App 的預設主題改為**淺色**（Task P5：`ThemeHelper.DEFAULT_THEME = LIGHT`；Task M1：`MainActivity` 讀 `PREF_THEME` 的預設值改用它）。所以全新安裝的使用者看到歌詞頁「宣紙白」、主頁「晨曦」；使用者自己切到深色主題後，沒選過背景的位置改用「夜讀」。使用者選過的背景永遠優先。

### D6. 其他開關

- 翻頁動畫（`PageAnimation`，預設開）：關閉時不設定 `DepthPageTransformer`，ViewPager2 用預設的滑動。子項目 B-11 之後會讓低階手機預設關閉，共用同一個 key。
- 播放器預設顯示（`MenuShow`，沿用 `PREF_MENU_SHOW`，預設開）：歌詞頁選單的「預設顯示／預設隱藏」兩項移除，保留「切換播放器」這個當次操作。
- 螢幕常亮（`KeepScreenOn`，預設開）：取代 A 在 `ContentHandler.onResume` 的固定行為。

### D7. 舊資源

刪除 `drawable-ldpi/bg*.jpg`（12 張，共 936 KB）、`MainActivity.bgResId`、`setBgColor`、主選單「桌布」子選單與 `PREF_BACKGROUND`。全新項目，不做偏好遷移。

### 和主計畫不同的地方（刻意的取捨）

- 主計畫寫「每款不到 5 KB」：紋理 PNG 約 4.3 KB、最大的向量圖約 2.7 KB，符合。光芒與星夜的程式繪製部分沒有檔案。
- 主計畫寫「只顯示譜時不做繁簡轉換」：A 之後已經沒有執行時轉換，只是讀檔；仍會讀文字檔來判斷有沒有歌詞。
- 主計畫寫「字元清單的雜湊寫進 manifest」：改成 manifest 直接記錄字型的涵蓋範圍，並以字型檔的 SHA-256 綁定，測試檢查歌詞與 strings 的字元是否都在範圍內。

### 已決定（rev 2，使用者，2026-10-02）

1. App 預設主題改為淺色（Task P5、M1）。
2. 歌詞字型預設「文楷」。
3. `xg171.txt` 的 `U+E5F2` 改為「，」並重新產生繁體歌詞（Task F0）；字型測試不留任何例外。

### 已知限制

- `ThemeHelper` 的主題存在靜態欄位，只有 `MainActivity.onCreate` 會從 pref 讀入。如果系統回收 process 後直接還原到 `ContentHandler`，會用預設（淺色）主題，直到回到主頁。這是既有行為（之前是預設深色），A2 不處理，記錄給子項目 C。

---

## 平行化地圖

| 階段 | Lane | Tasks（lane 內依序） | 檔案範圍（只能改這些） | 前置 |
|---|---|---|---|---|
| 0 | — | Task 0 | `hymnchtv/build.gradle`、`.gitignore`、`res/values*/strings_reading.xml`、`res/values/reading_arrays.xml` | A 完成 |
| 1 | P | P1 → P2 → P3 → P4 → P5 | `java/.../reading/{ReadingPrefKeys,DisplayMode,LyricsFontSize,ScoreTint,ScorePages,ReadingPrefs}.kt`、對應測試、`test/.../reading/FakeSharedPreferences.kt`、`utils/ThemeHelper.java`、`test/.../utils/ThemeDefaultTest.kt` | 階段 0 |
| 1 | B | B1 → B2 → B3 | `java/.../reading/background/*.kt`、`test/.../reading/background/*`、`androidTest/.../reading/background/*`、`tools/gen_backgrounds.py`、`res/drawable*/bg_*`、`res/drawable*/bgx_*`、`res/values/ids_reading.xml` | 階段 0 |
| 1 | F | F0 → F1 → F2 → F3 | `assets/lyrics_xg_text/xg171.txt`、`assets/lyrics_xg_text_hant_{tw,hk}/xg171.txt`、`assets/lyrics_hant_manifest.txt`、`assets/lyrics_t2s_map.txt`、`tools/gen_font_subset.py`、`tools/font_subset_manifest.txt`、`res/font/*`、`assets/licenses/*`、`java/.../reading/LyricsTypefaces.kt`、`test/.../reading/FontSubsetTest.kt`、`About.java`、`res/layout/about.xml` | 階段 0 |
| 2 | S | S1 → S2 → S3 | `java/.../reading/{ReadingSettingsActivity,ReadingSettingsFragment,BackgroundPickerActivity}.kt`、`res/xml/reading_preferences.xml`、`res/layout/{reading_settings,background_picker,background_picker_item}.xml`、`AndroidManifest.xml`、`test/.../reading/ReadingPreferencesXmlTest.kt`、S3 另有：`java/.../reading/background/{PhotoBackgroundImporter,BackgroundPrefs,BackgroundPolicy}.kt`、`utils/WallPaperUtil.java` 與 `res/layout/wallpaper_editor.xml`（刪除）、`hymnchtv/build.gradle` 與根 `build.gradle`（移除 ucrop、jitpack）、`res/values*/strings.xml`（只刪 `wp_size`）、`res/values*/strings_reading.xml`、`MainActivity.java`（只改到能編譯：移除 `WallPaperUtil` import、`sbguser` 分支，`setWallpaper()` 改讀 `BackgroundPrefs.photoFile`）、對應 test／androidTest  | 階段 1 全部合併 |
| 3 | I | I1 → I2 | `ContentHandler.java`、`res/layout/content_main.xml`、`res/menu/menu_content.xml`、`mediaconfig/LyricsEnglishRecord.java`、`ContentView.java`、`res/layout/content_lyrics.xml`、`androidTest/.../ContentHandlerReadingTest.kt` | 階段 2 合併 |
| 3 | M | M1 | `MainActivity.java`、`res/layout/main.xml`、`res/layout-land/main.xml`、`res/menu/menu_main.xml`（`utils/WallPaperUtil.java` 已在 S3 刪除，不再屬於任何 lane） | 階段 2 合併 |
| 4 | — | C1 → V1 → R1 | 清理、模擬器驗證、審查 | 階段 3 全部合併 |

（`java/...` = `hymnchtv/src/main/java/org/cog/hymnchtv`；`test/...` = `hymnchtv/src/test/java/org/cog/hymnchtv`；`androidTest/...` = `hymnchtv/src/androidTest/java/org/cog/hymnchtv`；`res/`、`assets/` 都在 `hymnchtv/src/main/` 底下。）

Instrumented test（`androidTest`）在平行 lane 只做編譯檢查（`./gradlew :hymnchtv:assembleDebugAndroidTest`），V1 才在 `api24` 與 `api34` 上實際執行。

---

## 檔案結構

**新增（Kotlin，純邏輯，有 JVM 單元測試）：**

| 檔案 | 職責 |
|---|---|
| `reading/ReadingPrefKeys.kt` | 閱讀設定的 pref key 常數 |
| `reading/DisplayMode.kt` | `DisplayMode` enum 與 `DisplayModePolicy` |
| `reading/LyricsFontSize.kt` | `LyricsFontSize`、`LyricsFont`、`LyricsScale` |
| `reading/ScoreTint.kt` | `ScoreTint`、`ScoreTintPolicy`（色彩矩陣） |
| `reading/ScorePages.kt` | 樂譜分頁檔名（修第 5 頁 bug 的依據） |
| `reading/background/Wcag.kt` | 亮度、對比、混色 |
| `reading/background/BackgroundPreset.kt` | 20 款背景的註冊表 |
| `reading/background/BackgroundPolicy.kt` | `BackgroundSlot`、`BackgroundChoice`、`ReadingPalette`、預設值解析 |
| `reading/background/PhotoBackground.kt` | 照片變暗、模糊、取樣倍率 |

**新增（Kotlin，Android 邊界）：**

| 檔案 | 職責 |
|---|---|
| `reading/ReadingPrefs.kt` | 從 `SharedPreferences` 安全讀取（用假 prefs 做 JVM 測試） |
| `reading/LyricsTypefaces.kt` | 載入並快取 HymnalKai |
| `reading/background/BackgroundDrawables.kt` | preset → drawable／名稱字串；`RaysDrawable`、`StarsDrawable` |
| `reading/background/BackgroundApplier.kt` | 把背景或照片套到 `ImageView` |
| `reading/background/BackgroundPrefs.kt` | 讀 pref、主題、照片檔，回傳實際套用的調色盤 |
| `reading/ReadingSettingsActivity.kt`、`ReadingSettingsFragment.kt` | 閱讀設定畫面 |
| `reading/BackgroundPickerActivity.kt` | 背景挑選格狀畫面 |

**JVM 測試：** `DisplayModeTest`、`LyricsFontSizeTest`、`ScoreTintTest`、`ScorePagesTest`、`ReadingPrefsTest`（＋`FakeSharedPreferences`）、`utils/ThemeDefaultTest`、`background/WcagTest`、`background/BackgroundPresetTest`、`background/BackgroundPolicyTest`、`background/PhotoPaletteTest`、`background/PhotoBackgroundTest`、`background/BackgroundResourcesTest`、`FontSubsetTest`、`ReadingPreferencesXmlTest`。

**Instrumented 測試（V1 在 API 24 與 34 執行）：** `reading/background/BackgroundDrawablesTest`（20 款背景都能 inflate 並繪製、照片解碼失敗的退回、照片換回預設時清掉模糊）、`ContentHandlerReadingTest`（A 的簡繁臨時切換、頁碼保存、螢幕常亮，以及 A2 的顯示模式臨時切換、從設定返回後的重建、只顯示詞釋放樂譜與切回時重新載入）。

**工具與產生物：**
- `tools/gen_backgrounds.py` → `res/drawable/bg_<id>.xml`（20）、`res/drawable/bgx_motif_*.xml`（5）、`res/drawable-nodpi/bgx_tex_*.png`（4）、`res/drawable-xxhdpi/bgx_tile_*.png`（2）
- `tools/gen_font_subset.py` → `res/font/hymnal_kai_{sc,tc}.ttf`、`tools/font_subset_manifest.txt`
- `tools/gen_lyrics_hant.py`（A 既有）重新執行 → `assets/lyrics_xg_text_hant_{tw,hk}/xg171.txt`、`assets/lyrics_hant_manifest.txt`、`assets/lyrics_t2s_map.txt`
- `assets/licenses/OFL-HymnalKai.txt`

**資源：** `res/values{,-zh,-b+zh+Hant}/strings_reading.xml`、`res/values/reading_arrays.xml`、`res/values/ids_reading.xml`、`res/xml/reading_preferences.xml`、`res/layout/{reading_settings,background_picker,background_picker_item}.xml`。

**修改：** `hymnchtv/build.gradle`、`.gitignore`、`AndroidManifest.xml`、`ContentView.java`（整檔替換）、`ContentHandler.java`、`MainActivity.java`、`utils/ThemeHelper.java`、`About.java`、`assets/lyrics_xg_text/xg171.txt`、`mediaconfig/LyricsEnglishRecord.java`、`res/layout/{content_lyrics,content_main,main,about}.xml`、`res/layout-land/main.xml`、`res/menu/{menu_main,menu_content}.xml`、三份 `strings.xml`（C1 刪除不用的字串）。

**刪除：** `utils/WallPaperUtil.java`、`res/layout/wallpaper_editor.xml`（兩者在 S3 已刪）、`utils/ChineseS2TSelection.java`、`res/layout/chinese_t2s_selection.xml`、`res/drawable-ldpi/bg*.jpg`（12 張）。

---

### Task 0：建立分支、記錄基準、共用設定與字串

**Files:**
- Modify: `hymnchtv/build.gradle`、`.gitignore`
- Create: `hymnchtv/src/main/res/values/strings_reading.xml`、`values-zh/strings_reading.xml`、`values-b+zh+Hant/strings_reading.xml`、`values/reading_arrays.xml`

所有 lane 都會用到的東西集中在這裡先做好，之後各 lane 就不必碰 `build.gradle` 和字串檔。

- [ ] **Step 1：建立分支**

  ```bash
  cd /Users/hitobias/orca/hymnchtv
  git status --short          # 必須是乾淨的
  git switch feat/zh-hant && git switch -c feat/reading-settings
  git branch --show-current
  ```

  Expected: 印出 `feat/reading-settings`。如果子項目 A 已經合併進 `master`，改從 `master` 開分支。

- [ ] **Step 2：記錄基準（不要啟動模擬器）**

  ```bash
  ./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug :hymnchtv:lintDebug --console=plain
  for k in MissingTranslation ExtraTranslation HardcodedText UnusedResources; do
    printf "%s: " $k; grep -c "\[$k\]" hymnchtv/build/reports/lint-results-debug.txt
  done
  ls -l hymnchtv/build/outputs/apk/debug/*.apk
  ```

  Expected: `BUILD SUCCESSFUL`。把四個 lint 數字和 APK 大小（bytes）抄進這個 task 的回報；V1 會拿來比較。之後提到「Task 0 的基準值」就是指這些數字。

- [ ] **Step 3：`hymnchtv/build.gradle` 加入依賴**

  在 `implementation 'androidx.lifecycle:lifecycle-extensions:2.2.0'` 的下一行加入：

  ```groovy
      implementation 'androidx.preference:preference:1.2.1'
  ```

  先到 https://maven.google.com/web/index.html#androidx.preference:preference 確認最新穩定版；若有更新的穩定版就用新版，並在回報中註明。

- [ ] **Step 4：`build.gradle` 的 `testOptions` 加入系統屬性**

  把：

  ```groovy
      testOptions {
          unitTests.all {
              it.systemProperty 'hymnchtv.assetsDir', file('src/main/assets').absolutePath
          }
      }
  ```

  改成：

  ```groovy
      testOptions {
          unitTests.all {
              it.systemProperty 'hymnchtv.assetsDir', file('src/main/assets').absolutePath
              it.systemProperty 'hymnchtv.resDir', file('src/main/res').absolutePath
              it.systemProperty 'hymnchtv.repoRoot', rootProject.projectDir.absolutePath
          }
      }
  ```

- [ ] **Step 5：`build.gradle` 檔尾加入測試輸入宣告**

  讓 Gradle 在產生物或產生工具改變時重新跑測試。加在檔案最後：

  ```groovy

  // Reading-settings sync tests (plan A2) read generated resources and generator inputs directly.
  tasks.withType(Test).configureEach {
      inputs.files(fileTree('src/main/res') {
          include 'drawable*/bg_*', 'drawable*/bgx_*', 'font/**', 'xml/reading_preferences.xml', 'values/reading_arrays.xml',
                  'values*/strings*.xml'
      }).withPathSensitivity(PathSensitivity.RELATIVE).withPropertyName('readingResources')
      inputs.files(fileTree('src/main/assets') { include 'licenses/**' })
          .withPathSensitivity(PathSensitivity.RELATIVE).withPropertyName('readingAssets')
      inputs.files(rootProject.file('tools/font_subset_manifest.txt'))
          .withPathSensitivity(PathSensitivity.RELATIVE).withPropertyName('readingGeneratorInputs')
  }
  ```

- [ ] **Step 6：`.gitignore` 檔尾加入**

  ```
  # A2: font sources (40 MB, downloaded) and the fontTools venv
  /tools/fonts-src/
  /.venv-tools/
  ```

- [ ] **Step 7：新增英文字串** `hymnchtv/src/main/res/values/strings_reading.xml`

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <resources>
      <!-- Reading settings (plan A2) -->
      <string name="reading_settings">Reading settings</string>
      <string name="pref_cat_lyrics">Lyrics</string>
      <string name="pref_cat_background">Background</string>
      <string name="pref_cat_screen">Player and screen</string>
      <string name="pref_display_mode">Display mode</string>
      <string name="display_mode_both">Score + lyrics</string>
      <string name="display_mode_score">Score only</string>
      <string name="display_mode_lyrics">Lyrics only</string>
      <string name="pref_font_size">Lyrics text size</string>
      <string name="font_size_small">Small</string>
      <string name="font_size_medium">Medium</string>
      <string name="font_size_large">Large</string>
      <string name="font_size_xlarge">Extra large</string>
      <string name="pref_lyrics_font">Lyrics font</string>
      <string name="lyrics_font_system">System font</string>
      <string name="lyrics_font_kai">Kai (brush script)</string>
      <string name="pref_main_background">Home screen background</string>
      <string name="pref_lyrics_background">Lyrics page background</string>
      <string name="pref_photo_dim">Photo dimming</string>
      <string name="pref_photo_blur">Photo blur</string>
      <string name="pref_photo_blur_unsupported">Requires Android 12 or later</string>
      <string name="pref_page_animation">Page-turn animation</string>
      <string name="pref_menu_show">Show the player by default</string>
      <string name="pref_keep_screen_on">Keep the screen on while reading</string>
      <string name="bg_photo">Your photo</string>
      <string name="bg_picker_sample">Holy, holy, holy</string>
      <string name="bg_selected_mark" translatable="false">✓</string>
      <string name="about_font_credit">The lyrics font HymnalKai is derived from LXGW WenKai and licensed under the SIL Open Font License 1.1. Tap to read the licence.</string>
      <string name="font_license_title">Font licence</string>

      <string name="bg_name_xuan">Rice paper</string>
      <string name="bg_name_linen">Linen grey</string>
      <string name="bg_name_parchment">Parchment</string>
      <string name="bg_name_mist">Morning mist</string>
      <string name="bg_name_dawn">Dawn</string>
      <string name="bg_name_sky">Daylight</string>
      <string name="bg_name_harvest">Wheat gold</string>
      <string name="bg_name_dusk">Dusk</string>
      <string name="bg_name_meadow">Meadow</string>
      <string name="bg_name_staff">Music staff</string>
      <string name="bg_name_olive">Olive branch</string>
      <string name="bg_name_wheat">Ears of wheat</string>
      <string name="bg_name_dove">Dove</string>
      <string name="bg_name_rays">Rays of light</string>
      <string name="bg_name_water">Living water</string>
      <string name="bg_name_vine">Vine</string>
      <string name="bg_name_nightread">Night reading</string>
      <string name="bg_name_starry">Starry night</string>
      <string name="bg_name_deepsea">Deep sea</string>
      <string name="bg_name_ink">Ink</string>
  </resources>
  ```

- [ ] **Step 8：新增簡中字串** `hymnchtv/src/main/res/values-zh/strings_reading.xml`

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <resources>
      <string name="reading_settings">阅读设置</string>
      <string name="pref_cat_lyrics">歌词</string>
      <string name="pref_cat_background">背景</string>
      <string name="pref_cat_screen">播放器与屏幕</string>
      <string name="pref_display_mode">显示模式</string>
      <string name="display_mode_both">谱＋词</string>
      <string name="display_mode_score">只显示谱</string>
      <string name="display_mode_lyrics">只显示词</string>
      <string name="pref_font_size">歌词字号</string>
      <string name="font_size_small">小</string>
      <string name="font_size_medium">中</string>
      <string name="font_size_large">大</string>
      <string name="font_size_xlarge">特大</string>
      <string name="pref_lyrics_font">歌词字体</string>
      <string name="lyrics_font_system">系统字体</string>
      <string name="lyrics_font_kai">文楷</string>
      <string name="pref_main_background">主页背景</string>
      <string name="pref_lyrics_background">歌词页背景</string>
      <string name="pref_photo_dim">照片变暗</string>
      <string name="pref_photo_blur">照片模糊</string>
      <string name="pref_photo_blur_unsupported">需要 Android 12 以上</string>
      <string name="pref_page_animation">翻页动画</string>
      <string name="pref_menu_show">默认显示播放器</string>
      <string name="pref_keep_screen_on">阅读时屏幕常亮</string>
      <string name="bg_photo">自定义照片</string>
      <string name="bg_picker_sample">圣哉，圣哉，圣哉</string>
      <string name="about_font_credit">歌词字体 HymnalKai 衍生自 LXGW WenKai（霞鹜文楷），采用 SIL Open Font License 1.1 授权。点此阅读授权全文。</string>
      <string name="font_license_title">字体授权</string>

      <string name="bg_name_xuan">宣纸白</string>
      <string name="bg_name_linen">亚麻灰</string>
      <string name="bg_name_parchment">羊皮纸</string>
      <string name="bg_name_mist">晨雾蓝</string>
      <string name="bg_name_dawn">晨曦</string>
      <string name="bg_name_sky">天光</string>
      <string name="bg_name_harvest">麦田金</string>
      <string name="bg_name_dusk">暮色</string>
      <string name="bg_name_meadow">青草地</string>
      <string name="bg_name_staff">五线谱</string>
      <string name="bg_name_olive">橄榄枝</string>
      <string name="bg_name_wheat">麦穗</string>
      <string name="bg_name_dove">白鸽</string>
      <string name="bg_name_rays">光芒</string>
      <string name="bg_name_water">活水</string>
      <string name="bg_name_vine">葡萄树</string>
      <string name="bg_name_nightread">夜读</string>
      <string name="bg_name_starry">星夜</string>
      <string name="bg_name_deepsea">深海</string>
      <string name="bg_name_ink">墨色</string>
  </resources>
  ```

- [ ] **Step 9：新增繁中字串** `hymnchtv/src/main/res/values-b+zh+Hant/strings_reading.xml`

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <resources>
      <string name="reading_settings">閱讀設定</string>
      <string name="pref_cat_lyrics">歌詞</string>
      <string name="pref_cat_background">背景</string>
      <string name="pref_cat_screen">播放器與螢幕</string>
      <string name="pref_display_mode">顯示模式</string>
      <string name="display_mode_both">譜＋詞</string>
      <string name="display_mode_score">只顯示譜</string>
      <string name="display_mode_lyrics">只顯示詞</string>
      <string name="pref_font_size">歌詞字級</string>
      <string name="font_size_small">小</string>
      <string name="font_size_medium">中</string>
      <string name="font_size_large">大</string>
      <string name="font_size_xlarge">特大</string>
      <string name="pref_lyrics_font">歌詞字型</string>
      <string name="lyrics_font_system">系統字型</string>
      <string name="lyrics_font_kai">文楷</string>
      <string name="pref_main_background">主頁背景</string>
      <string name="pref_lyrics_background">歌詞頁背景</string>
      <string name="pref_photo_dim">照片變暗</string>
      <string name="pref_photo_blur">照片模糊</string>
      <string name="pref_photo_blur_unsupported">需要 Android 12 以上</string>
      <string name="pref_page_animation">翻頁動畫</string>
      <string name="pref_menu_show">預設顯示播放器</string>
      <string name="pref_keep_screen_on">閱讀時螢幕保持開啟</string>
      <string name="bg_photo">自訂照片</string>
      <string name="bg_picker_sample">聖哉，聖哉，聖哉</string>
      <string name="about_font_credit">歌詞字型 HymnalKai 衍生自 LXGW WenKai（霞鶩文楷），採用 SIL Open Font License 1.1 授權。點此閱讀授權全文。</string>
      <string name="font_license_title">字型授權</string>

      <string name="bg_name_xuan">宣紙白</string>
      <string name="bg_name_linen">亞麻灰</string>
      <string name="bg_name_parchment">羊皮紙</string>
      <string name="bg_name_mist">晨霧藍</string>
      <string name="bg_name_dawn">晨曦</string>
      <string name="bg_name_sky">天光</string>
      <string name="bg_name_harvest">麥田金</string>
      <string name="bg_name_dusk">暮色</string>
      <string name="bg_name_meadow">青草地</string>
      <string name="bg_name_staff">五線譜</string>
      <string name="bg_name_olive">橄欖枝</string>
      <string name="bg_name_wheat">麥穗</string>
      <string name="bg_name_dove">白鴿</string>
      <string name="bg_name_rays">光芒</string>
      <string name="bg_name_water">活水</string>
      <string name="bg_name_vine">葡萄樹</string>
      <string name="bg_name_nightread">夜讀</string>
      <string name="bg_name_starry">星夜</string>
      <string name="bg_name_deepsea">深海</string>
      <string name="bg_name_ink">墨色</string>
  </resources>
  ```

  「字型」「螢幕」「設定」「預設」是台灣用語，和 A 的繁中字串一致。

- [ ] **Step 10：新增清單陣列** `hymnchtv/src/main/res/values/reading_arrays.xml`

  值必須是 enum 名稱（S1 的 `ReadingPreferencesXmlTest` 會檢查）；標籤引用已翻譯的字串，所以陣列本身不翻譯。

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <resources>
      <string-array name="display_mode_entries" translatable="false">
          <item>@string/display_mode_both</item>
          <item>@string/display_mode_score</item>
          <item>@string/display_mode_lyrics</item>
      </string-array>
      <string-array name="display_mode_values" translatable="false">
          <item>SCORE_AND_LYRICS</item>
          <item>SCORE_ONLY</item>
          <item>LYRICS_ONLY</item>
      </string-array>

      <string-array name="lyrics_lang_entries" translatable="false">
          <item>@string/lyrics_follow_ui</item>
          <item>@string/lyrics_simplified</item>
          <item>@string/lyrics_traditional</item>
      </string-array>
      <string-array name="lyrics_lang_values" translatable="false">
          <item>FOLLOW_UI</item>
          <item>SIMPLIFIED</item>
          <item>TRADITIONAL</item>
      </string-array>

      <string-array name="conversion_entries" translatable="false">
          <item>@string/S2TW</item>
          <item>@string/S2HK</item>
      </string-array>
      <string-array name="conversion_values" translatable="false">
          <item>S2TW</item>
          <item>S2HK</item>
      </string-array>

      <string-array name="font_size_entries" translatable="false">
          <item>@string/font_size_small</item>
          <item>@string/font_size_medium</item>
          <item>@string/font_size_large</item>
          <item>@string/font_size_xlarge</item>
      </string-array>
      <string-array name="font_size_values" translatable="false">
          <item>SMALL</item>
          <item>MEDIUM</item>
          <item>LARGE</item>
          <item>XLARGE</item>
      </string-array>

      <string-array name="lyrics_font_entries" translatable="false">
          <item>@string/lyrics_font_system</item>
          <item>@string/lyrics_font_kai</item>
      </string-array>
      <string-array name="lyrics_font_values" translatable="false">
          <item>SYSTEM</item>
          <item>KAI</item>
      </string-array>
  </resources>
  ```

- [ ] **Step 11：確認可以建置**

  ```bash
  ./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug :hymnchtv:lintDebug --console=plain
  for k in MissingTranslation ExtraTranslation; do printf "%s: " $k; grep -c "\[$k\]" hymnchtv/build/reports/lint-results-debug.txt; done
  ```

  Expected: `BUILD SUCCESSFUL`；`MissingTranslation` 和 `ExtraTranslation` 和基準相同（0／0）。`UnusedResources` 會暫時增加（字串還沒被使用），C1 之後再比對。

- [ ] **Step 12：Commit**

  ```bash
  git add hymnchtv/build.gradle .gitignore hymnchtv/src/main/res/values*/strings_reading.xml hymnchtv/src/main/res/values/reading_arrays.xml
  git commit -m "chore: prepare reading settings (preference dependency, test properties, strings)" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

## 階段 1 · Lane P：閱讀設定的純邏輯

Lane P 的子代理在 worktree `../hymnchtv-a2-p` 工作。所有測試用：
`./gradlew :hymnchtv:testDebugUnitTest --tests '<類別>' --console=plain`。

### Task P1：ReadingPrefKeys、DisplayMode、DisplayModePolicy

**Files:**
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/reading/ReadingPrefKeys.kt`
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/reading/DisplayMode.kt`
- Test: `hymnchtv/src/test/java/org/cog/hymnchtv/reading/DisplayModeTest.kt`

**前置:** Task 0。

- [ ] **Step 1：寫會失敗的測試** `DisplayModeTest.kt`

  ```kotlin
  package org.cog.hymnchtv.reading

  import com.google.common.truth.Truth.assertThat
  import org.cog.hymnchtv.reading.DisplayMode.LYRICS_ONLY
  import org.cog.hymnchtv.reading.DisplayMode.SCORE_AND_LYRICS
  import org.cog.hymnchtv.reading.DisplayMode.SCORE_ONLY
  import org.junit.Test

  class DisplayModeTest {
      @Test
      fun parsesStoredNames() {
          DisplayMode.entries.forEach { assertThat(DisplayMode.fromPref(it.name)).isEqualTo(it) }
      }

      @Test
      fun invalidOrMissingFallsBackToScoreAndLyrics() {
          listOf(null, "", "score_only", "BOTH").forEach {
              assertThat(DisplayMode.fromPref(it)).isEqualTo(SCORE_AND_LYRICS)
          }
      }

      @Test
      fun nextCyclesThroughAllModes() {
          assertThat(SCORE_AND_LYRICS.next()).isEqualTo(SCORE_ONLY)
          assertThat(SCORE_ONLY.next()).isEqualTo(LYRICS_ONLY)
          assertThat(LYRICS_ONLY.next()).isEqualTo(SCORE_AND_LYRICS)
      }

      @Test
      fun visibilityFlags() {
          assertThat(SCORE_AND_LYRICS.showScore && SCORE_AND_LYRICS.showLyrics).isTrue()
          assertThat(SCORE_ONLY.showScore && !SCORE_ONLY.showLyrics).isTrue()
          assertThat(!LYRICS_ONLY.showScore && LYRICS_ONLY.showLyrics).isTrue()
      }

      @Test
      fun sessionOverrideWinsOverStoredDefault() {
          assertThat(DisplayModePolicy.resolve(LYRICS_ONLY, SCORE_ONLY)).isEqualTo(LYRICS_ONLY)
          assertThat(DisplayModePolicy.resolve(null, SCORE_ONLY)).isEqualTo(SCORE_ONLY)
      }

      @Test
      fun lyricsOnlyWithoutTextShowsBoth() {
          assertThat(DisplayModePolicy.effective(LYRICS_ONLY, false)).isEqualTo(SCORE_AND_LYRICS)
          assertThat(DisplayModePolicy.effective(LYRICS_ONLY, true)).isEqualTo(LYRICS_ONLY)
          assertThat(DisplayModePolicy.effective(SCORE_ONLY, false)).isEqualTo(SCORE_ONLY)
          assertThat(DisplayModePolicy.effective(SCORE_AND_LYRICS, false)).isEqualTo(SCORE_AND_LYRICS)
      }

      @Test
      fun lyricsTextThresholdMatchesJiaoChangHint() {
          assertThat(DisplayModePolicy.hasLyricsText(null)).isFalse()
          assertThat(DisplayModePolicy.hasLyricsText("")).isFalse()
          assertThat(DisplayModePolicy.hasLyricsText("詞".repeat(39))).isFalse()
          assertThat(DisplayModePolicy.hasLyricsText("詞".repeat(40))).isTrue()
      }
  }
  ```

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.reading.DisplayModeTest' --console=plain`
  Expected: 編譯失敗，`Unresolved reference 'DisplayMode'`。

- [ ] **Step 2：新增** `ReadingPrefKeys.kt`

  ```kotlin
  package org.cog.hymnchtv.reading

  /** SharedPreferences keys (file MainActivity.PREF_SETTINGS) owned by the reading settings (plan A2). */
  object ReadingPrefKeys {
      const val DISPLAY_MODE = "DisplayMode"
      const val LYRICS_FONT_SIZE = "LyricsFontSize"
      const val LYRICS_FONT = "LyricsFont"
      /** Shared with sub-project B-11 (low-RAM phones will default it off). */
      const val PAGE_ANIMATION = "PageAnimation"
      const val KEEP_SCREEN_ON = "KeepScreenOn"
      /** Player shown by default; MainActivity.PREF_MENU_SHOW aliases this key. */
      const val MENU_SHOW = "MenuShow"
      /** Pinch-zoom scale per orientation; ContentView.PREF_LYRICS_SCALE_P / _L alias these keys. */
      const val LYRICS_SCALE_P = "LyricsScaleP"
      const val LYRICS_SCALE_L = "LyricsScaleL"
  }
  ```

- [ ] **Step 3：實作** `DisplayMode.kt`

  ```kotlin
  package org.cog.hymnchtv.reading

  /** What a lyrics page shows (plan A2). Declaration order = the order the lyrics-page button cycles through. */
  enum class DisplayMode(val showScore: Boolean, val showLyrics: Boolean) {
      SCORE_AND_LYRICS(true, true),
      SCORE_ONLY(true, false),
      LYRICS_ONLY(false, true);

      fun next(): DisplayMode = entries[(ordinal + 1) % entries.size]

      companion object {
          /** Never throws; unknown values mean SCORE_AND_LYRICS (the behaviour before A2). */
          @JvmStatic
          fun fromPref(value: String?): DisplayMode = entries.firstOrNull { it.name == value } ?: SCORE_AND_LYRICS
      }
  }

  object DisplayModePolicy {
      /** Fewer characters than this means the hymn has no usable lyrics text (same threshold as the JiaoChang hint). */
      const val MIN_LYRICS_CHARS = 40

      @JvmStatic
      fun hasLyricsText(text: CharSequence?): Boolean = text != null && text.length >= MIN_LYRICS_CHARS

      /** The lyrics-page button wins for this session; otherwise the stored default applies. */
      @JvmStatic
      fun resolve(sessionOverride: DisplayMode?, stored: DisplayMode): DisplayMode = sessionOverride ?: stored

      /** "Lyrics only" on a hymn without lyrics text would leave an empty page, so both are shown instead. */
      @JvmStatic
      fun effective(mode: DisplayMode, hasLyricsText: Boolean): DisplayMode =
          if (mode == DisplayMode.LYRICS_ONLY && !hasLyricsText) DisplayMode.SCORE_AND_LYRICS else mode
  }
  ```

- [ ] **Step 4：執行測試**

  Run: 同 Step 1。
  Expected: 7 個測試全部通過。

- [ ] **Step 5：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/reading/ReadingPrefKeys.kt \
          hymnchtv/src/main/java/org/cog/hymnchtv/reading/DisplayMode.kt \
          hymnchtv/src/test/java/org/cog/hymnchtv/reading/DisplayModeTest.kt
  git commit -m "feat: add display mode model and policy" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

### Task P2：LyricsFontSize、LyricsFont、LyricsScale

**Files:**
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/reading/LyricsFontSize.kt`
- Test: `hymnchtv/src/test/java/org/cog/hymnchtv/reading/LyricsFontSizeTest.kt`

**前置:** P1。

- [ ] **Step 1：寫會失敗的測試** `LyricsFontSizeTest.kt`

  ```kotlin
  package org.cog.hymnchtv.reading

  import com.google.common.truth.Truth.assertThat
  import org.junit.Test

  class LyricsFontSizeTest {
      @Test
      fun presetsGrowAndMediumKeepsTheOldDefaults() {
          val scales = LyricsFontSize.entries.map { it.scale }
          assertThat(scales).isInStrictOrder()
          assertThat(LyricsFontSize.MEDIUM.scale * LyricsScale.BASE_SP_PORTRAIT).isEqualTo(20f)
          assertThat(LyricsFontSize.MEDIUM.scale * LyricsScale.BASE_SP_LANDSCAPE).isEqualTo(35f)
          assertThat(LyricsFontSize.SMALL.scale).isEqualTo(LyricsScale.MIN)
      }

      @Test
      fun fontSizeFromPrefNeverThrows() {
          LyricsFontSize.entries.forEach { assertThat(LyricsFontSize.fromPref(it.name)).isEqualTo(it) }
          listOf(null, "", "medium", "HUGE").forEach { assertThat(LyricsFontSize.fromPref(it)).isEqualTo(LyricsFontSize.MEDIUM) }
      }

      @Test
      fun lyricsFontFromPrefDefaultsToKai() {
          assertThat(LyricsFont.fromPref("SYSTEM")).isEqualTo(LyricsFont.SYSTEM)
          assertThat(LyricsFont.fromPref("KAI")).isEqualTo(LyricsFont.KAI)
          listOf(null, "", "kai").forEach { assertThat(LyricsFont.fromPref(it)).isEqualTo(LyricsFont.KAI) }
      }

      @Test
      fun noStoredScaleStartsFromThePreset() {
          assertThat(LyricsScale.resolve(null, LyricsFontSize.LARGE)).isEqualTo(1.5f)
          assertThat(LyricsScale.resolve(Float.NaN, LyricsFontSize.LARGE)).isEqualTo(1.5f)
          assertThat(LyricsScale.resolve(0f, LyricsFontSize.SMALL)).isEqualTo(1.0f)
          assertThat(LyricsScale.resolve(-2f, LyricsFontSize.XLARGE)).isEqualTo(2.0f)
      }

      @Test
      fun storedPinchScaleWinsButIsClamped() {
          assertThat(LyricsScale.resolve(3.0f, LyricsFontSize.SMALL)).isEqualTo(3.0f)
          assertThat(LyricsScale.resolve(9.0f, LyricsFontSize.SMALL)).isEqualTo(LyricsScale.MAX)
          assertThat(LyricsScale.resolve(0.5f, LyricsFontSize.LARGE)).isEqualTo(LyricsScale.MIN)
      }
  }
  ```

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.reading.LyricsFontSizeTest' --console=plain`
  Expected: 編譯失敗，`Unresolved reference 'LyricsFontSize'`。

- [ ] **Step 2：實作** `LyricsFontSize.kt`

  ```kotlin
  package org.cog.hymnchtv.reading

  /** Default lyrics size (plan A2); [scale] multiplies LyricsScale's base size. MEDIUM equals the pre-A2 default. */
  enum class LyricsFontSize(val scale: Float) {
      SMALL(1.0f),
      MEDIUM(1.25f),
      LARGE(1.5f),
      XLARGE(2.0f);

      companion object {
          /** Never throws; unknown values mean MEDIUM. */
          @JvmStatic
          fun fromPref(value: String?): LyricsFontSize = entries.firstOrNull { it.name == value } ?: MEDIUM
      }
  }

  /** Lyrics typeface: the device font, or the bundled HymnalKai (LXGW WenKai subset). */
  enum class LyricsFont {
      SYSTEM,
      KAI;

      companion object {
          /** Never throws; unknown values mean KAI. */
          @JvmStatic
          fun fromPref(value: String?): LyricsFont = entries.firstOrNull { it.name == value } ?: KAI
      }
  }

  object LyricsScale {
      /** Text size in sp at scale 1.0 (= SMALL). */
      const val BASE_SP_PORTRAIT = 16
      const val BASE_SP_LANDSCAPE = 28

      /** Same limits as ZoomTextView (MIN_SCALE_FACTOR / MAX_SCALE_FACTOR). */
      const val MIN = 1.0f
      const val MAX = 5.0f

      /** A stored pinch scale wins (clamped); a missing or broken value starts from the chosen preset. */
      @JvmStatic
      fun resolve(stored: Float?, preset: LyricsFontSize): Float =
          if (stored == null || stored.isNaN() || stored <= 0f) preset.scale else stored.coerceIn(MIN, MAX)
  }
  ```

- [ ] **Step 3：執行測試**

  Run: 同 Step 1。
  Expected: 5 個測試全部通過。

- [ ] **Step 4：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/reading/LyricsFontSize.kt \
          hymnchtv/src/test/java/org/cog/hymnchtv/reading/LyricsFontSizeTest.kt
  git commit -m "feat: add lyrics font size presets, scale resolution and font choice" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

### Task P3：ScoreTint 與 ScorePages

**Files:**
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/reading/ScoreTint.kt`
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/reading/ScorePages.kt`
- Test: `hymnchtv/src/test/java/org/cog/hymnchtv/reading/ScoreTintTest.kt`
- Test: `hymnchtv/src/test/java/org/cog/hymnchtv/reading/ScorePagesTest.kt`

**前置:** P2。

- [ ] **Step 1：寫會失敗的測試** `ScoreTintTest.kt`

  ```kotlin
  package org.cog.hymnchtv.reading

  import com.google.common.truth.Truth.assertThat
  import org.junit.Test

  class ScoreTintTest {
      private val white = 0xFFFFFFFF.toInt()
      private val black = 0xFF000000.toInt()
      private val paper = 0xFF24211D.toInt()   // 夜讀 base
      private val ink = 0xFFECE4D6.toInt()     // 夜讀 text

      @Test
      fun levelZeroOnLightBackgroundLeavesTheScoreAlone() {
          val tint = ScoreTintPolicy.resolve(0, false, paper, ink)
          assertThat(tint).isEqualTo(ScoreTint.None)
          assertThat(ScoreTintPolicy.matrix(tint)).isNull()
      }

      @Test
      fun levelZeroOnDarkBackgroundMapsPaperAndInk() {
          val tint = ScoreTintPolicy.resolve(0, true, paper, ink)
          assertThat(tint).isEqualTo(ScoreTint.Duotone(paper, ink))
          val m = ScoreTintPolicy.matrix(tint)!!
          assertThat(ScoreTintPolicy.applyTo(m, white)).isEqualTo(paper)
          assertThat(ScoreTintPolicy.applyTo(m, black)).isEqualTo(ink)
      }

      @Test
      fun manualLevelsKeepTheOldInversion() {
          val m = ScoreTintPolicy.matrix(ScoreTintPolicy.resolve(1, false, paper, ink))!!
          assertThat(ScoreTintPolicy.applyTo(m, white)).isEqualTo(0xFF1A1A1A.toInt())   // 255 - 0.9 * 255 = 25.5 -> 26
          assertThat(ScoreTintPolicy.applyTo(m, black)).isEqualTo(white)
          assertThat(ScoreTintPolicy.resolve(3, true, paper, ink)).isEqualTo(ScoreTint.Invert(-0.7f))
      }

      @Test
      fun unknownLevelsFallBackToAutomatic() {
          assertThat(ScoreTintPolicy.resolve(7, false, paper, ink)).isEqualTo(ScoreTint.None)
          assertThat(ScoreTintPolicy.resolve(-1, true, paper, ink)).isEqualTo(ScoreTint.Duotone(paper, ink))
      }

      @Test
      fun levelCountMatchesTheMenuCycle() {
          assertThat(ScoreTintPolicy.LEVEL_COUNT).isEqualTo(4)
      }
  }
  ```

- [ ] **Step 2：寫會失敗的測試** `ScorePagesTest.kt`（第 5 頁 bug 的回歸測試）

  ```kotlin
  package org.cog.hymnchtv.reading

  import com.google.common.truth.Truth.assertThat
  import org.junit.Test

  class ScorePagesTest {
      @Test
      fun fivePagesUseFiveDistinctFiles() {
          // db152 is the 5-page hymn; before A2 page 5 (d.png) was loaded into page 4's ImageView
          assertThat(ScorePages.fileNames("lyrics_db_score/db152", 5)).containsExactly(
              "lyrics_db_score/db152.png", "lyrics_db_score/db152a.png", "lyrics_db_score/db152b.png",
              "lyrics_db_score/db152c.png", "lyrics_db_score/db152d.png",
          ).inOrder()
      }

      @Test
      fun singlePage() {
          assertThat(ScorePages.fileNames("p", 1)).containsExactly("p.png")
      }

      @Test
      fun outOfRangeCountsAreClamped() {
          assertThat(ScorePages.fileNames("p", 0)).containsExactly("p.png")
          assertThat(ScorePages.fileNames("p", -3)).containsExactly("p.png")
          assertThat(ScorePages.fileNames("p", 9)).hasSize(ScorePages.MAX_PAGES)
      }
  }
  ```

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.reading.Score*' --console=plain`
  Expected: 編譯失敗，`Unresolved reference 'ScoreTintPolicy'` 與 `'ScorePages'`。

- [ ] **Step 3：實作** `ScoreTint.kt`

  ```kotlin
  package org.cog.hymnchtv.reading

  import kotlin.math.roundToInt

  /** How the opaque white score PNGs are recoloured (plan A2). */
  sealed interface ScoreTint {
      data object None : ScoreTint

      /** The pre-A2 manual inversion: each channel becomes 255 + multiplier * value. */
      data class Invert(val multiplier: Float) : ScoreTint

      /** White paper becomes [paper], black notes become [ink]; channels in between are interpolated. */
      data class Duotone(val paper: Int, val ink: Int) : ScoreTint
  }

  object ScoreTintPolicy {
      /** Levels cycled by the "score colour" menu; level 0 = automatic (follow the background). */
      const val LEVEL_COUNT = 4
      private val INVERT_MULTIPLIERS = floatArrayOf(0f, -0.9f, -0.8f, -0.7f)

      @JvmStatic
      fun resolve(manualLevel: Int, darkBackground: Boolean, paper: Int, ink: Int): ScoreTint = when {
          manualLevel in 1 until LEVEL_COUNT -> ScoreTint.Invert(INVERT_MULTIPLIERS[manualLevel])
          darkBackground -> ScoreTint.Duotone(paper, ink)
          else -> ScoreTint.None
      }

      /** 4x5 matrix for ColorMatrixColorFilter, or null for "no filter". */
      @JvmStatic
      fun matrix(tint: ScoreTint): FloatArray? = when (tint) {
          ScoreTint.None -> null
          is ScoreTint.Invert -> tint.multiplier.let { m ->
              floatArrayOf(
                  m, 0f, 0f, 0f, 255f,
                  0f, m, 0f, 0f, 255f,
                  0f, 0f, m, 0f, 255f,
                  0f, 0f, 0f, 1f, 0f,
              )
          }
          is ScoreTint.Duotone -> {
              fun ch(color: Int, shift: Int) = (color shr shift and 0xFF).toFloat()
              floatArrayOf(
                  (ch(tint.paper, 16) - ch(tint.ink, 16)) / 255f, 0f, 0f, 0f, ch(tint.ink, 16),
                  0f, (ch(tint.paper, 8) - ch(tint.ink, 8)) / 255f, 0f, 0f, ch(tint.ink, 8),
                  0f, 0f, (ch(tint.paper, 0) - ch(tint.ink, 0)) / 255f, 0f, ch(tint.ink, 0),
                  0f, 0f, 0f, 1f, 0f,
              )
          }
      }

      /** Applies [matrix] to one ARGB colour the way ColorMatrixColorFilter does (used by tests). */
      @JvmStatic
      fun applyTo(matrix: FloatArray, color: Int): Int {
          val src = floatArrayOf(
              (color shr 16 and 0xFF).toFloat(), (color shr 8 and 0xFF).toFloat(),
              (color and 0xFF).toFloat(), (color ushr 24).toFloat(),
          )
          fun out(row: Int): Int {
              var sum = matrix[row * 5 + 4]
              for (i in 0 until 4) sum += matrix[row * 5 + i] * src[i]
              return sum.roundToInt().coerceIn(0, 255)
          }
          return (out(3) shl 24) or (out(0) shl 16) or (out(1) shl 8) or out(2)
      }
  }
  ```

- [ ] **Step 4：實作** `ScorePages.kt`

  ```kotlin
  package org.cog.hymnchtv.reading

  /** Score image names: the hymn itself, then the a–d suffixes for pages 2–5. */
  object ScorePages {
      const val MAX_PAGES = 5
      private val SUFFIXES = listOf("", "a", "b", "c", "d")

      @JvmStatic
      fun fileNames(prefix: String, pages: Int): List<String> =
          SUFFIXES.take(pages.coerceIn(1, MAX_PAGES)).map { "$prefix$it.png" }
  }
  ```

- [ ] **Step 5：執行測試**

  Run: 同 Step 2。
  Expected: 8 個測試全部通過。

- [ ] **Step 6：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/reading/ScoreTint.kt hymnchtv/src/main/java/org/cog/hymnchtv/reading/ScorePages.kt \
          hymnchtv/src/test/java/org/cog/hymnchtv/reading/ScoreTintTest.kt hymnchtv/src/test/java/org/cog/hymnchtv/reading/ScorePagesTest.kt
  git commit -m "feat: add score tint policy and score page names" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

### Task P4：ReadingPrefs（從 SharedPreferences 安全讀取）

**Files:**
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/reading/ReadingPrefs.kt`
- Create（測試工具）: `hymnchtv/src/test/java/org/cog/hymnchtv/reading/FakeSharedPreferences.kt`
- Test: `hymnchtv/src/test/java/org/cog/hymnchtv/reading/ReadingPrefsTest.kt`

**前置:** P3。`SharedPreferences` 是介面，可以在 JVM 測試裡自行實作，不需要 Robolectric。

- [ ] **Step 1：新增測試工具** `FakeSharedPreferences.kt`

  ```kotlin
  package org.cog.hymnchtv.reading

  import android.content.SharedPreferences

  /** Map-backed SharedPreferences for JVM tests; a wrong-type read throws ClassCastException like Android does. */
  class FakeSharedPreferences(initial: Map<String, Any?> = emptyMap()) : SharedPreferences {
      val values: MutableMap<String, Any?> = initial.toMutableMap()

      override fun getAll(): MutableMap<String, *> = values.toMutableMap()
      override fun getString(key: String, defValue: String?): String? = if (key in values) values[key] as String? else defValue

      @Suppress("UNCHECKED_CAST")
      override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? =
          if (key in values) values[key] as MutableSet<String>? else defValues

      override fun getInt(key: String, defValue: Int): Int = if (key in values) values[key] as Int else defValue
      override fun getLong(key: String, defValue: Long): Long = if (key in values) values[key] as Long else defValue
      override fun getFloat(key: String, defValue: Float): Float = if (key in values) values[key] as Float else defValue
      override fun getBoolean(key: String, defValue: Boolean): Boolean = if (key in values) values[key] as Boolean else defValue
      override fun contains(key: String): Boolean = key in values
      override fun edit(): SharedPreferences.Editor = Editor()
      override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
      override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

      private inner class Editor : SharedPreferences.Editor {
          private val pending = mutableMapOf<String, Any?>()
          private val removed = mutableSetOf<String>()
          private var clearAll = false

          private fun put(key: String, value: Any?): SharedPreferences.Editor {
              pending[key] = value
              return this
          }

          override fun putString(key: String, value: String?) = put(key, value)
          override fun putStringSet(key: String, values: MutableSet<String>?) = put(key, values)
          override fun putInt(key: String, value: Int) = put(key, value)
          override fun putLong(key: String, value: Long) = put(key, value)
          override fun putFloat(key: String, value: Float) = put(key, value)
          override fun putBoolean(key: String, value: Boolean) = put(key, value)

          override fun remove(key: String): SharedPreferences.Editor {
              removed += key
              return this
          }

          override fun clear(): SharedPreferences.Editor {
              clearAll = true
              return this
          }

          override fun commit(): Boolean {
              if (clearAll) values.clear()
              removed.forEach { values.remove(it) }
              values.putAll(pending)
              return true
          }

          override fun apply() {
              commit()
          }
      }
  }
  ```

  `android.jar` 的 `key` 參數沒有可空性標註，所以這裡宣告成非空的 `String`。如果編譯器仍回報 override 簽章不符，只調整該參數的 `?`，其餘不動。

- [ ] **Step 2：寫會失敗的測試** `ReadingPrefsTest.kt`

  ```kotlin
  package org.cog.hymnchtv.reading

  import com.google.common.truth.Truth.assertThat
  import org.junit.Test

  class ReadingPrefsTest {
      @Test
      fun emptyPrefsGiveTheDefaults() {
          val sp = FakeSharedPreferences()
          assertThat(ReadingPrefs.displayMode(sp)).isEqualTo(DisplayMode.SCORE_AND_LYRICS)
          assertThat(ReadingPrefs.fontSize(sp)).isEqualTo(LyricsFontSize.MEDIUM)
          assertThat(ReadingPrefs.lyricsFont(sp)).isEqualTo(LyricsFont.KAI)
          assertThat(ReadingPrefs.pageAnimation(sp)).isTrue()
          assertThat(ReadingPrefs.keepScreenOn(sp)).isTrue()
          assertThat(ReadingPrefs.lyricsScale(sp, true)).isEqualTo(LyricsFontSize.MEDIUM.scale)
      }

      @Test
      fun storedValuesAreRead() {
          val sp = FakeSharedPreferences(
              mapOf(
                  ReadingPrefKeys.DISPLAY_MODE to "LYRICS_ONLY",
                  ReadingPrefKeys.LYRICS_FONT_SIZE to "XLARGE",
                  ReadingPrefKeys.LYRICS_FONT to "SYSTEM",
                  ReadingPrefKeys.PAGE_ANIMATION to false,
                  ReadingPrefKeys.KEEP_SCREEN_ON to false,
                  ReadingPrefKeys.LYRICS_SCALE_L to 2.5f,
              )
          )
          assertThat(ReadingPrefs.displayMode(sp)).isEqualTo(DisplayMode.LYRICS_ONLY)
          assertThat(ReadingPrefs.fontSize(sp)).isEqualTo(LyricsFontSize.XLARGE)
          assertThat(ReadingPrefs.lyricsFont(sp)).isEqualTo(LyricsFont.SYSTEM)
          assertThat(ReadingPrefs.pageAnimation(sp)).isFalse()
          assertThat(ReadingPrefs.keepScreenOn(sp)).isFalse()
          assertThat(ReadingPrefs.lyricsScale(sp, false)).isEqualTo(2.5f)
          assertThat(ReadingPrefs.lyricsScale(sp, true)).isEqualTo(LyricsFontSize.XLARGE.scale)
      }

      @Test
      fun wrongTypesFallBackInsteadOfCrashing() {
          val sp = FakeSharedPreferences(
              mapOf(
                  ReadingPrefKeys.DISPLAY_MODE to 3,
                  ReadingPrefKeys.PAGE_ANIMATION to "yes",
                  ReadingPrefKeys.LYRICS_SCALE_P to "big",
              )
          )
          assertThat(ReadingPrefs.displayMode(sp)).isEqualTo(DisplayMode.SCORE_AND_LYRICS)
          assertThat(ReadingPrefs.pageAnimation(sp)).isTrue()
          assertThat(ReadingPrefs.lyricsScale(sp, true)).isEqualTo(LyricsFontSize.MEDIUM.scale)
      }

      @Test
      fun choosingASizeResetsBothOrientations() {
          val sp = FakeSharedPreferences(mapOf(ReadingPrefKeys.LYRICS_SCALE_P to 4f, ReadingPrefKeys.LYRICS_SCALE_L to 3f))
          ReadingPrefs.resetLyricsScale(sp.edit(), LyricsFontSize.SMALL).apply()
          assertThat(sp.values[ReadingPrefKeys.LYRICS_SCALE_P]).isEqualTo(1.0f)
          assertThat(sp.values[ReadingPrefKeys.LYRICS_SCALE_L]).isEqualTo(1.0f)
      }
  }
  ```

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.reading.ReadingPrefsTest' --console=plain`
  Expected: 編譯失敗，`Unresolved reference 'ReadingPrefs'`。

- [ ] **Step 3：實作** `ReadingPrefs.kt`

  ```kotlin
  package org.cog.hymnchtv.reading

  import android.content.SharedPreferences

  /** Typed, never-throwing reads of the reading settings (plan A2). */
  object ReadingPrefs {
      @JvmStatic
      fun displayMode(sp: SharedPreferences): DisplayMode = DisplayMode.fromPref(string(sp, ReadingPrefKeys.DISPLAY_MODE))

      @JvmStatic
      fun fontSize(sp: SharedPreferences): LyricsFontSize = LyricsFontSize.fromPref(string(sp, ReadingPrefKeys.LYRICS_FONT_SIZE))

      @JvmStatic
      fun lyricsFont(sp: SharedPreferences): LyricsFont = LyricsFont.fromPref(string(sp, ReadingPrefKeys.LYRICS_FONT))

      @JvmStatic
      fun pageAnimation(sp: SharedPreferences): Boolean = bool(sp, ReadingPrefKeys.PAGE_ANIMATION, true)

      @JvmStatic
      fun keepScreenOn(sp: SharedPreferences): Boolean = bool(sp, ReadingPrefKeys.KEEP_SCREEN_ON, true)

      /** Pinch scale for the orientation, or the chosen preset's scale when there is no usable stored value. */
      @JvmStatic
      fun lyricsScale(sp: SharedPreferences, portrait: Boolean): Float {
          val key = if (portrait) ReadingPrefKeys.LYRICS_SCALE_P else ReadingPrefKeys.LYRICS_SCALE_L
          val stored = if (sp.contains(key)) runCatching { sp.getFloat(key, 0f) }.getOrNull() else null
          return LyricsScale.resolve(stored, fontSize(sp))
      }

      /** Choosing a size restarts pinch zoom from that size in both orientations. */
      @JvmStatic
      fun resetLyricsScale(editor: SharedPreferences.Editor, size: LyricsFontSize): SharedPreferences.Editor =
          editor.putFloat(ReadingPrefKeys.LYRICS_SCALE_P, size.scale).putFloat(ReadingPrefKeys.LYRICS_SCALE_L, size.scale)

      private fun string(sp: SharedPreferences, key: String): String? = runCatching { sp.getString(key, null) }.getOrNull()

      private fun bool(sp: SharedPreferences, key: String, default: Boolean): Boolean =
          runCatching { sp.getBoolean(key, default) }.getOrDefault(default)
  }
  ```

- [ ] **Step 4：執行 lane P 全部測試**

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.reading.*' --console=plain`
  Expected: 全部通過（P1～P4 共 24 個）。

- [ ] **Step 5：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/reading/ReadingPrefs.kt \
          hymnchtv/src/test/java/org/cog/hymnchtv/reading/FakeSharedPreferences.kt \
          hymnchtv/src/test/java/org/cog/hymnchtv/reading/ReadingPrefsTest.kt
  git commit -m "feat: add typed reading preference readers" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

### Task P5：App 預設主題改為淺色（使用者決策）

**Files:**
- Modify: `hymnchtv/src/main/java/org/cog/hymnchtv/utils/ThemeHelper.java`
- Test: `hymnchtv/src/test/java/org/cog/hymnchtv/utils/ThemeDefaultTest.kt`

**前置:** P4。`MainActivity` 讀 `PREF_THEME` 的預設值在 Task M1 改（`MainActivity.java` 屬於 lane M）。

- [ ] **Step 1：寫會失敗的測試** `ThemeDefaultTest.kt`

  ```kotlin
  package org.cog.hymnchtv.utils

  import com.google.common.truth.Truth.assertThat
  import org.junit.Test

  /** New users get the light theme (user decision 2026-10-02); MainActivity uses the same constant (Task M1). */
  class ThemeDefaultTest {
      @Test
      fun defaultThemeIsLight() {
          assertThat(ThemeHelper.DEFAULT_THEME).isEqualTo(ThemeHelper.Theme.LIGHT)
      }

      @Test
      fun freshProcessStartsWithTheDefault() {
          // No JVM test calls ThemeHelper.setTheme, so the static field still holds its initial value
          assertThat(ThemeHelper.getAppTheme()).isEqualTo(ThemeHelper.DEFAULT_THEME)
      }
  }
  ```

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.utils.ThemeDefaultTest' --console=plain`
  Expected: 編譯失敗，`Unresolved reference 'DEFAULT_THEME'`。

- [ ] **Step 2：修改** `ThemeHelper.java`

  把：

  ```java
      // Note: mTheme will get initialized from DB by ConfigurationUtils on app startup
      private static Theme mTheme = Theme.DARK;
  ```

  改成：

  ```java
      /** Theme for users who never chose one (user decision 2026-10-02: light). MainActivity uses it as the pref default. */
      public static final Theme DEFAULT_THEME = Theme.LIGHT;

      // Note: mTheme is set from PREF_THEME by MainActivity.onCreate
      private static Theme mTheme = DEFAULT_THEME;
  ```

- [ ] **Step 3：執行測試**

  Run: 同 Step 1。
  Expected: 2 個測試通過。

- [ ] **Step 4：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/utils/ThemeHelper.java hymnchtv/src/test/java/org/cog/hymnchtv/utils/ThemeDefaultTest.kt
  git commit -m "feat: default to the light theme for new users" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

  Lane P 完成，回報給協調者合併。

---

## 階段 1 · Lane B：背景

Lane B 的子代理在 worktree `../hymnchtv-a2-b` 工作。

### Task B1：Wcag、BackgroundPreset 註冊表、BackgroundPolicy、PhotoBackground

**Files:**
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/reading/background/Wcag.kt`
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/reading/background/BackgroundPreset.kt`
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/reading/background/BackgroundPolicy.kt`
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/reading/background/PhotoBackground.kt`
- Test: `hymnchtv/src/test/java/org/cog/hymnchtv/reading/background/{WcagTest,BackgroundPresetTest,BackgroundPolicyTest,PhotoPaletteTest,PhotoBackgroundTest}.kt`

**前置:** Task 0。

- [ ] **Step 1：寫會失敗的測試** `WcagTest.kt`

  ```kotlin
  package org.cog.hymnchtv.reading.background

  import com.google.common.truth.Truth.assertThat
  import org.junit.Test

  class WcagTest {
      private val white = 0xFFFFFFFF.toInt()
      private val black = 0xFF000000.toInt()

      @Test
      fun blackOnWhiteIs21() {
          assertThat(Wcag.contrast(black, white)).isWithin(0.01).of(21.0)
          assertThat(Wcag.contrast(white, black)).isWithin(0.01).of(21.0)
      }

      @Test
      fun sameColourIs1() {
          assertThat(Wcag.contrast(0xFF7A3B2E.toInt(), 0xFF7A3B2E.toInt())).isWithin(1e-9).of(1.0)
      }

      @Test
      fun knownPair() {
          // #2b2a28 on #f8f6f0 (宣紙白); 13.27 computed independently with the WCAG 2.x formula
          assertThat(Wcag.contrast(0xFF2B2A28.toInt(), 0xFFF8F6F0.toInt())).isWithin(0.01).of(13.27)
      }

      @Test
      fun blendIsOpaqueAndRounded() {
          assertThat(Wcag.blend(black, white, 0.5f)).isEqualTo(0xFF808080.toInt())
          assertThat(Wcag.blend(white, black, 0f)).isEqualTo(white)
      }
  }
  ```

- [ ] **Step 2：寫會失敗的測試** `BackgroundPresetTest.kt`

  ```kotlin
  package org.cog.hymnchtv.reading.background

  import com.google.common.truth.Truth.assertThat
  import com.google.common.truth.Truth.assertWithMessage
  import org.junit.Test

  class BackgroundPresetTest {
      private fun hex(c: Int) = "#%06x".format(c and 0xFFFFFF)

      @Test
      fun twentyPresetsWithUniqueLowercaseIds() {
          assertThat(BackgroundPreset.entries).hasSize(20)
          assertThat(BackgroundPreset.entries.map { it.id }.toSet()).hasSize(20)
          BackgroundPreset.entries.forEach { assertThat(it.id).matches("[a-z]+") }
      }

      @Test
      fun categoriesMatchTheDesign() {
          val counts = BackgroundPreset.entries.groupingBy { it.category }.eachCount()
          assertThat(counts).containsExactly(
              BackgroundCategory.CALM, 4, BackgroundCategory.GRADIENT, 5,
              BackgroundCategory.MOTIF, 7, BackgroundCategory.NIGHT, 4,
          )
      }

      @Test
      fun textMeetsWcagAaOnEverySwatch() {
          for (p in BackgroundPreset.entries) {
              for (s in p.swatches()) {
                  assertWithMessage("${p.id}: text ${hex(p.textColor)} on ${hex(s)}")
                      .that(Wcag.contrast(p.textColor, s)).isAtLeast(4.5)
              }
          }
      }

      @Test
      fun accentMeetsLargeTextContrastOnEverySwatch() {
          for (p in BackgroundPreset.entries) {
              for (s in p.swatches()) {
                  assertWithMessage("${p.id}: accent ${hex(p.accentColor)} on ${hex(s)}")
                      .that(Wcag.contrast(p.accentColor, s)).isAtLeast(3.0)
              }
          }
      }

      @Test
      fun darkFlagMatchesBaseLuminance() {
          for (p in BackgroundPreset.entries) {
              assertWithMessage(p.id).that(p.isDark).isEqualTo(Wcag.luminance(p.baseColor) < 0.2)
          }
      }

      @Test
      fun fromIdIsTotal() {
          assertThat(BackgroundPreset.fromId("xuan")).isEqualTo(BackgroundPreset.XUAN)
          assertThat(BackgroundPreset.fromId("XUAN")).isNull()
          assertThat(BackgroundPreset.fromId("bg0")).isNull()
          assertThat(BackgroundPreset.fromId(null)).isNull()
      }
  }
  ```

- [ ] **Step 3：寫會失敗的測試** `BackgroundPolicyTest.kt`

  ```kotlin
  package org.cog.hymnchtv.reading.background

  import com.google.common.truth.Truth.assertThat
  import org.junit.Test

  class BackgroundPolicyTest {
      private fun preset(p: BackgroundPreset) = BackgroundChoice.Preset(p)

      @Test
      fun lightThemeDefaults() {
          assertThat(BackgroundPolicy.resolve(null, BackgroundSlot.LYRICS, false, false)).isEqualTo(preset(BackgroundPreset.XUAN))
          assertThat(BackgroundPolicy.resolve(null, BackgroundSlot.MAIN, false, false)).isEqualTo(preset(BackgroundPreset.DAWN))
      }

      @Test
      fun darkThemeDefaultsBothToNightReading() {
          BackgroundSlot.entries.forEach {
              assertThat(BackgroundPolicy.resolve(null, it, true, false)).isEqualTo(preset(BackgroundPreset.NIGHTREAD))
          }
      }

      @Test
      fun userChoiceWinsEvenInDarkTheme() {
          assertThat(BackgroundPolicy.resolve("olive", BackgroundSlot.LYRICS, true, false)).isEqualTo(preset(BackgroundPreset.OLIVE))
      }

      @Test
      fun invalidValuesFallBackToTheDefault() {
          listOf("", "bg0", "XUAN", "5").forEach {
              assertThat(BackgroundPolicy.resolve(it, BackgroundSlot.LYRICS, false, false)).isEqualTo(preset(BackgroundPreset.XUAN))
          }
      }

      @Test
      fun photoNeedsTheFile() {
          assertThat(BackgroundPolicy.resolve(BackgroundPolicy.PHOTO, BackgroundSlot.MAIN, false, true)).isEqualTo(BackgroundChoice.Photo)
          assertThat(BackgroundPolicy.resolve(BackgroundPolicy.PHOTO, BackgroundSlot.MAIN, false, false)).isEqualTo(preset(BackgroundPreset.DAWN))
      }

      @Test
      fun prefValueRoundTrips() {
          for (p in BackgroundPreset.entries) {
              val stored = BackgroundPolicy.prefValue(preset(p))
              assertThat(BackgroundPolicy.resolve(stored, BackgroundSlot.MAIN, true, false)).isEqualTo(preset(p))
          }
          val photo = BackgroundPolicy.prefValue(BackgroundChoice.Photo)
          assertThat(BackgroundPolicy.resolve(photo, BackgroundSlot.LYRICS, false, true)).isEqualTo(BackgroundChoice.Photo)
      }

      @Test
      fun paletteFollowsTheChoice() {
          val ink = BackgroundPolicy.palette(preset(BackgroundPreset.INK))
          assertThat(ink.isDark).isTrue()
          assertThat(ink.paperColor).isEqualTo(BackgroundPreset.INK.baseColor)
          assertThat(ink.textColor).isEqualTo(BackgroundPreset.INK.textColor)
          assertThat(ink.backdropColor).isEqualTo(0)
          assertThat(BackgroundPolicy.palette(BackgroundChoice.Photo)).isEqualTo(BackgroundPolicy.PHOTO_PALETTE)
          assertThat(BackgroundPolicy.PHOTO_PALETTE.isDark).isTrue()
      }

      @Test
      fun slotFromNameNeverThrows() {
          assertThat(BackgroundSlot.fromName("MAIN")).isEqualTo(BackgroundSlot.MAIN)
          assertThat(BackgroundSlot.fromName(null)).isEqualTo(BackgroundSlot.LYRICS)
          assertThat(BackgroundSlot.fromName("main")).isEqualTo(BackgroundSlot.LYRICS)
      }
  }
  ```

- [ ] **Step 4：寫會失敗的測試** `PhotoBackgroundTest.kt`

  ```kotlin
  package org.cog.hymnchtv.reading.background

  import com.google.common.truth.Truth.assertThat
  import org.junit.Test

  class PhotoBackgroundTest {
      @Test
      fun dimIsClampedPercentOfOpaque() {
          assertThat(PhotoBackground.dimAlpha(40)).isEqualTo(102)
          assertThat(PhotoBackground.dimAlpha(0)).isEqualTo(PhotoBackground.dimAlpha(PhotoBackground.DIM_MIN))
          assertThat(PhotoBackground.dimAlpha(100)).isEqualTo(204)
      }

      @Test
      fun blurIsClampedAndScaledByDensity() {
          assertThat(PhotoBackground.blurRadiusPx(10, 3f)).isEqualTo(30f)
          assertThat(PhotoBackground.blurRadiusPx(99, 2f)).isEqualTo(50f)
          assertThat(PhotoBackground.blurRadiusPx(-1, 2f)).isEqualTo(0f)
      }

      @Test
      fun sampleSizeKeepsBothSidesAtLeastTheScreen() {
          assertThat(PhotoBackground.sampleSize(4320, 9600, 1080, 2400)).isEqualTo(4)
          assertThat(PhotoBackground.sampleSize(4000, 3000, 1080, 2400)).isEqualTo(1)
          assertThat(PhotoBackground.sampleSize(1080, 2400, 1080, 2400)).isEqualTo(1)
      }

      @Test
      fun sampleSizeIgnoresBrokenSizes() {
          assertThat(PhotoBackground.sampleSize(0, 100, 10, 10)).isEqualTo(1)
          assertThat(PhotoBackground.sampleSize(100, 100, 0, 10)).isEqualTo(1)
      }
  }
  ```

- [ ] **Step 4b：寫會失敗的測試** `PhotoPaletteTest.kt`（Codex P1：照片背景的可讀性要有證明）

  ```kotlin
  package org.cog.hymnchtv.reading.background

  import com.google.common.truth.Truth.assertThat
  import com.google.common.truth.Truth.assertWithMessage
  import org.junit.Test

  /**
   * Text on a photo is drawn on PHOTO_PALETTE.backdropColor. Blending is monotonic in each channel, so testing
   * every grey from black to white (both ends included) covers the worst case of any photo pixel.
   */
  class PhotoPaletteTest {
      private val palette = BackgroundPolicy.PHOTO_PALETTE
      private val alpha = (palette.backdropColor ushr 24) / 255f

      private fun underPanel(grey: Int): Int {
          val photo = (0xFF shl 24) or (grey shl 16) or (grey shl 8) or grey
          return Wcag.blend(photo, palette.backdropColor, alpha)
      }

      @Test
      fun panelIsAtLeast85PercentOpaque() {
          assertThat(alpha).isAtLeast(0.85f)
      }

      @Test
      fun textAndAccentPassOnAnyPhotoPixel() {
          for (grey in (0..255 step 15) + 255) {
              val behind = underPanel(grey)
              assertWithMessage("text on grey $grey").that(Wcag.contrast(palette.textColor, behind)).isAtLeast(4.5)
              assertWithMessage("accent on grey $grey").that(Wcag.contrast(palette.accentColor, behind)).isAtLeast(3.0)
          }
      }
  }
  ```

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.reading.background.*' --console=plain`
  Expected: 編譯失敗，`Unresolved reference 'Wcag'` 等。

- [ ] **Step 5：實作** `Wcag.kt`

  ```kotlin
  package org.cog.hymnchtv.reading.background

  import kotlin.math.max
  import kotlin.math.min
  import kotlin.math.pow
  import kotlin.math.roundToInt

  /** WCAG 2.x contrast maths on 0xAARRGGBB colours; alpha is ignored (colours are treated as opaque). */
  object Wcag {
      @JvmStatic
      fun luminance(color: Int): Double {
          fun channel(shift: Int): Double {
              val c = (color shr shift and 0xFF) / 255.0
              return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
          }
          return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
      }

      @JvmStatic
      fun contrast(a: Int, b: Int): Double {
          val la = luminance(a)
          val lb = luminance(b)
          return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
      }

      /** [over] painted with [alpha] on top of opaque [base]; the result is opaque. */
      @JvmStatic
      fun blend(base: Int, over: Int, alpha: Float): Int {
          fun mix(shift: Int): Int =
              ((over shr shift and 0xFF) * alpha + (base shr shift and 0xFF) * (1 - alpha)).roundToInt()
          return (0xFF shl 24) or (mix(16) shl 16) or (mix(8) shl 8) or mix(0)
      }
  }
  ```

- [ ] **Step 6：實作** `BackgroundPreset.kt`

  色值全部來自選樣頁的 `shelves` 陣列。`overlays` 的 alpha 是「最強的那一點」：雜訊紋理用 CSS alpha 的兩倍（`feTurbulence` 的峰值約為平均的兩倍，產生器也用同一個值），圖案用 CSS 的 rgba alpha；星夜的星點是設計值 0.28（見 D5，CSS 原本 0.8 會讓壓在星點上的字不合格）。

  ```kotlin
  package org.cog.hymnchtv.reading.background

  import org.cog.hymnchtv.reading.background.BackgroundCategory.CALM
  import org.cog.hymnchtv.reading.background.BackgroundCategory.GRADIENT
  import org.cog.hymnchtv.reading.background.BackgroundCategory.MOTIF
  import org.cog.hymnchtv.reading.background.BackgroundCategory.NIGHT

  enum class BackgroundCategory { CALM, GRADIENT, MOTIF, NIGHT }

  /** A translucent layer drawn over the stops (texture grain, motif, light rays); [maxAlpha] is its strongest pixel. */
  data class Overlay(val color: Int, val maxAlpha: Float)

  private fun argb(hex: String): Int = (0xFF000000L or hex.removePrefix("#").toLong(16)).toInt()

  /**
   * The 20 drawn backgrounds (plan A2). Declaration order is the picker order.
   * [stops] must appear verbatim in res/drawable/bg_<id>.xml (BackgroundResourcesTest), which
   * tools/gen_backgrounds.py writes from the same values.
   */
  enum class BackgroundPreset(
      val id: String,
      val category: BackgroundCategory,
      stopHex: List<String>,
      overlayHex: List<Pair<String, Float>>,
      textHex: String,
      accentHex: String,
  ) {
      XUAN("xuan", CALM, listOf("#f8f6f0"), listOf("#594d40" to 0.14f), "#2b2a28", "#7a3b2e"),
      LINEN("linen", CALM, listOf("#ecebe6"), listOf("#000000" to 0.05f), "#2b2a28", "#3d5a73"),
      PARCHMENT("parchment", CALM, listOf("#f6ead0", "#e3cd9e"), listOf("#735226" to 0.2f), "#3a2f22", "#8a4b1f"),
      MIST("mist", CALM, listOf("#e7eef3"), emptyList(), "#2b2a28", "#2f5d84"),
      DAWN("dawn", GRADIENT, listOf("#fbd9bd", "#fff1e2", "#fffaf4"), emptyList(), "#2b2a28", "#b2542b"),
      SKY("sky", GRADIENT, listOf("#c9def2", "#eef5fb", "#f8fbfe"), emptyList(), "#2b2a28", "#2c5f93"),
      HARVEST("harvest", GRADIENT, listOf("#f3dd9b", "#faf0d2", "#fdf9ec"), emptyList(), "#33291a", "#8c6216"),
      DUSK("dusk", GRADIENT, listOf("#e3d6ef", "#f6e4ec", "#fcf3f6"), emptyList(), "#2b2a28", "#7a3f74"),
      MEADOW("meadow", GRADIENT, listOf("#f6fbf2", "#e5f2df", "#d5ead0"), emptyList(), "#2b2a28", "#3c6b35"),
      STAFF("staff", MOTIF, listOf("#faf9f5"), listOf("#465064" to 0.13f), "#2b2a28", "#34466b"),
      OLIVE("olive", MOTIF, listOf("#f7f8f1"), listOf("#6e8246" to 0.22f, "#5c6e3c" to 0.35f), "#2b2a28", "#4f6230"),
      WHEAT("wheat", MOTIF, listOf("#fffdf6", "#f8f0da"), listOf("#be963c" to 0.25f, "#967328" to 0.35f), "#2b2a28", "#80591a"),
      DOVE("dove", MOTIF, listOf("#eef3f9", "#fafcfe"), listOf("#5a78a0" to 0.16f), "#2b2a28", "#355c8c"),
      RAYS("rays", MOTIF, listOf("#fbe7b9", "#fdf6e6"), listOf("#ffffff" to 0.55f), "#33291a", "#9a5d12"),
      WATER("water", MOTIF, listOf("#f5fbfc", "#e1f0f3"), listOf("#286e8c" to 0.22f), "#2b2a28", "#1f6a80"),
      VINE("vine", MOTIF, listOf("#faf8fb"), listOf("#645078" to 0.3f, "#6e508c" to 0.2f, "#5a7846" to 0.2f), "#2b2a28", "#5e3f73"),
      NIGHTREAD("nightread", NIGHT, listOf("#24211d"), listOf("#e6d9bf" to 0.1f), "#ece4d6", "#e3b77a"),
      // Star dots are capped at 0.28 so text drawn over the brightest one still passes (StarsDrawable reads this)
      STARRY("starry", NIGHT, listOf("#0f1a33", "#1c2a4a"), listOf("#ffffff" to 0.28f), "#e6ebf5", "#f2d48a"),
      DEEPSEA("deepsea", NIGHT, listOf("#0f2a33", "#123d42"), emptyList(), "#e2efee", "#8fd3c7"),
      INK("ink", NIGHT, listOf("#1b1d22"), listOf("#ccccd9" to 0.12f), "#e6e6ea", "#b9c7e8");

      /** Gradient stops (or the single solid colour), top to bottom. */
      val stops: List<Int> = stopHex.map(::argb)
      val overlays: List<Overlay> = overlayHex.map { (hex, alpha) -> Overlay(argb(hex), alpha) }
      val textColor: Int = argb(textHex)
      val accentColor: Int = argb(accentHex)

      /** First stop; on dark backgrounds the score paper is recoloured to this. */
      val baseColor: Int get() = stops.first()
      val isDark: Boolean get() = category == NIGHT

      /** Every colour a reader can see behind text: each stop, plain and under each overlay at full strength. */
      fun swatches(): List<Int> = stops + stops.flatMap { s -> overlays.map { Wcag.blend(s, it.color, it.maxAlpha) } }

      companion object {
          @JvmStatic
          fun fromId(id: String?): BackgroundPreset? = entries.firstOrNull { it.id == id }
      }
  }
  ```

- [ ] **Step 7：實作** `BackgroundPolicy.kt`

  ```kotlin
  package org.cog.hymnchtv.reading.background

  /**
   * Colours used on top of a background (plan A2).
   * [paperColor]: what white score paper becomes on dark backgrounds.
   * [backdropColor]: ARGB of the panel drawn behind text (0 = none); photos need it because their pixels are unknown.
   */
  data class ReadingPalette(
      val textColor: Int,
      val accentColor: Int,
      val isDark: Boolean,
      val paperColor: Int,
      val backdropColor: Int = 0,
  )

  /** The two independently chosen backgrounds; [prefKey] stores a preset id or BackgroundPolicy.PHOTO. */
  enum class BackgroundSlot(val prefKey: String, val lightDefault: BackgroundPreset) {
      MAIN("MainBackground", BackgroundPreset.DAWN),
      LYRICS("LyricsBackground", BackgroundPreset.XUAN);

      companion object {
          /** Never throws; unknown names mean LYRICS. */
          @JvmStatic
          fun fromName(name: String?): BackgroundSlot = entries.firstOrNull { it.name == name } ?: LYRICS
      }
  }

  sealed interface BackgroundChoice {
      data class Preset(val preset: BackgroundPreset) : BackgroundChoice
      data object Photo : BackgroundChoice
  }

  object BackgroundPolicy {
      /** Stored value meaning "the user's own photo" (its file name is in MainActivity.PREF_WALLPAPER). */
      const val PHOTO = "photo"

      @JvmField
      val DARK_DEFAULT = BackgroundPreset.NIGHTREAD

      /**
       * A photo can be any colour, so text sits on an 85 % #1e1e1e panel; PhotoPaletteTest proves AA contrast
       * for every photo pixel from black to white. Dim and blur are cosmetic only.
       */
      @JvmField
      val PHOTO_PALETTE = ReadingPalette(
          textColor = 0xFFF2EFE8.toInt(),
          accentColor = 0xFFE3B77A.toInt(),
          isDark = true,
          paperColor = 0xFF1E1E1E.toInt(),
          backdropColor = 0xD91E1E1E.toInt(),
      )

      @JvmStatic
      fun defaultFor(slot: BackgroundSlot, darkTheme: Boolean): BackgroundPreset =
          if (darkTheme) DARK_DEFAULT else slot.lightDefault

      /** The stored choice if still valid; otherwise the slot default (also when a chosen photo has disappeared). */
      @JvmStatic
      fun resolve(stored: String?, slot: BackgroundSlot, darkTheme: Boolean, photoAvailable: Boolean): BackgroundChoice {
          if (stored == PHOTO && photoAvailable) return BackgroundChoice.Photo
          return BackgroundChoice.Preset(BackgroundPreset.fromId(stored) ?: defaultFor(slot, darkTheme))
      }

      @JvmStatic
      fun palette(choice: BackgroundChoice): ReadingPalette = when (choice) {
          BackgroundChoice.Photo -> PHOTO_PALETTE
          is BackgroundChoice.Preset -> choice.preset.let {
              ReadingPalette(it.textColor, it.accentColor, it.isDark, it.baseColor)
          }
      }

      @JvmStatic
      fun prefValue(choice: BackgroundChoice): String = when (choice) {
          BackgroundChoice.Photo -> PHOTO
          is BackgroundChoice.Preset -> choice.preset.id
      }
  }
  ```

- [ ] **Step 8：實作** `PhotoBackground.kt`

  ```kotlin
  package org.cog.hymnchtv.reading.background

  /** Dim and blur settings for the user's photo background (plan A2). */
  object PhotoBackground {
      const val PREF_DIM = "PhotoDim"
      const val PREF_BLUR = "PhotoBlur"

      /** Percent of black laid over the photo; never 0 so the light text stays readable. */
      const val DIM_MIN = 20
      const val DIM_MAX = 80
      const val DIM_DEFAULT = 40

      /** Blur radius in dp (RenderEffect, API 31+). */
      const val BLUR_MAX = 25
      const val BLUR_DEFAULT = 0

      @JvmStatic
      fun dimAlpha(percent: Int): Int = (percent.coerceIn(DIM_MIN, DIM_MAX) * 255 + 50) / 100

      @JvmStatic
      fun blurRadiusPx(dp: Int, density: Float): Float = dp.coerceIn(0, BLUR_MAX) * density

      /** Largest power-of-two inSampleSize that keeps both sides at least the requested size. */
      @JvmStatic
      fun sampleSize(srcWidth: Int, srcHeight: Int, reqWidth: Int, reqHeight: Int): Int {
          if (srcWidth <= 0 || srcHeight <= 0 || reqWidth <= 0 || reqHeight <= 0) return 1
          var size = 1
          while (srcWidth / (size * 2) >= reqWidth && srcHeight / (size * 2) >= reqHeight) size *= 2
          return size
      }
  }
  ```

- [ ] **Step 9：執行測試**

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.reading.background.*' --console=plain`
  Expected: 全部通過（5 個類別，共 24 個測試）。如果 `textMeetsWcagAaOnEverySwatch` 或 `accent...` 失敗，**不要改門檻**，把失敗訊息回報給協調者（表示色值抄錯，或設計需要調色）。

- [ ] **Step 10：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/reading/background hymnchtv/src/test/java/org/cog/hymnchtv/reading/background
  git commit -m "feat: add background preset registry with WCAG contrast checks" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

### Task B2：背景產生器與 drawable 資源

**Files:**
- Create: `tools/gen_backgrounds.py`
- Generate (commit): `hymnchtv/src/main/res/drawable/bg_*.xml`（20）、`res/drawable/bgx_motif_*.xml`（5）、`res/drawable-nodpi/bgx_tex_*.png`（4）、`res/drawable-xxhdpi/bgx_tile_*.png`（2）
- Test: `hymnchtv/src/test/java/org/cog/hymnchtv/reading/background/BackgroundResourcesTest.kt`

**前置:** B1。命名規則：`bg_<id>.xml` 是 20 個背景本身；`bgx_*` 是它們引用的紋理、拼貼、圖案。舊桌布叫 `bg0`～`bg25`，不會衝突（C1 刪除）。

- [ ] **Step 1：寫會失敗的測試** `BackgroundResourcesTest.kt`

  ```kotlin
  package org.cog.hymnchtv.reading.background

  import com.google.common.truth.Truth.assertThat
  import com.google.common.truth.Truth.assertWithMessage
  import org.junit.Test
  import java.io.File

  /** Fails when res/ and BackgroundPreset drift apart or tools/gen_backgrounds.py was not re-run (plan A2). */
  class BackgroundResourcesTest {
      private val res = File(checkNotNull(System.getProperty("hymnchtv.resDir")) { "hymnchtv.resDir not set" })
      private val drawableDirs: List<File> = res.listFiles { f -> f.isDirectory && f.name.startsWith("drawable") }!!.toList()
      private fun layer(p: BackgroundPreset) = File(res, "drawable/bg_${p.id}.xml")

      @Test
      fun everyPresetHasAGeneratedLayerList() {
          for (p in BackgroundPreset.entries) {
              assertWithMessage(p.id).that(layer(p).isFile).isTrue()
              assertWithMessage(p.id).that(layer(p).readText()).contains("Generated by tools/gen_backgrounds.py")
          }
      }

      @Test
      fun noLayerListWithoutAPreset() {
          val files = File(res, "drawable").listFiles { f -> f.name.matches(Regex("bg_[a-z]+\\.xml")) }!!.map { it.name }.toSet()
          assertThat(files).isEqualTo(BackgroundPreset.entries.map { "bg_${it.id}.xml" }.toSet())
      }

      @Test
      fun layerListsDeclareTheRegistryStops() {
          for (p in BackgroundPreset.entries) {
              val xml = layer(p).readText().lowercase()
              p.stops.forEach { s ->
                  val hex = "#%06x".format(s and 0xFFFFFF)
                  assertWithMessage("${p.id} should contain $hex").that(xml).contains(hex)
              }
          }
      }

      @Test
      fun referencedDrawablesExist() {
          val names = drawableDirs.flatMap { d -> d.listFiles()!!.map { it.name.substringBefore('.') } }.toSet()
          for (p in BackgroundPreset.entries) {
              Regex("@drawable/([a-z0-9_]+)").findAll(layer(p).readText()).forEach {
                  assertWithMessage("${p.id} -> ${it.value}").that(names).contains(it.groupValues[1])
              }
          }
      }

      @Test
      fun generatedFilesStaySmall() {
          val files = drawableDirs.flatMap { d -> d.listFiles { f -> f.name.startsWith("bg_") || f.name.startsWith("bgx_") }!!.toList() }
          assertThat(files).isNotEmpty()
          files.forEach { assertWithMessage(it.path).that(it.length()).isAtMost(5120L) }
      }
  }
  ```

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.reading.background.BackgroundResourcesTest' --console=plain`
  Expected: `everyPresetHasAGeneratedLayerList`、`noLayerListWithoutAPreset`、`layerListsDeclareTheRegistryStops`、`generatedFilesStaySmall` 失敗（還沒有檔案）；`referencedDrawablesExist` 可能因找不到檔案而失敗。

- [ ] **Step 2：新增產生器** `tools/gen_backgrounds.py`

  這份程式在 2026-10-02 已在暫存目錄實際跑過：產生 31 個檔案、最大 4,367 bytes、連跑兩次輸出的 SHA-1 完全相同。

  ```python
  #!/usr/bin/env python3
  """Generate the reading-background drawables (plan A2): layer-lists, motif vectors, texture and tile PNGs.

  Usage: tools/gen_backgrounds.py
  Stdlib only and deterministic: running it twice produces identical files.
  Colours mirror BackgroundPreset.kt; BackgroundResourcesTest checks that every declared stop appears here.
  """
  import math
  import pathlib
  import random
  import struct
  import sys
  import zlib

  ROOT = pathlib.Path(__file__).resolve().parent.parent
  RES = ROOT / "hymnchtv/src/main/res"
  DRAWABLE = RES / "drawable"
  NODPI = RES / "drawable-nodpi"
  XXHDPI = RES / "drawable-xxhdpi"
  ANDROID_NS = 'xmlns:android="http://schemas.android.com/apk/res/android"'
  HEADER = "<!-- Generated by tools/gen_backgrounds.py; do not edit by hand. -->"
  MAX_BYTES = 5120
  NOISE_SIZE = 96   # px, drawable-nodpi
  NOISE_CELL = 12   # px per value-noise lattice cell; 96 / 12 = 8 cells, so the tile wraps seamlessly
  LEVELS = 16       # 4-bit palette: 16 alpha steps of one colour


  def alpha(a):
      return int(a * 255 + 0.5)


  def argb(rgb_hex, a):
      return "#%02x%s" % (alpha(a), rgb_hex.lstrip("#"))


  def num(v):
      text = ("%.2f" % v).rstrip("0").rstrip(".")
      return "0" if text in ("-0", "") else text


  def png(path, width, height, rows, color_type, bit_depth=8, palette=None, trns=None):
      def chunk(tag, data):
          return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

      raw = b"".join(b"\x00" + bytes(r) for r in rows)
      out = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, bit_depth, color_type, 0, 0, 0))
      if palette:
          out += chunk(b"PLTE", bytes(palette))
      if trns:
          out += chunk(b"tRNS", bytes(trns))
      out += chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")
      path.write_bytes(out)


  # ---------- textures and tiles ----------

  NOISE = {  # name: (grain colour, strongest alpha) -- CSS feTurbulence colour, alpha doubled for the peak
      "xuan": ("#594d40", 0.14),
      "parchment": ("#735226", 0.20),
      "nightread": ("#e6d9bf", 0.10),
      "ink": ("#ccccd9", 0.12),
  }


  def noise_tile(name, rgb_hex, max_alpha):
      rnd = random.Random("hymnal-" + name)
      cells = NOISE_SIZE // NOISE_CELL
      lattice = [[rnd.random() for _ in range(cells)] for _ in range(cells)]

      def smooth(x, y):
          gx, gy = x / NOISE_CELL, y / NOISE_CELL
          x0, y0 = int(gx), int(gy)
          fx, fy = gx - x0, gy - y0
          fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
          x1, y1 = (x0 + 1) % cells, (y0 + 1) % cells
          top = lattice[y0][x0] + (lattice[y0][x1] - lattice[y0][x0]) * fx
          bottom = lattice[y1][x0] + (lattice[y1][x1] - lattice[y1][x0]) * fx
          return top + (bottom - top) * fy

      rows = []
      for y in range(NOISE_SIZE):
          levels = [min(LEVELS - 1, int((0.5 * rnd.random() + 0.5 * smooth(x, y)) * LEVELS)) for x in range(NOISE_SIZE)]
          rows.append([(levels[i] << 4) | levels[i + 1] for i in range(0, NOISE_SIZE, 2)])
      r, g, b = bytes.fromhex(rgb_hex.lstrip("#"))
      top = alpha(max_alpha)
      trns = [round(i * top / (LEVELS - 1)) for i in range(LEVELS)]
      png(NODPI / f"bgx_tex_{name}.png", NOISE_SIZE, NOISE_SIZE, rows, color_type=3, bit_depth=4,
          palette=[r, g, b] * LEVELS, trns=trns)


  def staff_tile():
      """240 x 64 dp at xxhdpi (3 px/dp); five 1 dp staff lines at y = 14, 20, 26, 32, 38 dp."""
      s, w, h = 3, 240 * 3, 64 * 3
      line = [70, 80, 100, alpha(0.13)] * w
      clear = [0, 0, 0, 0] * w
      ys = {y * s + k for y in (14, 20, 26, 32, 38) for k in range(s)}
      png(XXHDPI / "bgx_tile_staff.png", w, h, [line if y in ys else clear for y in range(h)], color_type=6)


  def linen_tile():
      """3 dp grid of 1 dp black hairlines at 2.5 %; crossings stack like the two CSS gradients."""
      s = 3
      steps = (0, alpha(0.025), alpha(1 - (1 - 0.025) ** 2))
      rows = [sum(([0, 0, 0, steps[(y < s) + (x < s)]] for x in range(3 * s)), []) for y in range(3 * s)]
      png(XXHDPI / "bgx_tile_linen.png", 3 * s, 3 * s, rows, color_type=6)


  # ---------- motif vectors ----------

  def path(d, fill=None, stroke=None, width=None):
      attrs = [f'android:pathData="{d}"']
      if fill:
          attrs.append(f'android:fillColor="{fill}"')
      if stroke:
          attrs += [f'android:strokeColor="{stroke}"', f'android:strokeWidth="{num(width)}"']
      return "    <path\n" + "".join(f"        {a}\n" for a in attrs[:-1]) + f"        {attrs[-1]} />\n"


  def circle_d(cx, cy, r):
      return f"M{num(cx - r)},{num(cy)} a{num(r)},{num(r)} 0 1,0 {num(2 * r)},0 a{num(r)},{num(r)} 0 1,0 {num(-2 * r)},0 Z"


  def ellipse_d(cx, cy, rx, ry, rot):
      """Closed sub-path for an ellipse rotated by rot degrees (SVG arcs carry the rotation, so no <group> is needed)."""
      t = math.radians(rot)
      dx, dy = rx * math.cos(t), rx * math.sin(t)
      arc = f"a{num(rx)},{num(ry)} {num(rot)} 1,0"
      return f"M{num(cx - dx)},{num(cy - dy)} {arc} {num(2 * dx)},{num(2 * dy)} {arc} {num(-2 * dx)},{num(-2 * dy)} Z"


  def vector(name, w, h, body):
      xml = (f'<?xml version="1.0" encoding="utf-8"?>\n{HEADER}\n<vector {ANDROID_NS}\n'
             f'    android:width="{w}dp"\n    android:height="{h}dp"\n'
             f'    android:viewportWidth="{w}"\n    android:viewportHeight="{h}">\n{body}</vector>\n')
      (DRAWABLE / f"bgx_motif_{name}.xml").write_text(xml, encoding="utf-8")


  def olive():
      leaves = [(150, 24, -35), (132, 40, -50), (118, 30, 20), (110, 58, -55), (96, 50, 25),
                (92, 80, -60), (76, 74, 30), (72, 104, -65), (58, 98, 30), (54, 128, -70)]
      body = path("M170,10 C120,40 80,80 40,160", stroke=argb("#5c6e3c", 0.35), width=2)
      body += path(" ".join(ellipse_d(x, y, 13, 5, r) for x, y, r in leaves), fill=argb("#6e8246", 0.22))
      vector("olive", 170, 170, body)


  def wheat():
      stems = ("M30,180 C34,120 40,80 52,30", "M70,180 C70,120 74,90 86,44", "M110,180 C106,130 108,100 118,60")
      body = "".join(path(d, stroke=argb("#967328", 0.35), width=1.6) for d in stems)
      grains = [ellipse_d(x + dx, y + dy + i * 11, 4, 7, rot)
                for x, y in ((52, 30), (86, 44), (118, 60)) for i in range(5) for dx, dy, rot in ((-4, 10, -25), (5, 14, 25))]
      body += path(" ".join(grains), fill=argb("#be963c", 0.25))
      vector("wheat", 150, 180, body)


  def dove():
      body = path("M18,70 C40,60 58,58 74,62 C70,40 80,18 104,6 C98,26 100,40 108,52 C118,50 128,52 136,58 "
                  "C124,60 116,66 112,74 C100,94 70,102 44,92 C34,88 24,84 4,86 C12,80 16,76 18,70 Z",
                  fill=argb("#5a78a0", 0.16))
      body += path(circle_d(118, 57, 1.8), fill=argb("#3c506e", 0.35))
      vector("dove", 140, 110, body)


  def vine():
      body = path("M0,30 C40,20 70,40 90,70 S130,120 170,130", stroke=argb("#645078", 0.3), width=2)
      grapes = ((70, 62), (64, 72), (76, 72), (70, 82), (60, 82), (80, 82), (66, 92), (74, 92), (70, 101))
      body += path(" ".join(circle_d(x, y, 5) for x, y in grapes), fill=argb("#6e508c", 0.2))
      body += path("M108,92 C120,76 140,80 138,98 C130,96 122,104 120,112 C110,106 104,100 108,92 Z",
                   fill=argb("#5a7846", 0.2))
      vector("vine", 170, 170, body)


  def waves():
      """Two rows of the 120 x 40 CSS wave tile, laid out as on a 360 dp wide screen (row 2 shifted 30 dp)."""
      def row(x0, y):
          d, x = f"M{x0},{y} Q{x0 + 15},{y - 12} {x0 + 30},{y}", x0 + 30
          while x < 360:
              x += 30
              d += f" T{x},{y}"
          return d

      stroke = argb("#286e8c", 0.22)
      vector("waves", 360, 62, path(row(-120, 42), stroke=stroke, width=1.6) + path(row(-90, 20), stroke=stroke, width=1.6))


  # ---------- layer-lists (one per preset) ----------

  def color(c):
      return f'    <item>\n        <color android:color="{c}" />\n    </item>\n'


  def gradient(start, end, center=None, center_y=None, radial=False):
      attrs = ['android:type="radial"' if radial else 'android:type="linear"']
      if not radial:
          attrs.append('android:angle="270"')
      attrs += [f'android:startColor="{start}"']
      if center:
          attrs.append(f'android:centerColor="{center}"')
      attrs.append(f'android:endColor="{end}"')
      if radial:
          attrs += ['android:centerX="0.5"', 'android:gradientRadius="115%p"']
      if center_y is not None:
          attrs.append(f'android:centerY="{num(center_y)}"')
      body = "".join(f"                {a}\n" for a in attrs[:-1]) + f"                {attrs[-1]} />\n"
      return f'    <item>\n        <shape android:shape="rectangle">\n            <gradient\n{body}        </shape>\n    </item>\n'


  def tile(name):
      return f'    <item>\n        <bitmap\n            android:src="@drawable/{name}"\n            android:tileMode="repeat" />\n    </item>\n'


  def motif(name, gravity, width, height, **insets):
      attrs = [f'android:drawable="@drawable/bgx_motif_{name}"', f'android:gravity="{gravity}"']
      if width:
          attrs.append(f'android:width="{width}dp"')
      attrs.append(f'android:height="{height}dp"')
      attrs += [f'android:{k}="{v}dp"' for k, v in insets.items()]
      return "    <item\n" + "".join(f"        {a}\n" for a in attrs[:-1]) + f"        {attrs[-1]} />\n"


  LAYERS = {
      "xuan": [color("#f8f6f0"), tile("bgx_tex_xuan")],
      "linen": [color("#ecebe6"), tile("bgx_tile_linen")],
      "parchment": [gradient("#f6ead0", "#e3cd9e", center="#f6ead0", center_y=0.5, radial=True), tile("bgx_tex_parchment")],
      "mist": [color("#e7eef3")],
      "dawn": [gradient("#fbd9bd", "#fffaf4", center="#fff1e2", center_y=0.45)],
      "sky": [gradient("#c9def2", "#f8fbfe", center="#eef5fb", center_y=0.6)],
      "harvest": [gradient("#f3dd9b", "#fdf9ec", center="#faf0d2", center_y=0.55)],
      "dusk": [gradient("#e3d6ef", "#fcf3f6", center="#f6e4ec", center_y=0.6)],
      "meadow": [gradient("#f6fbf2", "#d5ead0", center="#e5f2df", center_y=0.7)],
      "staff": [color("#faf9f5"), tile("bgx_tile_staff")],
      "olive": [color("#f7f8f1"), motif("olive", "top|right", 150, 150, top=-6, right=-10)],
      "wheat": [gradient("#fffdf6", "#f8f0da"), motif("wheat", "bottom|right", 120, 144, bottom=-10, right=4)],
      "dove": [gradient("#eef3f9", "#fafcfe"), motif("dove", "top|right", 110, 86, top=34, right=10)],
      "rays": [gradient("#fbe7b9", "#fdf6e6", center="#fdf6e6", center_y=0.7)],  # rays drawn in code (RaysDrawable)
      "water": [gradient("#f5fbfc", "#e1f0f3"), motif("waves", "bottom|fill_horizontal", None, 62)],
      "vine": [color("#faf8fb"), motif("vine", "bottom|left", 150, 150, bottom=-10, left=-20)],
      "nightread": [color("#24211d"), tile("bgx_tex_nightread")],
      "starry": [gradient("#0f1a33", "#1c2a4a")],  # stars drawn in code (StarsDrawable)
      "deepsea": [gradient("#0f2a33", "#123d42")],
      "ink": [color("#1b1d22"), tile("bgx_tex_ink")],
  }


  def layer_list(name, items):
      xml = f'<?xml version="1.0" encoding="utf-8"?>\n{HEADER}\n<layer-list {ANDROID_NS}>\n' + "".join(items) + "</layer-list>\n"
      (DRAWABLE / f"bg_{name}.xml").write_text(xml, encoding="utf-8")


  def clean():
      for folder in (DRAWABLE, NODPI, XXHDPI):
          folder.mkdir(parents=True, exist_ok=True)
          for f in list(folder.glob("bg_*.xml")) + list(folder.glob("bgx_*")):
              f.unlink()


  def main():
      clean()
      for name, (rgb, a) in NOISE.items():
          noise_tile(name, rgb, a)
      staff_tile()
      linen_tile()
      for make in (olive, wheat, dove, vine, waves):
          make()
      for name, items in LAYERS.items():
          layer_list(name, items)
      outputs = sorted(p for d in (DRAWABLE, NODPI, XXHDPI) for p in list(d.glob("bg_*.xml")) + list(d.glob("bgx_*")))
      too_big = [p for p in outputs if p.stat().st_size > MAX_BYTES]
      for p in outputs:
          print(f"{p.stat().st_size:6d}  {p.relative_to(ROOT)}")
      if too_big:
          sys.exit(f"over {MAX_BYTES} bytes: {', '.join(p.name for p in too_big)}")
      print(f"Generated {len(outputs)} files ({len(LAYERS)} backgrounds)")


  if __name__ == "__main__":
      main()
  ```

- [ ] **Step 3：產生並檢查**

  ```bash
  chmod +x tools/gen_backgrounds.py
  tools/gen_backgrounds.py | tail -3
  shasum hymnchtv/src/main/res/drawable*/bg_* hymnchtv/src/main/res/drawable*/bgx_* | shasum
  tools/gen_backgrounds.py > /dev/null
  shasum hymnchtv/src/main/res/drawable*/bg_* hymnchtv/src/main/res/drawable*/bgx_* | shasum
  file hymnchtv/src/main/res/drawable-nodpi/bgx_tex_xuan.png hymnchtv/src/main/res/drawable-xxhdpi/bgx_tile_staff.png
  ```

  Expected:
  - 最後一行是 `Generated 31 files (20 backgrounds)`，沒有 `over 5120 bytes` 錯誤。
  - 兩次 `shasum | shasum` 的結果相同（可重現）。
  - `bgx_tex_xuan.png: PNG image data, 96 x 96, 4-bit colormap`；`bgx_tile_staff.png: PNG image data, 720 x 192, 8-bit/color RGBA`。

- [ ] **Step 4：執行測試並確認可建置**

  ```bash
  ./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.reading.background.*' --console=plain
  ./gradlew :hymnchtv:assembleDebug --console=plain
  ```

  Expected: 測試全部通過；`assembleDebug` 成功（aapt2 會驗證所有 layer-list 與 vector 的 XML）。

- [ ] **Step 5：驗證保護機制（每項做完就還原，不要 commit）**

  1. 把 `res/drawable/bg_mist.xml` 的 `#e7eef3` 改成 `#e7eef4`：`layerListsDeclareTheRegistryStops` 要失敗。還原：`tools/gen_backgrounds.py`。
  2. `touch hymnchtv/src/main/res/drawable/bg_extra.xml`：`noLayerListWithoutAPreset` 要失敗。還原：`rm hymnchtv/src/main/res/drawable/bg_extra.xml`。

- [ ] **Step 6：Commit**

  ```bash
  git add tools/gen_backgrounds.py hymnchtv/src/main/res/drawable/bg_*.xml hymnchtv/src/main/res/drawable/bgx_*.xml \
          hymnchtv/src/main/res/drawable-nodpi/bgx_*.png hymnchtv/src/main/res/drawable-xxhdpi/bgx_*.png \
          hymnchtv/src/test/java/org/cog/hymnchtv/reading/background/BackgroundResourcesTest.kt
  git commit -m "feat: generate the 20 reading backgrounds as drawables" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

### Task B3：Android 端的背景繪製與套用

**Files:**
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/reading/background/BackgroundDrawables.kt`（含 `RaysDrawable`、`StarsDrawable`）
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/reading/background/BackgroundApplier.kt`
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/reading/background/BackgroundPrefs.kt`
- Create: `hymnchtv/src/main/res/values/ids_reading.xml`
- Test（instrumented）: `hymnchtv/src/androidTest/java/org/cog/hymnchtv/reading/background/BackgroundDrawablesTest.kt`

**前置:** B2。這些是 Android 邊界，沒有 JVM 測試；Step 5 的 instrumented test 在這裡只編譯，V1 在 API 24 與 34 執行。

- [ ] **Step 1：新增** `BackgroundDrawables.kt`

  ```kotlin
  package org.cog.hymnchtv.reading.background

  import android.content.Context
  import android.graphics.Canvas
  import android.graphics.Color
  import android.graphics.ColorFilter
  import android.graphics.Paint
  import android.graphics.Path
  import android.graphics.PixelFormat
  import android.graphics.RadialGradient
  import android.graphics.Rect
  import android.graphics.Shader
  import android.graphics.drawable.ColorDrawable
  import android.graphics.drawable.Drawable
  import android.graphics.drawable.GradientDrawable
  import android.graphics.drawable.LayerDrawable
  import androidx.annotation.DrawableRes
  import androidx.annotation.StringRes
  import androidx.appcompat.content.res.AppCompatResources
  import org.cog.hymnchtv.R
  import kotlin.math.cos
  import kotlin.math.hypot
  import kotlin.math.sin

  /** Maps BackgroundPreset to its drawable and name (plan A2). */
  object BackgroundDrawables {
      /** A new drawable for [preset]; rays and stars are drawn in code on top of the generated layer-list. */
      @JvmStatic
      fun create(context: Context, preset: BackgroundPreset): Drawable {
          val base = AppCompatResources.getDrawable(context, drawableRes(preset)) ?: ColorDrawable(preset.baseColor)
          val extra: Drawable? = when (preset) {
              BackgroundPreset.RAYS -> RaysDrawable()
              BackgroundPreset.STARRY -> StarsDrawable(context.resources.displayMetrics.density)
              else -> null
          }
          return if (extra == null) base else LayerDrawable(arrayOf(base, extra))
      }

      @DrawableRes
      @JvmStatic
      fun drawableRes(preset: BackgroundPreset): Int = when (preset) {
          BackgroundPreset.XUAN -> R.drawable.bg_xuan
          BackgroundPreset.LINEN -> R.drawable.bg_linen
          BackgroundPreset.PARCHMENT -> R.drawable.bg_parchment
          BackgroundPreset.MIST -> R.drawable.bg_mist
          BackgroundPreset.DAWN -> R.drawable.bg_dawn
          BackgroundPreset.SKY -> R.drawable.bg_sky
          BackgroundPreset.HARVEST -> R.drawable.bg_harvest
          BackgroundPreset.DUSK -> R.drawable.bg_dusk
          BackgroundPreset.MEADOW -> R.drawable.bg_meadow
          BackgroundPreset.STAFF -> R.drawable.bg_staff
          BackgroundPreset.OLIVE -> R.drawable.bg_olive
          BackgroundPreset.WHEAT -> R.drawable.bg_wheat
          BackgroundPreset.DOVE -> R.drawable.bg_dove
          BackgroundPreset.RAYS -> R.drawable.bg_rays
          BackgroundPreset.WATER -> R.drawable.bg_water
          BackgroundPreset.VINE -> R.drawable.bg_vine
          BackgroundPreset.NIGHTREAD -> R.drawable.bg_nightread
          BackgroundPreset.STARRY -> R.drawable.bg_starry
          BackgroundPreset.DEEPSEA -> R.drawable.bg_deepsea
          BackgroundPreset.INK -> R.drawable.bg_ink
      }

      @StringRes
      @JvmStatic
      fun nameRes(preset: BackgroundPreset): Int = when (preset) {
          BackgroundPreset.XUAN -> R.string.bg_name_xuan
          BackgroundPreset.LINEN -> R.string.bg_name_linen
          BackgroundPreset.PARCHMENT -> R.string.bg_name_parchment
          BackgroundPreset.MIST -> R.string.bg_name_mist
          BackgroundPreset.DAWN -> R.string.bg_name_dawn
          BackgroundPreset.SKY -> R.string.bg_name_sky
          BackgroundPreset.HARVEST -> R.string.bg_name_harvest
          BackgroundPreset.DUSK -> R.string.bg_name_dusk
          BackgroundPreset.MEADOW -> R.string.bg_name_meadow
          BackgroundPreset.STAFF -> R.string.bg_name_staff
          BackgroundPreset.OLIVE -> R.string.bg_name_olive
          BackgroundPreset.WHEAT -> R.string.bg_name_wheat
          BackgroundPreset.DOVE -> R.string.bg_name_dove
          BackgroundPreset.RAYS -> R.string.bg_name_rays
          BackgroundPreset.WATER -> R.string.bg_name_water
          BackgroundPreset.VINE -> R.string.bg_name_vine
          BackgroundPreset.NIGHTREAD -> R.string.bg_name_nightread
          BackgroundPreset.STARRY -> R.string.bg_name_starry
          BackgroundPreset.DEEPSEA -> R.string.bg_name_deepsea
          BackgroundPreset.INK -> R.string.bg_name_ink
      }

      @StringRes
      @JvmStatic
      fun nameRes(choice: BackgroundChoice): Int = when (choice) {
          BackgroundChoice.Photo -> R.string.bg_photo
          is BackgroundChoice.Preset -> nameRes(choice.preset)
      }

      /** Rounded panel behind text for [ReadingPalette.backdropColor]; null when the palette needs none. */
      @JvmStatic
      fun backdrop(context: Context, palette: ReadingPalette): Drawable? {
          if (palette.backdropColor == 0) return null
          return GradientDrawable().apply {
              setColor(palette.backdropColor)
              cornerRadius = 10f * context.resources.displayMetrics.density
          }
      }
  }

  /**
   * 光芒: CSS repeating-conic-gradient(from 0deg at 50% -10%, white .55 0-4deg, clear 4-12deg).
   * Drawn as one Path, so no screen-sized bitmap cache (unlike a full-screen VectorDrawable).
   */
  class RaysDrawable : Drawable() {
      private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = RAY_COLOR }
      private val path = Path()

      override fun onBoundsChange(bounds: Rect) {
          path.reset()
          val cx = bounds.exactCenterX()
          val cy = bounds.top - bounds.height() * 0.1f
          val reach = 2f * hypot(bounds.width().toFloat(), bounds.height().toFloat())
          var deg = 0
          while (deg < 360) {
              // CSS conic angles: 0deg points up, clockwise
              path.moveTo(cx, cy)
              path.lineTo(cx + reach * sin(rad(deg)), cy - reach * cos(rad(deg)))
              path.lineTo(cx + reach * sin(rad(deg + WIDTH_DEG)), cy - reach * cos(rad(deg + WIDTH_DEG)))
              path.close()
              deg += PERIOD_DEG
          }
      }

      override fun draw(canvas: Canvas) {
          canvas.drawPath(path, paint)
      }

      override fun setAlpha(alpha: Int) {
          paint.alpha = (RAY_COLOR ushr 24) * alpha / 255
          invalidateSelf()
      }

      override fun setColorFilter(colorFilter: ColorFilter?) {
          paint.colorFilter = colorFilter
          invalidateSelf()
      }

      @Suppress("OVERRIDE_DEPRECATION")
      override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

      private fun rad(deg: Int): Float = Math.toRadians(deg.toDouble()).toFloat()

      private companion object {
          const val PERIOD_DEG = 12
          const val WIDTH_DEG = 4
          const val RAY_COLOR = 0x8DFFFFFF.toInt()   // white at .55
      }
  }

  /**
   * 星夜: five faint stars at the CSS positions; radius 1.5x the CSS value so they survive the fade-out.
   * The brightest star uses the STARRY overlay alpha from the registry (contrast-tested); the others keep the CSS ratios.
   */
  class StarsDrawable(private val density: Float) : Drawable() {
      private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
      private var alphaScale = 1f

      override fun draw(canvas: Canvas) {
          val b = bounds
          for (star in STARS) {
              val cx = b.left + b.width() * star.x
              val cy = b.top + b.height() * star.y
              val radius = star.radiusDp * density * 1.5f
              val alpha = (255 * star.relativeAlpha * MAX_ALPHA * alphaScale).toInt()
              paint.shader = RadialGradient(cx, cy, radius, Color.argb(alpha, 255, 255, 255), Color.TRANSPARENT, Shader.TileMode.CLAMP)
              canvas.drawCircle(cx, cy, radius, paint)
          }
      }

      override fun setAlpha(alpha: Int) {
          alphaScale = alpha / 255f
          invalidateSelf()
      }

      override fun setColorFilter(colorFilter: ColorFilter?) {
          paint.colorFilter = colorFilter
          invalidateSelf()
      }

      @Suppress("OVERRIDE_DEPRECATION")
      override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

      private data class Star(val x: Float, val y: Float, val radiusDp: Float, val relativeAlpha: Float)

      private companion object {
          val MAX_ALPHA = BackgroundPreset.STARRY.overlays.maxOf { it.maxAlpha }
          // CSS alphas .8 .7 .6 .6 .5, relative to the brightest
          val STARS = listOf(
              Star(0.20f, 0.18f, 1.2f, 1.0f),
              Star(0.72f, 0.12f, 1.0f, 0.875f),
              Star(0.85f, 0.34f, 1.4f, 0.75f),
              Star(0.38f, 0.08f, 1.0f, 0.75f),
              Star(0.58f, 0.28f, 1.0f, 0.625f),
          )
      }
  }
  ```

- [ ] **Step 2：新增** `BackgroundApplier.kt`

  ```kotlin
  package org.cog.hymnchtv.reading.background

  import android.content.SharedPreferences
  import android.graphics.Bitmap
  import android.graphics.BitmapFactory
  import android.graphics.Color
  import android.graphics.PorterDuff
  import android.graphics.RenderEffect
  import android.graphics.Shader
  import android.os.Build
  import android.widget.ImageView
  import org.cog.hymnchtv.R
  import timber.log.Timber
  import java.io.File

  /** Shows a background in a full-size ImageView that sits behind the content (plan A2). */
  object BackgroundApplier {
      /**
       * Applies [choice] and returns what is really on screen: a photo that cannot be decoded is replaced by
       * [fallback], so the caller's palette always matches the pixels.
       */
      @JvmStatic
      fun apply(target: ImageView, choice: BackgroundChoice, prefs: SharedPreferences, photo: File?, fallback: BackgroundPreset): BackgroundChoice {
          if (choice is BackgroundChoice.Preset) {
              showPreset(target, choice.preset)
              return choice
          }
          val metrics = target.resources.displayMetrics
          val bitmap = photo?.let { decodePhoto(it, metrics.widthPixels, metrics.heightPixels) }
          if (bitmap == null) {
              Timber.w("Background photo unreadable (%s); showing %s", photo, fallback.id)
              showPreset(target, fallback)
              return BackgroundChoice.Preset(fallback)
          }
          target.background = null
          target.scaleType = ImageView.ScaleType.CENTER_CROP
          target.setImageBitmap(bitmap)
          val dim = intPref(prefs, PhotoBackground.PREF_DIM, PhotoBackground.DIM_DEFAULT)
          target.setColorFilter(Color.argb(PhotoBackground.dimAlpha(dim), 0, 0, 0), PorterDuff.Mode.SRC_ATOP)
          val blur = intPref(prefs, PhotoBackground.PREF_BLUR, PhotoBackground.BLUR_DEFAULT)
          setBlur(target, PhotoBackground.blurRadiusPx(blur, metrics.density))
          return choice
      }

      /** Decodes [file] no larger than needed for [reqWidth] x [reqHeight]; null if it is not an image. */
      @JvmStatic
      fun decodePhoto(file: File, reqWidth: Int, reqHeight: Int): Bitmap? {
          val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
          BitmapFactory.decodeFile(file.path, bounds)
          if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
          val options = BitmapFactory.Options().apply {
              inSampleSize = PhotoBackground.sampleSize(bounds.outWidth, bounds.outHeight, reqWidth, reqHeight)
          }
          return BitmapFactory.decodeFile(file.path, options)
      }

      private fun showPreset(target: ImageView, preset: BackgroundPreset) {
          target.setImageDrawable(null)
          target.clearColorFilter()
          setBlur(target, 0f)
          target.background = BackgroundDrawables.create(target.context, preset)
      }

      private fun setBlur(target: ImageView, radiusPx: Float) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
              target.setRenderEffect(
                  if (radiusPx > 0f) RenderEffect.createBlurEffect(radiusPx, radiusPx, Shader.TileMode.CLAMP) else null
              )
              // View has no getter for its RenderEffect; record the radius so tests can see what was applied
              target.setTag(R.id.tag_blur_radius, radiusPx)
          }
      }

      /** Blur radius last applied to [target] (0 when none or below API 31); for tests. */
      @JvmStatic
      fun appliedBlurRadius(target: ImageView): Float = (target.getTag(R.id.tag_blur_radius) as? Float) ?: 0f

      private fun intPref(prefs: SharedPreferences, key: String, default: Int): Int =
          runCatching { prefs.getInt(key, default) }.getOrDefault(default)
  }
  ```

- [ ] **Step 3：新增** `BackgroundPrefs.kt`

  ```kotlin
  package org.cog.hymnchtv.reading.background

  import android.content.SharedPreferences
  import android.widget.ImageView
  import org.cog.hymnchtv.MainActivity
  import org.cog.hymnchtv.persistance.FileBackend
  import org.cog.hymnchtv.utils.ThemeHelper
  import org.cog.hymnchtv.utils.WallPaperUtil
  import java.io.File

  /** Reads the background settings and applies them; the single entry point for MainActivity and ContentHandler. */
  object BackgroundPrefs {
      /** The user's cropped photo (WallPaperUtil), or null if none is stored or the file is gone. */
      @JvmStatic
      fun photoFile(prefs: SharedPreferences): File? {
          val name = runCatching { prefs.getString(MainActivity.PREF_WALLPAPER, null) }.getOrNull() ?: return null
          return FileBackend.getHymnchtvStore(WallPaperUtil.DIR_WALLPAPER + name, false)?.takeIf { it.isFile }
      }

      @JvmStatic
      fun isDarkTheme(): Boolean = ThemeHelper.isAppTheme(ThemeHelper.Theme.DARK)

      @JvmStatic
      fun resolve(prefs: SharedPreferences, slot: BackgroundSlot): BackgroundChoice = BackgroundPolicy.resolve(
          runCatching { prefs.getString(slot.prefKey, null) }.getOrNull(), slot, isDarkTheme(), photoFile(prefs) != null,
      )

      /** Shows the slot's background in [target] and returns the palette matching what is actually shown. */
      @JvmStatic
      fun applyTo(target: ImageView, prefs: SharedPreferences, slot: BackgroundSlot): ReadingPalette {
          val applied = BackgroundApplier.apply(
              target, resolve(prefs, slot), prefs, photoFile(prefs), BackgroundPolicy.defaultFor(slot, isDarkTheme()),
          )
          return BackgroundPolicy.palette(applied)
      }
  }
  ```

  確認 `FileBackend.getHymnchtvStore(String, boolean)` 是 `public static File`（`persistance/FileBackend.java:232`）；`WallPaperUtil.DIR_WALLPAPER` 和 `MainActivity.PREF_WALLPAPER` 是 `public static final String`。

- [ ] **Step 4：新增** `hymnchtv/src/main/res/values/ids_reading.xml`

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <resources>
      <!-- BackgroundApplier records the applied blur radius under this tag (View has no RenderEffect getter) -->
      <item name="tag_blur_radius" type="id" />
  </resources>
  ```

- [ ] **Step 5：新增 instrumented test** `BackgroundDrawablesTest.kt`

  ```kotlin
  package org.cog.hymnchtv.reading.background

  import android.content.Context
  import android.graphics.Bitmap
  import android.graphics.Canvas
  import android.graphics.Color
  import android.os.Build
  import android.widget.ImageView
  import androidx.test.ext.junit.runners.AndroidJUnit4
  import androidx.test.platform.app.InstrumentationRegistry
  import com.google.common.truth.Truth.assertThat
  import com.google.common.truth.Truth.assertWithMessage
  import org.cog.hymnchtv.MainActivity
  import org.junit.After
  import org.junit.Test
  import org.junit.runner.RunWith
  import java.io.File

  /** Run on API 24 and 34 (Task V1): every generated layer-list and vector must inflate and draw on both. */
  @RunWith(AndroidJUnit4::class)
  class BackgroundDrawablesTest {
      private val instrumentation = InstrumentationRegistry.getInstrumentation()
      private val ctx: Context = instrumentation.targetContext
      private val prefs = ctx.getSharedPreferences("BackgroundDrawablesTest", Context.MODE_PRIVATE)

      @After
      fun clearPrefs() {
          prefs.edit().clear().commit()
      }

      @Test
      fun everyPresetInflatesAndDraws() {
          for (preset in BackgroundPreset.entries) {
              val drawable = BackgroundDrawables.create(ctx, preset)
              val bitmap = Bitmap.createBitmap(270, 600, Bitmap.Config.ARGB_8888)
              drawable.setBounds(0, 0, bitmap.width, bitmap.height)
              drawable.draw(Canvas(bitmap))
              val centre = bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)
              assertWithMessage("${preset.id} centre pixel").that(Color.alpha(centre)).isEqualTo(255)
              bitmap.recycle()
          }
      }

      @Test
      fun unreadablePhotoFallsBackToThePreset() {
          val junk = File(ctx.cacheDir, "not-an-image.jpg").apply { writeText("not an image") }
          lateinit var applied: BackgroundChoice
          lateinit var view: ImageView
          instrumentation.runOnMainSync {
              view = ImageView(ctx)
              applied = BackgroundApplier.apply(view, BackgroundChoice.Photo, prefs, junk, BackgroundPreset.XUAN)
          }
          assertThat(applied).isEqualTo(BackgroundChoice.Preset(BackgroundPreset.XUAN))
          assertThat(view.drawable).isNull()
          assertThat(view.background).isNotNull()
      }

      @Test
      fun switchingFromPhotoToPresetClearsBlur() {
          val photo = File(ctx.cacheDir, "photo.png")
          val source = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
          photo.outputStream().use { source.compress(Bitmap.CompressFormat.PNG, 100, it) }
          prefs.edit().putInt(PhotoBackground.PREF_BLUR, 10).commit()
          lateinit var view: ImageView
          var blurredRadius = 0f
          instrumentation.runOnMainSync {
              view = ImageView(ctx)
              BackgroundApplier.apply(view, BackgroundChoice.Photo, prefs, photo, BackgroundPreset.XUAN)
              blurredRadius = BackgroundApplier.appliedBlurRadius(view)
              BackgroundApplier.apply(view, BackgroundChoice.Preset(BackgroundPreset.MIST), prefs, photo, BackgroundPreset.XUAN)
          }
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
              assertThat(blurredRadius).isGreaterThan(0f)
          }
          assertThat(BackgroundApplier.appliedBlurRadius(view)).isEqualTo(0f)
          assertThat(view.drawable).isNull()
          assertThat(view.colorFilter).isNull()
      }

      @Test
      fun photoPrefKeyIsTheWallpaperKey() {
          // BackgroundPrefs.photoFile reads the file name WallPaperUtil writes
          assertThat(MainActivity.PREF_WALLPAPER).isEqualTo("WallPaper")
      }
  }
  ```

- [ ] **Step 6：建置與 lint**

  ```bash
  ./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug :hymnchtv:assembleDebugAndroidTest :hymnchtv:lintDebug --console=plain
  grep -c "\[NewApi\]" hymnchtv/build/reports/lint-results-debug.txt
  ```

  Expected: `BUILD SUCCESSFUL`；`NewApi` 數量和 Task 0 相同（`RenderEffect` 有 `SDK_INT` 判斷）。不要執行 `connectedDebugAndroidTest`（V1 才跑）。

- [ ] **Step 7：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/reading/background/BackgroundDrawables.kt \
          hymnchtv/src/main/java/org/cog/hymnchtv/reading/background/BackgroundApplier.kt \
          hymnchtv/src/main/java/org/cog/hymnchtv/reading/background/BackgroundPrefs.kt \
          hymnchtv/src/main/res/values/ids_reading.xml \
          hymnchtv/src/androidTest/java/org/cog/hymnchtv/reading/background/BackgroundDrawablesTest.kt
  git commit -m "feat: draw reading backgrounds and the user photo with dim and blur" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

  Lane B 完成，回報給協調者合併。

---

## 階段 1 · Lane F：歌詞字型

Lane F 的子代理在 worktree `../hymnchtv-a2-f` 工作。F0 先修正歌詞資料，因為字型必須涵蓋修正後的歌詞。字型原檔（約 40 MB）和 fontTools venv 都不進 git，要在這個 worktree 裡各準備一份（Task F2 Step 1～2）。

### Task F0：修正 `xg171.txt` 的私用區字元（使用者決策）

**Files:**
- Modify: `hymnchtv/src/main/assets/lyrics_xg_text/xg171.txt`
- Regenerate (commit): `hymnchtv/src/main/assets/lyrics_xg_text_hant_tw/xg171.txt`、`lyrics_xg_text_hant_hk/xg171.txt`、`lyrics_hant_manifest.txt`、`lyrics_t2s_map.txt`（後兩者只在內容有變時才會出現在 diff）

**前置:** Task 0。需要 OpenCC CLI（`brew install opencc`，A 的 Task 0 已安裝）。

第 37 行是 `“这么大的救恩”<U+E5F2>即已赐给我们，`，`U+E5F2` 是私用區字元（UTF-8 `EE 97 B2`），任何字型都沒有它。改成全形逗號「，」（`U+FF0C`，UTF-8 `EF BC 8C`）。

- [ ] **Step 1：確認現況**

  ```bash
  F=hymnchtv/src/main/assets/lyrics_xg_text/xg171.txt
  file "$F"
  grep -c $'\xee\x97\xb2' "$F" hymnchtv/src/main/assets/lyrics_xg_text_hant_*/xg171.txt
  grep -rlc $'\xee\x97\xb2' hymnchtv/src/main/assets/lyrics_*_text | wc -l
  ```

  Expected: `UTF-8 text, with CRLF line terminators`；三個檔案各 `1`；最後一行 `1`（整個簡體歌詞只有這一處）。不符就停下來回報。

- [ ] **Step 2：只替換這三個位元組（保留 CRLF 與其他內容）**

  ```bash
  python3 - <<'EOF'
  import pathlib
  p = pathlib.Path("hymnchtv/src/main/assets/lyrics_xg_text/xg171.txt")
  data = p.read_bytes()
  bad, good = "".encode("utf-8"), "，".encode("utf-8")
  assert data.count(bad) == 1, data.count(bad)
  line = data.split(b"\r\n")[36]
  assert bad in line and line.startswith("“这么大的救恩”".encode("utf-8")), line
  p.write_bytes(data.replace(bad, good))
  print("fixed")
  EOF
  git diff --stat hymnchtv/src/main/assets/lyrics_xg_text/xg171.txt
  file hymnchtv/src/main/assets/lyrics_xg_text/xg171.txt
  ```

  Expected: `fixed`；diff 是 `1 file changed, 1 insertion(+), 1 deletion(-)`；仍是 `with CRLF line terminators`。

- [ ] **Step 3：重新產生繁體歌詞**

  ```bash
  tools/gen_lyrics_hant.py
  git status --short hymnchtv/src/main/assets
  grep -c $'\xee\x97\xb2' hymnchtv/src/main/assets/lyrics_xg_text_hant_*/xg171.txt
  ```

  Expected: 變動的只有 `lyrics_xg_text/xg171.txt`、兩個 `lyrics_xg_text_hant_*/xg171.txt`、`lyrics_hant_manifest.txt`，可能還有 `lyrics_t2s_map.txt`；兩個繁體檔的計數都是 `0`。不要加 `--report`（校對 CSV 不需要更新）。若有其他檔案變動，停下來回報（表示 OpenCC 版本和 A 產生時不同）。

- [ ] **Step 4：執行 A 的同步測試**

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.lyrics.*' --tests 'org.cog.hymnchtv.search.*' --console=plain`
  Expected: 全部通過（`LyricsHantAssetsTest` 確認 manifest 與檔案一致）。

- [ ] **Step 5：Commit**

  ```bash
  git add hymnchtv/src/main/assets/lyrics_xg_text/xg171.txt hymnchtv/src/main/assets/lyrics_xg_text_hant_tw/xg171.txt \
          hymnchtv/src/main/assets/lyrics_xg_text_hant_hk/xg171.txt hymnchtv/src/main/assets/lyrics_hant_manifest.txt \
          hymnchtv/src/main/assets/lyrics_t2s_map.txt
  git commit -m "fix: replace a private-use character in xg171 lyrics with a full-width comma" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

### Task F1：字型同步測試（先寫，會失敗）

**Files:**
- Test: `hymnchtv/src/test/java/org/cog/hymnchtv/reading/FontSubsetTest.kt`

**前置:** F0；Task 0（`hymnchtv.resDir`、`hymnchtv.repoRoot` 系統屬性，以及 `strings_reading.xml`）。

測試不解析 TrueType（Codex 建議簡化）：產生工具用 fontTools 把字型實際的涵蓋範圍和命名紀錄寫進 manifest，測試用字型檔的 SHA-256 確認 manifest 描述的就是這個檔案。

manifest 格式（Tab 分隔，`# ` 開頭是說明行）：

```
font	sc	hymnchtv/src/main/res/font/hymnal_kai_sc.ttf	<sha256>
name	sc	<nameID>	<text>          （每一筆命名紀錄一行）
covers	sc	0020-007E 00D7 2014 ...   （cmap 的十六進位範圍）
```

- [ ] **Step 1：寫會失敗的測試** `FontSubsetTest.kt`

  ```kotlin
  package org.cog.hymnchtv.reading

  import com.google.common.truth.Truth.assertThat
  import com.google.common.truth.Truth.assertWithMessage
  import org.junit.Test
  import java.io.File
  import java.security.MessageDigest

  /**
   * HymnalKai SC/TC must cover every character of their lyrics and of every strings file, with no exceptions,
   * and carry no OFL Reserved Font Name (plan A2). Coverage comes from tools/font_subset_manifest.txt, which
   * tools/gen_font_subset.py writes from the font's real cmap; the SHA-256 check ties it to the committed font.
   */
  class FontSubsetTest {
      private fun dir(property: String) = File(checkNotNull(System.getProperty(property)) { "$property not set" })
      private val res = dir("hymnchtv.resDir")
      private val assets = dir("hymnchtv.assetsDir")
      private val repoRoot = dir("hymnchtv.repoRoot")

      private val rows: List<List<String>> by lazy {
          File(repoRoot, "tools/font_subset_manifest.txt").readLines()
              .filter { it.isNotBlank() && !it.startsWith("# ") }
              .map { it.split('\t') }
      }

      private fun row(kind: String, variant: String): List<String> = rows.single { it[0] == kind && it[1] == variant }

      private fun covered(variant: String): Set<Int> = row("covers", variant)[2].split(' ').flatMap { run ->
          val ends = run.split('-').map { it.toInt(16) }
          (ends.first()..ends.last()).toList()
      }.toSet()

      /** Same rule as tools/gen_font_subset.py: everything except C0 controls, DEL and the BOM. */
      private fun codePoints(files: List<File>): Set<Int> = files
          .flatMap { it.readText(Charsets.UTF_8).codePoints().toArray().asList() }
          .filter { it >= 0x20 && it != 0x7F && it != 0xFEFF }
          .toSet()

      private fun lyricFiles(dirPattern: Regex): List<File> =
          assets.listFiles { f -> f.isDirectory && dirPattern.matches(f.name) }!!
              .flatMap { d -> d.listFiles { f -> f.name.endsWith(".txt") }!!.toList() }

      private fun stringFiles(): List<File> =
          res.listFiles { f -> f.isDirectory && f.name.startsWith("values") }!!
              .flatMap { d -> d.listFiles { f -> f.name.startsWith("strings") && f.name.endsWith(".xml") }!!.toList() }

      private fun assertCovers(variant: String, lyricDirs: Regex) {
          val lyrics = codePoints(lyricFiles(lyricDirs))
          assertThat(lyrics.size).isGreaterThan(2000)   // guards against a wrong assets path
          val strings = codePoints(stringFiles())
          assertThat(strings.size).isGreaterThan(300)
          val missing = (lyrics + strings) - covered(variant)
          val listing = missing.sorted().joinToString(" ") { "U+%04X(%s)".format(it, String(Character.toChars(it))) }
          assertWithMessage("$variant font lacks $listing (fix the text or regenerate; there is no exception list)")
              .that(missing).isEmpty()
      }

      @Test
      fun scCoversSimplifiedLyricsAndAllStrings() = assertCovers("sc", Regex("lyrics_[a-z]+_text"))

      @Test
      fun tcCoversBothTraditionalVariantsAndAllStrings() = assertCovers("tc", Regex("lyrics_[a-z]+_text_hant_(tw|hk)"))

      @Test
      fun committedFontsAreTheOnesTheManifestDescribes() {
          for (variant in listOf("sc", "tc")) {
              val (_, _, path, digest) = row("font", variant)
              assertThat(path).isEqualTo("hymnchtv/src/main/res/font/hymnal_kai_$variant.ttf")
              assertWithMessage(path).that(sha256(File(repoRoot, path))).isEqualTo(digest)
          }
      }

      @Test
      fun namingRecordsAreHymnalKaiOnly() {
          for ((variant, family) in listOf("sc" to "HymnalKai SC", "tc" to "HymnalKai TC")) {
              val names = rows.filter { it[0] == "name" && it[1] == variant }
              assertThat(names.single { it[2] == "1" }[3]).isEqualTo(family)
              names.forEach { record ->
                  RESERVED.forEach { word -> assertWithMessage(record.joinToString(" ")).that(record[3].lowercase()).doesNotContain(word) }
              }
          }
      }

      @Test
      fun oflLicenceShipsWithTheApp() {
          val text = File(assets, "licenses/OFL-HymnalKai.txt").readText()
          assertThat(text).contains("SIL OPEN FONT LICENSE Version 1.1")
          assertThat(text).contains("HymnalKai")
          assertThat(text).contains("LxgwWenKai")
          assertThat(text).contains("LxgwWenkaiTC")
      }

      private fun sha256(file: File): String =
          MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }

      private companion object {
          val RESERVED = listOf("lxgw", "霞鹜", "霞鶩", "落霞孤鹜", "落霞孤鶩")
      }
  }
  ```

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.reading.FontSubsetTest' --console=plain`
  Expected: 5 個測試全部失敗（`FileNotFoundException`：manifest、字型、授權檔都還不存在）。

- [ ] **Step 2：Commit（只有測試，先標明會失敗）**

  ```bash
  git add hymnchtv/src/test/java/org/cog/hymnchtv/reading/FontSubsetTest.kt
  git commit -m "test: add HymnalKai font coverage and naming checks (red until F2)" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

  這個 commit 之後 `testDebugUnitTest` 會失敗，F2 會讓它恢復。lane F 合併前一定要做完 F2。

---

### Task F2：產生 HymnalKai SC／TC 子集與授權檔

**Files:**
- Create: `tools/gen_font_subset.py`
- Generate (commit): `hymnchtv/src/main/res/font/hymnal_kai_sc.ttf`、`hymnal_kai_tc.ttf`、`tools/font_subset_manifest.txt`
- Create: `hymnchtv/src/main/assets/licenses/OFL-HymnalKai.txt`

**前置:** F1。

- [ ] **Step 1：建立 fontTools venv（git 忽略）**

  ```bash
  python3 -m venv .venv-tools
  .venv-tools/bin/pip install --quiet fonttools==4.66.1
  .venv-tools/bin/python -c "import fontTools; print(fontTools.version)"
  ```

  Expected: 印出 `4.66.1`。產生工具會檢查版本，版本不同就拒絕執行（不同版本的輸出位元組可能不同）。

- [ ] **Step 2：取得字型原檔（git 忽略）並核對 SHA-256**

  ```bash
  mkdir -p tools/fonts-src
  SP=/private/tmp/claude-501/-Users-hitobias-orca-hymnchtv/010ed055-844a-4fb6-b27b-6d8859efcf3e/scratchpad
  cp "$SP/LXGWWenKai-Regular.ttf" "$SP/LXGWWenKaiTC-Regular.ttf" tools/fonts-src/ 2>/dev/null || {
    curl -fL -o tools/fonts-src/LXGWWenKai-Regular.ttf https://github.com/lxgw/LxgwWenKai/releases/download/v1.522/LXGWWenKai-Regular.ttf
    curl -fL -o tools/fonts-src/LXGWWenKaiTC-Regular.ttf https://github.com/lxgw/LxgwWenkaiTC/releases/download/v1.522/LXGWWenKaiTC-Regular.ttf
  }
  shasum -a 256 tools/fonts-src/*.ttf
  ```

  Expected:

  ```
  39ad71264b588165b469e35e6afb162a378dacd1f95348160240ba9038ac3009  tools/fonts-src/LXGWWenKai-Regular.ttf
  b1a0795862c1415bf3f393ea50b2a4ea6275012cf5bad3f94feeb1222f555731  tools/fonts-src/LXGWWenKaiTC-Regular.ttf
  ```

  暫存目錄裡的檔案是 2026-10-02 下載的 v1.522（name 表的版本字串是 `Version 1.522; March 17, 2026`）。如果暫存目錄已被清掉而且下載網址 404，或 SHA-256 不符，**停下來回報**，不要換成別的版本。

- [ ] **Step 3：新增產生工具** `tools/gen_font_subset.py`

  這份程式在 2026-10-02 已用相同的字型、fontTools 版本、F0 修正後的歌詞與 Task 0 的新字串實際跑過：輸出 1.49 MB／2.10 MB，缺字 0，連跑兩次的字型與 manifest 的 SHA-256 完全相同。

  ```python
  #!/usr/bin/env python3
  """Subset LXGW WenKai / LXGW WenKai TC into the lyrics fonts HymnalKai SC / TC (plan A2).

  Usage: .venv-tools/bin/python tools/gen_font_subset.py
  Needs fontTools == FONTTOOLS_VERSION (pinned: output must be byte-identical) and the two source fonts in
  tools/fonts-src/ (git-ignored; URLs and SHA-256 in SOURCES).

  Each font must cover every character of its lyrics (SC: Simplified; TC: both Traditional variants) and of every
  strings file; there are no exceptions. The manifest records each font's SHA-256, its naming records and the code
  points its cmap covers, so FontSubsetTest can check coverage without parsing TrueType.

  Licence: SIL OFL 1.1. "LXGW", 霞鹜, 霞鶩, 落霞孤鹜 and 落霞孤鶩 are Reserved Font Names, and a subset is a
  Modified Version, so every naming record is replaced with "HymnalKai". Copyright and licence records are kept.
  """
  import hashlib
  import pathlib
  import sys

  ROOT = pathlib.Path(__file__).resolve().parent.parent
  ASSETS = ROOT / "hymnchtv/src/main/assets"
  RES = ROOT / "hymnchtv/src/main/res"
  FONT_DIR = RES / "font"
  SRC_DIR = ROOT / "tools/fonts-src"
  MANIFEST = ROOT / "tools/font_subset_manifest.txt"
  FONTTOOLS_VERSION = "4.66.1"

  SOURCES = {
      "sc": ("LXGWWenKai-Regular.ttf", "39ad71264b588165b469e35e6afb162a378dacd1f95348160240ba9038ac3009",
             "https://github.com/lxgw/LxgwWenKai/releases/download/v1.522/LXGWWenKai-Regular.ttf"),
      "tc": ("LXGWWenKaiTC-Regular.ttf", "b1a0795862c1415bf3f393ea50b2a4ea6275012cf5bad3f94feeb1222f555731",
             "https://github.com/lxgw/LxgwWenkaiTC/releases/download/v1.522/LXGWWenKaiTC-Regular.ttf"),
  }
  OUTPUTS = {  # variant: (file in res/font, family name, PostScript family)
      "sc": ("hymnal_kai_sc.ttf", "HymnalKai SC", "HymnalKaiSC"),
      "tc": ("hymnal_kai_tc.ttf", "HymnalKai TC", "HymnalKaiTC"),
  }
  LYRIC_DIRS = {"sc": ("lyrics_*_text",), "tc": ("lyrics_*_text_hant_tw", "lyrics_*_text_hant_hk")}
  RESERVED = ("lxgw", "霞鹜", "霞鶩", "落霞孤鹜", "落霞孤鶩")
  NAMING_IDS = {1, 2, 3, 4, 6, 16, 17, 18, 21, 22, 25}


  def sha256(path):
      return hashlib.sha256(path.read_bytes()).hexdigest()


  def wanted(cp):
      """Same rule as FontSubsetTest: everything except C0 controls, DEL and the BOM."""
      return cp >= 0x20 and cp != 0x7F and cp != 0xFEFF


  def codepoints(paths):
      found = set()
      for p in paths:
          found.update(cp for cp in map(ord, p.read_text(encoding="utf-8")) if wanted(cp))
      return found


  def lyric_files(variant):
      return sorted(f for pattern in LYRIC_DIRS[variant] for d in ASSETS.glob(pattern) if d.is_dir() for f in d.glob("*.txt"))


  def string_files():
      return sorted(RES.glob("values*/strings*.xml"))


  def ranges(cps):
      """Sorted code points as 'XXXX-YYYY' / 'XXXX' runs, space separated."""
      out, items = [], sorted(cps)
      i = 0
      while i < len(items):
          j = i
          while j + 1 < len(items) and items[j + 1] == items[j] + 1:
              j += 1
          out.append("%04X" % items[i] if i == j else "%04X-%04X" % (items[i], items[j]))
          i = j + 1
      return " ".join(out)


  def check_sources():
      for name, digest, url in SOURCES.values():
          path = SRC_DIR / name
          if not path.is_file():
              sys.exit(f"missing {path.relative_to(ROOT)}; download it from {url}")
          if sha256(path) != digest:
              sys.exit(f"{name}: SHA-256 mismatch (expected {digest}); wrong release?")


  def rename(font, family, ps_family):
      table = font["name"]
      table.names = [r for r in table.names if r.nameID not in NAMING_IDS]
      revision = "%.3f" % font["head"].fontRevision
      values = {1: family, 2: "Regular", 3: f"{revision};{ps_family}-Regular", 4: f"{family} Regular", 6: f"{ps_family}-Regular"}
      for name_id, text in values.items():
          table.setName(text, name_id, 3, 1, 0x409)
      naming = sorted((r.nameID, r.toUnicode()) for r in table.names if r.nameID in NAMING_IDS)
      for name_id, text in naming:
          if any(word in text.lower() for word in RESERVED):
              sys.exit(f"name ID {name_id} still contains a Reserved Font Name: {text}")
      return naming


  def build(variant, wanted_codepoints):
      from fontTools import subset

      opts = subset.Options()
      opts.name_IDs = ["*"]
      opts.name_languages = ["*"]
      opts.name_legacy = True
      opts.notdef_outline = True
      opts.recalc_timestamp = False
      opts.drop_tables += ["DSIG"]
      font = subset.load_font(str(SRC_DIR / SOURCES[variant][0]), opts)
      subsetter = subset.Subsetter(opts)
      subsetter.populate(unicodes=sorted(wanted_codepoints))
      subsetter.subset(font)
      out_name, family, ps_family = OUTPUTS[variant]
      naming = rename(font, family, ps_family)
      out = FONT_DIR / out_name
      subset.save_font(font, str(out), opts)
      return out, set(font.getBestCmap()), naming


  def main():
      import fontTools
      if fontTools.version != FONTTOOLS_VERSION:
          sys.exit(f"fontTools {fontTools.version} found; pin {FONTTOOLS_VERSION} so the output stays byte-identical")
      check_sources()
      FONT_DIR.mkdir(exist_ok=True)
      ui = codepoints(string_files()) | set(range(0x20, 0x7F))
      rows, problems = [], []
      for variant in ("sc", "tc"):
          required = codepoints(lyric_files(variant)) | ui
          out, cmap, naming = build(variant, required)
          missing = sorted(required - cmap)
          if missing:
              problems.append(f"{variant}: not in the source font: " + " ".join(f"U+{cp:04X}({chr(cp)})" for cp in missing))
          rows.append(f"font\t{variant}\t{out.relative_to(ROOT).as_posix()}\t{sha256(out)}")
          rows += [f"name\t{variant}\t{name_id}\t{text}" for name_id, text in naming]
          rows.append(f"covers\t{variant}\t{ranges(cmap)}")
          print(f"{out.name}: {out.stat().st_size / 1e6:.2f} MB, {len(required)} required code points, {len(missing)} missing")
      if problems:
          sys.exit("\n".join(problems) + "\nFix the lyrics or strings (usually a typo); there is no exception list.")
      MANIFEST.write_text(f"# generated by tools/gen_font_subset.py; fontTools {FONTTOOLS_VERSION}\n" + "\n".join(rows) + "\n",
                          encoding="utf-8")


  if __name__ == "__main__":
      main()
  ```

- [ ] **Step 4：產生字型並確認可重現**

  ```bash
  chmod +x tools/gen_font_subset.py
  .venv-tools/bin/python tools/gen_font_subset.py
  shasum -a 256 hymnchtv/src/main/res/font/*.ttf tools/font_subset_manifest.txt
  .venv-tools/bin/python tools/gen_font_subset.py > /dev/null 2>&1
  shasum -a 256 hymnchtv/src/main/res/font/*.ttf tools/font_subset_manifest.txt
  ```

  Expected:
  - 輸出包含 `hymnal_kai_sc.ttf: 1.49 MB, 3224 required code points, 0 missing` 和 `hymnal_kai_tc.ttf: 2.10 MB, 3249 required code points, 0 missing`（若 Task 0 之後字串有變，數字可能差幾個；`0 missing` 不能變）。
  - fontTools 會印一行 `meta NOT subset; don't know how to subset; dropped`，這是正常的（`meta` 表只是語言標記）。
  - 兩次 `shasum` 結果相同。
  - 如果工具以 `not in the source font: ...` 結束：表示歌詞或字串出現了字型沒有的字。**停下來**，把清單交給使用者確認（通常是錯字）。不能加例外。

- [ ] **Step 5：新增授權檔** `hymnchtv/src/main/assets/licenses/OFL-HymnalKai.txt`

  ```bash
  mkdir -p hymnchtv/src/main/assets/licenses
  curl -fsL https://raw.githubusercontent.com/lxgw/LxgwWenKai/v1.522/OFL.txt -o /tmp/ofl-sc.txt
  curl -fsL https://raw.githubusercontent.com/lxgw/LxgwWenkaiTC/v1.522/OFL.txt -o /tmp/ofl-tc.txt
  head -3 /tmp/ofl-sc.txt /tmp/ofl-tc.txt
  {
    printf '%s\n' \
      'HymnalKai SC and HymnalKai TC, the lyrics fonts of this app, are Modified Versions' \
      '(character subsets, renamed) of LXGW WenKai and LXGW WenKai TC v1.522.' \
      'Sources: https://github.com/lxgw/LxgwWenKai and https://github.com/lxgw/LxgwWenkaiTC' \
      'They are distributed under the SIL Open Font License 1.1, reproduced below.' \
      '' '===== LXGW WenKai (https://github.com/lxgw/LxgwWenKai) =====' ''
    cat /tmp/ofl-sc.txt
    printf '%s\n' '' '===== LXGW WenKai TC (https://github.com/lxgw/LxgwWenkaiTC) =====' ''
    cat /tmp/ofl-tc.txt
  } > hymnchtv/src/main/assets/licenses/OFL-HymnalKai.txt
  grep -c "SIL OPEN FONT LICENSE Version 1.1" hymnchtv/src/main/assets/licenses/OFL-HymnalKai.txt
  ```

  Expected: `head` 顯示兩份檔案都以 `Copyright` 開頭；最後的 `grep -c` 印出 `2`。如果 `v1.522` 標籤下沒有 `OFL.txt`（404），改用 `main` 分支的網址並在回報中註明。

- [ ] **Step 6：執行測試**

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.reading.FontSubsetTest' --console=plain`
  Expected: 5 個測試全部通過。

- [ ] **Step 7：驗證保護機制（每項做完就還原，不要 commit）**

  1. 在 `hymnchtv/src/main/assets/lyrics_db_text/db1.txt` 結尾加上 `𠀀`（U+20000）：`scCoversSimplifiedLyricsAndAllStrings` 要失敗，訊息含 `U+20000`。還原：`git checkout -- hymnchtv/src/main/assets/lyrics_db_text/db1.txt`。
  2. 執行 `python3 -c "import pathlib;p=pathlib.Path('hymnchtv/src/main/res/values/strings_reading.xml');p.write_text(p.read_text(encoding='utf-8').replace('Holy, holy, holy','Holy, holy, holy\ue000'),encoding='utf-8')"`，在字串裡加入私用區字元 U+E000：兩個 `...AndAllStrings` 測試都要失敗。還原：`git checkout -- hymnchtv/src/main/res/values/strings_reading.xml`。
  3. 把 `tools/font_subset_manifest.txt` 第一個 `font` 行的雜湊改掉一個字：`committedFontsAreTheOnesTheManifestDescribes` 要失敗。還原：重跑 `.venv-tools/bin/python tools/gen_font_subset.py`。

- [ ] **Step 8：Commit**

  ```bash
  git add tools/gen_font_subset.py tools/font_subset_manifest.txt \
          hymnchtv/src/main/res/font/hymnal_kai_sc.ttf hymnchtv/src/main/res/font/hymnal_kai_tc.ttf \
          hymnchtv/src/main/assets/licenses/OFL-HymnalKai.txt
  git status --short tools hymnchtv/src/main/res/font   # tools/fonts-src 與 .venv-tools 不能出現
  git commit -m "feat: subset LXGW WenKai into the HymnalKai SC/TC lyrics fonts" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

### Task F3：LyricsTypefaces 與 About 頁的字型致謝

**Files:**
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/reading/LyricsTypefaces.kt`
- Modify: `hymnchtv/src/main/java/org/cog/hymnchtv/About.java`
- Modify: `hymnchtv/src/main/res/layout/about.xml`

**前置:** F2。

- [ ] **Step 1：新增** `LyricsTypefaces.kt`

  ```kotlin
  package org.cog.hymnchtv.reading

  import android.content.Context
  import android.content.res.Resources
  import android.graphics.Typeface
  import androidx.core.content.res.ResourcesCompat
  import org.cog.hymnchtv.R
  import timber.log.Timber
  import java.util.concurrent.Executors

  /** HymnalKai SC/TC for the lyrics views (plan A2); loaded once, off the main thread when possible. */
  object LyricsTypefaces {
      private var simplified: Typeface? = null
      private var traditional: Typeface? = null
      private val loader = Executors.newSingleThreadExecutor { r -> Thread(r, "lyrics-font").apply { isDaemon = true } }

      /** Starts decoding both fonts in the background so the first lyrics page does not wait for 3.5 MB of TTF. */
      @JvmStatic
      fun preload(context: Context) {
          val app = context.applicationContext
          loader.execute {
              get(app, false)
              get(app, true)
          }
      }

      /** The font for one script, or null if it cannot be loaded (the caller keeps the system font). */
      @JvmStatic
      @Synchronized
      fun get(context: Context, traditionalScript: Boolean): Typeface? {
          val cached = if (traditionalScript) traditional else simplified
          if (cached != null) return cached
          val res = if (traditionalScript) R.font.hymnal_kai_tc else R.font.hymnal_kai_sc
          val loaded = try {
              ResourcesCompat.getFont(context.applicationContext, res)
          } catch (e: Resources.NotFoundException) {
              Timber.e(e, "Lyrics font %s could not be loaded", if (traditionalScript) "TC" else "SC")
              null
          }
          if (traditionalScript) traditional = loaded else simplified = loaded
          return loaded
      }
  }
  ```

- [ ] **Step 2：`about.xml` 加入致謝文字**

  在 `android:id="@+id/copyRight"` 那個 `TextView` 的結束（`android:text="@string/copyright" />`）之後插入：

  ```xml

                  <TextView
                      android:id="@+id/font_credit"
                      android:layout_width="fill_parent"
                      android:layout_height="wrap_content"
                      android:gravity="center_horizontal"
                      android:paddingStart="10dp"
                      android:paddingEnd="10dp"
                      android:paddingBottom="10dp"
                      android:text="@string/about_font_credit"
                      android:textColor="?android:attr/colorAccent"
                      android:textSize="14sp" />
  ```

- [ ] **Step 3：`About.java` 顯示授權全文**

  1. 在 `hymnchtvHelp.setOnClickListener(this);` 的下一行加入：

     ```java
             findViewById(R.id.font_credit).setOnClickListener(this);
     ```

  2. 在 `onClick` 裡，`else if (id == R.id.hymnchtv_help || id == R.id.hymnchtv_link) { ... }` 這個區塊之後、最後的 `else { finish(); }` 之前，加入：

     ```java
             else if (id == R.id.font_credit) {
                 showFontLicense();
             }
     ```

  3. 在 `onLongClick` 方法之前加入：

     ```java
         /**
          * Show the SIL OFL text that must ship with the bundled HymnalKai fonts (plan A2).
          */
         private void showFontLicense() {
             String licence;
             try (InputStream in = getAssets().open("licenses/OFL-HymnalKai.txt")) {
                 licence = new String(IOUtils.toByteArray(in), StandardCharsets.UTF_8);
             }
             catch (IOException e) {
                 Timber.e(e, "Font licence asset missing");
                 HymnsApp.showToastMessage(R.string.font_license_title);
                 return;
             }
             new AlertDialog.Builder(this)
                     .setTitle(R.string.font_license_title)
                     .setMessage(licence)
                     .setPositiveButton(R.string.ok, null)
                     .show();
         }
     ```

  4. 補上缺少的 import（先 `grep -n "^import" hymnchtv/src/main/java/org/cog/hymnchtv/About.java` 看已有哪些）：

     ```java
     import androidx.appcompat.app.AlertDialog;
     import java.io.IOException;
     import java.io.InputStream;
     import java.nio.charset.StandardCharsets;
     import org.apache.commons.io.IOUtils;
     import timber.log.Timber;
     ```

     `commons-io` 已經是依賴（`build.gradle`）。`HymnsApp.showToastMessage(int)` 已存在（`ZoomTextView` 有用到）。

- [ ] **Step 4：建置**

  ```bash
  ./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug --console=plain
  unzip -l hymnchtv/build/outputs/apk/debug/*.apk | grep -E "res/font|licenses"
  ```

  Expected: `BUILD SUCCESSFUL`；APK 內有兩個字型檔與 `assets/licenses/OFL-HymnalKai.txt`。（字型的資源路徑可能被 aapt2 縮短，只要看到兩個約 1.5／2.1 MB 的項目即可。）

- [ ] **Step 5：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/reading/LyricsTypefaces.kt \
          hymnchtv/src/main/java/org/cog/hymnchtv/About.java hymnchtv/src/main/res/layout/about.xml
  git commit -m "feat: load lyrics fonts and credit the OFL font in About" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

  Lane F 完成，回報給協調者合併。

---

## 階段 2 · Lane S：閱讀設定畫面與背景挑選

Lane S 的子代理在 worktree `../hymnchtv-a2-s` 工作（從合併了 P、B、F 的 `feat/reading-settings` 開出）。

### Task S1：ReadingSettingsActivity、ReadingSettingsFragment、reading_preferences.xml

**Files:**
- Create: `hymnchtv/src/main/res/xml/reading_preferences.xml`
- Create: `hymnchtv/src/main/res/layout/reading_settings.xml`
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/reading/ReadingSettingsActivity.kt`
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/reading/ReadingSettingsFragment.kt`
- Modify: `hymnchtv/src/main/AndroidManifest.xml`
- Test: `hymnchtv/src/test/java/org/cog/hymnchtv/reading/ReadingPreferencesXmlTest.kt`

**前置:** 階段 1 全部合併。`ReadingSettingsFragment` 會引用 S2 才建立的 `BackgroundPickerActivity`，所以 S1 先不接背景挑選的點擊（Step 6 的程式碼已標明），S2 再補上。

- [ ] **Step 1：寫會失敗的測試** `ReadingPreferencesXmlTest.kt`

  ```kotlin
  package org.cog.hymnchtv.reading

  import com.google.common.truth.Truth.assertThat
  import org.cog.hymnchtv.lyrics.HantVariant
  import org.cog.hymnchtv.lyrics.LyricsLang
  import org.cog.hymnchtv.lyrics.LyricsLanguagePolicy
  import org.cog.hymnchtv.reading.background.BackgroundSlot
  import org.cog.hymnchtv.reading.background.PhotoBackground
  import org.junit.Test
  import org.w3c.dom.Document
  import org.w3c.dom.Element
  import java.io.File
  import javax.xml.parsers.DocumentBuilderFactory

  /** Keeps reading_preferences.xml and reading_arrays.xml in step with the keys, enums and defaults the code reads. */
  class ReadingPreferencesXmlTest {
      private val res = File(checkNotNull(System.getProperty("hymnchtv.resDir")) { "hymnchtv.resDir not set" })

      private fun parse(path: String): Document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(res, path))

      private fun prefs(): Map<String, Element> {
          val nodes = parse("xml/reading_preferences.xml").getElementsByTagName("*")
          return (0 until nodes.length).map { nodes.item(it) as Element }
              .filter { it.hasAttribute("app:key") }
              .associateBy { it.getAttribute("app:key") }
      }

      private fun array(name: String): List<String> {
          val arrays = parse("values/reading_arrays.xml").getElementsByTagName("string-array")
          val node = (0 until arrays.length).map { arrays.item(it) as Element }.single { it.getAttribute("name") == name }
          val items = node.getElementsByTagName("item")
          return (0 until items.length).map { items.item(it).textContent.trim() }
      }

      @Test
      fun keysAreTheOnesTheCodeReads() {
          assertThat(prefs().keys).containsExactly(
              ReadingPrefKeys.DISPLAY_MODE,
              LyricsLanguagePolicy.PREF_LYRICS_DEFAULT,
              "ConversionType", // ContentView.PREF_CONVERSION_TYPE (Java constant, not visible to JVM tests)
              ReadingPrefKeys.LYRICS_FONT_SIZE,
              ReadingPrefKeys.LYRICS_FONT,
              BackgroundSlot.MAIN.prefKey,
              BackgroundSlot.LYRICS.prefKey,
              PhotoBackground.PREF_DIM,
              PhotoBackground.PREF_BLUR,
              ReadingPrefKeys.PAGE_ANIMATION,
              ReadingPrefKeys.MENU_SHOW,
              ReadingPrefKeys.KEEP_SCREEN_ON,
          )
      }

      @Test
      fun defaultsMatchTheCode() {
          val p = prefs()
          fun attr(key: String, name: String) = p.getValue(key).getAttribute(name)
          fun default(key: String) = attr(key, "app:defaultValue")
          assertThat(default(ReadingPrefKeys.DISPLAY_MODE)).isEqualTo(DisplayMode.fromPref(null).name)
          assertThat(default(LyricsLanguagePolicy.PREF_LYRICS_DEFAULT)).isEqualTo(LyricsLang.fromPref(null).name)
          assertThat(default("ConversionType")).isEqualTo(HantVariant.TW.prefValue)
          assertThat(default(ReadingPrefKeys.LYRICS_FONT_SIZE)).isEqualTo(LyricsFontSize.fromPref(null).name)
          assertThat(default(ReadingPrefKeys.LYRICS_FONT)).isEqualTo(LyricsFont.fromPref(null).name)
          listOf(ReadingPrefKeys.PAGE_ANIMATION, ReadingPrefKeys.MENU_SHOW, ReadingPrefKeys.KEEP_SCREEN_ON).forEach {
              assertThat(default(it)).isEqualTo("true")
          }
          assertThat(default(PhotoBackground.PREF_DIM)).isEqualTo(PhotoBackground.DIM_DEFAULT.toString())
          assertThat(attr(PhotoBackground.PREF_DIM, "app:min")).isEqualTo(PhotoBackground.DIM_MIN.toString())
          assertThat(attr(PhotoBackground.PREF_DIM, "android:max")).isEqualTo(PhotoBackground.DIM_MAX.toString())
          assertThat(default(PhotoBackground.PREF_BLUR)).isEqualTo(PhotoBackground.BLUR_DEFAULT.toString())
          assertThat(attr(PhotoBackground.PREF_BLUR, "android:max")).isEqualTo(PhotoBackground.BLUR_MAX.toString())
      }

      @Test
      fun listValuesAreTheEnumNames() {
          assertThat(array("display_mode_values")).containsExactlyElementsIn(DisplayMode.entries.map { it.name }).inOrder()
          assertThat(array("font_size_values")).containsExactlyElementsIn(LyricsFontSize.entries.map { it.name }).inOrder()
          assertThat(array("lyrics_font_values")).containsExactlyElementsIn(LyricsFont.entries.map { it.name }).inOrder()
          assertThat(array("lyrics_lang_values")).containsExactlyElementsIn(LyricsLang.entries.map { it.name }).inOrder()
          assertThat(array("conversion_values")).containsExactlyElementsIn(HantVariant.entries.map { it.prefValue }).inOrder()
      }

      @Test
      fun everyListHasOneLabelPerValue() {
          listOf("display_mode", "font_size", "lyrics_font", "lyrics_lang", "conversion").forEach {
              assertThat(array("${it}_entries")).hasSize(array("${it}_values").size)
          }
      }
  }
  ```

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.reading.ReadingPreferencesXmlTest' --console=plain`
  Expected: `keysAreTheOnesTheCodeReads` 與 `defaultsMatchTheCode` 失敗（`FileNotFoundException: .../xml/reading_preferences.xml`）；兩個陣列測試通過（陣列在 Task 0 已建立）。

- [ ] **Step 2：新增** `hymnchtv/src/main/res/xml/reading_preferences.xml`

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <!-- Reading settings (plan A2). Keys and defaults are checked by ReadingPreferencesXmlTest. -->
  <PreferenceScreen xmlns:android="http://schemas.android.com/apk/res/android"
      xmlns:app="http://schemas.android.com/apk/res-auto">

      <PreferenceCategory
          app:iconSpaceReserved="false"
          app:title="@string/pref_cat_lyrics">

          <ListPreference
              app:defaultValue="SCORE_AND_LYRICS"
              app:entries="@array/display_mode_entries"
              app:entryValues="@array/display_mode_values"
              app:iconSpaceReserved="false"
              app:key="DisplayMode"
              app:title="@string/pref_display_mode"
              app:useSimpleSummaryProvider="true" />

          <ListPreference
              app:defaultValue="FOLLOW_UI"
              app:entries="@array/lyrics_lang_entries"
              app:entryValues="@array/lyrics_lang_values"
              app:iconSpaceReserved="false"
              app:key="LyricsDefaultLang"
              app:title="@string/lyrics_default_title"
              app:useSimpleSummaryProvider="true" />

          <!-- Hidden while LyricsLanguagePolicy.HK_VARIANT_ENABLED is false -->
          <ListPreference
              app:defaultValue="S2TW"
              app:entries="@array/conversion_entries"
              app:entryValues="@array/conversion_values"
              app:iconSpaceReserved="false"
              app:key="ConversionType"
              app:title="@string/STD"
              app:useSimpleSummaryProvider="true" />

          <ListPreference
              app:defaultValue="MEDIUM"
              app:entries="@array/font_size_entries"
              app:entryValues="@array/font_size_values"
              app:iconSpaceReserved="false"
              app:key="LyricsFontSize"
              app:title="@string/pref_font_size"
              app:useSimpleSummaryProvider="true" />

          <ListPreference
              app:defaultValue="KAI"
              app:entries="@array/lyrics_font_entries"
              app:entryValues="@array/lyrics_font_values"
              app:iconSpaceReserved="false"
              app:key="LyricsFont"
              app:title="@string/pref_lyrics_font"
              app:useSimpleSummaryProvider="true" />
      </PreferenceCategory>

      <PreferenceCategory
          app:iconSpaceReserved="false"
          app:title="@string/pref_cat_background">

          <!-- Opens BackgroundPickerActivity, which writes the pref itself -->
          <Preference
              app:iconSpaceReserved="false"
              app:key="MainBackground"
              app:persistent="false"
              app:title="@string/pref_main_background" />

          <Preference
              app:iconSpaceReserved="false"
              app:key="LyricsBackground"
              app:persistent="false"
              app:title="@string/pref_lyrics_background" />

          <SeekBarPreference
              android:max="80"
              app:defaultValue="40"
              app:iconSpaceReserved="false"
              app:key="PhotoDim"
              app:min="20"
              app:showSeekBarValue="true"
              app:title="@string/pref_photo_dim" />

          <SeekBarPreference
              android:max="25"
              app:defaultValue="0"
              app:iconSpaceReserved="false"
              app:key="PhotoBlur"
              app:min="0"
              app:showSeekBarValue="true"
              app:title="@string/pref_photo_blur" />
      </PreferenceCategory>

      <PreferenceCategory
          app:iconSpaceReserved="false"
          app:title="@string/pref_cat_screen">

          <SwitchPreferenceCompat
              app:defaultValue="true"
              app:iconSpaceReserved="false"
              app:key="PageAnimation"
              app:title="@string/pref_page_animation" />

          <SwitchPreferenceCompat
              app:defaultValue="true"
              app:iconSpaceReserved="false"
              app:key="MenuShow"
              app:title="@string/pref_menu_show" />

          <SwitchPreferenceCompat
              app:defaultValue="true"
              app:iconSpaceReserved="false"
              app:key="KeepScreenOn"
              app:title="@string/pref_keep_screen_on" />
      </PreferenceCategory>
  </PreferenceScreen>
  ```

- [ ] **Step 3：執行測試**

  Run: 同 Step 1。
  Expected: 4 個測試全部通過。

- [ ] **Step 4：新增** `hymnchtv/src/main/res/layout/reading_settings.xml`

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
      android:id="@+id/readingSettingsContainer"
      android:layout_width="match_parent"
      android:layout_height="match_parent" />
  ```

- [ ] **Step 5：新增** `ReadingSettingsActivity.kt`

  ```kotlin
  package org.cog.hymnchtv.reading

  import android.content.Intent
  import android.content.SharedPreferences
  import android.os.Bundle
  import org.cog.hymnchtv.BaseActivity
  import org.cog.hymnchtv.ContentView
  import org.cog.hymnchtv.MainActivity
  import org.cog.hymnchtv.R

  /**
   * Hosts ReadingSettingsFragment (plan A2). Settings apply immediately; on finish the result carries
   * ContentView.EXTR_KEY_HAS_CHANGES so an open lyrics page can rebuild itself.
   */
  class ReadingSettingsActivity : BaseActivity() {
      private lateinit var prefs: SharedPreferences
      private var changed = false
      private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> changed = true }

      override fun onCreate(savedInstanceState: Bundle?) {
          super.onCreate(savedInstanceState)
          setContentView(R.layout.reading_settings)
          setTitle(R.string.reading_settings)
          changed = savedInstanceState?.getBoolean(STATE_CHANGED) ?: false
          prefs = getSharedPreferences(MainActivity.PREF_SETTINGS, MODE_PRIVATE)
          // Registered before the fragment exists so its one-off self-heal also counts as a change
          prefs.registerOnSharedPreferenceChangeListener(listener)
          if (savedInstanceState == null) {
              supportFragmentManager.beginTransaction()
                  .replace(R.id.readingSettingsContainer, ReadingSettingsFragment())
                  .commit()
          }
      }

      override fun onSaveInstanceState(outState: Bundle) {
          super.onSaveInstanceState(outState)
          outState.putBoolean(STATE_CHANGED, changed)
      }

      override fun onDestroy() {
          prefs.unregisterOnSharedPreferenceChangeListener(listener)
          super.onDestroy()
      }

      override fun finish() {
          setResult(RESULT_OK, Intent().putExtra(ContentView.EXTR_KEY_HAS_CHANGES, changed))
          super.finish()
      }

      private companion object {
          const val STATE_CHANGED = "state_changed"
      }
  }
  ```

  如果 `BaseActivity.onCreate` 的可見度讓 Kotlin 編譯失敗（例如它是 `public` 但宣告了 `final`），停下來回報。

- [ ] **Step 6：新增** `ReadingSettingsFragment.kt`

  ```kotlin
  package org.cog.hymnchtv.reading

  import android.content.SharedPreferences
  import android.os.Build
  import android.os.Bundle
  import androidx.activity.result.contract.ActivityResultContracts
  import androidx.preference.ListPreference
  import androidx.preference.Preference
  import androidx.preference.PreferenceFragmentCompat
  import androidx.preference.SeekBarPreference
  import org.cog.hymnchtv.ContentView
  import org.cog.hymnchtv.MainActivity
  import org.cog.hymnchtv.R
  import org.cog.hymnchtv.lyrics.LyricsLanguagePolicy
  import org.cog.hymnchtv.reading.background.BackgroundDrawables
  import org.cog.hymnchtv.reading.background.BackgroundPrefs
  import org.cog.hymnchtv.reading.background.BackgroundSlot
  import org.cog.hymnchtv.reading.background.PhotoBackground

  /** The reading settings (plan A2); sub-project C can host this fragment in its settings page unchanged. */
  class ReadingSettingsFragment : PreferenceFragmentCompat() {
      private val pickBackground = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
          refreshBackgroundSummaries()
      }

      override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
          preferenceManager.sharedPreferencesName = MainActivity.PREF_SETTINGS
          healConversionType()
          setPreferencesFromResource(R.xml.reading_preferences, rootKey)

          findPreference<ListPreference>(ContentView.PREF_CONVERSION_TYPE)?.isVisible = LyricsLanguagePolicy.HK_VARIANT_ENABLED

          findPreference<ListPreference>(ReadingPrefKeys.LYRICS_FONT_SIZE)?.setOnPreferenceChangeListener { _, newValue ->
              prefs()?.let { ReadingPrefs.resetLyricsScale(it.edit(), LyricsFontSize.fromPref(newValue as? String)).apply() }
              true
          }

          if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
              findPreference<SeekBarPreference>(PhotoBackground.PREF_BLUR)?.apply {
                  isEnabled = false
                  summary = getString(R.string.pref_photo_blur_unsupported)
              }
          }

          for (slot in BackgroundSlot.entries) {
              findPreference<Preference>(slot.prefKey)?.setOnPreferenceClickListener {
                  // S2: pickBackground.launch(BackgroundPickerActivity.intent(requireContext(), slot))
                  true
              }
          }
      }

      override fun onResume() {
          super.onResume()
          refreshBackgroundSummaries()
      }

      private fun prefs(): SharedPreferences? = preferenceManager.sharedPreferences

      private fun refreshBackgroundSummaries() {
          val sp = prefs() ?: return
          for (slot in BackgroundSlot.entries) {
              findPreference<Preference>(slot.prefKey)?.summary = getString(BackgroundDrawables.nameRes(BackgroundPrefs.resolve(sp, slot)))
          }
      }

      /** Same self-heal as the old ChineseS2TSelection (plan A.1.5): rewrite an invalid ConversionType once. */
      private fun healConversionType() {
          val sp = prefs() ?: return
          val raw = runCatching { sp.getString(ContentView.PREF_CONVERSION_TYPE, null) }.getOrNull()
          if (raw != null && !LyricsLanguagePolicy.isCanonical(raw)) {
              val variant = LyricsLanguagePolicy.parseVariant(raw, resources.configuration.locales[0])
              sp.edit().putString(ContentView.PREF_CONVERSION_TYPE, variant.prefValue).apply()
          }
      }
  }
  ```

  `pickBackground` 在 S1 還沒有被呼叫，編譯器可能警告「unused」，S2 會用到。

- [ ] **Step 7：`AndroidManifest.xml` 註冊 Activity**

  在 `.utils.ChineseS2TSelection` 那個 `<activity ... />` 區塊之後加入：

  ```xml

          <activity
              android:name=".reading.ReadingSettingsActivity"
              android:label="@string/reading_settings" />
  ```

- [ ] **Step 8：建置**

  Run: `./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug --console=plain`
  Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 9：Commit**

  ```bash
  git add hymnchtv/src/main/res/xml/reading_preferences.xml hymnchtv/src/main/res/layout/reading_settings.xml \
          hymnchtv/src/main/java/org/cog/hymnchtv/reading/ReadingSettingsActivity.kt \
          hymnchtv/src/main/java/org/cog/hymnchtv/reading/ReadingSettingsFragment.kt \
          hymnchtv/src/main/AndroidManifest.xml \
          hymnchtv/src/test/java/org/cog/hymnchtv/reading/ReadingPreferencesXmlTest.kt
  git commit -m "feat: add reading settings screen" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

### Task S2：背景挑選畫面

**Files:**
- Create: `hymnchtv/src/main/res/layout/background_picker.xml`、`background_picker_item.xml`
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/reading/BackgroundPickerActivity.kt`
- Modify: `hymnchtv/src/main/java/org/cog/hymnchtv/reading/ReadingSettingsFragment.kt`
- Modify: `hymnchtv/src/main/AndroidManifest.xml`

**前置:** S1。

- [ ] **Step 1：新增** `background_picker.xml`

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <GridView xmlns:android="http://schemas.android.com/apk/res/android"
      android:id="@+id/backgroundGrid"
      android:layout_width="match_parent"
      android:layout_height="match_parent"
      android:clipToPadding="false"
      android:horizontalSpacing="8dp"
      android:numColumns="3"
      android:padding="12dp"
      android:stretchMode="columnWidth"
      android:verticalSpacing="12dp" />
  ```

- [ ] **Step 2：新增** `background_picker_item.xml`

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
      android:layout_width="match_parent"
      android:layout_height="wrap_content"
      android:orientation="vertical">

      <FrameLayout
          android:layout_width="match_parent"
          android:layout_height="140dp"
          android:background="@android:color/darker_gray">

          <ImageView
              android:id="@+id/bgImage"
              android:layout_width="match_parent"
              android:layout_height="match_parent"
              android:importantForAccessibility="no"
              android:scaleType="centerCrop" />

          <TextView
              android:id="@+id/bgSample"
              android:layout_width="match_parent"
              android:layout_height="wrap_content"
              android:layout_gravity="center"
              android:gravity="center"
              android:padding="6dp"
              android:text="@string/bg_picker_sample"
              android:textSize="16sp" />

          <TextView
              android:id="@+id/bgCheck"
              android:layout_width="wrap_content"
              android:layout_height="wrap_content"
              android:layout_gravity="top|end"
              android:padding="6dp"
              android:text="@string/bg_selected_mark"
              android:textSize="22sp"
              android:textStyle="bold"
              android:visibility="gone" />
      </FrameLayout>

      <TextView
          android:id="@+id/bgName"
          android:layout_width="match_parent"
          android:layout_height="wrap_content"
          android:gravity="center"
          android:maxLines="1"
          android:paddingTop="4dp"
          android:textSize="14sp" />
  </LinearLayout>
  ```

- [ ] **Step 3：新增** `BackgroundPickerActivity.kt`

  ```kotlin
  package org.cog.hymnchtv.reading

  import android.content.Context
  import android.content.Intent
  import android.content.SharedPreferences
  import android.graphics.Typeface
  import android.os.Bundle
  import android.view.View
  import android.view.ViewGroup
  import android.widget.BaseAdapter
  import android.widget.GridView
  import android.widget.ImageView
  import android.widget.TextView
  import androidx.activity.result.contract.ActivityResultContracts
  import org.cog.hymnchtv.BaseActivity
  import org.cog.hymnchtv.MainActivity
  import org.cog.hymnchtv.R
  import org.cog.hymnchtv.reading.background.BackgroundApplier
  import org.cog.hymnchtv.reading.background.BackgroundChoice
  import org.cog.hymnchtv.reading.background.BackgroundDrawables
  import org.cog.hymnchtv.reading.background.BackgroundPolicy
  import org.cog.hymnchtv.reading.background.BackgroundPreset
  import org.cog.hymnchtv.reading.background.BackgroundPrefs
  import org.cog.hymnchtv.reading.background.BackgroundSlot
  import org.cog.hymnchtv.reading.background.ReadingPalette
  import org.cog.hymnchtv.utils.WallPaperUtil

  /** Grid of the 20 backgrounds plus "your photo" for one slot (plan A2); writes the slot's pref and finishes. */
  class BackgroundPickerActivity : BaseActivity() {
      private lateinit var prefs: SharedPreferences
      private lateinit var slot: BackgroundSlot
      private val choices: List<BackgroundChoice> =
          BackgroundPreset.entries.map { BackgroundChoice.Preset(it) } + BackgroundChoice.Photo

      private val pickPhoto = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
          if (result.resultCode == RESULT_OK && BackgroundPrefs.photoFile(prefs) != null) {
              choose(BackgroundChoice.Photo)
          }
      }

      override fun onCreate(savedInstanceState: Bundle?) {
          super.onCreate(savedInstanceState)
          setContentView(R.layout.background_picker)
          prefs = getSharedPreferences(MainActivity.PREF_SETTINGS, MODE_PRIVATE)
          slot = BackgroundSlot.fromName(intent.getStringExtra(EXTRA_SLOT))
          setTitle(if (slot == BackgroundSlot.MAIN) R.string.pref_main_background else R.string.pref_lyrics_background)

          val grid = findViewById<GridView>(R.id.backgroundGrid)
          grid.adapter = Adapter(BackgroundPrefs.resolve(prefs, slot))
          grid.setOnItemClickListener { _, _, position, _ ->
              val choice = choices[position]
              if (choice is BackgroundChoice.Photo) {
                  // WallPaperUtil crops a new photo or re-uses the stored one; RESULT_OK means a photo file exists
                  pickPhoto.launch(Intent(this, WallPaperUtil::class.java))
              } else {
                  choose(choice)
              }
          }
      }

      private fun choose(choice: BackgroundChoice) {
          prefs.edit().putString(slot.prefKey, BackgroundPolicy.prefValue(choice)).apply()
          setResult(RESULT_OK)
          finish()
      }

      private inner class Adapter(private val current: BackgroundChoice) : BaseAdapter() {
          private val sampleFont: Typeface =
              (if (ReadingPrefs.lyricsFont(prefs) == LyricsFont.KAI) LyricsTypefaces.get(this@BackgroundPickerActivity, false) else null)
                  ?: Typeface.DEFAULT

          override fun getCount(): Int = choices.size
          override fun getItem(position: Int): Any = choices[position]
          override fun getItemId(position: Int): Long = position.toLong()

          override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
              val view = convertView ?: layoutInflater.inflate(R.layout.background_picker_item, parent, false)
              val choice = choices[position]
              val palette = bind(view.findViewById(R.id.bgImage), choice)
              view.findViewById<TextView>(R.id.bgSample).apply {
                  setTextColor(palette.textColor)
                  typeface = sampleFont
                  // Photo cell: the sample sits on the same 85 % panel the lyrics use, so the preview tells the truth
                  background = BackgroundDrawables.backdrop(this@BackgroundPickerActivity, palette)
              }
              view.findViewById<TextView>(R.id.bgName).setText(BackgroundDrawables.nameRes(choice))
              view.findViewById<TextView>(R.id.bgCheck).apply {
                  visibility = if (choice == current) View.VISIBLE else View.GONE
                  setTextColor(palette.accentColor)
              }
              return view
          }

          private fun bind(image: ImageView, choice: BackgroundChoice): ReadingPalette {
              if (choice is BackgroundChoice.Preset) {
                  image.setImageDrawable(null)
                  image.background = BackgroundDrawables.create(this@BackgroundPickerActivity, choice.preset)
                  return BackgroundPolicy.palette(choice)
              }
              image.background = null
              // No photo yet: the grey cell background shows through
              image.setImageBitmap(BackgroundPrefs.photoFile(prefs)?.let { BackgroundApplier.decodePhoto(it, THUMB_PX, THUMB_PX) })
              return BackgroundPolicy.PHOTO_PALETTE
          }
      }

      companion object {
          private const val EXTRA_SLOT = "slot"
          private const val THUMB_PX = 360

          @JvmStatic
          fun intent(context: Context, slot: BackgroundSlot): Intent =
              Intent(context, BackgroundPickerActivity::class.java).putExtra(EXTRA_SLOT, slot.name)
      }
  }
  ```

- [ ] **Step 4：在 Fragment 接上挑選畫面**

  在 `ReadingSettingsFragment.kt` 把：

  ```kotlin
                  // S2: pickBackground.launch(BackgroundPickerActivity.intent(requireContext(), slot))
  ```

  改成：

  ```kotlin
                  pickBackground.launch(BackgroundPickerActivity.intent(requireContext(), slot))
  ```

- [ ] **Step 5：`AndroidManifest.xml` 註冊**

  在 S1 加入的 `.reading.ReadingSettingsActivity` 區塊之後加入：

  ```xml

          <activity android:name=".reading.BackgroundPickerActivity" />
  ```

- [ ] **Step 6：建置與 lint**

  ```bash
  ./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug :hymnchtv:lintDebug --console=plain
  for k in MissingTranslation ExtraTranslation HardcodedText; do printf "%s: " $k; grep -c "\[$k\]" hymnchtv/build/reports/lint-results-debug.txt; done
  ```

  Expected: `BUILD SUCCESSFUL`；三個數字和 Task 0 的基準相同。

- [ ] **Step 7：Commit**

  ```bash
  git add hymnchtv/src/main/res/layout/background_picker.xml hymnchtv/src/main/res/layout/background_picker_item.xml \
          hymnchtv/src/main/java/org/cog/hymnchtv/reading/BackgroundPickerActivity.kt \
          hymnchtv/src/main/java/org/cog/hymnchtv/reading/ReadingSettingsFragment.kt hymnchtv/src/main/AndroidManifest.xml
  git commit -m "feat: add background picker" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

  Lane S 完成，回報給協調者合併。

### Task S3：移除舊桌布程式，改用系統照片挑選器匯入（使用者決策，rev 3）

使用者決定整支舊桌布程式（`WallPaperUtil`＋UCrop 裁切）移除。Lane S 在 S2 之後多做這個 task，另含兩項審查修正，皆已完成：

- 刪除：`utils/WallPaperUtil.java`、`res/layout/wallpaper_editor.xml`、manifest 的 `.utils.WallPaperUtil` 與 `com.yalantis.ucrop.UCropActivity`、字串 `wp_size`（三份）、`hymnchtv/build.gradle` 的 `com.github.yalantis:ucrop`、根 `build.gradle` 的 jitpack 倉庫（只有 ucrop 用到）。
- 新增 `reading/background/PhotoBackgroundImporter.kt`：`BackgroundPickerActivity` 的照片格用 `ActivityResultContracts.PickVisualMedia(ImageOnly)`（舊版 Android 自動退回文件挑選器，不需儲存權限）。結果 Uri 在背景執行緒處理：`ContentResolver` 開串流 → 讀尺寸 → 以 `PhotoBackground.boundedSampleSize` 解碼（長邊 ≤ 螢幕長邊 2 倍）→ 套用 EXIF 方向（`androidx.exifinterface:1.3.6`，S3 明確加入依賴，原本只是 Glide 的傳遞依賴）→ JPEG q85 寫到 `filesDir/backgrounds/photo.tmp` → `renameTo` 成 `photo.jpg`。`IOException`／`SecurityException`／`IllegalStateException`／OOM 都清掉暫存檔、顯示 toast（`bg_photo_import_failed`）並保持原狀；成功才把該位置（`EXTRA_SLOT`）的 pref 設為 `photo`。不做裁切，顯示仍是 centerCrop＋變暗／模糊。
- `BackgroundPrefs.photoFile(prefs)` 改讀固定路徑（簽名不變，`prefs` 參數保留以相容 I、M 的呼叫；路徑來自 `HymnsApp.getGlobalContext().filesDir`），不再使用 `PREF_WALLPAPER`、`DIR_WALLPAPER`、`FileBackend`。B3 Step 3 的 `photoFile`、`BackgroundDrawablesTest.photoPrefKeyIsTheWallpaperKey`、S2 `BackgroundPickerActivity` 啟動 `WallPaperUtil` 的程式碼已被 S3 取代，以 repo 內實際程式碼為準。
- `MainActivity.java` 只改最少的地方（lane M 之後再整理）：移除 `DIR_WALLPAPER` 與 `WallPaperUtil` 兩個 import、`sbguser` 的點擊分支，`setWallpaper()` 的自訂照片分支改成 `BackgroundPrefs.photoFile(mSharedPref)`（新增 `BackgroundPrefs` import）。選單 item `sbguser` 與 `PREF_WALLPAPER` 常數仍在，由 M1 刪除。
- 審查修正（設定畫面）：`reading_preferences.xml` 不再有任何 `app:defaultValue`，每個項目都是 `app:persistent="false"`。`ReadingSettingsFragment.bindStoredValues()` 從 `ReadingPrefs` 讀出有效值顯示（不寫入），使用者改動時才由 listener 寫入；`ConversionType` 的自我修復移到 `ReadingSettingsActivity.onCreate`，在註冊變更 listener 之前執行，所以只有使用者造成的寫入才會讓結果帶 `EXTR_KEY_HAS_CHANGES`。`ReadingPreferencesXmlTest.openingTheScreenWritesNothing` 守住這件事（原本的 `defaultsMatchTheCode` 拆成它和 `seekBarRangesMatchTheCode`）。
- 審查修正（挑選畫面）：照片縮圖只在背景執行緒解碼一次再快取，預設背景 drawable 依位置快取。
- 測試：`PhotoBackgroundImporterTest`（JVM：路徑、EXIF 方向對照；androidTest：匯入成功與失敗保留舊檔，只編譯檢查，V1 才跑）。新字串避開字型子集沒有的字（例如「張」），否則 `FontSubsetTest` 會失敗。

---

## 階段 3 · Lane I：歌詞頁

Lane I 的子代理在 worktree `../hymnchtv-a2-i` 工作（從合併了 S 的 `feat/reading-settings` 開出）。I1 先做，因為 I2 的 `ContentView` 要呼叫 I1 新增的 `ContentHandler` 方法。

### Task I1：ContentHandler 接上閱讀設定

**Files:**
- Modify: `hymnchtv/src/main/java/org/cog/hymnchtv/ContentHandler.java`
- Modify: `hymnchtv/src/main/res/layout/content_main.xml`
- Modify: `hymnchtv/src/main/res/menu/menu_content.xml`
- Modify: `hymnchtv/src/main/java/org/cog/hymnchtv/mediaconfig/LyricsEnglishRecord.java`

**前置:** 階段 2 合併。下面引用的原始碼片段是寫計畫時（`feat/zh-hant` @ 7486dbd）的內容；以當下分支的實際內容為準，行號可能偏移，用文字搜尋定位，找不到就停下來回報。

- [ ] **Step 1：整檔替換** `content_main.xml`

  加入最底層的背景 `ImageView`；根 layout 的 5 dp padding 改成各子 view 的 margin，背景才能鋪滿整個螢幕。（不用 `layout_marginHorizontal`：它是 API 26 才有的屬性，minSdk 是 24。）

  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
      android:id="@+id/linear"
      android:layout_width="match_parent"
      android:layout_height="match_parent"
      android:background="?attr/colorPrimary">

      <!-- Reading-settings background (plan A2); its own view so a photo can be blurred without blurring the lyrics -->
      <ImageView
          android:id="@+id/lyricsBackground"
          android:layout_width="match_parent"
          android:layout_height="match_parent"
          android:importantForAccessibility="no"
          android:scaleType="centerCrop" />

      <androidx.viewpager2.widget.ViewPager2
          android:id="@+id/viewPager"
          android:layout_width="match_parent"
          android:layout_height="match_parent"
          android:layout_above="@+id/mediaPlayer"
          android:layout_alignParentStart="true"
          android:layout_alignParentTop="true"
          android:layout_margin="5dp"
          android:spacing="20dp" />

      <LinearLayout
          android:id="@+id/mediaPlayer"
          android:layout_width="match_parent"
          android:layout_height="wrap_content"
          android:layout_alignParentBottom="true"
          android:layout_marginStart="5dp"
          android:layout_marginEnd="5dp"
          android:layout_marginBottom="5dp"
          android:orientation="vertical" />

      <LinearLayout
          android:id="@+id/filexferGui"
          android:layout_width="match_parent"
          android:layout_height="wrap_content"
          android:layout_alignTop="@+id/mediaPlayer"
          android:layout_marginStart="5dp"
          android:layout_marginEnd="5dp"
          android:orientation="vertical" />

      <LinearLayout
          android:id="@+id/webView"
          android:layout_width="match_parent"
          android:layout_height="wrap_content"
          android:layout_alignTop="@+id/viewPager"
          android:layout_marginStart="5dp"
          android:layout_marginEnd="5dp"
          android:orientation="vertical" />

  </RelativeLayout>
  ```

- [ ] **Step 2：`menu_content.xml`**

  1. 刪除 `alwayshow` 與 `alwayhide` 兩個 `<item>`（「預設顯示／隱藏播放器」已移到閱讀設定）。
  2. 在第一個 `<item>` 之前加入：

     ```xml
         <item
             android:id="@+id/readingSettings"
             android:title="@string/reading_settings" />

     ```

- [ ] **Step 3：`LyricsEnglishRecord.java`：英文歌詞字色跟著歌詞頁背景**

  1. 在 `private static final WeakHashMap<Context, LyricsEnglishRecord> INSTANCES = ...;` 的下一行加入：

     ```java

         /** Set by ContentHandler from the lyrics background (plan A2); null = follow the app theme as before. */
         private static volatile Boolean sDarkBackground = null;

         public static void setDarkBackground(boolean dark) {
             sDarkBackground = dark;
         }
     ```

  2. 在 `str2Html` 裡把：

     ```java
             // Change text and ulr according to app theme
             if (ThemeHelper.isAppTheme(ThemeHelper.Theme.DARK)) {
     ```

     改成：

     ```java
             // Text and link colours follow the lyrics background (plan A2), else the app theme
             boolean dark = (sDarkBackground != null) ? sDarkBackground : ThemeHelper.isAppTheme(ThemeHelper.Theme.DARK);
             if (dark) {
     ```

- [ ] **Step 4：`ContentHandler.java` 的 import**

  在既有 import 中補上（已存在的就跳過）：

  ```java
  import android.app.Activity;

  import androidx.activity.result.ActivityResultLauncher;
  import androidx.activity.result.contract.ActivityResultContracts;
  import androidx.annotation.VisibleForTesting;

  import org.cog.hymnchtv.mediaconfig.LyricsEnglishRecord;
  import org.cog.hymnchtv.reading.DisplayMode;
  import org.cog.hymnchtv.reading.LyricsTypefaces;
  import org.cog.hymnchtv.reading.ReadingPrefs;
  import org.cog.hymnchtv.reading.ReadingSettingsActivity;
  import org.cog.hymnchtv.reading.background.BackgroundPolicy;
  import org.cog.hymnchtv.reading.background.BackgroundPrefs;
  import org.cog.hymnchtv.reading.background.BackgroundSlot;
  import org.cog.hymnchtv.reading.background.ReadingPalette;
  ```

- [ ] **Step 5：欄位**

  把：

  ```java
      private static final String STATE_LYRICS_OVERRIDE = "state_lyrics_override"; // -1 none, 0 simplified, 1 traditional
  ```

  改成：

  ```java
      private static final String STATE_LYRICS_OVERRIDE = "state_lyrics_override"; // -1 none, 0 simplified, 1 traditional
      private static final String STATE_DISPLAY_OVERRIDE = "state_display_override"; // DisplayMode name; absent = none

      /** Per-session display mode chosen with button_mode; null = the default from the reading settings (plan A2). */
      public DisplayMode displayModeOverride = null;

      /** Colours matching the lyrics background actually shown; read by every ContentView page. */
      private ReadingPalette mLyricsPalette;

      private final ActivityResultLauncher<Intent> mReadingSettingsLauncher = registerForActivityResult(
              new ActivityResultContracts.StartActivityForResult(), result -> {
                  Intent data = result.getData();
                  onReadingSettingsReturned(result.getResultCode() == Activity.RESULT_OK && data != null
                          && data.getBooleanExtra(ContentView.EXTR_KEY_HAS_CHANGES, false));
              });
  ```

- [ ] **Step 6：`onCreate` 還原臨時切換**

  把：

  ```java
          if (savedInstanceState != null) {
              int saved = savedInstanceState.getInt(STATE_LYRICS_OVERRIDE, -1);
              lyricsViewOverride = (saved == -1) ? null : (saved == 1);
          }
  ```

  改成：

  ```java
          if (savedInstanceState != null) {
              int saved = savedInstanceState.getInt(STATE_LYRICS_OVERRIDE, -1);
              lyricsViewOverride = (saved == -1) ? null : (saved == 1);
              displayModeOverride = savedInstanceState.containsKey(STATE_DISPLAY_OVERRIDE)
                      ? DisplayMode.fromPref(savedInstanceState.getString(STATE_DISPLAY_OVERRIDE)) : null;
          }
  ```

- [ ] **Step 7：`onCreate` 套用背景、預載字型**

  把：

  ```java
          setContentView(R.layout.content_main);
          registerForContextMenu(findViewById(R.id.linear));
  ```

  改成：

  ```java
          setContentView(R.layout.content_main);
          registerForContextMenu(findViewById(R.id.linear));

          // Reading settings (plan A2): background first, so pages created below read the matching palette
          sPreference = getSharedPreferences(PREF_SETTINGS, 0);
          mLyricsPalette = BackgroundPrefs.applyTo(findViewById(R.id.lyricsBackground), sPreference, BackgroundSlot.LYRICS);
          LyricsEnglishRecord.setDarkBackground(mLyricsPalette.isDark());
          LyricsTypefaces.preload(this);
  ```

  然後把同一個方法後面的：

  ```java
          // Always start with UiPlayer hidden if in landscape mode
          sPreference = getSharedPreferences(PREF_SETTINGS, 0);
          isShowPlayerUi = sPreference.getBoolean(PREF_MENU_SHOW, true);
  ```

  改成（`sPreference` 已在上面設定）：

  ```java
          // Always start with UiPlayer hidden if in landscape mode
          isShowPlayerUi = sPreference.getBoolean(PREF_MENU_SHOW, true);
  ```

- [ ] **Step 8：翻頁動畫改成設定**

  把：

  ```java
          mPager.setPageTransformer(new DepthPageTransformer());
  ```

  改成：

  ```java
          // Plan A2: page-turn animation is a reading setting (B-11 will default it off on low-RAM phones)
          if (ReadingPrefs.pageAnimation(sPreference)) {
              mPager.setPageTransformer(new DepthPageTransformer());
          }
  ```

- [ ] **Step 9：螢幕常亮改成設定**

  把 `onResume` 裡的：

  ```java
          // Keep the screen on while lyrics/score are shown (plan A.1.7); window-level so pager changes never drop it
          getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  ```

  改成：

  ```java
          // Keep the screen on while lyrics/score are shown (plan A.1.7); a reading setting since A2, default on
          if (ReadingPrefs.keepScreenOn(sPreference)) {
              getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
          }
          else {
              getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
          }
  ```

  `onPause` 的 `clearFlags` 保持不變。

- [ ] **Step 10：保存顯示模式的臨時切換**

  在 `onSaveInstanceState` 的 `outState.putInt(STATE_LYRICS_OVERRIDE, ...);` 之後加入：

  ```java
          if (displayModeOverride != null) {
              outState.putString(STATE_DISPLAY_OVERRIDE, displayModeOverride.name());
          }
  ```

- [ ] **Step 11：新增兩個 public 方法**

  加在 `showMediaPlayerUi()` 方法之後：

  ```java
      /**
       * Colours for the lyrics background on screen (plan A2). A page restored by the FragmentManager can ask
       * before onCreate has applied the background, so fall back to the resolved (not yet shown) choice.
       */
      public ReadingPalette getLyricsPalette() {
          if (mLyricsPalette == null) {
              mLyricsPalette = BackgroundPolicy.palette(
                      BackgroundPrefs.resolve(getSharedPreferences(PREF_SETTINGS, 0), BackgroundSlot.LYRICS));
          }
          return mLyricsPalette;
      }

      /** Opens the reading settings; on return with changes all pages are rebuilt (see onReadingSettingsReturned). */
      public void openReadingSettings() {
          mReadingSettingsLauncher.launch(new Intent(this, ReadingSettingsActivity.class));
      }

      /**
       * New defaults: drop both session toggles and rebuild every page with the new settings.
       * Public so ContentHandlerReadingTest can drive the settings-return path without the settings UI.
       */
      @VisibleForTesting
      public void onReadingSettingsReturned(boolean hasChanges) {
          if (hasChanges) {
              lyricsViewOverride = null;
              displayModeOverride = null;
              recreate();
          }
      }
  ```

- [ ] **Step 12：`onContextItemSelected`**

  把：

  ```java
      public boolean onContextItemSelected(MenuItem item) {
          SharedPreferences.Editor editor = sPreference.edit();
          ContentView contentView = (ContentView) mPagerAdapter.mFragments.get(mPager.getCurrentItem());

          int itemId = item.getItemId();
          if (itemId == R.id.alwayshow) {
              isShowPlayerUi = true;
              editor.putBoolean(PREF_MENU_SHOW, true);
              editor.apply();
              showPlayerUi(true);
              return true;
          }
          else if (itemId == R.id.alwayhide) {
              isShowPlayerUi = false;
              editor.putBoolean(PREF_MENU_SHOW, false);
              editor.apply();
              showPlayerUi(false);
              return true;
          }
          else if (itemId == R.id.menutoggle) {
  ```

  改成：

  ```java
      public boolean onContextItemSelected(MenuItem item) {
          ContentView contentView = (ContentView) mPagerAdapter.mFragments.get(mPager.getCurrentItem());

          int itemId = item.getItemId();
          if (itemId == R.id.readingSettings) {
              openReadingSettings();
              return true;
          }
          else if (itemId == R.id.menutoggle) {
  ```

  然後確認方法內沒有其他地方用到 `editor`：`grep -n "editor\." hymnchtv/src/main/java/org/cog/hymnchtv/ContentHandler.java`。若還有，停下來回報。

- [ ] **Step 13：建置**

  Run: `./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug --console=plain`
  Expected: `BUILD SUCCESSFUL`。（`ContentView` 還沒用到新方法，I2 會接上。）

- [ ] **Step 14：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/ContentHandler.java hymnchtv/src/main/res/layout/content_main.xml \
          hymnchtv/src/main/res/menu/menu_content.xml hymnchtv/src/main/java/org/cog/hymnchtv/mediaconfig/LyricsEnglishRecord.java
  git commit -m "feat: lyrics page honours background, page animation and screen-on settings" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

### Task I2：ContentView 的顯示模式、字型、字級、樂譜色調；修第 5 頁 bug

**Files:**
- Modify（整檔替換）: `hymnchtv/src/main/java/org/cog/hymnchtv/ContentView.java`
- Modify（整檔替換）: `hymnchtv/src/main/res/layout/content_lyrics.xml`

**Files（續）:**
- Test（instrumented）: `hymnchtv/src/androidTest/java/org/cog/hymnchtv/ContentHandlerReadingTest.kt`

**前置:** I1。改動遍布整個類別，所以整檔替換。計畫裡的版本是以寫計畫時的 A 版本為底，**執行時要和當下 `feat/zh-hant` 的最新版本比對**（Codex P2），不能只比固定的 commit：

```bash
git show feat/zh-hant:hymnchtv/src/main/java/org/cog/hymnchtv/ContentView.java > /tmp/ContentView.zh-hant.java
# 先把 Step 2 的新檔寫到 /tmp/ContentView.a2.java，再比對
git diff --no-index /tmp/ContentView.zh-hant.java /tmp/ContentView.a2.java
```

逐一檢查每個 hunk，都必須屬於 Step 2 開頭列出的 A2 變更。出現其他差異（例如 A 之後又修了簡繁切換），表示 `feat/zh-hant` 在寫計畫後又改過這個檔案：把那些修改移植進新檔，並在回報中逐條列出；看不懂的就停下來問。

- [ ] **Step 1：整檔替換** `content_lyrics.xml`

  變更：5 張樂譜包進 `scoreContainer`；按鈕列加上 `button_mode`、拿掉 `colorPrimary` 底色；兩個歌詞 view 拿掉 `tv_border_bg_primary` 底色（字直接寫在背景上）。

  ```xml
  <?xml version="1.0" encoding="utf-8"?>

  <ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
      xmlns:tools="http://schemas.android.com/tools"
      android:layout_width="fill_parent"
      android:layout_height="fill_parent"
      tools:ignore="ContentDescription">

      <LinearLayout
          android:id="@+id/lyricsView"
          android:layout_width="match_parent"
          android:layout_height="wrap_content"
          android:orientation="vertical">

          <!-- Up to 5 score pages; hidden as a whole in "lyrics only" (plan A2) -->
          <LinearLayout
              android:id="@+id/scoreContainer"
              android:layout_width="match_parent"
              android:layout_height="wrap_content"
              android:orientation="vertical">

              <ImageView
                  android:id="@+id/contentView"
                  android:layout_width="match_parent"
                  android:layout_height="match_parent"
                  android:adjustViewBounds="true"
                  android:scaleType="fitStart" />

              <ImageView
                  android:id="@+id/contentView_a"
                  android:layout_width="match_parent"
                  android:layout_height="wrap_content"
                  android:adjustViewBounds="true"
                  android:scaleType="fitStart" />

              <ImageView
                  android:id="@+id/contentView_b"
                  android:layout_width="match_parent"
                  android:layout_height="wrap_content"
                  android:adjustViewBounds="true"
                  android:scaleType="fitStart" />

              <ImageView
                  android:id="@+id/contentView_c"
                  android:layout_width="match_parent"
                  android:layout_height="wrap_content"
                  android:adjustViewBounds="true"
                  android:scaleType="fitStart" />

              <ImageView
                  android:id="@+id/contentView_d"
                  android:layout_width="match_parent"
                  android:layout_height="wrap_content"
                  android:adjustViewBounds="true"
                  android:scaleType="fitStart" />
          </LinearLayout>

          <LinearLayout
              android:id="@+id/lyricsButtonBar"
              android:layout_width="fill_parent"
              android:layout_height="wrap_content"
              android:orientation="horizontal"
              android:padding="3dp">

              <Button
                  android:id="@+id/button_ts"
                  style="@style/ButtonTop"
                  android:layout_weight="1"
                  android:text="@string/lyrics_chinese_traditional"
                  android:textSize="22sp" />

              <Button
                  android:id="@+id/button_english"
                  style="@style/ButtonTop"
                  android:layout_marginStart="4dp"
                  android:layout_weight="1"
                  android:text="@string/lyrics_chinese_english"
                  android:textSize="22sp" />

              <Button
                  android:id="@+id/button_mode"
                  style="@style/ButtonTop"
                  android:layout_marginStart="4dp"
                  android:layout_weight="1"
                  android:text="@string/display_mode_both"
                  android:textSize="18sp" />
          </LinearLayout>

          <org.cog.hymnchtv.utils.ZoomTextView
              android:id="@+id/lyrics_simplified"
              android:layout_width="match_parent"
              android:layout_height="wrap_content"
              android:padding="10dp"
              android:textIsSelectable="true"
              android:textSize="20sp" />

          <org.cog.hymnchtv.utils.ZoomTextView
              android:id="@+id/lyrics_traditional"
              android:layout_width="match_parent"
              android:layout_height="wrap_content"
              android:padding="10dp"
              android:textIsSelectable="true"
              android:textSize="20sp" />

          <WebView
              android:id="@+id/lyrics_english"
              android:layout_width="match_parent"
              android:layout_height="wrap_content"
              android:gravity="center_horizontal"
              tools:ignore="WebViewLayout" />
      </LinearLayout>
  </ScrollView>
  ```

- [ ] **Step 2：整檔替換** `ContentView.java`

  和 A 版本相比的差異：
  - `PREF_LYRICS_SCALE_P／L` 改為 `ReadingPrefKeys` 的別名；縮放起點改由 `ReadingPrefs.lyricsScale` 決定，基準字級改用 `LyricsScale`。
  - 新增 `button_mode`、`scoreContainer`、`applyDisplayMode()`；臨時切換存在 `ContentHandler.displayModeOverride`。
  - 先讀歌詞文字再決定要不要載入樂譜；「只顯示詞」不載入樂譜；「只顯示譜」不觸發教唱提示。
  - `showLyricsScore` 改成依 `ScorePages` 迴圈處理 5 個 `ImageView`（修正第 5 頁蓋掉第 4 頁）；多出來的 view 設為 `GONE`。
  - 樂譜色調改用 `ScoreTintPolicy`（等級 0 = 跟背景自動）；切換色調只更新濾鏡，不重新載入圖片。
  - 歌詞文字色、連結色、選取色、字型跟著調色盤與字型設定；照片背景時歌詞與英文歌詞後面加 85% 底板。
  - 從含譜的模式切到「只顯示詞」時，用 `Glide.clear` 釋放樂譜點陣圖；切回時重新載入。
  - 長按簡繁鍵或模式鍵改呼叫 `ContentHandler.openReadingSettings()`；刪除自己的 `ActivityResultLauncher` 和 `ChineseS2TSelection` 的引用。

  ```java
  /*
   * hymnchtv: COG hymns' lyrics viewer and player client
   * Copyright 2020 Eng Chong Meng
   *
   * Licensed under the Apache License, Version 2.0 (the "License");
   * you may not use this file except in compliance with the License.
   * You may obtain a copy of the License at
   *
   * http://www.apache.org/licenses/LICENSE-2.0
   *
   * Unless required by applicable law or agreed to in writing, software
   * distributed under the License is distributed on an "AS IS" BASIS,
   * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   * See the License for the specific language governing permissions and
   * limitations under the License.
   */
  package org.cog.hymnchtv;

  import static org.cog.hymnchtv.MainActivity.HYMN_BB;
  import static org.cog.hymnchtv.MainActivity.HYMN_DB;
  import static org.cog.hymnchtv.MainActivity.HYMN_ER;
  import static org.cog.hymnchtv.MainActivity.HYMN_XB;
  import static org.cog.hymnchtv.MainActivity.HYMN_XG;
  import static org.cog.hymnchtv.MainActivity.HYMN_YB;
  import static org.cog.hymnchtv.MainActivity.PREF_SETTINGS;
  import static org.cog.hymnchtv.utils.ZoomTextView.STEP_SCALE_FACTOR;

  import android.annotation.SuppressLint;
  import android.content.Context;
  import android.content.SharedPreferences;
  import android.graphics.Color;
  import android.graphics.ColorFilter;
  import android.graphics.ColorMatrixColorFilter;
  import android.graphics.Typeface;
  import android.os.Bundle;
  import android.os.Handler;
  import android.os.Looper;
  import android.text.TextUtils;
  import android.view.ContextMenu;
  import android.view.LayoutInflater;
  import android.view.View;
  import android.view.ViewGroup;
  import android.webkit.WebSettings;
  import android.webkit.WebView;
  import android.widget.Button;
  import android.widget.ImageView;

  import androidx.annotation.NonNull;
  import androidx.annotation.Nullable;
  import androidx.fragment.app.Fragment;

  import java.io.BufferedReader;
  import java.io.IOException;
  import java.io.InputStreamReader;
  import java.nio.charset.StandardCharsets;
  import java.util.List;
  import java.util.Locale;

  import com.bumptech.glide.Glide;

  import org.cog.hymnchtv.glide.MyGlideApp;
  import org.cog.hymnchtv.lyrics.HantVariant;
  import org.cog.hymnchtv.lyrics.LyricsAssets;
  import org.cog.hymnchtv.lyrics.LyricsLang;
  import org.cog.hymnchtv.lyrics.LyricsLanguagePolicy;
  import org.cog.hymnchtv.mediaconfig.LyricsEnglishRecord;
  import org.cog.hymnchtv.reading.DisplayMode;
  import org.cog.hymnchtv.reading.DisplayModePolicy;
  import org.cog.hymnchtv.reading.LyricsFont;
  import org.cog.hymnchtv.reading.LyricsScale;
  import org.cog.hymnchtv.reading.LyricsTypefaces;
  import org.cog.hymnchtv.reading.ReadingPrefKeys;
  import org.cog.hymnchtv.reading.ReadingPrefs;
  import org.cog.hymnchtv.reading.ScorePages;
  import org.cog.hymnchtv.reading.ScoreTintPolicy;
  import org.cog.hymnchtv.reading.background.BackgroundDrawables;
  import org.cog.hymnchtv.reading.background.ReadingPalette;
  import org.cog.hymnchtv.utils.HymnIdx2NoConvert;
  import org.cog.hymnchtv.utils.ZoomTextView;
  import org.jetbrains.annotations.NotNull;

  import timber.log.Timber;

  /**
   * The class displays the hymn lyrics content selected by user;
   * It is a part of the whole Hymn lyrics content UI display
   * Note: The context menu needs to be created here, instead its parent, for it to be visible
   *
   * @author Eng Chong Meng
   */
  public class ContentView extends Fragment implements ZoomTextView.ZoomTextListener, View.OnClickListener,
          View.OnLongClickListener, LyricsEnglishRecord.EnglishLyricsListener {
      public static String SCORE_DB_DIR = "lyrics_db_score/";
      public static String SCORE_BB_DIR = "lyrics_bb_score/";
      public static String SCORE_ER_DIR = "lyrics_er_score/";
      public static String SCORE_XB_DIR = "lyrics_xb_score/";
      public static String SCORE_XG_DIR = "lyrics_xg_score/";
      public static String SCORE_YB_DIR = "lyrics_yb_score/";

      public static String LYRICS_DB_DIR = "lyrics_db_text/";
      public static String LYRICS_BB_DIR = "lyrics_bb_text/";
      public static String LYRICS_ER_DIR = "lyrics_er_text/";
      public static String LYRICS_XB_DIR = "lyrics_xb_text/";
      public static String LYRICS_XG_DIR = "lyrics_xg_text/";
      public static String LYRICS_YB_DIR = "lyrics_yb_text/";

      public static String LYRICS_TOC = "lyrics_toc/";

      public final static String LYRICS_TYPE = "lyricsType";
      public final static String LYRICS_INDEX = "lyricsIndex";

      public static final String EXTR_KEY_HAS_CHANGES = "hasChanges";
      public final static String PREF_SCORE_COLOR = "ScoreColor";
      public static final String PREF_CONVERSION_TYPE = "ConversionType";
      public static final String PREF_LYRICS_SCALE_P = ReadingPrefKeys.LYRICS_SCALE_P;
      public static final String PREF_LYRICS_SCALE_L = ReadingPrefKeys.LYRICS_SCALE_L;
      public static final String PREF_LYRICS_ENGLISH_SCALE_P = "LyricsScaleEP";
      public static final String PREF_LYRICS_ENGLISH_SCALE_L = "LyricsScaleEL";

      /** One view per score page: the hymn itself, then suffixes a-d (plan A2: page 5 no longer reuses page 4's view). */
      private static final int[] SCORE_VIEW_IDS = {R.id.contentView, R.id.contentView_a, R.id.contentView_b,
              R.id.contentView_c, R.id.contentView_d};

      public ContentHandler mContentHandler;
      private LyricsEnglishRecord mLyricsEnglishRecord;

      private Button btn_ts;
      private Button btn_english;
      private Button btn_mode;
      private View mConvertView;
      private View lyricsView;
      private View scoreContainer;
      private ZoomTextView lyricsSimplify;
      private ZoomTextView lyricsTraditional;
      private WebView lyricsEnglish;

      private Integer mHymnNoEng = null;
      private boolean isErGe;
      private boolean mLyricsLoaded = false;
      private boolean hasEnglishLyrics = false;
      private boolean mScoreLoaded = false;
      private boolean mHasLyricsText = false;

      /** Score colour level from the context menu; 0 = automatic (follows the background, see ScoreTintPolicy). */
      private static int mScoreColor = 0;

      private static float lyricsScaleP;
      private static float lyricsScaleL;
      private static float lyricsScaleEP;
      private static float lyricsScaleEL;

      private ReadingPalette mPalette;
      private String mResPrefix;
      private int[] mHymnScoreInfo;

      private SharedPreferences mSharedPref;
      private SharedPreferences.Editor mEditor;

  // Need this to prevent crash on rotation if there are other constructors implementation
  // public ContentView() { }

      @Override
      public void onAttach(@NonNull @NotNull Context context) {
          super.onAttach(context);
          mContentHandler = (ContentHandler) context;

          mLyricsEnglishRecord = LyricsEnglishRecord.getInstanceFor(mContentHandler);
          mLyricsEnglishRecord.registerLyricsListener(this);

          mSharedPref = mContentHandler.getSharedPreferences(PREF_SETTINGS, 0);
          mEditor = mSharedPref.edit();
      }

      @SuppressLint("SetJavaScriptEnabled")
      @Override
      public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
          mConvertView = inflater.inflate(R.layout.content_lyrics, container, false);
          scoreContainer = mConvertView.findViewById(R.id.scoreContainer);

          btn_ts = mConvertView.findViewById(R.id.button_ts);
          btn_ts.setOnClickListener(this);
          btn_ts.setOnLongClickListener(this);

          btn_english = mConvertView.findViewById(R.id.button_english);
          btn_english.setOnClickListener(this);
          btn_english.setOnLongClickListener(this);

          btn_mode = mConvertView.findViewById(R.id.button_mode);
          btn_mode.setOnClickListener(this);
          btn_mode.setOnLongClickListener(this);

          lyricsView = mConvertView.findViewById(R.id.lyricsView);
          lyricsSimplify = mConvertView.findViewById(R.id.lyrics_simplified);
          lyricsSimplify.registerZoomTextListener(this);

          lyricsTraditional = mConvertView.findViewById(R.id.lyrics_traditional);
          lyricsTraditional.registerZoomTextListener(this);

          lyricsEnglish = mConvertView.findViewById(R.id.lyrics_english);

          mPalette = mContentHandler.getLyricsPalette();
          applyPaletteAndFont();

          lyricsScaleP = ReadingPrefs.lyricsScale(mSharedPref, true);
          lyricsScaleL = ReadingPrefs.lyricsScale(mSharedPref, false);
          lyricsScaleEP = mSharedPref.getFloat(PREF_LYRICS_ENGLISH_SCALE_P, 1.0f);
          lyricsScaleEL = mSharedPref.getFloat(PREF_LYRICS_ENGLISH_SCALE_L, 1.0f);

          mScoreColor = mSharedPref.getInt(PREF_SCORE_COLOR, 0);

          mLyricsLoaded = false;
          hasEnglishLyrics = false;
          mScoreLoaded = false;
          mHasLyricsText = false;

          Bundle bundle = getArguments();
          if (bundle != null) {
              String lyricsType = getArguments().getString(LYRICS_TYPE);
              int lyricsIndex = getArguments().getInt(LYRICS_INDEX);

              if (!TextUtils.isEmpty(lyricsType)) {
                  updateHymnContent(lyricsType, lyricsIndex);
              }
          }
          applyDisplayMode(true);
          return mConvertView;
      }

      @Override
      public void onResume() {
          super.onResume();
          registerForContextMenu(lyricsView);
          Timber.w("Content View on Resume");

          // get the corresponding English lyrics# or null if none
          mHymnNoEng = mContentHandler.getHymnNoEng();
          boolean autoEnglish = mContentHandler.mAutoEnglish;
          if (autoEnglish) {
              // autoload English lyrics for first entry only.
              mContentHandler.mAutoEnglish = false;
              hasEnglishLyrics = true;
          }
          // ViewPager2 only resumes the visible page: re-apply toggles (script, display mode) made on another page.
          // Do not reload English lyrics that are already showing.
          applyDisplayMode(autoEnglish || !hasEnglishLyrics);
      }

      @Override
      public void onPause() {
          unregisterForContextMenu(lyricsView);
          super.onPause();
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public void onCreateContextMenu(@NotNull ContextMenu menu, @NotNull View v, ContextMenu.ContextMenuInfo menuInfo) {
          super.onCreateContextMenu(menu, v, menuInfo);
          mContentHandler.getMenuInflater().inflate(R.menu.menu_content, menu);

          // Hide "英文歌词" if no associated English lyrics
          menu.findItem(R.id.lyrcsEnglish).setVisible(mHymnNoEng != null);
          menu.findItem(R.id.lyrcsEnglishDelete).setVisible(mHymnNoEng != null && hasEnglishLyrics);
      }

      @Override
      public void onClick(View v) {
          int id = v.getId();
          if (id == R.id.button_ts) {
              if (!hasEnglishLyrics) {
                  // Session-only toggle; the persisted default is set in the reading settings
                  mContentHandler.lyricsViewOverride = !isShowTraditional();
              }
              else {
                  hasEnglishLyrics = false;
              }
              toggleLyricsView();
          }
          else if (id == R.id.button_english) {
              hasEnglishLyrics = !hasEnglishLyrics;
              toggleLyricsView();
          }
          else if (id == R.id.button_mode) {
              // Session-only toggle (plan A2); the persisted default is set in the reading settings
              mContentHandler.displayModeOverride = currentDisplayMode().next();
              applyDisplayMode(true);
          }
      }

      @Override
      public boolean onLongClick(View v) {
          int id = v.getId();
          if (id == R.id.button_ts || id == R.id.button_mode) {
              mContentHandler.openReadingSettings();
              return true;
          }
          else if (id == R.id.button_english) {
              if (View.VISIBLE == lyricsEnglish.getVisibility()) {
                  mContentHandler.initWebView(ContentHandler.UrlType.englishLyrics);
              }
              else {
                  HymnsApp.showToastMessage("Reinit English lyrics");
                  reinitEnglishLyrics();
              }
              return true;
          }
          return false;
      }

      private void reinitEnglishLyrics() {
          MainActivity.showContent(mContentHandler, mContentHandler.mHymnType, mContentHandler.getHymnNo(), false, mHymnNoEng);
      }

      /**
       * The lyrics png/jpg file has the following formats: HYMN_ER, HYMN_XB, HYMN_XG, HYMN_YB, HYMN_BB, HYMN_DB
       * i.e. er, xb, xg, yb, bb, db followed by the hymn number, a, b, c etc for more than one page;
       * The files are stored in asset respective sub-dir e.g. LYRICS_XB_SCORE
       * The content view can support up to 5 pages for user vertical scrolls
       *
       * @param hymnType see below cases
       * @param hymnIndex hymn index provided by the page adapter when user scroll
       */
      private void updateHymnContent(String hymnType, int hymnIndex) {
          String resFName;
          mHymnScoreInfo = HymnIdx2NoConvert.hymnIdx2NoConvert(hymnType, hymnIndex);

          // Chinese lyrics#
          int lyricsNo = mHymnScoreInfo[0];
          isErGe = HYMN_ER.equals(hymnType);

          switch (hymnType) {
          case HYMN_ER:
              mResPrefix = SCORE_ER_DIR + lyricsNo;
              resFName = LYRICS_ER_DIR + "er" + lyricsNo + ".txt";
              break;

          case HYMN_XB:
              mResPrefix = SCORE_XB_DIR + "xb" + lyricsNo;
              resFName = LYRICS_XB_DIR + "xb" + lyricsNo + ".txt";
              break;

          case HYMN_XG:
              mResPrefix = SCORE_XG_DIR + "xg" + lyricsNo;
              resFName = LYRICS_XG_DIR + "xg" + lyricsNo + ".txt";
              break;

          case HYMN_YB:
              mResPrefix = SCORE_YB_DIR + "yb" + lyricsNo;
              resFName = LYRICS_YB_DIR + "yb" + lyricsNo + ".txt";
              break;

          case HYMN_BB:
              mResPrefix = SCORE_BB_DIR + "bb" + lyricsNo;
              resFName = LYRICS_BB_DIR + "bb" + lyricsNo + ".txt";
              break;

          case HYMN_DB:
              mResPrefix = SCORE_DB_DIR + "db" + lyricsNo;
              resFName = LYRICS_DB_DIR + "db" + lyricsNo + ".txt";
              break;

          default:
              Timber.e("Unsupported content type: %s", hymnType);
              return;
          }

          // Text first (plan A2): it is a few KB and decides whether "lyrics only" can be honoured
          if (!TextUtils.isEmpty(resFName)) {
              setLyricsTextScale();
              showLyricsChText(resFName);
          }

          // The score images are the expensive part; "lyrics only" skips them until the mode changes
          if (DisplayModePolicy.effective(currentDisplayMode(), mHasLyricsText).getShowScore()) {
              showLyricsScore(mResPrefix, mHymnScoreInfo);
          }
      }

      /** Score colour for the current manual level and lyrics background (plan A2). */
      @Nullable
      private ColorFilter scoreColorFilter() {
          float[] matrix = ScoreTintPolicy.matrix(ScoreTintPolicy.resolve(mScoreColor, mPalette.isDark(),
                  mPalette.getPaperColor(), mPalette.getTextColor()));
          return (matrix == null) ? null : new ColorMatrixColorFilter(matrix);
      }

      public void toggleScoreColor() {
          mScoreColor = (mScoreColor + 1) % ScoreTintPolicy.LEVEL_COUNT;
          mEditor.putInt(PREF_SCORE_COLOR, mScoreColor);
          mEditor.apply();
          applyScoreFilter();
      }

      private void applyScoreFilter() {
          ColorFilter filter = scoreColorFilter();
          for (int id : SCORE_VIEW_IDS) {
              ImageView view = mConvertView.findViewById(id);
              view.setColorFilter(filter);
          }
      }

      /**
       * Display the selected Hymn Lyric Scores. Scores with multi-pages have suffixed with a, b, c and d.
       * i.e. support a total of 5 pages maximum.
       *
       * @param resPrefix The selected Hymn Lyric scores fileName prefix
       * @param hymnScoreInfo Contain info for the hymnNo and number of pages of the selected Lyric Scores
       */
      private void showLyricsScore(String resPrefix, int[] hymnScoreInfo) {
          Context ctx = getContext();
          ColorFilter filter = scoreColorFilter();
          List<String> names = ScorePages.fileNames(resPrefix, hymnScoreInfo[1]);
          for (int i = 0; i < SCORE_VIEW_IDS.length; i++) {
              ImageView view = mConvertView.findViewById(SCORE_VIEW_IDS[i]);
              if (i < names.size()) {
                  view.setVisibility(View.VISIBLE);
                  view.setColorFilter(filter);
                  MyGlideApp.loadImage(ctx, view, names.get(i));
              }
              else {
                  view.setVisibility(View.GONE);
              }
          }
          mScoreLoaded = true;
      }

      /**
       * Display the selected hymn lyrics text
       *
       * @param resFName Lyrics text resource fileName
       */
      private void showLyricsChText(String resFName) {
          String lyrics = readAsset(resFName);
          if (lyrics != null) {
              lyricsSimplify.setText(lyrics);
              lyricsTraditional.setText(loadTraditional(resFName, lyrics));
          }
          mHasLyricsText = DisplayModePolicy.hasLyricsText(lyrics);

          // Auto launch or hint user to view lyrics text via online JiaoChang if available; er,length > 47.
          // Not in "score only": the reader asked not to see lyrics.
          if (!mHasLyricsText && currentDisplayMode() != DisplayMode.SCORE_ONLY) {
              mContentHandler.selectJC();
          }
      }

      /** Pre-generated Traditional lyrics (plan A.1.9); the sync test guarantees they exist, Simplified is a last resort. */
      private String loadTraditional(String resFName, String simplified) {
          HantVariant variant = LyricsLanguagePolicy.parseVariant(mSharedPref.getString(PREF_CONVERSION_TYPE, null), uiLocale());
          String hantPath = LyricsAssets.hantPath(resFName, variant);
          String text = (hantPath == null) ? null : readAsset(hantPath);
          if (text != null) {
              return text;
          }
          Timber.w("Missing pre-generated lyrics for %s (%s); showing Simplified", resFName, hantPath);
          return simplified;
      }

      /** @return the asset text with '\n' line ends, or null if it cannot be read. */
      private String readAsset(String path) {
          try (BufferedReader reader = new BufferedReader(
                  new InputStreamReader(getResources().getAssets().open(path), StandardCharsets.UTF_8))) {
              StringBuilder text = new StringBuilder();
              String line;
              while ((line = reader.readLine()) != null) {
                  text.append(line).append('\n');
              }
              return text.toString();
          }
          catch (IOException e) {
              Timber.w("Error reading file: %s", path);
              return null;
          }
      }

      /** Text colour, link/selection colours, text backdrop and typeface from the reading settings (plan A2). */
      private void applyPaletteAndFont() {
          boolean kai = ReadingPrefs.lyricsFont(mSharedPref) == LyricsFont.KAI;
          styleLyrics(lyricsSimplify, kai ? LyricsTypefaces.get(mContentHandler, false) : null);
          styleLyrics(lyricsTraditional, kai ? LyricsTypefaces.get(mContentHandler, true) : null);
          // Photo backgrounds: English lyrics also sit on the contrast-tested panel (PhotoPaletteTest).
          // The WebView's own colour stays transparent; the panel is the View background (set last, so it wins).
          lyricsEnglish.setBackgroundColor(Color.TRANSPARENT);
          lyricsEnglish.setBackground(BackgroundDrawables.backdrop(mContentHandler, mPalette));
      }

      private void styleLyrics(ZoomTextView view, @Nullable Typeface typeface) {
          int accent = mPalette.getAccentColor();
          view.setTextColor(mPalette.getTextColor());
          view.setLinkTextColor(accent);
          view.setHighlightColor((accent & 0x00FFFFFF) | 0x40000000);
          view.setTypeface(typeface != null ? typeface : Typeface.DEFAULT);
          // null for drawn backgrounds; an 85 % panel for photos
          view.setBackground(BackgroundDrawables.backdrop(mContentHandler, mPalette));
      }

      /**
       * Update the lyrics text view default size and the stored scale factor
       * Also being used onConfiguration change
       */
      public void setLyricsTextScale() {
          if (lyricsEnglish == null || lyricsSimplify == null || lyricsTraditional == null) {
              Timber.e(new Exception("Lyrics content view is null"));
              return;
          }

          // English lyrics text size in webSettings
          final WebSettings webSettings = lyricsEnglish.getSettings();

          if (HymnsApp.isPortrait) {
              lyricsSimplify.scaleTextSize(LyricsScale.BASE_SP_PORTRAIT, lyricsScaleP);
              lyricsTraditional.scaleTextSize(LyricsScale.BASE_SP_PORTRAIT, lyricsScaleP);
              webSettings.setDefaultFontSize((int) (18 * lyricsScaleEP));
          }
          else {
              lyricsSimplify.scaleTextSize(LyricsScale.BASE_SP_LANDSCAPE, lyricsScaleL);
              lyricsTraditional.scaleTextSize(LyricsScale.BASE_SP_LANDSCAPE, lyricsScaleL);
              webSettings.setDefaultFontSize((int) (26 * lyricsScaleEL));
          }
      }

      /**
       * Increase or decrease the lyrics text scale factor
       *
       * @param stepInc true if size increment else decrement
       */
      public void setLyricsTextSize(boolean stepInc) {
          if (lyricsEnglish.getVisibility() == View.VISIBLE) {
              setLyricsEnglishTextScale(stepInc);
          }
          else {
              float scaleFactor = lyricsSimplify.onTextSizeChange(stepInc);
              lyricsTraditional.onTextSizeChange(stepInc);
              updateTextScale(scaleFactor);
          }
      }

      // Handler for english lyrics textSize changes
      private void setLyricsEnglishTextScale(boolean stepInc) {
          float tmpScale = stepInc ? STEP_SCALE_FACTOR : -STEP_SCALE_FACTOR;

          if (HymnsApp.isPortrait) {
              lyricsScaleEP += tmpScale;
              mEditor.putFloat(PREF_LYRICS_ENGLISH_SCALE_P, lyricsScaleEP);
          }
          else {
              lyricsScaleEL += tmpScale;
              mEditor.putFloat(PREF_LYRICS_ENGLISH_SCALE_L, lyricsScaleEL);
          }
          mEditor.apply();
          setLyricsTextScale();
      }

      /**
       * Save the user selected scale factory to preference settings
       *
       * @param scaleFactor scale factor
       */
      @Override
      public void updateTextScale(Float scaleFactor) {
          if (HymnsApp.isPortrait) {
              lyricsScaleP = scaleFactor;
              mEditor.putFloat(PREF_LYRICS_SCALE_P, scaleFactor);
          }
          else {
              lyricsScaleL = scaleFactor;
              mEditor.putFloat(PREF_LYRICS_SCALE_L, scaleFactor);
          }
          mEditor.apply();
      }

      /** Stored default, or this session's button_mode choice. */
      private DisplayMode currentDisplayMode() {
          return DisplayModePolicy.resolve(mContentHandler.displayModeOverride, ReadingPrefs.displayMode(mSharedPref));
      }

      /**
       * Show/hide score, buttons and lyrics for the display mode (plan A2).
       *
       * @param refreshLyrics false keeps the current lyrics views as they are (avoids re-fetching English lyrics)
       */
      private void applyDisplayMode(boolean refreshLyrics) {
          DisplayMode chosen = currentDisplayMode();
          DisplayMode shown = DisplayModePolicy.effective(chosen, mHasLyricsText);
          btn_mode.setText(displayModeLabel(chosen));

          if (shown.getShowScore() && !mScoreLoaded && mResPrefix != null) {
              showLyricsScore(mResPrefix, mHymnScoreInfo);
          }
          else if (!shown.getShowScore() && mScoreLoaded) {
              releaseScores();
          }
          scoreContainer.setVisibility(shown.getShowScore() ? View.VISIBLE : View.GONE);

          btn_ts.setVisibility(shown.getShowLyrics() ? View.VISIBLE : View.GONE);
          btn_english.setVisibility(shown.getShowLyrics() && mHymnNoEng != null ? View.VISIBLE : View.GONE);
          if (!shown.getShowLyrics()) {
              lyricsSimplify.setVisibility(View.GONE);
              lyricsTraditional.setVisibility(View.GONE);
              lyricsEnglish.setVisibility(View.GONE);
          }
          else if (refreshLyrics) {
              toggleLyricsView();
          }
      }

      /** "Lyrics only": let Glide recycle the score bitmaps; switching back reloads them (Codex P2). */
      private void releaseScores() {
          for (int id : SCORE_VIEW_IDS) {
              ImageView view = mConvertView.findViewById(id);
              Glide.with(this).clear(view);
          }
          mScoreLoaded = false;
      }

      private static int displayModeLabel(DisplayMode mode) {
          switch (mode) {
          case SCORE_ONLY:
              return R.string.display_mode_score;
          case LYRICS_ONLY:
              return R.string.display_mode_lyrics;
          default:
              return R.string.display_mode_both;
          }
      }

      private void toggleLyricsView() {
          lyricsTraditional.setVisibility(View.GONE);
          lyricsSimplify.setVisibility(View.GONE);
          lyricsEnglish.setVisibility(View.GONE);

          if (hasEnglishLyrics && mHymnNoEng != null) {
              lyricsEnglish.setVisibility(View.VISIBLE);
              Timber.d("Lyrics English #%s loaded: %s", mHymnNoEng, mLyricsLoaded);
              if (!mLyricsLoaded) {
                  showLyricsEnglish(LyricsEnglishRecord.str2Html("<h3>" + getResources().getString(R.string.download_wait) + "</h3>"), false);
              }
              mLyricsEnglishRecord.fetchLyrics(mHymnNoEng, isErGe);
          }
          else {
              if (!isShowTraditional()) {
                  lyricsSimplify.setVisibility(View.VISIBLE);
              }
              else {
                  lyricsTraditional.setVisibility(View.VISIBLE);
              }
          }
      }

      private boolean isShowTraditional() {
          Boolean override = mContentHandler.lyricsViewOverride;
          if (override != null) {
              return override;
          }
          LyricsLang pref = LyricsLang.fromPref(mSharedPref.getString(LyricsLanguagePolicy.PREF_LYRICS_DEFAULT, null));
          return LyricsLanguagePolicy.resolveShowTraditional(pref, uiLocale());
      }

      private Locale uiLocale() {
          return mContentHandler.getResources().getConfiguration().getLocales().get(0);
      }

      @Override
      public void showLyricsEnglish(final String lyrics, boolean preload) {
          new Handler(Looper.getMainLooper()).post(() -> {
              if (lyrics != null) {
                  mLyricsLoaded = true;
                  if (preload) {
                      lyricsEnglish.loadUrl("about:blank");
                  }

                  new Handler(Looper.getMainLooper()).postDelayed(() -> {
                      // stop canGoBack to display about:blank. not working 100%.
                      lyricsEnglish.clearHistory();
                      // Timber.d("Show Lyrics English: %s", lyrics.length());
                      lyricsEnglish.loadDataWithBaseURL(null, lyrics, "text/html", "utf8", null);
                  }, 100);
              }
              else {
                  lyricsEnglish.loadUrl(LyricsEnglishRecord.HYMNAL_LINK_MAIN + mHymnNoEng);
              }
          });
      }
  }
  ```

  注意事項：
  - Kotlin 的 `ReadingPalette.isDark` 在 Java 是 `isDark()`、`backdropColor` 是 `getBackdropColor()`、`DisplayMode.showScore` 是 `getShowScore()`。
  - `MyGlideApp.loadImage` 內部用 Glide 載入；`Glide.with(this).clear(view)` 用同一個 Fragment 取消並釋放請求。
  - `switch` 用在 Kotlin enum 上是允許的（Java 會產生 ordinal 對應表）。
  - `ContentHandler.java` 的 `onConfigurationChanged` 呼叫 `setLyricsTextScale()`，簽章沒變。

- [ ] **Step 3：新增 instrumented test** `ContentHandlerReadingTest.kt`（Codex P2：A 與 A2 的互動）

  直接用 intent 開 `ContentHandler`（大本 1，歌詞夠長，不會觸發教唱提示），不經過 `MainActivity` 的權限與 changelog 對話框。測試只增刪自己設定的 pref key，不清掉裝置上的其他設定。

  ```kotlin
  package org.cog.hymnchtv

  import android.content.Context
  import android.content.Intent
  import android.os.Bundle
  import android.os.SystemClock
  import android.view.View
  import android.view.WindowManager
  import android.widget.ImageView
  import androidx.test.core.app.ActivityScenario
  import androidx.test.core.app.ApplicationProvider
  import androidx.test.ext.junit.runners.AndroidJUnit4
  import androidx.test.platform.app.InstrumentationRegistry
  import androidx.viewpager2.widget.ViewPager2
  import com.google.common.truth.Truth.assertThat
  import org.cog.hymnchtv.lyrics.LyricsLang
  import org.cog.hymnchtv.lyrics.LyricsLanguagePolicy
  import org.cog.hymnchtv.reading.DisplayMode
  import org.cog.hymnchtv.reading.ReadingPrefKeys
  import org.junit.After
  import org.junit.Before
  import org.junit.Test
  import org.junit.runner.RunWith
  import java.util.concurrent.atomic.AtomicReference

  /** A behaviours (script toggle, page, screen-on) together with A2's display mode (Task I2). Run on API 24 and 34. */
  @RunWith(AndroidJUnit4::class)
  class ContentHandlerReadingTest {
      private val ctx: Context = ApplicationProvider.getApplicationContext()
      private val prefs = ctx.getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE)
      private val keys = listOf(
          ReadingPrefKeys.DISPLAY_MODE, ReadingPrefKeys.KEEP_SCREEN_ON, ReadingPrefKeys.MENU_SHOW,
          LyricsLanguagePolicy.PREF_LYRICS_DEFAULT,
      )

      @Before
      fun setUp() {
          prefs.edit()
              .putString(ReadingPrefKeys.DISPLAY_MODE, DisplayMode.SCORE_AND_LYRICS.name)
              .putBoolean(ReadingPrefKeys.KEEP_SCREEN_ON, true)
              .putBoolean(ReadingPrefKeys.MENU_SHOW, false)
              .putString(LyricsLanguagePolicy.PREF_LYRICS_DEFAULT, LyricsLang.SIMPLIFIED.name)
              .commit()
      }

      @After
      fun tearDown() {
          val editor = prefs.edit()
          keys.forEach { editor.remove(it) }
          editor.commit()
      }

      private fun launch(): ActivityScenario<ContentHandler> {
          val extras = Bundle().apply {
              putString(MainActivity.ATTR_HYMN_TYPE, MainActivity.HYMN_DB)
              putInt(MainActivity.ATTR_HYMN_NUMBER, 1)
          }
          return ActivityScenario.launch(Intent(ctx, ContentHandler::class.java).putExtras(extras))
      }

      private fun <T> ActivityScenario<ContentHandler>.read(block: (ContentHandler) -> T): T {
          val ref = AtomicReference<T>()
          onActivity { ref.set(block(it)) }
          return ref.get()
      }

      /** The page ViewPager2 has resumed (exactly one at rest). */
      private fun page(activity: ContentHandler): View? =
          activity.supportFragmentManager.fragments.filterIsInstance<ContentView>().singleOrNull { it.isResumed }?.view

      private fun ActivityScenario<ContentHandler>.waitFor(what: String, condition: (ContentHandler) -> Boolean) {
          val end = SystemClock.uptimeMillis() + 10_000
          while (!read { condition(it) }) {
              check(SystemClock.uptimeMillis() < end) { "timed out waiting for $what" }
              InstrumentationRegistry.getInstrumentation().waitForIdleSync()
              SystemClock.sleep(50)
          }
      }

      private fun ActivityScenario<ContentHandler>.click(id: Int) = onActivity { page(it)!!.findViewById<View>(id).performClick() }

      private fun visible(activity: ContentHandler, id: Int) = page(activity)!!.findViewById<View>(id).visibility == View.VISIBLE

      @Test
      fun keepScreenOnFollowsTheSetting() {
          launch().use { scenario ->
              scenario.waitFor("page") { page(it) != null }
              assertThat(scenario.read { it.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON }).isNotEqualTo(0)
              prefs.edit().putBoolean(ReadingPrefKeys.KEEP_SCREEN_ON, false).commit()
              scenario.recreate()
              scenario.waitFor("page") { page(it) != null }
              assertThat(scenario.read { it.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON }).isEqualTo(0)
          }
      }

      @Test
      fun scriptToggleAndPageSurviveRecreate() {
          launch().use { scenario ->
              scenario.waitFor("page") { page(it) != null }
              scenario.click(R.id.button_ts)
              assertThat(scenario.read { visible(it, R.id.lyrics_traditional) }).isTrue()
              val item = scenario.read { it.findViewById<ViewPager2>(R.id.viewPager).currentItem } + 1
              scenario.onActivity { it.findViewById<ViewPager2>(R.id.viewPager).setCurrentItem(item, false) }
              scenario.waitFor("next page") { page(it) != null }
              scenario.recreate()
              scenario.waitFor("restored page") { page(it) != null }
              assertThat(scenario.read { it.findViewById<ViewPager2>(R.id.viewPager).currentItem }).isEqualTo(item)
              assertThat(scenario.read { it.lyricsViewOverride }).isTrue()
              assertThat(scenario.read { visible(it, R.id.lyrics_traditional) }).isTrue()
          }
      }

      @Test
      fun displayModeToggleSurvivesRecreate() {
          launch().use { scenario ->
              scenario.waitFor("page") { page(it) != null }
              scenario.click(R.id.button_mode)   // SCORE_AND_LYRICS -> SCORE_ONLY
              assertThat(scenario.read { it.displayModeOverride }).isEqualTo(DisplayMode.SCORE_ONLY)
              assertThat(scenario.read { visible(it, R.id.lyrics_simplified) || visible(it, R.id.lyrics_traditional) }).isFalse()
              assertThat(scenario.read { visible(it, R.id.button_ts) }).isFalse()
              scenario.recreate()
              scenario.waitFor("restored page") { page(it) != null }
              assertThat(scenario.read { it.displayModeOverride }).isEqualTo(DisplayMode.SCORE_ONLY)
              assertThat(scenario.read { visible(it, R.id.scoreContainer) }).isTrue()
              assertThat(scenario.read { visible(it, R.id.lyrics_simplified) }).isFalse()
          }
      }

      @Test
      fun lyricsOnlyReleasesScoresAndSwitchingBackReloadsThem() {
          launch().use { scenario ->
              scenario.waitFor("first score image") { a -> page(a)?.findViewById<ImageView>(R.id.contentView)?.drawable != null }
              scenario.click(R.id.button_mode)   // -> SCORE_ONLY
              scenario.click(R.id.button_mode)   // -> LYRICS_ONLY
              assertThat(scenario.read { it.displayModeOverride }).isEqualTo(DisplayMode.LYRICS_ONLY)
              assertThat(scenario.read { visible(it, R.id.scoreContainer) }).isFalse()
              assertThat(scenario.read { page(it)!!.findViewById<ImageView>(R.id.contentView).drawable }).isNull()
              scenario.click(R.id.button_mode)   // -> SCORE_AND_LYRICS
              scenario.waitFor("reloaded score image") { a -> page(a)?.findViewById<ImageView>(R.id.contentView)?.drawable != null }
              assertThat(scenario.read { visible(it, R.id.scoreContainer) && visible(it, R.id.lyrics_simplified) }).isTrue()
          }
      }

      @Test
      fun settingsReturnWithChangesDropsTogglesAndRebuilds() {
          launch().use { scenario ->
              scenario.waitFor("page") { page(it) != null }
              scenario.click(R.id.button_ts)
              scenario.click(R.id.button_mode)
              scenario.onActivity { it.onReadingSettingsReturned(false) }
              assertThat(scenario.read { it.displayModeOverride }).isEqualTo(DisplayMode.SCORE_ONLY)
              prefs.edit().putString(ReadingPrefKeys.DISPLAY_MODE, DisplayMode.LYRICS_ONLY.name).commit()
              scenario.onActivity { it.onReadingSettingsReturned(true) }
              scenario.waitFor("rebuilt page") { page(it) != null }
              assertThat(scenario.read { it.displayModeOverride }).isNull()
              assertThat(scenario.read { it.lyricsViewOverride }).isNull()
              assertThat(scenario.read { visible(it, R.id.scoreContainer) }).isFalse()   // new default LYRICS_ONLY applied
          }
      }
  }
  ```

  如果 `ContentHandler` 在測試環境開不起來（例如 `HymnsApp.mMediaDownloadHandler` 尚未初始化而當機），**不要刪測試**：在 V1 以 log 確認原因後回報協調者決定。

- [ ] **Step 4：建置與全部測試**

  ```bash
  ./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug :hymnchtv:assembleDebugAndroidTest :hymnchtv:lintDebug --console=plain
  grep -rn "ChineseS2TSelection" hymnchtv/src/main/java/org/cog/hymnchtv/ContentView.java || echo "no reference"
  ```

  Expected: `BUILD SUCCESSFUL`；印出 `no reference`。instrumented test 在 V1 執行。

- [ ] **Step 5：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/ContentView.java hymnchtv/src/main/res/layout/content_lyrics.xml \
          hymnchtv/src/androidTest/java/org/cog/hymnchtv/ContentHandlerReadingTest.kt
  git commit -m "feat: display modes, lyrics font, size presets and score tint on lyrics pages" -m "Also fixes the 5th score page being loaded into the 4th page's view." -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

  Lane I 完成，回報給協調者合併。

---

## 階段 3 · Lane M：主頁

Lane M 的子代理在 worktree `../hymnchtv-a2-m` 工作，和 lane I 同時進行（檔案不重疊）。

### Task M1：主頁背景、閱讀設定選單、移除舊桌布程式

**Files:**
- Modify: `hymnchtv/src/main/java/org/cog/hymnchtv/MainActivity.java`
- Modify: `hymnchtv/src/main/res/layout/main.xml`、`hymnchtv/src/main/res/layout-land/main.xml`
- Modify: `hymnchtv/src/main/res/menu/menu_main.xml`
- Modify（test）: `hymnchtv/src/test/java/org/cog/hymnchtv/utils/ThemeDefaultTest.kt`

**前置:** 階段 2 合併。舊 JPG 檔本身在 C1 才刪除（這裡先移除所有程式引用）。`utils/WallPaperUtil.java`、`wallpaper_editor.xml`、UCrop 依賴與 `sbguser` 的點擊分支已在 S3 刪除；S3 讓 `MainActivity.setWallpaper()` 的自訂照片分支改讀 `BackgroundPrefs.photoFile(mSharedPref)`（M1 Step 6 會把整個 `setWallpaper()` 換掉）。所以 M1 不要再碰 `WallPaperUtil`，也找不到 `DIR_WALLPAPER`。

- [ ] **Step 1：`layout/main.xml` 加上背景 view**

  1. 把檔案開頭：

     ```xml
     <?xml version="1.0" encoding="utf-8"?>
     <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
         xmlns:tools="http://schemas.android.com/tools"
         android:id="@+id/viewMain"
         android:layout_width="match_parent"
         android:layout_height="match_parent"
         android:background="@drawable/bg0"
         android:orientation="vertical"
         android:padding="5dp">
     ```

     改成：

     ```xml
     <?xml version="1.0" encoding="utf-8"?>
     <FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
         xmlns:tools="http://schemas.android.com/tools"
         android:layout_width="match_parent"
         android:layout_height="match_parent">

     <!-- Reading-settings background (plan A2); its own view so a photo can be blurred without blurring the keys -->
     <ImageView
         android:id="@+id/mainBackground"
         android:layout_width="match_parent"
         android:layout_height="match_parent"
         android:importantForAccessibility="no"
         android:scaleType="centerCrop" />

     <LinearLayout
         android:id="@+id/viewMain"
         android:layout_width="match_parent"
         android:layout_height="match_parent"
         android:orientation="vertical"
         android:padding="5dp">
     ```

  2. 在檔案最後的 `</LinearLayout>` 之後加上一行 `</FrameLayout>`。
  3. 在 `android:text="@string/hint_hymn_ui"` 那一行的上一行加入 `        android:id="@+id/tv_hint"`（縮排和同一個 TextView 的其他屬性一致）。

- [ ] **Step 2：`layout-land/main.xml` 做同樣的事**

  1. 開頭：

     ```xml
     <?xml version="1.0" encoding="utf-8"?>
     <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
         xmlns:tools="http://schemas.android.com/tools"
         android:id="@+id/viewMain"
         android:layout_width="match_parent"
         android:layout_height="match_parent"
         android:background="@drawable/bg0"
         android:baselineAligned="false"
         android:orientation="horizontal"
         android:padding="5dp">
     ```

     改成：

     ```xml
     <?xml version="1.0" encoding="utf-8"?>
     <FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
         xmlns:tools="http://schemas.android.com/tools"
         android:layout_width="match_parent"
         android:layout_height="match_parent">

     <!-- Reading-settings background (plan A2); its own view so a photo can be blurred without blurring the keys -->
     <ImageView
         android:id="@+id/mainBackground"
         android:layout_width="match_parent"
         android:layout_height="match_parent"
         android:importantForAccessibility="no"
         android:scaleType="centerCrop" />

     <LinearLayout
         android:id="@+id/viewMain"
         android:layout_width="match_parent"
         android:layout_height="match_parent"
         android:baselineAligned="false"
         android:orientation="horizontal"
         android:padding="5dp">
     ```

  2. 檔尾加 `</FrameLayout>`；`hint_hymn_ui` 的 TextView 加上 `android:id="@+id/tv_hint"`。
  3. 檢查：`grep -c "tv_hint\|mainBackground\|bg0" hymnchtv/src/main/res/layout/main.xml hymnchtv/src/main/res/layout-land/main.xml` 每個檔案都是 `2`（`tv_hint` 1 次、`mainBackground` 1 次、`bg0` 0 次）。

- [ ] **Step 3：`menu_main.xml`**

  1. 把：

     ```xml
         <item
             android:id="@+id/lyricsLanguage"
             android:title="@string/lyrics_language_menu" />
     ```

     改成：

     ```xml
         <item
             android:id="@+id/readingSettings"
             android:title="@string/reading_settings" />
     ```

  2. 刪除整個 `<item android:id="@+id/bg" android:title="@string/wall_paper"> ... </item>` 區塊（含 `sbguser`、`sbg1`～`sbg12`；`sbguser` 的程式分支 S3 已刪，選單 item 與字串 `wallpaperUser` 仍在，這一步一起刪）。

- [ ] **Step 4：`MainActivity.java` 常數與欄位**

  1. 把 `public static final String PREF_MENU_SHOW = "MenuShow";` 改成：

     ```java
         public static final String PREF_MENU_SHOW = ReadingPrefKeys.MENU_SHOW;
     ```

  2. 刪除 `public static final String PREF_BACKGROUND = "Background";`。
  3. 刪除欄位 `private LinearLayout background;`，以及 `initButton()` 裡的 `background = findViewById(R.id.viewMain);`。
  4. 刪除：

     ```java
         // Available background wall papers
         public static int[] bgResId = {R.drawable.bg0, R.drawable.bg1, R.drawable.bg2, R.drawable.bg3, R.drawable.bg4, R.drawable.bg5,
                 R.drawable.bg20, R.drawable.bg21, R.drawable.bg22, R.drawable.bg23, R.drawable.bg24, R.drawable.bg25};
     ```

- [ ] **Step 5：選單處理**

  1. 把：

     ```java
         else if (itemId == R.id.lyricsLanguage) {
             startActivity(new Intent(this, ChineseS2TSelection.class));
             return true;
         }
     ```

     改成：

     ```java
         else if (itemId == R.id.readingSettings) {
             mStartForResult.launch(new Intent(this, ReadingSettingsActivity.class));
             return true;
         }
     ```

  2. 刪除從 `// === Set background color ===` 開始，到 `sbg12` 分支結束的所有 `else if` 分支（`sbg1`～`sbg12`，共 12 個；`sbguser` 分支 S3 已刪）。刪完後，前一個分支（`R.id.black`）的 `}` 之後接著就是 `else if (itemId == R.id.sn_convert) {`。

- [ ] **Step 6：背景套用**

  1. `initUserSettings()` 裡的 `setWallpaper();` 改成 `applyMainBackground();`。
  2. 把整個 `setWallpaper()` 方法（含上方 Javadoc）換成：

     ```java
         /**
          * Show the main-screen background chosen in the reading settings (plan A2) and colour the hint to match.
          */
         private void applyMainBackground() {
             ReadingPalette palette = BackgroundPrefs.applyTo(findViewById(R.id.mainBackground), mSharedPref, BackgroundSlot.MAIN);
             TextView hint = findViewById(R.id.tv_hint);
             hint.setTextColor(palette.getAccentColor());
             // Photo backgrounds: the hint sits on the same contrast-tested panel as the lyrics (null otherwise)
             hint.setBackground(BackgroundDrawables.backdrop(this, palette));
         }
     ```

  3. 刪除整個 `setBgColor(int bgMode, int resId)` 方法（含 Javadoc）。
  4. 把 `mStartForResult` 的實作：

     ```java
         ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(new StartActivityForResult(), result -> {
             if (result.getResultCode() == Activity.RESULT_OK) {
                 Intent intent = result.getData();
                 Uri uri = (intent == null) ? null : intent.getData();
                 if (uri == null) {
                     Timber.d("No image data selected: %s", intent);
                 }
                 else {
                     setWallpaper();
                 }
             }
         });
     ```

     改成：

     ```java
         ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(new StartActivityForResult(), result -> {
             // Back from the reading settings: the main background (or its photo dim/blur) may have changed
             if (result.getResultCode() == Activity.RESULT_OK) {
                 applyMainBackground();
             }
         });
     ```

- [ ] **Step 7：MainActivity 的 import**

  加入：

  ```java
  import org.cog.hymnchtv.reading.ReadingPrefKeys;
  import org.cog.hymnchtv.reading.ReadingSettingsActivity;
  import org.cog.hymnchtv.reading.background.BackgroundDrawables;
  import org.cog.hymnchtv.reading.background.BackgroundPrefs;
  import org.cog.hymnchtv.reading.background.BackgroundSlot;
  import org.cog.hymnchtv.reading.background.ReadingPalette;
  ```

  刪除 `import org.cog.hymnchtv.utils.ChineseS2TSelection;`。然後逐一檢查下列 import 是否還有使用者，沒有就刪除：`import android.graphics.drawable.Drawable;`、`import java.io.File;`、`import org.cog.hymnchtv.persistance.FileBackend;`、`import android.net.Uri;`、`import android.widget.LinearLayout;`。檢查方式：`grep -n "Drawable\b\|\bFile\b\|FileBackend\|\bUri\b\|LinearLayout" hymnchtv/src/main/java/org/cog/hymnchtv/MainActivity.java`，只出現在 import 行的才刪。

- [ ] **Step 7b：預設主題改用 `ThemeHelper.DEFAULT_THEME`（使用者決策：淺色）**

  在 `onCreate` 把：

  ```java
          String theme = mSharedPref.getString(PREF_THEME, Theme.DARK.toString());
  ```

  改成：

  ```java
          String theme = mSharedPref.getString(PREF_THEME, ThemeHelper.DEFAULT_THEME.toString());
  ```

  選單的 `R.id.themeDark` 分支（`setAppTheme(Theme.DARK.toString(), true)`）不動，那是使用者主動選深色。

  在 `ThemeDefaultTest.kt`（P5 建立）加入一個測試，並補上 `import java.io.File`：

  ```kotlin
      @Test
      fun mainActivityReadsThePrefWithTheSharedDefault() {
          val root = File(checkNotNull(System.getProperty("hymnchtv.repoRoot")) { "hymnchtv.repoRoot not set" })
          val source = File(root, "hymnchtv/src/main/java/org/cog/hymnchtv/MainActivity.java").readText()
          assertThat(source).contains("getString(PREF_THEME, ThemeHelper.DEFAULT_THEME.toString())")
          assertThat(source).doesNotContain("getString(PREF_THEME, Theme.DARK.toString())")
      }
  ```

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.utils.ThemeDefaultTest' --console=plain`
  Expected: 3 個測試通過（改 `MainActivity` 之前，新測試會失敗）。

- [ ] **Step 8：移除 `PREF_WALLPAPER` 殘留**

  S3 之後 `MainActivity.PREF_WALLPAPER` 沒有任何使用者（`BackgroundPrefs.photoFile` 讀固定的私有路徑 `filesDir/backgrounds/photo.jpg`）。刪除 `public static final String PREF_WALLPAPER = "WallPaper";`；`PREF_BACKGROUND` 依 Step 9 的 grep 一併清掉。

- [ ] **Step 9：確認沒有殘留引用**

  ```bash
  grep -rn "bgResId\|PREF_BACKGROUND\|PREF_WALLPAPER\|WallPaperUtil\|DIR_WALLPAPER\|setBgColor\|setWallpaper\|R.id.sbg\|R.id.bg\b\|lyricsLanguage\|R.drawable.bg[0-9]" hymnchtv/src/main/java hymnchtv/src/main/res/layout hymnchtv/src/main/res/layout-land hymnchtv/src/main/res/menu || echo "clean"
  ./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug --console=plain
  ```

  Expected: 印出 `clean`；`BUILD SUCCESSFUL`。

- [ ] **Step 10：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/MainActivity.java hymnchtv/src/main/res/layout/main.xml \
          hymnchtv/src/main/res/layout-land/main.xml hymnchtv/src/main/res/menu/menu_main.xml \
          hymnchtv/src/test/java/org/cog/hymnchtv/utils/ThemeDefaultTest.kt
  git commit -m "feat: main screen background, reading settings menu entry and light default theme" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

  Lane M 完成，回報給協調者合併。

---

## 階段 4：清理、驗證、審查（在主 checkout 依序進行）

### Task C1：刪除舊桌布、ChineseS2TSelection 與不再使用的字串

**Files:**
- Create（instrumented test）: `hymnchtv/src/androidTest/java/org/cog/hymnchtv/reading/PhotoBackdropTest.kt`
- Delete: `hymnchtv/src/main/java/org/cog/hymnchtv/utils/ChineseS2TSelection.java`、`hymnchtv/src/main/res/layout/chinese_t2s_selection.xml`、`hymnchtv/src/main/res/drawable-ldpi/bg*.jpg`
- Modify: `hymnchtv/src/main/AndroidManifest.xml`、三份 `strings.xml`
- Modify: `hymnchtv/src/test/java/org/cog/hymnchtv/reading/background/BackgroundResourcesTest.kt`

**前置:** 階段 3 全部合併，`./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug` 通過。

- [ ] **Step 1：先加回歸測試（會失敗）**

  在 `BackgroundResourcesTest` 加入：

  ```kotlin
      @Test
      fun legacyJpegWallpapersAreGone() {
          val legacy = drawableDirs.flatMap { d -> d.listFiles { f -> f.name.matches(Regex("bg\\d+\\.jpg")) }!!.toList() }
          assertThat(legacy).isEmpty()
      }
  ```

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.reading.background.BackgroundResourcesTest' --console=plain`
  Expected: `legacyJpegWallpapersAreGone` 失敗，列出 12 個 `bg*.jpg`。

- [ ] **Step 2：刪除舊檔案**

  ```bash
  grep -rn "ChineseS2TSelection\|chinese_t2s_selection" hymnchtv/src/main --include=*.java --include=*.kt --include=*.xml
  ```

  Expected: 只剩 `ChineseS2TSelection.java` 本身、`chinese_t2s_selection.xml` 本身和 `AndroidManifest.xml` 的 `<activity android:name=".utils.ChineseS2TSelection" ... />`。若還有其他引用，停下來回報。

  ```bash
  git rm hymnchtv/src/main/java/org/cog/hymnchtv/utils/ChineseS2TSelection.java \
         hymnchtv/src/main/res/layout/chinese_t2s_selection.xml \
         hymnchtv/src/main/res/drawable-ldpi/bg*.jpg
  ```

  然後從 `AndroidManifest.xml` 刪除：

  ```xml
          <activity
              android:name=".utils.ChineseS2TSelection"
              android:label="@string/STD" />
  ```

- [ ] **Step 3：刪除不再使用的字串**

  候選：`sbg1`～`sbg12`、`wall_paper`、`wallpaperUser`、`lyrics_language_menu`、`menu_media_ui_default_show`、`menu_media_ui_default_hide`。先確認每一個都沒有引用：

  ```bash
  for n in sbg1 sbg2 sbg3 sbg4 sbg5 sbg6 sbg7 sbg8 sbg9 sbg10 sbg11 sbg12 wall_paper wallpaperUser lyrics_language_menu \
           menu_media_ui_default_show menu_media_ui_default_hide; do
    hits=$(grep -rn "R.string.$n\b\|@string/$n\"" hymnchtv/src/main --include=*.java --include=*.kt --include=*.xml | wc -l)
    echo "$n $hits"
  done
  ```

  Expected: 每一個都是 `0`。不是 0 的那個不要刪，並在回報中註明。

  刪除（三份 `strings.xml` 都處理；這些字串都是單行）：

  ```bash
  python3 - <<'EOF'
  import pathlib, re
  names = [f"sbg{i}" for i in range(1, 13)] + ["wall_paper", "wallpaperUser", "lyrics_language_menu",
           "menu_media_ui_default_show", "menu_media_ui_default_hide"]
  pattern = re.compile(r'^\s*<string name="(%s)"[^>]*>.*</string>\s*\n' % "|".join(names), re.M)
  for d in ("values", "values-zh", "values-b+zh+Hant"):
      p = pathlib.Path(f"hymnchtv/src/main/res/{d}/strings.xml")
      text = p.read_text(encoding="utf-8")
      new, count = pattern.subn("", text)
      p.write_text(new, encoding="utf-8")
      print(d, count)
  EOF
  ```

  Expected: 三行都是 `16`（16 個字串各刪一次）。若某個檔案少於 16，用 `grep -n` 找出那個字串，確認它是不是跨行，手動刪除。

- [ ] **Step 3b：新增 instrumented test** `hymnchtv/src/androidTest/java/org/cog/hymnchtv/reading/PhotoBackdropTest.kt`

  Codex P2：照片模式下，所有直接寫在照片上的文字都要有底板。放在 C1 是因為它同時用到 lane I、M、S 的成果。V1 Step 0 執行。

  ```kotlin
  package org.cog.hymnchtv.reading

  import android.Manifest
  import android.content.Context
  import android.content.Intent
  import android.graphics.Bitmap
  import android.graphics.Color
  import android.graphics.drawable.GradientDrawable
  import android.os.Build
  import android.os.Bundle
  import android.os.SystemClock
  import android.view.View
  import android.widget.GridView
  import android.widget.TextView
  import androidx.test.core.app.ActivityScenario
  import androidx.test.core.app.ApplicationProvider
  import androidx.test.ext.junit.runners.AndroidJUnit4
  import androidx.test.platform.app.InstrumentationRegistry
  import androidx.test.rule.GrantPermissionRule
  import com.google.common.truth.Truth.assertThat
  import org.cog.hymnchtv.ContentHandler
  import org.cog.hymnchtv.ContentView
  import org.cog.hymnchtv.MainActivity
  import org.cog.hymnchtv.R
  import org.cog.hymnchtv.reading.background.BackgroundPolicy
  import org.cog.hymnchtv.reading.background.BackgroundSlot
  import org.cog.hymnchtv.reading.background.PhotoBackgroundImporter
  import org.junit.After
  import org.junit.Before
  import org.junit.Rule
  import org.junit.Test
  import org.junit.runner.RunWith
  import java.io.File
  import java.util.concurrent.atomic.AtomicReference

  /** In photo mode every text drawn on the photo has the PHOTO_PALETTE panel behind it (Codex P2). API 24 and 34. */
  @RunWith(AndroidJUnit4::class)
  class PhotoBackdropTest {
      @get:Rule
      val permissions: GrantPermissionRule = if (Build.VERSION.SDK_INT >= 33) {
          GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
      } else {
          GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)
      }

      private val ctx: Context = ApplicationProvider.getApplicationContext()
      private val prefs = ctx.getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE)
      private val keys = listOf(BackgroundSlot.MAIN.prefKey, BackgroundSlot.LYRICS.prefKey)
      private lateinit var photo: File

      @Before
      fun setUp() {
          photo = PhotoBackgroundImporter.photoFileIn(ctx.filesDir)
          photo.parentFile?.mkdirs()
          val white = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
          photo.outputStream().use { white.compress(Bitmap.CompressFormat.JPEG, 85, it) }
          prefs.edit()
              .putString(BackgroundSlot.MAIN.prefKey, BackgroundPolicy.PHOTO)
              .putString(BackgroundSlot.LYRICS.prefKey, BackgroundPolicy.PHOTO)
              .commit()
      }

      @After
      fun tearDown() {
          val editor = prefs.edit()
          keys.forEach { editor.remove(it) }
          editor.commit()
          photo.delete()
      }

      private fun assertPanel(view: View, what: String) {
          val panel = view.background
          assertThat(panel).named(what).isInstanceOf(GradientDrawable::class.java)
          assertThat((panel as GradientDrawable).color?.defaultColor).named(what)
              .isEqualTo(BackgroundPolicy.PHOTO_PALETTE.backdropColor)
      }

      private fun <A : android.app.Activity, T> ActivityScenario<A>.read(block: (A) -> T): T {
          val ref = AtomicReference<T>()
          onActivity { ref.set(block(it)) }
          return ref.get()
      }

      @Test
      fun lyricsPageTextSitsOnThePanel() {
          val extras = Bundle().apply {
              putString(MainActivity.ATTR_HYMN_TYPE, MainActivity.HYMN_DB)
              putInt(MainActivity.ATTR_HYMN_NUMBER, 1)
          }
          ActivityScenario.launch<ContentHandler>(Intent(ctx, ContentHandler::class.java).putExtras(extras)).use { scenario ->
              val end = SystemClock.uptimeMillis() + 10_000
              fun page(a: ContentHandler) =
                  a.supportFragmentManager.fragments.filterIsInstance<ContentView>().singleOrNull { it.isResumed }?.view
              while (scenario.read { page(it) == null }) {
                  check(SystemClock.uptimeMillis() < end) { "no lyrics page" }
                  InstrumentationRegistry.getInstrumentation().waitForIdleSync()
              }
              scenario.onActivity {
                  val page = page(it)!!
                  assertPanel(page.findViewById(R.id.lyrics_simplified), "lyrics_simplified")
                  assertPanel(page.findViewById(R.id.lyrics_traditional), "lyrics_traditional")
                  assertPanel(page.findViewById(R.id.lyrics_english), "lyrics_english")
              }
          }
      }

      @Test
      fun mainScreenHintSitsOnThePanel() {
          // MainActivity may show its changelog dialog; the hint is still in the activity's own window
          ActivityScenario.launch(MainActivity::class.java).use { scenario ->
              scenario.onActivity { assertPanel(it.findViewById<TextView>(R.id.tv_hint), "tv_hint") }
          }
      }

      @Test
      fun pickerPhotoPreviewSampleSitsOnThePanel() {
          val intent = BackgroundPickerActivity.intent(ctx, BackgroundSlot.LYRICS)
          ActivityScenario.launch<BackgroundPickerActivity>(intent).use { scenario ->
              scenario.onActivity {
                  val grid = it.findViewById<GridView>(R.id.backgroundGrid)
                  val adapter = grid.adapter
                  val photoCell = adapter.getView(adapter.count - 1, null, grid)   // "your photo" is the last cell
                  assertPanel(photoCell.findViewById(R.id.bgSample), "picker photo sample")
                  val presetCell = adapter.getView(0, null, grid)
                  assertThat(presetCell.findViewById<View>(R.id.bgSample).background).isNull()
              }
          }
      }
  }
  ```

  說明：
  - 照片放在 `PhotoBackgroundImporter.photoFileIn(ctx.filesDir)`（App 私有目錄，不需要任何儲存權限；測試前後會覆蓋／刪除這個檔，只在模擬器上跑）。`GrantPermissionRule` 只保留 `MainActivity` 自己需要的權限。
  - `MainActivity` 啟動時若跳出系統權限對話框導致 `launch` 逾時，先查 `MainActivity.onCreate` 實際要求哪些權限，加進 `GrantPermissionRule`。
  - `GradientDrawable.getColor()` 是 API 24 起才有的方法，minSdk 剛好符合。

  Run: `./gradlew :hymnchtv:assembleDebugAndroidTest --console=plain`
  Expected: `BUILD SUCCESSFUL`（只編譯；V1 Step 0 才執行）。

- [ ] **Step 4：完整建置、測試、lint**

  ```bash
  ./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug :hymnchtv:lintDebug --console=plain
  for k in MissingTranslation ExtraTranslation HardcodedText UnusedResources; do
    printf "%s: " $k; grep -c "\[$k\]" hymnchtv/build/reports/lint-results-debug.txt
  done
  ls -l hymnchtv/build/outputs/apk/debug/*.apk
  ```

  Expected:
  - `BUILD SUCCESSFUL`，所有單元測試通過。
  - `MissingTranslation`、`ExtraTranslation`、`HardcodedText` 和 Task 0 的基準相同；`UnusedResources` 不高於基準。若 `UnusedResources` 增加，列出新增的項目並刪除真正沒用的資源（不要刪 `bgx_*`，它們由 layer-list 引用）。
  - APK 大小約比基準多 2.5～3 MB（字型約 +3.6 MB 未壓縮、舊 JPG −0.9 MB）。把數字寫進回報。

- [ ] **Step 5：Commit**

  ```bash
  git add -A hymnchtv/src/main/AndroidManifest.xml hymnchtv/src/main/res/values*/strings.xml \
          hymnchtv/src/test/java/org/cog/hymnchtv/reading/background/BackgroundResourcesTest.kt \
          hymnchtv/src/androidTest/java/org/cog/hymnchtv/reading/PhotoBackdropTest.kt
  git status --short   # 只能看到上面的檔案和 Step 2 的刪除
  git commit -m "refactor: remove the old wallpapers, the lyrics-language screen and unused strings" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
  ```

---

### Task V1：模擬器驗證（API 24 與 API 34）

**前置:** C1。這是唯一使用模擬器的 task；開始前先確認沒有其他代理正在用 `api24`／`api34`（`adb devices` 沒有其他人的 session），必要時和使用者協調時段。

- [ ] **Step 1：準備**

  ```bash
  EMU=/opt/homebrew/share/android-commandlinetools/emulator/emulator
  $EMU -avd api34 -no-snapshot-load &
  adb wait-for-device && adb shell getprop sys.boot_completed   # 等到印出 1
  ./gradlew :hymnchtv:installDebug --console=plain
  adb shell pm clear org.cog.hymnchtv
  SHOTS=/private/tmp/claude-501/a2-shots && mkdir -p $SHOTS
  ```

  截圖：`adb exec-out screencap -p > $SHOTS/<名稱>.png`。每一項檢查都存一張截圖，最後交給使用者。

- [ ] **Step 2：API 34 檢查清單**

  0. **Instrumented tests**：`./gradlew :hymnchtv:connectedDebugAndroidTest --console=plain`，`BackgroundDrawablesTest`、`ContentHandlerReadingTest`、`PhotoBackdropTest`（照片模式下 `lyrics_simplified`、`lyrics_traditional`、`lyrics_english`、主頁 `tv_hint`、挑選畫面照片格的 `bgSample` 都有底板）與 A 的 `ResourceLocaleResolutionTest` 全部通過。
  1. **全新安裝的預設**：App 預設是**淺色**主題 → 主頁「晨曦」、歌詞頁「宣紙白」；選單切到深色主題 → 兩處都是「夜讀」；先選過背景再切主題 → 保持使用者的選擇。主頁提示文字是背景的強調色。
  2. **入口**：主選單「閱讀設定」、歌詞頁長按簡繁鍵、長按模式鍵、歌詞頁長按選單的「閱讀設定」都能開啟同一個畫面。歌詞頁長按選單已沒有「預設顯示／隱藏播放器」。
  3. **顯示模式**：
     - 設定「只顯示詞」：沒有樂譜，按鈕列在最上面。
     - 歌詞頁按模式鍵循環三種模式，翻頁後仍保持；旋轉螢幕仍保持；返回主頁再進來，回到設定的預設值。
     - 「只顯示譜」：簡繁鍵和英文鍵隱藏，只剩模式鍵；不會跳出教唱提示。
     - 「只顯示詞」開一首沒有歌詞文字的詩歌（例如青年詩歌 182，`lyrics_yb_text/yb182.txt` 只有 26 字；若編號對應不同，就從目錄找到對應的那首）：要同時顯示樂譜，不會是空頁。
  4. **第 5 頁 bug**：大本 152（`db152` 有 5 頁）往下捲，5 張樂譜都不同，第 5 頁是 `db152d.png` 的內容，第 4 頁沒有被蓋掉。
  5. **字級**：選「特大」→ 歌詞變大；兩指縮放 → 改變；離開再進來 → 保持縮放後的大小；再選「小」→ 直向與橫向都回到 16 sp／28 sp。
  6. **字型**：「文楷」時簡體與繁體歌詞都是楷體（繁體看「說」「為」等字形是台灣字形）；改「系統字型」→ 恢復系統字型。
  7. **背景**：
     - 挑選畫面 21 格都能正常顯示，目前選的那格有勾選標記；截一張整頁圖。
     - 每一類至少實際套用一款到歌詞頁：宣紙白（紋理）、天光（漸層）、橄欖枝（右上角圖案）、麥穗（右下角）、葡萄樹（左下角）、活水（底部波浪）、光芒（放射線）、五線譜（拼貼）、星夜（星點）、墨色（深色紋理）。
     - 深色背景時樂譜自動變成「背景色的紙、文字色的音符」；長按選單「樂譜顏色」循環 4 次會回到自動。
     - 主頁和歌詞頁可以分別選不同的背景。
  8. **英文歌詞**：深色主題＋歌詞頁「宣紙白」時，英文歌詞是黑字；「夜讀」時是白字。
  9. **自訂照片**：挑選畫面選「自訂照片」→ 裁切 → 回來後該頁使用照片；歌詞、英文歌詞與主頁提示文字後面都有深色底板；分別用一張全白和一張全黑的照片測，字都清楚。變暗滑桿有效；模糊滑桿有效（API 34），換回預設背景後模糊消失。主頁和歌詞頁都能用照片。用 `adb shell` 刪除照片檔後重新開啟，回到預設背景，不會當機。
  9b. **只顯示詞與記憶體**：大本 152 在「譜＋詞」捲到最下面，切到「只顯示詞」再切回，5 張樂譜都重新出現且正確；切換前後各跑一次 `adb shell dumpsys meminfo org.cog.hymnchtv | grep -E "TOTAL|Graphics"`，「只顯示詞」時 Graphics 不應高於「譜＋詞」。
  10. **翻頁動畫**：關閉後翻頁是一般滑動，沒有縮小淡出。
  11. **播放器預設顯示**：關閉後進歌詞頁，播放器預設隱藏；長按選單「切換播放器」仍可用。
  12. **螢幕常亮**：`adb shell settings put system screen_off_timeout 15000`。開啟時歌詞頁超過 15 秒不熄螢幕；關閉後會熄。測完改回原值。
  13. **About**：有字型致謝那一行，點擊顯示 OFL 全文。
  14. **語言**：分別在簡中、繁中、英文介面開閱讀設定和挑選畫面，所有文字都是該語言，沒有英文殘留。
  15. **API 33+ 語言切換重建**：在歌詞頁按模式鍵切到「只顯示譜」，到系統設定改 App 語言再回來，模式仍是「只顯示譜」。
  16. **記憶體（B-5）**：主頁靜置後 `adb shell dumpsys meminfo org.cog.hymnchtv | grep -E "TOTAL|Graphics"`，記錄數字，並和 `feat/zh-hant` 的 APK 在同樣操作下的數字比較。

- [ ] **Step 3：API 24**

  關閉 api34，啟動 `api24`，重做第 0～8 項、第 9b～14 項。另外確認：
  - 「照片模糊」滑桿是停用狀態，說明是「需要 Android 12 以上」；變暗仍有效。
  - 文楷字型在 API 24 正常顯示（`ResourcesCompat` 的相容路徑）。

- [ ] **Step 4：回報**

  把每一項的結果（通過／失敗＋截圖檔名）、Step 2 第 16 項的記憶體數字、C1 的 APK 大小寫成清單交給協調者。有任何失敗就開 bug 修正 task，修完重跑相關項目；不要在 V1 裡直接改程式。

---

### Task R1：審查

**前置:** V1 全部通過。

- [ ] **Step 1：code-reviewer 子代理**

  派一個 code-reviewer 子代理審查 `git diff feat/zh-hant...feat/reading-settings`，重點：
  - 平行 lane 合併後有沒有重複或互相矛盾的程式（例如兩處都讀同一個 pref 但預設值不同）。
  - `ContentView` 整檔替換有沒有遺漏 A 的行為（簡繁切換、英文歌詞、教唱提示、縮放儲存）。
  - 背景與照片的記憶體：照片解碼尺寸、`StarsDrawable` 每次 `draw` 建立 `RadialGradient` 是否需要快取。
  - 錯誤處理：字型載入失敗、照片檔損毀、pref 型別錯誤都不能當機。

- [ ] **Step 2：`/codex review`**

  對同一個 diff 執行 `/codex review`。

- [ ] **Step 3：處理意見**

  P1 一律修正（每個修正一個 `fix:` commit，修完重跑 `./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug` 和受影響的 V1 項目）。P2 以下列給使用者決定。

- [ ] **Step 4：交給使用者**

  回報內容：完成的 task、V1 結果與截圖位置、審查意見與處理方式，以及本計畫「待確認」的三個問題。由使用者決定何時用 superpowers:finishing-a-development-branch 開 PR。

---

## 自我檢查（寫計畫時已確認）

- 主計畫 A2 表格的每一列都有對應 task：顯示模式（P1、I2）、預設字級（P2、P4、S1、I2）、翻頁動畫（I1）、播放器預設顯示（S1、I1）、螢幕常亮（I1）、字型（F1～F3、I2）、背景（B1～B3、S2、I1、M1）。
- 主計畫的三個待決問題在 D2 有答案；B-5 由 C1 解決；`showLyricsScore` 第 5 頁 bug 由 P3（測試）與 I2（修正）處理。
- 平行 lane 的檔案範圍互不重疊：`build.gradle` 與所有字串只在 Task 0 修改；`AndroidManifest.xml` 只在 S1、S2、C1 修改，三者在不同階段。
- 型別與名稱一致性：`ReadingPalette` 的欄位（`textColor`、`accentColor`、`isDark`、`paperColor`、`backdropColor`）、`BackgroundSlot.prefKey`、`PhotoBackground.PREF_DIM／PREF_BLUR`、`ReadingPrefKeys` 的 key 字串與 `reading_preferences.xml` 一致，並由 `ReadingPreferencesXmlTest` 檢查。
- rev 2：使用者三項決策對應 P5＋M1（淺色預設）、D4＋各 pref 測試（文楷預設）、F0＋`FontSubsetTest`（修正私用區字元、無例外）；Codex 意見對應 D5＋`PhotoPaletteTest`（照片底板）、STARRY 疊層＋`StarsDrawable`（星點納入對比）、`FontSubsetTest`（涵蓋 strings）、I1（margin）、I2（`Glide.clear`、比對當下 `feat/zh-hant`、`ContentHandlerReadingTest`）、B3（`BackgroundDrawablesTest`）、F1／F2（以 manifest 取代 TTF 解析器，刪除工具雜湊）。
