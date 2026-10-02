package org.cog.hymnchtv.persistance.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.cog.hymnchtv.persistance.room.entity.MediaRecordEntity

/** Rows come back ordered by hymnNo, then isFu, then mediaType so same-number records have a defined order. */
@Dao
interface MediaRecordDao {
    /** REPLACE keeps the old "same key overwrites" behaviour; returns the rowid (-1 on failure). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: MediaRecordEntity): Long

    @Query(
        "SELECT * FROM media_record WHERE hymnType = :hymnType AND hymnNo = :hymnNo AND isFu = :isFu " +
            "AND mediaType = :mediaType"
    )
    fun find(hymnType: String, hymnNo: Int, isFu: Boolean, mediaType: String): MediaRecordEntity?

    @Query(
        "DELETE FROM media_record WHERE hymnType = :hymnType AND hymnNo = :hymnNo AND isFu = :isFu " +
            "AND mediaType = :mediaType"
    )
    fun delete(hymnType: String, hymnNo: Int, isFu: Boolean, mediaType: String): Int

    @Query("SELECT * FROM media_record WHERE hymnType = :hymnType ORDER BY hymnNo ASC, isFu ASC, mediaType ASC")
    fun listByType(hymnType: String): List<MediaRecordEntity>

    @Query(
        "SELECT * FROM media_record WHERE hymnType = :hymnType AND hymnNo = :hymnNo AND isFu = :isFu " +
            "ORDER BY hymnNo ASC, isFu ASC, mediaType ASC"
    )
    fun listByHymn(hymnType: String, hymnNo: Int, isFu: Boolean): List<MediaRecordEntity>

    /** Records whose mediaUri is a web link (LIKE is case-insensitive, as before). */
    @Query(
        "SELECT * FROM media_record WHERE hymnType = :hymnType AND mediaUri LIKE 'http%' " +
            "ORDER BY hymnNo ASC, isFu ASC, mediaType ASC"
    )
    fun linksByType(hymnType: String): List<MediaRecordEntity>
}
