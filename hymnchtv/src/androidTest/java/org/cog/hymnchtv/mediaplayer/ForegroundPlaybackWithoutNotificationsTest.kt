package org.cog.hymnchtv.mediaplayer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.MediaGuiController
import org.cog.hymnchtv.R
import org.cog.hymnchtv.ui.lyrics.LyricsTestBase
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * API 33+ with POST_NOTIFICATIONS denied: playback still runs in the foreground, only the notification is hidden.
 * Revoking the permission kills the app, so it is revoked before the run (see the plan); otherwise the test is skipped.
 */
@RunWith(AndroidJUnit4::class)
class ForegroundPlaybackWithoutNotificationsTest : LyricsTestBase() {
    private val wav = File(ctx.cacheDir, "fg-silence-60s.wav")

    @Before
    fun onlyWithoutThePermission() {
        assumeTrue(Build.VERSION.SDK_INT >= 33)
        assumeTrue(ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED)
    }

    @After
    fun deleteWav() {
        wav.delete()
    }

    @Test
    fun playbackRunsInTheForegroundWithoutAVisibleNotification() {
        launch().use { s ->
            val ctl = s.read { it.supportFragmentManager.findFragmentById(R.id.mediaPlayer) as MediaGuiController }
            s.onActivity { ctl.playUriForTest(ForegroundPlaybackSupport.silence(wav)) }
            try {
                s.await("playback to start", 15_000) { ctl.playerStateForTest() == 3 }
                ForegroundPlaybackSupport.awaitForeground(true, "the foreground service")
                assertThat(ForegroundPlaybackSupport.playbackNotification()).isNull()
            } finally {
                s.onActivity { ctl.stopPlay() }
            }
        }
    }
}
