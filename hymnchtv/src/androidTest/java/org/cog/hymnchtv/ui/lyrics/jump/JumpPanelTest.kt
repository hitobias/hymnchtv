package org.cog.hymnchtv.ui.lyrics.jump

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.button.MaterialButton
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.cog.hymnchtv.ContentHandler
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.QuickTest
import org.cog.hymnchtv.R
import org.cog.hymnchtv.hymn.HymnRef
import org.cog.hymnchtv.persistance.DatabaseBackend
import org.cog.hymnchtv.ui.FragmentHost
import org.cog.hymnchtv.ui.home.HomePrefs
import org.cog.hymnchtv.ui.picker.HymnLabels
import org.cog.hymnchtv.ui.search.SearchAdapter
import org.junit.Test
import org.junit.runner.RunWith

/** The jump panel: second overflow entry, picker in jump mode, slot row, recent jumps, search, and its back key. */
@RunWith(AndroidJUnit4::class)
class JumpPanelTest : JumpTestBase() {

    /** Only inside awaitTop/onTop conditions (main thread); never call onTop from there (runOnMainSync is not reentrant). */
    private fun panelOf(a: ContentHandler): JumpPanelFragment? =
        a.supportFragmentManager.findFragmentByTag(JumpPanelFragment.TAG) as JumpPanelFragment?

    private fun openPanel() {
        onTop { it.onLyricsAction(R.id.lyricsJump) }
        awaitTop("the jump panel") { panelOf(it)?.view != null }
    }

    private fun awaitPanelGone() = awaitTop("the panel to close") { panelOf(it) == null }

    private fun <T> inPanel(block: (View) -> T): T = onTop { a ->
        block((a.supportFragmentManager.findFragmentByTag(JumpPanelFragment.TAG) as JumpPanelFragment).requireView())
    }

    private fun tap(id: Int) = onView(withId(id)).inRoot(isDialog()).perform(click())

    private fun press(vararg ids: Int) = ids.forEach { onView(withId(it)).inRoot(isDialog()).perform(scrollTo(), click()) }

    @Test
    @QuickTest
    fun jumpIsTheSecondOverflowEntryAndOpensThePanelInJumpMode() {
        launch(MainActivity.HYMN_DB, 5).use { s ->
            s.revealChrome()
            s.onActivity { it.setChromeHeld(true) }
            onView(withId(R.id.btn_more)).perform(click())
            val menu = android.widget.PopupMenu(ctx, View(ctx)).apply { inflate(R.menu.menu_lyrics_more) }.menu
            assertThat(menu.getItem(0).itemId).isEqualTo(R.id.home)
            assertThat(menu.getItem(1).itemId).isEqualTo(R.id.lyricsJump)
            onView(withText(R.string.jump_menu)).inRoot(isPlatformPopup()).perform(click())
            awaitTop("the jump panel") { (it.supportFragmentManager.findFragmentByTag(JumpPanelFragment.TAG) as JumpPanelFragment?)?.view != null }
            inPanel { v ->
                assertThat(v.findViewById<View>(R.id.btn_set_next).visibility).isEqualTo(View.VISIBLE)
                assertThat(v.findViewById<View>(R.id.btn_toc).visibility).isEqualTo(View.GONE)
                assertThat(v.findViewById<View>(R.id.btn_recent_more).visibility).isEqualTo(View.GONE)
                assertThat(v.findViewById<MaterialButton>(R.id.bs_db).isChecked).isTrue()
                assertThat(v.findViewById<TextView>(R.id.tv_recent_label).text.toString()).isEqualTo(ctx.getString(R.string.jump_recent_label))
                assertThat(v.findViewById<View>(R.id.recent_empty).visibility).isEqualTo(View.VISIBLE)
            }
        }
    }

