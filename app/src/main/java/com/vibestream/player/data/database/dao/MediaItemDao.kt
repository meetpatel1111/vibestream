package com.vibestream.player.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vibestream.player.data.database.entity.MediaItemEntity
import com.vibestream.player.data.model.MediaType
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for media item operations
 */
@Dao
interface MediaItemDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mediaItem: MediaItemEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(mediaItems: List<MediaItemEntity>): List<Long>
    
    @Update
    suspend fun update(mediaItem: MediaItemEntity)
    
    @Delete
    suspend fun delete(mediaItem: MediaItemEntity)
    
    @Query("DELETE FROM media_item WHERE id = :id")
    suspend fun deleteById(id: String)
    
    @Query("SELECT * FROM media_item WHERE id = :id")
    suspend fun getById(id: String): MediaItemEntity?
    
    @Query("SELECT * FROM media_item WHERE uri = :uri")
    suspend fun getByUri(uri: String): MediaItemEntity?
    
    @Query("SELECT * FROM media_item ORDER BY title ASC")
    fun getAllFlow(): Flow<List<MediaItemEntity>>
    
    @Query("SELECT * FROM media_item WHERE type = :type ORDER BY title ASC")
    fun getByTypeFlow(type: MediaType): Flow<List<MediaItemEntity>>
    
    @Query("SELECT * FROM media_item WHERE folder_id = :folderId ORDER BY title ASC")
    suspend fun getByFolderId(folderId: String): List<MediaItemEntity>
    
    @Query("SELECT * FROM media_item WHERE artist = :artist ORDER BY album ASC, track_no ASC")
    suspend fun getByArtist(artist: String): List<MediaItemEntity>
    
    @Query("SELECT * FROM media_item WHERE album = :album ORDER BY track_no ASC")
    suspend fun getByAlbum(album: String): List<MediaItemEntity>
    
    @Query("SELECT * FROM media_item WHERE genre = :genre ORDER BY artist ASC, album ASC")
    suspend fun getByGenre(genre: String): List<MediaItemEntity>
    
    @Query("SELECT * FROM media_item WHERE is_favorite = 1 ORDER BY date_added DESC")
    suspend fun getFavorites(): List<MediaItemEntity>
    
    @Query("SELECT * FROM media_item WHERE rating >= :minRating ORDER BY rating DESC")
    suspend fun getByMinRating(minRating: Float): List<MediaItemEntity>
    
    @Query("SELECT * FROM media_item ORDER BY date_added DESC LIMIT :limit")
    suspend fun getRecentlyAdded(limit: Int): List<MediaItemEntity>
    
    @Query("SELECT * FROM media_item ORDER BY last_played_at DESC LIMIT :limit")
    suspend fun getRecentlyPlayed(limit: Int): List<MediaItemEntity>
    
    @Query("SELECT * FROM media_item ORDER BY play_count DESC LIMIT :limit")
    suspend fun getTopPlayed(limit: Int): List<MediaItemEntity>
    
    @Query("""
        SELECT * FROM media_item 
        WHERE title LIKE '%' || :query || '%' 
        OR artist LIKE '%' || :query || '%' 
        OR album LIKE '%' || :query || '%'
        ORDER BY title ASC
    """)
    suspend fun search(query: String): List<MediaItemEntity>
    
    @Query("SELECT DISTINCT artist FROM media_item WHERE artist IS NOT NULL ORDER BY artist ASC")
    suspend fun getAllArtists(): List<String>
    
    @Query("SELECT DISTINCT album FROM media_item WHERE album IS NOT NULL ORDER BY album ASC")
    suspend fun getAllAlbums(): List<String>
    
    @Query("SELECT DISTINCT genre FROM media_item WHERE genre IS NOT NULL ORDER BY genre ASC")
    suspend fun getAllGenres(): List<String>
    
    @Query("UPDATE media_item SET is_favorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean)
    
    @Query("UPDATE media_item SET rating = :rating WHERE id = :id")
    suspend fun setRating(id: String, rating: Float)
    
    @Query("UPDATE media_item SET last_play_position = :position, last_played_at = :timestamp, play_count = play_count + 1 WHERE id = :id")
    suspend fun updatePlaybackInfo(id: String, position: Long, timestamp: Long)
    
    @Query("SELECT COUNT(*) FROM media_item")
    suspend fun getItemCount(): Int
    
    @Query("SELECT COUNT(*) FROM media_item WHERE type = :type")
    suspend fun getItemCountByType(type: MediaType): Int
    
    @Query("SELECT SUM(size_bytes) FROM media_item")
    suspend fun getTotalSize(): Long?
    
    @Query("SELECT SUM(duration_ms) FROM media_item")
    suspend fun getTotalDuration(): Long?
    
    @Query("DELETE FROM media_item")
    suspend fun deleteAll()
}