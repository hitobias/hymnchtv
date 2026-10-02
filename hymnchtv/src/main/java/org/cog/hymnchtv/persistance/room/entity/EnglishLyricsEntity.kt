package org.cog.hymnchtv.persistance.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Downloaded English lyrics (html) of one English hymn; one row per hymn number. */
@Entity(tableName = "english_lyrics")
data class EnglishLyricsEntity(
    @PrimaryKey val hymnNoEng: Int,
    val lyricsEng: String?,
)
