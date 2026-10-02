package org.cog.hymnchtv.ui.home

import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.chip.ChipGroup
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.R
import org.cog.hymnchtv.hymn.HymnRef
import org.cog.hymnchtv.hymnhistory.HistoryRecord
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.cog.hymnchtv.persistance.DatabaseBackend
import org.cog.hymnchtv.ui.FragmentHost
import org.cog.hymnchtv.ui.picker.HymnLabels
import org.cog.hymnchtv.ui.picker.PickerTestSupport
import org.cog.hymnchtv.ui.picker.PickerTestSupport.ctx
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.containsString
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The "recently opened" page and the recent chips on the home tab. */
@RunWith(AndroidJUnit4::class)
class HistoryFragmentTest {
    @Before fun setUp() = PickerTestSupport.prepare()

    @After fun tearDown() = PickerTestSupport.cleanUp()

    private fun records(n: Int) = (1..n).map { HistoryRecord(HymnTypes.DB, it, false, "標題$it", 1000L + it) }.toTypedArray()

    private fun openHistoryPage() {
        FragmentHost.eventually { onView(withId(R.id.btn_recent_more)).check(matches(isDisplayed())) }
        onView(withId(R.id.btn_recent_more)).perform(click())
        onView(withId(R.id.history_list)).check(matches(isDisplayed()))
    }

    private fun chipLabel(no: Int) = HymnLabels.chip(ctx, HymnRef(HymnTypes.DB, no))

    @Test fun chipsShowAtMostEightNewestFirst() {
        PickerTestSupport.resetHistory(*records(12))
        PickerTestSupport.launch { scenario ->
            FragmentHost.eventually {
                scenario.onActivity { a ->
                    val chips = a.findViewById<ChipGroup>(R.id.recent_chips)
                    assertThat(chips.childCount).isEqualTo(8)
                    assertThat((chips.getChildAt(0) as TextView).text.toString()).isEqualTo(chipLabel(12))
                }
            }
        }
    }

    @Test fun emptyHistoryHidesTheRecentRow() {
        PickerTestSupport.resetHistory()
        PickerTestSupport.launch { scenario ->
            scenario.onActivity { a -> assertThat(a.findViewById<View>(R.id.recentArea).visibility).isEqualTo(View.GONE) }
        }
    }

    @Test fun historyPageListsRecordsAndTheTrailingButtonDeletes() {
        PickerTestSupport.resetHistory(*records(2))
        PickerTestSupport.launch { scenario ->
            openHistoryPage()
            FragmentHost.eventually {
                scenario.onActivity { a -> assertThat(a.findViewById<RecyclerView>(R.id.history_list).adapter!!.itemCount).isEqualTo(2) }
            }
            onView(allOf(withId(R.id.b_delete_in_list), isDisplayed())).perform(click())
            FragmentHost.eventually { assertThat(DatabaseBackend.getInstance(ctx).historyRecords).hasSize(1) }
        }
    }

    @Test fun swipingARowDeletesIt() {
        PickerTestSupport.resetHistory(*records(2))
        PickerTestSupport.launch {
            openHistoryPage()
            FragmentHost.eventually { onView(withText(containsString("標題2"))).check(matches(isDisplayed())) }
            onView(withText(containsString("標題2"))).perform(swipeLeft())
            FragmentHost.eventually { assertThat(DatabaseBackend.getInstance(ctx).historyRecords.map { r -> r.hymnNo }).containsExactly(1) }
        }
    }

    @Test fun longPressOnARowAsksBeforeDeleting() {
        PickerTestSupport.resetHistory(*records(1))
        PickerTestSupport.launch {
            openHistoryPage()
            FragmentHost.eventually { onView(withText(containsString("標題1"))).check(matches(isDisplayed())) }
            onView(withText(containsString("標題1"))).perform(longClick())
            onView(withText(R.string.delete)).inRoot(isDialog()).check(matches(isDisplayed()))
            assertThat(DatabaseBackend.getInstance(ctx).historyRecords).hasSize(1)
            onView(withText(android.R.string.cancel)).inRoot(isDialog()).perform(click())
            assertThat(DatabaseBackend.getInstance(ctx).historyRecords).hasSize(1)
        }
    }

    @Test fun chipLongPressRemovesTheRecordAfterConfirmation() {
        PickerTestSupport.resetHistory(*records(1))
        PickerTestSupport.launch {
            FragmentHost.eventually { onView(withText(chipLabel(1))).check(matches(isDisplayed())) }
            onView(withText(chipLabel(1))).perform(longClick())
            onView(allOf(withText(R.string.delete), isAssignableFrom(Button::class.java))).inRoot(isDialog()).perform(click())
            FragmentHost.eventually { assertThat(DatabaseBackend.getInstance(ctx).historyRecords).isEmpty() }
        }
    }
}
