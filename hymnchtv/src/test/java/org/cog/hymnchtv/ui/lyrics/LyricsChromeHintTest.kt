package org.cog.hymnchtv.ui.lyrics

import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.reading.FakeSharedPreferences
import org.junit.Test

/** Plan 6c: the "tap the middle to show the toolbar" hint appears once, ever. */
class LyricsChromeHintTest {
    @Test
    fun shownOnceThenNeverAgain() {
        val prefs = FakeSharedPreferences()
        assertThat(LyricsChromeHint.shouldShow(prefs)).isTrue()
        LyricsChromeHint.markShown(prefs)
        assertThat(LyricsChromeHint.shouldShow(prefs)).isFalse()
    }

    @Test
    fun aBrokenStoredValueDoesNotRepeatTheHint() {
        val prefs = FakeSharedPreferences(mapOf(LyricsChromeHint.PREF_KEY to "oops"))
        assertThat(LyricsChromeHint.shouldShow(prefs)).isFalse()
    }
}
