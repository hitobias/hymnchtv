package org.cog.hymnchtv.reading.background

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.exifinterface.media.ExifInterface
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors

/**
 * Copies a photo chosen with the system photo picker into app-private storage (plan A2, task S3): decoded no larger
 * than twice the screen, EXIF orientation applied, saved as JPEG through a temp file and an atomic rename.
 * Nothing is cropped; the background is shown centerCrop with dim and blur.
 */
object PhotoBackgroundImporter {
    const val DIR_NAME = "backgrounds"
    const val FILE_NAME = "photo.jpg"
    const val TEMP_NAME = "photo.tmp"
    const val JPEG_QUALITY = 85

    /** Rotation in degrees (clockwise) applied first, then a horizontal mirror if [flip]. */
    data class Transform(val degrees: Int, val flip: Boolean) {
        val isIdentity: Boolean get() = degrees == 0 && !flip
    }

    private val worker = Executors.newSingleThreadExecutor { r -> Thread(r, "photo-import").apply { isDaemon = true } }
    private val main by lazy { Handler(Looper.getMainLooper()) }

    /** The one stored photo; every reader (BackgroundPrefs, picker) uses this path. */
    @JvmStatic
    fun photoFileIn(filesDir: File): File = File(File(filesDir, DIR_NAME), FILE_NAME)

    @JvmStatic
    fun tempFileIn(filesDir: File): File = File(File(filesDir, DIR_NAME), TEMP_NAME)

    /** Maps an EXIF orientation tag to the transform that makes the pixels upright. */
    @JvmStatic
    fun orientationTransform(exifOrientation: Int): Transform = when (exifOrientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> Transform(90, false)
        ExifInterface.ORIENTATION_ROTATE_180 -> Transform(180, false)
        ExifInterface.ORIENTATION_ROTATE_270 -> Transform(270, false)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> Transform(0, true)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> Transform(180, true)
        ExifInterface.ORIENTATION_TRANSPOSE -> Transform(90, true)
        ExifInterface.ORIENTATION_TRANSVERSE -> Transform(270, true)
        else -> Transform(0, false)
    }

    /** Imports [uri] off the main thread; [onDone] runs on the main thread with true when the photo file was replaced. */
    @JvmStatic
    fun importAsync(context: Context, uri: Uri, onDone: (Boolean) -> Unit) {
        val app = context.applicationContext
        worker.execute {
            val ok = import(app, uri)
            main.post { onDone(ok) }
        }
    }

    /** Blocking import; on any failure the previously stored photo is left untouched. */
    @JvmStatic
    fun import(context: Context, uri: Uri): Boolean {
        val filesDir = context.filesDir
        val temp = tempFileIn(filesDir)
        return try {
            val metrics = context.resources.displayMetrics
            val bitmap = decode(context, uri, metrics.widthPixels, metrics.heightPixels)
            val upright = try {
                applyTransform(bitmap, orientationTransform(readOrientation(context, uri)))
            } finally {
                if (!bitmap.isRecycled) bitmap.recycle()
            }
            try {
                temp.parentFile?.mkdirs()
                temp.outputStream().use { check(upright.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it)) { "JPEG encode failed" } }
            } finally {
                upright.recycle()
            }
            check(temp.renameTo(photoFileIn(filesDir))) { "Cannot move ${temp.path} into place" }
            true
        } catch (e: IOException) {
            fail(temp, e)
        } catch (e: SecurityException) {
            fail(temp, e)
        } catch (e: IllegalStateException) {
            fail(temp, e)
        } catch (e: OutOfMemoryError) {
            fail(temp, e)
        }
    }

    private fun fail(temp: File, e: Throwable): Boolean {
        Timber.w(e, "Background photo import failed")
        temp.delete()
        return false
    }

    private fun decode(context: Context, uri: Uri, screenW: Int, screenH: Int): Bitmap {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        (resolver.openInputStream(uri) ?: throw IOException("Cannot open $uri")).use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Not an image: $uri")
        val options = BitmapFactory.Options().apply {
            inSampleSize = PhotoBackground.boundedSampleSize(bounds.outWidth, bounds.outHeight, screenW, screenH)
        }
        return (resolver.openInputStream(uri) ?: throw IOException("Cannot open $uri")).use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IOException("Cannot decode $uri")
    }

    private fun readOrientation(context: Context, uri: Uri): Int = try {
        context.contentResolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
    } catch (e: IOException) {
        Timber.d(e, "No EXIF orientation")
        ExifInterface.ORIENTATION_NORMAL
    }

    private fun applyTransform(source: Bitmap, transform: Transform): Bitmap {
        if (transform.isIdentity) return source
        val matrix = Matrix().apply {
            postRotate(transform.degrees.toFloat())
            if (transform.flip) postScale(-1f, 1f)
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }
}
