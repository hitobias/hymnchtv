package org.cog.hymnchtv.ui.picker

/** Largest size, stepping down from [maxSize], at which [fits] holds; never below [minSize]. */
object UniformFit {
    @JvmStatic
    fun pick(maxSize: Float, minSize: Float, step: Float, fits: (Float) -> Boolean): Float {
        require(step > 0f && maxSize >= minSize) { "bad range $minSize..$maxSize step $step" }
        var size = maxSize
        while (size > minSize && !fits(size)) size -= step
        return size.coerceAtLeast(minSize)
    }
}
