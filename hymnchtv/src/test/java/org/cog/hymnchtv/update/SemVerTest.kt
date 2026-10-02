package org.cog.hymnchtv.update

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Random

class SemVerTest {
    private fun v(text: String) = checkNotNull(SemVer.parse(text)) { "unparsable: $text" }

    @Test
    fun parsesPlainAndTaggedVersions() {
        assertThat(SemVer.parse("2.9.2")).isEqualTo(SemVer(2, 9, 2))
        assertThat(SemVer.parse("v2.10.0")).isEqualTo(SemVer(2, 10, 0))
        assertThat(SemVer.parse("V2.10.0")).isEqualTo(SemVer(2, 10, 0))
        assertThat(SemVer.parse(" 2.10.0\n")).isEqualTo(SemVer(2, 10, 0))
    }

    @Test
    fun parsesPreReleaseAndIgnoresBuildMetadata() {
        assertThat(SemVer.parse("2.10.0-beta.1")).isEqualTo(SemVer(2, 10, 0, listOf("beta", "1")))
        assertThat(SemVer.parse("2.10.0+20261020")).isEqualTo(SemVer(2, 10, 0))
        assertThat(SemVer.parse("2.10.0-rc.1+abc")).isEqualTo(SemVer(2, 10, 0, listOf("rc", "1")))
    }

    @Test
    fun rejectsInvalidText() {
        listOf(
            null, "", "  ", "v", "2.10", "2.10.0.1", "02.1.0", "2.x.0", "2.10.0-",
            "2.10.0-beta..1", "99999999999.0.0", "release-2.10.0",
        ).forEach { assertThat(SemVer.parse(it)).isNull() }
    }

    @Test
    fun comparesNumericallyNotLexically() {
        assertThat(v("2.10.0")).isGreaterThan(v("2.9.2"))
        assertThat(v("2.9.10")).isGreaterThan(v("2.9.9"))
        assertThat(v("3.0.0")).isGreaterThan(v("2.99.99"))
        assertThat(v("10.0.0")).isGreaterThan(v("9.9.9"))
    }

    @Test
    fun releaseOutranksItsPreReleases() {
        assertThat(v("2.10.0")).isGreaterThan(v("2.10.0-rc.1"))
        assertThat(v("2.10.0-rc.1")).isGreaterThan(v("2.9.2"))
    }

    @Test
    fun followsSemverSpecPrecedenceExample() {
        val ordered = listOf(
            "1.0.0-alpha", "1.0.0-alpha.1", "1.0.0-alpha.beta", "1.0.0-beta",
            "1.0.0-beta.2", "1.0.0-beta.11", "1.0.0-rc.1", "1.0.0",
        ).map(::v)
        assertThat(ordered.shuffled(Random(7)).sorted()).containsExactlyElementsIn(ordered).inOrder()
        ordered.zipWithNext().forEach { (lower, higher) -> assertThat(lower).isLessThan(higher) }
    }

    @Test
    fun buildMetadataDoesNotAffectEquality() {
        assertThat(v("v2.10.0").compareTo(v("2.10.0+build.5"))).isEqualTo(0)
        assertThat(v("v2.10.0")).isEqualTo(v("2.10.0+build.5"))
    }

    @Test
    fun toStringIsCanonical() {
        assertThat(v("v2.10.0").toString()).isEqualTo("2.10.0")
        assertThat(v("2.10.0-rc.1+x").toString()).isEqualTo("2.10.0-rc.1")
    }
}
