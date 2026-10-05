package org.cog.hymnchtv.ui.playlist

import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.cog.hymnchtv.R
import org.cog.hymnchtv.notebook.Notebook
import org.cog.hymnchtv.ui.notebook.NotebookPages

/**
 * The Playlists tab of the history page, split out of HistoryFragment (research report: keep that fragment small). Create it
 * in onViewCreated; it lives with the host's view (its store runs in the view lifecycle scope).
 */
class PlaylistsTabController(private val host: Fragment, private val root: View) {
    private val list: RecyclerView = root.findViewById(R.id.playlists_list)
    private val empty: TextView = root.findViewById(R.id.playlists_empty)
    private val adapter = PlaylistAdapter { open(it.id) }
    private val store = PlaylistsStore(
        Notebook.get(host.requireContext()).playlists, host.viewLifecycleOwner.lifecycleScope, Dispatchers.IO,
    )
    private var snackbar: Snackbar? = null

    init {
        list.layoutManager = LinearLayoutManager(host.requireContext())
        list.adapter = adapter
        root.findViewById<View>(R.id.playlists_new).setOnClickListener { askForName() }
        host.childFragmentManager.setFragmentResultListener(REQUEST_NEW, host.viewLifecycleOwner) { _, result ->
            result.getString(PlaylistNameDialog.RESULT_NAME)?.let { name -> store.create(name) { created -> open(created.id) } }
        }
        host.viewLifecycleOwner.lifecycleScope.launch {
            host.viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { store.state.collect { render(it) } }
        }
    }

    fun setVisible(visible: Boolean) {
        root.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun reload() = store.load()

    fun release() {
        snackbar?.dismiss()
        snackbar = null
    }

    private fun render(state: PlaylistsState) {
        adapter.submitList(state.rows)
        empty.setText(if (state.loadFailed) R.string.playlist_error else R.string.playlist_empty_list)
        empty.visibility = if (!state.loading && state.rows.isEmpty()) View.VISIBLE else View.GONE
        if (state.message != null) {
            snackbar = Snackbar.make(root, R.string.playlist_error, Snackbar.LENGTH_LONG).also(Snackbar::show)
            store.consumeMessage()
        }
    }

    private fun askForName() {
        if (host.childFragmentManager.findFragmentByTag(PlaylistNameDialog.TAG) != null) return
        PlaylistNameDialog.newInstance(REQUEST_NEW, null).show(host.childFragmentManager, PlaylistNameDialog.TAG)
    }

    private fun open(playlistId: String) {
        if (host.isAdded) host.startActivity(NotebookPages.playlist(host.requireContext(), playlistId))
    }

    private companion object {
        const val REQUEST_NEW = "playlist_new"
    }
}
