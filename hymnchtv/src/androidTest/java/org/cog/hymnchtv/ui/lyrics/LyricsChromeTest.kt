package org.cog.hymnchtv.ui.lyrics

import android.content.Context
import android.util.TypedValue
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.reading.ReadingPrefKeys
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Visual redesign 6c: toolbar auto-hide, centre tap, inset, page hand-over, TalkBack. Real touches, real 3 s / 4 s timers. */
@RunWith(AndroidJUnit4::class)
class LyricsChromeTest : LyricsTestBase() {
    private val prefs get() = ctx.getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE)

    @Before
    fun fixture() {
        prefs.edit().putBoolean(ReadingPrefKeys.MENU_SHOW, true).remove(LyricsChromeHint.PREF_KEY).commit()
    }

    @After
    fun cleanFixture() {
        prefs.edit().remove(ReadingPrefKeys.MENU_SHOW).remove(LyricsChromeHint.PREF_KEY).commit()
    }

    private fun dp(value: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), ctx.resources.displayMetrics).toInt()

    @Test
    fun startsShownThenFadesAndRecordsTheHint() {
        launch().use { s ->
            assertThat(s.topBarShown()).isTrue()
            assertThat(s.buttonBarShown()).isTrue()
            s.advance(2_999)
            assertThat(s.topBarShown()).isTrue()
            s.hide()
            assertThat(s.buttonBarShown()).isFalse()
            assertThat(prefs.getBoolean(LyricsChromeHint.PREF_KEY, false)).isTrue()
        }
    }

    private fun topBarShownIn(a: org.cog.hymnchtv.ContentHandler) =
        page(a)!!.findViewById<View>(R.id.lyrics_top_bar).visibility == View.VISIBLE

    /** The opening 3 s pass; the fade itself runs for real (150 ms). */
    private fun androidx.test.core.app.ActivityScenario<org.cog.hymnchtv.ContentHandler>.hide() {
        advance(3_000)
        await("fade", 6_000) { !topBarShownIn(it) }
    }

    @Test
    fun centreTapShowsAndAnEdgeTapDoesNot() {
        launch().use { s ->
            s.hide()
            val edge = s.hostPoint(0.05f, 0.5f)
            tap(edge[0], edge[1])
            assertThat(s.topBarShown()).isFalse()
            val top = s.hostPoint(0.5f, 0.1f)
            tap(top[0], top[1])
            assertThat(s.topBarShown()).isFalse()
            val c = s.hostPoint(0.5f, 0.5f)
            tap(c[0], c[1])
            assertThat(s.topBarShown()).isTrue()
            assertThat(s.buttonBarShown()).isTrue()
            // and hides again after 4 s without interaction
            s.advance(3_999)
            assertThat(s.topBarShown()).isTrue()
            s.advance(1)
            s.await("idle fade", 6_000) { !topBarShownIn(it) }
        }
    }

    @Test
    fun centreTapWhileShownHidesAtOnce() {
        launch().use { s ->
            val c = s.hostPoint(0.5f, 0.5f)
            tap(c[0], c[1])
            assertThat(s.topBarShown()).isFalse()
        }
    }

    @Test
    fun longPressDoesNotToggle() {
        launch().use { s ->
            s.hide()
            val c = s.hostPoint(0.5f, 0.5f)
            tap(c[0], c[1], holdMs = 700)
            assertThat(s.topBarShown()).isFalse()
        }
    }

    @Test
    fun scrollAndSwipeDoNotToggle() {
        launch().use { s ->
            s.hide()
            val c = s.hostPoint(0.5f, 0.5f)
            drag(c[0], c[1], 0, -(s.hostPoint(0.5f, 0.5f)[1] / 2))
            assertThat(s.topBarShown()).isFalse()
            val start = s.item()
            val w = s.hostPoint(1f, 0.5f)[0] - s.hostPoint(0f, 0.5f)[0]
            drag(c[0] + w * 2 / 5, c[1], -(w * 17 / 20), 0)   // long enough to pass the half-page mark even after the pager takes over late
            assertThat(s.item()).isEqualTo(start + 1)
            s.awaitPage()
            assertThat(s.topBarShown()).isFalse()
        }
    }

    @Test
    fun newPageFollowsTheCurrentState() {
        launch().use { s ->
            s.hide()
            val c = s.hostPoint(0.5f, 0.5f)
            tap(c[0], c[1])   // shown again
            val w = s.hostPoint(1f, 0.5f)[0] - s.hostPoint(0f, 0.5f)[0]
            val start = s.item()
            drag(c[0] + w * 2 / 5, c[1], -(w * 17 / 20), 0)   // long enough to pass the half-page mark even after the pager takes over late
            s.awaitPage()
            assertThat(s.item()).isEqualTo(start + 1)
            assertThat(s.topBarShown()).isTrue()
            assertThat(s.buttonBarShown()).isTrue()
        }
    }

    @Test
    fun playerCardIsNotPartOfTheAutoHide() {
        launch().use { s ->
            s.hide()
            assertThat(s.read { it.findViewById<View>(R.id.playerUi).visibility }).isEqualTo(View.VISIBLE)
        }
    }

    @Test
    fun touchExplorationKeepsEverythingShown() {
        launch().use { s ->
            s.onActivity { it.setChromeAlwaysVisible(true) }
            s.advance(60_000)
            assertThat(s.topBarShown()).isTrue()
            assertThat(s.buttonBarShown()).isTrue()
            val c = s.hostPoint(0.5f, 0.5f)
            tap(c[0], c[1])
            assertThat(s.topBarShown()).isTrue()
        }
    }

    @Test
    fun heldWhileTheOverflowMenuStaysOpenIsRepresentedByTheHold() {
        launch().use { s ->
            s.onActivity { it.setChromeHeld(true) }
            s.advance(60_000)
            assertThat(s.topBarShown()).isTrue()
            s.onActivity { it.setChromeHeld(false) }
            s.advance(4_000)
            s.await("fade", 6_000) { !topBarShownIn(it) }
        }
    }

    @Test
    fun lyricsPaddingFollowsTheOverlays() {
        launch().use { s ->
            s.onActivity { it.setChromeHeld(true) }   // measure while shown, whatever the launch took
            // The player layer floats over the pager: its reserve and the system bottom inset are part of the padding
            val extra = dp(8) + s.read { it.playerReserve + it.systemBottomInset }
            // shown
            val top = s.pageView(R.id.lyrics_top_bar).height
            val bottom = s.pageView(R.id.lyricsButtonBar).height
            assertThat(top).isGreaterThan(0)
            assertThat(s.scroll().paddingTop).isEqualTo(top)
            assertThat(s.scroll().paddingBottom).isEqualTo(bottom + extra)
            s.onActivity { it.setChromeHeld(false) }
            s.advance(4_000)
            s.await("padding") { page(it)!!.findViewById<android.widget.ScrollView>(R.id.lyrics_scroll).paddingTop == 0 }
            assertThat(s.scroll().paddingBottom).isEqualTo(extra)
        }
    }

    @Test
    fun topBarHasFiveEqualButtonsAtAnyWidth() {
        launch().use { s ->
            val bar = s.pageView(R.id.lyrics_top_bar) as android.view.ViewGroup
            assertThat(bar.childCount).isEqualTo(5)
            val widths = (0 until 5).map { bar.getChildAt(it).width }
            assertThat(widths.max() - widths.min()).isAtMost(1)   // equal weights, at most a pixel of rounding
            (0 until 5).forEach { assertThat(bar.getChildAt(it).height).isAtLeast(dp(48)) }
        }
    }
}
