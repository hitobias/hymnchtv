package org.cog.hymnchtv.notebook.record

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
import org.cog.hymnchtv.notebook.fakes.FakeNotebookPrefs
import org.cog.hymnchtv.notebook.fakes.InMemorySingLogRepository
import org.cog.hymnchtv.notebook.model.Clock
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.cog.hymnchtv.notebook.model.Occasion
import org.cog.hymnchtv.notebook.model.SingSource
import org.junit.Test
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class SingTrackerTest {
    private val key = HymnKey.of(HymnTypes.DB, 1)
    private val other = HymnKey.of(HymnTypes.DB, 2)
    private val threshold = AutoRecordConfig.DEFAULT_VISIBLE_THRESHOLD_MILLIS
    private val window = AutoRecordConfig.DEFAULT_DEDUPE_WINDOW_MILLIS

    private class Harness(scope: TestScope, prefs: FakeNotebookPrefs, base: Long = BASE, zone: () -> TimeZone = { UTC }) {
        val clock = Clock { base + scope.testScheduler.currentTime }
        val repo = InMemorySingLogRepository(clock)
        val tracker = SingTracker(repo, prefs, clock, scope.backgroundScope, zone = zone)
    }

    private fun TestScope.harness(prefs: FakeNotebookPrefs = FakeNotebookPrefs()) = Harness(this, prefs)

    private fun TestScope.advance(millis: Long) {
        advanceTimeBy(millis)
        runCurrent()
    }

    @Test
    fun visibleForThresholdRecordsOnce() = runTest {
        val h = harness()
        h.tracker.onHymnVisible(key)
        advance(threshold - 1)
        assertThat(h.repo.rows).isEmpty()
        advance(1)
        val log = h.repo.rows.values.single()
        assertThat(log.hymn).isEqualTo(key)
        assertThat(log.source).isEqualTo(SingSource.AUTO)
        assertThat(log.sungAt).isEqualTo(BASE + threshold)
        advance(threshold * 5)
        assertThat(h.repo.rows).hasSize(1)
    }

    @Test
    fun hidingRestartsTheTimer() = runTest {
        val h = harness()
        h.tracker.onHymnVisible(key)
        advance(threshold - 20_000)
        h.tracker.onHymnHidden(key)
        advance(threshold)
        assertThat(h.repo.rows).isEmpty()

        h.tracker.onHymnVisible(key)
        advance(threshold - 1)
        assertThat(h.repo.rows).isEmpty()
        advance(1)
        assertThat(h.repo.rows).hasSize(1)
    }

    @Test
    fun switchingHymnCancelsThePreviousTimer() = runTest {
        val h = harness()
        h.tracker.onHymnVisible(key)
        advance(threshold / 2)
        h.tracker.onHymnVisible(other)
        advance(threshold)
        assertThat(h.repo.rows.values.map { it.hymn }).containsExactly(other)
    }

    @Test
    fun repeatedVisibleForTheSameHymnKeepsTheTimer() = runTest {
        val h = harness()
        h.tracker.onHymnVisible(key)
        advance(threshold / 2)
        h.tracker.onHymnVisible(key)
        advance(threshold / 2)
        assertThat(h.repo.rows).hasSize(1)
    }

    @Test
    fun hidingAnotherHymnIsIgnored() = runTest {
        val h = harness()
        h.tracker.onHymnVisible(key)
        h.tracker.onHymnHidden(other)
        advance(threshold)
        assertThat(h.repo.rows).hasSize(1)
    }

    @Test
    fun mediaCompletedRecordsImmediately() = runTest {
        val h = harness()
        h.tracker.onMediaCompleted(key)
        runCurrent()
        assertThat(h.repo.rows.values.single().sungAt).isEqualTo(BASE)
    }

    @Test
    fun simultaneousTriggersRecordOnce() = runTest {
        val h = harness()
        h.tracker.onMediaCompleted(key)
        h.tracker.onMediaCompleted(key)
        h.tracker.onMediaCompleted(key)
        runCurrent()
        assertThat(h.repo.rows).hasSize(1)
    }

    @Test
    fun dedupeWindowAppliesAcrossTriggers() = runTest {
        val h = harness()
        h.tracker.onMediaCompleted(key)
        runCurrent()
        h.tracker.onHymnVisible(key)
        advance(threshold)
        assertThat(h.repo.rows).hasSize(1)

        h.tracker.onHymnHidden(key)
        advance(window - threshold - 1)
        h.tracker.onMediaCompleted(key)
        runCurrent()
        assertThat(h.repo.rows).hasSize(1)

        advance(1)
        h.tracker.onMediaCompleted(key)
        runCurrent()
        assertThat(h.repo.rows).hasSize(2)
    }

    @Test
    fun disabledPrefSkipsEverything() = runTest {
        val h = harness(FakeNotebookPrefs(autoRecord = false))
        h.tracker.onMediaCompleted(key)
        h.tracker.onHymnVisible(key)
        advance(threshold)
        assertThat(h.repo.rows).isEmpty()
    }

    @Test
    fun occasionComesFromInference() = runTest {
        val h = harness(FakeNotebookPrefs(lastChosen = Occasion.SMALL_GROUP))
        h.tracker.onMediaCompleted(key)
        runCurrent()
        assertThat(h.repo.rows.values.single().occasion).isEqualTo(Occasion.SMALL_GROUP)
    }

    @Test
    fun zoneIsReadAtRecordTime() = runTest {
        var zone: TimeZone = UTC
        val h = Harness(this, FakeNotebookPrefs(), base = SUNDAY_0200_UTC, zone = { zone })
        h.tracker.onMediaCompleted(key)
        runCurrent()
        zone = TAIPEI // the user flies to Taipei: Sunday 10:00 local
        h.tracker.onMediaCompleted(other)
        runCurrent()
        assertThat(h.repo.rows.values.sortedBy { it.hymn.hymnNo }.map { it.occasion })
            .containsExactly(Occasion.HOME, Occasion.LORDS_DAY).inOrder()
    }

    @Test
    fun repositoryFailureIsSwallowed() = runTest {
        val h = harness()
        h.repo.failNext = true
        h.tracker.onMediaCompleted(key)
        runCurrent()
        assertThat(h.repo.rows).isEmpty()
        h.tracker.onMediaCompleted(key)
        runCurrent()
        assertThat(h.repo.rows).hasSize(1)
    }

    @Test
    fun recordedFlowEmitsEachNewLogOnly() = runTest {
        val h = harness()
        val emitted = mutableListOf<SingLogEntity>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { h.tracker.recorded.toList(emitted) }
        h.tracker.onMediaCompleted(key)
        runCurrent()
        h.tracker.onMediaCompleted(key)
        runCurrent()
        assertThat(emitted.map { it.id }).containsExactly(h.repo.rows.keys.single())
    }

    private companion object {
        const val BASE = 1_790_733_600_000L            // Wednesday 2026-09-30 02:00 UTC
        const val SUNDAY_0200_UTC = 1_791_079_200_000L // Sunday 2026-10-04 02:00 UTC = 10:00 Taipei
        val UTC: TimeZone = TimeZone.getTimeZone("UTC")
        val TAIPEI: TimeZone = TimeZone.getTimeZone("Asia/Taipei")
    }
}
