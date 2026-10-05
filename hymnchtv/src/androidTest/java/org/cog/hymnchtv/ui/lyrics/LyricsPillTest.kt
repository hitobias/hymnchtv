package org.cog.hymnchtv.ui.lyrics

import android.graphics.Rect
import android.view.View
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.ContentHandler
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.QuickTest
import org.cog.hymnchtv.R
import org.junit.Test
import org.junit.runner.RunWith

/** Spec rev 3 section 3: the bottom capsule never overlaps the player, keeps its words whole and disables ZH/EN without English. */
@RunWith(AndroidJUnit4::class)
class LyricsPillTest : LyricsTestBase() {

    private fun dp(v: Int): Int = (v * ctx.resources.displayMetrics.density + 0.5f).toInt()

    private fun screenRect(v: View): Rect {
        val at = IntArray(2).also { v.getLocationOnScreen(it) }
        return Rect(at[0], at[1], at[0] + v.width, at[1] + v.height)
    }

    private fun assertNoOverlap(s: ActivityScenario<ContentHandler>, otherId: Int) {
        s.onActivity { a ->
            val pill = screenRect(page(a)!!.findViewById(R.id.lyricsButtonBar))
            val other = a.findViewById<View>(otherId)
            if (other.visibility == View.VISIBLE && other.width > 0) {
                assertThat(Rect.intersects(pill, screenRect(other))).isFalse()
            }
            val dm = a.resources.displayMetrics
            assertThat(pill.left).isAtLeast(0)
            assertThat(pill.right).isAtMost(dm.widthPixels)
        }
    }

    @Test
    @QuickTest
    fun capsuleSitsBesideThePlayerCapsule() {
        launch().use { s ->
            s.revealChrome()
            s.onActivity { it.setChromeHeld(true) }
            assertNoOverlap(s, R.id.playerCapsule)
        }
    }

    @Test
    fun capsuleSitsAboveTheExpandedCard() {
        launchExpanded().use { s ->
            s.revealChrome()
            s.onActivity { it.setChromeHeld(true) }
            assertNoOverlap(s, R.id.mediaPlayer)
        }
    }

    @Test
    fun capsuleSpansTheWidthWhenThePlayerIsHidden() {
        launch().use { s ->
            s.onActivity { it.onLyricsAction(R.id.menutoggle) }
            s.revealChrome()
            s.onActivity { it.setChromeHeld(true) }
            s.onActivity { a ->
                val pill = page(a)!!.findViewById<View>(R.id.lyricsButtonBar)
                val end = (pill.layoutParams as android.view.ViewGroup.MarginLayoutParams).marginEnd
                assertThat(end).isEqualTo(dp(5))
            }
            s.onActivity { it.onLyricsAction(R.id.menutoggle) } // restore the player for later tests
        }
    }

    @Test
    fun wordsAreNeverCut() {
        launch().use { s ->
            s.revealChrome()
            s.onActivity { it.setChromeHeld(true) }
            s.onActivity { a ->
                listOf(R.id.button_ts, R.id.button_english, R.id.button_mode).forEach { id ->
                    val t = page(a)!!.findViewById<TextView>(id)
                    val layout = t.layout ?: return@forEach
                    assertThat(layout.lineCount).isAtMost(1)
                    if (t.text.isNotEmpty()) assertThat(layout.getEllipsisCount(0)).isEqualTo(0)
                    // compound padding = padding + drawable + drawablePadding (the mode icon counts)
                    val needed = t.paint.measureText(t.text.toString()) + t.compoundPaddingLeft + t.compoundPaddingRight
                    assertThat(t.width.toFloat()).isAtLeast(needed - 1f)
                    val bar = page(a)!!.findViewById<View>(R.id.lyricsButtonBar)
                    assertThat(t.right).isAtMost(bar.width - bar.paddingRight)
                }
            }
        }
    }

    @Test
    fun aaButtonKeepsBothLetters() {
        launch().use { s ->
            s.revealChrome()
            s.onActivity { it.setChromeHeld(true) }
            s.onActivity { a ->
                val t = page(a)!!.findViewById<TextView>(R.id.btn_aa)
                assertThat(t.layout.lineCount).isAtMost(1)
                val needed = t.paint.measureText("Aa") + t.compoundPaddingLeft + t.compoundPaddingRight
                assertThat(t.width.toFloat()).isAtLeast(needed - 1f)
            }
        }
    }

    @Test
    fun activeSideIsAnnouncedToTalkBack() {
        launch().use { s ->
            s.onActivity { a ->
                val ts = page(a)!!.findViewById<TextView>(R.id.button_ts)
                val trad = a.getString(R.string.c_pill_trad)
                val simp = a.getString(R.string.c_pill_simp)
                assertThat(androidx.core.view.ViewCompat.getStateDescription(ts)?.toString()).isAnyOf(trad, simp)
                // 5 of the 大本 shows Simplified lyrics here, so the label must match what the text highlights
                val spans = (ts.text as android.text.Spanned).getSpans(0, ts.text.length, android.text.style.ForegroundColorSpan::class.java)
                assertThat(spans).hasLength(1)
                val start = (ts.text as android.text.Spanned).getSpanStart(spans[0])
                val label = if (start == 0) trad else simp
                assertThat(androidx.core.view.ViewCompat.getStateDescription(ts)?.toString()).isEqualTo(label)
            }
        }
    }

    @Test
    fun languageItemIsEnabledWithEnglish() {
        launch().use { s ->
            s.onActivity { a ->
                assertThat(a.hymnNoEng).isNotNull()
                assertThat(page(a)!!.findViewById<TextView>(R.id.button_english).isEnabled).isTrue()
            }
        }
    }

    @Test
    fun languageItemIsDisabledWithoutEnglish() {
        // 新歌頌詠 (HYMN_XB) has no English counterpart (HymnNoCh2EngXRef)
        launch(MainActivity.HYMN_XB, 1).use { s ->
            s.onActivity { a ->
                assertThat(a.hymnNoEng).isNull()
                val english = page(a)!!.findViewById<TextView>(R.id.button_english)
                assertThat(english.isEnabled).isFalse()
                assertThat(english.contentDescription.toString()).isEqualTo(a.getString(R.string.c_cd_no_english))
            }
        }
    }
}
