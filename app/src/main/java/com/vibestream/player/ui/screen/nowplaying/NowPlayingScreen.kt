package com.vibestream.player.ui.screen.nowplaying

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vibestream.player.ui.subtitle.SubtitleControls
import com.vibestream.player.ui.subtitle.SubtitleDisplay
import com.vibestream.player.ui.audio.AudioEffectsPanel
import com.vibestream.player.domain.subtitle.SubtitleManager
import androidx.hilt.navigation.compose.hiltViewModel
import com.vibestream.player.ui.gesture.*
import com.vibestream.player.util.rememberSystemController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    onNavigateBack: () -> Unit,
    viewModel: NowPlayingViewModel = hiltViewModel(),
    isInPipMode: Boolean = false,
    onEnterPip: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val systemController = rememberSystemController()
    val gestureState = rememberGestureState()
    
    val playbackState by viewModel.playbackState.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val subtitleState by viewModel.subtitleState.collectAsState()
    val audioConfig by viewModel.audioConfig.collectAsState()
    
    var controlsVisible by remember { mutableStateOf(!isInPipMode) }
    
    // Get current subtitle for position
    val currentSubtitle = remember(currentPosition, subtitleState) {
        viewModel.getCurrentSubtitle(currentPosition)
    }
    
    // Handle back button
    BackHandler {
        onNavigateBack()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Video surface placeholder
        VideoSurface(
            modifier = Modifier.fillMaxSize(),
            mediaItem = playbackState.currentMediaItem
        )
        
        // Subtitle display overlay
        SubtitleDisplay(
            subtitle = currentSubtitle,
            config = subtitleState.config,
            modifier = Modifier.fillMaxSize()
        )
        
        // Gesture detector overlay
        PlayerGestureDetector(
            modifier = Modifier.fillMaxSize(),
            currentPosition = currentPosition,
            duration = duration,
            currentVolume = systemController.getCurrentVolume(),
            currentBrightness = systemController.getCurrentBrightness(),
            onGestureEvent = { event ->
                when (event) {
                    is GestureEvent.SingleTap -> {
                        controlsVisible = !controlsVisible
                    }
                    
                    is GestureEvent.DoubleTap -> {
                        val seekMs = if (event.isLeft) -10000L else 10000L
                        viewModel.seek(currentPosition + seekMs)
                        scope.launch {
                            gestureState.showDoubleTap(event.isLeft)
                        }
                    }
                    
                    is GestureEvent.Seek -> {
                        scope.launch {
                            gestureState.showSeek(
                                GestureUtils.formatSeekTime(event.previewPosition),
                                if (event.deltaMs > 0) "+${GestureUtils.formatSeekTime(event.deltaMs)}" 
                                else GestureUtils.formatSeekTime(event.deltaMs)
                            )
                        }
                    }
                    
                    is GestureEvent.Volume -> {
                        systemController.setVolume(event.currentLevel)
                        scope.launch {
                            gestureState.showVolume(GestureUtils.formatPercentage(event.currentLevel))
                        }
                    }
                    
                    is GestureEvent.Brightness -> {
                        systemController.setBrightness(event.currentLevel)
                        scope.launch {
                            gestureState.showBrightness(GestureUtils.formatPercentage(event.currentLevel))
                        }
                    }
                    
                    is GestureEvent.GestureStart -> {
                        controlsVisible = false
                    }
                    
                    else -> { /* Handle other gestures */ }
                }
            }
        ) {
            // Empty content - gestures are handled above
        }
        
        // Gesture feedback overlay
        GestureOverlay(
            modifier = Modifier.fillMaxSize(),
            gestureState = gestureState
        )
        
        // Audio effects controls (left side)
        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(16.dp)
        ) {
            AudioEffectsPanel(
                config = audioConfig,
                onConfigChange = { config ->
                    viewModel.updateAudioConfig(config)
                }
            )
        }
        
        // Subtitle controls (right side)
        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(16.dp)
        ) {
            SubtitleControls(
                subtitleState = subtitleState,
                onTrackSelected = { trackId ->
                    viewModel.selectSubtitleTrack(trackId)
                },
                onSyncAdjust = { offset ->
                    viewModel.setSubtitleSyncOffset(offset)
                },
                onConfigChange = { config ->
                    viewModel.updateSubtitleConfig(config)
                }
            )
        }
        
        // Playback controls overlay
        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            PlaybackControls(
                playbackState = playbackState,
                currentPosition = currentPosition,
                duration = duration,
                onPlayPause = { viewModel.togglePlayPause() },
                onSeek = { position -> viewModel.seek(position) },
                onPrevious = { viewModel.previous() },
                onNext = { viewModel.next() },
                modifier = Modifier.padding(16.dp)
            )
        }
        
        // Top controls (back button, title, options)
        AnimatedVisibility(
            visible = controlsVisible && !isInPipMode,
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            TopControls(
                title = playbackState.currentMediaItem?.title ?: "Unknown",
                onBack = onNavigateBack,
                onMoreOptions = { /* TODO: Show options menu */ },
                onPictureInPicture = if (playbackState.currentMediaItem?.type == com.vibestream.player.data.model.MediaType.VIDEO) {
                    onEnterPip
                } else null,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}