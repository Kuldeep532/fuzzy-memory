package com.nexuswavetech.nexusplus.features.tts

import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.compose.animation.*
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.delay
import java.util.*
import kotlin.math.roundToInt

/**
 * Automatic language detection — scans Unicode block ranges to identify script/language.
 * Switches the TTS engine locale before synthesis begins.
 */
private fun detectLanguage(text: String): Locale {
    val chars = text.take(200)
    val devanagari = chars.count { it.code in 0x0900..0x097F }
    val arabic = chars.count { it.code in 0x0600..0x06FF }
    val chinese = chars.count { it.code in 0x4E00..0x9FFF }
    val japanese = chars.count { it.code in 0x3040..0x30FF }
    val korean = chars.count { it.code in 0xAC00..0xD7AF }
    val latin = chars.count { it.code in 0x0041..0x024F }
    val spanish = Regex("[áéíóúüñ¿¡]", RegexOption.IGNORE_CASE).containsMatchIn(chars)

    return when {
        devanagari > 3  -> Locale("hi", "IN")
        arabic > 3      -> Locale("ar", "SA")
        chinese > 3     -> Locale.SIMPLIFIED_CHINESE
        japanese > 3    -> Locale.JAPANESE
        korean > 3      -> Locale.KOREAN
        spanish         -> Locale("es", "ES")
        else            -> Locale.ENGLISH
    }
}

@Composable
fun NexusTtsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current

    var inputText by remember { mutableStateOf("") }
    var pitch by remember { mutableStateOf(1.0f) }
    var speechRate by remember { mutableStateOf(1.0f) }
    var isSpeaking by remember { mutableStateOf(false) }
    var detectedLanguage by remember { mutableStateOf<Locale?>(null) }
    var ttsReady by remember { mutableStateOf(false) }

    val tts = remember {
        mutableStateOf<TextToSpeech?>(null)
    }

    DisposableEffect(Unit) {
        tts.value = TextToSpeech(context) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
        }
        onDispose {
            tts.value?.stop()
            tts.value?.shutdown()
        }
    }

    // Update detected language as user types
    LaunchedEffect(inputText) {
        if (inputText.length > 5) {
            delay(500)
            detectedLanguage = detectLanguage(inputText)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "Nexus Speech Engine", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = { Text("Enter text to speak…") },
                placeholder = { Text("Type or paste text in any language") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp)
                    .semantics {
                        contentDescription = "Text input for speech synthesis. " +
                            "Language is detected automatically from your input."
                    },
                maxLines = 8
            )

            // Detected language badge
            AnimatedVisibility(visible = detectedLanguage != null && inputText.length > 5) {
                detectedLanguage?.let { locale ->
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.Translate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Auto-detected: ${locale.displayLanguage}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.semantics {
                                    contentDescription = "Language auto-detected as ${locale.displayLanguage}"
                                }
                            )
                        }
                    }
                }
            }

            // Pitch slider
            Column {
                Text(
                    text = "Pitch: ${(pitch * 10).roundToInt() / 10.0}x",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.semantics { contentDescription = "Pitch control. Current value: ${(pitch * 10).roundToInt() / 10.0}x" }
                )
                Slider(
                    value = pitch,
                    onValueChange = { pitch = it },
                    valueRange = 0.5f..2.0f,
                    steps = 14,
                    modifier = Modifier.semantics {
                        contentDescription = "Pitch slider. Adjust from 0.5 to 2.0. Current: ${(pitch * 10).roundToInt() / 10.0}"
                    }
                )
            }

            // Speech rate slider
            Column {
                Text(
                    text = "Speed: ${(speechRate * 10).roundToInt() / 10.0}x",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.semantics { contentDescription = "Speech rate control. Current value: ${(speechRate * 10).roundToInt() / 10.0}x" }
                )
                Slider(
                    value = speechRate,
                    onValueChange = { speechRate = it },
                    valueRange = 0.25f..3.0f,
                    steps = 27,
                    modifier = Modifier.semantics {
                        contentDescription = "Speech rate slider. Adjust from 0.25 to 3.0. Current: ${(speechRate * 10).roundToInt() / 10.0}"
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        val engine = tts.value ?: return@Button
                        val text = inputText.trim()
                        if (text.isBlank()) return@Button

                        val locale = detectLanguage(text)
                        engine.language = locale
                        engine.setPitch(pitch)
                        engine.setSpeechRate(speechRate)
                        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "nexus_tts")
                        isSpeaking = true
                        detectedLanguage = locale
                        view.announceForAccessibility("Speaking in ${locale.displayLanguage}")
                    },
                    enabled = ttsReady && inputText.isNotBlank() && !isSpeaking,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .semantics { contentDescription = "Speak text. Activates text-to-speech synthesis." }
                ) {
                    Icon(Icons.Filled.RecordVoiceOver, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Speak")
                }

                OutlinedButton(
                    onClick = {
                        tts.value?.stop()
                        isSpeaking = false
                        view.announceForAccessibility("Speech stopped")
                    },
                    enabled = isSpeaking,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .semantics { contentDescription = "Stop speech playback." }
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Stop")
                }
            }

            OutlinedButton(
                onClick = { inputText = "" },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Clear, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Clear")
            }

            if (!ttsReady) {
                Text(
                    "Text-to-Speech engine initialising…",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
