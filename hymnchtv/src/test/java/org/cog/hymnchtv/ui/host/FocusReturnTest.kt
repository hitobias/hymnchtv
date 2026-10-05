package org.cog.hymnchtv.ui.host

import android.view.View
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.R
import org.junit.Test

class FocusReturnTest {
    @Test
    fun theViewThatHadTheKeyboardFocusComesFirst() {
        assertThat(FocusReturn.target(42, MainHost.TAG_SEARCH)).isEqualTo(42)
    }

    @Test
    fun otherwiseTheOverlaysOwnButton() {
        assertThat(FocusReturn.target(View.NO_ID, MainHost.TAG_SEARCH)).isEqualTo(R.id.tv_search)
        assertThat(FocusReturn.target(View.NO_ID, MainHost.TAG_HISTORY)).isEqualTo(R.id.btn_recent_more)
    }

    @Test
    fun anUnknownOverlayHasNoTarget() {
        assertThat(FocusReturn.target(View.NO_ID, "notebook")).isEqualTo(View.NO_ID)
    }
}
