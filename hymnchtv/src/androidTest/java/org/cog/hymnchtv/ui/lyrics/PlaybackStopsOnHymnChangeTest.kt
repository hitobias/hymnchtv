package org.cog.hymnchtv.ui.lyrics

import android.net.Uri
import android.view.View
import androidx.core.net.toUri
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.viewpager2.widget.ViewPager2
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.ContentHandler
import org.cog.hymnchtv.MediaGuiController
import org.cog.hymnchtv.R
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Moving to another hymn (next button, swipe) stops what is playing and resets the bar; auto-next is the only exception. */
@RunWith(AndroidJUnit4::class)
class PlaybackStopsOnHymnChangeTest : LyricsTestBase() {
    private val wav = File(ctx.cacheDir, "silence-60s.wav")

    @After
    fun deleteWav() {
        wav.delete()
    }

    /** 60 s of 8 kHz 16-bit mono silence: long enough that it cannot finish by itself during the test. */
    private fun silence(): Uri {
        val rate = 8000
        val data = ByteArray(rate * 2 * 60)
        val out = ByteArrayOutputStream()
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + data.size); put("WAVE".toByteArray())
            put("fmt ".toByteArray()); putInt(16); putShort(1); putShort(1); putInt(rate); putInt(rate * 2); putShort(2); putShort(16)
            put("data".toByteArray()); putInt(data.size)
        }
        out.write(header.array())
        out.write(data)
        wav.writeBytes(out.toByteArray())
        return wav.toUri()
    }

    private fun controller(s: ActivityScenario<ContentHandler>): MediaGuiController =
        s.read { it.supportFragmentManager.findFragmentById(R.id.mediaPlayer) as MediaGuiController }

    private fun playing(s: ActivityScenario<ContentHandler>): MediaGuiController {
        val ctl = controller(s)
        s.onActivity { ctl.playUriForTest(silence()) }
        awaitState(s, ctl, PLAY, "playback to start")
        return ctl
    }

    private fun awaitState(s: ActivityScenario<ContentHandler>, ctl: MediaGuiController, state: Int, what: String) {
        s.await(what, 15_000) { ctl.playerStateForTest() == state }
    }

    private fun assertBarIsReset(s: ActivityScenario<ContentHandler>, ctl: MediaGuiController) {
        s.onActivity {
            assertThat(it.findViewById<android.widget.TextView>(R.id.playback_position).text.toString()).isEqualTo("00:00")
            assertThat(it.findViewById<android.widget.SeekBar>(R.id.playback_seekbar).progress).isEqualTo(0)
            assertThat(it.findViewById<View>(R.id.playback_play).contentDescription.toString()).isEqualTo(it.getString(R.string.c_play))
        }
        assertThat(ctl.playerStateForTest()).isEqualTo(STOP)
    }

    @Test
    fun nextButtonStopsPlaybackAndResetsTheBar() {
        launch().use { s ->
            s.onActivity { it.setChromeHeld(true) }
            val ctl = playing(s)
            val before = s.item()
            onView(withId(R.id.btn_next)).perform(click())
            s.await("the next page", 10_000) { it.findViewById<ViewPager2>(R.id.viewPager).currentItem == before + 1 }
            awaitState(s, ctl, STOP, "playback to stop")
            assertBarIsReset(s, ctl)
        }
    }

    @Test
    fun swipingToAnotherHymnStopsPlayback() {
        launch().use { s ->
            val ctl = playing(s)
            val before = s.item()
            val c = s.hostPoint(0.75f, 0.5f)
            val w = s.read { it.findViewById<ViewPager2>(R.id.viewPager).width }
            drag(c[0], c[1], -w / 2, 0)
            s.await("the page to turn", 10_000) { it.findViewById<ViewPager2>(R.id.viewPager).currentItem == before + 1 }
            awaitState(s, ctl, STOP, "playback to stop")
            assertBarIsReset(s, ctl)
        }
    }

    @Test
    fun staysOnTheSameHymnWhilePlayingKeepsPlaying() {
        launch().use { s ->
            val ctl = playing(s)
            Thread.sleep(1500)
            assertThat(ctl.playerStateForTest()).isEqualTo(PLAY)
            ctl.stopPlay()
        }
    }

    private companion object {
        const val STOP = 0
        const val PLAY = 3
    }
}
