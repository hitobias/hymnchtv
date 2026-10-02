package org.cog.hymnchtv.ui.lyrics

/** Padding of the lyrics scroll layer so the overlays never hide a line (plan 6c). Values are pixels. */
data class LyricsPadding(val top: Int, val bottom: Int)

object LyricsInsets {
    /**
     * Top: the top bar while shown. Bottom: play card (when it is an overlay) + three-button bar while shown
     * + system bottom inset + [extra].
     */
    @JvmStatic
    fun padding(
        topBarHeight: Int, topBarShown: Boolean,
        buttonBarHeight: Int, buttonBarShown: Boolean,
        playCardHeight: Int, systemBottom: Int, extra: Int,
    ): LyricsPadding = LyricsPadding(
        top = if (topBarShown) topBarHeight.coerceAtLeast(0) else 0,
        bottom = (if (buttonBarShown) buttonBarHeight.coerceAtLeast(0) else 0) +
            playCardHeight.coerceAtLeast(0) + systemBottom.coerceAtLeast(0) + extra.coerceAtLeast(0),
    )

    /** New scrollY that keeps the line the reader is on in place when the top padding changes (0 stays 0). */
    @JvmStatic
    fun scrollAfterTopPaddingChange(scrollY: Int, oldTop: Int, newTop: Int): Int =
        if (scrollY <= 0) 0 else (scrollY + newTop - oldTop).coerceAtLeast(0)
}
