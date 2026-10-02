package org.cog.hymnchtv.ui.picker

import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.hymn.HymnSource
import org.cog.hymnchtv.hymnhistory.HistoryRecord
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.cog.hymnchtv.persistance.DatabaseBackend
import org.cog.hymnchtv.ui.FragmentHost
import org.cog.hymnchtv.ui.picker.PickerTestSupport.ctx
import org.cog.hymnchtv.ui.picker.PickerTestSupport.launch
import org.cog.hymnchtv.ui.picker.PickerTestSupport.type
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The home entry: book first, then the number, with a live preview. Valid BB numbers: 1-37, 101-150 (38-100 are gaps). */
@RunWith(AndroidJUnit4::class)
class HymnPickerTest {
    @Before fun setUp() = PickerTestSupport.prepare()

    @After fun tearDown() = PickerTestSupport.cleanUp()

    private fun pick(id: Int) = onView(withId(id)).perform(scrollTo(), click())

    private fun entryShows(text: String) = onView(withId(R.id.tv_entry)).check(matches(withText(containsString(text))))

    private fun noteShows(text: String) = onView(withId(R.id.tv_preview_note)).check(matches(withText(containsString(text))))

    private fun view(scenario: androidx.test.core.app.ActivityScenario<MainActivity>, id: Int): View {
        var v: View? = null
        scenario.onActivity { v = it.findViewById(id) }
        return v!!
    }

    @Test fun sourceButtonsAreSingleSelectAcrossBothRowsAndExposeChecked() = launch { scenario ->
        pick(R.id.bs_db)
        pick(R.id.bs_yb)
        scenario.onActivity { a ->
            assertThat(a.findViewById<com.google.android.material.button.MaterialButton>(R.id.bs_db).isChecked).isFalse()
            assertThat(a.findViewById<com.google.android.material.button.MaterialButton>(R.id.bs_yb).isChecked).isTrue()
            assertThat(HymnSource.entries.count { s ->
                a.findViewById<com.google.android.material.button.MaterialButton>(sourceViewId(s)).isChecked
            }).isEqualTo(1)
        }
        // tapping the chosen one again keeps exactly one source chosen
        pick(R.id.bs_yb)
        scenario.onActivity { a ->
            assertThat(a.findViewById<com.google.android.material.button.MaterialButton>(R.id.bs_yb).isChecked).isTrue()
        }
    }

    private fun sourceViewId(s: HymnSource) = when (s) {
        HymnSource.DB -> R.id.bs_db
        HymnSource.BB -> R.id.bs_bb
        HymnSource.XB -> R.id.bs_xb
        HymnSource.XG -> R.id.bs_xg
        HymnSource.YB -> R.id.bs_yb
        HymnSource.ER -> R.id.bs_er
        HymnSource.ENGLISH -> R.id.bs_english
    }

    @Test fun theChosenSourceIsRememberedAcrossRestarts() {
        launch { pick(R.id.bs_english) }
        launch { scenario ->
            scenario.onActivity { a ->
                assertThat(a.findViewById<com.google.android.material.button.MaterialButton>(R.id.bs_english).isChecked).isTrue()
            }
        }
    }

    @Test fun keysThatLeadNowhereAreDisabledAndSayWhy() = launch { scenario ->
        pick(R.id.bs_bb)
        type("3")
        onView(withId(R.id.n9)).check(matches(not(isEnabled())))
        val key = view(scenario, R.id.n9)
        assertThat(ViewCompat.getStateDescription(key)).isEqualTo(ctx.getString(R.string.c_key_unavailable))
        onView(withId(R.id.n7)).check(matches(isEnabled()))
    }

    @Test fun anInvalidNumberOffersTheOtherBooksWhereItExists() = launch { scenario ->
        pick(R.id.bs_bb)
        type("40")
        // the number stays big on the card, the message goes to its right side
        entryShows("40")
        onView(withId(R.id.title_preview)).check(matches(withText(ctx.getString(R.string.c_preview_invalid, HymnLabels.longName(ctx, HymnSource.BB), "40"))))
        onView(withId(R.id.btn_open)).check(matches(not(isEnabled())))
        val dbChip = androidx.test.espresso.matcher.ViewMatchers.withText(HymnLabels.sourceName(ctx, HymnSource.DB))
        onView(org.hamcrest.CoreMatchers.allOf(dbChip, androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA(withId(R.id.also_in_group)))).perform(click())
        scenario.onActivity { a ->
            assertThat(a.findViewById<com.google.android.material.button.MaterialButton>(R.id.bs_db).isChecked).isTrue()
        }
        entryShows("40")
        onView(withId(R.id.btn_open)).check(matches(isEnabled()))
    }

    @Test fun fuModeUsesTheAppendixAndLeavingForABookWithoutOneSaysSo() = launch {
        pick(R.id.bs_db)
        pick(R.id.n10)
        type("3")
        entryShows(ctx.getString(R.string.c_label_fu, 3))
        pick(R.id.bs_bb)
        FragmentHost.eventually { onView(withId(R.id.title_preview)).check(matches(withText(R.string.c_notice_no_fu))) }
        entryShows("3")
    }

