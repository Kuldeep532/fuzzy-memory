package com.nexuswavetech.nexusplus.features.nasa

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
data class MarsPhoto(
    val id: Int,
    val sol: Int,
    val img_src: String,
    val earth_date: String,
    val camera: MarsCamera,
    val rover: MarsRoverInfo,
)

@Serializable
data class MarsCamera(
    val name: String,
    val full_name: String,
)

@Serializable
data class MarsRoverInfo(
    val name: String,
    val status: String,
    val landing_date: String,
    val launch_date: String,
)

@Serializable
data class MarsPhotosResponse(val photos: List<MarsPhoto>)

@Serializable
data class MarsManifestResponse(val photo_manifest: MarsManifest)

@Serializable
data class MarsManifest(
    val name: String,
    val landing_date: String,
    val launch_date: String,
    val status: String,
    val max_sol: Int,
    val max_date: String,
    val total_photos: Int,
)

data class MarsRoverState(
    val isLoading: Boolean = false,
    val photos: List<MarsPhoto> = emptyList(),
    val rover: String = "curiosity",
    val sol: Int = 1000,
    val error: String? = null,
)

class NasaMarsRoverViewModel : ViewModel() {
    private val _state = MutableStateFlow(MarsRoverState())
    val state: StateFlow<MarsRoverState> = _state.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    init { loadPhotos() }

    fun setRover(rover: String) { _state.value = _state.value.copy(rover = rover.lowercase()) }
    fun setSol(sol: Int) { _state.value = _state.value.copy(sol = sol.coerceAtLeast(0)) }

    fun loadPhotos() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = fetchPhotos(_state.value.rover, _state.value.sol)
            _state.value = if (result.isSuccess) {
                MarsRoverState(
                    isLoading = false,
                    photos = result.getOrNull() ?: emptyList(),
                    rover = _state.value.rover,
                    sol = _state.value.sol,
                )
            } else {
                _state.value.copy(isLoading = false, error = result.exceptionOrNull()?.message)
            }
        }
    }

    private suspend fun fetchPhotos(rover: String, sol: Int): Result<List<MarsPhoto>> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "https://api.nasa.gov/mars-photos/api/v1/rovers/$rover/photos?sol=$sol&api_key=DEMO_KEY"
            val response = fetchHttp(url)
            val root = json.decodeFromString<MarsPhotosResponse>(response)
            root.photos
        }
    }
}

// ── Screen ────────────────────────────────────────────────────

@Composable
fun NasaMarsRoverScreen(
    onBack: () -> Unit,
    viewModel: NasaMarsRoverViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
) {
    val state by viewModel.state.collectAsState()
    var solInput by remember { mutableStateOf(state.sol.toString()) }

    Scaffold(
        topBar = {
            NexusTopBar(
                title = "Mars Rover Photos",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { viewModel.loadPhotos() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
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
            // Controls
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Rover", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("curiosity", "perseverance", "opportunity", "spirit").forEach { rover ->
                        FilterChip(
                            selected = state.rover == rover,
                            onClick = { viewModel.setRover(rover); viewModel.loadPhotos() },
                            label = { Text(rover.replaceFirstChar { it.uppercase() }) },
                        )
                    }
                }

                OutlinedTextField(
                    value = solInput,
                    onValueChange = { solInput = it.filter { c -> c.isDigit() } },
                    label = { Text("Sol (Martian day)") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            solInput.toIntOrNull()?.let { viewModel.setSol(it); viewModel.loadPhotos() }
                        }) { Icon(Icons.Filled.Search, "Search") }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(8.dp))
                            Text("Fetching Mars photos…", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                state.error != null -> {
                    ErrorCard(state.error!!) { viewModel.loadPhotos() }
                }
                state.photos.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Filled.ImageNotSupported, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("No photos for Sol ${state.sol}", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                        Text("Try a different sol or rover", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> {
                    Text(
                        "${state.photos.size} photos from ${state.rover.replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    state.photos.take(30).forEach { photo ->
                        PhotoCard(photo)
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoCard(photo: MarsPhoto) {
    ElevatedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(photo.camera.full_name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text("Sol: ${photo.sol}  •  Earth date: ${photo.earth_date}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Image URL:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Text(photo.img_src, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            Text("Rover: ${photo.rover.name} (${photo.rover.status})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
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
