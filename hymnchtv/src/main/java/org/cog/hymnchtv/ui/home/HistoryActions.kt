package org.cog.hymnchtv.ui.home

import android.content.Context
import org.cog.hymnchtv.concurrent.AppExecutors
import org.cog.hymnchtv.hymn.HymnRef
import org.cog.hymnchtv.hymnhistory.HistoryRecord
import org.cog.hymnchtv.persistance.DatabaseBackend

/** History reads and writes; the database is never touched on the main thread, results come back on it. */
object HistoryActions {
    /** [onResult] runs on the main thread with the newest-first records. */
    fun load(context: Context, onResult: (List<HistoryRecord>) -> Unit) {
        val appContext = context.applicationContext
        AppExecutors.io("history-load") {
            val records = DatabaseBackend.getInstance(appContext).historyRecords
            AppExecutors.MAIN.post { onResult(records) }
        }
    }

    /** [onDone] runs on the main thread with whether a row was removed. */
    fun delete(context: Context, record: HistoryRecord, onDone: (Boolean) -> Unit) {
        val appContext = context.applicationContext
        AppExecutors.io("history-delete") {
            val deleted = DatabaseBackend.getInstance(appContext).deleteHymnHistory(record) == 1
            AppExecutors.MAIN.post { onDone(deleted) }
        }
    }

    /** The record as a [HymnRef]: the media layer stores isFu = 0 for the youth appendix, [HymnRef] derives it from the number. */
    fun refOf(record: HistoryRecord): HymnRef = HymnRef(record.hymnType, record.hymnNo)
}
