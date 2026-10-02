package org.cog.hymnchtv.mediaconfig

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LocalMediaIndexTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun matchesTheSameNamesAsContentHandler() {
        val match = HymnFileName.matcher(9)
        assertThat(match("ChHymns-0009.mp3")).isTrue()
        assertThat(match("D9建造.mp3")).isTrue()
        assertThat(match("ChHymns-0019.mp3")).isFalse()
        assertThat(match("ChHymns-0090.mp3")).isFalse()
        assertThat(HymnFileName.matcher(609)("D609建造.mp3")).isTrue()
    }

    @Test
    fun findsFilesAndListsEachDirectoryOnce() {
        val dir = tmp.newFolder("db_media")
        File(dir, "ChHymns-0009.mp3").writeText("")
        val listed = mutableListOf<String>()
        val index = LocalMediaIndex { key -> listed += key; File(tmp.root, key) }

        assertThat(index.has("db_media", 9)).isTrue()
        assertThat(index.has("db_media", 10)).isFalse()
        assertThat(index.has("db_media", 9)).isTrue()
        assertThat(listed).containsExactly("db_media")
    }

    @Test
    fun missingDirectoryMeansNoLocalFile() {
        val index = LocalMediaIndex { key -> File(tmp.root, key) }
        assertThat(index.has("absent", 1)).isFalse()
    }

    @Test
    fun unresolvableDirectoryMeansNoLocalFile() {
        val index = LocalMediaIndex { null }
        assertThat(index.has("any", 1)).isFalse()
    }
}
