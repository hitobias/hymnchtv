package org.cog.hymnchtv.share

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.nio.file.Files

class MediaShareFilesTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val storage get() = File(tmp.root, "storage/emulated/0")
    private val cache get() = File(tmp.root, "cache")
    private val data get() = File(tmp.root, "data/user/0/app")
    private val forbidden get() = listOf(data, cache)
    private val sd get() = File(tmp.root, "storage/ABCD-1234")
    private val roots get() = listOf(storage, sd)

    private fun file(path: String, text: String = path) = File(storage, path).apply { parentFile!!.mkdirs(); writeText(text) }

    @Test
    fun filesUnderDownloadHymnalAreSharedInPlace() {
        val export = file("Download/hymnal/export/hymn_link.csv")
        assertThat(MediaShareFiles.prepare(cache, roots, forbidden, listOf(export), 1000L).files).containsExactly(export.canonicalFile)
        assertThat(File(cache, ShareFiles.DIR).exists()).isFalse()
    }

    @Test
    fun filesElsewhereAreCopiedIntoOneNewShareDirectory() {
        val video = file("Movies/v.mp4", "video")
        val sibling = file("Download/hymnal2/a.mp3", "audio")          // same prefix, other directory
        val shared = MediaShareFiles.prepare(cache, roots, forbidden, listOf(video, sibling), 1000L).files
        assertThat(shared.map { it.parentFile }.toSet()).containsExactly(File(cache, "share/1000"))
        assertThat(shared.map { it.name }).containsExactly("v.mp4", "a.mp3").inOrder()
        assertThat(shared[0].readText()).isEqualTo("video")
        assertThat(video.exists()).isTrue()
    }

    @Test
    fun theRootMatchesTheAppFolder() {
        assertThat(MediaShareFiles.HYMNAL_ROOT).isEqualTo("Download/hymnal/")
    }

    private fun private(path: String) = File(data, path).apply { parentFile!!.mkdirs(); writeText("secret") }

    /** A refused file is reported, never copied, and never stops the others. */
    private fun assertRejected(vararg files: File) {
        val result = MediaShareFiles.prepare(cache, roots, forbidden, files.toList(), 1000L)
        assertThat(result.files).isEmpty()
        assertThat(result.refused).hasSize(files.size)
        assertThat(File(cache, ShareFiles.DIR).exists()).isFalse()
    }

    @Test
    fun aFileOnAnSdCardVolumeIsCopied() {
        val clip = File(sd, "Music/a.mp3").apply { parentFile!!.mkdirs(); writeText("sd") }
        val result = MediaShareFiles.prepare(cache, roots, forbidden, listOf(clip), 1000L)
        assertThat(result.refused).isEmpty()
        assertThat(result.files.single().readText()).isEqualTo("sd")
        assertThat(result.files.single().parentFile).isEqualTo(File(cache, "share/1000"))
    }

    @Test
    fun aRefusedFileDoesNotStopTheOthers() {
        val db = private("databases/hymn.db")
        val video = file("Movies/v.mp4", "video")
        val result = MediaShareFiles.prepare(cache, roots, forbidden, listOf(db, video), 1000L)
        assertThat(result.refused).containsExactly(db)
        assertThat(result.files.map { it.name }).containsExactly("v.mp4")
    }

    @Test
    fun aNameShareFilesRefusesIsAnIoExceptionAndLeavesNothingBehind() {
        val odd = file("Movies/a\\b.mp4")
        try {
            MediaShareFiles.prepare(cache, roots, forbidden, listOf(odd), 1000L)
            throw AssertionError("expected IOException")
        } catch (e: IOException) {
            // refused
        }
        assertThat(File(cache, "share/1000").exists()).isFalse()
    }

    @Test
    fun volumeRootsAreDerivedFromTheAppSpecificDirs() {
        val dirs = listOf(
            File("/storage/emulated/0/Android/data/com.ziontkec.hymnal/files"),
            null,
            File("/storage/ABCD-1234/Android/data/com.ziontkec.hymnal/files"),
        )
        assertThat(MediaShareFiles.volumeRoots(dirs)).containsExactly(File("/storage/emulated/0"), File("/storage/ABCD-1234")).inOrder()
    }

    @Test
    fun aFileInTheAppDataDirectoryIsRefused() {
        storage.mkdirs()
        assertRejected(private("databases/hymn.db"))
    }

    @Test
    fun aDotDotPathIntoTheAppDataDirectoryIsRefused() {
        val db = private("databases/hymn.db")
        file("Movies/x.mp4")
        assertRejected(File(storage, "Movies/../../../../../" + db.relativeTo(tmp.root).path))
    }

    @Test
    fun aSymlinkToAppDataIsRefused() {
        val db = private("databases/hymn.db")
        val link = File(storage, "Movies/link.mp4").apply { parentFile!!.mkdirs() }
        try {
            Files.createSymbolicLink(link.toPath(), db.toPath())
        } catch (e: Exception) {
            return // symlinks not available here
        }
        assertRejected(link)
    }

    @Test
    fun aSymlinkInHymnalToAppDataIsRefused() {
        val db = private("databases/hymn.db")
        val link = File(storage, "Download/hymnal/export/link.csv").apply { parentFile!!.mkdirs() }
        try {
            Files.createSymbolicLink(link.toPath(), db.toPath())
        } catch (e: Exception) {
            return
        }
        assertRejected(link)
    }

    @Test
    fun aFileOutsideExternalStorageIsRefused() {
        val outside = File(tmp.root, "elsewhere/a.mp3").apply { parentFile!!.mkdirs(); writeText("x") }
        assertRejected(outside)
    }

    @Test
    fun directoriesAndMissingFilesAreRefused() {
        val dir = File(storage, "Movies").apply { mkdirs() }
        assertRejected(dir)
        assertRejected(File(storage, "Movies/none.mp4"))
    }
}
