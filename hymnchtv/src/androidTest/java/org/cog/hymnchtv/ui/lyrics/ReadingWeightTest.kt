package org.cog.hymnchtv.ui.lyrics

import android.content.Context
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

    private fun boldFace(): Typeface? = LyricsTypefaces.peek(false, true)

    @Test
    fun panelBoldSwitchesToTheMediumFileAndBackToRegular() {
        launch().use { s ->
            s.await("regular face") { lyrics(s).typeface === regularFace() && regularFace() != null }
            s.openPanel()
            onView(withId(R.id.aa_weight_bold)).perform(click())
            assertThat(ReadingPrefs.lyricsWeight(prefs)).isEqualTo(LyricsWeight.BOLD)
            assertThat(prefs.getString("lyrics_font_weight", null)).isEqualTo("bold")
            s.await("Medium face applied") { boldFace() != null && lyrics(s).typeface === boldFace() }
            assertThat(boldFace()).isNotSameInstanceAs(regularFace())
            // A real weight, not synthetic bolding: the Medium face is drawn as-is
            assertThat(lyrics(s).typeface.style).isEqualTo(Typeface.NORMAL)
            onView(withId(R.id.aa_weight_regular)).perform(click())
            s.await("Regular face applied") { lyrics(s).typeface === regularFace() }
        }
    }

    @Test
    fun weightAlsoWorksWithTheSystemFont() {
        launch().use { s ->
            s.openPanel()
            onView(withId(R.id.aa_font_system)).perform(click())
            onView(withId(R.id.aa_weight_bold)).perform(click())
            instrumentation.waitForIdleSync()
            assertThat(lyrics(s).typeface.isBold).isTrue()
            onView(withId(R.id.aa_weight_regular)).perform(click())
            instrumentation.waitForIdleSync()
            assertThat(lyrics(s).typeface.isBold).isFalse()
        }
    }

    @Test
    fun theAdjacentPageFollowsTheChange() {
        launch().use { s ->
            s.openPanel()
            onView(withId(R.id.aa_weight_bold)).perform(click())
            s.await("Medium face applied") { boldFace() != null && lyrics(s).typeface === boldFace() }
            s.onActivity {
                val pager = it.findViewById<ViewPager2>(R.id.viewPager)
                pager.setCurrentItem(pager.currentItem + 1, false)
            }
            s.awaitPage()
            s.await("next page in Medium") { lyrics(s).typeface === boldFace() }
        }
    }

    @Test
    fun settingsScreenWritesTheSameKeyAndTheLyricsPageFollowsOnReturn() {
        ActivityScenario.launch(ReadingSettingsActivity::class.java).use { settings ->
            settings.onActivity { activity ->
                val fragment = activity.supportFragmentManager.fragments.filterIsInstance<ReadingSettingsFragment>().single()
                val pref = fragment.findPreference<ListPreference>(ReadingPrefKeys.LYRICS_FONT_WEIGHT)!!
                assertThat(pref.value).isEqualTo("regular")
                assertThat(pref.callChangeListener("bold")).isTrue()
            }
        }
        assertThat(ReadingPrefs.lyricsWeight(prefs)).isEqualTo(LyricsWeight.BOLD)
        launch().use { s ->
            s.onActivity { it.onReadingSettingsReturned(true) }
            s.awaitPage()
            s.await("Medium face after settings") { boldFace() != null && lyrics(s).typeface === boldFace() }
        }
    }
}
