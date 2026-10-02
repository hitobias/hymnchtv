package org.cog.hymnchtv.notebook.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EnumParsingTest {
    @Test
    fun occasionParsesStoredNames() {
        Occasion.entries.forEach { assertThat(Occasion.fromStorage(it.name)).isEqualTo(it) }
    }

    @Test
    fun occasionReturnsNullForUnknown() {
        listOf(null, "", "lords_day", "主日", "SUNDAY").forEach {
            assertThat(Occasion.fromStorage(it)).isNull()
        }
    }

    @Test
    fun sourceParsesStoredNames() {
        assertThat(SingSource.fromStorage("AUTO")).isEqualTo(SingSource.AUTO)
        assertThat(SingSource.fromStorage("MANUAL")).isEqualTo(SingSource.MANUAL)
        assertThat(SingSource.fromStorage("auto")).isNull()
        assertThat(SingSource.fromStorage(null)).isNull()
    }
}
