package com.vibestream.player.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.vibestream.player.domain.player.PlaybackController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Broadcast receiver for handling media button events and notification actions
 */
@AndroidEntryPoint
class MediaButtonReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var playbackController: PlaybackController
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            MediaNotificationHelper.ACTION_PLAY_PAUSE -> {
                val currentState = playbackController.playbackState.value
                if (currentState.isPlaying) {
                    playbackController.pause()
                } else {
                    playbackController.play()
                }
            }
            
            MediaNotificationHelper.ACTION_PREVIOUS -> {
                serviceScope.launch {
                    playbackController.previous()
                }
            }
            
            MediaNotificationHelper.ACTION_NEXT -> {
                serviceScope.launch {
                    playbackController.next()
                }
            }
            
            MediaNotificationHelper.ACTION_STOP -> {
                playbackController.stop()
                // Stop the service
                context.stopService(Intent(context, MediaPlaybackService::class.java))
            }
            
            Intent.ACTION_MEDIA_BUTTON -> {
                // Handle hardware media button events
                // This would typically be handled by MediaSession, but we can add fallback here
            }
        }
    }
    
    companion object {
        /**
         * Create intent filter for media actions
         */
        fun createIntentFilter(): IntentFilter {
            return IntentFilter().apply {
                addAction(MediaNotificationHelper.ACTION_PLAY_PAUSE)
                addAction(MediaNotificationHelper.ACTION_PREVIOUS)
                addAction(MediaNotificationHelper.ACTION_NEXT)
                addAction(MediaNotificationHelper.ACTION_STOP)
                addAction(Intent.ACTION_MEDIA_BUTTON)
            }
        }
    }
}