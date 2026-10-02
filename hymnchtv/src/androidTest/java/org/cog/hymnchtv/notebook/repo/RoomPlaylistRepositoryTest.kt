package org.cog.hymnchtv.notebook.repo

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.cog.hymnchtv.notebook.data.NotebookDatabase
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.cog.hymnchtv.notebook.repo.room.RoomPlaylistRepository
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomPlaylistRepositoryTest {
    private lateinit var db: NotebookDatabase
    private lateinit var repo: RoomPlaylistRepository
    private val clock = TestClock(1_000)
    private val k1 = HymnKey.of(HymnTypes.DB, 1)
    private val k2 = HymnKey.of(HymnTypes.DB, 781)
    private val k3 = HymnKey.of(HymnTypes.ER, 5)

    @Before
    fun setUp() {
        db = NotebookDatabase.inMemory(ApplicationProvider.getApplicationContext())
        repo = RoomPlaylistRepository(db, clock, TestIds(), testDevice)
    }

    @After
    fun tearDown() = db.close()

    private fun rejects(block: suspend () -> Unit) = runBlocking {
        assertThat(runCatching { block() }.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun itemsAppendRemoveAndReorderWithSparsePositions(): Unit = runBlocking {
        val pl = repo.createPlaylist("  主日 10/4 ")
        assertThat(pl.name).isEqualTo("主日 10/4")
        val a = checkNotNull(repo.addItem(pl.id, k1))
        val b = checkNotNull(repo.addItem(pl.id, k2))
        val c = checkNotNull(repo.addItem(pl.id, k3))
        assertThat(listOf(a, b, c).map { it.position }).containsExactly(0, 1, 2).inOrder()

        assertThat(repo.removeItem(b.id)).isTrue()
        assertThat(repo.removeItem(b.id)).isFalse()
        assertThat(repo.items(pl.id).map { it.id }).containsExactly(a.id, c.id).inOrder()

        clock.now = 2_000
        val reordered = repo.reorder(pl.id, listOf(c.id, a.id))
        assertThat(reordered.map { it.id to it.position }).containsExactly(c.id to 3, a.id to 4).inOrder()
        assertThat(reordered.map { it.updatedAt }).containsExactly(2_000L, 2_000L)
        assertThat(repo.items(pl.id).map { it.id }).containsExactly(c.id, a.id).inOrder()
        assertThat(checkNotNull(repo.addItem(pl.id, k2)).position).isEqualTo(5)
    }

    @Test
    fun reorderInTheCurrentOrderChangesNothing(): Unit = runBlocking {
        val pl = repo.createPlaylist("歌單")
        val a = checkNotNull(repo.addItem(pl.id, k1))
        val b = checkNotNull(repo.addItem(pl.id, k2))
        assertThat(repo.reorder(pl.id, listOf(a.id, b.id))).containsExactly(a, b).inOrder()
    }

    @Test
    fun reorderRejectsNonPermutation(): Unit = runBlocking {
        val pl = repo.createPlaylist("歌單")
        val a = checkNotNull(repo.addItem(pl.id, k1))
        checkNotNull(repo.addItem(pl.id, k2))
        rejects { repo.reorder(pl.id, listOf(a.id)) }
        rejects { repo.reorder(pl.id, listOf(a.id, androidTestUuid(404))) }
    }

    @Test
    fun deletingPlaylistSoftDeletesItems(): Unit = runBlocking {
        val pl = repo.createPlaylist("歌單")
        val a = checkNotNull(repo.addItem(pl.id, k1))
        clock.now = 2_000
        assertThat(repo.delete(pl.id)).isTrue()
        assertThat(repo.findById(pl.id)).isNull()
        assertThat(repo.items(pl.id)).isEmpty()
        assertThat(repo.addItem(pl.id, k2)).isNull()
        assertThat(db.playlistItemDao().findById(a.id)?.deletedAt).isEqualTo(2_000L)
    }

    @Test
    fun renameAndNameValidation(): Unit = runBlocking {
        val pl = repo.createPlaylist("舊名")
        clock.now = 2_000
        val renamed = repo.rename(pl.id, " 新名 ")
        assertThat(renamed?.name).isEqualTo("新名")
        assertThat(renamed?.updatedAt).isEqualTo(2_000L)
        assertThat(repo.rename(androidTestUuid(404), "x")).isNull()
        rejects { repo.rename(pl.id, " ") }
        rejects { repo.createPlaylist("") }
    }
}
