package com.nexuswavetech.nexusplus.features.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar

private data class BatteryInfo(
    val level: Int = 0,
    val isCharging: Boolean = false,
    val chargePlugin: String = "—",
    val voltage: Float = 0f,
    val temperature: Float = 0f,
    val health: String = "—",
    val technology: String = "—",
)

private fun decodePlug(plugType: Int) = when (plugType) {
    BatteryManager.BATTERY_PLUGGED_AC  -> "AC Charger"
    BatteryManager.BATTERY_PLUGGED_USB -> "USB"
    BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
    else -> "Not charging"
}

private fun decodeHealth(h: Int) = when (h) {
    BatteryManager.BATTERY_HEALTH_GOOD             -> "Good"
    BatteryManager.BATTERY_HEALTH_OVERHEAT         -> "Overheating"
    BatteryManager.BATTERY_HEALTH_DEAD             -> "Dead"
    BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE     -> "Over Voltage"
    BatteryManager.BATTERY_HEALTH_COLD             -> "Cold"
    BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
    else -> "Unknown"
}

@Composable
fun BatteryMonitorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var info by remember { mutableStateOf(BatteryInfo()) }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val lvl     = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale   = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                val status  = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val plug    = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
                val volts   = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) / 1000f
                val tempC   = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f
                val health  = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
                val tech    = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "—"
                val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                               status == BatteryManager.BATTERY_STATUS_FULL
                info = BatteryInfo(
                    level       = (lvl * 100f / scale).toInt(),
                    isCharging  = charging,
                    chargePlugin = if (charging) decodePlug(plug) else "Not charging",
                    voltage     = volts,
                    temperature = tempC,
                    health      = decodeHealth(health),
                    technology  = tech,
                )
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose { context.unregisterReceiver(receiver) }
    }

    val batteryColor = when {
        info.level >= 70                          -> MaterialTheme.colorScheme.tertiary
        info.level >= 30                          -> MaterialTheme.colorScheme.secondary
        else                                      -> MaterialTheme.colorScheme.error
    }

    val progress by animateFloatAsState(
        targetValue   = info.level / 100f,
        animationSpec = tween(600),
        label         = "battery_progress",
    )

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "Battery Monitor", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // ── Battery gauge ─────────────────────────────────────────────
            Card(
                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Battery level ${info.level} percent. ${if (info.isCharging) "Charging via ${info.chargePlugin}." else "Not charging."}" },
            ) {
                Column(
                    modifier            = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(
                        imageVector = when {
                            info.isCharging && info.level >= 80 -> Icons.Filled.BatteryChargingFull
                            info.isCharging                     -> Icons.Filled.Battery3Bar
                            info.level >= 80                    -> Icons.Filled.BatteryFull
                            info.level >= 50                    -> Icons.Filled.Battery3Bar
                            info.level >= 20                    -> Icons.Filled.Battery1Bar
                            else                                -> Icons.Filled.Battery0Bar
                        },
                        contentDescription = null,
                        tint               = batteryColor,
                        modifier           = Modifier.size(64.dp),
                    )
                    Text(
                        "${info.level}%",
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = batteryColor,
                    )
                    LinearProgressIndicator(
                        progress         = { progress },
                        modifier         = Modifier.fillMaxWidth().height(12.dp),
                        color            = batteryColor,
                        strokeCap        = StrokeCap.Round,
                        trackColor       = MaterialTheme.colorScheme.surfaceContainerHighest,
                    )
                    Text(
                        if (info.isCharging) "⚡ Charging via ${info.chargePlugin}" else "Not charging",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = if (info.isCharging) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ── Stats grid ────────────────────────────────────────────────
            Card(
                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("Battery Details", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.primary)
                    BatRow(Icons.Filled.Thermostat, "Temperature", "%.1f °C".format(info.temperature))
                    HorizontalDivider()
                    BatRow(Icons.Filled.ElectricBolt, "Voltage", "%.2f V".format(info.voltage))
                    HorizontalDivider()
                    BatRow(Icons.Filled.HealthAndSafety, "Health", info.health)
                    HorizontalDivider()
                    BatRow(Icons.Filled.Memory, "Technology", info.technology)
                }
            }

            // ── Tips ──────────────────────────────────────────────────────
            if (info.temperature > 40f) {
                Card(
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error)
                        Text("Battery temperature is high (${info.temperature}°C). Remove case and reduce usage.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }
    }
}

@Composable
private fun BatRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$label: $value" },
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
    }
}
