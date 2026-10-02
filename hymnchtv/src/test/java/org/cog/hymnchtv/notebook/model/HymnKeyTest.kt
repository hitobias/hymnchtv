package org.cog.hymnchtv.notebook.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HymnKeyTest {
    private fun throwsIae(block: () -> Unit) =
        assertThat(runCatching(block).exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)

    @Test
    fun ofDerivesFuOnlyForDbAboveOffset() {
        assertThat(HymnKey.of(HymnTypes.DB, 780).isFu).isFalse()
        assertThat(HymnKey.of(HymnTypes.DB, 781).isFu).isTrue()
        assertThat(HymnKey.of(HymnTypes.DB, 786).isFu).isTrue()
        assertThat(HymnKey.of(HymnTypes.YB, 276).isFu).isFalse()
        assertThat(HymnKey.of(HymnTypes.BB, 1005).isFu).isFalse()
    }

    @Test
    fun rangesFollowHymnNoValidate() {
        val valid = listOf(
            HymnTypes.DB to 1, HymnTypes.DB to 786, HymnTypes.BB to 37, HymnTypes.BB to 101, HymnTypes.BB to 1005,
            HymnTypes.ER to 17, HymnTypes.ER to 101, HymnTypes.ER to 1232, HymnTypes.XB to 167, HymnTypes.XB to 171,
            HymnTypes.XG to 33, HymnTypes.XG to 35, HymnTypes.XG to 206, HymnTypes.YB to 277,
        )
        val invalid = listOf(
            HymnTypes.DB to 0, HymnTypes.DB to 787, HymnTypes.BB to 38, HymnTypes.BB to 100, HymnTypes.BB to 1006,
            HymnTypes.BB to 2000, HymnTypes.ER to 18, HymnTypes.ER to 1233, HymnTypes.XB to 168, HymnTypes.XB to 170,
            HymnTypes.XB to 172, HymnTypes.XG to 34, HymnTypes.XG to 207, HymnTypes.YB to 278, "x" to 1,
        )
        valid.forEach { (type, no) -> assertThat(HymnKey.isValid(type, no)).isTrue() }
        invalid.forEach { (type, no) ->
            assertThat(HymnKey.isValid(type, no)).isFalse()
            assertThat(HymnKey.ofOrNull(type, no)).isNull()
            throwsIae { HymnKey.of(type, no) }
        }
        assertThat(HymnKey.ofOrNull(null, 1)).isNull()
    }

    @Test
    fun nonCanonicalFuFlagIsRejected() {
        throwsIae { HymnKey(HymnTypes.DB, 781, false) }
        throwsIae { HymnKey(HymnTypes.DB, 1, true) }
        throwsIae { HymnKey(HymnTypes.YB, 276, true) }
        assertThat(HymnKey.isCanonical(HymnTypes.DB, 781, false)).isFalse()
        assertThat(HymnKey.isCanonical(HymnTypes.DB, 781, true)).isTrue()
    }

    @Test
    fun equalityIsByValue() {
        assertThat(HymnKey.of(HymnTypes.ER, 12)).isEqualTo(HymnKey(HymnTypes.ER, 12, false))
    }
}
