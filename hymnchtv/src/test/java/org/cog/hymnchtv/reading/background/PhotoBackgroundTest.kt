package org.cog.hymnchtv.reading.background

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PhotoBackgroundTest {
    @Test
    fun dimIsClampedPercentOfOpaque() {
        assertThat(PhotoBackground.dimAlpha(40)).isEqualTo(102)
        assertThat(PhotoBackground.dimAlpha(0)).isEqualTo(PhotoBackground.dimAlpha(PhotoBackground.DIM_MIN))
        assertThat(PhotoBackground.dimAlpha(100)).isEqualTo(204)
    }

    @Test
    fun blurIsClampedAndScaledByDensity() {
        assertThat(PhotoBackground.blurRadiusPx(10, 3f)).isEqualTo(30f)
        assertThat(PhotoBackground.blurRadiusPx(99, 2f)).isEqualTo(50f)
        assertThat(PhotoBackground.blurRadiusPx(-1, 2f)).isEqualTo(0f)
    }

    @Test
    fun sampleSizeKeepsBothSidesAtLeastTheScreen() {
        assertThat(PhotoBackground.sampleSize(4320, 9600, 1080, 2400)).isEqualTo(4)
        assertThat(PhotoBackground.sampleSize(4000, 3000, 1080, 2400)).isEqualTo(1)
        assertThat(PhotoBackground.sampleSize(1080, 2400, 1080, 2400)).isEqualTo(1)
    }

    @Test
    fun sampleSizeIgnoresBrokenSizes() {
        assertThat(PhotoBackground.sampleSize(0, 100, 10, 10)).isEqualTo(1)
        assertThat(PhotoBackground.sampleSize(100, 100, 0, 10)).isEqualTo(1)
    }

    @Test
    fun boundedSampleSizeCapsPixelsAtTwiceTheScreen() {
        // landscape photo on a portrait screen: the both-sides rule alone would keep all 36 Mpx
        assertThat(PhotoBackground.sampleSize(8000, 4500, 1080, 2400)).isEqualTo(1)
        assertThat(PhotoBackground.boundedSampleSize(8000, 4500, 1080, 2400)).isEqualTo(4)
        assertThat(PhotoBackground.boundedSampleSize(4000, 3000, 1080, 2400)).isEqualTo(2)
        assertThat(PhotoBackground.boundedSampleSize(4000, 2250, 1080, 2400)).isEqualTo(2)
        assertThat(PhotoBackground.boundedSampleSize(4320, 9600, 1080, 2400)).isEqualTo(4)
        assertThat(PhotoBackground.boundedSampleSize(1200, 2600, 1080, 2400)).isEqualTo(1)
        assertThat(PhotoBackground.boundedSampleSize(0, 100, 10, 10)).isEqualTo(1)
        for ((w, h) in listOf(8000 to 4500, 4000 to 3000, 12000 to 9000)) {
            val size = PhotoBackground.boundedSampleSize(w, h, 1080, 2400)
            assertThat((w / size).toLong() * (h / size)).isAtMost(2L * 1080 * 2400)
        }
    }
}
