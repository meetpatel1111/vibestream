package com.vibestream.player.data.audio

import android.media.audiofx.*
import android.util.Log
import com.vibestream.player.domain.audio.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android AudioEffect-based implementation of audio DSP
 */
@Singleton
class AndroidAudioEffectProcessor @Inject constructor() : AudioEffectProcessor {
    
    private var audioSessionId: Int = 0
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    
    private val _isEnabled = MutableStateFlow(false)
    private val isEffectEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()
    
    private val _currentConfig = MutableStateFlow(AudioDspConfig())
    val currentConfig: StateFlow<AudioDspConfig> = _currentConfig.asStateFlow()
    
    companion object {
        private const val TAG = "AndroidAudioEffectProcessor"
    }
    
    override fun initialize(): Boolean {
        return try {
            Log.d(TAG, "Initializing audio effects for session: $audioSessionId")
            createEffects()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize audio effects", e)
            false
        }
    }
    
    fun setAudioSession(sessionId: Int) {
        if (audioSessionId != sessionId) {
            release() // Release old effects
            audioSessionId = sessionId
            if (sessionId != 0) {
                initialize()
            }
        }
    }
    
    private fun createEffects() {
        try {
            // Create Equalizer
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = false
            }
            
            // Create Bass Boost
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = false
            }
            
