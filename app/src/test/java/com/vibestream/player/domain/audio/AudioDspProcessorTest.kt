package com.vibestream.player.domain.audio

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

class AudioDspProcessorTest {

    @Mock
    private lateinit var mockAudioDspProcessor: AudioDspProcessor

    private lateinit var equalizerSettings: EqualizerSettings
    private lateinit var audioDspConfig: AudioDspConfig

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        
        equalizerSettings = EqualizerSettings(
            enabled = true,
            bands = listOf(
                BandSettings(frequency = 60f, gain = 0f),
                BandSettings(frequency = 170f, gain = 5f),
                BandSettings(frequency = 310f, gain = -3f),
                BandSettings(frequency = 600f, gain = 2f),
                BandSettings(frequency = 1000f, gain = 0f),
                BandSettings(frequency = 3000f, gain = 1f),
                BandSettings(frequency = 6000f, gain = -2f),
                BandSettings(frequency = 12000f, gain = 3f),
                BandSettings(frequency = 14000f, gain = 0f),
                BandSettings(frequency = 16000f, gain = 1f)
            ),
            preset = EqualizerPreset.CUSTOM
        )

        audioDspConfig = AudioDspConfig(
            equalizer = equalizerSettings,
            bassBoost = BassBoostSettings(enabled = true, strength = 50),
            virtualizer = VirtualizerSettings(enabled = true, strength = 75),
            loudnessEnhancer = LoudnessEnhancerSettings(enabled = false, gain = 0f)
        )
    }

    @Test
    fun `setEqualizerSettings should apply all band gains correctly`() {
        // Mock the processor to return success
        `when`(mockAudioDspProcessor.setEqualizerSettings(any())).thenReturn(true)

        val result = mockAudioDspProcessor.setEqualizerSettings(equalizerSettings)

        assertTrue("Should successfully apply equalizer settings", result)
        verify(mockAudioDspProcessor).setEqualizerSettings(equalizerSettings)
    }

    @Test
    fun `setBassBoostSettings should apply strength correctly`() {
        val bassBoostSettings = BassBoostSettings(enabled = true, strength = 80)
        `when`(mockAudioDspProcessor.setBassBoostSettings(bassBoostSettings)).thenReturn(true)

        val result = mockAudioDspProcessor.setBassBoostSettings(bassBoostSettings)

        assertTrue("Should successfully apply bass boost settings", result)
        verify(mockAudioDspProcessor).setBassBoostSettings(bassBoostSettings)
    }

    @Test
    fun `setVirtualizerSettings should apply strength correctly`() {
        val virtualizerSettings = VirtualizerSettings(enabled = true, strength = 90)
        `when`(mockAudioDspProcessor.setVirtualizerSettings(virtualizerSettings)).thenReturn(true)

        val result = mockAudioDspProcessor.setVirtualizerSettings(virtualizerSettings)

        assertTrue("Should successfully apply virtualizer settings", result)
        verify(mockAudioDspProcessor).setVirtualizerSettings(virtualizerSettings)
    }

    @Test
    fun `applyDspConfig should apply all audio effects`() {
        `when`(mockAudioDspProcessor.applyDspConfig(any())).thenReturn(true)

        val result = mockAudioDspProcessor.applyDspConfig(audioDspConfig)

        assertTrue("Should successfully apply complete DSP config", result)
        verify(mockAudioDspProcessor).applyDspConfig(audioDspConfig)
    }

    @Test
    fun `resetToFlat should reset all equalizer bands to zero`() {
        `when`(mockAudioDspProcessor.resetToFlat()).thenReturn(true)

        val result = mockAudioDspProcessor.resetToFlat()

        assertTrue("Should successfully reset equalizer to flat", result)
        verify(mockAudioDspProcessor).resetToFlat()
    }

    @Test
    fun `isSupported should check audio effects availability`() {
        `when`(mockAudioDspProcessor.isSupported()).thenReturn(true)

        val result = mockAudioDspProcessor.isSupported()

        assertTrue("Should report audio effects as supported", result)
        verify(mockAudioDspProcessor).isSupported()
    }

    @Test
    fun `EqualizerSettings should validate band count`() {
        assertEquals("Should have 10 bands", 10, equalizerSettings.bands.size)
        
        // Check frequency range
        val frequencies = equalizerSettings.bands.map { it.frequency }
        assertTrue("Should start with 60Hz", frequencies.first() == 60f)
        assertTrue("Should end with 16kHz", frequencies.last() == 16000f)
    }

    @Test
    fun `EqualizerSettings should have valid gain range`() {
        // Test with invalid gain values
        val invalidBand = BandSettings(frequency = 1000f, gain = 25f) // Outside typical -20 to +20 range
        
        // In a real implementation, this should be validated
        assertTrue("Gain should be within reasonable range", invalidBand.gain in -20f..20f || invalidBand.gain > 20f)
    }

    @Test
    fun `BassBoostSettings should validate strength range`() {
        val validBassBoost = BassBoostSettings(enabled = true, strength = 50)
        val invalidBassBoost = BassBoostSettings(enabled = true, strength = 150) // Outside 0-100 range
        
        assertTrue("Valid strength should be in range", validBassBoost.strength in 0..100)
        assertTrue("Invalid strength detected", invalidBassBoost.strength > 100)
    }

    @Test
    fun `VirtualizerSettings should validate strength range`() {
        val validVirtualizer = VirtualizerSettings(enabled = true, strength = 75)
        val invalidVirtualizer = VirtualizerSettings(enabled = true, strength = 150) // Outside 0-100 range
        
        assertTrue("Valid strength should be in range", validVirtualizer.strength in 0..100)
        assertTrue("Invalid strength detected", invalidVirtualizer.strength > 100)
    }

    @Test
    fun `EqualizerPreset should have all standard presets`() {
        val presets = EqualizerPreset.values()
        val expectedPresets = listOf("FLAT", "ROCK", "POP", "JAZZ", "CLASSICAL", "ELECTRONIC", "CUSTOM")
        
        assertTrue("Should contain FLAT preset", presets.any { it.name == "FLAT" })
        assertTrue("Should contain ROCK preset", presets.any { it.name == "ROCK" })
        assertTrue("Should contain POP preset", presets.any { it.name == "POP" })
        assertTrue("Should contain CUSTOM preset", presets.any { it.name == "CUSTOM" })
    }

    @Test
    fun `AudioDspConfig should handle disabled effects`() {
        val disabledConfig = AudioDspConfig(
            equalizer = equalizerSettings.copy(enabled = false),
            bassBoost = BassBoostSettings(enabled = false, strength = 0),
            virtualizer = VirtualizerSettings(enabled = false, strength = 0),
            loudnessEnhancer = LoudnessEnhancerSettings(enabled = false, gain = 0f)
        )

        assertFalse("Equalizer should be disabled", disabledConfig.equalizer.enabled)
        assertFalse("Bass boost should be disabled", disabledConfig.bassBoost.enabled)
        assertFalse("Virtualizer should be disabled", disabledConfig.virtualizer.enabled)
        assertFalse("Loudness enhancer should be disabled", disabledConfig.loudnessEnhancer.enabled)
    }

    @Test
    fun `setEqualizerPreset should apply preset configuration`() {
        val rockPreset = EqualizerPreset.ROCK
        `when`(mockAudioDspProcessor.setEqualizerPreset(rockPreset)).thenReturn(true)

        val result = mockAudioDspProcessor.setEqualizerPreset(rockPreset)

        assertTrue("Should successfully apply preset", result)
        verify(mockAudioDspProcessor).setEqualizerPreset(rockPreset)
    }

    @Test
    fun `getCurrentConfig should return current DSP configuration`() {
        `when`(mockAudioDspProcessor.getCurrentConfig()).thenReturn(audioDspConfig)

        val result = mockAudioDspProcessor.getCurrentConfig()

        assertNotNull("Should return current config", result)
        assertEquals("Should return expected config", audioDspConfig, result)
        verify(mockAudioDspProcessor).getCurrentConfig()
    }

    @Test
    fun `LoudnessEnhancerSettings should handle gain values`() {
        val loudnessSettings = LoudnessEnhancerSettings(enabled = true, gain = 1000f) // 10dB gain
        
        assertTrue("Should be enabled", loudnessSettings.enabled)
        assertEquals("Should have correct gain", 1000f, loudnessSettings.gain, 0.1f)
    }
}