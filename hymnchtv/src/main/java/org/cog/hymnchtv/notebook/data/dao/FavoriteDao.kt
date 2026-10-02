package org.cog.hymnchtv.notebook.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import org.cog.hymnchtv.notebook.data.entity.FavoriteEntity

/** Upsert is safe here: the id is derived from the hymn, so the PK and the unique hymn index always agree. */
@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorite WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    suspend fun findActive(): List<FavoriteEntity>

    @Query("SELECT * FROM favorite")
    suspend fun findAllIncludingDeleted(): List<FavoriteEntity>

    @Query("SELECT * FROM favorite WHERE id = :id")
    suspend fun findById(id: String): FavoriteEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM favorite WHERE " + HYMN_MATCH + " AND deletedAt IS NULL)")
    suspend fun isFavorite(hymnType: String, hymnNo: Int, isFu: Boolean): Boolean

    @Upsert
    suspend fun upsert(row: FavoriteEntity)

    @Upsert
    suspend fun upsertAll(rows: List<FavoriteEntity>)
}
