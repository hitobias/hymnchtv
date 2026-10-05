package org.cog.hymnchtv.nav

import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.hymn.HymnRef
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.junit.Test

class JumpStateTest {
    private val db5 = HymnRef(HymnTypes.DB, 5)
    private val db100 = HymnRef(HymnTypes.DB, 100)
    private val bb37 = HymnRef(HymnTypes.BB, 37)
    private val xg12 = HymnRef(HymnTypes.XG, 12)
    private fun entry(ref: HymnRef, y: Int = 0) = JumpEntry(ref, ReadingPosition(y, 1000))

    @Test fun emptyCannotReturn() {
        assertThat(JumpState.EMPTY.canReturn()).isFalse()
        assertThat(JumpState.EMPTY.popRecent(0)).isNull()
    }

    @Test fun pushThenPopGivesTheNewestEntryBack() {
        val state = JumpState.EMPTY.push(entry(db5, 10)).push(entry(db100, 20))
        val popped = state.popRecent(0)!!
        assertThat(popped.entry).isEqualTo(entry(db100, 20))
        assertThat(popped.state.recentFirst()).containsExactly(entry(db5, 10))
    }

    @Test fun pushIsImmutable() {
        val before = JumpState.EMPTY.push(entry(db5))
        before.push(entry(db100))
        assertThat(before.recentFirst()).containsExactly(entry(db5))
    }

    @Test fun theStackKeepsTheNewestTen() {
        var state = JumpState.EMPTY
        for (no in 1..12) state = state.push(entry(HymnRef(HymnTypes.DB, no)))
        assertThat(state.stack).hasSize(JumpState.MAX_STACK)
        assertThat(state.recentFirst().first().ref).isEqualTo(HymnRef(HymnTypes.DB, 12))
        assertThat(state.recentFirst().last().ref).isEqualTo(HymnRef(HymnTypes.DB, 3))
    }

    @Test fun theSameHymnOnTopIsReplacedNotDoubled() {
        val state = JumpState.EMPTY.push(entry(db5, 10)).push(entry(db5, 99))
        assertThat(state.recentFirst()).containsExactly(entry(db5, 99))
    }

    @Test fun popRecentDropsThatEntryAndEveryNewerOne() {
        val state = JumpState.EMPTY.push(entry(db5)).push(entry(db100)).push(entry(bb37))
        val popped = state.popRecent(1)!!
        assertThat(popped.entry.ref).isEqualTo(db100)
        assertThat(popped.state.recentFirst().map { it.ref }).containsExactly(db5)
        assertThat(state.popRecent(3)).isNull()
        assertThat(state.popRecent(-1)).isNull()
    }

    @Test fun theSlotIsSetOverwrittenAndClearedAndSurvivesPushAndPop() {
        val withSlot = JumpState.EMPTY.withSlot(xg12)
        assertThat(withSlot.slot).isEqualTo(xg12)
        assertThat(withSlot.withSlot(bb37).slot).isEqualTo(bb37)
        assertThat(withSlot.withSlot(null).slot).isNull()
        val pushed = withSlot.push(entry(db5))
        assertThat(pushed.slot).isEqualTo(xg12)
        assertThat(pushed.popRecent(0)!!.state.slot).isEqualTo(xg12)
    }

    @Test fun encodeDecodeRoundTripWithAppendixHymnsAndSlot() {
        val state = JumpState.EMPTY
            .push(entry(HymnRef(HymnTypes.DB, 781), 40))
            .push(entry(HymnRef(HymnTypes.YB, 276), 0))
            .push(entry(bb37, 1234))
            .withSlot(xg12)
        assertThat(JumpState.decode(state.encode())).isEqualTo(state)
        assertThat(JumpState.decode(JumpState.EMPTY.encode())).isEqualTo(JumpState.EMPTY)
    }

    @Test fun decodeNeverThrowsAndFallsBackToEmpty() {
        listOf(null, "", "garbage", "j0|-", "j1", "|||").forEach {
            assertThat(JumpState.decode(it)).isEqualTo(JumpState.EMPTY)
        }
    }

    @Test fun decodeDropsEntriesThatNameNoRealHymn() {
        val text = "j1|hymn_bb,2000|hymn_db,9999,0,0|nobook,1,0,0|hymn_db,5,10,100|hymn_db,6,x,1"
        val state = JumpState.decode(text)
        assertThat(state.slot).isNull()
        assertThat(state.recentFirst()).containsExactly(JumpEntry(db5, ReadingPosition(10, 100)))
    }

    @Test fun decodeKeepsAtMostTen() {
        val text = "j1|-" + (1..15).joinToString("") { "|hymn_db,$it,0,0" }
        val state = JumpState.decode(text)
        assertThat(state.stack).hasSize(JumpState.MAX_STACK)
        assertThat(state.recentFirst().first().ref).isEqualTo(HymnRef(HymnTypes.DB, 15))
    }
}
