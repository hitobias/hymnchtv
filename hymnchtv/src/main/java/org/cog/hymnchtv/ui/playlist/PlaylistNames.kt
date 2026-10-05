package org.cog.hymnchtv.ui.playlist

import org.cog.hymnchtv.notebook.model.NotebookValidation

/** The rule of NotebookValidation.playlistName, without exceptions, for the name dialog. */
object PlaylistNames {
    const val MAX = NotebookValidation.MAX_PLAYLIST_NAME_LENGTH

    /** The stored form of [input] (trimmed), or null when the repository would reject it. */
    fun normalized(input: String): String? = input.trim().takeIf { it.isNotEmpty() && it.length <= MAX }
}
