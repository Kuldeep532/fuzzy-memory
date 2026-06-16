package com.nexuswavetech.nexusplus.features.aira

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.koin.androidx.compose.koinViewModel
import java.io.IOException
import java.util.concurrent.TimeUnit

data class AiraMessage(
    val id: Long = System.currentTimeMillis(),
    val role: String,
    val content: String,
    val isError: Boolean = false
)

data class AiraUiState(
    val messages: List<AiraMessage> = listOf(
        AiraMessage(
            role = "assistant",
            content = "Hi! I'm Aira, your AI assistant in Nexus Plus. I can help you with questions, analysis, summaries, and more. How can I assist you today?"
        )
    ),
    val inputText: String = "",
    val isLoading: Boolean = false
)

// ── Resilient endpoint configuration ─────────────────────────────────────────

private data class AiraEndpoint(val url: String, val buildBody: (JSONArray) -> String)

private val AIRA_ENDPOINTS = listOf(
    // Primary: Pollinations.ai OpenAI-large (free, no key required)
    AiraEndpoint("https://text.pollinations.ai/") { messages ->
        JSONObject().apply {
            put("messages", messages)
            put("model", "openai-large")
            put("seed", (1..999999).random())
        }.toString()
    },
    // Fallback 1: Pollinations.ai mistral model
    AiraEndpoint("https://text.pollinations.ai/") { messages ->
        JSONObject().apply {
            put("messages", messages)
            put("model", "mistral")
            put("seed", (1..999999).random())
        }.toString()
    },
    // Fallback 2: HuggingFace Inference API (Mistral-7B-Instruct — free tier)
    AiraEndpoint("https://api-inference.huggingface.co/models/mistralai/Mistral-7B-Instruct-v0.3") { messages ->
        val lastUser = (0 until messages.length())
            .map { messages.getJSONObject(it) }
            .lastOrNull { it.getString("role") == "user" }
            ?.getString("content") ?: ""
        JSONObject().apply {
            put("inputs", lastUser)
            put("parameters", JSONObject().apply {
                put("max_new_tokens", 512)
                put("return_full_text", false)
            })
        }.toString()
    },
    // Fallback 3: HuggingFace Zephyr-7B (extremely reliable free model)
    AiraEndpoint("https://api-inference.huggingface.co/models/HuggingFaceH4/zephyr-7b-beta") { messages ->
        val lastUser = (0 until messages.length())
            .map { messages.getJSONObject(it) }
            .lastOrNull { it.getString("role") == "user" }
            ?.getString("content") ?: ""
        JSONObject().apply {
            put("inputs", "<|system|>\nYou are Aira, a helpful AI assistant in Nexus Plus.\n<|user|>\n$lastUser\n<|assistant|>")
            put("parameters", JSONObject().apply {
                put("max_new_tokens", 512)
                put("return_full_text", false)
            })
        }.toString()
    }
)

// ── In-memory offline response cache ─────────────────────────────────────────

private object AiraOfflineCache {
    private val cache = LinkedHashMap<String, String>(64, 0.75f, true)
    private const val MAX_SIZE = 50

    fun put(prompt: String, response: String) {
        val key = prompt.trim().lowercase().take(120)
        if (cache.size >= MAX_SIZE) cache.entries.iterator().let { it.next(); it.remove() }
        cache[key] = response
    }

    fun get(prompt: String): String? = cache[prompt.trim().lowercase().take(120)]

