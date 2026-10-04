package org.cog.hymnchtv.ui.motion

import android.animation.ValueAnimator
import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.transition.ChangeBounds
import android.transition.ChangeClipBounds
import android.transition.ChangeImageTransform
import android.transition.ChangeTransform
import android.transition.Fade
import android.transition.Transition
import android.transition.TransitionSet
import android.view.View
import android.view.Window
import android.view.animation.Interpolator
import androidx.core.view.ViewCompat
import androidx.core.view.animation.PathInterpolatorCompat
import androidx.fragment.app.FragmentTransaction
import org.cog.hymnchtv.R

/**
 * Motion of the redesign (spec 5b-4): fade-through 200 ms between pages, and one shared element (the hymn number) from the
 * home preview to the lyrics page header, 250 ms with the Material 3 emphasized easing. Nothing animates when the
 * system turned animations off.
 */
object Motion {
    /** Transition name of the hymn number on the home preview and the lyrics page header. */
    const val SHARED_NUMBER = "hymn_number_shared"

    /** Intent extra: the lyrics page was opened from the home preview, so its header number is a shared element. */
    const val EXTRA_SHARED_NUMBER = "attr_shared_number"

    const val FADE_MS = 200L
    const val SHARED_MS = 250L
    const val POSTPONE_TIMEOUT_MS = 300L

    private const val FADE_OUT_MS = 90L

    /** True unless the system "remove animations" setting (or the animator duration scale 0) is on. */
    @JvmStatic
    fun enabled(context: Context): Boolean {
        val scale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        if (scale == 0f) return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ValueAnimator.areAnimatorsEnabled()
    }

    /** M3 emphasized easing, cubic-bezier(0.2, 0, 0, 1). */
    @JvmStatic
    fun emphasized(): Interpolator = PathInterpolatorCompat.create(0.2f, 0f, 0f, 1f)

    /** Opens the lyrics page with a shared hymn number when [number] is given, otherwise with a plain fade. */
    @JvmStatic
    fun contentOptions(activity: Activity, number: View?): Bundle? {
        if (!enabled(activity)) return null
        if (number == null) return ActivityOptions.makeCustomAnimation(activity, R.anim.fade_in_200, R.anim.fade_out_90).toBundle()
        ViewCompat.setTransitionName(number, SHARED_NUMBER)
        return ActivityOptions.makeSceneTransitionAnimation(activity, number, SHARED_NUMBER).toBundle()
    }

    /** The home window fades out and back in around a page opened from it. */
    @JvmStatic
    fun applyHostWindow(window: Window, context: Context) {
        if (!enabled(context)) return
        window.exitTransition = fade()
        window.reenterTransition = fade()
    }

    /** The lyrics page window: fade in/out, the shared number moves in but never back (the hymn may have changed by then). */
    @JvmStatic
    fun applyContentWindow(window: Window, context: Context) {
        if (!enabled(context)) {
            window.enterTransition = null
            window.returnTransition = null
            window.sharedElementEnterTransition = null
            window.sharedElementReturnTransition = null
            return
        }
        window.enterTransition = fade()
        window.returnTransition = fade()
        window.sharedElementEnterTransition = sharedMove()
        window.sharedElementReturnTransition = null
    }

    /** Fade-through between two full pages (catalogue, settings): the old page fades out, then the new one fades in. */
    @JvmStatic
    fun fadeThrough(tx: FragmentTransaction, context: Context): FragmentTransaction {
        if (!enabled(context)) return tx
        return tx.setCustomAnimations(R.animator.fade_through_in, R.animator.fade_through_out, R.animator.fade_through_in, R.animator.fade_through_out)
    }

    private fun fade(): Transition = Fade().setDuration(FADE_MS)

    private fun sharedMove(): Transition = TransitionSet().apply {
        ordering = TransitionSet.ORDERING_TOGETHER
        addTransition(ChangeBounds())
        addTransition(ChangeTransform())
        addTransition(ChangeClipBounds())
        addTransition(ChangeImageTransform())
        duration = SHARED_MS
        interpolator = emphasized()
    }
}
