package com.nexuswavetech.nexusplus.features.base64tool

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar

@OptIn(ExperimentalEncodingApi::class)
@Composable
fun Base64ToolScreen(onBack: () -> Unit) {
    val clipboard   = LocalClipboardManager.current
    var input       by remember { mutableStateOf("") }
    var output      by remember { mutableStateOf("") }
    var mode        by remember { mutableStateOf(Base64Mode.ENCODE) }
    var error       by remember { mutableStateOf<String?>(null) }

    fun process() {
        error  = null
        output = ""
        if (input.isBlank()) { error = "Input is empty"; return }
        output = try {
            if (mode == Base64Mode.ENCODE)
                Base64.Default.encode(input.toByteArray(Charsets.UTF_8))
            else
                Base64.Default.decode(input.trim()).toString(Charsets.UTF_8)
        } catch (e: Exception) {
            error = "Invalid Base64 input"
            ""
        }
    }

    Column(Modifier.fillMaxSize()) {
        NexusTopBar(title = "Base64 Encoder / Decoder", onBack = onBack)

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode chips
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Base64Mode.entries.forEach { m ->
                    FilterChip(
                        selected = mode == m,
                        onClick  = { mode = m; output = ""; error = null },
                        label    = { Text(m.label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            OutlinedTextField(
                value         = input,
                onValueChange = { input = it; output = ""; error = null },
                label         = { Text(if (mode == Base64Mode.ENCODE) "Plain text" else "Base64 string") },
                modifier      = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
                    .semantics { contentDescription = "Input text for Base64 ${mode.label.lowercase()}." },
                maxLines = 10
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick  = ::process,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.SwapVert, null)
                    Spacer(Modifier.width(6.dp))
                    Text(mode.label)
                }
                IconButton(onClick = { input = ""; output = ""; error = null }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Clear all")
                }
            }

            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            AnimatedVisibility(output.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider()
                    Text("Result", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics { heading() })
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Text(
                            output,
                            Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .semantics { contentDescription = "Result: $output" },
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick  = { clipboard.setText(AnnotatedString(output)) },
                            modifier = Modifier.weight(1f)
                        ) { Icon(Icons.Filled.ContentCopy, null); Spacer(Modifier.width(4.dp)); Text("Copy") }
                        OutlinedButton(
                            onClick  = { input = output; output = ""; mode = if (mode == Base64Mode.ENCODE) Base64Mode.DECODE else Base64Mode.ENCODE },
                            modifier = Modifier.weight(1f)
                        ) { Icon(Icons.Filled.SwapVert, null); Spacer(Modifier.width(4.dp)); Text("Swap") }
                    }
                }
            }
        }
    }
}

enum class Base64Mode(val label: String) { ENCODE("Encode"), DECODE("Decode") }
