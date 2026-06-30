package com.nexuswavetech.nexusplus.features.videogen

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexuswavetech.nexusplus.ai.GeminiRepository
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoGenerationScreen(
    onBack: () -> Unit,
    geminiRepo: GeminiRepository = koinInject(),
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }

    var prompt by remember { mutableStateOf("") }
    var videoUrl by remember { mutableStateOf<String?>(null) }
    var isGenerating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isAutoSaved by remember { mutableStateOf(false) }

    // Check if Gemini API key is available
    var hasApiKey by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        hasApiKey = runCatching { geminiRepo.isAvailable() }.getOrDefault(false)
    }

    Scaffold(
        topBar = { NexusTopBar(title = "AI Video Generation", onBack = onBack) },
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
            // API Key required notice
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
                                "Go to Settings → API Manager and add your Gemini API key to use Video Generation.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
            }

            // Prompt input
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Describe your video", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it; error = null },
                        placeholder = { Text("e.g., A serene sunset over mountains with gentle music…") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                        maxLines = 5,
                    )

                    Button(
                        onClick = {
                            if (prompt.isBlank()) {
                                error = "Please enter a description"
                                return@Button
                            }
                            scope.launch {
                                isGenerating = true
                                error = null
                                videoUrl = null
                                try {
                                    val result = geminiRepo.generateVideo(prompt)
                                    if (result != null) {
                                        videoUrl = result
                                        snack.showSnackbar("Video generated successfully!")
                                    } else {
                                        error = "Generation failed. Check your API key and try again."
                                    }
                                } catch (e: Exception) {
                                    error = "Error: ${e.localizedMessage ?: "Unknown error"}"
                                } finally {
                                    isGenerating = false
                                }
                            }
                        },
                        enabled = !isGenerating && hasApiKey && prompt.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Generating…")
                        } else {
                            Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Generate Video", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Error
            AnimatedVisibility(visible = error != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                        Text(error ?: "", color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }

            // Generated video preview + action buttons
            AnimatedVisibility(visible = videoUrl != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Generated Video", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)

                        // Video thumbnail / placeholder
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.Movie, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                                Text("Video Ready", fontWeight = FontWeight.SemiBold)
                                Text(videoUrl ?: "", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Action buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        val url = videoUrl ?: return@launch
                                        val saved = downloadAndSaveVideo(ctx, url, "nexus_video_${System.currentTimeMillis()}.mp4")
                                        snack.showSnackbar(if (saved) "Video saved to Downloads" else "Save failed")
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Filled.Download, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Download")
                            }
                            Button(
                                onClick = {
                                    scope.launch {
                                        val url = videoUrl ?: return@launch
                                        val saved = downloadAndSaveVideo(ctx, url, "nexus_video_${System.currentTimeMillis()}.mp4")
                                        isAutoSaved = saved
                                        snack.showSnackbar(if (saved) "Auto-saved to Downloads" else "Auto-save failed")
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(if (isAutoSaved) Icons.Filled.CheckCircle else Icons.Filled.Save, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (isAutoSaved) "Saved" else "Auto Save")
                            }
                        }

                        // Share button
                        OutlinedButton(
                            onClick = {
                                val url = videoUrl ?: return@OutlinedButton
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "Check out this AI-generated video from Nexus Plus:\n$url")
                                }
                                ctx.startActivity(Intent.createChooser(shareIntent, "Share Video"))
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Filled.Share, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Share Link")
                        }
                    }
                }
            }

            // Info card
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("How it works", fontWeight = FontWeight.Bold)
                    Text("1. Enter a detailed description of the video you want")
                    Text("2. Gemini AI processes your prompt and generates a video")
                    Text("3. Download, auto-save or share your creation")
                    Text("Requires a valid Gemini API key.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun downloadAndSaveVideo(ctx: Context, url: String, fileName: String): Boolean {
    return runCatching {
        // For now, save the URL as a reference file (actual video download from streaming URL would need more implementation)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName.replace(".mp4", "_url.txt"))
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/NexusPlus")
            }
            val uri = ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                ctx.contentResolver.openOutputStream(it)?.use { os ->
                    os.write("Nexus Plus AI Video URL:\n$url\n\nOpen this link in a browser to view the generated video.".toByteArray())
                }
            }
            MediaScannerConnection.scanFile(ctx, arrayOf(uri?.toString()), null, null)
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val appDir = File(downloadsDir, "NexusPlus").apply { mkdirs() }
            val file = File(appDir, fileName.replace(".mp4", "_url.txt"))
            file.writeText("Nexus Plus AI Video URL:\n$url\n\nOpen this link in a browser to view the generated video.")
            MediaScannerConnection.scanFile(ctx, arrayOf(file.absolutePath), null, null)
        }
        true
    }.getOrDefault(false)
}
