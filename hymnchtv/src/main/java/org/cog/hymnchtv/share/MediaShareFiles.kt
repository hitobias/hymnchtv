package org.cog.hymnchtv.share

import org.cog.hymnchtv.persistance.PathTrust
import java.io.File
import java.io.FileInputStream
import java.io.IOException

/**
 * Media config "share" (1.6.0): the import file and the media file go out through the FileProvider, whose only external
 * root is Download/hymnal/ (file_paths.xml). A file outside it (another folder, an SD card) is copied into a new
 * cacheDir/share/<dir>/ first; ShareFiles removes it after a day.
 */
object MediaShareFiles {
    /** Download/hymnal relative to the external storage root: <external-path name="hymnal"> in file_paths.xml. */
    const val HYMNAL_ROOT = "Download/hymnal/"

    @JvmStatic
    fun isUnderRoot(file: File, externalStorage: File): Boolean = PathTrust.isUnder(file.path, File(externalStorage, HYMNAL_ROOT))

    /**
     * Every file is canonicalised first (symlinks and "../" resolved). Only a regular file whose canonical path is on one of
     * the [volumeRoots] (the primary external storage first, then any SD card) and outside every [forbidden] directory (the app's
     * private data, caches and external app dirs) is shared: media records come from imported files, so their paths are
     * untrusted. A refused file is skipped, not fatal: it goes to [Prepared.refused] and the others are still prepared.
     * @throws IOException when a copy cannot be made; nothing is left in the share cache then
     */
    @JvmStatic
    @Throws(IOException::class)
    fun prepare(cacheDir: File, volumeRoots: List<File>, forbidden: List<File>, files: List<File>, nowMillis: Long): Prepared {
        val ready = ArrayList<File>()
        val refused = ArrayList<File>()
        for (file in files) {
            val canonical = checked(file, volumeRoots, forbidden)
            if (canonical == null) refused += file else ready += canonical
        }
        var shareDir: File? = null
        try {
            val shared = ready.map { file ->
                if (volumeRoots.firstOrNull()?.let { isUnderRoot(file, it) } == true) { // the FileProvider serves the primary volume only
                    file
                } else {
                    val dir = shareDir ?: ShareFiles.newShareDir(cacheDir, nowMillis).also { shareDir = it }
                    FileInputStream(file).use { ShareFiles.write(dir, file.name, it) }
                }
            }
            return Prepared(shared, refused)
        } catch (e: Exception) { // IOException, or IllegalArgumentException for a name ShareFiles refuses
            shareDir?.deleteRecursively()
            throw if (e is IOException) e else IOException("Cannot prepare the share", e)
        }
    }

    /** [files] are ready for the FileProvider; [refused] were not shareable (private, outside storage, missing, not a file). */
    data class Prepared(val files: List<File>, val refused: List<File>)

    /** The storage volume roots (primary and SD cards) from the app-specific external dirs ".../Android/data/<pkg>/files". */
    @JvmStatic
    fun volumeRoots(externalFilesDirs: List<File?>): List<File> =
        externalFilesDirs.filterNotNull().mapNotNull { dir ->
            val marker = dir.path.indexOf("/Android/data/")
            if (marker > 0) File(dir.path.substring(0, marker)) else null
        }.distinct()

    private fun checked(file: File, volumeRoots: List<File>, forbidden: List<File>): File? {
        val canonical = try {
            file.canonicalFile
        } catch (e: IOException) {
            return null
        }
        if (!canonical.isFile) return null
        return canonical.takeIf { PathTrust.isAcceptable(it.path, forbidden, volumeRoots) }
    }
}
