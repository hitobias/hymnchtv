package org.cog.hymnchtv.notebook.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import org.cog.hymnchtv.notebook.data.entity.PlaylistItemEntity

/**
 * New items use insert (ABORT): Room's @Upsert falls back to UPDATE-by-PK on *any* unique violation, which would
 * silently drop a new row that collides on (playlistId, position). Upsert is used only for rows whose slot is known free.
 */
@Dao
interface PlaylistItemDao {
    @Query(
        "SELECT * FROM playlist_item WHERE playlistId = :playlistId AND deletedAt IS NULL " +
            "ORDER BY position ASC, createdAt ASC",
    )
    suspend fun itemsOf(playlistId: String): List<PlaylistItemEntity>

    /** Highest slot ever used in the playlist (soft-deleted rows keep their slot under the unique index). */
    @Query("SELECT MAX(position) FROM playlist_item WHERE playlistId = :playlistId")
    suspend fun maxPositionIncludingDeleted(playlistId: String): Int?

    @Query("SELECT * FROM playlist_item")
    suspend fun findAllIncludingDeleted(): List<PlaylistItemEntity>

    @Query("SELECT * FROM playlist_item WHERE id = :id")
    suspend fun findById(id: String): PlaylistItemEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(row: PlaylistItemEntity)

    @Upsert
    suspend fun upsert(row: PlaylistItemEntity)

    @Upsert
    suspend fun upsertAll(rows: List<PlaylistItemEntity>)
}
