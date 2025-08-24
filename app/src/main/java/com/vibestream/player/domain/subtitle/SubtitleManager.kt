package com.vibestream.player.domain.subtitle

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.vibestream.player.data.database.dao.SubtitleTrackDao
import com.vibestream.player.data.database.entity.SubtitleTrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.mozilla.universalchardet.UniversalDetector
import java.io.File
import java.io.InputStream
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Subtitle configuration and preferences
 */
data class SubtitleConfig(
    val fontSize: Float = 16f,
    val fontColor: Int = 0xFFFFFFFF.toInt(),
    val backgroundColor: Int = 0x80000000.toInt(),
    val strokeColor: Int = 0xFF000000.toInt(),
    val strokeWidth: Float = 2f,
    val verticalMargin: Float = 0.1f, // 10% from bottom
    val horizontalMargin: Float = 0.05f, // 5% from sides
    val shadowEnabled: Boolean = true,
    val shadowColor: Int = 0xFF000000.toInt(),
    val shadowOffset: Float = 2f,
    val alignment: SubtitleAlignment = SubtitleAlignment.CENTER,
    val autoLoadExternal: Boolean = true,
    val preferredLanguages: List<String> = listOf("en", "zh", "es", "fr"),
    val syncOffsetMs: Long = 0L
)

/**
 * Current subtitle state
 */
data class SubtitleState(
    val isEnabled: Boolean = true,
    val currentTrack: SubtitleTrackInfo? = null,
    val availableTracks: List<SubtitleTrackInfo> = emptyList(),
    val currentEntry: SubtitleEntry? = null,
    val syncOffset: Long = 0L,
    val config: SubtitleConfig = SubtitleConfig()
)

/**
 * Subtitle track information
 */
data class SubtitleTrackInfo(
    val id: String,
    val title: String,
    val language: String? = null,
    val isExternal: Boolean = false,
    val uri: String? = null,
    val format: SubtitleFormat = SubtitleFormat.UNKNOWN,
    val isDefault: Boolean = false
)

/**
 * Subtitle manager for handling subtitle loading, parsing, and display
 */
