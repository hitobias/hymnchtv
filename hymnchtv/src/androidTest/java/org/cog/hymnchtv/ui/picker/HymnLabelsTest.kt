package org.cog.hymnchtv.ui.picker

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.hymn.HymnRef
import org.cog.hymnchtv.hymnhistory.HistoryRecord
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HymnLabelsTest {
    private fun ctx(tag: String): Context {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val config = Configuration(base.resources.configuration).apply { setLocales(LocaleList.forLanguageTags(tag)) }
        return base.createConfigurationContext(config)
    }

    private val bb37 = HymnRef(HymnTypes.BB, 37)
    private val dbFu3 = HymnRef(HymnTypes.DB, 783)
    private val ybFu1 = HymnRef(HymnTypes.YB, 276)

    @Test fun traditionalLabels() {
        val c = ctx("zh-TW")
        assertThat(HymnLabels.headline(c, bb37)).isEqualTo("補充本 第 37 首")
        assertThat(HymnLabels.headline(c, dbFu3)).isEqualTo("大本詩歌 附 3")
        assertThat(HymnLabels.chip(c, bb37)).isEqualTo("補37")
        assertThat(HymnLabels.chip(c, dbFu3)).isEqualTo("大附3")
        assertThat(HymnLabels.chip(c, ybFu1)).isEqualTo("青附1")
        assertThat(HymnLabels.chip(c, HymnRef(HymnTypes.XG, 12))).isEqualTo("新詩12")
        assertThat(HymnLabels.spoken(c, bb37, "祂的計劃")).isEqualTo("補充本，第 37 首，祂的計劃")
    }

    @Test fun simplifiedLabels() {
        val c = ctx("zh-CN")
        assertThat(HymnLabels.headline(c, bb37)).isEqualTo("补充本 第 37 首")
        assertThat(HymnLabels.chip(c, ybFu1)).isEqualTo("青附1")
        assertThat(HymnLabels.chip(c, HymnRef(HymnTypes.DB, 123))).isEqualTo("大123")
    }

    @Test fun englishLabels() {
        val c = ctx("en-US")
        assertThat(HymnLabels.chip(c, dbFu3)).isEqualTo("Main App.3")
        assertThat(HymnLabels.chip(c, bb37)).isEqualTo("Supp 37")
        assertThat(HymnLabels.headline(c, ybFu1)).contains("Appx. 1")
        assertThat(HymnLabels.bookName(c, dbFu3)).isEqualTo("Hymns")
        assertThat(HymnLabels.headline(c, bb37)).isEqualTo("Supplement No. 37")
    }

    @Test fun youthFuIsShownAsFuNotAsTwoHundredSeventySix() {
        // the media layer stores isFu = 0 for the youth appendix; the history must still show 附1
        val record = HistoryRecord(HymnTypes.YB, 276, false, "標題", 1L)
        val text = record.toString()
        assertThat(text).contains("附1")
        assertThat(text).doesNotContain("276")
    }
}
