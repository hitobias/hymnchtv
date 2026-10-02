package org.cog.hymnchtv.update

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class Sha256FileTest {
    private val hex = "3095e51c68bb82552f8aaf30fa5a77a7fe6786c35952b984181f28d923aceccd"
    private val apk = "hymnal-1.1.0.apk"

    @Test
    fun acceptsExactShasumLine() {
        assertThat(Sha256File.parse("$hex  $apk\n", apk)).isEqualTo(hex)
        assertThat(Sha256File.parse("$hex  $apk", apk)).isEqualTo(hex)
        assertThat(Sha256File.parse("${hex.uppercase()}  $apk\n", apk)).isEqualTo(hex)
    }

    @Test
    fun rejectsOtherFileNames() {
        listOf("hymnal-1.0.0.apk", "hymnal-1.1.0.apk.sha256", "./$apk", "dist/$apk").forEach {
            assertThat(Sha256File.parse("$hex  $it\n", apk)).isNull()
        }
    }

    @Test
    fun rejectsWrongWhitespaceAndMarkers() {
        listOf("$hex $apk", "$hex   $apk", " $hex  $apk", "$hex  $apk ", "$hex *$apk", "$hex\t$apk", "$hex  $apk\r\n", hex)
            .forEach { assertThat(Sha256File.parse(it, apk)).isNull() }
    }

    @Test
    fun rejectsMultiLineAndTruncatedContent() {
        listOf(
            "$hex  $apk\n$hex  $apk\n", "\n$hex  $apk\n", "$hex  $apk\n\n",
            "${hex.dropLast(1)}  $apk", "$hex${"0"}  $apk", "${hex.take(32)}", null, "",
        ).forEach { assertThat(Sha256File.parse(it, apk)).isNull() }
    }

    @Test
    fun reconcileRequiresFileHashAndMatchingDigest() {
        assertThat(Sha256File.reconcile(hex, null)).isEqualTo(hex)
        assertThat(Sha256File.reconcile(hex, hex)).isEqualTo(hex)
        assertThat(Sha256File.reconcile(hex, "0".repeat(64))).isNull()
        assertThat(Sha256File.reconcile(null, hex)).isNull()
    }
}
