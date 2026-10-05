package org.cog.hymnchtv.service.androidupdate

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import timber.log.Timber

/**
 * Runs [DailyUpdateCheck] for [UpdateScheduler] on WorkManager's thread. WorkManager stores this class name with every
 * scheduled work: never rename or move it (proguard-rules.pro keeps the name for R8).
 */
class UpdateCheckWorker @JvmOverloads constructor(
    context: Context,
    params: WorkerParameters,
    private val check: DailyUpdateCheck = DailyUpdateCheck.forApp(context.applicationContext),
) : Worker(context, params) {
    override fun doWork(): Result = try {
        check.run()
        Result.success()
    } catch (e: RuntimeException) {
        Timber.w(e, "Update check failed")
        Result.failure()
    }
}
