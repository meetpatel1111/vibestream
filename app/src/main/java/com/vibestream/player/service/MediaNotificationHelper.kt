package com.vibestream.player.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.vibestream.player.R
import com.vibestream.player.data.model.MediaItem
import com.vibestream.player.data.model.PlaybackState
import com.vibestream.player.ui.MainActivity
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for creating rich media notifications
 */
@Singleton
class MediaNotificationHelper @Inject constructor(
    private val context: Context,
    private val imageLoader: ImageLoader
) {
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = \"vibestream_playback\"
        
        // Notification actions
        const val ACTION_PLAY_PAUSE = \"com.vibestream.player.PLAY_PAUSE\"
        const val ACTION_PREVIOUS = \"com.vibestream.player.PREVIOUS\"
        const val ACTION_NEXT = \"com.vibestream.player.NEXT\"
        const val ACTION_STOP = \"com.vibestream.player.STOP\"
    }
    
    private var currentArtwork: Bitmap? = null
    private val notificationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Create media notification with current playback state
     */
    suspend fun createNotification(
        mediaItem: MediaItem?,
        playbackState: PlaybackState,
        sessionToken: androidx.media.session.MediaSessionCompat.Token?
    ): Notification = withContext(Dispatchers.Main) {\n        \n        val builder = NotificationCompat.Builder(context, CHANNEL_ID)\n            .setContentTitle(mediaItem?.title ?: \"VibeStream\")\n            .setContentText(formatSubtitle(mediaItem))\n            .setSubText(mediaItem?.album)\n            .setSmallIcon(R.drawable.ic_launcher_foreground)\n            .setContentIntent(createContentIntent())\n            .setDeleteIntent(createDeleteIntent())\n            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)\n            .setOngoing(playbackState.isPlaying)\n            .setShowWhen(false)\n            .setOnlyAlertOnce(true)\n            .setPriority(NotificationCompat.PRIORITY_LOW)\n        \n        // Set large icon (album art)\n        val artwork = getArtwork(mediaItem)\n        if (artwork != null) {\n            builder.setLargeIcon(artwork)\n        }\n        \n        // Configure media style\n        val mediaStyle = MediaStyle()\n            .setShowActionsInCompactView(0, 1, 2)\n        \n        sessionToken?.let { token ->\n            mediaStyle.setMediaSession(token)\n        }\n        \n        builder.setStyle(mediaStyle)\n        \n        // Add action buttons\n        addNotificationActions(builder, playbackState)\n        \n        // Set color scheme\n        builder.color = context.getColor(R.color.primary)\n        \n        return@withContext builder.build()\n    }\n    \n    /**\n     * Update notification with new state\n     */\n    fun updateNotification(\n        mediaItem: MediaItem?,\n        playbackState: PlaybackState,\n        sessionToken: androidx.media.session.MediaSessionCompat.Token?\n    ) {\n        notificationScope.launch {\n            val notification = createNotification(mediaItem, playbackState, sessionToken)\n            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager\n            notificationManager.notify(NOTIFICATION_ID, notification)\n        }\n    }\n    \n    /**\n     * Cancel notification\n     */\n    fun cancelNotification() {\n        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager\n        notificationManager.cancel(NOTIFICATION_ID)\n    }\n    \n    /**\n     * Get artwork for media item\n     */\n    private suspend fun getArtwork(mediaItem: MediaItem?): Bitmap? {\n        if (mediaItem == null) return null\n        \n        return try {\n            // First try embedded artwork from thumbnail path\n            mediaItem.thumbnailPath?.let { path ->\n                BitmapFactory.decodeFile(path)\n            } ?: run {\n                // Try to extract artwork from media file\n                val request = ImageRequest.Builder(context)\n                    .data(mediaItem.uri)\n                    .build()\n                \n                val result = imageLoader.execute(request)\n                if (result is SuccessResult) {\n                    result.drawable.toBitmap()\n                } else {\n                    null\n                }\n            }\n        } catch (e: Exception) {\n            null\n        }\n    }\n    \n    /**\n     * Add action buttons to notification\n     */\n    private fun addNotificationActions(\n        builder: NotificationCompat.Builder,\n        playbackState: PlaybackState\n    ) {\n        // Previous button\n        builder.addAction(\n            NotificationCompat.Action(\n                R.drawable.ic_skip_previous,\n                \"Previous\",\n                createActionIntent(ACTION_PREVIOUS)\n            )\n        )\n        \n        // Play/Pause button\n        val playPauseIcon = if (playbackState.isPlaying) {\n            R.drawable.ic_pause\n        } else {\n            R.drawable.ic_play_arrow\n        }\n        \n        val playPauseTitle = if (playbackState.isPlaying) \"Pause\" else \"Play\"\n        \n        builder.addAction(\n            NotificationCompat.Action(\n                playPauseIcon,\n                playPauseTitle,\n                createActionIntent(ACTION_PLAY_PAUSE)\n            )\n        )\n        \n        // Next button\n        builder.addAction(\n            NotificationCompat.Action(\n                R.drawable.ic_skip_next,\n                \"Next\",\n                createActionIntent(ACTION_NEXT)\n            )\n        )\n        \n        // Stop button (not shown in compact view)\n        builder.addAction(\n            NotificationCompat.Action(\n                R.drawable.ic_close,\n                \"Stop\",\n                createActionIntent(ACTION_STOP)\n            )\n        )\n    }\n    \n    /**\n     * Create pending intent for notification actions\n     */\n    private fun createActionIntent(action: String): PendingIntent {\n        val intent = Intent(action).apply {\n            setPackage(context.packageName)\n        }\n        \n        return PendingIntent.getBroadcast(\n            context,\n            action.hashCode(),\n            intent,\n            PendingIntent.FLAG_IMMUTABLE\n        )\n    }\n    \n    /**\n     * Create content intent for notification tap\n     */\n    private fun createContentIntent(): PendingIntent {\n        val intent = Intent(context, MainActivity::class.java).apply {\n            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP\n        }\n        \n        return PendingIntent.getActivity(\n            context,\n            0,\n            intent,\n            PendingIntent.FLAG_IMMUTABLE\n        )\n    }\n    \n    /**\n     * Create delete intent for notification dismissal\n     */\n    private fun createDeleteIntent(): PendingIntent {\n        return createActionIntent(ACTION_STOP)\n    }\n    \n    /**\n     * Format subtitle text for notification\n     */\n    private fun formatSubtitle(mediaItem: MediaItem?): String {\n        return when {\n            mediaItem?.artist != null && mediaItem.album != null -> \n                \"${mediaItem.artist} • ${mediaItem.album}\"\n            mediaItem?.artist != null -> mediaItem.artist\n            mediaItem?.album != null -> mediaItem.album\n            else -> \"Unknown Artist\"\n        }\n    }\n    \n    /**\n     * Release resources\n     */\n    fun release() {\n        notificationScope.cancel()\n        currentArtwork?.recycle()\n        currentArtwork = null\n    }\n}"}, {"original_text": "", "replace_all": false}]