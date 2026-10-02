package org.cog.hymnchtv.hymn

import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.junit.Test
import java.io.File
import java.util.Locale

class EnglishXRefTest {
    private val assets = File(checkNotNull(System.getProperty("hymnchtv.assetsDir")) { "hymnchtv.assetsDir not set" })
    private val realText by lazy { File(assets, EnglishXRef.ASSET).readText(Charsets.UTF_8) }

    @Test fun parseHandlesDoubleTargetsHashesInTitlesAndNonDataLines() {
        val xref = EnglishXRef.parse(
            "0001~0099\r\n^ 0001: 他的计划 #db1\r\n^ 0005: 含 # 号的标题 #bb12\r\n^ 0005: 同号 #db6\r\n^ 0007: 新诗 #xg3\r\n垃圾行\r\n",
        )
        assertThat(xref.candidates(1)).containsExactly(HymnRef(HymnTypes.DB, 1))
        assertThat(xref.candidates(5)).containsExactly(HymnRef(HymnTypes.BB, 12), HymnRef(HymnTypes.DB, 6)).inOrder()
        assertThat(xref.candidates(7)).containsExactly(HymnRef(HymnTypes.XG, 3))
        assertThat(xref.has(2)).isFalse()
        assertThat(xref.candidates(2)).isEmpty()
        assertThat(xref.numbers).containsExactly(1, 5, 7).inOrder()
    }

    /** The 12-line algorithm of the old HomeFragment.EnglishCrossRef, kept here as the behaviour oracle. */
    private fun oldFind(lines: List<String>, eng: Int, dbPage: Boolean): Pair<String, Int>? {
        val key = Regex(String.format(Locale.CHINA, "\\^ %04d:.+?", eng))
        val idx = lines.indexOfFirst { key.matches(it) }
        if (idx == -1) return null
        val line = if (dbPage && lines.getOrNull(idx + 1)?.let { key.matches(it) } == true) lines[idx + 1] else lines[idx]
        val tn = line.replace(Regex(".+? #(.+?)"), "$1")
        val no = tn.substring(2).toIntOrNull() ?: return null
        val type = when {
            tn.startsWith("xg") -> HymnTypes.XG
            tn.startsWith("db") -> HymnTypes.DB
            else -> HymnTypes.BB
        }
        return type to no
    }

    @Test fun matchesTheOldShortAndLongPressForEveryEnglishNumber() {
        val xref = EnglishXRef.parse(realText)
        val lines = realText.split(Regex("\r\n|\n"))
        assertThat(xref.numbers).isNotEmpty()
        for (eng in 1..1300) {
            val first = oldFind(lines, eng, dbPage = false)
            val second = oldFind(lines, eng, dbPage = true)
            val c = xref.candidates(eng)
            if (first == null) {
                assertThat(c).isEmpty()
                continue
            }
            assertThat(c.first().book to c.first().storedNo).isEqualTo(first)
            // old long press: the second line when it belongs to the same English number, else the first one
            assertThat((c.getOrNull(1) ?: c.first()).let { it.book to it.storedNo }).isEqualTo(second)
        }
    }

    @Test fun numbersCountEqualsDistinctEnglishNumbersInTheAsset() {
        // line 729 of the asset ("^ 1099: ... #bb71100~1199") lost its line break; the old lookup could not open it either
        val distinct = Regex("""(?m)^\^ (\d{4}):.* #(?:db|bb|xg)\d+[ \t]*\r?$""").findAll(realText).map { it.groupValues[1] }.toSet()
        assertThat(EnglishXRef.parse(realText).numbers).hasSize(distinct.size)
    }
}
