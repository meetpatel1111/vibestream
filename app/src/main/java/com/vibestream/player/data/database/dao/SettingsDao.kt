package com.vibestream.player.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vibestream.player.data.database.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for settings operations
 */
@Dao
interface SettingsDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(settings: SettingsEntity)
    
    @Query("SELECT * FROM settings WHERE key = :key")
    suspend fun getByKey(key: String): SettingsEntity?
    
    @Query("SELECT * FROM settings WHERE key = :key")
    fun getByKeyFlow(key: String): Flow<SettingsEntity?>
    
    @Query("SELECT * FROM settings WHERE category = :category")
    suspend fun getByCategory(category: String): List<SettingsEntity>
    
    @Query("SELECT * FROM settings")
    suspend fun getAll(): List<SettingsEntity>
    
    @Query("DELETE FROM settings WHERE key = :key")
    suspend fun deleteByKey(key: String)
    
    @Query("DELETE FROM settings WHERE category = :category")
    suspend fun deleteByCategory(category: String)
    
    @Query("DELETE FROM settings")
    suspend fun deleteAll()
}