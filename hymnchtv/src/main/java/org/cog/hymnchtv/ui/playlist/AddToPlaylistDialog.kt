package org.cog.hymnchtv.ui.playlist

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.cog.hymnchtv.HymnsApp
import org.cog.hymnchtv.R
import org.cog.hymnchtv.notebook.model.HymnKey

/**
 * "Add to playlist…" from the lyrics page's More menu (wired in Task 18): pick a playlist, or make a new one, and the hymn is
 * appended. The write outlives the dialog; the result is a toast, like the favourite toggle's.
 */
class AddToPlaylistDialog : DialogFragment() {
    private val vm: AddToPlaylistViewModel by viewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater.inflate(R.layout.dialog_add_to_playlist, null)
        val list = view.findViewById<RecyclerView>(R.id.add_playlist_list)
        val adapter = PlaylistAdapter { row -> add(row.id) }
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter
        view.findViewById<android.view.View>(R.id.add_playlist_new).setOnClickListener {
            if (childFragmentManager.findFragmentByTag(PlaylistNameDialog.TAG) == null) {
                PlaylistNameDialog.newInstance(REQUEST_NEW, null).show(childFragmentManager, PlaylistNameDialog.TAG)
            }
        }
        childFragmentManager.setFragmentResultListener(REQUEST_NEW, this) { _, result ->
            result.getString(PlaylistNameDialog.RESULT_NAME)?.let(::createAndAdd)
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { vm.store.state.collect { adapter.submitList(it.rows) } }
        }
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.playlist_choose)
            .setView(view)
            .setNegativeButton(R.string.cancel, null)
            .create()
    }

    private fun add(playlistId: String) {
        val key = vm.key ?: return dismissAllowingStateLoss()
        vm.adder.add(playlistId, key, ::report)
        dismissAllowingStateLoss()
    }

    private fun createAndAdd(name: String) {
        val key = vm.key ?: return dismissAllowingStateLoss()
        vm.adder.createAndAdd(name, key, ::report)
        dismissAllowingStateLoss()
    }

    companion object {
        const val TAG = "add_to_playlist"
        const val ARG_TYPE = "hymn_type"
        const val ARG_NO = "hymn_no"
        private const val REQUEST_NEW = "add_to_new_playlist"

        /** Shows the dialog for [key] unless one is already open. */
        @JvmStatic
        fun show(fragmentManager: FragmentManager, key: HymnKey) {
            if (fragmentManager.findFragmentByTag(TAG) != null || fragmentManager.isStateSaved) return
            AddToPlaylistDialog().apply { arguments = bundleOf(ARG_TYPE to key.hymnType, ARG_NO to key.hymnNo) }
                .show(fragmentManager, TAG)
        }

        /** Runs on the main thread after the write, also when the dialog is long gone. */
        private fun report(playlistName: String?) {
            if (playlistName != null) {
                HymnsApp.showToastMessage(R.string.playlist_added, playlistName)
            } else {
                HymnsApp.showToastMessage(R.string.playlist_error)
            }
        }
    }
}
