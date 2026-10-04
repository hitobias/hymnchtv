package org.cog.hymnchtv.ui.host

import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.reading.background.BackgroundChoice
import org.cog.hymnchtv.reading.background.BackgroundPolicy
import org.cog.hymnchtv.reading.background.BackgroundPreset
import org.cog.hymnchtv.reading.background.UiTokens
import org.cog.hymnchtv.reading.background.Wcag
import org.junit.Test

/** The frame around the tabs (toolbar, bottom navigation) must stay readable on every home background. */
class MainChromeTest {
    private val choices: List<Pair<String, BackgroundChoice>> =
        BackgroundPreset.entries.map { it.id to BackgroundChoice.Preset(it) } + ("photo" to BackgroundChoice.Photo)

    @Test
    fun barTextAndSelectedTabAreReadableOnEveryBackground() {
        for ((name, choice) in choices) {
            val input = BackgroundPolicy.tokenInput(choice)
            val c = MainChrome.colors(input, UiTokens.from(input))
            assertThat(Wcag.contrast(c.onBar, c.bar)).isAtLeast(UiTokens.MIN_TEXT_CONTRAST)
            // Opaque muted text over the bar: what the unselected tab label really shows
            assertThat(Wcag.contrast(UiTokens.over(c.bar, c.onBarMuted), c.bar)).isAtLeast(UiTokens.MIN_TEXT_CONTRAST)
            assertThat(Wcag.contrast(c.accent, c.bar)).isAtLeast(UiTokens.MIN_GRAPHIC_CONTRAST)
            assertThat(Wcag.contrast(c.accent, c.indicator)).isAtLeast(UiTokens.MIN_GRAPHIC_CONTRAST)
            assertThat(c.bar ushr 24).isEqualTo(0xFF)
            assertThat(c.isDark).isEqualTo(input.isDark)
            println("chrome ok: $name")
        }
    }

    @Test
    fun keyboardLiftsTheFrameAndTheNavigationOnlyPadsForTheGestureAreaWithoutIt() {
        assertThat(MainChrome.frameInsets(0, 80, 0, 48, 0)).isEqualTo(FrameInsets(0, 80, 0, 0, 48))
        assertThat(MainChrome.frameInsets(0, 80, 0, 48, 600)).isEqualTo(FrameInsets(0, 80, 0, 600, 0))
        assertThat(MainChrome.frameInsets(-1, -1, -1, -1, 0)).isEqualTo(FrameInsets(0, 0, 0, 0, 0))
    }

    @Test
    fun homeBarIconsAndTitleAreReadableOnTheBackgroundTheyAreDrawnOn() {
        for ((name, choice) in choices) {
            val input = BackgroundPolicy.tokenInput(choice)
            val tokens = UiTokens.from(input)
            val c = MainChrome.homeColors(input, tokens)
            for (swatch in input.swatches) {
                val seen = UiTokens.over(opaqueOf(swatch), c.bar)
                assertThat(Wcag.contrast(c.onBar, seen)).isAtLeast(UiTokens.MIN_TEXT_CONTRAST)
            }
            if (!input.isPhoto) assertThat(c.bar ushr 24).isEqualTo(0)
            println("home bar ok: $name")
        }
    }

    private fun opaqueOf(rgb: Int) = (0xFF shl 24) or (rgb and 0xFFFFFF)
}
