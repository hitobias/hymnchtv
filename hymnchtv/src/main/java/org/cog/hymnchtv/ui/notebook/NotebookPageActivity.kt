package org.cog.hymnchtv.ui.notebook

import android.os.Bundle
import org.cog.hymnchtv.BaseActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.ui.theme.SystemBars
import timber.log.Timber

/**
 * Hosts one notebook page (a hymn's notes or a playlist) full-screen, like ReadingSettingsActivity: the page draws its own
 * title bar and pads for the system bars. Not exported; opened only through [NotebookPages].
 */
class NotebookPageActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SystemBars.enable(this)
        setContentView(R.layout.activity_notebook_page)
        if (savedInstanceState == null) {
            val page = NotebookPages.fragmentFor(intent.extras)
            if (page == null) {
                Timber.w("Notebook page opened without a valid target: %s", intent.extras?.keySet())
                finish()
                return
            }
            supportFragmentManager.beginTransaction().replace(R.id.notebook_page_container, page).commit()
        }
    }
}
