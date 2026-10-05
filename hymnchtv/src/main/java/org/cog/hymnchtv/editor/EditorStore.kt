package org.cog.hymnchtv.editor

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest

/**
 * File access of the media links export editor (RichTextEditor). Text is read and written byte for byte as UTF-8, so
 * CSV line breaks survive. Unsaved edits go to a draft file in the cache directory, never into the saved-state Bundle:
 * a large export exceeds the binder limit and crashes the app on API 24+ when it is stopped.
 * A file that is not valid UTF-8 is refused (1.6.0): saving it back as UTF-8 would destroy its text.
 */
object EditorStore {
    private const val DRAFT_PREFIX = "editor_draft_"

    /**
     * Largest file the editor opens. Android 7 has a 32 MB heap: a 2 MB export in the EditText (UTF-16 text, its
     * layout, and with an accessibility service on, a full text copy per change in each accessibility event) ran out
     * of memory. 256 KiB is a few thousand media links; a larger export is refused with a message instead.
     */
    const val MAX_EDIT_BYTES: Long = 256L * 1024

    /**
     * The text for the editor; [fromDraft] if it is an unsaved draft, [draftMissing] if a draft was wanted but is gone,
     * [tooLarge] (and no text) if the file exceeds [MAX_EDIT_BYTES], [notUtf8] (and no text) if it is not UTF-8 text.
     */
    data class Loaded(
        val text: String,
        val fromDraft: Boolean,
        val draftMissing: Boolean = false,
        val tooLarge: Boolean = false,
        val notUtf8: Boolean = false,
    )

    /** The file's bytes are not valid UTF-8. */
    class NotUtf8Exception(name: String) : IOException("$name is not UTF-8 text")

    @JvmStatic
    @Throws(IOException::class)
    fun read(file: File): String {
        val decoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return try {
            decoder.decode(ByteBuffer.wrap(file.readBytes())).toString()
        } catch (e: CharacterCodingException) {
            throw NotUtf8Exception(file.name)
        }
    }

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

    /** The draft of [fileUri] when [preferDraft] and one exists, else the text of [source] if it is not too large. */
    @JvmStatic
    @Throws(IOException::class)
    fun load(source: File, cacheDir: File, fileUri: String, preferDraft: Boolean): Loaded {
        val draft = draftFile(cacheDir, fileUri)
        if (preferDraft && draft.isFile) return Loaded(read(draft), true)
        if (source.length() > MAX_EDIT_BYTES) return Loaded("", fromDraft = false, draftMissing = false, tooLarge = true)
        return try {
            Loaded(read(source), false, preferDraft)
        } catch (e: NotUtf8Exception) {
            Loaded("", fromDraft = false, draftMissing = false, notUtf8 = true)
        }
    }
}
