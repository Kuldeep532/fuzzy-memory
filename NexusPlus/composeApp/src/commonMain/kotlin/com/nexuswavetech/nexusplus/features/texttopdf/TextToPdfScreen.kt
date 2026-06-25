package com.nexuswavetech.nexusplus.features.texttopdf

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextToPdfScreen(
    onBack      : () -> Unit,
    onExportPdf : ((title: String, body: String, fontSizePt: Float) -> Unit)? = null,
) {
    var inputText      by remember { mutableStateOf("") }
    var title          by remember { mutableStateOf("") }
    var fontSize       by remember { mutableStateOf(14f) }
    var showPreview    by remember { mutableStateOf(false) }
    var selectedFont   by remember { mutableStateOf(0) }
    val fontFamilies   = listOf("Default" to FontFamily.Default, "Serif" to FontFamily.Serif, "Monospace" to FontFamily.Monospace)
    val charCount      = inputText.length
    val wordCount      = if (inputText.isBlank()) 0 else inputText.trim().split(Regex("\\s+")).size

    Scaffold(
        topBar = { NexusTopBar(title = "Text to PDF", onBack = onBack) },
    ) { padding ->
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            // ── Document title ─────────────────────────────────────────────
            OutlinedTextField(
                value         = title,
                onValueChange = { title = it },
                label         = { Text("Document Title") },
                leadingIcon   = { Icon(Icons.Filled.Article, null) },
                singleLine    = true,
                modifier      = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Document title input" },
            )

            // ── Format controls ────────────────────────────────────────────
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape  = MaterialTheme.shapes.medium,
            ) {
                Column(
                    modifier            = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "Format",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Font size: ${fontSize.toInt()}pt",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilledTonalIconButton(
                                onClick  = { if (fontSize > 10f) fontSize-- },
                                modifier = Modifier
                                    .size(32.dp)
                                    .semantics { contentDescription = "Decrease font size" },
                            ) { Text("-", style = MaterialTheme.typography.labelLarge) }
                            FilledTonalIconButton(
                                onClick  = { if (fontSize < 24f) fontSize++ },
                                modifier = Modifier
                                    .size(32.dp)
                                    .semantics { contentDescription = "Increase font size" },
                            ) { Text("+", style = MaterialTheme.typography.labelLarge) }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        fontFamilies.forEachIndexed { i, (label, _) ->
                            FilterChip(
                                selected = selectedFont == i,
                                onClick  = { selectedFont = i },
                                label    = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.semantics {
                                    contentDescription = "$label font${if (selectedFont == i) ". Selected." else ""}"
                                },
                            )
                        }
                    }
                }
            }

            // ── Stats row ─────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape    = MaterialTheme.shapes.small,
                    color    = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.semantics { contentDescription = "$wordCount words" },
                ) {
                    Text(
                        "$wordCount words",
                        style    = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color    = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                Surface(
                    shape    = MaterialTheme.shapes.small,
                    color    = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.semantics { contentDescription = "$charCount characters" },
                ) {
                    Text(
                        "$charCount chars",
                        style    = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color    = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }

            // ── Text input or preview ──────────────────────────────────────
            if (!showPreview) {
                OutlinedTextField(
                    value         = inputText,
                    onValueChange = { inputText = it },
                    label         = { Text("Type or paste your text…") },
                    modifier      = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .semantics { contentDescription = "Text content input area. $wordCount words entered." },
                    maxLines      = Int.MAX_VALUE,
                )
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape  = MaterialTheme.shapes.small,
                ) {
                    SelectionContainer {
                        Text(
                            text     = inputText.ifBlank { "Preview appears here…" },
                            style    = MaterialTheme.typography.bodyMedium.copy(
                                fontSize   = fontSize.sp,
                                fontFamily = fontFamilies[selectedFont].second,
                            ),
                            color    = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                        )
                    }
                }
            }

            // ── Action buttons ────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick  = { showPreview = !showPreview },
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = if (showPreview) "Show editor" else "Preview document" },
                ) {
                    Icon(
                        if (showPreview) Icons.Filled.Edit else Icons.Filled.Preview,
                        null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (showPreview) "Edit" else "Preview")
                }
                Button(
                    onClick  = { onExportPdf?.invoke(title, inputText, fontSize) },
                    enabled  = inputText.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Generate and share PDF document" },
                ) {
                    Icon(Icons.Filled.PictureAsPdf, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Export PDF")
                }
            }
        }
    }
}
