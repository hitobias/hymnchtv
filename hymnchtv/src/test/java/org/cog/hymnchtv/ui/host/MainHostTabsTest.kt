package org.cog.hymnchtv.ui.host

import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.R
import org.junit.Test

class MainHostTabsTest {
    @Test
    fun withoutNotebookTheMyHymnsTabIsLeftOut() {
        assertThat(MainHost.tabIds(notebookEnabled = false))
            .containsExactly(R.id.nav_home, R.id.nav_toc, R.id.nav_settings).inOrder()
    }

    @Test
    fun withNotebookAllFourTabsAreListedInOrder() {
        assertThat(MainHost.tabIds(notebookEnabled = true))
            .containsExactly(R.id.nav_home, R.id.nav_toc, R.id.nav_my_hymns, R.id.nav_settings).inOrder()
    }

    @Test
    fun releaseBuildHidesTheNotebookUi() {
        assertThat(UiFlags.NOTEBOOK_UI_ENABLED).isFalse()
    }
}
