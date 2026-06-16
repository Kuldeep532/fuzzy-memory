package com.nexuswavetech.nexusplus.features.storage

import android.os.Environment
import android.os.StatFs
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar

private data class StorageStats(
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
)

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000L -> "%.1f GB".format(bytes / 1_000_000_000.0)
        bytes >= 1_000_000L     -> "%.1f MB".format(bytes / 1_000_000.0)
        bytes >= 1_000L         -> "%.1f KB".format(bytes / 1_000.0)
        else                    -> "$bytes B"
    }
}

private fun getStorageStats(path: String): StorageStats? {
    return runCatching {
        val stat  = StatFs(path)
        val total = stat.totalBytes
        val free  = stat.availableBytes
        StorageStats(totalBytes = total, usedBytes = total - free, freeBytes = free)
    }.getOrNull()
}

@Composable
fun StorageAnalyzerScreen(onBack: () -> Unit) {
    val internalPath = Environment.getDataDirectory().absolutePath
    val externalPath = Environment.getExternalStorageDirectory().absolutePath

    val internal = remember { getStorageStats(internalPath) }
    val external = remember { getStorageStats(externalPath) }

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "Storage Analyzer", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            // Internal storage
            internal?.let { stats ->
                StorageCard(
                    title     = "Internal Storage",
                    icon      = Icons.Filled.Storage,
                    stats     = stats,
                    cardColor = MaterialTheme.colorScheme.primaryContainer,
                )
            }

            // External storage
            external?.let { stats ->
                StorageCard(
                    title     = "External Storage / SD Card",
                    icon      = Icons.Filled.SdCard,
                    stats     = stats,
                    cardColor = MaterialTheme.colorScheme.secondaryContainer,
                )
            }

            // Categories estimation
            internal?.let { stats ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Estimated Breakdown", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.primary)

                        val used = stats.usedBytes
                        val cats = listOf(
                            Triple("System & Apps",    Icons.Filled.Android,      MaterialTheme.colorScheme.primary),
                            Triple("Media & Photos",   Icons.Filled.PhotoLibrary, MaterialTheme.colorScheme.secondary),
                            Triple("Documents",        Icons.Filled.Description,  MaterialTheme.colorScheme.tertiary),
                            Triple("Other / Cache",    Icons.Filled.Folder,       MaterialTheme.colorScheme.outline),
                        )
                        val ratios = listOf(0.40f, 0.30f, 0.15f, 0.15f)

                        cats.forEachIndexed { i, (label, icon, color) ->
                            val estBytes = (used * ratios[i]).toLong()
                            Row(
                                modifier              = Modifier
                                    .fillMaxWidth()
                                    .semantics { contentDescription = "$label: approximately ${formatBytes(estBytes)}" },
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
                                Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                Text(
                                    "~${formatBytes(estBytes)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            // Tips
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("💡 Storage Tips", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onTertiaryContainer)
                    listOf(
                        "Clear app caches via Settings → Apps",
                        "Move photos to cloud storage",
                        "Uninstall unused apps",
                        "Delete downloaded files you no longer need",
                    ).forEach { tip ->
                        Text("• $tip", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageCard(
    title: String,
    icon: ImageVector,
    stats: StorageStats,
    cardColor: androidx.compose.ui.graphics.Color,
) {
    val usedFraction = (stats.usedBytes.toFloat() / stats.totalBytes.toFloat()).coerceIn(0f, 1f)
    val usedPct      = (usedFraction * 100).toInt()
    val progress by animateFloatAsState(
        targetValue   = usedFraction,
        animationSpec = tween(800),
        label         = "storage_progress",
    )
    val barColor = when {
        usedFraction > 0.9f -> MaterialTheme.colorScheme.error
        usedFraction > 0.7f -> MaterialTheme.colorScheme.secondary
        else                -> MaterialTheme.colorScheme.tertiary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$title: ${formatBytes(stats.usedBytes)} used of ${formatBytes(stats.totalBytes)} total ($usedPct%). ${formatBytes(stats.freeBytes)} free." },
        colors   = CardDefaults.cardColors(containerColor = cardColor),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(icon, null, modifier = Modifier.size(28.dp))
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }

            LinearProgressIndicator(
                progress         = { progress },
                modifier         = Modifier.fillMaxWidth().height(10.dp),
                color            = barColor,
                strokeCap        = StrokeCap.Round,
                trackColor       = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Used", style = MaterialTheme.typography.bodySmall)
                    Text(formatBytes(stats.usedBytes), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$usedPct%", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold))
                    Text("used", style = MaterialTheme.typography.bodySmall)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Free", style = MaterialTheme.typography.bodySmall)
                    Text(formatBytes(stats.freeBytes), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text("Total: ${formatBytes(stats.totalBytes)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }
    }
}
