package org.cog.hymnchtv.ui.lyrics.jump

import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.ScrollView
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.viewpager2.widget.ViewPager2
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.QuickTest
import org.cog.hymnchtv.R
import org.cog.hymnchtv.nav.ReadingPosition
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

/** The back key walks the return stack and restores where the reader was; page turns and Home do not touch it. */
@RunWith(AndroidJUnit4::class)
class JumpNavigationTest : JumpTestBase() {

    private fun awaitScrollY(what: String, expected: Int) = awaitTop(what, 5_000) { a ->
        abs(page(a)!!.findViewById<ScrollView>(R.id.lyrics_scroll).scrollY - expected) <= 2
    }

    @Test
    @QuickTest
    fun backReturnsStepByStepAndRestoresTheReadingPositions() {
        launch(MainActivity.HYMN_DB, 5).use {
            val y5 = scrollTopTo(600)
            assertWithMessage("DB 5 must scroll on a 320x640 screen").that(y5).isGreaterThan(200)
            jump(db100)
            val y100 = scrollTopTo(300)
            assertWithMessage("DB 100 must scroll on a 320x640 screen").that(y100).isGreaterThan(100)
            jump(bb37)
            assertThat(topStack()).containsExactly(db100, db5).inOrder()

            back()
            awaitSettled(db100) // across books
            awaitScrollY("DB 100 scrolled back", y100)
            back()
            awaitSettled(db5) // within the book
            awaitScrollY("DB 5 scrolled back", y5)
            assertThat(topStack()).isEmpty()
        }
    }

    @Test
    fun backWithAnEmptyStackLeavesTheLyricsPageAsBefore() {
        launch(MainActivity.HYMN_DB, 5).use { s ->
            back()
            val end = SystemClock.uptimeMillis() + 5_000
            while (s.state != Lifecycle.State.DESTROYED) {
                check(SystemClock.uptimeMillis() < end) { "lyrics page did not finish" }
                SystemClock.sleep(50)
            }
        }
    }

    @Test
    fun pageTurnsAndTheNextButtonDoNotAddToTheStack() {
        launch(MainActivity.HYMN_DB, 5).use {
            onTop { it.findViewById<ViewPager2>(R.id.viewPager).setCurrentItem(idx(db5) + 1, false) }
            onTop { it.scrollNextHymn() }
            SystemClock.sleep(500)
            assertThat(topStack()).isEmpty()
        }
    }

    @Test
    fun theHomeKeyDoesNotPopTheStack() {
        launch(MainActivity.HYMN_DB, 5).use {
            jump(db100)
            onTop { instrumentation.callActivityOnUserLeaving(it) }
            SystemClock.sleep(500)
            assertThat(topRef()).isEqualTo(db100)
            assertThat(topStack()).containsExactly(db5)
            assertThat(onTop { it.isFinishing }).isFalse()
        }
    }

    @Test
    fun aRecreationRightAfterAReturnStillRestoresThePosition() {
        launch(MainActivity.HYMN_DB, 5).use {
            val y5 = scrollTopTo(600)
            assertWithMessage("DB 5 must scroll on a 320x640 screen").that(y5).isGreaterThan(200)
            jump(db100)
            val before = onTop { it }
            val saved = Bundle()
            // the return, a state save and the recreation in one main-thread turn: the page has not taken the position yet
            onTop { a ->
                a.onBackPressedDispatcher.onBackPressed()
                instrumentation.callActivityOnSaveInstanceState(a, saved)
                a.recreate()
            }
            // deterministic part: the pending position is in the saved state
            assertThat(ReadingPosition.decode(saved.getString("state_pending_position"))?.scrollY).isEqualTo(y5)
            awaitTop("the recreated lyrics page", 15_000) { it !== before }
            awaitSettled(db5)
            awaitScrollY("DB 5 scrolled back after the recreation", y5)
        }
    }

    @Test
    fun theReadersOwnScrollEndsTheRestore() {
        launch(MainActivity.HYMN_DB, 5).use {
            val y5 = scrollTopTo(600)
            assertWithMessage("DB 5 must scroll on a 320x640 screen").that(y5).isGreaterThan(200)
            jump(db100)
            back()
            awaitSettled(db5)
            awaitScrollY("DB 5 scrolled back", y5)
            // within the restore window: the reader scrolls, then the page lays out again (e.g. an image arrives)
            onTop { a ->
                page(a)!!.findViewById<ScrollView>(R.id.lyrics_scroll).scrollTo(0, 0)
                page(a)!!.findViewById<View>(R.id.lyricsView).requestLayout()
            }
            SystemClock.sleep(600)
            assertThat(topScrollY()).isEqualTo(0)
        }
    }

    @Test
    fun recreateKeepsTheStackAndBackStillWorks() {
        launch(MainActivity.HYMN_DB, 5).use {
            jump(db100)
            jump(bb37)
            val before = onTop { it }
            onTop { it.recreate() }
            awaitTop("the recreated lyrics page", 15_000) { it !== before }
            awaitSettled(bb37)
            back()
            awaitSettled(db100)
            back()
            awaitSettled(db5)
        }
    }
}
