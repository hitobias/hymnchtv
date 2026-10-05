package org.cog.hymnchtv.notebook.contract

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.cog.hymnchtv.notebook.contract.SingLogRepositoryContract.Companion.DEVICE
import org.cog.hymnchtv.notebook.contract.SingLogRepositoryContract.Companion.NOW
import org.cog.hymnchtv.notebook.model.Clock
import org.cog.hymnchtv.notebook.model.DeviceIdProvider
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.cog.hymnchtv.notebook.model.NotebookValidation
import org.cog.hymnchtv.notebook.repo.PlaylistRepository
import org.junit.After
import org.junit.Test

/**
 * Behaviour every PlaylistRepository must share (the playlist screens rely on it). Subclasses:
 * InMemoryPlaylistRepositoryContractTest (JVM) and RoomPlaylistRepositoryContractTest (androidTest).
 */
abstract class PlaylistRepositoryContract {
    protected val clock = ContractClock(NOW)
    private val db1 = HymnKey.of(HymnTypes.DB, 1)
    private val db2 = HymnKey.of(HymnTypes.DB, 2)
    private val db3 = HymnKey.of(HymnTypes.DB, 3)
    private val repo by lazy { newRepository(clock, DeviceIdProvider { DEVICE }) }

    protected abstract fun newRepository(clock: Clock, device: DeviceIdProvider): PlaylistRepository

    protected open fun tearDownRepository() {}

    @After
    fun closeRepository() = tearDownRepository()

    private fun rejects(block: suspend () -> Unit) = runBlocking {
        assertThat(runCatching { block() }.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun createTrimsTheNameAndRejectsBlankOrTooLong() {
        val playlist = runBlocking { repo.createPlaylist("  主日  ") }
        assertThat(playlist.name).isEqualTo("主日")
        assertThat(playlist.updatedBy).isEqualTo(DEVICE)
        rejects { repo.createPlaylist("   ") }
        rejects { repo.createPlaylist("x".repeat(NotebookValidation.MAX_PLAYLIST_NAME_LENGTH + 1)) }
    }

    @Test
    fun itemsKeepTheirOrderAndSlotsOnlyGrow(): Unit = runBlocking {
        val p = repo.createPlaylist("p")
        val a = checkNotNull(repo.addItem(p.id, db1))
        val b = checkNotNull(repo.addItem(p.id, db2))
        assertThat(repo.removeItem(b.id)).isTrue()
        assertThat(repo.removeItem(b.id)).isFalse()
        val c = checkNotNull(repo.addItem(p.id, db3))
        assertThat(c.position).isGreaterThan(b.position)
        assertThat(repo.items(p.id).map { it.id }).containsExactly(a.id, c.id).inOrder()
    }

    @Test
    fun theSameHymnMayAppearTwice(): Unit = runBlocking {
        val p = repo.createPlaylist("p")
        repo.addItem(p.id, db1)
        repo.addItem(p.id, db1)
        assertThat(repo.items(p.id).map { it.hymn }).containsExactly(db1, db1)
    }

    @Test
    fun addingToAMissingOrDeletedPlaylistGivesNull(): Unit = runBlocking {
        assertThat(repo.addItem(MISSING, db1)).isNull()
        val p = repo.createPlaylist("p")
        assertThat(repo.delete(p.id)).isTrue()
        assertThat(repo.addItem(p.id, db1)).isNull()
    }

    @Test
    fun reorderAppliesAPermutationAndRejectsAnythingElse(): Unit = runBlocking {
        val p = repo.createPlaylist("p")
        val a = checkNotNull(repo.addItem(p.id, db1))
        val b = checkNotNull(repo.addItem(p.id, db2))
        val c = checkNotNull(repo.addItem(p.id, db3))
        val reordered = repo.reorder(p.id, listOf(c.id, a.id, b.id))
        assertThat(reordered.map { it.id }).containsExactly(c.id, a.id, b.id).inOrder()
        assertThat(repo.items(p.id).map { it.id }).containsExactly(c.id, a.id, b.id).inOrder()
        rejects { repo.reorder(p.id, listOf(a.id, b.id)) }
        rejects { repo.reorder(p.id, listOf(a.id, b.id, MISSING)) }
    }

    @Test
    fun renameMovesThePlaylistToTheTopAndDeleteHidesItAndItsItems(): Unit = runBlocking {
        val one = repo.createPlaylist("one")
        clock.now = NOW + 1
        val two = repo.createPlaylist("two")
        clock.now = NOW + 2
        assertThat(repo.rename(one.id, " uno ")?.name).isEqualTo("uno")
        assertThat(repo.findAll().map { it.name }).containsExactly("uno", "two").inOrder()
        repo.addItem(one.id, db1)
        assertThat(repo.delete(one.id)).isTrue()
        assertThat(repo.items(one.id)).isEmpty()
        assertThat(repo.findAll().map { it.id }).containsExactly(two.id)
        assertThat(repo.rename(one.id, "again")).isNull()
    }

    @Test
    fun createWithItemStoresThePlaylistAndItsFirstHymn(): Unit = runBlocking {
        val (playlist, item) = repo.createPlaylistWithItem(" 晚上 ", db1)
        assertThat(playlist.name).isEqualTo("晚上")
        assertThat(repo.items(playlist.id).map { it.id }).containsExactly(item.id)
        rejects { repo.createPlaylistWithItem("   ", db1) }
        assertThat(repo.findAll().map { it.id }).containsExactly(playlist.id)
    }

    private companion object {
        const val MISSING = "00000000-0000-0000-0000-0000000000ff"
    }
}
