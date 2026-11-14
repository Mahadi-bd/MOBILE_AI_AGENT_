package com.example.data.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/** One message of the conversation sent to the AI. */
data class ChatTurn(val isUser: Boolean, val text: String)

/** Result of checking an API key in the settings. */
data class VerifyResult(val status: Status, val message: String) {
    enum class Status { OK, INVALID, OFFLINE, ERROR }
}

private data class AiResult(val text: String?, val error: String?, val code: Int = 0)
private data class Attempt(val model: String, val thinking: Boolean)

/**
 * Talks to the Gemini REST API using the API key that the user entered in the settings.
 * Model ids are discovered from the user's own key (models.list) so the app keeps working
 * when Google renames or retires models.
 */
class GeminiAiService(private val context: Context) {

    companion object {
        const val DEFAULT_FAST = "gemini-3.6-flash"
        const val DEFAULT_THINKING = "gemini-3.8-flash"
        private const val BASE = "https://generativelanguage.googleapis.com/v1beta"
        private const val PREF_FAST = "model_fast"
        private const val PREF_THINKING = "model_thinking"
        private val MODEL_REGEX = Regex("^gemini-(\\d+)(?:\\.(\\d+))?-flash$")
    }

    private val prefs = context.applicationContext.getSharedPreferences("life_easy_prefs", Context.MODE_PRIVATE)

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    fun fastModel(): String = prefs.getString(PREF_FAST, null) ?: DEFAULT_FAST
    fun thinkingModel(): String = prefs.getString(PREF_THINKING, null) ?: DEFAULT_THINKING

    // ------------------------------------------------------------------ chat

    fun streamAiResponse(
        turns: List<ChatTurn>,
        isThinkingMode: Boolean,
        personaRole: String,
        voiceTone: String,
        targetLanguage: String
    ): Flow<String> = flow {
        val apiKey = ApiKeyStore.load(context)

        val system = "You are the assistant inside the LIFE EASY app, a personal productivity helper. " +
            "Role: $personaRole. Tone: $voiceTone. Reply language: $targetLanguage " +
            "(if the user writes in another language, reply in the user's language). " +
            "Be helpful, concise and accurate. If you are not sure about something, say so. " +
            if (isThinkingMode) "Think carefully and give a well-structured, step-by-step answer." else ""

        val result = if (apiKey.isBlank()) {
            AiResult(
                null,
                "AI চালু করতে আপনার নিজের Gemini API key দিন: নিচের Persona ট্যাব → AI সেটিং। (Add your own Gemini API key in Persona → AI settings.)"
            )
        } else {
            generate(apiKey, system, normalize(turns), isThinkingMode, allowRefresh = true)
        }

        val output = result.text ?: ("⚠ " + (result.error ?: "Could not get a response."))
        val buffer = StringBuilder()
        for (word in output.split(" ")) {
            buffer.append(word).append(" ")
            emit(buffer.toString())
            delay(18)
        }
    }.flowOn(Dispatchers.IO)

    /** Gemini wants turns that start with the user and alternate roles. */
    private fun normalize(turns: List<ChatTurn>): List<ChatTurn> {
        val merged = ArrayList<ChatTurn>()
        for (t in turns) {
            if (t.text.isBlank()) continue
            val last = merged.lastOrNull()
            if (last != null && last.isUser == t.isUser) {
                merged[merged.size - 1] = ChatTurn(t.isUser, last.text + "\n" + t.text)
            } else {
                merged.add(t)
            }
        }
        while (merged.isNotEmpty() && !merged.first().isUser) merged.removeAt(0)
        return merged
    }

    private fun generate(
        apiKey: String,
        system: String,
        turns: List<ChatTurn>,
        thinkingMode: Boolean,
        allowRefresh: Boolean
    ): AiResult {
        if (turns.isEmpty()) return AiResult(null, "Empty message.")
        val attempts = if (thinkingMode) {
            listOf(
                Attempt(thinkingModel(), true),
                Attempt(thinkingModel(), false),
                Attempt(fastModel(), false)
            )
        } else {
            listOf(Attempt(fastModel(), false))
        }.distinct()

        var last = AiResult(null, "Unknown error")
        for (attempt in attempts) {
            last = callGemini(apiKey, attempt.model, system, turns, attempt.thinking)
            if (last.text != null) return last
            // wrong key / no permission: other models will not help
            if (last.code == 401 || last.code == 403 || last.code == 400 && last.error?.contains("API key") == true) break
            if (last.code == -1) break // offline
        }
        if (last.code == 404 && allowRefresh && refreshModels(apiKey)) {
            return generate(apiKey, system, turns, thinkingMode, allowRefresh = false)
        }
        return last
    }

