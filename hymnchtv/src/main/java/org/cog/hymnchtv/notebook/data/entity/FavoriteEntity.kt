package org.cog.hymnchtv.notebook.data.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.SyncRecord

/** One row per hymn ever favorited; id = FavoriteIds.forKey(hymn); un-favoriting is a soft delete. */
@Entity(
    tableName = "favorite",
    indices = [Index(value = [HymnKey.COL_TYPE, HymnKey.COL_NO, HymnKey.COL_FU], unique = true)],
)
data class FavoriteEntity(
    @PrimaryKey override val id: String,
    @Embedded val hymn: HymnKey,
    override val createdAt: Long,
    override val updatedAt: Long,
    override val deletedAt: Long? = null,
    override val updatedBy: String = "",
) : SyncRecord
