# 子項目 C：介面現代化 實作計畫

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 hymnchtv 改造成 Material 3 標準的現代 App：底部導覽四頁（首頁/目錄/我的詩歌/設定）、詩本紙本版式歌詞頁、聚會速記首頁，並把長按/選單裏的功能改成看得見的操作。

**Architecture:** `MainActivity`（singleTask）改成「`MaterialToolbar` + `FragmentContainerView` + `BottomNavigationView`」主機，四個分頁各一個 Fragment（不引入 navigation component，手動 `FragmentTransaction`）。歌詞頁 `ContentHandler` 維持獨立 Activity。主題從 AppCompat 淺/深改成 `Theme.Material3.DayNight.*`，`ThemeHelper` 改成 facade。**C 不引用任何 D-1 的類別**；C 只提供空槽位/空按鈕，D-1（其 Task I1～I3）再接入。契約見 spec §9（C-1～C-11）。

**Tech Stack:** Java 11（既有畫面）、Kotlin（新畫面）、`com.google.android.material:material:1.12.0` + `androidx.preference:preference-ktx:1.2.1`（新增）、`androidx.appcompat`/`fragment`/`viewpager2`（既有）、minSdk 24 / compileSdk 37。

---

## 修訂紀錄

- rev 1（2026-10-02）：初版。Codex 審查出 16 P1 + 4 P2。
- rev 2（2026-10-02）：依 Codex 全部 P1 修正。關鍵改動：①並行化重排——四個分頁 Fragment 先各自獨立建立（Phase 1，不碰 `MainActivity.java`），`MainActivity` 主機遷移集中到單一 lane（Phase 2）；②`MainActivity.setAppTheme` 改委託 `ThemePrefs`（修 `Theme.valueOf("SYSTEM")` 崩潰）；③`ContentHandler` manifest theme 改 `@style/AppTheme`；④補 `preference-ktx`；⑤設定頁改用 `OnPreferenceChangeListener`；⑥C 對 D-1 只留空槽位、不引用其類別；⑦筆記本列槽位移到 `content_main.xml`（單一宿主）；⑧播放器列保留 `SeekBar`（不換 `Slider`）；⑨smoke test 提前到 Phase 0；⑩edge-to-edge 只套 `MainActivity`；⑪拍號/調號改第 4 行解析。
- rev 3（2026-10-02）：依 Codex 第二、三輪 P1（+4 P2）修正。關鍵改動：①新增 Task 4「綁入 LXGW WenKai 字型」（硬關卡，因 `res/font/` 與 `tools/gen_font_subset.py` 皆不存在；檔名 `lxgw_wenkai_regular` 避免與 family XML 撞資源名）；②`ThemePrefs.apply` 同步 `ThemeHelper` 快取，`ThemePrefs.resync` 於 `onConfigurationChanged` 處理 SYSTEM 模式系統深淺色切換；③edge-to-edge 豁免 `MainActivity` 免於 `HymnsApp.EdgeToEdgeDisable` 重複 insets；④`MainHost` 狀態還原改從 `savedInstanceState` 讀取、`mainHost` 為欄位、`MainActivity` 明確列出要移除的 home 欄位/方法與回呼；⑤smoke test 補翻頁 + 播放；⑥TOC 等價測試改用 checked-in fixtures；⑦H1 補「記住上次詩歌本」；⑧spec §9 改為「C 提供空槽位/按鈕，D-1 接線」。Codex 誤判「nav_toc 重複」不成立（已確認）。

## 給執行者（Sonnet 5.5）的說明

這份計畫由 **Sonnet 5.5（`claude-sonnet-5-5`）** 子代理執行，可在各自 worktree 平行。主對話（協調者）只開 worktree、派工、合併、跑模擬器與最後審查。

**開工前（協調者）：**
1. 確認 spec §7 的待確認項目（品牌色、DayNight、搜尋入口、字級、exit、長按替代搜尋）已由使用者定案；寫進本檔 rev 3。
2. 本檔 rev 2 再送 Codex 審查，處理完所有 P1 才開始 Task 0。
3. 前置：A2、B、D-1a 已合併（D-1 UI 的 G0）。本計畫的 `HymnTitleSource`（C-8）若 D-1 先合併，改用其 `notebook/ui/titles/` 版本（見 Task H2）。

**執行規則：**
- 每條 lane 只改自己「檔案範圍」的檔案；要改範圍外的檔案就停下來回報。
- **`MainActivity.java` 只有 Phase 2（Lane 0）能改。** Phase 1 的四個 Fragment lane 一律不碰 `MainActivity.java`。
- 資料層（`persistance/`）、媒體（`mediaplayer/`、`mediaconfig/`、`MediaContentHandler`、`MediaDownloadHandler`）**一律不改簽名**。
- **C 不 import 任何 `org.cog.hymnchtv.notebook.*` 類別。** 對 D-1 只留「空容器 id／空按鈕 id／介面（由 D-1 定義）」，實際接線在 D-1 的 Task I1～I3。
- `ContentHandler` 的三個接點 `showHymn`/`onPlaybackCompleted`/`NotebookBarHost`（C-10）不得破壞。
- 模擬器沿用 `api34nb`（`emulator-5580`）/`api24nb`（`emulator-5582`）；不要碰 `api24`/`api34`。所有 adb/gradle 指令前 `export ANDROID_SERIAL=…`。

---

## 平行化地圖與合併關卡

| 階段 | Lane | Tasks（依序） | 檔案範圍 | 前置 |
|---|---|---|---|---|
| 0 | 0 | Task 0 → 1 → 2 → 3 → 4 | `hymnchtv/build.gradle`、`res/values/theme.xml`、`res/values/c_dimens.xml`、`res/values*/strings_c.xml`、`res/font/`（字型，Task 4）、`ThemeHelper.java`、`BaseActivity.java`、`HymnsApp.java`、`MainActivity.java`（僅 `setAppTheme`/`onCreate` 的 theme 兩處）、`AndroidManifest.xml`（`ContentHandler` theme）、`ui/theme/`（新）、`androidTest/.../ui/SmokeFlowTest.kt`、對應測試 | — |
| 1 | H | H1 → H2 | `ui/home/`（新）、`ui/titles/`（新，C-8）、`res/layout/fragment_home.xml`（新）、對應測試 | **G0** |
| 1 | T | T1 | `ui/toc/`（新）、`res/layout/fragment_toc.xml`（新）、`HymnToc.java`（抽共用，不動簽名） | G0 |
| 1 | S | S1 | `ui/settings/`（新）、`res/xml/c_preferences.xml`（新）、對應測試 | G0 |
| 1 | M | M1 | `ui/myhymns/`（新）、`res/layout/fragment_myhymns.xml`（新） | G0 |
| 2 | 0 | HOST1 → HOST2 | `MainActivity.java`、`res/layout/activity_main_host.xml`（新）、`res/menu/menu_bottom_nav.xml`（新）、`ui/host/MainHost.kt`（新）、`res/menu/menu_main.xml`（刪除） | **G1**：Phase 1 四 lane 合併 |
| 3 | L | L1 → L2 → L3 → L4 | `ContentHandler.java`、`ContentView.java`、`res/layout/content_lyrics.xml`、`res/layout/content_main.xml`、`res/layout/media_player_audio_ui.xml`、`res/menu/menu_content.xml`、`ui/lyrics/`（新）、對應測試 | **G2**：HOST 合併 |
| 4 | 0 | F1 → F2 | `MainActivity.java`、`About.java`、`MediaConfig.java`、`res/menu/menu_content.xml`（刪除）、`AndroidManifest.xml`（移除 `HymnToc` 若已遷移）、`androidTest/` | **G3**：L 合併 |

