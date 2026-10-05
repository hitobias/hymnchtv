package org.cog.hymnchtv.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

/** The media link and the files of one share, prepared on AppExecutors.io and handed to the chooser on the main thread. */
data class SharePayload(val mediaUrl: String?, val files: List<File>)

/** Copies a hymn's score page (when it has one) and lyrics from the assets into a new cacheDir/share/<dir>/. */
object LyricsShareFiles {
    /**
     * @return the score file (if the hymn has a score) followed by the lyrics file, both in a directory of their own
     * @throws IOException for an unknown hymn, missing lyrics, an undecodable score, or a cache that cannot be written
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
            score?.use { files += writePng(shareDir, paths.scoreName, it, paths.score) }
            assets.open(paths.lyrics).use { files += ShareFiles.write(shareDir, paths.lyricsName, it) }
            return files
        } catch (e: IOException) {
            shareDir.deleteRecursively()
            throw e
        }
    }

    /**
     * The score assets are lossless WebP since 1.6.0; receivers still get a PNG (some messengers turn a WebP into a
     * sticker) with the same pixels, compressed straight into the file. A page too large for the heap (API 24: 32 MB) is
     * decoded again as RGB_565; any OutOfMemoryError of the conversion surfaces as an IOException.
     */
    @Throws(IOException::class)
    internal fun writePng(shareDir: File, fileName: String, input: InputStream, name: String): File {
        try {
            val encoded = input.readBytes()
            val bitmap = decode(encoded, Bitmap.Config.ARGB_8888) ?: throw IOException("Cannot decode $name")
            val file = File(shareDir, fileName)
            try {
                FileOutputStream(file).use { out ->
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) throw IOException("Cannot encode $name as PNG")
                }
            } finally {
                bitmap.recycle()
            }
            return file
        } catch (e: OutOfMemoryError) {
            throw IOException("Score page too large to share: $name", e)
        }
    }

    private fun decode(bytes: ByteArray, config: Bitmap.Config): Bitmap? = try {
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inPreferredConfig = config })
    } catch (e: OutOfMemoryError) {
        if (config == Bitmap.Config.RGB_565) throw IOException("Score page too large to share", e)
        decode(bytes, Bitmap.Config.RGB_565)
    }
}
