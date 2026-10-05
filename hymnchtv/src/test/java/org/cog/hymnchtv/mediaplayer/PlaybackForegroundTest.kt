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

    @Test
    fun theForegroundLingersOnlyWhenTheLastPlayerCompleted() {
        assertThat(PlaybackForeground.lingersAfterCompletion(playersLeft = 0, recording = false)).isTrue()
        assertThat(PlaybackForeground.lingersAfterCompletion(playersLeft = 1, recording = false)).isFalse()
        assertThat(PlaybackForeground.lingersAfterCompletion(playersLeft = 0, recording = true)).isFalse()
        assertThat(PlaybackForeground.LINGER_MS).isEqualTo(5_000L)
    }
}