**合併關卡：**
- **G0**：Task 0～4 commit，`testDebugUnitTest` 綠，`assembleDebug` 成功，**`api34nb` 上 `SmokeFlowTest` 綠（開工前基線）**。
- **G1**：Phase 1 四 lane 合併（各自獨立，無 `MainActivity.java` 衝突），`assembleDebug` 成功。
- **G2**：HOST 合併，App 可啟動、四分頁切換、share intent 仍工作（手動）。
- **G3**：L 合併，`api34nb` 上 `SmokeFlowTest` 仍綠（歌詞頁重設計後）。
- **G4**：F 合併，spec §4.3 長按逐項核對。

---

## 檔案結構

（`java/...`、`ui/...` = `hymnchtv/src/main/java/org/cog/hymnchtv/...`；`test/...` = `hymnchtv/src/test/java/org/cog/hymnchtv`；`androidTest/...` = `hymnchtv/src/androidTest/java/org/cog/hymnchtv`；`res/` 在 `hymnchtv/src/main/`。）

**新增（Kotlin）：**

| 檔案 | 職責 | Task |
|---|---|---|
| `ui/theme/ThemePrefs.kt` | 主題偏好（跟隨系統/淺色/深色）單一來源 | 0 |
| `ui/theme/EdgeToEdge.kt` | 僅對 `MainActivity` 套 edge-to-edge | 2 |
| `ui/home/HomeFragment.kt`、`ui/home/HomeViewModel.kt` | 首頁分頁（鍵盤/詩歌本/即時詩名/歷史/＋歌單） | H1 |
| `ui/titles/HymnTitleSource.kt`、`ui/titles/AssetHymnTitles.kt` | 依「類別+編號」取詩名（沿用 `HymnToc.getHymnTitle` 演算法） | H2 |
| `ui/toc/TocFragment.kt`、`ui/toc/TocBuilder.kt` | 目錄分頁；`TocBuilder` 抽 `HymnToc.getHymnToc` 邏輯 | T1 |
| `ui/settings/SettingsFragment.kt` | 設定分頁（`PreferenceFragmentCompat` + change listener） | S1 |
| `ui/myhymns/MyHymnsFragment.kt` | 我的詩歌分頁空槽位（C-1/C-2） | M1 |
| `ui/host/MainHost.kt` | `MainActivity` 分頁主機（含 tab 狀態保存） | HOST1 |
| `ui/lyrics/LyricsMeta.kt` | 拍號/調號解析 + 節數 span | L1/L4 |

**修改（Java）：** `MainActivity.java`（僅 Phase 0 theme 兩處 + Phase 2 主機遷移）、`ContentHandler.java`、`ContentView.java`、`HymnToc.java`（抽共用）、`About.java`（入口）、`ThemeHelper.java`、`BaseActivity.java`、`HymnsApp.java`、`AndroidManifest.xml`。

**刪除/替換：** `res/menu/menu_main.xml`（Phase 2 刪）、`res/layout/main.xml`（Phase 2 被 `fragment_home.xml` 取代）、`res/menu/menu_content.xml`（Phase 3 被歌詞頁頂列取代）。

---

## Task 0：加入 Material 3 與 Preference 依賴

**Files:** Modify `hymnchtv/build.gradle`

- [ ] **Step 1: 加兩個依賴**

在 `hymnchtv/build.gradle` 的 `dependencies { … }` 加：

```groovy
implementation 'com.google.android.material:material:1.12.0'
implementation 'androidx.preference:preference-ktx:1.2.1'
```

（`Theme.Material3.DayNight.NoActionBar`、`PreferenceFragmentCompat`、`SeekBarPreference` 都需要。）

- [ ] **Step 2: 改寫主題為 Material 3 DayNight**

`res/values/theme.xml` 整檔替換為：

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources xmlns:android="http://schemas.android.com/apk/res/android">

    <style name="AppTheme" parent="Theme.Material3.DayNight.NoActionBar">
        <item name="colorPrimary">@color/color_primary</item>
        <item name="colorOnPrimary">@color/white</item>
        <item name="colorPrimaryContainer">@color/color_primary_dark</item>
        <item name="colorSecondary">@color/color_accent</item>
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
        <item name="listItemRow">@style/RowList.Light</item>
        <item name="listItemRadio">@style/RadioListItem.Light</item>
    </style>

    <style name="AppTheme.Light" parent="Theme.Material3.Light.NoActionBar">
        <item name="colorPrimary">@color/color_primary</item>
        <item name="colorOnPrimary">@color/white</item>
        <item name="colorSecondary">@color/color_accent</item>
        <item name="listItemRow">@style/RowList.Light</item>
        <item name="listItemRadio">@style/RadioListItem.Light</item>
    </style>

    <style name="AppTheme.Dark" parent="Theme.Material3.Dark.NoActionBar">
        <item name="colorPrimary">@color/color_primary</item>
        <item name="colorOnPrimary">@color/white</item>
        <item name="colorSecondary">@color/color_accent</item>
        <item name="listItemRow">@style/RowList.Dark</item>
        <item name="listItemRadio">@style/RadioListItem.Dark</item>
    </style>
</resources>
```

> 品牌色是否改設計稿色值，見 spec §7 第 1 項；此處沿用 `#09354d`。

- [ ] **Step 3: `ContentHandler` 的 manifest theme 改 `@style/AppTheme`**

`AndroidManifest.xml` 的 `ContentHandler` activity，把：

```xml
android:theme="@style/Theme.AppCompat.Light.NoActionBar"
```

改成：

```xml
android:theme="@style/AppTheme"
```

（否則歌詞頁沿用 AppCompat Light，L1 換 Material 元件會觸發 Material theme 強制檢查而 crash，且無 DayNight。）

- [ ] **Step 4: 建立 `ThemePrefs`（三選一 + DayNight）**

`ui/theme/ThemePrefs.kt`：

```kotlin
package org.cog.hymnchtv.ui.theme

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/** 深色模式偏好。與 spec §7 第 2 項一致。 */
enum class NightMode(val delegate: Int) {
    SYSTEM(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
    LIGHT(AppCompatDelegate.MODE_NIGHT_NO),
    DARK(AppCompatDelegate.MODE_NIGHT_YES);

    companion object {
        fun from(name: String?): NightMode = entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}

object ThemePrefs {
    const val PREF_THEME = "Theme" // 沿用既有 key；舊值 DARK/LIGHT 仍會正確對應

    fun current(context: Context): NightMode =
        NightMode.from(context.getSharedPreferences("Settings", 0).getString(PREF_THEME, null))

    fun apply(context: Context, mode: NightMode) {
        context.getSharedPreferences("Settings", 0).edit().putString(PREF_THEME, mode.name).apply()
        AppCompatDelegate.setDefaultNightMode(mode.delegate)
        // 同步 ThemeHelper 快取（About/WebViewLyrics/LyricsEnglishRecord 用 isAppTheme(DARK) 選 CSS）
        org.cog.hymnchtv.utils.ThemeHelper.syncDark(resolveDark(context, mode))
    }

    fun applyStored(context: Context) = apply(context, current(context))

    /** 系統深淺色在 app 存活期間變更（SYSTEM 模式）時，重新同步 ThemeHelper 快取（rev 3 #2）。 */
    fun resync(context: Context) {
        org.cog.hymnchtv.utils.ThemeHelper.syncDark(resolveDark(context, current(context)))
    }

    /** SYSTEM 依目前 uiMode 解析成實際亮暗。 */
    private fun resolveDark(context: Context, mode: NightMode): Boolean = when (mode) {
        NightMode.DARK -> true
        NightMode.LIGHT -> false
        NightMode.SYSTEM ->
            (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
    }
}
```

- [ ] **Step 5: 寫失敗測試**

`test/.../ui/theme/ThemePrefsTest.kt`：

