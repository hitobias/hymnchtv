package org.cog.hymnchtv.ui.lyrics.jump

import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.ComponentDialog
import androidx.activity.addCallback
import androidx.core.view.ViewCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import com.google.android.material.color.MaterialColors
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.hymn.HymnRef
import org.cog.hymnchtv.hymn.HymnSource
import org.cog.hymnchtv.lyrics.LyricsScript
import org.cog.hymnchtv.reading.background.BackgroundChoice
import org.cog.hymnchtv.reading.background.BackgroundPrefs
import org.cog.hymnchtv.reading.background.BackgroundSlot
import org.cog.hymnchtv.ui.home.HomeColors
import org.cog.hymnchtv.ui.home.HomeScrollView
import org.cog.hymnchtv.ui.home.KeypadSizer
import org.cog.hymnchtv.ui.home.RecentFit
import org.cog.hymnchtv.ui.picker.HymnLabels
import org.cog.hymnchtv.ui.picker.HymnPickerController
import org.cog.hymnchtv.ui.picker.HymnPickerViewModel
import org.cog.hymnchtv.ui.picker.HymnPickerViews
import org.cog.hymnchtv.ui.picker.PickerHost
import org.cog.hymnchtv.ui.picker.PickerMode
import org.cog.hymnchtv.ui.search.SearchFragment
import org.cog.hymnchtv.ui.search.SearchHost
import org.cog.hymnchtv.ui.titles.AssetHymnTitles
import org.cog.hymnchtv.ui.titles.HymnTitleSource

/**
 * The lyrics page's jump panel (H5), full screen (the home picker is a whole-screen fitter, so no bottom sheet): the
 * shared picker in [PickerMode.JUMP] ("open" jumps, "play next" fills the slot), the slot row, the return stack as
 * "recent jumps", and the search page over it. Back closes the search first, then the panel. Every change goes through
 * [JumpHost] (the lyrics page).
 */
class JumpPanelFragment : DialogFragment(R.layout.panel_jump), PickerHost, SearchHost {
    private val vm: HymnPickerViewModel by viewModels()
    private var views: HymnPickerViews? = null
    private var controller: HymnPickerController? = null
    private var slotText: TextView? = null
    private var slotClear: View? = null
    private var searchContainer: View? = null
    private var keypadSizer: KeypadSizer? = null
    private val backStackListener = FragmentManager.OnBackStackChangedListener { syncSearchContainer() }

