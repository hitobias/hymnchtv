package org.cog.hymnchtv.ui.host

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** The navigation structure of the single-page home: pages are classified by their back stack name. */
class MainHostTabsTest {
    @Test
    fun tocAndSettingsAreFullPages() {
        assertThat(MainHost.isFullPage(MainHost.TAG_TOC)).isTrue()
        assertThat(MainHost.isFullPage(MainHost.TAG_SETTINGS)).isTrue()
        assertThat(MainHost.isOverlay(MainHost.TAG_TOC)).isFalse()
    }

    @Test
    fun historyAndSearchAreOverlays() {
        assertThat(MainHost.isOverlay(MainHost.TAG_HISTORY)).isTrue()
        assertThat(MainHost.isOverlay(MainHost.TAG_SEARCH)).isTrue()
        assertThat(MainHost.isFullPage(MainHost.TAG_SEARCH)).isFalse()
    }

    @Test
    fun unknownOrMissingNamesAreNeitherAndNoBottomNavigationIdsRemain() {
        assertThat(MainHost.isFullPage(null)).isFalse()
        assertThat(MainHost.isOverlay("x")).isFalse()
        val ids = org.cog.hymnchtv.R.id::class.java.fields.map { it.name }
        assertThat(ids).doesNotContain("bottom_nav")
    }
}
