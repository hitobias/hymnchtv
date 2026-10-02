package org.cog.hymnchtv.persistance.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import org.cog.hymnchtv.persistance.HistoryPrune
import org.cog.hymnchtv.persistance.room.entity.HymnHistoryEntity

@Dao
abstract class HymnHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(entity: HymnHistoryEntity): Long

    @Query("SELECT COUNT(*) FROM hymn_history")
    abstract fun count(): Int

    /** timeStamp of the [offset]-th (0-based) oldest row, null when there are fewer rows. */
    @Query("SELECT timeStamp FROM hymn_history ORDER BY timeStamp ASC LIMIT 1 OFFSET :offset")
    abstract fun timeStampAt(offset: Int): Long?

    /** Strictly older rows only: rows tied with the pivot are kept. */
    @Query("DELETE FROM hymn_history WHERE timeStamp < :pivot")
    abstract fun deleteOlderThan(pivot: Long): Int

    /**
     * Legacy semantics: deletes by (hymnType, hymnNo) and deliberately ignores isFu, so the Fu and the non-Fu
     * row of the same number go together.
     */
    @Query("DELETE FROM hymn_history WHERE hymnType = :hymnType AND hymnNo = :hymnNo")
    abstract fun deleteByNumber(hymnType: String, hymnNo: Int): Int

    @Query("SELECT * FROM hymn_history ORDER BY timeStamp DESC, hymnType ASC, hymnNo ASC, isFu ASC")
    abstract fun listNewestFirst(): List<HymnHistoryEntity>

    /**
     * Purge (if over [limit]) and insert in ONE transaction, otherwise two concurrent writers could purge each
     * other's rows. Returns the number of purged rows.
     */
    @Transaction
    open fun purgeAndInsert(entity: HymnHistoryEntity, limit: Int): Int {
        var purged = 0
        val pivotIndex = HistoryPrune.pivotIndex(count(), limit)
        if (pivotIndex != null) {
            val pivot = timeStampAt(pivotIndex - 1)
            if (pivot != null) purged = deleteOlderThan(pivot)
        }
        insert(entity)
        return purged
    }
}
