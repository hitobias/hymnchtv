package org.cog.hymnchtv.ui.lyrics

import android.net.Uri
import android.os.SystemClock
import android.view.View
import androidx.core.net.toUri
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.viewpager2.widget.ViewPager2
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.ContentHandler
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.MediaGuiController
import org.cog.hymnchtv.R
import org.cog.hymnchtv.hymn.HymnRef
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
            s.revealChrome()
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
            val c = s.hostPoint(0.9f, 0.5f)
            val w = s.read { it.findViewById<ViewPager2>(R.id.viewPager).width }
            drag(c[0], c[1], -(w * 17 / 20), 0)   // long enough to pass the half-page mark even after the pager takes over late
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

    /** Polls on the main thread without the scenario: on plan path B the launched page is gone after a cross-book jump. */
    private fun awaitOnMain(what: String, timeoutMs: Long = 15_000, condition: () -> Boolean) {
        val end = SystemClock.uptimeMillis() + timeoutMs
        while (true) {
            var ok = false
            instrumentation.runOnMainSync { ok = condition() }
            if (ok) return
            check(SystemClock.uptimeMillis() < end) { "timed out waiting for $what" }
            SystemClock.sleep(50)
        }
    }

    private fun finishLyricsPages() = instrumentation.runOnMainSync {
        ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
            .filterIsInstance<ContentHandler>().forEach { it.finish() }
    }

    private fun resumedLyricsPage(): ContentHandler? = ActivityLifecycleMonitorRegistry.getInstance()
        .getActivitiesInStage(Stage.RESUMED).filterIsInstance<ContentHandler>().singleOrNull()

    /** The bar of the lyrics page in front (path A: the same page; path B: the page the jump opened) is reset. */
    private fun assertFrontBarIsReset(expected: HymnRef) {
        awaitOnMain("$expected in front") { resumedLyricsPage()?.currentRef() == expected }
        var position = ""
        var progress = -1
        var play = ""
        var expectedPlay = ""
        instrumentation.runOnMainSync {
            val a = resumedLyricsPage()!!
            position = a.findViewById<android.widget.TextView>(R.id.playback_position).text.toString()
            progress = a.findViewById<android.widget.SeekBar>(R.id.playback_seekbar).progress
            play = a.findViewById<View>(R.id.playback_play).contentDescription.toString()
            expectedPlay = a.getString(R.string.c_play)
        }
        assertThat(position).isEqualTo("00:00")
        assertThat(progress).isEqualTo(0)
        assertThat(play).isEqualTo(expectedPlay)
    }

    @Test
    fun jumpingWithinTheBookStopsPlaybackAndResetsTheBar() {
        launch().use { s ->
            val ctl = playing(s)
            val target = HymnRef(MainActivity.HYMN_DB, 100)
            s.onActivity { it.onJump(target) }
            s.await("the jump target") { it.currentRef() == target }
            awaitState(s, ctl, STOP, "playback to stop")
            assertBarIsReset(s, ctl)
        }
    }

    @Test
    fun jumpingToAnotherBookStopsPlaybackAndResetsTheBar() {
        launch().use { s ->
            val ctl = playing(s)
            val target = HymnRef(MainActivity.HYMN_BB, 37)
            s.onActivity { it.onJump(target) }
            awaitOnMain("playback to stop") { ctl.playerStateForTest() == STOP }
            assertFrontBarIsReset(target)
        }
        finishLyricsPages()
    }

    @Test
    fun backToThePreviousHymnStopsPlaybackBeforeAnythingElse() {
        launch().use { s ->
            val target = HymnRef(MainActivity.HYMN_DB, 100)
            s.onActivity { it.onJump(target) }
            s.await("the jump target") { it.currentRef() == target }
            s.awaitPage()
            val ctl = playing(s)
            // one back press: returns to DB 5 (and stops by the hymn-change rule), it does not merely stop the audio
            s.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            s.await("the previous hymn") { it.currentRef() == HymnRef(MainActivity.HYMN_DB, 5) }
            awaitState(s, ctl, STOP, "playback to stop")
            assertBarIsReset(s, ctl)
        }
    }

    private companion object {
        const val STOP = 0
        const val PLAY = 3
    }
}
