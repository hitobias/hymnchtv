package org.cog.hymnchtv.notebook.data.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.Occasion
import org.cog.hymnchtv.notebook.model.SingSource
import org.cog.hymnchtv.notebook.model.SyncRecord

/** One time a hymn was sung. No row limit (unlike hymnHistory). */
@Entity(
    tableName = "sing_log",
    indices = [
        Index(value = [HymnKey.COL_TYPE, HymnKey.COL_NO, HymnKey.COL_FU, "sungAt"]),
        Index(value = ["sungAt"]),
        Index(value = ["playlistId"]),
    ],
)
data class SingLogEntity(
    @PrimaryKey override val id: String,
    @Embedded val hymn: HymnKey,
    val sungAt: Long,
    val occasion: Occasion,
    val source: SingSource,
    val playlistId: String? = null,
    override val createdAt: Long,
    override val updatedAt: Long,
    override val deletedAt: Long? = null,
    override val updatedBy: String = "",
) : SyncRecord
