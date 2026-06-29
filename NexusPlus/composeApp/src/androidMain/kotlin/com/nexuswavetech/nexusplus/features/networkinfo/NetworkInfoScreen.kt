package com.nexuswavetech.nexusplus.features.networkinfo

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.NetworkInterface

private data class NetworkDetails(
    val connectionType : String = "Checking...",
    val ssid           : String = "N/A",
    val localIp        : String = "N/A",
    val signalStrength : String = "N/A",
    val linkSpeed      : String = "N/A",
    val frequency      : String = "N/A",
    val publicIp       : String = "Fetching...",
    val dnsServer      : String = "N/A",
    val isLoaded       : Boolean = false,
)

@Suppress("DEPRECATION")
private suspend fun fetchNetworkDetails(context: Context): NetworkDetails {
    val cm  = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val wm  = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val net = cm.activeNetwork
    val cap = net?.let { cm.getNetworkCapabilities(it) }

    val connectionType = when {
        cap == null                                                -> "No connection"
        cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)     -> "Wi-Fi"
        cap.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile Data"
        cap.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
        else                                                      -> "Connected"
    }

    val wifiInfo = wm.connectionInfo
    val isWifi   = connectionType == "Wi-Fi"

    val ssid   = if (isWifi) wifiInfo?.ssid?.removeSurrounding("\"") ?: "N/A" else "N/A"
    val rssi   = wifiInfo?.rssi ?: Int.MIN_VALUE
    val signal = if (isWifi && rssi > Int.MIN_VALUE)
        "$rssi dBm  (${WifiManager.calculateSignalLevel(rssi, 5)}/5 bars)"
        else "N/A"
    val speed  = if (isWifi) wifiInfo?.linkSpeed?.let { "$it Mbps" } ?: "N/A" else "N/A"
    val freq   = if (isWifi) wifiInfo?.frequency?.let { "$it MHz"  } ?: "N/A" else "N/A"

    val localIp = withContext(Dispatchers.IO) {
        runCatching {
            NetworkInterface.getNetworkInterfaces()
                ?.toList()
                ?.flatMap { it.inetAddresses.toList() }
                ?.firstOrNull { !it.isLoopbackAddress && it is Inet4Address }
                ?.hostAddress ?: "N/A"
        }.getOrDefault("N/A")
    }

    val dns = System.getProperty("net.dns1") ?: "N/A"

    val publicIp = withContext(Dispatchers.IO) {
        runCatching {
            java.net.URL("https://api.ipify.org").readText(Charsets.UTF_8).trim()
        }.getOrDefault("Unable to fetch")
    }

    return NetworkDetails(
        connectionType = connectionType,
        ssid           = ssid,
        localIp        = localIp,
        signalStrength = signal,
        linkSpeed      = speed,
        frequency      = freq,
        publicIp       = publicIp,
        dnsServer      = dns,
        isLoaded       = true,
    )
}

@Composable
fun NetworkInfoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var details by remember { mutableStateOf(NetworkDetails()) }
    var loading by remember { mutableStateOf(true) }

    fun refresh() {
        loading = true
        details = NetworkDetails()
    }

    LaunchedEffect(loading) {
        if (loading) {
            details = fetchNetworkDetails(context)
            loading = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        NexusTopBar(
            title   = "Network Info",
            onBack  = onBack,
            actions = {
                IconButton(
                    onClick  = { refresh() },
                    modifier = Modifier.semantics { contentDescription = "Refresh network information" }
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                }
            }
        )

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Fetching network info...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ConnectionTypeCard(type = details.connectionType)

                if (details.ssid != "N/A") {
                    InfoGroup(title = "Wi-Fi") {
                        InfoRow(label = "Network (SSID)",    value = details.ssid,           copyable = false)
                        InfoRow(label = "Signal Strength",  value = details.signalStrength,  copyable = false)
                        InfoRow(label = "Link Speed",       value = details.linkSpeed,        copyable = false)
                        InfoRow(label = "Frequency",        value = details.frequency,        copyable = false)
                    }
                }

                InfoGroup(title = "IP Addresses") {
                    InfoRow(label = "Local IP",   value = details.localIp,  context = context)
                    InfoRow(label = "Public IP",  value = details.publicIp, context = context)
                    InfoRow(label = "DNS Server", value = details.dnsServer, copyable = false)
                }
            }
        }
    }
}

@Composable
private fun ConnectionTypeCard(type: String) {
    val icon = when (type) {
        "Wi-Fi"       -> Icons.Filled.Wifi
        "Mobile Data" -> Icons.Filled.SignalCellularAlt
        "Ethernet"    -> Icons.Filled.Cable
        "No connection" -> Icons.Filled.WifiOff
        else          -> Icons.Filled.NetworkCheck
    }
    val containerColor = if (type == "No connection")
        MaterialTheme.colorScheme.errorContainer
    else
        MaterialTheme.colorScheme.primaryContainer

    Card(
        colors   = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Connection type: $type" }
    ) {
        Row(
            modifier              = Modifier.padding(20.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint     = if (type == "No connection")
                    MaterialTheme.colorScheme.onErrorContainer
                else
                    MaterialTheme.colorScheme.onPrimaryContainer
            )
            Column {
                Text(
                    "Connection Type",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (type == "No connection")
                        MaterialTheme.colorScheme.onErrorContainer
                    else
                        MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    type,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (type == "No connection")
                        MaterialTheme.colorScheme.onErrorContainer
                    else
                        MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun InfoGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                title,
                style    = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color    = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            content()
        }
    }
}

@Composable
private fun InfoRow(
    label    : String,
    value    : String,
    copyable : Boolean = true,
    context  : Context? = null,
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .semantics { contentDescription = "$label: $value" },
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
            )
        }
        if (copyable && context != null && value != "N/A" && value != "Fetching...") {
            IconButton(
                onClick  = {
                    val cb = context.getSystemService(ClipboardManager::class.java)
                    cb?.setPrimaryClip(ClipData.newPlainText(label, value))
                },
                modifier = Modifier.semantics { contentDescription = "Copy $label to clipboard" }
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}
