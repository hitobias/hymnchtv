package org.cog.hymnchtv.notebook.model

import androidx.room.ColumnInfo

/**
 * Canonical hymn identity: (hymnType, internal hymnNo, isFu) where isFu is always derived from the number,
 * so a 附 hymn has exactly one representation. Embedded in every notebook entity.
 */
data class HymnKey(
    @ColumnInfo(name = COL_TYPE) val hymnType: String,
    @ColumnInfo(name = COL_NO) val hymnNo: Int,
    @ColumnInfo(name = COL_FU) val isFu: Boolean,
) {
    init {
        require(isCanonical(hymnType, hymnNo, isFu)) { "Non-canonical hymn key: $hymnType/$hymnNo/fu=$isFu" }
    }

    companion object {
        const val COL_TYPE = "hymnType"
        const val COL_NO = "hymnNo"
        const val COL_FU = "isFu"

        @JvmStatic
        fun isValid(hymnType: String?, hymnNo: Int): Boolean = HymnNumbering.isValid(hymnType, hymnNo)

        @JvmStatic
        fun isCanonical(hymnType: String?, hymnNo: Int, isFu: Boolean): Boolean =
            hymnType != null && HymnNumbering.isValid(hymnType, hymnNo) && isFu == HymnNumbering.isFu(hymnType, hymnNo)

        /** Throws IllegalArgumentException when the number is not valid for the book. */
        @JvmStatic
        fun of(hymnType: String, hymnNo: Int): HymnKey = HymnKey(hymnType, hymnNo, HymnNumbering.isFu(hymnType, hymnNo))

        /** Null instead of an exception, for UI callers holding unchecked values. */
        @JvmStatic
        fun ofOrNull(hymnType: String?, hymnNo: Int): HymnKey? =
            if (hymnType != null && HymnNumbering.isValid(hymnType, hymnNo)) of(hymnType, hymnNo) else null
    }
}
