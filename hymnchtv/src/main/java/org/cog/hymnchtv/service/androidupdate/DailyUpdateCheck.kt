package org.cog.hymnchtv.service.androidupdate

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.concurrent.AppExecutors
import org.cog.hymnchtv.service.androidnotification.NotificationHelper
import org.cog.hymnchtv.update.UpdateInstallActivity
import org.cog.hymnchtv.utils.DeviceLock
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.BooleanSupplier

/**
 * One update check, run by [UpdateCheckWorker] (moved from OnlineUpdateService.checkAppUpdate in 1.6.0). When a newer
 * release is confirmed it posts the "update available" notification and, at most once per process, opens the update dialog
 * if the app is in front of an unlocked device ([UpdatePromptPolicy]).
 */
class DailyUpdateCheck(
    private val source: Source,
    private val notifier: Notifier,
    private val prompted: AtomicBoolean = PROMPTED,
) {
    interface Source {
        /**
         * The one network check of this run (UpdateServiceImpl.checkInBackground): the notification text when a newer,
         * checksum-published release is confirmed, else null. The dialog reuses its result; nothing is fetched twice.
         */
        fun check(): String?

        fun isForeground(): Boolean

        fun isLocked(): Boolean

        /** Opens the dialog for the release [check] found, on the main thread, if an activity is still resumed then. */
        fun openUpdateDialog()
    }

    fun interface Notifier {
        /** @return false when notifications are not permitted (API 33+) or switched off */
        fun showAvailable(text: String): Boolean
    }

    fun run() {
        val text = source.check() ?: return
        notifier.showAvailable(text)
        if (UpdatePromptPolicy.shouldPrompt(prompted.get(), source.isForeground(), BooleanSupplier { source.isLocked() }) &&
            prompted.compareAndSet(false, true)
        ) {
            source.openUpdateDialog()
        }
    }

    companion object {
        /** The dialog opens by itself at most once per process (OnlineUpdateService.updateNotified before 1.6.0). */
        private val PROMPTED = AtomicBoolean(false)
        private const val NOTIFY_TAG = "hymnal_update_available"
        private const val NOTIFY_ID = 1

        @JvmStatic
        fun forApp(context: Context): DailyUpdateCheck = DailyUpdateCheck(AppSource(context), AppNotifier(context))
    }

    private class AppSource(private val context: Context) : Source {
        private val service: UpdateServiceImpl get() = UpdateServiceImpl.getInstance()
        override fun check(): String? = service.checkInBackground()
        override fun isForeground(): Boolean = MainActivity.isForeground
        override fun isLocked(): Boolean = DeviceLock.isLocked(context)

        /** The worker thread decided; the main thread checks again that an activity is resumed and the device unlocked. */
        override fun openUpdateDialog() {
            AppExecutors.MAIN.post {
                val resumed = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                if (resumed && MainActivity.isForeground && !DeviceLock.isLocked(context)) service.offerLatest()
            }
        }
    }

    private class AppNotifier(private val context: Context) : Notifier {
        @SuppressLint("MissingPermission")
        override fun showAvailable(text: String): Boolean {
            val permitted = Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            val manager = NotificationManagerCompat.from(context)
            if (!permitted || !manager.areNotificationsEnabled()) return false
            // Opens a foreground activity that runs the check: a notification must not start a service that starts activities
            val pending = PendingIntent.getActivity(
                context, 0, UpdateInstallActivity.checkIntent(context), NotificationHelper.getPendingIntentFlag(false, true),
            )
            val notification = NotificationCompat.Builder(context, NotificationHelper.DEFAULT_GROUP)
                .setSmallIcon(R.drawable.hymnchtv)
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setTicker(text)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(text)
                .setContentIntent(pending)
                .build()
            manager.notify(NOTIFY_TAG, NOTIFY_ID, notification)
            return true
        }
    }
}
