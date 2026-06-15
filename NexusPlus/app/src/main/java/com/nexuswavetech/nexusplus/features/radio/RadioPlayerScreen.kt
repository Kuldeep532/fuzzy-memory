package com.nexuswavetech.nexusplus.features.radio

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexuswavetech.nexusplus.core.HapticHelper
import com.nexuswavetech.nexusplus.core.SettingsRepository
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.*
import org.koin.compose.koinInject
import org.json.JSONArray
import org.koin.androidx.compose.koinViewModel
import java.io.IOException

// ── Data model ──────────────────────────────────────────────────────────────

data class RadioStation(
    val stationUuid: String,
    val name: String,
    val url: String,
    val country: String,
    val language: String,
    val tags: String,
    val votes: Int
)

// ── ViewModel ────────────────────────────────────────────────────────────────

data class RadioUiState(
    val stations: List<RadioStation> = emptyList(),
    val currentStation: RadioStation? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val error: String? = null
)

class RadioViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RadioUiState(isLoading = true))
    val uiState: StateFlow<RadioUiState> = _uiState.asStateFlow()

    private val client = OkHttpClient()
    private var exoPlayer: androidx.media3.exoplayer.ExoPlayer? = null

    init { fetchStations() }

    fun fetchStations(query: String = "top100") {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val url = "https://at1.api.radio-browser.info/json/stations/search" +
                "?limit=50&order=votes&reverse=true&hidebroken=true"
            val request = Request.Builder().url(url).build()
            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: "[]"
                val json = JSONArray(body)
                val stations = (0 until json.length()).map { i ->
                    val obj = json.getJSONObject(i)
                    RadioStation(
                        stationUuid = obj.optString("stationuuid"),
                        name = obj.optString("name"),
                        url = obj.optString("url_resolved"),
                        country = obj.optString("country"),
                        language = obj.optString("language"),
                        tags = obj.optString("tags"),
                        votes = obj.optInt("votes")
                    )
                }.filter { it.url.isNotBlank() }
                _uiState.update { it.copy(stations = stations, isLoading = false) }
            } catch (e: IOException) {
                _uiState.update { it.copy(error = "Network error: ${e.message}", isLoading = false) }
            }
        }
    }

    fun onStationSelected(station: RadioStation, context: android.content.Context) {
        exoPlayer?.release()
        val player = androidx.media3.exoplayer.ExoPlayer.Builder(context).build().also {
            val mediaItem = androidx.media3.common.MediaItem.fromUri(station.url)
            it.setMediaItem(mediaItem)
            it.prepare()
            it.play()
        }
        exoPlayer = player
        _uiState.update { it.copy(currentStation = station, isPlaying = true) }
    }

    fun onPlayPauseToggled() {
        val player = exoPlayer ?: return
        if (player.isPlaying) player.pause() else player.play()
        _uiState.update { it.copy(isPlaying = player.isPlaying) }
    }

    fun onSearchChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    override fun onCleared() {
        exoPlayer?.release()
        exoPlayer = null
        super.onCleared()
    }
}

// ── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun RadioPlayerScreen(
    onBack: () -> Unit,
    viewModel: RadioViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val view = LocalView.current
    val haptic   = koinInject<HapticHelper>()
    val settings = koinInject<SettingsRepository>()
    val hapticEnabled by settings.touchVibration.collectAsState(initial = true)

    val filteredStations = remember(uiState.stations, uiState.searchQuery) {
        uiState.stations.filter {
            uiState.searchQuery.isBlank() ||
            it.name.contains(uiState.searchQuery, ignoreCase = true) ||
            it.country.contains(uiState.searchQuery, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "Online Radio", onBack = onBack)

        // Now playing bar
        AnimatedVisibility(visible = uiState.currentStation != null) {
            uiState.currentStation?.let { station ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .semantics {
                            contentDescription = "Now playing: ${station.name} from ${station.country}. " +
                                if (uiState.isPlaying) "Playing. Tap play button to pause." else "Paused. Tap play button to resume."
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = station.name,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = station.country,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        IconButton(
                            onClick = {
                                haptic.click(view, hapticEnabled)
                                viewModel.onPlayPauseToggled()
                                val msg = if (uiState.isPlaying) "${station.name} paused" else "${station.name} playing"
                                view.announceForAccessibility(msg)
                            },
                            modifier = Modifier.semantics {
                                contentDescription = if (uiState.isPlaying) "Pause ${station.name}" else "Play ${station.name}"
                            }
                        ) {
                            Icon(
                                imageVector = if (uiState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // Search
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = viewModel::onSearchChanged,
            label = { Text("Search stations…") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .semantics { contentDescription = "Search radio stations by name or country." },
            singleLine = true
        )

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.semantics { contentDescription = "Loading radio stations." }
                )
            }
        } else if (uiState.error != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { viewModel.fetchStations() }) { Text("Retry") }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredStations, key = { it.stationUuid }) { station ->
                    StationListItem(
                        station = station,
                        isPlaying = uiState.currentStation?.stationUuid == station.stationUuid && uiState.isPlaying,
                        onClick = {
                            haptic.click(view, hapticEnabled)
                            view.announceForAccessibility("Playing ${station.name}")
                            viewModel.onStationSelected(station, context)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StationListItem(station: RadioStation, isPlaying: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "${station.name} radio station from ${station.country}." +
                    if (isPlaying) " Currently playing." else " Double tap to play."
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.GraphicEq else Icons.Filled.Radio,
                contentDescription = null,
                tint = if (isPlaying) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1
                )
                Text(
                    text = buildString {
                        if (station.country.isNotBlank()) append(station.country)
                        if (station.language.isNotBlank()) append(" • ${station.language}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            if (station.votes > 0) {
                Text(
                    text = "♥ ${station.votes}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}
