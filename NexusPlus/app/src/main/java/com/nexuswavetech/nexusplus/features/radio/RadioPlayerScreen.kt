package com.nexuswavetech.nexusplus.features.radio

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import org.json.JSONArray
import org.koin.compose.koinInject
import org.koin.androidx.compose.koinViewModel
import java.io.IOException

data class RadioStation(
    val stationUuid: String,
    val name: String,
    val url: String,
    val country: String,
    val language: String,
    val tags: String,
    val votes: Int,
    val isFavorite: Boolean = false
)

enum class IndianRadioCategory(val label: String, val tag: String) {
    ALL("All 🇮🇳", ""),
    HINDI("Hindi", "hindi"),
    NEWS("News", "news"),
    DEVOTIONAL("Devotional", "devotional"),
    CLASSICAL("Classical", "classical"),
    BOLLYWOOD("Bollywood", "bollywood"),
    REGIONAL("Regional", "regional"),
    ENGLISH("English", "english"),
    PUNJABI("Punjabi", "punjabi"),
    TAMIL("Tamil", "tamil")
}

data class RadioUiState(
    val stations: List<RadioStation> = emptyList(),
    val currentStation: RadioStation? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val selectedCategory: IndianRadioCategory = IndianRadioCategory.ALL,
    val error: String? = null,
    val favoriteIds: Set<String> = emptySet()
)

class RadioViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RadioUiState(isLoading = true))
    val uiState: StateFlow<RadioUiState> = _uiState.asStateFlow()

    private val client = OkHttpClient()
    private var exoPlayer: androidx.media3.exoplayer.ExoPlayer? = null

    init { fetchIndianStations() }

    fun fetchIndianStations(category: IndianRadioCategory = _uiState.value.selectedCategory) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, error = null, selectedCategory = category) }
            try {
                val tagParam = if (category.tag.isNotBlank()) "&tag=${category.tag}" else ""
                val url = "https://at1.api.radio-browser.info/json/stations/search" +
                    "?limit=100&countrycode=IN&order=votes&reverse=true&hidebroken=true$tagParam"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: "[]"
                val json = JSONArray(body)
                val favorites = _uiState.value.favoriteIds
                val stations = (0 until json.length()).map { i ->
                    val obj = json.getJSONObject(i)
                    RadioStation(
                        stationUuid = obj.optString("stationuuid"),
                        name        = obj.optString("name").trim(),
                        url         = obj.optString("url_resolved"),
                        country     = obj.optString("country"),
                        language    = obj.optString("language"),
                        tags        = obj.optString("tags"),
                        votes       = obj.optInt("votes"),
                        isFavorite  = obj.optString("stationuuid") in favorites
                    )
                }.filter { it.url.isNotBlank() && it.name.isNotBlank() }
                _uiState.update { it.copy(stations = stations, isLoading = false) }
            } catch (e: IOException) {
                _uiState.update { it.copy(error = "Could not load Indian radio stations. Check your connection.", isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed: ${e.message}", isLoading = false) }
            }
        }
    }

    fun onStationSelected(station: RadioStation, context: android.content.Context) {
        if (exoPlayer == null) {
            exoPlayer = androidx.media3.exoplayer.ExoPlayer.Builder(context).build().also { player ->
                player.addListener(object : androidx.media3.common.Player.Listener {
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        _uiState.update { it.copy(
                            error = "Stream unavailable. Please try another station.",
                            isPlaying = false
                        )}
                    }
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _uiState.update { it.copy(isPlaying = isPlaying) }
                    }
                })
            }
        }
        val player = exoPlayer!!
        player.stop()
        player.clearMediaItems()
        player.setMediaItem(androidx.media3.common.MediaItem.fromUri(station.url))
        player.prepare()
        player.play()
        _uiState.update { it.copy(currentStation = station, isPlaying = true, error = null) }
    }

    fun onPlayPauseToggled() {
        val player = exoPlayer ?: return
        if (player.isPlaying) player.pause() else player.play()
        _uiState.update { it.copy(isPlaying = player.isPlaying) }
    }

    fun onSearchChanged(query: String) = _uiState.update { it.copy(searchQuery = query) }

    fun toggleFavorite(stationUuid: String) {
        val current = _uiState.value.favoriteIds.toMutableSet()
        if (stationUuid in current) current.remove(stationUuid) else current.add(stationUuid)
        val updated = _uiState.value.stations.map { s ->
            s.copy(isFavorite = s.stationUuid in current)
        }
        _uiState.update { it.copy(favoriteIds = current, stations = updated) }
    }

    override fun onCleared() {
        exoPlayer?.release()
        exoPlayer = null
        super.onCleared()
    }
}

