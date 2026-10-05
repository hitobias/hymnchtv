package org.cog.hymnchtv.ui.playlist

/** Reordering for drag and for the TalkBack "move up / move down" actions. */
object ItemMoves {
    /** A new list with the element at [from] moved to [to]; the same list when nothing moves or an index is out of range. */
    fun <T> move(list: List<T>, from: Int, to: Int): List<T> {
        if (from !in list.indices || to !in list.indices || from == to) return list
        val rest = list.filterIndexed { index, _ -> index != from }
        return rest.take(to) + list[from] + rest.drop(to)
    }
}
