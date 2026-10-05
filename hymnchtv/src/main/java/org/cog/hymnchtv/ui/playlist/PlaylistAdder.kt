package org.cog.hymnchtv.ui.playlist

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.repo.PlaylistRepository
import timber.log.Timber

/**
 * Adds a hymn from the lyrics page. [scope] is NotebookGraph.appScope (process lifetime), so closing the dialog never drops the
 * write. [onDone] runs on [main] with the playlist name, or null when nothing was added.
 */
class PlaylistAdder(
    private val repo: PlaylistRepository,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher,
    private val main: CoroutineDispatcher,
) {
    fun add(playlistId: String, key: HymnKey, onDone: (String?) -> Unit) = launchWrite(onDone) {
        val playlist = repo.findById(playlistId) ?: return@launchWrite null
        repo.addItem(playlist.id, key)?.let { playlist.name }
    }

    /** One transaction (createPlaylistWithItem): a failure leaves no empty playlist behind. */
    fun createAndAdd(name: String, key: HymnKey, onDone: (String?) -> Unit) = launchWrite(onDone) {
        repo.createPlaylistWithItem(name, key).first.name
    }

    private fun launchWrite(onDone: (String?) -> Unit, block: suspend () -> String?) {
        scope.launch {
            val name = withContext(io) {
                try {
                    block()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.w(e, "Adding to a playlist failed")
                    null
                }
            }
            withContext(main) { onDone(name) }
        }
    }
}
