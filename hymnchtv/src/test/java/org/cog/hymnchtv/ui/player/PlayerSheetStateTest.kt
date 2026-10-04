package org.cog.hymnchtv.ui.player

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlayerSheetStateTest {
    private val start = PlayerSheetState(collapsed = false)

    @Test
    fun defaultStartsCollapsedAsTheCapsule() {
        val fresh = PlayerSheetState()
        assertThat(fresh.collapsed).isTrue()
        assertThat(fresh.userHidden).isFalse()
        assertThat(fresh.display(portrait = true)).isEqualTo(SheetDisplay.CAPSULE)
        assertThat(fresh.expand(portrait = true).display(true)).isEqualTo(SheetDisplay.CARD)
    }

    @Test
    fun expandedStateShowsTheCardInPortrait() {
        assertThat(start.display(portrait = true)).isEqualTo(SheetDisplay.CARD)
    }

    @Test
    fun collapseAndExpandInPortraitOnlyTouchCollapsed() {
        val collapsed = start.collapse(portrait = true)
        assertThat(collapsed.collapsed).isTrue()
        assertThat(collapsed.display(true)).isEqualTo(SheetDisplay.CAPSULE)
        assertThat(collapsed.expand(true)).isEqualTo(start)
        assertThat(start.landscapeExpanded).isFalse()
    }

    @Test
    fun landscapeShowsTheCapsuleWithoutChangingCollapsed() {
        assertThat(start.display(portrait = false)).isEqualTo(SheetDisplay.CAPSULE)
        assertThat(start.collapse(true).display(portrait = false)).isEqualTo(SheetDisplay.CAPSULE)
    }

    @Test
    fun landscapeExpandIsTemporaryAndLeavesCollapsedAlone() {
        val collapsed = start.collapse(true)
        val expanded = collapsed.expand(portrait = false)
        assertThat(expanded.display(false)).isEqualTo(SheetDisplay.CARD)
        assertThat(expanded.collapsed).isTrue()
        assertThat(expanded.collapse(portrait = false).display(false)).isEqualTo(SheetDisplay.CAPSULE)
        val back = expanded.onOrientationChanged(portrait = true)
        assertThat(back.landscapeExpanded).isFalse()
        assertThat(back.display(true)).isEqualTo(SheetDisplay.CAPSULE)
    }

    @Test
    fun portraitOrientationAlwaysClearsLandscapeExpansion() {
        val expanded = start.collapse(true).expand(portrait = false)
        val portrait = PlayerSheetState.isPortrait(android.content.res.Configuration.ORIENTATION_PORTRAIT)
        val landscape = PlayerSheetState.isPortrait(android.content.res.Configuration.ORIENTATION_LANDSCAPE)
        assertThat(portrait).isTrue()
        assertThat(landscape).isFalse()
        assertThat(expanded.onOrientationChanged(portrait).landscapeExpanded).isFalse()
        assertThat(expanded.onOrientationChanged(landscape).landscapeExpanded).isTrue()
    }

    @Test
    fun landscapeRoundTripKeepsAnExpandedCard() {
        val landscape = start.onOrientationChanged(portrait = false)
        assertThat(landscape.display(false)).isEqualTo(SheetDisplay.CAPSULE)
        assertThat(landscape.onOrientationChanged(true).display(true)).isEqualTo(SheetDisplay.CARD)
    }

    @Test
    fun userHiddenWinsEverywhereAndRestoresTheForm() {
        val hidden = start.collapse(true).toggleUserHidden()
        assertThat(hidden.display(true)).isEqualTo(SheetDisplay.HIDDEN)
        assertThat(hidden.display(false)).isEqualTo(SheetDisplay.HIDDEN)
        assertThat(hidden.expand(false).display(false)).isEqualTo(SheetDisplay.HIDDEN)
        assertThat(hidden.toggleUserHidden().display(true)).isEqualTo(SheetDisplay.CAPSULE)
    }

    @Test
    fun videoHidesTheLayer() {
        assertThat(start.display(true, videoActive = true)).isEqualTo(SheetDisplay.HIDDEN)
    }

    @Test
    fun stateIsImmutable() {
        val before = start.copy()
        start.collapse(true); start.toggleUserHidden(); start.expand(false)
        assertThat(start).isEqualTo(before)
    }

    @Test
    fun snapByDistance() {
        assertThat(PlayerSheetState.shouldCollapse(81f, 200f, 0f, 1f)).isTrue()
        assertThat(PlayerSheetState.shouldCollapse(80f, 200f, 0f, 1f)).isFalse()
        assertThat(PlayerSheetState.shouldCollapse(10f, 200f, 0f, 1f)).isFalse()
    }

    @Test
    fun snapByVelocityScalesWithDensity() {
        assertThat(PlayerSheetState.shouldCollapse(5f, 200f, 1001f, 1f)).isTrue()
        assertThat(PlayerSheetState.shouldCollapse(5f, 200f, 1001f, 2f)).isFalse()
        assertThat(PlayerSheetState.shouldCollapse(5f, 200f, 2001f, 2f)).isTrue()
        assertThat(PlayerSheetState.shouldCollapse(5f, 200f, -3000f, 1f)).isFalse()
    }

    @Test
    fun capsuleExpandsOnUpwardDragOrFling() {
        assertThat(PlayerSheetState.shouldExpand(41f, 0f, 1f)).isTrue()
        assertThat(PlayerSheetState.shouldExpand(10f, 0f, 1f)).isFalse()
        assertThat(PlayerSheetState.shouldExpand(10f, 1500f, 1f)).isTrue()
    }

    @Test
    fun capsuleFormFollowsPlayback() {
        assertThat(PlayerSheetState.capsuleForm(PlaybackUiState())).isEqualTo(CapsuleForm.NOTE)
        assertThat(PlayerSheetState.capsuleForm(PlaybackUiState(active = true))).isEqualTo(CapsuleForm.PLAYBACK)
        assertThat(PlayerSheetState.capsuleForm(PlaybackUiState(isPlaying = true, active = true))).isEqualTo(CapsuleForm.PLAYBACK)
    }

    @Test
    fun reserveByDisplay() {
        assertThat(PlayerSheetState.reserve(SheetDisplay.HIDDEN, 300, 2f)).isEqualTo(0)
        assertThat(PlayerSheetState.reserve(SheetDisplay.CARD, 300, 2f)).isEqualTo(300)
        assertThat(PlayerSheetState.reserve(SheetDisplay.CAPSULE, 300, 2f)).isEqualTo(144)
    }

    @Test
    fun progressIsClamped() {
        assertThat(PlaybackUiState(positionMs = 50, durationMs = 200).progress).isWithin(0.001f).of(0.25f)
        assertThat(PlaybackUiState(positionMs = 500, durationMs = 200).progress).isEqualTo(1f)
        assertThat(PlaybackUiState(positionMs = 5, durationMs = 0).progress).isEqualTo(0f)
    }
}
