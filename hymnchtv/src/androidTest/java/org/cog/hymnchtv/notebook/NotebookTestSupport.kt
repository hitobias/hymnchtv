package org.cog.hymnchtv.notebook

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.cog.hymnchtv.notebook.data.entity.NoteEntity
import org.cog.hymnchtv.notebook.data.entity.PlaylistEntity
import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.Occasion
import org.cog.hymnchtv.notebook.model.SingSource
import org.cog.hymnchtv.notebook.settings.NotebookPrefs

/**
 * Shared helpers for instrumented notebook tests (notes, playlists, sing logs). Everything goes through the repositories (no raw
 * table access) and must run on the instrumentation thread, never inside runOnMainSync.
 */
object NotebookTestSupport {
    private fun graph(): NotebookGraph {
        check(Looper.myLooper() != Looper.getMainLooper()) { "NotebookTestSupport must not run on the main thread" }
        return Notebook.get(ApplicationProvider.getApplicationContext())
    }

    fun resetNotes(vararg keys: HymnKey) = runBlocking {
        val g = graph()
        keys.forEach { key -> g.notes.findByHymn(key).forEach { g.notes.delete(it.id) } }
    }

    fun addNote(key: HymnKey, body: String): NoteEntity = runBlocking { graph().notes.add(key, body) }

    fun notes(key: HymnKey): List<NoteEntity> = runBlocking { graph().notes.findByHymn(key) }

    fun resetPlaylists() = runBlocking {
        val g = graph()
        g.playlists.findAll().forEach { g.playlists.delete(it.id) }
    }

    fun playlist(name: String, vararg keys: HymnKey): PlaylistEntity = runBlocking {
        val g = graph()
        val created = g.playlists.createPlaylist(name)
        keys.forEach { g.playlists.addItem(created.id, it) }
        created
    }

    fun playlists(): List<PlaylistEntity> = runBlocking { graph().playlists.findAll() }

    fun items(playlistId: String): List<HymnKey> = runBlocking { graph().playlists.items(playlistId).map { it.hymn } }

    fun resetSingLogs(vararg keys: HymnKey) = runBlocking {
        val g = graph()
        keys.forEach { key -> g.singLogs.findByHymn(key).forEach { g.singLogs.delete(it.id) } }
    }

    fun recordSing(key: HymnKey, sungAt: Long): SingLogEntity =
        runBlocking { graph().singLogs.record(key, sungAt, Occasion.HOME, SingSource.MANUAL) }

    fun singLogs(key: HymnKey): List<SingLogEntity> = runBlocking { graph().singLogs.findByHymn(key) }

    /** Back to the production default (no stored value); commit() so the next read sees it. */
    fun resetAutoRecord() {
        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences(NotebookPrefs.FILE_NAME, Context.MODE_PRIVATE).edit()
            .remove(NotebookPrefs.KEY_AUTO_RECORD).commit()
    }

    fun setAutoRecord(enabled: Boolean) {
        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences(NotebookPrefs.FILE_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(NotebookPrefs.KEY_AUTO_RECORD, enabled).commit()
    }
}
