package org.cog.hymnchtv.ui.notes

import android.content.Context
import org.cog.hymnchtv.notebook.model.HymnKey
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * The note editor's unsaved text across process death: one UTF-8 file per draft in [dir] (app-private noBackupFilesDir, which
 * the system does not evict like the cache and Auto Backup leaves out), named by a token that is new for every editor session
 * ("<note id or new-<book>-<number>>-<uuid>"), so a late cleanup of a closed session can never hit a newer draft. Only the
 * token goes into the SavedStateHandle, so a long draft never reaches a Bundle (1.4.1 fixed a TransactionTooLargeException).
 * Writes go to a temporary file first, so a half-written draft never replaces a good one. Call off the main thread except
 * where a caller says otherwise.
 */
class NoteDraftFiles(private val dir: File) {
    fun write(token: String, text: String) {
        val target = file(token)
        dir.mkdirs()
        val temp = File(dir, "$token.tmp")
        temp.writeText(text, Charsets.UTF_8)
        if (!temp.renameTo(target)) {
            target.delete()
            if (!temp.renameTo(target)) throw IOException("Cannot store the note draft")
        }
    }

    fun read(token: String): String? = file(token).takeIf { it.isFile }?.readText(Charsets.UTF_8)

    fun delete(token: String) {
        file(token).delete()
    }

    /** Removes drafts nobody saved or discarded (e.g. the app was uninstalled from Recents mid-edit) after [maxAgeMillis]. */
    fun deleteOlderThan(nowMillis: Long, maxAgeMillis: Long) {
        dir.listFiles().orEmpty().filter { it.isFile && nowMillis - it.lastModified() > maxAgeMillis }.forEach { it.delete() }
    }

    private fun file(token: String): File {
        require(TOKEN.matches(token)) { "Not a draft token" }
        return File(dir, "$token.txt")
    }

    companion object {
        private val TOKEN = Regex("[a-z0-9_-]{1,80}")

        /** Leftover drafts older than this are removed at app start. */
        const val MAX_AGE_MS = 7 * 24 * 60 * 60 * 1000L

        /** A fresh token for one editor session of note [noteId] (null: a new note of [key]). */
        fun newToken(noteId: String?, key: HymnKey): String =
            (noteId ?: "new-${key.hymnType}-${key.hymnNo}") + "-" + UUID.randomUUID()

        @JvmStatic
        fun inApp(context: Context) = NoteDraftFiles(File(context.noBackupFilesDir, "note-drafts"))
    }
}
