package org.cog.hymnchtv.persistance.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.cog.hymnchtv.persistance.room.entity.EnglishLyricsEntity

@Dao
interface EnglishLyricsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: EnglishLyricsEntity): Long

    @Query("DELETE FROM english_lyrics WHERE hymnNoEng = :hymnNoEng")
    fun delete(hymnNoEng: Int): Int

    @Query("SELECT lyricsEng FROM english_lyrics WHERE hymnNoEng = :hymnNoEng")
    fun lyrics(hymnNoEng: Int): String?
}
