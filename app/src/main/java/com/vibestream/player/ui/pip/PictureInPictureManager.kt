package com.vibestream.player.ui.pip

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.util.Rational
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.vibestream.player.data.model.MediaItem
import com.vibestream.player.data.model.MediaType

/**
 * Picture-in-Picture manager for video playback
 */
class PictureInPictureManager(
    private val activity: Activity
) {
    
    /**
     * Check if PiP is supported on this device
     */
    fun isPipSupported(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        } else {
            false
        }
    }
    
    /**
     * Enter Picture-in-Picture mode
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun enterPictureInPictureMode(
        mediaItem: MediaItem?,
        sourceRect: Rect? = null
    ): Boolean {
        if (!isPipSupported()) return false
        
        val aspectRatio = mediaItem?.let { item ->
            if (item.type == MediaType.VIDEO && item.width != null && item.height != null) {
                Rational(item.width, item.height)
            } else {
                Rational(16, 9) // Default aspect ratio
            }
        } ?: Rational(16, 9)
        
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(aspectRatio)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setAutoEnterEnabled(true)
                    setSeamlessResizeEnabled(true)
                }
                sourceRect?.let { setSourceRectHint(it) }
            }
            .build()
        
        return try {
            activity.enterPictureInPictureMode(params)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Update PiP parameters (e.g., when aspect ratio changes)
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun updatePictureInPictureParams(
        mediaItem: MediaItem?,
        sourceRect: Rect? = null
    ) {
        if (!isPipSupported()) return
        
        val aspectRatio = mediaItem?.let { item ->
            if (item.type == MediaType.VIDEO && item.width != null && item.height != null) {
                Rational(item.width, item.height)
            } else {
                Rational(16, 9)
            }
        } ?: Rational(16, 9)
        
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(aspectRatio)
            .apply {
                sourceRect?.let { setSourceRectHint(it) }
            }
            .build()
        
        try {
            activity.setPictureInPictureParams(params)
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    /**
     * Check if currently in PiP mode
     */
    fun isInPictureInPictureMode(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.isInPictureInPictureMode
        } else {
            false
        }
    }
}

/**
 * Composable to remember PiP manager
 */
@Composable
fun rememberPictureInPictureManager(): PictureInPictureManager? {
    val context = LocalContext.current
    return remember(context) {
        (context as? Activity)?.let { PictureInPictureManager(it) }
    }
}

/**
 * Composable effect to handle PiP state changes
 */
@Composable
fun PictureInPictureEffect(
    mediaItem: MediaItem?,
    isPlaying: Boolean,
    onPipModeChanged: (Boolean) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val activity = context as? Activity
    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        activity?.let { act ->
                            val pipManager = PictureInPictureManager(act)
                            if (pipManager.isInPictureInPictureMode()) {
                                onPipModeChanged(true)
                            }
                        }
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        activity?.let { act ->
                            val pipManager = PictureInPictureManager(act)
                            if (!pipManager.isInPictureInPictureMode()) {
                                onPipModeChanged(false)
                            }
                        }
                    }
                }
                else -> {}
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

/**
 * Auto-enter PiP when backgrounded during video playback
 */
@Composable
fun AutoPictureInPictureEffect(
    mediaItem: MediaItem?,
    isPlaying: Boolean,
    enabled: Boolean = true
) {
    val pipManager = rememberPictureInPictureManager()
    val lifecycleOwner = LocalLifecycleOwner.current
    
    DisposableEffect(mediaItem, isPlaying, enabled, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE && 
                enabled && 
                isPlaying && 
                mediaItem?.type == MediaType.VIDEO &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                
                pipManager?.enterPictureInPictureMode(mediaItem)
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}