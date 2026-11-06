package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.LogType
import com.example.data.VoiceLogEntry
import com.example.data.VoiceLogRepository
import com.example.network.AiClient
import com.example.network.AiResponse
import com.example.network.EndpointConfig
import com.example.network.EndpointType
import com.example.voice.SpeechManager
import com.example.voice.TtsManager
import com.example.voice.VoiceCommandResult
import com.example.voice.VoiceTriggerDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UiNotification(
    val message: String,
    val isError: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

class VoiceInteractionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: VoiceLogRepository
    private val aiClient = AiClient()
    private val ttsManager = TtsManager(application)
    private var speechManager: SpeechManager? = null

    val allLogs: StateFlow<List<VoiceLogEntry>>

    private val _endpoints = MutableStateFlow<List<EndpointConfig>>(EndpointConfig.defaultPresets())
    val endpoints: StateFlow<List<EndpointConfig>> = _endpoints.asStateFlow()

    private val _activeEndpoint = MutableStateFlow<EndpointConfig>(EndpointConfig.defaultPresets()[1]) // Default to Local DeepSeek
    val activeEndpoint: StateFlow<EndpointConfig> = _activeEndpoint.asStateFlow()

    private val _isContinuousMode = MutableStateFlow(false)
    val isContinuousMode: StateFlow<Boolean> = _isContinuousMode.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    val isSpeaking: StateFlow<Boolean> = ttsManager.isSpeaking

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _audioRms = MutableStateFlow(0f)
    val audioRms: StateFlow<Float> = _audioRms.asStateFlow()

    private val _lastUserQuery = MutableStateFlow("")
    val lastUserQuery: StateFlow<String> = _lastUserQuery.asStateFlow()

    private val _lastAssistantResponse = MutableStateFlow("")
    val lastAssistantResponse: StateFlow<String> = _lastAssistantResponse.asStateFlow()

    private val _lastLatencyMs = MutableStateFlow<Long?>(null)
    val lastLatencyMs: StateFlow<Long?> = _lastLatencyMs.asStateFlow()

    private val _notification = MutableStateFlow<UiNotification?>(null)
    val notification: StateFlow<UiNotification?> = _notification.asStateFlow()

    private val _connectionStatus = MutableStateFlow<Pair<Boolean, String>?>(null)
    val connectionStatus: StateFlow<Pair<Boolean, String>?> = _connectionStatus.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = VoiceLogRepository(db.voiceLogDao())
        allLogs = repository.allLogs.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        initSpeechManager()
    }

    private fun initSpeechManager() {
        speechManager = SpeechManager(getApplication()) { finalResult ->
            handleFinalTranscription(finalResult)
        }

        viewModelScope.launch {
            speechManager?.isListening?.collect { listening ->
                _isListening.value = listening
            }
        }
        viewModelScope.launch {
            speechManager?.partialTranscript?.collect { text ->
                _partialTranscript.value = text
            }
        }
        viewModelScope.launch {
            speechManager?.audioRms?.collect { rms ->
                _audioRms.value = rms
            }
        }
    }

    fun onPttDown() {
        // Barge-in: interrupt any playing TTS
        ttsManager.stop()
        _isProcessing.value = false
        speechManager?.startListening(continuous = false)
    }

    fun onPttUp() {
        speechManager?.stopListening()
    }

    fun toggleContinuousMode() {
        ttsManager.stop()
        val nextMode = !_isContinuousMode.value
        _isContinuousMode.value = nextMode
        if (nextMode) {
            speechManager?.startListening(continuous = true)
            notify("Continuous streaming audio ON", isError = false)
        } else {
            speechManager?.stopListening()
            notify("Continuous streaming audio OFF", isError = false)
        }
    }

    fun stopSpeaking() {
        ttsManager.stop()
    }

    fun selectEndpoint(config: EndpointConfig) {
        _activeEndpoint.value = config
        _connectionStatus.value = null
        notify("Switched to ${config.name}")
    }

    fun updateEndpoint(updated: EndpointConfig) {
        val currentList = _endpoints.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == updated.id }
        if (index != -1) {
            currentList[index] = updated
        } else {
            currentList.add(updated)
        }
        _endpoints.value = currentList
        if (_activeEndpoint.value.id == updated.id) {
            _activeEndpoint.value = updated
        }
        notify("Saved endpoint configuration: ${updated.name}")
    }

    fun testActiveConnection() {
        val target = _activeEndpoint.value
        viewModelScope.launch {
            _connectionStatus.value = Pair(false, "Pinging ${target.baseUrl}...")
            val result = aiClient.testConnection(target)
            _connectionStatus.value = result
        }
    }

    private fun handleFinalTranscription(text: String) {
        val clean = text.trim()
        if (clean.isBlank()) return
        _lastUserQuery.value = clean

        when (val command = VoiceTriggerDetector.evaluate(clean)) {
            is VoiceCommandResult.LogItem -> {
                // Verbally triggered local note or checklist item!
                // Per user specification: Stored strictly locally in database, zero network send
                viewModelScope.launch {
                    val entry = VoiceLogEntry(
                        content = command.content,
                        rawTranscription = command.rawTranscription,
                        type = command.type,
                        tags = command.tags,
                        endpointSource = _activeEndpoint.value.name
                    )
                    repository.insert(entry)

                    val label = if (command.type == LogType.CHECKLIST) "Checklist Item" else "Voice Note"
                    notify("Logged $label: \"${command.content}\"")
                    _lastAssistantResponse.value = "Saved local $label: \"${command.content}\""

                    // Speak brief local audible confirmation
                    ttsManager.speak("Saved $label: ${command.content}")
                }
            }
            is VoiceCommandResult.AiQuery -> {
                // Regular query: send to selected AI endpoint
                if (_activeEndpoint.value.type == EndpointType.LOCAL_NOTES_ONLY) {
                    // In Quick Notes mode, save as a note directly
                    viewModelScope.launch {
                        val entry = VoiceLogEntry(
                            content = command.query,
                            rawTranscription = clean,
                            type = LogType.NOTE,
                            tags = "#quick-capture, #local",
                            endpointSource = "Local Notes"
                        )
                        repository.insert(entry)
                        notify("Captured local note: \"${command.query}\"")
                        _lastAssistantResponse.value = "Captured note: \"${command.query}\""
                        ttsManager.speak("Note recorded")
                    }
                    return
                }

                // Query external or local AI endpoint
                viewModelScope.launch {
                    _isProcessing.value = true
                    _lastAssistantResponse.value = "Processing query..."
                    val response = aiClient.query(_activeEndpoint.value, command.query)
                    _isProcessing.value = false

                    when (response) {
                        is AiResponse.Success -> {
                            _lastLatencyMs.value = response.latencyMs
                            _lastAssistantResponse.value = response.text
                            // Audible reply via TTS
                            ttsManager.speak(response.text)
                        }
                        is AiResponse.Error -> {
                            _lastLatencyMs.value = null
                            _lastAssistantResponse.value = "Error: ${response.message}"
                            notify(response.message, isError = true)
                            ttsManager.speak("Endpoint returned an error: ${response.message}")
                        }
                    }
                }
            }
        }
    }

    // Manual / Text query fallback or testing
    fun sendTextQuery(text: String) {
        handleFinalTranscription(text)
    }

    fun toggleLogCompleted(entry: VoiceLogEntry) {
        viewModelScope.launch {
            repository.update(entry.copy(isCompleted = !entry.isCompleted))
        }
    }

    fun deleteLog(entry: VoiceLogEntry) {
        viewModelScope.launch {
            repository.delete(entry)
            notify("Deleted item")
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.clearAll()
            notify("Cleared all notes and checklist items")
        }
    }

    fun notify(message: String, isError: Boolean = false) {
        _notification.value = UiNotification(message, isError)
    }

    fun clearNotification() {
        _notification.value = null
    }

    override fun onCleared() {
        super.onCleared()
        speechManager?.destroy()
        ttsManager.shutdown()
    }
}
