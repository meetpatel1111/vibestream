package com.vibestream.player.test.util

import com.vibestream.player.data.model.MediaItem
import com.vibestream.player.data.model.Playlist
import com.vibestream.player.data.model.PlaylistType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.mockito.Mockito
import org.mockito.stubbing.OngoingStubbing

/**
 * Test utilities for creating mock data and common test operations
 */
object TestDataFactory {

    /**
     * Creates a sample MediaItem for testing
     */
    fun createMediaItem(
        id: Long = 1L,
        title: String = "Test Song",
        artist: String = "Test Artist",
        album: String = "Test Album",
        duration: Long = 180000L,
        path: String = "/test/path.mp3",
        mimeType: String = "audio/mpeg"
    ): MediaItem = MediaItem(
        id = id,
        title = title,
        artist = artist,
        album = album,
        duration = duration,
        path = path,
        mimeType = mimeType,
        size = 5000000L,
        dateAdded = System.currentTimeMillis(),
        genre = "Rock",
        year = 2020,
        trackNumber = 1,
        discNumber = 1
    )

    /**
     * Creates a list of sample MediaItems for testing
     */
    fun createMediaItemList(count: Int = 3): List<MediaItem> = 
        (1..count).map { index ->
            createMediaItem(
                id = index.toLong(),
                title = "Song $index",
                artist = "Artist $index",
                album = "Album ${(index - 1) / 3 + 1}",
                trackNumber = index
            )
        }

    /**
     * Creates a sample Playlist for testing
     */
    fun createPlaylist(
        id: Long = 1L,
        name: String = "Test Playlist",
        type: PlaylistType = PlaylistType.USER_CREATED,
        mediaItemIds: List<Long> = listOf(1L, 2L, 3L)
    ): Playlist = Playlist(
        id = id,
        name = name,
        type = type,
        description = "Test playlist description",
        mediaItemIds = mediaItemIds,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        artworkPath = null
    )

    /**
     * Creates a sample SRT subtitle content for testing
     */
    fun createSrtSubtitleContent(): String = """
        1
        00:00:01,000 --> 00:00:03,000
        Hello World
        
        2
        00:00:04,000 --> 00:00:06,000
        This is a test subtitle
        
        3
        00:00:07,500 --> 00:00:09,500
        <i>Italic text</i> and <b>bold text</b>
    """.trimIndent()

    /**
     * Creates a sample VTT subtitle content for testing
     */
    fun createVttSubtitleContent(): String = """
        WEBVTT
        
        00:00:01.000 --> 00:00:03.000
        Hello VTT World
        
        00:00:04.000 --> 00:00:06.000
        This is a VTT test subtitle
        
        00:00:07.500 --> 00:00:09.500
        <c.yellow>Colored text</c>
    """.trimIndent()

    /**
     * Creates a sample ASS subtitle content for testing
     */
    fun createAssSubtitleContent(): String = """
        [Script Info]
        Title: Test ASS Subtitles
        ScriptType: v4.00+
        
        [V4+ Styles]
        Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour
        Style: Default,Arial,20,&H00FFFFFF,&H000000FF
        
        [Events]
        Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
        Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Hello ASS World
        Dialogue: 0,0:00:04.00,0:00:06.00,Default,,0,0,0,,This is an ASS test
        Dialogue: 0,0:00:07.50,0:00:09.50,Default,,0,0,0,,{\b1}Bold text{\b0}
    """.trimIndent()

    /**
     * Creates audio metadata map for testing
     */
    fun createAudioMetadata(): Map<String, String> = mapOf(
        "title" to "Test Song",
        "artist" to "Test Artist",
        "album" to "Test Album",
        "genre" to "Rock",
        "year" to "2020",
        "track" to "1",
        "disc" to "1",
        "duration" to "180000"
    )

    /**
     * Creates video metadata map for testing
     */
    fun createVideoMetadata(): Map<String, String> = mapOf(
        "title" to "Test Video",
        "duration" to "3600000",
        "width" to "1920",
        "height" to "1080",
        "framerate" to "30",
        "bitrate" to "5000000"
    )
}

/**
 * Extension functions for easier test setup
 */
object TestExtensions {

    /**
     * Creates a flow from a single value for testing reactive components
     */
    fun <T> T.asFlow(): Flow<T> = flowOf(this)

    /**
     * Mockito extension for stubbing with better Kotlin support
     */
    inline fun <reified T> mock(): T = Mockito.mock(T::class.java)

    /**
     * Extension for stubbing suspend functions
     */
    suspend fun <T> OngoingStubbing<T>.thenReturnSuspend(value: T): OngoingStubbing<T> = 
        this.thenReturn(value)
}

/**
 * Test assertions for media-specific validations
 */
