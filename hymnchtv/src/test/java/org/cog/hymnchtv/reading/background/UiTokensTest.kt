package org.cog.hymnchtv.reading.background

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/** The contrast matrix of the visual redesign spec, section 4 and 7.1. */
class UiTokensTest {
    private fun hex(c: Int) = "#%08x".format(c)

    private val choices: List<Pair<String, BackgroundChoice>> =
        BackgroundPreset.entries.map { it.id to BackgroundChoice.Preset(it) } +
            ("photo" to BackgroundChoice.Photo) +
            BackgroundSlot.entries.flatMap { slot ->
                listOf(false, true).map { dark ->
                    "default-${slot.name}-dark=$dark" to BackgroundChoice.Preset(BackgroundPolicy.defaultFor(slot, dark))
                }
            }

    private fun forEachSwatch(block: (name: String, input: TokenInput, t: UiTokens, swatch: Int) -> Unit) {
        for ((name, choice) in choices) {
            val input = BackgroundPolicy.tokenInput(choice)
            val tokens = UiTokens.from(input)
            input.swatches.forEach { block(name, input, tokens, it) }
        }
    }

    @Test
    fun onSurfaceMeetsAaOnEverySurfaceKind() = forEachSwatch { name, _, t, s ->
        UiTokens.backdropsOver(s, t.surface, t.surfaceTone).forEach { b ->
            assertWithMessage("$name: onSurface ${hex(t.onSurface)} on ${hex(b)}").that(Wcag.contrast(t.onSurface, b)).isAtLeast(4.5)
        }
    }

    @Test
    fun onSurfaceMutedMeetsAa() = forEachSwatch { name, _, t, s ->
        UiTokens.backdropsOver(s, t.surface, t.surfaceTone).forEach { b ->
            val drawn = UiTokens.over(b, t.onSurfaceMuted)
            assertWithMessage("$name: muted ${hex(t.onSurfaceMuted)} on ${hex(b)}").that(Wcag.contrast(drawn, b)).isAtLeast(4.5)
        }
    }

    @Test
    fun accentIsGraphicContrastAndOnAccentIsAa() = forEachSwatch { name, _, t, s ->
        UiTokens.backdropsOver(s, t.surface, t.surfaceTone).forEach { b ->
            assertWithMessage("$name: accent ${hex(t.accent)} on ${hex(b)}").that(Wcag.contrast(t.accent, b)).isAtLeast(3.0)
        }
        assertWithMessage("$name: onAccent on accent").that(Wcag.contrast(t.onAccent, t.accent)).isAtLeast(4.5)
    }

    @Test
    fun onOutlineActionMeetsAaOnSurface() = forEachSwatch { name, _, t, s ->
        val card = UiTokens.over(s, t.surface)
        assertWithMessage("$name: ${hex(t.onOutlineAction)} on ${hex(card)}").that(Wcag.contrast(t.onOutlineAction, card)).isAtLeast(4.5)
    }

    @Test
    fun disabledTextIsAtLeastThreeToOneButDimmerThanActive() = forEachSwatch { name, _, t, s ->
        val card = UiTokens.over(s, t.disabledSurface)
        val onBar = UiTokens.over(card, t.disabledSurface)
        listOf(card, onBar).forEach {
            assertWithMessage("$name: disabled ${hex(t.disabledOnSurface)} on ${hex(it)}").that(Wcag.contrast(t.disabledOnSurface, it)).isAtLeast(3.0)
            assertWithMessage("$name: disabled is dimmer than active").that(Wcag.contrast(t.disabledOnSurface, it))
                .isAtMost(Wcag.contrast(t.onSurface, it))
        }
    }

    @Test
    fun outlineIsThreeToOneAgainstAdjacentSurfaces() = forEachSwatch { name, _, t, s ->
        UiTokens.backdropsOver(s, t.surface, t.surfaceTone).forEach { b ->
            assertWithMessage("$name: outline ${hex(t.outline)} on ${hex(b)}").that(Wcag.contrast(UiTokens.over(b, t.outline), b)).isAtLeast(3.0)
        }
    }

    @Test
    fun disabledSurfaceIsFlatSurfaceAndToneDiffersFromIt() = forEachSwatch { name, _, t, _ ->
        assertThat(t.disabledSurface).isEqualTo(t.surface)
        assertWithMessage(name).that(t.surfaceTone).isNotEqualTo(t.surface)
    }

    @Test
    fun surfaceIsTranslucentWithTheSpecifiedBaseAlpha() {
        for ((name, choice) in choices) {
            val t = UiTokens.from(BackgroundPolicy.tokenInput(choice))
            val a = (t.surface ushr 24) / 255f
            assertWithMessage(name).that(a).isAtLeast(if (choice == BackgroundChoice.Photo) 0.87f else 0.91f)
            assertWithMessage(name).that(a).isAtMost(1f)
        }
    }

    @Test
    fun photoSurfaceIsTheDarkPanel() {
        val t = UiTokens.from(BackgroundPolicy.tokenInput(BackgroundChoice.Photo))
        assertThat(t.surface and 0xFFFFFF).isEqualTo(0x1E1E1E)
    }

    @Test
    fun darkBackgroundInLightAppStillUsesDarkSurfaces() {
        // The user's chosen background decides, not DayNight: NIGHTREAD has a dark surface whatever the system mode
        val t = UiTokens.from(BackgroundPolicy.tokenInput(BackgroundChoice.Preset(BackgroundPreset.NIGHTREAD)))
        assertThat(Wcag.luminance(t.surface)).isLessThan(0.2)
        assertThat(Wcag.luminance(t.onSurface)).isGreaterThan(0.5)
    }

    @Test
    fun swatchesCoverEveryOverlayCombination() {
        val olive = BackgroundPreset.OLIVE
        assertThat(olive.swatches()).hasSize(4)
        assertThat(BackgroundPreset.MIST.swatches()).containsExactly(BackgroundPreset.MIST.stops[0])
        assertThat(BackgroundPreset.DAWN.swatches()).hasSize(3)
        // both layers together are painted in order over the stop
        val first = Wcag.blend(olive.stops[0], olive.overlays[0].color, olive.overlays[0].maxAlpha)
        val both = Wcag.blend(first, olive.overlays[1].color, olive.overlays[1].maxAlpha)
        assertThat(olive.swatches()).contains(both)
    }

    @Test
    fun photoSwatchesSpanBlackToWhite() {
        val s = BackgroundPolicy.tokenInput(BackgroundChoice.Photo).swatches
        assertThat(s).contains(0xFF000000.toInt())
        assertThat(s).contains(0xFFFFFFFF.toInt())
    }

    @Test
    fun requiresSwatches() {
        val e = runCatching { TokenInput(0, 0, 0, false, false, emptyList()) }.exceptionOrNull()
        assertThat(e).isInstanceOf(IllegalArgumentException::class.java)
    }
}
