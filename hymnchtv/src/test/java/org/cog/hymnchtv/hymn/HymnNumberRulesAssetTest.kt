package org.cog.hymnchtv.hymn

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.cog.hymnchtv.utils.HymnNoValidate
import org.junit.Test
import java.io.File

class HymnNumberRulesAssetTest {
    private val assets = File(checkNotNull(System.getProperty("hymnchtv.assetsDir")) { "hymnchtv.assetsDir not set" })

    private fun assetNumbers(prefix: String): List<Int> =
        File(assets, "lyrics_${prefix}_text").listFiles().orEmpty()
            .mapNotNull { Regex("^$prefix(\\d+)\\.txt$").matchEntire(it.name)?.groupValues?.get(1)?.toInt() }.sorted()

    @Test fun rulesEqualTheLyricsFilesPerBook() {
        mapOf(
            HymnTypes.DB to "db", HymnTypes.BB to "bb", HymnTypes.XB to "xb", HymnTypes.XG to "xg", HymnTypes.ER to "er",
        ).forEach { (book, prefix) ->
            assertWithMessage(book).that(HymnNumberRules.storedNumbers(book)).containsExactlyElementsIn(assetNumbers(prefix)).inOrder()
        }
    }

    @Test fun youthBookCoversOneToTwoHundredSeventySeven() {
        assertThat(HymnNumberRules.storedNumbers(HymnTypes.YB)).isEqualTo((1..277).toList())
    }

    @Test fun youthOffsetMatchesHymnNoValidate() {
        assertThat(HymnNumberRules.YB_NO_MAX).isEqualTo(HymnNoValidate.HYMN_YB_NO_MAX)
    }
}
