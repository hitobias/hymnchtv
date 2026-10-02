package org.cog.hymnchtv.reading

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/**
 * HymnalKai SC/TC must cover every character of their lyrics and of every strings file, with no exceptions,
 * and carry no OFL Reserved Font Name (plan A2). Coverage comes from tools/font_subset_manifest.txt, which
 * tools/gen_font_subset.py writes from the font's real cmap; the SHA-256 check ties it to the committed font.
 */
class FontSubsetTest {
    private fun dir(property: String) = File(checkNotNull(System.getProperty(property)) { "$property not set" })
    private val res = dir("hymnchtv.resDir")
    private val assets = dir("hymnchtv.assetsDir")
    private val repoRoot = dir("hymnchtv.repoRoot")

    private val rows: List<List<String>> by lazy {
        File(repoRoot, "tools/font_subset_manifest.txt").readLines()
            .filter { it.isNotBlank() && !it.startsWith("# ") }
            .map { it.split('\t') }
    }

    private fun row(kind: String, variant: String): List<String> = rows.single { it[0] == kind && it[1] == variant }

    private fun covered(variant: String): Set<Int> = row("covers", variant)[2].split(' ').flatMap { run ->
        val ends = run.split('-').map { it.toInt(16) }
        (ends.first()..ends.last()).toList()
    }.toSet()

    /** Same rule as tools/gen_font_subset.py: everything except C0 controls, DEL and the BOM. */
    private fun codePoints(files: List<File>): Set<Int> = files
        .flatMap { it.readText(Charsets.UTF_8).codePoints().toArray().asList() }
        .filter { it >= 0x20 && it != 0x7F && it != 0xFEFF }
        .toSet()

    private fun lyricFiles(dirPattern: Regex): List<File> =
        assets.listFiles { f -> f.isDirectory && dirPattern.matches(f.name) }!!
            .flatMap { d -> d.listFiles { f -> f.name.endsWith(".txt") }!!.toList() }

    private fun stringFiles(): List<File> =
        res.listFiles { f -> f.isDirectory && f.name.startsWith("values") }!!
            .flatMap { d -> d.listFiles { f -> f.name.startsWith("strings") && f.name.endsWith(".xml") }!!.toList() }

    private fun assertCovers(variant: String, lyricDirs: Regex) {
        val lyrics = codePoints(lyricFiles(lyricDirs))
        assertThat(lyrics.size).isGreaterThan(2000)   // guards against a wrong assets path
        val strings = codePoints(stringFiles())
        assertThat(strings.size).isGreaterThan(300)
        val missing = (lyrics + strings) - covered(variant)
        val listing = missing.sorted().joinToString(" ") { "U+%04X(%s)".format(it, String(Character.toChars(it))) }
        assertWithMessage("$variant font lacks $listing (fix the text or regenerate; there is no exception list)")
            .that(missing).isEmpty()
    }

    @Test
    fun scCoversSimplifiedLyricsAndAllStrings() = assertCovers("sc", Regex("lyrics_[a-z]+_text"))

    @Test
    fun tcCoversBothTraditionalVariantsAndAllStrings() = assertCovers("tc", Regex("lyrics_[a-z]+_text_hant_(tw|hk)"))

    @Test
    fun committedFontsAreTheOnesTheManifestDescribes() {
        for (variant in listOf("sc", "tc")) {
            val (_, _, path, digest) = row("font", variant)
            assertThat(path).isEqualTo("hymnchtv/src/main/res/font/hymnal_kai_$variant.ttf")
            assertWithMessage(path).that(sha256(File(repoRoot, path))).isEqualTo(digest)
        }
    }

    @Test
    fun namingRecordsAreHymnalKaiOnly() {
        for ((variant, family) in listOf("sc" to "HymnalKai SC", "tc" to "HymnalKai TC")) {
            val names = rows.filter { it[0] == "name" && it[1] == variant }
            assertThat(names.single { it[2] == "1" }[3]).isEqualTo(family)
            names.forEach { record ->
                RESERVED.forEach { word -> assertWithMessage(record.joinToString(" ")).that(record[3].lowercase()).doesNotContain(word) }
            }
        }
    }

    @Test
    fun oflLicenceShipsWithTheApp() {
        val text = File(assets, "licenses/OFL-HymnalKai.txt").readText()
        assertThat(text).contains("SIL OPEN FONT LICENSE Version 1.1")
        assertThat(text).contains("HymnalKai")
        assertThat(text).contains("LxgwWenKai")
        assertThat(text).contains("LxgwWenkaiTC")
    }

    private fun sha256(file: File): String =
        MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }

    private companion object {
        val RESERVED = listOf("lxgw", "霞鹜", "霞鶩", "落霞孤鹜", "落霞孤鶩")
    }
}
