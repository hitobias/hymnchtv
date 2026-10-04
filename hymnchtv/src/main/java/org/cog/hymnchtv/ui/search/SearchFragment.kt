package org.cog.hymnchtv.ui.search

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.hymn.HymnSource
import org.cog.hymnchtv.search.SearchPage
import org.cog.hymnchtv.search.SearchResult
import org.cog.hymnchtv.search.SearchScope
import org.cog.hymnchtv.ui.picker.HymnLabels

/** Full-screen search over titles and lyrics, shown over the tabs. The result is kept by [SearchViewModel]. */
class SearchFragment : Fragment(R.layout.fragment_search) {
    private val vm: SearchViewModel by viewModels()
    private val handler = Handler(Looper.getMainLooper())
    private var views: Views? = null
    private lateinit var adapter: SearchAdapter

    private class Views(root: View) {
        val input: TextInputEditText = root.findViewById(R.id.search_input)
        val scope: ChipGroup = root.findViewById(R.id.search_scope)
        val current: Chip = root.findViewById(R.id.scope_current)
        val all: Chip = root.findViewById(R.id.scope_all)
        val status: TextView = root.findViewById(R.id.tv_search_status)
        val progress: ProgressBar = root.findViewById(R.id.search_progress)
        val results: RecyclerView = root.findViewById(R.id.search_results)
        val empty: View = root.findViewById(R.id.search_empty)
    }

    private val prefs: SharedPreferences
        get() = requireContext().getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm.init(arguments?.getString(ARG_BOOK))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val v = Views(view)
        views = v
        adapter = SearchAdapter(::open)
        v.results.layoutManager = LinearLayoutManager(requireContext())
        v.results.adapter = adapter

        bindScope(v)
        v.input.setText(vm.query)
        v.input.doAfterTextChanged { text ->
            handler.removeCallbacksAndMessages(null)
            val typed = text?.toString().orEmpty()
            if (typed != vm.query) handler.postDelayed({ startSearch(typed) }, DEBOUNCE_MS)
        }
        v.input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                handler.removeCallbacksAndMessages(null)
                startSearch(v.input.text?.toString().orEmpty())
                true
            } else {
                false
            }
        }
        vm.onChanged = { render() }
        render()
        if (vm.query.isEmpty()) showKeyboard(v.input)
    }

    override fun onStart() {
        super.onStart()
        (activity as? AppCompatActivity)?.supportActionBar?.setTitle(R.string.c_search_title)
    }

    override fun onDestroyView() {
        handler.removeCallbacksAndMessages(null)
        // the ViewModel outlives a rotation and keeps the result; it stops a running search itself when the page is closed
        vm.onChanged = null
        views = null
        super.onDestroyView()
    }

    private fun bindScope(v: Views) {
        val book = vm.currentBook
        val source = book?.let { HymnSource.ofBook(it) }
        v.current.visibility = if (source == null) View.GONE else View.VISIBLE
        if (source != null) v.current.text = getString(R.string.c_scope_current, HymnLabels.longName(requireContext(), source))
        v.scope.check(if (vm.scope is SearchScope.All) R.id.scope_all else R.id.scope_current)
        v.scope.setOnCheckedStateChangeListener { _, ids ->
            val next = if (ids.firstOrNull() == R.id.scope_current && book != null) SearchScope.Book(book) else SearchScope.All
            vm.setScope(next, requireContext(), prefs)
        }
    }

    private fun startSearch(text: String) {
        if (views == null) return
        vm.search(text.trim(), requireContext(), prefs)
    }

    private fun render() {
        val v = views ?: return
        val state = vm.state
        v.progress.visibility = if (state is SearchUiState.Loading) View.VISIBLE else View.GONE
        v.empty.visibility = if (state is SearchUiState.Done && state.page.results.isEmpty() && !state.page.hasMore) View.VISIBLE else View.GONE
        when (state) {
            SearchUiState.Idle -> {
                adapter.submitList(emptyList())
                v.status.setText(R.string.c_search_empty_hint)
            }
            SearchUiState.Loading -> v.status.setText(R.string.c_search_loading)
            is SearchUiState.Done -> showPage(v, state.page)
        }
    }

    private fun showPage(v: Views, page: SearchPage) {
        adapter.submitList(page.results)
        v.status.text = when {
            page.hasMore -> getString(R.string.c_search_status_more)
            page.results.isEmpty() -> getString(R.string.c_search_status_none)
            else -> getString(R.string.c_search_status_count, page.results.size)
        }
    }

    private fun open(result: SearchResult) {
        val ref = result.ref
        MainActivity.setHymnTypeNo(ref.book, ref.storedNo)
        MainActivity.showContent(requireContext(), ref.book, ref.storedNo, false)
    }

    private fun showKeyboard(input: View) {
        input.requestFocus()
        input.post {
            (input.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                ?.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    companion object {
        private const val ARG_BOOK = "search_book"
        private const val DEBOUNCE_MS = 300L

        /** [book] is the source chosen on the home tab; null (English) searches all books. */
        fun newInstance(book: String?): SearchFragment = SearchFragment().apply {
            arguments = Bundle().apply { putString(ARG_BOOK, book) }
        }
    }
}
