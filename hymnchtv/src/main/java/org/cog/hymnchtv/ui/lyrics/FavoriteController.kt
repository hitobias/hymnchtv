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
    private var toggleCancels = mutableListOf<Cancellable>()
    private var destroyed = false

    /** True when [type]/[no] is the current hymn and it is known to be a favourite. */
    fun isMarked(type: String?, no: Int): Boolean =
        marked == true && current != null && current == HymnKey.ofOrNull(type, no)

    fun canToggle(): Boolean = current != null

    fun onHymnChanged(type: String?, no: Int) {
        if (destroyed) return
        val key = HymnKey.ofOrNull(type, no)
        beginNewGeneration()
        current = key
        marked = null
        notifyState()
        if (key == null) return
        val gen = queryGen
        queryCancel = async.isFavorite(key) { outcome ->
            if (destroyed || gen != queryGen || key != current) return@isFavorite
            queryCancel = null
            val value = (outcome as? Outcome.Ok)?.value ?: return@isFavorite
            marked = value
            notifyState()
        }
    }

    fun toggle() {
        val key = current ?: return
        if (destroyed || inFlightToggleKey == key) return
        beginNewGeneration()
        inFlightToggleKey = key
        var cancel: Cancellable? = null
        cancel = async.toggleFavorite(key) { outcome ->
            cancel?.let { toggleCancels.remove(it) }
            if (destroyed) return@toggleFavorite
            if (inFlightToggleKey == key) inFlightToggleKey = null
            if (key != current) return@toggleFavorite
            when (outcome) {
                is Outcome.Ok -> {
                    marked = outcome.value
                    notifyState()
                    listener.onToggled(outcome.value)
                }
                is Outcome.Err -> listener.onError()
            }
        }
        toggleCancels.add(cancel)
    }

    fun destroy() {
        destroyed = true
        queryCancel?.cancel()
        queryCancel = null
        toggleCancels.forEach { it.cancel() }
        toggleCancels = mutableListOf()
    }

    private fun beginNewGeneration() {
        queryGen++
        queryCancel?.cancel()
        queryCancel = null
    }

    private fun notifyState() = listener.onState(marked == true, current != null)
}
