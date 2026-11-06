package com.example

import com.example.data.LogType
import com.example.voice.VoiceCommandResult
import com.example.voice.VoiceTriggerDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceTriggerDetectorTest {

    @Test
    fun testNoteTriggerEvaluation() {
        val input = "Note: buy coffee for the morning"
        val result = VoiceTriggerDetector.evaluate(input)
        assertTrue(result is VoiceCommandResult.LogItem)
        val log = result as VoiceCommandResult.LogItem
        assertEquals(LogType.NOTE, log.type)
        assertEquals("Buy coffee for the morning", log.content)
    }

    @Test
    fun testChecklistTriggerEvaluation() {
        val input = "Todo: update docker compose file"
        val result = VoiceTriggerDetector.evaluate(input)
        assertTrue(result is VoiceCommandResult.LogItem)
        val log = result as VoiceCommandResult.LogItem
        assertEquals(LogType.CHECKLIST, log.type)
        assertEquals("Update docker compose file", log.content)
    }

    @Test
    fun testAddToChecklistTriggerEvaluation() {
        val input = "Add to checklist: review DeepSeek container logs"
        val result = VoiceTriggerDetector.evaluate(input)
        assertTrue(result is VoiceCommandResult.LogItem)
        val log = result as VoiceCommandResult.LogItem
        assertEquals(LogType.CHECKLIST, log.type)
        assertEquals("Review DeepSeek container logs", log.content)
    }

    @Test
    fun testRegularAiQueryEvaluation() {
        val input = "What is the capital of France?"
        val result = VoiceTriggerDetector.evaluate(input)
        assertTrue(result is VoiceCommandResult.AiQuery)
        val query = result as VoiceCommandResult.AiQuery
        assertEquals("What is the capital of France?", query.query)
    }
}