```kotlin
package org.cog.hymnchtv.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemePrefsTest {
    @Test fun unknownNameFallsBackToSystem() {
        assertEquals(NightMode.SYSTEM, NightMode.from(null))
        assertEquals(NightMode.SYSTEM, NightMode.from("BOGUS"))
    }

    @Test fun legacyValuesStillMap() {
        assertEquals(NightMode.LIGHT, NightMode.from("LIGHT"))
        assertEquals(NightMode.DARK, NightMode.from("DARK"))
        assertEquals(NightMode.SYSTEM, NightMode.from("SYSTEM"))
    }
}
```

- [ ] **Step 6: 跑測試**

Run: `./gradlew :hymnchtv:testDebugUnitTest --tests "org.cog.hymnchtv.ui.theme.ThemePrefsTest"`
Expected: PASS。

- [ ] **Step 7: Commit**

```bash
git add hymnchtv/build.gradle hymnchtv/src/main/res/values/theme.xml \
        hymnchtv/src/main/AndroidManifest.xml \
        hymnchtv/src/main/java/org/cog/hymnchtv/ui/theme/ \
        hymnchtv/src/test/java/org/cog/hymnchtv/ui/theme/
git commit -m "feat(c): Material 3 DayNight theme + preference dep"
```

---

## Task 1：`ThemeHelper` facade + `MainActivity.setAppTheme` 委託 + DayNight

**Files:** Modify `utils/ThemeHelper.java`、`BaseActivity.java`、`HymnsApp.java`、`MainActivity.java`（僅 theme 兩處）

- [ ] **Step 1: `ThemeHelper` 保留簽名，改成不呼叫 `ctx.setTheme`**

`utils/ThemeHelper.java`：保留 `enum Theme { LIGHT, DARK }` 與 `getAppTheme`/`setAppTheme`/`isAppTheme`（`About.getAboutInfo()` 仍依 `isAppTheme(DARK)` 選 CSS）。`setTheme(Context)` 改 no-op：

```java
public static void setTheme(Context ctx) {
    // DayNight is applied globally via AppCompatDelegate in HymnsApp; keep signature for compat.
}
```

`getAppThemeResourceId()` 維持回傳 LIGHT/DARK 對應 resource（僅 `About`/其它可能呼叫處相容；實際上不再由 Activity 套用）。

再加一個給 `ThemePrefs` 回寫快取的靜態方法（修 rev 2 的深色 CSS 失配）：

```java
/** Called by ThemePrefs after the DayNight mode is resolved. */
public static void syncDark(boolean dark) {
    mTheme = dark ? Theme.DARK : Theme.LIGHT;
}
```

- [ ] **Step 2: `BaseActivity` 移除 `setTheme` 呼叫**

`BaseActivity.onCreate` 從：

```java
ThemeHelper.setTheme(this);
super.onCreate(savedInstanceState);
```

改成：

```java
super.onCreate(savedInstanceState);
```

保留 `attachBaseContext` 的 `LocaleStore.wrap`。

- [ ] **Step 3: `HymnsApp.onCreate` 套用 DayNight + 動態取色**

在 `HymnsApp.onCreate()` 的 `DatabaseBackend.getInstance(this);` 之後加：

```java
org.cog.hymnchtv.ui.theme.ThemePrefs.applyStored(this);
com.google.android.material.color.DynamicColors.applyToActivitiesIfAvailable(this);
```

> 動態取色是 spec §3.1 的明確要求（非可選）。Android 12+ 動態色會覆蓋 `colorPrimary` 等；品牌 fallback 只在無動態色裝置生效。若需強制品牌色，改為不呼叫 `DynamicColors`（spec §7 待確認第 1 項）。

`HymnsApp` 既有的 `onConfigurationChanged`（現設定 `isPortrait`）追加同步（rev 3 #2）：

```java
@Override
public void onConfigurationChanged(@NonNull Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    isPortrait = (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT);
    // SYSTEM 模式：系統深淺色切換時，重新同步 ThemeHelper 快取（供 About/WebViewLyrics/LyricsEnglishRecord 選 CSS）
    org.cog.hymnchtv.ui.theme.ThemePrefs.resync(this);
}
```

- [ ] **Step 4: `MainActivity` 修 `Theme.valueOf("SYSTEM")` 崩潰**

`MainActivity.onCreate` 刪除這兩行（`HymnsApp` 已全域套用 DayNight）：

```java
String theme = mSharedPref.getString(PREF_THEME, Theme.DARK.toString());
setAppTheme(theme, false);
```

`setAppTheme(String, boolean)` 改為委託 `ThemePrefs`（避免舊選單 `themeDark/themeLight` 觸發 `Theme.valueOf("SYSTEM")` 崩潰，Phase 2 才移除選單）：

```java
private void setAppTheme(String sTheme, boolean prefChange) {
    if (prefChange) {
        org.cog.hymnchtv.ui.theme.ThemePrefs.apply(this,
                org.cog.hymnchtv.ui.theme.NightMode.from(sTheme));
        recreate();
    }
}
```

- [ ] **Step 5: 編譯 + 手動驗證主題切換**

Run: `./gradlew :hymnchtv:assembleDebug`；`api34nb` 啟動，切換淺色/深色不崩潰、`About` 正常。

- [ ] **Step 6: Commit**

```bash
git add hymnchtv/src/main/java/org/cog/hymnchtv/utils/ThemeHelper.java \
        hymnchtv/src/main/java/org/cog/hymnchtv/BaseActivity.java \
        hymnchtv/src/main/java/org/cog/hymnchtv/HymnsApp.java \
        hymnchtv/src/main/java/org/cog/hymnchtv/MainActivity.java
git commit -m "feat(c): ThemeHelper facade + DayNight + fix setAppTheme SYSTEM crash"
```

---

## Task 2：edge-to-edge 只套 `MainActivity`

**Files:** Create `ui/theme/EdgeToEdge.kt`；Modify `MainActivity.java`（僅加一行）

- [ ] **Step 1: helper（只在明確呼叫的 Activity 套用）**

`ui/theme/EdgeToEdge.kt`：

```kotlin
package org.cog.hymnchtv.ui.theme

import android.app.Activity
import androidx.core.view.WindowCompat

object EdgeToEdge {
    fun enable(activity: Activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
    }
}
```

- [ ] **Step 2: 只對 `MainActivity` 套用**

`MainActivity.onCreate` 在 `super.onCreate(savedInstanceState)` 之後加：

```java
org.cog.hymnchtv.ui.theme.EdgeToEdge.enable(this);
```

**不**改 `BaseActivity`、**不**移除 `HymnsApp.EdgeToEdgeDisable()`（其它 Activity 維持現況，避免 `ContentHandler`/`HymnToc` 系統列重疊）。但要在 `HymnsApp.EdgeToEdgeDisable()` 的 `onActivityPostCreated` 加一個豁免，避免 API 35+ 對 `MainActivity` 重複套 padding（否則與 host 的 `fitsSystemWindows` 雙重 insets）：

```java
// 在 setOnApplyWindowInsetsListener 最前面：
if (activity instanceof org.cog.hymnchtv.MainActivity) return; // edge-to-edge 由 MainActivity 自理
```

insets 由 `activity_main_host.xml` 用 `android:fitsSystemWindows="true"` 處理（HOST1）。

- [ ] **Step 3: 編譯驗證**

Run: `./gradlew :hymnchtv:assembleDebug`
Expected: BUILD SUCCESSFUL。

- [ ] **Step 4: Commit**

```bash
git add hymnchtv/src/main/java/org/cog/hymnchtv/ui/theme/EdgeToEdge.kt \
        hymnchtv/src/main/java/org/cog/hymnchtv/MainActivity.java
git commit -m "feat(c): edge-to-edge scoped to MainActivity"
```

---

## Task 3：開工前 smoke test（基線，先於重構）

