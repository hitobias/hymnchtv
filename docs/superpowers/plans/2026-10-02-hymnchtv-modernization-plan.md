# hymnchtv 現代化計畫：繁中、效能、介面、使用體驗

- 日期：2026-10-02（rev 4：已納入四輪 Codex 審查的意見、使用者觀點的建議，以及決策 D2「API 33+ 支援系統 App 語言頁」）
- 基準版本：v2.9.2（versionCode 209020），分支 `master` @ 5e559b1
- 狀態：**草案，待審查，尚未實作**

## 0. 範圍與拆分

使用者需求：

1. 效能優化：手機上開啟時會卡頓
2. 介面語言新增繁體中文，並可設為預設
3. 歌詞可選預設語言，新增繁中選項
4. 介面現代化，參考大廠設計
5. 必要時可用 Kotlin 改寫
6. 站在使用者的角度改善 app

以上需求互相獨立、規模差異大，拆成 4 個子項目依序進行。每個子項目各自走「設計 → 計畫 → 實作 → 驗證 → 審查」，一個子項目一個分支、一個 PR。

| 子項目 | 內容 | 分支 | 規模 | 計畫成熟度 |
|---|---|---|---|---|
| A | 介面繁中、歌詞預設語言、搜尋安全性、螢幕常亮 | `feat/zh-hant` | 小～中 | **可執行**（已處理 Codex 的 P1） |
| Z | 新身分發佈：新的 applicationId、停用或改寫內建更新服務（目前指向原作者 cmeng-git）、About 頁的連結與版權資訊、簽章 | `chore/new-identity` | 小～中 | 只有大綱，使用者在 2026-10-02 決定要做；排在第一次正式發佈之前 |
| S | 帳號與同步（之後再做，見下方大綱） | — | 中～大 | 只有大綱 |
| A2 | 閱讀設定：顯示模式（譜／詞／兩者）、歌詞預設字級、關閉翻頁動畫、播放器預設顯示與螢幕常亮開關 | `feat/reading-settings` | 小～中 | 只有大綱，開工前要先設計並經 Codex 審查 |
| B | 效能優化 | `perf/startup-and-jank` | 中 | 熱點已找出，但 P1 尚未處理，開工前要補完（見 B.3） |
| C | 介面現代化 | `feat/material3-ui` | 大 | 只有大綱，需要另做視覺設計與 spec |
| D | 使用體驗功能（收藏與歌單、鎖屏播放控制等） | 依功能分開 | 中～大 | 只有大綱，需確認範圍 |

已確認的決策：

- 執行順序：A → A2 → B → C（A2 是使用者在 2026-10-02 決定新增的，接在 A 之後，因為兩者都會改到 `ContentView`）。D 的各項功能可以在 C 之後做，也可以和 C 穿插進行，等 D 範圍確認後再排。
- 首次安裝時，介面語言**跟隨系統**：系統為繁中（台／港／澳或 `zh-Hant`）時顯示繁中，簡中顯示簡中，其他語言顯示英文。使用者手動選擇後就記住。
- 歌詞預設語言是**獨立設定**，選項有「跟隨介面（預設）／簡體／繁體」。歌詞頁的「簡／繁」按鈕只切換當下的檢視。

### 專案現況（影響本計畫的事實）

- **沒有任何測試**：沒有 `src/test`，也沒有 `src/androidTest`。
- **Kotlin**：AGP 9.3.3 內建 Kotlin 支援，`:android-youtube-player:core` 已經有 `.kt` 檔，而且沒有額外套用 kotlin plugin。因此 `:hymnchtv` 可以直接新增 Kotlin 檔，和 Java 混用。
- **語系**：
  - `res/values` 是簡中（預設語言），`res/values-en` 是英文。
  - `HymnsApp.attachBaseContext` 讀取 `PREF_LOCALE`（沒有值時寫死成 `zh-Hans-CN`），再交給 `LocaleHelper.setLocale` 包裝 context。
  - `BaseActivity.attachBaseContext` 用的是 `LocaleHelper` 的靜態欄位 `mLanguage`。
  - 切換語言時會呼叫 `doRestart()`，也就是結束 process 再重新啟動。
  - `initLanguage()`（`MainActivity.java:1055`）無條件隱藏 `appLanguage`（系統 App 語言設定的入口），只顯示 `appLocale` 子選單（中文／English）。
  - `LocaleHelper.getAppLanguage()` 有 bug：它回傳的是 `getDisplayName()`，不是 BCP-47 tag。目前沒有任何地方呼叫它。
  - `androidResources.generateLocaleConfig = true`。手寫的 `res/xml/locale_config.xml` 內容是 `zh-rCN` 和 `en`。
  - `HymnsApp.java:106-109` 會先建立一個 WebView，註解寫明目的是「防止 WebView 把 UI locale 重設成系統預設」。
- **歌詞**：
  - 原始歌詞只有簡體版。繁體由 `ChineseConverter.convert(text, ConversionType, ctx)` 透過 OpenCC JNI 即時轉換。
  - `ContentView` 用 `PREF_SIMPLIFY` 決定顯示簡體或繁體，`button_ts` 每按一次就改寫一次這個 pref。
  - `PREF_CONVERSION_TYPE` 用 `ConversionType.valueOf()` 讀取（`ContentView.java:189, 603`、`ChineseS2TSelection.java:53`）。遇到非法值會直接丟例外而當機。
- **搜尋**：
  - `MainActivity.java:336` 在開啟 `ContentSearch` 之前，**已經**把查詢字串做 T2S 轉換。
  - 但 `ContentSearch.java:321` 用 `Pattern.compile(sString.replace("他", "[祂|他]"))` 直接把使用者輸入當成正規表達式。輸入 `(`、`[`、`*` 這類字元可能丟出 `PatternSyntaxException`，或改變搜尋語意。另外 `[祂|他]` 這個字元類別裡多了一個 `|`。
- **螢幕常亮**：程式碼裡沒有 `FLAG_KEEP_SCREEN_ON` 或 `keepScreenOn`，唱詩唱到一半螢幕可能會變暗。

---

## 子項目 A：介面繁中、歌詞預設語言、搜尋安全性、螢幕常亮

### A.1 設計

#### A.1.1 語言模型（Kotlin，純邏輯，可以用 JVM 測試）

`org.cog.hymnchtv.locale`

```kotlin
enum class AppLanguage(val tag: String?) {
    SYSTEM(null), ZH_HANS("zh-Hans-CN"), ZH_HANT("zh-Hant-TW"), EN("en-US");
    companion object {
        /** null、空字串、非法值 → null（交給呼叫端決定 fallback）。絕不丟例外。 */
        fun fromTag(v: String?): AppLanguage?
    }
}

object LanguageResolver {
    /** SYSTEM 解析為實際 Locale；其他直接回傳。 */
    fun resolve(lang: AppLanguage, system: Locale): Locale
    // zh + (script=Hant 或 region ∈ {TW,HK,MO}) → zh-Hant-TW（system 是 zh-HK/MO 時保留該 region）
    // zh 其他 → zh-Hans-CN；其他語言 → en-US
    fun isTraditional(locale: Locale): Boolean
}
```

#### A.1.2 語言來源：依 API 等級擇一，不做雙向同步（使用者決策 D2：支援系統的「App 語言」設定頁；處理 Codex P1-2 與 rev 2 的新 P1）

