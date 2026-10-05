package org.cog.hymnchtv.ui.lyrics.jump

import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.viewpager2.widget.ViewPager2
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.QuickTest
import org.cog.hymnchtv.R
import org.cog.hymnchtv.hymn.HymnRef
import org.cog.hymnchtv.ui.picker.HymnLabels
import org.junit.Test
import org.junit.runner.RunWith

/** "Play next" slot: the next button shows it and goes there once; auto-next never reads it. */
@RunWith(AndroidJUnit4::class)
class NextSlotTest : JumpTestBase() {
    private fun nextDescription(): String = onTop { page(it)!!.findViewById<View>(R.id.btn_next).contentDescription.toString() }
    private fun slotDescription(ref: HymnRef) = ctx.getString(R.string.jump_next_slot, HymnLabels.spoken(ctx, ref, null))
    private fun topSlot(): HymnRef? = onTop { it.jumpState.slot }

    @Test
    fun aSlotShowsOnTheNextButtonOfEveryPage() {
        launch(MainActivity.HYMN_DB, 5).use {
            assertThat(nextDescription()).isEqualTo(ctx.getString(R.string.c_next_hymn))
            onTop { it.onSetNext(xg12) }
            assertThat(nextDescription()).isEqualTo(slotDescription(xg12))
            onTop { it.findViewById<ViewPager2>(R.id.viewPager).setCurrentItem(idx(db5) + 1, false) }
            awaitSettled(HymnRef(MainActivity.HYMN_DB, 6))
            assertThat(nextDescription()).isEqualTo(slotDescription(xg12))
        }
    }

    @Test
    @QuickTest
    fun theNextButtonGoesToTheSlotOnceAndRemembersWhereItCameFrom() {
        launch(MainActivity.HYMN_DB, 5).use { s ->
            onTop { it.onSetNext(xg12) }
            s.revealChrome()
            s.onActivity { it.setChromeHeld(true) }
            onView(withId(R.id.btn_next)).perform(click())
            awaitSettled(xg12)
            assertThat(topSlot()).isNull()
            assertThat(topStack()).containsExactly(db5)
            assertThat(nextDescription()).isEqualTo(ctx.getString(R.string.c_next_hymn))
            // with the slot used up, next is the next page again
            onTop { it.onLyricsAction(R.id.btn_next) }
            awaitSettled(HymnRef(MainActivity.HYMN_XG, 13))
        }
    }

    @Test
    fun clearingTheSlotRemovesTheBadge() {
        launch(MainActivity.HYMN_DB, 5).use {
            onTop { it.onSetNext(xg12) }
            onTop { it.onSetNext(null) }
            assertThat(topSlot()).isNull()
            assertThat(nextDescription()).isEqualTo(ctx.getString(R.string.c_next_hymn))
        }
    }

    @Test
    fun autoNextIgnoresTheSlot() {
        launch(MainActivity.HYMN_DB, 5).use {
            onTop { it.onSetNext(xg12) }
            assertThat(onTop { it.advanceForAutoNextForTest() }).isTrue()
            awaitSettled(HymnRef(MainActivity.HYMN_DB, 6))
            assertThat(topSlot()).isEqualTo(xg12)
            assertThat(topStack()).isEmpty()
        }
    }

    @Test
    fun recreateKeepsTheSlot() {
        launch(MainActivity.HYMN_DB, 5).use {
            onTop { it.onSetNext(xg12) }
            val before = onTop { it }
            onTop { it.recreate() }
            awaitTop("the recreated lyrics page", 15_000) { it !== before }
            awaitSettled(db5)
            assertThat(topSlot()).isEqualTo(xg12)
            assertThat(nextDescription()).isEqualTo(slotDescription(xg12))
        }
    }

    @Test
    fun aSlotEqualToTheHymnOnScreenIsClearedAndNextTurnsThePage() {
        launch(MainActivity.HYMN_DB, 5).use {
            onTop { it.onSetNext(db5) }
            onTop { it.onLyricsAction(R.id.btn_next) }
            awaitSettled(HymnRef(MainActivity.HYMN_DB, 6))
            assertThat(topSlot()).isNull()
            assertThat(topStack()).isEmpty()
        }
    }

    @Test
    fun theSlotClearsWhenTheReaderArrivesThereByAnyWay() {
        val db6 = HymnRef(MainActivity.HYMN_DB, 6)
        launch(MainActivity.HYMN_DB, 5).use {
            onTop { it.onSetNext(db6) }
            onTop { it.findViewById<ViewPager2>(R.id.viewPager).setCurrentItem(idx(db6), false) }
            awaitSettled(db6)
            assertThat(topSlot()).isNull()
            assertThat(nextDescription()).isEqualTo(ctx.getString(R.string.c_next_hymn))
        }
    }
}
