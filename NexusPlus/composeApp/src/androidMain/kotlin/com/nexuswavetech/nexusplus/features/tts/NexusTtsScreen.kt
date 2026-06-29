package com.nexuswavetech.nexusplus.features.tts

import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    val inputText      by viewModel.inputText.collectAsState()
    val notificationFilter by viewModel.notificationFilter.collectAsState()
    val windowChange       by viewModel.windowChangeDetection.collectAsState()
    val focusTracking      by viewModel.focusTracking.collectAsState()
    val continuousRead     by viewModel.continuousRead.collectAsState()
    val duplicateFilter    by viewModel.duplicateFilter.collectAsState()
    val autoStart          by viewModel.autoStart.collectAsState()

    val snackbarHost = remember { SnackbarHostState() }

    val installedEngines: List<TtsEngineInfo> = remember {
        try {
            val intent = Intent("android.intent.action.TTS_SERVICE")
            context.packageManager
                .queryIntentServices(intent, PackageManager.GET_META_DATA)
                .mapNotNull { it.serviceInfo }
                .map { si -> TtsEngineInfo(si.packageName, si.loadLabel(context.packageManager).toString()) }
        } catch (_: Exception) { emptyList() }
    }

    val currentEnginePkg: String = remember {
        try { Settings.Secure.getString(context.contentResolver, "tts_default_synth") ?: "" }
        catch (_: Exception) { "" }
    }
    val currentEngineLabel = remember(currentEnginePkg, installedEngines) {
        installedEngines.firstOrNull { it.packageName == currentEnginePkg }?.label
            ?: if (currentEnginePkg.isNotBlank()) currentEnginePkg.substringAfterLast('.') else "System Default"
    }

    val languageOptions: List<Pair<String, String>> = remember(voices) {
        val extras = voices
            .map { it.locale.language }
            .distinct()
            .sorted()
            .map { code -> code to (java.util.Locale(code).displayLanguage.ifBlank { code }) }
        listOf(SettingsRepository.TTS_LANG_AUTO to "Auto") + extras
    }

    var selectedLanguage by remember(selectedVoice) {
        mutableStateOf(selectedVoice?.locale?.language ?: SettingsRepository.TTS_LANG_AUTO)
    }

    val filteredVoices: List<NseVoiceProfile> = remember(voices, selectedLanguage) {
        if (selectedLanguage == SettingsRepository.TTS_LANG_AUTO) voices
        else voices.filter { it.locale.language == selectedLanguage }
    }

    val modeOptions = remember {
        listOf(
            SettingsRepository.TTS_MODE_AUTO   to "Auto — detect language automatically",
            SettingsRepository.TTS_MODE_SINGLE to "Single — one voice for all text",
            SettingsRepository.TTS_MODE_DUAL   to "Mix — blend primary + secondary voice",
            SettingsRepository.TTS_MODE_MIXED  to "Advanced — pipeline with language detection",
        )
    }

    val isReady = engineState == NseState.Ready || engineState == NseState.Speaking

    Scaffold(
        topBar = {
            NexusTopBar(
                title = "Auto TTS (NSE 4.0)",
                onBack = onBack,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHost) },
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 8.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.resetSettings()
                            scope.launch { snackbarHost.showSnackbar("Reset to defaults") }
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                    ) {
                        Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Reset")
                    }
                    Button(
                        onClick = {
                            viewModel.saveSettings()
                            scope.launch { snackbarHost.showSnackbar("✓ Settings saved") }
                        },
                        modifier = Modifier.weight(2f).height(52.dp),
                        enabled = hasUnsaved,
                    ) {
                        Icon(Icons.Filled.Save, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Save Settings", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {

            // ── ENGINE STATUS BAR ────────────────────────────────────────────
            EngineStatusBar(state = engineState, isReady = isReady)

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {

                Spacer(Modifier.height(8.dp))

                // ════════════════════════════════════════════════════════════
                // STEP 1 — TTS ENGINE
                // ════════════════════════════════════════════════════════════
                TtsStepCard(
                    step = 1,
                    title = "Select TTS Engine",
                    icon = Icons.Filled.Hub,
                    isComplete = currentEnginePkg.isNotBlank(),
                ) {
                    Text(
                        "Which voice engine should read the screen?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(Modifier.height(8.dp))

                    // Current engine display
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Filled.RecordVoiceOver,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Active Engine",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                            )
                            Text(
                                currentEngineLabel,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        if (isReady) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = "Engine ready",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilledTonalButton(
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
                        FilledTonalButton(
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

                // ════════════════════════════════════════════════════════════
                // STEP 2 — LANGUAGE
                // ════════════════════════════════════════════════════════════
                TtsStepCard(
                    step = 2,
                    title = "Select Language",
                    icon = Icons.Filled.Language,
                    isComplete = selectedLanguage != SettingsRepository.TTS_LANG_AUTO,
                ) {
                    Text(
                        "Pick the language you want the TTS to speak. You can select multiple languages using Mix mode.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(Modifier.height(10.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp),
                    ) {
                        items(languageOptions) { (code, name) ->
                            val isSelected = selectedLanguage == code
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedLanguage = code
                                    viewModel.onVoiceSelected(null)
                                },
                                label = {
                                    Text(
                                        name,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp,
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                modifier = Modifier
                                    .height(44.dp)
                                    .semantics { contentDescription = "Language: $name" },
                            )
                        }
                    }

                    if (voices.isEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                            ),
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Filled.Warning,
                                    null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    "No voices found. Please download a TTS engine (Google TTS recommended).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                    }
                }

                // ════════════════════════════════════════════════════════════
                // STEP 3 — VOICE SELECTION
                // ════════════════════════════════════════════════════════════
                TtsStepCard(
                    step = 3,
                    title = "Select Voice",
                    icon = Icons.Filled.RecordVoiceOver,
                    isComplete = selectedVoice != null,
                ) {
                    Text(
                        "Choose a specific voice for the selected language. 'Auto' lets the engine decide.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(Modifier.height(10.dp))

                    if (filteredVoices.isEmpty() && selectedLanguage != SettingsRepository.TTS_LANG_AUTO) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(16.dp))
                                Text(
                                    "No voices available for this language. Download from Google TTS.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                    } else {
                        // Auto option
                        VoiceOption(
                            label = "Auto (Recommended)",
                            description = "Engine picks the best voice automatically",
                            isSelected = selectedVoice == null,
                            onClick = { viewModel.onVoiceSelected(null) },
                        )

                        if (filteredVoices.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(Modifier.height(4.dp))
                        }

                        filteredVoices.take(6).forEach { voice ->
                            val displayName = java.util.Locale(voice.locale.language, voice.locale.country)
                                .displayName.ifBlank { voice.name }
                            VoiceOption(
                                label = displayName,
                                description = voice.name,
                                isSelected = selectedVoice == voice,
                                onClick = { viewModel.onVoiceSelected(voice) },
                            )
                        }

                        if (filteredVoices.size > 6) {
                            Text(
                                "+${filteredVoices.size - 6} more voices available",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }

                    // Secondary voice in Mix mode
                    AnimatedVisibility(visible = screenReaderMode == SettingsRepository.TTS_MODE_DUAL) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider()
                            Text(
                                "Secondary Voice (for Mix Mode)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                            )
                            val secondaryOptions = filteredVoices.filter { it != selectedVoice }
                            VoiceOption(
                                label = "None",
                                description = "Disable secondary voice",
                                isSelected = secondaryVoice == null,
                                onClick = { viewModel.onSecondaryVoiceSelected(null) },
                            )
                            secondaryOptions.take(4).forEach { voice ->
                                val displayName = java.util.Locale(voice.locale.language, voice.locale.country)
                                    .displayName.ifBlank { voice.name }
                                VoiceOption(
                                    label = displayName,
                                    description = voice.name,
                                    isSelected = secondaryVoice == voice,
                                    onClick = { viewModel.onSecondaryVoiceSelected(voice) },
                                )
                            }
                        }
                    }
                }

                // ════════════════════════════════════════════════════════════
                // STEP 4 — SPEED & PITCH
                // ════════════════════════════════════════════════════════════
                TtsStepCard(
                    step = 4,
                    title = "Speed & Pitch",
                    icon = Icons.Filled.Tune,
                    isComplete = true,
                ) {
                    Text(
                        "Adjust how fast and high-pitched the voice sounds.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(Modifier.height(8.dp))

                    // Speed
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Speed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Speed", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                "${"%.1f".format(speechRate)}×",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                    Slider(
                        value = speechRate,
                        onValueChange = viewModel::onSpeechRateChange,
                        valueRange = 0.25f..3.0f,
                        steps = 27,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Speech speed: ${"%.1f".format(speechRate)} times" },
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Slow (0.25×)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Fast (3×)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(Modifier.height(8.dp))

                    // Pitch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Pitch", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                "${"%.1f".format(pitch)}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                    Slider(
                        value = pitch,
                        onValueChange = viewModel::onPitchChange,
                        valueRange = 0.5f..2.0f,
                        steps = 14,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Pitch: ${"%.1f".format(pitch)}" },
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Deep (0.5)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("High (2.0)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // ════════════════════════════════════════════════════════════
                // STEP 5 — TEST SPEECH
                // ════════════════════════════════════════════════════════════
                TtsStepCard(
                    step = 5,
                    title = "Test Your Voice",
                    icon = Icons.Filled.PlayCircle,
                    isComplete = false,
                ) {
                    Text(
                        "Type anything and press Speak to hear how your settings sound.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = viewModel::onTextChange,
                        placeholder = {
                            Text(
                                "Type in any language — नमस्ते, Hello, مرحبا…",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Text to speak. Type your test text here." },
                        maxLines = 4,
                        trailingIcon = {
                            if (inputText.isNotEmpty()) {
                                IconButton(onClick = viewModel::clearText) {
                                    Icon(Icons.Filled.Clear, "Clear text")
                                }
                            }
                        },
                        shape = MaterialTheme.shapes.medium,
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = viewModel::speak,
                            enabled = inputText.isNotBlank() && isReady,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .semantics { contentDescription = if (isReady) "Speak" else "Engine not ready" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            Icon(Icons.Filled.VolumeUp, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Speak", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        OutlinedButton(
                            onClick = viewModel::stop,
                            enabled = isSpeaking,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .semantics { contentDescription = "Stop speaking" },
                        ) {
                            Icon(Icons.Filled.Stop, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Stop", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }

                    // Quick test phrases
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Quick phrases:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val phrases = listOf("Hello!", "नमस्ते", "مرحبا", "Test 1 2 3", "How are you?")
                        items(phrases) { phrase ->
                            SuggestionChip(
                                onClick = { viewModel.onTextChange(phrase) },
                                label = { Text(phrase, fontSize = 13.sp) },
                            )
                        }
                    }
                }

                // ════════════════════════════════════════════════════════════
                // SCREEN READER MODE
                // ════════════════════════════════════════════════════════════
                TtsStepCard(
                    step = 6,
                    title = "Reading Mode",
                    icon = Icons.Filled.Accessibility,
                    isComplete = true,
                ) {
                    Text(
                        "How should NSE handle multi-language content?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(Modifier.height(10.dp))

                    Column(
                        modifier = Modifier.selectableGroup(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        modeOptions.forEach { (mode, label) ->
                            val (title, desc) = label.split(" — ").let {
                                if (it.size >= 2) it[0] to it[1] else label to ""
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(
                                        if (screenReaderMode == mode) MaterialTheme.colorScheme.primaryContainer
                                        else Color.Transparent
                                    )
                                    .selectable(
                                        selected = screenReaderMode == mode,
                                        role = Role.RadioButton,
                                        onClick = { viewModel.onScreenReaderModeChange(mode) },
                                    )
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                RadioButton(
                                    selected = screenReaderMode == mode,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (screenReaderMode == mode)
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurface,
                                        ),
                                    )
                                    if (desc.isNotBlank()) {
                                        Text(
                                            desc,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (screenReaderMode == mode)
                                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ════════════════════════════════════════════════════════════
                // ACCESSIBILITY TOGGLES
                // ════════════════════════════════════════════════════════════
                TtsStepCard(
                    step = 7,
                    title = "Accessibility Options",
                    icon = Icons.Filled.AdminPanelSettings,
                    isComplete = true,
                ) {
                    Text(
                        "Fine-tune how NSE reads content for screen reader users.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(Modifier.height(8.dp))

                    TtsToggleRow("Filter Notifications", "Skip speaking notification pop-ups", notificationFilter, viewModel::onNotificationFilterChange)
                    TtsToggleRow("Window Detection", "Announce when switching apps or screens", windowChange, viewModel::onWindowChangeDetectionChange)
                    TtsToggleRow("Focus Tracking", "Follow accessibility focus in real time", focusTracking, viewModel::onFocusTrackingChange)
                    TtsToggleRow("Continuous Reading", "Read all visible content automatically", continuousRead, viewModel::onContinuousReadChange)
                    TtsToggleRow("Duplicate Filter", "Prevent repeating identical sentences", duplicateFilter, viewModel::onDuplicateFilterChange)
                    TtsToggleRow("Auto Start", "Start screen reader when accessibility connects", autoStart, viewModel::onAutoStartChange)
                }

                // ── Activate System-Wide TTS ──────────────────────────────
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                Icons.Filled.Info,
                                null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                "Enable Device-Wide Screen Reader",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "To use NSE as your device's main screen reader (reads all apps), go to Android Accessibility Settings and enable NSE Auto TTS.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary,
                            ),
                        ) {
                            Icon(Icons.Filled.Accessibility, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Open Accessibility Settings", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ── Sub-components ────────────────────────────────────────────────────────────

@Composable
private fun EngineStatusBar(state: NseState, isReady: Boolean) {
    val (text, icon, containerColor, contentColor) = when {
        isReady && state == NseState.Speaking -> Quad(
            "Speaking…",
            Icons.Filled.VolumeUp,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
        )
        isReady -> Quad(
            "NSE Engine Ready — Free & Offline TTS",
            Icons.Filled.CheckCircle,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
        state is NseState.Error -> Quad(
            "Error: ${state.message}",
            Icons.Filled.ErrorOutline,
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
        )
        else -> Quad(
            "Engine initialising…",
            Icons.Filled.HourglassEmpty,
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, null, tint = contentColor, modifier = Modifier.size(18.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = contentColor,
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
private fun TtsStepCard(
    step: Int,
    title: String,
    icon: ImageVector,
    isComplete: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 12.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isComplete) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isComplete) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp),
                            )
                        } else {
                            Text(
                                "$step",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.weight(1f))
                if (isComplete) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Complete",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun VoiceOption(
    label: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .semantics { contentDescription = "Voice: $label. ${if (isSelected) "Selected." else ""}" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
            ),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (description != label && description.isNotBlank()) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun TtsToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.semantics { contentDescription = "$label: ${if (checked) "on" else "off"}" },
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}
