package org.cog.hymnchtv.reading.background

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/** WCAG 2.x contrast maths on 0xAARRGGBB colours; alpha is ignored (colours are treated as opaque). */
object Wcag {
    @JvmStatic
    fun luminance(color: Int): Double {
        fun channel(shift: Int): Double {
            val c = (color shr shift and 0xFF) / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }

    @JvmStatic
    fun contrast(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    /** [over] painted with [alpha] on top of opaque [base]; the result is opaque. */
    @JvmStatic
    fun blend(base: Int, over: Int, alpha: Float): Int {
        fun mix(shift: Int): Int =
            ((over shr shift and 0xFF) * alpha + (base shr shift and 0xFF) * (1 - alpha)).roundToInt()
        return (0xFF shl 24) or (mix(16) shl 16) or (mix(8) shl 8) or mix(0)
    }
}
