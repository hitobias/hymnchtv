package org.cog.hymnchtv.ui.lyrics

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeRight
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import org.hamcrest.Matcher
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.HymnsApp
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.reading.DisplayMode
import org.cog.hymnchtv.reading.LyricsFont
import org.cog.hymnchtv.reading.LyricsFontSize
import org.cog.hymnchtv.reading.LyricsScale
import org.cog.hymnchtv.reading.ReadingPrefKeys
import org.cog.hymnchtv.reading.ReadingPrefs
import org.cog.hymnchtv.reading.background.BackgroundPreset
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Visual redesign 6b/7: the Aa panel's four controls take effect on the page at once and write the settings' keys. */
@RunWith(AndroidJUnit4::class)
class ReadingPanelTest : LyricsTestBase() {
    private val prefs get() = ctx.getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE)

    @Before
    fun fixture() {
        prefs.edit()
            .putBoolean(ReadingPrefKeys.MENU_SHOW, false)
            .putString(ReadingPrefKeys.DISPLAY_MODE, DisplayMode.SCORE_AND_LYRICS.name)
            .putString(ReadingPrefKeys.LYRICS_FONT_SIZE, LyricsFontSize.MEDIUM.name)
            .putString(ReadingPrefKeys.LYRICS_FONT, LyricsFont.KAI.name)
            .putString("LyricsBackground", BackgroundPreset.XUAN.id)
            // a stored pinch scale that would hide an enum change if it were not reset
            .putFloat(ReadingPrefKeys.LYRICS_SCALE_P, 3.2f)
            .putFloat(ReadingPrefKeys.LYRICS_SCALE_L, 3.2f)
            .commit()
    }

    @After
    fun cleanFixture() {
        prefs.edit().remove(ReadingPrefKeys.MENU_SHOW).remove(ReadingPrefKeys.DISPLAY_MODE).remove(ReadingPrefKeys.LYRICS_FONT_SIZE)
            .remove(ReadingPrefKeys.LYRICS_FONT).remove("LyricsBackground").remove(ReadingPrefKeys.LYRICS_SCALE_P)
            .remove(ReadingPrefKeys.LYRICS_SCALE_L).commit()
    }

    private fun androidx.test.core.app.ActivityScenario<org.cog.hymnchtv.ContentHandler>.openPanel() {
        revealChrome()
        onActivity { it.setChromeHeld(true); it.showReadingPanel() }
        instrumentation.waitForIdleSync()
        onView(withId(R.id.aa_size_slider)).check(matches(isDisplayed()))
    }

    private fun lyricsView(s: androidx.test.core.app.ActivityScenario<org.cog.hymnchtv.ContentHandler>) =
        s.read { page(it)!!.findViewById<TextView>(R.id.lyrics_simplified) }

    /** The dots sit in a horizontally scrolling row; the view need not be 90 % on screen to be tapped. */
    private fun clickDirectly() = object : ViewAction {
        override fun getConstraints(): Matcher<View> = org.hamcrest.Matchers.any(View::class.java)
        override fun getDescription() = "performClick"
        override fun perform(uiController: UiController, view: View) {
            view.performClick()
            uiController.loopMainThreadUntilIdle()
        }
    }

    private fun sp(value: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, ctx.resources.displayMetrics)

    @Test
    fun aaButtonOpensThePanel() {
        launch().use { s ->
            s.revealChrome()
            s.onActivity { it.setChromeHeld(true) }
            onView(withId(R.id.btn_aa)).perform(click())
            onView(withId(R.id.aa_size_slider)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun themeDotAppliesAtOnceAndWritesTheSlotKey() {
        launch().use { s ->
            val before = s.read { it.lyricsTokens.surface }
            s.openPanel()
            val preset = BackgroundPreset.DIM_GREY
            onView(withContentDescription(ctx.getString(org.cog.hymnchtv.reading.background.BackgroundDrawables.nameRes(preset)))).perform(clickDirectly())
            instrumentation.waitForIdleSync()
            assertThat(prefs.getString("LyricsBackground", null)).isEqualTo(preset.id)
            assertThat(lyricsView(s).currentTextColor).isEqualTo(preset.textColor)
            assertThat(s.read { it.lyricsTokens.surface }).isNotEqualTo(before)
            // the top bar plate follows the new tokens
            val plate = s.read { (page(it)!!.findViewById<View>(R.id.lyrics_top_bar).background as android.graphics.drawable.ColorDrawable).color }
            assertThat(plate).isEqualTo(s.read { it.lyricsTokens.surface })
        }
    }

    @Test
    fun fontSizeAppliesEvenWithAStoredPinchScale() {
        launch().use { s ->
            s.openPanel()
            // A drag anywhere on the track moves the thumb to the finger (user change)
            onView(withId(R.id.aa_size_slider)).perform(swipeRight())
            instrumentation.waitForIdleSync()
            assertThat(ReadingPrefs.fontSize(prefs)).isEqualTo(LyricsFontSize.XLARGE)
            val base = if (HymnsApp.isPortrait) LyricsScale.BASE_SP_PORTRAIT else LyricsScale.BASE_SP_LANDSCAPE
            assertThat(lyricsView(s).textSize).isWithin(1f).of(sp(base * LyricsFontSize.XLARGE.scale))
        }
    }

    @Test
    fun typefaceSwitchesBetweenSystemAndKai() {
        launch().use { s ->
            s.openPanel()
            onView(withId(R.id.aa_font_system)).perform(click())
            instrumentation.waitForIdleSync()
            assertThat(ReadingPrefs.lyricsFont(prefs)).isEqualTo(LyricsFont.SYSTEM)
            assertThat(lyricsView(s).typeface).isSameInstanceAs(Typeface.DEFAULT)
            onView(withId(R.id.aa_font_kai)).perform(click())
            assertThat(ReadingPrefs.lyricsFont(prefs)).isEqualTo(LyricsFont.KAI)
            s.await("Kai face loaded") { page(it)!!.findViewById<TextView>(R.id.lyrics_simplified).typeface !== Typeface.DEFAULT }
        }
    }

    @Test
    fun displayModeApplyEvenWithASessionOverride() {
        launch().use { s ->
            s.onActivity { it.displayModeOverride = DisplayMode.SCORE_ONLY }
            s.openPanel()
            onView(withId(R.id.aa_mode_lyrics)).perform(click())
            instrumentation.waitForIdleSync()
            assertThat(ReadingPrefs.displayMode(prefs)).isEqualTo(DisplayMode.LYRICS_ONLY)
            assertThat(s.read { it.displayModeOverride }).isNull()
            assertThat(s.pageView(R.id.scoreContainer).visibility).isEqualTo(View.GONE)
            assertThat(lyricsView(s).visibility).isEqualTo(View.VISIBLE)
        }
    }

    @Test
    fun theNextPageReadsTheChangedPrefs() {
        launch().use { s ->
            s.openPanel()
            onView(withId(R.id.aa_mode_lyrics)).perform(click())
            instrumentation.waitForIdleSync()
            s.onActivity { it.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager).setCurrentItem(it.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager).currentItem + 1, false) }
            s.awaitPage()
            assertThat(s.pageView(R.id.scoreContainer).visibility).isEqualTo(View.GONE)
        }
    }

    @Test
    fun panelHoldsTheToolbarsWhileOpen() {
        launch().use { s ->
            s.openPanel()
            s.advance(60_000)
            assertThat(s.topBarShown()).isTrue()
        }
    }
}
