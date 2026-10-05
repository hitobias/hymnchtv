package org.cog.hymnchtv.ui.playlist

import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.notebook.model.NotebookValidation
import org.junit.Test

class PlaylistNamesTest {
    @Test fun namesAreTrimmed() {
        assertThat(PlaylistNames.normalized("  主日聚會 ")).isEqualTo("主日聚會")
    }

    @Test fun blankOrTooLongNamesAreRejectedLikeTheRepositoryDoes() {
        assertThat(PlaylistNames.normalized("   ")).isNull()
        assertThat(PlaylistNames.normalized("x".repeat(NotebookValidation.MAX_PLAYLIST_NAME_LENGTH))).isNotNull()
        assertThat(PlaylistNames.normalized("x".repeat(NotebookValidation.MAX_PLAYLIST_NAME_LENGTH + 1))).isNull()
        assertThat(PlaylistNames.MAX).isEqualTo(NotebookValidation.MAX_PLAYLIST_NAME_LENGTH)
    }
}
