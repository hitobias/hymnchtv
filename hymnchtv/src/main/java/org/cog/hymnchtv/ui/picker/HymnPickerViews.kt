package org.cog.hymnchtv.ui.picker

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import org.cog.hymnchtv.R
import org.cog.hymnchtv.hymn.HymnSource
import org.cog.hymnchtv.ui.home.HomeColors

/** The views of hymn_picker.xml (portrait and landscape share every id), looked up once per view creation. */
class HymnPickerViews(val root: View) {
    /** Only the home tab has a background image behind the picker. */
    val background: ImageView? = root.findViewById(R.id.mainBackground)

    val searchField: TextView = root.findViewById(R.id.tv_search)

    val toc: MaterialButton = root.findViewById(R.id.btn_toc)

    val previewArea: View = root.findViewById(R.id.previewArea)
    val book: TextView = root.findViewById(R.id.tv_book)
    val entry: TextView = root.findViewById(R.id.tv_entry)
    val title: TextView = root.findViewById(R.id.title_preview)
    val note: TextView = root.findViewById(R.id.tv_preview_note)
    val alsoScroll: View = root.findViewById(R.id.also_in_scroll)
    val alsoLabel: TextView = root.findViewById(R.id.tv_also_label)
    val alsoIn: ChipGroup = root.findViewById(R.id.also_in_group)

    val keypadArea: View = root.findViewById(R.id.keypadArea)
    val actionArea: View = root.findViewById(R.id.actionArea)

    /** n0..n9 in digit order. */
    val digits: List<MaterialButton> = DIGIT_IDS.map { root.findViewById(it) }
    val fu: MaterialButton = root.findViewById(R.id.n10)
    val delete: MaterialButton = root.findViewById(R.id.n11)

    val open: MaterialButton = root.findViewById(R.id.btn_open)
    val addPlaylist: MaterialButton = root.findViewById(R.id.btn_add_playlist)
    val setNext: MaterialButton = root.findViewById(R.id.btn_set_next)

    val recentArea: View = root.findViewById(R.id.recentArea)
    val recentLabel: TextView = root.findViewById(R.id.tv_recent_label)
    val recentChips: android.widget.LinearLayout = root.findViewById(R.id.recent_chips)
    val recentEmpty: View = root.findViewById(R.id.recent_empty)
    val recentEmptyIcon: ImageView = root.findViewById(R.id.recent_empty_icon)
    val recentEmptyText: TextView = root.findViewById(R.id.recent_empty_text)
    val recentMore: MaterialButton = root.findViewById(R.id.btn_recent_more)

    /** The source buttons by source (the first four on the first row, the rest on the second). */
    val books: Map<HymnSource, MaterialButton> = linkedMapOf(
        HymnSource.DB to root.findViewById(R.id.bs_db),
        HymnSource.BB to root.findViewById(R.id.bs_bb),
        HymnSource.XB to root.findViewById(R.id.bs_xb),
        HymnSource.XG to root.findViewById(R.id.bs_xg),
        HymnSource.YB to root.findViewById(R.id.bs_yb),
        HymnSource.ER to root.findViewById(R.id.bs_er),
        HymnSource.ENGLISH to root.findViewById(R.id.bs_english),
    )

    /** What the home theme last applied; null on the jump panel, which keeps the app theme. Items created later take it too. */
    var theme: HomeColors? = null

    /** Keeps the book labels one size; replaced whenever the theme is applied. */
    var bookFitter: org.cog.hymnchtv.ui.home.BookLabelFitter? = null

    /** Gives a recent-hymn item the picker's colours (its small time line is dimmer than the label). */
    fun styleRecent(item: View) {
        theme?.styleRecent(item)
    }

    /** Gives a chip the picker's colours instead of the theme's surface colours. */
    fun styleChip(chip: com.google.android.material.chip.Chip) {
        theme?.styleChip(chip)
    }

    private companion object {
        val DIGIT_IDS = listOf(
            R.id.n0, R.id.n1, R.id.n2, R.id.n3, R.id.n4, R.id.n5, R.id.n6, R.id.n7, R.id.n8, R.id.n9,
        )
    }
}
