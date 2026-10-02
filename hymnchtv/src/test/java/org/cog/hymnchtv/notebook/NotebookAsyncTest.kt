package org.cog.hymnchtv.notebook

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.cog.hymnchtv.notebook.backup.BackupIo
import org.cog.hymnchtv.notebook.backup.ExportResult
import org.cog.hymnchtv.notebook.backup.ImportResult
import org.cog.hymnchtv.notebook.data.SingStats
import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
import org.cog.hymnchtv.notebook.fakes.FakeNotebookPrefs
import org.cog.hymnchtv.notebook.fakes.InMemoryFavoriteRepository
import org.cog.hymnchtv.notebook.fakes.InMemorySingLogRepository
import org.cog.hymnchtv.notebook.model.Clock
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.cog.hymnchtv.notebook.model.Occasion
import org.cog.hymnchtv.notebook.model.SingSource
import org.cog.hymnchtv.notebook.record.SingTracker
import org.cog.hymnchtv.notebook.repo.FavoriteRepository
import org.junit.Test
import java.util.TimeZone
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@OptIn(ExperimentalCoroutinesApi::class)
class NotebookAsyncTest {
    private val key = HymnKey.of(HymnTypes.DB, 1)

    private object UnusedBackupIo : BackupIo {
        override suspend fun exportTo(uri: Uri): ExportResult = throw UnsupportedOperationException()
        override suspend fun importFrom(uri: Uri): ImportResult = throw UnsupportedOperationException()
    }

    private class Harness(scope: TestScope) {
        val clock = Clock { 1_790_733_600_000L + scope.testScheduler.currentTime }
        val favorites = InMemoryFavoriteRepository(clock)
        val singLogs = InMemorySingLogRepository(clock)
        val prefs = FakeNotebookPrefs()
        val tracker = SingTracker(singLogs, prefs, clock, scope.backgroundScope, zone = { TimeZone.getTimeZone("UTC") })
        val async = NotebookAsync(
            favorites, singLogs, prefs, tracker, UnusedBackupIo,
            callbackDispatcher = UnconfinedTestDispatcher(scope.testScheduler),
            workDispatcher = UnconfinedTestDispatcher(scope.testScheduler),
        )
    }

    @Test
    fun toggleFavoriteDeliversTheNewState() = runTest {
        val h = Harness(this)
        val outcomes = mutableListOf<Outcome<Boolean>>()
        h.async.toggleFavorite(key) { outcomes += it }
        h.async.toggleFavorite(key) { outcomes += it }
        h.async.isFavorite(key) { outcomes += it }
        runCurrent()
        assertThat(outcomes).containsExactly(Outcome.Ok(true), Outcome.Ok(false), Outcome.Ok(false)).inOrder()
    }

    @Test
    fun recordManualBypassesDedupeAndRemembersTheOccasion() = runTest {
        val h = Harness(this)
        val recorded = mutableListOf<Outcome<SingLogEntity>>()
        h.async.recordManual(key, Occasion.SMALL_GROUP, 5_000) { recorded += it }
        h.async.recordManual(key, Occasion.SMALL_GROUP, 5_001) { recorded += it }
        runCurrent()
        assertThat(recorded.map { it.getOrNull()?.source }).containsExactly(SingSource.MANUAL, SingSource.MANUAL)
        assertThat(h.prefs.lastChosenOccasion).isEqualTo(Occasion.SMALL_GROUP)

        var stats: Outcome<SingStats>? = null
        h.async.singStats(key) { stats = it }
        runCurrent()
        assertThat(stats).isEqualTo(Outcome.Ok(SingStats(2, 5_001L)))
    }

    @Test
    fun recordManualRejectsInvalidTimes() = runTest {
        val h = Harness(this)
        var outcome: Outcome<SingLogEntity>? = null
        h.async.recordManual(key, Occasion.HOME, -1) { outcome = it }
        runCurrent()
        assertThat(outcome?.errorOrNull()).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(h.prefs.lastChosenOccasion).isNull()
        assertThat(h.singLogs.rows).isEmpty()
    }

    @Test
    fun updateSingLogRemembersTheCorrectedOccasion() = runTest {
        val h = Harness(this)
        val log = h.singLogs.record(key, 1_000, Occasion.HOME, SingSource.AUTO)
        var updated: Outcome<SingLogEntity?>? = null
        h.async.updateSingLog(log.copy(occasion = Occasion.PRAYER_MEETING)) { updated = it }
        runCurrent()
        assertThat(updated?.getOrNull()?.occasion).isEqualTo(Occasion.PRAYER_MEETING)
        assertThat(h.prefs.lastChosenOccasion).isEqualTo(Occasion.PRAYER_MEETING)
    }

