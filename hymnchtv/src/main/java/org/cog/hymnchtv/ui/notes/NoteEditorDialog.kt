package org.cog.hymnchtv.ui.notes

import android.app.Dialog
import android.os.Bundle
import android.text.InputFilter
import android.widget.EditText
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.cog.hymnchtv.R

/**
 * Writes a new note or edits one (with delete). Shown by [HymnNotesFragment] in its child fragment manager, so it shares the
 * page's ViewModel; the draft is kept there (and in its private draft file across process death), so the editor comes back with its
 * text after rotation or process death.
 * Back and Cancel ask before dropping unsaved text.
 */
class NoteEditorDialog : DialogFragment() {
    private val vm: HymnNotesViewModel by viewModels(ownerProducer = { requireParentFragment() })

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val noteId = vm.editing?.takeIf { it != HymnNotesViewModel.NEW_NOTE }
        val original = vm.originalOf(noteId)
        val view = layoutInflater.inflate(R.layout.dialog_note_editor, null)
        val input = view.findViewById<EditText>(R.id.note_input)
        input.filters = arrayOf(InputFilter.LengthFilter(NoteDraft.MAX_CHARS))
        input.setText(vm.draft ?: original.orEmpty())
        input.setSelection(input.text.length)
        vm.draft = input.text.toString()

        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (noteId == null) R.string.notes_add else R.string.notes_edit)
            .setView(view)
            .setPositiveButton(R.string.notes_save, null)
            .setNegativeButton(R.string.cancel, null)
        if (noteId != null) builder.setNeutralButton(R.string.notes_delete, null)
        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.setOnShowListener {
            val save = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val refresh = { save.isEnabled = NoteDraft.canSave(input.text.toString(), original) }
            refresh()
            input.doAfterTextChanged {
                vm.draft = it?.toString().orEmpty()
                refresh()
            }
            save.setOnClickListener { saveAndClose(noteId, input.text.toString()) }
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener { leave(noteId, input.text.toString(), original) }
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.setOnClickListener { noteId?.let(::confirmDelete) }
        }
        dialog.onBackPressedDispatcher.addCallback(this) { leave(noteId, input.text.toString(), original) }
        return dialog
    }

    private fun leave(noteId: String?, text: String, original: String?) {
        if (!NoteDraft.isDirty(text, original)) {
            close()
            return
        }
        val ask = MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.notes_unsaved)
            .setNegativeButton(R.string.notes_dont_save) { _, _ -> close() }
            .setNeutralButton(R.string.cancel, null)
        if (NoteDraft.canSave(text, original)) ask.setPositiveButton(R.string.notes_save) { _, _ -> saveAndClose(noteId, text) }
        ask.show()
    }

    private fun saveAndClose(noteId: String?, text: String) {
        vm.store?.save(noteId, text)
        close()
    }

    private fun confirmDelete(noteId: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.notes_delete)
            .setMessage(R.string.notes_delete_confirm)
            .setPositiveButton(R.string.delete) { _, _ ->
                vm.store?.delete(noteId)
                close()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun close() {
        vm.closeEditor()
        dismissAllowingStateLoss()
    }

    companion object {
        const val TAG = "note_editor"
    }
}
