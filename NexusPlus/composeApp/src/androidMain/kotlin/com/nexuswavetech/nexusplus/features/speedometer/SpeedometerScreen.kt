package com.nexuswavetech.nexusplus.features.speedometer

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlin.math.roundToInt

private const val MS_TO_KMH = 3.6f
private const val MS_TO_MPH = 2.23694f
private const val MAX_SPEED_KMH = 220f
private const val SWEEP_ANGLE   = 240f
private const val START_ANGLE   = 150f

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SpeedometerScreen(onBack: () -> Unit) {
    val context    = LocalContext.current
    val permission = rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)

    var speedMs  by remember { mutableFloatStateOf(0f) }
    var maxMs    by remember { mutableFloatStateOf(0f) }
    var totalMs  by remember { mutableFloatStateOf(0f) }
    var readings by remember { mutableIntStateOf(0) }
    var hasGps   by remember { mutableStateOf(false) }
    var altitude by remember { mutableDoubleStateOf(0.0) }
    var accuracy by remember { mutableFloatStateOf(0f) }

    val speedKmh = speedMs * MS_TO_KMH
    val maxKmh   = maxMs   * MS_TO_KMH
    val avgKmh   = if (readings > 0) (totalMs / readings) * MS_TO_KMH else 0f
    val speedMph = speedMs * MS_TO_MPH

    // GPS listener
    DisposableEffect(permission.status.isGranted) {
        if (!permission.status.isGranted) return@DisposableEffect onDispose {}
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val listener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                if (loc.hasSpeed()) {
                    speedMs  = loc.speed.coerceAtLeast(0f)
                    if (speedMs > maxMs) maxMs = speedMs
                    totalMs += speedMs
                    readings++
                    hasGps   = true
                    altitude = loc.altitude
                    accuracy = loc.accuracy
                }
            }
            @Suppress("OVERRIDE_DEPRECATION")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String)  { hasGps = true }
            override fun onProviderDisabled(provider: String) { hasGps = false }
        }
        try {
            @SuppressLint("MissingPermission")
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                hasGps = true
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500L, 0f, listener)
            }
        } catch (_: Exception) {}
        onDispose {
            try { lm.removeUpdates(listener) } catch (_: Exception) {}
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "Speedometer", onBack = onBack)

        when {
            !permission.status.isGranted -> {
                PermissionRequest(
                    showRationale = permission.status.shouldShowRationale,
                    onRequest     = { permission.launchPermissionRequest() },
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // GPS status chip
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = if (hasGps) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.errorContainer,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                if (hasGps) Icons.Filled.GpsFixed else Icons.Filled.GpsOff,
                                contentDescription = null, modifier = Modifier.size(16.dp),
                                tint = if (hasGps) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Text(
                                if (hasGps) "GPS Active" else "Searching GPS…",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (hasGps) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }

                    // Gauge
                    val animatedFraction by animateFloatAsState(
                        targetValue = (speedKmh / MAX_SPEED_KMH).coerceIn(0f, 1f),
                        animationSpec = tween(300),
                        label = "speedGauge",
                    )
                    val primary = MaterialTheme.colorScheme.primary
                    val track   = MaterialTheme.colorScheme.surfaceVariant

                    Box(contentAlignment = Alignment.Center) {
                        Canvas(
                            modifier = Modifier
                                .size(220.dp)
                                .semantics { contentDescription = "${speedKmh.roundToInt()} km/h" },
                        ) {
                            val strokeW = 22f
                            val inset   = strokeW / 2 + 4
                            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                            val topLeft = Offset(inset, inset)

                            drawArc(color = track, startAngle = START_ANGLE, sweepAngle = SWEEP_ANGLE,
                                useCenter = false, topLeft = topLeft, size = arcSize,
                                style = Stroke(width = strokeW, cap = StrokeCap.Round))

                            drawArc(color = primary, startAngle = START_ANGLE,
                                sweepAngle = SWEEP_ANGLE * animatedFraction,
                                useCenter = false, topLeft = topLeft, size = arcSize,
                                style = Stroke(width = strokeW, cap = StrokeCap.Round))
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${speedKmh.roundToInt()}",
                                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold, fontSize = 56.sp),
                            )
                            Text("km/h", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${speedMph.roundToInt()} mph", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }

                    // Stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        StatCard("Max Speed",  "${maxKmh.roundToInt()} km/h", Modifier.weight(1f))
                        StatCard("Avg Speed",  "${avgKmh.roundToInt()} km/h", Modifier.weight(1f))
                    }

                    // Additional info
                    if (hasGps && altitude != 0.0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            StatCard("Altitude",  "${altitude.roundToInt()} m", Modifier.weight(1f))
                            StatCard("Accuracy",  "±${accuracy.roundToInt()} m",  Modifier.weight(1f))
                        }
                    }

                    // Reset button
                    OutlinedButton(
                        onClick = { speedMs = 0f; maxMs = 0f; totalMs = 0f; readings = 0 },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Reset Statistics") }

                    // Disclaimer
                    Text(
                        "⚠ For reference only. Do not use while driving.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PermissionRequest(showRationale: Boolean, onRequest: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(Icons.Filled.Speed, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Location Permission Required", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
            Text(
                if (showRationale) "GPS location is needed to measure your speed accurately."
                else "Nexus Speedometer needs location access to read your GPS speed.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onRequest, modifier = Modifier.fillMaxWidth()) {
                Text("Grant Permission")
            }
        }
    }
}
