package org.cog.hymnchtv.notebook.model

/**
 * An existing log is a duplicate of a new one at [at] when |existing − at| < window (both bounds exclusive).
 * Bounds saturate at Long.MIN_VALUE / Long.MAX_VALUE instead of overflowing.
 */
object DedupeWindow {
    @JvmStatic
    fun lowerExclusive(at: Long, windowMillis: Long): Long {
        require(windowMillis >= 0) { "windowMillis must be >= 0" }
        return if (at < Long.MIN_VALUE + windowMillis) Long.MIN_VALUE else at - windowMillis
    }

    @JvmStatic
    fun upperExclusive(at: Long, windowMillis: Long): Long {
        require(windowMillis >= 0) { "windowMillis must be >= 0" }
        return if (at > Long.MAX_VALUE - windowMillis) Long.MAX_VALUE else at + windowMillis
    }

    @JvmStatic
    fun contains(existing: Long, at: Long, windowMillis: Long): Boolean =
        existing > lowerExclusive(at, windowMillis) && existing < upperExclusive(at, windowMillis)
}
