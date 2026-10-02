package org.cog.hymnchtv.ui.home

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.hymnhistory.HistoryRecord

/** Full-screen list of the recently opened hymns (over the tabs): tap opens, the trailing button or a swipe deletes, a long press asks first. */
class HistoryFragment : Fragment(R.layout.fragment_history) {
    private var list: RecyclerView? = null
    private var empty: View? = null
    private lateinit var adapter: HistoryAdapter

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
                if (record != null) delete(record)
            }

            override fun getSwipeDirs(rv: RecyclerView, holder: RecyclerView.ViewHolder): Int =
                if (holder is HistoryAdapter.RowHolder) super.getSwipeDirs(rv, holder) else 0
        }
        ItemTouchHelper(swipe).attachToRecyclerView(recycler)
    }

    override fun onStart() {
        super.onStart()
        (activity as? AppCompatActivity)?.supportActionBar?.setTitle(R.string.c_history_title)
        reload()
    }

    override fun onDestroyView() {
        list = null
        empty = null
        super.onDestroyView()
    }

    private fun reload() {
        HistoryActions.load(requireContext()) { records -> if (list != null) show(records) }
    }

    /** The records on screen (without day headings), newest first. */
    private var shown: List<HistoryRecord> = emptyList()

    private fun show(records: List<HistoryRecord>) {
        shown = records
        adapter.submitList(HistoryAdapter.group(requireContext(), records, System.currentTimeMillis()))
        empty?.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun open(record: HistoryRecord) {
        val ref = HistoryActions.refOf(record)
        MainActivity.setHymnTypeNo(ref.book, ref.storedNo)
        MainActivity.showContent(requireContext(), ref.book, ref.storedNo, false)
    }

    private fun delete(record: HistoryRecord) {
        HistoryActions.delete(requireContext(), record) { deleted ->
            if (list == null) return@delete
            // The row was already gone (or the delete failed): reload so the list tells the truth
            if (deleted) show(shown.filterNot { it === record }) else reload()
        }
    }

    private fun confirmDelete(record: HistoryRecord) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete)
            .setMessage(getString(R.string.delete_history, record.toString()))
            .setPositiveButton(R.string.delete) { _, _ -> delete(record) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
