package org.cog.hymnchtv.ui.settings.backup

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.cog.hymnchtv.notebook.backup.BackupError
import org.cog.hymnchtv.notebook.backup.ExportResult
import org.cog.hymnchtv.notebook.backup.ImportResult
import timber.log.Timber

/** [running] disables the backup entries; [message] is shown once and then consumed. */
data class BackupUiState(val running: Boolean = false, val message: BackupMessage? = null)

/**
 * Runs one export or import at a time in [scope] (the settings ViewModel's, so rotation does not interrupt it). [D] is the
 * document handle: a SAF Uri in the app, a String in tests. Main-thread only.
 */
class BackupRunner<D>(
    private val scope: CoroutineScope,
    private val exportTo: suspend (D) -> ExportResult,
    private val importFrom: suspend (D) -> ImportResult,
) {
    private val mutable = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = mutable.asStateFlow()

    /** False, and nothing starts, while another job runs. */
    fun runExport(document: D): Boolean = start { BackupMessages.of(exportTo(document)) }

    fun runImport(document: D): Boolean = start { BackupMessages.of(importFrom(document)) }

    fun consumeMessage() {
        mutable.value = mutable.value.copy(message = null)
    }

    private fun start(work: suspend () -> BackupMessage): Boolean {
        if (mutable.value.running) return false
        mutable.value = BackupUiState(running = true)
        scope.launch {
            val message = try {
                work()
            } catch (e: CancellationException) {
                mutable.value = BackupUiState()
                throw e
            } catch (e: Exception) {
                // BackupService maps its own errors; anything reaching here is a bug, reported as a storage failure
                Timber.e(e, "Notebook backup job failed unexpectedly")
                BackupMessage.Failed(BackupError.STORAGE)
            }
            mutable.value = BackupUiState(running = false, message = message)
        }
        return true
    }
}
