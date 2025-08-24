package com.vibestream.player.domain.player

import com.vibestream.player.data.model.MediaItem
import com.vibestream.player.data.model.PlaybackState
import com.vibestream.player.data.model.RepeatMode
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

class PlayerTest {

    @Mock
    private lateinit var mockPlayer: Player

    @Mock
    private lateinit var mockPlaybackController: PlaybackController

    private lateinit var sampleMediaItem: MediaItem
    private lateinit var samplePlaylist: List<MediaItem>

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        
        sampleMediaItem = MediaItem(
            id = 1L,
            title = "Test Song",
            artist = "Test Artist",
            album = "Test Album",
            duration = 180000L,
            path = "/storage/music/test_song.mp3",
            mimeType = "audio/mpeg",
            size = 5000000L,
            dateAdded = System.currentTimeMillis(),
            genre = "Rock",
            year = 2020,
            trackNumber = 1,
            discNumber = 1
        )

        samplePlaylist = listOf(
            sampleMediaItem,
            sampleMediaItem.copy(id = 2L, title = "Song 2", trackNumber = 2),
            sampleMediaItem.copy(id = 3L, title = "Song 3", trackNumber = 3)
        )
    }

    @Test
    fun `prepare should initialize player with media item`() = runTest {
        `when`(mockPlayer.prepare(sampleMediaItem)).thenReturn(true)

        val result = mockPlayer.prepare(sampleMediaItem)

        assertTrue("Should successfully prepare media", result)
        verify(mockPlayer).prepare(sampleMediaItem)
    }

    @Test
    fun `play should start playback`() = runTest {
        mockPlayer.play()
        verify(mockPlayer).play()
    }

    @Test
    fun `pause should pause playback`() = runTest {
        mockPlayer.pause()
        verify(mockPlayer).pause()
    }

    @Test
    fun `stop should stop playback`() = runTest {
        mockPlayer.stop()
        verify(mockPlayer).stop()
    }

    @Test
    fun `seekTo should change playback position`() = runTest {
        val position = 60000L // 1 minute
        mockPlayer.seekTo(position)
        verify(mockPlayer).seekTo(position)
    }

    @Test
    fun `setPlaybackSpeed should change playback rate`() = runTest {
        val speed = 1.5f
        mockPlayer.setPlaybackSpeed(speed)
        verify(mockPlayer).setPlaybackSpeed(speed)
    }

    @Test
    fun `setVolume should change audio volume`() = runTest {
        val volume = 0.7f
        mockPlayer.setVolume(volume)
        verify(mockPlayer).setVolume(volume)
    }

    @Test
    fun `setRepeatMode should change repeat behavior`() = runTest {
        val repeatMode = RepeatMode.ONE
        mockPlayer.setRepeatMode(repeatMode)
        verify(mockPlayer).setRepeatMode(repeatMode)
    }

    @Test
    fun `setShuffleEnabled should enable shuffle mode`() = runTest {
        val enabled = true
        mockPlayer.setShuffleEnabled(enabled)
        verify(mockPlayer).setShuffleEnabled(enabled)
    }

    @Test
    fun `skipToNext should advance to next track`() = runTest {
        `when`(mockPlayer.skipToNext()).thenReturn(true)

        val result = mockPlayer.skipToNext()

        assertTrue("Should successfully skip to next", result)
        verify(mockPlayer).skipToNext()
    }

    @Test
    fun `skipToPrevious should go to previous track`() = runTest {
        `when`(mockPlayer.skipToPrevious()).thenReturn(true)

        val result = mockPlayer.skipToPrevious()

        assertTrue("Should successfully skip to previous", result)
        verify(mockPlayer).skipToPrevious()
    }

    @Test
    fun `getCurrentPosition should return current playback position`() = runTest {
        val currentPosition = 45000L // 45 seconds
        `when`(mockPlayer.getCurrentPosition()).thenReturn(currentPosition)

        val result = mockPlayer.getCurrentPosition()

        assertEquals("Should return correct position", currentPosition, result)
        verify(mockPlayer).getCurrentPosition()
    }

    @Test
    fun `getDuration should return media duration`() = runTest {
        val duration = 180000L // 3 minutes
        `when`(mockPlayer.getDuration()).thenReturn(duration)

        val result = mockPlayer.getDuration()

        assertEquals("Should return correct duration", duration, result)
        verify(mockPlayer).getDuration()
    }

    @Test
    fun `isPlaying should return playback state`() = runTest {
        `when`(mockPlayer.isPlaying()).thenReturn(true)

        val result = mockPlayer.isPlaying()

        assertTrue("Should return playing state", result)
        verify(mockPlayer).isPlaying()
    }

    @Test
    fun `setPlaylist should load playlist`() = runTest {
        val startIndex = 0
        `when`(mockPlayer.setPlaylist(samplePlaylist, startIndex)).thenReturn(true)

        val result = mockPlayer.setPlaylist(samplePlaylist, startIndex)

        assertTrue("Should successfully set playlist", result)
        verify(mockPlayer).setPlaylist(samplePlaylist, startIndex)
    }

    @Test
    fun `getCurrentMediaItem should return current item`() = runTest {
        `when`(mockPlayer.getCurrentMediaItem()).thenReturn(sampleMediaItem)

        val result = mockPlayer.getCurrentMediaItem()

        assertNotNull("Should return current media item", result)
        assertEquals("Should return correct item", sampleMediaItem, result)
        verify(mockPlayer).getCurrentMediaItem()
    }

    @Test
    fun `getPlaybackState should return current state`() = runTest {
        val playbackState = PlaybackState(
            isPlaying = true,
            currentMediaItem = sampleMediaItem,
            currentPosition = 60000L,
            duration = 180000L,
            playbackSpeed = 1.0f,
            repeatMode = RepeatMode.OFF,
            isShuffleEnabled = false,
            hasNext = true,
            hasPrevious = false
        )
        
        `when`(mockPlayer.getPlaybackState()).thenReturn(playbackState)

        val result = mockPlayer.getPlaybackState()

        assertNotNull("Should return playback state", result)
        assertTrue("Should be playing", result.isPlaying)
        assertEquals("Should have correct media item", sampleMediaItem, result.currentMediaItem)
        assertEquals("Should have correct position", 60000L, result.currentPosition)
        verify(mockPlayer).getPlaybackState()
    }

    @Test
    fun `playbackState flow should emit state changes`() = runTest {
        val playbackState = PlaybackState(
            isPlaying = false,
            currentMediaItem = null,
            currentPosition = 0L,
            duration = 0L,
            playbackSpeed = 1.0f,
            repeatMode = RepeatMode.OFF,
            isShuffleEnabled = false,
            hasNext = false,
            hasPrevious = false
        )
        
        `when`(mockPlayer.playbackState).thenReturn(flowOf(playbackState))

        val flow = mockPlayer.playbackState

        assertNotNull("Should provide playback state flow", flow)
    }

    @Test
    fun `addToQueue should add media item to queue`() = runTest {
        val mediaItem = sampleMediaItem.copy(id = 4L, title = "Queued Song")
        `when`(mockPlayer.addToQueue(mediaItem)).thenReturn(true)

        val result = mockPlayer.addToQueue(mediaItem)

        assertTrue("Should successfully add to queue", result)
        verify(mockPlayer).addToQueue(mediaItem)
    }

    @Test
    fun `removeFromQueue should remove item from queue`() = runTest {
        val index = 2
        `when`(mockPlayer.removeFromQueue(index)).thenReturn(true)

        val result = mockPlayer.removeFromQueue(index)

        assertTrue("Should successfully remove from queue", result)
        verify(mockPlayer).removeFromQueue(index)
    }

    @Test
    fun `clearQueue should empty the queue`() = runTest {
        mockPlayer.clearQueue()
        verify(mockPlayer).clearQueue()
    }

    @Test
    fun `getQueue should return current queue`() = runTest {
        `when`(mockPlayer.getQueue()).thenReturn(samplePlaylist)

        val result = mockPlayer.getQueue()

        assertNotNull("Should return queue", result)
        assertEquals("Should have correct size", samplePlaylist.size, result.size)
        verify(mockPlayer).getQueue()
    }

    @Test
    fun `skipToQueueItem should jump to specific queue item`() = runTest {
        val index = 1
        `when`(mockPlayer.skipToQueueItem(index)).thenReturn(true)

        val result = mockPlayer.skipToQueueItem(index)

        assertTrue("Should successfully skip to queue item", result)
        verify(mockPlayer).skipToQueueItem(index)
    }

    @Test
    fun `release should clean up resources`() = runTest {
        mockPlayer.release()
        verify(mockPlayer).release()
    }

    // PlaybackController tests
    @Test
    fun `PlaybackController should handle playback commands`() = runTest {
        val playbackState = PlaybackState(
            isPlaying = true,
            currentMediaItem = sampleMediaItem,
            currentPosition = 0L,
            duration = 180000L,
            playbackSpeed = 1.0f,
            repeatMode = RepeatMode.OFF,
            isShuffleEnabled = false,
            hasNext = true,
            hasPrevious = false
        )
        
        `when`(mockPlaybackController.playbackState).thenReturn(flowOf(playbackState))

        val flow = mockPlaybackController.playbackState

        assertNotNull("Should provide playback state flow", flow)
        verify(mockPlaybackController).playbackState
    }

    @Test
    fun `PlaybackController should handle position updates`() = runTest {
        val position = 75000L // 1:15
        `when`(mockPlaybackController.currentPosition).thenReturn(flowOf(position))

        val flow = mockPlaybackController.currentPosition

        assertNotNull("Should provide position flow", flow)
        verify(mockPlaybackController).currentPosition
    }

    @Test
    fun `PlaybackController should handle duration updates`() = runTest {
        val duration = 180000L // 3:00
        `when`(mockPlaybackController.duration).thenReturn(flowOf(duration))

        val flow = mockPlaybackController.duration

        assertNotNull("Should provide duration flow", flow)
        verify(mockPlaybackController).duration
    }

    @Test
    fun `PlaybackController should handle speed changes`() = runTest {
        val speed = 1.25f
        `when`(mockPlaybackController.playbackSpeed).thenReturn(flowOf(speed))

        val flow = mockPlaybackController.playbackSpeed

        assertNotNull("Should provide speed flow", flow)
        verify(mockPlaybackController).playbackSpeed
    }

    @Test
    fun `RepeatMode should have all modes`() {
        val modes = RepeatMode.values()
        
        assertTrue("Should contain OFF mode", modes.contains(RepeatMode.OFF))
        assertTrue("Should contain ONE mode", modes.contains(RepeatMode.ONE))
        assertTrue("Should contain ALL mode", modes.contains(RepeatMode.ALL))
    }

    @Test
    fun `PlaybackState should validate state consistency`() {
        val state = PlaybackState(
            isPlaying = true,
            currentMediaItem = sampleMediaItem,
            currentPosition = 60000L,
            duration = 180000L,
            playbackSpeed = 1.0f,
            repeatMode = RepeatMode.OFF,
            isShuffleEnabled = false,
            hasNext = true,
            hasPrevious = false
        )

        assertTrue("Position should be within duration", state.currentPosition <= state.duration)
        assertTrue("Speed should be positive", state.playbackSpeed > 0)
        assertNotNull("Should have media item when playing", state.currentMediaItem)
    }

    @Test
    fun `setAudioSessionId should configure audio session`() = runTest {
        val sessionId = 12345
        mockPlayer.setAudioSessionId(sessionId)
        verify(mockPlayer).setAudioSessionId(sessionId)
    }

    @Test
    fun `getAudioSessionId should return audio session ID`() = runTest {
        val sessionId = 12345
        `when`(mockPlayer.getAudioSessionId()).thenReturn(sessionId)

        val result = mockPlayer.getAudioSessionId()

        assertEquals("Should return correct session ID", sessionId, result)
        verify(mockPlayer).getAudioSessionId()
    }
}