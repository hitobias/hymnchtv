package org.cog.hymnchtv.ui.home

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class HistoryTimeTest {
    private val taipei = TimeZone.getTimeZone("Asia/Taipei")
    private val utc = TimeZone.getTimeZone("UTC")
    private val la = TimeZone.getTimeZone("America/Los_Angeles")

    private fun at(zone: TimeZone, y: Int, mo: Int, d: Int, h: Int = 12, mi: Int = 0): Long =
        Calendar.getInstance(zone).apply { clear(); set(y, mo - 1, d, h, mi, 0) }.timeInMillis

    private val now = at(taipei, 2026, 10, 2, 10, 42)

    @Test fun sameDayIsToday() {
        assertThat(HistoryTime.bucket(now, at(taipei, 2026, 10, 2, 0, 0), taipei)).isEqualTo(HistoryTime.Bucket.TODAY)
        assertThat(HistoryTime.bucket(now, now, taipei)).isEqualTo(HistoryTime.Bucket.TODAY)
    }

    @Test fun yesterdayIsTheCalendarDayBeforeNotTwentyFourHours() {
        // 23:59 the day before is "yesterday" although it is only 11 hours ago; 00:00 two days ago is not
        assertThat(HistoryTime.bucket(now, at(taipei, 2026, 10, 1, 23, 59), taipei)).isEqualTo(HistoryTime.Bucket.YESTERDAY)
        assertThat(HistoryTime.bucket(now, at(taipei, 2026, 10, 1, 0, 0), taipei)).isEqualTo(HistoryTime.Bucket.YESTERDAY)
        assertThat(HistoryTime.bucket(now, at(taipei, 2026, 9, 30, 23, 59), taipei)).isEqualTo(HistoryTime.Bucket.WEEK)
    }

    @Test fun crossingMidnightByMinutes() {
        val justAfter = at(taipei, 2026, 10, 2, 0, 1)
        val justBefore = at(taipei, 2026, 10, 1, 23, 59)
        assertThat(HistoryTime.bucket(justAfter, justBefore, taipei)).isEqualTo(HistoryTime.Bucket.YESTERDAY)
        assertThat(HistoryTime.dayDiff(justAfter, justBefore, taipei)).isEqualTo(1)
    }

    @Test fun withinSevenDaysIsWeekAndFromSevenDaysIsOlder() {
        assertThat(HistoryTime.bucket(now, at(taipei, 2026, 9, 26, 8, 0), taipei)).isEqualTo(HistoryTime.Bucket.WEEK)   // 6 days
        assertThat(HistoryTime.bucket(now, at(taipei, 2026, 9, 25, 8, 0), taipei)).isEqualTo(HistoryTime.Bucket.OLDER)  // 7 days
    }

    @Test fun acrossTheYearBoundary() {
        val january = at(taipei, 2026, 1, 2, 9, 0)
        val december = at(taipei, 2025, 12, 31, 20, 0)
        assertThat(HistoryTime.bucket(january, december, taipei)).isEqualTo(HistoryTime.Bucket.WEEK)
        assertThat(HistoryTime.sameYear(january, december, taipei)).isFalse()
        assertThat(HistoryTime.sameYear(now, at(taipei, 2026, 1, 1), taipei)).isTrue()
    }

    @Test fun theZoneDecidesWhichDayAnInstantIsOn() {
        // 2026-10-02 01:30 in Taipei is still 10-01 in UTC and Los Angeles
        val instant = at(taipei, 2026, 10, 2, 1, 30)
        val noonUtc = at(utc, 2026, 10, 2, 12, 0)
        assertThat(HistoryTime.dayDiff(noonUtc, instant, taipei)).isEqualTo(0)
        assertThat(HistoryTime.dayDiff(noonUtc, instant, utc)).isEqualTo(1)
        assertThat(HistoryTime.dayDiff(at(la, 2026, 10, 2, 9, 0), instant, la)).isEqualTo(1)
    }

    @Test fun daylightSavingDayIsStillOneDay() {
        // US clocks went back on 2026-11-01: that day has 25 hours
        val before = at(la, 2026, 10, 31, 23, 0)
        val after = at(la, 2026, 11, 1, 23, 0)
        val next = at(la, 2026, 11, 2, 1, 0)
        assertThat(HistoryTime.dayDiff(after, before, la)).isEqualTo(1)
        assertThat(HistoryTime.dayDiff(next, after, la)).isEqualTo(1)
    }

    @Test fun aFutureRecordCountsAsToday() {
        assertThat(HistoryTime.bucket(now, now + 3 * 86_400_000L, taipei)).isEqualTo(HistoryTime.Bucket.TODAY)
    }

    @Test fun dayKeyGroupsOneCalendarDay() {
        assertThat(HistoryTime.dayKey(at(taipei, 2026, 10, 1, 0, 0), taipei)).isEqualTo(HistoryTime.dayKey(at(taipei, 2026, 10, 1, 23, 59), taipei))
        assertThat(HistoryTime.dayKey(at(taipei, 2026, 10, 1, 23, 59), taipei)).isNotEqualTo(HistoryTime.dayKey(at(taipei, 2026, 10, 2, 0, 0), taipei))
    }
}
