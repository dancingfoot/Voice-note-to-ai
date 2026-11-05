package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SpeechManager(
    private val context: Context,
    private val onFinalResult: (String) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _audioRms = MutableStateFlow(0f)
    val audioRms: StateFlow<Float> = _audioRms.asStateFlow()

    private var isContinuousMode = false
    private var isExplicitlyStopped = true

    init {
        initializeRecognizer()
    }

    private fun initializeRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e("SpeechManager", "Speech recognition not available on this device")
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                }

                override fun onBeginningOfSpeech() {
                    _isListening.value = true
                }

                override fun onRmsChanged(rmsdB: Float) {
                    // Normalize RMS dB (typical range: -2 to 10 dB) to 0.0 .. 1.0
                    val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                    _audioRms.value = normalized
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _audioRms.value = 0f
                }

                override fun onError(error: Int) {
                    _audioRms.value = 0f
                    Log.d("SpeechManager", "SpeechRecognizer error: $error")
                    // If in continuous mode and not explicitly stopped, restart listening
                    if (isContinuousMode && !isExplicitlyStopped) {
                        restartListening()
                    } else {
                        _isListening.value = false
                    }
                }

                override fun onResults(results: Bundle?) {
                    _audioRms.value = 0f
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull().orEmpty()
                    _partialTranscript.value = ""
                    if (text.isNotBlank()) {
                        onFinalResult(text)
                    }

                    if (isContinuousMode && !isExplicitlyStopped) {
                        restartListening()
                    } else {
                        _isListening.value = false
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull().orEmpty()
                    if (text.isNotBlank()) {
                        _partialTranscript.value = text
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    fun startListening(continuous: Boolean) {
        isContinuousMode = continuous
        isExplicitlyStopped = false
        _partialTranscript.value = ""
        launchRecognizerIntent()
    }

    fun stopListening() {
        isExplicitlyStopped = true
        _audioRms.value = 0f
        _isListening.value = false
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.w("SpeechManager", "Error stopping recognizer", e)
        }
    }

    fun cancel() {
        isExplicitlyStopped = true
        _audioRms.value = 0f
        _isListening.value = false
        try {
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            Log.w("SpeechManager", "Error cancelling recognizer", e)
        }
    }

    private fun restartListening() {
        try {
            speechRecognizer?.cancel()
            launchRecognizerIntent()
        } catch (e: Exception) {
            Log.w("SpeechManager", "Error restarting recognizer", e)
        }
    }

    private fun launchRecognizerIntent() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try {
            speechRecognizer?.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            Log.e("SpeechManager", "Failed to start listening", e)
            _isListening.value = false
        }
    }

    fun destroy() {
        isExplicitlyStopped = true
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w("SpeechManager", "Error destroying speech recognizer", e)
        }
        speechRecognizer = null
    }
}
