package com.vibestream.player.ui.screen.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vibestream.player.data.model.MediaType

@Composable
fun LibraryScreen(
    mediaType: MediaType,
    onNavigateToNowPlaying: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = when (mediaType) {
                MediaType.VIDEO -> "Video Library"
                MediaType.AUDIO -> "Music Library"
            },
            style = MaterialTheme.typography.headlineMedium
        )
        
        // TODO: Implement media grid/list
        // TODO: Add search functionality
        // TODO: Add filter options
        // TODO: Add sorting options
    }
}