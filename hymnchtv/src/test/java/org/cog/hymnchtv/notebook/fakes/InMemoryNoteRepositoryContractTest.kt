package org.cog.hymnchtv.notebook.fakes

import org.cog.hymnchtv.notebook.contract.NoteRepositoryContract
import org.cog.hymnchtv.notebook.model.Clock
import org.cog.hymnchtv.notebook.model.DeviceIdProvider
import org.cog.hymnchtv.notebook.repo.NoteRepository

class InMemoryNoteRepositoryContractTest : NoteRepositoryContract() {
    override fun newRepository(clock: Clock, device: DeviceIdProvider): NoteRepository =
        InMemoryNoteRepository(clock, device = device.deviceId())
}
