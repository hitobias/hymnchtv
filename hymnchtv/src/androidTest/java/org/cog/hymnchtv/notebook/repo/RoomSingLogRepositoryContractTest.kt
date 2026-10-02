package org.cog.hymnchtv.notebook.repo

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.cog.hymnchtv.notebook.contract.SingLogRepositoryContract
import org.cog.hymnchtv.notebook.data.NotebookDatabase
import org.cog.hymnchtv.notebook.model.Clock
import org.cog.hymnchtv.notebook.model.DeviceIdProvider
import org.cog.hymnchtv.notebook.model.IdGenerator
import org.cog.hymnchtv.notebook.repo.room.RoomSingLogRepository
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomSingLogRepositoryContractTest : SingLogRepositoryContract() {
    private var db: NotebookDatabase? = null

    override fun newRepository(clock: Clock, device: DeviceIdProvider): SingLogRepository {
        val database = NotebookDatabase.inMemory(ApplicationProvider.getApplicationContext())
        db = database
        return RoomSingLogRepository(database, clock, IdGenerator.RANDOM_UUID, device)
    }

    override fun tearDownRepository() {
        db?.close()
    }
}
