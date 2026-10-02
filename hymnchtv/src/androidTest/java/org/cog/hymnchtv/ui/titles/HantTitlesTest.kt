package org.cog.hymnchtv.ui.titles

import android.content.Context
import android.widget.ExpandableListView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.lyrics.LyricsLang
import org.cog.hymnchtv.lyrics.LyricsLanguagePolicy
import org.cog.hymnchtv.search.SearchResult
import org.cog.hymnchtv.ui.FragmentHost
import org.cog.hymnchtv.ui.picker.PickerTestSupport
import org.cog.hymnchtv.ui.picker.PickerTestSupport.ctx
import org.cog.hymnchtv.ui.search.SearchAdapter
import org.cog.hymnchtv.ui.toc.TocFragment
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Hymn titles and the table of contents follow the lyrics-language setting (Traditional: 詩/榮, Simplified: 诗/荣). */
@RunWith(AndroidJUnit4::class)
class HantTitlesTest {
    private val prefs get() = ctx.getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE)

    @Before fun setUp() = PickerTestSupport.prepare()

    @After fun tearDown() {
        prefs.edit().remove(LyricsLanguagePolicy.PREF_LYRICS_DEFAULT).commit()
        PickerTestSupport.cleanUp()
    }

    private fun lyricsLang(lang: LyricsLang) = prefs.edit().putString(LyricsLanguagePolicy.PREF_LYRICS_DEFAULT, lang.name).commit()

    private fun tocGroups(scenario: ActivityScenario<MainActivity>): List<String> {
        var result = emptyList<String>()
        scenario.onActivity { a ->
            val adapter = a.findViewById<ExpandableListView>(R.id.hymnToc).expandableListAdapter
            result = (0 until adapter.groupCount).map { adapter.getGroup(it) as String }
        }
        return result
    }

    private fun tocChild(scenario: ActivityScenario<MainActivity>, group: Int, child: Int): String {
        var result = ""
        scenario.onActivity { a ->
            result = a.findViewById<ExpandableListView>(R.id.hymnToc).expandableListAdapter.getChild(group, child) as String
        }
        return result
    }

    private fun showToc(lang: LyricsLang, block: (ActivityScenario<MainActivity>) -> Unit) {
        lyricsLang(lang)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { FragmentHost.show(it, TocFragment()) }
            block(scenario)
        }
    }

    @Test fun tocCategoriesAndItemsAreTraditional() = showToc(LyricsLang.TRADITIONAL) { scenario ->
        FragmentHost.eventually { assertThat(tocGroups(scenario).firstOrNull()).isEqualTo("頌三一神") }
        assertThat(tocGroups(scenario)).contains("聖靈豐滿")
        assertThat(tocChild(scenario, 0, 0)).isEqualTo("0001: 祂的計劃（英1）")
    }

    @Test fun tocCategoriesAndItemsAreSimplifiedWhenChosen() = showToc(LyricsLang.SIMPLIFIED) { scenario ->
        FragmentHost.eventually { assertThat(tocGroups(scenario).firstOrNull()).isEqualTo("颂三一神") }
        assertThat(tocChild(scenario, 0, 0)).isEqualTo("0001: 祂的计划（英1）")
    }

    @Test fun homePreviewShowsTheTraditionalTitle() = PickerTestSupport.launch {
        lyricsLang(LyricsLang.TRADITIONAL)
        PickerTestSupport.type("1")
        FragmentHost.eventually { assertThat(previewText(it)).isEqualTo("祂的計劃（英1）") }
    }

    @Test fun homePreviewShowsTheSimplifiedTitleWhenChosen() = PickerTestSupport.launch {
        lyricsLang(LyricsLang.SIMPLIFIED)
        PickerTestSupport.type("1")
        FragmentHost.eventually { assertThat(previewText(it)).isEqualTo("祂的计划（英1）") }
    }

    private fun previewText(scenario: ActivityScenario<MainActivity>): String {
        var text = ""
        scenario.onActivity { text = it.findViewById<TextView>(R.id.title_preview).text.toString() }
        return text
    }

    private fun searchTitles(scenario: ActivityScenario<MainActivity>, query: String): List<String> {
        onView(withId(R.id.tv_search)).perform(scrollTo(), click())
        onView(withId(R.id.search_input)).perform(replaceText(query))
        var list = emptyList<SearchResult>()
        FragmentHost.eventually(timeoutMs = 60_000) {
            scenario.onActivity { a ->
                list = (a.findViewById<RecyclerView>(R.id.search_results).adapter as SearchAdapter).currentList
            }
            assertThat(list).isNotEmpty()
        }
        return list.map { it.title }
    }

    @Test fun searchResultTitlesAreTraditional() {
        lyricsLang(LyricsLang.TRADITIONAL)
        PickerTestSupport.launch { scenario ->
            val titles = searchTitles(scenario, "祂的計劃")
            assertThat(titles).contains("祂的計劃（英1）")
            assertThat(titles.joinToString()).doesNotContain("计划")
        }
    }

    @Test fun searchResultTitlesAreSimplifiedWhenChosen() {
        lyricsLang(LyricsLang.SIMPLIFIED)
        PickerTestSupport.launch { scenario ->
            val titles = searchTitles(scenario, "祂的计划")
            assertThat(titles).contains("祂的计划（英1）")
        }
    }
}
