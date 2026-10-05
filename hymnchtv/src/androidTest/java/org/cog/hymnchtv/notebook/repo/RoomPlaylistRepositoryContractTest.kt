package org.cog.hymnchtv.notebook.repo

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.cog.hymnchtv.notebook.contract.PlaylistRepositoryContract
import org.cog.hymnchtv.notebook.model.Clock
import org.cog.hymnchtv.notebook.model.DeviceIdProvider
import org.cog.hymnchtv.notebook.model.IdGenerator
import org.cog.hymnchtv.notebook.repo.room.RoomPlaylistRepository
import org.cog.hymnchtv.persistance.room.HymnchtvDatabase
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomPlaylistRepositoryContractTest : PlaylistRepositoryContract() {
    private var db: HymnchtvDatabase? = null

    override fun newRepository(clock: Clock, device: DeviceIdProvider): PlaylistRepository {
        val database = HymnchtvDatabase.inMemory(ApplicationProvider.getApplicationContext())
        db = database
        return RoomPlaylistRepository(database, clock, IdGenerator.RANDOM_UUID, device)
    }

    override fun tearDownRepository() {
        db?.close()
    }
}
