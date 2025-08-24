package com.vibestream.player.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.NetworkWifi
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.vibestream.player.R

/**
 * App navigation screens
 */
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Videos : Screen("videos")
    object Music : Screen("music")
    object Folders : Screen("folders")
    object Network : Screen("network")
    object Playlists : Screen("playlists")
    object Cast : Screen("cast")
    object Settings : Screen("settings")
    object NowPlaying : Screen("now_playing")
}

/**
 * Bottom navigation item configuration
 */
data class BottomNavItem(
    val screen: Screen,
    val icon: ImageVector,
    @StringRes val title: Int
) {
    companion object {
        val items = listOf(
            BottomNavItem(
                screen = Screen.Home,
                icon = Icons.Default.Home,
                title = R.string.nav_home
            ),
            BottomNavItem(
                screen = Screen.Videos,
                icon = Icons.Default.Movie,
                title = R.string.nav_videos
            ),
            BottomNavItem(
                screen = Screen.Music,
                icon = Icons.Default.LibraryMusic,
                title = R.string.nav_music
            ),
            BottomNavItem(
                screen = Screen.Folders,
                icon = Icons.Default.Folder,
                title = R.string.nav_folders
            ),
            BottomNavItem(
                screen = Screen.Settings,
                icon = Icons.Default.Settings,
                title = R.string.nav_settings
            )
        )
    }
}