package org.cog.hymnchtv.ui.playlist

/** What the playlist page's main button does. */
sealed interface PlayButton {
    object Start : PlayButton

    data class Next(val index: Int) : PlayButton

    object Restart : PlayButton
}

/**
 * "Open in order" without a meeting mode: the page remembers the item opened last (by id, so moving or removing items keeps it
 * right) and offers the one after it.
 */
object PlaylistOrder {
    fun button(itemIds: List<String>, lastOpenedId: String?): PlayButton? {
        if (itemIds.isEmpty()) return null
        val opened = lastOpenedId?.let(itemIds::indexOf) ?: -1
        return when {
            opened < 0 -> PlayButton.Start
            opened + 1 < itemIds.size -> PlayButton.Next(opened + 1)
            else -> PlayButton.Restart
        }
    }

    /** The index the button opens. */
    fun target(button: PlayButton): Int = when (button) {
        PlayButton.Start, PlayButton.Restart -> 0
        is PlayButton.Next -> button.index
    }
}
