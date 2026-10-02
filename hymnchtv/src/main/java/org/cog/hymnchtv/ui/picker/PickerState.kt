package org.cog.hymnchtv.ui.picker

import org.cog.hymnchtv.hymn.HymnRef
import org.cog.hymnchtv.hymn.HymnSource

enum class Notice { NO_FU_IN_BOOK }

/** What the home entry holds: the chosen [source], the typed [digits], appendix mode and the chosen English candidate. Immutable. */
data class PickerState(
    val source: HymnSource = HymnSource.DB,
    val digits: String = "",
    val isFu: Boolean = false,
    val englishPick: Int = 0,
    val notice: Notice? = null,
)

/** The live preview derived from a [PickerState]. */
sealed interface Preview {
    object Empty : Preview

    data class Valid(val ref: HymnRef) : Preview

    data class Invalid(val source: HymnSource, val number: Int, val isFu: Boolean, val alsoIn: List<HymnSource>) : Preview

    data class English(val englishNo: Int, val candidates: List<HymnRef>, val pick: Int) : Preview {
        val target: HymnRef get() = candidates[pick]
    }

    data class NoCounterpart(val englishNo: Int) : Preview
}

enum class PickerMode { HOME, JUMP }

/** Which buttons the picker shows in which mode. */
data class PickerChrome(val showToc: Boolean, val showMoreHistory: Boolean, val showAddPlaylist: Boolean, val showSetNext: Boolean) {
    companion object {
        fun of(mode: PickerMode, notebookEnabled: Boolean): PickerChrome = when (mode) {
            PickerMode.HOME -> PickerChrome(showToc = true, showMoreHistory = true, showAddPlaylist = notebookEnabled, showSetNext = false)
            PickerMode.JUMP -> PickerChrome(showToc = false, showMoreHistory = false, showAddPlaylist = false, showSetNext = true)
        }
    }
}
