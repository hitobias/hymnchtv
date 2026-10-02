# 子項目 A：介面繁中＋歌詞預設語言 實作計畫

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

## 給執行者（Sonnet 5.5）的說明

使用者決定由 **Sonnet 5.5（`claude-sonnet-5-5`）** 執行這份計畫。建議的啟動方式：

1. 在 repo 根目錄 `/Users/hitobias/orca/hymnchtv` 開一個新的 Claude Code session，用 `/model` 切換到 Sonnet 5.5，或直接執行 `claude --model claude-sonnet-5-5`。
2. 確認目前在 `feat/zh-hant` 分支：`git branch --show-current`。
3. 輸入以下指示：

   > 使用 superpowers:executing-plans 執行 `docs/superpowers/plans/2026-10-02-zh-hant-implementation.md`。從 Task 0 Step 1 的驗證開始，這台電腦已經完成 Step 2、3，接著做 Step 4。每個 task 完成後回報，遇到計畫和程式碼對不上就停下來問我，不要自行猜測。

   如果想讓每個 task 由子代理執行、主對話只負責協調，改用 `superpowers:subagent-driven-development`，並指定子代理也使用 Sonnet。

**執行規則：**
- **照順序執行**：Task 0 → 1 → 2 → 4 → 6 → 6B → 7 → 8 → 9 → 10 → 11 → 12 → 13 → 14 → 15。Task 3 和 Task 5 已經刪除。
- **Task 9 和 Task 10 一起 commit**：Task 9 刪除 `LocaleHelper` 之後，要到 Task 10 才能再次編譯成功。
- **計畫和程式碼對不上就停下來**：例如行號偏移後找不到計畫裡引用的原始程式碼，就停下來回報，不要自己改計畫的意圖。
- **整合型 task 要更小心**：Task 9～12 會跨多個檔案。每完成一個 task，都要跑 `./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug`。
- **需要模擬器的步驟**：Task 8、10～13、15 有 instrumented test 或手動驗證，要先啟動 `api34`，必要時也啟動 `api24`。
- **人工確認的項目**：兩份校對清單（`2026-10-02-zh-hant-terms.md` 和 `lyrics-hant-review.csv`）要交給使用者確認，不能自行判定。
- **最後的審查**：Task 15 的審查要用 code-reviewer 和 `/codex review` 兩種方式。審查發現 P1 時要先修好，才能開 PR。

**Goal:** 這個子項目要做到下列幾件事：
- 介面新增繁體中文。第一次安裝時跟隨系統語言；Android 13 以上和系統設定的「App 語言」頁共用同一份設定。
- 歌詞可以設定預設語言：跟隨介面、簡體或繁體。繁體歌詞改用預先產生的台灣版和香港版檔案，不再即時轉換。
- 搜尋時輸入特殊字元不再當機。
- 看歌詞時螢幕不會自動關閉。

**Architecture:**
- 判斷規則都寫成 Kotlin 純函式（`locale/`、`lyrics/`、`search/`），用 JVM 單元測試，採 TDD。
- 和 Android 打交道的部分集中在一個薄薄的 Kotlin 類別：`LocaleStore`。
- 全新項目，沒有舊使用者，所以不做任何偏好遷移（使用者決策，2026-10-02）。
- 既有的 Java Activity 只改呼叫點。
- 預設資源（`values/`）改成英文，簡中搬到 `values-zh/`，繁中放在 `values-b+zh+Hant/`。
- 語言的唯一真實來源依 API 等級而定：API 33 以上是 framework 的 per-app locale，API 33 以下是 `PREF_LOCALE`。

**Tech Stack:** Android（minSdk 24、compileSdk 37、AGP 9.3.3 內建 Kotlin）、Java 11 與 Kotlin 混用、JUnit 4.13.2、Truth 1.4.5、AndroidX Test（runner 1.7.0、ext-junit 1.3.0）、OpenCC CLI（只用來產生字串初稿）。

**規格來源:** `docs/superpowers/plans/2026-10-02-hymnchtv-modernization-plan.md` 的「子項目 A」（rev 4）。

**分支:** `feat/zh-hant`（已建立）。

**編號說明:** 原本的 Task 3（LocaleMigration）和 Task 5（LyricsMigration）已刪除，因為這是全新項目，不需要遷移。其餘 task 保留原編號，方便對照先前的審查紀錄。

**Commit 規則:**
- 使用 conventional commits。
- 每個 commit 訊息的結尾加上：

  ```
  Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
  ```

**和 spec 不同的地方（刻意的取捨）:**
- **A.2 第 7 點「Activity 重建後保留狀態」不寫 instrumented test**：
  - `MainActivity` 首次啟動會跳出權限要求和 changelog 對話框，`ContentHandler` 又依賴有版權的 assets，repo 裡只有樣本。這兩點都會讓 `ActivityScenario.recreate()` 測試很不穩定。
  - 改成 Task 15 的手動驗證：開啟開發者選項「不保留活動」，再加上 API 34 切換語言。
- **Java 裡寫死的中文只處理純顯示用途的 2 處**：`ContentHandler.java:1214`、`UpdateServiceImpl.java:156`。
  - 其他寫死的中文（`HymnToc` 的目錄分類、`MediaType`、`MediaConfig`、`NotionRecord`、`QQRecord`、筆畫表）同時被拿來做資料比對或解析，改動會破壞功能，所以不在 A 的範圍內。
  - 這些字串的繁體顯示，留給 spec 的 A-opt-2 或子項目 C 處理。
  - 英文介面原本就刻意保留中文的詩歌本名稱（例如 `hymn_title_db`），維持不變。

---

## 檔案結構

**新增（Kotlin，純邏輯，有 JVM 單元測試）：**

| 檔案 | 職責 |
|---|---|
| `hymnchtv/src/main/java/org/cog/hymnchtv/locale/LocaleRules.kt` | 判斷 `Locale` 是否為繁中、簡中 |
| `hymnchtv/src/main/java/org/cog/hymnchtv/locale/AppLanguage.kt` | 介面語言的 enum，以及和 pref 值、framework tag、`Locale` 之間的互相轉換 |
| `hymnchtv/src/main/java/org/cog/hymnchtv/lyrics/LyricsLang.kt` | 歌詞預設語言的 enum |
| `hymnchtv/src/main/java/org/cog/hymnchtv/lyrics/HantVariant.kt` | 繁體歌詞的地區版本（TW、HK）與對應的目錄後綴 |
| `hymnchtv/src/main/java/org/cog/hymnchtv/lyrics/LyricsAssets.kt` | 簡體歌詞路徑轉成繁體歌詞路徑 |
| `hymnchtv/src/main/java/org/cog/hymnchtv/lyrics/LyricsLanguagePolicy.kt` | 決定歌詞顯示簡或繁、轉換標準的預設值與解析 |
| `hymnchtv/src/main/java/org/cog/hymnchtv/search/SearchPattern.kt` | 把使用者的搜尋字串安全地轉成 `Pattern` |

**新增（Kotlin，Android 邊界，靠手動和 instrumented test 驗證）：**

| 檔案 | 職責 |
|---|---|
| `hymnchtv/src/main/java/org/cog/hymnchtv/locale/LocaleStore.kt` | 依 API 等級讀寫介面語言；API 33 以下負責包裝 context |

**測試：**
- `hymnchtv/src/test/java/org/cog/hymnchtv/locale/LocaleRulesTest.kt`
- `hymnchtv/src/test/java/org/cog/hymnchtv/locale/AppLanguageTest.kt`
- `hymnchtv/src/test/java/org/cog/hymnchtv/lyrics/LyricsLanguagePolicyTest.kt`
- `hymnchtv/src/test/java/org/cog/hymnchtv/search/SearchPatternTest.kt`
- `hymnchtv/src/test/java/org/cog/hymnchtv/lyrics/LyricsAssetsTest.kt`
- `hymnchtv/src/test/java/org/cog/hymnchtv/lyrics/LyricsHantAssetsTest.kt`：檢查簡體與繁體的 assets 是否同步
- `hymnchtv/src/androidTest/java/org/cog/hymnchtv/ResourceLocaleResolutionTest.kt`

**修改：**

| 檔案 | 修改內容 |
|---|---|
| `hymnchtv/build.gradle` | 加入測試依賴 |
| `hymnchtv/src/main/java/org/cog/hymnchtv/HymnsApp.java` | `attachBaseContext`、`onCreate` |
| `hymnchtv/src/main/java/org/cog/hymnchtv/BaseActivity.java` | `attachBaseContext` |
| `hymnchtv/src/main/java/org/cog/hymnchtv/MainActivity.java` | 選單、`setAppLocale`、`initLanguage`、狀態保存 |
| `hymnchtv/src/main/java/org/cog/hymnchtv/ContentView.java` | 歌詞的簡繁判斷 |
| `hymnchtv/src/main/java/org/cog/hymnchtv/ContentHandler.java` | 歌詞 override、螢幕常亮、狀態保存、寫死的字串 |
| `hymnchtv/src/main/java/org/cog/hymnchtv/ContentSearch.java` | 改用 `SearchPattern` |
| `hymnchtv/src/main/java/org/cog/hymnchtv/utils/ChineseS2TSelection.java` | 新增歌詞預設語言，處理非法值 |
| `hymnchtv/src/main/java/org/cog/hymnchtv/service/androidupdate/UpdateServiceImpl.java:156` | 寫死的字串 |
| `hymnchtv/src/main/res/menu/menu_main.xml` | 語言子選單、歌詞語言入口 |
| `hymnchtv/src/main/res/layout/chinese_t2s_selection.xml` | 新增歌詞預設語言的 RadioGroup |
| `hymnchtv/src/main/res/layout/hymn_toc_list_item.xml`、`hymn_toc_list_group.xml` | 範例文字改成 `tools:text` |

**工具與產生的 assets（Task 6B）：**
- `tools/gen_lyrics_hant.py`
- `tools/lyrics_hant_overrides.tsv`
- `assets/lyrics_*_text_hant_tw/`、`assets/lyrics_*_text_hant_hk/`、`assets/lyrics_hant_manifest.txt`

**資源搬移與新增：**
- `res/values/strings.xml`（簡中）移到 `res/values-zh/strings.xml`
- `res/values-en/strings.xml`（英文）移到 `res/values/strings.xml`，`values-en/` 目錄刪除
- 新增 `res/values-b+zh+Hant/strings.xml`
- 新增 `res/resources.properties`
- 刪除 `res/xml/locale_config.xml`（manifest 沒有引用它）

**刪除：**
- `hymnchtv/src/main/java/org/cog/hymnchtv/utils/LocaleHelper.java`

---

### Task 0：準備環境、修好建置、記錄基準

**Files:**
- Commit: `gradle/wrapper/gradle-wrapper.jar`
- Commit: OpenCC submodule 的 gitlink（`lib-opencc-android/src/main/jni/OpenCC`）

**現況（2026-10-02 在這台 Mac 上已確認）：** 這個 repo 原本**無法直接 build**，原因有兩個：
- 缺少 `gradle/wrapper/gradle-wrapper.jar`。
- `.gitmodules` 宣告了 OpenCC submodule，但 repo 裡沒有對應的 gitlink，所以 `git submodule update --init` 什麼都不會做。

在這台電腦上，下列項目已經處理好：
- `local.properties`（已在 `.gitignore` 中）：`sdk.dir=/opt/homebrew/share/android-commandlinetools`
- 已安裝的 SDK 套件：`platforms;android-37.0`、`build-tools;37.0.0`、`ndk;28.2.13676358`、`cmake;4.1.2`、`emulator`、`system-images;android-24;google_apis;arm64-v8a`、`system-images;android-34;google_apis;arm64-v8a`
- 已安裝 `opencc`（1.4.2，Homebrew）。系統 JDK 是 Temurin 17，Gradle wrapper 使用的是它。
- `gradle-wrapper.jar`（9.7.1）和 OpenCC（`ver.1.4.2`，commit `025f371`）都已放在工作目錄裡，但**尚未 commit**，`git status` 會顯示為 untracked。
- 基準 build 已通過（`BUILD SUCCESSFUL`）。lint 基準值：`MissingTranslation` 0、`ExtraTranslation` 0、`HardcodedText` 8。

如果換到一台新電腦，Step 1 到 Step 3 要完整做一次；在這台電腦上，只要做 Step 1 的驗證指令，然後做 Step 4 和 Step 5。

- [ ] **Step 1：確認工具都在（新電腦要先安裝）**

  新電腦的安裝指令：

  ```bash
  brew install opencc
  echo "sdk.dir=/opt/homebrew/share/android-commandlinetools" > local.properties   # 依實際 SDK 路徑調整
  yes | sdkmanager "platforms;android-37.0" "build-tools;37.0.0" "ndk;28.2.13676358" "cmake;4.1.2" "emulator" \
    "system-images;android-24;google_apis;arm64-v8a" "system-images;android-34;google_apis;arm64-v8a"
  ```

  驗證指令：

  ```bash
  opencc --help | head -1
  ls /opt/homebrew/share/android-commandlinetools/{platforms,build-tools,ndk,cmake,system-images}
  ```

  Expected: 第一行印出 `Open Chinese Convert (OpenCC) Command Line Tool`，而且上面列出的 SDK 套件都存在。注意：`opencc --version` 在 1.4.2 版不能用，只能用 `--help`。

