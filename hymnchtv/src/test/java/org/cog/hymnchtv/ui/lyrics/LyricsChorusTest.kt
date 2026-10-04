package org.cog.hymnchtv.ui.lyrics

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File

/** Chorus detection against the real lyrics assets: only a lone "副" / "副歌" label opens a chorus body. */
class LyricsChorusTest {
    private val assets = File(checkNotNull(System.getProperty("hymnchtv.assetsDir")))

    private fun read(path: String) = File(assets, path).readText().replace("\r\n", "\n")

    private fun bodies(text: String) = LyricsMeta.chorusBodyRanges(text).map { text.substring(it.first, it.last + 1) }

    @Test
    fun colonLabelOpensTheChorusBody() {
        val er4 = read("lyrics_er_text/er4.txt")
        assertThat(bodies(er4)).containsExactly("一切光明美丽之物，伟大或是渺小，\n它们都在欢乐歌颂，父神智慧、奇妙。")
        val marked = LyricsMeta.verseMarkerRanges(er4).map { er4.substring(it.first, it.last + 1) }
        assertThat(marked).containsAtLeast("一", "副：", "二", "三")
    }

    @Test
    fun parenthesisLabelsOpenTheChorusBody() {
        listOf("lyrics_db_text/db101.txt", "lyrics_xb_text/xb11.txt", "lyrics_er_text/er114.txt").forEach { path ->
            val text = read(path)
            val found = bodies(text)
            assertWithMessage(path).that(found).isNotEmpty()
            found.forEach { body ->
                assertThat(body).doesNotContain("\n\n")
                assertThat(body.lines().none { l -> l.isBlank() }).isTrue()
            }
        }
        assertThat(bodies(read("lyrics_db_text/db101.txt")).single()).startsWith("基督复活！阿利路亚")
        assertThat(bodies(read("lyrics_xb_text/xb11.txt")).single()).startsWith("我要起来")
    }

    @Test
    fun atLeastFiveHymnsWithAChorusAreRecognised() {
        val paths = listOf(
            "lyrics_er_text/er4.txt", "lyrics_er_text/er114.txt", "lyrics_er_text/er412.txt", "lyrics_er_text/er406.txt",
            "lyrics_er_text/er816.txt", "lyrics_db_text/db101.txt", "lyrics_xb_text/xb11.txt",
        )
        paths.forEach { assertWithMessage(it).that(bodies(read(it))).isNotEmpty() }
    }

    @Test
    fun inlineChorusNotesNeverStartAChorus() {
        // The same hymn in Simplified, HK and TW Traditional: "（接上面副歌）" ends a line of text
        listOf("lyrics_bb_text/bb317.txt", "lyrics_bb_text_hant_hk/bb317.txt", "lyrics_bb_text_hant_tw/bb317.txt").forEach { path ->
            val text = read(path)
            val marked = LyricsMeta.verseMarkerRanges(text).map { text.substring(it.first, it.last + 1) }
            assertWithMessage(path).that(marked.none { it.contains("接") }).isTrue()
            // without its real labels the hymn has no chorus at all: the note alone indents nothing
            val noLabels = text.lines().filterNot { it.trim() == "（副）" }.joinToString("\n")
            assertThat(noLabels).contains("接上面副歌")
            assertWithMessage(path).that(LyricsMeta.chorusBodyRanges(noLabels)).isEmpty()
            // with them, the bodies are exactly the three labelled blocks
            assertWithMessage(path).that(bodies(text)).hasSize(3)
        }
    }

    @Test
    fun notesAboutAChorusInVerseTextAreNotLabels() {
        val noted = "336\n标题\n\n一\n第一行\n（第三节无副歌）\n（第六节不唱副歌）\n8686双副（英116，无副歌）\n接副歌\n"
        assertThat(LyricsMeta.chorusBodyRanges(noted)).isEmpty()
        assertThat(LyricsMeta.verseMarkerRanges(noted).map { noted.substring(it.first, it.last + 1) }).containsExactly("一")
    }

    @Test
    fun everyMarkerLineInTheCorpusIsAShortLoneLabel() {
        val dirs = listOf("lyrics_er_text", "lyrics_xb_text", "lyrics_db_text", "lyrics_bb_text", "lyrics_xg_text", "lyrics_yb_text")
        var chorusHymns = 0
        dirs.forEach { dir ->
            File(assets, dir).listFiles { f -> f.name.endsWith(".txt") }!!.forEach { f ->
                val text = f.readText().replace("\r\n", "\n")
                LyricsMeta.verseMarkerRanges(text).forEach { r ->
                    val marker = text.substring(r.first, r.last + 1)
                    assertWithMessage("${f.name}: $marker").that(marker.length).isAtMost(8)
                    assertThat(marker).doesNotContain("接")
                }
                val found = LyricsMeta.chorusBodyRanges(text)
                if (found.isNotEmpty()) chorusHymns++
                found.forEach { r ->
                    val body = text.substring(r.first, r.last + 1)
                    assertWithMessage(f.name).that(body.lines().none { it.isBlank() }).isTrue()
                }
            }
        }
        assertThat(chorusHymns).isGreaterThan(300)
    }

    @Test
    fun blankLinesAfterTheTitleAreFound() {
        val text = "1\n标题\n\n一\n甲\n\n二\n乙"
        val blanks = LyricsMeta.blankLineRanges(text).map { it.first }
        assertThat(blanks).containsExactly(text.indexOf("\n\n") + 1, text.indexOf("甲\n\n") + 2).inOrder()
    }
}
