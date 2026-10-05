package org.cog.hymnchtv.ui.lyrics

import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.viewpager2.widget.ViewPager2
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.ContentHandler
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.MediaType
import org.cog.hymnchtv.R
import org.cog.hymnchtv.hymn.HymnRef
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Youth hymn 1 has its lyrics in the supplement book (bb876). Its bibletool lookup used to rewrite mHymnType/mHymnNo,
 * after which next, swipe, jump and the queued next used the wrong book. The hymn on screen must stay a youth hymn.
 */
@RunWith(AndroidJUnit4::class)
class YouthCrossRefViewingTest : LyricsTestBase() {
    val yb1 = HymnRef(MainActivity.HYMN_YB, 1)

    /** The 唱詩 lookup with downloads allowed: the youth branch that resolves the cross-reference (no network: it returns the link). */
    fun lookUpBibleTool(s: ActivityScenario<ContentHandler>): List<Uri> {
        val done = CountDownLatch(1)
        val result = AtomicReference<List<Uri>?>()
        s.onActivity { it.fetchPlayHymn(MediaType.HYMN_CHANGSHI, true) { list -> result.set(list); done.countDown() } }
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue()
        return checkNotNull(result.get()) { "the lookup was dropped" }
    }

    fun ActivityScenario<ContentHandler>.assertViewing(ref: HymnRef) {
        await("$ref on screen") { it.currentRef() == ref }
        read { a ->
            assertThat(a.mHymnType).isEqualTo(ref.book)
            assertThat(a.currentRef()).isEqualTo(ref)
        }
    }

    @Test
    fun theBibleToolLookupKeepsTheYouthHymnOnScreen() {
        assertThat(MainActivity.ybXTable[1]).isEqualTo("bb876") // the cross-reference this test relies on
        launch(MainActivity.HYMN_YB, 1).use { s ->
            val uris = lookUpBibleTool(s)
            assertThat(uris.single().toString()).contains("B876") // still the supplement book's bibletool page
            s.assertViewing(yb1)
        }
    }

    @Test
    fun nextAfterTheLookupStaysInTheYouthBook() {
        launch(MainActivity.HYMN_YB, 1).use { s ->
            lookUpBibleTool(s)
            s.onActivity { it.scrollNextHymn() }
            s.assertViewing(HymnRef(MainActivity.HYMN_YB, 2))
        }
    }

    @Test
    fun swipeAfterTheLookupStaysInTheYouthBook() {
        launch(MainActivity.HYMN_YB, 1).use { s ->
            lookUpBibleTool(s)
            s.onActivity { it.findViewById<ViewPager2>(R.id.viewPager).setCurrentItem(2, false) }
            s.assertViewing(HymnRef(MainActivity.HYMN_YB, 3))
        }
    }
}
