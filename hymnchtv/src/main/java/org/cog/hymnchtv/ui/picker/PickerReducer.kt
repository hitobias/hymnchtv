package org.cog.hymnchtv.ui.picker

import org.cog.hymnchtv.hymn.EnglishXRef
import org.cog.hymnchtv.hymn.HymnNumberRules
import org.cog.hymnchtv.hymn.HymnRef
import org.cog.hymnchtv.hymn.HymnSource

/** Pure functions over [PickerState]; each returns a new state and never changes the one it is given. */
object PickerReducer {
    fun digitEnabled(s: PickerState, digit: Int, xref: EnglishXRef): Boolean {
        val book = s.source.book
        return if (book == null) {
            HymnNumberRules.canAppendEnglishDigit(s.digits, digit, xref.numbers)
        } else {
            HymnNumberRules.canAppendDigit(book, s.digits, s.isFu, digit)
        }
    }

    /** A key that is not available returns [s] itself. */
    fun pressDigit(s: PickerState, digit: Int, xref: EnglishXRef): PickerState =
        if (!digitEnabled(s, digit, xref)) s else s.copy(digits = s.digits + digit, englishPick = 0, notice = null)

    fun pressFu(s: PickerState): PickerState =
        if (!s.source.supportsFu) s.copy(notice = Notice.NO_FU_IN_BOOK) else s.copy(isFu = !s.isFu, digits = "", englishPick = 0, notice = null)

    fun backspace(s: PickerState): PickerState = when {
        s.digits.isNotEmpty() -> s.copy(digits = s.digits.dropLast(1), englishPick = 0, notice = null)
        s.isFu -> s.copy(isFu = false, notice = null)
        else -> s.copy(notice = null)
    }

    /** Starts a new entry (used for the first key after coming back to the screen); the source stays. */
    fun cleared(s: PickerState): PickerState = s.copy(digits = "", isFu = false, englishPick = 0, notice = null)

    fun selectSource(s: PickerState, next: HymnSource): PickerState = when {
        next == s.source -> s
        s.isFu && !next.supportsFu -> s.copy(source = next, isFu = false, englishPick = 0, notice = Notice.NO_FU_IN_BOOK)
        else -> s.copy(source = next, englishPick = 0, notice = null)
    }

    fun pickEnglish(s: PickerState, index: Int): PickerState =
        if (s.source != HymnSource.ENGLISH || index < 0) s else s.copy(englishPick = index)

    fun preview(s: PickerState, xref: EnglishXRef): Preview {
        val number = s.digits.toIntOrNull() ?: return Preview.Empty
        val book = s.source.book
        if (book == null) {
            val candidates = xref.candidates(number)
            if (candidates.isEmpty()) return Preview.NoCounterpart(number)
            return Preview.English(number, candidates, s.englishPick.coerceIn(0, candidates.lastIndex))
        }
        val ref = HymnRef.fromEntry(book, number, s.isFu)
        return if (ref != null) {
            Preview.Valid(ref)
        } else {
            Preview.Invalid(s.source, number, s.isFu, HymnNumberRules.alsoValidIn(number, s.isFu, s.source))
        }
    }

    fun target(p: Preview): HymnRef? = when (p) {
        is Preview.Valid -> p.ref
        is Preview.English -> p.target
        else -> null
    }
}
