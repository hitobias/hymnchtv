package org.cog.hymnchtv

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** A finished download whose file cannot be resolved is reported as failed instead of crashing the receiver. */
@RunWith(AndroidJUnit4::class)
class MediaDownloadedFileTest {
    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun aNullUriGivesNoFile() {
        assertThat(MediaDownloadHandler.downloadedFile(ctx, null)).isNull()
    }

    @Test
    fun aMissingFileGivesNoFile() {
        val missing = File(ctx.cacheDir, "no-such-download.mp3").apply { delete() }
        assertThat(MediaDownloadHandler.downloadedFile(ctx, Uri.fromFile(missing))).isNull()
    }

    /** A file:// uri is never used in place (the path is the sender's word): the result is a copy in Download/hymnal/tmp. */
    @Test
    fun anExistingFileIsReturnedAsACopy() {
        val file = File(ctx.cacheDir, "downloaded-test.mp3").apply { writeText("x") }
        var copy: File? = null
        try {
            copy = MediaDownloadHandler.downloadedFile(ctx, Uri.fromFile(file))
            assertThat(copy).isNotNull()
            assertThat(copy!!.readText()).isEqualTo("x")
        } finally {
            file.delete()
            copy?.delete()
        }
    }
}