    private val host: JumpHost get() = requireActivity() as JumpHost

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val prefs = requireContext().getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE)
        // Starts from the book on screen (once per ViewModel); PickerMode.JUMP never writes the home tab's remembered book.
        // Not in onCreate: after a recreation the FragmentManager restores this panel before the pager exists.
        vm.restoreSource(HymnSource.ofBook(host.currentRef().book)?.prefValue)
        val pickerViews = HymnPickerViews(view)
        views = pickerViews
        controller = HymnPickerController(pickerViews, this, PickerMode.JUMP, vm, prefs, ::titleSource, ::titleTraditional)
        // The home page's look: the MAIN background and its tokens style the picker, and the same fitter sizes it
        val background = checkNotNull(pickerViews.background) { "the jump panel needs its background view" }
        val applied = BackgroundPrefs.applyWithTokens(background, prefs, BackgroundSlot.MAIN)
        HomeColors(requireContext(), applied.tokens, applied.choice, applied.palette).apply(pickerViews)
        val onBackground = if (applied.choice is BackgroundChoice.Photo) applied.tokens.onSurface else applied.palette.textColor
        listOf<TextView>(
            view.findViewById(R.id.jump_title), view.findViewById(R.id.jump_slot_text),
            view.findViewById(R.id.jump_close), view.findViewById(R.id.jump_slot_clear),
        ).forEach { it.setTextColor(onBackground) }
        ViewCompat.setAccessibilityHeading(view.findViewById(R.id.jump_title), true)
        keypadSizer = KeypadSizer(pickerViews, view.findViewById<HomeScrollView>(R.id.viewMain), NoRecentRows, alwaysScrollable = true)
            .also { it.attach() }
        pickerViews.recentLabel.setText(R.string.jump_recent_label)
        pickerViews.recentEmptyText.setText(R.string.jump_recent_empty)
        view.findViewById<View>(R.id.jump_close).setOnClickListener { dismiss() }
        slotText = view.findViewById(R.id.jump_slot_text)
        slotClear = view.findViewById<View>(R.id.jump_slot_clear).also { clear ->
            clear.setOnClickListener {
                host.onSetNext(null)
                renderSlot()
            }
        }
        searchContainer = view.findViewById(R.id.jump_search_container)
        childFragmentManager.addOnBackStackChangedListener(backStackListener)
        syncSearchContainer()
        (requireDialog() as ComponentDialog).onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (childFragmentManager.backStackEntryCount > 0) childFragmentManager.popBackStack() else dismiss()
        }
        renderSlot()
        renderRecent()
    }

    override fun onStart() {
        super.onStart()
        dialog?.setTitle(R.string.jump_title) // what TalkBack announces when the window opens
        dialog?.window?.let { window ->
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            window.setBackgroundDrawable(ColorDrawable(MaterialColors.getColor(requireView(), com.google.android.material.R.attr.colorSurface)))
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        host.setChromeHeld(true)
    }

    override fun onResume() {
        super.onResume()
        controller?.onResume()
    }

    override fun onDismiss(dialog: DialogInterface) {
        (activity as? JumpHost)?.setChromeHeld(false)
        super.onDismiss(dialog)
    }

    override fun onDestroyView() {
        childFragmentManager.removeOnBackStackChangedListener(backStackListener)
        keypadSizer?.detach()
        keypadSizer = null
        controller?.release()
        controller = null
        views = null
        slotText = null
        slotClear = null
        searchContainer = null
        super.onDestroyView()
    }

    // ---- PickerHost / SearchHost ----

    /** An English number opens its Chinese counterpart, like the home tab (the English page itself is not a jump target). */
    override fun onOpenRef(ref: HymnRef, englishNo: Int?) = jump(ref)

    override fun openSearch(book: String?) {
        if (childFragmentManager.findFragmentByTag(TAG_SEARCH) != null || childFragmentManager.isStateSaved) return
        childFragmentManager.beginTransaction()
            .replace(R.id.jump_search_container, SearchFragment.newInstance(book), TAG_SEARCH)
            .addToBackStack(TAG_SEARCH)
            .commit()
    }

    override fun onSetNext(ref: HymnRef) {
        host.onSetNext(ref)
        dismiss()
    }

    override fun onSearchResult(ref: HymnRef) = jump(ref)

    private fun jump(ref: HymnRef) {
        host.onJump(ref)
        dismiss()
    }

    // ---- rendering ----

    private fun syncSearchContainer() {
        searchContainer?.visibility = if (childFragmentManager.backStackEntryCount > 0) View.VISIBLE else View.GONE
    }

    private fun renderSlot() {
        val text = slotText ?: return
        val slot = host.jumpState.slot
        text.text = slot?.let { getString(R.string.jump_next_slot, HymnLabels.chip(requireContext(), it)) } ?: getString(R.string.jump_slot_none)
        slotClear?.visibility = if (slot == null) View.GONE else View.VISIBLE
    }

    private fun renderRecent() {
        val v = views ?: return
        val entries = host.jumpState.recentFirst()
        v.recentChips.removeAllViews()
        entries.forEachIndexed { index, entry -> v.recentChips.addView(rowOf(v, index, entry.ref)) }
        v.recentEmpty.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun rowOf(v: HymnPickerViews, index: Int, ref: HymnRef): View {
        val ctx = requireContext()
        val item = layoutInflater.inflate(R.layout.item_recent, v.recentChips, false)
        item.findViewById<TextView>(R.id.tv_recent_label_item).text = HymnLabels.chip(ctx, ref)
        item.findViewById<View>(R.id.tv_recent_when).visibility = View.GONE
        item.contentDescription = getString(R.string.jump_back_to, HymnLabels.spoken(ctx, ref, null))
        v.styleRecent(item)
        item.setOnClickListener {
            host.onReturnTo(index)
            dismiss()
        }
        return item
    }

    private fun titleTraditional(): Boolean = LyricsScript.hantVariant(requireContext()) != null

    private fun titleSource(): HymnTitleSource = AssetHymnTitles.from(requireContext(), LyricsScript.hantVariant(requireContext()))

    /** The return stack is not fitted into the screen (it lists up to ten rows below the fold and the panel scrolls). */
    private object NoRecentRows : RecentFit {
        override val total: Int = 0
        override var onChanged: (() -> Unit)? = null
        override fun fit(count: Int, empty: Boolean) = Unit
    }

    companion object {
        const val TAG = "jump_panel"
        private const val TAG_SEARCH = "jump_search"
    }
}
