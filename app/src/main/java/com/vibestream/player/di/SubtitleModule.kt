package com.vibestream.player.di

import com.vibestream.player.domain.subtitle.SubtitleManager
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for subtitle system dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object SubtitleModule {
    // SubtitleManager is already injectable via @Inject constructor
}