package org.cog.hymnchtv.ui.home

import org.cog.hymnchtv.MainActivity.HYMN_DB
import org.cog.hymnchtv.MainActivity.HYMN_YB
import org.cog.hymnchtv.utils.HymnNoValidate.HYMN_DB_NO_MAX
import org.cog.hymnchtv.utils.HymnNoValidate.HYMN_YB_NO_MAX

/** Reads what the user typed on the home keypad ("12", or "附3" for a Fu hymn) without any side effect (no toasts). */
object HomeEntry {
    const val FU = "附"

    /** @return the hymn number to look up in [hymnType], or null when the entry is empty, malformed, or has no such Fu. */
    fun hymnNo(entry: String, hymnType: String): Int? {
        val isFu = entry.startsWith(FU)
        val no = entry.removePrefix(FU).toIntOrNull()?.takeIf { it >= 1 } ?: return null
        if (!isFu) return no
        return when (hymnType) {
            HYMN_DB -> no + HYMN_DB_NO_MAX
            HYMN_YB -> no + HYMN_YB_NO_MAX
            else -> null
        }
    }
}
