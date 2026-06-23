package com.nexuswavetech.nexusplus.features.colorpalette

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlin.math.abs
import kotlin.math.roundToInt

// ── Color math helpers ────────────────────────────────────────────────────────

private fun Color.toHex(): String {
    val r = (red   * 255).roundToInt()
    val g = (green * 255).roundToInt()
    val b = (blue  * 255).roundToInt()
    return "#%02X%02X%02X".format(r, g, b)
}

private fun Color.toHsv(): Triple<Float, Float, Float> {
    val r = red; val g = green; val b = blue
    val max = maxOf(r, g, b); val min = minOf(r, g, b)
    val delta = max - min
    val v = max
    val s = if (max == 0f) 0f else delta / max
    val h = when {
        delta == 0f -> 0f
        max == r    -> 60f * (((g - b) / delta) % 6)
        max == g    -> 60f * (((b - r) / delta) + 2)
        else        -> 60f * (((r - g) / delta) + 4)
    }.let { if (it < 0) it + 360f else it }
    return Triple(h, s, v)
}

private fun hsvToColor(h: Float, s: Float, v: Float): Color {
    val hh = (h % 360f) / 60f
    val i  = hh.toInt()
    val ff = hh - i
    val p  = v * (1 - s); val q = v * (1 - s * ff); val t = v * (1 - s * (1 - ff))
    return when (i) {
        0 -> Color(v, t, p)
        1 -> Color(q, v, p)
        2 -> Color(p, v, t)
        3 -> Color(p, q, v)
        4 -> Color(t, p, v)
        else -> Color(v, p, q)
    }
}

private fun generatePalette(seed: Color, type: String): List<Pair<String, Color>> {
    val (h, s, v) = seed.toHsv()
    return when (type) {
        "Complementary" -> listOf(
            "Seed"          to seed,
            "Complement"    to hsvToColor((h + 180) % 360, s, v),
            "Light Seed"    to hsvToColor(h, s * 0.6f, minOf(v * 1.3f, 1f)),
            "Dark Seed"     to hsvToColor(h, s, v * 0.7f),
            "Light Compl."  to hsvToColor((h + 180) % 360, s * 0.6f, minOf(v * 1.3f, 1f)),
        )
        "Analogous" -> listOf(
            "Seed"   to seed,
            "-30 deg" to hsvToColor((h - 30 + 360) % 360, s, v),
            "+30 deg" to hsvToColor((h + 30) % 360, s, v),
            "-60 deg" to hsvToColor((h - 60 + 360) % 360, s, v),
            "+60 deg" to hsvToColor((h + 60) % 360, s, v),
        )
        "Triadic" -> listOf(
            "Seed"    to seed,
            "+120 deg" to hsvToColor((h + 120) % 360, s, v),
            "+240 deg" to hsvToColor((h + 240) % 360, s, v),
            "Tint 1"  to hsvToColor(h, s * 0.5f, minOf(v * 1.2f, 1f)),
            "Shade 1" to hsvToColor(h, s, v * 0.6f),
        )
        "Monochromatic" -> listOf(
            "90%"  to hsvToColor(h, s, v * 0.9f),
            "70%"  to hsvToColor(h, s, v * 0.7f),
            "Seed" to seed,
            "50%"  to hsvToColor(h, s, v * 0.5f),
            "30%"  to hsvToColor(h, s, v * 0.3f),
        )
        else -> listOf("Seed" to seed)
    }
}

private fun parseHex(hex: String): Color? = runCatching {
    val cleaned = hex.removePrefix("#").padEnd(6, '0').take(6)
    val r = cleaned.substring(0, 2).toInt(16)
    val g = cleaned.substring(2, 4).toInt(16)
    val b = cleaned.substring(4, 6).toInt(16)
    Color(r, g, b)
}.getOrNull()

// ── Screen ────────────────────────────────────────────────────────────────────

