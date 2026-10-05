package org.cog.hymnchtv.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import org.cog.hymnchtv.notebook.Notebook
import org.cog.hymnchtv.notebook.backup.UriBackupIo
import org.cog.hymnchtv.ui.settings.backup.BackupRunner
import org.cog.hymnchtv.ui.settings.backup.BackupRunnerHolder

/**
 * Gives the settings page the process-wide backup runner. The job runs in the notebook's app scope (BackupDocuments moves it to
 * IO), so leaving Settings or rotating never cancels it; its result waits in the runner until a Settings page shows it.
 */
class BackupViewModel(app: Application) : AndroidViewModel(app) {
    val runner: BackupRunner<Uri> = holder(app).get()

    private companion object {
        private val holderLock = Any()
        private var shared: BackupRunnerHolder<Uri>? = null

        fun holder(app: Application): BackupRunnerHolder<Uri> = synchronized(holderLock) {
            shared ?: BackupRunnerHolder {
                val graph = Notebook.get(app)
                val io = UriBackupIo(app.contentResolver, graph.backup)
                BackupRunner<Uri>(graph.appScope, { io.exportTo(it) }, { io.importFrom(it) })
            }.also { shared = it }
        }
    }
}
