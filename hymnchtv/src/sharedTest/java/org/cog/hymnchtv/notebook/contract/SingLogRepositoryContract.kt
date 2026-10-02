package org.cog.hymnchtv.notebook.contract

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.cog.hymnchtv.notebook.data.SingStats
import org.cog.hymnchtv.notebook.model.Clock
import org.cog.hymnchtv.notebook.model.DeviceIdProvider
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.cog.hymnchtv.notebook.model.NotebookValidation
import org.cog.hymnchtv.notebook.model.Occasion
import org.cog.hymnchtv.notebook.model.SingSource
import org.cog.hymnchtv.notebook.repo.SingLogRepository
import org.junit.After
import org.junit.Test

class ContractClock(@Volatile var now: Long) : Clock {
    override fun nowMillis(): Long = now
}

/**
 * Behaviour every SingLogRepository must share. Subclasses: InMemorySingLogRepositoryContractTest (JVM) and
 * RoomSingLogRepositoryContractTest (androidTest). Add a case here whenever the fake and Room could diverge.
 */
abstract class SingLogRepositoryContract {
    protected val clock = ContractClock(NOW)
    private val key = HymnKey.of(HymnTypes.DB, 1)
    private val repo by lazy { newRepository(clock, DeviceIdProvider { DEVICE }) }

    protected abstract fun newRepository(clock: Clock, device: DeviceIdProvider): SingLogRepository

    protected open fun tearDownRepository() {}

    @After
    fun closeRepository() = tearDownRepository()

    private fun rejects(block: suspend () -> Unit) = runBlocking {
        assertThat(runCatching { block() }.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun recordStampsIdTimesAndDevice(): Unit = runBlocking {
        val log = repo.record(key, NOW - 10, Occasion.HOME, SingSource.MANUAL)
        assertThat(NotebookValidation.uuid(log.id)).isEqualTo(log.id)
        assertThat(log.createdAt).isEqualTo(NOW)
        assertThat(log.updatedAt).isEqualTo(NOW)
        assertThat(log.deletedAt).isNull()
        assertThat(log.updatedBy).isEqualTo(DEVICE)
        assertThat(repo.findById(log.id)).isEqualTo(log)
    }

    @Test
    fun dedupeWindowIsExclusiveOnBothSides(): Unit = runBlocking {
        repo.record(key, NOW, Occasion.HOME, SingSource.MANUAL)
        assertThat(repo.recordUnlessDuplicate(key, NOW - WINDOW + 1, Occasion.HOME, SingSource.AUTO, WINDOW)).isNull()
        assertThat(repo.recordUnlessDuplicate(key, NOW + WINDOW - 1, Occasion.HOME, SingSource.AUTO, WINDOW)).isNull()
        assertThat(repo.recordUnlessDuplicate(key, NOW - WINDOW, Occasion.HOME, SingSource.AUTO, WINDOW)).isNotNull()
        assertThat(repo.statsFor(key).singCount).isEqualTo(2)
    }

    @Test
    fun illegalInputIsRejectedEvenWhenItWouldBeADuplicate(): Unit = runBlocking {
        repo.record(key, NOW + DAY, Occasion.HOME, SingSource.MANUAL)
        rejects { repo.recordUnlessDuplicate(key, NOW + DAY + 1, Occasion.HOME, SingSource.AUTO, WINDOW) }
        rejects { repo.recordUnlessDuplicate(key, -1, Occasion.HOME, SingSource.AUTO, WINDOW) }
        rejects { repo.recordUnlessDuplicate(key, Long.MAX_VALUE, Occasion.HOME, SingSource.AUTO, WINDOW) }
        rejects { repo.recordUnlessDuplicate(key, NOW + DAY, Occasion.HOME, SingSource.AUTO, -1) }
        rejects { repo.record(key, -1, Occasion.HOME, SingSource.MANUAL) }
        assertThat(repo.statsFor(key).singCount).isEqualTo(1)
    }

    @Test
    fun updateValidatesTheTimeAndKeepsCreatedAt(): Unit = runBlocking {
        val log = repo.record(key, NOW, Occasion.HOME, SingSource.AUTO)
        clock.now = NOW + 1_000
        rejects { repo.update(log.copy(sungAt = -1)) }
        rejects { repo.update(log.copy(sungAt = NOW + 1_000 + DAY + 1)) }
        val updated = repo.update(log.copy(occasion = Occasion.SMALL_GROUP))
        assertThat(updated).isEqualTo(log.copy(occasion = Occasion.SMALL_GROUP, updatedAt = NOW + 1_000))
    }

    @Test
    fun softDeleteHidesTheRowAndNoLongerBlocksAutoRecord(): Unit = runBlocking {
        val log = repo.record(key, NOW, Occasion.HOME, SingSource.AUTO)
        assertThat(repo.delete(log.id)).isTrue()
        assertThat(repo.delete(log.id)).isFalse()
        assertThat(repo.findById(log.id)).isNull()
        assertThat(repo.update(log)).isNull()
        assertThat(repo.statsFor(key)).isEqualTo(SingStats(0, null))
        assertThat(repo.recordUnlessDuplicate(key, NOW, Occasion.HOME, SingSource.AUTO, WINDOW)).isNotNull()
    }

    @Test
    fun queriesReturnActiveRowsNewestFirst(): Unit = runBlocking {
        repo.record(key, NOW - 300, Occasion.HOME, SingSource.AUTO)
        repo.record(key, NOW - 100, Occasion.HOME, SingSource.AUTO)
        repo.record(HymnKey.of(HymnTypes.ER, 1), NOW - 200, Occasion.HOME, SingSource.AUTO)
        assertThat(repo.statsFor(key)).isEqualTo(SingStats(2, NOW - 100))
        assertThat(repo.findByHymn(key).map { it.sungAt }).containsExactly(NOW - 100, NOW - 300).inOrder()
        assertThat(repo.latestFor(key)?.sungAt).isEqualTo(NOW - 100)
        assertThat(repo.findBetween(NOW - 300, NOW - 100).map { it.sungAt }).containsExactly(NOW - 200, NOW - 300).inOrder()
        rejects { repo.findBetween(2, 1) }
    }

    companion object {
        const val NOW = 1_790_733_600_000L
        const val DAY = NotebookValidation.MAX_FUTURE_SKEW_MILLIS
        const val WINDOW = 3 * 3_600_000L
        const val DEVICE = "00000000-0000-0000-0000-0000000000cc"
    }
}
