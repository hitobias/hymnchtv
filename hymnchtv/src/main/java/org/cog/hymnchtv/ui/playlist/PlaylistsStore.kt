package org.cog.hymnchtv.ui.playlist

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
import org.cog.hymnchtv.notebook.data.entity.PlaylistEntity
import org.cog.hymnchtv.notebook.repo.PlaylistRepository
import timber.log.Timber

/** A one-shot problem for the screen to show; cleared with consumeMessage(). */
enum class PlaylistMessage { WRITE_FAILED }

data class PlaylistsState(
    val loading: Boolean = true,
    val rows: List<PlaylistRow> = emptyList(),
    val loadFailed: Boolean = false,
    val message: PlaylistMessage? = null,
)

/** The playlists with their sizes (playlists tab, add-to-playlist dialog). Reads and writes run on [io]. */
class PlaylistsStore(
    private val repo: PlaylistRepository,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher,
) {
    private val mutable = MutableStateFlow(PlaylistsState())
    val state: StateFlow<PlaylistsState> = mutable.asStateFlow()
    private var loadJob: Job? = null

    fun load() {
        loadJob?.cancel()
        loadJob = scope.launch {
            try {
                val rows = withContext(io) {
                    val playlists = repo.findAll()
                    PlaylistRows.build(playlists, playlists.associate { it.id to repo.items(it.id).size })
                }
                mutable.update { it.copy(loading = false, rows = rows, loadFailed = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Loading the playlists failed")
                mutable.update { it.copy(loading = false, loadFailed = true) }
            }
        }
    }

    /** [onCreated] gets the stored playlist (on [scope]); a rejected name or a failure is reported through the state. */
    fun create(name: String, onCreated: (PlaylistEntity) -> Unit = {}) {
        scope.launch {
            val created = withContext(NonCancellable + io) {
                try {
                    repo.createPlaylist(name)
                } catch (e: Exception) {
                    Timber.w(e, "Creating a playlist failed")
                    null
                }
            }
            if (created == null) mutable.update { it.copy(message = PlaylistMessage.WRITE_FAILED) } else onCreated(created)
            load()
        }
    }

    fun consumeMessage() = mutable.update { it.copy(message = null) }
}
