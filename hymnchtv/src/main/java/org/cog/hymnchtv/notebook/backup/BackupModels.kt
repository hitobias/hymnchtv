package org.cog.hymnchtv.notebook.backup

import org.cog.hymnchtv.notebook.data.entity.FavoriteEntity
import org.cog.hymnchtv.notebook.data.entity.NoteEntity
import org.cog.hymnchtv.notebook.data.entity.PlaylistEntity
import org.cog.hymnchtv.notebook.data.entity.PlaylistItemEntity
import org.cog.hymnchtv.notebook.data.entity.SingLogEntity

/** All rows of the five notebook tables (including soft-deleted rows when used for backup). */
data class NotebookTables(
    val favorites: List<FavoriteEntity> = emptyList(),
    val singLogs: List<SingLogEntity> = emptyList(),
    val notes: List<NoteEntity> = emptyList(),
    val playlists: List<PlaylistEntity> = emptyList(),
    val playlistItems: List<PlaylistItemEntity> = emptyList(),
) {
    val rowCount: Int
        get() = favorites.size + singLogs.size + notes.size + playlists.size + playlistItems.size

    companion object {
        @JvmField
        val EMPTY = NotebookTables()
    }
}

/** Contents of one backup file. [schemaVersion] versions the JSON layout, not the Room schema. */
data class BackupSnapshot(
    val schemaVersion: Int,
    val exportedAt: Long,
    val appVersionName: String,
    val tables: NotebookTables,
)

/** Hard limits for untrusted backup files. */
data class BackupLimits(
    val maxRowsPerTable: Int = 100_000,
    val maxNestingDepth: Int = 4,
    val maxFutureSkewMillis: Long = 24 * 60 * 60 * 1000L,
    val maxAppVersionLength: Int = 64,
) {
    init {
        require(maxRowsPerTable > 0 && maxNestingDepth > 0 && maxFutureSkewMillis >= 0 && maxAppVersionLength > 0)
    }
}

/** Rows dropped during decoding, by reason. */
data class SkippedRows(val invalid: Int = 0, val futureTimestamp: Int = 0) {
    val total: Int
        get() = invalid + futureTimestamp

    operator fun plus(other: SkippedRows) =
        SkippedRows(invalid + other.invalid, futureTimestamp + other.futureTimestamp)

    companion object {
        @JvmField
        val NONE = SkippedRows()
    }
}

/** UI maps each value to a localized message (UI wave). */
enum class BackupError { NOT_JSON, WRONG_FORMAT, UNSUPPORTED_VERSION, TOO_LARGE, IO, STORAGE }

sealed interface DecodeResult {
    data class Success(val snapshot: BackupSnapshot, val skipped: SkippedRows) : DecodeResult
    data class Failure(val error: BackupError, val detail: String) : DecodeResult
}

data class MergeStats(val inserted: Int, val updated: Int, val unchanged: Int) {
    operator fun plus(other: MergeStats) =
        MergeStats(inserted + other.inserted, updated + other.updated, unchanged + other.unchanged)

    companion object {
        @JvmField
        val ZERO = MergeStats(0, 0, 0)
    }
}

/** [changes] holds only rows to upsert (inserted, updated, or re-slotted playlist items). */
data class MergeResult(val changes: NotebookTables, val stats: MergeStats)

sealed interface ExportResult {
    data class Success(val rowCount: Int) : ExportResult
    data class Failure(val error: BackupError, val detail: String) : ExportResult
}

sealed interface ImportResult {
    data class Success(val stats: MergeStats, val skipped: SkippedRows) : ImportResult
    data class Failure(val error: BackupError, val detail: String) : ImportResult
}
