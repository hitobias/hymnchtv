package org.cog.hymnchtv.notebook.fakes

import org.cog.hymnchtv.notebook.data.entity.PlaylistEntity
import org.cog.hymnchtv.notebook.data.entity.PlaylistItemEntity
import org.cog.hymnchtv.notebook.model.Clock
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.IdGenerator
import org.cog.hymnchtv.notebook.model.NotebookValidation
import org.cog.hymnchtv.notebook.model.isActive
import org.cog.hymnchtv.notebook.repo.PlaylistRepository

/** PlaylistRepository in memory; PlaylistRepositoryContract keeps it in line with RoomPlaylistRepository. */
class InMemoryPlaylistRepository(
    private val clock: Clock,
    private val ids: IdGenerator = SequentialIds(),
    private val device: String = TEST_DEVICE,
) : PlaylistRepository {
    @Volatile
    var playlists: Map<String, PlaylistEntity> = emptyMap()
        private set

    @Volatile
    var allItems: Map<String, PlaylistItemEntity> = emptyMap()
        private set

    /** The next call throws once (to test error paths). */
    @Volatile
    var failNext: Boolean = false

    /** The next addItem throws after its checks, i.e. half-way through createPlaylistWithItem. */
    @Volatile
    var failNextAddItem: Boolean = false

    private fun enter() {
        if (failNext) {
            failNext = false
            throw IllegalStateException("simulated failure")
        }
    }

    private fun putPlaylist(row: PlaylistEntity): PlaylistEntity {
        playlists = playlists + (row.id to row)
        return row
    }

    private fun putItems(rows: List<PlaylistItemEntity>) {
        allItems = allItems + rows.associateBy { it.id }
    }

    override suspend fun findAll(): List<PlaylistEntity> {
        enter()
        return playlists.values.filter { it.isActive }.sortedWith(compareByDescending<PlaylistEntity> { it.updatedAt }.thenBy { it.id })
    }

    override suspend fun findById(id: String): PlaylistEntity? = playlists[id]?.takeIf { it.isActive }

    override suspend fun create(item: PlaylistEntity): PlaylistEntity {
        enter()
        val now = clock.nowMillis()
        return putPlaylist(
            item.copy(
                id = NotebookValidation.uuid(item.id.ifBlank { ids.newId() }), name = NotebookValidation.playlistName(item.name),
                createdAt = now, updatedAt = now, deletedAt = null, updatedBy = device,
            ),
        )
    }

    override suspend fun update(item: PlaylistEntity): PlaylistEntity? {
        enter()
        val existing = findById(item.id) ?: return null
        return putPlaylist(
            item.copy(
                name = NotebookValidation.playlistName(item.name), createdAt = existing.createdAt,
                updatedAt = clock.nowMillis(), deletedAt = null, updatedBy = device,
            ),
        )
    }

    override suspend fun delete(id: String): Boolean {
        enter()
        val existing = findById(id) ?: return false
        val now = clock.nowMillis()
        putPlaylist(existing.copy(updatedAt = now, deletedAt = now, updatedBy = device))
        putItems(activeItems(id).map { it.copy(updatedAt = now, deletedAt = now, updatedBy = device) })
        return true
    }

    override suspend fun createPlaylist(name: String): PlaylistEntity =
        create(PlaylistEntity(id = "", name = name, createdAt = 0, updatedAt = 0))

    override suspend fun rename(id: String, name: String): PlaylistEntity? = findById(id)?.let { update(it.copy(name = name)) }

    override suspend fun items(playlistId: String): List<PlaylistItemEntity> {
        enter()
        return activeItems(playlistId)
    }

    override suspend fun addItem(playlistId: String, key: HymnKey): PlaylistItemEntity? {
        enter()
        if (findById(playlistId) == null) return null
        if (failNextAddItem) {
            failNextAddItem = false
            throw IllegalStateException("simulated failure")
        }
        val now = clock.nowMillis()
        val row = PlaylistItemEntity(
            id = ids.newId(), playlistId = playlistId, position = nextSlot(playlistId), hymn = key,
            createdAt = now, updatedAt = now, updatedBy = device,
        )
        putItems(listOf(row))
        return row
    }

    /** Like Room's transaction: on any failure both maps go back to what they were. */
    override suspend fun createPlaylistWithItem(name: String, key: HymnKey): Pair<PlaylistEntity, PlaylistItemEntity> {
        val savedPlaylists = playlists
        val savedItems = allItems
        try {
            val playlist = createPlaylist(name)
            return playlist to checkNotNull(addItem(playlist.id, key))
        } catch (e: Exception) {
            playlists = savedPlaylists
            allItems = savedItems
            throw e
        }
    }

    override suspend fun removeItem(itemId: String): Boolean {
        enter()
        val existing = allItems[itemId]?.takeIf { it.isActive } ?: return false
        val now = clock.nowMillis()
        putItems(listOf(existing.copy(updatedAt = now, deletedAt = now, updatedBy = device)))
        return true
    }

    override suspend fun reorder(playlistId: String, orderedItemIds: List<String>): List<PlaylistItemEntity> {
        enter()
        val current = activeItems(playlistId)
        require(orderedItemIds.size == current.size && orderedItemIds.toSet() == current.map { it.id }.toSet()) {
            "orderedItemIds must be a permutation of the playlist's active items"
        }
        if (orderedItemIds == current.map { it.id }) return current
        val byId = current.associateBy { it.id }
        val base = nextSlot(playlistId)
        val now = clock.nowMillis()
        val reordered = orderedItemIds.mapIndexed { index, id ->
            byId.getValue(id).copy(position = base + index, updatedAt = now, updatedBy = device)
        }
        putItems(reordered)
        return reordered
    }

    private fun activeItems(playlistId: String): List<PlaylistItemEntity> = allItems.values
        .filter { it.playlistId == playlistId && it.isActive }
        .sortedWith(compareBy<PlaylistItemEntity> { it.position }.thenBy { it.createdAt })

    private fun nextSlot(playlistId: String): Int =
        (allItems.values.filter { it.playlistId == playlistId }.maxOfOrNull { it.position } ?: -1) + 1
}
