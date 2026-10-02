package org.cog.hymnchtv.reading.background

/** Text colours for the main screen (hymn-number entry, search box, keypad) on top of the chosen background. */
object MainScreenColors {
    const val MIN_TEXT_CONTRAST = 4.5
    private const val HINT_ALPHA = 0x99   // 60 %

    /** The user's font colour when it is readable on [palette]'s background, else the palette's own text colour. */
    @JvmStatic
    fun textColor(preferred: Int, palette: ReadingPalette): Int =
        if (Wcag.contrast(preferred, palette.paperColor) >= MIN_TEXT_CONTRAST) preferred else palette.textColor

    @JvmStatic
    fun hintColor(text: Int): Int = (HINT_ALPHA shl 24) or (text and 0x00FFFFFF)
}
