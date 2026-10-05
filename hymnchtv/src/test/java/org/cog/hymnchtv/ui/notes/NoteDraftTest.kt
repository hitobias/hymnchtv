package org.cog.hymnchtv.ui.notes

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NoteDraftTest {
    @Test fun aNewNoteCanBeSavedOnceItHasText() {
        assertThat(NoteDraft.canSave("", null)).isFalse()
        assertThat(NoteDraft.canSave(" \n ", null)).isFalse()
        assertThat(NoteDraft.canSave("主日", null)).isTrue()
    }

    @Test fun anUnchangedNoteIsNotDirtyAndCannotBeSaved() {
        assertThat(NoteDraft.isDirty("same", "same")).isFalse()
        assertThat(NoteDraft.canSave("same", "same")).isFalse()
        assertThat(NoteDraft.isDirty("changed", "same")).isTrue()
        assertThat(NoteDraft.canSave("changed", "same")).isTrue()
    }

    @Test fun anEmptyNewEditorIsNotDirty() {
        assertThat(NoteDraft.isDirty("", null)).isFalse()
        assertThat(NoteDraft.isDirty("x", null)).isTrue()
    }

    @Test fun theLimitIsTheRepositorysOwn() {
        assertThat(NoteDraft.MAX_CHARS).isEqualTo(org.cog.hymnchtv.notebook.model.NotebookValidation.MAX_NOTE_LENGTH)
        assertThat(NoteDraft.canSave("y".repeat(NoteDraft.MAX_CHARS), null)).isTrue()
        assertThat(NoteDraft.canSave("y".repeat(NoteDraft.MAX_CHARS + 1), null)).isFalse()
    }
}
