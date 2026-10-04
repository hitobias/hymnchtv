package org.cog.hymnchtv.ui.lyrics

/** Which of the four media sources (media, jiaochang, changshi, banzhou) a hymn has; none means the play button has nothing to play. */
object MediaAvailability {
    /** True when at least one source is available. An empty array (not evaluated yet) counts as available so the button is never locked by mistake. */
    @JvmStatic
    fun hasAny(available: BooleanArray): Boolean = available.isEmpty() || available.any { it }
}
