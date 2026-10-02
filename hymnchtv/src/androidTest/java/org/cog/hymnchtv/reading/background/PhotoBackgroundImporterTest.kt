package org.cog.hymnchtv.reading.background

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Compile-checked in the parallel lanes; run on API 24 and 34 in Task V1. */
@RunWith(AndroidJUnit4::class)
class PhotoBackgroundImporterTest {
    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    private val stored = PhotoBackgroundImporter.photoFileIn(ctx.filesDir)

    @After
    fun cleanUp() {
        stored.delete()
        PhotoBackgroundImporter.tempFileIn(ctx.filesDir).delete()
    }

    private fun source(name: String, write: (File) -> Unit): Uri = Uri.fromFile(File(ctx.cacheDir, name).also(write))

    @Test
    fun importWritesADecodableJpegAndLeavesNoTempFile() {
        val uri = source("import_src.png") { f ->
            val bmp = Bitmap.createBitmap(300, 200, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
            f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        assertThat(PhotoBackgroundImporter.import(ctx, uri)).isTrue()
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(stored.path, bounds)
        assertThat(bounds.outWidth).isEqualTo(300)
        assertThat(bounds.outHeight).isEqualTo(200)
        assertThat(PhotoBackgroundImporter.tempFileIn(ctx.filesDir).exists()).isFalse()
    }

    @Test
    fun failedImportKeepsThePreviousPhoto() {
        stored.parentFile?.mkdirs()
        stored.writeBytes(byteArrayOf(1, 2, 3))
        val garbage = source("import_bad.png") { it.writeText("not an image") }
        assertThat(PhotoBackgroundImporter.import(ctx, garbage)).isFalse()
        assertThat(stored.readBytes()).isEqualTo(byteArrayOf(1, 2, 3))
        assertThat(PhotoBackgroundImporter.tempFileIn(ctx.filesDir).exists()).isFalse()
    }
}