private val PALETTE_TYPES = listOf("Complementary", "Analogous", "Triadic", "Monochromatic")
private val PRESETS = listOf(
    "Indigo"  to Color(0xFF3F51B5),
    "Coral"   to Color(0xFFFF6B6B),
    "Emerald" to Color(0xFF2ECC71),
    "Amber"   to Color(0xFFFFBF00),
    "Violet"  to Color(0xFF9C27B0),
    "Sky"     to Color(0xFF03A9F4),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPaletteScreen(
    onBack       : () -> Unit,
    onCopyToClip : ((String) -> Unit)? = null,
) {
    var hexInput       by remember { mutableStateOf("3F51B5") }
    var seedColor      by remember { mutableStateOf(Color(0xFF3F51B5)) }
    var hexError       by remember { mutableStateOf(false) }
    var selectedType   by remember { mutableStateOf(0) }
    var copiedHex      by remember { mutableStateOf<String?>(null) }

    val palette = remember(seedColor, selectedType) {
        generatePalette(seedColor, PALETTE_TYPES[selectedType])
    }

    Scaffold(
        topBar = { NexusTopBar(title = "Color Palette Generator", onBack = onBack) },
    ) { padding ->
        LazyColumn(
            modifier            = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {

            // ── Seed color input ───────────────────────────────────────────
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape  = MaterialTheme.shapes.medium,
                ) {
                    Column(
                        modifier            = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            "Seed Colour",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(seedColor)
                                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                    .semantics { contentDescription = "Current seed colour: ${seedColor.toHex()}" },
                            )
                            OutlinedTextField(
                                value         = hexInput,
                                onValueChange = { input ->
                                    hexInput = input.uppercase().filter { it.isLetterOrDigit() || it == '#' }
                                    val parsed = parseHex(hexInput)
                                    hexError = parsed == null && hexInput.length >= 6
                                    parsed?.let { seedColor = it }
                                },
                                label         = { Text("Hex colour code") },
                                prefix        = { Text("#", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                isError       = hexError,
                                supportingText = if (hexError) ({ Text("Invalid hex colour") }) else null,
                                singleLine    = true,
                                modifier      = Modifier
                                    .weight(1f)
                                    .semantics { contentDescription = "Hex colour input. Enter 6 hex digits." },
                                textStyle     = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                            )
                        }

                        Text("Presets", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier              = Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                        ) {
                            PRESETS.forEach { (name, color) ->
                                Box(
                                    modifier         = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width  = if (seedColor == color) 3.dp else 1.dp,
                                            color  = if (seedColor == color) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                            shape  = CircleShape,
                                        )
                                        .clickable {
                                            seedColor = color
                                            hexInput  = color.toHex().removePrefix("#")
                                            hexError  = false
                                        }
                                        .semantics { contentDescription = "$name preset colour ${color.toHex()}" },
                                )
                            }
                        }
                    }
                }
            }

            // ── Palette type selector ─────────────────────────────────────
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PALETTE_TYPES.forEachIndexed { i, type ->
                        FilterChip(
                            selected = selectedType == i,
                            onClick  = { selectedType = i },
                            label    = { Text(type, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.semantics {
                                contentDescription = "$type palette${if (selectedType == i) ". Selected." else ""}"
                            },
                        )
                    }
                }
            }

            // ── Generated palette ─────────────────────────────────────────
            item {
                Text(
                    "${PALETTE_TYPES[selectedType]} Palette",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            items(palette) { (label, color) ->
                val hex    = color.toHex()
                val onColor = if (color.luminance() > 0.35f) Color.Black else Color.White
                Card(
                    onClick  = {
                        copiedHex = hex
                        onCopyToClip?.invoke(hex)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .semantics { contentDescription = "$label colour $hex. Tap to copy." },
                    shape    = MaterialTheme.shapes.medium,
                ) {
                    Box(
                        modifier         = Modifier
                            .fillMaxSize()
                            .background(color),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Row(
                            modifier              = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = onColor)
                                Text(hex,   style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = onColor.copy(alpha = 0.8f))
                            }
                            if (copiedHex == hex) {
                                Surface(
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = onColor.copy(alpha = 0.15f),
                                ) {
                                    Text(
                                        "Copied!",
                                        style    = MaterialTheme.typography.labelSmall,
                                        color    = onColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            } else {
                                Icon(Icons.Filled.ContentCopy, null, tint = onColor.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
