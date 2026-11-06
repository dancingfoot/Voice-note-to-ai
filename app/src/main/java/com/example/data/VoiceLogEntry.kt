package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class LogType {
    NOTE,
    CHECKLIST
}

@Entity(tableName = "voice_logs")
data class VoiceLogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val rawTranscription: String,
    val type: LogType,
    val isCompleted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val tags: String = "", // e.g., "#quick, #voice"
    val endpointSource: String = "LOCAL_TRIGGER"
)
