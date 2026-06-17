package com.nexuswavetech.nexusplus.features.tts

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexuswavetech.nexusplus.core.SettingsRepository
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexusTtsScreen(
    onBack: () -> Unit,
    viewModel: NseViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val engineState by viewModel.engineState.collectAsState()
    val voices by viewModel.availableVoices.collectAsState()
    val selectedVoice by viewModel.selectedVoice.collectAsState()
    val secondaryVoice by viewModel.secondaryVoice.collectAsState()
    val pitch by viewModel.pitch.collectAsState()
    val speechRate by viewModel.speechRate.collectAsState()
    val screenReaderMode by viewModel.screenReaderMode.collectAsState()
    val hasUnsaved by viewModel.hasUnsavedChanges.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val detectedLocale by viewModel.detectedLocale.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isPipeline by remember { mutableStateOf(viewModel.isPipelineEngine) }
    val cachedCount by viewModel.cachedPhraseCount.collectAsState()

    // Accessibility toggles
    val notificationFilter by viewModel.notificationFilter.collectAsState()
    val windowChange by viewModel.windowChangeDetection.collectAsState()
    val focusTracking by viewModel.focusTracking.collectAsState()
    val continuousRead by viewModel.continuousRead.collectAsState()
    val duplicateFilter by viewModel.duplicateFilter.collectAsState()
    val autoStart by viewModel.autoStart.collectAsState()

    val snackbarHost = remember { SnackbarHostState() }

    val modeOptions = remember {
        listOf(
            SettingsRepository.TTS_MODE_AUTO to "Auto \u2014 detect language and voice",
            SettingsRepository.TTS_MODE_SINGLE to "Single \u2014 one voice for all content",
            SettingsRepository.TTS_MODE_DUAL to "Dual \u2014 primary and secondary voices",
            SettingsRepository.TTS_MODE_MIXED to "Mixed \u2014 intelligent multi-voice switching",
        )
    }

    val languageOptions: List<Pair<String, String>> = remember(voices) {
        val extra = voices.map { it.locale.language }
            .distinct()
            .sorted()
            .map { code -> code to (Locale(code).displayLanguage.ifBlank { code }) }
        listOf(SettingsRepository.TTS_LANG_AUTO to "Auto (system locale)") + extra
    }

    val filteredVoices: List<NseVoiceProfile> = remember(voices, screenReaderMode) {
        when (screenReaderMode) {
            SettingsRepository.TTS_MODE_SINGLE -> voices
            SettingsRepository.TTS_MODE_DUAL -> voices
            else -> voices
        }
    }

    val voiceOptions: List<Pair<NseVoiceProfile?, String>> = remember(filteredVoices) {
        listOf(null to "Auto") + filteredVoices.map { v ->
            v to v.locale.displayName.ifBlank { v.locale.language }
        }
    }

    val secondaryVoiceOptions: List<Pair<NseVoiceProfile?, String>> = remember(filteredVoices, selectedVoice) {
        listOf(null to "Auto") + filteredVoices
            .filter { it != selectedVoice }
            .map { v -> v to v.locale.displayName.ifBlank { v.locale.language } }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NexusTopBar(title = "NSE Auto TTS Screen Reader", onBack = onBack)
        SnackbarHost(hostState = snackbarHost, modifier = Modifier.padding(8.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Engine status banner ─────────────────────────────────────────
            EngineStatusBanner(state = engineState)

            // ── Engine badge ───────────────────────────────────────────────
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        Icons.Filled.RecordVoiceOver,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Nexus Speech Engine 4.0",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            if (isPipeline) "NSE Pipeline \u00b7 PCM cache \u00b7 $cachedCount cached"
                            else "NSE Standard \u00b7 Android TTS",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                        )
                        if (detectedLocale != null) {
                            Text(
                                "Detected: ${detectedLocale?.displayName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                            )
                        }
                    }
                }
            }

            // ── Quick Speak Test ───────────────────────────────────────────
            SettingsSection(title = "Quick Speak Test") {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = viewModel::onTextChange,
                    label = { Text("Enter text to speak") },
                    placeholder = { Text("Type or paste text in any language") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    trailingIcon = {
                        if (inputText.isNotEmpty()) {
                            IconButton(onClick = viewModel::clearText) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear text")
                            }
                        }
                    }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = viewModel::speak,
                        enabled = inputText.isNotBlank() && engineState == NseState.Ready,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Speak")
                    }
                    OutlinedButton(
                        onClick = viewModel::stop,
                        enabled = isSpeaking,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Stop")
                    }
                }
            }

            // ── Screen Reader Mode ───────────────────────────────────────
            SettingsSection(title = "Screen Reader Mode") {
                modeOptions.forEach { (mode, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = screenReaderMode == mode,
                                onClick = { viewModel.onScreenReaderModeChange(mode) }
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = screenReaderMode == mode,
                            onClick = null,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            label,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            // ── Voice Selection ──────────────────────────────────────────
            AnimatedVisibility(
                visible = screenReaderMode == SettingsRepository.TTS_MODE_SINGLE ||
                    screenReaderMode == SettingsRepository.TTS_MODE_DUAL ||
                    screenReaderMode == SettingsRepository.TTS_MODE_MIXED,
                enter = fadeIn(tween(200)) + expandVertically(),
                exit = fadeOut(tween(150)) + shrinkVertically(),
            ) {
                SettingsSection(title = if (screenReaderMode == SettingsRepository.TTS_MODE_DUAL) "Primary Voice" else "Voice") {
                    TtsDropdown(
                        options = voiceOptions,
                        selected = selectedVoice,
                        onSelect = viewModel::onVoiceSelected,
                    )
                    if (filteredVoices.isEmpty()) {
                        Text(
                            "No voices found. Check system TTS settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = screenReaderMode == SettingsRepository.TTS_MODE_DUAL,
                enter = fadeIn(tween(200)) + expandVertically(),
                exit = fadeOut(tween(150)) + shrinkVertically(),
            ) {
                SettingsSection(title = "Secondary Voice") {
                    TtsDropdown(
                        options = secondaryVoiceOptions,
                        selected = secondaryVoice,
                        onSelect = viewModel::onSecondaryVoiceSelected,
                    )
                }
            }

            // ── Voice Parameters ──────────────────────────────────────
            SettingsSection(title = "Voice Parameters") {
                Text(
                    "Speed: ${"%.1f".format(speechRate)}×",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics {
                        contentDescription = "Speech speed: ${"%.1f".format(speechRate)} times"
                    },
                )
                Slider(
                    value = speechRate,
                    onValueChange = viewModel::onSpeechRateChange,
                    valueRange = 0.25f..3.0f,
                    steps = 27,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Pitch: ${"%.1f".format(pitch)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics {
                        contentDescription = "Voice pitch: ${"%.1f".format(pitch)}"
                    },
                )
                Slider(
                    value = pitch,
                    onValueChange = viewModel::onPitchChange,
                    valueRange = 0.5f..2.0f,
                    steps = 14,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // ── Accessibility Behavior ────────────────────────────────
            SettingsSection(title = "Accessibility Behavior") {
                ToggleItem(
                    label = "Filter Notifications",
                    description = "Skip speaking notification pop-ups",
                    checked = notificationFilter,
                    onCheckedChange = viewModel::onNotificationFilterChange,
                )
                ToggleItem(
                    label = "Window Change Detection",
                    description = "Announce when switching apps or windows",
                    checked = windowChange,
                    onCheckedChange = viewModel::onWindowChangeDetectionChange,
                )
                ToggleItem(
                    label = "Focus Tracking",
                    description = "Follow accessibility focus in real time",
                    checked = focusTracking,
                    onCheckedChange = viewModel::onFocusTrackingChange,
                )
                ToggleItem(
                    label = "Continuous Reading",
                    description = "Read all visible content when a window changes",
                    checked = continuousRead,
                    onCheckedChange = viewModel::onContinuousReadChange,
                )
                ToggleItem(
                    label = "Duplicate Filter",
                    description = "Prevent repeating the same content twice",
                    checked = duplicateFilter,
                    onCheckedChange = viewModel::onDuplicateFilterChange,
                )
                ToggleItem(
                    label = "Auto Start",
                    description = "Automatically start the screen reader when the service connects",
                    checked = autoStart,
                    onCheckedChange = viewModel::onAutoStartChange,
                )
            }

            // ── Settings Actions ──────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        viewModel.saveSettings()
                        scope.launch {
                            snackbarHost.showSnackbar("Settings saved successfully")
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = hasUnsaved,
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Save Settings")
                }
                OutlinedButton(
                    onClick = {
                        viewModel.resetSettings()
                        scope.launch {
                            snackbarHost.showSnackbar("Settings reset to defaults")
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Reset")
                }
            }

            // ── System TTS Settings ──────────────────────────────────
            OutlinedButton(
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent("com.android.settings.TTS_SETTINGS")
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }.onFailure {
                        context.startActivity(
                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Open system Text-to-Speech settings." },
            ) {
                Icon(Icons.Filled.Settings, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Open System TTS Settings")
            }

            // ── Info ─────────────────────────────────────────────────
            Text(
                "Nexus Speech Engine is registered as a system TTS provider and Accessibility Service. " +
                    "Enable it in Settings \u2192 Accessibility to use the Auto TTS Screen Reader device-wide.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Private sub-components ───────────────────────────────────────────────

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
            )
            content()
        }
    }
}

@Composable
private fun ToggleItem(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> TtsDropdown(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selected }?.second ?: selected.toString()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            singleLine = true,
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = { onSelect(value); expanded = false },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    leadingIcon = if (value == selected) {
                        { Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary) }
                    } else null,
                )
            }
        }
    }
}

@Composable
private fun EngineStatusBanner(state: NseState) {
    AnimatedVisibility(
        visible = state != NseState.Ready && state != NseState.Speaking,
        enter = fadeIn(tween(200)) + expandVertically(),
        exit = fadeOut(tween(150)) + shrinkVertically(),
    ) {
        val (text, color) = when (state) {
            is NseState.Initialising -> "Nexus Speech Engine initialising\u2026" to
                MaterialTheme.colorScheme.onSurfaceVariant
            is NseState.Error -> "\u26a0 ${state.message}" to
                MaterialTheme.colorScheme.error
            else -> "" to MaterialTheme.colorScheme.onSurface
        }
        if (text.isNotEmpty()) {
            Text(
                text = text,
                color = color,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.semantics { contentDescription = text },
            )
        }
    }
}
