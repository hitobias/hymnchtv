package org.cog.hymnchtv.ui.home

import android.content.Context
import android.os.SystemClock
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.ContentHandler
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.hymn.HymnRef
import org.cog.hymnchtv.lyrics.LyricsLang
import org.cog.hymnchtv.lyrics.LyricsLanguagePolicy
import org.cog.hymnchtv.lyrics.LyricsScript
import org.cog.hymnchtv.notebook.FavoriteTestSupport
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.cog.hymnchtv.ui.FragmentHost
import org.cog.hymnchtv.ui.picker.HymnLabels
import org.cog.hymnchtv.ui.picker.PickerTestSupport
import org.cog.hymnchtv.ui.picker.PickerTestSupport.ctx
import org.cog.hymnchtv.ui.titles.AssetHymnTitles
import org.hamcrest.CoreMatchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The Favourites tab of the "History & favourites" page. */
@RunWith(AndroidJUnit4::class)
class FavoritesTabTest {
    private val prefs get() = ctx.getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE)
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Before fun setUp() {
        PickerTestSupport.prepare()
        PickerTestSupport.resetHistory()
        FavoriteTestSupport.reset()
        prefs.edit().remove(HomePrefs.HISTORY_TAB).commit()
    }

    @After fun tearDown() {
        FavoriteTestSupport.reset()
        prefs.edit().remove(HomePrefs.HISTORY_TAB).remove(LyricsLanguagePolicy.PREF_LYRICS_DEFAULT).commit()
        PickerTestSupport.cleanUp()
    }

    private fun openHistoryPage() {
        FragmentHost.eventually { onView(withId(R.id.btn_recent_more)).check(matches(withEffectiveVisibility(Visibility.VISIBLE))) }
        onView(withId(R.id.btn_recent_more)).perform(scrollTo(), click())
        onView(withId(R.id.history_tabs)).check(matches(isDisplayed()))
    }

    /** The label of a tab on the page (the home page below has its own "Recent" heading). */
    private fun tab(label: Int) = allOf(withText(label), isDescendantOfA(withId(R.id.history_tabs)))

    private fun openFavouritesTab() {
        openHistoryPage()
        onView(tab(R.string.fav_tab_favorites)).perform(click())
        onView(withId(R.id.favorites_list)).check(matches(isDisplayed()))
    }

    private fun favouriteRows(): List<FavoriteRow> {
        var rows = emptyList<FavoriteRow>()
        instrumentation.runOnMainSync {
            val activity = resumed(MainActivity::class.java) ?: return@runOnMainSync
            val list = activity.findViewById<RecyclerView>(R.id.favorites_list) ?: return@runOnMainSync
            rows = (list.adapter as FavoriteAdapter).currentList
        }
        return rows
    }

    private fun <T : android.app.Activity> resumed(type: Class<T>): T? =
        ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).filterIsInstance(type).firstOrNull()

    /** The lyrics page the app opened: (book, stored number); the page is closed again. */
    private fun openedHymn(): Pair<String, Int> {
        var opened: Pair<String, Int>? = null
        FragmentHost.eventually(6000) {
            instrumentation.runOnMainSync {
                opened = resumed(ContentHandler::class.java)?.let {
                    it.intent.getStringExtra(MainActivity.ATTR_HYMN_TYPE).orEmpty() to it.intent.getIntExtra(MainActivity.ATTR_HYMN_NUMBER, -1)
                }
            }
            assertThat(opened).isNotNull()
        }
        instrumentation.runOnMainSync { resumed(ContentHandler::class.java)?.finish() }
        return opened!!
    }

    private fun headline(book: String, stored: Int) = HymnLabels.headline(ctx, HymnRef(book, stored))

    @Test fun favouritesTabListsFavouritesNewestFirstAndTapOpensIt() {
        FavoriteTestSupport.add(HymnKey.of(HymnTypes.DB, 5))
        SystemClock.sleep(30)
        FavoriteTestSupport.add(HymnKey.of(HymnTypes.DB, 6))
        PickerTestSupport.launch {
            openFavouritesTab()
            FragmentHost.eventually { assertThat(favouriteRows().map { it.ref.storedNo }).containsExactly(6, 5).inOrder() }
            onView(allOf(withText(headline(HymnTypes.DB, 6)), isDisplayed())).perform(click())
            assertThat(openedHymn()).isEqualTo(HymnTypes.DB to 6)
        }
    }

    @Test fun youthSupplementOpensTheRightHymn() {
        FavoriteTestSupport.add(HymnKey.of(HymnTypes.YB, 276))
        PickerTestSupport.launch {
            openFavouritesTab()
            val expected = headline(HymnTypes.YB, 276)
            assertThat(expected).contains(ctx.getString(R.string.c_label_fu, 1))
            FragmentHost.eventually { onView(allOf(withText(expected), isDisplayed())).check(matches(isDisplayed())) }
            onView(allOf(withText(expected), isDisplayed())).perform(click())
            assertThat(openedHymn()).isEqualTo(HymnTypes.YB to 276)
        }
    }

    private fun starOf(book: String, stored: Int) =
        allOf(withId(R.id.b_unfavorite), hasSibling(hasDescendant(withText(headline(book, stored)))))

    @Test fun unfavouriteRemovesRowAndUndoBringsItBack() {
        FavoriteTestSupport.add(HymnKey.of(HymnTypes.DB, 5))
        SystemClock.sleep(30)
        FavoriteTestSupport.add(HymnKey.of(HymnTypes.DB, 6))
        PickerTestSupport.launch {
            openFavouritesTab()
            FragmentHost.eventually { assertThat(favouriteRows().map { it.ref.storedNo }).containsExactly(6, 5).inOrder() }
            onView(starOf(HymnTypes.DB, 5)).perform(click())
            FragmentHost.eventually { assertThat(favouriteRows().map { it.ref.storedNo }).containsExactly(6) }
            onView(withText(R.string.fav_removed)).check(matches(isDisplayed()))
            assertThat(FavoriteTestSupport.isFavorite(HymnKey.of(HymnTypes.DB, 5))).isFalse()

            onView(withText(R.string.fav_undo)).perform(click())
            FragmentHost.eventually { assertThat(favouriteRows().map { it.ref.storedNo }).containsExactly(5, 6).inOrder() }
            assertThat(FavoriteTestSupport.isFavorite(HymnKey.of(HymnTypes.DB, 5))).isTrue()
        }
    }

    @Test fun emptyStateShowsFavEmptyText() {
        PickerTestSupport.launch {
            openFavouritesTab()
            FragmentHost.eventually {
                onView(withId(R.id.tv_favorites_empty)).check(matches(allOf(isDisplayed(), withText(R.string.fav_empty))))
            }
        }
    }

    @Test fun removingTheLastFavouriteShowsTheEmptyState() {
        FavoriteTestSupport.add(HymnKey.of(HymnTypes.DB, 5))
        PickerTestSupport.launch {
            openFavouritesTab()
            FragmentHost.eventually { onView(starOf(HymnTypes.DB, 5)).check(matches(isDisplayed())) }
            onView(starOf(HymnTypes.DB, 5)).perform(click())
            FragmentHost.eventually { onView(withId(R.id.tv_favorites_empty)).check(matches(isDisplayed())) }
        }
    }

    private fun titleOfFirstRow(): String? = favouriteRows().firstOrNull()?.title

    @Test fun titlesFollowTheLyricsScript() {
        FavoriteTestSupport.add(HymnKey.of(HymnTypes.DB, 1))
        val simplified = AssetHymnTitles.from(ctx, null).lookup(HymnTypes.DB, 1)
        prefs.edit().putString(LyricsLanguagePolicy.PREF_LYRICS_DEFAULT, LyricsLang.TRADITIONAL.name).commit()
        val variant = LyricsScript.hantVariant(ctx)
        assertThat(variant).isNotNull()
        val traditional = AssetHymnTitles.from(ctx, variant).lookup(HymnTypes.DB, 1)
        assertThat(traditional).isNotEqualTo(simplified)

        PickerTestSupport.launch {
            openFavouritesTab()
            FragmentHost.eventually { assertThat(titleOfFirstRow()).isEqualTo(traditional) }
            onView(allOf(withId(R.id.tv_fav_title), withText(traditional))).check(matches(isDisplayed()))
        }
        prefs.edit().putString(LyricsLanguagePolicy.PREF_LYRICS_DEFAULT, LyricsLang.SIMPLIFIED.name).commit()
        PickerTestSupport.launch {
            openFavouritesTab()
            FragmentHost.eventually { assertThat(titleOfFirstRow()).isEqualTo(simplified) }
        }
    }

    @Test fun tabChoiceIsRemembered() {
        PickerTestSupport.launch {
            openHistoryPage()
            onView(withId(R.id.history_list)).check(matches(isDisplayed()))
            onView(tab(R.string.fav_tab_favorites)).perform(click())
            pressBack()
            openHistoryPage()
            onView(withId(R.id.favorites_list)).check(matches(isDisplayed()))
            assertThat(prefs.getInt(HomePrefs.HISTORY_TAB, -1)).isEqualTo(HistoryTab.FAVORITES.pref)
            onView(tab(R.string.fav_tab_recent)).perform(click())
            onView(withId(R.id.history_list)).check(matches(isDisplayed()))
        }
    }

    @Test fun leavingThePageQuicklyDoesNotCrash() {
        FavoriteTestSupport.add(HymnKey.of(HymnTypes.DB, 5))
        PickerTestSupport.launch {
            repeat(10) {
                openFavouritesTab()
                pressBack()
            }
            FragmentHost.eventually { onView(withId(R.id.btn_recent_more)).check(matches(withEffectiveVisibility(Visibility.VISIBLE))) }
        }
    }
}
