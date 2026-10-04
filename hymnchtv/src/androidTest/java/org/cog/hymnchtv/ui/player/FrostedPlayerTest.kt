package org.cog.hymnchtv.ui.player

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.ParcelFileDescriptor
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import eightbitlab.com.blurview.BlurView
import org.cog.hymnchtv.ContentHandler
import org.cog.hymnchtv.HymnsApp
import org.cog.hymnchtv.R
import org.cog.hymnchtv.reading.background.GlassMode
import org.cog.hymnchtv.reading.background.UiTokens
import org.cog.hymnchtv.reading.background.Wcag
import org.cog.hymnchtv.ui.lyrics.LyricsTestBase
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** The frosted player (spec 1.1.1): which layer is blurring when, the fallback below API 31, high-contrast text, insets. */
@RunWith(AndroidJUnit4::class)
class FrostedPlayerTest : LyricsTestBase() {
    private val blurs get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    @After
    fun resetHighContrast() {
        shell("settings put secure ${GlassPolicy.HIGH_TEXT_CONTRAST_KEY} 0")
    }

    private fun shell(command: String) {
        val pfd = instrumentation.uiAutomation.executeShellCommand(command)
        ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }
    }

    private fun ContentHandler.card() = findViewById<GlassFrameLayout>(R.id.playerUi)
    private fun ContentHandler.capsule() = findViewById<PlayerCapsuleView>(R.id.playerCapsule)

    /** Class name of the BlurView's controller: NoOpController means no blur pipeline, no bitmap, was ever created. */
    private fun controllerOf(view: BlurView): String =
        BlurView::class.java.getDeclaredField("blurController").apply { isAccessible = true }.get(view)!!.javaClass.simpleName

    private fun ActivityScenario<ContentHandler>.awaitCollapsed() = await("the capsule") {
        it.capsule().let { c -> c.visibility == View.VISIBLE && c.alpha == 1f } && it.card().visibility == View.GONE
    }

    private fun ActivityScenario<ContentHandler>.awaitExpanded() = await("the card") {
        it.capsule().visibility == View.GONE && it.card().let { c -> c.visibility == View.VISIBLE && c.alpha == 1f && c.scaleX == 1f }
    }

    @Test
    fun cardIsGlassOnApi31AndAFlatFallbackBelow() {
        launchExpanded().use { s ->
            s.read { a ->
                val card = a.card()
                val glass = card.glass!!
                val blur = card.findViewById<View>(R.id.playerBlur)
                assertThat(blur).isInstanceOf(BlurView::class.java)
                assertThat(glass.supported).isEqualTo(blurs)
                val alpha = glass.tint ushr 24
                if (blurs) {
                    assertThat(blur.visibility).isEqualTo(View.VISIBLE)
                    assertThat(glass.mode).isEqualTo(GlassMode.BLUR)
                    assertThat(glass.blurActive).isTrue()
                    assertThat(controllerOf(blur as BlurView)).isEqualTo("RenderNodeBlurController")
                    assertThat(alpha).isAtLeast(Math.round(0.72f * 255))
                    assertThat(alpha).isAtMost(Math.round(0.94f * 255))
                }
                else {
                    // Flat, solid-looking fallback: no blur controller, no bitmap
                    assertThat(blur.visibility).isEqualTo(View.GONE)
                    assertThat(glass.mode).isEqualTo(GlassMode.FALLBACK)
                    assertThat(glass.blurActive).isFalse()
                    assertThat(controllerOf(blur as BlurView)).isEqualTo("NoOpController")
                    assertThat(alpha).isEqualTo(Math.round(0.94f * 255))
                }
                assertThat(card.clipToOutline).isTrue()
                assertThat(card.background).isNotNull()
                assertThat(a.playerSheet.cardBlurActive()).isEqualTo(blurs)
                assertThat(a.capsule().glass!!.blurActive).isFalse()
            }
        }
    }

    @Test
    fun capsuleIsGlassToo() {
        launch().use { s ->
            s.onActivity { it.playerSheet.collapse(animate = false) }
            s.awaitCollapsed()
            s.read { a ->
                val capsule = a.capsule()
                val glass = capsule.glass!!
                val blur = capsule.findViewById<View>(R.id.capsuleBlur)
                assertThat(blur).isInstanceOf(BlurView::class.java)
                assertThat(glass.blurActive).isEqualTo(blurs)
                assertThat(blur.visibility).isEqualTo(if (blurs) View.VISIBLE else View.GONE)
                assertThat(controllerOf(blur as BlurView)).isEqualTo(if (blurs) "RenderNodeBlurController" else "NoOpController")
                assertThat(capsule.clipToOutline).isTrue()
                assertThat(glass.tint).isEqualTo(a.playerTokens.surface)
                assertThat(a.card().glass!!.blurActive).isFalse()
            }
        }
    }

    @Test
    fun blurFollowsCollapseExpandHideAndTheTransition() {
        launchExpanded().use { s ->
            assertThat(s.read { it.playerSheet.cardBlurActive() }).isEqualTo(blurs)
            // Mid-transition both layers are on screen: both blur (the first frame of the container transform)
            s.onActivity { it.playerSheet.collapse(animate = true) }
            val mid = s.read { it.card().visibility to it.capsule().visibility }
            if (mid.first == View.VISIBLE) {
                assertThat(s.read { it.playerSheet.cardBlurActive() }).isEqualTo(blurs)
                assertThat(s.read { it.playerSheet.capsuleBlurActive() }).isEqualTo(blurs)
            }
            s.awaitCollapsed()
            assertThat(s.read { it.playerSheet.cardBlurActive() }).isFalse()
            assertThat(s.read { it.playerSheet.capsuleBlurActive() }).isEqualTo(blurs)
            s.onActivity { it.playerSheet.expand(animate = true) }
            s.awaitExpanded()
            assertThat(s.read { it.playerSheet.cardBlurActive() }).isEqualTo(blurs)
            assertThat(s.read { it.playerSheet.capsuleBlurActive() }).isFalse()
            // Hidden by the user: nothing shown, nothing blurring
            s.onActivity { it.playerSheet.toggleUserHidden() }
            s.await("hidden") { it.card().visibility == View.GONE && it.capsule().visibility == View.GONE }
            assertThat(s.read { it.playerSheet.cardBlurActive() }).isFalse()
            assertThat(s.read { it.playerSheet.capsuleBlurActive() }).isFalse()
            s.onActivity { it.playerSheet.toggleUserHidden() }
            s.awaitExpanded()
            assertThat(s.read { it.playerSheet.cardBlurActive() }).isEqualTo(blurs)
        }
    }

    @Test
    fun highContrastTextTurnsTheGlassOpaqueAndBack() {
        launchExpanded().use { s ->
            shell("settings put secure ${GlassPolicy.HIGH_TEXT_CONTRAST_KEY} 1")
            s.await("opaque glass") { it.glassMode == GlassMode.OPAQUE }
            s.read { a ->
                val glass = a.card().glass!!
                assertThat(glass.mode).isEqualTo(GlassMode.OPAQUE)
                assertThat(glass.tint ushr 24).isEqualTo(0xFF)
                assertThat(glass.blurActive).isFalse()
                assertThat(a.capsule().glass!!.tint ushr 24).isEqualTo(0xFF)
                val title = a.card().findViewById<TextView>(R.id.hymn_info)
                assertThat(Wcag.contrast(title.currentTextColor, UiTokens.over(0xFF000000.toInt(), glass.tint))).isAtLeast(4.5)
                assertThat(title.currentTextColor).isEqualTo(a.playerTokens.onSurface)
            }
            shell("settings put secure ${GlassPolicy.HIGH_TEXT_CONTRAST_KEY} 0")
            s.await("glass again") { it.glassMode != GlassMode.OPAQUE }
            s.read { a ->
                val glass = a.card().glass!!
                assertThat(glass.mode).isEqualTo(if (blurs) GlassMode.BLUR else GlassMode.FALLBACK)
                assertThat(glass.tint ushr 24).isLessThan(0xFF)
                assertThat(glass.blurActive).isEqualTo(blurs)
            }
        }
    }

    @Test
    fun textStaysReadableOnTheGlassTint() {
        launch().use { s ->
            s.read { a ->
                val tokens = a.playerTokens
                val glass = a.card().glass!!
                assertThat(glass.tint).isEqualTo(tokens.surface)
                listOf(R.id.hymn_info, R.id.playback_auto_stream).forEach {
                    val view = a.card().findViewById<TextView>(it)
                    assertThat(view.currentTextColor).isEqualTo(tokens.onSurface)
                }
            }
        }
    }

    /** The blur layer fills the card exactly and the glass adds no inset: the host margins are the system insets only. */
    private fun assertInsetsUntouched(s: ActivityScenario<ContentHandler>) {
        s.read { a ->
            val card = a.card()
            val blur = card.findViewById<View>(R.id.playerBlur)
            if (blurs) { // below API 31 the layer is gone: the flat tint is the card's own background
                assertThat(blur.width).isEqualTo(card.width)
                assertThat(blur.height).isEqualTo(card.height)
                assertThat(blur.left).isEqualTo(0)
                assertThat(blur.top).isEqualTo(0)
            }
            val host = a.findViewById<View>(R.id.mediaPlayer)
            assertThat((host.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin).isEqualTo(a.playerSheet.systemBottom)
            assertThat(a.playerReserve).isEqualTo(host.height)
            assertThat(card.height).isEqualTo(host.height)
        }
    }

    @Test
    fun landscapeKeepsTheInsetsAndTheBlurFillsTheCard() {
        launchExpanded().use { s ->
            assertInsetsUntouched(s)
            s.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
            s.await("landscape", 15_000) { !HymnsApp.isPortrait && it.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE }
            s.awaitPage()
            Thread.sleep(600)
            // Landscape starts as the capsule: expand the card to measure it
            s.onActivity { it.playerSheet.expand(animate = false) }
            s.awaitExpanded()
            assertInsetsUntouched(s)
            s.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
        }
    }

    @Test
    fun keyboardKeepsTheInsetsAndTheBlurFillsTheCard() {
        shell("settings put secure show_ime_with_hard_keyboard 1")
        launchExpanded().use { s ->
            val before = s.read { it.playerSheet.systemBottom }
            s.onActivity {
                val field = it.card().findViewById<View>(R.id.repeatCount)
                field.requestFocus()
                WindowCompat.getInsetsController(it.window, field).show(WindowInsetsCompat.Type.ime())
            }
            val shown = runCatching { s.await("the keyboard", 8_000) { it.playerSheet.systemBottom > before } }.isSuccess
            assumeTrue("this emulator shows no soft keyboard", shown)
            Thread.sleep(500)
            assertInsetsUntouched(s)
            s.onActivity { WindowCompat.getInsetsController(it.window, it.card()).hide(WindowInsetsCompat.Type.ime()) }
        }
    }
}
