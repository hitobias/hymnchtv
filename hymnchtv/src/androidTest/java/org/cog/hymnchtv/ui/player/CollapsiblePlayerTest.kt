package org.cog.hymnchtv.ui.player

import org.cog.hymnchtv.QuickTest
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.View
import android.widget.ScrollView
import androidx.core.net.toUri
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.viewpager2.widget.ViewPager2
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.ContentHandler
import org.cog.hymnchtv.ContentView
import org.cog.hymnchtv.HymnsApp
import org.cog.hymnchtv.MediaGuiController
import org.cog.hymnchtv.R
import org.cog.hymnchtv.ui.lyrics.LyricsTestBase
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** The collapsible player: card to capsule and back, from buttons, drags, rotation, the overflow switch and playback. */
@RunWith(AndroidJUnit4::class)
class CollapsiblePlayerTest : LyricsTestBase() {
    private val wav = File(ctx.cacheDir, "player-silence-60s.wav")

    @After
    fun cleanUp() {
        wav.delete()
    }

    private fun silence(): Uri {
        val rate = 8000
        val data = ByteArray(rate * 2 * 60)
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + data.size); put("WAVE".toByteArray())
            put("fmt ".toByteArray()); putInt(16); putShort(1); putShort(1); putInt(rate); putInt(rate * 2); putShort(2); putShort(16)
            put("data".toByteArray()); putInt(data.size)
        }
        wav.writeBytes(header.array() + data)
        return wav.toUri()
    }

    private fun ActivityScenario<ContentHandler>.id(id: Int): View = read { it.findViewById<View>(id) }
    private fun ActivityScenario<ContentHandler>.vis(id: Int): Int = read { it.findViewById<View>(id).visibility }
    private fun ActivityScenario<ContentHandler>.click(id: Int) = onActivity { it.findViewById<View>(id).performClick() }
    private fun ActivityScenario<ContentHandler>.guiController() =
        read { it.supportFragmentManager.findFragmentById(R.id.mediaPlayer) as MediaGuiController }

    private fun ActivityScenario<ContentHandler>.awaitDisplay(what: String, condition: (ContentHandler) -> Boolean) {
        try {
            await(what, 10_000, condition)
        } catch (e: IllegalStateException) {
            throw IllegalStateException(e.message + read {
                val c = it.findViewById<View>(R.id.playerUi)
                val k = it.findViewById<View>(R.id.playerCapsule)
                " [capsule=${k.visibility} a=${k.alpha} card=${c.visibility} a=${c.alpha} sx=${c.scaleX} " +
                    "rendered=${it.playerSheet.rendered} state=${it.playerSheet.state} portrait=${HymnsApp.isPortrait} " +
                    "orientation=${it.resources.configuration.orientation}]"
            }, e)
        }
    }

    private fun ActivityScenario<ContentHandler>.awaitCollapsed() = awaitDisplay("the capsule") {
        it.findViewById<View>(R.id.playerCapsule).let { c -> c.visibility == View.VISIBLE && c.alpha == 1f } &&
            it.findViewById<View>(R.id.playerUi).visibility == View.GONE
    }

    private fun ActivityScenario<ContentHandler>.awaitExpanded() = awaitDisplay("the card") {
        it.findViewById<View>(R.id.playerCapsule).visibility == View.GONE &&
            it.findViewById<View>(R.id.playerUi).let { c -> c.visibility == View.VISIBLE && c.alpha == 1f && c.scaleX == 1f }
    }

    private fun ActivityScenario<ContentHandler>.awaitNothing() = awaitDisplay("nothing shown") {
        it.findViewById<View>(R.id.playerCapsule).visibility == View.GONE && it.findViewById<View>(R.id.playerUi).visibility == View.GONE
    }

    private fun centre(s: ActivityScenario<ContentHandler>, id: Int, fy: Float = 0.5f): IntArray = s.read {
        val v = it.findViewById<View>(id)
        val loc = IntArray(2)
        v.getLocationOnScreen(loc)
        intArrayOf(loc[0] + v.width / 2, loc[1] + (v.height * fy).toInt())
    }

    private fun rotate(s: ActivityScenario<ContentHandler>, landscape: Boolean) {
        s.onActivity {
            it.requestedOrientation = if (landscape) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        s.await("rotation", 15_000) { HymnsApp.isPortrait != landscape && it.resources.configuration.orientation ==
            (if (landscape) android.content.res.Configuration.ORIENTATION_LANDSCAPE else android.content.res.Configuration.ORIENTATION_PORTRAIT) }
        s.awaitPage()
        Thread.sleep(500)
    }

    private fun playing(s: ActivityScenario<ContentHandler>): MediaGuiController {
        val ctl = s.guiController()
        s.onActivity { ctl.playUriForTest(silence()) }
        s.await("playback", 15_000) { ctl.playerStateForTest() == PLAY }
        return ctl
    }

    /** A cold open shows the capsule; tests that work on the card expand it explicitly first. */
    private fun launchExpanded(): ActivityScenario<ContentHandler> = launch().also {
        it.awaitCollapsed()
        it.onActivity { a -> a.playerSheet.expand(true) }
        it.awaitExpanded()
    }

    @Test
    @QuickTest
    fun coldOpenStartsAsTheCapsule() {
        launch().use { s ->
            s.awaitCollapsed()
            assertThat(s.vis(R.id.playerUi)).isEqualTo(View.GONE)
            assertThat(s.vis(R.id.playerCapsule)).isEqualTo(View.VISIBLE)
            assertThat(s.vis(R.id.capsuleNote)).isEqualTo(View.VISIBLE)
        }
    }

    @Test
    @QuickTest
    fun expandedCardStartsWithTheHandleAndSurvivesRecreation() {
        launchExpanded().use { s ->
            assertThat(s.vis(R.id.sheet_handle)).isEqualTo(View.VISIBLE)
            s.recreate()
            s.awaitPage()
            s.awaitExpanded()
        }
    }

    @Test
    @QuickTest
    fun collapseButtonShowsTheCapsuleAndNoteExpands() {
        launchExpanded().use { s ->
            s.click(R.id.btn_player_collapse)
            s.awaitCollapsed()
            assertThat(s.vis(R.id.capsuleNote)).isEqualTo(View.VISIBLE)
            assertThat(s.vis(R.id.capsulePlay)).isEqualTo(View.GONE)
            s.click(R.id.capsuleNote)
            s.awaitExpanded()
        }
    }

    @Test
    fun capsuleTapOnTheNoteWithARealTouchExpands() {
        launchExpanded().use { s ->
            s.click(R.id.btn_player_collapse)
            s.awaitCollapsed()
            val c = centre(s, R.id.capsuleNote)
            tap(c[0], c[1])
            s.awaitExpanded()
        }
    }

    @Test
    @QuickTest
    fun playingCapsuleHasPlayKeyAndExpandArea() {
        launchExpanded().use { s ->
            val ctl = playing(s)
            s.click(R.id.btn_player_collapse)
            s.awaitCollapsed()
            assertThat(s.vis(R.id.capsulePlay)).isEqualTo(View.VISIBLE)
            assertThat(s.vis(R.id.capsuleExpand)).isEqualTo(View.VISIBLE)
            assertThat(s.vis(R.id.capsuleNote)).isEqualTo(View.GONE)
            val info = s.read { it.findViewById<android.widget.TextView>(R.id.hymn_info).text.toString() }
            s.await("the pause label") { it.findViewById<View>(R.id.capsulePlay).contentDescription?.toString() == it.getString(R.string.c_player_pause_named, info) }
            // The key plays and pauses with a real touch
            val key = centre(s, R.id.capsulePlay)
            tap(key[0], key[1])
            s.await("pause", 10_000) { ctl.playerStateForTest() == PAUSE }
            s.await("the play label") { it.findViewById<View>(R.id.capsulePlay).contentDescription?.toString() == it.getString(R.string.c_player_play_named, info) }
            assertThat(s.vis(R.id.playerCapsule)).isEqualTo(View.VISIBLE)   // still the capsule
            tap(key[0], key[1])
            s.await("play", 10_000) { ctl.playerStateForTest() == PLAY }
            val arrow = centre(s, R.id.capsuleExpand)
            tap(arrow[0], arrow[1])
            s.awaitExpanded()
            ctl.stopPlay()
        }
    }

    @Test
    fun dragFromTheHandleCollapses() {
        launchExpanded().use { s ->
            val h = centre(s, R.id.sheet_handle)
            val height = s.read { it.findViewById<View>(R.id.playerUi).height }
            drag(h[0], h[1], 0, height / 2)
            s.awaitCollapsed()
        }
    }

    @Test
    fun shortDragSpringsBack() {
        launchExpanded().use { s ->
            val h = centre(s, R.id.sheet_handle)
            val height = s.read { it.findViewById<View>(R.id.playerUi).height }
            drag(h[0], h[1], 0, height / 8)
            s.awaitExpanded()
        }
    }

    @Test
    fun dragFromTheProgressBarDoesNotCollapse() {
        launchExpanded().use { s ->
            val bar = centre(s, R.id.playback_seekbar)
            val height = s.read { it.findViewById<View>(R.id.playerUi).height }
            drag(bar[0], bar[1], 0, height / 2)
            assertThat(s.vis(R.id.playerUi)).isEqualTo(View.VISIBLE)
            assertThat(s.vis(R.id.playerCapsule)).isEqualTo(View.GONE)
        }
    }

    @Test
    fun upwardDragOnTheCapsuleExpands() {
        launchExpanded().use { s ->
            s.click(R.id.btn_player_collapse)
            s.awaitCollapsed()
            val c = centre(s, R.id.playerCapsule)
            drag(c[0], c[1], 0, -dp(120))
            s.awaitExpanded()
        }
    }

    @Test
    fun changingHymnKeepsTheCapsuleAndTurnsItIntoTheNote() {
        launchExpanded().use { s ->
            s.onActivity { it.setChromeHeld(true) }
            val ctl = playing(s)
            s.click(R.id.btn_player_collapse)
            s.awaitCollapsed()
            assertThat(s.vis(R.id.capsulePlay)).isEqualTo(View.VISIBLE)
            val before = s.item()
            s.onActivity { page(it)!!.findViewById<View>(R.id.btn_next).performClick() }
            s.await("the next page", 10_000) { it.findViewById<ViewPager2>(R.id.viewPager).currentItem == before + 1 }
            s.await("playback to stop", 15_000) { ctl.playerStateForTest() == STOP }
            s.await("the note") { it.findViewById<View>(R.id.capsuleNote).visibility == View.VISIBLE }
            s.awaitCollapsed()
        }
    }

    @Test
    fun portraitCollapsedStaysCollapsedAfterLandscape() {
        launchExpanded().use { s ->
            s.click(R.id.btn_player_collapse)
            s.awaitCollapsed()
            rotate(s, landscape = true)
            s.awaitCollapsed()
            // expanding by hand in landscape is temporary
            s.click(R.id.capsuleNote)
            s.awaitExpanded()
            rotate(s, landscape = false)
            s.awaitCollapsed()
        }
    }

    @Test
    fun portraitExpandedShowsTheCapsuleInLandscapeAndComesBack() {
        launchExpanded().use { s ->
            rotate(s, landscape = true)
            s.awaitCollapsed()
            rotate(s, landscape = false)
            s.awaitExpanded()
        }
    }

    @Test
    fun hidingThePlayerBarHidesBothAndShowingRestoresTheForm() {
        launchExpanded().use { s ->
            s.click(R.id.btn_player_collapse)
            s.awaitCollapsed()
            s.onActivity { it.onLyricsAction(R.id.menutoggle) }
            s.awaitNothing()
            s.onActivity { it.playerSheet.expand(false) }
            s.awaitNothing()
            s.onActivity { it.playerSheet.collapse(false) }
            s.awaitNothing()
            s.onActivity { it.onLyricsAction(R.id.menutoggle) }
            s.awaitCollapsed()

            s.onActivity { it.playerSheet.expand(false) }
            s.awaitExpanded()
            s.onActivity { it.onLyricsAction(R.id.menutoggle) }
            s.awaitNothing()
            s.onActivity { it.onLyricsAction(R.id.menutoggle) }
            s.awaitExpanded()
        }
    }

    @Test
    fun everyBuiltPageFollowsTheBottomPaddingFormula() {
        launchExpanded().use { s ->
            s.onActivity { it.setChromeHeld(true) }
            fun expectedAndActual(): List<Pair<Int, Int>> = s.read { a ->
                val reserve = a.playerReserve
                a.supportFragmentManager.fragments.filterIsInstance<ContentView>().filter { it.view != null }.map {
                    val v = it.view!!
                    val bar = v.findViewById<View>(R.id.lyricsButtonBar).height
                    (bar + reserve + a.systemBottomInset + dp(8)) to v.findViewById<ScrollView>(R.id.lyrics_scroll).paddingBottom
                }
            }
            s.await("padding (card)") { a -> expectedAndActual().let { e -> e.isNotEmpty() && e.all { it.first == it.second } } }
            assertThat(s.read { it.playerReserve }).isEqualTo(s.read { it.findViewById<View>(R.id.mediaPlayer).height })
            assertThat(s.read { it.playerReserve }).isGreaterThan(dp(100))
            s.click(R.id.btn_player_collapse)
            s.awaitCollapsed()
            s.await("padding (capsule)") { expectedAndActual().let { e -> e.isNotEmpty() && e.all { it.first == it.second } } }
            assertThat(s.read { it.playerReserve }).isEqualTo(dp(72))
            s.onActivity { it.onLyricsAction(R.id.menutoggle) }
            s.await("padding (hidden)") { expectedAndActual().let { e -> e.isNotEmpty() && e.all { it.first == it.second } } }
            assertThat(s.read { it.playerReserve }).isEqualTo(0)
        }
    }

    @Test
    fun lyricsScrollAboveTheCapsule() {
        launchExpanded().use { s ->
            s.click(R.id.btn_player_collapse)
            s.awaitCollapsed()
            s.onActivity { page(it)!!.findViewById<ScrollView>(R.id.lyrics_scroll).fullScroll(View.FOCUS_DOWN) }
            Thread.sleep(500)
            val bottom = s.read {
                val v = page(it)!!.findViewById<View>(R.id.lyricsView)
                val loc = IntArray(2)
                v.getLocationOnScreen(loc)
                loc[1] + v.height
            }
            val capsuleTop = centre(s, R.id.playerCapsule, 0f)[1]
            assertThat(bottom).isAtMost(capsuleTop)
        }
    }

    @Test
    fun buttonsCarryContentDescriptions() {
        launchExpanded().use { s ->
            assertThat(s.id(R.id.btn_player_collapse).contentDescription.toString()).isEqualTo(ctx.getString(R.string.c_player_collapse))
            assertThat(s.id(R.id.capsuleNote).contentDescription.toString()).isEqualTo(ctx.getString(R.string.c_player_expand))
            assertThat(s.id(R.id.capsuleExpand).contentDescription.toString()).isEqualTo(ctx.getString(R.string.c_player_expand))
            assertThat(s.id(R.id.capsulePlay).contentDescription).isNotNull()
            assertThat(s.id(R.id.sheet_handle).importantForAccessibility).isEqualTo(View.IMPORTANT_FOR_ACCESSIBILITY_NO)
        }
    }

    @Test
    @QuickTest
    fun stateSurvivesRecreation() {
        launchExpanded().use { s ->
            s.click(R.id.btn_player_collapse)
            s.awaitCollapsed()
            s.recreate()
            s.awaitPage()
            s.awaitCollapsed()
        }
    }

    private fun dp(v: Int): Int = (v * ctx.resources.displayMetrics.density + 0.5f).toInt()

    private companion object {
        const val STOP = 0
        const val PAUSE = 2
        const val PLAY = 3
    }
}