- [ ] **Step 2：取得 OpenCC 原始碼（新電腦才需要，這台已經有了）**

  ```bash
  git clone -q --depth 1 --branch ver.1.4.2 https://github.com/BYVoid/OpenCC.git lib-opencc-android/src/main/jni/OpenCC
  git -C lib-opencc-android/src/main/jni/OpenCC rev-parse --short HEAD
  ```

  Expected: 印出 `025f371`。

- [ ] **Step 3：取得 gradle wrapper jar（新電腦才需要，這台已經有了）**

  ```bash
  brew install gradle
  tmp=$(mktemp -d) && (cd "$tmp" && touch settings.gradle && gradle wrapper --gradle-version 9.7.1 -q) \
    && cp "$tmp/gradle/wrapper/gradle-wrapper.jar" gradle/wrapper/
  ```

- [ ] **Step 4：把 submodule 和 wrapper 正式加進 repo**

  目的是讓其他電腦和 CI 也能直接 build。

  ```bash
  git submodule add https://github.com/BYVoid/OpenCC.git lib-opencc-android/src/main/jni/OpenCC
  git diff --cached --stat
  git diff --cached .gitmodules
  ```

  Expected: `git diff --cached --stat` 顯示 `lib-opencc-android/src/main/jni/OpenCC` 是新的 gitlink（commit `025f371…`）。

  `.gitmodules` 不能出現重複的 `[submodule ...]` 區塊。如果出現重複，手動刪掉多的那一段，保留原本那段，包括「Pinned to the ver.1.4.2 release tag」的註解。

  ```bash
  git add gradle/wrapper/gradle-wrapper.jar .gitmodules
  git commit -m "chore: add gradle wrapper jar and register OpenCC submodule"
  git submodule status
  ```

  Expected: `git submodule status` 印出 `025f371… lib-opencc-android/src/main/jni/OpenCC (ver.1.4.2)` 這類格式的一行。

- [ ] **Step 5：準備兩台模擬器並確認基準 build**

  ```bash
  echo no | avdmanager create avd -n api24 -k "system-images;android-24;google_apis;arm64-v8a"
  echo no | avdmanager create avd -n api34 -k "system-images;android-34;google_apis;arm64-v8a"
  /opt/homebrew/share/android-commandlinetools/emulator/emulator -list-avds
  ./gradlew :hymnchtv:assembleDebug :hymnchtv:lintDebug --console=plain
  for k in MissingTranslation ExtraTranslation HardcodedText; do
    printf "%s: " $k; grep -c "\[$k\]" hymnchtv/build/reports/lint-results-debug.txt
  done
  ```

  Expected:
  - `-list-avds` 列出 `api24` 和 `api34`。
  - build 結果為 `BUILD SUCCESSFUL`。
  - lint 數量為 `0 / 0 / 8`，和上面記錄的基準值相同。

  之後各個 task 裡提到「Task 0 記錄的基準值」，指的就是 `MissingTranslation 0`、`ExtraTranslation 0`、`HardcodedText 8`。

  啟動模擬器：`/opt/homebrew/share/android-commandlinetools/emulator/emulator -avd api34 &`。需要跑 instrumented test 或手動測試時再啟動就好。

---

### Task 1：建立測試基礎設施

**Files:**
- Modify: `hymnchtv/build.gradle`（`dependencies {` 區塊的最後）
- Create: `hymnchtv/src/test/java/org/cog/hymnchtv/locale/LocaleRulesTest.kt`

- [ ] **Step 1：在 `hymnchtv/build.gradle` 的 `dependencies { ... }` 區塊結尾，`implementation 'org.jsoup:jsoup:1.23.2'` 的下一行加入以下依賴**

  ```groovy
      testImplementation 'junit:junit:4.13.2'
      testImplementation 'com.google.truth:truth:1.4.5'

      androidTestImplementation 'androidx.test:runner:1.7.0'
      androidTestImplementation 'androidx.test:core:1.7.0'
      androidTestImplementation 'androidx.test.ext:junit:1.3.0'
      androidTestImplementation 'com.google.truth:truth:1.4.5'
  ```

- [ ] **Step 2：寫第一個會失敗的測試** `hymnchtv/src/test/java/org/cog/hymnchtv/locale/LocaleRulesTest.kt`

  ```kotlin
  package org.cog.hymnchtv.locale

  import com.google.common.truth.Truth.assertThat
  import org.junit.Test
  import java.util.Locale

  class LocaleRulesTest {
      private fun t(tag: String) = Locale.forLanguageTag(tag)

      @Test
      fun traditionalByRegion() {
          listOf("zh-TW", "zh-HK", "zh-MO").forEach {
              assertThat(LocaleRules.isTraditional(t(it))).isTrue()
          }
      }

      @Test
      fun traditionalByScript() {
          listOf("zh-Hant", "zh-Hant-CN").forEach {
              assertThat(LocaleRules.isTraditional(t(it))).isTrue()
          }
      }

      @Test
      fun simplified() {
          listOf("zh-CN", "zh-SG", "zh", "zh-Hans-TW").forEach {
              assertThat(LocaleRules.isTraditional(t(it))).isFalse()
          }
      }

      @Test
      fun nonChineseIsNeverTraditional() {
          listOf("en-US", "ja-JP", "fr").forEach {
              assertThat(LocaleRules.isTraditional(t(it))).isFalse()
          }
      }

      @Test
      fun isChinese() {
          assertThat(LocaleRules.isChinese(t("zh-TW"))).isTrue()
          assertThat(LocaleRules.isChinese(t("en"))).isFalse()
      }
  }
  ```

- [ ] **Step 3：執行測試，確認它失敗**

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.locale.LocaleRulesTest'`
  Expected: 編譯失敗，訊息為 `Unresolved reference 'LocaleRules'`。這同時證明 `src/test` 的 Kotlin 有被編譯。

- [ ] **Step 4：寫最小實作** `hymnchtv/src/main/java/org/cog/hymnchtv/locale/LocaleRules.kt`

  ```kotlin
  package org.cog.hymnchtv.locale

  import java.util.Locale

  /** Pure rules for classifying a [Locale]; no Android dependency. */
  object LocaleRules {
      private val TRADITIONAL_REGIONS = setOf("TW", "HK", "MO")

      @JvmStatic
      fun isChinese(locale: Locale): Boolean = locale.language == "zh"

      /** An explicit script wins over region: zh-Hans-TW is simplified, zh-Hant-CN is traditional. */
      @JvmStatic
      fun isTraditional(locale: Locale): Boolean {
          if (!isChinese(locale)) return false
          if (locale.script.isNotEmpty()) return locale.script == "Hant"
          return locale.country in TRADITIONAL_REGIONS
      }
  }
  ```

- [ ] **Step 5：執行測試，確認它通過**

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.locale.LocaleRulesTest'`
  Expected: BUILD SUCCESSFUL，5 個測試全部通過。

- [ ] **Step 6：Commit**

  ```bash
  git add hymnchtv/build.gradle hymnchtv/src/test hymnchtv/src/main/java/org/cog/hymnchtv/locale/LocaleRules.kt
  git commit -m "test: add JVM test infrastructure and LocaleRules"
  ```

---

### Task 2：AppLanguage

**Files:**
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/locale/AppLanguage.kt`
- Test: `hymnchtv/src/test/java/org/cog/hymnchtv/locale/AppLanguageTest.kt`

- [ ] **Step 1：寫會失敗的測試**

  ```kotlin
  package org.cog.hymnchtv.locale

  import com.google.common.truth.Truth.assertThat
  import org.junit.Test
  import java.util.Locale

  class AppLanguageTest {
      @Test
      fun fromPrefAcceptsPersistedValues() {
          assertThat(AppLanguage.fromPref("system")).isEqualTo(AppLanguage.SYSTEM)
          assertThat(AppLanguage.fromPref("zh-Hans-CN")).isEqualTo(AppLanguage.ZH_HANS)
          assertThat(AppLanguage.fromPref("zh-Hant-TW")).isEqualTo(AppLanguage.ZH_HANT)
          assertThat(AppLanguage.fromPref("en-US")).isEqualTo(AppLanguage.EN)
      }

      @Test
      fun fromPrefReturnsNullForMissingOrIllegal() {
          listOf(null, "", "  ", "ja-JP", "garbage").forEach {
              assertThat(AppLanguage.fromPref(it)).isNull()
          }
      }

      @Test
      fun prefValueRoundTrips() {
          AppLanguage.values().forEach {
              assertThat(AppLanguage.fromPref(it.prefValue)).isEqualTo(it)
          }
      }

      @Test
      fun fromFrameworkTagsEmptyIsSystem() {
          assertThat(AppLanguage.fromFrameworkTags(emptyList())).isEqualTo(AppLanguage.SYSTEM)
      }

      @Test
      fun fromFrameworkTagsMapsChineseVariants() {
          listOf("zh-TW", "zh-HK", "zh-MO", "zh-Hant-TW").forEach {
              assertThat(AppLanguage.fromFrameworkTags(listOf(it))).isEqualTo(AppLanguage.ZH_HANT)
          }
          listOf("zh-CN", "zh-Hans-CN", "zh").forEach {
              assertThat(AppLanguage.fromFrameworkTags(listOf(it))).isEqualTo(AppLanguage.ZH_HANS)
          }
      }

      @Test
      fun fromFrameworkTagsUsesFirstAndFallsBackToEnglish() {
          assertThat(AppLanguage.fromFrameworkTags(listOf("en-GB", "zh-TW"))).isEqualTo(AppLanguage.EN)
          assertThat(AppLanguage.fromFrameworkTags(listOf("ja-JP"))).isEqualTo(AppLanguage.EN)
      }

      @Test
      fun toLocale() {
          assertThat(AppLanguage.SYSTEM.toLocale()).isNull()
          assertThat(AppLanguage.ZH_HANT.toLocale()).isEqualTo(Locale.forLanguageTag("zh-Hant-TW"))
      }
  }
  ```

- [ ] **Step 2：執行測試，確認它失敗**

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.locale.AppLanguageTest'`
  Expected: 編譯失敗，訊息為 `Unresolved reference 'AppLanguage'`。

- [ ] **Step 3：實作**

  ```kotlin
  package org.cog.hymnchtv.locale

  import java.util.Locale

  /**
   * UI language choice. [tag] is the BCP-47 tag applied to resources; null means follow the system.
   * [prefValue] is what is persisted in PREF_LOCALE (API < 33).
   */
  enum class AppLanguage(val tag: String?) {
      SYSTEM(null), ZH_HANS("zh-Hans-CN"), ZH_HANT("zh-Hant-TW"), EN("en-US");

      val prefValue: String get() = tag ?: SYSTEM_PREF_VALUE

      fun toLocale(): Locale? = tag?.let(Locale::forLanguageTag)

      companion object {
          private const val SYSTEM_PREF_VALUE = "system"

          /** Never throws: null, blank or unknown values return null so callers pick the fallback. */
          @JvmStatic
          fun fromPref(value: String?): AppLanguage? {
              if (value.isNullOrBlank()) return null
              return values().firstOrNull { it.prefValue == value }
          }

          /** Maps the framework per-app locale list (API 33+); unsupported languages display as English. */
          @JvmStatic
          fun fromFrameworkTags(tags: List<String>): AppLanguage {
              val first = tags.firstOrNull() ?: return SYSTEM
              val locale = Locale.forLanguageTag(first)
              return when {
                  LocaleRules.isTraditional(locale) -> ZH_HANT
                  LocaleRules.isChinese(locale) -> ZH_HANS
                  else -> EN
              }
          }
      }
  }
  ```

- [ ] **Step 4：執行測試，確認它通過**

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.locale.AppLanguageTest'`
  Expected: 7 個測試全部通過。

- [ ] **Step 5：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/locale/AppLanguage.kt hymnchtv/src/test/java/org/cog/hymnchtv/locale/AppLanguageTest.kt
  git commit -m "feat: add AppLanguage model with pref and framework tag mapping"
  ```

---

### Task 4：LyricsLang、HantVariant 與 LyricsLanguagePolicy

**Files:**
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/lyrics/LyricsLang.kt`
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/lyrics/HantVariant.kt`
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/lyrics/LyricsLanguagePolicy.kt`
- Test: `hymnchtv/src/test/java/org/cog/hymnchtv/lyrics/LyricsLanguagePolicyTest.kt`

