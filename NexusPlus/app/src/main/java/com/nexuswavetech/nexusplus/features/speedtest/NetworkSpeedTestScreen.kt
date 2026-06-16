package com.nexuswavetech.nexusplus.features.speedtest

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import kotlin.math.min

private enum class TestState { IDLE, PING, DOWNLOAD, UPLOAD, DONE }

@Composable
fun NetworkSpeedTestScreen(onBack: () -> Unit) {
    val view = LocalView.current

    var state        by remember { mutableStateOf(TestState.IDLE) }
    var pingMs       by remember { mutableStateOf<Long?>(null) }
    var downloadMbps by remember { mutableStateOf<Double?>(null) }
    var uploadMbps   by remember { mutableStateOf<Double?>(null) }
    var error        by remember { mutableStateOf<String?>(null) }
    var liveSpeed    by remember { mutableStateOf(0.0) }

    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    suspend fun runTests() {
        error = null
        pingMs = null; downloadMbps = null; uploadMbps = null

        // ── Ping ──────────────────────────────────────────────────────────────
        state = TestState.PING
        pingMs = withContext(Dispatchers.IO) {
            runCatching {
                val url = "https://www.google.com"
                val t0  = System.currentTimeMillis()
                client.newCall(Request.Builder().url(url).head().build()).execute().use {}
                System.currentTimeMillis() - t0
            }.getOrElse { -1L }
        }
        delay(300)

        // ── Download ──────────────────────────────────────────────────────────
        state = TestState.DOWNLOAD
        downloadMbps = withContext(Dispatchers.IO) {
            runCatching {
                val url    = "https://httpbin.org/bytes/512000" // 512 KB test
                val t0     = System.currentTimeMillis()
                val bytes  = client.newCall(Request.Builder().url(url).build()).execute().use { r ->
                    r.body?.bytes()?.size?.toLong() ?: 0L
                }
                val elapsed = (System.currentTimeMillis() - t0).coerceAtLeast(1)
                (bytes * 8.0 / 1_000_000.0) / (elapsed / 1000.0)  // Mbps
            }.getOrElse { -1.0 }
        }.also { liveSpeed = it ?: 0.0 }
        delay(300)

        // ── Upload ────────────────────────────────────────────────────────────
        state = TestState.UPLOAD
        uploadMbps = withContext(Dispatchers.IO) {
            runCatching {
                val payload = ByteArray(256_000) { it.toByte() }    // 256 KB
                val body    = payload.toRequestBody("application/octet-stream".toMediaType())
                val t0      = System.currentTimeMillis()
                client.newCall(Request.Builder().url("https://httpbin.org/post").post(body).build()).execute().use {}
                val elapsed = (System.currentTimeMillis() - t0).coerceAtLeast(1)
                (payload.size * 8.0 / 1_000_000.0) / (elapsed / 1000.0)
            }.getOrElse { -1.0 }
        }.also { liveSpeed = it ?: 0.0 }

        state = TestState.DONE
        view.announceForAccessibility("Speed test complete. Download: ${downloadMbps?.let { "%.1f Mbps".format(it) } ?: "error"}")
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "Speed Test", onBack = onBack)

        Column(
            modifier            = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // ── Gauge ──────────────────────────────────────────────────────
            SpeedGauge(
                speed        = liveSpeed,
                maxSpeed     = 100.0,
                isRunning    = state in setOf(TestState.DOWNLOAD, TestState.UPLOAD),
                currentLabel = when (state) {
                    TestState.IDLE, TestState.DONE -> "Mbps"
                    TestState.PING     -> "Ping…"
                    TestState.DOWNLOAD -> "↓ Download"
                    TestState.UPLOAD   -> "↑ Upload"
                },
            )

            // ── Results row ────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ResultCard("Ping", pingMs?.let { if (it < 0) "Error" else "${it}ms" } ?: "—", Icons.Filled.Wifi, MaterialTheme.colorScheme.tertiary)
                ResultCard("Download", downloadMbps?.let { if (it < 0) "Error" else "%.1f Mbps".format(it) } ?: "—", Icons.Filled.ArrowDownward, MaterialTheme.colorScheme.primary)
                ResultCard("Upload", uploadMbps?.let { if (it < 0) "Error" else "%.1f Mbps".format(it) } ?: "—", Icons.Filled.ArrowUpward, MaterialTheme.colorScheme.secondary)
            }

            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            }

            var runCount by remember { mutableStateOf(0) }
            val isRunning = state !in setOf(TestState.IDLE, TestState.DONE)

            LaunchedEffect(runCount) {
                if (runCount > 0) runTests()
            }

            Button(
                onClick  = { if (!isRunning) runCount++ },
                enabled  = !isRunning,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                if (isRunning) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text(when (state) { TestState.PING -> "Measuring ping…"; TestState.DOWNLOAD -> "Testing download…"; else -> "Testing upload…" })
                } else {
                    Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (state == TestState.DONE) "Run Again" else "Start Test")
                }
            }

            Text(
                "Speed is measured by downloading a test file from httpbin.org. Results may vary by network.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SpeedGauge(speed: Double, maxSpeed: Double, isRunning: Boolean, currentLabel: String) {
    val animatedSpeed by animateFloatAsState(
        targetValue   = min(speed.toFloat(), maxSpeed.toFloat()),
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label         = "gauge_speed",
    )
    val sweep = (animatedSpeed / maxSpeed.toFloat()) * 240f

    val primary   = MaterialTheme.colorScheme.primary
    val surface   = MaterialTheme.colorScheme.surfaceVariant
    val secondary = MaterialTheme.colorScheme.secondary

    Box(modifier = Modifier.size(220.dp).semantics { contentDescription = "Speed gauge: ${"%.1f".format(speed)} Mbps" }, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke     = 20.dp.toPx()
            val inset      = stroke / 2
            val arcRect    = Size(size.width - stroke, size.height - stroke)
            val startAngle = 150f
            drawArc(color = surface, startAngle = startAngle, sweepAngle = 240f, useCenter = false, topLeft = Offset(inset, inset), size = arcRect, style = Stroke(stroke, cap = StrokeCap.Round))
            if (sweep > 0) {
                drawArc(color = if (speed > maxSpeed * 0.8) secondary else primary, startAngle = startAngle, sweepAngle = sweep, useCenter = false, topLeft = Offset(inset, inset), size = arcRect, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("%.1f".format(animatedSpeed), style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 40.sp), color = primary)
            Text(currentLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RowScope.ResultCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Card(modifier = Modifier.weight(1f)) {
        Column(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, null, modifier = Modifier.size(20.dp), tint = color)
            Text(value, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