**Files:** Create `androidTest/.../ui/SmokeFlowTest.kt`

- [ ] **Step 1: 寫 smoke test（輸入編號 → 開歌詞 → 翻頁 → 播放）**

`androidTest/.../ui/SmokeFlowTest.kt`，對「現有 UI」跑基線：

```kotlin
package org.cog.hymnchtv.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.cog.hymnchtv.R
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SmokeFlowTest {
    @Test fun inputOpensLyricsFlipPageThenPlay() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            onView(withId(R.id.n1)).perform(click())      // 輸入 1
            onView(withId(R.id.bs_db)).perform(click())   // 大本詩歌 → 歌詞頁
            onView(withId(R.id.viewPager)).check(matches(isDisplayed()))
            onView(withId(R.id.viewPager)).perform(swipeLeft())  // 翻下一頁
            onView(withId(R.id.viewPager)).check(matches(isDisplayed()))
            onView(withId(R.id.playback_play)).perform(click())  // 播放（斷言不崩潰）
            scenario.onActivity { a -> assertFalse(a.isFinishing) }
        }
    }
}
```

> 播放器列預設顯示（`PREF_MENU_SHOW` 預設 true），故 `playback_play` 可見。翻頁（`swipeLeft`）與播放（`click`）都真實執行，不允許 fallback 跳過；若未來播放器列預設改成隱藏，測試 setup 先寫 `PREF_MENU_SHOW=true`。

- [ ] **Step 2: 在 `api34nb` 跑基線**

Run: `export ANDROID_SERIAL=emulator-5580 && ./gradlew :hymnchtv:connectedDebugAndroidTest --tests "org.cog.hymnchtv.ui.SmokeFlowTest"`
Expected: PASS（這是後續所有重構的回歸保護）。

- [ ] **Step 3: Commit**

```bash
git add hymnchtv/src/androidTest/java/org/cog/hymnchtv/ui/SmokeFlowTest.kt
git commit -m "test(c): smoke flow baseline before UI refactor"
```

> **G0 關卡**：Task 0～4 commit，`testDebugUnitTest` 綠、`assembleDebug` 成功、`SmokeFlowTest` 綠。

---

## Task 4：綁入 LXGW WenKai 字型（硬關卡）

**Files:** Create `res/font/lxgw_wenkai.ttf`（或子集）、`res/font/lxgw_wenkai.xml`；Modify `res/values*/strings_c.xml`

> 前置事實（rev 3 核對）：目前 `res/font/` 不存在、`tools/gen_font_subset.py` 與 `tools/font_subset_manifest.txt` 也都不存在（那些是 D-1 UI 計畫的產物，尚未建立）。所以 C **自己綁入**文楷字型，不假設外部管線。

- [ ] **Step 1: 下載 LXGW WenKai（OFL 1.1）**

從 <https://github.com/lxgw/LxgwWenKai/releases> 下載 `LXGWWenKai-Regular.ttf`（OFL 1.1，可商用，授權義務見 About 頁既有說明），放到 `hymnchtv/src/main/res/font/lxgw_wenkai_regular.ttf`。**檔名必須是 `lxgw_wenkai_regular`**（rev 3 #1：若檔名也叫 `lxgw_wenkai`，會與 family XML 產生同名 `@font/lxgw_wenkai` 資源，aapt2 報 duplicate resource）。檔案較大（約 20MB）時，用 `pyftsubset` 或後續 D-1 的子集管線縮小；**本 Task 先以完整字型通過，子集是後續最佳化**。

- [ ] **Step 2: 字型 family XML**

`res/font/lxgw_wenkai.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<font-family xmlns:android="http://schemas.android.com/apk/res/android">
    <font android:fontStyle="normal" android:fontWeight="400" android:font="@font/lxgw_wenkai_regular" />
</font-family>
```

- [ ] **Step 3: 硬關卡驗證**

Run: `ls -la hymnchtv/src/main/res/font/lxgw_wenkai_regular.ttf && ./gradlew :hymnchtv:assembleDebug`
Expected: 字型檔存在、build 成功。**檔不存在即停**（L1 依賴此字型，不允許 serif 過渡）。

- [ ] **Step 4: Commit**

```bash
git add hymnchtv/src/main/res/font/
git commit -m "feat(c): bundle LXGW WenKai font (OFL 1.1)"
```

---

## Task H1：首頁分頁（`HomeFragment` 完整實作，獨立於 `MainActivity`）

**Files:** Create `ui/home/HomeFragment.kt`、`ui/home/HomeViewModel.kt`、`res/layout/fragment_home.xml`

> 本 lane 只新增檔案，**不碰 `MainActivity.java`**。`HomeFragment` 自足：呼叫 `MainActivity.showContent(...)`（static）開歌詞；`MainActivity` 舊鍵盤仍存在，Phase 2 才移除。

- [ ] **Step 1: 完整首頁 layout（id 與舊 `main.xml` 一致）**

`res/layout/fragment_home.xml`：結構等同舊 `main.xml`（`tv_entry`、`historyListView`、`tv_search`、`btn_search`、數字鍵 `n0`…`n11`、6 詩歌本 `bs_db/bs_bb/bs_er/bs_xb/bs_xg/bs_yb`、`btn_english`），差異：
- 根用 `NestedScrollView` + `LinearLayout`，頂部留 `android:fitsSystemWindows="true"` 或 padding 供 insets。
- 按鈕改 `com.google.android.material.button.MaterialButton`（`minHeight="48dp"`）。
- 6 個詩歌本**放大**（方向 C）：`minHeight="64dp"`、`textSize="26sp"`、2×3 格線。
- 加 `@id/title_preview`（`TextView`，`tv_entry` 下）顯示即時詩名（方向 C）。
- 加 `@id/btn_add_playlist`（`MaterialButton`，文字「＋歌單」）與 `@id/btn_next`（Phase 2 才接線，此處只宣告 id，click no-op）。

- [ ] **Step 2: `HomeViewModel` 持有狀態**

`ui/home/HomeViewModel.kt`：

```kotlin
package org.cog.hymnchtv.ui.home

import androidx.lifecycle.ViewModel
import org.cog.hymnchtv.MainActivity.HYMN_DB

class HomeViewModel : ViewModel() {
    var hymnType: String = HYMN_DB
    var number: String = ""
    var isFu: Boolean = false
    var isToc: Boolean = false
    var autoClear: Boolean = false
}
```

- [ ] **Step 3: `HomeFragment` 實作鍵盤/詩歌本/歷史/即時詩名**

`HomeFragment` 移植 `MainActivity` 的 `onNumberClick`/`onHymnButtonClicked`/`showHymnFromEng`/`initHistoryList`/`setFontSize`/`setFontColor`/`setWallpaper` 邏輯，改成操作自身 view。`showContent` 用 `MainActivity.showContent(requireContext(), ...)`（static，不變）。歷史列保留滑動刪除 + 長按刪除 + **加一個可見刪除按鈕**（row 尾 `ImageButton`）。**選詩歌本時把 `hymnType` 存入 `PREF_LAST_HYMN_TYPE`（SharedPreferences），`onCreate` 讀回並高亮對應按鈕（記住上次詩歌本，方向 C）。**

- [ ] **Step 4: 即時詩名（方向 C）**

`tv_entry` 加 `TextWatcher`：輸入改變呼叫 `HymnTitleSource.lookup(hymnType, number)` 填 `title_preview`；空/無效清空。`HymnTitleSource` 於 H2 定義，此步先用介面。

- [ ] **Step 5: 手動驗證（獨立預覽）+ Commit**

以一個臨時 host 或 `@Preview` 驗證；正式接線在 HOST。commit：

```bash
git add hymnchtv/src/main/java/org/cog/hymnchtv/ui/home/ \
        hymnchtv/src/main/res/layout/fragment_home.xml
git commit -m "feat(c): home fragment (keyboard, hymn books, live title, history)"
```