- [ ] **Step 1：寫會失敗的測試**

  ```kotlin
  package org.cog.hymnchtv.lyrics

  import com.google.common.truth.Truth.assertThat
  import com.zqc.opencc.android.lib.ConversionType
  import org.junit.Test
  import java.util.Locale

  class LyricsLanguagePolicyTest {
      private val hans = Locale.forLanguageTag("zh-Hans-CN")
      private val hantTw = Locale.forLanguageTag("zh-Hant-TW")
      private val hantHk = Locale.forLanguageTag("zh-Hant-HK")
      private val en = Locale.forLanguageTag("en-US")

      @Test
      fun lyricsLangFromPrefIsTotal() {
          assertThat(LyricsLang.fromPref("TRADITIONAL")).isEqualTo(LyricsLang.TRADITIONAL)
          assertThat(LyricsLang.fromPref("SIMPLIFIED")).isEqualTo(LyricsLang.SIMPLIFIED)
          listOf(null, "", "bogus", "traditional").forEach {
              assertThat(LyricsLang.fromPref(it)).isEqualTo(LyricsLang.FOLLOW_UI)
          }
      }

      @Test
      fun explicitChoiceIgnoresUi() {
          listOf(hans, hantTw, en).forEach {
              assertThat(LyricsLanguagePolicy.resolveShowTraditional(LyricsLang.TRADITIONAL, it)).isTrue()
              assertThat(LyricsLanguagePolicy.resolveShowTraditional(LyricsLang.SIMPLIFIED, it)).isFalse()
          }
      }

      @Test
      fun followUi() {
          assertThat(LyricsLanguagePolicy.resolveShowTraditional(LyricsLang.FOLLOW_UI, hantTw)).isTrue()
          assertThat(LyricsLanguagePolicy.resolveShowTraditional(LyricsLang.FOLLOW_UI, hantHk)).isTrue()
          assertThat(LyricsLanguagePolicy.resolveShowTraditional(LyricsLang.FOLLOW_UI, hans)).isFalse()
          assertThat(LyricsLanguagePolicy.resolveShowTraditional(LyricsLang.FOLLOW_UI, en)).isFalse()
      }

      @Test
      fun hantVariantMetadata() {
          assertThat(HantVariant.TW.conversion).isEqualTo(ConversionType.S2TW)
          assertThat(HantVariant.HK.conversion).isEqualTo(ConversionType.S2HK)
          assertThat(HantVariant.TW.prefValue).isEqualTo("S2TW")
          assertThat(HantVariant.HK.dirSuffix).isEqualTo("_hant_hk")
      }

      @Test
      fun defaultVariant() {
          assertThat(LyricsLanguagePolicy.defaultVariant(hantHk)).isEqualTo(HantVariant.HK)
          assertThat(LyricsLanguagePolicy.defaultVariant(Locale.forLanguageTag("zh-MO"))).isEqualTo(HantVariant.HK)
          listOf(hantTw, hans, en).forEach {
              assertThat(LyricsLanguagePolicy.defaultVariant(it)).isEqualTo(HantVariant.TW)
          }
      }

      @Test
      fun parseVariantReadsStoredValues() {
          assertThat(LyricsLanguagePolicy.parseVariant("S2HK", hans)).isEqualTo(HantVariant.HK)
          assertThat(LyricsLanguagePolicy.parseVariant("S2TW", hantHk)).isEqualTo(HantVariant.TW)
      }

      @Test
      fun parseVariantFallsBackToLocaleDefaultWithoutThrowing() {
          listOf(null, "", "S2T", "S2TWP", "T2S", "s2tw", "bogus").forEach {
              assertThat(LyricsLanguagePolicy.parseVariant(it, hantHk)).isEqualTo(HantVariant.HK)
              assertThat(LyricsLanguagePolicy.parseVariant(it, hantTw)).isEqualTo(HantVariant.TW)
          }
      }

      @Test
      fun canonicalValues() {
          assertThat(LyricsLanguagePolicy.isCanonical("S2TW")).isTrue()
          assertThat(LyricsLanguagePolicy.isCanonical("S2HK")).isTrue()
          listOf(null, "S2T", "S2TWP", "bogus").forEach {
              assertThat(LyricsLanguagePolicy.isCanonical(it)).isFalse()
          }
      }
  }
  ```

- [ ] **Step 2：執行測試，確認它失敗**

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.lyrics.LyricsLanguagePolicyTest'`
  Expected: 編譯失敗，出現 `Unresolved reference 'LyricsLang'`。

- [ ] **Step 3：實作 `LyricsLang.kt`**

  ```kotlin
  package org.cog.hymnchtv.lyrics

  /** Default lyrics script; persisted by [name] under LyricsLanguagePolicy.PREF_LYRICS_DEFAULT. */
  enum class LyricsLang {
      FOLLOW_UI, SIMPLIFIED, TRADITIONAL;

      companion object {
          /** Never throws; unknown values mean FOLLOW_UI. */
          @JvmStatic
          fun fromPref(value: String?): LyricsLang = values().firstOrNull { it.name == value } ?: FOLLOW_UI
      }
  }
  ```

- [ ] **Step 4：實作 `HantVariant.kt`**

  ```kotlin
  package org.cog.hymnchtv.lyrics

  import com.zqc.opencc.android.lib.ConversionType

  /**
   * Regional Traditional Chinese lyrics variant. Each one has pre-generated assets in
   * lyrics_<type>_text<dirSuffix>/ (plan A.1.9); [conversion] is used only as a runtime fallback.
   */
  enum class HantVariant(val conversion: ConversionType, val dirSuffix: String) {
      TW(ConversionType.S2TW, "_hant_tw"),
      HK(ConversionType.S2HK, "_hant_hk");

      /** Value stored in PREF_CONVERSION_TYPE. */
      val prefValue: String get() = conversion.name
  }
  ```

- [ ] **Step 5：實作 `LyricsLanguagePolicy.kt`**

  ```kotlin
  package org.cog.hymnchtv.lyrics

  import org.cog.hymnchtv.locale.LocaleRules
  import java.util.Locale

  object LyricsLanguagePolicy {
      const val PREF_LYRICS_DEFAULT = "LyricsDefaultLang"

      private val HK_REGIONS = setOf("HK", "MO")

      @JvmStatic
      fun resolveShowTraditional(pref: LyricsLang, uiLocale: Locale): Boolean = when (pref) {
          LyricsLang.SIMPLIFIED -> false
          LyricsLang.TRADITIONAL -> true
          LyricsLang.FOLLOW_UI -> LocaleRules.isTraditional(uiLocale)
      }

      @JvmStatic
      fun defaultVariant(uiLocale: Locale): HantVariant =
          if (LocaleRules.isChinese(uiLocale) && uiLocale.country in HK_REGIONS) HantVariant.HK else HantVariant.TW

      /** True only for values written by this version ("S2TW"/"S2HK"). */
      @JvmStatic
      fun isCanonical(value: String?): Boolean = HantVariant.values().any { it.prefValue == value }

      /** Never throws; a missing or invalid value follows the UI region. */
      @JvmStatic
      fun parseVariant(value: String?, uiLocale: Locale): HantVariant =
          HantVariant.values().firstOrNull { it.prefValue == value } ?: defaultVariant(uiLocale)
  }
  ```

- [ ] **Step 6：執行測試，確認它通過**

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.lyrics.LyricsLanguagePolicyTest'`
  Expected: 8 個測試全部通過。

- [ ] **Step 7：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/lyrics hymnchtv/src/test/java/org/cog/hymnchtv/lyrics
  git commit -m "feat: add lyrics default language policy and Hant variants"
  ```

---

### Task 6：SearchPattern

**Files:**
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/search/SearchPattern.kt`
- Test: `hymnchtv/src/test/java/org/cog/hymnchtv/search/SearchPatternTest.kt`

- [ ] **Step 1：寫會失敗的測試**

  ```kotlin
  package org.cog.hymnchtv.search

  import com.google.common.truth.Truth.assertThat
  import org.junit.Test

  class SearchPatternTest {
      private fun finds(query: String, text: String) = SearchPattern.build(query)!!.matcher(text).find()

      @Test
      fun blankQueryReturnsNull() {
          listOf("", "   ", "\t\n").forEach { assertThat(SearchPattern.build(it)).isNull() }
      }

      @Test
      fun regexMetaCharactersAreLiteralAndNeverThrow() {
          listOf("(", "[", "*", "\\", "\\E", "a|b", "?", "+", "{2}", "^$", ".").forEach {
              assertThat(finds(it, "xx${it}yy")).isTrue()
          }
          assertThat(finds(".", "abc")).isFalse()
      }

      @Test
      fun heMatchesBothHeAndHim() {
          assertThat(finds("他", "祂")).isTrue()
          assertThat(finds("他", "他")).isTrue()
          assertThat(finds("跟随他", "跟随祂走")).isTrue()
          assertThat(finds("他爱他", "祂爱他")).isTrue()
          assertThat(finds("他", "|")).isFalse()
      }

      @Test
      fun himOnlyMatchesHim() {
          assertThat(finds("祂", "祂")).isTrue()
          assertThat(finds("祂", "他")).isFalse()
      }

      @Test
      fun heAtEdgesWithMetaCharacters() {
          assertThat(finds("他(", "祂(")).isTrue()
          assertThat(finds("(他", "(祂")).isTrue()
      }

      @Test
      fun innerSpacesKeptOuterTrimmed() {
          assertThat(finds("  主 耶稣  ", "主 耶稣")).isTrue()
          assertThat(finds("主 耶稣", "主耶稣")).isFalse()
      }

      @Test
      fun surrogatePairs() {
          assertThat(finds("𠀀他", "𠀀祂")).isTrue()
          assertThat(finds("🙏", "a🙏b")).isTrue()
      }
  }
  ```

- [ ] **Step 2：執行測試，確認它失敗**

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.search.SearchPatternTest'`
  Expected: 編譯失敗，訊息為 `Unresolved reference 'SearchPattern'`。

- [ ] **Step 3：實作**

  ```kotlin
  package org.cog.hymnchtv.search

  import java.util.regex.Pattern

  /**
   * Builds a literal search pattern from user input. Only "他" is widened to also match "祂";
   * every other character, including regex meta characters, is matched literally.
   */
  object SearchPattern {
      private const val HE = "他"
      private const val HE_OR_HIM = "[祂他]"

      /** @return null when the query is blank; never throws PatternSyntaxException. */
      @JvmStatic
      fun build(query: String?): Pattern? {
          val q = query?.trim().orEmpty()
          if (q.isEmpty()) return null
          // Quote each segment separately; quoting the whole string first would also quote HE_OR_HIM.
          val regex = q.split(HE).joinToString(HE_OR_HIM) { if (it.isEmpty()) "" else Pattern.quote(it) }
          return Pattern.compile(regex)
      }
  }
  ```

- [ ] **Step 4：執行測試，確認它通過**

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.search.SearchPatternTest'`
  Expected: 7 個測試全部通過。

- [ ] **Step 5：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/search hymnchtv/src/test/java/org/cog/hymnchtv/search
  git commit -m "feat: add literal search pattern builder"
  ```

---

### Task 6B：預先產生繁體歌詞與同步測試（spec A.1.9）

**Files:**
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/lyrics/LyricsAssets.kt`
- Test: `hymnchtv/src/test/java/org/cog/hymnchtv/lyrics/LyricsAssetsTest.kt`
- Create: `tools/gen_lyrics_hant.py`
- Create: `tools/lyrics_hant_overrides.tsv`
- Test: `hymnchtv/src/test/java/org/cog/hymnchtv/lyrics/LyricsHantAssetsTest.kt`
- Generate (commit): `hymnchtv/src/main/assets/lyrics_*_text_hant_tw/`、`lyrics_*_text_hant_hk/`、`hymnchtv/src/main/assets/lyrics_hant_manifest.txt`
- Generate (commit): `docs/superpowers/plans/lyrics-hant-review.csv`

- [ ] **Step 1：為路徑對應寫會失敗的測試** `LyricsAssetsTest.kt`

  ```kotlin
  package org.cog.hymnchtv.lyrics

  import com.google.common.truth.Truth.assertThat
  import org.junit.Test

  class LyricsAssetsTest {
      @Test
      fun mapsSimplifiedPathToVariantDir() {
          assertThat(LyricsAssets.hantPath("lyrics_db_text/db1.txt", HantVariant.TW))
              .isEqualTo("lyrics_db_text_hant_tw/db1.txt")
          assertThat(LyricsAssets.hantPath("lyrics_er_text/er12.txt", HantVariant.HK))
              .isEqualTo("lyrics_er_text_hant_hk/er12.txt")
      }

      @Test
      fun rejectsPathsWithoutDirectory() {
          assertThat(LyricsAssets.hantPath("db1.txt", HantVariant.TW)).isNull()
          assertThat(LyricsAssets.hantPath("", HantVariant.TW)).isNull()
      }
  }
  ```

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.lyrics.LyricsAssetsTest'`
  Expected: 編譯失敗，出現 `Unresolved reference 'LyricsAssets'`。

- [ ] **Step 2：實作 `LyricsAssets.kt`**

  ```kotlin
  package org.cog.hymnchtv.lyrics

  /** Asset path rules for the pre-generated Traditional Chinese lyrics (plan A.1.9). */
  object LyricsAssets {
      /** "lyrics_db_text/db1.txt" + TW -> "lyrics_db_text_hant_tw/db1.txt"; null if [simplifiedPath] has no directory. */
      @JvmStatic
      fun hantPath(simplifiedPath: String, variant: HantVariant): String? {
          val slash = simplifiedPath.indexOf('/')
          if (slash <= 0) return null
          return simplifiedPath.substring(0, slash) + variant.dirSuffix + simplifiedPath.substring(slash)
      }
  }
  ```

  Run 同一個測試。
  Expected: 2 個測試全部通過。

