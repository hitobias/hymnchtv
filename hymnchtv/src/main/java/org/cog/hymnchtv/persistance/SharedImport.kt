package org.cog.hymnchtv.persistance

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * Copies a file another app shared with us (ACTION_SEND, or the file picker) into our tmp directory, safely: the
 * sender's display name is reduced to a plain file name (no directories, no "..", no control characters, at most
 * [MAX_NAME_BYTES] UTF-8 bytes with its extension kept, 1.6.0), the bytes go to a temporary file in
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

    /** Longest file name kept, in UTF-8 bytes (file systems allow 255; room is left for freeName's "-20"). */
    const val MAX_NAME_BYTES = 200

    /** A longer "extension" is just part of the name. */
    private const val MAX_EXTENSION_BYTES = 16

    /**
     * The last path segment of [raw] without control characters, at most [MAX_NAME_BYTES] UTF-8 bytes; null if it is blank,
     * "." or "..", or if [raw] has a ".." segment anywhere.
     */
    @JvmStatic
    fun safeName(raw: String?): String? {
        if (raw == null) return null
        val segments = raw.split('/', '\\')
        if (segments.any { it.trim() == ".." }) return null
        val name = stripControls(segments.last()).trim()
        if (name.isEmpty() || name == "." || name == "..") return null
        return capUtf8(name, MAX_NAME_BYTES)
    }

    /** C0 and C1 control characters and DEL (U+0000 included) removed. */
    internal fun stripControls(text: String): String = text.filterNot { it.code < 0x20 || it.code in 0x7F..0x9F }

    /** [name] cut to [maxBytes] UTF-8 bytes at a code point boundary; the extension (last ".xxx", <= 16 bytes) is kept. */
    internal fun capUtf8(name: String, maxBytes: Int): String {
        if (utf8Size(name) <= maxBytes) return name
        val dot = name.lastIndexOf('.')
        val extension = if (dot > 0 && utf8Size(name.substring(dot)) <= MAX_EXTENSION_BYTES) name.substring(dot) else ""
        val base = if (extension.isEmpty()) name else name.substring(0, dot)
        val budget = maxBytes - utf8Size(extension)
        val cut = StringBuilder()
        var used = 0
        var i = 0
        while (i < base.length) {
            val codePoint = base.codePointAt(i)
            val bytes = utf8Size(String(Character.toChars(codePoint)))
            if (used + bytes > budget) break
            cut.appendCodePoint(codePoint)
            used += bytes
            i += Character.charCount(codePoint)
        }
        return cut.toString().trimEnd() + extension
    }

    private fun utf8Size(text: String): Int = text.toByteArray(Charsets.UTF_8).size

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
        var moved = false
        try {
            val copied = try {
                source.open()?.use { input -> FileOutputStream(temp).use { out -> input.copyTo(out) } } != null
            } catch (e: IOException) {
                false
            } catch (e: RuntimeException) { // SecurityException, IllegalStateException... from a foreign provider
                false
            }
            if (!copied) return null
            // rename replaces an older file we own in one step; on API 30+ a file left by an earlier install of the
            // app may refuse that, then the copy keeps a free alternate name instead
            val target = File(dir, name)
            if (temp.renameTo(target)) {
                moved = true
                return target
            }
            val alternate = freeName(dir, name)
            if (alternate != null && temp.renameTo(alternate)) {
                moved = true
                return alternate
            }
            return null
        } finally {
            if (!moved) temp.delete()
        }
    }

    const val PART_MAX_AGE_MS = 60 * 60 * 1000L

    /** Deletes ".import-*.part" files in [dir] older than [maxAgeMs] (left by a process killed mid-copy). */
    @JvmStatic
    fun cleanStaleParts(dir: File, nowMillis: Long, maxAgeMs: Long): Int =
        dir.listFiles().orEmpty().count {
            it.name.startsWith(".import-") && it.name.endsWith(".part") && it.lastModified() < nowMillis - maxAgeMs && it.delete()
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
