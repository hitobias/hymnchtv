package org.cog.hymnchtv.ui.home

import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import org.cog.hymnchtv.MainActivity.HYMN_BB
import org.cog.hymnchtv.MainActivity.HYMN_DB
import org.cog.hymnchtv.MainActivity.HYMN_ER
import org.cog.hymnchtv.MainActivity.HYMN_XB
import org.cog.hymnchtv.MainActivity.HYMN_XG
import org.cog.hymnchtv.MainActivity.HYMN_YB
import org.cog.hymnchtv.R

/** The views of fragment_home, looked up once per view creation. */
class HomeViews(root: View) {
    val background: ImageView = root.findViewById(R.id.mainBackground)
    val hint: TextView = root.findViewById(R.id.tv_hint)
    val entry: TextView = root.findViewById(R.id.tv_entry)
    val preview: TextView = root.findViewById(R.id.title_preview)
    val history: RecyclerView = root.findViewById(R.id.historyListView)
    val search: EditText = root.findViewById(R.id.tv_search)
    val searchButton: MaterialButton = root.findViewById(R.id.btn_search)
    val keypadArea: View = root.findViewById(R.id.keypadArea)
    val actionArea: View = root.findViewById(R.id.actionArea)
    val english: MaterialButton = root.findViewById(R.id.btn_english)
    val addPlaylist: MaterialButton = root.findViewById(R.id.btn_add_playlist)
    val next: MaterialButton = root.findViewById(R.id.btn_next)

    /** n0..n9 in digit order. */
    val digits: List<MaterialButton> = DIGIT_IDS.map { root.findViewById(it) }
    val fu: MaterialButton = root.findViewById(R.id.n10)
    val delete: MaterialButton = root.findViewById(R.id.n11)

    val books: Map<String, MaterialButton> = mapOf(
        HYMN_ER to root.findViewById(R.id.bs_er),
        HYMN_XB to root.findViewById(R.id.bs_xb),
        HYMN_XG to root.findViewById(R.id.bs_xg),
        HYMN_YB to root.findViewById(R.id.bs_yb),
        HYMN_BB to root.findViewById(R.id.bs_bb),
        HYMN_DB to root.findViewById(R.id.bs_db),
    )

    /** Every button whose label follows the user's font colour. */
    val coloredButtons: List<MaterialButton> = digits + fu + delete + books.values + english + searchButton

    private companion object {
        val DIGIT_IDS = listOf(
            R.id.n0, R.id.n1, R.id.n2, R.id.n3, R.id.n4, R.id.n5, R.id.n6, R.id.n7, R.id.n8, R.id.n9,
        )
    }
}
