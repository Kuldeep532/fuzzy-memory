package com.nexuswavetech.nexusplus.features.music

import android.Manifest
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.koin.androidx.compose.koinViewModel
import kotlin.math.max
import kotlin.math.min

// ── Data models ──────────────────────────────────────────────────────────

data class MediaTrack(
    val id       : String,
    val title    : String,
    val artist   : String,
    val album    : String,
    val imageUrl : String,
    val mediaUri : String,
    val durationMs: Long,
    val isVideo  : Boolean = false,
)

enum class RepeatMode { OFF, ONE, ALL }

private fun Long.formatDuration(): String {
    val totalSeconds = this / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

// ── ViewModel ──────────────────────────────────────────────────────────────────

class FullMediaPlayerViewModel : ViewModel() {

    private val _tracks = MutableStateFlow<List<MediaTrack>>(emptyList())
    val tracks: StateFlow<List<MediaTrack>> = _tracks.asStateFlow()

    private val _currentTrack = MutableStateFlow<MediaTrack?>(null)
    val currentTrack: StateFlow<MediaTrack?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _progressMs = MutableStateFlow(0L)
    val progressMs: StateFlow<Long> = _progressMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _shuffle = MutableStateFlow(false)
    val shuffle: StateFlow<Boolean> = _shuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _showPlayer = MutableStateFlow(false)
    val showPlayer: StateFlow<Boolean> = _showPlayer.asStateFlow()

    private var mediaController: MediaController? = null
    private var progressJob: Job? = null
    private var trackIndex = 0

    fun initController(context: android.content.Context) {
        val sessionToken = SessionToken(
            context,
            android.content.ComponentName(context, NexusPlaybackService::class.java),
        )
        val future = androidx.media3.session.MediaController.Builder(context, sessionToken).buildAsync()
        future.addListener(
            {
                mediaController = runCatching { future.get() }.getOrNull()
                mediaController?.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        _isPlaying.value = playing
                        if (playing) startProgressPolling() else stopProgressPolling()
                    }
                    override fun onPlaybackStateChanged(state: Int) {
                        _isLoading.value = state == Player.STATE_BUFFERING
                    }
                    override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                        item?.mediaMetadata?.title?.toString()?.let { title ->
                            _currentTrack.value = _tracks.value.find { it.title == title }
                        }
                    }
                    override fun onPositionDiscontinuity(
                        oldPosition: Player.PositionInfo,
                        newPosition: Player.PositionInfo,
                        reason: Int
                    ) {
                        _progressMs.value = newPosition.positionMs
                    }
                })
            },
            MoreExecutors.directExecutor(),
        )
    }

    fun scanMedia(context: android.content.Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val allTracks = mutableListOf<MediaTrack>()

            // Audio
            val audioCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

            val audioProjection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID,
            )
            context.contentResolver.query(
                audioCollection, audioProjection,
                "${MediaStore.Audio.Media.IS_MUSIC} != 0", null,
                "${MediaStore.Audio.Media.TITLE} ASC",
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val albumId = cursor.getLong(albumIdCol)
                    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    val artUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"), albumId,
                    )
                    allTracks.add(
                        MediaTrack(
                            id = id.toString(),
                            title = cursor.getString(titleCol) ?: "Unknown",
                            artist = cursor.getString(artistCol) ?: "Unknown Artist",
                            album = cursor.getString(albumCol) ?: "Unknown Album",
                            imageUrl = artUri.toString(),
                            mediaUri = uri.toString(),
                            durationMs = cursor.getLong(durCol),
                            isVideo = false,
                        )
                    )
                }
            }

            // Video
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val videoCollection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                val videoProjection = arrayOf(
                    MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.TITLE,
                    MediaStore.Video.Media.ARTIST,
                    MediaStore.Video.Media.ALBUM,
                    MediaStore.Video.Media.DURATION,
                )
                context.contentResolver.query(
                    videoCollection, videoProjection, null, null,
                    "${MediaStore.Video.Media.TITLE} ASC",
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                    val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                    val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.ARTIST)
                    val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.ALBUM)
                    val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                        allTracks.add(
                            MediaTrack(
                                id = "v_$id",
                                title = cursor.getString(titleCol) ?: "Unknown Video",
                                artist = cursor.getString(artistCol) ?: "",
                                album = cursor.getString(albumCol) ?: "",
                                imageUrl = "",
                                mediaUri = uri.toString(),
                                durationMs = cursor.getLong(durCol),
                                isVideo = true,
                            )
                        )
                    }
                }
            }

            _tracks.value = allTracks
            _isLoading.value = false
        }
    }

    fun playTrack(track: MediaTrack, context: android.content.Context? = null) {
        mediaController?.let { ctrl ->
            val mediaItem = MediaItem.Builder()
                .setUri(track.mediaUri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                        .setArtworkUri(if (track.imageUrl.isNotBlank()) Uri.parse(track.imageUrl) else null)
                        .build(),
                )
                .build()
            ctrl.setMediaItem(mediaItem)
            ctrl.prepare()
            ctrl.play()
        }
        _currentTrack.value = track
        trackIndex = _tracks.value.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        _showPlayer.value = true
        _durationMs.value = track.durationMs
    }

    fun togglePlayPause() {
        mediaController?.let { ctrl ->
            if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _progressMs.value = positionMs
    }

    fun nextTrack() {
        val tracks = _tracks.value
        if (tracks.isEmpty()) return
        if (_shuffle.value) {
            trackIndex = (trackIndex + 1 + (0 until tracks.size).random()) % tracks.size
        } else {
            trackIndex = (trackIndex + 1) % tracks.size
        }
        playTrack(tracks[trackIndex])
    }

    fun previousTrack() {
        val tracks = _tracks.value
        if (tracks.isEmpty()) return
        if (_shuffle.value) {
            trackIndex = (trackIndex - 1 + (0 until tracks.size).random()).coerceIn(0, tracks.size - 1)
        } else {
            trackIndex = (trackIndex - 1 + tracks.size) % tracks.size
        }
        playTrack(tracks[trackIndex])
    }

    fun toggleShuffle() {
        _shuffle.value = !_shuffle.value
    }

    fun cycleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        mediaController?.let { ctrl ->
            ctrl.repeatMode = when (_repeatMode.value) {
                RepeatMode.OFF -> Player.REPEAT_MODE_OFF
                RepeatMode.ALL -> Player.REPEAT_MODE_ALL
                RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            }
        }
    }

    fun showPlayer() { _showPlayer.value = true }
    fun hidePlayer() { _showPlayer.value = false }

    private fun startProgressPolling() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                mediaController?.let { ctrl ->
                    _progressMs.value = ctrl.currentPosition
                    _durationMs.value = ctrl.duration.coerceAtLeast(0L)
                }
                delay(500L)
            }
        }
    }

    private fun stopProgressPolling() {
        progressJob?.cancel()
    }

    override fun onCleared() {
        stopProgressPolling()
        mediaController?.release()
        super.onCleared()
    }
}

