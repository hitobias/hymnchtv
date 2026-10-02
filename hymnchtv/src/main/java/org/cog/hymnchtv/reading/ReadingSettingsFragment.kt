package org.cog.hymnchtv.reading

import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import org.cog.hymnchtv.ContentView
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.lyrics.LyricsLanguagePolicy
import org.cog.hymnchtv.reading.background.BackgroundDrawables
import org.cog.hymnchtv.reading.background.BackgroundPrefs
import org.cog.hymnchtv.reading.background.BackgroundSlot
import org.cog.hymnchtv.reading.background.PhotoBackground

/** The reading settings (plan A2); sub-project C can host this fragment in its settings page unchanged. */
class ReadingSettingsFragment : PreferenceFragmentCompat() {
    private val pickBackground = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        refreshBackgroundSummaries()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = MainActivity.PREF_SETTINGS
        healConversionType()
        setPreferencesFromResource(R.xml.reading_preferences, rootKey)

        findPreference<ListPreference>(ContentView.PREF_CONVERSION_TYPE)?.isVisible = LyricsLanguagePolicy.HK_VARIANT_ENABLED

        findPreference<ListPreference>(ReadingPrefKeys.LYRICS_FONT_SIZE)?.setOnPreferenceChangeListener { _, newValue ->
            prefs()?.let { ReadingPrefs.resetLyricsScale(it.edit(), LyricsFontSize.fromPref(newValue as? String)).apply() }
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
                // S2: pickBackground.launch(BackgroundPickerActivity.intent(requireContext(), slot))
                true
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshBackgroundSummaries()
    }

    private fun prefs(): SharedPreferences? = preferenceManager.sharedPreferences

    private fun refreshBackgroundSummaries() {
        val sp = prefs() ?: return
        for (slot in BackgroundSlot.entries) {
            findPreference<Preference>(slot.prefKey)?.summary = getString(BackgroundDrawables.nameRes(BackgroundPrefs.resolve(sp, slot)))
        }
    }

    /** Same self-heal as the old ChineseS2TSelection (plan A.1.5): rewrite an invalid ConversionType once. */
    private fun healConversionType() {
        val sp = prefs() ?: return
        val raw = runCatching { sp.getString(ContentView.PREF_CONVERSION_TYPE, null) }.getOrNull()
        if (raw != null && !LyricsLanguagePolicy.isCanonical(raw)) {
            val variant = LyricsLanguagePolicy.parseVariant(raw, resources.configuration.locales[0])
            sp.edit().putString(ContentView.PREF_CONVERSION_TYPE, variant.prefValue).apply()
        }
    }
}
