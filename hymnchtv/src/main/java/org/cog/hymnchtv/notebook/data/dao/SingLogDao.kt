package org.cog.hymnchtv.notebook.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import org.cog.hymnchtv.notebook.data.SingStats
import org.cog.hymnchtv.notebook.data.entity.SingLogEntity

@Dao
interface SingLogDao {
    @Query("SELECT * FROM sing_log WHERE deletedAt IS NULL ORDER BY sungAt DESC")
    suspend fun findActive(): List<SingLogEntity>

    @Query("SELECT * FROM sing_log")
    suspend fun findAllIncludingDeleted(): List<SingLogEntity>

    @Query("SELECT * FROM sing_log WHERE id = :id")
    suspend fun findById(id: String): SingLogEntity?

    @Query(
        "SELECT COUNT(*) AS singCount, MAX(sungAt) AS lastSungAt FROM sing_log WHERE " + HYMN_MATCH +
            " AND deletedAt IS NULL",
    )
    suspend fun statsFor(hymnType: String, hymnNo: Int, isFu: Boolean): SingStats

    @Query("SELECT * FROM sing_log WHERE " + HYMN_MATCH + " AND deletedAt IS NULL ORDER BY sungAt DESC LIMIT 1")
    suspend fun latestForHymn(hymnType: String, hymnNo: Int, isFu: Boolean): SingLogEntity?

    /** Dedupe probe used inside the recordUnlessDuplicate transaction; both bounds exclusive. */
    @Query(
        "SELECT EXISTS(SELECT 1 FROM sing_log WHERE " + HYMN_MATCH +
            " AND deletedAt IS NULL AND sungAt > :fromExclusive AND sungAt < :toExclusive)",
    )
    suspend fun existsBetween(hymnType: String, hymnNo: Int, isFu: Boolean, fromExclusive: Long, toExclusive: Long): Boolean

    @Query("SELECT * FROM sing_log WHERE " + HYMN_MATCH + " AND deletedAt IS NULL ORDER BY sungAt DESC")
    suspend fun findByHymn(hymnType: String, hymnNo: Int, isFu: Boolean): List<SingLogEntity>

    @Query(
        "SELECT * FROM sing_log WHERE deletedAt IS NULL AND sungAt >= :fromInclusive AND sungAt < :toExclusive " +
            "ORDER BY sungAt DESC",
    )
    suspend fun findBetween(fromInclusive: Long, toExclusive: Long): List<SingLogEntity>

    @Upsert
    suspend fun upsert(row: SingLogEntity)

    @Upsert
    suspend fun upsertAll(rows: List<SingLogEntity>)
}
