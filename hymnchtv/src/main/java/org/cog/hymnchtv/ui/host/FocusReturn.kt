package org.cog.hymnchtv.ui.host

import android.view.View
import org.cog.hymnchtv.R

/**
 * Which view gets the focus back when an overlay closes (1.6.0): the one that had the keyboard focus when it opened, else
 * the home page button that opens that overlay (the picker's search field and "more history" button).
 */
object FocusReturn {
    @JvmStatic
    fun target(recordedFocusId: Int, closedTag: String?): Int = when {
        recordedFocusId != View.NO_ID -> recordedFocusId
        closedTag == MainHost.TAG_SEARCH -> R.id.tv_search
        closedTag == MainHost.TAG_HISTORY -> R.id.btn_recent_more
        else -> View.NO_ID
    }
}
