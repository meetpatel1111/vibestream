package com.vibestream.player.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Header
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        
        // Settings categories
        val settingsCategories = listOf(
            SettingsCategory(
                title = "Playback",
                icon = Icons.Default.PlayArrow,
                items = listOf(
                    SettingsItem.Toggle(
                        title = "Auto Play Next",
                        subtitle = "Automatically play next item in queue",
                        checked = uiState.autoPlayNext,
                        onCheckedChange = viewModel::setAutoPlayNext
                    ),
                    SettingsItem.Toggle(
                        title = "Resume Playback",
                        subtitle = "Resume from last position when reopening",
                        checked = uiState.resumePlayback,
                        onCheckedChange = viewModel::setResumePlayback
                    ),
                    SettingsItem.Slider(
                        title = "Seek Step",
                        subtitle = "${uiState.seekStepSeconds}s",
                        value = uiState.seekStepSeconds.toFloat(),
                        valueRange = 5f..60f,
                        onValueChange = { viewModel.setSeekStepSeconds(it.toInt()) }
                    ),
                    SettingsItem.Selection(
                        title = "Default Speed",
                        subtitle = "${uiState.defaultSpeed}x",
                        options = listOf("0.5x", "0.75x", "1.0x", "1.25x", "1.5x", "2.0x"),
                        selectedIndex = when(uiState.defaultSpeed) {
                            0.5f -> 0
                            0.75f -> 1
                            1.0f -> 2
                            1.25f -> 3
                            1.5f -> 4
                            2.0f -> 5
                            else -> 2
                        },
                        onSelectionChange = { index ->
                            val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                            viewModel.setDefaultSpeed(speeds[index])
                        }
                    )
                )
            ),
            SettingsCategory(
                title = "Video",
                icon = Icons.Default.Movie,
                items = listOf(
                    SettingsItem.Selection(
                        title = "Video Quality",
                        subtitle = uiState.videoQuality,
                        options = listOf("Auto", "720p", "1080p", "4K"),
                        selectedIndex = when(uiState.videoQuality) {
                            "Auto" -> 0
                            "720p" -> 1
                            "1080p" -> 2
                            "4K" -> 3
                            else -> 0
                        },
                        onSelectionChange = { index ->
                            val qualities = listOf("Auto", "720p", "1080p", "4K")
                            viewModel.setVideoQuality(qualities[index])
                        }
                    ),
                    SettingsItem.Toggle(
                        title = "Hardware Acceleration",
                        subtitle = "Use hardware decoding when available",
                        checked = uiState.hardwareAcceleration,
                        onCheckedChange = viewModel::setHardwareAcceleration
                    ),
                    SettingsItem.Toggle(
                        title = "Auto Picture-in-Picture",
                        subtitle = "Enter PiP mode when switching apps",
                        checked = uiState.autoPictureInPicture,
                        onCheckedChange = viewModel::setAutoPictureInPicture
                    )
                )
            ),
            SettingsCategory(
                title = "Audio",
                icon = Icons.Default.VolumeUp,
                items = listOf(
                    SettingsItem.Toggle(
                        title = "Normalize Volume",
                        subtitle = "Apply ReplayGain for consistent volume",
                        checked = uiState.normalizeVolume,
                        onCheckedChange = viewModel::setNormalizeVolume
                    ),
                    SettingsItem.Selection(
                        title = "Audio Quality",
                        subtitle = uiState.audioQuality,
                        options = listOf("Auto", "High", "Lossless"),
                        selectedIndex = when(uiState.audioQuality) {
                            "Auto" -> 0
                            "High" -> 1
                            "Lossless" -> 2
                            else -> 0
                        },
                        onSelectionChange = { index ->
                            val qualities = listOf("Auto", "High", "Lossless")
                            viewModel.setAudioQuality(qualities[index])
                        }
                    ),
                    SettingsItem.Slider(
                        title = "Crossfade Duration",
                        subtitle = "${uiState.crossfadeDuration}ms",
                        value = uiState.crossfadeDuration.toFloat(),
                        valueRange = 0f..5000f,
                        onValueChange = { viewModel.setCrossfadeDuration(it.toLong()) }
                    )
                )
            ),
            SettingsCategory(
                title = "Subtitles",
                icon = Icons.Default.Subtitles,
                items = listOf(
                    SettingsItem.Toggle(
                        title = "Auto Load External",
                        subtitle = "Automatically load external subtitle files",
                        checked = uiState.autoLoadSubtitles,
                        onCheckedChange = viewModel::setAutoLoadSubtitles
                    ),
                    SettingsItem.Selection(
                        title = "Default Language",
                        subtitle = uiState.defaultSubtitleLanguage,
                        options = listOf("None", "English", "Chinese", "Spanish", "French"),
                        selectedIndex = when(uiState.defaultSubtitleLanguage) {
                            "None" -> 0
                            "English" -> 1
                            "Chinese" -> 2
                            "Spanish" -> 3
                            "French" -> 4
                            else -> 0
                        },
                        onSelectionChange = { index ->
                            val languages = listOf("None", "English", "Chinese", "Spanish", "French")
                            viewModel.setDefaultSubtitleLanguage(languages[index])
                        }
                    ),
                    SettingsItem.Slider(
                        title = "Font Size",
                        subtitle = "${uiState.subtitleFontSize}sp",
                        value = uiState.subtitleFontSize,
                        valueRange = 12f..32f,
                        onValueChange = viewModel::setSubtitleFontSize
                    )
                )
            ),
            SettingsCategory(
                title = "Library",
                icon = Icons.Default.LibraryMusic,
                items = listOf(
                    SettingsItem.Toggle(
                        title = "Auto Scan",
                        subtitle = "Automatically scan for new media files",
                        checked = uiState.autoScanLibrary,
                        onCheckedChange = viewModel::setAutoScanLibrary
                    ),
                    SettingsItem.Toggle(
                        title = "Include Hidden Files",
                        subtitle = "Scan hidden files and folders",
                        checked = uiState.includeHiddenFiles,
                        onCheckedChange = viewModel::setIncludeHiddenFiles
                    ),
                    SettingsItem.Action(
                        title = "Scan Library Now",
                        subtitle = "Manually scan for media files",
                        onClick = viewModel::scanLibrary
                    ),
                    SettingsItem.Action(
                        title = "Clear Cache",
                        subtitle = "Clear thumbnails and temporary files",
                        onClick = viewModel::clearCache
                    )
                )
            ),
            SettingsCategory(
                title = "Gestures",
                icon = Icons.Default.TouchApp,
                items = listOf(
                    SettingsItem.Toggle(
                        title = "Enable Gestures",
                        subtitle = "Swipe to seek, adjust volume and brightness",
                        checked = uiState.gesturesEnabled,
                        onCheckedChange = viewModel::setGesturesEnabled
                    ),
                    SettingsItem.Slider(
                        title = "Gesture Sensitivity",
                        subtitle = "${(uiState.gestureSensitivity * 100).toInt()}%",
                        value = uiState.gestureSensitivity,
                        valueRange = 0.1f..2.0f,
                        onValueChange = viewModel::setGestureSensitivity
                    ),
                    SettingsItem.Toggle(
                        title = "Haptic Feedback",
                        subtitle = "Vibrate on gesture interactions",
                        checked = uiState.hapticFeedback,
                        onCheckedChange = viewModel::setHapticFeedback
                    )
                )
            ),
            SettingsCategory(
                title = "Interface",
                icon = Icons.Default.Palette,
                items = listOf(
                    SettingsItem.Selection(
                        title = "Theme",
                        subtitle = uiState.theme,
                        options = listOf("System", "Light", "Dark", "AMOLED"),
                        selectedIndex = when(uiState.theme) {
                            "System" -> 0
                            "Light" -> 1
                            "Dark" -> 2
                            "AMOLED" -> 3
                            else -> 0
                        },
                        onSelectionChange = { index ->
                            val themes = listOf("System", "Light", "Dark", "AMOLED")
                            viewModel.setTheme(themes[index])
                        }
                    ),
                    SettingsItem.Toggle(
                        title = "Keep Screen On",
                        subtitle = "Prevent screen from turning off during playback",
                        checked = uiState.keepScreenOn,
                        onCheckedChange = viewModel::setKeepScreenOn
                    ),
                    SettingsItem.Toggle(
                        title = "Show Thumbnails",
                        subtitle = "Display video thumbnails in library",
                        checked = uiState.showThumbnails,
                        onCheckedChange = viewModel::setShowThumbnails
                    )
                )
            ),
            SettingsCategory(
                title = "Privacy",
                icon = Icons.Default.Security,
                items = listOf(
                    SettingsItem.Toggle(
                        title = "Save Play History",
                        subtitle = "Remember playback positions and history",
                        checked = uiState.savePlayHistory,
                        onCheckedChange = viewModel::setSavePlayHistory
                    ),
                    SettingsItem.Toggle(
                        title = "Analytics",
                        subtitle = "Help improve the app by sharing usage data",
                        checked = uiState.analyticsEnabled,
                        onCheckedChange = viewModel::setAnalyticsEnabled
                    ),
                    SettingsItem.Action(
                        title = "Clear Play History",
                        subtitle = "Delete all playback history",
                        onClick = viewModel::clearPlayHistory
                    ),
                    SettingsItem.Action(
                        title = "Export Settings",
                        subtitle = "Save settings to file",
                        onClick = viewModel::exportSettings
                    )
                )
            ),
            SettingsCategory(
                title = "About",
                icon = Icons.Default.Info,
                items = listOf(
                    SettingsItem.Info(
                        title = "Version",
                        subtitle = "1.0.0 (M1)"
                    ),
                    SettingsItem.Action(
                        title = "Open Source Licenses",
                        subtitle = "View third-party licenses",
                        onClick = viewModel::showLicenses
                    ),
                    SettingsItem.Action(
                        title = "Privacy Policy",
                        subtitle = "View privacy policy",
                        onClick = viewModel::showPrivacyPolicy
                    )
                )
            )
        )
        
        items(settingsCategories) { category ->
            SettingsCategoryCard(
                category = category,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}