package org.cog.hymnchtv.ui.lyrics

import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.nav.ViewingCause
import org.junit.Test

/** H5 "D-1 接點" 3: opening and changing a hymn start a reading for the singing log; a recreation does not. */
class ReadingPolicyTest {
    @Test fun openAndChangeStartAReadingRestoreDoesNot() {
        assertThat(ReadingPolicy.startsReading(ViewingCause.OPENED)).isTrue()
        assertThat(ReadingPolicy.startsReading(ViewingCause.CHANGED)).isTrue()
        assertThat(ReadingPolicy.startsReading(ViewingCause.RESTORED)).isFalse()
    }

    @Test fun everyCauseIsDecided() {
        // a cause added later must be decided here on purpose
        assertThat(ViewingCause.values().toSet()).containsExactly(ViewingCause.OPENED, ViewingCause.CHANGED, ViewingCause.RESTORED)
    }
}
