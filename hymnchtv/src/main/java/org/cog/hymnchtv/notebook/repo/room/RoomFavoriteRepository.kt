package org.cog.hymnchtv.notebook.repo.room

import androidx.room.withTransaction
import org.cog.hymnchtv.notebook.data.NotebookDatabase
import org.cog.hymnchtv.notebook.data.entity.FavoriteEntity
import org.cog.hymnchtv.notebook.model.Clock
import org.cog.hymnchtv.notebook.model.DeviceIdProvider
import org.cog.hymnchtv.notebook.model.FavoriteIds
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.isActive
import org.cog.hymnchtv.notebook.repo.FavoriteRepository

class RoomFavoriteRepository(
    private val db: NotebookDatabase,
    private val clock: Clock,
    private val device: DeviceIdProvider,
) : FavoriteRepository {
    private val dao get() = db.favoriteDao()

    override suspend fun findAll(): List<FavoriteEntity> = dao.findActive()

    override suspend fun findById(id: String): FavoriteEntity? = dao.findById(id)?.takeIf { it.isActive }

    /** The id is always derived from the hymn key; the incoming id is ignored. */
    override suspend fun create(item: FavoriteEntity): FavoriteEntity =
        checkNotNull(setFavorite(item.hymn, true)) { "setFavorite(true) always returns a row" }

    /** A favorite has no editable content besides its key (= its identity), so update only bumps updatedAt. */
    override suspend fun update(item: FavoriteEntity): FavoriteEntity? = db.withTransaction {
        val existing = findById(item.id) ?: return@withTransaction null
        val row = existing.copy(updatedAt = clock.nowMillis(), updatedBy = device.deviceId())
        dao.upsert(row)
        row
    }

    override suspend fun delete(id: String): Boolean = db.withTransaction {
        val existing = findById(id) ?: return@withTransaction false
        val now = clock.nowMillis()
        dao.upsert(existing.copy(updatedAt = now, deletedAt = now, updatedBy = device.deviceId()))
        true
    }

    override suspend fun isFavorite(key: HymnKey): Boolean = dao.isFavorite(key.hymnType, key.hymnNo, key.isFu)

    override suspend fun setFavorite(key: HymnKey, favorite: Boolean): FavoriteEntity? = db.withTransaction {
        val id = FavoriteIds.forKey(key)
        val existing = dao.findById(id)
        val now = clock.nowMillis()
        val by = device.deviceId()
        val row = if (favorite) {
            if (existing != null && existing.isActive) return@withTransaction existing
            existing?.copy(updatedAt = now, deletedAt = null, updatedBy = by)
                ?: FavoriteEntity(id, key, now, now, null, by)
        } else {
            if (existing == null || !existing.isActive) return@withTransaction existing
            existing.copy(updatedAt = now, deletedAt = now, updatedBy = by)
        }
        dao.upsert(row)
        row
    }

    override suspend fun toggle(key: HymnKey): Boolean = db.withTransaction {
        val next = !dao.isFavorite(key.hymnType, key.hymnNo, key.isFu)
        setFavorite(key, next)
        next
    }
}
