package com.vibestream.player.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for play history tracking
 */
@Entity(
    tableName = "play_history",
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
        Index(value = ["last_played_at"]),
        Index(value = ["device_id"])
    ]
)
data class PlayHistoryEntity(
    @PrimaryKey
    val id: String,
    
    @ColumnInfo(name = "media_id")
    val mediaId: String,
    
    @ColumnInfo(name = "last_position_ms")
    val lastPositionMs: Long,
    
    @ColumnInfo(name = "completed_pct")
    val completedPct: Float,
    
    @ColumnInfo(name = "last_played_at")
    val lastPlayedAt: Long,
    
    @ColumnInfo(name = "device_id")
    val deviceId: String,
    
    @ColumnInfo(name = "session_duration_ms")
    val sessionDurationMs: Long? = null,
    
    @ColumnInfo(name = "play_count")
    val playCount: Int = 1
)