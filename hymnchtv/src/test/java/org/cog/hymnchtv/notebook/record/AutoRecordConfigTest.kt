package org.cog.hymnchtv.notebook.record

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AutoRecordConfigTest {
    @Test
    fun defaultsMatchTheSpec() {
        val config = AutoRecordConfig()
        assertThat(config.visibleThresholdMillis).isEqualTo(2 * 60 * 1000L)
        assertThat(config.dedupeWindowMillis).isEqualTo(3 * 60 * 60 * 1000L)
    }

    @Test
    fun rejectsInvalidValues() {
        assertThat(runCatching { AutoRecordConfig(visibleThresholdMillis = 0) }.exceptionOrNull())
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThat(runCatching { AutoRecordConfig(dedupeWindowMillis = -1) }.exceptionOrNull())
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
