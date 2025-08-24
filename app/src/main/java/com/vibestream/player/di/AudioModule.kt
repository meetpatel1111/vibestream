package com.vibestream.player.di

import com.vibestream.player.data.audio.AndroidAudioEffectProcessor
import com.vibestream.player.domain.audio.AudioEffectProcessor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for audio DSP dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AudioModule {
    
    @Binds
    @Singleton
    abstract fun bindAudioEffectProcessor(
        androidAudioEffectProcessor: AndroidAudioEffectProcessor
    ): AudioEffectProcessor
}