package org.cog.hymnchtv.ui.home

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** The one-page home plan: how many recent rows and how tall the keys are for a given height. */
class HomeFitTest {
    private fun plan(available: Float, fixed: Float = FIXED, recent: Int = 5, header: Float = 48f) =
        HomeFit.plan(HomeFit.Input(availableDp = available, fixedDp = fixed, recentHeaderDp = header, recentTotal = recent))

    @Test fun tallScreenShowsFiveRecentAndTallestKeys() {
        val p = plan(available = 900f)
        assertThat(p.scrollable).isFalse()
        assertThat(p.recentCount).isEqualTo(5)
        assertThat(p.keyHeightDp).isEqualTo(72f)
    }

    @Test fun keysGrowToSeventyTwoBeforeRecentRowsAreAdded() {
        val min = FIXED + 48f + HomeFit.KEYPAD_MIN_DP + HomeFit.SAFETY_DP
        val mid = plan(available = min + 60f)
        assertThat(mid.keyHeightDp).isEqualTo(48f + 15f)
        assertThat(mid.recentCount).isEqualTo(0)
        val full = plan(available = min + 96f + HomeFit.RECENT_ROW_DP)
        assertThat(full.keyHeightDp).isEqualTo(72f)
        assertThat(full.recentCount).isEqualTo(1)
    }

    @Test fun recentCountNeverExceedsWhatTheHistoryHolds() {
        assertThat(plan(available = 900f, recent = 2).recentCount).isEqualTo(2)
        assertThat(plan(available = 900f, recent = 9).recentCount).isEqualTo(HomeFit.MAX_RECENT)
    }

    @Test fun smallerScreenDropsRecentRowsBeforeKeysShrinkBelow48() {
        val p = plan(available = FIXED + 48f + HomeFit.KEYPAD_MIN_DP + HomeFit.SAFETY_DP + 96f + HomeFit.RECENT_ROW_DP * 2 + 1f)
        assertThat(p.scrollable).isFalse()
        assertThat(p.recentCount).isEqualTo(2)
        assertThat(p.keyHeightDp).isEqualTo(72f)
    }

    @Test fun recentGoesToZeroButTheHeaderStaysWhenOnlyTheKeysFit() {
        val p = plan(available = FIXED + 48f + HomeFit.KEYPAD_MIN_DP + HomeFit.SAFETY_DP + 96f + 10f)
        assertThat(p.scrollable).isFalse()
        assertThat(p.recentCount).isEqualTo(0)
    }

    @Test fun whenTheKeysAt48DoNotFitThePageScrolls() {
        val p = plan(available = FIXED + 48f + HomeFit.KEYPAD_MIN_DP - 1f)
        assertThat(p.scrollable).isTrue()
        assertThat(p.keyHeightDp).isEqualTo(48f)
    }

    @Test fun screen411x891FitsWithoutScrolling() {
        // 891 - status 24 - top bar 48 - gesture bar 16 - padding 24 = 779 of content; fixed about 354
        val p = plan(available = 779f, fixed = 354f)
        assertThat(p.scrollable).isFalse()
        assertThat(p.keyHeightDp).isAtLeast(48f)
    }

    @Test fun screen360x640ScrollsBecauseTheControlsNeedMoreThanItHas() {
        // 640 - 24 - 48 - 48 - 24 = 496 of content, but the controls alone need 354 + 48 + 216
        assertThat(plan(available = 496f, fixed = 354f).scrollable).isTrue()
    }

    @Test fun largeFontGrowsTheFixedPartAndForcesScrolling() {
        assertThat(plan(available = 779f, fixed = 520f).scrollable).isTrue()
    }

    @Test fun emptyHistoryShowsTheEmptyStateOnlyWhenItFits() {
        val tight = FIXED + 48f + HomeFit.KEYPAD_MIN_DP + HomeFit.SAFETY_DP + 96f
        assertThat(plan(available = tight, recent = 0).showEmpty).isFalse()
        val roomy = tight + HomeFit.EMPTY_BLOCK_DP
        val p = plan(available = roomy, recent = 0)
        assertThat(p.showEmpty).isTrue()
        assertThat(p.recentCount).isEqualTo(0)
    }

    @Test fun keysGrowMonotonicallyWithHeight() {
        var last = 0f
        for (h in 480..900 step 10) {
            val p = plan(available = h.toFloat())
            if (!p.scrollable) {
                assertThat(p.keyHeightDp).isAtLeast(last)
                last = p.keyHeightDp
            }
        }
    }

    @Test fun planNeverOverflowsTheAvailableHeightWhenNotScrollable() {
        for (h in 400..1000 step 7) {
            val p = plan(available = h.toFloat())
            if (p.scrollable) continue
            val used = FIXED + 48f + HomeFit.keypadDp(p.keyHeightDp) + p.recentCount * HomeFit.RECENT_ROW_DP
            assertThat(used).isAtMost(h - HomeFit.SAFETY_DP + 0.01f)
        }
    }

    private companion object {
        const val FIXED = 240f
    }
}
