package org.cog.hymnchtv.notebook.contract

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.cog.hymnchtv.notebook.contract.SingLogRepositoryContract.Companion.DEVICE
import org.cog.hymnchtv.notebook.contract.SingLogRepositoryContract.Companion.NOW
import org.cog.hymnchtv.notebook.model.Clock
import org.cog.hymnchtv.notebook.model.DeviceIdProvider
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.cog.hymnchtv.notebook.model.NotebookValidation
import org.cog.hymnchtv.notebook.repo.NoteRepository
import org.junit.After
import org.junit.Test

/**
 * Behaviour every NoteRepository must share (the notes page relies on it). Subclasses: InMemoryNoteRepositoryContractTest (JVM)
 * and RoomNoteRepositoryContractTest (androidTest).
 */
abstract class NoteRepositoryContract {
    protected val clock = ContractClock(NOW)
    private val db1 = HymnKey.of(HymnTypes.DB, 1)
    private val db2 = HymnKey.of(HymnTypes.DB, 2)
    private val repo by lazy { newRepository(clock, DeviceIdProvider { DEVICE }) }

    protected abstract fun newRepository(clock: Clock, device: DeviceIdProvider): NoteRepository

    protected open fun tearDownRepository() {}

    @After
    fun closeRepository() = tearDownRepository()

    private fun rejects(block: suspend () -> Unit) = runBlocking {
        assertThat(runCatching { block() }.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun addStampsTheRowAndEachHymnListsItsOwnNotesNewestFirst(): Unit = runBlocking {
        val first = repo.add(db1, "first")
        clock.now = NOW + 1_000
        val second = repo.add(db1, "  second\nline")
        repo.add(db2, "other hymn")
        assertThat(NotebookValidation.uuid(first.id)).isEqualTo(first.id)
        assertThat(first.createdAt).isEqualTo(NOW)
        assertThat(first.updatedBy).isEqualTo(DEVICE)
        assertThat(repo.findByHymn(db1).map { it.id }).containsExactly(second.id, first.id).inOrder()
        // the text is kept verbatim: leading spaces and line breaks matter
        assertThat(repo.findByHymn(db1).first().body).isEqualTo("  second\nline")
    }

    @Test
    fun blankOrTooLongTextIsRejected() {
        rejects { repo.add(db1, " \n ") }
        rejects { repo.add(db1, "x".repeat(NotebookValidation.MAX_NOTE_LENGTH + 1)) }
    }

    @Test
    fun updateKeepsCreatedAtAndBumpsUpdatedAt(): Unit = runBlocking {
        val note = repo.add(db1, "a")
        clock.now = NOW + 5_000
        val updated = checkNotNull(repo.update(note.copy(body = "b")))
        assertThat(updated.createdAt).isEqualTo(NOW)
        assertThat(updated.updatedAt).isEqualTo(NOW + 5_000)
        assertThat(repo.findById(note.id)?.body).isEqualTo("b")
    }

    @Test
    fun deleteHidesTheNoteAndADeletedNoteCannotBeUpdated(): Unit = runBlocking {
        val note = repo.add(db1, "a")
        assertThat(repo.delete(note.id)).isTrue()
        assertThat(repo.delete(note.id)).isFalse()
        assertThat(repo.findByHymn(db1)).isEmpty()
        assertThat(repo.update(note.copy(body = "c"))).isNull()
    }
}
