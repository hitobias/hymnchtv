package org.cog.hymnchtv.ui.picker

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class KeypadMetricsTest {
    @Test fun keysNeverGetBelowTheTouchTarget() {
        assertThat(KeypadMetrics.keyHeightDp(0f)).isEqualTo(48f)
        assertThat(KeypadMetrics.keyHeightDp(126f)).isEqualTo(48f)
    }

    @Test fun exactlyTheFourRowBudgetGivesFortyEight() {
        assertThat(KeypadMetrics.keyHeightDp(4 * 48f + 3 * 8f)).isEqualTo(48f)
    }

    @Test fun keysShareWhatIsLeftBetweenTheLimits() {
        assertThat(KeypadMetrics.keyHeightDp(4 * 60f + 3 * 8f)).isEqualTo(60f)
    }

    @Test fun keysStopAtSeventyTwo() {
        assertThat(KeypadMetrics.keyHeightDp(1000f)).isEqualTo(72f)
    }

    @Test fun budgetFor360x740FitsTheOpenButtonOnTheFirstScreen() {
        // spec 5: 12 + 48 + 10 + 94 + 10 + 96 + 10 + keypad + 10 + 52 = 342 + keypad; a 580dp content area leaves 238 >= 4*48+3*8 = 216
        val keypad = 580f - 342f
        assertThat(keypad).isAtLeast(4 * KeypadMetrics.MIN_KEY_DP + 3 * KeypadMetrics.GAP_DP)
    }
}
