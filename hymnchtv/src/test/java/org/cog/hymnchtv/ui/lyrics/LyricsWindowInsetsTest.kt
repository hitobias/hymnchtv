package org.cog.hymnchtv.ui.lyrics

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LyricsWindowInsetsTest {
    @Test
    fun keyboardSitsOnTopOfTheNavigationBarNeverUnderIt() {
        assertThat(LyricsWindowInsets.resolve(0, 90, 0, 48, 0)).isEqualTo(ContentInsets(0, 90, 0, 48))
        assertThat(LyricsWindowInsets.resolve(0, 90, 0, 48, 700)).isEqualTo(ContentInsets(0, 90, 0, 700))
        assertThat(LyricsWindowInsets.resolve(-3, -1, -2, -1, -5)).isEqualTo(ContentInsets(0, 0, 0, 0))
    }
}
