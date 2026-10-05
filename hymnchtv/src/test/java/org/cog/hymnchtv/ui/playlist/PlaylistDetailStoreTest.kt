package org.cog.hymnchtv.ui.playlist

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.cog.hymnchtv.notebook.data.entity.PlaylistItemEntity
import org.cog.hymnchtv.notebook.fakes.InMemoryPlaylistRepository
import org.cog.hymnchtv.notebook.fakes.MutableClock
import org.cog.hymnchtv.notebook.fakes.testUuid
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.cog.hymnchtv.notebook.repo.PlaylistRepository
import org.junit.Test

/** Lets a test hold a write half-way (each queued gate is awaited by the next call of that kind). */
private class DelayedPlaylistRepository(private val inner: InMemoryPlaylistRepository) : PlaylistRepository by inner {
    val removeGates = ArrayDeque<CompletableDeferred<Unit>>()
    val reorderGates = ArrayDeque<CompletableDeferred<Unit>>()
    val addGates = ArrayDeque<CompletableDeferred<Unit>>()
    var addCalls = 0

    override suspend fun removeItem(itemId: String): Boolean {
        removeGates.removeFirstOrNull()?.await()
        return inner.removeItem(itemId)
    }

    override suspend fun reorder(playlistId: String, orderedItemIds: List<String>): List<PlaylistItemEntity> {
        reorderGates.removeFirstOrNull()?.await()
        return inner.reorder(playlistId, orderedItemIds)
    }

