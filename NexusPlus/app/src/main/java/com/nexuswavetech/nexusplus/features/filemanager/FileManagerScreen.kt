package com.nexuswavetech.nexusplus.features.filemanager

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─── Helpers ──────────────────────────────────────────────────────────────────

private val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

private fun formatSize(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576L     -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1_024L         -> "%.1f KB".format(bytes / 1_024.0)
    else                    -> "$bytes B"
}

private fun getFileIcon(file: File): ImageVector = when {
    file.isDirectory -> Icons.Filled.Folder
    file.extension.lowercase() in setOf("jpg","jpeg","png","gif","webp","bmp","heic") -> Icons.Filled.Image
    file.extension.lowercase() in setOf("mp4","mkv","avi","mov","webm","m4v","3gp")  -> Icons.Filled.VideoFile
    file.extension.lowercase() in setOf("mp3","wav","flac","aac","ogg","m4a","opus") -> Icons.Filled.AudioFile
    file.extension.lowercase() in setOf("pdf")  -> Icons.Filled.PictureAsPdf
    file.extension.lowercase() in setOf("zip","rar","tar","gz","7z") -> Icons.Filled.FolderZip
    file.extension.lowercase() in setOf("apk")  -> Icons.Filled.Android
    file.extension.lowercase() in setOf("txt","md","log","rtf") -> Icons.Filled.Description
    else -> Icons.AutoMirrored.Filled.InsertDriveFile
}

private fun getFileIconTint(file: File, primary: Color, onSurface: Color): Color = when {
    file.isDirectory -> primary
    file.extension.lowercase() in setOf("jpg","jpeg","png","gif","webp","bmp","heic") -> Color(0xFF9C27B0)
    file.extension.lowercase() in setOf("mp4","mkv","avi","mov","webm")               -> Color(0xFFF44336)
    file.extension.lowercase() in setOf("mp3","wav","flac","aac","ogg","m4a","opus")  -> Color(0xFF2196F3)
    file.extension.lowercase() in setOf("pdf")  -> Color(0xFFE53935)
    file.extension.lowercase() in setOf("apk")  -> Color(0xFF4CAF50)
    else -> onSurface
}

private val IMAGE_EXTS = setOf("jpg","jpeg","png","gif","webp","bmp","heic")
private val AUDIO_EXTS = setOf("mp3","wav","flac","aac","ogg","m4a","opus")
private val VIDEO_EXTS = setOf("mp4","mkv","avi","mov","webm","m4v","3gp")
private val DOC_EXTS   = setOf("pdf","txt","doc","docx","xls","xlsx","ppt","pptx","rtf","md","log")

private fun openFileWithSystem(context: android.content.Context, file: File) {
    runCatching {
        val uri  = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val ext  = file.extension.lowercase()
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Open with"))
    }
}

// ─── Sort mode ────────────────────────────────────────────────────────────────

private enum class SortMode(val label: String, val icon: ImageVector) {
    NAME_ASC ("Name A→Z",  Icons.Filled.SortByAlpha),
    NAME_DESC("Name Z→A",  Icons.Filled.SortByAlpha),
    SIZE_ASC ("Size ↑",    Icons.AutoMirrored.Filled.Sort),
    SIZE_DESC("Size ↓",    Icons.AutoMirrored.Filled.Sort),
    DATE_NEW ("Newest",    Icons.Filled.CalendarToday),
    DATE_OLD ("Oldest",    Icons.Filled.CalendarToday),
    TYPE     ("Type",      Icons.Filled.Label),
}

