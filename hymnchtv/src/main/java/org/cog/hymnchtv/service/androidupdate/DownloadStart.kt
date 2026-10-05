package org.cog.hymnchtv.service.androidupdate

import android.app.DownloadManager

/**
 * The order in which an update download starts (1.6.0). Before, the receiver listened before the id was stored and ignored
 * unknown ids, so a download finishing early lost its completion broadcast. Now: enqueue, store the id and its record
 * synchronously, listen, then look once: a download already finished is verified at once.
 */
object DownloadStart {
    interface Steps {
        fun enqueue(): Long

        /** The id list and the PendingDownload record, written synchronously (commit). */
        fun persist(id: Long)

        fun registerReceiver()

        fun status(id: Long): Int

        /** Already finished: verify (off the main thread); the receiver may also fire, verification runs once. */
        fun verifyNow(id: Long)
    }

    @JvmStatic
    fun run(steps: Steps): Long {
        val id = steps.enqueue()
        steps.persist(id)
        steps.registerReceiver()
        if (steps.status(id) == DownloadManager.STATUS_SUCCESSFUL) steps.verifyNow(id)
        return id
    }
}
