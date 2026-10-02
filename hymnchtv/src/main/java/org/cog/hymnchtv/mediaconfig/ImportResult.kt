package org.cog.hymnchtv.mediaconfig

/** Outcome of one url-record import: [imported] rows written out of [total] well-formed lines. */
data class ImportResult(val imported: Int, val total: Int) {
    companion object {
        @JvmField
        val EMPTY = ImportResult(0, 0)
    }
}
