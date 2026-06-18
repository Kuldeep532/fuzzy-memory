package com.nexuswavetech.nexusplus.features.voices

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexuswavetech.nexusplus.model.ModelDownloadManager
import com.nexuswavetech.nexusplus.model.ModelRegistry
import com.nexuswavetech.nexusplus.model.ModelRegistry.NexusModel
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import org.koin.compose.koinInject

/**
 * Nexus Plus — Download Voices Screen
 *
 * Shows all 28 verified Piper TTS voices (from ModelRegistry) grouped by language.
 * Each voice card shows: flag, name, size, quality, download status, and action button.
 * Download/cancel/delete operations are handled by [ModelDownloadManager].
 *
 * Sources: https://huggingface.co/rhasspy/piper-voices (official, v1.0.0)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadVoicesScreen(onBack: () -> Unit) {
    val downloadManager = koinInject<ModelDownloadManager>()
    val progressMap by downloadManager.progress.collectAsState()

    val voices = remember { ModelRegistry.allVoices() }
    val grouped = remember(voices) { voices.groupBy { it.language } }

    var searchQuery by remember { mutableStateOf("") }
    val filteredGrouped = remember(grouped, searchQuery) {
        if (searchQuery.isBlank()) grouped
        else grouped
            .mapValues { (_, vs) ->
                vs.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                    it.language.contains(searchQuery, ignoreCase = true) ||
                    it.locale.contains(searchQuery, ignoreCase = true)
                }
            }
            .filter { it.value.isNotEmpty() }
    }

    val totalDownloaded = remember(progressMap, voices) {
        voices.count { downloadManager.isDownloaded(it) }
    }

    Scaffold(
        topBar = {
            NexusTopBar(title = "Download Voices", onBack = onBack)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // ── Header info card ──────────────────────────────────────────
            item {
                Surface(
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(12.dp),
                    color         = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    tonalElevation = 1.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Filled.RecordVoiceOver,
                            contentDescription = null,
                            tint     = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Nexus Piper TTS Voices",
                                style      = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color      = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                "${voices.size} voices · ${filteredGrouped.size} languages · $totalDownloaded downloaded",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                            )
                            Text(
                                "Source: rhasspy/piper-voices (HuggingFace)",
                                style    = MaterialTheme.typography.labelSmall,
                                color    = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.55f),
                                fontSize = 10.sp,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // ── Search bar ───────────────────────────────────────────────
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search language or voice…") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Filled.Close, null) } }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
                Spacer(Modifier.height(8.dp))
            }

            // ── Voice groups ─────────────────────────────────────────────
            filteredGrouped.entries.sortedBy { it.key }.forEach { (language, voiceList) ->
                // Language header
                item(key = "header_$language") {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 4.dp),
                    ) {
                        Text(
                            voiceList.first().flagEmoji,
                            fontSize = 20.sp,
                        )
                        Text(
                            language,
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${voiceList.size} ${if (voiceList.size == 1) "voice" else "voices"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(Modifier.height(4.dp))
                }

                // Voice cards
                items(voiceList, key = { it.id }) { model ->
                    VoiceCard(
                        model      = model,
                        progress   = progressMap[model.id],
                        isDownloaded = downloadManager.isDownloaded(model),
                        onDownload = { downloadManager.enqueue(model) },
                        onCancel   = { downloadManager.cancel(model.id) },
                        onDelete   = { downloadManager.delete(model) },
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

// ── Voice Card ────────────────────────────────────────────────────────────────

@Composable
private fun VoiceCard(
    model       : NexusModel,
    progress    : ModelDownloadManager.DownloadProgress?,
    isDownloaded: Boolean,
    onDownload  : () -> Unit,
    onCancel    : () -> Unit,
    onDelete    : () -> Unit,
) {
    val isActive    = progress?.state == ModelDownloadManager.DownloadProgress.State.DOWNLOADING ||
                      progress?.state == ModelDownloadManager.DownloadProgress.State.QUEUED
    val isVerifying = progress?.state == ModelDownloadManager.DownloadProgress.State.VERIFYING
    val hasFailed   = progress?.state == ModelDownloadManager.DownloadProgress.State.FAILED

    Surface(
        modifier      = Modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(10.dp),
        color         = when {
            isDownloaded -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
            hasFailed    -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
            else         -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        },
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Flag + quality badge
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(model.flagEmoji, fontSize = 22.sp)
                    if (model.isDefault) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary,
                        ) {
                            Text(
                                "DEFAULT",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                style    = MaterialTheme.typography.labelSmall,
                                color    = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 8.sp,
                            )
                        }
                    }
                }

                // Name + meta
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        model.name,
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onSurface,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${model.locale} · ${formatSize(model.sizeBytes)} · ${model.quality.name.lowercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        model.description,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Status + action button
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    when {
                        isDownloaded && !isActive -> {
                            StatusChip("✓ Ready", MaterialTheme.colorScheme.secondary)
                            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.DeleteOutline, "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                        isActive || isVerifying -> {
                            IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Close, "Cancel", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                        hasFailed -> {
                            StatusChip("Failed", MaterialTheme.colorScheme.error)
                            IconButton(onClick = onDownload, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Refresh, "Retry", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                        }
                        else -> {
                            IconButton(onClick = onDownload, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Download, "Download", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // ── Progress bar ──────────────────────────────────────────────
            if (isActive || isVerifying) {
                val progressValue = progress?.let {
                    if (it.totalBytes > 0) it.bytesDownloaded.toFloat() / it.totalBytes.toFloat()
                    else null
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (progressValue != null) {
                        LinearProgressIndicator(
                            progress  = { progressValue },
                            modifier  = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)))
                    }
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            when (progress?.state) {
                                ModelDownloadManager.DownloadProgress.State.QUEUED      -> "Queued…"
                                ModelDownloadManager.DownloadProgress.State.VERIFYING   -> "Verifying integrity…"
                                ModelDownloadManager.DownloadProgress.State.DOWNLOADING -> "Downloading…"
                                else -> ""
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (progressValue != null && progress != null) {
                            Text(
                                "${progress.percent}% · ${formatSize(progress.bytesDownloaded)} / ${formatSize(progress.totalBytes)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Status chip ───────────────────────────────────────────────────────────────

@Composable
private fun StatusChip(label: String, color: androidx.compose.ui.graphics.Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            style    = MaterialTheme.typography.labelSmall,
            color    = color,
            fontSize = 10.sp,
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatSize(bytes: Long): String = when {
    bytes >= 1_000_000L -> "${"%.0f".format(bytes / 1_000_000.0)} MB"
    bytes >= 1_000L     -> "${"%.0f".format(bytes / 1_000.0)} KB"
    else                -> "$bytes B"
}
