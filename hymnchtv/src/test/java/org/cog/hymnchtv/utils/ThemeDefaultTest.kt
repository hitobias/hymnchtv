package org.cog.hymnchtv.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** New users get the light theme (user decision 2026-10-02); MainActivity uses the same constant (Task M1). */
class ThemeDefaultTest {
    @Test
    fun defaultThemeIsLight() {
        assertThat(ThemeHelper.DEFAULT_THEME).isEqualTo(ThemeHelper.Theme.LIGHT)
    }

    @Test
    fun freshProcessStartsWithTheDefault() {
        // No JVM test calls ThemeHelper.setTheme, so the static field still holds its initial value
        assertThat(ThemeHelper.getAppTheme()).isEqualTo(ThemeHelper.DEFAULT_THEME)
    }
}
