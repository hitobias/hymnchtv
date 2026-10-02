package org.cog.hymnchtv.ui.lyrics

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

/** Plan 6c: auto-hide of the lyrics page toolbars, driven by a fake clock. */
class ChromeControllerTest {
    private val timer = FakeChromeTimer()
    private val changes = mutableListOf<Boolean>()
    private lateinit var chrome: ChromeController

    @Before
    fun setUp() {
        chrome = ChromeController(timer) { changes += it }
    }

    @Test
    fun startsVisibleAndFadesAfterThreeSeconds() {
        chrome.start()
        assertThat(chrome.isVisible).isTrue()
        timer.advance(2_999)
        assertThat(chrome.isVisible).isTrue()
        timer.advance(1)
        assertThat(chrome.isVisible).isFalse()
        assertThat(changes).containsExactly(false)
    }

    @Test
    fun centreTapShowsThenHidesAfterFourSeconds() {
        chrome.start()
        timer.advance(3_000)
        chrome.toggle()
        assertThat(chrome.isVisible).isTrue()
        timer.advance(3_999)
        assertThat(chrome.isVisible).isTrue()
        timer.advance(1)
        assertThat(chrome.isVisible).isFalse()
    }

    @Test
    fun toggleWhileVisibleHidesAtOnceAndCancelsTheTimer() {
        chrome.start()
        chrome.toggle()
        assertThat(chrome.isVisible).isFalse()
        timer.advance(10_000)
        assertThat(changes).containsExactly(false)
    }

    @Test
    fun interactionRestartsTheFourSecondTimer() {
        chrome.start()
        timer.advance(2_000)
        chrome.onInteraction()
        timer.advance(3_999)
        assertThat(chrome.isVisible).isTrue()
        timer.advance(1)
        assertThat(chrome.isVisible).isFalse()
    }

    @Test
    fun interactionWhileHiddenDoesNotShow() {
        chrome.start()
        timer.advance(3_000)
        chrome.onInteraction()
        assertThat(chrome.isVisible).isFalse()
    }

    @Test
    fun heldWhileASheetOrMenuIsOpenThenRestartsOnRelease() {
        chrome.start()
        chrome.setHeld(true)
        timer.advance(60_000)
        assertThat(chrome.isVisible).isTrue()
        chrome.setHeld(false)
        timer.advance(3_999)
        assertThat(chrome.isVisible).isTrue()
        timer.advance(1)
        assertThat(chrome.isVisible).isFalse()
    }

    @Test
    fun releasingTheHoldWhileHiddenStaysHidden() {
        chrome.start()
        timer.advance(3_000)
        chrome.setHeld(true)
        chrome.setHeld(false)
        assertThat(chrome.isVisible).isFalse()
    }

    @Test
    fun touchExplorationKeepsEverythingVisible() {
        chrome.start()
        chrome.setAlwaysVisible(true)
        timer.advance(60_000)
        assertThat(chrome.isVisible).isTrue()
        chrome.toggle()
        assertThat(chrome.isVisible).isTrue()
    }

    @Test
    fun touchExplorationShowsWhatWasHiddenAndLeavingItRestartsTheTimer() {
        chrome.start()
        timer.advance(3_000)
        chrome.setAlwaysVisible(true)
        assertThat(chrome.isVisible).isTrue()
        chrome.setAlwaysVisible(false)
        timer.advance(4_000)
        assertThat(chrome.isVisible).isFalse()
    }

    @Test
    fun startTwiceDoesNotStackTimers() {
        chrome.start()
        timer.advance(1_000)
        chrome.start()
        timer.advance(2_999)
        assertThat(chrome.isVisible).isTrue()
        timer.advance(1)
        assertThat(chrome.isVisible).isFalse()
        assertThat(changes).containsExactly(false)
    }

    @Test
    fun restoredStateIsKeptWithoutTheOpeningFade() {
        chrome.restore(visible = false)
        assertThat(chrome.isVisible).isFalse()
        timer.advance(10_000)
        assertThat(changes).isEmpty()
    }

    @Test
    fun restoringVisibleStartsTheFourSecondTimer() {
        chrome.restore(visible = true)
        timer.advance(3_999)
        assertThat(chrome.isVisible).isTrue()
        timer.advance(1)
        assertThat(chrome.isVisible).isFalse()
    }
}

/** Deterministic [ChromeTimer]: time only moves in [advance]. */
class FakeChromeTimer : ChromeTimer {
    private data class Task(val at: Long, val action: Runnable)

    private var now = 0L
    private val tasks = mutableListOf<Task>()

    override fun postDelayed(delayMs: Long, action: Runnable): Any = Task(now + delayMs, action).also { tasks += it }

    override fun cancel(token: Any) {
        tasks.remove(token)
    }

    fun advance(ms: Long) {
        val end = now + ms
        while (true) {
            val next = tasks.filter { it.at <= end }.minByOrNull { it.at } ?: break
            tasks.remove(next)
            now = next.at
            next.action.run()
        }
        now = end
    }
}
