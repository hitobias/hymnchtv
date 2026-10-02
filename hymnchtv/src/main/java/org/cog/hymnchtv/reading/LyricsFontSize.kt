package org.cog.hymnchtv.reading

/** Default lyrics size (plan A2); [scale] multiplies LyricsScale's base size. MEDIUM equals the pre-A2 default. */
enum class LyricsFontSize(val scale: Float) {
    SMALL(1.0f),
    MEDIUM(1.25f),
    LARGE(1.5f),
    XLARGE(2.0f);

    companion object {
        /** Never throws; unknown values mean MEDIUM. */
        @JvmStatic
        fun fromPref(value: String?): LyricsFontSize = entries.firstOrNull { it.name == value } ?: MEDIUM
    }
}

/** Lyrics typeface: the device font, or the bundled HymnalKai (LXGW WenKai subset). */
enum class LyricsFont {
    SYSTEM,
    KAI;

    companion object {
        /** Never throws; unknown values mean KAI. */
        @JvmStatic
        fun fromPref(value: String?): LyricsFont = entries.firstOrNull { it.name == value } ?: KAI
    }
}

object LyricsScale {
    /** Text size in sp at scale 1.0 (= SMALL). */
    const val BASE_SP_PORTRAIT = 16
    const val BASE_SP_LANDSCAPE = 28

    /** Same limits as ZoomTextView (MIN_SCALE_FACTOR / MAX_SCALE_FACTOR). */
    const val MIN = 1.0f
    const val MAX = 5.0f

    /** A stored pinch scale wins (clamped); a missing or broken value starts from the chosen preset. */
    @JvmStatic
    fun resolve(stored: Float?, preset: LyricsFontSize): Float =
        if (stored == null || stored.isNaN() || stored <= 0f) preset.scale else stored.coerceIn(MIN, MAX)
}
