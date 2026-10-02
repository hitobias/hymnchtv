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
}
