package com.vibestream.player.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for audio device profiles with EQ settings
 */
@Entity(
    tableName = "audio_device_profile",
    indices = [
        Index(value = ["name"])
    ]
)
data class AudioDeviceProfileEntity(
    @PrimaryKey
    val id: String,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "eq_json")
    val eqJson: String, // JSON string for equalizer settings
    
    @ColumnInfo(name = "preamp_db")
    val preampDb: Float = 0f,
    
    @ColumnInfo(name = "limiter_enabled")
    val limiterEnabled: Boolean = false,
    
    @ColumnInfo(name = "bass_boost")
    val bassBoost: Float = 0f,
    
    @ColumnInfo(name = "virtualizer")
    val virtualizer: Float = 0f,
    
    @ColumnInfo(name = "replay_gain_mode")
    val replayGainMode: String = "OFF", // OFF, TRACK, ALBUM
    
    @ColumnInfo(name = "replay_gain_preamp")
    val replayGainPreamp: Float = 0f,
    
    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = false,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)