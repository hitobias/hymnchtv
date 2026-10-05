package org.cog.hymnchtv.ui.settings

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import kotlinx.coroutines.launch
import org.cog.hymnchtv.About
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.about.HelpActivity
import org.cog.hymnchtv.locale.AppLanguage
import org.cog.hymnchtv.locale.LocaleStore
import org.cog.hymnchtv.mediaconfig.MediaConfig
import org.cog.hymnchtv.notebook.Notebook
import org.cog.hymnchtv.notebook.backup.BackupDocuments
import org.cog.hymnchtv.notebook.backup.BackupFileName
import org.cog.hymnchtv.reading.ReadingSettingsActivity
import org.cog.hymnchtv.service.androidupdate.UpdateServiceImpl
import org.cog.hymnchtv.ui.page.PagePreferenceFragment
import org.cog.hymnchtv.ui.settings.backup.BackupUiState
import org.cog.hymnchtv.ui.theme.NightMode
import org.cog.hymnchtv.ui.theme.ThemePrefs
import timber.log.Timber
import kotlin.concurrent.thread

/**
 * Settings page: everything the old main menu held, plus the notebook's singing-log switch and backup (D-1 F3/F4). Every
 * preference is non-persistent (see c_preferences.xml): the screen shows the effective value and a change listener applies it,
 * so opening the screen never writes a default.
 */
class SettingsFragment : PagePreferenceFragment() {
    override val pageTitleRes get() = R.string.page_title_settings

    private val prefs get() = requireContext().getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE)

    private val backup: BackupViewModel by viewModels()

    private val exportLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument(BackupDocuments.MIME_TYPE)) { uri: Uri? -> onExportPicked(uri) }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? -> onImportPicked(uri) }

    /** The document picker's answer; null means the user backed out, and nothing starts. */
    @VisibleForTesting
    internal fun onExportPicked(uri: Uri?) {
        if (uri != null) backup.runner.runExport(uri)
    }

    @VisibleForTesting
    internal fun onImportPicked(uri: Uri?) {
        if (uri != null) backup.runner.runImport(uri)
    }

    override fun onResume() {
        super.onResume()
        // A job may have finished while the page was stopped or being recreated (the ViewModel kept its message). Posted:
        // showNow() must not run while the fragment managers are still dispatching this resume.
        view?.post { if (isResumed) showPendingBackupResult() }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.c_preferences, rootKey)
        bindAppearance()
        bindActions()
        bindNotebook()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { backup.runner.state.collect { renderBackup(it) } }
        }
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

    private fun bindActions() {
        onClick("reading_settings") { startActivity(Intent(requireContext(), ReadingSettingsActivity::class.java)) }
        onClick("media_config") { startActivity(Intent(requireContext(), MediaConfig::class.java)) }
        // The update check does network IO
        onClick("check_update") { thread(name = "check-update") { UpdateServiceImpl.getInstance().checkForUpdates() } }
        onClick("permission_request") { openAppPermissionSettings() }
        onClick("online_help") { startActivity(Intent(requireContext(), HelpActivity::class.java)) }
        onClick("about") { startActivity(Intent(requireContext(), About::class.java)) }
    }

    /** The singing-log switch (shows the stored value, writes only on change) and the two backup entries. */
    private fun bindNotebook() {
        val async = Notebook.async(requireContext())
        findPreference<SwitchPreferenceCompat>(KEY_AUTO_RECORD)?.apply {
            isChecked = async.isAutoRecordEnabled()
            setOnPreferenceChangeListener { _, newValue ->
                async.setAutoRecordEnabled(newValue as Boolean)
                true
            }
        }
        onClick(KEY_EXPORT) { launchPicker { exportLauncher.launch(BackupFileName.suggested(System.currentTimeMillis())) } }
        onClick(KEY_IMPORT) { launchPicker { importLauncher.launch(BackupDocuments.OPEN_MIME_TYPES) } }
    }

    /** A device without a documents UI cannot pick a file: say so instead of crashing. */
    private fun launchPicker(launch: () -> Unit) {
        try {
            launch()
        } catch (e: ActivityNotFoundException) {
            Timber.w(e, "No document picker for the notebook backup")
            showDialog(getString(R.string.backup_error_io))
        }
    }

    private fun renderBackup(state: BackupUiState) {
        listOf(KEY_EXPORT, KEY_IMPORT).forEach { findPreference<Preference>(it)?.isEnabled = !state.running }
        findPreference<Preference>(KEY_EXPORT)?.setSummary(if (state.running) R.string.backup_running else R.string.backup_export_summary)
        showPendingBackupResult()
    }

    /**
     * Shows the finished job's message once the page can show a dialog, and only then consumes it: showNow() adds the dialog
     * synchronously, so the message is never cleared for a dialog that was not added. While the page is not resumed or its
     * state is saved the message stays in the ViewModel; onResume (also after recreation) shows it.
     */
    private fun showPendingBackupResult() {
        val message = backup.runner.state.value.message ?: return
        if (showDialog(BackupMessageText.of(requireContext(), message))) backup.runner.consumeMessage()
    }

    /** False when a dialog cannot be shown now (not resumed, or state already saved). */
    private fun showDialog(text: String): Boolean {
        if (!isResumed || childFragmentManager.isStateSaved) return false
        (childFragmentManager.findFragmentByTag(BackupResultDialog.TAG) as? DialogFragment)?.dismissNow()
        BackupResultDialog.newInstance(text).showNow(childFragmentManager, BackupResultDialog.TAG)
        return childFragmentManager.findFragmentByTag(BackupResultDialog.TAG) != null
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
        const val KEY_AUTO_RECORD = "auto_record"
        const val KEY_EXPORT = "backup_export"
        const val KEY_IMPORT = "backup_import"
    }
}
