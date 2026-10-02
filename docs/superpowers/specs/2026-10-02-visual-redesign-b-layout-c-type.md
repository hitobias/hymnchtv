# 首頁與歌詞頁視覺重設計（B 版面＋C 字形）

> 狀態：**rev 10 定案**（2026-10-03）。Codex 第 9 輪 P1=0；其 P2×2、P3×1 已併入（播放卡位置與歌詞 inset、Aa 主題圓點尺寸與 320dp 捲動、不做項措辭）。前次：rev 9（2026-10-03）。rev 8 Codex：P1×1（頂列 320dp 容納不下 8 鈕）、P2×1 已修正。前次：rev 8（2026-10-03）。rev 7 Codex：P1×1、P2×2、P3×1 已修正（預覽卡高度改以內容量測並重算預算、歌詞層底部 inset、目錄鈕文字 token、照片截圖 fixture）。前次：rev 7（2026-10-03）。rev 6 Codex：P1×2、P2×1、P3×1 已照建議修正（TokenInput 明列 isPhoto、作用中／停用文字門檻分開、accent 對所有相鄰底取最差、閱讀色調整需同步更新表與測試）。前次：rev 6（2026-10-03）。rev 5 Codex：P1×2、P2×1 已併入（鍵盤下限統一 48dp、停用狀態改用 token 不再整體 alpha、rasterize 測試改為逐像素合成後驗證）。前次：rev 5（2026-10-03）。rev 4 Codex：P1×3、P2×2 已併入（outline 動態 ≥3:1、免捲動改為「開啟鍵首屏可見」並附高度預算、swatches 窮舉 overlay 組合＋插樁 rasterize 驗證、只有譜模式按鈕停用、英文標籤定稿）。前次：rev 4（2026-10-03）。rev 3 Codex：P1×3、P2×2 已併入（目錄鈕改實底、免捲動保證改為 360×800、下方三鈕定稿短標籤、Aa 套用以 view 生命週期為準、首頁 applyHomeTheme）。前次：rev 3（2026-10-02 深夜）。rev 2 Codex：P1×3、P2×2 已併入（§4 前景色全矩陣、§6b 即時套用路徑、§6c 播放卡規則簡化與疊放版面、§7 Aa 測試）。前次：rev 2（2026-10-02 深夜）。rev 1 Codex：P1×2、P2×4、P3×2，已全部併入；另依使用者要求加入微信讀書風格的「閱讀色」主題、「Aa」快速面板與歌詞頁工具列自動隱藏（§6a–§6c）。依據：使用者真機回饋（1.1.0-preview1 截圖）與三方向比較頁 https://claude.ai/artifact/N5UwDxyHKAyKdpLxH5DnUC ，使用者選定「B 的版面、C 的字形」。
> 範圍：C 子項目 1.1（H3 首頁已實作於 `feat/c-home-search`）之上的視覺層修訂；不改互動規則（spec `2026-10-02-home-entry-and-lyrics-jump-design.md` 仍有效）。

## 1. 使用者回饋（必須解決）

1. 首頁：第二列詩歌本顯示「…」、「目錄›」被擠成「目›」；詩名是簡體；「開啟」鍵深藍底配深色字幾乎看不見；鍵盤區與下方大片空白、佈局鬆散；整體「沒有高級感」。
2. 歌詞頁：各種按鈕（頂列譜色、A-、A+、分享、媒體、下一首、⋮；下方簡-繁、中英、模式；播放卡的媒體／教唱／唱詩／伴奏、連播、速度）背景與文字對比太低，閱讀困難；按鈕底色和整體背景不搭。

## 2. 設計原則

- **B 版面**：卡片與色塊層次清楚；按鍵看得出可以按；同類元件同尺寸、同圓角、同內距。
- **C 字形**：號碼與詩名大而粗；詩歌本名稱與詩名用楷體粗體；按鍵數字粗體。
- **配色從背景來**：所有表面、按鈕、文字顏色由目前背景的 `ReadingPalette`（A2：`reading/background/BackgroundPolicy.kt`，含照片背景的 `PHOTO_PALETTE`）推導，不寫死；換背景、切深淺色時一起變。
- **可讀性是硬性要求**：**作用中**文字對背後實際表面 ≥ 4.5:1（WCAG AA，大字亦以 4.5 為目標）；**停用**元件文字 ≥ 3:1（WCAG 1.4.3 非作用中元件例外）；按鈕外框、圖示、選中狀態對相鄰表面 ≥ 3:1。用既有 `reading/background/Wcag.kt` 計算並寫成自動測試。

