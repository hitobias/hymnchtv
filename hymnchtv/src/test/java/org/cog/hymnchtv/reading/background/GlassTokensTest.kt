package org.cog.hymnchtv.reading.background

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/** The contrast matrix of the frosted player spec, sections 3 and 6: every preset, the reading defaults and the photo. */
class GlassTokensTest {
    private fun hex(c: Int) = "#%08x".format(c)

    private val choices: List<Pair<String, BackgroundChoice>> =
        BackgroundPreset.entries.map { it.id to BackgroundChoice.Preset(it) } +
            ("photo" to BackgroundChoice.Photo) +
            BackgroundSlot.entries.flatMap { slot ->
                listOf(false, true).map { dark ->
                    "default-${slot.name}-dark=$dark" to BackgroundChoice.Preset(BackgroundPolicy.defaultFor(slot, dark))
                }
            }

    /** What can sit behind the glass, per mode: the swatch, and the worst case of the text showing through. */
    private fun behind(mode: GlassMode, input: TokenInput, swatch: Int): List<Int> {
        val text = UiTokens.over(0xFF000000.toInt(), (0xFF shl 24) or (input.textColor and 0xFFFFFF))
        return when (mode) {
            GlassMode.BLUR -> listOf(swatch, Wcag.blend(swatch, text, 0.5f))
            GlassMode.FALLBACK -> listOf(swatch, text)
            GlassMode.OPAQUE -> listOf(swatch)
        }
    }

    private fun forEach(block: (name: String, mode: GlassMode, t: UiTokens, backdrops: List<Int>) -> Unit) {
        for ((name, choice) in choices) {
            val input = BackgroundPolicy.tokenInput(choice)
            for (mode in GlassMode.entries) {
                val t = UiTokens.glass(input, mode)
                for (swatch in input.swatches) {
                    // glass, a tone chip on the glass, and a tone chip on a tone chip (the lyrics top bar)
                    val b = behind(mode, input, swatch).flatMap { UiTokens.backdropsOver(it, t.surface, t.surfaceTone) }
                    block("$name/$mode", mode, t, b)
                }
            }
        }
    }

    @Test
    fun onSurfaceMeetsAaOnEveryLayer() = forEach { name, _, t, backdrops ->
        backdrops.forEach {
            assertWithMessage("$name: onSurface ${hex(t.onSurface)} on ${hex(it)}").that(Wcag.contrast(t.onSurface, it)).isAtLeast(4.5)
        }
    }

    @Test
    fun mutedMeetsAa() = forEach { name, _, t, backdrops ->
        backdrops.forEach {
            assertWithMessage("$name: muted on ${hex(it)}").that(Wcag.contrast(UiTokens.over(it, t.onSurfaceMuted), it)).isAtLeast(4.5)
        }
    }

    @Test
    fun accentIsGraphicContrastAndOnAccentIsAa() = forEach { name, _, t, backdrops ->
        backdrops.forEach {
            assertWithMessage("$name: accent ${hex(t.accent)} on ${hex(it)}").that(Wcag.contrast(t.accent, it)).isAtLeast(3.0)
        }
        assertWithMessage("$name: onAccent on accent").that(Wcag.contrast(t.onAccent, t.accent)).isAtLeast(4.5)
    }

    @Test
    fun outlineIsThreeToOne() = forEach { name, _, t, backdrops ->
        backdrops.forEach {
            assertWithMessage("$name: outline on ${hex(it)}").that(Wcag.contrast(UiTokens.over(it, t.outline), it)).isAtLeast(3.0)
        }
    }

    @Test
    fun onOutlineActionAndDisabledTextHold() {
        for ((name, choice) in choices) {
            val input = BackgroundPolicy.tokenInput(choice)
            for (mode in GlassMode.entries) {
                val t = UiTokens.glass(input, mode)
                for (swatch in input.swatches) for (bg in behind(mode, input, swatch)) {
                    val glass = UiTokens.over(bg, t.surface)
                    assertWithMessage("$name/$mode: onOutlineAction on ${hex(glass)}")
                        .that(Wcag.contrast(t.onOutlineAction, glass)).isAtLeast(4.5)
                    // an unavailable source chip is the disabled surface on the glass, and sits on the glass bar too
                    listOf(glass, UiTokens.over(glass, t.disabledSurface)).forEach {
                        assertWithMessage("$name/$mode: disabled on ${hex(it)}").that(Wcag.contrast(t.disabledOnSurface, it)).isAtLeast(3.0)
                    }
                }
            }
        }
    }

    @Test
    fun alphaIsWithinTheSpecifiedRange() {
        for ((name, choice) in choices) {
            val input = BackgroundPolicy.tokenInput(choice)
            val blur = UiTokens.glass(input, GlassMode.BLUR).surface ushr 24
            assertWithMessage(name).that(blur).isAtLeast(Math.round(0.72f * 255))
            assertWithMessage(name).that(blur).isAtMost(Math.round(0.94f * 255))
            assertThat(UiTokens.glass(input, GlassMode.FALLBACK).surface ushr 24).isEqualTo(Math.round(0.94f * 255))
            assertThat(UiTokens.glass(input, GlassMode.OPAQUE).surface ushr 24).isEqualTo(0xFF)
        }
    }

    @Test
    fun rgbIsTheSameInEveryMode() {
        for ((name, choice) in choices) {
            val input = BackgroundPolicy.tokenInput(choice)
            val rgb = UiTokens.from(input).surface and 0xFFFFFF
            GlassMode.entries.forEach { assertWithMessage("$name/$it").that(UiTokens.glass(input, it).surface and 0xFFFFFF).isEqualTo(rgb) }
        }
    }

    @Test
    fun tintFormulaIgnoresTheAlphaAlreadyInTheSurface() {
        assertThat(UiTokens.glassTint(0x80123456.toInt(), 0.72f)).isEqualTo((0xB8 shl 24) or 0x123456)
        assertThat(UiTokens.glassTint(0xFF123456.toInt(), 0.94f)).isEqualTo((0xF0 shl 24) or 0x123456)
        assertThat(UiTokens.opaqueGlassTint(0x12123456)).isEqualTo(0xFF123456.toInt())
    }

    @Test
    fun lightBackgroundsStartAtTheBlurAlpha() {
        val t = UiTokens.glass(BackgroundPolicy.tokenInput(BackgroundChoice.Preset(BackgroundPreset.MIST)), GlassMode.BLUR)
        assertThat(t.surface ushr 24).isAtLeast(Math.round(0.72f * 255))
    }
}
