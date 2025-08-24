package com.vibestream.player.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vibestream.player.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.vibestream.player.ui.navigation.BottomNavItem
import com.vibestream.player.ui.navigation.Screen
import com.vibestream.player.ui.screen.home.HomeScreen
import com.vibestream.player.ui.screen.library.LibraryScreen
import com.vibestream.player.ui.screen.nowplaying.NowPlayingScreen
import com.vibestream.player.ui.screen.settings.SettingsScreen
import androidx.compose.animation.AnimatedVisibility

@Composable
fun VibeStreamApp(
    isInPipMode: Boolean = false,
    onEnterPip: () -> Unit = {}
) {
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            
            // Hide bottom navigation in Now Playing screen or PiP mode
            if (currentDestination?.route != Screen.NowPlaying.route && !isInPipMode) {
                NavigationBar {
                    BottomNavItem.items.forEach { item ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = stringResource(item.title)
                                )
                            },
                            label = { Text(stringResource(item.title)) },
                            selected = currentDestination?.hierarchy?.any { 
                                it.route == item.screen.route 
                            } == true,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    // Pop up to the start destination of the graph to
                                    // avoid building up a large stack of destinations
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    // Avoid multiple copies of the same destination when
                                    // reselecting the same item
                                    launchSingleTop = true
                                    // Restore state when reselecting a previously selected item
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToNowPlaying = {
                        navController.navigate(Screen.NowPlaying.route)
                    }
                )
            }
            
            composable(Screen.Videos.route) {
                LibraryScreen(
                    mediaType = com.vibestream.player.data.model.MediaType.VIDEO,
                    onNavigateToNowPlaying = {
                        navController.navigate(Screen.NowPlaying.route)
                    }
                )
            }
            
            composable(Screen.Music.route) {
                LibraryScreen(
                    mediaType = com.vibestream.player.data.model.MediaType.AUDIO,
                    onNavigateToNowPlaying = {
                        navController.navigate(Screen.NowPlaying.route)
                    }
                )
            }
            
            composable(Screen.Folders.route) {
                // TODO: Implement FoldersScreen
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Folders",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }
            
            composable(Screen.Network.route) {
                // TODO: Implement NetworkScreen
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Network",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }
            
            composable(Screen.Playlists.route) {
                // TODO: Implement PlaylistsScreen
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Playlists",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }
            
            composable(Screen.Cast.route) {
                // TODO: Implement CastScreen
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Cast",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }
            
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
            
            composable(Screen.NowPlaying.route) {
                NowPlayingScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    isInPipMode = isInPipMode,
                    onEnterPip = onEnterPip
                )
            }
        }
    }
}