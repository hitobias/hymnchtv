package org.cog.hymnchtv.ui.home

import android.content.Context
import android.text.format.DateUtils
import org.cog.hymnchtv.R
import java.util.TimeZone

/** The words for a history time: the clock follows the system 12/24-hour setting, the formats follow the locale. */
object HistoryTimeText {
    private fun zone(): TimeZone = TimeZone.getDefault()

    /** Time of day ("10:42"). */
    fun clock(ctx: Context, thenMillis: Long): String = DateUtils.formatDateTime(ctx, thenMillis, DateUtils.FORMAT_SHOW_TIME)

    /** The small line under a recent chip: the time today, "Yesterday", the weekday within a week, else the date ("10/1"; with the year across years). */
    fun short(ctx: Context, nowMillis: Long, thenMillis: Long): String = when (HistoryTime.bucket(nowMillis, thenMillis, zone())) {
        HistoryTime.Bucket.TODAY -> clock(ctx, thenMillis)
        HistoryTime.Bucket.YESTERDAY -> ctx.getString(R.string.c_date_yesterday)
        HistoryTime.Bucket.WEEK -> DateUtils.formatDateTime(ctx, thenMillis, DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_WEEKDAY)
        HistoryTime.Bucket.OLDER -> {
            val base = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NUMERIC_DATE
            DateUtils.formatDateTime(ctx, thenMillis, if (HistoryTime.sameYear(nowMillis, thenMillis, zone())) base or DateUtils.FORMAT_NO_YEAR else base)
        }
    }

    /** Heading of a day on the history page: "Today", "Yesterday" or the full date. */
    fun dayHeader(ctx: Context, nowMillis: Long, thenMillis: Long): String = when (HistoryTime.bucket(nowMillis, thenMillis, zone())) {
        HistoryTime.Bucket.TODAY -> ctx.getString(R.string.c_date_today)
        HistoryTime.Bucket.YESTERDAY -> ctx.getString(R.string.c_date_yesterday)
        else -> {
            val base = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY
            DateUtils.formatDateTime(ctx, thenMillis, if (HistoryTime.sameYear(nowMillis, thenMillis, zone())) base or DateUtils.FORMAT_NO_YEAR else base or DateUtils.FORMAT_SHOW_YEAR)
        }
    }

    /** For TalkBack: the weekday, full date and time. */
    fun spoken(ctx: Context, thenMillis: Long): String = DateUtils.formatDateTime(
        ctx,
        thenMillis,
        DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_TIME,
    )
}
