package org.cog.hymnchtv.search

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class T2sMapTest {
    @Test
    fun parsesLinesAndSkipsMalformed() {
        val map = T2sMap.parse(listOf("頌\t颂", "乾\t乾干", "", "bad", "x\t", "ab\tc"))
        assertThat(map.candidates("頌")).isEqualTo("颂")
        assertThat(map.candidates("乾")).isEqualTo("乾干")
        assertThat(map.candidates("神")).isNull()
        assertThat(map.size).isEqualTo(2)
    }

    @Test
    fun realAssetMapsCommonTraditionalChars() {
        val assets = File(checkNotNull(System.getProperty("hymnchtv.assetsDir")))
        val map = T2sMap.parse(File(assets, "lyrics_t2s_map.txt").readLines())
        assertThat(map.candidates("讚")).contains("赞")
        assertThat(map.candidates("劃")).contains("划")
        assertThat(map.candidates("裡")).contains("里")
    }

    @Test
    fun recoveryVersionAndTaiwanFormsBothFindSimplifiedLyrics() {
        val assets = File(checkNotNull(System.getProperty("hymnchtv.assetsDir")))
        val map = T2sMap.parse(File(assets, "lyrics_t2s_map.txt").readLines())
        // The lyrics show the Recovery Version's 裏; people type Taiwan's 裡. Search runs on the Simplified text.
        for (query in listOf("在灵裏", "在靈裡", "在灵里")) {
            assertThat(SearchPattern.build(query, map)!!.matcher("活在灵里面").find()).isTrue()
        }
    }

    @Test
    fun realAssetMapIsConsistentWithSimplifiedSources() {
        val assets = File(checkNotNull(System.getProperty("hymnchtv.assetsDir")))
        val sourceText = assets.listFiles { f -> f.isDirectory && Regex("lyrics_[a-z]+_text").matches(f.name) }!!
            .flatMap { dir -> dir.listFiles { f -> f.name.endsWith(".txt") }!!.toList() }
            .joinToString("") { it.readText(Charsets.UTF_8) }
        val sourceChars = sourceText.codePoints().toArray().map { String(Character.toChars(it)) }.toSet()
        val entries = File(assets, "lyrics_t2s_map.txt").readLines().filter { it.isNotEmpty() }.map { it.split('\t') }
        assertThat(entries).isNotEmpty()
        for ((key, candidates) in entries) {
            val chars = candidates.codePoints().toArray().map { String(Character.toChars(it)) }
            if (key in sourceChars) assertThat(chars).contains(key)
            assertThat(sourceChars).containsAtLeastElementsIn(chars)
        }
    }
}
