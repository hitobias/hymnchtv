package org.cog.hymnchtv.ui.toc

import android.content.Context
import org.cog.hymnchtv.HymnToc
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.toc.YbCrossRef
import org.cog.hymnchtv.utils.HymnNoValidate
import timber.log.Timber
import java.io.IOException
import java.util.Locale

/**
 * Builds the table of contents shown by HymnToc and by the TOC tab: category -> ordered "%04d: title" items.
 * This is the generation logic of HymnToc.getHymnToc/getHymnTocType/getHymnTitle moved here without changing its
 * output (the fixtures in test/resources/toc_fixtures were dumped from the original code and pin it).
 *
 * A reader returns an asset's text, or null when the asset is missing; it is a parameter so unit tests can read
 * the asset directory instead of needing an Android Context.
 */
object TocBuilder {
    private val LINE_BREAK = Regex("\r\n|\n")
    private val INDEX_HEADER = Regex("^.+画$|[A-Z]|[0-9~]+")
    private val YB_LINE = Regex("^#([0-9]+) (.+?) #([xyb]b[0-9]+[ab]*)")
    private const val TOC_DIR = "lyrics_toc/"
    private const val STROKE_FILE = "_stroke.txt"
    private const val PINYIN_FILE = "_pinyin.txt"
    private const val ENGLISH_FILE = "_eng2ch.txt"
    private const val FU_LABEL = "附"
    private const val TEXT_PREFIX_LENGTH = 2

    /** How one hymn book lays out its tables of contents. */
    private class Book(
        val code: String,
        val categories: Array<String>,
        val bounds: IntArray,
        val maxNo: Int,
        val hasEnglishToc: Boolean,
        /** Where a category continues in the next hundred (補充本, 儿童诗歌); resolved lazily, see [limits]. */
        val skipLimits: ((Limits) -> IntArray)? = null,
    )

    /** The numbers at which BB and ER categories jump to the next hundred (HymnNoValidate.rangeBbLimit / rangeErLimit). */
    private class Limits(val bb: IntArray, val er: IntArray)

    private val books: Map<String, Book> = mapOf(
        MainActivity.HYMN_DB to Book("db", TocData.hymnCategoryDb, TocData.categoryDb, HymnNoValidate.HYMN_DB_NO_TMAX, true),
        MainActivity.HYMN_BB to Book("bb", TocData.hymnCategoryBb, TocData.categoryBb, HymnNoValidate.HYMN_BB_NO_MAX, true) { it.bb },
        MainActivity.HYMN_XB to Book("xb", TocData.hymnCategoryXb, TocData.categoryXb, HymnNoValidate.HYMN_XB_NO_MAX, false),
        MainActivity.HYMN_XG to Book("xg", TocData.hymnCategoryXg, TocData.categoryXg, HymnNoValidate.HYMN_XG_NO_MAX, true),
        MainActivity.HYMN_YB to Book("yb", TocData.hymnCategoryYb, TocData.categoryYb, HymnNoValidate.HYMN_YB_NO_TMAX, false),
        MainActivity.HYMN_ER to Book("er", TocData.hymnCategoryEr, TocData.categoryEr, HymnNoValidate.HYMN_ER_NO_MAX, false) { it.er },
    )

    /** Convenience for the app: reads assets through [context]. Same arguments as HymnToc.getHymnToc had. */
    @JvmStatic
    fun build(context: Context, hymnType: String, tocPage: String): LinkedHashMap<String, List<String>> {
        val assets = context.applicationContext.assets
        val reader = { path: String ->
            try {
                assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
            } catch (e: IOException) {
                Timber.w("Content toc not available: %s: %s", path, e.message)
                null
            }
        }
        return build(reader, hymnType, tocPage, HymnNoValidate.rangeBbLimit, HymnNoValidate.rangeErLimit)
    }

    /**
     * @param hymnType one of the MainActivity.HYMN_* book types; an unknown one gives an empty map
     * @param tocPage one of HymnToc.TOC_CATEGORY / TOC_STROKE / TOC_PINYIN / TOC_ENGLISH; any other (e.g. TOC_TITLE) gives an empty map
     */
    fun build(
        reader: (String) -> String?,
        hymnType: String,
        tocPage: String,
        bbLimit: IntArray,
        erLimit: IntArray,
    ): LinkedHashMap<String, List<String>> {
        val book = books[hymnType] ?: return LinkedHashMap()
        return when (tocPage) {
            HymnToc.TOC_CATEGORY -> if (hymnType == MainActivity.HYMN_YB) ybCategories(book, reader) else categories(book, reader, Limits(bbLimit, erLimit))
            HymnToc.TOC_STROKE -> indexed(reader, "${TOC_DIR}toc_${book.code}$STROKE_FILE")
            HymnToc.TOC_PINYIN -> indexed(reader, "${TOC_DIR}toc_${book.code}$PINYIN_FILE")
            HymnToc.TOC_ENGLISH -> if (book.hasEnglishToc) indexed(reader, "${TOC_DIR}toc_${book.code}$ENGLISH_FILE") else LinkedHashMap()
            else -> LinkedHashMap()
        }
    }

