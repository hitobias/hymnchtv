package org.cog.hymnchtv.ui.home

import java.util.Calendar
import java.util.TimeZone

/** Calendar-day arithmetic for the history dates (no Android dependency; the text is made by [HistoryTimeText]). */
object HistoryTime {
    private const val DAY_MS = 86_400_000L

    enum class Bucket { TODAY, YESTERDAY, WEEK, OLDER }

    /** Days between the calendar day of [thenMillis] and that of [nowMillis] in [zone] (midnight to midnight, not 24-hour spans). */
    fun dayDiff(nowMillis: Long, thenMillis: Long, zone: TimeZone): Int = (localDay(nowMillis, zone) - localDay(thenMillis, zone)).toInt()

    private fun localDay(millis: Long, zone: TimeZone): Long = Math.floorDiv(millis + zone.getOffset(millis), DAY_MS)

    /** A record dated in the future (clock set back) counts as today. */
    fun bucket(dayDiff: Int): Bucket = when {
        dayDiff <= 0 -> Bucket.TODAY
        dayDiff == 1 -> Bucket.YESTERDAY
        dayDiff < WEEK_DAYS -> Bucket.WEEK
        else -> Bucket.OLDER
    }

    fun bucket(nowMillis: Long, thenMillis: Long, zone: TimeZone): Bucket = bucket(dayDiff(nowMillis, thenMillis, zone))

    fun sameYear(nowMillis: Long, thenMillis: Long, zone: TimeZone): Boolean =
        yearOf(nowMillis, zone) == yearOf(thenMillis, zone)

    /** Key that is equal for two instants on the same calendar day (groups the history page). */
    fun dayKey(millis: Long, zone: TimeZone): Long = localDay(millis, zone)

    private fun yearOf(millis: Long, zone: TimeZone): Int =
        Calendar.getInstance(zone).apply { timeInMillis = millis }.get(Calendar.YEAR)

    /** Within this many days (exclusive of today and yesterday) a weekday name is enough. */
    private const val WEEK_DAYS = 7
}
