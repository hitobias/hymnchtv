package org.cog.hymnchtv.update

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class InstallGateTest {
    @Test
    fun asksForUnknownSourcesOnlyOnOreoAndLaterWhenNotAllowed() {
        assertThat(InstallGate.nextStep(26, false)).isEqualTo(InstallStep.ALLOW_UNKNOWN_SOURCES)
        assertThat(InstallGate.nextStep(35, false)).isEqualTo(InstallStep.ALLOW_UNKNOWN_SOURCES)
        assertThat(InstallGate.nextStep(26, true)).isEqualTo(InstallStep.LAUNCH_INSTALLER)
        assertThat(InstallGate.nextStep(25, false)).isEqualTo(InstallStep.LAUNCH_INSTALLER)
    }
}