- [ ] **Step 3：讓測試取得 assets 路徑，並寫會失敗的同步測試**

  不依賴測試的工作目錄（rev 5 Codex P2）。在 `hymnchtv/build.gradle` 的 `android { ... }` 區塊內（`lint {` 之前）加入：

  ```groovy
      testOptions {
          unitTests.all {
              it.systemProperty 'hymnchtv.assetsDir', file('src/main/assets').absolutePath
          }
      }
  ```

  新增 `hymnchtv/src/test/java/org/cog/hymnchtv/lyrics/LyricsHantAssetsTest.kt`：

  ```kotlin
  package org.cog.hymnchtv.lyrics

  import com.google.common.truth.Truth.assertThat
  import org.junit.Test
  import java.io.File
  import java.security.MessageDigest

  /**
   * Fails when lyrics sources, generated Traditional outputs, the overrides table or the generator
   * change without re-running tools/gen_lyrics_hant.py (plan A.1.9).
   */
  class LyricsHantAssetsTest {
      private val assets = File(checkNotNull(System.getProperty("hymnchtv.assetsDir")) { "hymnchtv.assetsDir not set" })
      private val repoRoot = assets.resolve("../../../..").canonicalFile
      private val sourceDirs: List<File> =
          assets.listFiles { f -> f.isDirectory && SOURCE_DIR.matches(f.name) }!!.sortedBy { it.name }

      private fun txtNames(dir: File): Set<String> =
          dir.listFiles { f -> f.isFile && f.name.endsWith(".txt") }?.map { it.name }?.toSet() ?: emptySet()

      private fun sha1(file: File): String =
          MessageDigest.getInstance("SHA-1").digest(file.readBytes()).joinToString("") { "%02x".format(it) }

      /** Strict parse: "#input<TAB>name<TAB>sha1" header rows and "path<TAB>sha1" rows, no duplicates. */
      private fun manifest(): Pair<Map<String, String>, Map<String, String>> {
          val inputs = mutableMapOf<String, String>()
          val files = mutableMapOf<String, String>()
          File(assets, "lyrics_hant_manifest.txt").readLines().filter { it.isNotBlank() && !it.startsWith("# ") }.forEach { line ->
              val cols = line.split('\t')
              if (cols[0] == "#input") {
                  check(cols.size == 3 && SHA1.matches(cols[2])) { "bad manifest input row: $line" }
                  check(inputs.put(cols[1], cols[2]) == null) { "duplicate input: ${cols[1]}" }
              } else {
                  check(cols.size == 2 && PATH.matches(cols[0]) && SHA1.matches(cols[1])) { "bad manifest row: $line" }
                  check(files.put(cols[0], cols[1]) == null) { "duplicate path: ${cols[0]}" }
              }
          }
          return inputs to files
      }

      @Test
      fun sourceDirectoriesFound() {
          assertThat(sourceDirs.map { it.name }).containsAtLeast("lyrics_db_text", "lyrics_bb_text")
      }

      @Test
      fun outputDirectoriesAreExactlyTheExpectedOnes() {
          val expected = sourceDirs.flatMap { d -> HantVariant.values().map { d.name + it.dirSuffix } }.toSet()
          val actual = assets.listFiles { f -> f.isDirectory && f.name.contains("_hant_") }!!.map { it.name }.toSet()
          assertThat(actual).isEqualTo(expected)
      }

      @Test
      fun everyVariantMirrorsTheSourceFileSet() {
          for (dir in sourceDirs) {
              for (variant in HantVariant.values()) {
                  assertThat(txtNames(File(assets, dir.name + variant.dirSuffix))).isEqualTo(txtNames(dir))
              }
          }
      }

      @Test
      fun manifestMatchesSourcesAndOutputs() {
          val actual = sourceDirs.flatMap { dir ->
              listOf(dir) + HantVariant.values().map { File(assets, dir.name + it.dirSuffix) }
          }.flatMap { dir -> txtNames(dir).map { "${dir.name}/$it" to sha1(File(dir, it)) } }.toMap()
          assertThat(manifest().second).isEqualTo(actual)
      }

      @Test
      fun manifestMatchesGeneratorInputs() {
          val inputs = manifest().first
          assertThat(inputs["tools/gen_lyrics_hant.py"]).isEqualTo(sha1(File(repoRoot, "tools/gen_lyrics_hant.py")))
          assertThat(inputs["tools/lyrics_hant_overrides.tsv"]).isEqualTo(sha1(File(repoRoot, "tools/lyrics_hant_overrides.tsv")))
      }

      private companion object {
          val SOURCE_DIR = Regex("lyrics_[a-z]+_text")
          val PATH = Regex("lyrics_[a-z]+_text(_hant_(tw|hk))?/[^/\t]+\\.txt")
          val SHA1 = Regex("[0-9a-f]{40}")
      }
  }
  ```

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.lyrics.LyricsHantAssetsTest'`
  Expected: 只有 `sourceDirectoriesFound` 通過，其餘 4 個測試失敗，因為還沒產生任何檔案。

- [ ] **Step 4：新增空的校對表** `tools/lyrics_hant_overrides.tsv`

  每一條校對都限定在單一檔案，並寫明預期的替換次數；實際次數不符時，產生工具會直接失敗（rev 5 Codex P1）。

  ```
  # Manual corrections applied after OpenCC (plan A.1.9). One rule = one file, exact expected hit count.
  # Format: variant<TAB>source-path<TAB>from<TAB>to<TAB>count
  #   variant     tw | hk | *        (* = both variants)
  #   source-path Simplified source relative to assets, e.g. lyrics_db_text/db12.txt
  #   from / to   Traditional text as produced by OpenCC / corrected text
  #   count       exact number (>= 1) of occurrences of "from" in that file (per variant); mismatch = error
  # Example (do not uncomment unless verified):
  # *	lyrics_db_text/db12.txt	皇後	皇后	1
  ```

- [ ] **Step 5：新增產生工具** `tools/gen_lyrics_hant.py`

  ```python
  #!/usr/bin/env python3
  """Generate Traditional Chinese lyrics assets from the Simplified sources with OpenCC (plan A.1.9).

  Usage: tools/gen_lyrics_hant.py [--report]
  Requires the OpenCC CLI (brew install opencc). Output is deterministic for the same inputs.
  """
  import argparse
  import csv
  import hashlib
  import pathlib
  import shutil
  import subprocess
  import sys

  ROOT = pathlib.Path(__file__).resolve().parent.parent
  ASSETS = ROOT / "hymnchtv/src/main/assets"
  GENERATOR = pathlib.Path(__file__).resolve()
  OVERRIDES = ROOT / "tools/lyrics_hant_overrides.tsv"
  MANIFEST = ASSETS / "lyrics_hant_manifest.txt"
  REPORT = ROOT / "docs/superpowers/plans/lyrics-hant-review.csv"
  VARIANTS = {"tw": "s2tw.json", "hk": "s2hk.json"}
  SPLIT = "\n@@@HYMNCHTV_SPLIT@@@\n"  # ASCII marker survives OpenCC unchanged
  AMBIGUOUS = set("于里后复发只干历面云台余松谷斗志准范冲尽获系钟制致表卷借恶征党丑")


  def sha1(data: bytes) -> str:
      return hashlib.sha1(data).hexdigest()


  def source_dirs():
      return sorted(d for d in ASSETS.glob("lyrics_*_text") if d.is_dir())


  def source_files():
      return [f for d in source_dirs() for f in sorted(d.glob("*.txt"))]


  def rel(path):
      return path.relative_to(ASSETS).as_posix()


  def read(path):
      # bytes -> str keeps CRLF line endings exactly
      return path.read_bytes().decode("utf-8")


  def opencc_batch(texts, config):
      joined = SPLIT.join(texts)
      if joined.count(SPLIT) != len(texts) - 1:
          sys.exit("A lyrics file contains the split marker")
      result = subprocess.run(["opencc", "-c", config], input=joined.encode("utf-8"),
                              capture_output=True, check=True)
      parts = result.stdout.decode("utf-8").split(SPLIT)
      if len(parts) != len(texts):
          sys.exit(f"OpenCC changed the split marker: {len(parts)} parts for {len(texts)} files")
      return parts


  def load_overrides(known_sources):
      rules = []
      if not OVERRIDES.exists():
          return rules
      for n, line in enumerate(OVERRIDES.read_text(encoding="utf-8").splitlines(), 1):
          if not line.strip() or line.startswith("#"):
              continue
          cols = line.split("\t")
          if len(cols) != 5 or cols[0] not in ("tw", "hk", "*") or not cols[2] or not cols[4].isdigit() or int(cols[4]) < 1:
              sys.exit(f"{OVERRIDES.name}:{n}: expected 'variant<TAB>source-path<TAB>from<TAB>to<TAB>count'")
          if cols[1] not in known_sources:
              sys.exit(f"{OVERRIDES.name}:{n}: unknown source file {cols[1]}")
          rules.append((n, cols[0], cols[1], cols[2], cols[3], int(cols[4])))
      return rules


  def apply_overrides(text, variant, source, rules):
      for n, v, path, frm, to, count in rules:
          if path != source or v not in (variant, "*"):
              continue
          hits = text.count(frm)
          if hits != count:
              sys.exit(f"{OVERRIDES.name}:{n}: expected {count} hit(s) of '{frm}' in {source} [{variant}], found {hits}")
          text = text.replace(frm, to)
      return text


  def clean_stale(expected_dirs, expected_files):
      for d in ASSETS.glob("lyrics_*_text_hant_*"):
          if d.is_dir() and d.name not in expected_dirs:
              shutil.rmtree(d)
      for d in expected_dirs:
          for f in (ASSETS / d).iterdir():
              if f.is_dir():
                  shutil.rmtree(f)
              elif rel(f) not in expected_files:
                  f.unlink()


  def main():
      parser = argparse.ArgumentParser()
      parser.add_argument("--report", action="store_true", help=f"also write {REPORT.relative_to(ROOT)}")
      args = parser.parse_args()
      if shutil.which("opencc") is None:
          sys.exit("opencc not found: brew install opencc")

      sources = source_files()
      texts = [read(p) for p in sources]
      rules = load_overrides({rel(p) for p in sources})
      converted = {}
      expected_dirs, expected_files = set(), set()
      for variant, config in VARIANTS.items():
          out = [apply_overrides(t, variant, rel(src), rules) for src, t in zip(sources, opencc_batch(texts, config))]
          converted[variant] = out
          for src, text in zip(sources, out):
              dst_dir = ASSETS / f"{src.parent.name}_hant_{variant}"
              dst_dir.mkdir(exist_ok=True)
              (dst_dir / src.name).write_bytes(text.encode("utf-8"))
              expected_dirs.add(dst_dir.name)
              expected_files.add(rel(dst_dir / src.name))
      clean_stale(expected_dirs, expected_files)

      rows = [f"#input\t{rel_path}\t{sha1(path.read_bytes())}"
              for rel_path, path in (("tools/gen_lyrics_hant.py", GENERATOR), ("tools/lyrics_hant_overrides.tsv", OVERRIDES))]
      outputs = sources + [ASSETS / f for f in sorted(expected_files)]
      rows += sorted(f"{rel(p)}\t{sha1(p.read_bytes())}" for p in outputs)
      version = "opencc unknown"  # opencc 1.4.2 has no --version flag; ask Homebrew when available
      if shutil.which("brew"):
          brew = subprocess.run(["brew", "list", "--versions", "opencc"], capture_output=True, text=True)
          version = brew.stdout.strip() or version
      header = f"# generated by tools/gen_lyrics_hant.py; {version}"
      MANIFEST.write_text(header + "\n" + "\n".join(rows) + "\n", encoding="utf-8")

      if args.report:
          with REPORT.open("w", encoding="utf-8", newline="") as fh:
              writer = csv.writer(fh)
              writer.writerow(["file", "line", "chars", "simplified", "tw", "hk"])
              for i, src in enumerate(sources):
                  s_lines = texts[i].splitlines()
                  tw_lines = converted["tw"][i].splitlines()
                  hk_lines = converted["hk"][i].splitlines()
                  for ln, s in enumerate(s_lines):
                      hits = sorted(AMBIGUOUS.intersection(s))
                      if hits:
                          writer.writerow([rel(src), ln + 1, "".join(hits), s, tw_lines[ln], hk_lines[ln]])
      print(f"Generated {len(sources)} files x {len(VARIANTS)} variants")


  if __name__ == "__main__":
      main()
  ```

  **已知限制**：OpenCC 的版本只記在 manifest 的說明行，不會被驗證。升級 OpenCC 之後必須手動重新產生一次，並用 `git diff --stat hymnchtv/src/main/assets` 檢查有沒有變化（rev 6 Codex P2，刻意不做成自動檢查）。

  manifest 的格式：
  - 第一行是 `# generated ...`，記錄 OpenCC 的版本，只作為參考。
  - `#input<TAB>路徑<TAB>sha1`：產生工具和校對表的雜湊。
  - `路徑<TAB>sha1`：每個簡體來源檔和每個繁體產物檔的雜湊。

  測試解析 manifest 時會略過 `# ` 開頭的說明行（見 Step 3 的 `filter`）。

- [ ] **Step 6：產生檔案並驗證**

  Run: `chmod +x tools/gen_lyrics_hant.py && tools/gen_lyrics_hant.py --report`
  Expected: 輸出 `Generated 2142 files x 2 variants`。實際數字等於 6 個 `lyrics_*_text` 目錄的 txt 檔總數。

  抽查以下幾點：
  - `diff <(file hymnchtv/src/main/assets/lyrics_db_text/db1.txt) <(file hymnchtv/src/main/assets/lyrics_db_text_hant_tw/db1.txt)`：兩者都是 `UTF-8 text, with CRLF line terminators`，換行格式要保留。
  - `head -3 hymnchtv/src/main/assets/lyrics_db_text_hant_tw/db1.txt`：第 2 行是「頌讚三一神－祂的計劃」之類的繁體文字。
  - 再跑一次 `tools/gen_lyrics_hant.py`，然後 `git status --short hymnchtv/src/main/assets | head`：不能有任何變動，確認輸出是可重現的。

