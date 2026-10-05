package org.cog.hymnchtv.share

import android.content.Context
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/** The media link and the files of one share, prepared on AppExecutors.io and handed to the chooser on the main thread. */
data class SharePayload(val mediaUrl: String?, val files: List<File>)

/** Copies a hymn's score page (when it has one) and lyrics from the assets into a new cacheDir/share/<dir>/. */
object LyricsShareFiles {
    /**
     * @return the score file (if the hymn has a score) followed by the lyrics file, both in a directory of their own
     * @throws IOException for an unknown hymn, missing lyrics, or a cache that cannot be written
     */
    @JvmStatic
    @Throws(IOException::class)
    fun prepare(context: Context, hymnType: String?, hymnNo: Int): List<File> {
        val paths = ShareAssets.paths(hymnType, hymnNo) ?: throw IOException("Nothing to share for $hymnType $hymnNo")
        val assets = context.assets
        val shareDir = ShareFiles.newShareDir(context.cacheDir, System.currentTimeMillis())
        try {
            val files = ArrayList<File>(2)
            // most youth hymns have lyrics only; only a missing asset is tolerated, a cache write error still throws
            val score = try {
                assets.open(paths.score)
            } catch (e: FileNotFoundException) {
                null
            }
            score?.use { files += ShareFiles.write(shareDir, paths.scoreName, it) }
            assets.open(paths.lyrics).use { files += ShareFiles.write(shareDir, paths.lyricsName, it) }
            return files
        } catch (e: IOException) {
            shareDir.deleteRecursively()
            throw e
        }
    }
}
