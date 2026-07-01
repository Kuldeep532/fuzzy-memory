package com.nexuswavetech.nexusplus.features.videodesc

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.media.MediaScannerConnection
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.nexuswavetech.nexusplus.ai.GeminiRepository
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

// ── State ──────────────────────────────────────────────────────────

data class VideoDescState(
    val videoUri: Uri? = null,
    val videoName: String = "",
    val durationMs: Long = 0L,
    val description: String = "",
    val isAnalyzing: Boolean = false,
    val error: String? = null,
)

// ── Screen ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoDescriptionScreen(
    onBack: () -> Unit,
    geminiRepo: GeminiRepository = koinInject(),
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }

    var state by remember { mutableStateOf(VideoDescState()) }
    var hasApiKey by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        hasApiKey = runCatching { geminiRepo.isAvailable() }.getOrDefault(false)
    }

    val pickVideo = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val doc = DocumentFile.fromSingleUri(ctx, it)
            val name = doc?.name ?: it.lastPathSegment ?: "Unknown video"
            state = VideoDescState(videoUri = it, videoName = name, isAnalyzing = true)
            scope.launch {
                try {
                    val videoPath = it.toString()
                    val desc = geminiRepo.describeVideo(videoPath, "Describe this video in detail including scenes, objects, people, actions, and mood.")
                    if (desc != null) {
                        state = state.copy(description = desc, isAnalyzing = false)
                        snack.showSnackbar("Video analyzed successfully!")
                    } else {
                        state = state.copy(error = "Analysis failed. Check your Gemini API key.", isAnalyzing = false)
                    }
                } catch (e: Exception) {
                    state = state.copy(error = "Error: ${e.localizedMessage ?: "Unknown error"}", isAnalyzing = false)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            NexusTopBar(
                title = "Video Description",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { pickVideo.launch("video/*") }) {
                        Icon(Icons.Filled.VideoLibrary, "Pick video", modifier = Modifier.size(22.dp))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // API Key notice
            if (!hasApiKey) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(28.dp))
                        Column {
                            Text("API Key Required", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text(
                                "Add your Gemini API key in Settings → API Manager to use Video Description.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
            }

            // Pick video card (if none selected)
            if (state.videoUri == null) {
                Card(
                    onClick = { pickVideo.launch("video/*") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Filled.VideoLibrary, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("Select a Video", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "TAP to pick a video. Gemini AI will analyze it and generate a detailed description.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            } else {
                // Video info card
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.VideoFile, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Text(state.videoName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        }
                        if (state.isAnalyzing) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Text("Analyzing with Gemini AI…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // Error
                AnimatedVisibility(visible = state.error != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                            Text(state.error ?: "", color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }

                // Description
                if (state.description.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.Description, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                Text("AI Description", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            }
                            Text(state.description, style = MaterialTheme.typography.bodyMedium)

                            // Action buttons
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = {
                                        val cb = ctx.getSystemService(android.content.ClipboardManager::class.java)
                                        cb?.setPrimaryClip(android.content.ClipData.newPlainText("Video Description", state.description))
                                        scope.launch { snack.showSnackbar("Description copied to clipboard") }
                                    },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Copy")
                                }
                                Button(
                                    onClick = {
                                        scope.launch {
                                            val saved = saveDescriptionToDownloads(ctx, state.videoName, state.description)
                                            snack.showSnackbar(if (saved) "Saved to Downloads" else "Save failed")
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(Icons.Filled.Save, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Save")
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, "Video: ${state.videoName}\n\nDescription:\n${state.description}")
                                    }
                                    ctx.startActivity(Intent.createChooser(shareIntent, "Share Description"))
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Filled.Share, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Share Description")
                            }

                            OutlinedButton(
                                onClick = { pickVideo.launch("video/*") },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Filled.VideoLibrary, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Analyze New Video")
                            }
                        }
                    }
                }
            }

            // How it works
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("How Video Description works", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    BulletItem(Icons.Filled.VideoLibrary, "Pick any video from your device")
                    BulletItem(Icons.Filled.AutoAwesome, "Gemini AI analyzes the video content")
                    BulletItem(Icons.Filled.Description, "Generates a detailed description")
                    BulletItem(Icons.Filled.Save, "Save, copy, or share the description")
                    Text("Requires a valid Gemini API key.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────

private fun saveDescriptionToDownloads(ctx: Context, videoName: String, description: String): Boolean {
    return runCatching {
        val fileName = "video_desc_${System.currentTimeMillis()}.txt"
        val content = "Video: $videoName\n\nDescription:\n$description\n\nGenerated by Nexus Plus AI"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/NexusPlus")
            }
            val uri = ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                ctx.contentResolver.openOutputStream(it)?.use { os -> os.write(content.toByteArray()) }
            }
            MediaScannerConnection.scanFile(ctx, arrayOf(uri?.toString()), null, null)
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val appDir = File(downloadsDir, "NexusPlus").apply { mkdirs() }
            val file = File(appDir, fileName)
            file.writeText(content)
            MediaScannerConnection.scanFile(ctx, arrayOf(file.absolutePath), null, null)
        }
        true
    }.getOrDefault(false)
}

@Composable
private fun BulletItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
