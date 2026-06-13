package com.nexuswavetech.nexusplus.features.stopwatch

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlagCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.delay

private fun formatElapsed(ms: Long): String {
    val minutes = (ms / 60_000) % 60
    val seconds = (ms / 1_000) % 60
    val centis  = (ms / 10) % 100
    return "%02d:%02d.%02d".format(minutes, seconds, centis)
}

@Composable
fun StopwatchScreen(onBack: () -> Unit) {
    var elapsedMs  by remember { mutableLongStateOf(0L) }
    var isRunning  by remember { mutableStateOf(false) }
    var laps       by remember { mutableStateOf<List<Long>>(emptyList()) }
    var lastLapMs  by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isRunning) {
        if (isRunning) {
            val startTime = System.currentTimeMillis() - elapsedMs
            while (isRunning) {
                elapsedMs = System.currentTimeMillis() - startTime
                delay(10L)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "Stopwatch", onBack = onBack)

        Column(
            modifier            = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(32.dp))

            // ── Main timer display ────────────────────────────────────────
            Box(
                modifier         = Modifier
                    .size(240.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text  = formatElapsed(elapsedMs),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 36.sp,
                    ),
                    color    = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.semantics {
                        contentDescription = "Elapsed time: ${formatElapsed(elapsedMs)}"
                    },
                )
            }

            Spacer(Modifier.height(8.dp))

            // Lap delta
            if (laps.isNotEmpty()) {
                val lapDelta = elapsedMs - lastLapMs
                Text(
                    "Lap time: ${formatElapsed(lapDelta)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(32.dp))

            // ── Controls ─────────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                verticalAlignment     = Alignment.CenterVertically,
                modifier              = Modifier.fillMaxWidth(),
            ) {
                // Reset
                FilledTonalIconButton(
                    onClick  = {
                        isRunning = false
                        elapsedMs  = 0L
                        lastLapMs  = 0L
                        laps       = emptyList()
                    },
                    modifier = Modifier.size(56.dp).semantics { contentDescription = "Reset stopwatch" },
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(28.dp))
                }

                // Start / Pause
                FloatingActionButton(
                    onClick      = { isRunning = !isRunning },
                    containerColor = if (isRunning) MaterialTheme.colorScheme.errorContainer
                                     else MaterialTheme.colorScheme.primaryContainer,
                    modifier     = Modifier.size(72.dp).semantics {
                        contentDescription = if (isRunning) "Pause stopwatch" else "Start stopwatch"
                    },
                ) {
                    AnimatedContent(targetState = isRunning, label = "play_pause") { running ->
                        Icon(
                            if (running) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier           = Modifier.size(36.dp),
                            tint               = if (running) MaterialTheme.colorScheme.onErrorContainer
                                                 else MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }

                // Lap
                FilledTonalIconButton(
                    onClick  = {
                        if (isRunning) {
                            laps      = laps + elapsedMs
                            lastLapMs = elapsedMs
                        }
                    },
                    enabled  = isRunning,
                    modifier = Modifier.size(56.dp).semantics { contentDescription = "Record lap" },
                ) {
                    Icon(Icons.Filled.FlagCircle, contentDescription = null, modifier = Modifier.size(28.dp))
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
        }

        // ── Lap list ─────────────────────────────────────────────────────
        if (laps.isNotEmpty()) {
            LazyColumn(
                modifier       = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            ) {
                itemsIndexed(laps.reversed()) { reversedIndex, lapMs ->
                    val lapNum   = laps.size - reversedIndex
                    val prevMs   = if (reversedIndex == laps.size - 1) 0L else laps[laps.size - reversedIndex - 2]
                    val lapDelta = lapMs - prevMs
                    val isLast   = reversedIndex == 0

                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .semantics { contentDescription = "Lap $lapNum: ${formatElapsed(lapDelta)}, total ${formatElapsed(lapMs)}" },
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "Lap $lapNum",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            formatElapsed(lapDelta),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                            ),
                            color = if (isLast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            formatElapsed(lapMs),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (reversedIndex < laps.size - 1) HorizontalDivider(Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}
