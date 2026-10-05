package org.cog.hymnchtv.ui.notes

import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.notebook.data.entity.NoteEntity
import org.cog.hymnchtv.notebook.fakes.testUuid
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.junit.Test

class NoteRowsTest {
    private val key = HymnKey.of(HymnTypes.DB, 5)

    private fun note(n: Int, created: Long, updated: Long = created, deleted: Long? = null) =
        NoteEntity(testUuid(n), key, "body $n", null, created, updated, deleted)

    @Test fun newestFirstAndDeletedRowsAreDropped() {
        val rows = NoteRows.build(listOf(note(1, 100), note(2, 300), note(3, 200, deleted = 250)))
        assertThat(rows.map { it.body }).containsExactly("body 2", "body 1").inOrder()
    }

    @Test fun aRowIsEditedOnlyWhenUpdatedAfterCreation() {
        val rows = NoteRows.build(listOf(note(1, 100), note(2, 100, updated = 900)))
        assertThat(rows.associate { it.id to it.edited }).containsExactly(testUuid(1), false, testUuid(2), true)
    }

    @Test fun equalTimesHaveAStableOrder() {
        val a = NoteRows.build(listOf(note(1, 100), note(2, 100)))
        val b = NoteRows.build(listOf(note(2, 100), note(1, 100)))
        assertThat(a).isEqualTo(b)
    }
}