**原則：每個 API 等級只有一個真實來源，不在兩份資料之間互相同步。** 因此不需要 synced tag，不需要 reconcile 狀態機，也不必處理寫入順序。

| API 等級 | 唯一來源 | 讀取 | 寫入（app 內選單） | 生效方式 |
|---|---|---|---|---|
| ≥ 33 | framework 的 per-app locale | `LocaleManager.getApplicationLocales()`：空清單表示 `SYSTEM`，否則取第一個 tag | `LocaleManager.setApplicationLocales(...)`，選 `SYSTEM` 時傳空清單 | framework 會自行套用設定並重建 Activity，**不呼叫 `doRestart()`** |
| < 33 | `PREF_LOCALE` | pref | `commit()` 寫入 pref | 沿用既有的 `doRestart()` |

- **在 API 33 以上，app 內選單和系統設定頁改的是同一份資料**，所以兩邊不可能不一致。`PREF_LOCALE` 在 API 33 以上不會被使用。
- **讀取函式**：`LocaleStore.current(): AppLanguage`，介面放在 `LocaleStore`，依 API 等級提供兩種實作，方便單元測試時替換成假資料。
  - 對應規則寫成純函式 `AppLanguage.fromFrameworkTags(tags: List<String>): AppLanguage`：
    - 空清單 → `SYSTEM`
    - zh-Hant、TW、HK、MO → `ZH_HANT`
    - 其他 zh → `ZH_HANS`
    - en → `EN`
    - **不支援的語言（例如 `ja`）→ `EN`**。只用於顯示和選單打勾，**不會回寫 framework**。
  - 系統設定頁只會列出 locale config 裡的語言（zh-Hans、zh-Hant、en），所以正常操作不會出現不支援的語言，只有透過 adb 之類的方式才會。
- **context 包裝**（rev 3 Codex P1，依官方文件，第三方 app 應把 `LocaleManager` 當成「寫入」的 API，讀取資源語言時應使用 process 內的 configuration）：
  - **API 33 以上：完全不自行包裝 context**，`HymnsApp.attachBaseContext` 和 `BaseActivity.attachBaseContext` 都直接使用 base。由 framework 統一套用語言，Application、Activity、Service 的 locale 自然一致。
  - **API 33 以下**：
    - 只有在 pref 是明確選擇（`ZH_HANS`、`ZH_HANT`、`EN`）時，才用 `LocaleHelper.wrap(ctx, 對應 Locale)` 包裝，這是沿用既有的機制。
    - `SYSTEM` 時不包裝，直接使用系統的 configuration。
- **「跟隨系統」但系統語言不支援（例如日文）時顯示英文**：改在資源層解決，不靠包裝（rev 3 Codex P1）。
  - **把預設資源改成英文**：
    - `values/`（目前是簡中）的內容移到 `values-zh/`。
    - `values-en/`（英文）的內容移到 `values/`。
    - 繁中放在 `values-b+zh+Hant/`。
  - 這樣在所有 API 等級上，系統語言是不支援的語言時都會落到英文，`SYSTEM` 模式是真正的「跟隨系統」。
  - 這也是 Android 的標準做法。詳見 A.1.8。
- **在哪裡決定語言**：
  - `HymnsApp.attachBaseContext`：決定要不要包裝 context。
  - `BaseActivity.attachBaseContext`：每次都重新計算，不再依賴靜態欄位 `mLanguage`。
  - **不新增** `HymnsApp.onConfigurationChanged` 的重新包裝（rev 3 Codex P1）。API 33 以上本來就不包裝，交由 framework 更新；API 33 以下的語言只會透過 app 內選單改變，改完一定 `doRestart()`。
- `doRestart()` 只在 API 33 以下、從 app 內選單切換時使用，而且只會在 `MainActivity` 呼叫。
- **刪除** `LocaleHelper.getAppLanguage()` 和 `setAppLanguage()`：前者有 bug，兩者都沒有地方呼叫。
- **保留 `HymnsApp` 預建 WebView 的做法**（它是為了處理 locale，見 Codex P2）。
- **預期的行為**：API 33 以上，不論從 app 內或系統設定頁切換語言，framework 都會觸發 configuration change，**Activity 會被重建**。這是正常的。驗收時只要確認重建後狀態正確、資料沒有遺失，例如輸入到一半的詩歌編號、目前的歌詞頁（rev 3 Codex P2）。

#### A.1.3 不做偏好遷移（使用者決策，2026-10-02：全新項目）

這是全新項目，**沒有舊使用者**，所以：
- 不做任何偏好遷移。
- 不需要判斷是升級還是全新安裝。
- 不需要相容舊的 pref 值。

`PREF_LOCALE` 沒有值時一律視為 `SYSTEM`；讀到非法值時也回到 `SYSTEM`，只做防禦性處理，不會當機。

#### A.1.4 選單（處理 Codex P1-3）

- `menu_main.xml` 的 `appLocale` 子選單改成 4 項：`localeSystem`、`localeChinese`（简体中文）、`localeChineseHant`（繁體中文）、`localeEnglish`。
  - 子選單包在 `<group android:checkableBehavior="single">` 裡。
  - 每個語言的標題都用該語言本身的寫法，不跟著介面翻譯，放在 `values/strings.xml` 並標 `translatable="false"`。
- 改寫 `initLanguage(Menu)`：
  - 一律隱藏 `appLanguage`。系統設定頁不再提供入口，因為 API 33 以上的使用者本來就能從系統設定進入，而且 A.1.2 已經處理同步。
  - 用 `LocaleStore.current()` 算出目前的 `AppLanguage`，把對應的項目設為 `setChecked(true)`。
  - 由於每次 `onCreateOptionsMenu` 和 `onCreateContextMenu` 都會呼叫它，打勾狀態會隨重建自動更新。
- `onOptionsItemSelected`：4 個項目都呼叫 `setAppLocale(AppLanguage)`，交給 `LocaleStore.set()`。
  - API 33 以上：呼叫 framework，由 framework 自行重建。
  - API 33 以下：寫入 pref，再呼叫 `doRestart()`。
  - 如果選的就是目前的語言，什麼都不做。

#### A.1.5 歌詞預設語言與防當機（處理 Codex P1-4）

```kotlin
enum class LyricsLang { FOLLOW_UI, SIMPLIFIED, TRADITIONAL
    companion object { fun fromPref(v: String?): LyricsLang = /* 非法 → FOLLOW_UI */ } }

/** 繁體歌詞的地區版本；每一種都有預先產生好的 assets（見 A.1.9）。 */
enum class HantVariant(val conversion: ConversionType, val dirSuffix: String) {
    TW(ConversionType.S2TW, "_hant_tw"), HK(ConversionType.S2HK, "_hant_hk") }

object LyricsLanguagePolicy {
    fun resolveShowTraditional(pref: LyricsLang, uiLocale: Locale): Boolean
    fun defaultVariant(uiLocale: Locale): HantVariant          // zh-HK/MO → HK；其他 → TW
    fun parseVariant(v: String?, uiLocale: Locale): HantVariant
    // "S2HK" → HK；"S2TW" → TW；null、非法值 → defaultVariant；絕不丟例外
}
```

