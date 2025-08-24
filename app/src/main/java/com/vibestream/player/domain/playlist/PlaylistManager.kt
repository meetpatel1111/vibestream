package com.vibestream.player.domain.playlist

import com.vibestream.player.data.database.dao.MediaItemDao
import com.vibestream.player.data.database.dao.PlaylistDao
import com.vibestream.player.data.database.entity.PlaylistEntity
import com.vibestream.player.data.model.MediaItem
import com.vibestream.player.data.model.MediaType
import com.vibestream.player.data.model.Playlist
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Smart playlist types
 */
enum class SmartPlaylistType {
    RECENTLY_ADDED,
    RECENTLY_PLAYED, 
    TOP_RATED,
    MOST_PLAYED,
    FAVORITES,
    RANDOM_MIX,
    NEVER_PLAYED,
    LONG_TRACKS,
    SHORT_TRACKS,
    HD_VIDEOS,
    LOSSLESS_AUDIO
}

/**
 * Playlist manager for creating, managing, and generating smart playlists
 */
@Singleton
class PlaylistManager @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val mediaItemDao: MediaItemDao
) {
    
    /**
     * Get all playlists including smart playlists
     */
    fun getAllPlaylists(): Flow<List<Playlist>> {
        return playlistDao.getAllPlaylistsFlow().map { entities ->
            val playlists = entities.map { entity ->
                Playlist(
                    id = entity.id,
                    name = entity.name,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt,
                    thumbnailPath = entity.thumbnailPath,
                    isSmartPlaylist = entity.isSmartPlaylist,
                    smartQuery = entity.smartQuery
                )
            }.toMutableList()
            
            // Add built-in smart playlists if they don't exist
            addBuiltInSmartPlaylists(playlists)
            
            playlists
        }
    }
    
    /**
     * Create a new user playlist
     */
    suspend fun createPlaylist(name: String): Result<Playlist> {
        return try {
            val playlist = PlaylistEntity(
                id = generatePlaylistId(),
                name = name,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            playlistDao.insertPlaylist(playlist)
            
            Result.success(
                Playlist(
                    id = playlist.id,
                    name = playlist.name,
                    createdAt = playlist.createdAt,
                    updatedAt = playlist.updatedAt
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete a playlist
     */
    suspend fun deletePlaylist(playlistId: String): Result<Unit> {
        return try {
            playlistDao.deletePlaylistById(playlistId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Add media item to playlist
     */
    suspend fun addToPlaylist(playlistId: String, mediaId: String): Result<Unit> {
        return try {
            playlistDao.addToPlaylist(playlistId, mediaId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Remove media item from playlist
     */
    suspend fun removeFromPlaylist(playlistId: String, mediaId: String): Result<Unit> {
        return try {
            playlistDao.removeFromPlaylist(playlistId, mediaId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get playlist contents
     */
    suspend fun getPlaylistContents(playlistId: String): Result<List<MediaItem>> {
        return try {
            val items = when {
                isSmartPlaylist(playlistId) -> generateSmartPlaylistContents(playlistId)
                else -> {
                    val entities = playlistDao.getPlaylistMediaItems(playlistId)
                    entities.map { it.toMediaItem() }
                }
            }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get playlist contents as flow for real-time updates
     */
    fun getPlaylistContentsFlow(playlistId: String): Flow<List<MediaItem>> {
        return if (isSmartPlaylist(playlistId)) {
            // Smart playlists need special handling for real-time updates
            mediaItemDao.getAllFlow().map { 
                generateSmartPlaylistContents(playlistId)
            }
        } else {
            playlistDao.getPlaylistMediaItemsFlow(playlistId).map { entities ->
                entities.map { it.toMediaItem() }
            }
        }
    }
    
    /**
     * Reorder playlist items
     */
    suspend fun reorderPlaylist(
        playlistId: String,
        fromIndex: Int,
        toIndex: Int
    ): Result<Unit> {
        return try {
            val items = playlistDao.getPlaylistItems(playlistId)
            if (fromIndex in items.indices && toIndex in items.indices) {
                // Remove and reinsert item
                playlistDao.removePlaylistItem(playlistId, items[fromIndex].mediaId)
                
                // Update positions
                val reorderedItems = items.toMutableList()
                val movedItem = reorderedItems.removeAt(fromIndex)
                reorderedItems.add(toIndex, movedItem)
                
                // Clear and re-add all items with new positions
                playlistDao.clearPlaylist(playlistId)
                reorderedItems.forEachIndexed { index, item ->
                    playlistDao.insertPlaylistItem(
                        item.copy(position = index)
                    )
                }
                
                playlistDao.updateItemCount(playlistId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Generate smart playlist contents based on type
     */
    private suspend fun generateSmartPlaylistContents(playlistId: String): List<MediaItem> {
        val type = getSmartPlaylistType(playlistId) ?: return emptyList()
        
        return when (type) {
            SmartPlaylistType.RECENTLY_ADDED -> {
                mediaItemDao.getRecentlyAdded(50).map { it.toMediaItem() }
            }
            
            SmartPlaylistType.RECENTLY_PLAYED -> {
                mediaItemDao.getRecentlyPlayed(50).map { it.toMediaItem() }
            }
            
            SmartPlaylistType.TOP_RATED -> {
                mediaItemDao.getByMinRating(4.0f).map { it.toMediaItem() }
            }
            
            SmartPlaylistType.MOST_PLAYED -> {
                mediaItemDao.getTopPlayed(50).map { it.toMediaItem() }
            }
            
            SmartPlaylistType.FAVORITES -> {
                mediaItemDao.getFavorites().map { it.toMediaItem() }
            }
            
            SmartPlaylistType.RANDOM_MIX -> {
                val allItems = mediaItemDao.getAllFlow().value
                allItems.shuffled().take(100).map { it.toMediaItem() }
            }
            
            SmartPlaylistType.NEVER_PLAYED -> {
                // Items with play count = 0
                val allItems = mediaItemDao.getAllFlow().value
                allItems.filter { it.playCount == 0 }.map { it.toMediaItem() }
            }
            
            SmartPlaylistType.LONG_TRACKS -> {
                // Tracks longer than 10 minutes
                val allItems = mediaItemDao.getAllFlow().value
                allItems.filter { it.duration > 600000 }.map { it.toMediaItem() }
            }
            
            SmartPlaylistType.SHORT_TRACKS -> {
                // Tracks shorter than 3 minutes
                val allItems = mediaItemDao.getAllFlow().value
                allItems.filter { it.duration < 180000 && it.duration > 0 }.map { it.toMediaItem() }
            }
            
            SmartPlaylistType.HD_VIDEOS -> {
                // Videos with height >= 720p
                val allItems = mediaItemDao.getAllFlow().value
                allItems.filter { 
                    it.type == MediaType.VIDEO && (it.height ?: 0) >= 720
                }.map { it.toMediaItem() }
            }
            
            SmartPlaylistType.LOSSLESS_AUDIO -> {
                // FLAC, ALAC, WAV files
                val allItems = mediaItemDao.getAllFlow().value
                allItems.filter { 
                    it.type == MediaType.AUDIO && 
                    (it.container?.lowercase() in listOf("flac", "wav", "alac", "ape"))
                }.map { it.toMediaItem() }
            }
        }
    }
    
    /**
     * Add built-in smart playlists if they don't exist
     */
    private fun addBuiltInSmartPlaylists(playlists: MutableList<Playlist>) {
        val smartPlaylistDefinitions = mapOf(
            SmartPlaylistType.RECENTLY_ADDED to "Recently Added",
            SmartPlaylistType.RECENTLY_PLAYED to "Recently Played", 
            SmartPlaylistType.TOP_RATED to "Top Rated",
            SmartPlaylistType.MOST_PLAYED to "Most Played",
            SmartPlaylistType.FAVORITES to "Favorites",
            SmartPlaylistType.RANDOM_MIX to "Random Mix",
            SmartPlaylistType.NEVER_PLAYED to "Never Played",
            SmartPlaylistType.LONG_TRACKS to "Long Tracks (10+ min)",
            SmartPlaylistType.SHORT_TRACKS to "Short Tracks (<3 min)",
            SmartPlaylistType.HD_VIDEOS to "HD Videos",
            SmartPlaylistType.LOSSLESS_AUDIO to "Lossless Audio"
        )
        
        smartPlaylistDefinitions.forEach { (type, name) ->
            val smartPlaylistId = "smart_${type.name.lowercase()}"
            if (playlists.none { it.id == smartPlaylistId }) {
                playlists.add(
                    Playlist(
                        id = smartPlaylistId,
                        name = name,
                        createdAt = 0L,
                        updatedAt = System.currentTimeMillis(),
                        isSmartPlaylist = true,
                        smartQuery = type.name
                    )
                )
            }
        }
    }
    
    /**
     * Check if playlist is a smart playlist
     */
    private fun isSmartPlaylist(playlistId: String): Boolean {
        return playlistId.startsWith("smart_")
    }
    
    /**
     * Get smart playlist type from ID
     */
    private fun getSmartPlaylistType(playlistId: String): SmartPlaylistType? {
        if (!isSmartPlaylist(playlistId)) return null
        
        val typeName = playlistId.removePrefix("smart_").uppercase()
        return try {
            SmartPlaylistType.valueOf(typeName)
        } catch (e: IllegalArgumentException) {
            null
        }
    }
    
    /**
     * Generate unique playlist ID
     */
    private fun generatePlaylistId(): String {
        return "playlist_${UUID.randomUUID().toString().replace("-", "")}"
    }
    
    /**
     * Export playlist to M3U format
     */
    suspend fun exportPlaylistToM3U(playlistId: String): Result<String> {
        return try {
            val playlist = playlistDao.getPlaylistById(playlistId)
                ?: return Result.failure(Exception("Playlist not found"))
            
            val items = getPlaylistContents(playlistId).getOrThrow()
            
            val m3uContent = buildString {
                appendLine("#EXTM3U")
                appendLine("#PLAYLIST:${playlist.name}")
                
                items.forEach { item ->
                    appendLine("#EXTINF:${item.duration / 1000},${item.artist} - ${item.title}")
                    appendLine(item.uri)
                }
            }
            
            Result.success(m3uContent)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Import playlist from M3U format
     */
    suspend fun importPlaylistFromM3U(
        playlistName: String,
        m3uContent: String
    ): Result<Playlist> {
        return try {
            val playlist = createPlaylist(playlistName).getOrThrow()
            
            val lines = m3uContent.lines()
            var currentTitle: String? = null
            
            for (line in lines) {
                when {
                    line.startsWith("#EXTINF:") -> {
                        // Extract title from EXTINF line
                        val titlePart = line.substringAfter(",")
                        currentTitle = titlePart
                    }
                    line.isNotBlank() && !line.startsWith("#") -> {
                        // This is a file path/URI
                        val mediaItem = mediaItemDao.getByUri(line)
                        if (mediaItem != null) {
                            addToPlaylist(playlist.id, mediaItem.id)
                        }
                    }
                }
            }
            
            Result.success(playlist)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Extension function to convert MediaItemEntity to MediaItem
 */
private fun com.vibestream.player.data.database.entity.MediaItemEntity.toMediaItem(): MediaItem {
    return MediaItem(
        id = id,
        uri = uri,
        type = type,
        title = title,
        album = album,
        artist = artist,
        albumArtist = albumArtist,
        genre = genre,
        trackNumber = trackNumber,
        discNumber = discNumber,
        year = year,
        duration = duration,
        bitrate = bitrate,
        sampleRate = sampleRate,
        channels = channels,
        codecAudio = codecAudio,
        codecVideo = codecVideo,
        container = container,
        width = width,
        height = height,
        isHdr = isHdr,
        rotation = rotation,
        fileSize = fileSize,
        dateAdded = dateAdded,
        dateModified = dateModified,
        hash = hash,
        folderId = folderId,
        isFavorite = isFavorite,
        rating = rating,
        replayGainTrack = replayGainTrack,
        replayGainAlbum = replayGainAlbum,
        lyrics = lyrics,
        thumbnailPath = thumbnailPath,
        waveformPath = waveformPath,
        lastPlayPosition = lastPlayPosition,
        playCount = playCount,
        lastPlayedAt = lastPlayedAt
    )
}