package org.cog.hymnchtv.ui.page

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Side and bottom system-bar insets of a full page: pads [root] by what the window hands it. The top inset is taken by the
 * [PageTitleBar]. A host that consumes or applies the insets itself (the main host pads its frame) never delivers them
 * here, so nothing is padded twice; a standalone host (the reading settings activity, a test) gets edge-to-edge pages.
 */
object PageInsets {
    @JvmStatic
    fun install(root: View) {
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.setPadding(bars.left, 0, bars.right, bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }
}
