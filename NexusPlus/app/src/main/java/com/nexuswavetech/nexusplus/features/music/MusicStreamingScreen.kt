package com.nexuswavetech.nexusplus.features.music

import android.content.ComponentName
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil.compose.AsyncImage
import com.google.common.util.concurrent.MoreExecutors
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.koin.androidx.compose.koinViewModel
import java.net.URLEncoder

// ── Data model ───────────────────────────────────────────────────────────────

data class MusicTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val imageUrl: String,
    val streamUrl: String,
    val duration: String
)

data class MusicUiState(
    val searchQuery: String = "",
    val tracks: List<MusicTrack> = emptyList(),
    val currentTrack: MusicTrack? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class MusicViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MusicUiState())
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    private val client = OkHttpClient()
    private var mediaController: MediaController? = null

    /** JioSaavn public API wrapper — no auth key required */
    private val baseUrl = "https://saavn.dev/api"

    fun initController(context: android.content.Context) {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, NexusPlaybackService::class.java)
        )
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        future.addListener(
            { mediaController = future.get() },
            MoreExecutors.directExecutor()
        )
    }

    fun onSearchChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun searchMusic(query: String = _uiState.value.searchQuery) {
        if (query.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val encoded = URLEncoder.encode(query, "UTF-8")
                val url = "$baseUrl/search/songs?query=$encoded&limit=20"
                val response = client.newCall(Request.Builder().url(url).build()).execute()
                val json = JSONObject(response.body?.string() ?: "{}")
                val results = json.optJSONObject("data")?.optJSONArray("results") ?: run {
                    _uiState.update { it.copy(isLoading = false, tracks = emptyList()) }
                    return@launch
                }
                val tracks = (0 until results.length()).mapNotNull { i ->
                    val obj = results.getJSONObject(i)
                    val downloadUrls = obj.optJSONArray("downloadUrl")
                    val streamUrl = (0 until (downloadUrls?.length() ?: 0))
                        .map { downloadUrls!!.getJSONObject(it) }
                        .lastOrNull { it.optString("quality") == "320kbps" || it.optString("quality") == "160kbps" }
                        ?.optString("url") ?: return@mapNotNull null
                    val images = obj.optJSONArray("image")
                    val imageUrl = images?.getJSONObject((images.length() - 1).coerceAtLeast(0))?.optString("url") ?: ""
                    MusicTrack(
                        id = obj.optString("id"),
                        title = obj.optString("name"),
                        artist = obj.optJSONArray("artists")
                            ?.optJSONObject(0)?.optJSONObject("primary")
                            ?.optJSONArray("primary")?.let { arr ->
                                (0 until arr.length()).map { arr.getJSONObject(it).optString("name") }.joinToString(", ")
                            } ?: obj.optString("primaryArtists"),
                        album = obj.optJSONObject("album")?.optString("name") ?: "",
                        imageUrl = imageUrl,
                        streamUrl = streamUrl,
                        duration = "${obj.optInt("duration", 0) / 60}:${String.format("%02d", obj.optInt("duration", 0) % 60)}"
                    )
                }
                _uiState.update { it.copy(tracks = tracks, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Search failed: ${e.message}") }
            }
        }
    }

    fun playTrack(track: MusicTrack) {
        val controller = mediaController ?: return
        val mediaItem = MediaItem.Builder()
            .setUri(track.streamUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    .setArtworkUri(android.net.Uri.parse(track.imageUrl))
                    .build()
            )
            .build()
        controller.setMediaItem(mediaItem)
        controller.prepare()
        controller.play()
        _uiState.update { it.copy(currentTrack = track, isPlaying = true) }
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) controller.pause() else controller.play()
        _uiState.update { it.copy(isPlaying = !it.isPlaying) }
    }

    override fun onCleared() {
        mediaController?.release()
        super.onCleared()
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun MusicStreamingScreen(
    onBack: () -> Unit,
    viewModel: MusicViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current

    LaunchedEffect(Unit) { viewModel.initController(context) }

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "Music Streaming", onBack = onBack)

        // Now playing bar
        AnimatedVisibility(visible = uiState.currentTrack != null) {
            uiState.currentTrack?.let { track ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .semantics {
                            contentDescription = "Now playing: ${track.title} by ${track.artist}. " +
                                if (uiState.isPlaying) "Playing." else "Paused."
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AsyncImage(
                            model = track.imageUrl,
                            contentDescription = null,
                            modifier = Modifier.size(52.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                track.title,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                track.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                maxLines = 1
                            )
                        }
                        IconButton(
                            onClick = {
                                viewModel.togglePlayPause()
                                view.announceForAccessibility(if (uiState.isPlaying) "Paused ${track.title}" else "Playing ${track.title}")
                            },
                            modifier = Modifier.semantics {
                                contentDescription = if (uiState.isPlaying) "Pause ${track.title}" else "Play ${track.title}"
                            }
                        ) {
                            Icon(
                                if (uiState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // Search
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchChanged,
                label = { Text("Search songs, artists…") },
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Music search. Enter song name or artist." },
                singleLine = true
            )
            IconButton(
                onClick = { viewModel.searchMusic() },
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Icon(Icons.Filled.Search, contentDescription = "Search")
            }
        }

        Spacer(Modifier.height(8.dp))

        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
            }
            uiState.tracks.isEmpty() && uiState.searchQuery.isNotBlank() -> Box(
                Modifier.fillMaxSize(), contentAlignment = Alignment.Center
            ) {
                Text("No tracks found")
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(uiState.tracks, key = { it.id }) { track ->
                    val isActive = uiState.currentTrack?.id == track.id
                    ListItem(
                        headlineContent = { Text(track.title, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal, maxLines = 1) },
                        supportingContent = { Text("${track.artist} • ${track.duration}", maxLines = 1) },
                        leadingContent = {
                            AsyncImage(
                                model = track.imageUrl,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp)
                            )
                        },
                        trailingContent = {
                            if (isActive && uiState.isPlaying) {
                                Icon(Icons.Filled.GraphicEq, contentDescription = "Currently playing", tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = "${track.title} by ${track.artist}. Duration ${track.duration}." +
                                    if (isActive) " Currently playing." else " Double tap to play."
                            },
                        colors = if (isActive) ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ) else ListItemDefaults.colors()
                    )
                    HorizontalDivider(thickness = 0.5.dp)
                }
            }
        }
    }
}
