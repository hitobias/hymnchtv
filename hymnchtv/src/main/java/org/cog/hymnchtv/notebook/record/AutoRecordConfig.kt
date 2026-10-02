package org.cog.hymnchtv.notebook.record

/** Code constants (not user settings; only the on/off switch is a setting). */
data class AutoRecordConfig(
    val visibleThresholdMillis: Long = DEFAULT_VISIBLE_THRESHOLD_MILLIS,
    val dedupeWindowMillis: Long = DEFAULT_DEDUPE_WINDOW_MILLIS,
) {
    init {
        require(visibleThresholdMillis > 0) { "visibleThresholdMillis must be > 0" }
        require(dedupeWindowMillis >= 0) { "dedupeWindowMillis must be >= 0" }
    }

    companion object {
        const val DEFAULT_VISIBLE_THRESHOLD_MILLIS = 2 * 60 * 1000L
        const val DEFAULT_DEDUPE_WINDOW_MILLIS = 3 * 60 * 60 * 1000L
    }
}
