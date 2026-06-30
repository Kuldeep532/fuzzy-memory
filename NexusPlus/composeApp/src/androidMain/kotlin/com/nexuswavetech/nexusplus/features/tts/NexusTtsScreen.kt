package com.nexuswavetech.nexusplus.features.tts

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import org.koin.androidx.compose.koinViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexusTtsScreen(
    onBack: () -> Unit,
    viewModel: NseViewModel = koinViewModel(),
) {
    val view          = LocalView.current

    val engineState   by viewModel.engineState.collectAsState()
    val voices        by viewModel.availableVoices.collectAsState()
    val inputText     by viewModel.inputText.collectAsState()
    val isSpeaking    by viewModel.isSpeaking.collectAsState()
    val speechRate    by viewModel.speechRate.collectAsState()
    val pitch         by viewModel.pitch.collectAsState()
    val currentMode   by viewModel.mode.collectAsState()
    val selectedVoice by viewModel.selectedVoice.collectAsState()
    val secondaryVoice by viewModel.secondaryVoice.collectAsState()
    val isReady = engineState == NseState.Ready || engineState == NseState.Speaking

    // Infer current mode label
    val currentModeLabel = when (currentMode) {
        is NseSpeechMode.Auto        -> "Auto"
        is NseSpeechMode.Mix         -> "Mix"
        is NseSpeechMode.DualVoice   -> "Dual"
        is NseSpeechMode.SingleVoice -> "Single"
    }

    // Auto-save whenever settings change (TalkBack-style — no manual Save button)
    LaunchedEffect(speechRate, pitch, currentMode, selectedVoice, secondaryVoice) {
        viewModel.saveSettings()
    }

    Scaffold(
        topBar = { NexusTopBar(title = "Text to Speech", onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {

            // ── Engine status banner ─────────────────────────────────────────
            val bannerColor by animateColorAsState(
                targetValue = when (engineState) {
                    NseState.Ready, NseState.Speaking -> MaterialTheme.colorScheme.primaryContainer
                    NseState.Initialising             -> MaterialTheme.colorScheme.surfaceVariant
                    else                              -> MaterialTheme.colorScheme.errorContainer
                },
                animationSpec = tween(400), label = "bannerColor",
            )
            val bannerFg = when (engineState) {
                NseState.Ready, NseState.Speaking -> MaterialTheme.colorScheme.onPrimaryContainer
                NseState.Initialising             -> MaterialTheme.colorScheme.onSurfaceVariant
                else                              -> MaterialTheme.colorScheme.onErrorContainer
            }
            val bannerText = when (engineState) {
                NseState.Initialising -> "Setting up voice engine…"
                NseState.Ready        -> "Ready · $currentModeLabel mode"
                NseState.Speaking     -> "Speaking…"
                else                  -> "Install a TTS engine from Play Store and download voices for offline use"
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bannerColor)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .semantics { contentDescription = "Engine status: $bannerText" },
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (engineState == NseState.Initialising) {
                    CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = bannerFg)
                } else {
                    Icon(
                        if (isReady) Icons.Filled.RecordVoiceOver else Icons.Filled.Error,
                        contentDescription = null,
                        tint = bannerFg,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(bannerText, style = MaterialTheme.typography.labelMedium, color = bannerFg, modifier = Modifier.weight(1f))
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {

                // ── Mode selector dropdown ────────────────────────────────────
                ModeDropdown(
                    currentLabel = currentModeLabel,
                    onModeSelected = { label ->
                        when (label) {
                            "Auto"   -> viewModel.onModeChange(NseSpeechMode.Auto)
                            "Mix"    -> viewModel.onModeChange(NseSpeechMode.Mix)
                            "Dual"   -> {
                                val p = selectedVoice
                                val s = secondaryVoice
                                viewModel.onModeChange(
                                    if (p != null && s != null) NseSpeechMode.DualVoice(p.locale, s.locale)
                                    else NseSpeechMode.Auto
                                )
                            }
                            "Single" -> {
                                val v = selectedVoice
                                viewModel.onModeChange(
                                    if (v != null) NseSpeechMode.SingleVoice(v.locale)
                                    else NseSpeechMode.Auto
                                )
                            }
                        }
                    },
                )

                // ── Language selector (visible in ALL modes) ─────────────────
                LanguageSelectorCard(
                    selectedLanguage = selectedVoice?.locale?.toLanguageTag() ?: "",
                    onLanguageSelected = viewModel::onLanguageSelected,
                )

                // ── Primary voice dropdown ────────────────────────────────────
                val primaryOptions: List<Pair<NseVoiceProfile?, String>> =
                    listOf(null to "Auto (detect language)") +
                    voices.map { v ->
                        v to buildString {
                            append(v.locale.displayLanguage.ifBlank { v.locale.toLanguageTag() })
                            if (v.name.isNotBlank()) append(" — ${v.name}")
                        }
                    }

                VoiceDropdown(
                    label       = when (currentModeLabel) { "Dual" -> "Primary Voice"; else -> "Voice" },
                    options     = primaryOptions,
                    selected    = selectedVoice,
                    onSelect    = viewModel::onVoiceSelected,
                )

                // ── Secondary voice (only for Dual mode) ─────────────────────
                AnimatedVisibility(visible = currentModeLabel == "Dual") {
                    VoiceDropdown(
                        label       = "Secondary Voice",
                        options     = primaryOptions,
                        selected    = secondaryVoice,
                        onSelect    = viewModel::onSecondaryVoiceSelected,
                    )
                }


                // ── Mix mode extra hint ──────────────────────────────────────
                AnimatedVisibility(visible = currentModeLabel == "Mix") {
                    Text("Mix mode auto-detects multiple languages from input text", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AnimatedVisibility(visible = currentModeLabel == "Auto") {
                    Text("Auto mode detects language automatically from your text", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // ── Text input ────────────────────────────────────────────────
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(1.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Text to speak",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                            )
                            if (inputText.isNotBlank()) {
                                IconButton(onClick = { viewModel.clearText() }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Filled.Clear, "Clear text", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        BasicTextField(
                            value = inputText,
                            onValueChange = viewModel::onTextChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp)
                                .semantics { contentDescription = "Type what you want spoken aloud" },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 26.sp,
                            ),
                            decorationBox = { inner ->
                                if (inputText.isEmpty()) {
                                    Text(
                                        "Type or paste text here…",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    )
                                }
                                inner()
                            },
                        )
                        Spacer(Modifier.height(10.dp))
                        // Quick phrase chips
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 2.dp)) {
                            items(listOf("Hello!", "Welcome!", "Thank you.", "Please wait.", "Good morning!")) { phrase ->
                                SuggestionChip(
                                    onClick = { viewModel.onTextChange(phrase) },
                                    label = { Text(phrase, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.semantics { contentDescription = "Quick phrase: $phrase" },
                                )
                            }
                        }
                    }
                }

                // ── Speak / Stop ─────────────────────────────────────────────
                Button(
                    onClick = {
                        if (isSpeaking) {
                            viewModel.stop()
                            view.announceForAccessibility("Speech stopped.")
                        } else {
                            viewModel.speak()
                            view.announceForAccessibility("Speaking.")
                        }
                    },
                    enabled = isReady && inputText.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .semantics { contentDescription = if (isSpeaking) "Stop speaking" else "Speak text aloud" },
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSpeaking) MaterialTheme.colorScheme.error
                                         else MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    AnimatedContent(isSpeaking, label = "speakIcon") { speaking ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                if (speaking) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(26.dp),
                            )
                            Text(
                                if (speaking) "Stop" else "Speak",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                // ── Speed ────────────────────────────────────────────────────
                SliderCard(
                    label    = "Speed",
                    valueStr = "${"%.1f".format(speechRate)}×",
                    value    = speechRate,
                    onValue  = viewModel::onSpeechRateChange,
                    range    = 0.25f..3.0f,
                    steps    = 27,
                    lo = "Slow", mid = "Normal", hi = "Fast",
                    cd = "Speed slider. Current: ${"%.1f".format(speechRate)} times normal.",
                )

                // ── Pitch ────────────────────────────────────────────────────
                SliderCard(
                    label    = "Pitch",
                    valueStr = "${"%.1f".format(pitch)}×",
                    value    = pitch,
                    onValue  = viewModel::onPitchChange,
                    range    = 0.5f..2.0f,
                    steps    = 15,
                    lo = "Low", mid = "Normal", hi = "High",
                    cd = "Pitch slider. Current: ${"%.1f".format(pitch)}.",
                )

                // ── Engine error card ────────────────────────────────────────
                AnimatedVisibility(visible = !isReady && engineState != NseState.Initialising) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Voice engine not available",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Text(
                                "Install Google Text-to-Speech or any TTS engine from the Play Store, then reopen this screen.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

// ── Mode dropdown ─────────────────────────────────────────────────────────────

private val MODE_LABELS = listOf(
    "Auto"   to "Detects language automatically",
    "Mix"    to "Switches voice per language segment",
    "Dual"   to "Primary + secondary voice",
    "Single" to "One voice for everything",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeDropdown(
    currentLabel: String,
    onModeSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val currentDesc = MODE_LABELS.firstOrNull { it.first == currentLabel }?.second ?: ""

    Card(
        shape  = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                "Speech Mode",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Text(
                currentDesc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
            ) {
                OutlinedTextField(
                    value         = currentLabel,
                    onValueChange = {},
                    readOnly      = true,
                    trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors        = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier      = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                        .fillMaxWidth()
                        .semantics { contentDescription = "Speech mode selector. Current: $currentLabel. $currentDesc" },
                    singleLine    = true,
                    leadingIcon   = { Icon(Icons.Filled.Tune, null, modifier = Modifier.size(18.dp)) },
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    MODE_LABELS.forEach { (label, desc) ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                    Text(desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            onClick = { onModeSelected(label); expanded = false },
                            leadingIcon = if (label == currentLabel) {
                                { Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) }
                            } else null,
                        )
                    }
                }
            }
        }
    }
}

// ── Voice dropdown ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceDropdown(
    label: String,
    options: List<Pair<NseVoiceProfile?, String>>,
    selected: NseVoiceProfile?,
    onSelect: (NseVoiceProfile?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selected }?.second ?: "Auto (detect language)"

    Card(
        shape  = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value         = selectedLabel,
                    onValueChange = {},
                    readOnly      = true,
                    trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors        = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier      = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                        .fillMaxWidth()
                        .semantics { contentDescription = "$label selector. Currently: $selectedLabel." },
                    singleLine    = true,
                    leadingIcon   = { Icon(Icons.Filled.RecordVoiceOver, null, modifier = Modifier.size(18.dp)) },
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.forEach { (voiceProfile, voiceLabel) ->
                        DropdownMenuItem(
                            text = { Text(voiceLabel, style = MaterialTheme.typography.bodyMedium) },
                            onClick = { onSelect(voiceProfile); expanded = false },
                            leadingIcon = if (voiceProfile == selected) {
                                { Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) }
                            } else null,
                        )
                    }
                }
            }
        }
    }
}

// ── Slider card ────────────────────────────────────────────────────────────────

@Composable
private fun SliderCard(
    label: String, valueStr: String,
    value: Float, onValue: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>, steps: Int,
    lo: String, mid: String, hi: String, cd: String,
) {
    Card(
        shape  = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                Text(valueStr, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = value, onValueChange = onValue,
                valueRange = range, steps = steps,
                modifier = Modifier.semantics { contentDescription = cd },
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(lo,  style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(mid, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(hi,  style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
