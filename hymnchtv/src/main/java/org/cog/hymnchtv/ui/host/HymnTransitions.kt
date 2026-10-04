package org.cog.hymnchtv.ui.host

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.transition.ChangeBounds
import android.transition.ChangeTransform
import android.transition.Fade
import android.transition.TransitionSet
import android.view.View
import android.view.animation.PathInterpolator
import android.util.Pair

/**
 * Motion between the home page and the lyrics page (spec 5b-4): the typed number moves to the lyrics header's place
 * (shared element, 250ms, M3 emphasized easing) while the rest fades through in 200ms. With the system's animations off
 * ([Settings.Global.ANIMATOR_DURATION_SCALE] = 0) no options are made at all, so the page simply switches.
 */
object HymnTransitions {
    const val SHARED_NUMBER = "hymn_number"
    const val EXTRA_NUMBER_TEXT = "hymnNumberText"
    const val SHARED_MS = 250L
    const val FADE_MS = 200L

    /** M3 emphasized easing, approximated by its decelerating control points. */
    private fun emphasized() = PathInterpolator(0.2f, 0f, 0f, 1f)

    @JvmStatic
    fun animationsEnabled(durationScale: Float): Boolean = durationScale > 0f

    @JvmStatic
    fun durationScale(context: Context): Float =
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)

    /** The options for opening the lyrics page, or null (a plain switch) when animations are off. */
    @JvmStatic
    fun openOptions(activity: Activity, shared: View?, durationScale: Float = durationScale(activity)): Bundle? {
        if (!animationsEnabled(durationScale)) return null
        val options = if (shared != null) {
            ActivityOptions.makeSceneTransitionAnimation(activity, Pair.create(shared, SHARED_NUMBER))
        } else {
            ActivityOptions.makeSceneTransitionAnimation(activity)
        }
        return options.toBundle()
    }

    /** The home page fades out and back in. */
    @JvmStatic
    fun prepareExit(activity: Activity) {
        activity.window.exitTransition = Fade().setDuration(FADE_MS)
        activity.window.reenterTransition = Fade().setDuration(FADE_MS)
    }

    /** The lyrics page fades in; its number view takes over the shared element. */
    @JvmStatic
    fun prepareEnter(activity: Activity) {
        val window = activity.window
        window.enterTransition = Fade().setDuration(FADE_MS)
        window.returnTransition = Fade().setDuration(FADE_MS)
        val move = {
            TransitionSet().addTransition(ChangeBounds()).addTransition(ChangeTransform())
                .setDuration(SHARED_MS).setInterpolator(emphasized())
        }
        window.sharedElementEnterTransition = move()
        window.sharedElementReturnTransition = move()
    }
}
