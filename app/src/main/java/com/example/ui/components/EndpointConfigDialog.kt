package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.network.EndpointConfig
import com.example.network.EndpointType
import com.example.voice.VoicePreset
import com.example.voice.VoiceProfile

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EndpointConfigDialog(
    currentEndpoint: EndpointConfig,
    availableEndpoints: List<EndpointConfig>,
    connectionStatus: Pair<Boolean, String>?,
    currentVoiceProfile: VoiceProfile,
    onDismiss: () -> Unit,
    onSelectEndpoint: (EndpointConfig) -> Unit,
    onSaveEndpoint: (EndpointConfig) -> Unit,
    onTestConnection: () -> Unit,
    onUpdateVoiceProfile: (VoiceProfile) -> Unit,
    onTestVoice: () -> Unit
) {
    var selectedId by remember { mutableStateOf(currentEndpoint.id) }
    val activeConfig = availableEndpoints.find { it.id == selectedId } ?: currentEndpoint

    var name by remember(activeConfig) { mutableStateOf(activeConfig.name) }
    var type by remember(activeConfig) { mutableStateOf(activeConfig.type) }
    var baseUrl by remember(activeConfig) { mutableStateOf(activeConfig.baseUrl) }
    var apiKey by remember(activeConfig) { mutableStateOf(activeConfig.apiKey) }
    var modelName by remember(activeConfig) { mutableStateOf(activeConfig.modelName) }
    var systemPrompt by remember(activeConfig) { mutableStateOf(activeConfig.systemPrompt) }

    // Voice Tuning State
    var selectedPreset by remember(currentVoiceProfile) { mutableStateOf(currentVoiceProfile.preset) }
    var speedRate by remember(currentVoiceProfile) { mutableFloatStateOf(currentVoiceProfile.speedRate) }
    var pitch by remember(currentVoiceProfile) { mutableFloatStateOf(currentVoiceProfile.pitch) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 24.dp)
                .border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(12.dp))
                .testTag("endpoint_config_dialog"),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SYSTEM & VOICE CONFIG",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Configure AI endpoints and neural speech engine",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_config_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Presets & Available Endpoints Selector
                Text(
                    text = "SELECT ACTIVE ENDPOINT",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    availableEndpoints.forEach { ep ->
                        val isSelected = ep.id == selectedId
                        Box(
                            modifier = Modifier
                                .border(
                                    BorderStroke(
                                        if (isSelected) 2.dp else 1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    ),
                                    RoundedCornerShape(6.dp)
                                )
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.surface
                                )
                                .clickable {
                                    selectedId = ep.id
                                    onSelectEndpoint(ep)
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("select_endpoint_${ep.id}")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = ep.badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = ep.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Endpoint Form
                Text(
                    text = "EDIT PROPERTIES: ${activeConfig.name.uppercase()}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("endpoint_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (type != EndpointType.LOCAL_NOTES_ONLY) {
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        label = { Text("Base URL / Host") },
                        supportingText = {
                            Text(
                                when (type) {
                                    EndpointType.LOCAL_DEEPSEEK -> "Local Podman/LAN e.g. http://192.168.1.150:11434/v1 or http://10.0.2.2:11434/v1"
                                    EndpointType.GOOGLE_AI_STUDIO -> "https://generativelanguage.googleapis.com/v1beta"
                                    EndpointType.CUSTOM_AGENT -> "Custom API / Harness Webhook URL"
                                    else -> ""
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("endpoint_url_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = modelName,
                        onValueChange = { modelName = it },
                        label = { Text("Model Name") },
                        supportingText = {
                            Text(
                                "e.g. deepseek-r1:latest, deepseek-chat, or gemini-3.8-flash",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("endpoint_model_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (type != EndpointType.LOCAL_DEEPSEEK) {
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            label = { Text("API Key / Bearer Token") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("endpoint_apikey_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    OutlinedTextField(
                        value = systemPrompt,
                        onValueChange = { systemPrompt = it },
                        label = { Text("System Instructions (Tone/Persona)") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("endpoint_system_prompt_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(8.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "Local Notes mode captures all verbal input directly into your on-device database without external network calls.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Ping connection test feedback
                if (type != EndpointType.LOCAL_NOTES_ONLY) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = onTestConnection,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.testTag("ping_test_button")
                        ) {
                            Icon(Icons.Default.NetworkCheck, contentDescription = "Ping", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("TEST REACHABILITY", style = MaterialTheme.typography.labelSmall)
                        }

                        if (connectionStatus != null) {
                            val (success, statusMsg) = connectionStatus
                            Text(
                                text = statusMsg,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // ==================== VOICE & SPEECH TUNING SECTION ====================
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary), RoundedCornerShape(8.dp)),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "NEURAL VOICE STUDIO",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Gemini-style natural voice synthesis",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = onTestVoice,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.testTag("test_voice_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Test Voice",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("AUDITION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "VOICE PERSONA",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            VoicePreset.values().forEach { preset ->
                                val isSelected = preset == selectedPreset
                                Box(
                                    modifier = Modifier
                                        .border(
                                            BorderStroke(
                                                if (isSelected) 1.5.dp else 1.dp,
                                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                            ),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                            else MaterialTheme.colorScheme.background
                                        )
                                        .clickable {
                                            selectedPreset = preset
                                            val updatedProfile = VoiceProfile(
                                                preset = preset,
                                                speedRate = speedRate,
                                                pitch = pitch
                                            )
                                            onUpdateVoiceProfile(updatedProfile)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .testTag("voice_preset_${preset.name}")
                                ) {
                                    Text(
                                        text = preset.displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Speed Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Cadence / Speed", style = MaterialTheme.typography.labelSmall)
                            Text(
                                String.format("%.2fx", speedRate),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = speedRate,
                            onValueChange = {
                                speedRate = it
                                onUpdateVoiceProfile(VoiceProfile(selectedPreset, speedRate, pitch))
                            },
                            valueRange = 0.8f..1.4f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("voice_speed_slider")
                        )

                        // Pitch Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Pitch", style = MaterialTheme.typography.labelSmall)
                            Text(
                                String.format("%.2fx", pitch),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = pitch,
                            onValueChange = {
                                pitch = it
                                onUpdateVoiceProfile(VoiceProfile(selectedPreset, speedRate, pitch))
                            },
                            valueRange = 0.8f..1.2f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("voice_pitch_slider")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons: Save & Apply
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("CANCEL", style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val updated = activeConfig.copy(
                                name = name,
                                type = type,
                                baseUrl = baseUrl,
                                apiKey = apiKey,
                                modelName = modelName,
                                systemPrompt = systemPrompt
                            )
                            onSaveEndpoint(updated)
                            onSelectEndpoint(updated)
                            onUpdateVoiceProfile(VoiceProfile(selectedPreset, speedRate, pitch))
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.testTag("save_endpoint_button")
                    ) {
                        Text("SAVE & ACTIVATE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
