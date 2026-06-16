package com.nexuswavetech.nexusplus.features.dochub

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.OpenInNew
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

data class DocEntry(val uri: Uri, val name: String, val size: Long, val mimeType: String)

@Composable
fun DocHubScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    var docs by remember { mutableStateOf<List<DocEntry>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }

    val pickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        val newDocs = uris.mapNotNull { uri ->
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                cursor.moveToFirst()
                val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                val name = if (nameIdx >= 0) cursor.getString(nameIdx) else uri.lastPathSegment ?: "File"
                val size = if (sizeIdx >= 0) cursor.getLong(sizeIdx) else 0L
                val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
                DocEntry(uri, name, size, mime)
            }
        }
        docs = (docs + newDocs).distinctBy { it.uri.toString() }
        view.announceForAccessibility("${newDocs.size} document${if (newDocs.size != 1) "s" else ""} added")
    }

    val filtered = remember(docs, searchQuery) {
        if (searchQuery.isBlank()) docs else docs.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    fun formatSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${"%.1f".format(bytes / (1024.0 * 1024))} MB"
    }

    fun docIcon(mime: String) = when {
        mime.contains("pdf") -> Icons.Filled.PictureAsPdf
        mime.contains("image") -> Icons.Filled.Image
        mime.contains("audio") -> Icons.Filled.AudioFile
        mime.contains("video") -> Icons.Filled.VideoFile
        mime.contains("text") || mime.contains("document") || mime.contains("word") -> Icons.Filled.Description
        mime.contains("spreadsheet") || mime.contains("excel") -> Icons.Filled.TableChart
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "Document Organizer", onBack = onBack)

        Column(modifier = Modifier.fillMaxSize()) {
            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search documents…") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .semantics { contentDescription = "Search documents" }
            )

            // Stats bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${filtered.size} document${if (filtered.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = { pickerLauncher.launch(arrayOf("*/*")) },
                    modifier = Modifier.semantics { contentDescription = "Add documents from device" }
                ) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Files")
                }
            }

            Spacer(Modifier.height(8.dp))

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics { contentDescription = if (docs.isEmpty()) "No documents added. Tap Add Files to import." else "No matching documents." },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Filled.FolderOpen, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Text(
                            if (docs.isEmpty()) "No documents yet" else "No results",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (docs.isEmpty()) {
                            Text("Tap Add Files to import documents\nfrom your device for quick access",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.uri.toString() }) { doc ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "${doc.name}. ${formatSize(doc.size)}." }
                        ) {
                            ListItem(
                                leadingContent = {
                                    Icon(
                                        docIcon(doc.mimeType), null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                },
                                headlineContent = {
                                    Text(doc.name, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                },
                                supportingContent = {
                                    Text(formatSize(doc.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                },
                                trailingContent = {
                                    Row {
                                        IconButton(onClick = {
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(doc.uri, doc.mimeType)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            runCatching { context.startActivity(intent) }
                                        }) {
                                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open ${doc.name}")
                                        }
                                        IconButton(onClick = {
                                            docs = docs.filter { it.uri != doc.uri }
                                            view.announceForAccessibility("${doc.name} removed")
                                        }) {
                                            Icon(Icons.Filled.RemoveCircleOutline, contentDescription = "Remove ${doc.name}", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
