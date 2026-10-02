package org.cog.hymnchtv.ui.lyrics

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.view.View
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.widget.CompoundButtonCompat
import org.cog.hymnchtv.R
import org.cog.hymnchtv.reading.background.UiTokens

/**
 * Paints the lyrics page player card from [UiTokens] (visual redesign spec section 6): `surface` card, source
 * selector as segmented buttons (unselected `surfaceTone`/`onSurface`, selected `accent`/`onAccent`), checkboxes and
 * speed in `onSurface`, seek bar `accent` over an `onSurfaceMuted` track at alpha 0.3.
 */
object PlayerCardStyle {
    private const val CARD_RADIUS_DP = 16f
    private const val CHIP_RADIUS_DP = 12f
    private const val TRACK_ALPHA = 0x4D // 0.3

    private val SOURCE_IDS = intArrayOf(R.id.btn_media, R.id.btn_jiaochang, R.id.btn_changshi, R.id.btn_banzhou)

    /** @param sourceAvailable whether media exists per source (media, jiaochang, changshi, banzhou); missing media reads as disabled */
    @JvmStatic
    fun apply(card: View, tokens: UiTokens, sourceAvailable: BooleanArray) {
        val density = card.resources.displayMetrics.density
        card.background = GradientDrawable().apply {
            setColor(tokens.surface)
            cornerRadius = CARD_RADIUS_DP * density
        }
        card.findViewById<View>(R.id.player_card)?.background = null

        card.findViewById<TextView>(R.id.hymn_info).setTextColor(tokens.onSurface)
        card.findViewById<TextView>(R.id.playback_position).setTextColor(tokens.onSurfaceMuted)
        card.findViewById<TextView>(R.id.playback_duration).setTextColor(tokens.onSurfaceMuted)
        card.findViewById<EditText>(R.id.repeatCount).setTextColor(tokens.onSurface)
        tintCheck(card.findViewById<CheckBox>(R.id.playback_auto_stream), tokens, tokens.onSurface)
        tintCheck(card.findViewById<CheckBox>(R.id.playback_repeat), tokens, tokens.onSurface)
        card.findViewById<TextView>(R.id.playback_auto_stream).setTextColor(tokens.onSurface)

        card.findViewById<SeekBar>(R.id.playback_seekbar).apply {
            progressTintList = ColorStateList.valueOf(tokens.accent)
            thumbTintList = ColorStateList.valueOf(tokens.accent)
            progressBackgroundTintList = ColorStateList.valueOf((tokens.onSurfaceMuted and 0xFFFFFF) or (TRACK_ALPHA shl 24))
        }
        styleSpeed(card, tokens)
        styleSources(card, tokens, sourceAvailable)
    }

    /** Only the source selector (availability changed). */
    @JvmStatic
    fun styleSources(card: View, tokens: UiTokens, available: BooleanArray) {
        val density = card.resources.displayMetrics.density
        card.findViewById<TextView>(R.id.btn_hymnSearch).apply {
            background = chip(tokens.surfaceTone, tokens.surfaceTone, density)
            setTextColor(tokens.onSurface)
            compoundDrawableTintList = ColorStateList.valueOf(tokens.onSurface)
        }
        SOURCE_IDS.forEachIndexed { i, id ->
            card.findViewById<TextView>(id).apply {
                background = chip(tokens.surfaceTone, tokens.accent, density)
                val idle = if (available.getOrElse(i) { true }) tokens.onSurface else tokens.disabledOnSurface
                setTextColor(
                    ColorStateList(
                        arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                        intArrayOf(tokens.onAccent, idle),
                    ),
                )
            }
        }
    }

    private fun chip(idle: Int, checked: Int, density: Float) = StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_checked), shape(checked, density))
        addState(intArrayOf(), shape(idle, density))
    }

    private fun shape(color: Int, density: Float) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = CHIP_RADIUS_DP * density
    }

    private fun tintCheck(box: CompoundButton, tokens: UiTokens, color: Int) {
        CompoundButtonCompat.setButtonTintList(
            box,
            ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(tokens.accent, color),
            ),
        )
    }

    private fun styleSpeed(card: View, tokens: UiTokens) {
        // The selected item view belongs to the Spinner and is rebuilt by it: colour the visible one now
        // and MediaGuiController.onItemSelected colours it again after every selection.
        (card.findViewById<android.widget.Spinner>(R.id.playback_speed).selectedView as? TextView)?.setTextColor(tokens.onSurface)
    }

    /** For the speed spinner's selected item view. */
    @JvmStatic
    fun styleSpeedItem(view: View?, tokens: UiTokens) {
        (view as? TextView)?.setTextColor(tokens.onSurface)
    }
}
