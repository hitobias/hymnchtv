package org.cog.hymnchtv.ui.host

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.hymnhistory.HistoryRecord
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.cog.hymnchtv.ui.FragmentHost
import org.cog.hymnchtv.ui.picker.PickerTestSupport
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Pages shown over the home page (recent history): the back key and opening a full page close them. */
@RunWith(AndroidJUnit4::class)
class OverlayNavigationTest {
    @Before fun setUp() {
        PickerTestSupport.prepare()
        PickerTestSupport.resetHistory(HistoryRecord(HymnTypes.DB, 1, false))
    }

    @After fun tearDown() = PickerTestSupport.cleanUp()

    private fun openHistory() {
        FragmentHost.eventually { onView(withId(R.id.btn_recent_more)).check(matches(withEffectiveVisibility(androidx.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE))) }
        onView(withId(R.id.btn_recent_more)).perform(androidx.test.espresso.action.ViewActions.scrollTo(), click())
        onView(withId(R.id.history_list)).check(matches(isDisplayed()))
    }

    @Test fun backClosesTheOverlayAndReturnsToHome() = PickerTestSupport.launch { scenario ->
        openHistory()
        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        onView(withId(R.id.history_list)).check(doesNotExist())
        onView(withId(R.id.tv_entry)).check(matches(isDisplayed()))
    }

    @Test fun openingAFullPageClosesTheOverlayFirst() = PickerTestSupport.launch { scenario ->
        openHistory()
        onView(withId(R.id.btn_home_settings)).perform(click())
        onView(withId(R.id.history_list)).check(doesNotExist())
        onView(withText(R.string.c_cat_appearance)).check(matches(isDisplayed()))
        // One back press leaves the page and lands on the bare home page, not on the closed overlay
        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        onView(withId(R.id.tv_entry)).check(matches(isDisplayed()))
        scenario.onActivity { a -> assertThat(a.supportFragmentManager.backStackEntryCount).isEqualTo(0) }
    }

    @Test fun backOrderIsOverlayThenFullPageThenHome() = PickerTestSupport.launch { scenario ->
        onView(withId(R.id.btn_home_toc)).perform(click())
        onView(withId(R.id.toc_books)).check(matches(isDisplayed()))
        scenario.onActivity { a -> assertThat(a.supportFragmentManager.backStackEntryCount).isEqualTo(1) }
        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        onView(withId(R.id.tv_entry)).check(matches(isDisplayed()))
        openHistory()
        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        onView(withId(R.id.history_list)).check(doesNotExist())
        onView(withId(R.id.tv_entry)).check(matches(isDisplayed()))
    }

    @Test fun overlayIsStillThereAfterRotation() = PickerTestSupport.launch { scenario ->
        openHistory()
        scenario.recreate()
        onView(withId(R.id.history_list)).check(matches(isDisplayed()))
        scenario.onActivity { a ->
            assertThat(a.supportFragmentManager.backStackEntryCount).isEqualTo(1)
            assertThat(a.supportActionBar?.title?.toString()).isEqualTo(a.getString(R.string.c_history_title))
        }
    }

    @Test fun titleReturnsToTheAppTitleWhenTheOverlayCloses() = PickerTestSupport.launch { scenario ->
        openHistory()
        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        FragmentHost.eventually {
            scenario.onActivity { a -> assertThat(a.supportActionBar?.title?.toString()).isEqualTo(a.getString(R.string.app_title_main)) }
        }
    }

    @Test fun contentBehindTheOverlayIsHiddenFromAccessibilityAndFocus() = PickerTestSupport.launch { scenario ->
        fun check(open: Boolean) = scenario.onActivity { a ->
            for (id in intArrayOf(R.id.fragment_container)) {
                val group = a.findViewById<android.view.ViewGroup>(id)
                assertThat(group.importantForAccessibility == android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS).isEqualTo(open)
                assertThat(group.descendantFocusability == android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS).isEqualTo(open)
            }
        }
        check(open = false)
        openHistory()
        check(open = true)
        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        FragmentHost.eventually { check(open = false) }
    }

    @Test fun showingTheSameOverlayTwiceAddsItOnce() = PickerTestSupport.launch { scenario ->
        openHistory()
        scenario.onActivity { a ->
            (a as MainActivity).openHistory()
            a.supportFragmentManager.executePendingTransactions()
            assertThat(a.supportFragmentManager.backStackEntryCount).isEqualTo(1)
        }
    }

    @Suppress("unused")
    private val keep = MainActivity::class
}