private fun List<File>.sorted(mode: SortMode): List<File> = when (mode) {
    SortMode.NAME_ASC  -> sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    SortMode.NAME_DESC -> sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })).reversed()
    SortMode.SIZE_ASC  -> sortedWith(compareBy({ !it.isDirectory }, { it.length() }))
    SortMode.SIZE_DESC -> sortedWith(compareBy({ !it.isDirectory }, { -it.length() }))
    SortMode.DATE_NEW  -> sortedWith(compareBy({ !it.isDirectory }, { -it.lastModified() }))
    SortMode.DATE_OLD  -> sortedWith(compareBy({ !it.isDirectory }, { it.lastModified() }))
    SortMode.TYPE      -> sortedWith(compareBy({ !it.isDirectory }, { it.extension.lowercase() }, { it.name.lowercase() }))
}

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalPermissionsApi::class, ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
    onBack: () -> Unit,
    onOpenImageViewer: ((Uri) -> Unit)? = null,
    onOpenDocReader:   ((Uri) -> Unit)? = null,
) {
    val context = LocalContext.current

    val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO)
    } else {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
    val permState = rememberMultiplePermissionsState(requiredPermissions)

    // ── Navigation state ───────────────────────────────────────────────────
    var currentDir by remember { mutableStateOf(Environment.getExternalStorageDirectory()) }
    val pathStack  = remember { mutableStateListOf<File>() }
    var rawFiles   by remember { mutableStateOf<List<File>>(emptyList()) }

    // ── UI state ───────────────────────────────────────────────────────────
    var sortMode         by remember { mutableStateOf(SortMode.NAME_ASC) }
    var showHidden       by remember { mutableStateOf(false) }
    var showSortMenu     by remember { mutableStateOf(false) }
    var multiSelectMode  by remember { mutableStateOf(false) }
    var selectedFiles    by remember { mutableStateOf<Set<File>>(emptySet()) }

    // ── Dialog state ───────────────────────────────────────────────────────
    var showCreateFolder        by remember { mutableStateOf(false) }
    var filesToDelete           by remember { mutableStateOf<Set<File>?>(null) }
    var fileToRename            by remember { mutableStateOf<File?>(null) }
    var showMoreMenu            by remember { mutableStateOf(false) }

    // ── Load files ─────────────────────────────────────────────────────────
    LaunchedEffect(currentDir, permState.allPermissionsGranted) {
        if (permState.allPermissionsGranted) {
            rawFiles = (currentDir.listFiles() ?: emptyArray()).toList()
        }
    }

    val files = remember(rawFiles, showHidden, sortMode) {
        rawFiles
            .filter { showHidden || !it.name.startsWith(".") }
            .sorted(sortMode)
    }

    // ── Storage stats ──────────────────────────────────────────────────────
    val storageTotal = Environment.getExternalStorageDirectory().totalSpace.coerceAtLeast(1L)
    val storageFree  = Environment.getExternalStorageDirectory().freeSpace
    val storageUsed  = storageTotal - storageFree
    val storageRatio = (storageUsed.toFloat() / storageTotal).coerceIn(0f, 1f)

    // ── Exit multi-select ──────────────────────────────────────────────────
    fun exitMultiSelect() { multiSelectMode = false; selectedFiles = emptySet() }
    fun selectFile(file: File) {
        selectedFiles = if (file in selectedFiles) selectedFiles - file else selectedFiles + file
        if (selectedFiles.isEmpty()) exitMultiSelect()
    }
    fun onFileTap(file: File) {
        if (multiSelectMode) { selectFile(file); return }
        if (file.isDirectory) {
            pathStack.add(currentDir); currentDir = file; return
        }
        val ext = file.extension.lowercase()
        when {
            ext in IMAGE_EXTS && onOpenImageViewer != null ->
                runCatching {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                    onOpenImageViewer(uri)
                }.onFailure { openFileWithSystem(context, file) }
            ext in DOC_EXTS && onOpenDocReader != null ->
                runCatching {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                    onOpenDocReader(uri)
                }.onFailure { openFileWithSystem(context, file) }
            else -> openFileWithSystem(context, file)
        }
    }
    fun onFileLongPress(file: File) {
        if (!multiSelectMode) { multiSelectMode = true; selectedFiles = setOf(file) }
        else selectFile(file)
    }

    // ── Back handler ───────────────────────────────────────────────────────
    fun handleBack() {
        when {
            multiSelectMode   -> exitMultiSelect()
            pathStack.isNotEmpty() -> { currentDir = pathStack.removeLast() }
            else -> onBack()
        }
    }

    Scaffold(
        floatingActionButton = {
            if (permState.allPermissionsGranted) {
                if (multiSelectMode && selectedFiles.isNotEmpty()) {
                    // Multi-select FAB — delete
                    ExtendedFloatingActionButton(
                        onClick = { filesToDelete = selectedFiles },
                        icon    = { Icon(Icons.Filled.Delete, null) },
                        text    = { Text("Delete (${selectedFiles.size})") },
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor   = MaterialTheme.colorScheme.onErrorContainer,
                    )
                } else if (!multiSelectMode) {
                    // Normal FAB — create folder
                    FloatingActionButton(onClick = { showCreateFolder = true }) {
                        Icon(Icons.Filled.CreateNewFolder, contentDescription = "Create folder")
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

            // ── Top Bar ────────────────────────────────────────────────────
            if (multiSelectMode) {
                MultiSelectTopBar(
                    count      = selectedFiles.size,
                    total      = files.size,
                    onClose    = { exitMultiSelect() },
                    onSelectAll = { selectedFiles = files.toSet() },
                    onDelete   = { if (selectedFiles.isNotEmpty()) filesToDelete = selectedFiles },
                    onShare    = {
                        if (selectedFiles.isNotEmpty()) shareFiles(context, selectedFiles.toList())
                    },
                )
            } else {
                NexusTopBar(
                    title  = "File Manager",
                    onBack = { handleBack() },
                    actions = {
                        // Sort
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.AutoMirrored.Filled.Sort, "Sort")
                            }
                            DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                SortMode.entries.forEach { sm ->
                                    DropdownMenuItem(
                                        text          = { Text(sm.label, fontWeight = if (sortMode == sm) FontWeight.Bold else FontWeight.Normal) },
                                        onClick       = { sortMode = sm; showSortMenu = false },
                                        leadingIcon   = { Icon(sm.icon, null, modifier = Modifier.size(18.dp)) },
                                    )
                                }
                            }
                        }
                        // More options
                        Box {
                            IconButton(onClick = { showMoreMenu = true }) {
                                Icon(Icons.Filled.MoreVert, "More")
                            }
                            DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                                DropdownMenuItem(
                                    text        = { Text(if (showHidden) "Hide hidden files" else "Show hidden files") },
                                    onClick     = { showHidden = !showHidden; showMoreMenu = false },
                                    leadingIcon = { Icon(if (showHidden) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null, modifier = Modifier.size(18.dp)) },
                                )
                            }
                        }
                    }
                )
            }

            if (!permState.allPermissionsGranted) {
                PermissionPrompt { permState.launchMultiplePermissionRequest() }
                return@Column
            }

            // ── Storage bar ────────────────────────────────────────────────
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Used ${formatSize(storageUsed)} / ${formatSize(storageTotal)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "${formatSize(storageFree)} free",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress      = { storageRatio },
                    modifier      = Modifier.fillMaxWidth().height(5.dp).clip(MaterialTheme.shapes.small),
                    color         = when {
                        storageRatio > 0.9f -> MaterialTheme.colorScheme.error
                        storageRatio > 0.7f -> MaterialTheme.colorScheme.tertiary
                        else                -> MaterialTheme.colorScheme.primary
                    },
                )
            }

            // ── Breadcrumb ─────────────────────────────────────────────────
            BreadcrumbRow(
                currentDir = currentDir,
                pathStack  = pathStack,
                onNavigate = { dir ->
                    val idx = pathStack.indexOf(dir)
                    if (idx >= 0) {
                        while (pathStack.size > idx) pathStack.removeLast()
                        currentDir = dir
                    }
                },
            )

            // ── Stats row ──────────────────────────────────────────────────
            val totalSz = remember(files) { files.filterNot { it.isDirectory }.sumOf { it.length() } }
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "${files.count { it.isDirectory }} folders · ${files.count { !it.isDirectory }} files · ${formatSize(totalSz)}",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }

            // ── File list ──────────────────────────────────────────────────
            if (files.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.FolderOpen, null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Spacer(Modifier.height(12.dp))
                        Text("This folder is empty",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier        = Modifier.fillMaxSize(),
                    contentPadding  = PaddingValues(bottom = 88.dp),
                ) {
                    items(files, key = { it.absolutePath }) { file ->
                        FileItem(
                            file           = file,
                            isSelected     = file in selectedFiles,
                            multiSelect    = multiSelectMode,
                            onTap          = { onFileTap(file) },
                            onLongPress    = { onFileLongPress(file) },
                            onRename       = { fileToRename = file },
                            onDelete       = { filesToDelete = setOf(file) },
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 58.dp), thickness = 0.5.dp)
                    }
                }
            }
        }
    }

    // ── Dialogs ────────────────────────────────────────────────────────────────
    if (showCreateFolder) {
        CreateFolderDialog(
            onConfirm = { name ->
                showCreateFolder = false
                if (name.isNotBlank()) {
                    val newDir = File(currentDir, name.trim())
                    if (!newDir.exists()) newDir.mkdirs()
                    rawFiles = (currentDir.listFiles() ?: emptyArray()).toList()
                }
            },
            onDismiss = { showCreateFolder = false },
        )
    }

    filesToDelete?.let { targets ->
        DeleteConfirmDialog(
            count     = targets.size,
            names     = targets.take(3).map { it.name },
            onConfirm = {
                filesToDelete = null
                targets.forEach { it.deleteRecursively() }
                rawFiles = (currentDir.listFiles() ?: emptyArray()).toList()
                exitMultiSelect()
            },
            onDismiss = { filesToDelete = null },
        )
    }

    fileToRename?.let { target ->
        RenameDialog(
            current   = target.name,
            onConfirm = { newName ->
                fileToRename = null
                if (newName.isNotBlank() && newName != target.name) {
                    val dest = File(currentDir, newName.trim())
                    if (!dest.exists()) {
                        target.renameTo(dest)
                        rawFiles = (currentDir.listFiles() ?: emptyArray()).toList()
                    }
                }
            },
            onDismiss = { fileToRename = null },
        )
    }
}

