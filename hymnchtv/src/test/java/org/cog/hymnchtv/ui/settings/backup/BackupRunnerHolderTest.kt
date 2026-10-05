package org.cog.hymnchtv.ui.settings.backup

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.cog.hymnchtv.notebook.backup.ExportResult
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BackupRunnerHolderTest {
    @Test fun aJobStartedThroughOneLookupFinishesAndTheNextLookupShowsItsResult() = runTest {
        val gate = CompletableDeferred<Unit>()
        var created = 0
        // backgroundScope stands for the app-wide scope: nothing a screen does can cancel it
        val holder = BackupRunnerHolder<String> {
            created++
            BackupRunner(backgroundScope, { gate.await(); ExportResult.Success(7) }, { error("not used") })
        }
        holder.get().runExport("doc")
        runCurrent()
        // the screen is gone; a new screen asks again
        gate.complete(Unit)
        runCurrent()
        assertThat(created).isEqualTo(1)
        assertThat(holder.get().state.value).isEqualTo(BackupUiState(running = false, message = BackupMessage.Exported(7)))
    }
}