@Composable
fun RadioPlayerScreen(
    onBack: () -> Unit,
    viewModel: RadioViewModel = koinViewModel()
) {
    val uiState  by viewModel.uiState.collectAsState()
    val context  = LocalContext.current
    val view     = LocalView.current
    val haptic   = koinInject<HapticHelper>()
    val settings = koinInject<SettingsRepository>()
    val hapticEnabled by settings.touchVibration.collectAsState(initial = true)

    var showFavoritesOnly by remember { mutableStateOf(false) }

    val displayedStations = remember(uiState.stations, uiState.searchQuery, showFavoritesOnly) {
        uiState.stations.filter { station ->
            val matchesSearch = uiState.searchQuery.isBlank() ||
                station.name.contains(uiState.searchQuery, ignoreCase = true) ||
                station.language.contains(uiState.searchQuery, ignoreCase = true) ||
                station.tags.contains(uiState.searchQuery, ignoreCase = true)
            val matchesFav = !showFavoritesOnly || station.isFavorite
            matchesSearch && matchesFav
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(
            title = "Indian Radio",
            onBack = onBack,
            actions = {
                IconButton(
                    onClick = {
                        haptic.click(view, hapticEnabled)
                        showFavoritesOnly = !showFavoritesOnly
                    },
                    modifier = Modifier.semantics {
                        contentDescription = if (showFavoritesOnly) "Show all stations" else "Show favorites only"
                    }
                ) {
                    Icon(
                        if (showFavoritesOnly) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = if (showFavoritesOnly) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = { viewModel.fetchIndianStations(uiState.selectedCategory) },
                    modifier = Modifier.semantics { contentDescription = "Refresh stations" }
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                }
            }
        )

        // Now playing bar
        AnimatedVisibility(visible = uiState.currentStation != null) {
            uiState.currentStation?.let { station ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .semantics {
                            contentDescription = "Now playing: ${station.name}. ${if (uiState.isPlaying) "Playing." else "Paused."}"
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(Icons.Filled.Radio, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(station.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 1)
                            if (station.language.isNotBlank()) Text(station.language, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        }
                        IconButton(
                            onClick = {
                                haptic.click(view, hapticEnabled)
                                viewModel.onPlayPauseToggled()
                                view.announceForAccessibility(if (uiState.isPlaying) "Paused ${station.name}" else "Playing ${station.name}")
                            },
                            modifier = Modifier.semantics { contentDescription = if (uiState.isPlaying) "Pause" else "Play" }
                        ) {
                            Icon(
                                if (uiState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // Category filter chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.semantics { contentDescription = "Radio category filter" }
        ) {
            items(IndianRadioCategory.entries.toList()) { category ->
                FilterChip(
                    selected = uiState.selectedCategory == category,
                    onClick  = {
                        haptic.click(view, hapticEnabled)
                        viewModel.fetchIndianStations(category)
                    },
                    label = { Text(category.label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        // Search
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = viewModel::onSearchChanged,
            label = { Text("Search Indian stations…") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            trailingIcon = {
                if (uiState.searchQuery.isNotBlank()) {
                    IconButton(onClick = { viewModel.onSearchChanged("") }) {
                        Icon(Icons.Filled.Clear, "Clear search")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .semantics { contentDescription = "Search Indian radio stations by name, language or genre." },
            singleLine = true
        )

        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = "Loading Indian radio stations." })
                    Text("Loading Indian stations…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            uiState.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(Icons.Filled.WifiOff, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                    Text(uiState.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = { viewModel.fetchIndianStations() }) { Text("Retry") }
                }
            }
            displayedStations.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (showFavoritesOnly) "No favorites yet. Tap ♥ on a station to add." else "No stations found.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text(
                        "${displayedStations.size} station${if (displayedStations.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(displayedStations, key = { it.stationUuid }) { station ->
                        val isActive = uiState.currentStation?.stationUuid == station.stationUuid
                        Card(
                            onClick = {
                                haptic.click(view, hapticEnabled)
                                view.announceForAccessibility("Playing ${station.name}")
                                viewModel.onStationSelected(station, context)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics {
                                    contentDescription = "${station.name}. ${station.language.ifBlank { "Indian" }} language.${if (isActive) " Currently playing" else ""}"
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isActive) MaterialTheme.colorScheme.secondaryContainer
                                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    if (isActive && uiState.isPlaying) Icons.Filled.GraphicEq else Icons.Filled.Radio,
                                    null,
                                    tint = if (isActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(26.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        station.name,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    val detail = buildString {
                                        if (station.language.isNotBlank()) append(station.language)
                                        val firstTag = station.tags.split(",").firstOrNull { it.trim().isNotBlank() }
                                        if (firstTag != null) append(" · $firstTag")
                                    }
                                    if (detail.isNotBlank()) {
                                        Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    }
                                }
                                if (station.votes > 0) {
                                    Text(
                                        "♥ ${station.votes}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        haptic.click(view, hapticEnabled)
                                        viewModel.toggleFavorite(station.stationUuid)
                                        view.announceForAccessibility(
                                            if (station.isFavorite) "Removed ${station.name} from favorites"
                                            else "Added ${station.name} to favorites"
                                        )
                                    },
                                    modifier = Modifier.size(36.dp).semantics {
                                        contentDescription = if (station.isFavorite) "Remove ${station.name} from favorites"
                                                             else "Add ${station.name} to favorites"
                                    }
                                ) {
                                    Icon(
                                        if (station.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                        null,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (station.isFavorite) MaterialTheme.colorScheme.error
                                               else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
