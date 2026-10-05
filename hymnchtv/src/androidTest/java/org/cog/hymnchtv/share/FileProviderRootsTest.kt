package org.cog.hymnchtv.share

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.persistance.FileBackend
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** 1.6.0 FileProvider roots on a device: Download/hymnal/ and the share cache are served, other external folders are not. */
@RunWith(AndroidJUnit4::class)
class FileProviderRootsTest {
    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    private val made = mutableListOf<File>()

    @After
    fun clean() {
        made.forEach { it.delete() }
    }

    private fun file(dir: File, name: String) = File(dir.apply { mkdirs() }, name).apply { writeText("x") }.also { made += it }

    @Test
    fun downloadHymnalAndTheShareCacheAreServed() {
        val tmp = FileBackend.getHymnchtvStore(FileBackend.TMP, false)
        assertThat(FileBackend.getUriForFile(ctx, file(tmp, "roots-test.csv")).authority).isEqualTo(ctx.packageName + ".files")
        val share = ShareFiles.newShareDir(ctx.cacheDir, System.currentTimeMillis())
        assertThat(FileBackend.getUriForFile(ctx, file(share, "roots-test.txt")).authority).isEqualTo(ctx.packageName + ".files")
    }

    @Test(expected = SecurityException::class)
    fun aFileInAnotherExternalFolderIsNotServed() {
        FileBackend.getUriForFile(ctx, file(ctx.getExternalFilesDir(null)!!, "roots-test.mp4"))
    }
}
