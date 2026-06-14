package com.nexuswavetech.nexusplus.features.tts

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.nexuswavetech.nexusplus.core.HapticHelper
import com.nexuswavetech.nexusplus.core.SettingsRepository
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlin.math.roundToInt
import java.util.Locale

// ─── Mode display metadata ────────────────────────────────────────────────

private data class ModeOption(
    val mode: NseSpeechMode,
    val label: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private val SPEECH_MODES = listOf(
    ModeOption(
        mode        = NseSpeechMode.Auto,
        label       = "Auto",
        description = "Detect language automatically from input text",
        icon        = Icons.Filled.AutoAwesome,
    ),
    ModeOption(
        mode        = NseSpeechMode.Mix,
        label       = "Mix",
        description = "Multi-language — voices switch per script segment",
        icon        = Icons.Filled.Translate,
    ),
)

// ─── Screen ───────────────────────────────────────────────────────────────

@Composable
fun NexusTtsScreen(
    onBack: () -> Unit,
    viewModel: NseViewModel = koinViewModel(),
) {
    val view          = LocalView.current
    val haptic        = koinInject<HapticHelper>()
    val settings      = koinInject<SettingsRepository>()
    val touchVib      by settings.touchVibration.collectAsState(initial = true)

    val engineState   by viewModel.engineState.collectAsState()
    val inputText     by viewModel.inputText.collectAsState()
    val pitch         by viewModel.pitch.collectAsState()
    val speechRate    by viewModel.speechRate.collectAsState()
    val mode          by viewModel.mode.collectAsState()
    val detectedLocale by viewModel.detectedLocale.collectAsState()
    val isSpeaking    by viewModel.isSpeaking.collectAsState()
    val isReady       by viewModel.isReady.collectAsState()
    val voices        by viewModel.availableVoices.collectAsState()
    val selectedVoice by viewModel.selectedVoice.collectAsState()

    var showVoicePicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {

        NexusTopBar(title = "Nexus Speech Engine", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {

            // ── Sacred greeting ────────────────────────────────────────────
            JaiShriKrishnaHeader()

            // ── Engine status ──────────────────────────────────────────────
            EngineStatusBanner(state = engineState)

            // ── Mode selector ──────────────────────────────────────────────
            ModeSelectorRow(
                currentMode     = mode,
                onModeSelected  = viewModel::onModeChange,
                onPickVoice     = { showVoicePicker = true },
                selectedVoice   = selectedVoice,
            )

            // ── Text input ─────────────────────────────────────────────────
            OutlinedTextField(
                value         = inputText,
                onValueChange = viewModel::onTextChange,
                label         = { Text("Enter text to speak…") },
                placeholder   = { Text("Type or paste text in any language") },
                modifier      = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp)
                    .semantics {
                        contentDescription =
                            "Text input for speech synthesis. " +
                            "Language is detected automatically from your input."
                    },
                maxLines      = 8,
            )

            // ── Auto-detected language badge ───────────────────────────────
            AnimatedVisibility(
                visible = detectedLocale != null && inputText.length > 5,
                enter   = fadeIn(tween(200)) + expandVertically(),
                exit    = fadeOut(tween(150)) + shrinkVertically(),
            ) {
                detectedLocale?.let { locale ->
                    DetectedLanguageBadge(locale = locale)
                }
            }

            // ── Pitch control ──────────────────────────────────────────────
            SpeechParamSlider(
                label          = "Pitch",
                value          = pitch,
                range          = 0.5f..2.0f,
                steps          = 14,
                onValueChange  = viewModel::onPitchChange,
                accessibilityLabel = "Pitch slider. Adjust voice pitch from 0.5 to 2.0.",
            )

            // ── Speed control ──────────────────────────────────────────────
            SpeechParamSlider(
                label          = "Speed",
                value          = speechRate,
                range          = 0.25f..3.0f,
                steps          = 27,
                onValueChange  = viewModel::onSpeechRateChange,
                accessibilityLabel = "Speech rate slider. Adjust speaking speed from 0.25 to 3.0.",
            )

            // ── Action buttons ─────────────────────────────────────────────
            Row(
                modifier             = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick  = {
                        haptic.confirm(view, touchVib)
                        viewModel.speak()
                        val localeLabel = detectedLocale?.displayLanguage ?: "default language"
                        view.announceForAccessibility("Speaking in $localeLabel")
                    },
                    enabled  = isReady && inputText.isNotBlank() && !isSpeaking,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .semantics {
                            contentDescription =
                                "Speak. Activates Nexus Speech Engine synthesis."
                        },
                ) {
                    Icon(Icons.Filled.RecordVoiceOver, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Speak")
                }

                OutlinedButton(
                    onClick  = {
                        haptic.click(view, touchVib)
                        viewModel.stop()
                        view.announceForAccessibility("Speech stopped")
                    },
                    enabled  = isSpeaking,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .semantics { contentDescription = "Stop speech playback." },
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Stop")
                }
            }

            OutlinedButton(
                onClick  = {
                    haptic.click(view, touchVib)
                    viewModel.clearText()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Clear text input." },
            ) {
                Icon(Icons.Filled.Clear, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Clear")
            }

            // ── Available voices info ──────────────────────────────────────
            if (voices.isNotEmpty()) {
                Text(
                    text  = "${voices.size} voices available on this device",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics {
                        contentDescription = "${voices.size} TTS voices available"
                    },
                )
            }
        }
    }

    // ── Voice picker bottom sheet ──────────────────────────────────────────
    if (showVoicePicker) {
        VoicePickerSheet(
            voices        = voices,
            selectedVoice = selectedVoice,
            onVoiceSelected = { voice ->
                viewModel.onVoiceSelected(voice)
                showVoicePicker = false
            },
            onDismiss = { showVoicePicker = false },
        )
    }
}

// ─── Sub-components ───────────────────────────────────────────────────────

@Composable
private fun JaiShriKrishnaHeader() {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "Jai Shri Krishna"
            },
    ) {
        Box(
            modifier         = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text      = "🙏 Jai Shri Krishna",
                style     = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color     = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun EngineStatusBanner(state: NseState) {
    AnimatedVisibility(
        visible = state != NseState.Ready && state != NseState.Speaking,
        enter   = fadeIn() + expandVertically(),
        exit    = fadeOut() + shrinkVertically(),
    ) {
        val (text, color) = when (state) {
            is NseState.Initialising -> "Nexus Speech Engine initialising…" to
                MaterialTheme.colorScheme.onSurfaceVariant
            is NseState.Error        -> "⚠ ${state.message}" to
                MaterialTheme.colorScheme.error
            else                     -> "" to MaterialTheme.colorScheme.onSurface
        }
        if (text.isNotEmpty()) {
            Text(
                text     = text,
                color    = color,
                style    = MaterialTheme.typography.bodySmall,
                modifier = Modifier.semantics { contentDescription = text },
            )
        }
    }
}

@Composable
private fun ModeSelectorRow(
    currentMode: NseSpeechMode,
    onModeSelected: (NseSpeechMode) -> Unit,
    onPickVoice: () -> Unit,
    selectedVoice: NseVoiceProfile?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text  = "Synthesis Mode",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        )
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SPEECH_MODES.forEach { option ->
                val selected = currentMode::class == option.mode::class
                FilterChip(
                    selected  = selected,
                    onClick   = { onModeSelected(option.mode) },
                    label     = { Text(option.label) },
                    leadingIcon = {
                        Icon(option.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    modifier  = Modifier
                        .semantics {
                            contentDescription = "${option.label} mode. ${option.description}." +
                                if (selected) " Currently selected." else ""
                            role = Role.RadioButton
                        },
                )
            }

            // Single voice chip
            val singleSelected = currentMode is NseSpeechMode.SingleVoice
            FilterChip(
                selected  = singleSelected,
                onClick   = onPickVoice,
                label     = {
                    Text(
                        if (singleSelected && selectedVoice != null)
                            selectedVoice.locale.displayLanguage
                        else "Single Voice"
                    )
                },
                leadingIcon = {
                    Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                modifier  = Modifier.semantics {
                    contentDescription = "Single voice mode. Tap to pick a specific voice."
                    role = Role.RadioButton
                },
            )
        }
    }
}

@Composable
private fun DetectedLanguageBadge(locale: Locale) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier             = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                Icons.Filled.Translate,
                contentDescription = null,
                modifier           = Modifier.size(16.dp),
            )
            Text(
                text     = "Auto-detected: ${locale.displayLanguage}",
                style    = MaterialTheme.typography.labelMedium,
                color    = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.semantics {
                    contentDescription = "Language auto-detected as ${locale.displayLanguage}"
                },
            )
        }
    }
}

