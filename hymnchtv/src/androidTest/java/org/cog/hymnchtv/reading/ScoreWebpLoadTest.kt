package org.cog.hymnchtv.reading

import android.content.Context
import android.graphics.BitmapFactory
import android.widget.ImageView
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.ui.lyrics.LyricsTestBase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The lyrics page shows WebP score pages (1.6.0): all five pages of db152 decode on API 24 and 34. */
@RunWith(AndroidJUnit4::class)
class ScoreWebpLoadTest : LyricsTestBase() {
    private val prefs get() = ctx.getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE)

    @Before
    fun scoreOnly() {
        prefs.edit().putString(ReadingPrefKeys.DISPLAY_MODE, DisplayMode.SCORE_ONLY.name).commit()
    }

    @After
    fun restore() {
        prefs.edit().remove(ReadingPrefKeys.DISPLAY_MODE).commit()
    }

    @Test
    fun allFivePagesOfDb152AreShown() {
        launch(MainActivity.HYMN_DB, 152).use { s ->
            val ids = intArrayOf(R.id.contentView, R.id.contentView_a, R.id.contentView_b, R.id.contentView_c, R.id.contentView_d)
            val names = ScorePages.fileNames("lyrics_db_score/db152", 5)
            // Glide's error drawable also has a width: the shown page must have the aspect ratio of its own asset
            val ratios = names.map { name ->
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                ctx.assets.open(name).use { BitmapFactory.decodeStream(it, null, bounds) }
                bounds.outWidth.toFloat() / bounds.outHeight
            }
            s.await("five decoded score pages", 20_000) { a ->
                val page = page(a) ?: return@await false
                ids.indices.all { i ->
                    val d = page.findViewById<ImageView>(ids[i]).drawable
                    d != null && d.intrinsicHeight > 0 &&
                        Math.abs(d.intrinsicWidth.toFloat() / d.intrinsicHeight - ratios[i]) < 0.02f
                }
            }
        }
    }
}
