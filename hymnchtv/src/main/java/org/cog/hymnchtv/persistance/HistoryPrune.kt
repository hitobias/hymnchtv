package org.cog.hymnchtv.persistance

/**
 * Pure arithmetic of the history purge (spec = the former DatabaseBackend.storeHymnHistory).
 * With N rows and a limit L, excess = N - L. When excess > 0 the rows older than the (excess + MARGIN)-th
 * oldest row (1-based, ordered by timeStamp ASC) are deleted, so a purge frees MARGIN extra slots and the
 * list is not purged again on every insert.
 */
object HistoryPrune {
    /** Extra rows removed beyond the excess, so the next purge is not needed right away. */
    const val MARGIN = 10

    /**
     * The 1-based position (timeStamp ASC) of the pivot row, or null when nothing has to be purged.
     * Rows with a timeStamp strictly smaller than the pivot's are deleted; rows tied with it are kept.
     */
    @JvmStatic
    fun pivotIndex(count: Int, limit: Int): Int? {
        val excess = count - limit
        return if (excess > 0) excess + MARGIN else null
    }
}
