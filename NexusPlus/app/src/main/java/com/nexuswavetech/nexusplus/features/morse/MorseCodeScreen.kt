package com.nexuswavetech.nexusplus.features.morse

import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexuswavetech.nexusplus.core.HapticHelper
import com.nexuswavetech.nexusplus.core.SettingsRepository
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

private val MORSE_MAP = mapOf(
    'A' to ".-",    'B' to "-...",  'C' to "-.-.",  'D' to "-..",
    'E' to ".",     'F' to "..-.",  'G' to "--.",   'H' to "....",
    'I' to "..",    'J' to ".---",  'K' to "-.-",   'L' to ".-..",
    'M' to "--",    'N' to "-.",    'O' to "---",   'P' to ".--.",
    'Q' to "--.-",  'R' to ".-.",   'S' to "...",   'T' to "-",
    'U' to "..-",   'V' to "...-",  'W' to ".--",   'X' to "-..-",
    'Y' to "-.--",  'Z' to "--..",
    '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--",
    '4' to "....-", '5' to ".....", '6' to "-....", '7' to "--...",
    '8' to "---..", '9' to "----.",
    '.' to ".-.-.-", ',' to "--..--", '?' to "..--..", '\'' to ".----.",
    '!' to "-.-.--", '/' to "-..-.", '(' to "-.--.", ')' to "-.--.-",
    '&' to ".-...", ':' to "---...", ';' to "-.-.-.", '=' to "-...-",
    '+' to ".-.-.",  '-' to "-....-", '_' to "..--.-", '"' to ".-..-.",
    '$' to "...-..-", '@' to ".--.-."
)

private val REVERSE_MAP = MORSE_MAP.entries.associate { (k, v) -> v to k }

fun textToMorse(text: String): String =
    text.uppercase().map { ch ->
        when (ch) {
            ' ' -> "/"
            else -> MORSE_MAP[ch] ?: "?"
        }
    }.joinToString(" ")

fun morseToText(morse: String): String =
    morse.trim().split(" / ").joinToString(" ") { word ->
        word.split(" ").joinToString("") { code ->
            (REVERSE_MAP[code] ?: '?').toString()
        }
    }

enum class MorseMode { TEXT_TO_MORSE, MORSE_TO_TEXT }

class MorseCodeViewModel(private val settings: SettingsRepository) : ViewModel() {
    var mode   by mutableStateOf(MorseMode.TEXT_TO_MORSE)
        private set
    var input  by mutableStateOf("")
        private set
    var output by mutableStateOf("")
        private set

    fun onModeChanged(m: MorseMode) { mode = m; input = ""; output = "" }
    fun onInputChanged(v: String)   {
        input  = v
        output = if (v.isBlank()) "" else when (mode) {
            MorseMode.TEXT_TO_MORSE -> textToMorse(v)
            MorseMode.MORSE_TO_TEXT -> morseToText(v)
        }
    }
    fun clearAll()                  { input = ""; output = "" }
    fun swapInputOutput()           {
        val newMode = if (mode == MorseMode.TEXT_TO_MORSE) MorseMode.MORSE_TO_TEXT else MorseMode.TEXT_TO_MORSE
        val prev = output
        mode   = newMode
        input  = prev
        output = if (prev.isBlank()) "" else when (newMode) {
            MorseMode.TEXT_TO_MORSE -> textToMorse(prev)
            MorseMode.MORSE_TO_TEXT -> morseToText(prev)
        }
    }

    fun playMorseVibration(context: android.content.Context) {
        val morse = if (mode == MorseMode.TEXT_TO_MORSE) output else input
        if (morse.isBlank()) return
        viewModelScope.launch {
            val speed = settings.morseVibrationSpeed.first()
            val unit: Long = when (speed) {
                SettingsRepository.MORSE_SPEED_SLOW -> 150L
                SettingsRepository.MORSE_SPEED_FAST -> 50L
                else                                -> 100L
            }
            val dotMs  = unit
            val dashMs = unit * 3
            val gapMs  = unit * 2
            val wordMs = unit * 5

            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator
            }
            for (char in morse) {
                when (char) {
                    '.' -> { vibrator.vibrate(VibrationEffect.createOneShot(dotMs,  VibrationEffect.DEFAULT_AMPLITUDE)); delay(dotMs  + gapMs) }
                    '-' -> { vibrator.vibrate(VibrationEffect.createOneShot(dashMs, VibrationEffect.DEFAULT_AMPLITUDE)); delay(dashMs + gapMs) }
                    ' ' -> delay(gapMs)
                    '/' -> delay(wordMs)
                }
            }
        }
    }
}

@Composable
fun MorseCodeScreen(onBack: () -> Unit, viewModel: MorseCodeViewModel = koinViewModel()) {
    val clipboard  = LocalClipboardManager.current
    val context    = LocalContext.current
    val view       = LocalView.current
    val haptic     = koinInject<HapticHelper>()
    val settings   = koinInject<SettingsRepository>()
    val touchVib   by settings.touchVibration.collectAsState(initial = true)

    Column(Modifier.fillMaxSize()) {
        NexusTopBar(title = "Morse Code", onBack = onBack)

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode selection
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(MorseMode.TEXT_TO_MORSE to "Text → Morse", MorseMode.MORSE_TO_TEXT to "Morse → Text").forEach { (m, label) ->
                    FilterChip(
                        selected = viewModel.mode == m,
                        onClick  = { viewModel.onModeChanged(m) },
                        label    = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            OutlinedTextField(
                value         = viewModel.input,
                onValueChange = viewModel::onInputChanged,
                label         = { Text(if (viewModel.mode == MorseMode.TEXT_TO_MORSE) "Enter text" else "Enter Morse code (use spaces and /)") },
                modifier      = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp)
                    .semantics { contentDescription = "Morse code input. ${if (viewModel.mode == MorseMode.TEXT_TO_MORSE) "Type text to convert to Morse code." else "Enter Morse code using dots, dashes, spaces, and / for word breaks."}" },
                maxLines = 8
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick  = {
                        haptic.click(view, touchVib)
                        viewModel.playMorseVibration(context)
                        view.announceForAccessibility("Playing Morse code via vibration")
                    },
                    enabled  = viewModel.output.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Vibration, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Vibrate")
                }
                OutlinedButton(
                    onClick  = {
                        haptic.click(view, touchVib)
                        viewModel.swapInputOutput()
                    },
                    enabled  = viewModel.output.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.SwapVert, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Swap")
                }
                IconButton(onClick = {
                    haptic.click(view, touchVib)
                    viewModel.clearAll()
                }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Clear")
                }
            }

            AnimatedVisibility(viewModel.output.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider()
                    Text("Result", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics { heading() })
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Text(
                            viewModel.output,
                            Modifier.fillMaxWidth().padding(12.dp).semantics { contentDescription = "Morse result: ${viewModel.output}" },
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                        )
                    }
                    OutlinedButton(
                        onClick  = {
                            haptic.click(view, touchVib)
                            clipboard.setText(AnnotatedString(viewModel.output))
                            view.announceForAccessibility("Morse code copied to clipboard")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Icon(Icons.Filled.ContentCopy, null); Spacer(Modifier.width(4.dp)); Text("Copy Result") }
                }
            }
        }
    }
}
