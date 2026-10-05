package org.cog.hymnchtv.resources

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

/**
 * R8 moves R into the default package, where Class.getPackage() is null: a resource lookup that asks R's package for its
 * name crashes the minified build (1.6.0). Use the app's package name or a constant namespace instead.
 */
class NoClassPackageLookupTest {
    @Test
    fun noMainSourceAsksAClassForItsPackage() {
        val offenders = File("src/main").walkTopDown()
            .filter { it.isFile && (it.extension == "java" || it.extension == "kt") }
            .filter { it.readText().contains("getPackage()") || it.readText().contains("java.`package`") }
            .map { it.path }.toList()
        assertThat(offenders).isEmpty()
    }
}
