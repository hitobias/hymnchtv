package org.cog.hymnchtv.ui.lyrics

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.view.Gravity
import android.widget.ImageButton
import android.widget.ImageView
import android.view.View
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.widget.CompoundButtonCompat
import org.cog.hymnchtv.R
import org.cog.hymnchtv.reading.background.GlassMode
import org.cog.hymnchtv.reading.background.UiTokens
import org.cog.hymnchtv.ui.player.GlassFrameLayout

/**
 * Paints the lyrics page player card from [UiTokens] (visual redesign spec section 6): `surface` (the glass tint) card, `accent` play
 * disc, repeat and speed as `surfaceTone` chips, source selector as segmented buttons (unselected `surfaceTone`/`onSurface`,
 * selected `accent`/`onAccent`, unavailable outlined with the disabled text colour), seek bar `accent` over an
 * `onSurfaceMuted` track at alpha 0.3.
 */
object PlayerCardStyle {
    private const val CARD_RADIUS_DP = 28f
    private const val CHIP_RADIUS_DP = 12f
    private const val TRACK_ALPHA = 0x4D // 0.3
    private const val DISABLED_PLAY_ALPHA = 0.38f

    private val SOURCE_IDS = intArrayOf(R.id.btn_media, R.id.btn_jiaochang, R.id.btn_changshi, R.id.btn_banzhou)

    /**
     * @param tokens the glass tokens of [mode] ([UiTokens.glass]): `surface` is the card's translucent tint
     * @param sourceAvailable whether media exists per source (media, jiaochang, changshi, banzhou); missing media reads as disabled
     */
    @JvmStatic
    fun apply(card: GlassFrameLayout, tokens: UiTokens, sourceAvailable: BooleanArray, mode: GlassMode) {
        card.applyGlass(tokens.surface, mode, CARD_RADIUS_DP)
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
        stylePlay(card, tokens)
        styleChips(card, tokens)
        styleSpeed(card, tokens)
        styleSources(card, tokens, sourceAvailable)
    }

    /** Only the source selector (availability changed). */
    @JvmStatic
    fun styleSources(card: View, tokens: UiTokens, available: BooleanArray) {
        val density = card.resources.displayMetrics.density
        styleMediaEmpty(card, tokens, available)
        card.findViewById<ImageButton>(R.id.btn_hymnSearch).apply {
            background = shape(tokens.surfaceTone, density)
            imageTintList = ColorStateList.valueOf(tokens.onSurface)
        }
        SOURCE_IDS.forEachIndexed { i, id ->
            val usable = available.getOrElse(i) { true }
            card.findViewById<TextView>(id).apply {
                // Unavailable sources are flat outlined chips with the disabled text colour; no underline either way
                background = chip(if (usable) tokens.surfaceTone else tokens.disabledSurface, tokens.accent, density,
                    if (usable) null else tokens.outline)
                (this as? CompoundButton)?.buttonDrawable = null
                gravity = Gravity.CENTER
                paint.isUnderlineText = false
                setTextColor(
                    ColorStateList(
                        arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                        intArrayOf(tokens.onAccent, if (usable) tokens.onSurface else tokens.disabledOnSurface),
                    ),
                )
            }
        }
    }

    /** No media at all: the play button is disabled (dimmed disc) and the explanation line shows, in `onSurfaceMuted`. */
    private fun styleMediaEmpty(card: View, tokens: UiTokens, available: BooleanArray) {
        val hasMedia = MediaAvailability.hasAny(available)
        card.findViewById<ImageView>(R.id.playback_play).apply {
            isEnabled = hasMedia
            alpha = if (hasMedia) 1f else DISABLED_PLAY_ALPHA
        }
        card.findViewById<TextView>(R.id.media_empty).apply {
            visibility = if (hasMedia) View.GONE else View.VISIBLE
            setTextColor(tokens.onSurfaceMuted)
            compoundDrawableTintList = ColorStateList.valueOf(tokens.onSurfaceMuted)
        }
    }

    /** The play button: `accent` disc with the `onAccent` play or pause icon. */
    private fun stylePlay(card: View, tokens: UiTokens) {
        card.findViewById<ImageView>(R.id.playback_play).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(tokens.accent)
            }
            imageTintList = ColorStateList.valueOf(tokens.onAccent)
        }
    }

    /** Repeat (icon + count) and speed are `surfaceTone` chips of the same height, either side of the play button. */
    private fun styleChips(card: View, tokens: UiTokens) {
        val density = card.resources.displayMetrics.density
        card.findViewById<View>(R.id.mp_repeat_chip).background = shape(tokens.surfaceTone, density)
        card.findViewById<View>(R.id.playback_speed).background = shape(tokens.surfaceTone, density)
    }

    private fun chip(idle: Int, checked: Int, density: Float, idleStroke: Int?) = StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_checked), shape(checked, density))
        addState(intArrayOf(), shape(idle, density, idleStroke))
    }

    private fun shape(color: Int, density: Float, stroke: Int? = null) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = CHIP_RADIUS_DP * density
        if (stroke != null) setStroke(density.toInt().coerceAtLeast(1), stroke)
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
