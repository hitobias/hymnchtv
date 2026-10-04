package org.cog.hymnchtv.ui.home

import org.cog.hymnchtv.QuickTest
import android.content.pm.ActivityInfo
import android.os.ParcelFileDescriptor
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.hymnhistory.HistoryRecord
import org.cog.hymnchtv.notebook.FavoriteTestSupport
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.cog.hymnchtv.ui.FragmentHost
import org.cog.hymnchtv.ui.picker.HymnLabels
import org.cog.hymnchtv.ui.picker.PickerTestSupport
import org.cog.hymnchtv.ui.picker.PickerTestSupport.ctx
import org.hamcrest.CoreMatchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The "History & favourites" button on the home page is always reachable, so the Favourites tab always has an entry. */
@RunWith(AndroidJUnit4::class)
class FavoritesEntryTest {
    @Before fun setUp() {
        PickerTestSupport.prepare()
        FavoriteTestSupport.reset()
        ctx.getSharedPreferences(MainActivity.PREF_SETTINGS, 0).edit().remove(HomePrefs.HISTORY_TAB).commit()
    }

    @After fun tearDown() {
        shell("wm size reset")
        FavoriteTestSupport.reset()
        PickerTestSupport.resetHistory()
        ctx.getSharedPreferences(MainActivity.PREF_SETTINGS, 0).edit().remove(HomePrefs.HISTORY_TAB).commit()
        PickerTestSupport.cleanUp()
    }

    private fun shell(command: String) {
        val pfd = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }
    }

    private fun records(n: Int) = (1..n).map { HistoryRecord(HymnTypes.DB, it, false, "標題$it", 1000L + it) }.toTypedArray()

    private fun assertEntryReachable(a: MainActivity) {
        assertThat(a.findViewById<View>(R.id.btn_recent_more).visibility).isEqualTo(View.VISIBLE)
        assertThat(a.findViewById<View>(R.id.btn_recent_more).hasOnClickListeners()).isTrue()
    }

    @Test fun entryVisibleWithNoHistory() {
        PickerTestSupport.resetHistory()
        PickerTestSupport.launch { scenario ->
            FragmentHost.eventually { scenario.onActivity { assertEntryReachable(it) } }
            onView(withId(R.id.btn_recent_more)).perform(scrollTo()).check(matches(isDisplayed()))
        }
    }

    @Test fun entryVisibleWhenNoRowsFit() {
        PickerTestSupport.resetHistory(*records(12))
        shell("wm size 480x700")
        PickerTestSupport.launch { scenario ->
            FragmentHost.eventually { scenario.onActivity { assertEntryReachable(it) } }
            onView(withId(R.id.btn_recent_more)).perform(scrollTo()).check(matches(isDisplayed()))
        }
    }

    @Test @QuickTest fun entryOpensHistoryWithFavouritesTabReachable() {
        PickerTestSupport.resetHistory()
        FavoriteTestSupport.add(HymnKey.of(HymnTypes.DB, 7))
        PickerTestSupport.launch { scenario ->
            FragmentHost.eventually { scenario.onActivity { assertEntryReachable(it) } }
            onView(withId(R.id.btn_recent_more)).perform(scrollTo(), click())
            onView(allOf(withText(R.string.fav_tab_favorites), androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA(withId(R.id.history_tabs)))).perform(click())
            val headline = HymnLabels.headline(ctx, org.cog.hymnchtv.hymn.HymnRef(HymnTypes.DB, 7))
            FragmentHost.eventually { onView(allOf(withText(headline), isDisplayed())).check(matches(isDisplayed())) }
            scenario.onActivity { assertThat(it.findViewById<RecyclerView>(R.id.favorites_list).adapter!!.itemCount).isEqualTo(1) }
        }
    }

    @Test fun entryVisibleInLandscape() {
        PickerTestSupport.resetHistory()
        PickerTestSupport.launch { scenario ->
            scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
            FragmentHost.eventually(6000) { scenario.onActivity { assertEntryReachable(it) } }
            onView(withId(R.id.btn_recent_more)).perform(scrollTo()).check(matches(isDisplayed()))
            scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
        }
    }
}