- 新增 pref `LyricsDefaultLang`。
- `PREF_CONVERSION_TYPE` 沿用原本的 key，但只會存 `S2TW` 或 `S2HK`。
- **香港字形暫時隱藏（使用者決策，2026-10-02）**：設定畫面不顯示「繁中字形」區塊，所有繁中使用者都用台灣字形，包括香港和澳門地區。香港版歌詞檔保留，`LyricsLanguagePolicy.HK_VARIANT_ENABLED` 改成 `true` 就能重新開放。
- **轉換標準從 4 個精簡成 2 個（使用者決策，2026-10-02）**：
  - 只保留「台灣」（S2TW，只轉字形）和「香港」（S2HK）。
  - 移除「標準繁體」（S2T）和「台灣詞彙」（S2TWP），因為 S2TWP 會改動詩歌用詞。
- **所有讀取 `PREF_CONVERSION_TYPE` 的地方都改用 `parseVariant`**：
  - `ContentView.java:189, 603`：原本用 `valueOf`，遇到非法值會當機。
  - `ChineseS2TSelection.java:53`：原本直接讀字串去勾選 RadioButton，遇到非法值會讓 `mConversionType` 沒有初始化。改成先用 `parseVariant` 初始化，再勾選對應的 RadioButton；如果讀到的原始值是非法的，就立刻把合法值寫回 pref。
- `button_ts` 改成只切換「這次的檢視」：
  - 狀態放在 `ContentHandler` 的欄位 `Boolean? lyricsViewOverride`，`null` 代表依照預設。
  - 翻頁時維持同一個狀態；`ContentHandler` 結束後就消失。
  - `ContentView` 每次要顯示時都計算 `override ?: policy.resolveShowTraditional(...)`。
  - 刪除 `PREF_SIMPLIFY`（全新項目，不需保留舊 key）。
- `ChineseS2TSelection` 的上方新增「歌詞預設語言」RadioGroup；下方的轉換標準改成只有「台灣／香港」兩個選項。主選單新增入口「歌詞語言」，開啟同一個畫面。
- 注意（Codex P2）：這些改動跨越 Activity 與 Fragment 的狀態，不是純局部修改。A.3 有對應的整合測試項目。

#### A.1.9 預先產生繁體歌詞（使用者決策，2026-10-02，取代執行時轉換）

- **目的**：
  - 不再於每次翻頁時呼叫 OpenCC。
  - 繁體歌詞可以人工校對，修正 OpenCC 遇到一字多義時的錯誤。
- **產物**：每個 `lyrics_<type>_text/` 都有兩個對應的目錄：
  - `lyrics_<type>_text_hant_tw/`：用 S2TW 產生
  - `lyrics_<type>_text_hant_hk/`：用 S2HK 產生

  檔名和原本的簡體檔相同。這些產物要 commit 進 repo（完整的簡體歌詞本來就在 repo 裡）。
- **大小**：簡體歌詞共約 1.6 MB，壓縮後約 0.6 MB。兩套繁體會讓 APK 多約 1.2 MB，相對於 99 MB 的 assets 可以忽略。
- **產生工具 `tools/gen_lyrics_hant.py`**（呼叫 OpenCC CLI）：
  1. 對每個簡體檔用 OpenCC 轉換。
  2. 套用校對表 `tools/lyrics_hant_overrides.tsv`。格式是 `variant<TAB>from<TAB>to`，`variant` 可以是 `tw`、`hk` 或 `*`；`from` 是轉換後的繁體片段，要帶足夠的上下文，避免誤替換。
  3. 寫出產物。
  4. 寫出 `assets/lyrics_hant_manifest.txt`，每行是 `來源相對路徑<TAB>來源的 sha1`。
  - 這個工具是冪等的：同樣的輸入加上同樣的校對表，產物完全相同。
- **校對（使用者決策：只校對一字多義的字）**：
  - `tools/gen_lyrics_hant.py --report` 會列出含有易錯字的歌詞行，輸出成 `docs/superpowers/plans/lyrics-hant-review.csv`，欄位是「檔案、行、簡體、台灣、香港」。
  - 易錯字清單：于、里、后、复、发、只、干、历、面、云、台、余、松、谷、斗、志、准、范、冲、尽、获、系、钟、制、致、表、卷、借、恶、征、党、丑。
  - 使用者或教會同工確認後，把需要修正的地方寫進校對表，再重新產生。
- **同步保護**：新增 JVM 單元測試 `LyricsHantAssetsTest`，直接讀 `src/main/assets`，檢查以下兩點，任一不符就失敗：
  - 每個簡體檔在兩個 `_hant_*` 目錄都有對應檔案，不多也不少。
  - manifest 裡的 sha1 和目前的簡體檔一致。

  這樣簡體歌詞更新了卻忘記重新產生時，測試就會失敗。
- **App 端**：
  - 純函式 `LyricsAssets.hantPath(simplifiedPath, variant)`，例如把 `lyrics_db_text/db1.txt` 對應到 `lyrics_db_text_hant_tw/db1.txt`。
  - `ContentView.showLyricsChText` 讀取簡體檔和對應 variant 的繁體檔，兩份都讀。每首只有幾 KB，這樣切換簡繁時不必重新讀檔。不再呼叫 `ChineseConverter`。
  - 如果繁體檔不存在（理論上會被測試擋下），就退回用 OpenCC 即時轉換，並記錄 `Timber.w`，不能當機。
- **不在 A 的範圍**：
  - 目錄（TOC）標題的繁體版：檔案可以用同一個工具產生，但 `HymnToc` 還用到筆畫索引（簡體和繁體的筆畫數不同），需要另外設計，延到 A-opt-2。
  - 搜尋結果的摘要：仍然是簡體。

#### A.1.6 搜尋輸入安全性（取代原本的 A-opt-1，處理 Codex P1-5）

- 刪除原本的 A-opt-1，因為 `MainActivity.java:336` 已經會做 T2S 轉換。
- 新增純函式 `SearchPattern.build(query: String): Pattern?`，規格如下：
  1. `q = query.trim()`。只去掉頭尾的空白，**中間的空白保留**，並視為字面字元。
  2. 如果 `q` 是空字串，回傳 `null`，由呼叫端提示使用者。
  3. `q.split("他", limit = -1)` 切成多段。每一段都用 `Pattern.quote()` 處理，空的段落產生空字串。再用 `"[祂他]"` 把各段接起來，然後 `Pattern.compile`。
  4. **禁止**先對整個字串做 `Pattern.quote(q)` 再替換「他」，因為那樣會把 `[祂他]` 也引用成字面文字，替換就失效了。
  - 「他」是 BMP 字元，用它切分不會切斷 surrogate pair；quote 本身也能安全處理 surrogate pair。
  - 順便修掉原本多出來的 `|`。
