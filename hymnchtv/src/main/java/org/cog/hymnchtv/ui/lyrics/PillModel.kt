package org.cog.hymnchtv.ui.lyrics

import org.cog.hymnchtv.reading.DisplayMode

/**
 * What the bottom capsule needs to know about the page.
 * @param shownHasLyrics whether the mode actually shown includes the lyrics text (false in score only)
 */
data class PillState(
    val traditional: Boolean,
    val englishShown: Boolean,
    val hasEnglish: Boolean,
    val mode: DisplayMode,
    val shownHasLyrics: Boolean,
    val showModeText: Boolean,
)

/** One "A/B" item: [active] is the side in use (0 first, 1 second) or -1 for none. */
data class PairItem(val enabled: Boolean, val active: Int)

enum class ModeIcon { LYRICS, SCORE, BOTH }

/** The three items of the bottom capsule (spec rev 3 section 3). Immutable. */
data class PillModel(val script: PairItem, val language: PairItem, val modeIcon: ModeIcon, val showModeText: Boolean) {
    companion object {
        private const val NONE = -1

        @JvmStatic
        fun from(s: PillState): PillModel {
            val lyrics = s.shownHasLyrics
            val script = if (!lyrics) PairItem(false, NONE) else PairItem(true, if (s.englishShown) NONE else if (s.traditional) 0 else 1)
            val languageUsable = lyrics && s.hasEnglish
            val language = if (!languageUsable) PairItem(false, NONE) else PairItem(true, if (s.englishShown) 1 else 0)
            val icon = when (s.mode) {
                DisplayMode.LYRICS_ONLY -> ModeIcon.LYRICS
                DisplayMode.SCORE_ONLY -> ModeIcon.SCORE
                DisplayMode.SCORE_AND_LYRICS -> ModeIcon.BOTH
            }
            return PillModel(script, language, icon, s.showModeText)
        }
    }
}