    @Test fun englishNumberPreviewsItsChineseHymnAndOffersTheOtherCandidate() = launch {
        pick(R.id.bs_english)
        type("1")
        entryShows("1")
        noteShows(HymnLabels.headline(ctx, org.cog.hymnchtv.hymn.HymnRef(HymnTypes.DB, 1)))
        onView(withId(R.id.btn_open)).check(matches(isEnabled()))
        // 254 has two Chinese counterparts: BB 401 first, DB 211 offered as a chip
        pick(R.id.n11)
        type("254")
        val other = HymnLabels.headline(ctx, org.cog.hymnchtv.hymn.HymnRef(HymnTypes.DB, 211))
        onView(org.hamcrest.CoreMatchers.allOf(withText(other), androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA(withId(R.id.also_in_group)))).perform(click())
        noteShows(HymnLabels.headline(ctx, org.cog.hymnchtv.hymn.HymnRef(HymnTypes.DB, 211)))
    }

    @Test fun englishNumberWithoutCounterpartCannotBeOpened() = launch {
        pick(R.id.bs_english)
        type("3")   // 3 is only a prefix of 30..39; no English hymn 3 has a Chinese counterpart
        onView(withId(R.id.title_preview)).check(matches(withText(R.string.c_preview_en_none)))
        onView(withId(R.id.btn_open)).check(matches(not(isEnabled())))
    }

    @Test fun openingWritesHistoryAndShowsLyrics() = launch {
        PickerTestSupport.resetHistory()
        pick(R.id.bs_bb)
        type("37")
        pick(R.id.btn_open)
        FragmentHost.eventually {
            val rec = DatabaseBackend.getInstance(ctx).historyRecords.firstOrNull()
            assertThat(rec?.hymnType).isEqualTo(HymnTypes.BB)
            assertThat(rec?.hymnNo).isEqualTo(37)
        }
        onView(withId(R.id.viewPager)).check(matches(androidx.test.espresso.matcher.ViewMatchers.isDisplayed()))
    }

    @Test fun youthAppendixIsShownAsFuInHistoryAndReopensTheSameHymn() = launch {
        PickerTestSupport.resetHistory()
        pick(R.id.bs_yb)
        pick(R.id.n10)
        type("1")
        pick(R.id.btn_open)
        FragmentHost.eventually {
            val rec = DatabaseBackend.getInstance(ctx).historyRecords.first()
            assertThat(rec.hymnType).isEqualTo(HymnTypes.YB)
            assertThat(rec.hymnNo).isEqualTo(276)
            // the media layer's isFu stays 0 for the youth book; the label is derived from the number
            assertThat(rec.isFu).isFalse()
            assertThat(rec.toString()).contains("附1")
        }
    }

    @Test fun contentsButtonIsInTheChromeAndAddPlaylistIsHiddenWhileTheNotebookUiIsOff() = launch {
        onView(withId(R.id.btn_toc)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        onView(withId(R.id.btn_add_playlist)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.btn_set_next)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test fun enteredNumberAndSourceSurviveRotation() = launch { scenario ->
        pick(R.id.bs_bb)
        type("37")
        scenario.recreate()
        entryShows("37")
        scenario.onActivity { a ->
            assertThat(a.findViewById<com.google.android.material.button.MaterialButton>(R.id.bs_bb).isChecked).isTrue()
        }
    }

    @Test fun previewIsAPoliteLiveRegionAndSourcesExposeCheckedState() = launch { scenario ->
        scenario.onActivity { a ->
            assertThat(ViewCompat.getAccessibilityLiveRegion(a.findViewById(R.id.previewArea))).isEqualTo(ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE)
            assertThat(a.findViewById<com.google.android.material.button.MaterialButton>(R.id.bs_db).isCheckable).isTrue()
            // reading order: search, sources, preview, keys, open, recent
            val order = listOf(R.id.tv_search, R.id.bs_db, R.id.previewArea, R.id.keypadArea, R.id.btn_open, R.id.recentArea)
            val positions = order.map { id -> orderIndex(a.findViewById(R.id.picker_root), a.findViewById(id)) }
            assertThat(positions).isInOrder()
        }
    }

    /** Depth-first index of [target] in the view tree below [root] (the order a screen reader walks it). */
    private fun orderIndex(root: View, target: View): Int {
        var counter = 0
        var found = -1
        fun walk(v: View) {
            if (v === target) found = counter
            counter++
            if (v is ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
        }
        walk(root)
        return found
    }

    @Test fun deleteKeyRemovesOneDigitAtATime() = launch {
        pick(R.id.bs_db)
        type("12")
        pick(R.id.n11)
        entryShows("1")
    }

    @Test fun historyOfAnOldFuRecordStillFormatsAsFu() {
        val rec = HistoryRecord(HymnTypes.DB, 783, true, "標題", 1L)
        assertThat(rec.toString()).contains("附3")
    }
}
