package org.cog.hymnchtv.persistance

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.io.InputStream

class SharedImportTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun source(text: String) = SharedImport.StreamSource { text.byteInputStream() }

    @Test
    fun safeNameKeepsOnlyThePlainFileName() {
        assertThat(SharedImport.safeName("hymn_link-20261005.csv")).isEqualTo("hymn_link-20261005.csv")
        assertThat(SharedImport.safeName("Download/hymnal/export.csv")).isEqualTo("export.csv")
        assertThat(SharedImport.safeName("a..b.csv")).isEqualTo("a..b.csv")
    }

    @Test
    fun safeNameRefusesTraversalAndEmptyNames() {
        val bad = listOf(null, "", " ", ".", "..", "../x.xml", "../../shared_prefs/evil.xml", "a/../b.csv", "..\\x.csv", "dir/")
        for (name in bad) {
            assertWithMessage(name.toString()).that(SharedImport.safeName(name)).isNull()
        }
    }

    @Test
    fun aTraversalNameWritesNothing() {
        val dir = tmp.newFolder("Download", "hymnal", "tmp")
        assertThat(SharedImport.copyInto(dir, "../../evil.xml", source("x"))).isNull()
        assertThat(File(tmp.root, "Download/evil.xml").exists()).isFalse()
        assertThat(dir.list()!!.toList()).isEmpty()
    }

    @Test
    fun reSharingTheSameNameReplacesTheOldContent() {
        val dir = tmp.newFolder("tmp")
        val first = SharedImport.copyInto(dir, "export.csv", source("v1"))!!
        val second = SharedImport.copyInto(dir, "export.csv", source("v2"))!!
        assertThat(second).isEqualTo(first)
        assertThat(second.readText()).isEqualTo("v2")
    }

    private val broken = SharedImport.StreamSource {
        object : InputStream() {
            override fun read(): Int = throw IOException("boom")
        }
    }

    @Test
    fun anUnopenableStreamLeavesNoFile() {
        val dir = tmp.newFolder("tmp")
        assertThat(SharedImport.copyInto(dir, "export.csv", SharedImport.StreamSource { null })).isNull()
        assertThat(dir.list()!!.toList()).isEmpty()
    }

    @Test
    fun aFailedCopyLeavesNoPartialFile() {
        val dir = tmp.newFolder("tmp")
        assertThat(SharedImport.copyInto(dir, "export.csv", broken)).isNull()
        assertThat(dir.list()!!.toList()).isEmpty()
    }

    @Test
    fun anUnopenableStreamKeepsTheOldFile() {
        val dir = tmp.newFolder("tmp")
        File(dir, "export.csv").writeText("old")
        assertThat(SharedImport.copyInto(dir, "export.csv", SharedImport.StreamSource { null })).isNull()
        assertThat(File(dir, "export.csv").readText()).isEqualTo("old")
        assertThat(dir.list()!!.toList()).containsExactly("export.csv")
    }

    @Test
    fun aFailedCopyKeepsTheOldFile() {
        val dir = tmp.newFolder("tmp")
        File(dir, "export.csv").writeText("old")
        assertThat(SharedImport.copyInto(dir, "export.csv", broken)).isNull()
        assertThat(File(dir, "export.csv").readText()).isEqualTo("old")
        assertThat(dir.list()!!.toList()).containsExactly("export.csv")
    }

    @Test
    fun aTakenNameGetsAFreeAlternate() {
        val dir = tmp.newFolder("tmp")
        File(dir, "export.csv").writeText("left by an earlier install, which this one may not replace (API 30+)")
        assertThat(SharedImport.freeName(dir, "export.csv")).isEqualTo(File(dir, "export-2.csv"))
    }

    @Test
    fun controlCharactersAreRemoved() {
        assertThat(SharedImport.safeName("hymn\u0000_\u001F link\u007F\u0085.csv")).isEqualTo("hymn_ link.csv")
        assertThat(SharedImport.safeName("\u0001\u0002")).isNull()
    }

    @Test
    fun aLongNameIsCutTo200Utf8BytesAndKeepsItsExtension() {
        val long = "詩".repeat(100) + ".csv"                       // 300 + 4 bytes
        val name = SharedImport.safeName(long)!!
        assertThat(name.toByteArray(Charsets.UTF_8).size).isAtMost(SharedImport.MAX_NAME_BYTES)
        assertThat(name).endsWith(".csv")
        assertThat(name.removeSuffix(".csv")).isEqualTo("詩".repeat(65))  // 195 bytes: a 3-byte character never splits
    }

    @Test
    fun anOverlongExtensionIsNotKept() {
        val name = SharedImport.safeName("a".repeat(150) + "." + "b".repeat(100))!!
        assertThat(name.toByteArray(Charsets.UTF_8).size).isEqualTo(SharedImport.MAX_NAME_BYTES)
        assertThat(name).startsWith("a".repeat(150) + ".")
    }

    @Test
    fun aShortNameIsUntouched() {
        assertThat(SharedImport.safeName("詩歌-匯出.csv")).isEqualTo("詩歌-匯出.csv")
    }

    @Test
    fun aLongNameStillImports() {
        val dir = tmp.newFolder("long")
        val file = SharedImport.copyInto(dir, "x".repeat(300) + ".csv", source("hymn_db,1"))
        assertThat(file).isNotNull()
        assertThat(file!!.name.length).isAtMost(SharedImport.MAX_NAME_BYTES)
        assertThat(file.readText()).isEqualTo("hymn_db,1")
    }
}

class SharedImportRuntimeFailureTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun aProviderThrowingRuntimeExceptionLeavesNoPartAndKeepsTheOldFile() {
        val dir = tmp.newFolder("tmp")
        File(dir, "export.csv").writeText("old")
        val bad = SharedImport.StreamSource { throw IllegalStateException("provider died") }
        assertThat(SharedImport.copyInto(dir, "export.csv", bad)).isNull()
        val midway = SharedImport.StreamSource {
            object : InputStream() {
                override fun read(): Int = throw IllegalStateException("died mid-copy")
            }
        }
        assertThat(SharedImport.copyInto(dir, "export.csv", midway)).isNull()
        assertThat(dir.list()!!.toList()).containsExactly("export.csv")
        assertThat(File(dir, "export.csv").readText()).isEqualTo("old")
    }

    @Test
    fun cleanStalePartsRemovesOnlyOldImportParts() {
        val dir = tmp.newFolder("tmp")
        val now = 1_800_000_000_000L
        val old = File(dir, ".import-1.part").apply { writeText("x"); setLastModified(now - 2 * 3600_000L) }
        val fresh = File(dir, ".import-2.part").apply { writeText("x"); setLastModified(now - 60_000L) }
        val other = File(dir, "export.csv").apply { writeText("x"); setLastModified(now - 9 * 3600_000L) }
        assertThat(SharedImport.cleanStaleParts(dir, now, SharedImport.PART_MAX_AGE_MS)).isEqualTo(1)
        assertThat(old.exists()).isFalse()
        assertThat(fresh.exists()).isTrue()
        assertThat(other.exists()).isTrue()
        assertThat(SharedImport.cleanStaleParts(File(dir, "missing"), now, SharedImport.PART_MAX_AGE_MS)).isEqualTo(0)
    }
}
