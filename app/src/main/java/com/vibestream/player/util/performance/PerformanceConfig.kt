package com.vibestream.player.util.performance

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Performance configuration manager for VibeStream
 */
@Singleton
class PerformanceConfig @Inject constructor(
    private val context: Context
) {
    
    private val deviceCapabilities by lazy { analyzeDeviceCapabilities() }
    
    companion object {
        // Performance thresholds
        const val HIGH_END_RAM_THRESHOLD = 6 * 1024 * 1024 * 1024L // 6GB
        const val MID_RANGE_RAM_THRESHOLD = 3 * 1024 * 1024 * 1024L // 3GB
        const val LOW_END_RAM_THRESHOLD = 2 * 1024 * 1024 * 1024L // 2GB
        
        const val HIGH_END_CPU_CORES = 8
        const val MID_RANGE_CPU_CORES = 4
        
        // Cache sizes based on device tier
        const val HIGH_END_IMAGE_CACHE_SIZE = 100 * 1024 * 1024 // 100MB
        const val MID_RANGE_IMAGE_CACHE_SIZE = 50 * 1024 * 1024 // 50MB
        const val LOW_END_IMAGE_CACHE_SIZE = 25 * 1024 * 1024 // 25MB
        
        const val HIGH_END_AUDIO_BUFFER_SIZE = 4096
        const val MID_RANGE_AUDIO_BUFFER_SIZE = 2048
        const val LOW_END_AUDIO_BUFFER_SIZE = 1024
        
        const val HIGH_END_VIDEO_CACHE_SIZE = 200 * 1024 * 1024 // 200MB
        const val MID_RANGE_VIDEO_CACHE_SIZE = 100 * 1024 * 1024 // 100MB
        const val LOW_END_VIDEO_CACHE_SIZE = 50 * 1024 * 1024 // 50MB
    }

    /**
     * Analyze device capabilities and determine performance tier
     */
    private fun analyzeDeviceCapabilities(): DeviceCapabilities {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val totalMemory = memoryInfo.totalMem
        val availableProcessors = Runtime.getRuntime().availableProcessors()
        val isLowRamDevice = activityManager.isLowRamDevice
        
        // Determine device tier
        val deviceTier = when {
            isLowRamDevice || totalMemory < LOW_END_RAM_THRESHOLD -> DeviceTier.LOW_END
            totalMemory >= HIGH_END_RAM_THRESHOLD && availableProcessors >= HIGH_END_CPU_CORES -> DeviceTier.HIGH_END
            totalMemory >= MID_RANGE_RAM_THRESHOLD && availableProcessors >= MID_RANGE_CPU_CORES -> DeviceTier.MID_RANGE
            else -> DeviceTier.LOW_END
        }
        
        return DeviceCapabilities(
            deviceTier = deviceTier,
            totalMemory = totalMemory,
            availableProcessors = availableProcessors,
            isLowRamDevice = isLowRamDevice,
            androidVersion = Build.VERSION.SDK_INT,
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            supportedAbis = Build.SUPPORTED_ABIS.toList(),
            has64BitSupport = Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()
        )
    }

    /**
     * Get performance settings based on device capabilities
     */
    fun getPerformanceSettings(): PerformanceSettings {
        return when (deviceCapabilities.deviceTier) {
            DeviceTier.HIGH_END -> PerformanceSettings(
                imageCacheSize = HIGH_END_IMAGE_CACHE_SIZE,
                videoCacheSize = HIGH_END_VIDEO_CACHE_SIZE,
                audioBufferSize = HIGH_END_AUDIO_BUFFER_SIZE,
                enableHardwareAcceleration = true,
                enableParallelProcessing = true,
                maxConcurrentTasks = 8,
                preloadNextTrack = true,
                enableAdvancedAudioEffects = true,
                enableVideoFilters = true,
                thumbnailQuality = ThumbnailQuality.HIGH,
                animationsEnabled = true,
                enablePredictiveLoading = true,
                memoryManagementStrategy = MemoryManagementStrategy.AGGRESSIVE_CACHING
            )
            
            DeviceTier.MID_RANGE -> PerformanceSettings(
                imageCacheSize = MID_RANGE_IMAGE_CACHE_SIZE,
                videoCacheSize = MID_RANGE_VIDEO_CACHE_SIZE,
                audioBufferSize = MID_RANGE_AUDIO_BUFFER_SIZE,
                enableHardwareAcceleration = true,
                enableParallelProcessing = true,
                maxConcurrentTasks = 4,
                preloadNextTrack = true,
                enableAdvancedAudioEffects = true,
                enableVideoFilters = false,
                thumbnailQuality = ThumbnailQuality.MEDIUM,
                animationsEnabled = true,
                enablePredictiveLoading = false,
                memoryManagementStrategy = MemoryManagementStrategy.BALANCED
            )
            
            DeviceTier.LOW_END -> PerformanceSettings(
                imageCacheSize = LOW_END_IMAGE_CACHE_SIZE,
                videoCacheSize = LOW_END_VIDEO_CACHE_SIZE,
                audioBufferSize = LOW_END_AUDIO_BUFFER_SIZE,
                enableHardwareAcceleration = false,
                enableParallelProcessing = false,
                maxConcurrentTasks = 2,
                preloadNextTrack = false,
                enableAdvancedAudioEffects = false,
                enableVideoFilters = false,
                thumbnailQuality = ThumbnailQuality.LOW,
                animationsEnabled = false,
                enablePredictiveLoading = false,
                memoryManagementStrategy = MemoryManagementStrategy.CONSERVATIVE
            )
        }
    }

    /**
     * Get device capabilities
     */
    fun getDeviceCapabilities(): DeviceCapabilities = deviceCapabilities

    /**
     * Check if feature should be enabled based on device capabilities
     */
    fun shouldEnableFeature(feature: PerformanceFeature): Boolean {
        val settings = getPerformanceSettings()
        
        return when (feature) {
            PerformanceFeature.HARDWARE_ACCELERATION -> settings.enableHardwareAcceleration
            PerformanceFeature.PARALLEL_PROCESSING -> settings.enableParallelProcessing
            PerformanceFeature.ADVANCED_AUDIO_EFFECTS -> settings.enableAdvancedAudioEffects
            PerformanceFeature.VIDEO_FILTERS -> settings.enableVideoFilters
            PerformanceFeature.ANIMATIONS -> settings.animationsEnabled
            PerformanceFeature.PREDICTIVE_LOADING -> settings.enablePredictiveLoading
            PerformanceFeature.PRELOAD_NEXT_TRACK -> settings.preloadNextTrack
        }
    }

    /**
     * Get optimal thread pool size for background tasks
     */
    fun getOptimalThreadPoolSize(taskType: TaskType): Int {
        val settings = getPerformanceSettings()
        val baseSize = settings.maxConcurrentTasks
        
        return when (taskType) {
            TaskType.IO_INTENSIVE -> minOf(baseSize, 4) // IO tasks don't need many threads
            TaskType.CPU_INTENSIVE -> baseSize // Use all available threads
            TaskType.NETWORK -> minOf(baseSize / 2, 3) // Limited network connections
            TaskType.UI_BACKGROUND -> 1 // UI background tasks should be serialized
        }
    }

    /**
     * Get recommended bitmap configuration based on device capabilities
     */
    fun getRecommendedBitmapConfig(): android.graphics.Bitmap.Config {
        return when (deviceCapabilities.deviceTier) {
            DeviceTier.HIGH_END -> android.graphics.Bitmap.Config.ARGB_8888
            DeviceTier.MID_RANGE -> android.graphics.Bitmap.Config.ARGB_8888
            DeviceTier.LOW_END -> android.graphics.Bitmap.Config.RGB_565
        }
    }

    /**
     * Get startup optimization strategy
     */
    fun getStartupOptimizations(): StartupOptimizations {
        return StartupOptimizations(
            deferNonCriticalInitialization = deviceCapabilities.deviceTier == DeviceTier.LOW_END,
            enableSplashScreenOptimization = true,
            preloadCriticalResources = deviceCapabilities.deviceTier == DeviceTier.HIGH_END,
            enableEarlyLibraryIndexing = deviceCapabilities.deviceTier != DeviceTier.LOW_END,
            backgroundInitializationDelay = when (deviceCapabilities.deviceTier) {
                DeviceTier.HIGH_END -> 100L
                DeviceTier.MID_RANGE -> 500L
                DeviceTier.LOW_END -> 1000L
            }
        )
    }
}

