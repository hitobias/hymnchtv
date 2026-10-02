package org.cog.hymnchtv.persistance.room.entity

/** The primary key of a [MediaRecordEntity]; a query projection, not a table. */
data class MediaRecordKey(
    val hymnType: String,
    val hymnNo: Int,
    val isFu: Boolean,
    val mediaType: String,
)
