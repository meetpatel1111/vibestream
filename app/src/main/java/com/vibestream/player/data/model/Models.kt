package com.vibestream.player.data.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Core media item representing a video or audio file
 */
@Parcelize
@Serializable
data class MediaItem(
    val id: String,
    val uri: String,
    val type: MediaType,
    val title: String,
    val album: String? = null,
    val artist: String? = null,
    val albumArtist: String? = null,
    val genre: String? = null,
    val trackNumber: Int? = null,
    val discNumber: Int? = null,
    val year: Int? = null,
    val duration: Long = 0L, // in milliseconds
    val bitrate: Int? = null,
    val sampleRate: Int? = null,
    val channels: Int? = null,
    val codecAudio: String? = null,
    val codecVideo: String? = null,
    val container: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val isHdr: Boolean = false,
    val rotation: Int = 0,
    val fileSize: Long = 0L,
    val dateAdded: Long = System.currentTimeMillis(),
    val dateModified: Long = System.currentTimeMillis(),
    val hash: String? = null,
    val folderId: String? = null,
    val isFavorite: Boolean = false,
    val rating: Float = 0f, // 0.0 to 5.0
    val replayGainTrack: Float? = null,
    val replayGainAlbum: Float? = null,
    val lyrics: String? = null,
    val chapters: List<Chapter> = emptyList(),
    val thumbnailPath: String? = null,
    val waveformPath: String? = null,
    val lastPlayPosition: Long = 0L,
    val playCount: Int = 0,
    val lastPlayedAt: Long? = null
) : Parcelable

enum class MediaType {
    VIDEO, AUDIO
}

/**
 * Chapter information for media items
 */
@Parcelize
@Serializable
data class Chapter(
    val title: String,
    val startTime: Long, // in milliseconds
    val endTime: Long // in milliseconds
) : Parcelable

/**
 * Playlist data model
 */
@Parcelize
@Serializable
data class Playlist(
    val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val items: List<String> = emptyList(), // Media item IDs
    val thumbnailPath: String? = null,
    val isSmartPlaylist: Boolean = false,
    val smartQuery: String? = null
) : Parcelable

/**
 * Playback state information
 */
@Serializable
data class PlaybackState(
    val isPlaying: Boolean = false,
    val isPreparing: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val currentMediaItem: MediaItem? = null,
    val queue: List<MediaItem> = emptyList(),
    val currentIndex: Int = 0,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffleMode: Boolean = false,
    val error: PlaybackError? = null
)

/**
 * Player events
 */
sealed class PlayerEvent {
    object PlaybackStarted : PlayerEvent()
    object PlaybackPaused : PlayerEvent()
    object PlaybackStopped : PlayerEvent()
    data class MediaItemChanged(val mediaItem: MediaItem) : PlayerEvent()
    data class PositionChanged(val position: Long) : PlayerEvent()
    data class BufferingStarted(val position: Long) : PlayerEvent()
    data class BufferingEnded(val position: Long) : PlayerEvent()
    data class PlaybackError(val error: String, val code: Int? = null) : PlayerEvent()
    data class TrackChanged(val trackType: TrackType, val trackId: String?) : PlayerEvent()
    object PlaybackCompleted : PlayerEvent()
}

enum class TrackType {
    AUDIO, SUBTITLE
}

/**
 * Playback error information
 */
@Serializable
data class PlaybackError(
    val message: String,
    val code: Int? = null,
    val recoverable: Boolean = false
)

/**
 * Library scan events
 */
sealed class ScanEvent {
    object Started : ScanEvent()
    data class Progress(val processed: Int, val total: Int, val currentFile: String) : ScanEvent()
    data class ItemFound(val mediaItem: MediaItem) : ScanEvent()
    data class ItemUpdated(val mediaItem: MediaItem) : ScanEvent()
    data class Error(val message: String, val file: String? = null) : ScanEvent()
    data class Completed(val itemsProcessed: Int, val itemsAdded: Int, val duration: Long) : ScanEvent()
}

/**
 * Library query for filtering and sorting
 */
data class LibraryQuery(
    val mediaType: MediaType? = null,
    val artist: String? = null,
    val album: String? = null,
    val genre: String? = null,
    val folderId: String? = null,
    val searchText: String? = null,
    val sortBy: SortBy = SortBy.TITLE,
    val sortOrder: SortOrder = SortOrder.ASC,
    val limit: Int? = null,
    val offset: Int = 0,
    val includeWithoutMetadata: Boolean = true,
    val favoritesOnly: Boolean = false,
    val minRating: Float? = null
)

enum class SortBy {
    TITLE, ARTIST, ALBUM, DURATION, DATE_ADDED, DATE_MODIFIED, PLAY_COUNT, RATING
}

enum class SortOrder {
    ASC, DESC
}

/**
 * Subtitle track information
 */
@Parcelize
@Serializable
data class SubtitleTrack(
    val id: String,
    val title: String,
    val language: String? = null,
    val isDefault: Boolean = false,
    val isExternal: Boolean = false,
    val uri: String? = null,
    val mimeType: String? = null
) : Parcelable

/**
 * Audio track information
 */
@Parcelize
@Serializable
data class AudioTrack(
    val id: String,
    val title: String,
    val language: String? = null,
    val isDefault: Boolean = false,
    val channels: Int? = null,
    val sampleRate: Int? = null,
    val bitrate: Int? = null,
    val codec: String? = null
) : Parcelable

/**
 * Network source for streaming
 */
@Parcelize
@Serializable
data class NetworkSource(
    val id: String,
    val name: String,
    val type: NetworkType,
    val uri: String,
    val username: String? = null,
    val isBookmarked: Boolean = false,
    val lastAccessed: Long? = null
) : Parcelable

enum class NetworkType {
    SMB, SFTP, FTP, WEBDAV, UPNP, DLNA, HTTP
}