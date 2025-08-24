package com.vibestream.player.domain.player

import com.vibestream.player.data.model.MediaItem
import com.vibestream.player.data.model.PlaybackState
import com.vibestream.player.data.model.PlayerEvent
import kotlinx.coroutines.flow.Flow

/**
 * Core player interface that defines the main playback contract.
 * This interface abstracts the underlying media engine (ExoPlayer, AVPlayer, etc.)
 */
interface Player {
    
    /**
     * Current playback state flow
     */
    val playbackState: Flow<PlaybackState>
    
    /**
     * Player events flow
     */
    val playerEvents: Flow<PlayerEvent>
    
    /**
     * Current position in milliseconds
     */
    val currentPosition: Flow<Long>
    
    /**
     * Total duration in milliseconds
     */
    val duration: Flow<Long>
    
    /**
     * Current playback speed multiplier
     */
    val playbackSpeed: Flow<Float>
    
    /**
     * Load and prepare a media source for playback
     */
    suspend fun load(mediaItem: MediaItem): Result<Unit>
    
    /**
     * Start or resume playback
     */
    fun play()
    
    /**
     * Pause playback
     */
    fun pause()
    
    /**
     * Stop playback and release resources
     */
    fun stop()
    
    /**
     * Seek to specific position in milliseconds
     */
    fun seek(positionMs: Long)
    
    /**
     * Set playback speed (0.25x to 4.0x)
     */
    fun setPlaybackSpeed(speed: Float)
    
    /**
     * Select audio track by ID
     */
    fun setAudioTrack(trackId: String?)
    
    /**
     * Select subtitle track by ID
     */
    fun setSubtitleTrack(trackId: String?)
    
    /**
     * Apply audio/video filters
     */
    fun setAudioFilters(filters: AudioFilters)
    
    /**
     * Apply video filters
     */
    fun setVideoFilters(filters: VideoFilters)
    
    /**
     * Set volume (0.0 to 1.0)
     */
    fun setVolume(volume: Float)
    
    /**
     * Enable/disable repeat mode
     */
    fun setRepeatMode(mode: RepeatMode)
    
    /**
     * Enable/disable shuffle mode
     */
    fun setShuffleMode(enabled: Boolean)
    
    /**
     * Release player resources
     */
    fun release()
}

/**
 * Audio filter configuration
 */
data class AudioFilters(
    val equalizer: EqualizerSettings? = null,
    val bassBoost: Float = 0f,
    val virtualizer: Float = 0f,
    val replayGain: ReplayGainSettings? = null
)

/**
 * Video filter configuration
 */
data class VideoFilters(
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val gamma: Float = 1f,
    val hue: Float = 0f
)

/**
 * Equalizer configuration
 */
data class EqualizerSettings(
    val preamp: Float = 0f,
    val bands: List<Float> = emptyList() // 10-band EQ values in dB
)

/**
 * ReplayGain settings
 */
data class ReplayGainSettings(
    val mode: ReplayGainMode = ReplayGainMode.TRACK,
    val preamp: Float = 0f
)

enum class ReplayGainMode {
    OFF, TRACK, ALBUM
}

enum class RepeatMode {
    OFF, ONE, ALL
}