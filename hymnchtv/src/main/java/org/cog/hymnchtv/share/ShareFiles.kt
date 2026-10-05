package org.cog.hymnchtv.share

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * Files handed to other apps by "share" live in cacheDir/share/<one directory per share>/ (FileProvider cache-path
 * "share"): private, never owned by an earlier install (the old Download/hymnal/tmp failed with EACCES/EEXIST on API
 * 30+). A receiver may open its uri long after the chooser (ShareWith opens the file chooser seconds after the text
 * one), so a share never reuses another share's directory and only shares older than [MAX_AGE_MS] are cleaned.
 */
object ShareFiles {
    const val DIR = "share"
    const val MAX_AGE_MS = 24 * 60 * 60 * 1000L
    private const val MAX_SAME_MILLI = 1000

    @JvmStatic
    fun dir(cacheDir: File): File = File(cacheDir, DIR)

    /** Creates a new, empty cacheDir/share/<nowMillis>[-n]/ for one share. */
    @JvmStatic
    @Throws(IOException::class)
    fun newShareDir(cacheDir: File, nowMillis: Long): File {
        val root = dir(cacheDir)
        if (!root.isDirectory && !root.mkdirs()) throw IOException("Cannot create $root")
        for (i in 0 until MAX_SAME_MILLI) {
            val candidate = File(root, if (i == 0) "$nowMillis" else "$nowMillis-$i")
            if (candidate.mkdir()) return candidate
        }
        throw IOException("No free share directory under $root")
    }

    /** Writes [input] to [shareDir]/[name]; [name] must be a plain file name. */
    @JvmStatic
    @Throws(IOException::class)
    fun write(shareDir: File, name: String, input: InputStream): File {
        require(name.isNotEmpty() && name != "." && name != ".." && !name.contains('/') && !name.contains('\\')) {
            "Not a plain file name: $name"
        }
        val file = File(shareDir, name)
        if (file.exists() && !file.delete()) throw IOException("Cannot replace $file")
        FileOutputStream(file).use { input.copyTo(it) }
        return file
    }

    /**
     * Deletes the shares (and any stray files) in cacheDir/share/ last modified more than [maxAgeMs] before
     * [nowMillis]. @return the number of entries deleted
     */
    @JvmStatic
    fun cleanOlderThan(cacheDir: File, nowMillis: Long, maxAgeMs: Long): Int =
        dir(cacheDir).listFiles().orEmpty().count { it.lastModified() < nowMillis - maxAgeMs && it.deleteRecursively() }
}
