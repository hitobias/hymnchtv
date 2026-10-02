package org.cog.hymnchtv.concurrent

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class AppExecutorsTest {
    @Test
    fun runsOffTheMainThreadOnTheSharedIoThread() {
        val done = CountDownLatch(1)
        val threadName = AtomicReference<String>()
        val onMain = AtomicBoolean(true)
        AppExecutors.io("test") {
            threadName.set(Thread.currentThread().name)
            onMain.set(Looper.myLooper() == Looper.getMainLooper())
            done.countDown()
        }
        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(threadName.get()).isEqualTo("hymn-io")
        assertThat(onMain.get()).isFalse()
    }

    @Test
    fun aFailingTaskDoesNotStopLaterTasks() {
        AppExecutors.io("boom") { throw IllegalStateException("expected in test") }
        val done = CountDownLatch(1)
        AppExecutors.io("after") { done.countDown() }
        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun ioThenMainRunsWorkOnIoAndDeliversOnMain() {
        val done = CountDownLatch(1)
        val workThread = AtomicReference<String>()
        val result = AtomicReference<Int>()
        val deliveredOnMain = AtomicBoolean(false)
        AppExecutors.ioThenMain("test", { true }, {
            workThread.set(Thread.currentThread().name)
            42
        }) { value ->
            result.set(value)
            deliveredOnMain.set(Looper.myLooper() == Looper.getMainLooper())
            done.countDown()
        }
        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(workThread.get()).isEqualTo("hymn-io")
        assertThat(result.get()).isEqualTo(42)
        assertThat(deliveredOnMain.get()).isTrue()
    }

    @Test
    fun ioThenMainDropsTheResultWhenTheOwnerIsGone() {
        val alive = AtomicBoolean(true)
        val delivered = AtomicBoolean(false)
        val workDone = CountDownLatch(1)
        AppExecutors.ioThenMain("test-dead", { alive.get() }, {
            alive.set(false) // the screen is destroyed while the work runs
            workDone.countDown()
            1
        }) { delivered.set(true) }
        assertThat(workDone.await(5, TimeUnit.SECONDS)).isTrue()
        // later main-thread work proves the delivery post has been processed
        val flushed = CountDownLatch(1)
        AppExecutors.ioThenMain("flush", { true }, { 0 }) { flushed.countDown() }
        assertThat(flushed.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(delivered.get()).isFalse()
    }

    @Test
    fun ioThenMainSkipsDeliveryWhenTheWorkFailsAndKeepsTheThreadAlive() {
        val delivered = AtomicBoolean(false)
        AppExecutors.ioThenMain<Int>("test-fail", { true }, { throw IllegalStateException("expected in test") }) {
            delivered.set(true)
        }
        val flushed = CountDownLatch(1)
        AppExecutors.ioThenMain("flush", { true }, { 0 }) { flushed.countDown() }
        assertThat(flushed.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(delivered.get()).isFalse()
    }
}
