package org.cog.hymnchtv.mediaconfig

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LyricsEnglishDbKeyTest {
    @Test
    fun daBenKeepsItsEnglishHymnNumber() {
        assertThat(LyricsEnglishRecord.dbHymnNo(18, false)).isEqualTo(18)
    }

    @Test
    fun erGeIsShiftedByTheOffsetSoItNeverCollidesWithDaBen() {
        assertThat(LyricsEnglishRecord.dbHymnNo(18, true)).isEqualTo(2018)
    }
}
