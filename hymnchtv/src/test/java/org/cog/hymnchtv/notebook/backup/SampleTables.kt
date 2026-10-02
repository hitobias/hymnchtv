package org.cog.hymnchtv.notebook.backup

import org.cog.hymnchtv.notebook.data.entity.FavoriteEntity
import org.cog.hymnchtv.notebook.data.entity.NoteEntity
import org.cog.hymnchtv.notebook.data.entity.PlaylistEntity
import org.cog.hymnchtv.notebook.data.entity.PlaylistItemEntity
import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
import org.cog.hymnchtv.notebook.fakes.testUuid
import org.cog.hymnchtv.notebook.model.FavoriteIds
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.cog.hymnchtv.notebook.model.Occasion
import org.cog.hymnchtv.notebook.model.SingSource
import org.cog.hymnchtv.notebook.model.SyncRecord

internal object SampleTables {
    const val DEVICE_A = "00000000-0000-0000-0000-00000000000a"
    const val DEVICE_B = "00000000-0000-0000-0000-00000000000b"
    val LOG_1 = testUuid(101)
    val LOG_2 = testUuid(102)
    val LOG_3 = testUuid(103)
    val NOTE_1 = testUuid(201)
    val NOTE_2 = testUuid(202)
    val PL_1 = testUuid(301)
    val IT_1 = testUuid(401)
    val IT_2 = testUuid(402)

    val db1 = HymnKey.of(HymnTypes.DB, 1)
    val fu1 = HymnKey.of(HymnTypes.DB, 781)
    val bb5 = HymnKey.of(HymnTypes.BB, 5)

    fun favorite(key: HymnKey, updatedAt: Long = 100, deletedAt: Long? = null, updatedBy: String = DEVICE_A) =
        FavoriteEntity(FavoriteIds.forKey(key), key, createdAt = 100, updatedAt = updatedAt, deletedAt = deletedAt, updatedBy = updatedBy)

    fun singLog(
        id: String,
        key: HymnKey = db1,
        sungAt: Long = 1_000,
        updatedAt: Long = 100,
        deletedAt: Long? = null,
        occasion: Occasion = Occasion.LORDS_DAY,
        source: SingSource = SingSource.MANUAL,
        playlistId: String? = null,
        updatedBy: String = DEVICE_A,
    ) = SingLogEntity(
        id = id, hymn = key, sungAt = sungAt, occasion = occasion, source = source, playlistId = playlistId,
        createdAt = 100, updatedAt = updatedAt, deletedAt = deletedAt, updatedBy = updatedBy,
    )

    fun note(
        id: String,
        key: HymnKey = db1,
        body: String = "主啊，我愛你\n\"引號\" / \\ 反斜線",
        singLogId: String? = null,
        updatedAt: Long = 100,
        deletedAt: Long? = null,
        updatedBy: String = DEVICE_A,
    ) = NoteEntity(id, key, body, singLogId, createdAt = 100, updatedAt = updatedAt, deletedAt = deletedAt, updatedBy = updatedBy)

    fun playlist(id: String, name: String = "主日 10/4", updatedAt: Long = 100, deletedAt: Long? = null) =
        PlaylistEntity(id, name, createdAt = 100, updatedAt = updatedAt, deletedAt = deletedAt, updatedBy = DEVICE_A)

    fun item(
        id: String,
        playlistId: String,
        position: Int,
        key: HymnKey = db1,
        updatedAt: Long = 100,
        deletedAt: Long? = null,
        updatedBy: String = DEVICE_A,
    ) = PlaylistItemEntity(id, playlistId, position, key, createdAt = 100, updatedAt = updatedAt, deletedAt = deletedAt, updatedBy = updatedBy)

    /** 10 rows: every column type, nulls, soft-deleted rows, CJK and escape-worthy text. */
    fun full() = NotebookTables(
        favorites = listOf(favorite(db1), favorite(bb5, updatedAt = 200, deletedAt = 200)),
        singLogs = listOf(
            singLog(LOG_1),
            singLog(LOG_2, key = fu1, occasion = Occasion.SMALL_GROUP, source = SingSource.AUTO, playlistId = PL_1),
            singLog(LOG_3, updatedAt = 300, deletedAt = 300),
        ),
        notes = listOf(note(NOTE_1, singLogId = LOG_1), note(NOTE_2, key = fu1)),
        playlists = listOf(playlist(PL_1)),
        playlistItems = listOf(item(IT_1, PL_1, 0), item(IT_2, PL_1, 1, key = fu1)),
    )

    /** Applies upserts by id, the same way BackupStore implementations do. */
    fun NotebookTables.upserted(changes: NotebookTables) = NotebookTables(
        upsertRows(favorites, changes.favorites),
        upsertRows(singLogs, changes.singLogs),
        upsertRows(notes, changes.notes),
        upsertRows(playlists, changes.playlists),
        upsertRows(playlistItems, changes.playlistItems),
    )

    private fun <T : SyncRecord> upsertRows(base: List<T>, changes: List<T>): List<T> =
        (base.associateBy { it.id } + changes.associateBy { it.id }).values.toList()
}
