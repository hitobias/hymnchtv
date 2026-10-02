package org.cog.hymnchtv

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.about.AboutLibrariesJson
import org.cog.hymnchtv.about.LicensesActivity
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The LXGW WenKai (HymnalKai font) credit and its SIL OFL 1.1 text are part of the generated licenses list that
 * replaced the old About page credit (sub-project Z); the licenses screen must open without crashing on API 24.
 */
@RunWith(AndroidJUnit4::class)
class AboutFontLicenseTest {
    @Test
    fun fontCreditIsInTheGeneratedLicensesWithFullOflText() {
        val context = getInstrumentation().targetContext
        val rows = context.resources.openRawResource(HymnsApp.getFileResId("aboutlibraries", "raw"))
            .bufferedReader().use { AboutLibrariesJson.parse(it.readText()) }
        val font = rows.single { it.id == "org.cog.hymnal:hymnalkai-font" }
        assertThat(font.description).contains("LXGW WenKai")
        assertThat(font.licenses.single().content).contains("SIL OPEN FONT LICENSE")
    }

    @Test
    fun licensesScreenOpensWithoutCrashing() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), LicensesActivity::class.java)
        ActivityScenario.launch<LicensesActivity>(intent).use { scenario ->
            getInstrumentation().waitForIdleSync()
            scenario.onActivity { assertThat(it.isFinishing).isFalse() }
        }
    }
}
