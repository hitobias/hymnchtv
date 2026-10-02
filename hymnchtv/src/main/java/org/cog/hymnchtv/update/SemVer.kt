package org.cog.hymnchtv.update

/**
 * Semantic version with semver.org 2.0.0 precedence. A leading "v"/"V" is accepted and build metadata
 * ("+...") is ignored, so v2.10.0 == 2.10.0+build.
 */
data class SemVer(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preRelease: List<String> = emptyList(),
) : Comparable<SemVer> {

    override fun compareTo(other: SemVer): Int {
        compareValues(major, other.major).let { if (it != 0) return it }
        compareValues(minor, other.minor).let { if (it != 0) return it }
        compareValues(patch, other.patch).let { if (it != 0) return it }
        return comparePreRelease(preRelease, other.preRelease)
    }

    override fun toString(): String =
        "$major.$minor.$patch" + if (preRelease.isEmpty()) "" else preRelease.joinToString(".", prefix = "-")

    companion object {
        private val PATTERN = Regex(
            """^[vV]?(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)""" +
                """(?:-([0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*))?""" +
                """(?:\+[0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*)?$"""
        )

        @JvmStatic
        fun parse(text: String?): SemVer? {
            val match = PATTERN.matchEntire(text?.trim() ?: return null) ?: return null
            val (major, minor, patch, pre) = match.destructured
            return SemVer(
                major.toIntOrNull() ?: return null,
                minor.toIntOrNull() ?: return null,
                patch.toIntOrNull() ?: return null,
                if (pre.isEmpty()) emptyList() else pre.split('.'),
            )
        }

        private fun comparePreRelease(a: List<String>, b: List<String>): Int {
            if (a.isEmpty() && b.isEmpty()) return 0
            if (a.isEmpty()) return 1
            if (b.isEmpty()) return -1
            for (i in 0 until minOf(a.size, b.size)) {
                val c = compareIdentifier(a[i], b[i])
                if (c != 0) return c
            }
            return compareValues(a.size, b.size)
        }

        private fun compareIdentifier(x: String, y: String): Int {
            val xNumeric = x.all { it in '0'..'9' }
            val yNumeric = y.all { it in '0'..'9' }
            return when {
                xNumeric && yNumeric -> compareNumeric(x, y)
                xNumeric -> -1
                yNumeric -> 1
                else -> x.compareTo(y)
            }
        }

        /** Compares digit strings of any length without overflow. */
        private fun compareNumeric(x: String, y: String): Int {
            val a = x.trimStart('0').ifEmpty { "0" }
            val b = y.trimStart('0').ifEmpty { "0" }
            return if (a.length != b.length) compareValues(a.length, b.length) else a.compareTo(b)
        }
    }
}
