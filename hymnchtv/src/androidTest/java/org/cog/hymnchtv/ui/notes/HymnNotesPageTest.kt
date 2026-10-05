package org.cog.hymnchtv.ui.notes

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.QuickTest
import org.cog.hymnchtv.R
import org.cog.hymnchtv.notebook.NotebookTestSupport
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.cog.hymnchtv.ui.FragmentHost
import org.cog.hymnchtv.ui.notebook.NotebookPageActivity
import org.cog.hymnchtv.ui.notebook.NotebookPages
import org.hamcrest.CoreMatchers.containsString
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The notes page of one hymn: write, edit, delete, leave with unsaved text, rotation and process death (long drafts), sing summary, bad intents. */
@RunWith(AndroidJUnit4::class)
class HymnNotesPageTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()
    private val db5 = HymnKey.of(HymnTypes.DB, 5)

    @Before fun setUp() {
        NotebookTestSupport.resetNotes(db5)
        NotebookTestSupport.resetSingLogs(db5)
    }

    @After fun tearDown() {
        NotebookTestSupport.resetNotes(db5)
        NotebookTestSupport.resetSingLogs(db5)
    }

    private fun launch(): ActivityScenario<NotebookPageActivity> =
        ActivityScenario.launch(NotebookPages.notes(ctx, db5))

    private fun shownBodies(s: ActivityScenario<NotebookPageActivity>): List<String> {
        var bodies = emptyList<String>()
        s.onActivity { a ->
            bodies = (a.findViewById<RecyclerView>(R.id.notes_list).adapter as NoteAdapter).currentList.map { it.body }
        }
        return bodies
    }

    private fun typeAndSave(text: String) {
        onView(withId(R.id.note_input)).inRoot(isDialog()).perform(replaceText(text))
        onView(withText(R.string.notes_save)).inRoot(isDialog()).perform(click())
    }

    @Test @QuickTest fun writeEditAndDeleteANote() {
        launch().use { s ->
            FragmentHost.eventually { onView(withId(R.id.notes_empty)).check(matches(isDisplayed())) }
            onView(withId(R.id.notes_add)).perform(click())
            typeAndSave("主日唱這首\n很受感動")
            FragmentHost.eventually { assertThat(NotebookTestSupport.notes(db5).map { it.body }).containsExactly("主日唱這首\n很受感動") }
            FragmentHost.eventually { assertThat(shownBodies(s)).containsExactly("主日唱這首\n很受感動") }

            onView(withText(containsString("主日唱這首"))).perform(click())
            typeAndSave("改過的筆記")
            FragmentHost.eventually { assertThat(NotebookTestSupport.notes(db5).map { it.body }).containsExactly("改過的筆記") }

            onView(withText("改過的筆記")).perform(click())
            onView(withText(R.string.notes_delete)).inRoot(isDialog()).perform(click())
            onView(withText(R.string.delete)).inRoot(isDialog()).perform(click())
            FragmentHost.eventually { assertThat(NotebookTestSupport.notes(db5)).isEmpty() }
            FragmentHost.eventually { onView(withId(R.id.notes_empty)).check(matches(isDisplayed())) }
        }
    }

    @Test fun notesAreListedNewestFirst() {
        NotebookTestSupport.addNote(db5, "第一則")
        Thread.sleep(20)
        NotebookTestSupport.addNote(db5, "第二則")
        launch().use { s -> FragmentHost.eventually { assertThat(shownBodies(s)).containsExactly("第二則", "第一則").inOrder() } }
    }

    @Test fun leavingWithUnsavedTextAsksAndDontSaveKeepsNothing() {
        launch().use {
            onView(withId(R.id.notes_add)).perform(click())
            onView(withId(R.id.note_input)).inRoot(isDialog()).perform(replaceText("草稿"))
            pressBack()
            onView(withText(R.string.notes_unsaved)).inRoot(isDialog()).check(matches(isDisplayed()))
            onView(withText(R.string.notes_dont_save)).inRoot(isDialog()).perform(click())
            Thread.sleep(500)
            assertThat(NotebookTestSupport.notes(db5)).isEmpty()
        }
    }

    @Test fun theDraftSurvivesRecreation() {
        launch().use { s ->
            onView(withId(R.id.notes_add)).perform(click())
            onView(withId(R.id.note_input)).inRoot(isDialog()).perform(replaceText("轉螢幕前的草稿"))
            s.recreate()
            onView(withId(R.id.note_input)).inRoot(isDialog()).check(matches(withText("轉螢幕前的草稿")))
            onView(withText(R.string.notes_save)).inRoot(isDialog()).perform(click())
            FragmentHost.eventually { assertThat(NotebookTestSupport.notes(db5).map { it.body }).containsExactly("轉螢幕前的草稿") }
        }
    }

    @Test fun theSingSummaryShowsOnlyWhenTheHymnWasSung() {
        launch().use { s ->
            FragmentHost.eventually { onView(withId(R.id.notes_empty)).check(matches(isDisplayed())) }
            s.onActivity { assertThat(it.findViewById<View>(R.id.notes_sung).visibility).isEqualTo(View.GONE) }
        }
        NotebookTestSupport.recordSing(db5, System.currentTimeMillis() - 60_000)
        launch().use { s ->
            FragmentHost.eventually { s.onActivity { assertThat(it.findViewById<View>(R.id.notes_sung).visibility).isEqualTo(View.VISIBLE) } }
        }
    }

    @Test fun anIntentWithoutAValidHymnClosesThePage() {
        val bad = Intent(ctx, NotebookPageActivity::class.java)
            .putExtra(NotebookPages.EXTRA_PAGE, NotebookPages.PAGE_NOTES)
            .putExtra(NotebookPages.ARG_HYMN_TYPE, HymnTypes.BB)
            .putExtra(NotebookPages.ARG_HYMN_NO, 2000)
        ActivityScenario.launch<NotebookPageActivity>(bad).use { s ->
            FragmentHost.eventually { assertThat(s.state).isEqualTo(Lifecycle.State.DESTROYED) }
        }
    }

    @Test fun aDraftLongerThanABundleCouldCarrySurvivesRecreation() {
        val long = "長".repeat(30_000)
        launch().use { s ->
            onView(withId(R.id.notes_add)).perform(click())
            onView(withId(R.id.note_input)).inRoot(isDialog()).perform(replaceText(long))
            s.recreate()
            onView(withId(R.id.note_input)).inRoot(isDialog()).check(matches(withText(long)))
        }
    }

    /**
     * Process death in miniature: a second ViewModel gets only what a Bundle would carry (the handle's small values) and must
     * find the long draft in its private file; closing the editor removes the file.
     */
    @Test fun aLongDraftComesBackFromItsFileAfterProcessDeath() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val long = "草稿".repeat(20_000)
        var restoredDraft: String? = null
        var restoredEditing: String? = null
        var largestSavedValue = -1
        var token: String? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val handle = SavedStateHandle(mapOf(NotebookPages.ARG_HYMN_TYPE to HymnTypes.DB, NotebookPages.ARG_HYMN_NO to 5))
            val first = HymnNotesViewModel(app, handle)
            first.openEditor(null)
            first.draft = long
            first.persistDraftNow()
            token = handle.get<String>(HymnNotesViewModel.KEY_DRAFT_TOKEN)
            val carried = handle.keys().associateWith { handle.get<Any?>(it) }
            largestSavedValue = carried.values.maxOf { (it as? String)?.length ?: 0 }
            val second = HymnNotesViewModel(app, SavedStateHandle(carried))
            restoredDraft = second.draft
            restoredEditing = second.editing
            second.closeEditor()
        }
        assertThat(largestSavedValue).isLessThan(100)
        assertThat(restoredDraft).isEqualTo(long)
        assertThat(restoredEditing).isEqualTo(HymnNotesViewModel.NEW_NOTE)
        FragmentHost.eventually { assertThat(NoteDraftFiles.inApp(ctx).read(token!!)).isNull() }
    }

    /** Close, then at once reopen a new note for the same hymn: the first session's late cleanup must not delete the second draft. */
    @Test fun aReopenedEditorsDraftSurvivesThePreviousCleanup() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        var firstToken: String? = null
        var secondToken: String? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val handle = SavedStateHandle(mapOf(NotebookPages.ARG_HYMN_TYPE to HymnTypes.DB, NotebookPages.ARG_HYMN_NO to 5))
            val vm = HymnNotesViewModel(app, handle)
            vm.openEditor(null)
            vm.draft = "第一次"
            vm.persistDraftNow()
            firstToken = handle.get<String>(HymnNotesViewModel.KEY_DRAFT_TOKEN)
            vm.closeEditor() // its delete runs later on IO
            vm.openEditor(null)
            vm.draft = "第二次"
            vm.persistDraftNow()
            secondToken = handle.get<String>(HymnNotesViewModel.KEY_DRAFT_TOKEN)
        }
        assertThat(secondToken).isNotEqualTo(firstToken)
        val files = NoteDraftFiles.inApp(ctx)
        FragmentHost.eventually { assertThat(files.read(firstToken!!)).isNull() }
        Thread.sleep(500)
        assertThat(files.read(secondToken!!)).isEqualTo("第二次")
        files.delete(secondToken!!)
    }

    @Test fun iconButtonsAndRowsHaveSpokenLabels() {
        NotebookTestSupport.addNote(db5, "可朗讀")
        launch().use { s ->
            FragmentHost.eventually { assertThat(shownBodies(s)).hasSize(1) }
            s.onActivity { a ->
                val row = a.findViewById<RecyclerView>(R.id.notes_list).getChildAt(0)
                assertThat(row.contentDescription.toString()).contains("可朗讀")
            }
        }
    }

    /** After process death the editor opens before the list has loaded: the stored text must be known only once it has. */
    @Test fun theOriginalTextOfAStoredNoteIsKnownOnlyOnceTheListHasLoaded() {
        val note = NotebookTestSupport.addNote(db5, "原文")
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        var vm: HymnNotesViewModel? = null
        var knownAtOnce = true
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val handle = SavedStateHandle(mapOf(NotebookPages.ARG_HYMN_TYPE to HymnTypes.DB, NotebookPages.ARG_HYMN_NO to 5))
            vm = HymnNotesViewModel(app, handle)
            knownAtOnce = vm!!.originalKnown()
        }
        assertThat(knownAtOnce).isFalse()
        FragmentHost.eventually {
            var known = false
            var text: String? = null
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                known = vm!!.originalKnown()
                text = vm!!.originalOf(note.id)
            }
            assertThat(known).isTrue()
            assertThat(text).isEqualTo("原文")
        }
    }
}
