package org.cog.hymnchtv.ui.lyrics

import org.cog.hymnchtv.notebook.Cancellable
import org.cog.hymnchtv.notebook.NotebookAsync
import org.cog.hymnchtv.notebook.Outcome
import org.cog.hymnchtv.notebook.model.HymnKey

/**
 * Favourite state of the hymn shown on the lyrics page. Main-thread only; every [NotebookAsync] callback
 * arrives on the main thread. The view only displays what this reports.
 */
class FavoriteController(
    private val async: NotebookAsync,
    private val listener: Listener,
) {
    interface Listener {
        fun onState(marked: Boolean, canToggle: Boolean)
        fun onToggled(marked: Boolean)
        fun onError()
    }

    private var current: HymnKey? = null
    private var marked: Boolean? = null
    private var queryCancel: Cancellable? = null
    private var inFlightToggleKey: HymnKey? = null
    private var queryGen = 0
    private var destroyed = false

    /** True when [type]/[no] is the current hymn and it is known to be a favourite. */
    fun isMarked(type: String?, no: Int): Boolean =
        marked == true && current != null && current == HymnKey.ofOrNull(type, no)

    /** Only when the state is known, so the menu text always matches what the action will do. */
    fun canToggle(): Boolean = current != null && marked != null

    fun onHymnChanged(type: String?, no: Int) {
        if (destroyed) return
        val key = HymnKey.ofOrNull(type, no)
        beginNewGeneration()
        current = key
        marked = null
        notifyState()
        if (key != null) query(key)
    }

    private fun query(key: HymnKey) {
        val gen = queryGen
        queryCancel = async.isFavorite(key) { outcome ->
            if (destroyed || gen != queryGen || key != current || inFlightToggleKey == key) return@isFavorite
            queryCancel = null
            val value = (outcome as? Outcome.Ok)?.value ?: return@isFavorite
            marked = value
            notifyState()
        }
    }

    /** Sets exactly the state the menu offers (idempotent), not "the opposite of whatever the row is now". */
    fun toggle() {
        val key = current ?: return
        val target = marked?.not() ?: return
        if (destroyed || inFlightToggleKey == key) return
        beginNewGeneration()
        inFlightToggleKey = key
        // Writes are not cancelled (not even by destroy): a cancelled write could be dropped half-way
        async.setFavorite(key, target) { outcome ->
            if (destroyed) return@setFavorite
            if (inFlightToggleKey == key) inFlightToggleKey = null
            if (key != current) return@setFavorite
            when (outcome) {
                is Outcome.Ok -> {
                    beginNewGeneration()
                    marked = target
                    notifyState()
                    listener.onToggled(target)
                }
                is Outcome.Err -> {
                    listener.onError()
                    beginNewGeneration()
                    query(key)
                }
            }
        }
    }

    /** Cancels only the query; a write already sent still completes, its callback does nothing after this. */
    fun destroy() {
        destroyed = true
        queryCancel?.cancel()
        queryCancel = null
    }

    private fun beginNewGeneration() {
        queryGen++
        queryCancel?.cancel()
        queryCancel = null
    }

    private fun notifyState() = listener.onState(marked == true, canToggle())
}