// ─── File item ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileItem(
    file:        File,
    isSelected:  Boolean,
    multiSelect: Boolean,
    onTap:       () -> Unit,
    onLongPress: () -> Unit,
    onRename:    () -> Unit,
    onDelete:    () -> Unit,
) {
    var showContextMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick      = { onTap() },
                onLongClick  = { if (!multiSelect) showContextMenu = true else onLongPress() },
            )
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                else Color.Transparent
            )
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Checkbox or icon
        if (multiSelect) {
            Checkbox(
                checked         = isSelected,
                onCheckedChange = { onTap() },
                modifier        = Modifier.size(24.dp),
            )
        } else {
            val tint = getFileIconTint(
                file,
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                imageVector        = getFileIcon(file),
                contentDescription = null,
                tint               = tint,
                modifier           = Modifier.size(28.dp),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                file.name,
                style    = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (file.isDirectory) FontWeight.SemiBold else FontWeight.Normal,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (file.isDirectory)
                    "${file.listFiles()?.size ?: 0} items · ${sdf.format(Date(file.lastModified()))}"
                else
                    "${formatSize(file.length())} · ${sdf.format(Date(file.lastModified()))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (file.isDirectory && !multiSelect) {
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        }
    }

    // Long-press context menu
    DropdownMenu(expanded = showContextMenu, onDismissRequest = { showContextMenu = false }) {
        DropdownMenuItem(
            text        = { Text("Rename") },
            onClick     = { showContextMenu = false; onRename() },
            leadingIcon = { Icon(Icons.Filled.Edit, null, modifier = Modifier.size(18.dp)) },
        )
        DropdownMenuItem(
            text        = { Text("Delete", color = MaterialTheme.colorScheme.error) },
            onClick     = { showContextMenu = false; onDelete() },
            leadingIcon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) },
        )
    }
}

