package org.cog.hymnchtv.ui.playlist

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.cog.hymnchtv.hymn.HymnRef
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.repo.PlaylistRepository
import timber.log.Timber

/** One hymn of the playlist page. */
data class DetailItem(val itemId: String, val key: HymnKey, val ref: HymnRef, val title: String?)

/** What undo needs to put a removed hymn back where it was. */
data class Removed(val key: HymnKey, val index: Int)

data class DetailState(
    val loading: Boolean = true,
    val name: String = "",
    val items: List<DetailItem> = emptyList(),
    /** The playlist no longer exists (deleted, or a stale link): the page closes. */
    val missing: Boolean = false,
    val loadFailed: Boolean = false,
    val lastOpenedId: String? = null,
    val message: PlaylistMessage? = null,
) {
    val button: PlayButton? get() = PlaylistOrder.button(items.map { it.itemId }, lastOpenedId)

    /** The row marked "Next". */
    val nextIndex: Int? get() = (button as? PlayButton.Next)?.index
}

/**
 * One playlist: name, hymns (with titles from [titleOf], called on [io]) and the "open in order" position. Changes show on
 * screen at once. Every write and every reload runs under [lock], one at a time in call order (NonCancellable + [io] alone
 * would let them overlap). A reload is shown only when no write is still queued, so an older storage snapshot never replaces
 * a newer screen; after the last write the stored state wins (a failed write is thereby undone on screen). [scope] must be
 * single-threaded (the ViewModel's main scope), since [pending] is touched only there.
 */
class PlaylistDetailStore(
    private val playlistId: String,
    private val repo: PlaylistRepository,
    private val titleOf: suspend (HymnRef) -> String?,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher,
    lastOpenedId: String? = null,
) {
    private class Loaded(val name: String, val items: List<DetailItem>)

    private val mutable = MutableStateFlow(DetailState(lastOpenedId = lastOpenedId))
    val state: StateFlow<DetailState> = mutable.asStateFlow()
    private val lock = Mutex()

    /** Writes queued or running; a reload is applied only when this is 0. */
    private var pending = 0

    fun load() = enqueue(write = null)

    fun markOpened(itemId: String) = mutable.update { it.copy(lastOpenedId = itemId) }

    fun rename(name: String) = enqueue(write = { repo.rename(playlistId, name) != null })

    /** [onDeleted] runs after a successful delete (the reload then also reports the playlist missing). */
    fun delete(onDeleted: () -> Unit) = enqueue(write = { repo.delete(playlistId) }, after = { ok -> if (ok) onDeleted() })

    /** Takes the item off the screen at once; returns what undo needs, or null when the item is not shown. */
    fun remove(itemId: String): Removed? {
        val items = mutable.value.items
        val index = items.indexOfFirst { it.itemId == itemId }
        if (index < 0) return null
        mutable.update { s -> s.copy(items = s.items.filterNot { it.itemId == itemId }) }
        enqueue(write = {
            repo.removeItem(itemId) // false: already gone, which is what was wanted
            true
        })
        return Removed(items[index].key, index)
    }

    /** A soft-deleted row cannot come back, so undo adds the hymn again and moves the new row to the old position. */
    fun undoRemove(removed: Removed) = enqueue(write = {
        val added = repo.addItem(playlistId, removed.key)
        if (added == null) {
            false
        } else {
            val others = repo.items(playlistId).map { it.id }.filterNot { it == added.id }
            val at = removed.index.coerceIn(0, others.size)
            repo.reorder(playlistId, others.take(at) + added.id + others.drop(at))
            true
        }
    })

    /** Shows [orderedIds] (must be exactly the shown items, in a new order) at once and stores it; anything else is ignored. */
    fun reorder(orderedIds: List<String>) {
        val byId = mutable.value.items.associateBy { it.itemId }
        if (orderedIds.size != byId.size || orderedIds.toSet() != byId.keys) return
        mutable.update { s -> s.copy(items = orderedIds.map(byId::getValue)) }
        enqueue(write = {
            // Recomputed against storage inside the lock: rows the screen knows keep the screen's order, rows it has not seen
            // yet (e.g. an undo whose reload is still queued) stay after them in stored order, so the permutation is valid
            val stored = repo.items(playlistId).map { it.id }
            val wanted = orderedIds.filter { it in stored } + stored.filterNot { it in orderedIds }
            if (wanted != stored) repo.reorder(playlistId, wanted)
            true
        })
    }

    fun move(from: Int, to: Int) = reorder(ItemMoves.move(mutable.value.items.map { it.itemId }, from, to))

    fun consumeMessage() = mutable.update { it.copy(message = null) }

    private fun enqueue(write: (suspend () -> Boolean)?, after: (Boolean) -> Unit = {}) {
        if (write != null) pending++
        scope.launch {
            lock.withLock {
                val ok = write?.let { writeNow(it) } ?: true
                if (write != null) pending--
                if (!ok) mutable.update { it.copy(message = PlaylistMessage.WRITE_FAILED) }
                val snapshot = try {
                    Result.success(readNow())
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.w(e, "Loading playlist %s failed", playlistId)
                    Result.failure(e)
                }
                if (pending == 0) apply(snapshot)
                after(ok)
            }
        }
    }

    private fun apply(snapshot: Result<Loaded?>) {
        val loaded = snapshot.getOrElse {
            mutable.update { s -> s.copy(loading = false, loadFailed = true) }
            return
        }
        mutable.update { s ->
            if (loaded == null) s.copy(loading = false, missing = true)
            else s.copy(loading = false, name = loaded.name, items = loaded.items, loadFailed = false)
        }
    }

    private suspend fun readNow(): Loaded? = withContext(io) {
        val playlist = repo.findById(playlistId) ?: return@withContext null
        Loaded(
            playlist.name,
            repo.items(playlistId).map { item ->
                val ref = HymnRef(item.hymn.hymnType, item.hymn.hymnNo)
                DetailItem(item.id, item.hymn, ref, titleOf(ref))
            },
        )
    }

    private suspend fun writeNow(block: suspend () -> Boolean): Boolean = withContext(NonCancellable + io) {
        try {
            block()
        } catch (e: Exception) {
            Timber.w(e, "A write to playlist %s failed", playlistId)
            false
        }
    }
}
