package org.cog.hymnchtv.reading

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import org.cog.hymnchtv.BaseActivity
import org.cog.hymnchtv.ContentView
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R

/**
 * Hosts ReadingSettingsFragment (plan A2). Settings apply immediately; on finish the result carries
 * ContentView.EXTR_KEY_HAS_CHANGES so an open lyrics page can rebuild itself.
 */
class ReadingSettingsActivity : BaseActivity() {
    private lateinit var prefs: SharedPreferences
    private var changed = false
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> changed = true }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.reading_settings)
        setTitle(R.string.reading_settings)
        changed = savedInstanceState?.getBoolean(STATE_CHANGED) ?: false
        prefs = getSharedPreferences(MainActivity.PREF_SETTINGS, MODE_PRIVATE)
        // Registered before the fragment exists so its one-off self-heal also counts as a change
        prefs.registerOnSharedPreferenceChangeListener(listener)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.readingSettingsContainer, ReadingSettingsFragment())
                .commit()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_CHANGED, changed)
    }

    override fun onDestroy() {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
        super.onDestroy()
    }

    override fun finish() {
        setResult(RESULT_OK, Intent().putExtra(ContentView.EXTR_KEY_HAS_CHANGES, changed))
        super.finish()
    }

    private companion object {
        const val STATE_CHANGED = "state_changed"
    }
}
