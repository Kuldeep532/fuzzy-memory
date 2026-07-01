package com.nexuswavetech.nexusplus.features.nasa

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexuswavetech.nexusplus.platform.fetchHttp
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ── ViewModel ──────────────────────────────────────────────────

@Serializable
data class ApodResponse(
    val title: String = "",
    val explanation: String = "",
    val url: String = "",
    val hdurl: String = "",
    val media_type: String = "image",
    val date: String = "",
    val copyright: String? = null,
)

data class ApodUiState(
    val isLoading: Boolean = false,
    val data: ApodResponse? = null,
    val error: String? = null,
)

class NasaApodViewModel : ViewModel() {
    private val _state = MutableStateFlow(ApodUiState())
    val state: StateFlow<ApodUiState> = _state.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    init { loadToday() }

    fun loadToday() {
        viewModelScope.launch {
            _state.value = ApodUiState(isLoading = true)
            val result = fetchApod("today")
            _state.value = if (result.isSuccess) {
                ApodUiState(isLoading = false, data = result.getOrNull())
            } else {
                ApodUiState(isLoading = false, error = result.exceptionOrNull()?.message ?: "Failed to load")
            }
        }
    }

    fun loadRandom() {
        viewModelScope.launch {
            _state.value = ApodUiState(isLoading = true)
            val result = fetchApod("random")
            _state.value = if (result.isSuccess) {
                ApodUiState(isLoading = false, data = result.getOrNull())
            } else {
                ApodUiState(isLoading = false, error = result.exceptionOrNull()?.message ?: "Failed to load")
            }
        }
    }

    private suspend fun fetchApod(mode: String): Result<ApodResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val base = "https://api.nasa.gov/planetary/apod?api_key=DEMO_KEY"
            val url = if (mode == "random") "$base&count=1" else base
            val response = fetchHttp(url)
            if (mode == "random") {
                // Response is a JSON array for count=1
                val list = json.decodeFromString<List<ApodResponse>>(response)
                list.firstOrNull() ?: throw Exception("Empty response")
            } else {
                json.decodeFromString<ApodResponse>(response)
            }
        }
    }
}

// ── Screen ────────────────────────────────────────────────────

@Composable
fun NasaApodScreen(
    onBack: () -> Unit,
    viewModel: NasaApodViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            NexusTopBar(
                title = "NASA APOD",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { viewModel.loadToday() }) {
                        Icon(Icons.Filled.Today, contentDescription = "Today")
                    }
                    IconButton(onClick = { viewModel.loadRandom() }) {
                        Icon(Icons.Filled.Shuffle, contentDescription = "Random")
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when {
                state.isLoading -> LoadingView()
                state.error != null -> ErrorView(state.error!!, onRetry = { viewModel.loadToday() })
                state.data != null -> ApodContent(state.data!!)
            }
        }
    }
}

@Composable
private fun LoadingView() {
    Box(modifier = Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(8.dp))
            Text("Fetching space imagery…", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Filled.CloudOff, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text(message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        FilledTonalButton(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
private fun ApodContent(data: ApodResponse) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = data.title,
            style  = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = "Date: ${data.date}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (data.media_type == "image") {
                    Text("Image URL:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(data.url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    if (data.hdurl.isNotBlank() && data.hdurl != data.url) {
                        Spacer(Modifier.height(4.dp))
                        Text("HD URL:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(data.hdurl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    Text("Media Type: ${data.media_type}", style = MaterialTheme.typography.titleMedium)
                    Text(data.url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        if (data.explanation.isNotBlank()) {
            Text("Explanation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(data.explanation, style = MaterialTheme.typography.bodyMedium)
        }

        if (!data.copyright.isNullOrBlank()) {
            Text(
                text = "© ${data.copyright}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }

        Text(
            text = "Powered by NASA Open APIs",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End,
        )
    }
}
