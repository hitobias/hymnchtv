package org.cog.hymnchtv.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import org.cog.hymnchtv.notebook.Notebook
import org.cog.hymnchtv.notebook.backup.UriBackupIo
import org.cog.hymnchtv.ui.settings.backup.BackupRunner

/** Holds the running export/import across rotation (the job runs in viewModelScope; BackupDocuments moves it to IO). */
class BackupViewModel(app: Application) : AndroidViewModel(app) {
    private val io = UriBackupIo(app.contentResolver, Notebook.get(app).backup)

    val runner = BackupRunner<Uri>(viewModelScope, { io.exportTo(it) }, { io.importFrom(it) })
}
