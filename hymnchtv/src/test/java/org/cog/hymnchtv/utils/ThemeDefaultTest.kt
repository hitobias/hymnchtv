package org.cog.hymnchtv.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

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

    @Test
    fun mainActivityReadsThePrefWithTheSharedDefault() {
        val root = File(checkNotNull(System.getProperty("hymnchtv.repoRoot")) { "hymnchtv.repoRoot not set" })
        val source = File(root, "hymnchtv/src/main/java/org/cog/hymnchtv/MainActivity.java").readText()
        assertThat(source).contains("getString(PREF_THEME, ThemeHelper.DEFAULT_THEME.toString())")
        assertThat(source).doesNotContain("getString(PREF_THEME, Theme.DARK.toString())")
    }
}
