package com.vibestream.player.util.performance

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.util.LruCache
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Performance optimization utilities for VibeStream
 */
@Singleton
class PerformanceManager @Inject constructor(
    private val context: Context
) {
    private val memoryCache = LruCache<String, Any>(getMemoryCacheSize())
    private val weakReferenceCache = ConcurrentHashMap<String, WeakReference<Any>>()
    private val performanceMonitor = PerformanceMonitor()
    
    companion object {
        private const val MEMORY_CACHE_PERCENTAGE = 0.125f // 12.5% of available memory
        private const val MAX_CACHE_SIZE = 50 * 1024 * 1024 // 50MB max
        private const val MIN_CACHE_SIZE = 4 * 1024 * 1024 // 4MB min
    }

    /**
     * Calculate optimal memory cache size based on available memory
     */
    private fun getMemoryCacheSize(): Int {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val availableMemory = memoryInfo.availMem
        val cacheSize = (availableMemory * MEMORY_CACHE_PERCENTAGE).toInt()
        
        return cacheSize.coerceIn(MIN_CACHE_SIZE, MAX_CACHE_SIZE)
    }

    /**
     * Get cached object or compute and cache it
     */
    inline fun <T> getOrCompute(key: String, crossinline compute: suspend () -> T): T {
        // Try memory cache first
        memoryCache.get(key)?.let { return it as T }
        
        // Try weak reference cache
        weakReferenceCache[key]?.get()?.let { return it as T }
        
        // Compute and cache
        return runBlocking {
            val result = compute()
            putInCache(key, result)
            result
        }
    }

    /**
     * Put object in appropriate cache based on size estimation
     */
    fun putInCache(key: String, value: Any) {
        val estimatedSize = estimateObjectSize(value)
        
        if (estimatedSize < 1024 * 1024) { // Less than 1MB, use memory cache
            memoryCache.put(key, value)
        } else { // Larger objects use weak reference cache
            weakReferenceCache[key] = WeakReference(value)
        }
    }

    /**
     * Estimate object size for cache decisions
     */
    private fun estimateObjectSize(obj: Any): Int = when (obj) {
        is String -> obj.length * 2 // Rough estimate for UTF-16
        is ByteArray -> obj.size
        is List<*> -> obj.size * 32 // Rough estimate
        is Map<*, *> -> obj.size * 64 // Rough estimate
        else -> 1024 // Default 1KB estimate
    }

    /**
     * Clear all caches
     */
    fun clearCaches() {
        memoryCache.evictAll()
        weakReferenceCache.clear()
        System.gc() // Suggest garbage collection
    }

    /**
     * Get memory usage statistics
     */
    fun getMemoryStats(): MemoryStats {
        val runtime = Runtime.getRuntime()
        val nativeHeap = Debug.getNativeHeapSize()
        val nativeAllocated = Debug.getNativeHeapAllocatedSize()
        
        return MemoryStats(
            totalMemory = runtime.totalMemory(),
            freeMemory = runtime.freeMemory(),
            usedMemory = runtime.totalMemory() - runtime.freeMemory(),
            maxMemory = runtime.maxMemory(),
            nativeHeapSize = nativeHeap,
            nativeHeapAllocated = nativeAllocated,
            cacheMemoryUsed = memoryCache.size(),
            weakCacheSize = weakReferenceCache.size
        )
    }

    /**
     * Monitor memory pressure and clean up if needed
     */
    suspend fun monitorMemoryPressure(): Flow<MemoryPressureLevel> = flow {
        while (true) {
            val stats = getMemoryStats()
            val usageRatio = stats.usedMemory.toFloat() / stats.maxMemory
            
            val pressureLevel = when {
                usageRatio > 0.9f -> MemoryPressureLevel.CRITICAL
                usageRatio > 0.7f -> MemoryPressureLevel.HIGH
                usageRatio > 0.5f -> MemoryPressureLevel.MODERATE
                else -> MemoryPressureLevel.LOW
            }
            
            emit(pressureLevel)
            
            // Auto cleanup on high pressure
            if (pressureLevel >= MemoryPressureLevel.HIGH) {
                performMemoryCleanup(pressureLevel)
            }
            
            delay(5000) // Check every 5 seconds
        }
    }

    /**
     * Perform memory cleanup based on pressure level
     */
    private fun performMemoryCleanup(level: MemoryPressureLevel) {
        when (level) {
            MemoryPressureLevel.CRITICAL -> {
                // Aggressive cleanup
                memoryCache.evictAll()
                weakReferenceCache.clear()
                System.gc()
            }
            MemoryPressureLevel.HIGH -> {
                // Moderate cleanup
                memoryCache.trimToSize(memoryCache.size() / 2)
                // Remove dead weak references
                weakReferenceCache.entries.removeAll { it.value.get() == null }
            }
            else -> {
                // Light cleanup
                weakReferenceCache.entries.removeAll { it.value.get() == null }
            }
        }
    }

    /**
     * Get performance monitor instance
     */
    fun getPerformanceMonitor(): PerformanceMonitor = performanceMonitor
}

