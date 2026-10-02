package org.cog.hymnchtv.notebook.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DedupeWindowTest {
    private val window = 3 * 3_600_000L
    private val now = 100 * 3_600_000L

    @Test
    fun strictlyInsideTheWindowOnBothSides() {
        assertThat(DedupeWindow.contains(now - window + 1, now, window)).isTrue()
        assertThat(DedupeWindow.contains(now + window - 1, now, window)).isTrue()
        assertThat(DedupeWindow.contains(now - window, now, window)).isFalse()
        assertThat(DedupeWindow.contains(now + window, now, window)).isFalse()
    }

    @Test
    fun zeroWindowNeverMatches() {
        assertThat(DedupeWindow.contains(now, now, 0)).isFalse()
        assertThat(DedupeWindow.lowerExclusive(now, 0)).isEqualTo(now)
        assertThat(DedupeWindow.upperExclusive(now, 0)).isEqualTo(now)
    }

    @Test
    fun boundsSaturateInsteadOfOverflowing() {
        assertThat(DedupeWindow.lowerExclusive(Long.MIN_VALUE + 5, 10)).isEqualTo(Long.MIN_VALUE)
        assertThat(DedupeWindow.upperExclusive(Long.MAX_VALUE - 5, 10)).isEqualTo(Long.MAX_VALUE)
        assertThat(DedupeWindow.contains(Long.MAX_VALUE - 1, Long.MAX_VALUE - 2, window)).isTrue()
        assertThat(runCatching { DedupeWindow.upperExclusive(0, -1) }.exceptionOrNull())
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
