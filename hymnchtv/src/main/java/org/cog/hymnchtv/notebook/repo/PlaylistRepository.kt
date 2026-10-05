package org.cog.hymnchtv.notebook.repo

import org.cog.hymnchtv.notebook.data.entity.PlaylistEntity
import org.cog.hymnchtv.notebook.data.entity.PlaylistItemEntity
import org.cog.hymnchtv.notebook.model.HymnKey

/** delete(playlistId) also soft-deletes the playlist's items. Positions are sparse, only-growing sort keys. */
interface PlaylistRepository : Repository<PlaylistEntity> {
    suspend fun createPlaylist(name: String): PlaylistEntity

    suspend fun rename(id: String, name: String): PlaylistEntity?

    /** Active items ordered by position. */
    suspend fun items(playlistId: String): List<PlaylistItemEntity>

    /** Appends after the highest slot ever used (in one transaction); null when the playlist is missing or deleted. */
    suspend fun addItem(playlistId: String, key: HymnKey): PlaylistItemEntity?

    /** Creates a playlist and appends [key] in one transaction: if any part fails, nothing is stored (no empty playlist). */
    suspend fun createPlaylistWithItem(name: String, key: HymnKey): Pair<PlaylistEntity, PlaylistItemEntity>

    suspend fun removeItem(itemId: String): Boolean

    /** orderedItemIds must be a permutation of the active item ids; returns items in the new order. */
    suspend fun reorder(playlistId: String, orderedItemIds: List<String>): List<PlaylistItemEntity>
}
