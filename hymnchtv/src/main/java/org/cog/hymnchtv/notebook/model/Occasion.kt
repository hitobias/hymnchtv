package org.cog.hymnchtv.notebook.model

/** Where a hymn was sung. Stored by name; UI labels: 主日／小排／禱告聚會／晨興／家中／其他. */
enum class Occasion {
    LORDS_DAY, SMALL_GROUP, PRAYER_MEETING, MORNING_REVIVAL, HOME, OTHER;

    companion object {
        /** Never throws: unknown or null values return null so callers choose the fallback. */
        @JvmStatic
        fun fromStorage(value: String?): Occasion? = entries.firstOrNull { it.name == value }
    }
}
