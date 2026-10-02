package org.cog.hymnchtv.notebook.model

import java.util.UUID

/** Boundary validation for user input and imported rows; throws IllegalArgumentException. */
object NotebookValidation {
    const val MAX_NOTE_LENGTH = 100_000
    const val MAX_PLAYLIST_NAME_LENGTH = 200

    /** Accepts only the lowercase canonical form produced by UUID.toString(). */
    @JvmStatic
    fun uuid(value: String): String {
        val canonical = try {
            UUID.fromString(value).toString()
        } catch (e: IllegalArgumentException) {
            null
        }
        require(canonical == value) { "Not a canonical lowercase UUID" }
        return value
    }

    /** Note text is kept verbatim (leading spaces and line breaks are meaningful). */
    @JvmStatic
    fun noteBody(value: String): String {
        require(value.isNotBlank()) { "Note must not be blank" }
        require(value.length <= MAX_NOTE_LENGTH) { "Note longer than $MAX_NOTE_LENGTH characters" }
        return value
    }

    @JvmStatic
    fun playlistName(value: String): String {
        val trimmed = value.trim()
        require(trimmed.isNotEmpty()) { "Playlist name must not be blank" }
        require(trimmed.length <= MAX_PLAYLIST_NAME_LENGTH) { "Playlist name longer than $MAX_PLAYLIST_NAME_LENGTH" }
        return trimmed
    }

    @JvmStatic
    fun timeRange(fromInclusive: Long, toExclusive: Long) {
        require(fromInclusive <= toExclusive) { "Invalid range $fromInclusive..$toExclusive" }
    }

    /** Same skew limit as backup import (BackupLimits.maxFutureSkewMillis). */
    const val MAX_FUTURE_SKEW_MILLIS = 24 * 60 * 60 * 1000L

    /** A sing time must be >= 0 and at most 24 h after [nowMillis]. */
    @JvmStatic
    fun sungAt(value: Long, nowMillis: Long): Long {
        require(value >= 0) { "sungAt must be >= 0" }
        require(value <= nowMillis + MAX_FUTURE_SKEW_MILLIS) { "sungAt is more than 24 h in the future" }
        return value
    }
}
