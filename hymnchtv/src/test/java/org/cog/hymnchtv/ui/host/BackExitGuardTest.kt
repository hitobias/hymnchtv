package org.cog.hymnchtv.ui.host

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BackExitGuardTest {
    @Test fun firstPressOnlyAsksForAnotherOne() {
        assertThat(BackExitGuard().onBackOnHome(1000)).isFalse()
    }

    @Test fun secondPressWithinTwoSecondsExits() {
        val guard = BackExitGuard()
        guard.onBackOnHome(1000)
        assertThat(guard.onBackOnHome(2999)).isTrue()
    }

    @Test fun secondPressAfterTheWindowStartsOver() {
        val guard = BackExitGuard()
        guard.onBackOnHome(1000)
        assertThat(guard.onBackOnHome(3001)).isFalse()
        assertThat(guard.onBackOnHome(4000)).isTrue()
    }

    @Test fun resetForgetsTheFirstPress() {
        val guard = BackExitGuard()
        guard.onBackOnHome(1000)
        guard.reset()
        assertThat(guard.onBackOnHome(1500)).isFalse()
    }

    @Test fun clockGoingBackwardsNeverExits() {
        val guard = BackExitGuard()
        guard.onBackOnHome(5000)
        assertThat(guard.onBackOnHome(1000)).isFalse()
    }
}
