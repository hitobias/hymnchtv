package org.cog.hymnchtv.share

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.persistance.FileBackend
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/** Share files come from the right asset directories, one directory per share, and the FileProvider can hand them out. */
@RunWith(AndroidJUnit4::class)
class LyricsShareFilesTest {
    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    private val shareRoot get() = ShareFiles.dir(ctx.cacheDir)

    @Before
    @After
    fun clean() {
        shareRoot.deleteRecursively()
    }

    private fun assetBytes(path: String) = ctx.assets.open(path).use { it.readBytes() }

    private val pngSignature = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

    private fun assetBitmap(path: String) = ctx.assets.open(path).use { BitmapFactory.decodeStream(it)!! }

    @Test
    fun aYouthHymnSharesItsScoreAndLyricsFromTheCache() {
        val files = LyricsShareFiles.prepare(ctx, MainActivity.HYMN_YB, 102)
        assertThat(files.map { it.name }).containsExactly("yb102.png", "yb102.txt").inOrder()
        files.forEach { assertThat(it.parentFile!!.parentFile).isEqualTo(shareRoot) }
        // the WebP asset goes out as a PNG with the very same pixels
        assertThat(files[0].readBytes().copyOf(8)).isEqualTo(pngSignature)
        assertThat(BitmapFactory.decodeFile(files[0].path).sameAs(assetBitmap("lyrics_yb_score/yb102.webp"))).isTrue()
        assertThat(files[1].readBytes()).isEqualTo(assetBytes("lyrics_yb_text/yb102.txt"))
        // file_paths.xml must expose cacheDir/share/, or the share intent cannot be built
        files.forEach { assertThat(FileBackend.getUriForFile(ctx, it).authority).isEqualTo(ctx.packageName + ".files") }
    }

    @Test
    fun aYouthHymnWithoutAScoreSharesTheLyricsOnly() {
        val files = LyricsShareFiles.prepare(ctx, MainActivity.HYMN_YB, 162)
        assertThat(files.map { it.name }).containsExactly("yb162.txt")
    }

    @Test
    fun twoSharesInARowNeverTouchEachOther() {
        val first = LyricsShareFiles.prepare(ctx, MainActivity.HYMN_YB, 102)
        val second = LyricsShareFiles.prepare(ctx, MainActivity.HYMN_YB, 102)
        assertThat(second[0].parentFile).isNotEqualTo(first[0].parentFile)
        assertThat(FileBackend.getUriForFile(ctx, second[1])).isNotEqualTo(FileBackend.getUriForFile(ctx, first[1]))
        // the first receiver may still be reading its files
        assertThat(first[1].readBytes()).isEqualTo(assetBytes("lyrics_yb_text/yb102.txt"))
    }

    @Test
    fun startupCleanRemovesOldSharesAndKeepsFreshOnes() {
        val old = LyricsShareFiles.prepare(ctx, MainActivity.HYMN_DB, 1)[0].parentFile!!
        val fresh = LyricsShareFiles.prepare(ctx, MainActivity.HYMN_DB, 2)[0].parentFile!!
        val now = System.currentTimeMillis()
        assertThat(old.setLastModified(now - ShareFiles.MAX_AGE_MS - 60_000)).isTrue()
        assertThat(ShareFiles.cleanOlderThan(ctx.cacheDir, now, ShareFiles.MAX_AGE_MS)).isEqualTo(1)
        assertThat(old.exists()).isFalse()
        assertThat(fresh.list()!!.toList()).containsExactly("db2.png", "db2.txt")
    }

    @Test(expected = IOException::class)
    fun anUnknownHymnFails() {
        LyricsShareFiles.prepare(ctx, "hymn_zz", 1)
    }

    /** xb157 (1429 x 2055) is the largest page: it must still be shared on API 24's 32 MB heap. */
    @Test
    fun theLargestScorePageIsShared() {
        val files = LyricsShareFiles.prepare(ctx, MainActivity.HYMN_XB, 157)
        assertThat(files[0].name).isEqualTo("xb157.png")
        assertThat(files[0].readBytes().copyOf(8)).isEqualTo(pngSignature)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(files[0].path, bounds)
        assertThat(bounds.outWidth to bounds.outHeight).isEqualTo(1429 to 2055)
    }
}
