package com.nexuswavetech.nexusplus.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * NSE 2.0 programmatic vector icon — generated via [ImageVector.Builder].
 * No raster assets used.
 *
 * Visual: Five vertical waveform bars (centred peak, symmetric falloff)
 *         with a sine-wave arc drawn above them, communicating
 *         "speech synthesis / audio intelligence".
 *
 * The fill colour is Color.Black — Material3 Icon composables apply the
 * caller's tint on top, so this acts as a mask.
 */
@Suppress("UNCHECKED_CAST")
val NseVectorIcon: ImageVector by lazy {
    ImageVector.Builder(
        name           = "NexusSpeechEngine",
        defaultWidth   = 24.dp,
        defaultHeight  = 24.dp,
        viewportWidth  = 24f,
        viewportHeight = 24f,
    ).apply {

        val barFill = SolidColor(Color.Black)

        // Bar 1 — leftmost, shortest
        path(fill = barFill) {
            moveTo(1.5f, 16f); lineTo(3.5f, 16f); lineTo(3.5f, 21f); lineTo(1.5f, 21f); close()
        }
        // Bar 2
        path(fill = barFill) {
            moveTo(5.5f, 12f); lineTo(7.5f, 12f); lineTo(7.5f, 21f); lineTo(5.5f, 21f); close()
        }
        // Bar 3 — centre, tallest
        path(fill = barFill) {
            moveTo(9.5f, 8f); lineTo(11.5f, 8f); lineTo(11.5f, 21f); lineTo(9.5f, 21f); close()
        }
        // Bar 4
        path(fill = barFill) {
            moveTo(13.5f, 12f); lineTo(15.5f, 12f); lineTo(15.5f, 21f); lineTo(13.5f, 21f); close()
        }
        // Bar 5 — rightmost, shortest
        path(fill = barFill) {
            moveTo(17.5f, 16f); lineTo(19.5f, 16f); lineTo(19.5f, 21f); lineTo(17.5f, 21f); close()
        }

        // Sine-wave arc above bars
        path(
            stroke          = SolidColor(Color.Black),
            strokeLineWidth = 1.6f,
            strokeLineCap   = StrokeCap.Round,
            fill            = null,
        ) {
            moveTo(1.5f, 7f)
            curveTo(4f, 2f, 7f, 11f, 10.5f, 5f)
            curveTo(14f, -1f, 17f, 9f, 19.5f, 4f)
        }

    }.build()
}
