package org.cog.hymnchtv

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.ScrollView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.viewpager2.widget.ViewPager2
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.reading.DisplayMode
import org.cog.hymnchtv.reading.ReadingPrefKeys
import org.cog.hymnchtv.ui.lyrics.LyricsChromeHint
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

/**
 * Lyrics page gestures: a mostly vertical drag (even with some sideways drift) only scrolls the lyrics,
 * a clearly horizontal swipe still turns the page. Raw MotionEvents are injected through Instrumentation.
 */
@RunWith(AndroidJUnit4::class)
class LyricsSwipeTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val prefs = ctx.getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE)
    private var mainScenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            TestPermissions.grantLaunchPermission(ctx.packageName)
            mainScenario = ActivityScenario.launch(MainActivity::class.java)
        }
        // Score + lyrics: tall enough to scroll vertically.
        prefs.edit()
            .putString(ReadingPrefKeys.DISPLAY_MODE, DisplayMode.SCORE_AND_LYRICS.name)
            .putBoolean(ReadingPrefKeys.MENU_SHOW, false)
            .putBoolean(LyricsChromeHint.PREF_KEY, true) // no hint Toast over the injected gestures
            .commit()
    }

    @After
    fun tearDown() {
        mainScenario?.close()
        mainScenario = null
        prefs.edit().remove(ReadingPrefKeys.DISPLAY_MODE).remove(ReadingPrefKeys.MENU_SHOW).remove(LyricsChromeHint.PREF_KEY).commit()
    }

    private fun launch(): ActivityScenario<ContentHandler> {
        val extras = Bundle().apply {
            putString(MainActivity.ATTR_HYMN_TYPE, MainActivity.HYMN_DB)
            putInt(MainActivity.ATTR_HYMN_NUMBER, 5)
        }
        return ActivityScenario.launch(Intent(ctx, ContentHandler::class.java).putExtras(extras))
    }

    private fun <T> ActivityScenario<ContentHandler>.read(block: (ContentHandler) -> T): T {
        val ref = AtomicReference<T>()
        onActivity { ref.set(block(it)) }
        return ref.get()
    }

    private fun ActivityScenario<ContentHandler>.item() = read { it.findViewById<ViewPager2>(R.id.viewPager).currentItem }

    private fun ActivityScenario<ContentHandler>.pagerSize() = read {
        val p = it.findViewById<ViewPager2>(R.id.viewPager)
        intArrayOf(p.width, p.height)
    }

    /** Screen centre of the page's lyrics ScrollView (null until a page is resumed). */
    private fun ActivityScenario<ContentHandler>.scrollCentre(): IntArray? = read { a ->
        val page = a.supportFragmentManager.fragments.filterIsInstance<ContentView>().singleOrNull { it.isResumed }?.view
        val sv = page?.let { findScrollView(it) }
        if (sv == null || sv.width == 0) null else {
            val loc = IntArray(2)
            sv.getLocationOnScreen(loc)
            intArrayOf(loc[0] + sv.width / 2, loc[1] + sv.height / 2)
        }
    }

    private fun findScrollView(v: View): ScrollView? {
        if (v is ScrollView) return v
        if (v is android.view.ViewGroup) for (i in 0 until v.childCount) findScrollView(v.getChildAt(i))?.let { return it }
        return null
    }

    private fun ActivityScenario<ContentHandler>.scrollY() = read { a ->
        a.supportFragmentManager.fragments.filterIsInstance<ContentView>().singleOrNull { it.isResumed }?.view
            ?.let { findScrollView(it)?.scrollY } ?: 0
    }

    private fun ActivityScenario<ContentHandler>.awaitPage() {
        val end = SystemClock.uptimeMillis() + 15_000
        // Idle pager: while a page turn is still settling, the resumed fragment may be the outgoing one, off screen
        while (scrollCentre() == null || read { it.findViewById<ViewPager2>(R.id.viewPager).scrollState } != ViewPager2.SCROLL_STATE_IDLE) {
            check(SystemClock.uptimeMillis() < end) { "timed out waiting for the page" }
            instrumentation.waitForIdleSync()
            SystemClock.sleep(100)
        }
        SystemClock.sleep(800) // let the score images load so the page can scroll
    }

    /** Down at (x0,y0), 20 evenly spaced moves to (x0+dx, y0+dy), up. */
    private fun drag(x0: Int, y0: Int, dx: Int, dy: Int) =
        dragPath(x0, y0, (1..20).map { dx * it / 20 to dy * it / 20 })

    /** Down at (x0,y0), moves through the offsets in [path] (relative to the down point), up at the last one; synchronous. */
    private fun dragPath(x0: Int, y0: Int, path: List<Pair<Int, Int>>) {
        val t0 = SystemClock.uptimeMillis()
        fun send(action: Int, x: Int, y: Int) {
            val e = MotionEvent.obtain(t0, SystemClock.uptimeMillis(), action, (x0 + x).toFloat(), (y0 + y).toFloat(), 0)
            instrumentation.sendPointerSync(e)
            e.recycle()
        }
        send(MotionEvent.ACTION_DOWN, 0, 0)
        for ((x, y) in path) {
            SystemClock.sleep(10)
            send(MotionEvent.ACTION_MOVE, x, y)
        }
        send(MotionEvent.ACTION_UP, path.last().first, path.last().second)
        instrumentation.waitForIdleSync()
        SystemClock.sleep(900) // settle a page-turn animation, if one started
    }

    private fun diagonalScrollDoesNotTurnPage(sideways: Double, upwards: Boolean) {
        launch().use { s ->
            s.awaitPage()
            val (c, size) = s.scrollCentre()!! to s.pagerSize()
            val before = s.item()
            val dy = (size[1] * 0.45).toInt() * if (upwards) -1 else 1
            drag(c[0], c[1], (dy * sideways).toInt(), dy)
            assertThat(s.item()).isEqualTo(before)
            if (upwards) assertThat(s.scrollY()).isGreaterThan(0)
        }
    }

    @Test
    @QuickTest
    fun slightlyDiagonalUpwardDragOnlyScrolls() = diagonalScrollDoesNotTurnPage(0.3, true)

    @Test
    fun fortyFiveDegreeUpwardDragOnlyScrolls() = diagonalScrollDoesNotTurnPage(1.0, true)

    @Test
    fun fortyFiveDegreeDownwardDragDoesNotTurnPage() = diagonalScrollDoesNotTurnPage(-1.0, false)

    @Test
    fun steepDiagonalUpwardDragOnlyScrolls() = diagonalScrollDoesNotTurnPage(-0.8, true)

    @Test
    fun slightlyDiagonalDownwardDragDoesNotTurnPage() = diagonalScrollDoesNotTurnPage(0.3, false)

    /** A thumb often drifts sideways first, then settles into a vertical scroll: the drift alone must not turn the page. */
    @Test
    fun sidewaysDriftAtTheStartOfAVerticalScrollDoesNotTurnPage() {
        launch().use { s ->
            s.awaitPage()
            val c = s.scrollCentre()!!
            val slop = ViewConfiguration.get(ctx).scaledTouchSlop
            val before = s.item()
            val h = s.pagerSize()[1]
            val path = mutableListOf(0 to 0, (slop * 1.5).toInt() to (-slop * 0.8).toInt())
            for (i in 1..20) path += ((slop * 1.5 + slop * 0.3 * i / 20).toInt() to (-h * 0.4 * i / 20 - slop).toInt())
            dragPath(c[0], c[1], path)
            assertThat(s.item()).isEqualTo(before)
            assertThat(s.scrollY()).isGreaterThan(0)
        }
    }

    /** Waits until the pager has settled on [expected]: a page turn animates after the finger lifts, and slower under load. */
    private fun ActivityScenario<ContentHandler>.awaitItem(expected: Int) {
        val end = SystemClock.uptimeMillis() + 10_000
        while (item() != expected || read { it.findViewById<ViewPager2>(R.id.viewPager).scrollState } != ViewPager2.SCROLL_STATE_IDLE) {
            check(SystemClock.uptimeMillis() < end) { "pager did not settle on page $expected (at ${item()})" }
            instrumentation.waitForIdleSync()
            SystemClock.sleep(50)
        }
    }

    /**
     * The swipe covers 70% of the page width, so the page turns by distance alone. A swipe of exactly half the width
     * (the earlier version) sits on the snap threshold and depends on the fling velocity, which a loaded machine
     * delivers too slowly to count.
     */
    @Test
    @QuickTest
    fun clearHorizontalSwipeTurnsToNextAndPreviousPage() {
        launch().use { s ->
            s.awaitPage()
            val c = s.scrollCentre()!!
            val w = s.pagerSize()[0]
            val start = s.item()
            drag(c[0] + w * 35 / 100, c[1], -(w * 70 / 100), 0)
            s.awaitItem(start + 1)
            s.awaitPage()
            val c2 = s.scrollCentre()!!
            drag(c2[0] - w * 35 / 100, c2[1], w * 70 / 100, (w * 0.05).toInt())
            s.awaitItem(start)
        }
    }
}
