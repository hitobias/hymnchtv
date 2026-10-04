package org.cog.hymnchtv.ui.lyrics

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.ScrollView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.viewpager2.widget.ViewPager2
import org.cog.hymnchtv.ContentHandler
import org.cog.hymnchtv.ContentView
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.TestPermissions
import org.junit.After
import org.junit.Before
import java.util.concurrent.atomic.AtomicReference

/** Fake clock for the chrome timers; only touched on the main thread (inside onActivity). */
class ManualChromeTimer : ChromeTimer {
    private class Task(val at: Long, val action: Runnable)
    private var now = 0L
    private val tasks = mutableListOf<Task>()
    override fun postDelayed(delayMs: Long, action: Runnable): Any = Task(now + delayMs, action).also { tasks += it }
    override fun cancel(token: Any) { tasks.remove(token) }
    fun advance(ms: Long) {
        val end = now + ms
        while (true) {
            val next = tasks.filter { it.at <= end }.minByOrNull { it.at } ?: break
            tasks.remove(next); now = next.at; next.action.run()
        }
        now = end
    }
}

/** Shared by the lyrics page instrumentation tests: launch, read on the main thread, inject raw touches. */
abstract class LyricsTestBase {
    val ctx: Context = ApplicationProvider.getApplicationContext()
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    lateinit var chromeTimer: ManualChromeTimer
    private var mainScenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUpBase() {
        chromeTimer = ManualChromeTimer()
        // The one-time "tap the middle" hint Toast would take the injected touches: mark it as already shown
        ctx.getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE).edit().putBoolean(LyricsChromeHint.PREF_KEY, true).commit()
        ContentHandler.sChromeTimerForTest = chromeTimer
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            TestPermissions.grantLaunchPermission(ctx.packageName)
            mainScenario = ActivityScenario.launch(MainActivity::class.java)
        }
    }

    @After
    fun tearDownBase() {
        ContentHandler.sChromeTimerForTest = null
        mainScenario?.close()
        mainScenario = null
    }

    fun launch(type: String = MainActivity.HYMN_DB, number: Int = 5): ActivityScenario<ContentHandler> {
        val extras = Bundle().apply {
            putString(MainActivity.ATTR_HYMN_TYPE, type)
            putInt(MainActivity.ATTR_HYMN_NUMBER, number)
        }
        return ActivityScenario.launch<ContentHandler>(Intent(ctx, ContentHandler::class.java).putExtras(extras)).also { it.awaitPage() }
    }

    /** A cold open starts with the player as the capsule: tests that work on the card expand it explicitly. */
    fun launchExpanded(type: String = MainActivity.HYMN_DB, number: Int = 5): ActivityScenario<ContentHandler> = launch(type, number).also {
        it.await("the capsule") { a -> a.findViewById<View>(R.id.playerCapsule).visibility == View.VISIBLE && a.findViewById<View>(R.id.playerUi).visibility == View.GONE }
        it.onActivity { a -> a.playerSheet.expand(true) }
        it.await("the card") { a ->
            val card = a.findViewById<View>(R.id.playerUi)
            a.findViewById<View>(R.id.playerCapsule).visibility == View.GONE && card.visibility == View.VISIBLE && card.alpha == 1f && card.scaleX == 1f
        }
    }

    fun <T> ActivityScenario<ContentHandler>.read(block: (ContentHandler) -> T): T {
        val ref = AtomicReference<T>()
        onActivity { ref.set(block(it)) }
        return ref.get()
    }

    fun page(activity: ContentHandler): View? =
        activity.supportFragmentManager.fragments.filterIsInstance<ContentView>().singleOrNull { it.isResumed }?.view

    fun ActivityScenario<ContentHandler>.item() = read { it.findViewById<ViewPager2>(R.id.viewPager).currentItem }

    fun ActivityScenario<ContentHandler>.awaitPage() {
        val end = SystemClock.uptimeMillis() + 15_000
        while (read { page(it) } == null || read { it.findViewById<ViewPager2>(R.id.viewPager).scrollState } != ViewPager2.SCROLL_STATE_IDLE) {
            check(SystemClock.uptimeMillis() < end) { "timed out waiting for the lyrics page" }
            instrumentation.waitForIdleSync()
            SystemClock.sleep(50)
        }
    }

    fun ActivityScenario<ContentHandler>.await(what: String, timeoutMs: Long = 10_000, condition: (ContentHandler) -> Boolean) {
        val end = SystemClock.uptimeMillis() + timeoutMs
        while (!read(condition)) {
            check(SystemClock.uptimeMillis() < end) { "timed out waiting for $what" }
            instrumentation.waitForIdleSync()
            SystemClock.sleep(50)
        }
    }

    /** A view of the page on screen. */
    fun ActivityScenario<ContentHandler>.pageView(id: Int): View = read { page(it)!!.findViewById<View>(id) }

    fun ActivityScenario<ContentHandler>.topBarShown(): Boolean = read { page(it)!!.findViewById<View>(R.id.lyrics_top_bar).visibility == View.VISIBLE }

    fun ActivityScenario<ContentHandler>.buttonBarShown(): Boolean = read { page(it)!!.findViewById<View>(R.id.lyricsButtonBar).visibility == View.VISIBLE }

    /**
     * Hymns open with the toolbars hidden (lyrics only): reveal them with a centre tap, like a reader does, before a test uses a
     * toolbar button. The fake chrome timer never advances on its own, so they then stay until the test moves the clock.
     */
    fun ActivityScenario<ContentHandler>.revealChrome() {
        // A tap that lands while the page is still settling after the launch can be lost: try again, a shown bar is never hidden by a retry
        repeat(3) {
            if (topBarShown()) return
            val c = hostPoint(0.5f, 0.5f)
            tap(c[0], c[1])
            SystemClock.sleep(500)
        }
        await("toolbars shown") { page(it)!!.findViewById<View>(R.id.lyrics_top_bar).visibility == View.VISIBLE }
    }

    fun ActivityScenario<ContentHandler>.scroll(): ScrollView = read { page(it)!!.findViewById<ScrollView>(R.id.lyrics_scroll) }

    /** Screen position of a point in the lyrics host, as fractions of its width and height. */
    fun ActivityScenario<ContentHandler>.hostPoint(fx: Float, fy: Float): IntArray = read { a ->
        val host = page(a)!!.findViewById<View>(R.id.lyrics_scroll_host)
        val loc = IntArray(2)
        host.getLocationOnScreen(loc)
        intArrayOf(loc[0] + (host.width * fx).toInt(), loc[1] + (host.height * fy).toInt())
    }

    /** Moves the fake clock on the main thread. */
    fun ActivityScenario<ContentHandler>.advance(ms: Long) = onActivity { chromeTimer.advance(ms) }

    fun tap(x: Int, y: Int, holdMs: Long = 50) {
        val t0 = SystemClock.uptimeMillis()
        send(t0, MotionEvent.ACTION_DOWN, x, y)
        SystemClock.sleep(holdMs)
        send(t0, MotionEvent.ACTION_UP, x, y)
        instrumentation.waitForIdleSync()
        SystemClock.sleep(300) // the fade
    }

    fun drag(x0: Int, y0: Int, dx: Int, dy: Int) {
        val t0 = SystemClock.uptimeMillis()
        send(t0, MotionEvent.ACTION_DOWN, x0, y0)
        for (i in 1..20) {
            SystemClock.sleep(10)
            send(t0, MotionEvent.ACTION_MOVE, x0 + dx * i / 20, y0 + dy * i / 20)
        }
        send(t0, MotionEvent.ACTION_UP, x0 + dx, y0 + dy)
        instrumentation.waitForIdleSync()
        SystemClock.sleep(900)
    }

    private fun send(t0: Long, action: Int, x: Int, y: Int) {
        val e = MotionEvent.obtain(t0, SystemClock.uptimeMillis(), action, x.toFloat(), y.toFloat(), 0)
        instrumentation.sendPointerSync(e)
        e.recycle()
    }
}
