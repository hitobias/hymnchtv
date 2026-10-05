package org.cog.hymnchtv.ui.lyrics

import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.ui.player.CapsuleForm
import org.cog.hymnchtv.ui.player.SheetDisplay
import org.junit.Test

class ChromePillLayoutTest {
    private val d = 2f // density: 1dp = 2px

    @Test
    fun besideTheCapsuleCentredAndClearOfIt() {
        val p = ChromePillLayout.place(PillAnchor(SheetDisplay.CAPSULE, capsuleWidthPx = 216, form = CapsuleForm.PLAYBACK),
            reservePx = 144, systemBottomPx = 48, density = d)
        assertThat(p.bottomMargin).isEqualTo(48 + 44)        // systemBottom + 22dp
        assertThat(p.endMargin).isEqualTo(216 + 38)          // capsule width + 19dp
        assertThat(p.startMargin).isEqualTo(10)              // 5dp
        assertThat(p.showModeText).isFalse()                 // playing capsule: icon only
    }

    @Test
    fun noteCapsuleKeepsTheModeText() {
        val p = ChromePillLayout.place(PillAnchor(SheetDisplay.CAPSULE, 112, CapsuleForm.NOTE), 144, 48, d)
        assertThat(p.endMargin).isEqualTo(112 + 38)
        assertThat(p.showModeText).isTrue()
    }

    @Test
    fun aboveTheExpandedCard() {
        val p = ChromePillLayout.place(PillAnchor(SheetDisplay.CARD, 216, CapsuleForm.PLAYBACK), reservePx = 400, systemBottomPx = 48, density = d)
        assertThat(p.bottomMargin).isEqualTo(400 + 48 + 16)  // reserve + systemBottom + 8dp
        assertThat(p.endMargin).isEqualTo(10)
        assertThat(p.showModeText).isTrue()
    }

    @Test
    fun playerHiddenSpansTheWidth() {
        val p = ChromePillLayout.place(PillAnchor(SheetDisplay.HIDDEN, 216, CapsuleForm.PLAYBACK), 0, 48, d)
        assertThat(p.bottomMargin).isEqualTo(48 + 24)        // systemBottom + 12dp
        assertThat(p.endMargin).isEqualTo(10)
        assertThat(p.showModeText).isTrue()
    }

    @Test
    fun negativeInputsCountAsZero() {
        val p = ChromePillLayout.place(PillAnchor(SheetDisplay.CARD, -1, CapsuleForm.NOTE), -5, -3, d)
        assertThat(p.bottomMargin).isEqualTo(16)
    }
}
