package com.vibestream.player.di

import android.content.Context
import com.vibestream.player.data.player.ExoPlayerImpl
import com.vibestream.player.domain.player.Player
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for player dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PlayerModule {
    
    @Binds
    @Singleton
    abstract fun bindPlayer(exoPlayerImpl: ExoPlayerImpl): Player
}