@Composable
private fun SpeechParamSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    accessibilityLabel: String,
) {
    val displayValue = (value * 10).roundToInt() / 10.0
    Column {
        Text(
            text     = "$label: ${displayValue}x",
            style    = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.semantics {
                contentDescription = "$label control. Current value: ${displayValue}x"
            },
        )
        Slider(
            value         = value,
            onValueChange = onValueChange,
            valueRange    = range,
            steps         = steps,
            modifier      = Modifier.semantics {
                contentDescription = "$accessibilityLabel Current: ${displayValue}x"
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoicePickerSheet(
    voices: List<NseVoiceProfile>,
    selectedVoice: NseVoiceProfile?,
    onVoiceSelected: (NseVoiceProfile?) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text     = "Choose a Voice",
                style    = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 8.dp),
            )

            // "Auto" option to reset
            ListItem(
                headlineContent = { Text("Auto — detect language automatically") },
                leadingContent  = {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                },
                modifier        = Modifier
                    .selectable(
                        selected = selectedVoice == null,
                        onClick  = { onVoiceSelected(null) },
                    )
                    .semantics { contentDescription = "Auto mode, detect language automatically" },
            )
            HorizontalDivider()

            val grouped = voices.groupBy { it.locale.displayLanguage }
            grouped.entries.sortedBy { it.key }.forEach { (language, voiceList) ->
                Text(
                    text     = language,
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                )
                voiceList.forEach { voice ->
                    ListItem(
                        headlineContent  = { Text(voice.name) },
                        supportingContent = {
                            Text(
                                buildString {
                                    if (voice.isNetworkRequired) append("Network required · ")
                                    append("Quality ${voice.quality}")
                                },
                            )
                        },
                        trailingContent  = {
                            if (voice == selectedVoice) {
                                Icon(Icons.Filled.Check, contentDescription = "Selected")
                            }
                        },
                        modifier         = Modifier
                            .selectable(
                                selected = voice == selectedVoice,
                                onClick  = { onVoiceSelected(voice) },
                            )
                            .semantics {
                                contentDescription =
                                    "Voice: ${voice.name}. ${voice.locale.displayLanguage}." +
                                    if (voice == selectedVoice) " Currently selected." else ""
                            },
                    )
                }
            }
        }
    }
}
