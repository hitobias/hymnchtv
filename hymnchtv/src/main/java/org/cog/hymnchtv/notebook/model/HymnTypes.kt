package org.cog.hymnchtv.notebook.model

/** Hymn book identifiers; values mirror MainActivity.HYMN_* (asserted by HymnTypesConsistencyTest). */
object HymnTypes {
    const val DB = "hymn_db"
    const val BB = "hymn_bb"
    const val ER = "hymn_er"
    const val XB = "hymn_xb"
    const val XG = "hymn_xg"
    const val YB = "hymn_yb"

    @JvmField
    val ALL: Set<String> = setOf(DB, BB, ER, XB, XG, YB)
}
