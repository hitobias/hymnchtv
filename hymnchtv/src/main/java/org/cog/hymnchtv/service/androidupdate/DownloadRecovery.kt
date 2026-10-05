package org.cog.hymnchtv.service.androidupdate

import android.app.DownloadManager
import org.cog.hymnchtv.update.SemVer

/** What start-up does with the update download the previous process recorded (1.6.0). */
object DownloadRecovery {
    enum class Action {
        /** Finished while the app was not running: verify and stage it now. */
        VERIFY,

        /** Still downloading: register the completion receiver again. */
        WAIT,

        /** Failed, missing, superseded or already installed: remove it, as every start did before 1.6.0. */
        DISCARD,
    }

    /**
     * @param isLatestId the record's id is the latest enqueued download id
     * @param status DownloadManager COLUMN_STATUS of that id (STATUS_FAILED when DownloadManager no longer knows it)
     * @param fileExists the expected file (app Download dir + hymnal-X.Y.Z.apk) exists
     * @param pending the recorded release version; [installed] the running app's version
     */
    @JvmStatic
    fun decide(isLatestId: Boolean, status: Int, fileExists: Boolean, pending: SemVer?, installed: SemVer?): Action = when {
        !isLatestId || pending == null || installed == null || pending <= installed -> Action.DISCARD
        status == DownloadManager.STATUS_SUCCESSFUL && fileExists -> Action.VERIFY
        status == DownloadManager.STATUS_PENDING || status == DownloadManager.STATUS_RUNNING ||
            status == DownloadManager.STATUS_PAUSED -> Action.WAIT
        else -> Action.DISCARD
    }
}
