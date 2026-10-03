package org.cog.hymnchtv.ui.picker

import android.content.Context
import android.content.res.Configuration
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.cog.hymnchtv.R
import org.cog.hymnchtv.reading.LyricsTypefaces
import org.cog.hymnchtv.reading.background.BackgroundChoice
import org.cog.hymnchtv.reading.background.BackgroundPolicy
import org.cog.hymnchtv.reading.background.BackgroundPreset
import org.cog.hymnchtv.reading.background.UiTokens
import org.cog.hymnchtv.ui.home.HomeColors
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Spec 5 and 7.2: the seven book cells and the contents button never show an ellipsis, in Chinese (Simplified and
 * Traditional) and English, on a 320dp wide screen (288dp of content) and with the system font enlarged to 1.3.
 * Inflated off-screen at the exact width so it holds on any emulator.
 */
@RunWith(AndroidJUnit4::class)
class HomeEllipsisTest {
    private val target: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun loadKai() {
        val latch = CountDownLatch(2)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            LyricsTypefaces.request(target, false) { latch.countDown() }
            LyricsTypefaces.request(target, true) { latch.countDown() }
        }
        assertThat(latch.await(20, TimeUnit.SECONDS)).isTrue()
    }

    private fun inflate(tag: String, widthDp: Int, fontScale: Float): HymnPickerViews {
        val config = Configuration(target.resources.configuration).apply {
            setLocale(Locale.forLanguageTag(tag))
            this.fontScale = fontScale
        }
        val ctx = ContextThemeWrapper(target.createConfigurationContext(config), R.style.AppTheme)
        lateinit var views: HymnPickerViews
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val root = LayoutInflater.from(ctx).inflate(R.layout.hymn_picker, null, false)
            views = HymnPickerViews(root)
            val choice = BackgroundChoice.Preset(BackgroundPreset.DAWN)
            val tokens = UiTokens.from(BackgroundPolicy.tokenInput(choice))
            HomeColors(ctx, tokens, choice, BackgroundPolicy.palette(choice)).apply(views)
            val px = (widthDp * ctx.resources.displayMetrics.density).toInt()
            // The label fitter sets one size for all cells after the first layout, which asks for another pass
            repeat(3) {
                root.measure(View.MeasureSpec.makeMeasureSpec(px, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
                root.layout(0, 0, root.measuredWidth, root.measuredHeight)
            }
            // A TextView builds its text layout lazily: draw each label once so the layout that gets checked is the one drawn
            (views.books.values + views.toc).forEach {
                it.draw(android.graphics.Canvas(android.graphics.Bitmap.createBitmap(it.width.coerceAtLeast(1), it.height.coerceAtLeast(1), android.graphics.Bitmap.Config.ARGB_8888)))
            }
        }
        return views
    }

    private fun assertWhole(views: HymnPickerViews, what: String) {
        val labels: List<TextView> = views.books.values.toList() + views.toc
        assertThat(labels).hasSize(8)
        for (b in labels) {
            val layout = b.layout
            assertWithMessage("$what: layout of ${b.text}").that(layout).isNotNull()
            assertWithMessage("$what: '${b.text}' lines").that(layout.lineCount).isEqualTo(1)
            assertWithMessage("$what: '${b.text}' is cut off").that(layout.getEllipsisCount(0)).isEqualTo(0)
        }
    }

    @Test
    fun noEllipsisAt320dpInChineseAndEnglish() {
        loadKai()
        for (tag in listOf("zh-Hans-CN", "zh-Hant-TW", "en-US")) {
            assertWhole(inflate(tag, CONTENT_DP_AT_320, 1.0f), "$tag @320dp")
        }
    }

    @Test
    fun noEllipsisAt320dpWithFontScale13() {
        loadKai()
        for (tag in listOf("zh-Hans-CN", "zh-Hant-TW", "en-US")) {
            assertWhole(inflate(tag, CONTENT_DP_AT_320, 1.3f), "$tag @320dp x1.3")
        }
    }

    @Test
    fun englishLabelsAreTheSpecifiedShortOnes() {
        val views = inflate("en-US", CONTENT_DP_AT_320, 1.0f)
        val shown = views.books.values.map { it.text.toString() } + views.toc.text.toString()
        assertThat(shown).containsExactly("Hymns", "Suppl", "NewSg", "NewHy", "Youth", "Child", "Eng", "Index").inOrder()
    }

    @Test
    fun allBookLabelsShareOneTextSize() {
        loadKai()
        for (scale in listOf(1.0f, 1.3f)) {
            val views = inflate("zh-Hans-CN", CONTENT_DP_AT_320, scale)
            val sizes = (views.books.values + views.toc).map { it.textSize }.toSet()
            assertThat(sizes).hasSize(1)
        }
    }

    @Test
    fun bothRowsHaveFourEqualCells() {
        val views = inflate("zh-Hans-CN", CONTENT_DP_AT_320, 1.0f)
        val cells = views.books.values.toList() + views.toc
        assertThat(cells.map { it.width }.toSet().size).isAtMost(2) // equal up to a rounding pixel
        assertThat(cells.map { it.height }.toSet()).hasSize(1)
        assertThat(cells.first().height).isEqualTo((44 * target.resources.displayMetrics.density).toInt())
    }

    private companion object {
        const val CONTENT_DP_AT_320 = 288
    }
}