---

## Task H2：`HymnTitleSource`（C-8，沿用既有標題演算法）

**Files:** Create `ui/titles/HymnTitleSource.kt`、`ui/titles/AssetHymnTitles.kt`；Modify `ui/home/HomeFragment.kt`

- [ ] **Step 1: 介面與實作（沿用 `HymnToc.getHymnTitle` 演算法）**

`ui/titles/HymnTitleSource.kt`：

```kotlin
package org.cog.hymnchtv.ui.titles

interface HymnTitleSource {
    fun lookup(hymnType: String, hymnNo: Int): String?
}
```

`ui/titles/AssetHymnTitles.kt`：把 `HymnToc.getHymnTitle(int, String)` 的演算法抽成靜態共用（**保留三處細節**，Codex rev 2 #11）：
1. 讀 `lyrics_*_text/<type><no>.txt` 第 2 行（標題），去掉類別前綴（最後一個「－」之後）。
2. 讀第 3 行，若含「（」則把「（…）」補進標題（如「（英1）」）。
3. **YB 對照**：`HYMN_YB` 用 `MainActivity.ybXTable` 對應到實際檔案（同 `MyPagerAdapter.getHymnFragment`）。
**C 與 D-1 各自擁有各自的 `HymnTitleSource`**：C 的 `ui/titles/HymnTitleSource`（同步 `lookup`）與 D-1 的 `notebook/ui/domain/HymnTitleSource`（`suspend titlesFor`）是**不同 package、不同簽名**，**不共用、不衝突、不互相刪除**。若 D-1 已先合併，仍建 C 自己的（本 task 不變）。

> 已知限制（P2）：標題目前從簡體檔 `lyrics_*_text/*.txt` 讀出，即時詩名顯示簡體；繁中標題屬後續可選（可套 `HantVariant`，同 `ContentView.loadTraditional`），本版不做。

- [ ] **Step 2: 單元測試**

`test/.../ui/titles/AssetHymnTitlesTest.kt`：`db`+`1` 回傳含「祂的計畫」的標題；不存在編號回傳 null；`yb` 對照詩歌回傳對應標題。

- [ ] **Step 3: `HomeFragment` 用介面注入**

`HomeFragment` 的即時查詢改透過 `HymnTitleSource`（供測試替換）。

- [ ] **Step 4: Commit**

```bash
git add hymnchtv/src/main/java/org/cog/hymnchtv/ui/titles/ \
        hymnchtv/src/test/java/org/cog/hymnchtv/ui/titles/ \
        hymnchtv/src/main/java/org/cog/hymnchtv/ui/home/HomeFragment.kt
git commit -m "feat(c): HymnTitleSource reusing HymnToc title algorithm (C-8)"
```

---

## Task T1：目錄分頁（`TocFragment` + `TocBuilder`）

**Files:** Create `ui/toc/TocFragment.kt`、`ui/toc/TocBuilder.kt`、`res/layout/fragment_toc.xml`；Modify `HymnToc.java`（改呼叫 `TocBuilder`，不動簽名）

- [ ] **Step 1: 抽取 `TocBuilder`（定義邊界 + 等價測試）**

`ui/toc/TocBuilder.kt`：把 `HymnToc.getHymnToc`/`getHymnTocType`/`getHymnTitle` 的**純產生邏輯**抽出，簽名：

```kotlin
object TocBuilder {
    /** @return 有序 map：類別 → 該類別下依序的「`%04d: 標題`」字串。input 完全與 HymnToc 相同。 */
    fun build(context: Context, hymnType: String, tocPage: String): LinkedHashMap<String, List<String>>
}
```

保留：六個詩歌本的編號跳號（`rangeBbLimit`/`rangeErLimit`）、YB 的 `mTocYB` 特例、`TOC_ENGLISH` 不排序、`Collections.sort`（stroke/pinyin）、第 3 行「（…）」補資訊。

- [ ] **Step 2: 等價測試（用 checked-in fixtures，rev 3）**

`HymnToc` 的 `getHymnToc`/`getHymnTocType` 是 **private instance method**，無法在測試直接呼叫比對。改用 **checked-in expected fixtures**：抽 `TocBuilder` 前，先對幾個 `(hymnType, tocPage)` 組合用現有 `HymnToc` 產出並存入 `test/resources/toc_fixtures/{db,bb,yb,er}-{CATEGORY,PINYIN}.txt`（每行 `類別<TAB>標題`）。抽完後 `TocBuilderTest` 讀 fixtures 斷言 `TocBuilder.build(...)` 與 fixture 逐行相等。覆蓋至少：`db`+`CATEGORY`、`db`+`PINYIN`、`bb`+`CATEGORY`、`yb`+`CATEGORY`、`er`+`CATEGORY`。測資 asset 目錄用 unit test 已設的 `hymnchtv.assetsDir`。

- [ ] **Step 3: `HymnToc` 改呼叫 `TocBuilder`（過渡期兩者並存）**

`HymnToc.initHymnTocAdapter` 的 `tocListDetail = getHymnToc(...)` 改 `tocListDetail = TocBuilder.build(this, hymnType, tocPage)`（行為不變，`HymnToc` Activity 仍可用）。

- [ ] **Step 4: `TocFragment`**

`ui/toc/TocFragment.kt` + `res/layout/fragment_toc.xml`：頂部 `MaterialButtonToggleGroup`（詩歌本）×2 行或 chips、`TabLayout`（目錄/類別/筆畫/拼音/英中）、`ExpandableListView`。子項點擊 → `MainActivity.showContent(requireContext(), hymnType, hymnNo, false)`。

- [ ] **Step 5: 手動驗證 + Commit**

手動：切詩歌本、切 TOC 類型、點子項開歌詞。

```bash
git add hymnchtv/src/main/java/org/cog/hymnchtv/ui/toc/ \
        hymnchtv/src/main/res/layout/fragment_toc.xml \
        hymnchtv/src/test/java/org/cog/hymnchtv/ui/toc/ \
        hymnchtv/src/main/java/org/cog/hymnchtv/HymnToc.java
git commit -m "feat(c): toc tab + TocBuilder extraction with parity tests"
```

---

## Task S1：設定分頁（`SettingsFragment` + `PreferenceFragmentCompat` + change listener）

**Files:** Create `ui/settings/SettingsFragment.kt`、`res/xml/c_preferences.xml`、`res/layout/fragment_settings.xml`、`res/values/array.xml`（補 `c_*` 陣列）、`res/values*/strings_c.xml`

> 本 lane 只新增檔案，**不碰 `MainActivity.java`**。Phase 2 才移除 `menu_main.xml`。

- [ ] **Step 1: 設定 XML（承接 `menu_main` 全部項）**

`res/xml/c_preferences.xml`：分類 外觀/語言/歌詞/桌布/媒體/其他。外觀用 `ListPreference`（key `Theme`，entryValues `SYSTEM/LIGHT/DARK`）；語言用 `ListPreference`（key `Locale`）；字級用 `SeekBarPreference`（key `TextSize`，25～50）；文字色用 `ListPreference`（key `TextColor`）；其餘 `Preference`：`lyricsLanguage`、`bg`（12 桌布 → 子 `ListPreference`）、`sbguser`、`media_config`、`check_update`、`permission_request`、`online_help`、`about`。

- [ ] **Step 2: `SettingsFragment` 用 change listener（不是 click 轉接）**

