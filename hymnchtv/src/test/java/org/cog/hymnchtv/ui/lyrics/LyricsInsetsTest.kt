package org.cog.hymnchtv.ui.lyrics

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Plan 6c: the lyrics layer padding follows the overlays that are shown right now. */
class LyricsInsetsTest {
    @Test
    fun topPaddingIsTheTopBarOnlyWhileItIsShown() {
        assertThat(LyricsInsets.padding(48, true, 52, true, 0, 0, 8).top).isEqualTo(48)
        assertThat(LyricsInsets.padding(48, false, 52, true, 0, 0, 8).top).isEqualTo(0)
    }

    @Test
    fun bottomPaddingAddsEveryShownPieceAndTheExtra() {
        assertThat(LyricsInsets.padding(48, true, 52, true, 100, 24, 8).bottom).isEqualTo(52 + 100 + 24 + 8)
    }

    @Test
    fun hiddenButtonsShrinkBackToSafeAreaPlusExtra() {
        assertThat(LyricsInsets.padding(48, true, 52, false, 0, 24, 8).bottom).isEqualTo(24 + 8)
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
