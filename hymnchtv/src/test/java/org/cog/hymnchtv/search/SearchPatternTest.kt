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
    fun recoveryVersionAndTaiwanGlyphsBothMatchSimplifiedText() {
        val map = T2sMap.parse(listOf("裏\t里", "裡\t里", "靈\t灵"))
        // Search runs on the Simplified lyrics: 里/裏/裡 queries all find the line shown as 「靈裏」.
        listOf("灵里", "靈裏", "靈裡").forEach {
            assertThat(SearchPattern.build(it, map)!!.matcher("活在灵里面").find()).isTrue()
        }
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

    private val t2s = T2sMap.parse(listOf("頌\t颂", "讚\t赞", "乾\t乾干", "祂\t祂"))

    private fun findsT2s(query: String, text: String) = SearchPattern.build(query, t2s)!!.matcher(text).find()

    @Test
    fun traditionalQueryMatchesSimplifiedText() {
        assertThat(findsT2s("頌讚", "颂赞三一神")).isTrue()
    }

    @Test
    fun mixedScriptQuery() {
        assertThat(findsT2s("颂讚", "颂赞三一神")).isTrue()
    }

    @Test
    fun ambiguousCharMatchesEveryCandidate() {
        assertThat(findsT2s("乾", "干净")).isTrue()
        assertThat(findsT2s("乾", "乾坤")).isTrue()
    }

    @Test
    fun heRuleStillAppliesWithMap() {
        assertThat(findsT2s("他", "祂")).isTrue()
    }

    @Test
    fun metaCharactersStillLiteralWithMap() {
        assertThat(findsT2s("(頌", "(颂")).isTrue()
    }
}
