package com.nexuswavetech.nexusplus.features.urlshortener

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ShortenedLink(
    val original: String,
    val shortened: String,
    val service: String,
    val createdAt: Long = System.currentTimeMillis(),
)

data class UrlShortenerUiState(
    val inputUrl: String = "",
    val isLoading: Boolean = false,
    val result: ShortenedLink? = null,
    val error: String? = null,
    val history: List<ShortenedLink> = emptyList(),
    val selectedService: String = "TinyURL",
    val copiedUrl: String? = null,
)

class UrlShortenerViewModel : ViewModel() {
    private val _state = MutableStateFlow(UrlShortenerUiState())
    val state = _state.asStateFlow()

    private val services = listOf("TinyURL", "is.gd", "v.gd")

    fun getServices() = services

    fun onUrlChange(url: String) {
        _state.value = _state.value.copy(inputUrl = url, error = null, result = null)
    }

    fun onServiceChange(service: String) {
        _state.value = _state.value.copy(selectedService = service)
    }

    fun shorten() {
        val raw = _state.value.inputUrl.trim()
        if (raw.isBlank()) {
            _state.value = _state.value.copy(error = "Please enter a URL")
            return
        }
        val url = if (!raw.startsWith("http://") && !raw.startsWith("https://")) "https://$raw" else raw

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, result = null)
            try {
                val shortened = when (_state.value.selectedService) {
                    "is.gd" -> shortenWithIsGd(url, vanity = false)
                    "v.gd"  -> shortenWithIsGd(url, vanity = true)
                    else    -> shortenWithTinyUrl(url)
                }
                val link = ShortenedLink(
                    original  = url,
                    shortened = shortened,
                    service   = _state.value.selectedService,
                )
                _state.value = _state.value.copy(
                    isLoading = false,
                    result    = link,
                    history   = listOf(link) + _state.value.history.take(19),
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error     = "Failed: ${e.message?.take(80) ?: "Network error"}",
                )
            }
        }
    }

    private fun shortenWithTinyUrl(longUrl: String): String {
        val apiUrl = "https://tinyurl.com/api-create.php?url=${java.net.URLEncoder.encode(longUrl, "UTF-8")}"
        val conn = URL(apiUrl).openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout    = 8000
        return conn.inputStream.bufferedReader().readText().trim()
    }

    private fun shortenWithIsGd(longUrl: String, vanity: Boolean): String {
        val base   = if (vanity) "https://v.gd/create.php" else "https://is.gd/create.php"
        val apiUrl = "$base?format=json&url=${java.net.URLEncoder.encode(longUrl, "UTF-8")}"
        val conn   = URL(apiUrl).openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout    = 8000
        val json = JSONObject(conn.inputStream.bufferedReader().readText())
        return json.getString("shorturl")
    }

    fun onCopied(url: String) {
        _state.value = _state.value.copy(copiedUrl = url)
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            _state.value = _state.value.copy(copiedUrl = null)
        }
    }

    fun clearHistory() {
        _state.value = _state.value.copy(history = emptyList())
    }
}

@Composable
fun UrlShortenerScreen(onBack: () -> Unit) {
    val vm = androidx.lifecycle.viewmodel.compose.viewModel<UrlShortenerViewModel>()
    val state by vm.state.collectAsState()
    val clipboard = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "URL Shortener", onBack = onBack)

        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Input card ──────────────────────────────────────────────
            item {
                Card(
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier  = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier            = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Link, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Text("Shorten a URL", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }

                        OutlinedTextField(
                            value           = state.inputUrl,
                            onValueChange   = vm::onUrlChange,
                            label           = { Text("Paste long URL here") },
                            placeholder     = { Text("https://example.com/very/long/path") },
                            singleLine      = true,
                            isError         = state.error != null,
                            supportingText  = state.error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                            leadingIcon     = { Icon(Icons.Filled.Language, null) },
                            trailingIcon    = if (state.inputUrl.isNotEmpty()) { {
                                IconButton(onClick = { vm.onUrlChange("") }) {
                                    Icon(Icons.Filled.Clear, "Clear")
                                }
                            } } else null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { vm.shorten() }),
                            modifier        = Modifier.fillMaxWidth(),
                        )

                        // Service selector
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            vm.getServices().forEach { svc ->
                                FilterChip(
                                    selected = state.selectedService == svc,
                                    onClick  = { vm.onServiceChange(svc) },
                                    label    = { Text(svc, style = MaterialTheme.typography.labelMedium) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }

                        Button(
                            onClick  = vm::shorten,
                            enabled  = !state.isLoading && state.inputUrl.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(Modifier.width(8.dp))
                                Text("Shortening…")
                            } else {
                                Icon(Icons.Filled.Compress, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Shorten URL")
                            }
                        }
                    }
                }
            }

            // ── Result card ─────────────────────────────────────────────
            item {
                AnimatedVisibility(
                    visible = state.result != null,
                    enter   = fadeIn() + expandVertically(),
                    exit    = fadeOut() + shrinkVertically(),
                ) {
                    state.result?.let { link ->
                        Card(
                            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier            = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                                    Text("Shortened!", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Spacer(Modifier.weight(1f))
                                    Surface(
                                        shape = MaterialTheme.shapes.extraSmall,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    ) {
                                        Text(
                                            link.service,
                                            style    = MaterialTheme.typography.labelSmall,
                                            color    = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        )
                                    }
                                }

                                Surface(
                                    shape    = MaterialTheme.shapes.small,
                                    color    = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier          = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            link.shortened,
                                            style    = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                            color    = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilledTonalButton(
                                        onClick  = {
                                            clipboard.setText(AnnotatedString(link.shortened))
                                            vm.onCopied(link.shortened)
                                        },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Icon(if (state.copiedUrl == link.shortened) Icons.Filled.Check else Icons.Filled.ContentCopy, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (state.copiedUrl == link.shortened) "Copied!" else "Copy")
                                    }
                                    FilledTonalButton(
                                        onClick  = { runCatching { uriHandler.openUri(link.shortened) } },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Icon(Icons.Filled.OpenInBrowser, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Open")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── History ─────────────────────────────────────────────────
            if (state.history.isNotEmpty()) {
                item {
                    Row(
                        modifier          = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "History",
                            style    = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = vm::clearHistory) {
                            Text("Clear All", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                items(state.history) { link ->
                    HistoryItem(
                        link      = link,
                        isCopied  = state.copiedUrl == link.shortened,
                        onCopy    = {
                            clipboard.setText(AnnotatedString(link.shortened))
                            vm.onCopied(link.shortened)
                        },
                        onOpen    = { runCatching { uriHandler.openUri(link.shortened) } },
                    )
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun HistoryItem(
    link: ShortenedLink,
    isCopied: Boolean,
    onCopy: () -> Unit,
    onOpen: () -> Unit,
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Row(
            modifier          = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Filled.Link, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    link.shortened,
                    style    = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color    = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    link.original,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "via ${link.service}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
            IconButton(onClick = onCopy, modifier = Modifier.size(36.dp)) {
                Icon(
                    if (isCopied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                    "Copy",
                    tint     = if (isCopied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
            IconButton(onClick = onOpen, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.OpenInBrowser, "Open", modifier = Modifier.size(18.dp))
            }
        }
    }
}
