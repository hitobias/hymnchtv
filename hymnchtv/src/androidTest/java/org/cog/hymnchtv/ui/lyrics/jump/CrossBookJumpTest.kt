package org.cog.hymnchtv.ui.lyrics.jump

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.viewpager2.widget.ViewPager2
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.ContentHandler
import org.cog.hymnchtv.ContentView
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.QuickTest
import org.cog.hymnchtv.R
import org.cog.hymnchtv.hymn.HymnRef
import org.cog.hymnchtv.nav.ViewingCause
import org.cog.hymnchtv.nav.ViewingObserver
import org.cog.hymnchtv.persistance.DatabaseBackend
import org.cog.hymnchtv.ui.FragmentHost
import org.cog.hymnchtv.utils.HymnIdx2NoConvert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Collections

/** Spike acceptance S1-S5 and S8 (plan Task 5): jumps inside a book and to another book show the right hymn and keep their state. */
@RunWith(AndroidJUnit4::class)
class CrossBookJumpTest : JumpTestBase() {

    private fun assertNewestHistory(ref: HymnRef) = FragmentHost.eventually(5_000) {
        val rec = DatabaseBackend.getInstance(ctx).historyRecords.first()
        assertThat(rec.hymnType).isEqualTo(ref.book)
        assertThat(rec.hymnNo).isEqualTo(ref.storedNo)
    }

    @Test
    @QuickTest
    fun aJumpInTheSameBookTurnsToThatPageAndRemembersWhereItCameFrom() {
        launch(MainActivity.HYMN_DB, 5).use {
            jump(db100)
            assertThat(topItem()).isEqualTo(idx(db100))
            assertThat(onTop { it.currentRef().storedNo }).isEqualTo(100)
            assertThat(topStack()).containsExactly(db5)
            assertNewestHistory(db100)
        }
    }

    @Test
    fun aJumpToAnotherBookShowsThatHymnAndRecordsHistory() {
        launch(MainActivity.HYMN_DB, 5).use {
            jump(bb37)
            val args = onTop { a ->
                a.supportFragmentManager.fragments.filterIsInstance<ContentView>().single { it.isResumed }.requireArguments()
            }
            assertThat(args.getString(ContentView.LYRICS_TYPE)).isEqualTo(MainActivity.HYMN_BB)
            assertThat(args.getInt(ContentView.LYRICS_INDEX)).isEqualTo(idx(bb37))
            assertThat(onTop { it.currentRef().storedNo }).isEqualTo(37)
            assertThat(topStack()).containsExactly(db5)
            assertNewestHistory(bb37)
        }
    }

    @Test
    fun afterACrossBookJumpNextAndSwipeStayInTheNewBook() {
        launch(MainActivity.HYMN_DB, 5).use {
            jump(bb37)
            onTop { it.scrollNextHymn() }
            val next = HymnRef(MainActivity.HYMN_BB, HymnIdx2NoConvert.hymnIdx2NoConvert(MainActivity.HYMN_BB, idx(bb37) + 1)[0])
            awaitSettled(next)
            onTop { it.findViewById<ViewPager2>(R.id.viewPager).setCurrentItem(idx(bb37), false) }
            awaitSettled(bb37)
            // turning pages is not a jump
            assertThat(topStack()).containsExactly(db5)
        }
    }

    @Test
    fun recreateAfterACrossBookJumpKeepsBookPageAndStack() {
        launch(MainActivity.HYMN_DB, 5).use {
            jump(bb37)
            val before = onTop { it }
            onTop { it.recreate() }
            awaitTop("the recreated lyrics page", 15_000) { it !== before }
            awaitSettled(bb37)
            assertThat(topItem()).isEqualTo(idx(bb37))
            assertThat(topStack()).containsExactly(db5)
        }
    }

    @Test
    fun theOldBooksPagesAreDestroyedAfterASwap() {
        launch(MainActivity.HYMN_DB, 5).use {
            val oldPages = pageRefs()
            assertThat(oldPages).isNotEmpty()
            jump(bb37)
            awaitDestroyed(oldPages)
            assertThat(pageBooks().toSet()).containsExactly(MainActivity.HYMN_BB)
        }
    }

    @Test
    fun aCrossBookJumpReportsTheTargetOnceThroughTheViewingEntryPoint() {
        val events = Collections.synchronizedList(mutableListOf<Pair<HymnRef, ViewingCause>>())
        ContentHandler.sViewingObserverForTest = ViewingObserver { ref, cause -> events += ref to cause }
        try {
            launch(MainActivity.HYMN_DB, 5).use {
                jump(bb37)
                SystemClock.sleep(500)
                val forTarget = events.toList().filter { it.first == bb37 }
                assertThat(forTarget).hasSize(1)
                // path A: a change in the same page; path B: the new page's first hymn
                assertThat(forTarget.single().second).isAnyOf(ViewingCause.CHANGED, ViewingCause.OPENED)
            }
        } finally {
            ContentHandler.sViewingObserverForTest = null
        }
    }
}
