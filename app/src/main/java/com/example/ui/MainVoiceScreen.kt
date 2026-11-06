package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.network.EndpointConfig
import com.example.ui.components.AudioWaveformVisualizer
import com.example.ui.components.EndpointConfigDialog
import com.example.ui.components.LogsAndChecklistSheet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainVoiceScreen(
    viewModel: VoiceInteractionViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // State collections
    val activeEndpoint by viewModel.activeEndpoint.collectAsStateWithLifecycle()
    val endpoints by viewModel.endpoints.collectAsStateWithLifecycle()
    val isContinuousMode by viewModel.isContinuousMode.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val partialTranscript by viewModel.partialTranscript.collectAsStateWithLifecycle()
    val audioRms by viewModel.audioRms.collectAsStateWithLifecycle()
    val lastUserQuery by viewModel.lastUserQuery.collectAsStateWithLifecycle()
    val lastAssistantResponse by viewModel.lastAssistantResponse.collectAsStateWithLifecycle()
    val lastLatencyMs by viewModel.lastLatencyMs.collectAsStateWithLifecycle()
    val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()
    val notification by viewModel.notification.collectAsStateWithLifecycle()
    val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
    val voiceProfile by viewModel.voiceProfile.collectAsStateWithLifecycle()

    // Dialog & Sheet States
    var showConfigDialog by remember { mutableStateOf(false) }
    var showLogsSheet by remember { mutableStateOf(false) }
    val logsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Manual text query state
    var manualQueryText by remember { mutableStateOf("") }
    var showManualInput by remember { mutableStateOf(false) }

    // Audio Permission Check
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted) {
            viewModel.notify("Microphone permission granted")
        } else {
            viewModel.notify("Microphone permission required for voice interaction", isError = true)
        }
    }

    // Handle incoming notifications
    LaunchedEffect(notification) {
        notification?.let {
            snackbarHostState.showSnackbar(it.message)
            viewModel.clearNotification()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // TOP BAR: Brand + Target Selector + Action Icons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MONOVOX",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "VOICE ENGINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Checklists / Logs Button with badge count
                    OutlinedButton(
                        onClick = { showLogsSheet = true },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("open_logs_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Checklist,
                            contentDescription = "Logs",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${allLogs.size}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Settings / Endpoint Config Button
                    IconButton(
                        onClick = { showConfigDialog = true },
                        modifier = Modifier
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(8.dp))
                            .size(38.dp)
                            .testTag("open_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configuration",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ENDPOINT SELECTOR PILL & TELEMETRY
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(8.dp)),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = activeEndpoint.badge,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = activeEndpoint.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (activeEndpoint.modelName.isNotBlank()) {
                                Text(
                                    text = activeEndpoint.modelName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (lastLatencyMs != null) {
                        Text(
                            text = "${lastLatencyMs}ms",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Permission Request Notice (if not granted)
            if (!hasAudioPermission) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "MICROPHONE PERMISSION NEEDED",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Monovox requires audio access to transcribe your spoken voice and process commands.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.testTag("request_audio_permission_button")
                        ) {
                            Text("GRANT PERMISSION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // CONVERSATION DISPLAY AREA (Scrollable)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Status Badge Banner
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val statusText = when {
                            isListening -> "● LISTENING..."
                            isProcessing -> "▲ INFERENCE RUNNING..."
                            isSpeaking -> "▶ SPEAKING AUDIBLE REPLY"
                            else -> "— READY"
                        }
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        // Barge-in button if TTS speaking
                        if (isSpeaking) {
                            OutlinedButton(
                                onClick = { viewModel.stopSpeaking() },
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.testTag("barge_in_stop_button")
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "Interrupt", modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("INTERRUPT", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    // Live Partial Transcript (Ongoing speech)
                    if (partialTranscript.isNotBlank()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.background,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "LIVE SPEECH",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = partialTranscript,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Last User Utterance
                    if (lastUserQuery.isNotBlank()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.background,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "USER QUERY",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = lastUserQuery,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }

                    // Assistant Response or Local Captured status
                    if (lastAssistantResponse.isNotBlank()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.background,
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${activeEndpoint.badge} RESPONSE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (isSpeaking) {
                                        Icon(
                                            imageVector = Icons.Default.VolumeUp,
                                            contentDescription = "Playing Audio",
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = lastAssistantResponse,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    } else if (lastUserQuery.isBlank() && partialTranscript.isBlank()) {
                        // Empty state tips
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "READY FOR VOICE INTERACTION",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "• Hold PUSH-TO-TALK or toggle LIVE STREAMING\n• Say \"Note: ...\" to save offline to Room DB\n• Say \"Todo: ...\" to create a checklist item\n• Any other utterance queries ${activeEndpoint.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // AUDIO WAVEFORM VISUALIZER
            AudioWaveformVisualizer(
                isListening = isListening,
                isSpeaking = isSpeaking,
                isProcessing = isProcessing,
                audioRms = audioRms
            )

            Spacer(modifier = Modifier.height(10.dp))

            // DUAL INPUT CONTROLS: Mode Selector + PTT Tactile Action
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Mode Toggle Row (Push-to-Talk vs Persistent Streaming)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // PTT Mode Pill
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                BorderStroke(
                                    if (!isContinuousMode) 1.5.dp else 1.dp,
                                    if (!isContinuousMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                ),
                                RoundedCornerShape(6.dp)
                            )
                            .background(
                                if (!isContinuousMode) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surface
                            )
                            .clickable {
                                if (isContinuousMode) viewModel.toggleContinuousMode()
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "PUSH-TO-TALK",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (!isContinuousMode) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Persistent Streaming Mode Pill
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                BorderStroke(
                                    if (isContinuousMode) 1.5.dp else 1.dp,
                                    if (isContinuousMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                ),
                                RoundedCornerShape(6.dp)
                            )
                            .background(
                                if (isContinuousMode) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surface
                            )
                            .clickable {
                                if (!hasAudioPermission) {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                } else {
                                    viewModel.toggleContinuousMode()
                                }
                            }
                            .padding(vertical = 8.dp)
                            .testTag("toggle_continuous_mode_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isContinuousMode) {
                                Icon(
                                    imageVector = Icons.Default.Radio,
                                    contentDescription = "Live",
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = if (isContinuousMode) "LIVE STREAMING ON" else "PERSISTENT STREAM",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isContinuousMode) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // PRIMARY ACTION: Push-to-Talk Tactile Button OR Persistent Mode Indicator
                if (!isContinuousMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                            .border(
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                                RoundedCornerShape(12.dp)
                            )
                            .pointerInput(hasAudioPermission) {
                                if (!hasAudioPermission) {
                                    detectTapGestures(
                                        onTap = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                                    )
                                } else {
                                    detectTapGestures(
                                        onPress = {
                                            viewModel.onPttDown()
                                            tryAwaitRelease()
                                            viewModel.onPttUp()
                                        }
                                    )
                                }
                            }
                            .testTag("ptt_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
                                contentDescription = "PTT Microphone",
                                modifier = Modifier.size(24.dp),
                                tint = if (isListening) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isListening) "LISTENING... (RELEASE TO SEND)" else "HOLD TO TALK (PUSH-TO-TALK)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isListening) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), RoundedCornerShape(12.dp))
                            .clickable { viewModel.toggleContinuousMode() }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Active Listening",
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "STREAM ACTIVE: SPEAK NATURALLY (TAP TO STOP)",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Toggle for Manual Text Input (for testing without voice or in quiet environments)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showManualInput) "Hide text input" else "Or type query / note command",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clickable { showManualInput = !showManualInput }
                            .padding(vertical = 4.dp, horizontal = 8.dp)
                    )
                }

                AnimatedVisibility(visible = showManualInput, enter = fadeIn(), exit = fadeOut()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = manualQueryText,
                            onValueChange = { manualQueryText = it },
                            placeholder = { Text("e.g. Note: review docs OR DeepSeek prompt") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("manual_query_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (manualQueryText.isNotBlank()) {
                                    viewModel.sendTextQuery(manualQueryText)
                                    manualQueryText = ""
                                }
                            },
                            modifier = Modifier
                                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary), RoundedCornerShape(8.dp))
                                .testTag("send_manual_query_button")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }

    // Modal Sheet: Logs & Checklists
    if (showLogsSheet) {
        LogsAndChecklistSheet(
            logs = allLogs,
            sheetState = logsSheetState,
            onDismiss = { showLogsSheet = false },
            onToggleCompleted = { viewModel.toggleLogCompleted(it) },
            onDeleteLog = { viewModel.deleteLog(it) },
            onClearAll = { viewModel.clearAllLogs() },
            onCopyNotice = { viewModel.notify(it) }
        )
    }

    // Dialog: Endpoint Configuration
    if (showConfigDialog) {
        EndpointConfigDialog(
            currentEndpoint = activeEndpoint,
            availableEndpoints = endpoints,
            connectionStatus = connectionStatus,
            currentVoiceProfile = voiceProfile,
            onDismiss = { showConfigDialog = false },
            onSelectEndpoint = { viewModel.selectEndpoint(it) },
            onSaveEndpoint = { viewModel.updateEndpoint(it) },
            onTestConnection = { viewModel.testActiveConnection() },
            onUpdateVoiceProfile = { viewModel.updateVoiceProfile(it) },
            onTestVoice = { viewModel.testVoice() }
        )
    }
}