## 3. 字形（C）

| 角色 | 字型 | 大小／粗細 |
|---|---|---|
| 號碼（預覽卡） | 系統 sans（Roboto／Noto Sans），`tabular-nums` | 40sp 粗體（360dp 以上）；320dp 寬時 34sp |
| 本名（預覽卡上方小標） | `hymnal_kai_*` | 14sp |
| 詩名（預覽卡） | `hymnal_kai_*`（依歌詞語言選 sc/tc） | 20sp 粗體，最多 2 行，超出省略 |
| 詩歌本格 | `hymnal_kai_*` | 16sp 粗體；英文介面用短標籤 15sp |
| 鍵盤數字 | 系統 sans | 26sp 粗體 |
| 鍵盤功能鍵（附、⌫） | 附用 `hymnal_kai_*` 18sp；⌫ 用圖示 24dp | |
| 開啟鍵 | 系統 sans | 17sp 粗體 |
| 最近 chip | 號碼 14sp 粗體；日期 11sp | |
| 歌詞頁按鈕標籤 | 系統 sans | 14sp 中粗（500） |

- 詩名內容依歌詞語言讀對應資產：繁體時讀 `lyrics_*_text_hant_*`（已由 `fix/hant-titles` 的 `LyricsScript.hantVariant` 實作，本設計沿用，缺檔才明確退回簡體）；楷體字型 sc/tc 依同一決策選擇。
- production 以 `Paint.hasGlyph()` 逐 code point 檢查，任一字缺失即整段改用系統字型（不得顯示豆腐字）；以假的 `HymnTitleSource` 輸出缺字字串的測試覆蓋此分支。
- 系統字級放大（1.3）時版面仍不重疊；允許整頁捲動。

## 4. 色彩 token（由 ReadingPalette 推導）

新增純函式 `UiTokens.from(input: TokenInput): UiTokens`（Kotlin，可單元測試）。`TokenInput` 為不可變輸入：`textColor`、`accentColor`、`baseColor`、`isDark`（取實際背景的明暗，**不**取 DayNight——使用者明選的淺色背景在深色模式下仍是淺色）、`swatches: List<Int>`（背景可能出現的代表色。preset：對每個漸層色停點，窮舉該 preset 所有 overlay 的組合（每層取 0 與最大 alpha，n 層共 2ⁿ 種，現有 preset n ≤ 3）依繪製順序合成後的所有結果；照片：`PHOTO_PALETTE` 經既有 dim 後的黑端點與白端點）。另加插樁測試：把每個 preset 的實際 drawable rasterize 成 108×192 bitmap，對**每個像素**先套用與正式 UI 相同的 `surface`／`surfaceTone`／`disabledSurface` 合成，再直接驗證 `onSurface`、`onSurfaceMuted`、`disabledOnSurface`、`onOutlineAction`（對合成後 `surface` ≥ 4.5:1）、`accent`、`outline` 的對比門檻，並驗證 `onAccent` 對 `accent` ≥ 4.5:1（不以背景亮度範圍作為涵蓋證明）、`isPhoto: Boolean`（`BackgroundChoice.Photo` 時為 true，決定 `surface` 走照片分支）。由 `BackgroundPolicy` 新增 `tokenInput(choice)` 產生（`ReadingPalette` 保留現有欄位不動，避免波及 A2）；preset 由新增的 `BackgroundPreset.swatches()` 提供。DayNight 只決定「未設定背景時的預設 preset」。下表中「淺色背景／深色背景」一律指 `input.isDark`。輸出：

