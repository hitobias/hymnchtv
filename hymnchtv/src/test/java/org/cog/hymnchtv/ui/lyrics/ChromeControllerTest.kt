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

    private fun showFor(chromeUnderTest: ChromeController = chrome) {
        chromeUnderTest.start()
        chromeUnderTest.toggle()
    }

    @Test
    fun startsHiddenWithNoTimerAndNoCallback() {
        chrome.start()
        assertThat(chrome.isVisible).isFalse()
        assertThat(timer.pendingCount).isEqualTo(0)
        timer.advance(60_000)
        assertThat(chrome.isVisible).isFalse()
        assertThat(changes).isEmpty()
    }

    @Test
    fun centreTapShowsThenHidesAfterFourSeconds() {
        chrome.start()
        chrome.toggle()
        assertThat(chrome.isVisible).isTrue()
        timer.advance(3_999)
        assertThat(chrome.isVisible).isTrue()
        timer.advance(1)
        assertThat(chrome.isVisible).isFalse()
        assertThat(changes).containsExactly(true, false).inOrder()
    }

    @Test
    fun toggleWhileVisibleHidesAtOnceAndCancelsTheTimer() {
        showFor()
        chrome.toggle()
        assertThat(chrome.isVisible).isFalse()
        timer.advance(10_000)
        assertThat(changes).containsExactly(true, false).inOrder()
    }

    @Test
    fun interactionRestartsTheFourSecondTimer() {
        showFor()
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
        chrome.onInteraction()
        assertThat(chrome.isVisible).isFalse()
    }

    @Test
    fun heldWhileASheetOrMenuIsOpenThenRestartsOnRelease() {
        showFor()
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
        chrome.setHeld(true)
        chrome.setHeld(false)
        assertThat(chrome.isVisible).isFalse()
    }

    @Test
    fun touchExplorationKeepsEverythingVisible() {
        chrome.start()
        chrome.setAlwaysVisible(true)
        assertThat(chrome.isVisible).isTrue()
        timer.advance(60_000)
        assertThat(chrome.isVisible).isTrue()
        chrome.toggle()
        assertThat(chrome.isVisible).isTrue()
    }

    @Test
    fun touchExplorationOnBeforeStartStillOpensVisible() {
        chrome.setAlwaysVisible(true)
        chrome.start()
        timer.advance(60_000)
        assertThat(chrome.isVisible).isTrue()
    }

    @Test
    fun leavingTouchExplorationRestartsTheTimer() {
        chrome.start()
        chrome.setAlwaysVisible(true)
        chrome.setAlwaysVisible(false)
        timer.advance(3_999)
        assertThat(chrome.isVisible).isTrue()
        timer.advance(1)
        assertThat(chrome.isVisible).isFalse()
    }

    @Test
    fun startTwiceStaysHiddenWithoutTimers() {
        chrome.start()
        chrome.start()
        assertThat(chrome.isVisible).isFalse()
        assertThat(timer.pendingCount).isEqualTo(0)
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
        assertThat(chrome.isVisible).isTrue()
        timer.advance(3_999)
        assertThat(chrome.isVisible).isTrue()
        timer.advance(1)
        assertThat(chrome.isVisible).isFalse()
    }

    @Test
    fun afterReleaseNothingIsScheduledAgain() {
        showFor()
        chrome.setHeld(true)
        chrome.release()
        chrome.setHeld(false) // the Aa sheet or menu is dismissed after the activity is gone
        chrome.onInteraction()
        chrome.toggle()
        chrome.restore(true)
        chrome.setAlwaysVisible(false)
        assertThat(timer.pendingCount).isEqualTo(0)
    }
}

/** Deterministic [ChromeTimer]: time only moves in [advance]. */
class FakeChromeTimer : ChromeTimer {
    private data class Task(val at: Long, val action: Runnable)

    private var now = 0L
    private val tasks = mutableListOf<Task>()

    val pendingCount: Int get() = tasks.size

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
