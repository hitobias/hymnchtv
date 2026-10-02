package org.cog.hymnchtv.search

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SearchPatternTest {
    private fun finds(query: String, text: String) = SearchPattern.build(query)!!.matcher(text).find()

    @Test
    fun blankQueryReturnsNull() {
        listOf("", "   ", "\t\n").forEach { assertThat(SearchPattern.build(it)).isNull() }
    }

    @Test
    fun regexMetaCharactersAreLiteralAndNeverThrow() {
        listOf("(", "[", "*", "\\", "\\E", "a|b", "?", "+", "{2}", "^$", ".").forEach {
            assertThat(finds(it, "xx${it}yy")).isTrue()
        }
        assertThat(finds(".", "abc")).isFalse()
    }

    @Test
    fun heMatchesBothHeAndHim() {
        assertThat(finds("他", "祂")).isTrue()
        assertThat(finds("他", "他")).isTrue()
        assertThat(finds("跟随他", "跟随祂走")).isTrue()
        assertThat(finds("他爱他", "祂爱他")).isTrue()
        assertThat(finds("他", "|")).isFalse()
    }

    @Test
    fun himOnlyMatchesHim() {
        assertThat(finds("祂", "祂")).isTrue()
        assertThat(finds("祂", "他")).isFalse()
    }

    @Test
    fun heAtEdgesWithMetaCharacters() {
        assertThat(finds("他(", "祂(")).isTrue()
        assertThat(finds("(他", "(祂")).isTrue()
    }

    @Test
    fun innerSpacesKeptOuterTrimmed() {
        assertThat(finds("  主 耶稣  ", "主 耶稣")).isTrue()
        assertThat(finds("主 耶稣", "主耶稣")).isFalse()
    }

    @Test
    fun surrogatePairs() {
        assertThat(finds("𠀀他", "𠀀祂")).isTrue()
        assertThat(finds("🙏", "a🙏b")).isTrue()
    }

    @Test
    fun consecutiveHeMatchesAnyCombination() {
        assertThat(finds("他他", "祂他")).isTrue()
        assertThat(finds("他他", "他祂")).isTrue()
    }

    @Test
    fun nullQueryReturnsNull() {
        assertThat(SearchPattern.build(null)).isNull()
    }
}
