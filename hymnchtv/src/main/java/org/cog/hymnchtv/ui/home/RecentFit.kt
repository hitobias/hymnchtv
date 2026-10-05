package org.cog.hymnchtv.ui.home

/** What [KeypadSizer] needs from the list under the picker: how many rows to reserve room for, and a way to show fewer. */
interface RecentFit {
    /** How many rows the page should reserve room for (the home history; the jump panel reserves none, its list scrolls). */
    val total: Int

    /** Called when [total] became known or changed, so the page can be fitted again. */
    var onChanged: (() -> Unit)?

    /** Shows the first [count] rows, or the empty hint when there are none and [empty] says it fits. */
    fun fit(count: Int, empty: Boolean)
}
