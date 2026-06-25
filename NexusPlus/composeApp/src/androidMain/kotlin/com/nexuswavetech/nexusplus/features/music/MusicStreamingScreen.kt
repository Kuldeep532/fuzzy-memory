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
import androidx.compose.ui.text.style.TextAlign
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
import org.koin.androidx.compose.koinViewModel

// ── Data models ───────────────────────────────────────────────────────────────

data class MusicTrack(
    val id       : String,
    val title    : String,
    val artist   : String,
    val album    : String,
    val imageUrl : String,
    val streamUrl: String,
    val duration : String,
)

data class MusicUiState(
    val localTracks  : List<MusicTrack> = emptyList(),
    val currentTrack : MusicTrack?      = null,
    val isPlaying    : Boolean          = false,
    val isLoading    : Boolean          = false,
    val error        : String?          = null,
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class MusicViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MusicUiState())
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    private var mediaController: MediaController? = null

    fun initController(context: android.content.Context) {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, NexusPlaybackService::class.java),
        )
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        future.addListener(
            { mediaController = runCatching { future.get() }.getOrNull() },
            MoreExecutors.directExecutor(),
        )
    }

    fun scanLocalMusic(context: android.content.Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val tracks   = mutableListOf<MusicTrack>()
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
                MediaStore.Audio.Media.ALBUM_ID,
            )
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

            context.contentResolver.query(
                collection, projection, selection, null,
                "${MediaStore.Audio.Media.TITLE} ASC",
            )?.use { cursor ->
                val idCol       = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol    = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol   = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol    = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdCol  = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                while (cursor.moveToNext()) {
                    val id       = cursor.getLong(idCol)
                    val uri      = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    val albumUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        cursor.getLong(albumIdCol),
                    )
                    val durationMs = cursor.getLong(durationCol)
                    val minutes = durationMs / 60000
                    val seconds = (durationMs % 60000) / 1000
                    tracks.add(
                        MusicTrack(
                            id        = id.toString(),
                            title     = cursor.getString(titleCol)  ?: "Unknown",
                            artist    = cursor.getString(artistCol) ?: "Unknown Artist",
                            album     = cursor.getString(albumCol)  ?: "Unknown Album",
                            imageUrl  = albumUri.toString(),
                            streamUrl = uri.toString(),
                            duration  = "$minutes:${"%02d".format(seconds)}",
                        ),
                    )
                }
            }
            _uiState.update { it.copy(localTracks = tracks, isLoading = false) }
        }
    }

    fun playTrack(track: MusicTrack) {
        mediaController?.let { ctrl ->
            val mediaItem = MediaItem.Builder()
                .setUri(track.streamUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                        .setArtworkUri(android.net.Uri.parse(track.imageUrl))
                        .build(),
                )
                .build()
            ctrl.setMediaItem(mediaItem)
            ctrl.prepare()
            ctrl.play()
        }
        _uiState.update { it.copy(currentTrack = track, isPlaying = true) }
    }

    fun togglePlayPause() {
        mediaController?.let { ctrl ->
            if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
            _uiState.update { it.copy(isPlaying = !it.isPlaying) }
        }
    }

    override fun onCleared() {
        mediaController?.release()
        super.onCleared()
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MusicStreamingScreen(
    onBack   : () -> Unit,
    viewModel: MusicViewModel = koinViewModel(),
) {
    val uiState = viewModel.uiState.collectAsState().value
    val context = LocalContext.current
    val view    = LocalView.current

    val audioPermission = rememberPermissionState(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_AUDIO
        else
            Manifest.permission.READ_EXTERNAL_STORAGE,
    )

    LaunchedEffect(Unit) { viewModel.initController(context) }

    Column(modifier = Modifier.fillMaxSize()) {

        NexusTopBar(title = "Nexus Media Player", onBack = onBack)

        // ── Now playing bar ────────────────────────────────────────────────
        AnimatedVisibility(visible = uiState.currentTrack != null) {
            uiState.currentTrack?.let { track ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .semantics {
                            contentDescription = "Now playing: ${track.title} by ${track.artist}." +
                                if (uiState.isPlaying) " Playing." else " Paused."
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AsyncImage(
                            model              = track.imageUrl.ifBlank { null },
                            contentDescription = null,
                            modifier           = Modifier.size(48.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                track.title,
                                style    = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                color    = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                track.artist,
                                style    = MaterialTheme.typography.bodySmall,
                                color    = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                maxLines = 1,
                            )
                        }
                        IconButton(
                            onClick  = {
                                viewModel.togglePlayPause()
                                view.announceForAccessibility(
                                    if (uiState.isPlaying) "Paused ${track.title}" else "Playing ${track.title}",
                                )
                            },
                            modifier = Modifier.semantics {
                                contentDescription = if (uiState.isPlaying) "Pause ${track.title}" else "Play ${track.title}"
                            },
                        ) {
                            Icon(
                                imageVector        = if (uiState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint               = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }
        }

        // ── Library content ────────────────────────────────────────────────
        if (!audioPermission.status.isGranted) {
            // Permission gate
            Box(
                modifier         = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier            = Modifier.padding(32.dp),
                ) {
                    Icon(
                        Icons.Filled.MusicNote,
                        contentDescription = null,
                        modifier           = Modifier.size(72.dp),
                        tint               = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    )
                    Text(
                        "Storage Permission Required",
                        style    = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "Grant access to scan and play music files from your device.",
                        style     = MaterialTheme.typography.bodyMedium,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Button(
                        onClick  = {
                            audioPermission.launchPermissionRequest()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Grant storage permission to access music files" },
                    ) { Text("Grant Permission") }
                }
            }
        } else {
            // Scan bar
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(
                    "${uiState.localTracks.size} song${if (uiState.localTracks.size != 1) "s" else ""} on device",
                    style    = MaterialTheme.typography.labelMedium,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics {
                        contentDescription = "${uiState.localTracks.size} songs found on device"
                    },
                )
                TextButton(
                    onClick  = { viewModel.scanLocalMusic(context) },
                    modifier = Modifier.semantics { contentDescription = "Rescan music library" },
                ) {
                    Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Scan")
                }
            }

            // Loading or list
            if (uiState.isLoading) {
                Box(
                    modifier         = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator()
                        Text("Scanning music library…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else if (uiState.localTracks.isEmpty()) {
                LaunchedEffect(Unit) { viewModel.scanLocalMusic(context) }
                Box(
                    modifier         = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier            = Modifier.semantics {
                            contentDescription = "No music files found on device"
                        },
                    ) {
                        Icon(
                            Icons.Filled.LibraryMusic,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                        )
                        Text(
                            "No music found on device",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "Add audio files to your device storage and tap Scan.",
                            style     = MaterialTheme.typography.bodyMedium,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier  = Modifier.padding(horizontal = 24.dp),
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(uiState.localTracks, key = { it.id }) { track ->
                        val isActive = uiState.currentTrack?.id == track.id
                        ListItem(
                            headlineContent   = {
                                Text(
                                    track.title,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    maxLines   = 1,
                                )
                            },
                            supportingContent = {
                                Text("${track.artist} · ${track.duration}", maxLines = 1)
                            },
                            leadingContent    = {
                                Icon(
                                    imageVector        = if (isActive && uiState.isPlaying) Icons.Filled.GraphicEq else Icons.Filled.MusicNote,
                                    contentDescription = null,
                                    tint               = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            trailingContent   = if (isActive) ({
                                Icon(
                                    if (uiState.isPlaying) Icons.Filled.VolumeUp else Icons.Filled.VolumeMute,
                                    contentDescription = null,
                                    tint               = MaterialTheme.colorScheme.primary,
                                    modifier           = Modifier.size(18.dp),
                                )
                            }) else null,
                            colors            = if (isActive)
                                ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                            else
                                ListItemDefaults.colors(),
                            modifier          = Modifier
                                .fillMaxWidth()
                                .semantics(mergeDescendants = true) {
                                    contentDescription = "${track.title} by ${track.artist}. Duration ${track.duration}.${if (isActive) if (uiState.isPlaying) " Currently playing." else " Paused." else ""}"
                                    customActions = listOf(
                                        CustomAccessibilityAction("Play ${track.title}") {
                                            viewModel.playTrack(track)
                                            view.announceForAccessibility("Playing ${track.title}")
                                            true
                                        },
                                    )
                                },
                        )
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}
