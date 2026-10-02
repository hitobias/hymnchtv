package org.cog.hymnchtv.ui.host

/**
 * Release switches for entry points whose backing feature is not part of the current release.
 * Flip [NOTEBOOK_UI_ENABLED] once the notebook UI (sub-project D-1) is wired: that is the moment D-1's UI plan (Task I2, Step 4)
 * switches it on; 1.1.0 keeps it false.
 */
object UiFlags {
    /** My-hymns tab and the home "add to playlist" button: hidden until the notebook UI of D-1 is finished (a later release than 1.1.0). */
    const val NOTEBOOK_UI_ENABLED = false
}
