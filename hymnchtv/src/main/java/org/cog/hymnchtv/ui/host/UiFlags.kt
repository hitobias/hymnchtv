package org.cog.hymnchtv.ui.host

/**
 * Release switches for entry points whose backing feature is not part of the current release.
 * Flip [NOTEBOOK_UI_ENABLED] once the notebook UI (sub-project D-1, 1.1) is wired.
 */
object UiFlags {
    /** My-hymns tab and the home "add to playlist" button: hidden in 1.0, their UI ships with D-1. */
    const val NOTEBOOK_UI_ENABLED = false
}
