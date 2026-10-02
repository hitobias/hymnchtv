package org.cog.hymnchtv.hymn

import org.cog.hymnchtv.notebook.model.HymnNumbering

/**
 * A hymn as the media layer numbers it: [book] plus [storedNo] (appendix hymns continue after the plain range:
 * main book 781..786, youth book 276..277). This is the single place where what the user types, what is opened,
 * what the history shows and where it is reopened from are converted into each other.
 */
data class HymnRef(val book: String, val storedNo: Int) {
    val isFu: Boolean get() = HymnNumberRules.fuOffset(book)?.let { storedNo > it } ?: false

    /** The number as printed: the appendix number for 附 hymns, the stored number otherwise. */
    val displayNo: Int get() = if (isFu) storedNo - HymnNumberRules.fuOffset(book)!! else storedNo

    val isValid: Boolean get() = HymnNumbering.isValid(book, storedNo)

    companion object {
        fun fromEntry(book: String, number: Int, isFu: Boolean): HymnRef? =
            HymnNumberRules.storedOf(book, number, isFu)?.let { HymnRef(book, it) }
    }
}
