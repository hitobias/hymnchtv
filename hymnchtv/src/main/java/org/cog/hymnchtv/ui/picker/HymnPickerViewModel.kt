package org.cog.hymnchtv.ui.picker

import androidx.lifecycle.ViewModel
import org.cog.hymnchtv.hymn.EnglishXRef
import org.cog.hymnchtv.hymn.HymnSource

/** Keeps the entry across rotation. [autoClear] makes the first digit or 附 key after coming back start a new entry. */
class HymnPickerViewModel : ViewModel() {
    var state: PickerState = PickerState()
        private set

    var autoClear: Boolean = false

    private var restored = false

    /** Applies the saved source once per ViewModel (later calls, e.g. after rotation, keep the live state). */
    fun restoreSource(saved: String?) {
        if (restored) return
        restored = true
        state = state.copy(source = HymnSource.fromPref(saved))
    }

    private fun startEntry(): PickerState {
        val base = if (autoClear) PickerReducer.cleared(state) else state
        autoClear = false
        return base
    }

    fun pressDigit(digit: Int, xref: EnglishXRef) {
        state = PickerReducer.pressDigit(startEntry(), digit, xref)
    }

    fun pressFu() {
        state = PickerReducer.pressFu(startEntry())
    }

    fun backspace() {
        autoClear = false
        state = PickerReducer.backspace(state)
    }

    fun selectSource(source: HymnSource) {
        state = PickerReducer.selectSource(state, source)
    }

    fun pickEnglish(index: Int) {
        state = PickerReducer.pickEnglish(state, index)
    }

    fun clearNotice() {
        if (state.notice != null) state = state.copy(notice = null)
    }

    /** Replaces the whole entry (reopening from the history). */
    fun set(next: PickerState) {
        autoClear = false
        state = next
    }
}
