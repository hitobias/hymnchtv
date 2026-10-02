package org.cog.hymnchtv.ui.picker

import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import androidx.core.view.ViewCompat
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors
import org.cog.hymnchtv.HymnToc
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
    private val xref: () -> EnglishXRef = EnglishXRefStore::current,
) {
    private val context: Context = views.root.context
    private val chrome = PickerChrome.of(mode, UiFlags.NOTEBOOK_UI_ENABLED)
    private val groupOf = views.books.mapValues { (source, _) -> if (source.ordinal < GROUP_1_SIZE) views.group1 else views.group2 }
    private val sourceOfButton = views.books.entries.associate { (source, button) -> button.id to source }

    private var suppress = false
    private var previewRequest = 0
    private var released = false

    init {
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
                vm.pressDigit(digit, xref())
                render()
            }
        }
        views.fu.setOnClickListener {
            vm.pressFu()
            render()
        }
        views.delete.setOnClickListener {
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
        val listener = MaterialButtonToggleGroup.OnButtonCheckedListener { _, checkedId, isChecked ->
            if (suppress) return@OnButtonCheckedListener
            val source = sourceOfButton[checkedId] ?: return@OnButtonCheckedListener
            if (isChecked) {
                vm.selectSource(source)
                persistSource(source)
            }
            // an unchecked selected button (tap on the chosen one) is restored by render(): exactly one source stays chosen
            render()
        }
        views.group1.addOnButtonCheckedListener(listener)
        views.group2.addOnButtonCheckedListener(listener)
    }

    private fun bindActions() {
        views.open.setOnClickListener { open() }
        views.searchField.setOnClickListener { host.openSearch(vm.state.source.book) }
        views.toc.setOnClickListener {
            val source = vm.state.source
            host.openToc(source.book ?: HymnTypes.DB, if (source == HymnSource.ENGLISH) HymnToc.TOC_ENGLISH else HymnToc.TOC_CATEGORY)
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
        suppress = true
        try {
            for ((s, button) in views.books) {
                val group = groupOf.getValue(s)
                if (s == source) {
                    if (!button.isChecked) group.check(button.id)
                } else if (button.isChecked) {
                    group.uncheck(button.id)
                }
            }
        } finally {
            suppress = false
        }
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
        val tint = if (state.isFu) MaterialColors.getColor(views.fu, com.google.android.material.R.attr.colorSecondaryContainer) else Color.TRANSPARENT
        views.fu.backgroundTintList = ColorStateList.valueOf(tint)
    }

    private fun renderPreview(state: PickerState, preview: Preview) {
        val request = ++previewRequest
        views.alsoScroll.visibility = View.GONE
        views.alsoIn.removeAllViews()
        views.title.visibility = View.VISIBLE
        views.title.text = ""
        when (preview) {
            Preview.Empty -> views.entry.text = emptyText(state)
            is Preview.Valid -> {
                views.entry.text = HymnLabels.headline(context, preview.ref)
                if (state.notice == null) showTitle(request, preview.ref)
            }
            is Preview.Invalid -> {
                val book = HymnLabels.longName(context, preview.source)
                views.entry.text = context.getString(
                    if (preview.isFu) R.string.c_preview_invalid_fu else R.string.c_preview_invalid,
                    book,
                    preview.number.toString(),
                )
                showChips(
                    context.getString(R.string.c_preview_also),
                    preview.alsoIn.map { source -> HymnLabels.sourceName(context, source) to { alsoIn(source) } },
                )
            }
            is Preview.English -> {
                views.entry.text = context.getString(R.string.c_preview_en_to, preview.englishNo, HymnLabels.headline(context, preview.target))
                val others = preview.candidates.withIndex().filter { it.index != preview.pick }
                if (others.isEmpty()) {
                    if (state.notice == null) showTitle(request, preview.target)
                } else {
                    showChips(
                        context.getString(R.string.c_preview_en_other),
                        others.map { (index, ref) -> HymnLabels.headline(context, ref) to { pickEnglish(index) } },
                    )
                }
            }
            is Preview.NoCounterpart -> views.entry.setText(R.string.c_preview_en_none)
        }
        if (state.notice == Notice.NO_FU_IN_BOOK && views.alsoScroll.visibility != View.VISIBLE) {
            views.title.text = context.getString(R.string.c_notice_no_fu)
        }
    }

    private fun emptyText(state: PickerState): String =
        if (state.isFu) {
            "${HymnLabels.longName(context, state.source)} ${context.getString(R.string.nn10)}"
        } else {
            context.getString(R.string.c_preview_empty)
        }

    private fun showTitle(request: Int, ref: HymnRef) {
        val source = titleSource()
        AppExecutors.io("picker-title") {
            val title = source.lookup(ref.book, ref.storedNo).orEmpty()
            // A newer keystroke supersedes this lookup
            AppExecutors.MAIN.post {
                if (!released && request == previewRequest && views.alsoScroll.visibility != View.VISIBLE) {
                    views.title.text = title
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
            }
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

    private companion object {
        const val GROUP_1_SIZE = 4
        const val MIN_TOUCH_DP = 48
    }
}
