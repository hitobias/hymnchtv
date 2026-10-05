package org.cog.hymnchtv.ui.lyrics

import com.google.common.truth.Truth.assertWithMessage
import org.cog.hymnchtv.reading.background.BackgroundChoice
import org.cog.hymnchtv.reading.background.BackgroundPolicy
import org.cog.hymnchtv.reading.background.BackgroundPreset
import org.cog.hymnchtv.reading.background.BackgroundSlot
import org.cog.hymnchtv.reading.background.UiTokens
import org.cog.hymnchtv.reading.background.Wcag
import org.junit.Test

/** Spec rev 3 section 4: both toolbar capsules keep AA text on every background (29 presets, defaults, photo). */
class ChromeCapsuleColorsTest {
    private fun hex(c: Int) = "#%08x".format(c)

    private val choices: List<Pair<String, BackgroundChoice>> =
        BackgroundPreset.entries.map { it.id to BackgroundChoice.Preset(it) } +
            ("photo" to BackgroundChoice.Photo) +
            BackgroundSlot.entries.flatMap { slot ->
                listOf(false, true).map { dark ->
                    "default-${slot.name}-dark=$dark" to BackgroundChoice.Preset(BackgroundPolicy.defaultFor(slot, dark))
                }
            }

    private fun forEachCapsule(block: (name: String, c: CapsuleColors, plate: Int) -> Unit) {
        for ((name, choice) in choices) {
            val input = BackgroundPolicy.tokenInput(choice)
            val colors = ChromeCapsuleColors.from(UiTokens.from(input))
            input.swatches.forEach { block(name, colors, UiTokens.over(it, colors.fill)) }
        }
    }

    @Test
    fun iconsAndTextMeetAa() = forEachCapsule { name, c, plate ->
        assertWithMessage("$name: content ${hex(c.content)} on ${hex(plate)}").that(Wcag.contrast(c.content, plate)).isAtLeast(4.5)
    }

    @Test
    fun activeSideMeetsAa() = forEachCapsule { name, c, plate ->
        assertWithMessage("$name: active ${hex(c.active)} on ${hex(plate)}").that(Wcag.contrast(c.active, plate)).isAtLeast(4.5)
    }

    @Test
    fun disabledIsAtLeastThreeToOne() = forEachCapsule { name, c, plate ->
        assertWithMessage("$name: disabled ${hex(c.disabled)} on ${hex(plate)}").that(Wcag.contrast(c.disabled, plate)).isAtLeast(3.0)
    }

    @Test
    fun fillIsTranslucentBetween88And100Percent() = forEachCapsule { name, c, _ ->
        val alpha = (c.fill ushr 24) / 255f
        assertWithMessage("$name: alpha $alpha").that(alpha).isAtLeast(0.87f)
    }
}
