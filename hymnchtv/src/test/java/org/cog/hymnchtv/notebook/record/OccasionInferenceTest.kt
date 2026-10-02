package org.cog.hymnchtv.notebook.record

import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.notebook.model.Occasion
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * Dates: 2026-10-04 Sunday, 2026-10-03 Saturday, 2026-09-30 Wednesday.
 * America/Los_Angeles: spring forward on Sunday 2026-03-08, fall back on Sunday 2026-11-01.
 */
class OccasionInferenceTest {
    private val taipei = TimeZone.getTimeZone("Asia/Taipei")
    private val utc = TimeZone.getTimeZone("UTC")
    private val la = TimeZone.getTimeZone("America/Los_Angeles")

    private fun at(zone: TimeZone, y: Int, m: Int, d: Int, h: Int, min: Int = 0): Long =
        Calendar.getInstance(zone).apply {
            clear()
            set(y, m - 1, d, h, min)
        }.timeInMillis

    private fun infer(time: Long, last: Occasion?, zone: TimeZone = taipei) = OccasionInference.infer(time, zone, last)

    @Test
    fun sundayWindowBoundaries() {
        assertThat(infer(at(taipei, 2026, 10, 4, 5, 59), Occasion.SMALL_GROUP)).isEqualTo(Occasion.SMALL_GROUP)
        assertThat(infer(at(taipei, 2026, 10, 4, 6, 0), Occasion.SMALL_GROUP)).isEqualTo(Occasion.LORDS_DAY)
        assertThat(infer(at(taipei, 2026, 10, 4, 12, 59), Occasion.SMALL_GROUP)).isEqualTo(Occasion.LORDS_DAY)
        assertThat(infer(at(taipei, 2026, 10, 4, 13, 0), Occasion.SMALL_GROUP)).isEqualTo(Occasion.SMALL_GROUP)
        assertThat(infer(at(taipei, 2026, 10, 4, 13, 0), null)).isEqualTo(Occasion.HOME)
    }

    @Test
    fun otherDaysUseTheLastChoice() {
        assertThat(infer(at(taipei, 2026, 10, 3, 10), Occasion.SMALL_GROUP)).isEqualTo(Occasion.SMALL_GROUP)
        assertThat(infer(at(taipei, 2026, 9, 30, 6), Occasion.MORNING_REVIVAL)).isEqualTo(Occasion.MORNING_REVIVAL)
    }

    @Test
    fun lastChoiceLordsDayOutsideTheWindowBecomesHome() {
        assertThat(infer(at(taipei, 2026, 9, 30, 20), Occasion.LORDS_DAY)).isEqualTo(Occasion.HOME)
    }

    @Test
    fun neverChosenIsHome() {
        assertThat(infer(at(taipei, 2026, 9, 30, 20), null)).isEqualTo(Occasion.HOME)
    }

    @Test
    fun sameInstantDependsOnTheZone() {
        val instant = at(utc, 2026, 10, 4, 2) // Sunday 10:00 in Taipei, Sunday 02:00 in UTC
        assertThat(infer(instant, null, taipei)).isEqualTo(Occasion.LORDS_DAY)
        assertThat(infer(instant, null, utc)).isEqualTo(Occasion.HOME)
    }

    @Test
    fun springForwardSundayUsesDaylightTime() {
        assertThat(infer(at(la, 2026, 3, 8, 5, 59), null, la)).isEqualTo(Occasion.HOME)
        assertThat(infer(at(la, 2026, 3, 8, 6, 0), null, la)).isEqualTo(Occasion.LORDS_DAY)
        // 13:30 UTC is 06:30 PDT on the switch day, but would be 05:30 under PST
        assertThat(infer(at(utc, 2026, 3, 8, 13, 30), null, la)).isEqualTo(Occasion.LORDS_DAY)
        // one week earlier the same UTC time is still 05:30 PST
        assertThat(infer(at(utc, 2026, 3, 1, 13, 30), null, la)).isEqualTo(Occasion.HOME)
    }

    @Test
    fun fallBackSundayUsesStandardTime() {
        assertThat(infer(at(la, 2026, 11, 1, 6, 0), null, la)).isEqualTo(Occasion.LORDS_DAY)
        assertThat(infer(at(la, 2026, 11, 1, 12, 59), null, la)).isEqualTo(Occasion.LORDS_DAY)
        assertThat(infer(at(la, 2026, 11, 1, 13, 0), null, la)).isEqualTo(Occasion.HOME)
        // 13:30 UTC is 05:30 PST after the switch, but would be 06:30 under PDT
        assertThat(infer(at(utc, 2026, 11, 1, 13, 30), null, la)).isEqualTo(Occasion.HOME)
    }

    @Test
    fun repeatedHourOnFallBackIsNotLordsDay() {
        val firstOneThirty = at(utc, 2026, 11, 1, 8, 30)  // 01:30 PDT
        val secondOneThirty = at(utc, 2026, 11, 1, 9, 30) // 01:30 PST
        assertThat(infer(firstOneThirty, null, la)).isEqualTo(Occasion.HOME)
        assertThat(infer(secondOneThirty, null, la)).isEqualTo(Occasion.HOME)
    }
}
