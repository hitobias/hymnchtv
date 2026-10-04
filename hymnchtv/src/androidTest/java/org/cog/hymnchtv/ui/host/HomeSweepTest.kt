package org.cog.hymnchtv.ui.host

import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.R
import org.cog.hymnchtv.ui.picker.PickerTestSupport
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** 1.2.0 sweep, home side: the top bar blends into the background, the number preview, the shared-number transition. */
@RunWith(AndroidJUnit4::class)
class HomeSweepTest {
    @Before fun setUp() = PickerTestSupport.prepare()

    @Test fun theTopBarAddsNoBandOnAPlainBackground() = PickerTestSupport.launch { scenario ->
        scenario.onActivity { a ->
            val bar = a.findViewById<View>(R.id.toolbar)
            // The default background is a plain preset: the bar is transparent and the page reaches behind it
            assertThat((bar.background as? ColorDrawable)?.color ?: 0).isEqualTo(0)
            val container = a.findViewById<View>(R.id.fragment_container)
            assertThat(container.top).isAtMost(bar.top)
        }
    }

    @Test fun theNumberPreviewIsTheDisplayLevelWithTabularFigures() = PickerTestSupport.launch { scenario ->
        scenario.onActivity { a ->
            val entry = a.findViewById<TextView>(R.id.tv_entry)
            assertThat(entry.textSize).isWithin(1f).of(48f * a.resources.configuration.fontScale * a.resources.displayMetrics.density)
            assertThat(entry.fontFeatureSettings).isEqualTo("tnum")
            assertThat(entry.typeface.isBold).isFalse()
        }
    }

    @Test fun thePreviewCardKeepsItsHeightWhileTyping() = PickerTestSupport.launch { scenario ->
        var before = 0
        scenario.onActivity { before = it.findViewById<View>(R.id.previewArea).height }
        PickerTestSupport.type("1")
        PickerTestSupport.type("23")
        scenario.onActivity { assertThat(it.findViewById<View>(R.id.previewArea).height).isEqualTo(before) }
    }

    @Test fun theKeypadNeverGrowsPast64dp() = PickerTestSupport.launch { scenario ->
        scenario.onActivity { a ->
            val d = a.resources.displayMetrics.density
            assertThat(a.findViewById<View>(R.id.n5).height).isAtMost((64 * d).toInt() + 1)
        }
    }
}
