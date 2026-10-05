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
     * @return [files] in order, each either itself (under Download/hymnal/) or its copy in one new share directory
     * @throws IOException when a copy cannot be made
     */
    @JvmStatic
    @Throws(IOException::class)
    fun prepare(cacheDir: File, externalStorage: File, files: List<File>, nowMillis: Long): List<File> {
        var shareDir: File? = null
        return files.map { file ->
            if (isUnderRoot(file, externalStorage)) {
                file
            } else {
                val dir = shareDir ?: ShareFiles.newShareDir(cacheDir, nowMillis).also { shareDir = it }
                FileInputStream(file).use { ShareFiles.write(dir, file.name, it) }
            }
        }
    }
}
