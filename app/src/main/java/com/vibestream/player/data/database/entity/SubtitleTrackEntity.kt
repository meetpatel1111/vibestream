package com.vibestream.player.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for subtitle tracks
 */
@Entity(
    tableName = "subtitle_track",
    foreignKeys = [
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["media_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["media_id"]),
        Index(value = ["lang"])
    ]
)
data class SubtitleTrackEntity(
    @PrimaryKey
    val id: String,
    
    @ColumnInfo(name = "media_id")
    val mediaId: String,
    
    @ColumnInfo(name = "uri")
    val uri: String,
    
    @ColumnInfo(name = "lang")
    val lang: String? = null,
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "default_flag")
    val defaultFlag: Boolean = false,
    
    @ColumnInfo(name = "external")
    val external: Boolean = false,
    
    @ColumnInfo(name = "mime_type")
    val mimeType: String? = null,
    
    @ColumnInfo(name = "encoding")
    val encoding: String? = null,
    
    @ColumnInfo(name = "sync_offset_ms")
    val syncOffsetMs: Long = 0L
)