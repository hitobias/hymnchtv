package org.cog.hymnchtv.ui.playlist

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlaylistOrderTest {
    private val ids = listOf("x", "y", "z")

    @Test fun anEmptyPlaylistHasNoButton() {
        assertThat(PlaylistOrder.button(emptyList(), null)).isNull()
    }

    @Test fun startsAtTheFirstThenOffersTheNextThenStartsAgain() {
        assertThat(PlaylistOrder.button(ids, null)).isEqualTo(PlayButton.Start)
        assertThat(PlaylistOrder.button(ids, "x")).isEqualTo(PlayButton.Next(1))
        assertThat(PlaylistOrder.button(ids, "y")).isEqualTo(PlayButton.Next(2))
        assertThat(PlaylistOrder.button(ids, "z")).isEqualTo(PlayButton.Restart)
    }

    @Test fun theLastOpenedItemIsFollowedByIdentityNotPosition() {
        // "x" was opened, then moved to the end: the next one is now none, so start again
        assertThat(PlaylistOrder.button(listOf("y", "z", "x"), "x")).isEqualTo(PlayButton.Restart)
        // the opened item was removed: back to the start
        assertThat(PlaylistOrder.button(listOf("y", "z"), "x")).isEqualTo(PlayButton.Start)
    }

    @Test fun targets() {
        assertThat(PlaylistOrder.target(PlayButton.Start)).isEqualTo(0)
        assertThat(PlaylistOrder.target(PlayButton.Restart)).isEqualTo(0)
        assertThat(PlaylistOrder.target(PlayButton.Next(2))).isEqualTo(2)
    }
}
