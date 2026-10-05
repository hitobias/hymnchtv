package org.cog.hymnchtv.ui.notes

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.cog.hymnchtv.notebook.fakes.InMemoryNoteRepository
import org.cog.hymnchtv.notebook.fakes.InMemorySingLogRepository
import org.cog.hymnchtv.notebook.fakes.MutableClock
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.cog.hymnchtv.notebook.model.Occasion
import org.cog.hymnchtv.notebook.model.SingSource
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HymnNotesStoreTest {
    private val db5 = HymnKey.of(HymnTypes.DB, 5)
    private val db6 = HymnKey.of(HymnTypes.DB, 6)

    private class Harness(private val scope: TestScope) {
        val clock = MutableClock(1_790_733_600_000L)
        val notes = InMemoryNoteRepository(clock)
        val singLogs = InMemorySingLogRepository(clock)

        fun store(key: HymnKey) =
            HymnNotesStore(key, notes, singLogs, scope.backgroundScope, UnconfinedTestDispatcher(scope.testScheduler))
    }

    @Test fun loadListsOnlyThisHymnNewestFirstWithItsSingStats() = runTest {
        val h = Harness(this)
        h.notes.add(db5, "older")
        h.clock.now += 1_000
        h.notes.add(db5, "newer")
        h.notes.add(db6, "other hymn")
        h.singLogs.record(db5, h.clock.now - 10, Occasion.HOME, SingSource.MANUAL)
        val store = h.store(db5)
        assertThat(store.state.value.loading).isTrue()
        store.load()
        runCurrent()
        val state = store.state.value
        assertThat(state.loading).isFalse()
        assertThat(state.rows.map { it.body }).containsExactly("newer", "older").inOrder()
        assertThat(state.stats?.singCount).isEqualTo(1)
        assertThat(state.loadFailed).isFalse()
    }

    @Test fun saveWithoutAnIdAddsANoteAndReloads() = runTest {
        val h = Harness(this)
        val store = h.store(db5)
        store.load()
        store.save(null, "  主日唱這首\n第二行")
        runCurrent()
        assertThat(store.state.value.rows.map { it.body }).containsExactly("  主日唱這首\n第二行")
        assertThat(h.notes.findByHymn(db5)).hasSize(1)
    }

    @Test fun saveWithAnIdReplacesTheTextAndMarksTheRowEdited() = runTest {
        val h = Harness(this)
        val note = h.notes.add(db5, "a")
        val store = h.store(db5)
        h.clock.now += 60_000
        store.save(note.id, "b")
        runCurrent()
        val row = store.state.value.rows.single()
        assertThat(row.body).isEqualTo("b")
        assertThat(row.edited).isTrue()
    }

    @Test fun aRejectedSaveIsReportedAndTheListStays() = runTest {
        val h = Harness(this)
        h.notes.add(db5, "kept")
        val store = h.store(db5)
        store.save(null, "   ")
        runCurrent()
        assertThat(store.state.value.message).isEqualTo(NotesMessage.SAVE_FAILED)
        assertThat(store.state.value.rows.map { it.body }).containsExactly("kept")
        store.consumeMessage()
        assertThat(store.state.value.message).isNull()
    }

    @Test fun savingANoteDeletedMeanwhileIsReported() = runTest {
        val h = Harness(this)
        val note = h.notes.add(db5, "a")
        h.notes.delete(note.id)
        val store = h.store(db5)
        store.save(note.id, "b")
        runCurrent()
        assertThat(store.state.value.message).isEqualTo(NotesMessage.SAVE_FAILED)
        assertThat(store.state.value.rows).isEmpty()
    }

    @Test fun deleteRemovesTheRow() = runTest {
        val h = Harness(this)
        val note = h.notes.add(db5, "a")
        val store = h.store(db5)
        store.delete(note.id)
        runCurrent()
        assertThat(store.state.value.rows).isEmpty()
        assertThat(store.state.value.message).isNull()
    }

    @Test fun aFailedLoadIsNotShownAsEmpty() = runTest {
        val h = Harness(this)
        h.notes.failNext = true
        val store = h.store(db5)
        store.load()
        runCurrent()
        assertThat(store.state.value.loading).isFalse()
        assertThat(store.state.value.loadFailed).isTrue()
    }
}
