package org.cog.hymnchtv.notebook.settings

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** User decision 2026-10-05 (D-1 F4): the automatic singing log is off until the user turns it on in Settings. */
class NotebookPrefsDefaultsTest {
    @Test fun automaticSingingLogIsOffByDefault() {
        assertThat(NotebookPrefs.DEFAULT_AUTO_RECORD).isFalse()
    }
}
