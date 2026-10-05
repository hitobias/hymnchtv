package org.cog.hymnchtv.nav

import org.cog.hymnchtv.hymn.HymnRef

/** A hymn left by a jump: where the back key returns to, and where the reader was on it. */
data class JumpEntry(val ref: HymnRef, val position: ReadingPosition)

/**
 * The lyrics page's jump state (H5), immutable: the return stack (oldest first, at most [MAX_STACK]) and the single
 * "play next" slot. It lives as long as the lyrics page; [encode]/[decode] carry it across a recreation (and, on the
 * fallback path of plan Task 5B, across a relaunch of the page).
 */
data class JumpState(val stack: List<JumpEntry> = emptyList(), val slot: HymnRef? = null) {

    /** A returned-to entry and the state without it (and without every newer entry). */
    data class Popped(val entry: JumpEntry, val state: JumpState)

    fun canReturn(): Boolean = stack.isNotEmpty()

    /** Newest first, as the panel lists them; index 0 is where the back key goes. */
    fun recentFirst(): List<JumpEntry> = stack.asReversed()

    /** Adds [entry] on top; the same hymn already on top is replaced, the oldest entry falls off past [MAX_STACK]. */
    fun push(entry: JumpEntry): JumpState {
        val base = if (stack.lastOrNull()?.ref == entry.ref) stack.dropLast(1) else stack
        return copy(stack = (base + entry).takeLast(MAX_STACK))
    }

    /** Returns to the [recentIndex]-th newest entry (0 = the back key); null when there is no such entry. */
    fun popRecent(recentIndex: Int): Popped? {
        if (recentIndex !in stack.indices) return null
        val at = stack.lastIndex - recentIndex
        return Popped(stack[at], copy(stack = stack.subList(0, at).toList()))
    }

    fun withSlot(ref: HymnRef?): JumpState = copy(slot = ref)

    /** Without the newest entries that name [ref]: going back to the hymn already on screen would do nothing. */
    fun withoutTop(ref: HymnRef): JumpState = copy(stack = stack.dropLastWhile { it.ref == ref })

    /** "j1|slot|book,no,y,h|..." (slot is "-" when empty); small enough for a Bundle or an Intent extra. */
    fun encode(): String = buildString {
        append(VERSION)
        append(SEP_PART).append(slot?.let(::refText) ?: NONE)
        stack.forEach { append(SEP_PART).append(refText(it.ref)).append(SEP_FIELD).append(it.position.encode()) }
    }

    companion object {
        const val MAX_STACK = 10

        @JvmField
        val EMPTY = JumpState()

        private const val VERSION = "j1"
        private const val SEP_PART = '|'
        private const val SEP_FIELD = ','
        private const val NONE = "-"

        private fun refText(ref: HymnRef) = "${ref.book}$SEP_FIELD${ref.storedNo}"

        /** Never throws: null, foreign or damaged text gives [EMPTY]; entries that name no real hymn are dropped. */
        @JvmStatic
        fun decode(text: String?): JumpState {
            val parts = text?.split(SEP_PART) ?: return EMPTY
            if (parts.size < 2 || parts[0] != VERSION) return EMPTY
            val slot = parts[1].takeIf { it != NONE }?.let { parseRef(it.split(SEP_FIELD)) }
            val stack = parts.drop(2).mapNotNull { parseEntry(it.split(SEP_FIELD)) }.takeLast(MAX_STACK)
            return JumpState(stack, slot)
        }

        private fun parseRef(fields: List<String>): HymnRef? {
            if (fields.size < 2) return null
            val no = fields[1].toIntOrNull() ?: return null
            return HymnRef(fields[0], no).takeIf { it.isValid }
        }

        private fun parseEntry(fields: List<String>): JumpEntry? {
            if (fields.size != 4) return null
            val ref = parseRef(fields) ?: return null
            val position = ReadingPosition.decode("${fields[2]}$SEP_FIELD${fields[3]}") ?: return null
            return JumpEntry(ref, position)
        }
    }
}
