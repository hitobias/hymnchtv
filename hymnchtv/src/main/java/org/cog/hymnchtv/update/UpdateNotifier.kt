package org.cog.hymnchtv.update

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.cog.hymnchtv.R
import org.cog.hymnchtv.service.androidnotification.NotificationHelper

/** "Update ready" notification; tapping it opens [UpdateInstallActivity] (never started from a receiver). */
object UpdateNotifier {
    private const val TAG = "hymnal_update_ready"
    private const val ID = 2

    /** @return false when notifications are not permitted, so the caller can fall back to a toast. */
    @JvmStatic
    @SuppressLint("MissingPermission")
    fun showReady(context: Context, apkName: String, versionName: String): Boolean {
        val permitted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val manager = NotificationManagerCompat.from(context)
        if (!permitted || !manager.areNotificationsEnabled()) return false
        val pending = PendingIntent.getActivity(
            context, 0, UpdateInstallActivity.intent(context, apkName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, NotificationHelper.DEFAULT_GROUP)
            .setSmallIcon(R.drawable.hymnchtv)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.update_ready_to_install, versionName))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        manager.notify(TAG, ID, notification)
        return true
    }

    @JvmStatic
    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(TAG, ID)
    }
}
