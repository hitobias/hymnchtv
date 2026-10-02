package org.cog.hymnchtv.notebook.data.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.SyncRecord

/** A personal note on a hymn, optionally linked to one sing log (D-1b builds the UI). */
@Entity(
    tableName = "note",
    indices = [
        Index(value = [HymnKey.COL_TYPE, HymnKey.COL_NO, HymnKey.COL_FU, "createdAt"]),
        Index(value = ["singLogId"]),
    ],
)
data class NoteEntity(
    @PrimaryKey override val id: String,
    @Embedded val hymn: HymnKey,
    val body: String,
    val singLogId: String? = null,
    override val createdAt: Long,
    override val updatedAt: Long,
    override val deletedAt: Long? = null,
    override val updatedBy: String = "",
) : SyncRecord
