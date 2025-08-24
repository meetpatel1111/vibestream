package com.vibestream.player.util.performance

import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.opengl.GLES20
import android.os.Build
import android.view.Surface
import androidx.annotation.RequiresApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hardware acceleration utilities for optimal media performance
 */
@Singleton
class HardwareAccelerationManager @Inject constructor(
    private val context: Context
) {
    
    private val supportedCodecs = mutableMapOf<String, CodecCapability>()
    private var glCapabilities: OpenGLCapabilities? = null
    
    init {
        detectHardwareCapabilities()
    }

    /**
     * Detect available hardware acceleration capabilities
     */
    private fun detectHardwareCapabilities() {
        detectVideoCodecs()
        detectAudioCodecs()
        // GL capabilities will be detected when GL context is available
    }

    /**
     * Detect hardware video codecs
     */
    private fun detectVideoCodecs() {
        val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
        
        for (codecInfo in codecList.codecInfos) {
            if (!codecInfo.isEncoder) { // We're interested in decoders
                for (type in codecInfo.supportedTypes) {
                    if (type.startsWith("video/")) {
                        val capability = analyzeCodecCapability(codecInfo, type)
                        supportedCodecs[type] = capability
                    }
                }
            }
        }
    }

    /**
     * Detect hardware audio codecs
     */
    private fun detectAudioCodecs() {
        val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
        
        for (codecInfo in codecList.codecInfos) {
            if (!codecInfo.isEncoder) {
                for (type in codecInfo.supportedTypes) {
                    if (type.startsWith("audio/")) {
                        val capability = analyzeCodecCapability(codecInfo, type)
                        supportedCodecs[type] = capability
                    }
                }
            }
        }
    }

    /**
     * Analyze codec capability details
     */
    private fun analyzeCodecCapability(codecInfo: MediaCodecInfo, mimeType: String): CodecCapability {
        val capabilities = codecInfo.getCapabilitiesForType(mimeType)
        val isHardwareAccelerated = isHardwareAccelerated(codecInfo)
        
        return CodecCapability(
            codecName = codecInfo.name,
            mimeType = mimeType,
            isHardwareAccelerated = isHardwareAccelerated,
            isVendor = codecInfo.isVendor,
            isSoftwareOnly = codecInfo.isSoftwareOnly,
            maxSupportedInstances = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                capabilities.maxSupportedInstances
            } else 1,
            videoCapabilities = if (mimeType.startsWith("video/")) {
                extractVideoCapabilities(capabilities)
            } else null,
            audioCapabilities = if (mimeType.startsWith("audio/")) {
                extractAudioCapabilities(capabilities)
            } else null
        )
    }

    /**
     * Check if codec is hardware accelerated
     */
    private fun isHardwareAccelerated(codecInfo: MediaCodecInfo): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            codecInfo.isHardwareAccelerated
        } else {
            // Heuristic for older Android versions
            val name = codecInfo.name.lowercase()
            !name.contains("sw") && 
            !name.contains("software") && 
            (name.contains("qcom") || name.contains("nvidia") || 
             name.contains("exynos") || name.contains("mtk") ||
             name.contains("intel") || name.contains("amd"))
        }
    }

    /**
     * Extract video-specific capabilities
     */
    private fun extractVideoCapabilities(capabilities: MediaCodecInfo.CodecCapabilities): VideoCapabilities? {
        val videoCapabilities = capabilities.videoCapabilities ?: return null
        
        return VideoCapabilities(
            supportedWidths = videoCapabilities.supportedWidths,
            supportedHeights = videoCapabilities.supportedHeights,
            supportedFrameRates = videoCapabilities.supportedFrameRates,
            bitrateRange = videoCapabilities.bitrateRange,
            supportsAdaptivePlayback = capabilities.isFeatureSupported(
                MediaCodecInfo.CodecCapabilities.FEATURE_AdaptivePlayback
            ),
            supportsTunneledPlayback = capabilities.isFeatureSupported(
                MediaCodecInfo.CodecCapabilities.FEATURE_TunneledPlayback
            ),
            supportsSecurePlayback = capabilities.isFeatureSupported(
                MediaCodecInfo.CodecCapabilities.FEATURE_SecurePlayback
            )
        )
    }

    /**
     * Extract audio-specific capabilities
     */
    private fun extractAudioCapabilities(capabilities: MediaCodecInfo.CodecCapabilities): AudioCapabilities? {
        val audioCapabilities = capabilities.audioCapabilities ?: return null
        
        return AudioCapabilities(
            supportedSampleRates = audioCapabilities.supportedSampleRates?.toList() ?: emptyList(),
            maxInputChannelCount = audioCapabilities.maxInputChannelCount,
            bitrateRange = audioCapabilities.bitrateRange
        )
    }

    /**
     * Detect OpenGL capabilities (call when GL context is available)
     */
    fun detectOpenGLCapabilities() {
        glCapabilities = OpenGLCapabilities(
            version = GLES20.glGetString(GLES20.GL_VERSION) ?: "Unknown",
            vendor = GLES20.glGetString(GLES20.GL_VENDOR) ?: "Unknown",
            renderer = GLES20.glGetString(GLES20.GL_RENDERER) ?: "Unknown",
            extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS)?.split(" ") ?: emptyList(),
            maxTextureSize = getMaxTextureSize(),
            supportsETC1 = hasExtension("GL_OES_compressed_ETC1_RGB8_texture"),
            supportsETC2 = hasExtension("GL_OES_compressed_ETC2_RGB8_texture"),
            supportsASTC = hasExtension("GL_KHR_texture_compression_astc_ldr"),
            supportsFloatTextures = hasExtension("GL_OES_texture_float")
        )
    }

    /**
     * Get maximum texture size supported by GPU
     */
    private fun getMaxTextureSize(): Int {
        val maxSize = IntArray(1)
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxSize, 0)
        return maxSize[0]
    }

    /**
     * Check if OpenGL extension is supported
     */
    private fun hasExtension(extensionName: String): Boolean {
        val extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS) ?: return false
        return extensions.contains(extensionName)
    }

    /**
     * Get optimal codec for media type
     */
    fun getOptimalCodec(mimeType: String): CodecCapability? {
        return supportedCodecs[mimeType]?.let { capability ->
            // Prefer hardware accelerated codecs
            if (capability.isHardwareAccelerated) capability else null
        } ?: supportedCodecs[mimeType] // Fallback to any available codec
    }

    /**
     * Check if hardware decoding is supported for media type
     */
    fun supportsHardwareDecoding(mimeType: String): Boolean {
        return supportedCodecs[mimeType]?.isHardwareAccelerated == true
    }

    /**
     * Get recommended video decoder settings
     */
    fun getRecommendedVideoSettings(mimeType: String, width: Int, height: Int): VideoDecoderSettings? {
        val codec = getOptimalCodec(mimeType) ?: return null
        val videoCapabilities = codec.videoCapabilities ?: return null
        
        // Check if resolution is supported
        if (!videoCapabilities.supportedWidths.contains(width) || 
            !videoCapabilities.supportedHeights.contains(height)) {
            return null
        }

        return VideoDecoderSettings(
            codecName = codec.codecName,
            useHardwareAcceleration = codec.isHardwareAccelerated,
            maxInstances = codec.maxSupportedInstances,
            enableAdaptivePlayback = videoCapabilities.supportsAdaptivePlayback,
            enableTunneledPlayback = videoCapabilities.supportsTunneledPlayback && hasSecureHardware(),
            preferredColorFormat = getPreferredColorFormat(codec)
        )
    }

    /**
     * Get recommended audio decoder settings
     */
    fun getRecommendedAudioSettings(mimeType: String, sampleRate: Int, channelCount: Int): AudioDecoderSettings? {
        val codec = getOptimalCodec(mimeType) ?: return null
        val audioCapabilities = codec.audioCapabilities ?: return null
        
        // Check if configuration is supported
        if (!audioCapabilities.supportedSampleRates.contains(sampleRate) ||
            channelCount > audioCapabilities.maxInputChannelCount) {
            return null
        }

        return AudioDecoderSettings(
            codecName = codec.codecName,
            useHardwareAcceleration = codec.isHardwareAccelerated,
            maxInstances = codec.maxSupportedInstances,
            optimalSampleRate = sampleRate,
            maxChannelCount = audioCapabilities.maxInputChannelCount
        )
    }

    /**
     * Get preferred color format for codec
     */
    private fun getPreferredColorFormat(codec: CodecCapability): Int {
        // Return optimal color format based on hardware
        return if (codec.isHardwareAccelerated) {
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        } else {
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar
        }
    }

    /**
     * Check if device has secure hardware for DRM content
     */
    private fun hasSecureHardware(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_VERIFIED_BOOT) ||
               context.packageManager.hasSystemFeature("android.hardware.drm.secure")
    }

    /**
     * Get hardware acceleration capabilities summary
     */
    fun getCapabilitiesSummary(): HardwareCapabilitiesSummary {
        val videoCodecs = supportedCodecs.filterKeys { it.startsWith("video/") }
        val audioCodecs = supportedCodecs.filterKeys { it.startsWith("audio/") }
        
        return HardwareCapabilitiesSummary(
            hardwareVideoCodecs = videoCodecs.filterValues { it.isHardwareAccelerated }.keys.toList(),
            softwareVideoCodecs = videoCodecs.filterValues { !it.isHardwareAccelerated }.keys.toList(),
            hardwareAudioCodecs = audioCodecs.filterValues { it.isHardwareAccelerated }.keys.toList(),
            softwareAudioCodecs = audioCodecs.filterValues { !it.isHardwareAccelerated }.keys.toList(),
            openGLCapabilities = glCapabilities,
            hasSecurePlayback = supportedCodecs.values.any { 
                it.videoCapabilities?.supportsSecurePlayback == true 
            },
            hasTunneledPlayback = supportedCodecs.values.any { 
                it.videoCapabilities?.supportsTunneledPlayback == true 
            }
        )
    }

    /**
     * Export capabilities as readable string
     */
    fun exportCapabilities(): String = buildString {
        val summary = getCapabilitiesSummary()
        
        appendLine("Hardware Acceleration Capabilities")
        appendLine("=================================")
        appendLine()
        
        appendLine("Hardware Video Codecs:")
        summary.hardwareVideoCodecs.forEach { appendLine("  - $it") }
        appendLine()
        
        appendLine("Software Video Codecs:")
        summary.softwareVideoCodecs.forEach { appendLine("  - $it") }
        appendLine()
        
        appendLine("Hardware Audio Codecs:")
        summary.hardwareAudioCodecs.forEach { appendLine("  - $it") }
        appendLine()
        
        appendLine("Software Audio Codecs:")
        summary.softwareAudioCodecs.forEach { appendLine("  - $it") }
        appendLine()
        
        appendLine("Features:")
        appendLine("  - Secure Playback: ${summary.hasSecurePlayback}")
        appendLine("  - Tunneled Playback: ${summary.hasTunneledPlayback}")
        
        summary.openGLCapabilities?.let { gl ->
            appendLine()
            appendLine("OpenGL Capabilities:")
            appendLine("  - Version: ${gl.version}")
            appendLine("  - Vendor: ${gl.vendor}")
            appendLine("  - Renderer: ${gl.renderer}")
            appendLine("  - Max Texture Size: ${gl.maxTextureSize}")
            appendLine("  - ETC1 Compression: ${gl.supportsETC1}")
            appendLine("  - ETC2 Compression: ${gl.supportsETC2}")
            appendLine("  - ASTC Compression: ${gl.supportsASTC}")
            appendLine("  - Float Textures: ${gl.supportsFloatTextures}")
        }
    }
}

