package org.cog.hymnchtv.ui.lyrics

import android.view.View
import android.webkit.WebView
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.ContentHandler
import org.cog.hymnchtv.R
import org.junit.Test
import org.junit.runner.RunWith

/** 1.6.0: the in-app web layer reuses its fragment (no back stack growth); the dead search types are gone. */
@RunWith(AndroidJUnit4::class)
class WebLayerTest : LyricsTestBase() {
    @Test
    fun openingTheWebLayerTwiceAddsNoBackStackEntries() {
        launch().use { s ->
            s.onActivity { it.initWebView(ContentHandler.UrlType.hymnBibleTool, "about:blank") }
            s.await("the web layer", 10_000) { a ->
                a.findViewById<View>(R.id.webView).visibility == View.VISIBLE &&
                    a.supportFragmentManager.findFragmentById(R.id.webView)?.view?.findViewById<WebView>(R.id.webview)?.url != null
            }
            val first = s.read { it.supportFragmentManager.findFragmentById(R.id.webView) }
            s.onActivity { it.initWebView(ContentHandler.UrlType.hymnBibleTool, "about:blank") }
            instrumentation.waitForIdleSync()
            s.read { a ->
                assertThat(a.supportFragmentManager.backStackEntryCount).isEqualTo(0)
                assertThat(a.supportFragmentManager.findFragmentById(R.id.webView)).isSameInstanceAs(first)
            }
        }
    }

    @Test
    fun aBibleToolLinkWithoutAUrlIsRefusedWithoutACrash() {
        launch().use { s ->
            s.onActivity { it.initWebView(ContentHandler.UrlType.hymnBibleTool) }
            instrumentation.waitForIdleSync()
            s.read { a ->
                assertThat(a.findViewById<View>(R.id.webView).visibility).isNotEqualTo(View.VISIBLE)
                assertThat(a.supportFragmentManager.findFragmentById(R.id.webView)).isNull()
            }
        }
    }

    @Test
    fun theUnusedSearchTypesAreGone() {
        assertThat(ContentHandler.UrlType.values().map { it.name }).containsNoneOf("hymnGoogleSearch", "hymnQqSearch")
    }
}
