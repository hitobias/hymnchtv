package org.cog.hymnchtv.ui.notes

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cog.hymnchtv.notebook.data.SingStats
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.repo.NoteRepository
import org.cog.hymnchtv.notebook.repo.SingLogRepository
import timber.log.Timber

/** A one-shot problem for the page to show; cleared with [HymnNotesStore.consumeMessage]. */
enum class NotesMessage { SAVE_FAILED, DELETE_FAILED }

data class NotesState(
    val loading: Boolean = true,
    val rows: List<NoteRow> = emptyList(),
    /** How often the hymn was sung (D-1 F4); shown only when it was. */
    val stats: SingStats? = null,
    val loadFailed: Boolean = false,
    val message: NotesMessage? = null,
)

/**
 * The notes of one hymn. Reads and writes run on [io]; [scope] is the ViewModel's. A write runs NonCancellable, so leaving the
 * page never drops a half-done save, and always ends with a reload, so the list shows what is stored.
 */
class HymnNotesStore(
    private val key: HymnKey,
    private val notes: NoteRepository,
    private val singLogs: SingLogRepository,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher,
) {
    private val mutable = MutableStateFlow(NotesState())
    val state: StateFlow<NotesState> = mutable.asStateFlow()
    private var loadJob: Job? = null

    fun load() {
        loadJob?.cancel()
        loadJob = scope.launch {
            try {
                val (entities, stats) = withContext(io) { notes.findByHymn(key) to singLogs.statsFor(key) }
                mutable.update { it.copy(loading = false, rows = NoteRows.build(entities), stats = stats, loadFailed = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Loading the notes of %s failed", key)
                mutable.update { it.copy(loading = false, loadFailed = true) }
            }
        }
    }

    /** Adds a note when [id] is null, otherwise replaces the text of that note. */
    fun save(id: String?, body: String) = write(NotesMessage.SAVE_FAILED) {
        if (id == null) {
            notes.add(key, body)
            true
        } else {
            val existing = notes.findById(id)
            existing != null && notes.update(existing.copy(body = body)) != null
        }
    }

    fun delete(id: String) = write(NotesMessage.DELETE_FAILED) { notes.delete(id) }

    fun consumeMessage() = mutable.update { it.copy(message = null) }

    private fun write(failure: NotesMessage, block: suspend () -> Boolean) {
        scope.launch {
            val ok = withContext(NonCancellable + io) {
                try {
                    block()
                } catch (e: Exception) {
                    Timber.w(e, "A note write for %s failed", key)
                    false
                }
            }
            if (!ok) mutable.update { it.copy(message = failure) }
            load()
        }
    }
}
