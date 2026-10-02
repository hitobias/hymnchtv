package org.cog.hymnchtv.toc

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class YbCrossRefTest {
    @Test
    fun parsesCrossReferencesAndSkipsYbToYb() {
        val text = "#0001 神就是爱 #bb876\r\n#0022 将一生奉献 #yb22\r\n#0245 你是我的喜乐冠冕 #xb161"
        assertThat(YbCrossRef.parse(text)).containsExactly(1, "bb876", 245, "xb161").inOrder()
    }

    @Test
    fun skipsMalformedLines() {
        val text = "\n#abc 标题 #bb1\n#0002 只有两栏\n#0003 标题 #\n#0004 标题 #bb9"
        assertThat(YbCrossRef.parse(text)).containsExactly(4, "bb9")
    }

    @Test
    fun bundledAssetHas113CrossReferences() {
        val assets = System.getProperty("hymnchtv.assetsDir")
        val table = YbCrossRef.parse(File(assets, YbCrossRef.ASSET).readText(Charsets.UTF_8))
        assertThat(table).hasSize(113)
        assertThat(table[1]).isEqualTo("bb876")
        assertThat(table[245]).isEqualTo("xb161")
        assertThat(table).doesNotContainKey(22)
    }
}
