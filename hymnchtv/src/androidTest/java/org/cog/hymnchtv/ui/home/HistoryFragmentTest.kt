package org.cog.hymnchtv.ui.home

import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
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
        FragmentHost.eventually { onView(withId(R.id.btn_recent_more)).check(matches(withEffectiveVisibility(androidx.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE))) }
        onView(withId(R.id.btn_recent_more)).perform(scrollTo(), click())
        onView(withId(R.id.history_list)).check(matches(isDisplayed()))
    }

    private fun chipLabel(no: Int) = HymnLabels.chip(ctx, HymnRef(HymnTypes.DB, no))

    @Test fun recentRowsShowAtMostFiveNewestFirst() {
        PickerTestSupport.resetHistory(*records(12))
        PickerTestSupport.launch { scenario ->
            FragmentHost.eventually {
                scenario.onActivity { a ->
                    val chips = a.findViewById<android.widget.LinearLayout>(R.id.recent_chips)
                    // The page lists what fits (0 to 5); in the scrolling exception (small screens) all five show
                    val scroller = chips.rootView.findViewById<android.view.View>(R.id.home_content).parent as org.cog.hymnchtv.ui.home.HomeScrollView
                    assertThat(chips.childCount).isAtMost(5)
                    if (scroller.scrollingAllowed) assertThat(chips.childCount).isEqualTo(5)
                    assertThat(chips.childCount).isGreaterThan(0)
                    assertThat(chips.getChildAt(0).findViewById<TextView>(R.id.tv_recent_label_item).text.toString()).isEqualTo(chipLabel(12))
                }
            }
        }
    }

    @Test fun emptyHistoryShowsTheEmptyStateAndKeepsTheAllHistoryEntry() {
        PickerTestSupport.resetHistory()
        PickerTestSupport.launch { scenario ->
            FragmentHost.eventually {
                scenario.onActivity { a ->
                    assertThat(a.findViewById<View>(R.id.recentArea).visibility).isEqualTo(View.VISIBLE)
                    assertThat(a.findViewById<android.widget.LinearLayout>(R.id.recent_chips).childCount).isEqualTo(0)
                    assertThat(a.findViewById<View>(R.id.recent_empty).visibility).isEqualTo(View.VISIBLE)
                    assertThat(a.findViewById<TextView>(R.id.recent_empty_text).text.toString()).isEqualTo(a.getString(R.string.c_recent_empty))
                    assertThat(a.findViewById<View>(R.id.btn_recent_more).visibility).isEqualTo(View.VISIBLE)
                }
            }
        }
    }

    @Test fun emptyStateGoesAwayWhenAHymnIsInTheHistory() {
        PickerTestSupport.resetHistory(*records(1))
        PickerTestSupport.launch { scenario ->
            FragmentHost.eventually {
                scenario.onActivity { a -> assertThat(a.findViewById<View>(R.id.recent_empty).visibility).isEqualTo(View.GONE) }
            }
        }
    }

    @Test fun historyPageListsRecordsAndTheTrailingButtonDeletes() {
        PickerTestSupport.resetHistory(*records(2))
        PickerTestSupport.launch { scenario ->
            openHistoryPage()
            FragmentHost.eventually {
                scenario.onActivity { a -> assertThat((a.findViewById<RecyclerView>(R.id.history_list).adapter as HistoryAdapter).currentList.filterIsInstance<HistoryItem.Row>()).hasSize(2) }
            }
            onView(allOf(withId(R.id.b_delete_in_list), androidx.test.espresso.matcher.ViewMatchers.hasSibling(withText(containsString("標題2"))))).perform(click())
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
            onView(allOf(withId(androidx.appcompat.R.id.alertTitle), withText(R.string.delete))).inRoot(isDialog()).check(matches(isDisplayed()))
            assertThat(DatabaseBackend.getInstance(ctx).historyRecords).hasSize(1)
            onView(withText(android.R.string.cancel)).inRoot(isDialog()).perform(click())
            assertThat(DatabaseBackend.getInstance(ctx).historyRecords).hasSize(1)
        }
    }

    @Test fun chipLongPressRemovesTheRecordAfterConfirmation() {
        PickerTestSupport.resetHistory(*records(1))
        PickerTestSupport.launch {
            FragmentHost.eventually { onView(withText(chipLabel(1))).check(matches(withEffectiveVisibility(androidx.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE))) }
            onView(withText(chipLabel(1))).perform(scrollTo(), longClick())
            onView(allOf(withText(R.string.delete), isAssignableFrom(Button::class.java))).inRoot(isDialog()).perform(click())
            FragmentHost.eventually { assertThat(DatabaseBackend.getInstance(ctx).historyRecords).isEmpty() }
        }
    }

    private fun at(daysAgo: Int, hour: Int, minute: Int): Long = java.util.Calendar.getInstance().apply {
        add(java.util.Calendar.DAY_OF_YEAR, -daysAgo)
        set(java.util.Calendar.HOUR_OF_DAY, hour); set(java.util.Calendar.MINUTE, minute); set(java.util.Calendar.SECOND, 0)
    }.timeInMillis

    private fun dated(no: Int, whenMillis: Long) = HistoryRecord(HymnTypes.DB, no, false, "標題$no", whenMillis)

    @Test fun chipsShowTimeYesterdayWeekdayAndDate() {
        val now = System.currentTimeMillis()
        // records are stored with their own times; the list is newest first
        val today = java.util.Calendar.getInstance().apply { timeInMillis = now; set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 1) }.timeInMillis
        PickerTestSupport.resetHistory(dated(1, today), dated(2, at(1, 12, 0)), dated(3, at(3, 12, 0)), dated(4, at(30, 12, 0)))
        PickerTestSupport.launch { scenario ->
            FragmentHost.eventually {
                scenario.onActivity { a ->
                    val chips = a.findViewById<android.widget.LinearLayout>(R.id.recent_chips)
                    fun whenOf(no: Int): String {
                        val label = chipLabel(no)
                        val item = (0 until chips.childCount).map { chips.getChildAt(it) }
                            .first { it.findViewById<TextView>(R.id.tv_recent_label_item).text == label }
                        return item.findViewById<TextView>(R.id.tv_recent_when).text.toString()
                    }
                    assertThat(whenOf(1)).isEqualTo(org.cog.hymnchtv.ui.home.HistoryTimeText.clock(a, today))
                    assertThat(whenOf(2)).isEqualTo(a.getString(R.string.c_date_yesterday))
                    assertThat(whenOf(3)).isEqualTo(android.text.format.DateUtils.formatDateTime(a, at(3, 12, 0), android.text.format.DateUtils.FORMAT_SHOW_WEEKDAY or android.text.format.DateUtils.FORMAT_ABBREV_WEEKDAY))
                    assertThat(whenOf(4)).isNotEmpty()
                    assertThat(whenOf(4)).doesNotContain(":")
                    val first = chips.getChildAt(0)
                    assertThat(first.contentDescription.toString()).contains(HymnLabels.headline(a, HymnRef(HymnTypes.DB, 1)).substringBefore(' '))
                }
            }
        }
    }

    @Test fun historyPageGroupsByDayWithHeadingsAndShowsTimes() {
        val todayTime = at(0, 0, 1)
        PickerTestSupport.resetHistory(dated(1, maxOf(todayTime, System.currentTimeMillis() - 1000)), dated(2, at(1, 12, 0)), dated(3, at(1, 9, 5)))
        PickerTestSupport.launch { scenario ->
            openHistoryPage()
            FragmentHost.eventually {
                scenario.onActivity { a ->
                    val items = (a.findViewById<RecyclerView>(R.id.history_list).adapter as HistoryAdapter).currentList
                    val headers = items.filterIsInstance<HistoryItem.Header>().map { it.text }
                    assertThat(headers).containsExactly(a.getString(R.string.c_date_today), a.getString(R.string.c_date_yesterday)).inOrder()
                    val rows = items.filterIsInstance<HistoryItem.Row>()
                    assertThat(rows.map { it.record.hymnNo }).containsExactly(1, 2, 3).inOrder()
                    assertThat(rows[1].time).isEqualTo(HistoryTimeText.clock(a, at(1, 12, 0)))
                }
            }
            onView(allOf(withId(R.id.tv_history_header), withText(R.string.c_date_yesterday))).check(matches(isDisplayed()))
        }
    }
}
