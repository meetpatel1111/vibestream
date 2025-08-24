package com.vibestream.player.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vibestream.player.R

@Composable
fun HomeScreen(
    onNavigateToNowPlaying: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "Welcome to VibeStream",
                style = MaterialTheme.typography.titleMedium
            )
            
            Text(
                text = "Your cross-platform media player",
                style = MaterialTheme.typography.bodyLarge
            )
            
            // TODO: Add Continue Watching section
            // TODO: Add Recently Added section
            // TODO: Add Quick access buttons
            // TODO: Add media scanning status
        }
    }
}