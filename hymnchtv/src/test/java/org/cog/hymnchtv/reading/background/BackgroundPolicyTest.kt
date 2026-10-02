package org.cog.hymnchtv.reading.background

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BackgroundPolicyTest {
    private fun preset(p: BackgroundPreset) = BackgroundChoice.Preset(p)

    @Test
    fun lightThemeDefaults() {
        assertThat(BackgroundPolicy.resolve(null, BackgroundSlot.LYRICS, false, false)).isEqualTo(preset(BackgroundPreset.XUAN))
        assertThat(BackgroundPolicy.resolve(null, BackgroundSlot.MAIN, false, false)).isEqualTo(preset(BackgroundPreset.DAWN))
    }

    @Test
    fun darkThemeDefaultsBothToNightReading() {
        BackgroundSlot.entries.forEach {
            assertThat(BackgroundPolicy.resolve(null, it, true, false)).isEqualTo(preset(BackgroundPreset.NIGHTREAD))
        }
    }

    @Test
    fun userChoiceWinsEvenInDarkTheme() {
        assertThat(BackgroundPolicy.resolve("olive", BackgroundSlot.LYRICS, true, false)).isEqualTo(preset(BackgroundPreset.OLIVE))
    }

    @Test
    fun invalidValuesFallBackToTheDefault() {
        listOf("", "bg0", "XUAN", "5").forEach {
            assertThat(BackgroundPolicy.resolve(it, BackgroundSlot.LYRICS, false, false)).isEqualTo(preset(BackgroundPreset.XUAN))
        }
    }

    @Test
    fun photoNeedsTheFile() {
        assertThat(BackgroundPolicy.resolve(BackgroundPolicy.PHOTO, BackgroundSlot.MAIN, false, true)).isEqualTo(BackgroundChoice.Photo)
        assertThat(BackgroundPolicy.resolve(BackgroundPolicy.PHOTO, BackgroundSlot.MAIN, false, false)).isEqualTo(preset(BackgroundPreset.DAWN))
    }

    @Test
    fun prefValueRoundTrips() {
        for (p in BackgroundPreset.entries) {
            val stored = BackgroundPolicy.prefValue(preset(p))
            assertThat(BackgroundPolicy.resolve(stored, BackgroundSlot.MAIN, true, false)).isEqualTo(preset(p))
        }
        val photo = BackgroundPolicy.prefValue(BackgroundChoice.Photo)
        assertThat(BackgroundPolicy.resolve(photo, BackgroundSlot.LYRICS, false, true)).isEqualTo(BackgroundChoice.Photo)
    }

    @Test
    fun paletteFollowsTheChoice() {
        val ink = BackgroundPolicy.palette(preset(BackgroundPreset.INK))
        assertThat(ink.isDark).isTrue()
        assertThat(ink.paperColor).isEqualTo(BackgroundPreset.INK.baseColor)
        assertThat(ink.textColor).isEqualTo(BackgroundPreset.INK.textColor)
        assertThat(ink.backdropColor).isEqualTo(0)
        assertThat(BackgroundPolicy.palette(BackgroundChoice.Photo)).isEqualTo(BackgroundPolicy.PHOTO_PALETTE)
        assertThat(BackgroundPolicy.PHOTO_PALETTE.isDark).isTrue()
    }

    @Test
    fun slotFromNameNeverThrows() {
        assertThat(BackgroundSlot.fromName("MAIN")).isEqualTo(BackgroundSlot.MAIN)
        assertThat(BackgroundSlot.fromName(null)).isEqualTo(BackgroundSlot.LYRICS)
        assertThat(BackgroundSlot.fromName("main")).isEqualTo(BackgroundSlot.LYRICS)
    }
}
