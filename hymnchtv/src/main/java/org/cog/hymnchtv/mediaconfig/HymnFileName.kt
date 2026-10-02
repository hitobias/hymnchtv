package org.cog.hymnchtv.mediaconfig

import java.util.regex.Pattern

/** File-name rule for locating a hymn's local media file by its number. */
object HymnFileName {
    @JvmStatic
    fun matcher(hymnNo: Int): (String) -> Boolean {
        // Exact word boundary number match e.g. ChHymns-0009.mp3
        val wordBounded = Pattern.compile("\\b0*$hymnNo\\b")
        // Optional prefix character/zero's, with exact number matching e.g. D609建造.mp3
        val nonDigitBounded = Pattern.compile("\\D0*$hymnNo\\D")
        return { name -> wordBounded.matcher(name).find() || nonDigitBounded.matcher(name).find() }
    }
}
