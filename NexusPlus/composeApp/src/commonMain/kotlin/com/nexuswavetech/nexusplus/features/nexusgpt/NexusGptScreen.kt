package com.nexuswavetech.nexusplus.features.nexusgpt

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexuswavetech.nexusplus.ai.GeminiRepository
import com.nexuswavetech.nexusplus.ai.GeminiMessage
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

// ── Models ────────────────────────────────────────────────────────

data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
)

data class NexusGptState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val inputText: String = "",
)

// ── ViewModel ──────────────────────────────────────────────────

class NexusGptViewModel(
    private val geminiRepo: GeminiRepository = koinInject(),
) : ViewModel() {

    private val _state = MutableStateFlow(NexusGptState())
    val state: StateFlow<NexusGptState> = _state.asStateFlow()

    fun onInputChange(text: String) { _state.value = _state.value.copy(inputText = text, error = null) }

    fun sendMessage() {
        val text = _state.value.inputText.trim()
        if (text.isBlank()) return

        val userMsg = ChatMessage(id = System.currentTimeMillis().toString(), text = text, isUser = true)
        val updated = _state.value.messages + userMsg
        _state.value = _state.value.copy(messages = updated, inputText = "", isLoading = true, error = null)

        viewModelScope.launch {
            try {
                val history = updated.map { GeminiMessage(text = it.text, isUser = it.isUser) }
                val response = geminiRepo.chat(history)
                val aiMsg = ChatMessage(
                    id = System.currentTimeMillis().toString(),
                    text = response ?: "No response from AI. Please try again.",
                    isUser = false,
                )
                _state.value = _state.value.copy(
                    messages = _state.value.messages + aiMsg,
                    isLoading = false,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Something went wrong",
                )
            }
        }
    }

    fun clearChat() { _state.value = _state.value.copy(messages = emptyList(), error = null) }
}

// ── Screen ────────────────────────────────────────────────────

@Composable
fun NexusGptScreen(
    onBack: () -> Unit,
) {
    val viewModel: NexusGptViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            NexusTopBar(
                title = "Nexus GPT",
                onBack = onBack,
                actions = {
                    if (state.messages.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearChat() }) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear chat")
                        }
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = state.inputText,
                        onValueChange = { viewModel.onInputChange(it) },
                        placeholder = { Text("Ask Nexus GPT anything…") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        trailingIcon = {
                            if (state.isLoading) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            }
                        },
                    )
                    FloatingActionButton(
                        onClick = { viewModel.sendMessage() },
                        modifier = Modifier.size(48.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                    ) {
                        Icon(Icons.Filled.Send, null, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
        ) {
            if (state.messages.isEmpty() && !state.isLoading) {
                EmptyChatView()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                ) {
                    items(state.messages, key = { it.id }) { msg ->
                        ChatBubble(msg)
                    }
                    if (state.isLoading) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                Text("Nexus GPT is thinking…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    AnimatedVisibility(visible = state.error != null) {
        state.error?.let { err ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.onInputChange("") }) { Text("Dismiss") }
                },
            ) { Text(err) }
        }
    }
}

@Composable
private fun EmptyChatView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.AutoMirrored.Filled.Chat, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        Spacer(Modifier.height(16.dp))
        Text(
            "Nexus GPT",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Advanced AI with context memory.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Ask anything — reasoning, coding, creative writing, analysis, and more.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
    }
}

@Composable
private fun ChatBubble(msg: ChatMessage) {
    val isUser = msg.isUser
    val bgColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = bgColor,
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(msg.text, color = textColor, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
