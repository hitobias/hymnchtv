package org.cog.hymnchtv.ui.notes

import org.cog.hymnchtv.notebook.model.NotebookValidation

/** Rules of the note editor. The draft never goes into a Bundle (see [NoteDraftFiles]), so the limit is the repository's. */
object NoteDraft {
    const val MAX_CHARS = NotebookValidation.MAX_NOTE_LENGTH

    /** True when [text] differs from what is stored ([original] null for a new note). */
    fun isDirty(text: String, original: String?): Boolean = text != (original ?: "")

    fun canSave(text: String, original: String?): Boolean =
        text.isNotBlank() && text.length <= MAX_CHARS && isDirty(text, original)
}
