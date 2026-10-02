package org.cog.hymnchtv.ui.home

import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.MainActivity.HYMN_BB
import org.cog.hymnchtv.MainActivity.HYMN_DB
import org.cog.hymnchtv.MainActivity.HYMN_YB
import org.junit.Test

class HomeEntryTest {
    @Test
    fun plainNumber() {
        assertThat(HomeEntry.hymnNo("12", HYMN_DB)).isEqualTo(12)
    }

    @Test
    fun emptyOrZeroIsNull() {
        assertThat(HomeEntry.hymnNo("", HYMN_DB)).isNull()
        assertThat(HomeEntry.hymnNo("0", HYMN_DB)).isNull()
    }

    @Test
    fun fuContinuesFromTheBooksLastNumber() {
        assertThat(HomeEntry.hymnNo("附1", HYMN_DB)).isEqualTo(781)
        assertThat(HomeEntry.hymnNo("附2", HYMN_YB)).isEqualTo(277)
    }

    @Test
    fun fuIsOnlyForDbAndYb() {
        assertThat(HomeEntry.hymnNo("附1", HYMN_BB)).isNull()
    }

    @Test
    fun bareFuOrGarbageIsNull() {
        assertThat(HomeEntry.hymnNo("附", HYMN_DB)).isNull()
        assertThat(HomeEntry.hymnNo("1x", HYMN_DB)).isNull()
    }
}
