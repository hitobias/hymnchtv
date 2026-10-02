package org.cog.hymnchtv.reading

import kotlin.math.roundToInt

/** How the opaque white score PNGs are recoloured (plan A2). */
sealed interface ScoreTint {
    data object None : ScoreTint

    /** The pre-A2 manual inversion: each channel becomes 255 + multiplier * value. */
    data class Invert(val multiplier: Float) : ScoreTint

    /** White paper becomes [paper], black notes become [ink]; channels in between are interpolated. */
    data class Duotone(val paper: Int, val ink: Int) : ScoreTint
}

object ScoreTintPolicy {
    /** Levels cycled by the "score colour" menu; level 0 = automatic (follow the background). */
    const val LEVEL_COUNT = 4
    private val INVERT_MULTIPLIERS = floatArrayOf(0f, -0.9f, -0.8f, -0.7f)

    @JvmStatic
    fun resolve(manualLevel: Int, darkBackground: Boolean, paper: Int, ink: Int): ScoreTint = when {
        manualLevel in 1 until LEVEL_COUNT -> ScoreTint.Invert(INVERT_MULTIPLIERS[manualLevel])
        darkBackground -> ScoreTint.Duotone(paper, ink)
        else -> ScoreTint.None
    }

    /** 4x5 matrix for ColorMatrixColorFilter, or null for "no filter". */
    @JvmStatic
    fun matrix(tint: ScoreTint): FloatArray? = when (tint) {
        ScoreTint.None -> null
        is ScoreTint.Invert -> tint.multiplier.let { m ->
            floatArrayOf(
                m, 0f, 0f, 0f, 255f,
                0f, m, 0f, 0f, 255f,
                0f, 0f, m, 0f, 255f,
                0f, 0f, 0f, 1f, 0f,
            )
        }
        is ScoreTint.Duotone -> {
            fun ch(color: Int, shift: Int) = (color shr shift and 0xFF).toFloat()
            floatArrayOf(
                (ch(tint.paper, 16) - ch(tint.ink, 16)) / 255f, 0f, 0f, 0f, ch(tint.ink, 16),
                0f, (ch(tint.paper, 8) - ch(tint.ink, 8)) / 255f, 0f, 0f, ch(tint.ink, 8),
                0f, 0f, (ch(tint.paper, 0) - ch(tint.ink, 0)) / 255f, 0f, ch(tint.ink, 0),
                0f, 0f, 0f, 1f, 0f,
            )
        }
    }

    /** Applies [matrix] to one ARGB colour the way ColorMatrixColorFilter does (used by tests). */
    @JvmStatic
    fun applyTo(matrix: FloatArray, color: Int): Int {
        val src = floatArrayOf(
            (color shr 16 and 0xFF).toFloat(), (color shr 8 and 0xFF).toFloat(),
            (color and 0xFF).toFloat(), (color ushr 24).toFloat(),
        )
        fun out(row: Int): Int {
            var sum = matrix[row * 5 + 4]
            for (i in 0 until 4) sum += matrix[row * 5 + i] * src[i]
            return sum.roundToInt().coerceIn(0, 255)
        }
        return (out(3) shl 24) or (out(0) shl 16) or (out(1) shl 8) or out(2)
    }
}
