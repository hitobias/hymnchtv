package org.cog.hymnchtv.ui.home

/** The tabs of the history page; [pref] is the value stored under [HomePrefs.HISTORY_TAB]. */
enum class HistoryTab(val pref: Int) {
    RECENT(0),
    FAVORITES(1),
    PLAYLISTS(2),
    ;

    companion object {
        /** Unknown or missing stored values open on the recent tab. */
        fun fromPref(value: Int?): HistoryTab = values().firstOrNull { it.pref == value } ?: RECENT
    }
}
