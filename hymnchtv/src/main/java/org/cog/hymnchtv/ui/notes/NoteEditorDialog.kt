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
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
        // Right after process death the list has not loaded yet: the stored text is then looked up when it is needed
        val original = { vm.originalOf(noteId) }
        val view = layoutInflater.inflate(R.layout.dialog_note_editor, null)
        val input = view.findViewById<EditText>(R.id.note_input)
        input.filters = arrayOf(InputFilter.LengthFilter(NoteDraft.MAX_CHARS))
        val restored = vm.draft
        input.setText(restored ?: original().orEmpty())
        input.setSelection(input.text.length)
        // Not yet known (stored text pending) and nothing restored: leave the draft unset, so it is filled in once loaded
        if (restored != null || noteId == null || vm.originalKnown()) vm.draft = input.text.toString()

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
            val refresh = {
                save.isEnabled = (noteId == null || vm.originalKnown()) && NoteDraft.canSave(input.text.toString(), original())
            }
            refresh()
            if (noteId != null && !vm.originalKnown()) {
                lifecycleScope.launch {
                    vm.store?.state?.first { !it.loading }
                    if (vm.draft == null) {
                        input.setText(original().orEmpty())
                        input.setSelection(input.text.length)
                        vm.draft = input.text.toString()
                    }
                    refresh()
                }
            }
            input.doAfterTextChanged {
                vm.draft = it?.toString().orEmpty()
                refresh()
            }
            save.setOnClickListener { saveAndClose(noteId, input.text.toString()) }
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener { leave(noteId, input.text.toString()) }
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.setOnClickListener { noteId?.let(::confirmDelete) }
        }
        dialog.onBackPressedDispatcher.addCallback(this) { leave(noteId, input.text.toString()) }
        return dialog
    }

    private fun leave(noteId: String?, text: String) {
        val original = vm.originalOf(noteId)
        // The stored text is still unknown (list not loaded): treat any text as unsaved rather than compare with nothing
        val dirty = if (noteId != null && !vm.originalKnown()) text.isNotEmpty() else NoteDraft.isDirty(text, original)
        if (!dirty) {
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
