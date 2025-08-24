package com.vibestream.player.ui.gesture

import android.content.Context
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.sign

/**
 * Gesture configuration for MX-style player controls
 */
data class GestureConfig(
    val seekSensitivity: Float = 90000f, // milliseconds per screen width
    val volumeSensitivity: Float = 0.3f, // volume change per screen height
    val brightnessSensitivity: Float = 0.3f, // brightness change per screen height
    val doubleTapSeekMs: Long = 10000L, // 10 seconds
    val longPressThresholdMs: Long = 500L,
    val gestureThreshold: Float = 20f, // minimum distance to trigger gesture
    val leftZoneRatio: Float = 0.4f, // left 40% for brightness
    val rightZoneRatio: Float = 0.4f, // right 40% for volume
    val enableHapticFeedback: Boolean = true
)

/**
 * Gesture event types
 */
sealed class GestureEvent {
    object SingleTap : GestureEvent()
    data class DoubleTap(val isLeft: Boolean) : GestureEvent()
    data class LongPress(val position: Offset) : GestureEvent()
    data class TwoFingerTap(val position: Offset) : GestureEvent()
    data class Seek(val deltaMs: Long, val previewPosition: Long) : GestureEvent()
    data class Volume(val delta: Float, val currentLevel: Float) : GestureEvent()
    data class Brightness(val delta: Float, val currentLevel: Float) : GestureEvent()
    data class Zoom(val scale: Float, val center: Offset) : GestureEvent()
    data class Pan(val offset: Offset) : GestureEvent()
    object GestureStart : GestureEvent()
    object GestureEnd : GestureEvent()
}

/**
 * Gesture state for tracking ongoing gestures
 */
private enum class GestureType {
    NONE, SEEK, VOLUME, BRIGHTNESS, ZOOM, PAN
}

/**
 * Player gesture detector composable with MX-style controls
 */
@Composable
fun PlayerGestureDetector(
    modifier: Modifier = Modifier,
    config: GestureConfig = GestureConfig(),
    currentPosition: Long = 0L,
    duration: Long = 0L,
    currentVolume: Float = 0.5f,
    currentBrightness: Float = 0.5f,
    onGestureEvent: (GestureEvent) -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    
    var gestureType by remember { mutableStateOf(GestureType.NONE) }
    var gestureStartPosition by remember { mutableStateOf(Offset.Zero) }
    var accumulatedDelta by remember { mutableStateOf(Offset.Zero) }
    var isDoubleTapWindow by remember { mutableStateOf(false) }
    var lastTapTime by remember { mutableStateOf(0L) }
    var lastTapPosition by remember { mutableStateOf(Offset.Zero) }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        val currentTime = System.currentTimeMillis()
                        val timeDiff = currentTime - lastTapTime
                        val positionDiff = (offset - lastTapPosition).getDistance()
                        
                        if (timeDiff < 300L && positionDiff < 100f) {
                            // Double tap detected
                            val isLeft = offset.x < size.width * 0.5f
                            onGestureEvent(GestureEvent.DoubleTap(isLeft))
                            isDoubleTapWindow = false
                        } else {
                            // Single tap with delay to check for double tap
                            lastTapTime = currentTime
                            lastTapPosition = offset
                            isDoubleTapWindow = true
                            
                            // Delay to wait for potential second tap
                            kotlinx.coroutines.launch {
                                delay(300L)
                                if (isDoubleTapWindow) {
                                    onGestureEvent(GestureEvent.SingleTap)
                                    isDoubleTapWindow = false
                                }
                            }
                        }
                    },
                    onLongPress = { offset ->
                        onGestureEvent(GestureEvent.LongPress(offset))
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        gestureStartPosition = offset
                        accumulatedDelta = Offset.Zero
                        onGestureEvent(GestureEvent.GestureStart)
                    },
                    onDragEnd = {
                        gestureType = GestureType.NONE
                        accumulatedDelta = Offset.Zero
                        onGestureEvent(GestureEvent.GestureEnd)
                    },
                    onDrag = { change ->
                        val totalDelta = accumulatedDelta + change
                        accumulatedDelta = totalDelta
                        
                        // Determine gesture type based on initial movement
                        if (gestureType == GestureType.NONE) {
                            val absDeltaX = abs(totalDelta.x)
                            val absDeltaY = abs(totalDelta.y)
                            
                            if (absDeltaX > config.gestureThreshold || absDeltaY > config.gestureThreshold) {
                                gestureType = when {
                                    absDeltaX > absDeltaY -> GestureType.SEEK
                                    gestureStartPosition.x < size.width * config.leftZoneRatio -> GestureType.BRIGHTNESS
                                    gestureStartPosition.x > size.width * (1f - config.rightZoneRatio) -> GestureType.VOLUME
                                    else -> GestureType.SEEK // Center area defaults to seek
                                }
                            }
                        }
                        
                        // Handle gesture based on type
                        when (gestureType) {
                            GestureType.SEEK -> {
                                val seekDelta = (totalDelta.x / size.width) * config.seekSensitivity
                                val newPosition = (currentPosition + seekDelta.toLong())
                                    .coerceIn(0L, duration)
                                onGestureEvent(GestureEvent.Seek(seekDelta.toLong(), newPosition))
                            }
                            
                            GestureType.VOLUME -> {
                                val volumeDelta = -(totalDelta.y / size.height) * config.volumeSensitivity
                                val newVolume = (currentVolume + volumeDelta).coerceIn(0f, 1f)
                                onGestureEvent(GestureEvent.Volume(volumeDelta, newVolume))
                            }
                            
                            GestureType.BRIGHTNESS -> {
                                val brightnessDelta = -(totalDelta.y / size.height) * config.brightnessSensitivity
                                val newBrightness = (currentBrightness + brightnessDelta).coerceIn(0f, 1f)
                                onGestureEvent(GestureEvent.Brightness(brightnessDelta, newBrightness))
                            }
                            
                            else -> { /* No gesture yet */ }
                        }
                    }
                )
            }
    ) {
        content()
    }
}

/**
 * Utility functions for gesture calculations
 */
object GestureUtils {
    
    /**
     * Format time for seek gesture display
     */
    fun formatSeekTime(positionMs: Long): String {
        val totalSeconds = positionMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
    
    /**
     * Format percentage for volume/brightness display
     */
    fun formatPercentage(value: Float): String {
        return "${(value * 100).toInt()}%"
    }
    
    /**
     * Calculate seek delta in a more user-friendly way
     */
    fun calculateSeekDelta(deltaX: Float, screenWidth: Float, maxSeekMs: Long = 600000L): Long {
        val ratio = (deltaX / screenWidth).coerceIn(-1f, 1f)
        return (ratio * maxSeekMs).toLong()
    }
    
    /**
     * Apply haptic feedback for gestures
     */
    fun performHapticFeedback(context: Context, type: GestureEvent) {
        // TODO: Implement haptic feedback based on gesture type
        // Light feedback for volume/brightness
        // Medium feedback for seek
        // Strong feedback for double tap
    }
}