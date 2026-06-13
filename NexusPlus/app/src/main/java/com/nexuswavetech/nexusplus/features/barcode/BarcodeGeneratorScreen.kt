package com.nexuswavetech.nexusplus.features.barcode

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar

private val formats = listOf(
    "CODE_128"  to BarcodeFormat.CODE_128,
    "EAN-13"    to BarcodeFormat.EAN_13,
    "EAN-8"     to BarcodeFormat.EAN_8,
    "UPC-A"     to BarcodeFormat.UPC_A,
    "ITF"       to BarcodeFormat.ITF,
    "CODE_39"   to BarcodeFormat.CODE_39,
    "DATA_MATRIX" to BarcodeFormat.DATA_MATRIX,
    "PDF_417"   to BarcodeFormat.PDF_417,
)

private fun generateBarcode(content: String, format: BarcodeFormat, width: Int = 600, height: Int = 200): Bitmap? {
    if (content.isBlank()) return null
    return runCatching {
        val hints = mapOf(EncodeHintType.MARGIN to 2)
        val bitMatrix = MultiFormatWriter().encode(content, format, width, height, hints)
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        bmp
    }.getOrNull()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeGeneratorScreen(onBack: () -> Unit) {
    var inputText     by remember { mutableStateOf("") }
    var formatIndex   by remember { mutableIntStateOf(0) }
    var bitmap        by remember { mutableStateOf<Bitmap?>(null) }
    var errorMsg      by remember { mutableStateOf<String?>(null) }
    var expanded      by remember { mutableStateOf(false) }

    val (formatName, barcodeFormat) = formats[formatIndex]

    val formatHints = mapOf(
        "CODE_128"   to "Any text or numbers (most versatile)",
        "EAN-13"     to "Exactly 12 digits (check digit auto-added)",
        "EAN-8"      to "Exactly 7 digits (check digit auto-added)",
        "UPC-A"      to "Exactly 11 digits (check digit auto-added)",
        "ITF"        to "Even number of digits only",
        "CODE_39"    to "Uppercase letters, digits, and - . $ / + % space",
        "DATA_MATRIX" to "Any text or binary data",
        "PDF_417"    to "Any text (2D stacked barcode)",
    )

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "Barcode Generator", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Format selector
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value         = formatName,
                    onValueChange = {},
                    readOnly      = true,
                    label         = { Text("Barcode Format") },
                    trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier      = Modifier.menuAnchor().fillMaxWidth(),
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    formats.forEachIndexed { i, (name, _) ->
                        DropdownMenuItem(
                            text    = { Text(name) },
                            onClick = { formatIndex = i; expanded = false; bitmap = null; errorMsg = null },
                        )
                    }
                }
            }

            // Format hint
            formatHints[formatName]?.let {
                Text("Format hint: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Input
            OutlinedTextField(
                value         = inputText,
                onValueChange = { inputText = it; bitmap = null; errorMsg = null },
                label         = { Text("Content to encode") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth().semantics { contentDescription = "Text to encode in barcode" },
            )

            // Generate button
            Button(
                onClick  = {
                    errorMsg = null
                    bitmap   = generateBarcode(inputText.trim(), barcodeFormat)
                    if (bitmap == null && inputText.isNotBlank()) {
                        errorMsg = "Could not generate $formatName barcode. Check format requirements."
                    }
                },
                enabled  = inputText.isNotBlank(),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Generate $formatName barcode" },
            ) {
                Icon(Icons.Filled.QrCodeScanner, null)
                Spacer(Modifier.width(8.dp))
                Text("Generate Barcode")
            }

            // Error
            errorMsg?.let {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) {
                    Text(it, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                }
            }

            // Preview
            bitmap?.let { bmp ->
                Card(
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "$formatName barcode for: $inputText" },
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Image(
                            bitmap            = bmp.asImageBitmap(),
                            contentDescription = "$formatName barcode",
                            modifier          = Modifier.fillMaxWidth().heightIn(min = 80.dp, max = 200.dp),
                        )
                        Text(
                            inputText.take(40) + if (inputText.length > 40) "…" else "",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            formatName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            // Format reference
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Barcode Format Guide", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.primary)
                    listOf(
                        "CODE 128" to "Most common; used in shipping and supply chain",
                        "EAN-13"   to "International standard for retail products",
                        "UPC-A"    to "North American retail standard",
                        "PDF 417"  to "High-capacity 2D; used in boarding passes",
                        "Data Matrix" to "Compact 2D; used in electronics",
                    ).forEach { (name, desc) ->
                        Row {
                            Text("• $name: ", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                            Text(desc, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
