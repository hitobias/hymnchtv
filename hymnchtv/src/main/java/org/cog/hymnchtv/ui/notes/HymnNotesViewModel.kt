package org.cog.hymnchtv.ui.notes

import android.app.Application
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cog.hymnchtv.lyrics.LyricsScript
import org.cog.hymnchtv.notebook.Notebook
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.ui.notebook.NotebookPages
import org.cog.hymnchtv.ui.titles.AssetHymnTitles
import timber.log.Timber

/**
 * The notes page's state: the store (lives across rotation), the hymn title, and the open editor with its draft. The hymn comes
 * from the fragment arguments (copied into [handle] by the default factory). The draft text is never put into [handle]: when
 * the state is saved it goes to a private file ([NoteDraftFiles]) and only its token is kept, so a draft of any length survives
 * process death without a large Bundle.
 */
class HymnNotesViewModel(app: Application, private val handle: SavedStateHandle) : AndroidViewModel(app) {
    private val drafts = NoteDraftFiles.inApp(app)

    val key: HymnKey? = HymnKey.ofOrNull(handle.get<String>(NotebookPages.ARG_HYMN_TYPE), handle.get<Int>(NotebookPages.ARG_HYMN_NO) ?: -1)

    private val graph = Notebook.get(app)
    val store: HymnNotesStore? = key?.let { HymnNotesStore(it, graph.notes, graph.singLogs, viewModelScope, Dispatchers.IO) }

    private val mutableTitle = MutableStateFlow<String?>(null)
    val title: StateFlow<String?> = mutableTitle.asStateFlow()

    /** The editor's text while it is open; null when closed or not typed into yet. */
    var draft: String? = null

    init {
        // After process death the token survived in the handle and the text in its file: read it back once. This one small
        // file read happens on the main thread, only on this restore path, so the editor can be rebuilt with its text at once.
        handle.get<String>(KEY_DRAFT_TOKEN)?.let { token ->
            draft = try {
                drafts.read(token)
            } catch (e: Exception) {
                Timber.w(e, "The note draft could not be read back")
                null
            }
        }
        // Called while the state is being saved (main thread): the text goes to its file, the Bundle gets nothing big
        handle.setSavedStateProvider(KEY_DRAFT_FLUSH) {
            persistDraftNow()
            Bundle()
        }
        store?.load()
        key?.let { k ->
            viewModelScope.launch {
                mutableTitle.value = withContext(Dispatchers.IO) {
                    try {
                        AssetHymnTitles.from(app, LyricsScript.hantVariant(app)).lookup(k.hymnType, k.hymnNo)
                    } catch (e: Exception) {
                        Timber.w(e, "No title for %s", k)
                        null
                    }
                }
            }
        }
    }

    /** The note the editor is open for: null while closed, [NEW_NOTE] for a new one. */
    val editing: String? get() = handle.get<String>(KEY_EDITING)

    fun openEditor(noteId: String?) {
        val k = key ?: return
        handle[KEY_EDITING] = noteId ?: NEW_NOTE
        handle[KEY_DRAFT_TOKEN] = NoteDraftFiles.newToken(noteId, k) // new per session: an old cleanup cannot reach it
        draft = null
    }

    /** False until the note list has loaded: right after process death [originalOf] cannot answer yet. */
    fun originalKnown(): Boolean = store?.state?.value?.loading == false

    /** The stored text of note [id]; null for a new note or before the list has loaded. */
    fun originalOf(id: String?): String? = id?.let { wanted -> store?.state?.value?.rows?.firstOrNull { it.id == wanted }?.body }

    /** Saved, discarded or deleted: the editor closes and its draft file goes (only this session's token). */
    fun closeEditor() {
        val token = handle.get<String>(KEY_DRAFT_TOKEN)
        handle[KEY_EDITING] = null
        handle[KEY_DRAFT_TOKEN] = null
        draft = null
        if (token != null) {
            viewModelScope.launch(Dispatchers.IO + NonCancellable) {
                try {
                    drafts.delete(token)
                } catch (e: Exception) {
                    Timber.w(e, "The note draft file could not be removed")
                }
            }
        }
    }

    /** Writes the open editor's text to its file (the saved-state provider calls it; tests call it directly). */
    @VisibleForTesting
    fun persistDraftNow() {
        val token = handle.get<String>(KEY_DRAFT_TOKEN) ?: return
        val text = draft ?: return
        try {
            drafts.write(token, text)
        } catch (e: Exception) {
            Timber.w(e, "The note draft could not be kept")
        }
    }

    companion object {
        const val NEW_NOTE = "new"

        @VisibleForTesting const val KEY_EDITING = "editing_note"

        @VisibleForTesting const val KEY_DRAFT_TOKEN = "editing_draft_token"

        private const val KEY_DRAFT_FLUSH = "editing_draft_flush"
    }
}
