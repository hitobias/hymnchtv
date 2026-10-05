package org.cog.hymnchtv.ui.lyrics.jump

import android.os.SystemClock
import android.widget.ScrollView
import androidx.lifecycle.Lifecycle
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.viewpager2.widget.ViewPager2
import org.cog.hymnchtv.ContentHandler
import org.cog.hymnchtv.ContentView
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.hymn.HymnRef
import org.cog.hymnchtv.ui.lyrics.LyricsTestBase
import org.cog.hymnchtv.utils.HymnNo2IdxConvert
import org.junit.After
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicReference

/**
 * Jump tests act on the lyrics page in front, not on the scenario's activity: on the fallback path (plan Task 5B) a
 * cross-book jump opens a new lyrics page, and the same tests must hold there.
 */
abstract class JumpTestBase : LyricsTestBase() {
    val db5 = HymnRef(MainActivity.HYMN_DB, 5)
    val db100 = HymnRef(MainActivity.HYMN_DB, 100)
    val bb37 = HymnRef(MainActivity.HYMN_BB, 37)
    val xg12 = HymnRef(MainActivity.HYMN_XG, 12)

    /** A page opened by a jump (path B) is not the scenario's: close every lyrics page so the next test starts clean. */
    @After
    fun finishEveryLyricsPage() {
        instrumentation.runOnMainSync {
            val monitor = ActivityLifecycleMonitorRegistry.getInstance()
            listOf(Stage.CREATED, Stage.STARTED, Stage.RESUMED, Stage.PAUSED, Stage.STOPPED)
                .flatMap { monitor.getActivitiesInStage(it) }
                .filterIsInstance<ContentHandler>().forEach { it.finish() }
        }
    }

    private fun topOrNull(): ContentHandler? = ActivityLifecycleMonitorRegistry.getInstance()
        .getActivitiesInStage(Stage.RESUMED).filterIsInstance<ContentHandler>().singleOrNull()

    fun awaitTop(what: String, timeoutMs: Long = 10_000, condition: (ContentHandler) -> Boolean) {
        val end = SystemClock.uptimeMillis() + timeoutMs
        while (true) {
            var ok = false
            instrumentation.runOnMainSync { ok = topOrNull()?.let(condition) ?: false }
            if (ok) return
            check(SystemClock.uptimeMillis() < end) { "timed out waiting for $what" }
            instrumentation.waitForIdleSync()
            SystemClock.sleep(50)
        }
    }

    /** Runs [block] on the main thread with the resumed lyrics page. */
    fun <T> onTop(block: (ContentHandler) -> T): T {
        awaitTop("a resumed lyrics page") { true }
        val ref = AtomicReference<T>()
        instrumentation.runOnMainSync { ref.set(block(checkNotNull(topOrNull()))) }
        return ref.get()
    }

    fun topRef(): HymnRef = onTop { it.currentRef() }
    fun topStack(): List<HymnRef> = onTop { a -> a.jumpState.recentFirst().map { it.ref } }
    fun topItem(): Int = onTop { it.findViewById<ViewPager2>(R.id.viewPager).currentItem }

    /** The book of every added lyrics page fragment of the page in front. */
    fun pageBooks(): List<String?> = onTop { a ->
        a.supportFragmentManager.fragments.filterIsInstance<ContentView>().filter { it.isAdded }
            .map { it.arguments?.getString(ContentView.LYRICS_TYPE) }
    }

    /** Waits until [ref] is on screen, its page resumed and the pager at rest. */
    fun awaitSettled(ref: HymnRef) = awaitTop("$ref on screen", 15_000) { a ->
        a.currentRef() == ref && page(a) != null &&
            a.findViewById<ViewPager2>(R.id.viewPager).scrollState == ViewPager2.SCROLL_STATE_IDLE
    }

    fun jump(to: HymnRef) {
        onTop { it.onJump(to) }
        awaitSettled(to)
    }

    fun back() {
        onTop { it.onBackPressedDispatcher.onBackPressed() }
        instrumentation.waitForIdleSync()
    }

    fun topScrollY(): Int = onTop { page(it)!!.findViewById<ScrollView>(R.id.lyrics_scroll).scrollY }

    /** Scrolls the page in front to [wanted] (or as far as it goes) once its lyrics are laid out; returns where it ended. */
    fun scrollTopTo(wanted: Int): Int {
        awaitTop("the lyrics laid out") { a -> (page(a)!!.findViewById<ScrollView>(R.id.lyrics_scroll).getChildAt(0)?.height ?: 0) > 0 }
        SystemClock.sleep(300)
        return onTop { a ->
            val sv = page(a)!!.findViewById<ScrollView>(R.id.lyrics_scroll)
            sv.scrollTo(0, wanted)
            sv.scrollY
        }
    }

    fun idx(ref: HymnRef): Int = HymnNo2IdxConvert.hymnNo2IdxConvert(ref.book, ref.storedNo)

    /** Lyrics page activities not yet destroyed (path B closes the old one asynchronously). Main thread only. */
    fun liveLyricsPagesOnMain(): Int {
        val monitor = ActivityLifecycleMonitorRegistry.getInstance()
        return listOf(Stage.PRE_ON_CREATE, Stage.CREATED, Stage.STARTED, Stage.RESUMED, Stage.PAUSED, Stage.STOPPED, Stage.RESTARTED)
            .flatMap { monitor.getActivitiesInStage(it) }.count { it is ContentHandler }
    }

    /** Weak references to every lyrics page fragment of the page in front (to check they are destroyed later). */
    fun pageRefs(): List<WeakReference<ContentView>> = onTop { a ->
        a.supportFragmentManager.fragments.filterIsInstance<ContentView>().map { WeakReference(it) }
    }

    /**
     * Waits until every fragment in [refs] is gone or DESTROYED (pending transactions run first). A page that was queued
     * by the adapter swap but removed before it ever started (INITIALIZED, not added) holds nothing and counts as gone.
     */
    fun awaitDestroyed(refs: List<WeakReference<ContentView>>) {
        onTop { it.supportFragmentManager.executePendingTransactions() }
        instrumentation.waitForIdleSync()
        awaitTop("the other book's pages to be destroyed", 10_000) {
            refs.all { ref -> ref.get()?.let { f -> !f.isAdded && f.lifecycle.currentState.let { st -> st == Lifecycle.State.DESTROYED || st == Lifecycle.State.INITIALIZED } } ?: true }
        }
    }
}
