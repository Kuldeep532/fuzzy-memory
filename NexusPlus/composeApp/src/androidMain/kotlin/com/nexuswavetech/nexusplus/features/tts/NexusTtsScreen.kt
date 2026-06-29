package com.nexuswavetech.nexusplus.features.tts

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexuswavetech.nexusplus.ui.components.NexusTopBar
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexusTtsScreen(
    onBack: () -> Unit,
    viewModel: NseViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val view    = LocalView.current
    val scope   = rememberCoroutineScope()
    val snack   = remember { SnackbarHostState() }

    val engineState by viewModel.engineState.collectAsState()
    val voices      by viewModel.availableVoices.collectAsState()
    val inputText   by viewModel.inputText.collectAsState()
    val isSpeaking  by viewModel.isSpeaking.collectAsState()
    val speechRate  by viewModel.speechRate.collectAsState()
    val pitch       by viewModel.pitch.collectAsState()
    val selectedVoice by viewModel.selectedVoice.collectAsState()
    val hasUnsaved  by viewModel.hasUnsavedChanges.collectAsState()

    val isReady = engineState == NseState.Ready || engineState == NseState.Speaking

    // Build simple voice list for dropdown
    val voiceOptions: List<Pair<NseVoiceProfile?, String>> = remember(voices) {
        listOf(null to "Auto (detect language)") +
        voices.map { v ->
            v to buildString {
                append(v.locale.displayLanguage.ifBlank { v.locale.toLanguageTag() })
                if (v.name.isNotBlank()) append(" — ${v.name}")
            }
        }
    }

    Scaffold(
        topBar = { NexusTopBar(title = "Auto TTS", onBack = onBack) },
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            if (hasUnsaved) Surface(tonalElevation = 4.dp, shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.resetSettings()
                            scope.launch { snack.showSnackbar("Reset to defaults") }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reset")
                    }
                    Button(
                        onClick = {
                            viewModel.saveSettings()
                            scope.launch { snack.showSnackbar("Settings saved") }
                        },
                        modifier = Modifier.weight(2f),
                    ) {
                        Icon(Icons.Filled.Save, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Save Settings", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {

            // ── Engine status banner ───────────────────────────────────────
            val bannerColor by animateColorAsState(
                targetValue = when (engineState) {
                    NseState.Ready, NseState.Speaking ->
                        MaterialTheme.colorScheme.primaryContainer
                    NseState.Initialising ->
                        MaterialTheme.colorScheme.surfaceVariant
                    else ->
                        MaterialTheme.colorScheme.errorContainer
                },
                animationSpec = tween(400),
                label = "bannerColor",
            )
            val bannerTextColor = when (engineState) {
                NseState.Ready, NseState.Speaking ->
                    MaterialTheme.colorScheme.onPrimaryContainer
                NseState.Initialising ->
                    MaterialTheme.colorScheme.onSurfaceVariant
                else ->
                    MaterialTheme.colorScheme.onErrorContainer
            }
            val bannerText = when (engineState) {
                NseState.Initialising -> "Setting up voice engine…"
                NseState.Ready        -> "Ready to speak"
                NseState.Speaking     -> "Speaking…"
                else                  -> "Voice engine error — check TTS settings"
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bannerColor)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .semantics { contentDescription = "TTS engine status: $bannerText" },
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (engineState == NseState.Initialising) {
                    CircularProgressIndicator(
                        Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = bannerTextColor,
                    )
                } else {
                    Icon(
                        if (isReady) Icons.Filled.RecordVoiceOver else Icons.Filled.Error,
                        contentDescription = null,
                        tint = bannerTextColor,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(bannerText, style = MaterialTheme.typography.labelMedium, color = bannerTextColor)

                if (engineState != NseState.Initialising) {
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(intent)
                        },
                        contentPadding = PaddingValues(horizontal = 4.dp),
                    ) {
                        Text("TTS Settings", style = MaterialTheme.typography.labelSmall, color = bannerTextColor)
                    }
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {

                // ── Text input ─────────────────────────────────────────────
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
                                "Enter text to speak",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                            )
                            if (inputText.isNotBlank()) {
                                IconButton(
                                    onClick = { viewModel.clearText() },
                                    modifier = Modifier.size(28.dp),
                                ) {
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
                                .heightIn(min = 130.dp)
                                .semantics {
                                    contentDescription = "Text input. Type what you want the device to speak aloud."
                                },
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

                        Spacer(Modifier.height(12.dp))

                        // Quick phrase chips
                        val quickPhrases = listOf("Hello, how are you?", "Welcome!", "Thank you.", "Please wait.")
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 2.dp),
                        ) {
                            items(quickPhrases) { phrase ->
                                SuggestionChip(
                                    onClick = { viewModel.onTextChange(phrase) },
                                    label = { Text(phrase, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.semantics {
                                        contentDescription = "Quick phrase: $phrase"
                                    },
                                )
                            }
                        }
                    }
                }

                // ── Speak / Stop buttons ────────────────────────────────────
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            .weight(1f)
                            .height(60.dp)
                            .semantics {
                                contentDescription = if (isSpeaking) "Stop speaking" else "Speak text aloud"
                            },
                        shape = MaterialTheme.shapes.large,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSpeaking) MaterialTheme.colorScheme.error
                                            else MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Icon(
                            if (isSpeaking) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isSpeaking) "Stop" else "Speak",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                // ── Speed slider ────────────────────────────────────────────
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(1.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                "Speed",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                "${"%.1f".format(speechRate)}×",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Slider(
                            value = speechRate,
                            onValueChange = viewModel::onSpeechRateChange,
                            valueRange = 0.25f..3.0f,
                            steps = 27,
                            modifier = Modifier.semantics {
                                contentDescription = "Speech speed slider. Current speed: ${"%.1f".format(speechRate)} times normal."
                            },
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Slow", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Normal (1×)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Fast", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // ── Pitch slider ────────────────────────────────────────────
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(1.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                "Pitch",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                "${"%.1f".format(pitch)}×",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Slider(
                            value = pitch,
                            onValueChange = viewModel::onPitchChange,
                            valueRange = 0.5f..2.0f,
                            steps = 15,
                            modifier = Modifier.semantics {
                                contentDescription = "Pitch slider. Current pitch: ${"%.1f".format(pitch)}."
                            },
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Low", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Normal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("High", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // ── Voice selector ──────────────────────────────────────────
                VoiceDropdown(
                    label = "Voice",
                    options = voiceOptions,
                    selected = selectedVoice,
                    onSelect = viewModel::onVoiceSelected,
                )

                // ── TTS engine not ready message ────────────────────────────
                AnimatedVisibility(visible = !isReady && engineState != NseState.Initialising) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                "TTS engine not available",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Text(
                                "Install a TTS engine from the Play Store (e.g. Google Text-to-Speech), then return here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                ),
                            ) {
                                Text("Open TTS Settings")
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
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
        shape = MaterialTheme.shapes.extraLarge,
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

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
            ) {
                OutlinedTextField(
                    value = selectedLabel,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                        .fillMaxWidth()
                        .semantics { contentDescription = "Voice selector. Currently: $selectedLabel." },
                    singleLine = true,
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
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
