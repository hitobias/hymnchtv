package org.cog.hymnchtv.ui.lyrics

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Plan 6c: the lyrics layer padding follows the overlays that are shown right now. */
class LyricsInsetsTest {
    @Test
    fun topPaddingIsTheTopCapsuleBottomEdgeWhileShown() {
        assertThat(LyricsInsets.padding(106, true, 100, true, 0, 0, 8).top).isEqualTo(106) // 5dp + 48dp at density 2
        assertThat(LyricsInsets.padding(106, false, 100, true, 0, 0, 8).top).isEqualTo(0)
    }

    @Test
    fun bottomPaddingIsTheHigherOfPlayerAndCapsulePlusExtra() {
        // capsule beside the player: the player (reserve 144 + system 48) is higher than the capsule (136)
        assertThat(LyricsInsets.padding(106, true, 136, true, 144, 48, 16).bottom).isEqualTo(144 + 48 + 16)
        // capsule above the card: the capsule is higher
        assertThat(LyricsInsets.padding(106, true, 552, true, 400, 48, 16).bottom).isEqualTo(552 + 16)
    }

    @Test
    fun hiddenCapsuleShrinksBackToPlayerPlusExtra() {
        assertThat(LyricsInsets.padding(106, true, 552, false, 400, 48, 16).bottom).isEqualTo(400 + 48 + 16)
    }

    @Test
    fun negativeInputsAreTreatedAsZero() {
        val p = LyricsInsets.padding(-1, true, -5, true, -9, -3, 8)
        assertThat(p.top).isEqualTo(0)
        assertThat(p.bottom).isEqualTo(8)
    }

    @Test
    fun scrollKeepsTheReadingPositionWhenTheTopPaddingChanges() {
        assertThat(LyricsInsets.scrollAfterTopPaddingChange(scrollY = 300, oldTop = 0, newTop = 48)).isEqualTo(348)
        assertThat(LyricsInsets.scrollAfterTopPaddingChange(scrollY = 300, oldTop = 48, newTop = 0)).isEqualTo(252)
        assertThat(LyricsInsets.scrollAfterTopPaddingChange(scrollY = 10, oldTop = 48, newTop = 0)).isEqualTo(0)
        assertThat(LyricsInsets.scrollAfterTopPaddingChange(scrollY = 0, oldTop = 0, newTop = 48)).isEqualTo(0)
    }
}
