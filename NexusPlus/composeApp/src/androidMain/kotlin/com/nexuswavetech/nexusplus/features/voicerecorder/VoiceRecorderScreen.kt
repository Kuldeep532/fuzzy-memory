package com.nexuswavetech.nexusplus.features.voicerecorder

import android.Manifest
import android.content.ContentValues
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class Recording(val name: String, val path: String, val durationSec: Int, val timestampMs: Long)

private val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VoiceRecorderScreen(onBack: () -> Unit) {
    val context         = LocalContext.current
    val audioPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    var isRecording  by remember { mutableStateOf(false) }
    var elapsedSec   by remember { mutableIntStateOf(0) }
    var recordings   by remember { mutableStateOf<List<Recording>>(emptyList()) }
    var playingIndex by remember { mutableIntStateOf(-1) }
    var recorder     by remember { mutableStateOf<MediaRecorder?>(null) }
    var player       by remember { mutableStateOf<MediaPlayer?>(null) }
    var currentPath  by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        onDispose {
            recorder?.apply { stop(); release() }
            player?.apply  { stop(); release() }
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            elapsedSec = 0
            while (isRecording) {
                delay(1_000L)
                elapsedSec++
            }
        }
    }

    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue   = 1f,
        targetValue    = 1.15f,
        animationSpec  = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label          = "pulse_scale",
    )

    fun startRecording() {
        val name      = "Nexus_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.m4a"
        val outputFile: File
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, name)
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
                put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/NexusPlus")
            }
            val uri = context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
            outputFile = File(context.cacheDir, name)
            currentPath = outputFile.absolutePath
        } else {
            outputFile = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), name)
            currentPath = outputFile.absolutePath
        }
        @Suppress("DEPRECATION")
        val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            MediaRecorder(context)
        else
            MediaRecorder()
        rec.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128_000)
            setAudioSamplingRate(44_100)
            setOutputFile(outputFile.absolutePath)
            prepare()
            start()
        }
        recorder   = rec
        isRecording = true
    }

    fun stopRecording() {
        runCatching { recorder?.stop() }
        recorder?.release()
        recorder    = null
        isRecording = false
        if (currentPath.isNotBlank() && File(currentPath).exists()) {
            val name = File(currentPath).name.removeSuffix(".m4a")
            recordings = listOf(Recording(name, currentPath, elapsedSec, System.currentTimeMillis())) + recordings
        }
    }

    fun stopPlaying() {
        player?.apply { stop(); release() }
        player       = null
        playingIndex = -1
    }

    fun playRecording(index: Int, path: String) {
        stopPlaying()
        if (File(path).exists()) {
            player = MediaPlayer().apply {
                setDataSource(path)
                prepare()
                start()
                setOnCompletionListener { playingIndex = -1 }
            }
            playingIndex = index
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "Voice Recorder", onBack = onBack)

        if (!audioPermission.status.isGranted) {
            Column(
                modifier            = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Filled.Mic, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Text("Microphone permission needed to record audio.", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { audioPermission.launchPermissionRequest() }) { Text("Grant Permission") }
            }
            return@Column
        }

        Column(
            modifier            = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── Recorder UI ───────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(if (isRecording) 140.dp * scale else 140.dp)
                    .clip(CircleShape)
                    .background(if (isRecording) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(
                    onClick = { if (isRecording) stopRecording() else startRecording() },
                    modifier = Modifier.size(130.dp).semantics {
                        contentDescription = if (isRecording) "Stop recording" else "Start recording"
                    },
                ) {
                    AnimatedContent(targetState = isRecording, label = "rec_icon") { recording ->
                        Icon(
                            imageVector        = if (recording) Icons.Filled.Stop else Icons.Filled.Mic,
                            contentDescription = null,
                            modifier           = Modifier.size(64.dp),
                            tint               = if (recording) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            if (isRecording) {
                val m = elapsedSec / 60; val s = elapsedSec % 60
                Text(
                    "🔴 %02d:%02d".format(m, s),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.error,
                )
            } else {
                Text("Record", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        HorizontalDivider()

        // ── Recordings list ───────────────────────────────────────────────
        if (recordings.isEmpty()) {
            Box(Modifier.fillMaxSize().semantics { contentDescription = "No recordings yet. Tap the microphone button to start recording." }, contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.GraphicEq, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    Text("No recordings yet", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text("Recordings (${recordings.size})", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 4.dp)) }
                itemsIndexed(recordings, key = { _, r -> r.timestampMs }) { index, rec ->
                    val m = rec.durationSec / 60; val s = rec.durationSec % 60
                    Card(modifier = Modifier.fillMaxWidth().semantics { contentDescription = "${rec.name}. Duration %02d:%02d.".format(m, s) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rec.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), maxLines = 1)
                                Text("%02d:%02d · %s".format(m, s, sdf.format(Date(rec.timestampMs))), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { if (playingIndex == index) stopPlaying() else playRecording(index, rec.path) }, modifier = Modifier.semantics { contentDescription = if (playingIndex == index) "Stop playback" else "Play ${rec.name}" }) {
                                Icon(if (playingIndex == index) Icons.Filled.Stop else Icons.Filled.PlayArrow, null, tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { recordings = recordings.toMutableList().also { it.removeAt(index) }; if (playingIndex == index) stopPlaying() }, modifier = Modifier.semantics { contentDescription = "Delete ${rec.name}" }) {
                                Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
