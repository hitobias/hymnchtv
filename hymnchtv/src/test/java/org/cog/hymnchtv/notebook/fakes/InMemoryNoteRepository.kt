package org.cog.hymnchtv.notebook.fakes

import org.cog.hymnchtv.notebook.data.entity.NoteEntity
import org.cog.hymnchtv.notebook.model.Clock
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.IdGenerator
import org.cog.hymnchtv.notebook.model.NotebookValidation
import org.cog.hymnchtv.notebook.model.isActive
import org.cog.hymnchtv.notebook.repo.NoteRepository

/** NoteRepository in memory; NoteRepositoryContract keeps it in line with RoomNoteRepository. Rows are replaced, never mutated. */
class InMemoryNoteRepository(
    private val clock: Clock,
    private val ids: IdGenerator = SequentialIds(),
    private val device: String = TEST_DEVICE,
) : NoteRepository {
    @Volatile
    var rows: Map<String, NoteEntity> = emptyMap()
        private set

    /** The next call throws once (to test error paths). */
    @Volatile
    var failNext: Boolean = false

    private fun enter() {
        if (failNext) {
            failNext = false
            throw IllegalStateException("simulated failure")
        }
    }

    private fun put(row: NoteEntity): NoteEntity {
        rows = rows + (row.id to row)
        return row
    }

    private fun newestFirst(list: Collection<NoteEntity>): List<NoteEntity> =
        list.filter { it.isActive }.sortedWith(compareByDescending<NoteEntity> { it.createdAt }.thenByDescending { it.id })

    override suspend fun findAll(): List<NoteEntity> = newestFirst(rows.values)

    override suspend fun findById(id: String): NoteEntity? = rows[id]?.takeIf { it.isActive }

    override suspend fun create(item: NoteEntity): NoteEntity {
        enter()
        val now = clock.nowMillis()
        val id = NotebookValidation.uuid(item.id.ifBlank { ids.newId() })
        return put(
            item.copy(
                id = id, body = NotebookValidation.noteBody(item.body), singLogId = item.singLogId?.let(NotebookValidation::uuid),
                createdAt = now, updatedAt = now, deletedAt = null, updatedBy = device,
            ),
        )
    }

    override suspend fun update(item: NoteEntity): NoteEntity? {
        enter()
        val existing = findById(item.id) ?: return null
        return put(
            item.copy(
                body = NotebookValidation.noteBody(item.body), createdAt = existing.createdAt,
                updatedAt = clock.nowMillis(), deletedAt = null, updatedBy = device,
            ),
        )
    }

    override suspend fun delete(id: String): Boolean {
        enter()
        val existing = findById(id) ?: return false
        val now = clock.nowMillis()
        put(existing.copy(updatedAt = now, deletedAt = now, updatedBy = device))
        return true
    }

    override suspend fun add(key: HymnKey, body: String, singLogId: String?): NoteEntity =
        create(NoteEntity(id = "", hymn = key, body = body, singLogId = singLogId, createdAt = 0, updatedAt = 0))

    override suspend fun findByHymn(key: HymnKey): List<NoteEntity> {
        enter()
        return newestFirst(rows.values.filter { it.hymn == key })
    }
}
