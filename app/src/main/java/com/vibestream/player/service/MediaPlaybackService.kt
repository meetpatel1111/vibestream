package com.vibestream.player.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.*
import coil.ImageLoader
import coil.request.ImageRequest
import com.vibestream.player.R
import com.vibestream.player.domain.player.PlaybackController
import com.vibestream.player.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * Media playback service for background audio and notification controls
 */
@AndroidEntryPoint
class MediaPlaybackService : MediaSessionService() {
    
    @Inject
    lateinit var playbackController: PlaybackController
    
    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private var serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "vibestream_playback"
        private const val CHANNEL_NAME = "VibeStream Playback"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        createNotificationChannel()
        
        // Initialize ExoPlayer for the service
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        
        // Create session activity pending intent
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        
        // Create media session
        mediaSession = MediaSession.Builder(this, player!!)
            .setSessionActivity(sessionActivityPendingIntent)
            .setCallback(MediaSessionCallback())
            .build()
        
        // Start as foreground service
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Observe playback state changes
        observePlaybackChanges()
    }
    
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }
    
    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession?.run {
            player?.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
    
    /**
     * Create notification channel for Android O+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Media playback controls"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Create media notification
     */
    private fun createNotification(): Notification {
        val currentMedia = playbackController.getCurrentItem()
        val playbackState = playbackController.playbackState.value
        
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(currentMedia?.title ?: "VibeStream")
            .setContentText(currentMedia?.artist ?: "Unknown Artist")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(playbackState.isPlaying)
            .setShowWhen(false)
        
        // Add media style
        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
            .setMediaSession(mediaSession?.sessionCompatToken)
            .setShowActionsInCompactView(0, 1, 2)
        
        builder.setStyle(mediaStyle)
        
        // Add action buttons
        builder.addAction(
            NotificationCompat.Action(
                R.drawable.ic_skip_previous,
                "Previous",
                createActionPendingIntent("PREVIOUS")
            )
        )
        
        val playPauseIcon = if (playbackState.isPlaying) {
            R.drawable.ic_pause
        } else {
            R.drawable.ic_play_arrow
        }
        
        builder.addAction(
            NotificationCompat.Action(
                playPauseIcon,
                if (playbackState.isPlaying) "Pause" else "Play",
                createActionPendingIntent("PLAY_PAUSE")
            )
        )
        
        builder.addAction(
            NotificationCompat.Action(
                R.drawable.ic_skip_next,
                "Next",
                createActionPendingIntent("NEXT")
            )
        )
        
        return builder.build()
    }
    
    /**
     * Create pending intent for notification actions
     */
    private fun createActionPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, MediaPlaybackService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    /**
     * Observe playback state changes and update notification
     */
    private fun observePlaybackChanges() {
        serviceScope.launch {
            playbackController.playbackState.collect { state ->
                val notification = createNotification()
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        
        // Handle notification actions
        when (intent?.action) {
            "PLAY_PAUSE" -> {
                if (playbackController.playbackState.value.isPlaying) {
                    playbackController.pause()
                } else {
                    playbackController.play()
                }
            }
            "NEXT" -> {
                serviceScope.launch {
                    playbackController.next()
                }
            }
            "PREVIOUS" -> {
                serviceScope.launch {
                    playbackController.previous()
                }
            }
        }
        
        return START_STICKY
    }
    
    /**
     * Callback for handling media session events
     */
    private inner class MediaSessionCallback : MediaSession.Callback {
        
        override fun onPlay(session: MediaSession, controller: MediaSession.ControllerInfo): ListenableFuture<SessionResult> {
            playbackController.play()
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
        
        override fun onPause(session: MediaSession, controller: MediaSession.ControllerInfo): ListenableFuture<SessionResult> {
            playbackController.pause()
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
        
        override fun onStop(session: MediaSession, controller: MediaSession.ControllerInfo): ListenableFuture<SessionResult> {
            playbackController.stop()
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
        
        override fun onSeekTo(session: MediaSession, controller: MediaSession.ControllerInfo, positionMs: Long): ListenableFuture<SessionResult> {
            playbackController.seek(positionMs)
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
        
        override fun onSkipToNext(session: MediaSession, controller: MediaSession.ControllerInfo): ListenableFuture<SessionResult> {
            serviceScope.launch {
                playbackController.next()
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
        
        override fun onSkipToPrevious(session: MediaSession, controller: MediaSession.ControllerInfo): ListenableFuture<SessionResult> {
            serviceScope.launch {
                playbackController.previous()
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }
}