/**
 * Device performance tier classification
 */
enum class DeviceTier {
    LOW_END,
    MID_RANGE,
    HIGH_END
}

/**
 * Performance features that can be enabled/disabled
 */
enum class PerformanceFeature {
    HARDWARE_ACCELERATION,
    PARALLEL_PROCESSING,
    ADVANCED_AUDIO_EFFECTS,
    VIDEO_FILTERS,
    ANIMATIONS,
    PREDICTIVE_LOADING,
    PRELOAD_NEXT_TRACK
}

/**
 * Task types for thread pool optimization
 */
enum class TaskType {
    IO_INTENSIVE,
    CPU_INTENSIVE,
    NETWORK,
    UI_BACKGROUND
}

/**
 * Thumbnail quality levels
 */
enum class ThumbnailQuality {
    LOW,
    MEDIUM,
    HIGH
}

/**
 * Memory management strategies
 */
enum class MemoryManagementStrategy {
    CONSERVATIVE,
    BALANCED,
    AGGRESSIVE_CACHING
}

/**
 * Device capabilities data class
 */
data class DeviceCapabilities(
    val deviceTier: DeviceTier,
    val totalMemory: Long,
    val availableProcessors: Int,
    val isLowRamDevice: Boolean,
    val androidVersion: Int,
    val manufacturer: String,
    val model: String,
    val supportedAbis: List<String>,
    val has64BitSupport: Boolean
)

