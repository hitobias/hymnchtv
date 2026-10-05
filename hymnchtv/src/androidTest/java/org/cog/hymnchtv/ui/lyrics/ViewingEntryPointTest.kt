package org.cog.hymnchtv.ui.lyrics

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.viewpager2.widget.ViewPager2
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.ContentHandler
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.hymn.HymnRef
import org.cog.hymnchtv.nav.ViewingCause
import org.cog.hymnchtv.nav.ViewingObserver
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Collections

/** The single "hymn on screen" entry point: once per cold open, once per change, RESTORED after a recreation. */
@RunWith(AndroidJUnit4::class)
class ViewingEntryPointTest : LyricsTestBase() {
    private val events = Collections.synchronizedList(mutableListOf<Pair<HymnRef, ViewingCause>>())
    private val db5 = HymnRef(MainActivity.HYMN_DB, 5)
    private val db6 = HymnRef(MainActivity.HYMN_DB, 6)

    @Before
    fun observe() {
        ContentHandler.sViewingObserverForTest = ViewingObserver { ref, cause -> events += ref to cause }
    }

    @After
    fun stopObserving() {
        ContentHandler.sViewingObserverForTest = null
    }

    @Test
    fun aColdOpenReportsTheFirstHymnOnceAsOpened() {
        launch(MainActivity.HYMN_DB, 5).use {
            assertThat(events.toList()).containsExactly(db5 to ViewingCause.OPENED)
        }
    }

    @Test
    fun aPageTurnReportsChangedAndARecreationReportsRestored() {
        launch(MainActivity.HYMN_DB, 5).use { s ->
            s.onActivity { it.findViewById<ViewPager2>(R.id.viewPager).setCurrentItem(5, false) }
            s.await("DB 6") { it.currentRef() == db6 }
            s.recreate()
            s.awaitPage()
            assertThat(events.toList()).containsExactly(
                db5 to ViewingCause.OPENED, db6 to ViewingCause.CHANGED, db6 to ViewingCause.RESTORED,
            ).inOrder()
        }
    }
}
