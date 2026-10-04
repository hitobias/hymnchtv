package org.cog.hymnchtv.ui.lyrics

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.cog.hymnchtv.notebook.NotebookAsync
import org.cog.hymnchtv.notebook.backup.BackupIo
import org.cog.hymnchtv.notebook.backup.ExportResult
import org.cog.hymnchtv.notebook.backup.ImportResult
import org.cog.hymnchtv.notebook.data.entity.FavoriteEntity
import org.cog.hymnchtv.notebook.fakes.FakeNotebookPrefs
import org.cog.hymnchtv.notebook.fakes.InMemoryFavoriteRepository
import org.cog.hymnchtv.notebook.fakes.InMemorySingLogRepository
import org.cog.hymnchtv.notebook.model.Clock
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.cog.hymnchtv.notebook.record.SingTracker
import org.cog.hymnchtv.notebook.repo.FavoriteRepository
import org.junit.Test
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class FavoriteControllerTest {
    private val k1 = HymnKey.of(HymnTypes.DB, 1)
    private val k2 = HymnKey.of(HymnTypes.DB, 2)

    private object UnusedBackupIo : BackupIo {
        override suspend fun exportTo(uri: Uri): ExportResult = throw UnsupportedOperationException()
        override suspend fun importFrom(uri: Uri): ImportResult = throw UnsupportedOperationException()
    }

    /** isFavorite / setFavorite of a key suspend on that key's own gate (when one is installed). */
    private class GatedRepo(private val d: InMemoryFavoriteRepository) : FavoriteRepository by d {
        val queryGates = mutableMapOf<HymnKey, CompletableDeferred<Unit>>()
        val toggleGates = mutableMapOf<HymnKey, CompletableDeferred<Unit>>()
        var toggleCalls = 0

        override suspend fun isFavorite(key: HymnKey): Boolean {
            queryGates[key]?.await()
            return d.isFavorite(key)
        }

        override suspend fun setFavorite(key: HymnKey, favorite: Boolean): FavoriteEntity? {
            toggleCalls++
            toggleGates[key]?.await()
            return d.setFavorite(key, favorite)
        }
    }

    private class Recorder : FavoriteController.Listener {
        val states = mutableListOf<Pair<Boolean, Boolean>>()
        val toggled = mutableListOf<Boolean>()
        var errors = 0
        override fun onState(marked: Boolean, canToggle: Boolean) { states += marked to canToggle }
        override fun onToggled(marked: Boolean) { toggled += marked }
        override fun onError() { errors++ }
    }

    private class Harness(scope: TestScope) {
        val clock = Clock { 1_790_733_600_000L + scope.testScheduler.currentTime }
        val mem = InMemoryFavoriteRepository(clock)
        val repo = GatedRepo(mem)
        private val singLogs = InMemorySingLogRepository(clock)
        private val prefs = FakeNotebookPrefs()
        private val tracker = SingTracker(singLogs, prefs, clock, scope.backgroundScope, zone = { TimeZone.getTimeZone("UTC") })
        val async = NotebookAsync(
            repo, singLogs, prefs, tracker, UnusedBackupIo,
            callbackDispatcher = UnconfinedTestDispatcher(scope.testScheduler),
            workDispatcher = UnconfinedTestDispatcher(scope.testScheduler),
        )
        val listener = Recorder()
        val controller = FavoriteController(async, listener)
    }

    @Test
    fun onHymnChangedQueriesAndReportsMarked() = runTest {
        val h = Harness(this)
        h.mem.setFavorite(k1, true)
        h.controller.onHymnChanged(HymnTypes.DB, 1)
        runCurrent()
        assertThat(h.listener.states.last()).isEqualTo(true to true)
        assertThat(h.controller.isMarked(HymnTypes.DB, 1)).isTrue()
        assertThat(h.controller.isMarked(HymnTypes.DB, 2)).isFalse()
    }

    @Test
    fun staleQueryResultIsDropped() = runTest {
        val h = Harness(this)
        h.mem.setFavorite(k1, true)
        val g1 = CompletableDeferred<Unit>().also { h.repo.queryGates[k1] = it }
        h.controller.onHymnChanged(HymnTypes.DB, 1)
        h.controller.onHymnChanged(HymnTypes.DB, 2)
        runCurrent()
        g1.complete(Unit)
        runCurrent()
        assertThat(h.listener.states.last()).isEqualTo(false to true)
        assertThat(h.listener.states.none { it.first }).isTrue()
    }

    @Test
    fun toggleFlipsAndNotifiesOnce() = runTest {
        val h = Harness(this)
        h.controller.onHymnChanged(HymnTypes.DB, 1)
        runCurrent()
        h.controller.toggle()
        runCurrent()
        assertThat(h.listener.toggled).containsExactly(true)
        assertThat(h.controller.isMarked(HymnTypes.DB, 1)).isTrue()
        h.controller.toggle()
        runCurrent()
        assertThat(h.listener.toggled).containsExactly(true, false).inOrder()
    }

    @Test
    fun toggleIgnoredWhileInFlightForSameKey() = runTest {
        val h = Harness(this)
        val gate = CompletableDeferred<Unit>().also { h.repo.toggleGates[k1] = it }
        h.controller.onHymnChanged(HymnTypes.DB, 1)
        runCurrent()
        h.controller.toggle()
        h.controller.toggle()
        runCurrent()
        gate.complete(Unit)
        runCurrent()
        assertThat(h.repo.toggleCalls).isEqualTo(1)
        assertThat(h.listener.toggled).containsExactly(true)
    }

    @Test
    fun toggleResultForALeftHymnIsDroppedButWriteHappens() = runTest {
        val h = Harness(this)
        val gate = CompletableDeferred<Unit>().also { h.repo.toggleGates[k1] = it }
        h.controller.onHymnChanged(HymnTypes.DB, 1)
        runCurrent()
        h.controller.toggle()
        h.controller.onHymnChanged(HymnTypes.DB, 2)
        runCurrent()
        gate.complete(Unit)
        runCurrent()
        assertThat(h.mem.isFavorite(k1)).isTrue()
        assertThat(h.listener.toggled).isEmpty()
        assertThat(h.controller.isMarked(HymnTypes.DB, 1)).isFalse()
        // the in-flight marker was cleared: the old hymn can be toggled again after coming back
        h.controller.onHymnChanged(HymnTypes.DB, 1)
        h.controller.toggle()
        runCurrent()
        assertThat(h.repo.toggleCalls).isEqualTo(2)
    }

    @Test
    fun invalidKeyDisablesToggle() = runTest {
        val h = Harness(this)
        h.controller.onHymnChanged(HymnTypes.BB, 2000)
        h.controller.toggle()
        runCurrent()
        assertThat(h.listener.states.last()).isEqualTo(false to false)
        assertThat(h.controller.canToggle()).isFalse()
        assertThat(h.repo.toggleCalls).isEqualTo(0)
    }

    @Test
    fun destroyCancelsPendingCallbacks() = runTest {
        val h = Harness(this)
        val q = CompletableDeferred<Unit>().also { h.repo.queryGates[k1] = it }
        val t = CompletableDeferred<Unit>().also { h.repo.toggleGates[k2] = it }
        h.controller.onHymnChanged(HymnTypes.DB, 2)
        runCurrent()
        h.controller.toggle()
        h.controller.onHymnChanged(HymnTypes.DB, 1)
        val before = h.listener.states.size
        h.controller.destroy()
        q.complete(Unit)
        t.complete(Unit)
        runCurrent()
        assertThat(h.listener.states.size).isEqualTo(before)
        assertThat(h.listener.toggled).isEmpty()
        assertThat(h.listener.errors).isEqualTo(0)
        // the write itself is not cancelled by destroy
        assertThat(h.mem.isFavorite(k2)).isTrue()
    }

    @Test
    fun failureKeepsStateAndReportsError() = runTest {
        val h = Harness(this)
        h.controller.onHymnChanged(HymnTypes.DB, 1)
        runCurrent()
        h.mem.failNext = true
        h.controller.toggle()
        runCurrent()
        assertThat(h.listener.errors).isEqualTo(1)
        assertThat(h.listener.toggled).isEmpty()
        // the state is re-queried after the failure, so the menu stays truthful
        assertThat(h.controller.canToggle()).isTrue()
        assertThat(h.controller.isMarked(HymnTypes.DB, 1)).isFalse()
        // marker cleared: retry works
        h.controller.toggle()
        runCurrent()
        assertThat(h.listener.toggled).containsExactly(true)
    }

    @Test
    fun toggleDisabledUntilStateKnown() = runTest {
        val h = Harness(this)
        val g = CompletableDeferred<Unit>().also { h.repo.queryGates[k1] = it }
        h.controller.onHymnChanged(HymnTypes.DB, 1)
        runCurrent()
        assertThat(h.controller.canToggle()).isFalse()
        assertThat(h.listener.states.last()).isEqualTo(false to false)
        h.controller.toggle()
        runCurrent()
        assertThat(h.repo.toggleCalls).isEqualTo(0)
        g.complete(Unit)
        runCurrent()
        assertThat(h.controller.canToggle()).isTrue()
        assertThat(h.listener.states.last()).isEqualTo(false to true)
    }

    @Test
    fun toggleSetsExactlyWhatTheMenuSays() = runTest {
        val h = Harness(this)
        h.mem.setFavorite(k1, true)
        h.controller.onHymnChanged(HymnTypes.DB, 1)
        runCurrent()
        // the menu says "remove": the write removes, even if the row was meanwhile re-added elsewhere
        h.controller.toggle()
        runCurrent()
        assertThat(h.mem.isFavorite(k1)).isFalse()
        assertThat(h.listener.toggled).containsExactly(false)
        assertThat(h.controller.isMarked(HymnTypes.DB, 1)).isFalse()
    }

    @Test
    fun queryStartedDuringToggleDoesNotOverrideResult() = runTest {
        val h = Harness(this)
        h.controller.onHymnChanged(HymnTypes.DB, 1)
        runCurrent()
        val toggleGate = CompletableDeferred<Unit>().also { h.repo.toggleGates[k1] = it }
        val queryGate = CompletableDeferred<Unit>().also { h.repo.queryGates[k1] = it }
        h.controller.toggle()
        h.controller.onHymnChanged(HymnTypes.DB, 2)
        h.controller.onHymnChanged(HymnTypes.DB, 1)
        runCurrent()
        toggleGate.complete(Unit)
        runCurrent()
        assertThat(h.controller.isMarked(HymnTypes.DB, 1)).isTrue()
        queryGate.complete(Unit)
        runCurrent()
        assertThat(h.controller.isMarked(HymnTypes.DB, 1)).isTrue()
        assertThat(h.listener.toggled).containsExactly(true)
    }
}
