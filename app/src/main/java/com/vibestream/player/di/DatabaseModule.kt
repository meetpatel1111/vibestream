package com.vibestream.player.di

import android.content.Context
import androidx.room.Room
import com.vibestream.player.data.database.VibeStreamDatabase
import com.vibestream.player.data.database.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for database dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VibeStreamDatabase {
        return VibeStreamDatabase.getDatabase(context)
    }
    
    @Provides
    fun provideMediaItemDao(database: VibeStreamDatabase): MediaItemDao {
        return database.mediaItemDao()
    }
    
    @Provides
    fun provideFolderDao(database: VibeStreamDatabase): FolderDao {
        return database.folderDao()
    }
    
    @Provides
    fun providePlaylistDao(database: VibeStreamDatabase): PlaylistDao {
        return database.playlistDao()
    }
    
    @Provides
    fun providePlayHistoryDao(database: VibeStreamDatabase): PlayHistoryDao {
        return database.playHistoryDao()
    }
    
    @Provides
    fun provideSubtitleTrackDao(database: VibeStreamDatabase): SubtitleTrackDao {
        return database.subtitleTrackDao()
    }
    
    @Provides
    fun provideAudioDeviceProfileDao(database: VibeStreamDatabase): AudioDeviceProfileDao {
        return database.audioDeviceProfileDao()
    }
    
    @Provides
    fun provideSettingsDao(database: VibeStreamDatabase): SettingsDao {
        return database.settingsDao()
    }
}