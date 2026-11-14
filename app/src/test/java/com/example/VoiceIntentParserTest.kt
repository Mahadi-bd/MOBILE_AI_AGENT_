package com.example

import com.example.data.ai.ParsedIntent
import com.example.data.ai.VoiceIntentParser
import com.example.data.local.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceIntentParserTest {

    @Test
    fun `test bengali task query intent`() {
        val intent = VoiceIntentParser.parse("আজ আমার কি কি টাস্ক আছে?")
        assertTrue(intent is ParsedIntent.ListTasks)
        assertEquals(true, (intent as ParsedIntent.ListTasks).isBengali)
    }

    @Test
    fun `test english task query intent`() {
        val intent = VoiceIntentParser.parse("What is my schedule today?")
        assertTrue(intent is ParsedIntent.ListTasks)
        assertEquals(false, (intent as ParsedIntent.ListTasks).isBengali)
    }

    @Test
    fun `test bengali task summary response generation`() {
        val sampleTasks = listOf(
            TaskEntity(title = "Team Stand-up", timeString = "09:00", isCompleted = true),
            TaskEntity(title = "Project Review", timeString = "10:30", isCompleted = false)
        )
        val response = VoiceIntentParser.generateTaskSummaryResponse(sampleTasks, isBengali = true)
        assertTrue(response.contains("আজ আপনার মোট 2টি শিডিউল আইটেম আছে"))
        assertTrue(response.contains("Team Stand-up (সম্পন্ন)"))
    }

    @Test
    fun `test add task english keeps original case`() {
        val intent = VoiceIntentParser.parse("Add task Buy Milk")
        assertTrue(intent is ParsedIntent.AddTask)
        assertEquals("Buy Milk", (intent as ParsedIntent.AddTask).title)
    }

    @Test
    fun `test add task bengali`() {
        val intent = VoiceIntentParser.parse("নতুন টাস্ক যোগ করো বাজার করা")
        assertTrue(intent is ParsedIntent.AddTask)
        assertEquals("বাজার করা", (intent as ParsedIntent.AddTask).title)
    }

    @Test
    fun `test general questions are not hijacked`() {
        assertTrue(VoiceIntentParser.parse("Explain task scheduling in operating systems") is ParsedIntent.GeneralAiQuery)
        assertTrue(VoiceIntentParser.parse("What is the cricket score today?") is ParsedIntent.GeneralAiQuery)
        assertTrue(VoiceIntentParser.parse("Write a poem about the weather") is ParsedIntent.GeneralAiQuery)
    }

    @Test
    fun `test weather and progress commands`() {
        assertTrue(VoiceIntentParser.parse("What's the weather today?") is ParsedIntent.ShowWeather)
        assertTrue(VoiceIntentParser.parse("Show my progress") is ParsedIntent.ShowScore)
    }
}
