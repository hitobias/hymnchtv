package org.cog.hymnchtv.mediaplayer

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlaybackForegroundTest {
    @Test
    fun anythingPlayingKeepsTheServiceInTheForeground() {
        assertThat(PlaybackForeground.of(playing = 1, pausedMidway = 0)).isEqualTo(PlaybackForeground.PLAYING)
        // the melody and the accompaniment play as two players
        assertThat(PlaybackForeground.of(playing = 2, pausedMidway = 1)).isEqualTo(PlaybackForeground.PLAYING)
    }

    @Test
    fun aPauseMidwayStaysInTheForegroundSoTheNotificationCanResume() {
        assertThat(PlaybackForeground.of(playing = 0, pausedMidway = 2)).isEqualTo(PlaybackForeground.PAUSED)
    }

    @Test
    fun stoppedOrOnlyInspectedLeavesTheForeground() {
        assertThat(PlaybackForeground.of(playing = 0, pausedMidway = 0)).isEqualTo(PlaybackForeground.NONE)
    }
}
