package org.cog.hymnchtv.notebook.repo

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.cog.hymnchtv.persistance.room.HymnchtvDatabase
import org.cog.hymnchtv.notebook.data.SingStats
import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.cog.hymnchtv.notebook.model.NotebookValidation
import org.cog.hymnchtv.notebook.model.Occasion
import org.cog.hymnchtv.notebook.model.SingSource
import org.cog.hymnchtv.notebook.repo.room.RoomSingLogRepository
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomSingLogRepositoryTest {
    private lateinit var db: HymnchtvDatabase
    private lateinit var repo: RoomSingLogRepository
    private val clock = TestClock(1_000)
    private val key = HymnKey.of(HymnTypes.DB, 1)
    private val hour = 3_600_000L

    @Before
    fun setUp() {
        db = HymnchtvDatabase.inMemory(ApplicationProvider.getApplicationContext())
        repo = RoomSingLogRepository(db, clock, TestIds(), testDevice)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun recordStampsAndFeedsStats(): Unit = runBlocking {
        val a = repo.record(key, sungAt = 100, occasion = Occasion.LORDS_DAY, source = SingSource.MANUAL)
        assertThat(a.id).isEqualTo(androidTestUuid(1))
        assertThat(a.createdAt).isEqualTo(1_000L)
        assertThat(a.updatedBy).isEqualTo(ANDROID_TEST_DEVICE)
        repo.record(key, 300, Occasion.HOME, SingSource.AUTO, playlistId = androidTestUuid(50))

        assertThat(repo.statsFor(key)).isEqualTo(SingStats(2, 300L))
        assertThat(repo.latestFor(key)?.sungAt).isEqualTo(300L)
        assertThat(repo.findByHymn(key).map { it.sungAt }).containsExactly(300L, 100L).inOrder()
    }

    @Test
    fun manualRecordBypassesDedupeButAutoDoesNot(): Unit = runBlocking {
        repo.record(key, 10 * hour, Occasion.HOME, SingSource.MANUAL)
        assertThat(repo.record(key, 10 * hour + 1, Occasion.HOME, SingSource.MANUAL)).isNotNull()
        assertThat(repo.recordUnlessDuplicate(key, 12 * hour, Occasion.HOME, SingSource.AUTO, 3 * hour)).isNull()
        assertThat(repo.recordUnlessDuplicate(key, 13 * hour + 1, Occasion.HOME, SingSource.AUTO, 3 * hour)).isNotNull()
        assertThat(repo.statsFor(key).singCount).isEqualTo(3)
    }

    @Test
    fun deletedLogDoesNotBlockAutoRecord(): Unit = runBlocking {
        val a = repo.record(key, 10 * hour, Occasion.HOME, SingSource.AUTO)
        repo.delete(a.id)
        assertThat(repo.recordUnlessDuplicate(key, 10 * hour, Occasion.HOME, SingSource.AUTO, 3 * hour)).isNotNull()
    }

    @Test
    fun createRejectsNonUuidIds(): Unit = runBlocking {
        val bad = SingLogEntity(
            id = "log-1", hymn = key, sungAt = 1, occasion = Occasion.HOME, source = SingSource.AUTO,
            createdAt = 0, updatedAt = 0,
        )
        assertThat(runCatching { repo.create(bad) }.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(repo.create(bad.copy(id = androidTestUuid(7))).id).isEqualTo(androidTestUuid(7))
    }

    @Test
    fun updateKeepsCreatedAtAndBumpsUpdatedAt(): Unit = runBlocking {
        val a = repo.record(key, 100, Occasion.HOME, SingSource.AUTO)
        clock.now = 2_000
        val updated = repo.update(a.copy(occasion = Occasion.SMALL_GROUP, createdAt = 0, updatedAt = 0))
        assertThat(updated).isEqualTo(a.copy(occasion = Occasion.SMALL_GROUP, updatedAt = 2_000))
        assertThat(repo.findById(a.id)).isEqualTo(updated)
    }

    @Test
    fun deletedLogsAreHiddenAndNotUpdatable(): Unit = runBlocking {
        val a = repo.record(key, 100, Occasion.HOME, SingSource.AUTO)
        clock.now = 2_000
        assertThat(repo.delete(a.id)).isTrue()
        assertThat(repo.findById(a.id)).isNull()
        assertThat(repo.update(a)).isNull()
        assertThat(repo.statsFor(key)).isEqualTo(SingStats(0, null))
        assertThat(db.singLogDao().findById(a.id)?.deletedAt).isEqualTo(2_000L)
    }

    @Test
    fun findBetweenValidatesRange(): Unit = runBlocking {
        repo.record(key, 100, Occasion.HOME, SingSource.AUTO)
        repo.record(key, 200, Occasion.HOME, SingSource.AUTO)
        assertThat(repo.findBetween(100, 200).map { it.sungAt }).containsExactly(100L)
        assertThat(runCatching { repo.findBetween(5, 1) }.exceptionOrNull())
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun sungAtOutsideTheAllowedRangeIsRejected(): Unit = runBlocking {
        val day = NotebookValidation.MAX_FUTURE_SKEW_MILLIS
        listOf(-1L, 1_000L + day + 1, Long.MAX_VALUE).forEach { bad ->
            assertThat(runCatching { repo.record(key, bad, Occasion.HOME, SingSource.MANUAL) }.exceptionOrNull())
                .isInstanceOf(IllegalArgumentException::class.java)
            assertThat(runCatching { repo.recordUnlessDuplicate(key, bad, Occasion.HOME, SingSource.AUTO, 3 * hour) }.exceptionOrNull())
                .isInstanceOf(IllegalArgumentException::class.java)
        }
        assertThat(repo.findAll()).isEmpty()
    }
}