    private fun callGemini(
        apiKey: String,
        model: String,
        system: String,
        turns: List<ChatTurn>,
        thinking: Boolean
    ): AiResult {
        return try {
            val contents = JSONArray()
            for (t in turns) {
                contents.put(
                    JSONObject().apply {
                        put("role", if (t.isUser) "user" else "model")
                        put("parts", JSONArray().put(JSONObject().put("text", t.text)))
                    }
                )
            }
            val body = JSONObject().apply {
                put("contents", contents)
                put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system))))
                if (thinking) {
                    put("generationConfig", JSONObject().put("thinkingConfig", JSONObject().put("thinkingLevel", "high")))
                }
            }
            val request = Request.Builder()
                .url("$BASE/models/$model:generateContent")
                .header("x-goog-api-key", apiKey)
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            http.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val apiMessage = try {
                        JSONObject(raw).optJSONObject("error")?.optString("message").orEmpty()
                    } catch (_: Exception) {
                        ""
                    }
                    return AiResult(null, friendlyError(response.code, apiMessage, model), response.code)
                }
                val parts = JSONObject(raw).optJSONArray("candidates")
                    ?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
                val sb = StringBuilder()
                if (parts != null) {
                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)
                        if (!part.optBoolean("thought", false)) sb.append(part.optString("text"))
                    }
                }
                val text = sb.toString().trim()
                if (text.isEmpty()) {
                    AiResult(null, "AI খালি উত্তর দিয়েছে (blocked or empty). অন্যভাবে প্রশ্ন করুন।", 200)
                } else {
                    AiResult(text, null, 200)
                }
            }
        } catch (_: IOException) {
            AiResult(null, "ইন্টারনেট সংযোগ নেই (No internet). সংযোগ দেখে আবার চেষ্টা করুন।", -1)
        } catch (_: Exception) {
            AiResult(null, "AI এর সাথে সংযোগে সমস্যা হয়েছে। আবার চেষ্টা করুন।", -2)
        }
    }

    private fun friendlyError(code: Int, apiMessage: String, model: String): String = when {
        code == 400 && apiMessage.contains("API key", ignoreCase = true) ->
            "API key সঠিক নয় (Invalid API key). Persona → AI সেটিং এ আবার দিন।"
        code == 401 || code == 403 ->
            "এই API key এর অনুমতি নেই (Permission denied). Google AI Studio থেকে নতুন key নিন।"
        code == 404 -> "AI মডেল \"$model\" পাওয়া যাচ্ছে না।"
        code == 429 -> "ফ্রি লিমিট শেষ বা খুব দ্রুত অনুরোধ (Quota exceeded). একটু পরে আবার চেষ্টা করুন।"
        code >= 500 -> "Google এর সার্ভারে সমস্যা। একটু পরে চেষ্টা করুন।"
        else -> "AI ত্রুটি ($code). ${apiMessage.take(120)}"
    }

    // ------------------------------------------------------------- settings

    /** Checks the key by listing models, then remembers the newest "flash" model for chat. */
    fun verifyKey(apiKey: String): VerifyResult {
        return try {
            val request = Request.Builder()
                .url("$BASE/models?pageSize=200")
                .header("x-goog-api-key", apiKey.trim())
                .build()
            http.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                when {
                    response.isSuccessful -> {
                        val chosen = pickModel(raw)
                        if (chosen != null) {
                            prefs.edit().putString(PREF_FAST, chosen).putString(PREF_THINKING, chosen).apply()
                            VerifyResult(VerifyResult.Status.OK, "✓ Key সঠিক। মডেল: $chosen")
                        } else {
                            VerifyResult(VerifyResult.Status.OK, "✓ Key সঠিক। (ডিফল্ট মডেল ব্যবহার হবে)")
                        }
                    }
                    response.code == 400 || response.code == 401 || response.code == 403 ->
                        VerifyResult(VerifyResult.Status.INVALID, "✗ Key সঠিক নয় বা অনুমতি নেই। আবার দেখে দিন।")
                    response.code == 429 ->
                        VerifyResult(VerifyResult.Status.OK, "✓ Key সংরক্ষিত। (এখন লিমিট পূর্ণ, একটু পরে চেষ্টা করুন)")
                    else -> VerifyResult(VerifyResult.Status.ERROR, "যাচাই করা যায়নি (${response.code})। পরে আবার চেষ্টা করুন।")
                }
            }
        } catch (_: IOException) {
            VerifyResult(VerifyResult.Status.OFFLINE, "ইন্টারনেট নেই। Key সংরক্ষিত হয়েছে, ইন্টারনেট পেলে যাচাই হবে।")
        } catch (_: Exception) {
            VerifyResult(VerifyResult.Status.ERROR, "যাচাই করা যায়নি।")
        }
    }

    private fun refreshModels(apiKey: String): Boolean {
        return try {
            val request = Request.Builder()
                .url("$BASE/models?pageSize=200")
                .header("x-goog-api-key", apiKey)
                .build()
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return false
                val chosen = pickModel(response.body?.string().orEmpty()) ?: return false
                prefs.edit().putString(PREF_FAST, chosen).putString(PREF_THINKING, chosen).apply()
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    /** Picks the newest "gemini-X.Y-flash" model that supports generateContent. */
    private fun pickModel(listJson: String): String? {
        val models = JSONObject(listJson).optJSONArray("models") ?: return null
        var best: String? = null
        var bestMajor = -1
        var bestMinor = -1
        for (i in 0 until models.length()) {
            val m = models.getJSONObject(i)
            val methods = m.optJSONArray("supportedGenerationMethods")
            var canGenerate = false
            if (methods != null) {
                for (k in 0 until methods.length()) if (methods.getString(k) == "generateContent") canGenerate = true
            }
            if (!canGenerate) continue
            val name = m.optString("name").removePrefix("models/")
            val match = MODEL_REGEX.find(name) ?: continue
            val major = match.groupValues[1].toInt()
            val minor = match.groupValues[2].ifEmpty { "0" }.toInt()
            if (major > bestMajor || (major == bestMajor && minor > bestMinor)) {
                best = name
                bestMajor = major
                bestMinor = minor
            }
        }
        return best
    }
}
