package com.nexuswavetech.nexusplus.features.voicetyper

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import java.util.Locale

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VoiceTyperScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current

    val micPermission = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)

    var isListening by remember { mutableStateOf(false) }
    var typedText by remember { mutableStateOf("") }
    var partialText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedLanguage by remember { mutableStateOf("en-IN") }

    val languages = listOf(
        "en-IN" to "English (India)",
        "en-US" to "English (US)",
        "hi-IN" to "Hindi",
        "ta-IN" to "Tamil",
        "te-IN" to "Telugu",
        "kn-IN" to "Kannada",
        "ml-IN" to "Malayalam",
        "mr-IN" to "Marathi",
        "gu-IN" to "Gujarati",
        "pa-IN" to "Punjabi",
        "bn-IN" to "Bengali"
    )

    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) SpeechRecognizer.createSpeechRecognizer(context) else null
    }

    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val scale by pulseAnim.animateFloat(
        initialValue = 1f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "scale"
    )

    fun startListening() {
        error = null
        partialText = ""
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p: Bundle?) { isListening = true; view.announceForAccessibility("Listening") }
            override fun onPartialResults(b: Bundle?) {
                partialText = b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
            }
            override fun onResults(b: Bundle?) {
                val result = b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                typedText = if (typedText.isBlank()) result else "${typedText.trimEnd()} $result"
                partialText = ""
                isListening = false
                view.announceForAccessibility("Speech recognised")
            }
            override fun onError(code: Int) {
                isListening = false
                partialText = ""
                error = when (code) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected. Try again."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Timed out. Tap mic to retry."
                    SpeechRecognizer.ERROR_NETWORK -> "Network error. Check connection."
                    else -> "Speech recognition error (code $code)"
                }
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rms: Float) {}
            override fun onBufferReceived(buf: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onEvent(type: Int, params: Bundle?) {}
        })
        speechRecognizer?.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, selectedLanguage)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
        )
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false
    }

    DisposableEffect(Unit) { onDispose { speechRecognizer?.destroy() } }

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "Voice Typer", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!micPermission.status.isGranted) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Microphone Permission Required", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        Button(onClick = { micPermission.launchPermissionRequest() }) { Text("Grant Permission") }
                    }
                }
            } else if (speechRecognizer == null) {
                Text("Speech recognition not available on this device.", color = MaterialTheme.colorScheme.error)
            } else {
                // Language selector
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = languages.find { it.first == selectedLanguage }?.second ?: selectedLanguage,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Recognition Language") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                            .semantics { contentDescription = "Language selection" }
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        languages.forEach { (code, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = { selectedLanguage = code; expanded = false }
                            )
                        }
                    }
                }

                // Mic button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    if (isListening) {
                        Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            modifier = Modifier
                                .size(100.dp)
                                .scale(scale)
                        ) {}
                    }
                    FloatingActionButton(
                        onClick = { if (isListening) stopListening() else startListening() },
                        modifier = Modifier
                            .size(80.dp)
                            .semantics {
                                contentDescription = if (isListening) "Stop listening" else "Start listening"
                            },
                        containerColor = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            if (isListening) Icons.Filled.MicOff else Icons.Filled.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Text(
                    if (isListening) "Listening… speak now" else "Tap mic to start",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Partial result
                AnimatedVisibility(partialText.isNotBlank()) {
                    Text(
                        partialText,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Light),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.semantics { contentDescription = "Partial: $partialText" }
                    )
                }

                // Text output
                OutlinedTextField(
                    value = typedText,
                    onValueChange = { typedText = it },
                    label = { Text("Typed Text") },
                    minLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Typed text output. You can also edit manually." }
                )

                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                // Actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("voice_typed", typedText))
                            view.announceForAccessibility("Copied to clipboard")
                        },
                        modifier = Modifier.weight(1f).semantics { contentDescription = "Copy to clipboard" },
                        enabled = typedText.isNotBlank()
                    ) {
                        Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Copy")
                    }
                    OutlinedButton(
                        onClick = { typedText = ""; partialText = ""; error = null; view.announceForAccessibility("Cleared") },
                        modifier = Modifier.weight(1f),
                        enabled = typedText.isNotBlank()
                    ) {
                        Icon(Icons.Filled.Clear, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Clear")
                    }
                }
            }
        }
    }
}
