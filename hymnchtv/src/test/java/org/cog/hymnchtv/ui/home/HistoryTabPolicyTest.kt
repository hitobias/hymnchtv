package org.cog.hymnchtv.ui.home

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HistoryTabPolicyTest {
    @Test fun knownValuesMapToTheirTab() {
        assertThat(HistoryTab.fromPref(0)).isEqualTo(HistoryTab.RECENT)
        assertThat(HistoryTab.fromPref(1)).isEqualTo(HistoryTab.FAVORITES)
        assertThat(HistoryTab.fromPref(2)).isEqualTo(HistoryTab.PLAYLISTS)
    }

    @Test fun unknownOrMissingFallsBackToRecent() {
        assertThat(HistoryTab.fromPref(null)).isEqualTo(HistoryTab.RECENT)
        assertThat(HistoryTab.fromPref(7)).isEqualTo(HistoryTab.RECENT)
        assertThat(HistoryTab.fromPref(-1)).isEqualTo(HistoryTab.RECENT)
    }

    @Test fun prefValueRoundTrips() {
        HistoryTab.values().forEach { assertThat(HistoryTab.fromPref(it.pref)).isEqualTo(it) }
    }
}
