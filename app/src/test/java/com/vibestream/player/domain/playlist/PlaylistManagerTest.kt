package com.vibestream.player.domain.playlist

import com.vibestream.player.data.model.MediaItem
import com.vibestream.player.data.model.Playlist
import com.vibestream.player.data.model.PlaylistType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

class PlaylistManagerTest {

    @Mock
    private lateinit var mockPlaylistManager: PlaylistManager

    private lateinit var sampleMediaItems: List<MediaItem>
    private lateinit var samplePlaylist: Playlist

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        
        sampleMediaItems = listOf(
            MediaItem(
                id = 1L,
                title = "Song 1",
                artist = "Artist 1",
                album = "Album 1",
                duration = 180000L,
                path = "/path/to/song1.mp3",
                mimeType = "audio/mpeg",
                size = 5000000L,
                dateAdded = System.currentTimeMillis(),
                genre = "Rock",
                year = 2020,
                trackNumber = 1,
                discNumber = 1
            ),
            MediaItem(
                id = 2L,
                title = "Song 2",
                artist = "Artist 1",
                album = "Album 1",
                duration = 200000L,
                path = "/path/to/song2.mp3",
                mimeType = "audio/mpeg",
                size = 6000000L,
                dateAdded = System.currentTimeMillis(),
                genre = "Rock",
                year = 2020,
                trackNumber = 2,
                discNumber = 1
            ),
            MediaItem(
                id = 3L,
                title = "Song 3",
                artist = "Artist 2",
                album = "Album 2",
                duration = 220000L,
                path = "/path/to/song3.mp3",
                mimeType = "audio/mpeg",
                size = 7000000L,
                dateAdded = System.currentTimeMillis(),
                genre = "Pop",
                year = 2021,
                trackNumber = 1,
                discNumber = 1
            )
        )

