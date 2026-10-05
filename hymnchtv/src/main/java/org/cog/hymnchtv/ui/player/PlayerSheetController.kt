package org.cog.hymnchtv.ui.player

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.view.animation.PathInterpolator
import android.widget.ImageView
import org.cog.hymnchtv.R
import org.cog.hymnchtv.reading.background.GlassMode
import org.cog.hymnchtv.reading.background.UiTokens
import org.cog.hymnchtv.ui.lyrics.ContentInsets
import org.cog.hymnchtv.ui.lyrics.PillAnchor

/** What the controller needs from the activity. */
interface PlayerSheetCallbacks {
    fun isPortrait(): Boolean

    /** Show or hide the card (the audio player fragment's root); a no-op while it does not exist. */
    fun setCardVisible(visible: Boolean)

    /** Play or pause from the capsule's key. */
    fun togglePlayback()

    /** The reserve or the system bottom inset changed: every lyrics page recomputes its padding. */
    fun onPlayerInsetsChanged()
}

/**
 * The only owner of what the player layer shows (spec section 4): [render] decides between the card, the capsule and
 * nothing from [state], the orientation and whether a video owns the layer. It also runs the container transform,
 * the drag-to-collapse, the haptics and tells the lyrics pages how much room to keep.
 */
class PlayerSheetController(
    private val host: ViewGroup,
    private val capsule: PlayerCapsuleView,
    initial: PlayerSheetState,
    private val callbacks: PlayerSheetCallbacks,
) : PlaybackUiListener, SheetDragLinearLayout.DragListener {
    private val context: Context = host.context
    private val density = context.resources.displayMetrics.density
    private val playButton: ProgressRingButton = capsule.findViewById(R.id.capsulePlay)
    private val expandButton: ImageView = capsule.findViewById(R.id.capsuleExpand)
    private val noteButton: ImageView = capsule.findViewById(R.id.capsuleNote)
    private val accessibility = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    private val touchExplorationListener = AccessibilityManager.TouchExplorationStateChangeListener { applyDragEnabled() }

    var state: PlayerSheetState = initial
        private set

    var videoActive: Boolean = false
        private set

    var systemBottom: Int = 0
        private set

    /** What is on screen now (null before the first render). */
    var rendered: SheetDisplay? = null
        private set

    private var card: GlassFrameLayout? = null
    private var playback = PlaybackUiState()
    private var tokens: UiTokens? = null
    private var animator: ValueAnimator? = null
    private var lastReserve = -1
    private var lastSystemBottom = -1

    init {
        capsule.onExpandGesture = { expand(animate = true) }
        expandButton.setOnClickListener { expand(animate = true) }
        noteButton.setOnClickListener { expand(animate = true) }
        playButton.setOnClickListener {
            haptic()
            callbacks.togglePlayback()
        }
        host.addOnLayoutChangeListener { _, _, top, _, bottom, _, oldTop, _, oldBottom ->
            if (bottom - top != oldBottom - oldTop) notifyInsets()
        }
        accessibility?.addTouchExplorationStateChangeListener(touchExplorationListener)
        applyDragEnabled()
        applyPlayback()
    }

    fun release() {
        animator?.cancel()
        accessibility?.removeTouchExplorationStateChangeListener(touchExplorationListener)
    }

    // ---- state changes: every one ends in render() ----

    fun collapse(animate: Boolean = true) {
        state = state.collapse(callbacks.isPortrait())
        render(animate)
        haptic()
    }

    fun expand(animate: Boolean = true) {
        state = state.expand(callbacks.isPortrait())
        render(animate)
        haptic()
    }

    /** The overflow menu's hide/show switch. */
    fun toggleUserHidden() {
        state = state.toggleUserHidden()
        render()
    }

    /** [portrait] comes from the new configuration, as the application's cached flag may not be updated yet. */
    fun onOrientationChanged(portrait: Boolean) {
        state = state.onOrientationChanged(portrait)
        render()
    }

    fun setVideoActive(active: Boolean) {
        videoActive = active
        render()
    }

    fun restore(restored: PlayerSheetState) {
        state = restored
        render()
    }

    // ---- views ----

    /** The audio player fragment created its card (again, after a video). */
    fun attachCard(view: GlassFrameLayout) {
        card = view
        body()?.dragListener = this
        view.findViewById<View>(R.id.btn_player_collapse)?.setOnClickListener { collapse(animate = true) }
        applyDragEnabled()
        tokens?.let { styleHandle(it) }
        resetCardTransform()
        callbacks.setCardVisible(rendered == SheetDisplay.CARD)
        notifyInsets()
    }

    fun detachCard(view: View) {
        if (card === view) {
            animator?.cancel()
            card = null
        }
    }

    /**
     * @param tokens the glass tokens of [mode]: their `surface` is the glass tint of the capsule and of the card
     * (the card paints itself in [org.cog.hymnchtv.ui.lyrics.PlayerCardStyle]; only the handle and the collapse key here)
     */
    fun applyTokens(tokens: UiTokens, mode: GlassMode) {
        this.tokens = tokens
        capsule.applyGlass(tokens.surface, mode, CAPSULE_RADIUS_DP)
        capsule.elevation = CAPSULE_ELEVATION_DP * density
        playButton.applyColors(tokens.accent, tokens.onAccent, tokens.surfaceTone)
        val tint = ColorStateList.valueOf(tokens.onSurface)
        expandButton.imageTintList = tint
        noteButton.imageTintList = ColorStateList.valueOf(tokens.onAccent)
        noteButton.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(tokens.accent)
        }
        styleHandle(tokens)
    }

    private fun styleHandle(tokens: UiTokens) {
        val c = card ?: return
        c.findViewById<View>(R.id.sheet_handle)?.background = GradientDrawable().apply {
            setColor(tokens.onSurfaceMuted)
            cornerRadius = HANDLE_HEIGHT_DP * density / 2f
        }
        (c.findViewById<View>(R.id.btn_player_collapse) as? ImageView)?.imageTintList = ColorStateList.valueOf(tokens.onSurface)
    }

    /** Window insets: the player layer and the capsule keep clear of the bars and the keyboard. */
    fun onContentInsets(insets: ContentInsets) {
        systemBottom = insets.bottom
        (host.layoutParams as? ViewGroup.MarginLayoutParams)?.let {
            if (it.bottomMargin != insets.bottom) {
                it.bottomMargin = insets.bottom
                host.layoutParams = it
            }
        }
        (capsule.layoutParams as? ViewGroup.MarginLayoutParams)?.let {
            val end = ((CAPSULE_MARGIN_DP * density) + insets.right).toInt()
            val bottom = ((CAPSULE_MARGIN_DP * density) + insets.bottom).toInt()
            if (it.rightMargin != end || it.bottomMargin != bottom) {
                it.rightMargin = end
                it.bottomMargin = bottom
                capsule.layoutParams = it
            }
        }
        notifyInsets()
    }

    // ---- rendering ----

    fun target(): SheetDisplay = state.display(callbacks.isPortrait(), videoActive)

    /** Show what [state] says. [animate] runs the container transform between card and capsule. */
    @JvmOverloads
    fun render(animate: Boolean = false) {
        val target = target()
        val from = rendered
        animator?.cancel()
        if (target == from) {
            snap(target)
            return
        }
        val cardView = card
        val canAnimate = animate && animationsEnabled() && cardView != null && from != null &&
            ((from == SheetDisplay.CARD && target == SheetDisplay.CAPSULE) || (from == SheetDisplay.CAPSULE && target == SheetDisplay.CARD))
        if (canAnimate) transition(cardView!!, toCard = target == SheetDisplay.CARD) else snap(target)
    }

    private fun snap(target: SheetDisplay) {
        animator = null
        rendered = target
        resetCardTransform()
        capsule.alpha = 1f
        capsule.scaleX = 1f
        capsule.scaleY = 1f
        callbacks.setCardVisible(target == SheetDisplay.CARD)
        capsule.visibility = if (target == SheetDisplay.CAPSULE) View.VISIBLE else View.GONE
        applyPlayback()
        notifyInsets()
    }

    private fun resetCardTransform() {
        card?.apply { translationX = 0f; translationY = 0f; scaleX = 1f; scaleY = 1f; alpha = 1f }
    }

    private fun animationsEnabled(): Boolean =
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f

    /** Where the capsule sits relative to the card's bottom-right corner, and the scale that makes the card as big as it. */
    private class Geometry(val sx: Float, val sy: Float, val tx: Float, val ty: Float)

    private fun geometry(cardView: View): Geometry {
        val form = PlayerSheetState.capsuleForm(playback)
        val capW = (if (form == CapsuleForm.PLAYBACK) CAPSULE_PLAYBACK_WIDTH_DP else CAPSULE_HEIGHT_DP) * density
        val capH = CAPSULE_HEIGHT_DP * density
        val root = host.parent as View
        val lp = capsule.layoutParams as ViewGroup.MarginLayoutParams
        val capRight = root.width - lp.rightMargin
        val capBottom = root.height - lp.bottomMargin
        val w = cardView.width.coerceAtLeast(1).toFloat()
        val h = cardView.height.coerceAtLeast(1).toFloat()
        return Geometry(capW / w, capH / h, (capRight - host.right).toFloat(), (capBottom - host.bottom).toFloat())
    }

    private fun transition(cardView: GlassFrameLayout, toCard: Boolean) {
        val to = if (toCard) SheetDisplay.CARD else SheetDisplay.CAPSULE
        rendered = to
        if (toCard) {
            callbacks.setCardVisible(true)
            capsule.visibility = View.VISIBLE
            cardView.alpha = 0f
            cardView.post { runTransition(cardView, toCard = true, to = to) }
        }
        else {
            capsule.visibility = View.VISIBLE
            capsule.alpha = 0f
            runTransition(cardView, toCard = false, to = to)
        }
        notifyInsets()
    }

    private fun runTransition(cardView: View, toCard: Boolean, to: SheetDisplay) {
        if (rendered != to) return
        val g = geometry(cardView)
        cardView.pivotX = cardView.width.toFloat()
        cardView.pivotY = cardView.height.toFloat()
        capsule.pivotX = capsule.width.toFloat().coerceAtLeast(1f)
        capsule.pivotY = capsule.height.toFloat().coerceAtLeast(1f)
        val startScaleX = cardView.scaleX.takeIf { !toCard } ?: g.sx
        val startScaleY = cardView.scaleY.takeIf { !toCard } ?: g.sy
        val startTx = if (toCard) g.tx else cardView.translationX
        val startTy = if (toCard) g.ty else cardView.translationY
        val startAlpha = if (toCard) 0f else cardView.alpha
        val endScaleX = if (toCard) 1f else g.sx
        val endScaleY = if (toCard) 1f else g.sy
        val endTx = if (toCard) 0f else g.tx
        val endTy = if (toCard) 0f else g.ty
        val anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = TRANSITION_MS
            interpolator = EMPHASIZED
            addUpdateListener {
                val p = it.animatedValue as Float
                cardView.scaleX = lerp(startScaleX, endScaleX, p)
                cardView.scaleY = lerp(startScaleY, endScaleY, p)
                cardView.translationX = lerp(startTx, endTx, p)
                cardView.translationY = lerp(startTy, endTy, p)
                // The card is on screen for the larger part of the way out, the capsule for the larger part of the way in
                val cardShare = if (toCard) ramp(p, 0f, 0.5f) else 1f - ramp(p, 0.3f, 0.7f)
                cardView.alpha = if (toCard) cardShare else startAlpha * cardShare
                val capShare = if (toCard) 1f - ramp(p, 0f, 0.4f) else ramp(p, 0.4f, 0.9f)
                capsule.alpha = capShare
                val s = lerp(CAPSULE_ENTER_SCALE, 1f, capShare)
                capsule.scaleX = s
                capsule.scaleY = s
            }
            addListener(object : AnimatorListenerAdapter() {
                private var cancelled = false
                override fun onAnimationCancel(a: Animator) {
                    cancelled = true
                }

                override fun onAnimationEnd(a: Animator) {
                    if (!cancelled && animator === a) snap(to)
                }
            })
        }
        animator = anim
        anim.start()
    }

    // ---- drag of the card ----

    override fun onDragStart() {
        animator?.cancel()
        animator = null
        card?.apply { pivotX = width.toFloat(); pivotY = height.toFloat() }
    }

    override fun onDrag(dy: Float) {
        val c = card ?: return
        val q = (dy / c.height.coerceAtLeast(1)).coerceIn(0f, 1f)
        c.translationY = dy
        val s = 1f - DRAG_SHRINK * q
        c.scaleX = s
        c.scaleY = s
        c.alpha = 1f - DRAG_FADE * q
    }

    override fun onDragEnd(dy: Float, velocityY: Float, cancelled: Boolean) {
        val c = card ?: return
        if (!cancelled && PlayerSheetState.shouldCollapse(dy, c.height.toFloat(), velocityY, density)) {
            collapse(animate = true)
        }
        else {
            val sy0 = c.translationY
            val s0 = c.scaleX
            val a0 = c.alpha
            animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = SPRING_BACK_MS
                interpolator = EMPHASIZED
                addUpdateListener {
                    val p = it.animatedValue as Float
                    c.translationY = lerp(sy0, 0f, p)
                    c.scaleX = lerp(s0, 1f, p)
                    c.scaleY = c.scaleX
                    c.alpha = lerp(a0, 1f, p)
                }
                start()
            }
        }
    }

    // ---- capsule content ----

    override fun onPlaybackUiState(state: PlaybackUiState) {
        playback = state
        applyPlayback()
    }

    private fun applyPlayback() {
        val form = PlayerSheetState.capsuleForm(playback)
        val playing = form == CapsuleForm.PLAYBACK
        val formChanged = playButton.visibility != (if (playing) View.VISIBLE else View.GONE)
        playButton.visibility = if (playing) View.VISIBLE else View.GONE
        expandButton.visibility = if (playing) View.VISIBLE else View.GONE
        noteButton.visibility = if (playing) View.GONE else View.VISIBLE
        playButton.progress = if (playing) playback.progress else 0f
        playButton.setImageResource(if (playback.isPlaying) R.drawable.ic_sym_pause else R.drawable.ic_sym_play_arrow)
        val verb = context.getString(if (playback.isPlaying) R.string.c_pause else R.string.c_play)
        val label = if (playback.hymnInfo.isBlank()) verb else context.getString(
            if (playback.isPlaying) R.string.c_player_pause_named else R.string.c_player_play_named, playback.hymnInfo)
        if (playButton.contentDescription != label) playButton.contentDescription = label
        if (formChanged) {
            capsule.requestLayout()
            // The capsule's width changed (note <-> playback): the toolbar capsule beside it moves
            callbacks.onPlayerInsetsChanged()
        }
    }

    private fun applyDragEnabled() {
        val enabled = accessibility?.isTouchExplorationEnabled != true
        capsule.dragEnabled = enabled
        body()?.dragEnabled = enabled
    }

    private fun body(): SheetDragLinearLayout? = card?.findViewById(R.id.playerBody)

    /** Whether the card's, and the capsule's, blur is running now (for tests and the performance check). */
    fun cardBlurActive(): Boolean = card?.glass?.blurActive == true

    fun capsuleBlurActive(): Boolean = capsule.glass?.blurActive == true

    // ---- insets for the lyrics ----

    /** What the lyrics pages keep clear above the system bars: the card's real height, the capsule's, or nothing. */
    fun playerReserve(): Int =
        if (videoActive) host.height else PlayerSheetState.reserve(rendered ?: target(), host.height, density)

    /** What the bottom toolbar capsule lines up with: a playing video counts as the card (it owns the layer). */
    fun pillAnchor(): PillAnchor {
        val shown = if (videoActive) SheetDisplay.CARD else (rendered ?: target())
        val form = PlayerSheetState.capsuleForm(playback)
        val widthDp = if (form == CapsuleForm.PLAYBACK) CAPSULE_PLAYBACK_WIDTH_DP else CAPSULE_HEIGHT_DP
        return PillAnchor(shown, (widthDp * density + 0.5f).toInt(), form)
    }

    private fun notifyInsets() {
        val reserve = playerReserve()
        if (reserve == lastReserve && systemBottom == lastSystemBottom) return
        lastReserve = reserve
        lastSystemBottom = systemBottom
        callbacks.onPlayerInsetsChanged()
    }

    private fun haptic() {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY
        host.performHapticFeedback(constant)
    }

    private companion object {
        const val TRANSITION_MS = 300L
        const val SPRING_BACK_MS = 200L
        const val CAPSULE_HEIGHT_DP = 56
        const val CAPSULE_PLAYBACK_WIDTH_DP = 108
        const val CAPSULE_MARGIN_DP = 16
        const val CAPSULE_RADIUS_DP = 28f
        const val CAPSULE_ELEVATION_DP = 6f
        const val CAPSULE_ENTER_SCALE = 0.8f
        const val HANDLE_HEIGHT_DP = 4
        const val DRAG_SHRINK = 0.12f
        const val DRAG_FADE = 0.3f

        /** Material 3 emphasized easing */
        val EMPHASIZED = PathInterpolator(0.2f, 0f, 0f, 1f)

        fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

        /** 0 before [from], 1 after [to], linear between. */
        fun ramp(p: Float, from: Float, to: Float) = ((p - from) / (to - from)).coerceIn(0f, 1f)
    }
}
