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
import org.cog.hymnchtv.reading.background.BackgroundPrefs
import org.cog.hymnchtv.reading.background.BackgroundSlot
import org.cog.hymnchtv.ui.host.MainNavigator
import org.cog.hymnchtv.ui.picker.HymnPickerController
import org.cog.hymnchtv.ui.picker.HymnPickerViewModel
import org.cog.hymnchtv.ui.picker.HymnPickerViews
import org.cog.hymnchtv.ui.picker.PickerHost
import org.cog.hymnchtv.ui.picker.PickerMode
import org.cog.hymnchtv.ui.picker.PickerState
import org.cog.hymnchtv.lyrics.LyricsScript
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

    private var homeColors: HomeColors? = null
    private var keypadSizer: KeypadSizer? = null
    private var views: HymnPickerViews? = null
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
        this.views = views
        keypadSizer = KeypadSizer(views, view.findViewById(R.id.viewMain)).also { it.attach() }
        recent = RecentChips(views, ::openFromHistory)
        // The background and tokens are applied in onResume (always follows), once, so a photo is decoded only once
        controller = HymnPickerController(views, this, PickerMode.HOME, vm, prefs, ::currentTitleSource, ::titleIsTraditional)
    }

    override fun onResume() {
        super.onResume()
        // The next key press starts a new number; settings and history may also have changed meanwhile
        applyHomeTheme()
        controller?.onResume()
        recent?.reload()
    }

    /** The host hides this tab instead of pausing it; settings changed meanwhile must show when it comes back. */
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            applyHomeTheme()
            recent?.reload()
        }
    }

    /**
     * Shows the MAIN slot's background and colours every home element from the UiTokens derived from it (spec 4). The
     * lyrics page builds its own tokens from the LYRICS slot; the two are never shared.
     */
    private fun applyHomeTheme() {
        val views = views ?: return
        val background = checkNotNull(views.background) { "the home layout needs its background view" }
        val applied = BackgroundPrefs.applyWithTokens(background, prefs, BackgroundSlot.MAIN)
        HomeColors(requireContext(), applied.tokens, applied.choice, applied.palette).also {
            homeColors = it
            it.apply(views)
        }
        keypadSizer?.update()
    }

    override fun onDestroyView() {
        controller?.release()
        recent?.release()
        keypadSizer?.detach()
        controller = null
        recent = null
        keypadSizer = null
        homeColors = null
        views = null
        super.onDestroyView()
    }

    private fun titleIsTraditional(): Boolean = LyricsScript.hantVariant(requireContext()) != null

    private fun currentTitleSource(): HymnTitleSource =
        // The script follows the reader's lyrics-language setting, which can change in settings between two lookups
        titleSource ?: AssetHymnTitles.from(requireContext(), LyricsScript.hantVariant(requireContext()))

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
