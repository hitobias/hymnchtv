package org.cog.hymnchtv.ui.player

/**
 * What the capsule needs to know about the audio player. [active] is true while a track is playing or paused
 * (not stopped or only prepared), which decides between the play key and the bare note button.
 */
data class PlaybackUiState(
    val isPlaying: Boolean = false,
    val positionMs: Int = 0,
    val durationMs: Int = 0,
    val hymnInfo: String = "",
    val active: Boolean = false,
) {
    /** 0..1 progress of the ring. */
    val progress: Float get() = if (durationMs <= 0) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
}

/** Receives every playback state or progress change of the audio player. */
fun interface PlaybackUiListener {
    fun onPlaybackUiState(state: PlaybackUiState)
}
