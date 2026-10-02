package org.cog.hymnchtv

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.about.AboutLibrariesJson
import org.junit.Test
import org.junit.runner.RunWith

/** Everything derived from the package name works under applicationId com.ziontkec.hymnal (sub-project Z). */
@RunWith(AndroidJUnit4::class)
class IdentityRuntimeTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun packageNameIsTheNewApplicationId() {
        assertThat(context.packageName).isEqualTo("com.ziontkec.hymnal")
    }

    @Test
    fun resourceLookupByNameWorksUnderNewApplicationId() {
        assertThat(HymnsApp.getFileResId("hymnchtv", "drawable")).isEqualTo(R.drawable.hymnchtv)
    }

    @Test
    fun fileProviderAuthorityFollowsApplicationId() {
        assertThat(context.packageManager.resolveContentProvider("com.ziontkec.hymnal.files", 0)).isNotNull()
    }

    @Test
    fun generatedLicensesAreBundledAndStartWithTheOriginalProjectNotice() {
        val resId = HymnsApp.getFileResId("aboutlibraries", "raw")
        assertThat(resId).isNotEqualTo(0)
        val rows = context.resources.openRawResource(resId).bufferedReader().use { AboutLibrariesJson.parse(it.readText()) }
        assertThat(rows.first().id).isEqualTo("org.cog:hymnchtv")
        assertThat(rows.first().description).contains("Copyright 2020 Eng Chong Meng")
    }
}