/**
 * Performance settings data class
 */
data class PerformanceSettings(
    val imageCacheSize: Int,
    val videoCacheSize: Int,
    val audioBufferSize: Int,
    val enableHardwareAcceleration: Boolean,
    val enableParallelProcessing: Boolean,
    val maxConcurrentTasks: Int,
    val preloadNextTrack: Boolean,
    val enableAdvancedAudioEffects: Boolean,
    val enableVideoFilters: Boolean,
    val thumbnailQuality: ThumbnailQuality,
    val animationsEnabled: Boolean,
    val enablePredictiveLoading: Boolean,
    val memoryManagementStrategy: MemoryManagementStrategy
)

/**
 * Startup optimization settings
 */
data class StartupOptimizations(
    val deferNonCriticalInitialization: Boolean,
    val enableSplashScreenOptimization: Boolean,
    val preloadCriticalResources: Boolean,
    val enableEarlyLibraryIndexing: Boolean,
    val backgroundInitializationDelay: Long
)

/**
 * Composable function to get performance config in Compose UI
 */
@Composable
fun rememberPerformanceConfig(): PerformanceConfig {
    val context = LocalContext.current
    return remember { PerformanceConfig(context) }
}

/**
 * Extension functions for easy feature checking
 */
fun PerformanceConfig.isHighEndDevice(): Boolean = 
    getDeviceCapabilities().deviceTier == DeviceTier.HIGH_END

fun PerformanceConfig.isMidRangeDevice(): Boolean = 
    getDeviceCapabilities().deviceTier == DeviceTier.MID_RANGE

fun PerformanceConfig.isLowEndDevice(): Boolean = 
    getDeviceCapabilities().deviceTier == DeviceTier.LOW_END

fun PerformanceConfig.canHandleHeavyOperations(): Boolean = 
    getDeviceCapabilities().deviceTier != DeviceTier.LOW_END

fun PerformanceConfig.getRecommendedPreviewQuality(): String = when (getDeviceCapabilities().deviceTier) {
    DeviceTier.HIGH_END -> "1080p"
    DeviceTier.MID_RANGE -> "720p"
    DeviceTier.LOW_END -> "480p"
}