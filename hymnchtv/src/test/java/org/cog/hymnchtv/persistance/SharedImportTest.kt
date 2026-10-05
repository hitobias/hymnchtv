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
}
