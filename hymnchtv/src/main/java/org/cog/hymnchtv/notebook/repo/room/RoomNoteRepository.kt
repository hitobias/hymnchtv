package org.cog.hymnchtv.notebook.repo.room

import androidx.room.withTransaction
import org.cog.hymnchtv.persistance.room.HymnchtvDatabase
import org.cog.hymnchtv.notebook.data.entity.NoteEntity
import org.cog.hymnchtv.notebook.model.Clock
import org.cog.hymnchtv.notebook.model.DeviceIdProvider
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.IdGenerator
import org.cog.hymnchtv.notebook.model.NotebookValidation
import org.cog.hymnchtv.notebook.model.isActive
import org.cog.hymnchtv.notebook.repo.NoteRepository

class RoomNoteRepository(
    private val db: HymnchtvDatabase,
    private val clock: Clock,
    private val ids: IdGenerator,
    private val device: DeviceIdProvider,
) : NoteRepository {
    private val dao get() = db.noteDao()

    override suspend fun findAll(): List<NoteEntity> = dao.findActive()

    override suspend fun findById(id: String): NoteEntity? = dao.findById(id)?.takeIf { it.isActive }

    override suspend fun create(item: NoteEntity): NoteEntity {
        val now = clock.nowMillis()
        val row = item.copy(
            id = resolveId(item.id, ids),
            body = NotebookValidation.noteBody(item.body),
            singLogId = optionalUuid(item.singLogId),
            createdAt = now, updatedAt = now, deletedAt = null, updatedBy = device.deviceId(),
        )
        dao.upsert(row)
        return row
    }

    override suspend fun update(item: NoteEntity): NoteEntity? = db.withTransaction {
        val existing = findById(item.id) ?: return@withTransaction null
        val row = item.copy(
            body = NotebookValidation.noteBody(item.body),
            singLogId = optionalUuid(item.singLogId),
            createdAt = existing.createdAt, updatedAt = clock.nowMillis(), deletedAt = null, updatedBy = device.deviceId(),
        )
        dao.upsert(row)
        row
    }

    override suspend fun delete(id: String): Boolean = db.withTransaction {
        val existing = findById(id) ?: return@withTransaction false
        val now = clock.nowMillis()
        dao.upsert(existing.copy(updatedAt = now, deletedAt = now, updatedBy = device.deviceId()))
        true
    }

    override suspend fun add(key: HymnKey, body: String, singLogId: String?): NoteEntity =
        create(NoteEntity(id = "", hymn = key, body = body, singLogId = singLogId, createdAt = 0, updatedAt = 0))

    override suspend fun findByHymn(key: HymnKey): List<NoteEntity> =
        dao.findByHymn(key.hymnType, key.hymnNo, key.isFu)
}
