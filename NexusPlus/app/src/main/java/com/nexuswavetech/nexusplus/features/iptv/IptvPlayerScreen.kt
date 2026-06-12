package com.nexuswavetech.nexusplus.features.iptv

import android.view.ViewGroup
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.androidx.compose.koinViewModel
import java.net.URLDecoder

data class IptvChannel(
    val name: String,
    val url: String,
    val logo: String = "",
    val group: String = ""
)

data class IptvUiState(
    val playlistUrl: String = "",
    val channels: List<IptvChannel> = emptyList(),
    val currentChannel: IptvChannel? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class IptvViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(IptvUiState())
    val uiState: StateFlow<IptvUiState> = _uiState.asStateFlow()

    private val client = OkHttpClient()

    /** Default free IPTV playlist from iptv-org GitHub */
    private val defaultPlaylistUrl =
        "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/us.m3u"

    init { loadPlaylist(defaultPlaylistUrl) }

    fun onUrlChanged(url: String) = _uiState.update { it.copy(playlistUrl = url) }

    fun loadPlaylist(url: String = _uiState.value.playlistUrl) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val request = Request.Builder().url(url.ifBlank { defaultPlaylistUrl }).build()
                val body = client.newCall(request).execute().body?.string() ?: ""
                val channels = parseM3u(body)
                _uiState.update { it.copy(channels = channels, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to load playlist: ${e.message}") }
            }
        }
    }

    fun onChannelSelected(channel: IptvChannel) {
        _uiState.update { it.copy(currentChannel = channel) }
    }

    private fun parseM3u(raw: String): List<IptvChannel> {
        val channels = mutableListOf<IptvChannel>()
        val lines = raw.lines()
        var name = ""
        var logo = ""
        var group = ""
        for (line in lines) {
            when {
                line.startsWith("#EXTINF") -> {
                    name = Regex(",(.*?)$").find(line)?.groupValues?.get(1)?.trim() ?: "Unknown"
                    logo = Regex("tvg-logo=\"(.*?)\"").find(line)?.groupValues?.get(1) ?: ""
                    group = Regex("group-title=\"(.*?)\"").find(line)?.groupValues?.get(1) ?: ""
                }
                line.startsWith("http") -> {
                    if (name.isNotBlank()) {
                        channels.add(IptvChannel(name = name, url = line.trim(), logo = logo, group = group))
                        name = ""; logo = ""; group = ""
                    }
                }
            }
        }
        return channels.take(200) // cap for performance
    }
}

@Composable
fun IptvPlayerScreen(
    onBack: () -> Unit,
    viewModel: IptvViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    LaunchedEffect(uiState.currentChannel) {
        uiState.currentChannel?.let { channel ->
            exoPlayer.setMediaItem(MediaItem.fromUri(channel.url))
            exoPlayer.prepare()
            exoPlayer.play()
            view.announceForAccessibility("Playing channel: ${channel.name}")
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE  -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> exoPlayer.play()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "IPTV / Live TV", onBack = onBack)

        // Video player
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    contentDescription = "Video player. ${uiState.currentChannel?.name ?: "No channel selected"}. " +
                        "Use player controls below the video."
                }
            },
            update = { it.player = exoPlayer },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        )

        // Channel list
        if (uiState.isLoading) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.semantics { contentDescription = "Loading channel list." }
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (uiState.channels.isEmpty()) {
                    item {
                        Text(
                            uiState.error ?: "No channels found.",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    items(uiState.channels, key = { it.url }) { channel ->
                        val isActive = uiState.currentChannel?.url == channel.url
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = channel.name,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            supportingContent = { if (channel.group.isNotBlank()) Text(channel.group) },
                            leadingContent = {
                                Icon(
                                    if (isActive) Icons.Filled.PlayCircle else Icons.Filled.LiveTv,
                                    contentDescription = null,
                                    tint = if (isActive) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics {
                                    contentDescription = "${channel.name} channel." +
                                        if (isActive) " Currently playing." else " Double tap to play."
                                },
                            colors = if (isActive) ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ) else ListItemDefaults.colors()
                        )
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}
