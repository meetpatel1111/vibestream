package com.vibestream.player.data.database

import androidx.room.TypeConverter
import com.vibestream.player.data.model.MediaType

/**
 * Room type converters for complex data types
 */
class DatabaseConverters {
    
    @TypeConverter
    fun fromMediaType(mediaType: MediaType): String {
        return mediaType.name
    }
    
    @TypeConverter
    fun toMediaType(mediaType: String): MediaType {
        return MediaType.valueOf(mediaType)
    }
}