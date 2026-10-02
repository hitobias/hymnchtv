package org.cog.hymnchtv.toc

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class LazyMapTest {
    @Test
    fun loadsOnFirstAccessOnly() {
        var loads = 0
        val map = LazyMap { loads++; mapOf(1 to "a") }
        assertThat(loads).isEqualTo(0)

        assertThat(map[1]).isEqualTo("a")
        assertThat(map.size).isEqualTo(1)
        assertThat(map.containsKey(2)).isFalse()
        assertThat(loads).isEqualTo(1)
    }

    @Test
    fun isReadOnly() {
        val map = LazyMap { mapOf(1 to "a") }
        // Kotlin's Map has no put; go through the Java interface, which Java callers (e.g. MainActivity) see.
        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "UNCHECKED_CAST")
        val javaMap = map as java.util.Map<Int, String>
        try {
            javaMap.put(2, "b")
            throw AssertionError("put should be unsupported")
        } catch (expected: UnsupportedOperationException) {
            assertThat(map).containsExactly(1, "a")
        }
    }

    /** Many threads hitting a cold map at once: exactly one load, and every thread sees the same contents. */
    @Test
    fun concurrentFirstAccessLoadsOnceAndAgrees() {
        val loads = AtomicInteger()
        val map = LazyMap {
            loads.incrementAndGet()
            Thread.sleep(50) // widen the race window
            mapOf(1 to "bb876", 245 to "xb161")
        }
        val threads = 16
        val pool = Executors.newFixedThreadPool(threads)
        val start = CountDownLatch(1)
        try {
            val futures = (1..threads).map {
                pool.submit<String?> {
                    start.await()
                    map[245]
                }
            }
            start.countDown()
            val results = futures.map { it.get(5, TimeUnit.SECONDS) }

            assertThat(loads.get()).isEqualTo(1)
            assertThat(results.toSet()).containsExactly("xb161")
        } finally {
            pool.shutdownNow()
        }
    }
}
