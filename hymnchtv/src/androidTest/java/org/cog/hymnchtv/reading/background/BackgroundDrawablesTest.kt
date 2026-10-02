package org.cog.hymnchtv.reading.background

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.widget.ImageView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Run on API 24 and 34 (Task V1): every generated layer-list and vector must inflate and draw on both. */
@RunWith(AndroidJUnit4::class)
class BackgroundDrawablesTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val ctx: Context = instrumentation.targetContext
    private val prefs = ctx.getSharedPreferences("BackgroundDrawablesTest", Context.MODE_PRIVATE)

    @After
    fun clearPrefs() {
        prefs.edit().clear().commit()
    }

    @Test
    fun everyPresetInflatesAndDraws() {
        for (preset in BackgroundPreset.entries) {
            val drawable = BackgroundDrawables.create(ctx, preset)
            val bitmap = Bitmap.createBitmap(270, 600, Bitmap.Config.ARGB_8888)
            drawable.setBounds(0, 0, bitmap.width, bitmap.height)
            drawable.draw(Canvas(bitmap))
            val centre = bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)
            assertWithMessage("${preset.id} centre pixel").that(Color.alpha(centre)).isEqualTo(255)
            bitmap.recycle()
        }
    }

    @Test
    fun unreadablePhotoFallsBackToThePreset() {
        val junk = File(ctx.cacheDir, "not-an-image.jpg").apply { writeText("not an image") }
        lateinit var applied: BackgroundChoice
        lateinit var view: ImageView
        instrumentation.runOnMainSync {
            view = ImageView(ctx)
            applied = BackgroundApplier.apply(view, BackgroundChoice.Photo, prefs, junk, BackgroundPreset.XUAN)
        }
        assertThat(applied).isEqualTo(BackgroundChoice.Preset(BackgroundPreset.XUAN))
        assertThat(view.drawable).isNull()
        assertThat(view.background).isNotNull()
    }

    @Test
    fun switchingFromPhotoToPresetClearsBlur() {
        val photo = File(ctx.cacheDir, "photo.png")
        val source = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        photo.outputStream().use { source.compress(Bitmap.CompressFormat.PNG, 100, it) }
        prefs.edit().putInt(PhotoBackground.PREF_BLUR, 10).commit()
        lateinit var view: ImageView
        var blurredRadius = 0f
        instrumentation.runOnMainSync {
            view = ImageView(ctx)
            BackgroundApplier.apply(view, BackgroundChoice.Photo, prefs, photo, BackgroundPreset.XUAN)
            blurredRadius = BackgroundApplier.appliedBlurRadius(view)
            BackgroundApplier.apply(view, BackgroundChoice.Preset(BackgroundPreset.MIST), prefs, photo, BackgroundPreset.XUAN)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            assertThat(blurredRadius).isGreaterThan(0f)
        }
        assertThat(BackgroundApplier.appliedBlurRadius(view)).isEqualTo(0f)
        assertThat(view.drawable).isNull()
        assertThat(view.colorFilter).isNull()
    }

    @Test
    fun photoFileIsTheImporterPath() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val stored = PhotoBackgroundImporter.photoFileIn(ctx.filesDir)
        stored.parentFile?.mkdirs()
        stored.writeBytes(ByteArray(1))
        try {
            assertThat(BackgroundPrefs.photoFile(prefs)).isEqualTo(stored)
        } finally {
            stored.delete()
        }
        assertThat(BackgroundPrefs.photoFile(prefs)).isNull()
    }
}
