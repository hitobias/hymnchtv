package org.cog.hymnchtv.ui.home

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.R
import org.cog.hymnchtv.hymn.HymnRef
import org.cog.hymnchtv.notebook.FavoriteTestSupport
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.cog.hymnchtv.ui.FragmentHost
import org.cog.hymnchtv.ui.picker.HymnLabels
import org.cog.hymnchtv.ui.picker.PickerTestSupport
import org.cog.hymnchtv.ui.picker.PickerTestSupport.ctx
import org.hamcrest.CoreMatchers.allOf
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Favourites survive an app restart. Run as two separate instrumentation calls with a force-stop in between:
 * `-e class ...FavoritesPersistenceTest#seed`, `adb shell am force-stop <pkg>`, `-e class ...FavoritesPersistenceTest#verify`.
 * (A plain full-suite run executes seed then verify in one process, which still checks the stored rows.)
 */
@RunWith(AndroidJUnit4::class)
class FavoritesPersistenceTest {
    private val db5 = HymnKey.of(HymnTypes.DB, 5)
    private val yb276 = HymnKey.of(HymnTypes.YB, 276)

    @Test fun seed() {
        // No reset(): the rows must be left behind for verify
        FavoriteTestSupport.reset()
        FavoriteTestSupport.add(db5, yb276)
        assertThat(FavoriteTestSupport.active().map { it.hymn }).containsAtLeast(db5, yb276)
    }

    @Test fun verify() {
        try {
            assertThat(FavoriteTestSupport.active().map { it.hymn }).containsAtLeast(db5, yb276)
            PickerTestSupport.prepare()
            PickerTestSupport.launch {
                FragmentHost.eventually { onView(withId(R.id.btn_recent_more)).check(matches(withEffectiveVisibility(Visibility.VISIBLE))) }
                onView(withId(R.id.btn_recent_more)).perform(scrollTo(), click())
                onView(allOf(withText(R.string.fav_tab_favorites), isDescendantOfA(withId(R.id.history_tabs)))).perform(click())
                listOf(HymnTypes.DB to 5, HymnTypes.YB to 276).forEach { (book, no) ->
                    val label = HymnLabels.headline(ctx, HymnRef(book, no))
                    FragmentHost.eventually { onView(allOf(withText(label), isDisplayed())).check(matches(isDisplayed())) }
                }
            }
        } finally {
            FavoriteTestSupport.reset()
            PickerTestSupport.cleanUp()
        }
    }
}