- `ContentSearch.java:321` 改成呼叫 `build`。順便把它移出檔案迴圈，讓 regex 只編譯一次（這也算是 B-6 的一小部分）。
- 單元測試要涵蓋：
  - 特殊字元：`(`、`[`、`*`、`\`、`\E`（quote 的邊界字元）
  - 「他」的位置：只輸入「他」、多個「他」、「他」在開頭或結尾
  - 「祂」：輸入「他」要能比對到「祂」；輸入「祂」只比對「祂」本身
  - 其他：中間有空白、頭尾有空白、純空白、emoji 或擴充漢字（surrogate pair）

#### A.1.7 螢幕常亮（使用者建議，小改動）

- 在 `ContentHandler`（Activity）層級設定：`onResume` 時 `getWindow().addFlags(FLAG_KEEP_SCREEN_ON)`，`onPause` 時 `clearFlags`。
  - 不設在 Fragment 或 layout 的 view 上，所以 ViewPager 換頁、view 重建時都不會失效（rev 2 Codex P2）。
  - 樂譜頁和歌詞頁都在這個 Activity 裡，所以兩種頁面都會生效。
- A 不提供開關，A2 才新增「螢幕常亮」開關（預設開啟）。

#### A.1.8 資源

- **調整資源目錄**（配合 A.1.2）：
  - 把 `values/strings.xml`、`array.xml` 等**需要翻譯**的檔案（目前是簡中）移到 `values-zh/`。
  - 把 `values-en/` 的對應檔案移到 `values/`。
  - `colors`、`styles`、`attrs`、`theme` 這些不需要翻譯的檔案留在 `values/`。
  - 搬移後要比對兩邊的 key 集合：
    - 英文版有、簡中版沒有的 key（`values-en` 目前比 `values` 多約 20 行）：逐條確認，在 `values-zh/` 補上。
    - 只出現在簡中版的 key：補上英文翻譯。

    目標是讓 lint 的 `MissingTranslation` 和 `ExtraTranslation` 都是 0。
  - **預設的 `values/` 必須是完整的英文**：預設資源不能缺任何一項，其他語言的資源可以缺。逐條檢查 `values-en` 搬過來之後還殘留的中文。例如 `app_name` 目前在 `values-en` 裡仍是「诗歌本」（rev 4 Codex P2）。**已決定（使用者，2026-10-02）**：`app_name` 英文用 `Hymnal`、繁中用「詩歌」、簡中用「诗歌」，取代原本的「诗歌本」。圖示上的文字不受影響。
  - **API 24 以上的資源解析**（實作時要用測試確認，見 A.3）：
    - zh-CN、zh-SG、zh-Hans → `values-zh`
    - zh-TW、zh-HK、zh-MO、zh-Hant → `values-b+zh+Hant`
    - 其他語言 → `values`（英文）
  - `res/resources.properties` 的內容改為 `unqualifiedResLocale=en-US`。
- 新增 `res/values-b+zh+Hant/strings.xml` 和 `array.xml`。
- 初稿用 OpenCC `s2twp` 從 `values/strings.xml`（265 條）轉換，再**人工逐條校對**。校對清單請使用者確認，特別是教會的慣用詞。
- 掃描 **Java 程式碼和所有資源 XML** 裡寫死的使用者可見字串（Codex P2）：
  - layout 中疑似範例文字的字串（例如 `hymn_toc_list_item.xml:26`、`hymn_toc_list_group.xml:23`），**要逐項確認 adapter 在 bind 時一定會覆寫 `setText`**，確認後才改成 `tools:text`；如果有任何一條路徑沒有覆寫，就改用 string resource（rev 2 Codex P2）。
  - 其他確實會顯示給使用者的字串移到 `strings.xml`。
- locale config：
  - 新增 `res/resources.properties`（`unqualifiedResLocale=en-US`），讓 `generateLocaleConfig` 產生 en、zh、zh-Hant，系統設定頁才會列出這三種語言。
  - 刪除手寫的 `locale_config.xml`；如果 manifest 有引用它，一併移除，改用產生的版本。
  - build 之後檢查合併後的 manifest 和產生的 locale config。
- `changelog_master.xml` 不翻譯。

### A.2 實作步驟（TDD）

1. **測試基礎設施**：`testImplementation 'junit:junit:4.13.2'`、`testImplementation 'com.google.truth:truth:1.4.4'`（版本固定，實作時先查 Maven Central 確認是最新的穩定版，然後固定下來）。另外加入 `androidTestImplementation 'androidx.test.ext:junit'` 和 `'androidx.test:runner'`，同樣固定版本，給資源解析的 instrumented test 使用。確認 `./gradlew :hymnchtv:testDebugUnitTest` 能執行。
2. 依照紅 → 綠 → 重構的順序實作下列模組：
   - `AppLanguage.fromTag` 與 `AppLanguage.fromFrameworkTags`
   - `LanguageResolver`
   - `LocaleStore`（用假資料測試兩種 API 等級的讀寫邏輯）
   - `LyricsLang.fromPref`
   - `LyricsLanguagePolicy`
   - `SearchPattern.build`

   每個模組都要測非法輸入、`null`、邊界條件。
3. **整合語言功能**：
   - `LocaleHelper` 改為委派給新模組。
   - `HymnsApp.attachBaseContext`：決定是否包裝 context（API 33 以上不包裝）。
   - `BaseActivity.attachBaseContext`：每次重新計算語言；API 33 以上不包裝。
   - `MainActivity`：更新選單與 `setAppLocale`。
4. **整合歌詞功能**：修改 `ContentView`、`ContentHandler`（override 狀態與 keepScreenOn 的 window flag）、`ChineseS2TSelection`。
5. **整合搜尋**：修改 `ContentSearch`。
6. **資源**：
   - **先調整資源目錄**：把預設資源改成英文，並單獨一個 commit，方便審查和回退。
   - 比對 key 集合，補齊缺的字串。
   - 新增 `tools/gen_zh_hant.sh`，產生繁中初稿後人工校對。
   - 處理寫死的字串。
   - 處理 locale config。
   - 新增一個 instrumented test（`src/androidTest`）：用 `createConfigurationContext` 分別建立 zh-CN、zh-SG、zh-TW、zh-HK、zh-MO、zh-Hant、ja、en 的 context，確認取到的字串來自正確的資源目錄。**這個測試必須在 API 24 和 API 34 的模擬器上都跑過**（rev 4 Codex P2）。
   - Java 程式碼裡寫死的簡中字串（例如 `ContentHandler.java:1214`）也一起移到資源檔。
7. **Activity 重建後保留狀態**：API 33 以上切換語言時 Activity 會被重建，所以要讓狀態能保存與還原（rev 4 Codex P2）：
   - `MainActivity`：在 `onSaveInstanceState` 保存輸入到一半的編號和目前選的詩歌本。
   - `ContentHandler`：保存目前的頁碼，以及歌詞檢視的 override 狀態。目前重建時會從原始 intent 重新定位，使用者翻到的頁面會遺失。
   - 用 instrumented test 驗證：先設定狀態，呼叫 `ActivityScenario.recreate()` 後，狀態仍然存在。
8. **驗證**（見 A.3），然後用 code-reviewer 和 Codex 審查 diff。

### A.3 驗證

- `./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug :hymnchtv:lintDebug` 全部通過，lint 不能新增 `MissingTranslation` 或 `HardcodedText`。
- 手動測試，至少在 API 24 和 API 34 以上的模擬器各跑一次：
  - **全新安裝**：系統語言分別設為 zh-TW、zh-HK、zh-CN、en-US，確認介面語言正確。
  - **app 內切換**：4 種語言都要切換到，而且重啟後選單的勾選要正確。
  - **API 34 系統設定**：
    - 在系統設定頁改 app 語言後回到 app，介面（包括用全域 context 取字串的 toast 和通知）和 app 內選單的勾選都要一致。Activity 被重建是預期行為，但重建後輸入到一半的編號、目前的歌詞頁都要保持。
    - 把系統設定改回「系統預設」，app 的選擇要變成「跟隨系統」。
    - 反過來，從 app 內切換語言後，系統設定頁要顯示相同的語言。
  - **系統語言是不支援的語言**（例如日文）＋跟隨系統：API 24 和 API 34 的介面都要顯示英文。
  - **資源目錄調整後的回歸測試**：簡中介面的所有畫面（主頁、目錄、搜尋、歌詞、媒體設定、關於）都要和 v2.9.2 顯示相同的文字，不能有任何字串掉回英文。可以用 lint 的翻譯檢查搭配逐頁截圖比對。
  - 切換語言後開啟英文歌詞（WebView），確認介面語言沒有被重設。
  - **歌詞預設**：3 種選項 × 簡繁兩種介面都要測。按切換鈕後翻頁要保持；離開再進來要回到預設值。
  - 手動把 `PREF_CONVERSION_TYPE` 改成非法值，app 不能當機。
  - **搜尋**：輸入 `(`、`[`、`*` 不能當機；輸入繁體字要搜得到；搜「他」要能找到含「祂」的歌詞。
  - 在歌詞頁放置超過系統休眠時間，螢幕不能熄滅；回到首頁後，螢幕恢復正常的休眠行為。

---

## 子項目 A2：閱讀設定（大綱，開工前要先設計並經 Codex 審查）

使用者在 2026-10-02 決定要加入的設定。目前歌詞頁一定同時顯示樂譜（`contentView`～`contentView_d` 的圖片）和歌詞文字，沒有任何選項可以只看其中一種。

| 設定 | 內容 | 備註 |
|---|---|---|
| 顯示模式 | 「譜＋詞」（預設，維持現狀）、「只顯示譜」、「只顯示詞」；設定裡選預設值，歌詞頁另有按鈕可以臨時切換 | 做法和 A 的「歌詞預設語言＋本次切換」一樣。「只顯示詞」時不載入樂譜圖片；「只顯示譜」時不做繁簡轉換。兩者都能減輕舊手機的負擔 |
| 歌詞預設字級 | 小／中／大／特大 | 要和現有的手勢縮放（`PREF_LYRICS_SCALE_P/L`）整合：預設字級決定起點，手勢再從這個起點微調 |
| 關閉翻頁動畫 | 停用 `DepthPageTransformer` | 和子項目 B 的「低階裝置模式」共用同一個開關，低階裝置預設關閉 |
| 播放器預設顯示 | 對應現有的 `PREF_MENU_SHOW` | 從歌詞頁的選單移到設定頁 |
| 螢幕常亮 | 開關，預設開啟 | 取代 A 的固定行為 |

| 歌詞字體 | 「系統字體」或「文楷」（以 LXGW WenKai 為基礎） | 見下方「字體」 |
| 背景 | 主頁和歌詞頁可以分開選；共 20 款程式繪製的背景，深色模式時自動換成深色款；保留自訂照片，並加上變暗和模糊滑桿 | 見下方「背景」 |

**字體（使用者在 2026-10-02 提出，已實測）**
- 原始字體太大：LXGW WenKai Regular 約 25.6 MB，WenKai TC Regular 約 15.3 MB。
- 歌詞、繁體產物和介面字串總共只用到約 3,900 個不重複的漢字。用 fonttools 子集化後：
  - 簡體版（WenKai）1.45 MB
  - 繁體版（WenKai TC，台灣字形）2.06 MB
  - 兩者合計約 3.5 MB；APK 壓縮後約多 2.2 MB。
- **授權**：SIL OFL 1.1。保留字型名稱包括「霞鹜」「霞鶩」「落霞孤鹜」「落霞孤鶩」「LXGW」，附加許可只涵蓋「未修改的重新編譯版本」和「只用於網頁字型傳遞的子集」。因此：
  - App 內建的子集字型屬於修改版本，**字型內部名稱必須改名**，例如 `HymnalKai`，介面上也不使用「霞鶩文楷」作為字型名稱。
  - 在 About 頁註明「字型衍生自 LXGW WenKai，SIL OFL 1.1」，並隨 app 附上 OFL 授權全文。
  - 如果想保留原名，要先取得作者的書面同意。
- **工具**：`tools/gen_font_subset.py` 從簡體歌詞、兩套繁體產物和 strings 收集字元後做子集化，並把字元清單的雜湊寫進 manifest。同步測試會檢查「歌詞用到的每一個字都在字型裡」。歌詞更新後，字型要跟著重新產生。
- **缺字的處理**：使用者自己輸入的文字（例如 D-1b 的筆記）如果出現子集裡沒有的字，Android 會自動改用系統字型顯示那個字，不會出現空白框。
- **套用範圍**：只套用在歌詞文字。樂譜是圖片，不受影響。

**背景（使用者在 2026-10-02 提出）**
- 選樣頁：https://claude.ai/artifact/V3fLKCbvjwetQLyMty8bZi
- 共 20 款，分成四類：淨色 4 款、漸層 5 款、意象 7 款（五線譜、橄欖枝、麥穗、白鴿、光芒、活水、葡萄樹）、深色 4 款。
- 全部用 GradientDrawable、VectorDrawable 或小圖重複拼貼繪製，每款不到 5 KB，任何解析度都清晰。這也取代 B-5 所說、目前放大 4 倍解碼的 JPG 桌布。
- 每款背景都附一組文字色和強調色，確保歌詞永遠清楚可讀。
- **使用者決策（2026-10-02）：20 款全部收錄。** 預設值：歌詞頁用「宣紙白」，因為最安靜、對比最高，適合長輩閱讀；主頁用「晨曦」；深色模式時兩者都改用「夜讀」。使用者隨時可以在設定中更改。

- **設定入口**：在 C 的設定頁完成之前，先沿用 A 新增的「歌詞語言」設定畫面，把它擴充成「閱讀設定」畫面；C 完成後再整體搬進設定頁。
- **設計時要決定的問題**：
  - 「只顯示詞」時，沒有歌詞文字的詩歌怎麼辦（例如只有樂譜的詩歌）？
  - 「只顯示譜」時，簡繁按鈕和英文按鈕要怎麼處理？
  - 橫向模式下，顯示模式要怎麼呈現？

---

## 子項目 B：效能優化

### B.1 熱點清單

依可能的影響排序，**全部尚未經過量測，可能有誤**：

| # | 熱點 | 位置 | 初步修法 |
|---|---|---|---|
| B-1 | 每次呼叫 OpenCC `convert()` 都重建 config 並載入字典，而且是在主執行緒上對每一頁執行，連簡體模式也照樣轉換；第一次呼叫時還會在主執行緒複製 1.1 MB 的 assets | `chineseconverter.cpp:57`、`ContentView.java:462`、`ChineseConverter.java:24-27` | **大部分已由 A.1.9 解決**：歌詞改讀預先產生的檔案，翻頁不再呼叫 OpenCC。剩下的只有搜尋時一次的 T2S 轉換，以及第一次使用時的 assets 複製，因此優先順序降為低 |
| B-2 | 每個歌詞頁都會 inflate 一個 WebView | `content_lyrics.xml:91` | 延遲到需要時才建立 WebView（需要先重構，見 B.3） |
| B-3 | `Application.onCreate` 預建 WebView | `HymnsApp.java:109` | 這是為了處理 locale 的問題，**不能直接刪除**；只考慮延後到 IdleHandler 執行，而且必須通過語系回歸測試 |
| B-4 | 每次翻頁都在主執行緒查 SQLite、掃目錄、呼叫 `getIdentifier` | `ContentHandler.java:613, 1090-1175, 1250` | 改成非同步（需要先設計，見 B.3） |
| B-5 | 桌布只放在 `drawable-ldpi`，可能被放大解碼；而且會重複解碼兩次 | `res/drawable-ldpi/bg*.jpg`、`main.xml:7`、`MainActivity.java:1259-1265` | 先量測實際解碼尺寸再決定（見 B.3） |
| B-6 | 搜尋和目錄在主執行緒開啟大量 asset 檔案 | `ContentSearch.java:93-260`、`HymnToc.java:273-300, 593` | 移到背景執行並顯示進度（regex 只編譯一次的部分已在 A.1.6 完成） |
| B-7 | `handleIntent` 裡呼叫了 `super.onStart()`（bug）；啟動時還有幾項零碎的同步工作 | `MainActivity.java:254, 377`、`HymnsApp.java:144, 256-264` | 刪掉多餘的呼叫；其餘工作延後執行 |
| B-8 | 樂譜圖片用 ARGB_8888 解碼；沒有開啟 R8；沒有 baseline profile | `MyGlideApp.java:76-85`、`build.gradle:32` | 每一項都需要另行評估（見 B.3） |


**使用者在 2026-10-02 決定新增的項目：**

| # | 熱點 | 位置 | 初步修法 |
|---|---|---|---|
| B-9a | 大量匯入媒體連結時，每一筆都各自 commit 一次。`url_import.txt` 有 2,696 筆，舊手機的儲存很慢，可能要花上幾十秒 | `MediaConfig.java:1106`、`NotionRecord.java:484`、`QQRecord.java:269` | 整批包在單一 transaction 裡（`beginTransaction`／`setTransactionSuccessful`／`endTransaction`），並在背景執行 |
| B-9b | 每翻一頁最多查 4 次資料庫（每種媒體類型各查一次），而且在主執行緒上執行 | `ContentHandler.getHymnMediaState`、`DatabaseBackend.getMediaRecord` | 新增 `getMediaRecords(hymnType, hymnNo, isFu)`，一次查出這首詩歌所有媒體類型；和 B-4 一起移到背景執行 |
| B-9c | 每次呼叫 `getWritableDatabase()` 都執行 `PRAGMA foreign_keys=ON`，但資料庫根本沒有外鍵 | `DatabaseBackend.java:437-443` | 刪掉這個 override |
| B-9d | 沒有開啟 WAL 模式：寫入時讀取會被卡住，磁碟寫入次數也比較多 | `DatabaseBackend` | 在 `onConfigure` 呼叫 `db.enableWriteAheadLogging()` |
| B-9e | 每開一首詩歌，都把整張歷史表讀出來，只為了計算有幾筆 | `DatabaseBackend.storeHymnHistory` | 改用 `DatabaseUtils.queryNumEntries`；只有超過上限時才刪除舊資料 |
| B-10 | ViewPager2 會預先建立大約 9 頁，每一頁都有一個 WebView 和 5 張圖片 | `ContentHandler.java:289-295` | 關閉 RecyclerView 的 item prefetch，並縮小 view cache。改完要量測翻頁是否變慢 |
| B-11 | 低階裝置模式：用 `ActivityManager.isLowRamDevice()` 或 memoryClass 判斷是否為低階手機 | 新增 `DeviceProfile` | 低階手機自動套用下列設定：關閉翻頁動畫（和 A2 的設定共用同一個開關）、樂譜改用 RGB_565（需先通過 B.3 第 7 項的視覺驗收）、桌布降低解析度 |

- **資料表結構**：把 6 張詩歌本資料表合併成一張、或改用 Room，對效能都沒有幫助。不過既然是全新項目，沒有既有資料要遷移，可以在子項目 D-1 新增「收藏／歌單」資料表時，順便評估是否直接改用 Room 並重設 schema。
- **既有 bug**：`ContentView.showLyricsScore` 的第 5 頁樂譜沒有呼叫 `findViewById(R.id.contentView_d)`，會覆蓋掉第 4 頁的圖。在 B 或 A2 修正。

### B.2 已知方向

- 先量測，有了數據才動手修。每一項修正都是一個獨立的 commit，修完重新量測並記錄。
- 背景工作的處理方式：
  - Kotlin 程式碼用 `viewLifecycleOwner.lifecycleScope` 搭配 `Dispatchers.IO`。
  - Java 程式碼用共用的 executor，並搭配 request token。
  - 同一個類別裡不混用這兩種方式。

### B.3 開工前必須補完的設計（Codex 的 P1，**目前尚未處理**）

1. **量測協定**：明確定義以下條件，然後依量測到的基準值訂出逐項的預期改善幅度，取代原本「30%／5%／20 MB」這些任意數字：
   - 基準裝置（型號／API）
   - build type（release 或 benchmark variant，不能用 debug）
   - 資料集（哪幾本詩歌、翻幾頁）
   - 重複次數與取中位數的方式
   - 散熱狀態
   - 可接受的誤差範圍

   StrictMode 的驗收只看「我們自己的程式碼在主執行緒做 IO」，不要求零違規。
2. **非同步 UI 生命週期**（B-1、B-4）：
   - 每個請求都帶 token（頁碼加上 generation），回到主執行緒後先確認 view 還在、頁面相符，才把結果填進畫面。
   - 定義載入中與失敗時的 UI。
   - 頁面銷毀時取消請求。
   - `getHymnMediaState` 改成「先回傳預設狀態，完成後再更新」，並定義快取在下載、刪除、匯入、外部儲存變動時如何失效。
3. **JNI 快取**：
   - 每個 config 對應一個 converter。除非能從 OpenCC 原始碼確認 `Converter::Convert` 是 thread-safe，否則每個 converter 都要有自己的 mutex。
   - 處理 `NewFromFile()` 失敗和 native 例外：回傳原文並記錄 log，不能讓 app 當機。
   - 字典版本改變時，快取要失效。
   - 測試改成驗證「並發時結果正確」和「失敗路徑」；效能用明確的 benchmark 門檻驗收，不寫「第二次比較快」這種不穩定的測試。
4. **OpenCC assets 初始化**：先寫到暫存目錄，完整複製後再原子性地 rename；整個初始化只用一把鎖；初始化完成前，轉換請求要等待，或暫時顯示簡體。
5. **WebView 延遲建立**（B-2）：先把 `ContentView` 裡所有存取 `lyricsEnglish` 的地方（字級、visibility、載入、長按、callback）改成可以處理 null 的寫法，再改用 ViewStub；同時對英文歌詞做回歸測試，包括自動載入的情況。
6. **桌布**（B-5）：先用 `dumpsys meminfo` 和 bitmap 尺寸實際量測。背景容器要另外定義，因為 Glide 只能載入到 `ImageView`，而桌布目前是用 `setBackgroundResource` 設在一般 view 上。冷啟動時的 window background 要保留，避免出現白屏。
7. **RGB_565、R8、baseline profile**：
   - RGB_565：用實際的樂譜圖片做視覺驗收，確認沒有 alpha 通道、灰階沒有失真。
   - baseline profile：需要 profile 產生流程（macrobenchmark module 或對應的產物），並在 release 版驗證。
   - R8：放在最後，單獨開一個 PR。

---

## 子項目 C：介面現代化（大綱，正式設計前需要再做一次 brainstorm）

Codex 的 P1 指出，C 目前**還不是可執行的計畫**。開工前必須先產出 `docs/superpowers/specs/…-ui-design.md`，內容包括：

- **現有功能對應表**：每個畫面、選單項目、長按功能，在新版介面中各放在哪裡。目前約有 16 處長按功能。
- **資料與狀態的歸屬**：每份資料由哪個 ViewModel 或 Activity 負責。
- **導航細節**：分享 intent、`singleTask` 的遷移方式、返回鍵的行為。
- **分階段計畫**：每個階段都要能單獨回退。
- **視覺方向**：準備 2～3 個 mockup，讓使用者選擇。

**視覺方向（使用者決策，2026-10-02，採用混合建議）**：設計稿見 https://claude.ai/artifact/JxKzrjhQfgJpyjywury5Ja
- **整體架構用方向 A（Material 3 標準）**：
  - 底部導覽四個分頁：首頁、目錄、我的詩歌、設定。
  - 使用標準 Material 3 元件，支援動態取色，畫面延伸到系統列。
  - 設定頁用標準的偏好設定清單。
- **歌詞頁用方向 B（詩本紙本）的版式**：
  - 文楷字體、宣紙白背景、紅色節數。
  - 標題下方印出拍號和調號，預設字級 24sp。
  - 簡中／繁中、譜詞的切換按鈕沿用 A 的樣式。
- **首頁輸入號碼加上方向 C（聚會速記）的行為**：
  - 輸入號碼時即時顯示詩歌標題。
  - 詩歌本按鈕放大。
  - 加入「＋歌單」按鈕。
  - 歌詞頁加上「下一首」，聚會中依歌單連續翻頁，不必回到首頁。

初步方向（尚未定案）：

- **設計系統**：Material 3 搭配 DayNight 主題；Android 12 以上支援動態取色，品牌色作為 fallback；畫面延伸到系統列（edge-to-edge）。
- **參考對象**：
  - Google 電話 app 的撥號盤，用於輸入詩歌編號。
  - YouVersion Bible 的閱讀頁和字級控制。
  - Apple Music 和 Spotify 的歌詞頁與迷你播放器。
- **使用者觀點的重點**：
  - **聚會報號的速度**：記住上次使用的詩歌本；首頁清楚顯示目前選的是哪一本；輸入號碼後直接開啟。
  - **長按功能改成看得見的操作**：把隱藏在長按裡的功能，改成畫面上的按鈕或設定項目。
  - **集中設定**：語言、歌詞語言、字級、主題、桌布、更新，統一放進用 `PreferenceFragmentCompat` 做的設定頁。
  - **首頁空間留給最常用的功能**：「軟體更新」按鈕從首頁移到設定頁。
  - **年長者友善**：觸控區至少 48dp、對比度達到 WCAG AA 標準、支援系統字級。
- **Kotlin**：只把被改寫的畫面轉成 Kotlin，一次一個畫面，不做全面重寫。
- **開工前的保護網**：先為關鍵流程寫一個 instrumented smoke test，流程是「輸入編號 → 開啟歌詞 → 翻頁 → 播放」。

---

## 子項目 D：使用體驗功能（大綱，需要確認範圍）

這一節整理自使用者觀點的分析。**已決定（2026-10-02，採用建議）**：

- **要做**：D-1（收藏與歌單）、D-2（鎖屏與通知列的播放控制）。
- **和 C 一起做**：D-3（底部迷你播放器）。
- **延後**：D-4（整本詩歌離線下載）、D-5（分享、投影大字模式、平板排版）。
- **排在 C 之後**：D-1 和 D-2 開工前，各自先寫 spec。

各項功能的說明如下：

| 功能 | 使用者價值 | 現況 | 備註 |
|---|---|---|---|
| D-1 收藏與「我的歌單」 | 帶詩歌的人或在家學唱的人，可以預先排好一組詩歌，再依序翻頁 | 只有歷史紀錄 | 需要新增資料表與 DB migration（目前 schema v5） |
| D-2 鎖屏與通知列的播放控制 | 關掉螢幕後仍可暫停、重播 | 沒有 MediaSession | 可以用 Media3 的 `MediaSessionService`，取代現在的 `AudioBgService` |
| D-3 底部迷你播放器 | 一邊看歌詞一邊控制播放 | 播放器 UI 佔用畫面 | 和 C 的歌詞頁設計綁在一起做 |
| D-4 整本詩歌離線下載 | 在 Wi-Fi 下一次下載，聚會現場不需要網路 | 只能逐首下載 | 要處理儲存空間和進度 UI |
| D-5 分享歌詞與連結、投影大字模式、平板排版 | 加分項 | 無 | 優先順序最低 |

---

### D-1 詩歌筆記本（使用者在 2026-10-02 決定全部要做）

| 階段 | 內容 |
|---|---|
| D-1a | 收藏、唱詩紀錄、唱過的次數與最近一次的日期，**加上備份匯出／匯入**（必須一起出，否則使用者換手機時資料會遺失；同時開啟 Android 自動備份） |
| D-1b | 筆記與心得：每首可以寫多則，依日期排列；可以連結到某一次唱詩紀錄；可以全文搜尋 |
| D-1c | 歌單與聚會模式：聚會中邊聽報號邊加入，結束後一鍵把整份歌單記成已唱；可以分享成文字 |
| D-1d | 統計與回顧：最常唱的詩歌、依分類或詩歌本統計還沒唱過的、行事曆熱圖、去年今天、很久沒唱的詩歌 |

**記錄方式（使用者決策）：自動記錄，可以修正。**

設計時要訂清楚的規則：
- **觸發條件**：歌詞頁連續停留超過門檻（建議 2 分鐘，可以調整），或媒體播放完畢，就自動記一筆「唱過」。
- **去重**：同一首詩歌在同一個時間窗內只算一次，例如 3 小時內。
- **場合**：自動推測。例如週日上午記為主日，其他時間預設為上次選的場合。
- **修正**：使用者可以刪除或修改任何一筆紀錄，也可以手動補一筆。
- **開關**：在設定中可以關閉自動記錄，改成手動。
- **和現有歷史紀錄的關係**：現有的 `hymnHistory`（打開過的詩歌，最多 200 筆）保持原用途。唱詩紀錄是另外一套，數量不設上限。

**資料**：全新項目，可以直接用 Room 建新的資料表，不用遷移。**資料表一開始就為同步預留欄位**（見子項目 S）：主鍵用 UUID、每筆都有 `updatedAt`、刪除只做標記（`deletedAt`），不真的刪掉。
- `favorite`
- `sing_log`：唱的時間、場合、來源（自動或手動）、所屬歌單
- `note`
- `playlist`／`playlist_item`

詩歌的識別方式沿用「類別＋編號＋是否附錄」。

**入口**：在 C 的底部導覽完成之前，按鈕先放在歌詞頁（☆ 收藏、唱過幾次、📝 筆記），另外加一個「我的詩歌」畫面；C 完成後再移到「我的」分頁。

## 子項目 Z：新身分發佈（使用者決策，2026-10-02）

- **applicationId**：`com.ziontkec.hymnal`。程式碼的 package／namespace 維持 `org.cog.hymnchtv`，不做大規模搬移；只改 applicationId，以及所有由它衍生的值，例如 FileProvider authority、通知頻道、`Download/hymnchtv` 目錄名稱（是否改名在設計時決定）。
- **發佈管道**：**只用 GitHub Releases，不上架 Google Play**。
  - App 內的「檢查更新」改成讀取 GitHub Releases 的最新版資訊，下載 APK 安裝。原本指向原作者 cmeng-git 的 `version.properties` 或 URL 全部移除。
  - 不經過 Play，所以拿不到雲端預先編譯的 profile。B 的 Baseline Profile（`profileinstaller`）因此對舊手機的冷啟動特別重要。
  - 簽章金鑰由使用者自行保管，不放進 repo（`settings.signing` 機制已存在）。
- **About 頁**：不顯示任何個人資料（作者、聯絡方式等）。只顯示：
  - App 名稱與版本
  - 開源授權聲明（原專案 Apache-2.0、LXGW WenKai／Klee 的 OFL 1.1、資料由 OpenCC 產生的說明）
  - 第三方程式庫清單

  原作者的版權聲明依 Apache-2.0 的要求保留在授權頁（屬於授權義務，不是個資）。
- **更新來源**：GitHub repo `hitobias/hymnchtv`，也就是目前 clone 的 origin，是 cmeng-git/hymnchtv 的 fork。使用 `https://api.github.com/repos/hitobias/hymnchtv/releases/latest`，下載 release 附件裡的 APK。

## 子項目 S：帳號與同步（大綱，使用者決策：先只存本機，之後再加）

- **範圍決策（2026-10-02）**：S 不進 1.0，1.0 之後再評估。

- **使用者地區（決策，2026-10-02）**：台灣、香港／澳門、海外華人，不包含中國大陸。因此：
  - 可以使用 Google 服務。D-1 階段就開啟 Android 自動備份（`android:allowBackup` 加上 `dataExtractionRules`），讓使用者換手機時能還原資料。
  - 後台優先考慮現成的託管服務（Supabase 或 Firebase），不必自己架伺服器。
  - 簡中介面仍然保留，給海外使用簡體的使用者。
- **觸發條件**：等到確定有人需要「多台裝置同步」或「共享歌單」（帶詩歌的人建好歌單，成員即時看到），才啟動這個子項目。
- **同步模型**：本機優先。先寫入 Room，有網路時再同步上去。衝突時以 `updatedAt` 較新的為準，刪除以 `deletedAt` 標記傳遞。
- **隱私**：筆記和心得屬於個人屬靈紀錄。同步要讓使用者自己選擇開啟，並清楚說明資料存在哪裡；上架前要準備隱私權政策。
- **後台規範**：
  - API 使用統一的回應格式（success／data／error／分頁 meta）。
  - 資料存取採 Repository 模式。
  - 所有輸入都要驗證。
  - 限制請求頻率。
  - 不在程式碼裡寫死金鑰。
  - 開工時另外做安全審查。
- **App 更新與媒體連結**（子項目 Z）：只需要靜態檔案，放在 GitHub Releases 或應用商店，不需要後台程式。

## 共通事項

- **Commit 格式**：子項目內用 conventional commits；正式發版時沿用專案原本的 `See RN x.y.z (date)` 格式。**已確認。**
- **版本規劃（2026-10-02 更新）**：新身分的第一個正式版是 **1.0.0**（versionCode `100000`），範圍為 A、A2、B、C、D-1（資料層＋介面）、Z；不含 S。全部完成後產生 1.0.0 release APK，並做發版前的全面問題檢查（審查、模擬器實測、效能、權限與隱私）。原「A 完成後發 2.10.0」作廢。每次發版都要更新 `changelog_master.xml`。
- **審查**：每個子項目完成後，都由 code-reviewer 和 Codex 審查 diff。
- **不做的事（YAGNI）**：
  - 不翻譯 changelog。
  - 不轉換 TOC 原始檔。
  - 不追求 80% 覆蓋率：專案原本沒有任何測試，所以目標是新寫的純邏輯 100% 覆蓋，不回頭替既有程式碼補測試。

## 主要風險

1. **語言同步**：app 內選單和系統設定頁的語言可能不一致。
   - 對策：
     - 每個 API 等級只有一個真實來源，不做雙向同步。
     - 純函式都有單元測試。
     - A.3 有系統設定頁的手動測試。
2. **設定值是非法值**：可能導致當機。
   - 對策：所有讀取 pref 的地方都要能處理非法值、絕不丟例外，並寫測試覆蓋。
3. **B 的非同步改動**：可能把結果寫到錯誤的頁面，或寫回已經銷毀的 view。
   - 對策：B.3 開工前必須先補完設計。
4. **OpenCC JNI 的並發與初始化**：多執行緒可能同時轉換或同時初始化字典，造成結果錯誤或讀到不完整的字典。
   - 對策：見 B.3 的第 3、4 項。
5. **沒有測試保護網**：重構 `MainActivity` 和 `ContentHandler` 時容易改壞既有功能。
   - 對策：C 開工前先寫 smoke test。

## GSTACK REVIEW REPORT

| Review | Trigger | Why | Runs | Status | Findings |
|--------|---------|-----|------|--------|----------|
| CEO Review | `/plan-ceo-review` | Scope & strategy | 0 | — | — |
| Codex Review | `/codex review` | Independent 2nd opinion | 4 | 子項目 A：CLEAR（no P1）；B 與 C：仍有 P1 未處理 | 第 1 輪 14 P1 + 4 P2；第 2 輪 4 P1 新增；第 3 輪 4 P1 新增；第 4 輪 0 P1、3 P2（已補進計畫） |
| Eng Review | `/plan-eng-review` | Architecture & tests (required) | 0 | — | — |
| Design Review | `/plan-design-review` | UI/UX gaps | 0 | — | — |
| DX Review | `/plan-devex-review` | Developer experience gaps | 0 | — | — |

- **CODEX**：子項目 A 經過 4 輪審查。主要修正如下：
  - API 33+ 改成「每個 API 等級只有一個真實來源」，並由 framework 套用語言，不再自行包裝 context。
  - 預設資源改成英文。
  - 全新項目，不做任何偏好遷移（使用者決策）。
  - 搜尋改成安全的字面比對。
  - 讀取 pref 時能處理非法值，不會當機。
  - Activity 重建後會保留狀態。
- **UNRESOLVED**：
  - B.3 有 7 項 P1，B 開工前必須補完。
  - C 需要另做 spec。
  - 繁中用語的校對清單：實作時產生，請使用者確認。
- **VERDICT**：子項目 A 已通過 Codex 審查，可以進入實作計畫。B、C、D 尚未通過審查。eng review required。
