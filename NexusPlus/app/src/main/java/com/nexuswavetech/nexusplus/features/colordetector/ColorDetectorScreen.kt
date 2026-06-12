package com.nexuswavetech.nexusplus.features.colordetector

import android.Manifest
import android.graphics.Bitmap
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.palette.graphics.Palette
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

// ── Colour name lookup — pure Kotlin, no API ─────────────────────────────────

private val COLOR_NAMES = mapOf(
    0xFF0000 to "Red",          0xFF4500 to "Orange Red",    0xFFA500 to "Orange",
    0xFFFF00 to "Yellow",       0x9ACD32 to "Yellow Green",  0x008000 to "Green",
    0x006400 to "Dark Green",   0x00FFFF to "Cyan",          0x0000FF to "Blue",
    0x00008B to "Dark Blue",    0x8A2BE2 to "Violet",        0xFF00FF to "Magenta",
    0xFF69B4 to "Hot Pink",     0xFFC0CB to "Pink",          0x800000 to "Maroon",
    0x808000 to "Olive",        0x008080 to "Teal",          0x800080 to "Purple",
    0xFFFFFF to "White",        0x000000 to "Black",         0x808080 to "Gray",
    0xC0C0C0 to "Silver",       0xD2691E to "Chocolate",     0xA52A2A to "Brown",
    0xF5DEB3 to "Wheat",        0xFAEBD7 to "Antique White", 0xF0E68C to "Khaki",
    0xADD8E6 to "Light Blue",   0x90EE90 to "Light Green",   0xFFB6C1 to "Light Pink",
    0x20B2AA to "Light Sea Green", 0x778899 to "Slate Gray", 0x4682B4 to "Steel Blue",
    0xDC143C to "Crimson",      0xFF7F50 to "Coral",         0x6495ED to "Cornflower Blue",
    0xE9967A to "Dark Salmon",  0x8FBC8F to "Dark Sea Green"
)

fun nearestColorName(argb: Int): String {
    val r = (argb shr 16) and 0xFF
    val g = (argb shr  8) and 0xFF
    val b = (argb       ) and 0xFF
    return COLOR_NAMES.minByOrNull { (hex, _) ->
        val cr = (hex.toInt() shr 16) and 0xFF
        val cg = (hex.toInt() shr  8) and 0xFF
        val cb = (hex.toInt()       ) and 0xFF
        (r - cr) * (r - cr) + (g - cg) * (g - cg) + (b - cb) * (b - cb)
    }?.value ?: "Unknown"
}

fun argbToHex(argb: Int): String = "#%06X".format(argb and 0xFFFFFF)

fun argbToRgb(argb: Int): String {
    val r = (argb shr 16) and 0xFF
    val g = (argb shr  8) and 0xFF
    val b = (argb       ) and 0xFF
    return "rgb($r, $g, $b)"
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ColorDetectorScreen(onBack: () -> Unit) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val clipboard      = LocalClipboardManager.current
    val view           = LocalView.current
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    var dominantColor  by remember { mutableStateOf<Int?>(null) }
    var palette        by remember { mutableStateOf<List<Int>>(emptyList()) }
    var frozen         by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        NexusTopBar(title = "Colour Detector", onBack = onBack)

        if (!cameraPermission.status.isGranted) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(Icons.Filled.CameraAlt, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("Camera Permission Required", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Button(onClick = { cameraPermission.launchPermissionRequest() }) { Text("Grant Permission") }
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                // Camera preview
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).also { pv ->
                                val provider = ProcessCameraProvider.getInstance(ctx)
                                provider.addListener({
                                    val cameraProvider = provider.get()
                                    val preview = Preview.Builder().build().apply {
                                        setSurfaceProvider(pv.surfaceProvider)
                                    }
                                    val analysis = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()
                                        .apply {
                                            setAnalyzer(ContextCompat.getMainExecutor(ctx)) { proxy ->
                                                if (!frozen) {
                                                    val bmp = proxy.toBitmap()
                                                        .let { Bitmap.createScaledBitmap(it, 100, 100, false) }
                                                    Palette.from(bmp).generate { p ->
                                                        if (p != null) {
                                                            dominantColor = p.getDominantColor(0xFF808080.toInt())
                                                            palette = listOfNotNull(
                                                                p.vibrantSwatch?.rgb,
                                                                p.mutedSwatch?.rgb,
                                                                p.lightVibrantSwatch?.rgb,
                                                                p.darkVibrantSwatch?.rgb,
                                                                p.lightMutedSwatch?.rgb,
                                                                p.darkMutedSwatch?.rgb
                                                            )
                                                        }
                                                    }
                                                }
                                                proxy.close()
                                            }
                                        }
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        analysis
                                    )
                                }, ContextCompat.getMainExecutor(ctx))
                            }
                        },
                        modifier = Modifier.fillMaxSize().semantics {
                            contentDescription = "Camera preview for colour detection. Dominant colour is shown below."
                        }
                    )

                    // Crosshair
                    Box(
                        Modifier
                            .size(40.dp)
                            .align(Alignment.Center)
                            .background(Color.White.copy(alpha = 0.3f), CircleShape)
                    )

                    // Freeze button
                    FloatingActionButton(
                        onClick  = {
                            frozen = !frozen
                            view.announceForAccessibility(if (frozen) "Camera frozen. Colour values locked." else "Camera resumed.")
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .semantics { contentDescription = if (frozen) "Resume camera. Unfreeze colour detection." else "Freeze camera. Lock current colour values." }
                    ) {
                        Icon(if (frozen) Icons.Filled.PlayArrow else Icons.Filled.Pause, null)
                    }
                }

                // Colour info panel
                dominantColor?.let { argb ->
                    val hex      = argbToHex(argb)
                    val rgb      = argbToRgb(argb)
                    val name     = nearestColorName(argb)
                    val composeColor = Color(argb)

                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .semantics {
                                    contentDescription = "Detected colour: $name. Hex: $hex. RGB: $rgb"
                                },
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(64.dp)
                                    .background(composeColor, MaterialTheme.shapes.medium)
                            )
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(name,  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
                                Text(hex,   style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.primary)
                                Text(rgb,   style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),  color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(
                                onClick  = {
                                    clipboard.setText(AnnotatedString(hex))
                                    view.announceForAccessibility("$name hex code $hex copied to clipboard")
                                },
                                modifier = Modifier.semantics { contentDescription = "Copy hex code $hex" }
                            ) { Icon(Icons.Filled.ContentCopy, null) }
                        }

                        // Palette swatches
                        if (palette.isNotEmpty()) {
                            Text("Palette", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                palette.take(6).forEach { swatchArgb ->
                                    Box(
                                        Modifier
                                            .size(36.dp)
                                            .background(Color(swatchArgb), CircleShape)
                                            .semantics { contentDescription = "${nearestColorName(swatchArgb)} ${argbToHex(swatchArgb)}" }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
