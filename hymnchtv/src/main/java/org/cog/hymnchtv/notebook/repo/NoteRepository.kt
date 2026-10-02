package org.cog.hymnchtv.notebook.repo

import org.cog.hymnchtv.notebook.data.entity.NoteEntity
import org.cog.hymnchtv.notebook.model.HymnKey

interface NoteRepository : Repository<NoteEntity> {
    suspend fun add(key: HymnKey, body: String, singLogId: String? = null): NoteEntity

    /** Active notes of one hymn, newest first. */
    suspend fun findByHymn(key: HymnKey): List<NoteEntity>
}
