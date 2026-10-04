package org.cog.hymnchtv.ui.lyrics

import android.content.Context
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.RelativeSizeSpan
import android.view.View
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.reading.ReadingPrefKeys
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Lyrics page typography on real hymns: header, verse numbers, chorus indent (spec 5b-5, section 10). */
@RunWith(AndroidJUnit4::class)
class LyricsTypographyTest : LyricsTestBase() {
    private val prefs = ctx.getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE)

    @Before
    fun lyricsOnly() {
        prefs.edit().remove(ReadingPrefKeys.DISPLAY_MODE).commit()
    }

    private fun shownText(s: androidx.test.core.app.ActivityScenario<org.cog.hymnchtv.ContentHandler>): Spanned {
        s.await("lyrics text") { a ->
            listOf(R.id.lyrics_simplified, R.id.lyrics_traditional).any {
                val v = page(a)!!.findViewById<TextView>(it)
                v.visibility == View.VISIBLE && v.text.length > 40
            }
        }
        return s.read { a ->
            listOf(R.id.lyrics_simplified, R.id.lyrics_traditional).map { page(a)!!.findViewById<TextView>(it) }
                .first { it.visibility == View.VISIBLE }.text as Spanned
        }
    }

    @Test
    fun headerAndVerseNumbersAreDressed() {
        launch(MainActivity.HYMN_ER, 4).use { s ->
            val text = shownText(s)
            val sizes = text.getSpans(0, text.length, AbsoluteSizeSpan::class.java)
            assertThat(sizes.map { it.size }).contains(s.read { it.resources.getDimensionPixelSize(R.dimen.lyrics_header_number) })
            val verseNumbers = text.getSpans(0, text.length, RelativeSizeSpan::class.java).filter { it.sizeChange == LyricsTypography.VERSE_NUMBER_SCALE }
            assertThat(verseNumbers).hasSize(3)   // 一 二 三
        }
    }

    @Test
    fun aLoneChorusLabelIndentsItsBody() {
        launch(MainActivity.HYMN_ER, 4).use { s ->
            val text = shownText(s)
            val chorus = text.getSpans(0, text.length, ChorusSpan::class.java)
            assertThat(chorus).hasLength(1)
            val body = text.substring(text.getSpanStart(chorus[0]), text.getSpanEnd(chorus[0]))
            assertThat(body).startsWith("一切光明美丽之物")
            assertThat(body).doesNotContain("\n\n")
        }
    }

    @Test
    fun inlineChorusNotesAddNoIndent() {
        // BB 317 has three labelled choruses and one trailing "（接上面副歌）": exactly three bodies, not four
        launch(MainActivity.HYMN_BB, 317).use { s ->
            val text = shownText(s)
            assertThat(text.getSpans(0, text.length, ChorusSpan::class.java)).hasLength(3)
        }
    }

    @Test
    fun hymnWithoutChorusLabelsHasNoChorusSpan() {
        launch(MainActivity.HYMN_XB, 1).use { s ->
            val text = shownText(s)
            assertThat(text.getSpans(0, text.length, ChorusSpan::class.java)).isEmpty()
        }
    }
}
