package com.vibestream.player.ui.screen.settings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vibestream.player.ui.theme.VibeStreamTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun settingsScreen_displaysMainTitle() {
        composeTestRule.setContent {
            VibeStreamTheme {
                SettingsScreen()
            }
        }

        // Verify the main title is displayed
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_displaysAllSections() {
        composeTestRule.setContent {
            VibeStreamTheme {
                SettingsScreen()
            }
        }

        // Verify all main sections are displayed
        composeTestRule.onNodeWithText("Playback").assertIsDisplayed()
        composeTestRule.onNodeWithText("Video").assertIsDisplayed()
        composeTestRule.onNodeWithText("Audio").assertIsDisplayed()
        composeTestRule.onNodeWithText("Subtitles").assertIsDisplayed()
        composeTestRule.onNodeWithText("Library").assertIsDisplayed()
        composeTestRule.onNodeWithText("Gestures").assertIsDisplayed()
        composeTestRule.onNodeWithText("Interface").assertIsDisplayed()
        composeTestRule.onNodeWithText("Privacy").assertIsDisplayed()
        composeTestRule.onNodeWithText("About").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_playbackSettingsDisplayed() {
        composeTestRule.setContent {
            VibeStreamTheme {
                SettingsScreen()
            }
        }

        // Verify playback settings are present
        composeTestRule.onNodeWithText("Auto Play Next").assertIsDisplayed()
        composeTestRule.onNodeWithText("Resume Playback").assertIsDisplayed()
        composeTestRule.onNodeWithText("Seek Step").assertIsDisplayed()
        composeTestRule.onNodeWithText("Default Speed").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_videoSettingsDisplayed() {
        composeTestRule.setContent {
            VibeStreamTheme {
                SettingsScreen()
            }
        }

        // Verify video settings are present
        composeTestRule.onNodeWithText("Video Quality").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hardware Acceleration").assertIsDisplayed()
        composeTestRule.onNodeWithText("Auto Picture-in-Picture").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_audioSettingsDisplayed() {
        composeTestRule.setContent {
            VibeStreamTheme {
                SettingsScreen()
            }
        }

        // Verify audio settings are present
        composeTestRule.onNodeWithText("Normalize Volume").assertIsDisplayed()
        composeTestRule.onNodeWithText("Audio Quality").assertIsDisplayed()
        composeTestRule.onNodeWithText("Crossfade Duration").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_subtitleSettingsDisplayed() {
        composeTestRule.setContent {
            VibeStreamTheme {
                SettingsScreen()
            }
        }

        // Verify subtitle settings are present
        composeTestRule.onNodeWithText("Auto Load External").assertIsDisplayed()
        composeTestRule.onNodeWithText("Default Language").assertIsDisplayed()
        composeTestRule.onNodeWithText("Font Size").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_librarySettingsDisplayed() {
        composeTestRule.setContent {
            VibeStreamTheme {
                SettingsScreen()
            }
        }

        // Verify library settings are present
        composeTestRule.onNodeWithText("Auto Scan").assertIsDisplayed()
        composeTestRule.onNodeWithText("Include Hidden Files").assertIsDisplayed()
        composeTestRule.onNodeWithText("Scan Library Now").assertIsDisplayed()
        composeTestRule.onNodeWithText("Clear Cache").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_gestureSettingsDisplayed() {
        composeTestRule.setContent {
            VibeStreamTheme {
                SettingsScreen()
            }
        }

        // Verify gesture settings are present
        composeTestRule.onNodeWithText("Enable Gestures").assertIsDisplayed()
        composeTestRule.onNodeWithText("Gesture Sensitivity").assertIsDisplayed()
        composeTestRule.onNodeWithText("Haptic Feedback").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_interfaceSettingsDisplayed() {
        composeTestRule.setContent {
            VibeStreamTheme {
                SettingsScreen()
            }
        }

        // Verify interface settings are present
        composeTestRule.onNodeWithText("Theme").assertIsDisplayed()
        composeTestRule.onNodeWithText("Keep Screen On").assertIsDisplayed()
        composeTestRule.onNodeWithText("Show Thumbnails").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_privacySettingsDisplayed() {
        composeTestRule.setContent {
            VibeStreamTheme {
                SettingsScreen()
            }
        }

        // Verify privacy settings are present
        composeTestRule.onNodeWithText("Save Play History").assertIsDisplayed()
        composeTestRule.onNodeWithText("Analytics").assertIsDisplayed()
        composeTestRule.onNodeWithText("Clear Play History").assertIsDisplayed()
        composeTestRule.onNodeWithText("Export Settings").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_aboutSettingsDisplayed() {
        composeTestRule.setContent {
            VibeStreamTheme {
                SettingsScreen()
            }
        }

        // Verify about settings are present
        composeTestRule.onNodeWithText("Version").assertIsDisplayed()
        composeTestRule.onNodeWithText("Open Source Licenses").assertIsDisplayed()
        composeTestRule.onNodeWithText("Privacy Policy").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_scrollable() {
        composeTestRule.setContent {
            VibeStreamTheme {
                SettingsScreen()
            }
        }

        // Verify screen is scrollable by checking top and bottom elements
        composeTestRule.onNodeWithText("Playback")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("About")
            .assertIsDisplayed()
    }

    @Test
    fun settingsScreen_actionItemsClickable() {
        composeTestRule.setContent {
            VibeStreamTheme {
                SettingsScreen()
            }
        }

        // Find and verify action items are clickable
        composeTestRule.onNodeWithText("Scan Library Now")
            .assertIsDisplayed()
            .assertHasClickAction()
        
        composeTestRule.onNodeWithText("Clear Cache")
            .assertIsDisplayed()
            .assertHasClickAction()
            
        composeTestRule.onNodeWithText("Clear Play History")
            .assertIsDisplayed()
            .assertHasClickAction()
    }
}
}