package org.cog.hymnchtv.ui.home

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.ui.motion.Motion
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
import org.cog.hymnchtv.ui.picker.KeyHaptics
import org.cog.hymnchtv.ui.picker.PickerHost
import org.cog.hymnchtv.ui.picker.PickerMode
import org.cog.hymnchtv.ui.picker.PickerState
import org.cog.hymnchtv.lyrics.LyricsScript
import org.cog.hymnchtv.ui.titles.AssetHymnTitles
import org.cog.hymnchtv.ui.titles.HymnTitleSource

/**
 * Home page: pick a hymn book, type the number, see the hymn's title at once, open it. The keys and preview are the shared
 * [HymnPickerController]; this fragment adds the user's appearance, the recent-hymns list and the navigation to the
 * contents page, the history page and the search page. It fits one screen (see [KeypadSizer]). It opens lyrics through the static [MainActivity.showContent].
 */
class HomeFragment : Fragment(R.layout.fragment_home), PickerHost {
    private val vm: HymnPickerViewModel by viewModels()

    /** Replaceable for tests; defaults to the bundled lyrics assets. */
    @VisibleForTesting
    var titleSource: HymnTitleSource? = null

    /** Key-tap feedback of the keys and book buttons; replaceable for tests. */
    @VisibleForTesting
    var haptic: (View) -> Unit = { KeyHaptics.keyTap(it) }

    private var homeColors: HomeColors? = null
    private var keypadSizer: KeypadSizer? = null
    private var views: HymnPickerViews? = null
    private var controller: HymnPickerController? = null
    private var recent: RecentChips? = null
    private var toolbarListener: View.OnLayoutChangeListener? = null

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
        // The preview number is the source of the shared element into the lyrics page (Motion)
        ViewCompat.setTransitionName(views.entry, Motion.SHARED_NUMBER)
        val recent = RecentChips(views, ::openFromHistory)
        this.recent = recent
        keypadSizer = KeypadSizer(views, view.findViewById(R.id.viewMain), recent).also { it.attach() }
        padForNavigationBar(view.findViewById(R.id.home_content))
        padForToolbar(view.findViewById(R.id.home_content))
        // The background and tokens are applied in onResume (always follows), once, so a photo is decoded only once
        controller = HymnPickerController(views, this, PickerMode.HOME, vm, prefs, ::currentTitleSource, ::titleIsTraditional, haptic = { haptic(it) })
    }

    override fun onResume() {
        super.onResume()
        // The next key press starts a new number; settings and history may also have changed meanwhile
        applyHomeTheme()
        controller?.onResume()
        recent?.reload()
    }

    /** The home page pads for the gesture or navigation bar itself, so its background reaches the screen edge. */
    private fun padForNavigationBar(content: View) {
        val base = content.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(content) { v, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            if (v.paddingBottom != base + bottom) {
                v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, base + bottom)
                // The viewport itself does not change, so the fit has to be asked for again
                v.post { keypadSizer?.update() }
            }
            insets
        }
        ViewCompat.requestApplyInsets(content)
    }

    /**
     * The page reaches behind the host's transparent toolbar (and the status bar), so its content starts below the toolbar:
     * the padding follows the toolbar's height, which grows by the status bar's size once the insets arrive.
     */
    private fun padForToolbar(content: View) {
        val toolbar = activity?.findViewById<View>(R.id.toolbar) ?: return
        val base = content.paddingTop
        val apply = {
            val top = base + toolbar.height
            if (content.paddingTop != top) {
                content.setPadding(content.paddingLeft, top, content.paddingRight, content.paddingBottom)
                content.post { keypadSizer?.update() }
            }
        }
        val listener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> apply() }
        toolbar.addOnLayoutChangeListener(listener)
        toolbarListener = listener
        apply()
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
        toolbarListener?.let { activity?.findViewById<View>(R.id.toolbar)?.removeOnLayoutChangeListener(it) }
        toolbarListener = null
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

    override fun onOpenRef(ref: HymnRef, englishNo: Int?) = openRef(ref, englishNo, shareNumber = true)

    /** [shareNumber]: the preview shows the hymn being opened, so its number may fly into the lyrics page header. */
    private fun openRef(ref: HymnRef, englishNo: Int?, shareNumber: Boolean) {
        MainActivity.setHymnTypeNo(ref.book, ref.storedNo)
        val number = if (shareNumber) views?.entry else null
        MainActivity.showContent(requireActivity(), ref.book, ref.storedNo, false, englishNo ?: -1, number)
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
        openRef(ref, null, shareNumber = false)
    }
}
