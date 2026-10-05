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
     * Every file is canonicalised first (symlinks and "../" resolved). Only a regular file whose canonical path is on
     * [externalStorage] and outside every [forbidden] directory (the app's private data, caches and external app dirs)
     * is shared: media records come from imported files, so their paths are untrusted.
     * @return the (canonical) files in order, each either itself (under Download/hymnal/) or its copy in one new share directory
     * @throws IOException when a file is refused or a copy cannot be made; nothing is left in the share cache then
     */
    @JvmStatic
    @Throws(IOException::class)
    fun prepare(cacheDir: File, externalStorage: File, forbidden: List<File>, files: List<File>, nowMillis: Long): List<File> {
        val checked = files.map { checked(it, externalStorage, forbidden) }
        var shareDir: File? = null
        try {
            return checked.map { file ->
                if (isUnderRoot(file, externalStorage)) {
                    file
                } else {
                    val dir = shareDir ?: ShareFiles.newShareDir(cacheDir, nowMillis).also { shareDir = it }
                    FileInputStream(file).use { ShareFiles.write(dir, file.name, it) }
                }
            }
        } catch (e: IOException) {
            shareDir?.deleteRecursively()
            throw e
        }
    }

    private fun checked(file: File, externalStorage: File, forbidden: List<File>): File {
        val canonical = try {
            file.canonicalFile
        } catch (e: IOException) {
            throw IOException("Cannot resolve ${file.name}", e)
        }
        if (!canonical.isFile) throw IOException("Not a regular file: ${file.name}")
        if (!PathTrust.isAcceptable(canonical.path, forbidden, listOf(externalStorage))) {
            throw IOException("Not shareable: ${file.name}")
        }
        return canonical
    }
}
