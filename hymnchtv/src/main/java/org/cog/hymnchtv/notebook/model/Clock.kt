package org.cog.hymnchtv.notebook.model

import java.util.UUID

fun interface Clock {
    fun nowMillis(): Long

    companion object {
        @JvmField
        val SYSTEM: Clock = Clock { System.currentTimeMillis() }
    }
}

fun interface IdGenerator {
    fun newId(): String

    companion object {
        @JvmField
        val RANDOM_UUID: IdGenerator = IdGenerator { UUID.randomUUID().toString() }
    }
}

/** Supplies the id stamped into updatedBy. Called on background threads only (may hit disk once). */
fun interface DeviceIdProvider {
    fun deviceId(): String
}
