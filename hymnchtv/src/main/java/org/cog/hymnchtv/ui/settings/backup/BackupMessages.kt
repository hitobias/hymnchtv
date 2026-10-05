package org.cog.hymnchtv.ui.settings.backup

import org.cog.hymnchtv.notebook.backup.BackupError
import org.cog.hymnchtv.notebook.backup.ExportResult
import org.cog.hymnchtv.notebook.backup.ImportResult

/** What the settings page tells the user after a backup job; BackupMessageText turns it into localized text. */
sealed interface BackupMessage {
    data class Exported(val rows: Int) : BackupMessage

    data class Imported(val inserted: Int, val updated: Int, val skipped: Int) : BackupMessage

    data class Failed(val error: BackupError) : BackupMessage
}

object BackupMessages {
    fun of(result: ExportResult): BackupMessage = when (result) {
        is ExportResult.Success -> BackupMessage.Exported(result.rowCount)
        is ExportResult.Failure -> BackupMessage.Failed(result.error)
    }

    fun of(result: ImportResult): BackupMessage = when (result) {
        is ImportResult.Success -> BackupMessage.Imported(result.stats.inserted, result.stats.updated, result.skipped.total)
        is ImportResult.Failure -> BackupMessage.Failed(result.error)
    }
}
