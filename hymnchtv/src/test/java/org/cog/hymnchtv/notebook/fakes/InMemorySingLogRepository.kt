package org.cog.hymnchtv.notebook.fakes

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.cog.hymnchtv.notebook.data.SingStats
import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
import org.cog.hymnchtv.notebook.model.Clock
import org.cog.hymnchtv.notebook.model.DedupeWindow
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.IdGenerator
import org.cog.hymnchtv.notebook.model.NotebookValidation
import org.cog.hymnchtv.notebook.model.Occasion
import org.cog.hymnchtv.notebook.model.SingSource
import org.cog.hymnchtv.notebook.model.isActive
import org.cog.hymnchtv.notebook.repo.SingLogRepository

/** Same contract as RoomSingLogRepository; the map is replaced, never mutated; writes are serialized by a Mutex. */
class InMemorySingLogRepository(
    private val clock: Clock,
    private val ids: IdGenerator = SequentialIds(),
    private val device: String = TEST_DEVICE,
) : SingLogRepository {
    private val writeLock = Mutex()

    @Volatile
    var rows: Map<String, SingLogEntity> = emptyMap()
        private set

    /** When true, the next latestFor/create/recordUnlessDuplicate throws IllegalStateException once. */
    @Volatile
    var failNext: Boolean = false

    private fun maybeFail() {
        if (failNext) {
            failNext = false
            throw IllegalStateException("simulated failure")
        }
    }

    private fun active(key: HymnKey) = rows.values.filter { it.isActive && it.hymn == key }

    private fun insertUnlocked(item: SingLogEntity): SingLogEntity {
        val now = clock.nowMillis()
        val row = item.copy(
            id = item.id.ifBlank { ids.newId() }, sungAt = NotebookValidation.sungAt(item.sungAt, now),
            createdAt = now, updatedAt = now, deletedAt = null, updatedBy = device,
        )
        rows = rows + (row.id to row)
        return row
    }

    override suspend fun findAll() = rows.values.filter { it.isActive }.sortedByDescending { it.sungAt }

    override suspend fun findById(id: String) = rows[id]?.takeIf { it.isActive }

    override suspend fun create(item: SingLogEntity): SingLogEntity = writeLock.withLock {
        maybeFail()
        insertUnlocked(item)
    }

    override suspend fun update(item: SingLogEntity): SingLogEntity? = writeLock.withLock {
        val existing = rows[item.id]?.takeIf { it.isActive } ?: return@withLock null
        val now = clock.nowMillis()
        val row = item.copy(
            sungAt = NotebookValidation.sungAt(item.sungAt, now),
            createdAt = existing.createdAt, updatedAt = now, deletedAt = null, updatedBy = device,
        )
        rows = rows + (row.id to row)
        row
    }

    override suspend fun delete(id: String): Boolean = writeLock.withLock {
        val existing = rows[id]?.takeIf { it.isActive } ?: return@withLock false
        val now = clock.nowMillis()
        rows = rows + (id to existing.copy(updatedAt = now, deletedAt = now, updatedBy = device))
        true
    }

    override suspend fun record(
        key: HymnKey,
        sungAt: Long,
        occasion: Occasion,
        source: SingSource,
        playlistId: String?,
    ): SingLogEntity = create(newLog(key, sungAt, occasion, source, playlistId))

    override suspend fun recordUnlessDuplicate(
        key: HymnKey,
        sungAt: Long,
        occasion: Occasion,
        source: SingSource,
        windowMillis: Long,
    ): SingLogEntity? {
        // Same order as RoomSingLogRepository: validate first, so an illegal time never comes back as "duplicate".
        require(windowMillis >= 0) { "windowMillis must be >= 0" }
        NotebookValidation.sungAt(sungAt, clock.nowMillis())
        return writeLock.withLock {
            maybeFail()
            if (active(key).any { DedupeWindow.contains(it.sungAt, sungAt, windowMillis) }) {
                null
            } else {
                insertUnlocked(newLog(key, sungAt, occasion, source, null))
            }
        }
    }

    override suspend fun statsFor(key: HymnKey): SingStats {
        val logs = active(key)
        return SingStats(logs.size, logs.maxOfOrNull { it.sungAt })
    }

    override suspend fun latestFor(key: HymnKey): SingLogEntity? {
        maybeFail()
        return active(key).maxByOrNull { it.sungAt }
    }

    override suspend fun findByHymn(key: HymnKey) = active(key).sortedByDescending { it.sungAt }

    override suspend fun findBetween(fromInclusive: Long, toExclusive: Long): List<SingLogEntity> {
        NotebookValidation.timeRange(fromInclusive, toExclusive)
        return findAll().filter { it.sungAt in fromInclusive until toExclusive }
    }

    private fun newLog(key: HymnKey, sungAt: Long, occasion: Occasion, source: SingSource, playlistId: String?) =
        SingLogEntity(
            id = "", hymn = key, sungAt = sungAt, occasion = occasion, source = source,
            playlistId = playlistId, createdAt = 0, updatedAt = 0,
        )
}
