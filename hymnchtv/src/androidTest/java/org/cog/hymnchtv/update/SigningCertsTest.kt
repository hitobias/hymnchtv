package org.cog.hymnchtv.update

import android.os.Build
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Certificate extraction on a real device. A differently signed archive is checked manually in Task 13. */
@RunWith(AndroidJUnit4::class)
class SigningCertsTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun installedAppHasExactlyOneSigner() {
        val installed = SigningCerts.installedApp(context)
        assertThat(installed.signers).hasSize(1)
        installed.signers.forEach { assertThat(it).matches("[0-9a-f]{64}") }
    }

    /** Our own installed APK file, read as an archive, must carry the same signer and package. */
    @Test
    fun ownApkReadAsArchiveMatchesInstalledSigner() {
        val archive = SigningCerts.archive(context, File(context.applicationInfo.sourceDir))
        assertThat(archive.packageName).isEqualTo(context.packageName)
        Log.i("SigningCertsTest", "API ${Build.VERSION.SDK_INT}: archive signers = ${archive.signers}")
        // Hard requirement on every API level (incl. 24–27 via GET_SIGNATURES): otherwise updates fail closed.
        assertThat(archive.signers).isNotEmpty()
        assertThat(archive.signers).isEqualTo(SigningCerts.installedApp(context).signers)
    }

    /** The trust decision on this device: our own APK is trusted as far as the signer is concerned. */
    @Test
    fun trustDecisionAcceptsOwnSigner() {
        val installed = SigningCerts.installedApp(context)
        val archive = SigningCerts.archive(context, File(context.applicationInfo.sourceDir))
        assertThat(ApkTrust.isSameSigner(installed.signers, archive.signers.orEmpty(), archive.signerHistory)).isTrue()
    }
}
