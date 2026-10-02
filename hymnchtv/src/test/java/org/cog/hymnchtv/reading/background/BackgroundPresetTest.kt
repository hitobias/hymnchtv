package org.cog.hymnchtv.reading.background

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

class BackgroundPresetTest {
    private fun hex(c: Int) = "#%06x".format(c and 0xFFFFFF)

    @Test
    fun twentyEightPresetsWithUniqueLowercaseIds() {
        assertThat(BackgroundPreset.entries).hasSize(28)
        assertThat(BackgroundPreset.entries.map { it.id }.toSet()).hasSize(28)
        BackgroundPreset.entries.forEach { assertThat(it.id).matches("[a-z]+(_[a-z]+)?") }
    }

    @Test
    fun categoriesMatchTheDesign() {
        val counts = BackgroundPreset.entries.groupingBy { it.category }.eachCount()
        assertThat(counts).containsExactly(
            BackgroundCategory.READING, 8, BackgroundCategory.CALM, 4, BackgroundCategory.GRADIENT, 5,
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
    fun readingColoursAreFirstAndPlain() {
        val reading = BackgroundPreset.entries.take(8)
        assertThat(reading.map { it.category }.toSet()).containsExactly(BackgroundCategory.READING)
        assertThat(reading.map { it.id }).containsExactly(
            "paper_white", "parchment_beige", "eye_green", "pale_blue", "pale_pink", "soft_grey", "dim_grey", "true_black",
        ).inOrder()
        reading.forEach { assertThat(it.overlays).isEmpty() }
        assertThat(reading.filter { it.isDark }.map { it.id }).containsExactly("dim_grey", "true_black")
    }

    @Test
    fun readingColourValuesMatchTheSpecTable() {
        val expected = mapOf(
            "paper_white" to Triple("#ffffff", "#222222", "#2f5d8a"),
            "parchment_beige" to Triple("#f5ecd7", "#3b3226", "#8a5a1f"),
            "eye_green" to Triple("#cfe8cf", "#1f2e22", "#2f6b3d"),
            "pale_blue" to Triple("#e3edf6", "#1e2a36", "#2c5f93"),
            "pale_pink" to Triple("#f7e8ea", "#2e2326", "#9b3b55"),
            "soft_grey" to Triple("#ececec", "#262626", "#4a4f57"),
            "dim_grey" to Triple("#2b2b2d", "#d8d8da", "#9fc2ff"),
            "true_black" to Triple("#000000", "#b8b8b8", "#e3b77a"),
        )
        expected.forEach { (id, v) ->
            val p = BackgroundPreset.fromId(id)!!
            assertThat(Triple(hex(p.baseColor), hex(p.textColor), hex(p.accentColor))).isEqualTo(v)
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
