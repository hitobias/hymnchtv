package org.cog.hymnchtv.lyrics

/** Default lyrics script; persisted by [name] under LyricsLanguagePolicy.PREF_LYRICS_DEFAULT. */
enum class LyricsLang {
    FOLLOW_UI, SIMPLIFIED, TRADITIONAL;

    companion object {
        /** Never throws; unknown values mean FOLLOW_UI. */
        @JvmStatic
        fun fromPref(value: String?): LyricsLang = entries.firstOrNull { it.name == value } ?: FOLLOW_UI
    }
}
