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
}
