package com.nexuswavetech.nexusplus.ai

import android.graphics.Bitmap
import android.util.Base64
import com.nexuswavetech.nexusplus.core.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * Nexus Plus — Gemini AI Repository
 *
 * Provides a clean API for calling Google Gemini Flash / Pro with:
 *  - Text generation
 *  - Vision (image + text) support
 *  - Streaming-simulation (buffered response returned at once)
 *  - Retry logic with exponential back-off
 *  - Secure key retrieval from [SettingsRepository]
 *
 * Build Verification Pending (requires a valid Gemini API key to execute live calls).
 */
class GeminiRepository(private val settings: SettingsRepository) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Returns the effective Gemini API key:
     *  1. User-entered key stored in DataStore (highest priority)
     *  2. Key baked into BuildConfig at build time via GEMINI_API_KEY env var (GitHub Secrets)
     *  3. Empty string → Gemini unavailable, fallback to free endpoints
     */
    private suspend fun effectiveApiKey(): String {
        val userKey = settings.geminiApiKey.first().trim()
        if (userKey.isNotBlank()) return userKey
        return try {
            val clazz = Class.forName("com.nexuswavetech.nexusplus.BuildConfig")
            val field = clazz.getField("GEMINI_API_KEY")
            (field.get(null) as? String)?.trim()?.takeIf { it.isNotBlank() } ?: ""
        } catch (_: Exception) { "" }
    }

    /**
     * Returns true when a Gemini API key is available (user-entered or from build-time secret).
     */
    suspend fun isAvailable(): Boolean = effectiveApiKey().isNotBlank()

    /**
     * Sends a multi-turn conversation to Gemini and returns the text reply.
     * The [history] list alternates user / model roles (oldest first).
     * Returns null when no API key is set.
     * Throws [GeminiException] on API or network errors after all retries.
     */
    suspend fun chat(
        history: List<GeminiMessage>,
        systemPrompt: String = AIRA_SYSTEM_PROMPT,
    ): String? = withContext(Dispatchers.IO) {
        val key   = effectiveApiKey()
        val model = settings.geminiModel.first().trim()
        if (key.isBlank()) return@withContext null

        val url = buildUrl(model, key)
        val body = buildChatBody(history, systemPrompt)
        callWithRetry(url, body)
    }

    /**
     * Vision call: sends [prompt] together with [image] to Gemini.
     * Returns null when no API key is set.
     */
    suspend fun vision(prompt: String, image: Bitmap): String? = withContext(Dispatchers.IO) {
        val key   = effectiveApiKey()
        val model = settings.geminiModel.first().trim()
        if (key.isBlank()) return@withContext null

        val url  = buildUrl(model, key)
        val body = buildVisionBody(prompt, image)
        callWithRetry(url, body)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun buildUrl(model: String, key: String) =
        "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$key"

    private fun buildChatBody(history: List<GeminiMessage>, systemPrompt: String): String {
        val contents = JSONArray()
        history.forEach { msg ->
            val parts = JSONArray().apply {
                put(JSONObject().apply { put("text", msg.text) })
            }
            contents.put(JSONObject().apply {
                put("role", if (msg.isUser) "user" else "model")
                put("parts", parts)
            })
        }
        return JSONObject().apply {
            put("system_instruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemPrompt) })
                })
            })
            put("contents", contents)
            put("generationConfig", buildGenerationConfig())
            put("safetySettings", buildSafetySettings())
        }.toString()
    }

    private fun buildVisionBody(prompt: String, image: Bitmap): String {
        val bos = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.JPEG, 85, bos)
        val b64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)

        val parts = JSONArray().apply {
            put(JSONObject().apply { put("text", prompt) })
            put(JSONObject().apply {
                put("inline_data", JSONObject().apply {
                    put("mime_type", "image/jpeg")
                    put("data", b64)
                })
            })
        }
        val contents = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "user")
                put("parts", parts)
            })
        }
        return JSONObject().apply {
            put("contents", contents)
            put("generationConfig", buildGenerationConfig())
            put("safetySettings", buildSafetySettings())
        }.toString()
    }

    private fun buildGenerationConfig() = JSONObject().apply {
        put("temperature", 0.7)
        put("topK", 40)
        put("topP", 0.95)
        put("maxOutputTokens", 2048)
    }

    private fun buildSafetySettings() = JSONArray().apply {
        listOf("HARM_CATEGORY_HARASSMENT", "HARM_CATEGORY_HATE_SPEECH",
               "HARM_CATEGORY_SEXUALLY_EXPLICIT", "HARM_CATEGORY_DANGEROUS_CONTENT").forEach { cat ->
            put(JSONObject().apply {
                put("category", cat)
                put("threshold", "BLOCK_MEDIUM_AND_ABOVE")
            })
        }
    }

    private suspend fun callWithRetry(url: String, bodyStr: String): String {
        var lastError: Exception? = null
        val delays = longArrayOf(0L, 1000L, 3000L)
        delays.forEach { delayMs ->
            if (delayMs > 0) kotlinx.coroutines.delay(delayMs)
            try {
                val request = Request.Builder()
                    .url(url)
                    .post(bodyStr.toRequestBody("application/json".toMediaType()))
                    .addHeader("Content-Type", "application/json")
                    .addHeader("User-Agent", "NexusPlus/1.2 Android")
                    .build()
                val response = client.newCall(request).execute()
                val raw = response.body?.string()?.trim() ?: ""
                if (response.isSuccessful && raw.isNotBlank()) {
                    return parseGeminiResponse(raw)
                } else {
                    val errMsg = try { JSONObject(raw).optString("error", raw) } catch (_: Exception) { raw }
                    lastError = GeminiException("HTTP ${response.code}: $errMsg")
                }
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: GeminiException("Unknown error")
    }

    private fun parseGeminiResponse(raw: String): String {
        return try {
            val obj = JSONObject(raw)
            val candidates = obj.getJSONArray("candidates")
            if (candidates.length() > 0) {
                val content = candidates.getJSONObject(0).getJSONObject("content")
                val parts = content.getJSONArray("parts")
                if (parts.length() > 0) parts.getJSONObject(0).getString("text") else raw
            } else {
                throw GeminiException("No candidates in response")
            }
        } catch (e: Exception) {
            throw GeminiException("Parse error: ${e.message}", e)
        }
    }

    // ── Constants ─────────────────────────────────────────────────────────────

    companion object {
        const val AIRA_SYSTEM_PROMPT = """You are Aira, a helpful, friendly and intelligent AI assistant built into the Nexus Plus app by Nexus Wave Technologies.
You are knowledgeable, concise, and always aim to give useful and accurate responses.
You can help with: general questions, writing, analysis, summarization, coding, math, translation, and productivity tasks.
Keep responses clear and well-structured. Use markdown formatting where helpful."""
    }
}

data class GeminiMessage(val text: String, val isUser: Boolean)

class GeminiException(message: String, cause: Throwable? = null) : Exception(message, cause)
