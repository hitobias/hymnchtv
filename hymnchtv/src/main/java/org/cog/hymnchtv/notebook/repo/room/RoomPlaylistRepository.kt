package org.cog.hymnchtv.notebook.repo.room

import androidx.room.withTransaction
import org.cog.hymnchtv.persistance.room.HymnchtvDatabase
import org.cog.hymnchtv.notebook.data.entity.PlaylistEntity
import org.cog.hymnchtv.notebook.data.entity.PlaylistItemEntity
import org.cog.hymnchtv.notebook.model.Clock
import org.cog.hymnchtv.notebook.model.DeviceIdProvider
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.IdGenerator
import org.cog.hymnchtv.notebook.model.NotebookValidation
import org.cog.hymnchtv.notebook.model.isActive
import org.cog.hymnchtv.notebook.repo.PlaylistRepository

class RoomPlaylistRepository(
    private val db: HymnchtvDatabase,
    private val clock: Clock,
    private val ids: IdGenerator,
    private val device: DeviceIdProvider,
) : PlaylistRepository {
    private val playlistDao get() = db.playlistDao()
    private val itemDao get() = db.playlistItemDao()

    override suspend fun findAll(): List<PlaylistEntity> = playlistDao.findActive()

    override suspend fun findById(id: String): PlaylistEntity? = playlistDao.findById(id)?.takeIf { it.isActive }

    override suspend fun create(item: PlaylistEntity): PlaylistEntity {
        val now = clock.nowMillis()
        val row = item.copy(
            id = resolveId(item.id, ids),
            name = NotebookValidation.playlistName(item.name),
            createdAt = now, updatedAt = now, deletedAt = null, updatedBy = device.deviceId(),
        )
        playlistDao.upsert(row)
        return row
    }

    override suspend fun update(item: PlaylistEntity): PlaylistEntity? = db.withTransaction {
        val existing = findById(item.id) ?: return@withTransaction null
        val row = item.copy(
            name = NotebookValidation.playlistName(item.name),
            createdAt = existing.createdAt, updatedAt = clock.nowMillis(), deletedAt = null, updatedBy = device.deviceId(),
        )
        playlistDao.upsert(row)
        row
    }

    override suspend fun delete(id: String): Boolean = db.withTransaction {
        val existing = findById(id) ?: return@withTransaction false
        val now = clock.nowMillis()
        val by = device.deviceId()
        playlistDao.upsert(existing.copy(updatedAt = now, deletedAt = now, updatedBy = by))
        val removed = itemDao.itemsOf(id).map { it.copy(updatedAt = now, deletedAt = now, updatedBy = by) }
        if (removed.isNotEmpty()) itemDao.upsertAll(removed)
        true
    }

    override suspend fun createPlaylist(name: String): PlaylistEntity =
        create(PlaylistEntity(id = "", name = name, createdAt = 0, updatedAt = 0))

    override suspend fun rename(id: String, name: String): PlaylistEntity? =
        findById(id)?.let { update(it.copy(name = name)) }

    override suspend fun items(playlistId: String): List<PlaylistItemEntity> = itemDao.itemsOf(playlistId)

    override suspend fun addItem(playlistId: String, key: HymnKey): PlaylistItemEntity? = db.withTransaction {
        if (findById(playlistId) == null) return@withTransaction null
        val now = clock.nowMillis()
        val row = PlaylistItemEntity(
            id = ids.newId(), playlistId = playlistId,
            position = nextSlot(playlistId),
            hymn = key, createdAt = now, updatedAt = now, updatedBy = device.deviceId(),
        )
        itemDao.insert(row)
        row
    }

    /** Composes the existing writes inside one Room transaction (nested withTransaction joins it); no new DAO query. */
    override suspend fun createPlaylistWithItem(name: String, key: HymnKey): Pair<PlaylistEntity, PlaylistItemEntity> =
        db.withTransaction {
            val playlist = createPlaylist(name)
            playlist to checkNotNull(addItem(playlist.id, key)) { "The new playlist vanished inside its own transaction" }
        }

    override suspend fun removeItem(itemId: String): Boolean = db.withTransaction {
        val existing = itemDao.findById(itemId)?.takeIf { it.isActive } ?: return@withTransaction false
        val now = clock.nowMillis()
        itemDao.upsert(existing.copy(updatedAt = now, deletedAt = now, updatedBy = device.deviceId()))
        true
    }

    /** Moves every item, in the new order, past the highest slot ever used, so no unique (playlistId, position) clash. */
    override suspend fun reorder(playlistId: String, orderedItemIds: List<String>): List<PlaylistItemEntity> =
        db.withTransaction {
            val current = itemDao.itemsOf(playlistId)
            require(orderedItemIds.size == current.size && orderedItemIds.toSet() == current.map { it.id }.toSet()) {
                "orderedItemIds must be a permutation of the playlist's active items"
            }
            if (orderedItemIds == current.map { it.id }) return@withTransaction current
            val byId = current.associateBy { it.id }
            val base = nextSlot(playlistId)
            val now = clock.nowMillis()
            val by = device.deviceId()
            val reordered = orderedItemIds.mapIndexed { index, id ->
                byId.getValue(id).copy(position = base + index, updatedAt = now, updatedBy = by)
            }
            itemDao.upsertAll(reordered)
            reordered
        }

    private suspend fun nextSlot(playlistId: String): Int = (itemDao.maxPositionIncludingDeleted(playlistId) ?: -1) + 1
}
