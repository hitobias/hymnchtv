package org.cog.hymnchtv.persistance

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * Copies a file another app shared with us (ACTION_SEND, or the file picker) into our tmp directory, safely: the
 * sender's display name is reduced to a plain file name (no directories, no ".."), the bytes go to a temporary file in
 * the same directory first, and only a complete copy replaces an older file of that name. Re-sharing a newer export
 * therefore never imports the old content, and a failed copy never destroys it.
 */
object SharedImport {
    /** The shared bytes; null when the sender's stream cannot be opened. */
    fun interface StreamSource {
        @Throws(IOException::class)
        fun open(): InputStream?
    }

    private const val MAX_ALTERNATES = 20

    /** The last path segment of [raw]; null if it is blank, "." or "..", or if [raw] has a ".." segment anywhere. */
    @JvmStatic
    fun safeName(raw: String?): String? {
        if (raw == null) return null
        val segments = raw.split('/', '\\')
        if (segments.any { it.trim() == ".." }) return null
        val name = segments.last().trim()
        if (name.isEmpty() || name == "." || name.contains('\u0000')) return null
        return name
    }

    /**
     * Copies [source] to [dir]/safeName([rawName]), replacing an older file only after the copy is complete.
     * @return the written file, or null if the name is unsafe, the stream cannot be opened or the copy fails
     */
    @JvmStatic
    fun copyInto(dir: File, rawName: String?, source: StreamSource): File? {
        val name = safeName(rawName) ?: return null
        val temp = try {
            File.createTempFile(".import-", ".part", dir)
        } catch (e: IOException) {
            return null
        }
        val copied = try {
            source.open()?.use { input -> FileOutputStream(temp).use { out -> input.copyTo(out) } } != null
        } catch (e: IOException) {
            false
        } catch (e: SecurityException) {
            false
        }
        if (!copied) {
            temp.delete()
            return null
        }
        // rename replaces an older file we own in one step; on API 30+ a file left by an earlier install of the app
        // may refuse that, then the copy keeps a free alternate name instead
        val target = File(dir, name)
        if (temp.renameTo(target)) return target
        val alternate = freeName(dir, name)
        if (alternate != null && temp.renameTo(alternate)) return alternate
        temp.delete()
        return null
    }

    /** The first of [dir]/[name], "base-2.ext", "base-3.ext"... that does not exist; null if none is free. */
    internal fun freeName(dir: File, name: String): File? {
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val ext = if (dot > 0) name.substring(dot) else ""
        for (i in 1..MAX_ALTERNATES) {
            val candidate = File(dir, if (i == 1) name else "$base-$i$ext")
            if (!candidate.exists()) return candidate
        }
        return null
    }
}