- [ ] **Step 7：執行同步測試，確認它通過**

  Run: `./gradlew :hymnchtv:testDebugUnitTest --tests 'org.cog.hymnchtv.lyrics.LyricsHantAssetsTest'`
  Expected: 5 個測試全部通過。

- [ ] **Step 8：驗證保護機制有效（每做完一項就還原，不要 commit）**

  1. 在 `lyrics_db_text/db1.txt` 的結尾加一個空白：`manifestMatchesSourcesAndOutputs` 要失敗。
  2. 直接手改 `lyrics_db_text_hant_tw/db1.txt` 的一個字：`manifestMatchesSourcesAndOutputs` 要失敗。
  3. 在校對表加一行註解：`manifestMatchesGeneratorInputs` 要失敗。
  4. 建立一個空目錄 `lyrics_zz_text_hant_tw`：`outputDirectoriesAreExactlyTheExpectedOnes` 要失敗。
  5. 在校對表加一條規則，故意寫錯次數，例如 `*	lyrics_db_text/db1.txt	神	神	99`，再執行 `tools/gen_lyrics_hant.py`：要以 `expected 99 hit(s)` 錯誤結束。

  每一項做完都只還原剛才改過的那一個檔案，不要整個目錄一起還原，以免蓋掉其他尚未 commit 的修改：
  - 第 1 項：`git checkout -- hymnchtv/src/main/assets/lyrics_db_text/db1.txt`
  - 第 2 項：`git checkout -- hymnchtv/src/main/assets/lyrics_db_text_hant_tw/db1.txt`
  - 第 3 項和第 5 項：`git checkout -- tools/lyrics_hant_overrides.tsv`
  - 第 4 項：`rmdir hymnchtv/src/main/assets/lyrics_zz_text_hant_tw`

- [ ] **Step 9：Commit**

  ```bash
  git add tools/gen_lyrics_hant.py tools/lyrics_hant_overrides.tsv \
          hymnchtv/build.gradle hymnchtv/src/main/java/org/cog/hymnchtv/lyrics/LyricsAssets.kt \
          hymnchtv/src/test/java/org/cog/hymnchtv/lyrics/LyricsAssetsTest.kt \
          hymnchtv/src/test/java/org/cog/hymnchtv/lyrics/LyricsHantAssetsTest.kt \
          hymnchtv/src/main/assets/lyrics_*_text_hant_tw hymnchtv/src/main/assets/lyrics_*_text_hant_hk \
          hymnchtv/src/main/assets/lyrics_hant_manifest.txt docs/superpowers/plans/lyrics-hant-review.csv
  git commit -m "feat: pre-generate Traditional Chinese lyrics (TW/HK) with sync test"
  ```

- [ ] **Step 10：交付校對清單（不擋後續 task）**

  把 `docs/superpowers/plans/lyrics-hant-review.csv` 交給使用者或教會同工確認。用 `chars` 欄位排序，方便逐字檢查。

  需要修正的地方寫進 `tools/lyrics_hant_overrides.tsv`。每條規則都要填檔案路徑和預期次數，計算方式是在該 variant 的產物檔裡執行 `grep -o 'from' 檔案 | wc -l`。然後重跑：

  ```bash
  tools/gen_lyrics_hant.py --report
  ./gradlew :hymnchtv:testDebugUnitTest
  ```

  另外用 `fix: correct Traditional lyrics terms` 為訊息單獨 commit。這一步可以和 Task 7～15 並行，但要在 PR 合併前完成。

---

### Task 7：資源目錄調整（預設改為英文）

**Files:**
- Move: `res/values/strings.xml` → `res/values-zh/strings.xml`
- Move: `res/values-en/strings.xml` → `res/values/strings.xml`
- Create: `hymnchtv/src/main/res/resources.properties`
- Delete: `hymnchtv/src/main/res/xml/locale_config.xml`

這個 task 只搬檔和調整語言相關的字串，**不改任何 Java 程式碼**，所以要獨立成一個 commit。

- [ ] **Step 1：搬移檔案（保留 git 歷史）**

  ```bash
  cd hymnchtv/src/main/res
  mkdir -p values-zh
  git mv values/strings.xml values-zh/strings.xml
  git mv values-en/strings.xml values/strings.xml
  rmdir values-en
  git rm xml/locale_config.xml
  cd -
  ```

  `values/array.xml`（播放速度等數值）不需要翻譯，保持原位。目前 `values` 和 `values-en` 的 key 集合完全相同，已經比對過，沒有任何一邊缺 key。

- [ ] **Step 2：新增 `hymnchtv/src/main/res/resources.properties`**

  ```properties
  unqualifiedResLocale=en-US
  ```

- [ ] **Step 3：修改預設語言檔 `res/values/strings.xml`（英文）**

  第 3～4 行：

  ```xml
      <!--string name="app_name">Hymnch</string // Fix app label in Chinese-->
      <string name="app_name">诗歌本</string>
  ```

  改成：

  ```xml
      <string name="app_name">Hymnal</string>
  ```

  `locale_menu`、`locale_chinese`、`locale_english` 這 3 行（約第 322～324 行）：

  ```xml
      <string name="locale_menu">Language</string>
      <string name="locale_chinese">中文(简体)</string>
      <string name="locale_english">English</string>
  ```

  改成下面這段。語言名稱一律用該語言自己的寫法，標示為 `translatable="false"`：

  ```xml
      <string name="locale_menu">Language</string>
      <string name="locale_system">Follow system</string>
      <string name="locale_chinese" translatable="false">简体中文</string>
      <string name="locale_chinese_hant" translatable="false">繁體中文</string>
      <string name="locale_english" translatable="false">English</string>
      <string name="lyrics_language_menu">Lyrics language</string>
      <string name="lyrics_default_title">Default lyrics language</string>
      <string name="lyrics_follow_ui">Follow app language</string>
      <string name="lyrics_simplified">Simplified Chinese</string>
      <string name="lyrics_traditional">Traditional Chinese</string>
      <string name="hymn_no_chinese_lyrics">English #%1$d: this English hymn has no matching Chinese lyrics</string>
      <string name="update_none">No updates</string>
  ```

- [ ] **Step 4：修改簡中檔 `res/values-zh/strings.xml`**

  第 4 行 `<string name="app_name">诗歌本</string>` 改成 `<string name="app_name">诗歌</string>`。

  第 299～301 行：

  ```xml
      <string name="locale_menu">应用界面语言</string>
      <string name="locale_chinese">中文(简体)</string>
      <string name="locale_english">English</string>
  ```

  改成下面這段。標示 `translatable="false"` 的字串只能放在預設的 `values/`，所以這裡要刪掉 `locale_chinese` 和 `locale_english`：

  ```xml
      <string name="locale_menu">应用界面语言</string>
      <string name="locale_system">跟随系统</string>
      <string name="lyrics_language_menu">歌词语言</string>
      <string name="lyrics_default_title">歌词默认语言</string>
      <string name="lyrics_follow_ui">跟随界面语言</string>
      <string name="lyrics_simplified">简体</string>
      <string name="lyrics_traditional">繁体</string>
      <string name="hymn_no_chinese_lyrics">英文 #%1$d: 这首英文诗歌没有匹配的中文歌词</string>
      <string name="update_none">无更新</string>
  ```

- [ ] **Step 5：Build 並檢查產生的 locale config**

  Run: `./gradlew :hymnchtv:assembleDebug && find hymnchtv/build/generated/res/localeConfig -name '*.xml'`
  Expected: BUILD SUCCESSFUL，而且找到產生出來的 `_generated_res_locale_config.xml`（或名稱相近的檔案）。

  用 `cat` 查看內容，裡面應該要有 `en-US` 和 `zh`。如果 build 報錯說 `resources.properties` 和手寫的 locale config 衝突，代表 Step 1 的刪除沒有生效，回頭確認。

- [ ] **Step 6：Lint 檢查翻譯**

  Run: `./gradlew :hymnchtv:lintDebug`，然後執行：

  ```bash
  grep -cE '\[(MissingTranslation|ExtraTranslation)\]' hymnchtv/build/reports/lint-results-debug.txt
  ```

  Expected: `0`（基準值是 0+0）。

- [ ] **Step 7：在 API 34 模擬器上把系統語言設為簡中，跑一次冒煙測試**

  Run: `./gradlew :hymnchtv:installDebug`

  確認主頁、目錄、搜尋、歌詞頁仍然是簡中，而且字串和 v2.9.2 相同。這時語言切換的程式碼還沒改，只是確認資源搬移沒有造成字串遺失。

- [ ] **Step 8：Commit**

  ```bash
  git add -A hymnchtv/src/main/res
  git commit -m "refactor: make English the default resource locale, move Simplified Chinese to values-zh"
  ```

---

### Task 8：繁中字串與資源解析測試

**Files:**
- Create: `hymnchtv/src/main/res/values-b+zh+Hant/strings.xml`
- Create: `tools/gen_zh_hant.sh`
- Create: `hymnchtv/src/androidTest/java/org/cog/hymnchtv/ResourceLocaleResolutionTest.kt`

- [ ] **Step 1：先寫會失敗的 instrumented test**

  ```kotlin
  package org.cog.hymnchtv

  import android.content.res.Configuration
  import android.os.LocaleList
  import androidx.test.ext.junit.runners.AndroidJUnit4
  import androidx.test.platform.app.InstrumentationRegistry
  import com.google.common.truth.Truth.assertThat
  import org.junit.Test
  import org.junit.runner.RunWith

  /** Verifies values / values-zh / values-b+zh+Hant are picked as designed (plan A.1.8). Run on API 24 and 34. */
  @RunWith(AndroidJUnit4::class)
  class ResourceLocaleResolutionTest {
      private fun localeSystemString(tag: String): String {
          val ctx = InstrumentationRegistry.getInstrumentation().targetContext
          val config = Configuration(ctx.resources.configuration)
          config.setLocales(LocaleList.forLanguageTags(tag))
          return ctx.createConfigurationContext(config).getString(R.string.locale_system)
      }

      @Test
      fun simplifiedChineseLocales() {
          listOf("zh-CN", "zh-SG", "zh-Hans").forEach {
              assertThat(localeSystemString(it)).isEqualTo("跟随系统")
          }
      }

      @Test
      fun traditionalChineseLocales() {
          listOf("zh-TW", "zh-HK", "zh-MO", "zh-Hant").forEach {
              assertThat(localeSystemString(it)).isEqualTo("跟隨系統")
          }
      }

      @Test
      fun otherLocalesFallBackToEnglish() {
          listOf("en-US", "ja-JP", "fr-FR").forEach {
              assertThat(localeSystemString(it)).isEqualTo("Follow system")
          }
      }
  }
  ```

- [ ] **Step 2：在 API 34 上執行，確認繁中測試失敗**

  Run: `./gradlew :hymnchtv:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=org.cog.hymnchtv.ResourceLocaleResolutionTest`
  Expected: `traditionalChineseLocales` 失敗，實際值是 `跟随系统`（因為還沒有繁中資源）；其他兩項通過。

- [ ] **Step 3：新增產生腳本 `tools/gen_zh_hant.sh`**

  ```bash
  #!/usr/bin/env bash
  # Regenerate the zh-Hant UI strings draft from Simplified Chinese. Output MUST be reviewed by hand.
  set -euo pipefail
  ROOT="$(cd "$(dirname "$0")/.." && pwd)"
  SRC="$ROOT/hymnchtv/src/main/res/values-zh/strings.xml"
  DST_DIR="$ROOT/hymnchtv/src/main/res/values-b+zh+Hant"
  command -v opencc >/dev/null || { echo "opencc not found: brew install opencc" >&2; exit 1; }
  mkdir -p "$DST_DIR"
  opencc -c s2twp.json -i "$SRC" -o "$DST_DIR/strings.xml"
  echo "Draft written to $DST_DIR/strings.xml - review every changed term before committing."
  ```

  Run: `chmod +x tools/gen_zh_hant.sh && tools/gen_zh_hant.sh`
  Expected: 產生 `values-b+zh+Hant/strings.xml`。

- [ ] **Step 4：人工校對並固定用語**

  1. `app_name` 改成 `<string name="app_name">詩歌</string>`。
  2. 確認 `locale_system` 是 `跟隨系統`。
  3. 列出 s2twp 做了詞彙替換（不只是字形轉換）的地方：

     ```bash
     diff <(opencc -c s2t.json -i hymnchtv/src/main/res/values-zh/strings.xml) hymnchtv/src/main/res/values-b+zh+Hant/strings.xml
     ```

     逐條判斷是否符合教會的慣用語。**以下詩歌本名稱必須維持原名，不能被替換**：
     - 大本詩歌
     - 補充本
     - 新歌頌詠
     - 新詩歌本
     - 青年詩歌
     - 兒童詩歌
     - 教唱
     - 唱詩
     - 伴奏

     如果被替換了，就改回 s2t 的結果。
  4. 確認所有 `%1$d`、`\'`、`&#…;` 這類格式碼和跳脫字元都完整保留：

     ```bash
     diff <(grep -o '%[0-9]\$[sd]' hymnchtv/src/main/res/values-zh/strings.xml | sort) <(grep -o '%[0-9]\$[sd]' hymnchtv/src/main/res/values-b+zh+Hant/strings.xml | sort)
     ```

     Expected: 沒有任何輸出。
  5. 把校對清單（原文 → s2t → 最終採用的寫法）存成 `docs/superpowers/plans/2026-10-02-zh-hant-terms.md`，在 PR 中請使用者確認。

