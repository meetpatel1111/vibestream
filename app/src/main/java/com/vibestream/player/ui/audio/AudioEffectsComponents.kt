package com.vibestream.player.ui.audio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibestream.player.domain.audio.*

/**
 * Main audio effects control panel
 */
@Composable
fun AudioEffectsPanel(
    config: AudioDspConfig,
    onConfigChange: (AudioDspConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(modifier = modifier) {
        // Toggle button
        IconButton(
            onClick = { expanded = !expanded },
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.7f))
        ) {
            Icon(
                imageVector = Icons.Default.Equalizer,
                contentDescription = "Audio Effects",
                tint = if (config.equalizer.enabled || config.bassBoost.enabled || config.virtualizer.enabled) 
                    MaterialTheme.colorScheme.primary else Color.White
            )
        }
        
        // Expanded panel
        AnimatedVisibility(
            visible = expanded,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.8f)
                )
            ) {
                LazyColumn(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Equalizer section
                    item {
                        EqualizerSection(
                            settings = config.equalizer,
                            onSettingsChange = { newSettings ->
                                onConfigChange(config.copy(equalizer = newSettings))
                            }
                        )
                    }
                    
                    // Bass boost section
                    item {
                        BassBoostSection(
                            settings = config.bassBoost,
                            onSettingsChange = { newSettings ->
                                onConfigChange(config.copy(bassBoost = newSettings))
                            }
                        )
                    }
                    
                    // Virtualizer section
                    item {
                        VirtualizerSection(
                            settings = config.virtualizer,
                            onSettingsChange = { newSettings ->
                                onConfigChange(config.copy(virtualizer = newSettings))
                            }
                        )
                    }
                    
                    // Playback speed section
                    item {
                        PlaybackSpeedSection(
                            settings = config.playbackSpeed,
                            onSettingsChange = { newSettings ->
                                onConfigChange(config.copy(playbackSpeed = newSettings))
                            }
                        )
                    }
                }\n            }\n        }\n    }\n}\n\n/**\n * 10-band equalizer UI\n */\n@Composable\nfun EqualizerSection(\n    settings: EqualizerSettings,\n    onSettingsChange: (EqualizerSettings) -> Unit,\n    modifier: Modifier = Modifier\n) {\n    var showPresets by remember { mutableStateOf(false) }\n    \n    Column(modifier = modifier) {\n        // Header with enable toggle\n        Row(\n            modifier = Modifier.fillMaxWidth(),\n            horizontalArrangement = Arrangement.SpaceBetween,\n            verticalAlignment = Alignment.CenterVertically\n        ) {\n            Text(\n                text = \"Equalizer\",\n                color = Color.White,\n                style = MaterialTheme.typography.titleMedium,\n                fontWeight = FontWeight.Bold\n            )\n            \n            Row(verticalAlignment = Alignment.CenterVertically) {\n                TextButton(\n                    onClick = { showPresets = true },\n                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White)\n                ) {\n                    Text(\"Presets\")\n                }\n                \n                Switch(\n                    checked = settings.enabled,\n                    onCheckedChange = { enabled ->\n                        onSettingsChange(settings.copy(enabled = enabled))\n                    },\n                    colors = SwitchDefaults.colors(\n                        checkedThumbColor = MaterialTheme.colorScheme.primary\n                    )\n                )\n            }\n        }\n        \n        if (settings.enabled) {\n            Spacer(modifier = Modifier.height(16.dp))\n            \n            // Preamp control\n            Column {\n                Text(\n                    text = \"Preamp: ${String.format(\"%.1f\", settings.preamp)}dB\",\n                    color = Color.White,\n                    style = MaterialTheme.typography.bodySmall\n                )\n                Slider(\n                    value = settings.preamp,\n                    onValueChange = { preamp ->\n                        onSettingsChange(settings.copy(preamp = preamp))\n                    },\n                    valueRange = -20f..20f,\n                    colors = SliderDefaults.colors(\n                        thumbColor = MaterialTheme.colorScheme.primary,\n                        activeTrackColor = MaterialTheme.colorScheme.primary\n                    )\n                )\n            }\n            \n            Spacer(modifier = Modifier.height(16.dp))\n            \n            // EQ bands\n            Row(\n                modifier = Modifier.fillMaxWidth(),\n                horizontalArrangement = Arrangement.SpaceEvenly\n            ) {\n                settings.bands.forEachIndexed { index, band ->\n                    EqualizerBandControl(\n                        band = band,\n                        onBandChange = { newBand ->\n                            val newBands = settings.bands.toMutableList()\n                            newBands[index] = newBand\n                            onSettingsChange(settings.copy(bands = newBands))\n                        },\n                        modifier = Modifier.weight(1f)\n                    )\n                }\n            }\n        }\n    }\n    \n    // Preset selection dialog\n    if (showPresets) {\n        EqualizerPresetDialog(\n            currentSettings = settings,\n            onPresetSelected = { preset ->\n                onSettingsChange(preset)\n                showPresets = false\n            },\n            onDismiss = { showPresets = false }\n        )\n    }\n}\n\n/**\n * Individual equalizer band control\n */\n@Composable\nfun EqualizerBandControl(\n    band: EqualizerBand,\n    onBandChange: (EqualizerBand) -> Unit,\n    modifier: Modifier = Modifier\n) {\n    Column(\n        modifier = modifier,\n        horizontalAlignment = Alignment.CenterHorizontally\n    ) {\n        // Frequency label\n        Text(\n            text = formatFrequency(band.frequency),\n            color = Color.White,\n            style = MaterialTheme.typography.bodySmall,\n            fontSize = 10.sp,\n            textAlign = TextAlign.Center\n        )\n        \n        // Vertical slider\n        Slider(\n            value = band.gain,\n            onValueChange = { gain ->\n                onBandChange(band.copy(gain = gain))\n            },\n            valueRange = -20f..20f,\n            colors = SliderDefaults.colors(\n                thumbColor = MaterialTheme.colorScheme.primary,\n                activeTrackColor = MaterialTheme.colorScheme.primary\n            ),\n            modifier = Modifier\n                .height(120.dp)\n                .width(40.dp)\n        )\n        \n        // Gain value\n        Text(\n            text = String.format(\"%.0f\", band.gain),\n            color = Color.White,\n            style = MaterialTheme.typography.bodySmall,\n            fontSize = 10.sp\n        )\n    }\n}\n\n/**\n * Bass boost control section\n */\n@Composable\nfun BassBoostSection(\n    settings: BassBoostSettings,\n    onSettingsChange: (BassBoostSettings) -> Unit,\n    modifier: Modifier = Modifier\n) {\n    Column(modifier = modifier) {\n        Row(\n            modifier = Modifier.fillMaxWidth(),\n            horizontalArrangement = Arrangement.SpaceBetween,\n            verticalAlignment = Alignment.CenterVertically\n        ) {\n            Text(\n                text = \"Bass Boost\",\n                color = Color.White,\n                style = MaterialTheme.typography.titleMedium,\n                fontWeight = FontWeight.Bold\n            )\n            \n            Switch(\n                checked = settings.enabled,\n                onCheckedChange = { enabled ->\n                    onSettingsChange(settings.copy(enabled = enabled))\n                },\n                colors = SwitchDefaults.colors(\n                    checkedThumbColor = MaterialTheme.colorScheme.primary\n                )\n            )\n        }\n        \n        if (settings.enabled) {\n            Spacer(modifier = Modifier.height(8.dp))\n            \n            Text(\n                text = \"Strength: ${(settings.strength * 100).toInt()}%\",\n                color = Color.White,\n                style = MaterialTheme.typography.bodySmall\n            )\n            \n            Slider(\n                value = settings.strength,\n                onValueChange = { strength ->\n                    onSettingsChange(settings.copy(strength = strength))\n                },\n                valueRange = 0f..1f,\n                colors = SliderDefaults.colors(\n                    thumbColor = MaterialTheme.colorScheme.primary,\n                    activeTrackColor = MaterialTheme.colorScheme.primary\n                )\n            )\n        }\n    }\n}\n\n/**\n * Virtualizer control section\n */\n@Composable\nfun VirtualizerSection(\n    settings: VirtualizerSettings,\n    onSettingsChange: (VirtualizerSettings) -> Unit,\n    modifier: Modifier = Modifier\n) {\n    Column(modifier = modifier) {\n        Row(\n            modifier = Modifier.fillMaxWidth(),\n            horizontalArrangement = Arrangement.SpaceBetween,\n            verticalAlignment = Alignment.CenterVertically\n        ) {\n            Text(\n                text = \"3D Audio\",\n                color = Color.White,\n                style = MaterialTheme.typography.titleMedium,\n                fontWeight = FontWeight.Bold\n            )\n            \n            Switch(\n                checked = settings.enabled,\n                onCheckedChange = { enabled ->\n                    onSettingsChange(settings.copy(enabled = enabled))\n                },\n                colors = SwitchDefaults.colors(\n                    checkedThumbColor = MaterialTheme.colorScheme.primary\n                )\n            )\n        }\n        \n        if (settings.enabled) {\n            Spacer(modifier = Modifier.height(8.dp))\n            \n            Text(\n                text = \"Strength: ${(settings.strength * 100).toInt()}%\",\n                color = Color.White,\n                style = MaterialTheme.typography.bodySmall\n            )\n            \n            Slider(\n                value = settings.strength,\n                onValueChange = { strength ->\n                    onSettingsChange(settings.copy(strength = strength))\n                },\n                valueRange = 0f..1f,\n                colors = SliderDefaults.colors(\n                    thumbColor = MaterialTheme.colorScheme.primary,\n                    activeTrackColor = MaterialTheme.colorScheme.primary\n                )\n            )\n        }\n    }\n}\n\n/**\n * Playback speed control section\n */\n@Composable\nfun PlaybackSpeedSection(\n    settings: PlaybackSpeedSettings,\n    onSettingsChange: (PlaybackSpeedSettings) -> Unit,\n    modifier: Modifier = Modifier\n) {\n    Column(modifier = modifier) {\n        Text(\n            text = \"Playback Speed\",\n            color = Color.White,\n            style = MaterialTheme.typography.titleMedium,\n            fontWeight = FontWeight.Bold\n        )\n        \n        Spacer(modifier = Modifier.height(8.dp))\n        \n        Text(\n            text = \"Speed: ${String.format(\"%.2f\", settings.speed)}x\",\n            color = Color.White,\n            style = MaterialTheme.typography.bodySmall\n        )\n        \n        Slider(\n            value = settings.speed,\n            onValueChange = { speed ->\n                onSettingsChange(settings.copy(speed = speed))\n            },\n            valueRange = 0.25f..4.0f,\n            colors = SliderDefaults.colors(\n                thumbColor = MaterialTheme.colorScheme.primary,\n                activeTrackColor = MaterialTheme.colorScheme.primary\n            )\n        )\n        \n        Row(\n            modifier = Modifier.fillMaxWidth(),\n            horizontalArrangement = Arrangement.spacedBy(16.dp)\n        ) {\n            Row(verticalAlignment = Alignment.CenterVertically) {\n                Checkbox(\n                    checked = settings.preservePitch,\n                    onCheckedChange = { preserve ->\n                        onSettingsChange(settings.copy(preservePitch = preserve))\n                    },\n                    colors = CheckboxDefaults.colors(\n                        checkedColor = MaterialTheme.colorScheme.primary\n                    )\n                )\n                Text(\n                    text = \"Preserve Pitch\",\n                    color = Color.White,\n                    style = MaterialTheme.typography.bodySmall\n                )\n            }\n        }\n    }\n}\n\n/**\n * Equalizer preset selection dialog\n */\n@Composable\nfun EqualizerPresetDialog(\n    currentSettings: EqualizerSettings,\n    onPresetSelected: (EqualizerSettings) -> Unit,\n    onDismiss: () -> Unit\n) {\n    val presets = listOf(\n        \"Flat\" to EqualizerSettings.PRESET_FLAT,\n        \"Rock\" to EqualizerSettings.PRESET_ROCK,\n        \"Pop\" to EqualizerSettings.PRESET_POP,\n        \"Jazz\" to EqualizerSettings.PRESET_JAZZ,\n        \"Classical\" to EqualizerSettings.PRESET_CLASSICAL\n    )\n    \n    AlertDialog(\n        onDismissRequest = onDismiss,\n        title = { Text(\"Equalizer Presets\") },\n        text = {\n            LazyColumn {\n                items(presets) { (name, preset) ->\n                    Row(\n                        modifier = Modifier\n                            .fillMaxWidth()\n                            .padding(vertical = 8.dp),\n                        verticalAlignment = Alignment.CenterVertically\n                    ) {\n                        RadioButton(\n                            selected = currentSettings.bands == preset.bands,\n                            onClick = { onPresetSelected(preset) }\n                        )\n                        Spacer(modifier = Modifier.width(8.dp))\n                        Text(name)\n                    }\n                }\n            }\n        },\n        confirmButton = {\n            TextButton(onClick = onDismiss) {\n                Text(\"Close\")\n            }\n        }\n    )\n}\n\n// Utility functions\n\nprivate fun formatFrequency(freq: Int): String {\n    return when {\n        freq >= 1000 -> \"${freq / 1000}k\"\n        else -> freq.toString()\n    }\n}"}, {"original_text": "", "replace_all": false}]