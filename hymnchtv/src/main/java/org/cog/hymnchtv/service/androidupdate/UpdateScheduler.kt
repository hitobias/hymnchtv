package org.cog.hymnchtv.service.androidupdate

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Schedules the update check with WorkManager (1.6.0). The alarm of 1.0-1.5 started an IntentService, which API 26+
 * refuses while the app is in the background, so the daily check only ran when the app happened to be open.
 * Two unique works, both waiting for a network: [DAILY] every 24 h, and [LAUNCH] 30 s after the app comes to the
 * foreground (the check the alarm did on launch).
 */
object UpdateScheduler {
    /** Unique work names; WorkManager persists them: never rename. */
    const val DAILY = "hymnal-update-daily"
    const val LAUNCH = "hymnal-update-launch"
    const val INTERVAL_HOURS = 24L
    const val LAUNCH_DELAY_SECONDS = 30L

    /** The 1.0-1.5 alarm target (OnlineUpdateService, removed in 1.6.0), cancelled once an update installed 1.6. */
    private const val LEGACY_SERVICE = "org.cog.hymnchtv.service.androidupdate.OnlineUpdateService"
    private const val LEGACY_ACTION = "org.cog.hymnchtv.ACTION_AUTO_UPDATE_APP"

    private fun network(): Constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    @JvmStatic
    fun dailyRequest(): PeriodicWorkRequest =
        PeriodicWorkRequestBuilder<UpdateCheckWorker>(INTERVAL_HOURS, TimeUnit.HOURS)
            // the first run is the launch work 30 s after start: do not fetch twice at the first launch
            .setInitialDelay(INTERVAL_HOURS, TimeUnit.HOURS)
            .setConstraints(network())
            .build()

    @JvmStatic
    fun launchRequest(): OneTimeWorkRequest =
        OneTimeWorkRequestBuilder<UpdateCheckWorker>()
            .setInitialDelay(LAUNCH_DELAY_SECONDS, TimeUnit.SECONDS)
            .setConstraints(network())
            .build()

    /** Call when the app comes to the foreground (HymnsApp). Existing works are kept, so a second call changes nothing. */
    @JvmStatic
    fun schedule(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.enqueueUniquePeriodicWork(DAILY, ExistingPeriodicWorkPolicy.KEEP, dailyRequest())
        workManager.enqueueUniqueWork(LAUNCH, ExistingWorkPolicy.KEEP, launchRequest())
        cancelLegacyAlarm(context)
    }

    private fun cancelLegacyAlarm(context: Context) {
        val intent = Intent(LEGACY_ACTION).setComponent(ComponentName(context.packageName, LEGACY_SERVICE))
        val pending = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            ?: return
        (context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)?.cancel(pending)
        pending.cancel()
    }
}
