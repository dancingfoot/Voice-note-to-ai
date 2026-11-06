package com.example.voice

import com.example.data.LogType

sealed class VoiceCommandResult {
    data class LogItem(
        val type: LogType,
        val content: String,
        val rawTranscription: String,
        val tags: String
    ) : VoiceCommandResult()

    data class AiQuery(val query: String) : VoiceCommandResult()
}

object VoiceTriggerDetector {

    private val NOTE_TRIGGERS = listOf(
        "log note:",
        "take a note:",
        "take a note",
        "log a note:",
        "log note",
        "new note:",
        "note:",
        "note that",
        "make a note:",
        "make a note"
    )

    private val CHECKLIST_TRIGGERS = listOf(
        "add to checklist:",
        "add to checklist",
        "checklist item:",
        "checklist:",
        "add todo:",
        "new todo:",
        "todo:",
        "task:",
        "add task:",
        "remember to"
    )

    fun evaluate(transcription: String): VoiceCommandResult {
        val trimmed = transcription.trim()
        val lower = trimmed.lowercase()

        // 1. Check checklist triggers
        for (trigger in CHECKLIST_TRIGGERS) {
            if (lower.startsWith(trigger)) {
                val content = trimmed.substring(trigger.length).trim().removePrefix(":").trim()
                if (content.isNotBlank()) {
                    return VoiceCommandResult.LogItem(
                        type = LogType.CHECKLIST,
                        content = content.replaceFirstChar { it.uppercase() },
                        rawTranscription = trimmed,
                        tags = "#todo, #voice"
                    )
                }
            }
        }

        // 2. Check note triggers
        for (trigger in NOTE_TRIGGERS) {
            if (lower.startsWith(trigger)) {
                val content = trimmed.substring(trigger.length).trim().removePrefix(":").trim()
                if (content.isNotBlank()) {
                    return VoiceCommandResult.LogItem(
                        type = LogType.NOTE,
                        content = content.replaceFirstChar { it.uppercase() },
                        rawTranscription = trimmed,
                        tags = "#note, #voice-log"
                    )
                }
            }
        }

        // 3. Regular conversational query to AI endpoint
        return VoiceCommandResult.AiQuery(trimmed)
    }
}
