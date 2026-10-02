package org.cog.hymnchtv.lyrics

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LyricsAssetsTest {
    @Test
    fun mapsSimplifiedPathToVariantDir() {
        assertThat(LyricsAssets.hantPath("lyrics_db_text/db1.txt", HantVariant.TW))
            .isEqualTo("lyrics_db_text_hant_tw/db1.txt")
        assertThat(LyricsAssets.hantPath("lyrics_er_text/er12.txt", HantVariant.HK))
            .isEqualTo("lyrics_er_text_hant_hk/er12.txt")
    }

    @Test
    fun rejectsPathsWithoutDirectory() {
        assertThat(LyricsAssets.hantPath("db1.txt", HantVariant.TW)).isNull()
        assertThat(LyricsAssets.hantPath("", HantVariant.TW)).isNull()
    }
}
