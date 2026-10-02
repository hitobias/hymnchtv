package org.cog.hymnchtv.lyrics

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/**
 * Fails when lyrics sources, generated Traditional outputs, the overrides table or the generator
 * change without re-running tools/gen_lyrics_hant.py (plan A.1.9).
 */
class LyricsHantAssetsTest {
    private val assets = File(checkNotNull(System.getProperty("hymnchtv.assetsDir")) { "hymnchtv.assetsDir not set" })
    private val repoRoot = assets.resolve("../../../..").canonicalFile
    private val sourceDirs: List<File> =
        assets.listFiles { f -> f.isDirectory && SOURCE_DIR.matches(f.name) }!!.sortedBy { it.name }

    private fun txtNames(dir: File): Set<String> =
        dir.listFiles { f -> f.isFile && f.name.endsWith(".txt") }?.map { it.name }?.toSet() ?: emptySet()

    private fun sha1(file: File): String =
        MessageDigest.getInstance("SHA-1").digest(file.readBytes()).joinToString("") { "%02x".format(it) }

    /** Strict parse: "#input<TAB>name<TAB>sha1" header rows and "path<TAB>sha1" rows, no duplicates. */
    private fun manifest(): Pair<Map<String, String>, Map<String, String>> {
        val inputs = mutableMapOf<String, String>()
        val files = mutableMapOf<String, String>()
        File(assets, "lyrics_hant_manifest.txt").readLines().filter { it.isNotBlank() && !it.startsWith("# ") }.forEach { line ->
            val cols = line.split('\t')
            if (cols[0] == "#input") {
                check(cols.size == 3 && SHA1.matches(cols[2])) { "bad manifest input row: $line" }
                check(inputs.put(cols[1], cols[2]) == null) { "duplicate input: ${cols[1]}" }
            } else {
                check(cols.size == 2 && PATH.matches(cols[0]) && SHA1.matches(cols[1])) { "bad manifest row: $line" }
                check(files.put(cols[0], cols[1]) == null) { "duplicate path: ${cols[0]}" }
            }
        }
        return inputs to files
    }

    @Test
    fun sourceDirectoriesFound() {
        assertThat(sourceDirs.map { it.name }).containsAtLeast("lyrics_db_text", "lyrics_bb_text")
    }

    @Test
    fun outputDirectoriesAreExactlyTheExpectedOnes() {
        val expected = sourceDirs.flatMap { d -> HantVariant.values().map { d.name + it.dirSuffix } }.toSet()
        val actual = assets.listFiles { f -> f.isDirectory && f.name.contains("_hant_") }!!.map { it.name }.toSet()
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun everyVariantMirrorsTheSourceFileSet() {
        for (dir in sourceDirs) {
            for (variant in HantVariant.values()) {
                assertThat(txtNames(File(assets, dir.name + variant.dirSuffix))).isEqualTo(txtNames(dir))
            }
        }
    }

    @Test
    fun manifestMatchesSourcesAndOutputs() {
        val actual = sourceDirs.flatMap { dir ->
            listOf(dir) + HantVariant.values().map { File(assets, dir.name + it.dirSuffix) }
        }.flatMap { dir -> txtNames(dir).map { "${dir.name}/$it" to sha1(File(dir, it)) } }.toMap()
        assertThat(manifest().second).isEqualTo(actual)
    }

    @Test
    fun manifestMatchesGeneratorInputs() {
        assertThat(manifest().first).isEqualTo(
            mapOf(
                "tools/gen_lyrics_hant.py" to sha1(File(repoRoot, "tools/gen_lyrics_hant.py")),
                "tools/lyrics_hant_overrides.tsv" to sha1(File(repoRoot, "tools/lyrics_hant_overrides.tsv")),
            )
        )
    }

    private companion object {
        val SOURCE_DIR = Regex("lyrics_[a-z]+_text")
        val PATH = Regex("lyrics_[a-z]+_text(_hant_(tw|hk))?/[^/\t]+\\.txt")
        val SHA1 = Regex("[0-9a-f]{40}")
    }
}
