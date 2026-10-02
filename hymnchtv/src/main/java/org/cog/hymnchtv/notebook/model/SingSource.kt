package org.cog.hymnchtv.notebook.model

/** How a sing log was created. */
enum class SingSource {
    AUTO, MANUAL;

    companion object {
        @JvmStatic
        fun fromStorage(value: String?): SingSource? = entries.firstOrNull { it.name == value }
    }
}
