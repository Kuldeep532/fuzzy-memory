package com.nexuswavetech.nexusplus.features.filemanager

import android.Manifest
import android.os.Build
import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

private fun formatSize(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576L     -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1_024L         -> "%.1f KB".format(bytes / 1_024.0)
    else                    -> "$bytes B"
}

private fun getFileIcon(file: File): ImageVector = when {
    file.isDirectory                              -> Icons.Filled.Folder
    file.extension.lowercase() in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp") -> Icons.Filled.Image
    file.extension.lowercase() in listOf("mp4", "mkv", "avi", "mov", "webm") -> Icons.Filled.VideoFile
    file.extension.lowercase() in listOf("mp3", "wav", "flac", "aac", "ogg") -> Icons.Filled.AudioFile
    file.extension.lowercase() in listOf("pdf")  -> Icons.Filled.PictureAsPdf
    file.extension.lowercase() in listOf("zip", "rar", "tar", "gz", "7z") -> Icons.Filled.FolderZip
    file.extension.lowercase() in listOf("apk")  -> Icons.Filled.Android
    file.extension.lowercase() in listOf("txt", "md", "log") -> Icons.Filled.Description
    else                                          -> Icons.Filled.InsertDriveFile
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun FileManagerScreen(onBack: () -> Unit) {
    val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO)
    } else {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    val permState = rememberMultiplePermissionsState(requiredPermissions)

    var currentDir by remember { mutableStateOf(Environment.getExternalStorageDirectory()) }
    var files      by remember { mutableStateOf<List<File>>(emptyList()) }
    val pathStack  = remember { mutableStateListOf<File>() }

    LaunchedEffect(currentDir, permState.allPermissionsGranted) {
        if (permState.allPermissionsGranted) {
            files = (currentDir.listFiles() ?: emptyArray())
                .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "File Manager", onBack = {
            if (pathStack.isNotEmpty()) {
                currentDir = pathStack.removeLast()
            } else {
                onBack()
            }
        })

        if (!permState.allPermissionsGranted) {
            Column(
                modifier            = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Filled.FolderOff, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Text("Storage permission needed to browse files.", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { permState.launchMultiplePermissionRequest() }) { Text("Grant Permission") }
            }
            return@Column
        }

        // Breadcrumb path
        Surface(
            color    = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            val relativePath = currentDir.absolutePath.removePrefix(Environment.getExternalStorageDirectory().absolutePath).ifBlank { "/" }
            Text(
                "📁 $relativePath",
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Stats row
        val totalSize = remember(files) { files.filterNot { it.isDirectory }.sumOf { it.length() } }
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
            Text(
                "${files.count { it.isDirectory }} folders · ${files.count { !it.isDirectory }} files · ${formatSize(totalSize)}",
                style    = MaterialTheme.typography.labelSmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }

        if (files.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("This folder is empty", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 4.dp)) {
                items(files, key = { it.absolutePath }) { file ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (file.isDirectory) {
                                    pathStack.add(currentDir)
                                    currentDir = file
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .semantics {
                                contentDescription = "${if (file.isDirectory) "Folder" else "File"}: ${file.name}. " +
                                    if (file.isDirectory) "" else "Size: ${formatSize(file.length())}."
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Icon(
                            imageVector        = getFileIcon(file),
                            contentDescription = null,
                            tint               = if (file.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier           = Modifier.size(28.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                file.name,
                                style    = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (file.isDirectory) FontWeight.SemiBold else FontWeight.Normal),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                if (file.isDirectory) "${file.listFiles()?.size ?: 0} items"
                                else "${formatSize(file.length())} · ${sdf.format(Date(file.lastModified()))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (file.isDirectory) {
                            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(start = 58.dp), thickness = 0.5.dp)
                }
            }
        }
    }
}
