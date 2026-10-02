package org.cog.hymnchtv.ui.titles

/**
 * Looks up a hymn's title by book and number (contract C-8). Synchronous on purpose: it reads one small asset.
 * Sub-project D-1 has its own, unrelated suspend-based title lookup; the two do not share types.
 */
fun interface HymnTitleSource {
    /** @return the title (with any extra "（…）" note), or null when [hymnType]/[hymnNo] does not exist. */
    fun lookup(hymnType: String, hymnNo: Int): String?
}