| token | 用途 | 推導 |
|---|---|---|
| `surface` | 卡片、預覽卡、最近 chip、搜尋框 | 淺色背景：`baseColor` 與白混合 92%，alpha 0.92；深色背景：`baseColor` 與白混合 10%，alpha 0.92；照片背景：`#1e1e1e` alpha 0.88 |
| `surfaceTone` | 鍵盤按鍵、詩歌本未選中格、歌詞頁一般按鈕 | `surface` 再向 `textColor` 混合 8%（淺）／12%（深） |
| `onSurface` | 上述表面上的文字 | 候選依序為 `textColor`、黑、白；對**每個** swatch 合成後的 `surface`、`surfaceTone` 與頂列底板（皆為 `onSurface` 實際會出現的底）計算對比，取最差值；選最差值最高的候選，且最差值必須 ≥ 4.5:1，否則提高 `surface`／`surfaceTone` 的不透明度（上限 1.0）後重算 |
| `onSurfaceMuted` | 次要文字（本名小標、日期、⌫） | `onSurface` alpha 0.72；以同一全矩陣檢查，不足 4.5:1 逐步提高到 1.0 |
| `accent` | 選中詩歌本、開啟鍵底色、歌詞頁選中狀態 | `palette.accentColor`；對所有實際相鄰底（每個 swatch 合成後的 `surface`、`surfaceTone`、頂列底板）取最差值，不足 3:1 時加深（淺色）或提亮（深色）直到 ≥ 3:1 |
| `onAccent` | 開啟鍵與選中格上的文字 | 黑或白中對 `accent` 對比較高者，且必須 ≥ 4.5:1；不足時再調整 `accent` 明度直到成立 |
| `disabledSurface` | 停用按鍵底色 | `surface` 本身（不加 tone），以「平面化」區別可按的 `surfaceTone` |
| `disabledOnSurface` | 停用按鍵文字與圖示 | `onSurface` 向 `disabledSurface` 混合；停用元件依 WCAG 1.4.3 非作用中元件例外不要求 4.5:1，但本設計仍要求全矩陣 ≥ 3:1 以保可讀；另以 `outline` 虛線外框作為非顏色的停用提示 |
| `onOutlineAction` | 目錄鈕文字（`surface` 實底上） | 起始為 `accent`；對每個 swatch 合成後的 `surface` 取最差值，不足 4.5:1 時加深／提亮直到 ≥ 4.5:1 |
| `outline` | 按鈕外框（目錄鈕）、照片背景上的卡片邊線 | `onSurface` 起始 alpha 0.24，對每個 swatch 合成後的相鄰表面不足 3:1 時逐步提高 alpha（上限 1.0）；納入 §7 矩陣 |

- 「表面」是半透明卡片疊在背景上；對比計算一律先用 `Wcag.blend()` 把表面合成到**每個** swatch 上，再取最差比值。
- 照片背景沿用 `PHOTO_PALETTE`（已證明 85% #1e1e1e 面板上 AA）。
- 不再使用硬編碼的 Material 預設藍（截圖中的開啟鍵 #4a6491 類色）。

## 5. 首頁版面（B）

由上而下（直向）：

1. **搜尋框**：高 48dp、圓角 24dp、`surface`；左側放大鏡圖示、提示「搜尋詩名或歌詞」。
2. **詩歌本格**：2 列 × 4 格，**兩列都是 4 等欄**（現行 `hymn_picker.xml` 第二列「3 本＋寬 2 格目錄」改為 4 等欄，橫向版同步），間距 6dp，每格高 44dp、圓角 12dp；順序「大本、補充、新歌、新詩／青年、兒童、英文、目錄›」。未選中 `surfaceTone`＋`onSurface`；選中 `accent`＋`onAccent`；「目錄›」用 `surface` 實底＋`onOutlineAction` 字＋`outline` 邊線（不得透明底：首頁背景可能是照片，透明底無法保證對比），與詩歌本同尺寸對齊、以邊線與字色區隔。`onOutlineAction` 對 `surface` 的文字對比納入 §4 全矩陣（≥ 4.5:1）。
   - 中文標籤固定兩字；英文介面用定稿短標籤（取代現行 `English`、`Contents`）：`Hymns`、`Suppl`、`NewSg`、`NewHy`、`Youth`、`Child`、`Eng`、`Index`（目錄鈕另以 › 圖示，不計入文字）；列入 ellipsize 插樁測試。任何寬度下不得出現省略號。
3. **預覽卡**：`surface`、圓角 16dp、內距 14dp；左側上方本名小標、下方大號碼；右側詩名（楷體粗體，靠右，最多 2 行）。無效號碼時右側顯示狀態訊息（「此本無第 N 首」與「此號亦見於」chip），沿用 H3 的 live region。
4. **鍵盤**：3 × 4，間距 8dp；每鍵 `surfaceTone`、圓角 14dp，高度 = 剩餘空間平均分配，偏好 56dp、硬下限 48dp、最大 72dp（填滿螢幕高度，不留大片空白；空間不足時才降到 48dp）；停用鍵用 `disabledSurface`＋`disabledOnSurface`（見 §4），不對整個 View 設 alpha，並保留 contentDescription 說明原因（H3 已有）。
5. **開啟鍵**：高 52dp、圓角 14dp、`accent` 底、`onAccent` 字「開啟 ›」；停用時 `surfaceTone` 底＋`onSurfaceMuted` 字。
6. **最近**：標題列「最近」＋右側「全部記錄 ›」；下方橫向 chip（`surface`、圓角 10dp），兩行：本名號碼（粗）／相對日期（今天時間、昨天、星期、M/d）。
7. 底部導覽列維持現狀（首頁、目錄、設定），顏色改用 token。

