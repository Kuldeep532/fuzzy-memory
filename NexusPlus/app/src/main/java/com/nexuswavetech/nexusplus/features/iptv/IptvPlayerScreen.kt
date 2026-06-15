package com.nexuswavetech.nexusplus.features.iptv

import android.view.ViewGroup
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
import androidx.lifecycle.compose.LocalLifecycleOwner
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
    val error: String? = null,
    val selectedRegion: IptvRegion = IptvRegion.INDIA
)

enum class IptvRegion(val label: String, val m3uUrl: String) {
    INDIA("🇮🇳 India",
        "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/in.m3u"),
    HINDI_NEWS("📺 Hindi News",
        "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/in_news.m3u"),
    KIDS("👶 Kids & Family",
        "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/in_entertainment.m3u"),
    SPORTS("⚽ Sports",
        "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/in_sports.m3u"),
    GLOBAL("🌍 Global (Top)",
        "https://raw.githubusercontent.com/iptv-org/iptv/master/index.nsfw.m3u"),
    CUSTOM("🔗 Custom URL", "")
}

class IptvViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(IptvUiState())
    val uiState: StateFlow<IptvUiState> = _uiState.asStateFlow()

    private val client = OkHttpClient()

    init { loadPlaylist(IptvRegion.INDIA.m3uUrl) }

    fun onRegionSelected(region: IptvRegion) {
        _uiState.update { it.copy(selectedRegion = region) }
        if (region != IptvRegion.CUSTOM) loadPlaylist(region.m3uUrl)
    }

    fun onUrlChanged(url: String) = _uiState.update { it.copy(playlistUrl = url) }

    fun loadPlaylist(url: String = _uiState.value.playlistUrl) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val finalUrl = url.ifBlank { _uiState.value.selectedRegion.m3uUrl }
                val request = Request.Builder().url(finalUrl).build()
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
        return channels.take(300)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IptvPlayerScreen(
    onBack: () -> Unit,
    viewModel: IptvViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showCustomUrl by remember { mutableStateOf(false) }
    var groupFilter by remember { mutableStateOf<String?>(null) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply { playWhenReady = true }
    }

    LaunchedEffect(uiState.currentChannel) {
        uiState.currentChannel?.let { channel ->
            exoPlayer.setMediaItem(MediaItem.fromUri(channel.url))
            exoPlayer.prepare()
            exoPlayer.play()
            view.announceForAccessibility("Playing: ${channel.name}")
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE  -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> if (uiState.currentChannel != null) exoPlayer.play()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer); exoPlayer.release() }
    }

    val groups = remember(uiState.channels) { uiState.channels.map { it.group }.distinct().filter { it.isNotBlank() } }
    val filteredChannels = remember(uiState.channels, groupFilter) {
        if (groupFilter == null) uiState.channels else uiState.channels.filter { it.group == groupFilter }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "IPTV / Live TV", onBack = onBack)

        // Region selector
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.semantics { contentDescription = "Region selection. Swipe to browse." }
        ) {
            items(IptvRegion.entries.toList()) { region ->
                FilterChip(
                    selected = uiState.selectedRegion == region,
                    onClick = {
                        if (region == IptvRegion.CUSTOM) showCustomUrl = true
                        else viewModel.onRegionSelected(region)
                    },
                    label = { Text(region.label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        // Custom URL dialog
        if (showCustomUrl) {
            AlertDialog(
                onDismissRequest = { showCustomUrl = false },
                title = { Text("Custom M3U URL") },
                text = {
                    OutlinedTextField(
                        value = uiState.playlistUrl,
                        onValueChange = viewModel::onUrlChanged,
                        label = { Text("Playlist URL (.m3u / .m3u8)") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.loadPlaylist(); showCustomUrl = false }) { Text("Load") }
                },
                dismissButton = { TextButton(onClick = { showCustomUrl = false }) { Text("Cancel") } }
            )
        }

        // Video player
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    contentDescription = "Video player. ${uiState.currentChannel?.name ?: "No channel selected"}"
                }
            },
            update = { it.player = exoPlayer },
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
        )

        // Now playing
        uiState.currentChannel?.let { ch ->
            Surface(color = MaterialTheme.colorScheme.primaryContainer) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.LiveTv, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(ch.name, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 1, modifier = Modifier.weight(1f))
                    if (ch.group.isNotBlank()) Text(ch.group, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                }
            }
        }

        // Group filter chips
        if (groups.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    FilterChip(selected = groupFilter == null, onClick = { groupFilter = null }, label = { Text("All") })
                }
                items(groups.take(15)) { group ->
                    FilterChip(
                        selected = groupFilter == group,
                        onClick = { groupFilter = if (groupFilter == group) null else group },
                        label = { Text(group, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        // Channel list
        when {
            uiState.isLoading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = "Loading channels." })
                    Text("Loading channels…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (filteredChannels.isEmpty()) {
                    item {
                        Text(
                            uiState.error ?: "No channels found.",
                            color = if (uiState.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    item {
                        Text(
                            "${filteredChannels.size} channel${if (filteredChannels.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(filteredChannels, key = { it.url }) { channel ->
                        val isActive = uiState.currentChannel?.url == channel.url
                        ListItem(
                            headlineContent = { Text(channel.name, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal) },
                            supportingContent = { if (channel.group.isNotBlank()) Text(channel.group, style = MaterialTheme.typography.labelSmall) },
                            leadingContent = {
                                Icon(
                                    if (isActive) Icons.Filled.PlayCircle else Icons.Filled.LiveTv,
                                    contentDescription = null,
                                    tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics(mergeDescendants = true) {
                                    contentDescription = "${channel.name}${if (channel.group.isNotBlank()) ", ${channel.group}" else ""}." +
                                        if (isActive) " Currently playing." else " Double tap to play."
                                    customActions = listOf(
                                        CustomAccessibilityAction("Play ${channel.name}") { viewModel.onChannelSelected(channel); true }
                                    )
                                },
                            colors = if (isActive) ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.primaryContainer) else ListItemDefaults.colors()
                        )
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}
