package com.example.data.ai

import com.example.data.local.TaskEntity

sealed interface ParsedIntent {
    data class ListTasks(val isBengali: Boolean) : ParsedIntent
    data class AddTask(val title: String, val time: String) : ParsedIntent
    data class CreateNote(val title: String, val content: String) : ParsedIntent
    data object ShowWeather : ParsedIntent
    data object ShowScore : ParsedIntent
    data class GeneralAiQuery(val query: String) : ParsedIntent
}

object VoiceIntentParser {

    private val addTaskEnglish = Regex(
        "^\\s*(?:please\\s+)?(?:add|create|new|make)\\s+(?:a\\s+|new\\s+)?(?:task|todo|reminder)\\s*(?:to|for|:|-)?\\s*(.+)$",
        RegexOption.IGNORE_CASE
    )
    private val bengaliAddWords = Regex("যোগ করুন|যোগ করো|যোগ কর|যোগ|তৈরি করুন|তৈরি করো|তৈরি|নতুন|টাস্ক|কাজ")

    private val englishListPhrases = listOf(
        "my task", "my schedule", "my agenda", "my to-do", "my todo", "tasks today",
        "today's task", "todays task", "today's schedule", "todays schedule",
        "show tasks", "list tasks", "what do i have"
    )
    private val englishScorePhrases = listOf("my score", "my progress", "my productivity")
    private val bengaliTaskWords = listOf("টাস্ক", "কাজ", "শিডিউল")
    private val bengaliListWords = listOf("কি কি", "কী কী", "আজ", "আমার", "তালিকা", "দেখাও", "শিডিউল")
    private val bengaliAddTriggers = listOf("যোগ", "নতুন", "তৈরি")
    private val weatherWords = listOf("weather", "temperature", "আবহাওয়া", "তাপমাত্রা")
    private val writingWords = listOf("poem", "essay", "write", "story", "explain", "কবিতা", "লেখ", "গল্প", "ব্যাখ্যা")

    private fun containsAny(text: String, words: List<String>) = words.any { text.contains(it) }

    /**
     * Only short, clear commands are treated as app commands.
     * Everything else (long questions, writing requests...) goes to the AI.
     */
    fun parse(input: String): ParsedIntent {
        val text = input.trim()
        val clean = text.lowercase()
        val wordCount = clean.split(Regex("\\s+")).count { it.isNotEmpty() }

        // Add task (English): "add task buy milk"
        addTaskEnglish.find(text)?.let { match ->
            val title = match.groupValues[1].trim().trimEnd('.', '!', '?')
            if (title.isNotEmpty() && wordCount <= 14) return ParsedIntent.AddTask(title, "15:00")
        }

        // Add task (Bengali): "নতুন টাস্ক যোগ করো বাজার করা"
        if (wordCount <= 14 && containsAny(clean, listOf("টাস্ক", "কাজ")) && containsAny(clean, bengaliAddTriggers)) {
            val title = text.replace(bengaliAddWords, " ").replace(Regex("\\s+"), " ").trim().trimEnd('।', '.', '!', '?')
            return ParsedIntent.AddTask(title.ifEmpty { "নতুন টাস্ক" }, "15:00")
        }

        // List tasks
        if (wordCount <= 9) {
            if (containsAny(clean, bengaliTaskWords) && containsAny(clean, bengaliListWords)) {
                return ParsedIntent.ListTasks(isBengali = true)
            }
            if (containsAny(clean, englishListPhrases)) {
                return ParsedIntent.ListTasks(isBengali = false)
            }
        }

        // Weather: short questions only ("write a poem about the weather" goes to the AI)
        if (wordCount <= 8 && containsAny(clean, weatherWords) && !containsAny(clean, writingWords)) {
            return ParsedIntent.ShowWeather
        }

        // Progress
        if (wordCount <= 8 && (containsAny(clean, englishScorePhrases) || clean.contains("আমার স্কোর") || clean.contains("আমার অগ্রগতি"))) {
            return ParsedIntent.ShowScore
        }

        return ParsedIntent.GeneralAiQuery(input)
    }

    fun generateTaskSummaryResponse(tasks: List<TaskEntity>, isBengali: Boolean): String {
        val pending = tasks.filter { !it.isCompleted }
        val completed = tasks.filter { it.isCompleted }

        return if (isBengali) {
            val taskListText = tasks.joinToString(", ") { "${it.timeString} - ${it.title}${if (it.isCompleted) " (সম্পন্ন)" else ""}" }
            "আজ আপনার মোট ${tasks.size}টি শিডিউল আইটেম আছে (${completed.size}টি সম্পন্ন এবং ${pending.size}টি বাকি)। সময়সূচী: $taskListText।"
        } else {
            val taskListText = tasks.joinToString(", ") { "${it.timeString} - ${it.title}${if (it.isCompleted) " (done)" else ""}" }
            "Here's your agenda for today. You have ${tasks.size} items scheduled (${completed.size} completed, ${pending.size} pending): $taskListText."
        }
    }
}
