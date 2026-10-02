package org.cog.hymnchtv.hymn

import org.cog.hymnchtv.notebook.model.HymnTypes

/**
 * The seven sources of the home entry, in the order the buttons are laid out. [prefValue] is what the home tab
 * stores under LastHymnType (the book ids the old home screen used, plus "english").
 */
enum class HymnSource(val prefValue: String, val book: String?) {
    DB(HymnTypes.DB, HymnTypes.DB),
    BB(HymnTypes.BB, HymnTypes.BB),
    XB(HymnTypes.XB, HymnTypes.XB),
    XG(HymnTypes.XG, HymnTypes.XG),
    YB(HymnTypes.YB, HymnTypes.YB),
    ER(HymnTypes.ER, HymnTypes.ER),
    ENGLISH("english", null),
    ;

    /** Only the main book and the youth book have an appendix (附). */
    val supportsFu: Boolean get() = this == DB || this == YB

    companion object {
        /** Unknown or missing values fall back to the main book. */
        fun fromPref(value: String?): HymnSource = entries.firstOrNull { it.prefValue == value } ?: DB

        fun ofBook(book: String): HymnSource? = entries.firstOrNull { it.book == book }
    }
}