    @Test
    fun failuresBecomeErr() = runTest {
        val h = Harness(this)
        h.favorites.failNext = true
        var outcome: Outcome<Boolean>? = null
        h.async.isFavorite(key) { outcome = it }
        runCurrent()
        assertThat(outcome?.errorOrNull()).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun cancelledCallsNeverCallBack() = runTest {
        val h = Harness(this)
        val gate = CompletableDeferred<Unit>().also { h.favorites.gate = it }
        val outcomes = mutableListOf<Outcome<Boolean>>()
        val call = h.async.isFavorite(key) { outcomes += it }
        call.cancel()
        gate.complete(Unit)
        runCurrent()
        assertThat(outcomes).isEmpty()
    }

    @Test
    fun trackerPassThroughIgnoresInvalidHymns() = runTest {
        val h = Harness(this)
        h.async.onMediaCompleted("bogus", 1)
        h.async.onMediaCompleted(HymnTypes.BB, 2000)
        h.async.onMediaCompleted(HymnTypes.BB, 50)
        h.async.onMediaCompleted(null, 1)
        runCurrent()
        assertThat(h.singLogs.rows).isEmpty()
        h.async.onMediaCompleted(HymnTypes.DB, 1)
        runCurrent()
        assertThat(h.singLogs.rows).hasSize(1)
    }

    @Test
    fun observeAutoRecordedDeliversUntilCancelled() = runTest {
        val h = Harness(this)
        val received = mutableListOf<Outcome<SingLogEntity>>()
        val subscription = h.async.observeAutoRecorded { received += it }
        h.async.onMediaCompleted(HymnTypes.DB, 1)
        runCurrent()
        assertThat(received).hasSize(1)

        subscription.cancel()
        h.async.onMediaCompleted(HymnTypes.DB, 2)
        runCurrent()
        assertThat(received).hasSize(1)
    }

    @Test
    fun autoRecordSwitchReadsAndWritesPrefs() = runTest {
        val h = Harness(this)
        assertThat(h.async.isAutoRecordEnabled()).isTrue()
        h.async.setAutoRecordEnabled(false)
        assertThat(h.prefs.autoRecordEnabled).isFalse()
    }

    private class HookedFavorites(private val inner: InMemoryFavoriteRepository, private val hook: () -> Unit) :
        FavoriteRepository by inner {
        override suspend fun isFavorite(key: HymnKey): Boolean {
            hook()
            return inner.isFavorite(key)
        }
    }

    @Test
    fun outOfMemoryBecomesErrInsteadOfCrashing() = runTest {
        val h = Harness(this)
        val async = NotebookAsync(
            HookedFavorites(h.favorites) { throw OutOfMemoryError("simulated") }, h.singLogs, h.prefs, h.tracker, UnusedBackupIo,
            callbackDispatcher = UnconfinedTestDispatcher(testScheduler),
            workDispatcher = UnconfinedTestDispatcher(testScheduler),
        )
        var outcome: Outcome<Boolean>? = null
        async.isFavorite(key) { outcome = it }
        runCurrent()
        assertThat(outcome?.errorOrNull()).isInstanceOf(OutOfMemoryError::class.java)
    }

    @Test
    fun workRunsOnTheWorkThreadAndCallbacksOnTheCallbackThread() {
        val workThread = Executors.newSingleThreadExecutor { Thread(it, "notebook-work-test") }
        val callbackThread = Executors.newSingleThreadExecutor { Thread(it, "notebook-callback-test") }
        val scope = CoroutineScope(Job())
        try {
            val clock = Clock { 1_790_733_600_000L }
            val singLogs = InMemorySingLogRepository(clock)
            val prefs = FakeNotebookPrefs()
            val workName = AtomicReference<String>()
            val favorites = HookedFavorites(InMemoryFavoriteRepository(clock)) { workName.set(Thread.currentThread().name) }
            val async = NotebookAsync(
                favorites, singLogs, prefs, SingTracker(singLogs, prefs, clock, scope), UnusedBackupIo,
                callbackDispatcher = callbackThread.asCoroutineDispatcher(),
                workDispatcher = workThread.asCoroutineDispatcher(),
            )
            val callbackName = AtomicReference<String>()
            val done = CountDownLatch(1)
            async.isFavorite(key) {
                callbackName.set(Thread.currentThread().name)
                done.countDown()
            }
            assertThat(done.await(5, TimeUnit.SECONDS)).isTrue()
            // the coroutine debug agent appends " @coroutine#N" to the thread name
            assertThat(workName.get()).startsWith("notebook-work-test")
            assertThat(callbackName.get()).startsWith("notebook-callback-test")
        } finally {
            scope.cancel()
            workThread.shutdownNow()
            callbackThread.shutdownNow()
        }
    }
}
