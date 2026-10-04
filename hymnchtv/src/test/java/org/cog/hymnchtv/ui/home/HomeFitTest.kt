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
        assertThat(p.keyHeightDp).isEqualTo(64f)
    }

    @Test fun keysNeverExceedSixtyFour() {
        for (h in 480..1400 step 13) {
            val p = plan(available = h.toFloat())
            if (!p.scrollable) assertThat(p.keyHeightDp).isAtMost(HomeFit.MAX_KEY_DP)
        }
        assertThat(HomeFit.MAX_KEY_DP).isEqualTo(64f)
    }

    @Test fun threeRecentRowsAreFilledBeforeTheKeysGrow() {
        val min = FIXED + 48f + HomeFit.KEYPAD_MIN_DP + HomeFit.SAFETY_DP
        val p = plan(available = min + HomeFit.RECENT_ROW_DP * 3 + 8f)
        assertThat(p.recentCount).isEqualTo(3)
        assertThat(p.keyHeightDp).isEqualTo(48f + 2f)
    }

    @Test fun keysGrowToSixtyFourBeforeTheFourthRowIsAdded() {
        val min = FIXED + 48f + HomeFit.KEYPAD_MIN_DP + HomeFit.SAFETY_DP + HomeFit.RECENT_ROW_DP * 3
        val mid = plan(available = min + 40f)
        assertThat(mid.recentCount).isEqualTo(3)
        assertThat(mid.keyHeightDp).isEqualTo(48f + 10f)
        val full = plan(available = min + 64f + HomeFit.RECENT_ROW_DP)
        assertThat(full.keyHeightDp).isEqualTo(64f)
        assertThat(full.recentCount).isEqualTo(4)
    }

    @Test fun fewerRowsThanTheTargetWhenTheHistoryIsShort() {
        val p = plan(available = 900f, recent = 2)
        assertThat(p.recentCount).isEqualTo(2)
        assertThat(p.keyHeightDp).isEqualTo(64f)
    }

    @Test fun recentCountNeverExceedsWhatTheHistoryHolds() {
        assertThat(plan(available = 900f, recent = 2).recentCount).isEqualTo(2)
        assertThat(plan(available = 900f, recent = 9).recentCount).isEqualTo(HomeFit.MAX_RECENT)
    }

    @Test fun smallerScreenDropsRecentRowsBeforeKeysShrinkBelow48() {
        val p = plan(available = FIXED + 48f + HomeFit.KEYPAD_MIN_DP + HomeFit.SAFETY_DP + HomeFit.RECENT_ROW_DP * 2 + 1f)
        assertThat(p.scrollable).isFalse()
        assertThat(p.recentCount).isEqualTo(2)
        assertThat(p.keyHeightDp).isAtMost(48.5f)
    }

    @Test fun recentGoesToZeroButTheHeaderStaysWhenOnlyTheKeysFit() {
        val p = plan(available = FIXED + 48f + HomeFit.KEYPAD_MIN_DP + HomeFit.SAFETY_DP + 10f)
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

    @Test fun screen411x891ShowsAtLeastThreeRecentRows() {
        // Same screen: the fixed parts of the real page measure about 354dp (search, books, preview, open and margins)
        val p = plan(available = 779f, fixed = 354f)
        assertThat(p.scrollable).isFalse()
        assertThat(p.recentCount).isAtLeast(3)
    }

    @Test fun screen360x640ScrollsBecauseTheControlsNeedMoreThanItHas() {
        // 640 - 24 - 48 - 48 - 24 = 496 of content, but the controls alone need 354 + 48 + 216
        assertThat(plan(available = 496f, fixed = 354f).scrollable).isTrue()
    }

    @Test fun largeFontGrowsTheFixedPartAndForcesScrolling() {
        assertThat(plan(available = 779f, fixed = 520f).scrollable).isTrue()
    }

    @Test fun emptyHistoryShowsTheEmptyStateOnlyWhenItFits() {
        val tight = FIXED + 48f + HomeFit.KEYPAD_MIN_DP + HomeFit.SAFETY_DP + 64f
        assertThat(plan(available = tight, recent = 0).showEmpty).isFalse()
        val roomy = tight + HomeFit.EMPTY_BLOCK_DP
        val p = plan(available = roomy, recent = 0)
        assertThat(p.showEmpty).isTrue()
        assertThat(p.recentCount).isEqualTo(0)
    }

    @Test fun keysGrowMonotonicallyWhileTheRowCountStaysTheSame() {
        var lastKey = 0f
        var lastCount = -1
        for (h in 480..900 step 2) {
            val p = plan(available = h.toFloat())
            if (p.scrollable) continue
            if (p.recentCount == lastCount) assertThat(p.keyHeightDp).isAtLeast(lastKey)
            lastKey = p.keyHeightDp
            lastCount = p.recentCount
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

    @Test fun theHeaderWithTheHistoryEntryIsAlwaysPartOfTheBase() {
        val exact = FIXED + 48f + HomeFit.KEYPAD_MIN_DP + HomeFit.SAFETY_DP
        assertThat(plan(available = exact, recent = 0).scrollable).isFalse()
        assertThat(plan(available = exact - 1f, recent = 0).scrollable).isTrue()
    }

    @Test fun whenNoRowFitsTheHeaderStillStaysAndNothingIsDropped() {
        val exact = FIXED + 48f + HomeFit.KEYPAD_MIN_DP + HomeFit.SAFETY_DP
        val p = plan(available = exact, recent = 12)
        assertThat(p.scrollable).isFalse()
        assertThat(p.recentCount).isEqualTo(0)
        val tiny = plan(available = 100f, recent = 12)
        assertThat(tiny.scrollable).isTrue()
        assertThat(tiny.recentCount).isEqualTo(HomeFit.MAX_RECENT)
    }
}
