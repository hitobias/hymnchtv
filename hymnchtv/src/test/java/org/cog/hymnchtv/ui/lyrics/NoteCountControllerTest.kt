package org.cog.hymnchtv.ui.lyrics

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.cog.hymnchtv.notebook.Cancellable
import org.cog.hymnchtv.notebook.NotebookAsync
import org.cog.hymnchtv.notebook.NotebookCallback
import org.cog.hymnchtv.notebook.Outcome
import org.cog.hymnchtv.notebook.backup.BackupIo
import org.cog.hymnchtv.notebook.backup.ExportResult
import org.cog.hymnchtv.notebook.backup.ImportResult
import org.cog.hymnchtv.notebook.fakes.FakeNotebookPrefs
import org.cog.hymnchtv.notebook.fakes.InMemoryFavoriteRepository
import org.cog.hymnchtv.notebook.fakes.InMemorySingLogRepository
import org.cog.hymnchtv.notebook.model.Clock
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.cog.hymnchtv.notebook.record.SingTracker
import org.junit.Test
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class NoteCountControllerTest {
    private val db1 = HymnKey.of(HymnTypes.DB, 1)
    private val db2 = HymnKey.of(HymnTypes.DB, 2)

    private object UnusedBackupIo : BackupIo {
        override suspend fun exportTo(uri: Uri): ExportResult = throw UnsupportedOperationException()
        override suspend fun importFrom(uri: Uri): ImportResult = throw UnsupportedOperationException()
    }

    private class Harness(scope: TestScope) {
        val clock = Clock { 1_790_733_600_000L + scope.testScheduler.currentTime }
        @Volatile var counts: Map<HymnKey, Int> = emptyMap()
        @Volatile var gate: CompletableDeferred<Unit>? = null
        @Volatile var fail = false
        private val singLogs = InMemorySingLogRepository(clock)
        private val prefs = FakeNotebookPrefs()
        private val tracker = SingTracker(singLogs, prefs, clock, scope.backgroundScope, zone = { TimeZone.getTimeZone("UTC") })
        val async = NotebookAsync(
            InMemoryFavoriteRepository(clock), singLogs, prefs, tracker, UnusedBackupIo,
            noteCounter = { key ->
                gate?.await()
                if (fail) throw IllegalStateException("simulated")
                counts[key] ?: 0
            },
            callbackDispatcher = UnconfinedTestDispatcher(scope.testScheduler),
            workDispatcher = UnconfinedTestDispatcher(scope.testScheduler),
        )
        val reported = mutableListOf<Int>()
        val controller = NoteCountController(async) { reported += it }
    }

    @Test fun aNewHymnReportsUnknownThenItsCount() = runTest {
        val h = Harness(this)
        h.counts = mapOf(db1 to 2)
        h.controller.onHymnChanged(HymnTypes.DB, 1)
        runCurrent()
        assertThat(h.reported).containsExactly(-1, 2).inOrder()
        assertThat(h.controller.countFor(HymnTypes.DB, 1)).isEqualTo(2)
        assertThat(h.controller.countFor(HymnTypes.DB, 2)).isEqualTo(-1)
        assertThat(h.controller.currentKey()).isEqualTo(db1)
    }

    @Test fun aNumberTheNotebookDoesNotStoreHasNoKey() = runTest {
        val h = Harness(this)
        h.controller.onHymnChanged(HymnTypes.BB, 2000)
        runCurrent()
        assertThat(h.controller.currentKey()).isNull()
        assertThat(h.reported).containsExactly(-1)
        assertThat(h.controller.countFor(HymnTypes.BB, 2000)).isEqualTo(-1)
    }

    @Test fun aLateResultForThePreviousHymnIsDropped() = runTest {
        val h = Harness(this)
        h.counts = mapOf(db1 to 5, db2 to 1)
        val slow = CompletableDeferred<Unit>()
        h.gate = slow
        h.controller.onHymnChanged(HymnTypes.DB, 1)
        runCurrent()
        h.gate = null
        h.controller.onHymnChanged(HymnTypes.DB, 2)
        runCurrent()
        slow.complete(Unit)
        runCurrent()
        assertThat(h.reported).doesNotContain(5)
        assertThat(h.reported.last()).isEqualTo(1)
        assertThat(h.controller.countFor(HymnTypes.DB, 2)).isEqualTo(1)
    }

    @Test fun refreshReportsTheNewCount() = runTest {
        val h = Harness(this)
        h.counts = mapOf(db1 to 1)
        h.controller.onHymnChanged(HymnTypes.DB, 1)
        runCurrent()
        h.counts = mapOf(db1 to 2)
        h.controller.refresh()
        runCurrent()
        assertThat(h.reported).containsExactly(-1, 1, 2).inOrder()
    }

    @Test fun nothingIsReportedAfterDestroy() = runTest {
        val h = Harness(this)
        val slow = CompletableDeferred<Unit>()
        h.gate = slow
        h.controller.onHymnChanged(HymnTypes.DB, 1)
        h.controller.destroy()
        slow.complete(Unit)
        runCurrent()
        h.controller.onHymnChanged(HymnTypes.DB, 2)
        runCurrent()
        assertThat(h.reported).containsExactly(-1)
    }

    @Test fun aFailedCountLeavesItUnknown() = runTest {
        val h = Harness(this)
        h.fail = true
        h.controller.onHymnChanged(HymnTypes.DB, 1)
        runCurrent()
        assertThat(h.reported).containsExactly(-1)
        assertThat(h.controller.countFor(HymnTypes.DB, 1)).isEqualTo(-1)
    }

    /** A query whose cancel() does nothing: every callback can still arrive, so only the controller's own guards drop stale ones. */
    private class UncancellableQuery : NoteCountController.Query {
        val pending = mutableListOf<Pair<HymnKey, NotebookCallback<Int>>>()
        var cancels = 0

        override fun start(key: HymnKey, callback: NotebookCallback<Int>): Cancellable {
            pending += key to callback
            return Cancellable { cancels++ }
        }

        fun deliver(index: Int, count: Int) = pending[index].second.onResult(Outcome.Ok(count))
    }

    @Test fun aResultThatIgnoredCancellationForThePreviousHymnIsStillDropped() {
        val query = UncancellableQuery()
        val reported = mutableListOf<Int>()
        val controller = NoteCountController(query) { reported += it }
        controller.onHymnChanged(HymnTypes.DB, 1)
        controller.onHymnChanged(HymnTypes.DB, 2)
        assertThat(query.cancels).isAtLeast(1)
        query.deliver(1, 1)
        query.deliver(0, 5)
        assertThat(reported).containsExactly(-1, -1, 1).inOrder()
        assertThat(controller.countFor(HymnTypes.DB, 2)).isEqualTo(1)
        assertThat(controller.countFor(HymnTypes.DB, 1)).isEqualTo(-1)
    }

    @Test fun aResultThatIgnoredCancellationAfterDestroyReportsNothing() {
        val query = UncancellableQuery()
        val reported = mutableListOf<Int>()
        val controller = NoteCountController(query) { reported += it }
        controller.onHymnChanged(HymnTypes.DB, 1)
        controller.destroy()
        query.deliver(0, 3)
        assertThat(reported).containsExactly(-1)
        assertThat(controller.countFor(HymnTypes.DB, 1)).isEqualTo(-1)
    }
}
