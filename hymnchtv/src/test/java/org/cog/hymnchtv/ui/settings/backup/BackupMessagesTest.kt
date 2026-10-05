package org.cog.hymnchtv.ui.settings.backup

import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.notebook.backup.BackupError
import org.cog.hymnchtv.notebook.backup.ExportResult
import org.cog.hymnchtv.notebook.backup.ImportResult
import org.cog.hymnchtv.notebook.backup.MergeStats
import org.cog.hymnchtv.notebook.backup.SkippedRows
import org.junit.Test

class BackupMessagesTest {
    @Test fun exportSuccessAndFailure() {
        assertThat(BackupMessages.of(ExportResult.Success(12))).isEqualTo(BackupMessage.Exported(12))
        assertThat(BackupMessages.of(ExportResult.Failure(BackupError.IO, "x"))).isEqualTo(BackupMessage.Failed(BackupError.IO))
    }

    @Test fun importReportsNewUpdatedAndAllSkippedRows() {
        val result = ImportResult.Success(MergeStats(inserted = 3, updated = 2, unchanged = 9), SkippedRows(invalid = 1, futureTimestamp = 2))
        assertThat(BackupMessages.of(result)).isEqualTo(BackupMessage.Imported(inserted = 3, updated = 2, skipped = 3))
    }

    @Test fun everyErrorStaysDistinguishable() {
        BackupError.values().forEach {
            assertThat(BackupMessages.of(ImportResult.Failure(it, "detail"))).isEqualTo(BackupMessage.Failed(it))
        }
    }
}
