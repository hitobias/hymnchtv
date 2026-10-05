package org.cog.hymnchtv.ui.lyrics

import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.reading.DisplayMode
import org.junit.Test

class PillModelTest {
    private fun state(
        traditional: Boolean = true, englishShown: Boolean = false, hasEnglish: Boolean = true,
        mode: DisplayMode = DisplayMode.SCORE_AND_LYRICS, shownHasLyrics: Boolean = true, showModeText: Boolean = true,
    ) = PillState(traditional, englishShown, hasEnglish, mode, shownHasLyrics, showModeText)

    @Test
    fun chineseShownHighlightsScriptAndChinese() {
        val m = PillModel.from(state(traditional = true))
        assertThat(m.script).isEqualTo(PairItem(enabled = true, active = 0))
        assertThat(m.language).isEqualTo(PairItem(enabled = true, active = 0))
        assertThat(PillModel.from(state(traditional = false)).script.active).isEqualTo(1)
    }

    @Test
    fun englishShownHighlightsEnglishOnly() {
        val m = PillModel.from(state(englishShown = true))
        assertThat(m.script).isEqualTo(PairItem(enabled = true, active = -1))
        assertThat(m.language).isEqualTo(PairItem(enabled = true, active = 1))
    }

    @Test
    fun noEnglishDisablesTheLanguageItem() {
        val m = PillModel.from(state(hasEnglish = false))
        assertThat(m.language).isEqualTo(PairItem(enabled = false, active = -1))
        assertThat(m.script.enabled).isTrue()
    }

    @Test
    fun scoreOnlyDisablesBothPairs() {
        val m = PillModel.from(state(mode = DisplayMode.SCORE_ONLY, shownHasLyrics = false))
        assertThat(m.script).isEqualTo(PairItem(enabled = false, active = -1))
        assertThat(m.language).isEqualTo(PairItem(enabled = false, active = -1))
    }

    @Test
    fun modeIconFollowsTheChosenModeAndTextFollowsTheFlag() {
        assertThat(PillModel.from(state(mode = DisplayMode.LYRICS_ONLY)).modeIcon).isEqualTo(ModeIcon.LYRICS)
        assertThat(PillModel.from(state(mode = DisplayMode.SCORE_ONLY)).modeIcon).isEqualTo(ModeIcon.SCORE)
        assertThat(PillModel.from(state(mode = DisplayMode.SCORE_AND_LYRICS)).modeIcon).isEqualTo(ModeIcon.BOTH)
        assertThat(PillModel.from(state(showModeText = false)).showModeText).isFalse()
    }
}
