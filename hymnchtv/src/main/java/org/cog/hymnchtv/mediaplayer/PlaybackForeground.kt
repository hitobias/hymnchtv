package org.cog.hymnchtv.mediaplayer

/**
 * Whether AudioBgService runs in the foreground (1.6.0). A started service that is not in the foreground is stopped by the
 * system about a minute after the app leaves the screen (API 26+), and its process may then be frozen or killed: so the
 * service is in the foreground while a hymn plays, and also while it is paused midway (the notification can resume it).
 */
enum class PlaybackForeground {
    PLAYING, PAUSED, NONE;

    companion object {
        /**
         * @param playing players that are playing
         * @param pausedMidway players that are paused with a position above 0 (a player only opened to read its duration
         * sits at 0 and does not count)
         */
        @JvmStatic
        fun of(playing: Int, pausedMidway: Int): PlaybackForeground = when {
            playing > 0 -> PLAYING
            pausedMidway > 0 -> PAUSED
            else -> NONE
        }

        /** How long the service stays in the foreground after the last player completed (AutoStream starts the next hymn). */
        const val LINGER_MS = 5_000L

        /**
         * After a player completes on its own, the foreground is kept for [LINGER_MS] when nothing else is left: the next
         * hymn then starts while still in the foreground (API 31+ refuses a foreground start from the background).
         */
        @JvmStatic
        fun lingersAfterCompletion(playersLeft: Int, recording: Boolean): Boolean = playersLeft == 0 && !recording
    }
}
