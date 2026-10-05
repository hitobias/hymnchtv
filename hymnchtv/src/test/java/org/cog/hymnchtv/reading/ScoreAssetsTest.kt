package org.cog.hymnchtv.reading

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File

/** 1.6.0: every bundled score page is a lossless WebP (tools/convert_scores_webp.py); none was lost on the way. */
class ScoreAssetsTest {
    private val assets = File(checkNotNull(System.getProperty("hymnchtv.assetsDir")) { "hymnchtv.assetsDir not set" })
    private val counts = mapOf("bb" to 521, "db" to 808, "er" to 330, "xb" to 168, "xg" to 205, "yb" to 61)
    private val name = Regex("""^(bb|dbs|db|xb|xg|yb)?\d+[a-d]?\.webp$""")

    @Test
    fun everyBookKeepsAllItsPagesAsWebp() {
        for ((book, count) in counts) {
            val files = File(assets, "lyrics_${book}_score").listFiles()!!.toList()
            assertWithMessage(book).that(files.map { it.name }.filterNot { name.matches(it) }).isEmpty()
            assertWithMessage(book).that(files).hasSize(count)
        }
    }

    @Test
    fun everyPageIsLosslessWebp() {
        for (book in counts.keys) {
            for (file in File(assets, "lyrics_${book}_score").listFiles()!!) {
                val head = file.inputStream().use { it.readNBytes(16) }
                val text = String(head, Charsets.US_ASCII)
                assertWithMessage(file.name).that(text.substring(0, 4)).isEqualTo("RIFF")
                assertWithMessage(file.name).that(text.substring(8, 12)).isEqualTo("WEBP")
                assertWithMessage(file.name).that(text.substring(12, 16)).isEqualTo("VP8L")
            }
        }
    }

    @Test
    fun theFivePageHymnHasAllItsFiles() {
        for (path in ScorePages.fileNames("lyrics_db_score/db152", 5)) {
            assertWithMessage(path).that(File(assets, path).isFile).isTrue()
        }
    }
}
