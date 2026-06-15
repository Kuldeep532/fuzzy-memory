package com.nexuswavetech.nexusplus.features.wifi

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar

private data class NetworkInfo(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val frequency: Int,
    val capabilities: String,
)

private fun rssiToPercent(rssi: Int) = when {
    rssi >= -50 -> 100
    rssi <= -100 -> 0
    else -> 2 * (rssi + 100)
}

private fun rssiToLabel(rssi: Int) = when {
    rssi >= -50  -> "Excellent"
    rssi >= -60  -> "Good"
    rssi >= -70  -> "Fair"
    rssi >= -80  -> "Weak"
    else         -> "Poor"
}

private fun rssiToColor(rssi: Int) = when {
    rssi >= -60  -> Color(0xFF4CAF50)
    rssi >= -70  -> Color(0xFFFFC107)
    else         -> Color(0xFFF44336)
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WifiAnalyzerScreen(onBack: () -> Unit) {
    val context      = LocalContext.current
    val wifiManager  = remember { context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager }
    val locPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    var networks  by remember { mutableStateOf<List<NetworkInfo>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }
    var snackState = remember { SnackbarHostState() }

    fun doScan() {
        isScanning = true
        @Suppress("DEPRECATION")
        val started = wifiManager.startScan()
        if (!started) {
            // Use cached results on newer Android
            val cached = wifiManager.scanResults
            networks  = cached.map {
                NetworkInfo(
                    ssid         = if (it.wifiSsid != null) it.wifiSsid.toString().removeSurrounding("\"") else @Suppress("DEPRECATION") it.SSID.removeSurrounding("\""),
                    bssid        = it.BSSID,
                    rssi         = it.level,
                    frequency    = it.frequency,
                    capabilities = it.capabilities,
                )
            }.sortedByDescending { it.rssi }
            isScanning = false
        }
    }

    DisposableEffect(locPermission.status.isGranted) {
        if (!locPermission.status.isGranted) return@DisposableEffect onDispose {}
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val results = wifiManager.scanResults
                networks = results.map {
                    NetworkInfo(
                        ssid         = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && it.wifiSsid != null)
                                           it.wifiSsid.toString().removeSurrounding("\"")
                                       else @Suppress("DEPRECATION") it.SSID.removeSurrounding("\""),
                        bssid        = it.BSSID,
                        rssi         = it.level,
                        frequency    = it.frequency,
                        capabilities = it.capabilities,
                    )
                }.sortedByDescending { it.rssi }
                isScanning = false
            }
        }
        context.registerReceiver(receiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        doScan()
        onDispose { context.unregisterReceiver(receiver) }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackState) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            NexusTopBar(title = "Wi-Fi Analyzer", onBack = onBack, actions = {
                if (locPermission.status.isGranted) {
                    IconButton(onClick = ::doScan, enabled = !isScanning) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Scan Wi-Fi networks")
                    }
                }
            })

            if (!locPermission.status.isGranted) {
                Column(
                    modifier            = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Filled.LocationOn, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(16.dp))
                    Text("Location permission is required to scan Wi-Fi networks on Android.", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { locPermission.launchPermissionRequest() }) { Text("Grant Permission") }
                }
                return@Column
            }

            if (isScanning) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        CircularProgressIndicator()
                        Text("Scanning for networks…", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                return@Column
            }

            // Connected network banner
            @Suppress("DEPRECATION")
            val connInfo = wifiManager.connectionInfo
            if (connInfo != null && connInfo.ssid.isNotBlank() && connInfo.ssid != "<unknown ssid>") {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Filled.Wifi, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Column {
                            Text("Connected", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            Text(connInfo.ssid.removeSurrounding("\""), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("Signal: ${rssiToLabel(connInfo.rssi)} (${connInfo.rssi} dBm)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }

            if (networks.isEmpty()) {
                Box(Modifier.fillMaxSize().semantics { contentDescription = "No networks found. Tap refresh to scan." }, contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Filled.WifiOff, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Text("No networks found", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FilledTonalButton(onClick = ::doScan) { Icon(Icons.Filled.Refresh, null); Spacer(Modifier.width(8.dp)); Text("Scan") }
                    }
                }
            } else {
                Text("${networks.size} networks found", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(networks, key = { _, n -> n.bssid }) { _, net ->
                        val pct   = rssiToPercent(net.rssi)
                        val color = rssiToColor(net.rssi)
                        val label = rssiToLabel(net.rssi)
                        val isSecure = net.capabilities.contains("WPA") || net.capabilities.contains("WEP")
                        val band = if (net.frequency >= 5000) "5 GHz" else "2.4 GHz"
                        val progress by animateFloatAsState(targetValue = pct / 100f, animationSpec = tween(500), label = "signal")

                        Card(
                            modifier = Modifier.fillMaxWidth().semantics {
                                contentDescription = "${net.ssid}. Signal: $label ($pct%). Band: $band. Security: ${if (isSecure) "Secured" else "Open"}."
                            },
                            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(if (isSecure) Icons.Filled.Lock else Icons.Filled.LockOpen, null, tint = color, modifier = Modifier.size(20.dp))
                                    Text(net.ssid.ifBlank { "(Hidden Network)" }, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f))
                                    Text(band, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(6.dp), color = color, strokeCap = StrokeCap.Round, trackColor = MaterialTheme.colorScheme.surfaceContainerHighest)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${net.rssi} dBm · $label", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${net.frequency} MHz", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
