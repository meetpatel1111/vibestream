package com.vibestream.player.domain.audio

/**
 * Audio DSP processor interface
 */
interface AudioProcessor {
    fun initialize(): Boolean
    fun release()
    fun setEnabled(enabled: Boolean)
    fun isEnabled(): Boolean
}

/**
 * 10-band equalizer configuration
 */
data class EqualizerSettings(
    val enabled: Boolean = false,
    val preamp: Float = 0f, // -20dB to +20dB
    val bands: List<EqualizerBand> = createDefaultBands()
) {
    companion object {
        fun createDefaultBands(): List<EqualizerBand> {
            return listOf(
                EqualizerBand(frequency = 31, gain = 0f),
                EqualizerBand(frequency = 62, gain = 0f),
                EqualizerBand(frequency = 125, gain = 0f),
                EqualizerBand(frequency = 250, gain = 0f),
                EqualizerBand(frequency = 500, gain = 0f),
                EqualizerBand(frequency = 1000, gain = 0f),
                EqualizerBand(frequency = 2000, gain = 0f),
                EqualizerBand(frequency = 4000, gain = 0f),
                EqualizerBand(frequency = 8000, gain = 0f),
                EqualizerBand(frequency = 16000, gain = 0f)
            )
        }
        
        // Preset equalizer configurations
        val PRESET_FLAT = EqualizerSettings(enabled = true)
        val PRESET_ROCK = EqualizerSettings(
            enabled = true,
            bands = listOf(
                EqualizerBand(31, 8f), EqualizerBand(62, 4f), EqualizerBand(125, -5f),
                EqualizerBand(250, -8f), EqualizerBand(500, -3f), EqualizerBand(1000, 4f),
                EqualizerBand(2000, 8f), EqualizerBand(4000, 11f), EqualizerBand(8000, 11f),
                EqualizerBand(16000, 11f)
            )
        )
        val PRESET_POP = EqualizerSettings(
            enabled = true,
            bands = listOf(
                EqualizerBand(31, -1f), EqualizerBand(62, 4f), EqualizerBand(125, 7f),
                EqualizerBand(250, 8f), EqualizerBand(500, 5f), EqualizerBand(1000, 0f),
                EqualizerBand(2000, -2f), EqualizerBand(4000, -2f), EqualizerBand(8000, -1f),
                EqualizerBand(16000, -1f)
            )
        )
        val PRESET_JAZZ = EqualizerSettings(
            enabled = true,
            bands = listOf(
                EqualizerBand(31, 4f), EqualizerBand(62, 2f), EqualizerBand(125, 0f),
                EqualizerBand(250, 2f), EqualizerBand(500, -2f), EqualizerBand(1000, -2f),
                EqualizerBand(2000, 0f), EqualizerBand(4000, 2f), EqualizerBand(8000, 4f),
                EqualizerBand(16000, 5f)
            )
        )
        val PRESET_CLASSICAL = EqualizerSettings(
            enabled = true,
            bands = listOf(
                EqualizerBand(31, 0f), EqualizerBand(62, 0f), EqualizerBand(125, 0f),
                EqualizerBand(250, 0f), EqualizerBand(500, 0f), EqualizerBand(1000, 0f),
                EqualizerBand(2000, -7f), EqualizerBand(4000, -7f), EqualizerBand(8000, -7f),
                EqualizerBand(16000, -9f)
            )
        )
    }
}

/**
 * Individual equalizer band
 */
data class EqualizerBand(
    val frequency: Int, // Hz
    val gain: Float     // dB (-20 to +20)
)

/**
 * Bass boost configuration
 */
data class BassBoostSettings(
    val enabled: Boolean = false,
    val strength: Float = 0f // 0.0 to 1.0
)

/**
 * Virtualizer (3D audio) configuration
 */
data class VirtualizerSettings(
    val enabled: Boolean = false,
    val strength: Float = 0f // 0.0 to 1.0
)

/**
 * Playback speed configuration with tempo preservation
 */
data class PlaybackSpeedSettings(
    val speed: Float = 1.0f,        // 0.25x to 4.0x
    val preservePitch: Boolean = true,
    val preserveTempo: Boolean = true
)

/**
 * ReplayGain settings for volume normalization
 */
data class ReplayGainSettings(
    val enabled: Boolean = false,
    val mode: ReplayGainMode = ReplayGainMode.TRACK,
    val preamp: Float = 0f,          // -20dB to +20dB
    val preventClipping: Boolean = true
)

enum class ReplayGainMode {
    OFF, TRACK, ALBUM
}

/**
 * Dynamic range compression/limiting
 */
data class DynamicsSettings(
    val limiterEnabled: Boolean = false,
    val limiterThreshold: Float = -6f,  // dB
    val compressorEnabled: Boolean = false,
    val compressorRatio: Float = 4f,     // 1:1 to 10:1
    val compressorThreshold: Float = -12f // dB
)

/**
 * Complete audio configuration
 */
data class AudioDspConfig(
    val equalizer: EqualizerSettings = EqualizerSettings(),
    val bassBoost: BassBoostSettings = BassBoostSettings(),
    val virtualizer: VirtualizerSettings = VirtualizerSettings(),
    val playbackSpeed: PlaybackSpeedSettings = PlaybackSpeedSettings(),
    val replayGain: ReplayGainSettings = ReplayGainSettings(),
    val dynamics: DynamicsSettings = DynamicsSettings(),
    val masterVolume: Float = 1.0f,
    val balance: Float = 0.0f, // -1.0 (left) to 1.0 (right)
    val crossfadeTime: Long = 0L // milliseconds
)

/**
 * Audio effect processor
 */
interface AudioEffectProcessor : AudioProcessor {
    fun applyEqualizer(settings: EqualizerSettings)
    fun applyBassBoost(settings: BassBoostSettings)
    fun applyVirtualizer(settings: VirtualizerSettings)
    fun applyPlaybackSpeed(settings: PlaybackSpeedSettings)
    fun applyReplayGain(settings: ReplayGainSettings)
    fun applyDynamics(settings: DynamicsSettings)
    fun setMasterVolume(volume: Float)
    fun setBalance(balance: Float)
}

/**
 * Audio analyzer for waveform and spectrum data
 */
interface AudioAnalyzer : AudioProcessor {
    fun getWaveformData(): FloatArray?
    fun getSpectrumData(): FloatArray?
    fun getRmsLevel(): Float
    fun getPeakLevel(): Float
}

/**
 * Audio session ID provider for effect attachment
 */
interface AudioSessionProvider {
    fun getAudioSessionId(): Int
}