- [ ] **Step 5：在 API 34 和 API 24 上執行 instrumented test，確認通過**

  Run（兩台模擬器各跑一次）：`./gradlew :hymnchtv:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=org.cog.hymnchtv.ResourceLocaleResolutionTest`
  Expected: 3 個測試全部通過。

  如果 API 24 上 zh-HK 或 zh-MO 落到 `values-zh`，就另外加 `values-zh-rHK/strings.xml` 和 `values-zh-rMO/strings.xml`，內容複製 `values-b+zh+Hant` 的版本，並在 `tools/gen_zh_hant.sh` 結尾加上 `cp` 指令，然後重跑測試。

- [ ] **Step 6：Commit**

  ```bash
  git add tools/gen_zh_hant.sh hymnchtv/src/main/res/values-b+zh+Hant hymnchtv/src/androidTest docs/superpowers/plans/2026-10-02-zh-hant-terms.md
  git commit -m "feat: add Traditional Chinese UI strings and resource resolution test"
  ```

---

### Task 9：LocaleStore，以及 HymnsApp／BaseActivity 的整合

**Files:**
- Create: `hymnchtv/src/main/java/org/cog/hymnchtv/locale/LocaleStore.kt`
- Modify: `hymnchtv/src/main/java/org/cog/hymnchtv/HymnsApp.java:100-166`
- Modify: `hymnchtv/src/main/java/org/cog/hymnchtv/BaseActivity.java:36-47`
- Delete: `hymnchtv/src/main/java/org/cog/hymnchtv/utils/LocaleHelper.java`

這個 task 的程式碼都在 Android 邊界，所以不寫 JVM 測試。判斷規則已經在 Task 2、4 測過，這裡由 Task 15 的手動測試驗證。

- [ ] **Step 1：新增 `LocaleStore.kt`**

  ```kotlin
  package org.cog.hymnchtv.locale

  import android.app.LocaleManager
  import android.content.Context
  import android.content.SharedPreferences
  import android.content.res.Configuration
  import android.os.Build
  import android.os.LocaleList
  import androidx.annotation.RequiresApi

  /**
   * Single source of truth for the UI language, chosen per API level (plan A.1.2):
   * API 33+ uses the framework per-app locale (shared with Settings > App languages);
   * API < 33 uses PREF_LOCALE and a wrapped context.
   */
  object LocaleStore {
      /** Same values as MainActivity.PREF_SETTINGS / PREF_LOCALE; duplicated to keep this class Android-light. */
      const val PREF_SETTINGS = "Settings"
      const val PREF_LOCALE = "Locale"

      @JvmStatic
      fun prefs(context: Context): SharedPreferences = context.getSharedPreferences(PREF_SETTINGS, Context.MODE_PRIVATE)

      @JvmStatic
      fun current(context: Context): AppLanguage =
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              AppLanguage.fromFrameworkTags(frameworkTags(context))
          } else {
              AppLanguage.fromPref(prefs(context).getString(PREF_LOCALE, null)) ?: AppLanguage.SYSTEM
          }

      /** @return true when the caller must restart the process to apply it (API < 33 only). */
      @JvmStatic
      fun set(context: Context, language: AppLanguage): Boolean {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              setFramework(context, language)
              return false
          }
          prefs(context).edit().putString(PREF_LOCALE, language.prefValue).commit()
          return true
      }

      /** API 33+: never wrap, the framework applies the locale. API < 33: wrap only for an explicit choice. */
      @JvmStatic
      fun wrap(base: Context): Context {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
          val locale = current(base).toLocale() ?: return base
          val config = Configuration(base.resources.configuration)
          config.setLocale(locale)
          config.setLayoutDirection(locale)
          return base.createConfigurationContext(config)
      }

      @RequiresApi(Build.VERSION_CODES.TIRAMISU)
      @JvmStatic
      fun frameworkTags(context: Context): List<String> {
          val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
          return (0 until locales.size()).map { locales[it].toLanguageTag() }
      }

      @RequiresApi(Build.VERSION_CODES.TIRAMISU)
      private fun setFramework(context: Context, language: AppLanguage) {
          val locales = language.tag?.let { LocaleList.forLanguageTags(it) } ?: LocaleList.getEmptyLocaleList()
          context.getSystemService(LocaleManager::class.java).applicationLocales = locales
      }
  }
  ```

- [ ] **Step 2：修改 `HymnsApp.attachBaseContext`（`HymnsApp.java:151-166`）**

  整個方法換成：

  ```java
      /**
       * Apply the UI language: wrapped only on API < 33 for an explicit choice (API 33+ uses the framework).
       */
      @Override
      protected void attachBaseContext(Context base) {
          mInstance = LocaleStore.wrap(base);
          super.attachBaseContext(mInstance);
      }
  ```

  import 的調整：
  - 刪除 `import org.cog.hymnchtv.utils.LocaleHelper;`、`import static org.cog.hymnchtv.MainActivity.PREF_LOCALE;`
  - 如果 `SharedPreferences` 和 `PREF_SETTINGS` 不再被使用，也一起刪除。
  - 新增 `import org.cog.hymnchtv.locale.LocaleStore;`

- [ ] **Step 3：修改 `BaseActivity.attachBaseContext`（`BaseActivity.java:36-47`）**

  ```java
      /**
       * Override AppCompatActivity#attachBaseContext() to support Locale setting; re-evaluated on every recreation.
       */
      @Override
      protected void attachBaseContext(Context base) {
          super.attachBaseContext(LocaleStore.wrap(base));
      }
  ```

  import 改成 `import org.cog.hymnchtv.locale.LocaleStore;`，並刪除 `LocaleHelper` 的 import。

- [ ] **Step 4：刪除 LocaleHelper**

  Run: `git rm hymnchtv/src/main/java/org/cog/hymnchtv/utils/LocaleHelper.java`

  `MainActivity` 還在引用它，會在 Task 10 一起修掉。這一步之後先不要 build，直接進行 Task 10，兩個 task 合併成一個 commit。

---

### Task 10：MainActivity 的選單、語言切換與狀態保存

**Files:**
- Modify: `hymnchtv/src/main/res/menu/menu_main.xml`（`appLanguage` 和 `appLocale` 兩個 item）
- Modify: `hymnchtv/src/main/java/org/cog/hymnchtv/MainActivity.java`（第 806-818、1055-1078、1233-1238 行，`onCreate`）

- [ ] **Step 1：修改 `menu_main.xml`**

  把下面這段：

  ```xml
      <item
          android:id="@+id/appLanguage"
          android:title="@string/locale_menu" />
      <item
          android:id="@+id/appLocale"
          android:title="@string/locale_menu">
          <menu>
              <item
                  android:id="@+id/localeChinese"
                  android:title="@string/locale_chinese" />
              <item
                  android:id="@+id/localeEnglish"
                  android:title="@string/locale_english" />
          </menu>
      </item>
  ```

  換成：

  ```xml
      <item
          android:id="@+id/appLocale"
          android:title="@string/locale_menu">
          <menu>
              <group android:checkableBehavior="single">
                  <item
                      android:id="@+id/localeSystem"
                      android:title="@string/locale_system" />
                  <item
                      android:id="@+id/localeChinese"
                      android:title="@string/locale_chinese" />
                  <item
                      android:id="@+id/localeChineseHant"
                      android:title="@string/locale_chinese_hant" />
                  <item
                      android:id="@+id/localeEnglish"
                      android:title="@string/locale_english" />
              </group>
          </menu>
      </item>
      <item
          android:id="@+id/lyricsLanguage"
          android:title="@string/lyrics_language_menu" />
  ```

- [ ] **Step 2：修改 `onOptionsItemSelected`（約第 806-818 行）**

  把下面這段：

  ```java
          else if (itemId == R.id.appLanguage) {
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                  setLanguage();
              }
              return true;
          }
          else if (itemId == R.id.localeChinese) {
              setAppLocale(LocaleHelper.LocaleChinese);
              return true;
          }
          else if (itemId == R.id.localeEnglish) {
              setAppLocale(LocaleHelper.LocaleEnglish);
              return true;
  ```

  換成（原本接在後面的 `// === Set font size ===` 註解和 `}` 保持不動）：

  ```java
          else if (itemId == R.id.localeSystem) {
              setAppLocale(AppLanguage.SYSTEM);
              return true;
          }
          else if (itemId == R.id.localeChinese) {
              setAppLocale(AppLanguage.ZH_HANS);
              return true;
          }
          else if (itemId == R.id.localeChineseHant) {
              setAppLocale(AppLanguage.ZH_HANT);
              return true;
          }
          else if (itemId == R.id.lyricsLanguage) {
              startActivity(new Intent(this, ChineseS2TSelection.class));
              return true;
          }
          else if (itemId == R.id.localeEnglish) {
              setAppLocale(AppLanguage.EN);
              return true;
  ```

- [ ] **Step 3：改寫 `initLanguage`，並刪除 `setLanguage`（第 1055-1078 行）**

  把 `initLanguage(Menu menu)` 整個方法，以及它下面的 `setLanguage()` 方法（連同上面的 `@RequiresApi` annotation，如果有的話），換成：

  ```java
      /**
       * Check the menu item of the current UI language; called on every menu creation so it follows recreation.
       */
      private void initLanguage(Menu menu) {
          int checkedId;
          switch (LocaleStore.current(this)) {
              case ZH_HANS:
                  checkedId = R.id.localeChinese;
                  break;
              case ZH_HANT:
                  checkedId = R.id.localeChineseHant;
                  break;
              case EN:
                  checkedId = R.id.localeEnglish;
                  break;
              default:
                  checkedId = R.id.localeSystem;
                  break;
          }
          MenuItem item = menu.findItem(checkedId);
          if (item != null) {
              item.setChecked(true);
          }
      }
  ```

- [ ] **Step 4：改寫 `setAppLocale`（第 1233-1238 行）**

  ```java
      private void setAppLocale(AppLanguage language) {
          if (language == LocaleStore.current(this)) {
              return;
          }
          // API 33+: framework applies it and recreates activities; API < 33: restart to re-wrap HymnsApp context
          if (LocaleStore.set(this, language)) {
              doRestart();
          }
      }
  ```

- [ ] **Step 5：保存主頁輸入到一半的編號**

  在 `onResume()` 方法之前新增：

  ```java
      private static final String STATE_NUMBER = "state_number";
      private static final String STATE_IS_FU = "state_is_fu";

      @Override
      protected void onSaveInstanceState(@NonNull Bundle outState) {
          super.onSaveInstanceState(outState);
          outState.putString(STATE_NUMBER, sNumber);
          outState.putBoolean(STATE_IS_FU, isFu);
      }
  ```

  在 `onCreate` 中 `initButton();` 這一行之後插入：

  ```java
          if (savedInstanceState != null) {
              sNumber = savedInstanceState.getString(STATE_NUMBER, "");
              isFu = savedInstanceState.getBoolean(STATE_IS_FU, false);
              mEntry.setText(sNumber);
          }
  ```

  注意：`onResume` 會把 `autoClear` 設成 true，所以還原後的編號會照常顯示，也可以直接按詩歌本按鈕開啟；但如果使用者接著按數字鍵，就會從頭重新輸入。這和目前「從其他畫面返回主頁」的行為一致，屬於預期行為。

- [ ] **Step 6：整理 import**

  - 刪除 `import org.cog.hymnchtv.utils.LocaleHelper;`。
  - 新增：

    ```java
    import org.cog.hymnchtv.locale.AppLanguage;
    import org.cog.hymnchtv.locale.LocaleStore;
    import org.cog.hymnchtv.utils.ChineseS2TSelection;
    ```

  - 如果已經 import 過 `ChineseS2TSelection`，就不要重複加。
  - 如果 `Settings`（`android.provider.Settings`）不再被使用，就刪掉它的 import；用 `grep -n 'Settings\.' MainActivity.java` 確認。

- [ ] **Step 7：Build 並執行全部單元測試**

  Run: `./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug`
  Expected: BUILD SUCCESSFUL。

  用 `grep -rn LocaleHelper hymnchtv/src` 確認，應該沒有任何輸出。

- [ ] **Step 8：快速手動驗證（API 34）**

  1. 安裝後從選單切換到「繁體中文」，介面要立刻變成繁中（Activity 會重建），而且選單上打勾的是「繁體中文」。
  2. 到「設定 → 應用程式 → 詩歌 → 語言」，顯示的應該是繁體中文。
  3. 在系統設定頁改成 English，回到 app 後是英文介面，選單打勾的是 English。

- [ ] **Step 9：Commit（包含 Task 9 和 Task 10）**

  ```bash
  git add -A hymnchtv/src/main/java hymnchtv/src/main/res/menu/menu_main.xml
  git commit -m "feat: per-API locale source of truth with zh-Hant and system language page support"
  ```

---

### Task 11：ChineseS2TSelection 新增歌詞預設語言，轉換標準精簡為台灣與香港

