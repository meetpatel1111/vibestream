package com.vibestream.player.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.vibestream.player.data.database.dao.*
import com.vibestream.player.data.database.entity.*

/**
 * Main Room database for VibeStream
 */
@Database(
    entities = [
        MediaItemEntity::class,
        FolderEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class,
        PlayHistoryEntity::class,
        SubtitleTrackEntity::class,
        AudioDeviceProfileEntity::class,
        SettingsEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(DatabaseConverters::class)
abstract class VibeStreamDatabase : RoomDatabase() {
    
    abstract fun mediaItemDao(): MediaItemDao
    abstract fun folderDao(): FolderDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playHistoryDao(): PlayHistoryDao
    abstract fun subtitleTrackDao(): SubtitleTrackDao
    abstract fun audioDeviceProfileDao(): AudioDeviceProfileDao
    abstract fun settingsDao(): SettingsDao
    
    companion object {
        @Volatile
        private var INSTANCE: VibeStreamDatabase? = null
        
        private const val DATABASE_NAME = "vibestream_database"
        
        fun getDatabase(context: Context): VibeStreamDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VibeStreamDatabase::class.java,
                    DATABASE_NAME
                )
                .addCallback(DatabaseCallback())
                .fallbackToDestructiveMigration() // For development only
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

/**
 * Database callback for initialization
 */
private class DatabaseCallback : RoomDatabase.Callback() {
    // TODO: Add any database initialization logic here
}