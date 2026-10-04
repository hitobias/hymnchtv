package org.cog.hymnchtv.ui.lyrics

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MediaAvailabilityTest {
    @Test
    fun noSourceMeansNoMedia() {
        assertThat(MediaAvailability.hasAny(booleanArrayOf(false, false, false, false))).isFalse()
    }

    @Test
    fun anySourceMeansMedia() {
        assertThat(MediaAvailability.hasAny(booleanArrayOf(false, false, false, true))).isTrue()
        assertThat(MediaAvailability.hasAny(booleanArrayOf(true, false, false, false))).isTrue()
    }

    @Test
    fun notEvaluatedYetIsNotLocked() {
        assertThat(MediaAvailability.hasAny(booleanArrayOf())).isTrue()
    }
}
