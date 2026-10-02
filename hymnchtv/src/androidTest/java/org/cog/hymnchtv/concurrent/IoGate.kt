package org.cog.hymnchtv.concurrent

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Test helper: occupies the single AppExecutors.io thread until [release], so a test can prove that work
 * submitted afterwards really runs on that thread and not on the caller (main) thread.
 */
class IoGate private constructor() {
    private val started = CountDownLatch(1)
    private val open = CountDownLatch(1)

    fun release() = open.countDown()

    companion object {
        /** Returns once the blocking task is running on the io thread; everything queued after it waits. */
        fun close(): IoGate {
            val gate = IoGate()
            AppExecutors.io("test-io-gate") {
                gate.started.countDown()
                gate.open.await(30, TimeUnit.SECONDS)
            }
            check(gate.started.await(10, TimeUnit.SECONDS)) { "AppExecutors.io did not pick up the gate task" }
            return gate
        }
    }
}