    @Test
    fun thePanelStartsFromTheBookOnScreenAndKeepsTheHomeTabsBook() {
        val prefs = ctx.getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE)
        prefs.edit().putString(HomePrefs.LAST_HYMN_TYPE, MainActivity.HYMN_XG).commit()
        launch(MainActivity.HYMN_BB, 37).use {
            openPanel()
            assertThat(inPanel { it.findViewById<MaterialButton>(R.id.bs_bb).isChecked }).isTrue()
            press(R.id.bs_db)
            assertThat(prefs.getString(HomePrefs.LAST_HYMN_TYPE, null)).isEqualTo(MainActivity.HYMN_XG)
        }
    }

    @Test
    @QuickTest
    fun typingANumberAndOpenJumpsThereAndRecordsHistory() {
        launch(MainActivity.HYMN_DB, 5).use {
            openPanel()
            press(R.id.n1, R.id.n0, R.id.n0, R.id.btn_open)
            awaitSettled(db100)
            awaitPanelGone()
            assertThat(topStack()).containsExactly(db5)
            FragmentHost.eventually(5_000) {
                val rec = DatabaseBackend.getInstance(ctx).historyRecords.first()
                assertThat(rec.hymnType).isEqualTo(MainActivity.HYMN_DB)
                assertThat(rec.hymnNo).isEqualTo(100)
            }
        }
    }

    @Test
    fun setNextFillsTheSlotAndStaysOnThisHymn() {
        launch(MainActivity.HYMN_DB, 5).use {
            openPanel()
            press(R.id.bs_xg, R.id.n1, R.id.n2, R.id.btn_set_next)
            awaitPanelGone()
            assertThat(onTop { it.jumpState.slot }).isEqualTo(xg12)
            assertThat(topRef()).isEqualTo(db5)
            assertThat(topStack()).isEmpty()
        }
    }

    @Test
    fun theSlotRowShowsAndClearsTheSlot() {
        launch(MainActivity.HYMN_DB, 5).use {
            onTop { it.onSetNext(xg12) }
            openPanel()
            assertThat(inPanel { it.findViewById<TextView>(R.id.jump_slot_text).text.toString() })
                .isEqualTo(ctx.getString(R.string.jump_next_slot, HymnLabels.chip(ctx, xg12)))
            tap(R.id.jump_slot_clear)
            assertThat(onTop { it.jumpState.slot }).isNull()
            assertThat(inPanel { it.findViewById<TextView>(R.id.jump_slot_text).text.toString() }).isEqualTo(ctx.getString(R.string.jump_slot_none))
            assertThat(inPanel { it.findViewById<View>(R.id.jump_slot_clear).visibility }).isEqualTo(View.GONE)
        }
    }

    @Test
    fun recentJumpsListTheStackNewestFirstAndATapReturns() {
        launch(MainActivity.HYMN_DB, 5).use {
            jump(db100)
            jump(bb37)
            openPanel()
            val labels = inPanel { v ->
                val rows = v.findViewById<LinearLayout>(R.id.recent_chips)
                (0 until rows.childCount).map { rows.getChildAt(it).findViewById<TextView>(R.id.tv_recent_label_item).text.toString() }
            }
            assertThat(labels).containsExactly(HymnLabels.chip(ctx, db100), HymnLabels.chip(ctx, db5)).inOrder()
            inPanel { it.findViewById<LinearLayout>(R.id.recent_chips).getChildAt(1).performClick() }
            awaitSettled(db5)
            awaitPanelGone()
            assertThat(topStack()).isEmpty()
        }
    }

    @Test
    fun searchInThePanelJumpsToTheTappedResult() {
        launch(MainActivity.HYMN_DB, 5).use {
            openPanel()
            press(R.id.tv_search)
            onView(withId(R.id.search_input)).inRoot(isDialog()).perform(replaceText("祂的計劃"))
            awaitTop("search results", 10_000) { a ->
                val p = a.supportFragmentManager.findFragmentByTag(JumpPanelFragment.TAG) as JumpPanelFragment?
                (p?.view?.findViewById<RecyclerView>(R.id.search_results)?.adapter?.itemCount ?: 0) > 0
            }
            val first = inPanel { (it.findViewById<RecyclerView>(R.id.search_results).adapter as SearchAdapter).currentList.first().ref }
            assertThat(first).isEqualTo(HymnRef(MainActivity.HYMN_DB, 1))
            inPanel { it.findViewById<RecyclerView>(R.id.search_results).findViewHolderForAdapterPosition(0)!!.itemView.performClick() }
            awaitSettled(first)
            awaitPanelGone()
            assertThat(topStack()).containsExactly(db5)
        }
    }

    @Test
    fun backClosesTheSearchFirstThenThePanel() {
        launch(MainActivity.HYMN_DB, 5).use {
            openPanel()
            press(R.id.tv_search)
            awaitTop("the search page") { panelOf(it)?.childFragmentManager?.backStackEntryCount == 1 }
            Espresso.closeSoftKeyboard()
            Espresso.pressBack()
            awaitTop("the search page to close, the panel still open") { panelOf(it)?.childFragmentManager?.backStackEntryCount == 0 }
            Espresso.pressBack()
            awaitPanelGone()
            assertThat(topRef()).isEqualTo(db5)
            assertThat(onTop { it.isFinishing }).isFalse()
        }
    }

    @Test
    fun theOpenPanelSurvivesARecreationOfTheLyricsPage() {
        launch(MainActivity.HYMN_DB, 5).use {
            jump(bb37)
            openPanel()
            val before = onTop { it }
            onTop { it.recreate() }
            awaitTop("the recreated page with its panel", 15_000) { it !== before && panelOf(it)?.view != null }
            assertThat(inPanel { it.findViewById<MaterialButton>(R.id.bs_bb).isChecked }).isTrue()
            assertThat(topStack()).containsExactly(db5)
            inPanel { it.findViewById<View>(R.id.jump_close).performClick() }
            awaitPanelGone()
            assertThat(topRef()).isEqualTo(bb37)
        }
    }

    @Test
    fun thePanelHasAWindowTitleAndAHeading() {
        launch(MainActivity.HYMN_DB, 5).use {
            openPanel()
            val title = onTop { a -> panelOf(a)!!.dialog!!.window!!.attributes.title?.toString() }
            assertThat(title).isEqualTo(ctx.getString(R.string.jump_title))
            assertThat(inPanel { androidx.core.view.ViewCompat.isAccessibilityHeading(it.findViewById(R.id.jump_title)) }).isTrue()
        }
    }


    @Test
    fun openIsTheFilledPrimaryButtonLikeTheHomePicker() {
        launch(MainActivity.HYMN_DB, 5).use {
            openPanel()
            press(R.id.n1)
            inPanel { v ->
                val open = v.findViewById<MaterialButton>(R.id.btn_open)
                assertThat(open.isEnabled).isTrue()
                assertThat(open.backgroundTintList).isNotNull()
                assertThat(open.backgroundTintList!!.defaultColor).isNotEqualTo(android.graphics.Color.TRANSPARENT)
                assertThat(open.width).isAtLeast(v.findViewById<View>(R.id.btn_set_next).width)
            }
        }
    }

    private companion object {
        val BOOK_IDS = listOf(R.id.bs_db, R.id.bs_bb, R.id.bs_xb, R.id.bs_xg, R.id.bs_yb, R.id.bs_er, R.id.bs_english)
    }

    @Test
    fun theKeypadAndOpenAreOnScreenWithoutScrollingAt320x640() {
        launch(MainActivity.HYMN_DB, 5).use {
            openPanel()
            // the fitter settles after the first layouts
            awaitTop("the panel fitted") { panelOf(it)?.view?.findViewById<View>(R.id.btn_open)?.height?.let { h -> h > 0 } == true }
            instrumentation.waitForIdleSync()
            android.os.SystemClock.sleep(500)
            inPanel { v ->
                val viewport = v.findViewById<View>(R.id.viewMain)
                val vp = android.graphics.Rect().also { r -> viewport.getGlobalVisibleRect(r) }
                assertThat(viewport.scrollY).isEqualTo(0)
                for ((what, id) in listOf("keypad" to R.id.keypadArea, "Open" to R.id.btn_open)) {
                    val view = v.findViewById<View>(id)
                    val loc = IntArray(2).also { l -> view.getLocationOnScreen(l) }
                    val vploc = IntArray(2).also { l -> viewport.getLocationOnScreen(l) }
                    assertWithMessage("$what top").that(loc[1]).isAtLeast(vploc[1])
                    assertWithMessage("$what bottom (viewport ${vploc[1]}..${vploc[1] + viewport.height})")
                        .that(loc[1] + view.height).isAtMost(vploc[1] + viewport.height)
                    assertWithMessage("$what fully visible").that(view.getGlobalVisibleRect(android.graphics.Rect())).isTrue()
                }
                assertWithMessage("Open does not cover the keypad").that(
                    IntArray(2).also { l -> v.findViewById<View>(R.id.btn_open).getLocationOnScreen(l) }[1],
                ).isAtLeast(IntArray(2).also { l -> v.findViewById<View>(R.id.keypadArea).getLocationOnScreen(l) }[1] + v.findViewById<View>(R.id.keypadArea).height)
                assertThat(vp.height()).isGreaterThan(0)
            }
        }
    }

    @Test
    fun theBookLabelsAreNotEllipsizedInThePanel() {
        launch(MainActivity.HYMN_DB, 5).use {
            openPanel()
            android.os.SystemClock.sleep(500)
            inPanel { v ->
                for (id in listOf(R.id.bs_db, R.id.bs_bb, R.id.bs_xb, R.id.bs_xg, R.id.bs_yb, R.id.bs_er, R.id.bs_english)) {
                    val b = v.findViewById<TextView>(id)
                    val layout = b.layout
                    assertWithMessage("layout of ${b.text}").that(layout).isNotNull()
                    assertWithMessage("'${b.text}' lines").that(layout.lineCount).isEqualTo(1)
                    assertWithMessage("'${b.text}' is cut off").that(layout.getEllipsisCount(0)).isEqualTo(0)
                }
            }
        }
    }
}
