package org.cog.hymnchtv.reading

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ScoreTintTest {
    private val white = 0xFFFFFFFF.toInt()
    private val black = 0xFF000000.toInt()
    private val paper = 0xFF24211D.toInt()   // 夜讀 base
    private val ink = 0xFFECE4D6.toInt()     // 夜讀 text

    @Test
    fun levelZeroOnLightBackgroundLeavesTheScoreAlone() {
        val tint = ScoreTintPolicy.resolve(0, false, paper, ink)
        assertThat(tint).isEqualTo(ScoreTint.None)
        assertThat(ScoreTintPolicy.matrix(tint)).isNull()
    }

    @Test
    fun levelZeroOnDarkBackgroundMapsPaperAndInk() {
        val tint = ScoreTintPolicy.resolve(0, true, paper, ink)
        assertThat(tint).isEqualTo(ScoreTint.Duotone(paper, ink))
        val m = ScoreTintPolicy.matrix(tint)!!
        assertThat(ScoreTintPolicy.applyTo(m, white)).isEqualTo(paper)
        assertThat(ScoreTintPolicy.applyTo(m, black)).isEqualTo(ink)
    }

    @Test
    fun manualLevelsKeepTheOldInversion() {
        val m = ScoreTintPolicy.matrix(ScoreTintPolicy.resolve(1, false, paper, ink))!!
        assertThat(ScoreTintPolicy.applyTo(m, white)).isEqualTo(0xFF1A1A1A.toInt())   // 255 - 0.9 * 255 = 25.5 -> 26
        assertThat(ScoreTintPolicy.applyTo(m, black)).isEqualTo(white)
        assertThat(ScoreTintPolicy.resolve(3, true, paper, ink)).isEqualTo(ScoreTint.Invert(-0.7f))
    }

    @Test
    fun unknownLevelsFallBackToAutomatic() {
        assertThat(ScoreTintPolicy.resolve(7, false, paper, ink)).isEqualTo(ScoreTint.None)
        assertThat(ScoreTintPolicy.resolve(-1, true, paper, ink)).isEqualTo(ScoreTint.Duotone(paper, ink))
    }

    @Test
    fun levelCountMatchesTheMenuCycle() {
        assertThat(ScoreTintPolicy.LEVEL_COUNT).isEqualTo(4)
    }
}
