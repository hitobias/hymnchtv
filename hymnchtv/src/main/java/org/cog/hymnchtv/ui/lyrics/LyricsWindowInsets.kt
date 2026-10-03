package org.cog.hymnchtv.ui.lyrics

import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.cog.hymnchtv.R

/** Told the resolved insets every time they change. */
fun interface InsetsListener {
    fun onInsets(insets: ContentInsets)
}

/** Space the lyrics screen keeps free around its content, in pixels. */
data class ContentInsets(val left: Int, val top: Int, val right: Int, val bottom: Int)

/**
 * The lyrics screen draws its background behind the system bars; the pager, the player card and the other
 * layers keep clear of them with two spacer views (top, bottom) and side margins (landscape bars, cut-outs).
 */
object LyricsWindowInsets {
    private const val BASE_MARGIN_DP = 5

    /** The keyboard sits on top of the navigation bar, never under it. */
    @JvmStatic
    fun resolve(barsLeft: Int, barsTop: Int, barsRight: Int, barsBottom: Int, imeBottom: Int): ContentInsets =
        ContentInsets(barsLeft.coerceAtLeast(0), barsTop.coerceAtLeast(0), barsRight.coerceAtLeast(0), maxOf(barsBottom, imeBottom, 0))

    /** Layers laid out with the 5dp side margins of content_main.xml; their margins grow by the side insets. */
    private val SIDE_LAYERS = intArrayOf(R.id.viewPager, R.id.notebookBar, R.id.mediaPlayer, R.id.filexferGui, R.id.webView)

    /** @param onInsets told the resolved insets every time they change (the player layer places itself with them) */
    @JvmStatic
    @JvmOverloads
    fun install(root: View, onInsets: InsetsListener? = null) {
        val density = root.resources.displayMetrics.density
        val base = (BASE_MARGIN_DP * density + 0.5f).toInt()
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, windowInsets ->
            val bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            val insets = resolve(bars.left, bars.top, bars.right, bars.bottom, ime.bottom)
            apply(root, base, insets)
            onInsets?.onInsets(insets)
            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.requestApplyInsets(root)
    }

    private fun apply(root: View, base: Int, insets: ContentInsets) {
        setHeight(root.findViewById(R.id.insetTop), insets.top)
        setHeight(root.findViewById(R.id.insetBottom), insets.bottom)
        for (id in SIDE_LAYERS) {
            val lp = root.findViewById<View>(id)?.layoutParams as? ViewGroup.MarginLayoutParams ?: continue
            val view = root.findViewById<View>(id)
            if (lp.leftMargin != base + insets.left || lp.rightMargin != base + insets.right) {
                lp.leftMargin = base + insets.left
                lp.rightMargin = base + insets.right
                view.layoutParams = lp
            }
        }
    }

    private fun setHeight(view: View?, height: Int) {
        view ?: return
        if (view.layoutParams.height != height) {
            view.layoutParams = view.layoutParams.apply { this.height = height }
        }
    }
}
