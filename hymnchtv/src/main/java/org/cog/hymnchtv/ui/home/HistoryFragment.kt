package org.cog.hymnchtv.ui.home

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.concurrent.AppExecutors
import org.cog.hymnchtv.hymnhistory.HistoryRecord
import org.cog.hymnchtv.lyrics.LyricsScript
import org.cog.hymnchtv.notebook.Cancellable
import org.cog.hymnchtv.notebook.Notebook
import org.cog.hymnchtv.notebook.Outcome
import org.cog.hymnchtv.notebook.data.entity.FavoriteEntity
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.ui.playlist.PlaylistsTabController
import org.cog.hymnchtv.ui.titles.AssetHymnTitles
import timber.log.Timber

/**
 * Full-screen "History & favourites" page (over the tabs). The Recent tab lists the recently opened hymns: tap opens, the
 * trailing button or a swipe deletes, a long press asks first. The Favourites tab lists the favourites: tap opens, the
 * trailing star removes (with undo). The Playlists tab (D-1 F2) is run by [PlaylistsTabController].
 */
class HistoryFragment : Fragment(R.layout.fragment_history) {
    private var list: RecyclerView? = null
    private var empty: View? = null
    private var favoritesList: RecyclerView? = null
    private var favoritesEmpty: View? = null
    private var tabs: TabLayout? = null
    private var playlistsTab: PlaylistsTabController? = null
    private lateinit var adapter: HistoryAdapter
    private lateinit var favoriteAdapter: FavoriteAdapter
    private var currentTab = HistoryTab.RECENT
    private var recentIsEmpty = false
    private var favoritesIsEmpty = false

    /** Bumped whenever the favourites change or a newer load starts, so an older snapshot arriving late is dropped. */
    private var loadGen = 0
    private var loadCall: Cancellable? = null
    private var snackbar: Snackbar? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recycler = view.findViewById<RecyclerView>(R.id.history_list)
        list = recycler
        empty = view.findViewById(R.id.tv_history_empty)
        adapter = HistoryAdapter(
            onOpen = { open(it) },
            onDelete = { delete(it) },
            onLongPress = { confirmDelete(it) },
        )
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter
        val swipe = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, h: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false

            override fun onSwiped(holder: RecyclerView.ViewHolder, direction: Int) {
                val position = holder.bindingAdapterPosition
                val record = if (position != RecyclerView.NO_POSITION) adapter.recordAt(position) else null
                if (record != null) delete(record, position)
            }

