package com.vibestream.player.ui.subtitle

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibestream.player.domain.subtitle.SubtitleAlignment
import com.vibestream.player.domain.subtitle.SubtitleConfig
import com.vibestream.player.domain.subtitle.SubtitleEntry
import com.vibestream.player.domain.subtitle.SubtitleState
import com.vibestream.player.domain.subtitle.SubtitleTrackInfo

/**
 * Subtitle display component
 */
@Composable
fun SubtitleDisplay(
    subtitle: SubtitleEntry?,
    config: SubtitleConfig,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = subtitle != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        subtitle?.let { entry ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = when (config.alignment) {
                    SubtitleAlignment.LEFT -> Alignment.BottomStart
                    SubtitleAlignment.CENTER -> Alignment.BottomCenter
                    SubtitleAlignment.RIGHT -> Alignment.BottomEnd
                }
            ) {
                SubtitleText(
                    text = entry.text,
                    config = config,
                    modifier = Modifier.padding(
                        horizontal = (LocalDensity.current.density * config.horizontalMargin * 100).dp,
                        vertical = (LocalDensity.current.density * config.verticalMargin * 100).dp
                    )
                )
            }
        }
    }
}

/**
 * Styled subtitle text component
 */
@Composable
private fun SubtitleText(
    text: String,
    config: SubtitleConfig,
    modifier: Modifier = Modifier
) {
    val backgroundColor = Color(config.backgroundColor)
    val textColor = Color(config.fontColor)
    val strokeColor = Color(config.strokeColor)
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // Shadow text (if enabled)
        if (config.shadowEnabled) {
            Text(
                text = text,
                fontSize = config.fontSize.sp,
                color = Color(config.shadowColor),
                textAlign = when (config.alignment) {
                    SubtitleAlignment.LEFT -> TextAlign.Start
                    SubtitleAlignment.CENTER -> TextAlign.Center
                    SubtitleAlignment.RIGHT -> TextAlign.End
                },
                modifier = Modifier.offset(
                    x = config.shadowOffset.dp,
                    y = config.shadowOffset.dp
                )
            )
        }
        
        // Main text with stroke
        Text(
            text = text,
            fontSize = config.fontSize.sp,
            color = textColor,
            textAlign = when (config.alignment) {
                SubtitleAlignment.LEFT -> TextAlign.Start
                SubtitleAlignment.CENTER -> TextAlign.Center
                SubtitleAlignment.RIGHT -> TextAlign.End
            },
            fontWeight = FontWeight.Medium,
            modifier = if (config.strokeWidth > 0) {
                Modifier.shadow(
                    elevation = config.strokeWidth.dp,
                    shape = RoundedCornerShape(2.dp)
                )
            } else Modifier
        )
    }
}

/**
 * Subtitle controls overlay
 */
@Composable
fun SubtitleControls(
    subtitleState: SubtitleState,
    onTrackSelected: (String?) -> Unit,
    onSyncAdjust: (Long) -> Unit,
    onConfigChange: (SubtitleConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var showControls by remember { mutableStateOf(false) }
    var showTrackSelection by remember { mutableStateOf(false) }
    var showSyncAdjustment by remember { mutableStateOf(false) }
    
    Column(modifier = modifier) {
        // Toggle button
        IconButton(
            onClick = { showControls = !showControls },
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.7f))
        ) {
            Icon(
                imageVector = Icons.Default.Subtitles,
                contentDescription = "Subtitle Controls",
                tint = if (subtitleState.isEnabled) MaterialTheme.colorScheme.primary else Color.White
            )
        }
        
        // Expanded controls
        AnimatedVisibility(
            visible = showControls,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Track selection
                OutlinedButton(
                    onClick = { showTrackSelection = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = subtitleState.currentTrack?.title ?: "No Subtitle",
                        maxLines = 1
                    )
                }
                
                // Sync adjustment
                if (subtitleState.isEnabled) {
                    OutlinedButton(
                        onClick = { showSyncAdjustment = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sync: ${subtitleState.syncOffset}ms"
                        )
                    }
                }
                
                // Font size adjustment
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TextDecrease,
                        contentDescription = "Decrease font size",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Slider(
                        value = subtitleState.config.fontSize,
                        onValueChange = { size ->
                            onConfigChange(subtitleState.config.copy(fontSize = size))
                        },
                        valueRange = 12f..32f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    
                    Icon(
                        imageVector = Icons.Default.TextIncrease,
                        contentDescription = "Increase font size",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
    
    // Track selection dialog
    if (showTrackSelection) {
        SubtitleTrackSelectionDialog(
            tracks = subtitleState.availableTracks,
            currentTrackId = subtitleState.currentTrack?.id,
            onTrackSelected = { trackId ->
                onTrackSelected(trackId)
                showTrackSelection = false
            },
            onDismiss = { showTrackSelection = false }
        )
    }
    
    // Sync adjustment dialog
    if (showSyncAdjustment) {
        SubtitleSyncDialog(
            currentOffset = subtitleState.syncOffset,
            onOffsetChanged = onSyncAdjust,
            onDismiss = { showSyncAdjustment = false }
        )
    }
}

/**
 * Subtitle track selection dialog
 */
@Composable
private fun SubtitleTrackSelectionDialog(
    tracks: List<SubtitleTrackInfo>,
    currentTrackId: String?,
    onTrackSelected: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Subtitle Track") },
        text = {
            LazyColumn {
                // No subtitle option
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentTrackId == null,
                            onClick = { onTrackSelected(null) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("No Subtitle")
                    }
                }
                
                // Available tracks
                items(tracks.size) { index ->
                    val track = tracks[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentTrackId == track.id,
                            onClick = { onTrackSelected(track.id) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(track.title)
                            if (track.language != null) {
                                Text(
                                    text = track.language,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        if (track.isExternal) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "External",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
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
 * Subtitle sync adjustment dialog
 */
@Composable
private fun SubtitleSyncDialog(
    currentOffset: Long,
    onOffsetChanged: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var tempOffset by remember { mutableStateOf(currentOffset) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Subtitle Sync") },
        text = {
            Column {
                Text(
                    text = "Adjust subtitle timing",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { 
                            tempOffset -= 100
                            onOffsetChanged(tempOffset)
                        }
                    ) {
                        Text("-100ms")
                    }
                    
                    Button(
                        onClick = { 
                            tempOffset -= 25
                            onOffsetChanged(tempOffset)
                        }
                    ) {
                        Text("-25ms")
                    }
                    
                    Text(
                        text = "${tempOffset}ms",
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Button(
                        onClick = { 
                            tempOffset += 25
                            onOffsetChanged(tempOffset)
                        }
                    ) {
                        Text("+25ms")
                    }
                    
                    Button(
                        onClick = { 
                            tempOffset += 100
                            onOffsetChanged(tempOffset)
                        }
                    ) {
                        Text("+100ms")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { 
                        tempOffset = 0
                        onOffsetChanged(0)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}