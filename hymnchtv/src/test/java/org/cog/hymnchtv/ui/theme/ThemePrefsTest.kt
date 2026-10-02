package org.cog.hymnchtv.ui.theme

import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.utils.ThemeHelper
import org.junit.Test

class ThemePrefsTest {
    @Test fun unknownNameFallsBackToTheA2Default() {
        assertThat(NightMode.from(null)).isEqualTo(NightMode.DEFAULT)
        assertThat(NightMode.from("BOGUS")).isEqualTo(NightMode.DEFAULT)
        assertThat(NightMode.DEFAULT.name).isEqualTo(ThemeHelper.DEFAULT_THEME.name)
    }

    @Test fun storedValuesMap() {
        assertThat(NightMode.from("LIGHT")).isEqualTo(NightMode.LIGHT)
        assertThat(NightMode.from("DARK")).isEqualTo(NightMode.DARK)
        assertThat(NightMode.from("SYSTEM")).isEqualTo(NightMode.SYSTEM)
    }

    @Test fun syncDarkUpdatesTheHelperCache() {
        ThemeHelper.syncDark(true)
        assertThat(ThemeHelper.isAppTheme(ThemeHelper.Theme.DARK)).isTrue()
        ThemeHelper.syncDark(false)
        assertThat(ThemeHelper.isAppTheme(ThemeHelper.Theme.LIGHT)).isTrue()
    }
}
