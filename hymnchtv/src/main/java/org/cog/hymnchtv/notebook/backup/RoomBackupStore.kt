package org.cog.hymnchtv.notebook.backup

import androidx.room.withTransaction
import org.cog.hymnchtv.persistance.room.HymnchtvDatabase
import org.cog.hymnchtv.notebook.data.entity.PlaylistItemEntity

/**
 * Upserts are safe for the merged rows: favorite ids are derived from the hymn, every other table is keyed by
 * UUID only, and BackupMerger re-slots playlist items so their *final* (playlistId, position) is unique.
 * Playlist items are written in two phases so swaps and cycles never collide mid-transaction.
 */
class RoomBackupStore(private val db: HymnchtvDatabase) : BackupStore {
    override suspend fun readAll(): NotebookTables = db.withTransaction { readAllRows() }

    override suspend fun <R> mergeAtomically(plan: (local: NotebookTables) -> Planned<R>): R =
        db.withTransaction {
            val planned = plan(readAllRows())
            writeRows(planned.upserts)
            planned.result
        }

    private suspend fun readAllRows() = NotebookTables(
        favorites = db.favoriteDao().findAllIncludingDeleted(),
        singLogs = db.singLogDao().findAllIncludingDeleted(),
        notes = db.noteDao().findAllIncludingDeleted(),
        playlists = db.playlistDao().findAllIncludingDeleted(),
        playlistItems = db.playlistItemDao().findAllIncludingDeleted(),
    )

    private suspend fun writeRows(rows: NotebookTables) {
        if (rows.favorites.isNotEmpty()) db.favoriteDao().upsertAll(rows.favorites)
        if (rows.singLogs.isNotEmpty()) db.singLogDao().upsertAll(rows.singLogs)
        if (rows.notes.isNotEmpty()) db.noteDao().upsertAll(rows.notes)
        if (rows.playlists.isNotEmpty()) db.playlistDao().upsertAll(rows.playlists)
        writePlaylistItems(rows.playlistItems)
    }

    /**
     * Phase 1 parks every row on its own negative slot (real positions are always >= 0), vacating all old slots of
     * the rows being moved; phase 2 writes the final slots, which BackupMerger guarantees are free. Both phases run
     * inside the caller's transaction, so readers never see the parked positions.
     */
    private suspend fun writePlaylistItems(items: List<PlaylistItemEntity>) {
        if (items.isEmpty()) return
        val dao = db.playlistItemDao()
        dao.upsertAll(items.mapIndexed { index, item -> item.copy(position = -1 - index) })
        dao.upsertAll(items)
    }
}
