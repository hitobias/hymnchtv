package org.cog.hymnchtv.ui.lyrics

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class LyricsMetaTest {
    @Test
    fun parsesMeterKeyOnLineThree() {
        val lines = listOf("1", "神说要有光", "D大调 4/4", "", "神说，‘要有光’。")
        assertThat(LyricsMeta.parseMeterKey(lines)).isEqualTo("D大调 4/4")
    }

    @Test
    fun parsesMeterKeyOnLineFourWhenLineThreeIsTheVerseStructure() {
        val lines = listOf("130", "赞美主－祂的爱", "8787双（英152）", "g小调4/2", "", "一")
        assertThat(LyricsMeta.parseMeterKey(lines)).isEqualTo("g小调 4/2")
    }

    @Test
    fun dropsLeadingSyllableCountAndKeepsFlatSharpPrefix() {
        assertThat(LyricsMeta.parseMeterKey(listOf("附3", "福音", "8888双降A大调6/8", "", "一"))).isEqualTo("降A大调 6/8")
        assertThat(LyricsMeta.parseMeterKey(listOf("177", "x", "升c小调 2/2", "", "一"))).isEqualTo("升c小调 2/2")
    }

    @Test
    fun keyWithoutMeterAndTraditionalVariantAndFullWidthLetter() {
        assertThat(LyricsMeta.parseMeterKey(listOf("254", "x", "降E大调", "", "一"))).isEqualTo("降E大调")
        assertThat(LyricsMeta.parseMeterKey(listOf("1", "x", "D大調 4/4"))).isEqualTo("D大調 4/4")
        assertThat(LyricsMeta.parseMeterKey(listOf("1", "x", "Ｃ大调 4/4"))).isEqualTo("Ｃ大调 4/4")
    }

    @Test
    fun noKeyMeansNull() {
        assertThat(LyricsMeta.parseMeterKey(listOf("1", "兴起", "", "一", "兴起！兴起！主在点名"))).isNull()
        assertThat(LyricsMeta.parseMeterKey(listOf("563", "x", "8787副（英779，不同调）", "", "一"))).isNull()
        assertThat(LyricsMeta.parseMeterKey(emptyList())).isNull()
    }

    @Test
    fun removeMeterLineKeepsTheRest() {
        val text = "1\n神说要有光\nD大调 4/4\n\n神说\n"
        assertThat(LyricsMeta.removeMeterLine(text)).isEqualTo("1\n神说要有光\n\n神说\n")
        val none = "1\n兴起\n\n一\n"
        assertThat(LyricsMeta.removeMeterLine(none)).isEqualTo(none)
    }

    @Test
    fun everyErLyricsFileHasAMeterKeyAndXbHasNone() {
        val assets = File(checkNotNull(System.getProperty("hymnchtv.assetsDir")))
        fun count(dir: String, pick: (Int, Int) -> Boolean) {
            val files = File(assets, dir).listFiles { f -> f.name.endsWith(".txt") }!!
            val withKey = files.count { LyricsMeta.parseMeterKey(it.readLines()) != null }
            assertThat(files.size).isGreaterThan(100)
            assertThat(pick(withKey, files.size)).isTrue()
        }
        count("lyrics_er_text") { k, n -> k == n }
        count("lyrics_xb_text") { k, _ -> k == 0 }
        count("lyrics_er_text_hant_tw") { k, n -> k == n }
        // the large books: nearly all have one
        count("lyrics_db_text") { k, n -> k > n * 0.9 }
        count("lyrics_bb_text") { k, n -> k > n * 0.9 }
    }

    @Test
    fun verseMarkersAreNumeralLinesAfterTheTitle() {
        val text = "130\n赞美主\n\n一\n神在爱里。\n（副）\n神是爱。\n十二\n3\n第二节\n一，二"
        val marked = LyricsMeta.verseMarkerRanges(text).map { text.substring(it.first, it.last + 1) }
        assertThat(marked).containsExactly("一", "（副）", "十二", "3", "第二节").inOrder()
    }

    @Test
    fun hymnNumberAndTitleLinesAreNeverMarked() {
        assertThat(LyricsMeta.verseMarkerRanges("1\n一\n\n二\n")).hasSize(1)
        assertThat(LyricsMeta.verseMarkerRanges("")).isEmpty()
    }

    @Test
    fun verseMarkersInRealLyricsAreNeverMistakenForText() {
        val assets = File(checkNotNull(System.getProperty("hymnchtv.assetsDir")))
        val files = File(assets, "lyrics_xb_text").listFiles { f -> f.name.endsWith(".txt") }!!
        files.forEach { f ->
            val text = f.readText().replace("\r\n", "\n")
            LyricsMeta.verseMarkerRanges(text).forEach { r ->
                val marker = text.substring(r.first, r.last + 1)
                assertThat(marker.length).isAtMost(8)
                assertThat(text.lines().any { it.trim() == marker }).isTrue()
            }
        }
        // nearly every XB hymn has numbered verses (the few single-verse hymns have none)
        val unnumbered = files.count { LyricsMeta.verseMarkerRanges(it.readText().replace("\r\n", "\n")).isEmpty() }
        assertThat(unnumbered).isLessThan(files.size / 10)
    }
}
