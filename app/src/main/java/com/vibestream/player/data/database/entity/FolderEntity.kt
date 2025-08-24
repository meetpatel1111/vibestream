package com.vibestream.player.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for folder configuration
 */
@Entity(
    tableName = "folder",
    indices = [
        Index(value = ["path"], unique = true)
    ]
)
data class FolderEntity(
    @PrimaryKey
    val id: String,
    
    @ColumnInfo(name = "path")
    val path: String,
    
    @ColumnInfo(name = "display_name")
    val displayName: String,
    
    @ColumnInfo(name = "included")
    val included: Boolean = true,
    
    @ColumnInfo(name = "excluded")
    val excluded: Boolean = false,
    
    @ColumnInfo(name = "recursive")
    val recursive: Boolean = true,
    
    @ColumnInfo(name = "last_scanned")
    val lastScanned: Long? = null,
    
    @ColumnInfo(name = "item_count")
    val itemCount: Int = 0
)