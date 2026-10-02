package org.cog.hymnchtv.about

import android.os.Bundle
import org.cog.hymnchtv.BaseActivity
import org.cog.hymnchtv.R

/** In-app help text (sub-project Z replaces every online help link with this screen). */
class HelpActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.help)
        setTitle(R.string.help)
    }
}
