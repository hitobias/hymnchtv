package org.cog.hymnchtv.notebook.fakes

import org.cog.hymnchtv.notebook.contract.PlaylistRepositoryContract
import org.cog.hymnchtv.notebook.model.Clock
import org.cog.hymnchtv.notebook.model.DeviceIdProvider
import org.cog.hymnchtv.notebook.repo.PlaylistRepository

class InMemoryPlaylistRepositoryContractTest : PlaylistRepositoryContract() {
    override fun newRepository(clock: Clock, device: DeviceIdProvider): PlaylistRepository =
        InMemoryPlaylistRepository(clock, device = device.deviceId())
}
