package org.cog.hymnchtv.reading

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ScorePagesTest {
    @Test
    fun fivePagesUseFiveDistinctFiles() {
        // db152 is the 5-page hymn; before A2 page 5 (d.webp) was loaded into page 4's ImageView
        assertThat(ScorePages.fileNames("lyrics_db_score/db152", 5)).containsExactly(
            "lyrics_db_score/db152.webp", "lyrics_db_score/db152a.webp", "lyrics_db_score/db152b.webp",
            "lyrics_db_score/db152c.webp", "lyrics_db_score/db152d.webp",
        ).inOrder()
    }

    @Test
    fun singlePage() {
        assertThat(ScorePages.fileNames("p", 1)).containsExactly("p.webp")
    }

    @Test
    fun outOfRangeCountsAreClamped() {
        assertThat(ScorePages.fileNames("p", 0)).containsExactly("p.webp")
        assertThat(ScorePages.fileNames("p", -3)).containsExactly("p.webp")
        assertThat(ScorePages.fileNames("p", 9)).hasSize(ScorePages.MAX_PAGES)
    }
}
