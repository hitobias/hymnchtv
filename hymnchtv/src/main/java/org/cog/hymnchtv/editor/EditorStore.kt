package org.cog.hymnchtv.editor

import java.io.File
import java.io.IOException

/**
 * File access of the media links export editor (RichTextEditor). Text is read and written byte for byte as UTF-8, so
 * CSV line breaks survive. Unsaved edits go to a draft file in the cache directory, never into the saved-state Bundle:
 * a large export exceeds the binder limit and crashes the app on API 24+ when it is stopped.
 */
object EditorStore {
    const val DRAFT_NAME = "editor_draft.txt"

    /** The text for the editor, and whether it is an unsaved draft rather than the file itself. */
    data class Loaded(val text: String, val fromDraft: Boolean)

    @JvmStatic
    @Throws(IOException::class)
    fun read(file: File): String = String(file.readBytes(), Charsets.UTF_8)

    @JvmStatic
    @Throws(IOException::class)
    fun write(file: File, text: String) {
        file.writeBytes(text.toByteArray(Charsets.UTF_8))
    }

    @JvmStatic
    fun draftFile(cacheDir: File): File = File(cacheDir, DRAFT_NAME)

    @JvmStatic
    @Throws(IOException::class)
    fun saveDraft(cacheDir: File, text: String) {
        write(draftFile(cacheDir), text)
    }

    @JvmStatic
    fun clearDraft(cacheDir: File) {
        draftFile(cacheDir).delete()
    }

    /** The draft when [preferDraft] and one exists, else the text of [source]. */
    @JvmStatic
    @Throws(IOException::class)
    fun load(source: File, cacheDir: File, preferDraft: Boolean): Loaded {
        val draft = draftFile(cacheDir)
        return if (preferDraft && draft.isFile) Loaded(read(draft), true) else Loaded(read(source), false)
    }
}
