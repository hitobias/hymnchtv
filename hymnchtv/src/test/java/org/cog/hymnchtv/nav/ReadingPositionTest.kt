package org.cog.hymnchtv.nav

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReadingPositionTest {
    @Test fun sameHeightRestoresTheSameOffset() {
        assertThat(ReadingPosition(600, 2400).restoredY(2400)).isEqualTo(600)
    }

    @Test fun aTallerPageScalesTheOffset() {
        // the reader enlarged the text: the same verse is now twice as far down
        assertThat(ReadingPosition(600, 2400).restoredY(4800)).isEqualTo(1200)
        assertThat(ReadingPosition(600, 2400).restoredY(1200)).isEqualTo(300)
    }

    @Test fun unmeasuredHeightsKeepTheOffset() {
        assertThat(ReadingPosition(600, 0).restoredY(2400)).isEqualTo(600)
        assertThat(ReadingPosition(600, 2400).restoredY(0)).isEqualTo(600)
    }

    @Test fun ofCountsNegativeValuesAsZero() {
        assertThat(ReadingPosition.of(-5, -1)).isEqualTo(ReadingPosition.TOP)
        assertThat(ReadingPosition.of(10, 20)).isEqualTo(ReadingPosition(10, 20))
    }

    @Test fun encodeDecodeRoundTrip() {
        val p = ReadingPosition(123, 4567)
        assertThat(ReadingPosition.decode(p.encode())).isEqualTo(p)
    }

    @Test fun decodeRejectsDamagedText() {
        listOf(null, "", "1", "a,b", "1,2,3", "-1,5", "5,-1").forEach {
            assertThat(ReadingPosition.decode(it)).isNull()
        }
    }
}
