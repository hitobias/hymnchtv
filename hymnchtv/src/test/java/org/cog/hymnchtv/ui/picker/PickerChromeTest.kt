package org.cog.hymnchtv.ui.picker

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PickerChromeTest {
    @Test fun homeShowsTocAndMoreAndPlaylistOnlyWhenNotebookIsOn() {
        assertThat(PickerChrome.of(PickerMode.HOME, false)).isEqualTo(PickerChrome(true, true, false, false))
        assertThat(PickerChrome.of(PickerMode.HOME, true)).isEqualTo(PickerChrome(true, true, true, false))
    }

    @Test fun jumpShowsOnlySetNext() {
        assertThat(PickerChrome.of(PickerMode.JUMP, false)).isEqualTo(PickerChrome(false, false, false, true))
        assertThat(PickerChrome.of(PickerMode.JUMP, true)).isEqualTo(PickerChrome(false, false, false, true))
    }
}
