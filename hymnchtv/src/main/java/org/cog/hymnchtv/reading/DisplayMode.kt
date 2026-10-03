package org.cog.hymnchtv.reading

/** What a lyrics page shows (plan A2). Declaration order = the order the lyrics-page button cycles through. */
enum class DisplayMode(val showScore: Boolean, val showLyrics: Boolean) {
    SCORE_AND_LYRICS(true, true),
    SCORE_ONLY(true, false),
    LYRICS_ONLY(false, true);

    fun next(): DisplayMode = entries[(ordinal + 1) % entries.size]

    companion object {
        /** Never throws; unknown values mean SCORE_AND_LYRICS (the behaviour before A2). */
        @JvmStatic
        fun fromPref(value: String?): DisplayMode = entries.firstOrNull { it.name == value } ?: SCORE_AND_LYRICS
    }
}

object DisplayModePolicy {
    /** Fewer characters than this means the hymn has no usable lyrics text (same threshold as the JiaoChang hint). */
    const val MIN_LYRICS_CHARS = 40

    @JvmStatic
    fun hasLyricsText(text: CharSequence?): Boolean = text != null && text.length >= MIN_LYRICS_CHARS

    /** The lyrics-page button wins for this session; otherwise the stored default applies. */
    @JvmStatic
    fun resolve(sessionOverride: DisplayMode?, stored: DisplayMode): DisplayMode = sessionOverride ?: stored

    /** "Lyrics only" on a hymn without lyrics text would leave an empty page, so both are shown instead. */
    @JvmStatic
    fun effective(mode: DisplayMode, hasLyricsText: Boolean): DisplayMode =
        if (mode == DisplayMode.LYRICS_ONLY && !hasLyricsText) DisplayMode.SCORE_AND_LYRICS else mode

    /** A theme change regenerates the English HTML only when the English page is on screen, never in "score only". */
    @JvmStatic
    fun refreshEnglishOnTheme(hasEnglish: Boolean, mode: DisplayMode, hasLyricsText: Boolean): Boolean =
        hasEnglish && effective(mode, hasLyricsText).showLyrics
}