            // Create Virtualizer
            virtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = false
            }
            
            // Create Loudness Enhancer (for ReplayGain-like functionality)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                    enabled = false
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating audio effects", e)
        }
    }
    
    override fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
            loudnessEnhancer?.release()
            
            equalizer = null
            bassBoost = null
            virtualizer = null
            loudnessEnhancer = null
            
            _isEnabled.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing audio effects", e)
        }
    }
    
    override fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
        
        try {
            equalizer?.enabled = enabled && _currentConfig.value.equalizer.enabled
            bassBoost?.enabled = enabled && _currentConfig.value.bassBoost.enabled
            virtualizer?.enabled = enabled && _currentConfig.value.virtualizer.enabled
            loudnessEnhancer?.enabled = enabled && _currentConfig.value.replayGain.enabled
        } catch (e: Exception) {
            Log.e(TAG, "Error setting effects enabled state", e)
        }
    }
    
    override fun isEnabled(): Boolean = _isEnabled.value
    
    override fun applyEqualizer(settings: EqualizerSettings) {
        try {
            equalizer?.let { eq ->
                eq.enabled = _isEnabled.value && settings.enabled
                
                if (settings.enabled) {
                    // Apply preamp (overall gain)
                    // Note: Android Equalizer doesn't have direct preamp, 
                    // so we'll adjust all bands proportionally
                    
                    val numBands = eq.numberOfBands.toInt()
                    val availableBands = minOf(numBands, settings.bands.size)
                    
                    for (i in 0 until availableBands) {
                        val bandGain = settings.bands[i].gain + settings.preamp
                        val gainInMillibels = (bandGain * 100).toInt().toShort()
                        
                        // Clamp to valid range
                        val clampedGain = gainInMillibels.coerceIn(
                            eq.getBandLevelRange()[0],
                            eq.getBandLevelRange()[1]
                        )
                        
                        eq.setBandLevel(i.toShort(), clampedGain)
                    }
                }
            }
            
            updateConfig { copy(equalizer = settings) }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying equalizer settings", e)
        }
    }
    
    override fun applyBassBoost(settings: BassBoostSettings) {
        try {
            bassBoost?.let { boost ->
                boost.enabled = _isEnabled.value && settings.enabled
                
                if (settings.enabled) {
                    val strength = (settings.strength * 1000).toInt().toShort()
                    boost.setStrength(strength)
                }
            }
            
            updateConfig { copy(bassBoost = settings) }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying bass boost settings", e)
        }
    }
    
    override fun applyVirtualizer(settings: VirtualizerSettings) {
        try {
            virtualizer?.let { virt ->
                virt.enabled = _isEnabled.value && settings.enabled
                
                if (settings.enabled) {
                    val strength = (settings.strength * 1000).toInt().toShort()
                    virt.setStrength(strength)
                }
            }
            
            updateConfig { copy(virtualizer = settings) }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying virtualizer settings", e)
        }
    }
    
    override fun applyPlaybackSpeed(settings: PlaybackSpeedSettings) {
        // Note: Playback speed is typically handled by the media player itself (ExoPlayer)\n        // This method updates our config but actual speed change should be done in the player\n        updateConfig { copy(playbackSpeed = settings) }\n    }\n    \n    override fun applyReplayGain(settings: ReplayGainSettings) {\n        try {\n            loudnessEnhancer?.let { enhancer ->\n                enhancer.enabled = _isEnabled.value && settings.enabled\n                \n                if (settings.enabled) {\n                    // Convert preamp to target gain in millibels\n                    val targetGain = (settings.preamp * 100).toInt()\n                    enhancer.setTargetGain(targetGain)\n                }\n            }\n            \n            updateConfig { copy(replayGain = settings) }\n        } catch (e: Exception) {\n            Log.e(TAG, \"Error applying replay gain settings\", e)\n        }\n    }\n    \n    override fun applyDynamics(settings: DynamicsSettings) {\n        // Note: Android doesn't have built-in compressor/limiter effects\n        // This would require custom DSP implementation or third-party libraries\n        updateConfig { copy(dynamics = settings) }\n    }\n    \n    override fun setMasterVolume(volume: Float) {\n        updateConfig { copy(masterVolume = volume.coerceIn(0f, 1f)) }\n    }\n    \n    override fun setBalance(balance: Float) {\n        updateConfig { copy(balance = balance.coerceIn(-1f, 1f)) }\n    }\n    \n    /**\n     * Get available equalizer presets\n     */\n    fun getAvailablePresets(): List<String> {\n        return equalizer?.let { eq ->\n            val presets = mutableListOf<String>()\n            for (i in 0 until eq.numberOfPresets) {\n                presets.add(eq.getPresetName(i.toShort()))\n            }\n            presets\n        } ?: emptyList()\n    }\n    \n    /**\n     * Apply system equalizer preset\n     */\n    fun applySystemPreset(presetIndex: Int) {\n        try {\n            equalizer?.let { eq ->\n                if (presetIndex in 0 until eq.numberOfPresets) {\n                    eq.usePreset(presetIndex.toShort())\n                }\n            }\n        } catch (e: Exception) {\n            Log.e(TAG, \"Error applying system preset\", e)\n        }\n    }\n    \n    /**\n     * Get equalizer frequency response\n     */\n    fun getEqualizerFrequencyResponse(): List<Pair<Int, Float>> {\n        return equalizer?.let { eq ->\n            val response = mutableListOf<Pair<Int, Float>>()\n            for (i in 0 until eq.numberOfBands) {\n                val centerFreq = eq.getCenterFreq(i.toShort()) / 1000 // Convert to Hz\n                val currentLevel = eq.getBandLevel(i.toShort()) / 100f // Convert to dB\n                response.add(Pair(centerFreq, currentLevel))\n            }\n            response\n        } ?: emptyList()\n    }\n    \n    private fun updateConfig(update: AudioDspConfig.() -> AudioDspConfig) {\n        _currentConfig.value = _currentConfig.value.update()\n    }\n    \n    /**\n     * Save current configuration to preferences\n     */\n    fun saveCurrentConfig(): AudioDspConfig {\n        return _currentConfig.value\n    }\n    \n    /**\n     * Restore configuration from saved settings\n     */\n    fun restoreConfig(config: AudioDspConfig) {\n        applyEqualizer(config.equalizer)\n        applyBassBoost(config.bassBoost)\n        applyVirtualizer(config.virtualizer)\n        applyPlaybackSpeed(config.playbackSpeed)\n        applyReplayGain(config.replayGain)\n        applyDynamics(config.dynamics)\n        setMasterVolume(config.masterVolume)\n        setBalance(config.balance)\n    }\n}"}, {"original_text": "", "replace_all": false}]