package org.cog.hymnchtv.ui.search

import org.cog.hymnchtv.hymn.HymnRef

/**
 * Receives the search result the reader tapped. [SearchFragment] asks its parent fragment, then its activity; with
 * neither (the home tab) it opens the lyrics page itself, as before. The lyrics jump panel is one (H5).
 */
fun interface SearchHost {
    fun onSearchResult(ref: HymnRef)
}