`ui/settings/SettingsFragment.kt` extends `PreferenceFragmentCompat`。`onCreatePreferences` inflate `c_preferences`，並為每個 key 設 `OnPreferenceChangeListener`：
- `Theme` → `ThemePrefs.apply(context, NightMode.from(newValue))`，`activity.recreate()`。
- `Locale` → `LocaleStore.set(requireContext(), AppLanguage.from(newValue))` + restart。
- `TextSize` → `MainActivity.setFontSize` 邏輯改寫到一個共用 `HomePrefs` 或直接寫 `SharedPreferences`（`PREF_TEXT_SIZE`），`HomeFragment` 讀取套用。
- `TextColor` → 寫 `PREF_TEXT_COLOR`。
- 其餘 `Preference` 的 `onPreferenceClick` → `startActivity`（`ChineseS2TSelection`/`WallPaperUtil`/`MediaConfig`/`About`）、`UpdateServiceImpl.checkForUpdates()`、系統權限頁、FAQ URL。

> `ListPreference`/`SeekBarPreference` 靠 change listener 套用並更新 summary；不複製 `MainActivity.onOptionsItemSelected` 的 view 操作。

- [ ] **Step 3: 陣列資源**

`res/values/array.xml` 補：`c_night_modes`/`c_night_mode_values`（跟隨系統/淺色/深色）、`c_locales`/`c_locale_values`、`c_font_colors`/`c_font_color_values`、`c_wallpapers`/`c_wallpaper_values`。

- [ ] **Step 4: 手動驗證（獨立預覽）+ Commit**

以臨時 host 預覽；正式接線在 HOST。commit：

```bash
git add hymnchtv/src/main/java/org/cog/hymnchtv/ui/settings/ \
        hymnchtv/src/main/res/xml/c_preferences.xml \
        hymnchtv/src/main/res/layout/fragment_settings.xml \
        hymnchtv/src/main/res/values/array.xml \
        hymnchtv/src/main/res/values/strings_c.xml
git commit -m "feat(c): settings tab (PreferenceFragmentCompat + change listeners)"
```

---

## Task M1：我的詩歌分頁（D-1 空槽位，C-1/C-2）

**Files:** Create `ui/myhymns/MyHymnsFragment.kt`、`res/layout/fragment_myhymns.xml`

- [ ] **Step 1: 空槽位 Fragment**

`ui/myhymns/MyHymnsFragment.kt`：`onCreateView` inflate `fragment_myhymns`（一個空 `FrameLayout` + 佔位 `TextView`「我的詩歌（D-1 完成後啟用）」）。**不 import 任何 `notebook.*` 類別**。D-1 合併後，由 D-1 的整合 task 把 `NotebookHomeFragment` 放進此容器（C-1）。

- [ ] **Step 2: 手動驗證 + Commit**

```bash
git add hymnchtv/src/main/java/org/cog/hymnchtv/ui/myhymns/ \
        hymnchtv/src/main/res/layout/fragment_myhymns.xml
git commit -m "feat(c): my-hymns tab as empty D-1 slot (C-1/C-2)"
```

> **G1 關卡**：Phase 1 四 lane 合併，`assembleDebug` 成功。

---

## Task HOST1：`MainHost` + 主機 layout（`MainActivity` 遷移）

**Files:** Create `res/layout/activity_main_host.xml`、`res/menu/menu_bottom_nav.xml`、`ui/host/MainHost.kt`；Modify `MainActivity.java`

> 從本 Task 起 `MainActivity.java` 才由本 lane 單獨修改。四 fragment 已於 Phase 1 建好。

- [ ] **Step 1: 底部導覽選單（含 icons + contentDescription）**

`res/menu/menu_bottom_nav.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<menu xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:id="@+id/nav_home"      android:icon="@drawable/ic_home"      android:title="@string/c_nav_home" />
    <item android:id="@+id/nav_toc"       android:icon="@drawable/ic_toc"       android:title="@string/c_nav_toc" />
    <item android:id="@+id/nav_my_hymns"  android:icon="@drawable/ic_my_hymns"  android:title="@string/c_nav_my_hymns" />
    <item android:id="@+id/nav_settings"  android:icon="@drawable/ic_settings"  android:title="@string/c_nav_settings" />
</menu>
```

（`ic_*` 用 M3 標準圖示 vector drawable；新增於 `res/drawable/`。）

- [ ] **Step 2: 主機 layout（含 Toolbar + insets）**

`res/layout/activity_main_host.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/viewMain"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fitsSystemWindows="true">

    <com.google.android.material.appbar.MaterialToolbar
        android:id="@+id/toolbar"
        android:layout_width="0dp"
        android:layout_height="?attr/actionBarSize"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <androidx.fragment.app.FragmentContainerView
        android:id="@+id/fragment_container"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintTop_toBottomOf="@id/toolbar"
        app:layout_constraintBottom_toTopOf="@id/bottom_nav"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />

    <com.google.android.material.bottomnavigation.BottomNavigationView
        android:id="@+id/bottom_nav"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        app:menu="@menu/menu_bottom_nav"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent" />
</androidx.constraintlayout.widget.ConstraintLayout>
```

- [ ] **Step 3: `MainHost`（保存 selected tab + 避免重複 add）**

`ui/host/MainHost.kt`：

```kotlin
package org.cog.hymnchtv.ui.host

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.cog.hymnchtv.R
import org.cog.hymnchtv.ui.home.HomeFragment
import org.cog.hymnchtv.ui.myhymns.MyHymnsFragment
import org.cog.hymnchtv.ui.settings.SettingsFragment
import org.cog.hymnchtv.ui.toc.TocFragment

class MainHost(private val activity: FragmentActivity) {
    private val fm = activity.supportFragmentManager
    private val tabs: Map<Int, () -> Fragment> = linkedMapOf(
        R.id.nav_home to { HomeFragment() },
        R.id.nav_toc to { TocFragment() },
        R.id.nav_my_hymns to { MyHymnsFragment() },
        R.id.nav_settings to { SettingsFragment() },
    )
    private var current: Int = R.id.nav_home

    /** @param savedTab 從 `MainActivity.onCreate(savedInstanceState)` 讀出的上次分頁（rev 3：不得讀 `activity.intent`）。 */
    fun attach(nav: BottomNavigationView, savedTab: Int) {
        nav.setOnItemSelectedListener { item ->
            if (tabs.containsKey(item.itemId)) { show(item.itemId); true } else false
        }
        show(if (tabs.containsKey(savedTab)) savedTab else R.id.nav_home)
    }

    fun onSaveState(out: Bundle) { out.putInt(EXTRA_TAB, current) }

    private fun show(tabId: Int) {
        if (tabId == current && fm.findFragmentByTag("tab:$tabId") != null) return
        val tx = fm.beginTransaction()
        fm.findFragmentByTag("tab:$tabId")?.let { tx.show(it) }
            ?: tx.add(R.id.fragment_container, tabs.getValue(tabId)(), "tab:$tabId")
        fm.findFragmentByTag("tab:$current")?.let { tx.hide(it) }
        tx.commit()
        current = tabId
    }

    companion object { const val EXTRA_TAB = "c_selected_tab" }
}
```

- [ ] **Step 4: `MainActivity` 接主機（一次性搬移，避免 crash）**

`MainActivity` 加欄位 `private org.cog.hymnchtv.ui.host.MainHost mainHost;`，`onCreate` 做**同一 commit 內**的完整切換：

```java
setContentView(R.layout.activity_main_host);
setSupportActionBar(findViewById(R.id.toolbar));
mainHost = new MainHost(this);
int savedTab = (savedInstanceState == null) ? R.id.nav_home
        : savedInstanceState.getInt(MainHost.EXTRA_TAB, R.id.nav_home);
mainHost.attach(findViewById(R.id.bottom_nav), savedTab);
handleIntent(getIntent());
getOnBackPressedDispatcher().addCallback(backPressedCallback);
```

`onSaveInstanceState` 改為（**不再存取 `mEntry`**，避免 null）：

```java
@Override protected void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    mainHost.onSaveState(outState);
}
```

