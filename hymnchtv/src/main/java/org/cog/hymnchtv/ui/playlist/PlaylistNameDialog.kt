package org.cog.hymnchtv.ui.playlist

import android.app.Dialog
import android.os.Bundle
import android.text.InputFilter
import android.view.WindowManager
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import org.cog.hymnchtv.R

/**
 * Asks for a playlist name (new, or rename when an initial name is given). The trimmed name goes back as a fragment result
 * under the request key, to the fragment manager that showed the dialog; an invalid name keeps the dialog open with a hint.
 */
class PlaylistNameDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()
        val initial = args.getString(ARG_INITIAL)
        val view = layoutInflater.inflate(R.layout.dialog_playlist_name, null)
        val layout = view.findViewById<TextInputLayout>(R.id.playlist_name_layout)
        val input = view.findViewById<EditText>(R.id.playlist_name_input)
        input.filters = arrayOf(InputFilter.LengthFilter(PlaylistNames.MAX))
        if (savedInstanceState == null) input.setText(initial.orEmpty())
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (initial == null) R.string.playlist_new else R.string.playlist_rename)
            .setView(view)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = PlaylistNames.normalized(input.text.toString())
                if (name == null) {
                    layout.error = getString(R.string.playlist_name_invalid, PlaylistNames.MAX)
                } else {
                    setFragmentResult(checkNotNull(args.getString(ARG_REQUEST)), bundleOf(RESULT_NAME to name))
                    dismiss()
                }
            }
        }
        input.requestFocus()
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        return dialog
    }

    companion object {
        const val TAG = "playlist_name"
        const val RESULT_NAME = "name"
        private const val ARG_REQUEST = "request"
        private const val ARG_INITIAL = "initial"

        fun newInstance(requestKey: String, initialName: String?) = PlaylistNameDialog().apply {
            arguments = bundleOf(ARG_REQUEST to requestKey, ARG_INITIAL to initialName)
        }
    }
}
