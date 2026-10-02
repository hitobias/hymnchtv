package org.cog.hymnchtv.ui.search

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.hymn.HymnRef
import org.cog.hymnchtv.hymn.HymnSource
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.cog.hymnchtv.persistance.DatabaseBackend
import org.cog.hymnchtv.search.SearchResult
import org.cog.hymnchtv.ui.FragmentHost
import org.cog.hymnchtv.ui.picker.HymnLabels
import org.cog.hymnchtv.ui.picker.PickerTestSupport
import org.cog.hymnchtv.ui.picker.PickerTestSupport.ctx
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The full-screen search page opened from the home tab's search field. */
@RunWith(AndroidJUnit4::class)
class SearchFragmentTest {
    @Before fun setUp() = PickerTestSupport.prepare()

    @After fun tearDown() = PickerTestSupport.cleanUp()

    private fun openSearch() = onView(withId(R.id.tv_search)).perform(scrollTo(), click())

    private fun search(text: String) = onView(withId(R.id.search_input)).perform(replaceText(text))

    private fun results(a: MainActivity): List<SearchResult> {
        val adapter = a.findViewById<RecyclerView>(R.id.search_results).adapter as SearchAdapter
        return adapter.currentList
    }

    private fun waitForResults(scenario: ActivityScenario<MainActivity>, atLeast: Int = 1): List<SearchResult> {
        var list = emptyList<SearchResult>()
        FragmentHost.eventually(timeoutMs = 60_000) {
            scenario.onActivity { list = results(it) }
            assertThat(list.size).isAtLeast(atLeast)
        }
        return list
    }

    private fun status(a: MainActivity) = a.findViewById<TextView>(R.id.tv_search_status).text.toString()

    @Test fun traditionalQueryFindsTheMainBookHymnFirstAndCountsResults() = PickerTestSupport.launch { scenario ->
        openSearch()
        search("祂的計劃")
        val list = waitForResults(scenario)
        assertThat(list.first().ref).isEqualTo(HymnRef(HymnTypes.DB, 1))
        FragmentHost.eventually {
            scenario.onActivity { a -> assertThat(status(a)).isEqualTo(a.getString(R.string.c_search_status_count, results(a).size)) }
        }
    }

    @Test fun tappingAYouthAppendixResultOpensItsLyrics() = PickerTestSupport.launch { scenario ->
        PickerTestSupport.resetHistory()
        onView(withId(R.id.bs_yb)).perform(scrollTo(), click())
        openSearch()
        search("爱的乐章一婚礼颂")
        val list = waitForResults(scenario)
        val target = HymnRef(HymnTypes.YB, 276)
        assertThat(list.map { it.ref }).contains(target)
        onView(withText(HymnLabels.headline(ctx, target))).perform(click())
        FragmentHost.eventually {
            val rec = DatabaseBackend.getInstance(ctx).historyRecords.first()
            assertThat(rec.hymnType).isEqualTo(HymnTypes.YB)
            assertThat(rec.hymnNo).isEqualTo(276)
        }
        onView(withId(R.id.viewPager)).check(matches(isDisplayed()))
    }

    @Test fun currentSourceScopeLimitsToThatBookAndAllBooksWidens() = PickerTestSupport.launch { scenario ->
        onView(withId(R.id.bs_bb)).perform(scrollTo(), click())
        openSearch()
        onView(withId(R.id.scope_current)).check(matches(withText(ctx.getString(R.string.c_scope_current, HymnLabels.longName(ctx, HymnSource.BB)))))
        search("主")
        val bbOnly = waitForResults(scenario)
        assertThat(bbOnly.map { it.ref.book }.distinct()).containsExactly(HymnTypes.BB)
        onView(withId(R.id.scope_all)).perform(click())
        FragmentHost.eventually(timeoutMs = 60_000) {
            scenario.onActivity { a -> assertThat(results(a).map { it.ref.book }).contains(HymnTypes.DB) }
        }
    }

    @Test fun englishSourceHasNoCurrentSourceScope() = PickerTestSupport.launch { scenario ->
        onView(withId(R.id.bs_english)).perform(scrollTo(), click())
        openSearch()
        scenario.onActivity { a -> assertThat(a.findViewById<View>(R.id.scope_current).visibility).isEqualTo(View.GONE) }
    }

    @Test fun aVeryCommonCharacterIsCappedAndSaysSo() = PickerTestSupport.launch { scenario ->
        openSearch()
        search("的")
        val list = waitForResults(scenario, atLeast = 200)
        assertThat(list).hasSize(200)
        FragmentHost.eventually { scenario.onActivity { a -> assertThat(status(a)).isEqualTo(a.getString(R.string.c_search_status_more)) } }
    }

    @Test fun noMatchSaysSo() = PickerTestSupport.launch { scenario ->
        openSearch()
        search("zzzqqq不存在的词xyz")
        FragmentHost.eventually(timeoutMs = 60_000) {
            scenario.onActivity { a -> assertThat(status(a)).isEqualTo(a.getString(R.string.c_search_status_none)) }
        }
    }

    @Test fun emptyQueryShowsTheHint() = PickerTestSupport.launch {
        openSearch()
        onView(withId(R.id.tv_search_status)).check(matches(withText(R.string.c_search_empty_hint)))
    }

    @Test fun resultSurvivesRotation() = PickerTestSupport.launch { scenario ->
        openSearch()
        search("祂的計劃")
        waitForResults(scenario)
        scenario.recreate()
        val list = waitForResults(scenario)
        assertThat(list.first().ref).isEqualTo(HymnRef(HymnTypes.DB, 1))
    }

    @Test fun backClosesTheSearchPageAndShowsTheTitleOfThePage() = PickerTestSupport.launch { scenario ->
        openSearch()
        scenario.onActivity { a -> assertThat(a.supportActionBar?.title?.toString()).isEqualTo(a.getString(R.string.c_search_title)) }
        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        onView(withId(R.id.search_input)).check(doesNotExist())
        onView(withId(R.id.tv_entry)).check(matches(isDisplayed()))
    }

    @Test fun leavingWhileASearchIsRunningDoesNotCrash() = PickerTestSupport.launch { scenario ->
        openSearch()
        search("不存在的字串qqq")
        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        Thread.sleep(1500)
        scenario.onActivity { a -> assertThat(a.isFinishing).isFalse() }
        onView(withId(R.id.tv_entry)).check(matches(isDisplayed()))
    }
}
