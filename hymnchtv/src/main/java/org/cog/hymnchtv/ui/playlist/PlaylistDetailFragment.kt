package org.cog.hymnchtv.ui.playlist

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.ui.notebook.NotebookPages
import org.cog.hymnchtv.ui.notebook.PageColors
import org.cog.hymnchtv.ui.page.PageInsets
import org.cog.hymnchtv.ui.page.PageTitleBar
import org.cog.hymnchtv.ui.picker.HymnLabels
import timber.log.Timber

/**
 * One playlist (D-1 F2, no meeting mode): reorder by handle or TalkBack, remove with undo, open in order ("Start" / "Next: …"
 * / "Start again"), share as plain text without notes, rename, delete.
 */
class PlaylistDetailFragment : Fragment(R.layout.fragment_playlist_detail) {
    private val vm: PlaylistDetailViewModel by viewModels()
    private var adapter: PlaylistItemAdapter? = null
    private var snackbar: Snackbar? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val store = vm.store ?: run {
            activity?.finish()
            return
        }
        val list = view.findViewById<RecyclerView>(R.id.playlist_items)
        val itemAdapter = PlaylistItemAdapter(
            onOpen = ::open,
            onRemove = ::remove,
            onMove = { from, to -> store.move(from, to) },
            onDragFinished = { ids -> store.reorder(ids) },
        )
        adapter = itemAdapter
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = itemAdapter
        itemAdapter.attachDrag(list)
        view.findViewById<PageTitleBar>(R.id.playlist_title_bar).trackScroll(list)
        view.findViewById<View>(R.id.playlist_play).setOnClickListener { play() }
        view.findViewById<View>(R.id.playlist_share).setOnClickListener { share() }
        view.findViewById<View>(R.id.playlist_more).setOnClickListener { showMore(it) }
        PageInsets.install(view)
        childFragmentManager.setFragmentResultListener(REQUEST_RENAME, viewLifecycleOwner) { _, result ->
            result.getString(PlaylistNameDialog.RESULT_NAME)?.let(store::rename)
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { store.state.collect { render(view, it) } }
        }
    }

    override fun onResume() {
        super.onResume()
        val root = view ?: return
        val palette = PageColors.paint(requireActivity(), root, root.findViewById(R.id.playlist_title_bar))
        adapter?.palette = palette
        root.findViewById<TextView>(R.id.playlist_name).setTextColor(palette.onCard)
        root.findViewById<TextView>(R.id.playlist_empty).setTextColor(palette.muted)
        listOf(R.id.playlist_share, R.id.playlist_more).forEach {
            root.findViewById<ImageButton>(it).imageTintList = ColorStateList.valueOf(palette.onCard)
        }
        PageColors.accentButton(root.findViewById<MaterialButton>(R.id.playlist_play), palette)
    }

    override fun onDestroyView() {
        snackbar?.dismiss()
        snackbar = null
        adapter = null
        super.onDestroyView()
    }

    /** The items the page shows (for tests). */
    @VisibleForTesting
    internal fun currentItemsForTest(): List<DetailItem> = adapter?.currentItems().orEmpty()

    /** The text the share button sends: name and hymns only (built from labels and titles, never from notes). */
    @VisibleForTesting
    internal fun shareText(): String {
        val state = vm.store?.state?.value ?: return ""
        val lines = state.items.map { PlaylistShareText.line(HymnLabels.headline(requireContext(), it.ref), it.title) }
        return PlaylistShareText.format(state.name, lines)
    }

    private fun render(root: View, state: DetailState) {
        if (state.missing) {
            activity?.finish()
            return
        }
        root.findViewById<TextView>(R.id.playlist_name).text = state.name
        adapter?.submit(state.items, state.nextIndex)
        root.findViewById<View>(R.id.playlist_empty).visibility =
            if (!state.loading && state.items.isEmpty()) View.VISIBLE else View.GONE
        val play = root.findViewById<MaterialButton>(R.id.playlist_play)
        val button = state.button
        play.isEnabled = button != null
        play.text = when (button) {
            null, PlayButton.Start -> getString(R.string.playlist_start)
            PlayButton.Restart -> getString(R.string.playlist_restart)
            is PlayButton.Next -> getString(R.string.playlist_next, HymnLabels.headline(requireContext(), state.items[button.index].ref))
        }
        root.findViewById<View>(R.id.playlist_share).isEnabled = state.items.isNotEmpty()
        if (state.message != null) {
            snackbar = Snackbar.make(root, R.string.playlist_error, Snackbar.LENGTH_LONG).also(Snackbar::show)
            vm.store?.consumeMessage()
        }
    }

    private fun play() {
        val button = vm.store?.state?.value?.button ?: return
        open(PlaylistOrder.target(button))
    }

    private fun open(index: Int) {
        val item = vm.store?.state?.value?.items?.getOrNull(index) ?: return
        vm.markOpened(item.itemId)
        MainActivity.setHymnTypeNo(item.ref.book, item.ref.storedNo)
        MainActivity.showContent(requireContext(), item.ref.book, item.ref.storedNo, false)
    }

    private fun remove(item: DetailItem) {
        val removed = vm.store?.remove(item.itemId) ?: return
        val root = view ?: return
        snackbar = Snackbar.make(root, R.string.playlist_item_removed, Snackbar.LENGTH_LONG)
            .setAction(R.string.fav_undo) { vm.store?.undoRemove(removed) }
            .also(Snackbar::show)
    }

    private fun share() {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText())
        try {
            startActivity(Intent.createChooser(send, getString(R.string.playlist_share_chooser)))
        } catch (e: ActivityNotFoundException) {
            Timber.w(e, "No app can share the playlist")
        }
    }

    private fun showMore(anchor: View) {
        PopupMenu(requireContext(), anchor).apply {
            menu.add(Menu.NONE, MENU_RENAME, Menu.NONE, R.string.playlist_rename)
            menu.add(Menu.NONE, MENU_DELETE, Menu.NONE, R.string.playlist_delete)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    MENU_RENAME -> rename()
                    MENU_DELETE -> confirmDelete()
                }
                true
            }
            show()
        }
    }

    private fun rename() {
        val name = vm.store?.state?.value?.name ?: return
        if (childFragmentManager.findFragmentByTag(PlaylistNameDialog.TAG) != null) return
        PlaylistNameDialog.newInstance(REQUEST_RENAME, name).show(childFragmentManager, PlaylistNameDialog.TAG)
    }

    private fun confirmDelete() {
        val name = vm.store?.state?.value?.name ?: return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.playlist_delete)
            .setMessage(getString(R.string.playlist_delete_confirm, name))
            .setPositiveButton(R.string.delete) { _, _ -> vm.store?.delete { activity?.finish() } }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    companion object {
        private const val REQUEST_RENAME = "playlist_rename"
        private const val MENU_RENAME = 1
        private const val MENU_DELETE = 2

        fun newInstance(playlistId: String) = PlaylistDetailFragment().apply {
            arguments = bundleOf(NotebookPages.ARG_PLAYLIST_ID to playlistId)
        }
    }
}
