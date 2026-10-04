package org.cog.hymnchtv.ui.home

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.cog.hymnchtv.ui.picker.KeypadMetrics
import org.junit.Test

/** The one-page home plan: how many recent rows and how tall the keys are for a given height. */
class HomeFitTest {
    private fun plan(available: Float, fixed: Float = FIXED, recent: Int = 5, header: Float = 48f, saving: Float = 0f) =
        HomeFit.plan(HomeFit.Input(availableDp = available, fixedDp = fixed, recentHeaderDp = header, recentTotal = recent, compactSavingDp = saving))

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

    @Test fun keysShrinkBelow48DownToTheFloorBeforeAnythingScrolls() {
        val p = plan(available = FIXED + 48f + HomeFit.KEYPAD_MIN_DP - 1f + HomeFit.SAFETY_DP)
        assertThat(p.scrollable).isFalse()
        assertThat(p.compact).isFalse()
        assertThat(p.keyHeightDp).isLessThan(48f)
        assertThat(p.keyHeightDp).isAtLeast(HomeFit.MIN_KEY_FLOOR_DP)
        assertThat(p.recentCount).isEqualTo(0)
    }

    @Test fun keysNeverShrinkBelowTheFloor() {
        for (h in 200..700 step 3) {
            val p = plan(available = h.toFloat())
            assertThat(p.keyHeightDp).isAtLeast(HomeFit.MIN_KEY_FLOOR_DP)
        }
        assertThat(HomeFit.MIN_KEY_FLOOR_DP).isEqualTo(40f)
    }

    @Test fun thePreviewCompactsBeforeTheRecentHeaderGoesBelowTheFold() {
        val need = FIXED + 48f + HomeFit.keypadDp(40f) + HomeFit.SAFETY_DP
        val p = plan(available = need - 10f, saving = 32f)
        assertThat(p.compact).isTrue()
        assertThat(p.scrollable).isFalse()
        assertThat(p.pinAction).isFalse()
    }

    @Test fun theRecentHeaderMovesBelowTheFoldWhileOpenStaysVisible() {
        val need = FIXED + HomeFit.keypadDp(40f) + HomeFit.SAFETY_DP
        val p = plan(available = need + 1f, saving = 32f)
        assertThat(p.scrollable).isTrue()
        assertThat(p.compact).isTrue()
        assertThat(p.pinAction).isFalse()
        assertThat(p.keyHeightDp).isAtLeast(40f)
    }

    @Test fun whenNothingFitsOpenIsPinned() {
        val p = plan(available = 200f, saving = 32f)
        assertThat(p.scrollable).isTrue()
        assertThat(p.pinAction).isTrue()
        assertThat(p.keyHeightDp).isEqualTo(40f)
    }

    @Test fun landscapeKeepsTheOldFallbackWithoutShrinkingOrPinning() {
        val p = HomeFit.plan(HomeFit.Input(availableDp = 100f, fixedDp = FIXED, recentHeaderDp = 48f, recentTotal = 5, portrait = false))
        assertThat(p.scrollable).isTrue()
        assertThat(p.keyHeightDp).isEqualTo(48f)
        assertThat(p.pinAction).isFalse()
        assertThat(p.compact).isFalse()
    }

    @Test fun openAndKeypadAreNeverCutOffOnTheTargetScreens() {
        for ((name, available, fixed) in TARGET_SCREENS) {
            val p = plan(available = available, fixed = fixed, saving = HomeFit.COMPACT_SAVING_DP)
            val keypad = HomeFit.keypadDp(p.keyHeightDp)
            val saved = if (p.compact) HomeFit.COMPACT_SAVING_DP else 0f
            val upToOpen = fixed - saved + keypad
            val visible = p.pinAction || upToOpen <= available
            assertWithMessage(name).that(visible).isTrue()
            assertWithMessage(name).that(p.keyHeightDp).isAtLeast(40f)
        }
    }

    @Test fun screen320x640NoLongerScrollsOrPinsWhenKeysAndCompactPreviewFit() {
        val p = plan(available = 520f, fixed = 340f, saving = HomeFit.COMPACT_SAVING_DP)
        assertThat(p.pinAction).isFalse()
        assertThat(p.compact).isTrue()
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
        assertThat(plan(available = 779f, fixed = 600f).scrollable).isTrue()
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

        /** Name, content height (screen minus toolbar, status bar, nav inset, padding) and fixed part: 372 at font 1.0, 410 at 1.3. */
        val TARGET_SCREENS = listOf(
            Triple("320x640 @1.0", 520f, 372f),
            Triple("360x640 @1.3", 520f, 410f),
            Triple("360x640 @1.0 3-button nav", 496f, 372f),
        )
    }

    @Test fun theHeaderWithTheHistoryEntryIsAlwaysPartOfTheBase() {
        // Keys may shrink to the floor before anything scrolls; the 48dp header still counts at that point
        val floorKeypad = HomeFit.KEYPAD_MIN_DP - KeypadMetrics.ROWS * (KeypadMetrics.MIN_KEY_DP - HomeFit.MIN_KEY_FLOOR_DP)
        val exact = FIXED + 48f + floorKeypad + HomeFit.SAFETY_DP
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
