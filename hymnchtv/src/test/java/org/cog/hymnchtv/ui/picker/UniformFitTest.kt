package org.cog.hymnchtv.ui.picker

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UniformFitTest {
    @Test fun keepsTheMaximumWhenItFits() {
        assertThat(UniformFit.pick(16f, 10f, 0.5f) { true }).isEqualTo(16f)
    }

    @Test fun stepsDownUntilItFits() {
        assertThat(UniformFit.pick(16f, 10f, 0.5f) { it <= 13f }).isEqualTo(13f)
    }

    @Test fun neverGoesBelowTheMinimum() {
        assertThat(UniformFit.pick(16f, 10f, 0.5f) { false }).isEqualTo(10f)
    }

    @Test fun rejectsABadRange() {
        val e = runCatching { UniformFit.pick(8f, 10f, 0.5f) { true } }.exceptionOrNull()
        assertThat(e).isInstanceOf(IllegalArgumentException::class.java)
    }
}
