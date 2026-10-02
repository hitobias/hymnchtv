package org.cog.hymnchtv.ui.lyrics

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Plan 6c: only a clean single tap in the middle 60 % x 60 % toggles the toolbars. */
class SingleTapDetectorTest {
    private val detector = SingleTapDetector(touchSlop = 8, longPressTimeoutMs = 400)
    private val w = 360
    private val h = 600

    private fun tap(x: Float, y: Float, durationMs: Long = 80, dx: Float = 0f, dy: Float = 0f): Boolean {
        detector.down(x, y, 0)
        if (dx != 0f || dy != 0f) detector.move(x + dx, y + dy)
        return detector.up(x + dx, y + dy, durationMs, w, h)
    }

    @Test
    fun centreTapCounts() {
        assertThat(tap(180f, 300f)).isTrue()
    }

    @Test
    fun edgesOutsideTheMiddleSixtyPercentDoNot() {
        assertThat(tap(10f, 300f)).isFalse()       // left strip (x < 20%)
        assertThat(tap(350f, 300f)).isFalse()      // right strip
        assertThat(tap(180f, 20f)).isFalse()       // top strip (y < 20%)
        assertThat(tap(180f, 590f)).isFalse()      // bottom strip
    }

    @Test
    fun regionBordersAreInclusiveOfTheMiddleSixtyPercent() {
        assertThat(tap(72f, 120f)).isTrue()        // 20 % of 360 / 600
        assertThat(tap(288f, 480f)).isTrue()       // 80 %
        assertThat(tap(71f, 300f)).isFalse()
        assertThat(tap(289f, 300f)).isFalse()
    }

    @Test
    fun movementPastTheSlopIsAScrollNotATap() {
        assertThat(tap(180f, 300f, dy = 9f)).isFalse()
        assertThat(tap(180f, 300f, dx = 9f)).isFalse()
        assertThat(tap(180f, 300f, dx = 5f, dy = 5f)).isTrue()   // drift inside the slop
    }

    @Test
    fun aMoveThatReturnsToTheStartStillCountsAsMoved() {
        detector.down(180f, 300f, 0)
        detector.move(180f, 330f)
        detector.move(180f, 300f)
        assertThat(detector.up(180f, 300f, 100, w, h)).isFalse()
    }

    @Test
    fun longPressIsNotATap() {
        assertThat(tap(180f, 300f, durationMs = 400)).isFalse()
        assertThat(tap(180f, 300f, durationMs = 399)).isTrue()
    }

    @Test
    fun aSecondFingerCancelsTheGesture() {
        detector.down(180f, 300f, 0)
        detector.pointerDown()
        assertThat(detector.up(180f, 300f, 50, w, h)).isFalse()
    }

    @Test
    fun cancelAndUpWithoutDownDoNothing() {
        assertThat(detector.up(180f, 300f, 50, w, h)).isFalse()
        detector.down(180f, 300f, 0)
        detector.cancel()
        assertThat(detector.up(180f, 300f, 50, w, h)).isFalse()
    }

    @Test
    fun theNextGestureStartsClean() {
        detector.down(180f, 300f, 0)
        detector.pointerDown()
        detector.up(180f, 300f, 50, w, h)
        assertThat(tap(180f, 300f)).isTrue()
    }

    @Test
    fun emptyViewNeverTaps() {
        detector.down(0f, 0f, 0)
        assertThat(detector.up(0f, 0f, 10, 0, 0)).isFalse()
    }
}
