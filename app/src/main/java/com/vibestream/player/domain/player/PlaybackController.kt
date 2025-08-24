package com.vibestream.player.domain.player

import com.vibestream.player.data.model.MediaItem
import com.vibestream.player.data.model.PlaybackState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates playback operations and manages queue state
 */
@Singleton
class PlaybackController @Inject constructor(
    private val player: Player
) {
    
    private val _queue = MutableStateFlow<List<MediaItem>>(emptyList())
    val queue: StateFlow<List<MediaItem>> = _queue.asStateFlow()
    
    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()
    
    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()
    
    private var originalQueue: List<MediaItem> = emptyList()
    private var shuffledIndices: List<Int> = emptyList()
    
    val playbackState: Flow<PlaybackState> = player.playbackState
    val currentPosition: Flow<Long> = player.currentPosition
    val duration: Flow<Long> = player.duration
    val playbackSpeed: Flow<Float> = player.playbackSpeed
    
    /**
     * Set the playback queue
     */
    suspend fun setQueue(items: List<MediaItem>, startIndex: Int = 0) {
        originalQueue = items
        _queue.value = items
        _currentIndex.value = startIndex.coerceIn(0, items.size - 1)
        
        if (items.isNotEmpty()) {
            loadCurrentItem()
        }
    }
    
    /**
     * Add items to the queue
     */
    suspend fun addToQueue(items: List<MediaItem>) {
        val currentQueue = _queue.value.toMutableList()
        currentQueue.addAll(items)
        _queue.value = currentQueue
        originalQueue = currentQueue
        
        if (_shuffleEnabled.value) {
            regenerateShuffledIndices()
        }
    }
    
    /**
     * Insert item at specific position
     */
    suspend fun insertAt(index: Int, item: MediaItem) {
        val currentQueue = _queue.value.toMutableList()
        val insertIndex = index.coerceIn(0, currentQueue.size)
        currentQueue.add(insertIndex, item)
        _queue.value = currentQueue
        originalQueue = currentQueue
        
        // Adjust current index if necessary
        if (insertIndex <= _currentIndex.value) {
            _currentIndex.value = _currentIndex.value + 1
        }
        
        if (_shuffleEnabled.value) {
            regenerateShuffledIndices()
        }
    }
    
    /**
     * Remove item from queue
     */
    suspend fun removeAt(index: Int) {
        val currentQueue = _queue.value.toMutableList()
        if (index in currentQueue.indices) {
            currentQueue.removeAt(index)
            _queue.value = currentQueue
            originalQueue = currentQueue
            
            // Adjust current index if necessary
            when {
                index < _currentIndex.value -> _currentIndex.value = _currentIndex.value - 1
                index == _currentIndex.value && _currentIndex.value >= currentQueue.size -> {
                    _currentIndex.value = (currentQueue.size - 1).coerceAtLeast(0)
                    if (currentQueue.isNotEmpty()) loadCurrentItem()
                }
            }
            
            if (_shuffleEnabled.value) {
                regenerateShuffledIndices()
            }
        }
    }
    
    /**
     * Play item at specific index
     */
    suspend fun playAt(index: Int) {
        val currentQueue = _queue.value
        if (index in currentQueue.indices) {
            _currentIndex.value = index
            loadCurrentItem()
            player.play()
        }
    }
    
    /**
     * Play next item in queue
     */
    suspend fun next() {
        val currentQueue = _queue.value
        if (currentQueue.isEmpty()) return
        
        val nextIndex = if (_shuffleEnabled.value) {
            getNextShuffledIndex()
        } else {
            (_currentIndex.value + 1) % currentQueue.size
        }
        
        _currentIndex.value = nextIndex
        loadCurrentItem()
        player.play()
    }
    
    /**
     * Play previous item in queue
     */
    suspend fun previous() {
        val currentQueue = _queue.value
        if (currentQueue.isEmpty()) return
        
        val prevIndex = if (_shuffleEnabled.value) {
            getPreviousShuffledIndex()
        } else {
            if (_currentIndex.value == 0) currentQueue.size - 1 else _currentIndex.value - 1
        }
        
        _currentIndex.value = prevIndex
        loadCurrentItem()
        player.play()
    }
    
    /**
     * Enable/disable shuffle mode
     */
    fun setShuffle(enabled: Boolean) {
        _shuffleEnabled.value = enabled
        
        if (enabled) {
            generateShuffledIndices()
        } else {
            shuffledIndices = emptyList()
        }
    }
    
    /**
     * Move queue item from one position to another
     */
    suspend fun moveItem(fromIndex: Int, toIndex: Int) {
        val currentQueue = _queue.value.toMutableList()
        if (fromIndex in currentQueue.indices && toIndex in currentQueue.indices) {
            val item = currentQueue.removeAt(fromIndex)
            currentQueue.add(toIndex, item)
            _queue.value = currentQueue
            originalQueue = currentQueue
            
            // Update current index
            when {
                fromIndex == _currentIndex.value -> _currentIndex.value = toIndex
                fromIndex < _currentIndex.value && toIndex >= _currentIndex.value -> 
                    _currentIndex.value = _currentIndex.value - 1
                fromIndex > _currentIndex.value && toIndex <= _currentIndex.value -> 
                    _currentIndex.value = _currentIndex.value + 1
            }
            
            if (_shuffleEnabled.value) {
                regenerateShuffledIndices()
            }
        }
    }
    
    /**
     * Clear the queue
     */
    fun clearQueue() {
        _queue.value = emptyList()
        originalQueue = emptyList()
        shuffledIndices = emptyList()
        _currentIndex.value = 0
        player.stop()
    }
    
    /**
     * Get current playing item
     */
    fun getCurrentItem(): MediaItem? {
        val currentQueue = _queue.value
        return if (_currentIndex.value in currentQueue.indices) {
            currentQueue[_currentIndex.value]
        } else null
    }
    
    // Player control delegation
    fun play() = player.play()
    fun pause() = player.pause()
    fun stop() = player.stop()
    fun seek(positionMs: Long) = player.seek(positionMs)
    fun setPlaybackSpeed(speed: Float) = player.setPlaybackSpeed(speed)
    fun setVolume(volume: Float) = player.setVolume(volume)
    fun setRepeatMode(mode: RepeatMode) = player.setRepeatMode(mode)
    fun setAudioTrack(trackId: String?) = player.setAudioTrack(trackId)
    fun setSubtitleTrack(trackId: String?) = player.setSubtitleTrack(trackId)
    fun setAudioFilters(filters: AudioFilters) = player.setAudioFilters(filters)
    fun setVideoFilters(filters: VideoFilters) = player.setVideoFilters(filters)
    
    private suspend fun loadCurrentItem() {
        getCurrentItem()?.let { item ->
            player.load(item)
        }
    }
    
    private fun generateShuffledIndices() {
        val currentQueue = _queue.value
        if (currentQueue.isEmpty()) return
        
        shuffledIndices = currentQueue.indices.shuffled()
        
        // Ensure current item is first in shuffle
        val currentShuffleIndex = shuffledIndices.indexOf(_currentIndex.value)
        if (currentShuffleIndex > 0) {
            shuffledIndices = shuffledIndices.toMutableList().apply {
                removeAt(currentShuffleIndex)
                add(0, _currentIndex.value)
            }
        }
    }
    
    private fun regenerateShuffledIndices() {
        if (_shuffleEnabled.value) {
            generateShuffledIndices()
        }
    }
    
    private fun getNextShuffledIndex(): Int {
        if (shuffledIndices.isEmpty()) return 0
        
        val currentShufflePosition = shuffledIndices.indexOf(_currentIndex.value)
        val nextShufflePosition = (currentShufflePosition + 1) % shuffledIndices.size
        return shuffledIndices[nextShufflePosition]
    }
    
    private fun getPreviousShuffledIndex(): Int {
        if (shuffledIndices.isEmpty()) return 0
        
        val currentShufflePosition = shuffledIndices.indexOf(_currentIndex.value)
        val prevShufflePosition = if (currentShufflePosition == 0) {
            shuffledIndices.size - 1
        } else {
            currentShufflePosition - 1
        }
        return shuffledIndices[prevShufflePosition]
    }
}