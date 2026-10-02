package org.cog.hymnchtv.reading.background

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WcagTest {
    private val white = 0xFFFFFFFF.toInt()
    private val black = 0xFF000000.toInt()

    @Test
    fun blackOnWhiteIs21() {
        assertThat(Wcag.contrast(black, white)).isWithin(0.01).of(21.0)
        assertThat(Wcag.contrast(white, black)).isWithin(0.01).of(21.0)
    }

    @Test
    fun sameColourIs1() {
        assertThat(Wcag.contrast(0xFF7A3B2E.toInt(), 0xFF7A3B2E.toInt())).isWithin(1e-9).of(1.0)
    }

    @Test
    fun knownPair() {
        // #2b2a28 on #f8f6f0 (宣紙白); 13.27 computed independently with the WCAG 2.x formula
        assertThat(Wcag.contrast(0xFF2B2A28.toInt(), 0xFFF8F6F0.toInt())).isWithin(0.01).of(13.27)
    }

    @Test
    fun blendIsOpaqueAndRounded() {
        assertThat(Wcag.blend(black, white, 0.5f)).isEqualTo(0xFF808080.toInt())
        assertThat(Wcag.blend(white, black, 0f)).isEqualTo(white)
    }
}
