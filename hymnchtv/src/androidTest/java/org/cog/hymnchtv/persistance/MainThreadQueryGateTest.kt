package org.cog.hymnchtv.persistance

import android.os.Handler
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.persistance.room.HymnchtvDatabase
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * The permanent main-thread gate (DatabaseBackend class doc): every database built by HymnchtvDatabase (the app
 * database and the test databases alike) refuses queries on the main thread and still serves other threads.
 */
@RunWith(AndroidJUnit4::class)
class MainThreadQueryGateTest {
    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun databaseRejectsMainThreadQueriesButServesOtherThreads() {
        val name = "gate-test.db"
        ctx.deleteDatabase(name)
        val db = HymnchtvDatabase.build(ctx, name)
        try {
            assertThat(db.mediaRecordDao()).isNotNull()
            // Worker thread (this instrumentation thread): allowed.
            db.query("SELECT 1", null).use { assertThat(it.moveToFirst()).isTrue() }

            val onMain = AtomicReference<Throwable?>()
            val done = CountDownLatch(1)
            Handler(Looper.getMainLooper()).post {
                try {
                    db.query("SELECT 1", null).close()
                } catch (t: Throwable) {
                    onMain.set(t)
                } finally {
                    done.countDown()
                }
            }
            assertThat(done.await(10, TimeUnit.SECONDS)).isTrue()
            assertThat(onMain.get()).isInstanceOf(IllegalStateException::class.java)
        } finally {
            db.close()
            ctx.deleteDatabase(name)
        }
    }
}
