package org.cog.hymnchtv.ui.lyrics

import org.cog.hymnchtv.notebook.Cancellable
import org.cog.hymnchtv.notebook.NotebookAsync
import org.cog.hymnchtv.notebook.NotebookCallback
import org.cog.hymnchtv.notebook.model.HymnKey

/**
 * Note count of the hymn on the lyrics page (D-1 F1), for the More menu's "Notes (N)" and the title-row mark. Main-thread only,
 * like [FavoriteController]: every [NotebookAsync] callback arrives on the main thread; a result for a hymn the reader has left
 * is dropped.
 */
class NoteCountController(private val query: Query, private val listener: Listener) {
    /** Starts the count of a hymn; the callback arrives on the main thread. Seam for tests; production uses [NotebookAsync]. */
    fun interface Query {
        fun start(key: HymnKey, callback: NotebookCallback<Int>): Cancellable
    }

    constructor(async: NotebookAsync, listener: Listener) : this({ key, callback -> async.noteCount(key, callback) }, listener)

    fun interface Listener {
        /** The count of the current hymn; -1 while it is unknown. */
        fun onCount(count: Int)
    }

    private var current: HymnKey? = null
    private var count: Int? = null
    private var queryCancel: Cancellable? = null
    private var queryGen = 0
    private var destroyed = false

    /** The current hymn; null for numbers the notebook does not store (e.g. the supplement's placeholder 2000). */
    fun currentKey(): HymnKey? = current

    /** The count of [type]/[no] when it is the current hymn and the count is known; -1 otherwise. */
    fun countFor(type: String?, no: Int): Int {
        val known = count ?: return -1
        val key = current ?: return -1
        return if (key == HymnKey.ofOrNull(type, no)) known else -1
    }

    fun onHymnChanged(type: String?, no: Int) {
        if (destroyed) return
        current = HymnKey.ofOrNull(type, no)
        count = null
        listener.onCount(-1)
        query()
    }

    /** Asks again (e.g. back from the notes page); the known count stays until the new one arrives. */
    fun refresh() {
        if (!destroyed) query()
    }

    /** Cancels the query; nothing is reported afterwards. */
    fun destroy() {
        destroyed = true
        cancelQuery()
    }

    private fun query() {
        cancelQuery()
        val key = current ?: return
        val gen = queryGen
        queryCancel = query.start(key) { outcome ->
            if (destroyed || gen != queryGen || key != current) return@start
            queryCancel = null
            val value = outcome.getOrNull() ?: return@start
            count = value
            listener.onCount(value)
        }
    }

    private fun cancelQuery() {
        queryGen++
        queryCancel?.cancel()
        queryCancel = null
    }
}
