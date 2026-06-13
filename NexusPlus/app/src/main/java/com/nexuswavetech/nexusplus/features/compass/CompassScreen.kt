package com.nexuswavetech.nexusplus.features.compass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlin.math.roundToInt

private fun degreesToDirection(deg: Float): String {
    val d = ((deg % 360) + 360) % 360
    return when {
        d < 22.5  -> "N"
        d < 67.5  -> "NE"
        d < 112.5 -> "E"
        d < 157.5 -> "SE"
        d < 202.5 -> "S"
        d < 247.5 -> "SW"
        d < 292.5 -> "W"
        d < 337.5 -> "NW"
        else      -> "N"
    }
}

@Composable
fun CompassScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val rotationSensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }
    val hasCompass = rotationSensor != null

    var azimuth by remember { mutableFloatStateOf(0f) }
    var pitch   by remember { mutableFloatStateOf(0f) }
    var roll    by remember { mutableFloatStateOf(0f) }

    DisposableEffect(hasCompass) {
        if (!hasCompass) return@DisposableEffect onDispose {}
        val rotMat    = FloatArray(9)
        val orientation = FloatArray(3)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotMat, event.values)
                SensorManager.getOrientation(rotMat, orientation)
                azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                pitch   = Math.toDegrees(orientation[1].toDouble()).toFloat()
                roll    = Math.toDegrees(orientation[2].toDouble()).toFloat()
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager.unregisterListener(listener) }
    }

    val displayAzimuth = ((azimuth % 360) + 360) % 360
    val animatedRotation by animateFloatAsState(
        targetValue   = -displayAzimuth,
        animationSpec = tween(durationMillis = 200),
        label         = "compass_rotation",
    )

    val primaryColor   = MaterialTheme.colorScheme.primary
    val surfaceColor   = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurfaceVariant
    val errorColor     = MaterialTheme.colorScheme.error
    val textMeasurer   = rememberTextMeasurer()

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "Compass", onBack = onBack)

        if (!hasCompass) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.Warning, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                    Text("No compass sensor available on this device.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                }
            }
            return@Column
        }

        Column(
            modifier            = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Compass dial
            Canvas(
                modifier = Modifier
                    .size(280.dp)
                    .semantics { contentDescription = "Compass pointing ${displayAzimuth.roundToInt()} degrees ${degreesToDirection(displayAzimuth)}" },
            ) {
                val radius  = size.minDimension / 2f
                val center  = Offset(size.width / 2f, size.height / 2f)

                // Background circle
                drawCircle(color = surfaceColor, radius = radius)

                rotate(animatedRotation, pivot = center) {
                    // Cardinal directions
                    val cardinals = listOf("N" to 0f, "E" to 90f, "S" to 180f, "W" to 270f)
                    cardinals.forEach { (label, angle) ->
                        rotate(angle, pivot = center) {
                            val textResult = textMeasurer.measure(
                                text  = label,
                                style = TextStyle(
                                    color      = if (label == "N") errorColor else onSurfaceColor,
                                    fontSize   = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                            )
                            drawText(
                                textLayoutResult = textResult,
                                topLeft          = Offset(
                                    center.x - textResult.size.width / 2f,
                                    center.y - radius + radius * 0.12f,
                                ),
                            )
                        }
                    }

                    // Tick marks
                    for (i in 0 until 360 step 5) {
                        rotate(i.toFloat(), pivot = center) {
                            val isMajor = i % 45 == 0
                            val start   = radius * (if (isMajor) 0.72f else 0.80f)
                            val end     = radius * 0.88f
                            drawLine(
                                color       = if (isMajor) onSurfaceColor else onSurfaceColor.copy(alpha = 0.4f),
                                start       = Offset(center.x, center.y - start),
                                end         = Offset(center.x, center.y - end),
                                strokeWidth = if (isMajor) 3f else 1.5f,
                            )
                        }
                    }
                }

                // Fixed North needle (red)
                drawLine(
                    color       = errorColor,
                    start       = center,
                    end         = Offset(center.x, center.y - radius * 0.6f),
                    strokeWidth = 6f,
                )
                // South needle (gray)
                drawLine(
                    color       = onSurfaceColor,
                    start       = center,
                    end         = Offset(center.x, center.y + radius * 0.4f),
                    strokeWidth = 6f,
                )
                // Center circle
                drawCircle(color = primaryColor, radius = 12f, center = center)
            }

            Spacer(Modifier.height(24.dp))

            // Direction display
            Text(
                degreesToDirection(displayAzimuth),
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "${displayAzimuth.roundToInt()}°",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(24.dp))

            // Tilt info
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Row(
                    modifier              = Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    TiltStat("Pitch", "${pitch.roundToInt()}°")
                    VerticalDivider(modifier = Modifier.height(40.dp))
                    TiltStat("Roll", "${roll.roundToInt()}°")
                    VerticalDivider(modifier = Modifier.height(40.dp))
                    TiltStat("Azimuth", "${displayAzimuth.roundToInt()}°")
                }
            }
        }
    }
}

@Composable
private fun TiltStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.semantics { contentDescription = "$label: $value" }) {
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
