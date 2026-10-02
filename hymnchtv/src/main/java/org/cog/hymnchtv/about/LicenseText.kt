package org.cog.hymnchtv.about

/** Plain-text rendering for the native licenses screen (no HTML, no clickable links). */
object LicenseText {
    private val BREAK_TAG = Regex("""<br\s*/?>\s*\n?""", RegexOption.IGNORE_CASE)

    @JvmStatic
    fun plain(content: String): String = content.replace("\r\n", "\n").replace(BREAK_TAG, "\n")

    @JvmStatic
    fun title(row: LicenseRow): String {
        val version = row.version?.let { " $it" }.orEmpty()
        val licenses = row.licenses.joinToString(", ") { it.name }
        return if (licenses.isEmpty()) "${row.name}$version" else "${row.name}$version — $licenses"
    }

    /** Description, then each license's name and full text; the license URL only when no text is bundled. */
    @JvmStatic
    fun detail(row: LicenseRow, missingTextLabel: String): String = buildString {
        row.description?.let { append(it).append("\n\n") }
        row.licenses.forEachIndexed { index, license ->
            if (index > 0) append("\n\n")
            append(license.name).append('\n')
            val content = license.content
            if (content != null) append(plain(content).trim())
            else {
                append(missingTextLabel)
                license.url?.let { append('\n').append(it) }
            }
        }
    }
}
