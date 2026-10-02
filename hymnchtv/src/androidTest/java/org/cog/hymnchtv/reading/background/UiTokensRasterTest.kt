package org.cog.hymnchtv.reading.background

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Visual redesign spec 4: the tokens must hold on the pixels each background really draws, not only on the swatch
 * colours. Every preset's drawable is rasterised at 108 x 192 and EVERY pixel gets the same compositing the UI uses
 * (surface, tone key, tone button on the top bar, disabled surface) before each token pair is checked.
 */
@RunWith(AndroidJUnit4::class)
class UiTokensRasterTest {
    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext

    private fun hex(c: Int) = "#%08x".format(c)

    private class Failures {
        val messages = mutableListOf<String>()
        fun check(ok: Boolean, what: () -> String) {
            if (!ok && messages.size < MAX_MESSAGES) messages += what()
            if (!ok) count++
        }
        var count = 0
        private companion object { const val MAX_MESSAGES = 5 }
    }

    private fun rasterise(preset: BackgroundPreset): Bitmap {
        val bitmap = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLACK)
        val drawable = BackgroundDrawables.create(ctx, preset)
        drawable.setBounds(0, 0, W, H)
        drawable.draw(Canvas(bitmap))
        return bitmap
    }

    @Test
    fun everyPixelOfEveryPresetMeetsEveryTokenThreshold() {
        for (preset in BackgroundPreset.entries) {
            val t = UiTokens.from(BackgroundPolicy.tokenInput(BackgroundChoice.Preset(preset)))
            val bitmap = rasterise(preset)
            val f = Failures()
            for (y in 0 until H) for (x in 0 until W) {
                val p = bitmap.getPixel(x, y)
                f.check(Color.alpha(p) == 255) { "($x,$y) not opaque ${hex(p)}" }
                val card = UiTokens.over(p, t.surface)
                val backdrops = UiTokens.backdropsOver(p, t.surface, t.surfaceTone)
                backdrops.forEach { b ->
                    f.check(Wcag.contrast(t.onSurface, b) >= 4.5) { "($x,$y) onSurface on ${hex(b)} = ${Wcag.contrast(t.onSurface, b)}" }
                    f.check(Wcag.contrast(UiTokens.over(b, t.onSurfaceMuted), b) >= 4.5) { "($x,$y) muted on ${hex(b)}" }
                    f.check(Wcag.contrast(t.accent, b) >= 3.0) { "($x,$y) accent on ${hex(b)} = ${Wcag.contrast(t.accent, b)}" }
                    f.check(Wcag.contrast(UiTokens.over(b, t.outline), b) >= 3.0) { "($x,$y) outline on ${hex(b)}" }
                }
                f.check(Wcag.contrast(t.onAccent, t.accent) >= 4.5) { "onAccent on accent" }
                f.check(Wcag.contrast(t.onOutlineAction, card) >= 4.5) { "($x,$y) onOutlineAction on ${hex(card)} = ${Wcag.contrast(t.onOutlineAction, card)}" }
                val disabled = UiTokens.over(p, t.disabledSurface)
                listOf(disabled, UiTokens.over(disabled, t.disabledSurface)).forEach { d ->
                    f.check(Wcag.contrast(t.disabledOnSurface, d) >= 3.0) { "($x,$y) disabled text on ${hex(d)} = ${Wcag.contrast(t.disabledOnSurface, d)}" }
                }
            }
            bitmap.recycle()
            assertWithMessage("${preset.id}: ${f.count} failing checks, e.g. ${f.messages}").that(f.count).isEqualTo(0)
        }
    }

    private companion object {
        const val W = 108
        const val H = 192
    }
}
