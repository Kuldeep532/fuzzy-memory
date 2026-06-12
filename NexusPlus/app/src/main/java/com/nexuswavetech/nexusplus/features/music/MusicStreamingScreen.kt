package com.nexuswavetech.nexusplus.features.music

import android.Manifest
import android.content.ComponentName
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
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
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.common.util.concurrent.MoreExecutors
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.koin.androidx.compose.koinViewModel
import java.net.URLEncoder

// ── Data models ───────────────────────────────────────────────────────────────

data class MusicTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val imageUrl: String,
    val streamUrl: String,
    val duration: String,
    val isLocal: Boolean = false
)

enum class MusicMode(val label: String) { OFFLINE("Offline"), ONLINE("Online") }

data class MusicUiState(
    val mode: MusicMode = MusicMode.ONLINE,
    val searchQuery: String = "",
    val tracks: List<MusicTrack> = emptyList(),
    val localTracks: List<MusicTrack> = emptyList(),
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

    /** JioSaavn public API wrapper */
    private val baseUrl = "https://saavn.dev/api"

    fun setMode(mode: MusicMode) = _uiState.update { it.copy(mode = mode, error = null) }

    fun initController(context: android.content.Context) {
        val sessionToken = SessionToken(context, ComponentName(context, NexusPlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        future.addListener({ mediaController = runCatching { future.get() }.getOrNull() }, MoreExecutors.directExecutor())
    }

    fun scanLocalMusic(context: android.content.Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val tracks = mutableListOf<MusicTrack>()
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            else
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID
            )
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            context.contentResolver.query(collection, projection, selection, null, "${MediaStore.Audio.Media.TITLE} ASC")?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    val albumUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), cursor.getLong(albumIdCol))
                    val durationMs = cursor.getLong(durationCol)
                    val minutes = durationMs / 60000; val seconds = (durationMs % 60000) / 1000
                    tracks.add(MusicTrack(
                        id = id.toString(),
                        title = cursor.getString(titleCol) ?: "Unknown",
                        artist = cursor.getString(artistCol) ?: "Unknown",
                        album = cursor.getString(albumCol) ?: "Unknown",
                        imageUrl = albumUri.toString(),
                        streamUrl = uri.toString(),
                        duration = "$minutes:${"%02d".format(seconds)}",
                        isLocal = true
                    ))
                }
            }
            _uiState.update { it.copy(localTracks = tracks, isLoading = false) }
        }
    }

    fun onSearchChanged(query: String) = _uiState.update { it.copy(searchQuery = query) }

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
                    val artists = obj.optJSONObject("artists")?.optJSONArray("primary")?.let { arr ->
                        (0 until arr.length()).joinToString(", ") { arr.getJSONObject(it).optString("name") }
                    } ?: obj.optString("primaryArtists", "Unknown")
                    MusicTrack(
                        id = obj.optString("id"),
                        title = obj.optString("name"),
                        artist = artists,
                        album = obj.optJSONObject("album")?.optString("name") ?: "",
                        imageUrl = imageUrl,
                        streamUrl = streamUrl,
                        duration = "${obj.optInt("duration", 0) / 60}:${"%02d".format(obj.optInt("duration", 0) % 60)}"
                    )
                }
                _uiState.update { it.copy(tracks = tracks, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Search failed: ${e.message}") }
            }
        }
    }

    fun playTrack(track: MusicTrack) {
        val controller = mediaController
        if (controller != null) {
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
        }
        _uiState.update { it.copy(currentTrack = track, isPlaying = true) }
    }

    fun togglePlayPause() {
        val controller = mediaController
        if (controller != null) {
            if (controller.isPlaying) controller.pause() else controller.play()
            _uiState.update { it.copy(isPlaying = !it.isPlaying) }
        }
    }

    override fun onCleared() { mediaController?.release(); super.onCleared() }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MusicStreamingScreen(
    onBack: () -> Unit,
    viewModel: MusicViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current

    val audioPermission = rememberPermissionState(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_AUDIO
        else
            Manifest.permission.READ_EXTERNAL_STORAGE
    )

    LaunchedEffect(Unit) { viewModel.initController(context) }

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "Music Player", onBack = onBack)

        // Mode tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MusicMode.values().forEach { mode ->
                FilterChip(
                    selected = uiState.mode == mode,
                    onClick = { viewModel.setMode(mode) },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                if (mode == MusicMode.OFFLINE) Icons.Filled.PhoneAndroid else Icons.Filled.CloudQueue,
                                null, modifier = Modifier.size(16.dp)
                            )
                            Text(mode.label)
                        }
                    },
                    modifier = Modifier.semantics { contentDescription = "${mode.label} music mode" }
                )
            }
        }

        // Now playing bar
        AnimatedVisibility(visible = uiState.currentTrack != null) {
            uiState.currentTrack?.let { track ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .semantics {
                            contentDescription = "Now playing: ${track.title} by ${track.artist}. " +
                                if (uiState.isPlaying) "Playing." else "Paused."
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AsyncImage(
                            model = track.imageUrl.ifBlank { null },
                            contentDescription = null,
                            modifier = Modifier.size(48.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(track.title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), maxLines = 1, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(track.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f), maxLines = 1)
                        }
                        IconButton(
                            onClick = {
                                viewModel.togglePlayPause()
                                view.announceForAccessibility(if (uiState.isPlaying) "Paused ${track.title}" else "Playing ${track.title}")
                            },
                            modifier = Modifier.semantics { contentDescription = if (uiState.isPlaying) "Pause" else "Play" }
                        ) {
                            Icon(
                                if (uiState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        when (uiState.mode) {
            MusicMode.OFFLINE -> OfflineMusicContent(uiState, viewModel, audioPermission.status.isGranted) {
                audioPermission.launchPermissionRequest()
                if (audioPermission.status.isGranted) viewModel.scanLocalMusic(context)
            }
            MusicMode.ONLINE  -> OnlineMusicContent(uiState, viewModel, view)
        }
    }
}

@Composable
private fun OfflineMusicContent(
    uiState: MusicUiState,
    viewModel: MusicViewModel,
    hasPermission: Boolean,
    onRequestAccess: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current

    Column(modifier = Modifier.fillMaxSize()) {
        if (!hasPermission) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(Icons.Filled.PhoneAndroid, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("Storage Permission Required", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("Grant access to scan and play music files from your device.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Button(onClick = onRequestAccess, modifier = Modifier.fillMaxWidth()) { Text("Grant Permission") }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${uiState.localTracks.size} song${if (uiState.localTracks.size != 1) "s" else ""} on device",
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = { viewModel.scanLocalMusic(context) }) {
                    Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Scan")
                }
            }
            if (uiState.localTracks.isEmpty()) {
                LaunchedEffect(Unit) { viewModel.scanLocalMusic(context) }
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (uiState.isLoading) Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(); Text("Scanning music library…")
                    }
                    else Text("No music found on device.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(uiState.localTracks, key = { it.id }) { track ->
                        val isActive = uiState.currentTrack?.id == track.id
                        ListItem(
                            headlineContent = { Text(track.title, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal, maxLines = 1) },
                            supportingContent = { Text("${track.artist} • ${track.duration}", maxLines = 1) },
                            leadingContent = {
                                Icon(
                                    if (isActive && uiState.isPlaying) Icons.Filled.GraphicEq else Icons.Filled.MusicNote,
                                    contentDescription = null,
                                    tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics(mergeDescendants = true) {
                                    contentDescription = "${track.title} by ${track.artist}. Duration ${track.duration}." +
                                        if (isActive) " Currently playing." else " Double tap to play."
                                    customActions = listOf(
                                        CustomAccessibilityAction("Play ${track.title}") { viewModel.playTrack(track); true }
                                    )
                                },
                            colors = if (isActive) ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)) else ListItemDefaults.colors()
                        )
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun OnlineMusicContent(uiState: MusicUiState, viewModel: MusicViewModel, view: android.view.View) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Search
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchChanged,
                label = { Text("Search songs, artists…") },
                modifier = Modifier.weight(1f).semantics { contentDescription = "Online music search" },
                singleLine = true
            )
            IconButton(
                onClick = { viewModel.searchMusic() },
                modifier = Modifier.align(Alignment.CenterVertically)
            ) { Icon(Icons.Filled.Search, contentDescription = "Search music") }
        }

        Spacer(Modifier.height(8.dp))

        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            uiState.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(uiState.error, color = MaterialTheme.colorScheme.error) }
            uiState.tracks.isEmpty() && uiState.searchQuery.isNotBlank() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No tracks found") }
            uiState.tracks.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.CloudQueue, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    Text("Search for songs or artists", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
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
                            AsyncImage(model = track.imageUrl.ifBlank { null }, contentDescription = null, modifier = Modifier.size(48.dp))
                        },
                        trailingContent = {
                            if (isActive && uiState.isPlaying) Icon(Icons.Filled.GraphicEq, "Currently playing", tint = MaterialTheme.colorScheme.primary)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics(mergeDescendants = true) {
                                contentDescription = "${track.title} by ${track.artist}. Duration ${track.duration}." +
                                    if (isActive) " Currently playing." else " Double tap to play."
                                customActions = listOf(
                                    CustomAccessibilityAction("Play ${track.title}") { viewModel.playTrack(track); view.announceForAccessibility("Playing ${track.title}"); true }
                                )
                            },
                        colors = if (isActive) ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)) else ListItemDefaults.colors()
                    )
                    HorizontalDivider(thickness = 0.5.dp)
                }
            }
        }
    }
}
