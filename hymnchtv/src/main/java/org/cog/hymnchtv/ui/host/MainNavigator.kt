package org.cog.hymnchtv.ui.host

/** What the screens inside the main host ask of it (implemented by MainActivity, delegating to [MainHost]). */
interface MainNavigator {
    /** Opens the table-of-contents page full-page with [book] and the index kind [page] preselected. */
    fun openToc(book: String, page: String)

    /** Opens the settings page full-page. */
    fun openSettings()

    /** Opens the full-screen "recently opened" page over the home page. */
    fun openHistory()

    /** Opens the full-screen search page; [book] is the book to search in first, null for all books. */
    fun openSearch(book: String?)
}
