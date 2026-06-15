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

class AiraViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AiraUiState())
    val uiState: StateFlow<AiraUiState> = _uiState.asStateFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
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
        _uiState.update { it.copy(
            messages = it.messages + userMsg,
            inputText = "",
            isLoading = true
        )}

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val history = _uiState.value.messages.takeLast(10)
                val messagesJson = JSONArray()
                messagesJson.put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                history.forEach { msg ->
                    if (!msg.isError) {
                        messagesJson.put(JSONObject().apply {
                            put("role", msg.role)
                            put("content", msg.content)
                        })
                    }
                }

                val bodyJson = JSONObject().apply {
                    put("messages", messagesJson)
                    put("model", "openai")
                    put("seed", (1..999999).random())
                }

                val request = Request.Builder()
                    .url("https://text.pollinations.ai/")
                    .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                val responseText = response.body?.string()?.trim() ?: ""

                if (response.isSuccessful && responseText.isNotBlank()) {
                    val aiMsg = AiraMessage(role = "assistant", content = responseText)
                    _uiState.update { it.copy(messages = it.messages + aiMsg, isLoading = false) }
                } else {
                    val errMsg = AiraMessage(role = "assistant", content = "Sorry, I couldn't process that request. Please try again.", isError = true)
                    _uiState.update { it.copy(messages = it.messages + errMsg, isLoading = false) }
                }
            } catch (e: IOException) {
                val errMsg = AiraMessage(role = "assistant", content = "Network error. Please check your connection and try again.", isError = true)
                _uiState.update { it.copy(messages = it.messages + errMsg, isLoading = false) }
            } catch (e: Exception) {
                val errMsg = AiraMessage(role = "assistant", content = "Something went wrong. Please try again.", isError = true)
                _uiState.update { it.copy(messages = it.messages + errMsg, isLoading = false) }
            }
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

        // Chat messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.messages, key = { it.id }) { message ->
                ChatBubble(message = message)
            }

            if (uiState.isLoading) {
                item {
                    AiraTypingIndicator()
                }
            }
        }

        // Input area
        Surface(
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
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
                    modifier = Modifier
                        .size(52.dp)
                        .semantics { contentDescription = "Send message to Aira" }
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