**Files:**
- Modify: `hymnchtv/src/main/res/layout/chinese_t2s_selection.xml`
- Modify: `hymnchtv/src/main/java/org/cog/hymnchtv/utils/ChineseS2TSelection.java`
- Modify: `res/values/strings.xml`、`res/values-zh/strings.xml`、`res/values-b+zh+Hant/strings.xml`：刪除 `S2T` 和 `S2TWP` 兩個字串

- [ ] **Step 1：修改 layout**

  (a) 在原本 `android:text="@string/STD"` 的 `TextView` **之前**插入下面這段：

  ```xml
      <TextView
          android:layout_width="match_parent"
          android:layout_height="wrap_content"
          android:layout_marginBottom="5dp"
          android:textStyle="bold"
          android:textSize="20sp"
          android:text="@string/lyrics_default_title" />

      <RadioGroup
          android:id="@+id/radioGroupLyricsDefault"
          android:layout_width="match_parent"
          android:layout_height="wrap_content"
          android:layout_marginTop="10dp"
          android:layout_marginBottom="20dp">

          <RadioButton
              android:id="@+id/radioLyricsFollowUi"
              android:layout_width="match_parent"
              android:layout_height="wrap_content"
              android:text="@string/lyrics_follow_ui" />

          <RadioButton
              android:id="@+id/radioLyricsSimplified"
              android:layout_width="match_parent"
              android:layout_height="wrap_content"
              android:text="@string/lyrics_simplified" />

          <RadioButton
              android:id="@+id/radioLyricsTraditional"
              android:layout_width="match_parent"
              android:layout_height="wrap_content"
              android:text="@string/lyrics_traditional" />
      </RadioGroup>
  ```

  (b) 在 `radioGroupVar` 裡，**刪除** `radioButtonS2T` 和 `radioButtonS2TWP` 這兩個 `RadioButton`，只留下 `radioButtonS2HK` 和 `radioButtonS2TW`。任何 `android:checked="true"` 也一併刪除，改由程式碼決定要勾選哪一個。

- [ ] **Step 2：刪除不再使用的字串**

  Run:

  ```bash
  for f in hymnchtv/src/main/res/values/strings.xml hymnchtv/src/main/res/values-zh/strings.xml "hymnchtv/src/main/res/values-b+zh+Hant/strings.xml"; do
    sed -i '' -e '/<string name="S2T">/d' -e '/<string name="S2TWP">/d' "$f"
  done
  grep -rn 'name="S2T"\|name="S2TWP"\|@string/S2T\b\|@string/S2TWP\|R.string.S2T\b\|R.string.S2TWP' hymnchtv/src/main || echo none
  ```

  Expected: 印出 `none`。

- [ ] **Step 3：修改 `ChineseS2TSelection.java`**

  (a) 欄位 `private ConversionType mConversionType;` 換成：

  ```java
      private HantVariant mVariant;
      private LyricsLang mLyricsLang;
  ```

  (b) `onCreate` 中第 52-59 行，原本是：

  ```java
          mSharedPref = getSharedPreferences(PREF_SETTINGS, 0);
          String cType = mSharedPref.getString(ContentView.PREF_CONVERSION_TYPE, ConversionType.S2T.toString());
          // mConversionType = Enum.valueOf(ConversionType.class, cType);
          checkRadioButton(cType);

          // Only enable OnCheckedChangeListener only after checkRadioButton()
          RadioGroup radioGroup = findViewById(R.id.radioGroupVar);
          radioGroup.setOnCheckedChangeListener(this);
  ```

  換成：

  ```java
          mSharedPref = getSharedPreferences(PREF_SETTINGS, 0);
          Locale uiLocale = getResources().getConfiguration().getLocales().get(0);
          String rawType = mSharedPref.getString(ContentView.PREF_CONVERSION_TYPE, null);
          mVariant = LyricsLanguagePolicy.parseVariant(rawType, uiLocale);
          if (rawType != null && !LyricsLanguagePolicy.isCanonical(rawType)) {
              // Self-heal a corrupted value so other readers never see it again
              mSharedPref.edit().putString(ContentView.PREF_CONVERSION_TYPE, mVariant.getPrefValue()).apply();
          }
          checkVariantButton(mVariant);

          mLyricsLang = LyricsLang.fromPref(mSharedPref.getString(LyricsLanguagePolicy.PREF_LYRICS_DEFAULT, null));
          checkLyricsLangButton(mLyricsLang);

          // Only enable OnCheckedChangeListener after the initial check states are set
          ((RadioGroup) findViewById(R.id.radioGroupVar)).setOnCheckedChangeListener(this);
          ((RadioGroup) findViewById(R.id.radioGroupLyricsDefault)).setOnCheckedChangeListener(this);
  ```

  (c) 整個 `checkRadioButton(String cType)` 方法換成下面兩個方法：

  ```java
      private void checkVariantButton(HantVariant variant) {
          int id = (variant == HantVariant.HK) ? R.id.radioButtonS2HK : R.id.radioButtonS2TW;
          ((RadioButton) findViewById(id)).setChecked(true);
      }

      private void checkLyricsLangButton(LyricsLang lang) {
          int id;
          switch (lang) {
              case SIMPLIFIED:
                  id = R.id.radioLyricsSimplified;
                  break;
              case TRADITIONAL:
                  id = R.id.radioLyricsTraditional;
                  break;
              default:
                  id = R.id.radioLyricsFollowUi;
                  break;
          }
          ((RadioButton) findViewById(id)).setChecked(true);
      }
  ```

  (d) 整個 `onCheckedChanged` 方法換成：

  ```java
      @Override
      public void onCheckedChanged(RadioGroup group, int checkedId) {
          if (group.findViewById(checkedId) == null) {
              return;
          }
          mHasChanges = true;
          if (group.getId() == R.id.radioGroupLyricsDefault) {
              if (checkedId == R.id.radioLyricsSimplified) {
                  mLyricsLang = LyricsLang.SIMPLIFIED;
              }
              else if (checkedId == R.id.radioLyricsTraditional) {
                  mLyricsLang = LyricsLang.TRADITIONAL;
              }
              else {
                  mLyricsLang = LyricsLang.FOLLOW_UI;
              }
          }
          else {
              mVariant = (checkedId == R.id.radioButtonS2HK) ? HantVariant.HK : HantVariant.TW;
          }
      }
  ```

  (e) `updateS2TSelection` 中原本的：

  ```java
              editor.putString(ContentView.PREF_CONVERSION_TYPE, mConversionType.toString());
  ```

  換成：

  ```java
              editor.putString(ContentView.PREF_CONVERSION_TYPE, mVariant.getPrefValue());
              editor.putString(LyricsLanguagePolicy.PREF_LYRICS_DEFAULT, mLyricsLang.name());
  ```

  (f) import 調整：
  - 刪除 `import com.zqc.opencc.android.lib.ConversionType;`。
  - 新增：

    ```java
    import java.util.Locale;
    import org.cog.hymnchtv.lyrics.HantVariant;
    import org.cog.hymnchtv.lyrics.LyricsLang;
    import org.cog.hymnchtv.lyrics.LyricsLanguagePolicy;
    ```

- [ ] **Step 4：Build**

  Run: `./gradlew :hymnchtv:assembleDebug`
  Expected: BUILD SUCCESSFUL。

- [ ] **Step 5：手動驗證非法值與舊值都不會當機**

  ```bash
  adb shell am force-stop org.cog.hymnchtv
  adb shell "run-as org.cog.hymnchtv sed -i 's#<string name=\"ConversionType\">[^<]*#<string name=\"ConversionType\">BOGUS#' shared_prefs/Settings.xml"
  ```

  如果 `ConversionType` 這個 key 還不存在，先在 app 裡選一次轉換標準，再執行上面的指令。

  驗證：
  1. 從主選單開啟「歌詞語言」：畫面正常顯示，勾選的是「台灣」（簡中或台灣介面）。
  2. 用 `run-as ... cat shared_prefs/Settings.xml` 確認，`ConversionType` 已經被修正成 `S2TW`。

- [ ] **Step 6：Commit**

  ```bash
  git add hymnchtv/src/main/res hymnchtv/src/main/java/org/cog/hymnchtv/utils/ChineseS2TSelection.java
  git commit -m "feat: default lyrics language setting; limit Traditional variants to Taiwan and Hong Kong"
  ```

---

### Task 12：歌詞頁接上新規則，加上螢幕常亮與狀態保存

**Files:**
- Modify: `hymnchtv/src/main/java/org/cog/hymnchtv/ContentHandler.java`（欄位區、第 229-313 行、第 1214 行）
- Modify: `hymnchtv/src/main/java/org/cog/hymnchtv/ContentView.java`（第 105、124、188-189、207 之後、250-260、563、600-604 行）

- [ ] **Step 1：在 `ContentHandler` 加入歌詞 override 欄位與狀態 key**

  加在欄位區，緊接在 `private LinearLayout mWebView;`（第 227 行）之後：

  ```java
      /** Per-session lyrics script chosen with button_ts; null = use the default from LyricsLanguagePolicy. */
      public Boolean lyricsViewOverride = null;

      private static final String STATE_PAGE = "state_page";
      private static final String STATE_LYRICS_OVERRIDE = "state_lyrics_override"; // -1 none, 0 simplified, 1 traditional
  ```

- [ ] **Step 2：在 `ContentHandler.onCreate` 還原狀態**

  在第 230 行 `super.onCreate(savedInstanceState);` **之前**插入。這個時間點早於 Fragment 重建：

  ```java
          if (savedInstanceState != null) {
              int saved = savedInstanceState.getInt(STATE_LYRICS_OVERRIDE, -1);
              lyricsViewOverride = (saved == -1) ? null : (saved == 1);
          }
  ```

  把第 297-302 行：

  ```java
          if (hymnIdx != -1)
              mPager.setCurrentItem(hymnIdx, false);
          else
              mPager.setCurrentItem(mHymnNo, false);
  ```

  換成：

  ```java
          if (savedInstanceState != null && savedInstanceState.containsKey(STATE_PAGE))
              mPager.setCurrentItem(savedInstanceState.getInt(STATE_PAGE), false);
          else if (hymnIdx != -1)
              mPager.setCurrentItem(hymnIdx, false);
          else
              mPager.setCurrentItem(mHymnNo, false);
  ```

- [ ] **Step 3：新增 `onSaveInstanceState`，並在 `onResume`／`onPause` 處理螢幕常亮**

  `onResume` 改成：

  ```java
      @Override
      protected void onResume() {
          super.onResume();
          onUserLeaveHint = false;
          showPlayerUi(isShowPlayerUi && HymnsApp.isPortrait);
          // Keep the screen on while lyrics/score are shown (plan A.1.7); window-level so pager changes never drop it
          getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
      }

      @Override
      protected void onPause() {
          getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
          super.onPause();
      }

      @Override
      protected void onSaveInstanceState(@NonNull Bundle outState) {
          super.onSaveInstanceState(outState);
          outState.putInt(STATE_PAGE, mPager.getCurrentItem());
          outState.putInt(STATE_LYRICS_OVERRIDE, lyricsViewOverride == null ? -1 : (lyricsViewOverride ? 1 : 0));
      }
  ```

  如果還沒有 import，就加上 `import android.view.WindowManager;` 和 `import androidx.annotation.NonNull;`。

- [ ] **Step 4：把寫死的字串移到資源檔（`ContentHandler.java:1214`）**

  ```java
              return String.format(Locale.CHINA, "英文 #%d: 这首英文诗歌没有匹配的中文歌词", mHymnNoEng);
  ```

  改成：

  ```java
              return getString(R.string.hymn_no_chinese_lyrics, mHymnNoEng);
  ```

