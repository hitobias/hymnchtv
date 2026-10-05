package org.cog.hymnchtv.nav

/**
 * Where the reader was on a lyrics page (H5 return stack): the scroll offset of `lyrics_scroll` and the height of its
 * content when it was measured, so the same place can be found again after the text size, script or mode changed.
 */
data class ReadingPosition(val scrollY: Int, val contentHeight: Int) {

    /** The offset of the same place once the page measures [newContentHeight]; the ScrollView clamps it itself. */
    fun restoredY(newContentHeight: Int): Int =
        if (contentHeight <= 0 || newContentHeight <= 0 || newContentHeight == contentHeight) scrollY
        else (scrollY.toLong() * newContentHeight / contentHeight).toInt()

    /** "scrollY,contentHeight"; see [decode]. */
    fun encode(): String = "$scrollY,$contentHeight"

    companion object {
        @JvmField
        val TOP = ReadingPosition(0, 0)

        /** From live view values; negative ones (not measured yet) count as 0. */
        @JvmStatic
        fun of(scrollY: Int, contentHeight: Int): ReadingPosition = ReadingPosition(maxOf(0, scrollY), maxOf(0, contentHeight))

        /** Never throws: null for missing, damaged or negative values. */
        @JvmStatic
        fun decode(text: String?): ReadingPosition? {
            val fields = text?.split(',') ?: return null
            if (fields.size != 2) return null
            val y = fields[0].toIntOrNull() ?: return null
            val h = fields[1].toIntOrNull() ?: return null
            return if (y < 0 || h < 0) null else ReadingPosition(y, h)
        }
    }
}
