package com.example.data

import kotlinx.coroutines.flow.Flow

class VoiceLogRepository(private val dao: VoiceLogDao) {
    val allLogs: Flow<List<VoiceLogEntry>> = dao.getAllLogs()

    fun getLogsByType(type: LogType): Flow<List<VoiceLogEntry>> = dao.getLogsByType(type)

    suspend fun insert(entry: VoiceLogEntry): Long = dao.insertLog(entry)

    suspend fun update(entry: VoiceLogEntry) = dao.updateLog(entry)

    suspend fun delete(entry: VoiceLogEntry) = dao.deleteLog(entry)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun clearAll() = dao.clearAll()
}
