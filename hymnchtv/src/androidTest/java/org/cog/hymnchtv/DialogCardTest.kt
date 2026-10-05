package org.cog.hymnchtv

import android.content.Intent
import android.graphics.Rect
import android.view.View
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.about.HelpActivity
import org.cog.hymnchtv.about.LicensesActivity
import org.cog.hymnchtv.utils.DialogActivity
import org.junit.Test
import org.junit.runner.RunWith

/** Spec rev 3 section 5: the shared dialog and About are cards with full-width buttons, primary at the bottom. */
@RunWith(AndroidJUnit4::class)
class DialogCardTest {
    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun rect(v: View) = Rect().also { v.getGlobalVisibleRect(it) }

    private fun dialogIntent(confirm: String?): Intent =
        DialogActivity.getDialogIntent(ctx, "Title", "Message").apply {
            putExtra(DialogActivity.EXTRA_CANCELABLE, true)
            if (confirm != null) putExtra(DialogActivity.EXTRA_CONFIRM_TXT, confirm)
        }

    @Test
    @QuickTest
    fun confirmDialogStacksTonalAbovePrimaryAtFullWidth() {
        ActivityScenario.launch<DialogActivity>(dialogIntent("Update")).use { s ->
            s.onActivity { a ->
                val ok = a.findViewById<View>(R.id.okButton)
                val cancel = a.findViewById<View>(R.id.cancelButton)
                val content = a.findViewById<View>(R.id.alertContent)
                assertThat(a.findViewById<TextView>(R.id.alertTitle).text.toString()).isEqualTo("Title")
                assertThat(cancel.visibility).isEqualTo(View.VISIBLE)
                assertThat(rect(cancel).bottom).isAtMost(rect(ok).top)          // primary at the bottom
                assertThat(ok.width).isEqualTo(content.width)                    // full width
                assertThat(cancel.width).isEqualTo(content.width)
                val screen = Rect(0, 0, a.resources.displayMetrics.widthPixels, a.resources.displayMetrics.heightPixels)
                assertThat(screen.contains(rect(ok))).isTrue()
            }
        }
    }

    @Test
    fun buttonsSitTwentyDpInsideTheCard() {
        ActivityScenario.launch<DialogActivity>(dialogIntent("Update")).use { s ->
            s.onActivity { a ->
                val card = Rect().also { a.window.decorView.findViewById<View>(android.R.id.content).getGlobalVisibleRect(it) }
                val ok = rect(a.findViewById(R.id.okButton))
                val edge = (16 * a.resources.displayMetrics.density + 0.5f).toInt()
                val screenW = a.resources.displayMetrics.widthPixels
                assertThat(card.left).isIn(com.google.common.collect.Range.closed(edge - 1, edge + 1))
                assertThat(screenW - card.right).isIn(com.google.common.collect.Range.closed(edge - 1, edge + 1))
                val inset = (20 * a.resources.displayMetrics.density + 0.5f).toInt()
                assertThat(ok.left - card.left).isIn(com.google.common.collect.Range.closed(inset - 1, inset + 1))
                assertThat(card.right - ok.right).isIn(com.google.common.collect.Range.closed(inset - 1, inset + 1))
            }
        }
    }

    @Test
    fun aboutHelpAndLicencesOpenTheirPagesAndOkCloses() {
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        for ((id, target) in listOf(R.id.about_help to HelpActivity::class.java.name, R.id.about_licenses to LicensesActivity::class.java.name)) {
            val monitor = instrumentation.addMonitor(target, null, true)
            ActivityScenario.launch(About::class.java).use { s ->
                s.onActivity { it.findViewById<View>(id).performClick() }
                // block = true swallows the launch, so the returned Activity is null by design; the hit count is the signal
                instrumentation.waitForMonitorWithTimeout(monitor, 5_000)
                assertThat(monitor.hits).isEqualTo(1)
            }
            instrumentation.removeMonitor(monitor)
        }
        ActivityScenario.launch(About::class.java).use { s ->
            s.onActivity { it.findViewById<View>(R.id.ok_button).performClick() }
            instrumentation.waitForIdleSync()
            assertThat(s.state).isEqualTo(androidx.lifecycle.Lifecycle.State.DESTROYED)
        }
    }

    @Test
    @QuickTest
    fun longMessageScrollsAndKeepsTheButtonsOnScreen() {
        val long = (1..200).joinToString("\n") { "Line $it of a very long release note" }
        val intent = DialogActivity.getDialogIntent(ctx, "Title", long).apply {
            putExtra(DialogActivity.EXTRA_CANCELABLE, true)
            putExtra(DialogActivity.EXTRA_CONFIRM_TXT, "Update")
        }
        ActivityScenario.launch<DialogActivity>(intent).use { s ->
            s.onActivity { a ->
                val screen = Rect(0, 0, a.resources.displayMetrics.widthPixels, a.resources.displayMetrics.heightPixels)
                // Real layout bounds: getGlobalVisibleRect is clipped to the window and would hide an overflowing button
                fun bounds(v: View) = IntArray(2).also { v.getLocationOnScreen(it) }.let { Rect(it[0], it[1], it[0] + v.width, it[1] + v.height) }
                assertThat(screen.contains(bounds(a.findViewById(R.id.okButton)))).isTrue()
                assertThat(screen.contains(bounds(a.findViewById(R.id.cancelButton)))).isTrue()
                val scroll = a.findViewById<View>(R.id.alertScroll)
                assertThat(scroll.height).isAtMost((a.resources.displayMetrics.heightPixels * 0.45f).toInt() + 1)
                assertThat(scroll.canScrollVertically(1)).isTrue()
            }
        }
    }

    @Test
    fun messageDialogHasOnlyThePrimaryButton() {
        ActivityScenario.launch<DialogActivity>(dialogIntent(null)).use { s ->
            s.onActivity { a ->
                assertThat(a.findViewById<View>(R.id.cancelButton).visibility).isEqualTo(View.GONE)
                assertThat(a.findViewById<View>(R.id.okButton).isShown).isTrue()
            }
        }
    }

    @Test
    @QuickTest
    fun aboutKeepsAllSixFunctionsReachable() {
        ActivityScenario.launch(About::class.java).use { s ->
            s.onActivity { a ->
                listOf(R.id.about_help, R.id.about_licenses, R.id.history_log, R.id.submit_logs, R.id.ok_button).forEach { id ->
                    val v = a.findViewById<View>(id)
                    assertThat(v.isShown).isTrue()
                    assertThat(v.isClickable).isTrue()
                    assertThat(v.hasOnClickListeners()).isTrue()
                }
                val update = a.findViewById<View>(R.id.check_new_version)
                assertThat(update.hasOnClickListeners()).isTrue()
                if (BuildConfig.DEBUG) assertThat(update.isShown).isTrue()
                assertThat(rect(update).bottom).isAtMost(rect(a.findViewById(R.id.ok_button)).top)
            }
        }
    }
}
