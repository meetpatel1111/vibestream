package com.vibestream.player.ui.playlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vibestream.player.data.model.MediaItem
import com.vibestream.player.data.model.MediaType
import com.vibestream.player.data.model.Playlist

/**
 * Playlist list screen showing all playlists
 */
@Composable
fun PlaylistsScreen(
    playlists: List<Playlist>,
    onPlaylistClick: (Playlist) -> Unit,
    onCreatePlaylist: () -> Unit,
    onDeletePlaylist: (Playlist) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Header with create button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Playlists",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            FloatingActionButton(
                onClick = onCreatePlaylist,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create Playlist"
                )
            }
        }
        
        // Playlist grid
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(playlists) { playlist ->
                PlaylistCard(
                    playlist = playlist,
                    onClick = { onPlaylistClick(playlist) },
                    onDelete = if (!playlist.isSmartPlaylist) {
                        { onDeletePlaylist(playlist) }
                    } else null
                )
            }
        }
    }
}

/**
 * Individual playlist card
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showContextMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showContextMenu = true }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Playlist icon
            Icon(
                imageVector = if (playlist.isSmartPlaylist) {
                    Icons.Default.AutoAwesome
                } else {
                    Icons.Default.PlaylistPlay
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Playlist info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = if (playlist.isSmartPlaylist) {
                        "Smart Playlist"
                    } else {
                        "Custom Playlist"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // More options button
            IconButton(
                onClick = { showContextMenu = true }
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options"
                )
            }
        }
    }
    
    // Context menu
    if (showContextMenu) {
        PlaylistContextMenu(
            playlist = playlist,
            onDismiss = { showContextMenu = false },
            onDelete = onDelete
        )
    }
}

/**
 * Playlist context menu
 */
@Composable
fun PlaylistContextMenu(
    playlist: Playlist,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(playlist.name) },
        text = {
            Column {
                if (!playlist.isSmartPlaylist) {
                    TextButton(
                        onClick = {
                            // TODO: Edit playlist
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit")
                    }
                }
                
                TextButton(
                    onClick = {
                        // TODO: Export playlist
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export")
                }
                
                if (onDelete != null) {
                    TextButton(
                        onClick = {
                            onDelete()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * Playlist detail screen showing playlist contents
 */
@Composable
fun PlaylistDetailScreen(
    playlist: Playlist,
    items: List<MediaItem>,
    onPlayItem: (MediaItem, Int) -> Unit,
    onRemoveItem: (MediaItem) -> Unit,
    onReorderItems: (Int, Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Header
        PlaylistHeader(
            playlist = playlist,
            itemCount = items.size,
            onBack = onBack,
            onPlayAll = {
                if (items.isNotEmpty()) {
                    onPlayItem(items.first(), 0)
                }
            },
            onShuffle = {
                if (items.isNotEmpty()) {
                    val randomIndex = items.indices.random()
                    onPlayItem(items[randomIndex], randomIndex)
                }
            }
        )
        
        // Items list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(items) { index, item ->
                PlaylistItemCard(
                    item = item,
                    index = index,
                    onClick = { onPlayItem(item, index) },
                    onRemove = if (!playlist.isSmartPlaylist) {
                        { onRemoveItem(item) }
                    } else null
                )
            }
        }
    }
}

/**
 * Playlist header with controls
 */
@Composable
fun PlaylistHeader(
    playlist: Playlist,
    itemCount: Int,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        // Top row with back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            if (!playlist.isSmartPlaylist) {
                IconButton(onClick = { /* TODO: Add items */ }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add items"
                    )
                }
            }
        }
        
        // Playlist info
        Column {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "$itemCount ${if (itemCount == 1) "item" else "items"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (playlist.isSmartPlaylist) {
                Text(
                    text = "Smart Playlist",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onPlayAll,
                enabled = itemCount > 0
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Play All")
            }
            
            OutlinedButton(
                onClick = onShuffle,
                enabled = itemCount > 0
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Shuffle")
            }
        }
    }
}

/**
 * Individual playlist item card
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistItemCard(
    item: MediaItem,
    index: Int,
    onClick: () -> Unit,
    onRemove: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showContextMenu by remember { mutableStateOf(false) }
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showContextMenu = true }
            ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Track number
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(32.dp)
            )
            
            // Media type icon
            Icon(
                imageVector = if (item.type == MediaType.VIDEO) {
                    Icons.Default.Movie
                } else {
                    Icons.Default.MusicNote
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Item info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (item.artist != null) {
                    Text(
                        text = item.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Duration
            Text(
                text = formatDuration(item.duration),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // More options
            if (onRemove != null) {
                IconButton(
                    onClick = { showContextMenu = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
    
    // Context menu
    if (showContextMenu && onRemove != null) {
        AlertDialog(
            onDismissRequest = { showContextMenu = false },
            title = { Text("Remove from playlist") },
            text = { Text("Are you sure you want to remove \"${item.title}\" from this playlist?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemove()
                        showContextMenu = false
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showContextMenu = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Create playlist dialog
 */
@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var playlistName by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Playlist") },
        text = {
            OutlinedTextField(
                value = playlistName,
                onValueChange = { playlistName = it },
                label = { Text("Playlist Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (playlistName.isNotBlank()) {
                        onConfirm(playlistName.trim())
                    }
                },
                enabled = playlistName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Utility functions

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}