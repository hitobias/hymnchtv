package org.cog.hymnchtv.ui.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import org.cog.hymnchtv.About
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.locale.AppLanguage
import org.cog.hymnchtv.locale.LocaleStore
import org.cog.hymnchtv.mediaconfig.MediaConfig
import org.cog.hymnchtv.reading.ReadingSettingsActivity
import org.cog.hymnchtv.service.androidupdate.UpdateServiceImpl
import org.cog.hymnchtv.ui.home.HomePrefs
import org.cog.hymnchtv.ui.theme.NightMode
import org.cog.hymnchtv.ui.theme.ThemePrefs
import kotlin.concurrent.thread

/**
 * Settings tab: everything the old main menu held. Every preference is non-persistent (see c_preferences.xml): the screen
 * shows the effective value and a change listener applies it, so opening the screen never writes a default.
 */
class SettingsFragment : PreferenceFragmentCompat() {
    private val prefs get() = requireContext().getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE)

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.c_preferences, rootKey)
        bindAppearance()
        bindHomeScreen()
        bindActions()
    }

    private fun bindAppearance() {
        list(KEY_THEME).apply {
            value = ThemePrefs.current(requireContext()).name
            setOnPreferenceChangeListener { _, newValue ->
                val mode = NightMode.from(newValue as String)
                if (mode != ThemePrefs.current(requireContext())) {
                    // AppCompatDelegate.setDefaultNightMode (inside apply) recreates the started activities itself
                    ThemePrefs.apply(requireContext(), mode)
                }
                true
            }
        }
        list(KEY_LOCALE).apply {
            value = LocaleStore.current(requireContext()).name
            setOnPreferenceChangeListener { _, newValue ->
                applyLocale(AppLanguage.valueOf(newValue as String))
                true
            }
        }
    }

    /** API 33+: the framework applies it and recreates activities; API < 33: the process restarts to re-wrap HymnsApp's context. */
    private fun applyLocale(language: AppLanguage) {
        if (language == LocaleStore.current(requireContext())) return
        if (LocaleStore.set(requireContext(), language)) restartApp()
    }

    private fun restartApp() {
        val intent = requireContext().packageManager.getLaunchIntentForPackage(requireContext().packageName) ?: return
        startActivity(Intent.makeRestartActivityTask(intent.component as ComponentName))
        Runtime.getRuntime().exit(0)
    }

    private fun bindHomeScreen() {
        findPreference<SeekBarPreference>(KEY_TEXT_SIZE)?.apply {
            value = HomePrefs.textSize(prefs)
            setOnPreferenceChangeListener { _, newValue ->
                prefs.edit().putInt(HomePrefs.TEXT_SIZE, newValue as Int).apply()
                true
            }
        }
        val black = ContextCompat.getColor(requireContext(), R.color.grey900)
        list(KEY_TEXT_COLOR).apply {
            // A stored colour that is none of the eight (should not happen) leaves the list without a selection
            value = HomeTextColors.nameOf(HomePrefs.textColor(prefs, black), black)
            setOnPreferenceChangeListener { _, newValue ->
                HomeTextColors.colorOf(newValue as String, black)?.let { prefs.edit().putInt(HomePrefs.TEXT_COLOR, it).apply() }
                true
            }
        }
    }

    private fun bindActions() {
        onClick("reading_settings") { startActivity(Intent(requireContext(), ReadingSettingsActivity::class.java)) }
        onClick("media_config") { startActivity(Intent(requireContext(), MediaConfig::class.java)) }
        // The update check does network IO
        onClick("check_update") { thread(name = "check-update") { UpdateServiceImpl.getInstance().checkForUpdates() } }
        onClick("permission_request") { openAppPermissionSettings() }
        onClick("online_help") { About.hymnUrlAccess(requireContext(), MainActivity.HYMNCHTV_FAQ) }
        onClick("about") { startActivity(Intent(requireContext(), About::class.java)) }
    }

    private fun openAppPermissionSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${requireContext().packageName}"))
            .addCategory(Intent.CATEGORY_DEFAULT)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun onClick(key: String, action: () -> Unit) {
        findPreference<Preference>(key)?.setOnPreferenceClickListener {
            action()
            true
        }
    }

    private fun list(key: String): ListPreference =
        checkNotNull(findPreference(key)) { "Missing preference $key in c_preferences.xml" }

    companion object {
        const val KEY_THEME = "Theme"
        const val KEY_LOCALE = "Locale"
        const val KEY_TEXT_SIZE = "TextSize"
        const val KEY_TEXT_COLOR = "TextColor"
    }
}
