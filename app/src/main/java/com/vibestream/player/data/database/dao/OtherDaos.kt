package com.vibestream.player.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vibestream.player.data.database.entity.FolderEntity
import com.vibestream.player.data.database.entity.AudioDeviceProfileEntity
import com.vibestream.player.data.database.entity.SubtitleTrackEntity
import com.vibestream.player.data.database.entity.PlayHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for folder operations
 */
@Dao
interface FolderDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: FolderEntity)
    
    @Update
    suspend fun update(folder: FolderEntity)
    
    @Delete
    suspend fun delete(folder: FolderEntity)
    
    @Query("SELECT * FROM folder WHERE id = :id")
    suspend fun getById(id: String): FolderEntity?
    
    @Query("SELECT * FROM folder WHERE included = 1")
    suspend fun getIncludedFolders(): List<FolderEntity>
    
    @Query("SELECT * FROM folder")
    fun getAllFlow(): Flow<List<FolderEntity>>
    
    @Query("DELETE FROM folder")
    suspend fun deleteAll()
}

/**
 * Room DAO for audio device profile operations
 */
@Dao
interface AudioDeviceProfileDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: AudioDeviceProfileEntity)
    
    @Update
    suspend fun update(profile: AudioDeviceProfileEntity)
    
    @Delete
    suspend fun delete(profile: AudioDeviceProfileEntity)
    
    @Query("SELECT * FROM audio_device_profile WHERE id = :id")
    suspend fun getById(id: String): AudioDeviceProfileEntity?
    
    @Query("SELECT * FROM audio_device_profile WHERE is_default = 1")
    suspend fun getDefaultProfile(): AudioDeviceProfileEntity?
    
    @Query("SELECT * FROM audio_device_profile ORDER BY name ASC")
    suspend fun getAll(): List<AudioDeviceProfileEntity>
    
    @Query("UPDATE audio_device_profile SET is_default = 0")
    suspend fun clearDefaultFlags()
    
    @Query("UPDATE audio_device_profile SET is_default = 1 WHERE id = :id")
    suspend fun setAsDefault(id: String)
}

/**
 * Room DAO for subtitle track operations
 */
@Dao
interface SubtitleTrackDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subtitleTrack: SubtitleTrackEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(subtitleTracks: List<SubtitleTrackEntity>)
    
    @Update
    suspend fun update(subtitleTrack: SubtitleTrackEntity)
    
    @Delete
    suspend fun delete(subtitleTrack: SubtitleTrackEntity)
    
    @Query("SELECT * FROM subtitle_track WHERE media_id = :mediaId ORDER BY lang ASC")
    suspend fun getByMediaId(mediaId: String): List<SubtitleTrackEntity>
    
    @Query("DELETE FROM subtitle_track WHERE media_id = :mediaId")
    suspend fun deleteByMediaId(mediaId: String)
}

/**
 * Room DAO for play history operations
 */
@Dao
interface PlayHistoryDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playHistory: PlayHistoryEntity)
    
    @Update
    suspend fun update(playHistory: PlayHistoryEntity)
    
    @Delete
    suspend fun delete(playHistory: PlayHistoryEntity)
    
    @Query("SELECT * FROM play_history WHERE media_id = :mediaId AND device_id = :deviceId")
    suspend fun getByMediaAndDevice(mediaId: String, deviceId: String): PlayHistoryEntity?
    
    @Query("SELECT * FROM play_history ORDER BY last_played_at DESC LIMIT :limit")
    suspend fun getRecentHistory(limit: Int): List<PlayHistoryEntity>
    
    @Query("DELETE FROM play_history WHERE last_played_at < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
    
    @Query("DELETE FROM play_history")
    suspend fun deleteAll()
}