package org.cog.hymnchtv.reading.background

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * Text on a photo is drawn on PHOTO_PALETTE.backdropColor. Blending is monotonic in each channel, so testing
 * every grey from black to white (both ends included) covers the worst case of any photo pixel.
 */
class PhotoPaletteTest {
    private val palette = BackgroundPolicy.PHOTO_PALETTE
    private val alpha = (palette.backdropColor ushr 24) / 255f

    private fun underPanel(grey: Int): Int {
        val photo = (0xFF shl 24) or (grey shl 16) or (grey shl 8) or grey
        return Wcag.blend(photo, palette.backdropColor, alpha)
    }

    @Test
    fun panelIsAtLeast85PercentOpaque() {
        assertThat(alpha).isAtLeast(0.85f)
    }

    @Test
    fun textAndAccentPassOnAnyPhotoPixel() {
        for (grey in (0..255 step 15) + 255) {
            val behind = underPanel(grey)
            assertWithMessage("text on grey $grey").that(Wcag.contrast(palette.textColor, behind)).isAtLeast(4.5)
            assertWithMessage("accent on grey $grey").that(Wcag.contrast(palette.accentColor, behind)).isAtLeast(3.0)
        }
    }
}
