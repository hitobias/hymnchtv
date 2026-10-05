package org.cog.hymnchtv.ui.host

import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.R
import org.cog.hymnchtv.hymnhistory.HistoryRecord
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.cog.hymnchtv.ui.FragmentHost
import org.cog.hymnchtv.ui.picker.PickerTestSupport
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** 1.6.0: quick repeated taps open one overlay, never a stack of them. */
@RunWith(AndroidJUnit4::class)
class OverlayGuardTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Before
    fun setUp() {
        PickerTestSupport.prepare()
        PickerTestSupport.resetHistory(HistoryRecord(HymnTypes.DB, 1, false))
    }

    @After
    fun tearDown() {
        instrumentation.setInTouchMode(true)
        PickerTestSupport.cleanUp()
    }

    @Test
    fun twoTapsBeforeTheFirstCommitRunsOpenOneOverlay() = PickerTestSupport.launch { scenario ->
        // all in one main-thread turn: no transaction runs between the calls
        scenario.onActivity { a ->
            a.openSearch(null)
            a.openSearch(null)
            a.openHistory()
        }
        instrumentation.waitForIdleSync()
        scenario.onActivity { a ->
            val fm = a.supportFragmentManager
            assertThat(fm.backStackEntryCount).isEqualTo(1)
            assertThat(fm.fragments.count { it.tag == MainHost.TAG_SEARCH }).isEqualTo(1)
            assertThat(fm.findFragmentByTag(MainHost.TAG_HISTORY)).isNull()
        }
    }

    @Test
    fun closingTheOverlayGivesTheKeyboardFocusBackToItsButton() = PickerTestSupport.launch { scenario ->
        FragmentHost.eventually(5_000) { onView(withId(R.id.btn_recent_more)).perform(scrollTo()) }
        instrumentation.setInTouchMode(false)
        instrumentation.waitForIdleSync()
        FragmentHost.eventually(3_000) {
            scenario.onActivity { a -> assertThat(a.findViewById<View>(R.id.btn_recent_more).requestFocus()).isTrue() }
        }
        scenario.onActivity { it.findViewById<View>(R.id.btn_recent_more).performClick() }
        onView(withId(R.id.history_list)).check(matches(isDisplayed()))
        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        FragmentHost.eventually(3_000) {
            scenario.onActivity { a -> assertThat(a.currentFocus?.id).isEqualTo(R.id.btn_recent_more) }
        }
    }

    @Test
    fun closingAnOverlayOpenedFromCodeGivesTheFocusBackToWhatHadIt() = PickerTestSupport.launch { scenario ->
        FragmentHost.eventually(5_000) { onView(withId(R.id.tv_search)).perform(scrollTo()) }
        instrumentation.setInTouchMode(false)
        instrumentation.waitForIdleSync()
        FragmentHost.eventually(3_000) {
            scenario.onActivity { a -> assertThat(a.findViewById<View>(R.id.tv_search).requestFocus()).isTrue() }
        }
        scenario.onActivity { it.openHistory() }
        onView(withId(R.id.history_list)).check(matches(isDisplayed()))
        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        FragmentHost.eventually(3_000) {
            scenario.onActivity { a -> assertThat(a.currentFocus?.id).isEqualTo(R.id.tv_search) }
        }
    }
}
