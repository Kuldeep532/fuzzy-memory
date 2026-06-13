package com.nexuswavetech.nexusplus.features.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar

@Composable
fun ClipboardManagerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val view    = LocalView.current
    val clipboardManager = remember {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    var history    by remember { mutableStateOf<List<String>>(emptyList()) }
    var inputText  by remember { mutableStateOf("") }
    var snackMsg   by remember { mutableStateOf<String?>(null) }
    val snackState = remember { SnackbarHostState() }

    LaunchedEffect(snackMsg) {
        snackMsg?.let {
            snackState.showSnackbar(it)
            snackMsg = null
        }
    }

    // Read current clipboard on open
    LaunchedEffect(Unit) {
        val current = clipboardManager.primaryClip?.getItemAt(0)?.text?.toString()
        if (!current.isNullOrBlank() && current !in history) {
            history = listOf(current) + history
        }
    }

    fun copyToClipboard(text: String) {
        clipboardManager.setPrimaryClip(ClipData.newPlainText("Nexus Clipboard", text))
        view.announceForAccessibility("Copied to clipboard: $text")
        snackMsg = "Copied!"
        if (text !in history) {
            history = listOf(text) + history
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            NexusTopBar(
                title   = "Clipboard Manager",
                onBack  = onBack,
            )

            // Input area
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value         = inputText,
                    onValueChange = { inputText = it },
                    label         = { Text("Type or paste text here") },
                    modifier      = Modifier.fillMaxWidth().semantics { contentDescription = "Text input for clipboard" },
                    minLines      = 3,
                    maxLines      = 5,
                    trailingIcon  = {
                        if (inputText.isNotBlank()) {
                            IconButton(onClick = { inputText = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear input")
                            }
                        }
                    },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick  = {
                            if (inputText.isNotBlank()) copyToClipboard(inputText.trim())
                        },
                        enabled  = inputText.isNotBlank(),
                        modifier = Modifier.semantics { contentDescription = "Copy text to clipboard" },
                    ) {
                        Icon(Icons.Filled.ContentCopy, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Copy")
                    }
                    OutlinedButton(
                        onClick = {
                            val current = clipboardManager.primaryClip?.getItemAt(0)?.text?.toString()
                            if (!current.isNullOrBlank()) {
                                inputText = current
                                if (current !in history) history = listOf(current) + history
                                snackMsg = "Pasted from clipboard"
                            } else {
                                snackMsg = "Clipboard is empty"
                            }
                        },
                        modifier = Modifier.semantics { contentDescription = "Paste from clipboard" },
                    ) {
                        Icon(Icons.Filled.ContentPaste, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Paste")
                    }
                }
            }

            HorizontalDivider()

            // History
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "History (${history.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.primary,
                )
                if (history.isNotEmpty()) {
                    TextButton(
                        onClick  = { history = emptyList() },
                        modifier = Modifier.semantics { contentDescription = "Clear all clipboard history" },
                    ) {
                        Icon(Icons.Filled.DeleteSweep, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Clear all")
                    }
                }
            }

            if (history.isEmpty()) {
                Box(
                    modifier         = Modifier.fillMaxSize().semantics { contentDescription = "No clipboard history yet" },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Filled.ContentPaste, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Text("No history yet", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Type and copy text above to save it here", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier       = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(history, key = { i, _ -> i }) { index, text ->
                        Card(
                            modifier = Modifier.fillMaxWidth().semantics {
                                contentDescription = "Clipboard item ${index + 1}: ${text.take(50)}"
                            },
                            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            Row(
                                modifier          = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text  = text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 3,
                                        overflow  = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        "${text.length} chars",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Row {
                                    IconButton(
                                        onClick  = { copyToClipboard(text) },
                                        modifier = Modifier.semantics { contentDescription = "Copy item ${index + 1}" },
                                    ) {
                                        Icon(Icons.Filled.ContentCopy, null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(
                                        onClick  = {
                                            inputText = text
                                            snackMsg = "Loaded into editor"
                                        },
                                        modifier = Modifier.semantics { contentDescription = "Load item ${index + 1} into editor" },
                                    ) {
                                        Icon(Icons.Filled.Edit, null)
                                    }
                                    IconButton(
                                        onClick  = { history = history.toMutableList().also { it.removeAt(index) } },
                                        modifier = Modifier.semantics { contentDescription = "Delete item ${index + 1}" },
                                    ) {
                                        Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