    /**
     * The hymn number a TOC item stands for: the number after "#" (stroke/pinyin/English lists), else the one before ":".
     * @return null when the item holds no number
     */
    @JvmStatic
    fun hymnNoOf(item: String): Int? {
        val hash = item.lastIndexOf('#')
        return if (hash != -1) item.substring(hash + 1).toIntOrNull() else item.split(':')[0].trim().toIntOrNull()
    }

    /** Splits like Java's String.split: trailing empty lines are dropped (the index walk below must not see them). */
    private fun String.toLines(): List<String> = split(LINE_BREAK).dropLastWhile { it.isEmpty() }

    // ---- by category: walk the book's numbers, cutting at the category bounds ----

    private fun categories(book: Book, reader: (String) -> String?, limits: Limits): LinkedHashMap<String, List<String>> {
        val skips = book.skipLimits?.invoke(limits)
        val result = LinkedHashMap<String, List<String>>()
        var hymnNo = 1
        for (x in 0 until book.bounds.size - 1) {
            val items = ArrayList<String>()
            val range = book.bounds[x] until book.bounds[x + 1]
            while (hymnNo <= book.maxNo) {
                // Some categories end early; the next hundred starts after the gap
                if (skips != null && hymnNo == skips[x]) hymnNo = 100 * (x + 1) + 1
                if (hymnNo !in range) break
                items.add(categoryItem(book, hymnNo, reader))
                hymnNo++
            }
            result[book.categories[x]] = items
        }
        return result
    }

    private fun categoryItem(book: Book, hymnNo: Int, reader: (String) -> String?): String {
        val title = hymnTitle(hymnNo, "lyrics_${book.code}_text/${book.code}$hymnNo.txt", reader)
        // 大本 supplement hymns are numbered on from 780: show them as 附N
        return if (book.code == "db" && hymnNo > HymnNoValidate.HYMN_DB_NO_MAX) {
            title.replace(": ", ": $FU_LABEL${hymnNo - HymnNoValidate.HYMN_DB_NO_MAX}-")
        } else title
    }

    /** "%04d: title（note）": line 2 minus its category, plus the "（…）" part of line 3. Empty when the file is missing. */
    private fun hymnTitle(hymnNo: Int, path: String, reader: (String) -> String?): String {
        val lines = (reader(path) ?: return "").toLines()
        var title = lines.getOrNull(1) ?: return ""
        title = title.substringAfterLast("－")
        val note = lines.getOrNull(2)?.let { third -> third.indexOf("（").takeIf { it != -1 }?.let(third::substring) }
        return String.format(Locale.CHINA, "%04d: %s", hymnNo, title + note.orEmpty())
    }

    /** 青年诗歌 has no lyrics-title walk: its list is the cross-reference table, "#1 title #bb876" shown as "1: title (bb876)". */
    private fun ybCategories(book: Book, reader: (String) -> String?): LinkedHashMap<String, List<String>> {
        val result = LinkedHashMap<String, List<String>>()
        val lines = (reader(YbCrossRef.ASSET) ?: return result).toLines()
        var hymnNo = 1
        for (x in 0 until book.bounds.size - 1) {
            val items = ArrayList<String>()
            val range = book.bounds[x] until book.bounds[x + 1]
            while (hymnNo <= book.maxNo && hymnNo in range) {
                val line = lines.getOrNull(hymnNo - 1) ?: break
                items.add(line.replace(YB_LINE, "$1: $2 ($3)"))
                hymnNo++
            }
            result[book.categories[x]] = items
        }
        return result
    }

    // ---- stroke / pinyin / English index files ----

    /**
     * Parses an index file: a header line (N画, a letter, a number range) opens a group; the "^ " lines after it are its items.
     * Stroke and pinyin groups list the first characters they hold and sort their items; the English cross-reference keeps both as is.
     */
    private fun indexed(reader: (String) -> String?, file: String): LinkedHashMap<String, List<String>> {
        val result = LinkedHashMap<String, List<String>>()
        val lines = (reader(file) ?: return result).toLines()
        val isEnglish = file.contains(ENGLISH_FILE)

        var category = ""
        var items = ArrayList<String>()
        var index = StringBuilder("（")
        var ml = 0
        while (ml < lines.size) {
            val start = ml
            if (INDEX_HEADER.matches(lines[ml])) {
                category = lines[ml++]
                items = ArrayList()
                index = StringBuilder("（")
            }
            while (ml < lines.size && lines[ml].startsWith("^ ")) {
                val first = lines[ml].substring(TEXT_PREFIX_LENGTH, TEXT_PREFIX_LENGTH + 1)
                if (!index.contains(first)) index.append(first)
                items.add(lines[ml++].substring(TEXT_PREFIX_LENGTH))
            }
            if (!isEnglish) {
                category += "$index）"
                items.sort()
            }
            result[category] = items
            // A line that is neither header nor item would never advance; skip it instead of looping forever
            if (ml == start) ml++
        }
        return result
    }
}
