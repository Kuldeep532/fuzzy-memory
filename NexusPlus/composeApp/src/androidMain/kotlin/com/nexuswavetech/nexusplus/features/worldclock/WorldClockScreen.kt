package com.nexuswavetech.nexusplus.features.worldclock

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.delay
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm:ss a")
private val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")

private val popularZones = listOf(
    "Asia/Kolkata"         to "India (IST)",
    "America/New_York"     to "New York (EST/EDT)",
    "America/Los_Angeles"  to "Los Angeles (PST/PDT)",
    "Europe/London"        to "London (GMT/BST)",
    "Europe/Paris"         to "Paris (CET/CEST)",
    "Asia/Tokyo"           to "Tokyo (JST)",
    "Asia/Dubai"           to "Dubai (GST)",
    "Asia/Singapore"       to "Singapore (SGT)",
    "Australia/Sydney"     to "Sydney (AEST/AEDT)",
    "America/Chicago"      to "Chicago (CST/CDT)",
    "America/Sao_Paulo"    to "São Paulo (BRT)",
    "Asia/Shanghai"        to "Shanghai (CST)",
    "Europe/Berlin"        to "Berlin (CET/CEST)",
    "Asia/Seoul"           to "Seoul (KST)",
    "Africa/Cairo"         to "Cairo (EET)",
    "Pacific/Auckland"     to "Auckland (NZST/NZDT)",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldClockScreen(onBack: () -> Unit) {
    var activeZones by remember {
        mutableStateOf(listOf("Asia/Kolkata", "America/New_York", "Europe/London", "Asia/Tokyo"))
    }
    var currentTime by remember { mutableStateOf(ZonedDateTime.now()) }
    var showPicker  by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = ZonedDateTime.now()
            delay(1_000L)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(
            title = "World Clock",
            onBack = onBack,
            actions = {
                IconButton(
                    onClick = { showPicker = true },
                    modifier = Modifier.semantics { contentDescription = "Add time zone" },
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                }
            }
        )

        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(activeZones, key = { it }) { zoneId ->
                val zone    = ZoneId.of(zoneId)
                val zonedDt = currentTime.withZoneSameInstant(zone)
                val label   = popularZones.firstOrNull { it.first == zoneId }?.second ?: zoneId.replace("_", " ")
                val timeStr = zonedDt.format(timeFormatter)
                val dateStr = zonedDt.format(dateFormatter)
                val offsetH = zonedDt.offset.totalSeconds / 3600
                val offsetM = kotlin.math.abs(zonedDt.offset.totalSeconds % 3600 / 60)
                val offset  = if (offsetM == 0) "UTC%+d".format(offsetH)
                              else "UTC%+d:%02d".format(offsetH, offsetM)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "$label. Time: $timeStr. Date: $dateStr. Offset: $offset" },
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text  = label,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text  = "$dateStr · $offset",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text  = timeStr,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        if (activeZones.size > 1) {
                            IconButton(
                                onClick  = { activeZones = activeZones - zoneId },
                                modifier = Modifier.semantics { contentDescription = "Remove $label" },
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPicker) {
        val available = popularZones.filter { it.first !in activeZones }
        AlertDialog(
            onDismissRequest = { showPicker = false },
            icon             = { Icon(Icons.Filled.Language, contentDescription = null) },
            title            = { Text("Add Time Zone") },
            text             = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(available) { (zoneId, label) ->
                        TextButton(
                            onClick  = {
                                activeZones = activeZones + zoneId
                                showPicker  = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "Add $label" },
                        ) {
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    if (available.isEmpty()) {
                        item { Text("All popular zones added.", modifier = Modifier.padding(16.dp)) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            },
        )
    }
}
