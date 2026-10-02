package org.cog.hymnchtv.ui.toc

import android.content.Context
import android.widget.ExpandableListView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.chip.Chip
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.ui.toc.TocConstants
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.ui.FragmentHost
import org.cog.hymnchtv.ui.home.HomePrefs
import org.cog.hymnchtv.utils.HymnNoValidate
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TocFragmentTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()
    private val prefs = ctx.getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE)

    @Before
    fun setUp() {
        FragmentHost.grantLaunchPermissions(ctx.packageName)
        prefs.edit().remove(HomePrefs.LAST_HYMN_TYPE).commit()
    }

    @After
    fun tearDown() {
        prefs.edit().remove(HomePrefs.LAST_HYMN_TYPE).commit()
    }

    private fun withToc(block: (ActivityScenario<MainActivity>) -> Unit) {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { FragmentHost.show(it, TocFragment()) }
            block(scenario)
        }
    }

    private fun groups(scenario: ActivityScenario<MainActivity>): List<String> {
        var result = emptyList<String>()
        scenario.onActivity { a ->
            val adapter = a.findViewById<ExpandableListView>(R.id.hymnToc).expandableListAdapter
            result = (0 until adapter.groupCount).map { adapter.getGroup(it) as String }
        }
        return result
    }

    @Test
    fun opensOnTheDaBenCategoriesAndSwitchesBook() = withToc { scenario ->
        FragmentHost.eventually { assertThat(groups(scenario).firstOrNull()).isEqualTo("颂三一神") }
        assertThat(groups(scenario)).hasSize(30)

        // Espresso's click does not toggle a Chip inside the scrolling chip row on this emulator; performClick is the real tap path
        scenario.onActivity { it.findViewById<Chip>(R.id.toc_book_bb).performClick() }
        FragmentHost.eventually { assertThat(groups(scenario).firstOrNull()).isEqualTo("赞美的话") }
    }

    @Test
    fun opensOnTheBookLastUsedOnHome() {
        prefs.edit().putString(HomePrefs.LAST_HYMN_TYPE, MainActivity.HYMN_ER).commit()
        withToc { scenario ->
            FragmentHost.eventually { assertThat(groups(scenario).firstOrNull()).isEqualTo("神的创造") }
        }
    }

    @Test
    fun strokeTabListsStrokeGroupsWithOpenableItems() = withToc { scenario ->
        onView(allOf(withText(R.string.hymn_stroke), isDescendantOfA(withId(R.id.toc_pages)))).perform(click())
        FragmentHost.eventually { assertThat(groups(scenario).firstOrNull()).startsWith("一画") }
        scenario.onActivity { a ->
            val adapter = a.findViewById<ExpandableListView>(R.id.hymnToc).expandableListAdapter
            assertThat(TocBuilder.hymnNoOf(adapter.getChild(0, 0) as String)).isNotNull()
        }
    }

    @Test
    fun englishTabOfABookWithoutOneExplainsWhy() {
        prefs.edit().putString(HomePrefs.LAST_HYMN_TYPE, MainActivity.HYMN_ER).commit()
        withToc {
            onView(allOf(withText(R.string.hymn_eng2ch), isDescendantOfA(withId(R.id.toc_pages)))).perform(click())
            onView(withId(R.id.toc_empty)).check(matches(allOf(isDisplayed(), withText(R.string.en2ch_hymn_same))))
        }
    }

    @Test
    fun appBuildMatchesTheUnitTestedBuildAndLimits() {
        // The unit tests pass the Bb/Er skip limits explicitly; check they are what the app really uses
        assertThat(HymnNoValidate.rangeBbLimit).isEqualTo(intArrayOf(38, 151, 259, 350, 471, 544, 630, 763, 881, 931, 1006))
        assertThat(HymnNoValidate.rangeErLimit).isEqualTo(intArrayOf(18, 125, 213, 324, 446, 525, 622, 720, 837, 921, 1040, 1119, 1233))
        val toc = TocBuilder.build(ctx, MainActivity.HYMN_BB, TocConstants.TOC_CATEGORY)
        assertThat(toc.keys).containsExactlyElementsIn(TocConstants.hymnCategoryBb.toList()).inOrder()
        assertThat(toc.values.sumOf { it.size }).isGreaterThan(400)
    }
}
