package org.cog.hymnchtv.reading.background

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

class BackgroundPresetTest {
    private fun hex(c: Int) = "#%06x".format(c and 0xFFFFFF)

    @Test
    fun twentyPresetsWithUniqueLowercaseIds() {
        assertThat(BackgroundPreset.entries).hasSize(20)
        assertThat(BackgroundPreset.entries.map { it.id }.toSet()).hasSize(20)
        BackgroundPreset.entries.forEach { assertThat(it.id).matches("[a-z]+") }
    }

    @Test
    fun categoriesMatchTheDesign() {
        val counts = BackgroundPreset.entries.groupingBy { it.category }.eachCount()
        assertThat(counts).containsExactly(
            BackgroundCategory.CALM, 4, BackgroundCategory.GRADIENT, 5,
            BackgroundCategory.MOTIF, 7, BackgroundCategory.NIGHT, 4,
        )
    }

    @Test
    fun textMeetsWcagAaOnEverySwatch() {
        for (p in BackgroundPreset.entries) {
            for (s in p.swatches()) {
                assertWithMessage("${p.id}: text ${hex(p.textColor)} on ${hex(s)}")
                    .that(Wcag.contrast(p.textColor, s)).isAtLeast(4.5)
            }
        }
    }

    @Test
    fun accentMeetsLargeTextContrastOnEverySwatch() {
        for (p in BackgroundPreset.entries) {
            for (s in p.swatches()) {
                assertWithMessage("${p.id}: accent ${hex(p.accentColor)} on ${hex(s)}")
                    .that(Wcag.contrast(p.accentColor, s)).isAtLeast(3.0)
            }
        }
    }

    @Test
    fun darkFlagMatchesBaseLuminance() {
        for (p in BackgroundPreset.entries) {
            assertWithMessage(p.id).that(p.isDark).isEqualTo(Wcag.luminance(p.baseColor) < 0.2)
        }
    }

    @Test
    fun fromIdIsTotal() {
        assertThat(BackgroundPreset.fromId("xuan")).isEqualTo(BackgroundPreset.XUAN)
        assertThat(BackgroundPreset.fromId("XUAN")).isNull()
        assertThat(BackgroundPreset.fromId("bg0")).isNull()
        assertThat(BackgroundPreset.fromId(null)).isNull()
    }
}
