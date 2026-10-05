package org.cog.hymnchtv.notebook.repo

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.cog.hymnchtv.notebook.contract.NoteRepositoryContract
import org.cog.hymnchtv.notebook.model.Clock
import org.cog.hymnchtv.notebook.model.DeviceIdProvider
import org.cog.hymnchtv.notebook.model.IdGenerator
import org.cog.hymnchtv.notebook.repo.room.RoomNoteRepository
import org.cog.hymnchtv.persistance.room.HymnchtvDatabase
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomNoteRepositoryContractTest : NoteRepositoryContract() {
    private var db: HymnchtvDatabase? = null

    override fun newRepository(clock: Clock, device: DeviceIdProvider): NoteRepository {
        val database = HymnchtvDatabase.inMemory(ApplicationProvider.getApplicationContext())
        db = database
        return RoomNoteRepository(database, clock, IdGenerator.RANDOM_UUID, device)
    }

    override fun tearDownRepository() {
        db?.close()
    }
}
