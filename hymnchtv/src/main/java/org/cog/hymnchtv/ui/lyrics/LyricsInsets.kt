package org.cog.hymnchtv.ui.lyrics

/** Padding of the lyrics scroll layer so the overlays never hide a line (plan 6c). Values are pixels. */
data class LyricsPadding(val top: Int, val bottom: Int)

object LyricsInsets {
    /**
     * Top: the top capsule's bottom edge in the page while shown. Bottom: the higher of the player layer (reserve + system
     * bottom inset) and the bottom capsule's top edge measured from the page bottom while shown, plus [extra].
     */
    @JvmStatic
    fun padding(
        topBarBottom: Int, topBarShown: Boolean,
        pillTopFromBottom: Int, pillShown: Boolean,
        playCardHeight: Int, systemBottom: Int, extra: Int,
    ): LyricsPadding {
        val player = playCardHeight.coerceAtLeast(0) + systemBottom.coerceAtLeast(0)
        val pill = if (pillShown) pillTopFromBottom.coerceAtLeast(0) else 0
        return LyricsPadding(
            top = if (topBarShown) topBarBottom.coerceAtLeast(0) else 0,
            bottom = maxOf(player, pill) + extra.coerceAtLeast(0),
        )
    }

    /** New scrollY that keeps the line the reader is on in place when the top padding changes (0 stays 0). */
    @JvmStatic
    fun scrollAfterTopPaddingChange(scrollY: Int, oldTop: Int, newTop: Int): Int =
        if (scrollY <= 0) 0 else (scrollY + newTop - oldTop).coerceAtLeast(0)
}
