package org.cog.hymnchtv.ui.lyrics

import org.cog.hymnchtv.nav.ViewingCause

/** Which reports of ContentHandler.onViewingHymn start a reading for the automatic singing log (D-1 F4). */
object ReadingPolicy {
    /** A new page or another hymn is a reading; a recreation (theme, language, text size) shows the same reading again. */
    @JvmStatic
    fun startsReading(cause: ViewingCause): Boolean = when (cause) {
        ViewingCause.OPENED, ViewingCause.CHANGED -> true
        ViewingCause.RESTORED -> false
    }
}
