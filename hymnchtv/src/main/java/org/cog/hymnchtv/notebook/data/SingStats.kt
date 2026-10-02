package org.cog.hymnchtv.notebook.data

/** How many times a hymn was sung (active logs) and when last; lastSungAt is null when never. */
data class SingStats(val singCount: Int, val lastSungAt: Long?)
