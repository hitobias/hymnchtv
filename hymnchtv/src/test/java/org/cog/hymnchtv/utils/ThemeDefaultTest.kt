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
    fun themePrefsUsesTheSharedDefault() {
        // C-0: the preference is read by ThemePrefs (applied in HymnsApp), whose default is ThemeHelper.DEFAULT_THEME
        val root = File(checkNotNull(System.getProperty("hymnchtv.repoRoot")) { "hymnchtv.repoRoot not set" })
        val source = File(root, "hymnchtv/src/main/java/org/cog/hymnchtv/ui/theme/ThemePrefs.kt").readText()
        assertThat(source).contains("val DEFAULT = LIGHT")
        assertThat(org.cog.hymnchtv.ui.theme.NightMode.DEFAULT.name).isEqualTo(ThemeHelper.DEFAULT_THEME.name)
    }
}
