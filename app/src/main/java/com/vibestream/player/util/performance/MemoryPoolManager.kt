package com.vibestream.player.util.performance

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Memory pool manager for efficient buffer allocation and zero-copy operations
 */
@Singleton
class MemoryPoolManager @Inject constructor() {
    
    private val pools = mutableMapOf<BufferSize, MemoryPool>()
    private val stats = PoolStatistics()
    private val mutex = Mutex()
    
    companion object {
        // Standard buffer sizes for different use cases
        val SMALL_BUFFER = BufferSize("small", 4 * 1024) // 4KB for metadata
        val MEDIUM_BUFFER = BufferSize("medium", 64 * 1024) // 64KB for audio frames
        val LARGE_BUFFER = BufferSize("large", 256 * 1024) // 256KB for video frames
        val HUGE_BUFFER = BufferSize("huge", 1024 * 1024) // 1MB for subtitle files
        
        private const val DEFAULT_POOL_SIZE = 10
        private const val MAX_POOL_SIZE = 50
    }
    
    init {
        // Initialize standard pools
        initializePool(SMALL_BUFFER, DEFAULT_POOL_SIZE)
        initializePool(MEDIUM_BUFFER, DEFAULT_POOL_SIZE)
        initializePool(LARGE_BUFFER, DEFAULT_POOL_SIZE / 2)
        initializePool(HUGE_BUFFER, DEFAULT_POOL_SIZE / 4)
    }

    /**
     * Initialize a memory pool for specific buffer size
     */
    private fun initializePool(bufferSize: BufferSize, initialSize: Int) {
        pools[bufferSize] = MemoryPool(bufferSize, initialSize)
    }

    /**
     * Acquire buffer from appropriate pool
     */
    suspend fun acquireBuffer(requestedSize: Int): PooledBuffer {
        val bufferSize = findBestFitBufferSize(requestedSize)
        val pool = pools[bufferSize] ?: run {
            // Create new pool if needed
            initializePool(bufferSize, DEFAULT_POOL_SIZE)
            pools[bufferSize]!!
        }
        
        return pool.acquire().also {
            stats.recordAcquisition(bufferSize)
        }
    }

    /**
     * Acquire buffer of specific type
     */
    suspend fun acquireBuffer(bufferSize: BufferSize): PooledBuffer {
        val pool = pools[bufferSize] ?: run {
            initializePool(bufferSize, DEFAULT_POOL_SIZE)
            pools[bufferSize]!!
        }
        
        return pool.acquire().also {
            stats.recordAcquisition(bufferSize)
        }
    }

    /**
     * Release buffer back to pool
     */
    suspend fun releaseBuffer(buffer: PooledBuffer) {
        val pool = pools[buffer.bufferSize]
        pool?.release(buffer)
        stats.recordRelease(buffer.bufferSize)
    }

    /**
     * Find best fitting buffer size for requested size
     */
    private fun findBestFitBufferSize(requestedSize: Int): BufferSize {
        return when {
            requestedSize <= SMALL_BUFFER.size -> SMALL_BUFFER
            requestedSize <= MEDIUM_BUFFER.size -> MEDIUM_BUFFER
            requestedSize <= LARGE_BUFFER.size -> LARGE_BUFFER
            requestedSize <= HUGE_BUFFER.size -> HUGE_BUFFER
            else -> BufferSize("custom_${requestedSize}", requestedSize)
        }
    }

    /**
     * Get pool statistics
     */
    fun getStatistics(): PoolStatistics = stats.copy()

    /**
     * Clear all pools and reset statistics
     */
    suspend fun clearPools() {
        mutex.withLock {
            pools.values.forEach { it.clear() }
            stats.reset()
        }
    }

    /**
     * Trim pools to remove excess buffers
     */
    suspend fun trimPools() {
        mutex.withLock {
            pools.values.forEach { it.trim() }
        }
    }

