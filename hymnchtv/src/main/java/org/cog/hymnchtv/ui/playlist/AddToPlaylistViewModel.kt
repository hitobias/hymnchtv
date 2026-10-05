package org.cog.hymnchtv.ui.playlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import org.cog.hymnchtv.notebook.Notebook
import org.cog.hymnchtv.notebook.model.HymnKey

/** The hymn to add (from the dialog arguments), the playlists to offer, and the writer that outlives the dialog. */
class AddToPlaylistViewModel(app: Application, handle: SavedStateHandle) : AndroidViewModel(app) {
    private val graph = Notebook.get(app)

    val key: HymnKey? = HymnKey.ofOrNull(handle.get<String>(AddToPlaylistDialog.ARG_TYPE), handle.get<Int>(AddToPlaylistDialog.ARG_NO) ?: -1)
    val store = PlaylistsStore(graph.playlists, viewModelScope, Dispatchers.IO)
    val adder = PlaylistAdder(graph.playlists, graph.appScope, Dispatchers.IO, Dispatchers.Main)

    init {
        store.load()
    }
}
