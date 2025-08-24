package com.vibestream.player.data.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player as ExoPlayer
import androidx.media3.exoplayer.ExoPlayer as ExoPlayerImpl
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.source.dash.DashMediaSource
import androidx.media3.exoplayer.source.hls.HlsMediaSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import com.vibestream.player.data.model.MediaItem
import com.vibestream.player.data.model.PlaybackState
import com.vibestream.player.data.model.PlayerEvent
import com.vibestream.player.domain.player.AudioFilters
import com.vibestream.player.domain.player.Player
import com.vibestream.player.domain.player.RepeatMode
import com.vibestream.player.domain.player.VideoFilters
import com.vibestream.player.domain.audio.AudioEffectProcessor
import com.vibestream.player.data.audio.AndroidAudioEffectProcessor
import com.vibestream.player.util.performance.PerformanceManager
import com.vibestream.player.util.performance.HardwareAccelerationManager
import com.vibestream.player.util.performance.MemoryPoolManager
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.mediacodec.MediaCodecVideoRenderer
import androidx.media3.exoplayer.video.VideoRendererEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ExoPlayer-based implementation of the Player interface
 */
@Singleton
class ExoPlayerImpl @Inject constructor(
    private val context: Context,
    private val audioEffectProcessor: AudioEffectProcessor,
    private val performanceManager: PerformanceManager,
    private val hardwareAccelerationManager: HardwareAccelerationManager,
    private val memoryPoolManager: MemoryPoolManager
) : Player {
    
    private var exoPlayer: ExoPlayerImpl? = null
    
    private val _playbackState = MutableStateFlow(PlaybackState())
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()
    
    private val _playerEvents = MutableStateFlow<PlayerEvent?>(null)
    override val playerEvents: Flow<PlayerEvent> = callbackFlow {
        val listener = object : ExoPlayer.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    ExoPlayer.STATE_READY -> {
                        if (exoPlayer?.playWhenReady == true) {
                            trySend(PlayerEvent.PlaybackStarted)
                            updatePlaybackState { copy(isPlaying = true, isPreparing = false) }
                        } else {
                            trySend(PlayerEvent.PlaybackPaused)
                            updatePlaybackState { copy(isPlaying = false, isPreparing = false) }
                        }
                    }
                    ExoPlayer.STATE_BUFFERING -> {
                        trySend(PlayerEvent.BufferingStarted(exoPlayer?.currentPosition ?: 0L))
                        updatePlaybackState { copy(isBuffering = true) }
                    }
                    ExoPlayer.STATE_ENDED -> {
                        trySend(PlayerEvent.PlaybackCompleted)
                        updatePlaybackState { copy(isPlaying = false) }
                    }
                    ExoPlayer.STATE_IDLE -> {
                        updatePlaybackState { copy(isPlaying = false, isPreparing = false) }
                    }
                }
            }
            
            override fun onPlayerError(error: PlaybackException) {
                val errorMessage = error.message ?: "Unknown playback error"
                trySend(PlayerEvent.PlaybackError(errorMessage, error.errorCode))
                updatePlaybackState { 
                    copy(
                        isPlaying = false, 
                        isPreparing = false,
                        error = com.vibestream.player.data.model.PlaybackError(
                            message = errorMessage,
                            code = error.errorCode,
                            recoverable = error.errorCode != PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED
                        )
                    )
                }
            }
            
            override fun onMediaItemTransition(mediaItem: ExoMediaItem?, reason: Int) {
                // Handle media item transition
            }
        }
        
        exoPlayer?.addListener(listener)
        
        awaitClose {
            exoPlayer?.removeListener(listener)
        }
    }
    
    override val currentPosition: Flow<Long> = callbackFlow {
        val updateInterval = 1000L // Update every second
        
        while (true) {
            val position = exoPlayer?.currentPosition ?: 0L
            trySend(position)
            kotlinx.coroutines.delay(updateInterval)
        }
    }
    
    override val duration: Flow<Long> = callbackFlow {
        val listener = object : ExoPlayer.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == ExoPlayer.STATE_READY) {
                    val duration = exoPlayer?.duration ?: C.TIME_UNSET
                    if (duration != C.TIME_UNSET) {
                        trySend(duration)
                        updatePlaybackState { copy(duration = duration) }
                    }
                }
            }
        }
        
        exoPlayer?.addListener(listener)
        
        awaitClose {
            exoPlayer?.removeListener(listener)
        }
    }
    
    override val playbackSpeed: Flow<Float> = callbackFlow {
        val listener = object : ExoPlayer.Listener {
            override fun onPlaybackParametersChanged(playbackParameters: androidx.media3.common.PlaybackParameters) {
                trySend(playbackParameters.speed)
                updatePlaybackState { copy(playbackSpeed = playbackParameters.speed) }
            }
        }
        
        exoPlayer?.addListener(listener)
        
        awaitClose {
            exoPlayer?.removeListener(listener)
        }
    }
    
    init {
        initializePlayer()
    }
    
    private fun initializePlayer() {
        // Create optimized renderers factory with hardware acceleration
        val renderersFactory = createOptimizedRenderersFactory()
        
        exoPlayer = ExoPlayerImpl.Builder(context, renderersFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .setSeekBackIncrementMs(10000) // 10 second seek back
            .setSeekForwardIncrementMs(30000) // 30 second seek forward
            .build()
        
        // Initialize audio effects with player's audio session
        exoPlayer?.audioSessionId?.let { sessionId ->
            if (audioEffectProcessor is AndroidAudioEffectProcessor) {
                audioEffectProcessor.setAudioSession(sessionId)
                audioEffectProcessor.initialize()
                audioEffectProcessor.setEnabled(true)
            }
        }
    }
    
    override suspend fun load(mediaItem: MediaItem): Result<Unit> {
        return try {
            val mediaSource = createMediaSource(mediaItem.uri)
            val exoMediaItem = ExoMediaItem.Builder()
                .setUri(mediaItem.uri)
                .setMediaId(mediaItem.id)
                .build()
            
            exoPlayer?.setMediaSource(mediaSource)
            exoPlayer?.prepare()
            
            updatePlaybackState { 
                copy(
                    currentMediaItem = mediaItem,
                    isPreparing = true,
                    error = null
                )
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun play() {
        exoPlayer?.play()
    }
    
    override fun pause() {
        exoPlayer?.pause()
    }
    
    override fun stop() {
        exoPlayer?.stop()
        updatePlaybackState { 
            copy(
                isPlaying = false, 
                isPreparing = false,
                currentPosition = 0L
            )
        }
    }
    
    override fun seek(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        updatePlaybackState { copy(currentPosition = positionMs) }
    }
    
    override fun setPlaybackSpeed(speed: Float) {
        val playbackParameters = androidx.media3.common.PlaybackParameters(speed)
        exoPlayer?.setPlaybackParameters(playbackParameters)
    }
    
    override fun setAudioTrack(trackId: String?) {
        // TODO: Implement audio track selection
    }
    
    override fun setSubtitleTrack(trackId: String?) {
        // TODO: Implement subtitle track selection
    }
    
    override fun setAudioFilters(filters: AudioFilters) {
        audioEffectProcessor.applyEqualizer(filters.equalizer ?: com.vibestream.player.domain.audio.EqualizerSettings())
        audioEffectProcessor.applyBassBoost(com.vibestream.player.domain.audio.BassBoostSettings(enabled = true, strength = filters.bassBoost / 100f))
        audioEffectProcessor.applyVirtualizer(com.vibestream.player.domain.audio.VirtualizerSettings(enabled = true, strength = filters.virtualizer / 100f))
        
        filters.replayGain?.let { replayGain ->
            audioEffectProcessor.applyReplayGain(replayGain)
        }
    }
    
    override fun setVideoFilters(filters: VideoFilters) {
        // TODO: Implement video filters (brightness, contrast, etc.)
    }
    
    override fun setVolume(volume: Float) {
        exoPlayer?.volume = volume.coerceIn(0f, 1f)
    }
    
    override fun setRepeatMode(mode: RepeatMode) {
        val exoRepeatMode = when (mode) {
            RepeatMode.OFF -> ExoPlayer.REPEAT_MODE_OFF
            RepeatMode.ONE -> ExoPlayer.REPEAT_MODE_ONE
            RepeatMode.ALL -> ExoPlayer.REPEAT_MODE_ALL
        }
        exoPlayer?.repeatMode = exoRepeatMode
        updatePlaybackState { copy(repeatMode = mode) }
    }
    
    override fun setShuffleMode(enabled: Boolean) {
        exoPlayer?.shuffleModeEnabled = enabled
        updatePlaybackState { copy(shuffleMode = enabled) }
    }
    
    override fun release() {
        audioEffectProcessor.release()
        exoPlayer?.release()
        exoPlayer = null
    }
    
    private fun createOptimizedRenderersFactory(): DefaultRenderersFactory {
        return object : DefaultRenderersFactory(context) {
            override fun getCodecAdapterFactory(): androidx.media3.exoplayer.mediacodec.MediaCodecAdapter.Factory {
                // Use hardware acceleration when available
                return if (hardwareAccelerationManager.supportsHardwareDecoding("video/avc")) {
                    androidx.media3.exoplayer.mediacodec.AsynchronousMediaCodecAdapter.Factory(0)
                } else {
                    super.getCodecAdapterFactory()
                }
            }
            
            override fun buildVideoRenderers(
                context: android.content.Context,
                extensionRendererMode: Int,
                mediaCodecSelector: MediaCodecSelector,
                enableDecoderFallback: Boolean,
                eventHandler: android.os.Handler,
                eventListener: VideoRendererEventListener,
                allowedVideoJoiningTimeMs: Long,
                out: java.util.ArrayList<androidx.media3.exoplayer.Renderer>
            ) {
                // Start performance monitoring for video rendering
                val timer = performanceManager.getPerformanceMonitor().startTiming("video_renderer_build")
                
                try {
                    super.buildVideoRenderers(
                        context,
                        extensionRendererMode,
                        mediaCodecSelector,
                        enableDecoderFallback,
                        eventHandler,
                        eventListener,
                        allowedVideoJoiningTimeMs,
                        out
                    )
                } finally {
                    timer.stop()
                }
            }
        }
    }
    
    private fun createMediaSource(uri: String): MediaSource {
        val dataSourceFactory = DefaultDataSource.Factory(
            context,
            DefaultHttpDataSource.Factory()
                .setUserAgent("VibeStream/1.0")
        )
        
        return when {
            uri.contains(".m3u8") -> {
                HlsMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(ExoMediaItem.fromUri(uri))
            }
            uri.contains(".mpd") -> {
                DashMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(ExoMediaItem.fromUri(uri))
            }
            else -> {
                ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(ExoMediaItem.fromUri(uri))
            }
        }
    }
    
    private fun updatePlaybackState(update: PlaybackState.() -> PlaybackState) {
        _playbackState.value = _playbackState.value.update()
    }
}