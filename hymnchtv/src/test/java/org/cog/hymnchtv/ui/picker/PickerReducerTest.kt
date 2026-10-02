package org.cog.hymnchtv.ui.picker

import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.hymn.EnglishXRef
import org.cog.hymnchtv.hymn.HymnRef
import org.cog.hymnchtv.hymn.HymnSource
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.junit.Test

class PickerReducerTest {
    private val xref = EnglishXRef.parse("^ 0001: a #db1\n^ 0005: b #bb12\n^ 0005: c #db6\n^ 0012: d #db9\n")

    private fun type(s: PickerState, vararg digits: Int) = digits.fold(s) { acc, d -> PickerReducer.pressDigit(acc, d, xref) }

    private val bb = PickerState(source = HymnSource.BB)

    @Test fun bbThirtySevenIsValid() {
        val p = PickerReducer.preview(type(bb, 3, 7), xref)
        assertThat(p).isEqualTo(Preview.Valid(HymnRef(HymnTypes.BB, 37)))
    }

    @Test fun bbFortyIsInvalidAndListsOtherBooks() {
        val p = PickerReducer.preview(type(bb, 4, 0), xref)
        assertThat(p).isEqualTo(Preview.Invalid(HymnSource.BB, 40, false, listOf(HymnSource.DB, HymnSource.XB, HymnSource.XG, HymnSource.YB)))
        assertThat(PickerReducer.target(p)).isNull()
    }

    @Test fun fuModes() {
        val db = PickerReducer.pressFu(PickerState(source = HymnSource.DB))
        assertThat(PickerReducer.preview(type(db, 3), xref)).isEqualTo(Preview.Valid(HymnRef(HymnTypes.DB, 783)))
        val yb = PickerReducer.pressFu(PickerState(source = HymnSource.YB))
        assertThat(PickerReducer.preview(type(yb, 1), xref)).isEqualTo(Preview.Valid(HymnRef(HymnTypes.YB, 276)))
    }

    @Test fun switchingFromFuToABookWithoutFuLeavesFuAndKeepsDigits() {
        val s = PickerReducer.selectSource(type(PickerReducer.pressFu(PickerState()), 3), HymnSource.BB)
        assertThat(s.isFu).isFalse(); assertThat(s.digits).isEqualTo("3"); assertThat(s.notice).isEqualTo(Notice.NO_FU_IN_BOOK)
    }

    @Test fun pressingFuInABookWithoutFuOnlyRaisesTheNotice() {
        val s = PickerReducer.pressFu(bb)
        assertThat(s.notice).isEqualTo(Notice.NO_FU_IN_BOOK); assertThat(s.isFu).isFalse()
    }

    @Test fun sourceChangeRecomputesThePreview() {
        val s = type(PickerState(source = HymnSource.BB), 3, 7)
        assertThat(PickerReducer.preview(s, xref)).isInstanceOf(Preview.Valid::class.java)
        assertThat(PickerReducer.preview(PickerReducer.selectSource(s, HymnSource.ER), xref)).isInstanceOf(Preview.Invalid::class.java)
    }

    @Test fun englishPreviewsAndCandidates() {
        val en = PickerState(source = HymnSource.ENGLISH)
        assertThat(PickerReducer.preview(type(en, 1), xref)).isEqualTo(Preview.English(1, listOf(HymnRef(HymnTypes.DB, 1)), 0))
        val five = type(en, 5)
        val p = PickerReducer.preview(five, xref) as Preview.English
        assertThat(p.candidates).hasSize(2)
        assertThat(PickerReducer.target(p)).isEqualTo(HymnRef(HymnTypes.BB, 12))
        val picked = PickerReducer.preview(PickerReducer.pickEnglish(five, 1), xref)
        assertThat(PickerReducer.target(picked)).isEqualTo(HymnRef(HymnTypes.DB, 6))
        // a number with no counterpart cannot even be typed; 2 is not a digit continuing 1 or 5 or 12
        assertThat(PickerReducer.digitEnabled(en, 2, xref)).isFalse()
        assertThat(PickerReducer.preview(en.copy(digits = "2"), xref)).isEqualTo(Preview.NoCounterpart(2))
        assertThat(PickerReducer.target(Preview.NoCounterpart(2))).isNull()
    }

    @Test fun backspaceSequence() {
        var s = PickerReducer.pressFu(PickerState())
        s = type(s, 3)
        s = PickerReducer.backspace(s); assertThat(s.digits).isEmpty(); assertThat(s.isFu).isTrue()
        s = PickerReducer.backspace(s); assertThat(s.isFu).isFalse()
        assertThat(PickerReducer.backspace(s)).isEqualTo(s)
    }

    @Test fun disabledKeyReturnsTheSameInstance() {
        val s = type(bb, 3)
        assertThat(PickerReducer.digitEnabled(s, 9, xref)).isFalse()
        assertThat(PickerReducer.pressDigit(s, 9, xref)).isSameInstanceAs(s)
    }

    @Test fun statesAreImmutable() {
        val s = PickerState(source = HymnSource.BB, digits = "4")
        val copy = s.copy()
        PickerReducer.pressDigit(s, 5, xref); PickerReducer.backspace(s); PickerReducer.selectSource(s, HymnSource.DB); PickerReducer.pressFu(s)
        assertThat(s).isEqualTo(copy)
    }

    @Test fun emptyEntryIsEmptyPreview() {
        assertThat(PickerReducer.preview(PickerState(), xref)).isEqualTo(Preview.Empty)
    }
}
