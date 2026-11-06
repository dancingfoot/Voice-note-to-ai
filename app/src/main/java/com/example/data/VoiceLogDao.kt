package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceLogDao {
    @Query("SELECT * FROM voice_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<VoiceLogEntry>>

    @Query("SELECT * FROM voice_logs WHERE type = :type ORDER BY timestamp DESC")
    fun getLogsByType(type: LogType): Flow<List<VoiceLogEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(entry: VoiceLogEntry): Long

    @Update
    suspend fun updateLog(entry: VoiceLogEntry)

    @Delete
    suspend fun deleteLog(entry: VoiceLogEntry)

    @Query("DELETE FROM voice_logs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM voice_logs")
    suspend fun clearAll()
}
