package org.cog.hymnchtv.ui.playlist

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.cog.hymnchtv.R
import org.cog.hymnchtv.ui.page.PagePalette
import org.cog.hymnchtv.ui.picker.HymnLabels

/**
 * The hymns of a playlist. Drag only by the handle (no long press); while a drag runs, new data from the store is held back and
 * the final order goes to [onDragFinished]. TalkBack gets "Move up" / "Move down" actions that call [onMove].
 */
class PlaylistItemAdapter(
    private val onOpen: (Int) -> Unit,
    private val onRemove: (DetailItem) -> Unit,
    private val onMove: (Int, Int) -> Unit,
    private val onDragFinished: (List<String>) -> Unit,
    /** The store's current items and "next" index; read when a drag ends (falls back to the last submitted). */
    private val currentState: (() -> Pair<List<DetailItem>, Int?>)? = null,
) : RecyclerView.Adapter<PlaylistItemAdapter.Holder>() {
    private var items: List<DetailItem> = emptyList()
    private var nextIndex: Int? = null
    private var dragging = false
    private var touchHelper: ItemTouchHelper? = null
    private var recycler: RecyclerView? = null

    /** Latest data from the store, also while a drag holds it back. */
    private var latestItems: List<DetailItem> = emptyList()
    private var latestNext: Int? = null

    /** After a TalkBack move: the moved row gets accessibility focus and its new place is spoken, once the list shows it. */
    private var pendingFocusId: String? = null
    private var pendingAnnouncement: String? = null
    private var lastAnnouncement: String? = null

    init {
        setHasStableIds(true)
    }

    var palette: PagePalette? = null
        set(value) {
            if (field == value) return
            field = value
            notifyItemRangeChanged(0, itemCount)
        }

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val drag: ImageView = view.findViewById(R.id.item_drag)
        val headline: TextView = view.findViewById(R.id.item_headline)
        val title: TextView = view.findViewById(R.id.item_title)
        val next: TextView = view.findViewById(R.id.item_next)
        val remove: ImageButton = view.findViewById(R.id.item_remove)
        var actionIds: List<Int> = emptyList()
    }

    /** The items on screen (for tests). */
    fun currentItems(): List<DetailItem> = items

    @SuppressLint("NotifyDataSetChanged")
    fun submit(newItems: List<DetailItem>, next: Int?) {
        latestItems = newItems
        latestNext = next
        if (dragging) return
        show(newItems, next)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun show(newItems: List<DetailItem>, next: Int?) {
        items = newItems
        nextIndex = next
        notifyDataSetChanged()
        announcePendingMove()
    }

    private fun announcePendingMove() {
        val id = pendingFocusId ?: return
        val text = pendingAnnouncement
        if (items.none { it.itemId == id }) return
        pendingFocusId = null
        pendingAnnouncement = null
        recycler?.post {
            val view = recycler?.findViewHolderForItemId(stableId(id))?.itemView ?: return@post
            view.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED)
            if (text != null) {
                lastAnnouncement = text
                view.announceForAccessibility(text)
            }
        }
    }

    internal fun beginDrag() {
        dragging = true
    }

    /** The drag is over: show what the store holds now (updates that arrived during the drag were held back). */
    internal fun endDrag(list: RecyclerView) {
        dragging = false
        // Handler, not View.post: a detached list would never run it
        Handler(Looper.getMainLooper()).post {
            if (dragging) return@post
            val state = currentState?.invoke() ?: (latestItems to latestNext)
            show(state.first, state.second)
        }
    }

    internal fun lastAnnouncement(): String? = lastAnnouncement

    fun attachDrag(list: RecyclerView) {
        val helper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun isLongPressDragEnabled() = false

            override fun onMove(rv: RecyclerView, from: RecyclerView.ViewHolder, to: RecyclerView.ViewHolder): Boolean {
                val a = from.bindingAdapterPosition
                val b = to.bindingAdapterPosition
                if (a == RecyclerView.NO_POSITION || b == RecyclerView.NO_POSITION) return false
                items = ItemMoves.move(items, a, b)
                notifyItemMoved(a, b)
                return true
            }

            override fun onSwiped(holder: RecyclerView.ViewHolder, direction: Int) = Unit

            override fun onSelectedChanged(holder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(holder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) beginDrag()
            }

            override fun clearView(rv: RecyclerView, holder: RecyclerView.ViewHolder) {
                super.clearView(rv, holder)
                if (dragging) {
                    onDragFinished(items.map { it.itemId })
                    endDrag(rv)
                }
            }
        })
        helper.attachToRecyclerView(list)
        recycler = list
        touchHelper = helper
    }

    override fun getItemCount() = items.size

    override fun getItemId(position: Int): Long = stableId(items[position].itemId)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(LayoutInflater.from(parent.context).inflate(R.layout.row_playlist_item, parent, false))

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        val ctx = holder.itemView.context
        holder.headline.text = HymnLabels.headline(ctx, item.ref)
        holder.title.text = item.title.orEmpty()
        holder.title.visibility = if (item.title.isNullOrBlank()) View.GONE else View.VISIBLE
        holder.next.visibility = if (position == nextIndex) View.VISIBLE else View.GONE
        palette?.let { p ->
            holder.headline.setTextColor(p.onCard)
            holder.title.setTextColor(p.muted)
            holder.next.setTextColor(p.onCard)
            holder.drag.imageTintList = ColorStateList.valueOf(p.muted)
            holder.remove.imageTintList = ColorStateList.valueOf(p.muted)
        }
        holder.itemView.contentDescription = ctx.getString(
            R.string.c_when_desc,
            HymnLabels.spoken(ctx, item.ref, item.title),
            ctx.getString(R.string.playlist_position, position + 1, items.size),
        )
        holder.itemView.setOnClickListener { positionOf(holder)?.let(onOpen) }
        holder.remove.setOnClickListener { positionOf(holder)?.let { onRemove(items[it]) } }
        holder.drag.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) touchHelper?.startDrag(holder)
            false
        }
        bindMoveActions(holder, position)
    }

    private fun positionOf(holder: Holder): Int? = holder.bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }

    private fun bindMoveActions(holder: Holder, position: Int) {
        holder.actionIds.forEach { ViewCompat.removeAccessibilityAction(holder.itemView, it) }
        val ctx = holder.itemView.context
        val up = if (position > 0) {
            ViewCompat.addAccessibilityAction(holder.itemView, ctx.getString(R.string.playlist_move_up)) { _, _ -> moveBy(holder, -1) }
        } else {
            null
        }
        val down = if (position < items.size - 1) {
            ViewCompat.addAccessibilityAction(holder.itemView, ctx.getString(R.string.playlist_move_down)) { _, _ -> moveBy(holder, 1) }
        } else {
            null
        }
        holder.actionIds = listOfNotNull(up, down)
    }

    private fun moveBy(holder: Holder, delta: Int): Boolean {
        val from = positionOf(holder) ?: return false
        val to = from + delta
        if (to !in items.indices) return false
        val ctx = holder.itemView.context
        pendingFocusId = items[from].itemId
        pendingAnnouncement = ctx.getString(R.string.playlist_moved, to + 1, items.size)
        onMove(from, to)
        return true
    }

    private companion object {
        /** A stable 64-bit id for an item id (a UUID string), so RecyclerView keeps a moved row's view. */
        fun stableId(itemId: String): Long = itemId.fold(1125899906842597L) { h, c -> 31 * h + c.code }
    }
}
