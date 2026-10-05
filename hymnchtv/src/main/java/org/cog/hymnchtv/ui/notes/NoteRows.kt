package org.cog.hymnchtv.ui.notes

import org.cog.hymnchtv.notebook.data.entity.NoteEntity

/** One note on the notes page. [edited] when its text changed after it was written. */
data class NoteRow(val id: String, val body: String, val createdAt: Long, val updatedAt: Long) {
    val edited: Boolean get() = updatedAt > createdAt
}

/** Pure list model of the notes page (no Android dependency, so it is unit tested on the JVM). */
object NoteRows {
    /** Newest first by creation time; deleted rows are dropped; equal times keep a stable order. */
    fun build(entities: List<NoteEntity>): List<NoteRow> = entities
        .filter { it.deletedAt == null }
        .sortedWith(compareByDescending<NoteEntity> { it.createdAt }.thenByDescending { it.id })
        .map { NoteRow(it.id, it.body, it.createdAt, it.updatedAt) }
}
