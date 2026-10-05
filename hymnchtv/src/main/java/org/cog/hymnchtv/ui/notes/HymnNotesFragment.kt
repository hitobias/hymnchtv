package org.cog.hymnchtv.ui.notes

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.cog.hymnchtv.R
import org.cog.hymnchtv.hymn.HymnRef
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.ui.home.HistoryTimeText
import org.cog.hymnchtv.ui.notebook.NotebookPages
import org.cog.hymnchtv.ui.notebook.PageColors
import org.cog.hymnchtv.ui.page.PageInsets
import org.cog.hymnchtv.ui.page.PageTitleBar
import org.cog.hymnchtv.ui.picker.HymnLabels

/** The notes of one hymn (D-1 F1): several per hymn, newest first; "New note" and a tap on a note open the editor. */
class HymnNotesFragment : Fragment(R.layout.fragment_hymn_notes) {
    private val vm: HymnNotesViewModel by viewModels()
    private var adapter: NoteAdapter? = null
    private var snackbar: Snackbar? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val key = vm.key
        val store = vm.store
        if (key == null || store == null) {
            activity?.finish()
            return
        }
        view.findViewById<TextView>(R.id.notes_hymn).text = HymnLabels.headline(requireContext(), HymnRef(key.hymnType, key.hymnNo))
        val list = view.findViewById<RecyclerView>(R.id.notes_list)
        val noteAdapter = NoteAdapter { openEditor(it.id) }
        adapter = noteAdapter
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = noteAdapter
        view.findViewById<PageTitleBar>(R.id.notes_title_bar).trackScroll(list)
        view.findViewById<View>(R.id.notes_add).setOnClickListener { openEditor(null) }
        PageInsets.install(view)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { store.state.collect { render(view, it) } }
                launch {
                    vm.title.collect { title ->
                        view.findViewById<TextView>(R.id.notes_hymn_title).apply {
                            text = title.orEmpty()
                            visibility = if (title.isNullOrBlank()) View.GONE else View.VISIBLE
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val root = view ?: return
        val palette = PageColors.paint(requireActivity(), root, root.findViewById(R.id.notes_title_bar))
        adapter?.palette = palette
        root.findViewById<TextView>(R.id.notes_hymn).setTextColor(palette.onCard)
        listOf(R.id.notes_hymn_title, R.id.notes_sung, R.id.notes_empty).forEach {
            root.findViewById<TextView>(it).setTextColor(palette.muted)
        }
        PageColors.accentButton(root.findViewById<MaterialButton>(R.id.notes_add), palette)
    }

    override fun onDestroyView() {
        snackbar?.dismiss()
        snackbar = null
        adapter = null
        super.onDestroyView()
    }

    private fun render(root: View, state: NotesState) {
        adapter?.submitList(state.rows)
        root.findViewById<TextView>(R.id.notes_empty).apply {
            setText(if (state.loadFailed) R.string.notes_load_error else R.string.notes_empty)
            visibility = if (!state.loading && state.rows.isEmpty()) View.VISIBLE else View.GONE
        }
        val stats = state.stats
        val last = stats?.lastSungAt
        root.findViewById<TextView>(R.id.notes_sung).apply {
            if (stats != null && stats.singCount > 0 && last != null) {
                text = getString(R.string.notes_sung, stats.singCount, HistoryTimeText.short(requireContext(), System.currentTimeMillis(), last))
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }
        state.message?.let { message ->
            val text = if (message == NotesMessage.SAVE_FAILED) R.string.notes_save_error else R.string.notes_delete_error
            snackbar = Snackbar.make(root, text, Snackbar.LENGTH_LONG).also(Snackbar::show)
            vm.store?.consumeMessage()
        }
    }

    private fun openEditor(noteId: String?) {
        if (childFragmentManager.findFragmentByTag(NoteEditorDialog.TAG) != null) return
        vm.openEditor(noteId)
        NoteEditorDialog().show(childFragmentManager, NoteEditorDialog.TAG)
    }

    companion object {
        fun newInstance(key: HymnKey) = HymnNotesFragment().apply {
            arguments = bundleOf(NotebookPages.ARG_HYMN_TYPE to key.hymnType, NotebookPages.ARG_HYMN_NO to key.hymnNo)
        }
    }
}
