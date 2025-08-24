package com.vibestream.player.domain.library

import com.vibestream.player.data.model.MediaItem
import com.vibestream.player.data.model.Playlist
import com.vibestream.player.data.model.ScanEvent
import com.vibestream.player.data.model.LibraryQuery
import kotlinx.coroutines.flow.Flow

/**
 * Interface for media library management and querying
 */
interface Library {
    
    /**
     * Scan media files from specified paths
     */
    fun scan(
        paths: List<String>,
        options: ScanOptions = ScanOptions()
    ): Flow<ScanEvent>
    
    /**
     * Query media items with filters, sorting, and pagination
     */
    suspend fun query(query: LibraryQuery): Result<List<MediaItem>>
    
    /**
     * Get all playlists
     */
    suspend fun getPlaylists(): Result<List<Playlist>>
    
    /**
     * Create a new playlist
     */
    suspend fun createPlaylist(name: String): Result<Playlist>
    
    /**
     * Add media item to playlist
     */
    suspend fun addToPlaylist(playlistId: String, mediaId: String): Result<Unit>
    
    /**
     * Remove media item from playlist
     */
    suspend fun removeFromPlaylist(playlistId: String, mediaId: String): Result<Unit>
    
    /**
     * Get media item by ID
     */
    suspend fun getMediaItem(id: String): Result<MediaItem?>
    
    /**
     * Get media items by folder
     */
    suspend fun getMediaItemsByFolder(folderId: String): Result<List<MediaItem>>
    
    /**
     * Search media items by text
     */
    suspend fun searchMediaItems(query: String): Result<List<MediaItem>>
    
    /**
     * Get recently played items
     */
    suspend fun getRecentlyPlayed(limit: Int = 20): Result<List<MediaItem>>
    
    /**
     * Get recently added items
     */
    suspend fun getRecentlyAdded(limit: Int = 20): Result<List<MediaItem>>
    
    /**
     * Get top played items
     */
    suspend fun getTopPlayed(limit: Int = 20): Result<List<MediaItem>>
    
    /**
     * Get all artists
     */
    suspend fun getArtists(): Result<List<String>>
    
    /**
     * Get all albums
     */
    suspend fun getAlbums(): Result<List<String>>
    
    /**
     * Get all genres
     */
    suspend fun getGenres(): Result<List<String>>
    
    /**
     * Update media item metadata
     */
    suspend fun updateMediaItem(mediaItem: MediaItem): Result<Unit>
    
    /**
     * Delete media item from library
     */
    suspend fun deleteMediaItem(id: String): Result<Unit>
    
    /**
     * Mark media item as favorite
     */
    suspend fun setFavorite(id: String, favorite: Boolean): Result<Unit>
    
    /**
     * Set media item rating
     */
    suspend fun setRating(id: String, rating: Float): Result<Unit>
    
    /**
     * Get library statistics
     */
    suspend fun getLibraryStats(): Result<LibraryStats>
}

/**
 * Scan options configuration
 */
data class ScanOptions(
    val includeSubfolders: Boolean = true,
    val excludeHidden: Boolean = true,
    val minFileSize: Long = 1024, // 1KB minimum
    val maxConcurrency: Int = 4,
    val extractThumbnails: Boolean = true,
    val extractWaveforms: Boolean = true,
    val calculateReplayGain: Boolean = true,
    val excludePatterns: List<String> = listOf(
        ".*\\.tmp",
        ".*\\.temp",
        ".*/\\.[^/]*", // Hidden files
        ".*/cache/.*",
        ".*/thumbnails/.*"
    )
)

/**
 * Library statistics
 */
data class LibraryStats(
    val totalItems: Int,
    val videoItems: Int,
    val audioItems: Int,
    val totalSize: Long,
    val totalDuration: Long,
    val lastScanTime: Long?
)