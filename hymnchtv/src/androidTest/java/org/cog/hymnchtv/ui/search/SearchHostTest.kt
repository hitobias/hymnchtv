package org.cog.hymnchtv.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.ContentHandler
import org.cog.hymnchtv.R
import org.cog.hymnchtv.hymn.HymnRef
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.cog.hymnchtv.ui.FragmentHost
import org.cog.hymnchtv.ui.picker.PickerTestSupport
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** A search page inside a fragment that is a SearchHost hands the tapped result to it and opens no lyrics page. */
@RunWith(AndroidJUnit4::class)
class SearchHostTest {
    @Before fun setUp() = PickerTestSupport.prepare()
    @After fun tearDown() = PickerTestSupport.cleanUp()

    class RecordingParent : Fragment(), SearchHost {
        val received = mutableListOf<HymnRef>()

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            FrameLayout(inflater.context).apply { id = CONTAINER }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            if (savedInstanceState == null) childFragmentManager.beginTransaction().replace(CONTAINER, SearchFragment.newInstance(null)).commit()
        }

        override fun onSearchResult(ref: HymnRef) {
            received += ref
        }

        companion object {
            val CONTAINER = View.generateViewId()
        }
    }

    @Test fun aParentSearchHostGetsTheResultInsteadOfALyricsPage() = PickerTestSupport.launch { scenario ->
        lateinit var parent: RecordingParent
        scenario.onActivity { parent = FragmentHost.show(it, RecordingParent()) }
        onView(withId(R.id.search_input)).perform(replaceText("祂的計劃"))
        FragmentHost.eventually(10_000) {
            var count = 0
            scenario.onActivity { count = it.findViewById<RecyclerView>(R.id.search_results).adapter!!.itemCount }
            assertThat(count).isGreaterThan(0)
        }
        scenario.onActivity { it.findViewById<RecyclerView>(R.id.search_results).findViewHolderForAdapterPosition(0)!!.itemView.performClick() }
        FragmentHost.eventually { assertThat(parent.received).containsExactly(HymnRef(HymnTypes.DB, 1)) }
        Thread.sleep(500)
        var lyricsPages = -1
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val monitor = ActivityLifecycleMonitorRegistry.getInstance()
            lyricsPages = listOf(Stage.PRE_ON_CREATE, Stage.CREATED, Stage.STARTED, Stage.RESUMED)
                .flatMap { monitor.getActivitiesInStage(it) }.count { it is ContentHandler }
        }
        assertThat(lyricsPages).isEqualTo(0)
    }
}
