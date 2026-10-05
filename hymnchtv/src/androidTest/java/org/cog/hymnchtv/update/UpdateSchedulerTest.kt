package org.cog.hymnchtv.update

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.service.androidupdate.DailyUpdateCheck
import org.cog.hymnchtv.service.androidupdate.UpdateCheckWorker
import org.cog.hymnchtv.service.androidupdate.UpdateScheduler
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** WorkManager scheduling of the update check (1.6.0) with the test WorkManager: nothing reaches the network. */
@RunWith(AndroidJUnit4::class)
class UpdateSchedulerTest {
    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        val config = Configuration.Builder().setExecutor(SynchronousExecutor()).setMinimumLoggingLevel(Log.DEBUG).build()
        WorkManagerTestInitHelper.initializeTestWorkManager(ctx, config)
    }

    private fun works(name: String): List<WorkInfo> = WorkManager.getInstance(ctx).getWorkInfosForUniqueWork(name).get()

    @Test
    fun theDailyCheckIsOnePeriodicWorkThatWaitsForTheNetwork() {
        UpdateScheduler.schedule(ctx)
        val daily = works(UpdateScheduler.DAILY).single()
        assertThat(daily.state).isEqualTo(WorkInfo.State.ENQUEUED)
        assertThat(daily.constraints.requiredNetworkType).isEqualTo(NetworkType.CONNECTED)
        assertThat(daily.periodicityInfo!!.repeatIntervalMillis).isEqualTo(TimeUnit.HOURS.toMillis(24))
        assertThat(daily.initialDelayMillis).isEqualTo(TimeUnit.HOURS.toMillis(24))
    }

    @Test
    fun schedulingAgainKeepsTheSameWorks() {
        UpdateScheduler.schedule(ctx)
        val daily = works(UpdateScheduler.DAILY).single().id
        val launch = works(UpdateScheduler.LAUNCH).single().id
        UpdateScheduler.schedule(ctx)
        assertThat(works(UpdateScheduler.DAILY).single().id).isEqualTo(daily)
        assertThat(works(UpdateScheduler.LAUNCH).single().id).isEqualTo(launch)
    }

    @Test
    fun theLaunchCheckRunsThirtySecondsLaterWithANetwork() {
        UpdateScheduler.schedule(ctx)
        val launch = works(UpdateScheduler.LAUNCH).single()
        assertThat(launch.constraints.requiredNetworkType).isEqualTo(NetworkType.CONNECTED)
        assertThat(launch.initialDelayMillis).isEqualTo(TimeUnit.SECONDS.toMillis(30))
    }

    private fun worker(check: DailyUpdateCheck): UpdateCheckWorker =
        TestListenableWorkerBuilder<UpdateCheckWorker>(ctx).setWorkerFactory(object : WorkerFactory() {
            override fun createWorker(appContext: android.content.Context, workerClassName: String, workerParameters: WorkerParameters) =
                UpdateCheckWorker(appContext, workerParameters, check)
        }).build()

    private class Source(private val fail: Boolean) : DailyUpdateCheck.Source {
        var ran = false
        override fun check(): String? {
            ran = true
            if (fail) throw IllegalStateException("boom")
            return null
        }
        override fun isForeground() = false
        override fun isLocked() = true
        override fun openUpdateDialog(claim: () -> Boolean) = Unit
    }

    @Test
    fun theWorkerRunsTheCheck() {
        val source = Source(fail = false)
        assertThat(worker(DailyUpdateCheck(source, { true }, AtomicBoolean())).doWork()).isEqualTo(ListenableWorker.Result.success())
        assertThat(source.ran).isTrue()
    }

    @Test
    fun anUnexpectedErrorEndsThisRunOnly() {
        val source = Source(fail = true)
        assertThat(worker(DailyUpdateCheck(source, { true }, AtomicBoolean())).doWork()).isEqualTo(ListenableWorker.Result.failure())
    }
}
