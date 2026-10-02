package org.cog.hymnchtv.notebook.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import org.cog.hymnchtv.notebook.data.entity.PlaylistEntity

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlist WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    suspend fun findActive(): List<PlaylistEntity>

    @Query("SELECT * FROM playlist")
    suspend fun findAllIncludingDeleted(): List<PlaylistEntity>

    @Query("SELECT * FROM playlist WHERE id = :id")
    suspend fun findById(id: String): PlaylistEntity?

    @Upsert
    suspend fun upsert(row: PlaylistEntity)

    @Upsert
    suspend fun upsertAll(rows: List<PlaylistEntity>)
}