- 間距：外側左右 16dp；區塊間 10dp。
- 鍵盤每鍵最小 48dp（觸控下限，唯一下限值）、最大 72dp；「最近」列為單行 chip，高度 52dp。
- 首頁內容區整頁可捲動。保證：在內容區高度 ≥ 580dp 的螢幕（約 360×740 以上，已扣狀態列、56dp 工具列與底部導覽）**「開啟」鍵在首屏可見**；「最近」列可在首屏下方。高度預算（最小值）：上內距 12＋搜尋 48＋間距 10＋詩歌本 94（2×44＋6）＋10＋預覽卡 96（量測：內距 28＋本名 14sp 行高約 20＋號碼 40sp 行高約 48；實作以實際量測為準，不設固定高度）＋10＋鍵盤 216（4×48＋3×8）＋10＋開啟 52 ＝ 558dp。保證門檻相應改為內容區 ≥ 580dp（約 360×740 以上）；系統字級放大（例如 1.3）時不保證首屏，允許整頁捲動，但元件不得重疊或裁切。剩餘高度依序分配給鍵盤（至 72dp）與「最近」列。
- 橫向：沿用 H3 的雙欄（左：搜尋、詩歌本、預覽、最近；右：鍵盤、開啟），套用同樣元件樣式。
- 首頁背景（A2 首頁背景設定）照常顯示在整頁後方；卡片半透明疊上。

## 6. 歌詞頁按鈕（同一套 token）

- **頂列內容（定稿）**：任何寬度只放 5 個：分享、媒體、Aa、下一首、⋮（5×48dp＝240dp，320dp 含內距可容納）。A-／A+ 移除（字級改由 Aa 面板滑桿調整）；譜色移入 ⋮ 溢出選單（與閱讀設定、顯示／隱藏播放條、英文歌詞、說明、主頁同列）。不使用水平捲動工具列。TalkBack 焦點順序依畫面左到右。
- **頂列樣式**：按鈕為 `surfaceTone` 圓角 12dp 色塊、高 40dp（觸控區 ≥ 48dp）、`onSurface` 14sp 500；選中／開啟狀態（例如譜色啟用）用 `accent`＋`onAccent`。頂列整條底板 `surface`，讓按鈕不直接壓在歌詞紙紋上。
- **下方三鈕**：與頂列同樣式，三等欄，14sp 單行，320dp 不得截斷或換行。標籤定稿（動態者依目前狀態）：

| 按鈕 | 繁中 | 簡中 | 英文 |
|---|---|---|---|
| 簡繁 | 簡-繁 | 简-繁 | Simp-Trad |
| 中英 | 中英 | 中英 | CN/EN |
| 模式（譜＋詞／只有詞／只有譜） | 譜詞／詞／譜 | 谱词／词／谱 | Both／Text／Score |

  （contentDescription 保留完整說明，例如「切換簡體與繁體歌詞」。）
  - 「只有譜」模式下，簡繁與中英兩鈕保留位置但停用（`disabledSurface`＋`disabledOnSurface`，contentDescription 說明「只有譜模式不適用」），三等欄版面不變。
- **播放卡**：卡片 `surface`；媒體來源單選（媒體／教唱／唱詩／伴奏）改為分段按鈕樣式：未選 `surfaceTone`＋`onSurface`，選中 `accent`＋`onAccent`；「連播」CheckBox 與速度按鈕用 `onSurface`；SeekBar 進度色 `accent`、軌道 `onSurfaceMuted` alpha 0.3。
- **首頁**：`HomeFragment` 以 `MAIN` 背景槽位建立自己的 `UiTokens`，經 `applyHomeTheme()` 套到首頁元件（取代 `HomeAppearance` 的硬編碼部分）；與歌詞頁的 `LYRICS` 槽位分開，背景變動時各自依自己的槽位重建，不得共用同一份 token。
- **歌詞頁單一傳播點**：`ContentHandler` 在套用 `LYRICS` 槽位背景時建立一次 `UiTokens`，明確呼叫 `ContentView`、頂列、下方三鈕與 `MediaGuiController.applyTokens(tokens)`；設定頁返回、Aa 面板改主題、activity 重建都走同一入口 `applyReadingTheme()`。`MediaGuiController` 移除對 Material `colorOnSurface` 與硬編碼 `Color.GRAY` 的依賴。
- 不改按鈕的功能與位置（H5 延後；F1 已移除長按）。

