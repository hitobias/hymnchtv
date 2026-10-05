package org.cog.hymnchtv.ui.playlist

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlaylistShareTextTest {
    @Test fun nameThenNumberedLines() {
        val text = PlaylistShareText.format(
            "主日聚會",
            listOf(PlaylistShareText.line("大本詩歌 第 5 首", "頌讚三一神"), PlaylistShareText.line("補充本 第 37 首", null)),
        )
        assertThat(text).isEqualTo("主日聚會\n1. 大本詩歌 第 5 首 頌讚三一神\n2. 補充本 第 37 首")
    }

    @Test fun anEmptyPlaylistIsJustItsName() {
        assertThat(PlaylistShareText.format("小排", emptyList())).isEqualTo("小排")
    }

    @Test fun onlyWhatIsPassedInIsShared() {
        // the share text is built from labels and titles only; nothing else (notes, sing times) can reach it
        val text = PlaylistShareText.format("p", listOf(PlaylistShareText.line("h", " ")))
        assertThat(text).isEqualTo("p\n1. h")
    }
}
