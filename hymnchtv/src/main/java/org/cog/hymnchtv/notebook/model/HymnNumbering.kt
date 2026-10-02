package org.cog.hymnchtv.notebook.model

/**
 * Valid internal hymn numbers per book, copied from utils/HymnNoValidate (asserted by HymnTypesConsistencyTest
 * and the instrumented HymnNumberingConsistencyTest). Internal numbers are what MainActivity.showContent()
 * receives: 附 of hymn_db are 780 + n (n = 1..6); 青年詩歌 supplements continue after 275 (276..277).
 */
object HymnNumbering {
    const val DB_NO_MAX = 780
    const val DB_NO_TMAX = 786
    const val BB_NO_MAX = 1005
    const val BB_DUMMY = 2000
    const val ER_NO_MAX = 1232
    const val XB_NO_MAX = 171
    const val XG_NO_MAX = 206
    const val YB_NO_TMAX = 277

    /** HymnNoValidate.rangeBbLimit: number i opens the gap [limit[i], 100 * (i + 1)]; the last entry has no gap. */
    @JvmField
    val BB_LIMITS: IntArray = intArrayOf(38, 151, 259, 350, 471, 544, 630, 763, 881, 931, 1006)

    /** HymnNoValidate.rangeErLimit, same rule as BB_LIMITS. */
    @JvmField
    val ER_LIMITS: IntArray = intArrayOf(18, 125, 213, 324, 446, 525, 622, 720, 837, 921, 1040, 1119, 1233)

    @JvmField
    val XB_INVALID: Set<Int> = setOf(168, 169, 170)

    @JvmField
    val XG_INVALID: Set<Int> = setOf(34)

    @JvmStatic
    fun isValid(hymnType: String?, hymnNo: Int): Boolean = when (hymnType) {
        HymnTypes.DB -> hymnNo in 1..DB_NO_TMAX
        HymnTypes.BB -> hymnNo in 1..BB_NO_MAX && !inGap(hymnNo, BB_LIMITS)
        HymnTypes.ER -> hymnNo in 1..ER_NO_MAX && !inGap(hymnNo, ER_LIMITS)
        HymnTypes.XB -> hymnNo in 1..XB_NO_MAX && hymnNo !in XB_INVALID
        HymnTypes.XG -> hymnNo in 1..XG_NO_MAX && hymnNo !in XG_INVALID
        HymnTypes.YB -> hymnNo in 1..YB_NO_TMAX
        else -> false
    }

    /** Same as MediaRecord.isFu(): only hymn_db numbers above 780 are 附. */
    @JvmStatic
    fun isFu(hymnType: String, hymnNo: Int): Boolean = hymnType == HymnTypes.DB && hymnNo > DB_NO_MAX

    /** HymnNoValidate builds inclusive Range(limit[i], 100 * (i + 1)) for every limit except the last. */
    private fun inGap(hymnNo: Int, limits: IntArray): Boolean =
        (0 until limits.size - 1).any { i -> hymnNo in limits[i]..(100 * (i + 1)) }
}
