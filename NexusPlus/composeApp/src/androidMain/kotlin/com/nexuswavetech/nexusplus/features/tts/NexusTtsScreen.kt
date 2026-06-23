package com.nexuswavetech.nexusplus.features.tts

import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.ui.unit.sp
import com.nexuswavetech.nexusplus.core.SettingsRepository
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private data class TtsEngineInfo(val packageName: String, val label: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexusTtsScreen(
    onBack: () -> Unit,
    viewModel: NseViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    val engineState    by viewModel.engineState.collectAsState()
    val voices         by viewModel.availableVoices.collectAsState()
    val selectedVoice  by viewModel.selectedVoice.collectAsState()
    val secondaryVoice by viewModel.secondaryVoice.collectAsState()
    val pitch          by viewModel.pitch.collectAsState()
    val speechRate     by viewModel.speechRate.collectAsState()
    val screenReaderMode by viewModel.screenReaderMode.collectAsState()
    val hasUnsaved     by viewModel.hasUnsavedChanges.collectAsState()
    val isSpeaking     by viewModel.isSpeaking.collectAsState()
    val detectedLocale by viewModel.detectedLocale.collectAsState()
    val inputText      by viewModel.inputText.collectAsState()
    val isPipeline     by remember { mutableStateOf(viewModel.isPipelineEngine) }
    val cachedCount    by viewModel.cachedPhraseCount.collectAsState()

    val notificationFilter by viewModel.notificationFilter.collectAsState()
    val windowChange       by viewModel.windowChangeDetection.collectAsState()
    val focusTracking      by viewModel.focusTracking.collectAsState()
    val continuousRead     by viewModel.continuousRead.collectAsState()
    val duplicateFilter    by viewModel.duplicateFilter.collectAsState()
    val autoStart          by viewModel.autoStart.collectAsState()

    val snackbarHost = remember { SnackbarHostState() }

    // ── Installed TTS engines from system ─────────────────────────────────
    val installedEngines: List<TtsEngineInfo> = remember {
        try {
            val intent = Intent("android.intent.action.TTS_SERVICE")
            context.packageManager
                .queryIntentServices(intent, PackageManager.GET_META_DATA)
                .mapNotNull { ri -> ri.serviceInfo }
                .map { si -> TtsEngineInfo(si.packageName, si.loadLabel(context.packageManager).toString()) }
        } catch (_: Exception) { emptyList() }
    }

    val currentEnginePkg: String = remember {
        try {
            Settings.Secure.getString(context.contentResolver, "tts_default_synth") ?: ""
        } catch (_: Exception) { "" }
    }
    val currentEngineLabel = remember(currentEnginePkg, installedEngines) {
        installedEngines.firstOrNull { it.packageName == currentEnginePkg }?.label
            ?: currentEnginePkg.substringAfterLast('.').ifBlank { "System Default" }
    }

    // ── Language options built from available voices ───────────────────────
    val languageOptions: List<Pair<String, String>> = remember(voices) {
        val extras = voices
            .map { it.locale.language }
            .distinct()
            .sorted()
            .map { code -> code to (java.util.Locale(code).displayLanguage.ifBlank { code }) }
        listOf(SettingsRepository.TTS_LANG_AUTO to "Auto (detect from text)") + extras
    }

    // ── Currently selected language (derived from selectedVoice or AUTO) ──
    var selectedLanguage by remember(selectedVoice) {
        mutableStateOf(selectedVoice?.locale?.language ?: SettingsRepository.TTS_LANG_AUTO)
    }

    // ── Voices filtered by selected language ──────────────────────────────
    val filteredVoices: List<NseVoiceProfile> = remember(voices, selectedLanguage) {
        if (selectedLanguage == SettingsRepository.TTS_LANG_AUTO) voices
        else voices.filter { it.locale.language == selectedLanguage }
    }

    val voiceOptions: List<Pair<NseVoiceProfile?, String>> = remember(filteredVoices) {
        listOf(null to "Auto") + filteredVoices.map { v ->
            v to java.util.Locale(v.locale.language, v.locale.country).displayName.ifBlank { v.name }
        }
    }

    val secondaryVoiceOptions: List<Pair<NseVoiceProfile?, String>> = remember(filteredVoices, selectedVoice) {
        listOf(null to "None") + filteredVoices
            .filter { it != selectedVoice }
            .map { v -> v to java.util.Locale(v.locale.language, v.locale.country).displayName.ifBlank { v.name } }
    }

    val modeOptions = remember {
        listOf(
            SettingsRepository.TTS_MODE_AUTO   to "Auto — detect language automatically",
            SettingsRepository.TTS_MODE_SINGLE to "Single — one voice for all text",
            SettingsRepository.TTS_MODE_DUAL   to "Mix — blend primary and secondary voices",
            SettingsRepository.TTS_MODE_MIXED  to "Advanced — pipeline with language detection",
        )
    }

    Scaffold(
        topBar = { NexusTopBar(title = "Speech Engine (NSE 4.0)", onBack = onBack) },
        snackbarHost = { SnackbarHost(hostState = snackbarHost) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {

            // ── Engine Status Banner ────────────────────────────────────────
            EngineStatusBanner(state = engineState)

            // ── Engine Badge ────────────────────────────────────────────────
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        modifier = Modifier.size(44.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.RecordVoiceOver, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp))
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Nexus Auto Speech Engine 4.0",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            buildString {
                                append(if (isPipeline) "NSE Pipeline · PCM cache · $cachedCount cached" else "NSE Standard · Android TTS")
                                if (detectedLocale != null) append(" · ${java.util.Locale(detectedLocale!!.language).displayLanguage}")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                        )
                    }
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (engineState == NseState.Ready || engineState == NseState.Speaking)
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                    ) {
                        Text(
                            text = when (engineState) {
                                NseState.Ready   -> "Ready"
                                NseState.Speaking -> "Speaking"
                                is NseState.Error -> "Error"
                                else -> "Init…"
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (engineState == NseState.Ready || engineState == NseState.Speaking)
                                MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            // ══════════════════════════════════════════════════════════════
            // SECTION 1 — LANGUAGE
            // ══════════════════════════════════════════════════════════════
            NseSection(title = "Language", icon = Icons.Filled.Language) {
                Text(
                    "Select the primary language for speech synthesis",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                NseDropdown(
                    label = "Primary Language",
                    options = languageOptions,
                    selected = selectedLanguage,
                    onSelect = { lang ->
                        selectedLanguage = lang
                        viewModel.onVoiceSelected(null)
                    },
                )
            }

            // ══════════════════════════════════════════════════════════════
            // SECTION 2 — TTS ENGINE (which engine is active)
            // ══════════════════════════════════════════════════════════════
            NseSection(title = "TTS Engine", icon = Icons.Filled.Settings) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Hub, null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Active Engine", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(currentEngineLabel, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                    }
                }

                if (installedEngines.size > 1) {
                    Text(
                        "Installed: ${installedEngines.joinToString(", ") { it.label }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
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
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Settings, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Change Engine", fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW).apply {
                                        data = android.net.Uri.parse("market://details?id=com.google.android.tts")
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                )
                            }.onFailure {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW).apply {
                                        data = android.net.Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.tts")
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Download, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Get Google TTS", fontSize = 13.sp)
                    }
                }
            }

            // ══════════════════════════════════════════════════════════════
            // SECTION 3 — VOICE
            // ══════════════════════════════════════════════════════════════
            NseSection(title = "Voice", icon = Icons.Filled.RecordVoiceOver) {
                Text(
                    "Choose the voice to use for reading text",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                NseDropdown(
                    label = "Primary Voice",
                    options = voiceOptions,
                    selected = selectedVoice,
                    onSelect = viewModel::onVoiceSelected,
                )
                if (filteredVoices.isEmpty() && selectedLanguage != SettingsRepository.TTS_LANG_AUTO) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(16.dp))
                            Text(
                                "No voices found for this language. Download voices from Google TTS.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = screenReaderMode == SettingsRepository.TTS_MODE_DUAL) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text("Secondary Voice (Mix Mode)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                        NseDropdown(
                            label = "Secondary Voice",
                            options = secondaryVoiceOptions,
                            selected = secondaryVoice,
                            onSelect = viewModel::onSecondaryVoiceSelected,
                        )
                    }
                }
            }

            // ══════════════════════════════════════════════════════════════
            // SECTION 4 — QUICK SPEAK TEST
            // ══════════════════════════════════════════════════════════════
            NseSection(title = "Test Speech", icon = Icons.Filled.PlayCircle) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = viewModel::onTextChange,
                    label = { Text("Enter text to speak") },
                    placeholder = { Text("Type in any language…") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    trailingIcon = {
                        if (inputText.isNotEmpty()) {
                            IconButton(onClick = viewModel::clearText) {
                                Icon(Icons.Filled.Clear, null)
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
                        enabled = inputText.isNotBlank() && (engineState == NseState.Ready),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Speak")
                    }
                    OutlinedButton(
                        onClick = viewModel::stop,
                        enabled = isSpeaking,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Stop, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Stop")
                    }
                }
            }

            // ══════════════════════════════════════════════════════════════
            // SECTION 5 — VOICE PARAMETERS
            // ══════════════════════════════════════════════════════════════
            NseSection(title = "Voice Parameters", icon = Icons.Filled.Tune) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Speed", style = MaterialTheme.typography.bodyMedium)
                        Text("${"%.2f".format(speechRate)}×", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = speechRate,
                        onValueChange = viewModel::onSpeechRateChange,
                        valueRange = 0.25f..3.0f,
                        steps = 27,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Pitch", style = MaterialTheme.typography.bodyMedium)
                        Text("${"%.2f".format(pitch)}", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = pitch,
                        onValueChange = viewModel::onPitchChange,
                        valueRange = 0.5f..2.0f,
                        steps = 14,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // ══════════════════════════════════════════════════════════════
            // SECTION 6 — SCREEN READER MODE (Advanced)
            // ══════════════════════════════════════════════════════════════
            NseSection(title = "Screen Reader Mode", icon = Icons.Filled.Hearing) {
                Text(
                    "Controls how NSE reads content when used as an accessibility service",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                modeOptions.forEach { (mode, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = screenReaderMode == mode, onClick = { viewModel.onScreenReaderModeChange(mode) })
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = screenReaderMode == mode, onClick = null)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(label.substringBefore(" —"), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text(label.substringAfter("— "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // ══════════════════════════════════════════════════════════════
            // SECTION 7 — ACCESSIBILITY BEHAVIOUR
            // ══════════════════════════════════════════════════════════════
            NseSection(title = "Accessibility Behaviour", icon = Icons.Filled.Accessibility) {
                NseToggle("Filter Notifications", "Skip speaking notification pop-ups", notificationFilter, viewModel::onNotificationFilterChange)
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                NseToggle("Window Change Detection", "Announce when switching apps or screens", windowChange, viewModel::onWindowChangeDetectionChange)
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                NseToggle("Focus Tracking", "Follow accessibility focus in real time", focusTracking, viewModel::onFocusTrackingChange)
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                NseToggle("Continuous Reading", "Read all visible content on window changes", continuousRead, viewModel::onContinuousReadChange)
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                NseToggle("Duplicate Filter", "Prevent repeating identical content", duplicateFilter, viewModel::onDuplicateFilterChange)
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                NseToggle("Auto Start", "Start screen reader when service connects", autoStart, viewModel::onAutoStartChange)
            }

            // ── Save / Reset ──────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        viewModel.saveSettings()
                        scope.launch { snackbarHost.showSnackbar("Settings saved") }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = hasUnsaved,
                ) {
                    Icon(Icons.Filled.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save Settings")
                }
                OutlinedButton(
                    onClick = {
                        viewModel.resetSettings()
                        scope.launch { snackbarHost.showSnackbar("Reset to defaults") }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Reset")
                }
            }

            // ── Download Voices ──────────────────────────────────────────
            FilledTonalButton(
                onClick = {
                    scope.launch {
                        var launched = false
                        runCatching {
                            context.startActivity(
                                Intent("com.android.settings.TTS_SETTINGS")
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                            launched = true
                        }
                        if (!launched) runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Download, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Download / Manage Voices")
            }

            // ── Info card ─────────────────────────────────────────────────
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = MaterialTheme.shapes.medium,
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text(
                        "NSE is registered as a system TTS provider and Accessibility Service. " +
                        "Enable it in Settings → Accessibility to use the screen reader device-wide.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Reusable sub-components ──────────────────────────────────────────────────

@Composable
private fun NseSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape    = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            content()
        }
    }
}

@Composable
private fun NseToggle(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> NseDropdown(
    label: String,
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
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            singleLine = true,
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
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
        exit  = fadeOut(tween(150)) + shrinkVertically(),
    ) {
        val (text, color) = when (state) {
            is NseState.Initialising -> "Nexus Speech Engine initialising…" to MaterialTheme.colorScheme.onSurfaceVariant
            is NseState.Error        -> "⚠ ${state.message}" to MaterialTheme.colorScheme.error
            else -> "" to MaterialTheme.colorScheme.onSurface
        }
        if (text.isNotEmpty()) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (state is NseState.Error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (state is NseState.Error) Icons.Filled.ErrorOutline else Icons.Filled.HourglassEmpty,
                        null, tint = color, modifier = Modifier.size(16.dp)
                    )
                    Text(text, color = color, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
