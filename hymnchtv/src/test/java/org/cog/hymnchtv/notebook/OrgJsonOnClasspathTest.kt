package org.cog.hymnchtv.notebook

import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Test

/** BackupCodec relies on a real org.json in JVM tests; android.jar only ships stubs that throw. */
class OrgJsonOnClasspathTest {
    @Test
    fun realOrgJsonIsUsedInJvmTests() {
        val encoded = JSONObject().put("a", JSONObject.NULL).put("b", 1L).toString()
        val parsed = JSONObject(encoded)
        assertThat(parsed.isNull("a")).isTrue()
        assertThat(parsed.getLong("b")).isEqualTo(1L)
    }
}
