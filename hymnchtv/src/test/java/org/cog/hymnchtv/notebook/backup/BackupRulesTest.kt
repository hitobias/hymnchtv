package org.cog.hymnchtv.notebook.backup

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.cog.hymnchtv.persistance.room.HymnchtvDatabase
import org.cog.hymnchtv.notebook.settings.NotebookPrefs
import org.junit.Test
import java.io.File

/** Keeps backup rule files and the manifest in sync with the DB and prefs file names. */
class BackupRulesTest {
  private fun read(path: String): String {
      val file = File(path)
      assertWithMessage("$path (working dir ${File(".").absolutePath})").that(file.exists()).isTrue()
      return file.readText()
  }

  private val dbIncludes = listOf("", "-wal", "-shm").map { "domain=\"database\" path=\"${HymnchtvDatabase.FILE_NAME}$it\"" }
  private val prefsInclude = "domain=\"sharedpref\" path=\"${NotebookPrefs.FILE_NAME}.xml\""
  private val settingsInclude = "domain=\"sharedpref\" path=\"${SettingsPrefsNameTest.SETTINGS_FILE}.xml\""
  // Match on the path attribute only: the XML comments legitimately mention e.g. notebook_device.xml.
  private val forbiddenPaths = listOf("dbHymnApp", "notebook.db", NotebookPrefs.DEVICE_FILE_NAME)
  private val forbiddenAttribute = "disableIfNoEncryptionCapabilities=\""


  private fun count(xml: String, needle: String) = Regex(Regex.escape(needle)).findAll(xml).count()

  @Test
  fun legacyRulesIncludeNotebookAndSettingsOnly() {
      val xml = read("src/main/res/xml/notebook_backup_rules.xml")
      (dbIncludes + listOf(prefsInclude, settingsInclude)).forEach { assertThat(count(xml, it)).isEqualTo(1) }
      assertThat(count(xml, "<include ")).isEqualTo(5)
      forbiddenPaths.forEach { assertThat(xml).doesNotContain("path=\"$it") }
  }

  @Test
  fun extractionRulesCoverCloudBackupAndDeviceTransfer() {
      val xml = read("src/main/res/xml/notebook_data_extraction_rules.xml")
      assertThat(xml).contains("<cloud-backup>")
      assertThat(xml).contains("<device-transfer>")
      (dbIncludes + listOf(prefsInclude, settingsInclude)).forEach { assertThat(count(xml, it)).isEqualTo(2) }
      assertThat(count(xml, "<include ")).isEqualTo(10)
      forbiddenPaths.forEach { assertThat(xml).doesNotContain("path=\"$it") }
      assertThat(xml).doesNotContain(forbiddenAttribute)
  }

  @Test
  fun manifestEnablesBackupWithBothRuleFiles() {
      val manifest = read("src/main/AndroidManifest.xml")
      assertThat(manifest).contains("android:allowBackup=\"true\"")
      assertThat(manifest).contains("android:dataExtractionRules=\"@xml/notebook_data_extraction_rules\"")
      assertThat(manifest).contains("android:fullBackupContent=\"@xml/notebook_backup_rules\"")
      assertThat(manifest).doesNotContain("tools:ignore=\"DataExtractionRules\"")
  }
}
