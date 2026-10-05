package org.cog.hymnchtv.ui.settings

import org.cog.hymnchtv.QuickTest
import android.content.Context
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.notebook.Notebook
import org.cog.hymnchtv.notebook.settings.NotebookPrefs
import org.cog.hymnchtv.reading.ReadingSettingsActivity
import org.cog.hymnchtv.ui.FragmentHost
import org.cog.hymnchtv.ui.theme.NightMode
import org.cog.hymnchtv.ui.theme.ThemePrefs
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsFragmentTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()
    private val prefs = ctx.getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE)

    @Before
    fun setUp() {
        FragmentHost.grantLaunchPermissions(ctx.packageName)
        clean()
    }

    @After
    fun tearDown() {
        clean()
        ThemePrefs.applyStored(ctx)
    }

    private fun clean() {
        prefs.edit().remove(ThemePrefs.PREF_THEME).commit()
    }

    private fun withSettings(block: (SettingsFragment, ActivityScenario<MainActivity>) -> Unit) {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var fragment: SettingsFragment? = null
            scenario.onActivity { fragment = FragmentHost.show(it, SettingsFragment()) }
            block(checkNotNull(fragment), scenario)
        }
    }

    private fun <T : Preference> ActivityScenario<MainActivity>.pref(fragment: SettingsFragment, key: String): T {
        var found: T? = null
        onActivity { found = fragment.findPreference(key) }
        return checkNotNull(found) { key }
    }

    @Test
    @QuickTest
    fun showsEffectiveValuesWithoutWritingThem() = withSettings { fragment, scenario ->
        assertThat(scenario.pref<ListPreference>(fragment, "Theme").value).isEqualTo(NightMode.DEFAULT.name)
        // Opening the screen froze nothing
        assertThat(prefs.contains(ThemePrefs.PREF_THEME)).isFalse()
    }

    @Test
    @QuickTest
    fun choosingDarkInTheDialogStoresIt() = withSettings { _, _ ->
        onView(withText(R.string.theme_menu)).perform(click())
        onView(withText(R.string.theme_dark)).inRoot(isDialog()).perform(click())
        FragmentHost.eventually { assertThat(ThemePrefs.current(ctx)).isEqualTo(NightMode.DARK) }
    }

    @Test
    fun homeTextSizeAndColorSettingsAreGone() = withSettings { fragment, scenario ->
        scenario.onActivity {
            assertThat(fragment.findPreference<Preference>("TextSize")).isNull()
            assertThat(fragment.findPreference<Preference>("TextColor")).isNull()
        }
    }

    private fun resetAutoRecord() {
        ctx.getSharedPreferences(NotebookPrefs.FILE_NAME, Context.MODE_PRIVATE).edit().remove(NotebookPrefs.KEY_AUTO_RECORD).commit()
    }

    @Test
    fun notebookCategoriesAreShownWithTheirEntries() = withSettings { fragment, scenario ->
        val singLog = scenario.pref<PreferenceCategory>(fragment, "c_cat_sing_log")
        val backup = scenario.pref<PreferenceCategory>(fragment, "c_cat_backup")
        assertThat(singLog.isVisible).isTrue()
        assertThat(singLog.preferenceCount).isEqualTo(1)
        assertThat(backup.isVisible).isTrue()
        assertThat(backup.preferenceCount).isEqualTo(2)
        assertThat(scenario.pref<Preference>(fragment, SettingsFragment.KEY_EXPORT).isEnabled).isTrue()
        assertThat(scenario.pref<Preference>(fragment, SettingsFragment.KEY_IMPORT).isEnabled).isTrue()
    }

    @Test
    @QuickTest
    fun autoRecordIsOffByDefaultAndTheSwitchTurnsItOn() {
        resetAutoRecord()
        try {
            withSettings { fragment, scenario ->
                assertThat(scenario.pref<SwitchPreferenceCompat>(fragment, SettingsFragment.KEY_AUTO_RECORD).isChecked).isFalse()
                // Opening the screen wrote nothing
                assertThat(ctx.getSharedPreferences(NotebookPrefs.FILE_NAME, Context.MODE_PRIVATE).contains(NotebookPrefs.KEY_AUTO_RECORD)).isFalse()
                scenario.onActivity { fragment.scrollToPreference(SettingsFragment.KEY_AUTO_RECORD) }
                onView(withText(R.string.sing_log_auto)).perform(click())
                FragmentHost.eventually { assertThat(Notebook.async(ctx).isAutoRecordEnabled()).isTrue() }
                assertThat(scenario.pref<SwitchPreferenceCompat>(fragment, SettingsFragment.KEY_AUTO_RECORD).isChecked).isTrue()
            }
        } finally {
            resetAutoRecord()
        }
    }

    @Test
    @QuickTest
    fun readingSettingsEntryOpensTheReadingSettings() = withSettings { _, _ ->
        onView(withText(R.string.reading_settings)).perform(click())
        FragmentHost.eventually {
            val resumed = InstrumentationRegistry.getInstrumentation().let {
                var names = emptyList<String>()
                it.runOnMainSync {
                    names = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).map { a -> a.javaClass.name }
                }
                names
            }
            assertThat(resumed).contains(ReadingSettingsActivity::class.java.name)
        }
        pressBack()
    }
}
