package org.cog.hymnchtv.notebook.fakes

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.cog.hymnchtv.notebook.data.SingStats
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.cog.hymnchtv.notebook.model.Occasion
import org.cog.hymnchtv.notebook.model.SingSource
import org.junit.Test

class InMemorySingLogRepositoryTest {
    private val key = HymnKey.of(HymnTypes.DB, 1)
    private val hour = 3_600_000L

    @Test
    fun recordStampsAndSoftDeletes() = runTest {
        val clock = MutableClock(1_000)
        val repo = InMemorySingLogRepository(clock)
        val a = repo.record(key, sungAt = 500, occasion = Occasion.HOME, source = SingSource.AUTO)
        assertThat(a.id).isEqualTo(testUuid(1))
        assertThat(a.createdAt).isEqualTo(1_000L)
        assertThat(a.updatedBy).isEqualTo(TEST_DEVICE)

        clock.now = 2_000
        repo.record(key, sungAt = 900, occasion = Occasion.HOME, source = SingSource.MANUAL)
        assertThat(repo.statsFor(key)).isEqualTo(SingStats(2, 900L))

        assertThat(repo.delete(a.id)).isTrue()
        assertThat(repo.delete(a.id)).isFalse()
        assertThat(repo.findById(a.id)).isNull()
        assertThat(repo.rows.getValue(a.id).deletedAt).isEqualTo(2_000L)
        assertThat(repo.statsFor(key)).isEqualTo(SingStats(1, 900L))
    }

    @Test
    fun recordUnlessDuplicateHonoursTheWindow() = runTest {
        val repo = InMemorySingLogRepository(MutableClock(0))
        assertThat(repo.recordUnlessDuplicate(key, 10 * hour, Occasion.HOME, SingSource.AUTO, 3 * hour)).isNotNull()
        assertThat(repo.recordUnlessDuplicate(key, 13 * hour - 1, Occasion.HOME, SingSource.AUTO, 3 * hour)).isNull()
        assertThat(repo.recordUnlessDuplicate(key, 13 * hour, Occasion.HOME, SingSource.AUTO, 3 * hour)).isNotNull()
    }

    @Test
    fun recordUnlessDuplicateIsAtomicUnderConcurrency() = runTest {
        val repo = InMemorySingLogRepository(MutableClock(0))
        val results = withContext(Dispatchers.Default) {
            (1..20).map { i ->
                async { repo.recordUnlessDuplicate(key, 10 * hour + i, Occasion.HOME, SingSource.AUTO, 3 * hour) }
            }.awaitAll()
        }
        assertThat(results.count { it != null }).isEqualTo(1)
        assertThat(repo.rows).hasSize(1)
    }

    @Test
    fun failNextThrowsOnce() = runTest {
        val repo = InMemorySingLogRepository(MutableClock(0))
        repo.failNext = true
        assertThat(runCatching { repo.latestFor(key) }.isFailure).isTrue()
        assertThat(repo.latestFor(key)).isNull()
    }
}
