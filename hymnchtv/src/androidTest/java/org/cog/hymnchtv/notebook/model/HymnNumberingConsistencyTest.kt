package org.cog.hymnchtv.notebook.model

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.utils.HymnNoValidate
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HymnNumberingConsistencyTest {
    @Test
    fun tablesMirrorHymnNoValidate() {
        assertThat(HymnNumbering.BB_LIMITS.toList()).isEqualTo(HymnNoValidate.rangeBbLimit.toList())
        assertThat(HymnNumbering.ER_LIMITS.toList()).isEqualTo(HymnNoValidate.rangeErLimit.toList())
        assertThat(HymnNumbering.XB_INVALID).isEqualTo(HymnNoValidate.rangeXbInvalid.toSet())
        assertThat(HymnNumbering.XG_INVALID).isEqualTo(HymnNoValidate.rangeXgInvalid.toSet())
    }

    @Test
    fun gapsMatchTheLegacyRangesNumberByNumber() {
        (1..2100).forEach { n ->
            val bbLegacy = n <= HymnNoValidate.HYMN_BB_NO_MAX && HymnNoValidate.rangeBbInvalid.none { it.contains(n) }
            val erLegacy = n <= HymnNoValidate.HYMN_ER_NO_MAX && HymnNoValidate.rangeErInvalid.none { it.contains(n) }
            assertThat(HymnNumbering.isValid(HymnTypes.BB, n)).isEqualTo(bbLegacy)
            assertThat(HymnNumbering.isValid(HymnTypes.ER, n)).isEqualTo(erLegacy)
        }
    }
}
