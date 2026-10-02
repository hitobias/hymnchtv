package org.cog.hymnchtv.notebook.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NotebookValidationTest {
    private fun rejects(block: () -> Unit) =
        assertThat(runCatching(block).exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)

    @Test
    fun uuidMustBeCanonicalLowercase() {
        val ok = "123e4567-e89b-12d3-a456-426614174000"
        assertThat(NotebookValidation.uuid(ok)).isEqualTo(ok)
        listOf(ok.uppercase(), "1-1-1-1-1", "", " ", "../../etc/passwd", "$ok ", ok + "0", "x".repeat(500))
            .forEach { rejects { NotebookValidation.uuid(it) } }
    }

    @Test
    fun noteBodyKeepsTextAsIs() {
        assertThat(NotebookValidation.noteBody("  感謝主\n")).isEqualTo("  感謝主\n")
        assertThat(NotebookValidation.noteBody("字".repeat(NotebookValidation.MAX_NOTE_LENGTH))).hasLength(100_000)
        rejects { NotebookValidation.noteBody("   ") }
        rejects { NotebookValidation.noteBody("字".repeat(NotebookValidation.MAX_NOTE_LENGTH + 1)) }
    }

    @Test
    fun playlistNameIsTrimmed() {
        assertThat(NotebookValidation.playlistName("  主日 10/4 ")).isEqualTo("主日 10/4")
        assertThat(NotebookValidation.MAX_PLAYLIST_NAME_LENGTH).isEqualTo(200)
        rejects { NotebookValidation.playlistName("  ") }
        rejects { NotebookValidation.playlistName("a".repeat(NotebookValidation.MAX_PLAYLIST_NAME_LENGTH + 1)) }
    }

    @Test
    fun timeRange() {
        NotebookValidation.timeRange(1, 1)
        rejects { NotebookValidation.timeRange(2, 1) }
    }

    @Test
    fun sungAtMustBeBetweenZeroAndTwentyFourHoursAhead() {
        val now = 1_000_000L
        val day = NotebookValidation.MAX_FUTURE_SKEW_MILLIS
        assertThat(NotebookValidation.sungAt(0, now)).isEqualTo(0L)
        assertThat(NotebookValidation.sungAt(now + day, now)).isEqualTo(now + day)
        rejects { NotebookValidation.sungAt(-1, now) }
        rejects { NotebookValidation.sungAt(now + day + 1, now) }
        rejects { NotebookValidation.sungAt(Long.MAX_VALUE, now) }
    }
}
