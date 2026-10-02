package org.cog.hymnchtv.notebook.backup

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object BackupFileName {
  private const val PREFIX = "hymnchtv-notebook-"
  private const val EXTENSION = ".json"
  private const val PATTERN = "yyyyMMdd-HHmm"

  /** Default title for ActivityResultContracts.CreateDocument, e.g. hymnchtv-notebook-20261002-1530.json. */
  @JvmStatic
  @JvmOverloads
  fun suggested(nowMillis: Long, zone: TimeZone = TimeZone.getDefault()): String {
      val format = SimpleDateFormat(PATTERN, Locale.US).apply { timeZone = zone }
      return PREFIX + format.format(Date(nowMillis)) + EXTENSION
  }
}