/**
 * Performance monitoring utilities
 */
class PerformanceMonitor {
    private val metrics = ConcurrentHashMap<String, PerformanceMetric>()
    
    /**
     * Start timing an operation
     */
    fun startTiming(operationName: String): TimingToken {
        val startTime = System.nanoTime()
        return TimingToken(operationName, startTime, this)
    }

    /**
     * Record timing for an operation
     */
    internal fun recordTiming(operationName: String, durationNanos: Long) {
        val metric = metrics.getOrPut(operationName) { PerformanceMetric(operationName) }
        metric.addSample(durationNanos)
    }

    /**
     * Get performance metrics for all operations
     */
    fun getAllMetrics(): Map<String, PerformanceMetric> = metrics.toMap()

    /**
     * Get metrics for specific operation
     */
    fun getMetric(operationName: String): PerformanceMetric? = metrics[operationName]

    /**
     * Clear all metrics
     */
    fun clearMetrics() = metrics.clear()

    /**
     * Export metrics as readable string
     */
    fun exportMetrics(): String = buildString {
        appendLine("Performance Metrics:")
        appendLine("==================")
        metrics.values.sortedBy { it.operationName }.forEach { metric ->
            appendLine(metric.toString())
        }
    }
}

/**
 * Token for timing operations
 */
class TimingToken(
    private val operationName: String,
    private val startTime: Long,
    private val monitor: PerformanceMonitor
) {
    /**
     * Stop timing and record the duration
     */
    fun stop() {
        val endTime = System.nanoTime()
        val duration = endTime - startTime
        monitor.recordTiming(operationName, duration)
    }
}

/**
 * Performance metric for tracking operation statistics
 */
class PerformanceMetric(val operationName: String) {
    private var sampleCount = 0L
    private var totalTime = 0L
    private var minTime = Long.MAX_VALUE
    private var maxTime = Long.MIN_VALUE
    
    /**
     * Add a timing sample
     */
    fun addSample(durationNanos: Long) {
        sampleCount++
        totalTime += durationNanos
        minTime = minOf(minTime, durationNanos)
        maxTime = maxOf(maxTime, durationNanos)
    }

    /**
     * Get average time in milliseconds
     */
    fun getAverageTimeMs(): Double = if (sampleCount > 0) {
        (totalTime / sampleCount) / 1_000_000.0
    } else 0.0

    /**
     * Get minimum time in milliseconds
     */
    fun getMinTimeMs(): Double = if (minTime != Long.MAX_VALUE) {
        minTime / 1_000_000.0
    } else 0.0

    /**
     * Get maximum time in milliseconds
     */
    fun getMaxTimeMs(): Double = if (maxTime != Long.MIN_VALUE) {
        maxTime / 1_000_000.0
    } else 0.0

    /**
     * Get total time in milliseconds
     */
    fun getTotalTimeMs(): Double = totalTime / 1_000_000.0

    /**
     * Get sample count
     */
    fun getSampleCount(): Long = sampleCount

    override fun toString(): String = buildString {
        appendLine("Operation: $operationName")
        appendLine("  Samples: $sampleCount")
        appendLine("  Average: ${"%.2f".format(getAverageTimeMs())}ms")
        appendLine("  Min: ${"%.2f".format(getMinTimeMs())}ms")
        appendLine("  Max: ${"%.2f".format(getMaxTimeMs())}ms")
        appendLine("  Total: ${"%.2f".format(getTotalTimeMs())}ms")
    }
}

/**
 * Memory statistics data class
 */
data class MemoryStats(
    val totalMemory: Long,
    val freeMemory: Long,
    val usedMemory: Long,
    val maxMemory: Long,
    val nativeHeapSize: Long,
    val nativeHeapAllocated: Long,
    val cacheMemoryUsed: Int,
    val weakCacheSize: Int
)

/**
 * Memory pressure levels
 */
enum class MemoryPressureLevel {
    LOW, MODERATE, HIGH, CRITICAL
}

/**
 * Extension functions for easy performance monitoring
 */
inline fun <T> PerformanceMonitor.time(operationName: String, block: () -> T): T {
    val token = startTiming(operationName)
    return try {
        block()
    } finally {
        token.stop()
    }
}

/**
 * Coroutine-aware performance monitoring
 */
suspend inline fun <T> PerformanceMonitor.timeSuspend(operationName: String, crossinline block: suspend () -> T): T {
    val token = startTiming(operationName)
    return try {
        block()
    } finally {
        token.stop()
    }
}