package org.cog.hymnchtv.hymn

import org.cog.hymnchtv.notebook.model.HymnTypes
import java.io.InputStream

/** English hymn number -> Chinese counterparts, from assets/lyrics_toc/toc_all_eng2ch.txt (no Android dependency). */
class EnglishXRef(private val byEnglish: Map<Int, List<HymnRef>>) {
    /** Counterparts in file order (the first one is what the old short press opened). */
    fun candidates(eng: Int): List<HymnRef> = byEnglish[eng].orEmpty()

    fun has(eng: Int): Boolean = byEnglish.containsKey(eng)

    /** English numbers that have a Chinese counterpart, ascending. */
    val numbers: List<Int> = byEnglish.keys.sorted()

    companion object {
        const val ASSET = "lyrics_toc/toc_all_eng2ch.txt"
        val EMPTY = EnglishXRef(emptyMap())

        private val LINE_BREAK = Regex("\r\n|\n")
        private val LINE = Regex("""^\^ (\d{4}):.* #(db|bb|xg)(\d+)\s*$""")

        fun parse(text: String): EnglishXRef {
            val map = LinkedHashMap<Int, MutableList<HymnRef>>()
            for (line in text.split(LINE_BREAK)) {
                val m = LINE.matchEntire(line) ?: continue
                val eng = m.groupValues[1].toInt()
                val book = when (m.groupValues[2]) {
                    "db" -> HymnTypes.DB
                    "bb" -> HymnTypes.BB
                    else -> HymnTypes.XG
                }
                val no = m.groupValues[3].toIntOrNull() ?: continue
                map.getOrPut(eng) { ArrayList() }.add(HymnRef(book, no))
            }
            return EnglishXRef(map.mapValues { it.value.toList() })
        }

        fun fromAssets(open: () -> InputStream): EnglishXRef =
            parse(open().bufferedReader(Charsets.UTF_8).use { it.readText() })
    }
}
