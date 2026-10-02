package org.cog.hymnchtv

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.utils.DialogActivity
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Every AppCompat activity declared with its own manifest theme must use an AppCompat theme. BaseActivity no longer
 * forces one (ThemeHelper.setTheme was removed), so a Holo dialog theme throws "You need to use a Theme.AppCompat theme".
 */
@RunWith(AndroidJUnit4::class)
class DialogThemeActivitiesTest {
    @Test
    fun aboutOpens() = assertOpens(About::class.java, Intent())

    @Test
    fun dialogActivityOpens() = assertOpens(
        DialogActivity::class.java,
        Intent().putExtra(DialogActivity.EXTRA_TITLE, "t").putExtra(DialogActivity.EXTRA_MESSAGE, "m")
            .putExtra(DialogActivity.EXTRA_CANCELABLE, true)
    )

    private fun <T : android.app.Activity> assertOpens(cls: Class<T>, extras: Intent) {
        val intent = Intent(ApplicationProvider.getApplicationContext(), cls).putExtras(extras)
        ActivityScenario.launch<T>(intent).use { scenario ->
            getInstrumentation().waitForIdleSync()
            scenario.onActivity { assertThat(it.isFinishing).isFalse() }
        }
    }

    @Test
    fun noAppCompatActivityKeepsANonAppCompatManifestTheme() {
        val pm = getInstrumentation().targetContext.packageManager
        val pkg = getInstrumentation().targetContext.packageName
        val info = pm.getPackageInfo(pkg, android.content.pm.PackageManager.GET_ACTIVITIES)
        val bad = info.activities!!.filter { a ->
            val cls = Class.forName(a.name)
            androidx.appcompat.app.AppCompatActivity::class.java.isAssignableFrom(cls) &&
                a.theme != 0 &&
                !isAppCompat(a.theme)
        }.map { it.name }
        assertThat(bad).isEmpty()
    }

    private fun isAppCompat(themeRes: Int): Boolean {
        val ctx = android.view.ContextThemeWrapper(getInstrumentation().targetContext, themeRes)
        val a = ctx.obtainStyledAttributes(intArrayOf(androidx.appcompat.R.attr.windowActionBar))
        return try { a.hasValue(0) } finally { a.recycle() }
    }
}
