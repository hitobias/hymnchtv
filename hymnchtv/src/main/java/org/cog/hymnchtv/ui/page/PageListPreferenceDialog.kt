package org.cog.hymnchtv.ui.page

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.preference.ListPreferenceDialogFragmentCompat

/** The list dialog of a [PagePreferenceFragment], painted with the page palette once it is shown (1.6.0). */
class PageListPreferenceDialog : ListPreferenceDialogFragmentCompat() {
    override fun onStart() {
        super.onStart()
        @Suppress("DEPRECATION") // the preference library finds its preference through the target fragment
        val palette = (targetFragment as? PagePreferenceFragment)?.palette ?: return
        (dialog as? AlertDialog)?.let { PageDialogPaint.apply(it, palette) }
    }

    companion object {
        /** PreferenceDialogFragmentCompat.ARG_KEY (protected) */
        private const val ARG_KEY = "key"

        @JvmStatic
        fun newInstance(key: String): PageListPreferenceDialog =
            PageListPreferenceDialog().apply { arguments = Bundle(1).apply { putString(ARG_KEY, key) } }
    }
}
