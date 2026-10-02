package org.cog.hymnchtv.search

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SnippetExtractorTest {
    private fun p(q: String) = SearchPattern.build(q)!!

    @Test fun skipsTheNumberLineNotFourCharactersAndFindsTheTitleLine() {
        val hit = SnippetExtractor.find("123\r\n颂赞－标题\r\n正文", p("颂"))!!
        assertThat(hit.lineIndex).isEqualTo(1)
        assertThat(hit.snippet).startsWith("颂赞－标题")
        assertThat(SnippetExtractor.find("颂1\r\n别的\r\n正文", p("颂"))).isNull()
    }

    @Test fun startsAtTheMatchingLineJoinsWithSpacesAndStopsAtSixtyFour() {
        val long = "字".repeat(40)
        val hit = SnippetExtractor.find("1\r\n标题\r\n$long\r\n甲乙丙\r\n$long\r\n$long\r\n", p("甲"))!!
        assertThat(hit.lineIndex).isEqualTo(3)
        assertThat(hit.snippet).startsWith("甲乙丙 ")
        assertThat(hit.snippet.length).isEqualTo(64)
        assertThat(SnippetExtractor.find("1\r\n标题\r\n$long\r\n", p("字"))!!.snippet.length).isEqualTo(40)
    }

    @Test fun mixedLineBreaks() {
        val hit = SnippetExtractor.find("1\r\n标题\n歌词甲\r\n歌词乙\n", p("歌词"))!!
        assertThat(hit.snippet).isEqualTo("歌词甲 歌词乙")
    }

    @Test fun onlyTheFirstMatchIsUsed() {
        val hit = SnippetExtractor.find("1\r\n标题\r\n甲一\r\n甲二\r\n", p("甲"))!!
        assertThat(hit.lineIndex).isEqualTo(2)
        assertThat(hit.snippet).isEqualTo("甲一 甲二")
    }

    @Test fun noMatchIsNull() {
        assertThat(SnippetExtractor.find("1\r\n标题\r\n正文\r\n", p("无"))).isNull()
    }

    @Test fun blankLinesAreDropped() {
        assertThat(SnippetExtractor.find("1\r\n标题\r\n甲\r\n\r\n乙\r\n", p("甲"))!!.snippet).isEqualTo("甲 乙")
    }
}
