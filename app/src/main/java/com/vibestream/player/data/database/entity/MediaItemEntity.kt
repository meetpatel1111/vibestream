package com.vibestream.player.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vibestream.player.data.model.MediaType

/**
 * Room entity for media items (videos and audio files)
 */
@Entity(
    tableName = "media_item",
    indices = [
        Index(value = ["uri"], unique = true),
        Index(value = ["title"]),
        Index(value = ["artist"]),
        Index(value = ["album"]),
        Index(value = ["date_added"]),
        Index(value = ["folder_id"]),
        Index(value = ["type"])
    ]
)
data class MediaItemEntity(
    @PrimaryKey
    val id: String,
    
    @ColumnInfo(name = "uri")
    val uri: String,
    
    @ColumnInfo(name = "type")
    val type: MediaType,
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "album")
    val album: String? = null,
    
    @ColumnInfo(name = "artist")
    val artist: String? = null,
    
    @ColumnInfo(name = "album_artist")
    val albumArtist: String? = null,
    
    @ColumnInfo(name = "genre")
    val genre: String? = null,
    
    @ColumnInfo(name = "track_no")
    val trackNumber: Int? = null,
    
    @ColumnInfo(name = "disc_no")
    val discNumber: Int? = null,
    
    @ColumnInfo(name = "year")
    val year: Int? = null,
    
    @ColumnInfo(name = "duration_ms")
    val duration: Long = 0L,
    
    @ColumnInfo(name = "bitrate")
    val bitrate: Int? = null,
    
    @ColumnInfo(name = "samplerate")
    val sampleRate: Int? = null,
    
    @ColumnInfo(name = "channels")
    val channels: Int? = null,
    
    @ColumnInfo(name = "codec_audio")
    val codecAudio: String? = null,
    
    @ColumnInfo(name = "codec_video")
    val codecVideo: String? = null,
    
    @ColumnInfo(name = "container")
    val container: String? = null,
    
    @ColumnInfo(name = "width")
    val width: Int? = null,
    
    @ColumnInfo(name = "height")
    val height: Int? = null,
    
    @ColumnInfo(name = "hdr")
    val isHdr: Boolean = false,
    
    @ColumnInfo(name = "rotation")
    val rotation: Int = 0,
    
    @ColumnInfo(name = "size_bytes")
    val fileSize: Long = 0L,
    
    @ColumnInfo(name = "date_added")
    val dateAdded: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "date_modified")
    val dateModified: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "hash")
    val hash: String? = null,
    
    @ColumnInfo(name = "folder_id")
    val folderId: String? = null,
    
    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,
    
    @ColumnInfo(name = "rating")
    val rating: Float = 0f,
    
    @ColumnInfo(name = "replay_gain_track")
    val replayGainTrack: Float? = null,
    
    @ColumnInfo(name = "replay_gain_album")
    val replayGainAlbum: Float? = null,
    
    @ColumnInfo(name = "lyrics")
    val lyrics: String? = null,
    
    @ColumnInfo(name = "chapters_json")
    val chaptersJson: String? = null,
    
    @ColumnInfo(name = "thumbnail_path")
    val thumbnailPath: String? = null,
    
    @ColumnInfo(name = "waveform_path")
    val waveformPath: String? = null,
    
    @ColumnInfo(name = "last_play_position")
    val lastPlayPosition: Long = 0L,
    
    @ColumnInfo(name = "play_count")
    val playCount: Int = 0,
    
    @ColumnInfo(name = "last_played_at")
    val lastPlayedAt: Long? = null
)