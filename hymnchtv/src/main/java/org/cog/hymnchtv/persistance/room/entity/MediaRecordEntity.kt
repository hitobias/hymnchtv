package org.cog.hymnchtv.persistance.room.entity

import androidx.room.Entity

/**
 * One media link or local file for a hymn. The six former per-hymn-book tables are normalized into this one
 * table (Room has no dynamic table names). The composite primary key keeps the old
 * UNIQUE(hymn_no, isFu, media_type) ON CONFLICT REPLACE semantics: writing the same key again overwrites the row.
 * [mediaUri] and [mediaFilePath] are nullable: link-only records carry no file path and vice versa.
 */
@Entity(tableName = "media_record", primaryKeys = ["hymnType", "hymnNo", "isFu", "mediaType"])
data class MediaRecordEntity(
    val hymnType: String,
    val hymnNo: Int,
    val isFu: Boolean,
    val mediaType: String,
    val mediaUri: String?,
    val mediaFilePath: String?,
)
