package com.nexuswavetech.nexusplus.features.videodesc

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.delay
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import java.util.*
import java.util.concurrent.TimeUnit

// ── State ──────────────────────────────────────────────────────────────────

data class VideoDescState(
    val videoUri: Uri? = null,
    val videoName: String = "",
    val durationMs: Long = 0L,
    val description: String = "",
    val isSpeaking: Boolean = false,
    val isAnalyzing: Boolean = false,
    val isPaused: Boolean = false,
    val progressMs: Long = 0L,
    val error: String? = null,
)

// ── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoDescriptionScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var state by remember { mutableStateOf(VideoDescState()) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    val pickVideo = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { analyzeVideo(ctx, it) { state = it } }
    }

    // Init TTS
    LaunchedEffect(Unit) {
        tts = TextToSpeech(ctx) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    // Progress tracking
    LaunchedEffect(state.isSpeaking, state.isPaused) {
        while (state.isSpeaking && !state.isPaused) {
            delay(500)
            state = state.copy(progressMs = state.progressMs + 500)
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
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Pick video button (if none selected) ──────────────────────
            if (state.videoUri == null) {
                item {
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
                            Text("TAP to pick any video file from your device. The app will generate a spoken description using on-device TTS — no internet required.",
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            } else {
                // ── Video info card ───────────────────────────────────────
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.VideoFile, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                Text(state.videoName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            }
                            Text("Duration: ${formatDuration(state.durationMs)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            // Progress bar while speaking
                            AnimatedVisibility(visible = state.isSpeaking) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    LinearProgressIndicator(
                                        progress = { if (state.durationMs > 0) (state.progressMs.toFloat() / state.durationMs).coerceIn(0f, 1f) else 0f },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    Text("${formatDuration(state.progressMs)} / ${formatDuration(state.durationMs)}",
                                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                // ── Description text ────────────────────────────────────────
                if (state.description.isNotBlank()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Filled.Description, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                    Text("Generated Description", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                }
                                Text(state.description, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                // ── Speak / Stop controls ─────────────────────────────────
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                if (state.isSpeaking) {
                                    tts?.stop()
                                    state = state.copy(isSpeaking = false, isPaused = false, progressMs = 0)
                                } else {
                                    speakDescription(ctx, tts, state.description) { id, done ->
                                        if (done) state = state.copy(isSpeaking = false, progressMs = 0)
                                    }
                                    state = state.copy(isSpeaking = true, isPaused = false, progressMs = 0)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = state.description.isNotBlank(),
                        ) {
                            Icon(if (state.isSpeaking) Icons.Filled.Stop else Icons.Filled.PlayArrow, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (state.isSpeaking) "Stop" else "Speak Description")
                        }

                        OutlinedButton(
                            onClick = { pickVideo.launch("video/*") },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Filled.VideoLibrary, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("New Video")
                        }
                    }
                }
            }

            // ── How it works ────────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("How Video Description works", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        BulletItem(Icons.Filled.VideoLibrary, "Pick any video from your device")
                        BulletItem(Icons.Filled.Analytics, "App reads video metadata (duration, resolution, format)")
                        BulletItem(Icons.Filled.RecordVoiceOver, "Generates a natural-language description")
                        BulletItem(Icons.Filled.OfflineBolt, "Speaks it aloud using device TTS — 100% offline, completely free")
                        Text("No API key needed. No internet needed. Fully accessible for visually impaired users.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

private fun analyzeVideo(ctx: Context, uri: Uri, onResult: (VideoDescState) -> Unit) {
    onResult(VideoDescState(videoUri = uri, isAnalyzing = true))
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(ctx, uri)
        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val durationMs = durationStr?.toLongOrNull() ?: 0L
        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH) ?: "?"
        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT) ?: "?"
        val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.let { (it.toIntOrNull() ?: 0) / 1000 } ?: 0
        val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION) ?: "0"
        val format = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "video/*"

        val doc = DocumentFile.fromSingleUri(ctx, uri)
        val name = doc?.name ?: uri.lastPathSegment ?: "Unknown video"

        val desc = buildString {
            append("Video titled \"$name\". ")
            append("Duration ${formatDuration(durationMs)}. ")
            append("Resolution ${width} by $height pixels. ")
            if (bitrate > 0) append("Bitrate ${bitrate} kilobits per second. ")
            if (rotation != "0") append("Rotated $rotation degrees. ")
            append("Format $format. ")
            append("This video is ready for playback.")
        }

        onResult(VideoDescState(
            videoUri = uri,
            videoName = name,
            durationMs = durationMs,
            description = desc,
            isAnalyzing = false,
        ))
    } catch (e: Exception) {
        onResult(VideoDescState(videoUri = uri, error = "Could not read video: ${e.message}", isAnalyzing = false))
    } finally {
        retriever.release()
    }
}

private fun speakDescription(ctx: Context, tts: TextToSpeech?, text: String, onDone: (String, Boolean) -> Unit) {
    if (tts == null || text.isBlank()) return
    val utteranceId = UUID.randomUUID().toString()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }
}

private fun formatDuration(ms: Long): String {
    val hrs = TimeUnit.MILLISECONDS.toHours(ms)
    val mins = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    val secs = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return when {
        hrs > 0 -> "%d:%02d:%02d".format(hrs, mins, secs)
        else -> "%d:%02d".format(mins, secs)
    }
}

@Composable
private fun BulletItem(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
