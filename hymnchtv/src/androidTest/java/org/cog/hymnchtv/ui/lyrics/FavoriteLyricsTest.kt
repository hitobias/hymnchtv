package org.cog.hymnchtv.ui.lyrics

import android.view.View
import android.widget.ImageView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.viewpager2.widget.ViewPager2
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.ContentHandler
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.notebook.FavoriteTestSupport
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Favourites on the lyrics page: menu item, star in the info row, paging, relaunch. */
@RunWith(AndroidJUnit4::class)
class FavoriteLyricsTest : LyricsTestBase() {
    // DB 5 has a key line; XB 1 has none (see LyricsTopBarTest), so its info row can only hold the star
    private val db5 = HymnKey.of(HymnTypes.DB, 5)

    @Before
    fun resetFavorites() {
        FavoriteTestSupport.reset()
    }

    private fun starVisible(s: ActivityScenario<ContentHandler>) =
        s.read { page(it)!!.findViewById<ImageView>(R.id.favorite_star).visibility == View.VISIBLE }

    private fun chooseFavoriteInMenu(s: ActivityScenario<ContentHandler>, expectedTitle: Int) {
        s.revealChrome()
        onView(withId(R.id.btn_more)).perform(click())
        onView(withText(expectedTitle)).inRoot(isPlatformPopup()).perform(click())
    }

    @Test
    fun favouriteStarAppearsAndMenuTextFlips() {
        launch(MainActivity.HYMN_DB, 5).use { s ->
            assertThat(starVisible(s)).isFalse()
            chooseFavoriteInMenu(s, R.string.fav_add)
            s.await("star shown") { page(it)!!.findViewById<View>(R.id.favorite_star).visibility == View.VISIBLE }
            assertThat(FavoriteTestSupport.isFavorite(db5)).isTrue()
            chooseFavoriteInMenu(s, R.string.fav_remove)
            s.await("star hidden") { page(it)!!.findViewById<View>(R.id.favorite_star).visibility == View.GONE }
            assertThat(FavoriteTestSupport.isFavorite(db5)).isFalse()
            // the menu is back to "add"
            s.revealChrome()
            onView(withId(R.id.btn_more)).perform(click())
            onView(withText(R.string.fav_add)).inRoot(isPlatformPopup()).perform(click())
            s.await("star shown again") { page(it)!!.findViewById<View>(R.id.favorite_star).visibility == View.VISIBLE }
        }
    }

    @Test
    fun pagingToAnotherHymnHidesTheStarAndBackShowsIt() {
        FavoriteTestSupport.add(db5)
        launch(MainActivity.HYMN_DB, 5).use { s ->
            s.await("star shown") { page(it)!!.findViewById<View>(R.id.favorite_star).visibility == View.VISIBLE }
            val start = s.item()
            s.onActivity { it.findViewById<ViewPager2>(R.id.viewPager).setCurrentItem(start + 1, false) }
            s.await("on the next hymn") { it.findViewById<ViewPager2>(R.id.viewPager).currentItem == start + 1 }
            s.awaitPage()
            s.await("star hidden on hymn 6") { page(it)!!.findViewById<View>(R.id.favorite_star).visibility == View.GONE }
            s.onActivity { it.findViewById<ViewPager2>(R.id.viewPager).setCurrentItem(start, false) }
            s.await("back on hymn 5") { it.findViewById<ViewPager2>(R.id.viewPager).currentItem == start }
            s.awaitPage()
            s.await("star shown again") { page(it)!!.findViewById<View>(R.id.favorite_star).visibility == View.VISIBLE }
        }
    }

    @Test
    fun hymnWithoutKeyShowsOnlyTheStar() {
        FavoriteTestSupport.add(HymnKey.of(HymnTypes.XB, 1))
        launch(MainActivity.HYMN_XB, 1).use { s ->
            s.await("info row shown") { page(it)!!.findViewById<View>(R.id.lyrics_info_row).visibility == View.VISIBLE }
            assertThat(s.pageView(R.id.meter_key).visibility).isEqualTo(View.GONE)
            assertThat(s.pageView(R.id.favorite_star).visibility).isEqualTo(View.VISIBLE)
            chooseFavoriteInMenu(s, R.string.fav_remove)
            s.await("info row hidden") { page(it)!!.findViewById<View>(R.id.lyrics_info_row).visibility == View.GONE }
        }
    }

    @Test
    fun twoCompleteTogglesEachTakeEffect() {
        launch(MainActivity.HYMN_DB, 5).use { s ->
            chooseFavoriteInMenu(s, R.string.fav_add)
            s.await("marked") { page(it)!!.findViewById<View>(R.id.favorite_star).visibility == View.VISIBLE }
            chooseFavoriteInMenu(s, R.string.fav_remove)
            s.await("unmarked") { page(it)!!.findViewById<View>(R.id.favorite_star).visibility == View.GONE }
            assertThat(FavoriteTestSupport.active()).isEmpty()
        }
    }

    @Test
    fun favouriteSurvivesActivityRelaunch() {
        launch(MainActivity.HYMN_DB, 5).use { s ->
            chooseFavoriteInMenu(s, R.string.fav_add)
            s.await("marked") { page(it)!!.findViewById<View>(R.id.favorite_star).visibility == View.VISIBLE }
        }
        launch(MainActivity.HYMN_DB, 5).use { s ->
            s.await("star after relaunch") { page(it)!!.findViewById<View>(R.id.favorite_star).visibility == View.VISIBLE }
        }
    }

    @Test
    fun starUsesAccentColor() {
        FavoriteTestSupport.add(db5)
        launch(MainActivity.HYMN_DB, 5).use { s ->
            s.await("star shown") { page(it)!!.findViewById<View>(R.id.favorite_star).visibility == View.VISIBLE }
            val accent = s.read { it.lyricsPalette.accentColor }
            val tint = s.read { page(it)!!.findViewById<ImageView>(R.id.favorite_star).imageTintList!!.defaultColor }
            assertThat(tint).isEqualTo(accent)
        }
    }
}
