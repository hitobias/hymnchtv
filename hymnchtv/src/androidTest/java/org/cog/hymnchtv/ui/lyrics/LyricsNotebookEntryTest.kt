package org.cog.hymnchtv.ui.lyrics

import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import org.cog.hymnchtv.ui.notebook.NotebookPageActivity
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The notebook entries of the lyrics page's More menu (D-1 F1/F2) and the notes mark in the title row. */
@RunWith(AndroidJUnit4::class)
class LyricsNotebookEntryTest : LyricsTestBase() {
    private val db5 = HymnKey.of(HymnTypes.DB, 5)

    @Before fun reset() {
        NotebookTestSupport.resetNotes(db5)
        NotebookTestSupport.resetPlaylists()
    }

    @After fun cleanUp() {
        NotebookTestSupport.resetNotes(db5)
        NotebookTestSupport.resetPlaylists()
    }

    private fun notebookPageResumed(): Boolean {
        var found = false
        instrumentation.runOnMainSync {
            found = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).any { it is NotebookPageActivity }
        }
        return found
    }

    private fun closeNotebookPages() = instrumentation.runOnMainSync {
        ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).filterIsInstance<NotebookPageActivity>().forEach { it.finish() }
    }

    @Test @QuickTest fun theMenuShowsTheNoteCountAndOpensTheNotesPage() {
        NotebookTestSupport.addNote(db5, "一")
        NotebookTestSupport.addNote(db5, "二")
        launch(MainActivity.HYMN_DB, 5).use { s ->
            s.await("note count known") { it.noteCountFor(MainActivity.HYMN_DB, 5) == 2 }
            s.revealChrome()
            onView(withId(R.id.btn_more)).perform(click())
            onView(withText(ctx.getString(R.string.notes_menu_count, 2))).inRoot(isPlatformPopup()).perform(click())
            FragmentHost.eventually { assertThat(notebookPageResumed()).isTrue() }
            closeNotebookPages()
        }
    }

    @Test fun withoutNotesTheMenuSaysNotesAndTheMarkIsHidden() {
        launch(MainActivity.HYMN_DB, 5).use { s ->
            s.await("note count known") { it.noteCountFor(MainActivity.HYMN_DB, 5) == 0 }
            assertThat(s.read { page(it)!!.findViewById<View>(R.id.notes_mark).visibility }).isEqualTo(View.GONE)
            s.revealChrome()
            onView(withId(R.id.btn_more)).perform(click())
            onView(withText(R.string.notes_menu)).inRoot(isPlatformPopup()).perform(click())
            FragmentHost.eventually { assertThat(notebookPageResumed()).isTrue() }
            closeNotebookPages()
        }
    }

    @Test fun theMarkAppearsWhenComingBackWithANote() {
        launch(MainActivity.HYMN_DB, 5).use { s ->
            s.await("note count known") { it.noteCountFor(MainActivity.HYMN_DB, 5) == 0 }
            NotebookTestSupport.addNote(db5, "回來後要看到")
            s.moveToState(Lifecycle.State.STARTED)
            s.moveToState(Lifecycle.State.RESUMED)
            s.await("note mark shown") { page(it)!!.findViewById<View>(R.id.notes_mark).visibility == View.VISIBLE }
            assertThat(s.read { page(it)!!.findViewById<View>(R.id.notes_mark).contentDescription.toString() })
                .isEqualTo(ctx.getString(R.string.notes_marked))
        }
    }

    @Test @QuickTest fun addToPlaylistFromTheMenu() {
        val sunday = NotebookTestSupport.playlist("主日")
        launch(MainActivity.HYMN_DB, 5).use { s ->
            s.await("hymn known") { it.hasNotebookHymn() }
            s.revealChrome()
            onView(withId(R.id.btn_more)).perform(click())
            onView(withText(R.string.playlist_menu_add)).inRoot(isPlatformPopup()).perform(click())
            onView(withText("主日")).inRoot(isDialog()).perform(click())
            FragmentHost.eventually { assertThat(NotebookTestSupport.items(sunday.id)).containsExactly(db5) }
        }
    }
}