**同一 commit 內移除的 home 欄位/方法（這些 id 已不存在，留著會 null crash）：**
- 欄位：`btn_n0`…`btn_n9`、`btn_fu`、`btn_del`、`btn_db/bb/er/xb/xg/yb`、`btn_search`、`btn_update`、`btn_english`、`mEntry`、`tv_Search`、`mHistoryListView`、`mTocSpinner`、`mTocSpinnerItem`、`mHistoryAdapter`、`background`、`sNumber`、`isFu`、`isToc`、`autoClear`、`mFontSize`、`mFontColor`、`mTocPage`、`mFsDelta`。
- 方法：`initButton`、`onNumberClick`、`onHymnButtonClicked`、`showHymnFromEng`、`showHymnToc`、`initHistoryList`、`showHymn`、`setFontSize`、`setFontColor`、`setWallpaper`、`setBgColor`、`initTocSpinnerItem`、`initUserSettings`、`initLanguage`、`onItemSelected`/`onNothingSelected`（`OnItemSelectedListener` 介面一併移除）。
- `onResume` 裏對 `btn_update` 的動畫/可見性（`mHasUpdate` 提示改由設定分頁「檢查更新」呈現，本版先移除該段）。
- `backPressedCallback` 改為：**非首頁分頁 → 回首頁；首頁 → `finish()`**（不再存取 `mHistoryListView`/`mEntry`）。

**保留（其它類別依賴）：** `showContent`（static）、`setHymnTypeNo`（static）、`mHymnType`/`mHymnNo`（static）、`getHymnType`（static）、`ybXTable`/`createYbXTable`（static）、`HYMN_*`/`ATTR_*`/`PREF_*` 常數、`HYMN_YB_ALT`、`handleIntent`/`onNewIntent`/`getFile`、`setAppTheme`（委託版）、`setAppLocale`/`doRestart`、`onRequestPermissionsResult`、`onInfoButtonClicked`、`isDeviceLocked`/`getInstance`。

- [ ] **Step 5: 手動驗證 + Commit**

手動：四分頁切換、旋轉後回到原分頁、share intent、`SmokeFlowTest` 仍綠。

```bash
git add hymnchtv/src/main/res/layout/activity_main_host.xml \
        hymnchtv/src/main/res/menu/menu_bottom_nav.xml \
        hymnchtv/src/main/res/drawable/ic_*.xml \
        hymnchtv/src/main/java/org/cog/hymnchtv/ui/host/MainHost.kt \
        hymnchtv/src/main/java/org/cog/hymnchtv/MainActivity.java
git commit -m "feat(c): MainActivity host with toolbar + bottom nav"
```

---

## Task HOST2：移除 `menu_main.xml` 與舊首頁殘留

**Files:** Modify `MainActivity.java`；Delete `res/menu/menu_main.xml`、`res/layout/main.xml`

- [ ] **Step 1: 移除 `menu_main` inflate 與 `onOptionsItemSelected` 的分支**

`MainActivity` 移除 `onCreateOptionsMenu`/`onCreateContextMenu`/`onContextItemSelected`/`onOptionsItemSelected` 對 `menu_main` 的處理（功能已在 `SettingsFragment`）。`git rm res/menu/menu_main.xml`。

- [ ] **Step 2: 移除舊 `main.xml`**

`git rm res/layout/main.xml`（其 id 已全在 `fragment_home.xml`）。

- [ ] **Step 3: 手動回歸 + Commit**

手動：無選單、所有設定在設定分頁、`SmokeFlowTest` 綠。

```bash
git rm hymnchtv/src/main/res/menu/menu_main.xml hymnchtv/src/main/res/layout/main.xml
git add hymnchtv/src/main/java/org/cog/hymnchtv/MainActivity.java
git commit -m "refactor(c): remove menu_main + old main layout"
```

> **G2 關卡**：四分頁切換 + share intent + `SmokeFlowTest` 綠。

---

## Task L1：歌詞頁版式（詩本紙本，方向 B）+ 文楷字型

**Files:** Modify `res/layout/content_lyrics.xml`、`res/values/c_dimens.xml`、`ContentView.java`；Create `ui/lyrics/LyricsMeta.kt`

- [ ] **Step 1: 文楷字型（已於 Task 4 綁入，直接引用）**

字型 `@font/lxgw_wenkai` 已於 Task 4 綁入（硬關卡）。在 `content_lyrics.xml` 的歌詞 `ZoomTextView` 設：

```xml
android:fontFamily="@font/lxgw_wenkai"
```

**不**用 serif 過渡（文楷是方向 B 的必要元素，spec §3.2）。

- [ ] **Step 2: 宣紙白背景 + 24sp**

`content_lyrics.xml` 的 `lyricsView` 根加 `android:background="@color/c_paper_white"`（新色，宣紙白，實際色值依設計稿）。歌詞 `ZoomTextView` 改 `android:textSize="24sp"`（方向 B 預設）。

- [ ] **Step 3: 拍號/調號（第 4 行）**

`content_lyrics.xml` 在 `contentView`（譜圖）與歌詞文字間加 `@id/meter_key`（`TextView`）。`ContentView.showLyricsChText` 解析歌詞檔**第 4 行**（如「降A大调4/4」）填入；缺值（如 ER 無此行）則 `GONE`。抽 `ui/lyrics/LyricsMeta.kt` 的 `parseMeterKey(lines: List<String>): String?` 純函式。

- [ ] **Step 4: 編譯 + 手動驗證**

手動：歌詞頁白底、文楷（或 serif 過渡）、24sp、拍號調號顯示、譜圖正常。

- [ ] **Step 5: Commit**

```bash
git add hymnchtv/src/main/res/layout/content_lyrics.xml \
        hymnchtv/src/main/res/values/c_dimens.xml \
        hymnchtv/src/main/java/org/cog/hymnchtv/ContentView.java \
        hymnchtv/src/main/java/org/cog/hymnchtv/ui/lyrics/LyricsMeta.kt
git commit -m "feat(c): lyrics paper style + WenKai + meter/key from line 4"
```

---

## Task L2：歌詞頁頂列按鈕（取代 `menu_content` 長按選單）

**Files:** Modify `res/layout/content_lyrics.xml`、`ContentView.java`、`ContentHandler.java`、`res/menu/menu_content.xml`

- [ ] **Step 1: 頂列按鈕列**

`content_lyrics.xml` 頂部加 `@id/lyrics_top_bar`（`LinearLayout`），放：`button_ts`（簡繁）、`button_english`（中英）、`btn_score_color`（譜顏色）、`btn_font_dec`/`btn_font_inc`（字級）、`btn_share`（分享）、`btn_media`（媒體設定）、`btn_more`（overflow：英文刪除/說明/回首頁）。

- [ ] **Step 2: `ContentView`/`ContentHandler` 接按鈕**

把 `ContentHandler.onContextItemSelected` 對應分支邏輯接到按鈕 click。停用 `registerForContextMenu`（`menu_content` 不再長按彈出）。

- [ ] **Step 3: 手動驗證 + Commit**

手動：所有按鈕可用、長按不再彈選單。

```bash
git add hymnchtv/src/main/res/layout/content_lyrics.xml \
        hymnchtv/src/main/java/org/cog/hymnchtv/ContentView.java \
        hymnchtv/src/main/java/org/cog/hymnchtv/ContentHandler.java
git commit -m "feat(c): lyrics top bar replaces context menu"
```

---

## Task L3：歌詞頁「下一首」+ 筆記本列槽位（C-5/C-6，slot 在 `content_main.xml`）

**Files:** Modify `res/layout/content_main.xml`、`ContentHandler.java`、`ui/lyrics/`（介面）

- [ ] **Step 1: 筆記本列槽位放 `content_main.xml`（單一宿主，rev 2 #9）**