    fun hasRecentMessages(): Boolean = cache.isNotEmpty()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class AiraViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AiraUiState())
    val uiState: StateFlow<AiraUiState> = _uiState.asStateFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val systemPrompt = """You are Aira, a helpful, friendly and intelligent AI assistant built into the Nexus Plus app by Nexus Wave Technologies. 
You are knowledgeable, concise, and always aim to give useful and accurate responses.
You can help with: general questions, writing, analysis, summarization, coding, math, translation, and productivity tasks.
Keep responses clear and well-structured. Use markdown formatting where helpful."""

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val input = _uiState.value.inputText.trim()
        if (input.isBlank() || _uiState.value.isLoading) return

        val userMsg = AiraMessage(role = "user", content = input)
        _uiState.update { it.copy(messages = it.messages + userMsg, inputText = "", isLoading = true) }

        viewModelScope.launch(Dispatchers.IO) {
            // Check offline cache first
            val cached = AiraOfflineCache.get(input)
            if (cached != null) {
                val aiMsg = AiraMessage(role = "assistant", content = cached)
                _uiState.update { it.copy(messages = it.messages + aiMsg, isLoading = false) }
                return@launch
            }

            val history = _uiState.value.messages.takeLast(10)
            val messagesJson = buildMessagesJson(history)

            val response = tryEndpointsWithRetry(messagesJson, input)
            _uiState.update { it.copy(messages = it.messages + response, isLoading = false) }
        }
    }

    private fun buildMessagesJson(history: List<AiraMessage>): JSONArray {
        val arr = JSONArray()
        arr.put(JSONObject().apply { put("role", "system"); put("content", systemPrompt) })
        history.forEach { msg ->
            if (!msg.isError) {
                arr.put(JSONObject().apply { put("role", msg.role); put("content", msg.content) })
            }
        }
        return arr
    }

    private suspend fun tryEndpointsWithRetry(messagesJson: JSONArray, userInput: String): AiraMessage {
        val errors = mutableListOf<String>()

        for ((index, endpoint) in AIRA_ENDPOINTS.withIndex()) {
            val attemptsForEndpoint = if (index == 0) 2 else 1
            repeat(attemptsForEndpoint) { attempt ->
                try {
                    val bodyStr = endpoint.buildBody(messagesJson)
                    val request = Request.Builder()
                        .url(endpoint.url)
                        .post(bodyStr.toRequestBody("application/json".toMediaType()))
                        .addHeader("Content-Type", "application/json")
                        .addHeader("User-Agent", "NexusPlus/1.2 Android")
                        .build()

                    val response = client.newCall(request).execute()
                    val responseText = response.body?.string()?.trim() ?: ""

                    if (response.isSuccessful && responseText.isNotBlank()) {
                        val parsed = parseResponse(responseText, index)
                        if (parsed.isNotBlank() && parsed.length > 5) {
                            AiraOfflineCache.put(userInput, parsed)
                            return AiraMessage(role = "assistant", content = parsed)
                        }
                    } else {
                        errors.add("Endpoint $index attempt $attempt: HTTP ${response.code}")
                        if (attempt == 0 && index == 0) delay(800)
                    }
                } catch (e: IOException) {
                    errors.add("Endpoint $index attempt $attempt: ${e.javaClass.simpleName}")
                    if (attempt == 0 && index == 0) delay(500)
                } catch (e: Exception) {
                    errors.add("Endpoint $index attempt $attempt: ${e.javaClass.simpleName}")
                }
            }
        }

        // All endpoints failed — check if we have any cached response for a similar query
        return if (AiraOfflineCache.hasRecentMessages()) {
            AiraMessage(
                role = "assistant",
                content = "I'm having trouble connecting right now. Please check your internet connection and try again. (Tried ${AIRA_ENDPOINTS.size} servers)",
                isError = true
            )
        } else {
            AiraMessage(
                role = "assistant",
                content = "I'm currently offline. Please check your connection and try again.",
                isError = true
            )
        }
    }

    private fun parseResponse(raw: String, endpointIndex: Int): String {
        return when {
            // HuggingFace array format: [{"generated_text": "..."}]
            endpointIndex >= 2 && raw.startsWith("[") -> {
                try {
                    val arr = JSONArray(raw)
                    if (arr.length() > 0) {
                        arr.getJSONObject(0).optString("generated_text", raw)
                            .removePrefix("<|assistant|>").trim()
                    } else raw
                } catch (_: Exception) { raw }
            }
            // JSON object with "text" key (some Pollinations responses)
            raw.startsWith("{") -> {
                try {
                    JSONObject(raw).optString("text", raw)
                } catch (_: Exception) { raw }
            }
            // Plain text (most Pollinations responses)
            else -> raw
        }
    }

    fun clearConversation() {
        _uiState.update {
            AiraUiState(
                messages = listOf(
                    AiraMessage(role = "assistant", content = "Conversation cleared. How can I help you?")
                )
            )
        }
    }
}

// ── UI ────────────────────────────────────────────────────────────────────────

@Composable
fun AiraAiScreen(
    onBack: () -> Unit,
    viewModel: AiraViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val view = LocalView.current

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading && uiState.messages.isNotEmpty()) {
            val last = uiState.messages.last()
            if (last.role == "assistant") {
                view.announceForAccessibility("Aira responded: ${last.content.take(100)}")
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(
            title = "Aira AI",
            onBack = onBack,
            actions = {
                IconButton(
                    onClick = { viewModel.clearConversation() },
                    modifier = Modifier.semantics { contentDescription = "Clear conversation" }
                ) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = null)
                }
            }
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.messages, key = { it.id }) { message ->
                ChatBubble(message = message)
            }

            if (uiState.isLoading) {
                item { AiraTypingIndicator() }
            }
        }

        Surface(tonalElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.inputText,
                    onValueChange = viewModel::onInputChanged,
                    placeholder = { Text("Ask Aira anything…") },
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Message input for Aira AI. Type your question here." },
                    maxLines = 5,
                    shape = RoundedCornerShape(24.dp)
                )
                FilledIconButton(
                    onClick = {
                        viewModel.sendMessage()
                        view.announceForAccessibility("Message sent")
                    },
                    enabled = uiState.inputText.isNotBlank() && !uiState.isLoading,
                    modifier = Modifier.size(52.dp).semantics { contentDescription = "Send message to Aira" }
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: AiraMessage) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = if (isUser) 16.dp else 4.dp,
                        topEnd = if (isUser) 4.dp else 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    )
                )
                .background(
                    if (isUser) MaterialTheme.colorScheme.primary
                    else if (message.isError) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .semantics {
                    contentDescription = if (isUser) "You: ${message.content}" else "Aira: ${message.content}"
                }
        ) {
            Text(
                text = message.content,
                color = if (isUser) MaterialTheme.colorScheme.onPrimary
                        else if (message.isError) MaterialTheme.colorScheme.onErrorContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (isUser) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun AiraTypingIndicator() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.semantics { contentDescription = "Aira is typing a response" }
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                Text(
                    "Aira is thinking…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
