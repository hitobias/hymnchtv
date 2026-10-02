package org.cog.hymnchtv.notebook.repo

import org.cog.hymnchtv.notebook.model.Clock
import org.cog.hymnchtv.notebook.model.DeviceIdProvider
import org.cog.hymnchtv.notebook.model.IdGenerator
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

fun androidTestUuid(n: Int): String = UUID(0L, n.toLong()).toString()

const val ANDROID_TEST_DEVICE = "00000000-0000-0000-0000-0000000000bb"

val testDevice = DeviceIdProvider { ANDROID_TEST_DEVICE }

class TestClock(@Volatile var now: Long) : Clock {
    override fun nowMillis(): Long = now
}

class TestIds : IdGenerator {
    private val counter = AtomicInteger()
    override fun newId(): String = androidTestUuid(counter.incrementAndGet())
}
