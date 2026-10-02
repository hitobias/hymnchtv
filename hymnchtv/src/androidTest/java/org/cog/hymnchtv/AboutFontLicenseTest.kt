package org.cog.hymnchtv

import android.content.Intent
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/** Tapping the font credit reads the bundled OFL text; this crashed on API 24 (commons-io needs java.nio.file). */
@RunWith(AndroidJUnit4::class)
class AboutFontLicenseTest {
    @Test
    fun tappingTheFontCreditShowsTheLicenceWithoutCrashing() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), About::class.java)
        ActivityScenario.launch<About>(intent).use { scenario ->
            scenario.onActivity { it.findViewById<View>(R.id.font_credit).performClick() }
            getInstrumentation().waitForIdleSync()
            scenario.onActivity { assertThat(it.isFinishing).isFalse() }
        }
    }
}
