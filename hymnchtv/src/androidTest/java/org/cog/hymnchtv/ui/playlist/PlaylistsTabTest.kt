package org.cog.hymnchtv.ui.playlist

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.QuickTest
import org.cog.hymnchtv.R
import org.cog.hymnchtv.notebook.NotebookTestSupport
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.cog.hymnchtv.ui.FragmentHost
import org.cog.hymnchtv.ui.home.HomePrefs
import org.cog.hymnchtv.ui.notebook.NotebookPageActivity
import org.cog.hymnchtv.ui.picker.PickerTestSupport
import org.cog.hymnchtv.ui.picker.PickerTestSupport.ctx
import org.hamcrest.CoreMatchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The Playlists tab of the "History & favourites" page. */
@RunWith(AndroidJUnit4::class)
class PlaylistsTabTest {
    private val prefs get() = ctx.getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE)
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Before fun setUp() {
        PickerTestSupport.prepare()
        NotebookTestSupport.resetPlaylists()
        prefs.edit().remove(HomePrefs.HISTORY_TAB).commit()
    }

    @After fun tearDown() {
        NotebookTestSupport.resetPlaylists()
        prefs.edit().remove(HomePrefs.HISTORY_TAB).commit()
        PickerTestSupport.cleanUp()
    }

    private fun tab(label: Int) = allOf(withText(label), isDescendantOfA(withId(R.id.history_tabs)))

    private fun openPlaylistsTab() {
        FragmentHost.eventually { onView(withId(R.id.btn_recent_more)).check(matches(withEffectiveVisibility(Visibility.VISIBLE))) }
        onView(withId(R.id.btn_recent_more)).perform(scrollTo(), click())
        onView(tab(R.string.playlist_tab)).perform(click())
        onView(withId(R.id.playlists_list)).check(matches(isDisplayed()))
    }

    private fun rows(): List<PlaylistRow> {
        var rows = emptyList<PlaylistRow>()
        instrumentation.runOnMainSync {
            val activity = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                .filterIsInstance<MainActivity>().firstOrNull() ?: return@runOnMainSync
            rows = (activity.findViewById<RecyclerView>(R.id.playlists_list)?.adapter as? PlaylistAdapter)?.currentList.orEmpty()
        }
        return rows
    }

    private fun resumedNotebookPage(): Boolean {
        var found = false
        instrumentation.runOnMainSync {
            found = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).any { it is NotebookPageActivity }
        }
        return found
    }

    @Test @QuickTest fun anEmptyTabExplainsHowToStart() {
        PickerTestSupport.launch {
            openPlaylistsTab()
            FragmentHost.eventually { onView(withId(R.id.playlists_empty)).check(matches(isDisplayed())) }
        }
    }

    @Test @QuickTest fun playlistsAreListedWithTheirSizesAndTapOpensOne() {
        NotebookTestSupport.playlist("主日", HymnKey.of(HymnTypes.DB, 5), HymnKey.of(HymnTypes.DB, 6))
        PickerTestSupport.launch {
            openPlaylistsTab()
            FragmentHost.eventually { assertThat(rows().map { it.name to it.count }).containsExactly("主日" to 2) }
            onView(withText("主日")).perform(click())
            FragmentHost.eventually { assertThat(resumedNotebookPage()).isTrue() }
            pressBack()
        }
    }

    @Test fun newPlaylistIsCreatedAndOpened() {
        PickerTestSupport.launch {
            openPlaylistsTab()
            onView(withId(R.id.playlists_new)).perform(click())
            onView(withId(R.id.playlist_name_input)).inRoot(isDialog()).perform(replaceText("  晚上聚會 "))
            onView(withText(android.R.string.ok)).inRoot(isDialog()).perform(click())
            FragmentHost.eventually { assertThat(NotebookTestSupport.playlists().map { it.name }).containsExactly("晚上聚會") }
            FragmentHost.eventually { assertThat(resumedNotebookPage()).isTrue() }
            pressBack()
            FragmentHost.eventually { assertThat(rows().map { it.name }).containsExactly("晚上聚會") }
        }
    }

    @Test fun aBlankNameKeepsTheDialogOpen() {
        PickerTestSupport.launch {
            openPlaylistsTab()
            onView(withId(R.id.playlists_new)).perform(click())
            onView(withId(R.id.playlist_name_input)).inRoot(isDialog()).perform(replaceText("   "))
            onView(withText(android.R.string.ok)).inRoot(isDialog()).perform(click())
            onView(withId(R.id.playlist_name_input)).inRoot(isDialog()).check(matches(isDisplayed()))
            assertThat(NotebookTestSupport.playlists()).isEmpty()
        }
    }

    @Test fun thePlaylistsTabIsRemembered() {
        PickerTestSupport.launch {
            openPlaylistsTab()
            pressBack()
            onView(withId(R.id.btn_recent_more)).perform(scrollTo(), click())
            onView(withId(R.id.playlists_list)).check(matches(isDisplayed()))
        }
    }
}
