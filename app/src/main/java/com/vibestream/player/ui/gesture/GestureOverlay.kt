package com.vibestream.player.ui.gesture

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Gesture feedback overlay that shows visual indicators for user gestures
 */
@Composable
fun GestureOverlay(
    modifier: Modifier = Modifier,
    gestureState: GestureState
) {
    Box(modifier = modifier.fillMaxSize()) {
        
        // Volume indicator (right side)
        AnimatedVisibility(
            visible = gestureState.volumeVisible,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            GestureIndicator(
                icon = Icons.Default.VolumeUp,
                value = gestureState.volumeText,
                modifier = Modifier.padding(end = 32.dp)
            )
        }
        
        // Brightness indicator (left side)
        AnimatedVisibility(
            visible = gestureState.brightnessVisible,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            GestureIndicator(
                icon = Icons.Default.Brightness6,
                value = gestureState.brightnessText,
                modifier = Modifier.padding(start = 32.dp)
            )
        }
        
        // Seek indicator (center)
        AnimatedVisibility(
            visible = gestureState.seekVisible,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            SeekIndicator(
                time = gestureState.seekTime,
                delta = gestureState.seekDelta
            )
        }
        
        // Double tap indicators
        AnimatedVisibility(
            visible = gestureState.doubleTapVisible,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(500)),
            modifier = Modifier.align(
                if (gestureState.doubleTapLeft) Alignment.CenterStart else Alignment.CenterEnd
            )
        ) {
            DoubleTapIndicator(
                isLeft = gestureState.doubleTapLeft,
                modifier = Modifier.padding(horizontal = 64.dp)
            )
        }
    }
}

/**
 * Generic gesture indicator for volume and brightness
 */
@Composable
private fun GestureIndicator(
    icon: ImageVector,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Seek gesture indicator with time display
 */
@Composable
private fun SeekIndicator(
    time: String,
    delta: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.8f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = time,
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        if (delta.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = delta,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Double tap indicator with directional arrow
 */
@Composable
private fun DoubleTapIndicator(
    isLeft: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isLeft) {
            Icon(
                imageVector = Icons.Default.FastRewind,
                contentDescription = "Rewind",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "10s",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Text(
                text = "10s",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.FastForward,
                contentDescription = "Fast Forward",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * State holder for gesture overlay
 */
@Stable
class GestureState {
    var volumeVisible by mutableStateOf(false)
    var volumeText by mutableStateOf("")
    
    var brightnessVisible by mutableStateOf(false)
    var brightnessText by mutableStateOf("")
    
    var seekVisible by mutableStateOf(false)
    var seekTime by mutableStateOf("")
    var seekDelta by mutableStateOf("")
    
    var doubleTapVisible by mutableStateOf(false)
    var doubleTapLeft by mutableStateOf(false)
    
    /**
     * Show volume indicator
     */
    suspend fun showVolume(percentage: String) {
        volumeText = percentage
        volumeVisible = true
        delay(1500)
        volumeVisible = false
    }
    
    /**
     * Show brightness indicator
     */
    suspend fun showBrightness(percentage: String) {
        brightnessText = percentage
        brightnessVisible = true
        delay(1500)
        brightnessVisible = false
    }
    
    /**
     * Show seek indicator
     */
    suspend fun showSeek(time: String, delta: String = "") {
        seekTime = time
        seekDelta = delta
        seekVisible = true
        delay(1000)
        seekVisible = false
    }
    
    /**
     * Show double tap indicator
     */
    suspend fun showDoubleTap(isLeft: Boolean) {
        doubleTapLeft = isLeft
        doubleTapVisible = true
        delay(800)
        doubleTapVisible = false
    }
    
    /**
     * Hide all indicators
     */
    fun hideAll() {
        volumeVisible = false
        brightnessVisible = false
        seekVisible = false
        doubleTapVisible = false
    }
}

/**
 * Remember gesture state
 */
@Composable
fun rememberGestureState(): GestureState {
    return remember { GestureState() }
}