            override fun getSwipeDirs(rv: RecyclerView, holder: RecyclerView.ViewHolder): Int =
                if (holder is HistoryAdapter.RowHolder) super.getSwipeDirs(rv, holder) else 0
        }
        ItemTouchHelper(swipe).attachToRecyclerView(recycler)
        playlistsTab = PlaylistsTabController(this, view.findViewById(R.id.playlists_tab))
        setUpFavorites(view)
    }

    private fun setUpFavorites(view: View) {
        val recycler = view.findViewById<RecyclerView>(R.id.favorites_list)
        favoritesList = recycler
        favoritesEmpty = view.findViewById(R.id.tv_favorites_empty)
        favoriteAdapter = FavoriteAdapter(onOpen = { openFavorite(it) }, onUnfavorite = { unfavorite(it) })
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = favoriteAdapter

        val tabBar = view.findViewById<TabLayout>(R.id.history_tabs)
        tabs = tabBar
        val saved = HistoryTab.fromPref(prefs().getInt(HomePrefs.HISTORY_TAB, HistoryTab.RECENT.pref))
        tabBar.addTab(tabBar.newTab().setText(R.string.fav_tab_recent).setTag(HistoryTab.RECENT), saved == HistoryTab.RECENT)
        tabBar.addTab(tabBar.newTab().setText(R.string.fav_tab_favorites).setTag(HistoryTab.FAVORITES), saved == HistoryTab.FAVORITES)
        tabBar.addTab(tabBar.newTab().setText(R.string.playlist_tab).setTag(HistoryTab.PLAYLISTS), saved == HistoryTab.PLAYLISTS)
        currentTab = saved
        refreshVisibility()
        tabBar.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentTab = tab.tag as? HistoryTab ?: HistoryTab.RECENT
                prefs().edit().putInt(HomePrefs.HISTORY_TAB, currentTab.pref).apply()
                refreshVisibility()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit

            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
    }

    private fun prefs() = requireContext().getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE)

    private fun refreshVisibility() {
        val tab = currentTab
        list?.visibility = visibleIf(tab == HistoryTab.RECENT)
        empty?.visibility = visibleIf(tab == HistoryTab.RECENT && recentIsEmpty)
        favoritesList?.visibility = visibleIf(tab == HistoryTab.FAVORITES)
        favoritesEmpty?.visibility = visibleIf(tab == HistoryTab.FAVORITES && favoritesIsEmpty)
        playlistsTab?.setVisible(tab == HistoryTab.PLAYLISTS)
    }

    private fun visibleIf(shown: Boolean) = if (shown) View.VISIBLE else View.GONE

    override fun onStart() {
        super.onStart()
        (activity as? AppCompatActivity)?.supportActionBar?.setTitle(R.string.c_history_title)
        reload()
    }

    override fun onDestroyView() {
        snackbar?.dismiss()
        snackbar = null
        loadCall?.cancel()
        loadCall = null
        loadGen++
        list = null
        empty = null
        favoritesList = null
        favoritesEmpty = null
        tabs = null
        playlistsTab?.release()
        playlistsTab = null
        super.onDestroyView()
    }

    private fun reload() {
        HistoryActions.load(requireContext()) { records -> if (list != null) show(records) }
        loadFavorites()
        playlistsTab?.reload()
    }

    /** The records on screen (without day headings), newest first. */
    private var shown: List<HistoryRecord> = emptyList()

    private fun show(records: List<HistoryRecord>) {
        shown = records
        adapter.submitList(HistoryAdapter.group(requireContext(), records, System.currentTimeMillis()))
        recentIsEmpty = records.isEmpty()
        refreshVisibility()
    }

    // ---- favourites ----

    private fun loadFavorites() {
        val gen = ++loadGen
        loadCall?.cancel()
        val appContext = requireContext().applicationContext
        // The script is read now, so a later change of the lyrics language is picked up by the next load
        val variant = LyricsScript.hantVariant(appContext)
        loadCall = Notebook.async(appContext).favorites { outcome ->
            if (list == null || gen != loadGen) return@favorites
            when (outcome) {
                is Outcome.Ok -> buildFavoriteRows(appContext, gen, variant, outcome.value)
                is Outcome.Err -> {
                    Timber.w(outcome.error, "Loading the favourites failed")
                    showFavoritesError()
                }
            }
        }
    }

    /** Title lookup reads assets, so it runs off the main thread. */
    private fun buildFavoriteRows(appContext: Context, gen: Int, variant: org.cog.hymnchtv.lyrics.HantVariant?, entities: List<FavoriteEntity>) {
        AppExecutors.io("favorites-titles") {
            val titles = AssetHymnTitles.from(appContext, variant)
            val rows = FavoriteRows.build(entities) { ref -> titles.lookup(ref.book, ref.storedNo) }
            AppExecutors.MAIN.post { if (list != null && gen == loadGen) showFavorites(rows) }
        }
    }

    private fun showFavorites(rows: List<FavoriteRow>, emptyText: Int = R.string.fav_empty) {
        (favoritesEmpty as? TextView)?.setText(emptyText)
        shownFavorites = rows
        favoriteAdapter.submitList(rows)
        favoritesIsEmpty = FavoriteRows.isEmpty(rows)
        refreshVisibility()
    }

    /** A failed load is not "no favourites": show the error text in the empty-state view. */
    private fun showFavoritesError() {
        showFavorites(emptyList(), R.string.fav_error)
    }

    private var shownFavorites: List<FavoriteRow> = emptyList()

    private fun openFavorite(row: FavoriteRow) {
        MainActivity.setHymnTypeNo(row.ref.book, row.ref.storedNo)
        MainActivity.showContent(requireContext(), row.ref.book, row.ref.storedNo, false)
    }

    /** Drops any load still in flight: its snapshot would show the old state, the write's own reload shows the new one. */
    private fun invalidateLoads() {
        loadGen++
        loadCall?.cancel()
        loadCall = null
    }

    private fun setFavorite(key: HymnKey, favorite: Boolean, onOk: () -> Unit) {
        invalidateLoads()
        // Not cancelled with the view: a cancelled write could be dropped; the callback ignores a destroyed view
        Notebook.async(requireContext().applicationContext).setFavorite(key, favorite) { outcome ->
            if (list == null) return@setFavorite
            if (outcome is Outcome.Ok) {
                onOk()
            } else {
                Timber.w(outcome.errorOrNull(), "Setting the favourite failed")
                snackbar = view?.let { Snackbar.make(it, R.string.fav_error, Snackbar.LENGTH_LONG).also(Snackbar::show) }
                loadFavorites()
            }
        }
    }

    private fun unfavorite(row: FavoriteRow) {
        setFavorite(row.key, false) {
            showFavorites(shownFavorites.filterNot { it.key == row.key })
            snackbar = view?.let { root ->
                Snackbar.make(root, R.string.fav_removed, Snackbar.LENGTH_LONG)
                    .setAction(R.string.fav_undo) { setFavorite(row.key, true) { loadFavorites() } }
                    .also(Snackbar::show)
            }
            loadFavorites()
        }
    }

    private fun open(record: HistoryRecord) {
        val ref = HistoryActions.refOf(record)
        MainActivity.setHymnTypeNo(ref.book, ref.storedNo)
        MainActivity.showContent(requireContext(), ref.book, ref.storedNo, false)
    }

    /** [position] is the row's adapter position when it was swiped away, so a failed delete can bring it back. */
    private fun delete(record: HistoryRecord, position: Int = RecyclerView.NO_POSITION) {
        HistoryActions.delete(requireContext(), record) { deleted ->
            if (list == null) return@delete
            if (deleted) {
                show(shown.filterNot { sameRecord(it, record) })
            } else {
                // The row was already gone (or the delete failed): put the swiped row back, then reload so the list tells the truth
                if (position != RecyclerView.NO_POSITION) adapter.notifyItemChanged(position)
                reload()
            }
        }
    }

    /** Records are matched by what identifies them (book, number, time), not by object identity: a reload makes new objects. */
    private fun sameRecord(a: HistoryRecord, b: HistoryRecord) =
        a.hymnType == b.hymnType && a.hymnNo == b.hymnNo && a.timeStamp == b.timeStamp

    private fun confirmDelete(record: HistoryRecord) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete)
            .setMessage(getString(R.string.delete_history, record.toString()))
            .setPositiveButton(R.string.delete) { _, _ -> delete(record) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
