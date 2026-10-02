package org.cog.hymnchtv.notebook.backup

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.TimeZone

class BackupFileNameTest {
    private val instant = 1_790_733_600_000L // 2026-09-30 02:00 UTC

    @Test
    fun usesLocalDateAndTime() {
        assertThat(BackupFileName.suggested(instant, TimeZone.getTimeZone("UTC")))
            .isEqualTo("hymnchtv-notebook-20260930-0200.json")
        assertThat(BackupFileName.suggested(instant, TimeZone.getTimeZone("Asia/Taipei")))
            .isEqualTo("hymnchtv-notebook-20260930-1000.json")
    }
}
