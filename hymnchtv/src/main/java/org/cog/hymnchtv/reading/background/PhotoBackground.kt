package org.cog.hymnchtv.reading.background

/** Dim and blur settings for the user's photo background (plan A2). */
object PhotoBackground {
    const val PREF_DIM = "PhotoDim"
    const val PREF_BLUR = "PhotoBlur"

    /** Percent of black laid over the photo; never 0 so the light text stays readable. */
    const val DIM_MIN = 20
    const val DIM_MAX = 80
    const val DIM_DEFAULT = 40

    /** Blur radius in dp (RenderEffect, API 31+). */
    const val BLUR_MAX = 25
    const val BLUR_DEFAULT = 0

    @JvmStatic
    fun dimAlpha(percent: Int): Int = (percent.coerceIn(DIM_MIN, DIM_MAX) * 255 + 50) / 100

    @JvmStatic
    fun blurRadiusPx(dp: Int, density: Float): Float = dp.coerceIn(0, BLUR_MAX) * density

    /** Largest power-of-two inSampleSize that keeps both sides at least the requested size. */
    @JvmStatic
    fun sampleSize(srcWidth: Int, srcHeight: Int, reqWidth: Int, reqHeight: Int): Int {
        if (srcWidth <= 0 || srcHeight <= 0 || reqWidth <= 0 || reqHeight <= 0) return 1
        var size = 1
        while (srcWidth / (size * 2) >= reqWidth && srcHeight / (size * 2) >= reqHeight) size *= 2
        return size
    }
}
