package org.cog.hymnchtv.ui.playlist

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.ContentHandler
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.QuickTest
import org.cog.hymnchtv.R
import org.cog.hymnchtv.hymn.HymnRef
import org.cog.hymnchtv.notebook.NotebookTestSupport
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.cog.hymnchtv.ui.FragmentHost
import org.cog.hymnchtv.ui.notebook.NotebookPageActivity
import org.cog.hymnchtv.ui.notebook.NotebookPages
import org.cog.hymnchtv.ui.picker.HymnLabels
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The playlist page: order, open in order, remove with undo, TalkBack moves, rename, delete, share text without notes. */
@RunWith(AndroidJUnit4::class)
class PlaylistDetailPageTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val db5 = HymnKey.of(HymnTypes.DB, 5)
    private val db6 = HymnKey.of(HymnTypes.DB, 6)
    private val db7 = HymnKey.of(HymnTypes.DB, 7)
    private var mainScenario: ActivityScenario<MainActivity>? = null

    @Before fun setUp() {
        NotebookTestSupport.resetPlaylists()
        NotebookTestSupport.resetNotes(db5)
        // API 24-28: ContentHandler needs MainActivity's instance (see MainThreadDbSupport)
        mainScenario = org.cog.hymnchtv.MainThreadDbSupport.launchMainActivityIfNeeded()
    }

    @After fun tearDown() {
        NotebookTestSupport.resetPlaylists()
        NotebookTestSupport.resetNotes(db5)
        mainScenario?.close()
    }

    private fun launch(id: String): ActivityScenario<NotebookPageActivity> =
        ActivityScenario.launch(NotebookPages.playlist(ctx, id))

    private fun shownKeys(s: ActivityScenario<NotebookPageActivity>): List<HymnKey> {
        var keys = emptyList<HymnKey>()
        s.onActivity { a -> keys = fragment(a).currentItemsForTest().map { it.key } }
        return keys
    }

    private fun fragment(a: NotebookPageActivity) =
        a.supportFragmentManager.fragments.filterIsInstance<PlaylistDetailFragment>().single()

    private fun headline(key: HymnKey) = HymnLabels.headline(ctx, HymnRef(key.hymnType, key.hymnNo))

    /** The lyrics page that opened: (book, stored number); it is closed again. */
    private fun openedHymn(): Pair<String, Int> {
        var opened: Pair<String, Int>? = null
        FragmentHost.eventually(6000) {
            instrumentation.runOnMainSync {
                opened = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                    .filterIsInstance<ContentHandler>().firstOrNull()?.let {
                        it.intent.getStringExtra(MainActivity.ATTR_HYMN_TYPE).orEmpty() to it.intent.getIntExtra(MainActivity.ATTR_HYMN_NUMBER, -1)
                    }
            }
            assertThat(opened).isNotNull()
        }
        instrumentation.runOnMainSync {
            ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).filterIsInstance<ContentHandler>().forEach { it.finish() }
        }
        return opened!!
    }

    private fun playText(s: ActivityScenario<NotebookPageActivity>): String {
        var text = ""
        s.onActivity { text = it.findViewById<TextView>(R.id.playlist_play).text.toString() }
        return text
    }

    @Test @QuickTest fun showsTheNameAndTheHymnsInOrder() {
        val p = NotebookTestSupport.playlist("主日聚會", db5, db6, db7)
        launch(p.id).use { s ->
            FragmentHost.eventually { assertThat(shownKeys(s)).containsExactly(db5, db6, db7).inOrder() }
            s.onActivity { assertThat(it.findViewById<TextView>(R.id.playlist_name).text.toString()).isEqualTo("主日聚會") }
        }
    }

    @Test @QuickTest fun startOpensTheFirstThenTheButtonOffersTheNext() {
        val p = NotebookTestSupport.playlist("主日", db5, db6)
        launch(p.id).use { s ->
            FragmentHost.eventually { assertThat(playText(s)).isEqualTo(ctx.getString(R.string.playlist_start)) }
            onView(withId(R.id.playlist_play)).perform(click())
            assertThat(openedHymn()).isEqualTo(HymnTypes.DB to 5)
            FragmentHost.eventually { assertThat(playText(s)).isEqualTo(ctx.getString(R.string.playlist_next, headline(db6))) }
            onView(withId(R.id.playlist_play)).perform(click())
            assertThat(openedHymn()).isEqualTo(HymnTypes.DB to 6)
            FragmentHost.eventually { assertThat(playText(s)).isEqualTo(ctx.getString(R.string.playlist_restart)) }
        }
    }

    @Test fun removeAndUndoPutTheHymnBackInPlace() {
        val p = NotebookTestSupport.playlist("主日", db5, db6, db7)
        launch(p.id).use { s ->
            FragmentHost.eventually { assertThat(shownKeys(s)).hasSize(3) }
            s.onActivity { a ->
                val row = a.findViewById<RecyclerView>(R.id.playlist_items).findViewHolderForAdapterPosition(1)!!.itemView
                row.findViewById<View>(R.id.item_remove).performClick()
            }
            FragmentHost.eventually { assertThat(NotebookTestSupport.items(p.id)).containsExactly(db5, db7).inOrder() }
            onView(withText(R.string.fav_undo)).perform(click())
            FragmentHost.eventually { assertThat(NotebookTestSupport.items(p.id)).containsExactly(db5, db6, db7).inOrder() }
        }
    }

    @Test fun theTalkBackMoveDownActionReorders() {
        val p = NotebookTestSupport.playlist("主日", db5, db6, db7)
        launch(p.id).use { s ->
            FragmentHost.eventually { assertThat(shownKeys(s)).hasSize(3) }
            s.onActivity { a ->
                val row = a.findViewById<RecyclerView>(R.id.playlist_items).findViewHolderForAdapterPosition(0)!!.itemView
                val label = a.getString(R.string.playlist_move_down)
                val action = row.createAccessibilityNodeInfo().actionList.first { it.label?.toString() == label }
                assertThat(row.performAccessibilityAction(action.id, null)).isTrue()
            }
            FragmentHost.eventually { assertThat(NotebookTestSupport.items(p.id)).containsExactly(db6, db5, db7).inOrder() }
        }
    }

    @Test fun renameAndDelete() {
        val p = NotebookTestSupport.playlist("舊名字", db5)
        launch(p.id).use { s ->
            FragmentHost.eventually { assertThat(shownKeys(s)).hasSize(1) }
            onView(withId(R.id.playlist_more)).perform(click())
            onView(withText(R.string.playlist_rename)).inRoot(isPlatformPopup()).perform(click())
            onView(withId(R.id.playlist_name_input)).inRoot(isDialog()).perform(replaceText("新名字"))
            onView(withText(android.R.string.ok)).inRoot(isDialog()).perform(click())
            FragmentHost.eventually { assertThat(NotebookTestSupport.playlists().single().name).isEqualTo("新名字") }

            onView(withId(R.id.playlist_more)).perform(click())
            onView(withText(R.string.playlist_delete)).inRoot(isPlatformPopup()).perform(click())
            onView(withText(R.string.delete)).inRoot(isDialog()).perform(click())
            FragmentHost.eventually { assertThat(NotebookTestSupport.playlists()).isEmpty() }
            FragmentHost.eventually { assertThat(s.state).isEqualTo(Lifecycle.State.DESTROYED) }
        }
    }

    @Test fun theShareTextHasTheHymnsButNeverTheNotes() {
        NotebookTestSupport.addNote(db5, "私人筆記，不可外流")
        val p = NotebookTestSupport.playlist("主日", db5, db6)
        launch(p.id).use { s ->
            FragmentHost.eventually { assertThat(shownKeys(s)).hasSize(2) }
            var text = ""
            s.onActivity { text = fragment(it).shareText() }
            assertThat(text.lines().first()).isEqualTo("主日")
            assertThat(text).contains("1. " + headline(db5))
            assertThat(text).contains("2. " + headline(db6))
            assertThat(text).doesNotContain("私人筆記")
        }
    }

    @Test fun rowControlsHaveSpokenLabels() {
        val p = NotebookTestSupport.playlist("主日", db5)
        launch(p.id).use { s ->
            FragmentHost.eventually { assertThat(shownKeys(s)).hasSize(1) }
            s.onActivity { a ->
                val row = a.findViewById<RecyclerView>(R.id.playlist_items).findViewHolderForAdapterPosition(0)!!.itemView
                assertThat(row.findViewById<View>(R.id.item_drag).contentDescription).isEqualTo(a.getString(R.string.playlist_reorder))
                assertThat(row.findViewById<View>(R.id.item_remove).contentDescription).isEqualTo(a.getString(R.string.playlist_remove_item))
                assertThat(row.contentDescription.toString()).contains(a.getString(R.string.playlist_position, 1, 1))
            }
        }
    }

    @Test fun aDeletedPlaylistClosesThePage() {
        val p = NotebookTestSupport.playlist("主日")
        NotebookTestSupport.resetPlaylists()
        launch(p.id).use { s -> FragmentHost.eventually { assertThat(s.state).isEqualTo(Lifecycle.State.DESTROYED) } }
    }
}
