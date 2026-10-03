package org.cog.hymnchtv.ui.picker

/** Keypad key height: the keys share the screen height left over, never below the 48dp touch target nor above 72dp. */
object KeypadMetrics {
    const val MIN_KEY_DP = 48f
    const val MAX_KEY_DP = 72f
    const val GAP_DP = 8f
    const val ROWS = 4

    /** Height of one key row when [availableDp] is what the whole keypad (4 rows and 3 gaps) may take. */
    @JvmStatic
    fun keyHeightDp(availableDp: Float): Float = ((availableDp - (ROWS - 1) * GAP_DP) / ROWS).coerceIn(MIN_KEY_DP, MAX_KEY_DP)
}
