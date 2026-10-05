package org.cog.hymnchtv.ui.settings.backup

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.cog.hymnchtv.notebook.backup.BackupError
import org.cog.hymnchtv.notebook.backup.ExportResult
import org.cog.hymnchtv.notebook.backup.ImportResult
import org.cog.hymnchtv.notebook.backup.MergeStats
import org.cog.hymnchtv.notebook.backup.SkippedRows
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BackupRunnerTest {
    @Test fun anExportRunsThenReportsAndTheMessageIsConsumedOnce() = runTest {
        val gate = CompletableDeferred<Unit>()
        val seen = mutableListOf<String>()
        val runner = BackupRunner<String>(
            backgroundScope,
            exportTo = { doc -> seen += doc; gate.await(); ExportResult.Success(5) },
            importFrom = { error("not used") },
        )
        assertThat(runner.runExport("out.json")).isTrue()
        runCurrent()
        assertThat(runner.state.value).isEqualTo(BackupUiState(running = true))
        gate.complete(Unit)
        runCurrent()
        assertThat(seen).containsExactly("out.json")
        assertThat(runner.state.value).isEqualTo(BackupUiState(running = false, message = BackupMessage.Exported(5)))
        runner.consumeMessage()
        assertThat(runner.state.value).isEqualTo(BackupUiState())
    }

    @Test fun aSecondJobWhileOneRunsIsRefused() = runTest {
        val gate = CompletableDeferred<Unit>()
        var imports = 0
        val runner = BackupRunner<String>(
            backgroundScope,
            exportTo = { gate.await(); ExportResult.Success(1) },
            importFrom = { imports++; ImportResult.Success(MergeStats.ZERO, SkippedRows.NONE) },
        )
        assertThat(runner.runExport("a")).isTrue()
        assertThat(runner.runImport("b")).isFalse()
        gate.complete(Unit)
        runCurrent()
        assertThat(imports).isEqualTo(0)
        assertThat(runner.runImport("b")).isTrue()
        runCurrent()
        assertThat(imports).isEqualTo(1)
        assertThat(runner.state.value.message).isEqualTo(BackupMessage.Imported(0, 0, 0))
    }

    @Test fun anUnexpectedExceptionBecomesAStorageFailure() = runTest {
        val runner = BackupRunner<String>(
            backgroundScope,
            exportTo = { throw IllegalStateException("boom") },
            importFrom = { error("not used") },
        )
        runner.runExport("a")
        runCurrent()
        assertThat(runner.state.value).isEqualTo(BackupUiState(running = false, message = BackupMessage.Failed(BackupError.STORAGE)))
    }
}