    /**
     * Get memory usage summary
     */
    fun getMemoryUsage(): MemoryUsageSummary {
        val poolUsage = pools.map { (bufferSize, pool) ->
            BufferPoolUsage(
                bufferSize = bufferSize,
                totalBuffers = pool.getTotalBuffers(),
                availableBuffers = pool.getAvailableBuffers(),
                usedBuffers = pool.getUsedBuffers(),
                totalMemory = pool.getTotalMemory(),
                usedMemory = pool.getUsedMemory()
            )
        }
        
        return MemoryUsageSummary(
            poolUsage = poolUsage,
            totalAllocatedMemory = poolUsage.sumOf { it.totalMemory },
            totalUsedMemory = poolUsage.sumOf { it.usedMemory },
            statistics = stats.copy()
        )
    }
}

/**
 * Memory pool for specific buffer size
 */
class MemoryPool(
    val bufferSize: BufferSize,
    initialSize: Int
) {
    private val availableBuffers = ConcurrentLinkedQueue<PooledBuffer>()
    private val usedBuffers = mutableSetOf<PooledBuffer>()
    private val totalBuffers = AtomicInteger(0)
    private val mutex = Mutex()
    
    init {
        // Pre-allocate initial buffers
        repeat(initialSize) {
            val buffer = createBuffer()
            availableBuffers.offer(buffer)
            totalBuffers.incrementAndGet()
        }
    }

    /**
     * Acquire buffer from pool
     */
    suspend fun acquire(): PooledBuffer {
        mutex.withLock {
            val buffer = availableBuffers.poll() ?: run {
                // Create new buffer if pool is empty
                val newBuffer = createBuffer()
                totalBuffers.incrementAndGet()
                newBuffer
            }
            
            buffer.buffer.clear() // Reset buffer state
            usedBuffers.add(buffer)
            return buffer
        }
    }

    /**
     * Release buffer back to pool
     */
    suspend fun release(buffer: PooledBuffer) {
        mutex.withLock {
            if (usedBuffers.remove(buffer)) {
                buffer.buffer.clear()
                
                // Don't exceed max pool size
                if (availableBuffers.size < MemoryPoolManager.MAX_POOL_SIZE) {
                    availableBuffers.offer(buffer)
                } else {
                    totalBuffers.decrementAndGet()
                }
            }
        }
    }

    /**
     * Create new buffer
     */
    private fun createBuffer(): PooledBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(bufferSize.size)
        return PooledBuffer(bufferSize, byteBuffer, System.currentTimeMillis())
    }

    /**
     * Clear all buffers
     */
    suspend fun clear() {
        mutex.withLock {
            availableBuffers.clear()
            usedBuffers.clear()
            totalBuffers.set(0)
        }
    }

    /**
     * Trim excess buffers
     */
    suspend fun trim() {
        mutex.withLock {
            val targetSize = MemoryPoolManager.DEFAULT_POOL_SIZE
            while (availableBuffers.size > targetSize) {
                availableBuffers.poll()?.let {
                    totalBuffers.decrementAndGet()
                }
            }
        }
    }

    /**
     * Get total number of buffers
     */
    fun getTotalBuffers(): Int = totalBuffers.get()

    /**
     * Get number of available buffers
     */
    fun getAvailableBuffers(): Int = availableBuffers.size

    /**
     * Get number of used buffers
     */
    fun getUsedBuffers(): Int = usedBuffers.size

    /**
     * Get total memory allocated by this pool
     */
    fun getTotalMemory(): Long = totalBuffers.get().toLong() * bufferSize.size

    /**
     * Get memory currently in use
     */
    fun getUsedMemory(): Long = usedBuffers.size.toLong() * bufferSize.size
}

/**
 * Pooled buffer wrapper
 */
data class PooledBuffer(
    val bufferSize: BufferSize,
    val buffer: ByteBuffer,
    val createdAt: Long
) {
    /**
     * Get effective size for current operation
     */
    fun getEffectiveSize(dataSize: Int): Int = minOf(dataSize, buffer.capacity())

    /**
     * Check if buffer can accommodate data size
     */
    fun canAccommodate(dataSize: Int): Boolean = dataSize <= buffer.capacity()

    /**
     * Write data to buffer
     */
    fun writeData(data: ByteArray, offset: Int = 0, length: Int = data.size): Int {
        val writeSize = minOf(length, buffer.remaining())
        buffer.put(data, offset, writeSize)
        return writeSize
    }

    /**
     * Read data from buffer
     */
    fun readData(data: ByteArray, offset: Int = 0, length: Int = data.size): Int {
        val readSize = minOf(length, buffer.remaining())
        buffer.get(data, offset, readSize)
        return readSize
    }

    /**
     * Get buffer usage percentage
     */
    fun getUsagePercentage(): Float {
        return if (buffer.capacity() > 0) {
            (buffer.position().toFloat() / buffer.capacity()) * 100f
        } else 0f
    }
}

