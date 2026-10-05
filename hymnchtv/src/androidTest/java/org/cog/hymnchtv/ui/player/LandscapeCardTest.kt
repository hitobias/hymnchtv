package org.cog.hymnchtv.ui.player

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.ContentHandler
import org.cog.hymnchtv.HymnsApp
import org.cog.hymnchtv.R
import org.cog.hymnchtv.ui.lyrics.LyricsTestBase
import org.junit.Test
import org.junit.runner.RunWith

/** 1.6.0: an expanded card in landscape takes at most 60% of the screen height and its rows scroll; portrait is unchanged. */
@RunWith(AndroidJUnit4::class)
class LandscapeCardTest : LyricsTestBase() {
    private fun rotate(s: ActivityScenario<ContentHandler>, landscape: Boolean) {
        s.onActivity {
            it.requestedOrientation = if (landscape) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        s.await("rotation", 15_000) {
            HymnsApp.isPortrait != landscape && it.resources.configuration.orientation ==
                (if (landscape) Configuration.ORIENTATION_LANDSCAPE else Configuration.ORIENTATION_PORTRAIT)
        }
        s.awaitPage()
        Thread.sleep(500)
    }

    private fun ActivityScenario<ContentHandler>.awaitCard() = await("the card", 10_000) { a ->
        a.findViewById<View>(R.id.playerUi).let { it.visibility == View.VISIBLE && it.alpha == 1f && it.scaleX == 1f }
    }

    @Test
    fun theLandscapeCardIsCappedAndScrolls() {
        launch().use { s ->
            try {
                rotate(s, landscape = true)
                s.onActivity { it.playerSheet.expand(true) }
                s.awaitCard()
                instrumentation.waitForIdleSync()
                s.read { a ->
                    val root = a.findViewById<View>(R.id.mediaPlayer).parent as View
                    val card = a.findViewById<View>(R.id.playerUi)
                    assertThat(card.height.toFloat()).isAtMost(root.height * PlayerSheetState.LANDSCAPE_CARD_MAX_FRACTION + 1f)
                    // on the 320 x 640dp test AVD the rows do not fit in 60% of 320dp
                    assertThat(a.findViewById<View>(R.id.player_scroll).canScrollVertically(1)).isTrue()
                }
            } finally {
                rotate(s, landscape = false)
            }
            s.onActivity { it.playerSheet.expand(true) }
            s.awaitCard()
            instrumentation.waitForIdleSync()
            s.read { a -> assertThat(a.findViewById<MaxHeightScrollView>(R.id.player_scroll).maxHeightPx).isEqualTo(0) }
        }
    }
}
