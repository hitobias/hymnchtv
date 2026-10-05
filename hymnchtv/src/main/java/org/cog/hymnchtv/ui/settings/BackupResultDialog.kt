package org.cog.hymnchtv.ui.settings

import android.app.Dialog
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.cog.hymnchtv.R

/** The outcome of a notebook export or import; a DialogFragment so it is still there after rotation. */
class BackupResultDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.backup_title)
        .setMessage(requireArguments().getString(ARG_TEXT))
        .setPositiveButton(android.R.string.ok, null)
        .create()

    companion object {
        const val TAG = "backup_result"

        @VisibleForTesting
        const val ARG_TEXT = "text"

        fun newInstance(text: String) = BackupResultDialog().apply { arguments = bundleOf(ARG_TEXT to text) }
    }
}
