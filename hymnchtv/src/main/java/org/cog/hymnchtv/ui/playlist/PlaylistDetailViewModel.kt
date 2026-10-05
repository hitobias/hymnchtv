package org.cog.hymnchtv.ui.playlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import org.cog.hymnchtv.hymn.HymnRef
import org.cog.hymnchtv.lyrics.LyricsScript
import org.cog.hymnchtv.notebook.Notebook
import org.cog.hymnchtv.ui.notebook.NotebookPages
import org.cog.hymnchtv.ui.titles.AssetHymnTitles

/** The playlist page's store; the item opened last is kept in [handle], so "Next" survives process death. */
class PlaylistDetailViewModel(app: Application, private val handle: SavedStateHandle) : AndroidViewModel(app) {
    private val graph = Notebook.get(app)

    val store: PlaylistDetailStore? = handle.get<String>(NotebookPages.ARG_PLAYLIST_ID)?.let { id ->
        PlaylistDetailStore(id, graph.playlists, titleLookup(app), viewModelScope, Dispatchers.IO, handle.get<String>(KEY_LAST_OPENED))
    }

    init {
        store?.load()
    }

    fun markOpened(itemId: String) {
        handle[KEY_LAST_OPENED] = itemId
        store?.markOpened(itemId)
    }

    private companion object {
        const val KEY_LAST_OPENED = "last_opened_item"

        /** The title source reads the lyrics script once, on first use (on the store's IO thread). */
        fun titleLookup(app: Application): suspend (HymnRef) -> String? {
            val titles by lazy { AssetHymnTitles.from(app, LyricsScript.hantVariant(app)) }
            return { ref -> runCatching { titles.lookup(ref.book, ref.storedNo) }.getOrNull() }
        }
    }
}
