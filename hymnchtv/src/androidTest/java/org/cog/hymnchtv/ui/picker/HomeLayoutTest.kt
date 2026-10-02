package org.cog.hymnchtv.ui.picker

import android.graphics.Rect
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.cog.hymnchtv.R
import org.cog.hymnchtv.hymnhistory.HistoryRecord
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.cog.hymnchtv.ui.FragmentHost
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeLayoutTest {
    @Before fun setUp() = PickerTestSupport.prepare()

    @After fun tearDown() = PickerTestSupport.cleanUp()

    private val fixedIds = listOf(
        R.id.tv_search, R.id.bs_db, R.id.bs_bb, R.id.bs_xb, R.id.bs_xg, R.id.bs_yb, R.id.bs_er, R.id.bs_english, R.id.btn_toc,
        R.id.n0, R.id.n1, R.id.n2, R.id.n3, R.id.n4, R.id.n5, R.id.n6, R.id.n7, R.id.n8, R.id.n9, R.id.n10, R.id.n11, R.id.btn_open,
    )

    @Test fun everythingFromSearchToOpenIsVisibleWithoutScrolling() {
        PickerTestSupport.launch { scenario ->
            scenario.onActivity { a ->
                val dm = a.resources.displayMetrics
                val heightDp = dm.heightPixels / dm.density
                val portrait = a.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
                val fontScale = a.resources.configuration.fontScale
                assumeTrue("needs a 720dp tall portrait screen at normal font size", portrait && heightDp >= 720 && fontScale <= 1.0f)
                for (id in listOf(R.id.tv_search, R.id.bs_db, R.id.tv_entry, R.id.n1, R.id.n11, R.id.btn_open)) {
                    val v = a.findViewById<View>(id)
                    val r = Rect()
                    val visible = v.getGlobalVisibleRect(r)
                    assertWithMessage("view ${a.resources.getResourceEntryName(id)} visible").that(visible).isTrue()
                    assertWithMessage("view ${a.resources.getResourceEntryName(id)} fully visible").that(r.height()).isEqualTo(v.height)
                }
            }
        }
    }

    @Test fun everyTouchTargetIsAtLeast48dp() {
        PickerTestSupport.resetHistory(HistoryRecord(HymnTypes.DB, 1, false, "標題", 1L))
        PickerTestSupport.launch { scenario ->
            FragmentHost.eventually {
                scenario.onActivity { a -> assertThat(a.findViewById<android.widget.LinearLayout>(R.id.recent_chips).childCount).isEqualTo(1) }
            }
            scenario.onActivity { a ->
                val min = 48 * a.resources.displayMetrics.density - 1
                val chips = a.findViewById<android.widget.LinearLayout>(R.id.recent_chips)
                val views: List<View> = fixedIds.map { a.findViewById<View>(it) } + listOf(chips.getChildAt(0), a.findViewById<View>(R.id.btn_recent_more))
                views.forEach { v ->
                    assertWithMessage("${v.javaClass.simpleName} ${v.id}").that(v.height.toFloat()).isAtLeast(min)
                }
            }
        }
    }

    @Test fun recentItemsShowTheirWholeNameAndTime() {
        PickerTestSupport.resetHistory(HistoryRecord(HymnTypes.DB, 123, false, "標題", System.currentTimeMillis()))
        PickerTestSupport.launch { scenario ->
            FragmentHost.eventually {
                scenario.onActivity { a -> assertThat(a.findViewById<android.widget.LinearLayout>(R.id.recent_chips).childCount).isEqualTo(1) }
            }
            scenario.onActivity { a ->
                val item = a.findViewById<android.widget.LinearLayout>(R.id.recent_chips).getChildAt(0)
                for (id in listOf(R.id.tv_recent_label_item, R.id.tv_recent_when)) {
                    val t = item.findViewById<android.widget.TextView>(id)
                    assertWithMessage("recent text ${t.text}").that(t.layout.getEllipsisCount(0)).isEqualTo(0)
                    assertThat(t.text.toString()).isNotEmpty()
                }
            }
        }
    }

    @Test fun sourceButtonsAndContentsButtonShowTheirWholeName() {
        PickerTestSupport.launch { scenario ->
            scenario.onActivity { a ->
                val ids = listOf(
                    R.id.bs_db, R.id.bs_bb, R.id.bs_xb, R.id.bs_xg, R.id.bs_yb, R.id.bs_er, R.id.bs_english, R.id.btn_toc,
                )
                ids.forEach { id ->
                    val b = a.findViewById<android.widget.TextView>(id)
                    val layout = b.layout
                    assertWithMessage("layout of ${b.text}").that(layout).isNotNull()
                    for (line in 0 until layout.lineCount) {
                        assertWithMessage("'${b.text}' is cut off on line $line").that(layout.getEllipsisCount(line)).isEqualTo(0)
                    }
                    assertWithMessage("'${b.text}' needs more width than it has")
                        .that(layout.getLineWidth(0).toInt()).isAtMost(b.width - b.paddingLeft - b.paddingRight)
                }
            }
        }
    }
}
