package org.cog.hymnchtv.ui.lyrics

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.LeadingMarginSpan
import android.text.style.LineHeightSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.widget.TextView

/** What [LyricsTypography] needs to dress a lyrics text; sizes are pixels, colours ARGB. */
data class LyricsStyle(
    val accent: Int,
    val verseColor: Int,
    val mutedColor: Int,
    val numberPx: Int,
    val titlePx: Int,
    val captionPx: Int,
    val headerGapPx: Int,
    val barPx: Int,
    val lineMultiplier: Float,
    /** The view the text is shown in; the chorus indent follows its (user scaled) text size. */
    val textView: TextView,
)

/**
 * Lyrics page typography (1.2.0): a large hymn number over the book and title, small verse numbers, indented
 * chorus bodies with a thin accent bar, and tighter gaps between verses. Only recognised markers are touched
 * ([LyricsMeta]); text without markers is left plain.
 */
object LyricsTypography {
    /** A blank line between verses is shrunk to about 1.2 text lines (1.7 line spacing x this). */
    const val VERSE_GAP_SCALE = 0.7f
    const val VERSE_NUMBER_SCALE = 0.75f
    const val CHORUS_INDENT_EM = 1.5f
    const val CHORUS_BAR_ALPHA = 0.4f
    private const val HEADER_LINES = 2

    /** Verse numbers and chorus labels coloured and bold; the text itself is unchanged. */
    @JvmStatic
    fun applyVerseSpans(text: CharSequence, color: Int): CharSequence {
        val spanned = SpannableStringBuilder(text)
        markVerses(spanned, color, shrinkNumbers = false)
        return spanned
    }

    /** The full lyrics dressing; [bookName] becomes a caption line between the hymn number and the title. */
    @JvmStatic
    fun apply(text: CharSequence, bookName: String?, style: LyricsStyle): CharSequence {
        val out = SpannableStringBuilder(text)
        markVerses(out, style.verseColor, shrinkNumbers = true)
        LyricsMeta.chorusBodyRanges(text).forEach { r ->
            out.setSpan(ChorusSpan(style.textView, style.barPx, withAlpha(style.accent, CHORUS_BAR_ALPHA)), r.first, r.last + 1, SPAN)
        }
        LyricsMeta.blankLineRanges(text).forEach { r ->
            out.setSpan(RelativeSizeSpan(VERSE_GAP_SCALE), r.first, r.last + 1, SPAN)
        }
        dressHeader(out, bookName, style)
        return out
    }

    private fun markVerses(out: SpannableStringBuilder, color: Int, shrinkNumbers: Boolean) {
        val numbers = LyricsMeta.verseNumberRanges(out).toSet()
        LyricsMeta.verseMarkerRanges(out).forEach { r ->
            val end = r.last + 1
            out.setSpan(ForegroundColorSpan(color), r.first, end, SPAN)
            out.setSpan(StyleSpan(Typeface.BOLD), r.first, end, SPAN)
            if (shrinkNumbers && r in numbers) out.setSpan(RelativeSizeSpan(VERSE_NUMBER_SCALE), r.first, end, SPAN)
        }
    }

    /** Line 0 is the hymn number, line 1 the title; the blank line after them becomes the header gap. */
    private fun dressHeader(out: SpannableStringBuilder, bookName: String?, style: LyricsStyle) {
        val lines = out.toString().split('\n')
        if (lines.size <= HEADER_LINES) return
        val numberEnd = lines[0].length
        val titleStart = numberEnd + 1
        val titleEnd = titleStart + lines[1].length
        val gapStart = titleEnd + 1
        if (lines[2].isBlank()) {
            out.setSpan(FixedLineHeightSpan((style.headerGapPx / style.lineMultiplier).toInt()), gapStart, gapStart + 1, SPAN)
            out.getSpans(gapStart, gapStart + 1, RelativeSizeSpan::class.java).forEach { out.removeSpan(it) }
        }
        out.setSpan(AbsoluteSizeSpan(style.titlePx, false), titleStart, titleEnd, SPAN)
        out.setSpan(StyleSpan(Typeface.BOLD), titleStart, titleEnd, SPAN)
        out.setSpan(AbsoluteSizeSpan(style.numberPx, false), 0, numberEnd, SPAN)
        out.setSpan(ForegroundColorSpan(style.accent), 0, numberEnd, SPAN)
        out.setSpan(TypefaceSpan(LIGHT_FAMILY), 0, numberEnd, SPAN)
        if (!bookName.isNullOrBlank()) {
            out.insert(titleStart, "$bookName\n")
            val bookEnd = titleStart + bookName.length
            out.setSpan(AbsoluteSizeSpan(style.captionPx, false), titleStart, bookEnd, SPAN)
            out.setSpan(ForegroundColorSpan(style.mutedColor), titleStart, bookEnd, SPAN)
        }
    }

    private fun withAlpha(color: Int, alpha: Float): Int =
        (color and 0x00FFFFFF) or ((alpha * 255).toInt() shl 24)

    private const val SPAN = Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    private const val LIGHT_FAMILY = "sans-serif-light"
}

/** Indents a chorus body by 1.5 em of the view's current text size and draws a thin bar in the gutter. */
class ChorusSpan(
    private val view: TextView,
    private val barPx: Int,
    private val barColor: Int,
) : LeadingMarginSpan {
    override fun getLeadingMargin(first: Boolean): Int = (LyricsTypography.CHORUS_INDENT_EM * view.textSize).toInt()

    override fun drawLeadingMargin(
        c: Canvas, p: Paint, x: Int, dir: Int, top: Int, baseline: Int, bottom: Int,
        text: CharSequence, start: Int, end: Int, first: Boolean, layout: Layout,
    ) {
        val saved = p.color
        val style = p.style
        p.color = barColor
        p.style = Paint.Style.FILL
        val left = if (dir > 0) x.toFloat() else (x - barPx).toFloat()
        c.drawRect(left, top.toFloat(), left + barPx, bottom.toFloat(), p)
        p.color = saved
        p.style = style
    }
}

/** A line of exactly [heightPx] before the layout's line spacing multiplier is applied. */
class FixedLineHeightSpan(private val heightPx: Int) : LineHeightSpan {
    override fun chooseHeight(
        text: CharSequence, start: Int, end: Int, spanstartv: Int, lineHeight: Int, fm: Paint.FontMetricsInt,
    ) {
        fm.ascent = -heightPx
        fm.top = fm.ascent
        fm.descent = 0
        fm.bottom = 0
    }
}
