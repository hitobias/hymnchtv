package org.cog.hymnchtv.ui.playlist

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.HymnsApp
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.QuickTest
import org.cog.hymnchtv.R
import org.cog.hymnchtv.notebook.NotebookTestSupport
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.cog.hymnchtv.ui.FragmentHost
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddToPlaylistDialogTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()
    private val db5 = HymnKey.of(HymnTypes.DB, 5)

    @Before fun setUp() {
        FragmentHost.grantLaunchPermissions(ctx.packageName)
        NotebookTestSupport.resetPlaylists()
    }

    @After fun tearDown() = NotebookTestSupport.resetPlaylists()

    private fun withDialog(block: () -> Unit) {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { AddToPlaylistDialog.show(it.supportFragmentManager, db5) }
            block()
        }
    }

    @Test @QuickTest fun pickingAPlaylistAddsTheHymnAndSaysWhere() {
        val p = NotebookTestSupport.playlist("主日")
        withDialog {
            onView(withText("主日")).inRoot(isDialog()).perform(click())
            FragmentHost.eventually { assertThat(NotebookTestSupport.items(p.id)).containsExactly(db5) }
            FragmentHost.eventually { assertThat(HymnsApp.getLastToastMessage()).isEqualTo(ctx.getString(R.string.playlist_added, "主日")) }
        }
    }

    @Test fun aNewPlaylistCanBeMadeFromTheDialog() {
        withDialog {
            onView(withId(R.id.add_playlist_new)).inRoot(isDialog()).perform(click())
            onView(withId(R.id.playlist_name_input)).inRoot(isDialog()).perform(replaceText("晚上"))
            onView(withText(android.R.string.ok)).inRoot(isDialog()).perform(click())
            FragmentHost.eventually {
                val created = NotebookTestSupport.playlists().single()
                assertThat(created.name).isEqualTo("晚上")
                assertThat(NotebookTestSupport.items(created.id)).containsExactly(db5)
            }
        }
    }
}
