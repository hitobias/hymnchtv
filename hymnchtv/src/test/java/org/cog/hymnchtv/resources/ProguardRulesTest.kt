package org.cog.hymnchtv.resources

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

/** R8 (1.6.0): what must survive minification, and resource shrinking stays off (MIDI is looked up by name). */
class ProguardRulesTest {
    private val rules = File("proguard-rules.pro").readText()
    private val gradle = File("build.gradle").readText()

    @Test
    fun releaseMinifiesButNeverShrinksResources() {
        val release = Regex("""release \{(.*?)\n        \}""", RegexOption.DOT_MATCHES_ALL).find(gradle)!!.groupValues[1]
        assertThat(release).contains("minifyEnabled = true")
        assertThat(release).contains("shrinkResources = false")
        assertThat(gradle).doesNotContain("shrinkResources = true")
    }

    @Test
    fun theRulesKeepWhatIsFoundByName() {
        assertThat(rules).contains("-keepattributes SourceFile,LineNumberTable")
        assertThat(rules).doesNotContain("-renamesourcefileattribute")
        assertThat(rules).contains("-keep class * extends androidx.fragment.app.Fragment { public <init>(); }")
        assertThat(rules).contains("-keep class net.duguying.pinyin.** { *; }")
        assertThat(rules).contains("org.cog.hymnchtv.service.androidupdate.UpdateCheckWorker")
        assertThat(rules).contains("YouTubePlayerBridge")
    }
}
