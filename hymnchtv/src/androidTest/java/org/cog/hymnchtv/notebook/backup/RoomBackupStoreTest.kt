package org.cog.hymnchtv.notebook.backup

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.cog.hymnchtv.persistance.room.HymnchtvDatabase
import org.cog.hymnchtv.notebook.data.entity.FavoriteEntity
import org.cog.hymnchtv.notebook.data.entity.NoteEntity
import org.cog.hymnchtv.notebook.data.entity.PlaylistEntity
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
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class RoomBackupStoreTest {
    private lateinit var db: HymnchtvDatabase
    private lateinit var store: RoomBackupStore
    private val key = HymnKey.of(HymnTypes.DB, 1)
    private fun id(n: Int) = UUID(0L, n.toLong()).toString()
    private val device = id(170)

    private val tables by lazy {
        NotebookTables(
            favorites = listOf(FavoriteEntity(FavoriteIds.forKey(key), key, 1, 2, 2, device)),
            singLogs = listOf(
                SingLogEntity(
                    id = id(1), hymn = key, sungAt = 5, occasion = Occasion.MORNING_REVIVAL, source = SingSource.MANUAL,
                    playlistId = id(3), createdAt = 1, updatedAt = 1, updatedBy = device,
                ),
            ),
            notes = listOf(NoteEntity(id(2), key, "筆記", id(1), createdAt = 1, updatedAt = 1, updatedBy = device)),
            playlists = listOf(PlaylistEntity(id(3), "歌單", createdAt = 1, updatedAt = 1, updatedBy = device)),
            playlistItems = listOf(PlaylistItemEntity(id(4), id(3), 0, key, createdAt = 1, updatedAt = 1, deletedAt = 3, updatedBy = device)),
        )
    }

    @Before
    fun setUp() {
        db = HymnchtvDatabase.inMemory(ApplicationProvider.getApplicationContext())
        store = RoomBackupStore(db)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun writesAndReadsEveryRowIncludingDeleted(): Unit = runBlocking {
        val result = store.mergeAtomically { local ->
            assertThat(local.rowCount).isEqualTo(0)
            Planned(tables, "done")
        }
        assertThat(result).isEqualTo("done")
        assertThat(store.readAll()).isEqualTo(tables)
    }

    @Test
    fun failingPlanWritesNothing(): Unit = runBlocking {
        val outcome = runCatching { store.mergeAtomically<Unit> { throw IllegalStateException("boom") } }
        assertThat(outcome.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
        assertThat(store.readAll().rowCount).isEqualTo(0)
    }

    @Test
    fun mergedPlaylistItemsNeverViolateTheSlotIndex(): Unit = runBlocking {
        store.mergeAtomically { Planned(tables.copy(playlistItems = listOf(tables.playlistItems.single().copy(deletedAt = null))), Unit) }
        val incoming = NotebookTables(
            playlistItems = listOf(PlaylistItemEntity(id(5), id(3), 0, key, createdAt = 2, updatedAt = 2, updatedBy = device)),
        )
        store.mergeAtomically { local -> BackupMerger.merge(local, incoming).let { Planned(it.changes, Unit) } }
        assertThat(store.readAll().playlistItems.map { it.id to it.position }).containsExactly(id(4) to 1, id(5) to 0) // the newer incoming item wins slot 0; the local item moves
    }

    private fun item(n: Int, position: Int, updatedAt: Long = 1) =
        PlaylistItemEntity(id(n), id(3), position, key, createdAt = 1, updatedAt = updatedAt, updatedBy = device)

    private suspend fun seedItems(vararg rows: PlaylistItemEntity) =
        store.mergeAtomically { Planned(NotebookTables(playlistItems = rows.toList()), Unit) }

    private suspend fun importItems(vararg rows: PlaylistItemEntity) =
        store.mergeAtomically { local ->
            BackupMerger.merge(local, NotebookTables(playlistItems = rows.toList())).let { Planned(it.changes, Unit) }
        }

    private suspend fun positions() = store.readAll().playlistItems.associate { it.id to it.position }

    @Test
    fun importedSwapOfTwoExistingItemsSucceeds(): Unit = runBlocking {
        seedItems(item(10, 0), item(11, 1))
        importItems(item(10, 1, updatedAt = 2), item(11, 0, updatedAt = 2))
        assertThat(positions()).containsExactly(id(10), 1, id(11), 0)
    }

    @Test
    fun importedThreeCycleSucceeds(): Unit = runBlocking {
        seedItems(item(10, 0), item(11, 1), item(12, 2))
        importItems(item(10, 1, updatedAt = 2), item(11, 2, updatedAt = 2), item(12, 0, updatedAt = 2))
        assertThat(positions()).containsExactly(id(10), 1, id(11), 2, id(12), 0)
    }

    @Test
    fun importedThreeWayReorderWithAnUnchangedMiddleSucceeds(): Unit = runBlocking {
        seedItems(item(10, 0), item(11, 1), item(12, 2))
        importItems(item(10, 2, updatedAt = 2), item(11, 1), item(12, 0, updatedAt = 2))
        assertThat(positions()).containsExactly(id(10), 2, id(11), 1, id(12), 0)
        assertThat(store.readAll().playlistItems.none { it.position < 0 }).isTrue()
    }
}
