package org.cog.hymnchtv.share

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class MediaShareFilesTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val storage get() = File(tmp.root, "storage/emulated/0")
    private val cache get() = File(tmp.root, "cache")

    private fun file(path: String, text: String = path) = File(storage, path).apply { parentFile!!.mkdirs(); writeText(text) }

    @Test
    fun filesUnderDownloadHymnalAreSharedInPlace() {
        val export = file("Download/hymnal/export/hymn_link.csv")
        assertThat(MediaShareFiles.prepare(cache, storage, listOf(export), 1000L)).containsExactly(export)
        assertThat(File(cache, ShareFiles.DIR).exists()).isFalse()
    }

    @Test
    fun filesElsewhereAreCopiedIntoOneNewShareDirectory() {
        val video = file("Movies/v.mp4", "video")
        val sibling = file("Download/hymnal2/a.mp3", "audio")          // same prefix, other directory
        val shared = MediaShareFiles.prepare(cache, storage, listOf(video, sibling), 1000L)
        assertThat(shared.map { it.parentFile }.toSet()).containsExactly(File(cache, "share/1000"))
        assertThat(shared.map { it.name }).containsExactly("v.mp4", "a.mp3").inOrder()
        assertThat(shared[0].readText()).isEqualTo("video")
        assertThat(video.exists()).isTrue()
    }

    @Test
    fun theRootMatchesTheAppFolder() {
        assertThat(MediaShareFiles.HYMNAL_ROOT).isEqualTo("Download/hymnal/")
    }
}
