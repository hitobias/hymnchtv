package org.cog.hymnchtv.reading

import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import org.cog.hymnchtv.ContentView
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.lyrics.LyricsLang
import org.cog.hymnchtv.lyrics.LyricsLanguagePolicy
import org.cog.hymnchtv.reading.background.BackgroundDrawables
import org.cog.hymnchtv.reading.background.BackgroundPrefs
import org.cog.hymnchtv.reading.background.BackgroundSlot
import org.cog.hymnchtv.reading.background.PhotoBackground
import org.cog.hymnchtv.ui.page.PagePreferenceFragment
import java.util.Locale

/** The reading settings (plan A2); sub-project C can host this fragment in its settings page unchanged. */
class ReadingSettingsFragment : PagePreferenceFragment() {
    override val pageTitleRes get() = R.string.reading_settings

    private val pickBackground = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        refreshBackgroundSummaries()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = MainActivity.PREF_SETTINGS
        setPreferencesFromResource(R.xml.reading_preferences, rootKey)
        bindStoredValues()

        findPreference<ListPreference>(ContentView.PREF_CONVERSION_TYPE)?.isVisible = LyricsLanguagePolicy.HK_VARIANT_ENABLED

        // Choosing a size also restarts pinch zoom from it; a user-originated write like every other setting here
        findPreference<ListPreference>(ReadingPrefKeys.LYRICS_FONT_SIZE)?.setOnPreferenceChangeListener { _, newValue ->
            prefs()?.edit()?.let {
                ReadingPrefs.resetLyricsScale(it.putString(ReadingPrefKeys.LYRICS_FONT_SIZE, newValue as String), LyricsFontSize.fromPref(newValue)).apply()
            }
            true
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            findPreference<SeekBarPreference>(PhotoBackground.PREF_BLUR)?.apply {
                isEnabled = false
                summary = getString(R.string.pref_photo_blur_unsupported)
            }
        }

        for (slot in BackgroundSlot.entries) {
            findPreference<Preference>(slot.prefKey)?.setOnPreferenceClickListener {
                pickBackground.launch(BackgroundPickerActivity.intent(requireContext(), slot))
                true
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshBackgroundSummaries()
    }

    /**
     * Every preference here is non-persistent (see reading_preferences.xml): showing the effective value must not write it,
     * or a later computed default (low-RAM devices turning PageAnimation off) would already be frozen by merely opening the screen.
     * A change by the user is written by [writeOnChange].
     */
    private fun bindStoredValues() {
        val sp = prefs() ?: return
        val locale = resources.configuration.locales[0]
        listValue(ReadingPrefKeys.DISPLAY_MODE, ReadingPrefs.displayMode(sp).name)
        listValue(LyricsLanguagePolicy.PREF_LYRICS_DEFAULT, LyricsLang.fromPref(storedString(sp, LyricsLanguagePolicy.PREF_LYRICS_DEFAULT)).name)
        listValue(ContentView.PREF_CONVERSION_TYPE, LyricsLanguagePolicy.parseVariant(storedString(sp, ContentView.PREF_CONVERSION_TYPE), locale).prefValue)
        listValue(ReadingPrefKeys.LYRICS_FONT_SIZE, ReadingPrefs.fontSize(sp).name)
        listValue(ReadingPrefKeys.LYRICS_FONT, ReadingPrefs.lyricsFont(sp).name)
        listValue(ReadingPrefKeys.LYRICS_FONT_WEIGHT, ReadingPrefs.lyricsWeight(sp).prefValue)
        switchValue(ReadingPrefKeys.PAGE_ANIMATION, ReadingPrefs.pageAnimation(sp))
        switchValue(ReadingPrefKeys.MENU_SHOW, runCatching { sp.getBoolean(ReadingPrefKeys.MENU_SHOW, true) }.getOrDefault(true))
        switchValue(ReadingPrefKeys.KEEP_SCREEN_ON, ReadingPrefs.keepScreenOn(sp))
        seekValue(PhotoBackground.PREF_DIM, PhotoBackground.DIM_DEFAULT)
        seekValue(PhotoBackground.PREF_BLUR, PhotoBackground.BLUR_DEFAULT)
    }

    private fun storedString(sp: SharedPreferences, key: String): String? = runCatching { sp.getString(key, null) }.getOrNull()

    private fun listValue(key: String, value: String) {
        findPreference<ListPreference>(key)?.let { it.value = value; writeOnChange(it) { sp, v -> sp.putString(key, v as String) } }
    }

    private fun switchValue(key: String, value: Boolean) {
        findPreference<SwitchPreferenceCompat>(key)?.let { it.isChecked = value; writeOnChange(it) { sp, v -> sp.putBoolean(key, v as Boolean) } }
    }

    private fun seekValue(key: String, default: Int) {
        val stored = runCatching { prefs()?.getInt(key, default) }.getOrNull() ?: default
        findPreference<SeekBarPreference>(key)?.let { it.value = stored; writeOnChange(it) { sp, v -> sp.putInt(key, v as Int) } }
    }

    private fun writeOnChange(pref: Preference, put: (SharedPreferences.Editor, Any) -> SharedPreferences.Editor) {
        pref.setOnPreferenceChangeListener { _, newValue ->
            prefs()?.let { put(it.edit(), newValue).apply() }
            true
        }
    }

    private fun prefs(): SharedPreferences? = preferenceManager.sharedPreferences

    private fun refreshBackgroundSummaries() {
        val sp = prefs() ?: return
        for (slot in BackgroundSlot.entries) {
            findPreference<Preference>(slot.prefKey)?.summary = getString(BackgroundDrawables.nameRes(BackgroundPrefs.resolve(sp, slot)))
        }
    }

    companion object {
        /** Same self-heal as the old ChineseS2TSelection (plan A.1.5): rewrite an invalid ConversionType once. */
        @JvmStatic
        fun healConversionType(sp: SharedPreferences, uiLocale: Locale) {
            val raw = runCatching { sp.getString(ContentView.PREF_CONVERSION_TYPE, null) }.getOrNull()
            if (raw != null && !LyricsLanguagePolicy.isCanonical(raw)) {
                val variant = LyricsLanguagePolicy.parseVariant(raw, uiLocale)
                sp.edit().putString(ContentView.PREF_CONVERSION_TYPE, variant.prefValue).apply()
            }
        }
    }
}