// Data classes for hardware capabilities
data class CodecCapability(
    val codecName: String,
    val mimeType: String,
    val isHardwareAccelerated: Boolean,
    val isVendor: Boolean,
    val isSoftwareOnly: Boolean,
    val maxSupportedInstances: Int,
    val videoCapabilities: VideoCapabilities?,
    val audioCapabilities: AudioCapabilities?
)

data class VideoCapabilities(
    val supportedWidths: android.util.Range<Int>,
    val supportedHeights: android.util.Range<Int>,
    val supportedFrameRates: android.util.Range<Double>,
    val bitrateRange: android.util.Range<Int>,
    val supportsAdaptivePlayback: Boolean,
    val supportsTunneledPlayback: Boolean,
    val supportsSecurePlayback: Boolean
)

data class AudioCapabilities(
    val supportedSampleRates: List<Int>,
    val maxInputChannelCount: Int,
    val bitrateRange: android.util.Range<Int>
)

data class OpenGLCapabilities(
    val version: String,
    val vendor: String,
    val renderer: String,
    val extensions: List<String>,
    val maxTextureSize: Int,
    val supportsETC1: Boolean,
    val supportsETC2: Boolean,
    val supportsASTC: Boolean,
    val supportsFloatTextures: Boolean
)

data class VideoDecoderSettings(
    val codecName: String,
    val useHardwareAcceleration: Boolean,
    val maxInstances: Int,
    val enableAdaptivePlayback: Boolean,
    val enableTunneledPlayback: Boolean,
    val preferredColorFormat: Int
)

data class AudioDecoderSettings(
    val codecName: String,
    val useHardwareAcceleration: Boolean,
    val maxInstances: Int,
    val optimalSampleRate: Int,
    val maxChannelCount: Int
)

data class HardwareCapabilitiesSummary(
    val hardwareVideoCodecs: List<String>,
    val softwareVideoCodecs: List<String>,
    val hardwareAudioCodecs: List<String>,
    val softwareAudioCodecs: List<String>,
    val openGLCapabilities: OpenGLCapabilities?,
    val hasSecurePlayback: Boolean,
    val hasTunneledPlayback: Boolean
)