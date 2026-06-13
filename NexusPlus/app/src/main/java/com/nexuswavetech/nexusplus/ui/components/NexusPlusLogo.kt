package com.nexuswavetech.nexusplus.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Nexus Plus programmatic logo — generated entirely via Compose Canvas APIs.
 * No raster assets, no SVG files, no external dependencies.
 *
 * Visual identity:
 *   • Outer hexagon → six-sided precision (Nexus structure)
 *   • Inner sine-wave arc → Nexus Wave Technologies signature
 *   • Two signal nodes (circles) at wave start / end
 *
 * Colour defaults match the Nexus brand palette; override for dark/light contexts.
 */
@Composable
fun NexusPlusLogo(
    modifier: Modifier = Modifier,
    primaryColor: Color = Color(0xFF4A3AFF),
    accentColor: Color  = Color(0xFF00C9A7),
    strokeWidth: Dp     = 3.dp,
) {
    Canvas(modifier = modifier) {
        val cx = size.width  / 2f
        val cy = size.height / 2f
        val r  = size.minDimension / 2f * 0.84f
        val sw = strokeWidth.toPx()

        // ── Outer hexagon ────────────────────────────────────────────────────
        val hexPath = Path()
        for (i in 0 until 6) {
            val angle = (Math.PI / 3.0 * i - Math.PI / 6.0).toFloat()
            val x = cx + r * cos(angle)
            val y = cy + r * sin(angle)
            if (i == 0) hexPath.moveTo(x, y) else hexPath.lineTo(x, y)
        }
        hexPath.close()
        drawPath(
            path  = hexPath,
            color = primaryColor,
            style = Stroke(width = sw, cap = StrokeCap.Round),
        )

        // ── Nexus Wave arc ───────────────────────────────────────────────────
        val waveLeft  = cx - r * 0.50f
        val waveRight = cx + r * 0.50f
        val amp       = r * 0.30f
        val wavePath  = Path()
        wavePath.moveTo(waveLeft, cy)
        wavePath.cubicTo(
            cx - r * 0.25f, cy - amp,
            cx + r * 0.25f, cy + amp,
            waveRight, cy,
        )
        drawPath(
            path  = wavePath,
            color = accentColor,
            style = Stroke(width = sw, cap = StrokeCap.Round),
        )

        // ── Signal nodes ─────────────────────────────────────────────────────
        val nodeR = sw * 1.1f
        drawCircle(color = primaryColor, radius = nodeR, center = Offset(waveLeft,  cy))
        drawCircle(color = accentColor,  radius = nodeR, center = Offset(waveRight, cy))
    }
}
