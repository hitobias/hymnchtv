package org.cog.hymnchtv.ui.lyrics

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.ViewCompat
import org.cog.hymnchtv.R

/** Draws a [PillModel] on the bottom capsule's three items; text colours and tint come from ChromeButtonStyle. */
class LyricsPillBinder(
    private val script: TextView,
    private val language: TextView,
    private val mode: TextView,
) {
    /**
     * @param modeLabel short name of the chosen mode (c_btn_mode_*); @param modeDescription full name for TalkBack
     */
    fun render(model: PillModel, colors: CapsuleColors, @StringRes modeLabel: Int, modeDescription: String) {
        val res = script.resources
        bindPair(script, model.script, res.getString(R.string.c_pill_trad), res.getString(R.string.c_pill_simp), colors)
        script.contentDescription = res.getString(if (model.script.enabled) R.string.c_cd_script else R.string.c_cd_score_only_na)

        bindPair(language, model.language, res.getString(R.string.c_pill_zh), res.getString(R.string.c_pill_en), colors)
        language.contentDescription = res.getString(
            when {
                model.language.enabled -> R.string.c_cd_cn_en
                model.script.enabled -> R.string.c_cd_no_english
                else -> R.string.c_cd_score_only_na
            },
        )

        mode.text = if (model.showModeText) res.getString(modeLabel) else ""
        mode.setCompoundDrawablesRelativeWithIntrinsicBounds(iconFor(model.modeIcon), 0, 0, 0)
        mode.contentDescription = res.getString(R.string.c_cd_mode, modeDescription)
    }

    private fun bindPair(view: TextView, item: PairItem, first: String, second: String, colors: CapsuleColors) {
        view.isEnabled = item.enabled
        val text = "$first$SEPARATOR$second"
        if (!item.enabled || item.active < 0) {
            view.text = text
            ViewCompat.setStateDescription(view, null)
            return
        }
        // TalkBack: the side in use is only shown by colour, so say it
        ViewCompat.setStateDescription(view, if (item.active == 0) first else second)
        val start = if (item.active == 0) 0 else first.length + SEPARATOR.length
        val end = if (item.active == 0) first.length else text.length
        view.text = SpannableString(text).apply {
            setSpan(ForegroundColorSpan(colors.active), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    @DrawableRes
    private fun iconFor(icon: ModeIcon): Int = when (icon) {
        ModeIcon.LYRICS -> R.drawable.ic_sym_article
        ModeIcon.SCORE -> R.drawable.ic_sym_queue_music
        ModeIcon.BOTH -> R.drawable.ic_sym_two_pager
    }

    private companion object {
        /** ASCII slash: the full-width one is not in the HymnalKai subset */
        const val SEPARATOR = "/"
    }
}
