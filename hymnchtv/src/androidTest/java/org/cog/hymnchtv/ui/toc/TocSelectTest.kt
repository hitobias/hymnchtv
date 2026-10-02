package org.cog.hymnchtv.ui.toc

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.chip.ChipGroup
import com.google.android.material.tabs.TabLayout
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.ui.toc.TocConstants
import org.cog.hymnchtv.R
import org.cog.hymnchtv.ui.FragmentHost
import org.cog.hymnchtv.ui.picker.PickerTestSupport
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The home tab's contents button opens the contents tab with the chosen book (or the English index) preselected. */
@RunWith(AndroidJUnit4::class)
class TocSelectTest {
    @Before fun setUp() = PickerTestSupport.prepare()

    @After fun tearDown() = PickerTestSupport.cleanUp()

    private fun selection(a: org.cog.hymnchtv.MainActivity): Pair<Int, Int> {
        val books = a.findViewById<ChipGroup>(R.id.toc_books)
        val pages = a.findViewById<TabLayout>(R.id.toc_pages)
        return books.checkedChipId to pages.selectedTabPosition
    }

    @Test fun chineseSourceOpensItsBookOnTheCategoryIndex() = PickerTestSupport.launch { scenario ->
        onView(withId(R.id.bs_bb)).perform(scrollTo(), click())
        onView(withId(R.id.btn_toc)).perform(scrollTo(), click())
        onView(withId(R.id.toc_books)).check(matches(isDisplayed()))
        FragmentHost.eventually {
            scenario.onActivity { a ->
                assertThat(selection(a)).isEqualTo(R.id.toc_book_bb to 0)
                assertThat(a.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_nav).selectedItemId).isEqualTo(R.id.nav_toc)
            }
        }
    }

    @Test fun englishSourceOpensTheEnglishToChineseIndexOfTheMainBook() = PickerTestSupport.launch { scenario ->
        onView(withId(R.id.bs_english)).perform(scrollTo(), click())
        onView(withId(R.id.btn_toc)).perform(scrollTo(), click())
        FragmentHost.eventually {
            scenario.onActivity { a -> assertThat(selection(a)).isEqualTo(R.id.toc_book_db to 3) }
        }
    }

    @Test fun contentsTabAlreadyCreatedIsRetargeted() = PickerTestSupport.launch { scenario ->
        onView(withId(R.id.nav_toc)).perform(click())
        onView(withId(R.id.nav_home)).perform(click())
        onView(withId(R.id.bs_xg)).perform(scrollTo(), click())
        onView(withId(R.id.btn_toc)).perform(scrollTo(), click())
        FragmentHost.eventually { scenario.onActivity { a -> assertThat(selection(a)).isEqualTo(R.id.toc_book_xg to 0) } }
    }

    @Test fun selectRejectsUnknownBooksAndPages() = PickerTestSupport.launch { scenario ->
        scenario.onActivity { a ->
            val toc = TocFragment()
            runCatching { toc.select("hymn_zz", TocConstants.TOC_CATEGORY) }.also { assertThat(it.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java) }
            runCatching { toc.select("hymn_db", "bogus") }.also { assertThat(it.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java) }
            assertThat(a).isNotNull()
        }
    }
}
