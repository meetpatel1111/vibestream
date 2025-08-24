package com.vibestream.player.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.vibestream.player.data.database.entity.PlaylistEntity
import com.vibestream.player.data.database.entity.PlaylistItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for playlist operations
 */
@Dao
interface PlaylistDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long
    
    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)
    
    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)
    
    @Query("DELETE FROM playlist WHERE id = :id")
    suspend fun deletePlaylistById(id: String)
    
    @Query("SELECT * FROM playlist WHERE id = :id")
    suspend fun getPlaylistById(id: String): PlaylistEntity?
    
    @Query("SELECT * FROM playlist ORDER BY name ASC")
    fun getAllPlaylistsFlow(): Flow<List<PlaylistEntity>>
    
    @Query("SELECT * FROM playlist ORDER BY name ASC")
    suspend fun getAllPlaylists(): List<PlaylistEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItem(item: PlaylistItemEntity)
    
    @Query("DELETE FROM playlist_item WHERE playlist_id = :playlistId AND media_id = :mediaId")
    suspend fun removePlaylistItem(playlistId: String, mediaId: String)
    
    @Query("SELECT * FROM playlist_item WHERE playlist_id = :playlistId ORDER BY position ASC")
    suspend fun getPlaylistItems(playlistId: String): List<PlaylistItemEntity>
    
    @Query("""
        SELECT mi.* FROM media_item mi
        INNER JOIN playlist_item pi ON mi.id = pi.media_id
        WHERE pi.playlist_id = :playlistId
        ORDER BY pi.position ASC
    """)
    suspend fun getPlaylistMediaItems(playlistId: String): List<com.vibestream.player.data.database.entity.MediaItemEntity>
    
    @Query("""
        SELECT mi.* FROM media_item mi
        INNER JOIN playlist_item pi ON mi.id = pi.media_id
        WHERE pi.playlist_id = :playlistId
        ORDER BY pi.position ASC
    """)
    fun getPlaylistMediaItemsFlow(playlistId: String): Flow<List<com.vibestream.player.data.database.entity.MediaItemEntity>>
    
    @Query("SELECT MAX(position) FROM playlist_item WHERE playlist_id = :playlistId")
    suspend fun getMaxPosition(playlistId: String): Int?
    
    @Query("UPDATE playlist SET item_count = (SELECT COUNT(*) FROM playlist_item WHERE playlist_id = :playlistId) WHERE id = :playlistId")
    suspend fun updateItemCount(playlistId: String)
    
    @Transaction
    suspend fun addToPlaylist(playlistId: String, mediaId: String) {
        val maxPosition = getMaxPosition(playlistId) ?: -1
        val newPosition = maxPosition + 1
        insertPlaylistItem(
            PlaylistItemEntity(
                playlistId = playlistId,
                mediaId = mediaId,
                position = newPosition
            )
        )
        updateItemCount(playlistId)
    }
    
    @Transaction
    suspend fun removeFromPlaylist(playlistId: String, mediaId: String) {
        removePlaylistItem(playlistId, mediaId)
        
        // Reorder remaining items
        val items = getPlaylistItems(playlistId)
        items.forEachIndexed { index, item ->
            if (item.position != index) {
                insertPlaylistItem(item.copy(position = index))
            }
        }
        updateItemCount(playlistId)
    }
    
    @Query("DELETE FROM playlist_item WHERE playlist_id = :playlistId")
    suspend fun clearPlaylist(playlistId: String)
    
    @Query("SELECT COUNT(*) FROM playlist")
    suspend fun getPlaylistCount(): Int
}