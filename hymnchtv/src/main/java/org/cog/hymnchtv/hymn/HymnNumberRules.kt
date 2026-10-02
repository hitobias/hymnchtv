package org.cog.hymnchtv.hymn

import org.cog.hymnchtv.notebook.model.HymnNumbering
import org.cog.hymnchtv.notebook.model.HymnTypes

/** Which hymn numbers exist in which book, built on [HymnNumbering] (no Android dependency). */
object HymnNumberRules {
    /** Same as HymnNoValidate.HYMN_YB_NO_MAX (asserted by HymnNumberRulesAssetTest); HymnNumbering only knows the total 277. */
    const val YB_NO_MAX = 275

    private const val MAX_DIGITS = 4

    /** The last plain number of books that have an appendix (附) after it; null when the book has none. */
    fun fuOffset(book: String): Int? = when (book) {
        HymnTypes.DB -> HymnNumbering.DB_NO_MAX
        HymnTypes.YB -> YB_NO_MAX
        else -> null
    }

    /** Stored number for what was typed, or null when no such hymn exists. */
    fun storedOf(book: String, number: Int, isFu: Boolean): Int? {
        if (number < 1) return null
        val offset = fuOffset(book)
        val stored = if (isFu) (offset ?: return null) + number else number
        if (!isFu && offset != null && number > offset) return null
        return stored.takeIf { HymnNumbering.isValid(book, it) }
    }

    fun isValid(book: String, number: Int, isFu: Boolean): Boolean = storedOf(book, number, isFu) != null

    private val storedCache = HashMap<String, List<Int>>()

    /** Every valid stored number of [book], ascending. */
    fun storedNumbers(book: String): List<Int> = synchronized(storedCache) {
        storedCache.getOrPut(book) { (1..maxStored(book)).filter { HymnNumbering.isValid(book, it) } }
    }

    private fun maxStored(book: String): Int = when (book) {
        HymnTypes.DB -> HymnNumbering.DB_NO_TMAX
        HymnTypes.BB -> HymnNumbering.BB_NO_MAX
        HymnTypes.ER -> HymnNumbering.ER_NO_MAX
        HymnTypes.XB -> HymnNumbering.XB_NO_MAX
        HymnTypes.XG -> HymnNumbering.XG_NO_MAX
        HymnTypes.YB -> HymnNumbering.YB_NO_TMAX
        else -> 0
    }

    /** The numbers a user can type in [book]: the appendix numbers when [isFu], the plain ones otherwise. */
    fun displayNumbers(book: String, isFu: Boolean): List<Int> {
        val offset = fuOffset(book)
        return storedNumbers(book).filter { (offset != null && it > offset) == isFu }.map { if (isFu) it - offset!! else it }
    }

    /** Whether typing [digit] after [prefix] can still lead to an existing hymn number. */
    fun canAppendDigit(book: String, prefix: String, isFu: Boolean, digit: Int): Boolean =
        canAppend(prefix, digit, displayNumbers(book, isFu))

    /** Same rule for English hymn numbers ([englishNumbers] is the set that has a Chinese counterpart). */
    fun canAppendEnglishDigit(prefix: String, digit: Int, englishNumbers: Collection<Int>): Boolean =
        canAppend(prefix, digit, englishNumbers)

    private fun canAppend(prefix: String, digit: Int, numbers: Collection<Int>): Boolean {
        if (digit !in 0..9) return false
        val next = prefix + digit
        if (next.length > MAX_DIGITS || next.startsWith("0")) return false
        return numbers.any { it.toString().startsWith(next) }
    }

    /** Other books (not English, not [except]) where [number] exists, in [HymnSource] order. */
    fun alsoValidIn(number: Int, isFu: Boolean, except: HymnSource): List<HymnSource> =
        HymnSource.entries.filter { it != except && it.book != null && isValid(it.book, number, isFu) }
}
