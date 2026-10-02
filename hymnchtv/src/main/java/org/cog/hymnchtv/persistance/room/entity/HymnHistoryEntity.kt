package org.cog.hymnchtv.persistance.room.entity

import androidx.room.Entity

/** One entry of the "recently viewed" list; the same (hymnType, hymnNo, isFu) overwrites the older row. */
@Entity(tableName = "hymn_history", primaryKeys = ["hymnType", "hymnNo", "isFu"])
data class HymnHistoryEntity(
    val hymnType: String,
    val hymnNo: Int,
    val isFu: Boolean,
    val hymnTitle: String?,
    val timeStamp: Long,
)
