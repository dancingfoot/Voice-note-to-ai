package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class VoicePreset(val displayName: String, val description: String) {
    WARM_AOEDE("Aoede / Kore (Warm)", "Warm, natural feminine tone (Gemini style)"),
    CONVERSATIONAL_PUCK("Puck (Conversational)", "Clear, engaging tone"),
    DEEP_CHARON("Charon (Deep)", "Deep, articulate tone"),
    SYSTEM_DEFAULT("System Default", "Device default synthesizer")
}

data class VoiceProfile(
    val preset: VoicePreset = VoicePreset.WARM_AOEDE,
    val speedRate: Float = 1.05f,
    val pitch: Float = 1.0f
)

class TtsManager(private val context: Context) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _currentProfile = MutableStateFlow(VoiceProfile())
    val currentProfile: StateFlow<VoiceProfile> = _currentProfile.asStateFlow()

    init {
        initializeTtsEngine()
    }

    private fun initializeTtsEngine() {
        // Attempt to prioritize Google TTS engine package
        val googleTtsPackage = "com.google.android.tts"
        tts = TextToSpeech(context.applicationContext, { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.let { engine ->
                    val result = engine.setLanguage(Locale.US)
                    if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                        isInitialized = true
                        applyProfile(_currentProfile.value)
                    }
                }
            } else {
                Log.w("TtsManager", "Google TTS init failed with status: $status, falling back to default")
                fallbackToDefaultTts()
            }
        }, googleTtsPackage)

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
            }

            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
            }
        })
    }

    private fun fallbackToDefaultTts() {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.let { engine ->
                    engine.setLanguage(Locale.US)
                    isInitialized = true
                    applyProfile(_currentProfile.value)
                }
            }
        }
    }

    fun applyProfile(profile: VoiceProfile) {
        _currentProfile.value = profile
        if (!isInitialized) return
        val engine = tts ?: return

        engine.setSpeechRate(profile.speedRate)
        engine.setPitch(profile.pitch)

        if (profile.preset == VoicePreset.SYSTEM_DEFAULT) {
            return
        }

        try {
            val availableVoices = engine.voices
            if (!availableVoices.isNullOrEmpty()) {
                val matchedVoice = findMatchingVoice(availableVoices, profile.preset)
                if (matchedVoice != null) {
                    engine.voice = matchedVoice
                }
            }
        } catch (e: Exception) {
            Log.w("TtsManager", "Could not set custom voice: ${e.message}")
        }
    }

    private fun findMatchingVoice(voices: Set<Voice>, preset: VoicePreset): Voice? {
        val usVoices = voices.filter { it.locale == Locale.US || it.locale.language == "en" }

        return when (preset) {
            VoicePreset.WARM_AOEDE -> {
                // Look for Google's warm neural voices (e.g. sfg, female, neural)
                usVoices.find { it.name.contains("sfg", ignoreCase = true) }
                    ?: usVoices.find { it.name.contains("female", ignoreCase = true) && it.name.contains("network", ignoreCase = true) }
                    ?: usVoices.find { it.name.contains("neural", ignoreCase = true) }
                    ?: usVoices.find { it.name.contains("female", ignoreCase = true) }
            }
            VoicePreset.CONVERSATIONAL_PUCK -> {
                // Look for Google's conversational male voices (e.g. iol, network)
                usVoices.find { it.name.contains("iol", ignoreCase = true) }
                    ?: usVoices.find { it.name.contains("male", ignoreCase = true) && it.name.contains("network", ignoreCase = true) }
                    ?: usVoices.find { it.name.contains("male", ignoreCase = true) }
            }
            VoicePreset.DEEP_CHARON -> {
                // Look for deeper tone male voices (e.g. tpd, tpc)
                usVoices.find { it.name.contains("tpd", ignoreCase = true) }
                    ?: usVoices.find { it.name.contains("tpc", ignoreCase = true) }
                    ?: usVoices.find { it.name.contains("male", ignoreCase = true) }
            }
            VoicePreset.SYSTEM_DEFAULT -> null
        }
    }

    fun testVoice() {
        speak("Hello! I am ready to assist with your queries and notes.")
    }

    fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (!isInitialized || text.isBlank()) return
        stop() // Barge-in: cancel prior speech
        val utteranceId = "utterance_${System.currentTimeMillis()}"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
