package com.vibestream.player.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for app settings storage
 */
@Entity(
    tableName = "settings",
    indices = [
        Index(value = ["key"], unique = true)
    ]
)
data class SettingsEntity(
    @PrimaryKey
    val key: String,
    
    @ColumnInfo(name = "value_json")
    val valueJson: String,
    
    @ColumnInfo(name = "category")
    val category: String? = null,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)