## 6a. 閱讀色主題（使用者要求「色彩主題多一些」，參考微信讀書）

- 現有 20 個背景預設全部保留，且因 §4 每個都成為完整主題（卡片、按鍵、開啟鍵、選中格、歌詞頁按鈕都隨之變色）。
- 新增類別 `READING`（「閱讀色」），8 個純色、低飽和、無圖案的預設，排在背景選擇器與 Aa 面板最前（初值；實作時若需調整，PR 必須同步更新本表與對比測試的期望值）：

| id | 名稱 | 底色 | 文字 | 重點 | 明暗 |
|---|---|---|---|---|---|
| `paper_white` | 純白 | #ffffff | #222222 | #2f5d8a | 淺 |
| `parchment_beige` | 米黃 | #f5ecd7 | #3b3226 | #8a5a1f | 淺 |
| `eye_green` | 豆沙綠 | #cfe8cf | #1f2e22 | #2f6b3d | 淺 |
| `pale_blue` | 淺藍 | #e3edf6 | #1e2a36 | #2c5f93 | 淺 |
| `pale_pink` | 淡粉 | #f7e8ea | #2e2326 | #9b3b55 | 淺 |
| `soft_grey` | 淺灰 | #ececec | #262626 | #4a4f57 | 淺 |
| `dim_grey` | 深灰 | #2b2b2d | #d8d8da | #9fc2ff | 深 |
| `true_black` | 純黑 | #000000 | #b8b8b8 | #e3b77a | 深 |

- `BackgroundPreset.isDark` 改為依明暗欄位（不再只看 `category == NIGHT`）；更新所有 exhaustive `when`、資源映射與現行「20 個／各類數量」測試（`BackgroundPresetTest`）。偏好存穩定 id，既有使用者設定不受影響。
- 背景選擇器縮圖改為「背景＋縮小的卡片與按鈕列」預覽。
- 預設值不變。

## 6b. 「Aa」快速面板（參考微信讀書）

- 歌詞頁頂列新增「Aa」按鈕，開啟 BottomSheet（不離開歌詞頁），內容由上而下：
  1. **主題**：一排圓點（視覺直徑 32dp、觸控區 48dp、間距 8dp；寬度不足時該列水平捲動，起始捲到目前選中的圓點；「更多…」為列尾的文字按鈕）（閱讀色 8 個在前，其後「更多…」開完整背景選擇器）；點選立即套用到歌詞頁（經 §6 `applyReadingTheme()`），選中圓點有勾與外圈。
  2. **字級**：滑桿（沿用 A2 `LyricsFontSize` 的級距）。改動時必須呼叫 `ReadingPrefs.resetLyricsScale()`（清除已存的橫直向 pinch scale，否則 enum 改了畫面不變），並立即套用到目前頁；相鄰頁若 view 已建立（以 `viewLifecycleOwner` 判斷）一併套用，未建立者在 `onViewCreated` 讀取最新偏好（字型、模式同理），不得對已銷毀或尚未建立的 View 操作。
  3. **字型**：楷體／系統字（沿用 A2 `LyricsTypefaces`）。新增可在現頁執行的 `ContentView.applyReadingPrefs()`（含非同步字型載入完成後套用），不得只在 `ContentView` 建立時生效。
  4. **顯示模式**：沿用 A2 `DisplayMode`。選擇時清除 session 的 `displayModeOverride` 並刷新目前頁。
  5. 「閱讀設定 ›」：開完整設定頁。
- 面板改的是與閱讀設定同一組偏好（同一 key），兩邊一致。
- 首頁主題由「設定 → 閱讀設定 → 首頁背景」選擇（同一套預設，含閱讀色）；不在首頁加面板。

## 6c. 歌詞頁工具列自動隱藏（使用者同意的折衷方案）