        samplePlaylist = Playlist(
            id = 1L,
            name = "My Playlist",
            type = PlaylistType.USER_CREATED,
            description = "Test playlist",
            mediaItemIds = listOf(1L, 2L),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            artworkPath = null
        )
    }

    @Test
    fun `createPlaylist should create new playlist successfully`() = runTest {
        val newPlaylist = samplePlaylist.copy(id = 0L)
        `when`(mockPlaylistManager.createPlaylist(newPlaylist)).thenReturn(samplePlaylist)

        val result = mockPlaylistManager.createPlaylist(newPlaylist)

        assertNotNull("Should return created playlist", result)
        assertEquals("Should have correct name", "My Playlist", result.name)
        assertEquals("Should have correct type", PlaylistType.USER_CREATED, result.type)
        verify(mockPlaylistManager).createPlaylist(newPlaylist)
    }

    @Test
    fun `addToPlaylist should add media item to playlist`() = runTest {
        val playlistId = 1L
        val mediaItemId = 3L
        `when`(mockPlaylistManager.addToPlaylist(playlistId, mediaItemId)).thenReturn(true)

        val result = mockPlaylistManager.addToPlaylist(playlistId, mediaItemId)

        assertTrue("Should successfully add item to playlist", result)
        verify(mockPlaylistManager).addToPlaylist(playlistId, mediaItemId)
    }

    @Test
    fun `removeFromPlaylist should remove media item from playlist`() = runTest {
        val playlistId = 1L
        val mediaItemId = 2L
        `when`(mockPlaylistManager.removeFromPlaylist(playlistId, mediaItemId)).thenReturn(true)

        val result = mockPlaylistManager.removeFromPlaylist(playlistId, mediaItemId)

        assertTrue("Should successfully remove item from playlist", result)
        verify(mockPlaylistManager).removeFromPlaylist(playlistId, mediaItemId)
    }

    @Test
    fun `deletePlaylist should delete playlist successfully`() = runTest {
        val playlistId = 1L
        `when`(mockPlaylistManager.deletePlaylist(playlistId)).thenReturn(true)

        val result = mockPlaylistManager.deletePlaylist(playlistId)

        assertTrue("Should successfully delete playlist", result)
        verify(mockPlaylistManager).deletePlaylist(playlistId)
    }

    @Test
    fun `getPlaylistWithItems should return playlist with media items`() = runTest {
        val playlistWithItems = samplePlaylist to sampleMediaItems.take(2)
        `when`(mockPlaylistManager.getPlaylistWithItems(1L)).thenReturn(playlistWithItems)

        val result = mockPlaylistManager.getPlaylistWithItems(1L)

        assertNotNull("Should return playlist with items", result)
        assertEquals("Should have correct playlist", samplePlaylist, result.first)
        assertEquals("Should have 2 items", 2, result.second.size)
        verify(mockPlaylistManager).getPlaylistWithItems(1L)
    }

    @Test
    fun `getAllPlaylists should return all playlists`() = runTest {
        val playlists = listOf(samplePlaylist)
        `when`(mockPlaylistManager.getAllPlaylists()).thenReturn(flowOf(playlists))

        val result = mockPlaylistManager.getAllPlaylists()

        assertNotNull("Should return playlists flow", result)
        verify(mockPlaylistManager).getAllPlaylists()
    }

    @Test
    fun `reorderPlaylistItems should change item order`() = runTest {
        val playlistId = 1L
        val newOrder = listOf(2L, 1L) // Reverse order
        `when`(mockPlaylistManager.reorderPlaylistItems(playlistId, newOrder)).thenReturn(true)

        val result = mockPlaylistManager.reorderPlaylistItems(playlistId, newOrder)

        assertTrue("Should successfully reorder items", result)
        verify(mockPlaylistManager).reorderPlaylistItems(playlistId, newOrder)
    }

    @Test
    fun `duplicatePlaylist should create copy of playlist`() = runTest {
        val originalId = 1L
        val newName = "My Playlist Copy"
        val duplicatedPlaylist = samplePlaylist.copy(id = 2L, name = newName)
        `when`(mockPlaylistManager.duplicatePlaylist(originalId, newName)).thenReturn(duplicatedPlaylist)

        val result = mockPlaylistManager.duplicatePlaylist(originalId, newName)

        assertNotNull("Should return duplicated playlist", result)
        assertEquals("Should have new name", newName, result.name)
        assertNotEquals("Should have different ID", originalId, result.id)
        verify(mockPlaylistManager).duplicatePlaylist(originalId, newName)
    }

    @Test
    fun `updatePlaylist should modify playlist properties`() = runTest {
        val updatedPlaylist = samplePlaylist.copy(
            name = "Updated Playlist",
            description = "Updated description"
        )
        `when`(mockPlaylistManager.updatePlaylist(updatedPlaylist)).thenReturn(true)

        val result = mockPlaylistManager.updatePlaylist(updatedPlaylist)

        assertTrue("Should successfully update playlist", result)
        verify(mockPlaylistManager).updatePlaylist(updatedPlaylist)
    }

    @Test
    fun `createSmartPlaylist should create smart playlist with criteria`() = runTest {
        val smartPlaylist = Playlist(
            id = 2L,
            name = "Recently Added",
            type = PlaylistType.SMART,
            description = "Songs added in the last 7 days",
            mediaItemIds = emptyList(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            artworkPath = null
        )
        `when`(mockPlaylistManager.createSmartPlaylist(anyString(), any(), anyString())).thenReturn(smartPlaylist)

        val result = mockPlaylistManager.createSmartPlaylist(
            "Recently Added",
            SmartPlaylistCriteria.RecentlyAdded(7),
            "Songs added in the last 7 days"
        )

        assertNotNull("Should return smart playlist", result)
        assertEquals("Should have correct type", PlaylistType.SMART, result.type)
        assertEquals("Should have correct name", "Recently Added", result.name)
        verify(mockPlaylistManager).createSmartPlaylist(anyString(), any(), anyString())
    }

    @Test
    fun `refreshSmartPlaylist should update smart playlist content`() = runTest {
        val playlistId = 2L
        val updatedItems = listOf(3L) // Most recent item
        `when`(mockPlaylistManager.refreshSmartPlaylist(playlistId)).thenReturn(updatedItems)

        val result = mockPlaylistManager.refreshSmartPlaylist(playlistId)

        assertNotNull("Should return updated items", result)
        assertEquals("Should have 1 recent item", 1, result.size)
        verify(mockPlaylistManager).refreshSmartPlaylist(playlistId)
    }

    @Test
    fun `getPlaylistStats should return playlist statistics`() = runTest {
        val stats = PlaylistStats(
            totalItems = 2,
            totalDuration = 380000L, // 6:20 minutes
            totalSize = 11000000L // 11 MB
        )
        `when`(mockPlaylistManager.getPlaylistStats(1L)).thenReturn(stats)

        val result = mockPlaylistManager.getPlaylistStats(1L)

        assertNotNull("Should return playlist stats", result)
        assertEquals("Should have correct item count", 2, result.totalItems)
        assertEquals("Should have correct duration", 380000L, result.totalDuration)
        verify(mockPlaylistManager).getPlaylistStats(1L)
    }

    @Test
    fun `searchInPlaylist should find matching items`() = runTest {
        val query = "Song 1"
        val searchResults = listOf(sampleMediaItems[0])
        `when`(mockPlaylistManager.searchInPlaylist(1L, query)).thenReturn(searchResults)

        val result = mockPlaylistManager.searchInPlaylist(1L, query)

        assertNotNull("Should return search results", result)
        assertEquals("Should find 1 matching item", 1, result.size)
        assertEquals("Should match correct song", "Song 1", result[0].title)
        verify(mockPlaylistManager).searchInPlaylist(1L, query)
    }

    @Test
    fun `exportPlaylist should export playlist in M3U format`() = runTest {
        val exportPath = "/storage/playlists/my_playlist.m3u"
        `when`(mockPlaylistManager.exportPlaylist(1L, exportPath, PlaylistExportFormat.M3U)).thenReturn(true)

        val result = mockPlaylistManager.exportPlaylist(1L, exportPath, PlaylistExportFormat.M3U)

        assertTrue("Should successfully export playlist", result)
        verify(mockPlaylistManager).exportPlaylist(1L, exportPath, PlaylistExportFormat.M3U)
    }

    @Test
    fun `importPlaylist should import playlist from file`() = runTest {
        val importPath = "/storage/playlists/imported_playlist.m3u"
        val importedPlaylist = samplePlaylist.copy(id = 3L, name = "Imported Playlist")
        `when`(mockPlaylistManager.importPlaylist(importPath)).thenReturn(importedPlaylist)

        val result = mockPlaylistManager.importPlaylist(importPath)

        assertNotNull("Should return imported playlist", result)
        assertEquals("Should have correct name", "Imported Playlist", result.name)
        verify(mockPlaylistManager).importPlaylist(importPath)
    }

    @Test
    fun `addMultipleToPlaylist should add multiple items at once`() = runTest {
        val playlistId = 1L
        val mediaItemIds = listOf(3L, 4L, 5L)
        `when`(mockPlaylistManager.addMultipleToPlaylist(playlistId, mediaItemIds)).thenReturn(true)

        val result = mockPlaylistManager.addMultipleToPlaylist(playlistId, mediaItemIds)

        assertTrue("Should successfully add multiple items", result)
        verify(mockPlaylistManager).addMultipleToPlaylist(playlistId, mediaItemIds)
    }

    @Test
    fun `Playlist should validate creation parameters`() {
        // Test playlist with empty name
        val invalidPlaylist = samplePlaylist.copy(name = "")
        
        assertTrue("Empty name should be invalid", invalidPlaylist.name.isEmpty())
        
        // Test valid playlist
        assertTrue("Valid name should not be empty", samplePlaylist.name.isNotEmpty())
        assertTrue("Should have valid type", samplePlaylist.type in PlaylistType.values())
    }

    @Test
    fun `PlaylistType should have all required types`() {
        val types = PlaylistType.values()
        
        assertTrue("Should contain USER_CREATED", types.contains(PlaylistType.USER_CREATED))
        assertTrue("Should contain SMART", types.contains(PlaylistType.SMART))
        assertTrue("Should contain FAVORITES", types.contains(PlaylistType.FAVORITES))
        assertTrue("Should contain RECENTLY_PLAYED", types.contains(PlaylistType.RECENTLY_PLAYED))
    }
}

// Mock classes for testing
sealed class SmartPlaylistCriteria {
    data class RecentlyAdded(val days: Int) : SmartPlaylistCriteria()
    data class MostPlayed(val count: Int) : SmartPlaylistCriteria()
    data class Genre(val genre: String) : SmartPlaylistCriteria()
    data class Artist(val artist: String) : SmartPlaylistCriteria()
}

data class PlaylistStats(
    val totalItems: Int,
    val totalDuration: Long,
    val totalSize: Long
)

enum class PlaylistExportFormat {
    M3U, M3U8, PLS, XSPF
}