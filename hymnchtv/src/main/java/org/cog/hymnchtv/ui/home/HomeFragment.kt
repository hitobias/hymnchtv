package org.cog.hymnchtv.ui.home

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.hymn.HymnRef
import org.cog.hymnchtv.hymn.HymnSource
import org.cog.hymnchtv.hymnhistory.HistoryRecord
import org.cog.hymnchtv.ui.host.MainNavigator
import org.cog.hymnchtv.ui.picker.HymnPickerController
import org.cog.hymnchtv.ui.picker.HymnPickerViewModel
import org.cog.hymnchtv.ui.picker.HymnPickerViews
import org.cog.hymnchtv.ui.picker.PickerHost
import org.cog.hymnchtv.ui.picker.PickerMode
import org.cog.hymnchtv.ui.picker.PickerState
import org.cog.hymnchtv.ui.titles.AssetHymnTitles
import org.cog.hymnchtv.ui.titles.HymnTitleSource

/**
 * Home tab: pick a hymn book, type the number, see the hymn's title at once, open it. The keys and preview are the shared
 * [HymnPickerController]; this fragment adds the user's appearance, the recent-hymns chips and the navigation to the
 * contents tab, the history page and the search page. It opens lyrics through the static [MainActivity.showContent].
 */
class HomeFragment : Fragment(R.layout.fragment_home), PickerHost {
    private val vm: HymnPickerViewModel by viewModels()

    /** Replaceable for tests; defaults to the bundled lyrics assets. */
    @VisibleForTesting
    var titleSource: HymnTitleSource? = null

    private var appearance: HomeAppearance? = null
    private var controller: HymnPickerController? = null
    private var recent: RecentChips? = null

    private val prefs: SharedPreferences
        get() = requireContext().getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm.restoreSource(runCatching { prefs.getString(HomePrefs.LAST_HYMN_TYPE, null) }.getOrNull())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val views = HymnPickerViews(view)
        appearance = HomeAppearance(requireContext(), views)
        recent = RecentChips(views, ::openFromHistory)
        controller = HymnPickerController(views, this, PickerMode.HOME, vm, prefs, ::currentTitleSource)
    }

    override fun onResume() {
        super.onResume()
        // The next key press starts a new number; settings and history may also have changed meanwhile
        refreshAppearance()
        controller?.onResume()
        recent?.reload()
    }

    /** The host hides this tab instead of pausing it; settings changed meanwhile must show when it comes back. */
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            refreshAppearance()
            recent?.reload()
        }
    }

    private fun refreshAppearance() {
        appearance?.apply(prefs)
    }

    override fun onDestroyView() {
        controller?.release()
        recent?.release()
        controller = null
        recent = null
        appearance = null
        super.onDestroyView()
    }

    private fun currentTitleSource(): HymnTitleSource =
        titleSource ?: AssetHymnTitles.from(requireContext()).also { titleSource = it }

    // ---- PickerHost ----

    override fun onOpenRef(ref: HymnRef, englishNo: Int?) {
        MainActivity.setHymnTypeNo(ref.book, ref.storedNo)
        if (englishNo == null) {
            MainActivity.showContent(requireContext(), ref.book, ref.storedNo, false)
        } else {
            MainActivity.showContent(requireContext(), ref.book, ref.storedNo, false, englishNo)
        }
    }

    override fun openSearch(book: String?) {
        navigator()?.openSearch(book)
    }

    override fun openToc(book: String, page: String) {
        navigator()?.openToc(book, page)
    }

    override fun openHistory() {
        navigator()?.openHistory()
    }

    private fun navigator(): MainNavigator? = activity as? MainNavigator

    /** Reopens a recent hymn; the picker shows it first (both appendix kinds included). */
    private fun openFromHistory(record: HistoryRecord) {
        val ref = HistoryActions.refOf(record)
        val source = HymnSource.ofBook(ref.book) ?: return
        controller?.show(PickerState(source = source, digits = ref.displayNo.toString(), isFu = ref.isFu))
        onOpenRef(ref, null)
    }
}
