package org.cog.hymnchtv.ui.lyrics

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.widget.TextView
import androidx.preference.ListPreference
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.viewpager2.widget.ViewPager2
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.ContentHandler
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.reading.DisplayMode
import org.cog.hymnchtv.reading.LyricsFont
import org.cog.hymnchtv.reading.LyricsTypefaces
import org.cog.hymnchtv.reading.LyricsWeight
import org.cog.hymnchtv.reading.ReadingPrefKeys
import org.cog.hymnchtv.reading.ReadingPrefs
import org.cog.hymnchtv.reading.ReadingSettingsActivity
import org.cog.hymnchtv.reading.ReadingSettingsFragment
import org.cog.hymnchtv.reading.background.BackgroundPreset
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Lyrics weight (Regular | Bold = HymnalKai Medium): Aa panel, reading settings and the adjacent page all agree. */
@RunWith(AndroidJUnit4::class)
class ReadingWeightTest : LyricsTestBase() {
    private val prefs get() = ctx.getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE)

    @Before
    fun fixture() {
        prefs.edit()
            .putBoolean(ReadingPrefKeys.MENU_SHOW, false)
            .putString(ReadingPrefKeys.DISPLAY_MODE, DisplayMode.LYRICS_ONLY.name)
            .putString(ReadingPrefKeys.LYRICS_FONT, LyricsFont.KAI.name)
            .putString(ReadingPrefKeys.LYRICS_FONT_WEIGHT, LyricsWeight.REGULAR.prefValue)
            .putString("LyricsBackground", BackgroundPreset.XUAN.id)
            .commit()
    }

    @After
    fun cleanFixture() {
        prefs.edit().remove(ReadingPrefKeys.MENU_SHOW).remove(ReadingPrefKeys.DISPLAY_MODE).remove(ReadingPrefKeys.LYRICS_FONT)
            .remove(ReadingPrefKeys.LYRICS_FONT_WEIGHT).remove("LyricsBackground").commit()
    }

    private fun ActivityScenario<ContentHandler>.openPanel() {
        onActivity { it.setChromeHeld(true); it.showReadingPanel() }
        instrumentation.waitForIdleSync()
        onView(withId(R.id.aa_weight_group)).check(matches(isDisplayed()))
    }

    private fun lyrics(s: ActivityScenario<ContentHandler>): TextView = s.read { page(it)!!.findViewById(R.id.lyrics_simplified) }

    private fun regularFace(): Typeface? = LyricsTypefaces.peek(false, false)

    private fun mediumFace(): Typeface? = LyricsTypefaces.peek(false, true)

    /** Ink (non-white pixels) of one glyph drawn with the view's own typeface and fake-bold flag, at a fixed size. */
    private fun ink(view: TextView): Int {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = view.typeface; isFakeBoldText = view.paint.isFakeBoldText; textSize = 120f; color = Color.BLACK
        }
        val bmp = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        Canvas(bmp).drawText("讚", 20f, 150f, paint)
        val px = IntArray(200 * 200).also { bmp.getPixels(it, 0, 200, 0, 0, 200, 200) }
        return px.sumOf { 255 - Color.green(it) }   // coverage-weighted, so anti-aliased edges count proportionally
    }

    private fun pick(s: ActivityScenario<ContentHandler>, button: Int) {
        onView(withId(button)).perform(click())
        instrumentation.waitForIdleSync()
    }

    @Test
    fun threeStepsSwitchAndEachIsHeavierThanTheLast() {
        launch().use { s ->
            s.await("regular face") { regularFace() != null && lyrics(s).typeface === regularFace() }
            s.openPanel()
            val regularInk = ink(lyrics(s))
            assertThat(lyrics(s).paint.isFakeBoldText).isFalse()

            pick(s, R.id.aa_weight_medium)
            assertThat(prefs.getString("lyrics_font_weight", null)).isEqualTo("medium")
            s.await("Medium face applied") { mediumFace() != null && lyrics(s).typeface === mediumFace() }
            assertThat(lyrics(s).paint.isFakeBoldText).isFalse()
            val mediumInk = ink(lyrics(s))

            pick(s, R.id.aa_weight_bold)
            assertThat(prefs.getString("lyrics_font_weight", null)).isEqualTo("bold")
            s.await("Bold applied") { lyrics(s).typeface === mediumFace() && lyrics(s).paint.isFakeBoldText }
            val boldInk = ink(lyrics(s))

            assertThat(mediumInk).isGreaterThan(regularInk)
            assertThat(boldInk).isGreaterThan(mediumInk)

            pick(s, R.id.aa_weight_regular)
            s.await("Regular again") { lyrics(s).typeface === regularFace() && !lyrics(s).paint.isFakeBoldText }
        }
    }

    @Test
    fun weightAlsoWorksWithTheSystemFont() {
        launch().use { s ->
            s.openPanel()
            onView(withId(R.id.aa_font_system)).perform(click())
            val regular = ink(lyrics(s))
            pick(s, R.id.aa_weight_medium)
            val medium = ink(lyrics(s))
            pick(s, R.id.aa_weight_bold)
            val bold = ink(lyrics(s))
            assertThat(lyrics(s).typeface).isSameInstanceAs(Typeface.DEFAULT_BOLD)
            assertThat(medium).isAtLeast(regular)
            assertThat(bold).isGreaterThan(regular)
            pick(s, R.id.aa_weight_regular)
            assertThat(lyrics(s).typeface).isSameInstanceAs(Typeface.DEFAULT)
        }
    }

    @Test
    fun theAdjacentPageFollowsTheChange() {
        launch().use { s ->
            s.openPanel()
            pick(s, R.id.aa_weight_bold)
            s.await("Bold applied") { mediumFace() != null && lyrics(s).typeface === mediumFace() && lyrics(s).paint.isFakeBoldText }
            s.onActivity {
                val pager = it.findViewById<ViewPager2>(R.id.viewPager)
                pager.setCurrentItem(pager.currentItem + 1, false)
            }
            s.awaitPage()
            s.await("next page in Bold") { lyrics(s).typeface === mediumFace() && lyrics(s).paint.isFakeBoldText }
        }
    }

    @Test
    fun settingsScreenWritesTheSameKeyAndTheLyricsPageFollowsOnReturn() {
        ActivityScenario.launch(ReadingSettingsActivity::class.java).use { settings ->
            settings.onActivity { activity ->
                val fragment = activity.supportFragmentManager.fragments.filterIsInstance<ReadingSettingsFragment>().single()
                val pref = fragment.findPreference<ListPreference>(ReadingPrefKeys.LYRICS_FONT_WEIGHT)!!
                assertThat(pref.value).isEqualTo("regular")
                assertThat(pref.callChangeListener("medium")).isTrue()
            }
        }
        assertThat(ReadingPrefs.lyricsWeight(prefs)).isEqualTo(LyricsWeight.MEDIUM)
        launch().use { s ->
            s.onActivity { it.onReadingSettingsReturned(true) }
            s.awaitPage()
            s.await("Medium face after settings") { mediumFace() != null && lyrics(s).typeface === mediumFace() }
        }
    }
}
