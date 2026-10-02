package org.cog.hymnchtv.persistance

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HistoryPruneTest {
    @Test
    fun noPurgeUpToTheLimit() {
        for (count in listOf(0, 1, 199, 200)) {
            assertThat(HistoryPrune.pivotIndex(count, 200)).isNull()
        }
    }

    @Test
    fun oneOverTheLimitPivotsOnTheEleventhOldest() {
        assertThat(HistoryPrune.pivotIndex(201, 200)).isEqualTo(11)
    }

    @Test
    fun pivotGrowsWithTheExcess() {
        assertThat(HistoryPrune.pivotIndex(1000, 200)).isEqualTo(810)
        assertThat(HistoryPrune.pivotIndex(205, 200)).isEqualTo(15)
    }

    @Test
    fun purgeKeepsLimitMinusMarginPlusOneRowsWhenTimestampsAreDistinct() {
        for (count in listOf(201, 250, 1000)) {
            val pivot = HistoryPrune.pivotIndex(count, 200)!!
            val kept = count - (pivot - 1)
            assertThat(kept).isEqualTo(200 - HistoryPrune.MARGIN + 1)
        }
    }

    @Test
    fun zeroLimitPurgesEverythingButTheMarginRows() {
        assertThat(HistoryPrune.pivotIndex(3, 0)).isEqualTo(13)
    }
}
