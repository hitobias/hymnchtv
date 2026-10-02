package org.cog.hymnchtv.notebook.data.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.SyncRecord

/**
 * position is a sparse, only-growing sort key, unique per playlist including soft-deleted rows.
 * No foreign key on playlistId: rows may arrive out of order from backups or future sync.
 */
@Entity(
    tableName = "playlist_item",
    indices = [Index(value = ["playlistId", "position"], unique = true)],
)
data class PlaylistItemEntity(
    @PrimaryKey override val id: String,
    val playlistId: String,
    val position: Int,
    @Embedded val hymn: HymnKey,
    override val createdAt: Long,
    override val updatedAt: Long,
    override val deletedAt: Long? = null,
    override val updatedBy: String = "",
) : SyncRecord
