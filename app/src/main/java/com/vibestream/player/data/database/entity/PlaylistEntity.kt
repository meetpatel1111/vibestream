package com.vibestream.player.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for playlists
 */
@Entity(
    tableName = "playlist",
    indices = [
        Index(value = ["name"])
    ]
)
data class PlaylistEntity(
    @PrimaryKey
    val id: String,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "thumbnail_path")
    val thumbnailPath: String? = null,
    
    @ColumnInfo(name = "is_smart_playlist")
    val isSmartPlaylist: Boolean = false,
    
    @ColumnInfo(name = "smart_query")
    val smartQuery: String? = null,
    
    @ColumnInfo(name = "item_count")
    val itemCount: Int = 0
)

/**
 * Room entity for playlist items (many-to-many relationship)
 */
@Entity(
    tableName = "playlist_item",
    primaryKeys = ["playlist_id", "media_id"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlist_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["media_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["playlist_id"]),
        Index(value = ["media_id"]),
        Index(value = ["playlist_id", "position"])
    ]
)
data class PlaylistItemEntity(
    @ColumnInfo(name = "playlist_id")
    val playlistId: String,
    
    @ColumnInfo(name = "media_id")
    val mediaId: String,
    
    @ColumnInfo(name = "position")
    val position: Int,
    
    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis()
)