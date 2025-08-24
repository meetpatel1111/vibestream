package com.vibestream.player.ui

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dagger.hilt.android.AndroidEntryPoint
import com.vibestream.player.ui.theme.VibeStreamTheme
import com.vibestream.player.ui.pip.PictureInPictureManager
import com.vibestream.player.domain.player.PlaybackController
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var playbackController: PlaybackController
    
    private var isInPipMode = false
    private lateinit var pipManager: PictureInPictureManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize PiP manager
        pipManager = PictureInPictureManager(this)
        
        // Enable edge-to-edge display
        enableEdgeToEdge()
        
        // Configure system bars for immersive experience
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = 
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        
        setContent {
            VibeStreamTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VibeStreamApp(
                        isInPipMode = isInPipMode,
                        onEnterPip = { enterPictureInPictureMode() }
                    )
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Handle any intent that launched the app (e.g., opening a media file)
        handleIntent()
    }
    
    private fun handleIntent() {
        intent?.let { intent ->
            when (intent.action) {
                "android.intent.action.VIEW" -> {
                    val uri = intent.data
                    val mimeType = intent.type
                    // TODO: Handle media file opening
                }
            }
        }
    }
    
    /**
     * Enter Picture-in-Picture mode
     */
    private fun enterPictureInPictureMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && pipManager.isPipSupported()) {
            val currentMedia = playbackController.getCurrentItem()
            pipManager.enterPictureInPictureMode(currentMedia)
        }
    }
    
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
        
        if (isInPictureInPictureMode) {
            // Hide UI elements that shouldn't be visible in PiP
            enterImmersiveMode()
        } else {
            // Restore UI when exiting PiP
            exitImmersiveMode()
        }
    }
    
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        
        // Auto-enter PiP when user navigates away during video playback
        val currentState = playbackController.playbackState.value
        val currentMedia = playbackController.getCurrentItem()
        
        if (currentState.isPlaying && 
            currentMedia?.type == com.vibestream.player.data.model.MediaType.VIDEO &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            pipManager.enterPictureInPictureMode(currentMedia)
        }
    }
}