object MediaTestAssertions {

    /**
     * Asserts that a MediaItem has valid properties
     */
    fun assertValidMediaItem(mediaItem: MediaItem) {
        assert(mediaItem.id >= 0) { "Media item ID should be non-negative" }
        assert(mediaItem.title.isNotBlank()) { "Media item title should not be blank" }
        assert(mediaItem.path.isNotBlank()) { "Media item path should not be blank" }
        assert(mediaItem.duration >= 0) { "Media item duration should be non-negative" }
        assert(mediaItem.size >= 0) { "Media item size should be non-negative" }
        assert(mediaItem.mimeType.isNotBlank()) { "Media item MIME type should not be blank" }
    }

    /**
     * Asserts that a Playlist has valid properties
     */
    fun assertValidPlaylist(playlist: Playlist) {
        assert(playlist.id >= 0) { "Playlist ID should be non-negative" }
        assert(playlist.name.isNotBlank()) { "Playlist name should not be blank" }
        assert(playlist.createdAt > 0) { "Playlist creation time should be valid" }
        assert(playlist.updatedAt >= playlist.createdAt) { "Playlist update time should be after creation" }
    }

    /**
     * Asserts that subtitle timing is valid
     */
    fun assertValidSubtitleTiming(startTime: Long, endTime: Long) {
        assert(startTime >= 0) { "Start time should be non-negative" }
        assert(endTime > startTime) { "End time should be after start time" }
    }

    /**
     * Asserts that audio settings are within valid ranges
     */
    fun assertValidAudioSettings(
        volume: Float? = null,
        speed: Float? = null,
        gain: Float? = null
    ) {
        volume?.let { assert(it in 0f..1f) { "Volume should be between 0 and 1" } }
        speed?.let { assert(it > 0f) { "Speed should be positive" } }
        gain?.let { assert(it in -20f..20f) { "Gain should be between -20 and 20 dB" } }
    }
}

/**
 * Mock builders for complex objects
 */
class MockPlayerBuilder {
    private var isPlaying = false
    private var currentPosition = 0L
    private var duration = 0L
    private var speed = 1.0f
    private var volume = 1.0f

    fun playing(playing: Boolean) = apply { isPlaying = playing }
    fun position(position: Long) = apply { currentPosition = position }
    fun duration(duration: Long) = apply { this.duration = duration }
    fun speed(speed: Float) = apply { this.speed = speed }
    fun volume(volume: Float) = apply { this.volume = volume }

    fun build(): MockPlayerState = MockPlayerState(
        isPlaying = isPlaying,
        currentPosition = currentPosition,
        duration = duration,
        speed = speed,
        volume = volume
    )
}

data class MockPlayerState(
    val isPlaying: Boolean,
    val currentPosition: Long,
    val duration: Long,
    val speed: Float,
    val volume: Float
)

/**
 * Test configurations for different scenarios
 */
object TestConfigurations {

    fun audioOnlyConfig() = TestConfig(
        includeVideo = false,
        includeAudio = true,
        includeSubtitles = false
    )

    fun videoWithSubtitlesConfig() = TestConfig(
        includeVideo = true,
        includeAudio = true,
        includeSubtitles = true
    )

    fun playlistConfig(itemCount: Int = 10) = TestConfig(
        includeVideo = true,
        includeAudio = true,
        includeSubtitles = false,
        playlistSize = itemCount
    )
}

data class TestConfig(
    val includeVideo: Boolean = true,
    val includeAudio: Boolean = true,
    val includeSubtitles: Boolean = false,
    val playlistSize: Int = 5
)

/**
 * Performance test utilities
 */
object PerformanceTestUtils {

    /**
     * Measures execution time of a block
     */
    inline fun <T> measureTimeMillis(block: () -> T): Pair<T, Long> {
        val startTime = System.currentTimeMillis()
        val result = block()
        val endTime = System.currentTimeMillis()
        return result to (endTime - startTime)
    }

    /**
     * Asserts that an operation completes within a time limit
     */
    inline fun <T> assertCompletesWithin(timeoutMs: Long, block: () -> T): T {
        val (result, duration) = measureTimeMillis(block)
        assert(duration <= timeoutMs) { 
            "Operation took ${duration}ms, expected <= ${timeoutMs}ms" 
        }
        return result
    }

    /**
     * Creates a large dataset for performance testing
     */
    fun createLargeMediaLibrary(size: Int = 1000): List<MediaItem> = 
        (1..size).map { index ->
            TestDataFactory.createMediaItem(
                id = index.toLong(),
                title = "Song $index",
                artist = "Artist ${index % 50 + 1}",
                album = "Album ${index % 100 + 1}"
            )
        }
}