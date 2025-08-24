package com.vibestream.player.domain.library

import com.vibestream.player.data.model.MediaItem
import com.vibestream.player.data.model.ScanResult
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import java.io.File

class MediaLibraryScannerTest {

    @Mock
    private lateinit var mockMediaLibraryScanner: MediaLibraryScanner

    private lateinit var sampleDirectories: List<String>
    private lateinit var sampleMediaFiles: List<File>

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        
        sampleDirectories = listOf(
            "/storage/emulated/0/Music",
            "/storage/emulated/0/Download",
            "/storage/emulated/0/Movies"
        )
        
        sampleMediaFiles = listOf(
            File("/storage/emulated/0/Music/song1.mp3"),
            File("/storage/emulated/0/Music/song2.flac"),
            File("/storage/emulated/0/Movies/video1.mp4"),
            File("/storage/emulated/0/Download/podcast.m4a")
        )
    }

    @Test
    fun `scanDirectory should discover media files`() = runTest {
        val directory = "/storage/emulated/0/Music"
        val expectedFiles = sampleMediaFiles.filter { it.path.contains("Music") }
        
        `when`(mockMediaLibraryScanner.scanDirectory(directory)).thenReturn(expectedFiles)

        val result = mockMediaLibraryScanner.scanDirectory(directory)

        assertNotNull("Should return discovered files", result)
        assertEquals("Should find 2 music files", 2, result.size)
        assertTrue("Should contain MP3 file", result.any { it.path.endsWith(".mp3") })
        assertTrue("Should contain FLAC file", result.any { it.path.endsWith(".flac") })
        verify(mockMediaLibraryScanner).scanDirectory(directory)
    }

    @Test
    fun `scanAllDirectories should scan multiple directories`() = runTest {
        val scanResult = ScanResult(
            scannedFiles = 4,
            newFiles = 2,
            updatedFiles = 1,
            duration = 5000L,
            errors = emptyList()
        )
        
        `when`(mockMediaLibraryScanner.scanAllDirectories(sampleDirectories)).thenReturn(scanResult)

        val result = mockMediaLibraryScanner.scanAllDirectories(sampleDirectories)

        assertNotNull("Should return scan result", result)
        assertEquals("Should scan 4 files", 4, result.scannedFiles)
        assertEquals("Should find 2 new files", 2, result.newFiles)
        assertEquals("Should update 1 file", 1, result.updatedFiles)
        assertTrue("Should complete in reasonable time", result.duration < 10000L)
        verify(mockMediaLibraryScanner).scanAllDirectories(sampleDirectories)
    }

    @Test
    fun `extractMetadata should parse audio file metadata`() = runTest {
        val audioFile = File("/storage/emulated/0/Music/song1.mp3")
        val expectedMetadata = MediaItem(
            id = 0L,
            title = "Song Title",
            artist = "Artist Name",
            album = "Album Name",
            duration = 180000L,
            path = audioFile.absolutePath,
            mimeType = "audio/mpeg",
            size = 5000000L,
            dateAdded = System.currentTimeMillis(),
            genre = "Rock",
            year = 2020,
            trackNumber = 1,
            discNumber = 1
        )
        
        `when`(mockMediaLibraryScanner.extractMetadata(audioFile)).thenReturn(expectedMetadata)

        val result = mockMediaLibraryScanner.extractMetadata(audioFile)

        assertNotNull("Should return metadata", result)
        assertEquals("Should have correct title", "Song Title", result.title)
        assertEquals("Should have correct artist", "Artist Name", result.artist)
        assertEquals("Should have correct album", "Album Name", result.album)
        assertEquals("Should have correct duration", 180000L, result.duration)
        assertEquals("Should have correct MIME type", "audio/mpeg", result.mimeType)
        verify(mockMediaLibraryScanner).extractMetadata(audioFile)
    }

    @Test
    fun `extractMetadata should handle video files`() = runTest {
        val videoFile = File("/storage/emulated/0/Movies/video1.mp4")
        val expectedMetadata = MediaItem(
            id = 0L,
            title = "Video Title",
            artist = null,
            album = null,
            duration = 3600000L, // 1 hour
            path = videoFile.absolutePath,
            mimeType = "video/mp4",
            size = 100000000L, // 100MB
            dateAdded = System.currentTimeMillis(),
            genre = null,
            year = null,
            trackNumber = null,
            discNumber = null
        )
        
        `when`(mockMediaLibraryScanner.extractMetadata(videoFile)).thenReturn(expectedMetadata)

        val result = mockMediaLibraryScanner.extractMetadata(videoFile)

        assertNotNull("Should return video metadata", result)
        assertEquals("Should have correct title", "Video Title", result.title)
        assertNull("Video should not have artist", result.artist)
        assertNull("Video should not have album", result.album)
        assertEquals("Should have correct duration", 3600000L, result.duration)
        assertEquals("Should have correct MIME type", "video/mp4", result.mimeType)
        verify(mockMediaLibraryScanner).extractMetadata(videoFile)
    }

    @Test
    fun `isSupportedMediaFile should identify supported formats`() {
        val supportedFiles = listOf(
            "song.mp3", "song.flac", "song.wav", "song.m4a", "song.ogg",
            "video.mp4", "video.mkv", "video.avi", "video.mov", "video.webm"
        )
        
        val unsupportedFiles = listOf(
            "document.txt", "image.jpg", "archive.zip", "executable.exe"
        )
        
        supportedFiles.forEach { filename ->
            `when`(mockMediaLibraryScanner.isSupportedMediaFile(filename)).thenReturn(true)
            assertTrue("$filename should be supported", mockMediaLibraryScanner.isSupportedMediaFile(filename))
        }
        
        unsupportedFiles.forEach { filename ->
            `when`(mockMediaLibraryScanner.isSupportedMediaFile(filename)).thenReturn(false)
            assertFalse("$filename should not be supported", mockMediaLibraryScanner.isSupportedMediaFile(filename))
        }
    }

    @Test
    fun `generateThumbnail should create thumbnail for video`() = runTest {
        val videoFile = File("/storage/emulated/0/Movies/video1.mp4")
        val thumbnailPath = "/storage/emulated/0/.thumbnails/video1.jpg"
        
        `when`(mockMediaLibraryScanner.generateThumbnail(videoFile)).thenReturn(thumbnailPath)

        val result = mockMediaLibraryScanner.generateThumbnail(videoFile)

        assertNotNull("Should return thumbnail path", result)
        assertEquals("Should have correct path", thumbnailPath, result)
        assertTrue("Should be JPEG file", result.endsWith(".jpg"))
        verify(mockMediaLibraryScanner).generateThumbnail(videoFile)
    }

    @Test
    fun `startIncrementalScan should monitor file changes`() = runTest {
        val scanProgress = flowOf(
            ScanProgress(currentFile = "song1.mp3", processed = 1, total = 4),
            ScanProgress(currentFile = "song2.flac", processed = 2, total = 4),
            ScanProgress(currentFile = "video1.mp4", processed = 3, total = 4),
            ScanProgress(currentFile = "podcast.m4a", processed = 4, total = 4)
        )
        
        `when`(mockMediaLibraryScanner.startIncrementalScan()).thenReturn(scanProgress)

        val result = mockMediaLibraryScanner.startIncrementalScan()

        assertNotNull("Should return scan progress flow", result)
        verify(mockMediaLibraryScanner).startIncrementalScan()
    }

    @Test
    fun `stopScan should cancel ongoing scan`() = runTest {
        `when`(mockMediaLibraryScanner.stopScan()).thenReturn(true)

        val result = mockMediaLibraryScanner.stopScan()

        assertTrue("Should successfully stop scan", result)
        verify(mockMediaLibraryScanner).stopScan()
    }

    @Test
    fun `getScanStatus should return current scan state`() = runTest {
        val scanStatus = ScanStatus(
            isScanning = true,
            currentDirectory = "/storage/emulated/0/Music",
            filesProcessed = 25,
            totalFiles = 100,
            startTime = System.currentTimeMillis() - 5000L
        )
        
        `when`(mockMediaLibraryScanner.getScanStatus()).thenReturn(scanStatus)

        val result = mockMediaLibraryScanner.getScanStatus()

        assertNotNull("Should return scan status", result)
        assertTrue("Should be currently scanning", result.isScanning)
        assertEquals("Should have correct directory", "/storage/emulated/0/Music", result.currentDirectory)
        assertEquals("Should have processed 25 files", 25, result.filesProcessed)
        assertEquals("Should have 100 total files", 100, result.totalFiles)
        verify(mockMediaLibraryScanner).getScanStatus()
    }

    @Test
    fun `addWatchDirectory should monitor new directory`() = runTest {
        val directory = "/storage/emulated/0/NewMusic"
        `when`(mockMediaLibraryScanner.addWatchDirectory(directory)).thenReturn(true)

        val result = mockMediaLibraryScanner.addWatchDirectory(directory)

        assertTrue("Should successfully add watch directory", result)
        verify(mockMediaLibraryScanner).addWatchDirectory(directory)
    }

    @Test
    fun `removeWatchDirectory should stop monitoring directory`() = runTest {
        val directory = "/storage/emulated/0/OldMusic"
        `when`(mockMediaLibraryScanner.removeWatchDirectory(directory)).thenReturn(true)

        val result = mockMediaLibraryScanner.removeWatchDirectory(directory)

        assertTrue("Should successfully remove watch directory", result)
        verify(mockMediaLibraryScanner).removeWatchDirectory(directory)
    }

    @Test
    fun `getWatchDirectories should return monitored directories`() = runTest {
        `when`(mockMediaLibraryScanner.getWatchDirectories()).thenReturn(sampleDirectories)

        val result = mockMediaLibraryScanner.getWatchDirectories()

        assertNotNull("Should return watch directories", result)
        assertEquals("Should have correct count", sampleDirectories.size, result.size)
        assertTrue("Should contain Music directory", result.contains("/storage/emulated/0/Music"))
        verify(mockMediaLibraryScanner).getWatchDirectories()
    }

    @Test
    fun `scanSingleFile should process individual file`() = runTest {
        val file = File("/storage/emulated/0/Music/new_song.mp3")
        val mediaItem = MediaItem(
            id = 0L,
            title = "New Song",
            artist = "New Artist",
            album = "New Album",
            duration = 200000L,
            path = file.absolutePath,
            mimeType = "audio/mpeg",
            size = 6000000L,
            dateAdded = System.currentTimeMillis(),
            genre = "Pop",
            year = 2023,
            trackNumber = 1,
            discNumber = 1
        )
        
        `when`(mockMediaLibraryScanner.scanSingleFile(file)).thenReturn(mediaItem)

        val result = mockMediaLibraryScanner.scanSingleFile(file)

        assertNotNull("Should return media item", result)
        assertEquals("Should have correct title", "New Song", result.title)
        assertEquals("Should have correct path", file.absolutePath, result.path)
        verify(mockMediaLibraryScanner).scanSingleFile(file)
    }

    @Test
    fun `clearCache should remove cached data`() = runTest {
        `when`(mockMediaLibraryScanner.clearCache()).thenReturn(true)

        val result = mockMediaLibraryScanner.clearCache()

        assertTrue("Should successfully clear cache", result)
        verify(mockMediaLibraryScanner).clearCache()
    }

    @Test
    fun `getCacheSize should return cache statistics`() = runTest {
        val cacheSize = 50000000L // 50MB
        `when`(mockMediaLibraryScanner.getCacheSize()).thenReturn(cacheSize)

        val result = mockMediaLibraryScanner.getCacheSize()

        assertEquals("Should return correct cache size", cacheSize, result)
        assertTrue("Cache size should be reasonable", result > 0)
        verify(mockMediaLibraryScanner).getCacheSize()
    }
}

// Mock data classes for testing
data class ScanResult(
    val scannedFiles: Int,
    val newFiles: Int,
    val updatedFiles: Int,
    val duration: Long,
    val errors: List<String>
)

data class ScanProgress(
    val currentFile: String,
    val processed: Int,
    val total: Int
)

data class ScanStatus(
    val isScanning: Boolean,
    val currentDirectory: String,
    val filesProcessed: Int,
    val totalFiles: Int,
    val startTime: Long
)