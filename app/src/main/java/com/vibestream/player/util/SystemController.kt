package com.vibestream.player.util

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Controller for system brightness and volume
 */
class SystemController(private val context: Context) {
    
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    /**
     * Get current volume level (0.0 to 1.0)
     */
    fun getCurrentVolume(): Float {
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return if (maxVolume > 0) currentVolume.toFloat() / maxVolume else 0f
    }
    
    /**
     * Set volume level (0.0 to 1.0)
     */
    fun setVolume(volume: Float) {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val targetVolume = (volume * maxVolume).toInt().coerceIn(0, maxVolume)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
    }
    
    /**
     * Adjust volume by delta (-1.0 to 1.0)
     */
    fun adjustVolume(delta: Float) {
        val currentVolume = getCurrentVolume()
        val newVolume = (currentVolume + delta).coerceIn(0f, 1f)
        setVolume(newVolume)
    }
    
    /**
     * Get current screen brightness (0.0 to 1.0)
     */
    fun getCurrentBrightness(): Float {
        return if (context is Activity) {
            val window = context.window
            val layoutParams = window.attributes
            if (layoutParams.screenBrightness == WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE) {
                // Use system brightness
                try {
                    val systemBrightness = Settings.System.getInt(
                        context.contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS
                    )
                    systemBrightness / 255f
                } catch (e: Settings.SettingNotFoundException) {
                    0.5f // Default to 50%
                }
            } else {
                layoutParams.screenBrightness
            }
        } else {
            0.5f // Default value when not in activity
        }
    }
    
    /**
     * Set screen brightness (0.0 to 1.0)
     */
    fun setBrightness(brightness: Float) {
        if (context is Activity) {
            val window = context.window
            val layoutParams = window.attributes
            layoutParams.screenBrightness = brightness.coerceIn(0.01f, 1.0f)
            window.attributes = layoutParams
        }
    }
    
    /**
     * Adjust brightness by delta (-1.0 to 1.0)
     */
    fun adjustBrightness(delta: Float) {
        val currentBrightness = getCurrentBrightness()
        val newBrightness = (currentBrightness + delta).coerceIn(0.01f, 1f)
        setBrightness(newBrightness)
    }
    
    /**
     * Reset brightness to system default
     */
    fun resetBrightness() {
        if (context is Activity) {
            val window = context.window
            val layoutParams = window.attributes
            layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            window.attributes = layoutParams
        }
    }
    
    /**
     * Check if device supports volume control
     */
    fun canControlVolume(): Boolean {
        return audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) > 0
    }
    
    /**
     * Check if device supports brightness control
     */
    fun canControlBrightness(): Boolean {
        return context is Activity
    }
}

/**
 * Composable to remember system controller
 */
@Composable
fun rememberSystemController(): SystemController {
    val context = LocalContext.current
    return remember(context) { SystemController(context) }
}