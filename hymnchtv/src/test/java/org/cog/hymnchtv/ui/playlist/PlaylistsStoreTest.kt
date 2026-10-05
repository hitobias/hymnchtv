package org.cog.hymnchtv.ui.playlist

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.cog.hymnchtv.notebook.data.entity.PlaylistEntity
import org.cog.hymnchtv.notebook.fakes.InMemoryPlaylistRepository
import org.cog.hymnchtv.notebook.fakes.MutableClock
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistsStoreTest {
    private val clock = MutableClock(1_790_733_600_000L)
    private val repo = InMemoryPlaylistRepository(clock)

    @Test fun loadListsPlaylistsWithTheirCounts() = runTest {
        val sunday = repo.createPlaylist("主日")
        repo.addItem(sunday.id, HymnKey.of(HymnTypes.DB, 1))
        repo.addItem(sunday.id, HymnKey.of(HymnTypes.DB, 2))
        clock.now += 1
        repo.createPlaylist("小排")
        val store = PlaylistsStore(repo, backgroundScope, UnconfinedTestDispatcher(testScheduler))
        store.load()
        runCurrent()
        assertThat(store.state.value.rows.map { it.name to it.count }).containsExactly("小排" to 0, "主日" to 2).inOrder()
        assertThat(store.state.value.loading).isFalse()
    }

    @Test fun createCallsBackWithTheNewPlaylistAndReloads() = runTest {
        val store = PlaylistsStore(repo, backgroundScope, UnconfinedTestDispatcher(testScheduler))
        var created: PlaylistEntity? = null
        store.create("  新歌單 ") { created = it }
        runCurrent()
        assertThat(created?.name).isEqualTo("新歌單")
        assertThat(store.state.value.rows.map { it.name }).containsExactly("新歌單")
    }

    @Test fun aRejectedNameIsReported() = runTest {
        val store = PlaylistsStore(repo, backgroundScope, UnconfinedTestDispatcher(testScheduler))
        var called = false
        store.create("   ") { called = true }
        runCurrent()
        assertThat(called).isFalse()
        assertThat(store.state.value.message).isEqualTo(PlaylistMessage.WRITE_FAILED)
        store.consumeMessage()
        assertThat(store.state.value.message).isNull()
    }

    @Test fun aFailedLoadIsNotShownAsEmpty() = runTest {
        repo.failNext = true
        val store = PlaylistsStore(repo, backgroundScope, UnconfinedTestDispatcher(testScheduler))
        store.load()
        runCurrent()
        assertThat(store.state.value.loadFailed).isTrue()
    }
}
