package org.cog.hymnchtv.editor

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest

/**
 * File access of the media links export editor (RichTextEditor). Text is read and written byte for byte as UTF-8, so
 * CSV line breaks survive. Unsaved edits go to a draft file in the cache directory, never into the saved-state Bundle:
 * a large export exceeds the binder limit and crashes the app on API 24+ when it is stopped.
 */
object EditorStore {
    private const val DRAFT_PREFIX = "editor_draft_"

    /** The text for the editor; [fromDraft] if it is an unsaved draft, [draftMissing] if a draft was wanted but is gone. */
    data class Loaded(val text: String, val fromDraft: Boolean, val draftMissing: Boolean = false)

    @JvmStatic
    @Throws(IOException::class)
    fun read(file: File): String = String(file.readBytes(), Charsets.UTF_8)

    /**
     * Writes to a temp file next to [file], syncs it, then renames it over [file]: a failure at any point leaves the
     * original untouched and no temp file behind.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun write(file: File, text: String) {
        val temp = File.createTempFile(".edit-", ".part", file.absoluteFile.parentFile)
        try {
            FileOutputStream(temp).use { out ->
                out.write(text.toByteArray(Charsets.UTF_8))
                out.flush()
                out.fd.sync()
            }
            if (!temp.renameTo(file)) throw IOException("Could not replace " + file.name)
        } finally {
            if (temp.exists()) temp.delete()
        }
    }

    /** One draft per edited file ([fileUri]), so two editors never share or delete each other's draft. */
    @JvmStatic
    fun draftFile(cacheDir: File, fileUri: String): File {
        val hash = MessageDigest.getInstance("SHA-256").digest(fileUri.toByteArray(Charsets.UTF_8))
        return File(cacheDir, DRAFT_PREFIX + hash.joinToString("") { "%02x".format(it) } + ".txt")
    }

    @JvmStatic
    @Throws(IOException::class)
    fun saveDraft(cacheDir: File, fileUri: String, text: String) {
        write(draftFile(cacheDir, fileUri), text)
    }

    @JvmStatic
    fun clearDraft(cacheDir: File, fileUri: String) {
        draftFile(cacheDir, fileUri).delete()
    }

    /** The draft of [fileUri] when [preferDraft] and one exists, else the text of [source]. */
    @JvmStatic
    @Throws(IOException::class)
    fun load(source: File, cacheDir: File, fileUri: String, preferDraft: Boolean): Loaded {
        val draft = draftFile(cacheDir, fileUri)
        if (preferDraft && draft.isFile) return Loaded(read(draft), true)
        return Loaded(read(source), false, preferDraft)
    }
}
