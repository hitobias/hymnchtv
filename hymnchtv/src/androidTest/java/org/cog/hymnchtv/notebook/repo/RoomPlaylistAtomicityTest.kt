package org.cog.hymnchtv.notebook.repo

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.cog.hymnchtv.notebook.model.DeviceIdProvider
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.cog.hymnchtv.notebook.model.IdGenerator
import org.cog.hymnchtv.notebook.repo.room.RoomPlaylistRepository
import org.cog.hymnchtv.persistance.room.HymnchtvDatabase
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

/** createPlaylistWithItem is one transaction: a failure in its second write rolls back the first. */
@RunWith(AndroidJUnit4::class)
class RoomPlaylistAtomicityTest {
    @Test
    fun aFailureWhileAddingTheHymnLeavesNoEmptyPlaylist(): Unit = runBlocking {
        val db = HymnchtvDatabase.inMemory(ApplicationProvider.getApplicationContext())
        try {
            // create() asks for the device id once, addItem() a second time: fail there, inside the transaction
            val calls = AtomicInteger()
            val device = DeviceIdProvider {
                if (calls.incrementAndGet() >= 2) throw IllegalStateException("simulated") else ANDROID_TEST_DEVICE
            }
            val repo = RoomPlaylistRepository(db, TestClock(1_790_733_600_000L), IdGenerator.RANDOM_UUID, device)
            assertThat(runCatching { repo.createPlaylistWithItem("晚上", HymnKey.of(HymnTypes.DB, 5)) }.isFailure).isTrue()
            assertThat(db.playlistDao().findAllIncludingDeleted()).isEmpty()
            assertThat(db.playlistItemDao().findAllIncludingDeleted()).isEmpty()
        } finally {
            db.close()
        }
    }
}
