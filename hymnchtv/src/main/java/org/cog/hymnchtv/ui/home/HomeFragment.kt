package org.cog.hymnchtv.ui.home

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.annotation.VisibleForTesting
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.cog.hymnchtv.ContentSearch
import org.cog.hymnchtv.HymnsApp
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.concurrent.AppExecutors
import org.cog.hymnchtv.hymnhistory.HistoryRecord
import org.cog.hymnchtv.persistance.DatabaseBackend
import org.cog.hymnchtv.ui.titles.AssetHymnTitles
import org.cog.hymnchtv.ui.titles.HymnTitleSource
import org.cog.hymnchtv.utils.HymnNoValidate
import timber.log.Timber
import java.io.IOException
import java.util.Locale

/**
 * Home tab: number keypad, the six hymn books (the last used one highlighted), live title of the typed number,
 * recent history and the "+ playlist" button. Self-contained: it opens lyrics through the static
 * [MainActivity.showContent]; the old MainActivity keypad stays until the host migration.
 */
class HomeFragment : Fragment(R.layout.fragment_home) {
    private val vm: HomeViewModel by viewModels()

    /** Replaceable for tests; defaults to the bundled lyrics assets. */
    @VisibleForTesting
    var titleSource: HymnTitleSource? = null

    private var views: HomeViews? = null
    private var appearance: HomeAppearance? = null
    private lateinit var historyAdapter: HistoryAdapter
    private var previewRequest = 0

