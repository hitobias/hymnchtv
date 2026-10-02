package org.cog.hymnchtv.ui.picker

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.widget.TextView
import org.cog.hymnchtv.reading.LyricsTypefaces
import java.util.Locale

/**
 * HymnalKai (the lyrics Kai face) for the home screen's book names and hymn titles (spec 3). The face is loaded off the
 * main thread; until then, and for any text it cannot draw, the system font stays so no missing-glyph box ever shows.
 */
object KaiText {
    /** Whether the interface (not the lyrics) is in Traditional Chinese, which picks the TC face for book names. */
    @JvmStatic
    fun isTraditionalUi(locale: Locale): Boolean =
        locale.language == "zh" && (locale.script == "Hant" || locale.country in TRADITIONAL_REGIONS)

    @JvmStatic
    fun isChineseUi(locale: Locale): Boolean = locale.language == "zh"

    /** Every printable code point of [text] has a glyph in the face; checked one code point at a time. */
    @JvmStatic
    fun canDraw(text: CharSequence, hasGlyph: (String) -> Boolean): Boolean {
        var i = 0
        val s = text.toString()
        while (i < s.length) {
            val cp = s.codePointAt(i)
            i += Character.charCount(cp)
            if (Character.isWhitespace(cp) || Character.isISOControl(cp)) continue
            if (!hasGlyph(String(Character.toChars(cp)))) return false
        }
        return true
    }

    /** The face to use for [text]: [kai] only when it can draw all of it, else null (keep the system font). */
    @JvmStatic
    fun <T : Any> faceFor(text: CharSequence, kai: T?, hasGlyph: (T, String) -> Boolean): T? =
        kai?.takeIf { face -> canDraw(text) { hasGlyph(face, it) } }

    private fun paintHasGlyph(face: Typeface, glyph: String): Boolean = Paint().apply { typeface = face }.hasGlyph(glyph)

    /**
     * Shows the view's current text in Kai when the face is loaded and complete for it, else in the system font. When the
     * face is not loaded yet, requests it and re-applies once it arrives (the text may have changed by then: it is read again).
     */
    @JvmStatic
    fun apply(view: TextView, traditional: Boolean, bold: Boolean) {
        val style = if (bold) Typeface.BOLD else Typeface.NORMAL
        val kai = LyricsTypefaces.peek(traditional)
        if (kai == null) {
            view.setTypeface(Typeface.DEFAULT, style)
            LyricsTypefaces.request(view.context, traditional) { if (view.isAttachedToWindow) apply(view, traditional, bold) }
            return
        }
        view.setTypeface(faceFor(view.text, kai, ::paintHasGlyph) ?: Typeface.DEFAULT, style)
    }

    /** [apply] for text that should be Kai only in a Chinese interface (English labels stay in the system font). */
    @JvmStatic
    fun applyForUi(view: TextView, context: Context, bold: Boolean) {
        val locale = context.resources.configuration.locales[0]
        if (isChineseUi(locale)) apply(view, isTraditionalUi(locale), bold) else view.setTypeface(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    private val TRADITIONAL_REGIONS = setOf("TW", "HK", "MO")
}
