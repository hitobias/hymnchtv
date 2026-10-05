package org.cog.hymnchtv.ui.picker

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PickerChromeTest {
    @Test fun homeShowsTocAndMore() {
        assertThat(PickerChrome.of(PickerMode.HOME)).isEqualTo(PickerChrome(showToc = true, showMoreHistory = true, showSetNext = false))
    }

    @Test fun jumpShowsOnlySetNext() {
        assertThat(PickerChrome.of(PickerMode.JUMP)).isEqualTo(PickerChrome(showToc = false, showMoreHistory = false, showSetNext = true))
    }
}
