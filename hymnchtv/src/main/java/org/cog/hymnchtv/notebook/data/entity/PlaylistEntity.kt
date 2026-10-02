package org.cog.hymnchtv.notebook.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.cog.hymnchtv.notebook.model.SyncRecord

@Entity(tableName = "playlist")
data class PlaylistEntity(
    @PrimaryKey override val id: String,
    val name: String,
    override val createdAt: Long,
    override val updatedAt: Long,
    override val deletedAt: Long? = null,
    override val updatedBy: String = "",
) : SyncRecord
