package org.cog.hymnchtv.mediaplayer

import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.ContentHandler
import org.cog.hymnchtv.MediaGuiController
import org.cog.hymnchtv.R
import org.cog.hymnchtv.mediaplayer.ForegroundPlaybackSupport.awaitForeground
import org.cog.hymnchtv.mediaplayer.ForegroundPlaybackSupport.notificationKey
import org.cog.hymnchtv.mediaplayer.ForegroundPlaybackSupport.playbackNotification
import org.cog.hymnchtv.mediaplayer.ForegroundPlaybackSupport.serviceForeground
import org.cog.hymnchtv.mediaplayer.ForegroundPlaybackSupport.shell
import org.cog.hymnchtv.ui.lyrics.LyricsTestBase
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** 1.6.0: playback runs in the foreground with a notification, survives the screen going off, and obeys the notification keys. */
@RunWith(AndroidJUnit4::class)
class ForegroundPlaybackTest : LyricsTestBase() {
    private val wav = File(ctx.cacheDir, "fg-silence-60s.wav")

    @After
    fun deleteWav() {
        wav.delete()
    }

    private fun controller(s: ActivityScenario<ContentHandler>): MediaGuiController =
        s.read { it.supportFragmentManager.findFragmentById(R.id.mediaPlayer) as MediaGuiController }

    private fun playing(s: ActivityScenario<ContentHandler>): MediaGuiController {
        val ctl = controller(s)
        s.onActivity { ctl.playUriForTest(ForegroundPlaybackSupport.silence(wav)) }
        s.await("playback to start", 15_000) { ctl.playerStateForTest() == PLAY }
        return ctl
    }

    private fun stop(s: ActivityScenario<ContentHandler>, ctl: MediaGuiController) {
        s.onActivity { ctl.stopPlay() }
    }

    private fun notificationsShown() = NotificationManagerCompat.from(ctx).areNotificationsEnabled()

    @Test
    fun playingRunsInTheForegroundWithItsNotification() {
        launch().use { s ->
            val ctl = playing(s)
            try {
                awaitForeground(true, "the foreground service")
                if (notificationsShown()) {
                    val n = playbackNotification()
                    assertThat(n).isNotNull()
                    assertThat(n!!.actions[0].title.toString()).isEqualTo(ctx.getString(R.string.playback_notify_pause))
                }
            } finally {
                stop(s, ctl)
            }
            // a normal stop from the player card ends the service itself, not only its notification
            awaitForeground(null, "the service to stop after a normal stop")
            assertThat(playbackNotification()).isNull()
        }
    }

    @Test
    fun theNotificationKeyPausesAndResumes() {
        launch().use { s ->
            val ctl = playing(s)
            try {
                awaitForeground(true, "the foreground service")
                SystemClock.sleep(1_000)   // a position above 0, so the pause counts as "paused midway"
                notificationKey(AudioBgService.ACTION_NOTIFY_TOGGLE)
                s.await("pause from the notification", 10_000) { ctl.playerStateForTest() == PAUSE }
                // paused midway: still in the foreground, so the notification can resume it
                awaitForeground(true, "staying in the foreground while paused")
                if (notificationsShown()) {
                    awaitPlayKey()
                }
                notificationKey(AudioBgService.ACTION_NOTIFY_TOGGLE)
                s.await("resume from the notification", 10_000) { ctl.playerStateForTest() == PLAY }
                assertThat(AudioBgService.anyPlayingForTest()).isTrue()
            } finally {
                stop(s, ctl)
            }
        }
    }

    private fun awaitPlayKey() {
        val end = SystemClock.uptimeMillis() + 5_000
        while (playbackNotification()?.actions?.get(0)?.title?.toString() != ctx.getString(R.string.playback_notify_play)) {
            check(SystemClock.uptimeMillis() < end) { "the notification never showed the play key" }
            SystemClock.sleep(100)
        }
    }

    @Test
    fun theStopKeyEndsPlaybackAndTheService() {
        launch().use { s ->
            val ctl = playing(s)
            awaitForeground(true, "the foreground service")
            notificationKey(AudioBgService.ACTION_NOTIFY_STOP)
            s.await("stop from the notification", 10_000) { ctl.playerStateForTest() == STOP }
            awaitForeground(null, "the service to stop")
            assertThat(AudioBgService.anyPlayingForTest()).isFalse()
        }
    }

    @Test
    fun keepsPlayingWhenTheScreenGoesOffAndTheAppIdles() {
        launch().use { s ->
            val ctl = playing(s)
            try {
                awaitForeground(true, "the foreground service")
                // the screen goes off: onPause/onStop without onUserLeaveHint (Home or moving another activity in front
                // would stop playback by design, so ActivityScenario.moveToState cannot be used here)
                shell("input keyevent KEYCODE_SLEEP")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // what the system does a minute after the app left the screen: background services are stopped
                    shell("am make-uid-idle ${ctx.packageName}")
                }
                SystemClock.sleep(3_000)
                assertThat(AudioBgService.anyPlayingForTest()).isTrue()
                assertThat(serviceForeground()).isTrue()
            } finally {
                shell("input keyevent KEYCODE_WAKEUP")
                shell("wm dismiss-keyguard")
                stop(s, ctl)
            }
        }
    }

    private companion object {
        const val STOP = 0
        const val PAUSE = 2
        const val PLAY = 3
    }
}
