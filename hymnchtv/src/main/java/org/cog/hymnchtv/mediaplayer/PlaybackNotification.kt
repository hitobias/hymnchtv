package org.cog.hymnchtv.mediaplayer

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import org.cog.hymnchtv.R
import org.cog.hymnchtv.service.androidnotification.NotificationHelper
import androidx.media.app.NotificationCompat as MediaNotificationCompat

/**
 * The notification of foreground playback (1.6.0): play/pause and stop, also on the lock screen; a tap brings the app back
 * as it was. Posted on the silent default channel. Without the notification permission (API 33+) the system hides it, but
 * the service still runs in the foreground and the music goes on.
 */
object PlaybackNotification {
    /** 1 and 2 are the update notifications (DailyUpdateCheck, UpdateNotifier). */
    const val ID = 3
    private const val REQUEST_TOGGLE = 31
    private const val REQUEST_STOP = 32
    private const val REQUEST_OPEN = 33

    @JvmStatic
    fun build(context: Context, playing: Boolean): Notification {
        val builder = NotificationCompat.Builder(context, NotificationHelper.DEFAULT_GROUP)
            .setSmallIcon(R.drawable.hymnchtv)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(if (playing) R.string.playback_notify_playing else R.string.playback_notify_paused))
            .setOngoing(playing)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(
                if (playing) R.drawable.ic_sym_pause else R.drawable.ic_sym_play_arrow,
                context.getString(if (playing) R.string.playback_notify_pause else R.string.playback_notify_play),
                serviceAction(context, AudioBgService.ACTION_NOTIFY_TOGGLE, REQUEST_TOGGLE),
            )
            .addAction(
                R.drawable.ic_sym_stop,
                context.getString(R.string.playback_notify_stop),
                serviceAction(context, AudioBgService.ACTION_NOTIFY_STOP, REQUEST_STOP),
            )
            .setStyle(MediaNotificationCompat.MediaStyle().setShowActionsInCompactView(0, 1))
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { launch ->
            // like a launcher tap: the task comes back with the lyrics page on top
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            builder.setContentIntent(
                PendingIntent.getActivity(context, REQUEST_OPEN, launch, NotificationHelper.getPendingIntentFlag(false, true)),
            )
        }
        return builder.build()
    }

    private fun serviceAction(context: Context, action: String, request: Int): PendingIntent =
        PendingIntent.getService(
            context, request, Intent(context, AudioBgService::class.java).setAction(action),
            NotificationHelper.getPendingIntentFlag(false, true),
        )
}
