package org.cog.hymnchtv.notebook.repo

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.cog.hymnchtv.persistance.room.HymnchtvDatabase
import org.cog.hymnchtv.notebook.data.entity.FavoriteEntity
import org.cog.hymnchtv.notebook.model.FavoriteIds
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.cog.hymnchtv.notebook.repo.room.RoomFavoriteRepository
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomFavoriteRepositoryTest {
    private lateinit var db: HymnchtvDatabase
    private lateinit var repo: RoomFavoriteRepository
    private val clock = TestClock(1_000)
    private val key = HymnKey.of(HymnTypes.DB, 1)

    @Before
    fun setUp() {
        db = HymnchtvDatabase.inMemory(ApplicationProvider.getApplicationContext())
        repo = RoomFavoriteRepository(db, clock, testDevice)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun toggleFlipsStateAndKeepsOneSoftDeletedRow(): Unit = runBlocking {
        assertThat(repo.toggle(key)).isTrue()
        assertThat(repo.isFavorite(key)).isTrue()

        clock.now = 2_000
        assertThat(repo.toggle(key)).isFalse()
        assertThat(repo.isFavorite(key)).isFalse()
        assertThat(repo.findAll()).isEmpty()
        val stored = db.favoriteDao().findAllIncludingDeleted().single()
        assertThat(stored.id).isEqualTo(FavoriteIds.forKey(key))
        assertThat(stored.createdAt).isEqualTo(1_000L)
        assertThat(stored.updatedAt).isEqualTo(2_000L)
        assertThat(stored.deletedAt).isEqualTo(2_000L)
        assertThat(stored.updatedBy).isEqualTo(ANDROID_TEST_DEVICE)

        clock.now = 3_000
        assertThat(repo.toggle(key)).isTrue()
        val revived = db.favoriteDao().findAllIncludingDeleted().single()
        assertThat(revived.deletedAt).isNull()
        assertThat(revived.createdAt).isEqualTo(1_000L)
        assertThat(revived.updatedAt).isEqualTo(3_000L)
    }

    @Test
    fun setFavoriteIsIdempotent(): Unit = runBlocking {
        val first = repo.setFavorite(key, true)
        clock.now = 5_000
        assertThat(repo.setFavorite(key, true)).isEqualTo(first)
        assertThat(repo.setFavorite(HymnKey.of(HymnTypes.BB, 3), false)).isNull()
        assertThat(db.favoriteDao().findAllIncludingDeleted()).hasSize(1)
    }

    @Test
    fun createIgnoresTheGivenIdAndDeleteIsSoft(): Unit = runBlocking {
        val created = repo.create(FavoriteEntity(id = androidTestUuid(99), hymn = key, createdAt = 0, updatedAt = 0))
        assertThat(created.id).isEqualTo(FavoriteIds.forKey(key))
        assertThat(repo.findById(created.id)).isEqualTo(created)
        assertThat(repo.delete(created.id)).isTrue()
        assertThat(repo.delete(created.id)).isFalse()
        assertThat(repo.findById(created.id)).isNull()
        assertThat(repo.delete(androidTestUuid(1))).isFalse()
    }
}
