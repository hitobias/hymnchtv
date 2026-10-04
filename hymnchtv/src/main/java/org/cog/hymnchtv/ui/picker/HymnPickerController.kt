package org.cog.hymnchtv.ui.picker

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import androidx.core.view.ViewCompat
import com.google.android.material.chip.Chip
import org.cog.hymnchtv.ui.toc.TocConstants
import org.cog.hymnchtv.R
import org.cog.hymnchtv.concurrent.AppExecutors
import org.cog.hymnchtv.hymn.EnglishXRef
import org.cog.hymnchtv.hymn.HymnRef
import org.cog.hymnchtv.hymn.HymnSource
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.cog.hymnchtv.ui.home.HomePrefs
import org.cog.hymnchtv.ui.host.UiFlags
import org.cog.hymnchtv.ui.titles.HymnTitleSource

/** What the screen around a picker does with the user's choices. Only [onOpenRef] and [openSearch] must be implemented. */
interface PickerHost {
    fun onOpenRef(ref: HymnRef, englishNo: Int?)

    fun openSearch(book: String?)

    fun onSetNext(ref: HymnRef) {}

    fun openToc(book: String, page: String) {}

    fun openHistory() {}

    fun onAddToPlaylist(ref: HymnRef?) {}
}

/** Binds [HymnPickerViews] to a [HymnPickerViewModel]: keys, source buttons, live preview and the open button. */
class HymnPickerController(
    private val views: HymnPickerViews,
    private val host: PickerHost,
    private val mode: PickerMode,
    private val vm: HymnPickerViewModel,
    private val prefs: SharedPreferences,
    private val titleSource: () -> HymnTitleSource,
    /** Whether titles are shown in Traditional (picks the TC Kai face), see LyricsScript. */
    private val titleTraditional: () -> Boolean = { false },
    private val xref: () -> EnglishXRef = EnglishXRefStore::current,
    /** Key-tap feedback; replaceable for tests. The system's haptic setting decides whether it is felt. */
    private val haptic: (View) -> Unit = { KeyHaptics.keyTap(it) },
) {
    private val context: Context = views.root.context
    private val chrome = PickerChrome.of(mode, UiFlags.NOTEBOOK_UI_ENABLED)

    private var suppress = false
    private var previewRequest = 0
    private var released = false
    private val heightFitter = PreviewHeightFitter(views, titleTraditional)

    init {
        heightFitter.fit()
        bindKeys()
        bindSources()
        bindActions()
        views.toc.visibility = visible(chrome.showToc)
        views.recentMore.visibility = visible(chrome.showMoreHistory)
        views.addPlaylist.visibility = visible(chrome.showAddPlaylist)
        views.setNext.visibility = visible(chrome.showSetNext)
        EnglishXRefStore.ensure(context) { if (!released) render() }
        render()
    }

    /** Call when the screen comes back: the next key starts a new number. */
    fun onResume() {
        vm.autoClear = true
        render()
    }

    fun release() {
        released = true
        previewRequest++
    }

    /** The hymn the open button would open, or null. */
    fun target(): HymnRef? = PickerReducer.target(PickerReducer.preview(vm.state, xref()))

    /** Replaces the entry (for example when reopening from the recent list) and shows it. */
    fun show(state: PickerState) {
        vm.set(state)
        render()
    }

    private fun bindKeys() {
        views.digits.forEachIndexed { digit, button ->
            button.setOnClickListener {
                haptic(button)
                vm.pressDigit(digit, xref())
                render()
            }
        }
        views.fu.setOnClickListener {
            haptic(views.fu)
            vm.pressFu()
            render()
        }
        views.delete.setOnClickListener {
            haptic(views.delete)
            vm.backspace()
            render()
        }
        views.delete.setOnLongClickListener {
            vm.set(PickerReducer.cleared(vm.state))
            render()
            true
        }
    }

    private fun bindSources() {
        // Exactly one source stays chosen: a tap on the chosen one changes nothing (render() restores the check)
        views.books.forEach { (source, button) ->
            button.setOnClickListener {
                haptic(button)
                vm.selectSource(source)
                persistSource(source)
                render()
            }
        }
    }

    private fun bindActions() {
        views.open.setOnClickListener { open() }
        views.searchField.setOnClickListener { host.openSearch(vm.state.source.book) }
        views.toc.setOnClickListener {
            val source = vm.state.source
            host.openToc(source.book ?: HymnTypes.DB, if (source == HymnSource.ENGLISH) TocConstants.TOC_ENGLISH else TocConstants.TOC_CATEGORY)
        }
        views.addPlaylist.setOnClickListener { host.onAddToPlaylist(target()) }
        views.setNext.setOnClickListener { target()?.let(host::onSetNext) }
        views.recentMore.setOnClickListener { host.openHistory() }
    }

    private fun persistSource(source: HymnSource) {
        // The jump panel starts from the current hymn's book and must not change what the home tab remembers
        if (mode == PickerMode.HOME) prefs.edit().putString(HomePrefs.LAST_HYMN_TYPE, source.prefValue).apply()
    }

    private fun open() {
        val preview = PickerReducer.preview(vm.state, xref())
        val ref = PickerReducer.target(preview) ?: return
        val englishNo = (preview as? Preview.English)?.englishNo
        vm.autoClear = true
        host.onOpenRef(ref, englishNo)
    }

    // ---- rendering ----

    private fun visible(show: Boolean) = if (show) View.VISIBLE else View.GONE

    private fun render() {
        if (released) return
        val state = vm.state
        val xr = xref()
        syncSources(state.source)
        renderKeys(state, xr)
        val preview = PickerReducer.preview(state, xr)
        renderPreview(state, preview)
        views.open.isEnabled = PickerReducer.target(preview) != null
        vm.clearNotice()
    }

    private fun syncSources(source: HymnSource) {
        for ((s, button) in views.books) button.isChecked = s == source
    }

    private fun renderKeys(state: PickerState, xr: EnglishXRef) {
        // The first key after coming back starts a new number, so availability is judged against an empty entry
        val effective = if (vm.autoClear) PickerReducer.cleared(state) else state
        val unavailable = context.getString(R.string.c_key_unavailable)
        views.digits.forEachIndexed { digit, button ->
            val enabled = PickerReducer.digitEnabled(effective, digit, xr)
            button.isEnabled = enabled
            ViewCompat.setStateDescription(button, if (enabled) null else unavailable)
        }
        views.fu.isEnabled = state.source.supportsFu
        ViewCompat.setStateDescription(views.fu, if (state.source.supportsFu) null else context.getString(R.string.c_notice_no_fu))
        views.fu.contentDescription = context.getString(R.string.c_key_fu_desc)
        views.fu.isSelected = state.isFu
    }

    private fun renderPreview(state: PickerState, preview: Preview) {
        val request = ++previewRequest
        views.alsoScroll.visibility = View.GONE
        views.alsoIn.removeAllViews()
        views.title.visibility = View.VISIBLE
        views.note.visibility = View.GONE
        status("")
        when (preview) {
            Preview.Empty -> {
                book(HymnLabels.longName(context, state.source))
                views.entry.text = if (state.isFu) context.getString(R.string.nn10) else EMPTY_NUMBER
                status(context.getString(R.string.c_preview_empty))
            }
            is Preview.Valid -> {
                book(HymnLabels.bookName(context, preview.ref))
                views.entry.text = HymnLabels.numberText(context, preview.ref)
                if (state.notice == null) showTitle(request, preview.ref)
            }
            is Preview.Invalid -> {
                val bookName = HymnLabels.longName(context, preview.source)
                book(bookName)
                views.entry.text = (if (preview.isFu) context.getString(R.string.c_label_fu, preview.number) else preview.number.toString())
                status(context.getString(if (preview.isFu) R.string.c_preview_invalid_fu else R.string.c_preview_invalid, bookName, preview.number.toString()))
                showChips(
                    context.getString(R.string.c_preview_also),
                    preview.alsoIn.map { source -> HymnLabels.sourceName(context, source) to { alsoIn(source) } },
                )
            }
            is Preview.English -> {
                book(HymnLabels.sourceName(context, HymnSource.ENGLISH))
                views.entry.text = preview.englishNo.toString()
                val others = preview.candidates.withIndex().filter { it.index != preview.pick }
                if (others.isEmpty()) {
                    if (state.notice == null) showTitle(request, preview.target)
                } else {
                    showChips(
                        context.getString(R.string.c_preview_en_other),
                        others.map { (index, ref) -> HymnLabels.headline(context, ref) to { pickEnglish(index) } },
                    )
                }
                views.note.text = HymnLabels.headline(context, preview.target)
                views.note.visibility = View.VISIBLE
            }
            is Preview.NoCounterpart -> {
                book(HymnLabels.sourceName(context, HymnSource.ENGLISH))
                views.entry.text = preview.englishNo.toString()
                status(context.getString(R.string.c_preview_en_none))
            }
        }
        if (state.notice == Notice.NO_FU_IN_BOOK && views.alsoScroll.visibility != View.VISIBLE) {
            status(context.getString(R.string.c_notice_no_fu))
        }
    }

    private fun book(name: String) {
        views.book.text = name
        KaiText.applyForUi(views.book, context, bold = false)
    }

    /** A message (not a hymn title): plain 16sp, so only titles carry the big Kai face. */
    private fun status(text: String) {
        views.title.text = text
        views.title.setTextSize(TypedValue.COMPLEX_UNIT_SP, STATUS_SP)
        views.title.setTypeface(Typeface.DEFAULT, Typeface.NORMAL)
    }

    private fun showTitle(request: Int, ref: HymnRef) {
        val source = titleSource()
        AppExecutors.io("picker-title") {
            val title = source.lookup(ref.book, ref.storedNo).orEmpty()
            // A newer keystroke supersedes this lookup
            AppExecutors.MAIN.post {
                if (!released && request == previewRequest && views.alsoScroll.visibility != View.VISIBLE) {
                    views.title.text = title
                    views.title.setTextSize(TypedValue.COMPLEX_UNIT_SP, TITLE_SP)
                    KaiText.apply(views.title, titleTraditional(), bold = true)
                }
            }
        }
    }

    private fun showChips(label: String, chips: List<Pair<String, () -> Unit>>) {
        if (chips.isEmpty()) return
        views.title.visibility = View.GONE
        views.alsoLabel.text = label
        chips.forEach { (text, action) ->
            val chip = Chip(context, null, com.google.android.material.R.attr.chipStyle).apply {
                this.text = text
                minHeight = (MIN_TOUCH_DP * resources.displayMetrics.density).toInt()
                setOnClickListener { action() }
            }.also(views::styleChip)
            views.alsoIn.addView(chip)
        }
        views.alsoScroll.visibility = View.VISIBLE
    }

    /** Switches to another book with the typed number kept. */
    private fun alsoIn(source: HymnSource) {
        vm.selectSource(source)
        persistSource(source)
        render()
    }

    private fun pickEnglish(index: Int) {
        vm.pickEnglish(index)
        render()
    }

    internal companion object {
        const val MIN_TOUCH_DP = 48
        const val EMPTY_NUMBER = "\u2014"
        const val TITLE_SP = 20f
        const val STATUS_SP = 16f
    }
}
