package org.cog.hymnchtv.reading.background

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MainScreenColorsTest {
    private val darkGrey = 0xFF212121.toInt()
    private val red = 0xFFFF0000.toInt()

    @Test
    fun preferredColourIsKeptWhenReadable() {
        val dawn = BackgroundPolicy.palette(BackgroundChoice.Preset(BackgroundPreset.DAWN))
        assertThat(MainScreenColors.textColor(darkGrey, dawn)).isEqualTo(darkGrey)
    }

    @Test
    fun unreadablePreferredColourFallsBackToPaletteText() {
        val night = BackgroundPolicy.palette(BackgroundChoice.Preset(BackgroundPreset.NIGHTREAD))
        assertThat(MainScreenColors.textColor(darkGrey, night)).isEqualTo(night.textColor)
        assertThat(MainScreenColors.textColor(darkGrey, BackgroundPolicy.PHOTO_PALETTE))
            .isEqualTo(BackgroundPolicy.PHOTO_PALETTE.textColor)
    }

    @Test
    fun resultAlwaysMeetsContrastOnEveryPalette() {
        val palettes = BackgroundPreset.entries.map { BackgroundPolicy.palette(BackgroundChoice.Preset(it)) } +
            BackgroundPolicy.PHOTO_PALETTE
        for (p in palettes) for (preferred in listOf(darkGrey, red, 0xFFFFFFFF.toInt())) {
            val c = MainScreenColors.textColor(preferred, p)
            assertThat(Wcag.contrast(c, p.paperColor)).isAtLeast(MainScreenColors.MIN_TEXT_CONTRAST)
        }
    }

    @Test
    fun hintIsTextAtSixtyPercentAlpha() {
        assertThat(MainScreenColors.hintColor(0xFFF2EFE8.toInt())).isEqualTo(0x99F2EFE8.toInt())
    }
}
