package org.cog.hymnchtv.share

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ShareFilesTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val now = 1_800_000_000_000L
    private val hour = 60 * 60 * 1000L

    @Test
    fun everyShareGetsItsOwnDirectoryEvenInTheSameMillisecond() {
        val cache = tmp.newFolder("cache")
        val first = ShareFiles.newShareDir(cache, now)
        val second = ShareFiles.newShareDir(cache, now)
        assertThat(first.parentFile).isEqualTo(ShareFiles.dir(cache))
        assertThat(second.parentFile).isEqualTo(ShareFiles.dir(cache))
        assertThat(second).isNotEqualTo(first)
        assertThat(first.isDirectory && second.isDirectory).isTrue()
    }

    @Test
    fun writeCopiesTheStreamIntoTheShareDirectory() {
        val shareDir = ShareFiles.newShareDir(tmp.newFolder("cache"), now)
        val file = ShareFiles.write(shareDir, "yb102.txt", "lyrics".byteInputStream())
        assertThat(file).isEqualTo(File(shareDir, "yb102.txt"))
        assertThat(file.readText()).isEqualTo("lyrics")
    }

    @Test
    fun namesWithDirectoriesAreRefused() {
        val shareDir = ShareFiles.newShareDir(tmp.newFolder("cache"), now)
        for (bad in listOf("../x.txt", "a/b.txt", "a\\b.txt", "..", ".", "")) {
            assertThrows(IllegalArgumentException::class.java) { ShareFiles.write(shareDir, bad, "x".byteInputStream()) }
        }
    }

    @Test
    fun cleanRemovesOnlySharesOlderThanADay() {
        val cache = tmp.newFolder("cache")
        val old = ShareFiles.newShareDir(cache, now - 30 * hour)
        ShareFiles.write(old, "db1.txt", "old".byteInputStream())
        old.setLastModified(now - 30 * hour)
        val fresh = ShareFiles.newShareDir(cache, now - hour)
        ShareFiles.write(fresh, "db2.txt", "fresh".byteInputStream())
        fresh.setLastModified(now - hour)

        assertThat(ShareFiles.cleanOlderThan(cache, now, ShareFiles.MAX_AGE_MS)).isEqualTo(1)
        assertThat(old.exists()).isFalse()
        assertThat(File(fresh, "db2.txt").readText()).isEqualTo("fresh")
    }

    @Test
    fun cleanWithoutADirectoryDoesNothing() {
        assertThat(ShareFiles.cleanOlderThan(tmp.newFolder("empty"), now, ShareFiles.MAX_AGE_MS)).isEqualTo(0)
    }
}