@Singleton
class SubtitleManager @Inject constructor(
    private val context: Context,
    private val subtitleTrackDao: SubtitleTrackDao
) {
    
    private val parsers = listOf(
        SrtSubtitleParser(),
        VttSubtitleParser(),
        AssSubtitleParser()
    )
    
    private val _subtitleState = MutableStateFlow(SubtitleState())
    val subtitleState: StateFlow<SubtitleState> = _subtitleState.asStateFlow()
    
    private var currentSubtitles: List<SubtitleEntry> = emptyList()
    private var lastSearchIndex = 0
    
    /**
     * Load subtitles for a media item
     */
    suspend fun loadSubtitlesForMedia(mediaId: String, mediaUri: String) {
        withContext(Dispatchers.IO) {
            try {
                val tracks = mutableListOf<SubtitleTrackInfo>()
                
                // Load embedded subtitles from database
                val embeddedTracks = subtitleTrackDao.getByMediaId(mediaId)
                tracks.addAll(embeddedTracks.map { entity ->
                    SubtitleTrackInfo(
                        id = entity.id,
                        title = entity.title,
                        language = entity.lang,
                        isExternal = entity.external,
                        uri = entity.uri,
                        format = detectFormatFromMimeType(entity.mimeType),
                        isDefault = entity.defaultFlag
                    )
                })
                
                // Auto-discover external subtitles if enabled
                if (_subtitleState.value.config.autoLoadExternal) {
                    val externalTracks = discoverExternalSubtitles(mediaUri)
                    tracks.addAll(externalTracks)
                }
                
                _subtitleState.value = _subtitleState.value.copy(
                    availableTracks = tracks
                )
                
                // Auto-select best subtitle track
                autoSelectSubtitleTrack(tracks)
                
            } catch (e: Exception) {
                // Handle error silently or emit error state
            }
        }
    }
    
    /**
     * Select a subtitle track
     */
    suspend fun selectSubtitleTrack(trackId: String?) {
        if (trackId == null) {
            // Disable subtitles
            _subtitleState.value = _subtitleState.value.copy(
                isEnabled = false,
                currentTrack = null,
                currentEntry = null
            )
            currentSubtitles = emptyList()
            return
        }
        
        val track = _subtitleState.value.availableTracks.find { it.id == trackId }
        if (track != null) {
            loadSubtitleTrack(track)
        }
    }
    
    /**
     * Update subtitle sync offset
     */
    fun setSyncOffset(offsetMs: Long) {
        _subtitleState.value = _subtitleState.value.copy(
            syncOffset = offsetMs
        )
    }
    
    /**
     * Update subtitle configuration
     */
    fun updateConfig(config: SubtitleConfig) {
        _subtitleState.value = _subtitleState.value.copy(
            config = config
        )
    }
    
    /**
     * Get current subtitle for playback position
     */
    fun getCurrentSubtitle(positionMs: Long): SubtitleEntry? {
        if (!_subtitleState.value.isEnabled || currentSubtitles.isEmpty()) {
            return null
        }
        
        val adjustedPosition = positionMs + _subtitleState.value.syncOffset
        
        // Optimize search using last known index
        val startIndex = (lastSearchIndex - 1).coerceAtLeast(0)
        
        for (i in startIndex until currentSubtitles.size) {
            val entry = currentSubtitles[i]
            
            when {
                adjustedPosition < entry.startTime -> {
                    // Position is before this subtitle
                    break
                }
                adjustedPosition in entry.startTime..entry.endTime -> {
                    // Found matching subtitle
                    lastSearchIndex = i
                    val currentEntry = if (entry != _subtitleState.value.currentEntry) {
                        _subtitleState.value = _subtitleState.value.copy(currentEntry = entry)
                        entry
                    } else {
                        entry
                    }
                    return currentEntry
                }
                // Continue searching
            }
        }
        
        // No subtitle found for current position
        if (_subtitleState.value.currentEntry != null) {
            _subtitleState.value = _subtitleState.value.copy(currentEntry = null)
        }
        return null
    }
    
    /**
     * Add external subtitle file
     */
    suspend fun addExternalSubtitle(uri: Uri, mediaId: String): Result<SubtitleTrackInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: return@withContext Result.failure(Exception("Cannot open subtitle file"))
                
                val encoding = detectEncoding(inputStream)
                inputStream.close()
                
                val format = detectFormatFromUri(uri)
                val fileName = getFileNameFromUri(uri)
                
                val trackInfo = SubtitleTrackInfo(
                    id = UUID.randomUUID().toString(),
                    title = fileName ?: "External Subtitle",
                    language = detectLanguageFromFileName(fileName),
                    isExternal = true,
                    uri = uri.toString(),
                    format = format
                )
                
                // Save to database
                val entity = SubtitleTrackEntity(
                    id = trackInfo.id,
                    mediaId = mediaId,
                    uri = uri.toString(),
                    lang = trackInfo.language,
                    title = trackInfo.title,
                    external = true,
                    mimeType = formatToMimeType(format),
                    encoding = encoding
                )
                
                subtitleTrackDao.insert(entity)
                
                // Update available tracks
                val currentTracks = _subtitleState.value.availableTracks.toMutableList()
                currentTracks.add(trackInfo)
                _subtitleState.value = _subtitleState.value.copy(
                    availableTracks = currentTracks
                )
                
                Result.success(trackInfo)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    private suspend fun loadSubtitleTrack(track: SubtitleTrackInfo) {
        withContext(Dispatchers.IO) {
            try {
                if (track.uri != null) {
                    val uri = Uri.parse(track.uri)
                    val inputStream = context.contentResolver.openInputStream(uri)
                        ?: throw Exception("Cannot open subtitle file")
                    
                    val parser = parsers.find { it.canParse(track.format) }
                        ?: throw Exception("Unsupported subtitle format: ${track.format}")
                    
                    val encoding = detectEncoding(inputStream)
                    inputStream.close()
                    
                    val parseStream = context.contentResolver.openInputStream(uri)!!
                    currentSubtitles = parser.parse(parseStream, encoding)
                    parseStream.close()
                    
                    lastSearchIndex = 0
                    
                    _subtitleState.value = _subtitleState.value.copy(
                        isEnabled = true,
                        currentTrack = track
                    )
                }
            } catch (e: Exception) {
                // Handle error
                _subtitleState.value = _subtitleState.value.copy(
                    isEnabled = false,
                    currentTrack = null
                )
            }
        }
    }
    
    private suspend fun discoverExternalSubtitles(mediaUri: String): List<SubtitleTrackInfo> {
        return withContext(Dispatchers.IO) {
            val tracks = mutableListOf<SubtitleTrackInfo>()
            
            try {
                val mediaFile = if (mediaUri.startsWith("content://")) {
                    // Handle content URI
                    val documentFile = DocumentFile.fromSingleUri(context, Uri.parse(mediaUri))
                    documentFile?.parentFile
                } else {
                    // Handle file path
                    File(mediaUri).parentFile
                }
                
                if (mediaFile != null) {
                    val mediaBaseName = getBaseName(mediaUri)
                    val subtitleExtensions = listOf("srt", "ass", "ssa", "vtt", "sub")
                    
                    for (ext in subtitleExtensions) {
                        val subtitleFile = File(mediaFile, "$mediaBaseName.$ext")
                        if (subtitleFile.exists()) {
                            val trackInfo = SubtitleTrackInfo(
                                id = UUID.randomUUID().toString(),
                                title = subtitleFile.name,
                                language = detectLanguageFromFileName(subtitleFile.name),
                                isExternal = true,
                                uri = subtitleFile.toURI().toString(),
                                format = detectFormatFromExtension(ext)
                            )
                            tracks.add(trackInfo)
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore errors in discovery
            }
            
            tracks
        }
    }
    
    private fun autoSelectSubtitleTrack(tracks: List<SubtitleTrackInfo>) {
        if (tracks.isEmpty()) return
        
        val config = _subtitleState.value.config
        
        // Priority: default > preferred language > first available
        val selectedTrack = tracks.find { it.isDefault }
            ?: tracks.find { track ->
                config.preferredLanguages.any { lang ->
                    track.language?.startsWith(lang, ignoreCase = true) == true
                }
            }
            ?: tracks.firstOrNull()
        
        selectedTrack?.let { track ->
            kotlinx.coroutines.GlobalScope.launch {
                selectSubtitleTrack(track.id)
            }
        }
    }
    
    // Utility functions
    
    private fun detectEncoding(inputStream: InputStream): String {
        val buffer = ByteArray(4096)
        val detector = UniversalDetector(null)
        
        try {
            var bytesRead: Int
            while (detector.dataAvailable() && inputStream.read(buffer).also { bytesRead = it } > 0) {
                detector.handleData(buffer, 0, bytesRead)
            }
            detector.dataEnd()
            
            return detector.detectedCharset ?: "UTF-8"
        } catch (e: Exception) {
            return "UTF-8"
        } finally {
            detector.reset()
        }
    }
    
    private fun detectFormatFromUri(uri: Uri): SubtitleFormat {
        val path = uri.path ?: return SubtitleFormat.UNKNOWN
        val extension = path.substringAfterLast('.', "").lowercase()
        return detectFormatFromExtension(extension)
    }
    
    private fun detectFormatFromExtension(extension: String): SubtitleFormat {
        return when (extension.lowercase()) {
            "srt" -> SubtitleFormat.SRT
            "ass" -> SubtitleFormat.ASS
            "ssa" -> SubtitleFormat.SSA
            "vtt" -> SubtitleFormat.VTT
            "sub" -> SubtitleFormat.SUB
            else -> SubtitleFormat.UNKNOWN
        }
    }
    
    private fun detectFormatFromMimeType(mimeType: String?): SubtitleFormat {
        return when (mimeType) {
            "text/srt", "application/x-subrip" -> SubtitleFormat.SRT
            "text/vtt" -> SubtitleFormat.VTT
            "text/x-ass" -> SubtitleFormat.ASS
            "text/x-ssa" -> SubtitleFormat.SSA
            else -> SubtitleFormat.UNKNOWN
        }
    }
    
    private fun formatToMimeType(format: SubtitleFormat): String {
        return when (format) {
            SubtitleFormat.SRT -> "text/srt"
            SubtitleFormat.VTT -> "text/vtt"
            SubtitleFormat.ASS -> "text/x-ass"
            SubtitleFormat.SSA -> "text/x-ssa"
            else -> "text/plain"
        }
    }
    
    private fun getBaseName(filePath: String): String {
        val fileName = filePath.substringAfterLast('/')
        return fileName.substringBeforeLast('.')
    }
    
    private fun getFileNameFromUri(uri: Uri): String? {
        return uri.path?.substringAfterLast('/')
    }
    
    private fun detectLanguageFromFileName(fileName: String?): String? {
        if (fileName == null) return null
        
        val languageCodes = mapOf(
            "en" to "English", "zh" to "Chinese", "es" to "Spanish",
            "fr" to "French", "de" to "German", "ja" to "Japanese",
            "ko" to "Korean", "ru" to "Russian", "it" to "Italian",
            "pt" to "Portuguese", "ar" to "Arabic", "hi" to "Hindi"
        )
        
        for ((code, _) in languageCodes) {
            if (fileName.contains(".$code.", ignoreCase = true) ||
                fileName.contains("_$code.", ignoreCase = true) ||
                fileName.endsWith(".$code", ignoreCase = true)) {
                return code
            }
        }
        
        return null
    }
}