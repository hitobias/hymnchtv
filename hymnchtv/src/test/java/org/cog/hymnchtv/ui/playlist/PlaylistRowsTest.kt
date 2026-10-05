package org.cog.hymnchtv.ui.playlist

import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.notebook.data.entity.PlaylistEntity
import org.cog.hymnchtv.notebook.fakes.testUuid
import org.junit.Test

class PlaylistRowsTest {
    private fun playlist(n: Int, updated: Long, deleted: Long? = null) =
        PlaylistEntity(testUuid(n), "list $n", 1L, updated, deleted)

    @Test fun mostRecentlyChangedFirstWithItemCounts() {
        val rows = PlaylistRows.build(
            listOf(playlist(1, 100), playlist(2, 300), playlist(3, 200, deleted = 250)),
            mapOf(testUuid(1) to 4, testUuid(2) to 0),
        )
        assertThat(rows.map { it.name }).containsExactly("list 2", "list 1").inOrder()
        assertThat(rows.map { it.count }).containsExactly(0, 4).inOrder()
    }

    @Test fun aMissingCountIsZero() {
        assertThat(PlaylistRows.build(listOf(playlist(1, 1)), emptyMap()).single().count).isEqualTo(0)
    }
}
