package org.cog.hymnchtv.notebook.repo.room

import androidx.room.withTransaction
import org.cog.hymnchtv.persistance.room.HymnchtvDatabase
import org.cog.hymnchtv.notebook.data.SingStats
import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
import org.cog.hymnchtv.notebook.model.Clock
import org.cog.hymnchtv.notebook.model.DedupeWindow
import org.cog.hymnchtv.notebook.model.DeviceIdProvider
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.IdGenerator
import org.cog.hymnchtv.notebook.model.NotebookValidation
import org.cog.hymnchtv.notebook.model.Occasion
import org.cog.hymnchtv.notebook.model.SingSource
import org.cog.hymnchtv.notebook.model.isActive
import org.cog.hymnchtv.notebook.repo.SingLogRepository

class RoomSingLogRepository(
    private val db: HymnchtvDatabase,
    private val clock: Clock,
    private val ids: IdGenerator,
    private val device: DeviceIdProvider,
) : SingLogRepository {
    private val dao get() = db.singLogDao()

    override suspend fun findAll(): List<SingLogEntity> = dao.findActive()

    override suspend fun findById(id: String): SingLogEntity? = dao.findById(id)?.takeIf { it.isActive }

    override suspend fun create(item: SingLogEntity): SingLogEntity {
        val now = clock.nowMillis()
        val row = item.copy(
            id = resolveId(item.id, ids),
            sungAt = NotebookValidation.sungAt(item.sungAt, now),
            playlistId = optionalUuid(item.playlistId),
            createdAt = now, updatedAt = now, deletedAt = null, updatedBy = device.deviceId(),
        )
        dao.upsert(row)
        return row
    }

    override suspend fun update(item: SingLogEntity): SingLogEntity? = db.withTransaction {
        val existing = findById(item.id) ?: return@withTransaction null
        val now = clock.nowMillis()
        val row = item.copy(
            sungAt = NotebookValidation.sungAt(item.sungAt, now),
            playlistId = optionalUuid(item.playlistId),
            createdAt = existing.createdAt, updatedAt = now, deletedAt = null, updatedBy = device.deviceId(),
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

    override suspend fun record(
        key: HymnKey,
        sungAt: Long,
        occasion: Occasion,
        source: SingSource,
        playlistId: String?,
    ): SingLogEntity = create(newLog(key, sungAt, occasion, source, playlistId))

    /** Check and insert run in one write transaction; SQLite allows a single writer, so this is atomic. */
    override suspend fun recordUnlessDuplicate(
        key: HymnKey,
        sungAt: Long,
        occasion: Occasion,
        source: SingSource,
        windowMillis: Long,
    ): SingLogEntity? {
        require(windowMillis >= 0) { "windowMillis must be >= 0" }
        NotebookValidation.sungAt(sungAt, clock.nowMillis())
        return db.withTransaction {
            val duplicate = dao.existsBetween(
                key.hymnType, key.hymnNo, key.isFu,
                DedupeWindow.lowerExclusive(sungAt, windowMillis), DedupeWindow.upperExclusive(sungAt, windowMillis),
            )
            if (duplicate) null else create(newLog(key, sungAt, occasion, source, null))
        }
    }

    override suspend fun statsFor(key: HymnKey): SingStats = dao.statsFor(key.hymnType, key.hymnNo, key.isFu)

    override suspend fun latestFor(key: HymnKey): SingLogEntity? =
        dao.latestForHymn(key.hymnType, key.hymnNo, key.isFu)

    override suspend fun findByHymn(key: HymnKey): List<SingLogEntity> =
        dao.findByHymn(key.hymnType, key.hymnNo, key.isFu)

    override suspend fun findBetween(fromInclusive: Long, toExclusive: Long): List<SingLogEntity> {
        NotebookValidation.timeRange(fromInclusive, toExclusive)
        return dao.findBetween(fromInclusive, toExclusive)
    }

    private fun newLog(key: HymnKey, sungAt: Long, occasion: Occasion, source: SingSource, playlistId: String?) =
        SingLogEntity(
            id = "", hymn = key, sungAt = sungAt, occasion = occasion, source = source,
            playlistId = playlistId, createdAt = 0, updatedAt = 0,
        )
}
