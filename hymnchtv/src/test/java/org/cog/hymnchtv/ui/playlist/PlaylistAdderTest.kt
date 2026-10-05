package org.cog.hymnchtv.ui.playlist

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.cog.hymnchtv.notebook.fakes.InMemoryPlaylistRepository
import org.cog.hymnchtv.notebook.fakes.MutableClock
import org.cog.hymnchtv.notebook.fakes.testUuid
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistAdderTest {
    private val db5 = HymnKey.of(HymnTypes.DB, 5)
    private val repo = InMemoryPlaylistRepository(MutableClock(1_790_733_600_000L))

    private fun kotlinx.coroutines.test.TestScope.adder(): PlaylistAdder {
        val now = UnconfinedTestDispatcher(testScheduler)
        return PlaylistAdder(repo, backgroundScope, now, now)
    }

    @Test fun addAppendsTheHymnAndReportsThePlaylistName() = runTest {
        val playlist = repo.createPlaylist("主日")
        var result: String? = "unset"
        adder().add(playlist.id, db5) { result = it }
        runCurrent()
        assertThat(result).isEqualTo("主日")
        assertThat(repo.items(playlist.id).map { it.hymn }).containsExactly(db5)
    }

    @Test fun addToAMissingPlaylistReportsNull() = runTest {
        var result: String? = "unset"
        adder().add(testUuid(404), db5) { result = it }
        runCurrent()
        assertThat(result).isNull()
    }

    @Test fun createAndAddMakesThePlaylistWithTheHymn() = runTest {
        var result: String? = null
        adder().createAndAdd(" 晚上 ", db5) { result = it }
        runCurrent()
        assertThat(result).isEqualTo("晚上")
        val created = repo.findAll().single()
        assertThat(repo.items(created.id).map { it.hymn }).containsExactly(db5)
    }

    @Test fun aFailureAfterCreatingLeavesNoEmptyPlaylist() = runTest {
        repo.failNextAddItem = true
        var result: String? = "unset"
        adder().createAndAdd("晚上", db5) { result = it }
        runCurrent()
        assertThat(result).isNull()
        assertThat(repo.findAll()).isEmpty()
    }

    @Test fun aFailureReportsNullInsteadOfThrowing() = runTest {
        repo.failNext = true
        var result: String? = "unset"
        adder().createAndAdd("x", db5) { result = it }
        runCurrent()
        assertThat(result).isNull()
    }
}
