package org.cog.hymnchtv.ui.lyrics

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.viewpager2.widget.ViewPager2
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.ContentHandler
import org.cog.hymnchtv.ContentView
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.TestPermissions
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

/** Lyrics top bar (plan C-4): every former context-menu entry has a visible control; long-press no longer opens a menu. */
@RunWith(AndroidJUnit4::class)
class LyricsTopBarTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()
    private var mainScenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            TestPermissions.grantLaunchPermission(ctx.packageName)
            mainScenario = ActivityScenario.launch(MainActivity::class.java)
        }
    }

    @After
    fun tearDown() {
        mainScenario?.close()
    }

    private fun launch(type: String = MainActivity.HYMN_ER, number: Int = 1): ActivityScenario<ContentHandler> {
        val extras = Bundle().apply {
            putString(MainActivity.ATTR_HYMN_TYPE, type)
            putInt(MainActivity.ATTR_HYMN_NUMBER, number)
        }
        return ActivityScenario.launch<ContentHandler>(Intent(ctx, ContentHandler::class.java).putExtras(extras))
            .also { it.waitForPage() }
    }

    private fun <T> ActivityScenario<ContentHandler>.read(block: (ContentHandler) -> T): T {
        val ref = AtomicReference<T>()
        onActivity { ref.set(block(it)) }
        return ref.get()
    }

    private fun page(activity: ContentHandler): View? =
        activity.supportFragmentManager.fragments.filterIsInstance<ContentView>().singleOrNull { it.isResumed }?.view

    private fun ActivityScenario<ContentHandler>.waitForPage() {
        val end = SystemClock.uptimeMillis() + 10_000
        while (read { page(it) } == null) {
            check(SystemClock.uptimeMillis() < end) { "timed out waiting for the lyrics page" }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            SystemClock.sleep(50)
        }
    }

    @Test
    fun topBarButtonsAreShown() {
        launch().use {
            listOf(R.id.btn_score_color, R.id.btn_font_dec, R.id.btn_font_inc, R.id.btn_next, R.id.btn_more).forEach { id ->
                onView(withId(id)).check(matches(isDisplayed()))
            }
        }
    }

    @Test
    fun overflowListsTheEntriesWithoutAButton() {
        launch().use {
            onView(withId(R.id.btn_more)).perform(click())
            listOf(R.string.reading_settings, R.string.menu_media_ui_toggle, R.string.help, R.string.home).forEach { s ->
                onView(withText(s)).inRoot(isPlatformPopup()).check(matches(isDisplayed()))
            }
        }
    }

    @Test
    fun nextButtonTurnsToTheNextHymn() {
        launch().use { scenario ->
            fun current() = scenario.read { it.findViewById<ViewPager2>(R.id.viewPager).currentItem }
            val before = current()
            onView(withId(R.id.btn_next)).perform(click())
            val end = SystemClock.uptimeMillis() + 5_000
            while (current() == before) {
                check(SystemClock.uptimeMillis() < end) { "page did not advance" }
                SystemClock.sleep(50)
            }
            assertThat(current()).isEqualTo(before + 1)
        }
    }

    @Test
    fun fontButtonsAndScoreColorChangeTheirSettings() {
        launch().use { scenario ->
            val prefs = ctx.getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE)
            val scoreBefore = prefs.getInt(ContentView.PREF_SCORE_COLOR, 0)
            onView(withId(R.id.btn_score_color)).perform(click())
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            assertThat(prefs.getInt(ContentView.PREF_SCORE_COLOR, 0)).isNotEqualTo(scoreBefore)

            val key = if (scenario.read { it.resources.configuration.orientation } == 2) ContentView.PREF_LYRICS_SCALE_L
            else ContentView.PREF_LYRICS_SCALE_P
            val sizeBefore = prefs.getFloat(key, -1f)
            onView(withId(R.id.btn_font_inc)).perform(click())
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            assertThat(prefs.getFloat(key, -1f)).isNotEqualTo(sizeBefore)
            prefs.edit().remove(ContentView.PREF_SCORE_COLOR).remove(key).commit()
        }
    }

    @Test
    fun meterAndKeyShownAboveTheLyricsAndRemovedFromTheText() {
        launch(MainActivity.HYMN_ER, 1).use { scenario ->
            val meter = scenario.read { page(it)!!.findViewById<TextView>(R.id.meter_key) }
            assertThat(meter.visibility).isEqualTo(View.VISIBLE)
            assertThat(meter.text.toString()).contains("4/4")
            val lyrics = scenario.read { page(it)!!.findViewById<TextView>(R.id.lyrics_simplified).text.toString() }
            assertThat(lyrics).doesNotContain("4/4")
        }
    }

    @Test
    fun hymnWithoutKeyHidesTheMeterLine() {
        launch(MainActivity.HYMN_XB, 1).use { scenario ->
            assertThat(scenario.read { page(it)!!.findViewById<View>(R.id.meter_key).visibility }).isEqualTo(View.GONE)
        }
    }
}
