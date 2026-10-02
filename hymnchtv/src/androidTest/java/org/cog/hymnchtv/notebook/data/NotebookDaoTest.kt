package org.cog.hymnchtv.notebook.data

import android.database.sqlite.SQLiteConstraintException
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.cog.hymnchtv.notebook.data.entity.FavoriteEntity
import org.cog.hymnchtv.notebook.data.entity.NoteEntity
import org.cog.hymnchtv.notebook.data.entity.PlaylistItemEntity
import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
import org.cog.hymnchtv.notebook.model.FavoriteIds
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.cog.hymnchtv.notebook.model.Occasion
import org.cog.hymnchtv.notebook.model.SingSource
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotebookDaoTest {
    private lateinit var db: NotebookDatabase
    private val db1 = HymnKey.of(HymnTypes.DB, 1)
    private val fu1 = HymnKey.of(HymnTypes.DB, 781)
    private val er1 = HymnKey.of(HymnTypes.ER, 1)

    @Before
    fun setUp() {
        db = NotebookDatabase.inMemory(ApplicationProvider.getApplicationContext())
    }

    @After
    fun tearDown() = db.close()

    private fun fav(key: HymnKey, deletedAt: Long? = null) =
        FavoriteEntity(FavoriteIds.forKey(key), key, createdAt = 10, updatedAt = 10, deletedAt = deletedAt)

    private fun log(id: String, key: HymnKey, sungAt: Long, deletedAt: Long? = null) = SingLogEntity(
        id = id, hymn = key, sungAt = sungAt, occasion = Occasion.HOME, source = SingSource.AUTO,
        createdAt = 1, updatedAt = 1, deletedAt = deletedAt,
    )

    private fun item(id: String, playlistId: String, position: Int, key: HymnKey = db1, deletedAt: Long? = null) =
        PlaylistItemEntity(id, playlistId, position, key, createdAt = 1, updatedAt = 1, deletedAt = deletedAt)

    @Test
    fun favoriteActiveVersusDeleted(): Unit = runBlocking {
        val dao = db.favoriteDao()
        dao.upsert(fav(db1))
        dao.upsert(fav(fu1, deletedAt = 20))

        assertThat(dao.isFavorite(db1.hymnType, db1.hymnNo, db1.isFu)).isTrue()
        assertThat(dao.isFavorite(fu1.hymnType, fu1.hymnNo, fu1.isFu)).isFalse()
        assertThat(dao.isFavorite(er1.hymnType, er1.hymnNo, er1.isFu)).isFalse()
        assertThat(dao.findActive().map { it.hymn }).containsExactly(db1)
        assertThat(dao.findAllIncludingDeleted()).hasSize(2)
        assertThat(dao.findById(FavoriteIds.forKey(fu1))?.deletedAt).isEqualTo(20L)
    }

    @Test
    fun singStatsCountActiveOnly(): Unit = runBlocking {
        val dao = db.singLogDao()
        dao.upsertAll(
            listOf(log("a", db1, 100), log("b", db1, 300), log("c", db1, 500, deletedAt = 600), log("d", fu1, 900)),
        )
        assertThat(dao.statsFor(db1.hymnType, db1.hymnNo, db1.isFu)).isEqualTo(SingStats(2, 300L))
        assertThat(dao.statsFor(er1.hymnType, er1.hymnNo, er1.isFu)).isEqualTo(SingStats(0, null))
        assertThat(dao.latestForHymn(db1.hymnType, db1.hymnNo, db1.isFu)?.id).isEqualTo("b")
        assertThat(dao.findByHymn(db1.hymnType, db1.hymnNo, db1.isFu).map { it.id }).containsExactly("b", "a").inOrder()
    }

    @Test
    fun existsBetweenIsExclusiveAndIgnoresDeletedAndOtherHymns(): Unit = runBlocking {
        val dao = db.singLogDao()
        dao.upsertAll(listOf(log("a", db1, 100), log("b", db1, 150, deletedAt = 151), log("c", fu1, 160)))
        assertThat(dao.existsBetween(db1.hymnType, db1.hymnNo, db1.isFu, 99, 200)).isTrue()
        assertThat(dao.existsBetween(db1.hymnType, db1.hymnNo, db1.isFu, 100, 200)).isFalse()
        assertThat(dao.existsBetween(db1.hymnType, db1.hymnNo, db1.isFu, 0, 100)).isFalse()
        assertThat(dao.existsBetween(db1.hymnType, db1.hymnNo, db1.isFu, 120, 200)).isFalse()
    }

    @Test
    fun findBetweenIsHalfOpenAndNewestFirst(): Unit = runBlocking {
        val dao = db.singLogDao()
        dao.upsertAll(listOf(log("a", db1, 100), log("b", fu1, 200), log("c", er1, 300)))
        assertThat(dao.findBetween(100, 300).map { it.id }).containsExactly("b", "a").inOrder()
    }

    @Test
    fun enumsNullablesAndUpdatedByRoundTrip(): Unit = runBlocking {
        val dao = db.singLogDao()
        val row = log("x", db1, 1).copy(
            occasion = Occasion.PRAYER_MEETING, source = SingSource.MANUAL, playlistId = "p1", updatedBy = "dev-1",
        )
        dao.upsert(row)
        assertThat(dao.findById("x")).isEqualTo(row)
    }

    @Test
    fun notesNewestFirstPerHymn(): Unit = runBlocking {
        val dao = db.noteDao()
        dao.upsertAll(
            listOf(
                NoteEntity("n1", db1, "第一則", null, createdAt = 1, updatedAt = 1),
                NoteEntity("n2", db1, "第二則", "x", createdAt = 2, updatedAt = 2),
                NoteEntity("n3", fu1, "附歌", null, createdAt = 3, updatedAt = 3),
            ),
        )
        assertThat(dao.findByHymn(db1.hymnType, db1.hymnNo, db1.isFu).map { it.id }).containsExactly("n2", "n1").inOrder()
    }

    @Test
    fun playlistItemsOrderedAndMaxPositionIncludesDeleted(): Unit = runBlocking {
        val dao = db.playlistItemDao()
        assertThat(dao.maxPositionIncludingDeleted("p")).isNull()
        dao.upsertAll(
            listOf(item("i2", "p", 1, fu1), item("i1", "p", 0), item("i3", "p", 2, er1, deletedAt = 5), item("i4", "other", 0)),
        )
        assertThat(dao.itemsOf("p").map { it.id }).containsExactly("i1", "i2").inOrder()
        assertThat(dao.maxPositionIncludingDeleted("p")).isEqualTo(2)
    }

    @Test
    fun positionIsUniquePerPlaylist(): Unit = runBlocking {
        val dao = db.playlistItemDao()
        dao.insert(item("i1", "p", 0))
        assertThat(runCatching { dao.insert(item("i9", "p", 0)) }.exceptionOrNull())
            .isInstanceOf(SQLiteConstraintException::class.java)
        dao.insert(item("i8", "other", 0))
        assertThat(dao.findAllIncludingDeleted()).hasSize(2)
    }
}
