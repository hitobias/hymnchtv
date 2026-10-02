package org.cog.hymnchtv.ui.lyrics

import kotlin.math.abs

/**
 * Decides whether a touch gesture was a single tap in the middle of the page (plan 6c).
 * Pure: fed from the lyrics scroll host, which gives it the same touch slop it uses for scroll/page decisions,
 * so a scroll or a page swipe can never count as a tap. Times are milliseconds relative to the first DOWN.
 */
class SingleTapDetector(
    private val touchSlop: Int,
    private val longPressTimeoutMs: Long,
    private val regionFraction: Float = CENTRE_FRACTION,
) {
    private var active = false
    private var cancelled = false
    private var downX = 0f
    private var downY = 0f

    fun down(x: Float, y: Float, @Suppress("UNUSED_PARAMETER") timeMs: Long) {
        active = true
        cancelled = false
        downX = x
        downY = y
    }

    fun move(x: Float, y: Float) {
        if (active && (abs(x - downX) > touchSlop || abs(y - downY) > touchSlop)) cancelled = true
    }

    /** A second finger: pinch or two-finger gesture. */
    fun pointerDown() {
        cancelled = true
    }

    fun cancel() {
        active = false
        cancelled = false
    }

    /** @return true when this gesture was a single tap inside the middle region of a [width] x [height] view. */
    fun up(x: Float, y: Float, durationMs: Long, width: Int, height: Int): Boolean {
        val wasTap = active && !cancelled && durationMs < longPressTimeoutMs &&
            abs(x - downX) <= touchSlop && abs(y - downY) <= touchSlop && inCentre(downX, downY, width, height)
        cancel()
        return wasTap
    }

    private fun inCentre(x: Float, y: Float, width: Int, height: Int): Boolean {
        if (width <= 0 || height <= 0) return false
        val marginX = width * (1 - regionFraction) / 2
        val marginY = height * (1 - regionFraction) / 2
        return x >= marginX && x <= width - marginX && y >= marginY && y <= height - marginY
    }

    companion object {
        const val CENTRE_FRACTION = 0.6f
    }
}
