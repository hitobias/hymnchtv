package org.cog.hymnchtv.notebook.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import org.cog.hymnchtv.notebook.data.entity.NoteEntity

@Dao
interface NoteDao {
    @Query("SELECT * FROM note WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    suspend fun findActive(): List<NoteEntity>

    @Query("SELECT * FROM note")
    suspend fun findAllIncludingDeleted(): List<NoteEntity>

    @Query("SELECT * FROM note WHERE id = :id")
    suspend fun findById(id: String): NoteEntity?

    @Query("SELECT * FROM note WHERE " + HYMN_MATCH + " AND deletedAt IS NULL ORDER BY createdAt DESC")
    suspend fun findByHymn(hymnType: String, hymnNo: Int, isFu: Boolean): List<NoteEntity>

    @Upsert
    suspend fun upsert(row: NoteEntity)

    @Upsert
    suspend fun upsertAll(rows: List<NoteEntity>)
}