- **頂列**：進入歌詞頁先顯示 3 秒後淡出（首次使用另顯示提示「點一下畫面可顯示工具列」，只顯示一次，記在偏好）；之後**點畫面中央區域**（寬度中間 60%、高度中間 60%）切換顯示；顯示後 4 秒無操作自動淡出（操作頂列任何按鈕會重新計時；開著 Aa 面板或溢出選單時不淡出）。
- **播放卡**：不參與自動隱藏——播放卡一旦顯示（使用者開啟了播放列或正在使用媒體）就維持顯示，直到使用者用既有「顯示／隱藏播放條」關閉；自動隱藏只作用於頂列與下方三鈕。如此不需判斷各播放器（音訊、ExoPlayer、YouTube）的播放／暫停狀態。
- **下方三鈕**（簡-繁、中英、模式）：隨頂列顯示／隱藏。
- 點擊判定：只認單擊（移動 < touch slop、時間 < long-press 時限），任何捲動或左右翻頁手勢都不觸發；與 `fix/lyrics-swipe` 的手勢判斷共用，不得互相干擾。
- **無障礙**：TalkBack（`AccessibilityManager.isTouchExplorationEnabled`）開啟時頂列、下方三鈕、播放卡一律常駐。
- 淡入淡出 150ms；系統「移除動畫」時直接顯示／隱藏。
- **版面**：`content_lyrics.xml` 由垂直 `LinearLayout` 改為可疊放的 `FrameLayout`（或 `ConstraintLayout`）：歌詞捲動內容為底層並占滿，頂列與下方三鈕為 overlay；隱藏時歌詞即延伸到原頂列位置；顯示時頂列浮在歌詞上方並使用 `surface` 底板。
- **疊放順序（由下而上）**：歌詞捲動層 → 頂列 overlay → 播放卡 overlay（貼齊底部）→ 下方三鈕 overlay（緊貼在播放卡上方；播放卡未顯示時貼齊底部）。
- **歌詞層底部 inset**：歌詞 `ScrollView` 的底部 padding ＝（播放卡顯示中的高度，未顯示為 0）＋（下方三鈕顯示中的高度，隱藏為 0）＋系統導覽安全區（`WindowInsets`）＋8dp，任一者顯示／隱藏或高度改變時即時更新，讓最後一行可完整捲到按鈕上方；三鈕隱藏時縮回為安全區＋8dp。頂列顯示時同理加頂部 padding（不讓第一行被遮住）。
- **控制者**：由 activity 層（`ContentHandler` 內新增的 `ChromeController`）統一計時與顯示狀態，作用於目前 pager 頁的 overlay；各 `ContentView` 不各自計時；換頁時新頁沿用目前顯示狀態。
- 測試：點中央顯示、4 秒後隱藏（以可注入的時鐘測試）、捲動與翻頁不觸發、播放卡顯示時不被自動隱藏、換頁沿用顯示狀態、TalkBack 常駐、首次提示只出現一次。

## 7. 驗收

1. **自動對比測試**（單元，純函式）：矩陣為「每個 `BackgroundPreset`（含新閱讀色）」＋「`PHOTO_PALETTE` 黑白端點」＋「未設定槽位時的淺色／深色預設」；對每個 swatch 合成表面後，`UiTokens` 的每組（作用中文字／表面，含 `onAccent`／`accent`、`onOutlineAction`／`surface`）≥ 4.5:1、（停用文字／停用表面）≥ 3:1、（選中、外框／相鄰表面）≥ 3:1。
2. **插樁測試**：首頁七本＋目錄格文字未被 ellipsize（檢查 `Layout.getEllipsisCount`）於 320dp 英文與中文；開啟鍵文字色與底色對比 ≥ 4.5:1（讀實際 View 顏色）；換背景後首頁卡片顏色改變。
3. **截圖**：首頁與歌詞頁 × {320×640、360×720} × {淺色預設背景、深色預設背景、照片背景（固定測試圖片 `hymnchtv/src/androidTest/assets/test_photo_bg.jpg`，以 `PhotoBackgroundImporter` 的測試入口匯入，圖片需含大片亮區與暗區）} × {中文繁、英文}，外加字級 1.3 與橫向各一張；全部存 scratchpad 供協調者檢視。
4. **Aa 面板插樁測試**：主題、字級、字型、顯示模式四項在面板內改變後，目前頁立即反映（字級在有 pinch scale 的情況下仍生效、顯示模式在有 override 時仍生效），且寫入與閱讀設定頁相同的偏好 key（關閉面板後開設定頁顯示相同值）。
5. 既有測試全過（單元、api24b、api34b 全套）。

## 8. 不做

- 除本規格明列的調整（移除 A-／A+、新增 Aa 面板、譜色移入溢出選單、工具列自動隱藏、閱讀色）外，不改互動流程、不加新功能。
- 不重產字型子集（除非 §3 的詩名檢查證明必要，需先回報）。
- 不動 H5 範圍（歌詞頁跳轉）。
