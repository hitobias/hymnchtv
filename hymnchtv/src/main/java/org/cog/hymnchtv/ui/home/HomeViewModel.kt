package org.cog.hymnchtv.ui.home

import androidx.lifecycle.ViewModel
import org.cog.hymnchtv.MainActivity.HYMN_BB
import org.cog.hymnchtv.MainActivity.HYMN_DB
import org.cog.hymnchtv.MainActivity.HYMN_ER
import org.cog.hymnchtv.MainActivity.HYMN_XB
import org.cog.hymnchtv.MainActivity.HYMN_XG
import org.cog.hymnchtv.MainActivity.HYMN_YB

/** Keypad state of the home tab; survives rotation (the old MainActivity kept these as fields). */
class HomeViewModel : ViewModel() {
    /** The selected (and remembered) hymn book; the live title preview looks the typed number up in it. */
    var hymnType: String = HYMN_DB
        private set

    /** What the entry field shows, e.g. "12" or "附3". */
    var number: String = ""

    var isFu: Boolean = false

    /** The next key press starts a new number (set on every resume and after an invalid entry). */
    var autoClear: Boolean = false

    private var restored = false

    /** Applies the persisted hymn book once; later calls (e.g. after rotation) keep the live selection. */
    fun restoreHymnType(saved: String?) {
        if (restored) return
        restored = true
        if (saved in HYMN_TYPES) hymnType = saved!!
    }

    fun selectHymnType(type: String) {
        require(type in HYMN_TYPES) { "Unknown hymn type: $type" }
        hymnType = type
    }

    companion object {
        val HYMN_TYPES = listOf(HYMN_ER, HYMN_XB, HYMN_XG, HYMN_YB, HYMN_BB, HYMN_DB)
    }
}
