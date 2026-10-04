package org.cog.hymnchtv.ui.lyrics

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.reading.DisplayMode
import org.cog.hymnchtv.reading.ReadingPrefKeys
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** 1.2.0: lyrics-only by default (stored choices kept), and the empty media state of the player card. */
@RunWith(AndroidJUnit4::class)
class LyricsDefaultsTest : LyricsTestBase() {
    private val prefs = ctx.getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE)

    @Before
    fun clearMode() {
        prefs.edit().remove(ReadingPrefKeys.DISPLAY_MODE).commit()
    }

    @After
    fun restore() {
        prefs.edit().remove(ReadingPrefKeys.DISPLAY_MODE).commit()
    }

    @Test
    fun unsetDisplayModeShowsLyricsOnly() {
        launch().use { s ->
            s.await("lyrics shown") { page(it)!!.findViewById<View>(R.id.lyrics_simplified).visibility == View.VISIBLE ||
                page(it)!!.findViewById<View>(R.id.lyrics_traditional).visibility == View.VISIBLE }
            assertThat(s.pageView(R.id.scoreContainer).visibility).isEqualTo(View.GONE)
        }
    }

    @Test
    fun storedScoreAndLyricsIsKept() {
        prefs.edit().putString(ReadingPrefKeys.DISPLAY_MODE, DisplayMode.SCORE_AND_LYRICS.name).commit()
        launch().use { s ->
            s.await("score shown") { page(it)!!.findViewById<View>(R.id.scoreContainer).visibility == View.VISIBLE }
        }
    }

    @Test
    fun playButtonIsDisabledAndExplainedWhenThereIsNoMedia() {
        launch().use { s ->
            val card = s.read { it.findViewById<View>(R.id.playerUi) }
            val tokens = s.read { it.lyricsTokens }
            fun style(available: BooleanArray) = s.onActivity { PlayerCardStyle.styleSources(card, tokens, available) }
            fun playEnabled() = s.read { card.findViewById<ImageView>(R.id.playback_play).isEnabled }
            fun emptyShown() = s.read { card.findViewById<TextView>(R.id.media_empty).visibility == View.VISIBLE }

            style(booleanArrayOf(false, false, false, false))
            assertThat(playEnabled()).isFalse()
            assertThat(emptyShown()).isTrue()
            assertThat(s.read { card.findViewById<TextView>(R.id.media_empty).text.toString() })
                .isEqualTo(ctx.getString(R.string.media_none_available))

            style(booleanArrayOf(false, false, false, true))
            assertThat(playEnabled()).isTrue()
            assertThat(emptyShown()).isFalse()
        }
    }
}
