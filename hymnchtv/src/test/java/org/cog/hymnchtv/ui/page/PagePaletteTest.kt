package org.cog.hymnchtv.ui.page

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.cog.hymnchtv.reading.background.BackgroundChoice
import org.cog.hymnchtv.reading.background.BackgroundPolicy
import org.cog.hymnchtv.reading.background.BackgroundPreset
import org.cog.hymnchtv.reading.background.UiTokens
import org.cog.hymnchtv.reading.background.Wcag
import org.junit.Test

/** The colours of the settings and contents pages (spec 5a): derived from the home background, every text at least 4.5:1. */
class PagePaletteTest {
    private val choices: List<Pair<String, BackgroundChoice>> =
        BackgroundPreset.entries.map { it.id to BackgroundChoice.Preset(it) } + ("photo" to BackgroundChoice.Photo)

    private fun hex(c: Int) = "#%08x".format(c)

    private fun forEachPalette(block: (name: String, p: PagePalette) -> Unit) {
        for ((name, choice) in choices) block(name, PagePalette.from(BackgroundPolicy.tokenInput(choice)))
    }

    @Test
    fun everyColourIsOpaque() = forEachPalette { name, p ->
        listOf(p.page, p.card, p.onCard, p.muted, p.divider, p.category, p.accent, p.onAccent, p.switchOff).forEach {
            assertWithMessage("$name ${hex(it)}").that(it ushr 24).isEqualTo(0xFF)
        }
    }

    @Test
    fun textMeetsAaOnThePageAndOnTheCard() = forEachPalette { name, p ->
        for ((label, color) in listOf("onCard" to p.onCard, "muted" to p.muted, "category" to p.category)) {
            assertWithMessage("$name $label on card").that(Wcag.contrast(color, p.card)).isAtLeast(4.5)
            assertWithMessage("$name $label on page").that(Wcag.contrast(color, p.page)).isAtLeast(4.5)
        }
        assertWithMessage("$name onAccent").that(Wcag.contrast(p.onAccent, p.accent)).isAtLeast(4.5)
    }

    @Test
    fun graphicsMeetThreeToOne() = forEachPalette { name, p ->
        assertWithMessage("$name accent on card").that(Wcag.contrast(p.accent, p.card)).isAtLeast(3.0)
        assertWithMessage("$name divider on card").that(Wcag.contrast(p.divider, p.card)).isAtLeast(1.1)
    }

    @Test
    fun cardIsToldApartFromThePage() = forEachPalette { name, p ->
        assertWithMessage(name).that(p.card).isNotEqualTo(p.page)
    }

    @Test
    fun darkPresetPageIsTheBaseAndCardIsTheSurfaceOverIt() {
        val preset = BackgroundPreset.NIGHTREAD
        val input = BackgroundPolicy.tokenInput(BackgroundChoice.Preset(preset))
        val t = UiTokens.from(input)
        val p = PagePalette.from(input)
        assertThat(p.page).isEqualTo((0xFF shl 24) or (preset.baseColor and 0xFFFFFF))
        assertThat(p.card).isEqualTo(UiTokens.over(p.page, t.surface))
    }

    @Test
    fun lightPresetPageIsTheToneOverTheBaseAndCardIsTheSurfaceOverTheBase() {
        val preset = BackgroundPreset.PAPER_WHITE
        val input = BackgroundPolicy.tokenInput(BackgroundChoice.Preset(preset))
        val t = UiTokens.from(input)
        val base = (0xFF shl 24) or (preset.baseColor and 0xFFFFFF)
        val p = PagePalette.from(input)
        assertThat(p.page).isEqualTo(UiTokens.over(base, t.surfaceTone))
        assertThat(p.card).isEqualTo(UiTokens.over(base, t.surface))
    }

    @Test
    fun photoPageIsTheSolidSurfaceNotThePhoto() {
        val input = BackgroundPolicy.tokenInput(BackgroundChoice.Photo)
        val t = UiTokens.from(input)
        val p = PagePalette.from(input)
        assertThat(p.page).isEqualTo(UiTokens.over((0xFF shl 24) or (input.baseColor and 0xFFFFFF), t.surface))
        assertThat(p.isDark).isTrue()
    }
}