- [ ] **Step 5：修改 `ContentView`**

  (a) 刪除欄位 `private boolean isSimplify;`（第 124 行）。

  (b) 刪除第 105 行的常數 `PREF_SIMPLIFY`。全新項目，不需要保留舊的 key；預設語言改由 `LyricsLanguagePolicy.PREF_LYRICS_DEFAULT` 記錄。

  (c) **刪除**第 188-189 行：

  ```java
          isSimplify = mSharedPref.getBoolean(PREF_SIMPLIFY, true);
          mConversionType = ConversionType.valueOf(mSharedPref.getString(PREF_CONVERSION_TYPE, ConversionType.S2T.toString()));
  ```

  同時刪除第 113 行的欄位 `private ConversionType mConversionType = ConversionType.S2T;`。之後每次載入歌詞時才讀取 variant，見 (j)。

  (d) 在 `onResume()` 裡，`registerForContextMenu(lyricsView);` 之後加上：

  ```java
          // ViewPager2 only resumes the visible page: re-apply a button_ts toggle made on another page
          if (!hasEnglishLyrics) {
              toggleLyricsView();
          }
  ```

  (e) `onClick` 裡原本的：

  ```java
              if (!hasEnglishLyrics) {
                  isSimplify = !isSimplify;
                  mEditor.putBoolean(PREF_SIMPLIFY, isSimplify);
                  mEditor.apply();
              }
  ```

  換成：

  ```java
              if (!hasEnglishLyrics) {
                  // Session-only toggle; the persisted default is set in ChineseS2TSelection
                  mContentHandler.lyricsViewOverride = !isShowTraditional();
              }
  ```

  (f) `toggleLyricsView()` 裡的 `if (isSimplify) {` 改成 `if (!isShowTraditional()) {`。

  (g) `mStartForResult` 裡原本的：

  ```java
                  if (!isSimplify && hasChanges) {
                      mConversionType = ConversionType.valueOf(mSharedPref.getString(PREF_CONVERSION_TYPE, ConversionType.S2T.toString()));
                      toggleLyricsView();
                  }
  ```

  換成：

  ```java
                  if (hasChanges) {
                      // New default and/or conversion standard: drop the session toggle and rebuild all pages
                      mContentHandler.lyricsViewOverride = null;
                      mContentHandler.recreate();
                  }
  ```

  (h) 在 `toggleLyricsView()` 方法之後新增兩個輔助方法：

  ```java
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
  ```

  (i) import 調整：

  - 新增：

    ```java
    import java.util.Locale;
    import org.cog.hymnchtv.lyrics.HantVariant;
    import org.cog.hymnchtv.lyrics.LyricsAssets;
    import org.cog.hymnchtv.lyrics.LyricsLang;
    import org.cog.hymnchtv.lyrics.LyricsLanguagePolicy;
    ```

    已經 import 過的就不要重複加。
  - 刪除 `import com.zqc.opencc.android.lib.ConversionType;`。
  - 保留 `ChineseConverter` 的 import，因為 (j) 的後備路徑還會用到它。

  (j) **改讀預先產生的繁體歌詞**（spec A.1.9）。`showLyricsChText` 裡原本整個 `try { ... } catch (IOException e) { ... }` 區塊（第 450-465 行，也就是從 `try {` 到 `Timber.w("Error reading file: %s", resFName);` 的右大括號）換成：

  ```java
          String lyrics = readAsset(resFName);
          if (lyrics != null) {
              lyricsSimplify.setText(lyrics);
              lyricsTraditional.setText(loadTraditional(resFName, lyrics));
          }
  ```

  方法結尾的 `selectJC()` 判斷保持不動。

  兩份檔案都讀進來：每首只有幾 KB，不需要 OpenCC，而且按「簡／繁」切換時不必重新讀檔。

  在 `showLyricsChText` 之後新增：

  ```java
      /** Pre-generated Traditional lyrics (plan A.1.9); runtime OpenCC only if the asset is unexpectedly missing. */
      private String loadTraditional(String resFName, String simplified) {
          HantVariant variant = LyricsLanguagePolicy.parseVariant(mSharedPref.getString(PREF_CONVERSION_TYPE, null), uiLocale());
          String hantPath = LyricsAssets.hantPath(resFName, variant);
          String text = (hantPath == null) ? null : readAsset(hantPath);
          if (text != null) {
              return text;
          }
          Timber.w("Missing pre-generated lyrics %s; converting at runtime", hantPath);
          return ChineseConverter.convert(simplified, variant.getConversion(), mContentHandler);
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
  ```

  如果 `InputStream` 這個 import 不再被使用，就把它刪掉。

- [ ] **Step 6：Build 並執行全部單元測試**

  Run: `./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug`
  Expected: BUILD SUCCESSFUL。

  用 `grep -n 'isSimplify' hymnchtv/src/main/java/org/cog/hymnchtv/ContentView.java` 確認，應該沒有任何輸出。

- [ ] **Step 7：手動驗證**

  在 API 34、介面為繁中的情況下操作：
  1. 開啟大本 1，歌詞應該是繁體。
  2. 按「簡／繁」切成簡體，左右翻頁，其他頁面也要維持簡體。
  3. 返回主頁再重新開啟，歌詞應該回到繁體。
  4. 停在歌詞頁不動，超過系統的螢幕逾時時間，螢幕不能熄滅。
  5. 回到主頁後，螢幕要恢復正常的逾時行為。
  6. 用 `adb logcat | grep -i "converting at runtime"` 觀察，連續翻 20 頁，不能出現任何一行。這代表全部讀的是預先產生的檔案。
  7. 在「歌詞語言」把轉換標準改成「香港」後回到歌詞頁：頁面會重建，顯示香港字形（例如「裏」）。

- [ ] **Step 8：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/ContentHandler.java hymnchtv/src/main/java/org/cog/hymnchtv/ContentView.java
  git commit -m "feat: lyrics follow default language with session toggle, keep screen on, preserve page on recreate"
  ```

---

### Task 13：搜尋改用 SearchPattern，並處理剩下的寫死字串

**Files:**
- Modify: `hymnchtv/src/main/java/org/cog/hymnchtv/ContentSearch.java:93-97, 110-244, 305-322`
- Modify: `hymnchtv/src/main/java/org/cog/hymnchtv/service/androidupdate/UpdateServiceImpl.java:156`
- Modify: `hymnchtv/src/main/res/layout/hymn_toc_list_item.xml:26`、`hymn_toc_list_group.xml:23`

- [ ] **Step 1：在 `ContentSearch.onCreate` 建立 pattern**

  第 95-97 行：

  ```java
          String searchString = getIntent().getExtras().getString(ATTR_SEARCH);
          if (TextUtils.isEmpty((searchString)))
              return;
  ```

  換成：

  ```java
          String searchString = getIntent().getExtras().getString(ATTR_SEARCH);
          Pattern searchPattern = SearchPattern.build(searchString);
          if (searchPattern == null) {
              HymnsApp.showToastMessage(R.string.error_search_empty);
              finish();
              return;
          }
  ```

- [ ] **Step 2：替換 6 個呼叫點，並修改方法簽名**

  Run:

  ```bash
  sed -i '' 's/getMatchResult(fname, searchString)/getMatchResult(fname, searchPattern)/' hymnchtv/src/main/java/org/cog/hymnchtv/ContentSearch.java
  grep -c 'getMatchResult(fname, searchPattern)' hymnchtv/src/main/java/org/cog/hymnchtv/ContentSearch.java
  ```

  Expected: `6`。

  把 `getMatchResult` 的簽名和 javadoc 改成：

  ```java
       * @param pattern the literal search pattern built by SearchPattern (compiled once per search)
       ...
      private String getMatchResult(String fName, Pattern pattern) {
  ```

  然後刪除第 321 行 `Pattern pattern = Pattern.compile(sString.replace("他", "[祂|他]"));`。

  新增 import `import org.cog.hymnchtv.search.SearchPattern;`。如果 `TextUtils` 不再被使用，就刪掉它的 import。

- [ ] **Step 3：處理 `UpdateServiceImpl.java:156`**

  ```java
                  String historyText = "&#9210; 无更新";
  ```

  改成：

  ```java
                  String historyText = "&#9210; " + context.getString(R.string.update_none);
  ```

  同一個方法前面幾行已經有 `Context context = HymnsApp.getInstance();`，可以直接用。

- [ ] **Step 4：layout 的範例文字改成 `tools:text`**

  `HymnTocExpandableListAdapter.java` 第 68 行和第 103 行一定會呼叫 `setText`，已經確認過。

  - `hymn_toc_list_item.xml:26`：把 `android:text="联于他死与复活"` 改成 `tools:text="联于他死与复活"`。
  - `hymn_toc_list_group.xml:23`：把 `android:text="得救的证实与快乐"` 改成 `tools:text="得救的证实与快乐"`。
  - 如果根元素還沒有 `xmlns:tools="http://schemas.android.com/tools"`，就加上。

- [ ] **Step 5：Build 並執行全部單元測試**

  Run: `./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug`
  Expected: BUILD SUCCESSFUL。

- [ ] **Step 6：手動驗證搜尋**

  分別搜尋以下內容：
  - `(`、`[`、`*`：都不能當機，正常顯示「沒有結果」或結果清單。
  - 「跟随他」：要能找到含「祂」的歌詞。
  - 繁體「恩典」：要能找到結果，因為查詢字串會先轉成簡體。

- [ ] **Step 7：Commit**

  ```bash
  git add hymnchtv/src/main/java/org/cog/hymnchtv/ContentSearch.java hymnchtv/src/main/java/org/cog/hymnchtv/service/androidupdate/UpdateServiceImpl.java hymnchtv/src/main/res/layout/hymn_toc_list_item.xml hymnchtv/src/main/res/layout/hymn_toc_list_group.xml
  git commit -m "fix: treat search input literally and move remaining UI strings to resources"
  ```

---

### Task 14：補上 Task 13 新增字串的繁中翻譯

**Files:**
- Modify: `hymnchtv/src/main/res/values-b+zh+Hant/strings.xml`

Task 8 產生繁中初稿時，Task 7 新增的字串已經在 `values-zh` 裡，所以已經被一起轉換。這個 task 只做確認。

- [ ] **Step 1：確認以下 key 都存在，而且值正確**

  Run:

  ```bash
  grep -E 'name="(locale_system|lyrics_language_menu|lyrics_default_title|lyrics_follow_ui|lyrics_simplified|lyrics_traditional|hymn_no_chinese_lyrics|update_none|app_name)"' hymnchtv/src/main/res/values-b+zh+Hant/strings.xml
  ```

  Expected: 9 行，值依序為：

  | key | 值 |
  |---|---|
  | `app_name` | 詩歌 |
  | `locale_system` | 跟隨系統 |
  | `lyrics_language_menu` | 歌詞語言 |
  | `lyrics_default_title` | 歌詞預設語言 |
  | `lyrics_follow_ui` | 跟隨介面語言 |
  | `lyrics_simplified` | 簡體 |
  | `lyrics_traditional` | 繁體 |
  | `hymn_no_chinese_lyrics` | 英文 #%1$d: 這首英文詩歌沒有對應的中文歌詞 |
  | `update_none` | 無更新 |

  不符合的就手動修正。

- [ ] **Step 2：確認繁中檔裡沒有不該出現的 key**

  繁中檔不能含有 `locale_chinese`、`locale_chinese_hant`、`locale_english`，因為它們是 `translatable="false"`。

  Run: `grep -cE 'name="locale_(chinese|chinese_hant|english)"' hymnchtv/src/main/res/values-b+zh+Hant/strings.xml`
  Expected: `0`。

- [ ] **Step 3：Lint**

  Run: `./gradlew :hymnchtv:lintDebug`，然後執行：

  ```bash
  grep -cE '\[(MissingTranslation|ExtraTranslation|HardcodedText)\]' hymnchtv/build/reports/lint-results-debug.txt
  ```

  Expected: 小於或等於 `8`（基準值是 0+0+8）。Task 13 修掉寫死的字串之後，通常會更少。

- [ ] **Step 4：Commit（如果有修改）**

  ```bash
  git add hymnchtv/src/main/res/values-b+zh+Hant/strings.xml
  git commit -m "fix: finalize zh-Hant strings for new UI entries"
  ```

---

### Task 15：完整驗證（spec A.3）與審查

**Files:** 無程式碼變更。驗證結果記錄在 PR 描述中。

- [ ] **Step 1：自動化檢查**

  Run: `./gradlew :hymnchtv:testDebugUnitTest :hymnchtv:assembleDebug :hymnchtv:lintDebug`
  Expected: 全部通過，lint 的翻譯類警告不能比基準值多。

  再分別在兩台模擬器上執行 `./gradlew :hymnchtv:connectedDebugAndroidTest`，在 API 24 和 API 34 上都要全部通過。

- [ ] **Step 2：全新安裝的語言（API 24、API 34 各跑一次）**

  每一種系統語言都先執行 `adb uninstall org.cog.hymnchtv`，再 `./gradlew :hymnchtv:installDebug` 重新安裝。

  | 系統語言 | 預期的介面 |
  |---|---|
  | zh-TW | 繁中 |
  | zh-HK | 繁中 |
  | zh-CN | 簡中 |
  | en-US | 英文 |
  | ja-JP | 英文 |

  同時確認選單上打勾的是「跟隨系統」。

- [ ] **Step 3：切換語言**

  - 在兩個 API 等級上，從 app 內選單依序切換四種語言，每次切換後確認介面和選單的打勾都正確。
  - 在 API 34 上，從系統設定頁切換語言。
  - 在 API 34 上，把系統設定頁改回「系統預設」，app 選單應該打勾「跟隨系統」。
  - 切換語言後開啟英文歌詞（WebView），確認介面語言沒有被重設。

- [ ] **Step 4：重建後狀態要保留**

  開啟開發者選項的「不保留活動」，然後：
  - 在主頁輸入「12」，切換到其他 app 再回來，「12」仍然在。
  - 在歌詞頁翻到第 5 頁，按過「簡／繁」按鈕後，切換到其他 app 再回來，頁面和簡繁狀態都要保留。

  在 API 34 上，於歌詞頁停留時從系統設定頁改語言，回到 app 後仍然停在同一頁。

  驗證完後關閉「不保留活動」。

- [ ] **Step 5：歌詞、搜尋、螢幕常亮**

  依照 Task 12 Step 7、Task 13 Step 6、Task 11 Step 4 的步驟，在 API 24 上各跑一次。

- [ ] **Step 6：審查**

  - 用 `superpowers:requesting-code-review` 或 code-reviewer agent 審查 `git diff master...feat/zh-hant`。
  - 用 `/codex review` 做第二份獨立審查。
  - 有 P1 就修正，並重跑 Step 1。

- [ ] **Step 7：準備 PR**

  PR 描述要包含：
  - Step 2～5 的結果表格
  - 繁中用語校對清單（`docs/superpowers/plans/2026-10-02-zh-hant-terms.md`），請使用者確認
