package org.cog.hymnchtv.about

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AboutLibrariesJsonTest {
    private val sample = checkNotNull(javaClass.getResource("/about/aboutlibraries_sample.json")).readText()
    private val rows = AboutLibrariesJson.parse(sample)

    @Test
    fun parsesEveryLibrary() {
        assertThat(rows).hasSize(5)
    }

    @Test
    fun pinnedProjectEntriesComeFirstThenAlphabetical() {
        assertThat(rows.map { it.id }).containsExactly(
            "org.cog:hymnchtv",
            "org.byvoid:opencc-data",
            "org.jetbrains:annotations",
            "javax.annotation:javax.annotation-api",
            "org.jetbrains.kotlin:kotlin-stdlib",
        ).inOrder()
    }

    @Test
    fun originalProjectCarriesApacheNoticeAndText() {
        val original = rows.first()
        assertThat(original.description).contains("Copyright 2020 Eng Chong Meng")
        assertThat(original.licenses.single().name).isEqualTo("Apache License 2.0")
        assertThat(original.licenses.single().content).startsWith("Apache License")
    }

    @Test
    fun resolvesHashKeyedLicensesAndKeepsUrlWhenTextIsMissing() {
        val javax = rows.first { it.id == "javax.annotation:javax.annotation-api" }
        assertThat(javax.licenses.map { it.name }).containsExactly("Other", "CDDL + GPLv2 with classpath exception").inOrder()
        assertThat(javax.licenses[1].content).isNull()
        assertThat(javax.licenses[1].url).isEqualTo("https://github.com/javaee/javax.annotation/blob/master/LICENSE")
    }

    @Test
    fun unknownLicenseKeyFallsBackToTheKey() {
        val json = """{"libraries":[{"uniqueId":"a:b","name":"B","licenses":["MPL-2.0"]}],"licenses":{}}"""
        assertThat(AboutLibrariesJson.parse(json).single().licenses.single().name).isEqualTo("MPL-2.0")
    }

    @Test
    fun toleratesMissingOptionalFields() {
        val json = """{"libraries":[{"uniqueId":"a:b"}]}"""
        val row = AboutLibrariesJson.parse(json).single()
        assertThat(row.name).isEqualTo("a:b")
        assertThat(row.version).isNull()
        assertThat(row.licenses).isEmpty()
    }

    @Test
    fun malformedInputGivesEmptyList() {
        listOf(null, "", "{", "[]", """{"libraries":"x"}""").forEach { assertThat(AboutLibrariesJson.parse(it)).isEmpty() }
    }
}