// ─── Multi-select top bar ─────────────────────────────────────────────────────

@Composable
private fun MultiSelectTopBar(
    count:        Int,
    total:        Int,
    onClose:      () -> Unit,
    onSelectAll:  () -> Unit,
    onDelete:     () -> Unit,
    onShare:      () -> Unit,
) {
    TopAppBar(
        title = { Text("$count of $total selected", style = MaterialTheme.typography.titleMedium) },
        navigationIcon = {
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Cancel") }
        },
        actions = {
            IconButton(onClick = onSelectAll) { Icon(Icons.Filled.SelectAll, "Select all") }
            IconButton(onClick = onShare)  { Icon(Icons.Filled.Share, "Share") }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    )
}

// ─── Breadcrumb ───────────────────────────────────────────────────────────────

@Composable
private fun BreadcrumbRow(
    currentDir: File,
    pathStack:  List<File>,
    onNavigate: (File) -> Unit,
) {
    val storageRoot    = Environment.getExternalStorageDirectory()
    val relativePath   = currentDir.absolutePath.removePrefix(storageRoot.absolutePath)
    val segments       = relativePath.split("/").filter { it.isNotBlank() }

    Surface(
        color    = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Storage, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(4.dp))
            Text(
                text     = "/ ${segments.joinToString(" / ")}".trimEnd('/').ifBlank { "/" },
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ─── Permission prompt ────────────────────────────────────────────────────────

@Composable
private fun PermissionPrompt(onRequest: () -> Unit) {
    Column(
        modifier            = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.FolderOff, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("Storage permission needed to browse files.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRequest) { Text("Grant Permission") }
    }
}

// ─── Dialogs ──────────────────────────────────────────────────────────────────

@Composable
private fun CreateFolderDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon     = { Icon(Icons.Filled.CreateNewFolder, null) },
        title    = { Text("New Folder") },
        text     = {
            OutlinedTextField(
                value         = name,
                onValueChange = { name = it },
                label         = { Text("Folder name") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text("Create")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun DeleteConfirmDialog(
    count:     Int,
    names:     List<String>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon     = { Icon(Icons.Filled.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
        title    = { Text(if (count == 1) "Delete file?" else "Delete $count items?") },
        text     = {
            Column {
                Text("This action cannot be undone.")
                if (names.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    names.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                    if (count > names.size) Text("• … and ${count - names.size} more", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun RenameDialog(current: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon     = { Icon(Icons.Filled.Edit, null) },
        title    = { Text("Rename") },
        text     = {
            OutlinedTextField(
                value         = name,
                onValueChange = { name = it },
                label         = { Text("New name") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank() && name != current) {
                Text("Rename")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ─── Share helper ─────────────────────────────────────────────────────────────

private fun shareFiles(context: android.content.Context, files: List<File>) {
    runCatching {
        val uris = ArrayList(files.map { file ->
            FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        })
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share files"))
    }
}