    private val prefs: SharedPreferences
        get() = requireContext().getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm.restoreHymnType(runCatching { prefs.getString(HomePrefs.LAST_HYMN_TYPE, null) }.getOrNull())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val v = HomeViews(view)
        views = v
        appearance = HomeAppearance(requireContext(), v)
        initHistory(v)
        bindKeys(v)
        bindActions(v)
        v.entry.doAfterTextChanged { refreshPreview() }
        v.entry.text = vm.number
        highlightBook(v)
    }

    override fun onResume() {
        super.onResume()
        // The next key press starts a new number; settings may also have changed meanwhile
        vm.autoClear = true
        views?.let { v ->
            val result = appearance?.apply(prefs)
            historyAdapter.textColor = result?.textColor
            highlightBook(v)
        }
        refreshPreview()
    }

    override fun onDestroyView() {
        views = null
        appearance = null
        super.onDestroyView()
    }

    /** The host calls this on back; true when the history list was open and has been closed. */
    fun onBackPressed(): Boolean {
        val v = views ?: return false
        if (v.history.visibility != View.VISIBLE) return false
        hideHistory(v)
        return true
    }

    // ---- keypad ----

    private fun bindKeys(v: HomeViews) {
        v.digits.forEachIndexed { digit, button -> button.setOnClickListener { onKey(v, digit.toString()) } }
        v.fu.setOnClickListener {
            vm.isFu = true
            vm.number = ""
            vm.autoClear = false
            onKey(v, HomeEntry.FU)
        }
        v.delete.setOnClickListener {
            setNumber(v, "")
            vm.isFu = false
        }
        v.entry.setOnClickListener { toggleHistory(v) }
    }

    private fun onKey(v: HomeViews, key: String) {
        if (vm.autoClear) {
            vm.autoClear = false
            vm.isFu = false
            vm.number = ""
        }
        setNumber(v, vm.number + key)
        // Starting a new number clears a stale content search
        if (vm.number.length == 1) v.search.setText("")
    }

    private fun setNumber(v: HomeViews, value: String) {
        vm.number = value
        v.entry.text = value
    }

    // ---- hymn books ----

    private fun bindActions(v: HomeViews) {
        v.books.forEach { (type, button) ->
            button.setOnClickListener { onHymnBookClicked(v, type, false) }
        }
        // A long press on 青年诗歌 picks the appendix hymn instead
        v.books.getValue(MainActivity.HYMN_YB).setOnLongClickListener {
            onHymnBookClicked(v, MainActivity.HYMN_YB, true)
            true
        }
        v.english.setOnClickListener { showHymnFromEnglish(false) }
        v.english.setOnLongClickListener {
            showHymnFromEnglish(true)
            true
        }
        v.searchButton.setOnClickListener { onSearch(v) }
        // C-7: wired by sub-project D-1; btn_next is shown and wired by the host (HOST1)
        v.addPlaylist.setOnClickListener { }
        v.next.setOnClickListener { }
    }

    private fun onHymnBookClicked(v: HomeViews, hymnType: String, altSelect: Boolean) {
        selectBook(v, hymnType)

        var digits = if (vm.isFu) vm.number.removePrefix(HomeEntry.FU) else vm.number
        if (digits.isEmpty()) digits = "0"
        var hymnNo = digits.toIntOrNull() ?: 0
        if (vm.isFu) {
            hymnNo += if (hymnType == MainActivity.HYMN_DB) HymnNoValidate.HYMN_DB_NO_MAX else HymnNoValidate.HYMN_YB_NO_MAX
        }
        if (altSelect) {
            MainActivity.HYMN_YB_ALT[hymnNo]?.let { hymnNo = it }
        }

        val valid = HymnNoValidate.validateHymnNo(hymnType, hymnNo, vm.isFu)
        when {
            valid != -1 -> {
                MainActivity.setHymnTypeNo(hymnType, hymnNo)
                openHymn(hymnType, valid)
            }
            // Only clear the entry when it is a Fu number for a book other than 大本
            vm.isFu && hymnType != MainActivity.HYMN_DB -> {
                setNumber(v, "")
                vm.isFu = false
            }
            else -> vm.autoClear = true
        }
    }

    private fun selectBook(v: HomeViews, hymnType: String) {
        vm.selectHymnType(hymnType)
        prefs.edit().putString(HomePrefs.LAST_HYMN_TYPE, hymnType).apply()
        highlightBook(v)
        refreshPreview()
    }

    private fun highlightBook(v: HomeViews) {
        val density = resources.displayMetrics.density
        v.books.forEach { (type, button) ->
            val selected = type == vm.hymnType
            button.isSelected = selected
            button.strokeWidth = ((if (selected) STROKE_SELECTED_DP else STROKE_NORMAL_DP) * density).toInt()
        }
    }

    private fun openHymn(hymnType: String, hymnNo: Int) {
        MainActivity.showContent(requireContext(), hymnType, hymnNo, false)
    }

    /** 英中对照: the typed number is an English hymn number; open its Chinese counterpart. */
    private fun showHymnFromEnglish(dbPage: Boolean) {
        var digits = if (vm.isFu) vm.number.removePrefix(HomeEntry.FU) else vm.number
        if (digits.isEmpty()) digits = "0"
        val hymnEng = digits.toIntOrNull() ?: return
        val appContext = requireContext().applicationContext
        AppExecutors.io("home-english") {
            // failed: the table could not be read; target null (no failure): no Chinese counterpart
            var failed = false
            val target = try {
                EnglishCrossRef.find(appContext, hymnEng, dbPage)
            } catch (e: IOException) {
                Timber.w("Content toc not available: %s", e.message)
                failed = true
                null
            }
            AppExecutors.MAIN.post {
                if (!isAdded || views == null) return@post
                when {
                    failed -> HymnsApp.showToastMessage(R.string.in_development)
                    // Pass in a non-existent HYMN_BB_DUMMY for the Chinese hymn number
                    target == null -> MainActivity.showContent(requireContext(), MainActivity.HYMN_BB, HymnNoValidate.HYMN_BB_DUMMY, false, hymnEng)
                    else -> MainActivity.showContent(requireContext(), target.first, target.second, false, hymnEng)
                }
            }
        }
    }

    private fun onSearch(v: HomeViews) {
        val text = v.search.text.toString().trim()
        if (text.isEmpty()) {
            HymnsApp.showToastMessage(R.string.error_search_empty)
            return
        }
        (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
            ?.hideSoftInputFromWindow(v.search.windowToken, 0)
        startActivity(Intent(requireContext(), ContentSearch::class.java).putExtra(MainActivity.ATTR_SEARCH, text))
    }

    // ---- live title ----

    private fun refreshPreview() {
        val v = views ?: return
        val request = ++previewRequest
        val type = vm.hymnType
        val hymnNo = HomeEntry.hymnNo(vm.number, type)
        if (hymnNo == null) {
            v.preview.text = ""
            return
        }
        val source = titleSource ?: AssetHymnTitles.from(requireContext()).also { titleSource = it }
        AppExecutors.io("home-title") {
            val title = source.lookup(type, hymnNo).orEmpty()
            // A newer keystroke supersedes this lookup
            AppExecutors.MAIN.post { if (request == previewRequest) views?.preview?.text = title }
        }
    }

    // ---- history ----

    private fun initHistory(v: HomeViews) {
        historyAdapter = HistoryAdapter(
            onOpen = { openFromHistory(it) },
            onDelete = { deleteHistory(it) },
            onLongPress = { confirmDeleteHistory(it) },
        )
        v.history.layoutManager = LinearLayoutManager(requireContext())
        v.history.adapter = historyAdapter
        val swipe = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, h: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false

            override fun onSwiped(holder: RecyclerView.ViewHolder, direction: Int) {
                val position = holder.bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) deleteHistory(historyAdapter.recordAt(position))
            }
        }
        ItemTouchHelper(swipe).attachToRecyclerView(v.history)
    }

    private fun toggleHistory(v: HomeViews) {
        if (v.history.visibility == View.VISIBLE) {
            hideHistory(v)
        } else {
            v.entry.setHint(R.string.hint_hymn_history)
            loadHistory()
            v.history.visibility = View.VISIBLE
        }
    }

    private fun hideHistory(v: HomeViews) {
        v.entry.setHint(R.string.hint_hymn_number_enter)
        v.history.visibility = View.GONE
    }

    private fun loadHistory() {
        val appContext = requireContext().applicationContext
        AppExecutors.io("home-history") {
            val records = DatabaseBackend.getInstance(appContext).historyRecords
            AppExecutors.MAIN.post { if (views != null) historyAdapter.submitList(records) }
        }
    }

    private fun openFromHistory(record: HistoryRecord) {
        val v = views ?: return
        hideHistory(v)
        vm.isFu = record.isFu
        setNumber(v, if (record.isFu && record.hymnType == MainActivity.HYMN_DB) HomeEntry.FU + (record.hymnNo - HymnNoValidate.HYMN_DB_NO_MAX) else record.hymnNo.toString())
        selectBook(v, record.hymnType)
        openHymn(record.hymnType, record.hymnNo)
    }

    private fun deleteHistory(record: HistoryRecord) {
        val appContext = requireContext().applicationContext
        AppExecutors.io("home-history-delete") {
            val deleted = DatabaseBackend.getInstance(appContext).deleteHymnHistory(record) == 1
            AppExecutors.MAIN.post {
                if (!deleted) {
                    // The row was already gone (or the delete failed): reload so the list tells the truth
                    if (views != null) loadHistory()
                    return@post
                }
                if (views != null) {
                    historyAdapter.submitList(historyAdapter.currentList.filterNot { it === record })
                }
            }
        }
    }

    private fun confirmDeleteHistory(record: HistoryRecord) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete)
            .setMessage(getString(R.string.delete_history, record.toString()))
            .setPositiveButton(R.string.delete) { _, _ -> deleteHistory(record) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private companion object {
        const val STROKE_NORMAL_DP = 1
        const val STROKE_SELECTED_DP = 4
    }
}

