package com.nexuswavetech.nexusplus.features.flashlight

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private fun getCameraId(context: Context): String? {
    val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    return manager.cameraIdList.firstOrNull { id ->
        manager.getCameraCharacteristics(id)
            .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
    }
}

private fun setTorch(context: Context, cameraId: String?, on: Boolean) {
    if (cameraId == null) return
    val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    runCatching { manager.setTorchMode(cameraId, on) }
}

@Composable
fun FlashlightScreen(onBack: () -> Unit) {
    val context  = LocalContext.current
    val cameraId = remember { getCameraId(context) }
    val hasFlash = cameraId != null

    var isOn      by remember { mutableStateOf(false) }
    var strobeOn  by remember { mutableStateOf(false) }
    var strobeHz  by remember { mutableFloatStateOf(5f) }

    // Turn torch off when leaving
    DisposableEffect(Unit) {
        onDispose {
            if (isOn || strobeOn) {
                strobeOn = false
                isOn     = false
                setTorch(context, cameraId, false)
            }
        }
    }

    // Strobe effect
    LaunchedEffect(strobeOn, strobeHz) {
        if (strobeOn) {
            val intervalMs = (1000f / strobeHz).toLong().coerceAtLeast(50L)
            while (isActive && strobeOn) {
                setTorch(context, cameraId, true)
                delay(intervalMs / 2)
                setTorch(context, cameraId, false)
                delay(intervalMs / 2)
            }
        } else {
            setTorch(context, cameraId, isOn)
        }
    }

    // Steady torch sync
    LaunchedEffect(isOn, strobeOn) {
        if (!strobeOn) {
            setTorch(context, cameraId, isOn)
        }
    }

    val glowColor by animateColorAsState(
        targetValue = if (isOn || strobeOn) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = tween(300),
        label = "glow",
    )

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "Flashlight", onBack = onBack)

        Column(
            modifier            = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (!hasFlash) {
                Icon(Icons.Filled.FlashOff, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(16.dp))
                Text("No flash available on this device.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                return@Column
            }

            // ── Torch button ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .shadow(if (isOn || strobeOn) 32.dp else 0.dp, CircleShape, ambientColor = MaterialTheme.colorScheme.primary)
                    .clip(CircleShape)
                    .background(glowColor),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(
                    onClick  = {
                        if (strobeOn) {
                            strobeOn = false; isOn = false
                        } else {
                            isOn = !isOn
                        }
                    },
                    modifier = Modifier
                        .size(180.dp)
                        .semantics { contentDescription = if (isOn || strobeOn) "Turn flashlight off" else "Turn flashlight on" },
                ) {
                    Icon(
                        imageVector        = if (isOn || strobeOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                        contentDescription = null,
                        modifier           = Modifier.size(96.dp),
                        tint               = if (isOn || strobeOn) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                if (strobeOn) "STROBE ON" else if (isOn) "ON" else "OFF",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = if (isOn || strobeOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(40.dp))

            // ── Strobe section ───────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "Strobe Mode",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        )
                        Switch(
                            checked         = strobeOn,
                            onCheckedChange = { checked ->
                                strobeOn = checked
                                if (checked) isOn = false
                            },
                        )
                    }
                    if (strobeOn) {
                        Text(
                            "Frequency: ${strobeHz.toInt()} Hz",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Slider(
                            value         = strobeHz,
                            onValueChange = { strobeHz = it },
                            valueRange    = 1f..20f,
                            steps         = 18,
                            modifier      = Modifier.semantics { contentDescription = "Strobe frequency: ${strobeHz.toInt()} Hz" },
                        )
                    }
                }
            }
        }
    }
}
