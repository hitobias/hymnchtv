package org.cog.hymnchtv.mediaconfig

import android.database.DatabaseUtils
import androidx.lifecycle.Observer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.persistance.DatabaseBackend
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** The url import runs without any Activity, rejects a second start, and clears its running flag. */
@RunWith(AndroidJUnit4::class)
class UrlImportJobTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val ctx = instrumentation.targetContext
    private lateinit var db: DatabaseBackend

    @Before
    fun setUp() {
        ctx.deleteDatabase(NAME)
        db = DatabaseBackend.createForTest(ctx, NAME)
    }

    @After
    fun tearDown() {
        db.close()
        ctx.deleteDatabase(NAME)
    }

    @Test
    fun runsWithoutAnActivityAndClearsTheRunningFlag() {
        val firstStarted = AtomicBoolean(false)
        val secondStarted = AtomicBoolean(true)
        val finished = CountDownLatch(1)
        val observer = Observer<Boolean> { running -> if (running == false) finished.countDown() }

        instrumentation.runOnMainSync {
            firstStarted.set(MediaConfig.startUrlImport(db, "url_import.txt", null, false))
            secondStarted.set(MediaConfig.startUrlImport(db, "url_import.txt", null, false))
            MediaConfig.urlImportRunning().observeForever(observer)
        }
        try {
            assertThat(firstStarted.get()).isTrue()
            assertThat(secondStarted.get()).isFalse()
            assertThat(finished.await(60, TimeUnit.SECONDS)).isTrue()
            assertThat(DatabaseUtils.queryNumEntries(db.readableDatabase, MainActivity.HYMN_DB)).isGreaterThan(0L)
        } finally {
            instrumentation.runOnMainSync { MediaConfig.urlImportRunning().removeObserver(observer) }
        }
    }

    @Test
    fun missingFileStillClearsTheRunningFlag() {
        val finished = CountDownLatch(1)
        val observer = Observer<Boolean> { running -> if (running == false) finished.countDown() }
        instrumentation.runOnMainSync {
            MediaConfig.startUrlImport(db, null, "/does/not/exist.txt", false)
            MediaConfig.urlImportRunning().observeForever(observer)
        }
        try {
            assertThat(finished.await(10, TimeUnit.SECONDS)).isTrue()
        } finally {
            instrumentation.runOnMainSync { MediaConfig.urlImportRunning().removeObserver(observer) }
        }
    }

    private companion object {
        const val NAME = "test-url-import-job.db"
    }
}
