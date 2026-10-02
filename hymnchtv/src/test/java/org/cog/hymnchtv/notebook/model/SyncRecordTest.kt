package org.cog.hymnchtv.notebook.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SyncRecordTest {
    private data class Row(
        override val id: String = "r",
        override val createdAt: Long = 0,
        override val updatedAt: Long,
        override val deletedAt: Long? = null,
        override val updatedBy: String = "d",
    ) : SyncRecord

    @Test
    fun activeMeansNotDeleted() {
        assertThat(Row(updatedAt = 1).isActive).isTrue()
        assertThat(Row(updatedAt = 1, deletedAt = 1).isActive).isFalse()
    }

    @Test
    fun versionIsLatestOfUpdatedAndDeleted() {
        assertThat(Row(updatedAt = 5).version).isEqualTo(5L)
        assertThat(Row(updatedAt = 5, deletedAt = 9).version).isEqualTo(9L)
        assertThat(Row(updatedAt = 9, deletedAt = 5).version).isEqualTo(9L)
    }
}