`content_main.xml` 在 `ViewPager2` 與 `mediaPlayer` 之間（或標題列）加 `@id/notebookBar`（`FrameLayout`）。**不放 `content_lyrics.xml`**（那是每頁 `ContentView`，會產生多個列）。C 只提供空容器（`visibility="gone"`）；D-1 的 `HymnNotebookBarFragment` 於其整合 task（I1）放進來並把容器設為 `visible`。

- [ ] **Step 2: 「下一首」按鈕（C-6）**

`content_lyrics.xml` 頂列加 `@id/btn_next`。click → 呼叫既有 `scrollNextHymn()`（同本翻下一首）當預設；D-1 合併後由 D-1 接 `HymnBarViewModel.nextInPlaylist()`。**C 不 import notebook 類別**，只留按鈕 id 與 `scrollNextHymn` fallback。

- [ ] **Step 3: 手動驗證 + Commit**

手動：下一首翻頁、`notebookBar` 容器存在（空）。

```bash
git add hymnchtv/src/main/res/layout/content_main.xml \
        hymnchtv/src/main/res/layout/content_lyrics.xml \
        hymnchtv/src/main/java/org/cog/hymnchtv/ContentHandler.java
git commit -m "feat(c): next button + notebook bar slot in content_main (C-5/C-6)"
```

---

## Task L4：紅色節數 span + 播放器列樣式（保留 `SeekBar`）

**Files:** Modify `ui/lyrics/LyricsMeta.kt`、`ContentView.java`、`res/layout/media_player_audio_ui.xml`、`MediaGuiController.java`

- [ ] **Step 1: 紅色節數 span**

`ContentView.showLyricsChText` 讀入歌詞後用 `SpannableString` 把節數行（`^[一二三四五六七八九十\d]+$` 或「第X節」）上色 `@color/c_verse_red` 並加粗。抽 `LyricsMeta.applyVerseSpans(text: CharSequence): CharSequence` 純函式 + 單元測試（span 數量/顏色）。

- [ ] **Step 2: 播放器列 M3 樣式（**保留 `SeekBar`，不換 `Slider`**）**

`media_player_audio_ui.xml` 的控制項只改**顏色/樣式**（`MaterialButton`/`MaterialRadioButton`），**保留 `SeekBar`**（rev 2 #10：`MediaGuiController` 依賴 `SeekBar.OnSeekBarChangeListener` 且 `playback_play` 是 `ImageView`+`AnimationDrawable`）。**不**把 `playback_play` 換 `MaterialButton`（會破壞 `AnimationDrawable` 背景）。僅「連播」開關（長按 play 的替代，spec §4.3 #8）加一個可見 `CheckBox`，接 `confirmAutoStream` 邏輯。

- [ ] **Step 3: 測試 + 手動 + Commit**

`test/.../ui/lyrics/LyricsMetaTest.kt` 驗證 span。手動：節數紅色、播放器列樣式、連播開關。

```bash
git add hymnchtv/src/main/java/org/cog/hymnchtv/ui/lyrics/ \
        hymnchtv/src/test/java/org/cog/hymnchtv/ui/lyrics/ \
        hymnchtv/src/main/java/org/cog/hymnchtv/ContentView.java \
        hymnchtv/src/main/res/layout/media_player_audio_ui.xml \
        hymnchtv/src/main/java/org/cog/hymnchtv/MediaGuiController.java
git commit -m "feat(c): red verse spans + restyled player bar (keep SeekBar)"
```

> **G3 關卡**：`api34nb` 上 `SmokeFlowTest` 仍綠；接點 `showHymn`/`onPlaybackCompleted`/`NotebookBarHost` 完好（C-10）。

---

## Task F1：移除長按/舊選單，搬移次要入口

**Files:** Modify `MainActivity.java`、`About.java`、`MediaConfig.java`、`ContentView.java`、`MediaGuiController.java`；Delete `res/menu/menu_content.xml`；Modify `AndroidManifest.xml`（移除 `HymnToc` 若已遷移）

- [ ] **Step 1: 逐項核對 spec §4.3 的 13 個長按**

#1（主畫面背景選單）已於 HOST2 移除；#2/#3（詩歌本/英中 alt）若保留長按，加 `contentDescription` 提示；#4（歷史刪除）已有可見按鈕（H1）；#5/#6（歌詞設定/英文重載）已由 L2 頂列取代；#7（歌詞頁選單）已停用（L2）；#8（連播）已加開關（L4）；#9–#12（QQ/Notion/BibleTool/Google 替代搜尋）移除；#13（About 長按查更新）改設定分頁「檢查更新」。

- [ ] **Step 2: 次要入口**

`About`/`MediaConfig`/`WallPaperUtil`/`ChineseS2TSelection` 入口已在設定分頁（S1）。移除 `About` 的 `history_log` 長按查更新。刪 `menu_content.xml`。

- [ ] **Step 3: 移除 `HymnToc` Activity（若 T 已完全遷移）**

`AndroidManifest.xml` 移除 `<activity android:name=".HymnToc" />`（僅在確認無其它呼叫者後）。

- [ ] **Step 4: 手動回歸 + Commit**

手動：無長按隱藏功能；所有功能在分頁/按鈕找到。

```bash
git rm hymnchtv/src/main/res/menu/menu_content.xml
git add hymnchtv/src/main/java/org/cog/hymnchtv/MainActivity.java \
        hymnchtv/src/main/java/org/cog/hymnchtv/About.java \
        hymnchtv/src/main/java/org/cog/hymnchtv/ContentView.java \
        hymnchtv/src/main/java/org/cog/hymnchtv/MediaGuiController.java \
        hymnchtv/src/main/AndroidManifest.xml
git commit -m "refactor(c): remove long-press/menu; move entries to settings"
```

---

## Task F2：對帳收尾（spec §9 逐條）

**Files:** 核對 spec §9 逐條；`AndroidManifest.xml`（若移除 `HymnToc`）

> 字型已於 Task 4 綁入（不再走 `gen_font_subset.py`，那管線是 D-1 的後續產物）。

- [ ] **Step 1: 依 spec §9 逐條核對 C-1～C-11**

把未滿足項補完（尤其 C-5 若 D-1 選選項 b、C-9 筆記本偏好嵌入）。把對帳清單寫進 PR 描述。

- [ ] **Step 2: 全面回歸 + Commit**

手動：spec §4 功能對應表逐項有落點、§4.3 長按全移除/可見化、`SmokeFlowTest` 綠。

```bash
git add -A
git commit -m "refactor(c): regenerate font subset + reconcile D-1 contract"
```

> **G4 關卡**：spec §4 逐項、`SmokeFlowTest` 全綠。

---

## 對帳清單（寫進 C 與 D-1 後合併方的 PR 描述）

- C-3 主題 parent：D-1 的 `Theme.Hymnchtv.Notebook.*` parent 改接 C 的 `AppTheme`（或刪除、`NotebookActivity` 用 C 主題）。
- C-1/C-2 宿主與 Toolbar：`NotebookHomeFragment` 進 `MyHymnsFragment`；Toolbar 由 `MainActivity` 的 `MaterialToolbar` 提供。
- C-5 擇一：歌詞頁筆記本列用 `content_main.xml` 的 `@id/notebookBar`（選項 a）或 C 自己 render `HymnBarViewModel.state`（選項 b）。
- C-8 擁有權：`HymnTitleSource`/`AssetHymnTitles` 由先合併方擁有，後合併方重用（C 的 `ui/titles/` 與 D-1 的 `notebook/ui/titles/` 擇一）。
- C-11 字型：LXGW WenKai 於 Task 4 綁入 `res/font/`；若與 D-1 的字型任務衝突，不手動合併二進位檔，擇一後重跑字型驗證（D-1 的 `FontSubsetTest` 若日後建立）。
