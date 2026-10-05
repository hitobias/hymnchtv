package org.cog.hymnchtv.nav

import org.cog.hymnchtv.hymn.HymnRef

/** Why the lyrics page reports the hymn on screen through its single entry point (ContentHandler.onViewingHymn). */
enum class ViewingCause {
    /** A new lyrics page shows its first hymn (also the page a path-B cross-book jump opens). */
    OPENED,

    /** The page was recreated (theme, language, text size): the same hymn again, not a new reading. */
    RESTORED,

    /** Another hymn in the same page: page turn, next button, auto-next, jump, return. */
    CHANGED,
}

/** Test observer of the viewing entry point (ContentHandler.sViewingObserverForTest); null in production. */
fun interface ViewingObserver {
    fun onViewing(ref: HymnRef, cause: ViewingCause)
}
