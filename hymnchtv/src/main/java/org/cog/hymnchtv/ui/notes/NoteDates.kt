package org.cog.hymnchtv.ui.notes

import android.content.Context
import org.cog.hymnchtv.R
import org.cog.hymnchtv.ui.home.HistoryTimeText

/** The date line of a note: the day and time it was written, plus when it was last edited. */
object NoteDates {
    fun label(ctx: Context, nowMillis: Long, row: NoteRow): String {
        val written = HistoryTimeText.dayHeader(ctx, nowMillis, row.createdAt) + " " + HistoryTimeText.clock(ctx, row.createdAt)
        return if (row.edited) {
            written + " · " + ctx.getString(R.string.notes_edited, HistoryTimeText.short(ctx, nowMillis, row.updatedAt))
        } else {
            written
        }
    }
}
