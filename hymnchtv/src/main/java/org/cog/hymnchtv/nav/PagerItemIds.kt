package org.cog.hymnchtv.nav

import org.cog.hymnchtv.notebook.model.HymnTypes

/**
 * Stable, book-qualified item ids for the lyrics pager (plan H5 path A): a jump to another book installs a new adapter,
 * and FragmentStateAdapter keys fragments and their saved state by item id, so two books must never share an id.
 * Fixed codes for the six books (never hashCode); unknown books get 0.
 */
object PagerItemIds {
    private const val STRIDE = 10_000L
    private val CODES = mapOf(
        HymnTypes.DB to 1L, HymnTypes.BB to 2L, HymnTypes.ER to 3L, HymnTypes.XB to 4L, HymnTypes.XG to 5L, HymnTypes.YB to 6L,
    )

    private fun code(book: String?): Long = CODES[book] ?: 0L

    @JvmStatic
    fun itemId(book: String?, position: Int): Long {
        require(position in 0 until STRIDE) { "page $position out of range" }
        return code(book) * STRIDE + position
    }

    @JvmStatic
    fun contains(book: String?, itemId: Long, itemCount: Int): Boolean =
        itemId >= 0 && itemId / STRIDE == code(book) && itemId % STRIDE < itemCount
}