/** Looks an English hymn number up in toc_all_eng2ch.txt (the old MainActivity.showHymnFromEng logic). */
internal object EnglishCrossRef {
    private const val TOC = "lyrics_toc/toc_all_eng2ch.txt"
    private val LINE_BREAK = Regex("\r\n|\n")
    private val TARGET = Regex(".+? #(.+?)")

    /**
     * @return the Chinese counterpart as (hymnType, hymnNo), or null when there is none
     * @throws IOException when the table cannot be read
     */
    fun find(context: Context, hymnEng: Int, dbPage: Boolean): Pair<String, Int>? {
        val lines = context.assets.open(TOC).bufferedReader(Charsets.UTF_8).use { it.readText() }.split(LINE_BREAK)
        val key = Regex(String.format(Locale.CHINA, "\\^ %04d:.+?", hymnEng))
        val idx = lines.indexOfFirst { key.matches(it) }
        if (idx == -1) return null

        // The DB page, when present, is the line right after the BB one
        val line = if (dbPage && lines.getOrNull(idx + 1)?.let { key.matches(it) } == true) lines[idx + 1] else lines[idx]
        val tn = line.replace(TARGET, "$1")
        val no = tn.substring(2).toIntOrNull() ?: return null
        return MainActivity.getHymnType(tn) to no
    }
}
