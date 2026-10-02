package org.cog.hymnchtv

import android.content.Context
import android.content.Intent
import android.os.Build
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

    /** API 24-28: FileBackend.getHymnchtvStore() reads MainActivity.getInstance(), which is null unless MainActivity exists. */
    private var mainScenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            TestPermissions.grantLaunchPermission(ctx.packageName)
            mainScenario = ActivityScenario.launch(MainActivity::class.java)
        }
        prefs.edit()
            .putString(ReadingPrefKeys.DISPLAY_MODE, DisplayMode.SCORE_AND_LYRICS.name)
            .putBoolean(ReadingPrefKeys.KEEP_SCREEN_ON, true)
            .putBoolean(ReadingPrefKeys.MENU_SHOW, false)
            .putString(LyricsLanguagePolicy.PREF_LYRICS_DEFAULT, LyricsLang.SIMPLIFIED.name)
            .commit()
    }

    @After
    fun tearDown() {
        mainScenario?.close()
        mainScenario = null
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

    @Test
    fun returningToAPageHiddenByAnEarlierModeShowsLyricsAgain() {
        launch().use { scenario ->
            scenario.waitFor("page") { page(it) != null }
            scenario.click(R.id.button_mode)   // -> SCORE_ONLY: every lyrics view GONE
            scenario.onActivity { it.displayModeOverride = DisplayMode.SCORE_AND_LYRICS }
            // Resuming the page re-applies the mode; lyrics must not stay hidden (refreshLyrics = false path)
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.STARTED)
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
            scenario.waitFor("lyrics back") { visible(it, R.id.lyrics_simplified) || visible(it, R.id.lyrics_traditional) }
        }
    }
}
