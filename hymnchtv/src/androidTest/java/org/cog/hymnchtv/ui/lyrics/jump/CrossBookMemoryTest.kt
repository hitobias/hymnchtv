package org.cog.hymnchtv.ui.lyrics.jump

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.nav.JumpState
import org.junit.Test
import org.junit.runner.RunWith

/** Spike acceptance S6 (plan Task 5): 20 cross-book jumps destroy every old page and leave one lyrics page (api24b: 32 MB). */
@RunWith(AndroidJUnit4::class)
class CrossBookMemoryTest : JumpTestBase() {

    @Test
    fun twentyCrossBookJumpsLeaveNoOldPagesBehind() {
        launch(MainActivity.HYMN_DB, 5).use {
            jump(bb37)
            val pagesAfterFirst = pageBooks().size
            val oldPages = mutableListOf<java.lang.ref.WeakReference<org.cog.hymnchtv.ContentView>>()
            repeat(20) { i ->
                val target = if (i % 2 == 0) db5 else bb37
                val before = pageRefs()
                oldPages += before
                jump(target)
                awaitDestroyed(before)
                assertThat(pageBooks().toSet()).containsExactly(target.book)
                assertThat(pageBooks().size).isAtMost(pagesAfterFirst + 1)
                awaitTop("a single lyrics page") { liveLyricsPagesOnMain() == 1 }
            }
            assertThat(topStack()).hasSize(JumpState.MAX_STACK)
            // nothing holds the old pages any more: they are collected (a few bounded attempts, a GC is only a request)
            var uncollected = oldPages.size
            repeat(10) {
                if (uncollected == 0) return@repeat
                Runtime.getRuntime().gc()
                System.runFinalization()
                android.os.SystemClock.sleep(100)
                uncollected = oldPages.count { it.get() != null }
            }
            assertThat(uncollected).isEqualTo(0)
        }
    }
}