/**
 * Buffer size definition
 */
data class BufferSize(
    val name: String,
    val size: Int
) {
    /**
     * Check if this size can accommodate requested size
     */
    fun canAccommodate(requestedSize: Int): Boolean = requestedSize <= size

    /**
     * Get efficiency for requested size (0-1)
     */
    fun getEfficiency(requestedSize: Int): Float {
        return if (requestedSize <= size) {
            requestedSize.toFloat() / size
        } else 0f
    }
}

/**
 * Pool statistics tracking
 */
data class PoolStatistics(
    private val acquisitions: AtomicLong = AtomicLong(0),
    private val releases: AtomicLong = AtomicLong(0),
    private val cacheHits: AtomicLong = AtomicLong(0),
    private val cacheMisses: AtomicLong = AtomicLong(0)
) {
    
    fun recordAcquisition(bufferSize: BufferSize) {
        acquisitions.incrementAndGet()
    }
    
    fun recordRelease(bufferSize: BufferSize) {
        releases.incrementAndGet()
    }
    
    fun recordCacheHit() {
        cacheHits.incrementAndGet()
    }
    
    fun recordCacheMiss() {
        cacheMisses.incrementAndGet()
    }
    
    fun getAcquisitions(): Long = acquisitions.get()
    fun getReleases(): Long = releases.get()
    fun getCacheHits(): Long = cacheHits.get()
    fun getCacheMisses(): Long = cacheMisses.get()
    
    fun getCacheHitRatio(): Float {
        val totalRequests = cacheHits.get() + cacheMisses.get()
        return if (totalRequests > 0) {
            cacheHits.get().toFloat() / totalRequests
        } else 0f
    }
    
    fun reset() {
        acquisitions.set(0)
        releases.set(0)
        cacheHits.set(0)
        cacheMisses.set(0)
    }
    
    fun copy(): PoolStatistics = PoolStatistics(
        AtomicLong(acquisitions.get()),
        AtomicLong(releases.get()),
        AtomicLong(cacheHits.get()),
        AtomicLong(cacheMisses.get())
    )
}

/**
 * Buffer pool usage information
 */
data class BufferPoolUsage(
    val bufferSize: BufferSize,
    val totalBuffers: Int,
    val availableBuffers: Int,
    val usedBuffers: Int,
    val totalMemory: Long,
    val usedMemory: Long
) {
    val utilizationPercentage: Float
        get() = if (totalBuffers > 0) {
            (usedBuffers.toFloat() / totalBuffers) * 100f
        } else 0f
}

/**
 * Memory usage summary
 */
data class MemoryUsageSummary(
    val poolUsage: List<BufferPoolUsage>,
    val totalAllocatedMemory: Long,
    val totalUsedMemory: Long,
    val statistics: PoolStatistics
) {
    val overallUtilization: Float
        get() = if (totalAllocatedMemory > 0) {
            (totalUsedMemory.toFloat() / totalAllocatedMemory) * 100f
        } else 0f
}

/**
 * Extension functions for easier buffer management
 */
suspend fun MemoryPoolManager.withBuffer(
    size: Int,
    block: suspend (PooledBuffer) -> Unit
) {
    val buffer = acquireBuffer(size)
    try {
        block(buffer)
    } finally {
        releaseBuffer(buffer)
    }
}

suspend fun MemoryPoolManager.withBuffer(
    bufferSize: BufferSize,
    block: suspend (PooledBuffer) -> Unit
) {
    val buffer = acquireBuffer(bufferSize)
    try {
        block(buffer)
    } finally {
        releaseBuffer(buffer)
    }
}