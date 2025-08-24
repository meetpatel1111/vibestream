package com.vibestream.player.ui.screen.nowplaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibestream.player.data.model.PlaybackState
import com.vibestream.player.domain.player.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.vibestream.player.domain.subtitle.SubtitleManager
import com.vibestream.player.domain.subtitle.SubtitleConfig
import com.vibestream.player.domain.subtitle.SubtitleEntry
import com.vibestream.player.domain.audio.AudioEffectProcessor
import com.vibestream.player.domain.audio.AudioDspConfig
import javax.inject.Inject

/**
 * ViewModel for the Now Playing screen
 */
@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    private val playbackController: PlaybackController,
    private val subtitleManager: SubtitleManager,
    private val audioEffectProcessor: AudioEffectProcessor
) : ViewModel() {
    
    // Expose playback state from controller
    val playbackState: Flow<PlaybackState> = playbackController.playbackState
    val currentPosition: Flow<Long> = playbackController.currentPosition
    val duration: Flow<Long> = playbackController.duration
    val playbackSpeed: Flow<Float> = playbackController.playbackSpeed
    
    // Expose subtitle state from manager
    val subtitleState = subtitleManager.subtitleState
    
    // Audio configuration state
    private val _audioConfig = MutableStateFlow(AudioDspConfig())
    val audioConfig: StateFlow<AudioDspConfig> = _audioConfig.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    /**
     * Toggle play/pause
     */
    fun togglePlayPause() {
        viewModelScope.launch {
            try {
                val currentState = playbackController.playbackState.value
                if (currentState.isPlaying) {\n                    playbackController.pause()\n                } else {\n                    playbackController.play()\n                }\n            } catch (e: Exception) {\n                _errorMessage.value = e.message\n            }\n        }\n    }\n    \n    /**\n     * Seek to specific position\n     */\n    fun seek(positionMs: Long) {\n        viewModelScope.launch {\n            try {\n                playbackController.seek(positionMs)\n            } catch (e: Exception) {\n                _errorMessage.value = e.message\n            }\n        }\n    }\n    \n    /**\n     * Skip to next track\n     */\n    fun next() {\n        viewModelScope.launch {\n            try {\n                playbackController.next()\n            } catch (e: Exception) {\n                _errorMessage.value = e.message\n            }\n        }\n    }\n    \n    /**\n     * Skip to previous track\n     */\n    fun previous() {\n        viewModelScope.launch {\n            try {\n                playbackController.previous()\n            } catch (e: Exception) {\n                _errorMessage.value = e.message\n            }\n        }\n    }\n    \n    /**\n     * Set playback speed\n     */\n    fun setPlaybackSpeed(speed: Float) {\n        viewModelScope.launch {\n            try {\n                playbackController.setPlaybackSpeed(speed)\n            } catch (e: Exception) {\n                _errorMessage.value = e.message\n            }\n        }\n    }\n    \n    /**\n     * Set volume\n     */\n    fun setVolume(volume: Float) {\n        viewModelScope.launch {\n            try {\n                playbackController.setVolume(volume)\n            } catch (e: Exception) {\n                _errorMessage.value = e.message\n            }\n        }\n    }\n    \n    /**
     * Update audio DSP configuration
     */
    fun updateAudioConfig(config: AudioDspConfig) {
        viewModelScope.launch {
            try {
                _audioConfig.value = config
                
                // Apply settings to audio processor
                audioEffectProcessor.applyEqualizer(config.equalizer)
                audioEffectProcessor.applyBassBoost(config.bassBoost)
                audioEffectProcessor.applyVirtualizer(config.virtualizer)
                audioEffectProcessor.applyPlaybackSpeed(config.playbackSpeed)
                audioEffectProcessor.applyReplayGain(config.replayGain)
                audioEffectProcessor.applyDynamics(config.dynamics)
                audioEffectProcessor.setMasterVolume(config.masterVolume)
                audioEffectProcessor.setBalance(config.balance)
                
                // Also update playback speed in the player
                playbackController.setPlaybackSpeed(config.playbackSpeed.speed)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }
    
    /**
     * Get current subtitle for position
     */
    fun getCurrentSubtitle(positionMs: Long): SubtitleEntry? {
        return subtitleManager.getCurrentSubtitle(positionMs)
    }
    
    /**
     * Select subtitle track
     */
    fun selectSubtitleTrack(trackId: String?) {
        viewModelScope.launch {
            try {
                subtitleManager.selectSubtitleTrack(trackId)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }
    
    /**
     * Set subtitle sync offset
     */
    fun setSubtitleSyncOffset(offsetMs: Long) {
        subtitleManager.setSyncOffset(offsetMs)
    }
    
    /**
     * Update subtitle configuration
     */
    fun updateSubtitleConfig(config: SubtitleConfig) {
        subtitleManager.updateConfig(config)
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    init {
        // Load subtitles when media changes
        viewModelScope.launch {
            playbackState.collect { state ->
                state.currentMediaItem?.let { media ->
                    subtitleManager.loadSubtitlesForMedia(media.id, media.uri)
                }
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Don't release the player here as it should be managed globally
    }
}