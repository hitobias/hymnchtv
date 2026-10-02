package org.cog.hymnchtv.notebook.backup;

import static com.google.common.truth.Truth.assertThat;

import org.cog.hymnchtv.MainActivity;
import org.junit.Test;

/** The backup rules name the legacy settings file literally; fail if MainActivity renames it. */
public class SettingsPrefsNameTest {
    public static final String SETTINGS_FILE = "Settings";

    @Test
    public void settingsFileMatchesMainActivity() {
        assertThat(MainActivity.PREF_SETTINGS).isEqualTo(SETTINGS_FILE);
    }
}