    override suspend fun addItem(playlistId: String, key: HymnKey): PlaylistItemEntity? {
        addCalls++
        addGates.removeFirstOrNull()?.await()
        return inner.addItem(playlistId, key)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistDetailStoreTest {
    private val db5 = HymnKey.of(HymnTypes.DB, 5)
    private val db6 = HymnKey.of(HymnTypes.DB, 6)
    private val db7 = HymnKey.of(HymnTypes.DB, 7)
    private val clock = MutableClock(1_790_733_600_000L)
    private val repo = InMemoryPlaylistRepository(clock)
    private val slow = DelayedPlaylistRepository(repo)

    private suspend fun seeded(): String {
        val playlist = repo.createPlaylist("主日")
        listOf(db5, db6, db7).forEach { repo.addItem(playlist.id, it) }
        return playlist.id
    }

    private fun TestScope.store(id: String, lastOpened: String? = null) = PlaylistDetailStore(
        id, slow, { ref -> "T${ref.storedNo}" }, backgroundScope, UnconfinedTestDispatcher(testScheduler), lastOpened,
    ).also { it.load(); runCurrent() }

    @Test fun loadShowsTheNameTheItemsInOrderAndTheirTitles() = runTest {
        val s = store(seeded()).state.value
        assertThat(s.name).isEqualTo("主日")
        assertThat(s.items.map { it.key }).containsExactly(db5, db6, db7).inOrder()
        assertThat(s.items.map { it.title }).containsExactly("T5", "T6", "T7").inOrder()
        assertThat(s.button).isEqualTo(PlayButton.Start)
    }

    @Test fun aMissingPlaylistIsReported() = runTest {
        assertThat(store(testUuid(999)).state.value.missing).isTrue()
    }

    @Test fun openingItemsMovesTheButtonAlong() = runTest {
        val store = store(seeded())
        val items = store.state.value.items
        store.markOpened(items[0].itemId)
        assertThat(store.state.value.button).isEqualTo(PlayButton.Next(1))
        assertThat(store.state.value.nextIndex).isEqualTo(1)
        store.markOpened(items[2].itemId)
        assertThat(store.state.value.button).isEqualTo(PlayButton.Restart)
        assertThat(store.state.value.nextIndex).isNull()
    }

    @Test fun theLastOpenedItemComesBackFromTheSavedState() = runTest {
        val id = seeded()
        val first = repo.items(id).first().id
        assertThat(store(id, lastOpened = first).state.value.button).isEqualTo(PlayButton.Next(1))
    }

    @Test fun removeHidesTheItemAtOnceAndUndoPutsItBackInPlace() = runTest {
        val id = seeded()
        val store = store(id)
        val middle = store.state.value.items[1]
        val removed = checkNotNull(store.remove(middle.itemId))
        assertThat(store.state.value.items.map { it.key }).containsExactly(db5, db7).inOrder()
        runCurrent()
        assertThat(repo.items(id).map { it.hymn }).containsExactly(db5, db7).inOrder()
        store.undoRemove(removed)
        runCurrent()
        assertThat(repo.items(id).map { it.hymn }).containsExactly(db5, db6, db7).inOrder()
        assertThat(store.state.value.items.map { it.key }).containsExactly(db5, db6, db7).inOrder()
    }

    @Test fun moveReordersOnScreenAndInStorage() = runTest {
        val id = seeded()
        val store = store(id)
        store.move(0, 2)
        assertThat(store.state.value.items.map { it.key }).containsExactly(db6, db7, db5).inOrder()
        runCurrent()
        assertThat(repo.items(id).map { it.hymn }).containsExactly(db6, db7, db5).inOrder()
    }

    @Test fun aReorderThatIsNotAPermutationIsIgnored() = runTest {
        val id = seeded()
        val store = store(id)
        store.reorder(listOf(store.state.value.items[0].itemId))
        runCurrent()
        assertThat(repo.items(id).map { it.hymn }).containsExactly(db5, db6, db7).inOrder()
        assertThat(store.state.value.message).isNull()
    }

    @Test fun renameChangesTheName() = runTest {
        val id = seeded()
        val store = store(id)
        store.rename("  晚上 ")
        runCurrent()
        assertThat(store.state.value.name).isEqualTo("晚上")
    }

    @Test fun deleteCallsBackAndThePlaylistIsGone() = runTest {
        val id = seeded()
        val store = store(id)
        var deleted = false
        store.delete { deleted = true }
        runCurrent()
        assertThat(deleted).isTrue()
        assertThat(repo.findById(id)).isNull()
    }

    @Test fun aFailedWriteIsReportedAndTheScreenReloads() = runTest {
        val id = seeded()
        val store = store(id)
        repo.failNext = true
        store.move(0, 1)
        runCurrent()
        assertThat(store.state.value.message).isEqualTo(PlaylistMessage.WRITE_FAILED)
        assertThat(store.state.value.items.map { it.key }).containsExactly(db5, db6, db7).inOrder()
    }

    @Test fun removeThenAnImmediateUndoEndsInTheOriginalOrder() = runTest {
        val id = seeded()
        val store = store(id)
        val gate = CompletableDeferred<Unit>().also { slow.removeGates += it }
        val removed = checkNotNull(store.remove(store.state.value.items[1].itemId))
        store.undoRemove(removed)
        runCurrent()
        // the remove is held: the undo waits behind it (never started) and nothing has been reloaded over the screen
        assertThat(slow.addCalls).isEqualTo(0)
        assertThat(store.state.value.items.map { it.key }).containsExactly(db5, db7).inOrder()
        gate.complete(Unit)
        runCurrent()
        assertThat(repo.items(id).map { it.hymn }).containsExactly(db5, db6, db7).inOrder()
        assertThat(store.state.value.items.map { it.key }).containsExactly(db5, db6, db7).inOrder()
        assertThat(store.state.value.message).isNull()
    }

    @Test fun twoQuickReordersEndInTheSecondOrderAndAnOlderReloadNeverShows() = runTest {
        val id = seeded()
        val store = store(id)
        val first = CompletableDeferred<Unit>().also { slow.reorderGates += it }
        val second = CompletableDeferred<Unit>().also { slow.reorderGates += it }
        store.move(0, 2) // 6, 7, 5
        store.move(0, 1) // 7, 6, 5
        runCurrent()
        // the second gate opens first: without the lock the second write would finish before the first and be overwritten
        second.complete(Unit)
        runCurrent()
        assertThat(repo.items(id).map { it.hymn }).containsExactly(db5, db6, db7).inOrder()
        first.complete(Unit)
        runCurrent()
        // both writes ran in call order; the older one's reload must not have replaced the newer screen
        assertThat(repo.items(id).map { it.hymn }).containsExactly(db7, db6, db5).inOrder()
        assertThat(store.state.value.items.map { it.key }).containsExactly(db7, db6, db5).inOrder()
        assertThat(store.state.value.message).isNull()
    }

    @Test fun aReorderWhileAnUndoIsOnItsWayKeepsTheRestoredHymn() = runTest {
        val id = seeded()
        val store = store(id)
        val gate = CompletableDeferred<Unit>().also { slow.addGates += it }
        val removed = checkNotNull(store.remove(store.state.value.items[1].itemId))
        store.undoRemove(removed)
        store.move(0, 1) // the screen knows 5, 7 only: 7, 5
        runCurrent()
        gate.complete(Unit)
        runCurrent()
        // the undo put 6 back between 5 and 7; the reorder kept the screen's order and left the row it never saw at the end
        val stored = repo.items(id).map { it.hymn }
        assertThat(stored).containsExactly(db7, db5, db6).inOrder()
        assertThat(store.state.value.items.map { it.key }).isEqualTo(stored)
        assertThat(store.state.value.message).isNull()
    }

    @Test fun aQueuedWriteSurvivesTheViewModelBeingCleared() = runTest {
        val id = seeded()
        val job = kotlinx.coroutines.Job()
        val scope = kotlinx.coroutines.CoroutineScope(job + UnconfinedTestDispatcher(testScheduler))
        val store = PlaylistDetailStore(id, slow, { null }, scope, UnconfinedTestDispatcher(testScheduler))
        store.load()
        runCurrent()
        val items = store.state.value.items
        val gate = CompletableDeferred<Unit>().also { slow.removeGates += it }
        store.remove(items[0].itemId) // holds the lock at the gate
        store.remove(items[1].itemId) // waits for the lock
        runCurrent()
        job.cancel() // the ViewModel is cleared
        gate.complete(Unit)
        runCurrent()
        assertThat(repo.items(id).map { it.hymn }).containsExactly(db7)
    }

    @Test fun aReorderWithNewIdsKeepsTheDraggedOrderAndAppendsTheRest() = runTest {
        val id = seeded()
        val store = store(id)
        val shown = store.state.value.items
        // a drag finished while the screen had been refreshed with one more item: ids differ from the shown set
        val extra = repo.addItem(id, HymnKey.of(HymnTypes.DB, 8))!!
        store.reorder(listOf(shown[2].itemId, shown[0].itemId, shown[1].itemId))
        runCurrent()
        assertThat(repo.items(id).map { it.hymn }.take(3)).containsExactly(db7, db5, db6).inOrder()
        assertThat(repo.items(id).last().id).isEqualTo(extra.id)
    }

    @Test fun aDraggedOrderMissingAShownItemIsMergedNotDropped() = runTest {
        val id = seeded()
        val store = store(id)
        val shown = store.state.value.items
        // the order lists only two of the three shown items (the third was removed meanwhile): known ids keep their order
        store.reorder(listOf(shown[2].itemId, shown[0].itemId))
        runCurrent()
        assertThat(repo.items(id).map { it.hymn }).containsExactly(db7, db5, db6).inOrder()
    }
}
