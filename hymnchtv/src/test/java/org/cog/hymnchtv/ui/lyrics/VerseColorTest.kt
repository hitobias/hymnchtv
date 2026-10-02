package org.cog.hymnchtv.ui.lyrics

import com.google.common.truth.Truth.assertWithMessage
import org.cog.hymnchtv.reading.background.BackgroundPreset
import org.cog.hymnchtv.reading.background.Wcag
import org.junit.Test
import java.io.File

/**
 * The red verse numbers must stay readable (WCAG AA) on every light background a reader can pick. Dark backgrounds
 * (and photos, which sit on a dark panel) use the palette's accent colour, which the A2 palette tests already cover.
 */
class VerseColorTest {
    private val red: Int by lazy {
        val res = File(checkNotNull(System.getProperty("hymnchtv.resDir")))
        val hex = Regex("<color name=\"c_verse_red\">#([0-9a-fA-F]{8})</color>")
            .find(File(res, "values/colors.xml").readText())!!.groupValues[1]
        hex.toLong(16).toInt()
    }

    @Test
    fun readableOnEveryLightPresetSwatch() {
        BackgroundPreset.entries.filterNot { it.isDark }.forEach { preset ->
            preset.swatches().forEach { swatch ->
                val ratio = Wcag.contrast(red, swatch)
                assertWithMessage("${preset.id} swatch %08x vs verse red %08x = %.2f".format(swatch, red, ratio))
                    .that(ratio).isAtLeast(4.5)
            }
        }
    }

    @Test
    fun accentReadableOnDarkPresets() {
        // AA on the plain stops; the brightest star dot / overlay pixel of a few presets is a decoration a bold
        // single-line numeral only rarely meets, so there it only has to reach 3:1.
        BackgroundPreset.entries.filter { it.isDark }.forEach { preset ->
            preset.swatches().forEach { swatch ->
                val ratio = Wcag.contrast(preset.accentColor, swatch)
                val needed = if (swatch in preset.stops) 4.5 else 3.0
                assertWithMessage("${preset.id} swatch %08x vs accent = %.2f".format(swatch, ratio))
                    .that(ratio).isAtLeast(needed)
            }
        }
    }
}