// ── Full screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun FullMediaPlayerScreen(
    onBack: () -> Unit,
    viewModel: FullMediaPlayerViewModel = koinViewModel(),
) {
    val tracks    = viewModel.tracks.collectAsState().value
    val current   = viewModel.currentTrack.collectAsState().value
    val isPlaying = viewModel.isPlaying.collectAsState().value
    val progress  = viewModel.progressMs.collectAsState().value
    val duration  = viewModel.durationMs.collectAsState().value
    val isLoading = viewModel.isLoading.collectAsState().value
    val shuffle   = viewModel.shuffle.collectAsState().value
    val repeat    = viewModel.repeatMode.collectAsState().value
    val showPlayer= viewModel.showPlayer.collectAsState().value

    val context   = LocalContext.current
    val view      = LocalView.current

    val audioPermission = rememberPermissionState(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_AUDIO
        else
            Manifest.permission.READ_EXTERNAL_STORAGE,
    )

    LaunchedEffect(Unit) { viewModel.initController(context) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    "Nexus Media Player",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                )
                IconButton(
                    onClick = { viewModel.scanMedia(context) },
                    modifier = Modifier.semantics { contentDescription = "Scan media library" },
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Rescan")
                }
            }

            if (!audioPermission.status.isGranted) {
                PermissionGate(audioPermission::launchPermissionRequest)
            } else {
                if (tracks.isEmpty() && !isLoading) {
                    LaunchedEffect(Unit) { viewModel.scanMedia(context) }
                }

                // Filter tabs
                var selectedTab by remember { mutableStateOf(0) }
                val tabs = listOf("All" to tracks.size, "Music" to tracks.count { !it.isVideo }, "Videos" to tracks.count { it.isVideo })
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, (label, count) ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text("$label ($count)") },
                        )
                    }
                }

                val filtered = when (selectedTab) {
                    1 -> tracks.filter { !it.isVideo }
                    2 -> tracks.filter { it.isVideo }
                    else -> tracks
                }

                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator()
                            Text("Scanning media library…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else if (filtered.isEmpty()) {
                    EmptyMediaView()
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        items(filtered, key = { it.id }) { track ->
                            val isActive = current?.id == track.id
                            ListItem(
                                headlineContent = {
                                    Text(
                                        track.title,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        "${track.artist} · ${track.durationMs.formatDuration()}",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                leadingContent = {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (track.imageUrl.isNotBlank()) {
                                            AsyncImage(
                                                model = track.imageUrl,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                        } else {
                                            Icon(
                                                imageVector = if (track.isVideo) Icons.Filled.Videocam else Icons.Filled.MusicNote,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                },
                                trailingContent = if (isActive) ({
                                    Icon(
                                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }) else null,
                                colors = if (isActive)
                                    ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                else ListItemDefaults.colors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics(mergeDescendants = true) {
                                        contentDescription = "${track.title} by ${track.artist}. Duration ${track.durationMs.formatDuration()}.${if (isActive) if (isPlaying) " Currently playing." else " Paused." else ""}"
                                    },
                            )
                            HorizontalDivider(thickness = 0.5.dp)
                        }
                    }
                }
            }
        }

        // Now Playing mini bar + full player overlay
        AnimatedVisibility(
            visible = current != null || showPlayer,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            current?.let { track ->
                if (!showPlayer) {
                    // Mini bar
                    MiniPlayerBar(
                        track = track,
                        isPlaying = isPlaying,
                        progress = progress,
                        duration = duration,
                        onPlayPause = { viewModel.togglePlayPause() },
                        onExpand = { viewModel.showPlayer() },
                    )
                } else {
                    // Full player
                    FullPlayerOverlay(
                        track = track,
                        isPlaying = isPlaying,
                        progress = progress,
                        duration = duration,
                        shuffle = shuffle,
                        repeat = repeat,
                        onPlayPause = { viewModel.togglePlayPause() },
                        onSeek = { viewModel.seekTo(it) },
                        onNext = { viewModel.nextTrack() },
                        onPrev = { viewModel.previousTrack() },
                        onShuffle = { viewModel.toggleShuffle() },
                        onRepeat = { viewModel.cycleRepeatMode() },
                        onCollapse = { viewModel.hidePlayer() },
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniPlayerBar(
    track: MediaTrack,
    isPlaying: Boolean,
    progress: Long,
    duration: Long,
    onPlayPause: () -> Unit,
    onExpand: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        onClick = onExpand,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    if (track.imageUrl.isNotBlank()) {
                        AsyncImage(model = track.imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
                    } else {
                        Icon(Icons.Filled.MusicNote, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(track.title, maxLines = 1, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                    Text(track.artist, maxLines = 1, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onPlayPause) {
                    Icon(
                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                IconButton(onClick = onExpand) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Expand player", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            // Progress bar
            LinearProgressIndicator(
                progress = { if (duration > 0) (progress.toFloat() / duration).coerceIn(0f, 1f) else 0f },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
            )
        }
    }
}

@Composable
private fun FullPlayerOverlay(
    track: MediaTrack,
    isPlaying: Boolean,
    progress: Long,
    duration: Long,
    shuffle: Boolean,
    repeat: RepeatMode,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onCollapse: () -> Unit,
) {
    val view = LocalView.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        MaterialTheme.colorScheme.background,
                    )
                )
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Collapse player")
                }
                Text("Now Playing", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                Spacer(Modifier.width(48.dp))
            }

            // Artwork
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (track.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = track.imageUrl,
                        contentDescription = "Album art for ${track.title}",
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = if (track.isVideo) Icons.Filled.Videocam else Icons.Filled.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                }
            }

            // Track info
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    track.title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    track.artist,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }

            // Seek bar
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = if (duration > 0) progress.toFloat() else 0f,
                    onValueChange = { onSeek(it.toLong()) },
                    valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                    modifier = Modifier.semantics { contentDescription = "Seek. ${progress.formatDuration()} of ${duration.formatDuration()}" },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(progress.formatDuration(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(duration.formatDuration(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onShuffle, modifier = Modifier.semantics { contentDescription = if (shuffle) "Shuffle on" else "Shuffle off" }) {
                    Icon(
                        Icons.Filled.Shuffle,
                        contentDescription = null,
                        tint = if (shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onPrev, modifier = Modifier.semantics { contentDescription = "Previous track" }) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = null, modifier = Modifier.size(32.dp))
                }
                FilledIconButton(
                    onClick = {
                        onPlayPause()
                        view.announceForAccessibility(if (isPlaying) "Paused" else "Playing")
                    },
                    modifier = Modifier
                        .size(72.dp)
                        .semantics { contentDescription = if (isPlaying) "Pause" else "Play" },
                    shape = CircleShape,
                ) {
                    Icon(
                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                    )
                }
                IconButton(onClick = onNext, modifier = Modifier.semantics { contentDescription = "Next track" }) {
                    Icon(Icons.Filled.SkipNext, contentDescription = null, modifier = Modifier.size(32.dp))
                }
                IconButton(
                    onClick = onRepeat,
                    modifier = Modifier.semantics {
                        contentDescription = when (repeat) {
                            RepeatMode.OFF -> "Repeat off"
                            RepeatMode.ALL -> "Repeat all"
                            RepeatMode.ONE -> "Repeat one"
                        }
                    },
                ) {
                    Icon(
                        if (repeat == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        contentDescription = null,
                        tint = if (repeat != RepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionGate(onGrant: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(Icons.Filled.MusicNote, null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
            Text("Storage Permission Required", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
            Text("Grant access to scan and play music & video files from your device.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Button(onClick = onGrant, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Grant storage permission" }) {
                Text("Grant Permission")
            }
        }
    }
}

@Composable
private fun EmptyMediaView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Filled.LibraryMusic, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
            Text("No media found", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Add audio or video files to your device storage and tap Scan.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
        }
    }
}
