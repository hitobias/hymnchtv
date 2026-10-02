package org.cog.hymnchtv.notebook.fakes

import org.cog.hymnchtv.notebook.contract.SingLogRepositoryContract
import org.cog.hymnchtv.notebook.model.Clock
import org.cog.hymnchtv.notebook.model.DeviceIdProvider
import org.cog.hymnchtv.notebook.repo.SingLogRepository

class InMemorySingLogRepositoryContractTest : SingLogRepositoryContract() {
    override fun newRepository(clock: Clock, device: DeviceIdProvider): SingLogRepository =
        InMemorySingLogRepository(clock, device = device.deviceId())
}
