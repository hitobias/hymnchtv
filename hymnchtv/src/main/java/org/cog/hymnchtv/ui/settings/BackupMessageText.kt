package org.cog.hymnchtv.ui.settings

import android.content.Context
import org.cog.hymnchtv.R
import org.cog.hymnchtv.notebook.backup.BackupError
import org.cog.hymnchtv.ui.settings.backup.BackupMessage

/** Localized text of a [BackupMessage]. */
object BackupMessageText {
    fun of(ctx: Context, message: BackupMessage): String = when (message) {
        is BackupMessage.Exported -> ctx.getString(R.string.backup_export_done, message.rows)
        is BackupMessage.Imported -> {
            val done = ctx.getString(R.string.backup_import_done, message.inserted, message.updated)
            if (message.skipped > 0) done + "\n" + ctx.getString(R.string.backup_import_skipped, message.skipped) else done
        }
        is BackupMessage.Failed -> ctx.getString(
            when (message.error) {
                BackupError.NOT_JSON, BackupError.WRONG_FORMAT -> R.string.backup_error_format
                BackupError.UNSUPPORTED_VERSION -> R.string.backup_error_version
                BackupError.TOO_LARGE -> R.string.backup_error_too_large
                BackupError.IO -> R.string.backup_error_io
                BackupError.STORAGE -> R.string.backup_error_storage
            },
        )
    }
}
