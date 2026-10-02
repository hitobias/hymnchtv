package org.cog.hymnchtv.ui.picker

import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.hymn.EnglishXRef
import org.cog.hymnchtv.hymn.HymnSource
import org.junit.Test

class HymnPickerViewModelTest {
    private val xref = EnglishXRef.EMPTY

    @Test fun restoreSourceMapsSavedValuesAndFallsBackToMainBook() {
        fun restored(v: String?) = HymnPickerViewModel().also { it.restoreSource(v) }.state.source
        assertThat(restored(null)).isEqualTo(HymnSource.DB)
        assertThat(restored("hymn_bb")).isEqualTo(HymnSource.BB)
        assertThat(restored("english")).isEqualTo(HymnSource.ENGLISH)
        assertThat(restored("垃圾")).isEqualTo(HymnSource.DB)
    }

    @Test fun restoreOnlyAppliesOnce() {
        val vm = HymnPickerViewModel()
        vm.restoreSource("hymn_bb"); vm.selectSource(HymnSource.XG); vm.restoreSource("hymn_er")
        assertThat(vm.state.source).isEqualTo(HymnSource.XG)
    }

    @Test fun stateSurvivesAsLongAsTheViewModel() {
        val vm = HymnPickerViewModel()
        vm.selectSource(HymnSource.BB); vm.pressDigit(4, xref); vm.pressDigit(5, xref)
        assertThat(vm.state.digits).isEqualTo("45")
    }

    @Test fun autoClearStartsANewEntryOnTheFirstDigitOnly() {
        val vm = HymnPickerViewModel()
        vm.pressDigit(1, xref); vm.pressDigit(2, xref)
        vm.autoClear = true
        vm.pressDigit(3, xref)
        assertThat(vm.state.digits).isEqualTo("3")
        vm.pressDigit(4, xref)
        assertThat(vm.state.digits).isEqualTo("34")
    }

    @Test fun autoClearDoesNotApplyToSourceChangeAndDropsFuOnTheFirstFuKey() {
        val vm = HymnPickerViewModel()
        vm.pressFu(); vm.pressDigit(3, xref)
        vm.autoClear = true
        vm.selectSource(HymnSource.YB)
        assertThat(vm.state.digits).isEqualTo("3"); assertThat(vm.autoClear).isTrue()
        vm.pressFu()
        assertThat(vm.state.isFu).isTrue(); assertThat(vm.state.digits).isEmpty()
    }
}
